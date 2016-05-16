/*
**
** Copyright 2012, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


#define LOG_TAG "AudioFlinger"
//#define LOG_NDEBUG 0
#define ATRACE_TAG ATRACE_TAG_AUDIO

#include "Configuration.h"
#include <math.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <cutils/properties.h>
#include <media/AudioParameter.h>
#include <utils/Log.h>
#include <utils/Trace.h>

#include <private/media/AudioTrackShared.h>
#include <hardware/audio.h>
#include <audio_effects/effect_ns.h>
#include <audio_effects/effect_aec.h>
#include <audio_utils/primitives.h>

// NBAIO implementations
#include <media/nbaio/AudioStreamOutSink.h>
#include <media/nbaio/MonoPipe.h>
#include <media/nbaio/MonoPipeReader.h>
#include <media/nbaio/Pipe.h>
#include <media/nbaio/PipeReader.h>
#include <media/nbaio/SourceAudioBufferProvider.h>

#include <powermanager/PowerManager.h>

#include <common_time/cc_helper.h>
#include <common_time/local_clock.h>

#include "AudioFlinger.h"
#include "AudioMixer.h"
#include "FastMixer.h"
#include "ServiceUtilities.h"
#include "SchedulingPolicyService.h"

#ifdef ADD_BATTERY_DATA
#include <media/IMediaPlayerService.h>
#include <media/IMediaDeathNotifier.h>
#endif

#ifdef DEBUG_CPU_USAGE
#include <cpustats/CentralTendencyStatistics.h>
#include <cpustats/ThreadCpuUsage.h>
#endif

// ----------------------------------------------------------------------------

// Note: the following macro is used for extremely verbose logging message.  In
// order to run with ALOG_ASSERT turned on, we need to have LOG_NDEBUG set to
// 0; but one side effect of this is to turn all LOGV's as well.  Some messages
// are so verbose that we want to suppress them even when we have ALOG_ASSERT
// turned on.  Do not uncomment the #def below unless you really know what you
// are doing and want to see all of the extremely verbose messages.
//#define VERY_VERY_VERBOSE_LOGGING
#ifdef VERY_VERY_VERBOSE_LOGGING
#define ALOGVV ALOGV
#else
#define ALOGVV(a...) do { } while(0)
#endif

namespace android {

// retry counts for buffer fill timeout
// 50 * ~20msecs = 1 second
static const int8_t kMaxTrackRetries = 50;
static const int8_t kMaxTrackStartupRetries = 50;
// allow less retry attempts on direct output thread.
// direct outputs can be a scarce resource in audio hardware and should
// be released as quickly as possible.
static const int8_t kMaxTrackRetriesDirect = 2;

// don't warn about blocked writes or record buffer overflows more often than this
static const nsecs_t kWarningThrottleNs = seconds(5);

// RecordThread loop sleep time upon application overrun or audio HAL read error
static const int kRecordThreadSleepUs = 5000;

// maximum time to wait for setParameters to complete
static const nsecs_t kSetParametersTimeoutNs = seconds(2);

// minimum sleep time for the mixer thread loop when tracks are active but in underrun
static const uint32_t kMinThreadSleepTimeUs = 5000;
// maximum divider applied to the active sleep time in the mixer thread loop
static const uint32_t kMaxThreadSleepTimeShift = 2;

// minimum normal mix buffer size, expressed in milliseconds rather than frames
static const uint32_t kMinNormalMixBufferSizeMs = 20;
// maximum normal mix buffer size
static const uint32_t kMaxNormalMixBufferSizeMs = 24;

// Offloaded output thread standby delay: allows track transition without going to standby
static const nsecs_t kOffloadStandbyDelayNs = seconds(1);

// Whether to use fast mixer
static const enum {
    FastMixer_Never,    // never initialize or use: for debugging only
    FastMixer_Always,   // always initialize and use, even if not needed: for debugging only
                        // normal mixer multiplier is 1
    FastMixer_Static,   // initialize if needed, then use all the time if initialized,
                        // multiplier is calculated based on min & max normal mixer buffer size
    FastMixer_Dynamic,  // initialize if needed, then use dynamically depending on track load,
                        // multiplier is calculated based on min & max normal mixer buffer size
    // FIXME for FastMixer_Dynamic:
    //  Supporting this option will require fixing HALs that can't handle large writes.
    //  For example, one HAL implementation returns an error from a large write,
    //  and another HAL implementation corrupts memory, possibly in the sample rate converter.
    //  We could either fix the HAL implementations, or provide a wrapper that breaks
    //  up large writes into smaller ones, and the wrapper would need to deal with scheduler.
} kUseFastMixer = FastMixer_Static;

// Priorities for requestPriority
static const int kPriorityAudioApp = 2;
static const int kPriorityFastMixer = 3;

// IAudioFlinger::createTrack() reports back to client the total size of shared memory area
// for the track.  The client then sub-divides this into smaller buffers for its use.
// Currently the client uses double-buffering by default, but doesn't tell us about that.
// So for now we just assume that client is double-buffered.
// FIXME It would be better for client to tell AudioFlinger whether it wants double-buffering or
// N-buffering, so AudioFlinger could allocate the right amount of memory.
// See the client's minBufCount and mNotificationFramesAct calculations for details.
static const int kFastTrackMultiplier = 1;

// ----------------------------------------------------------------------------

#ifdef ADD_BATTERY_DATA
// To collect the amplifier usage
static void addBatteryData(uint32_t params) {
    sp<IMediaPlayerService> service = IMediaDeathNotifier::getMediaPlayerService();
    if (service == NULL) {
        // it already logged
        return;
    }

    service->addBatteryData(params);
}
#endif


// ----------------------------------------------------------------------------
//      CPU Stats
// ----------------------------------------------------------------------------

class CpuStats {
public:
    CpuStats();
    void sample(const String8 &title);
#ifdef DEBUG_CPU_USAGE
private:
    ThreadCpuUsage mCpuUsage;           // instantaneous thread CPU usage in wall clock ns
    CentralTendencyStatistics mWcStats; // statistics on thread CPU usage in wall clock ns

    CentralTendencyStatistics mHzStats; // statistics on thread CPU usage in cycles

    int mCpuNum;                        // thread's current CPU number
    int mCpukHz;                        // frequency of thread's current CPU in kHz
#endif
};

CpuStats::CpuStats()
#ifdef DEBUG_CPU_USAGE
    : mCpuNum(-1), mCpukHz(-1)
#endif
{
}

void CpuStats::sample(const String8 &title) {
#ifdef DEBUG_CPU_USAGE
    // get current thread's delta CPU time in wall clock ns
    double wcNs;
    bool valid = mCpuUsage.sampleAndEnable(wcNs);

    // record sample for wall clock statistics
    if (valid) {
        mWcStats.sample(wcNs);
    }

    // get the current CPU number
    int cpuNum = sched_getcpu();

    // get the current CPU frequency in kHz
    int cpukHz = mCpuUsage.getCpukHz(cpuNum);

    // check if either CPU number or frequency changed
    if (cpuNum != mCpuNum || cpukHz != mCpukHz) {
        mCpuNum = cpuNum;
        mCpukHz = cpukHz;
        // ignore sample for purposes of cycles
        valid = false;
    }

    // if no change in CPU number or frequency, then record sample for cycle statistics
    if (valid && mCpukHz > 0) {
        double cycles = wcNs * cpukHz * 0.000001;
        mHzStats.sample(cycles);
    }

    unsigned n = mWcStats.n();
    // mCpuUsage.elapsed() is expensive, so don't call it every loop
    if ((n & 127) == 1) {
        long long elapsed = mCpuUsage.elapsed();
        if (elapsed >= DEBUG_CPU_USAGE * 1000000000LL) {
            double perLoop = elapsed / (double) n;
            double perLoop100 = perLoop * 0.01;
            double perLoop1k = perLoop * 0.001;
            double mean = mWcStats.mean();
            double stddev = mWcStats.stddev();
            double minimum = mWcStats.minimum();
            double maximum = mWcStats.maximum();
            double meanCycles = mHzStats.mean();
            double stddevCycles = mHzStats.stddev();
            double minCycles = mHzStats.minimum();
            double maxCycles = mHzStats.maximum();
            mCpuUsage.resetElapsed();
            mWcStats.reset();
            mHzStats.reset();
            ALOGD("CPU usage for %s over past %.1f secs\n"
                "  (%u mixer loops at %.1f mean ms per loop):\n"
                "  us per mix loop: mean=%.0f stddev=%.0f min=%.0f max=%.0f\n"
                "  %% of wall: mean=%.1f stddev=%.1f min=%.1f max=%.1f\n"
                "  MHz: mean=%.1f, stddev=%.1f, min=%.1f max=%.1f",
                    title.string(),
                    elapsed * .000000001, n, perLoop * .000001,
                    mean * .001,
                    stddev * .001,
                    minimum * .001,
                    maximum * .001,
                    mean / perLoop100,
                    stddev / perLoop100,
                    minimum / perLoop100,
                    maximum / perLoop100,
                    meanCycles / perLoop1k,
                    stddevCycles / perLoop1k,
                    minCycles / perLoop1k,
                    maxCycles / perLoop1k);

        }
    }
#endif
};

// ----------------------------------------------------------------------------
//      ThreadBase
// ----------------------------------------------------------------------------

AudioFlinger::ThreadBase::ThreadBase(const sp<AudioFlinger>& audioFlinger, audio_io_handle_t id,
        audio_devices_t outDevice, audio_devices_t inDevice, type_t type)
    :   Thread(false /*canCallJava*/),
        mType(type),
        mAudioFlinger(audioFlinger),
        // mSampleRate, mFrameCount, mChannelMask, mChannelCount, mFrameSize, and mFormat are
        // set by PlaybackThread::readOutputParameters() or RecordThread::readInputParameters()
        mParamStatus(NO_ERROR),
        //FIXME: mStandby should be true here. Is this some kind of hack?
        mStandby(false), mOutDevice(outDevice), mInDevice(inDevice),
        mAudioSource(AUDIO_SOURCE_DEFAULT), mId(id),
        // mName will be set by concrete (non-virtual) subclass
        mDeathRecipient(new PMDeathRecipient(this))
{
}

AudioFlinger::ThreadBase::~ThreadBase()
{
    // mConfigEvents should be empty, but just in case it isn't, free the memory it owns
    for (size_t i = 0; i < mConfigEvents.size(); i++) {
        delete mConfigEvents[i];
    }
    mConfigEvents.clear();

    mParamCond.broadcast();
    // do not lock the mutex in destructor
    releaseWakeLock_l();
    if (mPowerManager != 0) {
        sp<IBinder> binder = mPowerManager->asBinder();
        binder->unlinkToDeath(mDeathRecipient);
    }
}

void AudioFlinger::ThreadBase::exit()
{
    ALOGV("ThreadBase::exit");
    // do any cleanup required for exit to succeed
    preExit();
    {
        // This lock prevents the following race in thread (uniprocessor for illustration):
        //  if (!exitPending()) {
        //      // context switch from here to exit()
        //      // exit() calls requestExit(), what exitPending() observes
        //      // exit() calls signal(), which is dropped since no waiters
        //      // context switch back from exit() to here
        //      mWaitWorkCV.wait(...);
        //      // now thread is hung
        //  }
        AutoMutex lock(mLock);
        requestExit();
        mWaitWorkCV.broadcast();
    }
    // When Thread::requestExitAndWait is made virtual and this method is renamed to
    // "virtual status_t requestExitAndWait()", replace by "return Thread::requestExitAndWait();"
    requestExitAndWait();
}

status_t AudioFlinger::ThreadBase::setParameters(const String8& keyValuePairs)
{
    status_t status;

    ALOGV("ThreadBase::setParameters() %s", keyValuePairs.string());
    Mutex::Autolock _l(mLock);

    mNewParameters.add(keyValuePairs);
    mWaitWorkCV.signal();
    // wait condition with timeout in case the thread loop has exited
    // before the request could be processed
    if (mParamCond.waitRelative(mLock, kSetParametersTimeoutNs) == NO_ERROR) {
        status = mParamStatus;
        mWaitWorkCV.signal();
    } else {
        status = TIMED_OUT;
    }
    return status;
}

void AudioFlinger::ThreadBase::sendIoConfigEvent(int event, int param)
{
    Mutex::Autolock _l(mLock);
    sendIoConfigEvent_l(event, param);
}

// sendIoConfigEvent_l() must be called with ThreadBase::mLock held
void AudioFlinger::ThreadBase::sendIoConfigEvent_l(int event, int param)
{
    IoConfigEvent *ioEvent = new IoConfigEvent(event, param);
    mConfigEvents.add(static_cast<ConfigEvent *>(ioEvent));
    ALOGV("sendIoConfigEvent() num events %d event %d, param %d", mConfigEvents.size(), event,
            param);
    mWaitWorkCV.signal();
}

// sendPrioConfigEvent_l() must be called with ThreadBase::mLock held
void AudioFlinger::ThreadBase::sendPrioConfigEvent_l(pid_t pid, pid_t tid, int32_t prio)
{
    PrioConfigEvent *prioEvent = new PrioConfigEvent(pid, tid, prio);
    mConfigEvents.add(static_cast<ConfigEvent *>(prioEvent));
    ALOGV("sendPrioConfigEvent_l() num events %d pid %d, tid %d prio %d",
          mConfigEvents.size(), pid, tid, prio);
    mWaitWorkCV.signal();
}

void AudioFlinger::ThreadBase::processConfigEvents()
{
    mLock.lock();
    while (!mConfigEvents.isEmpty()) {
        ALOGV("processConfigEvents() remaining events %d", mConfigEvents.size());
        ConfigEvent *event = mConfigEvents[0];
        mConfigEvents.removeAt(0);
        // release mLock before locking AudioFlinger mLock: lock order is always
        // AudioFlinger then ThreadBase to avoid cross deadlock
        mLock.unlock();
        switch(event->type()) {
            case CFG_EVENT_PRIO: {
                PrioConfigEvent *prioEvent = static_cast<PrioConfigEvent *>(event);
                // FIXME Need to understand why this has be done asynchronously
                int err = requestPriority(prioEvent->pid(), prioEvent->tid(), prioEvent->prio(),
                        true /*asynchronous*/);
                if (err != 0) {
                    ALOGW("Policy SCHED_FIFO priority %d is unavailable for pid %d tid %d; "
                          "error %d",
                          prioEvent->prio(), prioEvent->pid(), prioEvent->tid(), err);
                }
            } break;
            case CFG_EVENT_IO: {
                IoConfigEvent *ioEvent = static_cast<IoConfigEvent *>(event);
                mAudioFlinger->mLock.lock();
                audioConfigChanged_l(ioEvent->event(), ioEvent->param());
                mAudioFlinger->mLock.unlock();
            } break;
            default:
                ALOGE("processConfigEvents() unknown event type %d", event->type());
                break;
        }
        delete event;
        mLock.lock();
    }
    mLock.unlock();
}

void AudioFlinger::ThreadBase::dumpBase(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    bool locked = AudioFlinger::dumpTryLock(mLock);
    if (!locked) {
        snprintf(buffer, SIZE, "thread %p maybe dead locked\n", this);
        write(fd, buffer, strlen(buffer));
    }

    snprintf(buffer, SIZE, "io handle: %d\n", mId);
    result.append(buffer);
    snprintf(buffer, SIZE, "TID: %d\n", getTid());
    result.append(buffer);
    snprintf(buffer, SIZE, "standby: %d\n", mStandby);
    result.append(buffer);
    snprintf(buffer, SIZE, "Sample rate: %u\n", mSampleRate);
    result.append(buffer);
    snprintf(buffer, SIZE, "HAL frame count: %d\n", mFrameCount);
    result.append(buffer);
    snprintf(buffer, SIZE, "Channel Count: %u\n", mChannelCount);
    result.append(buffer);
    snprintf(buffer, SIZE, "Channel Mask: 0x%08x\n", mChannelMask);
    result.append(buffer);
    snprintf(buffer, SIZE, "Format: %d\n", mFormat);
    result.append(buffer);
    snprintf(buffer, SIZE, "Frame size: %u\n", mFrameSize);
    result.append(buffer);

    snprintf(buffer, SIZE, "\nPending setParameters commands: \n");
    result.append(buffer);
    result.append(" Index Command");
    for (size_t i = 0; i < mNewParameters.size(); ++i) {
        snprintf(buffer, SIZE, "\n %02d    ", i);
        result.append(buffer);
        result.append(mNewParameters[i]);
    }

    snprintf(buffer, SIZE, "\n\nPending config events: \n");
    result.append(buffer);
    for (size_t i = 0; i < mConfigEvents.size(); i++) {
        mConfigEvents[i]->dump(buffer, SIZE);
        result.append(buffer);
    }
    result.append("\n");

    write(fd, result.string(), result.size());

    if (locked) {
        mLock.unlock();
    }
}

void AudioFlinger::ThreadBase::dumpEffectChains(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    snprintf(buffer, SIZE, "\n- %d Effect Chains:\n", mEffectChains.size());
    write(fd, buffer, strlen(buffer));

    for (size_t i = 0; i < mEffectChains.size(); ++i) {
        sp<EffectChain> chain = mEffectChains[i];
        if (chain != 0) {
            chain->dump(fd, args);
        }
    }
}

void AudioFlinger::ThreadBase::acquireWakeLock(int uid)
{
    Mutex::Autolock _l(mLock);
    acquireWakeLock_l(uid);
}

String16 AudioFlinger::ThreadBase::getWakeLockTag()
{
    switch (mType) {
        case MIXER:
            return String16("AudioMix");
        case DIRECT:
            return String16("AudioDirectOut");
        case DUPLICATING:
            return String16("AudioDup");
        case RECORD:
            return String16("AudioIn");
        case OFFLOAD:
            return String16("AudioOffload");
        default:
            ALOG_ASSERT(false);
            return String16("AudioUnknown");
    }
}

void AudioFlinger::ThreadBase::acquireWakeLock_l(int uid)
{
    getPowerManager_l();
    if (mPowerManager != 0) {
        sp<IBinder> binder = new BBinder();
        status_t status;
        if (uid >= 0) {
            status = mPowerManager->acquireWakeLockWithUid(POWERMANAGER_PARTIAL_WAKE_LOCK,
                    binder,
                    getWakeLockTag(),
                    String16("media"),
                    uid);
        } else {
            status = mPowerManager->acquireWakeLock(POWERMANAGER_PARTIAL_WAKE_LOCK,
                    binder,
                    getWakeLockTag(),
                    String16("media"));
        }
        if (status == NO_ERROR) {
            mWakeLockToken = binder;
        }
        ALOGV("acquireWakeLock_l() %s status %d", mName, status);
    }
}

void AudioFlinger::ThreadBase::releaseWakeLock()
{
    Mutex::Autolock _l(mLock);
    releaseWakeLock_l();
}

void AudioFlinger::ThreadBase::releaseWakeLock_l()
{
    if (mWakeLockToken != 0) {
        ALOGV("releaseWakeLock_l() %s", mName);
        if (mPowerManager != 0) {
            mPowerManager->releaseWakeLock(mWakeLockToken, 0);
        }
        mWakeLockToken.clear();
    }
}

void AudioFlinger::ThreadBase::updateWakeLockUids(const SortedVector<int> &uids) {
    Mutex::Autolock _l(mLock);
    updateWakeLockUids_l(uids);
}

void AudioFlinger::ThreadBase::getPowerManager_l() {

    if (mPowerManager == 0) {
        // use checkService() to avoid blocking if power service is not up yet
        sp<IBinder> binder =
            defaultServiceManager()->checkService(String16("power"));
        if (binder == 0) {
            ALOGW("Thread %s cannot connect to the power manager service", mName);
        } else {
            mPowerManager = interface_cast<IPowerManager>(binder);
            binder->linkToDeath(mDeathRecipient);
        }
    }
}

void AudioFlinger::ThreadBase::updateWakeLockUids_l(const SortedVector<int> &uids) {

    getPowerManager_l();
    if (mWakeLockToken == NULL) {
        ALOGE("no wake lock to update!");
        return;
    }
    if (mPowerManager != 0) {
        sp<IBinder> binder = new BBinder();
        status_t status;
        status = mPowerManager->updateWakeLockUids(mWakeLockToken, uids.size(), uids.array());
        ALOGV("acquireWakeLock_l() %s status %d", mName, status);
    }
}

void AudioFlinger::ThreadBase::clearPowerManager()
{
    Mutex::Autolock _l(mLock);
    releaseWakeLock_l();
    mPowerManager.clear();
}

void AudioFlinger::ThreadBase::PMDeathRecipient::binderDied(const wp<IBinder>& who)
{
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        thread->clearPowerManager();
    }
    ALOGW("power manager service died !!!");
}

void AudioFlinger::ThreadBase::setEffectSuspended(
        const effect_uuid_t *type, bool suspend, int sessionId)
{
    Mutex::Autolock _l(mLock);
    setEffectSuspended_l(type, suspend, sessionId);
}

void AudioFlinger::ThreadBase::setEffectSuspended_l(
        const effect_uuid_t *type, bool suspend, int sessionId)
{
    sp<EffectChain> chain = getEffectChain_l(sessionId);
    if (chain != 0) {
        if (type != NULL) {
            chain->setEffectSuspended_l(type, suspend);
        } else {
            chain->setEffectSuspendedAll_l(suspend);
        }
    }

    updateSuspendedSessions_l(type, suspend, sessionId);
}

void AudioFlinger::ThreadBase::checkSuspendOnAddEffectChain_l(const sp<EffectChain>& chain)
{
    ssize_t index = mSuspendedSessions.indexOfKey(chain->sessionId());
    if (index < 0) {
        return;
    }

    const KeyedVector <int, sp<SuspendedSessionDesc> >& sessionEffects =
            mSuspendedSessions.valueAt(index);

    for (size_t i = 0; i < sessionEffects.size(); i++) {
        sp<SuspendedSessionDesc> desc = sessionEffects.valueAt(i);
        for (int j = 0; j < desc->mRefCount; j++) {
            if (sessionEffects.keyAt(i) == EffectChain::kKeyForSuspendAll) {
                chain->setEffectSuspendedAll_l(true);
            } else {
                ALOGV("checkSuspendOnAddEffectChain_l() suspending effects %08x",
                    desc->mType.timeLow);
                chain->setEffectSuspended_l(&desc->mType, true);
            }
        }
    }
}

void AudioFlinger::ThreadBase::updateSuspendedSessions_l(const effect_uuid_t *type,
                                                         bool suspend,
                                                         int sessionId)
{
    ssize_t index = mSuspendedSessions.indexOfKey(sessionId);

    KeyedVector <int, sp<SuspendedSessionDesc> > sessionEffects;

    if (suspend) {
        if (index >= 0) {
            sessionEffects = mSuspendedSessions.valueAt(index);
        } else {
            mSuspendedSessions.add(sessionId, sessionEffects);
        }
    } else {
        if (index < 0) {
            return;
        }
        sessionEffects = mSuspendedSessions.valueAt(index);
    }


    int key = EffectChain::kKeyForSuspendAll;
    if (type != NULL) {
        key = type->timeLow;
    }
    index = sessionEffects.indexOfKey(key);

    sp<SuspendedSessionDesc> desc;
    if (suspend) {
        if (index >= 0) {
            desc = sessionEffects.valueAt(index);
        } else {
            desc = new SuspendedSessionDesc();
            if (type != NULL) {
                desc->mType = *type;
            }
            sessionEffects.add(key, desc);
            ALOGV("updateSuspendedSessions_l() suspend adding effect %08x", key);
        }
        desc->mRefCount++;
    } else {
        if (index < 0) {
            return;
        }
        desc = sessionEffects.valueAt(index);
        if (--desc->mRefCount == 0) {
            ALOGV("updateSuspendedSessions_l() restore removing effect %08x", key);
            sessionEffects.removeItemsAt(index);
            if (sessionEffects.isEmpty()) {
                ALOGV("updateSuspendedSessions_l() restore removing session %d",
                                 sessionId);
                mSuspendedSessions.removeItem(sessionId);
            }
        }
    }
    if (!sessionEffects.isEmpty()) {
        mSuspendedSessions.replaceValueFor(sessionId, sessionEffects);
    }
}

void AudioFlinger::ThreadBase::checkSuspendOnEffectEnabled(const sp<EffectModule>& effect,
                                                            bool enabled,
                                                            int sessionId)
{
    Mutex::Autolock _l(mLock);
    checkSuspendOnEffectEnabled_l(effect, enabled, sessionId);
}

void AudioFlinger::ThreadBase::checkSuspendOnEffectEnabled_l(const sp<EffectModule>& effect,
                                                            bool enabled,
                                                            int sessionId)
{
    if (mType != RECORD) {
        // suspend all effects in AUDIO_SESSION_OUTPUT_MIX when enabling any effect on
        // another session. This gives the priority to well behaved effect control panels
        // and applications not using global effects.
        // Enabling post processing in AUDIO_SESSION_OUTPUT_STAGE session does not affect
        // global effects
        if ((sessionId != AUDIO_SESSION_OUTPUT_MIX) && (sessionId != AUDIO_SESSION_OUTPUT_STAGE)) {
            setEffectSuspended_l(NULL, enabled, AUDIO_SESSION_OUTPUT_MIX);
        }
    }

    sp<EffectChain> chain = getEffectChain_l(sessionId);
    if (chain != 0) {
        chain->checkSuspendOnEffectEnabled(effect, enabled);
    }
}

// ThreadBase::createEffect_l() must be called with AudioFlinger::mLock held
sp<AudioFlinger::EffectHandle> AudioFlinger::ThreadBase::createEffect_l(
        const sp<AudioFlinger::Client>& client,
        const sp<IEffectClient>& effectClient,
        int32_t priority,
        int sessionId,
        effect_descriptor_t *desc,
        int *enabled,
        status_t *status
        )
{
    sp<EffectModule> effect;
    sp<EffectHandle> handle;
    status_t lStatus;
    sp<EffectChain> chain;
    bool chainCreated = false;
    bool effectCreated = false;
    bool effectRegistered = false;

    lStatus = initCheck();
    if (lStatus != NO_ERROR) {
        ALOGW("createEffect_l() Audio driver not initialized.");
        goto Exit;
    }

    // Allow global effects only on offloaded and mixer threads
    if (sessionId == AUDIO_SESSION_OUTPUT_MIX) {
        switch (mType) {
        case MIXER:
        case OFFLOAD:
            break;
        case DIRECT:
        case DUPLICATING:
        case RECORD:
        default:
            ALOGW("createEffect_l() Cannot add global effect %s on thread %s", desc->name, mName);
            lStatus = BAD_VALUE;
            goto Exit;
        }
    }

    // Only Pre processor effects are allowed on input threads and only on input threads
    if ((mType == RECORD) != ((desc->flags & EFFECT_FLAG_TYPE_MASK) == EFFECT_FLAG_TYPE_PRE_PROC)) {
        ALOGW("createEffect_l() effect %s (flags %08x) created on wrong thread type %d",
                desc->name, desc->flags, mType);
        lStatus = BAD_VALUE;
        goto Exit;
    }

    ALOGV("createEffect_l() thread %p effect %s on session %d", this, desc->name, sessionId);

    { // scope for mLock
        Mutex::Autolock _l(mLock);

        // check for existing effect chain with the requested audio session
        chain = getEffectChain_l(sessionId);
        if (chain == 0) {
            // create a new chain for this session
            ALOGV("createEffect_l() new effect chain for session %d", sessionId);
            chain = new EffectChain(this, sessionId);
            addEffectChain_l(chain);
            chain->setStrategy(getStrategyForSession_l(sessionId));
            chainCreated = true;
        } else {
            effect = chain->getEffectFromDesc_l(desc);
        }

        ALOGV("createEffect_l() got effect %p on chain %p", effect.get(), chain.get());

        if (effect == 0) {
            int id = mAudioFlinger->nextUniqueId();
            // Check CPU and memory usage
            lStatus = AudioSystem::registerEffect(desc, mId, chain->strategy(), sessionId, id);
            if (lStatus != NO_ERROR) {
                goto Exit;
            }
            effectRegistered = true;
            // create a new effect module if none present in the chain
            effect = new EffectModule(this, chain, desc, id, sessionId);
            lStatus = effect->status();
            if (lStatus != NO_ERROR) {
                goto Exit;
            }
            effect->setOffloaded(mType == OFFLOAD, mId);

            lStatus = chain->addEffect_l(effect);
            if (lStatus != NO_ERROR) {
                goto Exit;
            }
            effectCreated = true;

            effect->setDevice(mOutDevice);
            effect->setDevice(mInDevice);
            effect->setMode(mAudioFlinger->getMode());
            effect->setAudioSource(mAudioSource);
        }
        // create effect handle and connect it to effect module
        handle = new EffectHandle(effect, client, effectClient, priority);
        lStatus = effect->addHandle(handle.get());
        if (enabled != NULL) {
            *enabled = (int)effect->isEnabled();
        }
    }

Exit:
    if (lStatus != NO_ERROR && lStatus != ALREADY_EXISTS) {
        Mutex::Autolock _l(mLock);
        if (effectCreated) {
            chain->removeEffect_l(effect);
        }
        if (effectRegistered) {
            AudioSystem::unregisterEffect(effect->id());
        }
        if (chainCreated) {
            removeEffectChain_l(chain);
        }
        handle.clear();
    }

    if (status != NULL) {
        *status = lStatus;
    }
    return handle;
}

sp<AudioFlinger::EffectModule> AudioFlinger::ThreadBase::getEffect(int sessionId, int effectId)
{
    Mutex::Autolock _l(mLock);
    return getEffect_l(sessionId, effectId);
}

sp<AudioFlinger::EffectModule> AudioFlinger::ThreadBase::getEffect_l(int sessionId, int effectId)
{
    sp<EffectChain> chain = getEffectChain_l(sessionId);
    return chain != 0 ? chain->getEffectFromId_l(effectId) : 0;
}

// PlaybackThread::addEffect_l() must be called with AudioFlinger::mLock and
// PlaybackThread::mLock held
status_t AudioFlinger::ThreadBase::addEffect_l(const sp<EffectModule>& effect)
{
    // check for existing effect chain with the requested audio session
    int sessionId = effect->sessionId();
    sp<EffectChain> chain = getEffectChain_l(sessionId);
    bool chainCreated = false;

    ALOGD_IF((mType == OFFLOAD) && !effect->isOffloadable(),
             "addEffect_l() on offloaded thread %p: effect %s does not support offload flags %x",
                    this, effect->desc().name, effect->desc().flags);

    if (chain == 0) {
        // create a new chain for this session
        ALOGV("addEffect_l() new effect chain for session %d", sessionId);
        chain = new EffectChain(this, sessionId);
        addEffectChain_l(chain);
        chain->setStrategy(getStrategyForSession_l(sessionId));
        chainCreated = true;
    }
    ALOGV("addEffect_l() %p chain %p effect %p", this, chain.get(), effect.get());

    if (chain->getEffectFromId_l(effect->id()) != 0) {
        ALOGW("addEffect_l() %p effect %s already present in chain %p",
                this, effect->desc().name, chain.get());
        return BAD_VALUE;
    }

    effect->setOffloaded(mType == OFFLOAD, mId);

    status_t status = chain->addEffect_l(effect);
    if (status != NO_ERROR) {
        if (chainCreated) {
            removeEffectChain_l(chain);
        }
        return status;
    }

    effect->setDevice(mOutDevice);
    effect->setDevice(mInDevice);
    effect->setMode(mAudioFlinger->getMode());
    effect->setAudioSource(mAudioSource);
    return NO_ERROR;
}

void AudioFlinger::ThreadBase::removeEffect_l(const sp<EffectModule>& effect) {

    ALOGV("removeEffect_l() %p effect %p", this, effect.get());
    effect_descriptor_t desc = effect->desc();
    if ((desc.flags & EFFECT_FLAG_TYPE_MASK) == EFFECT_FLAG_TYPE_AUXILIARY) {
        detachAuxEffect_l(effect->id());
    }

    sp<EffectChain> chain = effect->chain().promote();
    if (chain != 0) {
        // remove effect chain if removing last effect
        if (chain->removeEffect_l(effect) == 0) {
            removeEffectChain_l(chain);
        }
    } else {
        ALOGW("removeEffect_l() %p cannot promote chain for effect %p", this, effect.get());
    }
}

void AudioFlinger::ThreadBase::lockEffectChains_l(
        Vector< sp<AudioFlinger::EffectChain> >& effectChains)
{
    effectChains = mEffectChains;
    for (size_t i = 0; i < mEffectChains.size(); i++) {
        mEffectChains[i]->lock();
    }
}

void AudioFlinger::ThreadBase::unlockEffectChains(
        const Vector< sp<AudioFlinger::EffectChain> >& effectChains)
{
    for (size_t i = 0; i < effectChains.size(); i++) {
        effectChains[i]->unlock();
    }
}

sp<AudioFlinger::EffectChain> AudioFlinger::ThreadBase::getEffectChain(int sessionId)
{
    Mutex::Autolock _l(mLock);
    return getEffectChain_l(sessionId);
}

sp<AudioFlinger::EffectChain> AudioFlinger::ThreadBase::getEffectChain_l(int sessionId) const
{
    size_t size = mEffectChains.size();
    for (size_t i = 0; i < size; i++) {
        if (mEffectChains[i]->sessionId() == sessionId) {
            return mEffectChains[i];
        }
    }
    return 0;
}

void AudioFlinger::ThreadBase::setMode(audio_mode_t mode)
{
    Mutex::Autolock _l(mLock);
    size_t size = mEffectChains.size();
    for (size_t i = 0; i < size; i++) {
        mEffectChains[i]->setMode_l(mode);
    }
}

void AudioFlinger::ThreadBase::disconnectEffect(const sp<EffectModule>& effect,
                                                    EffectHandle *handle,
                                                    bool unpinIfLast) {

    Mutex::Autolock _l(mLock);
    ALOGV("disconnectEffect() %p effect %p", this, effect.get());
    // delete the effect module if removing last handle on it
    if (effect->removeHandle(handle) == 0) {
        if (!effect->isPinned() || unpinIfLast) {
            removeEffect_l(effect);
            AudioSystem::unregisterEffect(effect->id());
        }
    }
}

// ----------------------------------------------------------------------------
//      Playback
// ----------------------------------------------------------------------------

AudioFlinger::PlaybackThread::PlaybackThread(const sp<AudioFlinger>& audioFlinger,
                                             AudioStreamOut* output,
                                             audio_io_handle_t id,
                                             audio_devices_t device,
                                             type_t type)
    :   ThreadBase(audioFlinger, id, device, AUDIO_DEVICE_NONE, type),
        mNormalFrameCount(0), mMixBuffer(NULL),
        mAllocMixBuffer(NULL), mSuspended(0), mBytesWritten(0),
        mActiveTracksGeneration(0),
        // mStreamTypes[] initialized in constructor body
        mOutput(output),
        mLastWriteTime(0), mNumWrites(0), mNumDelayedWrites(0), mInWrite(false),
        mMixerStatus(MIXER_IDLE),
        mMixerStatusIgnoringFastTracks(MIXER_IDLE),
        standbyDelay(AudioFlinger::mStandbyTimeInNsecs),
        mBytesRemaining(0),
        mCurrentWriteLength(0),
        mUseAsyncWrite(false),
        mWriteAckSequence(0),
        mDrainSequence(0),
        mSignalPending(false),
        mScreenState(AudioFlinger::mScreenState),
        // index 0 is reserved for normal mixer's submix
        mFastTrackAvailMask(((1 << FastMixerState::kMaxFastTracks) - 1) & ~1),
        // mLatchD, mLatchQ,
        mLatchDValid(false), mLatchQValid(false)
{
    snprintf(mName, kNameLength, "AudioOut_%X", id);
    mNBLogWriter = audioFlinger->newWriter_l(kLogSize, mName);

    // Assumes constructor is called by AudioFlinger with it's mLock held, but
    // it would be safer to explicitly pass initial masterVolume/masterMute as
    // parameter.
    //
    // If the HAL we are using has support for master volume or master mute,
    // then do not attenuate or mute during mixing (just leave the volume at 1.0
    // and the mute set to false).
    mMasterVolume = audioFlinger->masterVolume_l();
    mMasterMute = audioFlinger->masterMute_l();
    if (mOutput && mOutput->audioHwDev) {
        if (mOutput->audioHwDev->canSetMasterVolume()) {
            mMasterVolume = 1.0;
        }

        if (mOutput->audioHwDev->canSetMasterMute()) {
            mMasterMute = false;
        }
    }

    readOutputParameters();

    // mStreamTypes[AUDIO_STREAM_CNT] is initialized by stream_type_t default constructor
    // There is no AUDIO_STREAM_MIN, and ++ operator does not compile
    for (audio_stream_type_t stream = (audio_stream_type_t) 0; stream < AUDIO_STREAM_CNT;
            stream = (audio_stream_type_t) (stream + 1)) {
        mStreamTypes[stream].volume = mAudioFlinger->streamVolume_l(stream);
        mStreamTypes[stream].mute = mAudioFlinger->streamMute_l(stream);
    }
    // mStreamTypes[AUDIO_STREAM_CNT] exists but isn't explicitly initialized here,
    // because mAudioFlinger doesn't have one to copy from
}

AudioFlinger::PlaybackThread::~PlaybackThread()
{
    mAudioFlinger->unregisterWriter(mNBLogWriter);
    delete [] mAllocMixBuffer;
}

void AudioFlinger::PlaybackThread::dump(int fd, const Vector<String16>& args)
{
    dumpInternals(fd, args);
    dumpTracks(fd, args);
    dumpEffectChains(fd, args);
}

void AudioFlinger::PlaybackThread::dumpTracks(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    result.appendFormat("Output thread %p stream volumes in dB:\n    ", this);
    for (int i = 0; i < AUDIO_STREAM_CNT; ++i) {
        const stream_type_t *st = &mStreamTypes[i];
        if (i > 0) {
            result.appendFormat(", ");
        }
        result.appendFormat("%d:%.2g", i, 20.0 * log10(st->volume));
        if (st->mute) {
            result.append("M");
        }
    }
    result.append("\n");
    write(fd, result.string(), result.length());
    result.clear();

    snprintf(buffer, SIZE, "Output thread %p tracks\n", this);
    result.append(buffer);
    Track::appendDumpHeader(result);
    for (size_t i = 0; i < mTracks.size(); ++i) {
        sp<Track> track = mTracks[i];
        if (track != 0) {
            track->dump(buffer, SIZE);
            result.append(buffer);
        }
    }

    snprintf(buffer, SIZE, "Output thread %p active tracks\n", this);
    result.append(buffer);
    Track::appendDumpHeader(result);
    for (size_t i = 0; i < mActiveTracks.size(); ++i) {
        sp<Track> track = mActiveTracks[i].promote();
        if (track != 0) {
            track->dump(buffer, SIZE);
            result.append(buffer);
        }
    }
    write(fd, result.string(), result.size());

    // These values are "raw"; they will wrap around.  See prepareTracks_l() for a better way.
    FastTrackUnderruns underruns = getFastTrackUnderruns(0);
    fdprintf(fd, "Normal mixer raw underrun counters: partial=%u empty=%u\n",
            underruns.mBitFields.mPartial, underruns.mBitFields.mEmpty);
}

void AudioFlinger::PlaybackThread::dumpInternals(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    snprintf(buffer, SIZE, "\nOutput thread %p internals\n", this);
    result.append(buffer);
    snprintf(buffer, SIZE, "Normal frame count: %d\n", mNormalFrameCount);
    result.append(buffer);
    snprintf(buffer, SIZE, "last write occurred (msecs): %llu\n",
            ns2ms(systemTime() - mLastWriteTime));
    result.append(buffer);
    snprintf(buffer, SIZE, "total writes: %d\n", mNumWrites);
    result.append(buffer);
    snprintf(buffer, SIZE, "delayed writes: %d\n", mNumDelayedWrites);
    result.append(buffer);
    snprintf(buffer, SIZE, "blocked in write: %d\n", mInWrite);
    result.append(buffer);
    snprintf(buffer, SIZE, "suspend count: %d\n", mSuspended);
    result.append(buffer);
    snprintf(buffer, SIZE, "mix buffer : %p\n", mMixBuffer);
    result.append(buffer);
    write(fd, result.string(), result.size());
    fdprintf(fd, "Fast track availMask=%#x\n", mFastTrackAvailMask);

    dumpBase(fd, args);
}

// Thread virtuals
status_t AudioFlinger::PlaybackThread::readyToRun()
{
    status_t status = initCheck();
    if (status == NO_ERROR) {
        ALOGI("AudioFlinger's thread %p ready to run", this);
    } else {
        ALOGE("No working audio driver found.");
    }
    return status;
}

void AudioFlinger::PlaybackThread::onFirstRef()
{
    run(mName, ANDROID_PRIORITY_URGENT_AUDIO);
}

// ThreadBase virtuals
void AudioFlinger::PlaybackThread::preExit()
{
    ALOGV("  preExit()");
    // FIXME this is using hard-coded strings but in the future, this functionality will be
    //       converted to use audio HAL extensions required to support tunneling
    mOutput->stream->common.set_parameters(&mOutput->stream->common, "exiting=1");
}

// PlaybackThread::createTrack_l() must be called with AudioFlinger::mLock held
sp<AudioFlinger::PlaybackThread::Track> AudioFlinger::PlaybackThread::createTrack_l(
        const sp<AudioFlinger::Client>& client,
        audio_stream_type_t streamType,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask,
        size_t frameCount,
        const sp<IMemory>& sharedBuffer,
        int sessionId,
        IAudioFlinger::track_flags_t *flags,
        pid_t tid,
        int uid,
        status_t *status)
{
    sp<Track> track;
    status_t lStatus;

    bool isTimed = (*flags & IAudioFlinger::TRACK_TIMED) != 0;

    // client expresses a preference for FAST, but we get the final say
    if (*flags & IAudioFlinger::TRACK_FAST) {
      if (
            // not timed
            (!isTimed) &&
            // either of these use cases:
            (
              // use case 1: shared buffer with any frame count
              (
                (sharedBuffer != 0)
              ) ||
              // use case 2: callback handler and frame count is default or at least as large as HAL
              (
                (tid != -1) &&
                ((frameCount == 0) ||
                (frameCount >= (mFrameCount * kFastTrackMultiplier)))
              )
            ) &&
            // PCM data
            audio_is_linear_pcm(format) &&
            // mono or stereo
            ( (channelMask == AUDIO_CHANNEL_OUT_MONO) ||
              (channelMask == AUDIO_CHANNEL_OUT_STEREO) ) &&
#ifndef FAST_TRACKS_AT_NON_NATIVE_SAMPLE_RATE
            // hardware sample rate
            (sampleRate == mSampleRate) &&
#endif
            // normal mixer has an associated fast mixer
            hasFastMixer() &&
            // there are sufficient fast track slots available
            (mFastTrackAvailMask != 0)
            // FIXME test that MixerThread for this fast track has a capable output HAL
            // FIXME add a permission test also?
        ) {
        // if frameCount not specified, then it defaults to fast mixer (HAL) frame count
        if (frameCount == 0) {
            frameCount = mFrameCount * kFastTrackMultiplier;
        }
        ALOGV("AUDIO_OUTPUT_FLAG_FAST accepted: frameCount=%d mFrameCount=%d",
                frameCount, mFrameCount);
      } else {
        ALOGV("AUDIO_OUTPUT_FLAG_FAST denied: isTimed=%d sharedBuffer=%p frameCount=%d "
                "mFrameCount=%d format=%d isLinear=%d channelMask=%#x sampleRate=%u mSampleRate=%u "
                "hasFastMixer=%d tid=%d fastTrackAvailMask=%#x",
                isTimed, sharedBuffer.get(), frameCount, mFrameCount, format,
                audio_is_linear_pcm(format),
                channelMask, sampleRate, mSampleRate, hasFastMixer(), tid, mFastTrackAvailMask);
        *flags &= ~IAudioFlinger::TRACK_FAST;
        // For compatibility with AudioTrack calculation, buffer depth is forced
        // to be at least 2 x the normal mixer frame count and cover audio hardware latency.
        // This is probably too conservative, but legacy application code may depend on it.
        // If you change this calculation, also review the start threshold which is related.
        uint32_t latencyMs = mOutput->stream->get_latency(mOutput->stream);
        uint32_t minBufCount = latencyMs / ((1000 * mNormalFrameCount) / mSampleRate);
        if (minBufCount < 2) {
            minBufCount = 2;
        }
        size_t minFrameCount = mNormalFrameCount * minBufCount;
        if (frameCount < minFrameCount) {
            frameCount = minFrameCount;
        }
      }
    }

    if (mType == DIRECT) {
        if ((format & AUDIO_FORMAT_MAIN_MASK) == AUDIO_FORMAT_PCM) {
            if (sampleRate != mSampleRate || format != mFormat || channelMask != mChannelMask) {
                ALOGE("createTrack_l() Bad parameter: sampleRate %u format %d, channelMask 0x%08x "
                        "for output %p with format %d",
                        sampleRate, format, channelMask, mOutput, mFormat);
                lStatus = BAD_VALUE;
                goto Exit;
            }
        }
    } else if (mType == OFFLOAD) {
        if (sampleRate != mSampleRate || format != mFormat || channelMask != mChannelMask) {
            ALOGE("createTrack_l() Bad parameter: sampleRate %d format %d, channelMask 0x%08x \""
                    "for output %p with format %d",
                    sampleRate, format, channelMask, mOutput, mFormat);
            lStatus = BAD_VALUE;
            goto Exit;
        }
    } else {
        if ((format & AUDIO_FORMAT_MAIN_MASK) != AUDIO_FORMAT_PCM) {
                ALOGE("createTrack_l() Bad parameter: format %d \""
                        "for output %p with format %d",
                        format, mOutput, mFormat);
                lStatus = BAD_VALUE;
                goto Exit;
        }
        // Resampler implementation limits input sampling rate to 2 x output sampling rate.
        if (sampleRate > mSampleRate*2) {
            ALOGE("Sample rate out of range: %u mSampleRate %u", sampleRate, mSampleRate);
            lStatus = BAD_VALUE;
            goto Exit;
        }
    }

    lStatus = initCheck();
    if (lStatus != NO_ERROR) {
        ALOGE("Audio driver not initialized.");
        goto Exit;
    }

    { // scope for mLock
        Mutex::Autolock _l(mLock);

        // all tracks in same audio session must share the same routing strategy otherwise
        // conflicts will happen when tracks are moved from one output to another by audio policy
        // manager
        uint32_t strategy = AudioSystem::getStrategyForStream(streamType);
        for (size_t i = 0; i < mTracks.size(); ++i) {
            sp<Track> t = mTracks[i];
            if (t != 0 && !t->isOutputTrack()) {
                uint32_t actual = AudioSystem::getStrategyForStream(t->streamType());
                if (sessionId == t->sessionId() && strategy != actual) {
                    ALOGE("createTrack_l() mismatched strategy; expected %u but found %u",
                            strategy, actual);
                    lStatus = BAD_VALUE;
                    goto Exit;
                }
            }
        }

        if (!isTimed) {
            track = new Track(this, client, streamType, sampleRate, format,
                    channelMask, frameCount, sharedBuffer, sessionId, uid, *flags);
        } else {
            track = TimedTrack::create(this, client, streamType, sampleRate, format,
                    channelMask, frameCount, sharedBuffer, sessionId, uid);
        }
        if (track == 0 || track->getCblk() == NULL || track->name() < 0) {
            lStatus = NO_MEMORY;
            goto Exit;
        }

        mTracks.add(track);

        sp<EffectChain> chain = getEffectChain_l(sessionId);
        if (chain != 0) {
            ALOGV("createTrack_l() setting main buffer %p", chain->inBuffer());
            track->setMainBuffer(chain->inBuffer());
            chain->setStrategy(AudioSystem::getStrategyForStream(track->streamType()));
            chain->incTrackCnt();
        }

        if ((*flags & IAudioFlinger::TRACK_FAST) && (tid != -1)) {
            pid_t callingPid = IPCThreadState::self()->getCallingPid();
            // we don't have CAP_SYS_NICE, nor do we want to have it as it's too powerful,
            // so ask activity manager to do this on our behalf
            sendPrioConfigEvent_l(callingPid, tid, kPriorityAudioApp);
        }
    }

    lStatus = NO_ERROR;

Exit:
    if (status) {
        *status = lStatus;
    }
    return track;
}

uint32_t AudioFlinger::PlaybackThread::correctLatency_l(uint32_t latency) const
{
    return latency;
}

uint32_t AudioFlinger::PlaybackThread::latency() const
{
    Mutex::Autolock _l(mLock);
    return latency_l();
}
uint32_t AudioFlinger::PlaybackThread::latency_l() const
{
    if (initCheck() == NO_ERROR) {
        return correctLatency_l(mOutput->stream->get_latency(mOutput->stream));
    } else {
        return 0;
    }
}

void AudioFlinger::PlaybackThread::setMasterVolume(float value)
{
    Mutex::Autolock _l(mLock);
    // Don't apply master volume in SW if our HAL can do it for us.
    if (mOutput && mOutput->audioHwDev &&
        mOutput->audioHwDev->canSetMasterVolume()) {
        mMasterVolume = 1.0;
    } else {
        mMasterVolume = value;
    }
}

void AudioFlinger::PlaybackThread::setMasterMute(bool muted)
{
    Mutex::Autolock _l(mLock);
    // Don't apply master mute in SW if our HAL can do it for us.
    if (mOutput && mOutput->audioHwDev &&
        mOutput->audioHwDev->canSetMasterMute()) {
        mMasterMute = false;
    } else {
        mMasterMute = muted;
    }
}

void AudioFlinger::PlaybackThread::setStreamVolume(audio_stream_type_t stream, float value)
{
    Mutex::Autolock _l(mLock);
    mStreamTypes[stream].volume = value;
    broadcast_l();
}

void AudioFlinger::PlaybackThread::setStreamMute(audio_stream_type_t stream, bool muted)
{
    Mutex::Autolock _l(mLock);
    mStreamTypes[stream].mute = muted;
    broadcast_l();
}

float AudioFlinger::PlaybackThread::streamVolume(audio_stream_type_t stream) const
{
    Mutex::Autolock _l(mLock);
    return mStreamTypes[stream].volume;
}

// addTrack_l() must be called with ThreadBase::mLock held
status_t AudioFlinger::PlaybackThread::addTrack_l(const sp<Track>& track)
{
    status_t status = ALREADY_EXISTS;

    // set retry count for buffer fill
    track->mRetryCount = kMaxTrackStartupRetries;
    if (mActiveTracks.indexOf(track) < 0) {
        // the track is newly added, make sure it fills up all its
        // buffers before playing. This is to ensure the client will
        // effectively get the latency it requested.
        if (!track->isOutputTrack()) {
            TrackBase::track_state state = track->mState;
            mLock.unlock();
            status = AudioSystem::startOutput(mId, track->streamType(), track->sessionId());
            mLock.lock();
            // abort track was stopped/paused while we released the lock
            if (state != track->mState) {
                if (status == NO_ERROR) {
                    mLock.unlock();
                    AudioSystem::stopOutput(mId, track->streamType(), track->sessionId());
                    mLock.lock();
                }
                return INVALID_OPERATION;
            }
            // abort if start is rejected by audio policy manager
            if (status != NO_ERROR) {
                return PERMISSION_DENIED;
            }
#ifdef ADD_BATTERY_DATA
            // to track the speaker usage
            addBatteryData(IMediaPlayerService::kBatteryDataAudioFlingerStart);
#endif
        }

        track->mFillingUpStatus = track->sharedBuffer() != 0 ? Track::FS_FILLED : Track::FS_FILLING;
        track->mResetDone = false;
        track->mPresentationCompleteFrames = 0;
        mActiveTracks.add(track);
        mWakeLockUids.add(track->uid());
        mActiveTracksGeneration++;
        mLatestActiveTrack = track;
        sp<EffectChain> chain = getEffectChain_l(track->sessionId());
        if (chain != 0) {
            ALOGV("addTrack_l() starting track on chain %p for session %d", chain.get(),
                    track->sessionId());
            chain->incActiveTrackCnt();
        }

        status = NO_ERROR;
    }

    ALOGV("signal playback thread");
    broadcast_l();

    return status;
}

bool AudioFlinger::PlaybackThread::destroyTrack_l(const sp<Track>& track)
{
    track->terminate();
    // active tracks are removed by threadLoop()
    bool trackActive = (mActiveTracks.indexOf(track) >= 0);
    track->mState = TrackBase::STOPPED;
    if (!trackActive) {
        removeTrack_l(track);
    } else if (track->isFastTrack() || track->isOffloaded()) {
        track->mState = TrackBase::STOPPING_1;
    }

    return trackActive;
}

void AudioFlinger::PlaybackThread::removeTrack_l(const sp<Track>& track)
{
    track->triggerEvents(AudioSystem::SYNC_EVENT_PRESENTATION_COMPLETE);
    mTracks.remove(track);
    deleteTrackName_l(track->name());
    // redundant as track is about to be destroyed, for dumpsys only
    track->mName = -1;
    if (track->isFastTrack()) {
        int index = track->mFastIndex;
        ALOG_ASSERT(0 < index && index < (int)FastMixerState::kMaxFastTracks);
        ALOG_ASSERT(!(mFastTrackAvailMask & (1 << index)));
        mFastTrackAvailMask |= 1 << index;
        // redundant as track is about to be destroyed, for dumpsys only
        track->mFastIndex = -1;
    }
    sp<EffectChain> chain = getEffectChain_l(track->sessionId());
    if (chain != 0) {
        chain->decTrackCnt();
    }
}

void AudioFlinger::PlaybackThread::broadcast_l()
{
    // Thread could be blocked waiting for async
    // so signal it to handle state changes immediately
    // If threadLoop is currently unlocked a signal of mWaitWorkCV will
    // be lost so we also flag to prevent it blocking on mWaitWorkCV
    mSignalPending = true;
    mWaitWorkCV.broadcast();
}

String8 AudioFlinger::PlaybackThread::getParameters(const String8& keys)
{
    Mutex::Autolock _l(mLock);
    if (initCheck() != NO_ERROR) {
        return String8();
    }

    char *s = mOutput->stream->common.get_parameters(&mOutput->stream->common, keys.string());
    const String8 out_s8(s);
    free(s);
    return out_s8;
}

// audioConfigChanged_l() must be called with AudioFlinger::mLock held
void AudioFlinger::PlaybackThread::audioConfigChanged_l(int event, int param) {
    AudioSystem::OutputDescriptor desc;
    void *param2 = NULL;

    ALOGV("PlaybackThread::audioConfigChanged_l, thread %p, event %d, param %d", this, event,
            param);

    switch (event) {
    case AudioSystem::OUTPUT_OPENED:
    case AudioSystem::OUTPUT_CONFIG_CHANGED:
        desc.channelMask = mChannelMask;
        desc.samplingRate = mSampleRate;
        desc.format = mFormat;
        desc.frameCount = mNormalFrameCount; // FIXME see
                                             // AudioFlinger::frameCount(audio_io_handle_t)
        desc.latency = latency();
        param2 = &desc;
        break;

    case AudioSystem::STREAM_CONFIG_CHANGED:
        param2 = &param;
    case AudioSystem::OUTPUT_CLOSED:
    default:
        break;
    }
    mAudioFlinger->audioConfigChanged_l(event, mId, param2);
}

void AudioFlinger::PlaybackThread::writeCallback()
{
    ALOG_ASSERT(mCallbackThread != 0);
    mCallbackThread->resetWriteBlocked();
}

void AudioFlinger::PlaybackThread::drainCallback()
{
    ALOG_ASSERT(mCallbackThread != 0);
    mCallbackThread->resetDraining();
}

void AudioFlinger::PlaybackThread::resetWriteBlocked(uint32_t sequence)
{
    Mutex::Autolock _l(mLock);
    // reject out of sequence requests
    if ((mWriteAckSequence & 1) && (sequence == mWriteAckSequence)) {
        mWriteAckSequence &= ~1;
        mWaitWorkCV.signal();
    }
}

void AudioFlinger::PlaybackThread::resetDraining(uint32_t sequence)
{
    Mutex::Autolock _l(mLock);
    // reject out of sequence requests
    if ((mDrainSequence & 1) && (sequence == mDrainSequence)) {
        mDrainSequence &= ~1;
        mWaitWorkCV.signal();
    }
}

// static
int AudioFlinger::PlaybackThread::asyncCallback(stream_callback_event_t event,
                                                void *param,
                                                void *cookie)
{
    AudioFlinger::PlaybackThread *me = (AudioFlinger::PlaybackThread *)cookie;
    ALOGV("asyncCallback() event %d", event);
    switch (event) {
    case STREAM_CBK_EVENT_WRITE_READY:
        me->writeCallback();
        break;
    case STREAM_CBK_EVENT_DRAIN_READY:
        me->drainCallback();
        break;
    default:
        ALOGW("asyncCallback() unknown event %d", event);
        break;
    }
    return 0;
}

void AudioFlinger::PlaybackThread::readOutputParameters()
{
    // unfortunately we have no way of recovering from errors here, hence the LOG_FATAL
    mSampleRate = mOutput->stream->common.get_sample_rate(&mOutput->stream->common);
    mChannelMask = mOutput->stream->common.get_channels(&mOutput->stream->common);
    if (!audio_is_output_channel(mChannelMask)) {
        LOG_FATAL("HAL channel mask %#x not valid for output", mChannelMask);
    }
    if ((mType == MIXER || mType == DUPLICATING) && mChannelMask != AUDIO_CHANNEL_OUT_STEREO) {
        LOG_FATAL("HAL channel mask %#x not supported for mixed output; "
                "must be AUDIO_CHANNEL_OUT_STEREO", mChannelMask);
    }
    mChannelCount = popcount(mChannelMask);
    mFormat = mOutput->stream->common.get_format(&mOutput->stream->common);
    if (!audio_is_valid_format(mFormat)) {
        LOG_FATAL("HAL format %d not valid for output", mFormat);
    }
    if ((mType == MIXER || mType == DUPLICATING) && mFormat != AUDIO_FORMAT_PCM_16_BIT) {
        LOG_FATAL("HAL format %d not supported for mixed output; must be AUDIO_FORMAT_PCM_16_BIT",
                mFormat);
    }
    mFrameSize = audio_stream_frame_size(&mOutput->stream->common);
    mFrameCount = mOutput->stream->common.get_buffer_size(&mOutput->stream->common) / mFrameSize;
    if (mFrameCount & 15) {
        ALOGW("HAL output buffer size is %u frames but AudioMixer requires multiples of 16 frames",
                mFrameCount);
    }

    if ((mOutput->flags & AUDIO_OUTPUT_FLAG_NON_BLOCKING) &&
            (mOutput->stream->set_callback != NULL)) {
        if (mOutput->stream->set_callback(mOutput->stream,
                                      AudioFlinger::PlaybackThread::asyncCallback, this) == 0) {
            mUseAsyncWrite = true;
            mCallbackThread = new AudioFlinger::AsyncCallbackThread(this);
        }
    }

    // Calculate size of normal mix buffer relative to the HAL output buffer size
    double multiplier = 1.0;
    if (mType == MIXER && (kUseFastMixer == FastMixer_Static ||
            kUseFastMixer == FastMixer_Dynamic)) {
        size_t minNormalFrameCount = (kMinNormalMixBufferSizeMs * mSampleRate) / 1000;
        size_t maxNormalFrameCount = (kMaxNormalMixBufferSizeMs * mSampleRate) / 1000;
        // round up minimum and round down maximum to nearest 16 frames to satisfy AudioMixer
        minNormalFrameCount = (minNormalFrameCount + 15) & ~15;
        maxNormalFrameCount = maxNormalFrameCount & ~15;
        if (maxNormalFrameCount < minNormalFrameCount) {
            maxNormalFrameCount = minNormalFrameCount;
        }
        multiplier = (double) minNormalFrameCount / (double) mFrameCount;
        if (multiplier <= 1.0) {
            multiplier = 1.0;
        } else if (multiplier <= 2.0) {
            if (2 * mFrameCount <= maxNormalFrameCount) {
                multiplier = 2.0;
            } else {
                multiplier = (double) maxNormalFrameCount / (double) mFrameCount;
            }
        } else {
            // prefer an even multiplier, for compatibility with doubling of fast tracks due to HAL
            // SRC (it would be unusual for the normal mix buffer size to not be a multiple of fast
            // track, but we sometimes have to do this to satisfy the maximum frame count
            // constraint)
            // FIXME this rounding up should not be done if no HAL SRC
            uint32_t truncMult = (uint32_t) multiplier;
            if ((truncMult & 1)) {
                if ((truncMult + 1) * mFrameCount <= maxNormalFrameCount) {
                    ++truncMult;
                }
            }
            multiplier = (double) truncMult;
        }
    }
    mNormalFrameCount = multiplier * mFrameCount;
    // round up to nearest 16 frames to satisfy AudioMixer
    mNormalFrameCount = (mNormalFrameCount + 15) & ~15;
    ALOGI("HAL output buffer size %u frames, normal mix buffer size %u frames", mFrameCount,
            mNormalFrameCount);

    delete[] mAllocMixBuffer;
    size_t align = (mFrameSize < sizeof(int16_t)) ? sizeof(int16_t) : mFrameSize;
    mAllocMixBuffer = new int8_t[mNormalFrameCount * mFrameSize + align - 1];
    mMixBuffer = (int16_t *) ((((size_t)mAllocMixBuffer + align - 1) / align) * align);
    memset(mMixBuffer, 0, mNormalFrameCount * mFrameSize);

    // force reconfiguration of effect chains and engines to take new buffer size and audio
    // parameters into account
    // Note that mLock is not held when readOutputParameters() is called from the constructor
    // but in this case nothing is done below as no audio sessions have effect yet so it doesn't
    // matter.
    // create a copy of mEffectChains as calling moveEffectChain_l() can reorder some effect chains
    Vector< sp<EffectChain> > effectChains = mEffectChains;
    for (size_t i = 0; i < effectChains.size(); i ++) {
        mAudioFlinger->moveEffectChain_l(effectChains[i]->sessionId(), this, this, false);
    }
}


status_t AudioFlinger::PlaybackThread::getRenderPosition(size_t *halFrames, size_t *dspFrames)
{
    if (halFrames == NULL || dspFrames == NULL) {
        return BAD_VALUE;
    }
    Mutex::Autolock _l(mLock);
    if (initCheck() != NO_ERROR) {
        return INVALID_OPERATION;
    }
    size_t framesWritten = mBytesWritten / mFrameSize;
    *halFrames = framesWritten;

    if (isSuspended()) {
        // return an estimation of rendered frames when the output is suspended
        size_t latencyFrames = (latency_l() * mSampleRate) / 1000;
        *dspFrames = framesWritten >= latencyFrames ? framesWritten - latencyFrames : 0;
        return NO_ERROR;
    } else {
        return mOutput->stream->get_render_position(mOutput->stream, dspFrames);
    }
}

uint32_t AudioFlinger::PlaybackThread::hasAudioSession(int sessionId) const
{
    Mutex::Autolock _l(mLock);
    uint32_t result = 0;
    if (getEffectChain_l(sessionId) != 0) {
        result = EFFECT_SESSION;
    }

    for (size_t i = 0; i < mTracks.size(); ++i) {
        sp<Track> track = mTracks[i];
        if (sessionId == track->sessionId() && !track->isInvalid()) {
            result |= TRACK_SESSION;
            break;
        }
    }

    return result;
}

uint32_t AudioFlinger::PlaybackThread::getStrategyForSession_l(int sessionId)
{
    // session AUDIO_SESSION_OUTPUT_MIX is placed in same strategy as MUSIC stream so that
    // it is moved to correct output by audio policy manager when A2DP is connected or disconnected
    if (sessionId == AUDIO_SESSION_OUTPUT_MIX) {
        return AudioSystem::getStrategyForStream(AUDIO_STREAM_MUSIC);
    }
    for (size_t i = 0; i < mTracks.size(); i++) {
        sp<Track> track = mTracks[i];
        if (sessionId == track->sessionId() && !track->isInvalid()) {
            return AudioSystem::getStrategyForStream(track->streamType());
        }
    }
    return AudioSystem::getStrategyForStream(AUDIO_STREAM_MUSIC);
}


AudioFlinger::AudioStreamOut* AudioFlinger::PlaybackThread::getOutput() const
{
    Mutex::Autolock _l(mLock);
    return mOutput;
}

AudioFlinger::AudioStreamOut* AudioFlinger::PlaybackThread::clearOutput()
{
    Mutex::Autolock _l(mLock);
    AudioStreamOut *output = mOutput;
    mOutput = NULL;
    // FIXME FastMixer might also have a raw ptr to mOutputSink;
    //       must push a NULL and wait for ack
    mOutputSink.clear();
    mPipeSink.clear();
    mNormalSink.clear();
    return output;
}

// this method must always be called either with ThreadBase mLock held or inside the thread loop
audio_stream_t* AudioFlinger::PlaybackThread::stream() const
{
    if (mOutput == NULL) {
        return NULL;
    }
    return &mOutput->stream->common;
}

uint32_t AudioFlinger::PlaybackThread::activeSleepTimeUs() const
{
    return (uint32_t)((uint32_t)((mNormalFrameCount * 1000) / mSampleRate) * 1000);
}

status_t AudioFlinger::PlaybackThread::setSyncEvent(const sp<SyncEvent>& event)
{
    if (!isValidSyncEvent(event)) {
        return BAD_VALUE;
    }

    Mutex::Autolock _l(mLock);

    for (size_t i = 0; i < mTracks.size(); ++i) {
        sp<Track> track = mTracks[i];
        if (event->triggerSession() == track->sessionId()) {
            (void) track->setSyncEvent(event);
            return NO_ERROR;
        }
    }

    return NAME_NOT_FOUND;
}

bool AudioFlinger::PlaybackThread::isValidSyncEvent(const sp<SyncEvent>& event) const
{
    return event->type() == AudioSystem::SYNC_EVENT_PRESENTATION_COMPLETE;
}

void AudioFlinger::PlaybackThread::threadLoop_removeTracks(
        const Vector< sp<Track> >& tracksToRemove)
{
    size_t count = tracksToRemove.size();
    if (count) {
        for (size_t i = 0 ; i < count ; i++) {
            const sp<Track>& track = tracksToRemove.itemAt(i);
            if (!track->isOutputTrack()) {
                AudioSystem::stopOutput(mId, track->streamType(), track->sessionId());
#ifdef ADD_BATTERY_DATA
                // to track the speaker usage
                addBatteryData(IMediaPlayerService::kBatteryDataAudioFlingerStop);
#endif
                if (track->isTerminated()) {
                    AudioSystem::releaseOutput(mId);
                }
            }
        }
    }
}

void AudioFlinger::PlaybackThread::checkSilentMode_l()
{
    if (!mMasterMute) {
        char value[PROPERTY_VALUE_MAX];
        if (property_get("ro.audio.silent", value, "0") > 0) {
            char *endptr;
            unsigned long ul = strtoul(value, &endptr, 0);
            if (*endptr == '\0' && ul != 0) {
                ALOGD("Silence is golden");
                // The setprop command will not allow a property to be changed after
                // the first time it is set, so we don't have to worry about un-muting.
                setMasterMute_l(true);
            }
        }
    }
}

// shared by MIXER and DIRECT, overridden by DUPLICATING
ssize_t AudioFlinger::PlaybackThread::threadLoop_write()
{
    // FIXME rewrite to reduce number of system calls
    mLastWriteTime = systemTime();
    mInWrite = true;
    ssize_t bytesWritten;

    // If an NBAIO sink is present, use it to write the normal mixer's submix
    if (mNormalSink != 0) {
#define mBitShift 2 // FIXME
        size_t count = mBytesRemaining >> mBitShift;
        size_t offset = (mCurrentWriteLength - mBytesRemaining) >> 1;
        ATRACE_BEGIN("write");
        // update the setpoint when AudioFlinger::mScreenState changes
        uint32_t screenState = AudioFlinger::mScreenState;
        if (screenState != mScreenState) {
            mScreenState = screenState;
            MonoPipe *pipe = (MonoPipe *)mPipeSink.get();
            if (pipe != NULL) {
                pipe->setAvgFrames((mScreenState & 1) ?
                        (pipe->maxFrames() * 7) / 8 : mNormalFrameCount * 2);
            }
        }
        ssize_t framesWritten = mNormalSink->write(mMixBuffer + offset, count);
        ATRACE_END();
        if (framesWritten > 0) {
            bytesWritten = framesWritten << mBitShift;
        } else {
            bytesWritten = framesWritten;
        }
        status_t status = mNormalSink->getTimestamp(mLatchD.mTimestamp);
        if (status == NO_ERROR) {
            size_t totalFramesWritten = mNormalSink->framesWritten();
            if (totalFramesWritten >= mLatchD.mTimestamp.mPosition) {
                mLatchD.mUnpresentedFrames = totalFramesWritten - mLatchD.mTimestamp.mPosition;
                mLatchDValid = true;
            }
        }
    // otherwise use the HAL / AudioStreamOut directly
    } else {
        // Direct output and offload threads
        size_t offset = (mCurrentWriteLength - mBytesRemaining) / sizeof(int16_t);
        if (mUseAsyncWrite) {
            ALOGW_IF(mWriteAckSequence & 1, "threadLoop_write(): out of sequence write request");
            mWriteAckSequence += 2;
            mWriteAckSequence |= 1;
            ALOG_ASSERT(mCallbackThread != 0);
            mCallbackThread->setWriteBlocked(mWriteAckSequence);
        }
        // FIXME We should have an implementation of timestamps for direct output threads.
        // They are used e.g for multichannel PCM playback over HDMI.
        bytesWritten = mOutput->stream->write(mOutput->stream,
                                                   mMixBuffer + offset, mBytesRemaining);
        if (mUseAsyncWrite &&
                ((bytesWritten < 0) || (bytesWritten == (ssize_t)mBytesRemaining))) {
            // do not wait for async callback in case of error of full write
            mWriteAckSequence &= ~1;
            ALOG_ASSERT(mCallbackThread != 0);
            mCallbackThread->setWriteBlocked(mWriteAckSequence);
        }
    }

    mNumWrites++;
    mInWrite = false;
    mStandby = false;
    return bytesWritten;
}

void AudioFlinger::PlaybackThread::threadLoop_drain()
{
    if (mOutput->stream->drain) {
        ALOGV("draining %s", (mMixerStatus == MIXER_DRAIN_TRACK) ? "early" : "full");
        if (mUseAsyncWrite) {
            ALOGW_IF(mDrainSequence & 1, "threadLoop_drain(): out of sequence drain request");
            mDrainSequence |= 1;
            ALOG_ASSERT(mCallbackThread != 0);
            mCallbackThread->setDraining(mDrainSequence);
        }
        mOutput->stream->drain(mOutput->stream,
            (mMixerStatus == MIXER_DRAIN_TRACK) ? AUDIO_DRAIN_EARLY_NOTIFY
                                                : AUDIO_DRAIN_ALL);
    }
}

void AudioFlinger::PlaybackThread::threadLoop_exit()
{
    // Default implementation has nothing to do
}

/*
The derived values that are cached:
 - mixBufferSize from frame count * frame size
 - activeSleepTime from activeSleepTimeUs()
 - idleSleepTime from idleSleepTimeUs()
 - standbyDelay from mActiveSleepTimeUs (DIRECT only)
 - maxPeriod from frame count and sample rate (MIXER only)

The parameters that affect these derived values are:
 - frame count
 - frame size
 - sample rate
 - device type: A2DP or not
 - device latency
 - format: PCM or not
 - active sleep time
 - idle sleep time
*/

void AudioFlinger::PlaybackThread::cacheParameters_l()
{
    mixBufferSize = mNormalFrameCount * mFrameSize;
    activeSleepTime = activeSleepTimeUs();
    idleSleepTime = idleSleepTimeUs();
}

void AudioFlinger::PlaybackThread::invalidateTracks(audio_stream_type_t streamType)
{
    ALOGV("MixerThread::invalidateTracks() mixer %p, streamType %d, mTracks.size %d",
            this,  streamType, mTracks.size());
    Mutex::Autolock _l(mLock);

    size_t size = mTracks.size();
    for (size_t i = 0; i < size; i++) {
        sp<Track> t = mTracks[i];
        if (t->streamType() == streamType) {
            t->invalidate();
        }
    }
}

status_t AudioFlinger::PlaybackThread::addEffectChain_l(const sp<EffectChain>& chain)
{
    int session = chain->sessionId();
    int16_t *buffer = mMixBuffer;
    bool ownsBuffer = false;

    ALOGV("addEffectChain_l() %p on thread %p for session %d", chain.get(), this, session);
    if (session > 0) {
        // Only one effect chain can be present in direct output thread and it uses
        // the mix buffer as input
        if (mType != DIRECT) {
            size_t numSamples = mNormalFrameCount * mChannelCount;
            buffer = new int16_t[numSamples];
            memset(buffer, 0, numSamples * sizeof(int16_t));
            ALOGV("addEffectChain_l() creating new input buffer %p session %d", buffer, session);
            ownsBuffer = true;
        }

        // Attach all tracks with same session ID to this chain.
        for (size_t i = 0; i < mTracks.size(); ++i) {
            sp<Track> track = mTracks[i];
            if (session == track->sessionId()) {
                ALOGV("addEffectChain_l() track->setMainBuffer track %p buffer %p", track.get(),
                        buffer);
                track->setMainBuffer(buffer);
                chain->incTrackCnt();
            }
        }

        // indicate all active tracks in the chain
        for (size_t i = 0 ; i < mActiveTracks.size() ; ++i) {
            sp<Track> track = mActiveTracks[i].promote();
            if (track == 0) {
                continue;
            }
            if (session == track->sessionId()) {
                ALOGV("addEffectChain_l() activating track %p on session %d", track.get(), session);
                chain->incActiveTrackCnt();
            }
        }
    }

    chain->setInBuffer(buffer, ownsBuffer);
    chain->setOutBuffer(mMixBuffer);
    // Effect chain for session AUDIO_SESSION_OUTPUT_STAGE is inserted at end of effect
    // chains list in order to be processed last as it contains output stage effects
    // Effect chain for session AUDIO_SESSION_OUTPUT_MIX is inserted before
    // session AUDIO_SESSION_OUTPUT_STAGE to be processed
    // after track specific effects and before output stage
    // It is therefore mandatory that AUDIO_SESSION_OUTPUT_MIX == 0 and
    // that AUDIO_SESSION_OUTPUT_STAGE < AUDIO_SESSION_OUTPUT_MIX
    // Effect chain for other sessions are inserted at beginning of effect
    // chains list to be processed before output mix effects. Relative order between other
    // sessions is not important
    size_t size = mEffectChains.size();
    size_t i = 0;
    for (i = 0; i < size; i++) {
        if (mEffectChains[i]->sessionId() < session) {
            break;
        }
    }
    mEffectChains.insertAt(chain, i);
    checkSuspendOnAddEffectChain_l(chain);

    return NO_ERROR;
}

size_t AudioFlinger::PlaybackThread::removeEffectChain_l(const sp<EffectChain>& chain)
{
    int session = chain->sessionId();

    ALOGV("removeEffectChain_l() %p from thread %p for session %d", chain.get(), this, session);

    for (size_t i = 0; i < mEffectChains.size(); i++) {
        if (chain == mEffectChains[i]) {
            mEffectChains.removeAt(i);
            // detach all active tracks from the chain
            for (size_t i = 0 ; i < mActiveTracks.size() ; ++i) {
                sp<Track> track = mActiveTracks[i].promote();
                if (track == 0) {
                    continue;
                }
                if (session == track->sessionId()) {
                    ALOGV("removeEffectChain_l(): stopping track on chain %p for session Id: %d",
                            chain.get(), session);
                    chain->decActiveTrackCnt();
                }
            }

            // detach all tracks with same session ID from this chain
            for (size_t i = 0; i < mTracks.size(); ++i) {
                sp<Track> track = mTracks[i];
                if (session == track->sessionId()) {
                    track->setMainBuffer(mMixBuffer);
                    chain->decTrackCnt();
                }
            }
            break;
        }
    }
    return mEffectChains.size();
}

status_t AudioFlinger::PlaybackThread::attachAuxEffect(
        const sp<AudioFlinger::PlaybackThread::Track> track, int EffectId)
{
    Mutex::Autolock _l(mLock);
    return attachAuxEffect_l(track, EffectId);
}

status_t AudioFlinger::PlaybackThread::attachAuxEffect_l(
        const sp<AudioFlinger::PlaybackThread::Track> track, int EffectId)
{
    status_t status = NO_ERROR;

    if (EffectId == 0) {
        track->setAuxBuffer(0, NULL);
    } else {
        // Auxiliary effects are always in audio session AUDIO_SESSION_OUTPUT_MIX
        sp<EffectModule> effect = getEffect_l(AUDIO_SESSION_OUTPUT_MIX, EffectId);
        if (effect != 0) {
            if ((effect->desc().flags & EFFECT_FLAG_TYPE_MASK) == EFFECT_FLAG_TYPE_AUXILIARY) {
                track->setAuxBuffer(EffectId, (int32_t *)effect->inBuffer());
            } else {
                status = INVALID_OPERATION;
            }
        } else {
            status = BAD_VALUE;
        }
    }
    return status;
}

void AudioFlinger::PlaybackThread::detachAuxEffect_l(int effectId)
{
    for (size_t i = 0; i < mTracks.size(); ++i) {
        sp<Track> track = mTracks[i];
        if (track->auxEffectId() == effectId) {
            attachAuxEffect_l(track, 0);
        }
    }
}

bool AudioFlinger::PlaybackThread::threadLoop()
{
    Vector< sp<Track> > tracksToRemove;

    standbyTime = systemTime();

    // MIXER
    nsecs_t lastWarning = 0;

    // DUPLICATING
    // FIXME could this be made local to while loop?
    writeFrames = 0;

    int lastGeneration = 0;

    cacheParameters_l();
    sleepTime = idleSleepTime;

    if (mType == MIXER) {
        sleepTimeShift = 0;
    }

    CpuStats cpuStats;
    const String8 myName(String8::format("thread %p type %d TID %d", this, mType, gettid()));

    acquireWakeLock();

    // mNBLogWriter->log can only be called while thread mutex mLock is held.
    // So if you need to log when mutex is unlocked, set logString to a non-NULL string,
    // and then that string will be logged at the next convenient opportunity.
    const char *logString = NULL;

    checkSilentMode_l();

    while (!exitPending())
    {
        cpuStats.sample(myName);

        Vector< sp<EffectChain> > effectChains;

        processConfigEvents();

        { // scope for mLock

            Mutex::Autolock _l(mLock);

            if (logString != NULL) {
                mNBLogWriter->logTimestamp();
                mNBLogWriter->log(logString);
                logString = NULL;
            }

            if (mLatchDValid) {
                mLatchQ = mLatchD;
                mLatchDValid = false;
                mLatchQValid = true;
            }

            if (checkForNewParameters_l()) {
                cacheParameters_l();
            }

            saveOutputTracks();
            if (mSignalPending) {
                // A signal was raised while we were unlocked
                mSignalPending = false;
            } else if (waitingAsyncCallback_l()) {
                if (exitPending()) {
                    break;
                }
                releaseWakeLock_l();
                mWakeLockUids.clear();
                mActiveTracksGeneration++;
                ALOGV("wait async completion");
                mWaitWorkCV.wait(mLock);
                ALOGV("async completion/wake");
                acquireWakeLock_l();
                standbyTime = systemTime() + standbyDelay;
                sleepTime = 0;

                continue;
            }
            if ((!mActiveTracks.size() && systemTime() > standbyTime) ||
                                   isSuspended()) {
                // put audio hardware into standby after short delay
                if (shouldStandby_l()) {

                    threadLoop_standby();

                    mStandby = true;
                }

                if (!mActiveTracks.size() && mConfigEvents.isEmpty()) {
                    // we're about to wait, flush the binder command buffer
                    IPCThreadState::self()->flushCommands();

                    clearOutputTracks();

                    if (exitPending()) {
                        break;
                    }

                    releaseWakeLock_l();
                    mWakeLockUids.clear();
                    mActiveTracksGeneration++;
                    // wait until we have something to do...
                    ALOGV("%s going to sleep", myName.string());
                    mWaitWorkCV.wait(mLock);
                    ALOGV("%s waking up", myName.string());
                    acquireWakeLock_l();

                    mMixerStatus = MIXER_IDLE;
                    mMixerStatusIgnoringFastTracks = MIXER_IDLE;
                    mBytesWritten = 0;
                    mBytesRemaining = 0;
                    checkSilentMode_l();

                    standbyTime = systemTime() + standbyDelay;
                    sleepTime = idleSleepTime;
                    if (mType == MIXER) {
                        sleepTimeShift = 0;
                    }

                    continue;
                }
            }
            // mMixerStatusIgnoringFastTracks is also updated internally
            mMixerStatus = prepareTracks_l(&tracksToRemove);

            // compare with previously applied list
            if (lastGeneration != mActiveTracksGeneration) {
                // update wakelock
                updateWakeLockUids_l(mWakeLockUids);
                lastGeneration = mActiveTracksGeneration;
            }

            // prevent any changes in effect chain list and in each effect chain
            // during mixing and effect process as the audio buffers could be deleted
            // or modified if an effect is created or deleted
            lockEffectChains_l(effectChains);
        } // mLock scope ends

        if (mBytesRemaining == 0) {
            mCurrentWriteLength = 0;
            if (mMixerStatus == MIXER_TRACKS_READY) {
                // threadLoop_mix() sets mCurrentWriteLength
                threadLoop_mix();
            } else if ((mMixerStatus != MIXER_DRAIN_TRACK)
                        && (mMixerStatus != MIXER_DRAIN_ALL)) {
                // threadLoop_sleepTime sets sleepTime to 0 if data
                // must be written to HAL
                threadLoop_sleepTime();
                if (sleepTime == 0) {
                    mCurrentWriteLength = mixBufferSize;
                }
            }
            mBytesRemaining = mCurrentWriteLength;
            if (isSuspended()) {
                sleepTime = suspendSleepTimeUs();
                // simulate write to HAL when suspended
                mBytesWritten += mixBufferSize;
                mBytesRemaining = 0;
            }

            // only process effects if we're going to write
            if (sleepTime == 0 && mType != OFFLOAD) {
                for (size_t i = 0; i < effectChains.size(); i ++) {
                    effectChains[i]->process_l();
                }
            }
        }
        // Process effect chains for offloaded thread even if no audio
        // was read from audio track: process only updates effect state
        // and thus does have to be synchronized with audio writes but may have
        // to be called while waiting for async write callback
        if (mType == OFFLOAD) {
            for (size_t i = 0; i < effectChains.size(); i ++) {
                effectChains[i]->process_l();
            }
        }

        // enable changes in effect chain
        unlockEffectChains(effectChains);

        if (!waitingAsyncCallback()) {
            // sleepTime == 0 means we must write to audio hardware
            if (sleepTime == 0) {
                if (mBytesRemaining) {
                    ssize_t ret = threadLoop_write();
                    if (ret < 0) {
                        mBytesRemaining = 0;
                    } else {
                        mBytesWritten += ret;
                        mBytesRemaining -= ret;
                    }
                } else if ((mMixerStatus == MIXER_DRAIN_TRACK) ||
                        (mMixerStatus == MIXER_DRAIN_ALL)) {
                    threadLoop_drain();
                }
if (mType == MIXER) {
                // write blocked detection
                nsecs_t now = systemTime();
                nsecs_t delta = now - mLastWriteTime;
                if (!mStandby && delta > maxPeriod) {
                    mNumDelayedWrites++;
                    if ((now - lastWarning) > kWarningThrottleNs) {
                        ATRACE_NAME("underrun");
                        ALOGW("write blocked for %llu msecs, %d delayed writes, thread %p",
                                ns2ms(delta), mNumDelayedWrites, this);
                        lastWarning = now;
                    }
                }
}

            } else {
                usleep(sleepTime);
            }
        }

        // Finally let go of removed track(s), without the lock held
        // since we can't guarantee the destructors won't acquire that
        // same lock.  This will also mutate and push a new fast mixer state.
        threadLoop_removeTracks(tracksToRemove);
        tracksToRemove.clear();

        // FIXME I don't understand the need for this here;
        //       it was in the original code but maybe the
        //       assignment in saveOutputTracks() makes this unnecessary?
        clearOutputTracks();

        // Effect chains will be actually deleted here if they were removed from
        // mEffectChains list during mixing or effects processing
        effectChains.clear();

        // FIXME Note that the above .clear() is no longer necessary since effectChains
        // is now local to this block, but will keep it for now (at least until merge done).
    }

    threadLoop_exit();

    // for DuplicatingThread, standby mode is handled by the outputTracks, otherwise ...
    if (mType == MIXER || mType == DIRECT || mType == OFFLOAD) {
        // put output stream into standby mode
        if (!mStandby) {
            mOutput->stream->common.standby(&mOutput->stream->common);
        }
    }

    releaseWakeLock();
    mWakeLockUids.clear();
    mActiveTracksGeneration++;

    ALOGV("Thread %p type %d exiting", this, mType);
    return false;
}

// removeTracks_l() must be called with ThreadBase::mLock held
void AudioFlinger::PlaybackThread::removeTracks_l(const Vector< sp<Track> >& tracksToRemove)
{
    size_t count = tracksToRemove.size();
    if (count) {
        for (size_t i=0 ; i<count ; i++) {
            const sp<Track>& track = tracksToRemove.itemAt(i);
            mActiveTracks.remove(track);
            mWakeLockUids.remove(track->uid());
            mActiveTracksGeneration++;
            ALOGV("removeTracks_l removing track on session %d", track->sessionId());
            sp<EffectChain> chain = getEffectChain_l(track->sessionId());
            if (chain != 0) {
                ALOGV("stopping track on chain %p for session Id: %d", chain.get(),
                        track->sessionId());
                chain->decActiveTrackCnt();
            }
            if (track->isTerminated()) {
                removeTrack_l(track);
            }
        }
    }

}

status_t AudioFlinger::PlaybackThread::getTimestamp_l(AudioTimestamp& timestamp)
{
    if (mNormalSink != 0) {
        return mNormalSink->getTimestamp(timestamp);
    }
    if (mType == OFFLOAD && mOutput->stream->get_presentation_position) {
        uint64_t position64;
        int ret = mOutput->stream->get_presentation_position(
                                                mOutput->stream, &position64, &timestamp.mTime);
        if (ret == 0) {
            timestamp.mPosition = (uint32_t)position64;
            return NO_ERROR;
        }
    }
    return INVALID_OPERATION;
}
// ----------------------------------------------------------------------------

AudioFlinger::MixerThread::MixerThread(const sp<AudioFlinger>& audioFlinger, AudioStreamOut* output,
        audio_io_handle_t id, audio_devices_t device, type_t type)
    :   PlaybackThread(audioFlinger, output, id, device, type),
        // mAudioMixer below
        // mFastMixer below
        mFastMixerFutex(0)
        // mOutputSink below
        // mPipeSink below
        // mNormalSink below
{
    ALOGV("MixerThread() id=%d device=%#x type=%d", id, device, type);
    ALOGV("mSampleRate=%u, mChannelMask=%#x, mChannelCount=%u, mFormat=%d, mFrameSize=%u, "
            "mFrameCount=%d, mNormalFrameCount=%d",
            mSampleRate, mChannelMask, mChannelCount, mFormat, mFrameSize, mFrameCount,
            mNormalFrameCount);
    mAudioMixer = new AudioMixer(mNormalFrameCount, mSampleRate);

    // FIXME - Current mixer implementation only supports stereo output
    if (mChannelCount != FCC_2) {
        ALOGE("Invalid audio hardware channel count %d", mChannelCount);
    }

    // create an NBAIO sink for the HAL output stream, and negotiate
    mOutputSink = new AudioStreamOutSink(output->stream);
    size_t numCounterOffers = 0;
    const NBAIO_Format offers[1] = {Format_from_SR_C(mSampleRate, mChannelCount)};
    ssize_t index = mOutputSink->negotiate(offers, 1, NULL, numCounterOffers);
    ALOG_ASSERT(index == 0);

    // initialize fast mixer depending on configuration
    bool initFastMixer;
    switch (kUseFastMixer) {
    case FastMixer_Never:
        initFastMixer = false;
        break;
    case FastMixer_Always:
        initFastMixer = true;
        break;
    case FastMixer_Static:
    case FastMixer_Dynamic:
        initFastMixer = mFrameCount < mNormalFrameCount;
        break;
    }
    if (initFastMixer) {

        // create a MonoPipe to connect our submix to FastMixer
        NBAIO_Format format = mOutputSink->format();
        // This pipe depth compensates for scheduling latency of the normal mixer thread.
        // When it wakes up after a maximum latency, it runs a few cycles quickly before
        // finally blocking.  Note the pipe implementation rounds up the request to a power of 2.
        MonoPipe *monoPipe = new MonoPipe(mNormalFrameCount * 4, format, true /*writeCanBlock*/);
        const NBAIO_Format offers[1] = {format};
        size_t numCounterOffers = 0;
        ssize_t index = monoPipe->negotiate(offers, 1, NULL, numCounterOffers);
        ALOG_ASSERT(index == 0);
        monoPipe->setAvgFrames((mScreenState & 1) ?
                (monoPipe->maxFrames() * 7) / 8 : mNormalFrameCount * 2);
        mPipeSink = monoPipe;

#ifdef TEE_SINK
        if (mTeeSinkOutputEnabled) {
            // create a Pipe to archive a copy of FastMixer's output for dumpsys
            Pipe *teeSink = new Pipe(mTeeSinkOutputFrames, format);
            numCounterOffers = 0;
            index = teeSink->negotiate(offers, 1, NULL, numCounterOffers);
            ALOG_ASSERT(index == 0);
            mTeeSink = teeSink;
            PipeReader *teeSource = new PipeReader(*teeSink);
            numCounterOffers = 0;
            index = teeSource->negotiate(offers, 1, NULL, numCounterOffers);
            ALOG_ASSERT(index == 0);
            mTeeSource = teeSource;
        }
#endif

        // create fast mixer and configure it initially with just one fast track for our submix
        mFastMixer = new FastMixer();
        FastMixerStateQueue *sq = mFastMixer->sq();
#ifdef STATE_QUEUE_DUMP
        sq->setObserverDump(&mStateQueueObserverDump);
        sq->setMutatorDump(&mStateQueueMutatorDump);
#endif
        FastMixerState *state = sq->begin();
        FastTrack *fastTrack = &state->mFastTracks[0];
        // wrap the source side of the MonoPipe to make it an AudioBufferProvider
        fastTrack->mBufferProvider = new SourceAudioBufferProvider(new MonoPipeReader(monoPipe));
        fastTrack->mVolumeProvider = NULL;
        fastTrack->mGeneration++;
        state->mFastTracksGen++;
        state->mTrackMask = 1;
        // fast mixer will use the HAL output sink
        state->mOutputSink = mOutputSink.get();
        state->mOutputSinkGen++;
        state->mFrameCount = mFrameCount;
        state->mCommand = FastMixerState::COLD_IDLE;
        // already done in constructor initialization list
        //mFastMixerFutex = 0;
        state->mColdFutexAddr = &mFastMixerFutex;
        state->mColdGen++;
        state->mDumpState = &mFastMixerDumpState;
#ifdef TEE_SINK
        state->mTeeSink = mTeeSink.get();
#endif
        mFastMixerNBLogWriter = audioFlinger->newWriter_l(kFastMixerLogSize, "FastMixer");
        state->mNBLogWriter = mFastMixerNBLogWriter.get();
        sq->end();
        sq->push(FastMixerStateQueue::BLOCK_UNTIL_PUSHED);

        // start the fast mixer
        mFastMixer->run("FastMixer", PRIORITY_URGENT_AUDIO);
        pid_t tid = mFastMixer->getTid();
        int err = requestPriority(getpid_cached, tid, kPriorityFastMixer);
        if (err != 0) {
            ALOGW("Policy SCHED_FIFO priority %d is unavailable for pid %d tid %d; error %d",
                    kPriorityFastMixer, getpid_cached, tid, err);
        }

#ifdef AUDIO_WATCHDOG
        // create and start the watchdog
        mAudioWatchdog = new AudioWatchdog();
        mAudioWatchdog->setDump(&mAudioWatchdogDump);
        mAudioWatchdog->run("AudioWatchdog", PRIORITY_URGENT_AUDIO);
        tid = mAudioWatchdog->getTid();
        err = requestPriority(getpid_cached, tid, kPriorityFastMixer);
        if (err != 0) {
            ALOGW("Policy SCHED_FIFO priority %d is unavailable for pid %d tid %d; error %d",
                    kPriorityFastMixer, getpid_cached, tid, err);
        }
#endif

    } else {
        mFastMixer = NULL;
    }

    switch (kUseFastMixer) {
    case FastMixer_Never:
    case FastMixer_Dynamic:
        mNormalSink = mOutputSink;
        break;
    case FastMixer_Always:
        mNormalSink = mPipeSink;
        break;
    case FastMixer_Static:
        mNormalSink = initFastMixer ? mPipeSink : mOutputSink;
        break;
    }
}

AudioFlinger::MixerThread::~MixerThread()
{
    if (mFastMixer != NULL) {
        FastMixerStateQueue *sq = mFastMixer->sq();
        FastMixerState *state = sq->begin();
        if (state->mCommand == FastMixerState::COLD_IDLE) {
            int32_t old = android_atomic_inc(&mFastMixerFutex);
            if (old == -1) {
                __futex_syscall3(&mFastMixerFutex, FUTEX_WAKE_PRIVATE, 1);
            }
        }
        state->mCommand = FastMixerState::EXIT;
        sq->end();
        sq->push(FastMixerStateQueue::BLOCK_UNTIL_PUSHED);
        mFastMixer->join();
        // Though the fast mixer thread has exited, it's state queue is still valid.
        // We'll use that extract the final state which contains one remaining fast track
        // corresponding to our sub-mix.
        state = sq->begin();
        ALOG_ASSERT(state->mTrackMask == 1);
        FastTrack *fastTrack = &state->mFastTracks[0];
        ALOG_ASSERT(fastTrack->mBufferProvider != NULL);
        delete fastTrack->mBufferProvider;
        sq->end(false /*didModify*/);
        delete mFastMixer;
#ifdef AUDIO_WATCHDOG
        if (mAudioWatchdog != 0) {
            mAudioWatchdog->requestExit();
            mAudioWatchdog->requestExitAndWait();
            mAudioWatchdog.clear();
        }
#endif
    }
    mAudioFlinger->unregisterWriter(mFastMixerNBLogWriter);
    delete mAudioMixer;
}


uint32_t AudioFlinger::MixerThread::correctLatency_l(uint32_t latency) const
{
    if (mFastMixer != NULL) {
        MonoPipe *pipe = (MonoPipe *)mPipeSink.get();
        latency += (pipe->getAvgFrames() * 1000) / mSampleRate;
    }
    return latency;
}


void AudioFlinger::MixerThread::threadLoop_removeTracks(const Vector< sp<Track> >& tracksToRemove)
{
    PlaybackThread::threadLoop_removeTracks(tracksToRemove);
}

ssize_t AudioFlinger::MixerThread::threadLoop_write()
{
    // FIXME we should only do one push per cycle; confirm this is true
    // Start the fast mixer if it's not already running
    if (mFastMixer != NULL) {
        FastMixerStateQueue *sq = mFastMixer->sq();
        FastMixerState *state = sq->begin();
        if (state->mCommand != FastMixerState::MIX_WRITE &&
                (kUseFastMixer != FastMixer_Dynamic || state->mTrackMask > 1)) {
            if (state->mCommand == FastMixerState::COLD_IDLE) {
                int32_t old = android_atomic_inc(&mFastMixerFutex);
                if (old == -1) {
                    __futex_syscall3(&mFastMixerFutex, FUTEX_WAKE_PRIVATE, 1);
                }
#ifdef AUDIO_WATCHDOG
                if (mAudioWatchdog != 0) {
                    mAudioWatchdog->resume();
                }
#endif
            }
            state->mCommand = FastMixerState::MIX_WRITE;
            mFastMixerDumpState.increaseSamplingN(mAudioFlinger->isLowRamDevice() ?
                    FastMixerDumpState::kSamplingNforLowRamDevice : FastMixerDumpState::kSamplingN);
            sq->end();
            sq->push(FastMixerStateQueue::BLOCK_UNTIL_PUSHED);
            if (kUseFastMixer == FastMixer_Dynamic) {
                mNormalSink = mPipeSink;
            }
        } else {
            sq->end(false /*didModify*/);
        }
    }
    return PlaybackThread::threadLoop_write();
}

void AudioFlinger::MixerThread::threadLoop_standby()
{
    // Idle the fast mixer if it's currently running
    if (mFastMixer != NULL) {
        FastMixerStateQueue *sq = mFastMixer->sq();
        FastMixerState *state = sq->begin();
        if (!(state->mCommand & FastMixerState::IDLE)) {
            state->mCommand = FastMixerState::COLD_IDLE;
            state->mColdFutexAddr = &mFastMixerFutex;
            state->mColdGen++;
            mFastMixerFutex = 0;
            sq->end();
            // BLOCK_UNTIL_PUSHED would be insufficient, as we need it to stop doing I/O now
            sq->push(FastMixerStateQueue::BLOCK_UNTIL_ACKED);
            if (kUseFastMixer == FastMixer_Dynamic) {
                mNormalSink = mOutputSink;
            }
#ifdef AUDIO_WATCHDOG
            if (mAudioWatchdog != 0) {
                mAudioWatchdog->pause();
            }
#endif
        } else {
            sq->end(false /*didModify*/);
        }
    }
    PlaybackThread::threadLoop_standby();
}

// Empty implementation for standard mixer
// Overridden for offloaded playback
void AudioFlinger::PlaybackThread::flushOutput_l()
{
}

bool AudioFlinger::PlaybackThread::waitingAsyncCallback_l()
{
    return false;
}

bool AudioFlinger::PlaybackThread::shouldStandby_l()
{
    return !mStandby;
}

bool AudioFlinger::PlaybackThread::waitingAsyncCallback()
{
    Mutex::Autolock _l(mLock);
    return waitingAsyncCallback_l();
}

// shared by MIXER and DIRECT, overridden by DUPLICATING
void AudioFlinger::PlaybackThread::threadLoop_standby()
{
    ALOGV("Audio hardware entering standby, mixer %p, suspend count %d", this, mSuspended);
    mOutput->stream->common.standby(&mOutput->stream->common);
    if (mUseAsyncWrite != 0) {
        // discard any pending drain or write ack by incrementing sequence
        mWriteAckSequence = (mWriteAckSequence + 2) & ~1;
        mDrainSequence = (mDrainSequence + 2) & ~1;
        ALOG_ASSERT(mCallbackThread != 0);
        mCallbackThread->setWriteBlocked(mWriteAckSequence);
        mCallbackThread->setDraining(mDrainSequence);
    }
}

void AudioFlinger::MixerThread::threadLoop_mix()
{
    // obtain the presentation timestamp of the next output buffer
    int64_t pts;
    status_t status = INVALID_OPERATION;

    if (mNormalSink != 0) {
        status = mNormalSink->getNextWriteTimestamp(&pts);
    } else {
        status = mOutputSink->getNextWriteTimestamp(&pts);
    }

    if (status != NO_ERROR) {
        pts = AudioBufferProvider::kInvalidPTS;
    }

    // mix buffers...
    mAudioMixer->process(pts);
    mCurrentWriteLength = mixBufferSize;
    // increase sleep time progressively when application underrun condition clears.
    // Only increase sleep time if the mixer is ready for two consecutive times to avoid
    // that a steady state of alternating ready/not ready conditions keeps the sleep time
    // such that we would underrun the audio HAL.
    if ((sleepTime == 0) && (sleepTimeShift > 0)) {
        sleepTimeShift--;
    }
    sleepTime = 0;
    standbyTime = systemTime() + standbyDelay;
    //TODO: delay standby when effects have a tail
}

void AudioFlinger::MixerThread::threadLoop_sleepTime()
{
    // If no tracks are ready, sleep once for the duration of an output
    // buffer size, then write 0s to the output
    if (sleepTime == 0) {
        if (mMixerStatus == MIXER_TRACKS_ENABLED) {
            sleepTime = activeSleepTime >> sleepTimeShift;
            if (sleepTime < kMinThreadSleepTimeUs) {
                sleepTime = kMinThreadSleepTimeUs;
            }
            // reduce sleep time in case of consecutive application underruns to avoid
            // starving the audio HAL. As activeSleepTimeUs() is larger than a buffer
            // duration we would end up writing less data than needed by the audio HAL if
            // the condition persists.
            if (sleepTimeShift < kMaxThreadSleepTimeShift) {
                sleepTimeShift++;
            }
        } else {
            sleepTime = idleSleepTime;
        }
    } else if (mBytesWritten != 0 || (mMixerStatus == MIXER_TRACKS_ENABLED)) {
        memset (mMixBuffer, 0, mixBufferSize);
        sleepTime = 0;
        ALOGV_IF(mBytesWritten == 0 && (mMixerStatus == MIXER_TRACKS_ENABLED),
                "anticipated start");
    }
    // TODO add standby time extension fct of effect tail
}

// prepareTracks_l() must be called with ThreadBase::mLock held
AudioFlinger::PlaybackThread::mixer_state AudioFlinger::MixerThread::prepareTracks_l(
        Vector< sp<Track> > *tracksToRemove)
{

    mixer_state mixerStatus = MIXER_IDLE;
    // find out which tracks need to be processed
    size_t count = mActiveTracks.size();
    size_t mixedTracks = 0;
    size_t tracksWithEffect = 0;
    // counts only _active_ fast tracks
    size_t fastTracks = 0;
    uint32_t resetMask = 0; // bit mask of fast tracks that need to be reset

    float masterVolume = mMasterVolume;
    bool masterMute = mMasterMute;

    if (masterMute) {
        masterVolume = 0;
    }
    // Delegate master volume control to effect in output mix effect chain if needed
    sp<EffectChain> chain = getEffectChain_l(AUDIO_SESSION_OUTPUT_MIX);
    if (chain != 0) {
        uint32_t v = (uint32_t)(masterVolume * (1 << 24));
        chain->setVolume_l(&v, &v);
        masterVolume = (float)((v + (1 << 23)) >> 24);
        chain.clear();
    }

    // prepare a new state to push
    FastMixerStateQueue *sq = NULL;
    FastMixerState *state = NULL;
    bool didModify = false;
    FastMixerStateQueue::block_t block = FastMixerStateQueue::BLOCK_UNTIL_PUSHED;
    if (mFastMixer != NULL) {
        sq = mFastMixer->sq();
        state = sq->begin();
    }

    for (size_t i=0 ; i<count ; i++) {
        const sp<Track> t = mActiveTracks[i].promote();
        if (t == 0) {
            continue;
        }

        // this const just means the local variable doesn't change
        Track* const track = t.get();

        // process fast tracks
        if (track->isFastTrack()) {

            // It's theoretically possible (though unlikely) for a fast track to be created
            // and then removed within the same normal mix cycle.  This is not a problem, as
            // the track never becomes active so it's fast mixer slot is never touched.
            // The converse, of removing an (active) track and then creating a new track
            // at the identical fast mixer slot within the same normal mix cycle,
            // is impossible because the slot isn't marked available until the end of each cycle.
            int j = track->mFastIndex;
            ALOG_ASSERT(0 < j && j < (int)FastMixerState::kMaxFastTracks);
            ALOG_ASSERT(!(mFastTrackAvailMask & (1 << j)));
            FastTrack *fastTrack = &state->mFastTracks[j];

            // Determine whether the track is currently in underrun condition,
            // and whether it had a recent underrun.
            FastTrackDump *ftDump = &mFastMixerDumpState.mTracks[j];
            FastTrackUnderruns underruns = ftDump->mUnderruns;
            uint32_t recentFull = (underruns.mBitFields.mFull -
                    track->mObservedUnderruns.mBitFields.mFull) & UNDERRUN_MASK;
            uint32_t recentPartial = (underruns.mBitFields.mPartial -
                    track->mObservedUnderruns.mBitFields.mPartial) & UNDERRUN_MASK;
            uint32_t recentEmpty = (underruns.mBitFields.mEmpty -
                    track->mObservedUnderruns.mBitFields.mEmpty) & UNDERRUN_MASK;
            uint32_t recentUnderruns = recentPartial + recentEmpty;
            track->mObservedUnderruns = underruns;
            // don't count underruns that occur while stopping or pausing
            // or stopped which can occur when flush() is called while active
            if (!(track->isStopping() || track->isPausing() || track->isStopped()) &&
                    recentUnderruns > 0) {
                // FIXME fast mixer will pull & mix partial buffers, but we count as a full underrun
                track->mAudioTrackServerProxy->tallyUnderrunFrames(recentUnderruns * mFrameCount);
            }

            // This is similar to the state machine for normal tracks,
            // with a few modifications for fast tracks.
            bool isActive = true;
            switch (track->mState) {
            case TrackBase::STOPPING_1:
                // track stays active in STOPPING_1 state until first underrun
                if (recentUnderruns > 0 || track->isTerminated()) {
                    track->mState = TrackBase::STOPPING_2;
                }
                break;
            case TrackBase::PAUSING:
                // ramp down is not yet implemented
                track->setPaused();
                break;
            case TrackBase::RESUMING:
                // ramp up is not yet implemented
                track->mState = TrackBase::ACTIVE;
                break;
            case TrackBase::ACTIVE:
                if (recentFull > 0 || recentPartial > 0) {
                    // track has provided at least some frames recently: reset retry count
                    track->mRetryCount = kMaxTrackRetries;
                }
                if (recentUnderruns == 0) {
                    // no recent underruns: stay active
                    break;
                }
                // there has recently been an underrun of some kind
                if (track->sharedBuffer() == 0) {
                    // were any of the recent underruns "empty" (no frames available)?
                    if (recentEmpty == 0) {
                        // no, then ignore the partial underruns as they are allowed indefinitely
                        break;
                    }
                    // there has recently been an "empty" underrun: decrement the retry counter
                    if (--(track->mRetryCount) > 0) {
                        break;
                    }
                    // indicate to client process that the track was disabled because of underrun;
                    // it will then automatically call start() when data is available
                    android_atomic_or(CBLK_DISABLED, &track->mCblk->mFlags);
                    // remove from active list, but state remains ACTIVE [confusing but true]
                    isActive = false;
                    break;
                }
                // fall through
            case TrackBase::STOPPING_2:
            case TrackBase::PAUSED:
            case TrackBase::STOPPED:
            case TrackBase::FLUSHED:   // flush() while active
                // Check for presentation complete if track is inactive
                // We have consumed all the buffers of this track.
                // This would be incomplete if we auto-paused on underrun
                {
                    size_t audioHALFrames =
                            (mOutput->stream->get_latency(mOutput->stream)*mSampleRate) / 1000;
                    size_t framesWritten = mBytesWritten / mFrameSize;
                    if (!(mStandby || track->presentationComplete(framesWritten, audioHALFrames))) {
                        // track stays in active list until presentation is complete
                        break;
                    }
                }
                if (track->isStopping_2()) {
                    track->mState = TrackBase::STOPPED;
                }
                if (track->isStopped()) {
                    // Can't reset directly, as fast mixer is still polling this track
                    //   track->reset();
                    // So instead mark this track as needing to be reset after push with ack
                    resetMask |= 1 << i;
                }
                isActive = false;
                break;
            case TrackBase::IDLE:
            default:
                LOG_FATAL("unexpected track state %d", track->mState);
            }

            if (isActive) {
                // was it previously inactive?
                if (!(state->mTrackMask & (1 << j))) {
                    ExtendedAudioBufferProvider *eabp = track;
                    VolumeProvider *vp = track;
                    fastTrack->mBufferProvider = eabp;
                    fastTrack->mVolumeProvider = vp;
                    fastTrack->mSampleRate = track->mSampleRate;
                    fastTrack->mChannelMask = track->mChannelMask;
                    fastTrack->mGeneration++;
                    state->mTrackMask |= 1 << j;
                    didModify = true;
                    // no acknowledgement required for newly active tracks
                }
                // cache the combined master volume and stream type volume for fast mixer; this
                // lacks any synchronization or barrier so VolumeProvider may read a stale value
                track->mCachedVolume = masterVolume * mStreamTypes[track->streamType()].volume;
                ++fastTracks;
            } else {
                // was it previously active?
                if (state->mTrackMask & (1 << j)) {
                    fastTrack->mBufferProvider = NULL;
                    fastTrack->mGeneration++;
                    state->mTrackMask &= ~(1 << j);
                    didModify = true;
                    // If any fast tracks were removed, we must wait for acknowledgement
                    // because we're about to decrement the last sp<> on those tracks.
                    block = FastMixerStateQueue::BLOCK_UNTIL_ACKED;
                } else {
                    LOG_FATAL("fast track %d should have been active", j);
                }
                tracksToRemove->add(track);
                // Avoids a misleading display in dumpsys
                track->mObservedUnderruns.mBitFields.mMostRecent = UNDERRUN_FULL;
            }
            continue;
        }

        {   // local variable scope to avoid goto warning

        audio_track_cblk_t* cblk = track->cblk();

        // The first time a track is added we wait
        // for all its buffers to be filled before processing it
        int name = track->name();
        // make sure that we have enough frames to mix one full buffer.
        // enforce this condition only once to enable draining the buffer in case the client
        // app does not call stop() and relies on underrun to stop:
        // hence the test on (mMixerStatus == MIXER_TRACKS_READY) meaning the track was mixed
        // during last round
        size_t desiredFrames;
        uint32_t sr = track->sampleRate();
        if (sr == mSampleRate) {
            desiredFrames = mNormalFrameCount;
        } else {
            // +1 for rounding and +1 for additional sample needed for interpolation
            desiredFrames = (mNormalFrameCount * sr) / mSampleRate + 1 + 1;
            // add frames already consumed but not yet released by the resampler
            // because cblk->framesReady() will include these frames
            desiredFrames += mAudioMixer->getUnreleasedFrames(track->name());
            // the minimum track buffer size is normally twice the number of frames necessary
            // to fill one buffer and the resampler should not leave more than one buffer worth
            // of unreleased frames after each pass, but just in case...
            ALOG_ASSERT(desiredFrames <= cblk->frameCount_);
        }
        uint32_t minFrames = 1;
        if ((track->sharedBuffer() == 0) && !track->isStopped() && !track->isPausing() &&
                (mMixerStatusIgnoringFastTracks == MIXER_TRACKS_READY)) {
            minFrames = desiredFrames;
        }
        // It's not safe to call framesReady() for a static buffer track, so assume it's ready
        size_t framesReady;
        if (track->sharedBuffer() == 0) {
            framesReady = track->framesReady();
        } else if (track->isStopped()) {
            framesReady = 0;
        } else {
            framesReady = 1;
        }
        if ((framesReady >= minFrames) && track->isReady() &&
                !track->isPaused() && !track->isTerminated())
        {
            ALOGVV("track %d s=%08x [OK] on thread %p", name, cblk->mServer, this);

            mixedTracks++;

            // track->mainBuffer() != mMixBuffer means there is an effect chain
            // connected to the track
            chain.clear();
            if (track->mainBuffer() != mMixBuffer) {
                chain = getEffectChain_l(track->sessionId());
                // Delegate volume control to effect in track effect chain if needed
                if (chain != 0) {
                    tracksWithEffect++;
                } else {
                    ALOGW("prepareTracks_l(): track %d attached to effect but no chain found on "
                            "session %d",
                            name, track->sessionId());
                }
            }


            int param = AudioMixer::VOLUME;
            if (track->mFillingUpStatus == Track::FS_FILLED) {
                // no ramp for the first volume setting
                track->mFillingUpStatus = Track::FS_ACTIVE;
                if (track->mState == TrackBase::RESUMING) {
                    track->mState = TrackBase::ACTIVE;
                    param = AudioMixer::RAMP_VOLUME;
                }
                mAudioMixer->setParameter(name, AudioMixer::RESAMPLE, AudioMixer::RESET, NULL);
            // FIXME should not make a decision based on mServer
            } else if (cblk->mServer != 0) {
                // If the track is stopped before the first frame was mixed,
                // do not apply ramp
                param = AudioMixer::RAMP_VOLUME;
            }

            // compute volume for this track
            uint32_t vl, vr, va;
            if (track->isPausing() || mStreamTypes[track->streamType()].mute) {
                vl = vr = va = 0;
                if (track->isPausing()) {
                    track->setPaused();
                }
            } else {

                // read original volumes with volume control
                float typeVolume = mStreamTypes[track->streamType()].volume;
                float v = masterVolume * typeVolume;
                AudioTrackServerProxy *proxy = track->mAudioTrackServerProxy;
                uint32_t vlr = proxy->getVolumeLR();
                vl = vlr & 0xFFFF;
                vr = vlr >> 16;
                // track volumes come from shared memory, so can't be trusted and must be clamped
                if (vl > MAX_GAIN_INT) {
                    ALOGV("Track left volume out of range: %04X", vl);
                    vl = MAX_GAIN_INT;
                }
                if (vr > MAX_GAIN_INT) {
                    ALOGV("Track right volume out of range: %04X", vr);
                    vr = MAX_GAIN_INT;
                }
                // now apply the master volume and stream type volume
                vl = (uint32_t)(v * vl) << 12;
                vr = (uint32_t)(v * vr) << 12;
                // assuming master volume and stream type volume each go up to 1.0,
                // vl and vr are now in 8.24 format

                uint16_t sendLevel = proxy->getSendLevel_U4_12();
                // send level comes from shared memory and so may be corrupt
                if (sendLevel > MAX_GAIN_INT) {
                    ALOGV("Track send level out of range: %04X", sendLevel);
                    sendLevel = MAX_GAIN_INT;
                }
                va = (uint32_t)(v * sendLevel);
            }

            // Delegate volume control to effect in track effect chain if needed
            if (chain != 0 && chain->setVolume_l(&vl, &vr)) {
                // Do not ramp volume if volume is controlled by effect
                param = AudioMixer::VOLUME;
                track->mHasVolumeController = true;
            } else {
                // force no volume ramp when volume controller was just disabled or removed
                // from effect chain to avoid volume spike
                if (track->mHasVolumeController) {
                    param = AudioMixer::VOLUME;
                }
                track->mHasVolumeController = false;
            }

            // Convert volumes from 8.24 to 4.12 format
            // This additional clamping is needed in case chain->setVolume_l() overshot
            vl = (vl + (1 << 11)) >> 12;
            if (vl > MAX_GAIN_INT) {
                vl = MAX_GAIN_INT;
            }
            vr = (vr + (1 << 11)) >> 12;
            if (vr > MAX_GAIN_INT) {
                vr = MAX_GAIN_INT;
            }

            if (va > MAX_GAIN_INT) {
                va = MAX_GAIN_INT;   // va is uint32_t, so no need to check for -
            }

            // XXX: these things DON'T need to be done each time
            mAudioMixer->setBufferProvider(name, track);
            mAudioMixer->enable(name);

            mAudioMixer->setParameter(name, param, AudioMixer::VOLUME0, (void *)vl);
            mAudioMixer->setParameter(name, param, AudioMixer::VOLUME1, (void *)vr);
            mAudioMixer->setParameter(name, param, AudioMixer::AUXLEVEL, (void *)va);
            mAudioMixer->setParameter(
                name,
                AudioMixer::TRACK,
                AudioMixer::FORMAT, (void *)track->format());
            mAudioMixer->setParameter(
                name,
                AudioMixer::TRACK,
                AudioMixer::CHANNEL_MASK, (void *)track->channelMask());
            // limit track sample rate to 2 x output sample rate, which changes at re-configuration
            uint32_t maxSampleRate = mSampleRate * 2;
            uint32_t reqSampleRate = track->mAudioTrackServerProxy->getSampleRate();
            if (reqSampleRate == 0) {
                reqSampleRate = mSampleRate;
            } else if (reqSampleRate > maxSampleRate) {
                reqSampleRate = maxSampleRate;
            }
            mAudioMixer->setParameter(
                name,
                AudioMixer::RESAMPLE,
                AudioMixer::SAMPLE_RATE,
                (void *)reqSampleRate);
            mAudioMixer->setParameter(
                name,
                AudioMixer::TRACK,
                AudioMixer::MAIN_BUFFER, (void *)track->mainBuffer());
            mAudioMixer->setParameter(
                name,
                AudioMixer::TRACK,
                AudioMixer::AUX_BUFFER, (void *)track->auxBuffer());

            // reset retry count
            track->mRetryCount = kMaxTrackRetries;

            // If one track is ready, set the mixer ready if:
            //  - the mixer was not ready during previous round OR
            //  - no other track is not ready
            if (mMixerStatusIgnoringFastTracks != MIXER_TRACKS_READY ||
                    mixerStatus != MIXER_TRACKS_ENABLED) {
                mixerStatus = MIXER_TRACKS_READY;
            }
        } else {
            if (framesReady < desiredFrames && !track->isStopped() && !track->isPaused()) {
                track->mAudioTrackServerProxy->tallyUnderrunFrames(desiredFrames);
            }
            // clear effect chain input buffer if an active track underruns to avoid sending
            // previous audio buffer again to effects
            chain = getEffectChain_l(track->sessionId());
            if (chain != 0) {
                chain->clearInputBuffer();
            }

            ALOGVV("track %d s=%08x [NOT READY] on thread %p", name, cblk->mServer, this);
            if ((track->sharedBuffer() != 0) || track->isTerminated() ||
                    track->isStopped() || track->isPaused()) {
                // We have consumed all the buffers of this track.
                // Remove it from the list of active tracks.
                // TODO: use actual buffer filling status instead of latency when available from
                // audio HAL
                size_t audioHALFrames = (latency_l() * mSampleRate) / 1000;
                size_t framesWritten = mBytesWritten / mFrameSize;
                if (mStandby || track->presentationComplete(framesWritten, audioHALFrames)) {
                    if (track->isStopped()) {
                        track->reset();
                    }
                    tracksToRemove->add(track);
                }
            } else {
                // No buffers for this track. Give it a few chances to
                // fill a buffer, then remove it from active list.
                if (--(track->mRetryCount) <= 0) {
                    ALOGI("BUFFER TIMEOUT: remove(%d) from active list on thread %p", name, this);
                    tracksToRemove->add(track);
                    // indicate to client process that the track was disabled because of underrun;
                    // it will then automatically call start() when data is available
                    android_atomic_or(CBLK_DISABLED, &cblk->mFlags);
                // If one track is not ready, mark the mixer also not ready if:
                //  - the mixer was ready during previous round OR
                //  - no other track is ready
                } else if (mMixerStatusIgnoringFastTracks == MIXER_TRACKS_READY ||
                                mixerStatus != MIXER_TRACKS_READY) {
                    mixerStatus = MIXER_TRACKS_ENABLED;
                }
            }
            mAudioMixer->disable(name);
        }

        }   // local variable scope to avoid goto warning
track_is_ready: ;

    }

    // Push the new FastMixer state if necessary
    bool pauseAudioWatchdog = false;
    if (didModify) {
        state->mFastTracksGen++;
        // if the fast mixer was active, but now there are no fast tracks, then put it in cold idle
        if (kUseFastMixer == FastMixer_Dynamic &&
                state->mCommand == FastMixerState::MIX_WRITE && state->mTrackMask <= 1) {
            state->mCommand = FastMixerState::COLD_IDLE;
            state->mColdFutexAddr = &mFastMixerFutex;
            state->mColdGen++;
            mFastMixerFutex = 0;
            if (kUseFastMixer == FastMixer_Dynamic) {
                mNormalSink = mOutputSink;
            }
            // If we go into cold idle, need to wait for acknowledgement
            // so that fast mixer stops doing I/O.
            block = FastMixerStateQueue::BLOCK_UNTIL_ACKED;
            pauseAudioWatchdog = true;
        }
    }
    if (sq != NULL) {
        sq->end(didModify);
        sq->push(block);
    }
#ifdef AUDIO_WATCHDOG
    if (pauseAudioWatchdog && mAudioWatchdog != 0) {
        mAudioWatchdog->pause();
    }
#endif

    // Now perform the deferred reset on fast tracks that have stopped
    while (resetMask != 0) {
        size_t i = __builtin_ctz(resetMask);
        ALOG_ASSERT(i < count);
        resetMask &= ~(1 << i);
        sp<Track> t = mActiveTracks[i].promote();
        if (t == 0) {
            continue;
        }
        Track* track = t.get();
        ALOG_ASSERT(track->isFastTrack() && track->isStopped());
        track->reset();
    }

    // remove all the tracks that need to be...
    removeTracks_l(*tracksToRemove);

    // mix buffer must be cleared if all tracks are connected to an
    // effect chain as in this case the mixer will not write to
    // mix buffer and track effects will accumulate into it
    if ((mBytesRemaining == 0) && ((mixedTracks != 0 && mixedTracks == tracksWithEffect) ||
            (mixedTracks == 0 && fastTracks > 0))) {
        // FIXME as a performance optimization, should remember previous zero status
        memset(mMixBuffer, 0, mNormalFrameCount * mChannelCount * sizeof(int16_t));
    }

    // if any fast tracks, then status is ready
    mMixerStatusIgnoringFastTracks = mixerStatus;
    if (fastTracks > 0) {
        mixerStatus = MIXER_TRACKS_READY;
    }
    return mixerStatus;
}

// getTrackName_l() must be called with ThreadBase::mLock held
int AudioFlinger::MixerThread::getTrackName_l(audio_channel_mask_t channelMask, int sessionId)
{
    return mAudioMixer->getTrackName(channelMask, sessionId);
}

// deleteTrackName_l() must be called with ThreadBase::mLock held
void AudioFlinger::MixerThread::deleteTrackName_l(int name)
{
    ALOGV("remove track (%d) and delete from mixer", name);
    mAudioMixer->deleteTrackName(name);
}

// checkForNewParameters_l() must be called with ThreadBase::mLock held
bool AudioFlinger::MixerThread::checkForNewParameters_l()
{
    // if !&IDLE, holds the FastMixer state to restore after new parameters processed
    FastMixerState::Command previousCommand = FastMixerState::HOT_IDLE;
    bool reconfig = false;

    while (!mNewParameters.isEmpty()) {

        if (mFastMixer != NULL) {
            FastMixerStateQueue *sq = mFastMixer->sq();
            FastMixerState *state = sq->begin();
            if (!(state->mCommand & FastMixerState::IDLE)) {
                previousCommand = state->mCommand;
                state->mCommand = FastMixerState::HOT_IDLE;
                sq->end();
                sq->push(FastMixerStateQueue::BLOCK_UNTIL_ACKED);
            } else {
                sq->end(false /*didModify*/);
            }
        }

        status_t status = NO_ERROR;
        String8 keyValuePair = mNewParameters[0];
        AudioParameter param = AudioParameter(keyValuePair);
        int value;

        if (param.getInt(String8(AudioParameter::keySamplingRate), value) == NO_ERROR) {
            reconfig = true;
        }
        if (param.getInt(String8(AudioParameter::keyFormat), value) == NO_ERROR) {
            if ((audio_format_t) value != AUDIO_FORMAT_PCM_16_BIT) {
                status = BAD_VALUE;
            } else {
                reconfig = true;
            }
        }
        if (param.getInt(String8(AudioParameter::keyChannels), value) == NO_ERROR) {
            if ((audio_channel_mask_t) value != AUDIO_CHANNEL_OUT_STEREO) {
                status = BAD_VALUE;
            } else {
                reconfig = true;
            }
        }
        if (param.getInt(String8(AudioParameter::keyFrameCount), value) == NO_ERROR) {
            // do not accept frame count changes if tracks are open as the track buffer
            // size depends on frame count and correct behavior would not be guaranteed
            // if frame count is changed after track creation
            if (!mTracks.isEmpty()) {
                status = INVALID_OPERATION;
            } else {
                reconfig = true;
            }
        }
        if (param.getInt(String8(AudioParameter::keyRouting), value) == NO_ERROR) {
#ifdef ADD_BATTERY_DATA
            // when changing the audio output device, call addBatteryData to notify
            // the change
            if (mOutDevice != value) {
                uint32_t params = 0;
                // check whether speaker is on
                if (value & AUDIO_DEVICE_OUT_SPEAKER) {
                    params |= IMediaPlayerService::kBatteryDataSpeakerOn;
                }

                audio_devices_t deviceWithoutSpeaker
                    = AUDIO_DEVICE_OUT_ALL & ~AUDIO_DEVICE_OUT_SPEAKER;
                // check if any other device (except speaker) is on
                if (value & deviceWithoutSpeaker ) {
                    params |= IMediaPlayerService::kBatteryDataOtherAudioDeviceOn;
                }

                if (params != 0) {
                    addBatteryData(params);
                }
            }
#endif

            // forward device change to effects that have requested to be
            // aware of attached audio device.
            if (value != AUDIO_DEVICE_NONE) {
                mOutDevice = value;
                for (size_t i = 0; i < mEffectChains.size(); i++) {
                    mEffectChains[i]->setDevice_l(mOutDevice);
                }
            }
        }

        if (status == NO_ERROR) {
            status = mOutput->stream->common.set_parameters(&mOutput->stream->common,
                                                    keyValuePair.string());
            if (!mStandby && status == INVALID_OPERATION) {
                mOutput->stream->common.standby(&mOutput->stream->common);
                mStandby = true;
                mBytesWritten = 0;
                status = mOutput->stream->common.set_parameters(&mOutput->stream->common,
                                                       keyValuePair.string());
            }
            if (status == NO_ERROR && reconfig) {
                readOutputParameters();
                delete mAudioMixer;
                mAudioMixer = new AudioMixer(mNormalFrameCount, mSampleRate);
                for (size_t i = 0; i < mTracks.size() ; i++) {
                    int name = getTrackName_l(mTracks[i]->mChannelMask, mTracks[i]->mSessionId);
                    if (name < 0) {
                        break;
                    }
                    mTracks[i]->mName = name;
                }
                sendIoConfigEvent_l(AudioSystem::OUTPUT_CONFIG_CHANGED);
            }
        }

        mNewParameters.removeAt(0);

        mParamStatus = status;
        mParamCond.signal();
        // wait for condition with time out in case the thread calling ThreadBase::setParameters()
        // already timed out waiting for the status and will never signal the condition.
        mWaitWorkCV.waitRelative(mLock, kSetParametersTimeoutNs);
    }

    if (!(previousCommand & FastMixerState::IDLE)) {
        ALOG_ASSERT(mFastMixer != NULL);
        FastMixerStateQueue *sq = mFastMixer->sq();
        FastMixerState *state = sq->begin();
        ALOG_ASSERT(state->mCommand == FastMixerState::HOT_IDLE);
        state->mCommand = previousCommand;
        sq->end();
        sq->push(FastMixerStateQueue::BLOCK_UNTIL_PUSHED);
    }

    return reconfig;
}


void AudioFlinger::MixerThread::dumpInternals(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    PlaybackThread::dumpInternals(fd, args);

    snprintf(buffer, SIZE, "AudioMixer tracks: %08x\n", mAudioMixer->trackNames());
    result.append(buffer);
    write(fd, result.string(), result.size());

    // Make a non-atomic copy of fast mixer dump state so it won't change underneath us
    const FastMixerDumpState copy(mFastMixerDumpState);
    copy.dump(fd);

#ifdef STATE_QUEUE_DUMP
    // Similar for state queue
    StateQueueObserverDump observerCopy = mStateQueueObserverDump;
    observerCopy.dump(fd);
    StateQueueMutatorDump mutatorCopy = mStateQueueMutatorDump;
    mutatorCopy.dump(fd);
#endif

#ifdef TEE_SINK
    // Write the tee output to a .wav file
    dumpTee(fd, mTeeSource, mId);
#endif

#ifdef AUDIO_WATCHDOG
    if (mAudioWatchdog != 0) {
        // Make a non-atomic copy of audio watchdog dump so it won't change underneath us
        AudioWatchdogDump wdCopy = mAudioWatchdogDump;
        wdCopy.dump(fd);
    }
#endif
}

uint32_t AudioFlinger::MixerThread::idleSleepTimeUs() const
{
    return (uint32_t)(((mNormalFrameCount * 1000) / mSampleRate) * 1000) / 2;
}

uint32_t AudioFlinger::MixerThread::suspendSleepTimeUs() const
{
    return (uint32_t)(((mNormalFrameCount * 1000) / mSampleRate) * 1000);
}

void AudioFlinger::MixerThread::cacheParameters_l()
{
    PlaybackThread::cacheParameters_l();

    // FIXME: Relaxed timing because of a certain device that can't meet latency
    // Should be reduced to 2x after the vendor fixes the driver issue
    // increase threshold again due to low power audio mode. The way this warning
    // threshold is calculated and its usefulness should be reconsidered anyway.
    maxPeriod = seconds(mNormalFrameCount) / mSampleRate * 15;
}

// ----------------------------------------------------------------------------

AudioFlinger::DirectOutputThread::DirectOutputThread(const sp<AudioFlinger>& audioFlinger,
        AudioStreamOut* output, audio_io_handle_t id, audio_devices_t device)
    :   PlaybackThread(audioFlinger, output, id, device, DIRECT)
        // mLeftVolFloat, mRightVolFloat
{
}

AudioFlinger::DirectOutputThread::DirectOutputThread(const sp<AudioFlinger>& audioFlinger,
        AudioStreamOut* output, audio_io_handle_t id, uint32_t device,
        ThreadBase::type_t type)
    :   PlaybackThread(audioFlinger, output, id, device, type)
        // mLeftVolFloat, mRightVolFloat
{
}

AudioFlinger::DirectOutputThread::~DirectOutputThread()
{
}

void AudioFlinger::DirectOutputThread::processVolume_l(Track *track, bool lastTrack)
{
    audio_track_cblk_t* cblk = track->cblk();
    float left, right;

    if (mMasterMute || mStreamTypes[track->streamType()].mute) {
        left = right = 0;
    } else {
        float typeVolume = mStreamTypes[track->streamType()].volume;
        float v = mMasterVolume * typeVolume;
        AudioTrackServerProxy *proxy = track->mAudioTrackServerProxy;
        uint32_t vlr = proxy->getVolumeLR();
        float v_clamped = v * (vlr & 0xFFFF);
        if (v_clamped > MAX_GAIN) v_clamped = MAX_GAIN;
        left = v_clamped/MAX_GAIN;
        v_clamped = v * (vlr >> 16);
        if (v_clamped > MAX_GAIN) v_clamped = MAX_GAIN;
        right = v_clamped/MAX_GAIN;
    }

    if (lastTrack) {
        if (left != mLeftVolFloat || right != mRightVolFloat) {
            mLeftVolFloat = left;
            mRightVolFloat = right;

            // Convert volumes from float to 8.24
            uint32_t vl = (uint32_t)(left * (1 << 24));
            uint32_t vr = (uint32_t)(right * (1 << 24));

            // Delegate volume control to effect in track effect chain if needed
            // only one effect chain can be present on DirectOutputThread, so if
            // there is one, the track is connected to it
            if (!mEffectChains.isEmpty()) {
                mEffectChains[0]->setVolume_l(&vl, &vr);
                left = (float)vl / (1 << 24);
                right = (float)vr / (1 << 24);
            }
            if (mOutput->stream->set_volume) {
                mOutput->stream->set_volume(mOutput->stream, left, right);
            }
        }
    }
}


AudioFlinger::PlaybackThread::mixer_state AudioFlinger::DirectOutputThread::prepareTracks_l(
    Vector< sp<Track> > *tracksToRemove
)
{
    size_t count = mActiveTracks.size();
    mixer_state mixerStatus = MIXER_IDLE;

    // find out which tracks need to be processed
    for (size_t i = 0; i < count; i++) {
        sp<Track> t = mActiveTracks[i].promote();
        // The track died recently
        if (t == 0) {
            continue;
        }

        Track* const track = t.get();
        audio_track_cblk_t* cblk = track->cblk();
        // Only consider last track started for volume and mixer state control.
        // In theory an older track could underrun and restart after the new one starts
        // but as we only care about the transition phase between two tracks on a
        // direct output, it is not a problem to ignore the underrun case.
        sp<Track> l = mLatestActiveTrack.promote();
        bool last = l.get() == track;

        // The first time a track is added we wait
        // for all its buffers to be filled before processing it
        uint32_t minFrames;
        if ((track->sharedBuffer() == 0) && !track->isStopped() && !track->isPausing()) {
            minFrames = mNormalFrameCount;
        } else {
            minFrames = 1;
        }

        if ((track->framesReady() >= minFrames) && track->isReady() &&
                !track->isPaused() && !track->isTerminated())
        {
            ALOGVV("track %d s=%08x [OK]", track->name(), cblk->mServer);

            if (track->mFillingUpStatus == Track::FS_FILLED) {
                track->mFillingUpStatus = Track::FS_ACTIVE;
                // make sure processVolume_l() will apply new volume even if 0
                mLeftVolFloat = mRightVolFloat = -1.0;
                if (track->mState == TrackBase::RESUMING) {
                    track->mState = TrackBase::ACTIVE;
                }
            }

            // compute volume for this track
            processVolume_l(track, last);
            if (last) {
                // reset retry count
                track->mRetryCount = kMaxTrackRetriesDirect;
                mActiveTrack = t;
                mixerStatus = MIXER_TRACKS_READY;
            }
        } else {
            // clear effect chain input buffer if the last active track started underruns
            // to avoid sending previous audio buffer again to effects
            if (!mEffectChains.isEmpty() && last) {
                mEffectChains[0]->clearInputBuffer();
            }

            ALOGVV("track %d s=%08x [NOT READY]", track->name(), cblk->mServer);
            if ((track->sharedBuffer() != 0) || track->isTerminated() ||
                    track->isStopped() || track->isPaused()) {
                // We have consumed all the buffers of this track.
                // Remove it from the list of active tracks.
                // TODO: implement behavior for compressed audio
                size_t audioHALFrames = (latency_l() * mSampleRate) / 1000;
                size_t framesWritten = mBytesWritten / mFrameSize;
                if (mStandby || !last ||
                        track->presentationComplete(framesWritten, audioHALFrames)) {
                    if (track->isStopped()) {
                        track->reset();
                    }
                    tracksToRemove->add(track);
                }
            } else {
                // No buffers for this track. Give it a few chances to
                // fill a buffer, then remove it from active list.
                // Only consider last track started for mixer state control
                if (--(track->mRetryCount) <= 0) {
                    ALOGV("BUFFER TIMEOUT: remove(%d) from active list", track->name());
                    tracksToRemove->add(track);
                    // indicate to client process that the track was disabled because of underrun;
                    // it will then automatically call start() when data is available
                    android_atomic_or(CBLK_DISABLED, &cblk->mFlags);
                } else if (last) {
                    mixerStatus = MIXER_TRACKS_ENABLED;
                }
            }
        }
    }

    // remove all the tracks that need to be...
    removeTracks_l(*tracksToRemove);

    return mixerStatus;
}

void AudioFlinger::DirectOutputThread::threadLoop_mix()
{
    size_t frameCount = mFrameCount;
    int8_t *curBuf = (int8_t *)mMixBuffer;
    // output audio to hardware
    while (frameCount) {
        AudioBufferProvider::Buffer buffer;
        buffer.frameCount = frameCount;
        mActiveTrack->getNextBuffer(&buffer);
        if (buffer.raw == NULL) {
            memset(curBuf, 0, frameCount * mFrameSize);
            break;
        }
        memcpy(curBuf, buffer.raw, buffer.frameCount * mFrameSize);
        frameCount -= buffer.frameCount;
        curBuf += buffer.frameCount * mFrameSize;
        mActiveTrack->releaseBuffer(&buffer);
    }
    mCurrentWriteLength = curBuf - (int8_t *)mMixBuffer;
    sleepTime = 0;
    standbyTime = systemTime() + standbyDelay;
    mActiveTrack.clear();
}

void AudioFlinger::DirectOutputThread::threadLoop_sleepTime()
{
    if (sleepTime == 0) {
        if (mMixerStatus == MIXER_TRACKS_ENABLED) {
            sleepTime = activeSleepTime;
        } else {
            sleepTime = idleSleepTime;
        }
    } else if (mBytesWritten != 0 && audio_is_linear_pcm(mFormat)) {
        memset(mMixBuffer, 0, mFrameCount * mFrameSize);
        sleepTime = 0;
    }
}

// getTrackName_l() must be called with ThreadBase::mLock held
int AudioFlinger::DirectOutputThread::getTrackName_l(audio_channel_mask_t channelMask,
        int sessionId)
{
    return 0;
}

// deleteTrackName_l() must be called with ThreadBase::mLock held
void AudioFlinger::DirectOutputThread::deleteTrackName_l(int name)
{
}

// checkForNewParameters_l() must be called with ThreadBase::mLock held
bool AudioFlinger::DirectOutputThread::checkForNewParameters_l()
{
    bool reconfig = false;

    while (!mNewParameters.isEmpty()) {
        status_t status = NO_ERROR;
        String8 keyValuePair = mNewParameters[0];
        AudioParameter param = AudioParameter(keyValuePair);
        int value;

        if (param.getInt(String8(AudioParameter::keyFrameCount), value) == NO_ERROR) {
            // do not accept frame count changes if tracks are open as the track buffer
            // size depends on frame count and correct behavior would not be garantied
            // if frame count is changed after track creation
            if (!mTracks.isEmpty()) {
                status = INVALID_OPERATION;
            } else {
                reconfig = true;
            }
        }
        if (status == NO_ERROR) {
            status = mOutput->stream->common.set_parameters(&mOutput->stream->common,
                                                    keyValuePair.string());
            if (!mStandby && status == INVALID_OPERATION) {
                mOutput->stream->common.standby(&mOutput->stream->common);
                mStandby = true;
                mBytesWritten = 0;
                status = mOutput->stream->common.set_parameters(&mOutput->stream->common,
                                                       keyValuePair.string());
            }
            if (status == NO_ERROR && reconfig) {
                readOutputParameters();
                sendIoConfigEvent_l(AudioSystem::OUTPUT_CONFIG_CHANGED);
            }
        }

        mNewParameters.removeAt(0);

        mParamStatus = status;
        mParamCond.signal();
        // wait for condition with time out in case the thread calling ThreadBase::setParameters()
        // already timed out waiting for the status and will never signal the condition.
        mWaitWorkCV.waitRelative(mLock, kSetParametersTimeoutNs);
    }
    return reconfig;
}

uint32_t AudioFlinger::DirectOutputThread::activeSleepTimeUs() const
{
    uint32_t time;
    if (audio_is_linear_pcm(mFormat)) {
        time = PlaybackThread::activeSleepTimeUs();
    } else {
        time = 10000;
    }
    return time;
}

uint32_t AudioFlinger::DirectOutputThread::idleSleepTimeUs() const
{
    uint32_t time;
    if (audio_is_linear_pcm(mFormat)) {
        time = (uint32_t)(((mFrameCount * 1000) / mSampleRate) * 1000) / 2;
    } else {
        time = 10000;
    }
    return time;
}

uint32_t AudioFlinger::DirectOutputThread::suspendSleepTimeUs() const
{
    uint32_t time;
    if (audio_is_linear_pcm(mFormat)) {
        time = (uint32_t)(((mFrameCount * 1000) / mSampleRate) * 1000);
    } else {
        time = 10000;
    }
    return time;
}

void AudioFlinger::DirectOutputThread::cacheParameters_l()
{
    PlaybackThread::cacheParameters_l();

    // use shorter standby delay as on normal output to release
    // hardware resources as soon as possible
    if (audio_is_linear_pcm(mFormat)) {
        standbyDelay = microseconds(activeSleepTime*2);
    } else {
        standbyDelay = kOffloadStandbyDelayNs;
    }
}

// ----------------------------------------------------------------------------

AudioFlinger::AsyncCallbackThread::AsyncCallbackThread(
        const wp<AudioFlinger::PlaybackThread>& playbackThread)
    :   Thread(false /*canCallJava*/),
        mPlaybackThread(playbackThread),
        mWriteAckSequence(0),
        mDrainSequence(0)
{
}

AudioFlinger::AsyncCallbackThread::~AsyncCallbackThread()
{
}

void AudioFlinger::AsyncCallbackThread::onFirstRef()
{
    run("Offload Cbk", ANDROID_PRIORITY_URGENT_AUDIO);
}

bool AudioFlinger::AsyncCallbackThread::threadLoop()
{
    while (!exitPending()) {
        uint32_t writeAckSequence;
        uint32_t drainSequence;

        {
            Mutex::Autolock _l(mLock);
            mWaitWorkCV.wait(mLock);
            if (exitPending()) {
                break;
            }
            ALOGV("AsyncCallbackThread mWriteAckSequence %d mDrainSequence %d",
                  mWriteAckSequence, mDrainSequence);
            writeAckSequence = mWriteAckSequence;
            mWriteAckSequence &= ~1;
            drainSequence = mDrainSequence;
            mDrainSequence &= ~1;
        }
        {
            sp<AudioFlinger::PlaybackThread> playbackThread = mPlaybackThread.promote();
            if (playbackThread != 0) {
                if (writeAckSequence & 1) {
                    playbackThread->resetWriteBlocked(writeAckSequence >> 1);
                }
                if (drainSequence & 1) {
                    playbackThread->resetDraining(drainSequence >> 1);
                }
            }
        }
    }
    return false;
}

void AudioFlinger::AsyncCallbackThread::exit()
{
    ALOGV("AsyncCallbackThread::exit");
    Mutex::Autolock _l(mLock);
    requestExit();
    mWaitWorkCV.broadcast();
}

void AudioFlinger::AsyncCallbackThread::setWriteBlocked(uint32_t sequence)
{
    Mutex::Autolock _l(mLock);
    // bit 0 is cleared
    mWriteAckSequence = sequence << 1;
}

void AudioFlinger::AsyncCallbackThread::resetWriteBlocked()
{
    Mutex::Autolock _l(mLock);
    // ignore unexpected callbacks
    if (mWriteAckSequence & 2) {
        mWriteAckSequence |= 1;
        mWaitWorkCV.signal();
    }
}

void AudioFlinger::AsyncCallbackThread::setDraining(uint32_t sequence)
{
    Mutex::Autolock _l(mLock);
    // bit 0 is cleared
    mDrainSequence = sequence << 1;
}

void AudioFlinger::AsyncCallbackThread::resetDraining()
{
    Mutex::Autolock _l(mLock);
    // ignore unexpected callbacks
    if (mDrainSequence & 2) {
        mDrainSequence |= 1;
        mWaitWorkCV.signal();
    }
}


// ----------------------------------------------------------------------------
AudioFlinger::OffloadThread::OffloadThread(const sp<AudioFlinger>& audioFlinger,
        AudioStreamOut* output, audio_io_handle_t id, uint32_t device)
    :   DirectOutputThread(audioFlinger, output, id, device, OFFLOAD),
        mHwPaused(false),
        mFlushPending(false),
        mPausedBytesRemaining(0)
{
    //FIXME: mStandby should be set to true by ThreadBase constructor
    mStandby = true;
}

void AudioFlinger::OffloadThread::threadLoop_exit()
{
    if (mFlushPending || mHwPaused) {
        // If a flush is pending or track was paused, just discard buffered data
        flushHw_l();
    } else {
        mMixerStatus = MIXER_DRAIN_ALL;
        threadLoop_drain();
    }
    mCallbackThread->exit();
    PlaybackThread::threadLoop_exit();
}

AudioFlinger::PlaybackThread::mixer_state AudioFlinger::OffloadThread::prepareTracks_l(
    Vector< sp<Track> > *tracksToRemove
)
{
    size_t count = mActiveTracks.size();

    mixer_state mixerStatus = MIXER_IDLE;
    bool doHwPause = false;
    bool doHwResume = false;

    ALOGV("OffloadThread::prepareTracks_l active tracks %d", count);

    // find out which tracks need to be processed
    for (size_t i = 0; i < count; i++) {
        sp<Track> t = mActiveTracks[i].promote();
        // The track died recently
        if (t == 0) {
            continue;
        }
        Track* const track = t.get();
        audio_track_cblk_t* cblk = track->cblk();
        // Only consider last track started for volume and mixer state control.
        // In theory an older track could underrun and restart after the new one starts
        // but as we only care about the transition phase between two tracks on a
        // direct output, it is not a problem to ignore the underrun case.
        sp<Track> l = mLatestActiveTrack.promote();
        bool last = l.get() == track;

        if (track->isPausing()) {
            track->setPaused();
            if (last) {
                if (!mHwPaused) {
                    doHwPause = true;
                    mHwPaused = true;
                }
                // If we were part way through writing the mixbuffer to
                // the HAL we must save this until we resume
                // BUG - this will be wrong if a different track is made active,
                // in that case we want to discard the pending data in the
                // mixbuffer and tell the client to present it again when the
                // track is resumed
                mPausedWriteLength = mCurrentWriteLength;
                mPausedBytesRemaining = mBytesRemaining;
                mBytesRemaining = 0;    // stop writing
            }
            tracksToRemove->add(track);
        } else if (track->framesReady() && track->isReady() &&
                !track->isPaused() && !track->isTerminated() && !track->isStopping_2()) {
            ALOGVV("OffloadThread: track %d s=%08x [OK]", track->name(), cblk->mServer);
            if (track->mFillingUpStatus == Track::FS_FILLED) {
                track->mFillingUpStatus = Track::FS_ACTIVE;
                // make sure processVolume_l() will apply new volume even if 0
                mLeftVolFloat = mRightVolFloat = -1.0;
                if (track->mState == TrackBase::RESUMING) {
                    track->mState = TrackBase::ACTIVE;
                    if (last) {
                        if (mPausedBytesRemaining) {
                            // Need to continue write that was interrupted
                            mCurrentWriteLength = mPausedWriteLength;
                            mBytesRemaining = mPausedBytesRemaining;
                            mPausedBytesRemaining = 0;
                        }
                        if (mHwPaused) {
                            doHwResume = true;
                            mHwPaused = false;
                            // threadLoop_mix() will handle the case that we need to
                            // resume an interrupted write
                        }
                        // enable write to audio HAL
                        sleepTime = 0;
                    }
                }
            }

            if (last) {
                sp<Track> previousTrack = mPreviousTrack.promote();
                if (previousTrack != 0) {
                    if (track != previousTrack.get()) {
                        // Flush any data still being written from last track
                        mBytesRemaining = 0;
                        if (mPausedBytesRemaining) {
                            // Last track was paused so we also need to flush saved
                            // mixbuffer state and invalidate track so that it will
                            // re-submit that unwritten data when it is next resumed
                            mPausedBytesRemaining = 0;
                            // Invalidate is a bit drastic - would be more efficient
                            // to have a flag to tell client that some of the
                            // previously written data was lost
                            previousTrack->invalidate();
                        }
                        // flush data already sent to the DSP if changing audio session as audio
                        // comes from a different source. Also invalidate previous track to force a
                        // seek when resuming.
                        if (previousTrack->sessionId() != track->sessionId()) {
                            previousTrack->invalidate();
                            mFlushPending = true;
                        }
                    }
                }
                mPreviousTrack = track;
                // reset retry count
                track->mRetryCount = kMaxTrackRetriesOffload;
                mActiveTrack = t;
                mixerStatus = MIXER_TRACKS_READY;
            }
        } else {
            ALOGVV("OffloadThread: track %d s=%08x [NOT READY]", track->name(), cblk->mServer);
            if (track->isStopping_1()) {
                // Hardware buffer can hold a large amount of audio so we must
                // wait for all current track's data to drain before we say
                // that the track is stopped.
                if (mBytesRemaining == 0) {
                    // Only start draining when all data in mixbuffer
                    // has been written
                    ALOGV("OffloadThread: underrun and STOPPING_1 -> draining, STOPPING_2");
                    track->mState = TrackBase::STOPPING_2; // so presentation completes after drain
                    // do not drain if no data was ever sent to HAL (mStandby == true)
                    if (last && !mStandby) {
                        // do not modify drain sequence if we are already draining. This happens
                        // when resuming from pause after drain.
                        if ((mDrainSequence & 1) == 0) {
                            sleepTime = 0;
                            standbyTime = systemTime() + standbyDelay;
                            mixerStatus = MIXER_DRAIN_TRACK;
                            mDrainSequence += 2;
                        }
                        if (mHwPaused) {
                            // It is possible to move from PAUSED to STOPPING_1 without
                            // a resume so we must ensure hardware is running
                            doHwResume = true;
                            mHwPaused = false;
                        }
                    }
                }
            } else if (track->isStopping_2()) {
                // Drain has completed or we are in standby, signal presentation complete
                if (!(mDrainSequence & 1) || !last || mStandby) {
                    track->mState = TrackBase::STOPPED;
                    size_t audioHALFrames =
                            (mOutput->stream->get_latency(mOutput->stream)*mSampleRate) / 1000;
                    size_t framesWritten =
                            mBytesWritten / audio_stream_frame_size(&mOutput->stream->common);
                    track->presentationComplete(framesWritten, audioHALFrames);
                    track->reset();
                    tracksToRemove->add(track);
                }
            } else {
                // No buffers for this track. Give it a few chances to
                // fill a buffer, then remove it from active list.
                if (--(track->mRetryCount) <= 0) {
                    ALOGV("OffloadThread: BUFFER TIMEOUT: remove(%d) from active list",
                          track->name());
                    tracksToRemove->add(track);
                    // indicate to client process that the track was disabled because of underrun;
                    // it will then automatically call start() when data is available
                    android_atomic_or(CBLK_DISABLED, &cblk->mFlags);
                } else if (last){
                    mixerStatus = MIXER_TRACKS_ENABLED;
                }
            }
        }
        // compute volume for this track
        processVolume_l(track, last);
    }

    // make sure the pause/flush/resume sequence is executed in the right order.
    // If a flush is pending and a track is active but the HW is not paused, force a HW pause
    // before flush and then resume HW. This can happen in case of pause/flush/resume
    // if resume is received before pause is executed.
    if (!mStandby && (doHwPause || (mFlushPending && !mHwPaused && (count != 0)))) {
        mOutput->stream->pause(mOutput->stream);
        if (!doHwPause) {
            doHwResume = true;
        }
    }
    if (mFlushPending) {
        flushHw_l();
        mFlushPending = false;
    }
    if (!mStandby && doHwResume) {
        mOutput->stream->resume(mOutput->stream);
    }

    // remove all the tracks that need to be...
    removeTracks_l(*tracksToRemove);

    return mixerStatus;
}

void AudioFlinger::OffloadThread::flushOutput_l()
{
    mFlushPending = true;
}

// must be called with thread mutex locked
bool AudioFlinger::OffloadThread::waitingAsyncCallback_l()
{
    ALOGVV("waitingAsyncCallback_l mWriteAckSequence %d mDrainSequence %d",
          mWriteAckSequence, mDrainSequence);
    if (mUseAsyncWrite && ((mWriteAckSequence & 1) || (mDrainSequence & 1))) {
        return true;
    }
    return false;
}

// must be called with thread mutex locked
bool AudioFlinger::OffloadThread::shouldStandby_l()
{
    bool TrackPaused = false;

    // do not put the HAL in standby when paused. AwesomePlayer clear the offloaded AudioTrack
    // after a timeout and we will enter standby then.
    if (mTracks.size() > 0) {
        TrackPaused = mTracks[mTracks.size() - 1]->isPaused();
    }

    return !mStandby && !TrackPaused;
}


bool AudioFlinger::OffloadThread::waitingAsyncCallback()
{
    Mutex::Autolock _l(mLock);
    return waitingAsyncCallback_l();
}

void AudioFlinger::OffloadThread::flushHw_l()
{
    mOutput->stream->flush(mOutput->stream);
    // Flush anything still waiting in the mixbuffer
    mCurrentWriteLength = 0;
    mBytesRemaining = 0;
    mPausedWriteLength = 0;
    mPausedBytesRemaining = 0;
    if (mUseAsyncWrite) {
        // discard any pending drain or write ack by incrementing sequence
        mWriteAckSequence = (mWriteAckSequence + 2) & ~1;
        mDrainSequence = (mDrainSequence + 2) & ~1;
        ALOG_ASSERT(mCallbackThread != 0);
        mCallbackThread->setWriteBlocked(mWriteAckSequence);
        mCallbackThread->setDraining(mDrainSequence);
    }
}

// ----------------------------------------------------------------------------

AudioFlinger::DuplicatingThread::DuplicatingThread(const sp<AudioFlinger>& audioFlinger,
        AudioFlinger::MixerThread* mainThread, audio_io_handle_t id)
    :   MixerThread(audioFlinger, mainThread->getOutput(), id, mainThread->outDevice(),
                DUPLICATING),
        mWaitTimeMs(UINT_MAX)
{
    addOutputTrack(mainThread);
}

AudioFlinger::DuplicatingThread::~DuplicatingThread()
{
    for (size_t i = 0; i < mOutputTracks.size(); i++) {
        mOutputTracks[i]->destroy();
    }
}

void AudioFlinger::DuplicatingThread::threadLoop_mix()
{
    // mix buffers...
    if (outputsReady(outputTracks)) {
        mAudioMixer->process(AudioBufferProvider::kInvalidPTS);
    } else {
        memset(mMixBuffer, 0, mixBufferSize);
    }
    sleepTime = 0;
    writeFrames = mNormalFrameCount;
    mCurrentWriteLength = mixBufferSize;
    standbyTime = systemTime() + standbyDelay;
}

void AudioFlinger::DuplicatingThread::threadLoop_sleepTime()
{
    if (sleepTime == 0) {
        if (mMixerStatus == MIXER_TRACKS_ENABLED) {
            sleepTime = activeSleepTime;
        } else {
            sleepTime = idleSleepTime;
        }
    } else if (mBytesWritten != 0) {
        if (mMixerStatus == MIXER_TRACKS_ENABLED) {
            writeFrames = mNormalFrameCount;
            memset(mMixBuffer, 0, mixBufferSize);
        } else {
            // flush remaining overflow buffers in output tracks
            writeFrames = 0;
        }
        sleepTime = 0;
    }
}

ssize_t AudioFlinger::DuplicatingThread::threadLoop_write()
{
    for (size_t i = 0; i < outputTracks.size(); i++) {
        outputTracks[i]->write(mMixBuffer, writeFrames);
    }
    mStandby = false;
    return (ssize_t)mixBufferSize;
}

void AudioFlinger::DuplicatingThread::threadLoop_standby()
{
    // DuplicatingThread implements standby by stopping all tracks
    for (size_t i = 0; i < outputTracks.size(); i++) {
        outputTracks[i]->stop();
    }
}

void AudioFlinger::DuplicatingThread::saveOutputTracks()
{
    outputTracks = mOutputTracks;
}

void AudioFlinger::DuplicatingThread::clearOutputTracks()
{
    outputTracks.clear();
}

void AudioFlinger::DuplicatingThread::addOutputTrack(MixerThread *thread)
{
    Mutex::Autolock _l(mLock);
    // FIXME explain this formula
    size_t frameCount = (3 * mNormalFrameCount * mSampleRate) / thread->sampleRate();
    OutputTrack *outputTrack = new OutputTrack(thread,
                                            this,
                                            mSampleRate,
                                            mFormat,
                                            mChannelMask,
                                            frameCount,
                                            IPCThreadState::self()->getCallingUid());
    if (outputTrack->cblk() != NULL) {
        thread->setStreamVolume(AUDIO_STREAM_CNT, 1.0f);
        mOutputTracks.add(outputTrack);
        ALOGV("addOutputTrack() track %p, on thread %p", outputTrack, thread);
        updateWaitTime_l();
    }
}

void AudioFlinger::DuplicatingThread::removeOutputTrack(MixerThread *thread)
{
    Mutex::Autolock _l(mLock);
    for (size_t i = 0; i < mOutputTracks.size(); i++) {
        if (mOutputTracks[i]->thread() == thread) {
            mOutputTracks[i]->destroy();
            mOutputTracks.removeAt(i);
            updateWaitTime_l();
            return;
        }
    }
    ALOGV("removeOutputTrack(): unkonwn thread: %p", thread);
}

// caller must hold mLock
void AudioFlinger::DuplicatingThread::updateWaitTime_l()
{
    mWaitTimeMs = UINT_MAX;
    for (size_t i = 0; i < mOutputTracks.size(); i++) {
        sp<ThreadBase> strong = mOutputTracks[i]->thread().promote();
        if (strong != 0) {
            uint32_t waitTimeMs = (strong->frameCount() * 2 * 1000) / strong->sampleRate();
            if (waitTimeMs < mWaitTimeMs) {
                mWaitTimeMs = waitTimeMs;
            }
        }
    }
}


bool AudioFlinger::DuplicatingThread::outputsReady(
        const SortedVector< sp<OutputTrack> > &outputTracks)
{
    for (size_t i = 0; i < outputTracks.size(); i++) {
        sp<ThreadBase> thread = outputTracks[i]->thread().promote();
        if (thread == 0) {
            ALOGW("DuplicatingThread::outputsReady() could not promote thread on output track %p",
                    outputTracks[i].get());
            return false;
        }
        PlaybackThread *playbackThread = (PlaybackThread *)thread.get();
        // see note at standby() declaration
        if (playbackThread->standby() && !playbackThread->isSuspended()) {
            ALOGV("DuplicatingThread output track %p on thread %p Not Ready", outputTracks[i].get(),
                    thread.get());
            return false;
        }
    }
    return true;
}

uint32_t AudioFlinger::DuplicatingThread::activeSleepTimeUs() const
{
    return (mWaitTimeMs * 1000) / 2;
}

void AudioFlinger::DuplicatingThread::cacheParameters_l()
{
    // updateWaitTime_l() sets mWaitTimeMs, which affects activeSleepTimeUs(), so call it first
    updateWaitTime_l();

    MixerThread::cacheParameters_l();
}

// ----------------------------------------------------------------------------
//      Record
// ----------------------------------------------------------------------------

AudioFlinger::RecordThread::RecordThread(const sp<AudioFlinger>& audioFlinger,
                                         AudioStreamIn *input,
                                         uint32_t sampleRate,
                                         audio_channel_mask_t channelMask,
                                         audio_io_handle_t id,
                                         audio_devices_t outDevice,
                                         audio_devices_t inDevice
#ifdef TEE_SINK
                                         , const sp<NBAIO_Sink>& teeSink
#endif
                                         ) :
    ThreadBase(audioFlinger, id, outDevice, inDevice, RECORD),
    mInput(input), mResampler(NULL), mRsmpOutBuffer(NULL), mRsmpInBuffer(NULL),
    // mRsmpInIndex and mBufferSize set by readInputParameters()
    mReqChannelCount(popcount(channelMask)),
    mReqSampleRate(sampleRate)
    // mBytesRead is only meaningful while active, and so is cleared in start()
    // (but might be better to also clear here for dump?)
#ifdef TEE_SINK
    , mTeeSink(teeSink)
#endif
{
    snprintf(mName, kNameLength, "AudioIn_%X", id);

    readInputParameters();
}


AudioFlinger::RecordThread::~RecordThread()
{
    delete[] mRsmpInBuffer;
    delete mResampler;
    delete[] mRsmpOutBuffer;
}

void AudioFlinger::RecordThread::onFirstRef()
{
    run(mName, PRIORITY_URGENT_AUDIO);
}

status_t AudioFlinger::RecordThread::readyToRun()
{
    status_t status = initCheck();
    ALOGW_IF(status != NO_ERROR,"RecordThread %p could not initialize", this);
    return status;
}

bool AudioFlinger::RecordThread::threadLoop()
{
    AudioBufferProvider::Buffer buffer;
    sp<RecordTrack> activeTrack;
    Vector< sp<EffectChain> > effectChains;

    nsecs_t lastWarning = 0;

    inputStandBy();
    {
        Mutex::Autolock _l(mLock);
        activeTrack = mActiveTrack;
        acquireWakeLock_l(activeTrack != 0 ? activeTrack->uid() : -1);
    }

    // used to verify we've read at least once before evaluating how many bytes were read
    bool readOnce = false;

    // start recording
    while (!exitPending()) {

        processConfigEvents();

        { // scope for mLock
            Mutex::Autolock _l(mLock);
            checkForNewParameters_l();
            if (mActiveTrack != 0 && activeTrack != mActiveTrack) {
                SortedVector<int> tmp;
                tmp.add(mActiveTrack->uid());
                updateWakeLockUids_l(tmp);
            }
            activeTrack = mActiveTrack;
            if (mActiveTrack == 0 && mConfigEvents.isEmpty()) {
                standby();

                if (exitPending()) {
                    break;
                }

                releaseWakeLock_l();
                ALOGV("RecordThread: loop stopping");
                // go to sleep
                mWaitWorkCV.wait(mLock);
                ALOGV("RecordThread: loop starting");
                acquireWakeLock_l(mActiveTrack != 0 ? mActiveTrack->uid() : -1);
                continue;
            }
            if (mActiveTrack != 0) {
                if (mActiveTrack->isTerminated()) {
                    removeTrack_l(mActiveTrack);
                    mActiveTrack.clear();
                } else if (mActiveTrack->mState == TrackBase::PAUSING) {
                    standby();
                    mActiveTrack.clear();
                    mStartStopCond.broadcast();
                } else if (mActiveTrack->mState == TrackBase::RESUMING) {
                    if (mReqChannelCount != mActiveTrack->channelCount()) {
                        mActiveTrack.clear();
                        mStartStopCond.broadcast();
                    } else if (readOnce) {
                        // record start succeeds only if first read from audio input
                        // succeeds
                        if (mBytesRead >= 0) {
                            mActiveTrack->mState = TrackBase::ACTIVE;
                        } else {
                            mActiveTrack.clear();
                        }
                        mStartStopCond.broadcast();
                    }
                    mStandby = false;
                }
            }

            lockEffectChains_l(effectChains);
        }

        if (mActiveTrack != 0) {
            if (mActiveTrack->mState != TrackBase::ACTIVE &&
                mActiveTrack->mState != TrackBase::RESUMING) {
                unlockEffectChains(effectChains);
                usleep(kRecordThreadSleepUs);
                continue;
            }
            for (size_t i = 0; i < effectChains.size(); i ++) {
                effectChains[i]->process_l();
            }

            buffer.frameCount = mFrameCount;
            status_t status = mActiveTrack->getNextBuffer(&buffer);
            if (status == NO_ERROR) {
                readOnce = true;
                size_t framesOut = buffer.frameCount;
                if (mResampler == NULL) {
                    // no resampling
                    while (framesOut) {
                        size_t framesIn = mFrameCount - mRsmpInIndex;
                        if (framesIn) {
                            int8_t *src = (int8_t *)mRsmpInBuffer + mRsmpInIndex * mFrameSize;
                            int8_t *dst = buffer.i8 + (buffer.frameCount - framesOut) *
                                    mActiveTrack->mFrameSize;
                            if (framesIn > framesOut)
                                framesIn = framesOut;
                            mRsmpInIndex += framesIn;
                            framesOut -= framesIn;
                            if (mChannelCount == mReqChannelCount) {
                                memcpy(dst, src, framesIn * mFrameSize);
                            } else {
                                if (mChannelCount == 1) {
                                    upmix_to_stereo_i16_from_mono_i16((int16_t *)dst,
                                            (int16_t *)src, framesIn);
                                } else {
                                    downmix_to_mono_i16_from_stereo_i16((int16_t *)dst,
                                            (int16_t *)src, framesIn);
                                }
                            }
                        }
                        if (framesOut && mFrameCount == mRsmpInIndex) {
                            void *readInto;
                            if (framesOut == mFrameCount && mChannelCount == mReqChannelCount) {
                                readInto = buffer.raw;
                                framesOut = 0;
                            } else {
                                readInto = mRsmpInBuffer;
                                mRsmpInIndex = 0;
                            }
                            mBytesRead = mInput->stream->read(mInput->stream, readInto,
                                    mBufferSize);
                            if (mBytesRead <= 0) {
                                if ((mBytesRead < 0) && (mActiveTrack->mState == TrackBase::ACTIVE))
                                {
                                    ALOGE("Error reading audio input");
                                    // Force input into standby so that it tries to
                                    // recover at next read attempt
                                    inputStandBy();
                                    usleep(kRecordThreadSleepUs);
                                }
                                mRsmpInIndex = mFrameCount;
                                framesOut = 0;
                                buffer.frameCount = 0;
                            }
#ifdef TEE_SINK
                            else if (mTeeSink != 0) {
                                (void) mTeeSink->write(readInto,
                                        mBytesRead >> Format_frameBitShift(mTeeSink->format()));
                            }
#endif
                        }
                    }
                } else {
                    // resampling

                    // resampler accumulates, but we only have one source track
                    memset(mRsmpOutBuffer, 0, framesOut * FCC_2 * sizeof(int32_t));
                    // alter output frame count as if we were expecting stereo samples
                    if (mChannelCount == 1 && mReqChannelCount == 1) {
                        framesOut >>= 1;
                    }
                    mResampler->resample(mRsmpOutBuffer, framesOut,
                            this /* AudioBufferProvider* */);
                    // ditherAndClamp() works as long as all buffers returned by
                    // mActiveTrack->getNextBuffer() are 32 bit aligned which should be always true.
                    if (mChannelCount == 2 && mReqChannelCount == 1) {
                        // temporarily type pun mRsmpOutBuffer from Q19.12 to int16_t
                        ditherAndClamp(mRsmpOutBuffer, mRsmpOutBuffer, framesOut);
                        // the resampler always outputs stereo samples:
                        // do post stereo to mono conversion
                        downmix_to_mono_i16_from_stereo_i16(buffer.i16, (int16_t *)mRsmpOutBuffer,
                                framesOut);
                    } else {
                        ditherAndClamp((int32_t *)buffer.raw, mRsmpOutBuffer, framesOut);
                    }
                    // now done with mRsmpOutBuffer

                }
                if (mFramestoDrop == 0) {
                    mActiveTrack->releaseBuffer(&buffer);
                } else {
                    if (mFramestoDrop > 0) {
                        mFramestoDrop -= buffer.frameCount;
                        if (mFramestoDrop <= 0) {
                            clearSyncStartEvent();
                        }
                    } else {
                        mFramestoDrop += buffer.frameCount;
                        if (mFramestoDrop >= 0 || mSyncStartEvent == 0 ||
                                mSyncStartEvent->isCancelled()) {
                            ALOGW("Synced record %s, session %d, trigger session %d",
                                  (mFramestoDrop >= 0) ? "timed out" : "cancelled",
                                  mActiveTrack->sessionId(),
                                  (mSyncStartEvent != 0) ? mSyncStartEvent->triggerSession() : 0);
                            clearSyncStartEvent();
                        }
                    }
                }
                mActiveTrack->clearOverflow();
            }
            // client isn't retrieving buffers fast enough
            else {
                if (!mActiveTrack->setOverflow()) {
                    nsecs_t now = systemTime();
                    if ((now - lastWarning) > kWarningThrottleNs) {
                        ALOGW("RecordThread: buffer overflow");
                        lastWarning = now;
                    }
                }
                // Release the processor for a while before asking for a new buffer.
                // This will give the application more chance to read from the buffer and
                // clear the overflow.
                usleep(kRecordThreadSleepUs);
            }
        }
        // enable changes in effect chain
        unlockEffectChains(effectChains);
        effectChains.clear();
    }

    standby();

    {
        Mutex::Autolock _l(mLock);
        for (size_t i = 0; i < mTracks.size(); i++) {
            sp<RecordTrack> track = mTracks[i];
            track->invalidate();
        }
        mActiveTrack.clear();
        mStartStopCond.broadcast();
    }

    releaseWakeLock();

    ALOGV("RecordThread %p exiting", this);
    return false;
}

void AudioFlinger::RecordThread::standby()
{
    if (!mStandby) {
        inputStandBy();
        mStandby = true;
    }
}

void AudioFlinger::RecordThread::inputStandBy()
{
    mInput->stream->common.standby(&mInput->stream->common);
}

sp<AudioFlinger::RecordThread::RecordTrack>  AudioFlinger::RecordThread::createRecordTrack_l(
        const sp<AudioFlinger::Client>& client,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask,
        size_t frameCount,
        int sessionId,
        int uid,
        IAudioFlinger::track_flags_t *flags,
        pid_t tid,
        status_t *status)
{
    sp<RecordTrack> track;
    status_t lStatus;

    lStatus = initCheck();
    if (lStatus != NO_ERROR) {
        ALOGE("createRecordTrack_l() audio driver not initialized");
        goto Exit;
    }
    // client expresses a preference for FAST, but we get the final say
    if (*flags & IAudioFlinger::TRACK_FAST) {
      if (
            // use case: callback handler and frame count is default or at least as large as HAL
            (
                (tid != -1) &&
                ((frameCount == 0) ||
                (frameCount >= (mFrameCount * kFastTrackMultiplier)))
            ) &&
            // FIXME when record supports non-PCM data, also check for audio_is_linear_pcm(format)
            // mono or stereo
            ( (channelMask == AUDIO_CHANNEL_OUT_MONO) ||
              (channelMask == AUDIO_CHANNEL_OUT_STEREO) ) &&
            // hardware sample rate
            (sampleRate == mSampleRate) &&
            // record thread has an associated fast recorder
            hasFastRecorder()
            // FIXME test that RecordThread for this fast track has a capable output HAL
            // FIXME add a permission test also?
        ) {
        // if frameCount not specified, then it defaults to fast recorder (HAL) frame count
        if (frameCount == 0) {
            frameCount = mFrameCount * kFastTrackMultiplier;
        }
        ALOGV("AUDIO_INPUT_FLAG_FAST accepted: frameCount=%d mFrameCount=%d",
                frameCount, mFrameCount);
      } else {
        ALOGV("AUDIO_INPUT_FLAG_FAST denied: frameCount=%d "
                "mFrameCount=%d format=%d isLinear=%d channelMask=%#x sampleRate=%u mSampleRate=%u "
                "hasFastRecorder=%d tid=%d",
                frameCount, mFrameCount, format,
                audio_is_linear_pcm(format),
                channelMask, sampleRate, mSampleRate, hasFastRecorder(), tid);
        *flags &= ~IAudioFlinger::TRACK_FAST;
        // For compatibility with AudioRecord calculation, buffer depth is forced
        // to be at least 2 x the record thread frame count and cover audio hardware latency.
        // This is probably too conservative, but legacy application code may depend on it.
        // If you change this calculation, also review the start threshold which is related.
        uint32_t latencyMs = 50; // FIXME mInput->stream->get_latency(mInput->stream);
        size_t mNormalFrameCount = 2048; // FIXME
        uint32_t minBufCount = latencyMs / ((1000 * mNormalFrameCount) / mSampleRate);
        if (minBufCount < 2) {
            minBufCount = 2;
        }
        size_t minFrameCount = mNormalFrameCount * minBufCount;
        if (frameCount < minFrameCount) {
            frameCount = minFrameCount;
        }
      }
    }

    // FIXME use flags and tid similar to createTrack_l()

    { // scope for mLock
        Mutex::Autolock _l(mLock);

        track = new RecordTrack(this, client, sampleRate,
                      format, channelMask, frameCount, sessionId, uid);

        if (track->getCblk() == 0) {
            ALOGE("createRecordTrack_l() no control block");
            lStatus = NO_MEMORY;
            track.clear();
            goto Exit;
        }
        mTracks.add(track);

        // disable AEC and NS if the device is a BT SCO headset supporting those pre processings
        bool suspend = audio_is_bluetooth_sco_device(mInDevice) &&
                        mAudioFlinger->btNrecIsOff();
        setEffectSuspended_l(FX_IID_AEC, suspend, sessionId);
        setEffectSuspended_l(FX_IID_NS, suspend, sessionId);

        if ((*flags & IAudioFlinger::TRACK_FAST) && (tid != -1)) {
            pid_t callingPid = IPCThreadState::self()->getCallingPid();
            // we don't have CAP_SYS_NICE, nor do we want to have it as it's too powerful,
            // so ask activity manager to do this on our behalf
            sendPrioConfigEvent_l(callingPid, tid, kPriorityAudioApp);
        }
    }
    lStatus = NO_ERROR;

Exit:
    if (status) {
        *status = lStatus;
    }
    return track;
}

status_t AudioFlinger::RecordThread::start(RecordThread::RecordTrack* recordTrack,
                                           AudioSystem::sync_event_t event,
                                           int triggerSession)
{
    ALOGV("RecordThread::start event %d, triggerSession %d", event, triggerSession);
    sp<ThreadBase> strongMe = this;
    status_t status = NO_ERROR;

    if (event == AudioSystem::SYNC_EVENT_NONE) {
        clearSyncStartEvent();
    } else if (event != AudioSystem::SYNC_EVENT_SAME) {
        mSyncStartEvent = mAudioFlinger->createSyncEvent(event,
                                       triggerSession,
                                       recordTrack->sessionId(),
                                       syncStartEventCallback,
                                       this);
        // Sync event can be cancelled by the trigger session if the track is not in a
        // compatible state in which case we start record immediately
        if (mSyncStartEvent->isCancelled()) {
            clearSyncStartEvent();
        } else {
            // do not wait for the event for more than AudioSystem::kSyncRecordStartTimeOutMs
            mFramestoDrop = - ((AudioSystem::kSyncRecordStartTimeOutMs * mReqSampleRate) / 1000);
        }
    }

    {
        AutoMutex lock(mLock);
        if (mActiveTrack != 0) {
            if (recordTrack != mActiveTrack.get()) {
                status = -EBUSY;
            } else if (mActiveTrack->mState == TrackBase::PAUSING) {
                mActiveTrack->mState = TrackBase::ACTIVE;
            }
            return status;
        }

        recordTrack->mState = TrackBase::IDLE;
        mActiveTrack = recordTrack;
        mLock.unlock();
        status_t status = AudioSystem::startInput(mId);
        mLock.lock();
        if (status != NO_ERROR) {
            mActiveTrack.clear();
            clearSyncStartEvent();
            return status;
        }
        mRsmpInIndex = mFrameCount;
        mBytesRead = 0;
        if (mResampler != NULL) {
            mResampler->reset();
        }
        mActiveTrack->mState = TrackBase::RESUMING;
        // signal thread to start
        ALOGV("Signal record thread");
        mWaitWorkCV.broadcast();
        // do not wait for mStartStopCond if exiting
        if (exitPending()) {
            mActiveTrack.clear();
            status = INVALID_OPERATION;
            goto startError;
        }
        mStartStopCond.wait(mLock);
        if (mActiveTrack == 0) {
            ALOGV("Record failed to start");
            status = BAD_VALUE;
            goto startError;
        }
        ALOGV("Record started OK");
        return status;
    }

startError:
    AudioSystem::stopInput(mId);
    clearSyncStartEvent();
    return status;
}

void AudioFlinger::RecordThread::clearSyncStartEvent()
{
    if (mSyncStartEvent != 0) {
        mSyncStartEvent->cancel();
    }
    mSyncStartEvent.clear();
    mFramestoDrop = 0;
}

void AudioFlinger::RecordThread::syncStartEventCallback(const wp<SyncEvent>& event)
{
    sp<SyncEvent> strongEvent = event.promote();

    if (strongEvent != 0) {
        RecordThread *me = (RecordThread *)strongEvent->cookie();
        me->handleSyncStartEvent(strongEvent);
    }
}

void AudioFlinger::RecordThread::handleSyncStartEvent(const sp<SyncEvent>& event)
{
    if (event == mSyncStartEvent) {
        // TODO: use actual buffer filling status instead of 2 buffers when info is available
        // from audio HAL
        mFramestoDrop = mFrameCount * 2;
    }
}

bool AudioFlinger::RecordThread::stop(RecordThread::RecordTrack* recordTrack) {
    ALOGV("RecordThread::stop");
    AutoMutex _l(mLock);
    if (recordTrack != mActiveTrack.get() || recordTrack->mState == TrackBase::PAUSING) {
        return false;
    }
    recordTrack->mState = TrackBase::PAUSING;
    // do not wait for mStartStopCond if exiting
    if (exitPending()) {
        return true;
    }
    mStartStopCond.wait(mLock);
    // if we have been restarted, recordTrack == mActiveTrack.get() here
    if (exitPending() || recordTrack != mActiveTrack.get()) {
        ALOGV("Record stopped OK");
        return true;
    }
    return false;
}

bool AudioFlinger::RecordThread::isValidSyncEvent(const sp<SyncEvent>& event) const
{
    return false;
}

status_t AudioFlinger::RecordThread::setSyncEvent(const sp<SyncEvent>& event)
{
#if 0   // This branch is currently dead code, but is preserved in case it will be needed in future
    if (!isValidSyncEvent(event)) {
        return BAD_VALUE;
    }

    int eventSession = event->triggerSession();
    status_t ret = NAME_NOT_FOUND;

    Mutex::Autolock _l(mLock);

    for (size_t i = 0; i < mTracks.size(); i++) {
        sp<RecordTrack> track = mTracks[i];
        if (eventSession == track->sessionId()) {
            (void) track->setSyncEvent(event);
            ret = NO_ERROR;
        }
    }
    return ret;
#else
    return BAD_VALUE;
#endif
}

// destroyTrack_l() must be called with ThreadBase::mLock held
void AudioFlinger::RecordThread::destroyTrack_l(const sp<RecordTrack>& track)
{
    track->terminate();
    track->mState = TrackBase::STOPPED;
    // active tracks are removed by threadLoop()
    if (mActiveTrack != track) {
        removeTrack_l(track);
    }
}

void AudioFlinger::RecordThread::removeTrack_l(const sp<RecordTrack>& track)
{
    mTracks.remove(track);
    // need anything related to effects here?
}

void AudioFlinger::RecordThread::dump(int fd, const Vector<String16>& args)
{
    dumpInternals(fd, args);
    dumpTracks(fd, args);
    dumpEffectChains(fd, args);
}

void AudioFlinger::RecordThread::dumpInternals(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    snprintf(buffer, SIZE, "\nInput thread %p internals\n", this);
    result.append(buffer);

    if (mActiveTrack != 0) {
        snprintf(buffer, SIZE, "In index: %d\n", mRsmpInIndex);
        result.append(buffer);
        snprintf(buffer, SIZE, "Buffer size: %u bytes\n", mBufferSize);
        result.append(buffer);
        snprintf(buffer, SIZE, "Resampling: %d\n", (mResampler != NULL));
        result.append(buffer);
        snprintf(buffer, SIZE, "Out channel count: %u\n", mReqChannelCount);
        result.append(buffer);
        snprintf(buffer, SIZE, "Out sample rate: %u\n", mReqSampleRate);
        result.append(buffer);
    } else {
        result.append("No active record client\n");
    }

    write(fd, result.string(), result.size());

    dumpBase(fd, args);
}

void AudioFlinger::RecordThread::dumpTracks(int fd, const Vector<String16>& args)
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    snprintf(buffer, SIZE, "Input thread %p tracks\n", this);
    result.append(buffer);
    RecordTrack::appendDumpHeader(result);
    for (size_t i = 0; i < mTracks.size(); ++i) {
        sp<RecordTrack> track = mTracks[i];
        if (track != 0) {
            track->dump(buffer, SIZE);
            result.append(buffer);
        }
    }

    if (mActiveTrack != 0) {
        snprintf(buffer, SIZE, "\nInput thread %p active tracks\n", this);
        result.append(buffer);
        RecordTrack::appendDumpHeader(result);
        mActiveTrack->dump(buffer, SIZE);
        result.append(buffer);

    }
    write(fd, result.string(), result.size());
}

// AudioBufferProvider interface
status_t AudioFlinger::RecordThread::getNextBuffer(AudioBufferProvider::Buffer* buffer, int64_t pts)
{
    size_t framesReq = buffer->frameCount;
    size_t framesReady = mFrameCount - mRsmpInIndex;
    int channelCount;

    if (framesReady == 0) {
        mBytesRead = mInput->stream->read(mInput->stream, mRsmpInBuffer, mBufferSize);
        if (mBytesRead <= 0) {
            if ((mBytesRead < 0) && (mActiveTrack->mState == TrackBase::ACTIVE)) {
                ALOGE("RecordThread::getNextBuffer() Error reading audio input");
                // Force input into standby so that it tries to
                // recover at next read attempt
                inputStandBy();
                usleep(kRecordThreadSleepUs);
            }
            buffer->raw = NULL;
            buffer->frameCount = 0;
            return NOT_ENOUGH_DATA;
        }
        mRsmpInIndex = 0;
        framesReady = mFrameCount;
    }

    if (framesReq > framesReady) {
        framesReq = framesReady;
    }

    if (mChannelCount == 1 && mReqChannelCount == 2) {
        channelCount = 1;
    } else {
        channelCount = 2;
    }
    buffer->raw = mRsmpInBuffer + mRsmpInIndex * channelCount;
    buffer->frameCount = framesReq;
    return NO_ERROR;
}

// AudioBufferProvider interface
void AudioFlinger::RecordThread::releaseBuffer(AudioBufferProvider::Buffer* buffer)
{
    mRsmpInIndex += buffer->frameCount;
    buffer->frameCount = 0;
}

bool AudioFlinger::RecordThread::checkForNewParameters_l()
{
    bool reconfig = false;

    while (!mNewParameters.isEmpty()) {
        status_t status = NO_ERROR;
        String8 keyValuePair = mNewParameters[0];
        AudioParameter param = AudioParameter(keyValuePair);
        int value;
        audio_format_t reqFormat = mFormat;
        uint32_t reqSamplingRate = mReqSampleRate;
        uint32_t reqChannelCount = mReqChannelCount;

        if (param.getInt(String8(AudioParameter::keySamplingRate), value) == NO_ERROR) {
            reqSamplingRate = value;
            reconfig = true;
        }
        if (param.getInt(String8(AudioParameter::keyFormat), value) == NO_ERROR) {
            if ((audio_format_t) value != AUDIO_FORMAT_PCM_16_BIT) {
                status = BAD_VALUE;
            } else {
                reqFormat = (audio_format_t) value;
                reconfig = true;
            }
        }
        if (param.getInt(String8(AudioParameter::keyChannels), value) == NO_ERROR) {
            reqChannelCount = popcount(value);
            reconfig = true;
        }
        if (param.getInt(String8(AudioParameter::keyFrameCount), value) == NO_ERROR) {
            // do not accept frame count changes if tracks are open as the track buffer
            // size depends on frame count and correct behavior would not be guaranteed
            // if frame count is changed after track creation
            if (mActiveTrack != 0) {
                status = INVALID_OPERATION;
            } else {
                reconfig = true;
            }
        }
        if (param.getInt(String8(AudioParameter::keyRouting), value) == NO_ERROR) {
            // forward device change to effects that have requested to be
            // aware of attached audio device.
            for (size_t i = 0; i < mEffectChains.size(); i++) {
                mEffectChains[i]->setDevice_l(value);
            }

            // store input device and output device but do not forward output device to audio HAL.
            // Note that status is ignored by the caller for output device
            // (see AudioFlinger::setParameters()
            if (audio_is_output_devices(value)) {
                mOutDevice = value;
                status = BAD_VALUE;
            } else {
                mInDevice = value;
                // disable AEC and NS if the device is a BT SCO headset supporting those
                // pre processings
                if (mTracks.size() > 0) {
                    bool suspend = audio_is_bluetooth_sco_device(mInDevice) &&
                                        mAudioFlinger->btNrecIsOff();
                    for (size_t i = 0; i < mTracks.size(); i++) {
                        sp<RecordTrack> track = mTracks[i];
                        setEffectSuspended_l(FX_IID_AEC, suspend, track->sessionId());
                        setEffectSuspended_l(FX_IID_NS, suspend, track->sessionId());
                    }
                }
            }
        }
        if (param.getInt(String8(AudioParameter::keyInputSource), value) == NO_ERROR &&
                mAudioSource != (audio_source_t)value) {
            // forward device change to effects that have requested to be
            // aware of attached audio device.
            for (size_t i = 0; i < mEffectChains.size(); i++) {
                mEffectChains[i]->setAudioSource_l((audio_source_t)value);
            }
            mAudioSource = (audio_source_t)value;
        }
        if (status == NO_ERROR) {
            status = mInput->stream->common.set_parameters(&mInput->stream->common,
                    keyValuePair.string());
            if (status == INVALID_OPERATION) {
                inputStandBy();
                status = mInput->stream->common.set_parameters(&mInput->stream->common,
                        keyValuePair.string());
            }
            if (reconfig) {
                if (status == BAD_VALUE &&
                    reqFormat == mInput->stream->common.get_format(&mInput->stream->common) &&
                    reqFormat == AUDIO_FORMAT_PCM_16_BIT &&
                    (mInput->stream->common.get_sample_rate(&mInput->stream->common)
                            <= (2 * reqSamplingRate)) &&
                    popcount(mInput->stream->common.get_channels(&mInput->stream->common))
                            <= FCC_2 &&
                    (reqChannelCount <= FCC_2)) {
                    status = NO_ERROR;
                }
                if (status == NO_ERROR) {
                    readInputParameters();
                    sendIoConfigEvent_l(AudioSystem::INPUT_CONFIG_CHANGED);
                }
            }
        }

        mNewParameters.removeAt(0);

        mParamStatus = status;
        mParamCond.signal();
        // wait for condition with time out in case the thread calling ThreadBase::setParameters()
        // already timed out waiting for the status and will never signal the condition.
        mWaitWorkCV.waitRelative(mLock, kSetParametersTimeoutNs);
    }
    return reconfig;
}

String8 AudioFlinger::RecordThread::getParameters(const String8& keys)
{
    Mutex::Autolock _l(mLock);
    if (initCheck() != NO_ERROR) {
        return String8();
    }

    char *s = mInput->stream->common.get_parameters(&mInput->stream->common, keys.string());
    const String8 out_s8(s);
    free(s);
    return out_s8;
}

void AudioFlinger::RecordThread::audioConfigChanged_l(int event, int param) {
    AudioSystem::OutputDescriptor desc;
    void *param2 = NULL;

    switch (event) {
    case AudioSystem::INPUT_OPENED:
    case AudioSystem::INPUT_CONFIG_CHANGED:
        desc.channelMask = mChannelMask;
        desc.samplingRate = mSampleRate;
        desc.format = mFormat;
        desc.frameCount = mFrameCount;
        desc.latency = 0;
        param2 = &desc;
        break;

    case AudioSystem::INPUT_CLOSED:
    default:
        break;
    }
    mAudioFlinger->audioConfigChanged_l(event, mId, param2);
}

void AudioFlinger::RecordThread::readInputParameters()
{
    delete[] mRsmpInBuffer;
    // mRsmpInBuffer is always assigned a new[] below
    delete[] mRsmpOutBuffer;
    mRsmpOutBuffer = NULL;
    delete mResampler;
    mResampler = NULL;

    mSampleRate = mInput->stream->common.get_sample_rate(&mInput->stream->common);
    mChannelMask = mInput->stream->common.get_channels(&mInput->stream->common);
    mChannelCount = popcount(mChannelMask);
    mFormat = mInput->stream->common.get_format(&mInput->stream->common);
    if (mFormat != AUDIO_FORMAT_PCM_16_BIT) {
        ALOGE("HAL format %d not supported; must be AUDIO_FORMAT_PCM_16_BIT", mFormat);
    }
    mFrameSize = audio_stream_frame_size(&mInput->stream->common);
    mBufferSize = mInput->stream->common.get_buffer_size(&mInput->stream->common);
    mFrameCount = mBufferSize / mFrameSize;
    mRsmpInBuffer = new int16_t[mFrameCount * mChannelCount];

    if (mSampleRate != mReqSampleRate && mChannelCount <= FCC_2 && mReqChannelCount <= FCC_2)
    {
        int channelCount;
        // optimization: if mono to mono, use the resampler in stereo to stereo mode to avoid
        // stereo to mono post process as the resampler always outputs stereo.
        if (mChannelCount == 1 && mReqChannelCount == 2) {
            channelCount = 1;
        } else {
            channelCount = 2;
        }
        mResampler = AudioResampler::create(16, channelCount, mReqSampleRate);
        mResampler->setSampleRate(mSampleRate);
        mResampler->setVolume(AudioMixer::UNITY_GAIN, AudioMixer::UNITY_GAIN);
        mRsmpOutBuffer = new int32_t[mFrameCount * FCC_2];

        // optmization: if mono to mono, alter input frame count as if we were inputing
        // stereo samples
        if (mChannelCount == 1 && mReqChannelCount == 1) {
            mFrameCount >>= 1;
        }

    }
    mRsmpInIndex = mFrameCount;
}

unsigned int AudioFlinger::RecordThread::getInputFramesLost()
{
    Mutex::Autolock _l(mLock);
    if (initCheck() != NO_ERROR) {
        return 0;
    }

    return mInput->stream->get_input_frames_lost(mInput->stream);
}

uint32_t AudioFlinger::RecordThread::hasAudioSession(int sessionId) const
{
    Mutex::Autolock _l(mLock);
    uint32_t result = 0;
    if (getEffectChain_l(sessionId) != 0) {
        result = EFFECT_SESSION;
    }

    for (size_t i = 0; i < mTracks.size(); ++i) {
        if (sessionId == mTracks[i]->sessionId()) {
            result |= TRACK_SESSION;
            break;
        }
    }

    return result;
}

KeyedVector<int, bool> AudioFlinger::RecordThread::sessionIds() const
{
    KeyedVector<int, bool> ids;
    Mutex::Autolock _l(mLock);
    for (size_t j = 0; j < mTracks.size(); ++j) {
        sp<RecordThread::RecordTrack> track = mTracks[j];
        int sessionId = track->sessionId();
        if (ids.indexOfKey(sessionId) < 0) {
            ids.add(sessionId, true);
        }
    }
    return ids;
}

AudioFlinger::AudioStreamIn* AudioFlinger::RecordThread::clearInput()
{
    Mutex::Autolock _l(mLock);
    AudioStreamIn *input = mInput;
    mInput = NULL;
    return input;
}

// this method must always be called either with ThreadBase mLock held or inside the thread loop
audio_stream_t* AudioFlinger::RecordThread::stream() const
{
    if (mInput == NULL) {
        return NULL;
    }
    return &mInput->stream->common;
}

status_t AudioFlinger::RecordThread::addEffectChain_l(const sp<EffectChain>& chain)
{
    // only one chain per input thread
    if (mEffectChains.size() != 0) {
        return INVALID_OPERATION;
    }
    ALOGV("addEffectChain_l() %p on thread %p", chain.get(), this);

    chain->setInBuffer(NULL);
    chain->setOutBuffer(NULL);

    checkSuspendOnAddEffectChain_l(chain);

    mEffectChains.add(chain);

    return NO_ERROR;
}

size_t AudioFlinger::RecordThread::removeEffectChain_l(const sp<EffectChain>& chain)
{
    ALOGV("removeEffectChain_l() %p from thread %p", chain.get(), this);
    ALOGW_IF(mEffectChains.size() != 1,
            "removeEffectChain_l() %p invalid chain size %d on thread %p",
            chain.get(), mEffectChains.size(), this);
    if (mEffectChains.size() == 1) {
        mEffectChains.removeAt(0);
    }
    return 0;
}

}; // namespace android

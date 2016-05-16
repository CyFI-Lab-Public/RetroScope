/*
**
** Copyright 2008, The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "AudioRecord"

#include <sys/resource.h>
#include <binder/IPCThreadState.h>
#include <media/AudioRecord.h>
#include <utils/Log.h>
#include <private/media/AudioTrackShared.h>
#include <media/IAudioFlinger.h>

#define WAIT_PERIOD_MS          10

namespace android {
// ---------------------------------------------------------------------------

// static
status_t AudioRecord::getMinFrameCount(
        size_t* frameCount,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask)
{
    if (frameCount == NULL) {
        return BAD_VALUE;
    }

    // default to 0 in case of error
    *frameCount = 0;

    size_t size = 0;
    status_t status = AudioSystem::getInputBufferSize(sampleRate, format, channelMask, &size);
    if (status != NO_ERROR) {
        ALOGE("AudioSystem could not query the input buffer size; status %d", status);
        return NO_INIT;
    }

    if (size == 0) {
        ALOGE("Unsupported configuration: sampleRate %u, format %d, channelMask %#x",
            sampleRate, format, channelMask);
        return BAD_VALUE;
    }

    // We double the size of input buffer for ping pong use of record buffer.
    size <<= 1;

    // Assumes audio_is_linear_pcm(format)
    uint32_t channelCount = popcount(channelMask);
    size /= channelCount * audio_bytes_per_sample(format);

    *frameCount = size;
    return NO_ERROR;
}

// ---------------------------------------------------------------------------

AudioRecord::AudioRecord()
    : mStatus(NO_INIT), mSessionId(0),
      mPreviousPriority(ANDROID_PRIORITY_NORMAL), mPreviousSchedulingGroup(SP_DEFAULT)
{
}

AudioRecord::AudioRecord(
        audio_source_t inputSource,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask,
        int frameCount,
        callback_t cbf,
        void* user,
        int notificationFrames,
        int sessionId,
        transfer_type transferType,
        audio_input_flags_t flags)
    : mStatus(NO_INIT), mSessionId(0),
      mPreviousPriority(ANDROID_PRIORITY_NORMAL),
      mPreviousSchedulingGroup(SP_DEFAULT),
      mProxy(NULL)
{
    mStatus = set(inputSource, sampleRate, format, channelMask, frameCount, cbf, user,
            notificationFrames, false /*threadCanCallJava*/, sessionId, transferType);
}

AudioRecord::~AudioRecord()
{
    if (mStatus == NO_ERROR) {
        // Make sure that callback function exits in the case where
        // it is looping on buffer empty condition in obtainBuffer().
        // Otherwise the callback thread will never exit.
        stop();
        if (mAudioRecordThread != 0) {
            mProxy->interrupt();
            mAudioRecordThread->requestExit();  // see comment in AudioRecord.h
            mAudioRecordThread->requestExitAndWait();
            mAudioRecordThread.clear();
        }
        if (mAudioRecord != 0) {
            mAudioRecord->asBinder()->unlinkToDeath(mDeathNotifier, this);
            mAudioRecord.clear();
        }
        IPCThreadState::self()->flushCommands();
        AudioSystem::releaseAudioSessionId(mSessionId);
    }
}

status_t AudioRecord::set(
        audio_source_t inputSource,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask,
        int frameCountInt,
        callback_t cbf,
        void* user,
        int notificationFrames,
        bool threadCanCallJava,
        int sessionId,
        transfer_type transferType,
        audio_input_flags_t flags)
{
    switch (transferType) {
    case TRANSFER_DEFAULT:
        if (cbf == NULL || threadCanCallJava) {
            transferType = TRANSFER_SYNC;
        } else {
            transferType = TRANSFER_CALLBACK;
        }
        break;
    case TRANSFER_CALLBACK:
        if (cbf == NULL) {
            ALOGE("Transfer type TRANSFER_CALLBACK but cbf == NULL");
            return BAD_VALUE;
        }
        break;
    case TRANSFER_OBTAIN:
    case TRANSFER_SYNC:
        break;
    default:
        ALOGE("Invalid transfer type %d", transferType);
        return BAD_VALUE;
    }
    mTransfer = transferType;

    // FIXME "int" here is legacy and will be replaced by size_t later
    if (frameCountInt < 0) {
        ALOGE("Invalid frame count %d", frameCountInt);
        return BAD_VALUE;
    }
    size_t frameCount = frameCountInt;

    ALOGV("set(): sampleRate %u, channelMask %#x, frameCount %u", sampleRate, channelMask,
            frameCount);

    AutoMutex lock(mLock);

    if (mAudioRecord != 0) {
        ALOGE("Track already in use");
        return INVALID_OPERATION;
    }

    if (inputSource == AUDIO_SOURCE_DEFAULT) {
        inputSource = AUDIO_SOURCE_MIC;
    }
    mInputSource = inputSource;

    if (sampleRate == 0) {
        ALOGE("Invalid sample rate %u", sampleRate);
        return BAD_VALUE;
    }
    mSampleRate = sampleRate;

    // these below should probably come from the audioFlinger too...
    if (format == AUDIO_FORMAT_DEFAULT) {
        format = AUDIO_FORMAT_PCM_16_BIT;
    }

    // validate parameters
    if (!audio_is_valid_format(format)) {
        ALOGE("Invalid format %d", format);
        return BAD_VALUE;
    }
    // Temporary restriction: AudioFlinger currently supports 16-bit PCM only
    if (format != AUDIO_FORMAT_PCM_16_BIT) {
        ALOGE("Format %d is not supported", format);
        return BAD_VALUE;
    }
    mFormat = format;

    if (!audio_is_input_channel(channelMask)) {
        ALOGE("Invalid channel mask %#x", channelMask);
        return BAD_VALUE;
    }
    mChannelMask = channelMask;
    uint32_t channelCount = popcount(channelMask);
    mChannelCount = channelCount;

    // Assumes audio_is_linear_pcm(format), else sizeof(uint8_t)
    mFrameSize = channelCount * audio_bytes_per_sample(format);

    // validate framecount
    size_t minFrameCount = 0;
    status_t status = AudioRecord::getMinFrameCount(&minFrameCount,
            sampleRate, format, channelMask);
    if (status != NO_ERROR) {
        ALOGE("getMinFrameCount() failed; status %d", status);
        return status;
    }
    ALOGV("AudioRecord::set() minFrameCount = %d", minFrameCount);

    if (frameCount == 0) {
        frameCount = minFrameCount;
    } else if (frameCount < minFrameCount) {
        ALOGE("frameCount %u < minFrameCount %u", frameCount, minFrameCount);
        return BAD_VALUE;
    }
    mFrameCount = frameCount;

    mNotificationFramesReq = notificationFrames;
    mNotificationFramesAct = 0;

    if (sessionId == 0 ) {
        mSessionId = AudioSystem::newAudioSessionId();
    } else {
        mSessionId = sessionId;
    }
    ALOGV("set(): mSessionId %d", mSessionId);

    mFlags = flags;

    // create the IAudioRecord
    status = openRecord_l(0 /*epoch*/);
    if (status) {
        return status;
    }

    if (cbf != NULL) {
        mAudioRecordThread = new AudioRecordThread(*this, threadCanCallJava);
        mAudioRecordThread->run("AudioRecord", ANDROID_PRIORITY_AUDIO);
    }

    mStatus = NO_ERROR;

    // Update buffer size in case it has been limited by AudioFlinger during track creation
    mFrameCount = mCblk->frameCount_;

    mActive = false;
    mCbf = cbf;
    mRefreshRemaining = true;
    mUserData = user;
    // TODO: add audio hardware input latency here
    mLatency = (1000*mFrameCount) / sampleRate;
    mMarkerPosition = 0;
    mMarkerReached = false;
    mNewPosition = 0;
    mUpdatePeriod = 0;
    AudioSystem::acquireAudioSessionId(mSessionId);
    mSequence = 1;
    mObservedSequence = mSequence;
    mInOverrun = false;

    return NO_ERROR;
}

// -------------------------------------------------------------------------

status_t AudioRecord::start(AudioSystem::sync_event_t event, int triggerSession)
{
    ALOGV("start, sync event %d trigger session %d", event, triggerSession);

    AutoMutex lock(mLock);
    if (mActive) {
        return NO_ERROR;
    }

    // reset current position as seen by client to 0
    mProxy->setEpoch(mProxy->getEpoch() - mProxy->getPosition());

    mNewPosition = mProxy->getPosition() + mUpdatePeriod;
    int32_t flags = android_atomic_acquire_load(&mCblk->mFlags);

    status_t status = NO_ERROR;
    if (!(flags & CBLK_INVALID)) {
        ALOGV("mAudioRecord->start()");
        status = mAudioRecord->start(event, triggerSession);
        if (status == DEAD_OBJECT) {
            flags |= CBLK_INVALID;
        }
    }
    if (flags & CBLK_INVALID) {
        status = restoreRecord_l("start");
    }

    if (status != NO_ERROR) {
        ALOGE("start() status %d", status);
    } else {
        mActive = true;
        sp<AudioRecordThread> t = mAudioRecordThread;
        if (t != 0) {
            t->resume();
        } else {
            mPreviousPriority = getpriority(PRIO_PROCESS, 0);
            get_sched_policy(0, &mPreviousSchedulingGroup);
            androidSetThreadPriority(0, ANDROID_PRIORITY_AUDIO);
        }
    }

    return status;
}

void AudioRecord::stop()
{
    AutoMutex lock(mLock);
    if (!mActive) {
        return;
    }

    mActive = false;
    mProxy->interrupt();
    mAudioRecord->stop();
    // the record head position will reset to 0, so if a marker is set, we need
    // to activate it again
    mMarkerReached = false;
    sp<AudioRecordThread> t = mAudioRecordThread;
    if (t != 0) {
        t->pause();
    } else {
        setpriority(PRIO_PROCESS, 0, mPreviousPriority);
        set_sched_policy(0, mPreviousSchedulingGroup);
    }
}

bool AudioRecord::stopped() const
{
    AutoMutex lock(mLock);
    return !mActive;
}

status_t AudioRecord::setMarkerPosition(uint32_t marker)
{
    if (mCbf == NULL) {
        return INVALID_OPERATION;
    }

    AutoMutex lock(mLock);
    mMarkerPosition = marker;
    mMarkerReached = false;

    return NO_ERROR;
}

status_t AudioRecord::getMarkerPosition(uint32_t *marker) const
{
    if (marker == NULL) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    *marker = mMarkerPosition;

    return NO_ERROR;
}

status_t AudioRecord::setPositionUpdatePeriod(uint32_t updatePeriod)
{
    if (mCbf == NULL) {
        return INVALID_OPERATION;
    }

    AutoMutex lock(mLock);
    mNewPosition = mProxy->getPosition() + updatePeriod;
    mUpdatePeriod = updatePeriod;

    return NO_ERROR;
}

status_t AudioRecord::getPositionUpdatePeriod(uint32_t *updatePeriod) const
{
    if (updatePeriod == NULL) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    *updatePeriod = mUpdatePeriod;

    return NO_ERROR;
}

status_t AudioRecord::getPosition(uint32_t *position) const
{
    if (position == NULL) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    *position = mProxy->getPosition();

    return NO_ERROR;
}

unsigned int AudioRecord::getInputFramesLost() const
{
    // no need to check mActive, because if inactive this will return 0, which is what we want
    return AudioSystem::getInputFramesLost(getInput());
}

// -------------------------------------------------------------------------

// must be called with mLock held
status_t AudioRecord::openRecord_l(size_t epoch)
{
    status_t status;
    const sp<IAudioFlinger>& audioFlinger = AudioSystem::get_audio_flinger();
    if (audioFlinger == 0) {
        ALOGE("Could not get audioflinger");
        return NO_INIT;
    }

    IAudioFlinger::track_flags_t trackFlags = IAudioFlinger::TRACK_DEFAULT;
    pid_t tid = -1;

    // Client can only express a preference for FAST.  Server will perform additional tests.
    // The only supported use case for FAST is callback transfer mode.
    if (mFlags & AUDIO_INPUT_FLAG_FAST) {
        if ((mTransfer != TRANSFER_CALLBACK) || (mAudioRecordThread == 0)) {
            ALOGW("AUDIO_INPUT_FLAG_FAST denied by client");
            // once denied, do not request again if IAudioRecord is re-created
            mFlags = (audio_input_flags_t) (mFlags & ~AUDIO_INPUT_FLAG_FAST);
        } else {
            trackFlags |= IAudioFlinger::TRACK_FAST;
            tid = mAudioRecordThread->getTid();
        }
    }

    mNotificationFramesAct = mNotificationFramesReq;

    if (!(mFlags & AUDIO_INPUT_FLAG_FAST)) {
        // Make sure that application is notified with sufficient margin before overrun
        if (mNotificationFramesAct == 0 || mNotificationFramesAct > mFrameCount/2) {
            mNotificationFramesAct = mFrameCount/2;
        }
    }

    audio_io_handle_t input = AudioSystem::getInput(mInputSource, mSampleRate, mFormat,
            mChannelMask, mSessionId);
    if (input == 0) {
        ALOGE("Could not get audio input for record source %d", mInputSource);
        return BAD_VALUE;
    }

    int originalSessionId = mSessionId;
    sp<IAudioRecord> record = audioFlinger->openRecord(input,
                                                       mSampleRate, mFormat,
                                                       mChannelMask,
                                                       mFrameCount,
                                                       &trackFlags,
                                                       tid,
                                                       &mSessionId,
                                                       &status);
    ALOGE_IF(originalSessionId != 0 && mSessionId != originalSessionId,
            "session ID changed from %d to %d", originalSessionId, mSessionId);

    if (record == 0 || status != NO_ERROR) {
        ALOGE("AudioFlinger could not create record track, status: %d", status);
        AudioSystem::releaseInput(input);
        return status;
    }
    sp<IMemory> iMem = record->getCblk();
    if (iMem == 0) {
        ALOGE("Could not get control block");
        return NO_INIT;
    }
    void *iMemPointer = iMem->pointer();
    if (iMemPointer == NULL) {
        ALOGE("Could not get control block pointer");
        return NO_INIT;
    }
    if (mAudioRecord != 0) {
        mAudioRecord->asBinder()->unlinkToDeath(mDeathNotifier, this);
        mDeathNotifier.clear();
    }
    mInput = input;
    mAudioRecord = record;
    mCblkMemory = iMem;
    audio_track_cblk_t* cblk = static_cast<audio_track_cblk_t*>(iMemPointer);
    mCblk = cblk;
    // FIXME missing fast track frameCount logic
    mAwaitBoost = false;
    if (mFlags & AUDIO_INPUT_FLAG_FAST) {
        if (trackFlags & IAudioFlinger::TRACK_FAST) {
            ALOGV("AUDIO_INPUT_FLAG_FAST successful; frameCount %u", mFrameCount);
            mAwaitBoost = true;
            // double-buffering is not required for fast tracks, due to tighter scheduling
            if (mNotificationFramesAct == 0 || mNotificationFramesAct > mFrameCount) {
                mNotificationFramesAct = mFrameCount;
            }
        } else {
            ALOGV("AUDIO_INPUT_FLAG_FAST denied by server; frameCount %u", mFrameCount);
            // once denied, do not request again if IAudioRecord is re-created
            mFlags = (audio_input_flags_t) (mFlags & ~AUDIO_INPUT_FLAG_FAST);
            if (mNotificationFramesAct == 0 || mNotificationFramesAct > mFrameCount/2) {
                mNotificationFramesAct = mFrameCount/2;
            }
        }
    }

    // starting address of buffers in shared memory
    void *buffers = (char*)cblk + sizeof(audio_track_cblk_t);

    // update proxy
    mProxy = new AudioRecordClientProxy(cblk, buffers, mFrameCount, mFrameSize);
    mProxy->setEpoch(epoch);
    mProxy->setMinimum(mNotificationFramesAct);

    mDeathNotifier = new DeathNotifier(this);
    mAudioRecord->asBinder()->linkToDeath(mDeathNotifier, this);

    return NO_ERROR;
}

status_t AudioRecord::obtainBuffer(Buffer* audioBuffer, int32_t waitCount)
{
    if (audioBuffer == NULL) {
        return BAD_VALUE;
    }
    if (mTransfer != TRANSFER_OBTAIN) {
        audioBuffer->frameCount = 0;
        audioBuffer->size = 0;
        audioBuffer->raw = NULL;
        return INVALID_OPERATION;
    }

    const struct timespec *requested;
    if (waitCount == -1) {
        requested = &ClientProxy::kForever;
    } else if (waitCount == 0) {
        requested = &ClientProxy::kNonBlocking;
    } else if (waitCount > 0) {
        long long ms = WAIT_PERIOD_MS * (long long) waitCount;
        struct timespec timeout;
        timeout.tv_sec = ms / 1000;
        timeout.tv_nsec = (int) (ms % 1000) * 1000000;
        requested = &timeout;
    } else {
        ALOGE("%s invalid waitCount %d", __func__, waitCount);
        requested = NULL;
    }
    return obtainBuffer(audioBuffer, requested);
}

status_t AudioRecord::obtainBuffer(Buffer* audioBuffer, const struct timespec *requested,
        struct timespec *elapsed, size_t *nonContig)
{
    // previous and new IAudioRecord sequence numbers are used to detect track re-creation
    uint32_t oldSequence = 0;
    uint32_t newSequence;

    Proxy::Buffer buffer;
    status_t status = NO_ERROR;

    static const int32_t kMaxTries = 5;
    int32_t tryCounter = kMaxTries;

    do {
        // obtainBuffer() is called with mutex unlocked, so keep extra references to these fields to
        // keep them from going away if another thread re-creates the track during obtainBuffer()
        sp<AudioRecordClientProxy> proxy;
        sp<IMemory> iMem;
        {
            // start of lock scope
            AutoMutex lock(mLock);

            newSequence = mSequence;
            // did previous obtainBuffer() fail due to media server death or voluntary invalidation?
            if (status == DEAD_OBJECT) {
                // re-create track, unless someone else has already done so
                if (newSequence == oldSequence) {
                    status = restoreRecord_l("obtainBuffer");
                    if (status != NO_ERROR) {
                        break;
                    }
                }
            }
            oldSequence = newSequence;

            // Keep the extra references
            proxy = mProxy;
            iMem = mCblkMemory;

            // Non-blocking if track is stopped
            if (!mActive) {
                requested = &ClientProxy::kNonBlocking;
            }

        }   // end of lock scope

        buffer.mFrameCount = audioBuffer->frameCount;
        // FIXME starts the requested timeout and elapsed over from scratch
        status = proxy->obtainBuffer(&buffer, requested, elapsed);

    } while ((status == DEAD_OBJECT) && (tryCounter-- > 0));

    audioBuffer->frameCount = buffer.mFrameCount;
    audioBuffer->size = buffer.mFrameCount * mFrameSize;
    audioBuffer->raw = buffer.mRaw;
    if (nonContig != NULL) {
        *nonContig = buffer.mNonContig;
    }
    return status;
}

void AudioRecord::releaseBuffer(Buffer* audioBuffer)
{
    // all TRANSFER_* are valid

    size_t stepCount = audioBuffer->size / mFrameSize;
    if (stepCount == 0) {
        return;
    }

    Proxy::Buffer buffer;
    buffer.mFrameCount = stepCount;
    buffer.mRaw = audioBuffer->raw;

    AutoMutex lock(mLock);
    mInOverrun = false;
    mProxy->releaseBuffer(&buffer);

    // the server does not automatically disable recorder on overrun, so no need to restart
}

audio_io_handle_t AudioRecord::getInput() const
{
    AutoMutex lock(mLock);
    return mInput;
}

// -------------------------------------------------------------------------

ssize_t AudioRecord::read(void* buffer, size_t userSize)
{
    if (mTransfer != TRANSFER_SYNC) {
        return INVALID_OPERATION;
    }

    if (ssize_t(userSize) < 0 || (buffer == NULL && userSize != 0)) {
        // sanity-check. user is most-likely passing an error code, and it would
        // make the return value ambiguous (actualSize vs error).
        ALOGE("AudioRecord::read(buffer=%p, size=%u (%d)", buffer, userSize, userSize);
        return BAD_VALUE;
    }

    ssize_t read = 0;
    Buffer audioBuffer;

    while (userSize >= mFrameSize) {
        audioBuffer.frameCount = userSize / mFrameSize;

        status_t err = obtainBuffer(&audioBuffer, &ClientProxy::kForever);
        if (err < 0) {
            if (read > 0) {
                break;
            }
            return ssize_t(err);
        }

        size_t bytesRead = audioBuffer.size;
        memcpy(buffer, audioBuffer.i8, bytesRead);
        buffer = ((char *) buffer) + bytesRead;
        userSize -= bytesRead;
        read += bytesRead;

        releaseBuffer(&audioBuffer);
    }

    return read;
}

// -------------------------------------------------------------------------

nsecs_t AudioRecord::processAudioBuffer(const sp<AudioRecordThread>& thread)
{
    mLock.lock();
    if (mAwaitBoost) {
        mAwaitBoost = false;
        mLock.unlock();
        static const int32_t kMaxTries = 5;
        int32_t tryCounter = kMaxTries;
        uint32_t pollUs = 10000;
        do {
            int policy = sched_getscheduler(0);
            if (policy == SCHED_FIFO || policy == SCHED_RR) {
                break;
            }
            usleep(pollUs);
            pollUs <<= 1;
        } while (tryCounter-- > 0);
        if (tryCounter < 0) {
            ALOGE("did not receive expected priority boost on time");
        }
        // Run again immediately
        return 0;
    }

    // Can only reference mCblk while locked
    int32_t flags = android_atomic_and(~CBLK_OVERRUN, &mCblk->mFlags);

    // Check for track invalidation
    if (flags & CBLK_INVALID) {
        (void) restoreRecord_l("processAudioBuffer");
        mLock.unlock();
        // Run again immediately, but with a new IAudioRecord
        return 0;
    }

    bool active = mActive;

    // Manage overrun callback, must be done under lock to avoid race with releaseBuffer()
    bool newOverrun = false;
    if (flags & CBLK_OVERRUN) {
        if (!mInOverrun) {
            mInOverrun = true;
            newOverrun = true;
        }
    }

    // Get current position of server
    size_t position = mProxy->getPosition();

    // Manage marker callback
    bool markerReached = false;
    size_t markerPosition = mMarkerPosition;
    // FIXME fails for wraparound, need 64 bits
    if (!mMarkerReached && (markerPosition > 0) && (position >= markerPosition)) {
        mMarkerReached = markerReached = true;
    }

    // Determine the number of new position callback(s) that will be needed, while locked
    size_t newPosCount = 0;
    size_t newPosition = mNewPosition;
    uint32_t updatePeriod = mUpdatePeriod;
    // FIXME fails for wraparound, need 64 bits
    if (updatePeriod > 0 && position >= newPosition) {
        newPosCount = ((position - newPosition) / updatePeriod) + 1;
        mNewPosition += updatePeriod * newPosCount;
    }

    // Cache other fields that will be needed soon
    size_t notificationFrames = mNotificationFramesAct;
    if (mRefreshRemaining) {
        mRefreshRemaining = false;
        mRemainingFrames = notificationFrames;
        mRetryOnPartialBuffer = false;
    }
    size_t misalignment = mProxy->getMisalignment();
    int32_t sequence = mSequence;

    // These fields don't need to be cached, because they are assigned only by set():
    //      mTransfer, mCbf, mUserData, mSampleRate

    mLock.unlock();

    // perform callbacks while unlocked
    if (newOverrun) {
        mCbf(EVENT_OVERRUN, mUserData, NULL);
    }
    if (markerReached) {
        mCbf(EVENT_MARKER, mUserData, &markerPosition);
    }
    while (newPosCount > 0) {
        size_t temp = newPosition;
        mCbf(EVENT_NEW_POS, mUserData, &temp);
        newPosition += updatePeriod;
        newPosCount--;
    }
    if (mObservedSequence != sequence) {
        mObservedSequence = sequence;
        mCbf(EVENT_NEW_IAUDIORECORD, mUserData, NULL);
    }

    // if inactive, then don't run me again until re-started
    if (!active) {
        return NS_INACTIVE;
    }

    // Compute the estimated time until the next timed event (position, markers)
    uint32_t minFrames = ~0;
    if (!markerReached && position < markerPosition) {
        minFrames = markerPosition - position;
    }
    if (updatePeriod > 0 && updatePeriod < minFrames) {
        minFrames = updatePeriod;
    }

    // If > 0, poll periodically to recover from a stuck server.  A good value is 2.
    static const uint32_t kPoll = 0;
    if (kPoll > 0 && mTransfer == TRANSFER_CALLBACK && kPoll * notificationFrames < minFrames) {
        minFrames = kPoll * notificationFrames;
    }

    // Convert frame units to time units
    nsecs_t ns = NS_WHENEVER;
    if (minFrames != (uint32_t) ~0) {
        // This "fudge factor" avoids soaking CPU, and compensates for late progress by server
        static const nsecs_t kFudgeNs = 10000000LL; // 10 ms
        ns = ((minFrames * 1000000000LL) / mSampleRate) + kFudgeNs;
    }

    // If not supplying data by EVENT_MORE_DATA, then we're done
    if (mTransfer != TRANSFER_CALLBACK) {
        return ns;
    }

    struct timespec timeout;
    const struct timespec *requested = &ClientProxy::kForever;
    if (ns != NS_WHENEVER) {
        timeout.tv_sec = ns / 1000000000LL;
        timeout.tv_nsec = ns % 1000000000LL;
        ALOGV("timeout %ld.%03d", timeout.tv_sec, (int) timeout.tv_nsec / 1000000);
        requested = &timeout;
    }

    while (mRemainingFrames > 0) {

        Buffer audioBuffer;
        audioBuffer.frameCount = mRemainingFrames;
        size_t nonContig;
        status_t err = obtainBuffer(&audioBuffer, requested, NULL, &nonContig);
        LOG_ALWAYS_FATAL_IF((err != NO_ERROR) != (audioBuffer.frameCount == 0),
                "obtainBuffer() err=%d frameCount=%u", err, audioBuffer.frameCount);
        requested = &ClientProxy::kNonBlocking;
        size_t avail = audioBuffer.frameCount + nonContig;
        ALOGV("obtainBuffer(%u) returned %u = %u + %u",
                mRemainingFrames, avail, audioBuffer.frameCount, nonContig);
        if (err != NO_ERROR) {
            if (err == TIMED_OUT || err == WOULD_BLOCK || err == -EINTR) {
                break;
            }
            ALOGE("Error %d obtaining an audio buffer, giving up.", err);
            return NS_NEVER;
        }

        if (mRetryOnPartialBuffer) {
            mRetryOnPartialBuffer = false;
            if (avail < mRemainingFrames) {
                int64_t myns = ((mRemainingFrames - avail) *
                        1100000000LL) / mSampleRate;
                if (ns < 0 || myns < ns) {
                    ns = myns;
                }
                return ns;
            }
        }

        size_t reqSize = audioBuffer.size;
        mCbf(EVENT_MORE_DATA, mUserData, &audioBuffer);
        size_t readSize = audioBuffer.size;

        // Sanity check on returned size
        if (ssize_t(readSize) < 0 || readSize > reqSize) {
            ALOGE("EVENT_MORE_DATA requested %u bytes but callback returned %d bytes",
                    reqSize, (int) readSize);
            return NS_NEVER;
        }

        if (readSize == 0) {
            // The callback is done consuming buffers
            // Keep this thread going to handle timed events and
            // still try to provide more data in intervals of WAIT_PERIOD_MS
            // but don't just loop and block the CPU, so wait
            return WAIT_PERIOD_MS * 1000000LL;
        }

        size_t releasedFrames = readSize / mFrameSize;
        audioBuffer.frameCount = releasedFrames;
        mRemainingFrames -= releasedFrames;
        if (misalignment >= releasedFrames) {
            misalignment -= releasedFrames;
        } else {
            misalignment = 0;
        }

        releaseBuffer(&audioBuffer);

        // FIXME here is where we would repeat EVENT_MORE_DATA again on same advanced buffer
        // if callback doesn't like to accept the full chunk
        if (readSize < reqSize) {
            continue;
        }

        // There could be enough non-contiguous frames available to satisfy the remaining request
        if (mRemainingFrames <= nonContig) {
            continue;
        }

#if 0
        // This heuristic tries to collapse a series of EVENT_MORE_DATA that would total to a
        // sum <= notificationFrames.  It replaces that series by at most two EVENT_MORE_DATA
        // that total to a sum == notificationFrames.
        if (0 < misalignment && misalignment <= mRemainingFrames) {
            mRemainingFrames = misalignment;
            return (mRemainingFrames * 1100000000LL) / mSampleRate;
        }
#endif

    }
    mRemainingFrames = notificationFrames;
    mRetryOnPartialBuffer = true;

    // A lot has transpired since ns was calculated, so run again immediately and re-calculate
    return 0;
}

status_t AudioRecord::restoreRecord_l(const char *from)
{
    ALOGW("dead IAudioRecord, creating a new one from %s()", from);
    ++mSequence;
    status_t result;

    // if the new IAudioRecord is created, openRecord_l() will modify the
    // following member variables: mAudioRecord, mCblkMemory and mCblk.
    // It will also delete the strong references on previous IAudioRecord and IMemory
    size_t position = mProxy->getPosition();
    mNewPosition = position + mUpdatePeriod;
    result = openRecord_l(position);
    if (result == NO_ERROR) {
        if (mActive) {
            // callback thread or sync event hasn't changed
            // FIXME this fails if we have a new AudioFlinger instance
            result = mAudioRecord->start(AudioSystem::SYNC_EVENT_SAME, 0);
        }
    }
    if (result != NO_ERROR) {
        ALOGW("restoreRecord_l() failed status %d", result);
        mActive = false;
    }

    return result;
}

// =========================================================================

void AudioRecord::DeathNotifier::binderDied(const wp<IBinder>& who)
{
    sp<AudioRecord> audioRecord = mAudioRecord.promote();
    if (audioRecord != 0) {
        AutoMutex lock(audioRecord->mLock);
        audioRecord->mProxy->binderDied();
    }
}

// =========================================================================

AudioRecord::AudioRecordThread::AudioRecordThread(AudioRecord& receiver, bool bCanCallJava)
    : Thread(bCanCallJava), mReceiver(receiver), mPaused(true), mPausedInt(false), mPausedNs(0LL)
{
}

AudioRecord::AudioRecordThread::~AudioRecordThread()
{
}

bool AudioRecord::AudioRecordThread::threadLoop()
{
    {
        AutoMutex _l(mMyLock);
        if (mPaused) {
            mMyCond.wait(mMyLock);
            // caller will check for exitPending()
            return true;
        }
        if (mPausedInt) {
            if (mPausedNs > 0) {
                (void) mMyCond.waitRelative(mMyLock, mPausedNs);
            } else {
                mMyCond.wait(mMyLock);
            }
            mPausedInt = false;
            return true;
        }
    }
    nsecs_t ns =  mReceiver.processAudioBuffer(this);
    switch (ns) {
    case 0:
        return true;
    case NS_INACTIVE:
        pauseInternal();
        return true;
    case NS_NEVER:
        return false;
    case NS_WHENEVER:
        // FIXME increase poll interval, or make event-driven
        ns = 1000000000LL;
        // fall through
    default:
        LOG_ALWAYS_FATAL_IF(ns < 0, "processAudioBuffer() returned %lld", ns);
        pauseInternal(ns);
        return true;
    }
}

void AudioRecord::AudioRecordThread::requestExit()
{
    // must be in this order to avoid a race condition
    Thread::requestExit();
    AutoMutex _l(mMyLock);
    if (mPaused || mPausedInt) {
        mPaused = false;
        mPausedInt = false;
        mMyCond.signal();
    }
}

void AudioRecord::AudioRecordThread::pause()
{
    AutoMutex _l(mMyLock);
    mPaused = true;
}

void AudioRecord::AudioRecordThread::resume()
{
    AutoMutex _l(mMyLock);
    if (mPaused || mPausedInt) {
        mPaused = false;
        mPausedInt = false;
        mMyCond.signal();
    }
}

void AudioRecord::AudioRecordThread::pauseInternal(nsecs_t ns)
{
    AutoMutex _l(mMyLock);
    mPausedInt = true;
    mPausedNs = ns;
}

// -------------------------------------------------------------------------

}; // namespace android

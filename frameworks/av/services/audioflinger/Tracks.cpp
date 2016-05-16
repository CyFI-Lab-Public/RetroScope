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

#include "Configuration.h"
#include <math.h>
#include <utils/Log.h>

#include <private/media/AudioTrackShared.h>

#include <common_time/cc_helper.h>
#include <common_time/local_clock.h>

#include "AudioMixer.h"
#include "AudioFlinger.h"
#include "ServiceUtilities.h"

#include <media/nbaio/Pipe.h>
#include <media/nbaio/PipeReader.h>

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

// ----------------------------------------------------------------------------
//      TrackBase
// ----------------------------------------------------------------------------

static volatile int32_t nextTrackId = 55;

// TrackBase constructor must be called with AudioFlinger::mLock held
AudioFlinger::ThreadBase::TrackBase::TrackBase(
            ThreadBase *thread,
            const sp<Client>& client,
            uint32_t sampleRate,
            audio_format_t format,
            audio_channel_mask_t channelMask,
            size_t frameCount,
            const sp<IMemory>& sharedBuffer,
            int sessionId,
            int clientUid,
            bool isOut)
    :   RefBase(),
        mThread(thread),
        mClient(client),
        mCblk(NULL),
        // mBuffer
        mState(IDLE),
        mSampleRate(sampleRate),
        mFormat(format),
        mChannelMask(channelMask),
        mChannelCount(popcount(channelMask)),
        mFrameSize(audio_is_linear_pcm(format) ?
                mChannelCount * audio_bytes_per_sample(format) : sizeof(int8_t)),
        mFrameCount(frameCount),
        mSessionId(sessionId),
        mIsOut(isOut),
        mServerProxy(NULL),
        mId(android_atomic_inc(&nextTrackId)),
        mTerminated(false)
{
    // if the caller is us, trust the specified uid
    if (IPCThreadState::self()->getCallingPid() != getpid_cached || clientUid == -1) {
        int newclientUid = IPCThreadState::self()->getCallingUid();
        if (clientUid != -1 && clientUid != newclientUid) {
            ALOGW("uid %d tried to pass itself off as %d", newclientUid, clientUid);
        }
        clientUid = newclientUid;
    }
    // clientUid contains the uid of the app that is responsible for this track, so we can blame
    // battery usage on it.
    mUid = clientUid;

    // client == 0 implies sharedBuffer == 0
    ALOG_ASSERT(!(client == 0 && sharedBuffer != 0));

    ALOGV_IF(sharedBuffer != 0, "sharedBuffer: %p, size: %d", sharedBuffer->pointer(),
            sharedBuffer->size());

    // ALOGD("Creating track with %d buffers @ %d bytes", bufferCount, bufferSize);
    size_t size = sizeof(audio_track_cblk_t);
    size_t bufferSize = (sharedBuffer == 0 ? roundup(frameCount) : frameCount) * mFrameSize;
    if (sharedBuffer == 0) {
        size += bufferSize;
    }

    if (client != 0) {
        mCblkMemory = client->heap()->allocate(size);
        if (mCblkMemory != 0) {
            mCblk = static_cast<audio_track_cblk_t *>(mCblkMemory->pointer());
            // can't assume mCblk != NULL
        } else {
            ALOGE("not enough memory for AudioTrack size=%u", size);
            client->heap()->dump("AudioTrack");
            return;
        }
    } else {
        // this syntax avoids calling the audio_track_cblk_t constructor twice
        mCblk = (audio_track_cblk_t *) new uint8_t[size];
        // assume mCblk != NULL
    }

    // construct the shared structure in-place.
    if (mCblk != NULL) {
        new(mCblk) audio_track_cblk_t();
        // clear all buffers
        mCblk->frameCount_ = frameCount;
        if (sharedBuffer == 0) {
            mBuffer = (char*)mCblk + sizeof(audio_track_cblk_t);
            memset(mBuffer, 0, bufferSize);
        } else {
            mBuffer = sharedBuffer->pointer();
#if 0
            mCblk->mFlags = CBLK_FORCEREADY;    // FIXME hack, need to fix the track ready logic
#endif
        }

#ifdef TEE_SINK
        if (mTeeSinkTrackEnabled) {
            NBAIO_Format pipeFormat = Format_from_SR_C(mSampleRate, mChannelCount);
            if (pipeFormat != Format_Invalid) {
                Pipe *pipe = new Pipe(mTeeSinkTrackFrames, pipeFormat);
                size_t numCounterOffers = 0;
                const NBAIO_Format offers[1] = {pipeFormat};
                ssize_t index = pipe->negotiate(offers, 1, NULL, numCounterOffers);
                ALOG_ASSERT(index == 0);
                PipeReader *pipeReader = new PipeReader(*pipe);
                numCounterOffers = 0;
                index = pipeReader->negotiate(offers, 1, NULL, numCounterOffers);
                ALOG_ASSERT(index == 0);
                mTeeSink = pipe;
                mTeeSource = pipeReader;
            }
        }
#endif

    }
}

AudioFlinger::ThreadBase::TrackBase::~TrackBase()
{
#ifdef TEE_SINK
    dumpTee(-1, mTeeSource, mId);
#endif
    // delete the proxy before deleting the shared memory it refers to, to avoid dangling reference
    delete mServerProxy;
    if (mCblk != NULL) {
        if (mClient == 0) {
            delete mCblk;
        } else {
            mCblk->~audio_track_cblk_t();   // destroy our shared-structure.
        }
    }
    mCblkMemory.clear();    // free the shared memory before releasing the heap it belongs to
    if (mClient != 0) {
        // Client destructor must run with AudioFlinger mutex locked
        Mutex::Autolock _l(mClient->audioFlinger()->mLock);
        // If the client's reference count drops to zero, the associated destructor
        // must run with AudioFlinger lock held. Thus the explicit clear() rather than
        // relying on the automatic clear() at end of scope.
        mClient.clear();
    }
}

// AudioBufferProvider interface
// getNextBuffer() = 0;
// This implementation of releaseBuffer() is used by Track and RecordTrack, but not TimedTrack
void AudioFlinger::ThreadBase::TrackBase::releaseBuffer(AudioBufferProvider::Buffer* buffer)
{
#ifdef TEE_SINK
    if (mTeeSink != 0) {
        (void) mTeeSink->write(buffer->raw, buffer->frameCount);
    }
#endif

    ServerProxy::Buffer buf;
    buf.mFrameCount = buffer->frameCount;
    buf.mRaw = buffer->raw;
    buffer->frameCount = 0;
    buffer->raw = NULL;
    mServerProxy->releaseBuffer(&buf);
}

status_t AudioFlinger::ThreadBase::TrackBase::setSyncEvent(const sp<SyncEvent>& event)
{
    mSyncEvents.add(event);
    return NO_ERROR;
}

// ----------------------------------------------------------------------------
//      Playback
// ----------------------------------------------------------------------------

AudioFlinger::TrackHandle::TrackHandle(const sp<AudioFlinger::PlaybackThread::Track>& track)
    : BnAudioTrack(),
      mTrack(track)
{
}

AudioFlinger::TrackHandle::~TrackHandle() {
    // just stop the track on deletion, associated resources
    // will be freed from the main thread once all pending buffers have
    // been played. Unless it's not in the active track list, in which
    // case we free everything now...
    mTrack->destroy();
}

sp<IMemory> AudioFlinger::TrackHandle::getCblk() const {
    return mTrack->getCblk();
}

status_t AudioFlinger::TrackHandle::start() {
    return mTrack->start();
}

void AudioFlinger::TrackHandle::stop() {
    mTrack->stop();
}

void AudioFlinger::TrackHandle::flush() {
    mTrack->flush();
}

void AudioFlinger::TrackHandle::pause() {
    mTrack->pause();
}

status_t AudioFlinger::TrackHandle::attachAuxEffect(int EffectId)
{
    return mTrack->attachAuxEffect(EffectId);
}

status_t AudioFlinger::TrackHandle::allocateTimedBuffer(size_t size,
                                                         sp<IMemory>* buffer) {
    if (!mTrack->isTimedTrack())
        return INVALID_OPERATION;

    PlaybackThread::TimedTrack* tt =
            reinterpret_cast<PlaybackThread::TimedTrack*>(mTrack.get());
    return tt->allocateTimedBuffer(size, buffer);
}

status_t AudioFlinger::TrackHandle::queueTimedBuffer(const sp<IMemory>& buffer,
                                                     int64_t pts) {
    if (!mTrack->isTimedTrack())
        return INVALID_OPERATION;

    PlaybackThread::TimedTrack* tt =
            reinterpret_cast<PlaybackThread::TimedTrack*>(mTrack.get());
    return tt->queueTimedBuffer(buffer, pts);
}

status_t AudioFlinger::TrackHandle::setMediaTimeTransform(
    const LinearTransform& xform, int target) {

    if (!mTrack->isTimedTrack())
        return INVALID_OPERATION;

    PlaybackThread::TimedTrack* tt =
            reinterpret_cast<PlaybackThread::TimedTrack*>(mTrack.get());
    return tt->setMediaTimeTransform(
        xform, static_cast<TimedAudioTrack::TargetTimeline>(target));
}

status_t AudioFlinger::TrackHandle::setParameters(const String8& keyValuePairs) {
    return mTrack->setParameters(keyValuePairs);
}

status_t AudioFlinger::TrackHandle::getTimestamp(AudioTimestamp& timestamp)
{
    return mTrack->getTimestamp(timestamp);
}


void AudioFlinger::TrackHandle::signal()
{
    return mTrack->signal();
}

status_t AudioFlinger::TrackHandle::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    return BnAudioTrack::onTransact(code, data, reply, flags);
}

// ----------------------------------------------------------------------------

// Track constructor must be called with AudioFlinger::mLock and ThreadBase::mLock held
AudioFlinger::PlaybackThread::Track::Track(
            PlaybackThread *thread,
            const sp<Client>& client,
            audio_stream_type_t streamType,
            uint32_t sampleRate,
            audio_format_t format,
            audio_channel_mask_t channelMask,
            size_t frameCount,
            const sp<IMemory>& sharedBuffer,
            int sessionId,
            int uid,
            IAudioFlinger::track_flags_t flags)
    :   TrackBase(thread, client, sampleRate, format, channelMask, frameCount, sharedBuffer,
            sessionId, uid, true /*isOut*/),
    mFillingUpStatus(FS_INVALID),
    // mRetryCount initialized later when needed
    mSharedBuffer(sharedBuffer),
    mStreamType(streamType),
    mName(-1),  // see note below
    mMainBuffer(thread->mixBuffer()),
    mAuxBuffer(NULL),
    mAuxEffectId(0), mHasVolumeController(false),
    mPresentationCompleteFrames(0),
    mFlags(flags),
    mFastIndex(-1),
    mCachedVolume(1.0),
    mIsInvalid(false),
    mAudioTrackServerProxy(NULL),
    mResumeToStopping(false)
{
    if (mCblk != NULL) {
        if (sharedBuffer == 0) {
            mAudioTrackServerProxy = new AudioTrackServerProxy(mCblk, mBuffer, frameCount,
                    mFrameSize);
        } else {
            mAudioTrackServerProxy = new StaticAudioTrackServerProxy(mCblk, mBuffer, frameCount,
                    mFrameSize);
        }
        mServerProxy = mAudioTrackServerProxy;
        // to avoid leaking a track name, do not allocate one unless there is an mCblk
        mName = thread->getTrackName_l(channelMask, sessionId);
        if (mName < 0) {
            ALOGE("no more track names available");
            return;
        }
        // only allocate a fast track index if we were able to allocate a normal track name
        if (flags & IAudioFlinger::TRACK_FAST) {
            mAudioTrackServerProxy->framesReadyIsCalledByMultipleThreads();
            ALOG_ASSERT(thread->mFastTrackAvailMask != 0);
            int i = __builtin_ctz(thread->mFastTrackAvailMask);
            ALOG_ASSERT(0 < i && i < (int)FastMixerState::kMaxFastTracks);
            // FIXME This is too eager.  We allocate a fast track index before the
            //       fast track becomes active.  Since fast tracks are a scarce resource,
            //       this means we are potentially denying other more important fast tracks from
            //       being created.  It would be better to allocate the index dynamically.
            mFastIndex = i;
            // Read the initial underruns because this field is never cleared by the fast mixer
            mObservedUnderruns = thread->getFastTrackUnderruns(i);
            thread->mFastTrackAvailMask &= ~(1 << i);
        }
    }
    ALOGV("Track constructor name %d, calling pid %d", mName,
            IPCThreadState::self()->getCallingPid());
}

AudioFlinger::PlaybackThread::Track::~Track()
{
    ALOGV("PlaybackThread::Track destructor");

    // The destructor would clear mSharedBuffer,
    // but it will not push the decremented reference count,
    // leaving the client's IMemory dangling indefinitely.
    // This prevents that leak.
    if (mSharedBuffer != 0) {
        mSharedBuffer.clear();
        // flush the binder command buffer
        IPCThreadState::self()->flushCommands();
    }
}

void AudioFlinger::PlaybackThread::Track::destroy()
{
    // NOTE: destroyTrack_l() can remove a strong reference to this Track
    // by removing it from mTracks vector, so there is a risk that this Tracks's
    // destructor is called. As the destructor needs to lock mLock,
    // we must acquire a strong reference on this Track before locking mLock
    // here so that the destructor is called only when exiting this function.
    // On the other hand, as long as Track::destroy() is only called by
    // TrackHandle destructor, the TrackHandle still holds a strong ref on
    // this Track with its member mTrack.
    sp<Track> keep(this);
    { // scope for mLock
        sp<ThreadBase> thread = mThread.promote();
        if (thread != 0) {
            Mutex::Autolock _l(thread->mLock);
            PlaybackThread *playbackThread = (PlaybackThread *)thread.get();
            bool wasActive = playbackThread->destroyTrack_l(this);
            if (!isOutputTrack() && !wasActive) {
                AudioSystem::releaseOutput(thread->id());
            }
        }
    }
}

/*static*/ void AudioFlinger::PlaybackThread::Track::appendDumpHeader(String8& result)
{
    result.append("   Name Client Type      Fmt Chn mask Session fCount S F SRate  "
                  "L dB  R dB    Server Main buf  Aux Buf Flags UndFrmCnt\n");
}

void AudioFlinger::PlaybackThread::Track::dump(char* buffer, size_t size)
{
    uint32_t vlr = mAudioTrackServerProxy->getVolumeLR();
    if (isFastTrack()) {
        sprintf(buffer, "   F %2d", mFastIndex);
    } else {
        sprintf(buffer, "   %4d", mName - AudioMixer::TRACK0);
    }
    track_state state = mState;
    char stateChar;
    if (isTerminated()) {
        stateChar = 'T';
    } else {
        switch (state) {
        case IDLE:
            stateChar = 'I';
            break;
        case STOPPING_1:
            stateChar = 's';
            break;
        case STOPPING_2:
            stateChar = '5';
            break;
        case STOPPED:
            stateChar = 'S';
            break;
        case RESUMING:
            stateChar = 'R';
            break;
        case ACTIVE:
            stateChar = 'A';
            break;
        case PAUSING:
            stateChar = 'p';
            break;
        case PAUSED:
            stateChar = 'P';
            break;
        case FLUSHED:
            stateChar = 'F';
            break;
        default:
            stateChar = '?';
            break;
        }
    }
    char nowInUnderrun;
    switch (mObservedUnderruns.mBitFields.mMostRecent) {
    case UNDERRUN_FULL:
        nowInUnderrun = ' ';
        break;
    case UNDERRUN_PARTIAL:
        nowInUnderrun = '<';
        break;
    case UNDERRUN_EMPTY:
        nowInUnderrun = '*';
        break;
    default:
        nowInUnderrun = '?';
        break;
    }
    snprintf(&buffer[7], size-7, " %6u %4u %08X %08X %7u %6u %1c %1d %5u %5.2g %5.2g  "
                                 "%08X %08X %08X 0x%03X %9u%c\n",
            (mClient == 0) ? getpid_cached : mClient->pid(),
            mStreamType,
            mFormat,
            mChannelMask,
            mSessionId,
            mFrameCount,
            stateChar,
            mFillingUpStatus,
            mAudioTrackServerProxy->getSampleRate(),
            20.0 * log10((vlr & 0xFFFF) / 4096.0),
            20.0 * log10((vlr >> 16) / 4096.0),
            mCblk->mServer,
            (int)mMainBuffer,
            (int)mAuxBuffer,
            mCblk->mFlags,
            mAudioTrackServerProxy->getUnderrunFrames(),
            nowInUnderrun);
}

uint32_t AudioFlinger::PlaybackThread::Track::sampleRate() const {
    return mAudioTrackServerProxy->getSampleRate();
}

// AudioBufferProvider interface
status_t AudioFlinger::PlaybackThread::Track::getNextBuffer(
        AudioBufferProvider::Buffer* buffer, int64_t pts)
{
    ServerProxy::Buffer buf;
    size_t desiredFrames = buffer->frameCount;
    buf.mFrameCount = desiredFrames;
    status_t status = mServerProxy->obtainBuffer(&buf);
    buffer->frameCount = buf.mFrameCount;
    buffer->raw = buf.mRaw;
    if (buf.mFrameCount == 0) {
        mAudioTrackServerProxy->tallyUnderrunFrames(desiredFrames);
    }
    return status;
}

// releaseBuffer() is not overridden

// ExtendedAudioBufferProvider interface

// Note that framesReady() takes a mutex on the control block using tryLock().
// This could result in priority inversion if framesReady() is called by the normal mixer,
// as the normal mixer thread runs at lower
// priority than the client's callback thread:  there is a short window within framesReady()
// during which the normal mixer could be preempted, and the client callback would block.
// Another problem can occur if framesReady() is called by the fast mixer:
// the tryLock() could block for up to 1 ms, and a sequence of these could delay fast mixer.
// FIXME Replace AudioTrackShared control block implementation by a non-blocking FIFO queue.
size_t AudioFlinger::PlaybackThread::Track::framesReady() const {
    return mAudioTrackServerProxy->framesReady();
}

size_t AudioFlinger::PlaybackThread::Track::framesReleased() const
{
    return mAudioTrackServerProxy->framesReleased();
}

// Don't call for fast tracks; the framesReady() could result in priority inversion
bool AudioFlinger::PlaybackThread::Track::isReady() const {
    if (mFillingUpStatus != FS_FILLING || isStopped() || isPausing()) {
        return true;
    }

    if (framesReady() >= mFrameCount ||
            (mCblk->mFlags & CBLK_FORCEREADY)) {
        mFillingUpStatus = FS_FILLED;
        android_atomic_and(~CBLK_FORCEREADY, &mCblk->mFlags);
        return true;
    }
    return false;
}

status_t AudioFlinger::PlaybackThread::Track::start(AudioSystem::sync_event_t event,
                                                    int triggerSession)
{
    status_t status = NO_ERROR;
    ALOGV("start(%d), calling pid %d session %d",
            mName, IPCThreadState::self()->getCallingPid(), mSessionId);

    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        if (isOffloaded()) {
            Mutex::Autolock _laf(thread->mAudioFlinger->mLock);
            Mutex::Autolock _lth(thread->mLock);
            sp<EffectChain> ec = thread->getEffectChain_l(mSessionId);
            if (thread->mAudioFlinger->isNonOffloadableGlobalEffectEnabled_l() ||
                    (ec != 0 && ec->isNonOffloadableEnabled())) {
                invalidate();
                return PERMISSION_DENIED;
            }
        }
        Mutex::Autolock _lth(thread->mLock);
        track_state state = mState;
        // here the track could be either new, or restarted
        // in both cases "unstop" the track

        if (state == PAUSED) {
            if (mResumeToStopping) {
                // happened we need to resume to STOPPING_1
                mState = TrackBase::STOPPING_1;
                ALOGV("PAUSED => STOPPING_1 (%d) on thread %p", mName, this);
            } else {
                mState = TrackBase::RESUMING;
                ALOGV("PAUSED => RESUMING (%d) on thread %p", mName, this);
            }
        } else {
            mState = TrackBase::ACTIVE;
            ALOGV("? => ACTIVE (%d) on thread %p", mName, this);
        }

        PlaybackThread *playbackThread = (PlaybackThread *)thread.get();
        status = playbackThread->addTrack_l(this);
        if (status == INVALID_OPERATION || status == PERMISSION_DENIED) {
            triggerEvents(AudioSystem::SYNC_EVENT_PRESENTATION_COMPLETE);
            //  restore previous state if start was rejected by policy manager
            if (status == PERMISSION_DENIED) {
                mState = state;
            }
        }
        // track was already in the active list, not a problem
        if (status == ALREADY_EXISTS) {
            status = NO_ERROR;
        } else {
            // Acknowledge any pending flush(), so that subsequent new data isn't discarded.
            // It is usually unsafe to access the server proxy from a binder thread.
            // But in this case we know the mixer thread (whether normal mixer or fast mixer)
            // isn't looking at this track yet:  we still hold the normal mixer thread lock,
            // and for fast tracks the track is not yet in the fast mixer thread's active set.
            ServerProxy::Buffer buffer;
            buffer.mFrameCount = 1;
            (void) mAudioTrackServerProxy->obtainBuffer(&buffer, true /*ackFlush*/);
        }
    } else {
        status = BAD_VALUE;
    }
    return status;
}

void AudioFlinger::PlaybackThread::Track::stop()
{
    ALOGV("stop(%d), calling pid %d", mName, IPCThreadState::self()->getCallingPid());
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        Mutex::Autolock _l(thread->mLock);
        track_state state = mState;
        if (state == RESUMING || state == ACTIVE || state == PAUSING || state == PAUSED) {
            // If the track is not active (PAUSED and buffers full), flush buffers
            PlaybackThread *playbackThread = (PlaybackThread *)thread.get();
            if (playbackThread->mActiveTracks.indexOf(this) < 0) {
                reset();
                mState = STOPPED;
            } else if (!isFastTrack() && !isOffloaded()) {
                mState = STOPPED;
            } else {
                // For fast tracks prepareTracks_l() will set state to STOPPING_2
                // presentation is complete
                // For an offloaded track this starts a drain and state will
                // move to STOPPING_2 when drain completes and then STOPPED
                mState = STOPPING_1;
            }
            ALOGV("not stopping/stopped => stopping/stopped (%d) on thread %p", mName,
                    playbackThread);
        }
    }
}

void AudioFlinger::PlaybackThread::Track::pause()
{
    ALOGV("pause(%d), calling pid %d", mName, IPCThreadState::self()->getCallingPid());
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        Mutex::Autolock _l(thread->mLock);
        PlaybackThread *playbackThread = (PlaybackThread *)thread.get();
        switch (mState) {
        case STOPPING_1:
        case STOPPING_2:
            if (!isOffloaded()) {
                /* nothing to do if track is not offloaded */
                break;
            }

            // Offloaded track was draining, we need to carry on draining when resumed
            mResumeToStopping = true;
            // fall through...
        case ACTIVE:
        case RESUMING:
            mState = PAUSING;
            ALOGV("ACTIVE/RESUMING => PAUSING (%d) on thread %p", mName, thread.get());
            playbackThread->broadcast_l();
            break;

        default:
            break;
        }
    }
}

void AudioFlinger::PlaybackThread::Track::flush()
{
    ALOGV("flush(%d)", mName);
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        Mutex::Autolock _l(thread->mLock);
        PlaybackThread *playbackThread = (PlaybackThread *)thread.get();

        if (isOffloaded()) {
            // If offloaded we allow flush during any state except terminated
            // and keep the track active to avoid problems if user is seeking
            // rapidly and underlying hardware has a significant delay handling
            // a pause
            if (isTerminated()) {
                return;
            }

            ALOGV("flush: offload flush");
            reset();

            if (mState == STOPPING_1 || mState == STOPPING_2) {
                ALOGV("flushed in STOPPING_1 or 2 state, change state to ACTIVE");
                mState = ACTIVE;
            }

            if (mState == ACTIVE) {
                ALOGV("flush called in active state, resetting buffer time out retry count");
                mRetryCount = PlaybackThread::kMaxTrackRetriesOffload;
            }

            mResumeToStopping = false;
        } else {
            if (mState != STOPPING_1 && mState != STOPPING_2 && mState != STOPPED &&
                    mState != PAUSED && mState != PAUSING && mState != IDLE && mState != FLUSHED) {
                return;
            }
            // No point remaining in PAUSED state after a flush => go to
            // FLUSHED state
            mState = FLUSHED;
            // do not reset the track if it is still in the process of being stopped or paused.
            // this will be done by prepareTracks_l() when the track is stopped.
            // prepareTracks_l() will see mState == FLUSHED, then
            // remove from active track list, reset(), and trigger presentation complete
            if (playbackThread->mActiveTracks.indexOf(this) < 0) {
                reset();
            }
        }
        // Prevent flush being lost if the track is flushed and then resumed
        // before mixer thread can run. This is important when offloading
        // because the hardware buffer could hold a large amount of audio
        playbackThread->flushOutput_l();
        playbackThread->broadcast_l();
    }
}

void AudioFlinger::PlaybackThread::Track::reset()
{
    // Do not reset twice to avoid discarding data written just after a flush and before
    // the audioflinger thread detects the track is stopped.
    if (!mResetDone) {
        // Force underrun condition to avoid false underrun callback until first data is
        // written to buffer
        android_atomic_and(~CBLK_FORCEREADY, &mCblk->mFlags);
        mFillingUpStatus = FS_FILLING;
        mResetDone = true;
        if (mState == FLUSHED) {
            mState = IDLE;
        }
    }
}

status_t AudioFlinger::PlaybackThread::Track::setParameters(const String8& keyValuePairs)
{
    sp<ThreadBase> thread = mThread.promote();
    if (thread == 0) {
        ALOGE("thread is dead");
        return FAILED_TRANSACTION;
    } else if ((thread->type() == ThreadBase::DIRECT) ||
                    (thread->type() == ThreadBase::OFFLOAD)) {
        return thread->setParameters(keyValuePairs);
    } else {
        return PERMISSION_DENIED;
    }
}

status_t AudioFlinger::PlaybackThread::Track::getTimestamp(AudioTimestamp& timestamp)
{
    // Client should implement this using SSQ; the unpresented frame count in latch is irrelevant
    if (isFastTrack()) {
        return INVALID_OPERATION;
    }
    sp<ThreadBase> thread = mThread.promote();
    if (thread == 0) {
        return INVALID_OPERATION;
    }
    Mutex::Autolock _l(thread->mLock);
    PlaybackThread *playbackThread = (PlaybackThread *)thread.get();
    if (!isOffloaded()) {
        if (!playbackThread->mLatchQValid) {
            return INVALID_OPERATION;
        }
        uint32_t unpresentedFrames =
                ((int64_t) playbackThread->mLatchQ.mUnpresentedFrames * mSampleRate) /
                playbackThread->mSampleRate;
        uint32_t framesWritten = mAudioTrackServerProxy->framesReleased();
        if (framesWritten < unpresentedFrames) {
            return INVALID_OPERATION;
        }
        timestamp.mPosition = framesWritten - unpresentedFrames;
        timestamp.mTime = playbackThread->mLatchQ.mTimestamp.mTime;
        return NO_ERROR;
    }

    return playbackThread->getTimestamp_l(timestamp);
}

status_t AudioFlinger::PlaybackThread::Track::attachAuxEffect(int EffectId)
{
    status_t status = DEAD_OBJECT;
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        PlaybackThread *playbackThread = (PlaybackThread *)thread.get();
        sp<AudioFlinger> af = mClient->audioFlinger();

        Mutex::Autolock _l(af->mLock);

        sp<PlaybackThread> srcThread = af->getEffectThread_l(AUDIO_SESSION_OUTPUT_MIX, EffectId);

        if (EffectId != 0 && srcThread != 0 && playbackThread != srcThread.get()) {
            Mutex::Autolock _dl(playbackThread->mLock);
            Mutex::Autolock _sl(srcThread->mLock);
            sp<EffectChain> chain = srcThread->getEffectChain_l(AUDIO_SESSION_OUTPUT_MIX);
            if (chain == 0) {
                return INVALID_OPERATION;
            }

            sp<EffectModule> effect = chain->getEffectFromId_l(EffectId);
            if (effect == 0) {
                return INVALID_OPERATION;
            }
            srcThread->removeEffect_l(effect);
            status = playbackThread->addEffect_l(effect);
            if (status != NO_ERROR) {
                srcThread->addEffect_l(effect);
                return INVALID_OPERATION;
            }
            // removeEffect_l() has stopped the effect if it was active so it must be restarted
            if (effect->state() == EffectModule::ACTIVE ||
                    effect->state() == EffectModule::STOPPING) {
                effect->start();
            }

            sp<EffectChain> dstChain = effect->chain().promote();
            if (dstChain == 0) {
                srcThread->addEffect_l(effect);
                return INVALID_OPERATION;
            }
            AudioSystem::unregisterEffect(effect->id());
            AudioSystem::registerEffect(&effect->desc(),
                                        srcThread->id(),
                                        dstChain->strategy(),
                                        AUDIO_SESSION_OUTPUT_MIX,
                                        effect->id());
            AudioSystem::setEffectEnabled(effect->id(), effect->isEnabled());
        }
        status = playbackThread->attachAuxEffect(this, EffectId);
    }
    return status;
}

void AudioFlinger::PlaybackThread::Track::setAuxBuffer(int EffectId, int32_t *buffer)
{
    mAuxEffectId = EffectId;
    mAuxBuffer = buffer;
}

bool AudioFlinger::PlaybackThread::Track::presentationComplete(size_t framesWritten,
                                                         size_t audioHalFrames)
{
    // a track is considered presented when the total number of frames written to audio HAL
    // corresponds to the number of frames written when presentationComplete() is called for the
    // first time (mPresentationCompleteFrames == 0) plus the buffer filling status at that time.
    // For an offloaded track the HAL+h/w delay is variable so a HAL drain() is used
    // to detect when all frames have been played. In this case framesWritten isn't
    // useful because it doesn't always reflect whether there is data in the h/w
    // buffers, particularly if a track has been paused and resumed during draining
    ALOGV("presentationComplete() mPresentationCompleteFrames %d framesWritten %d",
                      mPresentationCompleteFrames, framesWritten);
    if (mPresentationCompleteFrames == 0) {
        mPresentationCompleteFrames = framesWritten + audioHalFrames;
        ALOGV("presentationComplete() reset: mPresentationCompleteFrames %d audioHalFrames %d",
                  mPresentationCompleteFrames, audioHalFrames);
    }

    if (framesWritten >= mPresentationCompleteFrames || isOffloaded()) {
        ALOGV("presentationComplete() session %d complete: framesWritten %d",
                  mSessionId, framesWritten);
        triggerEvents(AudioSystem::SYNC_EVENT_PRESENTATION_COMPLETE);
        mAudioTrackServerProxy->setStreamEndDone();
        return true;
    }
    return false;
}

void AudioFlinger::PlaybackThread::Track::triggerEvents(AudioSystem::sync_event_t type)
{
    for (int i = 0; i < (int)mSyncEvents.size(); i++) {
        if (mSyncEvents[i]->type() == type) {
            mSyncEvents[i]->trigger();
            mSyncEvents.removeAt(i);
            i--;
        }
    }
}

// implement VolumeBufferProvider interface

uint32_t AudioFlinger::PlaybackThread::Track::getVolumeLR()
{
    // called by FastMixer, so not allowed to take any locks, block, or do I/O including logs
    ALOG_ASSERT(isFastTrack() && (mCblk != NULL));
    uint32_t vlr = mAudioTrackServerProxy->getVolumeLR();
    uint32_t vl = vlr & 0xFFFF;
    uint32_t vr = vlr >> 16;
    // track volumes come from shared memory, so can't be trusted and must be clamped
    if (vl > MAX_GAIN_INT) {
        vl = MAX_GAIN_INT;
    }
    if (vr > MAX_GAIN_INT) {
        vr = MAX_GAIN_INT;
    }
    // now apply the cached master volume and stream type volume;
    // this is trusted but lacks any synchronization or barrier so may be stale
    float v = mCachedVolume;
    vl *= v;
    vr *= v;
    // re-combine into U4.16
    vlr = (vr << 16) | (vl & 0xFFFF);
    // FIXME look at mute, pause, and stop flags
    return vlr;
}

status_t AudioFlinger::PlaybackThread::Track::setSyncEvent(const sp<SyncEvent>& event)
{
    if (isTerminated() || mState == PAUSED ||
            ((framesReady() == 0) && ((mSharedBuffer != 0) ||
                                      (mState == STOPPED)))) {
        ALOGW("Track::setSyncEvent() in invalid state %d on session %d %s mode, framesReady %d ",
              mState, mSessionId, (mSharedBuffer != 0) ? "static" : "stream", framesReady());
        event->cancel();
        return INVALID_OPERATION;
    }
    (void) TrackBase::setSyncEvent(event);
    return NO_ERROR;
}

void AudioFlinger::PlaybackThread::Track::invalidate()
{
    // FIXME should use proxy, and needs work
    audio_track_cblk_t* cblk = mCblk;
    android_atomic_or(CBLK_INVALID, &cblk->mFlags);
    android_atomic_release_store(0x40000000, &cblk->mFutex);
    // client is not in server, so FUTEX_WAKE is needed instead of FUTEX_WAKE_PRIVATE
    (void) __futex_syscall3(&cblk->mFutex, FUTEX_WAKE, INT_MAX);
    mIsInvalid = true;
}

void AudioFlinger::PlaybackThread::Track::signal()
{
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        PlaybackThread *t = (PlaybackThread *)thread.get();
        Mutex::Autolock _l(t->mLock);
        t->broadcast_l();
    }
}

// ----------------------------------------------------------------------------

sp<AudioFlinger::PlaybackThread::TimedTrack>
AudioFlinger::PlaybackThread::TimedTrack::create(
            PlaybackThread *thread,
            const sp<Client>& client,
            audio_stream_type_t streamType,
            uint32_t sampleRate,
            audio_format_t format,
            audio_channel_mask_t channelMask,
            size_t frameCount,
            const sp<IMemory>& sharedBuffer,
            int sessionId,
            int uid) {
    if (!client->reserveTimedTrack())
        return 0;

    return new TimedTrack(
        thread, client, streamType, sampleRate, format, channelMask, frameCount,
        sharedBuffer, sessionId, uid);
}

AudioFlinger::PlaybackThread::TimedTrack::TimedTrack(
            PlaybackThread *thread,
            const sp<Client>& client,
            audio_stream_type_t streamType,
            uint32_t sampleRate,
            audio_format_t format,
            audio_channel_mask_t channelMask,
            size_t frameCount,
            const sp<IMemory>& sharedBuffer,
            int sessionId,
            int uid)
    : Track(thread, client, streamType, sampleRate, format, channelMask,
            frameCount, sharedBuffer, sessionId, uid, IAudioFlinger::TRACK_TIMED),
      mQueueHeadInFlight(false),
      mTrimQueueHeadOnRelease(false),
      mFramesPendingInQueue(0),
      mTimedSilenceBuffer(NULL),
      mTimedSilenceBufferSize(0),
      mTimedAudioOutputOnTime(false),
      mMediaTimeTransformValid(false)
{
    LocalClock lc;
    mLocalTimeFreq = lc.getLocalFreq();

    mLocalTimeToSampleTransform.a_zero = 0;
    mLocalTimeToSampleTransform.b_zero = 0;
    mLocalTimeToSampleTransform.a_to_b_numer = sampleRate;
    mLocalTimeToSampleTransform.a_to_b_denom = mLocalTimeFreq;
    LinearTransform::reduce(&mLocalTimeToSampleTransform.a_to_b_numer,
                            &mLocalTimeToSampleTransform.a_to_b_denom);

    mMediaTimeToSampleTransform.a_zero = 0;
    mMediaTimeToSampleTransform.b_zero = 0;
    mMediaTimeToSampleTransform.a_to_b_numer = sampleRate;
    mMediaTimeToSampleTransform.a_to_b_denom = 1000000;
    LinearTransform::reduce(&mMediaTimeToSampleTransform.a_to_b_numer,
                            &mMediaTimeToSampleTransform.a_to_b_denom);
}

AudioFlinger::PlaybackThread::TimedTrack::~TimedTrack() {
    mClient->releaseTimedTrack();
    delete [] mTimedSilenceBuffer;
}

status_t AudioFlinger::PlaybackThread::TimedTrack::allocateTimedBuffer(
    size_t size, sp<IMemory>* buffer) {

    Mutex::Autolock _l(mTimedBufferQueueLock);

    trimTimedBufferQueue_l();

    // lazily initialize the shared memory heap for timed buffers
    if (mTimedMemoryDealer == NULL) {
        const int kTimedBufferHeapSize = 512 << 10;

        mTimedMemoryDealer = new MemoryDealer(kTimedBufferHeapSize,
                                              "AudioFlingerTimed");
        if (mTimedMemoryDealer == NULL)
            return NO_MEMORY;
    }

    sp<IMemory> newBuffer = mTimedMemoryDealer->allocate(size);
    if (newBuffer == NULL) {
        newBuffer = mTimedMemoryDealer->allocate(size);
        if (newBuffer == NULL)
            return NO_MEMORY;
    }

    *buffer = newBuffer;
    return NO_ERROR;
}

// caller must hold mTimedBufferQueueLock
void AudioFlinger::PlaybackThread::TimedTrack::trimTimedBufferQueue_l() {
    int64_t mediaTimeNow;
    {
        Mutex::Autolock mttLock(mMediaTimeTransformLock);
        if (!mMediaTimeTransformValid)
            return;

        int64_t targetTimeNow;
        status_t res = (mMediaTimeTransformTarget == TimedAudioTrack::COMMON_TIME)
            ? mCCHelper.getCommonTime(&targetTimeNow)
            : mCCHelper.getLocalTime(&targetTimeNow);

        if (OK != res)
            return;

        if (!mMediaTimeTransform.doReverseTransform(targetTimeNow,
                                                    &mediaTimeNow)) {
            return;
        }
    }

    size_t trimEnd;
    for (trimEnd = 0; trimEnd < mTimedBufferQueue.size(); trimEnd++) {
        int64_t bufEnd;

        if ((trimEnd + 1) < mTimedBufferQueue.size()) {
            // We have a next buffer.  Just use its PTS as the PTS of the frame
            // following the last frame in this buffer.  If the stream is sparse
            // (ie, there are deliberate gaps left in the stream which should be
            // filled with silence by the TimedAudioTrack), then this can result
            // in one extra buffer being left un-trimmed when it could have
            // been.  In general, this is not typical, and we would rather
            // optimized away the TS calculation below for the more common case
            // where PTSes are contiguous.
            bufEnd = mTimedBufferQueue[trimEnd + 1].pts();
        } else {
            // We have no next buffer.  Compute the PTS of the frame following
            // the last frame in this buffer by computing the duration of of
            // this frame in media time units and adding it to the PTS of the
            // buffer.
            int64_t frameCount = mTimedBufferQueue[trimEnd].buffer()->size()
                               / mFrameSize;

            if (!mMediaTimeToSampleTransform.doReverseTransform(frameCount,
                                                                &bufEnd)) {
                ALOGE("Failed to convert frame count of %lld to media time"
                      " duration" " (scale factor %d/%u) in %s",
                      frameCount,
                      mMediaTimeToSampleTransform.a_to_b_numer,
                      mMediaTimeToSampleTransform.a_to_b_denom,
                      __PRETTY_FUNCTION__);
                break;
            }
            bufEnd += mTimedBufferQueue[trimEnd].pts();
        }

        if (bufEnd > mediaTimeNow)
            break;

        // Is the buffer we want to use in the middle of a mix operation right
        // now?  If so, don't actually trim it.  Just wait for the releaseBuffer
        // from the mixer which should be coming back shortly.
        if (!trimEnd && mQueueHeadInFlight) {
            mTrimQueueHeadOnRelease = true;
        }
    }

    size_t trimStart = mTrimQueueHeadOnRelease ? 1 : 0;
    if (trimStart < trimEnd) {
        // Update the bookkeeping for framesReady()
        for (size_t i = trimStart; i < trimEnd; ++i) {
            updateFramesPendingAfterTrim_l(mTimedBufferQueue[i], "trim");
        }

        // Now actually remove the buffers from the queue.
        mTimedBufferQueue.removeItemsAt(trimStart, trimEnd);
    }
}

void AudioFlinger::PlaybackThread::TimedTrack::trimTimedBufferQueueHead_l(
        const char* logTag) {
    ALOG_ASSERT(mTimedBufferQueue.size() > 0,
                "%s called (reason \"%s\"), but timed buffer queue has no"
                " elements to trim.", __FUNCTION__, logTag);

    updateFramesPendingAfterTrim_l(mTimedBufferQueue[0], logTag);
    mTimedBufferQueue.removeAt(0);
}

void AudioFlinger::PlaybackThread::TimedTrack::updateFramesPendingAfterTrim_l(
        const TimedBuffer& buf,
        const char* logTag) {
    uint32_t bufBytes        = buf.buffer()->size();
    uint32_t consumedAlready = buf.position();

    ALOG_ASSERT(consumedAlready <= bufBytes,
                "Bad bookkeeping while updating frames pending.  Timed buffer is"
                " only %u bytes long, but claims to have consumed %u"
                " bytes.  (update reason: \"%s\")",
                bufBytes, consumedAlready, logTag);

    uint32_t bufFrames = (bufBytes - consumedAlready) / mFrameSize;
    ALOG_ASSERT(mFramesPendingInQueue >= bufFrames,
                "Bad bookkeeping while updating frames pending.  Should have at"
                " least %u queued frames, but we think we have only %u.  (update"
                " reason: \"%s\")",
                bufFrames, mFramesPendingInQueue, logTag);

    mFramesPendingInQueue -= bufFrames;
}

status_t AudioFlinger::PlaybackThread::TimedTrack::queueTimedBuffer(
    const sp<IMemory>& buffer, int64_t pts) {

    {
        Mutex::Autolock mttLock(mMediaTimeTransformLock);
        if (!mMediaTimeTransformValid)
            return INVALID_OPERATION;
    }

    Mutex::Autolock _l(mTimedBufferQueueLock);

    uint32_t bufFrames = buffer->size() / mFrameSize;
    mFramesPendingInQueue += bufFrames;
    mTimedBufferQueue.add(TimedBuffer(buffer, pts));

    return NO_ERROR;
}

status_t AudioFlinger::PlaybackThread::TimedTrack::setMediaTimeTransform(
    const LinearTransform& xform, TimedAudioTrack::TargetTimeline target) {

    ALOGVV("setMediaTimeTransform az=%lld bz=%lld n=%d d=%u tgt=%d",
           xform.a_zero, xform.b_zero, xform.a_to_b_numer, xform.a_to_b_denom,
           target);

    if (!(target == TimedAudioTrack::LOCAL_TIME ||
          target == TimedAudioTrack::COMMON_TIME)) {
        return BAD_VALUE;
    }

    Mutex::Autolock lock(mMediaTimeTransformLock);
    mMediaTimeTransform = xform;
    mMediaTimeTransformTarget = target;
    mMediaTimeTransformValid = true;

    return NO_ERROR;
}

#define min(a, b) ((a) < (b) ? (a) : (b))

// implementation of getNextBuffer for tracks whose buffers have timestamps
status_t AudioFlinger::PlaybackThread::TimedTrack::getNextBuffer(
    AudioBufferProvider::Buffer* buffer, int64_t pts)
{
    if (pts == AudioBufferProvider::kInvalidPTS) {
        buffer->raw = NULL;
        buffer->frameCount = 0;
        mTimedAudioOutputOnTime = false;
        return INVALID_OPERATION;
    }

    Mutex::Autolock _l(mTimedBufferQueueLock);

    ALOG_ASSERT(!mQueueHeadInFlight,
                "getNextBuffer called without releaseBuffer!");

    while (true) {

        // if we have no timed buffers, then fail
        if (mTimedBufferQueue.isEmpty()) {
            buffer->raw = NULL;
            buffer->frameCount = 0;
            return NOT_ENOUGH_DATA;
        }

        TimedBuffer& head = mTimedBufferQueue.editItemAt(0);

        // calculate the PTS of the head of the timed buffer queue expressed in
        // local time
        int64_t headLocalPTS;
        {
            Mutex::Autolock mttLock(mMediaTimeTransformLock);

            ALOG_ASSERT(mMediaTimeTransformValid, "media time transform invalid");

            if (mMediaTimeTransform.a_to_b_denom == 0) {
                // the transform represents a pause, so yield silence
                timedYieldSilence_l(buffer->frameCount, buffer);
                return NO_ERROR;
            }

            int64_t transformedPTS;
            if (!mMediaTimeTransform.doForwardTransform(head.pts(),
                                                        &transformedPTS)) {
                // the transform failed.  this shouldn't happen, but if it does
                // then just drop this buffer
                ALOGW("timedGetNextBuffer transform failed");
                buffer->raw = NULL;
                buffer->frameCount = 0;
                trimTimedBufferQueueHead_l("getNextBuffer; no transform");
                return NO_ERROR;
            }

            if (mMediaTimeTransformTarget == TimedAudioTrack::COMMON_TIME) {
                if (OK != mCCHelper.commonTimeToLocalTime(transformedPTS,
                                                          &headLocalPTS)) {
                    buffer->raw = NULL;
                    buffer->frameCount = 0;
                    return INVALID_OPERATION;
                }
            } else {
                headLocalPTS = transformedPTS;
            }
        }

        uint32_t sr = sampleRate();

        // adjust the head buffer's PTS to reflect the portion of the head buffer
        // that has already been consumed
        int64_t effectivePTS = headLocalPTS +
                ((head.position() / mFrameSize) * mLocalTimeFreq / sr);

        // Calculate the delta in samples between the head of the input buffer
        // queue and the start of the next output buffer that will be written.
        // If the transformation fails because of over or underflow, it means
        // that the sample's position in the output stream is so far out of
        // whack that it should just be dropped.
        int64_t sampleDelta;
        if (llabs(effectivePTS - pts) >= (static_cast<int64_t>(1) << 31)) {
            ALOGV("*** head buffer is too far from PTS: dropped buffer");
            trimTimedBufferQueueHead_l("getNextBuffer, buf pts too far from"
                                       " mix");
            continue;
        }
        if (!mLocalTimeToSampleTransform.doForwardTransform(
                (effectivePTS - pts) << 32, &sampleDelta)) {
            ALOGV("*** too late during sample rate transform: dropped buffer");
            trimTimedBufferQueueHead_l("getNextBuffer, bad local to sample");
            continue;
        }

        ALOGVV("*** getNextBuffer head.pts=%lld head.pos=%d pts=%lld"
               " sampleDelta=[%d.%08x]",
               head.pts(), head.position(), pts,
               static_cast<int32_t>((sampleDelta >= 0 ? 0 : 1)
                   + (sampleDelta >> 32)),
               static_cast<uint32_t>(sampleDelta & 0xFFFFFFFF));

        // if the delta between the ideal placement for the next input sample and
        // the current output position is within this threshold, then we will
        // concatenate the next input samples to the previous output
        const int64_t kSampleContinuityThreshold =
                (static_cast<int64_t>(sr) << 32) / 250;

        // if this is the first buffer of audio that we're emitting from this track
        // then it should be almost exactly on time.
        const int64_t kSampleStartupThreshold = 1LL << 32;

        if ((mTimedAudioOutputOnTime && llabs(sampleDelta) <= kSampleContinuityThreshold) ||
           (!mTimedAudioOutputOnTime && llabs(sampleDelta) <= kSampleStartupThreshold)) {
            // the next input is close enough to being on time, so concatenate it
            // with the last output
            timedYieldSamples_l(buffer);

            ALOGVV("*** on time: head.pos=%d frameCount=%u",
                    head.position(), buffer->frameCount);
            return NO_ERROR;
        }

        // Looks like our output is not on time.  Reset our on timed status.
        // Next time we mix samples from our input queue, then should be within
        // the StartupThreshold.
        mTimedAudioOutputOnTime = false;
        if (sampleDelta > 0) {
            // the gap between the current output position and the proper start of
            // the next input sample is too big, so fill it with silence
            uint32_t framesUntilNextInput = (sampleDelta + 0x80000000) >> 32;

            timedYieldSilence_l(framesUntilNextInput, buffer);
            ALOGV("*** silence: frameCount=%u", buffer->frameCount);
            return NO_ERROR;
        } else {
            // the next input sample is late
            uint32_t lateFrames = static_cast<uint32_t>(-((sampleDelta + 0x80000000) >> 32));
            size_t onTimeSamplePosition =
                    head.position() + lateFrames * mFrameSize;

            if (onTimeSamplePosition > head.buffer()->size()) {
                // all the remaining samples in the head are too late, so
                // drop it and move on
                ALOGV("*** too late: dropped buffer");
                trimTimedBufferQueueHead_l("getNextBuffer, dropped late buffer");
                continue;
            } else {
                // skip over the late samples
                head.setPosition(onTimeSamplePosition);

                // yield the available samples
                timedYieldSamples_l(buffer);

                ALOGV("*** late: head.pos=%d frameCount=%u", head.position(), buffer->frameCount);
                return NO_ERROR;
            }
        }
    }
}

// Yield samples from the timed buffer queue head up to the given output
// buffer's capacity.
//
// Caller must hold mTimedBufferQueueLock
void AudioFlinger::PlaybackThread::TimedTrack::timedYieldSamples_l(
    AudioBufferProvider::Buffer* buffer) {

    const TimedBuffer& head = mTimedBufferQueue[0];

    buffer->raw = (static_cast<uint8_t*>(head.buffer()->pointer()) +
                   head.position());

    uint32_t framesLeftInHead = ((head.buffer()->size() - head.position()) /
                                 mFrameSize);
    size_t framesRequested = buffer->frameCount;
    buffer->frameCount = min(framesLeftInHead, framesRequested);

    mQueueHeadInFlight = true;
    mTimedAudioOutputOnTime = true;
}

// Yield samples of silence up to the given output buffer's capacity
//
// Caller must hold mTimedBufferQueueLock
void AudioFlinger::PlaybackThread::TimedTrack::timedYieldSilence_l(
    uint32_t numFrames, AudioBufferProvider::Buffer* buffer) {

    // lazily allocate a buffer filled with silence
    if (mTimedSilenceBufferSize < numFrames * mFrameSize) {
        delete [] mTimedSilenceBuffer;
        mTimedSilenceBufferSize = numFrames * mFrameSize;
        mTimedSilenceBuffer = new uint8_t[mTimedSilenceBufferSize];
        memset(mTimedSilenceBuffer, 0, mTimedSilenceBufferSize);
    }

    buffer->raw = mTimedSilenceBuffer;
    size_t framesRequested = buffer->frameCount;
    buffer->frameCount = min(numFrames, framesRequested);

    mTimedAudioOutputOnTime = false;
}

// AudioBufferProvider interface
void AudioFlinger::PlaybackThread::TimedTrack::releaseBuffer(
    AudioBufferProvider::Buffer* buffer) {

    Mutex::Autolock _l(mTimedBufferQueueLock);

    // If the buffer which was just released is part of the buffer at the head
    // of the queue, be sure to update the amt of the buffer which has been
    // consumed.  If the buffer being returned is not part of the head of the
    // queue, its either because the buffer is part of the silence buffer, or
    // because the head of the timed queue was trimmed after the mixer called
    // getNextBuffer but before the mixer called releaseBuffer.
    if (buffer->raw == mTimedSilenceBuffer) {
        ALOG_ASSERT(!mQueueHeadInFlight,
                    "Queue head in flight during release of silence buffer!");
        goto done;
    }

    ALOG_ASSERT(mQueueHeadInFlight,
                "TimedTrack::releaseBuffer of non-silence buffer, but no queue"
                " head in flight.");

    if (mTimedBufferQueue.size()) {
        TimedBuffer& head = mTimedBufferQueue.editItemAt(0);

        void* start = head.buffer()->pointer();
        void* end   = reinterpret_cast<void*>(
                        reinterpret_cast<uint8_t*>(head.buffer()->pointer())
                        + head.buffer()->size());

        ALOG_ASSERT((buffer->raw >= start) && (buffer->raw < end),
                    "released buffer not within the head of the timed buffer"
                    " queue; qHead = [%p, %p], released buffer = %p",
                    start, end, buffer->raw);

        head.setPosition(head.position() +
                (buffer->frameCount * mFrameSize));
        mQueueHeadInFlight = false;

        ALOG_ASSERT(mFramesPendingInQueue >= buffer->frameCount,
                    "Bad bookkeeping during releaseBuffer!  Should have at"
                    " least %u queued frames, but we think we have only %u",
                    buffer->frameCount, mFramesPendingInQueue);

        mFramesPendingInQueue -= buffer->frameCount;

        if ((static_cast<size_t>(head.position()) >= head.buffer()->size())
            || mTrimQueueHeadOnRelease) {
            trimTimedBufferQueueHead_l("releaseBuffer");
            mTrimQueueHeadOnRelease = false;
        }
    } else {
        LOG_FATAL("TimedTrack::releaseBuffer of non-silence buffer with no"
                  " buffers in the timed buffer queue");
    }

done:
    buffer->raw = 0;
    buffer->frameCount = 0;
}

size_t AudioFlinger::PlaybackThread::TimedTrack::framesReady() const {
    Mutex::Autolock _l(mTimedBufferQueueLock);
    return mFramesPendingInQueue;
}

AudioFlinger::PlaybackThread::TimedTrack::TimedBuffer::TimedBuffer()
        : mPTS(0), mPosition(0) {}

AudioFlinger::PlaybackThread::TimedTrack::TimedBuffer::TimedBuffer(
    const sp<IMemory>& buffer, int64_t pts)
        : mBuffer(buffer), mPTS(pts), mPosition(0) {}


// ----------------------------------------------------------------------------

AudioFlinger::PlaybackThread::OutputTrack::OutputTrack(
            PlaybackThread *playbackThread,
            DuplicatingThread *sourceThread,
            uint32_t sampleRate,
            audio_format_t format,
            audio_channel_mask_t channelMask,
            size_t frameCount,
            int uid)
    :   Track(playbackThread, NULL, AUDIO_STREAM_CNT, sampleRate, format, channelMask, frameCount,
                NULL, 0, uid, IAudioFlinger::TRACK_DEFAULT),
    mActive(false), mSourceThread(sourceThread), mClientProxy(NULL)
{

    if (mCblk != NULL) {
        mOutBuffer.frameCount = 0;
        playbackThread->mTracks.add(this);
        ALOGV("OutputTrack constructor mCblk %p, mBuffer %p, "
                "mCblk->frameCount_ %u, mChannelMask 0x%08x",
                mCblk, mBuffer,
                mCblk->frameCount_, mChannelMask);
        // since client and server are in the same process,
        // the buffer has the same virtual address on both sides
        mClientProxy = new AudioTrackClientProxy(mCblk, mBuffer, mFrameCount, mFrameSize);
        mClientProxy->setVolumeLR((uint32_t(uint16_t(0x1000)) << 16) | uint16_t(0x1000));
        mClientProxy->setSendLevel(0.0);
        mClientProxy->setSampleRate(sampleRate);
        mClientProxy = new AudioTrackClientProxy(mCblk, mBuffer, mFrameCount, mFrameSize,
                true /*clientInServer*/);
    } else {
        ALOGW("Error creating output track on thread %p", playbackThread);
    }
}

AudioFlinger::PlaybackThread::OutputTrack::~OutputTrack()
{
    clearBufferQueue();
    delete mClientProxy;
    // superclass destructor will now delete the server proxy and shared memory both refer to
}

status_t AudioFlinger::PlaybackThread::OutputTrack::start(AudioSystem::sync_event_t event,
                                                          int triggerSession)
{
    status_t status = Track::start(event, triggerSession);
    if (status != NO_ERROR) {
        return status;
    }

    mActive = true;
    mRetryCount = 127;
    return status;
}

void AudioFlinger::PlaybackThread::OutputTrack::stop()
{
    Track::stop();
    clearBufferQueue();
    mOutBuffer.frameCount = 0;
    mActive = false;
}

bool AudioFlinger::PlaybackThread::OutputTrack::write(int16_t* data, uint32_t frames)
{
    Buffer *pInBuffer;
    Buffer inBuffer;
    uint32_t channelCount = mChannelCount;
    bool outputBufferFull = false;
    inBuffer.frameCount = frames;
    inBuffer.i16 = data;

    uint32_t waitTimeLeftMs = mSourceThread->waitTimeMs();

    if (!mActive && frames != 0) {
        start();
        sp<ThreadBase> thread = mThread.promote();
        if (thread != 0) {
            MixerThread *mixerThread = (MixerThread *)thread.get();
            if (mFrameCount > frames) {
                if (mBufferQueue.size() < kMaxOverFlowBuffers) {
                    uint32_t startFrames = (mFrameCount - frames);
                    pInBuffer = new Buffer;
                    pInBuffer->mBuffer = new int16_t[startFrames * channelCount];
                    pInBuffer->frameCount = startFrames;
                    pInBuffer->i16 = pInBuffer->mBuffer;
                    memset(pInBuffer->raw, 0, startFrames * channelCount * sizeof(int16_t));
                    mBufferQueue.add(pInBuffer);
                } else {
                    ALOGW("OutputTrack::write() %p no more buffers in queue", this);
                }
            }
        }
    }

    while (waitTimeLeftMs) {
        // First write pending buffers, then new data
        if (mBufferQueue.size()) {
            pInBuffer = mBufferQueue.itemAt(0);
        } else {
            pInBuffer = &inBuffer;
        }

        if (pInBuffer->frameCount == 0) {
            break;
        }

        if (mOutBuffer.frameCount == 0) {
            mOutBuffer.frameCount = pInBuffer->frameCount;
            nsecs_t startTime = systemTime();
            status_t status = obtainBuffer(&mOutBuffer, waitTimeLeftMs);
            if (status != NO_ERROR) {
                ALOGV("OutputTrack::write() %p thread %p no more output buffers; status %d", this,
                        mThread.unsafe_get(), status);
                outputBufferFull = true;
                break;
            }
            uint32_t waitTimeMs = (uint32_t)ns2ms(systemTime() - startTime);
            if (waitTimeLeftMs >= waitTimeMs) {
                waitTimeLeftMs -= waitTimeMs;
            } else {
                waitTimeLeftMs = 0;
            }
        }

        uint32_t outFrames = pInBuffer->frameCount > mOutBuffer.frameCount ? mOutBuffer.frameCount :
                pInBuffer->frameCount;
        memcpy(mOutBuffer.raw, pInBuffer->raw, outFrames * channelCount * sizeof(int16_t));
        Proxy::Buffer buf;
        buf.mFrameCount = outFrames;
        buf.mRaw = NULL;
        mClientProxy->releaseBuffer(&buf);
        pInBuffer->frameCount -= outFrames;
        pInBuffer->i16 += outFrames * channelCount;
        mOutBuffer.frameCount -= outFrames;
        mOutBuffer.i16 += outFrames * channelCount;

        if (pInBuffer->frameCount == 0) {
            if (mBufferQueue.size()) {
                mBufferQueue.removeAt(0);
                delete [] pInBuffer->mBuffer;
                delete pInBuffer;
                ALOGV("OutputTrack::write() %p thread %p released overflow buffer %d", this,
                        mThread.unsafe_get(), mBufferQueue.size());
            } else {
                break;
            }
        }
    }

    // If we could not write all frames, allocate a buffer and queue it for next time.
    if (inBuffer.frameCount) {
        sp<ThreadBase> thread = mThread.promote();
        if (thread != 0 && !thread->standby()) {
            if (mBufferQueue.size() < kMaxOverFlowBuffers) {
                pInBuffer = new Buffer;
                pInBuffer->mBuffer = new int16_t[inBuffer.frameCount * channelCount];
                pInBuffer->frameCount = inBuffer.frameCount;
                pInBuffer->i16 = pInBuffer->mBuffer;
                memcpy(pInBuffer->raw, inBuffer.raw, inBuffer.frameCount * channelCount *
                        sizeof(int16_t));
                mBufferQueue.add(pInBuffer);
                ALOGV("OutputTrack::write() %p thread %p adding overflow buffer %d", this,
                        mThread.unsafe_get(), mBufferQueue.size());
            } else {
                ALOGW("OutputTrack::write() %p thread %p no more overflow buffers",
                        mThread.unsafe_get(), this);
            }
        }
    }

    // Calling write() with a 0 length buffer, means that no more data will be written:
    // If no more buffers are pending, fill output track buffer to make sure it is started
    // by output mixer.
    if (frames == 0 && mBufferQueue.size() == 0) {
        // FIXME borken, replace by getting framesReady() from proxy
        size_t user = 0;    // was mCblk->user
        if (user < mFrameCount) {
            frames = mFrameCount - user;
            pInBuffer = new Buffer;
            pInBuffer->mBuffer = new int16_t[frames * channelCount];
            pInBuffer->frameCount = frames;
            pInBuffer->i16 = pInBuffer->mBuffer;
            memset(pInBuffer->raw, 0, frames * channelCount * sizeof(int16_t));
            mBufferQueue.add(pInBuffer);
        } else if (mActive) {
            stop();
        }
    }

    return outputBufferFull;
}

status_t AudioFlinger::PlaybackThread::OutputTrack::obtainBuffer(
        AudioBufferProvider::Buffer* buffer, uint32_t waitTimeMs)
{
    ClientProxy::Buffer buf;
    buf.mFrameCount = buffer->frameCount;
    struct timespec timeout;
    timeout.tv_sec = waitTimeMs / 1000;
    timeout.tv_nsec = (int) (waitTimeMs % 1000) * 1000000;
    status_t status = mClientProxy->obtainBuffer(&buf, &timeout);
    buffer->frameCount = buf.mFrameCount;
    buffer->raw = buf.mRaw;
    return status;
}

void AudioFlinger::PlaybackThread::OutputTrack::clearBufferQueue()
{
    size_t size = mBufferQueue.size();

    for (size_t i = 0; i < size; i++) {
        Buffer *pBuffer = mBufferQueue.itemAt(i);
        delete [] pBuffer->mBuffer;
        delete pBuffer;
    }
    mBufferQueue.clear();
}


// ----------------------------------------------------------------------------
//      Record
// ----------------------------------------------------------------------------

AudioFlinger::RecordHandle::RecordHandle(
        const sp<AudioFlinger::RecordThread::RecordTrack>& recordTrack)
    : BnAudioRecord(),
    mRecordTrack(recordTrack)
{
}

AudioFlinger::RecordHandle::~RecordHandle() {
    stop_nonvirtual();
    mRecordTrack->destroy();
}

sp<IMemory> AudioFlinger::RecordHandle::getCblk() const {
    return mRecordTrack->getCblk();
}

status_t AudioFlinger::RecordHandle::start(int /*AudioSystem::sync_event_t*/ event,
        int triggerSession) {
    ALOGV("RecordHandle::start()");
    return mRecordTrack->start((AudioSystem::sync_event_t)event, triggerSession);
}

void AudioFlinger::RecordHandle::stop() {
    stop_nonvirtual();
}

void AudioFlinger::RecordHandle::stop_nonvirtual() {
    ALOGV("RecordHandle::stop()");
    mRecordTrack->stop();
}

status_t AudioFlinger::RecordHandle::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    return BnAudioRecord::onTransact(code, data, reply, flags);
}

// ----------------------------------------------------------------------------

// RecordTrack constructor must be called with AudioFlinger::mLock held
AudioFlinger::RecordThread::RecordTrack::RecordTrack(
            RecordThread *thread,
            const sp<Client>& client,
            uint32_t sampleRate,
            audio_format_t format,
            audio_channel_mask_t channelMask,
            size_t frameCount,
            int sessionId,
            int uid)
    :   TrackBase(thread, client, sampleRate, format,
                  channelMask, frameCount, 0 /*sharedBuffer*/, sessionId, uid, false /*isOut*/),
        mOverflow(false)
{
    ALOGV("RecordTrack constructor");
    if (mCblk != NULL) {
        mAudioRecordServerProxy = new AudioRecordServerProxy(mCblk, mBuffer, frameCount,
                mFrameSize);
        mServerProxy = mAudioRecordServerProxy;
    }
}

AudioFlinger::RecordThread::RecordTrack::~RecordTrack()
{
    ALOGV("%s", __func__);
}

// AudioBufferProvider interface
status_t AudioFlinger::RecordThread::RecordTrack::getNextBuffer(AudioBufferProvider::Buffer* buffer,
        int64_t pts)
{
    ServerProxy::Buffer buf;
    buf.mFrameCount = buffer->frameCount;
    status_t status = mServerProxy->obtainBuffer(&buf);
    buffer->frameCount = buf.mFrameCount;
    buffer->raw = buf.mRaw;
    if (buf.mFrameCount == 0) {
        // FIXME also wake futex so that overrun is noticed more quickly
        (void) android_atomic_or(CBLK_OVERRUN, &mCblk->mFlags);
    }
    return status;
}

status_t AudioFlinger::RecordThread::RecordTrack::start(AudioSystem::sync_event_t event,
                                                        int triggerSession)
{
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        RecordThread *recordThread = (RecordThread *)thread.get();
        return recordThread->start(this, event, triggerSession);
    } else {
        return BAD_VALUE;
    }
}

void AudioFlinger::RecordThread::RecordTrack::stop()
{
    sp<ThreadBase> thread = mThread.promote();
    if (thread != 0) {
        RecordThread *recordThread = (RecordThread *)thread.get();
        if (recordThread->stop(this)) {
            AudioSystem::stopInput(recordThread->id());
        }
    }
}

void AudioFlinger::RecordThread::RecordTrack::destroy()
{
    // see comments at AudioFlinger::PlaybackThread::Track::destroy()
    sp<RecordTrack> keep(this);
    {
        sp<ThreadBase> thread = mThread.promote();
        if (thread != 0) {
            if (mState == ACTIVE || mState == RESUMING) {
                AudioSystem::stopInput(thread->id());
            }
            AudioSystem::releaseInput(thread->id());
            Mutex::Autolock _l(thread->mLock);
            RecordThread *recordThread = (RecordThread *) thread.get();
            recordThread->destroyTrack_l(this);
        }
    }
}

void AudioFlinger::RecordThread::RecordTrack::invalidate()
{
    // FIXME should use proxy, and needs work
    audio_track_cblk_t* cblk = mCblk;
    android_atomic_or(CBLK_INVALID, &cblk->mFlags);
    android_atomic_release_store(0x40000000, &cblk->mFutex);
    // client is not in server, so FUTEX_WAKE is needed instead of FUTEX_WAKE_PRIVATE
    (void) __futex_syscall3(&cblk->mFutex, FUTEX_WAKE, INT_MAX);
}


/*static*/ void AudioFlinger::RecordThread::RecordTrack::appendDumpHeader(String8& result)
{
    result.append("Client Fmt Chn mask Session S   Server fCount\n");
}

void AudioFlinger::RecordThread::RecordTrack::dump(char* buffer, size_t size)
{
    snprintf(buffer, size, "%6u %3u %08X %7u %1d %08X %6u\n",
            (mClient == 0) ? getpid_cached : mClient->pid(),
            mFormat,
            mChannelMask,
            mSessionId,
            mState,
            mCblk->mServer,
            mFrameCount);
}

}; // namespace android

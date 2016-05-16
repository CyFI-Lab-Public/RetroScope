/*
**
** Copyright 2007, The Android Open Source Project
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
#define LOG_TAG "AudioTrack"

#include <sys/resource.h>
#include <audio_utils/primitives.h>
#include <binder/IPCThreadState.h>
#include <media/AudioTrack.h>
#include <utils/Log.h>
#include <private/media/AudioTrackShared.h>
#include <media/IAudioFlinger.h>

#define WAIT_PERIOD_MS                  10
#define WAIT_STREAM_END_TIMEOUT_SEC     120


namespace android {
// ---------------------------------------------------------------------------

// static
status_t AudioTrack::getMinFrameCount(
        size_t* frameCount,
        audio_stream_type_t streamType,
        uint32_t sampleRate)
{
    if (frameCount == NULL) {
        return BAD_VALUE;
    }

    // default to 0 in case of error
    *frameCount = 0;

    // FIXME merge with similar code in createTrack_l(), except we're missing
    //       some information here that is available in createTrack_l():
    //          audio_io_handle_t output
    //          audio_format_t format
    //          audio_channel_mask_t channelMask
    //          audio_output_flags_t flags
    uint32_t afSampleRate;
    if (AudioSystem::getOutputSamplingRate(&afSampleRate, streamType) != NO_ERROR) {
        return NO_INIT;
    }
    size_t afFrameCount;
    if (AudioSystem::getOutputFrameCount(&afFrameCount, streamType) != NO_ERROR) {
        return NO_INIT;
    }
    uint32_t afLatency;
    if (AudioSystem::getOutputLatency(&afLatency, streamType) != NO_ERROR) {
        return NO_INIT;
    }

    // Ensure that buffer depth covers at least audio hardware latency
    uint32_t minBufCount = afLatency / ((1000 * afFrameCount) / afSampleRate);
    if (minBufCount < 2) {
        minBufCount = 2;
    }

    *frameCount = (sampleRate == 0) ? afFrameCount * minBufCount :
            afFrameCount * minBufCount * sampleRate / afSampleRate;
    ALOGV("getMinFrameCount=%d: afFrameCount=%d, minBufCount=%d, afSampleRate=%d, afLatency=%d",
            *frameCount, afFrameCount, minBufCount, afSampleRate, afLatency);
    return NO_ERROR;
}

// ---------------------------------------------------------------------------

AudioTrack::AudioTrack()
    : mStatus(NO_INIT),
      mIsTimed(false),
      mPreviousPriority(ANDROID_PRIORITY_NORMAL),
      mPreviousSchedulingGroup(SP_DEFAULT)
{
}

AudioTrack::AudioTrack(
        audio_stream_type_t streamType,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask,
        int frameCount,
        audio_output_flags_t flags,
        callback_t cbf,
        void* user,
        int notificationFrames,
        int sessionId,
        transfer_type transferType,
        const audio_offload_info_t *offloadInfo,
        int uid)
    : mStatus(NO_INIT),
      mIsTimed(false),
      mPreviousPriority(ANDROID_PRIORITY_NORMAL),
      mPreviousSchedulingGroup(SP_DEFAULT)
{
    mStatus = set(streamType, sampleRate, format, channelMask,
            frameCount, flags, cbf, user, notificationFrames,
            0 /*sharedBuffer*/, false /*threadCanCallJava*/, sessionId, transferType,
            offloadInfo, uid);
}

AudioTrack::AudioTrack(
        audio_stream_type_t streamType,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask,
        const sp<IMemory>& sharedBuffer,
        audio_output_flags_t flags,
        callback_t cbf,
        void* user,
        int notificationFrames,
        int sessionId,
        transfer_type transferType,
        const audio_offload_info_t *offloadInfo,
        int uid)
    : mStatus(NO_INIT),
      mIsTimed(false),
      mPreviousPriority(ANDROID_PRIORITY_NORMAL),
      mPreviousSchedulingGroup(SP_DEFAULT)
{
    mStatus = set(streamType, sampleRate, format, channelMask,
            0 /*frameCount*/, flags, cbf, user, notificationFrames,
            sharedBuffer, false /*threadCanCallJava*/, sessionId, transferType, offloadInfo, uid);
}

AudioTrack::~AudioTrack()
{
    if (mStatus == NO_ERROR) {
        // Make sure that callback function exits in the case where
        // it is looping on buffer full condition in obtainBuffer().
        // Otherwise the callback thread will never exit.
        stop();
        if (mAudioTrackThread != 0) {
            mProxy->interrupt();
            mAudioTrackThread->requestExit();   // see comment in AudioTrack.h
            mAudioTrackThread->requestExitAndWait();
            mAudioTrackThread.clear();
        }
        mAudioTrack->asBinder()->unlinkToDeath(mDeathNotifier, this);
        mAudioTrack.clear();
        IPCThreadState::self()->flushCommands();
        AudioSystem::releaseAudioSessionId(mSessionId);
    }
}

status_t AudioTrack::set(
        audio_stream_type_t streamType,
        uint32_t sampleRate,
        audio_format_t format,
        audio_channel_mask_t channelMask,
        int frameCountInt,
        audio_output_flags_t flags,
        callback_t cbf,
        void* user,
        int notificationFrames,
        const sp<IMemory>& sharedBuffer,
        bool threadCanCallJava,
        int sessionId,
        transfer_type transferType,
        const audio_offload_info_t *offloadInfo,
        int uid)
{
    switch (transferType) {
    case TRANSFER_DEFAULT:
        if (sharedBuffer != 0) {
            transferType = TRANSFER_SHARED;
        } else if (cbf == NULL || threadCanCallJava) {
            transferType = TRANSFER_SYNC;
        } else {
            transferType = TRANSFER_CALLBACK;
        }
        break;
    case TRANSFER_CALLBACK:
        if (cbf == NULL || sharedBuffer != 0) {
            ALOGE("Transfer type TRANSFER_CALLBACK but cbf == NULL || sharedBuffer != 0");
            return BAD_VALUE;
        }
        break;
    case TRANSFER_OBTAIN:
    case TRANSFER_SYNC:
        if (sharedBuffer != 0) {
            ALOGE("Transfer type TRANSFER_OBTAIN but sharedBuffer != 0");
            return BAD_VALUE;
        }
        break;
    case TRANSFER_SHARED:
        if (sharedBuffer == 0) {
            ALOGE("Transfer type TRANSFER_SHARED but sharedBuffer == 0");
            return BAD_VALUE;
        }
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

    ALOGV_IF(sharedBuffer != 0, "sharedBuffer: %p, size: %d", sharedBuffer->pointer(),
            sharedBuffer->size());

    ALOGV("set() streamType %d frameCount %u flags %04x", streamType, frameCount, flags);

    AutoMutex lock(mLock);

    // invariant that mAudioTrack != 0 is true only after set() returns successfully
    if (mAudioTrack != 0) {
        ALOGE("Track already in use");
        return INVALID_OPERATION;
    }

    mOutput = 0;

    // handle default values first.
    if (streamType == AUDIO_STREAM_DEFAULT) {
        streamType = AUDIO_STREAM_MUSIC;
    }

    if (sampleRate == 0) {
        uint32_t afSampleRate;
        if (AudioSystem::getOutputSamplingRate(&afSampleRate, streamType) != NO_ERROR) {
            return NO_INIT;
        }
        sampleRate = afSampleRate;
    }
    mSampleRate = sampleRate;

    // these below should probably come from the audioFlinger too...
    if (format == AUDIO_FORMAT_DEFAULT) {
        format = AUDIO_FORMAT_PCM_16_BIT;
    }
    if (channelMask == 0) {
        channelMask = AUDIO_CHANNEL_OUT_STEREO;
    }

    // validate parameters
    if (!audio_is_valid_format(format)) {
        ALOGE("Invalid format %d", format);
        return BAD_VALUE;
    }

    // AudioFlinger does not currently support 8-bit data in shared memory
    if (format == AUDIO_FORMAT_PCM_8_BIT && sharedBuffer != 0) {
        ALOGE("8-bit data in shared memory is not supported");
        return BAD_VALUE;
    }

    // force direct flag if format is not linear PCM
    // or offload was requested
    if ((flags & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD)
            || !audio_is_linear_pcm(format)) {
        ALOGV( (flags & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD)
                    ? "Offload request, forcing to Direct Output"
                    : "Not linear PCM, forcing to Direct Output");
        flags = (audio_output_flags_t)
                // FIXME why can't we allow direct AND fast?
                ((flags | AUDIO_OUTPUT_FLAG_DIRECT) & ~AUDIO_OUTPUT_FLAG_FAST);
    }
    // only allow deep buffering for music stream type
    if (streamType != AUDIO_STREAM_MUSIC) {
        flags = (audio_output_flags_t)(flags &~AUDIO_OUTPUT_FLAG_DEEP_BUFFER);
    }

    if (!audio_is_output_channel(channelMask)) {
        ALOGE("Invalid channel mask %#x", channelMask);
        return BAD_VALUE;
    }
    mChannelMask = channelMask;
    uint32_t channelCount = popcount(channelMask);
    mChannelCount = channelCount;

    if (audio_is_linear_pcm(format)) {
        mFrameSize = channelCount * audio_bytes_per_sample(format);
        mFrameSizeAF = channelCount * sizeof(int16_t);
    } else {
        mFrameSize = sizeof(uint8_t);
        mFrameSizeAF = sizeof(uint8_t);
    }

    audio_io_handle_t output = AudioSystem::getOutput(
                                    streamType,
                                    sampleRate, format, channelMask,
                                    flags,
                                    offloadInfo);

    if (output == 0) {
        ALOGE("Could not get audio output for stream type %d", streamType);
        return BAD_VALUE;
    }

    mVolume[LEFT] = 1.0f;
    mVolume[RIGHT] = 1.0f;
    mSendLevel = 0.0f;
    mFrameCount = frameCount;
    mReqFrameCount = frameCount;
    mNotificationFramesReq = notificationFrames;
    mNotificationFramesAct = 0;
    mSessionId = sessionId;
    if (uid == -1 || (IPCThreadState::self()->getCallingPid() != getpid())) {
        mClientUid = IPCThreadState::self()->getCallingUid();
    } else {
        mClientUid = uid;
    }
    mAuxEffectId = 0;
    mFlags = flags;
    mCbf = cbf;

    if (cbf != NULL) {
        mAudioTrackThread = new AudioTrackThread(*this, threadCanCallJava);
        mAudioTrackThread->run("AudioTrack", ANDROID_PRIORITY_AUDIO, 0 /*stack*/);
    }

    // create the IAudioTrack
    status_t status = createTrack_l(streamType,
                                  sampleRate,
                                  format,
                                  frameCount,
                                  flags,
                                  sharedBuffer,
                                  output,
                                  0 /*epoch*/);

    if (status != NO_ERROR) {
        if (mAudioTrackThread != 0) {
            mAudioTrackThread->requestExit();   // see comment in AudioTrack.h
            mAudioTrackThread->requestExitAndWait();
            mAudioTrackThread.clear();
        }
        //Use of direct and offloaded output streams is ref counted by audio policy manager.
        // As getOutput was called above and resulted in an output stream to be opened,
        // we need to release it.
        AudioSystem::releaseOutput(output);
        return status;
    }

    mStatus = NO_ERROR;
    mStreamType = streamType;
    mFormat = format;
    mSharedBuffer = sharedBuffer;
    mState = STATE_STOPPED;
    mUserData = user;
    mLoopPeriod = 0;
    mMarkerPosition = 0;
    mMarkerReached = false;
    mNewPosition = 0;
    mUpdatePeriod = 0;
    AudioSystem::acquireAudioSessionId(mSessionId);
    mSequence = 1;
    mObservedSequence = mSequence;
    mInUnderrun = false;
    mOutput = output;

    return NO_ERROR;
}

// -------------------------------------------------------------------------

status_t AudioTrack::start()
{
    AutoMutex lock(mLock);

    if (mState == STATE_ACTIVE) {
        return INVALID_OPERATION;
    }

    mInUnderrun = true;

    State previousState = mState;
    if (previousState == STATE_PAUSED_STOPPING) {
        mState = STATE_STOPPING;
    } else {
        mState = STATE_ACTIVE;
    }
    if (previousState == STATE_STOPPED || previousState == STATE_FLUSHED) {
        // reset current position as seen by client to 0
        mProxy->setEpoch(mProxy->getEpoch() - mProxy->getPosition());
        // force refresh of remaining frames by processAudioBuffer() as last
        // write before stop could be partial.
        mRefreshRemaining = true;
    }
    mNewPosition = mProxy->getPosition() + mUpdatePeriod;
    int32_t flags = android_atomic_and(~CBLK_DISABLED, &mCblk->mFlags);

    sp<AudioTrackThread> t = mAudioTrackThread;
    if (t != 0) {
        if (previousState == STATE_STOPPING) {
            mProxy->interrupt();
        } else {
            t->resume();
        }
    } else {
        mPreviousPriority = getpriority(PRIO_PROCESS, 0);
        get_sched_policy(0, &mPreviousSchedulingGroup);
        androidSetThreadPriority(0, ANDROID_PRIORITY_AUDIO);
    }

    status_t status = NO_ERROR;
    if (!(flags & CBLK_INVALID)) {
        status = mAudioTrack->start();
        if (status == DEAD_OBJECT) {
            flags |= CBLK_INVALID;
        }
    }
    if (flags & CBLK_INVALID) {
        status = restoreTrack_l("start");
    }

    if (status != NO_ERROR) {
        ALOGE("start() status %d", status);
        mState = previousState;
        if (t != 0) {
            if (previousState != STATE_STOPPING) {
                t->pause();
            }
        } else {
            setpriority(PRIO_PROCESS, 0, mPreviousPriority);
            set_sched_policy(0, mPreviousSchedulingGroup);
        }
    }

    return status;
}

void AudioTrack::stop()
{
    AutoMutex lock(mLock);
    // FIXME pause then stop should not be a nop
    if (mState != STATE_ACTIVE) {
        return;
    }

    if (isOffloaded()) {
        mState = STATE_STOPPING;
    } else {
        mState = STATE_STOPPED;
    }

    mProxy->interrupt();
    mAudioTrack->stop();
    // the playback head position will reset to 0, so if a marker is set, we need
    // to activate it again
    mMarkerReached = false;
#if 0
    // Force flush if a shared buffer is used otherwise audioflinger
    // will not stop before end of buffer is reached.
    // It may be needed to make sure that we stop playback, likely in case looping is on.
    if (mSharedBuffer != 0) {
        flush_l();
    }
#endif

    sp<AudioTrackThread> t = mAudioTrackThread;
    if (t != 0) {
        if (!isOffloaded()) {
            t->pause();
        }
    } else {
        setpriority(PRIO_PROCESS, 0, mPreviousPriority);
        set_sched_policy(0, mPreviousSchedulingGroup);
    }
}

bool AudioTrack::stopped() const
{
    AutoMutex lock(mLock);
    return mState != STATE_ACTIVE;
}

void AudioTrack::flush()
{
    if (mSharedBuffer != 0) {
        return;
    }
    AutoMutex lock(mLock);
    if (mState == STATE_ACTIVE || mState == STATE_FLUSHED) {
        return;
    }
    flush_l();
}

void AudioTrack::flush_l()
{
    ALOG_ASSERT(mState != STATE_ACTIVE);

    // clear playback marker and periodic update counter
    mMarkerPosition = 0;
    mMarkerReached = false;
    mUpdatePeriod = 0;
    mRefreshRemaining = true;

    mState = STATE_FLUSHED;
    if (isOffloaded()) {
        mProxy->interrupt();
    }
    mProxy->flush();
    mAudioTrack->flush();
}

void AudioTrack::pause()
{
    AutoMutex lock(mLock);
    if (mState == STATE_ACTIVE) {
        mState = STATE_PAUSED;
    } else if (mState == STATE_STOPPING) {
        mState = STATE_PAUSED_STOPPING;
    } else {
        return;
    }
    mProxy->interrupt();
    mAudioTrack->pause();
}

status_t AudioTrack::setVolume(float left, float right)
{
    if (left < 0.0f || left > 1.0f || right < 0.0f || right > 1.0f) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    mVolume[LEFT] = left;
    mVolume[RIGHT] = right;

    mProxy->setVolumeLR((uint32_t(uint16_t(right * 0x1000)) << 16) | uint16_t(left * 0x1000));

    if (isOffloaded()) {
        mAudioTrack->signal();
    }
    return NO_ERROR;
}

status_t AudioTrack::setVolume(float volume)
{
    return setVolume(volume, volume);
}

status_t AudioTrack::setAuxEffectSendLevel(float level)
{
    if (level < 0.0f || level > 1.0f) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    mSendLevel = level;
    mProxy->setSendLevel(level);

    return NO_ERROR;
}

void AudioTrack::getAuxEffectSendLevel(float* level) const
{
    if (level != NULL) {
        *level = mSendLevel;
    }
}

status_t AudioTrack::setSampleRate(uint32_t rate)
{
    if (mIsTimed || isOffloaded()) {
        return INVALID_OPERATION;
    }

    uint32_t afSamplingRate;
    if (AudioSystem::getOutputSamplingRate(&afSamplingRate, mStreamType) != NO_ERROR) {
        return NO_INIT;
    }
    // Resampler implementation limits input sampling rate to 2 x output sampling rate.
    if (rate == 0 || rate > afSamplingRate*2 ) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    mSampleRate = rate;
    mProxy->setSampleRate(rate);

    return NO_ERROR;
}

uint32_t AudioTrack::getSampleRate() const
{
    if (mIsTimed) {
        return 0;
    }

    AutoMutex lock(mLock);
    return mSampleRate;
}

status_t AudioTrack::setLoop(uint32_t loopStart, uint32_t loopEnd, int loopCount)
{
    if (mSharedBuffer == 0 || mIsTimed || isOffloaded()) {
        return INVALID_OPERATION;
    }

    if (loopCount == 0) {
        ;
    } else if (loopCount >= -1 && loopStart < loopEnd && loopEnd <= mFrameCount &&
            loopEnd - loopStart >= MIN_LOOP) {
        ;
    } else {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    // See setPosition() regarding setting parameters such as loop points or position while active
    if (mState == STATE_ACTIVE) {
        return INVALID_OPERATION;
    }
    setLoop_l(loopStart, loopEnd, loopCount);
    return NO_ERROR;
}

void AudioTrack::setLoop_l(uint32_t loopStart, uint32_t loopEnd, int loopCount)
{
    // FIXME If setting a loop also sets position to start of loop, then
    //       this is correct.  Otherwise it should be removed.
    mNewPosition = mProxy->getPosition() + mUpdatePeriod;
    mLoopPeriod = loopCount != 0 ? loopEnd - loopStart : 0;
    mStaticProxy->setLoop(loopStart, loopEnd, loopCount);
}

status_t AudioTrack::setMarkerPosition(uint32_t marker)
{
    // The only purpose of setting marker position is to get a callback
    if (mCbf == NULL || isOffloaded()) {
        return INVALID_OPERATION;
    }

    AutoMutex lock(mLock);
    mMarkerPosition = marker;
    mMarkerReached = false;

    return NO_ERROR;
}

status_t AudioTrack::getMarkerPosition(uint32_t *marker) const
{
    if (isOffloaded()) {
        return INVALID_OPERATION;
    }
    if (marker == NULL) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    *marker = mMarkerPosition;

    return NO_ERROR;
}

status_t AudioTrack::setPositionUpdatePeriod(uint32_t updatePeriod)
{
    // The only purpose of setting position update period is to get a callback
    if (mCbf == NULL || isOffloaded()) {
        return INVALID_OPERATION;
    }

    AutoMutex lock(mLock);
    mNewPosition = mProxy->getPosition() + updatePeriod;
    mUpdatePeriod = updatePeriod;
    return NO_ERROR;
}

status_t AudioTrack::getPositionUpdatePeriod(uint32_t *updatePeriod) const
{
    if (isOffloaded()) {
        return INVALID_OPERATION;
    }
    if (updatePeriod == NULL) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    *updatePeriod = mUpdatePeriod;

    return NO_ERROR;
}

status_t AudioTrack::setPosition(uint32_t position)
{
    if (mSharedBuffer == 0 || mIsTimed || isOffloaded()) {
        return INVALID_OPERATION;
    }
    if (position > mFrameCount) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    // Currently we require that the player is inactive before setting parameters such as position
    // or loop points.  Otherwise, there could be a race condition: the application could read the
    // current position, compute a new position or loop parameters, and then set that position or
    // loop parameters but it would do the "wrong" thing since the position has continued to advance
    // in the mean time.  If we ever provide a sequencer in server, we could allow a way for the app
    // to specify how it wants to handle such scenarios.
    if (mState == STATE_ACTIVE) {
        return INVALID_OPERATION;
    }
    mNewPosition = mProxy->getPosition() + mUpdatePeriod;
    mLoopPeriod = 0;
    // FIXME Check whether loops and setting position are incompatible in old code.
    // If we use setLoop for both purposes we lose the capability to set the position while looping.
    mStaticProxy->setLoop(position, mFrameCount, 0);

    return NO_ERROR;
}

status_t AudioTrack::getPosition(uint32_t *position) const
{
    if (position == NULL) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    if (isOffloaded()) {
        uint32_t dspFrames = 0;

        if (mOutput != 0) {
            uint32_t halFrames;
            AudioSystem::getRenderPosition(mOutput, &halFrames, &dspFrames);
        }
        *position = dspFrames;
    } else {
        // IAudioTrack::stop() isn't synchronous; we don't know when presentation completes
        *position = (mState == STATE_STOPPED || mState == STATE_FLUSHED) ? 0 :
                mProxy->getPosition();
    }
    return NO_ERROR;
}

status_t AudioTrack::getBufferPosition(size_t *position)
{
    if (mSharedBuffer == 0 || mIsTimed) {
        return INVALID_OPERATION;
    }
    if (position == NULL) {
        return BAD_VALUE;
    }

    AutoMutex lock(mLock);
    *position = mStaticProxy->getBufferPosition();
    return NO_ERROR;
}

status_t AudioTrack::reload()
{
    if (mSharedBuffer == 0 || mIsTimed || isOffloaded()) {
        return INVALID_OPERATION;
    }

    AutoMutex lock(mLock);
    // See setPosition() regarding setting parameters such as loop points or position while active
    if (mState == STATE_ACTIVE) {
        return INVALID_OPERATION;
    }
    mNewPosition = mUpdatePeriod;
    mLoopPeriod = 0;
    // FIXME The new code cannot reload while keeping a loop specified.
    // Need to check how the old code handled this, and whether it's a significant change.
    mStaticProxy->setLoop(0, mFrameCount, 0);
    return NO_ERROR;
}

audio_io_handle_t AudioTrack::getOutput()
{
    AutoMutex lock(mLock);
    return mOutput;
}

// must be called with mLock held
audio_io_handle_t AudioTrack::getOutput_l()
{
    if (mOutput) {
        return mOutput;
    } else {
        return AudioSystem::getOutput(mStreamType,
                                      mSampleRate, mFormat, mChannelMask, mFlags);
    }
}

status_t AudioTrack::attachAuxEffect(int effectId)
{
    AutoMutex lock(mLock);
    status_t status = mAudioTrack->attachAuxEffect(effectId);
    if (status == NO_ERROR) {
        mAuxEffectId = effectId;
    }
    return status;
}

// -------------------------------------------------------------------------

// must be called with mLock held
status_t AudioTrack::createTrack_l(
        audio_stream_type_t streamType,
        uint32_t sampleRate,
        audio_format_t format,
        size_t frameCount,
        audio_output_flags_t flags,
        const sp<IMemory>& sharedBuffer,
        audio_io_handle_t output,
        size_t epoch)
{
    status_t status;
    const sp<IAudioFlinger>& audioFlinger = AudioSystem::get_audio_flinger();
    if (audioFlinger == 0) {
        ALOGE("Could not get audioflinger");
        return NO_INIT;
    }

    // Not all of these values are needed under all conditions, but it is easier to get them all

    uint32_t afLatency;
    status = AudioSystem::getLatency(output, streamType, &afLatency);
    if (status != NO_ERROR) {
        ALOGE("getLatency(%d) failed status %d", output, status);
        return NO_INIT;
    }

    size_t afFrameCount;
    status = AudioSystem::getFrameCount(output, streamType, &afFrameCount);
    if (status != NO_ERROR) {
        ALOGE("getFrameCount(output=%d, streamType=%d) status %d", output, streamType, status);
        return NO_INIT;
    }

    uint32_t afSampleRate;
    status = AudioSystem::getSamplingRate(output, streamType, &afSampleRate);
    if (status != NO_ERROR) {
        ALOGE("getSamplingRate(output=%d, streamType=%d) status %d", output, streamType, status);
        return NO_INIT;
    }

    // Client decides whether the track is TIMED (see below), but can only express a preference
    // for FAST.  Server will perform additional tests.
    if ((flags & AUDIO_OUTPUT_FLAG_FAST) && !(
            // either of these use cases:
            // use case 1: shared buffer
            (sharedBuffer != 0) ||
            // use case 2: callback handler
            (mCbf != NULL))) {
        ALOGW("AUDIO_OUTPUT_FLAG_FAST denied by client");
        // once denied, do not request again if IAudioTrack is re-created
        flags = (audio_output_flags_t) (flags & ~AUDIO_OUTPUT_FLAG_FAST);
        mFlags = flags;
    }
    ALOGV("createTrack_l() output %d afLatency %d", output, afLatency);

    // The client's AudioTrack buffer is divided into n parts for purpose of wakeup by server, where
    //  n = 1   fast track; nBuffering is ignored
    //  n = 2   normal track, no sample rate conversion
    //  n = 3   normal track, with sample rate conversion
    //          (pessimistic; some non-1:1 conversion ratios don't actually need triple-buffering)
    //  n > 3   very high latency or very small notification interval; nBuffering is ignored
    const uint32_t nBuffering = (sampleRate == afSampleRate) ? 2 : 3;

    mNotificationFramesAct = mNotificationFramesReq;

    if (!audio_is_linear_pcm(format)) {

        if (sharedBuffer != 0) {
            // Same comment as below about ignoring frameCount parameter for set()
            frameCount = sharedBuffer->size();
        } else if (frameCount == 0) {
            frameCount = afFrameCount;
        }
        if (mNotificationFramesAct != frameCount) {
            mNotificationFramesAct = frameCount;
        }
    } else if (sharedBuffer != 0) {

        // Ensure that buffer alignment matches channel count
        // 8-bit data in shared memory is not currently supported by AudioFlinger
        size_t alignment = /* format == AUDIO_FORMAT_PCM_8_BIT ? 1 : */ 2;
        if (mChannelCount > 1) {
            // More than 2 channels does not require stronger alignment than stereo
            alignment <<= 1;
        }
        if (((size_t)sharedBuffer->pointer() & (alignment - 1)) != 0) {
            ALOGE("Invalid buffer alignment: address %p, channel count %u",
                    sharedBuffer->pointer(), mChannelCount);
            return BAD_VALUE;
        }

        // When initializing a shared buffer AudioTrack via constructors,
        // there's no frameCount parameter.
        // But when initializing a shared buffer AudioTrack via set(),
        // there _is_ a frameCount parameter.  We silently ignore it.
        frameCount = sharedBuffer->size()/mChannelCount/sizeof(int16_t);

    } else if (!(flags & AUDIO_OUTPUT_FLAG_FAST)) {

        // FIXME move these calculations and associated checks to server

        // Ensure that buffer depth covers at least audio hardware latency
        uint32_t minBufCount = afLatency / ((1000 * afFrameCount)/afSampleRate);
        ALOGV("afFrameCount=%d, minBufCount=%d, afSampleRate=%u, afLatency=%d",
                afFrameCount, minBufCount, afSampleRate, afLatency);
        if (minBufCount <= nBuffering) {
            minBufCount = nBuffering;
        }

        size_t minFrameCount = (afFrameCount*sampleRate*minBufCount)/afSampleRate;
        ALOGV("minFrameCount: %u, afFrameCount=%d, minBufCount=%d, sampleRate=%u, afSampleRate=%u"
                ", afLatency=%d",
                minFrameCount, afFrameCount, minBufCount, sampleRate, afSampleRate, afLatency);

        if (frameCount == 0) {
            frameCount = minFrameCount;
        } else if (frameCount < minFrameCount) {
            // not ALOGW because it happens all the time when playing key clicks over A2DP
            ALOGV("Minimum buffer size corrected from %d to %d",
                     frameCount, minFrameCount);
            frameCount = minFrameCount;
        }
        // Make sure that application is notified with sufficient margin before underrun
        if (mNotificationFramesAct == 0 || mNotificationFramesAct > frameCount/nBuffering) {
            mNotificationFramesAct = frameCount/nBuffering;
        }

    } else {
        // For fast tracks, the frame count calculations and checks are done by server
    }

    IAudioFlinger::track_flags_t trackFlags = IAudioFlinger::TRACK_DEFAULT;
    if (mIsTimed) {
        trackFlags |= IAudioFlinger::TRACK_TIMED;
    }

    pid_t tid = -1;
    if (flags & AUDIO_OUTPUT_FLAG_FAST) {
        trackFlags |= IAudioFlinger::TRACK_FAST;
        if (mAudioTrackThread != 0) {
            tid = mAudioTrackThread->getTid();
        }
    }

    if (flags & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD) {
        trackFlags |= IAudioFlinger::TRACK_OFFLOAD;
    }

    sp<IAudioTrack> track = audioFlinger->createTrack(streamType,
                                                      sampleRate,
                                                      // AudioFlinger only sees 16-bit PCM
                                                      format == AUDIO_FORMAT_PCM_8_BIT ?
                                                              AUDIO_FORMAT_PCM_16_BIT : format,
                                                      mChannelMask,
                                                      frameCount,
                                                      &trackFlags,
                                                      sharedBuffer,
                                                      output,
                                                      tid,
                                                      &mSessionId,
                                                      mName,
                                                      mClientUid,
                                                      &status);

    if (track == 0) {
        ALOGE("AudioFlinger could not create track, status: %d", status);
        return status;
    }
    sp<IMemory> iMem = track->getCblk();
    if (iMem == 0) {
        ALOGE("Could not get control block");
        return NO_INIT;
    }
    // invariant that mAudioTrack != 0 is true only after set() returns successfully
    if (mAudioTrack != 0) {
        mAudioTrack->asBinder()->unlinkToDeath(mDeathNotifier, this);
        mDeathNotifier.clear();
    }
    mAudioTrack = track;
    mCblkMemory = iMem;
    audio_track_cblk_t* cblk = static_cast<audio_track_cblk_t*>(iMem->pointer());
    mCblk = cblk;
    size_t temp = cblk->frameCount_;
    if (temp < frameCount || (frameCount == 0 && temp == 0)) {
        // In current design, AudioTrack client checks and ensures frame count validity before
        // passing it to AudioFlinger so AudioFlinger should not return a different value except
        // for fast track as it uses a special method of assigning frame count.
        ALOGW("Requested frameCount %u but received frameCount %u", frameCount, temp);
    }
    frameCount = temp;
    mAwaitBoost = false;
    if (flags & AUDIO_OUTPUT_FLAG_FAST) {
        if (trackFlags & IAudioFlinger::TRACK_FAST) {
            ALOGV("AUDIO_OUTPUT_FLAG_FAST successful; frameCount %u", frameCount);
            mAwaitBoost = true;
            if (sharedBuffer == 0) {
                // double-buffering is not required for fast tracks, due to tighter scheduling
                if (mNotificationFramesAct == 0 || mNotificationFramesAct > frameCount) {
                    mNotificationFramesAct = frameCount;
                }
            }
        } else {
            ALOGV("AUDIO_OUTPUT_FLAG_FAST denied by server; frameCount %u", frameCount);
            // once denied, do not request again if IAudioTrack is re-created
            flags = (audio_output_flags_t) (flags & ~AUDIO_OUTPUT_FLAG_FAST);
            mFlags = flags;
            if (sharedBuffer == 0) {
                if (mNotificationFramesAct == 0 || mNotificationFramesAct > frameCount/nBuffering) {
                    mNotificationFramesAct = frameCount/nBuffering;
                }
            }
        }
    }
    if (flags & AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD) {
        if (trackFlags & IAudioFlinger::TRACK_OFFLOAD) {
            ALOGV("AUDIO_OUTPUT_FLAG_OFFLOAD successful");
        } else {
            ALOGW("AUDIO_OUTPUT_FLAG_OFFLOAD denied by server");
            flags = (audio_output_flags_t) (flags & ~AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD);
            mFlags = flags;
            return NO_INIT;
        }
    }

    mRefreshRemaining = true;

    // Starting address of buffers in shared memory.  If there is a shared buffer, buffers
    // is the value of pointer() for the shared buffer, otherwise buffers points
    // immediately after the control block.  This address is for the mapping within client
    // address space.  AudioFlinger::TrackBase::mBuffer is for the server address space.
    void* buffers;
    if (sharedBuffer == 0) {
        buffers = (char*)cblk + sizeof(audio_track_cblk_t);
    } else {
        buffers = sharedBuffer->pointer();
    }

    mAudioTrack->attachAuxEffect(mAuxEffectId);
    // FIXME don't believe this lie
    mLatency = afLatency + (1000*frameCount) / sampleRate;
    mFrameCount = frameCount;
    // If IAudioTrack is re-created, don't let the requested frameCount
    // decrease.  This can confuse clients that cache frameCount().
    if (frameCount > mReqFrameCount) {
        mReqFrameCount = frameCount;
    }

    // update proxy
    if (sharedBuffer == 0) {
        mStaticProxy.clear();
        mProxy = new AudioTrackClientProxy(cblk, buffers, frameCount, mFrameSizeAF);
    } else {
        mStaticProxy = new StaticAudioTrackClientProxy(cblk, buffers, frameCount, mFrameSizeAF);
        mProxy = mStaticProxy;
    }
    mProxy->setVolumeLR((uint32_t(uint16_t(mVolume[RIGHT] * 0x1000)) << 16) |
            uint16_t(mVolume[LEFT] * 0x1000));
    mProxy->setSendLevel(mSendLevel);
    mProxy->setSampleRate(mSampleRate);
    mProxy->setEpoch(epoch);
    mProxy->setMinimum(mNotificationFramesAct);

    mDeathNotifier = new DeathNotifier(this);
    mAudioTrack->asBinder()->linkToDeath(mDeathNotifier, this);

    return NO_ERROR;
}

status_t AudioTrack::obtainBuffer(Buffer* audioBuffer, int32_t waitCount)
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

status_t AudioTrack::obtainBuffer(Buffer* audioBuffer, const struct timespec *requested,
        struct timespec *elapsed, size_t *nonContig)
{
    // previous and new IAudioTrack sequence numbers are used to detect track re-creation
    uint32_t oldSequence = 0;
    uint32_t newSequence;

    Proxy::Buffer buffer;
    status_t status = NO_ERROR;

    static const int32_t kMaxTries = 5;
    int32_t tryCounter = kMaxTries;

    do {
        // obtainBuffer() is called with mutex unlocked, so keep extra references to these fields to
        // keep them from going away if another thread re-creates the track during obtainBuffer()
        sp<AudioTrackClientProxy> proxy;
        sp<IMemory> iMem;

        {   // start of lock scope
            AutoMutex lock(mLock);

            newSequence = mSequence;
            // did previous obtainBuffer() fail due to media server death or voluntary invalidation?
            if (status == DEAD_OBJECT) {
                // re-create track, unless someone else has already done so
                if (newSequence == oldSequence) {
                    status = restoreTrack_l("obtainBuffer");
                    if (status != NO_ERROR) {
                        buffer.mFrameCount = 0;
                        buffer.mRaw = NULL;
                        buffer.mNonContig = 0;
                        break;
                    }
                }
            }
            oldSequence = newSequence;

            // Keep the extra references
            proxy = mProxy;
            iMem = mCblkMemory;

            if (mState == STATE_STOPPING) {
                status = -EINTR;
                buffer.mFrameCount = 0;
                buffer.mRaw = NULL;
                buffer.mNonContig = 0;
                break;
            }

            // Non-blocking if track is stopped or paused
            if (mState != STATE_ACTIVE) {
                requested = &ClientProxy::kNonBlocking;
            }

        }   // end of lock scope

        buffer.mFrameCount = audioBuffer->frameCount;
        // FIXME starts the requested timeout and elapsed over from scratch
        status = proxy->obtainBuffer(&buffer, requested, elapsed);

    } while ((status == DEAD_OBJECT) && (tryCounter-- > 0));

    audioBuffer->frameCount = buffer.mFrameCount;
    audioBuffer->size = buffer.mFrameCount * mFrameSizeAF;
    audioBuffer->raw = buffer.mRaw;
    if (nonContig != NULL) {
        *nonContig = buffer.mNonContig;
    }
    return status;
}

void AudioTrack::releaseBuffer(Buffer* audioBuffer)
{
    if (mTransfer == TRANSFER_SHARED) {
        return;
    }

    size_t stepCount = audioBuffer->size / mFrameSizeAF;
    if (stepCount == 0) {
        return;
    }

    Proxy::Buffer buffer;
    buffer.mFrameCount = stepCount;
    buffer.mRaw = audioBuffer->raw;

    AutoMutex lock(mLock);
    mInUnderrun = false;
    mProxy->releaseBuffer(&buffer);

    // restart track if it was disabled by audioflinger due to previous underrun
    if (mState == STATE_ACTIVE) {
        audio_track_cblk_t* cblk = mCblk;
        if (android_atomic_and(~CBLK_DISABLED, &cblk->mFlags) & CBLK_DISABLED) {
            ALOGW("releaseBuffer() track %p name=%s disabled due to previous underrun, restarting",
                    this, mName.string());
            // FIXME ignoring status
            mAudioTrack->start();
        }
    }
}

// -------------------------------------------------------------------------

ssize_t AudioTrack::write(const void* buffer, size_t userSize)
{
    if (mTransfer != TRANSFER_SYNC || mIsTimed) {
        return INVALID_OPERATION;
    }

    if (ssize_t(userSize) < 0 || (buffer == NULL && userSize != 0)) {
        // Sanity-check: user is most-likely passing an error code, and it would
        // make the return value ambiguous (actualSize vs error).
        ALOGE("AudioTrack::write(buffer=%p, size=%u (%d)", buffer, userSize, userSize);
        return BAD_VALUE;
    }

    size_t written = 0;
    Buffer audioBuffer;

    while (userSize >= mFrameSize) {
        audioBuffer.frameCount = userSize / mFrameSize;

        status_t err = obtainBuffer(&audioBuffer, &ClientProxy::kForever);
        if (err < 0) {
            if (written > 0) {
                break;
            }
            return ssize_t(err);
        }

        size_t toWrite;
        if (mFormat == AUDIO_FORMAT_PCM_8_BIT && !(mFlags & AUDIO_OUTPUT_FLAG_DIRECT)) {
            // Divide capacity by 2 to take expansion into account
            toWrite = audioBuffer.size >> 1;
            memcpy_to_i16_from_u8(audioBuffer.i16, (const uint8_t *) buffer, toWrite);
        } else {
            toWrite = audioBuffer.size;
            memcpy(audioBuffer.i8, buffer, toWrite);
        }
        buffer = ((const char *) buffer) + toWrite;
        userSize -= toWrite;
        written += toWrite;

        releaseBuffer(&audioBuffer);
    }

    return written;
}

// -------------------------------------------------------------------------

TimedAudioTrack::TimedAudioTrack() {
    mIsTimed = true;
}

status_t TimedAudioTrack::allocateTimedBuffer(size_t size, sp<IMemory>* buffer)
{
    AutoMutex lock(mLock);
    status_t result = UNKNOWN_ERROR;

#if 1
    // acquire a strong reference on the IMemory and IAudioTrack so that they cannot be destroyed
    // while we are accessing the cblk
    sp<IAudioTrack> audioTrack = mAudioTrack;
    sp<IMemory> iMem = mCblkMemory;
#endif

    // If the track is not invalid already, try to allocate a buffer.  alloc
    // fails indicating that the server is dead, flag the track as invalid so
    // we can attempt to restore in just a bit.
    audio_track_cblk_t* cblk = mCblk;
    if (!(cblk->mFlags & CBLK_INVALID)) {
        result = mAudioTrack->allocateTimedBuffer(size, buffer);
        if (result == DEAD_OBJECT) {
            android_atomic_or(CBLK_INVALID, &cblk->mFlags);
        }
    }

    // If the track is invalid at this point, attempt to restore it. and try the
    // allocation one more time.
    if (cblk->mFlags & CBLK_INVALID) {
        result = restoreTrack_l("allocateTimedBuffer");

        if (result == NO_ERROR) {
            result = mAudioTrack->allocateTimedBuffer(size, buffer);
        }
    }

    return result;
}

status_t TimedAudioTrack::queueTimedBuffer(const sp<IMemory>& buffer,
                                           int64_t pts)
{
    status_t status = mAudioTrack->queueTimedBuffer(buffer, pts);
    {
        AutoMutex lock(mLock);
        audio_track_cblk_t* cblk = mCblk;
        // restart track if it was disabled by audioflinger due to previous underrun
        if (buffer->size() != 0 && status == NO_ERROR &&
                (mState == STATE_ACTIVE) && (cblk->mFlags & CBLK_DISABLED)) {
            android_atomic_and(~CBLK_DISABLED, &cblk->mFlags);
            ALOGW("queueTimedBuffer() track %p disabled, restarting", this);
            // FIXME ignoring status
            mAudioTrack->start();
        }
    }
    return status;
}

status_t TimedAudioTrack::setMediaTimeTransform(const LinearTransform& xform,
                                                TargetTimeline target)
{
    return mAudioTrack->setMediaTimeTransform(xform, target);
}

// -------------------------------------------------------------------------

nsecs_t AudioTrack::processAudioBuffer(const sp<AudioTrackThread>& thread)
{
    // Currently the AudioTrack thread is not created if there are no callbacks.
    // Would it ever make sense to run the thread, even without callbacks?
    // If so, then replace this by checks at each use for mCbf != NULL.
    LOG_ALWAYS_FATAL_IF(mCblk == NULL);

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
    int32_t flags = android_atomic_and(
        ~(CBLK_UNDERRUN | CBLK_LOOP_CYCLE | CBLK_LOOP_FINAL | CBLK_BUFFER_END), &mCblk->mFlags);

    // Check for track invalidation
    if (flags & CBLK_INVALID) {
        // for offloaded tracks restoreTrack_l() will just update the sequence and clear
        // AudioSystem cache. We should not exit here but after calling the callback so
        // that the upper layers can recreate the track
        if (!isOffloaded() || (mSequence == mObservedSequence)) {
            status_t status = restoreTrack_l("processAudioBuffer");
            mLock.unlock();
            // Run again immediately, but with a new IAudioTrack
            return 0;
        }
    }

    bool waitStreamEnd = mState == STATE_STOPPING;
    bool active = mState == STATE_ACTIVE;

    // Manage underrun callback, must be done under lock to avoid race with releaseBuffer()
    bool newUnderrun = false;
    if (flags & CBLK_UNDERRUN) {
#if 0
        // Currently in shared buffer mode, when the server reaches the end of buffer,
        // the track stays active in continuous underrun state.  It's up to the application
        // to pause or stop the track, or set the position to a new offset within buffer.
        // This was some experimental code to auto-pause on underrun.   Keeping it here
        // in "if 0" so we can re-visit this if we add a real sequencer for shared memory content.
        if (mTransfer == TRANSFER_SHARED) {
            mState = STATE_PAUSED;
            active = false;
        }
#endif
        if (!mInUnderrun) {
            mInUnderrun = true;
            newUnderrun = true;
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

    // Determine number of new position callback(s) that will be needed, while locked
    size_t newPosCount = 0;
    size_t newPosition = mNewPosition;
    size_t updatePeriod = mUpdatePeriod;
    // FIXME fails for wraparound, need 64 bits
    if (updatePeriod > 0 && position >= newPosition) {
        newPosCount = ((position - newPosition) / updatePeriod) + 1;
        mNewPosition += updatePeriod * newPosCount;
    }

    // Cache other fields that will be needed soon
    uint32_t loopPeriod = mLoopPeriod;
    uint32_t sampleRate = mSampleRate;
    size_t notificationFrames = mNotificationFramesAct;
    if (mRefreshRemaining) {
        mRefreshRemaining = false;
        mRemainingFrames = notificationFrames;
        mRetryOnPartialBuffer = false;
    }
    size_t misalignment = mProxy->getMisalignment();
    uint32_t sequence = mSequence;

    // These fields don't need to be cached, because they are assigned only by set():
    //     mTransfer, mCbf, mUserData, mFormat, mFrameSize, mFrameSizeAF, mFlags
    // mFlags is also assigned by createTrack_l(), but not the bit we care about.

    mLock.unlock();

    if (waitStreamEnd) {
        AutoMutex lock(mLock);

        sp<AudioTrackClientProxy> proxy = mProxy;
        sp<IMemory> iMem = mCblkMemory;

        struct timespec timeout;
        timeout.tv_sec = WAIT_STREAM_END_TIMEOUT_SEC;
        timeout.tv_nsec = 0;

        mLock.unlock();
        status_t status = mProxy->waitStreamEndDone(&timeout);
        mLock.lock();
        switch (status) {
        case NO_ERROR:
        case DEAD_OBJECT:
        case TIMED_OUT:
            mLock.unlock();
            mCbf(EVENT_STREAM_END, mUserData, NULL);
            mLock.lock();
            if (mState == STATE_STOPPING) {
                mState = STATE_STOPPED;
                if (status != DEAD_OBJECT) {
                   return NS_INACTIVE;
                }
            }
            return 0;
        default:
            return 0;
        }
    }

    // perform callbacks while unlocked
    if (newUnderrun) {
        mCbf(EVENT_UNDERRUN, mUserData, NULL);
    }
    // FIXME we will miss loops if loop cycle was signaled several times since last call
    //       to processAudioBuffer()
    if (flags & (CBLK_LOOP_CYCLE | CBLK_LOOP_FINAL)) {
        mCbf(EVENT_LOOP_END, mUserData, NULL);
    }
    if (flags & CBLK_BUFFER_END) {
        mCbf(EVENT_BUFFER_END, mUserData, NULL);
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
        mCbf(EVENT_NEW_IAUDIOTRACK, mUserData, NULL);
        // for offloaded tracks, just wait for the upper layers to recreate the track
        if (isOffloaded()) {
            return NS_INACTIVE;
        }
    }

    // if inactive, then don't run me again until re-started
    if (!active) {
        return NS_INACTIVE;
    }

    // Compute the estimated time until the next timed event (position, markers, loops)
    // FIXME only for non-compressed audio
    uint32_t minFrames = ~0;
    if (!markerReached && position < markerPosition) {
        minFrames = markerPosition - position;
    }
    if (loopPeriod > 0 && loopPeriod < minFrames) {
        minFrames = loopPeriod;
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
        ns = ((minFrames * 1000000000LL) / sampleRate) + kFudgeNs;
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
        ALOGV("obtainBuffer(%u) returned %u = %u + %u err %d",
                mRemainingFrames, avail, audioBuffer.frameCount, nonContig, err);
        if (err != NO_ERROR) {
            if (err == TIMED_OUT || err == WOULD_BLOCK || err == -EINTR ||
                    (isOffloaded() && (err == DEAD_OBJECT))) {
                return 0;
            }
            ALOGE("Error %d obtaining an audio buffer, giving up.", err);
            return NS_NEVER;
        }

        if (mRetryOnPartialBuffer && !isOffloaded()) {
            mRetryOnPartialBuffer = false;
            if (avail < mRemainingFrames) {
                int64_t myns = ((mRemainingFrames - avail) * 1100000000LL) / sampleRate;
                if (ns < 0 || myns < ns) {
                    ns = myns;
                }
                return ns;
            }
        }

        // Divide buffer size by 2 to take into account the expansion
        // due to 8 to 16 bit conversion: the callback must fill only half
        // of the destination buffer
        if (mFormat == AUDIO_FORMAT_PCM_8_BIT && !(mFlags & AUDIO_OUTPUT_FLAG_DIRECT)) {
            audioBuffer.size >>= 1;
        }

        size_t reqSize = audioBuffer.size;
        mCbf(EVENT_MORE_DATA, mUserData, &audioBuffer);
        size_t writtenSize = audioBuffer.size;
        size_t writtenFrames = writtenSize / mFrameSize;

        // Sanity check on returned size
        if (ssize_t(writtenSize) < 0 || writtenSize > reqSize) {
            ALOGE("EVENT_MORE_DATA requested %u bytes but callback returned %d bytes",
                    reqSize, (int) writtenSize);
            return NS_NEVER;
        }

        if (writtenSize == 0) {
            // The callback is done filling buffers
            // Keep this thread going to handle timed events and
            // still try to get more data in intervals of WAIT_PERIOD_MS
            // but don't just loop and block the CPU, so wait
            return WAIT_PERIOD_MS * 1000000LL;
        }

        if (mFormat == AUDIO_FORMAT_PCM_8_BIT && !(mFlags & AUDIO_OUTPUT_FLAG_DIRECT)) {
            // 8 to 16 bit conversion, note that source and destination are the same address
            memcpy_to_i16_from_u8(audioBuffer.i16, (const uint8_t *) audioBuffer.i8, writtenSize);
            audioBuffer.size <<= 1;
        }

        size_t releasedFrames = audioBuffer.size / mFrameSizeAF;
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
        if (writtenSize < reqSize) {
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
            return (mRemainingFrames * 1100000000LL) / sampleRate;
        }
#endif

    }
    mRemainingFrames = notificationFrames;
    mRetryOnPartialBuffer = true;

    // A lot has transpired since ns was calculated, so run again immediately and re-calculate
    return 0;
}

status_t AudioTrack::restoreTrack_l(const char *from)
{
    ALOGW("dead IAudioTrack, %s, creating a new one from %s()",
          isOffloaded() ? "Offloaded" : "PCM", from);
    ++mSequence;
    status_t result;

    // refresh the audio configuration cache in this process to make sure we get new
    // output parameters in getOutput_l() and createTrack_l()
    AudioSystem::clearAudioConfigCache();

    if (isOffloaded()) {
        return DEAD_OBJECT;
    }

    // force new output query from audio policy manager;
    mOutput = 0;
    audio_io_handle_t output = getOutput_l();

    // if the new IAudioTrack is created, createTrack_l() will modify the
    // following member variables: mAudioTrack, mCblkMemory and mCblk.
    // It will also delete the strong references on previous IAudioTrack and IMemory

    // take the frames that will be lost by track recreation into account in saved position
    size_t position = mProxy->getPosition() + mProxy->getFramesFilled();
    mNewPosition = position + mUpdatePeriod;
    size_t bufferPosition = mStaticProxy != NULL ? mStaticProxy->getBufferPosition() : 0;
    result = createTrack_l(mStreamType,
                           mSampleRate,
                           mFormat,
                           mReqFrameCount,  // so that frame count never goes down
                           mFlags,
                           mSharedBuffer,
                           output,
                           position /*epoch*/);

    if (result == NO_ERROR) {
        // continue playback from last known position, but
        // don't attempt to restore loop after invalidation; it's difficult and not worthwhile
        if (mStaticProxy != NULL) {
            mLoopPeriod = 0;
            mStaticProxy->setLoop(bufferPosition, mFrameCount, 0);
        }
        // FIXME How do we simulate the fact that all frames present in the buffer at the time of
        //       track destruction have been played? This is critical for SoundPool implementation
        //       This must be broken, and needs to be tested/debugged.
#if 0
        // restore write index and set other indexes to reflect empty buffer status
        if (!strcmp(from, "start")) {
            // Make sure that a client relying on callback events indicating underrun or
            // the actual amount of audio frames played (e.g SoundPool) receives them.
            if (mSharedBuffer == 0) {
                // restart playback even if buffer is not completely filled.
                android_atomic_or(CBLK_FORCEREADY, &mCblk->mFlags);
            }
        }
#endif
        if (mState == STATE_ACTIVE) {
            result = mAudioTrack->start();
        }
    }
    if (result != NO_ERROR) {
        //Use of direct and offloaded output streams is ref counted by audio policy manager.
        // As getOutput was called above and resulted in an output stream to be opened,
        // we need to release it.
        AudioSystem::releaseOutput(output);
        ALOGW("restoreTrack_l() failed status %d", result);
        mState = STATE_STOPPED;
    }

    return result;
}

status_t AudioTrack::setParameters(const String8& keyValuePairs)
{
    AutoMutex lock(mLock);
    return mAudioTrack->setParameters(keyValuePairs);
}

status_t AudioTrack::getTimestamp(AudioTimestamp& timestamp)
{
    AutoMutex lock(mLock);
    // FIXME not implemented for fast tracks; should use proxy and SSQ
    if (mFlags & AUDIO_OUTPUT_FLAG_FAST) {
        return INVALID_OPERATION;
    }
    if (mState != STATE_ACTIVE && mState != STATE_PAUSED) {
        return INVALID_OPERATION;
    }
    status_t status = mAudioTrack->getTimestamp(timestamp);
    if (status == NO_ERROR) {
        timestamp.mPosition += mProxy->getEpoch();
    }
    return status;
}

String8 AudioTrack::getParameters(const String8& keys)
{
    if (mOutput) {
        return AudioSystem::getParameters(mOutput, keys);
    } else {
        return String8::empty();
    }
}

status_t AudioTrack::dump(int fd, const Vector<String16>& args) const
{

    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    result.append(" AudioTrack::dump\n");
    snprintf(buffer, 255, "  stream type(%d), left - right volume(%f, %f)\n", mStreamType,
            mVolume[0], mVolume[1]);
    result.append(buffer);
    snprintf(buffer, 255, "  format(%d), channel count(%d), frame count(%d)\n", mFormat,
            mChannelCount, mFrameCount);
    result.append(buffer);
    snprintf(buffer, 255, "  sample rate(%u), status(%d)\n", mSampleRate, mStatus);
    result.append(buffer);
    snprintf(buffer, 255, "  state(%d), latency (%d)\n", mState, mLatency);
    result.append(buffer);
    ::write(fd, result.string(), result.size());
    return NO_ERROR;
}

uint32_t AudioTrack::getUnderrunFrames() const
{
    AutoMutex lock(mLock);
    return mProxy->getUnderrunFrames();
}

// =========================================================================

void AudioTrack::DeathNotifier::binderDied(const wp<IBinder>& who)
{
    sp<AudioTrack> audioTrack = mAudioTrack.promote();
    if (audioTrack != 0) {
        AutoMutex lock(audioTrack->mLock);
        audioTrack->mProxy->binderDied();
    }
}

// =========================================================================

AudioTrack::AudioTrackThread::AudioTrackThread(AudioTrack& receiver, bool bCanCallJava)
    : Thread(bCanCallJava), mReceiver(receiver), mPaused(true), mPausedInt(false), mPausedNs(0LL),
      mIgnoreNextPausedInt(false)
{
}

AudioTrack::AudioTrackThread::~AudioTrackThread()
{
}

bool AudioTrack::AudioTrackThread::threadLoop()
{
    {
        AutoMutex _l(mMyLock);
        if (mPaused) {
            mMyCond.wait(mMyLock);
            // caller will check for exitPending()
            return true;
        }
        if (mIgnoreNextPausedInt) {
            mIgnoreNextPausedInt = false;
            mPausedInt = false;
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
    nsecs_t ns = mReceiver.processAudioBuffer(this);
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

void AudioTrack::AudioTrackThread::requestExit()
{
    // must be in this order to avoid a race condition
    Thread::requestExit();
    resume();
}

void AudioTrack::AudioTrackThread::pause()
{
    AutoMutex _l(mMyLock);
    mPaused = true;
}

void AudioTrack::AudioTrackThread::resume()
{
    AutoMutex _l(mMyLock);
    mIgnoreNextPausedInt = true;
    if (mPaused || mPausedInt) {
        mPaused = false;
        mPausedInt = false;
        mMyCond.signal();
    }
}

void AudioTrack::AudioTrackThread::pauseInternal(nsecs_t ns)
{
    AutoMutex _l(mMyLock);
    mPausedInt = true;
    mPausedNs = ns;
}

}; // namespace android

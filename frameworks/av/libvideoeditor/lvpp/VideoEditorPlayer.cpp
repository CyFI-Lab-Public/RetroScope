/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_NDEBUG 1
#define LOG_TAG "VideoEditorPlayer"
#include <utils/Log.h>

#include "VideoEditorPlayer.h"
#include "PreviewPlayer.h"

#include <media/Metadata.h>
#include <media/stagefright/MediaExtractor.h>

#include <system/audio.h>

namespace android {

VideoEditorPlayer::VideoEditorPlayer(NativeWindowRenderer* renderer)
    : mPlayer(new PreviewPlayer(renderer)) {

    ALOGV("VideoEditorPlayer");
    mPlayer->setListener(this);
}

VideoEditorPlayer::~VideoEditorPlayer() {
    ALOGV("~VideoEditorPlayer");

    reset();
    mVeAudioSink.clear();

    delete mPlayer;
    mPlayer = NULL;
}

status_t VideoEditorPlayer::initCheck() {
    ALOGV("initCheck");
    return OK;
}


status_t VideoEditorPlayer::setAudioPlayer(VideoEditorAudioPlayer *audioPlayer) {
    return mPlayer->setAudioPlayer(audioPlayer);
}


status_t VideoEditorPlayer::setDataSource(
        const char *url, const KeyedVector<String8, String8> *headers) {
    ALOGI("setDataSource('%s')", url);
    if (headers != NULL) {
        ALOGE("Headers parameter is not supported");
        return INVALID_OPERATION;
    }

    return mPlayer->setDataSource(url);
}

//We donot use this in preview, dummy implimentation as this is pure virtual
status_t VideoEditorPlayer::setDataSource(int fd, int64_t offset,
    int64_t length) {
    ALOGE("setDataSource(%d, %lld, %lld) Not supported", fd, offset, length);
    return (!OK);
}

status_t VideoEditorPlayer::setVideoSurface(const sp<Surface> &surface) {
    ALOGV("setVideoSurface");

    mPlayer->setSurface(surface);
    return OK;
}

status_t VideoEditorPlayer::setVideoSurfaceTexture(const sp<IGraphicBufferProducer> &bufferProducer) {
    ALOGV("setVideoSurfaceTexture");

    mPlayer->setSurfaceTexture(bufferProducer);
    return OK;
}

status_t VideoEditorPlayer::prepare() {
    ALOGV("prepare");
    return mPlayer->prepare();
}

status_t VideoEditorPlayer::prepareAsync() {
    return mPlayer->prepareAsync();
}

status_t VideoEditorPlayer::start() {
    ALOGV("start");
    return mPlayer->play();
}

status_t VideoEditorPlayer::stop() {
    ALOGV("stop");
    return pause();
}

status_t VideoEditorPlayer::pause() {
    ALOGV("pause");
    return mPlayer->pause();
}

bool VideoEditorPlayer::isPlaying() {
    ALOGV("isPlaying");
    return mPlayer->isPlaying();
}

status_t VideoEditorPlayer::seekTo(int msec) {
    ALOGV("seekTo");
    status_t err = mPlayer->seekTo((int64_t)msec * 1000);
    return err;
}

status_t VideoEditorPlayer::getCurrentPosition(int *msec) {
    ALOGV("getCurrentPosition");
    int64_t positionUs;
    status_t err = mPlayer->getPosition(&positionUs);

    if (err != OK) {
        return err;
    }

    *msec = (positionUs + 500) / 1000;
    return OK;
}

status_t VideoEditorPlayer::getDuration(int *msec) {
    ALOGV("getDuration");

    int64_t durationUs;
    status_t err = mPlayer->getDuration(&durationUs);

    if (err != OK) {
        *msec = 0;
        return OK;
    }

    *msec = (durationUs + 500) / 1000;
    return OK;
}

status_t VideoEditorPlayer::reset() {
    ALOGV("reset");
    mPlayer->reset();
    return OK;
}

status_t VideoEditorPlayer::setLooping(int loop) {
    ALOGV("setLooping");
    return mPlayer->setLooping(loop);
}

status_t VideoEditorPlayer::setParameter(int key, const Parcel &request) {
    ALOGE("setParameter not implemented");
    return INVALID_OPERATION;
}

status_t VideoEditorPlayer::getParameter(int key, Parcel *reply) {
    ALOGE("getParameter not implemented");
    return INVALID_OPERATION;
}

player_type VideoEditorPlayer::playerType() {
    ALOGV("playerType");
    return STAGEFRIGHT_PLAYER;
}

void VideoEditorPlayer::acquireLock() {
    ALOGV("acquireLock");
    mPlayer->acquireLock();
}

void VideoEditorPlayer::releaseLock() {
    ALOGV("releaseLock");
    mPlayer->releaseLock();
}

status_t VideoEditorPlayer::invoke(const Parcel &request, Parcel *reply) {
    return INVALID_OPERATION;
}

void VideoEditorPlayer::setAudioSink(const sp<AudioSink> &audioSink) {
    MediaPlayerInterface::setAudioSink(audioSink);

    mPlayer->setAudioSink(audioSink);
}

status_t VideoEditorPlayer::getMetadata(
        const media::Metadata::Filter& ids, Parcel *records) {
    using media::Metadata;

    uint32_t flags = mPlayer->getSourceSeekFlags();

    Metadata metadata(records);

    metadata.appendBool(
            Metadata::kPauseAvailable,
            flags & MediaExtractor::CAN_PAUSE);

    metadata.appendBool(
            Metadata::kSeekBackwardAvailable,
            flags & MediaExtractor::CAN_SEEK_BACKWARD);

    metadata.appendBool(
            Metadata::kSeekForwardAvailable,
            flags & MediaExtractor::CAN_SEEK_FORWARD);

    metadata.appendBool(
            Metadata::kSeekAvailable,
            flags & MediaExtractor::CAN_SEEK);

    return OK;
}

status_t VideoEditorPlayer::loadEffectsSettings(
    M4VSS3GPP_EffectSettings* pEffectSettings, int nEffects) {
    ALOGV("loadEffectsSettings");
    return mPlayer->loadEffectsSettings(pEffectSettings, nEffects);
}

status_t VideoEditorPlayer::loadAudioMixSettings(
    M4xVSS_AudioMixingSettings* pAudioMixSettings) {
    ALOGV("VideoEditorPlayer: loadAudioMixSettings");
    return mPlayer->loadAudioMixSettings(pAudioMixSettings);
}

status_t VideoEditorPlayer::setAudioMixPCMFileHandle(
    M4OSA_Context pAudioMixPCMFileHandle) {

    ALOGV("VideoEditorPlayer: loadAudioMixSettings");
    return mPlayer->setAudioMixPCMFileHandle(pAudioMixPCMFileHandle);
}

status_t VideoEditorPlayer::setAudioMixStoryBoardParam(
    M4OSA_UInt32 audioMixStoryBoardTS,
    M4OSA_UInt32 currentMediaBeginCutTime,
    M4OSA_UInt32 primaryTrackVolValue) {

    ALOGV("VideoEditorPlayer: loadAudioMixSettings");
    return mPlayer->setAudioMixStoryBoardParam(audioMixStoryBoardTS,
     currentMediaBeginCutTime, primaryTrackVolValue);
}

status_t VideoEditorPlayer::setPlaybackBeginTime(uint32_t msec) {
    ALOGV("setPlaybackBeginTime");
    return mPlayer->setPlaybackBeginTime(msec);
}

status_t VideoEditorPlayer::setPlaybackEndTime(uint32_t msec) {
    ALOGV("setPlaybackEndTime");
    return mPlayer->setPlaybackEndTime(msec);
}

status_t VideoEditorPlayer::setStoryboardStartTime(uint32_t msec) {
    ALOGV("setStoryboardStartTime");
    return mPlayer->setStoryboardStartTime(msec);
}

status_t VideoEditorPlayer::setProgressCallbackInterval(uint32_t cbInterval) {
    ALOGV("setProgressCallbackInterval");
    return mPlayer->setProgressCallbackInterval(cbInterval);
}

status_t VideoEditorPlayer::setMediaRenderingMode(
    M4xVSS_MediaRendering mode,
    M4VIDEOEDITING_VideoFrameSize outputVideoSize) {

    ALOGV("setMediaRenderingMode");
    return mPlayer->setMediaRenderingMode(mode, outputVideoSize);
}

status_t VideoEditorPlayer::resetJniCallbackTimeStamp() {
    ALOGV("resetJniCallbackTimeStamp");
    return mPlayer->resetJniCallbackTimeStamp();
}

status_t VideoEditorPlayer::setImageClipProperties(
    uint32_t width, uint32_t height) {
    return mPlayer->setImageClipProperties(width, height);
}

status_t VideoEditorPlayer::readFirstVideoFrame() {
    return mPlayer->readFirstVideoFrame();
}

status_t VideoEditorPlayer::getLastRenderedTimeMs(uint32_t *lastRenderedTimeMs) {
    mPlayer->getLastRenderedTimeMs(lastRenderedTimeMs);
    return NO_ERROR;
}

/* Implementation of AudioSink interface */
#undef LOG_TAG
#define LOG_TAG "VeAudioSink"

int VideoEditorPlayer::VeAudioOutput::mMinBufferCount = 4;
bool VideoEditorPlayer::VeAudioOutput::mIsOnEmulator = false;

VideoEditorPlayer::VeAudioOutput::VeAudioOutput()
    : mCallback(NULL),
      mCallbackCookie(NULL) {
    mStreamType = AUDIO_STREAM_MUSIC;
    mLeftVolume = 1.0;
    mRightVolume = 1.0;
    mLatency = 0;
    mMsecsPerFrame = 0;
    mNumFramesWritten = 0;
    setMinBufferCount();
}

VideoEditorPlayer::VeAudioOutput::~VeAudioOutput() {
    close();
}

void VideoEditorPlayer::VeAudioOutput::setMinBufferCount() {

    mIsOnEmulator = false;
    mMinBufferCount = 4;
}

bool VideoEditorPlayer::VeAudioOutput::isOnEmulator() {

    setMinBufferCount();
    return mIsOnEmulator;
}

int VideoEditorPlayer::VeAudioOutput::getMinBufferCount() {

    setMinBufferCount();
    return mMinBufferCount;
}

ssize_t VideoEditorPlayer::VeAudioOutput::bufferSize() const {

    if (mTrack == 0) return NO_INIT;
    return mTrack->frameCount() * frameSize();
}

ssize_t VideoEditorPlayer::VeAudioOutput::frameCount() const {

    if (mTrack == 0) return NO_INIT;
    return mTrack->frameCount();
}

ssize_t VideoEditorPlayer::VeAudioOutput::channelCount() const
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->channelCount();
}

ssize_t VideoEditorPlayer::VeAudioOutput::frameSize() const
{
    if (mTrack == 0) return NO_INIT;
    return mTrack->frameSize();
}

uint32_t VideoEditorPlayer::VeAudioOutput::latency () const
{
    return mLatency;
}

float VideoEditorPlayer::VeAudioOutput::msecsPerFrame() const
{
    return mMsecsPerFrame;
}

status_t VideoEditorPlayer::VeAudioOutput::getPosition(uint32_t *position) const {

    if (mTrack == 0) return NO_INIT;
    return mTrack->getPosition(position);
}

status_t VideoEditorPlayer::VeAudioOutput::getFramesWritten(uint32_t *written) const {

    if (mTrack == 0) return NO_INIT;
    *written = mNumFramesWritten;
    return OK;
}

status_t VideoEditorPlayer::VeAudioOutput::open(
        uint32_t sampleRate, int channelCount, audio_channel_mask_t channelMask,
        audio_format_t format, int bufferCount,
        AudioCallback cb, void *cookie, audio_output_flags_t flags,
        const audio_offload_info_t *offloadInfo) {

    mCallback = cb;
    mCallbackCookie = cookie;

    // Check argument "bufferCount" against the mininum buffer count
    if (bufferCount < mMinBufferCount) {
        ALOGV("bufferCount (%d) is too small and increased to %d",
            bufferCount, mMinBufferCount);
        bufferCount = mMinBufferCount;

    }
    ALOGV("open(%u, %d, %d, %d)", sampleRate, channelCount, format, bufferCount);
    if (mTrack != 0) close();
    uint32_t afSampleRate;
    size_t afFrameCount;
    int frameCount;

    if (AudioSystem::getOutputFrameCount(&afFrameCount, mStreamType) !=
     NO_ERROR) {
        return NO_INIT;
    }
    if (AudioSystem::getOutputSamplingRate(&afSampleRate, mStreamType) !=
     NO_ERROR) {
        return NO_INIT;
    }

    frameCount = (sampleRate*afFrameCount*bufferCount)/afSampleRate;

    if (channelMask == CHANNEL_MASK_USE_CHANNEL_ORDER) {
        switch(channelCount) {
          case 1:
            channelMask = AUDIO_CHANNEL_OUT_MONO;
            break;
          case 2:
            channelMask = AUDIO_CHANNEL_OUT_STEREO;
            break;
          default:
            return NO_INIT;
        }
    }

    sp<AudioTrack> t;
    if (mCallback != NULL) {
        t = new AudioTrack(
                mStreamType,
                sampleRate,
                format,
                channelMask,
                frameCount,
                flags,
                CallbackWrapper,
                this);
    } else {
        t = new AudioTrack(
                mStreamType,
                sampleRate,
                format,
                channelMask,
                frameCount,
                flags);
    }

    if ((t == 0) || (t->initCheck() != NO_ERROR)) {
        ALOGE("Unable to create audio track");
        return NO_INIT;
    }

    ALOGV("setVolume");
    t->setVolume(mLeftVolume, mRightVolume);
    mMsecsPerFrame = 1.e3 / (float) sampleRate;
    mLatency = t->latency();
    mTrack = t;
    return NO_ERROR;
}

status_t VideoEditorPlayer::VeAudioOutput::start() {

    ALOGV("start");
    if (mTrack != 0) {
        mTrack->setVolume(mLeftVolume, mRightVolume);
        status_t status = mTrack->start();
        if (status == NO_ERROR) {
            mTrack->getPosition(&mNumFramesWritten);
        }
        return status;
    }
    return NO_INIT;
}

void VideoEditorPlayer::VeAudioOutput::snoopWrite(
    const void* buffer, size_t size) {
    // Visualization buffers not supported
    return;

}

ssize_t VideoEditorPlayer::VeAudioOutput::write(
     const void* buffer, size_t size) {

    LOG_FATAL_IF(mCallback != NULL, "Don't call write if supplying a callback.");

    //ALOGV("write(%p, %u)", buffer, size);
    if (mTrack != 0) {
        snoopWrite(buffer, size);
        ssize_t ret = mTrack->write(buffer, size);
        mNumFramesWritten += ret / 4; // assume 16 bit stereo
        return ret;
    }
    return NO_INIT;
}

void VideoEditorPlayer::VeAudioOutput::stop() {

    ALOGV("stop");
    if (mTrack != 0) mTrack->stop();
}

void VideoEditorPlayer::VeAudioOutput::flush() {

    ALOGV("flush");
    if (mTrack != 0) mTrack->flush();
}

void VideoEditorPlayer::VeAudioOutput::pause() {

    ALOGV("VeAudioOutput::pause");
    if (mTrack != 0) mTrack->pause();
}

void VideoEditorPlayer::VeAudioOutput::close() {

    ALOGV("close");
    mTrack.clear();
}

void VideoEditorPlayer::VeAudioOutput::setVolume(float left, float right) {

    ALOGV("setVolume(%f, %f)", left, right);
    mLeftVolume = left;
    mRightVolume = right;
    if (mTrack != 0) {
        mTrack->setVolume(left, right);
    }
}

// static
void VideoEditorPlayer::VeAudioOutput::CallbackWrapper(
        int event, void *cookie, void *info) {
    //ALOGV("VeAudioOutput::callbackwrapper");
    if (event != AudioTrack::EVENT_MORE_DATA) {
        return;
    }

    VeAudioOutput *me = (VeAudioOutput *)cookie;
    AudioTrack::Buffer *buffer = (AudioTrack::Buffer *)info;

    size_t actualSize = (*me->mCallback)(
            me, buffer->raw, buffer->size, me->mCallbackCookie,
            MediaPlayerBase::AudioSink::CB_EVENT_FILL_BUFFER);

    buffer->size = actualSize;

    if (actualSize > 0) {
        me->snoopWrite(buffer->raw, actualSize);
    }
}

status_t VideoEditorPlayer::VeAudioOutput::dump(int fd, const Vector<String16>& args) const
{
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;

    result.append(" VeAudioOutput\n");
    snprintf(buffer, SIZE-1, "  stream type(%d), left - right volume(%f, %f)\n",
            mStreamType, mLeftVolume, mRightVolume);
    result.append(buffer);
    snprintf(buffer, SIZE-1, "  msec per frame(%f), latency (%d)\n",
            mMsecsPerFrame, mLatency);
    result.append(buffer);
    ::write(fd, result.string(), result.size());
    if (mTrack != 0) {
        mTrack->dump(fd, args);
    }
    return NO_ERROR;
}

int VideoEditorPlayer::VeAudioOutput::getSessionId() const {

    return mSessionId;
}

}  // namespace android

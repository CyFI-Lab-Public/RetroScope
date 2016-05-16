/*
 * Copyright (C) 2009 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "AudioPlayer"
#include <utils/Log.h>
#include <cutils/compiler.h>

#include <binder/IPCThreadState.h>
#include <media/AudioTrack.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/AudioPlayer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>

#include "include/AwesomePlayer.h"

namespace android {

AudioPlayer::AudioPlayer(
        const sp<MediaPlayerBase::AudioSink> &audioSink,
        uint32_t flags,
        AwesomePlayer *observer)
    : mInputBuffer(NULL),
      mSampleRate(0),
      mLatencyUs(0),
      mFrameSize(0),
      mNumFramesPlayed(0),
      mNumFramesPlayedSysTimeUs(ALooper::GetNowUs()),
      mPositionTimeMediaUs(-1),
      mPositionTimeRealUs(-1),
      mSeeking(false),
      mReachedEOS(false),
      mFinalStatus(OK),
      mSeekTimeUs(0),
      mStarted(false),
      mIsFirstBuffer(false),
      mFirstBufferResult(OK),
      mFirstBuffer(NULL),
      mAudioSink(audioSink),
      mObserver(observer),
      mPinnedTimeUs(-1ll),
      mPlaying(false),
      mStartPosUs(0),
      mCreateFlags(flags) {
}

AudioPlayer::~AudioPlayer() {
    if (mStarted) {
        reset();
    }
}

void AudioPlayer::setSource(const sp<MediaSource> &source) {
    CHECK(mSource == NULL);
    mSource = source;
}

status_t AudioPlayer::start(bool sourceAlreadyStarted) {
    CHECK(!mStarted);
    CHECK(mSource != NULL);

    status_t err;
    if (!sourceAlreadyStarted) {
        err = mSource->start();

        if (err != OK) {
            return err;
        }
    }

    // We allow an optional INFO_FORMAT_CHANGED at the very beginning
    // of playback, if there is one, getFormat below will retrieve the
    // updated format, if there isn't, we'll stash away the valid buffer
    // of data to be used on the first audio callback.

    CHECK(mFirstBuffer == NULL);

    MediaSource::ReadOptions options;
    if (mSeeking) {
        options.setSeekTo(mSeekTimeUs);
        mSeeking = false;
    }

    mFirstBufferResult = mSource->read(&mFirstBuffer, &options);
    if (mFirstBufferResult == INFO_FORMAT_CHANGED) {
        ALOGV("INFO_FORMAT_CHANGED!!!");

        CHECK(mFirstBuffer == NULL);
        mFirstBufferResult = OK;
        mIsFirstBuffer = false;
    } else {
        mIsFirstBuffer = true;
    }

    sp<MetaData> format = mSource->getFormat();
    const char *mime;
    bool success = format->findCString(kKeyMIMEType, &mime);
    CHECK(success);
    CHECK(useOffload() || !strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    success = format->findInt32(kKeySampleRate, &mSampleRate);
    CHECK(success);

    int32_t numChannels, channelMask;
    success = format->findInt32(kKeyChannelCount, &numChannels);
    CHECK(success);

    if(!format->findInt32(kKeyChannelMask, &channelMask)) {
        // log only when there's a risk of ambiguity of channel mask selection
        ALOGI_IF(numChannels > 2,
                "source format didn't specify channel mask, using (%d) channel order", numChannels);
        channelMask = CHANNEL_MASK_USE_CHANNEL_ORDER;
    }

    audio_format_t audioFormat = AUDIO_FORMAT_PCM_16_BIT;

    if (useOffload()) {
        if (mapMimeToAudioFormat(audioFormat, mime) != OK) {
            ALOGE("Couldn't map mime type \"%s\" to a valid AudioSystem::audio_format", mime);
            audioFormat = AUDIO_FORMAT_INVALID;
        } else {
            ALOGV("Mime type \"%s\" mapped to audio_format 0x%x", mime, audioFormat);
        }
    }

    int avgBitRate = -1;
    format->findInt32(kKeyBitRate, &avgBitRate);

    if (mAudioSink.get() != NULL) {

        uint32_t flags = AUDIO_OUTPUT_FLAG_NONE;
        audio_offload_info_t offloadInfo = AUDIO_INFO_INITIALIZER;

        if (allowDeepBuffering()) {
            flags |= AUDIO_OUTPUT_FLAG_DEEP_BUFFER;
        }
        if (useOffload()) {
            flags |= AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD;

            int64_t durationUs;
            if (format->findInt64(kKeyDuration, &durationUs)) {
                offloadInfo.duration_us = durationUs;
            } else {
                offloadInfo.duration_us = -1;
            }

            offloadInfo.sample_rate = mSampleRate;
            offloadInfo.channel_mask = channelMask;
            offloadInfo.format = audioFormat;
            offloadInfo.stream_type = AUDIO_STREAM_MUSIC;
            offloadInfo.bit_rate = avgBitRate;
            offloadInfo.has_video = ((mCreateFlags & HAS_VIDEO) != 0);
            offloadInfo.is_streaming = ((mCreateFlags & IS_STREAMING) != 0);
        }

        status_t err = mAudioSink->open(
                mSampleRate, numChannels, channelMask, audioFormat,
                DEFAULT_AUDIOSINK_BUFFERCOUNT,
                &AudioPlayer::AudioSinkCallback,
                this,
                (audio_output_flags_t)flags,
                useOffload() ? &offloadInfo : NULL);

        if (err == OK) {
            mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
            mFrameSize = mAudioSink->frameSize();

            if (useOffload()) {
                // If the playback is offloaded to h/w we pass the
                // HAL some metadata information
                // We don't want to do this for PCM because it will be going
                // through the AudioFlinger mixer before reaching the hardware
                sendMetaDataToHal(mAudioSink, format);
            }

            err = mAudioSink->start();
            // do not alter behavior for non offloaded tracks: ignore start status.
            if (!useOffload()) {
                err = OK;
            }
        }

        if (err != OK) {
            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

    } else {
        // playing to an AudioTrack, set up mask if necessary
        audio_channel_mask_t audioMask = channelMask == CHANNEL_MASK_USE_CHANNEL_ORDER ?
                audio_channel_out_mask_from_count(numChannels) : channelMask;
        if (0 == audioMask) {
            return BAD_VALUE;
        }

        mAudioTrack = new AudioTrack(
                AUDIO_STREAM_MUSIC, mSampleRate, AUDIO_FORMAT_PCM_16_BIT, audioMask,
                0, AUDIO_OUTPUT_FLAG_NONE, &AudioCallback, this, 0);

        if ((err = mAudioTrack->initCheck()) != OK) {
            mAudioTrack.clear();

            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

        mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
        mFrameSize = mAudioTrack->frameSize();

        mAudioTrack->start();
    }

    mStarted = true;
    mPlaying = true;
    mPinnedTimeUs = -1ll;

    return OK;
}

void AudioPlayer::pause(bool playPendingSamples) {
    CHECK(mStarted);

    if (playPendingSamples) {
        if (mAudioSink.get() != NULL) {
            mAudioSink->stop();
        } else {
            mAudioTrack->stop();
        }

        mNumFramesPlayed = 0;
        mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();
    } else {
        if (mAudioSink.get() != NULL) {
            mAudioSink->pause();
        } else {
            mAudioTrack->pause();
        }

        mPinnedTimeUs = ALooper::GetNowUs();
    }

    mPlaying = false;
}

status_t AudioPlayer::resume() {
    CHECK(mStarted);
    status_t err;

    if (mAudioSink.get() != NULL) {
        err = mAudioSink->start();
    } else {
        err = mAudioTrack->start();
    }

    if (err == OK) {
        mPlaying = true;
    }

    return err;
}

void AudioPlayer::reset() {
    CHECK(mStarted);

    ALOGV("reset: mPlaying=%d mReachedEOS=%d useOffload=%d",
                                mPlaying, mReachedEOS, useOffload() );

    if (mAudioSink.get() != NULL) {
        mAudioSink->stop();
        // If we're closing and have reached EOS, we don't want to flush
        // the track because if it is offloaded there could be a small
        // amount of residual data in the hardware buffer which we must
        // play to give gapless playback.
        // But if we're resetting when paused or before we've reached EOS
        // we can't be doing a gapless playback and there could be a large
        // amount of data queued in the hardware if the track is offloaded,
        // so we must flush to prevent a track switch being delayed playing
        // the buffered data that we don't want now
        if (!mPlaying || !mReachedEOS) {
            mAudioSink->flush();
        }

        mAudioSink->close();
    } else {
        mAudioTrack->stop();

        if (!mPlaying || !mReachedEOS) {
            mAudioTrack->flush();
        }

        mAudioTrack.clear();
    }

    // Make sure to release any buffer we hold onto so that the
    // source is able to stop().

    if (mFirstBuffer != NULL) {
        mFirstBuffer->release();
        mFirstBuffer = NULL;
    }

    if (mInputBuffer != NULL) {
        ALOGV("AudioPlayer releasing input buffer.");

        mInputBuffer->release();
        mInputBuffer = NULL;
    }

    mSource->stop();

    // The following hack is necessary to ensure that the OMX
    // component is completely released by the time we may try
    // to instantiate it again.
    // When offloading, the OMX component is not used so this hack
    // is not needed
    if (!useOffload()) {
        wp<MediaSource> tmp = mSource;
        mSource.clear();
        while (tmp.promote() != NULL) {
            usleep(1000);
        }
    } else {
        mSource.clear();
    }
    IPCThreadState::self()->flushCommands();

    mNumFramesPlayed = 0;
    mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();
    mPositionTimeMediaUs = -1;
    mPositionTimeRealUs = -1;
    mSeeking = false;
    mSeekTimeUs = 0;
    mReachedEOS = false;
    mFinalStatus = OK;
    mStarted = false;
    mPlaying = false;
    mStartPosUs = 0;
}

// static
void AudioPlayer::AudioCallback(int event, void *user, void *info) {
    static_cast<AudioPlayer *>(user)->AudioCallback(event, info);
}

bool AudioPlayer::isSeeking() {
    Mutex::Autolock autoLock(mLock);
    return mSeeking;
}

bool AudioPlayer::reachedEOS(status_t *finalStatus) {
    *finalStatus = OK;

    Mutex::Autolock autoLock(mLock);
    *finalStatus = mFinalStatus;
    return mReachedEOS;
}

void AudioPlayer::notifyAudioEOS() {
    ALOGV("AudioPlayer@0x%p notifyAudioEOS", this);

    if (mObserver != NULL) {
        mObserver->postAudioEOS(0);
        ALOGV("Notified observer of EOS!");
    }
}

status_t AudioPlayer::setPlaybackRatePermille(int32_t ratePermille) {
    if (mAudioSink.get() != NULL) {
        return mAudioSink->setPlaybackRatePermille(ratePermille);
    } else if (mAudioTrack != 0){
        return mAudioTrack->setSampleRate(ratePermille * mSampleRate / 1000);
    } else {
        return NO_INIT;
    }
}

// static
size_t AudioPlayer::AudioSinkCallback(
        MediaPlayerBase::AudioSink *audioSink,
        void *buffer, size_t size, void *cookie,
        MediaPlayerBase::AudioSink::cb_event_t event) {
    AudioPlayer *me = (AudioPlayer *)cookie;

    switch(event) {
    case MediaPlayerBase::AudioSink::CB_EVENT_FILL_BUFFER:
        return me->fillBuffer(buffer, size);

    case MediaPlayerBase::AudioSink::CB_EVENT_STREAM_END:
        ALOGV("AudioSinkCallback: stream end");
        me->mReachedEOS = true;
        me->notifyAudioEOS();
        break;

    case MediaPlayerBase::AudioSink::CB_EVENT_TEAR_DOWN:
        ALOGV("AudioSinkCallback: Tear down event");
        me->mObserver->postAudioTearDown();
        break;
    }

    return 0;
}

void AudioPlayer::AudioCallback(int event, void *info) {
    switch (event) {
    case AudioTrack::EVENT_MORE_DATA:
        {
        AudioTrack::Buffer *buffer = (AudioTrack::Buffer *)info;
        size_t numBytesWritten = fillBuffer(buffer->raw, buffer->size);
        buffer->size = numBytesWritten;
        }
        break;

    case AudioTrack::EVENT_STREAM_END:
        mReachedEOS = true;
        notifyAudioEOS();
        break;
    }
}

uint32_t AudioPlayer::getNumFramesPendingPlayout() const {
    uint32_t numFramesPlayedOut;
    status_t err;

    if (mAudioSink != NULL) {
        err = mAudioSink->getPosition(&numFramesPlayedOut);
    } else {
        err = mAudioTrack->getPosition(&numFramesPlayedOut);
    }

    if (err != OK || mNumFramesPlayed < numFramesPlayedOut) {
        return 0;
    }

    // mNumFramesPlayed is the number of frames submitted
    // to the audio sink for playback, but not all of them
    // may have played out by now.
    return mNumFramesPlayed - numFramesPlayedOut;
}

size_t AudioPlayer::fillBuffer(void *data, size_t size) {
    if (mNumFramesPlayed == 0) {
        ALOGV("AudioCallback");
    }

    if (mReachedEOS) {
        return 0;
    }

    bool postSeekComplete = false;
    bool postEOS = false;
    int64_t postEOSDelayUs = 0;

    size_t size_done = 0;
    size_t size_remaining = size;
    while (size_remaining > 0) {
        MediaSource::ReadOptions options;
        bool refreshSeekTime = false;

        {
            Mutex::Autolock autoLock(mLock);

            if (mSeeking) {
                if (mIsFirstBuffer) {
                    if (mFirstBuffer != NULL) {
                        mFirstBuffer->release();
                        mFirstBuffer = NULL;
                    }
                    mIsFirstBuffer = false;
                }

                options.setSeekTo(mSeekTimeUs);
                refreshSeekTime = true;

                if (mInputBuffer != NULL) {
                    mInputBuffer->release();
                    mInputBuffer = NULL;
                }

                mSeeking = false;
                if (mObserver) {
                    postSeekComplete = true;
                }
            }
        }

        if (mInputBuffer == NULL) {
            status_t err;

            if (mIsFirstBuffer) {
                mInputBuffer = mFirstBuffer;
                mFirstBuffer = NULL;
                err = mFirstBufferResult;

                mIsFirstBuffer = false;
            } else {
                err = mSource->read(&mInputBuffer, &options);
            }

            CHECK((err == OK && mInputBuffer != NULL)
                   || (err != OK && mInputBuffer == NULL));

            Mutex::Autolock autoLock(mLock);

            if (err != OK) {
                if (!mReachedEOS) {
                    if (useOffload()) {
                        // no more buffers to push - stop() and wait for STREAM_END
                        // don't set mReachedEOS until stream end received
                        if (mAudioSink != NULL) {
                            mAudioSink->stop();
                        } else {
                            mAudioTrack->stop();
                        }
                    } else {
                        if (mObserver) {
                            // We don't want to post EOS right away but only
                            // after all frames have actually been played out.

                            // These are the number of frames submitted to the
                            // AudioTrack that you haven't heard yet.
                            uint32_t numFramesPendingPlayout =
                                getNumFramesPendingPlayout();

                            // These are the number of frames we're going to
                            // submit to the AudioTrack by returning from this
                            // callback.
                            uint32_t numAdditionalFrames = size_done / mFrameSize;

                            numFramesPendingPlayout += numAdditionalFrames;

                            int64_t timeToCompletionUs =
                                (1000000ll * numFramesPendingPlayout) / mSampleRate;

                            ALOGV("total number of frames played: %lld (%lld us)",
                                    (mNumFramesPlayed + numAdditionalFrames),
                                    1000000ll * (mNumFramesPlayed + numAdditionalFrames)
                                        / mSampleRate);

                            ALOGV("%d frames left to play, %lld us (%.2f secs)",
                                 numFramesPendingPlayout,
                                 timeToCompletionUs, timeToCompletionUs / 1E6);

                            postEOS = true;
                            if (mAudioSink->needsTrailingPadding()) {
                                postEOSDelayUs = timeToCompletionUs + mLatencyUs;
                            } else {
                                postEOSDelayUs = 0;
                            }
                        }

                        mReachedEOS = true;
                    }
                }

                mFinalStatus = err;
                break;
            }

            if (mAudioSink != NULL) {
                mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
            } else {
                mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
            }

            if(mInputBuffer->range_length() != 0) {
                CHECK(mInputBuffer->meta_data()->findInt64(
                        kKeyTime, &mPositionTimeMediaUs));
            }

            // need to adjust the mStartPosUs for offload decoding since parser
            // might not be able to get the exact seek time requested.
            if (refreshSeekTime) {
                if (useOffload()) {
                    if (postSeekComplete) {
                        ALOGV("fillBuffer is going to post SEEK_COMPLETE");
                        mObserver->postAudioSeekComplete();
                        postSeekComplete = false;
                    }

                    mStartPosUs = mPositionTimeMediaUs;
                    ALOGV("adjust seek time to: %.2f", mStartPosUs/ 1E6);
                }
                // clear seek time with mLock locked and once we have valid mPositionTimeMediaUs
                // and mPositionTimeRealUs
                // before clearing mSeekTimeUs check if a new seek request has been received while
                // we were reading from the source with mLock released.
                if (!mSeeking) {
                    mSeekTimeUs = 0;
                }
            }

            if (!useOffload()) {
                mPositionTimeRealUs =
                    ((mNumFramesPlayed + size_done / mFrameSize) * 1000000)
                        / mSampleRate;
                ALOGV("buffer->size() = %d, "
                     "mPositionTimeMediaUs=%.2f mPositionTimeRealUs=%.2f",
                     mInputBuffer->range_length(),
                     mPositionTimeMediaUs / 1E6, mPositionTimeRealUs / 1E6);
            }

        }

        if (mInputBuffer->range_length() == 0) {
            mInputBuffer->release();
            mInputBuffer = NULL;

            continue;
        }

        size_t copy = size_remaining;
        if (copy > mInputBuffer->range_length()) {
            copy = mInputBuffer->range_length();
        }

        memcpy((char *)data + size_done,
               (const char *)mInputBuffer->data() + mInputBuffer->range_offset(),
               copy);

        mInputBuffer->set_range(mInputBuffer->range_offset() + copy,
                                mInputBuffer->range_length() - copy);

        size_done += copy;
        size_remaining -= copy;
    }

    if (useOffload()) {
        // We must ask the hardware what it has played
        mPositionTimeRealUs = getOutputPlayPositionUs_l();
        ALOGV("mPositionTimeMediaUs=%.2f mPositionTimeRealUs=%.2f",
             mPositionTimeMediaUs / 1E6, mPositionTimeRealUs / 1E6);
    }

    {
        Mutex::Autolock autoLock(mLock);
        mNumFramesPlayed += size_done / mFrameSize;
        mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();

        if (mReachedEOS) {
            mPinnedTimeUs = mNumFramesPlayedSysTimeUs;
        } else {
            mPinnedTimeUs = -1ll;
        }
    }

    if (postEOS) {
        mObserver->postAudioEOS(postEOSDelayUs);
    }

    if (postSeekComplete) {
        mObserver->postAudioSeekComplete();
    }

    return size_done;
}

int64_t AudioPlayer::getRealTimeUs() {
    Mutex::Autolock autoLock(mLock);
    if (useOffload()) {
        if (mSeeking) {
            return mSeekTimeUs;
        }
        mPositionTimeRealUs = getOutputPlayPositionUs_l();
        return mPositionTimeRealUs;
    }

    return getRealTimeUsLocked();
}

int64_t AudioPlayer::getRealTimeUsLocked() const {
    CHECK(mStarted);
    CHECK_NE(mSampleRate, 0);
    int64_t result = -mLatencyUs + (mNumFramesPlayed * 1000000) / mSampleRate;

    // Compensate for large audio buffers, updates of mNumFramesPlayed
    // are less frequent, therefore to get a "smoother" notion of time we
    // compensate using system time.
    int64_t diffUs;
    if (mPinnedTimeUs >= 0ll) {
        diffUs = mPinnedTimeUs;
    } else {
        diffUs = ALooper::GetNowUs();
    }

    diffUs -= mNumFramesPlayedSysTimeUs;

    return result + diffUs;
}

int64_t AudioPlayer::getOutputPlayPositionUs_l() const
{
    uint32_t playedSamples = 0;
    if (mAudioSink != NULL) {
        mAudioSink->getPosition(&playedSamples);
    } else {
        mAudioTrack->getPosition(&playedSamples);
    }

    const int64_t playedUs = (static_cast<int64_t>(playedSamples) * 1000000 ) / mSampleRate;

    // HAL position is relative to the first buffer we sent at mStartPosUs
    const int64_t renderedDuration = mStartPosUs + playedUs;
    ALOGV("getOutputPlayPositionUs_l %lld", renderedDuration);
    return renderedDuration;
}

int64_t AudioPlayer::getMediaTimeUs() {
    Mutex::Autolock autoLock(mLock);

    if (useOffload()) {
        if (mSeeking) {
            return mSeekTimeUs;
        }
        mPositionTimeRealUs = getOutputPlayPositionUs_l();
        ALOGV("getMediaTimeUs getOutputPlayPositionUs_l() mPositionTimeRealUs %lld",
              mPositionTimeRealUs);
        return mPositionTimeRealUs;
    }


    if (mPositionTimeMediaUs < 0 || mPositionTimeRealUs < 0) {
        // mSeekTimeUs is either seek time while seeking or 0 if playback did not start.
        return mSeekTimeUs;
    }

    int64_t realTimeOffset = getRealTimeUsLocked() - mPositionTimeRealUs;
    if (realTimeOffset < 0) {
        realTimeOffset = 0;
    }

    return mPositionTimeMediaUs + realTimeOffset;
}

bool AudioPlayer::getMediaTimeMapping(
        int64_t *realtime_us, int64_t *mediatime_us) {
    Mutex::Autolock autoLock(mLock);

    if (useOffload()) {
        mPositionTimeRealUs = getOutputPlayPositionUs_l();
        *realtime_us = mPositionTimeRealUs;
        *mediatime_us = mPositionTimeRealUs;
    } else {
        *realtime_us = mPositionTimeRealUs;
        *mediatime_us = mPositionTimeMediaUs;
    }

    return mPositionTimeRealUs != -1 && mPositionTimeMediaUs != -1;
}

status_t AudioPlayer::seekTo(int64_t time_us) {
    Mutex::Autolock autoLock(mLock);

    ALOGV("seekTo( %lld )", time_us);

    mSeeking = true;
    mPositionTimeRealUs = mPositionTimeMediaUs = -1;
    mReachedEOS = false;
    mSeekTimeUs = time_us;
    mStartPosUs = time_us;

    // Flush resets the number of played frames
    mNumFramesPlayed = 0;
    mNumFramesPlayedSysTimeUs = ALooper::GetNowUs();

    if (mAudioSink != NULL) {
        if (mPlaying) {
            mAudioSink->pause();
        }
        mAudioSink->flush();
        if (mPlaying) {
            mAudioSink->start();
        }
    } else {
        if (mPlaying) {
            mAudioTrack->pause();
        }
        mAudioTrack->flush();
        if (mPlaying) {
            mAudioTrack->start();
        }
    }

    return OK;
}

}

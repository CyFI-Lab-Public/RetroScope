/*
 * Copyright (C) 2010 The Android Open Source Project
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
#define LOG_TAG "NuPlayer"
#include <utils/Log.h>

#include "NuPlayer.h"

#include "HTTPLiveSource.h"
#include "NuPlayerDecoder.h"
#include "NuPlayerDriver.h"
#include "NuPlayerRenderer.h"
#include "NuPlayerSource.h"
#include "RTSPSource.h"
#include "StreamingSource.h"
#include "GenericSource.h"
#include "mp4/MP4Source.h"

#include "ATSParser.h"

#include <cutils/properties.h> // for property_get
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/ACodec.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MetaData.h>
#include <gui/IGraphicBufferProducer.h>

#include "avc_utils.h"

#include "ESDS.h"
#include <media/stagefright/Utils.h>

namespace android {

struct NuPlayer::Action : public RefBase {
    Action() {}

    virtual void execute(NuPlayer *player) = 0;

private:
    DISALLOW_EVIL_CONSTRUCTORS(Action);
};

struct NuPlayer::SeekAction : public Action {
    SeekAction(int64_t seekTimeUs)
        : mSeekTimeUs(seekTimeUs) {
    }

    virtual void execute(NuPlayer *player) {
        player->performSeek(mSeekTimeUs);
    }

private:
    int64_t mSeekTimeUs;

    DISALLOW_EVIL_CONSTRUCTORS(SeekAction);
};

struct NuPlayer::SetSurfaceAction : public Action {
    SetSurfaceAction(const sp<NativeWindowWrapper> &wrapper)
        : mWrapper(wrapper) {
    }

    virtual void execute(NuPlayer *player) {
        player->performSetSurface(mWrapper);
    }

private:
    sp<NativeWindowWrapper> mWrapper;

    DISALLOW_EVIL_CONSTRUCTORS(SetSurfaceAction);
};

struct NuPlayer::ShutdownDecoderAction : public Action {
    ShutdownDecoderAction(bool audio, bool video)
        : mAudio(audio),
          mVideo(video) {
    }

    virtual void execute(NuPlayer *player) {
        player->performDecoderShutdown(mAudio, mVideo);
    }

private:
    bool mAudio;
    bool mVideo;

    DISALLOW_EVIL_CONSTRUCTORS(ShutdownDecoderAction);
};

struct NuPlayer::PostMessageAction : public Action {
    PostMessageAction(const sp<AMessage> &msg)
        : mMessage(msg) {
    }

    virtual void execute(NuPlayer *) {
        mMessage->post();
    }

private:
    sp<AMessage> mMessage;

    DISALLOW_EVIL_CONSTRUCTORS(PostMessageAction);
};

// Use this if there's no state necessary to save in order to execute
// the action.
struct NuPlayer::SimpleAction : public Action {
    typedef void (NuPlayer::*ActionFunc)();

    SimpleAction(ActionFunc func)
        : mFunc(func) {
    }

    virtual void execute(NuPlayer *player) {
        (player->*mFunc)();
    }

private:
    ActionFunc mFunc;

    DISALLOW_EVIL_CONSTRUCTORS(SimpleAction);
};

////////////////////////////////////////////////////////////////////////////////

NuPlayer::NuPlayer()
    : mUIDValid(false),
      mSourceFlags(0),
      mVideoIsAVC(false),
      mAudioEOS(false),
      mVideoEOS(false),
      mScanSourcesPending(false),
      mScanSourcesGeneration(0),
      mPollDurationGeneration(0),
      mTimeDiscontinuityPending(false),
      mFlushingAudio(NONE),
      mFlushingVideo(NONE),
      mSkipRenderingAudioUntilMediaTimeUs(-1ll),
      mSkipRenderingVideoUntilMediaTimeUs(-1ll),
      mVideoLateByUs(0ll),
      mNumFramesTotal(0ll),
      mNumFramesDropped(0ll),
      mVideoScalingMode(NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW),
      mStarted(false) {
}

NuPlayer::~NuPlayer() {
}

void NuPlayer::setUID(uid_t uid) {
    mUIDValid = true;
    mUID = uid;
}

void NuPlayer::setDriver(const wp<NuPlayerDriver> &driver) {
    mDriver = driver;
}

void NuPlayer::setDataSourceAsync(const sp<IStreamSource> &source) {
    sp<AMessage> msg = new AMessage(kWhatSetDataSource, id());

    sp<AMessage> notify = new AMessage(kWhatSourceNotify, id());

    char prop[PROPERTY_VALUE_MAX];
    if (property_get("media.stagefright.use-mp4source", prop, NULL)
            && (!strcmp(prop, "1") || !strcasecmp(prop, "true"))) {
        msg->setObject("source", new MP4Source(notify, source));
    } else {
        msg->setObject("source", new StreamingSource(notify, source));
    }

    msg->post();
}

static bool IsHTTPLiveURL(const char *url) {
    if (!strncasecmp("http://", url, 7)
            || !strncasecmp("https://", url, 8)
            || !strncasecmp("file://", url, 7)) {
        size_t len = strlen(url);
        if (len >= 5 && !strcasecmp(".m3u8", &url[len - 5])) {
            return true;
        }

        if (strstr(url,"m3u8")) {
            return true;
        }
    }

    return false;
}

void NuPlayer::setDataSourceAsync(
        const char *url, const KeyedVector<String8, String8> *headers) {
    sp<AMessage> msg = new AMessage(kWhatSetDataSource, id());
    size_t len = strlen(url);

    sp<AMessage> notify = new AMessage(kWhatSourceNotify, id());

    sp<Source> source;
    if (IsHTTPLiveURL(url)) {
        source = new HTTPLiveSource(notify, url, headers, mUIDValid, mUID);
    } else if (!strncasecmp(url, "rtsp://", 7)) {
        source = new RTSPSource(notify, url, headers, mUIDValid, mUID);
    } else if ((!strncasecmp(url, "http://", 7)
                || !strncasecmp(url, "https://", 8))
                    && ((len >= 4 && !strcasecmp(".sdp", &url[len - 4]))
                    || strstr(url, ".sdp?"))) {
        source = new RTSPSource(notify, url, headers, mUIDValid, mUID, true);
    } else {
        source = new GenericSource(notify, url, headers, mUIDValid, mUID);
    }

    msg->setObject("source", source);
    msg->post();
}

void NuPlayer::setDataSourceAsync(int fd, int64_t offset, int64_t length) {
    sp<AMessage> msg = new AMessage(kWhatSetDataSource, id());

    sp<AMessage> notify = new AMessage(kWhatSourceNotify, id());

    sp<Source> source = new GenericSource(notify, fd, offset, length);
    msg->setObject("source", source);
    msg->post();
}

void NuPlayer::prepareAsync() {
    (new AMessage(kWhatPrepare, id()))->post();
}

void NuPlayer::setVideoSurfaceTextureAsync(
        const sp<IGraphicBufferProducer> &bufferProducer) {
    sp<AMessage> msg = new AMessage(kWhatSetVideoNativeWindow, id());

    if (bufferProducer == NULL) {
        msg->setObject("native-window", NULL);
    } else {
        msg->setObject(
                "native-window",
                new NativeWindowWrapper(
                    new Surface(bufferProducer)));
    }

    msg->post();
}

void NuPlayer::setAudioSink(const sp<MediaPlayerBase::AudioSink> &sink) {
    sp<AMessage> msg = new AMessage(kWhatSetAudioSink, id());
    msg->setObject("sink", sink);
    msg->post();
}

void NuPlayer::start() {
    (new AMessage(kWhatStart, id()))->post();
}

void NuPlayer::pause() {
    (new AMessage(kWhatPause, id()))->post();
}

void NuPlayer::resume() {
    (new AMessage(kWhatResume, id()))->post();
}

void NuPlayer::resetAsync() {
    (new AMessage(kWhatReset, id()))->post();
}

void NuPlayer::seekToAsync(int64_t seekTimeUs) {
    sp<AMessage> msg = new AMessage(kWhatSeek, id());
    msg->setInt64("seekTimeUs", seekTimeUs);
    msg->post();
}

// static
bool NuPlayer::IsFlushingState(FlushStatus state, bool *needShutdown) {
    switch (state) {
        case FLUSHING_DECODER:
            if (needShutdown != NULL) {
                *needShutdown = false;
            }
            return true;

        case FLUSHING_DECODER_SHUTDOWN:
            if (needShutdown != NULL) {
                *needShutdown = true;
            }
            return true;

        default:
            return false;
    }
}

void NuPlayer::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatSetDataSource:
        {
            ALOGV("kWhatSetDataSource");

            CHECK(mSource == NULL);

            sp<RefBase> obj;
            CHECK(msg->findObject("source", &obj));

            mSource = static_cast<Source *>(obj.get());

            looper()->registerHandler(mSource);

            CHECK(mDriver != NULL);
            sp<NuPlayerDriver> driver = mDriver.promote();
            if (driver != NULL) {
                driver->notifySetDataSourceCompleted(OK);
            }
            break;
        }

        case kWhatPrepare:
        {
            mSource->prepareAsync();
            break;
        }

        case kWhatGetTrackInfo:
        {
            uint32_t replyID;
            CHECK(msg->senderAwaitsResponse(&replyID));

            status_t err = INVALID_OPERATION;
            if (mSource != NULL) {
                Parcel* reply;
                CHECK(msg->findPointer("reply", (void**)&reply));
                err = mSource->getTrackInfo(reply);
            }

            sp<AMessage> response = new AMessage;
            response->setInt32("err", err);

            response->postReply(replyID);
            break;
        }

        case kWhatSelectTrack:
        {
            uint32_t replyID;
            CHECK(msg->senderAwaitsResponse(&replyID));

            status_t err = INVALID_OPERATION;
            if (mSource != NULL) {
                size_t trackIndex;
                int32_t select;
                CHECK(msg->findSize("trackIndex", &trackIndex));
                CHECK(msg->findInt32("select", &select));
                err = mSource->selectTrack(trackIndex, select);
            }

            sp<AMessage> response = new AMessage;
            response->setInt32("err", err);

            response->postReply(replyID);
            break;
        }

        case kWhatPollDuration:
        {
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));

            if (generation != mPollDurationGeneration) {
                // stale
                break;
            }

            int64_t durationUs;
            if (mDriver != NULL && mSource->getDuration(&durationUs) == OK) {
                sp<NuPlayerDriver> driver = mDriver.promote();
                if (driver != NULL) {
                    driver->notifyDuration(durationUs);
                }
            }

            msg->post(1000000ll);  // poll again in a second.
            break;
        }

        case kWhatSetVideoNativeWindow:
        {
            ALOGV("kWhatSetVideoNativeWindow");

            mDeferredActions.push_back(
                    new ShutdownDecoderAction(
                        false /* audio */, true /* video */));

            sp<RefBase> obj;
            CHECK(msg->findObject("native-window", &obj));

            mDeferredActions.push_back(
                    new SetSurfaceAction(
                        static_cast<NativeWindowWrapper *>(obj.get())));

            if (obj != NULL) {
                // If there is a new surface texture, instantiate decoders
                // again if possible.
                mDeferredActions.push_back(
                        new SimpleAction(&NuPlayer::performScanSources));
            }

            processDeferredActions();
            break;
        }

        case kWhatSetAudioSink:
        {
            ALOGV("kWhatSetAudioSink");

            sp<RefBase> obj;
            CHECK(msg->findObject("sink", &obj));

            mAudioSink = static_cast<MediaPlayerBase::AudioSink *>(obj.get());
            break;
        }

        case kWhatStart:
        {
            ALOGV("kWhatStart");

            mVideoIsAVC = false;
            mAudioEOS = false;
            mVideoEOS = false;
            mSkipRenderingAudioUntilMediaTimeUs = -1;
            mSkipRenderingVideoUntilMediaTimeUs = -1;
            mVideoLateByUs = 0;
            mNumFramesTotal = 0;
            mNumFramesDropped = 0;
            mStarted = true;

            mSource->start();

            uint32_t flags = 0;

            if (mSource->isRealTime()) {
                flags |= Renderer::FLAG_REAL_TIME;
            }

            mRenderer = new Renderer(
                    mAudioSink,
                    new AMessage(kWhatRendererNotify, id()),
                    flags);

            looper()->registerHandler(mRenderer);

            postScanSources();
            break;
        }

        case kWhatScanSources:
        {
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));
            if (generation != mScanSourcesGeneration) {
                // Drop obsolete msg.
                break;
            }

            mScanSourcesPending = false;

            ALOGV("scanning sources haveAudio=%d, haveVideo=%d",
                 mAudioDecoder != NULL, mVideoDecoder != NULL);

            bool mHadAnySourcesBefore =
                (mAudioDecoder != NULL) || (mVideoDecoder != NULL);

            if (mNativeWindow != NULL) {
                instantiateDecoder(false, &mVideoDecoder);
            }

            if (mAudioSink != NULL) {
                instantiateDecoder(true, &mAudioDecoder);
            }

            if (!mHadAnySourcesBefore
                    && (mAudioDecoder != NULL || mVideoDecoder != NULL)) {
                // This is the first time we've found anything playable.

                if (mSourceFlags & Source::FLAG_DYNAMIC_DURATION) {
                    schedulePollDuration();
                }
            }

            status_t err;
            if ((err = mSource->feedMoreTSData()) != OK) {
                if (mAudioDecoder == NULL && mVideoDecoder == NULL) {
                    // We're not currently decoding anything (no audio or
                    // video tracks found) and we just ran out of input data.

                    if (err == ERROR_END_OF_STREAM) {
                        notifyListener(MEDIA_PLAYBACK_COMPLETE, 0, 0);
                    } else {
                        notifyListener(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, err);
                    }
                }
                break;
            }

            if ((mAudioDecoder == NULL && mAudioSink != NULL)
                    || (mVideoDecoder == NULL && mNativeWindow != NULL)) {
                msg->post(100000ll);
                mScanSourcesPending = true;
            }
            break;
        }

        case kWhatVideoNotify:
        case kWhatAudioNotify:
        {
            bool audio = msg->what() == kWhatAudioNotify;

            sp<AMessage> codecRequest;
            CHECK(msg->findMessage("codec-request", &codecRequest));

            int32_t what;
            CHECK(codecRequest->findInt32("what", &what));

            if (what == ACodec::kWhatFillThisBuffer) {
                status_t err = feedDecoderInputData(
                        audio, codecRequest);

                if (err == -EWOULDBLOCK) {
                    if (mSource->feedMoreTSData() == OK) {
                        msg->post(10000ll);
                    }
                }
            } else if (what == ACodec::kWhatEOS) {
                int32_t err;
                CHECK(codecRequest->findInt32("err", &err));

                if (err == ERROR_END_OF_STREAM) {
                    ALOGV("got %s decoder EOS", audio ? "audio" : "video");
                } else {
                    ALOGV("got %s decoder EOS w/ error %d",
                         audio ? "audio" : "video",
                         err);
                }

                mRenderer->queueEOS(audio, err);
            } else if (what == ACodec::kWhatFlushCompleted) {
                bool needShutdown;

                if (audio) {
                    CHECK(IsFlushingState(mFlushingAudio, &needShutdown));
                    mFlushingAudio = FLUSHED;
                } else {
                    CHECK(IsFlushingState(mFlushingVideo, &needShutdown));
                    mFlushingVideo = FLUSHED;

                    mVideoLateByUs = 0;
                }

                ALOGV("decoder %s flush completed", audio ? "audio" : "video");

                if (needShutdown) {
                    ALOGV("initiating %s decoder shutdown",
                         audio ? "audio" : "video");

                    (audio ? mAudioDecoder : mVideoDecoder)->initiateShutdown();

                    if (audio) {
                        mFlushingAudio = SHUTTING_DOWN_DECODER;
                    } else {
                        mFlushingVideo = SHUTTING_DOWN_DECODER;
                    }
                }

                finishFlushIfPossible();
            } else if (what == ACodec::kWhatOutputFormatChanged) {
                if (audio) {
                    int32_t numChannels;
                    CHECK(codecRequest->findInt32(
                                "channel-count", &numChannels));

                    int32_t sampleRate;
                    CHECK(codecRequest->findInt32("sample-rate", &sampleRate));

                    ALOGV("Audio output format changed to %d Hz, %d channels",
                         sampleRate, numChannels);

                    mAudioSink->close();

                    audio_output_flags_t flags;
                    int64_t durationUs;
                    // FIXME: we should handle the case where the video decoder
                    // is created after we receive the format change indication.
                    // Current code will just make that we select deep buffer
                    // with video which should not be a problem as it should
                    // not prevent from keeping A/V sync.
                    if (mVideoDecoder == NULL &&
                            mSource->getDuration(&durationUs) == OK &&
                            durationUs
                                > AUDIO_SINK_MIN_DEEP_BUFFER_DURATION_US) {
                        flags = AUDIO_OUTPUT_FLAG_DEEP_BUFFER;
                    } else {
                        flags = AUDIO_OUTPUT_FLAG_NONE;
                    }

                    int32_t channelMask;
                    if (!codecRequest->findInt32("channel-mask", &channelMask)) {
                        channelMask = CHANNEL_MASK_USE_CHANNEL_ORDER;
                    }

                    CHECK_EQ(mAudioSink->open(
                                sampleRate,
                                numChannels,
                                (audio_channel_mask_t)channelMask,
                                AUDIO_FORMAT_PCM_16_BIT,
                                8 /* bufferCount */,
                                NULL,
                                NULL,
                                flags),
                             (status_t)OK);
                    mAudioSink->start();

                    mRenderer->signalAudioSinkChanged();
                } else {
                    // video

                    int32_t width, height;
                    CHECK(codecRequest->findInt32("width", &width));
                    CHECK(codecRequest->findInt32("height", &height));

                    int32_t cropLeft, cropTop, cropRight, cropBottom;
                    CHECK(codecRequest->findRect(
                                "crop",
                                &cropLeft, &cropTop, &cropRight, &cropBottom));

                    int32_t displayWidth = cropRight - cropLeft + 1;
                    int32_t displayHeight = cropBottom - cropTop + 1;

                    ALOGV("Video output format changed to %d x %d "
                         "(crop: %d x %d @ (%d, %d))",
                         width, height,
                         displayWidth,
                         displayHeight,
                         cropLeft, cropTop);

                    sp<AMessage> videoInputFormat =
                        mSource->getFormat(false /* audio */);

                    // Take into account sample aspect ratio if necessary:
                    int32_t sarWidth, sarHeight;
                    if (videoInputFormat->findInt32("sar-width", &sarWidth)
                            && videoInputFormat->findInt32(
                                "sar-height", &sarHeight)) {
                        ALOGV("Sample aspect ratio %d : %d",
                              sarWidth, sarHeight);

                        displayWidth = (displayWidth * sarWidth) / sarHeight;

                        ALOGV("display dimensions %d x %d",
                              displayWidth, displayHeight);
                    }

                    notifyListener(
                            MEDIA_SET_VIDEO_SIZE, displayWidth, displayHeight);
                }
            } else if (what == ACodec::kWhatShutdownCompleted) {
                ALOGV("%s shutdown completed", audio ? "audio" : "video");
                if (audio) {
                    mAudioDecoder.clear();

                    CHECK_EQ((int)mFlushingAudio, (int)SHUTTING_DOWN_DECODER);
                    mFlushingAudio = SHUT_DOWN;
                } else {
                    mVideoDecoder.clear();

                    CHECK_EQ((int)mFlushingVideo, (int)SHUTTING_DOWN_DECODER);
                    mFlushingVideo = SHUT_DOWN;
                }

                finishFlushIfPossible();
            } else if (what == ACodec::kWhatError) {
                ALOGE("Received error from %s decoder, aborting playback.",
                     audio ? "audio" : "video");

                mRenderer->queueEOS(audio, UNKNOWN_ERROR);
            } else if (what == ACodec::kWhatDrainThisBuffer) {
                renderBuffer(audio, codecRequest);
            } else if (what != ACodec::kWhatComponentAllocated
                    && what != ACodec::kWhatComponentConfigured
                    && what != ACodec::kWhatBuffersAllocated) {
                ALOGV("Unhandled codec notification %d '%c%c%c%c'.",
                      what,
                      what >> 24,
                      (what >> 16) & 0xff,
                      (what >> 8) & 0xff,
                      what & 0xff);
            }

            break;
        }

        case kWhatRendererNotify:
        {
            int32_t what;
            CHECK(msg->findInt32("what", &what));

            if (what == Renderer::kWhatEOS) {
                int32_t audio;
                CHECK(msg->findInt32("audio", &audio));

                int32_t finalResult;
                CHECK(msg->findInt32("finalResult", &finalResult));

                if (audio) {
                    mAudioEOS = true;
                } else {
                    mVideoEOS = true;
                }

                if (finalResult == ERROR_END_OF_STREAM) {
                    ALOGV("reached %s EOS", audio ? "audio" : "video");
                } else {
                    ALOGE("%s track encountered an error (%d)",
                         audio ? "audio" : "video", finalResult);

                    notifyListener(
                            MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, finalResult);
                }

                if ((mAudioEOS || mAudioDecoder == NULL)
                        && (mVideoEOS || mVideoDecoder == NULL)) {
                    notifyListener(MEDIA_PLAYBACK_COMPLETE, 0, 0);
                }
            } else if (what == Renderer::kWhatPosition) {
                int64_t positionUs;
                CHECK(msg->findInt64("positionUs", &positionUs));

                CHECK(msg->findInt64("videoLateByUs", &mVideoLateByUs));

                if (mDriver != NULL) {
                    sp<NuPlayerDriver> driver = mDriver.promote();
                    if (driver != NULL) {
                        driver->notifyPosition(positionUs);

                        driver->notifyFrameStats(
                                mNumFramesTotal, mNumFramesDropped);
                    }
                }
            } else if (what == Renderer::kWhatFlushComplete) {
                int32_t audio;
                CHECK(msg->findInt32("audio", &audio));

                ALOGV("renderer %s flush completed.", audio ? "audio" : "video");
            } else if (what == Renderer::kWhatVideoRenderingStart) {
                notifyListener(MEDIA_INFO, MEDIA_INFO_RENDERING_START, 0);
            } else if (what == Renderer::kWhatMediaRenderingStart) {
                ALOGV("media rendering started");
                notifyListener(MEDIA_STARTED, 0, 0);
            }
            break;
        }

        case kWhatMoreDataQueued:
        {
            break;
        }

        case kWhatReset:
        {
            ALOGV("kWhatReset");

            mDeferredActions.push_back(
                    new ShutdownDecoderAction(
                        true /* audio */, true /* video */));

            mDeferredActions.push_back(
                    new SimpleAction(&NuPlayer::performReset));

            processDeferredActions();
            break;
        }

        case kWhatSeek:
        {
            int64_t seekTimeUs;
            CHECK(msg->findInt64("seekTimeUs", &seekTimeUs));

            ALOGV("kWhatSeek seekTimeUs=%lld us", seekTimeUs);

            mDeferredActions.push_back(
                    new SimpleAction(&NuPlayer::performDecoderFlush));

            mDeferredActions.push_back(new SeekAction(seekTimeUs));

            processDeferredActions();
            break;
        }

        case kWhatPause:
        {
            CHECK(mRenderer != NULL);
            mSource->pause();
            mRenderer->pause();
            break;
        }

        case kWhatResume:
        {
            CHECK(mRenderer != NULL);
            mSource->resume();
            mRenderer->resume();
            break;
        }

        case kWhatSourceNotify:
        {
            onSourceNotify(msg);
            break;
        }

        default:
            TRESPASS();
            break;
    }
}

void NuPlayer::finishFlushIfPossible() {
    if (mFlushingAudio != FLUSHED && mFlushingAudio != SHUT_DOWN) {
        return;
    }

    if (mFlushingVideo != FLUSHED && mFlushingVideo != SHUT_DOWN) {
        return;
    }

    ALOGV("both audio and video are flushed now.");

    if (mTimeDiscontinuityPending) {
        mRenderer->signalTimeDiscontinuity();
        mTimeDiscontinuityPending = false;
    }

    if (mAudioDecoder != NULL) {
        mAudioDecoder->signalResume();
    }

    if (mVideoDecoder != NULL) {
        mVideoDecoder->signalResume();
    }

    mFlushingAudio = NONE;
    mFlushingVideo = NONE;

    processDeferredActions();
}

void NuPlayer::postScanSources() {
    if (mScanSourcesPending) {
        return;
    }

    sp<AMessage> msg = new AMessage(kWhatScanSources, id());
    msg->setInt32("generation", mScanSourcesGeneration);
    msg->post();

    mScanSourcesPending = true;
}

status_t NuPlayer::instantiateDecoder(bool audio, sp<Decoder> *decoder) {
    if (*decoder != NULL) {
        return OK;
    }

    sp<AMessage> format = mSource->getFormat(audio);

    if (format == NULL) {
        return -EWOULDBLOCK;
    }

    if (!audio) {
        AString mime;
        CHECK(format->findString("mime", &mime));
        mVideoIsAVC = !strcasecmp(MEDIA_MIMETYPE_VIDEO_AVC, mime.c_str());
    }

    sp<AMessage> notify =
        new AMessage(audio ? kWhatAudioNotify : kWhatVideoNotify,
                     id());

    *decoder = audio ? new Decoder(notify) :
                       new Decoder(notify, mNativeWindow);
    looper()->registerHandler(*decoder);

    (*decoder)->configure(format);

    return OK;
}

status_t NuPlayer::feedDecoderInputData(bool audio, const sp<AMessage> &msg) {
    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));

    if ((audio && IsFlushingState(mFlushingAudio))
            || (!audio && IsFlushingState(mFlushingVideo))) {
        reply->setInt32("err", INFO_DISCONTINUITY);
        reply->post();
        return OK;
    }

    sp<ABuffer> accessUnit;

    bool dropAccessUnit;
    do {
        status_t err = mSource->dequeueAccessUnit(audio, &accessUnit);

        if (err == -EWOULDBLOCK) {
            return err;
        } else if (err != OK) {
            if (err == INFO_DISCONTINUITY) {
                int32_t type;
                CHECK(accessUnit->meta()->findInt32("discontinuity", &type));

                bool formatChange =
                    (audio &&
                     (type & ATSParser::DISCONTINUITY_AUDIO_FORMAT))
                    || (!audio &&
                            (type & ATSParser::DISCONTINUITY_VIDEO_FORMAT));

                bool timeChange = (type & ATSParser::DISCONTINUITY_TIME) != 0;

                ALOGI("%s discontinuity (formatChange=%d, time=%d)",
                     audio ? "audio" : "video", formatChange, timeChange);

                if (audio) {
                    mSkipRenderingAudioUntilMediaTimeUs = -1;
                } else {
                    mSkipRenderingVideoUntilMediaTimeUs = -1;
                }

                if (timeChange) {
                    sp<AMessage> extra;
                    if (accessUnit->meta()->findMessage("extra", &extra)
                            && extra != NULL) {
                        int64_t resumeAtMediaTimeUs;
                        if (extra->findInt64(
                                    "resume-at-mediatimeUs", &resumeAtMediaTimeUs)) {
                            ALOGI("suppressing rendering of %s until %lld us",
                                    audio ? "audio" : "video", resumeAtMediaTimeUs);

                            if (audio) {
                                mSkipRenderingAudioUntilMediaTimeUs =
                                    resumeAtMediaTimeUs;
                            } else {
                                mSkipRenderingVideoUntilMediaTimeUs =
                                    resumeAtMediaTimeUs;
                            }
                        }
                    }
                }

                mTimeDiscontinuityPending =
                    mTimeDiscontinuityPending || timeChange;

                if (formatChange || timeChange) {
                    if (mFlushingAudio == NONE && mFlushingVideo == NONE) {
                        // And we'll resume scanning sources once we're done
                        // flushing.
                        mDeferredActions.push_front(
                                new SimpleAction(
                                    &NuPlayer::performScanSources));
                    }

                    flushDecoder(audio, formatChange);
                } else {
                    // This stream is unaffected by the discontinuity

                    if (audio) {
                        mFlushingAudio = FLUSHED;
                    } else {
                        mFlushingVideo = FLUSHED;
                    }

                    finishFlushIfPossible();

                    return -EWOULDBLOCK;
                }
            }

            reply->setInt32("err", err);
            reply->post();
            return OK;
        }

        if (!audio) {
            ++mNumFramesTotal;
        }

        dropAccessUnit = false;
        if (!audio
                && mVideoLateByUs > 100000ll
                && mVideoIsAVC
                && !IsAVCReferenceFrame(accessUnit)) {
            dropAccessUnit = true;
            ++mNumFramesDropped;
        }
    } while (dropAccessUnit);

    // ALOGV("returned a valid buffer of %s data", audio ? "audio" : "video");

#if 0
    int64_t mediaTimeUs;
    CHECK(accessUnit->meta()->findInt64("timeUs", &mediaTimeUs));
    ALOGV("feeding %s input buffer at media time %.2f secs",
         audio ? "audio" : "video",
         mediaTimeUs / 1E6);
#endif

    reply->setBuffer("buffer", accessUnit);
    reply->post();

    return OK;
}

void NuPlayer::renderBuffer(bool audio, const sp<AMessage> &msg) {
    // ALOGV("renderBuffer %s", audio ? "audio" : "video");

    sp<AMessage> reply;
    CHECK(msg->findMessage("reply", &reply));

    if (IsFlushingState(audio ? mFlushingAudio : mFlushingVideo)) {
        // We're currently attempting to flush the decoder, in order
        // to complete this, the decoder wants all its buffers back,
        // so we don't want any output buffers it sent us (from before
        // we initiated the flush) to be stuck in the renderer's queue.

        ALOGV("we're still flushing the %s decoder, sending its output buffer"
             " right back.", audio ? "audio" : "video");

        reply->post();
        return;
    }

    sp<ABuffer> buffer;
    CHECK(msg->findBuffer("buffer", &buffer));

    int64_t &skipUntilMediaTimeUs =
        audio
            ? mSkipRenderingAudioUntilMediaTimeUs
            : mSkipRenderingVideoUntilMediaTimeUs;

    if (skipUntilMediaTimeUs >= 0) {
        int64_t mediaTimeUs;
        CHECK(buffer->meta()->findInt64("timeUs", &mediaTimeUs));

        if (mediaTimeUs < skipUntilMediaTimeUs) {
            ALOGV("dropping %s buffer at time %lld as requested.",
                 audio ? "audio" : "video",
                 mediaTimeUs);

            reply->post();
            return;
        }

        skipUntilMediaTimeUs = -1;
    }

    mRenderer->queueBuffer(audio, buffer, reply);
}

void NuPlayer::notifyListener(int msg, int ext1, int ext2, const Parcel *in) {
    if (mDriver == NULL) {
        return;
    }

    sp<NuPlayerDriver> driver = mDriver.promote();

    if (driver == NULL) {
        return;
    }

    driver->notifyListener(msg, ext1, ext2, in);
}

void NuPlayer::flushDecoder(bool audio, bool needShutdown) {
    ALOGV("[%s] flushDecoder needShutdown=%d",
          audio ? "audio" : "video", needShutdown);

    if ((audio && mAudioDecoder == NULL) || (!audio && mVideoDecoder == NULL)) {
        ALOGI("flushDecoder %s without decoder present",
             audio ? "audio" : "video");
    }

    // Make sure we don't continue to scan sources until we finish flushing.
    ++mScanSourcesGeneration;
    mScanSourcesPending = false;

    (audio ? mAudioDecoder : mVideoDecoder)->signalFlush();
    mRenderer->flush(audio);

    FlushStatus newStatus =
        needShutdown ? FLUSHING_DECODER_SHUTDOWN : FLUSHING_DECODER;

    if (audio) {
        CHECK(mFlushingAudio == NONE
                || mFlushingAudio == AWAITING_DISCONTINUITY);

        mFlushingAudio = newStatus;

        if (mFlushingVideo == NONE) {
            mFlushingVideo = (mVideoDecoder != NULL)
                ? AWAITING_DISCONTINUITY
                : FLUSHED;
        }
    } else {
        CHECK(mFlushingVideo == NONE
                || mFlushingVideo == AWAITING_DISCONTINUITY);

        mFlushingVideo = newStatus;

        if (mFlushingAudio == NONE) {
            mFlushingAudio = (mAudioDecoder != NULL)
                ? AWAITING_DISCONTINUITY
                : FLUSHED;
        }
    }
}

sp<AMessage> NuPlayer::Source::getFormat(bool audio) {
    sp<MetaData> meta = getFormatMeta(audio);

    if (meta == NULL) {
        return NULL;
    }

    sp<AMessage> msg = new AMessage;

    if(convertMetaDataToMessage(meta, &msg) == OK) {
        return msg;
    }
    return NULL;
}

status_t NuPlayer::setVideoScalingMode(int32_t mode) {
    mVideoScalingMode = mode;
    if (mNativeWindow != NULL) {
        status_t ret = native_window_set_scaling_mode(
                mNativeWindow->getNativeWindow().get(), mVideoScalingMode);
        if (ret != OK) {
            ALOGE("Failed to set scaling mode (%d): %s",
                -ret, strerror(-ret));
            return ret;
        }
    }
    return OK;
}

status_t NuPlayer::getTrackInfo(Parcel* reply) const {
    sp<AMessage> msg = new AMessage(kWhatGetTrackInfo, id());
    msg->setPointer("reply", reply);

    sp<AMessage> response;
    status_t err = msg->postAndAwaitResponse(&response);
    return err;
}

status_t NuPlayer::selectTrack(size_t trackIndex, bool select) {
    sp<AMessage> msg = new AMessage(kWhatSelectTrack, id());
    msg->setSize("trackIndex", trackIndex);
    msg->setInt32("select", select);

    sp<AMessage> response;
    status_t err = msg->postAndAwaitResponse(&response);

    return err;
}

void NuPlayer::schedulePollDuration() {
    sp<AMessage> msg = new AMessage(kWhatPollDuration, id());
    msg->setInt32("generation", mPollDurationGeneration);
    msg->post();
}

void NuPlayer::cancelPollDuration() {
    ++mPollDurationGeneration;
}

void NuPlayer::processDeferredActions() {
    while (!mDeferredActions.empty()) {
        // We won't execute any deferred actions until we're no longer in
        // an intermediate state, i.e. one more more decoders are currently
        // flushing or shutting down.

        if (mRenderer != NULL) {
            // There's an edge case where the renderer owns all output
            // buffers and is paused, therefore the decoder will not read
            // more input data and will never encounter the matching
            // discontinuity. To avoid this, we resume the renderer.

            if (mFlushingAudio == AWAITING_DISCONTINUITY
                    || mFlushingVideo == AWAITING_DISCONTINUITY) {
                mRenderer->resume();
            }
        }

        if (mFlushingAudio != NONE || mFlushingVideo != NONE) {
            // We're currently flushing, postpone the reset until that's
            // completed.

            ALOGV("postponing action mFlushingAudio=%d, mFlushingVideo=%d",
                  mFlushingAudio, mFlushingVideo);

            break;
        }

        sp<Action> action = *mDeferredActions.begin();
        mDeferredActions.erase(mDeferredActions.begin());

        action->execute(this);
    }
}

void NuPlayer::performSeek(int64_t seekTimeUs) {
    ALOGV("performSeek seekTimeUs=%lld us (%.2f secs)",
          seekTimeUs,
          seekTimeUs / 1E6);

    mSource->seekTo(seekTimeUs);

    if (mDriver != NULL) {
        sp<NuPlayerDriver> driver = mDriver.promote();
        if (driver != NULL) {
            driver->notifyPosition(seekTimeUs);
            driver->notifySeekComplete();
        }
    }

    // everything's flushed, continue playback.
}

void NuPlayer::performDecoderFlush() {
    ALOGV("performDecoderFlush");

    if (mAudioDecoder == NULL && mVideoDecoder == NULL) {
        return;
    }

    mTimeDiscontinuityPending = true;

    if (mAudioDecoder != NULL) {
        flushDecoder(true /* audio */, false /* needShutdown */);
    }

    if (mVideoDecoder != NULL) {
        flushDecoder(false /* audio */, false /* needShutdown */);
    }
}

void NuPlayer::performDecoderShutdown(bool audio, bool video) {
    ALOGV("performDecoderShutdown audio=%d, video=%d", audio, video);

    if ((!audio || mAudioDecoder == NULL)
            && (!video || mVideoDecoder == NULL)) {
        return;
    }

    mTimeDiscontinuityPending = true;

    if (mFlushingAudio == NONE && (!audio || mAudioDecoder == NULL)) {
        mFlushingAudio = FLUSHED;
    }

    if (mFlushingVideo == NONE && (!video || mVideoDecoder == NULL)) {
        mFlushingVideo = FLUSHED;
    }

    if (audio && mAudioDecoder != NULL) {
        flushDecoder(true /* audio */, true /* needShutdown */);
    }

    if (video && mVideoDecoder != NULL) {
        flushDecoder(false /* audio */, true /* needShutdown */);
    }
}

void NuPlayer::performReset() {
    ALOGV("performReset");

    CHECK(mAudioDecoder == NULL);
    CHECK(mVideoDecoder == NULL);

    cancelPollDuration();

    ++mScanSourcesGeneration;
    mScanSourcesPending = false;

    mRenderer.clear();

    if (mSource != NULL) {
        mSource->stop();

        looper()->unregisterHandler(mSource->id());

        mSource.clear();
    }

    if (mDriver != NULL) {
        sp<NuPlayerDriver> driver = mDriver.promote();
        if (driver != NULL) {
            driver->notifyResetComplete();
        }
    }

    mStarted = false;
}

void NuPlayer::performScanSources() {
    ALOGV("performScanSources");

    if (!mStarted) {
        return;
    }

    if (mAudioDecoder == NULL || mVideoDecoder == NULL) {
        postScanSources();
    }
}

void NuPlayer::performSetSurface(const sp<NativeWindowWrapper> &wrapper) {
    ALOGV("performSetSurface");

    mNativeWindow = wrapper;

    // XXX - ignore error from setVideoScalingMode for now
    setVideoScalingMode(mVideoScalingMode);

    if (mDriver != NULL) {
        sp<NuPlayerDriver> driver = mDriver.promote();
        if (driver != NULL) {
            driver->notifySetSurfaceComplete();
        }
    }
}

void NuPlayer::onSourceNotify(const sp<AMessage> &msg) {
    int32_t what;
    CHECK(msg->findInt32("what", &what));

    switch (what) {
        case Source::kWhatPrepared:
        {
            if (mSource == NULL) {
                // This is a stale notification from a source that was
                // asynchronously preparing when the client called reset().
                // We handled the reset, the source is gone.
                break;
            }

            int32_t err;
            CHECK(msg->findInt32("err", &err));

            sp<NuPlayerDriver> driver = mDriver.promote();
            if (driver != NULL) {
                driver->notifyPrepareCompleted(err);
            }

            int64_t durationUs;
            if (mDriver != NULL && mSource->getDuration(&durationUs) == OK) {
                sp<NuPlayerDriver> driver = mDriver.promote();
                if (driver != NULL) {
                    driver->notifyDuration(durationUs);
                }
            }
            break;
        }

        case Source::kWhatFlagsChanged:
        {
            uint32_t flags;
            CHECK(msg->findInt32("flags", (int32_t *)&flags));

            sp<NuPlayerDriver> driver = mDriver.promote();
            if (driver != NULL) {
                driver->notifyFlagsChanged(flags);
            }

            if ((mSourceFlags & Source::FLAG_DYNAMIC_DURATION)
                    && (!(flags & Source::FLAG_DYNAMIC_DURATION))) {
                cancelPollDuration();
            } else if (!(mSourceFlags & Source::FLAG_DYNAMIC_DURATION)
                    && (flags & Source::FLAG_DYNAMIC_DURATION)
                    && (mAudioDecoder != NULL || mVideoDecoder != NULL)) {
                schedulePollDuration();
            }

            mSourceFlags = flags;
            break;
        }

        case Source::kWhatVideoSizeChanged:
        {
            int32_t width, height;
            CHECK(msg->findInt32("width", &width));
            CHECK(msg->findInt32("height", &height));

            notifyListener(MEDIA_SET_VIDEO_SIZE, width, height);
            break;
        }

        case Source::kWhatBufferingStart:
        {
            notifyListener(MEDIA_INFO, MEDIA_INFO_BUFFERING_START, 0);
            break;
        }

        case Source::kWhatBufferingEnd:
        {
            notifyListener(MEDIA_INFO, MEDIA_INFO_BUFFERING_END, 0);
            break;
        }

        case Source::kWhatSubtitleData:
        {
            sp<ABuffer> buffer;
            CHECK(msg->findBuffer("buffer", &buffer));

            int32_t trackIndex;
            int64_t timeUs, durationUs;
            CHECK(buffer->meta()->findInt32("trackIndex", &trackIndex));
            CHECK(buffer->meta()->findInt64("timeUs", &timeUs));
            CHECK(buffer->meta()->findInt64("durationUs", &durationUs));

            Parcel in;
            in.writeInt32(trackIndex);
            in.writeInt64(timeUs);
            in.writeInt64(durationUs);
            in.writeInt32(buffer->size());
            in.writeInt32(buffer->size());
            in.write(buffer->data(), buffer->size());

            notifyListener(MEDIA_SUBTITLE_DATA, 0, 0, &in);
            break;
        }

        case Source::kWhatQueueDecoderShutdown:
        {
            int32_t audio, video;
            CHECK(msg->findInt32("audio", &audio));
            CHECK(msg->findInt32("video", &video));

            sp<AMessage> reply;
            CHECK(msg->findMessage("reply", &reply));

            queueDecoderShutdown(audio, video, reply);
            break;
        }

        default:
            TRESPASS();
    }
}

////////////////////////////////////////////////////////////////////////////////

void NuPlayer::Source::notifyFlagsChanged(uint32_t flags) {
    sp<AMessage> notify = dupNotify();
    notify->setInt32("what", kWhatFlagsChanged);
    notify->setInt32("flags", flags);
    notify->post();
}

void NuPlayer::Source::notifyVideoSizeChanged(int32_t width, int32_t height) {
    sp<AMessage> notify = dupNotify();
    notify->setInt32("what", kWhatVideoSizeChanged);
    notify->setInt32("width", width);
    notify->setInt32("height", height);
    notify->post();
}

void NuPlayer::Source::notifyPrepared(status_t err) {
    sp<AMessage> notify = dupNotify();
    notify->setInt32("what", kWhatPrepared);
    notify->setInt32("err", err);
    notify->post();
}

void NuPlayer::Source::onMessageReceived(const sp<AMessage> &msg) {
    TRESPASS();
}

void NuPlayer::queueDecoderShutdown(
        bool audio, bool video, const sp<AMessage> &reply) {
    ALOGI("queueDecoderShutdown audio=%d, video=%d", audio, video);

    mDeferredActions.push_back(
            new ShutdownDecoderAction(audio, video));

    mDeferredActions.push_back(
            new SimpleAction(&NuPlayer::performScanSources));

    mDeferredActions.push_back(new PostMessageAction(reply));

    processDeferredActions();
}

}  // namespace android

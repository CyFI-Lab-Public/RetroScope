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
#define LOG_TAG "RTSPSource"
#include <utils/Log.h>

#include "RTSPSource.h"

#include "AnotherPacketSource.h"
#include "MyHandler.h"
#include "SDPLoader.h"

#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>

namespace android {

const int64_t kNearEOSTimeoutUs = 2000000ll; // 2 secs

NuPlayer::RTSPSource::RTSPSource(
        const sp<AMessage> &notify,
        const char *url,
        const KeyedVector<String8, String8> *headers,
        bool uidValid,
        uid_t uid,
        bool isSDP)
    : Source(notify),
      mURL(url),
      mUIDValid(uidValid),
      mUID(uid),
      mFlags(0),
      mIsSDP(isSDP),
      mState(DISCONNECTED),
      mFinalResult(OK),
      mDisconnectReplyID(0),
      mBuffering(true),
      mSeekGeneration(0),
      mEOSTimeoutAudio(0),
      mEOSTimeoutVideo(0) {
    if (headers) {
        mExtraHeaders = *headers;

        ssize_t index =
            mExtraHeaders.indexOfKey(String8("x-hide-urls-from-log"));

        if (index >= 0) {
            mFlags |= kFlagIncognito;

            mExtraHeaders.removeItemsAt(index);
        }
    }
}

NuPlayer::RTSPSource::~RTSPSource() {
    if (mLooper != NULL) {
        mLooper->stop();
    }
}

void NuPlayer::RTSPSource::prepareAsync() {
    if (mLooper == NULL) {
        mLooper = new ALooper;
        mLooper->setName("rtsp");
        mLooper->start();

        mReflector = new AHandlerReflector<RTSPSource>(this);
        mLooper->registerHandler(mReflector);
    }

    CHECK(mHandler == NULL);
    CHECK(mSDPLoader == NULL);

    sp<AMessage> notify = new AMessage(kWhatNotify, mReflector->id());

    CHECK_EQ(mState, (int)DISCONNECTED);
    mState = CONNECTING;

    if (mIsSDP) {
        mSDPLoader = new SDPLoader(notify,
                (mFlags & kFlagIncognito) ? SDPLoader::kFlagIncognito : 0,
                mUIDValid, mUID);

        mSDPLoader->load(
                mURL.c_str(), mExtraHeaders.isEmpty() ? NULL : &mExtraHeaders);
    } else {
        mHandler = new MyHandler(mURL.c_str(), notify, mUIDValid, mUID);
        mLooper->registerHandler(mHandler);

        mHandler->connect();
    }

    sp<AMessage> notifyStart = dupNotify();
    notifyStart->setInt32("what", kWhatBufferingStart);
    notifyStart->post();
}

void NuPlayer::RTSPSource::start() {
}

void NuPlayer::RTSPSource::stop() {
    if (mLooper == NULL) {
        return;
    }
    sp<AMessage> msg = new AMessage(kWhatDisconnect, mReflector->id());

    sp<AMessage> dummy;
    msg->postAndAwaitResponse(&dummy);
}

void NuPlayer::RTSPSource::pause() {
    int64_t mediaDurationUs = 0;
    getDuration(&mediaDurationUs);
    for (size_t index = 0; index < mTracks.size(); index++) {
        TrackInfo *info = &mTracks.editItemAt(index);
        sp<AnotherPacketSource> source = info->mSource;

        // Check if EOS or ERROR is received
        if (source != NULL && source->isFinished(mediaDurationUs)) {
            return;
        }
    }
    mHandler->pause();
}

void NuPlayer::RTSPSource::resume() {
    mHandler->resume();
}

status_t NuPlayer::RTSPSource::feedMoreTSData() {
    return mFinalResult;
}

sp<MetaData> NuPlayer::RTSPSource::getFormatMeta(bool audio) {
    sp<AnotherPacketSource> source = getSource(audio);

    if (source == NULL) {
        return NULL;
    }

    return source->getFormat();
}

bool NuPlayer::RTSPSource::haveSufficientDataOnAllTracks() {
    // We're going to buffer at least 2 secs worth data on all tracks before
    // starting playback (both at startup and after a seek).

    static const int64_t kMinDurationUs = 2000000ll;

    int64_t mediaDurationUs = 0;
    getDuration(&mediaDurationUs);
    if ((mAudioTrack != NULL && mAudioTrack->isFinished(mediaDurationUs))
            || (mVideoTrack != NULL && mVideoTrack->isFinished(mediaDurationUs))) {
        return true;
    }

    status_t err;
    int64_t durationUs;
    if (mAudioTrack != NULL
            && (durationUs = mAudioTrack->getBufferedDurationUs(&err))
                    < kMinDurationUs
            && err == OK) {
        ALOGV("audio track doesn't have enough data yet. (%.2f secs buffered)",
              durationUs / 1E6);
        return false;
    }

    if (mVideoTrack != NULL
            && (durationUs = mVideoTrack->getBufferedDurationUs(&err))
                    < kMinDurationUs
            && err == OK) {
        ALOGV("video track doesn't have enough data yet. (%.2f secs buffered)",
              durationUs / 1E6);
        return false;
    }

    return true;
}

status_t NuPlayer::RTSPSource::dequeueAccessUnit(
        bool audio, sp<ABuffer> *accessUnit) {
    if (mBuffering) {
        if (!haveSufficientDataOnAllTracks()) {
            return -EWOULDBLOCK;
        }

        mBuffering = false;

        sp<AMessage> notify = dupNotify();
        notify->setInt32("what", kWhatBufferingEnd);
        notify->post();
    }

    sp<AnotherPacketSource> source = getSource(audio);

    if (source == NULL) {
        return -EWOULDBLOCK;
    }

    status_t finalResult;
    if (!source->hasBufferAvailable(&finalResult)) {
        if (finalResult == OK) {
            int64_t mediaDurationUs = 0;
            getDuration(&mediaDurationUs);
            sp<AnotherPacketSource> otherSource = getSource(!audio);
            status_t otherFinalResult;

            // If other source already signaled EOS, this source should also signal EOS
            if (otherSource != NULL &&
                    !otherSource->hasBufferAvailable(&otherFinalResult) &&
                    otherFinalResult == ERROR_END_OF_STREAM) {
                source->signalEOS(ERROR_END_OF_STREAM);
                return ERROR_END_OF_STREAM;
            }

            // If this source has detected near end, give it some time to retrieve more
            // data before signaling EOS
            if (source->isFinished(mediaDurationUs)) {
                int64_t eosTimeout = audio ? mEOSTimeoutAudio : mEOSTimeoutVideo;
                if (eosTimeout == 0) {
                    setEOSTimeout(audio, ALooper::GetNowUs());
                } else if ((ALooper::GetNowUs() - eosTimeout) > kNearEOSTimeoutUs) {
                    setEOSTimeout(audio, 0);
                    source->signalEOS(ERROR_END_OF_STREAM);
                    return ERROR_END_OF_STREAM;
                }
                return -EWOULDBLOCK;
            }

            if (!(otherSource != NULL && otherSource->isFinished(mediaDurationUs))) {
                // We should not enter buffering mode
                // if any of the sources already have detected EOS.
                mBuffering = true;

                sp<AMessage> notify = dupNotify();
                notify->setInt32("what", kWhatBufferingStart);
                notify->post();
            }

            return -EWOULDBLOCK;
        }
        return finalResult;
    }

    setEOSTimeout(audio, 0);

    return source->dequeueAccessUnit(accessUnit);
}

sp<AnotherPacketSource> NuPlayer::RTSPSource::getSource(bool audio) {
    if (mTSParser != NULL) {
        sp<MediaSource> source = mTSParser->getSource(
                audio ? ATSParser::AUDIO : ATSParser::VIDEO);

        return static_cast<AnotherPacketSource *>(source.get());
    }

    return audio ? mAudioTrack : mVideoTrack;
}

void NuPlayer::RTSPSource::setEOSTimeout(bool audio, int64_t timeout) {
    if (audio) {
        mEOSTimeoutAudio = timeout;
    } else {
        mEOSTimeoutVideo = timeout;
    }
}

status_t NuPlayer::RTSPSource::getDuration(int64_t *durationUs) {
    *durationUs = 0ll;

    int64_t audioDurationUs;
    if (mAudioTrack != NULL
            && mAudioTrack->getFormat()->findInt64(
                kKeyDuration, &audioDurationUs)
            && audioDurationUs > *durationUs) {
        *durationUs = audioDurationUs;
    }

    int64_t videoDurationUs;
    if (mVideoTrack != NULL
            && mVideoTrack->getFormat()->findInt64(
                kKeyDuration, &videoDurationUs)
            && videoDurationUs > *durationUs) {
        *durationUs = videoDurationUs;
    }

    return OK;
}

status_t NuPlayer::RTSPSource::seekTo(int64_t seekTimeUs) {
    sp<AMessage> msg = new AMessage(kWhatPerformSeek, mReflector->id());
    msg->setInt32("generation", ++mSeekGeneration);
    msg->setInt64("timeUs", seekTimeUs);
    msg->post(200000ll);

    return OK;
}

void NuPlayer::RTSPSource::performSeek(int64_t seekTimeUs) {
    if (mState != CONNECTED) {
        return;
    }

    mState = SEEKING;
    mHandler->seek(seekTimeUs);
}

void NuPlayer::RTSPSource::onMessageReceived(const sp<AMessage> &msg) {
    if (msg->what() == kWhatDisconnect) {
        uint32_t replyID;
        CHECK(msg->senderAwaitsResponse(&replyID));

        mDisconnectReplyID = replyID;
        finishDisconnectIfPossible();
        return;
    } else if (msg->what() == kWhatPerformSeek) {
        int32_t generation;
        CHECK(msg->findInt32("generation", &generation));

        if (generation != mSeekGeneration) {
            // obsolete.
            return;
        }

        int64_t seekTimeUs;
        CHECK(msg->findInt64("timeUs", &seekTimeUs));

        performSeek(seekTimeUs);
        return;
    }

    CHECK_EQ(msg->what(), (int)kWhatNotify);

    int32_t what;
    CHECK(msg->findInt32("what", &what));

    switch (what) {
        case MyHandler::kWhatConnected:
        {
            onConnected();

            notifyVideoSizeChanged(0, 0);

            uint32_t flags = 0;

            if (mHandler->isSeekable()) {
                flags = FLAG_CAN_PAUSE
                        | FLAG_CAN_SEEK
                        | FLAG_CAN_SEEK_BACKWARD
                        | FLAG_CAN_SEEK_FORWARD;
            }

            notifyFlagsChanged(flags);
            notifyPrepared();
            break;
        }

        case MyHandler::kWhatDisconnected:
        {
            onDisconnected(msg);
            break;
        }

        case MyHandler::kWhatSeekDone:
        {
            mState = CONNECTED;
            break;
        }

        case MyHandler::kWhatAccessUnit:
        {
            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));

            if (mTSParser == NULL) {
                CHECK_LT(trackIndex, mTracks.size());
            } else {
                CHECK_EQ(trackIndex, 0u);
            }

            sp<ABuffer> accessUnit;
            CHECK(msg->findBuffer("accessUnit", &accessUnit));

            int32_t damaged;
            if (accessUnit->meta()->findInt32("damaged", &damaged)
                    && damaged) {
                ALOGI("dropping damaged access unit.");
                break;
            }

            if (mTSParser != NULL) {
                size_t offset = 0;
                status_t err = OK;
                while (offset + 188 <= accessUnit->size()) {
                    err = mTSParser->feedTSPacket(
                            accessUnit->data() + offset, 188);
                    if (err != OK) {
                        break;
                    }

                    offset += 188;
                }

                if (offset < accessUnit->size()) {
                    err = ERROR_MALFORMED;
                }

                if (err != OK) {
                    sp<AnotherPacketSource> source = getSource(false /* audio */);
                    if (source != NULL) {
                        source->signalEOS(err);
                    }

                    source = getSource(true /* audio */);
                    if (source != NULL) {
                        source->signalEOS(err);
                    }
                }
                break;
            }

            TrackInfo *info = &mTracks.editItemAt(trackIndex);

            sp<AnotherPacketSource> source = info->mSource;
            if (source != NULL) {
                uint32_t rtpTime;
                CHECK(accessUnit->meta()->findInt32("rtp-time", (int32_t *)&rtpTime));

                if (!info->mNPTMappingValid) {
                    // This is a live stream, we didn't receive any normal
                    // playtime mapping. We won't map to npt time.
                    source->queueAccessUnit(accessUnit);
                    break;
                }

                int64_t nptUs =
                    ((double)rtpTime - (double)info->mRTPTime)
                        / info->mTimeScale
                        * 1000000ll
                        + info->mNormalPlaytimeUs;

                accessUnit->meta()->setInt64("timeUs", nptUs);

                source->queueAccessUnit(accessUnit);
            }
            break;
        }

        case MyHandler::kWhatEOS:
        {
            int32_t finalResult;
            CHECK(msg->findInt32("finalResult", &finalResult));
            CHECK_NE(finalResult, (status_t)OK);

            if (mTSParser != NULL) {
                sp<AnotherPacketSource> source = getSource(false /* audio */);
                if (source != NULL) {
                    source->signalEOS(finalResult);
                }

                source = getSource(true /* audio */);
                if (source != NULL) {
                    source->signalEOS(finalResult);
                }

                return;
            }

            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));
            CHECK_LT(trackIndex, mTracks.size());

            TrackInfo *info = &mTracks.editItemAt(trackIndex);
            sp<AnotherPacketSource> source = info->mSource;
            if (source != NULL) {
                source->signalEOS(finalResult);
            }

            break;
        }

        case MyHandler::kWhatSeekDiscontinuity:
        {
            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));
            CHECK_LT(trackIndex, mTracks.size());

            TrackInfo *info = &mTracks.editItemAt(trackIndex);
            sp<AnotherPacketSource> source = info->mSource;
            if (source != NULL) {
                source->queueDiscontinuity(ATSParser::DISCONTINUITY_SEEK, NULL);
            }

            break;
        }

        case MyHandler::kWhatNormalPlayTimeMapping:
        {
            size_t trackIndex;
            CHECK(msg->findSize("trackIndex", &trackIndex));
            CHECK_LT(trackIndex, mTracks.size());

            uint32_t rtpTime;
            CHECK(msg->findInt32("rtpTime", (int32_t *)&rtpTime));

            int64_t nptUs;
            CHECK(msg->findInt64("nptUs", &nptUs));

            TrackInfo *info = &mTracks.editItemAt(trackIndex);
            info->mRTPTime = rtpTime;
            info->mNormalPlaytimeUs = nptUs;
            info->mNPTMappingValid = true;
            break;
        }

        case SDPLoader::kWhatSDPLoaded:
        {
            onSDPLoaded(msg);
            break;
        }

        default:
            TRESPASS();
    }
}

void NuPlayer::RTSPSource::onConnected() {
    CHECK(mAudioTrack == NULL);
    CHECK(mVideoTrack == NULL);

    size_t numTracks = mHandler->countTracks();
    for (size_t i = 0; i < numTracks; ++i) {
        int32_t timeScale;
        sp<MetaData> format = mHandler->getTrackFormat(i, &timeScale);

        const char *mime;
        CHECK(format->findCString(kKeyMIMEType, &mime));

        if (!strcasecmp(mime, MEDIA_MIMETYPE_CONTAINER_MPEG2TS)) {
            // Very special case for MPEG2 Transport Streams.
            CHECK_EQ(numTracks, 1u);

            mTSParser = new ATSParser;
            return;
        }

        bool isAudio = !strncasecmp(mime, "audio/", 6);
        bool isVideo = !strncasecmp(mime, "video/", 6);

        TrackInfo info;
        info.mTimeScale = timeScale;
        info.mRTPTime = 0;
        info.mNormalPlaytimeUs = 0ll;
        info.mNPTMappingValid = false;

        if ((isAudio && mAudioTrack == NULL)
                || (isVideo && mVideoTrack == NULL)) {
            sp<AnotherPacketSource> source = new AnotherPacketSource(format);

            if (isAudio) {
                mAudioTrack = source;
            } else {
                mVideoTrack = source;
            }

            info.mSource = source;
        }

        mTracks.push(info);
    }

    mState = CONNECTED;
}

void NuPlayer::RTSPSource::onSDPLoaded(const sp<AMessage> &msg) {
    status_t err;
    CHECK(msg->findInt32("result", &err));

    mSDPLoader.clear();

    if (mDisconnectReplyID != 0) {
        err = UNKNOWN_ERROR;
    }

    if (err == OK) {
        sp<ASessionDescription> desc;
        sp<RefBase> obj;
        CHECK(msg->findObject("description", &obj));
        desc = static_cast<ASessionDescription *>(obj.get());

        AString rtspUri;
        if (!desc->findAttribute(0, "a=control", &rtspUri)) {
            ALOGE("Unable to find url in SDP");
            err = UNKNOWN_ERROR;
        } else {
            sp<AMessage> notify = new AMessage(kWhatNotify, mReflector->id());

            mHandler = new MyHandler(rtspUri.c_str(), notify, mUIDValid, mUID);
            mLooper->registerHandler(mHandler);

            mHandler->loadSDP(desc);
        }
    }

    if (err != OK) {
        if (mState == CONNECTING) {
            // We're still in the preparation phase, signal that it
            // failed.
            notifyPrepared(err);
        }

        mState = DISCONNECTED;
        mFinalResult = err;

        if (mDisconnectReplyID != 0) {
            finishDisconnectIfPossible();
        }
    }
}

void NuPlayer::RTSPSource::onDisconnected(const sp<AMessage> &msg) {
    if (mState == DISCONNECTED) {
        return;
    }

    status_t err;
    CHECK(msg->findInt32("result", &err));
    CHECK_NE(err, (status_t)OK);

    mLooper->unregisterHandler(mHandler->id());
    mHandler.clear();

    if (mState == CONNECTING) {
        // We're still in the preparation phase, signal that it
        // failed.
        notifyPrepared(err);
    }

    mState = DISCONNECTED;
    mFinalResult = err;

    if (mDisconnectReplyID != 0) {
        finishDisconnectIfPossible();
    }
}

void NuPlayer::RTSPSource::finishDisconnectIfPossible() {
    if (mState != DISCONNECTED) {
        if (mHandler != NULL) {
            mHandler->disconnect();
        } else if (mSDPLoader != NULL) {
            mSDPLoader->cancel();
        }
        return;
    }

    (new AMessage)->postReply(mDisconnectReplyID);
    mDisconnectReplyID = 0;
}

}  // namespace android

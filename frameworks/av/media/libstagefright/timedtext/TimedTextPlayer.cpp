 /*
 * Copyright (C) 2012 The Android Open Source Project
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
#define LOG_TAG "TimedTextPlayer"
#include <utils/Log.h>

#include <limits.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/timedtext/TimedTextDriver.h>
#include <media/stagefright/MediaErrors.h>
#include <media/MediaPlayerInterface.h>

#include "TimedTextPlayer.h"

#include "TimedTextSource.h"

namespace android {

// Event should be fired a bit earlier considering the processing time till
// application actually gets the notification message.
static const int64_t kAdjustmentProcessingTimeUs = 100000ll;
static const int64_t kMaxDelayUs = 5000000ll;
static const int64_t kWaitTimeUsToRetryRead = 100000ll;
static const int64_t kInvalidTimeUs = INT_MIN;

TimedTextPlayer::TimedTextPlayer(const wp<MediaPlayerBase> &listener)
    : mListener(listener),
      mSource(NULL),
      mPendingSeekTimeUs(kInvalidTimeUs),
      mPaused(false),
      mSendSubtitleGeneration(0) {
}

TimedTextPlayer::~TimedTextPlayer() {
    if (mSource != NULL) {
        mSource->stop();
        mSource.clear();
        mSource = NULL;
    }
}

void TimedTextPlayer::start() {
    (new AMessage(kWhatStart, id()))->post();
}

void TimedTextPlayer::pause() {
    (new AMessage(kWhatPause, id()))->post();
}

void TimedTextPlayer::resume() {
    (new AMessage(kWhatResume, id()))->post();
}

void TimedTextPlayer::seekToAsync(int64_t timeUs) {
    sp<AMessage> msg = new AMessage(kWhatSeek, id());
    msg->setInt64("seekTimeUs", timeUs);
    msg->post();
}

void TimedTextPlayer::setDataSource(sp<TimedTextSource> source) {
    sp<AMessage> msg = new AMessage(kWhatSetSource, id());
    msg->setObject("source", source);
    msg->post();
}

void TimedTextPlayer::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatPause: {
            mPaused = true;
            break;
        }
        case kWhatResume: {
            mPaused = false;
            if (mPendingSeekTimeUs != kInvalidTimeUs) {
                seekToAsync(mPendingSeekTimeUs);
                mPendingSeekTimeUs = kInvalidTimeUs;
            } else {
                doRead();
            }
            break;
        }
        case kWhatStart: {
            sp<MediaPlayerBase> listener = mListener.promote();
            if (listener == NULL) {
                ALOGE("Listener is NULL when kWhatStart is received.");
                break;
            }
            mPaused = false;
            mPendingSeekTimeUs = kInvalidTimeUs;
            int32_t positionMs = 0;
            listener->getCurrentPosition(&positionMs);
            int64_t seekTimeUs = positionMs * 1000ll;

            notifyListener();
            mSendSubtitleGeneration++;
            doSeekAndRead(seekTimeUs);
            break;
        }
        case kWhatRetryRead: {
            int32_t generation = -1;
            CHECK(msg->findInt32("generation", &generation));
            if (generation != mSendSubtitleGeneration) {
                // Drop obsolete msg.
                break;
            }
            int64_t seekTimeUs;
            int seekMode;
            if (msg->findInt64("seekTimeUs", &seekTimeUs) &&
                msg->findInt32("seekMode", &seekMode)) {
                MediaSource::ReadOptions options;
                options.setSeekTo(
                    seekTimeUs,
                    static_cast<MediaSource::ReadOptions::SeekMode>(seekMode));
                doRead(&options);
            } else {
                doRead();
            }
            break;
        }
        case kWhatSeek: {
            int64_t seekTimeUs = kInvalidTimeUs;
            // Clear a displayed timed text before seeking.
            notifyListener();
            msg->findInt64("seekTimeUs", &seekTimeUs);
            if (seekTimeUs == kInvalidTimeUs) {
                sp<MediaPlayerBase> listener = mListener.promote();
                if (listener != NULL) {
                    int32_t positionMs = 0;
                    listener->getCurrentPosition(&positionMs);
                    seekTimeUs = positionMs * 1000ll;
                }
            }
            if (mPaused) {
                mPendingSeekTimeUs = seekTimeUs;
                break;
            }
            mSendSubtitleGeneration++;
            doSeekAndRead(seekTimeUs);
            break;
        }
        case kWhatSendSubtitle: {
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));
            if (generation != mSendSubtitleGeneration) {
                // Drop obsolete msg.
                break;
            }
            // If current time doesn't reach to the fire time,
            // re-post the message with the adjusted delay time.
            int64_t fireTimeUs = kInvalidTimeUs;
            if (msg->findInt64("fireTimeUs", &fireTimeUs)) {
                // TODO: check if fireTimeUs is not kInvalidTimeUs.
                int64_t delayUs = delayUsFromCurrentTime(fireTimeUs);
                if (delayUs > 0) {
                    msg->post(delayUs);
                    break;
                }
            }
            sp<RefBase> obj;
            if (msg->findObject("subtitle", &obj)) {
                sp<ParcelEvent> parcelEvent;
                parcelEvent = static_cast<ParcelEvent*>(obj.get());
                notifyListener(&(parcelEvent->parcel));
                doRead();
            } else {
                notifyListener();
            }
            break;
        }
        case kWhatSetSource: {
            mSendSubtitleGeneration++;
            sp<RefBase> obj;
            msg->findObject("source", &obj);
            if (mSource != NULL) {
                mSource->stop();
                mSource.clear();
                mSource = NULL;
            }
            // null source means deselect track.
            if (obj == NULL) {
                mPendingSeekTimeUs = kInvalidTimeUs;
                mPaused = false;
                notifyListener();
                break;
            }
            mSource = static_cast<TimedTextSource*>(obj.get());
            status_t err = mSource->start();
            if (err != OK) {
                notifyError(err);
                break;
            }
            Parcel parcel;
            err = mSource->extractGlobalDescriptions(&parcel);
            if (err != OK) {
                notifyError(err);
                break;
            }
            notifyListener(&parcel);
            break;
        }
    }
}

void TimedTextPlayer::doSeekAndRead(int64_t seekTimeUs) {
    MediaSource::ReadOptions options;
    options.setSeekTo(seekTimeUs, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    doRead(&options);
}

void TimedTextPlayer::doRead(MediaSource::ReadOptions* options) {
    int64_t startTimeUs = 0;
    int64_t endTimeUs = 0;
    sp<ParcelEvent> parcelEvent = new ParcelEvent();
    CHECK(mSource != NULL);
    status_t err = mSource->read(&startTimeUs, &endTimeUs,
                                 &(parcelEvent->parcel), options);
    if (err == WOULD_BLOCK) {
        sp<AMessage> msg = new AMessage(kWhatRetryRead, id());
        if (options != NULL) {
            int64_t seekTimeUs = kInvalidTimeUs;
            MediaSource::ReadOptions::SeekMode seekMode =
                MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC;
            CHECK(options->getSeekTo(&seekTimeUs, &seekMode));
            msg->setInt64("seekTimeUs", seekTimeUs);
            msg->setInt32("seekMode", seekMode);
        }
        msg->setInt32("generation", mSendSubtitleGeneration);
        msg->post(kWaitTimeUsToRetryRead);
        return;
    } else if (err != OK) {
        notifyError(err);
        return;
    }

    postTextEvent(parcelEvent, startTimeUs);
    if (endTimeUs > 0) {
        CHECK_GE(endTimeUs, startTimeUs);
        // send an empty timed text to clear the subtitle when it reaches to the
        // end time.
        postTextEvent(NULL, endTimeUs);
    }
}

void TimedTextPlayer::postTextEvent(const sp<ParcelEvent>& parcel, int64_t timeUs) {
    int64_t delayUs = delayUsFromCurrentTime(timeUs);
    sp<AMessage> msg = new AMessage(kWhatSendSubtitle, id());
    msg->setInt32("generation", mSendSubtitleGeneration);
    if (parcel != NULL) {
        msg->setObject("subtitle", parcel);
    }
    msg->setInt64("fireTimeUs", timeUs);
    msg->post(delayUs);
}

int64_t TimedTextPlayer::delayUsFromCurrentTime(int64_t fireTimeUs) {
    sp<MediaPlayerBase> listener = mListener.promote();
    if (listener == NULL) {
        // TODO: it may be better to return kInvalidTimeUs
        ALOGE("%s: Listener is NULL. (fireTimeUs = %lld)",
              __FUNCTION__, fireTimeUs);
        return 0;
    }
    int32_t positionMs = 0;
    listener->getCurrentPosition(&positionMs);
    int64_t positionUs = positionMs * 1000ll;

    if (fireTimeUs <= positionUs + kAdjustmentProcessingTimeUs) {
        return 0;
    } else {
        int64_t delayUs = fireTimeUs - positionUs - kAdjustmentProcessingTimeUs;
        if (delayUs > kMaxDelayUs) {
            return kMaxDelayUs;
        }
        return delayUs;
    }
}

void TimedTextPlayer::notifyError(int error) {
    sp<MediaPlayerBase> listener = mListener.promote();
    if (listener == NULL) {
        ALOGE("%s(error=%d): Listener is NULL.", __FUNCTION__, error);
        return;
    }
    listener->sendEvent(MEDIA_INFO, MEDIA_INFO_TIMED_TEXT_ERROR, error);
}

void TimedTextPlayer::notifyListener(const Parcel *parcel) {
    sp<MediaPlayerBase> listener = mListener.promote();
    if (listener == NULL) {
        ALOGE("%s: Listener is NULL.", __FUNCTION__);
        return;
    }
    if (parcel != NULL && (parcel->dataSize() > 0)) {
        listener->sendEvent(MEDIA_TIMED_TEXT, 0, 0, parcel);
    } else {  // send an empty timed text to clear the screen
        listener->sendEvent(MEDIA_TIMED_TEXT);
    }
}

}  // namespace android

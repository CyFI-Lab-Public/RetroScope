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

#ifndef NUPLAYER_SOURCE_H_

#define NUPLAYER_SOURCE_H_

#include "NuPlayer.h"

#include <media/stagefright/foundation/AMessage.h>

namespace android {

struct ABuffer;
struct MetaData;

struct NuPlayer::Source : public AHandler {
    enum Flags {
        FLAG_CAN_PAUSE          = 1,
        FLAG_CAN_SEEK_BACKWARD  = 2,  // the "10 sec back button"
        FLAG_CAN_SEEK_FORWARD   = 4,  // the "10 sec forward button"
        FLAG_CAN_SEEK           = 8,  // the "seek bar"
        FLAG_DYNAMIC_DURATION   = 16,
    };

    enum {
        kWhatPrepared,
        kWhatFlagsChanged,
        kWhatVideoSizeChanged,
        kWhatBufferingStart,
        kWhatBufferingEnd,
        kWhatSubtitleData,
        kWhatQueueDecoderShutdown,
    };

    // The provides message is used to notify the player about various
    // events.
    Source(const sp<AMessage> &notify)
        : mNotify(notify) {
    }

    virtual void prepareAsync() = 0;

    virtual void start() = 0;
    virtual void stop() {}
    virtual void pause() {}
    virtual void resume() {}

    // Returns OK iff more data was available,
    // an error or ERROR_END_OF_STREAM if not.
    virtual status_t feedMoreTSData() = 0;

    virtual sp<AMessage> getFormat(bool audio);

    virtual status_t dequeueAccessUnit(
            bool audio, sp<ABuffer> *accessUnit) = 0;

    virtual status_t getDuration(int64_t *durationUs) {
        return INVALID_OPERATION;
    }

    virtual status_t getTrackInfo(Parcel* reply) const {
        return INVALID_OPERATION;
    }

    virtual status_t selectTrack(size_t trackIndex, bool select) {
        return INVALID_OPERATION;
    }

    virtual status_t seekTo(int64_t seekTimeUs) {
        return INVALID_OPERATION;
    }

    virtual bool isRealTime() const {
        return false;
    }

protected:
    virtual ~Source() {}

    virtual void onMessageReceived(const sp<AMessage> &msg);

    virtual sp<MetaData> getFormatMeta(bool audio) { return NULL; }

    sp<AMessage> dupNotify() const { return mNotify->dup(); }

    void notifyFlagsChanged(uint32_t flags);
    void notifyVideoSizeChanged(int32_t width, int32_t height);
    void notifyPrepared(status_t err = OK);

private:
    sp<AMessage> mNotify;

    DISALLOW_EVIL_CONSTRUCTORS(Source);
};

}  // namespace android

#endif  // NUPLAYER_SOURCE_H_


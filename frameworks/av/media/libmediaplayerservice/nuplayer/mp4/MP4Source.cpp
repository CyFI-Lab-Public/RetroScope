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

#include "MP4Source.h"

#include "FragmentedMP4Parser.h"
#include "../NuPlayerStreamListener.h"

#include <media/IStreamSource.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MetaData.h>

namespace android {

struct StreamSource : public FragmentedMP4Parser::Source {
    StreamSource(const sp<IStreamSource> &source)
        : mListener(new NuPlayer::NuPlayerStreamListener(source, 0)),
          mPosition(0) {
        mListener->start();
    }

    virtual ssize_t readAt(off64_t offset, void *data, size_t size) {
        if (offset < mPosition) {
            return -EPIPE;
        }

        while (offset > mPosition) {
            char buffer[1024];
            off64_t skipBytes = offset - mPosition;
            if (skipBytes > sizeof(buffer)) {
                skipBytes = sizeof(buffer);
            }

            sp<AMessage> extra;
            ssize_t n;
            for (;;) {
                n = mListener->read(buffer, skipBytes, &extra);

                if (n == -EWOULDBLOCK) {
                    usleep(10000);
                    continue;
                }

                break;
            }

            ALOGV("skipped %ld bytes at offset %lld", n, mPosition);

            if (n < 0) {
                return n;
            }

            mPosition += n;
        }

        sp<AMessage> extra;
        size_t total = 0;
        while (total < size) {
            ssize_t n = mListener->read(
                    (uint8_t *)data + total, size - total, &extra);

            if (n == -EWOULDBLOCK) {
                usleep(10000);
                continue;
            } else if (n == 0) {
                break;
            } else if (n < 0) {
                mPosition += total;
                return n;
            }

            total += n;
        }

        ALOGV("read %ld bytes at offset %lld", n, mPosition);

        mPosition += total;

        return total;
    }

    bool isSeekable() {
        return false;
    }

private:
    sp<NuPlayer::NuPlayerStreamListener> mListener;
    off64_t mPosition;

    DISALLOW_EVIL_CONSTRUCTORS(StreamSource);
};

MP4Source::MP4Source(
        const sp<AMessage> &notify, const sp<IStreamSource> &source)
    : Source(notify),
      mSource(source),
      mLooper(new ALooper),
      mParser(new FragmentedMP4Parser),
      mEOS(false) {
    mLooper->registerHandler(mParser);
}

MP4Source::~MP4Source() {
}

void MP4Source::prepareAsync() {
    notifyVideoSizeChanged(0, 0);
    notifyFlagsChanged(0);
    notifyPrepared();
}

void MP4Source::start() {
    mLooper->start(false /* runOnCallingThread */);
    mParser->start(new StreamSource(mSource));
}

status_t MP4Source::feedMoreTSData() {
    return mEOS ? ERROR_END_OF_STREAM : (status_t)OK;
}

sp<AMessage> MP4Source::getFormat(bool audio) {
    return mParser->getFormat(audio);
}

status_t MP4Source::dequeueAccessUnit(
        bool audio, sp<ABuffer> *accessUnit) {
    return mParser->dequeueAccessUnit(audio, accessUnit);
}

}  // namespace android

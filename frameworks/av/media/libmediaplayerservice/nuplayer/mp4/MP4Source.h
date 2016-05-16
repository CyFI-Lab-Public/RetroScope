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

#ifndef MP4_SOURCE_H
#define MP4_SOURCE_H

#include "NuPlayerSource.h"

namespace android {

struct FragmentedMP4Parser;

struct MP4Source : public NuPlayer::Source {
    MP4Source(const sp<AMessage> &notify, const sp<IStreamSource> &source);

    virtual void prepareAsync();
    virtual void start();

    virtual status_t feedMoreTSData();

    virtual sp<AMessage> getFormat(bool audio);

    virtual status_t dequeueAccessUnit(
            bool audio, sp<ABuffer> *accessUnit);

protected:
    virtual ~MP4Source();

private:
    sp<IStreamSource> mSource;
    sp<ALooper> mLooper;
    sp<FragmentedMP4Parser> mParser;
    bool mEOS;

    DISALLOW_EVIL_CONSTRUCTORS(MP4Source);
};

}  // namespace android

#endif // MP4_SOURCE_H

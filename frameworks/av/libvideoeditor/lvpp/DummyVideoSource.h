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

#ifndef DUMMY_VIDEOSOURCE_H_
#define DUMMY_VIDEOSOURCE_H_

#include <media/stagefright/MediaSource.h>
#include "M4OSA_Clock.h"
#include "M4OSA_Time.h"
#include "M4OSA_Types.h"

namespace android {

class  MediaBuffer;
class  MetaData;

struct DummyVideoSource : public MediaSource {

public:
    static sp<DummyVideoSource> Create(
                uint32_t width, uint32_t height,
                uint64_t clipDuration, const char *imageUri);

    virtual status_t start(MetaData *params = NULL);
    virtual status_t stop();
    virtual sp<MetaData> getFormat();

    virtual status_t read(
                MediaBuffer **buffer,
                const MediaSource::ReadOptions *options = NULL);

protected:
    virtual ~DummyVideoSource();

private:
    uint32_t mFrameWidth;
    uint32_t mFrameHeight;
    uint64_t mImageClipDuration;
    const char *mUri;
    int64_t mFrameTimeUs;
    bool mIsFirstImageFrame;
    void *mImageBuffer;
    M4OSA_Time mImagePlayStartTime;
    uint32_t mImageSeekTime;

    DummyVideoSource(
            uint32_t width, uint32_t height,
            uint64_t clipDuration, const char *imageUri);

    // Don't call me
    DummyVideoSource(const DummyVideoSource &);
    DummyVideoSource &operator=(const DummyVideoSource &);

};


}//namespace android


#endif //DUMMY_VIDEOSOURCE_H_


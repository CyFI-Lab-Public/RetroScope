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

#ifndef AAC_ADTS_EXTRACTOR_H_
#define AAC_ADTS_EXTRACTOR_H_

#include <utils/Vector.h>

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <utils/String8.h>

namespace android {

struct AMessage;
class String8;


class AacAdtsSource : public MediaSource {
public:
    AacAdtsSource(const sp<DataSource> &source,
              const sp<MetaData> &meta,
              //const Vector<uint64_t> &offset_vector,
              int64_t frame_duration_us);

    virtual status_t start(MetaData *params = NULL);
    virtual status_t stop();

    virtual sp<MetaData> getFormat();

    virtual status_t read(
            MediaBuffer **buffer, const ReadOptions *options = NULL);

protected:
    virtual ~AacAdtsSource();

private:
    static const size_t kMaxFrameSize;
    sp<DataSource> mDataSource;
    sp<MetaData> mMeta;

    off64_t mOffset;
    int64_t mCurrentTimeUs;
    bool mStarted;
    MediaBufferGroup *mGroup;

    int64_t mFrameDurationUs;

    AacAdtsSource(const AacAdtsSource &);
    AacAdtsSource &operator=(const AacAdtsSource &);
};


class AacAdtsExtractor : public MediaExtractor {
public:
    AacAdtsExtractor(const sp<DataSource> &source);

    virtual size_t countTracks();
    virtual sp<MediaSource> getTrack(size_t index);
    virtual sp<MetaData> getTrackMetaData(size_t index, uint32_t flags);

    virtual sp<MetaData> getMetaData();

protected:
    virtual ~AacAdtsExtractor();

private:
    sp<DataSource> mDataSource;
    sp<MetaData> mMeta;
    status_t mInitCheck;

    int64_t mFrameDurationUs;

    AacAdtsExtractor(const AacAdtsExtractor &);
    AacAdtsExtractor &operator=(const AacAdtsExtractor &);

};

}  // namespace android

#endif  // AAC_ADTS_EXTRACTOR_H_

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

#ifndef DUMMY_AUDIOSOURCE_H_
#define DUMMY_AUDIOSOURCE_H_

#include <media/stagefright/MediaSource.h>


namespace android {

class MetaData;
struct MediaBufferGroup;

struct DummyAudioSource : public MediaSource {

public:
    static sp<DummyAudioSource> Create(
                int32_t samplingRate, int32_t channelCount,
                int64_t frameDurationUs, int64_t audioDurationUs);

    virtual status_t start(MetaData *params = NULL);
    virtual status_t stop();
    virtual sp<MetaData> getFormat();

    virtual status_t read(
                MediaBuffer **buffer,
                const MediaSource::ReadOptions *options = NULL);

    void setDuration(int64_t audioDurationUs);

protected:
    virtual ~DummyAudioSource();

private:
    int32_t mSamplingRate;
    int32_t mChannelCount;
    int64_t mFrameDurationUs;
    int32_t mNumberOfSamplePerFrame;
    int64_t mAudioDurationUs;
    int64_t mTimeStampUs;
    Mutex mLock;

    MediaBufferGroup *mBufferGroup;

    DummyAudioSource(
            int32_t samplingRate, int32_t channelCount,
            int64_t frameDurationUs, int64_t audioDurationUs);

    // Don't call me
    DummyAudioSource(const DummyAudioSource &);
    DummyAudioSource &operator=(const DummyAudioSource &);

};

}//namespace android


#endif //DUMMY_AUDIOSOURCE_H_


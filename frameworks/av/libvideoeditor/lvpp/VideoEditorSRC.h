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


#include <stdint.h>
#include <media/stagefright/MediaSource.h>
#include <media/AudioBufferProvider.h>
#include "AudioResampler.h"

namespace android {

struct MediaBuffer;

class VideoEditorSRC : public MediaSource , public AudioBufferProvider {

public:
    VideoEditorSRC(const sp<MediaSource> &source);

    virtual status_t start (MetaData *params = NULL);
    virtual status_t stop();
    virtual sp<MetaData> getFormat();
    virtual status_t read (
                MediaBuffer **buffer, const ReadOptions *options = NULL);

    virtual status_t getNextBuffer(Buffer* buffer, int64_t pts);
    virtual void releaseBuffer(Buffer* buffer);

    // Sampling freqencies
    enum {
        kFreq8000Hz  = 8000,
        kFreq11025Hz = 11025,
        kFreq12000Hz = 12000,
        kFreq16000Hz = 16000,
        kFreq22050Hz = 22050,
        kFreq24000Hz = 24000,
        kFreq32000Hz = 32000,
        kFreq44100Hz = 44100,
        kFreq48000Hz = 48000,
    };

protected :
    virtual ~VideoEditorSRC();

private:
    AudioResampler *mResampler;
    sp<MediaSource> mSource;
    int mChannelCnt;
    int mSampleRate;
    int32_t mOutputSampleRate;
    bool mStarted;
    sp<MetaData> mOutputFormat;

    MediaBuffer* mBuffer;
    int32_t mLeftover;
    bool mFormatChanged;
    bool mStopPending;

    int64_t mInitialTimeStampUs;
    int64_t mAccuOutBufferSize;

    int64_t mSeekTimeUs;
    ReadOptions::SeekMode mSeekMode;

    VideoEditorSRC();
    void checkAndSetResampler();

    // Don't call me
    VideoEditorSRC(const VideoEditorSRC&);
    VideoEditorSRC &operator=(const VideoEditorSRC &);

};

} //namespce android


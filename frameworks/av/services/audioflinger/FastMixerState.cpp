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

#include "Configuration.h"
#include "FastMixerState.h"

namespace android {

FastTrack::FastTrack() :
    mBufferProvider(NULL), mVolumeProvider(NULL), mSampleRate(0),
    mChannelMask(AUDIO_CHANNEL_OUT_STEREO), mGeneration(0)
{
}

FastTrack::~FastTrack()
{
}

FastMixerState::FastMixerState() :
    mFastTracksGen(0), mTrackMask(0), mOutputSink(NULL), mOutputSinkGen(0),
    mFrameCount(0), mCommand(INITIAL), mColdFutexAddr(NULL), mColdGen(0),
    mDumpState(NULL), mTeeSink(NULL), mNBLogWriter(NULL)
{
}

FastMixerState::~FastMixerState()
{
}

}   // namespace android

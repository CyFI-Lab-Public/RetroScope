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

#ifndef ANDROID_AUDIO_FAST_MIXER_STATE_H
#define ANDROID_AUDIO_FAST_MIXER_STATE_H

#include <system/audio.h>
#include <media/ExtendedAudioBufferProvider.h>
#include <media/nbaio/NBAIO.h>
#include <media/nbaio/NBLog.h>

namespace android {

struct FastMixerDumpState;

class VolumeProvider {
public:
    // Return the track volume in U4_12 format: left in lower half, right in upper half. The
    // provider implementation is responsible for validating that the return value is in range.
    virtual uint32_t getVolumeLR() = 0;
protected:
    VolumeProvider() { }
    virtual ~VolumeProvider() { }
};

// Represents the state of a fast track
struct FastTrack {
    FastTrack();
    /*virtual*/ ~FastTrack();

    ExtendedAudioBufferProvider* mBufferProvider; // must be NULL if inactive, or non-NULL if active
    VolumeProvider*         mVolumeProvider; // optional; if NULL then full-scale
    unsigned                mSampleRate;     // optional; if zero then use mixer sample rate
    audio_channel_mask_t    mChannelMask;    // AUDIO_CHANNEL_OUT_MONO or AUDIO_CHANNEL_OUT_STEREO
    int                     mGeneration;     // increment when any field is assigned
};

// Represents a single state of the fast mixer
struct FastMixerState {
                FastMixerState();
    /*virtual*/ ~FastMixerState();

    static const unsigned kMaxFastTracks = 8;   // must be between 2 and 32 inclusive

    // all pointer fields use raw pointers; objects are owned and ref-counted by the normal mixer
    FastTrack   mFastTracks[kMaxFastTracks];
    int         mFastTracksGen; // increment when any mFastTracks[i].mGeneration is incremented
    unsigned    mTrackMask;     // bit i is set if and only if mFastTracks[i] is active
    NBAIO_Sink* mOutputSink;    // HAL output device, must already be negotiated
    int         mOutputSinkGen; // increment when mOutputSink is assigned
    size_t      mFrameCount;    // number of frames per fast mix buffer
    enum Command {
        INITIAL = 0,            // used only for the initial state
        HOT_IDLE = 1,           // do nothing
        COLD_IDLE = 2,          // wait for the futex
        IDLE = 3,               // either HOT_IDLE or COLD_IDLE
        EXIT = 4,               // exit from thread
        // The following commands also process configuration changes, and can be "or"ed:
        MIX = 0x8,              // mix tracks
        WRITE = 0x10,           // write to output sink
        MIX_WRITE = 0x18,       // mix tracks and write to output sink
    } mCommand;
    int32_t*    mColdFutexAddr; // for COLD_IDLE only, pointer to the associated futex
    unsigned    mColdGen;       // increment when COLD_IDLE is requested so it's only performed once
    // This might be a one-time configuration rather than per-state
    FastMixerDumpState* mDumpState; // if non-NULL, then update dump state periodically
    NBAIO_Sink* mTeeSink;       // if non-NULL, then duplicate write()s to this non-blocking sink
    NBLog::Writer* mNBLogWriter; // non-blocking logger
};  // struct FastMixerState

}   // namespace android

#endif  // ANDROID_AUDIO_FAST_MIXER_STATE_H

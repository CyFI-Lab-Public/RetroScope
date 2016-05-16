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

#ifndef STATIC_AUDIO_TRACK_STATE_H
#define STATIC_AUDIO_TRACK_STATE_H

namespace android {

// Represents a single state of an AudioTrack that was created in static mode (shared memory buffer
// supplied by the client).  This state needs to be communicated from the client to server.  As this
// state is too large to be updated atomically without a mutex, and mutexes aren't allowed here, the
// state is wrapped by a SingleStateQueue.
struct StaticAudioTrackState {
    // do not define constructors, destructors, or virtual methods
    size_t  mLoopStart;
    size_t  mLoopEnd;
    int     mLoopCount;
};

}   // namespace android

#endif  // STATIC_AUDIO_TRACK_STATE_H

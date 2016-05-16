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

#ifndef _MEDIA_BUFFER_PULLER_H
#define _MEDIA_BUFFER_PULLER_H

#include <utils/threads.h>
#include <utils/Vector.h>


namespace android {

struct MediaSource;
struct MediaBuffer;

/*
 * An object of this class can pull a list of media buffers
 * from a MediaSource repeatedly. The user can then get the
 * buffers from that list.
 */
struct MediaBufferPuller {
public:
    MediaBufferPuller(const sp<MediaSource>& source);
    ~MediaBufferPuller();

    // Start to build up the list of the buffers.
    void start();

    // Release the list of the available buffers, and stop
    // pulling buffers from the MediaSource.
    void stop();

    // Get a buffer from the list. If there is no buffer available
    // at the time this method is called, NULL is returned.
    MediaBuffer* getBufferBlocking();

    // Get a buffer from the list. If there is no buffer available
    // at the time this method is called, it blocks waiting for
    // a buffer to become available or until stop() is called.
    MediaBuffer* getBufferNonBlocking();

    // Add a buffer to the end of the list available media buffers
    void putBuffer(MediaBuffer* buffer);

    // Check whether the source returned an error or not.
    bool hasMediaSourceReturnedError() const;

private:
    static int acquireThreadStart(void* arg);
    void acquireThreadFunc();

    static int releaseThreadStart(void* arg);
    void releaseThreadFunc();

    sp<MediaSource> mSource;
    Vector<MediaBuffer*> mBuffers;
    Vector<MediaBuffer*> mReleaseBuffers;

    mutable Mutex mLock;
    Condition mUserCond;     // for the user of this class
    Condition mAcquireCond;  // for the acquire thread
    Condition mReleaseCond;  // for the release thread

    bool mAskToStart;      // Asks the threads to start
    bool mAskToStop;       // Asks the threads to stop
    bool mAcquireStopped;  // The acquire thread has stopped
    bool mReleaseStopped;  // The release thread has stopped
    status_t mSourceError; // Error returned by MediaSource read

    // Don't call me!
    MediaBufferPuller(const MediaBufferPuller&);
    MediaBufferPuller& operator=(const MediaBufferPuller&);
};

}  // namespace android

#endif  // _MEDIA_BUFFER_PULLER_H

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

//#define LOG_NDEBUG 0
#define LOG_TAG "MediaBufferPuller"
#include <utils/Log.h>

#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include "MediaBufferPuller.h"

namespace android {


MediaBufferPuller::MediaBufferPuller(const sp<MediaSource>& source)
    : mSource(source),
      mAskToStart(false),
      mAskToStop(false),
      mAcquireStopped(false),
      mReleaseStopped(false),
      mSourceError(OK) {

    androidCreateThread(acquireThreadStart, this);
    androidCreateThread(releaseThreadStart, this);
}

MediaBufferPuller::~MediaBufferPuller() {
    stop();
}

bool MediaBufferPuller::hasMediaSourceReturnedError() const {
    Mutex::Autolock autolock(mLock);
    return ((mSourceError != OK) ? true : false);
}
void MediaBufferPuller::start() {
    Mutex::Autolock autolock(mLock);
    mAskToStart = true;
    mAcquireCond.signal();
    mReleaseCond.signal();
}

void MediaBufferPuller::stop() {
    Mutex::Autolock autolock(mLock);
    mAskToStop = true;
    mAcquireCond.signal();
    mReleaseCond.signal();
    while (!mAcquireStopped || !mReleaseStopped) {
        mUserCond.wait(mLock);
    }

    // Release remaining buffers
    for (size_t i = 0; i < mBuffers.size(); i++) {
        mBuffers.itemAt(i)->release();
    }

    for (size_t i = 0; i < mReleaseBuffers.size(); i++) {
        mReleaseBuffers.itemAt(i)->release();
    }

    mBuffers.clear();
    mReleaseBuffers.clear();
}

MediaBuffer* MediaBufferPuller::getBufferNonBlocking() {
    Mutex::Autolock autolock(mLock);
    if (mBuffers.empty()) {
        return NULL;
    } else {
        MediaBuffer* b = mBuffers.itemAt(0);
        mBuffers.removeAt(0);
        return b;
    }
}

MediaBuffer* MediaBufferPuller::getBufferBlocking() {
    Mutex::Autolock autolock(mLock);
    while (mBuffers.empty() && !mAcquireStopped) {
        mUserCond.wait(mLock);
    }

    if (mBuffers.empty()) {
        return NULL;
    } else {
        MediaBuffer* b = mBuffers.itemAt(0);
        mBuffers.removeAt(0);
        return b;
    }
}

void MediaBufferPuller::putBuffer(MediaBuffer* buffer) {
    Mutex::Autolock autolock(mLock);
    mReleaseBuffers.push(buffer);
    mReleaseCond.signal();
}

int MediaBufferPuller::acquireThreadStart(void* arg) {
    MediaBufferPuller* self = (MediaBufferPuller*)arg;
    self->acquireThreadFunc();
    return 0;
}

int MediaBufferPuller::releaseThreadStart(void* arg) {
    MediaBufferPuller* self = (MediaBufferPuller*)arg;
    self->releaseThreadFunc();
    return 0;
}

void MediaBufferPuller::acquireThreadFunc() {
    mLock.lock();

    // Wait for the start signal
    while (!mAskToStart && !mAskToStop) {
        mAcquireCond.wait(mLock);
    }

    // Loop until we are asked to stop, or there is nothing more to read
    while (!mAskToStop) {
        MediaBuffer* pBuffer;
        mLock.unlock();
        status_t result = mSource->read(&pBuffer, NULL);
        mLock.lock();
        mSourceError = result;
        if (result != OK) {
            break;
        }
        mBuffers.push(pBuffer);
        mUserCond.signal();
    }

    mAcquireStopped = true;
    mUserCond.signal();
    mLock.unlock();
}

void MediaBufferPuller::releaseThreadFunc() {
    mLock.lock();

    // Wait for the start signal
    while (!mAskToStart && !mAskToStop) {
        mReleaseCond.wait(mLock);
    }

    // Loop until we are asked to stop
    while (1) {
        if (mReleaseBuffers.empty()) {
            if (mAskToStop) {
                break;
            } else {
                mReleaseCond.wait(mLock);
                continue;
            }
        }
        MediaBuffer* pBuffer = mReleaseBuffers.itemAt(0);
        mReleaseBuffers.removeAt(0);
        mLock.unlock();
        pBuffer->release();
        mLock.lock();
    }

    mReleaseStopped = true;
    mUserCond.signal();
    mLock.unlock();
}

};  // namespace android

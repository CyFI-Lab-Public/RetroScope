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
#define LOG_TAG "TimedTextDriver"
#include <utils/Log.h>

#include <binder/IPCThreadState.h>

#include <media/mediaplayer.h>
#include <media/MediaPlayerInterface.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/ALooper.h>
#include <media/stagefright/timedtext/TimedTextDriver.h>

#include "TextDescriptions.h"
#include "TimedTextPlayer.h"
#include "TimedTextSource.h"

namespace android {

TimedTextDriver::TimedTextDriver(
        const wp<MediaPlayerBase> &listener)
    : mLooper(new ALooper),
      mListener(listener),
      mState(UNINITIALIZED),
      mCurrentTrackIndex(UINT_MAX) {
    mLooper->setName("TimedTextDriver");
    mLooper->start();
    mPlayer = new TimedTextPlayer(listener);
    mLooper->registerHandler(mPlayer);
}

TimedTextDriver::~TimedTextDriver() {
    mTextSourceVector.clear();
    mTextSourceTypeVector.clear();
    mLooper->stop();
}

status_t TimedTextDriver::selectTrack_l(size_t index) {
    if (mCurrentTrackIndex == index) {
        return OK;
    }
    sp<TimedTextSource> source;
    source = mTextSourceVector.valueFor(index);
    mPlayer->setDataSource(source);
    if (mState == UNINITIALIZED) {
        mState = PREPARED;
    }
    mCurrentTrackIndex = index;
    return OK;
}

status_t TimedTextDriver::start() {
    Mutex::Autolock autoLock(mLock);
    switch (mState) {
        case UNINITIALIZED:
            return INVALID_OPERATION;
        case PLAYING:
            return OK;
        case PREPARED:
            mPlayer->start();
            mState = PLAYING;
            return OK;
        case PAUSED:
            mPlayer->resume();
            mState = PLAYING;
            return OK;
        default:
            TRESPASS();
    }
    return UNKNOWN_ERROR;
}

status_t TimedTextDriver::pause() {
    Mutex::Autolock autoLock(mLock);
    ALOGV("%s() is called", __FUNCTION__);
    switch (mState) {
        case UNINITIALIZED:
            return INVALID_OPERATION;
        case PLAYING:
            mPlayer->pause();
            mState = PAUSED;
            return OK;
        case PREPARED:
            return INVALID_OPERATION;
        case PAUSED:
            return OK;
        default:
            TRESPASS();
    }
    return UNKNOWN_ERROR;
}

status_t TimedTextDriver::selectTrack(size_t index) {
    status_t ret = OK;
    Mutex::Autolock autoLock(mLock);
    ALOGV("%s() is called", __FUNCTION__);
    switch (mState) {
        case UNINITIALIZED:
        case PREPARED:
        case PAUSED:
            ret = selectTrack_l(index);
            break;
        case PLAYING:
            mPlayer->pause();
            ret = selectTrack_l(index);
            if (ret != OK) {
                break;
            }
            mPlayer->start();
            break;
        defaut:
            TRESPASS();
    }
    return ret;
}

status_t TimedTextDriver::unselectTrack(size_t index) {
    Mutex::Autolock autoLock(mLock);
    ALOGV("%s() is called", __FUNCTION__);
    if (mCurrentTrackIndex != index) {
        return INVALID_OPERATION;
    }
    mCurrentTrackIndex = UINT_MAX;
    switch (mState) {
        case UNINITIALIZED:
            return INVALID_OPERATION;
        case PLAYING:
            mPlayer->setDataSource(NULL);
            mState = UNINITIALIZED;
            return OK;
        case PREPARED:
        case PAUSED:
            mState = UNINITIALIZED;
            return OK;
        default:
            TRESPASS();
    }
    return UNKNOWN_ERROR;
}

status_t TimedTextDriver::seekToAsync(int64_t timeUs) {
    Mutex::Autolock autoLock(mLock);
    ALOGV("%s() is called", __FUNCTION__);
    switch (mState) {
        case UNINITIALIZED:
            return INVALID_OPERATION;
        case PREPARED:
            mPlayer->seekToAsync(timeUs);
            mPlayer->pause();
            mState = PAUSED;
            return OK;
        case PAUSED:
            mPlayer->seekToAsync(timeUs);
            mPlayer->pause();
            return OK;
        case PLAYING:
            mPlayer->seekToAsync(timeUs);
            return OK;
        defaut:
            TRESPASS();
    }
    return UNKNOWN_ERROR;
}

status_t TimedTextDriver::addInBandTextSource(
        size_t trackIndex, const sp<MediaSource>& mediaSource) {
    sp<TimedTextSource> source =
            TimedTextSource::CreateTimedTextSource(mediaSource);
    if (source == NULL) {
        return ERROR_UNSUPPORTED;
    }
    Mutex::Autolock autoLock(mLock);
    mTextSourceVector.add(trackIndex, source);
    mTextSourceTypeVector.add(TEXT_SOURCE_TYPE_IN_BAND);
    return OK;
}

status_t TimedTextDriver::addOutOfBandTextSource(
        size_t trackIndex, const char *uri, const char *mimeType) {

    // To support local subtitle file only for now
    if (strncasecmp("file://", uri, 7)) {
        ALOGE("uri('%s') is not a file", uri);
        return ERROR_UNSUPPORTED;
    }

    sp<DataSource> dataSource =
            DataSource::CreateFromURI(uri);
    return createOutOfBandTextSource(trackIndex, mimeType, dataSource);
}

status_t TimedTextDriver::addOutOfBandTextSource(
        size_t trackIndex, int fd, off64_t offset, off64_t length, const char *mimeType) {

    if (fd < 0) {
        ALOGE("Invalid file descriptor: %d", fd);
        return ERROR_UNSUPPORTED;
    }

    sp<DataSource> dataSource = new FileSource(dup(fd), offset, length);
    return createOutOfBandTextSource(trackIndex, mimeType, dataSource);
}

status_t TimedTextDriver::createOutOfBandTextSource(
        size_t trackIndex,
        const char *mimeType,
        const sp<DataSource>& dataSource) {

    if (dataSource == NULL) {
        return ERROR_UNSUPPORTED;
    }

    sp<TimedTextSource> source;
    if (strcasecmp(mimeType, MEDIA_MIMETYPE_TEXT_SUBRIP) == 0) {
        source = TimedTextSource::CreateTimedTextSource(
                dataSource, TimedTextSource::OUT_OF_BAND_FILE_SRT);
    }

    if (source == NULL) {
        ALOGE("Failed to create timed text source");
        return ERROR_UNSUPPORTED;
    }

    Mutex::Autolock autoLock(mLock);
    mTextSourceVector.add(trackIndex, source);
    mTextSourceTypeVector.add(TEXT_SOURCE_TYPE_OUT_OF_BAND);
    return OK;
}

size_t TimedTextDriver::countExternalTracks() const {
    size_t nTracks = 0;
    for (size_t i = 0, n = mTextSourceTypeVector.size(); i < n; ++i) {
        if (mTextSourceTypeVector[i] == TEXT_SOURCE_TYPE_OUT_OF_BAND) {
            ++nTracks;
        }
    }
    return nTracks;
}

void TimedTextDriver::getExternalTrackInfo(Parcel *parcel) {
    Mutex::Autolock autoLock(mLock);
    for (size_t i = 0, n = mTextSourceTypeVector.size(); i < n; ++i) {
        if (mTextSourceTypeVector[i] == TEXT_SOURCE_TYPE_IN_BAND) {
            continue;
        }

        sp<MetaData> meta = mTextSourceVector.valueAt(i)->getFormat();

        // There are two fields.
        parcel->writeInt32(2);

        // track type.
        parcel->writeInt32(MEDIA_TRACK_TYPE_TIMEDTEXT);
        const char *lang = "und";
        if (meta != NULL) {
            meta->findCString(kKeyMediaLanguage, &lang);
        }
        parcel->writeString16(String16(lang));
    }
}

}  // namespace android

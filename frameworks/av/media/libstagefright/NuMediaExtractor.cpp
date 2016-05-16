/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "NuMediaExtractor"
#include <utils/Log.h>

#include <media/stagefright/NuMediaExtractor.h>

#include "include/ESDS.h"
#include "include/NuCachedSource2.h"
#include "include/WVMExtractor.h"

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>

namespace android {

NuMediaExtractor::NuMediaExtractor()
    : mIsWidevineExtractor(false),
      mTotalBitrate(-1ll),
      mDurationUs(-1ll) {
}

NuMediaExtractor::~NuMediaExtractor() {
    releaseTrackSamples();

    for (size_t i = 0; i < mSelectedTracks.size(); ++i) {
        TrackInfo *info = &mSelectedTracks.editItemAt(i);

        CHECK_EQ((status_t)OK, info->mSource->stop());
    }

    mSelectedTracks.clear();
}

status_t NuMediaExtractor::setDataSource(
        const char *path, const KeyedVector<String8, String8> *headers) {
    Mutex::Autolock autoLock(mLock);

    if (mImpl != NULL) {
        return -EINVAL;
    }

    sp<DataSource> dataSource =
        DataSource::CreateFromURI(path, headers);

    if (dataSource == NULL) {
        return -ENOENT;
    }

    mIsWidevineExtractor = false;
    if (!strncasecmp("widevine://", path, 11)) {
        String8 mimeType;
        float confidence;
        sp<AMessage> dummy;
        bool success = SniffWVM(dataSource, &mimeType, &confidence, &dummy);

        if (!success
                || strcasecmp(
                    mimeType.string(), MEDIA_MIMETYPE_CONTAINER_WVM)) {
            return ERROR_UNSUPPORTED;
        }

        sp<WVMExtractor> extractor = new WVMExtractor(dataSource);
        extractor->setAdaptiveStreamingMode(true);

        mImpl = extractor;
        mIsWidevineExtractor = true;
    } else {
        mImpl = MediaExtractor::Create(dataSource);
    }

    if (mImpl == NULL) {
        return ERROR_UNSUPPORTED;
    }

    sp<MetaData> fileMeta = mImpl->getMetaData();
    const char *containerMime;
    if (fileMeta != NULL
            && fileMeta->findCString(kKeyMIMEType, &containerMime)
            && !strcasecmp(containerMime, "video/wvm")) {
        // We always want to use "cryptoPluginMode" when using the wvm
        // extractor. We can tell that it is this extractor by looking
        // at the container mime type.
        // The cryptoPluginMode ensures that the extractor will actually
        // give us data in a call to MediaSource::read(), unlike its
        // default mode that we use from AwesomePlayer.
        static_cast<WVMExtractor *>(mImpl.get())->setCryptoPluginMode(true);
    } else if (mImpl->getDrmFlag()) {
        // For all other drm content, we don't want to expose decrypted
        // content to Java application.
        mImpl.clear();
        mImpl = NULL;
        return ERROR_UNSUPPORTED;
    }

    mDataSource = dataSource;

    updateDurationAndBitrate();

    return OK;
}

status_t NuMediaExtractor::setDataSource(int fd, off64_t offset, off64_t size) {

    Mutex::Autolock autoLock(mLock);

    if (mImpl != NULL) {
        return -EINVAL;
    }

    sp<FileSource> fileSource = new FileSource(dup(fd), offset, size);

    status_t err = fileSource->initCheck();
    if (err != OK) {
        return err;
    }

    mImpl = MediaExtractor::Create(fileSource);

    if (mImpl == NULL) {
        return ERROR_UNSUPPORTED;
    }

    mDataSource = fileSource;

    updateDurationAndBitrate();

    return OK;
}

status_t NuMediaExtractor::setDataSource(const sp<DataSource> &source) {
    Mutex::Autolock autoLock(mLock);

    if (mImpl != NULL) {
        return -EINVAL;
    }

    status_t err = source->initCheck();
    if (err != OK) {
        return err;
    }

    mImpl = MediaExtractor::Create(source);

    if (mImpl == NULL) {
        return ERROR_UNSUPPORTED;
    }

    mDataSource = source;

    updateDurationAndBitrate();

    return OK;
}

void NuMediaExtractor::updateDurationAndBitrate() {
    mTotalBitrate = 0ll;
    mDurationUs = -1ll;

    for (size_t i = 0; i < mImpl->countTracks(); ++i) {
        sp<MetaData> meta = mImpl->getTrackMetaData(i);

        int32_t bitrate;
        if (!meta->findInt32(kKeyBitRate, &bitrate)) {
            const char *mime;
            CHECK(meta->findCString(kKeyMIMEType, &mime));
            ALOGV("track of type '%s' does not publish bitrate", mime);

            mTotalBitrate = -1ll;
        } else if (mTotalBitrate >= 0ll) {
            mTotalBitrate += bitrate;
        }

        int64_t durationUs;
        if (meta->findInt64(kKeyDuration, &durationUs)
                && durationUs > mDurationUs) {
            mDurationUs = durationUs;
        }
    }
}

size_t NuMediaExtractor::countTracks() const {
    Mutex::Autolock autoLock(mLock);

    return mImpl == NULL ? 0 : mImpl->countTracks();
}

status_t NuMediaExtractor::getTrackFormat(
        size_t index, sp<AMessage> *format) const {
    Mutex::Autolock autoLock(mLock);

    *format = NULL;

    if (mImpl == NULL) {
        return -EINVAL;
    }

    if (index >= mImpl->countTracks()) {
        return -ERANGE;
    }

    sp<MetaData> meta = mImpl->getTrackMetaData(index);
    return convertMetaDataToMessage(meta, format);
}

status_t NuMediaExtractor::getFileFormat(sp<AMessage> *format) const {
    Mutex::Autolock autoLock(mLock);

    *format = NULL;

    if (mImpl == NULL) {
        return -EINVAL;
    }

    sp<MetaData> meta = mImpl->getMetaData();

    const char *mime;
    CHECK(meta->findCString(kKeyMIMEType, &mime));
    *format = new AMessage();
    (*format)->setString("mime", mime);

    uint32_t type;
    const void *pssh;
    size_t psshsize;
    if (meta->findData(kKeyPssh, &type, &pssh, &psshsize)) {
        sp<ABuffer> buf = new ABuffer(psshsize);
        memcpy(buf->data(), pssh, psshsize);
        (*format)->setBuffer("pssh", buf);
    }

    return OK;
}

status_t NuMediaExtractor::selectTrack(size_t index) {
    Mutex::Autolock autoLock(mLock);

    if (mImpl == NULL) {
        return -EINVAL;
    }

    if (index >= mImpl->countTracks()) {
        return -ERANGE;
    }

    for (size_t i = 0; i < mSelectedTracks.size(); ++i) {
        TrackInfo *info = &mSelectedTracks.editItemAt(i);

        if (info->mTrackIndex == index) {
            // This track has already been selected.
            return OK;
        }
    }

    sp<MediaSource> source = mImpl->getTrack(index);

    CHECK_EQ((status_t)OK, source->start());

    mSelectedTracks.push();
    TrackInfo *info = &mSelectedTracks.editItemAt(mSelectedTracks.size() - 1);

    info->mSource = source;
    info->mTrackIndex = index;
    info->mFinalResult = OK;
    info->mSample = NULL;
    info->mSampleTimeUs = -1ll;
    info->mTrackFlags = 0;

    const char *mime;
    CHECK(source->getFormat()->findCString(kKeyMIMEType, &mime));

    if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_VORBIS)) {
        info->mTrackFlags |= kIsVorbis;
    }

    return OK;
}

status_t NuMediaExtractor::unselectTrack(size_t index) {
    Mutex::Autolock autoLock(mLock);

    if (mImpl == NULL) {
        return -EINVAL;
    }

    if (index >= mImpl->countTracks()) {
        return -ERANGE;
    }

    size_t i;
    for (i = 0; i < mSelectedTracks.size(); ++i) {
        TrackInfo *info = &mSelectedTracks.editItemAt(i);

        if (info->mTrackIndex == index) {
            break;
        }
    }

    if (i == mSelectedTracks.size()) {
        // Not selected.
        return OK;
    }

    TrackInfo *info = &mSelectedTracks.editItemAt(i);

    if (info->mSample != NULL) {
        info->mSample->release();
        info->mSample = NULL;

        info->mSampleTimeUs = -1ll;
    }

    CHECK_EQ((status_t)OK, info->mSource->stop());

    mSelectedTracks.removeAt(i);

    return OK;
}

void NuMediaExtractor::releaseTrackSamples() {
    for (size_t i = 0; i < mSelectedTracks.size(); ++i) {
        TrackInfo *info = &mSelectedTracks.editItemAt(i);

        if (info->mSample != NULL) {
            info->mSample->release();
            info->mSample = NULL;

            info->mSampleTimeUs = -1ll;
        }
    }
}

ssize_t NuMediaExtractor::fetchTrackSamples(
        int64_t seekTimeUs, MediaSource::ReadOptions::SeekMode mode) {
    TrackInfo *minInfo = NULL;
    ssize_t minIndex = -1;

    for (size_t i = 0; i < mSelectedTracks.size(); ++i) {
        TrackInfo *info = &mSelectedTracks.editItemAt(i);

        if (seekTimeUs >= 0ll) {
            info->mFinalResult = OK;

            if (info->mSample != NULL) {
                info->mSample->release();
                info->mSample = NULL;
                info->mSampleTimeUs = -1ll;
            }
        } else if (info->mFinalResult != OK) {
            continue;
        }

        if (info->mSample == NULL) {
            MediaSource::ReadOptions options;
            if (seekTimeUs >= 0ll) {
                options.setSeekTo(seekTimeUs, mode);
            }
            status_t err = info->mSource->read(&info->mSample, &options);

            if (err != OK) {
                CHECK(info->mSample == NULL);

                info->mFinalResult = err;

                if (info->mFinalResult != ERROR_END_OF_STREAM) {
                    ALOGW("read on track %d failed with error %d",
                          info->mTrackIndex, err);
                }

                info->mSampleTimeUs = -1ll;
                continue;
            } else {
                CHECK(info->mSample != NULL);
                CHECK(info->mSample->meta_data()->findInt64(
                            kKeyTime, &info->mSampleTimeUs));
            }
        }

        if (minInfo == NULL  || info->mSampleTimeUs < minInfo->mSampleTimeUs) {
            minInfo = info;
            minIndex = i;
        }
    }

    return minIndex;
}

status_t NuMediaExtractor::seekTo(
        int64_t timeUs, MediaSource::ReadOptions::SeekMode mode) {
    Mutex::Autolock autoLock(mLock);

    ssize_t minIndex = fetchTrackSamples(timeUs, mode);

    if (minIndex < 0) {
        return ERROR_END_OF_STREAM;
    }

    return OK;
}

status_t NuMediaExtractor::advance() {
    Mutex::Autolock autoLock(mLock);

    ssize_t minIndex = fetchTrackSamples();

    if (minIndex < 0) {
        return ERROR_END_OF_STREAM;
    }

    TrackInfo *info = &mSelectedTracks.editItemAt(minIndex);

    info->mSample->release();
    info->mSample = NULL;
    info->mSampleTimeUs = -1ll;

    return OK;
}

status_t NuMediaExtractor::readSampleData(const sp<ABuffer> &buffer) {
    Mutex::Autolock autoLock(mLock);

    ssize_t minIndex = fetchTrackSamples();

    if (minIndex < 0) {
        return ERROR_END_OF_STREAM;
    }

    TrackInfo *info = &mSelectedTracks.editItemAt(minIndex);

    size_t sampleSize = info->mSample->range_length();

    if (info->mTrackFlags & kIsVorbis) {
        // Each sample's data is suffixed by the number of page samples
        // or -1 if not available.
        sampleSize += sizeof(int32_t);
    }

    if (buffer->capacity() < sampleSize) {
        return -ENOMEM;
    }

    const uint8_t *src =
        (const uint8_t *)info->mSample->data()
            + info->mSample->range_offset();

    memcpy((uint8_t *)buffer->data(), src, info->mSample->range_length());

    if (info->mTrackFlags & kIsVorbis) {
        int32_t numPageSamples;
        if (!info->mSample->meta_data()->findInt32(
                    kKeyValidSamples, &numPageSamples)) {
            numPageSamples = -1;
        }

        memcpy((uint8_t *)buffer->data() + info->mSample->range_length(),
               &numPageSamples,
               sizeof(numPageSamples));
    }

    buffer->setRange(0, sampleSize);

    return OK;
}

status_t NuMediaExtractor::getSampleTrackIndex(size_t *trackIndex) {
    Mutex::Autolock autoLock(mLock);

    ssize_t minIndex = fetchTrackSamples();

    if (minIndex < 0) {
        return ERROR_END_OF_STREAM;
    }

    TrackInfo *info = &mSelectedTracks.editItemAt(minIndex);
    *trackIndex = info->mTrackIndex;

    return OK;
}

status_t NuMediaExtractor::getSampleTime(int64_t *sampleTimeUs) {
    Mutex::Autolock autoLock(mLock);

    ssize_t minIndex = fetchTrackSamples();

    if (minIndex < 0) {
        return ERROR_END_OF_STREAM;
    }

    TrackInfo *info = &mSelectedTracks.editItemAt(minIndex);
    *sampleTimeUs = info->mSampleTimeUs;

    return OK;
}

status_t NuMediaExtractor::getSampleMeta(sp<MetaData> *sampleMeta) {
    Mutex::Autolock autoLock(mLock);

    *sampleMeta = NULL;

    ssize_t minIndex = fetchTrackSamples();

    if (minIndex < 0) {
        return ERROR_END_OF_STREAM;
    }

    TrackInfo *info = &mSelectedTracks.editItemAt(minIndex);
    *sampleMeta = info->mSample->meta_data();

    return OK;
}

bool NuMediaExtractor::getTotalBitrate(int64_t *bitrate) const {
    if (mTotalBitrate >= 0) {
        *bitrate = mTotalBitrate;
        return true;
    }

    off64_t size;
    if (mDurationUs >= 0 && mDataSource->getSize(&size) == OK) {
        *bitrate = size * 8000000ll / mDurationUs;  // in bits/sec
        return true;
    }

    return false;
}

// Returns true iff cached duration is available/applicable.
bool NuMediaExtractor::getCachedDuration(
        int64_t *durationUs, bool *eos) const {
    Mutex::Autolock autoLock(mLock);

    int64_t bitrate;
    if (mIsWidevineExtractor) {
        sp<WVMExtractor> wvmExtractor =
            static_cast<WVMExtractor *>(mImpl.get());

        status_t finalStatus;
        *durationUs = wvmExtractor->getCachedDurationUs(&finalStatus);
        *eos = (finalStatus != OK);
        return true;
    } else if ((mDataSource->flags() & DataSource::kIsCachingDataSource)
            && getTotalBitrate(&bitrate)) {
        sp<NuCachedSource2> cachedSource =
            static_cast<NuCachedSource2 *>(mDataSource.get());

        status_t finalStatus;
        size_t cachedDataRemaining =
            cachedSource->approxDataRemaining(&finalStatus);

        *durationUs = cachedDataRemaining * 8000000ll / bitrate;
        *eos = (finalStatus != OK);
        return true;
    }

    return false;
}

}  // namespace android

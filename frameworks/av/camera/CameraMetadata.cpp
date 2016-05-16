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

// #define LOG_NDEBUG 0

#define LOG_TAG "Camera2-Metadata"
#include <utils/Log.h>
#include <utils/Errors.h>

#include <camera/CameraMetadata.h>
#include <binder/Parcel.h>

namespace android {

typedef Parcel::WritableBlob WritableBlob;
typedef Parcel::ReadableBlob ReadableBlob;

CameraMetadata::CameraMetadata() :
        mBuffer(NULL), mLocked(false) {
}

CameraMetadata::CameraMetadata(size_t entryCapacity, size_t dataCapacity) :
        mLocked(false)
{
    mBuffer = allocate_camera_metadata(entryCapacity, dataCapacity);
}

CameraMetadata::CameraMetadata(const CameraMetadata &other) :
        mLocked(false) {
    mBuffer = clone_camera_metadata(other.mBuffer);
}

CameraMetadata::CameraMetadata(camera_metadata_t *buffer) :
        mBuffer(NULL), mLocked(false) {
    acquire(buffer);
}

CameraMetadata &CameraMetadata::operator=(const CameraMetadata &other) {
    return operator=(other.mBuffer);
}

CameraMetadata &CameraMetadata::operator=(const camera_metadata_t *buffer) {
    if (mLocked) {
        ALOGE("%s: Assignment to a locked CameraMetadata!", __FUNCTION__);
        return *this;
    }

    if (CC_LIKELY(buffer != mBuffer)) {
        camera_metadata_t *newBuffer = clone_camera_metadata(buffer);
        clear();
        mBuffer = newBuffer;
    }
    return *this;
}

CameraMetadata::~CameraMetadata() {
    mLocked = false;
    clear();
}

const camera_metadata_t* CameraMetadata::getAndLock() {
    mLocked = true;
    return mBuffer;
}

status_t CameraMetadata::unlock(const camera_metadata_t *buffer) {
    if (!mLocked) {
        ALOGE("%s: Can't unlock a non-locked CameraMetadata!", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if (buffer != mBuffer) {
        ALOGE("%s: Can't unlock CameraMetadata with wrong pointer!",
                __FUNCTION__);
        return BAD_VALUE;
    }
    mLocked = false;
    return OK;
}

camera_metadata_t* CameraMetadata::release() {
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return NULL;
    }
    camera_metadata_t *released = mBuffer;
    mBuffer = NULL;
    return released;
}

void CameraMetadata::clear() {
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return;
    }
    if (mBuffer) {
        free_camera_metadata(mBuffer);
        mBuffer = NULL;
    }
}

void CameraMetadata::acquire(camera_metadata_t *buffer) {
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return;
    }
    clear();
    mBuffer = buffer;

    ALOGE_IF(validate_camera_metadata_structure(mBuffer, /*size*/NULL) != OK,
             "%s: Failed to validate metadata structure %p",
             __FUNCTION__, buffer);
}

void CameraMetadata::acquire(CameraMetadata &other) {
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return;
    }
    acquire(other.release());
}

status_t CameraMetadata::append(const CameraMetadata &other) {
    return append(other.mBuffer);
}

status_t CameraMetadata::append(const camera_metadata_t* other) {
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    size_t extraEntries = get_camera_metadata_entry_count(other);
    size_t extraData = get_camera_metadata_data_count(other);
    resizeIfNeeded(extraEntries, extraData);

    return append_camera_metadata(mBuffer, other);
}

size_t CameraMetadata::entryCount() const {
    return (mBuffer == NULL) ? 0 :
            get_camera_metadata_entry_count(mBuffer);
}

bool CameraMetadata::isEmpty() const {
    return entryCount() == 0;
}

status_t CameraMetadata::sort() {
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    return sort_camera_metadata(mBuffer);
}

status_t CameraMetadata::checkType(uint32_t tag, uint8_t expectedType) {
    int tagType = get_camera_metadata_tag_type(tag);
    if ( CC_UNLIKELY(tagType == -1)) {
        ALOGE("Update metadata entry: Unknown tag %d", tag);
        return INVALID_OPERATION;
    }
    if ( CC_UNLIKELY(tagType != expectedType) ) {
        ALOGE("Mismatched tag type when updating entry %s (%d) of type %s; "
                "got type %s data instead ",
                get_camera_metadata_tag_name(tag), tag,
                camera_metadata_type_names[tagType],
                camera_metadata_type_names[expectedType]);
        return INVALID_OPERATION;
    }
    return OK;
}

status_t CameraMetadata::update(uint32_t tag,
        const int32_t *data, size_t data_count) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if ( (res = checkType(tag, TYPE_INT32)) != OK) {
        return res;
    }
    return updateImpl(tag, (const void*)data, data_count);
}

status_t CameraMetadata::update(uint32_t tag,
        const uint8_t *data, size_t data_count) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if ( (res = checkType(tag, TYPE_BYTE)) != OK) {
        return res;
    }
    return updateImpl(tag, (const void*)data, data_count);
}

status_t CameraMetadata::update(uint32_t tag,
        const float *data, size_t data_count) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if ( (res = checkType(tag, TYPE_FLOAT)) != OK) {
        return res;
    }
    return updateImpl(tag, (const void*)data, data_count);
}

status_t CameraMetadata::update(uint32_t tag,
        const int64_t *data, size_t data_count) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if ( (res = checkType(tag, TYPE_INT64)) != OK) {
        return res;
    }
    return updateImpl(tag, (const void*)data, data_count);
}

status_t CameraMetadata::update(uint32_t tag,
        const double *data, size_t data_count) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if ( (res = checkType(tag, TYPE_DOUBLE)) != OK) {
        return res;
    }
    return updateImpl(tag, (const void*)data, data_count);
}

status_t CameraMetadata::update(uint32_t tag,
        const camera_metadata_rational_t *data, size_t data_count) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if ( (res = checkType(tag, TYPE_RATIONAL)) != OK) {
        return res;
    }
    return updateImpl(tag, (const void*)data, data_count);
}

status_t CameraMetadata::update(uint32_t tag,
        const String8 &string) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    if ( (res = checkType(tag, TYPE_BYTE)) != OK) {
        return res;
    }
    return updateImpl(tag, (const void*)string.string(), string.size());
}

status_t CameraMetadata::updateImpl(uint32_t tag, const void *data,
        size_t data_count) {
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    int type = get_camera_metadata_tag_type(tag);
    if (type == -1) {
        ALOGE("%s: Tag %d not found", __FUNCTION__, tag);
        return BAD_VALUE;
    }
    size_t data_size = calculate_camera_metadata_entry_data_size(type,
            data_count);

    res = resizeIfNeeded(1, data_size);

    if (res == OK) {
        camera_metadata_entry_t entry;
        res = find_camera_metadata_entry(mBuffer, tag, &entry);
        if (res == NAME_NOT_FOUND) {
            res = add_camera_metadata_entry(mBuffer,
                    tag, data, data_count);
        } else if (res == OK) {
            res = update_camera_metadata_entry(mBuffer,
                    entry.index, data, data_count, NULL);
        }
    }

    if (res != OK) {
        ALOGE("%s: Unable to update metadata entry %s.%s (%x): %s (%d)",
                __FUNCTION__, get_camera_metadata_section_name(tag),
                get_camera_metadata_tag_name(tag), tag, strerror(-res), res);
    }

    IF_ALOGV() {
        ALOGE_IF(validate_camera_metadata_structure(mBuffer, /*size*/NULL) !=
                 OK,

                 "%s: Failed to validate metadata structure after update %p",
                 __FUNCTION__, mBuffer);
    }

    return res;
}

bool CameraMetadata::exists(uint32_t tag) const {
    camera_metadata_ro_entry entry;
    return find_camera_metadata_ro_entry(mBuffer, tag, &entry) == 0;
}

camera_metadata_entry_t CameraMetadata::find(uint32_t tag) {
    status_t res;
    camera_metadata_entry entry;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        entry.count = 0;
        return entry;
    }
    res = find_camera_metadata_entry(mBuffer, tag, &entry);
    if (CC_UNLIKELY( res != OK )) {
        entry.count = 0;
        entry.data.u8 = NULL;
    }
    return entry;
}

camera_metadata_ro_entry_t CameraMetadata::find(uint32_t tag) const {
    status_t res;
    camera_metadata_ro_entry entry;
    res = find_camera_metadata_ro_entry(mBuffer, tag, &entry);
    if (CC_UNLIKELY( res != OK )) {
        entry.count = 0;
        entry.data.u8 = NULL;
    }
    return entry;
}

status_t CameraMetadata::erase(uint32_t tag) {
    camera_metadata_entry_t entry;
    status_t res;
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }
    res = find_camera_metadata_entry(mBuffer, tag, &entry);
    if (res == NAME_NOT_FOUND) {
        return OK;
    } else if (res != OK) {
        ALOGE("%s: Error looking for entry %s.%s (%x): %s %d",
                __FUNCTION__,
                get_camera_metadata_section_name(tag),
                get_camera_metadata_tag_name(tag), tag, strerror(-res), res);
        return res;
    }
    res = delete_camera_metadata_entry(mBuffer, entry.index);
    if (res != OK) {
        ALOGE("%s: Error deleting entry %s.%s (%x): %s %d",
                __FUNCTION__,
                get_camera_metadata_section_name(tag),
                get_camera_metadata_tag_name(tag), tag, strerror(-res), res);
    }
    return res;
}

void CameraMetadata::dump(int fd, int verbosity, int indentation) const {
    dump_indented_camera_metadata(mBuffer, fd, verbosity, indentation);
}

status_t CameraMetadata::resizeIfNeeded(size_t extraEntries, size_t extraData) {
    if (mBuffer == NULL) {
        mBuffer = allocate_camera_metadata(extraEntries * 2, extraData * 2);
        if (mBuffer == NULL) {
            ALOGE("%s: Can't allocate larger metadata buffer", __FUNCTION__);
            return NO_MEMORY;
        }
    } else {
        size_t currentEntryCount = get_camera_metadata_entry_count(mBuffer);
        size_t currentEntryCap = get_camera_metadata_entry_capacity(mBuffer);
        size_t newEntryCount = currentEntryCount +
                extraEntries;
        newEntryCount = (newEntryCount > currentEntryCap) ?
                newEntryCount * 2 : currentEntryCap;

        size_t currentDataCount = get_camera_metadata_data_count(mBuffer);
        size_t currentDataCap = get_camera_metadata_data_capacity(mBuffer);
        size_t newDataCount = currentDataCount +
                extraData;
        newDataCount = (newDataCount > currentDataCap) ?
                newDataCount * 2 : currentDataCap;

        if (newEntryCount > currentEntryCap ||
                newDataCount > currentDataCap) {
            camera_metadata_t *oldBuffer = mBuffer;
            mBuffer = allocate_camera_metadata(newEntryCount,
                    newDataCount);
            if (mBuffer == NULL) {
                ALOGE("%s: Can't allocate larger metadata buffer", __FUNCTION__);
                return NO_MEMORY;
            }
            append_camera_metadata(mBuffer, oldBuffer);
            free_camera_metadata(oldBuffer);
        }
    }
    return OK;
}

status_t CameraMetadata::readFromParcel(const Parcel& data,
                                        camera_metadata_t** out) {

    status_t err = OK;

    camera_metadata_t* metadata = NULL;

    if (out) {
        *out = NULL;
    }

    // arg0 = metadataSize (int32)
    int32_t metadataSizeTmp = -1;
    if ((err = data.readInt32(&metadataSizeTmp)) != OK) {
        ALOGE("%s: Failed to read metadata size (error %d %s)",
              __FUNCTION__, err, strerror(-err));
        return err;
    }
    const size_t metadataSize = static_cast<size_t>(metadataSizeTmp);

    if (metadataSize == 0) {
        ALOGV("%s: Read 0-sized metadata", __FUNCTION__);
        return OK;
    }

    // NOTE: this doesn't make sense to me. shouldnt the blob
    // know how big it is? why do we have to specify the size
    // to Parcel::readBlob ?

    ReadableBlob blob;
    // arg1 = metadata (blob)
    do {
        if ((err = data.readBlob(metadataSize, &blob)) != OK) {
            ALOGE("%s: Failed to read metadata blob (sized %d). Possible "
                  " serialization bug. Error %d %s",
                  __FUNCTION__, metadataSize, err, strerror(-err));
            break;
        }
        const camera_metadata_t* tmp =
                       reinterpret_cast<const camera_metadata_t*>(blob.data());

        metadata = allocate_copy_camera_metadata_checked(tmp, metadataSize);
        if (metadata == NULL) {
            // We consider that allocation only fails if the validation
            // also failed, therefore the readFromParcel was a failure.
            err = BAD_VALUE;
        }
    } while(0);
    blob.release();

    if (out) {
        ALOGV("%s: Set out metadata to %p", __FUNCTION__, metadata);
        *out = metadata;
    } else if (metadata != NULL) {
        ALOGV("%s: Freed camera metadata at %p", __FUNCTION__, metadata);
        free_camera_metadata(metadata);
    }

    return err;
}

status_t CameraMetadata::writeToParcel(Parcel& data,
                                       const camera_metadata_t* metadata) {
    status_t res = OK;

    // arg0 = metadataSize (int32)

    if (metadata == NULL) {
        return data.writeInt32(0);
    }

    const size_t metadataSize = get_camera_metadata_compact_size(metadata);
    res = data.writeInt32(static_cast<int32_t>(metadataSize));
    if (res != OK) {
        return res;
    }

    // arg1 = metadata (blob)
    WritableBlob blob;
    do {
        res = data.writeBlob(metadataSize, &blob);
        if (res != OK) {
            break;
        }
        copy_camera_metadata(blob.data(), metadataSize, metadata);

        IF_ALOGV() {
            if (validate_camera_metadata_structure(
                        (const camera_metadata_t*)blob.data(),
                        &metadataSize) != OK) {
                ALOGV("%s: Failed to validate metadata %p after writing blob",
                       __FUNCTION__, blob.data());
            } else {
                ALOGV("%s: Metadata written to blob. Validation success",
                        __FUNCTION__);
            }
        }

        // Not too big of a problem since receiving side does hard validation
        // Don't check the size since the compact size could be larger
        if (validate_camera_metadata_structure(metadata, /*size*/NULL) != OK) {
            ALOGW("%s: Failed to validate metadata %p before writing blob",
                   __FUNCTION__, metadata);
        }

    } while(false);
    blob.release();

    return res;
}

status_t CameraMetadata::readFromParcel(Parcel *parcel) {

    ALOGV("%s: parcel = %p", __FUNCTION__, parcel);

    status_t res = OK;

    if (parcel == NULL) {
        ALOGE("%s: parcel is null", __FUNCTION__);
        return BAD_VALUE;
    }

    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return INVALID_OPERATION;
    }

    camera_metadata *buffer = NULL;
    // TODO: reading should return a status code, in case validation fails
    res = CameraMetadata::readFromParcel(*parcel, &buffer);

    if (res != NO_ERROR) {
        ALOGE("%s: Failed to read from parcel. Metadata is unchanged.",
              __FUNCTION__);
        return res;
    }

    clear();
    mBuffer = buffer;

    return OK;
}

status_t CameraMetadata::writeToParcel(Parcel *parcel) const {

    ALOGV("%s: parcel = %p", __FUNCTION__, parcel);

    if (parcel == NULL) {
        ALOGE("%s: parcel is null", __FUNCTION__);
        return BAD_VALUE;
    }

    return CameraMetadata::writeToParcel(*parcel, mBuffer);
}

void CameraMetadata::swap(CameraMetadata& other) {
    if (mLocked) {
        ALOGE("%s: CameraMetadata is locked", __FUNCTION__);
        return;
    } else if (other.mLocked) {
        ALOGE("%s: Other CameraMetadata is locked", __FUNCTION__);
        return;
    }

    camera_metadata* thisBuf = mBuffer;
    camera_metadata* otherBuf = other.mBuffer;

    other.mBuffer = thisBuf;
    mBuffer = otherBuf;
}

}; // namespace android

/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include <pthread.h>
#include <system/camera_metadata.h>

//#define LOG_NDEBUG 0
#define LOG_TAG "Metadata"
#include <cutils/log.h>

#define ATRACE_TAG (ATRACE_TAG_CAMERA | ATRACE_TAG_HAL)
#include <cutils/trace.h>
#include "ScopedTrace.h"

#include "Metadata.h"

namespace default_camera_hal {

Metadata::Metadata()
  : mHead(NULL),
    mTail(NULL),
    mEntryCount(0),
    mDataCount(0),
    mGenerated(NULL),
    mDirty(true)
{
    // NULL (default) pthread mutex attributes
    pthread_mutex_init(&mMutex, NULL);
}

Metadata::~Metadata()
{
    Entry *current = mHead;

    while (current != NULL) {
        Entry *tmp = current;
        current = current->mNext;
        delete tmp;
    }

    if (mGenerated != NULL)
        free_camera_metadata(mGenerated);

    pthread_mutex_destroy(&mMutex);
}

Metadata::Metadata(uint8_t mode, uint8_t intent)
  : mHead(NULL),
    mTail(NULL),
    mEntryCount(0),
    mDataCount(0),
    mGenerated(NULL),
    mDirty(true)
{
    pthread_mutex_init(&mMutex, NULL);

    if (validate(ANDROID_CONTROL_MODE, TYPE_BYTE, 1)) {
        int res = add(ANDROID_CONTROL_MODE, 1, &mode);
        if (res != 0) {
            ALOGE("%s: Unable to add mode to template!", __func__);
        }
    } else {
        ALOGE("%s: Invalid mode constructing template!", __func__);
    }

    if (validate(ANDROID_CONTROL_CAPTURE_INTENT, TYPE_BYTE, 1)) {
        int res = add(ANDROID_CONTROL_CAPTURE_INTENT, 1, &intent);
        if (res != 0) {
            ALOGE("%s: Unable to add capture intent to template!", __func__);
        }
    } else {
        ALOGE("%s: Invalid capture intent constructing template!", __func__);
    }
}

int Metadata::addUInt8(uint32_t tag, int count, uint8_t *data)
{
    if (!validate(tag, TYPE_BYTE, count)) return -EINVAL;
    return add(tag, count, data);
}

int Metadata::addInt32(uint32_t tag, int count, int32_t *data)
{
    if (!validate(tag, TYPE_INT32, count)) return -EINVAL;
    return add(tag, count, data);
}

int Metadata::addFloat(uint32_t tag, int count, float *data)
{
    if (!validate(tag, TYPE_FLOAT, count)) return -EINVAL;
    return add(tag, count, data);
}

int Metadata::addInt64(uint32_t tag, int count, int64_t *data)
{
    if (!validate(tag, TYPE_INT64, count)) return -EINVAL;
    return add(tag, count, data);
}

int Metadata::addDouble(uint32_t tag, int count, double *data)
{
    if (!validate(tag, TYPE_DOUBLE, count)) return -EINVAL;
    return add(tag, count, data);
}

int Metadata::addRational(uint32_t tag, int count,
        camera_metadata_rational_t *data)
{
    if (!validate(tag, TYPE_RATIONAL, count)) return -EINVAL;
    return add(tag, count, data);
}

bool Metadata::validate(uint32_t tag, int tag_type, int count)
{
    if (get_camera_metadata_tag_type(tag) < 0) {
        ALOGE("%s: Invalid metadata entry tag: %d", __func__, tag);
        return false;
    }
    if (tag_type < 0 || tag_type >= NUM_TYPES) {
        ALOGE("%s: Invalid metadata entry tag type: %d", __func__, tag_type);
        return false;
    }
    if (tag_type != get_camera_metadata_tag_type(tag)) {
        ALOGE("%s: Tag %d called with incorrect type: %s(%d)", __func__, tag,
                camera_metadata_type_names[tag_type], tag_type);
        return false;
    }
    if (count < 1) {
        ALOGE("%s: Invalid metadata entry count: %d", __func__, count);
        return false;
    }
    return true;
}

int Metadata::add(uint32_t tag, int count, void *tag_data)
{
    int tag_type = get_camera_metadata_tag_type(tag);
    size_t type_sz = camera_metadata_type_size[tag_type];

    // Allocate array to hold new metadata
    void *data = malloc(count * type_sz);
    if (data == NULL)
        return -ENOMEM;
    memcpy(data, tag_data, count * type_sz);

    pthread_mutex_lock(&mMutex);
    mEntryCount++;
    mDataCount += calculate_camera_metadata_entry_data_size(tag_type, count);
    push(new Entry(tag, data, count));
    mDirty = true;
    pthread_mutex_unlock(&mMutex);
    return 0;
}

camera_metadata_t* Metadata::generate()
{
    pthread_mutex_lock(&mMutex);
    // Reuse if old generated metadata still valid
    if (!mDirty && mGenerated != NULL) {
        ALOGV("%s: Reusing generated metadata at %p", __func__, mGenerated);
        goto out;
    }
    // Destroy old metadata
    if (mGenerated != NULL) {
        ALOGV("%s: Freeing generated metadata at %p", __func__, mGenerated);
        free_camera_metadata(mGenerated);
        mGenerated = NULL;
    }
    // Generate new metadata structure
    ALOGV("%s: Generating new camera metadata structure, Entries:%d Data:%d",
            __func__, mEntryCount, mDataCount);
    mGenerated = allocate_camera_metadata(mEntryCount, mDataCount);
    if (mGenerated == NULL) {
        ALOGE("%s: Failed to allocate metadata (%d entries %d data)",
                __func__, mEntryCount, mDataCount);
        goto out;
    }
    // Walk list of entries adding each one to newly allocated metadata
    for (Entry *current = mHead; current != NULL; current = current->mNext) {
        int res = add_camera_metadata_entry(mGenerated, current->mTag,
                current->mData, current->mCount);
        if (res != 0) {
            ALOGE("%s: Failed to add camera metadata: %d", __func__, res);
            free_camera_metadata(mGenerated);
            mGenerated = NULL;
            goto out;
        }
    }

out:
    pthread_mutex_unlock(&mMutex);
    return mGenerated;
}

Metadata::Entry::Entry(uint32_t tag, void *data, int count)
  : mNext(NULL),
    mPrev(NULL),
    mTag(tag),
    mData(data),
    mCount(count)
{
}

void Metadata::push(Entry *e)
{
    if (mHead == NULL) {
        mHead = mTail = e;
    } else {
        mTail->insertAfter(e);
        mTail = e;
    }
}

Metadata::Entry::~Entry()
{
    if (mNext != NULL)
        mNext->mPrev = mPrev;
    if (mPrev != NULL)
        mPrev->mNext = mNext;
}

void Metadata::Entry::insertAfter(Entry *e)
{
    if (e == NULL)
        return;
    if (mNext != NULL)
        mNext->mPrev = e;
    e->mNext = mNext;
    e->mPrev = this;
    mNext = e;
}

} // namespace default_camera_hal

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

#ifndef METADATA_H_
#define METADATA_H_

#include <hardware/camera3.h>
#include <hardware/gralloc.h>
#include <system/camera_metadata.h>
#include <system/graphics.h>

namespace default_camera_hal {
// Metadata is a convenience class for dealing with libcamera_metadata
class Metadata {
    public:
        Metadata();
        ~Metadata();
        // Constructor used for request metadata templates
        Metadata(uint8_t mode, uint8_t intent);

        // Parse and add an entry
        int addUInt8(uint32_t tag, int count, uint8_t *data);
        int addInt32(uint32_t tag, int count, int32_t *data);
        int addFloat(uint32_t tag, int count, float *data);
        int addInt64(uint32_t tag, int count, int64_t *data);
        int addDouble(uint32_t tag, int count, double *data);
        int addRational(uint32_t tag, int count,
                camera_metadata_rational_t *data);
        // Generate a camera_metadata structure and fill it with internal data
        camera_metadata_t *generate();

    private:
        // Validate the tag, type and count for a metadata entry
        bool validate(uint32_t tag, int tag_type, int count);
        // Add a verified tag with data to this Metadata structure
        int add(uint32_t tag, int count, void *tag_data);

        class Entry {
            public:
                Entry(uint32_t tag, void *data, int count);
                ~Entry();
                Entry *mNext;
                Entry *mPrev;
                const uint32_t mTag;
                const void *mData;
                const int mCount;
                void insertAfter(Entry *e);
        };
        // List ends
        Entry *mHead;
        Entry *mTail;
        // Append entry to list
        void push(Entry *e);
        // Total of entries and entry data size
        int mEntryCount;
        int mDataCount;
        // Save generated metadata, invalidated on update
        camera_metadata_t *mGenerated;
        // Flag to force metadata regeneration
        bool mDirty;
        // Lock protecting the Metadata object for modifications
        pthread_mutex_t mMutex;
};
} // namespace default_camera_hal

#endif // METADATA_H_

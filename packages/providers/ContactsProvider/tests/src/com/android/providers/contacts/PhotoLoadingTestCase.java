/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.providers.contacts;

import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.collect.Maps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Adds support for loading photo files easily from test resources.
 */
@SmallTest
public class PhotoLoadingTestCase extends AndroidTestCase {

    private Map<Integer, PhotoEntry> photoResourceCache = Maps.newHashMap();
    protected static enum PhotoSize {
        ORIGINAL,
        DISPLAY_PHOTO,
        THUMBNAIL
    }

    protected final class PhotoEntry {
        Map<PhotoSize, byte[]> photoMap = Maps.newHashMap();
        public PhotoEntry(byte[] original) {
            try {
                PhotoProcessor processor = newPhotoProcessor(original, false);
                photoMap.put(PhotoSize.ORIGINAL, original);
                photoMap.put(PhotoSize.DISPLAY_PHOTO, processor.getDisplayPhotoBytes());
                photoMap.put(PhotoSize.THUMBNAIL, processor.getThumbnailPhotoBytes());
            } catch (IOException ignored) {
                // Test is probably going to fail as a result anyway.
            }
        }

        public byte[] getPhoto(PhotoSize size) {
            return photoMap.get(size);
        }
    }

    // The test photo will be loaded frequently in tests, so we'll just process it once.
    private static PhotoEntry testPhotoEntry;

    /**
     * Create a new {@link PhotoProcessor} for unit tests.
     *
     * The instance generated here is always configured for 256x256 regardless of the
     * device memory size.
     */
    protected PhotoProcessor newPhotoProcessor(byte[] data, boolean forceCropToSquare)
            throws IOException {
        return new PhotoProcessor(data, 256, 96, forceCropToSquare);
    }

    protected byte[] loadTestPhoto() {
        int testPhotoId = com.android.providers.contacts.tests.R.drawable.ic_contact_picture;
        if (testPhotoEntry == null) {
            loadPhotoFromResource(testPhotoId, PhotoSize.ORIGINAL);
            testPhotoEntry = photoResourceCache.get(testPhotoId);
        }
        return testPhotoEntry.getPhoto(PhotoSize.ORIGINAL);
    }

    protected byte[] loadTestPhoto(PhotoSize size) {
        loadTestPhoto();
        return testPhotoEntry.getPhoto(size);
    }

    protected byte[] loadPhotoFromResource(int resourceId, PhotoSize size) {
        PhotoEntry entry = photoResourceCache.get(resourceId);
        if (entry == null) {
            final Resources resources = getTestContext().getResources();
            InputStream is = resources.openRawResource(resourceId);
            byte[] content = readInputStreamFully(is);
            entry = new PhotoEntry(content);
            photoResourceCache.put(resourceId, entry);
        }
        return entry.getPhoto(size);
    }

    protected byte[] readInputStreamFully(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[10000];
        int count;
        try {
            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return os.toByteArray();
    }
}

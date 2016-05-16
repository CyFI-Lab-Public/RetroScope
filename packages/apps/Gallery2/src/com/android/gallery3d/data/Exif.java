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

package com.android.gallery3d.data;

import android.util.Log;

import com.android.gallery3d.exif.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

public class Exif {
    private static final String TAG = "GalleryExif";

    /**
     * Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
     */
    public static int getOrientation(InputStream is) {
        if (is == null) {
            return 0;
        }
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(is);
            Integer val = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            if (val == null) {
                return 0;
            } else {
                return ExifInterface.getRotationForOrientationValue(val.shortValue());
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to read EXIF orientation", e);
            return 0;
        }
    }

    /**
     * Returns an exif interface instance for the given JPEG image.
     *
     * @param jpegData a valid JPEG image containing EXIF data
     */
    public static ExifInterface getExif(byte[] jpegData) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(jpegData);
        } catch (IOException e) {
            Log.w(TAG, "Failed to read EXIF data", e);
        }
        return exif;
    }

    /**
     * Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
     */
    public static int getOrientation(ExifInterface exif) {
        Integer val = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        if (val == null) {
            return 0;
        } else {
            return ExifInterface.getRotationForOrientationValue(val.shortValue());
        }
    }

    /**
     * See {@link #getOrientation(byte[])}, but using the picture bytes instead.
     */
    public static int getOrientation(byte[] jpegData) {
        if (jpegData == null)
            return 0;

        ExifInterface exif = getExif(jpegData);
        return getOrientation(exif);
    }
}

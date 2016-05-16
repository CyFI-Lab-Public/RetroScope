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

package com.android.gallery3d.ingest.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.android.gallery3d.data.Exif;
import com.android.photos.data.GalleryBitmapPool;

public class MtpBitmapFetch {
    private static int sMaxSize = 0;

    public static void recycleThumbnail(Bitmap b) {
        if (b != null) {
            GalleryBitmapPool.getInstance().put(b);
        }
    }

    public static Bitmap getThumbnail(MtpDevice device, MtpObjectInfo info) {
        byte[] imageBytes = device.getThumbnail(info.getObjectHandle());
        if (imageBytes == null) {
            return null;
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
        if (o.outWidth == 0 || o.outHeight == 0) {
            return null;
        }
        o.inBitmap = GalleryBitmapPool.getInstance().get(o.outWidth, o.outHeight);
        o.inMutable = true;
        o.inJustDecodeBounds = false;
        o.inSampleSize = 1;
        try {
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
        } catch (IllegalArgumentException e) {
            // BitmapFactory throws an exception rather than returning null
            // when image decoding fails and an existing bitmap was supplied
            // for recycling, even if the failure was not caused by the use
            // of that bitmap.
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }
    }

    public static BitmapWithMetadata getFullsize(MtpDevice device, MtpObjectInfo info) {
        return getFullsize(device, info, sMaxSize);
    }

    public static BitmapWithMetadata getFullsize(MtpDevice device, MtpObjectInfo info, int maxSide) {
        byte[] imageBytes = device.getObject(info.getObjectHandle(), info.getCompressedSize());
        if (imageBytes == null) {
            return null;
        }
        Bitmap created;
        if (maxSide > 0) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
            int w = o.outWidth;
            int h = o.outHeight;
            int comp = Math.max(h, w);
            int sampleSize = 1;
            while ((comp >> 1) >= maxSide) {
                comp = comp >> 1;
                sampleSize++;
            }
            o.inSampleSize = sampleSize;
            o.inJustDecodeBounds = false;
            created = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
        } else {
            created = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }
        if (created == null) {
            return null;
        }

        return new BitmapWithMetadata(created, Exif.getOrientation(imageBytes));
    }

    public static void configureForContext(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        sMaxSize = Math.max(metrics.heightPixels, metrics.widthPixels);
    }
}

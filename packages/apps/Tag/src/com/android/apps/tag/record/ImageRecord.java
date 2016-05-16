/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag.record;

import com.android.apps.tag.R;
import com.google.common.base.Preconditions;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NdefRecord;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

/**
 * A NdefRecord corresponding to an image type.
 */
public class ImageRecord extends ParsedNdefRecord {

    public static final String RECORD_TYPE = "ImageRecord";

    private final Bitmap mBitmap;

    private ImageRecord(Bitmap bitmap) {
        mBitmap = Preconditions.checkNotNull(bitmap);
    }

    @Override
    public View getView(Activity activity, LayoutInflater inflater, ViewGroup parent, int offset) {
        ImageView image = (ImageView) inflater.inflate(R.layout.tag_image, parent, false);
        image.setImageBitmap(mBitmap);
        return image;
    }

    public static ImageRecord parse(NdefRecord record) {
        String mimeType = record.toMimeType();
        if (mimeType == null) {
            throw new IllegalArgumentException("not a valid image file");
        }
        Preconditions.checkArgument(mimeType.startsWith("image/"));

        // Try to ensure it's a legal, valid image
        byte[] content = record.getPayload();
        Bitmap bitmap = BitmapFactory.decodeByteArray(content, 0, content.length);
        if (bitmap == null) {
            throw new IllegalArgumentException("not a valid image file");
        }
        return new ImageRecord(bitmap);
    }

    public static boolean isImage(NdefRecord record) {
        try {
            parse(record);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static NdefRecord newImageRecord(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        byte[] content = out.toByteArray();
        return NdefRecord.createMime("image/jpeg", content);
    }
}

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

package com.android.photos.data;

import android.content.Context;
import android.content.CursorLoader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;

import com.android.photos.drawables.DataUriThumbnailDrawable;
import com.android.photos.shims.LoaderCompatShim;

import java.util.ArrayList;

public class PhotoSetLoader extends CursorLoader implements LoaderCompatShim<Cursor> {

    public static final String SUPPORTED_OPERATIONS = "supported_operations";

    private static final Uri CONTENT_URI = Files.getContentUri("external");
    public static final String[] PROJECTION = new String[] {
        FileColumns._ID,
        FileColumns.DATA,
        FileColumns.WIDTH,
        FileColumns.HEIGHT,
        FileColumns.DATE_ADDED,
        FileColumns.MEDIA_TYPE,
        SUPPORTED_OPERATIONS,
    };

    private static final String SORT_ORDER = FileColumns.DATE_ADDED + " DESC";
    private static final String SELECTION =
            FileColumns.MEDIA_TYPE + " == " + FileColumns.MEDIA_TYPE_IMAGE
            + " OR "
            + FileColumns.MEDIA_TYPE + " == " + FileColumns.MEDIA_TYPE_VIDEO;

    public static final int INDEX_ID = 0;
    public static final int INDEX_DATA = 1;
    public static final int INDEX_WIDTH = 2;
    public static final int INDEX_HEIGHT = 3;
    public static final int INDEX_DATE_ADDED = 4;
    public static final int INDEX_MEDIA_TYPE = 5;
    public static final int INDEX_SUPPORTED_OPERATIONS = 6;

    private static final Uri GLOBAL_CONTENT_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/external/");
    private final ContentObserver mGlobalObserver = new ForceLoadContentObserver();

    public PhotoSetLoader(Context context) {
        super(context, CONTENT_URI, PROJECTION, SELECTION, null, SORT_ORDER);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        getContext().getContentResolver().registerContentObserver(GLOBAL_CONTENT_URI,
                true, mGlobalObserver);
    }

    @Override
    protected void onReset() {
        super.onReset();
        getContext().getContentResolver().unregisterContentObserver(mGlobalObserver);
    }

    @Override
    public Drawable drawableForItem(Cursor item, Drawable recycle) {
        DataUriThumbnailDrawable drawable = null;
        if (recycle == null || !(recycle instanceof DataUriThumbnailDrawable)) {
            drawable = new DataUriThumbnailDrawable();
        } else {
            drawable = (DataUriThumbnailDrawable) recycle;
        }
        drawable.setImage(item.getString(INDEX_DATA),
                item.getInt(INDEX_WIDTH), item.getInt(INDEX_HEIGHT));
        return drawable;
    }

    @Override
    public Uri uriForItem(Cursor item) {
        return null;
    }

    @Override
    public ArrayList<Uri> urisForSubItems(Cursor item) {
        return null;
    }

    @Override
    public void deleteItemWithPath(Object path) {

    }

    @Override
    public Object getPathForItem(Cursor item) {
        return null;
    }
}

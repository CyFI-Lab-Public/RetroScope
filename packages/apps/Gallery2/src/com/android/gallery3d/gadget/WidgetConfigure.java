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

package com.android.gallery3d.gadget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AlbumPicker;
import com.android.gallery3d.app.DialogPicker;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.filtershow.crop.CropExtras;

public class WidgetConfigure extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "WidgetConfigure";

    public static final String KEY_WIDGET_TYPE = "widget-type";
    private static final String KEY_PICKED_ITEM = "picked-item";

    private static final int REQUEST_WIDGET_TYPE = 1;
    private static final int REQUEST_CHOOSE_ALBUM = 2;
    private static final int REQUEST_CROP_IMAGE = 3;
    private static final int REQUEST_GET_PHOTO = 4;

    public static final int RESULT_ERROR = RESULT_FIRST_USER;

    // Scale up the widget size since we only specified the minimized
    // size of the gadget. The real size could be larger.
    // Note: There is also a limit on the size of data that can be
    // passed in Binder's transaction.
    private static float WIDGET_SCALE_FACTOR = 1.5f;
    private static int MAX_WIDGET_SIDE = 360;

    private int mAppWidgetId = -1;
    private Uri mPickedItem;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        if (mAppWidgetId == -1) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        if (savedState == null) {
            if (ApiHelper.HAS_REMOTE_VIEWS_SERVICE) {
                Intent intent = new Intent(this, WidgetTypeChooser.class);
                startActivityForResult(intent, REQUEST_WIDGET_TYPE);
            } else { // Choose the photo type widget
                setWidgetType(new Intent()
                        .putExtra(KEY_WIDGET_TYPE, R.id.widget_type_photo));
            }
        } else {
            mPickedItem = savedState.getParcelable(KEY_PICKED_ITEM);
        }
    }

    protected void onSaveInstanceStates(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PICKED_ITEM, mPickedItem);
    }

    private void updateWidgetAndFinish(WidgetDatabaseHelper.Entry entry) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews views = PhotoAppWidgetProvider.buildWidget(this, mAppWidgetId, entry);
        manager.updateAppWidget(mAppWidgetId, views);
        setResult(RESULT_OK, new Intent().putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            setResult(resultCode, new Intent().putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
            finish();
            return;
        }

        if (requestCode == REQUEST_WIDGET_TYPE) {
            setWidgetType(data);
        } else if (requestCode == REQUEST_CHOOSE_ALBUM) {
            setChoosenAlbum(data);
        } else if (requestCode == REQUEST_GET_PHOTO) {
            setChoosenPhoto(data);
        } else if (requestCode == REQUEST_CROP_IMAGE) {
            setPhotoWidget(data);
        } else {
            throw new AssertionError("unknown request: " + requestCode);
        }
    }

    private void setPhotoWidget(Intent data) {
        // Store the cropped photo in our database
        Bitmap bitmap = (Bitmap) data.getParcelableExtra("data");
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
        try {
            helper.setPhoto(mAppWidgetId, mPickedItem, bitmap);
            updateWidgetAndFinish(helper.getEntry(mAppWidgetId));
        } finally {
            helper.close();
        }
    }

    private void setChoosenPhoto(Intent data) {
        Resources res = getResources();

        float width = res.getDimension(R.dimen.appwidget_width);
        float height = res.getDimension(R.dimen.appwidget_height);

        // We try to crop a larger image (by scale factor), but there is still
        // a bound on the binder limit.
        float scale = Math.min(WIDGET_SCALE_FACTOR,
                MAX_WIDGET_SIDE / Math.max(width, height));

        int widgetWidth = Math.round(width * scale);
        int widgetHeight = Math.round(height * scale);

        mPickedItem = data.getData();
        Intent request = new Intent(CropActivity.CROP_ACTION, mPickedItem)
                .putExtra(CropExtras.KEY_OUTPUT_X, widgetWidth)
                .putExtra(CropExtras.KEY_OUTPUT_Y, widgetHeight)
                .putExtra(CropExtras.KEY_ASPECT_X, widgetWidth)
                .putExtra(CropExtras.KEY_ASPECT_Y, widgetHeight)
                .putExtra(CropExtras.KEY_SCALE_UP_IF_NEEDED, true)
                .putExtra(CropExtras.KEY_SCALE, true)
                .putExtra(CropExtras.KEY_RETURN_DATA, true);
        startActivityForResult(request, REQUEST_CROP_IMAGE);
    }

    private void setChoosenAlbum(Intent data) {
        String albumPath = data.getStringExtra(AlbumPicker.KEY_ALBUM_PATH);
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
        try {
            String relativePath = null;
            GalleryApp galleryApp = (GalleryApp) getApplicationContext();
            DataManager manager = galleryApp.getDataManager();
            Path path = Path.fromString(albumPath);
            MediaSet mediaSet = (MediaSet) manager.getMediaObject(path);
            if (mediaSet instanceof LocalAlbum) {
                int bucketId = Integer.parseInt(path.getSuffix());
                // If the chosen album is a local album, find relative path
                // Otherwise, leave the relative path field empty
                relativePath = LocalAlbum.getRelativePath(bucketId);
                Log.i(TAG, "Setting widget, album path: " + albumPath
                        + ", relative path: " + relativePath);
            }
            helper.setWidget(mAppWidgetId,
                    WidgetDatabaseHelper.TYPE_ALBUM, albumPath, relativePath);
            updateWidgetAndFinish(helper.getEntry(mAppWidgetId));
        } finally {
            helper.close();
        }
    }

    private void setWidgetType(Intent data) {
        int widgetType = data.getIntExtra(KEY_WIDGET_TYPE, R.id.widget_type_shuffle);
        if (widgetType == R.id.widget_type_album) {
            Intent intent = new Intent(this, AlbumPicker.class);
            startActivityForResult(intent, REQUEST_CHOOSE_ALBUM);
        } else if (widgetType == R.id.widget_type_shuffle) {
            WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
            try {
                helper.setWidget(mAppWidgetId, WidgetDatabaseHelper.TYPE_SHUFFLE, null, null);
                updateWidgetAndFinish(helper.getEntry(mAppWidgetId));
            } finally {
                helper.close();
            }
        } else {
            // Explicitly send the intent to the DialogPhotoPicker
            Intent request = new Intent(this, DialogPicker.class)
                    .setAction(Intent.ACTION_GET_CONTENT)
                    .setType("image/*");
            startActivityForResult(request, REQUEST_GET_PHOTO);
        }
    }
}

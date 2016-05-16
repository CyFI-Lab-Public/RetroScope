/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.filtershow.crop.CropExtras;

import java.lang.IllegalArgumentException;

/**
 * Wallpaper picker for the gallery application. This just redirects to the
 * standard pick action.
 */
public class Wallpaper extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "Wallpaper";

    private static final String IMAGE_TYPE = "image/*";
    private static final String KEY_STATE = "activity-state";
    private static final String KEY_PICKED_ITEM = "picked-item";

    private static final int STATE_INIT = 0;
    private static final int STATE_PHOTO_PICKED = 1;

    private int mState = STATE_INIT;
    private Uri mPickedItem;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            mState = bundle.getInt(KEY_STATE);
            mPickedItem = (Uri) bundle.getParcelable(KEY_PICKED_ITEM);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle saveState) {
        saveState.putInt(KEY_STATE, mState);
        if (mPickedItem != null) {
            saveState.putParcelable(KEY_PICKED_ITEM, mPickedItem);
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private Point getDefaultDisplaySize(Point size) {
        Display d = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= ApiHelper.VERSION_CODES.HONEYCOMB_MR2) {
            d.getSize(size);
        } else {
            size.set(d.getWidth(), d.getHeight());
        }
        return size;
    }

    @SuppressWarnings("fallthrough")
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        switch (mState) {
            case STATE_INIT: {
                mPickedItem = intent.getData();
                if (mPickedItem == null) {
                    Intent request = new Intent(Intent.ACTION_GET_CONTENT)
                            .setClass(this, DialogPicker.class)
                            .setType(IMAGE_TYPE);
                    startActivityForResult(request, STATE_PHOTO_PICKED);
                    return;
                }
                mState = STATE_PHOTO_PICKED;
                // fall-through
            }
            case STATE_PHOTO_PICKED: {
                Intent cropAndSetWallpaperIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WallpaperManager wpm = WallpaperManager.getInstance(getApplicationContext());
                    try {
                        cropAndSetWallpaperIntent = wpm.getCropAndSetWallpaperIntent(mPickedItem);
                        startActivity(cropAndSetWallpaperIntent);
                        finish();
                        return;
                    } catch (ActivityNotFoundException anfe) {
                        // ignored; fallthru to existing crop activity
                    } catch (IllegalArgumentException iae) {
                        // ignored; fallthru to existing crop activity
                    }
                }

                int width = getWallpaperDesiredMinimumWidth();
                int height = getWallpaperDesiredMinimumHeight();
                Point size = getDefaultDisplaySize(new Point());
                float spotlightX = (float) size.x / width;
                float spotlightY = (float) size.y / height;
                cropAndSetWallpaperIntent = new Intent(CropActivity.CROP_ACTION)
                    .setClass(this, CropActivity.class)
                    .setDataAndType(mPickedItem, IMAGE_TYPE)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtra(CropExtras.KEY_OUTPUT_X, width)
                    .putExtra(CropExtras.KEY_OUTPUT_Y, height)
                    .putExtra(CropExtras.KEY_ASPECT_X, width)
                    .putExtra(CropExtras.KEY_ASPECT_Y, height)
                    .putExtra(CropExtras.KEY_SPOTLIGHT_X, spotlightX)
                    .putExtra(CropExtras.KEY_SPOTLIGHT_Y, spotlightY)
                    .putExtra(CropExtras.KEY_SCALE, true)
                    .putExtra(CropExtras.KEY_SCALE_UP_IF_NEEDED, true)
                    .putExtra(CropExtras.KEY_SET_AS_WALLPAPER, true);
                startActivity(cropAndSetWallpaperIntent);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            setResult(resultCode);
            finish();
            return;
        }
        mState = requestCode;
        if (mState == STATE_PHOTO_PICKED) {
            mPickedItem = data.getData();
        }

        // onResume() would be called next
    }
}

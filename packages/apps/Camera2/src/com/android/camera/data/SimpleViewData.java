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

package com.android.camera.data;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.android.camera.ui.FilmStripView;
import com.android.camera.util.PhotoSphereHelper;

/**
 * A LocalData that does nothing but only shows a view.
 */
public class SimpleViewData implements LocalData {
    private static final String TAG = "CAM_SimpleViewData";

    private final int mWidth;
    private final int mHeight;
    private final View mView;
    private final long mDateTaken;
    private final long mDateModified;

    public SimpleViewData(
            View v, int width, int height,
            int dateTaken, int dateModified) {
        mView = v;
        mWidth = width;
        mHeight = height;
        mDateTaken = dateTaken;
        mDateModified = dateModified;
    }

    @Override
    public long getDateTaken() {
        return mDateTaken;
    }

    @Override
    public long getDateModified() {
        return mDateModified;
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getOrientation() {
        return 0;
    }

    @Override
    public int getViewType() {
        return FilmStripView.ImageData.VIEW_TYPE_REMOVABLE;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public Uri getContentUri() {
        return Uri.EMPTY;
    }

    @Override
    public int getLocalDataType() {
        return LOCAL_VIEW;
    }

    @Override
    public LocalData refresh(ContentResolver resolver) {
        return null;
    }

    @Override
    public boolean isUIActionSupported(int action) {
        return false;
    }

    @Override
    public boolean isDataActionSupported(int action) {
        return false;
    }

    @Override
    public boolean delete(Context c) {
        return false;
    }

    @Override
    public View getView(Activity activity, int width, int height, Drawable placeHolder,
            LocalDataAdapter adapter) {
        return mView;
    }

    @Override
    public void prepare() {
        // do nothing.
    }

    @Override
    public void recycle() {
        // do nothing.
    }

    @Override
    public void isPhotoSphere(Context context, PanoramaSupportCallback callback) {
        // Not a photo sphere panorama.
        callback.panoramaInfoAvailable(false, false);
    }

    @Override
    public void viewPhotoSphere(PhotoSphereHelper.PanoramaViewHelper helper) {
        // do nothing.
    }

    @Override
    public void onFullScreen(boolean fullScreen) {
        // do nothing.
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return true;
    }

    @Override
    public MediaDetails getMediaDetails(Context context) {
        return null;
    }

    @Override
    public double[] getLatLong() {
        return null;
    }

    @Override
    public boolean isPhoto() {
        return false;
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public boolean rotate90Degrees(Context context, LocalDataAdapter adapter,
            int currentDataId, boolean clockwise) {
        // We don't support rotation for SimpleViewData.
        Log.w(TAG, "Unexpected call in rotate90Degrees()");
        return false;
    }

    @Override
    public long getSizeInBytes() {
        return 0;
    }

    @Override
    public long getContentId() {
        return -1;
    }
}

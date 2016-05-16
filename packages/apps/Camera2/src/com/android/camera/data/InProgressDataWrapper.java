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
import android.view.View;
import android.widget.FrameLayout;

import com.android.camera.util.PhotoSphereHelper;
import com.android.camera2.R;

/**
 * A wrapper class for in-progress data. Data that's still being processed
 * should not supporting any actions. Only methods related to actions like
 * {@link #isDataActionSupported(int)} and
 * {@link #isUIActionSupported(int)} are implemented by this class.
 */
public class InProgressDataWrapper implements LocalData {

    final LocalData mLocalData;
    private boolean mHasProgressBar;

    public InProgressDataWrapper(LocalData wrappedData) {
        mLocalData = wrappedData;
    }

    public InProgressDataWrapper(LocalData wrappedData, boolean hasProgressBar) {
        this(wrappedData);
        mHasProgressBar = hasProgressBar;
    }

    @Override
    public View getView(
            Activity a, int width, int height,
            Drawable placeHolder, LocalDataAdapter adapter) {
        View v =  mLocalData.getView(a, width, height, placeHolder, adapter);

        if (mHasProgressBar) {
            // Return a framelayout with the progressbar and imageview.
            FrameLayout frame = new FrameLayout(a);
            frame.setLayoutParams(new FrameLayout.LayoutParams(width, height));
            frame.addView(v);
            a.getLayoutInflater()
                    .inflate(R.layout.placeholder_progressbar, frame);
            return frame;
        }

        return v;
    }

    @Override
    public long getDateTaken() {
        return mLocalData.getDateTaken();
    }

    @Override
    public long getDateModified() {
        return mLocalData.getLocalDataType();
    }

    @Override
    public String getTitle() {
        return mLocalData.getTitle();
    }

    @Override
    public boolean isDataActionSupported(int actions) {
        return false;
    }

    @Override
    public boolean delete(Context c) {
        // No actions are allowed to modify the wrapped data.
        return false;
    }

    @Override
    public boolean rotate90Degrees(
            Context context, LocalDataAdapter adapter,
            int currentDataId, boolean clockwise) {
        // No actions are allowed to modify the wrapped data.
        return false;
    }

    @Override
    public void onFullScreen(boolean fullScreen) {
        mLocalData.onFullScreen(fullScreen);
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return mLocalData.canSwipeInFullScreen();
    }

    @Override
    public String getPath() {
        return mLocalData.getPath();
    }

    @Override
    public String getMimeType() {
        return mLocalData.getMimeType();
    }

    @Override
    public MediaDetails getMediaDetails(Context context) {
        return mLocalData.getMediaDetails(context);
    }

    @Override
    public int getLocalDataType() {
        // Force the data type to be in-progress data.
        return LOCAL_IN_PROGRESS_DATA;
    }

    @Override
    public long getSizeInBytes() {
        return mLocalData.getSizeInBytes();
    }

    @Override
    public LocalData refresh(ContentResolver resolver) {
        return mLocalData.refresh(resolver);
    }

    @Override
    public long getContentId() {
        return mLocalData.getContentId();
    }

    @Override
    public int getWidth() {
        return mLocalData.getWidth();
    }

    @Override
    public int getHeight() {
        return mLocalData.getHeight();
    }

    @Override
    public int getOrientation() {
        return mLocalData.getOrientation();
    }

    @Override
    public int getViewType() {
        return mLocalData.getViewType();
    }

    @Override
    public double[] getLatLong() {
        return mLocalData.getLatLong();
    }

    @Override
    public boolean isUIActionSupported(int action) {
        return false;
    }

    @Override
    public void prepare() {
        mLocalData.prepare();
    }

    @Override
    public void recycle() {
        mLocalData.recycle();
    }

    @Override
    public void isPhotoSphere(Context context, PanoramaSupportCallback callback) {
        mLocalData.isPhotoSphere(context, callback);
    }

    @Override
    public void viewPhotoSphere(PhotoSphereHelper.PanoramaViewHelper helper) {
        mLocalData.viewPhotoSphere(helper);
    }

    @Override
    public boolean isPhoto() {
        return mLocalData.isPhoto();
    }

    @Override
    public Uri getContentUri() {
        return mLocalData.getContentUri();
    }
}

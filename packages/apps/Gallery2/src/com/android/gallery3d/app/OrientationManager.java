/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.provider.Settings;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.ui.OrientationSource;

public class OrientationManager implements OrientationSource {
    private static final String TAG = "OrientationManager";

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;

    private Activity mActivity;
    private MyOrientationEventListener mOrientationListener;
    // If the framework orientation is locked.
    private boolean mOrientationLocked = false;

    // This is true if "Settings -> Display -> Rotation Lock" is checked. We
    // don't allow the orientation to be unlocked if the value is true.
    private boolean mRotationLockedSetting = false;

    public OrientationManager(Activity activity) {
        mActivity = activity;
        mOrientationListener = new MyOrientationEventListener(activity);
    }

    public void resume() {
        ContentResolver resolver = mActivity.getContentResolver();
        mRotationLockedSetting = Settings.System.getInt(
                resolver, Settings.System.ACCELEROMETER_ROTATION, 0) != 1;
        mOrientationListener.enable();
    }

    public void pause() {
        mOrientationListener.disable();
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Orientation handling
    //
    //  We can choose to lock the framework orientation or not. If we lock the
    //  framework orientation, we calculate a a compensation value according to
    //  current device orientation and send it to listeners. If we don't lock
    //  the framework orientation, we always set the compensation value to 0.
    ////////////////////////////////////////////////////////////////////////////

    // Lock the framework orientation to the current device orientation
    public void lockOrientation() {
        if (mOrientationLocked) return;
        mOrientationLocked = true;
        if (ApiHelper.HAS_ORIENTATION_LOCK) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        } else {
            mActivity.setRequestedOrientation(calculateCurrentScreenOrientation());
        }
    }

    // Unlock the framework orientation, so it can change when the device
    // rotates.
    public void unlockOrientation() {
        if (!mOrientationLocked) return;
        mOrientationLocked = false;
        Log.d(TAG, "unlock orientation");
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    private int calculateCurrentScreenOrientation() {
        int displayRotation = getDisplayRotation();
        // Display rotation >= 180 means we need to use the REVERSE landscape/portrait
        boolean standard = displayRotation < 180;
        if (mActivity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            return standard
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        } else {
            if (displayRotation == 90 || displayRotation == 270) {
                // If displayRotation = 90 or 270 then we are on a landscape
                // device. On landscape devices, portrait is a 90 degree
                // clockwise rotation from landscape, so we need
                // to flip which portrait we pick as display rotation is counter clockwise
                standard = !standard;
            }
            return standard
                    ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
    }

    // This listens to the device orientation, so we can update the compensation.
    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            orientation = roundOrientation(orientation, 0);
        }
    }

    @Override
    public int getDisplayRotation() {
        return getDisplayRotation(mActivity);
    }

    @Override
    public int getCompensation() {
        return 0;
    }

    private static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    private static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
        }
        return 0;
    }
}

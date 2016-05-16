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

package com.android.testingcamera2;

/**
 * A camera control class wraps the control parameters.
 */
public class CameraControls {
    private int mSensitivity = 100;
    private long mExposure = 10000000L; //10ms
    private long mFrameDuration = 50000000L; // 50ms
    private boolean mManualControlEnabled = false;

    public CameraControls() {
    }

    public synchronized void setSensitivity(int sensitivity) {
        mSensitivity = sensitivity;
    }

    public synchronized void setExposure(long exposure) {
        mExposure = exposure;
    }

    public synchronized void setFrameDuration(long frameDuration) {
        mFrameDuration = frameDuration;
    }

    public synchronized void enableManualControl(boolean enable) {
        mManualControlEnabled = enable;
    }

    public synchronized boolean isManualControlEnabled() {
        return mManualControlEnabled;
    }

    public synchronized int getSensitivity() {
        return mSensitivity;
    }

    public synchronized long getExposure() {
        return mExposure;
    }

    public synchronized long getFrameDuration() {
        return mFrameDuration;
    }
}

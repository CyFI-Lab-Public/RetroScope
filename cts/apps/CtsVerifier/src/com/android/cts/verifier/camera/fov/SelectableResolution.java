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

package com.android.cts.verifier.camera.fov;

/**
 * A resolution to be used in the adapter that feeds the resolution spinner.
 */
public class SelectableResolution {
    public final int cameraId;
    public final int width;
    public final int height;
    public boolean passed;
    public boolean tested;
    public float measuredFOV;

    public SelectableResolution(int cameraId, int width, int height) {
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
        this.passed = false;
        this.tested = false;
    }

    @Override
    public String toString() {
        return "Cam " + cameraId + ": " + width + " x " + height + " - "
                + (!tested ? "untested" : "done");
    }
}

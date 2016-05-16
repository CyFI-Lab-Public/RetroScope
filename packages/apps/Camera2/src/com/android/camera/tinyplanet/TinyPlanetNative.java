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

package com.android.camera.tinyplanet;

import android.graphics.Bitmap;

/**
 * TinyPlanet native interface.
 */
public class TinyPlanetNative {
    static {
        System.loadLibrary("jni_tinyplanet");
    }

    /**
     * Create a tiny planet.
     *
     * @param in the 360 degree stereographically mapped panoramic input image.
     * @param width the width of the input image.
     * @param height the height of the input image.
     * @param out the resulting tiny planet.
     * @param outputSize the width and height of the square output image.
     * @param scale the scale factor (used for fast previews).
     * @param angleRadians the angle of the tiny planet in radians.
     */
    public static native void process(Bitmap in, int width, int height, Bitmap out, int outputSize,
            float scale, float angleRadians);
}

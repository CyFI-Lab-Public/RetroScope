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

package com.android.dreams.phototable;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * An interpolator where the rate of change starts out quickly and
 * and then decelerates.
 *
 */
public class SoftLandingInterpolator implements Interpolator {
    private final LinearInterpolator fly;
    private final DecelerateInterpolator slide;
    private final float mI;
    private final float mO;
    private final float upperRange;
    private final float bottom;
    private final float top;

    public SoftLandingInterpolator(float i, float o) {
        fly = new LinearInterpolator();
        slide = new  DecelerateInterpolator();
        mI = i;
        mO = o;
        final float epsilon = Math.min(mI / 2f, (1f - mI) / 2f);
        bottom = mI - epsilon;
        top = mI + epsilon;
        upperRange = 1f - bottom;
    }

    public float getInterpolation(float input) {
        final float f = fly.getInterpolation(input / upperRange) * mO;
        final float s = slide.getInterpolation((input - bottom) / upperRange) * (1f - mO) + mO;

        float value;
        if (input < bottom) {
            value = f;
        } else if (input < top) {
            final float alpha = (input - bottom) / (top - bottom);
            value = (1f - alpha) * f + alpha * s;
        } else {
            value = s;
        }

        return value;
    }
}

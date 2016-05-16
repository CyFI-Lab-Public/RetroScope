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

package com.android.gallery3d.filtershow.controller;

public class ParameterSaturation extends BasicParameterInt {
    public static String sParameterType = "ParameterSaturation";
    float[] mHSVO = new float[4];

    public ParameterSaturation(int id, int value) {
        super(id, value, 0, 100);
    }

    @Override
    public String getParameterType() {
        return sParameterType;
    }

    public void setColor(float[] hsvo) {
        mHSVO = hsvo;
    }

    public float[] getColor() {
        mHSVO[3] = getValue() / (float) getMaximum();
        return mHSVO;
    }
}

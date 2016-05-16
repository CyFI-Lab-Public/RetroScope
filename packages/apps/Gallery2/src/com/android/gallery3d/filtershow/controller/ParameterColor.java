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

import android.graphics.Color;

import java.util.Arrays;

public class ParameterColor implements Parameter {
    public static String sParameterType = "ParameterColor";
    protected Control mControl;
    protected FilterView mEditor;
    float[] mHSVO = new float[4];
    String mParameterName;
    int mValue;
    public final int ID;
    int[] mBasColors = {
            Color.RED & 0x80FFFFFF,
            Color.GREEN & 0x80FFFFFF,
            Color.BLUE & 0x80FFFFFF,
            Color.BLACK & 0x80FFFFFF,
            Color.WHITE & 0x80FFFFFF
    };

    public ParameterColor(int id, int defaultColor) {
        ID = id;
        Color.colorToHSV(defaultColor, mHSVO);
        mHSVO[3] = ((defaultColor >> 24) & 0xFF) / (float) 255;
    }

    @Override
    public String getParameterType() {
        return sParameterType;
    }

    public void setColor(float[] hsvo) {
        mHSVO = hsvo;
        mValue = Color.HSVToColor((int) (hsvo[3] * 255), mHSVO);
    }

    public float[] getColor() {
        return mHSVO;
    }

    public void copyFrom(Parameter src) {
        if (!(src instanceof ParameterColor)) {
            throw new IllegalArgumentException(src.getClass().getName());
        }
        ParameterColor p = (ParameterColor) src;

        mValue = p.mValue;
        System.arraycopy(p.mHSVO, 0, mHSVO, 0, 4);
    }

    @Override
    public String getParameterName() {
        return mParameterName;
    }

    @Override
    public String getValueString() {
        return "(" + Integer.toHexString(mValue) + ")";
    }

    @Override
    public void setController(Control control) {
        mControl = control;
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        mValue = value;
        Color.colorToHSV(mValue, mHSVO);
        mHSVO[3] = ((mValue >> 24) & 0xFF) / (float) 255;
    }

    @Override
    public String toString() {
        return getValueString();
    }

    @Override
    public void setFilterView(FilterView editor) {
        mEditor = editor;
    }

    public void copyPalletFrom(ParameterColor parameterColor) {
        System.arraycopy(parameterColor.mBasColors,0,mBasColors,0,mBasColors.length);
    }

    public void setColorpalette(int[] palette) {
        mBasColors = Arrays.copyOf(palette, palette.length);
    }

    public int[] getColorPalette() {
        return mBasColors;
    }
}

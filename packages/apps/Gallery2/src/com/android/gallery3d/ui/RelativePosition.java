/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.ui;

public class RelativePosition {
    private float mAbsoluteX;
    private float mAbsoluteY;
    private float mReferenceX;
    private float mReferenceY;

    public void setAbsolutePosition(int absoluteX, int absoluteY) {
        mAbsoluteX = absoluteX;
        mAbsoluteY = absoluteY;
    }

    public void setReferencePosition(int x, int y) {
        mReferenceX = x;
        mReferenceY = y;
    }

    public float getX() {
        return mAbsoluteX - mReferenceX;
    }

    public float getY() {
        return mAbsoluteY - mReferenceY;
    }
}

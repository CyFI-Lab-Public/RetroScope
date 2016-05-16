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

package com.android.gallery3d.filtershow.filters;

import android.graphics.RectF;

public class RedEyeCandidate implements FilterPoint {
    RectF mRect = new RectF();
    RectF mBounds = new RectF();

    public RedEyeCandidate(RedEyeCandidate candidate) {
        mRect.set(candidate.mRect);
        mBounds.set(candidate.mBounds);
    }

    public RedEyeCandidate(RectF rect, RectF bounds) {
        mRect.set(rect);
        mBounds.set(bounds);
    }

    public boolean equals(RedEyeCandidate candidate) {
        if (candidate.mRect.equals(mRect)
                && candidate.mBounds.equals(mBounds)) {
            return true;
        }
        return false;
    }

    public boolean intersect(RectF rect) {
        return mRect.intersect(rect);
    }

    public RectF getRect() {
        return mRect;
    }
}

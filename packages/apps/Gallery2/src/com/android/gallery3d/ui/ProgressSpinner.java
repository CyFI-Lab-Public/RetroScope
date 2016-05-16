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

import android.content.Context;

import com.android.gallery3d.R;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;

public class ProgressSpinner {
    private static float ROTATE_SPEED_OUTER = 1080f / 3500f;
    private static float ROTATE_SPEED_INNER = -720f / 3500f;
    private final ResourceTexture mOuter;
    private final ResourceTexture mInner;
    private final int mWidth;
    private final int mHeight;

    private float mInnerDegree = 0f;
    private float mOuterDegree = 0f;
    private long mAnimationTimestamp = -1;

    public ProgressSpinner(Context context) {
        mOuter = new ResourceTexture(context, R.drawable.spinner_76_outer_holo);
        mInner = new ResourceTexture(context, R.drawable.spinner_76_inner_holo);

        mWidth = Math.max(mOuter.getWidth(), mInner.getWidth());
        mHeight = Math.max(mOuter.getHeight(), mInner.getHeight());
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void startAnimation() {
        mAnimationTimestamp = -1;
        mOuterDegree = 0;
        mInnerDegree = 0;
    }

    public void draw(GLCanvas canvas, int x, int y) {
        long now = AnimationTime.get();
        if (mAnimationTimestamp == -1) mAnimationTimestamp = now;
        mOuterDegree += (now - mAnimationTimestamp) * ROTATE_SPEED_OUTER;
        mInnerDegree += (now - mAnimationTimestamp) * ROTATE_SPEED_INNER;

        mAnimationTimestamp = now;

        // just preventing overflow
        if (mOuterDegree > 360) mOuterDegree -= 360f;
        if (mInnerDegree < 0) mInnerDegree += 360f;

        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);

        canvas.translate(x + mWidth / 2, y + mHeight / 2);
        canvas.rotate(mInnerDegree, 0, 0, 1);
        mOuter.draw(canvas, -mOuter.getWidth() / 2, -mOuter.getHeight() / 2);
        canvas.rotate(mOuterDegree - mInnerDegree, 0, 0, 1);
        mInner.draw(canvas, -mInner.getWidth() / 2, -mInner.getHeight() / 2);
        canvas.restore();
    }
}

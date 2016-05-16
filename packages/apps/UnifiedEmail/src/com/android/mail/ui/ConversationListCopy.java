/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

/**
 * A normally inert view that draws a bitmap copy of the existing conversation list view when
 * transitioning.
 *
 */
public class ConversationListCopy extends View {

    private Bitmap mBitmap;

    private static final String LOG_TAG = LogTag.getLogTag();

    public ConversationListCopy(Context c) {
        this(c, null);
    }

    public ConversationListCopy(Context c, AttributeSet attrs) {
        super(c, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null) {
            return;
        }
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    /**
     * Copy a bitmap of the source view.
     * <p>
     * Callers MUST call {@link #unbind()} when the copy is no longer needed.
     *
     * @param v
     */
    public void bind(View v) {
        unbind();

        if (v.getWidth() == 0 || v.getHeight() == 0) {
            return;
        }

        try {
            mBitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Config.ARGB_8888);
            v.draw(new Canvas(mBitmap));
        } catch (OutOfMemoryError e) {
            LogUtils.e(LOG_TAG, e, "Unable to create fancy list transition bitmap");
            // mBitmap will just be null, and this won't draw, which is fine
        }
    }

    /**
     * Release resources from a previous {@link #bind(View)}.
     */
    public void unbind() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

}

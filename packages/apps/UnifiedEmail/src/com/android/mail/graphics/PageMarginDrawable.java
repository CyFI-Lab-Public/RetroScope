/*
 * Copyright (C) 2013 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;

/**
 * Custom drawable for the page margins between conversations.
 * Allows us to reduce a layer of overdraw by no longer having
 * a background in the {@link com.android.mail.browse.ConversationPager}.
 */
public class PageMarginDrawable extends InsetDrawable {

    private final Paint mPaint;

    public PageMarginDrawable(Drawable drawable, int insetLeft, int insetTop,
            int insetRight, int insetBottom, int color) {
        super(drawable, insetLeft, insetTop, insetRight, insetBottom);
        mPaint = new Paint();
        mPaint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), mPaint);
        super.draw(canvas);
    }
}

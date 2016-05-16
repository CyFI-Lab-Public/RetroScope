/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;

public class BitmapScreenNail implements ScreenNail {
    private final BitmapTexture mBitmapTexture;

    public BitmapScreenNail(Bitmap bitmap) {
        mBitmapTexture = new BitmapTexture(bitmap);
    }

    @Override
    public int getWidth() {
        return mBitmapTexture.getWidth();
    }

    @Override
    public int getHeight() {
        return mBitmapTexture.getHeight();
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        mBitmapTexture.draw(canvas, x, y, width, height);
    }

    @Override
    public void noDraw() {
        // do nothing
    }

    @Override
    public void recycle() {
        mBitmapTexture.recycle();
    }

    @Override
    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        canvas.drawTexture(mBitmapTexture, source, dest);
    }
}

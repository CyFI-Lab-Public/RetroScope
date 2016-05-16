/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.acceleration.cts;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AcceleratedView extends View {

    private final CountDownLatch mDrawLatch = new CountDownLatch(1);

    private boolean mIsHardwareAccelerated;

    public AcceleratedView(Context context) {
        super(context);
    }

    public AcceleratedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AcceleratedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (this) {
            mIsHardwareAccelerated = canvas.isHardwareAccelerated();
        }
        mDrawLatch.countDown();
    }

    public boolean isCanvasHardwareAccelerated() {
        try {
            if (mDrawLatch.await(1, TimeUnit.SECONDS)) {
                synchronized (this) {
                    return mIsHardwareAccelerated;
                }
            } else {
                throw new IllegalStateException("View was not drawn...");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

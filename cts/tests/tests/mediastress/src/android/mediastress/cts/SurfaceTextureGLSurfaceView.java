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

/* Original code copied from NDK Native-media sample code */

package android.mediastress.cts;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class SurfaceTextureGLSurfaceView extends GLSurfaceView {

    private SurfaceTextureRenderer mRenderer;
    private final OnSurfaceChangedListener mListener;

    public SurfaceTextureGLSurfaceView(Context context, OnSurfaceChangedListener listener) {
        this(context, null, listener);
    }

    public SurfaceTextureGLSurfaceView(Context context, AttributeSet attributeSet,
            OnSurfaceChangedListener listener) {
        super(context, attributeSet);
        mListener = listener;
        init(listener);
    }

    private void init(OnSurfaceChangedListener listener) {
        setEGLContextClientVersion(2);
        mRenderer = new SurfaceTextureRenderer(listener);
        setRenderer(mRenderer);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRenderer.getSurfaceTexture();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // ugly but renderer has no notification for surfaceDestroyed.
        if (mListener != null) {
            // surface destroyed long time ago, but let existing stuffs cleared
            mListener.onSurfaceDestroyed();
        }
        super.surfaceDestroyed(holder);
    }
}


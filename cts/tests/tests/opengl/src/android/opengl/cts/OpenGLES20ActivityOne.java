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
package android.opengl.cts;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import java.lang.InterruptedException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OpenGLES20ActivityOne extends Activity {

    public static final String EXTRA_VIEW_TYPE = "viewType";
    public static final String EXTRA_VIEW_INDEX = "viewIndex";

    OpenGLES20View view;
    Renderer mRenderer;
    int mRendererType;
    private CountDownLatch mLatch = new CountDownLatch(1);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        int viewType = getIntent().getIntExtra(EXTRA_VIEW_TYPE, -1);
        int viewIndex = getIntent().getIntExtra(EXTRA_VIEW_INDEX, -1);

        view = new OpenGLES20View(this, viewType, viewIndex, mLatch);
        setContentView(view);
    }

    public int getNoOfAttachedShaders() {
        return ((RendererBase)mRenderer).mShaderCount[0];
    }

    public int glGetError() {
        return ((RendererBase)mRenderer).mError;
    }

    public boolean waitForFrameDrawn() {
        boolean result = false;
        try {
            result = mLatch.await(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // just return false
        }
        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (view != null) {
            view.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (view != null) {
            view.onResume();
        }
    }

    class OpenGLES20View extends GLSurfaceView {

        public OpenGLES20View(Context context, int type, int index, CountDownLatch latch) {
            super(context);
            setEGLContextClientVersion(2);

            if (type == Constants.SHADER) {
                if (index == 1) {
                    mRenderer = new RendererOneShaderTest(latch);
                } else if(index == 2) {
                    mRenderer = new RendererTwoShaderTest(latch);
                } else if(index == 3) {
                    mRenderer = new RendererThreeShaderTest(latch);
                } else if(index == 4) {
                    mRenderer = new RendererFourShaderTest(latch);
                } else if(index == 5) {
                    mRenderer = new RendererFiveShaderTest(latch);
                } else if(index == 6) {
                    mRenderer = new RendererSixShaderTest(latch);
                } else if(index == 7) {
                    mRenderer = new RendererSevenShaderTest(latch);
                } else if(index == 8) {
                    mRenderer = new RendererEightShaderTest(latch);
                } else if(index == 9) {
                    mRenderer = new RendererNineShaderTest(latch);
                } else if(index == 10) {
                    mRenderer = new RendererTenShaderTest(latch);
                } else if(index == 11) {
                    mRenderer = new RendererElevenShaderTest(latch);
                } else if(index == 12) {
                    mRenderer = new RendererTwelveShaderTest(latch);
                } else {
                    throw new RuntimeException();
                }
            } else if (type == Constants.PROGRAM) {
                if (index == 1) {
                    mRenderer = new RendererOneProgramTest(latch);
                }
            }
            setRenderer(mRenderer);
        }

        @Override
        public void setEGLContextClientVersion(int version) {
            super.setEGLContextClientVersion(version);
        }

    }
}

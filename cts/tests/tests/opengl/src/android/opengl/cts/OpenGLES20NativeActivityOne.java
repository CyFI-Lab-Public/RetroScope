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

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.lang.InterruptedException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLES20NativeActivityOne extends Activity {

    public static final String EXTRA_VIEW_TYPE = "viewType";
    public static final String EXTRA_VIEW_INDEX = "viewIndex";

    int mValue;

    OpenGLES20View view;
    GL2Renderer mRenderer;
    int mRendererType;

    private CountDownLatch mLatch = new CountDownLatch(1);

    /**
     * Called when the activity is first created.
     */
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
        view.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(view != null) {
            view.onResume();
        }
    }

    class OpenGLES20View extends GLSurfaceView {
        public OpenGLES20View(Context context, int category, int testCase, CountDownLatch latch) {
            super(context);
            setEGLContextClientVersion(2);
            mRenderer = new GL2Renderer(category, testCase, latch);
            setRenderer(mRenderer);
        }

        @Override
        public void setEGLContextClientVersion(int version) {
            super.setEGLContextClientVersion(version);
        }

        public GL2Renderer getRenderer() {
            return mRenderer;
        }
    }
}
class GL2Renderer implements GLSurfaceView.Renderer {
    private String TAG = "GL2Renderer";
    private int mCategory = -1;
    private int mTestCase = -1;
    int mAttachShaderError = -1;
    int mShaderCount = -1;

    private CountDownLatch mLatch;

    public GL2Renderer(int category, int testcase, CountDownLatch latch) {
        this.mCategory = category;
        this.mTestCase = testcase;
        mLatch = latch;
    }

    public void onDrawFrame(GL10 gl) {

    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG ,"onSurfaceCreated");
        GL2JniLibOne.init(mCategory, mTestCase, width, height);
        this.mAttachShaderError = GL2JniLibOne.getAttachShaderError();
        Log.i(TAG,"error:" + mAttachShaderError);
        this.mShaderCount = GL2JniLibOne.getAttachedShaderCount();
        Log.i(TAG,"ShaderCount:" + mShaderCount);
        mLatch.countDown();
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }
}

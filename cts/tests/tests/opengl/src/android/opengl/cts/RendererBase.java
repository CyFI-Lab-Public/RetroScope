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

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;

import java.util.concurrent.CountDownLatch;

public abstract class RendererBase implements GLSurfaceView.Renderer {

    FloatBuffer floatBuffer;
    int mProgram;
    int maPositionHandle;
    float[] mColorOne = new float[4];

    int[] mShaderCount = null;
    int mError;

    // child may need to manipulate them directly
    protected CountDownLatch mLatch;
    protected boolean mDrawn = false;

    public RendererBase(CountDownLatch latch) {
        mLatch = latch;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    public int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mDrawn) {
            return;
        }
        mDrawn = true;
        doOnDrawFrame(gl);
        mLatch.countDown();
    }

    /// dummy method to be overridden by child
    public void doOnDrawFrame(GL10 gl) {
    }
}

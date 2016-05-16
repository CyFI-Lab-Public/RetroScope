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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.CountDownLatch;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;

public class NativeRendererOneColorBufferTest extends RendererBase {
    private int mProgramObject;
    private int mWidth;
    private int mHeight;
    private FloatBuffer mVertices;
    private ShortBuffer mIndexBuffer;

    private static String TAG = "HelloTriangleRenderer";

    // Our vertices.
    private float mVerticesData[] = {
           -0.5f,  0.5f, 0.0f,  // 0, Top Left
           -0.5f, -0.5f, 0.0f,  // 1, Bottom Left
            0.5f, -0.5f, 0.0f,  // 2, Bottom Right
            0.5f,  0.5f, 0.0f,  // 3, Top Right
    };

    private float[] mVertexColor = {};

    private short[] mIndices = { 0, 1, 2, 0, 2, 3 };
    private FloatBuffer mColor;

    public NativeRendererOneColorBufferTest(Context context, CountDownLatch latch) {
        super(latch);
    }

    public NativeRendererOneColorBufferTest(Context context, float[] color, CountDownLatch latch) {
        super(latch);
        this.mVertexColor = color;
    }

    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

    }

    public void doOnDrawFrame(GL10 glUnused) {
      Log.i(TAG,"onDrawFrame start");

      float[] result = GL2JniLibOne.draw(3, 1, mVertexColor);
      mColorOne = result;
    }

    public float[] getActualRGBA() {
        return this.mColorOne;
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        mWidth = width;
        mHeight = height;
        Log.i(TAG,"onSurfaceCreated start");
        GL2JniLibOne.init(3,1, width, height);
        Log.i(TAG,"onSurfaceCreated finish");
    }
}

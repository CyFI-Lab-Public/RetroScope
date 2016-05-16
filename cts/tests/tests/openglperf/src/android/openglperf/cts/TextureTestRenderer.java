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
package android.openglperf.cts;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import junit.framework.Assert;

/**
 * This test is for checking an issue
 * mentioned in https://groups.google.com/group/android-developers/browse_thread/thread/
 * 9c0ccb1a215586a8/2f28259213340eab?hl=en&lnk=gst&q=opengl+galaxy#2f28259213340eab
 * The GPU driver seems to change texture coordinates inside glDrawTexfOES, and
 * texture inside following drawing does not work as expected.
 * The thread also mentions texture malfunctioning with rotation,
 * but that was not observed, but the case will cover both.
 */
public class TextureTestRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "TextureTest";
    private static final float[] mVerticesData = {
        // X, Y, Z, s, t
        -1.0f, -1.0f, 0, 0.6f, 0.f,
        1.0f, -1.0f, 0, 0.7f, 0.f,
        -1.0f,  1.0f, 0, 0.6f, 0.1f,
        1.0f,   1.0f, 0, 0.7f, 0.1f,
    };
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int GEOMETRY_STRIDE = 20;

    private final FloatBuffer mVertices;
    private final Bitmap mBitmap;
    private int mTextureID;
    private boolean mOesDrawTextureSupported = false;
    private int mWidth;
    private int mHeight;
    private float mRotationAngle = 0f;

    public TextureTestRenderer() {
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);
        // 4x4 texture with 4 colors
        int[] colors = {
                Color.RED, Color.RED, Color.BLUE, Color.BLUE,
                Color.RED, Color.RED, Color.BLUE, Color.BLUE,
                Color.GREEN, Color.GREEN, Color.CYAN, Color.CYAN,
                Color.GREEN, Color.GREEN, Color.CYAN, Color.CYAN
        };
        mBitmap = Bitmap.createBitmap(colors, 4, 4, Bitmap.Config.ARGB_8888);
    }

    public void onDrawFrame(GL10 gl) {
        // Redraw background color
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
                GL10.GL_REPLACE);

        // Set GL_MODELVIEW transformation mode
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity(); // reset the matrix to its default state

        // When using GL_MODELVIEW, you must set the view point
        GLU.gluLookAt(gl, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        gl.glActiveTexture(GL10.GL_TEXTURE0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        // red for default color
        gl.glColor4f(1.0f, 0f, 0f, 1.0f);
        gl.glVertexPointer(3, GL10.GL_FLOAT, GEOMETRY_STRIDE, mVertices);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, GEOMETRY_STRIDE, mVertices
                .duplicate().position(3));
        if (mOesDrawTextureSupported) {
            // left / bottom / width / height (negative w/h flip the image)
            int crop[] = new int[] { 0, 0, 4, 4 };

            ((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D,
                    GL11Ext.GL_TEXTURE_CROP_RECT_OES, crop, 0);
            // 4x4 drawing in left-bottom corner
            ((GL11Ext) gl).glDrawTexfOES(0, 0, 0, 4, 4);
        }
        gl.glPushMatrix();
        gl.glRotatef(mRotationAngle, 0f, 0f, 1f);
        mRotationAngle += 10f;
        if (mRotationAngle > 360f) {
            mRotationAngle = 0f;
        }
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
        gl.glPopMatrix();
        // for one pixel
        IntBuffer pixel = ByteBuffer.allocateDirect(4).asIntBuffer();
        gl.glReadPixels(mWidth / 2, mHeight / 2, 1, 1, GL10.GL_RGBA,
                GL10.GL_UNSIGNED_BYTE, pixel);
        pixel.position(0);
        Log.i(TAG, "pixel read " + Integer.toHexString(pixel.get(0)));

        Assert.assertEquals(0x0000ffff, pixel.get(0)); // BLUE in RGBA

        checkGlError(gl, "onDrawFrame");
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
        gl.glViewport(0, 0, width, height);

        // make adjustments for screen ratio
        float ratio = (float) width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION); // set matrix to projection mode
        gl.glLoadIdentity(); // reset the matrix to its default state
        gl.glFrustumf(-2 * ratio, 2 * ratio, -2, 2, 3, 7);
        checkGlError(gl, "onSurfaceChanged");
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String extensions = gl.glGetString(GL10.GL_EXTENSIONS);
        Log.i(TAG, "extensions: " + extensions);
        if (extensions.contains("GL_OES_draw_texture")) {
            mOesDrawTextureSupported = true;
        }
        // Set the background frame color
        gl.glClearColor(0f, 0f, 0f, 1.0f);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                GL10.GL_NEAREST); // necessary due to low resolution texture

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
        checkGlError(gl, "onSurfaceCreated");
    }

    private void checkGlError(GL10 gl, String op) {
        int error;
        while ((error = gl.glGetError()) != GL10.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new IllegalStateException(op + ": glError " + error);
        }
    }
}

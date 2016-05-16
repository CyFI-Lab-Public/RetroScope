/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier.sensors;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;

import com.android.cts.verifier.R;

public class AccelerometerTestRenderer implements GLSurfaceView.Renderer, SensorEventListener {

    /**
     * A representation of a 3D triangular wedge or arrowhead shape, suitable
     * for pointing a direction.
     */
    private static class Wedge {
        private final static int VERTS = 6;

        /**
         * Storage for the vertices.
         */
        private FloatBuffer mFVertexBuffer;

        /**
         * Storage for the drawing sequence of the vertices. This contains
         * integer indices into the mFVertextBuffer structure.
         */
        private ShortBuffer mIndexBuffer;

        /**
         * Storage for the texture used on the surface of the wedge.
         */
        private FloatBuffer mTexBuffer;

        public Wedge() {
            // Buffers to be passed to gl*Pointer() functions
            // must be direct & use native ordering

            ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 6 * 4);
            vbb.order(ByteOrder.nativeOrder());
            mFVertexBuffer = vbb.asFloatBuffer();

            ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
            tbb.order(ByteOrder.nativeOrder());
            mTexBuffer = tbb.asFloatBuffer();

            ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 8 * 2);
            ibb.order(ByteOrder.nativeOrder());
            mIndexBuffer = ibb.asShortBuffer();

            /**
             * Coordinates of the vertices making up a simple wedge. Six total
             * vertices, representing two isosceles triangles, side by side,
             * centered on the origin separated by 0.25 units, with elongated
             * ends pointing down the negative Z axis.
             */
            float[] coords = {
                    // X, Y, Z
                    -0.125f, -0.25f, -0.25f,
                    -0.125f,  0.25f, -0.25f,
                    -0.125f,  0.0f,   0.559016994f,
                     0.125f, -0.25f, -0.25f,
                     0.125f,  0.25f, -0.25f,
                     0.125f,  0.0f,   0.559016994f,
            };

            for (int i = 0; i < VERTS; i++) {
                for (int j = 0; j < 3; j++) {
                    mFVertexBuffer.put(coords[i * 3 + j] * 2.0f);
                }
            }

            for (int i = 0; i < VERTS; i++) {
                for (int j = 0; j < 2; j++) {
                    mTexBuffer.put(coords[i * 3 + j] * 2.0f + 0.5f);
                }
            }

            // left face
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 2);

            // right face
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 3);

            // top side, 2 triangles to make rect
            mIndexBuffer.put((short) 2);
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 2);

            // bottom side, 2 triangles to make rect
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 2);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 5);

            // base, 2 triangles to make rect
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 0);

            mFVertexBuffer.position(0);
            mTexBuffer.position(0);
            mIndexBuffer.position(0);
        }

        public void draw(GL10 gl) {
            gl.glFrontFace(GL10.GL_CCW);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
            gl.glEnable(GL10.GL_TEXTURE_2D);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
            gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 24, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
        }
    }

    /**
     * A representation of the Z-axis in vector form.
     */
    protected static final float[] Z_AXIS = new float[] {
            0, 0, 1
    };

    /**
     * Computes the cross product of two vectors, storing the resulting
     * pseudovector in out. All arrays must be length 3 or more, and out is
     * overwritten.
     * 
     * @param left the left operand of the cross product
     * @param right the right operand of the cross product
     * @param out the array into which to store the cross-product pseudovector's
     *            data
     */
    public static void crossProduct(float[] left, float[] right, float[] out) {
        out[0] = left[1] * right[2] - left[2] * right[1];
        out[1] = left[2] * right[0] - left[0] * right[2];
        out[2] = left[0] * right[1] - left[1] * right[0];
    }

    /**
     * Computes the dot product of two vectors.
     * 
     * @param left the first dot product operand
     * @param right the second dot product operand
     * @return the dot product of left and right
     */
    public static float dotProduct(float[] left, float[] right) {
        return left[0] * right[0] + left[1] * right[1] + left[2] * right[2];
    }

    /**
     * Normalizes the input vector into a unit vector.
     * 
     * @param vector the vector to normalize. Contents are overwritten.
     */
    public static void normalize(float[] vector) {
        double mag = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2]
                * vector[2]);
        vector[0] /= mag;
        vector[1] /= mag;
        vector[2] /= mag;
    }

    /**
     * The angle around mCrossProd to rotate to align Z-axis with gravity.
     */
    protected float mAngle;

    private Context mContext;

    /**
     * The (pseudo)vector around which to rotate to align Z-axis with gravity.
     */
    protected float[] mCrossProd = new float[3];

    private int mTextureID;

    private Wedge mWedge;

    /**
     * It's a constructor. Can you dig it?
     * 
     * @param context the Android Context that owns this renderer
     */
    public AccelerometerTestRenderer(Context context) {
        mContext = context;
        mWedge = new Wedge();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // no-op
    }

    /**
     * Actually draws the wedge.
     */
    public void onDrawFrame(GL10 gl) {
        // set up the texture for drawing
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glActiveTexture(GL10.GL_TEXTURE0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        // clear the screen and draw
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glRotatef(-mAngle * 180 / (float) Math.PI, mCrossProd[0], mCrossProd[1], mCrossProd[2]);
        mWedge.draw(gl);
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            /*
             * For this test we want *only* accelerometer data, so we can't use
             * the convenience methods on SensorManager; so compute manually.
             */
            normalize(event.values);

            /*
             * Because we need to invert gravity (because the accelerometer vector
             * actually points up), that constitutes a 180-degree rotation around X,
             * which means we need to invert Y.
             */
            event.values[1] *= -1;

            crossProduct(event.values, Z_AXIS, mCrossProd);
            mAngle = (float) Math.acos(dotProduct(event.values, Z_AXIS));
        }
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
        float ratio = (float) w / h;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7);
        GLU.gluLookAt(gl, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // set up general OpenGL config
        gl.glClearColor(0.6f, 0f, 0.4f, 1); // a nice purpley magenta
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        // create the texture we use on the wedge
        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

        InputStream is = mContext.getResources().openRawResource(R.raw.sns_texture);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }
}

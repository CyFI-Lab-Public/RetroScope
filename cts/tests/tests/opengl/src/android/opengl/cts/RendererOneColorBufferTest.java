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

public class RendererOneColorBufferTest extends RendererBase {
    private int mProgramObject;
    private int mWidth;
    private int mHeight;
    private FloatBuffer mVertices;
    private ShortBuffer mIndexBuffer;

    private static String TAG = "RendererOneColorBufferTest";

    // Our vertices.
    private float mVerticesData[] = {
           -0.5f,  0.5f, 0.0f,  // 0, Top Left
           -0.5f, -0.5f, 0.0f,  // 1, Bottom Left
            0.5f, -0.5f, 0.0f,  // 2, Bottom Right
            0.5f,  0.5f, 0.0f,  // 3, Top Right
    };

    private float[] mVertexColor = {
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f
    };

    // The order we like to connect them.
    private short[] mIndices = { 0, 1, 2, 0, 2, 3 };
    private FloatBuffer mColor;


    public RendererOneColorBufferTest(Context context, CountDownLatch latch) {
        super(latch);
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(mIndices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
        mIndexBuffer.put(mIndices);
        mIndexBuffer.position(0);

        mColor = ByteBuffer.allocateDirect(mVertexColor.length*4).
                order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColor.put(mVertexColor).position(0);
    }

    public RendererOneColorBufferTest(Context context, float[] colors, CountDownLatch latch) {
        super(latch);
        mVertexColor = colors;
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(mIndices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
        mIndexBuffer.put(mIndices);
        mIndexBuffer.position(0);

        mColor = ByteBuffer.allocateDirect(mVertexColor.length*4).
                order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColor.put(mVertexColor).position(0);
    }

    private int LoadShader(int type, String shaderSrc) {
        int shader;
        int[] compiled = new int[1];

        // Create the shader object
        shader = GLES20.glCreateShader(type);

        if (shader == 0)
            return 0;

        // Load the shader source
        GLES20.glShaderSource(shader, shaderSrc);

        // Compile the shader
        GLES20.glCompileShader(shader);

        // Check the compile status
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }


    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        String vShaderStr =
              "attribute vec4 vPosition;    \n"
            + "attribute vec4 vColor;       \n"
            + "varying vec4 varyColor;      \n"
            + "void main()                  \n"
            + "{                            \n"
            + "   gl_Position = vPosition;  \n"
            + "   varyColor = vColor;       \n"
            + "}                            \n";

        String fShaderStr =
            "precision mediump float;       \n"
            + "varying vec4 varyColor;      \n"
            + "void main()                  \n"
            + "{                            \n"
            + "  gl_FragColor = varyColor;  \n"
            + "}                            \n";

        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = LoadShader(GLES20.GL_VERTEX_SHADER, vShaderStr);
        fragmentShader = LoadShader(GLES20.GL_FRAGMENT_SHADER, fShaderStr);

        // Create the program object
        programObject = GLES20.glCreateProgram();

        if (programObject == 0)
            return;

        GLES20.glAttachShader(programObject, vertexShader);
        GLES20.glAttachShader(programObject, fragmentShader);

        // Bind vPosition to attribute 0
        GLES20.glBindAttribLocation(programObject, 0, "vPosition");
        GLES20.glBindAttribLocation(programObject, 1, "vColor");

        // Link the program
        GLES20.glLinkProgram(programObject);

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0)
        {
            Log.e(TAG, "Error linking program:");
            Log.e(TAG, GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return;
        }

        // Store the program object
        mProgramObject = programObject;

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void doOnDrawFrame(GL10 glUnused)
    {
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use the program object
        GLES20.glUseProgram(mProgramObject);

        // Load the vertex data
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, mVertices);
        GLES20.glEnableVertexAttribArray(0);

        int mColorHandle = GLES20.glGetAttribLocation(mProgramObject, "vColor");
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, mColor);
        GLES20.glEnableVertexAttribArray(1);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndices.length, 
                GLES20.GL_UNSIGNED_SHORT, mIndexBuffer);

        int x = 1;
        int y =1;
        IntBuffer   pinbuffer   = IntBuffer.allocate(1*1*4);

        GLES20.glReadPixels(mWidth/2, mHeight/2, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                pinbuffer);
        int []      pin         = pinbuffer.array();
        int pixel = pin[0];
        float a = (pixel >> 24) & 0xFF;
        float b = (pixel >> 16) & 0xFF;
        float g = (pixel >> 8) & 0xFF;
        float r = pixel & 0xFF;
        Log.i(TAG, "w " + mWidth + " h " + mHeight + " rgba" + r + " " + g + " " + b + " " + a);
        mColorOne[0] = r;
        mColorOne[1] = g;
        mColorOne[2] = b;
        mColorOne[3] = a;
    }

    public float[] getActualRGBA() {
        return this.mColorOne;
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        Log.i(TAG, "onSurfaceChanged " + width + " " + height);
        mWidth = width;
        mHeight = height;
        // Set the viewport
        GLES20.glViewport(0, 0, mWidth, mHeight);
    }
}

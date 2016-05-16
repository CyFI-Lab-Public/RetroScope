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

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RendererTwoShaderTest extends RendererBase {
    private final String fragmentShaderCode = "precision mediump float;  \n"
            + "void main(){              \n"
            + " gl_FragColor = vec4 (0.63671875, 0.76953125, 0.22265625, 1.0); \n"
            + "}  \n";

    public RendererTwoShaderTest(CountDownLatch latch) {
        super(latch);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        initShapes();
        //give a wrong value
        int vertexShaderOne = 9999;
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram =  GLES20.glCreateProgram();
        // some driver crashes instead of returning error.
        GLES20.glAttachShader(mProgram, vertexShaderOne);
        mError = GLES20.glGetError();
        mLatch.countDown();
    }

    public void initShapes(){
        float triangleCoords[] = {   -0.5f, -0.25f, 0,
                 0.5f, -0.25f, 0,
                 0.0f,  0.559016994f, 0};
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(triangleCoords);
        floatBuffer.position(0);
    }
}

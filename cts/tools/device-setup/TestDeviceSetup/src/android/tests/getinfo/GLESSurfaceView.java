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

package android.tests.getinfo;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class GLESSurfaceView extends GLSurfaceView {
    private static final String TAG = "GLESSurfaceView";

    private int mGLVersion;//1, 2, 3
    private CountDownLatch mDone;
    private DeviceInfoActivity mParent;
    /**
     *
     * @param parent
     * @param glVersion the version of GLES API to use inside the view
     * @param done to notify the completion of the task
     */
    public GLESSurfaceView(DeviceInfoActivity parent, int glVersion, CountDownLatch done){
        super(parent);

        mParent = parent;
        mGLVersion = glVersion;
        mDone = done;
        if (glVersion > 1) {
            // Default is 1 so only set if bigger than 1
            setEGLContextClientVersion(glVersion);
        }
        setRenderer(new OpenGLESRenderer());
    }

    public class OpenGLESRenderer implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            String extensions;
            String vendor;
            String renderer;
            if (mGLVersion == 2) {
                extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
                vendor = GLES20.glGetString(GLES20.GL_VENDOR);
                renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            } else if (mGLVersion == 3) {
                extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
                vendor = GLES30.glGetString(GLES30.GL_VENDOR);
                renderer = GLES30.glGetString(GLES30.GL_RENDERER);
            } else {
                extensions = gl.glGetString(GL10.GL_EXTENSIONS);
                vendor = gl.glGetString(GL10.GL_VENDOR);
                renderer = gl.glGetString(GL10.GL_RENDERER);
            }
            Log.i(TAG, "extensions : " + extensions);
            Log.i(TAG, "vendor : " + vendor);
            Log.i(TAG, "renderer : " + renderer);
            mParent.setGraphicsInfo(vendor, renderer);
            Scanner scanner = new Scanner(extensions);
            scanner.useDelimiter(" ");
            while (scanner.hasNext()) {
                String ext = scanner.next();
                if (ext.contains("texture")) {
                    if (ext.contains("compression") || ext.contains("compressed")) {
                        Log.i(TAG, "Compression supported: " + ext);
                        mParent.addCompressedTextureFormat(ext);
                    }
                }
            }

            mDone.countDown();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

    }
}

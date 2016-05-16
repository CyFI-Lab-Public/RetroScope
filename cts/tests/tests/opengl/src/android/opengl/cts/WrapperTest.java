/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.test.AndroidTestCase;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Test some aspects of the Java-language wrappers generated for OpenGL.
 */
public class WrapperTest extends AndroidTestCase {
    private static final String TAG = "WrapperTest";

    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;


    /**
     * Tests range-checking on glGetIntegerv in GLES 1.x.
     */
    public void testGetIntegerv1() {
        eglSetup(1, 1, 1);  // GLES 1.x with 1x1 pbuffer

        checkGlError("start");

        int[] countBuf = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_NUM_COMPRESSED_TEXTURE_FORMATS, countBuf, 0);
        checkGlError("glGetIntegerv(count)");

        int formatCount = countBuf[0];
        Log.d(TAG, "got count=" + formatCount);

        // try with a buffer large enough to hold all results
        GLES10.glGetIntegerv(GLES10.GL_COMPRESSED_TEXTURE_FORMATS, new int[formatCount], 0);
        checkGlError("glGetIntegerv(full1)");

        // try with an exact-fit IntBuffer
        ByteBuffer fullByteBuf = ByteBuffer.allocateDirect(4 * formatCount);
        IntBuffer fullIntBuf = fullByteBuf.asIntBuffer();
        GLES10.glGetIntegerv(GLES10.GL_COMPRESSED_TEXTURE_FORMATS, fullIntBuf);
        checkGlError("glGetIntegerv(full2)");

        // try with an oversized IntBuffer with an offset
        final int OFFSET = 5;
        ByteBuffer oversizeByteBuf = ByteBuffer.allocateDirect(4 * (formatCount+OFFSET));
        IntBuffer oversizeIntBuf = oversizeByteBuf.asIntBuffer();
        oversizeIntBuf.position(OFFSET);
        GLES10.glGetIntegerv(GLES10.GL_COMPRESSED_TEXTURE_FORMATS, oversizeIntBuf);
        checkGlError("glGetIntegerv(full3)");
        assertEquals(oversizeIntBuf.get(OFFSET), fullIntBuf.get(0));

        // retry with a buffer that's too small -- should throw
        ByteBuffer partialByteBuf = ByteBuffer.allocateDirect(4 * (formatCount - 1));
        IntBuffer partialIntBuf = partialByteBuf.asIntBuffer();
        try {
            GLES10.glGetIntegerv(GLES10.GL_COMPRESSED_TEXTURE_FORMATS, partialIntBuf);
            checkGlError("glGetIntegerv(partial1)");
            throw new RuntimeException("buffer has overrun (intbuf)");
        } catch (IllegalArgumentException iae) {
            // good
        }

        try {
            GLES10.glGetIntegerv(GLES10.GL_COMPRESSED_TEXTURE_FORMATS, new int[formatCount-1], 0);
            checkGlError("glGetIntegerv(partial2)");
            throw new RuntimeException("buffer has overrun (int[])");
        } catch (IllegalArgumentException iae) {
            // good
        }

        eglRelease();
    }

    /**
     * Tests range-checking on glGetIntegerv in GLES 2.x.
     */
    public void testGetIntegerv2() {
        eglSetup(2, 1, 1);  // GLES 2.x with 1x1 pbuffer

        checkGlError("start");

        int[] countBuf = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_NUM_COMPRESSED_TEXTURE_FORMATS, countBuf, 0);
        checkGlError("glGetIntegerv(count)");

        int formatCount = countBuf[0];
        Log.d(TAG, "got count=" + formatCount);

        // try with a buffer large enough to hold all results
        GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, new int[formatCount], 0);
        checkGlError("glGetIntegerv(full1)");

        // try with an exact-fit IntBuffer
        ByteBuffer fullByteBuf = ByteBuffer.allocateDirect(4 * formatCount);
        IntBuffer fullIntBuf = fullByteBuf.asIntBuffer();
        GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, fullIntBuf);
        checkGlError("glGetIntegerv(full2)");

        // try with an oversized IntBuffer with an offset
        final int OFFSET = 5;
        ByteBuffer oversizeByteBuf = ByteBuffer.allocateDirect(4 * (formatCount+OFFSET));
        IntBuffer oversizeIntBuf = oversizeByteBuf.asIntBuffer();
        oversizeIntBuf.position(OFFSET);
        GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, oversizeIntBuf);
        checkGlError("glGetIntegerv(full3)");
        assertEquals(oversizeIntBuf.get(OFFSET), fullIntBuf.get(0));

        // retry with a buffer that's too small -- should throw
        ByteBuffer partialByteBuf = ByteBuffer.allocateDirect(4 * (formatCount - 1));
        IntBuffer partialIntBuf = partialByteBuf.asIntBuffer();
        try {
            GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, partialIntBuf);
            checkGlError("glGetIntegerv(partial1)");
            throw new RuntimeException("buffer has overrun (intbuf)");
        } catch (IllegalArgumentException iae) {
            // good
        }

        try {
            GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, new int[formatCount-1], 0);
            checkGlError("glGetIntegerv(partial2)");
            throw new RuntimeException("buffer has overrun (int[])");
        } catch (IllegalArgumentException iae) {
            // good
        }

        eglRelease();
    }

    /**
     * Tests whether EGL is releasing resources when the thread exits.  If
     * it doesn't, we'll consume memory rapidly, and will fail or be
     * killed within a couple hundred iterations.
     * <p>
     * It may be worthwhile to watch the memory growth with procrank or showmap
     * while the test runs to detect smaller leaks.
     */
    public void testThreadCleanup() throws Throwable {
        class WrappedTest implements Runnable {
            public Throwable mThrowable;

            private static final int WIDTH = 1280;
            private static final int HEIGHT = 720;

            @Override
            public void run() {
                try {
                    eglSetup(2, WIDTH, HEIGHT);
                    if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                        throw new RuntimeException("eglMakeCurrent failed");
                    }
                    eglRelease();
                } catch (Throwable th) {
                    mThrowable = th;
                }
            }
        }

        WrappedTest wrappedTest = new WrappedTest();

        // Android has "reference-counted" EGL initialization.  We want our eglTerminate call
        // to be the "last" termination, since that's the situation we're trying to test, but
        // it's possible that some previous test failed to balance eglInitialize and
        // eglTerminate.  So we call eglTerminate several times in a desperate attempt to
        // zero out the refcount.
        //
        // Before we can terminate we need to be sure that the display has been initialized
        // at least once.
        eglSetup(2, 1, 1);
        for (int i = 0; i < 100; i++) {
            EGL14.eglTerminate(mEGLDisplay);
        }

        for (int i = 0; i < 1000; i++) {
            if ((i % 25) == 0) {
                Log.d(TAG, "iteration " + i);
            }

            Thread th = new Thread(wrappedTest, "EGL thrash");
            th.start();
            th.join();
            if (wrappedTest.mThrowable != null) {
                throw wrappedTest.mThrowable;
            }
        }
    }

    /**
     * Checks for GL errors.
     */
    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    /**
     * Prepares EGL.  Pass in the desired GLES API version (1 or 2).
     * <p>
     * Sets mEGLDisplay, mEGLContext, and mEGLSurface, and makes them current.
     */
    private void eglSetup(int api, int width, int height) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        int renderableType;
        switch (api) {
            case 1:
                renderableType = EGL14.EGL_OPENGL_ES_BIT;
                break;
            case 2:
                renderableType = EGL14.EGL_OPENGL_ES2_BIT;
                break;
            default:
                throw new RuntimeException("unsupported API level " + api);
        }

        // Configure EGL for OpenGL ES 1.0 or 2.0, with a pbuffer
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+pbuffer ES" + api + " EGL config");
        }

        // Create context
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, api,
                EGL14.EGL_NONE
        };
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                attrib_list, 0);
        checkEglError("eglCreateContext");
        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }

        // Create a 1x1 pbuffer surface
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
        checkEglError("eglCreatePbufferSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }

        // Make it current
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Releases EGL goodies.
     */
    private void eglRelease() {
        // Terminating the display will release most objects, but won't discard the current
        // surfaces and context until we release the thread.  It shouldn't matter what order
        // we do these in.
        if (mEGLDisplay != null) {
            EGL14.eglTerminate(mEGLDisplay);
            EGL14.eglReleaseThread();
        }

        // null everything out so future attempts to use this object will cause an NPE
        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
    }

    /**
     * Checks for EGL errors.
     */
    private void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Log.e(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }
}

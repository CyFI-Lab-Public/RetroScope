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

package com.android.cts.verifier.camera.analyzer;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;

import java.io.FileOutputStream;
import java.io.File;
import java.lang.Runnable;

/**
 * Provides an abstraction for all camera tests and allows communication
 * between camera test classes with the UI thread. This base class provides
 * functions to contruct and access debug output images. It can access and
 * set the pointer address of checkerboard centers and radius. It also provides
 * native methods to convert an image shot by the camera into a native
 * character array. Another native method it provides is to create a native
 * test handler with desired debug height and width.
 */
public abstract class CameraTests{

    public static final int CAMERA_TEST_NOT_RUN = 0;
    public static final int CAMERA_TEST_SUCCESS = 1;
    public static final int CAMERA_TEST_FAILURE = 2;

    private static final String TAG = "CameraTests";

    /** Memory address of the color checker centers. */
    private static long sCheckerCenterAddress = 0;
    /** Memory address of the color checker radius. */
    private static long sCheckerRadiusAddress = 0;
    /** The surface view linked with the camera preview. */
    private static SurfaceView sCameraView;
    /** Image debug output. */
    private Bitmap mDebugOutput;
    /** Shared camera instance. */
    protected static Camera mTestCamera = null;

    /**
     * Constructs the base CameraTests class.
     */
    public CameraTests() {}

    /**
     * Returns debug Bitmap image. In the test to find the color checker,
     * the debug image will be the captured image with a matched color checker
     * overlay on top. In the exposure compensation test, the debug image
     * will be the response curve of the camera.
     * @return A low-resolution Bitmap to be displayed in the UI.
     */
    public Bitmap getDebugOutput() {
        return mDebugOutput;
    }

    public String getDebugText() {
        return "";
    }

    public abstract String getTestName();

    /**
     * Gets the detailed report for CTS output.
     */
    public String getResultText(){
        return "Details not available \n";
    }

    /**
     * Provides a polymorphism to start the run() method for all child classes.
     */
    public abstract void run(int index);

    public SurfaceView getCameraView() {
        return sCameraView;
    }

    public static void setCameraView(SurfaceView cameraView) {
        sCameraView = cameraView;
    }

    /**
     * Refreshes the camera instance when the activity opens a new camera.
     */
    public static void setCamera(Camera newCamera) {
        mTestCamera = newCamera;
    }

    public static Camera getCamera() {
        return mTestCamera;
    }

    /**
     * Sets the memory address of the checker centers and checker radius.
     *
     * @param inputCenterAddress the new memory address of
     *                           the color checker centers
     * @param inputRadiusAddress the new memory address of
     *                           the color checker radius
     */
    public static void setCheckerAddress(long inputCenterAddress, long inputRadiusAddress) {
        sCheckerCenterAddress = inputCenterAddress;
        sCheckerRadiusAddress = inputRadiusAddress;
    }

    /**
     * Provides polymorphism to indicate whether the checker memory addresses
     * should be copied.
     *
     * @return <code>true</code> if the class invoking the method needs to
     *                           update the memory address of the color checker
     *                           centers and radius;
     *         <code>false</code> if the class invoking the method does NOT
     *                           update the memory address of the color checker
     *                           centers and radius.
     */
    public boolean copyCheckerAddress() {
        return false;
    }

    public void cleanUp() {
    }

    public static long getCheckerCenter() {
        return sCheckerCenterAddress;
    }

    public static long getCheckerRadius() {
        return sCheckerRadiusAddress;
    }

    public abstract String getTestName(int index);

    public abstract int getResult(int index);

    public abstract int getNumTests();

    /**
     * Provides a native method to convert the input Bitmap of the captured
     * image into a native character array and constructs a native image class
     * with this character array. This method currently supports conversion
     * of Bitmaps in the formats of RGB_565 and RGB_8888.
     *
     * @param input the input Bitmap, which is decoded from the camrea data.
     *
     * @return the memory address of the native image class which contains
     * the character array.
     */
    public native long findNative(Bitmap input);

    /**
     * Provides a native method to create a native image handler class. The
     * native image handler class is the base class for all test classes and
     * contains the debug output as a character array.
     *
     * @param outputHeight the desired height for the debug output
     * @param outputWidth the desired width for the debug output
     *
     * @return the memory address of the native test handler class.
     */
    public native long createImageTestHandler(int outputHeight, int outputWidth);

    /**
     * Provides a native method to clean up the memory taken by the handler.
     *
     * @param handlerAddress the memory address of the native test handler,
     * which contains the debug output.
     */
    public native void cleanUpHandler(long handlerAddress);

    /**
     * Provides a native method to convert the native debug output from
     * character array back to Bitmap and copy it to the debug output of this
     * class.
     *
     * @param handlerAddress the memory address of the native test handler,
     * which contains the debug output.
     */
    public native void displayHandlerDebugOutput(long handlerAddress);

    static {
        System.loadLibrary("cameraanalyzer");
    }
}

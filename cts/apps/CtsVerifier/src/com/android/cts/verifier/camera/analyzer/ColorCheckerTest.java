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
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.Log;
import android.widget.ImageView;

import java.util.List;

/** Locates a Xrite Classic Color Checker grid pattern in an image, stores the
 * center positions and the color checker radius, and provides a function to
 * get the memory address of these two properties.
 *
 * The pattern is a 6x4 grid of square color patches. The detection routine
 * assumes the pattern is placed roughly facing the camera, with the long size
 * roughly horizontal. It also assumes that the grey squares are in the bottom
 * row.
 */
public class ColorCheckerTest extends CameraTests {

    private static final String TAG = "ColorCheckerTest";

    /** Memory address of the image class instance that contains the image. */
    private long mClassAddress;
    /** Memory address of the image test class instance. */
    private long mTestHandler;
    /** Thread lock. */
    private final Object mProcessingImage = new Object();
    /** Boolean to tell whether auto focus has succeded.*/
    private boolean mAutoFocusSuccess;
    /** Boolean to tell whether auto focus is supported.*/
    private boolean mAutoFocusEnabled = false;
    /** Singleton instance of the class.*/
    private static ColorCheckerTest singletonTest = null;

    private boolean mFindCheckerSuccess = false;
    private boolean mHasRunOnce = false;

    /**
     * Constructs a <code>ColorCheckerTest</code> instance with a given
     * Camera pointer.
     */
    private ColorCheckerTest() {
        super();
    }

    /**
     * Updates the camera and parameter when the activity switches camera.
     */
    public void updateCamera() {
        Camera.Parameters params = mTestCamera.getParameters();
        List<String> supportedFocusModes = params.getSupportedFocusModes();

        // Sets camera focus mode to Auto focus if it is supported.
        if (supportedFocusModes.contains(params.FOCUS_MODE_AUTO)) {
            Log.v(TAG, "Auto focus possible");
            params.setFocusMode(params.FOCUS_MODE_AUTO);
            mTestCamera.setParameters(params);
            mAutoFocusEnabled = true;
        } else {
            mAutoFocusEnabled = false;
        }
    }

    public static synchronized ColorCheckerTest getSingletonTest() {
        if (singletonTest == null) {
            Log.v(TAG, "Creating a new ColorCheckerTest instance");
            singletonTest = new ColorCheckerTest();
            singletonTest.initializeTest();
        }
        return singletonTest;
    }

    private void initializeTest() {
        // Creates a native test handler with a 120x160 pixel debug output
        mTestHandler = createColorCheckerTest(120, 160);
    }

    @Override
    public synchronized void run(int index) {
        Log.v(TAG, "ColorCheckerTest thread started!");
        mAutoFocusSuccess = false;
        mFindCheckerSuccess = false;
        mHasRunOnce = true;
        // Sets camera focus mode to Auto focus if it is supported.
        if (mAutoFocusEnabled) {
            while (!mAutoFocusSuccess) {
                // Starts the auto focus process of the camera.
                mTestCamera.autoFocus(mAutoFocusListener);

                // Locks thread until the camera finishes taking picture.
                synchronized (mProcessingImage) {
                    try{
                        Log.v(TAG, "Start waiting for Image");
                        mProcessingImage.wait();
                    } catch (InterruptedException e) {
                        Log.v(TAG, "Callback wait fails!");
                    }
                }
            }
        } else {
            mTestCamera.takePicture(null, null, null, mTestJpegListener);
            synchronized (mProcessingImage) {
                try{
                    Log.v(TAG, "Start waiting for Image");
                    mProcessingImage.wait();
                } catch (InterruptedException e) {
                    Log.v(TAG, "Callback wait fails!");
                }
            }
        }

        // Launches the native method to find the color checker in the image.
        mFindCheckerSuccess = processColorCheckerTest(mTestHandler);
        // Displays the debug output from the native test handler instance.
        displayHandlerDebugOutput(mTestHandler);

        Log.v(TAG, "Callback has returned!");
    }

    private Camera.AutoFocusCallback mAutoFocusListener = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean mSuccess, Camera mCamera) {
            if (mSuccess) {
                mAutoFocusSuccess = true;
                Log.v(TAG, "Autofocus success!");
                mCamera.takePicture(null, null, null, mTestJpegListener);
            } else {
                try{
                    Log.v(TAG, "Autofocus failed. Please adjust!");
                    Thread.sleep(4000);
                    Log.v(TAG, "END Waiting");
                } catch (InterruptedException e){}

                synchronized (mProcessingImage) {
                    mProcessingImage.notifyAll();
                }
            }
        }
    };

    private Camera.PictureCallback mTestJpegListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera mCamera) {
            Log.v(TAG, "Shutter pressed down!");

            // Changes the focus mode to fixed to avoid focus shift after
            // auto focus is successful.
            //Camera.Parameters params = mCamera.getParameters();
            //params.setFocusMode(params.FOCUS_MODE_FIXED);
            //mCamera.setParameters(params);

            // Decodes the camera data to Bitmap and creates a native image
            // class with the Bitmap.
            Bitmap inputImage;
            inputImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            long bufferAddress = findNative(inputImage);
            Log.v(TAG, "findNative method finishes");

            // Cleans up the Bitmap memory space.
            inputImage.recycle();
            data = null;
            inputImage = null;
            System.gc();

            // Constructs a test handler class to handle the image.
            createColorCheckerClass(bufferAddress, mTestHandler);

            mCamera.startPreview();

            // Notifies the thread lock the image capture is done.
            synchronized (mProcessingImage) {
                mProcessingImage.notifyAll();
            }
        }
    };

    /**
     * Overrides the base class method and use the memory addresses of the
     * checker centers and radius computed by the native test handler class
     * to update the values stored in the base class.
     *
     * @return <code>true</code> indicating a memory address upload is needed.
     */
    @Override
    public boolean copyCheckerAddress() {
        if (mFindCheckerSuccess) {
            setCheckerAddress(getColorCheckerCenterAdd(mTestHandler),
                              getColorCheckerRadiusAdd(mTestHandler));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void cleanUp() {
        cleanUpHandler(mTestHandler);
    }

    @Override
    public String getTestName() {
        return "Color Checker test: \n";
    }

    @Override
    public String getTestName(int index) {
        return "Find color checker";
    }

    @Override
    public int getResult(int index) {
        if (mFindCheckerSuccess) {
            return CameraTests.CAMERA_TEST_SUCCESS;
        } else {
            if (mHasRunOnce) {
                return CameraTests.CAMERA_TEST_FAILURE;
            } else {
                return CameraTests.CAMERA_TEST_NOT_RUN;
            }
        }
    }

    @Override
    public int getNumTests() {
        return 1;
    }

    /**
     * Gets the memory address of the vector storing the color checker centers
     * from the native test handler instance.
     *
     * @param handlerAddress the memory address of the native test handler
     * instance
     *
     * @return memory address of the native vector storing the color checker
     * centers' coordinates
     */
    private native long getColorCheckerRadiusAdd(long handlerAddress);

    /**
     * Gets the memory address of the vector storing the color checker radius
     * from the native test handler instance.
     *
     * @param handlerAddress the memory address of the native test handler
     * instance.
     *
     * @return memory address of the native vector storing the color checker
     * centers' coordinates
     */
    private native long getColorCheckerCenterAdd(long handlerAddress);

    /**
     * Creates a native color checker test handler instance.
     *
     * @param outputWidth the desired width for the debug output
     * @param outputHeight the desired height of the debug output
     *
     * @return memory address of the native test handler instance
     */
    private native long createColorCheckerTest(int outputWidth, int outputHeight);

    /**
     * Loads a native image class instances and extracts data from it to add
     * to the test handler.
     *
     * @param bufferAddress the memory address of the image class instance
     * @param handlerAddress the memory address of the test handler instance
     */
    private native void createColorCheckerClass(long bufferAddress, long handlerAddress);

    /**
     * Processes the data in the native test handler instance. Computes test
     * results with all the data and construct a debug image or debug text
     * outputs.
     *
     * @param handlerAddress the memory address of the test handler instance
     */
    private native boolean processColorCheckerTest(long handlerAddress);

    static {
        System.loadLibrary("cameraanalyzer");
    }
}

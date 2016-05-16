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
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

public class ExposureCompensationTest extends CameraTests {

    private static final String TAG = "ExposureCompensationTest";

    /** Records the current exposure level. */
    private float mExposureLevel;
    /** Lock for the camera object.*/
    private final Object mProcessingImage = new Object();
    /** Lock for the camera's auto focusing task.*/
    private final Object mAutoFocusing = new Object();
    /** Memory address of the native test handler.*/
    private long mTestHandler;
    /** Test results. */
    private int[] mTestResults;
    /** Number of sub-tests. */
    private int mNumTests;
    /** Camera Parameters. */
    private Camera.Parameters mParams;
    /** Debug results in text. */
    private String mDebugText;

    private static ExposureCompensationTest singletonTest = null;

    private ExposureCompensationTest(){
        super();
    }

    /** Prepares the camera and the related parameters for the test.*/
    public void updateCamera() {
        mParams = mTestCamera.getParameters();
        Log.v(TAG, String.format("Exposure level is from %d to %d",
                                 mParams.getMinExposureCompensation(),
                                 mParams.getMaxExposureCompensation()));
        mNumTests = (int) ((float) (mParams.getMaxExposureCompensation() -
                                    mParams.getMinExposureCompensation())
                            * mParams.getExposureCompensationStep());
        mTestResults = new int[mNumTests + 1];
        for (int i = 0; i < mNumTests + 1; ++i) {
            mTestResults[i] = CameraTests.CAMERA_TEST_NOT_RUN;
        }
    }

    public static synchronized ExposureCompensationTest getSingletonTest() {
        if (singletonTest == null) {
            Log.v(TAG, "Creating a new ExposureCompensationTest instance");
            singletonTest = new ExposureCompensationTest();
            singletonTest.initializeTest();
        }
        return singletonTest;
    }

    private void initializeTest() {
        mDebugText = new String();
        // Creates a native test handler with a 120x160 pixel debug output
        mTestHandler = createExposureCompensationTest(200, 280);
    }

    @Override
    public synchronized void run(int index){
        Log.v(TAG, "ExposureCompensationTest thread started!");

        int testRangeMin, testRangeMax;
        if (index == 0) {
            testRangeMin = mParams.getMinExposureCompensation();
            testRangeMax = mParams.getMaxExposureCompensation();
        } else {
            testRangeMin = (int) ((float)(index - 1) / mParams.getExposureCompensationStep())
                    + mParams.getMinExposureCompensation();
            testRangeMax = (int) ((float)(index) / mParams.getExposureCompensationStep())
                    + mParams.getMinExposureCompensation();
        }

        /** Checks for each exposure compensation setting within the test range.*/
        for (int i = testRangeMin;
                i <= testRangeMax; i += 1){
            mExposureLevel = i * mParams.getExposureCompensationStep();
            Log.v(TAG, String.format("Current exposure level is %d", i));
            int mCameraExposure;

            do{
                mParams.setExposureCompensation(i);
                mTestCamera.setParameters(mParams);

                try{
                    Log.v(TAG, "Waiting");
                    Thread.sleep(4000);
                    Log.v(TAG, "END Waiting");
                } catch (InterruptedException e){
                    //TODO: error handling.
                }

                mParams = mTestCamera.getParameters();
                mCameraExposure = mParams.getExposureCompensation();
                Log.v(TAG, String.format("Camera exposure level is %d", mCameraExposure));
            } while (mCameraExposure != i);

            mTestCamera.takePicture(null, null, null, mTestJpegListener);

            synchronized (mProcessingImage) {
                try{
                    Log.v(TAG, "Start waiting for Image");
                    mProcessingImage.wait();
                } catch (InterruptedException e){
                    Log.v(TAG, "Callback wait fails!");
                }
            }
        }

        mDebugText = processExposureCompensationTest(mTestHandler);
        displayHandlerDebugOutput(mTestHandler);

        Log.v(TAG, "Callback has returned!");
        mParams.setExposureCompensation(0);
        mTestCamera.setParameters(mParams);
    }

    private Camera.PictureCallback mTestJpegListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera mCamera) {
            Log.v(TAG, "Shutter pressed down!");
            Log.v(TAG, String.format("Current exposure is %f", mExposureLevel));

            Bitmap inputImage;

            inputImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            long bufferAddress = findNative(inputImage);
            Log.v(TAG, "findNative method finishes");

            inputImage.recycle();
            data = null;
            inputImage = null;
            System.gc();

            createExposureCompensationClass(bufferAddress, mTestHandler,
                                            getCheckerCenter(), getCheckerRadius(),
                                            mExposureLevel);
            mCamera.startPreview();

            synchronized (mProcessingImage) {
                mProcessingImage.notifyAll();
            }
        }
    };

    @Override
    public String getTestName(int index) {
        switch (index) {
            case 0:
                return "EC All Range";
            default:
                return String.format("EC %d -> %d", (index - mNumTests / 2 - 1) * 10,
                                     (index - mNumTests / 2) * 10);
        }
    }

    @Override
    public int getResult(int index) {
        return mTestResults[index];
    }

    @Override
    public int getNumTests() {
        return mNumTests + 1;
    }

    @Override
    public String getTestName() {
        return "Exposure Compensation Test: \n";
    }

    @Override
    public String getDebugText() {
        return mDebugText;
    }

    private native long createExposureCompensationTest(int outputHeight, int outputWidth);

    private native void createExposureCompensationClass(long bufferAddress, long handlerAddress,
            long checkerCenterAddress, long checkerAadiusAddress, float mExposureLevel);

    private native String processExposureCompensationTest(long handlerAddress);

    static {
        System.loadLibrary("cameraanalyzer");
    }
}

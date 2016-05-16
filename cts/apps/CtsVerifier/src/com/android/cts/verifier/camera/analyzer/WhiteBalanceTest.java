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

import java.util.List;

/**
 * Implements a test to verify whether the correlated color temperatures (CTT)
 * of the supported white balance modes are inside the range camera
 * manufacturers generally agree on.
 *
 * The test assumes that the Daylight white balance mode has a CCT of 5200K,
 * which is widely agreed in industry and academics. It then use this as a
 * benchmark and compare images taken with other white balance settings.
 * Using the pixel values of the grey squares on the color checker, the CCT
 * of other white balance modes can be computed. The reference ranges were
 * summarized with the help of online resources. For the Auto mode, the
 * reference CCT is computed as the CCT that will keep the grey squares appear
 * grey in the result image.
 */
public class WhiteBalanceTest extends CameraTests {

    private static final String TAG = "WhiteBalanceTest";

    /** Current white balance mode. */
    private String mWhiteBalance;
    /** Array to store the reference CCT's of each mode. */
    private int[][] mReferenceTemperature;
    /** List of supported white balance mode on a device. */
    private List<String> mWhiteBalanceList;
    /** The index of the white balance mode "Auto". */
    private int mAutoId;

    /** Debug results in text. */
    private String mDebugText;
    /** Memory address of the native test handler instance. */
    private long mTestHandler;
    /** Thread lock. */
    private final Object mProcessingImage = new Object();
    /** Test result to show. */
    private int[] mTestResults;
    /** Number of test. */
    private int mNumTests;
    /** Camera Parameters. */
    private Camera.Parameters mParams;
    /** Singleton test instance. */
    private static WhiteBalanceTest singletonTest = null;
    /** Boolean to check whehter daylight wb has been recorded. */
    private boolean mHasDaylight = false;

    /**
     * Constructs a <code>WhiteBalanceTest</code> instance with a given
     * Camera pointer.
     */
    private WhiteBalanceTest() {
        super();
    }

    public void updateCamera() {
        mAutoId = 0;
        mHasDaylight = false;
        mParams = mTestCamera.getParameters();
        mWhiteBalanceList = mParams.getSupportedWhiteBalance();
        mNumTests = mWhiteBalanceList.size() + 1;
        mTestResults = new int[mNumTests];
        for (int i = 0; i < mNumTests; ++i) {
            mTestResults[i] = CameraTests.CAMERA_TEST_NOT_RUN;
        }

        if (mWhiteBalanceList != null) {
            mReferenceTemperature = new int[mWhiteBalanceList.size()][2];

            // Sets the reference CCT of the supported white balance modes
            for (int i = 0; i < mWhiteBalanceList.size(); i++) {
                setReferenceTemperature(i, mWhiteBalanceList.get(i));
                if (mWhiteBalanceList.get(i).equals("auto")) {
                    mAutoId = i;
                }
            }
        }
    }

    public static synchronized WhiteBalanceTest getSingletonTest() {
        if (singletonTest == null) {
            Log.v(TAG, "Creating a new WhiteBalanceTest instance");
            singletonTest = new WhiteBalanceTest();
            singletonTest.initializeTest();
        }
        return singletonTest;
    }

    private void initializeTest() {
        mDebugText = new String();
        // Creates a native white balance test handler.
        // mTestHandler stores the memory address of this instance.
        mTestHandler = createWhiteBalanceTest();
    }

    private void initializeWhiteBalanceTest() {
        mWhiteBalance = "daylight";
        takePicture(mWhiteBalance);
        int colorTemperature = processWhiteBalanceTest(mTestHandler);

        setReferenceTemperature(mAutoId, colorTemperature);
        mHasDaylight = true;
    }

    private void takePicture(String whiteBalance) {
        mParams.setWhiteBalance(whiteBalance);
        mTestCamera.setParameters(mParams);

        try{
            Log.v(TAG, "Waiting for white balance to adjust");
            Thread.sleep(4000);
            Log.v(TAG, "END Waiting");
        } catch (InterruptedException e) {}

        mTestCamera.takePicture(null, null, null, mTestJpegListener);

        // Thread locks until image capture is done
        synchronized (mProcessingImage) {
            try{
                Log.v(TAG, "Start waiting for Image");
                mProcessingImage.wait();
            } catch (InterruptedException e) {
                Log.v(TAG, "Callback wait fails!");
            }
        }
    }

    /**
     * Runs the white balance camera test instance.
     */
    @Override
    public synchronized void run(int index) {
        Log.v(TAG, "WhiteBalanceTest thread started!");

        if (!mHasDaylight) {
            initializeWhiteBalanceTest();
        }

        if (index != 0) {
            // Retrieves the list of supported white balance mode.
            mParams = mTestCamera.getParameters();

            int i = index - 1;
            mWhiteBalance = mWhiteBalanceList.get(i);

            Log.v(TAG, "Current white balance is " + mWhiteBalance);

            takePicture(mWhiteBalance);

            // Processes the white balance test data in the native code.
            // Returns an array of CCT of each white balance modes, given the CCT
            // of the "Daylight" mode is 5200K.
            int colorTemperature = 0;

            Log.v(TAG, "Finished taking picture, ready to process");
            colorTemperature = processWhiteBalanceTest(mTestHandler);

            // Records the index of the "Auto" white balance mode
            if (mWhiteBalance.equals("daylight")) {
                setReferenceTemperature(mAutoId, colorTemperature);
                prepareDebugText(5200, index);
                // Computes the reference CCT range of the "Auto" mode. Assuming that
                // all grey squares on the color checker should be imaged as grey under
                // a CCT, this CCT is used as a middle point to provide a range.
                //setReferenceTemperature(mAutoId, colorTemperature[mWhiteBalanceList.size()]);
            } else {
                // Prepares the debug output.
                prepareDebugText(colorTemperature, index);
            }
        } else {
            for (int i = 0; i < mWhiteBalanceList.size(); i++) {
                run(i + 1);
            }
        }

        mParams.setWhiteBalance("auto");
        mTestCamera.setParameters(mParams);
    }

    /**
     * Prepares the debug results in HTML text. For each white balance mode,
     * the CCT will be printed in green if it is in the reference range and
     * red otherwise. The reference CCT range is also printed below, with
     * green meaning the CCT range is satisfied and red otherwise.
     *
     * @param colorTemperature the CCT of the supported white balance modes
     */
    private void prepareDebugText(int colorTemperature, int index) {
        mDebugText += String.format("CCT Ref is %d, %d, and CCT is %d",
                                  mReferenceTemperature[index - 1][0],
                                  mReferenceTemperature[index - 1][1],
                                  colorTemperature);
        Log.v(TAG, String.format("CCT Ref is %d, %d, and CCT is %d",
                                  mReferenceTemperature[index - 1][0],
                                  mReferenceTemperature[index - 1][1],
                                  colorTemperature));
        if ((colorTemperature >= mReferenceTemperature[index - 1][0]) &&
                (colorTemperature <= mReferenceTemperature[index - 1][1])) {
            mTestResults[index] = CameraTests.CAMERA_TEST_SUCCESS;
        } else {
            mTestResults[index] = CameraTests.CAMERA_TEST_FAILURE;
        }

    }

    @Override
    public String getDebugText() {
        return mDebugText;
    }

    @Override
    public String getResultText() {
        return mDebugText;
    }

    @Override
    public String getTestName() {
        return "White Balance Test: \n";
    }

    @Override
    public String getTestName(int index) {
        if (index != 0){
            return String.format("%s mode test", mWhiteBalanceList.get(index - 1));
        } else {
            return "Run all tests";
        }
    }

    @Override
    public int getResult(int index) {
        return mTestResults[index];
    }

    @Override
    public int getNumTests() {
        return mNumTests;
    }

    /**
     * Sets the reference temperatures for the white balance modes of
     * incandescent, fluorescent, warm-fluorescent, daylight, cloudy, shade
     * and twilight. These are the currently supported White balance mode
     * listed on the Android camera API.
     *
     * The reference range are summarized based on the published settings of
     * Canon, Nikon and the references from Wikipedia on color temperature.
     *
     * @param i the index of the current white balance mode
     * @param referenceWhiteBalance the name of the white balance mode.
     */
    private void setReferenceTemperature(int i, String referenceWhiteBalance) {
        if (referenceWhiteBalance.equals("incandescent")) {
            mReferenceTemperature[i][0] = 2500;
            mReferenceTemperature[i][1] = 3500;
        } else if (referenceWhiteBalance.equals("fluorescent")) {
            mReferenceTemperature[i][0] = 3000;
            mReferenceTemperature[i][1] = 6500;
        } else if (referenceWhiteBalance.equals("warm-fluorescent")) {
            mReferenceTemperature[i][0] = 2500;
            mReferenceTemperature[i][1] = 3000;
        } else if (referenceWhiteBalance.equals("daylight")) {
            mReferenceTemperature[i][0] = 5000;
            mReferenceTemperature[i][1] = 5400;
        } else if (referenceWhiteBalance.equals("cloudy-daylight")) {
            mReferenceTemperature[i][0] = 5500;
            mReferenceTemperature[i][1] = 7500;
        } else if (referenceWhiteBalance.equals("shade")) {
            mReferenceTemperature[i][0] = 6800;
            mReferenceTemperature[i][1] = 8000;
        } else if (referenceWhiteBalance.equals("twilight")) {
            mReferenceTemperature[i][0] = 10000;
            mReferenceTemperature[i][1] = 14000;
        }
    }

    /** Sets a reference range of CCT based on a given middle point CCT.
     * The rerence range is from -10% of the CCT value to +10%.
     *
     * @param i the index of the current white balance mode
     * @param t the middle point CCT.
     */
    private void setReferenceTemperature(int i, int t) {
        mReferenceTemperature[i][0] = (int)((double)t * 0.9);
        mReferenceTemperature[i][1] = (int)((double)t * 1.1);
    }

    private Camera.PictureCallback mTestJpegListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera mCamera) {
            Log.v(TAG, "Shutter pressed down!");
            Bitmap inputImage;
            try {
                FileOutputStream outStream = new FileOutputStream(
                        String.format("/sdcard/wb%d.jpg", System.currentTimeMillis()));
                outStream.write(data);
                outStream.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {}
            // Decodes the camera data to Bitmap and creates a native image
            // class with the Bitmap.
            inputImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            long bufferAddress = findNative(inputImage);
            Log.v(TAG, "findNative method finishes");

            // Cleans up the Bitmap memory space.
            inputImage.recycle();
            data = null;
            inputImage = null;
            System.gc();

            // Adds the data from the current image base class to the native
            // white balance test handler.
            createWhiteBalanceClass(bufferAddress, mTestHandler,
                                    getCheckerCenter(), getCheckerRadius(), mWhiteBalance);

            mCamera.startPreview();

            // Notifies the thread lock that the image capture is finished
            synchronized (mProcessingImage) {
                mProcessingImage.notifyAll();
            }
        }
    };

    /**
     * Creates a native white balance test handler.
     *
     * @return the memory address of the test handler
     */
    private native long createWhiteBalanceTest();

    /**
     * Adds the data of interest from the image class pointed by
     * <code>bufferAddress</code> to the handler class pointed by
     * <code>handlerAddress</code> by using the color checker coordinates.
     * Also sets the white balance of this test.
     *
     * @param bufferAddress the memory address of the native image class
     * containing the current camera captured image
     * @param handlerAddress the memory address of the native white balance
     * test handler instance
     * @param checkerCenterAddress the memory address of the color checker
     * center coordinates
     * @param checkerRadiusAddress the memory address of the color checker
     * radius
     * @param whiteBalance the white balance mode used for shooting the image
     */
    private native void createWhiteBalanceClass(long bufferAddress, long handlerAddress,
                                                long checkerCenterAddress,
                                                long checkerRadiusAddress,
                                                String whiteBalance);

    /**
     * Processes the white balance test in the native code. This is executed
     * after the images are taken with all possible white balance modes. It
     * uses the "Daylight" white balance mode as reference and computes the
     * CCT of other white balance modes.
     *
     * @param handlerAddress the memory address of the native white balance
     * test handler instance.
     * @param colorTemperature the array to store the computed CCT of all white
     * balance modes.
     */
    private native int processWhiteBalanceTest(long handlerAddress);

    private native int getAutoTemperature(long handlerAddress);

    static {
        System.loadLibrary("cameraanalyzer");
    }
}

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
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implements a test to verify whether the camera metering system works as
 * described in the API.
 *
 * The test consists two sub-categories. The first one has tests with only
 * one metering area defined. The second one has tests with two metering areas
 * defined. For each single sub-test, we use a random number generator to
 * decide where to put some of the metering areas to and how much weight should
 * be assigned to each area. For different tests, we use different ways to
 * define other metering areas and their weight, in order to cover all possible
 * fail cases. The metering areas are contrained to the grey squares in the
 * bottom of the color checker.
 */
public class MeteringTest extends CameraTests {

    private static final String TAG = "MeteringTest";

    /** A long wait.*/
    private static final int LONG_SLEEP = 4000;
    /** Debug result in text. */
    private String mDebugText;
    /** Thread lock. */
    private final Object mProcessingImage = new Object();
    /** Memory address of the native test handler. */
    private long mTestHandler;
    /** The maximum number of metering area the device supports. */
    private int mMaxNumMeteringArea;
    /** The metering areas. */
    private List<Camera.Area> mGreyAreas;
    /** The coordinates of the grey squares on the color checker. */
    private int[] mGreyCoordinates = new int[24];
    /** Random number generator. */
    private final Random mRandomGenerator = new Random();
    /** Reference comparison result for tests. */
    private ArrayList<Boolean>  mReferenceCompareResults;
    /** Number of tests in the same instance. */
    private int mTestCount;
    /** Reference test logs. */
    private ArrayList<String> mReferenceLogs;
    /** Test result to show. */
    private int[] mTestResults;
    /** Number of tests. */
    private int mNumTests;
    /** Camera Parameters. */
    private Camera.Parameters mParams;
    /** Singleton test instance. */
    private static MeteringTest singletonTest = null;

    /** Constructs a <code>MeteringTest</code> instance with the given
     * camera pointer.
     */
    private MeteringTest() {
        super();
    }

    public void updateCamera() {
        // Looks up how many metering area the device supports.
        mParams = mTestCamera.getParameters();
        mMaxNumMeteringArea = mParams.getMaxNumMeteringAreas();
        Log.v(TAG, String.format("Maximum number if metering area is %d", mMaxNumMeteringArea));
        if (mMaxNumMeteringArea == 0) {
            mDebugText = "Custom Metering not supported!";
            Log.v(TAG, "Custom Metering not supported");
        }
    }

    public static synchronized MeteringTest getSingletonTest() {
        if (singletonTest == null) {
            Log.v(TAG, "Creating a new MeteringTest instance");
            singletonTest = new MeteringTest();
            singletonTest.initializeTest();
        }
        return singletonTest;
    }

    private void initializeTest() {
        // Creates a native metering test handler.
        mTestHandler = createMeteringTest();
        mDebugText = new String();
        mReferenceCompareResults = new ArrayList<Boolean>();
        mReferenceLogs = new ArrayList<String>();
        mNumTests = 3;
        mTestResults = new int[mNumTests];
        for (int i = 0; i < mNumTests; ++i) {
            mTestResults[i] = CameraTests.CAMERA_TEST_NOT_RUN;
        }
    }

    /**
     * Runs the metering test instance.
     */
    @Override
    public synchronized void run(int index) {
        if (index == 0) {
            run(1);
            run(2);
            return;
        }
        Log.v(TAG, "MeteringTest thread started!");

        // Finds the coordinates of the grey squares on the color checker.
        // The coordinate system has (-1000, -1000) on the upper left corner.
        // And (1000, 1000) on the bottom right corner.
        findGreyCoordinates(mGreyCoordinates, getCheckerCenter(), getCheckerRadius());

        if (mMaxNumMeteringArea > 0) {
            mTestCount = 0;
            // Runs the metering tests category by category.
            switch (index) {
                case 1:
                    runOneAreaTest();
                    break;
                case 2:
                    if (mMaxNumMeteringArea > 1) {
                        runTwoAreasTest();
                    }
                    break;
                default:
                    break;
            }
        }

        mParams = mTestCamera.getParameters();
        mParams.setMeteringAreas(null);
        mTestCamera.setParameters(mParams);

        boolean[] testCompareResults = new boolean[2 * mTestCount];

        // Processes the image data taken so far and stores the test results.
        processMeteringTest(mTestHandler, testCompareResults);
        // Prepares debug output based on the test results.
        prepareDebugText(testCompareResults, index);

        mReferenceCompareResults.clear();
        mReferenceLogs.clear();
    }

    /**
     * Prepares the test results in HTML text string to show in the UI.
     *
     * If the test result is the same as the reference result, the text will be
     * shown in green. Otherwise it would be shown as red.
     *
     * @param testCompareResults the array storing the comparison results from
     * the data taken by the camera so far.
     */
    private void prepareDebugText(boolean[] testCompareResults, int index) {
        mDebugText = "";
        boolean groupTestPassed = true;
        for (int i = 0; i < mTestCount; ++i) {
              String testLog;
              boolean testPassed = true;
              testLog = mReferenceLogs.get(i);
              mDebugText += (testLog + "<br/>");

              if (testCompareResults[i * 2] == mReferenceCompareResults.get(i * 2)) {
                  mDebugText += String.format(
                      "Picture 1 equivalent to Picture 2 is %b \n",
                      testCompareResults[i * 2]);
              } else {
                  mDebugText += String.format(
                      "Picture 1 equivalent to Picture 2 is %b \n",
                      testCompareResults[i * 2]);
                  testPassed = false;
              }

              if (testCompareResults[i * 2 + 1] == mReferenceCompareResults.get(i * 2 + 1)) {
                  mDebugText += String.format(
                      "Picture 1 darker than Picture 2 is %b \n",
                      testCompareResults[i * 2 + 1]);
              } else {
                  mDebugText += String.format(
                      "Picture 1 darker than Picture 2 is %b \n",
                      testCompareResults[i * 2 + 1]);
                  testPassed = false;
              }

              if (testPassed) {
                  mDebugText += "Test passed! \n";
              } else {
                  mDebugText += "Test failed! \n";
                  groupTestPassed = false;
              }
              Log.v(TAG, String.format("%s", mDebugText));
         }

        if (groupTestPassed) {
            mTestResults[index] = CameraTests.CAMERA_TEST_SUCCESS;
        } else {
            mTestResults[index] = CameraTests.CAMERA_TEST_FAILURE;
        }

    }

    /**
     * Runs tests to check whether the metering functionalities work properly
     * when one metering area is added.
     */
    private void runOneAreaTest() {
        int weight1;
        int weight2;
        int square1;
        int square2;

        Log.v(TAG, "Running one area Test");

        // Test case 1: Two images have the same metering area. Image 1 has
        // a diffent weight than Image 2. The result images should be
        // identical.
        // Tests whether weight normalization works.
        square1 = mRandomGenerator.nextInt(6);
        weight1 = mRandomGenerator.nextInt(100) + 1;
        runSingleTest(square1, square1, weight1);

        square2 = square1;
        weight2 = mRandomGenerator.nextInt(100) + 901;
        runSingleTest(square2, square2, weight2);
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        Log.v(TAG, String.format("Running test for %d square with weights %d, %d",
                                 square1, weight1, weight2));
        mReferenceLogs.add(String.format(
            "Running test for %d 1x1 square with weights %d, %d", square1, weight1, weight2));
        ++mTestCount;

        // Test case 2: Two images have different metering areas. Image 1 has
        // one of the grey squares as its metering area. Image 2 has a darker
        // grey square as its metering area. The weights for both images are
        // the same. Image 1 is expected to be darker than Image 2.
        // Tests whether metering on uni-brightness patches work.
        square1 = mRandomGenerator.nextInt(5);
        weight1 = mRandomGenerator.nextInt(1000) + 1;
        runSingleTest(square1, square1, weight1);

        square2 = mRandomGenerator.nextInt(6 - square1 - 1) + square1 + 1;
        weight2 = weight1;
        runSingleTest(square2, square2, weight2);
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add(String.format(
            "Running test for %d, %d 1x1 square with weight %d", square1, square2, weight1));
        ++mTestCount;

        // Test case 3: Two images have different metering areas. Image one has
        // one of the grey squares as its metering area. Image 2 has a
        // rectangle which contains Image 1's metering area and the neighboring
        // darker grey square. The weights for both tests are the same. Image 1
        // is expected to be darker than Image 2.
        // Tests whether metering on patches with different brightness works.
        square1 = mRandomGenerator.nextInt(5);
        weight1 = mRandomGenerator.nextInt(1000) + 1;
        runSingleTest(square1, square1, weight1);

        square2 = square1;
        weight2 = weight1;
        runSingleTest(square2, square2 + 1, weight2);
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add(String.format(
            "Running test for %d 1x1, 1x2 square with weight %d", square1, weight1));
        ++mTestCount;

        // Test case 4: Two images have different metering areas. Image one has
        // two neighboring grey squares as its metering area. Image 2 has two
        // darker neighboring grey squares as its metering area. Weights are
        // the same for both images. Image 1 is expected to be darker than
        // Image 2.
        // Tests whether metering on two mixed-brightness patches work.
        square1 = mRandomGenerator.nextInt(4);
        weight1 = mRandomGenerator.nextInt(1000) + 1;
        runSingleTest(square1, square1 + 1, weight1);

        square2 = mRandomGenerator.nextInt(5 - square1 - 1) + square1 + 1;
        weight2 = weight1;
        runSingleTest(square2, square2 + 1, weight2);
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add(String.format(
            "Running test for %d, %d 1x2 square with weight %d", square1, square2, weight1));
        ++mTestCount;

        // Test case 5: Two images have different metering areas. Image one has
        // three neighboring grey squares as its metering area. Image 2 has
        // three darker neighboring grey squares as its metering area. Weights
        // are the same. Image 1 is expected to be darker than Image 2.
        // Tests whether metering on three mixed-brightness patches work.
        square1 = mRandomGenerator.nextInt(3);
        weight1 = mRandomGenerator.nextInt(1000) + 1;
        runSingleTest(square1, square1 + 2, weight1);

        square2 = mRandomGenerator.nextInt(4 - square1 - 1) + square1 + 1;
        weight2 = weight1;
        runSingleTest(square2, square2 + 2, weight2);
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add(String.format(
            "Running test for %d, %d 1x3 square with weight %d", square1, square2, weight1));
        ++mTestCount;
    }

    /**
     * Runs metering tests to verify the functionalities when there are two
     * areas set as the metering area.
     */
    private void runTwoAreasTest() {
        int[] weight1 = new int[2];
        int[] weight2 = new int[2];
        int[] square1Start = new int[2];
        int[] square2Start = new int[2];
        int[] square1End = new int[2];
        int[] square2End = new int[2];

        Log.v(TAG, "Running two-area Test");

        // Test case 1: Image 1 has two metering areas. They are two adjacent
        // grey squares (each set as a metering area). The two areas have the
        // same weight. Image 2 has one metering area, which is the combination
        // of Image 1's two metering areas as a rectangle. The weight is the
        // same as that of Image 1's individual area. Image 1 is expected to
        // be equivalent to Image 2.
        // Tests whether having seperating a metering area into two will yield
        // the same result.
        square1Start[0] = mRandomGenerator.nextInt(5);
        square1End[0] = square1Start[0];
        weight1[0] = mRandomGenerator.nextInt(1000) + 1;
        square1Start[1] = square1Start[0] + 1;
        square1End[1] = square1Start[1];
        weight1[1] = weight1[0];
        runMultipleAreaTest(square1Start, square1End, weight1);

        square2Start[0] = square1Start[0];
        weight2[0] = weight1[0];
        runSingleTest(square2Start[0], square2Start[0] + 1, weight2[0]);
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add(String.format(
            "Running test for %d, %d 1x1 square with weight %d",
            square1Start[0], square1Start[1], weight1[0]));
        ++mTestCount;

        // Test case 2: Image 1 has two metering areas. They are two random
        // grey squareson the color checker. The brighter square has a larger
        // weight than the darker square. Image 2 has the same two metering
        // areas as Image 1. The weights for both are equal to the weight of
        // the darker square in Image 1, which is smaller than the weight of
        // the brighter square in Image 1. Image 1 is expected to be darker
        // than Image 2.
        // Tests whether giving one of the two metering areas a different
        // weight would change the image in the correct way.
        square1Start[0] = mRandomGenerator.nextInt(4);
        square1End[0] = square1Start[0];
        weight1[0] = mRandomGenerator.nextInt(100) + 901;
        square1Start[1] = mRandomGenerator.nextInt(5 - square1Start[0] - 1) + square1Start[0] + 1;
        square1End[1] = square1Start[1];
        weight1[1] = mRandomGenerator.nextInt(100) + 1;
        runMultipleAreaTest(square1Start, square1End, weight1);

        square2Start[0] = square1Start[0];
        square2End[0] = square2Start[0];
        weight2[0] = weight1[1];
        square2Start[1] = square1Start[1];
        square2End[1] = square1End[1];
        weight2[1] = weight2[0];
        runMultipleAreaTest(square2Start, square2End, weight2);
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add(String.format(
            "Running test for %d, %d 1x1 square with weight %d, %d",
            square1Start[0], square1Start[1], weight1[0], weight2[1]));
        ++mTestCount;

        // Test case 3: Image 1 has two metering areas. Both are set to the
        // same random grey square on the color checker. The weight for both
        // are the same. Image 2 has one meterig area, which is the same as
        // Image 1's chosen grey square. The weight for it is the same as
        // Image 1's weight for one metering area. Image 1 is expected to be
        // equivalent to Image 2.
        // Tests whether defining overlapping metering area works.
        square1Start[0] = mRandomGenerator.nextInt(6);
        square1End[0] = square1Start[0];
        weight1[0] = mRandomGenerator.nextInt(1000) + 1;
        square1Start[1] = square1Start[0];
        square1End[1] = square1Start[1];
        weight1[1] = weight1[0];
        runMultipleAreaTest(square1Start, square1End, weight1);

        square2Start[0] = square1Start[0];
        square2End[0] = square2Start[0];
        weight2[0] = weight1[0];
        runSingleTest(square2Start[0], square2End[0], weight2[0]);
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add(String.format(
            "Running test for %d 1x1 square with weight %d,", square1Start[0], weight1[0]));
        ++mTestCount;

        // Test case 4: Image 1 has two metering areas. The first one is a
        // grey square on the color checker. The second one is a rectangle
        // containing the first metering area's grey square and its neighboring
        // darker square. The weights for both metering area are the same.
        // Image 2 has two metering areas. The first one is the same grey
        // square as Image 1's first metering area. The second one is the
        // neighboring darker grey square. The weight for the brighter square
        // is double the weight of Image 1's weights for each metering area.
        // The weight for the Image 2's darker grey square is the same as
        // Image 1's weight for each of its metering areas. Image 1 is expected
        // to be equivalent to Image 2.
        // Tests whether the weights for overlapping metering area add up.
        square1Start[0] = mRandomGenerator.nextInt(2);
        square1End[0] = square1Start[0];
        weight1[0] = mRandomGenerator.nextInt(500) + 1;
        square1Start[1] = square1Start[0];
        square1End[1] = square1Start[1] + 1;
        weight1[1] = weight1[0];
        runMultipleAreaTest(square1Start, square1End, weight1);

        square2Start[0] = square1Start[0];
        square2End[0] = square1End[0];
        weight2[0] = weight1[0] * 2;
        square2Start[1] = square2Start[0] + 1;
        square2End[1] = square2Start[1];
        weight2[1] = weight1[1];
        runMultipleAreaTest(square2Start, square2End, weight2);
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add(String.format(
            "Running test for %d 1x2 1x1 and 1x2 square with weight %d,",
            square1Start[0], weight1[0]));
        ++mTestCount;
    }

    /**
     * Runs the metering test when multiple metering areas are defined.
     *
     * @param startIndex the array storing the index of the grey square where
     * one metering area starts
     * @param endIndex the array storing the index of the grey square where one
     * metering area ends.
     * @param weight the array storing the weight for each metering area.
     */
    private void runMultipleAreaTest(int[] startIndex, int[] endIndex, int[] weight) {
        int numAreas = startIndex.length;
        mParams = mTestCamera.getParameters();
        List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

        for (int i = 0; i < numAreas; ++i) {
            meteringAreas.add(makeArea(startIndex[i], endIndex[i], weight[i]));
            Log.v(TAG, String.format("Add metering area for %d, %d, %d",
                                     startIndex[i], endIndex[i], weight[i]));
        }
        mParams.setMeteringAreas(meteringAreas);
        mTestCamera.setParameters(mParams);
        takePicture();
    }

    /**
     * Runs the metering test when one metering area is defined.
     *
     * @param startIndex the index of the grey square where the metering area
     * starts
     * @param endIndex the index of the grey square where the metering area
     * ends.
     * @param weight the weight for the metering area.
     */
    private void runSingleTest(int startIndex, int endIndex, int weight) {
        mParams = mTestCamera.getParameters();
        List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

        Log.v(TAG, String.format("Single test for %d, %d, %d", startIndex, endIndex, weight));
        meteringAreas.add(makeArea(startIndex, endIndex, weight));
        mParams.setMeteringAreas(meteringAreas);
        mTestCamera.setParameters(mParams);
        takePicture();
    }

    /**
     * Takes picture with the camera instance linked to this test class.
     */
    private void takePicture() {
        // Waits for the metering to be stable
        try{
            Log.v(TAG, "Waiting for metering");
            Thread.sleep(LONG_SLEEP);
            Log.v(TAG, "END Waiting");
        } catch (InterruptedException e) {}

        mTestCamera.takePicture(null, null, null, mTestJpegListener);

        // Locks thread until picture is taken and ready for processing.
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
     * Constructs a <code>Camera.Area</code> object of the metering area.
     * Given the start and end index of one metering area, it takes the upper
     * left corner of the starting square and the bottom right corner of the
     * end square to construct an Area.
     *
     * @param startIndex the index of the grey square where the metering area
     * starts
     * @param endIndex the index of the grey square where the metering area
     * ends
     * @param weight the weight of this metering area.
     *
     * @return a <code>Camera.Area</code> object which represents this metering
     * area
     */
    private Camera.Area makeArea(int startIndex, int endIndex, int weight) {
        Rect areaRect = new Rect(mGreyCoordinates[startIndex * 4],
                                 mGreyCoordinates[startIndex * 4 + 1],
                                 mGreyCoordinates[endIndex * 4 + 2],
                                 mGreyCoordinates[endIndex * 4 + 3]);
        Camera.Area area = new Camera.Area(areaRect, weight);

        return area;
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
        return "Metering Test: \n";
    }

    @Override
    public String getTestName(int index) {
        switch (index) {
            case 0:
                return "Run all tests";
            case 1:
                return "One metering area tests";
            case 2:
                return "Multiple metering areas tests";
            default:
                return "";
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

    private Camera.PictureCallback mTestJpegListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera mCamera) {
            Log.v(TAG, "Shutter pressed down!");
            Bitmap inputImage;
            try {
                FileOutputStream outStream = new FileOutputStream(
                    String.format("/sdcard/metering%d.jpg", System.currentTimeMillis()));
                outStream.write(data);
                outStream.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {}

            // Decodes the input data of the camera.
            inputImage = BitmapFactory.decodeByteArray(data, 0, data.length);

            // Records the memory address of the native image class instance.
            long bufferAddress = findNative(inputImage);
            Log.v(TAG, "findNative method finishes");

            // Cleans up the memory taken by the bitmap.
            inputImage.recycle();
            data = null;
            inputImage = null;
            System.gc();

            // Add the image data to the native test handler.
            createMeteringClass(bufferAddress, mTestHandler,
                                getCheckerCenter(), getCheckerRadius());
            mCamera.startPreview();

            // Releases thread lock after the image is processed.
            synchronized (mProcessingImage) {
                mProcessingImage.notifyAll();
            }
        }
    };

    /**
     * Finds the coordinates of the grey squares on the color checker.
     * The coordinates are computed by using the checker center and radius.
     * The coordinates are converted to a system with (-1000, -1000) in the
     * upper left corner and (1000, 1000) in the  bottom right corner.
     *
     * @param greyCoordinates the array to store the coordinates of the grey
     * squares
     * @param checkerCenterAddress the memory address pointing to the vector
     * storing the color checker's centers.
     * @param checkerRadiusAddress the memory address pointing to the vetor
     * storing the color checker's radius.
     */
    private native void findGreyCoordinates(int[] greyCoordinates,
                                            long checkerCenterAddress, long checkerRadiusAddress);

    /**
     * Creates the native metering test handler with no debug image.
     *
     * @return the memory address pointing to the native test handler instance
     */
    private native long createMeteringTest();

    /**
     * Adds the data from the native image class instance to the native test
     * handler.
     *
     * @param bufferAddress the meory address of the native image class
     * @param handlerAddress the memory address of the native test handler
     * @param checkerCenterAddress the memory address of the checker cneters
     * @param checkerRadiusAddress the meory address of the checker radius
     */
    private native void createMeteringClass(long bufferAddress, long handlerAddress,
                                            long checkerCenterAddress,
                                            long checkerRadiusAddress);

    /**
     * Process the data stored in the native test handler and stores the
     * comparison results.
     *
     * @param handlerAddress the memory address of the native test handler
     * @param testCompareResults the boolean array to store the test comparison
     * results
     */
    private native void processMeteringTest(long handlerAddress, boolean[] testCompareResults);

    static {
        System.loadLibrary("cameraanalyzer");
    }
}

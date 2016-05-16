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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a test to verify whether the Auto Exposure Lock functions as
 * described in the API.
 *
 * The test consists three sub-categories. The first set of tests focus on
 * testing whether the auto exposure lock works in various situations.
 * For all tests in this set, the lock is set during the period when the camera
 * preview is open. In this way the lock locks exposure according to the
 * lighting at the moment of setting the lock. The second set of tests focus on
 * testing whether the auto exposure lock works as expected after turning the
 * preview off and on. The lock is set during the period when the camera
 * preview is turned off. The lock is expected to lock an exposure level
 * identical to the one before the preview is turned off. One special case in
 * this category is to set lock before the preview is turned on for the first
 * time.
 */
public class AutoLockTest extends CameraTests {

    private static final String TAG = "AutoLockTest";
    /** USB permission to connect to ADK. */
    private static final String ACTION_USB_PERMISSION = "com.android.cts.verifier.USB_PERMISSION";
    /** Defines a long sleep period.*/
    private static final int SHORT_SLEEP = 2000;
    /** Defines a short sleep period. */
    private static final int LONG_SLEEP = 4000;

    /** Test results in text format. */
    private String mDebugText = new String();
    /** Detailed report. */
    private String mResultText = new String();
    /** Thread lock of the camera callbacks. */
    private final Object mProcessingImage = new Object();
    /** Memory address of the native test handler. */
    private long mTestHandler;
    /** Array storing the reference test results. */
    private ArrayList<Boolean> mReferenceCompareResults;
    /** Array storing the reference test scenario logs. */
    private ArrayList<String> mReferenceLogs;
    /** Number of tests so far. */
    private int mTestCount;

    /** Usb Manager of the USB connections. */
    private UsbManager mUsbManager;
    /** Intent for getting the permission to access the ADK. */
    private PendingIntent mPermissionIntent;
    /** Boolean to represent whether a permission is gained. */
    private boolean mPermissionRequestPending;
    /** USB accessory pointing to the ADK. */
    private UsbAccessory mAccessory;
    /** File descriptor of the USB communication port. */
    private ParcelFileDescriptor mFileDescriptor;
    /** Output stream to write commands for ADK to. */
    private FileOutputStream mOutputStream;

    /** Pointer to the CameraAnalyzerActivity activity. */
    private CameraAnalyzerActivity mActivity;
    /** Boolean to tell whether the accessory is opened. */
    private boolean mSetupReady;
    /** Thread lock for setting up the usb. */
    private final Object mUsbSetup = new Object();
    /** Boolean to indicate whether there is an ADK attached. */
    private boolean mUsingUsb = false;
    /** Test results.*/
    private int[] mTestResults;
    /** Number of tests. */
    private int mNumTests;
    /** Singleton test instance.*/
    private static AutoLockTest singletonTest = null;

    /**
     * Receives the notice of whether the connection to ADK is granted or
     * denied.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, String.format("Received USB broadcast with action %s ", action));

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory =
                        (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // Grants the permission to connect to the ADK.
                        Log.v(TAG, "Open accessory 3");
                        openAccessory(accessory);
                        // Overwrites the old camera instsance with the one currently opened
                        // by the CameraAnalyzerActivity instance, since the permission
                        // dialogue pauses the CameraAnalyzerActivity and forces the camera to
                        // be released and reopened when the dialogue disappears.
                        mTestCamera = mActivity.getCameraInstance();
                    } else {
                        // Denies the permission to connect to the ADK.
                        Log.d(TAG, "permission denied for accessory " + accessory);
                    }
                    // Marks that the permission request has been processed.
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                // Invokes when the USB is detached.
                // Closes the accessory if it has not been closed.
                Log.v(TAG, "Usb device detached");
                mUsingUsb = false;
                UsbAccessory accessory =
                    (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }
        }
    };

    /**
     * Opens the ADK from USB and attaches the output stream with it.
     *
     * Notifies the tread lock that the USB setup is ready.
     */
    private void openAccessory(UsbAccessory accessory) {
        Log.d(TAG, "openAccessory: " + accessory);
        mFileDescriptor = mUsbManager.openAccessory(accessory);

        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mOutputStream = new FileOutputStream(fd);
            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }

        // Unlocks the thread lock of waiting for the USB to be ready.
        synchronized (mUsbSetup) {
            mSetupReady = true;
            Log.v(TAG, "Setup ready");
            mUsbSetup.notifyAll();
        }
    }

    /**
     * Closes the ADK and detaches the output stream from it.
     */
    private void closeAccessory() {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    /**
     * Constructs the AutoLockTest class, which can execute a series of tests
     * to verify whether the device's Auto Exposure Lock is working properly.
     *
     * The test uses the LED lights on an ADK device as light source to change
     * the lighting condition of the environment. The usb connection to the
     * ADK board is established in the constructor.
     *
     * @param hostActivity pointer to the <code>CameraAnalyzerActivity</code>
     * that instructs the Auto Lock Test
     * @param mCamera pointer to the current camera instance
     */
    private AutoLockTest(){
        super();
    }

    public static synchronized AutoLockTest getSingletonTest() {
        if (singletonTest == null) {
            Log.v(TAG, "Creating a new AutoLockTest instance");
            singletonTest = new AutoLockTest();
            singletonTest.initializeTest();
        }
        return singletonTest;
    }

    private void initializeTest() {
        // Creates a native test handler with a 120x160 pixel debug output
        mTestHandler = createAutoLockTest();
        mReferenceCompareResults = new ArrayList<Boolean>();
        mReferenceLogs = new ArrayList<String>();
        mNumTests = 4;
        mTestResults = new int[mNumTests];
        for (int i = 0; i < mNumTests; ++i) {
            mTestResults[i] = CameraTests.CAMERA_TEST_NOT_RUN;
        }
    }

    public void updateCamera() {}

    public void setActivity(CameraAnalyzerActivity hostActivity){
        if (mUsingUsb) {
            closeConnection();
        }

        mActivity = hostActivity;

        mSetupReady = false;

        Log.v(TAG, "Start to test ADK connection");
        // Starts to establish the connection to the ADK board.
        mUsbManager = (UsbManager) mActivity.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(mActivity, 0,
                                                       new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        mActivity.registerReceiver(mUsbReceiver, filter);

        if (mActivity.getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) mActivity.getLastNonConfigurationInstance();
            Log.v(TAG, "Open acceossory 1");
            openAccessory(mAccessory);
        }

        // Skips the permission listener if the user already grants the ADK
        // permission previously in the app.
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                Log.v(TAG, "Open accessory 2");
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
            mUsingUsb = true;
        } else {
            Log.d(TAG, "accessory is null");
            mUsingUsb = false;
        }

    }

    /**
     * Closes the accessories and unregister the USB listener at the end of
     * tests.
     */
    public void closeConnection() {
        closeAccessory();
        mActivity.unregisterReceiver(mUsbReceiver);
    }

    protected void finalize ()  {
        if (mUsingUsb) {
            closeConnection();
        }
    }

    /**
     * Runs the Auto Lock tests. A total of 19 tests have been coded and
     * included. Developers can freely comment out tests not interested. In
     * the release version, all tests will be executed.
     */
    @Override
    public synchronized void run(int index){
        if (index == 0) {
            for (int i = 1; i < mNumTests; ++i) {
                run(i);
            }
            return;
        }

        Log.v(TAG, "AutoLockTest thread started!");

        if (mUsingUsb && (!mSetupReady)) {
            // USB connection is not set up. Locks thread and wait.
            Log.v(TAG, "Setup not ready, waiting");
            synchronized (mUsbSetup) {
                try{
                    Log.v(TAG, "Start waiting for Image");
                    mUsbSetup.wait();
                } catch (InterruptedException e) {
                    Log.v(TAG, "Callback wait fails!");
                }
            }
        }

        // Restarts the camera intance and attach the preview to the corrent
        // UI elements.
        restartCamera();
        startPreview();

        mTestCount = 0;
        switch (index) {
            case 1:
                Log.v(TAG, "SP -> TP1 -> SP -> +1 -> Lock -> -1 -> TP2");
                test0();
                Log.v(TAG, "SP -> TP1 -> SP -> Lock -> +1 -> TP2 -> -1");
                test1();
                Log.v(TAG, "SP -> Lock -> +1 -> TP1 -> SP -> -1 -> Lock -> TP2");
                test2();
                Log.v(TAG, "SP -> Lock -> +1 -> TP1 -> SP -> Lock -> -1 -> TP2");
                test3();
                break;
            case 2:
                Log.v(TAG, "SP -> +1 -> TP1 -> -1 -> Lock -> SP -> TP2");
                test4();
                Log.v(TAG, "SP -> +1 -> TP1 -> Lock -> SP -> -1 -> TP2");
                test5();
                Log.v(TAG, "SP -> TP1 -> +1 -> Lock -> SP -> -1 -> TP2");
                test6();
                Log.v(TAG, "SP -> TP1 -> +1 -> Lock -> SP -> TP2");
                test7();
                Log.v(TAG, "SP -> TP1 -> Lock -> SP -> +1 -> TP2");
                test8();
                Log.v(TAG, "SP -> +1 -> Lock -> -1 -> TP1 -> Lock -> SP -> TP2");
                test9();
                Log.v(TAG, "SP -> +1 -> Lock -> TP1 -> -1 -> Lock -> SP -> TP2");
                test10();
                Log.v(TAG, "SP -> Lock -> TP1 -> +1 -> Lock -> SP -> -1 -> TP2");
                test11();
                break;
            case 3:
                Log.v(TAG, "Restart -> Lock -> SP -> TP1 -> Restart -> Lock -> SP -> +1 -> TP2");
                test12();
                Log.v(TAG, "Restart -> Lock -> SP -> +1 -> TP1 -> -1 -> Lock -> SP -> TP2");
                test13();
                Log.v(TAG, "Restart -> Lock -> SP -> +1 -> TP1 -> Lock -> SP -> -1 -> TP2");
                test14();
                Log.v(TAG, "Restart -> Lock -> SP -> TP1 -> +1 -> Lock -> SP -> -1 -> TP2");
                test15();
                Log.v(TAG, "Restart -> Lock -> SP -> TP1 -> +1 -> Lock -> SP -> TP2");
                test16();
                Log.v(TAG, "Restart -> Lock -> SP -> TP1 -> Lock -> SP -> +1 -> TP2");
                test17();
                Log.v(TAG, "Restart -> Lock -> SP -> TP1 -> Lock -> SP -> TP2");
                test18();
                break;
            default:
                break;
        }

        releaseLock();

        Log.v(TAG, "Ready to process data");
        boolean[] testCompareResults = new boolean[2 * mTestCount];

        // Processes the data stored in the native test handler instance.
        // Stores the test results into a boolean array.
        processAutoLockTest(mTestHandler, testCompareResults);

        // Prepares the test result text output with the booelan result array.
        prepareDebugText(testCompareResults, index);
        mReferenceCompareResults.clear();
        mReferenceLogs.clear();
    }

    /**
     * Compares two images taken under the same lighting, Image 1 without AE
     * lock and Image 2 with AE locked under a bright light. Image 1 is
     * expected to be brighter than Image 2.
     * Tests whether AE lock works compared to no AE lock.
     */
    private void test0() {
        releaseLock();
        takePicture();
        startPreview();
        turnOnLight();
        setLock();
        turnOffLight();
        takePicture();
        startPreview();
        releaseLock();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Same lighting condition with one different lock");
        ++mTestCount;
    }

    /**
     * Compares two images taken under different lighting, Image 1 without AE
     * lock and Image 2 with with AE locked under the same light Image 1 is
     * taken. Image 2 is taken under a bright light. Image 1 is expected to be
     * darker than Image 2.
     * Tests whether AE lock works compared to no AE lock.
     */
    private void test1() {
        releaseLock();
        takePicture();
        startPreview();
        setLock();
        turnOnLight();
        takePicture();
        turnOffLight();
        startPreview();
        releaseLock();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("One same lock with different lighting");
        ++mTestCount;
    }

    /**
     * Compares two images taken under different light, both with AE locked
     * under the same lighting. Image 1 is taken under a brighter light.
     * Image 1 is expected to be brighter than Image 2.
     * Tests whether AE locks the exposure to the same level in the same
     * lighting condition after preview restarts.
     */
     private void test2() {
        releaseLock();
        setLock();
        turnOnLight();
        takePicture();
        startPreview();
        turnOffLight();
        setLock();
        takePicture();
        startPreview();
        releaseLock();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Same locking locations with different lighting");
        ++mTestCount;
    }

    /**
     * Compares two images taken under different light, Image 1 with AE locked
     * under normal light and Image 2 with AE locked under a bright light.
     * Image 1 is taken under a bright light and Image 2 is taken in the normal
     * lighting. Image 1 is expected to be brighter than Image 2.
     * Tests whether AE lock can adjust to change of lighting conditions.
     */
    private void test3() {
        releaseLock();
        setLock();
        turnOnLight();
        takePicture();
        startPreview();
        setLock();
        turnOffLight();
        takePicture();
        startPreview();
        releaseLock();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Different locking locations with different lighting");
        ++mTestCount;
    }

    /**
     * Compares two images taken under different lighting, Image 1 without
     * AE lock and Image 2 with AE lock set before camera preview resumes.
     * Image 1 is taken under a bright light and the light is turned off before
     * camera preview starts again. Image 1 is expected to be brighter than
     * Image 2.
     * Tests whether setting AE lock between camera preview stops and restarts
     * can retain the exposure level of the previous AE-unlocked photo.
     */
    private void test4() {
        releaseLock();
        turnOnLight();
        takePicture();
        turnOffLight();
        setLock();
        startPreview();
        takePicture();
        startPreview();
        releaseLock();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after takePicture and light change, before preview");
        ++mTestCount;
    }

    /**
     * Compares two images taken under different lighting, Image 1 without
     * AE lock and Image 2 with AE lock set before camera preview resumes.
     * Image 1 is taken under a bright light and the light is turned off after
     * preview restars but before Image 2 is taken. Image 1 is expected to be
     * brighter than Image 2.
     * Tests whether setting AE lock between camera preview stops and restarts
     * can retain the exposure level of the previous AE-unlocked photo.
     */
    private void test5() {
        releaseLock();
        turnOnLight();
        takePicture();
        setLock();
        startPreview();
        turnOffLight();
        takePicture();
        startPreview();
        releaseLock();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock between takePicture and light change, w/o light change");
        ++mTestCount;
    }

    private void test6() {
        releaseLock();
        takePicture();
        turnOnLight();
        setLock();
        startPreview();
        turnOffLight();
        takePicture();
        startPreview();
        releaseLock();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add("Lock after takePicture and light change, before preview.");
        ++mTestCount;
    }

    private void test7() {
        releaseLock();
        takePicture();
        turnOnLight();
        setLock();
        startPreview();
        takePicture();
        startPreview();
        releaseLock();
        turnOffLight();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after takePicture and light change, before preview.");
        ++mTestCount;
    }

    private void test8() {
        releaseLock();
        takePicture();
        setLock();
        startPreview();
        turnOnLight();
        takePicture();
        startPreview();
        releaseLock();
        turnOffLight();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after takePicture and before startPreview.");
        ++mTestCount;
    }

    private void test9() {
        releaseLock();
        turnOnLight();
        setLock();
        turnOffLight();
        takePicture();
        setLock();
        startPreview();
        takePicture();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test10() {
        releaseLock();
        turnOnLight();
        setLock();
        takePicture();
        turnOffLight();
        setLock();
        startPreview();
        takePicture();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test11() {
        releaseLock();
        setLock();
        takePicture();
        turnOnLight();
        setLock();
        startPreview();
        turnOffLight();
        takePicture();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test12() {
        //"Restart -> Lock -> SP -> TP1 -> Restart -> Lock -> SP -> +1 -> TP2"
        restartCamera();
        setLock();
        startPreview();
        takePicture();
        releaseLock();
        restartCamera();
        setLock();
        startPreview();
        turnOnLight();
        takePicture();
        releaseLock();
        turnOffLight();
        startPreview();
        //mTestCamera.release();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock before first preview");
        ++mTestCount;
    }

    private void test13() {
        //"Restart -> Lock -> SP -> +1 -> TP1 -> -1 -> Lock -> SP -> TP2"
        restartCamera();
        setLock();
        startPreview();
        turnOnLight();
        takePicture();
        turnOffLight();
        setLock();
        startPreview();
        takePicture();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test14() {
        //"Restart -> Lock -> SP -> +1 -> TP1 -> Lock -> SP -> -1 -> TP2"
        restartCamera();
        setLock();
        startPreview();
        turnOnLight();
        takePicture();
        setLock();
        startPreview();
        turnOffLight();
        takePicture();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(true);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test15() {
        //"Restart -> Lock -> SP -> TP1 -> +1 -> Lock -> SP -> -1 -> TP2"
         restartCamera();
        setLock();
        startPreview();
        takePicture();
        turnOnLight();
        setLock();
        startPreview();
        turnOffLight();
        takePicture();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test16() {
        //"Restart -> Lock -> SP -> TP1 -> +1 -> Lock -> SP -> TP2"
        restartCamera();
        setLock();
        startPreview();
        takePicture();
        turnOnLight();
        setLock();
        startPreview();
        takePicture();
        turnOffLight();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test17() {
        //"Restart -> Lock -> SP -> TP1 -> Lock -> SP -> +1 -> TP2"
        restartCamera();
        setLock();
        startPreview();
        takePicture();
        setLock();
        startPreview();
        turnOnLight();
        takePicture();
        turnOffLight();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(false);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    private void test18() {
        //"Restart -> Lock -> SP -> TP1 -> Lock -> SP -> TP2"
        restartCamera();
        setLock();
        startPreview();
        takePicture();
        setLock();
        startPreview();
        takePicture();
        releaseLock();
        startPreview();
        mReferenceCompareResults.add(false);
        mReferenceCompareResults.add(true);
        mReferenceLogs.add("Lock after first lock with changing light");
        ++mTestCount;
    }

    /**
     * Restarts the camera by releasing the current instance and get a new
     * instance. Also connects this new camera instance's preview to the proper
     * UI surfaceview.
     */
    private void restartCamera() {
        Log.v(TAG, "Restarting Camera");

        mTestCamera.release();
        Log.v(TAG, "Camera released");

        try {
            mTestCamera = Camera.open(mActivity.getCameraIdx());
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to open the camera", e);
        }

        Camera.Parameters params = mTestCamera.getParameters();
        params.setPictureFormat(ImageFormat.JPEG);
        params.setPictureSize(640, 480);
        mTestCamera.setParameters(params);

        try {
            mTestCamera.setPreviewDisplay(super.getCameraView().getHolder());
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect camera to display: " + e);
        }
    }

    /**
     * Starts Camera preview with a delay of 2 seconds to let it adjust to
     * the lighting condition.
     */
    private void startPreview() {
        mTestCamera.startPreview();
        try{
            Log.v(TAG, "Waiting");
            Thread.sleep(2000);
            Log.v(TAG, "END Waiting");
        } catch (InterruptedException e){}
    }

    /**
     * Sends command to ADK to turn on all the LED lights to white.
     * Waits for 4 seconds for the camera to adjust to the new lighting.
     */
    private void turnOnLight() {
        Log.v(TAG, "Turn on light");
        if (mUsingUsb) {
            byte[] buffer = new byte[3];

            buffer[0] = (byte) 3;
            buffer[1] = (byte) 0;
            buffer[2] = (byte) 1;
            if (mOutputStream != null && buffer[1] != -1) {
                try {
                    mOutputStream.write(buffer);
                } catch (IOException e) {
                    Log.e(TAG, "write failed", e);
                }
            }
        } else {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mActivity.getApplicationContext(), "Turn on light!", 4).show();

                }
            });
        }

        try{
            Log.v(TAG, "Waiting, Please Turn on light");
            Thread.sleep(LONG_SLEEP);
            Log.v(TAG, "END Waiting");
        } catch (InterruptedException e){}
    }

    /**
     * Sends command to ADK to turn off all LED lights.
     * Waits for 4 seconds for the camera to adjust to the new lighting.
     */
    private void turnOffLight() {
        Log.v(TAG, "Turn off light");
        if (mUsingUsb) {
            byte[] buffer = new byte[3];

            buffer[0] = (byte) 3;
            buffer[1] = (byte) 0;
            buffer[2] = (byte) 0;
            if (mOutputStream != null && buffer[1] != -1) {
                try {
                    mOutputStream.write(buffer);
                } catch (IOException e) {
                    Log.e(TAG, "write failed", e);
                }
            }
        } else {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mActivity.getApplicationContext(), "Turn off light!", 4).show();

                }
            });
        }

        try{
            Log.v(TAG, "Waiting, Please Turn off light");
            Thread.sleep(LONG_SLEEP);
            Log.v(TAG, "END Waiting");
        } catch (InterruptedException e){}
    }

    /**
     * Sets the Auto Exposure Lock.
     * Waits for 2 seonds for the lock to function.
     */
    private void setLock() {
        Camera.Parameters params = mTestCamera.getParameters();

        params.setAutoExposureLock(true);
        params.setAutoWhiteBalanceLock(true);
        mTestCamera.setParameters(params);
        try{
            Log.v(TAG, "Waiting to set lock");
            Thread.sleep(2000);
            Log.v(TAG, "END Waiting");
        } catch (InterruptedException e){}
    }

    /**
     * Releases the Auto Exposure Lock.
     * Waits for 4 seconds afterwards for the Auto Exposure to be adjusted
     * to the lighting condition.
     */
    private void releaseLock() {
        Camera.Parameters params = mTestCamera.getParameters();

        params.setAutoExposureLock(false);
        params.setAutoWhiteBalanceLock(false);
        mTestCamera.setParameters(params);
        try{
            Log.v(TAG, "Waiting to release lock");
            Thread.sleep(LONG_SLEEP);
            Log.v(TAG, "END Waiting");
        } catch (InterruptedException e){}

    }

    /**
     * Takes a picture and locks thread until the picture callback finishes.
     */
    private void takePicture(){
        mTestCamera.takePicture(null, null, null, mTestJpegListener);

        synchronized (mProcessingImage) {
            try{
                Log.v(TAG, "Start waiting for Image");
              //  System.gc();
                mProcessingImage.wait();
            } catch (InterruptedException e){
                 Log.v(TAG, "Callback wait fails!");
            }
        }
    }

    /**
     * Prepare for the result to be shown in the UI. The result for each single
     * test is shown in green if it matches the reference result. It is shown
     * in red otherwise.
     */
    private void prepareDebugText(boolean[] testCompareResults, int index) {
        boolean groupTestPassed = true;
         for (int i = 0; i < mTestCount; ++i) {
              String testLog;
              boolean testPassed = true;
              testLog = mReferenceLogs.get(i);
              mDebugText += (testLog + "<br/>");

              if (testCompareResults[i * 2] == mReferenceCompareResults.get(i * 2)) {
                  mDebugText += String.format(
                      "Picture 1 brighter than Picture 2 is %b \n",
                      testCompareResults[i * 2]);
              } else {
                  mDebugText += String.format(
                      "Picture 1 brighter than Picture 2 is %b \n",
                      testCompareResults[i * 2]);
                  testPassed = false;
              }

              if (testCompareResults[i * 2 + 1] == mReferenceCompareResults.get(i * 2 + 1)) {
                  mDebugText += String.format(
                      "Picture 1 is equivalent to Picture 2 is %b \n",
                      testCompareResults[i * 2 + 1]);
              } else {
                  mDebugText += String.format(
                      "Picture 1 is equivalent to Picture 2 is %b \n",
                      testCompareResults[i * 2 + 1]);
                  testPassed = false;
              }

              if (testPassed) {
                  mDebugText += "Test passed! \n";
              } else {
                  mDebugText += "Test failed! \n";
                  groupTestPassed = false;
              }
         }
        if (groupTestPassed) {
            mTestResults[index] = CameraTests.CAMERA_TEST_SUCCESS;
        } else {
            mTestResults[index] = CameraTests.CAMERA_TEST_FAILURE;
        }
    }

    /**
     * Clears the debug text so that new test results can be added.
     */
    public void clearDebugText() {
        mDebugText = "";
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
        return "Auto Exposure Lock test: \n";
    }

    @Override
    public String getTestName(int index) {
        switch (index) {
            case 0:
                return "Run all tests";
            case 1:
                return "Compulsory tests";
            case 2:
                return "Recommended tests (preview behavior)";
            case 3:
                return "Optional tests (default lock)";
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

            // Decodes the camera input data into Bitmap.
            // Constructs a native image class with the image.
            inputImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            long bufferAddress = findNative(inputImage);
            Log.v(TAG, "findNative method finishes");

            // Cleans up memory taken by the Bitmap.
            data = null;
            inputImage.recycle();
            inputImage = null;
            System.gc();

            // Passes data from the native image class to the native
            // test handler.
            createAutoLockClass(bufferAddress, mTestHandler,
                                getCheckerCenter(), getCheckerRadius());

            // Unlocks the thread lock.
            synchronized (mProcessingImage) {
                mProcessingImage.notifyAll();
            }
        }
    };

    private native long createAutoLockTest();

    private native void createAutoLockClass(long bufferAddress, long handlerAddress,
                                            long checkerCenterAddress,
                                            long checkerRadiusAddress);

    private native void processAutoLockTest(long handlerAddress, boolean[] testCompareResults);

    static {
        System.loadLibrary("cameraanalyzer");
    }
}

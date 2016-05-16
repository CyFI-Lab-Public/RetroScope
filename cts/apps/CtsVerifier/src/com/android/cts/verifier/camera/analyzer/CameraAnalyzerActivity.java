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

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.Context;

import java.io.IOException;
import java.lang.Thread;
import java.util.List;

/**
 * Controls the UI activities of the camera quality test app. It is created
 * as soon as the app started. Users can launch different quality tests with
 * the buttons in the UI. This class will manage the threading for different
 * tests. Also it will provide debug output or debug text results for tests.
 */
public class CameraAnalyzerActivity extends PassFailButtons.Activity {

    private static final String TAG = "CameraAnalyzer";
    private SurfaceView mCameraView;
    private ImageView mResultView;
    private Button mFindCheckerButton;
    private Button mExposureCompensationButton;
    private Button mWhiteBalanceButton;
    private Button mAutoLockButton;
    private Button mMeteringButton;
    private ListView mTestList;
    private TwoColumnAdapter mAdapter;

    private Camera mCamera;
    private int mCameraIdx = 0;
    private boolean mIsCameraOpen = false;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    private boolean mProcessingPicture = false;
    private boolean mTestInProgress = false;
    private final Object mProcessingTest = new Object();
    private CameraTests mCurrentTest = null;

    private long mCheckerCenterAddress;
    private long mCheckerRadiusAddress;

    private String mResultReport = "";
    static final String[] TESTS = new String[] {"Test1", "Test2"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ca_main);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.camera_analyzer, R.string.ca_info, -1);

        mFindCheckerButton = (Button) findViewById(R.id.findcheckerboardbutton);
        mExposureCompensationButton = (Button) findViewById(R.id.exposurecompensationbutton);
        mWhiteBalanceButton = (Button) findViewById(R.id.whitebalancebutton);
        mAutoLockButton = (Button) findViewById(R.id.lockbutton);
        mMeteringButton = (Button) findViewById(R.id.meteringbutton);
        mCameraView = (SurfaceView) findViewById(R.id.cameraview);
        mResultView = (ImageView) findViewById(R.id.resultview);
        mTestList = (ListView) findViewById(R.id.ca_tests);
        mAdapter = new TwoColumnAdapter(this);

        // Initialize the list view.
        initializeAdapter();
        mTestList.setAdapter(mAdapter);
        mTestList.setOnItemClickListener(mListListener);

        mFindCheckerButton.setOnClickListener(mFindCheckerListener);
        mExposureCompensationButton.setOnClickListener(mExposureCompensationListener);
        mWhiteBalanceButton.setOnClickListener(mWhiteBalanceListener);
        mAutoLockButton.setOnClickListener(mAutoLockListener);
        mMeteringButton.setOnClickListener(mMeteringListener);
        mCameraView.getHolder().addCallback(mSurfaceChangeListener);

        // Disables all test buttons except the color checker test one.
        // They will be enabled after the color checker is located.
        mExposureCompensationButton.setEnabled(false);
        mWhiteBalanceButton.setEnabled(false);
        mAutoLockButton.setEnabled(false);
        mMeteringButton.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        openCamera(mCameraIdx);
        Camera.Parameters params = mCamera.getParameters();
        params.setPictureFormat(ImageFormat.JPEG);
        params.setPictureSize(640, 480);
        mCamera.setParameters(params);
        Log.v(TAG, "Set resolution to 640*480");
    }

    @Override
    public void onPause() {
        super.onPause();
        CameraTests.getCamera().release();
        mIsCameraOpen = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ca_menu, menu);

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; ++camIdx) {
            MenuItem cameraMenuItem = menu.add(0, camIdx, Menu.NONE,
                                               String.format("Open Camera %d", camIdx));
        }
      return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != mCameraIdx) {
            mCameraIdx = item.getItemId();
            new SwitchCameraTask().execute(mCameraIdx);
        }
        return false;
    }

    private class SwitchCameraTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... camIdx) {
            if (mTestInProgress) {
                synchronized (mProcessingTest) {
                    try{
                        Log.v(TAG, "Waiting for test to finish");
                        mProcessingTest.wait();
                    } catch (InterruptedException e){
                         Log.v(TAG, "test wait fails!");
                    }
                }
            }

            openCamera((int)camIdx[0]);
            return null;
        }
    }

    private synchronized void openCamera(int camIdx) {
        if (mIsCameraOpen) {
            CameraTests.getCamera().release();
            Log.v(TAG, "Releasing the cameratests camera");
        }
        try {
            mCamera = Camera.open(camIdx);
            mIsCameraOpen = true;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to open the camera", e);
        }

        try {
            mCamera.setPreviewDisplay(mCameraView.getHolder());
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect camera to display: " + e);
        }
        mCamera.startPreview();
        CameraTests.setCamera(mCamera);

        ColorCheckerTest.getSingletonTest().updateCamera();
        WhiteBalanceTest.getSingletonTest().updateCamera();
        ExposureCompensationTest.getSingletonTest().updateCamera();
        MeteringTest.getSingletonTest().updateCamera();
        AutoLockTest.getSingletonTest().updateCamera();
    }

    public Camera getCameraInstance() {
        return mCamera;
    }

    public void disableAll() {
        mExposureCompensationButton.setEnabled(false);
        mWhiteBalanceButton.setEnabled(false);
        mAutoLockButton.setEnabled(false);
        mMeteringButton.setEnabled(false);
        mFindCheckerButton.setEnabled(false);
    }

    public void enableAll() {
        mExposureCompensationButton.setEnabled(true);
        mWhiteBalanceButton.setEnabled(true);
        mAutoLockButton.setEnabled(true);
        mMeteringButton.setEnabled(true);
        mFindCheckerButton.setEnabled(true);
    }

    /**
     * Provides an abstraction for the Camera tests. The camera tests will
     * run in the background and the results will be shown in the UI thread
     * after the tests are processed.
     */
    private class DebugOutputProcessingTask extends AsyncTask<Integer,
                                                              Integer,
                                                              Integer> {
        @Override
        protected Integer doInBackground(Integer... cameraTestIndex) {
            Log.v(TAG, "Do in Background started!");

            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK, "CameraQualityTest");
            mWakeLock.acquire();

            mTestInProgress = true;

            // Processes the camera tests one by one and publishes their
            // debug output or debug text results after each test is done.
            mCurrentTest.run((int)cameraTestIndex[0]);
            publishProgress(cameraTestIndex);

            Log.v(TAG, "Do in Background thread returns!");
            return cameraTestIndex[0];
        }

        @Override
        protected void onProgressUpdate(Integer... cameraTestIndex) {
            Log.v(TAG, "Prepare to get debug output!");

            // Copies the debug output image or text results to the UI.
            mResultView.setImageBitmap(mCurrentTest.getDebugOutput());
            mResultReport += (mCurrentTest.getTestName() + mCurrentTest.getDebugText());
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Integer cameraTestIndex) {

            // If the test is to find the color checker, copy the memory
            // address of the found color checker centers and radius to the
            // CameraTests class' static fields.
            if (mCurrentTest.copyCheckerAddress()) {
                mCheckerCenterAddress = CameraTests.getCheckerCenter();
                mCheckerRadiusAddress = CameraTests.getCheckerRadius();
            }

            if (mCurrentTest.copyCheckerAddress() ||
                !mCurrentTest.getTestName().contains("Color Checker")) {
                // Enables the button of all other tests after the color checker
                // is found. Now the user can start all other available tests.
                enableAll();
            }

            mWakeLock.release();
            mTestInProgress = false;
            synchronized (mProcessingTest) {
                mProcessingTest.notifyAll();
            }

        }
    }

    // Creates and runs a new test to find color checker in the captured image.
    // It is invoked when users press the Find color checker button in the UI.
    private View.OnClickListener mFindCheckerListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "Running new color checker finding tests!");
            ColorCheckerTest colorCheckerTest = ColorCheckerTest.getSingletonTest();

            mCurrentTest = colorCheckerTest;
            initializeAdapter();
        }
    };

    // Creates and runs a new test to test the exposure compensation function.
    // It is invoked when users press the Exposure Compensation Test button
    // in the UI.
    private View.OnClickListener mExposureCompensationListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "Running new exposure compensation tests!");

            ExposureCompensationTest exposureCompensationTest =
                    ExposureCompensationTest.getSingletonTest();

            mCurrentTest = exposureCompensationTest;
            initializeAdapter();

            // Loads the memory address of the checker centers and radius
            // from the this class and set the two values for the new test.
            ExposureCompensationTest.setCheckerAddress(mCheckerCenterAddress,
                                                   mCheckerRadiusAddress);
        }
    };

    // Creates and runs a new test to test the white balance function.
    // It is invoked when users press the White Balance Test button in the UI.
    private View.OnClickListener mWhiteBalanceListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "Running new white balance tests!");

            WhiteBalanceTest whiteBalanceTest = WhiteBalanceTest.getSingletonTest();

            mCurrentTest = whiteBalanceTest;
            initializeAdapter();

            // Loads the memory address of the checker centers and radius
            // from the this class and set the two values for the new test.
            WhiteBalanceTest.setCheckerAddress(mCheckerCenterAddress, mCheckerRadiusAddress);
        }
    };

    // Creates and runs a new test to test the camera metering function.
    // It is invoked when users press the Metering Test button in the UI.
    private View.OnClickListener mMeteringListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "Running new metering tests!");

            MeteringTest meteringTest = MeteringTest.getSingletonTest();

            mCurrentTest = meteringTest;
            initializeAdapter();

            // Loads the memory address of the checker centers and radius
            // from the this class and set the two values for the new test.
            MeteringTest.setCheckerAddress(mCheckerCenterAddress, mCheckerRadiusAddress);
        }
    };

    // Creates and runs new tests to test the camera auto exposure lock.
    // It is invoked when users press the AWB and AE Lock Test button
    // in the UI.
    private View.OnClickListener mAutoLockListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "Running New auto exposure/wb lock tests!");

            // Loads the memory address of the checker centers and radius
            // from the this class and set the two values for the new test.
            AutoLockTest.setCheckerAddress(mCheckerCenterAddress, mCheckerRadiusAddress);

            // Construct all base case test scenearios for the Auto Lock test.
            // Detailed documentation on each test can be found in native code.
            AutoLockTest autoLockTest = AutoLockTest.getSingletonTest();
            autoLockTest.setActivity(CameraAnalyzerActivity.this);

            mCurrentTest = autoLockTest;
            initializeAdapter();

        }
    };

    // Creates a list listner that launches the experiment with the user's click
    private AdapterView.OnItemClickListener mListListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.v(TAG, String.format("Item %d selected!", position));
            if (!mTestInProgress) {
                DebugOutputProcessingTask captureTask = new DebugOutputProcessingTask();
                disableAll();
                captureTask.execute(position);
            }
        }
    };

    private SurfaceHolder.Callback mSurfaceChangeListener =
            new SurfaceHolder.Callback() {

        // Sets the aspect ratio of the camera preview to 4:3
        @Override
        public void surfaceChanged(SurfaceHolder holder,
                                   int format,
                                   int width,
                                   int height) {
            int x = mCameraView.getWidth();
            int y = mCameraView.getHeight();
            Log.v(TAG, String.format("Measures are %d, %d", x, y));

            if ( x > 4.0 / 3.0 * y) {
                android.view.ViewGroup.LayoutParams lp = mCameraView.getLayoutParams();
                lp.height =  y;
                lp.width = (int)(4.0 / 3.0 * lp.height);
                Log.v(TAG, String.format("params are %d, %d", lp.width, lp.height));
                mCameraView.setLayoutParams(lp);
            }

            try {
                mCamera.setPreviewDisplay(mCameraView.getHolder());
            } catch (IOException e) {
                throw new RuntimeException("Unable to connect camera to display: " + e);
            }
            CameraTests.setCameraView(mCameraView);
            mCamera.startPreview();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {}
    };

    @Override
    public String getTestDetails() {
        return mResultReport;
    }

    class TwoColumnAdapter extends ArrayAdapter<String> {
        TwoColumnAdapter(Context context) {
            super(context, R.layout.ca_row);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.ca_row, parent, false);
            ImageView iconField = (ImageView) row.findViewById(R.id.caTestIcon);
            TextView nameField = (TextView) row.findViewById(R.id.caTestName);
            TextView resultField = (TextView) row.findViewById(R.id.caTestResult);
            if (mCurrentTest != null) {
                nameField.setText(mCurrentTest.getTestName(position));
                int result = mCurrentTest.getResult(position);
                switch (result) {
                    case CameraTests.CAMERA_TEST_SUCCESS:
                        resultField.setText("Success");
                        iconField.setBackgroundColor(Color.rgb(0x99,0xCC,00));
                        resultField.setTextColor(Color.rgb(0x99,0xCC,00));
                        break;
                    case CameraTests.CAMERA_TEST_FAILURE:
                        resultField.setText("Failed!");
                        iconField.setBackgroundColor(Color.rgb(0xFF,0x44,0x44));
                        resultField.setTextColor(Color.rgb(0xFF,0x44,0x44));
                        break;
                    case CameraTests.CAMERA_TEST_NOT_RUN:
                        resultField.setText("Tap to run");
                        iconField.setBackgroundColor(Color.rgb(0x00,0x99,0xCC));
                        resultField.setTextColor(Color.rgb(0x33,0xB5,0xE5));
                        break;
                }
            }
            return row;
        }
    }

    private void initializeAdapter() {
        mAdapter.clear();
        if (mCurrentTest != null) {
            for (int i = 0; i < mCurrentTest.getNumTests(); ++i) {
                mAdapter.add(mCurrentTest.getTestName(i));
            }
        }
    }

    public int getCameraIdx() {
        return mCameraIdx;
    }
}

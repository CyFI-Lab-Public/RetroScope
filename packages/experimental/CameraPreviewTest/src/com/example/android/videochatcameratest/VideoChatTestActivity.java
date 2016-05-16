// Copyright 2010 Google Inc.
// All Rights Reserved.

package com.example.android.videochatcameratest;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.List;

public class VideoChatTestActivity extends Activity {

    static final private int NUM_CAMERA_PREVIEW_BUFFERS = 2;
    static final boolean sRunningOnHoneycomb;
    static final private String TAG = "VideoChatTest";
    TextView mTextStatusHistory;
    static {
        sRunningOnHoneycomb =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
    }

    public VideoChatTestActivity() {
    }

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.videochatcameratest_activity);

        FrameLayout fl = (FrameLayout)findViewById(R.id.previewFrame);

        if (sRunningOnHoneycomb) {
            fl.addView(new SurfaceTextureView(this));
        } else {
            fl.addView(new CameraPreviewView(this));
        }

        ((Button) findViewById(R.id.gobutton)).setOnClickListener(mGoListener);

        ((TextView)findViewById(R.id.statushistory)).setVerticalScrollBarEnabled(true);
        mTextStatusHistory = (TextView) findViewById(R.id.statushistory);

        logMessage("Display Orientation " + getDisplayOrientation());
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            dumpCameraCaps(i);
        }
    }

    private void logMessage(String message) {
        Log.v(TAG, message);
        mTextStatusHistory.append(message + "\r\n");
    }

    public int getCameraOrientation(int id) {
        Camera.CameraInfo info =
            new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        return info.orientation;
    }

    private void dumpCameraCaps(int id) {
        Camera cam = Camera.open(id);
        Camera.Parameters params = cam.getParameters();
        List<Integer> formats = params.getSupportedPreviewFormats();
        List<int[]> frameRates = params.getSupportedPreviewFpsRange();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        logMessage("Camera " + id);
        logMessage("Orientation " + getCameraOrientation(id));
        logMessage("Sizes");
        for (Size size : sizes) {
            logMessage(size.width + "x" + size.height);
        }
        logMessage("frameRates");
        for (int[] rates : frameRates) {
            logMessage(rates[0] + "-" + rates[1]);
        }
        logMessage("formats");
        for (Integer format : formats) {
            logMessage(format.toString());
        }
        cam.release();
    }
    /**
     * Called when the activity is about to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    private int getDisplayOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return degrees;
    }

    /**
     * A call-back for when the user presses the back button.
     */
    OnClickListener mGoListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int degrees = getDisplayOrientation();
            new CameraTestRunner().execute(new Integer[] { degrees });
        }
    };
    
    private class CameraTestRunner extends AsyncTask<Integer, String, Void> {

        TextView mTextStatus;
        private int mDisplayOrientation;
        private volatile boolean mClearStatusOnNextUpdate;

        @Override
        protected Void doInBackground(Integer... params) {
            mDisplayOrientation = params[0];
            mTextStatus = (TextView) findViewById(R.id.status);
            boolean testFrontCamera =
                    ((CheckBox) findViewById(R.id.frontcameracheckbox)).isChecked();
            boolean testBackCamera = ((CheckBox) findViewById(R.id.backcameracheckbox)).isChecked();
            boolean testQVGA = ((CheckBox) findViewById(R.id.qvgacheckbox)).isChecked();
            boolean testVGA = ((CheckBox) findViewById(R.id.vgacheckbox)).isChecked();
            boolean test15fps = ((CheckBox) findViewById(R.id.fps15checkbox)).isChecked();
            boolean test30fps = ((CheckBox) findViewById(R.id.fps30checkbox)).isChecked();
            boolean testRotate0 = ((CheckBox) findViewById(R.id.rotate0checkbox)).isChecked();
            boolean testRotate90 = ((CheckBox) findViewById(R.id.rotate90checkbox)).isChecked();
            boolean testRotate180 = ((CheckBox) findViewById(R.id.rotate180checkbox)).isChecked();
            boolean testRotate270 = ((CheckBox) findViewById(R.id.rotate270checkbox)).isChecked();
            boolean testRotateAuto = ((CheckBox) findViewById(R.id.rotateautocheckbox)).isChecked();

            ArrayList<Integer> setDisplayOrentationAngles = new ArrayList<Integer>();

            if (testRotate0) {
                setDisplayOrentationAngles.add(0);
            }
            if (testRotate90) {
                setDisplayOrentationAngles.add(90);
            }
            if (testRotate180) {
                setDisplayOrentationAngles.add(180);
            }
            if (testRotate270) {
                setDisplayOrentationAngles.add(270);
            }
            if (testRotateAuto) {
                setDisplayOrentationAngles.add(-1);
            }

            final int widths[] = new int[] {320, 640};
            final int heights[] = new int[] {240, 480};

            final int framerates[] = new int[] {15, 30};

            ArrayList<Integer> whichCameras = new ArrayList<Integer>();
            int numCameras = Camera.getNumberOfCameras();
            if (testFrontCamera) {
                for (int i = 0; i < numCameras; i++) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        whichCameras.add(i);
                        break;
                    }
                }
            }
            if (testBackCamera) {
                for (int i = 0; i < numCameras; i++) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        whichCameras.add(i);
                        break;
                    }
                }
            }
            do {
                mClearStatusOnNextUpdate = true;
                for (Integer whichCamera : whichCameras) {
                    for (int whichResolution = 0; whichResolution < 2; whichResolution++) {
                        if (whichResolution == 0 && !testQVGA) {
                            continue;
                        }
                        if (whichResolution == 1 && !testVGA) {
                            continue;
                        }

                        for (int whichFramerate = 0; whichFramerate < 2; whichFramerate++) {
                            if (whichFramerate == 0 && !test15fps) {
                                continue;
                            }
                            if (whichFramerate == 1 && !test30fps) {
                                continue;
                            }

                            TestCamera(whichCamera, widths[whichResolution],
                                    heights[whichResolution], framerates[whichFramerate],
                                    setDisplayOrentationAngles);
                        }
                    }
                }
            } while (((CheckBox) findViewById(R.id.repeatcheckbox)).isChecked());
            // start tests

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            final String allDoneString = "Test complete"; 
            Log.v(TAG, allDoneString);
            mTextStatus.setText(allDoneString);
            mTextStatusHistory.append(allDoneString + "\r\n");
        }

        
        private class FrameCatcher implements Camera.PreviewCallback {
            public int mFrames = 0;
            private final int mExpectedSize;
            public FrameCatcher(int width, int height) {
                mExpectedSize = width * height * 3 / 2;
            }
            
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mExpectedSize != data.length) {
                    throw new UnsupportedOperationException("bad size, got " + data.length + " expected " + mExpectedSize);
                }
                mFrames++;
                camera.addCallbackBuffer(data);
            }

        }

        private void setupCallback(Camera camera, FrameCatcher catcher, int bufferSize) {
            camera.setPreviewCallbackWithBuffer(null);
            camera.setPreviewCallbackWithBuffer(catcher);
            for (int i = 0; i < NUM_CAMERA_PREVIEW_BUFFERS; i++) {
                byte [] cameraBuffer = new byte[bufferSize];
                camera.addCallbackBuffer(cameraBuffer);
            }
        }

        private int getAutoDisplayOrientation(int displayOrientationDegrees,
                int cameraId, android.hardware.Camera camera) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + displayOrientationDegrees) % 360;
                result = (360 - result) % 360; // compensate the mirror
            } else { // back-facing
                result = (info.orientation - displayOrientationDegrees + 360) % 360;
            }
            return result;
        }

        protected void TestCamera(int whichCamera,
                int width, int height,
                int frameRate,
                List<Integer> setDisplayOrentationAngles) {
            String baseStatus = "Camera id " + whichCamera + " " + 
                width + "x" + height + " " +
                frameRate + "fps";
            publishProgress("Initializing " + baseStatus);
            String status = "";
            boolean succeeded = true;
            Log.v(TAG, "Start test -- id " + whichCamera + " " + width + "x" + height +
                    " " + frameRate + "fps");
            Camera camera;
            FrameLayout previewBlock = (FrameLayout)findViewById(R.id.previewFrame);
            SurfaceTextureView surfaceTextureView = null;
            CameraPreviewView previewView = null;
            if (sRunningOnHoneycomb) {
                surfaceTextureView = (SurfaceTextureView)previewBlock.getChildAt(0);
            } else {
                previewView = (CameraPreviewView)previewBlock.getChildAt(0);
            }

            camera = Camera.open(whichCamera);
            publishProgress("Opened " + baseStatus);
            try {
                try {
                    if (sRunningOnHoneycomb) {
                        camera.setPreviewTexture(surfaceTextureView.getSurfaceTexture());
                    } else {
                        camera.setPreviewDisplay(previewView.mHolder);
                    }
                } catch (IOException exception) {
                    succeeded = false;
                    status = exception.toString();
                    return;
                }

                camera.setPreviewCallbackWithBuffer(null);
                Camera.Parameters parameters = camera.getParameters();

                publishProgress("Changing preview parameters " + width + "x" + height + baseStatus);

                parameters.setPreviewSize(width, height);
                parameters.setPreviewFormat(ImageFormat.NV21);

                parameters.setPreviewFrameRate(frameRate);
                camera.setParameters(parameters);

                publishProgress("Validating preview parameters " + baseStatus);

                parameters = camera.getParameters();
                Size setSize = parameters.getPreviewSize();
                if (setSize.width != width || setSize.height != height) {
                    status += "Bad reported size, wanted " + width + "x" + height + ", got " +
                    setSize.width + "x" + setSize.height;
                    succeeded = false;
                }

                if (parameters.getPreviewFrameRate() != frameRate) {
                    status += "Bad reported frame rate, wanted " + frameRate
                    + ", got " + parameters.getPreviewFrameRate();
                    succeeded = false;
                }

                publishProgress("Initializing callback buffers " + baseStatus);
                int imageFormat = parameters.getPreviewFormat();
                if (imageFormat != ImageFormat.NV21) {
                    status = "Bad reported image format, wanted NV21 (" + ImageFormat.NV21 +
                            ") got " + imageFormat;
                    succeeded = false;
                    throw new UnsupportedOperationException(status);
                }
                int bufferSize;
                bufferSize = setSize.width * setSize.height
                                * ImageFormat.getBitsPerPixel(imageFormat) / 8;
                int sizeWeShouldHave = (width * height * 3 / 2);
                if (bufferSize != sizeWeShouldHave) {
                    status = "Bad calculate size. Should have been " + (width * height * 3 / 2) +
                            " but got " + imageFormat;
                    succeeded = false;
                    throw new UnsupportedOperationException(status);
                }

                FrameCatcher catcher = new FrameCatcher(setSize.width, setSize.height);

                if (succeeded) {
                    publishProgress("Starting " + baseStatus);
                } else {
                    publishProgress("Starting " + baseStatus + " -- but " + status);
                }

                int numPasses;
                boolean doSetDisplayOrientation;
                if (setDisplayOrentationAngles == null || setDisplayOrentationAngles.size() == 0) {
                    numPasses = 1;
                    doSetDisplayOrientation = false;
                } else {
                    numPasses = setDisplayOrentationAngles.size();
                    doSetDisplayOrientation = true;
                }

                for (int i = 0; i < numPasses; i++) {
                    if (doSetDisplayOrientation) {
                        int rotation = setDisplayOrentationAngles.get(i);
                        if (rotation == -1) {
                            rotation = getAutoDisplayOrientation(mDisplayOrientation,
                                    whichCamera, camera);
                        }
                        publishProgress("setDisplayOrientation to " + rotation);
                        try {
                            camera.setDisplayOrientation(rotation);
                        } catch (RuntimeException exception) {
                            succeeded = false;
                            status = exception.toString();
                            return;
                        }
                    }
                    if (sRunningOnHoneycomb) {
                        surfaceTextureView.resetFrameCounter();
                        surfaceTextureView.setCameraEnabled(true);
                    } else {
                        setupCallback(camera, catcher, bufferSize);
                    }
                    camera.startPreview();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException exception) {
                        succeeded = false;
                        status = exception.toString();
                        return;
                    }
                    if (sRunningOnHoneycomb) {
                        surfaceTextureView.setCameraEnabled(false);
                    } else {
                        camera.setPreviewCallbackWithBuffer(null);
                    }
                    camera.stopPreview();
                }

                int frames;
                if (sRunningOnHoneycomb) {
                    frames = surfaceTextureView.getFrameCounter();
                } else {
                    frames = catcher.mFrames;
                }
                if (frames == 0) {
                    succeeded = false;
                    publishProgress("Preview callback received no frames from " + baseStatus);
                } else {
                    publishProgress("Preview callback got " + frames + " frames (~" +
                            Math.round(((double)frames)/(5.0 * numPasses)) + "fps) " +
                            baseStatus);
                }
                try {
                    camera.setPreviewDisplay(null);
                } catch (IOException exception) {
                    succeeded = false;
                    status = exception.toString();
                    return;
                }
            } finally {
                Log.v(TAG, "Releasing camera");

                if (succeeded) {
                    publishProgress("Success " + baseStatus);
                } else {
                    publishProgress("Finished " + baseStatus + " -- but " + status);
                }

                camera.release();
            }
        }
        
        @Override
        protected void onProgressUpdate(String... message) {
            if (mClearStatusOnNextUpdate) {
                mClearStatusOnNextUpdate = false;
                mTextStatusHistory.setText("");
            }
            Log.v(TAG, message[0]);
            mTextStatus.setText(message[0]);
            mTextStatusHistory.append(message[0] + "\r\n");
        }
    }
}

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

package com.android.camera2.its;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Rational;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingCameraManager;
import com.android.ex.camera2.blocking.BlockingCameraManager.BlockingOpenException;
import com.android.ex.camera2.blocking.BlockingStateListener;

import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ItsService extends Service {
    public static final String TAG = ItsService.class.getSimpleName();
    public static final String PYTAG = "CAMERA-ITS-PY";

    // Supported intents
    public static final String ACTION_CAPTURE = "com.android.camera2.its.CAPTURE";
    public static final String ACTION_3A = "com.android.camera2.its.3A";
    public static final String ACTION_GETPROPS = "com.android.camera2.its.GETPROPS";
    private static final int MESSAGE_CAPTURE = 1;
    private static final int MESSAGE_3A = 2;
    private static final int MESSAGE_GETPROPS = 3;

    // Timeouts, in seconds.
    public static final int TIMEOUT_CAPTURE = 10;
    public static final int TIMEOUT_3A = 10;

    // State transition timeouts, in ms.
    private static final long TIMEOUT_IDLE_MS = 2000;
    private static final long TIMEOUT_STATE_MS = 500;

    private static final int MAX_CONCURRENT_READER_BUFFERS = 8;

    public static final String REGION_KEY = "regions";
    public static final String REGION_AE_KEY = "ae";
    public static final String REGION_AWB_KEY = "awb";
    public static final String REGION_AF_KEY = "af";
    public static final String TRIGGER_KEY = "triggers";
    public static final String TRIGGER_AE_KEY = "ae";
    public static final String TRIGGER_AF_KEY = "af";

    private CameraManager mCameraManager = null;
    private HandlerThread mCameraThread = null;
    private BlockingCameraManager mBlockingCameraManager = null;
    private BlockingStateListener mCameraListener = null;
    private CameraDevice mCamera = null;
    private ImageReader mCaptureReader = null;
    private CameraCharacteristics mCameraCharacteristics = null;

    private HandlerThread mCommandThread;
    private Handler mCommandHandler;
    private HandlerThread mSaveThread;
    private Handler mSaveHandler;
    private HandlerThread mResultThread;
    private Handler mResultHandler;

    private ConditionVariable mInterlock3A = new ConditionVariable(true);
    private volatile boolean mIssuedRequest3A = false;
    private volatile boolean mConvergedAE = false;
    private volatile boolean mConvergedAF = false;
    private volatile boolean mConvergedAWB = false;

    private CountDownLatch mCaptureCallbackLatch;

    public interface CaptureListener {
        void onCaptureAvailable(Image capture);
    }

    public abstract class CaptureResultListener extends CameraDevice.CaptureListener {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        try {
            // Get handle to camera manager.
            mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            if (mCameraManager == null) {
                throw new ItsException("Failed to connect to camera manager");
            }
            mBlockingCameraManager = new BlockingCameraManager(mCameraManager);
            mCameraListener = new BlockingStateListener();

            // Open the camera device, and get its properties.
            String[] devices;
            try {
                devices = mCameraManager.getCameraIdList();
                if (devices == null || devices.length == 0) {
                    throw new ItsException("No camera devices");
                }
            } catch (CameraAccessException e) {
                throw new ItsException("Failed to get device ID list", e);
            }

            mCameraThread = new HandlerThread("ItsCameraThread");
            try {
                mCameraThread.start();
                Handler cameraHandler = new Handler(mCameraThread.getLooper());

                // TODO: Add support for specifying which device to open.
                mCamera = mBlockingCameraManager.openCamera(devices[0], mCameraListener,
                        cameraHandler);
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(devices[0]);
            } catch (CameraAccessException e) {
                throw new ItsException("Failed to open camera", e);
            } catch (BlockingOpenException e) {
                throw new ItsException("Failed to open camera (after blocking)", e);
            }

            // Create a thread to receive images and save them.
            mSaveThread = new HandlerThread("SaveThread");
            mSaveThread.start();
            mSaveHandler = new Handler(mSaveThread.getLooper());

            // Create a thread to receive capture results and process them
            mResultThread = new HandlerThread("ResultThread");
            mResultThread.start();
            mResultHandler = new Handler(mResultThread.getLooper());

            // Create a thread to process commands.
            mCommandThread = new HandlerThread("CaptureThread");
            mCommandThread.start();
            mCommandHandler = new Handler(mCommandThread.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    try {
                        switch (msg.what) {
                            case MESSAGE_CAPTURE:
                                doCapture((Uri) msg.obj);
                                break;
                            case MESSAGE_3A:
                                do3A((Uri) msg.obj);
                                break;
                            case MESSAGE_GETPROPS:
                                doGetProps();
                                break;
                            default:
                                throw new ItsException("Unknown message type");
                        }
                        Log.i(PYTAG, "### DONE");
                        return true;
                    }
                    catch (ItsException e) {
                        Log.e(TAG, "Script failed: ", e);
                        Log.e(PYTAG, "### FAIL");
                        return true;
                    }
                }
            });
        } catch (ItsException e) {
            Log.e(TAG, "Script failed: ", e);
            Log.e(PYTAG, "### FAIL");
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (mCommandThread != null) {
                mCommandThread.quit();
                mCommandThread = null;
            }
            if (mSaveThread != null) {
                mSaveThread.quit();
                mSaveThread = null;
            }
            if (mCameraThread != null) {
                mCameraThread.quitSafely();
                mCameraThread = null;
            }
            try {
                mCamera.close();
            } catch (Exception e) {
                throw new ItsException("Failed to close device");
            }
        } catch (ItsException e) {
            Log.e(TAG, "Script failed: ", e);
            Log.e(PYTAG, "### FAIL");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.i(PYTAG, "### RECV");
            String action = intent.getAction();
            if (ACTION_CAPTURE.equals(action)) {
                Uri uri = intent.getData();
                Message m = mCommandHandler.obtainMessage(MESSAGE_CAPTURE, uri);
                mCommandHandler.sendMessage(m);
            } else if (ACTION_3A.equals(action)) {
                Uri uri = intent.getData();
                Message m = mCommandHandler.obtainMessage(MESSAGE_3A, uri);
                mCommandHandler.sendMessage(m);
            } else if (ACTION_GETPROPS.equals(action)) {
                Uri uri = intent.getData();
                Message m = mCommandHandler.obtainMessage(MESSAGE_GETPROPS, uri);
                mCommandHandler.sendMessage(m);
            } else {
                throw new ItsException("Unhandled intent: " + intent.toString());
            }
        } catch (ItsException e) {
            Log.e(TAG, "Script failed: ", e);
            Log.e(PYTAG, "### FAIL");
        }
        return START_STICKY;
    }

    private ImageReader.OnImageAvailableListener
            createAvailableListener(final CaptureListener listener) {
        return new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image i = null;
                try {
                    i = reader.acquireNextImage();
                    listener.onCaptureAvailable(i);
                } finally {
                    if (i != null) {
                        i.close();
                    }
                }
            }
        };
    }

    private ImageReader.OnImageAvailableListener
            createAvailableListenerDropper(final CaptureListener listener) {
        return new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image i = reader.acquireNextImage();
                i.close();
            }
        };
    }

    private void doGetProps() throws ItsException {
        String fileName = ItsUtils.getMetadataFileName(0);
        File mdFile = ItsUtils.getOutputFile(ItsService.this, fileName);
        ItsUtils.storeCameraCharacteristics(mCameraCharacteristics, mdFile);
        Log.i(PYTAG,
              String.format("### FILE %s",
                            ItsUtils.getExternallyVisiblePath(ItsService.this, mdFile.toString())));
    }

    private void prepareCaptureReader(int width, int height, int format) {
        if (mCaptureReader == null
                || mCaptureReader.getWidth() != width
                || mCaptureReader.getHeight() != height
                || mCaptureReader.getImageFormat() != format) {
            if (mCaptureReader != null) {
                mCaptureReader.close();
            }
            mCaptureReader = ImageReader.newInstance(width, height, format,
                    MAX_CONCURRENT_READER_BUFFERS);
        }
    }

    private void do3A(Uri uri) throws ItsException {
        try {
            if (uri == null || !uri.toString().endsWith(".json")) {
                throw new ItsException("Invalid URI: " + uri);
            }

            // Start a 3A action, and wait for it to converge.
            // Get the converged values for each "A", and package into JSON result for caller.

            // 3A happens on full-res frames.
            android.hardware.camera2.Size sizes[] = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_AVAILABLE_JPEG_SIZES);
            int width = sizes[0].getWidth();
            int height = sizes[0].getHeight();
            int format = ImageFormat.YUV_420_888;

            prepareCaptureReader(width, height, format);
            List<Surface> outputSurfaces = new ArrayList<Surface>(1);
            outputSurfaces.add(mCaptureReader.getSurface());
            mCamera.configureOutputs(outputSurfaces);
            mCameraListener.waitForState(BlockingStateListener.STATE_BUSY,
                    TIMEOUT_STATE_MS);
            mCameraListener.waitForState(BlockingStateListener.STATE_IDLE,
                    TIMEOUT_IDLE_MS);

            // Add a listener that just recycles buffers; they aren't saved anywhere.
            ImageReader.OnImageAvailableListener readerListener =
                    createAvailableListenerDropper(mCaptureListener);
            mCaptureReader.setOnImageAvailableListener(readerListener, mSaveHandler);

            // Get the user-specified regions for AE, AWB, AF.
            // Note that the user specifies normalized [x,y,w,h], which is converted below
            // to an [x0,y0,x1,y1] region in sensor coords. The capture request region
            // also has a fifth "weight" element: [x0,y0,x1,y1,w].
            int[] regionAE = new int[]{0,0,width-1,height-1,1};
            int[] regionAF = new int[]{0,0,width-1,height-1,1};
            int[] regionAWB = new int[]{0,0,width-1,height-1,1};
            JSONObject params = ItsUtils.loadJsonFile(uri);
            if (params.has(REGION_KEY)) {
                JSONObject regions = params.getJSONObject(REGION_KEY);
                if (regions.has(REGION_AE_KEY)) {
                    int[] r = ItsUtils.getJsonRectFromArray(
                            regions.getJSONArray(REGION_AE_KEY), true, width, height);
                    regionAE = new int[]{r[0],r[1],r[0]+r[2]-1,r[1]+r[3]-1,1};
                }
                if (regions.has(REGION_AF_KEY)) {
                    int[] r = ItsUtils.getJsonRectFromArray(
                            regions.getJSONArray(REGION_AF_KEY), true, width, height);
                    regionAF = new int[]{r[0],r[1],r[0]+r[2]-1,r[1]+r[3]-1,1};
                }
                if (regions.has(REGION_AWB_KEY)) {
                    int[] r = ItsUtils.getJsonRectFromArray(
                            regions.getJSONArray(REGION_AWB_KEY), true, width, height);
                    regionAWB = new int[]{r[0],r[1],r[0]+r[2]-1,r[1]+r[3]-1,1};
                }
            }
            Log.i(TAG, "AE region: " + Arrays.toString(regionAE));
            Log.i(TAG, "AF region: " + Arrays.toString(regionAF));
            Log.i(TAG, "AWB region: " + Arrays.toString(regionAWB));

            // By default, AE and AF both get triggered, but the user can optionally override this.
            boolean doAE = true;
            boolean doAF = true;
            if (params.has(TRIGGER_KEY)) {
                JSONObject triggers = params.getJSONObject(TRIGGER_KEY);
                if (triggers.has(TRIGGER_AE_KEY)) {
                    doAE = triggers.getBoolean(TRIGGER_AE_KEY);
                }
                if (triggers.has(TRIGGER_AF_KEY)) {
                    doAF = triggers.getBoolean(TRIGGER_AF_KEY);
                }
            }

            mInterlock3A.open();
            mIssuedRequest3A = false;
            mConvergedAE = false;
            mConvergedAWB = false;
            mConvergedAF = false;
            long tstart = System.currentTimeMillis();
            boolean triggeredAE = false;
            boolean triggeredAF = false;

            // Keep issuing capture requests until 3A has converged.
            // First do AE, then do AF and AWB together.
            while (true) {

                // Block until can take the next 3A frame. Only want one outstanding frame
                // at a time, to simplify the logic here.
                if (!mInterlock3A.block(TIMEOUT_3A * 1000) ||
                        System.currentTimeMillis() - tstart > TIMEOUT_3A * 1000) {
                    throw new ItsException("3A failed to converge (timeout)");
                }
                mInterlock3A.close();

                // If not converged yet, issue another capture request.
                if ((doAE && !mConvergedAE) || !mConvergedAWB || (doAF && !mConvergedAF)) {

                    // Baseline capture request for 3A.
                    CaptureRequest.Builder req = mCamera.createCaptureRequest(
                            CameraDevice.TEMPLATE_PREVIEW);
                    req.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                    req.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                    req.set(CaptureRequest.CONTROL_CAPTURE_INTENT,
                            CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);
                    req.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    req.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
                    req.set(CaptureRequest.CONTROL_AE_LOCK, false);
                    req.set(CaptureRequest.CONTROL_AE_REGIONS, regionAE);
                    req.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_AUTO);
                    req.set(CaptureRequest.CONTROL_AF_REGIONS, regionAF);
                    req.set(CaptureRequest.CONTROL_AWB_MODE,
                            CaptureRequest.CONTROL_AWB_MODE_AUTO);
                    req.set(CaptureRequest.CONTROL_AWB_LOCK, false);
                    req.set(CaptureRequest.CONTROL_AWB_REGIONS, regionAWB);

                    // Trigger AE first.
                    if (doAE && !triggeredAE) {
                        Log.i(TAG, "Triggering AE");
                        req.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                        triggeredAE = true;
                    }

                    // After AE has converged, trigger AF.
                    if (doAF && !triggeredAF && (!doAE || (triggeredAE && mConvergedAE))) {
                        Log.i(TAG, "Triggering AF");
                        req.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                CaptureRequest.CONTROL_AF_TRIGGER_START);
                        triggeredAF = true;
                    }

                    req.addTarget(mCaptureReader.getSurface());

                    mIssuedRequest3A = true;
                    mCamera.capture(req.build(), mCaptureResultListener, mResultHandler);
                } else {
                    Log.i(TAG, "3A converged");
                    break;
                }
            }
        } catch (android.hardware.camera2.CameraAccessException e) {
            throw new ItsException("Access error: ", e);
        } catch (org.json.JSONException e) {
            throw new ItsException("JSON error: ", e);
        }
    }

    private void doCapture(Uri uri) throws ItsException {
        try {
            if (uri == null || !uri.toString().endsWith(".json")) {
                throw new ItsException("Invalid URI: " + uri);
            }

            // Parse the JSON to get the list of capture requests.
            List<CaptureRequest.Builder> requests = ItsUtils.loadRequestList(mCamera, uri);

            // Set the output surface and listeners.
            try {
                // Default:
                // Capture full-frame images. Use the reported JPEG size rather than the sensor
                // size since this is more likely to be the unscaled size; the crop from sensor
                // size is probably for the ISP (e.g. demosaicking) rather than the encoder.
                android.hardware.camera2.Size sizes[] = mCameraCharacteristics.get(
                        CameraCharacteristics.SCALER_AVAILABLE_JPEG_SIZES);
                int width = sizes[0].getWidth();
                int height = sizes[0].getHeight();
                int format = ImageFormat.YUV_420_888;

                JSONObject jsonOutputSpecs = ItsUtils.getOutputSpecs(uri);
                if (jsonOutputSpecs != null) {
                    // Use the user's JSON capture spec.
                    int width2 = jsonOutputSpecs.optInt("width");
                    int height2 = jsonOutputSpecs.optInt("height");
                    if (width2 > 0) {
                        width = width2;
                    }
                    if (height2 > 0) {
                        height = height2;
                    }
                    String sformat = jsonOutputSpecs.optString("format");
                    if ("yuv".equals(sformat)) {
                        format = ImageFormat.YUV_420_888;
                    } else if ("jpg".equals(sformat) || "jpeg".equals(sformat)) {
                        format = ImageFormat.JPEG;
                    } else if ("".equals(sformat)) {
                        // No format specified.
                    } else {
                        throw new ItsException("Unsupported format: " + sformat);
                    }
                }

                Log.i(PYTAG, String.format("### SIZE %d %d", width, height));

                prepareCaptureReader(width, height, format);
                List<Surface> outputSurfaces = new ArrayList<Surface>(1);
                outputSurfaces.add(mCaptureReader.getSurface());
                mCamera.configureOutputs(outputSurfaces);
                mCameraListener.waitForState(BlockingStateListener.STATE_BUSY,
                        TIMEOUT_STATE_MS);
                mCameraListener.waitForState(BlockingStateListener.STATE_IDLE,
                        TIMEOUT_IDLE_MS);

                ImageReader.OnImageAvailableListener readerListener =
                        createAvailableListener(mCaptureListener);
                mCaptureReader.setOnImageAvailableListener(readerListener, mSaveHandler);

                // Plan for how many callbacks need to be received throughout the duration of this
                // sequence of capture requests.
                int numCaptures = requests.size();
                mCaptureCallbackLatch = new CountDownLatch(
                        numCaptures * ItsUtils.getCallbacksPerCapture(format));

            } catch (CameraAccessException e) {
                throw new ItsException("Error configuring outputs", e);
            }

            // Initiate the captures.
            for (int i = 0; i < requests.size(); i++) {
                CaptureRequest.Builder req = requests.get(i);
                Log.i(PYTAG, String.format("### CAPT %d of %d", i+1, requests.size()));
                req.addTarget(mCaptureReader.getSurface());
                mCamera.capture(req.build(), mCaptureResultListener, mResultHandler);
            }

            // Make sure all callbacks have been hit (wait until captures are done).
            try {
                if (!mCaptureCallbackLatch.await(TIMEOUT_CAPTURE, TimeUnit.SECONDS)) {
                    throw new ItsException(
                            "Timeout hit, but all callbacks not received");
                }
            } catch (InterruptedException e) {
                throw new ItsException("Interrupted: ", e);
            }

        } catch (android.hardware.camera2.CameraAccessException e) {
            throw new ItsException("Access error: ", e);
        }
    }

    private final CaptureListener mCaptureListener = new CaptureListener() {
        @Override
        public void onCaptureAvailable(Image capture) {
            try {
                int format = capture.getFormat();
                String extFileName = null;
                if (format == ImageFormat.JPEG) {
                    String fileName = ItsUtils.getJpegFileName(capture.getTimestamp());
                    ByteBuffer buf = capture.getPlanes()[0].getBuffer();
                    extFileName = ItsUtils.writeImageToFile(ItsService.this, buf, fileName);
                } else if (format == ImageFormat.YUV_420_888) {
                    String fileName = ItsUtils.getYuvFileName(capture.getTimestamp());
                    byte[] img = ItsUtils.getDataFromImage(capture);
                    ByteBuffer buf = ByteBuffer.wrap(img);
                    extFileName = ItsUtils.writeImageToFile(ItsService.this, buf, fileName);
                } else {
                    throw new ItsException("Unsupported image format: " + format);
                }
                Log.i(PYTAG, String.format("### FILE %s", extFileName));
                mCaptureCallbackLatch.countDown();
            } catch (ItsException e) {
                Log.e(TAG, "Script error: " + e);
                Log.e(PYTAG, "### FAIL");
            }
        }
    };

    private static float r2f(Rational r) {
        return (float)r.getNumerator() / (float)r.getDenominator();
    }

    private final CaptureResultListener mCaptureResultListener = new CaptureResultListener() {
        @Override
        public void onCaptureStarted(CameraDevice camera, CaptureRequest request, long timestamp) {
        }

        @Override
        public void onCaptureCompleted(CameraDevice camera, CaptureRequest request,
                CaptureResult result) {
            try {
                // Currently result has all 0 values.
                if (request == null || result == null) {
                    throw new ItsException("Request/result is invalid");
                }

                StringBuilder logMsg = new StringBuilder();
                logMsg.append(String.format(
                        "Capt result: AE=%d, AF=%d, AWB=%d, sens=%d, exp=%.1fms, dur=%.1fms, ",
                        result.get(CaptureResult.CONTROL_AE_STATE),
                        result.get(CaptureResult.CONTROL_AF_STATE),
                        result.get(CaptureResult.CONTROL_AWB_STATE),
                        result.get(CaptureResult.SENSOR_SENSITIVITY),
                        result.get(CaptureResult.SENSOR_EXPOSURE_TIME).intValue() / 1000000.0f,
                        result.get(CaptureResult.SENSOR_FRAME_DURATION).intValue() / 1000000.0f));
                if (result.get(CaptureResult.COLOR_CORRECTION_GAINS) != null) {
                    logMsg.append(String.format(
                            "gains=[%.1f, %.1f, %.1f, %.1f], ",
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[0],
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[1],
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[2],
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[3]));
                } else {
                    logMsg.append("gains=[], ");
                }
                if (result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM) != null) {
                    logMsg.append(String.format(
                            "xform=[%.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f], ",
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[0]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[1]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[2]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[3]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[4]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[5]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[6]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[7]),
                             r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[8])));
                } else {
                    logMsg.append("xform=[], ");
                }
                logMsg.append(String.format(
                        "foc=%.1f",
                        result.get(CaptureResult.LENS_FOCUS_DISTANCE)));
                Log.i(TAG, logMsg.toString());

                mConvergedAE = result.get(CaptureResult.CONTROL_AE_STATE) ==
                                          CaptureResult.CONTROL_AE_STATE_CONVERGED;
                mConvergedAF = result.get(CaptureResult.CONTROL_AF_STATE) ==
                                          CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED;
                mConvergedAWB = result.get(CaptureResult.CONTROL_AWB_STATE) ==
                                           CaptureResult.CONTROL_AWB_STATE_CONVERGED;

                if (mConvergedAE) {
                    Log.i(PYTAG, String.format(
                            "### 3A-E %d %d",
                            result.get(CaptureResult.SENSOR_SENSITIVITY).intValue(),
                            result.get(CaptureResult.SENSOR_EXPOSURE_TIME).intValue()
                            ));
                }

                if (mConvergedAF) {
                    Log.i(PYTAG, String.format(
                            "### 3A-F %f",
                            result.get(CaptureResult.LENS_FOCUS_DISTANCE)
                            ));
                }

                if (mConvergedAWB && result.get(CaptureResult.COLOR_CORRECTION_GAINS) != null
                        && result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM) != null) {
                    Log.i(PYTAG, String.format(
                            "### 3A-W %f %f %f %f %f %f %f %f %f %f %f %f %f",
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[0],
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[1],
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[2],
                            result.get(CaptureResult.COLOR_CORRECTION_GAINS)[3],
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[0]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[1]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[2]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[3]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[4]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[5]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[6]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[7]),
                            r2f(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)[8])
                            ));
                }

                if (mIssuedRequest3A) {
                    mIssuedRequest3A = false;
                    mInterlock3A.open();
                } else {
                    String fileName = ItsUtils.getMetadataFileName(
                            result.get(CaptureResult.SENSOR_TIMESTAMP));
                    File mdFile = ItsUtils.getOutputFile(ItsService.this, fileName);
                    ItsUtils.storeResults(mCameraCharacteristics, request, result, mdFile);
                    mCaptureCallbackLatch.countDown();
                }
            } catch (ItsException e) {
                Log.e(TAG, "Script error: " + e);
                Log.e(PYTAG, "### FAIL");
            } catch (Exception e) {
                Log.e(TAG, "Script error: " + e);
                Log.e(PYTAG, "### FAIL");
            }
        }

        @Override
        public void onCaptureFailed(CameraDevice camera, CaptureRequest request,
                CaptureFailure failure) {
            mCaptureCallbackLatch.countDown();
            Log.e(TAG, "Script error: capture failed");
            Log.e(PYTAG, "### FAIL");
        }
    };

}

/*
 * Copyright 2013 The Android Open Source Project
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

package android.hardware.camera2.cts;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import static android.hardware.camera2.cts.CameraTestUtils.*;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingStateListener;
import static com.android.ex.camera2.blocking.BlockingStateListener.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CameraCaptureResultTest extends AndroidTestCase {
    private static final String TAG = "CameraCaptureResultTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private CameraManager mCameraManager;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private ImageReader mImageReader;
    private Surface mSurface;
    private BlockingStateListener mCameraListener;

    private static final int MAX_NUM_IMAGES = 5;
    private static final int NUM_FRAMES_VERIFIED = 300;
    private static final long WAIT_FOR_RESULT_TIMEOUT_MS = 3000;

    // List that includes all public keys from CaptureResult
    List<CameraMetadata.Key<?>> mAllKeys;

    // List tracking the failed test keys.
    List<CameraMetadata.Key<?>> mFailedKeys = new ArrayList<CameraMetadata.Key<?>>();

    @Override
    public void setContext(Context context) {
        mAllKeys = getAllCaptureResultKeys();
        super.setContext(context);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Can't connect to camera manager", mCameraManager);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCameraListener = new BlockingStateListener();
        mFailedKeys.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        mHandlerThread.quitSafely();
        super.tearDown();
    }

    public void testCameraCaptureResultAllKeys() throws Exception {
        /**
         * Hardcode a key waiver list for the keys we want to skip the sanity check.
         * FIXME: We need get ride of this list, see bug 11116270.
         */
        List<CameraMetadata.Key<?>> waiverkeys = new ArrayList<CameraMetadata.Key<?>>();
        waiverkeys.add(CaptureResult.EDGE_MODE);
        waiverkeys.add(CaptureResult.JPEG_GPS_COORDINATES);
        waiverkeys.add(CaptureResult.JPEG_GPS_PROCESSING_METHOD);
        waiverkeys.add(CaptureResult.JPEG_GPS_TIMESTAMP);
        waiverkeys.add(CaptureResult.JPEG_ORIENTATION);
        waiverkeys.add(CaptureResult.JPEG_QUALITY);
        waiverkeys.add(CaptureResult.JPEG_THUMBNAIL_QUALITY);
        waiverkeys.add(CaptureResult.JPEG_THUMBNAIL_SIZE);
        waiverkeys.add(CaptureResult.SENSOR_TEMPERATURE);
        waiverkeys.add(CaptureResult.TONEMAP_CURVE_BLUE);
        waiverkeys.add(CaptureResult.TONEMAP_CURVE_GREEN);
        waiverkeys.add(CaptureResult.TONEMAP_CURVE_RED);
        waiverkeys.add(CaptureResult.TONEMAP_MODE);
        waiverkeys.add(CaptureResult.STATISTICS_PREDICTED_COLOR_GAINS);
        waiverkeys.add(CaptureResult.STATISTICS_PREDICTED_COLOR_TRANSFORM);
        waiverkeys.add(CaptureResult.STATISTICS_SCENE_FLICKER);

        String[] ids = mCameraManager.getCameraIdList();
        for (int i = 0; i < ids.length; i++) {
            CameraCharacteristics props = mCameraManager.getCameraCharacteristics(ids[i]);
            assertNotNull("CameraCharacteristics shouldn't be null", props);
            Integer hwLevel = props.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (hwLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                continue;
            }
            // TODO: check for LIMITED keys

            CameraDevice camera = null;
            try {
                Size[] sizes = CameraTestUtils.getSupportedSizeForFormat(
                        ImageFormat.YUV_420_888, ids[i], mCameraManager);
                CameraTestUtils.assertArrayNotEmpty(sizes, "Available sizes shouldn't be empty");
                createDefaultSurface(sizes[0]);

                if (VERBOSE) {
                    Log.v(TAG, "Testing camera " + ids[i] + "for size " + sizes[0].toString());
                }

                camera = CameraTestUtils.openCamera(
                        mCameraManager, ids[i], mCameraListener, mHandler);
                assertNotNull(
                        String.format("Failed to open camera device %s", ids[i]), camera);
                mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_OPEN_TIMEOUT_MS);

                List<Surface> outputSurfaces = new ArrayList<Surface>(1);
                outputSurfaces.add(mSurface);
                camera.configureOutputs(outputSurfaces);
                mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
                mCameraListener.waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);

                CaptureRequest.Builder requestBuilder =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                assertNotNull("Failed to create capture request", requestBuilder);
                requestBuilder.addTarget(mSurface);

                // Enable face detection if supported
                byte[] faceModes = props.get(
                        CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                assertNotNull("Available face detection modes shouldn't be null", faceModes);
                for (int m = 0; m < faceModes.length; m++) {
                    if (faceModes[m] == CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL) {
                        if (VERBOSE) {
                            Log.v(TAG, "testCameraCaptureResultAllKeys - " +
                                    "setting facedetection mode to full");
                        }
                        requestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                (int)faceModes[m]);
                    }
                }

                // Enable lensShading mode, it should be supported by full mode device.
                requestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,
                        CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_ON);

                SimpleCaptureListener captureListener = new SimpleCaptureListener();
                camera.setRepeatingRequest(requestBuilder.build(), captureListener, mHandler);

                for (int m = 0; m < NUM_FRAMES_VERIFIED; m++) {
                    if(VERBOSE) {
                        Log.v(TAG, "Testing frame " + m);
                    }
                    validateCaptureResult(
                            captureListener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS),
                            waiverkeys);
                }

                // Stop repeat, wait for captures to complete, and disconnect from surfaces
                camera.configureOutputs(/*outputs*/ null);
                mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
                mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_IDLE_TIMEOUT_MS);
                // Camera has disconnected, clear out the reader
                mSurface.release();
                mImageReader.close();
            } finally {
                if (camera != null) {
                    camera.close();
                }
            }

        }
    }

    private void validateCaptureResult(CaptureResult result,
            List<CameraMetadata.Key<?>> skippedKeys) throws Exception {
        for (CameraMetadata.Key<?> key : mAllKeys) {
            if (!skippedKeys.contains(key) && result.get(key) == null) {
                mFailedKeys.add(key);
            }
        }

        StringBuffer failedKeyNames = new StringBuffer("Below Keys have null values:\n");
        for (CameraMetadata.Key<?> key : mFailedKeys) {
            failedKeyNames.append(key.getName() + "\n");
        }

        assertTrue("Some keys have null values, " + failedKeyNames.toString(),
                mFailedKeys.isEmpty());
    }

    private static class SimpleCaptureListener extends CameraDevice.CaptureListener {
        LinkedBlockingQueue<CaptureResult> mQueue = new LinkedBlockingQueue<CaptureResult>();

        @Override
        public void onCaptureStarted(CameraDevice camera, CaptureRequest request, long timestamp)
        {
        }

        @Override
        public void onCaptureCompleted(CameraDevice camera, CaptureRequest request,
                CaptureResult result) {
            try {
                mQueue.put(result);
            } catch (InterruptedException e) {
                throw new UnsupportedOperationException(
                        "Can't handle InterruptedException in onCaptureCompleted");
            }
        }

        @Override
        public void onCaptureFailed(CameraDevice camera, CaptureRequest request,
                CaptureFailure failure) {
        }

        @Override
        public void onCaptureSequenceCompleted(CameraDevice camera, int sequenceId,
                int frameNumber) {
        }

        public CaptureResult getCaptureResult(long timeout) throws InterruptedException {
            CaptureResult result = mQueue.poll(timeout, TimeUnit.MILLISECONDS);
            assertNotNull("Wait for a capture result timed out in " + timeout + "ms", result);
            return result;
        }
    }

    private void createDefaultSurface(Size sz) {
        mImageReader =
                ImageReader.newInstance(sz.getWidth(),
                        sz.getHeight(),
                        ImageFormat.YUV_420_888,
                        MAX_NUM_IMAGES);
        mImageReader.setOnImageAvailableListener(new ImageDropperListener(), mHandler);
        mSurface = mImageReader.getSurface();
    }

    /**
     * TODO: Use CameraCharacteristics.getAvailableCaptureResultKeys() once we can filter out
     * @hide keys.
     *
     */

    /*@O~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~
     * The key entries below this point are generated from metadata
     * definitions in /system/media/camera/docs. Do not modify by hand or
     * modify the comment blocks at the start or end.
     *~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~*/

    private static List<CameraMetadata.Key<?>> getAllCaptureResultKeys() {
        ArrayList<CameraMetadata.Key<?>> resultKeys = new ArrayList<CameraMetadata.Key<?>>();
        resultKeys.add(CaptureResult.COLOR_CORRECTION_TRANSFORM);
        resultKeys.add(CaptureResult.COLOR_CORRECTION_GAINS);
        resultKeys.add(CaptureResult.CONTROL_AE_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_AF_MODE);
        resultKeys.add(CaptureResult.CONTROL_AF_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_AWB_MODE);
        resultKeys.add(CaptureResult.CONTROL_AWB_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_MODE);
        resultKeys.add(CaptureResult.CONTROL_AE_STATE);
        resultKeys.add(CaptureResult.CONTROL_AF_STATE);
        resultKeys.add(CaptureResult.CONTROL_AWB_STATE);
        resultKeys.add(CaptureResult.EDGE_MODE);
        resultKeys.add(CaptureResult.FLASH_MODE);
        resultKeys.add(CaptureResult.FLASH_STATE);
        resultKeys.add(CaptureResult.JPEG_GPS_COORDINATES);
        resultKeys.add(CaptureResult.JPEG_GPS_PROCESSING_METHOD);
        resultKeys.add(CaptureResult.JPEG_GPS_TIMESTAMP);
        resultKeys.add(CaptureResult.JPEG_ORIENTATION);
        resultKeys.add(CaptureResult.JPEG_QUALITY);
        resultKeys.add(CaptureResult.JPEG_THUMBNAIL_QUALITY);
        resultKeys.add(CaptureResult.JPEG_THUMBNAIL_SIZE);
        resultKeys.add(CaptureResult.LENS_APERTURE);
        resultKeys.add(CaptureResult.LENS_FILTER_DENSITY);
        resultKeys.add(CaptureResult.LENS_FOCAL_LENGTH);
        resultKeys.add(CaptureResult.LENS_FOCUS_DISTANCE);
        resultKeys.add(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE);
        resultKeys.add(CaptureResult.LENS_FOCUS_RANGE);
        resultKeys.add(CaptureResult.LENS_STATE);
        resultKeys.add(CaptureResult.NOISE_REDUCTION_MODE);
        resultKeys.add(CaptureResult.REQUEST_FRAME_COUNT);
        resultKeys.add(CaptureResult.SCALER_CROP_REGION);
        resultKeys.add(CaptureResult.SENSOR_EXPOSURE_TIME);
        resultKeys.add(CaptureResult.SENSOR_FRAME_DURATION);
        resultKeys.add(CaptureResult.SENSOR_SENSITIVITY);
        resultKeys.add(CaptureResult.SENSOR_TIMESTAMP);
        resultKeys.add(CaptureResult.SENSOR_TEMPERATURE);
        resultKeys.add(CaptureResult.STATISTICS_FACE_DETECT_MODE);
        resultKeys.add(CaptureResult.STATISTICS_FACE_IDS);
        resultKeys.add(CaptureResult.STATISTICS_FACE_LANDMARKS);
        resultKeys.add(CaptureResult.STATISTICS_FACE_RECTANGLES);
        resultKeys.add(CaptureResult.STATISTICS_FACE_SCORES);
        resultKeys.add(CaptureResult.STATISTICS_LENS_SHADING_MAP);
        resultKeys.add(CaptureResult.STATISTICS_PREDICTED_COLOR_GAINS);
        resultKeys.add(CaptureResult.STATISTICS_PREDICTED_COLOR_TRANSFORM);
        resultKeys.add(CaptureResult.STATISTICS_SCENE_FLICKER);
        resultKeys.add(CaptureResult.TONEMAP_CURVE_BLUE);
        resultKeys.add(CaptureResult.TONEMAP_CURVE_GREEN);
        resultKeys.add(CaptureResult.TONEMAP_CURVE_RED);
        resultKeys.add(CaptureResult.TONEMAP_MODE);
        resultKeys.add(CaptureResult.BLACK_LEVEL_LOCK);

        // Add STATISTICS_FACES key separately here because it is not
        // defined in metadata xml file.
        resultKeys.add(CaptureResult.STATISTICS_FACES);

        return resultKeys;
    }

    /*~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~
     * End generated code
     *~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~O@*/
}

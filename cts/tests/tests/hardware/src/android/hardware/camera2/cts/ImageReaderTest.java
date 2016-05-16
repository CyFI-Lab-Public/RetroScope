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

import static android.hardware.camera2.cts.CameraTestUtils.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.*;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.Size;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingCameraManager.BlockingOpenException;
import com.android.ex.camera2.blocking.BlockingStateListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Basic test for ImageReader APIs. It uses CameraDevice as producer, camera
 * sends the data to the surface provided by imageReader. Below image formats
 * are tested:</p>
 *
 * <p>YUV_420_888: flexible YUV420, it is mandatory format for camera. </p>
 * <p>JPEG: used for JPEG still capture, also mandatory format. </p>
 * <p>Some invalid access test. </p>
 * <p>TODO: Add more format tests? </p>
 */
public class ImageReaderTest extends AndroidTestCase {
    private static final String TAG = "ImageReaderTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final boolean DUMP_FILE = false;
    private static final String DEBUG_FILE_NAME_BASE =
            Environment.getExternalStorageDirectory().getPath();
    // number of frame (for streaming requests) to be verified.
    // TODO: Need extend it to bigger number
    private static final int NUM_FRAME_VERIFIED = 1;
    // Max number of images can be accessed simultaneously from ImageReader.
    private static final int MAX_NUM_IMAGES = 5;

    private CameraManager mCameraManager;
    private CameraDevice mCamera;
    private BlockingStateListener mCameraListener;
    private String[] mCameraIds;
    private ImageReader mReader = null;
    private Handler mHandler = null;
    private SimpleImageListener mListener = null;
    private CameraTestThread mLooperThread = null;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Can't connect to camera manager!", mCameraManager);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCameraIds = mCameraManager.getCameraIdList();
        mLooperThread = new CameraTestThread();
        mHandler = mLooperThread.start();
        mCameraListener = new BlockingStateListener();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mReader != null) {
            mReader.close();
            mReader = null;
        }
        mLooperThread.close();
        mHandler = null;
        super.tearDown();
    }

    public void testImageReaderFromCameraFlexibleYuv() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            Log.i(TAG, "Testing Camera " + mCameraIds[i]);
            openDevice(mCameraIds[i]);
            bufferFormatTestByCamera(ImageFormat.YUV_420_888, mCameraIds[i]);
            closeDevice(mCameraIds[i]);
        }
    }

    public void testImageReaderFromCameraJpeg() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            Log.v(TAG, "Testing Camera " + mCameraIds[i]);
            openDevice(mCameraIds[i]);
            bufferFormatTestByCamera(ImageFormat.JPEG, mCameraIds[i]);
            closeDevice(mCameraIds[i]);
        }
    }

    public void testImageReaderFromCameraRaw() {
        // TODO: can test this once raw is supported
    }

    public void testImageReaderInvalidAccessTest() {
        // TODO: test invalid access case, see if we can receive expected
        // exceptions
    }

    private void bufferFormatTestByCamera(int format, String cameraId) throws Exception {
        CameraCharacteristics properties = mCameraManager.getCameraCharacteristics(cameraId);
        assertNotNull("Can't get camera properties!", properties);

        /**
         * TODO: cleanup the color format mess, we probably need define formats
         * in Image class instead of using ImageFormat for camera. also,
         * probably make sense to change the available format type from Enum[]
         * to int[]. It'll also be nice to put this into a helper function and
         * move to util class.
         */
        int[] availableFormats = properties.get(CameraCharacteristics.SCALER_AVAILABLE_FORMATS);
        assertArrayNotEmpty(availableFormats,
                "availableFormats should not be empty");
        Arrays.sort(availableFormats);
        assertTrue("Can't find the format " + format + " in supported formats " +
                Arrays.toString(availableFormats),
                Arrays.binarySearch(availableFormats, format) >= 0);

        Size[] availableSizes = getSupportedSizeForFormat(format, mCamera.getId(), mCameraManager);
        assertArrayNotEmpty(availableSizes, "availableSizes should not be empty");

        // for each resolution, test imageReader:
        for (Size sz : availableSizes) {
            if (VERBOSE) Log.v(TAG, "Testing size " + sz.toString() + " for camera " + cameraId);

            prepareImageReader(sz, format);

            CaptureRequest request = prepareCaptureRequest(format);

            captureAndValidateImage(request, sz, format);

            stopCapture();
        }
    }

    private class SimpleImageListener implements ImageReader.OnImageAvailableListener {
        private int mPendingImages = 0;
        private final Object mImageSyncObject = new Object();

        @Override
        public void onImageAvailable(ImageReader reader) {
            if (VERBOSE) Log.v(TAG, "new image available");
            synchronized (mImageSyncObject) {
                mPendingImages++;
                mImageSyncObject.notifyAll();
            }
        }

        public boolean isImagePending() {
            synchronized (mImageSyncObject) {
                return (mPendingImages > 0);
            }
        }

        public void waitForImage() {
            final int TIMEOUT_MS = 5000;
            synchronized (mImageSyncObject) {
                while (mPendingImages == 0) {
                    try {
                        if (VERBOSE)
                            Log.d(TAG, "waiting for next image");
                        mImageSyncObject.wait(TIMEOUT_MS);
                        if (mPendingImages == 0) {
                            fail("wait for next image timed out");
                        }
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
                mPendingImages--;
            }
        }
    }

    private void prepareImageReader(Size sz, int format) throws Exception {
        int width = sz.getWidth();
        int height = sz.getHeight();
        mReader = ImageReader.newInstance(width, height, format, MAX_NUM_IMAGES);
        mListener  = new SimpleImageListener();
        mReader.setOnImageAvailableListener(mListener, mHandler);
        if (VERBOSE) Log.v(TAG, "Preparing ImageReader size " + sz.toString());
    }

    private CaptureRequest prepareCaptureRequest(int format) throws Exception {
        List<Surface> outputSurfaces = new ArrayList<Surface>(1);
        Surface surface = mReader.getSurface();
        assertNotNull("Fail to get surface from ImageReader", surface);
        outputSurfaces.add(surface);
        mCamera.configureOutputs(outputSurfaces);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);

        CaptureRequest.Builder captureBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        assertNotNull("Fail to get captureRequest", captureBuilder);
        captureBuilder.addTarget(mReader.getSurface());

        return captureBuilder.build();
    }

    private void captureAndValidateImage(CaptureRequest request,
            Size sz, int format) throws Exception {
        // TODO: Add more format here, and wrap each one as a function.
        Image img;
        int captureCount = NUM_FRAME_VERIFIED;

        // Only verify single image for still capture
        if (format == ImageFormat.JPEG) {
            captureCount = 1;
            mCamera.capture(request, null, null);
        } else {
            mCamera.setRepeatingRequest(request, null, null);
        }

        for (int i = 0; i < captureCount; i++) {
            assertNotNull("Image listener is null", mListener);
            if (VERBOSE) Log.v(TAG, "Waiting for an Image");
            mListener.waitForImage();
            img = mReader.acquireNextImage();
            if (VERBOSE) Log.v(TAG, "Got next image");
            validateImage(img, sz.getWidth(), sz.getHeight(), format);
            img.close();
            // Return the pending images to producer in case the validation is slower
            // than the image producing rate. Otherwise, it could cause the producer
            // starvation.
            while (mListener.isImagePending()) {
                mListener.waitForImage();
                img = mReader.acquireNextImage();
                img.close();
            }
        }
    }

    private void stopCapture() throws CameraAccessException {
        if (VERBOSE) Log.v(TAG, "Stopping capture and waiting for idle");
        // Stop repeat, wait for captures to complete, and disconnect from surfaces
        mCamera.configureOutputs(/*outputs*/ null);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_IDLE_TIMEOUT_MS);
        // Camera has disconnected, clear out the reader
        mReader.close();
        mReader = null;
        mListener = null;
    }

    private void openDevice(String cameraId) {
        if (mCamera != null) {
            throw new IllegalStateException("Already have open camera device");
        }
        try {
            mCamera = CameraTestUtils.openCamera(
                mCameraManager, cameraId, mCameraListener, mHandler);
        } catch (CameraAccessException e) {
            mCamera = null;
            fail("Fail to open camera, " + Log.getStackTraceString(e));
        } catch (BlockingOpenException e) {
            mCamera = null;
            fail("Fail to open camera, " + Log.getStackTraceString(e));
        }
        mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_OPEN_TIMEOUT_MS);
    }

    private void closeDevice(String cameraId) {
        mCamera.close();
        mCamera = null;
    }

    private void validateImage(Image image, int width, int height, int format) {
        checkImage(image, width, height, format);

        /**
         * TODO: validate timestamp:
         * 1. capture result timestamp against the image timestamp (need
         * consider frame drops)
         * 2. timestamps should be monotonically increasing for different requests
         */
        if(VERBOSE) Log.v(TAG, "validating Image");
        byte[] data = getDataFromImage(image);
        assertTrue("Invalid image data", data != null && data.length > 0);

        if (format == ImageFormat.JPEG) {
            validateJpegData(data, width, height);
        } else {
            validateYuvData(data, width, height, format, image.getTimestamp());
        }
    }

    private void validateJpegData(byte[] jpegData, int width, int height) {
        BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
        // DecodeBound mode: only parse the frame header to get width/height.
        // it doesn't decode the pixel.
        bmpOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, bmpOptions);
        assertEquals(width, bmpOptions.outWidth);
        assertEquals(height, bmpOptions.outHeight);

        // Pixel decoding mode: decode whole image. check if the image data
        // is decodable here.
        assertNotNull("Decoding jpeg failed",
                BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length));
        if (DUMP_FILE) {
            String fileName =
                    DEBUG_FILE_NAME_BASE + width + "x" + height + ".yuv";
            dumpFile(fileName, jpegData);
        }
    }

    private void validateYuvData(byte[] yuvData, int width, int height, int format, long ts) {
        checkYuvFormat(format);
        if (VERBOSE) Log.v(TAG, "Validating YUV data");
        int expectedSize = width * height * ImageFormat.getBitsPerPixel(format) / 8;
        assertEquals("Yuv data doesn't match", expectedSize, yuvData.length);

        // TODO: Can add data validation if we have test pattern(tracked by b/9625427)

        if (DUMP_FILE) {
            String fileName =
                    DEBUG_FILE_NAME_BASE + "/" + width + "x" + height + "_" + ts / 1e6 + ".yuv";
            dumpFile(fileName, yuvData);
        }
    }
}

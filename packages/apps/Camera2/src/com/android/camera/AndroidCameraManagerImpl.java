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

package com.android.camera;

import static com.android.camera.util.CameraUtil.Assert;

import java.io.IOException;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * A class to implement {@link CameraManager} of the Android camera framework.
 */
class AndroidCameraManagerImpl implements CameraManager {
    private static final String TAG = "CAM_" +
            AndroidCameraManagerImpl.class.getSimpleName();

    private Parameters mParameters;
    private boolean mParametersIsDirty;
    private IOException mReconnectIOException;

    /* Messages used in CameraHandler. */
    // Camera initialization/finalization
    private static final int OPEN_CAMERA = 1;
    private static final int RELEASE =     2;
    private static final int RECONNECT =   3;
    private static final int UNLOCK =      4;
    private static final int LOCK =        5;
    // Preview
    private static final int SET_PREVIEW_TEXTURE_ASYNC =        101;
    private static final int START_PREVIEW_ASYNC =              102;
    private static final int STOP_PREVIEW =                     103;
    private static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 104;
    private static final int ADD_CALLBACK_BUFFER =              105;
    private static final int SET_PREVIEW_DISPLAY_ASYNC =        106;
    private static final int SET_PREVIEW_CALLBACK =             107;
    // Parameters
    private static final int SET_PARAMETERS =     201;
    private static final int GET_PARAMETERS =     202;
    private static final int REFRESH_PARAMETERS = 203;
    // Focus, Zoom
    private static final int AUTO_FOCUS =                   301;
    private static final int CANCEL_AUTO_FOCUS =            302;
    private static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 303;
    private static final int SET_ZOOM_CHANGE_LISTENER =     304;
    // Face detection
    private static final int SET_FACE_DETECTION_LISTENER = 461;
    private static final int START_FACE_DETECTION =        462;
    private static final int STOP_FACE_DETECTION =         463;
    private static final int SET_ERROR_CALLBACK =          464;
    // Presentation
    private static final int ENABLE_SHUTTER_SOUND =    501;
    private static final int SET_DISPLAY_ORIENTATION = 502;

    private CameraHandler mCameraHandler;
    private android.hardware.Camera mCamera;

    // Used to retain a copy of Parameters for setting parameters.
    private Parameters mParamsToSet;

    AndroidCameraManagerImpl() {
        HandlerThread ht = new HandlerThread("Camera Handler Thread");
        ht.start();
        mCameraHandler = new CameraHandler(ht.getLooper());
    }

    private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        private void startFaceDetection() {
            mCamera.startFaceDetection();
        }

        private void stopFaceDetection() {
            mCamera.stopFaceDetection();
        }

        private void setFaceDetectionListener(FaceDetectionListener listener) {
            mCamera.setFaceDetectionListener(listener);
        }

        private void setPreviewTexture(Object surfaceTexture) {
            try {
                mCamera.setPreviewTexture((SurfaceTexture) surfaceTexture);
            } catch (IOException e) {
                Log.e(TAG, "Could not set preview texture", e);
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private void enableShutterSound(boolean enable) {
            mCamera.enableShutterSound(enable);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        private void setAutoFocusMoveCallback(
                android.hardware.Camera camera, Object cb) {
            camera.setAutoFocusMoveCallback((AutoFocusMoveCallback) cb);
        }

        public void requestTakePicture(
                final ShutterCallback shutter,
                final PictureCallback raw,
                final PictureCallback postView,
                final PictureCallback jpeg) {
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCamera.takePicture(shutter, raw, postView, jpeg);
                    } catch (RuntimeException e) {
                        // TODO: output camera state and focus state for debugging.
                        Log.e(TAG, "take picture failed.");
                        throw e;
                    }
                }
            });
        }

        /**
         * Waits for all the {@code Message} and {@code Runnable} currently in the queue
         * are processed.
         *
         * @return {@code false} if the wait was interrupted, {@code true} otherwise.
         */
        public boolean waitDone() {
            final Object waitDoneLock = new Object();
            final Runnable unlockRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (waitDoneLock) {
                        waitDoneLock.notifyAll();
                    }
                }
            };

            synchronized (waitDoneLock) {
                mCameraHandler.post(unlockRunnable);
                try {
                    waitDoneLock.wait();
                } catch (InterruptedException ex) {
                    Log.v(TAG, "waitDone interrupted");
                    return false;
                }
            }
            return true;
        }

        /**
         * This method does not deal with the API level check.  Everyone should
         * check first for supported operations before sending message to this handler.
         */
        @Override
        public void handleMessage(final Message msg) {
            try {
                switch (msg.what) {
                    case OPEN_CAMERA:
                        mCamera = android.hardware.Camera.open(msg.arg1);
                        if (mCamera != null) {
                            mParametersIsDirty = true;

                            // Get a instance of Camera.Parameters for later use.
                            if (mParamsToSet == null) {
                                mParamsToSet = mCamera.getParameters();
                            }
                        } else {
                            if (msg.obj != null) {
                                ((CameraOpenErrorCallback) msg.obj).onDeviceOpenFailure(msg.arg1);
                            }
                        }
                        return;

                    case RELEASE:
                        mCamera.release();
                        mCamera = null;
                        return;

                    case RECONNECT:
                        mReconnectIOException = null;
                        try {
                            mCamera.reconnect();
                        } catch (IOException ex) {
                            mReconnectIOException = ex;
                        }
                        return;

                    case UNLOCK:
                        mCamera.unlock();
                        return;

                    case LOCK:
                        mCamera.lock();
                        return;

                    case SET_PREVIEW_TEXTURE_ASYNC:
                        setPreviewTexture(msg.obj);
                        return;

                    case SET_PREVIEW_DISPLAY_ASYNC:
                        try {
                            mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return;

                    case START_PREVIEW_ASYNC:
                        mCamera.startPreview();
                        return;

                    case STOP_PREVIEW:
                        mCamera.stopPreview();
                        return;

                    case SET_PREVIEW_CALLBACK_WITH_BUFFER:
                        mCamera.setPreviewCallbackWithBuffer(
                            (PreviewCallback) msg.obj);
                        return;

                    case ADD_CALLBACK_BUFFER:
                        mCamera.addCallbackBuffer((byte[]) msg.obj);
                        return;

                    case AUTO_FOCUS:
                        mCamera.autoFocus((AutoFocusCallback) msg.obj);
                        return;

                    case CANCEL_AUTO_FOCUS:
                        mCamera.cancelAutoFocus();
                        return;

                    case SET_AUTO_FOCUS_MOVE_CALLBACK:
                        setAutoFocusMoveCallback(mCamera, msg.obj);
                        return;

                    case SET_DISPLAY_ORIENTATION:
                        mCamera.setDisplayOrientation(msg.arg1);
                        return;

                    case SET_ZOOM_CHANGE_LISTENER:
                        mCamera.setZoomChangeListener(
                            (OnZoomChangeListener) msg.obj);
                        return;

                    case SET_FACE_DETECTION_LISTENER:
                        setFaceDetectionListener((FaceDetectionListener) msg.obj);
                        return;

                    case START_FACE_DETECTION:
                        startFaceDetection();
                        return;

                    case STOP_FACE_DETECTION:
                        stopFaceDetection();
                        return;

                    case SET_ERROR_CALLBACK:
                        mCamera.setErrorCallback((ErrorCallback) msg.obj);
                        return;

                    case SET_PARAMETERS:
                        mParametersIsDirty = true;
                        mParamsToSet.unflatten((String) msg.obj);
                        mCamera.setParameters(mParamsToSet);
                        return;

                    case GET_PARAMETERS:
                        if (mParametersIsDirty) {
                            mParameters = mCamera.getParameters();
                            mParametersIsDirty = false;
                        }
                        return;

                    case SET_PREVIEW_CALLBACK:
                        mCamera.setPreviewCallback((PreviewCallback) msg.obj);
                        return;

                    case ENABLE_SHUTTER_SOUND:
                        enableShutterSound((msg.arg1 == 1) ? true : false);
                        return;

                    case REFRESH_PARAMETERS:
                        mParametersIsDirty = true;
                        return;

                    default:
                        throw new RuntimeException("Invalid CameraProxy message=" + msg.what);
                }
            } catch (RuntimeException e) {
                if (msg.what != RELEASE && mCamera != null) {
                    try {
                        mCamera.release();
                    } catch (Exception ex) {
                        Log.e(TAG, "Fail to release the camera.");
                    }
                    mCamera = null;
                } else if (mCamera == null) {
                    if (msg.what == OPEN_CAMERA) {
                        if (msg.obj != null) {
                            ((CameraOpenErrorCallback) msg.obj).onDeviceOpenFailure(msg.arg1);
                        }
                    } else {
                        Log.w(TAG, "Cannot handle message, mCamera is null.");
                    }
                    return;
                }
                throw e;
            }
        }
    }

    @Override
    public CameraManager.CameraProxy cameraOpen(
        Handler handler, int cameraId, CameraOpenErrorCallback callback) {
        mCameraHandler.obtainMessage(OPEN_CAMERA, cameraId, 0,
                CameraOpenErrorCallbackForward.getNewInstance(
                        handler, callback)).sendToTarget();
        mCameraHandler.waitDone();
        if (mCamera != null) {
            return new AndroidCameraProxyImpl();
        } else {
            return null;
        }
    }

    /**
     * A class which implements {@link CameraManager.CameraProxy} and
     * camera handler thread.
     * TODO: Save the handler for the callback here to avoid passing the same
     * handler multiple times.
     */
    public class AndroidCameraProxyImpl implements CameraManager.CameraProxy {

        private AndroidCameraProxyImpl() {
            Assert(mCamera != null);
        }

        @Override
        public android.hardware.Camera getCamera() {
            return mCamera;
        }

        @Override
        public void release() {
            // release() must be synchronous so we know exactly when the camera
            // is released and can continue on.
            mCameraHandler.sendEmptyMessage(RELEASE);
            mCameraHandler.waitDone();
        }

        @Override
        public boolean reconnect(Handler handler, CameraOpenErrorCallback cb) {
            mCameraHandler.sendEmptyMessage(RECONNECT);
            mCameraHandler.waitDone();
            CameraOpenErrorCallback cbforward =
                    CameraOpenErrorCallbackForward.getNewInstance(handler, cb);
            if (mReconnectIOException != null) {
                if (cbforward != null) {
                    cbforward.onReconnectionFailure(AndroidCameraManagerImpl.this);
                }
                return false;
            }
            return true;
        }

        @Override
        public void unlock() {
            mCameraHandler.sendEmptyMessage(UNLOCK);
            mCameraHandler.waitDone();
        }

        @Override
        public void lock() {
            mCameraHandler.sendEmptyMessage(LOCK);
        }

        @Override
        public void setPreviewTexture(SurfaceTexture surfaceTexture) {
            mCameraHandler.obtainMessage(SET_PREVIEW_TEXTURE_ASYNC, surfaceTexture).sendToTarget();
        }

        @Override
        public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
            mCameraHandler.obtainMessage(SET_PREVIEW_DISPLAY_ASYNC, surfaceHolder).sendToTarget();
        }

        @Override
        public void startPreview() {
            mCameraHandler.sendEmptyMessage(START_PREVIEW_ASYNC);
        }

        @Override
        public void stopPreview() {
            mCameraHandler.sendEmptyMessage(STOP_PREVIEW);
            mCameraHandler.waitDone();
        }

        @Override
        public void setPreviewDataCallback(
                Handler handler, CameraPreviewDataCallback cb) {
            mCameraHandler.obtainMessage(
                    SET_PREVIEW_CALLBACK,
                    PreviewCallbackForward.getNewInstance(handler, this, cb)).sendToTarget();
        }

        @Override
        public void setPreviewDataCallbackWithBuffer(
                Handler handler, CameraPreviewDataCallback cb) {
            mCameraHandler.obtainMessage(
                    SET_PREVIEW_CALLBACK_WITH_BUFFER,
                    PreviewCallbackForward.getNewInstance(handler, this, cb)).sendToTarget();
        }

        @Override
        public void addCallbackBuffer(byte[] callbackBuffer) {
            mCameraHandler.obtainMessage(ADD_CALLBACK_BUFFER, callbackBuffer).sendToTarget();
        }

        @Override
        public void autoFocus(Handler handler, CameraAFCallback cb) {
            mCameraHandler.obtainMessage(
                    AUTO_FOCUS,
                    AFCallbackForward.getNewInstance(handler, this, cb)).sendToTarget();
        }

        @Override
        public void cancelAutoFocus() {
            mCameraHandler.removeMessages(AUTO_FOCUS);
            mCameraHandler.sendEmptyMessage(CANCEL_AUTO_FOCUS);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void setAutoFocusMoveCallback(
                Handler handler, CameraAFMoveCallback cb) {
            mCameraHandler.obtainMessage(
                    SET_AUTO_FOCUS_MOVE_CALLBACK,
                    AFMoveCallbackForward.getNewInstance(handler, this, cb)).sendToTarget();
        }

        @Override
        public void takePicture(
                Handler handler,
                CameraShutterCallback shutter,
                CameraPictureCallback raw,
                CameraPictureCallback post,
                CameraPictureCallback jpeg) {
            mCameraHandler.requestTakePicture(
                    ShutterCallbackForward.getNewInstance(handler, this, shutter),
                    PictureCallbackForward.getNewInstance(handler, this, raw),
                    PictureCallbackForward.getNewInstance(handler, this, post),
                    PictureCallbackForward.getNewInstance(handler, this, jpeg));
        }

        @Override
        public void setDisplayOrientation(int degrees) {
            mCameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, degrees, 0)
                    .sendToTarget();
        }

        @Override
        public void setZoomChangeListener(OnZoomChangeListener listener) {
            mCameraHandler.obtainMessage(SET_ZOOM_CHANGE_LISTENER, listener).sendToTarget();
        }

        public void setFaceDetectionCallback(
                Handler handler, CameraFaceDetectionCallback cb) {
            mCameraHandler.obtainMessage(
                    SET_FACE_DETECTION_LISTENER,
                    FaceDetectionCallbackForward.getNewInstance(handler, this, cb)).sendToTarget();
        }

        @Override
        public void startFaceDetection() {
            mCameraHandler.sendEmptyMessage(START_FACE_DETECTION);
        }

        @Override
        public void stopFaceDetection() {
            mCameraHandler.sendEmptyMessage(STOP_FACE_DETECTION);
        }

        @Override
        public void setErrorCallback(ErrorCallback cb) {
            mCameraHandler.obtainMessage(SET_ERROR_CALLBACK, cb).sendToTarget();
        }

        @Override
        public void setParameters(Parameters params) {
            if (params == null) {
                Log.v(TAG, "null parameters in setParameters()");
                return;
            }
            mCameraHandler.obtainMessage(SET_PARAMETERS, params.flatten())
                    .sendToTarget();
        }

        @Override
        public Parameters getParameters() {
            mCameraHandler.sendEmptyMessage(GET_PARAMETERS);
            mCameraHandler.waitDone();
            return mParameters;
        }

        @Override
        public void refreshParameters() {
            mCameraHandler.sendEmptyMessage(REFRESH_PARAMETERS);
        }

        @Override
        public void enableShutterSound(boolean enable) {
            mCameraHandler.obtainMessage(
                    ENABLE_SHUTTER_SOUND, (enable ? 1 : 0), 0).sendToTarget();
        }
    }

    /**
     * A helper class to forward AutoFocusCallback to another thread.
     */
    private static class AFCallbackForward implements AutoFocusCallback {
        private final Handler mHandler;
        private final CameraProxy mCamera;
        private final CameraAFCallback mCallback;

        /**
         * Returns a new instance of {@link AFCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param camera  The {@link CameraProxy} which the callback is from.
         * @param cb      The callback to be invoked.
         * @return        The instance of the {@link AFCallbackForward},
         *                or null if any parameter is null.
         */
        public static AFCallbackForward getNewInstance(
                Handler handler, CameraProxy camera, CameraAFCallback cb) {
            if (handler == null || camera == null || cb == null) return null;
            return new AFCallbackForward(handler, camera, cb);
        }

        private AFCallbackForward(
                Handler h, CameraProxy camera, CameraAFCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onAutoFocus(final boolean b, Camera camera) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onAutoFocus(b, mCamera);
                }
            });
        }
    }

    /** A helper class to forward AutoFocusMoveCallback to another thread. */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class AFMoveCallbackForward implements AutoFocusMoveCallback {
        private final Handler mHandler;
        private final CameraAFMoveCallback mCallback;
        private final CameraProxy mCamera;

        /**
         * Returns a new instance of {@link AFMoveCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param camera  The {@link CameraProxy} which the callback is from.
         * @param cb      The callback to be invoked.
         * @return        The instance of the {@link AFMoveCallbackForward},
         *                or null if any parameter is null.
         */
        public static AFMoveCallbackForward getNewInstance(
                Handler handler, CameraProxy camera, CameraAFMoveCallback cb) {
            if (handler == null || camera == null || cb == null) return null;
            return new AFMoveCallbackForward(handler, camera, cb);
        }

        private AFMoveCallbackForward(
                Handler h, CameraProxy camera, CameraAFMoveCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onAutoFocusMoving(
                final boolean moving, android.hardware.Camera camera) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onAutoFocusMoving(moving, mCamera);
                }
            });
        }
    }

    /**
     * A helper class to forward ShutterCallback to to another thread.
     */
    private static class ShutterCallbackForward implements ShutterCallback {
        private final Handler mHandler;
        private final CameraShutterCallback mCallback;
        private final CameraProxy mCamera;

        /**
         * Returns a new instance of {@link ShutterCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param camera  The {@link CameraProxy} which the callback is from.
         * @param cb      The callback to be invoked.
         * @return        The instance of the {@link ShutterCallbackForward},
         *                or null if any parameter is null.
         */
        public static ShutterCallbackForward getNewInstance(
                Handler handler, CameraProxy camera, CameraShutterCallback cb) {
            if (handler == null || camera == null || cb == null) return null;
            return new ShutterCallbackForward(handler, camera, cb);
        }

        private ShutterCallbackForward(
                Handler h, CameraProxy camera, CameraShutterCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onShutter() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onShutter(mCamera);
                }
            });
        }
    }

    /**
     * A helper class to forward PictureCallback to another thread.
     */
    private static class PictureCallbackForward implements PictureCallback {
        private final Handler mHandler;
        private final CameraPictureCallback mCallback;
        private final CameraProxy mCamera;

        /**
         * Returns a new instance of {@link PictureCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param camera  The {@link CameraProxy} which the callback is from.
         * @param cb      The callback to be invoked.
         * @return        The instance of the {@link PictureCallbackForward},
         *                or null if any parameters is null.
         */
        public static PictureCallbackForward getNewInstance(
                Handler handler, CameraProxy camera, CameraPictureCallback cb) {
            if (handler == null || camera == null || cb == null) return null;
            return new PictureCallbackForward(handler, camera, cb);
        }

        private PictureCallbackForward(
                Handler h, CameraProxy camera, CameraPictureCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onPictureTaken(
                final byte[] data, android.hardware.Camera camera) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPictureTaken(data, mCamera);
                }
            });
        }
    }

    /**
     * A helper class to forward PreviewCallback to another thread.
     */
    private static class PreviewCallbackForward implements PreviewCallback {
        private final Handler mHandler;
        private final CameraPreviewDataCallback mCallback;
        private final CameraProxy mCamera;

        /**
         * Returns a new instance of {@link PreviewCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param camera  The {@link CameraProxy} which the callback is from.
         * @param cb      The callback to be invoked.
         * @return        The instance of the {@link PreviewCallbackForward},
         *                or null if any parameters is null.
         */
        public static PreviewCallbackForward getNewInstance(
                Handler handler, CameraProxy camera, CameraPreviewDataCallback cb) {
            if (handler == null || camera == null || cb == null) return null;
            return new PreviewCallbackForward(handler, camera, cb);
        }

        private PreviewCallbackForward(
                Handler h, CameraProxy camera, CameraPreviewDataCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onPreviewFrame(
                final byte[] data, android.hardware.Camera camera) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPreviewFrame(data, mCamera);
                }
            });
        }
    }

    private static class FaceDetectionCallbackForward implements FaceDetectionListener {
        private final Handler mHandler;
        private final CameraFaceDetectionCallback mCallback;
        private final CameraProxy mCamera;

        /**
         * Returns a new instance of {@link FaceDetectionCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param camera  The {@link CameraProxy} which the callback is from.
         * @param cb      The callback to be invoked.
         * @return        The instance of the {@link FaceDetectionCallbackForward},
         *                or null if any parameter is null.
         */
        public static FaceDetectionCallbackForward getNewInstance(
                Handler handler, CameraProxy camera, CameraFaceDetectionCallback cb) {
            if (handler == null || camera == null || cb == null) return null;
            return new FaceDetectionCallbackForward(handler, camera, cb);
        }

        private FaceDetectionCallbackForward(
                Handler h, CameraProxy camera, CameraFaceDetectionCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onFaceDetection(
                final Camera.Face[] faces, Camera camera) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onFaceDetection(faces, mCamera);
                }
            });
        }
    }

    /**
     * A callback helps to invoke the original callback on another
     * {@link android.os.Handler}.
     */
    private static class CameraOpenErrorCallbackForward implements CameraOpenErrorCallback {
        private final Handler mHandler;
        private final CameraOpenErrorCallback mCallback;

        /**
         * Returns a new instance of {@link FaceDetectionCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param cb The callback to be invoked.
         * @return The instance of the {@link FaceDetectionCallbackForward}, or
         *         null if any parameter is null.
         */
        public static CameraOpenErrorCallbackForward getNewInstance(
                Handler handler, CameraOpenErrorCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenErrorCallbackForward(handler, cb);
        }

        private CameraOpenErrorCallbackForward(
                Handler h, CameraOpenErrorCallback cb) {
            // Given that we are using the main thread handler, we can create it
            // here instead of holding onto the PhotoModule objects. In this
            // way, we can avoid memory leak.
            mHandler = new Handler(Looper.getMainLooper());
            mCallback = cb;
        }

        @Override
        public void onCameraDisabled(final int cameraId) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraDisabled(cameraId);
                }
            });
        }

        @Override
        public void onDeviceOpenFailure(final int cameraId) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeviceOpenFailure(cameraId);
                }
            });
        }

        @Override
        public void onReconnectionFailure(final CameraManager mgr) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onReconnectionFailure(mgr);
                }
            });
        }
    }
}

/*
 * Copyright (C) 2012 The Android Open Source Project
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

/* Original code copied from NDK Native-media sample code */
package android.mediastress.cts;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class NativeMediaActivity extends Activity implements OnSurfaceChangedListener {
    public static final String EXTRA_VIDEO_QUALITY = "videoQuality";
    // should be long enough. time-out can be treated as error
    public static final long NATIVE_MEDIA_LIFECYCLE_TIMEOUT_MS = 10000;
    static final String TAG = "NativeMedia";
    static final String[] MEDIA = {
        "bbb_short/480x360/mp4_libx264_libfaac/" +
        "bbb_short.ffmpeg.480x360.mp4.libx264_1000kbps_30fps.libfaac_stereo_192kbps_44100Hz.ts",
        "bbb_short/720x480/mp4_libx264_libfaac/" +
        "bbb_short.ffmpeg.720x480.mp4.libx264_1000kbps_30fps.libfaac_stereo_192kbps_44100Hz.ts",
        "bbb_short/1280x720/mp4_libx264_libfaac/" +
        "bbb_short.ffmpeg.1280x720.mp4.libx264_1000kbps_30fps.libfaac_stereo_192kbps_44100Hz.ts",
        "bbb_short/1920x1080/mp4_libx264_libfaac/" +
        "bbb_short.ffmpeg.1920x1080.mp4.libx264_5000kbps_30fps.libfaac_stereo_192kbps_48000Hz.ts"
    };

    private SurfaceTextureGLSurfaceView mGLView;
    private volatile boolean mNativeCreated = false;
    /** 0 for default (480x360), other value can be CamcorderProfile.QUALITY_480P / 720P / 1080P */
    private int mVideoQuality = 0;
    // native media status queued whenever there is a change in life.
    private final BlockingQueue<Boolean> mNativeWaitQ = new LinkedBlockingQueue<Boolean>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        mVideoQuality = getIntent().getIntExtra(EXTRA_VIDEO_QUALITY, mVideoQuality);
        mGLView = new SurfaceTextureGLSurfaceView(this, this);
        setContentView(mGLView);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.w(TAG, "configuration changed " + newConfig.orientation);
        super.onConfigurationChanged(newConfig);
    }

    /**
     * should be called by GLThread after GlSurface is created.
     */
    @Override
    public void onSurfaceCreated() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onSurfaceCreated create engine");
                // initialize native media system
                Assert.assertTrue(createEngine());
                Assert.assertTrue(setSurfaceForNative());
                String fileName = getMediaString();
                Log.i(TAG, "start playing " + fileName);
                Assert.assertTrue(createMediaPlayer("file://" + fileName));
                mNativeCreated = true;
                mNativeWaitQ.add(mNativeCreated);
            }
        });
    }

    /**
     * should be called inside main thread
     */
    @Override
    public void onSurfaceDestroyed() {
        shutdownIfActive();
    }

    /**
     * check if native media is alive. If it does not become alive
     * for more than certain time, assertion fail will happen.
     * @return the status of native media, true if it is alive, null if timed-out
     * @throws InterruptedException
     */
    public Boolean waitForNativeMediaLifeCycle() throws InterruptedException {
        return mNativeWaitQ.poll(NATIVE_MEDIA_LIFECYCLE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onPause() {
        //GLSurfaceView destroys surface on pause. so shutdown should be done.
        shutdownIfActive();
        mGLView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
        if (mNativeCreated) {
            Assert.assertTrue(playOrPauseMediaPlayer(true));
        }
    }

    @Override
    protected void onDestroy() {
        if (mNativeCreated) {
            shutdown();
        }
        super.onDestroy();
    }

    private void shutdownIfActive() {
        if (mNativeCreated) {
            Log.i(TAG, "shutdownIfActive shutdown");
            // surface no longer available, so just shutdown
            shutdown();
            mNativeCreated = false;
            mNativeWaitQ.add(mNativeCreated);
        }
    }

    private boolean setSurfaceForNative() {
        SurfaceTexture st = mGLView.getSurfaceTexture();
        Assert.assertNotNull(st);
        Surface s = new Surface(st);
        boolean res = setSurface(s);
        s.release();
        return res;
    }

    private String getMediaString() {
        int mediaIndex = 0; // default: 480x360
        switch(mVideoQuality) {
        case CamcorderProfile.QUALITY_1080P:
            mediaIndex = 3;
            break;
        case CamcorderProfile.QUALITY_720P:
            mediaIndex = 2;
            break;
        case CamcorderProfile.QUALITY_480P:
            mediaIndex = 1;
            break;
        }
        return WorkDir.getMediaDirString() + MEDIA[mediaIndex];
    }

    /**
     * creates OpenMaxAl Engine
    */
    public static native boolean createEngine();
    /**
     * set surface to render. should be called before creating Media player
     * @param surface
     */
    public static native boolean setSurface(Surface surface);

    public static native boolean createMediaPlayer(String fileUri);
    public static native boolean playOrPauseMediaPlayer(boolean play);
    public static native void shutdown();

    /** Load jni on initialization */
    static {
         System.loadLibrary("ctsmediastress_jni");
    }

}

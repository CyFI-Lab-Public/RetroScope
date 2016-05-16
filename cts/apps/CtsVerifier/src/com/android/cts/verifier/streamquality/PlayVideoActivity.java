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

package com.android.cts.verifier.streamquality;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.streamquality.StreamingVideoActivity.Stream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * Activity that plays a video and allows the user to select pass/fail after 60 seconds.
 */
public class PlayVideoActivity extends PassFailButtons.Activity
        implements SurfaceHolder.Callback, OnErrorListener, OnPreparedListener,
        OnVideoSizeChangedListener {
    /**
     * Intent extra defining the {@link Stream} information
     */
    static final String EXTRA_STREAM = "com.android.cts.verifier.streamquality.EXTRA_STREAM";

    private static final String TAG = PlayVideoActivity.class.getName();
    private static final long ENABLE_PASS_DELAY = 60 * 1000;

    private static final int FAIL_DIALOG_ID = 1;

    private final Runnable enablePassButton = new Runnable() {
        @Override public void run() {
            setEnablePassButton(true);
        }
    };

    private Stream mStream;
    private SurfaceHolder mHolder;
    private SurfaceView mSurfaceView;
    private FrameLayout mVideoFrame;
    private MediaPlayer mPlayer;
    private Handler mHandler = new Handler();
    private int mVideoWidth;
    private int mVideoHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sv_play);
        setPassFailButtonClickListeners();

        setEnablePassButton(false);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mVideoFrame = (FrameLayout) findViewById(R.id.videoframe);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mStream = (Stream) getIntent().getSerializableExtra(EXTRA_STREAM);
    }

    private void setEnablePassButton(boolean enable) {
        getPassButton().setEnabled(enable);
    }

    private void playVideo() {
        mPlayer = new MediaPlayer();
        mPlayer.setDisplay(mHolder);
        mPlayer.setScreenOnWhilePlaying(true);
        mPlayer.setOnVideoSizeChangedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnPreparedListener(this);
        try {
            mPlayer.setDataSource(mStream.uri);
        } catch (IOException e) {
            Log.e(TAG, "Unable to play video, setDataSource failed", e);
            showDialog(FAIL_DIALOG_ID);
            return;
        }
        mPlayer.prepareAsync();
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case FAIL_DIALOG_ID:
                return new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.sv_failed_title))
                        .setMessage(getString(R.string.sv_failed_message))
                        .setNegativeButton("Close", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PassFailButtons.setTestResultAndFinish(PlayVideoActivity.this,
                                        getTestId(), null, false);
                            }
                        })
                        .show();
            default:
                return super.onCreateDialog(id, args);
        }
    }

    @Override
    public String getTestId() {
        return getTestId(mStream.code);
    }

    public static String getTestId(String code) {
        return PlayVideoActivity.class.getName() + "_" + code;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // This test must be completed in one session
        mHandler.removeCallbacks(enablePassButton);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        playVideo();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Unable to play video, got onError with code " + what + ", extra " + extra);
        showDialog(FAIL_DIALOG_ID);
        return true;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (width != 0 && height != 0) {
            mVideoWidth = width;
            mVideoHeight = height;
            fillScreen();
        }
    }

    private void startVideoPlayback() {
        mPlayer.start();

        // Enable Pass button after 60 seconds
        mHandler.postDelayed(enablePassButton, ENABLE_PASS_DELAY);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        startVideoPlayback();
    }

    private void fillScreen() {
        mHolder.setFixedSize(mVideoWidth, mVideoHeight);
        Rect rect = new Rect();
        mVideoFrame.getDrawingRect(rect);
        LayoutParams lp = mSurfaceView.getLayoutParams();
        float aspectRatio = ((float) mVideoWidth) / mVideoHeight;
        if (rect.width() / aspectRatio <= rect.height()) {
            lp.width = rect.width();
            lp.height = (int) (rect.width() / aspectRatio);
        } else {
            lp.width = (int) (rect.height() * aspectRatio);
            lp.height = rect.height();
        }
        mSurfaceView.setLayoutParams(lp);
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}
}

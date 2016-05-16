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

package com.android.cts.verifier.sensors;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.cts.helpers.SensorNotSupportedException;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import com.android.cts.verifier.R;
import com.android.cts.verifier.TestResult;

import java.security.InvalidParameterException;
import java.util.concurrent.Semaphore;

/**
 * Base class to author Sensor semi-automated test cases.
 * These tests can only wait for operators to notify at some intervals, but the test needs to be
 * autonomous to verify the data collected.
 */
public abstract class BaseSensorSemiAutomatedTestActivity
        extends Activity
        implements View.OnClickListener, Runnable {
    protected final String LOG_TAG = "TestRunner";

    private final Semaphore mSemaphore = new Semaphore(0);

    private TextView mLogView;
    private View mNextView;
    private Thread mWorkerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snsr_semi_auto_test);
    }

    @Override
    public void onStart() {
        super.onStart();

        mLogView = (TextView) this.findViewById(R.id.log_text);
        mNextView = this.findViewById(R.id.next_button);
        mNextView.setOnClickListener(this);
        mLogView.setMovementMethod(new ScrollingMovementMethod());

        updateButton(false /*enabled*/);
        mWorkerThread = new Thread(this);
        mWorkerThread.start();
    }

    @Override
    public void onClick(View target) {
        mSemaphore.release();
    }

    @Override
    public void run() {
        String message = "";
        SensorTestResult testResult = SensorTestResult.PASS;
        try {
            onRun();
        } catch(SensorNotSupportedException e) {
            // the sensor is not supported/available in the device, log a warning and skip the test
            testResult = SensorTestResult.SKIPPED;
            message = e.getMessage();
        } catch(Throwable e) {
            testResult = SensorTestResult.FAIL;
            message = e.getMessage();
        }
        setTestResult(testResult, message);
        appendText("\nTest completed. Press 'Next' to finish.\n");
        waitForUser();
        finish();
    }

    /**
     * This is the method that subclasses will implement to define the operations that need to be
     * verified.
     * Any exception thrown will cause the test to fail, additionally mAssert can be used to verify
     * the tests state.
     *
     * throws Throwable
     */
    protected abstract void onRun() throws Throwable;

    /**
     * Helper methods for subclasses to interact with the UI and the operator.
     */
    protected void appendText(String text, int textColor) {
        this.runOnUiThread(new TextAppender(mLogView, text, textColor));
    }

    protected void appendText(String text) {
        this.runOnUiThread(new TextAppender(mLogView, text));
    }

    protected void clearText() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogView.setText("");
            }
        });
    }

    protected void updateButton(boolean enabled) {
        this.runOnUiThread(new ButtonEnabler(this.mNextView, enabled));
    }

    protected void waitForUser() {
        updateButton(true);
        try {
            mSemaphore.acquire();
        } catch(InterruptedException e) {}
        updateButton(false);
    }

    protected void logSuccess() {
        appendText("PASS", Color.GREEN);
    }

    protected void playSound() {
        MediaPlayer player = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI);
        player.start();
        try {
            Thread.sleep(500);
        } catch(InterruptedException e) {
        } finally {
            player.stop();
        }
    }

    /**
     * Private methods.
     */
    private String getTestId() {
        return this.getClass().getName();
    }

    private void setTestResult(SensorTestResult testResult, String message) {
        int textColor;
        switch(testResult) {
            case SKIPPED:
                textColor = Color.YELLOW;
                TestResult.setPassedResult(this, this.getTestId(), message);
                break;
            case PASS:
                textColor = Color.GREEN;
                TestResult.setPassedResult(this, this.getTestId(), message);
                break;
            case FAIL:
                textColor = Color.RED;
                TestResult.setFailedResult(this, this.getTestId(), message);
                break;
            default:
                throw new InvalidParameterException("Unrecognized testResult.");
        }
        appendText(message, textColor);
    }

    private enum SensorTestResult {
        SKIPPED,
        PASS,
        FAIL
    }

    private class TextAppender implements Runnable {
        private final TextView mTextView;
        private final SpannableStringBuilder mMessageBuilder;

        public TextAppender(TextView textView, String message, int textColor) {
            mTextView = textView;
            mMessageBuilder = new SpannableStringBuilder(message + "\n");

            ForegroundColorSpan colorSpan = new ForegroundColorSpan(textColor);
            mMessageBuilder.setSpan(
                    colorSpan,
                    0 /*start*/,
                    message.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        public TextAppender(TextView textView, String message) {
            this(textView, message, textView.getCurrentTextColor());
        }

        @Override
        public void run() {
            mTextView.append(mMessageBuilder);
        }
    }

    private class ButtonEnabler implements Runnable {
        private final View mButtonView;
        private final boolean mButtonEnabled;

        public ButtonEnabler(View buttonView, boolean buttonEnabled) {
            mButtonView = buttonView;
            mButtonEnabled = buttonEnabled;
        }

        @Override
        public void run() {
            mButtonView.setEnabled(mButtonEnabled);
        }
    }
}

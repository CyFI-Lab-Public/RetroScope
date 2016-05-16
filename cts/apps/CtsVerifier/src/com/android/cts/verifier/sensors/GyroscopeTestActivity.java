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

package com.android.cts.verifier.sensors;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.app.AlertDialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Manual test for testing the gyroscope sensor. This test consists of 6 steps for all the
 * different ways to rotate the device along the x, y, and z axis. It also raises a warning
 * if the values seem to high and may be degrees.
 */
public class GyroscopeTestActivity extends PassFailButtons.Activity {

    private static final int NUM_STAGES = 6;
    private static final String STAGE_INDEX_EXTRA = "stageIndex";

    private static final int BACKGROUND_BLACK = 0;
    private static final int BACKGROUND_RED = 1;
    private static final int BACKGROUND_GREEN = 2;

    private AtomicInteger mBackgroundColor = new AtomicInteger(BACKGROUND_BLACK);

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorListener mSensorListener;
    private GLSurfaceView mGLSurfaceView;
    private TextView mProgressText;
    private TextView mSensorText;

    private AlertDialog mNoGyroscopeWarningDialog;
    private AlertDialog mDegreesWarningDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snsr_gyro);
        setInfoResources(R.string.snsr_gyro_test, R.string.snsr_gyro_test_info, 0);
        setPassFailButtonClickListeners();

        // This activity is reused 6 times with different settings to test each rotation direction
        final int stageIndex = getIntent().getIntExtra(STAGE_INDEX_EXTRA, 0);
        Settings settings = getSettings(stageIndex);

        // Hitting the pass button goes to the next test activity. Only the last one ends the test.
        if (stageIndex + 1 < NUM_STAGES) {
            setPassButtonGoesToNextStage(stageIndex);
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorListener = new SensorListener(settings.mSensorEventIndex,
                settings.mExpectPositiveValue);

        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setRenderer(new RotationGuideRenderer(settings.mRotateX, settings.mRotateY,
                settings.mRotateZ));

        mProgressText = (TextView) findViewById(R.id.progress);
        mProgressText.setText(String.format(getString(R.string.snsr_gyro_test_progress),
                settings.mStageIndex + 1, settings.mTotalStages));

        mSensorText = (TextView) findViewById(R.id.sensor_value);
    }

    private Settings getSettings(int stageIndex) {
        switch (stageIndex) {
            case 0:
                return new Settings(stageIndex, NUM_STAGES, 0, 0, 1, 2, true);
            case 1:
                return new Settings(stageIndex, NUM_STAGES, 0, 0, -1, 2, false);
            case 2:
                return new Settings(stageIndex, NUM_STAGES, 0, 1, 0, 1, true);
            case 3:
                return new Settings(stageIndex, NUM_STAGES, 0, -1, 0, 1, false);
            case 4:
                return new Settings(stageIndex, NUM_STAGES, 1, 0, 0, 0, true);
            case 5:
                return new Settings(stageIndex, NUM_STAGES, -1, 0, 0, 0, false);
            default:
                throw new IllegalArgumentException("Bad stage index: " + stageIndex);
        }
    }

    /** Bundle of settings for testing a certain rotation direction. */
    class Settings {
        int mStageIndex;
        int mTotalStages;
        float mRotateX;
        float mRotateY;
        float mRotateZ;
        int mSensorEventIndex;
        boolean mExpectPositiveValue;

        Settings(int stageIndex, int totalStages, float rotateX, float rotateY, float rotateZ,
                int sensorEventIndex, boolean expectPositiveValue) {
            mStageIndex = stageIndex;
            mTotalStages = totalStages;
            mRotateX = rotateX;
            mRotateY = rotateY;
            mRotateZ = rotateZ;
            mSensorEventIndex = sensorEventIndex;
            mExpectPositiveValue = expectPositiveValue;
        }
    }

    private void setPassButtonGoesToNextStage(final int stageIndex) {
        findViewById(R.id.pass_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GyroscopeTestActivity.this,
                        GyroscopeTestActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                intent.putExtra(STAGE_INDEX_EXTRA, stageIndex + 1);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mSensorManager.registerListener(mSensorListener, mSensor,
                SensorManager.SENSOR_DELAY_UI)) {
            showNoGyroscopeWarningDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorListener, mSensor);
    }

    /** Renders a spinning block to indicate how the device should be rotated in the test. */
    class RotationGuideRenderer implements GLSurfaceView.Renderer {

        private static final double ANGLE_INCREMENT = 1.0;

        private final Monolith mMonolith = new Monolith();

        private float mAngle = 0.0f;

        private float mRotateX;

        private float mRotateY;

        private float mRotateZ;

        public RotationGuideRenderer(float rotateX, float rotateY, float rotateZ) {
            mRotateX = rotateX;
            mRotateY = rotateY;
            mRotateZ = rotateZ;
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            clearBackground(gl);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glRotatef(mAngle, mRotateX, mRotateY, mRotateZ);
            mMonolith.draw(gl);
            mAngle += ANGLE_INCREMENT;
        }

        private void clearBackground(GL10 gl) {
            switch (mBackgroundColor.get()) {
                case BACKGROUND_GREEN:
                    gl.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                    break;

                case BACKGROUND_RED:
                    gl.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
                    break;

                default:
                    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                    break;
            }
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            float ratio = (float) width / height;
            gl.glFrustumf(-ratio, ratio, -1, 1, 3, 15);
            GLU.gluLookAt(gl, 0, 0, 10, 0, 0, 0, 0, 1, 0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnable(GL10.GL_LIGHTING);
            gl.glEnable(GL10.GL_LIGHT0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, new float[] {0.75f, 0.75f, 0.75f, 1f}, 0);
        }
    }

    /** Rectangular block that is rotated by {@link RotationGuideRenderer}. */
    class Monolith {

        private static final int NUM_VERTICES = 8;

        private static final int NUM_INDICES = 36;

        private FloatBuffer mVertexBuffer;

        private ShortBuffer mIndexBuffer;

        public Monolith() {
            mVertexBuffer = ByteBuffer.allocateDirect(NUM_VERTICES * 3 * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            float[] coordinates = {
                    -0.65f, -1, 0.2f,
                    -0.65f, 1, 0.2f,
                    0.65f, 1, 0.2f,
                    0.65f, -1, 0.2f,

                    -0.65f, -1, -0.2f,
                    -0.65f, 1, -0.2f,
                    0.65f, 1, -0.2f,
                    0.65f, -1, -0.2f,
            };

            for (int i = 0; i < coordinates.length; i++) {
                mVertexBuffer.put(coordinates[i]);
            }

            mIndexBuffer = ByteBuffer.allocateDirect(NUM_INDICES * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();

            // Front
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 2);
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 2);
            mIndexBuffer.put((short) 3);

            // Back
            mIndexBuffer.put((short) 7);
            mIndexBuffer.put((short) 6);
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 7);
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 4);

            // Right
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 2);
            mIndexBuffer.put((short) 6);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 6);
            mIndexBuffer.put((short) 7);

            // Left
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 0);

            // Top
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 6);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 6);
            mIndexBuffer.put((short) 2);

            // Bottom
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 7);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 0);

            mVertexBuffer.position(0);
            mIndexBuffer.position(0);
        }

        public void draw(GL10 gl) {
            gl.glColor4f(0.5f, 0.5f, 0.5f, 1f);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
            gl.glDrawElements(GL10.GL_TRIANGLES, NUM_INDICES, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
        }
    }

    class SensorListener implements SensorEventListener {

        /** Throw away other events that are smaller than this. */
        private static final double MOVING_AMOUNT = 0.1;

        private final int mEventIndex;

        private final boolean mExpectPositive;

        SensorListener(int eventIndex, boolean expectPositive) {
            mEventIndex = eventIndex;
            mExpectPositive = expectPositive;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float value = event.values[mEventIndex];
            if (value > MOVING_AMOUNT) {
                if (mExpectPositive) {
                    updateWidgets(value, BACKGROUND_GREEN, R.drawable.fs_good);
                } else {
                    updateWidgets(value, BACKGROUND_RED, R.drawable.fs_error);
                }
            } else if (value < -MOVING_AMOUNT) {
                if (mExpectPositive) {
                    updateWidgets(value, BACKGROUND_RED, R.drawable.fs_error);
                } else {
                    updateWidgets(value, BACKGROUND_GREEN, R.drawable.fs_good);
                }
            } else {
                updateWidgets(value, BACKGROUND_BLACK, R.drawable.fs_indeterminate);
            }

            if (value > 10) {
                showDegreesWarningDialog();
            }
        }

        void updateWidgets(float sensorValue, int backgroundColor, int icon) {
            synchronized (GyroscopeTestActivity.this) {
                mBackgroundColor.set(backgroundColor);
            }
            mSensorText.setText(String.format("%+.2f", sensorValue));
            mSensorText.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private void showNoGyroscopeWarningDialog() {
        if (mNoGyroscopeWarningDialog == null) {
            mNoGyroscopeWarningDialog = new AlertDialog.Builder(GyroscopeTestActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.snsr_gyro_test_no_gyro_title)
                .setMessage(R.string.snsr_gyro_test_no_gyro_message)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        }
        if (!mNoGyroscopeWarningDialog.isShowing()) {
            mNoGyroscopeWarningDialog.show();
        }
    }

    private void showDegreesWarningDialog() {
        if (mDegreesWarningDialog == null) {
            mDegreesWarningDialog = new AlertDialog.Builder(GyroscopeTestActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.snsr_gyro_test_degrees_title)
                    .setMessage(R.string.snsr_gyro_test_degrees_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
        if (!mDegreesWarningDialog.isShowing()) {
            mDegreesWarningDialog.show();
        }
    }
}

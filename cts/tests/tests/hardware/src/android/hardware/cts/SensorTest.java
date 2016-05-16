/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.hardware.cts;

import java.lang.IllegalArgumentException;
import java.lang.Override;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

public class SensorTest extends AndroidTestCase {
    private SensorManager mSensorManager;
    private TriggerListener mTriggerListener;
    private SensorListener mSensorListener;
    private ArrayList<Sensor> mContinuousSensorList;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mTriggerListener = new TriggerListener();
        mSensorListener = new SensorListener();
        mContinuousSensorList = new ArrayList<Sensor>();
        for (int i = Sensor.TYPE_ACCELEROMETER; i <= Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR; ++i) {
            Sensor sensor = mSensorManager.getDefaultSensor(i);
            // Skip all non-continuous mode sensors.
            if (sensor == null || Sensor.TYPE_SIGNIFICANT_MOTION == i ||
                Sensor.TYPE_STEP_COUNTER == i || Sensor.TYPE_STEP_DETECTOR == i ||
                Sensor.TYPE_LIGHT == i || Sensor.TYPE_PROXIMITY == i ||
                Sensor.TYPE_AMBIENT_TEMPERATURE == i) {
                continue;
            }
            mContinuousSensorList.add(sensor);
        }
    }

    public void testSensorOperations() {
        // Because we can't know every sensors unit details, so we can't assert
        // get values with specified values.
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        assertNotNull(sensors);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean hasAccelerometer = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        // accelerometer sensor is optional
        if (hasAccelerometer) {
            assertEquals(Sensor.TYPE_ACCELEROMETER, sensor.getType());
            assertSensorValues(sensor);
        } else {
            assertNull(sensor);
        }

        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        boolean hasStepCounter = getContext().getPackageManager().hasSystemFeature(
                                        PackageManager.FEATURE_SENSOR_STEP_COUNTER);
        // stepcounter sensor is optional
        if (hasStepCounter) {
            assertEquals(Sensor.TYPE_STEP_COUNTER, sensor.getType());
            assertSensorValues(sensor);
        } else {
            assertNull(sensor);
        }

        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        boolean hasStepDetector = getContext().getPackageManager().hasSystemFeature(
                                        PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
        // stepdetector sensor is optional
        if (hasStepDetector) {
            assertEquals(Sensor.TYPE_STEP_DETECTOR, sensor.getType());
            assertSensorValues(sensor);
        } else {
            assertNull(sensor);
        }

        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        boolean hasCompass = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_SENSOR_COMPASS);
        // compass sensor is optional
        if (hasCompass) {
            assertEquals(Sensor.TYPE_MAGNETIC_FIELD, sensor.getType());
            assertSensorValues(sensor);
        } else {
            assertNull(sensor);
        }

        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        // orientation sensor is required if the device can physically implement it
        if (hasCompass && hasAccelerometer) {
            assertEquals(Sensor.TYPE_ORIENTATION, sensor.getType());
            assertSensorValues(sensor);
        }

        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
        // temperature sensor is optional
        if (sensor != null) {
            assertEquals(Sensor.TYPE_TEMPERATURE, sensor.getType());
            assertSensorValues(sensor);
        }
    }

    public void testValuesForAllSensors() {
        for (int i = Sensor.TYPE_ACCELEROMETER; i <= Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR; ++i) {
            Sensor sensor = mSensorManager.getDefaultSensor(i);
            if (sensor != null) {
                assertSensorValues(sensor);
            }
        }
    }

    public void testRequestTriggerWithNonTriggerSensor() {
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean result;
        if (sensor != null) {
            result = mSensorManager.requestTriggerSensor(mTriggerListener, sensor);
            assertFalse(result);
        }
    }

    public void testCancelTriggerWithNonTriggerSensor() {
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean result;
        if (sensor != null) {
            result = mSensorManager.cancelTriggerSensor(mTriggerListener, sensor);
            assertFalse(result);
        }
    }

    public void testRegisterWithTriggerSensor() {
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        boolean result;
        if (sensor != null) {
            result = mSensorManager.registerListener(mSensorListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            assertFalse(result);
        }
    }

    public void testRegisterTwiceWithSameSensor() {
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean result;
        if (sensor != null) {
            result = mSensorManager.registerListener(mSensorListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            assertTrue(result);
            result = mSensorManager.registerListener(mSensorListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            assertFalse(result);
        }
    }

    // Register for updates from each continuous mode sensor, wait for 25 events, call flush and
    // wait for flushCompleteEvent before unregistering for the sensor.
    public void testBatchAndFlush() throws Exception {
        for (Sensor sensor : mContinuousSensorList) {
            final CountDownLatch eventReceived = new CountDownLatch(25);
            final CountDownLatch flushReceived = new CountDownLatch(1);
            SensorEventListener2 listener = new SensorEventListener2() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    eventReceived.countDown();
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

                @Override
                public void onFlushCompleted(Sensor sensor) {
                    flushReceived.countDown();
                }
            };
            boolean result = mSensorManager.registerListener(listener, sensor,
                                            SensorManager.SENSOR_DELAY_NORMAL, 10000000);
            assertTrue(result);
            // Wait for 25 events and call flush.
            eventReceived.await();
            result = mSensorManager.flush(listener);
            assertTrue(result);
            flushReceived.await();
            mSensorManager.unregisterListener(listener);
        }
    }

    // Same as testBatchAndFlush but using Handler version of the API to register for sensors.
    // onSensorChanged is now called on a background thread.
    public void testBatchAndFlushWithHandler() throws Exception {
        for (Sensor sensor : mContinuousSensorList) {
            final CountDownLatch eventReceived = new CountDownLatch(25);
            final CountDownLatch flushReceived = new CountDownLatch(1);
            SensorEventListener2 listener = new SensorEventListener2() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    eventReceived.countDown();
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

                @Override
                public void onFlushCompleted(Sensor sensor) {
                    flushReceived.countDown();
                }
            };
            HandlerThread handlerThread = new HandlerThread("sensorThread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());
            boolean result = mSensorManager.registerListener(listener, sensor,
                                            SensorManager.SENSOR_DELAY_NORMAL, 15000000,
                                            handler);
            assertTrue(result);
            // Wait for 25 events and call flush.
            eventReceived.await();
            result = mSensorManager.flush(listener);
            assertTrue(result);
            flushReceived.await();
            mSensorManager.unregisterListener(listener);
        }
    }

    // Call registerListener for multiple sensors at a time and call flush.
    public void testBatchAndFlushWithMutipleSensors() throws Exception {
        int numSensors = mContinuousSensorList.size() < 3 ? mContinuousSensorList.size() : 3;
        if (numSensors == 0) {
            return;
        }
        final CountDownLatch eventReceived = new CountDownLatch(numSensors * 50);
        final CountDownLatch flushReceived = new CountDownLatch(numSensors);
        SensorEventListener2 listener = new SensorEventListener2() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                eventReceived.countDown();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onFlushCompleted(Sensor sensor) {
                flushReceived.countDown();
            }
        };
        for (int i = 0; i < numSensors; ++i) {
            Sensor sensor = mContinuousSensorList.get(i);
            boolean result = mSensorManager.registerListener(listener, sensor,
                                            SensorManager.SENSOR_DELAY_FASTEST);
            assertTrue(result);
        }
        // Wait for N events and call flush.
        eventReceived.await();
        boolean result = mSensorManager.flush(listener);
        assertTrue(result);
        flushReceived.await();
        mSensorManager.unregisterListener(listener);
    }

    private void assertSensorValues(Sensor sensor) {
        assertTrue(sensor.getMaximumRange() >= 0);
        assertTrue(sensor.getPower() >= 0);
        assertTrue(sensor.getResolution() >= 0);
        assertNotNull(sensor.getVendor());
        assertTrue(sensor.getVersion() > 0);
        assertTrue(sensor.getFifoMaxEventCount() >= 0);
        assertTrue(sensor.getFifoReservedEventCount() >= 0);
    }

    @SuppressWarnings("deprecation")
    public void testLegacySensorOperations() {
        final SensorManager mSensorManager =
                (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);

        // We expect the set of sensors reported by the new and legacy APIs to be consistent.
        int sensors = 0;
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensors |= SensorManager.SENSOR_ACCELEROMETER;
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            sensors |= SensorManager.SENSOR_MAGNETIC_FIELD;
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {
            sensors |= SensorManager.SENSOR_ORIENTATION | SensorManager.SENSOR_ORIENTATION_RAW;
        }
        assertEquals(sensors, mSensorManager.getSensors());
    }

    class TriggerListener extends TriggerEventListener {
        @Override
        public void onTrigger(TriggerEvent event) {
        }
    }

    class SensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}

/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.app.cts;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.location.LocationManager;
import android.net.sip.SipManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.telephony.TelephonyManager;
import android.test.InstrumentationTestCase;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test for checking that the {@link PackageManager} is reporting the correct features.
 */
public class SystemFeaturesTest extends InstrumentationTestCase {

    private Context mContext;
    private PackageManager mPackageManager;
    private HashSet<String> mAvailableFeatures;

    private ActivityManager mActivityManager;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Instrumentation instrumentation = getInstrumentation();
        mContext = instrumentation.getTargetContext();
        mPackageManager = mContext.getPackageManager();
        mAvailableFeatures = new HashSet<String>();
        if (mPackageManager.getSystemAvailableFeatures() != null) {
            for (FeatureInfo feature : mPackageManager.getSystemAvailableFeatures()) {
                mAvailableFeatures.add(feature.name);
            }
        }
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Check for features improperly prefixed with "android." that are not defined in
     * {@link PackageManager}.
     */
    public void testFeatureNamespaces() throws IllegalArgumentException, IllegalAccessException {
        Set<String> officialFeatures = getFeatureConstantsNames("FEATURE_");
        assertFalse(officialFeatures.isEmpty());

        Set<String> notOfficialFeatures = new HashSet<String>(mAvailableFeatures);
        notOfficialFeatures.removeAll(officialFeatures);

        for (String featureName : notOfficialFeatures) {
            if (featureName != null) {
                assertFalse("Use a different namespace than 'android' for " + featureName,
                        featureName.startsWith("android"));
            }
        }
    }

    public void testBluetoothFeature() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            assertAvailable(PackageManager.FEATURE_BLUETOOTH);
        } else {
            assertNotAvailable(PackageManager.FEATURE_BLUETOOTH);
        }
    }

    public void testCameraFeatures() {
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            assertNotAvailable(PackageManager.FEATURE_CAMERA);
            assertNotAvailable(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
            assertNotAvailable(PackageManager.FEATURE_CAMERA_FLASH);
            assertNotAvailable(PackageManager.FEATURE_CAMERA_FRONT);
            assertNotAvailable(PackageManager.FEATURE_CAMERA_ANY);
        } else {
            assertAvailable(PackageManager.FEATURE_CAMERA_ANY);
            checkFrontCamera();
            checkRearCamera();
        }
    }

    private void checkFrontCamera() {
        CameraInfo info = new CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        int frontCameraId = -1;
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
            }
        }

        if (frontCameraId > -1) {
            assertAvailable(PackageManager.FEATURE_CAMERA_FRONT);
        } else {
            assertNotAvailable(PackageManager.FEATURE_CAMERA_FRONT);
        }
    }

    private void checkRearCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
            if (camera != null) {
                assertAvailable(PackageManager.FEATURE_CAMERA);

                Camera.Parameters params = camera.getParameters();
                if (params.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_AUTO)) {
                    assertAvailable(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
                } else {
                    assertNotAvailable(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
                }

                if (params.getFlashMode() != null) {
                    assertAvailable(PackageManager.FEATURE_CAMERA_FLASH);
                } else {
                    assertNotAvailable(PackageManager.FEATURE_CAMERA_FLASH);
                }
            } else {
                assertNotAvailable(PackageManager.FEATURE_CAMERA);
                assertNotAvailable(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
                assertNotAvailable(PackageManager.FEATURE_CAMERA_FLASH);
            }
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    public void testLiveWallpaperFeature() {
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            assertAvailable(PackageManager.FEATURE_LIVE_WALLPAPER);
        } catch (ActivityNotFoundException e) {
            assertNotAvailable(PackageManager.FEATURE_LIVE_WALLPAPER);
        }
    }

    public void testLocationFeatures() {
        if (mLocationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            assertAvailable(PackageManager.FEATURE_LOCATION);
            assertAvailable(PackageManager.FEATURE_LOCATION_GPS);
        } else {
            assertNotAvailable(PackageManager.FEATURE_LOCATION_GPS);
        }

        if (mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            assertAvailable(PackageManager.FEATURE_LOCATION);
            assertAvailable(PackageManager.FEATURE_LOCATION_NETWORK);
        } else {
            assertNotAvailable(PackageManager.FEATURE_LOCATION_NETWORK);
        }
    }

    public void testNfcFeatures() {
        if (NfcAdapter.getDefaultAdapter(mContext) != null) {
            assertAvailable(PackageManager.FEATURE_NFC);
        } else {
            assertNotAvailable(PackageManager.FEATURE_NFC);
        }
    }

    public void testScreenFeatures() {
        assertTrue(mPackageManager.hasSystemFeature(PackageManager.FEATURE_SCREEN_LANDSCAPE)
                || mPackageManager.hasSystemFeature(PackageManager.FEATURE_SCREEN_PORTRAIT));
    }

    /**
     * Check that the sensor features reported by the PackageManager correspond to the sensors
     * returned by {@link SensorManager#getSensorList(int)}.
     */
    public void testSensorFeatures() throws Exception {
        Set<String> featuresLeft = getFeatureConstantsNames("FEATURE_SENSOR_");

        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_ACCELEROMETER,
                Sensor.TYPE_ACCELEROMETER);
        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_BAROMETER,
                Sensor.TYPE_PRESSURE);
        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_COMPASS,
                Sensor.TYPE_MAGNETIC_FIELD);
        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_GYROSCOPE,
                Sensor.TYPE_GYROSCOPE);
        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_LIGHT,
                Sensor.TYPE_LIGHT);
        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_PROXIMITY,
                Sensor.TYPE_PROXIMITY);
        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_STEP_COUNTER,
                Sensor.TYPE_STEP_COUNTER);
        assertFeatureForSensor(featuresLeft, PackageManager.FEATURE_SENSOR_STEP_DETECTOR,
                Sensor.TYPE_STEP_DETECTOR);

        assertTrue("Assertions need to be added to this test for " + featuresLeft,
                featuresLeft.isEmpty());
    }

    /** Get a list of feature constants in PackageManager matching a prefix. */
    private static Set<String> getFeatureConstantsNames(String prefix)
            throws IllegalArgumentException, IllegalAccessException {
        Set<String> features = new HashSet<String>();
        Field[] fields = PackageManager.class.getFields();
        for (Field field : fields) {
            if (field.getName().startsWith(prefix)) {
                String feature = (String) field.get(null);
                features.add(feature);
            }
        }
        return features;
    }

    public void testSipFeatures() {
        if (SipManager.newInstance(mContext) != null) {
            assertAvailable(PackageManager.FEATURE_SIP);
        } else {
            assertNotAvailable(PackageManager.FEATURE_SIP);
            assertNotAvailable(PackageManager.FEATURE_SIP_VOIP);
        }

        if (SipManager.isApiSupported(mContext)) {
            assertAvailable(PackageManager.FEATURE_SIP);
        } else {
            assertNotAvailable(PackageManager.FEATURE_SIP);
            assertNotAvailable(PackageManager.FEATURE_SIP_VOIP);
        }

        if (SipManager.isVoipSupported(mContext)) {
            assertAvailable(PackageManager.FEATURE_SIP);
            assertAvailable(PackageManager.FEATURE_SIP_VOIP);
        } else {
            assertNotAvailable(PackageManager.FEATURE_SIP_VOIP);
        }
    }

    /**
     * Check that if the PackageManager declares a sensor feature that the device has at least
     * one sensor that matches that feature. Also check that if a PackageManager does not declare
     * a sensor that the device also does not have such a sensor.
     *
     * @param featuresLeft to check in order to make sure the test covers all sensor features
     * @param expectedFeature that the PackageManager may report
     * @param expectedSensorType that that {@link SensorManager#getSensorList(int)} may have
     */
    private void assertFeatureForSensor(Set<String> featuresLeft, String expectedFeature,
            int expectedSensorType) {
        assertTrue("Features left " + featuresLeft + " to check did not include "
                + expectedFeature, featuresLeft.remove(expectedFeature));

        boolean hasSensorFeature = mPackageManager.hasSystemFeature(expectedFeature);

        List<Sensor> sensors = mSensorManager.getSensorList(expectedSensorType);
        List<String> sensorNames = new ArrayList<String>(sensors.size());
        for (Sensor sensor : sensors) {
            sensorNames.add(sensor.getName());
        }
        boolean hasSensorType = !sensors.isEmpty();

        String message = "PackageManager#hasSystemFeature(" + expectedFeature + ") returns "
                + hasSensorFeature
                + " but SensorManager#getSensorList(" + expectedSensorType + ") shows sensors "
                + sensorNames;

        assertEquals(message, hasSensorFeature, hasSensorType);
    }

    /**
     * Check that the {@link TelephonyManager#getPhoneType()} matches the reported features.
     */
    public void testTelephonyFeatures() {
        int phoneType = mTelephonyManager.getPhoneType();
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_GSM:
                assertAvailable(PackageManager.FEATURE_TELEPHONY);
                assertAvailable(PackageManager.FEATURE_TELEPHONY_GSM);
                break;

            case TelephonyManager.PHONE_TYPE_CDMA:
                assertAvailable(PackageManager.FEATURE_TELEPHONY);
                assertAvailable(PackageManager.FEATURE_TELEPHONY_CDMA);
                break;

            case TelephonyManager.PHONE_TYPE_NONE:
                assertNotAvailable(PackageManager.FEATURE_TELEPHONY);
                assertNotAvailable(PackageManager.FEATURE_TELEPHONY_CDMA);
                assertNotAvailable(PackageManager.FEATURE_TELEPHONY_GSM);
                break;

            default:
                throw new IllegalArgumentException("Did you add a new phone type? " + phoneType);
        }
    }

    public void testTouchScreenFeatures() {
        ConfigurationInfo configInfo = mActivityManager.getDeviceConfigurationInfo();
        if (configInfo.reqTouchScreen != Configuration.TOUCHSCREEN_NOTOUCH) {
            assertAvailable(PackageManager.FEATURE_TOUCHSCREEN);
        } else {
            assertNotAvailable(PackageManager.FEATURE_TOUCHSCREEN);
        }

        // TODO: Add tests for the other touchscreen features.
    }

    public void testUsbAccessory() {
        assertAvailable(PackageManager.FEATURE_USB_ACCESSORY);
    }

    public void testWifiFeature() throws Exception {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            // no WiFi, skip the test
            return;
        }
        boolean enabled = mWifiManager.isWifiEnabled();
        try {
            // WifiManager is hard-coded to return true,
            // the case without WiFi is already handled,
            // so this case MUST have WiFi.
            if (mWifiManager.setWifiEnabled(true)) {
                assertAvailable(PackageManager.FEATURE_WIFI);
            }
        } finally {
            if (!enabled) {
                mWifiManager.setWifiEnabled(false);
            }
        }
    }

    private void assertAvailable(String feature) {
        assertTrue("PackageManager#hasSystemFeature should return true for " + feature,
                mPackageManager.hasSystemFeature(feature));
        assertTrue("PackageManager#getSystemAvailableFeatures should have " + feature,
                mAvailableFeatures.contains(feature));
    }

    private void assertNotAvailable(String feature) {
        assertFalse("PackageManager#hasSystemFeature should NOT return true for " + feature,
                mPackageManager.hasSystemFeature(feature));
        assertFalse("PackageManager#getSystemAvailableFeatures should NOT have " + feature,
                mAvailableFeatures.contains(feature));
    }
}

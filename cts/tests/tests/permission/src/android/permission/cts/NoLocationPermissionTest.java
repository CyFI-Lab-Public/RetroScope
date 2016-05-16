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

package android.permission.cts;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.List;

/**
 * Verify the location access without specific permissions.
 */
public class NoLocationPermissionTest extends AndroidTestCase {
    private static final String TEST_PROVIDER_NAME = "testProvider";

    private LocationManager mLocationManager;
    private List<String> mAllProviders;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mLocationManager = (LocationManager) getContext().getSystemService(
                Context.LOCATION_SERVICE);
        mAllProviders = mLocationManager.getAllProviders();

        assertNotNull(mLocationManager);
        assertNotNull(mAllProviders);
    }

    private boolean isKnownLocationProvider(String provider) {
        return mAllProviders.contains(provider);
    }

    /**
     * Verify that listen or get cell location requires permissions.
     * <p>
     * Requires Permission: {@link
     * android.Manifest.permission#ACCESS_COARSE_LOCATION.}
     */
    @SmallTest
    public void testListenCellLocation() {
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener();
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
            fail("TelephonyManager.listen(LISTEN_CELL_LOCATION) did not" +
                    " throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        try {
            telephonyManager.getCellLocation();
            fail("TelephonyManager.getCellLocation did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that get cell location requires permissions.
     * <p>
     * Requires Permission: {@link
     * android.Manifest.permission#ACCESS_COARSE_LOCATION.}
     */
    @SmallTest
    public void testListenCellLocation2() {
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener();

        try {
            telephonyManager.getNeighboringCellInfo();
            fail("TelephonyManager.getNeighbouringCellInfo did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        try {
            telephonyManager.getAllCellInfo();
            fail("TelephonyManager.getAllCellInfo did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Helper method to verify that calling requestLocationUpdates with given
     * provider throws SecurityException.
     * 
     * @param provider the String provider name.
     */
    private void checkRequestLocationUpdates(String provider) {
        if (!isKnownLocationProvider(provider)) {
            // skip this test if the provider is unknown
            return;
        }

        LocationListener mockListener = new MockLocationListener();
        Looper looper = Looper.myLooper();
        try {
            mLocationManager.requestLocationUpdates(provider, 0, 0, mockListener);
            fail("LocationManager.requestLocationUpdates did not" +
                    " throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mLocationManager.requestLocationUpdates(provider, 0, 0, mockListener, looper);
            fail("LocationManager.requestLocationUpdates did not" +
                    " throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that listening for network requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testRequestLocationUpdatesNetwork() {
        checkRequestLocationUpdates(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Verify that listening for GPS location requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testRequestLocationUpdatesGps() {
        checkRequestLocationUpdates(LocationManager.GPS_PROVIDER);
    }

    /**
     * Verify that adding a proximity alert requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testAddProximityAlert() {
        PendingIntent mockPendingIntent = PendingIntent.getBroadcast(getContext(),
                0, new Intent("mockIntent"), PendingIntent.FLAG_ONE_SHOT);
        try {
            mLocationManager.addProximityAlert(0, 0, 100, -1, mockPendingIntent);
            fail("LocationManager.addProximityAlert did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Helper method to verify that calling getLastKnownLocation with given
     * provider throws SecurityException.
     * 
     * @param provider the String provider name.
     */
    private void checkGetLastKnownLocation(String provider) {
        if (!isKnownLocationProvider(provider)) {
            // skip this test if the provider is unknown
            return;
        }

        try {
            mLocationManager.getLastKnownLocation(provider);
            fail("LocationManager.getLastKnownLocation did not" +
                    " throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that getting the last known GPS location requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testGetLastKnownLocationGps() {
        checkGetLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    /**
     * Verify that getting the last known network location requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testGetLastKnownLocationNetwork() {
        checkGetLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Helper method to verify that calling getProvider with given provider
     * throws SecurityException.
     * 
     * @param provider the String provider name.
     */
    private void checkGetProvider(String provider) {
        if (!isKnownLocationProvider(provider)) {
            // skip this test if the provider is unknown
            return;
        }

        try {
            mLocationManager.getProvider(provider);
            fail("LocationManager.getProvider did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that getting the GPS provider requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testGetProviderGps() {
        checkGetProvider(LocationManager.GPS_PROVIDER);
    }

    /**
     * Verify that getting the network provider requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}.
     */
    // TODO: remove from small test suite until network provider can be enabled
    // on test devices
    // @SmallTest
    public void testGetProviderNetwork() {
        checkGetProvider(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Helper method to verify that calling isProviderEnabled with given
     * provider throws SecurityException.
     * 
     * @param provider the String provider name.
     */
    private void checkIsProviderEnabled(String provider) {
        if (!isKnownLocationProvider(provider)) {
            // skip this test if the provider is unknown
            return;
        }

        try {
            mLocationManager.isProviderEnabled(provider);
            fail("LocationManager.isProviderEnabled did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that checking IsProviderEnabled for GPS requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testIsProviderEnabledGps() {
        checkIsProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Verify that checking IsProviderEnabled for network requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SmallTest
    public void testIsProviderEnabledNetwork() {
        checkIsProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Verify that checking addTestProvider for network requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testAddTestProvider() {
        final int TEST_POWER_REQUIREMENT_VALE = 0;
        final int TEST_ACCURACY_VALUE = 1;

        try {
            mLocationManager.addTestProvider(TEST_PROVIDER_NAME, true, true, true, true,
                    true, true, true, TEST_POWER_REQUIREMENT_VALE, TEST_ACCURACY_VALUE);
            fail("LocationManager.addTestProvider did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that checking removeTestProvider for network requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testRemoveTestProvider() {
        try {
            mLocationManager.removeTestProvider(TEST_PROVIDER_NAME);
            fail("LocationManager.removeTestProvider did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that checking setTestProviderLocation for network requires
     * permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testSetTestProviderLocation() {
        Location location = new Location(TEST_PROVIDER_NAME);
        location.makeComplete();

        try {
            mLocationManager.setTestProviderLocation(TEST_PROVIDER_NAME, location);
            fail("LocationManager.setTestProviderLocation did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that checking clearTestProviderLocation for network requires
     * permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testClearTestProviderLocation() {
        try {
            mLocationManager.clearTestProviderLocation(TEST_PROVIDER_NAME);
            fail("LocationManager.clearTestProviderLocation did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that checking setTestProviderEnabled requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testSetTestProviderEnabled() {
        try {
            mLocationManager.setTestProviderEnabled(TEST_PROVIDER_NAME, true);
            fail("LocationManager.setTestProviderEnabled did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that checking clearTestProviderEnabled requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testClearTestProviderEnabled() {
        try {
            mLocationManager.clearTestProviderEnabled(TEST_PROVIDER_NAME);
            fail("LocationManager.setTestProviderEnabled did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that checking setTestProviderStatus requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testSetTestProviderStatus() {
        try {
            mLocationManager.setTestProviderStatus(TEST_PROVIDER_NAME, 0, Bundle.EMPTY, 0);
            fail("LocationManager.setTestProviderStatus did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
        }
    }

    /**
     * Verify that checking clearTestProviderStatus requires permissions.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#ACCESS_MOCK_LOCATION}.
     */
    @SmallTest
    public void testClearTestProviderStatus() {
        try {
            mLocationManager.clearTestProviderStatus(TEST_PROVIDER_NAME);
            fail("LocationManager.setTestProviderStatus did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    private static class MockLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
            // ignore
        }

        public void onProviderDisabled(String provider) {
            // ignore
        }

        public void onProviderEnabled(String provider) {
            // ignore
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // ignore
        }
    }
}

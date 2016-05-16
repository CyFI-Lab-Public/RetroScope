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

package android.location2.cts;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.test.InstrumentationTestCase;

import java.util.List;
import java.lang.Thread;

/**
 * Requires the permissions
 * android.permission.ACCESS_MOCK_LOCATION to mock provider
 * android.permission.ACCESS_COARSE_LOCATION to access network provider
 * android.permission.ACCESS_LOCATION_EXTRA_COMMANDS to send extra commands to provider
 */
public class LocationManagerTest extends InstrumentationTestCase {

    private static final long TEST_TIME_OUT_MS = 10 * 1000;

    private static final double LAT = 10.0;
    private static final double LNG = 40.0;
    private static final double FUDGER_DELTA = 0.2;

    private LocationManager mManager;

    private Context mContext;

    private PendingIntent mPendingIntent;

    private TestIntentReceiver mIntentReceiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();

        mManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // test that mock locations are allowed so a more descriptive error message can be logged
        if (Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 0) {
            fail("Mock locations are currently disabled in Settings - this test requires "
                    + "mock locations");
        }
    }

    public void testGetGpsProvider_notAllowed() {
        doTestGetFineProvider_notAllowed(LocationManager.GPS_PROVIDER);
    }

    public void testGetFineProvider_notAllowed() {
        doTestGetFineProvider_notAllowed("my fine provider name");
    }

    private void doTestGetFineProvider_notAllowed(String providerName) {
        addTestProvider(providerName, Criteria.ACCURACY_FINE, false, true, false);

        try {
            mManager.getProvider(providerName);
            fail("LocationManager.getProvider() did not throw SecurityException as expected");
        } catch (SecurityException expected) {
        } finally {
            removeTestProvider(providerName);
        }
    }

    /**
     * Work around b/11446702 by clearing the test provider before removing it
     */
    private void removeTestProvider(String providerName) {
        mManager.clearTestProviderEnabled(providerName);
        mManager.removeTestProvider(providerName);
    }

    public void testGetNetworkProvider_allowed() {
        doTestGetCoarseProvider_allowed(LocationManager.NETWORK_PROVIDER);
    }

    public void testGetCoarseProvider_allowed() {
        doTestGetCoarseProvider_allowed("my coarse provider name");
    }

    public void doTestGetCoarseProvider_allowed(String providerName) {
        try {
            addTestProvider(providerName, Criteria.ACCURACY_COARSE, true, false, true);
            assertNotNull(mManager.getProvider(providerName));
        } finally {
            removeTestProvider(providerName);
        }
    }

    public void testGetNetworkProviderLocationUpdates_withIntent() {
        doTestGetLocationUpdates_withIntent(LocationManager.NETWORK_PROVIDER);
    }

    public void testGetNetworkProviderLocationUpdates_withListener() {
        doTestGetLocationUpdates_withListener(LocationManager.NETWORK_PROVIDER);
    }

    public void testGetCoarseLocationUpdates_withIntent() {
        doTestGetLocationUpdates_withIntent("my coarse provider name");
    }

    public void testGetCoarseLocationUpdates_withListener() {
        doTestGetLocationUpdates_withListener("my coarse provider name");
    }


    private void doTestGetLocationUpdates_withIntent(String providerName) {
        try {
            addTestProvider(providerName, Criteria.ACCURACY_COARSE, true, false, true);
            registerIntentReceiver();

            mManager.requestLocationUpdates(providerName, 0, 0, mPendingIntent);
            updateLocation(providerName, LAT, LNG);
            waitForReceiveBroadcast();

            assertNotNull(mIntentReceiver.getLastReceivedIntent());
            final Location location = mManager.getLastKnownLocation(providerName);
            assertEquals(providerName, location.getProvider());

            assertEquals(3000.0f, location.getAccuracy());
            assertEquals(LAT, location.getLatitude(), FUDGER_DELTA);
            assertEquals(LNG, location.getLongitude(), FUDGER_DELTA);

            mManager.removeUpdates(mPendingIntent);
        } finally {
            removeTestProvider(providerName);
        }
    }

    private void doTestGetLocationUpdates_withListener(String providerName) {
        try {
            addTestProvider(providerName, Criteria.ACCURACY_COARSE, true, false, true);

            MockLocationListener listener = new MockLocationListener();
            HandlerThread handlerThread = new HandlerThread("testLocationUpdates for "
                    + providerName);
            handlerThread.start();

            mManager.requestLocationUpdates(
                    providerName, 0, 0, listener, handlerThread.getLooper());
            updateLocation(providerName, LAT, LNG);

            assertTrue(listener.hasCalledOnLocationChanged(TEST_TIME_OUT_MS));
            Location location = listener.getLocation();
            assertEquals(providerName, location.getProvider());

            assertEquals(3000.0f, location.getAccuracy());
            assertEquals(LAT, location.getLatitude(), FUDGER_DELTA);
            assertEquals(LNG, location.getLongitude(), FUDGER_DELTA);

            mManager.removeUpdates(listener);
        } finally {
            removeTestProvider(providerName);
        }
    }

    /**
     * Helper method to add a test provider with given name.
     */
    private void addTestProvider(final String providerName, int accuracy, boolean requiresNetwork,
            boolean requiresSatellite, boolean requiresCell) {
        mManager.addTestProvider(providerName,
                requiresNetwork,
                requiresSatellite,
                requiresCell,
                false, // hasMonetaryCost,
                false, // supportsAltitude,
                false, // supportsSpeed,
                false, // supportsBearing,
                Criteria.POWER_MEDIUM, // powerRequirement
                accuracy); // accuracy
        mManager.setTestProviderEnabled(providerName, true);
    }

    public void testGetProviders() {
        List<String> providers = mManager.getProviders(false);

        assertFalse(hasProvider(providers, LocationManager.PASSIVE_PROVIDER));
        assertFalse(hasProvider(providers, LocationManager.GPS_PROVIDER));
    }

    private boolean hasProvider(List<String> providers, String providerName) {
        for (String provider : providers) {
            if (provider != null && provider.equals(providerName)) {
                return true;
            }
        }
        return false;
    }

    public void testGpsStatusListener() {
        try {
            mManager.addGpsStatusListener(new MockGpsStatusListener());
            fail("Should have failed to add a gps status listener");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mManager.addGpsStatusListener(null);
            fail("Should have failed to add a gps status listener");
        } catch (SecurityException e) {
            // expected
        }
    }

    public void testSendExtraCommand() {
        addTestProvider(LocationManager.NETWORK_PROVIDER, Criteria.ACCURACY_COARSE, true, false, true);
        addTestProvider(LocationManager.GPS_PROVIDER, Criteria.ACCURACY_FINE, false, true, false);

        // Unknown command
        assertFalse(mManager.sendExtraCommand(LocationManager.NETWORK_PROVIDER, "unknown", new Bundle()));

        try {
            mManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "unknown", new Bundle());
            fail("Should have failed to send a command to the gps provider");
        } catch (SecurityException expected) {
        } finally {
            removeTestProvider(LocationManager.GPS_PROVIDER);
            removeTestProvider(LocationManager.NETWORK_PROVIDER);
        }
    }

    private void registerIntentReceiver() {
        String intentKey = "LocationManagerTest";
        Intent proximityIntent = new Intent(intentKey);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, proximityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        mIntentReceiver = new TestIntentReceiver(intentKey);
        mContext.registerReceiver(mIntentReceiver, mIntentReceiver.getFilter());
    }

    /**
     * Blocks until receive intent notification or time out.
     */
    private void waitForReceiveBroadcast() {
        synchronized (mIntentReceiver) {
            try {
                mIntentReceiver.wait(TEST_TIME_OUT_MS);
            } catch (InterruptedException e) {
                fail("Interrupted while waiting for intent: " + e);
            }
        }
    }

    private void updateLocation(final String providerName, final double latitude,
            final double longitude) {
        Location nlocation = new Location(providerName);
        nlocation.setLatitude(latitude);
        nlocation.setLongitude(longitude);
        nlocation.setAccuracy(3000.0f);
        nlocation.setTime(java.lang.System.currentTimeMillis());
        nlocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        Location location = new Location(providerName);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(1.0f);
        location.setTime(java.lang.System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        location.setExtraLocation(Location.EXTRA_NO_GPS_LOCATION, nlocation);

        mManager.setTestProviderLocation(providerName, location);
    }

    /**
     * Helper class that receives a proximity intent and notifies the main class
     * when received
     */
    private static class TestIntentReceiver extends BroadcastReceiver {
        private String mExpectedAction;

        private Intent mLastReceivedIntent;

        public TestIntentReceiver(String expectedAction) {
            mExpectedAction = expectedAction;
            mLastReceivedIntent = null;
        }

        public IntentFilter getFilter() {
            IntentFilter filter = new IntentFilter(mExpectedAction);
            return filter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && mExpectedAction.equals(intent.getAction())) {
                synchronized (this) {
                    mLastReceivedIntent = intent;
                    notify();
                }
            }
        }

        public Intent getLastReceivedIntent() {
            return mLastReceivedIntent;
        }

        public void clearReceivedIntents() {
            mLastReceivedIntent = null;
        }
    }

    private static class MockLocationListener implements LocationListener {
        private String mProvider;
        private int mStatus;
        private Location mLocation;
        private Object mStatusLock = new Object();
        private Object mLocationLock = new Object();
        private Object mLocationRequestLock = new Object();

        private boolean mHasCalledOnLocationChanged;

        private boolean mHasCalledOnProviderDisabled;

        private boolean mHasCalledOnProviderEnabled;

        private boolean mHasCalledOnStatusChanged;

        private boolean mHasCalledRequestLocation;

        public void reset(){
            mHasCalledOnLocationChanged = false;
            mHasCalledOnProviderDisabled = false;
            mHasCalledOnProviderEnabled = false;
            mHasCalledOnStatusChanged = false;
            mHasCalledRequestLocation = false;
            mProvider = null;
            mStatus = 0;
        }

        /**
         * Call to inform listener that location has been updates have been requested
         */
        public void setLocationRequested() {
            synchronized (mLocationRequestLock) {
                mHasCalledRequestLocation = true;
                mLocationRequestLock.notify();
            }
        }

        public boolean hasCalledLocationRequested(long timeout) throws InterruptedException {
            synchronized (mLocationRequestLock) {
                if (timeout > 0 && !mHasCalledRequestLocation) {
                    mLocationRequestLock.wait(timeout);
                }
            }
            return mHasCalledRequestLocation;
        }

        /**
         * Check whether onLocationChanged() has been called. Wait up to timeout milliseconds
         * for the callback.
         * @param timeout Maximum time to wait for the callback, 0 to return immediately.
         */
        public boolean hasCalledOnLocationChanged(long timeout) {
            synchronized (mLocationLock) {
                if (timeout > 0 && !mHasCalledOnLocationChanged) {
                    try {
                        mLocationLock.wait(timeout);
                    } catch (InterruptedException e) {
                        fail("Interrupted while waiting for location change: " + e);
                    }
                }
            }
            return mHasCalledOnLocationChanged;
        }

        public boolean hasCalledOnProviderDisabled() {
            return mHasCalledOnProviderDisabled;
        }

        public boolean hasCalledOnProviderEnabled() {
            return mHasCalledOnProviderEnabled;
        }

        public boolean hasCalledOnStatusChanged(long timeout) throws InterruptedException {
            synchronized(mStatusLock) {
                // wait(0) would wait forever
                if (timeout > 0 && !mHasCalledOnStatusChanged) {
                    mStatusLock.wait(timeout);
                }
            }
            return mHasCalledOnStatusChanged;
        }

        public void onLocationChanged(Location location) {
            mLocation = location;
            synchronized (mLocationLock) {
                mHasCalledOnLocationChanged = true;
                mLocationLock.notify();
            }
        }

        public void onProviderDisabled(String provider) {
            mHasCalledOnProviderDisabled = true;
        }

        public void onProviderEnabled(String provider) {
            mHasCalledOnProviderEnabled = true;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            mProvider = provider;
            mStatus = status;
            synchronized (mStatusLock) {
                mHasCalledOnStatusChanged = true;
                mStatusLock.notify();
            }
        }

        public String getProvider() {
            return mProvider;
        }

        public int getStatus() {
            return mStatus;
        }

        public Location getLocation() {
            return mLocation;
        }
    }

    private static class MockGpsStatusListener implements Listener {
        private boolean mHasCallOnGpsStatusChanged;

        public boolean hasCallOnGpsStatusChanged() {
            return mHasCallOnGpsStatusChanged;
        }

        public void reset(){
            mHasCallOnGpsStatusChanged = false;
        }

        public void onGpsStatusChanged(int event) {
            mHasCallOnGpsStatusChanged = true;
        }
    }
}

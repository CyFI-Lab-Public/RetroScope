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

package android.location.cts;


import android.content.Context;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.test.AndroidTestCase;

public class LocationProviderTest extends AndroidTestCase {
    private static final String PROVIDER_NAME = "location_provider_test";

    private LocationManager mLocationManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mLocationManager =
            (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        addTestProvider(PROVIDER_NAME);
    }

    @Override
    protected void tearDown() throws Exception {
        // Work around b/11446702 by clearing the test provider before removing it
        mLocationManager.clearTestProviderLocation(PROVIDER_NAME);
        mLocationManager.removeTestProvider(PROVIDER_NAME);
        super.tearDown();
    }

    /**
     * Adds a test provider with the given name.
     */
    private void addTestProvider(String providerName) {
        mLocationManager.addTestProvider(
                providerName,
                true,  // requiresNetwork,
                false, // requiresSatellite,
                false, // requiresCell,
                false, // hasMonetaryCost,
                true,  // supportsAltitude,
                false, // supportsSpeed,
                true,  // supportsBearing,
                Criteria.POWER_MEDIUM,   // powerRequirement,
                Criteria.ACCURACY_FINE); // accuracy
        mLocationManager.setTestProviderEnabled(providerName, true);
    }

    public void testGetName() {
        LocationProvider locationProvider = mLocationManager.getProvider(PROVIDER_NAME);
        assertEquals(PROVIDER_NAME, locationProvider.getName());
    }

    public void testMeetsCriteria() {
        LocationProvider locationProvider = mLocationManager.getProvider(PROVIDER_NAME);

        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(true);
        assertTrue(locationProvider.meetsCriteria(criteria));
    }
}

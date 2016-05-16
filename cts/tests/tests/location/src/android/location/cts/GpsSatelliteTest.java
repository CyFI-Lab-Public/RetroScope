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
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.test.AndroidTestCase;

import java.util.Iterator;

public class GpsSatelliteTest extends AndroidTestCase {
    private GpsSatellite mGpsSatellite;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LocationManager lm =
            (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        GpsStatus gpsStatus = lm.getGpsStatus(null);

        Iterator<GpsSatellite> iterator = gpsStatus.getSatellites().iterator();
        if (iterator.hasNext()) {
            mGpsSatellite = iterator.next();
        }
    }

    public void testGetAzimuth() {
        if (mGpsSatellite != null) {
            assertTrue(mGpsSatellite.getAzimuth() >= 0 && mGpsSatellite.getAzimuth() <= 360);
        }
    }

    public void testGetElevation() {
        if (mGpsSatellite != null) {
            assertTrue(mGpsSatellite.getElevation() >= 0 && mGpsSatellite.getElevation() <= 90);
        }
    }

    public void testGetPrn() {
        if (mGpsSatellite != null) {
            // make sure there is no exception.
            mGpsSatellite.getPrn();
        }
    }

    public void testGetSnr() {
        if (mGpsSatellite != null) {
            // make sure there is no exception.
            mGpsSatellite.getSnr();
        }
    }

    public void testHasAlmanac() {
        if (mGpsSatellite != null) {
            // make sure there is no exception.
            mGpsSatellite.hasAlmanac();
        }
    }

    public void testHasEphemeris() {
        if (mGpsSatellite != null) {
            // make sure there is no exception.
            mGpsSatellite.hasEphemeris();
        }
    }

    public void testUsedInFix() {
        if (mGpsSatellite != null) {
            // make sure there is no exception.
            mGpsSatellite.usedInFix();
        }
    }
}

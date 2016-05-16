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

public class GpsStatusTest extends AndroidTestCase {
    private GpsStatus mGpsStatus;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LocationManager lm =
            (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        mGpsStatus = lm.getGpsStatus(null);
    }

    public void testGetSatellites() {
        Iterable<GpsSatellite> satellites = mGpsStatus.getSatellites();
        assertNotNull(satellites);

        final int maxSatellites = mGpsStatus.getMaxSatellites();
        assertTrue(maxSatellites > 0);

        Iterator<GpsSatellite> iterator = satellites.iterator();
        // get the total of satellites
        int count = 0;
        while (iterator.hasNext()) {
            count++;
        }
        // the real total could not be larger than maxSatellites
        assertTrue(count <= maxSatellites);
    }

    public void testGetTimeToFirstFix() {
        // make sure there is no exception.
        mGpsStatus.getTimeToFirstFix();
    }
}

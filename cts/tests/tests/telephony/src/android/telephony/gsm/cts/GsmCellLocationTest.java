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

package android.telephony.gsm.cts;

import android.os.Bundle;
import android.telephony.gsm.GsmCellLocation;
import android.test.AndroidTestCase;

public class GsmCellLocationTest extends AndroidTestCase {

    private static final int CID_VALUE = 20;
    private static final int LAC_VALUE = 10;
    private static final int INVALID_CID = -1;
    private static final int INVALID_LAC = -1;

    public void testGsmCellLocation() {
        Bundle bundle = new Bundle();

        GsmCellLocation gsmCellLocation = new GsmCellLocation();
        checkLacAndCid(INVALID_LAC, INVALID_CID, gsmCellLocation);

        gsmCellLocation.setLacAndCid(LAC_VALUE, CID_VALUE);
        gsmCellLocation.fillInNotifierBundle(bundle);
        gsmCellLocation = new GsmCellLocation(bundle);
        checkLacAndCid(LAC_VALUE, CID_VALUE, gsmCellLocation);

        gsmCellLocation.setStateInvalid();
        checkLacAndCid(INVALID_LAC, INVALID_CID, gsmCellLocation);

        gsmCellLocation.setLacAndCid(LAC_VALUE, CID_VALUE);
        checkLacAndCid(LAC_VALUE, CID_VALUE, gsmCellLocation);

        assertEquals(LAC_VALUE ^ CID_VALUE, gsmCellLocation.hashCode());
        assertNotNull(gsmCellLocation.toString());

        GsmCellLocation testGCSEquals = new GsmCellLocation();
        assertFalse(gsmCellLocation.equals(testGCSEquals));
        testGCSEquals.setLacAndCid(LAC_VALUE, CID_VALUE);
        assertTrue(gsmCellLocation.equals(testGCSEquals));
    }

    private void checkLacAndCid(int expectedLac, int expectedCid, GsmCellLocation gsmCellLocation) {
        assertEquals(expectedLac, gsmCellLocation.getLac());
        assertEquals(expectedCid, gsmCellLocation.getCid());
    }
}

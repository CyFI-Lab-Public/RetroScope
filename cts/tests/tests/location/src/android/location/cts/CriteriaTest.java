/*
 * Copyright (C) 2009 The Android Open Source Project
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


import android.location.Criteria;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class CriteriaTest extends AndroidTestCase {
    public void testConstructor() {
        new Criteria();

        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        c.setAltitudeRequired(true);
        c.setBearingRequired(true);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);
        c.setSpeedRequired(true);
        Criteria criteria = new Criteria(c);
        assertEquals(Criteria.ACCURACY_FINE, criteria.getAccuracy());
        assertTrue(criteria.isAltitudeRequired());
        assertTrue(criteria.isBearingRequired());
        assertTrue(criteria.isCostAllowed());
        assertTrue(criteria.isSpeedRequired());
        assertEquals(Criteria.POWER_HIGH, criteria.getPowerRequirement());

        try {
            new Criteria(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected.
        }
    }

    public void testDescribeContents() {
        Criteria criteria = new Criteria();
        criteria.describeContents();
    }

    public void testAccessAccuracy() {
        Criteria criteria = new Criteria();

        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        assertEquals(Criteria.ACCURACY_FINE, criteria.getAccuracy());

        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        assertEquals(Criteria.ACCURACY_COARSE, criteria.getAccuracy());

        try {
            // It should throw IllegalArgumentException
            criteria.setAccuracy(-1);
            // issue 1728526
        } catch (IllegalArgumentException e) {
            // expected.
        }

        try {
            // It should throw IllegalArgumentException
            criteria.setAccuracy(Criteria.ACCURACY_COARSE + 1);
            // issue 1728526
        } catch (IllegalArgumentException e) {
            // expected.
        }
    }

    public void testAccessPowerRequirement() {
        Criteria criteria = new Criteria();

        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        assertEquals(Criteria.NO_REQUIREMENT, criteria.getPowerRequirement());

        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        assertEquals(Criteria.POWER_MEDIUM, criteria.getPowerRequirement());

        try {
            criteria.setPowerRequirement(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected.
        }

        try {
            criteria.setPowerRequirement(Criteria.POWER_HIGH + 1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected.
        }
    }

    public void testAccessAltitudeRequired() {
        Criteria criteria = new Criteria();

        criteria.setAltitudeRequired(false);
        assertFalse(criteria.isAltitudeRequired());

        criteria.setAltitudeRequired(true);
        assertTrue(criteria.isAltitudeRequired());
    }

    public void testAccessBearingRequired() {
        Criteria criteria = new Criteria();

        criteria.setBearingRequired(false);
        assertFalse(criteria.isBearingRequired());

        criteria.setBearingRequired(true);
        assertTrue(criteria.isBearingRequired());
    }

    public void testAccessCostAllowed() {
        Criteria criteria = new Criteria();

        criteria.setCostAllowed(false);
        assertFalse(criteria.isCostAllowed());

        criteria.setCostAllowed(true);
        assertTrue(criteria.isCostAllowed());
    }

    public void testAccessSpeedRequired() {
        Criteria criteria = new Criteria();

        criteria.setSpeedRequired(false);
        assertFalse(criteria.isSpeedRequired());

        criteria.setSpeedRequired(true);
        assertTrue(criteria.isSpeedRequired());
    }

    public void testWriteToParcel() {
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setSpeedRequired(true);

        Parcel parcel = Parcel.obtain();
        criteria.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Criteria newCriteria = Criteria.CREATOR.createFromParcel(parcel);

        assertEquals(criteria.getAccuracy(), newCriteria.getAccuracy());
        assertEquals(criteria.getPowerRequirement(), newCriteria.getPowerRequirement());
        assertEquals(criteria.isAltitudeRequired(), newCriteria.isAltitudeRequired());
        assertEquals(criteria.isBearingRequired(), newCriteria.isBearingRequired());
        assertEquals(criteria.isSpeedRequired(), newCriteria.isSpeedRequired());
        assertEquals(criteria.isCostAllowed(), newCriteria.isCostAllowed());
    }
}

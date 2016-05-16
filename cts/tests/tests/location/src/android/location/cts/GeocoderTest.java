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


import android.location.Geocoder;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.util.Locale;

public class GeocoderTest extends AndroidTestCase {

    private static final int MAX_NUM_RETRIES = 5;
    private static final int TIME_BETWEEN_RETRIES_MS = 10 * 1000;

    public void testConstructor() {
        new Geocoder(getContext());

        new Geocoder(getContext(), Locale.ENGLISH);

        try {
            new Geocoder(getContext(), null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected.
        }
    }

    public void testGetFromLocation() throws IOException, InterruptedException {
        Geocoder geocoder = new Geocoder(getContext());

        // There is no guarantee that geocoder.getFromLocation returns accurate results
        // Thus only test that calling the method with valid arguments doesn't produce
        // an unexpected exception
        // Note: there is a risk this test will fail if device under test does not have
        // a network connection. This is why we try the geocode 5 times if it fails due
        // to a network error.
        int numRetries = 0;
        while (numRetries < MAX_NUM_RETRIES) {
            try {
                geocoder.getFromLocation(60, 30, 5);
                break;
            } catch (IOException e) {
                Thread.sleep(TIME_BETWEEN_RETRIES_MS);
                numRetries++;
            }
        }
        if (numRetries >= MAX_NUM_RETRIES) {
            fail("Failed to geocode location " + MAX_NUM_RETRIES + " times.");
        }


        try {
            // latitude is less than -90
            geocoder.getFromLocation(-91, 30, 5);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            // latitude is greater than 90
            geocoder.getFromLocation(91, 30, 5);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            // longitude is less than -180
            geocoder.getFromLocation(10, -181, 5);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            // longitude is greater than 180
            geocoder.getFromLocation(10, 181, 5);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetFromLocationName() throws IOException, InterruptedException {
        Geocoder geocoder = new Geocoder(getContext(), Locale.US);

        // There is no guarantee that geocoder.getFromLocationName returns accurate results.
        // Thus only test that calling the method with valid arguments doesn't produce
        // an unexpected exception
        // Note: there is a risk this test will fail if device under test does not have
        // a network connection. This is why we try the geocode 5 times if it fails due
        // to a network error.
        int numRetries = 0;
        while (numRetries < MAX_NUM_RETRIES) {
            try {
                geocoder.getFromLocationName("Dalvik,Iceland", 5);
                break;
            } catch (IOException e) {
                Thread.sleep(TIME_BETWEEN_RETRIES_MS);
                numRetries++;
            }
        }
        if (numRetries >= MAX_NUM_RETRIES) {
            fail("Failed to geocode location name " + MAX_NUM_RETRIES + " times.");
        }

        try {
            geocoder.getFromLocationName(null, 5);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            geocoder.getFromLocationName("Beijing", 5, -91, 100, 45, 130);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            geocoder.getFromLocationName("Beijing", 5, 25, 190, 45, 130);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            geocoder.getFromLocationName("Beijing", 5, 25, 100, 91, 130);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            geocoder.getFromLocationName("Beijing", 5, 25, 100, 45, -181);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}

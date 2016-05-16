/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.permission.cts;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Test the non-location-related functionality of TelephonyManager.
 */
public class TelephonyManagerPermissionTest extends AndroidTestCase {

    TelephonyManager mTelephonyManager = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        assertNotNull(mTelephonyManager);
    }

    /**
     * Verify that TelephonyManager.getDeviceId requires Permission.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE}.
     */
    @SmallTest
    public void testGetDeviceId() {
        try {
            String id = mTelephonyManager.getDeviceId();
            fail("Got device ID: " + id);
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that TelephonyManager.getLine1Number requires Permission.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE}.
     */
    @SmallTest
    public void testGetLine1Number() {
        try {
            String nmbr = mTelephonyManager.getLine1Number();
            fail("Got line 1 number: " + nmbr);
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that TelephonyManager.getSimSerialNumber requires Permission.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE}.
     */
    @SmallTest
    public void testGetSimSerialNumber() {
        try {
            String nmbr = mTelephonyManager.getSimSerialNumber();
            fail("Got SIM serial number: " + nmbr);
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that TelephonyManager.getSubscriberId requires Permission.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE}.
     */
    @SmallTest
    public void testGetSubscriberId() {
        try {
            String sid = mTelephonyManager.getSubscriberId();
            fail("Got subscriber id: " + sid);
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that TelephonyManager.getVoiceMailNumber requires Permission.
     * <p>
     * Requires Permission:
     * {@link android.Manifest.permission#READ_PHONE_STATE}.
     */
    @SmallTest
    public void testVoiceMailNumber() {
        try {
            String vmnum = mTelephonyManager.getVoiceMailNumber();
            fail("Got voicemail number: " + vmnum);
        } catch (SecurityException e) {
            // expected
        }
    }
}

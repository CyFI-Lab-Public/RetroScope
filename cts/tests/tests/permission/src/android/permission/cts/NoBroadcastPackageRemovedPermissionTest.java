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

package android.permission.cts;

import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify Context related methods without specific BROADCAST series permissions.
 */
public class NoBroadcastPackageRemovedPermissionTest extends AndroidTestCase {
    private static final String TEST_RECEIVER_PERMISSION = "receiverPermission";

    /**
     * Verify that Context#sendStickyBroadcast(Intent),
     * Context#removeStickyBroadcast(Intent)
     * requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#BROADCAST_STICKY }.
     */
    @SmallTest
    public void testSendOrRemoveStickyBroadcast() {
        try {
            mContext.sendStickyBroadcast(createIntent(Intent.ACTION_WALLPAPER_CHANGED));
            fail("Context.sendStickyBroadcast did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mContext.removeStickyBroadcast(createIntent(Intent.ACTION_WALLPAPER_CHANGED));
            fail("Context.removeStickyBroadcast did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that Context#sendBroadcast(Intent),
     * Context#sendBroadcast(Intent, String)
     * Context#sendOrderedBroadcast(Intent, String, BroadcastReceiver,
     *                              Handler, int, String, Bundle)
     * Context#sendOrderedBroadcast(Intent, String) with ACTION_UID_REMOVED
     * with ACTION_PACKAGE_REMOVED requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#BROADCAST_PACKAGE_REMOVED}.
     */
    @SmallTest
    public void testSendBroadcast() {
        try {
            mContext.sendBroadcast(createIntent(Intent.ACTION_PACKAGE_REMOVED));
            fail("Context.sendBroadcast did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mContext.sendBroadcast(createIntent(Intent.ACTION_PACKAGE_REMOVED),
                    TEST_RECEIVER_PERMISSION);
            fail("Context.sendBroadcast did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mContext.sendOrderedBroadcast(createIntent(Intent.ACTION_PACKAGE_REMOVED),
                    TEST_RECEIVER_PERMISSION, null, null, 0, "initialData", Bundle.EMPTY);
            fail("Context.sendOrderedBroadcast did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mContext.sendOrderedBroadcast(createIntent(Intent.ACTION_PACKAGE_REMOVED),
                    TEST_RECEIVER_PERMISSION);
            fail("Context.sendOrderedBroadcast did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    private Intent createIntent(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        return intent;
    }
}

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

import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify that rebooting requires Permission.
 */
public class RebootPermissionTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Verify that rebooting by sending a broadcast Intent requires Permission.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#REBOOT}.
     */
    @SmallTest
    public void testBroadcastReboot() {
        try {
            mContext.sendBroadcast(new Intent(Intent.ACTION_REBOOT));
        } catch (SecurityException e) {
            // expected
        }
    }

}

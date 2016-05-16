/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.pm.PackageManager;
import android.os.Process;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify permission behaviors with android:maxSdkVersion
 */
public class PermissionMaxSdkVersionTest extends AndroidTestCase {
    // These two permission names must match the corresponding <uses-permission>
    // declarations in the test app manifest.
    static final String UNGRANTABLE_PERMISSION = "android.permission.VIBRATE";
    static final String GRANTABLE_PERMISSION = "android.permission.FLASHLIGHT";

    /**
     * Verify that with android:maxSdkVersion set to a previous API level,
     * the permission is not being granted.
     */
    @SmallTest
    public void testMaxSdkInPast() {
        int result = mContext.checkPermission(UNGRANTABLE_PERMISSION,
                Process.myPid(), Process.myUid());
        assertEquals("Permissions with maxSdkVersion in the past should not be granted",
                result,
                PackageManager.PERMISSION_DENIED);
    }

    /**
     * Verify that with android:maxSdkVersion set to a future API level,
     * the permission is being granted.
     */
    @SmallTest
    public void testMaxSdkInFuture() {
        int result = mContext.checkPermission(GRANTABLE_PERMISSION,
                Process.myPid(), Process.myUid());
        assertEquals("Permissions with maxSdkVersion in the future should be granted",
                result,
                PackageManager.PERMISSION_GRANTED);
    }
}

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

package android.content.pm.cts;


import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class PermissionInfoTest extends AndroidTestCase {
    private static final String PERMISSION_NAME = "android.permission.INTERNET";
    private static final String DEFAULT_DISCPRIPTION = "Allows the app to create network sockets "
            + "and use custom network protocols. The browser and other applications provide means "
            + "to send data to the internet, so this permission is not required to send data to "
            + "the internet.";

    public void testPermissionInfo() throws NameNotFoundException {
        PackageManager pm = getContext().getPackageManager();
        Parcel p = Parcel.obtain();
        // Test constructors
        new PermissionInfo();
        PermissionInfo permissionInfo = pm.getPermissionInfo(PERMISSION_NAME, 0);
        PermissionInfo infoFromExisted = new PermissionInfo(permissionInfo);
        checkInfoSame(permissionInfo, infoFromExisted);

        // Test toString, describeContents, loadDescription
        assertNotNull(permissionInfo.toString());
        assertEquals(0, permissionInfo.describeContents());
        assertEquals(DEFAULT_DISCPRIPTION, permissionInfo.loadDescription(pm));

        permissionInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        PermissionInfo infoFromParcel = PermissionInfo.CREATOR.createFromParcel(p);
        checkInfoSame(permissionInfo, infoFromParcel);
        p.recycle();
    }

    private void checkInfoSame(PermissionInfo expected, PermissionInfo actual) {
        assertEquals(expected.name, actual.name);
        assertEquals(expected.group, actual.group);
        assertEquals(expected.descriptionRes, actual.descriptionRes);
        assertEquals(expected.protectionLevel, actual.protectionLevel);
        assertEquals(expected.nonLocalizedDescription, actual.nonLocalizedDescription);
    }
}

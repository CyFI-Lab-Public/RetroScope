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


import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class ServiceInfoTest extends AndroidTestCase {
    private static final String PACKAGE_NAME = "com.android.cts.stub";
    private static final String SERVICE_NAME = "android.content.pm.cts.TestPmService";

    public void testServiceInfo() throws NameNotFoundException {
        PackageManager pm = getContext().getPackageManager();
        ComponentName componentName = new ComponentName(PACKAGE_NAME, SERVICE_NAME);
        Parcel p = Parcel.obtain();

        // Test ServiceInfo()
        new ServiceInfo();

        ServiceInfo serviceInfo = pm.getServiceInfo(componentName, 0);
        // Test ServiceInfo(ServiceInfo orig)
        ServiceInfo infoFromExisted = new ServiceInfo(serviceInfo);
        checkInfoSame(serviceInfo, infoFromExisted);
        // Test toString, describeContents
        assertNotNull(serviceInfo.toString());
        assertEquals(0, serviceInfo.describeContents());

        // Test writeToParcel
        serviceInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        ServiceInfo infoFromParcel = ServiceInfo.CREATOR.createFromParcel(p);
        checkInfoSame(serviceInfo, infoFromParcel);
    }

    private void checkInfoSame(ServiceInfo expected, ServiceInfo actual) {
        assertEquals(expected.name, actual.name);
        assertEquals(expected.permission, actual.permission);
    }
}

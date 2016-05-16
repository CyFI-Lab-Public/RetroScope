/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.admin.cts;

import android.app.admin.DeviceAdminInfo;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.test.AndroidTestCase;

public class DeviceAdminInfoTest extends AndroidTestCase {

    private PackageManager mPackageManager;

    private ComponentName mComponent;

    private ComponentName mSecondComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageManager = mContext.getPackageManager();
        mComponent = getReceiverComponent();
        mSecondComponent = getSecondReceiverComponent();
    }

    static ComponentName getReceiverComponent() {
        return new ComponentName("android.deviceadmin.cts",
                "android.deviceadmin.cts.CtsDeviceAdminReceiver");
    }

    static ComponentName getSecondReceiverComponent() {
        return new ComponentName("android.deviceadmin.cts",
                "android.deviceadmin.cts.CtsDeviceAdminReceiver2");
    }

    public void testDeviceAdminInfo() throws Exception {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = mPackageManager.getReceiverInfo(mComponent,
                PackageManager.GET_META_DATA);

        DeviceAdminInfo info = new DeviceAdminInfo(mContext, resolveInfo);
        assertEquals(mComponent, info.getComponent());
        assertEquals(mComponent.getPackageName(), info.getPackageName());
        assertEquals(mComponent.getClassName(), info.getReceiverName());

        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_FORCE_LOCK));
        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_LIMIT_PASSWORD));
        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_RESET_PASSWORD));
        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_WATCH_LOGIN));
        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_WIPE_DATA));

        assertEquals("force-lock",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_FORCE_LOCK));
        assertEquals("limit-password",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_LIMIT_PASSWORD));
        assertEquals("reset-password",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_RESET_PASSWORD));
        assertEquals("watch-login",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_WATCH_LOGIN));
        assertEquals("wipe-data",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_WIPE_DATA));
    }

    public void testDeviceAdminInfo2() throws Exception {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = mPackageManager.getReceiverInfo(mSecondComponent,
                PackageManager.GET_META_DATA);

        DeviceAdminInfo info = new DeviceAdminInfo(mContext, resolveInfo);
        assertEquals(mSecondComponent, info.getComponent());
        assertEquals(mSecondComponent.getPackageName(), info.getPackageName());
        assertEquals(mSecondComponent.getClassName(), info.getReceiverName());

        assertFalse(info.usesPolicy(DeviceAdminInfo.USES_POLICY_FORCE_LOCK));
        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_LIMIT_PASSWORD));
        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_RESET_PASSWORD));
        assertFalse(info.usesPolicy(DeviceAdminInfo.USES_POLICY_WATCH_LOGIN));
        assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_WIPE_DATA));

        assertEquals("force-lock",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_FORCE_LOCK));
        assertEquals("limit-password",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_LIMIT_PASSWORD));
        assertEquals("reset-password",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_RESET_PASSWORD));
        assertEquals("watch-login",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_WATCH_LOGIN));
        assertEquals("wipe-data",
                info.getTagForPolicy(DeviceAdminInfo.USES_POLICY_WIPE_DATA));
    }
}

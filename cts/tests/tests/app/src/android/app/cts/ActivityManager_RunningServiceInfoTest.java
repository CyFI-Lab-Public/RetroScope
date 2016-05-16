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
package android.app.cts;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class ActivityManager_RunningServiceInfoTest extends AndroidTestCase {
    private ActivityManager.RunningServiceInfo mRunningServiceInfo;
    private ComponentName mService;
    private static final String PROCESS = "process";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRunningServiceInfo = new ActivityManager.RunningServiceInfo();
        mService = new ComponentName(getContext(), MockActivity.class);

        mRunningServiceInfo.service = mService;
        mRunningServiceInfo.pid = 1;
        mRunningServiceInfo.process = PROCESS;
        mRunningServiceInfo.foreground = true;
        mRunningServiceInfo.activeSince = 1l;
        mRunningServiceInfo.started = true;
        mRunningServiceInfo.clientCount = 2;
        mRunningServiceInfo.crashCount = 1;
        mRunningServiceInfo.lastActivityTime = 1l;
        mRunningServiceInfo.restarting = 1l;
    }

    public void testConstructor() {
        new ActivityManager.RunningServiceInfo();
    }

    public void testDescribeContents() {
        assertEquals(0, mRunningServiceInfo.describeContents());
    }

    public void testWriteToParcel() throws Exception {

        Parcel parcel = Parcel.obtain();
        mRunningServiceInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.RunningServiceInfo values =
            ActivityManager.RunningServiceInfo.CREATOR.createFromParcel(parcel);
        assertEquals(mService, values.service);
        assertEquals(1, values.pid);
        assertEquals(PROCESS, values.process);
        assertTrue(values.foreground);
        assertEquals(1l, values.activeSince);
        assertTrue(values.started);
        assertEquals(2, values.clientCount);
        assertEquals(1, values.crashCount);
        assertEquals(1l, values.lastActivityTime);
        assertEquals(1l, values.restarting);
    }

    public void testReadFromParcel() throws Exception {

        Parcel parcel = Parcel.obtain();
        mRunningServiceInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.RunningServiceInfo values =
            new ActivityManager.RunningServiceInfo();
        values.readFromParcel(parcel);
        assertEquals(mService, values.service);
        assertEquals(1, values.pid);
        assertEquals(PROCESS, values.process);
        assertTrue(values.foreground);
        assertEquals(1l, values.activeSince);
        assertTrue(values.started);
        assertEquals(2, values.clientCount);
        assertEquals(1, values.crashCount);
        assertEquals(1l, values.lastActivityTime);
        assertEquals(1l, values.restarting);
    }

}

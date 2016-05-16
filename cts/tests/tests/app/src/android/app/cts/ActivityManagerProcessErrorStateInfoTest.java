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
import android.os.Parcel;
import android.test.AndroidTestCase;

public class ActivityManagerProcessErrorStateInfoTest extends AndroidTestCase {
    protected ActivityManager.ProcessErrorStateInfo mErrorStateInfo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mErrorStateInfo = new ActivityManager.ProcessErrorStateInfo();
    }

    public void testConstructor() {
        new ActivityManager.ProcessErrorStateInfo();
    }

    public void testDescribeContents() {
        assertEquals(0, mErrorStateInfo.describeContents());
    }

    public void testWriteToParcel() throws Exception {
        int condition = 1;
        String processName = "processName";
        int pid = 2;
        int uid = 3;
        String tag = "tag";
        String shortMsg = "shortMsg";
        String longMsg = "longMsg";

        mErrorStateInfo.condition = condition;
        mErrorStateInfo.processName = processName;
        mErrorStateInfo.pid = pid;
        mErrorStateInfo.uid = uid;
        mErrorStateInfo.tag = tag;
        mErrorStateInfo.shortMsg = shortMsg;
        mErrorStateInfo.longMsg = longMsg;

        Parcel parcel = Parcel.obtain();
        mErrorStateInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.ProcessErrorStateInfo values =
            ActivityManager.ProcessErrorStateInfo.CREATOR.createFromParcel(parcel);

        assertEquals(condition, values.condition);
        assertEquals(processName, values.processName);
        assertEquals(pid, values.pid);
        assertEquals(uid, values.uid);
        assertEquals(tag, values.tag);
        // null?
        assertEquals(shortMsg, values.shortMsg);
        assertEquals(longMsg, values.longMsg);
        assertNull(values.crashData);  // Deprecated field: always null
    }

    public void testReadFromParcel() throws Exception {
        int condition = 1;
        String processName = "processName";
        int pid = 2;
        int uid = 3;
        String tag = "tag";
        String shortMsg = "shortMsg";
        String longMsg = "longMsg";

        mErrorStateInfo.condition = condition;
        mErrorStateInfo.processName = processName;
        mErrorStateInfo.pid = pid;
        mErrorStateInfo.uid = uid;
        mErrorStateInfo.tag = tag;
        mErrorStateInfo.shortMsg = shortMsg;
        mErrorStateInfo.longMsg = longMsg;

        Parcel parcel = Parcel.obtain();
        mErrorStateInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.ProcessErrorStateInfo values = new ActivityManager.ProcessErrorStateInfo();
        values.readFromParcel(parcel);

        assertEquals(condition, values.condition);
        assertEquals(processName, values.processName);
        assertEquals(pid, values.pid);
        assertEquals(uid, values.uid);
        assertEquals(tag, values.tag);
        assertEquals(shortMsg, values.shortMsg);
        assertEquals(longMsg, values.longMsg);
        assertNull(values.crashData);  // Deprecated field: always null
    }

}

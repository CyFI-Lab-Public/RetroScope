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
import android.content.Intent;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class ActivityManagerRecentTaskInfoTest extends AndroidTestCase {
    protected ActivityManager.RecentTaskInfo mRecentTaskInfo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRecentTaskInfo = new ActivityManager.RecentTaskInfo();
    }

    public void testConstructor() {
        new ActivityManager.RecentTaskInfo();
    }

    public void testDescribeContents() {
        assertEquals(0, mRecentTaskInfo.describeContents());
    }

    public void testWriteToParcel() throws Exception {
        int id = 1;
        Intent baseIntent = null;
        ComponentName origActivity = null;
        mRecentTaskInfo.id = id;
        mRecentTaskInfo.baseIntent = baseIntent;
        mRecentTaskInfo.origActivity = origActivity;

        Parcel parcel = Parcel.obtain();
        mRecentTaskInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.RecentTaskInfo values = ActivityManager.RecentTaskInfo.CREATOR
                .createFromParcel(parcel);
        assertEquals(id, values.id);
        assertEquals(null, values.baseIntent);
        assertEquals(null, values.origActivity);
        // test baseIntent,origActivity is not null, and id is -1(not running)
        baseIntent = new Intent();
        baseIntent.setAction(Intent.ACTION_CALL);
        origActivity = new ComponentName(mContext, this.getClass());
        mRecentTaskInfo.id = -1;
        mRecentTaskInfo.baseIntent = baseIntent;
        mRecentTaskInfo.origActivity = origActivity;
        parcel = Parcel.obtain();
        mRecentTaskInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        values = ActivityManager.RecentTaskInfo.CREATOR
                .createFromParcel(parcel);
        assertEquals(-1, values.id);
        assertNotNull(values.baseIntent);
        assertEquals(Intent.ACTION_CALL, values.baseIntent.getAction());
        assertEquals(origActivity, values.origActivity);
    }

    public void testReadFromParcel() throws Exception {
        int id = 1;
        Intent baseIntent = null;
        ComponentName origActivity = null;
        mRecentTaskInfo.id = id;
        mRecentTaskInfo.baseIntent = baseIntent;
        mRecentTaskInfo.origActivity = origActivity;

        Parcel parcel = Parcel.obtain();
        mRecentTaskInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.RecentTaskInfo values = new ActivityManager.RecentTaskInfo();
        values.readFromParcel(parcel);
        assertEquals(id, values.id);
        assertEquals(null, values.baseIntent);
        assertEquals(null, values.origActivity);
        // test baseIntent,origActivity is not null, and id is -1(not running)
        baseIntent = new Intent();
        baseIntent.setAction(Intent.ACTION_CALL);
        origActivity = new ComponentName(mContext, this.getClass());
        mRecentTaskInfo.id = -1;
        mRecentTaskInfo.baseIntent = baseIntent;
        mRecentTaskInfo.origActivity = origActivity;
        parcel = Parcel.obtain();
        mRecentTaskInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        values.readFromParcel(parcel);
        assertEquals(-1, values.id);
        assertNotNull(values.baseIntent);
        assertEquals(Intent.ACTION_CALL, values.baseIntent.getAction());
        assertEquals(origActivity, values.origActivity);
    }

}

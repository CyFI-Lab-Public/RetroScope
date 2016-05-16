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

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Parcel;
import android.test.AndroidTestCase;


public class ActivityManager_RunningAppProcessInfoTest extends AndroidTestCase {

    public void testRunningAppProcessInfo() {
        // test constructor
        new RunningAppProcessInfo();
        new RunningAppProcessInfo("test", 100, new String[]{"com.android", "com.android.test"});

        final ActivityManager am = (ActivityManager)
                    getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningAppProcessInfo> list = am.getRunningAppProcesses();
        final RunningAppProcessInfo rap = list.get(0);

        // test describeContents function
        assertEquals(0, rap.describeContents());
        final Parcel p = Parcel.obtain();

        // test writeToParcel function
        rap.writeToParcel(p, 0);

        // test readFromParcel function
        final RunningAppProcessInfo r = new RunningAppProcessInfo();
        p.setDataPosition(0);
        r.readFromParcel(p);

        assertEquals(rap.pid, r.pid);
        assertEquals(rap.processName, r.processName);
        assertEquals(rap.pkgList.length, r.pkgList.length);

        for (int i = 0; i < rap.pkgList.length; i++) {
            assertEquals(rap.pkgList[i], r.pkgList[i]);
        }
    }

}

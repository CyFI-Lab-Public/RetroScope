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

import com.android.cts.stub.R;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.StringBuilderPrinter;

/**
 * Test {@link ApplicationInfo}.
 */
public class ApplicationInfoTest extends AndroidTestCase {
    private ApplicationInfo mApplicationInfo;
    private String mPackageName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageName = getContext().getPackageName();
    }

    public void testConstructor() {
        ApplicationInfo info = new ApplicationInfo();
        // simple test to ensure packageName is copied by copy constructor
        // TODO: consider expanding to check all member variables
        info.packageName = mPackageName;
        ApplicationInfo copy = new ApplicationInfo(info);
        assertEquals(info.packageName, copy.packageName);
    }

    public void testWriteToParcel() throws NameNotFoundException {
        mApplicationInfo = mContext.getPackageManager().getApplicationInfo(mPackageName, 0);

        Parcel p = Parcel.obtain();
        mApplicationInfo.writeToParcel(p, 0);

        p.setDataPosition(0);
        ApplicationInfo info = ApplicationInfo.CREATOR.createFromParcel(p);
        assertEquals(mApplicationInfo.taskAffinity, info.taskAffinity);
        assertEquals(mApplicationInfo.permission, info.permission);
        assertEquals(mApplicationInfo.processName, info.processName);
        assertEquals(mApplicationInfo.className, info.className);
        assertEquals(mApplicationInfo.theme, info.theme);
        assertEquals(mApplicationInfo.flags, info.flags);
        assertEquals(mApplicationInfo.sourceDir, info.sourceDir);
        assertEquals(mApplicationInfo.publicSourceDir, info.publicSourceDir);
        assertEquals(mApplicationInfo.sharedLibraryFiles, info.sharedLibraryFiles);
        assertEquals(mApplicationInfo.dataDir, info.dataDir);
        assertEquals(mApplicationInfo.uid, info.uid);
        assertEquals(mApplicationInfo.enabled, info.enabled);
        assertEquals(mApplicationInfo.manageSpaceActivityName, info.manageSpaceActivityName);
        assertEquals(mApplicationInfo.descriptionRes, info.descriptionRes);
    }

    public void testToString() {
        mApplicationInfo = new ApplicationInfo();
        assertNotNull(mApplicationInfo.toString());
    }

    public void testDescribeContents() throws NameNotFoundException {
       mApplicationInfo = mContext.getPackageManager().getApplicationInfo(mPackageName, 0);

        assertEquals(0, mApplicationInfo.describeContents());
    }

    public void testDump() {
        mApplicationInfo = new ApplicationInfo();

        StringBuilder sb = new StringBuilder();
        assertEquals(0, sb.length());
        StringBuilderPrinter p = new StringBuilderPrinter(sb);

        String prefix = "";
        mApplicationInfo.dump(p, prefix);
        assertNotNull(sb.toString());
        assertTrue(sb.length() > 0);
    }

    public void testLoadDescription() throws NameNotFoundException {
        mApplicationInfo = mContext.getPackageManager().getApplicationInfo(mPackageName, 0);

        assertNull(mApplicationInfo.loadDescription(mContext.getPackageManager()));

        mApplicationInfo.descriptionRes = R.string.hello_world;
        assertEquals(mContext.getResources().getString(R.string.hello_world),
                mApplicationInfo.loadDescription(mContext.getPackageManager()));
    }
}

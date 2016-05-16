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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo.DisplayNameComparator;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.AndroidTestCase;

/**
 * Test {@link DisplayNameComparator}.
 */
public class ApplicationInfo_DisplayNameComparatorTest extends AndroidTestCase {
    private static final String PACKAGE_NAME = "com.android.cts.stub";
    DisplayNameComparator mDisplayNameComparator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDisplayNameComparator = null;
    }

    public void testConstructor() {
        PackageManager pm = getContext().getPackageManager();
        new DisplayNameComparator(pm);

        new DisplayNameComparator(null);
    }

    public void testCompare() throws NameNotFoundException {
        PackageManager pm = getContext().getPackageManager();
        mDisplayNameComparator = new ApplicationInfo.DisplayNameComparator(pm);

        ApplicationInfo info1 = new ApplicationInfo();
        ApplicationInfo info2 = new ApplicationInfo();
        info1.packageName = PACKAGE_NAME;
        info2.packageName = PACKAGE_NAME;
        assertEquals(0, mDisplayNameComparator.compare(info1, info2));

        info1 = mContext.getPackageManager().getApplicationInfo(PACKAGE_NAME, 0);
        info2.packageName = PACKAGE_NAME + ".2";
        assertTrue((mDisplayNameComparator.compare(info1, info2) < 0));

        info1 = new ApplicationInfo();
        info1.packageName = PACKAGE_NAME + ".1";
        info2 = mContext.getPackageManager().getApplicationInfo(PACKAGE_NAME, 0);
        assertTrue((mDisplayNameComparator.compare(info1, info2) > 0));

        try {
            mDisplayNameComparator.compare(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }
    }
}

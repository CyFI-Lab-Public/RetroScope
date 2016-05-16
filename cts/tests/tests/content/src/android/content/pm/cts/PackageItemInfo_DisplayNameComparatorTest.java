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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageItemInfo.DisplayNameComparator;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.AndroidTestCase;

public class PackageItemInfo_DisplayNameComparatorTest extends AndroidTestCase {
    private static final String PACKAGE_NAME = "com.android.cts.stub";
    private static final String ACTIVITY_NAME = "android.content.pm.cts.TestPmActivity";
    private static final String CMPACTIVITY_NAME = "android.content.pm.cts.TestPmCompare";

    public void testDisplayNameComparator() throws NameNotFoundException {
        PackageManager pm = getContext().getPackageManager();
        DisplayNameComparator comparator = new DisplayNameComparator(pm);

        ComponentName componentName = new ComponentName(PACKAGE_NAME, ACTIVITY_NAME);
        ActivityInfo activityInfo = pm.getActivityInfo(componentName, 0);
        PackageItemInfo pkgItemInfo = new PackageItemInfo(activityInfo);

        componentName = new ComponentName(PACKAGE_NAME, CMPACTIVITY_NAME);
        activityInfo = pm.getActivityInfo(componentName, 0);
        PackageItemInfo cmpInfo = new PackageItemInfo(activityInfo);
        assertTrue(comparator.compare(pkgItemInfo, cmpInfo) < 0);
        assertTrue(comparator.compare(pkgItemInfo, pkgItemInfo) == 0);
        assertTrue(comparator.compare(cmpInfo, pkgItemInfo) > 0);
    }
}

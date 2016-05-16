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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.Printer;

public class ResolveInfoTest extends AndroidTestCase {
    private static final String PACKAGE_NAME = "com.android.cts.stub";
    private static final String MAIN_ACTION_NAME = "android.intent.action.MAIN";
    private static final String ACTIVITY_NAME = "android.content.pm.cts.TestPmActivity";
    private static final String SERVICE_NAME = "android.content.pm.cts.activity.PMTEST_SERVICE";

    public final void testResolveInfo() {
        // Test constructor
        new ResolveInfo();

        PackageManager pm = getContext().getPackageManager();
        Intent intent = new Intent(MAIN_ACTION_NAME);
        intent.setComponent(new ComponentName(PACKAGE_NAME, ACTIVITY_NAME));
        ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        // Test loadLabel, loadIcon, getIconResource, toString, describeContents
        String expectedLabel = "Android TestCase";
        assertEquals(expectedLabel, resolveInfo.loadLabel(pm).toString());
        assertNotNull(resolveInfo.loadIcon(pm));
        assertTrue(resolveInfo.getIconResource() != 0);
        assertNotNull(resolveInfo.toString());
        assertEquals(0, resolveInfo.describeContents());
    }

    public final void testDump() {
        PackageManager pm = getContext().getPackageManager();
        Intent intent = new Intent(SERVICE_NAME);
        ResolveInfo resolveInfo = pm.resolveService(intent, PackageManager.GET_RESOLVED_FILTER);

        Parcel p = Parcel.obtain();
        resolveInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        ResolveInfo infoFromParcel = ResolveInfo.CREATOR.createFromParcel(p);
        // Test writeToParcel
        assertEquals(resolveInfo.getIconResource(), infoFromParcel.getIconResource());
        assertEquals(resolveInfo.priority, infoFromParcel.priority);
        assertEquals(resolveInfo.preferredOrder, infoFromParcel.preferredOrder);
        assertEquals(resolveInfo.match, infoFromParcel.match);
        assertEquals(resolveInfo.specificIndex, infoFromParcel.specificIndex);
        assertEquals(resolveInfo.labelRes, infoFromParcel.labelRes);
        assertEquals(resolveInfo.nonLocalizedLabel, infoFromParcel.nonLocalizedLabel);
        assertEquals(resolveInfo.icon, infoFromParcel.icon);

        // Test dump
        TestPrinter printer = new TestPrinter();
        String prefix = "TestResolveInfo";
        resolveInfo.dump(printer, prefix);
        assertTrue(printer.isPrintlnCalled);
    }

    private class TestPrinter implements Printer {
        public boolean isPrintlnCalled;
        public void println(String x) {
            isPrintlnCalled = true;
        }
    }
}

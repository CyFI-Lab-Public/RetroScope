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
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class InstrumentationInfoTest extends AndroidTestCase {
    private static final String PACKAGE_NAME = "com.android.cts.stub";
    private static final String INSTRUMENTATION_NAME =
            "android.content.pm.cts.TestPmInstrumentation";

    public void testInstrumentationInfo() throws NameNotFoundException {
        PackageManager pm = getContext().getPackageManager();
        ComponentName componentName = new ComponentName(PACKAGE_NAME, INSTRUMENTATION_NAME);
        Parcel p = Parcel.obtain();

        // Test constructors
        new InstrumentationInfo();
        InstrumentationInfo instrInfo = pm.getInstrumentationInfo(componentName, 0);
        InstrumentationInfo infoFromExisted = new InstrumentationInfo(instrInfo);
        checkInfoSame(instrInfo, infoFromExisted);

        // Test toString, describeContents
        assertNotNull(instrInfo.toString());
        assertEquals(0, instrInfo.describeContents());

        // Test writeToParcel
        instrInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        InstrumentationInfo infoFromParcel = InstrumentationInfo.CREATOR.createFromParcel(p);
        checkInfoSame(instrInfo, infoFromParcel);
        p.recycle();
    }

    private void checkInfoSame(InstrumentationInfo expected, InstrumentationInfo actual) {
        assertEquals(expected.name, actual.name);
        assertEquals(expected.dataDir, actual.dataDir);
        assertEquals(expected.handleProfiling, actual.handleProfiling);
        assertEquals(expected.functionalTest, actual.functionalTest);
        assertEquals(expected.targetPackage, actual.targetPackage);
        assertEquals(expected.sourceDir, actual.sourceDir);
        assertEquals(expected.publicSourceDir, actual.publicSourceDir);
    }
}

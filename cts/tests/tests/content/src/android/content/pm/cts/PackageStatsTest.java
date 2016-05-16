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


import android.content.pm.PackageStats;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class PackageStatsTest extends AndroidTestCase {
    private static final String PACKAGE_NAME = "com.android.cts.stub";

    public void testPackageStats() {
        // Set mock data to make sure the functionality of constructor
        long codeSize = 10000;
        long cacheSize = 10240;
        long dataSize = 4096;

        // Test PackageStats(String pkgName), PackageStats(PackageStats pStats)
        PackageStats stats = new PackageStats(PACKAGE_NAME);
        assertEquals(PACKAGE_NAME, stats.packageName);
        stats.cacheSize = codeSize;
        stats.codeSize = cacheSize;
        stats.dataSize = dataSize;
        PackageStats infoFromExisted = new PackageStats(stats);
        checkInfoSame(stats, infoFromExisted);

        // Test toString, describeContents
        assertNotNull(stats.toString());
        assertEquals(0, stats.describeContents());

        // Test writeToParcel, PackageStats(Parcel source)
        Parcel p = Parcel.obtain();
        stats.writeToParcel(p, 0);
        p.setDataPosition(0);
        // CREATOR invokes public PackageStats(Parcel source)
        PackageStats infoFromParcel = PackageStats.CREATOR.createFromParcel(p);
        checkInfoSame(stats, infoFromParcel);
        p.recycle();
    }

    private void checkInfoSame(PackageStats expected, PackageStats actual) {
        assertEquals(expected.packageName, actual.packageName);
        assertEquals(expected.cacheSize, actual.cacheSize);
        assertEquals(expected.dataSize, actual.dataSize);
        assertEquals(expected.codeSize, actual.codeSize);
    }
}

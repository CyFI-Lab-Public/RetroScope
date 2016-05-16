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
import android.os.Process;
import android.test.AndroidTestCase;

public class ActivityManagerMemoryInfoTest extends AndroidTestCase {
    protected ActivityManager.MemoryInfo mMemory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMemory = new ActivityManager.MemoryInfo();
    }

    public void testDescribeContents() {
        assertEquals(0, mMemory.describeContents());
    }

    public void testWriteToParcel() throws Exception {
        final long AVAILMEM = Process.getFreeMemory();
        mMemory.availMem = AVAILMEM;
        final long THRESHOLD = 500l;
        mMemory.threshold = THRESHOLD;
        final boolean LOWMEMORY = true;
        mMemory.lowMemory = LOWMEMORY;
        Parcel parcel = Parcel.obtain();
        mMemory.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.MemoryInfo values =
            ActivityManager.MemoryInfo.CREATOR.createFromParcel(parcel);
        assertEquals(AVAILMEM, values.availMem);
        assertEquals(THRESHOLD, values.threshold);
        assertEquals(LOWMEMORY, values.lowMemory);

        // test null condition.
        try {
            mMemory.writeToParcel(null, 0);
            fail("writeToParcel should throw out NullPointerException when Parcel is null");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testReadFromParcel() throws Exception {
        final long AVAILMEM = Process.getFreeMemory();
        mMemory.availMem = AVAILMEM;
        final long THRESHOLD = 500l;
        mMemory.threshold = THRESHOLD;
        final boolean LOWMEMORY = true;
        mMemory.lowMemory = LOWMEMORY;
        Parcel parcel = Parcel.obtain();
        mMemory.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ActivityManager.MemoryInfo result = new ActivityManager.MemoryInfo();
        result.readFromParcel(parcel);
        assertEquals(AVAILMEM, result.availMem);
        assertEquals(THRESHOLD, result.threshold);
        assertEquals(LOWMEMORY, result.lowMemory);

        // test null condition.
        result = new ActivityManager.MemoryInfo();
        try {
            result.readFromParcel(null);
            fail("readFromParcel should throw out NullPointerException when Parcel is null");
        } catch (NullPointerException e) {
            // expected
        }
    }

}

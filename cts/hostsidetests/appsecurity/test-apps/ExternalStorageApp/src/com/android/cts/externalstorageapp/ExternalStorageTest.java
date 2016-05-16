/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.externalstorageapp;

import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_NONE;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_READ;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_WRITE;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirNoAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertFileNoAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertFileReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.buildGiftForPackage;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.getAllPackageSpecificPaths;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.readInt;

import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.util.List;

/**
 * Test external storage from an application that has no external storage
 * permissions.
 */
public class ExternalStorageTest extends AndroidTestCase {

    public void testPrimaryNoAccess() throws Exception {
        assertDirNoAccess(Environment.getExternalStorageDirectory());
    }

    /**
     * Verify that above our package directories we always have no access.
     */
    public void testAllWalkingUpTreeNoAccess() throws Exception {
        final List<File> paths = getAllPackageSpecificPaths(getContext());
        final String packageName = getContext().getPackageName();

        for (File path : paths) {
            if (path == null) continue;

            assertTrue(path.getAbsolutePath().contains(packageName));

            // Walk up until we drop our package
            while (path.getAbsolutePath().contains(packageName)) {
                assertDirReadWriteAccess(path);
                path = path.getParentFile();
            }

            // Keep walking up until we leave device
            while (Environment.MEDIA_MOUNTED.equals(Environment.getStorageState(path))) {
                assertDirNoAccess(path);
                path = path.getParentFile();
            }
        }
    }

    /**
     * Verify we can read only our gifts.
     */
    public void doVerifyGifts() throws Exception {
        final File none = buildGiftForPackage(getContext(), PACKAGE_NONE);
        assertFileReadWriteAccess(none);
        assertEquals(100, readInt(none));

        final File read = buildGiftForPackage(getContext(), PACKAGE_READ);
        assertFileNoAccess(read);

        final File write = buildGiftForPackage(getContext(), PACKAGE_WRITE);
        assertFileNoAccess(write);
    }
}

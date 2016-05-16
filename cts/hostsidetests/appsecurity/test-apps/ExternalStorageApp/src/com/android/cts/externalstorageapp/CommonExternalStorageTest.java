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

import android.content.Context;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests common functionality that should be supported regardless of external
 * storage status.
 */
public class CommonExternalStorageTest extends AndroidTestCase {
    public static final String TAG = "CommonExternalStorageTest";

    public static final String PACKAGE_NONE = "com.android.cts.externalstorageapp";
    public static final String PACKAGE_READ = "com.android.cts.readexternalstorageapp";
    public static final String PACKAGE_WRITE = "com.android.cts.writeexternalstorageapp";

    /**
     * Primary storage must always be mounted.
     */
    public void testExternalStorageMounted() {
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
    }

    /**
     * Verify that single path is always first item in multiple.
     */
    public void testMultipleCacheDirs() throws Exception {
        final File single = getContext().getExternalCacheDir();
        assertNotNull("Primary storage must always be available", single);
        final File firstMultiple = getContext().getExternalCacheDirs()[0];
        assertEquals(single, firstMultiple);
    }

    /**
     * Verify that single path is always first item in multiple.
     */
    public void testMultipleFilesDirs() throws Exception {
        final File single = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assertNotNull("Primary storage must always be available", single);
        final File firstMultiple = getContext()
                .getExternalFilesDirs(Environment.DIRECTORY_PICTURES)[0];
        assertEquals(single, firstMultiple);
    }

    /**
     * Verify that single path is always first item in multiple.
     */
    public void testMultipleObbDirs() throws Exception {
        final File single = getContext().getObbDir();
        assertNotNull("Primary storage must always be available", single);
        final File firstMultiple = getContext().getObbDirs()[0];
        assertEquals(single, firstMultiple);
    }

    /**
     * Verify we can write to our own package dirs.
     */
    public void testAllPackageDirsWritable() throws Exception {
        final List<File> paths = getAllPackageSpecificPaths(getContext());
        for (File path : paths) {
            assertNotNull("Valid media must be inserted during CTS", path);
            assertEquals("Valid media must be inserted during CTS", Environment.MEDIA_MOUNTED,
                    Environment.getStorageState(path));

            assertDirReadWriteAccess(path);

            final File directChild = new File(path, "directChild");
            final File subdir = new File(path, "subdir");
            final File subdirChild = new File(path, "subdirChild");

            writeInt(directChild, 32);
            subdir.mkdirs();
            assertDirReadWriteAccess(subdir);
            writeInt(subdirChild, 64);

            assertEquals(32, readInt(directChild));
            assertEquals(64, readInt(subdirChild));
        }

        for (File path : paths) {
            deleteContents(path);
        }
    }

    /**
     * Return a set of several package-specific external storage paths.
     */
    public static List<File> getAllPackageSpecificPaths(Context context) {
        final List<File> paths = new ArrayList<File>();
        Collections.addAll(paths, context.getExternalCacheDirs());
        Collections.addAll(paths, context.getExternalFilesDirs(null));
        Collections.addAll(paths, context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES));
        Collections.addAll(paths, context.getObbDirs());
        return paths;
    }

    public static List<File> getAllPackageSpecificPathsExceptObb(Context context) {
        final List<File> paths = new ArrayList<File>();
        Collections.addAll(paths, context.getExternalCacheDirs());
        Collections.addAll(paths, context.getExternalFilesDirs(null));
        Collections.addAll(paths, context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES));
        return paths;
    }

    public static List<File> getPrimaryPackageSpecificPaths(Context context) {
        final List<File> paths = new ArrayList<File>();
        Collections.addAll(paths, context.getExternalCacheDir());
        Collections.addAll(paths, context.getExternalFilesDir(null));
        Collections.addAll(paths, context.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        Collections.addAll(paths, context.getObbDir());
        return paths;
    }

    public static List<File> getSecondaryPackageSpecificPaths(Context context) {
        final List<File> paths = new ArrayList<File>();
        Collections.addAll(paths, dropFirst(context.getExternalCacheDirs()));
        Collections.addAll(paths, dropFirst(context.getExternalFilesDirs(null)));
        Collections.addAll(
                paths, dropFirst(context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES)));
        Collections.addAll(paths, dropFirst(context.getObbDirs()));
        return paths;
    }

    private static File[] dropFirst(File[] before) {
        final File[] after = new File[before.length - 1];
        System.arraycopy(before, 1, after, 0, after.length);
        return after;
    }

    public static File buildGiftForPackage(Context context, String packageName) {
        final File myCache = context.getExternalCacheDir();
        return new File(myCache.getAbsolutePath().replace(context.getPackageName(), packageName),
                packageName + ".gift");
    }

    public static File buildProbeFile(File dir) {
        return new File(dir, ".probe_" + System.nanoTime());
    }

    public static void assertDirReadOnlyAccess(File path) {
        Log.d(TAG, "Asserting read-only access to " + path);

        assertTrue("exists", path.exists());
        assertTrue("read", path.canRead());
        assertTrue("execute", path.canExecute());
        assertNotNull("list", path.list());

        try {
            final File probe = buildProbeFile(path);
            probe.createNewFile();
            probe.delete();
            fail("able to create probe!");
        } catch (IOException e) {
            // expected
        }
    }

    public static void assertDirReadWriteAccess(File path) {
        Log.d(TAG, "Asserting read/write access to " + path);

        assertTrue("exists", path.exists());
        assertTrue("read", path.canRead());
        assertTrue("execute", path.canExecute());
        assertNotNull("list", path.list());

        try {
            final File probe = buildProbeFile(path);
            probe.createNewFile();
            probe.delete();
        } catch (IOException e) {
            fail("failed to create probe!");
        }
    }

    public static void assertDirNoAccess(File path) {
        Log.d(TAG, "Asserting no access to " + path);

        assertFalse("read", path.canRead());
        assertNull("list", path.list());

        try {
            final File probe = buildProbeFile(path);
            probe.createNewFile();
            probe.delete();
            fail("able to create probe!");
        } catch (IOException e) {
            // expected
        }
    }

    public static void assertDirNoWriteAccess(File path) {
        Log.d(TAG, "Asserting no write access to " + path);

        try {
            final File probe = buildProbeFile(path);
            probe.createNewFile();
            probe.delete();
            fail("able to create probe!");
        } catch (IOException e) {
            // expected
        }
    }

    public static void assertFileReadOnlyAccess(File path) {
        try {
            new FileInputStream(path).close();
        } catch (IOException e) {
            fail("failed to read!");
        }

        try {
            new FileOutputStream(path, true).close();
            fail("able to write!");
        } catch (IOException e) {
            // expected
        }
    }

    public static void assertFileReadWriteAccess(File path) {
        try {
            new FileInputStream(path).close();
        } catch (IOException e) {
            fail("failed to read!");
        }

        try {
            new FileOutputStream(path, true).close();
        } catch (IOException e) {
            fail("failed to write!");
        }
    }

    public static void assertFileNoAccess(File path) {
        try {
            new FileInputStream(path).close();
            fail("able to read!");
        } catch (IOException e) {
            // expected
        }

        try {
            new FileOutputStream(path, true).close();
            fail("able to write!");
        } catch (IOException e) {
            // expected
        }
    }

    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteContents(file);
                }
                assertTrue(file.delete());
            }
            assertEquals(0, dir.listFiles().length);
        }
    }

    public static void writeInt(File file, int value) throws IOException {
        final DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
        try {
            os.writeInt(value);
        } finally {
            os.close();
        }
    }

    public static int readInt(File file) throws IOException {
        final DataInputStream is = new DataInputStream(new FileInputStream(file));
        try {
            return is.readInt();
        } finally {
            is.close();
        }
    }
}

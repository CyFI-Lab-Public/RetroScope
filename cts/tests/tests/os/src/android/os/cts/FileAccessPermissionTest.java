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

package android.os.cts;

import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This is testing for file access permissions.
 *
 * <LI><B>/system should be mounted read-only</B><BR> <LI><B>applications should
 * be able to read/write to their own data directory (and only this directory,
 * except for external storage -- see below)</B><BR> <LI><B>applications should
 * not be able to read/write data in another applications /data/data space</B>
 * <BR> <LI><B>external storage directory (/sdcard on G1) should be world
 * read/write</B>
 * Pay attention that if run test test on emulator. You must using mksdcard to
 * create a sdcard image file then start emulator with command emulator -sdcard <filepath>
 * If run this on device, must insert a sdcard into device.
 *
 * mksdcard <size> <file>
 * emulator -sdcard <filepath>
 *
 * TODO: Combine this file with {@link android.permission.cts.FileSystemPermissionTest}
 */
public class FileAccessPermissionTest extends AndroidTestCase {

    /**
     * Test /system dir access.
     */
    public void testSystemDirAccess() {
        File file = new File("/system");
        assertTrue(file.canRead());
        assertFalse(file.canWrite());

        File fakeSystemFile = new File(file, "test");
        try {
            fakeSystemFile.createNewFile();
            fail("should throw out IO exception");
        } catch (IOException e) {
        }

        assertFalse(fakeSystemFile.mkdirs());

        file = new File("/system/app");
        assertTrue(file.canRead());
        assertFalse(file.canWrite());

        // Test not writable / deletable for all files
        File[] allFiles = file.listFiles();
        for (File f : allFiles) {
            assertFalse(f.canWrite());
            assertFalse(f.delete());
        }
    }

    /**
     * Test apks in /system/app.
     */
    public void testApksAlwaysReadable() {
        File file = new File("/system/app");

        // Test readable for only apk files
        File[] apkFiles = file.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".apk");
            }
        });
        for (File f : apkFiles) {
            assertTrue(f.canRead());
        }
    }

    /**
     * Test dir which app can and cannot access.
     */
    public void testAccessAppDataDir() {
        // test /data/app dir.
        File file = new File("/data/app");
        assertTrue(file.isDirectory());
        assertFalse(file.canRead());
        assertFalse(file.canWrite());
        File[] files = file.listFiles();
        assertTrue(files == null || files.length == 0);

        // test app data dir.
        File dir = getContext().getFilesDir();
        assertTrue(dir.canRead());
        assertTrue(dir.canWrite());
        File newFile = new File(dir, System.currentTimeMillis() + "test.txt");
        try {
            assertTrue(newFile.createNewFile());
            writeFileCheck(newFile);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // test not app data dir
        File userAppDataDir = new File("/data/data");
        File otherAppDataDir = new File(userAppDataDir, "com.test.test.dir");
        assertFalse(otherAppDataDir.mkdirs());
        files = userAppDataDir.listFiles();
        assertTrue(files == null || files.length == 0);
        File newOtherAppFile = new File(userAppDataDir, "test.txt");
        try {
            assertFalse(newOtherAppFile.createNewFile());
            writeFileCheck(newOtherAppFile);
            fail("Created file in other app's directory");
        } catch (IOException e) {
            // expected
        }

        // test /sdcard dir.
        File sdcardDir = Environment.getExternalStorageDirectory();
        assertTrue(sdcardDir.exists());
        File sdcardFile = new File(sdcardDir, System.currentTimeMillis() + "test.txt");
        try {
            assertTrue(sdcardFile.createNewFile());
            writeFileCheck(sdcardFile);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        File sdcardSubDir = new File(sdcardDir, System.currentTimeMillis() + "test");
        assertTrue(sdcardSubDir.mkdirs());
        File sdcardSubDirFile = new File(sdcardSubDir, System.currentTimeMillis() + "test.txt");
        try {
            assertTrue(sdcardSubDirFile.createNewFile());
            writeFileCheck(sdcardSubDirFile);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            assertTrue(sdcardSubDir.delete());
        }
    }

    private void writeFileCheck(File file) {
        FileOutputStream fout = null;
        FileInputStream fin = null;
        try {
            byte[]data = new byte[]{0x1, 0x2, 0x3,0x4};
            fout = new FileOutputStream(file);
            fout.write(data);
            fout.flush();
            fout.close();
            fout = null;
            fin = new FileInputStream(file);
            for (int i = 0; i < 4; i++) {
                assertEquals(i + 1, fin.read());
            }
            fin.close();
            fin = null;
        } catch (FileNotFoundException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
            assertTrue(file.delete());
        }
    }

}

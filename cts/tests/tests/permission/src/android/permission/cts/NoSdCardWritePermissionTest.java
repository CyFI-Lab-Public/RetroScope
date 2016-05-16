/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.permission.cts;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.test.AndroidTestCase;

/**
 * Test writing to SD card requires permissions
 */
public class NoSdCardWritePermissionTest extends AndroidTestCase {

    /**
     * Verify that writing to the external storage device requires {@link
     * android.permission.WRITE_EXTERNAL_STORAGE}.
     * @since 4
     */
    public void testWriteExternalStorage() throws FileNotFoundException, IOException {
        try {
            String fl = Environment.getExternalStorageDirectory().toString() +
                         "/this-should-not-exist.txt";
            FileOutputStream strm = new FileOutputStream(fl);
            strm.write("Oops!".getBytes());
            strm.flush();
            strm.close();
            fail("Was able to create and write to " + fl);
        } catch (SecurityException e) {
            // expected
        } catch (FileNotFoundException e) {
            // expected
        }
    }
}

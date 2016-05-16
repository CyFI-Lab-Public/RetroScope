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

package android.drm.cts;

import java.util.Iterator;
import android.util.Log;
import android.test.AndroidTestCase;
import android.drm.DrmSupportInfo;

public class DrmSupportInfoTest extends AndroidTestCase {
    private static final String TAG = "CtsDrmSupportInfoTest";

    public static void testAddInvalidMimeTypes() throws Exception {
        checkAddInvalidMimeType(null);
        checkAddInvalidMimeType("");
    }

    public static void testAddValidMimeTypes() throws Exception {
        checkAddValidMimeType("Hello");
        checkAddValidMimeType("World");
        checkAddValidMimeType("Hello World");
    }

    public static void testAddInvalidFileSuffixes() throws Exception {
        checkAddInvalidFileSuffix("");
    }

    public static void testAddValidFileSuffixes() throws Exception {
        // Note that null file suffix is valid (for files without suffixes)
        checkAddValidFileSuffix(null);
        checkAddValidFileSuffix("Hell");
        checkAddValidFileSuffix("World");
        checkAddValidFileSuffix("Hello World");
    }

    public static void testSetInvalidDescriptions() throws Exception {
        checkSetInvalidDescription("");
        checkSetInvalidDescription(null);
    }

    public static void testSetAndGetDescription() throws Exception {
        checkSetAndGetDescription("Hello");
        checkSetAndGetDescription("World");
        checkSetAndGetDescription("Hello World");
    }

    public static void testEquals() throws Exception {
        checkEqualsOnDefaultInfoObjects();
        checkEqualsOnNondefaultInfoObjects();
    }

    private static void checkEqualsOnDefaultInfoObjects() throws Exception {
        DrmSupportInfo info1 = new DrmSupportInfo();
        DrmSupportInfo info2 = new DrmSupportInfo();
        assertTrue(info1.equals(info2));
    }

    private static void checkEqualsOnNondefaultInfoObjects() throws Exception {
        DrmSupportInfo info1 = new DrmSupportInfo();
        DrmSupportInfo info2 = new DrmSupportInfo();
        DrmSupportInfo info3 = new DrmSupportInfo();
        info1.addMimeType("Hello");
        info2.addMimeType("hello");  // lowercase 'h'
        assertFalse(info1.equals(info2));
        info3.addMimeType("Hello");
        assertTrue(info1.equals(info3));
        info1.setDescription("World");
        info3.setDescription("world");  // lowercase 'w'
        assertFalse(info1.equals(info3));
        info3.setDescription("World");
        assertTrue(info1.equals(info3));
        info1.addFileSuffix("txt");
        info3.addFileSuffix("TXT");  // uppercase 'TXT'
        assertFalse(info1.equals(info3));
    }

    private static void checkSetAndGetDescription(String description) throws Exception {
        DrmSupportInfo info = new DrmSupportInfo();
        info.setDescription(description);
        assertEquals(info.getDescriprition(), description);
    }

    private static void checkSetInvalidDescription(String description) throws Exception {
        DrmSupportInfo info = new DrmSupportInfo();
        try {
            info.setDescription(description);
            fail("Description '" + description + "' was accepted for DrmSupportInfo");
        } catch (IllegalArgumentException e) {
            // Expected and thus intentionally ignored.
        }
    }

    private static void checkAddInvalidFileSuffix(String fileSuffix) throws Exception {
        DrmSupportInfo info = new DrmSupportInfo();
        try {
            info.addFileSuffix(fileSuffix);
            info = null;
            fail("File suffix '" + fileSuffix + "' was accepted for DrmSupportInfo");
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored.
        }
    }

    private static void checkAddValidFileSuffix(String fileSuffix) throws Exception {
        DrmSupportInfo info = new DrmSupportInfo();
        info.addFileSuffix(fileSuffix);
        Iterator<String> suffixes = info.getFileSuffixIterator();
        assertTrue(suffixes.hasNext());
        String suffix = (String) suffixes.next();
        assertEquals(suffix, fileSuffix);
    }
    private static void checkAddInvalidMimeType(String mimeType) throws Exception {
        DrmSupportInfo info = new DrmSupportInfo();
        try {
            info.addMimeType(mimeType);
            fail("Mime type '" + mimeType + "' was accepted for DrmSupportInfo");
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored.
        }
    }

    private static void checkAddValidMimeType(String mimeType) throws Exception {
        DrmSupportInfo info = new DrmSupportInfo();
        info.addMimeType(mimeType);
        Iterator<String> mimes = info.getMimeTypeIterator();
        assertTrue(mimes.hasNext());
        String mime = (String) mimes.next();
        assertEquals(mime, mimeType);
    }
}

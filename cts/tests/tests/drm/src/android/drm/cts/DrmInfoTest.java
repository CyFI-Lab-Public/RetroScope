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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import android.util.Log;
import android.test.AndroidTestCase;
import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;

public class DrmInfoTest extends AndroidTestCase {
    private static final String TAG = "CtsDrmInfoTest";
    private static final byte[] DEFAULT_DATA = new byte[1];
    private static final String DEFAULT_MIME = "video/";
    private static final int DEFAULT_TYPE =
            DrmInfoRequest.TYPE_REGISTRATION_INFO;

    public static void testInvalidInfoTypes() throws Exception {
        checkInvalidInfoType(DrmInfoRequest.TYPE_REGISTRATION_INFO - 1);
        checkInvalidInfoType(
                DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_PROGRESS_INFO + 1);
    }

    public static void testValidInfoTypes() throws Exception {
        checkValidInfoType(DrmInfoRequest.TYPE_REGISTRATION_INFO);
        checkValidInfoType(DrmInfoRequest.TYPE_UNREGISTRATION_INFO);
        checkValidInfoType(DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO);
        checkValidInfoType(
                DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_PROGRESS_INFO);
    }

    public static void testGetInfoType() throws Exception {
        checkGetInfoType(DrmInfoRequest.TYPE_REGISTRATION_INFO);
        checkGetInfoType(DrmInfoRequest.TYPE_UNREGISTRATION_INFO);
        checkGetInfoType(DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO);
        checkGetInfoType(
                DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_PROGRESS_INFO);
    }

    public static void testInvalidMimeTypes() throws Exception {
        checkInvalidMimeType("");
        checkInvalidMimeType(null);
    }

    public static void testGetMimeType() throws Exception {
        checkGetMimeType("Hello");
        checkGetMimeType("World");
        checkGetMimeType("Hello World");
    }

    public static void testInvalidData() throws Exception {
        checkInvalidData(null);
    }

    public static void testGetData() throws Exception {
        checkGetData(DEFAULT_DATA);
    }

    public static void testPutAndGetKeys() throws Exception {
        HashMap<String, Object> attributes = new HashMap<String, Object>(3);
        attributes.put("Hello", "");
        attributes.put("World", null);
        attributes.put("Hello World", "Hello World");

        // Store all the attributes in DrmInfo object info.
        DrmInfo info = new DrmInfo(DEFAULT_TYPE, DEFAULT_DATA, DEFAULT_MIME);
        Iterator<String> keys = attributes.keySet().iterator();
        while (keys.hasNext()) {
            final String key = (String) keys.next();
            info.put(key, attributes.get(key));
        }

        // info object must have all the keys that attributes does.
        Iterator<String> infoKeys = info.keyIterator();
        while (keys.hasNext()) {
            final String key = (String) keys.next();
            assertEquals(info.get(key), attributes.get(key));
        }

        // Attributes object must have all the keys that info does.
        while (infoKeys.hasNext()) {
            final String key = (String) infoKeys.next();
            assertEquals(info.get(key), attributes.get(key));
        }

        // Also test the set of the values
        Iterator<Object> iterator = info.iterator();
        Collection<Object> vals = attributes.values();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            assertTrue(vals.contains(o));
        }
    }

    private static void checkInvalidData(byte[] data) throws Exception {
        try {
            DrmInfo info = new DrmInfo(DEFAULT_TYPE, data, DEFAULT_MIME);
            fail("Data " + data + " was accepted for DrmInfo");
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored.
        }
    }

    private static void checkInvalidMimeType(String mimeType) throws Exception {
        try {
            DrmInfo info = new DrmInfo(DEFAULT_TYPE, DEFAULT_DATA, mimeType);
            fail("Mime type " + mimeType + " was accepted for DrmInfo");
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored.
        }
    }

    private static void checkGetData(byte[] data) throws Exception {
        DrmInfo info = new DrmInfo(DEFAULT_TYPE, data, DEFAULT_MIME);
        assertEquals(info.getData(), data);
    }

    private static void checkGetMimeType(String mimeType) throws Exception {
        DrmInfo info = new DrmInfo(DEFAULT_TYPE, DEFAULT_DATA, mimeType);
        assertEquals(info.getMimeType(), mimeType);
    }

    private static void checkGetInfoType(int type) throws Exception {
        DrmInfo info = new DrmInfo(type, DEFAULT_DATA, DEFAULT_MIME);
        assertEquals(info.getInfoType(), type);
    }

    private static void checkInvalidInfoType(int type) throws Exception {
        try {
            DrmInfo info = new DrmInfo(type, DEFAULT_DATA, DEFAULT_MIME);
            fail("Info type " + type + " was accepted for DrmInfo");
        } catch(IllegalArgumentException e) {
            // Expected, and thus intentionally ignored.
        }
    }

    private static void checkValidInfoType(int type) throws Exception {
        DrmInfo info = new DrmInfo(type, DEFAULT_DATA, DEFAULT_MIME);
    }
}

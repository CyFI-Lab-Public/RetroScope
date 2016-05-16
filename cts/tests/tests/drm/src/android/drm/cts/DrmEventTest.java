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
import android.util.Log;
import android.test.AndroidTestCase;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;
import android.drm.DrmErrorEvent;

public class DrmEventTest extends AndroidTestCase {
    private static String TAG = "CtsDrmEventTest";

    public static void testGetAttribute() throws Exception {
        HashMap<String, Object> attributes = new HashMap<String, Object>(3);
        attributes.put("Hello World", attributes);
        attributes.put("Hello", "World");
        attributes.put("World", "");

        // DrmInfoEvent related
        checkGetAttributeWithEventType(null, "NotNull", true);
        checkGetAttributeWithEventType(null, null, true);
        checkGetAttributeWithEventType(attributes, null, true);
        checkGetAttributeWithEventType(attributes, "", true);
        checkGetAttributeWithEventType(attributes, "Hello", true);
        checkGetAttributeWithEventType(attributes, "World", true);
        checkGetAttributeWithEventType(attributes, "Hello World", true);

        // DrmErrorEvent related
        checkGetAttributeWithEventType(null, "NotNull", false);
        checkGetAttributeWithEventType(null, null, false);
        checkGetAttributeWithEventType(attributes, null, false);
        checkGetAttributeWithEventType(attributes, "", false);
        checkGetAttributeWithEventType(attributes, "Hello", false);
        checkGetAttributeWithEventType(attributes, "World", false);
        checkGetAttributeWithEventType(attributes, "Hello World", false);
    }

    public static void testGetMessage() throws Exception {
        // DrmInfoEvent related
        checkGetMessageWithEventType(null, true);
        checkGetMessageWithEventType("", true);
        checkGetMessageWithEventType("Hello World", true);

        // DrmErrorEvent related
        checkGetMessageWithEventType(null, false);
        checkGetMessageWithEventType("", false);
        checkGetMessageWithEventType("Hello World", false);
    }

    public static void testGetUniqueId() throws Exception {
        // DrmInfoEvent related
        checkGetUniqueIdWithEventType(-1, true);
        checkGetUniqueIdWithEventType(0,  true);
        checkGetUniqueIdWithEventType(1,  true);

        // DrmErrorEvent related
        checkGetUniqueIdWithEventType(-1, false);
        checkGetUniqueIdWithEventType(0,  false);
        checkGetUniqueIdWithEventType(1,  false);
    }

    public static void testValidErrorEventTypes() throws Exception {
        checkValidErrorType(DrmErrorEvent.TYPE_RIGHTS_NOT_INSTALLED);

        checkValidErrorType(
                DrmErrorEvent.TYPE_RIGHTS_RENEWAL_NOT_ALLOWED);

        checkValidErrorType(DrmErrorEvent.TYPE_NOT_SUPPORTED);
        checkValidErrorType(DrmErrorEvent.TYPE_OUT_OF_MEMORY);
        checkValidErrorType(DrmErrorEvent.TYPE_NO_INTERNET_CONNECTION);
        checkValidErrorType(DrmErrorEvent.TYPE_PROCESS_DRM_INFO_FAILED);
        checkValidErrorType(DrmErrorEvent.TYPE_REMOVE_ALL_RIGHTS_FAILED);
        checkValidErrorType(DrmErrorEvent.TYPE_ACQUIRE_DRM_INFO_FAILED);
    }

    public static void testValidInfoEventTypes() throws Exception {
        checkValidInfoType(
                DrmInfoEvent.TYPE_ALREADY_REGISTERED_BY_ANOTHER_ACCOUNT);

        checkValidInfoType(DrmInfoEvent.TYPE_REMOVE_RIGHTS);
        checkValidInfoType(DrmInfoEvent.TYPE_RIGHTS_INSTALLED);
        checkValidInfoType(DrmInfoEvent.TYPE_WAIT_FOR_RIGHTS);

        checkValidInfoType(
                DrmInfoEvent.TYPE_ACCOUNT_ALREADY_REGISTERED);

        checkValidInfoType(DrmInfoEvent.TYPE_RIGHTS_REMOVED);


        // DrmEvent should be just DrmInfoEvent
        checkValidInfoType(DrmEvent.TYPE_ALL_RIGHTS_REMOVED);
        checkValidInfoType(DrmEvent.TYPE_DRM_INFO_PROCESSED);

    }

    public static void testInvalidErrorEventTypes() throws Exception {
        checkInfoTypeInErrorEvent(
                DrmInfoEvent.TYPE_ALREADY_REGISTERED_BY_ANOTHER_ACCOUNT);

        checkInfoTypeInErrorEvent(DrmInfoEvent.TYPE_REMOVE_RIGHTS);
        checkInfoTypeInErrorEvent(DrmInfoEvent.TYPE_RIGHTS_INSTALLED);
        checkInfoTypeInErrorEvent(DrmInfoEvent.TYPE_WAIT_FOR_RIGHTS);

        checkInfoTypeInErrorEvent(
                DrmInfoEvent.TYPE_ACCOUNT_ALREADY_REGISTERED);

        checkInfoTypeInErrorEvent(DrmInfoEvent.TYPE_RIGHTS_REMOVED);


        // DrmEvent should be just DrmInfoEvent
        checkInfoTypeInErrorEvent(DrmEvent.TYPE_ALL_RIGHTS_REMOVED);
        checkInfoTypeInErrorEvent(DrmEvent.TYPE_DRM_INFO_PROCESSED);
    }

    public static void testInvalidInfoEventTypes() throws Exception {
        checkErrorTypeInInfoEvent(DrmErrorEvent.TYPE_RIGHTS_NOT_INSTALLED);

        checkErrorTypeInInfoEvent(
                DrmErrorEvent.TYPE_RIGHTS_RENEWAL_NOT_ALLOWED);

        checkErrorTypeInInfoEvent(DrmErrorEvent.TYPE_NOT_SUPPORTED);
        checkErrorTypeInInfoEvent(DrmErrorEvent.TYPE_OUT_OF_MEMORY);
        checkErrorTypeInInfoEvent(DrmErrorEvent.TYPE_NO_INTERNET_CONNECTION);
        checkErrorTypeInInfoEvent(DrmErrorEvent.TYPE_PROCESS_DRM_INFO_FAILED);
        checkErrorTypeInInfoEvent(DrmErrorEvent.TYPE_REMOVE_ALL_RIGHTS_FAILED);
        checkErrorTypeInInfoEvent(DrmErrorEvent.TYPE_ACQUIRE_DRM_INFO_FAILED);
    }

    private static DrmEvent createDrmEvent(
            boolean isInfo, int id, String msg, HashMap<String, Object> attributes) {

        if (isInfo) {
            int type = DrmInfoEvent.TYPE_RIGHTS_INSTALLED;
            if (attributes == null) {
                return new DrmInfoEvent(id, type, msg);
            } else {
                return new DrmInfoEvent(id, type, msg, attributes);
            }
        } else {
            int type = DrmErrorEvent.TYPE_NOT_SUPPORTED;
            if (attributes == null) {
                return new DrmErrorEvent(id, type, msg);
            } else {
                return new DrmErrorEvent(id, type, msg, attributes);
            }
        }
    }

    private static void checkGetAttributeWithEventType(
        HashMap<String, Object> attributes, String key, boolean isInfo) throws Exception {
        DrmEvent event = createDrmEvent(isInfo, 0, "", attributes);
        if (attributes == null) {
            assertNull(event.getAttribute(key));
        } else {
            assertEquals(event.getAttribute(key), attributes.get(key));
        }
    }

    private static void checkGetUniqueIdWithEventType(
            int id, boolean isInfo) throws Exception {

        DrmEvent event = createDrmEvent(isInfo, id, "", null);
        assertEquals(id, event.getUniqueId());
    }

    private static void checkGetMessageWithEventType(
            String msg, boolean isInfo) throws Exception {

        DrmEvent event = createDrmEvent(isInfo, 0, msg, null);
        assertNotNull(event);
        if (msg == null) {
            assertNotNull(event.getMessage());
        } else {
            assertEquals(event.getMessage(), msg);
        }
    }

    private static void checkValidInfoType(int type) throws Exception {
        DrmInfoEvent infoEvent = new DrmInfoEvent(0, type, "");
        assertEquals(infoEvent.getType(), type);
    }

    private static void checkValidErrorType(int type) throws Exception {
        DrmErrorEvent errEvent = new DrmErrorEvent(0, type, "");
        assertEquals(errEvent.getType(), type);
    }

    private static void checkInfoTypeInErrorEvent(int type) throws Exception {
        try {
            DrmErrorEvent errEvent = new DrmErrorEvent(0, type, "");
            fail("Info type accepted for DrmErrorEvent: " + type);
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored
        }

        try {
            DrmErrorEvent errEvent = new DrmErrorEvent(0, type, "", null);
            fail("Info type accepted for DrmErrorEvent: " + type);
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored
        }
    }

    private static void checkErrorTypeInInfoEvent(int type) throws Exception {
        try {
            DrmInfoEvent infoEvent = new DrmInfoEvent(0, type, "");
            fail("Error type accepted for DrmInfoEvent: " + type);
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored
        }

        try {
            DrmInfoEvent infoEvent = new DrmInfoEvent(0, type, "", null);
            fail("Error type accepted for DrmInfoEvent: " + type);
        } catch(IllegalArgumentException e) {
            // Expected and thus intentionally ignored
        }

        /*
         * We could not do the following because the existing
         * public API has design flaws.
         *
        try {
            DrmEvent event = new DrmEvent(id, type, msg);
            fail("Error type accepted for DrmEvent: " + type);
        } catch(IllegalArgumentException iae) {
            // intentionally ignored
        }

        */
    }
}

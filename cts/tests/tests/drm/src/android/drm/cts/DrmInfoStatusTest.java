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

import android.util.Log;
import android.test.AndroidTestCase;
import android.drm.DrmInfoStatus;
import android.drm.DrmInfoRequest;
import android.drm.ProcessedData;

public class DrmInfoStatusTest extends AndroidTestCase {
    private static final String TAG = "CtsDrmInfoStatusTest";
    private static final ProcessedData DEFAULT_DATA = null;
    private static final String DEFAULT_MIME = "video/";
    private static final int DEFAULT_TYPE =
            DrmInfoRequest.TYPE_REGISTRATION_INFO;

    public static void testInvalidStatusCodes() throws Exception {
        checkInvalidStatusCode(DrmInfoStatus.STATUS_ERROR + 1);
        checkInvalidStatusCode(DrmInfoStatus.STATUS_OK - 1);
    }

    public static void testValidStatusCodes() throws Exception {
        checkValidStatusCode(DrmInfoStatus.STATUS_ERROR);
        checkValidStatusCode(DrmInfoStatus.STATUS_OK);
    }

    private static void checkInvalidStatusCode(int statusCode) throws Exception {
        try {
            DrmInfoStatus info = new DrmInfoStatus(
                    statusCode, DEFAULT_TYPE, DEFAULT_DATA, DEFAULT_MIME);
            info = null;
            fail("Status code " + statusCode + " was accepted for DrmInfoStatus");
        } catch(IllegalArgumentException e) {
            // Expected, and thus intentionally ignored.
        }
    }

    private static void checkValidStatusCode(int statusCode) throws Exception {
        DrmInfoStatus info = new DrmInfoStatus(
                statusCode, DEFAULT_TYPE, DEFAULT_DATA, DEFAULT_MIME);
        info = null;
    }
}

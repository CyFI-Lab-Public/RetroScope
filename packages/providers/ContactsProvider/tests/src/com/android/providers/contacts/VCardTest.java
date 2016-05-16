/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.providers.contacts;

import android.content.ContentResolver;
import android.test.suitebuilder.annotation.MediumTest;

import com.android.providers.contacts.testutil.RawContactUtil;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;

/**
 * Tests (or integration tests) verifying if vCard library works well with {@link ContentResolver}.
 *
 * Unit tests for vCard itself should be availabel in vCard library.
 */
@MediumTest
public class VCardTest extends BaseContactsProvider2Test {
    private static final String TAG = "VCardTest";
    private static final boolean DEBUG = false;

    /**
     * Confirms the app fetches a stored contact from resolver and output the name as part of
     * a vCard string.
     */
    public void testCompose() {
        RawContactUtil.createRawContactWithName(mResolver, "John", "Doe");
        final VCardComposer composer = new VCardComposer(
                getContext(), mResolver, VCardConfig.VCARD_TYPE_DEFAULT, null, true);
        assertTrue(composer.init());
        int total = composer.getCount();
        assertEquals(1, total);
        String vcard = composer.createOneEntry();
        assertNotNull(vcard);

        // Check vCard very roughly.
        assertTrue(vcard.contains("John"));
        assertTrue(vcard.contains("Doe"));
    }
}

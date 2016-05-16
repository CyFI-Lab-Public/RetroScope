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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.VoicemailContract;
import android.provider.VoicemailContract.Status;
import android.provider.VoicemailContract.Voicemails;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Unit tests for {@link VoicemailCleanupService}.
 */
@SmallTest
public class VoicemailCleanupServiceTest extends BaseVoicemailProviderTest {
    private static final String TEST_PACKAGE_1 = "package1";
    private static final String TEST_PACKAGE_2 = "package2";
    // Object under test.
    private VoicemailCleanupService mCleanupService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setUpForFullPermission();
        mCleanupService = new VoicemailCleanupService();
    }

    public void testIntentHandling() {
        mCleanupService = new VoicemailCleanupService();
        insertDataForPackage(TEST_PACKAGE_1);
        insertDataForPackage(TEST_PACKAGE_2);
        checkDataExistsForPackages(TEST_PACKAGE_1, TEST_PACKAGE_2);
        // No action on PACKAGE_CHANGED.
        sendIntent(TEST_PACKAGE_1, Intent.ACTION_PACKAGE_CHANGED, null);
        checkDataExistsForPackages(TEST_PACKAGE_1, TEST_PACKAGE_2);

        // No action on PACKAGE_REPLACED.
        sendIntent(TEST_PACKAGE_1, Intent.ACTION_PACKAGE_REPLACED, null);
        checkDataExistsForPackages(TEST_PACKAGE_1, TEST_PACKAGE_2);

        // No action on PACKAGE_REMOVED with EXTRA_REPLACING = true.
        sendIntent(TEST_PACKAGE_1, Intent.ACTION_PACKAGE_REMOVED, true);
        checkDataExistsForPackages(TEST_PACKAGE_1, TEST_PACKAGE_2);

        // Data removed on PACKAGE_REMOVED but with no EXTRA_REPLACING.
        sendIntent(TEST_PACKAGE_1, Intent.ACTION_PACKAGE_REMOVED, null);
        checkDataDoesNotExistForPackage(TEST_PACKAGE_1);
        checkDataExistsForPackages(TEST_PACKAGE_2);

        // Data removed on PACKAGE_REMOVED with EXTRA_REPLACING = false.
        sendIntent(TEST_PACKAGE_2, Intent.ACTION_PACKAGE_REMOVED, false);
        checkDataDoesNotExistForPackage(TEST_PACKAGE_1);
        checkDataDoesNotExistForPackage(TEST_PACKAGE_2);
    }

    private void sendIntent(String sourcePackage, String action, Boolean replacingExtra) {
        Intent packageIntent = new Intent(action, Uri.parse("package:" + sourcePackage));
        if (replacingExtra != null) {
            packageIntent.putExtra(Intent.EXTRA_REPLACING, replacingExtra);
        }
        mCleanupService.handleIntentInternal(packageIntent, mResolver);
    }

    private void insertDataForPackage(String sourcePackage) {
        ContentValues values = new ContentValues();
        values.put(VoicemailContract.SOURCE_PACKAGE_FIELD, sourcePackage);
        mResolver.insert(Voicemails.buildSourceUri(sourcePackage), values);
        mResolver.insert(Status.buildSourceUri(sourcePackage), values);
    }

    void checkDataExistsForPackages(String... sourcePackages) {
        for (String sourcePackage : sourcePackages) {
            checkDataExistsForPackage(sourcePackage);
        }
    }

    private void checkDataExistsForPackage(String sourcePackage) {
        Cursor cursor = mResolver.query(
                Voicemails.buildSourceUri(sourcePackage), null, null, null, null);
        assertNotSame(0, cursor.getCount());
        cursor = mResolver.query(
                Status.buildSourceUri(sourcePackage), null, null, null, null);
        assertNotSame(0, cursor.getCount());
    }

    private void checkDataDoesNotExistForPackage(String sourcePackage) {
        Cursor cursor = mResolver.query(
                Voicemails.buildSourceUri(sourcePackage), null, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor = mResolver.query(
                Status.buildSourceUri(sourcePackage), null, null, null, null);
        assertEquals(0, cursor.getCount());
    }
}

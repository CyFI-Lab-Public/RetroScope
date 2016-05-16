/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.provider.cts;

import static android.provider.ContactsContract.DataUsageFeedback;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.cts.contacts.DataUtil;
import android.provider.cts.contacts.DatabaseAsserts;
import android.provider.cts.contacts.RawContactUtil;
import android.test.AndroidTestCase;
import android.text.TextUtils;

public class ContactsContract_DataUsageTest extends AndroidTestCase {

    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getContext().getContentResolver();
    }

    public void testSingleDataUsageFeedback_incrementsCorrectDataItems() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);

        long[] dataIds = setupRawContactDataItems(ids.mRawContactId);

        // Update just 1 data item at a time.
        updateDataUsageAndAssert(dataIds[1], 1);
        updateDataUsageAndAssert(dataIds[1], 2);

        updateDataUsageAndAssert(dataIds[2], 1);
        updateDataUsageAndAssert(dataIds[2], 2);
        updateDataUsageAndAssert(dataIds[2], 3);

        // Go back and update the previous data item again.
        updateDataUsageAndAssert(dataIds[1], 3);

        deleteDataUsage();
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    public void testMultiIdDataUsageFeedback_incrementsCorrectDataItems() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);

        long[] dataIds = setupRawContactDataItems(ids.mRawContactId);

        assertDataUsageEquals(dataIds, 0, 0, 0, 0);

        updateMultipleAndAssertUpdateSuccess(new long[] {dataIds[1], dataIds[2]});
        assertDataUsageEquals(dataIds, 0, 1, 1, 0);

        updateMultipleAndAssertUpdateSuccess(new long[]{dataIds[1], dataIds[2]});
        assertDataUsageEquals(dataIds, 0, 2, 2, 0);

        updateDataUsageAndAssert(dataIds[1], 3);
        assertDataUsageEquals(dataIds, 0, 3, 2, 0);

        updateMultipleAndAssertUpdateSuccess(new long[]{dataIds[0], dataIds[1]});
        assertDataUsageEquals(dataIds, 1, 4, 2, 0);

        deleteDataUsage();
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    private long[] setupRawContactDataItems(long rawContactId) {
        // Create 4 data items.
        long[] dataIds = new long[4];
        dataIds[0] = DataUtil.insertPhoneNumber(mResolver, rawContactId, "555-5555");
        dataIds[1] = DataUtil.insertPhoneNumber(mResolver, rawContactId, "555-5554");
        dataIds[2] = DataUtil.insertEmail(mResolver, rawContactId, "test@thisisfake.com");
        dataIds[3] = DataUtil.insertPhoneNumber(mResolver, rawContactId, "555-5556");
        return dataIds;
    }

    /**
     * Updates multiple data ids at once.  And asserts the update returned success.
     */
    private void updateMultipleAndAssertUpdateSuccess(long[] dataIds) {
        String[] ids = new String[dataIds.length];
        for (int i = 0; i < dataIds.length; i++) {
            ids[i] = String.valueOf(dataIds[i]);
        }
        Uri uri = DataUsageFeedback.FEEDBACK_URI.buildUpon().appendPath(TextUtils.join(",", ids))
                .appendQueryParameter(DataUsageFeedback.USAGE_TYPE,
                        DataUsageFeedback.USAGE_TYPE_CALL).build();
        int result = mResolver.update(uri, new ContentValues(), null, null);
        assertTrue(result > 0);
    }

    /**
     * Updates a single data item usage.  Asserts the update was successful.  Asserts the usage
     * number is equal to expected value.
     */
    private void updateDataUsageAndAssert(long dataId, int assertValue) {
        Uri uri = DataUsageFeedback.FEEDBACK_URI.buildUpon().appendPath(String.valueOf(dataId))
                .appendQueryParameter(DataUsageFeedback.USAGE_TYPE,
                        DataUsageFeedback.USAGE_TYPE_CALL).build();
        int result = mResolver.update(uri, new ContentValues(), null, null);
        assertTrue(result > 0);

        assertDataUsageEquals(dataId, assertValue);
    }

    /**
     * Assert that the given data ids have usage values in the respective order.
     */
    private void assertDataUsageEquals(long[] dataIds, int... expectedValues) {
        if (dataIds.length != expectedValues.length) {
            throw new IllegalArgumentException("dataIds and expectedValues must be the same size");
        }

        for (int i = 0; i < dataIds.length; i++) {
            assertDataUsageEquals(dataIds[i], expectedValues[i]);
        }
    }

    /**
     * Assert a single data item has a specific usage value.
     */
    private void assertDataUsageEquals(long dataId, int expectedValue) {
        // Query and assert value is expected.
        String[] projection = new String[]{ContactsContract.Data.TIMES_USED};
        String[] record = DataUtil.queryById(mResolver, dataId, projection);
        assertNotNull(record);
        long actual = 0;
        // Tread null as 0
        if (record[0] != null) {
            actual = Long.parseLong(record[0]);
        }
        assertEquals(expectedValue, actual);
    }

    private void deleteDataUsage() {
        mResolver.delete(DataUsageFeedback.DELETE_USAGE_URI, null, null);
    }
}

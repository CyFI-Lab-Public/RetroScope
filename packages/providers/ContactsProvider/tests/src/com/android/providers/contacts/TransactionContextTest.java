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

package com.android.providers.contacts;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;

/**
 * Unit tests for TransactionContext.
 */
@SmallTest
public class TransactionContextTest extends TestCase {

    public void testClearExceptSearchIndexUpdates_returnsNewSets() {
        TransactionContext context = new TransactionContext(false);
        context.markRawContactDirtyAndChanged(1L, false);
        context.rawContactUpdated(1L);
        context.rawContactInserted(1L, 1L);
        context.syncStateUpdated(1L, new Object());

        context.clearExceptSearchIndexUpdates();

        Set<Long> newDirty = context.getDirtyRawContactIds();
        Set<Long> newChanged = context.getChangedRawContactIds();
        Set<Long> newInserted = context.getInsertedRawContactIds();
        Set<Long> newUpdated = context.getUpdatedRawContactIds();
        Set<Map.Entry<Long, Object>> newSync = context.getUpdatedSyncStates();

        assertTrue(newDirty.isEmpty());
        assertTrue(newChanged.isEmpty());
        assertTrue(newInserted.isEmpty());
        assertTrue(newUpdated.isEmpty());
        assertTrue(newSync.isEmpty());
    }

    public void testMarkDirtyAndChanged_onlyUpdatesChanged() {
        TransactionContext context = new TransactionContext(false);

        context.markRawContactDirtyAndChanged(1L, true /* isSyncAdapter */);

        assertEquals(1, context.getChangedRawContactIds().size());
        assertEquals(0, context.getDirtyRawContactIds().size());
    }

    public void testMarkDirtyAndChanged_onlyUpdatesDirtyAndChanged() {
        TransactionContext context = new TransactionContext(false);

        context.markRawContactDirtyAndChanged(1L, false /* isSyncAdapter */);

        assertEquals(1, context.getChangedRawContactIds().size());
        assertEquals(1, context.getDirtyRawContactIds().size());
    }

    public void testRawContactInserted_affectsChangedContacts() {
        TransactionContext context = new TransactionContext(false);
        assertTrue(context.getChangedRawContactIds().isEmpty());

        context.rawContactInserted(1L, 2L);
        assertEquals(1, context.getChangedRawContactIds().size());
        assertTrue(context.getChangedRawContactIds().contains(1L));

        context.rawContactInserted(5L, 10L);
        assertEquals(2, context.getChangedRawContactIds().size());
        assertTrue(context.getChangedRawContactIds().contains(5L));
    }

    public void testMarkRawContactChangedOrDeletedOrInserted_affectsChangedContacts() {
        TransactionContext context = new TransactionContext(false);
        assertTrue(context.getChangedRawContactIds().isEmpty());

        context.markRawContactChangedOrDeletedOrInserted(1L);
        assertEquals(1, context.getChangedRawContactIds().size());
        assertTrue(context.getChangedRawContactIds().contains(1L));

        context.rawContactInserted(5L, 10L);
        assertEquals(2, context.getChangedRawContactIds().size());
        assertTrue(context.getChangedRawContactIds().contains(5L));
    }
}

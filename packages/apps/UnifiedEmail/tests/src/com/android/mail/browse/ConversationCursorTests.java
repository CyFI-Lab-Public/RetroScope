/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.browse;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import java.util.ArrayList;

public class ConversationCursorTests extends ProviderTestCase2<TestProvider> {

    public ConversationCursorTests(Class<TestProvider> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    Activity mActivity;
    Context mMockContext;
    MockContentResolver mMockResolver;

    private static final String CONVO_TABLE = "convo";

    public ConversationCursorTests() {
        super(TestProvider.class, TestProvider.AUTHORITY);
        mActivity = new Activity() {
            @Override
            public ContentResolver getContentResolver() {
                return mMockResolver;
            }
        };
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mMockContext = getMockContext();
        mMockResolver = (MockContentResolver)mMockContext.getContentResolver();
        mMockResolver.addProvider(TestProvider.AUTHORITY, new TestProvider(mMockContext));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static final String SUBJECT_COLUMN = "subject";
    private static final String FOLDER_COLUMN = "folder";
    private static final String READ_COLUMN = "read";
    private static final String STARRED_COLUMN = "starred";
    private static final String URI_COLUMN = "uri";

//    private static final int SUBJECT_INDEX = 0;
//    private static final int FOLDER_INDEX = 1;
//    private static final int READ_INDEX = 2;
//    private static final int STARRED_INDEX = 3;
    private static final int URI_INDEX = 4;

    private static ContentValues makeConvo(String subject, String folder, int read, int starred) {
        ContentValues cv = new ContentValues();
        cv.put(SUBJECT_COLUMN, subject);
        cv.put(FOLDER_COLUMN, folder);
        cv.put(READ_COLUMN, read);
        cv.put(STARRED_COLUMN, starred);
        return cv;
    }

    private Uri setupConvoList() throws RemoteException, OperationApplicationException {
        // The Uri is content://com.android.canhaz/pony
        Uri uri = new Uri.Builder().scheme("content").authority(TestProvider.AUTHORITY)
                .path(CONVO_TABLE).build();
        // Our array of CPO's to be used with applyBatch
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        // Insert 9 convos
        ContentValues values = makeConvo("Subj1", "Folder", 0, 0);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj2", "Folder", 0, 1);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj3", "Folder", 0, 0);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj4", "Folder", 0, 1);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj5", "Folder", 1, 0);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj6", "Folder", 1, 1);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj7", "Folder", 1, 0);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj8", "Folder", 1, 1);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        values = makeConvo("Subj9", "Folder", 1, 0);
        ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        // Apply the batch with one insert operation
        ContentProviderResult[] results = mMockResolver.applyBatch(TestProvider.AUTHORITY, ops);
        ops.clear();
        // Set up the uri column (basis of the cache)
        for (ContentProviderResult result: results) {
            ops.add(ContentProviderOperation
                    .newUpdate(result.uri)
                    .withValue(URI_COLUMN, result.uri.toString())
                    .build());
        }
        mMockResolver.applyBatch(TestProvider.AUTHORITY, ops);
        return uri;
    }

    public void testLocalDelete() throws RemoteException, OperationApplicationException {
        Uri uri = setupConvoList();

//        // Now, get our CC
//        ConversationCursor cc =
//                ConversationCursor.create(mActivity, URI_COLUMN, uri, CONVO_PROJECTION);
//        ConversationProvider cp = ConversationCursor.sProvider;
//        Cursor uc = ConversationCursor.sUnderlyingCursor;
//        // First things first; cc & uc should both have 9 rows
//        assertEquals(9, cc.getCount());
//        assertEquals(9, uc.getCount());
//
//        // Get the uri's of our convos (the mock provider doesn't order rows)
//        String[] uris = new String[cc.getCount()];
//        int i = 0;
//        while (cc.moveToNext()) {
//            uris[i++] = cc.getString(URI_INDEX);
//        }
//
//        // Get a random uri (first will do)
//        String uriString = uris[0];
//        Uri ccUri = Uri.parse(uriString);
//        // It should have the authority of CP
//        assertEquals(ccUri.getAuthority(), ConversationProvider.AUTHORITY);
//
//        // Try deleting a row locally
//        cp.deleteLocal(Uri.parse(uris[4]));
//        assertEquals(8, cc.getCount());
//        assertEquals(9, uc.getCount());
    }
}

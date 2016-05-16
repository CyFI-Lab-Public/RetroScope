/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.providers.protos.mock;

import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


// TODO: Create a UiProviderTest, and change MockUiProviderTests to extend it.  This would allow us
// to share the validation logic when we add new UI providers.

public class MockUiProviderTests extends AndroidTestCase {

    private Set<String> mTraversedUris;
    private String mLogTag;

    @Override
    public void setUp() {
        mLogTag = LogTag.getLogTag();
    }

    @SmallTest
    public void testTraverseContentProvider() {

        // Get the starting Uri
        final Uri accountUri = MockUiProvider.getAccountsUri();

        mTraversedUris = new HashSet<String>();
        traverseUri(accountUri);
    }

    public void testGetFolders() {
        final Uri accountsUri = MockUiProvider.getAccountsUri();
        MockUiProvider provider = new MockUiProvider();
        Cursor cursor = provider.query(accountsUri, UIProvider.ACCOUNTS_PROJECTION, null, null,
                null);
        ArrayList<Uri> folderUris = new ArrayList<Uri>();
        if (cursor != null) {
            Uri foldersUri;
            int folderUriIndex = cursor.getColumnIndex(UIProvider.AccountColumns.FOLDER_LIST_URI);
            while (cursor.moveToNext()) {
                // Verify that we can get the folders URI.
                foldersUri = Uri.parse(cursor.getString(folderUriIndex));
                assertNotNull(foldersUri);
                folderUris.add(foldersUri);
            }
        }
        // Now, verify that we can get folders.
        ArrayList<Uri> childUris = new ArrayList<Uri>();
        ArrayList<Uri> convUris = new ArrayList<Uri>();
        int count = 0;
        for (Uri u : folderUris) {
            Cursor foldersCursor = provider.query(u, UIProvider.FOLDERS_PROJECTION, null, null,
                    null);
            assertNotNull(foldersCursor);
            assertEquals(foldersCursor.getCount(), 2);
            Uri childUri;
            Uri convUri;
            int name = foldersCursor.getColumnIndex(UIProvider.FolderColumns.NAME);
            int childColumnIndex = foldersCursor
                    .getColumnIndex(UIProvider.FolderColumns.CHILD_FOLDERS_LIST_URI);
            int convColumnIndex = foldersCursor
                    .getColumnIndex(UIProvider.FolderColumns.CONVERSATION_LIST_URI);
            int hasChildrenIndex = foldersCursor
                    .getColumnIndex(UIProvider.FolderColumns.HAS_CHILDREN);
            while (foldersCursor.moveToNext()) {
                switch (count) {
                    case 0:
                        assertEquals(foldersCursor.getString(name), "Folder zero");
                        childUri = Uri.parse(foldersCursor.getString(childColumnIndex));
                        assertNotNull(childUri);
                        childUris.add(childUri);
                        convUri = Uri.parse(foldersCursor.getString(convColumnIndex));
                        convUris.add(convUri);
                        assertEquals(foldersCursor.getInt(hasChildrenIndex), 1);
                        break;
                    case 1:
                        assertEquals(foldersCursor.getString(name), "Folder one");
                        assertEquals(foldersCursor.getInt(hasChildrenIndex), 0);
                        childUri = Uri.parse(foldersCursor.getString(childColumnIndex));
                        childUris.add(childUri);
                        convUri = Uri.parse(foldersCursor.getString(convColumnIndex));
                        convUris.add(convUri);
                        break;
                    case 2:
                        assertEquals(foldersCursor.getString(name), "Folder two");
                        assertEquals(foldersCursor.getInt(hasChildrenIndex), 0);
                        childUri = Uri.parse(foldersCursor.getString(childColumnIndex));
                        childUris.add(childUri);
                        convUri = Uri.parse(foldersCursor.getString(convColumnIndex));
                        convUris.add(convUri);
                        break;
                    case 3:
                        assertEquals(foldersCursor.getString(name), "Folder three");
                        assertEquals(foldersCursor.getInt(hasChildrenIndex), 0);
                        childUri = Uri.parse(foldersCursor.getString(childColumnIndex));
                        childUris.add(childUri);
                        convUri = Uri.parse(foldersCursor.getString(convColumnIndex));
                        convUris.add(convUri);
                        break;
                }
                count++;
            }
        }
        count = 0;
        for (Uri u : childUris) {
            Cursor childFoldersCursor = provider.query(u, UIProvider.FOLDERS_PROJECTION, null,
                    null, null);
            if (childFoldersCursor != null) {
                int name = childFoldersCursor.getColumnIndex(UIProvider.FolderColumns.NAME);
                while (childFoldersCursor.moveToNext()) {
                    switch (count) {
                        case 0:
                            assertEquals(childFoldersCursor.getString(name), "Folder zeroChild0");
                            break;
                        case 1:
                            assertEquals(childFoldersCursor.getString(name), "Folder zeroChild1");
                            break;
                    }
                    count++;
                }
            }
        }
        assertEquals(count, 2);
        count = 0;
        ArrayList<Uri> messageUris = new ArrayList<Uri>();
        for (Uri u : convUris) {
            Cursor convFoldersCursor = provider.query(u, UIProvider.CONVERSATION_PROJECTION, null,
                    null, null);
            if (convFoldersCursor != null) {
                int subject = UIProvider.CONVERSATION_SUBJECT_COLUMN;
                int messageUriCol = UIProvider.CONVERSATION_MESSAGE_LIST_URI_COLUMN;
                Uri messageUri;
                while (convFoldersCursor.moveToNext()) {
                    switch (count) {
                        case 0:
                            assertEquals(convFoldersCursor.getString(subject),
                                    "Conversation zeroConv0");
                            messageUri = Uri.parse(convFoldersCursor.getString(messageUriCol));
                            messageUris.add(messageUri);
                            break;
                        case 1:
                            assertEquals(convFoldersCursor.getString(subject),
                                    "Conversation zeroConv1");
                            messageUri = Uri.parse(convFoldersCursor.getString(messageUriCol));
                            messageUris.add(messageUri);
                            break;
                    }
                    count++;
                }
            }
        }
        assertEquals(count, 4);
        count = 0;
        ArrayList<Uri> attachmentUris = new ArrayList<Uri>();
        for (Uri u : messageUris) {
            Cursor messageCursor = provider.query(u, UIProvider.MESSAGE_PROJECTION, null, null,
                    null);
            if (messageCursor != null) {
                int subject = messageCursor.getColumnIndex(UIProvider.MessageColumns.SUBJECT);
                int attachmentUriCol = messageCursor
                        .getColumnIndex(UIProvider.MessageColumns.ATTACHMENT_LIST_URI);
                int hasAttachments = messageCursor
                        .getColumnIndex(UIProvider.MessageColumns.HAS_ATTACHMENTS);
                while (messageCursor.moveToNext()) {
                    switch (count) {
                        case 0:
                            assertEquals(messageCursor.getString(subject), "Message zeroConv0");
                            assertEquals(messageCursor.getInt(hasAttachments), 1);
                            attachmentUris
                                    .add(Uri.parse(messageCursor.getString(attachmentUriCol)));
                            count++;
                            break;
                        case 1:
                            assertEquals(messageCursor.getString(subject), "Message zeroConv1");
                            assertEquals(messageCursor.getInt(hasAttachments), 1);
                            attachmentUris
                                    .add(Uri.parse(messageCursor.getString(attachmentUriCol)));
                            count++;
                            break;
                    }
                }
            }
        }
        assertEquals(count, 2);
        count = 0;
        for (Uri u : attachmentUris) {
            Cursor attachmentCursor = provider.query(u, UIProvider.ATTACHMENT_PROJECTION, null,
                    null, null);
            if (attachmentCursor != null) {
                int name = attachmentCursor.getColumnIndex(UIProvider.AttachmentColumns.NAME);
                while (attachmentCursor.moveToNext()) {
                    switch (count) {
                        case 0:
                            assertEquals(attachmentCursor.getString(name), "Attachment zero");
                            break;
                        case 1:
                            assertEquals(attachmentCursor.getString(name), "Attachment one");
                            break;
                    }
                    count++;
                }
            }
        }
        assertEquals(count, 2);
    }

    private void traverseUri(Uri uri) {
        if (uri == null) {
            return;
        }

        LogUtils.i(mLogTag, "Traversing: %s", uri.toString());

        final ContentResolver resolver = getContext().getContentResolver();

        final Cursor cursor = resolver.query(uri, null, null, null, null);

        mTraversedUris.add(uri.toString());

        if (cursor != null) {
            try {
                // get the columns
                final String[] columns = cursor.getColumnNames();

                // Go through each of rows
                while (cursor.moveToNext()) {

                    // Go through each of the columns find the ones that returns uris
                    for (String columnName : columns) {
                        if (columnName.toLowerCase().contains("uri")) {
                            final String uriString =
                                    cursor.getString(cursor.getColumnIndex(columnName));

                            if (!mTraversedUris.contains(uriString)) {
                                final Uri childUri = Uri.parse(uriString);

                                traverseUri(childUri);
                            }
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        } else {
            // fail("query returned null");
            LogUtils.e(mLogTag, "query returned null: %s", uri.toString());
        }
    }


    /**
     * Test to add
     * 1) Make sure that the query result columns match the UIProvider schema
     * 2) Make sure the data is valid
     */
}
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

package android.content.cts;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData.Item;
import android.net.Uri;
import android.test.AndroidTestCase;

public class ClipboardManagerTest extends AndroidTestCase {

    private ClipboardManager mClipboardManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mClipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public void testSetGetText() {
        mClipboardManager.setText("Test Text 1");
        assertEquals("Test Text 1", mClipboardManager.getText());

        mClipboardManager.setText("Test Text 2");
        assertEquals("Test Text 2", mClipboardManager.getText());
    }

    public void testHasPrimaryClip() {
        if (mClipboardManager.hasPrimaryClip()) {
            assertNotNull(mClipboardManager.getPrimaryClip());
            assertNotNull(mClipboardManager.getPrimaryClipDescription());
        } else {
            assertNull(mClipboardManager.getPrimaryClip());
            assertNull(mClipboardManager.getPrimaryClipDescription());
        }

        mClipboardManager.setPrimaryClip(ClipData.newPlainText("Label", "Text"));
        assertTrue(mClipboardManager.hasPrimaryClip());
    }

    public void testSetPrimaryClip_plainText() {
        ClipData textData = ClipData.newPlainText("TextLabel", "Text");
        assertSetPrimaryClip(textData, "TextLabel",
                new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN},
                new ExpectedClipItem("Text", null, null));
    }

    public void testSetPrimaryClip_intent() {
        Intent intent = new Intent(mContext, ClipboardManagerTest.class);
        ClipData intentData = ClipData.newIntent("IntentLabel", intent);
        assertSetPrimaryClip(intentData, "IntentLabel",
                new String[] {ClipDescription.MIMETYPE_TEXT_INTENT},
                new ExpectedClipItem(null, intent, null));
    }

    public void testSetPrimaryClip_rawUri() {
        Uri uri = Uri.parse("http://www.google.com");
        ClipData uriData = ClipData.newRawUri("UriLabel", uri);
        assertSetPrimaryClip(uriData, "UriLabel",
                new String[] {ClipDescription.MIMETYPE_TEXT_URILIST},
                new ExpectedClipItem(null, null, uri));
    }

    public void testSetPrimaryClip_contentUri() {
        Uri contentUri = Uri.parse("content://cts/test/for/clipboardmanager");
        ClipData contentUriData = ClipData.newUri(getContext().getContentResolver(),
                "ContentUriLabel", contentUri);
        assertSetPrimaryClip(contentUriData, "ContentUriLabel",
                new String[] {ClipDescription.MIMETYPE_TEXT_URILIST},
                new ExpectedClipItem(null, null, contentUri));
    }

    public void testSetPrimaryClip_complexItem() {
        Intent intent = new Intent(mContext, ClipboardManagerTest.class);
        Uri uri = Uri.parse("http://www.google.com");
        ClipData multiData = new ClipData(new ClipDescription("ComplexItemLabel",
                new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN,
                        ClipDescription.MIMETYPE_TEXT_INTENT,
                        ClipDescription.MIMETYPE_TEXT_URILIST}),
                new Item("Text", intent, uri));
        assertSetPrimaryClip(multiData, "ComplexItemLabel",
                new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN,
                        ClipDescription.MIMETYPE_TEXT_INTENT,
                        ClipDescription.MIMETYPE_TEXT_URILIST},
                new ExpectedClipItem("Text", intent, uri));
    }

    public void testSetPrimaryClip_multipleItems() {
        Intent intent = new Intent(mContext, ClipboardManagerTest.class);
        Uri uri = Uri.parse("http://www.google.com");
        ClipData textData = ClipData.newPlainText("TextLabel", "Text");
        textData.addItem(new Item("More Text"));
        textData.addItem(new Item(intent));
        textData.addItem(new Item(uri));
        assertSetPrimaryClip(textData, "TextLabel",
                new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN},
                new ExpectedClipItem("Text", null, null),
                new ExpectedClipItem("More Text", null, null),
                new ExpectedClipItem(null, intent, null),
                new ExpectedClipItem(null, null, uri));
    }

    private class ExpectedClipItem {
        CharSequence mText;
        Intent mIntent;
        Uri mUri;

        ExpectedClipItem(CharSequence text, Intent intent, Uri uri) {
            mText = text;
            mIntent = intent;
            mUri = uri;
        }
    }

    private void assertSetPrimaryClip(ClipData clipData,
            String expectedLabel,
            String[] expectedMimeTypes,
            ExpectedClipItem... expectedClipItems) {

        mClipboardManager.setPrimaryClip(clipData);
        assertTrue(mClipboardManager.hasPrimaryClip());

        if (expectedClipItems != null
                && expectedClipItems.length > 0
                && expectedClipItems[0].mText != null) {
            assertTrue(mClipboardManager.hasText());
        } else {
            assertFalse(mClipboardManager.hasText());
        }

        assertNotNull(mClipboardManager.getPrimaryClip());
        assertNotNull(mClipboardManager.getPrimaryClipDescription());

        ClipData data = mClipboardManager.getPrimaryClip();
        if (expectedClipItems != null) {
            assertEquals(expectedClipItems.length, data.getItemCount());
            for (int i = 0; i < expectedClipItems.length; i++) {
                assertClipItem(expectedClipItems[i], data.getItemAt(i));
            }
        } else {
            throw new IllegalArgumentException("Should have at least one expectedClipItem...");
        }

        assertClipDescription(data.getDescription(),
                expectedLabel, expectedMimeTypes);

        assertClipDescription(mClipboardManager.getPrimaryClipDescription(),
                expectedLabel, expectedMimeTypes);
    }

    private void assertClipDescription(ClipDescription description, String expectedLabel,
            String... mimeTypes) {
        assertEquals(expectedLabel, description.getLabel());
        assertEquals(mimeTypes.length, description.getMimeTypeCount());
        int mimeTypeCount = description.getMimeTypeCount();
        for (int i = 0; i < mimeTypeCount; i++) {
            assertEquals(mimeTypes[i], description.getMimeType(i));
        }
    }

    private void assertClipItem(ExpectedClipItem expectedItem, Item item) {
        assertEquals(expectedItem.mText, item.getText());
        if (expectedItem.mIntent != null) {
            assertNotNull(item.getIntent());
        } else {
            assertNull(item.getIntent());
        }
        if (expectedItem.mUri != null) {
            assertEquals(expectedItem.mUri.toString(), item.getUri().toString());
        } else {
            assertNull(item.getUri());
        }
    }
}

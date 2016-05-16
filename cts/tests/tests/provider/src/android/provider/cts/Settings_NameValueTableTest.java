/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.provider.cts;


import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.NameValueTable;
import android.test.AndroidTestCase;

public class Settings_NameValueTableTest extends AndroidTestCase {
    public void testPutString() {
        ContentResolver cr = mContext.getContentResolver();
        Uri uri = Settings.System.CONTENT_URI;
        String name = "name1";
        String value = "value1";

        // before putString
        Cursor c = cr.query(uri, null, null, null, null);
        try {
            assertNotNull(c);
            int origCount = c.getCount();
            c.close();

            MyNameValueTable.putString(cr, uri, name, value);
            c = cr.query(uri, null, null, null, null);
            assertNotNull(c);
            assertEquals(origCount + 1, c.getCount());
            c.close();

            // query this row
            String selection = NameValueTable.NAME + "=\"" + name + "\"";
            c = cr.query(uri, null, selection, null, null);
            assertNotNull(c);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals("name1", c.getString(c.getColumnIndexOrThrow(NameValueTable.NAME)));
            assertEquals("value1", c.getString(c.getColumnIndexOrThrow(NameValueTable.VALUE)));
            c.close();

            // delete this row
            cr.delete(uri, selection, null);
            c = cr.query(uri, null, null, null, null);
            assertNotNull(c);
            assertEquals(origCount, c.getCount());
        } finally {
            // TODO should clean up more better
            c.close();
        }
    }

    public void testGetUriFor() {
        Uri uri = Uri.parse("content://authority/path");
        String name = "table";

        Uri res = NameValueTable.getUriFor(uri, name);
        assertNotNull(res);
        assertEquals(Uri.withAppendedPath(uri, name), res);
    }

    private static class MyNameValueTable extends NameValueTable {
        protected static boolean putString(ContentResolver resolver, Uri uri, String name,
                String value) {
            return NameValueTable.putString(resolver, uri, name, value);
        }
    }
}

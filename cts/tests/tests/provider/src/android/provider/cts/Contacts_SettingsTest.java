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
import android.provider.Contacts.Settings;
import android.test.InstrumentationTestCase;

public class Contacts_SettingsTest extends InstrumentationTestCase {
    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
    }

    public void testAccessSetting() {
        String key1 = "key 1";
        String value1 = "value 1";
        String key2 = "key 2";
        String value2 = "value 2";
        Settings.setSetting(mContentResolver, "account", key1, value1);
        Settings.setSetting(mContentResolver, "account", key2, value2);
        assertEquals(value1, Settings.getSetting(mContentResolver, "account", key1));
        assertEquals(value2, Settings.getSetting(mContentResolver, "account", key2));
        assertNull(Settings.getSetting(mContentResolver, "account", "key not exist"));

        Settings.setSetting(mContentResolver, "account", key1, value2);
        assertEquals(value2, Settings.getSetting(mContentResolver, "account", key1));
    }
}

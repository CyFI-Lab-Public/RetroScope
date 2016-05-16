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
import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.test.AndroidTestCase;

public class Settings_SecureTest extends AndroidTestCase {

    private static final String NO_SUCH_SETTING = "NoSuchSetting";

    /**
     * Setting that will have a string value to trigger SettingNotFoundException caused by
     * NumberFormatExceptions for getInt, getFloat, and getLong.
     */
    private static final String STRING_VALUE_SETTING = Secure.ANDROID_ID;

    private ContentResolver cr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        cr = mContext.getContentResolver();
        assertNotNull(cr);
        assertSettingsForTests();
    }

    /** Check that the settings that will be used for testing have proper values. */
    private void assertSettingsForTests() {
        assertNull(Secure.getString(cr, NO_SUCH_SETTING));

        String value = Secure.getString(cr, STRING_VALUE_SETTING);
        assertNotNull(value);
        try {
            Integer.parseInt(value);
            fail("Shouldn't be able to parse this setting's value for later tests.");
        } catch (NumberFormatException expected) {
        }
    }

    public void testGetDefaultValues() {
        assertEquals(10, Secure.getInt(cr, "int", 10));
        assertEquals(20, Secure.getLong(cr, "long", 20));
        assertEquals(30.0f, Secure.getFloat(cr, "float", 30), 0.001);
    }

    public void testGetPutInt() {
        assertNull(Secure.getString(cr, NO_SUCH_SETTING));

        try {
            Secure.putInt(cr, NO_SUCH_SETTING, -1);
            fail("SecurityException should have been thrown!");
        } catch (SecurityException expected) {
        }

        try {
            Secure.getInt(cr, NO_SUCH_SETTING);
            fail("SettingNotFoundException should have been thrown!");
        } catch (SettingNotFoundException expected) {
        }

        try {
            Secure.getInt(cr, STRING_VALUE_SETTING);
            fail("SettingNotFoundException should have been thrown!");
        } catch (SettingNotFoundException expected) {
        }
    }

    public void testGetPutFloat() throws SettingNotFoundException {
        assertNull(Secure.getString(cr, NO_SUCH_SETTING));

        try {
            Secure.putFloat(cr, NO_SUCH_SETTING, -1);
            fail("SecurityException should have been thrown!");
        } catch (SecurityException expected) {
        }

        try {
            Secure.getFloat(cr, NO_SUCH_SETTING);
            fail("SettingNotFoundException should have been thrown!");
        } catch (SettingNotFoundException expected) {
        }

        try {
            Secure.getFloat(cr, STRING_VALUE_SETTING);
            fail("SettingNotFoundException should have been thrown!");
        } catch (SettingNotFoundException expected) {
        }
    }

    public void testGetPutLong() {
        assertNull(Secure.getString(cr, NO_SUCH_SETTING));

        try {
            Secure.putLong(cr, NO_SUCH_SETTING, -1);
            fail("SecurityException should have been thrown!");
        } catch (SecurityException expected) {
        }

        try {
            Secure.getLong(cr, NO_SUCH_SETTING);
            fail("SettingNotFoundException should have been thrown!");
        } catch (SettingNotFoundException expected) {
        }

        try {
            Secure.getLong(cr, STRING_VALUE_SETTING);
            fail("SettingNotFoundException should have been thrown!");
        } catch (SettingNotFoundException expected) {
        }
    }

    public void testGetPutString() {
        assertNull(Secure.getString(cr, NO_SUCH_SETTING));

        try {
            Secure.putString(cr, NO_SUCH_SETTING, "-1");
            fail("SecurityException should have been thrown!");
        } catch (SecurityException expected) {
        }

        assertNotNull(Secure.getString(cr, STRING_VALUE_SETTING));

        assertNull(Secure.getString(cr, NO_SUCH_SETTING));
    }

    public void testGetUriFor() {
        String name = "table";

        Uri uri = Secure.getUriFor(name);
        assertNotNull(uri);
        assertEquals(Uri.withAppendedPath(Secure.CONTENT_URI, name), uri);
    }

    public void testUnknownSourcesOffByDefault() throws SettingNotFoundException {
        assertEquals("Device should not ship with 'Unknown Sources' enabled by default.",
                0, Settings.Global.getInt(cr, Settings.Global.INSTALL_NON_MARKET_APPS));
    }
}

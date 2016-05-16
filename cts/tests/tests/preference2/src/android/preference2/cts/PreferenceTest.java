/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.preference2.cts;

import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.test.ActivityInstrumentationTestCase2;

import com.android.cts.preference2.R;

public class PreferenceTest
        extends ActivityInstrumentationTestCase2<PreferenceFromCodeActivity> {

    private PreferenceFromCodeActivity mActivity;
    private Preference mPreference;

    public PreferenceTest() {
        super(PreferenceFromCodeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mPreference = (CheckBoxPreference) mActivity.findPreference(
                "checkbox_preference");
    }

    public void testNotNull() {
        assertNotNull(mPreference);
    }

    public void testGetSummary() {
        String summary = (String) mPreference.getSummary();
        String summaryExp = mActivity.getResources().getString(
                R.string.summary_checkbox_preference);
        assertEquals(summaryExp, summary);
    }

    public void testSetSummary() throws Throwable {
        final String summary = "New Summary";
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mPreference.setSummary(summary);
            }
        });
        assertEquals(summary, mPreference.getSummary());
    }

    public void testIsEnabled() {
        assertTrue(mPreference.isEnabled());
    }

    public void testSetEnabled() throws Throwable {
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mPreference.setEnabled(false);
            }
        });
        assertFalse(mPreference.isEnabled());
    }

    public void testIsPersistent() {
        assertTrue(mPreference.isPersistent());
    }

    public void testIsSelectable() {
        assertTrue(mPreference.isSelectable());
    }

    public void testGetTitle() {
        CharSequence title= mPreference.getTitle();
        String titleExp = mActivity.getResources().getString(
                R.string.title_checkbox_preference);
        assertEquals(titleExp, title);
    }

    public void testSetTitle() throws Throwable {
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mPreference.setTitle(R.string.title_my_preference);
            }
        });
        String titleExp = mActivity.getResources().getString(
                R.string.title_my_preference);
        String title = (String) mPreference.getTitle();
        assertEquals(titleExp, title);
    }

    public void testHasKey() {
        assertTrue(mPreference.hasKey());
    }

    public void testGetKey() {
        String key = mPreference.getKey();
        String keyExp = "checkbox_preference";
        assertEquals(keyExp, key);
    }

    public void testSetKey() throws Throwable {
        final String key = "key1";
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mPreference.setKey(key);
            }
        });
        assertEquals(key, mPreference.getKey());
    }

     public void testGetPreferenceManager() {
        assertNotNull(mPreference.getPreferenceManager());
    }

    public void testGetSharedPreferences() {
        assertNotNull(mPreference.getSharedPreferences());
    }

    public void testSetTitle_CharSequence() throws Throwable {
        final CharSequence titleExp = "Test Title";
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mPreference.setTitle(titleExp);
            }
        });

        assertEquals(titleExp, mPreference.getTitle());
    }

    public void testGetContext() {
        assertNotNull(mPreference.getContext());
    }

    public void testGetOrder() throws Throwable {
        int order = mPreference.getOrder();
        assertEquals(0, order);
    }

    public void testSetOrder() throws Throwable {
        int orderExp = 1;
        mPreference.setOrder(orderExp);
        int order = mPreference.getOrder();
        assertEquals(orderExp, order);
    }

    public void testGetIcon() {
        Drawable iconExp = mActivity.getResources().getDrawable(R.drawable.ic_launcher);
        Drawable icon = mPreference.getIcon();
        assertEquals(iconExp.getIntrinsicHeight(), icon.getIntrinsicHeight());
        assertEquals(iconExp.getIntrinsicWidth(), icon.getIntrinsicWidth());
        assertEquals(iconExp.getOpacity(), icon.getOpacity());
    }

    public void testgetShouldDisableView() {
        assertTrue(mPreference.getShouldDisableView());
    }

    public void testSetShouldDisableView() throws Throwable {
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mPreference.setShouldDisableView(false);
            }
        });
        assertFalse(mPreference.getShouldDisableView());
    }

    public void testPersistInt() {
        CustomPreference mCustomPreference = new CustomPreference(mActivity);
        String key = "" + Math.random();
        mCustomPreference.setKey(key);
        PreferenceGroup mPreferenceGroup = (PreferenceGroup) mActivity.findPreference(
                "pref-group");
        mPreferenceGroup.addPreference(mCustomPreference);
        try {
            int expected = 1;
            mCustomPreference.persistInt(expected);
            int actual = mCustomPreference.getPersistedInt(0);
            assertEquals(expected, actual);
        } finally {
            mPreferenceGroup.removePreference(mCustomPreference);
        }
    }

    public void testPersistBoolean() {
        CustomPreference mCustomPreference = new CustomPreference(mActivity);
        String key = "" + Math.random();
        mCustomPreference.setKey(key);
        PreferenceGroup mPreferenceGroup = (PreferenceGroup) mActivity.findPreference(
                "pref-group");
        mPreferenceGroup.addPreference(mCustomPreference);
        try {
            boolean expected = true;
            boolean result = mCustomPreference.persistBoolean(expected);
            assertTrue(result);
            boolean actual = mCustomPreference.getPersistedBoolean(false);
            assertEquals(expected, actual);
        } finally {
            mPreferenceGroup.removePreference(mCustomPreference);
        }
    }

    public void testPersistString() {
        CustomPreference mCustomPreference = new CustomPreference(mActivity);
        String key = "" + Math.random();
        mCustomPreference.setKey(key);
        PreferenceGroup mPreferenceGroup = (PreferenceGroup) mActivity.findPreference(
                "pref-group");
        mPreferenceGroup.addPreference(mCustomPreference);
        try {
            String expected = "a";
            boolean result = mCustomPreference.persistString(expected);
            assertTrue(result);
            String actual = mCustomPreference.getPersistedString("b");
            assertEquals(expected, actual);
        } finally {
            mPreferenceGroup.removePreference(mCustomPreference);
        }
    }

    public void testPersistFloat() {
        CustomPreference mCustomPreference = new CustomPreference(mActivity);
        String key = "" + Math.random();
        mCustomPreference.setKey(key);
        PreferenceGroup mPreferenceGroup = (PreferenceGroup) mActivity.findPreference(
                "pref-group");
        mPreferenceGroup.addPreference(mCustomPreference);
        try {
            float expected = 9.999f;
            boolean result = mCustomPreference.persistFloat(expected);
            assertTrue(result);
            float actual = mCustomPreference.getPersistedFloat(0.000f);
            assertEquals(expected, actual);
        } finally {
            mPreferenceGroup.removePreference(mCustomPreference);
        }
    }

    public void testPersistLong() {
        CustomPreference mCustomPreference = new CustomPreference(mActivity);
        String key = "" + Math.random();
        mCustomPreference.setKey(key);
        PreferenceGroup mPreferenceGroup = (PreferenceGroup) mActivity.findPreference(
        "pref-group");
        mPreferenceGroup.addPreference(mCustomPreference);
        try {
            long expected = 99999999l;
            boolean result = mCustomPreference.persistLong(expected);
            assertTrue(result);
            long actual = mCustomPreference.getPersistedLong(10000000l);
            assertEquals(expected, actual);
        } finally {
            mPreferenceGroup.removePreference(mCustomPreference);
        }
    }

    public void testShouldCommit() {
        CustomPreference mCustomPreference = (CustomPreference) mActivity.findPreference(
                "custom-preference");
        assertTrue(mCustomPreference.shouldCommit());
    }

    public void testShouldPersist() {
        CustomPreference mCustomPreference = (CustomPreference) mActivity.findPreference(
                "custom-preference");
        assertTrue(mCustomPreference.shouldPersist());
    }
}


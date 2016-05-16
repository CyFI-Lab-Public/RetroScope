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

import com.android.cts.preference2.R;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference2.cts.PreferenceFragmentActivity.PrefFragment;
import android.test.ActivityInstrumentationTestCase2;

public class PreferenceGroupTest
        extends ActivityInstrumentationTestCase2<PreferenceFromCodeActivity> {

    private PreferenceFromCodeActivity mActivity;
    private PreferenceGroup mPreferenceGroup;

    public PreferenceGroupTest() {
        super(PreferenceFromCodeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mPreferenceGroup = (PreferenceGroup) mActivity.findPreference("pref-group");
        CheckBoxPreference checkboxPref = new CheckBoxPreference(mActivity);
        checkboxPref.setKey("checkbox_preference_group");
        checkboxPref.setIcon(R.drawable.ic_launcher);
        checkboxPref.setTitle(R.string.title_checkbox_preference);
        checkboxPref.setSummary(R.string.summary_checkbox_preference);
        checkboxPref.setSummaryOn(R.string.summary_on_checkbox_preference);
        checkboxPref.setSummaryOff(R.string.summary_off_checkbox_preference);
        checkboxPref.setChecked(true);
        mPreferenceGroup.addPreference(checkboxPref);
    }

    public void testAddPreference() {
        Preference preference = mActivity.findPreference("pref-group");
        assertNotNull(preference);
    }

    public void testFindPreference() {
        Preference preference = mPreferenceGroup.findPreference("checkbox_preference_group");
        assertNotNull(preference);
    }

    public void testPreferenceCount() {
        assertEquals(1, mPreferenceGroup.getPreferenceCount());
    }

    public void testSetOrderingAsAdded() {
        mPreferenceGroup.setOrderingAsAdded(false);
        assertFalse(mPreferenceGroup.isOrderingAsAdded());
    }

    public void testRemoveAll() {
        mPreferenceGroup.removeAll();
        assertEquals(0, mPreferenceGroup.getPreferenceCount());
    }

    public void testRemovePreference() {
        Preference preference = mPreferenceGroup.findPreference("checkbox_preference_group");
        mPreferenceGroup.removePreference(preference);
        assertEquals(0, mPreferenceGroup.getPreferenceCount());
    }

    public void testIsOnSameScreenAsChildren() {
        CustomPreferenceGroup mCustomPreferenceGroup = (CustomPreferenceGroup) 
                mActivity.findPreference("custom-pref-group");
        assertTrue(mCustomPreferenceGroup.isOnSameScreenAsChildren());
    }

    public void testOnPrepareAddPreference( ) {
        CustomPreferenceGroup mCustomPreferenceGroup = (CustomPreferenceGroup) 
                mActivity.findPreference("custom-pref-group");
        CheckBoxPreference checkboxPref = new CheckBoxPreference(mActivity);
        checkboxPref.setKey("checkbox_preference-2");
        mCustomPreferenceGroup.addPreference(checkboxPref);
        assertTrue(mCustomPreferenceGroup.mOnPrepareCalled);
    }
}

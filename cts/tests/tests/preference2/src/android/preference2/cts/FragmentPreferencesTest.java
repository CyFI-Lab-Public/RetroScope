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

import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference2.cts.FragmentPreferences.PrefsFragment;
import android.test.ActivityInstrumentationTestCase2;

public class FragmentPreferencesTest
        extends ActivityInstrumentationTestCase2<FragmentPreferences> {

    private FragmentPreferences mActivity;
    private PrefsFragment mPrefsFragment;

    public FragmentPreferencesTest() {
        super(FragmentPreferences.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mPrefsFragment = (PrefsFragment) mActivity.getFragmentManager().
                findFragmentById(android.R.id.content);
    }

    public void testGetPreferenceManager() {
        PreferenceManager prefManager = mPrefsFragment.getPreferenceManager();
        assertNotNull(prefManager);
    }

    public void testGetPreferenceScreen() {
        PreferenceScreen prefScreen = mPrefsFragment.getPreferenceScreen();
        assertNotNull(prefScreen);
    }

    public void testFindPreference() {
        Preference pref = mPrefsFragment.findPreference("checkbox_preference");
        assertNotNull(pref);
    }

    public void testSetPreferenceScreen() {
        mPrefsFragment.setPreferenceScreen(null);
        assertNull(mPrefsFragment.getPreferenceScreen());
    }
}

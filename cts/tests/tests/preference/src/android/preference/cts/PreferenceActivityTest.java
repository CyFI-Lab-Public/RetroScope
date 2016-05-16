/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.preference.cts;

import com.android.cts.stub.R;

import android.content.Intent;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.test.ActivityInstrumentationTestCase2;

public class PreferenceActivityTest
        extends ActivityInstrumentationTestCase2<PreferenceStubActivity> {

    private PreferenceActivity mActivity;

    public PreferenceActivityTest() {
        super(PreferenceStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testAddPreferencesFromIntent() {
        assertNull(mActivity.findPreference("check_box_preference_from_intent"));

        Intent intent = new Intent(mActivity, PreferenceStubActivity.class);
        mActivity.addPreferencesFromIntent(intent);

        assertNotNull(mActivity.findPreference("check_box_preference_from_intent"));
    }

    public void testAddPreferencesFromResource() {
        assertNull(mActivity.findPreference("check_box_preference_from_resource"));

        mActivity.addPreferencesFromResource(R.xml.preferences_from_resource);

        assertNotNull(mActivity.findPreference("check_box_preference_from_resource"));
    }

    public void testGetPreferenceManager() {
        assertNotNull(mActivity.getPreferenceManager());
    }

    public void testGetPreferenceScreen() {
        assertNull(mActivity.getPreferenceScreen());

        PreferenceManager prefManager = mActivity.getPreferenceManager();
        PreferenceScreen prefScreen = prefManager.createPreferenceScreen(mActivity);
        mActivity.setPreferenceScreen(prefScreen);

        assertEquals(prefScreen, mActivity.getPreferenceScreen());
    }
}
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

import android.preference.CheckBoxPreference;
import android.test.ActivityInstrumentationTestCase2;

import com.android.cts.preference2.R;

public class CheckBoxPreferenceTest
        extends ActivityInstrumentationTestCase2<PreferenceFromCodeActivity> {

    private PreferenceFromCodeActivity mActivity;

    public CheckBoxPreferenceTest() {
        super(PreferenceFromCodeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testNotNull() {
        CheckBoxPreference checkBoxPref = (CheckBoxPreference) mActivity.findPreference(
                    "checkbox_preference");
        assertNotNull(checkBoxPref);
    }

    public void testGetSummary() {
        CheckBoxPreference checkBoxPref = (CheckBoxPreference) mActivity.findPreference(
                    "checkbox_preference");
        String summary = (String) checkBoxPref.getSummary();
        String summaryExp = mActivity.getResources().getString(
            R.string.summary_checkbox_preference);
        assertEquals(summaryExp, summary);
    }
}

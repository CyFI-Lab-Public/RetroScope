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

import android.preference.ListPreference;
import android.test.ActivityInstrumentationTestCase2;

import com.android.cts.preference2.R;

public class ListPreferenceTest
        extends ActivityInstrumentationTestCase2<PreferenceFromCodeActivity> {

    private PreferenceFromCodeActivity mActivity;
    private ListPreference mListPref;

    public ListPreferenceTest() {
        super(PreferenceFromCodeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mListPref = (ListPreference) mActivity.findPreference("list_preference");
    }

    public void testGetEntries() {
        String[] entries = convertToStringArray((CharSequence[]) mListPref.getEntries());
        String[] entriesExp = mActivity.getResources().getStringArray(
                R.array.entries_list_preference);
        compareArrays(entriesExp, entries);
    }

    public void testGetEntryValues() {
        String[] entryValues = convertToStringArray((CharSequence[]) mListPref.getEntryValues());
        String[] entryValuesExp = mActivity.getResources().getStringArray(
                R.array.entryvalues_list_preference);
        compareArrays(entryValuesExp,entryValues);
    }

    public void testIsEnabled() {
        boolean isEnabled = mListPref.isEnabled();
        assertTrue(isEnabled);
    }

    private synchronized String[] convertToStringArray(CharSequence[] array){
        String[] strArray = new String[array.length];
        for(int i = 0; i < array.length; i++){
            strArray[i] = (String) array[i];
        }
        return strArray;
    }

    private void compareArrays(String[] firstArray,String[] secArray){
        assertEquals(firstArray.length, secArray.length);
        for(int i = 0; i < firstArray.length; i++) {
            assertEquals(firstArray[i], secArray[i]);
        }
    }
}

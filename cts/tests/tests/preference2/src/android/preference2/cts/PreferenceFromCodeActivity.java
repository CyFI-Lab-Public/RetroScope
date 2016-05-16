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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

public class PreferenceFromCodeActivity extends PreferenceActivity {
    private Preference mPref;
    protected PreferenceScreen mPrefScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefScreen = getPreferenceManager().createPreferenceScreen(this);
        addPreferenceCategory();
        setPreferenceScreen(mPrefScreen);
    }

    private void addPreferenceCategory() {
        PreferenceCategory prefCat = new PreferenceCategory(this);
        prefCat.setTitle(R.string.inline_preferences);
        prefCat.setKey("pref-1");
        mPrefScreen.addPreference(prefCat);

        //CheckBox Preference
        CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
        checkboxPref.setKey("checkbox_preference");
        checkboxPref.setIcon(R.drawable.ic_launcher);
        checkboxPref.setTitle(R.string.title_checkbox_preference);
        checkboxPref.setSummary(R.string.summary_checkbox_preference);
        checkboxPref.setSummaryOn(R.string.summary_on_checkbox_preference);
        checkboxPref.setSummaryOff(R.string.summary_off_checkbox_preference);
        checkboxPref.setChecked(true);
        checkboxPref.shouldDisableDependents();
        prefCat.addPreference(checkboxPref);

        // Switch preference
        SwitchPreference switchPref = new SwitchPreference(this);
        switchPref.setKey("switch_preference");
        switchPref.setTitle(R.string.title_switch_preference);
        switchPref.setSummary(R.string.summary_switch_preference);
        switchPref.setSummaryOn(R.string.summary_on_switch_preference);
        switchPref.setSummaryOff(R.string.summary_off_switch_preference);
        switchPref.setSwitchTextOff(R.string.switchtext_off);
        switchPref.setSwitchTextOn(R.string.switchtext_on);
        prefCat.addPreference(switchPref);

        SwitchPreference switchPref2 = new SwitchPreference(this);
        switchPref2.setKey("switch_preference_2");
        switchPref2.setTitle(R.string.title_switch_preference);
        switchPref2.setSummary(R.string.summary_switch_preference);
        switchPref2.setSummaryOn(R.string.summary_on_switch_preference);
        switchPref2.setSummaryOff(R.string.summary_off_switch_preference);
        prefCat.addPreference(switchPref2);

        // Dialog based preferences
        PreferenceCategory dialogBasedPrefCat = new PreferenceCategory(this);
        dialogBasedPrefCat.setTitle(R.string.dialog_based_preferences);
        mPrefScreen.addPreference(dialogBasedPrefCat);

        // Edit text preference
        EditTextPreference editTextPref = new EditTextPreference(this);
        editTextPref.setDialogTitle(R.string.dialog_title_edittext_preference);
        editTextPref.setKey("edittext_preference");
        editTextPref.setTitle(R.string.title_edittext_preference);
        editTextPref.setSummary(R.string.summary_edittext_preference);
        editTextPref.setText(getResources().getString(R.string.text_edittext_preference));
        dialogBasedPrefCat.addPreference(editTextPref);

        EditTextPreference dialogPref = new EditTextPreference(this);
        dialogPref.setDialogTitle(R.string.dialogtitle_dialog_preference);
        dialogPref.setKey("dialog_preference");
        dialogPref.setTitle(R.string.title_dialog_preference);
        dialogPref.setSummary(R.string.summary_dialog_preference);
        dialogPref.setDialogIcon(R.drawable.ic_launcher);
        dialogPref.setDialogMessage(R.string.dialog_message);
        dialogPref.setNegativeButtonText(R.string.negative_button_text);
        dialogPref.setPositiveButtonText(R.string.positive_button_text);

        dialogBasedPrefCat.addPreference(dialogPref);

        ListPreference listPref = new ListPreference(this);
        listPref.setEntries(R.array.entries_list_preference);
        listPref.setEntryValues(R.array.entryvalues_list_preference);
        listPref.setDialogTitle(R.string.dialog_title_list_preference);
        listPref.setKey("list_preference");
        listPref.setTitle(R.string.title_list_preference);
        listPref.setSummary(R.string.summary_list_preference);
        dialogBasedPrefCat.addPreference(listPref);

        PreferenceGroup prefGroup = new PreferenceCategory(this);
        prefGroup.setTitle(R.string.preference_group_title);
        prefGroup.setKey("pref-group");
        mPrefScreen.addPreference(prefGroup);

        CustomPreferenceGroup customPrefGroup = new CustomPreferenceGroup(this);
        customPrefGroup.setTitle(R.string.custom_preference_group_title);
        customPrefGroup.setKey("custom-pref-group");
        mPrefScreen.addPreference(customPrefGroup);

        CustomPreference customPreference = new CustomPreference(this);
        customPreference .setTitle(R.string.custom_preference);
        customPreference.setKey("custom-preference");
        mPrefScreen.addPreference(customPreference);
    }
}

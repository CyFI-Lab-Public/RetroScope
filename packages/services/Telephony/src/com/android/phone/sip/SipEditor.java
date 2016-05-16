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

package com.android.phone.sip;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.R;
import com.android.phone.SipUtil;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * The activity class for editing a new or existing SIP profile.
 */
public class SipEditor extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_DISCARD = Menu.FIRST + 1;
    private static final int MENU_REMOVE = Menu.FIRST + 2;

    private static final String TAG = SipEditor.class.getSimpleName();
    private static final String KEY_PROFILE = "profile";
    private static final String GET_METHOD_PREFIX = "get";
    private static final char SCRAMBLED = '*';
    private static final int NA = 0;

    private PrimaryAccountSelector mPrimaryAccountSelector;
    private AdvancedSettings mAdvancedSettings;
    private SipSharedPreferences mSharedPreferences;
    private boolean mDisplayNameSet;
    private boolean mHomeButtonClicked;
    private boolean mUpdateRequired;
    private boolean mUpdatedFieldIsEmpty;

    private SipManager mSipManager;
    private SipProfileDb mProfileDb;
    private SipProfile mOldProfile;
    private CallManager mCallManager;
    private Button mRemoveButton;

    enum PreferenceKey {
        Username(R.string.username, 0, R.string.default_preference_summary),
        Password(R.string.password, 0, R.string.default_preference_summary),
        DomainAddress(R.string.domain_address, 0, R.string.default_preference_summary),
        DisplayName(R.string.display_name, 0, R.string.display_name_summary),
        ProxyAddress(R.string.proxy_address, 0, R.string.optional_summary),
        Port(R.string.port, R.string.default_port, R.string.default_port),
        Transport(R.string.transport, R.string.default_transport, NA),
        SendKeepAlive(R.string.send_keepalive, R.string.sip_system_decide, NA),
        AuthUserName(R.string.auth_username, 0, R.string.optional_summary);

        final int text;
        final int initValue;
        final int defaultSummary;
        Preference preference;

        /**
         * @param key The key name of the preference.
         * @param initValue The initial value of the preference.
         * @param defaultSummary The default summary value of the preference
         *        when the preference value is empty.
         */
        PreferenceKey(int text, int initValue, int defaultSummary) {
            this.text = text;
            this.initValue = initValue;
            this.defaultSummary = defaultSummary;
        }

        String getValue() {
            if (preference instanceof EditTextPreference) {
                return ((EditTextPreference) preference).getText();
            } else if (preference instanceof ListPreference) {
                return ((ListPreference) preference).getValue();
            }
            throw new RuntimeException("getValue() for the preference " + this);
        }

        void setValue(String value) {
            if (preference instanceof EditTextPreference) {
                String oldValue = getValue();
                ((EditTextPreference) preference).setText(value);
                if (this != Password) {
                    Log.v(TAG, this + ": setValue() " + value + ": " + oldValue
                            + " --> " + getValue());
                }
            } else if (preference instanceof ListPreference) {
                ((ListPreference) preference).setValue(value);
            }

            if (TextUtils.isEmpty(value)) {
                preference.setSummary(defaultSummary);
            } else if (this == Password) {
                preference.setSummary(scramble(value));
            } else if ((this == DisplayName)
                    && value.equals(getDefaultDisplayName())) {
                preference.setSummary(defaultSummary);
            } else {
                preference.setSummary(value);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mHomeButtonClicked = false;
        if (mCallManager.getState() != PhoneConstants.State.IDLE) {
            mAdvancedSettings.show();
            getPreferenceScreen().setEnabled(false);
            if (mRemoveButton != null) mRemoveButton.setEnabled(false);
        } else {
            getPreferenceScreen().setEnabled(true);
            if (mRemoveButton != null) mRemoveButton.setEnabled(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "start profile editor");
        super.onCreate(savedInstanceState);

        mSipManager = SipManager.newInstance(this);
        mSharedPreferences = new SipSharedPreferences(this);
        mProfileDb = new SipProfileDb(this);
        mCallManager = CallManager.getInstance();

        setContentView(R.layout.sip_settings_ui);
        addPreferencesFromResource(R.xml.sip_edit);

        SipProfile p = mOldProfile = (SipProfile) ((savedInstanceState == null)
                ? getIntent().getParcelableExtra(SipSettings.KEY_SIP_PROFILE)
                : savedInstanceState.getParcelable(KEY_PROFILE));

        PreferenceGroup screen = (PreferenceGroup) getPreferenceScreen();
        for (int i = 0, n = screen.getPreferenceCount(); i < n; i++) {
            setupPreference(screen.getPreference(i));
        }

        if (p == null) {
            screen.setTitle(R.string.sip_edit_new_title);
        }

        mAdvancedSettings = new AdvancedSettings();
        mPrimaryAccountSelector = new PrimaryAccountSelector(p);

        loadPreferencesFromProfile(p);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "SipEditor onPause(): finishing? " + isFinishing());
        if (!isFinishing()) {
            mHomeButtonClicked = true;
            validateAndSetResult();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_DISCARD, 0, R.string.sip_menu_discard)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, MENU_SAVE, 0, R.string.sip_menu_save)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, MENU_REMOVE, 0, R.string.remove_sip_account)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem removeMenu = menu.findItem(MENU_REMOVE);
        removeMenu.setVisible(mOldProfile != null);
        menu.findItem(MENU_SAVE).setEnabled(mUpdateRequired);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                validateAndSetResult();
                return true;

            case MENU_DISCARD:
                finish();
                return true;

            case MENU_REMOVE: {
                setRemovedProfileAndFinish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                validateAndSetResult();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void saveAndRegisterProfile(SipProfile p) throws IOException {
        if (p == null) return;
        mProfileDb.saveProfile(p);
        if (p.getAutoRegistration()
                || mSharedPreferences.isPrimaryAccount(p.getUriString())) {
            try {
                mSipManager.open(p, SipUtil.createIncomingCallPendingIntent(),
                        null);
            } catch (Exception e) {
                Log.e(TAG, "register failed: " + p.getUriString(), e);
            }
        }
    }

    private void deleteAndUnregisterProfile(SipProfile p) {
        if (p == null) return;
        mProfileDb.deleteProfile(p);
        unregisterProfile(p.getUriString());
    }

    private void unregisterProfile(String uri) {
        try {
            mSipManager.close(uri);
        } catch (Exception e) {
            Log.e(TAG, "unregister failed: " + uri, e);
        }
    }

    private void setRemovedProfileAndFinish() {
        Intent intent = new Intent(this, SipSettings.class);
        setResult(RESULT_FIRST_USER, intent);
        Toast.makeText(this, R.string.removing_account, Toast.LENGTH_SHORT)
                .show();
        replaceProfile(mOldProfile, null);
        // do finish() in replaceProfile() in a background thread
    }

    private void showAlert(Throwable e) {
        String msg = e.getMessage();
        if (TextUtils.isEmpty(msg)) msg = e.toString();
        showAlert(msg);
    }

    private void showAlert(final String message) {
        if (mHomeButtonClicked) {
            Log.v(TAG, "Home button clicked, don't show dialog: " + message);
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(SipEditor.this)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(message)
                        .setPositiveButton(R.string.alert_dialog_ok, null)
                        .show();
            }
        });
    }

    private boolean isEditTextEmpty(PreferenceKey key) {
        EditTextPreference pref = (EditTextPreference) key.preference;
        return TextUtils.isEmpty(pref.getText())
                || pref.getSummary().equals(getString(key.defaultSummary));
    }

    private void validateAndSetResult() {
        boolean allEmpty = true;
        CharSequence firstEmptyFieldTitle = null;
        for (PreferenceKey key : PreferenceKey.values()) {
            Preference p = key.preference;
            if (p instanceof EditTextPreference) {
                EditTextPreference pref = (EditTextPreference) p;
                boolean fieldEmpty = isEditTextEmpty(key);
                if (allEmpty && !fieldEmpty) allEmpty = false;

                // use default value if display name is empty
                if (fieldEmpty) {
                    switch (key) {
                        case DisplayName:
                            pref.setText(getDefaultDisplayName());
                            break;
                        case AuthUserName:
                        case ProxyAddress:
                            // optional; do nothing
                            break;
                        case Port:
                            pref.setText(getString(R.string.default_port));
                            break;
                        default:
                            if (firstEmptyFieldTitle == null) {
                                firstEmptyFieldTitle = pref.getTitle();
                            }
                    }
                } else if (key == PreferenceKey.Port) {
                    int port = Integer.parseInt(PreferenceKey.Port.getValue());
                    if ((port < 1000) || (port > 65534)) {
                        showAlert(getString(R.string.not_a_valid_port));
                        return;
                    }
                }
            }
        }

        if (allEmpty || !mUpdateRequired) {
            finish();
            return;
        } else if (firstEmptyFieldTitle != null) {
            showAlert(getString(R.string.empty_alert, firstEmptyFieldTitle));
            return;
        }
        try {
            SipProfile profile = createSipProfile();
            Intent intent = new Intent(this, SipSettings.class);
            intent.putExtra(SipSettings.KEY_SIP_PROFILE, (Parcelable) profile);
            setResult(RESULT_OK, intent);
            Toast.makeText(this, R.string.saving_account, Toast.LENGTH_SHORT)
                    .show();

            replaceProfile(mOldProfile, profile);
            // do finish() in replaceProfile() in a background thread
        } catch (Exception e) {
            Log.w(TAG, "Can not create new SipProfile", e);
            showAlert(e);
        }
    }

    private void unregisterOldPrimaryAccount() {
        String primaryAccountUri = mSharedPreferences.getPrimaryAccount();
        Log.v(TAG, "old primary: " + primaryAccountUri);
        if ((primaryAccountUri != null)
                && !mSharedPreferences.isReceivingCallsEnabled()) {
            Log.v(TAG, "unregister old primary: " + primaryAccountUri);
            unregisterProfile(primaryAccountUri);
        }
    }

    private void replaceProfile(final SipProfile oldProfile,
            final SipProfile newProfile) {
        // Replace profile in a background thread as it takes time to access the
        // storage; do finish() once everything goes fine.
        // newProfile may be null if the old profile is to be deleted rather
        // than being modified.
        new Thread(new Runnable() {
            public void run() {
                try {
                    // if new profile is primary, unregister the old primary account
                    if ((newProfile != null) && mPrimaryAccountSelector.isSelected()) {
                        unregisterOldPrimaryAccount();
                    }

                    mPrimaryAccountSelector.commit(newProfile);
                    deleteAndUnregisterProfile(oldProfile);
                    saveAndRegisterProfile(newProfile);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Can not save/register new SipProfile", e);
                    showAlert(e);
                }
            }
        }, "SipEditor").start();
    }

    private String getProfileName() {
        return PreferenceKey.Username.getValue() + "@"
                + PreferenceKey.DomainAddress.getValue();
    }

    private SipProfile createSipProfile() throws Exception {
            return new SipProfile.Builder(
                    PreferenceKey.Username.getValue(),
                    PreferenceKey.DomainAddress.getValue())
                    .setProfileName(getProfileName())
                    .setPassword(PreferenceKey.Password.getValue())
                    .setOutboundProxy(PreferenceKey.ProxyAddress.getValue())
                    .setProtocol(PreferenceKey.Transport.getValue())
                    .setDisplayName(PreferenceKey.DisplayName.getValue())
                    .setPort(Integer.parseInt(PreferenceKey.Port.getValue()))
                    .setSendKeepAlive(isAlwaysSendKeepAlive())
                    .setAutoRegistration(
                            mSharedPreferences.isReceivingCallsEnabled())
                    .setAuthUserName(PreferenceKey.AuthUserName.getValue())
                    .build();
    }

    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (!mUpdateRequired) {
            mUpdateRequired = true;
            if (mOldProfile != null) {
                unregisterProfile(mOldProfile.getUriString());
            }
        }
        if (pref instanceof CheckBoxPreference) {
            invalidateOptionsMenu();
            return true;
        }
        String value = (newValue == null) ? "" : newValue.toString();
        if (TextUtils.isEmpty(value)) {
            pref.setSummary(getPreferenceKey(pref).defaultSummary);
        } else if (pref == PreferenceKey.Password.preference) {
            pref.setSummary(scramble(value));
        } else {
            pref.setSummary(value);
        }

        if (pref == PreferenceKey.DisplayName.preference) {
            ((EditTextPreference) pref).setText(value);
            checkIfDisplayNameSet();
        }

        // SAVE menu should be enabled once the user modified some preference.
        invalidateOptionsMenu();
        return true;
    }

    private PreferenceKey getPreferenceKey(Preference pref) {
        for (PreferenceKey key : PreferenceKey.values()) {
            if (key.preference == pref) return key;
        }
        throw new RuntimeException("not possible to reach here");
    }

    private void loadPreferencesFromProfile(SipProfile p) {
        if (p != null) {
            Log.v(TAG, "Edit the existing profile : " + p.getProfileName());
            try {
                Class profileClass = SipProfile.class;
                for (PreferenceKey key : PreferenceKey.values()) {
                    Method meth = profileClass.getMethod(GET_METHOD_PREFIX
                            + getString(key.text), (Class[])null);
                    if (key == PreferenceKey.SendKeepAlive) {
                        boolean value = ((Boolean)
                                meth.invoke(p, (Object[]) null)).booleanValue();
                        key.setValue(getString(value
                                ? R.string.sip_always_send_keepalive
                                : R.string.sip_system_decide));
                    } else {
                        Object value = meth.invoke(p, (Object[])null);
                        key.setValue((value == null) ? "" : value.toString());
                    }
                }
                checkIfDisplayNameSet();
            } catch (Exception e) {
                Log.e(TAG, "Can not load pref from profile", e);
            }
        } else {
            Log.v(TAG, "Edit a new profile");
            for (PreferenceKey key : PreferenceKey.values()) {
                key.preference.setOnPreferenceChangeListener(this);

                // FIXME: android:defaultValue in preference xml file doesn't
                // work. Even if we setValue() for each preference in the case
                // of (p != null), the dialog still shows android:defaultValue,
                // not the value set by setValue(). This happens if
                // android:defaultValue is not empty. Is it a bug?
                if (key.initValue != 0) {
                    key.setValue(getString(key.initValue));
                }
            }
            mDisplayNameSet = false;
        }
    }

    private boolean isAlwaysSendKeepAlive() {
        ListPreference pref = (ListPreference)
                PreferenceKey.SendKeepAlive.preference;
        return getString(R.string.sip_always_send_keepalive).equals(
                pref.getValue());
    }

    private void setCheckBox(PreferenceKey key, boolean checked) {
        CheckBoxPreference pref = (CheckBoxPreference) key.preference;
        pref.setChecked(checked);
    }

    private void setupPreference(Preference pref) {
        pref.setOnPreferenceChangeListener(this);
        for (PreferenceKey key : PreferenceKey.values()) {
            String name = getString(key.text);
            if (name.equals(pref.getKey())) {
                key.preference = pref;
                return;
            }
        }
    }

    private void checkIfDisplayNameSet() {
        String displayName = PreferenceKey.DisplayName.getValue();
        mDisplayNameSet = !TextUtils.isEmpty(displayName)
                && !displayName.equals(getDefaultDisplayName());
        Log.d(TAG, "displayName set? " + mDisplayNameSet);
        if (mDisplayNameSet) {
            PreferenceKey.DisplayName.preference.setSummary(displayName);
        } else {
            PreferenceKey.DisplayName.setValue("");
        }
    }

    private static String getDefaultDisplayName() {
        return PreferenceKey.Username.getValue();
    }

    private static String scramble(String s) {
        char[] cc = new char[s.length()];
        Arrays.fill(cc, SCRAMBLED);
        return new String(cc);
    }

    // only takes care of the primary account setting in SipSharedSettings
    private class PrimaryAccountSelector {
        private CheckBoxPreference mCheckbox;
        private final boolean mWasPrimaryAccount;

        // @param profile profile to be edited; null if adding new profile
        PrimaryAccountSelector(SipProfile profile) {
            mCheckbox = (CheckBoxPreference) getPreferenceScreen()
                    .findPreference(getString(R.string.set_primary));
            boolean noPrimaryAccountSet =
                    !mSharedPreferences.hasPrimaryAccount();
            boolean editNewProfile = (profile == null);
            mWasPrimaryAccount = !editNewProfile
                    && mSharedPreferences.isPrimaryAccount(
                            profile.getUriString());

            Log.v(TAG, " noPrimaryAccountSet: " + noPrimaryAccountSet);
            Log.v(TAG, " editNewProfile: " + editNewProfile);
            Log.v(TAG, " mWasPrimaryAccount: " + mWasPrimaryAccount);

            mCheckbox.setChecked(mWasPrimaryAccount
                    || (editNewProfile && noPrimaryAccountSet));
        }

        boolean isSelected() {
            return mCheckbox.isChecked();
        }

        // profile is null if the user removes it
        void commit(SipProfile profile) {
            if ((profile != null) && mCheckbox.isChecked()) {
                mSharedPreferences.setPrimaryAccount(profile.getUriString());
            } else if (mWasPrimaryAccount) {
                mSharedPreferences.unsetPrimaryAccount();
            }
            Log.d(TAG, " primary account changed to : "
                    + mSharedPreferences.getPrimaryAccount());
        }
    }

    private class AdvancedSettings
            implements Preference.OnPreferenceClickListener {
        private Preference mAdvancedSettingsTrigger;
        private Preference[] mPreferences;
        private boolean mShowing = false;

        AdvancedSettings() {
            mAdvancedSettingsTrigger = getPreferenceScreen().findPreference(
                    getString(R.string.advanced_settings));
            mAdvancedSettingsTrigger.setOnPreferenceClickListener(this);

            loadAdvancedPreferences();
        }

        private void loadAdvancedPreferences() {
            PreferenceGroup screen = (PreferenceGroup) getPreferenceScreen();

            addPreferencesFromResource(R.xml.sip_advanced_edit);
            PreferenceGroup group = (PreferenceGroup) screen.findPreference(
                    getString(R.string.advanced_settings_container));
            screen.removePreference(group);

            mPreferences = new Preference[group.getPreferenceCount()];
            int order = screen.getPreferenceCount();
            for (int i = 0, n = mPreferences.length; i < n; i++) {
                Preference pref = group.getPreference(i);
                pref.setOrder(order++);
                setupPreference(pref);
                mPreferences[i] = pref;
            }
        }

        void show() {
            mShowing = true;
            mAdvancedSettingsTrigger.setSummary(R.string.advanced_settings_hide);
            PreferenceGroup screen = (PreferenceGroup) getPreferenceScreen();
            for (Preference pref : mPreferences) {
                screen.addPreference(pref);
                Log.v(TAG, "add pref " + pref.getKey() + ": order=" + pref.getOrder());
            }
        }

        private void hide() {
            mShowing = false;
            mAdvancedSettingsTrigger.setSummary(R.string.advanced_settings_show);
            PreferenceGroup screen = (PreferenceGroup) getPreferenceScreen();
            for (Preference pref : mPreferences) {
                screen.removePreference(pref);
            }
        }

        public boolean onPreferenceClick(Preference preference) {
            Log.v(TAG, "optional settings clicked");
            if (!mShowing) {
                show();
            } else {
                hide();
            }
            return true;
        }
    }
}

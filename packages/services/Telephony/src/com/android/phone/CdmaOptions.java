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

package com.android.phone;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

/**
 * List of Phone-specific settings screens.
 */
public class CdmaOptions {
    private static final String LOG_TAG = "CdmaOptions";

    private CdmaSystemSelectListPreference mButtonCdmaSystemSelect;
    private CdmaSubscriptionListPreference mButtonCdmaSubscription;

    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
    private static final String BUTTON_CDMA_ACTIVATE_DEVICE_KEY = "cdma_activate_device_key";

    private PreferenceActivity mPrefActivity;
    private PreferenceScreen mPrefScreen;
    private Phone mPhone;

    public CdmaOptions(PreferenceActivity prefActivity, PreferenceScreen prefScreen, Phone phone) {
        mPrefActivity = prefActivity;
        mPrefScreen = prefScreen;
        mPhone = phone;
        create();
    }

    protected void create() {
        mPrefActivity.addPreferencesFromResource(R.xml.cdma_options);

        mButtonCdmaSystemSelect = (CdmaSystemSelectListPreference)mPrefScreen
                .findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);

        mButtonCdmaSubscription = (CdmaSubscriptionListPreference)mPrefScreen
                .findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY);

        mButtonCdmaSystemSelect.setEnabled(true);
        if(deviceSupportsNvAndRuim()) {
            log("Both NV and Ruim supported, ENABLE subscription type selection");
            mButtonCdmaSubscription.setEnabled(true);
        } else {
            log("Both NV and Ruim NOT supported, REMOVE subscription type selection");
            mPrefScreen.removePreference(mPrefScreen
                                .findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY));
        }

        final boolean voiceCapable = mPrefActivity.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
        final boolean isLTE = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        if (voiceCapable || isLTE) {
            // This option should not be available on voice-capable devices (i.e. regular phones)
            // and is replaced by the LTE data service item on LTE devices
            mPrefScreen.removePreference(
                    mPrefScreen.findPreference(BUTTON_CDMA_ACTIVATE_DEVICE_KEY));
        }
    }

    private boolean deviceSupportsNvAndRuim() {
        // retrieve the list of subscription types supported by device.
        String subscriptionsSupported = SystemProperties.get("ril.subscription.types");
        boolean nvSupported = false;
        boolean ruimSupported = false;

        log("deviceSupportsnvAnRum: prop=" + subscriptionsSupported);
        if (!TextUtils.isEmpty(subscriptionsSupported)) {
            // Searches through the comma-separated list for a match for "NV"
            // and "RUIM" to update nvSupported and ruimSupported.
            for (String subscriptionType : subscriptionsSupported.split(",")) {
                subscriptionType = subscriptionType.trim();
                if (subscriptionType.equalsIgnoreCase("NV")) {
                    nvSupported = true;
                }
                if (subscriptionType.equalsIgnoreCase("RUIM")) {
                    ruimSupported = true;
                }
            }
        }

        log("deviceSupportsnvAnRum: nvSupported=" + nvSupported +
                " ruimSupported=" + ruimSupported);
        return (nvSupported && ruimSupported);
    }

    public boolean preferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            log("preferenceTreeClick: return BUTTON_CDMA_ROAMING_KEY true");
            return true;
        }
        if (preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            log("preferenceTreeClick: return CDMA_SUBSCRIPTION_KEY true");
            return true;
        }
        return false;
    }

    public void showDialog(Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            mButtonCdmaSystemSelect.showDialog(null);
        } else if (preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            mButtonCdmaSubscription.showDialog(null);
        }
    }

    protected void log(String s) {
        android.util.Log.d(LOG_TAG, s);
    }
}

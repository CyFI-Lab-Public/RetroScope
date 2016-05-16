/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.provider.CallLog.Calls;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import com.google.android.collect.Sets;

import java.util.Locale;
import java.util.Set;

/**
 * Default implementation of {@link CallLogInsertionHelper}.
 * <p>
 * It added the country ISO abbreviation and the geocoded location.
 * It checks for legacy unknown numbers and updates number presentation.
 * <p>
 * It uses {@link PhoneNumberOfflineGeocoder} to compute the geocoded location of a phone number.
 */
/*package*/ class DefaultCallLogInsertionHelper implements CallLogInsertionHelper {
    private static DefaultCallLogInsertionHelper sInstance;

    private static final Set<String> LEGACY_UNKNOWN_NUMBERS = Sets.newHashSet("-1", "-2", "-3");

    private final CountryMonitor mCountryMonitor;
    private PhoneNumberUtil mPhoneNumberUtil;
    private PhoneNumberOfflineGeocoder mPhoneNumberOfflineGeocoder;
    private final Locale mLocale;

    public static synchronized DefaultCallLogInsertionHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DefaultCallLogInsertionHelper(context);
        }
        return sInstance;
    }

    private DefaultCallLogInsertionHelper(Context context) {
        mCountryMonitor = new CountryMonitor(context);
        mLocale = context.getResources().getConfiguration().locale;
    }

    @Override
    public void addComputedValues(ContentValues values) {
        // Insert the current country code, so we know the country the number belongs to.
        String countryIso = getCurrentCountryIso();
        values.put(Calls.COUNTRY_ISO, countryIso);
        // Insert the geocoded location, so that we do not need to compute it on the fly.
        values.put(Calls.GEOCODED_LOCATION,
                getGeocodedLocationFor(values.getAsString(Calls.NUMBER), countryIso));

        final String number = values.getAsString(Calls.NUMBER);
        if (LEGACY_UNKNOWN_NUMBERS.contains(number)) {
            values.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_UNKNOWN);
            values.put(Calls.NUMBER, "");
        }
    }

    private String getCurrentCountryIso() {
        return mCountryMonitor.getCountryIso();
    }

    private synchronized PhoneNumberUtil getPhoneNumberUtil() {
        if (mPhoneNumberUtil == null) {
            mPhoneNumberUtil = PhoneNumberUtil.getInstance();
        }
        return mPhoneNumberUtil;
    }

    private PhoneNumber parsePhoneNumber(String number, String countryIso) {
        try {
            return getPhoneNumberUtil().parse(number, countryIso);
        } catch (NumberParseException e) {
            return null;
        }
    }

    private synchronized PhoneNumberOfflineGeocoder getPhoneNumberOfflineGeocoder() {
        if (mPhoneNumberOfflineGeocoder == null) {
            mPhoneNumberOfflineGeocoder = PhoneNumberOfflineGeocoder.getInstance();
        }
        return mPhoneNumberOfflineGeocoder;
    }

    @Override
    public String getGeocodedLocationFor(String number, String countryIso) {
        PhoneNumber structuredPhoneNumber = parsePhoneNumber(number, countryIso);
        if (structuredPhoneNumber != null) {
            return getPhoneNumberOfflineGeocoder().getDescriptionForNumber(
                    structuredPhoneNumber, mLocale);
        } else {
            return null;
        }
    }
}

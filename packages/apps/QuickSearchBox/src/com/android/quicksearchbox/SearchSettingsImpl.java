/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.android.common.SharedPreferencesCompat;

/**
 * Manages user settings.
 */
public class SearchSettingsImpl implements SearchSettings {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchSettingsImpl";

    // Name of the preferences file used to store search preference
    public static final String PREFERENCES_NAME = "SearchSettings";

    /**
     * Preference key used for storing the index of the next voice search hint to show.
     */
    private static final String NEXT_VOICE_SEARCH_HINT_INDEX_PREF = "next_voice_search_hint";

    /**
     * Preference key used to store the time at which the first voice search hint was displayed.
     */
    private static final String FIRST_VOICE_HINT_DISPLAY_TIME = "first_voice_search_hint_time";

    /**
     * Preference key for the version of voice search we last got hints from.
     */
    private static final String LAST_SEEN_VOICE_SEARCH_VERSION = "voice_search_version";

    /**
     * Preference key for storing whether searches always go to google.com. Public
     * so that it can be used by PreferenceControllers.
     */
    public static final String USE_GOOGLE_COM_PREF = "use_google_com";

    /**
     * Preference key for the base search URL. This value is normally set by
     * a SearchBaseUrlHelper instance. Public so classes can listen to changes
     * on this key.
     */
    public static final String SEARCH_BASE_DOMAIN_PREF = "search_base_domain";

    /**
     * This is the time at which the base URL was stored, and is set using
     * @link{System.currentTimeMillis()}.
     */
    private static final String SEARCH_BASE_DOMAIN_APPLY_TIME = "search_base_domain_apply_time";

    private final Context mContext;

    private final Config mConfig;

    public SearchSettingsImpl(Context context, Config config) {
        mContext = context;
        mConfig = config;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Config getConfig() {
        return mConfig;
    }

    @Override
    public void upgradeSettingsIfNeeded() {
    }

    public SharedPreferences getSearchPreferences() {
        return getContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    protected void storeBoolean(String name, boolean value) {
        SharedPreferencesCompat.apply(getSearchPreferences().edit().putBoolean(name, value));
    }

    protected void storeInt(String name, int value) {
        SharedPreferencesCompat.apply(getSearchPreferences().edit().putInt(name, value));
    }

    protected void storeLong(String name, long value) {
        SharedPreferencesCompat.apply(getSearchPreferences().edit().putLong(name, value));
    }

    protected void storeString(String name, String value) {
        SharedPreferencesCompat.apply(getSearchPreferences().edit().putString(name, value));
    }

    protected void removePref(String name) {
        SharedPreferencesCompat.apply(getSearchPreferences().edit().remove(name));
    }

    /**
     * Informs our listeners about the updated settings data.
     */
    @Override
    public void broadcastSettingsChanged() {
        // We use a message broadcast since the listeners could be in multiple processes.
        Intent intent = new Intent(SearchManager.INTENT_ACTION_SEARCH_SETTINGS_CHANGED);
        Log.i(TAG, "Broadcasting: " + intent);
        getContext().sendBroadcast(intent);
    }

    @Override
    public int getNextVoiceSearchHintIndex(int size) {
            int i = getAndIncrementIntPreference(getSearchPreferences(),
                    NEXT_VOICE_SEARCH_HINT_INDEX_PREF);
            return i % size;
    }

    // TODO: Could this be made atomic to avoid races?
    private int getAndIncrementIntPreference(SharedPreferences prefs, String name) {
        int i = prefs.getInt(name, 0);
        storeInt(name, i + 1);
        return i;
    }

    @Override
    public void resetVoiceSearchHintFirstSeenTime() {
        storeLong(FIRST_VOICE_HINT_DISPLAY_TIME, System.currentTimeMillis());
    }

    @Override
    public boolean haveVoiceSearchHintsExpired(int currentVoiceSearchVersion) {
        SharedPreferences prefs = getSearchPreferences();

        if (currentVoiceSearchVersion != 0) {
            long currentTime = System.currentTimeMillis();
            int lastVoiceSearchVersion = prefs.getInt(LAST_SEEN_VOICE_SEARCH_VERSION, 0);
            long firstHintTime = prefs.getLong(FIRST_VOICE_HINT_DISPLAY_TIME, 0);
            if (firstHintTime == 0 || currentVoiceSearchVersion != lastVoiceSearchVersion) {
                SharedPreferencesCompat.apply(prefs.edit()
                        .putInt(LAST_SEEN_VOICE_SEARCH_VERSION, currentVoiceSearchVersion)
                        .putLong(FIRST_VOICE_HINT_DISPLAY_TIME, currentTime));
                firstHintTime = currentTime;
            }
            if (currentTime - firstHintTime > getConfig().getVoiceSearchHintActivePeriod()) {
                if (DBG) Log.d(TAG, "Voice seach hint period expired; not showing hints.");
                return true;
            } else {
                return false;
            }
        } else {
            if (DBG) Log.d(TAG, "Could not determine voice search version; not showing hints.");
            return true;
        }
    }

    /**
     * @return true if user searches should always be based at google.com, false
     *     otherwise.
     */
    @Override
    public boolean shouldUseGoogleCom() {
        // Note that this preserves the old behaviour of using google.com
        // for searches, with the gl= parameter set.
        return getSearchPreferences().getBoolean(USE_GOOGLE_COM_PREF, true);
    }

    @Override
    public void setUseGoogleCom(boolean useGoogleCom) {
        storeBoolean(USE_GOOGLE_COM_PREF, useGoogleCom);
    }

    @Override
    public long getSearchBaseDomainApplyTime() {
        return getSearchPreferences().getLong(SEARCH_BASE_DOMAIN_APPLY_TIME, -1);
    }

    @Override
    public String getSearchBaseDomain() {
        // Note that the only time this will return null is on the first run
        // of the app, or when settings have been cleared. Callers should
        // ideally check that getSearchBaseDomainApplyTime() is not -1 before
        // calling this function.
        return getSearchPreferences().getString(SEARCH_BASE_DOMAIN_PREF, null);
    }

    @Override
    public void setSearchBaseDomain(String searchBaseUrl) {
        Editor sharedPrefEditor = getSearchPreferences().edit();
        sharedPrefEditor.putString(SEARCH_BASE_DOMAIN_PREF, searchBaseUrl);
        sharedPrefEditor.putLong(SEARCH_BASE_DOMAIN_APPLY_TIME, System.currentTimeMillis());

        SharedPreferencesCompat.apply(sharedPrefEditor);
    }
}

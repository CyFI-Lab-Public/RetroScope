/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.mail.preferences;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import android.content.Context;

import java.util.Map;

/**
 * Preferences relevant to one specific account.
 */
public class AccountPreferences extends VersionedPrefs {

    private static final String PREFS_NAME_PREFIX = "Account";

    private static Map<String, AccountPreferences> mInstances = Maps.newHashMap();

    public static final class PreferenceKeys {
        /**
         * A temporary preference that can be set during account setup, if we do not know what the
         * default inbox is yet. This value should be moved into the appropriate
         * {@link FolderPreferences} once we have the inbox, and removed from here.
         */
        private static final String DEFAULT_INBOX_NOTIFICATIONS_ENABLED =
                "inbox-notifications-enabled";

        /** Boolean value indicating whether notifications are enabled */
        public static final String NOTIFICATIONS_ENABLED = "notifications-enabled";

        /**
         * Number of time user has dismissed / seen the toast for account sync is off message.
         */
        public static final String ACCOUNT_SYNC_OFF_DISMISSES = "num-of-dismisses-account-sync-off";

        /**
         * The count reported last time the "X unseen in Outbox" tip was displayed.
         */
        public static final String LAST_SEEN_OUTBOX_COUNT = "last-seen-outbox-count";

        public static final ImmutableSet<String> BACKUP_KEYS =
                new ImmutableSet.Builder<String>().add(NOTIFICATIONS_ENABLED).build();
    }

    /**
     * @param account The account email. This must never change for the account.
     */
    public AccountPreferences(final Context context, final String account) {
        super(context, buildSharedPrefsName(account));
    }

    private static String buildSharedPrefsName(final String account) {
        return PREFS_NAME_PREFIX + '-' + account;
    }

    public static synchronized AccountPreferences get(Context context, String accountEmail) {
        AccountPreferences pref = mInstances.get(accountEmail);
        if (pref == null) {
            pref = new AccountPreferences(context, accountEmail);
            mInstances.put(accountEmail, pref);
        }
        return pref;
    }

    @Override
    protected void performUpgrade(final int oldVersion, final int newVersion) {
        if (oldVersion > newVersion) {
            throw new IllegalStateException(
                    "You appear to have downgraded your app. Please clear app data.");
        }
    }

    @Override
    protected boolean canBackup(final String key) {
        return PreferenceKeys.BACKUP_KEYS.contains(key);
    }

    public boolean isDefaultInboxNotificationsEnabledSet() {
        return getSharedPreferences().contains(PreferenceKeys.DEFAULT_INBOX_NOTIFICATIONS_ENABLED);
    }

    public boolean getDefaultInboxNotificationsEnabled() {
        return getSharedPreferences()
                .getBoolean(PreferenceKeys.DEFAULT_INBOX_NOTIFICATIONS_ENABLED, true);
    }

    public void setDefaultInboxNotificationsEnabled(final boolean enabled) {
        getEditor().putBoolean(PreferenceKeys.DEFAULT_INBOX_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public void clearDefaultInboxNotificationsEnabled() {
        getEditor().remove(PreferenceKeys.DEFAULT_INBOX_NOTIFICATIONS_ENABLED).apply();
    }

    public boolean areNotificationsEnabled() {
        return getSharedPreferences().getBoolean(PreferenceKeys.NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(final boolean enabled) {
        getEditor().putBoolean(PreferenceKeys.NOTIFICATIONS_ENABLED, enabled).apply();
        notifyBackupPreferenceChanged();
    }

    public int getNumOfDismissesForAccountSyncOff() {
        return getSharedPreferences().getInt(PreferenceKeys.ACCOUNT_SYNC_OFF_DISMISSES, 0);
    }

    public void resetNumOfDismissesForAccountSyncOff() {
        final int value = getSharedPreferences().getInt(
                PreferenceKeys.ACCOUNT_SYNC_OFF_DISMISSES, 0);
        if (value != 0) {
            getEditor().putInt(PreferenceKeys.ACCOUNT_SYNC_OFF_DISMISSES, 0).apply();
        }
    }

    public void incNumOfDismissesForAccountSyncOff() {
        final int value = getSharedPreferences().getInt(
                PreferenceKeys.ACCOUNT_SYNC_OFF_DISMISSES, 0);
        getEditor().putInt(PreferenceKeys.ACCOUNT_SYNC_OFF_DISMISSES, value + 1).apply();
    }

    public int getLastSeenOutboxCount() {
        return getSharedPreferences().getInt(PreferenceKeys.LAST_SEEN_OUTBOX_COUNT, 0);
    }

    public void setLastSeenOutboxCount(final int count) {
        getEditor().putInt(PreferenceKeys.LAST_SEEN_OUTBOX_COUNT, count).apply();
    }
}

/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.android.mail.MailIntentService;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A high-level API to store and retrieve preferences, that can be versioned in a similar manner as
 * SQLite databases. You must not use the preference key
 * {@value VersionedPrefs#PREFS_VERSION_NUMBER}
 */
public abstract class VersionedPrefs {
    private final Context mContext;
    private final String mSharedPreferencesName;
    private final SharedPreferences mSharedPreferences;
    private final Editor mEditor;

    /** The key for the version number of the {@link SharedPreferences} file. */
    private static final String PREFS_VERSION_NUMBER = "prefs-version-number";

    /**
     * The current version number for {@link SharedPreferences}. This is a constant for all
     * applications based on UnifiedEmail.
     */
    protected static final int CURRENT_VERSION_NUMBER = 3;

    protected static final String LOG_TAG = LogTag.getLogTag();

    /**
     * @param sharedPrefsName The name of the {@link SharedPreferences} file to use
     */
    protected VersionedPrefs(final Context context, final String sharedPrefsName) {
        mContext = context.getApplicationContext();
        mSharedPreferencesName = sharedPrefsName;
        mSharedPreferences = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();

        final int oldVersion = getCurrentVersion();

        performUpgrade(oldVersion, CURRENT_VERSION_NUMBER);
        setCurrentVersion(CURRENT_VERSION_NUMBER);

        if (!hasMigrationCompleted()) {
            final boolean migrationComplete = PreferenceMigratorHolder.createPreferenceMigrator()
                    .performMigration(context, oldVersion, CURRENT_VERSION_NUMBER);

            if (migrationComplete) {
                setMigrationComplete();
            }
        }
    }

    protected Context getContext() {
        return mContext;
    }

    public String getSharedPreferencesName() {
        return mSharedPreferencesName;
    }

    protected SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    protected Editor getEditor() {
        return mEditor;
    }

    /**
     * Returns the current version of the {@link SharedPreferences} file.
     */
    private int getCurrentVersion() {
        return mSharedPreferences.getInt(PREFS_VERSION_NUMBER, 0);
    }

    private void setCurrentVersion(final int versionNumber) {
        getEditor().putInt(PREFS_VERSION_NUMBER, versionNumber);

        /*
         * If the only preference we have is the version number, we do not want to commit it.
         * Instead, we will wait for some other preference to be written. This prevents us from
         * creating a file with only the version number.
         */
        if (shouldBackUp()) {
            getEditor().apply();
        }
    }

    protected boolean hasMigrationCompleted() {
        return MailPrefs.get(mContext).hasMigrationCompleted();
    }

    protected void setMigrationComplete() {
        MailPrefs.get(mContext).setMigrationComplete();
    }

    /**
     * Commits all pending changes to the preferences.
     */
    public void commit() {
        getEditor().commit();
    }

    /**
     * Upgrades the {@link SharedPreferences} file.
     *
     * @param oldVersion The current version
     * @param newVersion The new version
     */
    protected abstract void performUpgrade(int oldVersion, int newVersion);

    @VisibleForTesting
    public void clearAllPreferences() {
        getEditor().clear().commit();
    }

    protected abstract boolean canBackup(String key);

    /**
     * Gets the value to backup for a given key-value pair. By default, returns the passed in value.
     *
     * @param key The key to backup
     * @param value The locally stored value for the given key
     * @return The value to backup
     */
    protected Object getBackupValue(final String key, final Object value) {
        return value;
    }

    /**
     * Gets the value to restore for a given key-value pair. By default, returns the passed in
     * value.
     *
     * @param key The key to restore
     * @param value The backed up value for the given key
     * @return The value to restore
     */
    protected Object getRestoreValue(final String key, final Object value) {
        return value;
    }

    /**
     * Return a list of shared preferences that should be backed up.
     */
    public List<BackupSharedPreference> getBackupPreferences() {
        final List<BackupSharedPreference> backupPreferences = Lists.newArrayList();
        final SharedPreferences sharedPreferences = getSharedPreferences();
        final Map<String, ?> preferences = sharedPreferences.getAll();

        for (final Map.Entry<String, ?> entry : preferences.entrySet()) {
            final String key = entry.getKey();

            if (!canBackup(key)) {
                continue;
            }

            final Object value = entry.getValue();
            final Object backupValue = getBackupValue(key, value);

            if (backupValue != null) {
                backupPreferences.add(new SimpleBackupSharedPreference(key, backupValue));
            }
        }

        return backupPreferences;
    }

    /**
     * Restores preferences from a backup.
     */
    public void restorePreferences(final List<BackupSharedPreference> preferences) {
        for (final BackupSharedPreference preference : preferences) {
            final String key = preference.getKey();
            final Object value = preference.getValue();

            if (!canBackup(key) || value == null) {
                continue;
            }

            final Object restoreValue = getRestoreValue(key, value);

            if (restoreValue instanceof Boolean) {
                getEditor().putBoolean(key, (Boolean) restoreValue);
                LogUtils.v(LOG_TAG, "MailPrefs Restore: %s", preference);
            } else if (restoreValue instanceof Float) {
                getEditor().putFloat(key, (Float) restoreValue);
                LogUtils.v(LOG_TAG, "MailPrefs Restore: %s", preference);
            } else if (restoreValue instanceof Integer) {
                getEditor().putInt(key, (Integer) restoreValue);
                LogUtils.v(LOG_TAG, "MailPrefs Restore: %s", preference);
            } else if (restoreValue instanceof Long) {
                getEditor().putLong(key, (Long) restoreValue);
                LogUtils.v(LOG_TAG, "MailPrefs Restore: %s", preference);
            } else if (restoreValue instanceof String) {
                getEditor().putString(key, (String) restoreValue);
                LogUtils.v(LOG_TAG, "MailPrefs Restore: %s", preference);
            } else if (restoreValue instanceof Set) {
                getEditor().putStringSet(key, (Set<String>) restoreValue);
            } else {
                LogUtils.e(LOG_TAG, "Unknown MailPrefs preference data type: %s", value.getClass());
            }
        }

        getEditor().apply();
    }

    /**
     * <p>
     * Checks if any of the preferences eligible for backup have been modified from their default
     * values, and therefore should be backed up.
     * </p>
     *
     * @return <code>true</code> if anything has been modified, <code>false</code> otherwise
     */
    public boolean shouldBackUp() {
        final Map<String, ?> allPrefs = getSharedPreferences().getAll();

        for (final String key : allPrefs.keySet()) {
            if (canBackup(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Notifies {@link BackupManager} that we have new data to back up.
     */
    protected void notifyBackupPreferenceChanged() {
        MailIntentService.broadcastBackupDataChanged(getContext());
    }
}

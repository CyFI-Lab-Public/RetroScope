/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.providers.contacts;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import libcore.icu.ICU;

/**
 * This will be launched during system boot, after the core system has
 * been brought up but before any non-persistent processes have been
 * started.  It is launched in a special state, with no content provider
 * or custom application class associated with the process running.
 *
 * It's job is to prime the contacts database. Either create it
 * if it doesn't exist, or open it and force any necessary upgrades.
 * All of this heavy lifting happens before the boot animation ends.
 */
public class ContactsUpgradeReceiver extends BroadcastReceiver {
    static final String TAG = "ContactsUpgradeReceiver";
    static final String PREF_DB_VERSION = "db_version";
    static final String PREF_ICU_VERSION = "icu_version";

    @Override
    public void onReceive(Context context, Intent intent) {
        // We are now running with the system up, but no apps started,
        // so can do whatever cleanup after an upgrade that we want.

        try {
            long startTime = System.currentTimeMillis();

            // Lookup the last known database version
            SharedPreferences prefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
            int prefDbVersion = prefs.getInt(PREF_DB_VERSION, 0);
            final String curIcuVersion = ICU.getIcuVersion();
            final String prefIcuVersion = prefs.getString(PREF_ICU_VERSION, "");

            // If the version is old go ahead and attempt to create or upgrade the database.
            if (prefDbVersion != ContactsDatabaseHelper.DATABASE_VERSION ||
                    !prefIcuVersion.equals(curIcuVersion)) {
                // Store the current version so this receiver isn't run again until the database
                // version number changes. This is intentionally done even before the upgrade path
                // is attempted to be conservative. If the upgrade fails for some reason and we
                // crash and burn we don't want to get into a loop doing so.
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(PREF_DB_VERSION, ContactsDatabaseHelper.DATABASE_VERSION);
                editor.putString(PREF_ICU_VERSION, curIcuVersion);
                editor.commit();

                // Ask for a reference to the database to force the helper to either
                // create the database or open it up, performing any necessary upgrades
                // in the process.
                ContactsDatabaseHelper helper = ContactsDatabaseHelper.getInstance(context);
                ProfileDatabaseHelper profileHelper = ProfileDatabaseHelper.getInstance(context);
                Log.i(TAG, "Creating or opening contacts database");
                try {
                    ActivityManagerNative.getDefault().showBootMessage(
                            context.getText(R.string.upgrade_msg), true);
                } catch (RemoteException e) {
                }

                helper.getWritableDatabase();
                profileHelper.getWritableDatabase();
                ContactsProvider2.updateLocaleOffline(context, helper, profileHelper);

                // Log the total time taken for the receiver to perform the operation
                EventLogTags.writeContactsUpgradeReceiver(System.currentTimeMillis() - startTime);
            }
        } catch (Throwable t) {
            // Something has gone terribly wrong. Disable this receiver for good so we can't
            // possibly end up in a reboot loop.
            Log.wtf(TAG, "Error during upgrade attempt. Disabling receiver.", t);
            context.getPackageManager().setComponentEnabledSetting(
                    new ComponentName(context, getClass()),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}

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

package android.security.cts;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;


public class SqliteJournalLeakTest extends AndroidTestCase {

    private static final int REPEAT = 5;

    private static final String[] DATABASES = {
        "/com.android.bluetooth/databases/btopp.db",
        "/com.android.browser/app_appcache/ApplicationCache.db",
        "/com.android.browser/app_databases/Databases.db",
        "/com.android.browser/app_geolocation/CachedGeoposition.db",
        "/com.android.browser/app_geolocation/GeolocationPermissions.db",
        "/com.android.browser/app_icons/WebpageIcons.db",
        "/com.android.browser/databases/browser.db",
        "/com.android.browser/databases/launcher.db",
        "/com.android.browser/databases/webview.db",
        "/com.android.browser/databases/webviewCache.db",
        "/com.android.email/databases/EmailProvider.db",
        "/com.android.email/databases/EmailProviderBody.db",
        "/com.android.email/databases/webview.db",
        "/com.android.email/databases/webviewCache.db",
        "/com.android.providers.calendar/databases/calendar.db",
        "/com.android.providers.contacts/databases/contacts2.db",
        "/com.android.providers.downloads/databases/downloads.db",
        "/com.android.providers.drm/databases/drm.db",
        "/com.android.providers.media/databases/internal.db",
        "/com.android.providers.settings/databases/settings.db",
        "/com.android.providers.tasks/databases/tasks.db",
        "/com.android.providers.telephony/optable.db",
        "/com.android.providers.telephony/databases/mmssms.db",
        "/com.android.providers.telephony/databases/nwk_info.db",
        "/com.android.providers.telephony/databases/telephony.db",
        "/com.android.providers.telephony/databases/tether_dun.db",
        "/com.android.providers.userdictionary/databases/user_dict.db",
        "/com.android.settings/databases/webview.db",
        "/com.android.settings/databases/webviewCache.db",
        "/com.android.vending/databases/billing4.db",
        "/com.android.vending/databases/market_assets.db",
        "/com.android.vending/databases/suggestions.db",
        "/com.android.vending/databases/webview.db",
        "/com.android.vending/databases/webviewCache.db"
    };

    /**
     * This method triggers activities that should cause database writes.
     * The goal of this is to try to make potentially short-lived journal
     * files show up.
     */
    private void doActivity() {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse("http:///localhost"));
        webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(webIntent);
        Intent dictIntent = new Intent("android.settings.USER_DICTIONARY_SETTINGS");
        dictIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(dictIntent);
    }

    private void checkDatabases(String suffix) {
        String msg = " is world readable. Please set its permissions to 600"
            + " by setting -DSQLITE_DEFAULT_FILE_PERMISSIONS=0600 in external/sqlite/dist/"
            + "Android.mk; see CVE-2011-3901.";
        String base = Environment.getDataDirectory().getAbsolutePath();
        for(int i=REPEAT; i > 0; i--) {
            doActivity();
            for (String name : DATABASES) {
                name = base + "/data" + name + suffix;
                File f = new File(name);
                assertFalse(name + msg, f.canRead());
            }
        }
    }

    public void testJournal() {
        checkDatabases("-journal");
    }

    public void testWal() {
        checkDatabases("-wal");
    }

    public void testShm() {
        checkDatabases("-shm");
    }
}

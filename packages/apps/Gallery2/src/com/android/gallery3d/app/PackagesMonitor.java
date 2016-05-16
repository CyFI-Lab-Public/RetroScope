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

package com.android.gallery3d.app;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.LightCycleHelper;

public class PackagesMonitor extends BroadcastReceiver {
    public static final String KEY_PACKAGES_VERSION  = "packages-version";

    public synchronized static int getPackagesVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(KEY_PACKAGES_VERSION, 1);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        intent.setClass(context, AsyncService.class);
        context.startService(intent);
    }

    public static class AsyncService extends IntentService {
        public AsyncService() {
            super("GalleryPackagesMonitorAsync");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            onReceiveAsync(this, intent);
        }
    }

    // Runs in a background thread.
    private static void onReceiveAsync(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int version = prefs.getInt(KEY_PACKAGES_VERSION, 1);
        prefs.edit().putInt(KEY_PACKAGES_VERSION, version + 1).commit();

        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            PicasaSource.onPackageAdded(context, packageName);
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            PicasaSource.onPackageRemoved(context, packageName);
        } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
            PicasaSource.onPackageChanged(context, packageName);
        }
    }
}

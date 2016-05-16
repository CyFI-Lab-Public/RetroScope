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

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.VoicemailContract.Status;
import android.provider.VoicemailContract.Voicemails;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;

/**
 * A service that cleans up voicemail related data for packages that are uninstalled.
 */
public class VoicemailCleanupService extends IntentService {
    private static final String TAG = "VoicemailCleanupService";

    public VoicemailCleanupService() {
        super("VoicemailCleanupService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        handleIntentInternal(intent, getContentResolver());
    }

    @VisibleForTesting
    void handleIntentInternal(Intent intent,
            ContentResolver contentResolver) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) &&
                !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            String packageUninstalled = intent.getData().getSchemeSpecificPart();
            Log.d(TAG, "Cleaning up data for package: " + packageUninstalled);
            // Delete both voicemail content and voicemail status entries for this package.
            contentResolver.delete(Voicemails.buildSourceUri(packageUninstalled), null, null);
            contentResolver.delete(Status.buildSourceUri(packageUninstalled), null, null);
        } else {
            Log.w(TAG, "Unexpected intent: " + intent);
        }
    }
}

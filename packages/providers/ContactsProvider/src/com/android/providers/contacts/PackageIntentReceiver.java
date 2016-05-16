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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * Package intent receiver that invokes {@link ContactsProvider2#onPackageChanged} to update
 * the contact directory list.
 */
public class PackageIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri packageUri = intent.getData();
        String packageName = packageUri.getSchemeSpecificPart();
        IContentProvider iprovider =
            context.getContentResolver().acquireProvider(ContactsContract.AUTHORITY);
        ContentProvider provider = ContentProvider.coerceToLocalContentProvider(iprovider);
        if (provider instanceof ContactsProvider2) {
            ((ContactsProvider2)provider).onPackageChanged(packageName);
        }
        handlePackageChangedForVoicemail(context, intent);
    }

    private void handlePackageChangedForVoicemail(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) &&
                !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            // Forward the intent to the cleanup service for handling the event.
            Intent intentToForward = new Intent(context, VoicemailCleanupService.class);
            intentToForward.setData(intent.getData());
            intentToForward.setAction(intent.getAction());
            intentToForward.putExtras(intent.getExtras());
            context.startService(intentToForward);
        }
    }
}

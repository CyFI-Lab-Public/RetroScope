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
package com.android.quicksearchbox;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

/**
 * Voice Search integration.
 */
public class VoiceSearch {

    private static final String TAG = "QSB.VoiceSearch";

    private final Context mContext;

    public VoiceSearch(Context context) {
        mContext = context;
    }

    protected Context getContext() {
        return mContext;
    }

    public boolean shouldShowVoiceSearch() {
        return isVoiceSearchAvailable();
    }

    protected Intent createVoiceSearchIntent() {
        return new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
    }

    private ResolveInfo getResolveInfo() {
        Intent intent = createVoiceSearchIntent();
        ResolveInfo ri = mContext.getPackageManager().
                resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return ri;
    }

    public boolean isVoiceSearchAvailable() {
        return getResolveInfo() != null;
    }

    public Intent createVoiceWebSearchIntent(Bundle appData) {
        if (!isVoiceSearchAvailable()) return null;
        Intent intent = createVoiceSearchIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        if (appData != null) {
            intent.putExtra(SearchManager.APP_DATA, appData);
        }
        return intent;
    }

    /**
     * Create an intent to launch the voice search help screen, if any exists.
     * @return The intent, or null.
     */
    public Intent createVoiceSearchHelpIntent() {
        return null;
    }

    /**
     * Gets the {@code versionCode} of the currently installed voice search package.
     *
     * @return The {@code versionCode} of voiceSearch, or 0 if none is installed.
     */
    public int getVersion() {
        ResolveInfo ri = getResolveInfo();
        if (ri == null) return 0;
        ComponentInfo ci = ri.activityInfo != null ? ri.activityInfo : ri.serviceInfo;
        try {
            return getContext().getPackageManager().getPackageInfo(ci.packageName, 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Cannot find voice search package " + ci.packageName, e);
            return 0;
        }
    }

    public ComponentName getComponent() {
        return createVoiceSearchIntent().resolveActivity(getContext().getPackageManager());
    }
}

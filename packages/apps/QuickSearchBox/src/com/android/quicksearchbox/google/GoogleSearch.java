/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.quicksearchbox.google;

import com.android.common.Search;
import com.android.quicksearchbox.QsbApplication;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * This class is purely here to get search queries and route them to
 * the global {@link Intent#ACTION_WEB_SEARCH}.
 */
public class GoogleSearch extends Activity {
    private static final String TAG = "GoogleSearch";
    private static final boolean DBG = false;

    // Used to figure out which domain to base search requests
    // on.
    private SearchBaseUrlHelper mSearchDomainHelper;

    // "source" parameter for Google search requests from unknown sources (e.g. apps). This will get
    // prefixed with the string 'android-' before being sent on the wire.
    final static String GOOGLE_SEARCH_SOURCE_UNKNOWN = "unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;

        // This should probably be moved so as to
        // send out the request to /checksearchdomain as early as possible.
        mSearchDomainHelper = QsbApplication.get(this).getSearchBaseUrlHelper();

        if (Intent.ACTION_WEB_SEARCH.equals(action) || Intent.ACTION_SEARCH.equals(action)) {
            handleWebSearchIntent(intent);
        }

        finish();
    }

    /**
     * Construct the language code (hl= paramater) for the given locale.
     */
    public static String getLanguage(Locale locale) {
        String language = locale.getLanguage();
        StringBuilder hl = new StringBuilder(language);
        String country = locale.getCountry();

        if (!TextUtils.isEmpty(country) && useLangCountryHl(language, country)) {
            hl.append('-');
            hl.append(country);
        }

        if (DBG) Log.d(TAG, "language " + language + ", country " + country + " -> hl=" + hl);
        return hl.toString();
    }

    // TODO: This is a workaround for bug 3232296. When that is fixed, this method can be removed.
    private static boolean useLangCountryHl(String language, String country) {
        // lang-country is currently only supported for a small number of locales
        if ("en".equals(language)) {
            return "GB".equals(country);
        } else if ("zh".equals(language)) {
            return "CN".equals(country) || "TW".equals(country);
        } else if ("pt".equals(language)) {
            return "BR".equals(country) || "PT".equals(country);
        } else {
            return false;
        }
    }

    private void handleWebSearchIntent(Intent intent) {
        Intent launchUriIntent = createLaunchUriIntentFromSearchIntent(intent);
        PendingIntent pending =
            intent.getParcelableExtra(SearchManager.EXTRA_WEB_SEARCH_PENDINGINTENT);
        if (pending == null || !launchPendingIntent(pending, launchUriIntent)) {
            launchIntent(launchUriIntent);
        }
    }

    private Intent createLaunchUriIntentFromSearchIntent(Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "Got search intent with no query.");
            return null;
        }

        // If the caller specified a 'source' url parameter, use that and if not use default.
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);
        String source = GOOGLE_SEARCH_SOURCE_UNKNOWN;
        if (appSearchData != null) {
            source = appSearchData.getString(Search.SOURCE);
        }
        
        // The browser can pass along an application id which it uses to figure out which
        // window to place a new search into. So if this exists, we'll pass it back to
        // the browser. Otherwise, add our own package name as the application id, so that
        // the browser can organize all searches launched from this provider together.
        String applicationId = intent.getStringExtra(Browser.EXTRA_APPLICATION_ID);
        if (applicationId == null) {
            applicationId = getPackageName();
        }

        try {
            String searchUri = mSearchDomainHelper.getSearchBaseUrl()
                    + "&source=android-" + source
                    + "&q=" + URLEncoder.encode(query, "UTF-8");
            Intent launchUriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri));
            launchUriIntent.putExtra(Browser.EXTRA_APPLICATION_ID, applicationId);
            launchUriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return launchUriIntent;
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Error", e);
            return null;
        }

    }

    private void launchIntent(Intent intent) {
        try {
            Log.i(TAG, "Launching intent: " + intent.toUri(0));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "No activity found to handle: " + intent);
        }
    }

    private boolean launchPendingIntent(PendingIntent pending, Intent fillIn) {
        try {
            pending.send(this, Activity.RESULT_OK, fillIn);
            return true;
        } catch (PendingIntent.CanceledException ex) {
            Log.i(TAG, "Pending intent cancelled: " + pending);
            return false;
        }
    }

}

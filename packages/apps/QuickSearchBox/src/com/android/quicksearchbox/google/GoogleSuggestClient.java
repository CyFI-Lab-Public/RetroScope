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

package com.android.quicksearchbox.google;

import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.android.quicksearchbox.Config;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.util.NamedTaskExecutor;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Use network-based Google Suggests to provide search suggestions.
 */
public class GoogleSuggestClient extends AbstractGoogleSource {

    private static final boolean DBG = false;
    private static final String LOG_TAG = "GoogleSearch";

    private static final String USER_AGENT = "Android/" + Build.VERSION.RELEASE;
    private String mSuggestUri;

    // TODO: this should be defined somewhere
    private static final String HTTP_TIMEOUT = "http.conn-manager.timeout";

    private final HttpClient mHttpClient;

    public GoogleSuggestClient(Context context, Handler uiThread,
            NamedTaskExecutor iconLoader, Config config) {
        super(context, uiThread, iconLoader);
        mHttpClient = AndroidHttpClient.newInstance(USER_AGENT, context);
        HttpParams params = mHttpClient.getParams();
        params.setLongParameter(HTTP_TIMEOUT, config.getHttpConnectTimeout());

        // NOTE:  Do not look up the resource here;  Localization changes may not have completed
        // yet (e.g. we may still be reading the SIM card).
        mSuggestUri = null;
    }

    @Override
    public ComponentName getIntentComponent() {
        return new ComponentName(getContext(), GoogleSearch.class);
    }

    @Override
    public SourceResult queryInternal(String query) {
        return query(query);
    }

    @Override
    public SourceResult queryExternal(String query) {
        return query(query);
    }

    /**
     * Queries for a given search term and returns a cursor containing
     * suggestions ordered by best match.
     */
    private SourceResult query(String query) {
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        if (!isNetworkConnected()) {
            Log.i(LOG_TAG, "Not connected to network.");
            return null;
        }
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            if (mSuggestUri == null) {
                Locale l = Locale.getDefault();
                String language = GoogleSearch.getLanguage(l);
                mSuggestUri = getContext().getResources().getString(R.string.google_suggest_base,
                                                                    language);
            }

            String suggestUri = mSuggestUri + encodedQuery;
            if (DBG) Log.d(LOG_TAG, "Sending request: " + suggestUri);
            HttpGet method = new HttpGet(suggestUri);
            HttpResponse response = mHttpClient.execute(method);
            if (response.getStatusLine().getStatusCode() == 200) {

                /* Goto http://www.google.com/complete/search?json=true&q=foo
                 * to see what the data format looks like. It's basically a json
                 * array containing 4 other arrays. We only care about the middle
                 * 2 which contain the suggestions and their popularity.
                 */
                JSONArray results = new JSONArray(EntityUtils.toString(response.getEntity()));
                JSONArray suggestions = results.getJSONArray(1);
                JSONArray popularity = results.getJSONArray(2);
                if (DBG) Log.d(LOG_TAG, "Got " + suggestions.length() + " results");
                return new GoogleSuggestCursor(this, query, suggestions, popularity);
            } else {
                if (DBG) Log.d(LOG_TAG, "Request failed " + response.getStatusLine());
            }
        } catch (UnsupportedEncodingException e) {
            Log.w(LOG_TAG, "Error", e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error", e);
        } catch (JSONException e) {
            Log.w(LOG_TAG, "Error", e);
        }
        return null;
    }

    @Override
    public SuggestionCursor refreshShortcut(String shortcutId, String oldExtraData) {
        return null;
    }

    private boolean isNetworkConnected() {
        NetworkInfo networkInfo = getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivity =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return null;
        }
        return connectivity.getActiveNetworkInfo();
    }

    private static class GoogleSuggestCursor extends AbstractGoogleSourceResult {

        /* Contains the actual suggestions */
        private final JSONArray mSuggestions;

        /* This contains the popularity of each suggestion
         * i.e. 165,000 results. It's not related to sorting.
         */
        private final JSONArray mPopularity;

        public GoogleSuggestCursor(Source source, String userQuery,
                JSONArray suggestions, JSONArray popularity) {
            super(source, userQuery);
            mSuggestions = suggestions;
            mPopularity = popularity;
        }

        @Override
        public int getCount() {
            return mSuggestions.length();
        }

        @Override
        public String getSuggestionQuery() {
            try {
                return mSuggestions.getString(getPosition());
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error parsing response: " + e);
                return null;
            }
        }

        @Override
        public String getSuggestionText2() {
            try {
                return mPopularity.getString(getPosition());
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error parsing response: " + e);
                return null;
            }
        }
    }
}

/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.providers;

import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;

import com.android.mail.R;
import com.android.mail.utils.MatrixCursorWithCachedColumns;

import java.util.ArrayList;

/**
 * Simple extension / instantiation of SearchRecentSuggestionsProvider, independent
 * of mail account or account capabilities.  Offers suggestions from historical searches
 * and contact email addresses on the device. The authority fro for this provider is obtained
 * through the MailAppProvider as follows:
 * final String AUTHORITY = MailAppProvider.getInstance().getSuggestionAuthority()
 * It needs to be done after the MailAppProvider is constructed.
 */
public class SuggestionsProvider extends SearchRecentSuggestionsProvider {
    /**
     * Mode used in the constructor of SuggestionsProvider.
     */
    public final static int MODE = DATABASE_MODE_QUERIES;
    /**
     * Columns over the contacts database that we return in the {@link ContactsCursor}.
     */
    private static final String[] CONTACTS_COLUMNS = new String[] {
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_QUERY,
            SearchManager.SUGGEST_COLUMN_ICON_1
    };
    private ArrayList<String> mFullQueryTerms;
    /** Used for synchronization */
    private final Object mTermsLock = new Object();
    private final static String[] sContract = new String[] {
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.DATA
    };
    /**
     * Minimum length of query before we start showing contacts suggestions.
     */
    static private final int MIN_QUERY_LENGTH_FOR_CONTACTS = 2;

    public SuggestionsProvider() {
        super();
    }

    @Override
    public boolean onCreate() {
        final String authority = getContext().getString(R.string.suggestions_authority);
        setupSuggestions(authority, MODE);
        super.onCreate();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String query = selectionArgs[0];
        MergeCursor mergeCursor = null;

        synchronized (mTermsLock) {
            mFullQueryTerms = null;
            super.setFullQueryTerms(mFullQueryTerms);
        }
        // Get the custom suggestions for email which are from, to, etc.
        if (query != null) {
            // Tokenize the query.
            String[] tokens = TextUtils.split(query,
                    SearchRecentSuggestionsProvider.QUERY_TOKEN_SEPARATOR);
            // There are multiple tokens, so query on the last token only.
            if (tokens != null && tokens.length > 1) {
                query = tokens[tokens.length - 1];
                // Leave off the last token since we are auto completing on it.
                synchronized (mTermsLock) {
                    mFullQueryTerms = new ArrayList<String>();
                    for (int i = 0, size = tokens.length - 1; i < size; i++) {
                        mFullQueryTerms.add(tokens[i]);
                    }
                    super.setFullQueryTerms(mFullQueryTerms);
                }
            } else {
                // Strip excess whitespace.
                query = query.trim();
            }
            ArrayList<Cursor> cursors = new ArrayList<Cursor>();
            // Pass query; at this point it is either the last term OR the
            // only term.
            cursors.add(super.query(uri, projection, selection, new String[] { query }, sortOrder));

            if (query.length() >= MIN_QUERY_LENGTH_FOR_CONTACTS) {
                cursors.add(new ContactsCursor().query(query));
            }
            mergeCursor = new MergeCursor(cursors.toArray(new Cursor[cursors.size()]));
        }
        return mergeCursor;
    }

    /**
     * Utility class to return a cursor over the contacts database
     */
    private final class ContactsCursor extends MatrixCursorWithCachedColumns {
        private final Context mContext;
        public ContactsCursor() {
            super(CONTACTS_COLUMNS);
            mContext = getContext();
        }

        /**
         * Searches over the contacts cursor with the specified query as the starting characters to
         * match.
         * @param query
         * @return a cursor over the contacts database with the contacts matching the query.
         */
        public ContactsCursor query(String query) {
            final Uri contactsUri = Uri.withAppendedPath(
                    ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(query));
            final Cursor cursor = mContext.getContentResolver().query(
                    contactsUri, sContract, null, null, null);
            // We don't want to show a contact icon here. Leaving the SEARCH_ICON_1 field
            // empty causes inconsistent behavior because the cursor is merged with the
            // historical suggestions, which have an icon.  The solution is to show an empty icon
            // instead.
            final String emptyIcon = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                    + mContext.getPackageName() + "/" + R.drawable.empty;
            if (cursor != null) {
                final int nameIndex = cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME);
                final int addressIndex = cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                String match;
                while (cursor.moveToNext()) {
                    match = cursor.getString(nameIndex);
                    match = !TextUtils.isEmpty(match) ? match : cursor.getString(addressIndex);
                    // The order of fields is:
                    // _ID, SUGGEST_COLUMN_TEXT_1, SUGGEST_COLUMN_QUERY, SUGGEST_COLUMN_ICON_1
                    addRow(new Object[] {0, match, createQuery(match), emptyIcon});
                }
                cursor.close();
            }
            return this;
        }
    }

    private String createQuery(String inMatch) {
        final StringBuilder query = new StringBuilder();
        if (mFullQueryTerms != null) {
            synchronized (mTermsLock) {
                for (int i = 0, size = mFullQueryTerms.size(); i < size; i++) {
                    query.append(mFullQueryTerms.get(i)).append(QUERY_TOKEN_SEPARATOR);
                }
            }
        }
        // Append the match as well.
        query.append(inMatch);
        // Example:
        // Search terms in the searchbox are : "pdf test*"
        // Contacts database contains: test@tester.com, test@other.com
        // If the user taps "test@tester.com", the query passed with
        // ACTION_SEARCH is:
        // "pdf test@tester.com"
        // If the user taps "test@other.com", the query passed with
        // ACTION_SEARCH is:
        // "pdf test@other.com"
        return query.toString();
    }
}
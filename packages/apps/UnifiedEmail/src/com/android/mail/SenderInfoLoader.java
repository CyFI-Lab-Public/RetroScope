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

package com.android.mail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A {@link Loader} to look up presence, contact URI, and photo data for a set of email
 * addresses.
 *
 */
public class SenderInfoLoader extends AsyncTaskLoader<ImmutableMap<String, ContactInfo>> {

    private static final String[] DATA_COLS = new String[] {
        Email._ID,                  // 0
        Email.DATA,                 // 1
        Email.CONTACT_PRESENCE,     // 2
        Email.CONTACT_ID,           // 3
        Email.PHOTO_ID,             // 4
    };
    private static final int DATA_EMAIL_COLUMN = 1;
    private static final int DATA_STATUS_COLUMN = 2;
    private static final int DATA_CONTACT_ID_COLUMN = 3;
    private static final int DATA_PHOTO_ID_COLUMN = 4;

    private static final String[] PHOTO_COLS = new String[] { Photo._ID, Photo.PHOTO };
    private static final int PHOTO_PHOTO_ID_COLUMN = 0;
    private static final int PHOTO_PHOTO_COLUMN = 1;

    /**
     * Limit the query params to avoid hitting the maximum of 99. We choose a number smaller than
     * 99 since the contacts provider may wrap our query in its own and insert more params.
     */
    static final int MAX_QUERY_PARAMS = 75;

    private final Set<String> mSenders;

    public SenderInfoLoader(Context context, Set<String> senders) {
        super(context);
        mSenders = senders;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public ImmutableMap<String, ContactInfo> loadInBackground() {
        if (mSenders == null || mSenders.isEmpty()) {
            return null;
        }

        return loadContactPhotos(
                getContext().getContentResolver(), mSenders, true /* decodeBitmaps */);
    }

    /**
     * Loads contact photos from the ContentProvider.
     * @param resolver {@link ContentResolver} to use in queries to the ContentProvider.
     * @param senderSet The email addresses of the sender images to return.
     * @param decodeBitmaps If {@code true}, decode the bitmaps and put them into
     *                      {@link ContactInfo}. Otherwise, just put the raw bytes of the photo
     *                      into the {@link ContactInfo}.
     * @return A mapping of email addresses to {@link ContactInfo}s. The {@link ContactInfo} will
     * contain either a byte array or an actual decoded bitmap for the sender image.
     */
    public static ImmutableMap<String, ContactInfo> loadContactPhotos(
            final ContentResolver resolver, final Set<String> senderSet,
            final boolean decodeBitmaps) {
        Cursor cursor = null;

        Map<String, ContactInfo> results = Maps.newHashMap();

        // temporary structures
        Map<Long, Pair<String, ContactInfo>> photoIdMap = Maps.newHashMap();
        ArrayList<String> photoIdsAsStrings = new ArrayList<String>();
        ArrayList<String> senders = getTruncatedQueryParams(senderSet);

        // Build first query
        StringBuilder query = new StringBuilder()
                .append(Data.MIMETYPE).append("='").append(Email.CONTENT_ITEM_TYPE)
                .append("' AND ").append(Email.DATA).append(" IN (");
        appendQuestionMarks(query, senders);
        query.append(')');

        try {
            cursor = resolver.query(Data.CONTENT_URI, DATA_COLS,
                    query.toString(), toStringArray(senders), null /* sortOrder */);

            if (cursor == null) {
                return null;
            }

            int i = -1;
            while (cursor.moveToPosition(++i)) {
                String email = cursor.getString(DATA_EMAIL_COLUMN);
                long contactId = cursor.getLong(DATA_CONTACT_ID_COLUMN);
                Integer status = null;
                Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

                if (!cursor.isNull(DATA_STATUS_COLUMN)) {
                    status = cursor.getInt(DATA_STATUS_COLUMN);
                }

                ContactInfo result = new ContactInfo(contactUri, status);

                if (!cursor.isNull(DATA_PHOTO_ID_COLUMN)) {
                    long photoId = cursor.getLong(DATA_PHOTO_ID_COLUMN);
                    photoIdsAsStrings.add(Long.toString(photoId));
                    photoIdMap.put(photoId, Pair.create(email, result));
                }
                results.put(email, result);
            }
            cursor.close();

            if (photoIdsAsStrings.isEmpty()) {
                return ImmutableMap.copyOf(results);
            }

            // Build second query: photoIDs->blobs
            // based on photo batch-select code in ContactPhotoManager
            photoIdsAsStrings = getTruncatedQueryParams(photoIdsAsStrings);
            query.setLength(0);
            query.append(Photo._ID).append(" IN (");
            appendQuestionMarks(query, photoIdsAsStrings);
            query.append(')');

            cursor = resolver.query(Data.CONTENT_URI, PHOTO_COLS,
                    query.toString(), toStringArray(photoIdsAsStrings), null /* sortOrder */);

            if (cursor == null) {
                return ImmutableMap.copyOf(results);
            }

            i = -1;
            while (cursor.moveToPosition(++i)) {
                byte[] photoBytes = cursor.getBlob(PHOTO_PHOTO_COLUMN);
                if (photoBytes == null) {
                    continue;
                }

                long photoId = cursor.getLong(PHOTO_PHOTO_ID_COLUMN);
                Pair<String, ContactInfo> prev = photoIdMap.get(photoId);
                String email = prev.first;
                ContactInfo prevResult = prev.second;

                if (decodeBitmaps) {
                    Bitmap photo = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                    // overwrite existing photo-less result
                    results.put(email,
                            new ContactInfo(prevResult.contactUri, prevResult.status, photo));
                } else {
                    // overwrite existing photoBytes-less result
                    results.put(email, new ContactInfo(
                            prevResult.contactUri, prevResult.status, photoBytes));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return ImmutableMap.copyOf(results);
    }

    static ArrayList<String> getTruncatedQueryParams(Collection<String> params) {
        int truncatedLen = Math.min(params.size(), MAX_QUERY_PARAMS);
        ArrayList<String> truncated = new ArrayList<String>(truncatedLen);

        int copied = 0;
        for (String param : params) {
            truncated.add(param);
            copied++;
            if (copied >= truncatedLen) {
                break;
            }
        }

        return truncated;
    }

    private static String[] toStringArray(Collection<String> items) {
        return items.toArray(new String[items.size()]);
    }

    static void appendQuestionMarks(StringBuilder query, Iterable<?> items) {
        boolean first = true;
        for (Object item : items) {
            if (first) {
                first = false;
            } else {
                query.append(',');
            }
            query.append('?');
        }
    }

}

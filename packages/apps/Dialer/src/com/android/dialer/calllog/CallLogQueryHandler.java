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
 * limitations under the License.
 */

package com.android.dialer.calllog;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.provider.VoicemailContract.Status;
import android.util.Log;

import com.android.common.io.MoreCloseables;
import com.android.contacts.common.database.NoNullCursorAsyncQueryHandler;
import com.android.dialer.voicemail.VoicemailStatusHelperImpl;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.annotation.concurrent.GuardedBy;

/** Handles asynchronous queries to the call log. */
public class CallLogQueryHandler extends NoNullCursorAsyncQueryHandler {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String TAG = "CallLogQueryHandler";
    private static final int NUM_LOGS_TO_DISPLAY = 1000;

    /** The token for the query to fetch the old entries from the call log. */
    private static final int QUERY_CALLLOG_TOKEN = 54;
    /** The token for the query to mark all missed calls as old after seeing the call log. */
    private static final int UPDATE_MARK_AS_OLD_TOKEN = 55;
    /** The token for the query to mark all new voicemails as old. */
    private static final int UPDATE_MARK_VOICEMAILS_AS_OLD_TOKEN = 56;
    /** The token for the query to mark all missed calls as read after seeing the call log. */
    private static final int UPDATE_MARK_MISSED_CALL_AS_READ_TOKEN = 57;
    /** The token for the query to fetch voicemail status messages. */
    private static final int QUERY_VOICEMAIL_STATUS_TOKEN = 58;

    private final int mLogLimit;

    /**
     * Call type similar to Calls.INCOMING_TYPE used to specify all types instead of one particular
     * type.
     */
    public static final int CALL_TYPE_ALL = -1;

    private final WeakReference<Listener> mListener;

    /** The cursor containing the old calls, or null if they have not yet been fetched. */
    @GuardedBy("this") private Cursor mCallLogCursor;
    /**
     * The identifier of the latest calls request.
     * <p>
     * A request for the list of calls requires two queries and hence the two cursor
     * and {@link #mCallLogCursor} above, corresponding to {@link #QUERY_CALLLOG_TOKEN}.
     * <p>
     * When a new request is about to be started, existing cursors are closed. However, it is
     * possible that one of the queries completes after the new request has started. This means that
     * we might merge two cursors that do not correspond to the same request. Moreover, this may
     * lead to a resource leak if the same query completes and we override the cursor without
     * closing it first.
     * <p>
     * To make sure we only join two cursors from the same request, we use this variable to store
     * the request id of the latest request and make sure we only process cursors corresponding to
     * the this request.
     */
    @GuardedBy("this") private int mCallsRequestId;

    /**
     * Simple handler that wraps background calls to catch
     * {@link SQLiteException}, such as when the disk is full.
     */
    protected class CatchingWorkerHandler extends AsyncQueryHandler.WorkerHandler {
        public CatchingWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                // Perform same query while catching any exceptions
                super.handleMessage(msg);
            } catch (SQLiteDiskIOException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            } catch (SQLiteFullException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            } catch (SQLiteDatabaseCorruptException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            }
        }
    }

    @Override
    protected Handler createHandler(Looper looper) {
        // Provide our special handler that catches exceptions
        return new CatchingWorkerHandler(looper);
    }

    public CallLogQueryHandler(ContentResolver contentResolver, Listener listener) {
        this(contentResolver, listener, -1);
    }

    public CallLogQueryHandler(ContentResolver contentResolver, Listener listener, int limit) {
        super(contentResolver);
        mListener = new WeakReference<Listener>(listener);
        mLogLimit = limit;
    }

    /**
     * Fetches the list of calls from the call log for a given type.
     * This call ignores the new or old state.
     * <p>
     * It will asynchronously update the content of the list view when the fetch completes.
     */
    public void fetchCalls(int callType, long newerThan) {
        cancelFetch();
        int requestId = newCallsRequest();
        fetchCalls(QUERY_CALLLOG_TOKEN, requestId, callType, false /* newOnly */, newerThan);
    }

    public void fetchCalls(int callType) {
        fetchCalls(callType, 0);
    }

    public void fetchVoicemailStatus() {
        startQuery(QUERY_VOICEMAIL_STATUS_TOKEN, null, Status.CONTENT_URI,
                VoicemailStatusHelperImpl.PROJECTION, null, null, null);
    }

    /** Fetches the list of calls in the call log. */
    private void fetchCalls(int token, int requestId, int callType, boolean newOnly,
            long newerThan) {
        // We need to check for NULL explicitly otherwise entries with where READ is NULL
        // may not match either the query or its negation.
        // We consider the calls that are not yet consumed (i.e. IS_READ = 0) as "new".
        StringBuilder where = new StringBuilder();
        List<String> selectionArgs = Lists.newArrayList();

        if (newOnly) {
            where.append(Calls.NEW);
            where.append(" = 1");
        }

        if (callType > CALL_TYPE_ALL) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            // Add a clause to fetch only items of type voicemail.
            where.append(String.format("(%s = ?)", Calls.TYPE));
            // Add a clause to fetch only items newer than the requested date
            selectionArgs.add(Integer.toString(callType));
        }

        if (newerThan > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(String.format("(%s > ?)", Calls.DATE));
            selectionArgs.add(Long.toString(newerThan));
        }

        final int limit = (mLogLimit == -1) ? NUM_LOGS_TO_DISPLAY : mLogLimit;
        final String selection = where.length() > 0 ? where.toString() : null;
        Uri uri = Calls.CONTENT_URI_WITH_VOICEMAIL.buildUpon()
                .appendQueryParameter(Calls.LIMIT_PARAM_KEY, Integer.toString(limit))
                .build();
        startQuery(token, requestId, uri,
                CallLogQuery._PROJECTION, selection, selectionArgs.toArray(EMPTY_STRING_ARRAY),
                Calls.DEFAULT_SORT_ORDER);
    }

    /** Cancel any pending fetch request. */
    private void cancelFetch() {
        cancelOperation(QUERY_CALLLOG_TOKEN);
    }

    /** Updates all new calls to mark them as old. */
    public void markNewCallsAsOld() {
        // Mark all "new" calls as not new anymore.
        StringBuilder where = new StringBuilder();
        where.append(Calls.NEW);
        where.append(" = 1");

        ContentValues values = new ContentValues(1);
        values.put(Calls.NEW, "0");

        startUpdate(UPDATE_MARK_AS_OLD_TOKEN, null, Calls.CONTENT_URI_WITH_VOICEMAIL,
                values, where.toString(), null);
    }

    /** Updates all new voicemails to mark them as old. */
    public void markNewVoicemailsAsOld() {
        // Mark all "new" voicemails as not new anymore.
        StringBuilder where = new StringBuilder();
        where.append(Calls.NEW);
        where.append(" = 1 AND ");
        where.append(Calls.TYPE);
        where.append(" = ?");

        ContentValues values = new ContentValues(1);
        values.put(Calls.NEW, "0");

        startUpdate(UPDATE_MARK_VOICEMAILS_AS_OLD_TOKEN, null, Calls.CONTENT_URI_WITH_VOICEMAIL,
                values, where.toString(), new String[]{ Integer.toString(Calls.VOICEMAIL_TYPE) });
    }

    /** Updates all missed calls to mark them as read. */
    public void markMissedCallsAsRead() {
        // Mark all "new" calls as not new anymore.
        StringBuilder where = new StringBuilder();
        where.append(Calls.IS_READ).append(" = 0");
        where.append(" AND ");
        where.append(Calls.TYPE).append(" = ").append(Calls.MISSED_TYPE);

        ContentValues values = new ContentValues(1);
        values.put(Calls.IS_READ, "1");

        startUpdate(UPDATE_MARK_MISSED_CALL_AS_READ_TOKEN, null, Calls.CONTENT_URI, values,
                where.toString(), null);
    }

    /**
     * Start a new request and return its id. The request id will be used as the cookie for the
     * background request.
     * <p>
     * Closes any open cursor that has not yet been sent to the requester.
     */
    private synchronized int newCallsRequest() {
        MoreCloseables.closeQuietly(mCallLogCursor);
        mCallLogCursor = null;
        return ++mCallsRequestId;
    }

    @Override
    protected void onNotNullableQueryComplete(int token, Object cookie, Cursor cursor) {
        if (token == QUERY_CALLLOG_TOKEN) {
            int requestId = ((Integer) cookie).intValue();
            if (requestId != mCallsRequestId) {
                // Ignore this query since it does not correspond to the latest request.
                return;
            }

            // Store the returned cursor.
            MoreCloseables.closeQuietly(mCallLogCursor);
            mCallLogCursor = cursor;
        } else if (token == QUERY_VOICEMAIL_STATUS_TOKEN) {
            updateVoicemailStatus(cursor);
            return;
        } else {
            Log.w(TAG, "Unknown query completed: ignoring: " + token);
            return;
        }

        if (mCallLogCursor != null) {
            updateAdapterData(mCallLogCursor);
            mCallLogCursor = null;
        }
    }

    /**
     * Updates the adapter in the call log fragment to show the new cursor data.
     */
    private void updateAdapterData(Cursor combinedCursor) {
        final Listener listener = mListener.get();
        if (listener != null) {
            listener.onCallsFetched(combinedCursor);
        }
    }

    private void updateVoicemailStatus(Cursor statusCursor) {
        final Listener listener = mListener.get();
        if (listener != null) {
            listener.onVoicemailStatusFetched(statusCursor);
        }
    }

    /** Listener to completion of various queries. */
    public interface Listener {
        /** Called when {@link CallLogQueryHandler#fetchVoicemailStatus()} completes. */
        void onVoicemailStatusFetched(Cursor statusCursor);

        /**
         * Called when {@link CallLogQueryHandler#fetchCalls(int)}complete.
         */
        void onCallsFetched(Cursor combinedCursor);
    }
}

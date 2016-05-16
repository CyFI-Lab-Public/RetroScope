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

package com.android.mail.browse;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;

import com.android.mail.content.ThreadSafeCursorWrapper;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.FolderList;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.ConversationListQueryParameters;
import com.android.mail.providers.UIProvider.ConversationOperations;
import com.android.mail.ui.ConversationListFragment;
import com.android.mail.utils.DrawIdler;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.NotificationActionUtils;
import com.android.mail.utils.NotificationActionUtils.NotificationAction;
import com.android.mail.utils.NotificationActionUtils.NotificationActionType;
import com.android.mail.utils.Utils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ConversationCursor is a wrapper around a conversation list cursor that provides update/delete
 * caching for quick UI response. This is effectively a singleton class, as the cache is
 * implemented as a static HashMap.
 */
public final class ConversationCursor implements Cursor, ConversationCursorOperationListener,
        DrawIdler.IdleListener {

    public static final String LOG_TAG = "ConvCursor";
    /** Turn to true for debugging. */
    private static final boolean DEBUG = false;
    /** A deleted row is indicated by the presence of DELETED_COLUMN in the cache map */
    private static final String DELETED_COLUMN = "__deleted__";
    /** An row cached during a requery is indicated by the presence of REQUERY_COLUMN in the map */
    private static final String UPDATE_TIME_COLUMN = "__updatetime__";
    /**
     * A sentinel value for the "index" of the deleted column; it's an int that is otherwise invalid
     */
    private static final int DELETED_COLUMN_INDEX = -1;
    /**
     * If a cached value within 10 seconds of a refresh(), preserve it. This time has been
     * chosen empirically (long enough for UI changes to propagate in any reasonable case)
     */
    private static final long REQUERY_ALLOWANCE_TIME = 10000L;

    /**
     * The index of the Uri whose data is reflected in the cached row. Updates/Deletes to this Uri
     * are cached
     */
    private static final int URI_COLUMN_INDEX = UIProvider.CONVERSATION_URI_COLUMN;

    private static final boolean DEBUG_DUPLICATE_KEYS = true;

    /** The resolver for the cursor instantiator's context */
    private final ContentResolver mResolver;

    /** Our sequence count (for changes sent to underlying provider) */
    private static int sSequence = 0;
    @VisibleForTesting
    static ConversationProvider sProvider;

    /** The cursor underlying the caching cursor */
    @VisibleForTesting
    UnderlyingCursorWrapper mUnderlyingCursor;
    /** The new cursor obtained via a requery */
    private volatile UnderlyingCursorWrapper mRequeryCursor;
    /** A mapping from Uri to updated ContentValues */
    private final HashMap<String, ContentValues> mCacheMap = new HashMap<String, ContentValues>();
    /** Cache map lock (will be used only very briefly - few ms at most) */
    private final Object mCacheMapLock = new Object();
    /** The listeners registered for this cursor */
    private final List<ConversationListener> mListeners = Lists.newArrayList();
    /**
     * The ConversationProvider instance // The runnable executing a refresh (query of underlying
     * provider)
     */
    private RefreshTask mRefreshTask;
    /** Set when we've sent refreshReady() to listeners */
    private boolean mRefreshReady = false;
    /** Set when we've sent refreshRequired() to listeners */
    private boolean mRefreshRequired = false;
    /** Whether our first query on this cursor should include a limit */
    private boolean mInitialConversationLimit = false;
    /** A list of mostly-dead items */
    private final List<Conversation> mMostlyDead = Lists.newArrayList();
    /** A list of items pending removal from a notification action. These may be undone later.
     *  Note: only modify on UI thread. */
    private final Set<Conversation> mNotificationTempDeleted = Sets.newHashSet();
    /** The name of the loader */
    private final String mName;
    /** Column names for this cursor */
    private String[] mColumnNames;
    // Column names as above, as a Set for quick membership checking
    private Set<String> mColumnNameSet;
    /** An observer on the underlying cursor (so we can detect changes from outside the UI) */
    private final CursorObserver mCursorObserver;
    /** Whether our observer is currently registered with the underlying cursor */
    private boolean mCursorObserverRegistered = false;
    /** Whether our loader is paused */
    private boolean mPaused = false;
    /** Whether or not sync from underlying provider should be deferred */
    private boolean mDeferSync = false;

    /** The current position of the cursor */
    private int mPosition = -1;

    /**
     * The number of cached deletions from this cursor (used to quickly generate an accurate count)
     */
    private int mDeletedCount = 0;

    /** Parameters passed to the underlying query */
    private Uri qUri;
    private String[] qProjection;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private void setCursor(UnderlyingCursorWrapper cursor) {
        // If we have an existing underlying cursor, make sure it's closed
        if (mUnderlyingCursor != null) {
            close();
        }
        mColumnNames = cursor.getColumnNames();
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String name : mColumnNames) {
            builder.add(name);
        }
        mColumnNameSet = builder.build();
        mRefreshRequired = false;
        mRefreshReady = false;
        mRefreshTask = null;
        resetCursor(cursor);

        resetNotificationActions();
        handleNotificationActions();
    }

    public ConversationCursor(Activity activity, Uri uri, boolean initialConversationLimit,
            String name) {
        mInitialConversationLimit = initialConversationLimit;
        mResolver = activity.getApplicationContext().getContentResolver();
        qUri = uri;
        mName = name;
        qProjection = UIProvider.CONVERSATION_PROJECTION;
        mCursorObserver = new CursorObserver(new Handler(Looper.getMainLooper()));
    }

    /**
     * Create a ConversationCursor; this should be called by the ListActivity using that cursor
     */
    public void load() {
        synchronized (mCacheMapLock) {
            try {
                // Create new ConversationCursor
                LogUtils.d(LOG_TAG, "Create: initial creation");
                setCursor(doQuery(mInitialConversationLimit));
            } finally {
                // If we used a limit, queue up a query without limit
                if (mInitialConversationLimit) {
                    mInitialConversationLimit = false;
                    // We want to notify about this change to allow the UI to requery.  We don't
                    // want to directly call refresh() here as this will start an AyncTask which
                    // is normally only run after the cursor is in the "refresh required"
                    // state
                    underlyingChanged();
                }
            }
        }
    }

    /**
     * Pause notifications to UI
     */
    public void pause() {
        mPaused = true;
        if (DEBUG) LogUtils.i(LOG_TAG, "[Paused: %s]", this);
    }

    /**
     * Resume notifications to UI; if any are pending, send them
     */
    public void resume() {
        mPaused = false;
        if (DEBUG) LogUtils.i(LOG_TAG, "[Resumed: %s]", this);
        checkNotifyUI();
    }

    private void checkNotifyUI() {
        if (DEBUG) LogUtils.i(LOG_TAG, "IN checkNotifyUI, this=%s", this);
        if (!mPaused && !mDeferSync) {
            if (mRefreshRequired && (mRefreshTask == null)) {
                notifyRefreshRequired();
            } else if (mRefreshReady) {
                notifyRefreshReady();
            }
        }
    }

    public Set<Long> getConversationIds() {
        return mUnderlyingCursor != null ? mUnderlyingCursor.conversationIds() : null;
    }

    private static class UnderlyingRowData {
        public final String innerUri;
        public Conversation conversation;

        public UnderlyingRowData(String innerUri, Conversation conversation) {
            this.innerUri = innerUri;
            this.conversation = conversation;
        }
    }

    /**
     * Simple wrapper for a cursor that provides methods for quickly determining
     * the existence of a row.
     */
    private static class UnderlyingCursorWrapper extends ThreadSafeCursorWrapper
            implements DrawIdler.IdleListener {

        /**
         * An AsyncTask that will fill as much of the cache as possible until either the cache is
         * full or the task is cancelled. If not cancelled and we're not done caching, it will
         * schedule another iteration to run upon completion.
         * <p>
         * Generally, only one task instance per {@link UnderlyingCursorWrapper} will run at a time.
         * But if an old task is cancelled, it may continue to execute at most one iteration (due
         * to the per-iteration cancellation-signal read), possibly concurrently with a new task.
         */
        private class CacheLoaderTask extends AsyncTask<Void, Void, Void> {
            private final int mStartPos;

            CacheLoaderTask(int startPosition) {
                mStartPos = startPosition;
            }

            @Override
            public Void doInBackground(Void... param) {
                try {
                    Utils.traceBeginSection("backgroundCaching");
                    if (DEBUG) LogUtils.i(LOG_TAG, "in cache job pos=%s c=%s", mStartPos,
                            getWrappedCursor());
                    final int count = getCount();
                    while (true) {
                        // It is possible for two instances of this loop to execute at once if
                        // an earlier task is cancelled but gets preempted. As written, this loop
                        // safely shares mCachePos without mutexes by only reading it once and
                        // writing it once (writing based on the previously-read value).
                        // The most that can happen is that one row's values is read twice.
                        final int pos = mCachePos;
                        if (isCancelled() || pos >= count) {
                            break;
                        }

                        final UnderlyingRowData rowData = mRowCache.get(pos);
                        if (rowData.conversation == null) {
                            // We are running in a background thread.  Set the position to the row
                            // we are interested in.
                            if (moveToPosition(pos)) {
                                rowData.conversation = new Conversation(
                                        UnderlyingCursorWrapper.this);
                            }
                        }
                        mCachePos = pos + 1;
                    }
                    System.gc();
                } finally {
                    Utils.traceEndSection();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mCacheLoaderTask = null;
                LogUtils.i(LOG_TAG, "ConversationCursor caching complete pos=%s", mCachePos);
            }

        }

        private class NewCursorUpdateObserver extends ContentObserver {
            public NewCursorUpdateObserver(Handler handler) {
                super(handler);
            }

            @Override
            public void onChange(boolean selfChange) {
                // Since this observer is used to keep track of changes that happen while
                // the Conversation objects are being pre-cached, and the conversation maps are
                // populated
                mCursorUpdated = true;
            }
        }

        // be polite by default; assume the device is initially busy and don't start pre-caching
        // until the idler connects and says we're idle
        private int mDrawState = DrawIdler.STATE_ACTIVE;
        /**
         * The one currently active cache task. We try to only run one at a time, but because we
         * don't interrupt the old task when cancelling, it may still run for a bit. See
         * {@link CacheLoaderTask#doInBackground(Void...)} for notes on thread safety.
         */
        private CacheLoaderTask mCacheLoaderTask;
        /**
         * The current row that the cache task is working on, or should work on next.
         * <p>
         * Not synchronized; see comments in {@link CacheLoaderTask#doInBackground(Void...)} for
         * notes on thread safety.
         */
        private int mCachePos;
        private boolean mCachingEnabled = true;
        private final NewCursorUpdateObserver mCursorUpdateObserver;
        private boolean mUpdateObserverRegistered = false;

        // Ideally these two objects could be combined into a Map from
        // conversationId -> position, but the cached values uses the conversation
        // uri as a key.
        private final Map<String, Integer> mConversationUriPositionMap;
        private final Map<Long, Integer> mConversationIdPositionMap;
        private final List<UnderlyingRowData> mRowCache;

        private boolean mCursorUpdated = false;

        public UnderlyingCursorWrapper(Cursor result) {
            super(result);

            // Register the content observer immediately, as we want to make sure that we don't miss
            // any updates
            mCursorUpdateObserver =
                    new NewCursorUpdateObserver(new Handler(Looper.getMainLooper()));
            if (result != null) {
                result.registerContentObserver(mCursorUpdateObserver);
                mUpdateObserverRegistered = true;
            }

            final long start = SystemClock.uptimeMillis();
            final Map<String, Integer> uriPositionMap;
            final Map<Long, Integer> idPositionMap;
            final UnderlyingRowData[] cache;
            final int count;
            Utils.traceBeginSection("blockingCaching");
            if (super.moveToFirst()) {
                count = super.getCount();
                cache = new UnderlyingRowData[count];
                int i = 0;

                uriPositionMap = Maps.newHashMapWithExpectedSize(count);
                idPositionMap = Maps.newHashMapWithExpectedSize(count);

                do {
                    final String innerUriString;
                    final long convId;

                    innerUriString = super.getString(URI_COLUMN_INDEX);
                    convId = super.getLong(UIProvider.CONVERSATION_ID_COLUMN);

                    if (DEBUG_DUPLICATE_KEYS) {
                        if (uriPositionMap.containsKey(innerUriString)) {
                            LogUtils.e(LOG_TAG, "Inserting duplicate conversation uri key: %s. " +
                                    "Cursor position: %d, iteration: %d map position: %d",
                                    innerUriString, getPosition(), i,
                                    uriPositionMap.get(innerUriString));
                        }
                        if (idPositionMap.containsKey(convId)) {
                            LogUtils.e(LOG_TAG, "Inserting duplicate conversation id key: %d" +
                                    "Cursor position: %d, iteration: %d map position: %d",
                                    convId, getPosition(), i, idPositionMap.get(convId));
                        }
                    }

                    uriPositionMap.put(innerUriString, i);
                    idPositionMap.put(convId, i);

                    cache[i] = new UnderlyingRowData(
                            innerUriString,
                            null /* conversation */);
                } while (super.moveToPosition(++i));

                if (uriPositionMap.size() != count || idPositionMap.size() != count) {
                    if (DEBUG_DUPLICATE_KEYS)  {
                        throw new IllegalStateException("Unexpected map sizes: cursorN=" + count
                                + " uriN=" + uriPositionMap.size() + " idN="
                                + idPositionMap.size());
                    } else {
                        LogUtils.e(LOG_TAG, "Unexpected map sizes.  Cursor size: %d, " +
                                "uri position map size: %d, id position map size: %d", count,
                                uriPositionMap.size(), idPositionMap.size());
                    }
                }
            } else {
                count = 0;
                cache = new UnderlyingRowData[0];
                uriPositionMap = Maps.newHashMap();
                idPositionMap = Maps.newHashMap();
            }
            mConversationUriPositionMap = Collections.unmodifiableMap(uriPositionMap);
            mConversationIdPositionMap = Collections.unmodifiableMap(idPositionMap);

            mRowCache = Collections.unmodifiableList(Arrays.asList(cache));
            final long end = SystemClock.uptimeMillis();
            LogUtils.i(LOG_TAG, "*** ConversationCursor pre-loading took %sms n=%s", (end-start),
                    count);

            Utils.traceEndSection();

            // Later, when the idler signals that the activity is idle, start a task to cache
            // conversations in pieces.
            mCachePos = 0;
        }

        /**
         * Resumes caching at {@link #mCachePos}.
         *
         * @return true if we actually resumed, false if we're done or stopped
         */
        private boolean resumeCaching() {
            if (mCacheLoaderTask != null) {
                throw new IllegalStateException("unexpected existing task: " + mCacheLoaderTask);
            }

            if (mCachingEnabled && mCachePos < getCount()) {
                mCacheLoaderTask = new CacheLoaderTask(mCachePos);
                mCacheLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return true;
            }
            return false;
        }

        private void pauseCaching() {
            if (mCacheLoaderTask != null) {
                LogUtils.i(LOG_TAG, "Cancelling caching startPos=%s pos=%s",
                        mCacheLoaderTask.mStartPos, mCachePos);
                mCacheLoaderTask.cancel(false /* interrupt */);
                mCacheLoaderTask = null;
            }
        }

        public void stopCaching() {
            pauseCaching();
            mCachingEnabled = false;
        }

        public boolean contains(String uri) {
            return mConversationUriPositionMap.containsKey(uri);
        }

        public Set<Long> conversationIds() {
            return mConversationIdPositionMap.keySet();
        }

        public int getPosition(long conversationId) {
            final Integer position = mConversationIdPositionMap.get(conversationId);
            return position != null ? position.intValue() : -1;
        }

        public int getPosition(String conversationUri) {
            final Integer position = mConversationUriPositionMap.get(conversationUri);
            return position != null ? position.intValue() : -1;
        }

        public String getInnerUri() {
            return mRowCache.get(getPosition()).innerUri;
        }

        public Conversation getConversation() {
            return mRowCache.get(getPosition()).conversation;
        }

        public void cacheConversation(Conversation conversation) {
            final UnderlyingRowData rowData = mRowCache.get(getPosition());
            if (rowData.conversation == null) {
                rowData.conversation = conversation;
            }
        }

        private void notifyConversationUIPositionChange() {
            Utils.notifyCursorUIPositionChange(this, getPosition());
        }

        /**
         * Returns a boolean indicating whether the cursor has been updated
         */
        public boolean isDataUpdated() {
            return mCursorUpdated;
        }

        public void disableUpdateNotifications() {
            if (mUpdateObserverRegistered) {
                getWrappedCursor().unregisterContentObserver(mCursorUpdateObserver);
                mUpdateObserverRegistered = false;
            }
        }

        @Override
        public void close() {
            stopCaching();
            disableUpdateNotifications();
            super.close();
        }

        @Override
        public void onStateChanged(DrawIdler idler, int newState) {
            final int oldState = mDrawState;
            mDrawState = newState;
            if (oldState != newState) {
                if (newState == DrawIdler.STATE_IDLE) {
                    // begin/resume caching
                    final boolean resumed = resumeCaching();
                    if (resumed) {
                        LogUtils.i(LOG_TAG, "Resuming caching, pos=%s idler=%s", mCachePos, idler);
                    }
                } else {
                    // pause caching
                    pauseCaching();
                }
            }
        }

    }

    /**
     * Runnable that performs the query on the underlying provider
     */
    private class RefreshTask extends AsyncTask<Void, Void, UnderlyingCursorWrapper> {
        private RefreshTask() {
        }

        @Override
        protected UnderlyingCursorWrapper doInBackground(Void... params) {
            if (DEBUG) {
                LogUtils.i(LOG_TAG, "[Start refresh of %s: %d]", mName, hashCode());
            }
            // Get new data
            final UnderlyingCursorWrapper result = doQuery(false);
            // Make sure window is full
            result.getCount();
            return result;
        }

        @Override
        protected void onPostExecute(UnderlyingCursorWrapper result) {
            synchronized(mCacheMapLock) {
                LogUtils.d(
                        LOG_TAG,
                        "Received notify ui callback and sending a notification is enabled? %s",
                        (!mPaused && !mDeferSync));
                // If cursor got closed (e.g. reset loader) in the meantime, cancel the refresh
                if (isClosed()) {
                    onCancelled(result);
                    return;
                }
                mRequeryCursor = result;
                mRefreshReady = true;
                if (DEBUG) {
                    LogUtils.i(LOG_TAG, "[Query done %s: %d]", mName, hashCode());
                }
                if (!mDeferSync && !mPaused) {
                    notifyRefreshReady();
                }
            }
        }

        @Override
        protected void onCancelled(UnderlyingCursorWrapper result) {
            if (DEBUG) {
                LogUtils.i(LOG_TAG, "[Ignoring refresh result: %d]", hashCode());
            }
            if (result != null) {
                result.close();
            }
        }
    }

    private UnderlyingCursorWrapper doQuery(boolean withLimit) {
        Uri uri = qUri;
        if (withLimit) {
            uri = uri.buildUpon().appendQueryParameter(ConversationListQueryParameters.LIMIT,
                    ConversationListQueryParameters.DEFAULT_LIMIT).build();
        }
        long time = System.currentTimeMillis();

        Utils.traceBeginSection("query");
        final Cursor result = mResolver.query(uri, qProjection, null, null, null);
        Utils.traceEndSection();
        if (result == null) {
            LogUtils.w(LOG_TAG, "doQuery returning null cursor, uri: " + uri);
        } else if (DEBUG) {
            time = System.currentTimeMillis() - time;
            LogUtils.i(LOG_TAG, "ConversationCursor query: %s, %dms, %d results",
                    uri, time, result.getCount());
        }
        System.gc();
        return new UnderlyingCursorWrapper(result);
    }

    static boolean offUiThread() {
        return Looper.getMainLooper().getThread() != Thread.currentThread();
    }

    /**
     * Reset the cursor; this involves clearing out our cache map and resetting our various counts
     * The cursor should be reset whenever we get fresh data from the underlying cursor. The cache
     * is locked during the reset, which will block the UI, but for only a very short time
     * (estimated at a few ms, but we can profile this; remember that the cache will usually
     * be empty or have a few entries)
     */
    private void resetCursor(UnderlyingCursorWrapper newCursorWrapper) {
        synchronized (mCacheMapLock) {
            // Walk through the cache
            final Iterator<Map.Entry<String, ContentValues>> iter =
                    mCacheMap.entrySet().iterator();
            final long now = System.currentTimeMillis();
            while (iter.hasNext()) {
                Map.Entry<String, ContentValues> entry = iter.next();
                final ContentValues values = entry.getValue();
                final String key = entry.getKey();
                boolean withinTimeWindow = false;
                boolean removed = false;
                if (values != null) {
                    Long updateTime = values.getAsLong(UPDATE_TIME_COLUMN);
                    if (updateTime != null && ((now - updateTime) < REQUERY_ALLOWANCE_TIME)) {
                        LogUtils.d(LOG_TAG, "IN resetCursor, keep recent changes to %s", key);
                        withinTimeWindow = true;
                    } else if (updateTime == null) {
                        LogUtils.e(LOG_TAG, "null updateTime from mCacheMap for key: %s", key);
                    }
                    if (values.containsKey(DELETED_COLUMN)) {
                        // Item is deleted locally AND deleted in the new cursor.
                        if (!newCursorWrapper.contains(key)) {
                            // Keep the deleted count up-to-date; remove the
                            // cache entry
                            mDeletedCount--;
                            removed = true;
                            LogUtils.d(LOG_TAG,
                                    "IN resetCursor, sDeletedCount decremented to: %d by %s",
                                    mDeletedCount, key);
                        }
                    }
                } else {
                    LogUtils.e(LOG_TAG, "null ContentValues from mCacheMap for key: %s", key);
                }
                // Remove the entry if it was time for an update or the item was deleted by the user.
                if (!withinTimeWindow || removed) {
                    iter.remove();
                }
            }

            // Swap cursor
            if (mUnderlyingCursor != null) {
                close();
            }
            mUnderlyingCursor = newCursorWrapper;

            mPosition = -1;
            mUnderlyingCursor.moveToPosition(mPosition);
            if (!mCursorObserverRegistered) {
                mUnderlyingCursor.registerContentObserver(mCursorObserver);
                mCursorObserverRegistered = true;

            }
            mRefreshRequired = false;

            // If the underlying cursor has received an update before we have gotten to this
            // point, we will want to make sure to refresh
            final boolean underlyingCursorUpdated = mUnderlyingCursor.isDataUpdated();
            mUnderlyingCursor.disableUpdateNotifications();
            if (underlyingCursorUpdated) {
                underlyingChanged();
            }
        }
        if (DEBUG) LogUtils.i(LOG_TAG, "OUT resetCursor, this=%s", this);
    }

    /**
     * Returns the conversation uris for the Conversations that the ConversationCursor is treating
     * as deleted.  This is an optimization to allow clients to determine if an item has been
     * removed, without having to iterate through the whole cursor
     */
    public Set<String> getDeletedItems() {
        synchronized (mCacheMapLock) {
            // Walk through the cache and return the list of uris that have been deleted
            final Set<String> deletedItems = Sets.newHashSet();
            final Iterator<Map.Entry<String, ContentValues>> iter =
                    mCacheMap.entrySet().iterator();
            final StringBuilder uriBuilder = new StringBuilder();
            while (iter.hasNext()) {
                final Map.Entry<String, ContentValues> entry = iter.next();
                final ContentValues values = entry.getValue();
                if (values.containsKey(DELETED_COLUMN)) {
                    // Since clients of the conversation cursor see conversation ConversationCursor
                    // provider uris, we need to make sure that this also returns these uris
                    deletedItems.add(uriToCachingUriString(entry.getKey(), uriBuilder));
                }
            }
            return deletedItems;
        }
    }

    /**
     * Returns the position of a conversation in the underlying cursor, without adjusting for the
     * cache. Notably, conversations which are marked as deleted in the cache but which haven't yet
     * been deleted in the underlying cursor will return non-negative here.
     * @param conversationId The id of the conversation we are looking for.
     * @return The position of the conversation in the underlying cursor, or -1 if not there.
     */
    public int getUnderlyingPosition(final long conversationId) {
        return mUnderlyingCursor.getPosition(conversationId);
    }

    /**
     * Returns the position, in the ConversationCursor, of the Conversation with the specified id.
     * The returned position will take into account any items that have been deleted.
     */
    public int getConversationPosition(long conversationId) {
        final int underlyingPosition = mUnderlyingCursor.getPosition(conversationId);
        if (underlyingPosition < 0) {
            // The conversation wasn't found in the underlying cursor, return the underlying result.
            return underlyingPosition;
        }

        // Walk through each of the deleted items.  If the deleted item is before the underlying
        // position, decrement the position
        synchronized (mCacheMapLock) {
            int updatedPosition = underlyingPosition;
            final Iterator<Map.Entry<String, ContentValues>> iter =
                    mCacheMap.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<String, ContentValues> entry = iter.next();
                final ContentValues values = entry.getValue();
                if (values.containsKey(DELETED_COLUMN)) {
                    // Since clients of the conversation cursor see conversation ConversationCursor
                    // provider uris, we need to make sure that this also returns these uris
                    final String conversationUri = entry.getKey();
                    final int deletedItemPosition = mUnderlyingCursor.getPosition(conversationUri);
                    if (deletedItemPosition == underlyingPosition) {
                        // The requested items has been deleted.
                        return -1;
                    }

                    if (deletedItemPosition >= 0 && deletedItemPosition < underlyingPosition) {
                        // This item has been deleted, but is still in the underlying cursor, at
                        // a position before the requested item.  Decrement the position of the
                        // requested item.
                        updatedPosition--;
                    }
                }
            }
            return updatedPosition;
        }
    }

    /**
     * Add a listener for this cursor; we'll notify it when our data changes
     */
    public void addListener(ConversationListener listener) {
        final int numPrevListeners;
        synchronized (mListeners) {
            numPrevListeners = mListeners.size();
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            } else {
                LogUtils.d(LOG_TAG, "Ignoring duplicate add of listener");
            }
        }

        if (numPrevListeners == 0 && mRefreshRequired) {
            // A refresh is required, but it came when there were no listeners.  Since this is the
            // first registered listener, we want to make sure that we don't drop this event.
            notifyRefreshRequired();
        }
    }

    /**
     * Remove a listener for this cursor
     */
    public void removeListener(ConversationListener listener) {
        synchronized(mListeners) {
            mListeners.remove(listener);
        }
    }

    @Override
    public void onStateChanged(DrawIdler idler, int newState) {
        if (mUnderlyingCursor != null) {
            mUnderlyingCursor.onStateChanged(idler, newState);
        }
    }

    /**
     * Generate a forwarding Uri to ConversationProvider from an original Uri.  We do this by
     * changing the authority to ours, but otherwise leaving the Uri intact.
     * NOTE: This won't handle query parameters, so the functionality will need to be added if
     * parameters are used in the future
     * @param uriStr the uri
     * @return a forwarding uri to ConversationProvider
     */
    private static String uriToCachingUriString(String uriStr, StringBuilder sb) {
        final String withoutScheme = uriStr.substring(
                uriStr.indexOf(ConversationProvider.URI_SEPARATOR)
                + ConversationProvider.URI_SEPARATOR.length());
        final String result;
        if (sb != null) {
            sb.setLength(0);
            sb.append(ConversationProvider.sUriPrefix);
            sb.append(withoutScheme);
            result = sb.toString();
        } else {
            result = ConversationProvider.sUriPrefix + withoutScheme;
        }
        return result;
    }

    /**
     * Regenerate the original Uri from a forwarding (ConversationProvider) Uri
     * NOTE: See note above for uriToCachingUri
     * @param uri the forwarding Uri
     * @return the original Uri
     */
    private static Uri uriFromCachingUri(Uri uri) {
        String authority = uri.getAuthority();
        // Don't modify uri's that aren't ours
        if (!authority.equals(ConversationProvider.AUTHORITY)) {
            return uri;
        }
        List<String> path = uri.getPathSegments();
        Uri.Builder builder = new Uri.Builder().scheme(uri.getScheme()).authority(path.get(0));
        for (int i = 1; i < path.size(); i++) {
            builder.appendPath(path.get(i));
        }
        return builder.build();
    }

    private static String uriStringFromCachingUri(Uri uri) {
        Uri underlyingUri = uriFromCachingUri(uri);
        // Remember to decode the underlying Uri as it might be encoded (as w/ Gmail)
        return Uri.decode(underlyingUri.toString());
    }

    public void setConversationColumn(Uri conversationUri, String columnName, Object value) {
        final String uriStr = uriStringFromCachingUri(conversationUri);
        synchronized (mCacheMapLock) {
            cacheValue(uriStr, columnName, value);
        }
        notifyDataChanged();
    }

    /**
     * Cache a column name/value pair for a given Uri
     * @param uriString the Uri for which the column name/value pair applies
     * @param columnName the column name
     * @param value the value to be cached
     */
    private void cacheValue(String uriString, String columnName, Object value) {
        // Calling this method off the UI thread will mess with ListView's reading of the cursor's
        // count
        if (offUiThread()) {
            LogUtils.e(LOG_TAG, new Error(),
                    "cacheValue incorrectly being called from non-UI thread");
        }

        synchronized (mCacheMapLock) {
            // Get the map for our uri
            ContentValues map = mCacheMap.get(uriString);
            // Create one if necessary
            if (map == null) {
                map = new ContentValues();
                mCacheMap.put(uriString, map);
            }
            // If we're caching a deletion, add to our count
            if (columnName.equals(DELETED_COLUMN)) {
                final boolean state = (Boolean)value;
                final boolean hasValue = map.get(columnName) != null;
                if (state && !hasValue) {
                    mDeletedCount++;
                    if (DEBUG) {
                        LogUtils.i(LOG_TAG, "Deleted %s, incremented deleted count=%d", uriString,
                                mDeletedCount);
                    }
                } else if (!state && hasValue) {
                    mDeletedCount--;
                    map.remove(columnName);
                    if (DEBUG) {
                        LogUtils.i(LOG_TAG, "Undeleted %s, decremented deleted count=%d", uriString,
                                mDeletedCount);
                    }
                    return;
                } else if (!state) {
                    // Trying to undelete, but it's not deleted; just return
                    if (DEBUG) {
                        LogUtils.i(LOG_TAG, "Undeleted %s, IGNORING, deleted count=%d", uriString,
                                mDeletedCount);
                    }
                    return;
                }
            }
            putInValues(map, columnName, value);
            map.put(UPDATE_TIME_COLUMN, System.currentTimeMillis());
            if (DEBUG && (!columnName.equals(DELETED_COLUMN))) {
                LogUtils.i(LOG_TAG, "Caching value for %s: %s", uriString, columnName);
            }
        }
    }

    /**
     * Get the cached value for the provided column; we special case -1 as the "deleted" column
     * @param columnIndex the index of the column whose cached value we want to retrieve
     * @return the cached value for this column, or null if there is none
     */
    private Object getCachedValue(int columnIndex) {
        final String uri = mUnderlyingCursor.getInnerUri();
        return getCachedValue(uri, columnIndex);
    }

    private Object getCachedValue(String uri, int columnIndex) {
        ContentValues uriMap = mCacheMap.get(uri);
        if (uriMap != null) {
            String columnName;
            if (columnIndex == DELETED_COLUMN_INDEX) {
                columnName = DELETED_COLUMN;
            } else {
                columnName = mColumnNames[columnIndex];
            }
            return uriMap.get(columnName);
        }
        return null;
    }

    /**
     * When the underlying cursor changes, we want to alert the listener
     */
    private void underlyingChanged() {
        synchronized(mCacheMapLock) {
            if (mCursorObserverRegistered) {
                try {
                    mUnderlyingCursor.unregisterContentObserver(mCursorObserver);
                } catch (IllegalStateException e) {
                    // Maybe the cursor was GC'd?
                }
                mCursorObserverRegistered = false;
            }
            mRefreshRequired = true;
            if (DEBUG) LogUtils.i(LOG_TAG, "IN underlyingChanged, this=%s", this);
            if (!mPaused) {
                notifyRefreshRequired();
            }
            if (DEBUG) LogUtils.i(LOG_TAG, "OUT underlyingChanged, this=%s", this);
        }
    }

    /**
     * Must be called on UI thread; notify listeners that a refresh is required
     */
    private void notifyRefreshRequired() {
        if (DEBUG) LogUtils.i(LOG_TAG, "[Notify: onRefreshRequired() this=%s]", this);
        if (!mDeferSync) {
            synchronized(mListeners) {
                for (ConversationListener listener: mListeners) {
                    listener.onRefreshRequired();
                }
            }
        }
    }

    /**
     * Must be called on UI thread; notify listeners that a new cursor is ready
     */
    private void notifyRefreshReady() {
        if (DEBUG) {
            LogUtils.i(LOG_TAG, "[Notify %s: onRefreshReady(), %d listeners]",
                    mName, mListeners.size());
        }
        synchronized(mListeners) {
            for (ConversationListener listener: mListeners) {
                listener.onRefreshReady();
            }
        }
    }

    /**
     * Must be called on UI thread; notify listeners that data has changed
     */
    private void notifyDataChanged() {
        if (DEBUG) {
            LogUtils.i(LOG_TAG, "[Notify %s: onDataSetChanged()]", mName);
        }
        synchronized(mListeners) {
            for (ConversationListener listener: mListeners) {
                listener.onDataSetChanged();
            }
        }

        handleNotificationActions();
    }

    /**
     * Put the refreshed cursor in place (called by the UI)
     */
    public void sync() {
        if (mRequeryCursor == null) {
            // This can happen during an animated deletion, if the UI isn't keeping track, or
            // if a new query intervened (i.e. user changed folders)
            if (DEBUG) {
                LogUtils.i(LOG_TAG, "[sync() %s; no requery cursor]", mName);
            }
            return;
        }
        synchronized(mCacheMapLock) {
            if (DEBUG) {
                LogUtils.i(LOG_TAG, "[sync() %s]", mName);
            }
            mRefreshTask = null;
            mRefreshReady = false;
            resetCursor(mRequeryCursor);
            mRequeryCursor = null;
        }
        notifyDataChanged();
    }

    public boolean isRefreshRequired() {
        return mRefreshRequired;
    }

    public boolean isRefreshReady() {
        return mRefreshReady;
    }

    /**
     * When we get a requery from the UI, we'll do it, but also clear the cache. The listener is
     * notified when the requery is complete
     * NOTE: This will have to change, of course, when we start using loaders...
     */
    public boolean refresh() {
        if (DEBUG) LogUtils.i(LOG_TAG, "[refresh() this=%s]", this);
        synchronized(mCacheMapLock) {
            if (mRefreshTask != null) {
                if (DEBUG) {
                    LogUtils.i(LOG_TAG, "[refresh() %s returning; already running %d]",
                            mName, mRefreshTask.hashCode());
                }
                return false;
            }
            if (mUnderlyingCursor != null) {
                mUnderlyingCursor.stopCaching();
            }
            mRefreshTask = new RefreshTask();
            mRefreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return true;
    }

    public void disable() {
        close();
        mCacheMap.clear();
        mListeners.clear();
        mUnderlyingCursor = null;
    }

    @Override
    public void close() {
        if (mUnderlyingCursor != null && !mUnderlyingCursor.isClosed()) {
            // Unregister our observer on the underlying cursor and close as usual
            if (mCursorObserverRegistered) {
                try {
                    mUnderlyingCursor.unregisterContentObserver(mCursorObserver);
                } catch (IllegalStateException e) {
                    // Maybe the cursor got GC'd?
                }
                mCursorObserverRegistered = false;
            }
            mUnderlyingCursor.close();
        }
    }

    /**
     * Move to the next not-deleted item in the conversation
     */
    @Override
    public boolean moveToNext() {
        while (true) {
            boolean ret = mUnderlyingCursor.moveToNext();
            if (!ret) {
                mPosition = getCount();
                if (DEBUG) {
                    LogUtils.i(LOG_TAG, "*** moveToNext returns false: pos = %d, und = %d" +
                            ", del = %d", mPosition, mUnderlyingCursor.getPosition(),
                            mDeletedCount);
                }
                return false;
            }
            if (getCachedValue(DELETED_COLUMN_INDEX) instanceof Integer) continue;
            mPosition++;
            return true;
        }
    }

    /**
     * Move to the previous not-deleted item in the conversation
     */
    @Override
    public boolean moveToPrevious() {
        while (true) {
            boolean ret = mUnderlyingCursor.moveToPrevious();
            if (!ret) {
                // Make sure we're before the first position
                mPosition = -1;
                return false;
            }
            if (getCachedValue(DELETED_COLUMN_INDEX) instanceof Integer) continue;
            mPosition--;
            return true;
        }
    }

    @Override
    public int getPosition() {
        return mPosition;
    }

    /**
     * The actual cursor's count must be decremented by the number we've deleted from the UI
     */
    @Override
    public int getCount() {
        if (mUnderlyingCursor == null) {
            throw new IllegalStateException(
                    "getCount() on disabled cursor: " + mName + "(" + qUri + ")");
        }
        return mUnderlyingCursor.getCount() - mDeletedCount;
    }

    @Override
    public boolean moveToFirst() {
        if (mUnderlyingCursor == null) {
            throw new IllegalStateException(
                    "moveToFirst() on disabled cursor: " + mName + "(" + qUri + ")");
        }
        mUnderlyingCursor.moveToPosition(-1);
        mPosition = -1;
        return moveToNext();
    }

    @Override
    public boolean moveToPosition(int pos) {
        if (mUnderlyingCursor == null) {
            throw new IllegalStateException(
                    "moveToPosition() on disabled cursor: " + mName + "(" + qUri + ")");
        }
        // Handle the "move to first" case before anything else; moveToPosition(0) in an empty
        // SQLiteCursor moves the position to 0 when returning false, which we will mirror.
        // But we don't want to return true on a subsequent "move to first", which we would if we
        // check pos vs mPosition first
        if (mUnderlyingCursor.getPosition() == -1) {
            LogUtils.d(LOG_TAG, "*** Underlying cursor position is -1 asking to move from %d to %d",
                    mPosition, pos);
        }
        if (pos == 0) {
            return moveToFirst();
        } else if (pos < 0) {
            mPosition = -1;
            mUnderlyingCursor.moveToPosition(mPosition);
            return false;
        } else if (pos == mPosition) {
            // Return false if we're past the end of the cursor
            return pos < getCount();
        } else if (pos > mPosition) {
            while (pos > mPosition) {
                if (!moveToNext()) {
                    return false;
                }
            }
            return true;
        } else if ((pos >= 0) && (mPosition - pos) > pos) {
            // Optimization if it's easier to move forward to position instead of backward
            if (DEBUG) {
                LogUtils.i(LOG_TAG, "*** Move from %d to %d, starting from first", mPosition, pos);
            }
            moveToFirst();
            return moveToPosition(pos);
        } else {
            while (pos < mPosition) {
                if (!moveToPrevious()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Make sure mPosition is correct after locally deleting/undeleting items
     */
    private void recalibratePosition() {
        final int pos = mPosition;
        moveToFirst();
        moveToPosition(pos);
    }

    @Override
    public boolean moveToLast() {
        throw new UnsupportedOperationException("moveToLast unsupported!");
    }

    @Override
    public boolean move(int offset) {
        throw new UnsupportedOperationException("move unsupported!");
    }

    /**
     * We need to override all of the getters to make sure they look at cached values before using
     * the values in the underlying cursor
     */
    @Override
    public double getDouble(int columnIndex) {
        Object obj = getCachedValue(columnIndex);
        if (obj != null) return (Double)obj;
        return mUnderlyingCursor.getDouble(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        Object obj = getCachedValue(columnIndex);
        if (obj != null) return (Float)obj;
        return mUnderlyingCursor.getFloat(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        Object obj = getCachedValue(columnIndex);
        if (obj != null) return (Integer)obj;
        return mUnderlyingCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        Object obj = getCachedValue(columnIndex);
        if (obj != null) return (Long)obj;
        return mUnderlyingCursor.getLong(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) {
        Object obj = getCachedValue(columnIndex);
        if (obj != null) return (Short)obj;
        return mUnderlyingCursor.getShort(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        // If we're asking for the Uri for the conversation list, we return a forwarding URI
        // so that we can intercept update/delete and handle it ourselves
        if (columnIndex == URI_COLUMN_INDEX) {
            return uriToCachingUriString(mUnderlyingCursor.getInnerUri(), null);
        }
        Object obj = getCachedValue(columnIndex);
        if (obj != null) return (String)obj;
        return mUnderlyingCursor.getString(columnIndex);
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        Object obj = getCachedValue(columnIndex);
        if (obj != null) return (byte[])obj;
        return mUnderlyingCursor.getBlob(columnIndex);
    }

    public byte[] getCachedBlob(int columnIndex) {
        return (byte[]) getCachedValue(columnIndex);
    }

    public Conversation getConversation() {
        Conversation c = getCachedConversation();
        if (c == null) {
            // not pre-cached. fall back to just-in-time construction.
            c = new Conversation(this);
            mUnderlyingCursor.cacheConversation(c);
        }

        return c;
    }

    /**
     * Returns a Conversation object for the current position, or null if it has not yet been
     * cached.
     *
     * This method will apply any cached column data to the result.
     *
     */
    public Conversation getCachedConversation() {
        Conversation result = mUnderlyingCursor.getConversation();
        if (result == null) {
            return null;
        }

        // apply any cached values
        // but skip over any cached values that aren't part of the cursor projection
        final ContentValues values = mCacheMap.get(mUnderlyingCursor.getInnerUri());
        if (values != null) {
            final ContentValues queryableValues = new ContentValues();
            for (String key : values.keySet()) {
                if (!mColumnNameSet.contains(key)) {
                    continue;
                }
                putInValues(queryableValues, key, values.get(key));
            }
            if (queryableValues.size() > 0) {
                // copy-on-write to help ensure the underlying cached Conversation is immutable
                // of course, any callers this method should also try not to modify them
                // overmuch...
                result = new Conversation(result);
                result.applyCachedValues(queryableValues);
            }
        }
        return result;
    }

    /**
     * Notifies the provider of the position of the conversation being accessed by the UI
     */
    public void notifyUIPositionChange() {
        mUnderlyingCursor.notifyConversationUIPositionChange();
    }

    private static void putInValues(ContentValues dest, String key, Object value) {
        // ContentValues has no generic "put", so we must test.  For now, the only classes
        // of values implemented are Boolean/Integer/String/Blob, though others are trivially
        // added
        if (value instanceof Boolean) {
            dest.put(key, ((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value instanceof Integer) {
            dest.put(key, (Integer) value);
        } else if (value instanceof String) {
            dest.put(key, (String) value);
        } else if (value instanceof byte[]) {
            dest.put(key, (byte[])value);
        } else {
            final String cname = value.getClass().getName();
            throw new IllegalArgumentException("Value class not compatible with cache: "
                    + cname);
        }
    }

    /**
     * Observer of changes to underlying data
     */
    private class CursorObserver extends ContentObserver {
        public CursorObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            // If we're here, then something outside of the UI has changed the data, and we
            // must query the underlying provider for that data;
            ConversationCursor.this.underlyingChanged();
        }
    }

    /**
     * ConversationProvider is the ContentProvider for our forwarding Uri's; it passes queries
     * and inserts directly, and caches updates/deletes before passing them through.  The caching
     * will cause a redraw of the list with updated values.
     */
    public abstract static class ConversationProvider extends ContentProvider {
        public static String AUTHORITY;
        public static String sUriPrefix;
        public static final String URI_SEPARATOR = "://";
        private ContentResolver mResolver;

        /**
         * Allows the implementing provider to specify the authority that should be used.
         */
        protected abstract String getAuthority();

        @Override
        public boolean onCreate() {
            sProvider = this;
            AUTHORITY = getAuthority();
            sUriPrefix = "content://" + AUTHORITY + "/";
            mResolver = getContext().getContentResolver();
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                String sortOrder) {
            return mResolver.query(
                    uriFromCachingUri(uri), projection, selection, selectionArgs, sortOrder);
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            insertLocal(uri, values);
            return ProviderExecute.opInsert(mResolver, uri, values);
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            throw new IllegalStateException("Unexpected call to ConversationProvider.update");
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            throw new IllegalStateException("Unexpected call to ConversationProvider.delete");
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        /**
         * Quick and dirty class that executes underlying provider CRUD operations on a background
         * thread.
         */
        static class ProviderExecute implements Runnable {
            static final int DELETE = 0;
            static final int INSERT = 1;
            static final int UPDATE = 2;

            final int mCode;
            final Uri mUri;
            final ContentValues mValues; //HEHEH
            final ContentResolver mResolver;

            ProviderExecute(int code, ContentResolver resolver, Uri uri, ContentValues values) {
                mCode = code;
                mUri = uriFromCachingUri(uri);
                mValues = values;
                mResolver = resolver;
            }

            static Uri opInsert(ContentResolver resolver, Uri uri, ContentValues values) {
                ProviderExecute e = new ProviderExecute(INSERT, resolver, uri, values);
                if (offUiThread()) return (Uri)e.go();
                new Thread(e).start();
                return null;
            }

            @Override
            public void run() {
                go();
            }

            public Object go() {
                switch(mCode) {
                    case DELETE:
                        return mResolver.delete(mUri, null, null);
                    case INSERT:
                        return mResolver.insert(mUri, mValues);
                    case UPDATE:
                        return mResolver.update(mUri,  mValues, null, null);
                    default:
                        return null;
                }
            }
        }

        private void insertLocal(Uri uri, ContentValues values) {
            // Placeholder for now; there's no local insert
        }

        private int mUndoSequence = 0;
        private ArrayList<Uri> mUndoDeleteUris = new ArrayList<Uri>();

        void addToUndoSequence(Uri uri) {
            if (sSequence != mUndoSequence) {
                mUndoSequence = sSequence;
                mUndoDeleteUris.clear();
            }
            mUndoDeleteUris.add(uri);
        }

        @VisibleForTesting
        void deleteLocal(Uri uri, ConversationCursor conversationCursor) {
            String uriString = uriStringFromCachingUri(uri);
            conversationCursor.cacheValue(uriString, DELETED_COLUMN, true);
            addToUndoSequence(uri);
        }

        @VisibleForTesting
        void undeleteLocal(Uri uri, ConversationCursor conversationCursor) {
            String uriString = uriStringFromCachingUri(uri);
            conversationCursor.cacheValue(uriString, DELETED_COLUMN, false);
        }

        void setMostlyDead(Conversation conv, ConversationCursor conversationCursor) {
            Uri uri = conv.uri;
            String uriString = uriStringFromCachingUri(uri);
            conversationCursor.setMostlyDead(uriString, conv);
            addToUndoSequence(uri);
        }

        void commitMostlyDead(Conversation conv, ConversationCursor conversationCursor) {
            conversationCursor.commitMostlyDead(conv);
        }

        boolean clearMostlyDead(Uri uri, ConversationCursor conversationCursor) {
            String uriString = uriStringFromCachingUri(uri);
            return conversationCursor.clearMostlyDead(uriString);
        }

        public void undo(ConversationCursor conversationCursor) {
            if (mUndoSequence == 0) {
                return;
            }

            for (Uri uri: mUndoDeleteUris) {
                if (!clearMostlyDead(uri, conversationCursor)) {
                    undeleteLocal(uri, conversationCursor);
                }
            }
            mUndoSequence = 0;
            conversationCursor.recalibratePosition();
            // Notify listeners that there was a change to the underlying
            // cursor to add back in some items.
            conversationCursor.notifyDataChanged();
        }

        @VisibleForTesting
        void updateLocal(Uri uri, ContentValues values, ConversationCursor conversationCursor) {
            if (values == null) {
                return;
            }
            String uriString = uriStringFromCachingUri(uri);
            for (String columnName: values.keySet()) {
                conversationCursor.cacheValue(uriString, columnName, values.get(columnName));
            }
        }

        public int apply(Collection<ConversationOperation> ops,
                ConversationCursor conversationCursor) {
            final HashMap<String, ArrayList<ContentProviderOperation>> batchMap =
                    new HashMap<String, ArrayList<ContentProviderOperation>>();
            // Increment sequence count
            sSequence++;

            // Execute locally and build CPO's for underlying provider
            boolean recalibrateRequired = false;
            for (ConversationOperation op: ops) {
                Uri underlyingUri = uriFromCachingUri(op.mUri);
                String authority = underlyingUri.getAuthority();
                ArrayList<ContentProviderOperation> authOps = batchMap.get(authority);
                if (authOps == null) {
                    authOps = new ArrayList<ContentProviderOperation>();
                    batchMap.put(authority, authOps);
                }
                ContentProviderOperation cpo = op.execute(underlyingUri);
                if (cpo != null) {
                    authOps.add(cpo);
                }
                // Keep track of whether our operations require recalibrating the cursor position
                if (op.mRecalibrateRequired) {
                    recalibrateRequired = true;
                }
            }

            // Recalibrate cursor position if required
            if (recalibrateRequired) {
                conversationCursor.recalibratePosition();
            }

            // Notify listeners that data has changed
            conversationCursor.notifyDataChanged();

            // Send changes to underlying provider
            final boolean notUiThread = offUiThread();
            for (final String authority: batchMap.keySet()) {
                final ArrayList<ContentProviderOperation> opList = batchMap.get(authority);
                if (notUiThread) {
                    try {
                        mResolver.applyBatch(authority, opList);
                    } catch (RemoteException e) {
                    } catch (OperationApplicationException e) {
                    }
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mResolver.applyBatch(authority, opList);
                            } catch (RemoteException e) {
                            } catch (OperationApplicationException e) {
                            }
                        }
                    }).start();
                }
            }
            return sSequence;
        }
    }

    void setMostlyDead(String uriString, Conversation conv) {
        LogUtils.d(LOG_TAG, "[Mostly dead, deferring: %s] ", uriString);
        cacheValue(uriString,
                UIProvider.ConversationColumns.FLAGS, Conversation.FLAG_MOSTLY_DEAD);
        conv.convFlags |= Conversation.FLAG_MOSTLY_DEAD;
        mMostlyDead.add(conv);
        mDeferSync = true;
    }

    void commitMostlyDead(Conversation conv) {
        conv.convFlags &= ~Conversation.FLAG_MOSTLY_DEAD;
        mMostlyDead.remove(conv);
        LogUtils.d(LOG_TAG, "[All dead: %s]", conv.uri);
        if (mMostlyDead.isEmpty()) {
            mDeferSync = false;
            checkNotifyUI();
        }
    }

    boolean clearMostlyDead(String uriString) {
        LogUtils.d(LOG_TAG, "[Clearing mostly dead %s] ", uriString);
        mMostlyDead.clear();
        mDeferSync = false;
        Object val = getCachedValue(uriString,
                UIProvider.CONVERSATION_FLAGS_COLUMN);
        if (val != null) {
            int flags = ((Integer)val).intValue();
            if ((flags & Conversation.FLAG_MOSTLY_DEAD) != 0) {
                cacheValue(uriString, UIProvider.ConversationColumns.FLAGS,
                        flags &= ~Conversation.FLAG_MOSTLY_DEAD);
                return true;
            }
        }
        return false;
    }




    /**
     * ConversationOperation is the encapsulation of a ContentProvider operation to be performed
     * atomically as part of a "batch" operation.
     */
    public class ConversationOperation {
        private static final int MOSTLY = 0x80;
        public static final int DELETE = 0;
        public static final int INSERT = 1;
        public static final int UPDATE = 2;
        public static final int ARCHIVE = 3;
        public static final int MUTE = 4;
        public static final int REPORT_SPAM = 5;
        public static final int REPORT_NOT_SPAM = 6;
        public static final int REPORT_PHISHING = 7;
        public static final int DISCARD_DRAFTS = 8;
        public static final int MOSTLY_ARCHIVE = MOSTLY | ARCHIVE;
        public static final int MOSTLY_DELETE = MOSTLY | DELETE;
        public static final int MOSTLY_DESTRUCTIVE_UPDATE = MOSTLY | UPDATE;

        private final int mType;
        private final Uri mUri;
        private final Conversation mConversation;
        private final ContentValues mValues;
        // True if an updated item should be removed locally (from ConversationCursor)
        // This would be the case for a folder change in which the conversation is no longer
        // in the folder represented by the ConversationCursor
        private final boolean mLocalDeleteOnUpdate;
        // After execution, this indicates whether or not the operation requires recalibration of
        // the current cursor position (i.e. it removed or added items locally)
        private boolean mRecalibrateRequired = true;
        // Whether this item is already mostly dead
        private final boolean mMostlyDead;

        public ConversationOperation(int type, Conversation conv) {
            this(type, conv, null);
        }

        public ConversationOperation(int type, Conversation conv, ContentValues values) {
            mType = type;
            mUri = conv.uri;
            mConversation = conv;
            mValues = values;
            mLocalDeleteOnUpdate = conv.localDeleteOnUpdate;
            mMostlyDead = conv.isMostlyDead();
        }

        private ContentProviderOperation execute(Uri underlyingUri) {
            Uri uri = underlyingUri.buildUpon()
                    .appendQueryParameter(UIProvider.SEQUENCE_QUERY_PARAMETER,
                            Integer.toString(sSequence))
                    .build();
            ContentProviderOperation op = null;
            switch(mType) {
                case UPDATE:
                    if (mLocalDeleteOnUpdate) {
                        sProvider.deleteLocal(mUri, ConversationCursor.this);
                    } else {
                        sProvider.updateLocal(mUri, mValues, ConversationCursor.this);
                        mRecalibrateRequired = false;
                    }
                    if (!mMostlyDead) {
                        op = ContentProviderOperation.newUpdate(uri)
                                .withValues(mValues)
                                .build();
                    } else {
                        sProvider.commitMostlyDead(mConversation, ConversationCursor.this);
                    }
                    break;
                case MOSTLY_DESTRUCTIVE_UPDATE:
                    sProvider.setMostlyDead(mConversation, ConversationCursor.this);
                    op = ContentProviderOperation.newUpdate(uri).withValues(mValues).build();
                    break;
                case INSERT:
                    sProvider.insertLocal(mUri, mValues);
                    op = ContentProviderOperation.newInsert(uri)
                            .withValues(mValues).build();
                    break;
                // Destructive actions below!
                // "Mostly" operations are reflected globally, but not locally, except to set
                // FLAG_MOSTLY_DEAD in the conversation itself
                case DELETE:
                    sProvider.deleteLocal(mUri, ConversationCursor.this);
                    if (!mMostlyDead) {
                        op = ContentProviderOperation.newDelete(uri).build();
                    } else {
                        sProvider.commitMostlyDead(mConversation, ConversationCursor.this);
                    }
                    break;
                case MOSTLY_DELETE:
                    sProvider.setMostlyDead(mConversation,ConversationCursor.this);
                    op = ContentProviderOperation.newDelete(uri).build();
                    break;
                case ARCHIVE:
                    sProvider.deleteLocal(mUri, ConversationCursor.this);
                    if (!mMostlyDead) {
                        // Create an update operation that represents archive
                        op = ContentProviderOperation.newUpdate(uri).withValue(
                                ConversationOperations.OPERATION_KEY,
                                ConversationOperations.ARCHIVE)
                                .build();
                    } else {
                        sProvider.commitMostlyDead(mConversation, ConversationCursor.this);
                    }
                    break;
                case MOSTLY_ARCHIVE:
                    sProvider.setMostlyDead(mConversation, ConversationCursor.this);
                    // Create an update operation that represents archive
                    op = ContentProviderOperation.newUpdate(uri).withValue(
                            ConversationOperations.OPERATION_KEY, ConversationOperations.ARCHIVE)
                            .build();
                    break;
                case MUTE:
                    if (mLocalDeleteOnUpdate) {
                        sProvider.deleteLocal(mUri, ConversationCursor.this);
                    }

                    // Create an update operation that represents mute
                    op = ContentProviderOperation.newUpdate(uri).withValue(
                            ConversationOperations.OPERATION_KEY, ConversationOperations.MUTE)
                            .build();
                    break;
                case REPORT_SPAM:
                case REPORT_NOT_SPAM:
                    sProvider.deleteLocal(mUri, ConversationCursor.this);

                    final String operation = mType == REPORT_SPAM ?
                            ConversationOperations.REPORT_SPAM :
                            ConversationOperations.REPORT_NOT_SPAM;

                    // Create an update operation that represents report spam
                    op = ContentProviderOperation.newUpdate(uri).withValue(
                            ConversationOperations.OPERATION_KEY, operation).build();
                    break;
                case REPORT_PHISHING:
                    sProvider.deleteLocal(mUri, ConversationCursor.this);

                    // Create an update operation that represents report phishing
                    op = ContentProviderOperation.newUpdate(uri).withValue(
                            ConversationOperations.OPERATION_KEY,
                            ConversationOperations.REPORT_PHISHING).build();
                    break;
                case DISCARD_DRAFTS:
                    sProvider.deleteLocal(mUri, ConversationCursor.this);

                    // Create an update operation that represents discarding drafts
                    op = ContentProviderOperation.newUpdate(uri).withValue(
                            ConversationOperations.OPERATION_KEY,
                            ConversationOperations.DISCARD_DRAFTS).build();
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "No such ConversationOperation type: " + mType);
            }

            return op;
        }
    }

    /**
     * For now, a single listener can be associated with the cursor, and for now we'll just
     * notify on deletions
     */
    public interface ConversationListener {
        /**
         * Data in the underlying provider has changed; a refresh is required to sync up
         */
        public void onRefreshRequired();
        /**
         * We've completed a requested refresh of the underlying cursor
         */
        public void onRefreshReady();
        /**
         * The data underlying the cursor has changed; the UI should redraw the list
         */
        public void onDataSetChanged();
    }

    @Override
    public boolean isFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBeforeFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAfterLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getColumnIndex(String columnName) {
        return mUnderlyingCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return mUnderlyingCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return mUnderlyingCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return mUnderlyingCursor.getColumnNames();
    }

    @Override
    public int getColumnCount() {
        return mUnderlyingCursor.getColumnCount();
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getType(int columnIndex) {
        return mUnderlyingCursor.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deactivate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return mUnderlyingCursor == null || mUnderlyingCursor.isClosed();
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        // Nope. We never notify of underlying changes on this channel, since the cursor watches
        // internally and offers onRefreshRequired/onRefreshReady to accomplish the same thing.
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        // See above.
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        // Nope. We use ConversationListener to accomplish this.
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        // See above.
    }

    @Override
    public Uri getNotificationUri() {
        if (mUnderlyingCursor == null) {
            return null;
        } else {
            return mUnderlyingCursor.getNotificationUri();
        }
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getExtras() {
        return mUnderlyingCursor != null ? mUnderlyingCursor.getExtras() : Bundle.EMPTY;
    }

    @Override
    public Bundle respond(Bundle extras) {
        if (mUnderlyingCursor != null) {
            return mUnderlyingCursor.respond(extras);
        }
        return Bundle.EMPTY;
    }

    @Override
    public boolean requery() {
        return true;
    }

    // Below are methods that update Conversation data (update/delete)

    public int updateBoolean(Conversation conversation, String columnName, boolean value) {
        return updateBoolean(Arrays.asList(conversation), columnName, value);
    }

    /**
     * Update an integer column for a group of conversations (see updateValues below)
     */
    public int updateInt(Collection<Conversation> conversations, String columnName,
            int value) {
        if (LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG)) {
            LogUtils.d(LOG_TAG, "ConversationCursor.updateInt(conversations=%s, columnName=%s)",
                    conversations.toArray(), columnName);
        }
        ContentValues cv = new ContentValues();
        cv.put(columnName, value);
        return updateValues(conversations, cv);
    }

    /**
     * Update a string column for a group of conversations (see updateValues below)
     */
    public int updateBoolean(Collection<Conversation> conversations, String columnName,
            boolean value) {
        ContentValues cv = new ContentValues();
        cv.put(columnName, value);
        return updateValues(conversations, cv);
    }

    /**
     * Update a string column for a group of conversations (see updateValues below)
     */
    public int updateString(Collection<Conversation> conversations, String columnName,
            String value) {
        return updateStrings(conversations, new String[] {
                columnName
        }, new String[] {
                value
        });
    }

    /**
     * Update a string columns for a group of conversations (see updateValues below)
     */
    public int updateStrings(Collection<Conversation> conversations,
            String[] columnNames, String[] values) {
        ContentValues cv = new ContentValues();
        for (int i = 0; i < columnNames.length; i++) {
            cv.put(columnNames[i], values[i]);
        }
        return updateValues(conversations, cv);
    }

    /**
     * Update a boolean column for a group of conversations, immediately in the UI and in a single
     * transaction in the underlying provider
     * @param conversations a collection of conversations
     * @param values the data to update
     * @return the sequence number of the operation (for undo)
     */
    public int updateValues(Collection<Conversation> conversations, ContentValues values) {
        return apply(
                getOperationsForConversations(conversations, ConversationOperation.UPDATE, values));
    }

    /**
     * Apply many operations in a single batch transaction.
     * @param op the collection of operations obtained through successive calls to
     * {@link #getOperationForConversation(Conversation, int, ContentValues)}.
     * @return the sequence number of the operation (for undo)
     */
    public int updateBulkValues(Collection<ConversationOperation> op) {
        return apply(op);
    }

    private ArrayList<ConversationOperation> getOperationsForConversations(
            Collection<Conversation> conversations, int type, ContentValues values) {
        final ArrayList<ConversationOperation> ops = Lists.newArrayList();
        for (Conversation conv: conversations) {
            ops.add(getOperationForConversation(conv, type, values));
        }
        return ops;
    }

    public ConversationOperation getOperationForConversation(Conversation conv, int type,
            ContentValues values) {
        return new ConversationOperation(type, conv, values);
    }

    public static void addFolderUpdates(ArrayList<Uri> folderUris, ArrayList<Boolean> add,
            ContentValues values) {
        ArrayList<String> folders = new ArrayList<String>();
        for (int i = 0; i < folderUris.size(); i++) {
            folders.add(folderUris.get(i).buildUpon().appendPath(add.get(i) + "").toString());
        }
        values.put(ConversationOperations.FOLDERS_UPDATED,
                TextUtils.join(ConversationOperations.FOLDERS_UPDATED_SPLIT_PATTERN, folders));
    }

    public static void addTargetFolders(Collection<Folder> targetFolders, ContentValues values) {
        values.put(Conversation.UPDATE_FOLDER_COLUMN, FolderList.copyOf(targetFolders).toBlob());
    }

    public ConversationOperation getConversationFolderOperation(Conversation conv,
            ArrayList<Uri> folderUris, ArrayList<Boolean> add, Collection<Folder> targetFolders) {
        return getConversationFolderOperation(conv, folderUris, add, targetFolders,
                new ContentValues());
    }

    public ConversationOperation getConversationFolderOperation(Conversation conv,
            ArrayList<Uri> folderUris, ArrayList<Boolean> add, Collection<Folder> targetFolders,
            ContentValues values) {
        addFolderUpdates(folderUris, add, values);
        addTargetFolders(targetFolders, values);
        return getOperationForConversation(conv, ConversationOperation.UPDATE, values);
    }

    // Convenience methods
    private int apply(Collection<ConversationOperation> operations) {
        return sProvider.apply(operations, this);
    }

    private void undoLocal() {
        sProvider.undo(this);
    }

    public void undo(final Context context, final Uri undoUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor c = context.getContentResolver().query(undoUri, UIProvider.UNDO_PROJECTION,
                        null, null, null);
                if (c != null) {
                    c.close();
                }
            }
        }).start();
        undoLocal();
    }

    /**
     * Delete a group of conversations immediately in the UI and in a single transaction in the
     * underlying provider. See applyAction for argument descriptions
     */
    public int delete(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.DELETE);
    }

    /**
     * As above, for archive
     */
    public int archive(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.ARCHIVE);
    }

    /**
     * As above, for mute
     */
    public int mute(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.MUTE);
    }

    /**
     * As above, for report spam
     */
    public int reportSpam(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.REPORT_SPAM);
    }

    /**
     * As above, for report not spam
     */
    public int reportNotSpam(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.REPORT_NOT_SPAM);
    }

    /**
     * As above, for report phishing
     */
    public int reportPhishing(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.REPORT_PHISHING);
    }

    /**
     * Discard the drafts in the specified conversations
     */
    public int discardDrafts(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.DISCARD_DRAFTS);
    }

    /**
     * As above, for mostly archive
     */
    public int mostlyArchive(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.MOSTLY_ARCHIVE);
    }

    /**
     * As above, for mostly delete
     */
    public int mostlyDelete(Collection<Conversation> conversations) {
        return applyAction(conversations, ConversationOperation.MOSTLY_DELETE);
    }

    /**
     * As above, for mostly destructive updates.
     */
    public int mostlyDestructiveUpdate(Collection<Conversation> conversations,
            ContentValues values) {
        return apply(
                getOperationsForConversations(conversations,
                        ConversationOperation.MOSTLY_DESTRUCTIVE_UPDATE, values));
    }

    /**
     * Convenience method for performing an operation on a group of conversations
     * @param conversations the conversations to be affected
     * @param opAction the action to take
     * @return the sequence number of the operation applied in CC
     */
    private int applyAction(Collection<Conversation> conversations, int opAction) {
        ArrayList<ConversationOperation> ops = Lists.newArrayList();
        for (Conversation conv: conversations) {
            ConversationOperation op =
                    new ConversationOperation(opAction, conv);
            ops.add(op);
        }
        return apply(ops);
    }

    /**
     * Do not make this method dependent on the internal mechanism of the cursor.
     * Currently just calls the parent implementation. If this is ever overriden, take care to
     * ensure that two references map to the same hashcode. If
     * ConversationCursor first == ConversationCursor second,
     * then
     * first.hashCode() == second.hashCode().
     * The {@link ConversationListFragment} relies on this behavior of
     * {@link ConversationCursor#hashCode()} to avoid storing dangerous references to the cursor.
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append(super.toString());
        sb.append(" mName=");
        sb.append(mName);
        sb.append(" mDeferSync=");
        sb.append(mDeferSync);
        sb.append(" mRefreshRequired=");
        sb.append(mRefreshRequired);
        sb.append(" mRefreshReady=");
        sb.append(mRefreshReady);
        sb.append(" mRefreshTask=");
        sb.append(mRefreshTask);
        sb.append(" mPaused=");
        sb.append(mPaused);
        sb.append(" mDeletedCount=");
        sb.append(mDeletedCount);
        sb.append(" mUnderlying=");
        sb.append(mUnderlyingCursor);
        sb.append("}");
        return sb.toString();
    }

    private void resetNotificationActions() {
        // Needs to be on the UI thread because it updates the ConversationCursor's internal
        // state which violates assumptions about how the ListView works and how
        // the ConversationViewPager works if performed off of the UI thread.
        // Also, prevents ConcurrentModificationExceptions on mNotificationTempDeleted.
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                final boolean changed = !mNotificationTempDeleted.isEmpty();

                for (final Conversation conversation : mNotificationTempDeleted) {
                    sProvider.undeleteLocal(conversation.uri, ConversationCursor.this);
                }

                mNotificationTempDeleted.clear();

                if (changed) {
                    notifyDataChanged();
                }
            }
        });
    }

    /**
     * If a destructive notification action was triggered, but has not yet been processed because an
     * "Undo" action is available, we do not want to show the conversation in the list.
     */
    public void handleNotificationActions() {
        // Needs to be on the UI thread because it updates the ConversationCursor's internal
        // state which violates assumptions about how the ListView works and how
        // the ConversationViewPager works if performed off of the UI thread.
        // Also, prevents ConcurrentModificationExceptions on mNotificationTempDeleted.
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                final SparseArrayCompat<NotificationAction> undoNotifications =
                        NotificationActionUtils.sUndoNotifications;
                final Set<Conversation> undoneConversations =
                        NotificationActionUtils.sUndoneConversations;

                final Set<Conversation> undoConversations =
                        Sets.newHashSetWithExpectedSize(undoNotifications.size());

                boolean changed = false;

                for (int i = 0; i < undoNotifications.size(); i++) {
                    final NotificationAction notificationAction =
                            undoNotifications.get(undoNotifications.keyAt(i));

                    // We only care about notifications that were for this folder
                    // or if the action was delete
                    final Folder folder = notificationAction.getFolder();
                    final boolean deleteAction = notificationAction.getNotificationActionType()
                            == NotificationActionType.DELETE;

                    if (folder.conversationListUri.equals(qUri) || deleteAction) {
                        // We only care about destructive actions
                        if (notificationAction.getNotificationActionType().getIsDestructive()) {
                            final Conversation conversation = notificationAction.getConversation();

                            undoConversations.add(conversation);

                            if (!mNotificationTempDeleted.contains(conversation)) {
                                sProvider.deleteLocal(conversation.uri, ConversationCursor.this);
                                mNotificationTempDeleted.add(conversation);

                                changed = true;
                            }
                        }
                    }
                }

                // Remove any conversations from the temporary deleted state
                // if they no longer have an undo notification
                final Iterator<Conversation> iterator = mNotificationTempDeleted.iterator();
                while (iterator.hasNext()) {
                    final Conversation conversation = iterator.next();

                    if (!undoConversations.contains(conversation)) {
                        // We should only be un-deleting local cursor edits
                        // if the notification was undone rather than just
                        // disappearing because the internal cursor
                        // gets updated when the undo goes away via timeout which
                        // will update everything properly.
                        if (undoneConversations.contains(conversation)) {
                            sProvider.undeleteLocal(conversation.uri, ConversationCursor.this);
                            undoneConversations.remove(conversation);
                        }
                        iterator.remove();

                        changed = true;
                    }
                }

                if (changed) {
                    notifyDataChanged();
                }
            }
        });
    }

    @Override
    public void markContentsSeen() {
        ConversationCursorOperationListener.OperationHelper.markContentsSeen(mUnderlyingCursor);
    }

    @Override
    public void emptyFolder() {
        ConversationCursorOperationListener.OperationHelper.emptyFolder(mUnderlyingCursor);
    }
}

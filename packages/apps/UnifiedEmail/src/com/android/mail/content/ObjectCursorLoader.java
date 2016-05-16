/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.mail.content;

import com.android.mail.utils.LogTag;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * A copy of the framework's {@link android.content.CursorLoader} class. Copied because
 * CursorLoader is not parameterized, and we want to parameterize over the underlying cursor type.
 * @param <T>
 */
public class ObjectCursorLoader<T> extends AsyncTaskLoader<ObjectCursor<T>> {
    final ForceLoadContentObserver mObserver;
    protected static final String LOG_TAG = LogTag.getLogTag();

    private Uri mUri;
    final String[] mProjection;
    // Copied over from CursorLoader, but none of our uses specify this. So these are hardcoded to
    // null right here.
    final String mSelection = null;
    final String[] mSelectionArgs = null;
    final String mSortOrder = null;

    /** The underlying cursor that contains the data. */
    ObjectCursor<T> mCursor;

    /** The factory that knows how to create T objects from cursors: one object per row. */
    private final CursorCreator<T> mFactory;

    private int mDebugDelayMs = 0;

    public ObjectCursorLoader(Context context, Uri uri, String[] projection,
            CursorCreator<T> factory) {
        super(context);

        /*
         * If these are null, it's going to crash anyway in loadInBackground(), but this stack trace
         * is much more useful.
         */
        if (factory == null) {
            throw new NullPointerException("The factory cannot be null");
        }

        mObserver = new ForceLoadContentObserver();
        setUri(uri);
        mProjection = projection;
        mFactory = factory;
    }

    /* Runs on a worker thread */
    @Override
    public ObjectCursor<T> loadInBackground() {
        final Cursor inner = getContext().getContentResolver().query(mUri, mProjection,
                mSelection, mSelectionArgs, mSortOrder);
        if (inner == null) {
            // If there's no underlying cursor, there's nothing to do.
            return null;
        }
        // Ensure the cursor window is filled
        inner.getCount();
        inner.registerContentObserver(mObserver);

        // Modifications to the ObjectCursor, create an Object Cursor and fill the cache.
        final ObjectCursor<T> cursor = getObjectCursor(inner);
        cursor.fillCache();

        try {
            if (mDebugDelayMs > 0) {
                Thread.sleep(mDebugDelayMs);
            }
        } catch (InterruptedException e) {}

        return cursor;
    }

    protected ObjectCursor<T> getObjectCursor(Cursor inner) {
        return new ObjectCursor<T>(inner, mFactory);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(ObjectCursor<T> cursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        final Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(ObjectCursor<T> cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("mUri="); writer.println(mUri);
        writer.print(prefix); writer.print("mProjection=");
        writer.println(Arrays.toString(mProjection));
        writer.print(prefix); writer.print("mSelection="); writer.println(mSelection);
        writer.print(prefix); writer.print("mSelectionArgs=");
        writer.println(Arrays.toString(mSelectionArgs));
        writer.print(prefix); writer.print("mSortOrder="); writer.println(mSortOrder);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }

    /**
     * For debugging loader-related race conditions. Delays the background thread load. The delay is
     * currently run after the query is complete.
     *
     * @param delayMs additional delay (in ms) to add to the background load operation
     * @return this object itself, for fluent chaining
     */
    public ObjectCursorLoader<T> setDebugDelay(int delayMs) {
        mDebugDelayMs = delayMs;
        return this;
    }

    public final Uri getUri() {
        return mUri;
    }

    public final void setUri(Uri uri) {
        if (uri == null) {
            throw new NullPointerException("The uri cannot be null");
        }
        mUri = uri;
    }
}

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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;

/**
 * Collects all corpus results for a single query.
 */
public class Suggestions {
    private static final boolean DBG = false;
    private static final String TAG = "QSB.Suggestions";

    /** True if {@link Suggestions#close} has been called. */
    private boolean mClosed = false;
    protected final String mQuery;

    /**
     * The observers that want notifications of changes to the published suggestions.
     * This object may be accessed on any thread.
     */
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private Source mSource;

    private SourceResult mResult;

    private int mRefCount = 0;

    private boolean mDone = false;

    public Suggestions(String query, Source source) {
        mQuery = query;
        mSource = source;
    }

    public void acquire() {
        mRefCount++;
    }

    public void release() {
        mRefCount--;
        if (mRefCount <= 0) {
            close();
        }
    }

    public Source getSource() {
        return mSource;
    }

    /**
     * Marks the suggestions set as complete, regardless of whether all corpora have
     * returned.
     */
    public void done() {
        mDone = true;
    }

    /**
     * Checks whether all sources have reported.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    public boolean isDone() {
        return mDone || mResult != null;
    }

    /**
     * Adds a list of corpus results. Must be called on the UI thread, or before this
     * object is seen by the UI thread.
     */
    public void addResults(SourceResult result) {
        if (isClosed()) {
            result.close();
            return;
        }

        if (DBG) {
            Log.d(TAG, "addResults["+ hashCode() + "] source:" +
                    result.getSource().getName() + " results:" + result.getCount());
        }
        if (!mQuery.equals(result.getUserQuery())) {
          throw new IllegalArgumentException("Got result for wrong query: "
                + mQuery + " != " + result.getUserQuery());
        }
        mResult = result;
        notifyDataSetChanged();
    }

    /**
     * Registers an observer that will be notified when the reported results or
     * the done status changes.
     */
    public void registerDataSetObserver(DataSetObserver observer) {
        if (mClosed) {
            throw new IllegalStateException("registerDataSetObserver() when closed");
        }
        mDataSetObservable.registerObserver(observer);
    }


    /**
     * Unregisters an observer.
     */
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * Calls {@link DataSetObserver#onChanged()} on all observers.
     */
    protected void notifyDataSetChanged() {
        if (DBG) Log.d(TAG, "notifyDataSetChanged()");
        mDataSetObservable.notifyChanged();
    }

    /**
     * Closes all the source results and unregisters all observers.
     */
    private void close() {
        if (DBG) Log.d(TAG, "close() [" + hashCode() + "]");
        if (mClosed) {
            throw new IllegalStateException("Double close()");
        }
        mClosed = true;
        mDataSetObservable.unregisterAll();
        if (mResult != null) {
            mResult.close();
        }
        mResult = null;
    }

    public boolean isClosed() {
        return mClosed;
    }

    @Override
    protected void finalize() {
        if (!mClosed) {
            Log.e(TAG, "LEAK! Finalized without being closed: Suggestions[" + getQuery() + "]");
        }
    }

    public String getQuery() {
        return mQuery;
    }

    /**
     * Gets the list of corpus results reported so far. Do not modify or hang on to
     * the returned iterator.
     */
    public SourceResult getResult() {
        return mResult;
    }

    public SourceResult getWebResult() {
        return mResult;
    }

    /**
     * Gets the number of source results.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    public int getResultCount() {
        if (isClosed()) {
            throw new IllegalStateException("Called getSourceCount() when closed.");
        }
        return mResult == null ? 0 : mResult.getCount();
    }

    @Override
    public String toString() {
        return "Suggestions@" + hashCode() + "{source=" + mSource
                + ",getResultCount()=" + getResultCount() + "}";
    }

}

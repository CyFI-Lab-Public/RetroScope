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
package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;

import java.util.HashMap;

/**
 * Base class for suggestions adapters. The templated class A is the list adapter class.
 */
public abstract class SuggestionsAdapterBase<A> implements SuggestionsAdapter<A> {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsAdapter";

    private DataSetObserver mDataSetObserver;

    private SuggestionCursor mCurrentSuggestions;
    private final HashMap<String, Integer> mViewTypeMap;
    private final SuggestionViewFactory mViewFactory;

    private Suggestions mSuggestions;

    private SuggestionClickListener mSuggestionClickListener;
    private OnFocusChangeListener mOnFocusChangeListener;

    private boolean mClosed = false;

    protected SuggestionsAdapterBase(SuggestionViewFactory viewFactory) {
        mViewFactory = viewFactory;
        mViewTypeMap = new HashMap<String, Integer>();
        for (String viewType : mViewFactory.getSuggestionViewTypes()) {
            if (!mViewTypeMap.containsKey(viewType)) {
                mViewTypeMap.put(viewType, mViewTypeMap.size());
            }
        }
    }

    @Override
    public abstract boolean isEmpty();

    public boolean isClosed() {
        return mClosed;
    }

    public void close() {
        setSuggestions(null);
        mClosed = true;
    }

    @Override
    public void setSuggestionClickListener(SuggestionClickListener listener) {
        mSuggestionClickListener = listener;
    }

    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        mOnFocusChangeListener = l;
    }

    @Override
    public void setSuggestions(Suggestions suggestions) {
        if (mSuggestions == suggestions) {
            return;
        }
        if (mClosed) {
            if (suggestions != null) {
                suggestions.release();
            }
            return;
        }
        if (mDataSetObserver == null) {
            mDataSetObserver = new MySuggestionsObserver();
        }
        // TODO: delay the change if there are no suggestions for the currently visible tab.
        if (mSuggestions != null) {
            mSuggestions.unregisterDataSetObserver(mDataSetObserver);
            mSuggestions.release();
        }
        mSuggestions = suggestions;
        if (mSuggestions != null) {
            mSuggestions.registerDataSetObserver(mDataSetObserver);
        }
        onSuggestionsChanged();
    }

    @Override
    public Suggestions getSuggestions() {
        return mSuggestions;
    }

    @Override
    public abstract SuggestionPosition getSuggestion(long suggestionId);

    protected int getCount() {
        return mCurrentSuggestions == null ? 0 : mCurrentSuggestions.getCount();
    }

    protected SuggestionPosition getSuggestion(int position) {
        if (mCurrentSuggestions == null) return null;
        return new SuggestionPosition(mCurrentSuggestions, position);
    }

    protected int getViewTypeCount() {
        return mViewTypeMap.size();
    }

    private String suggestionViewType(Suggestion suggestion) {
        String viewType = mViewFactory.getViewType(suggestion);
        if (!mViewTypeMap.containsKey(viewType)) {
            throw new IllegalStateException("Unknown viewType " + viewType);
        }
        return viewType;
    }

    protected int getSuggestionViewType(SuggestionCursor cursor, int position) {
        if (cursor == null) {
            return 0;
        }
        cursor.moveTo(position);
        return mViewTypeMap.get(suggestionViewType(cursor));
    }

    protected int getSuggestionViewTypeCount() {
        return mViewTypeMap.size();
    }

    protected View getView(SuggestionCursor suggestions, int position, long suggestionId,
            View convertView, ViewGroup parent) {
        suggestions.moveTo(position);
        View v = mViewFactory.getView(suggestions, suggestions.getUserQuery(), convertView, parent);
        if (v instanceof SuggestionView) {
            ((SuggestionView) v).bindAdapter(this, suggestionId);
        } else {
            SuggestionViewClickListener l = new SuggestionViewClickListener(suggestionId);
            v.setOnClickListener(l);
        }

        if (mOnFocusChangeListener != null) {
            v.setOnFocusChangeListener(mOnFocusChangeListener);
        }
        return v;
    }

    protected void onSuggestionsChanged() {
        if (DBG) Log.d(TAG, "onSuggestionsChanged(" + mSuggestions + ")");
        SuggestionCursor cursor = null;
        if (mSuggestions != null) {
            cursor = mSuggestions.getResult();
        }
        changeSuggestions(cursor);
    }

    public SuggestionCursor getCurrentSuggestions() {
        return mCurrentSuggestions;
    }

    /**
     * Replace the cursor.
     *
     * This does not close the old cursor. Instead, all the cursors are closed in
     * {@link #setSuggestions(Suggestions)}.
     */
    private void changeSuggestions(SuggestionCursor newCursor) {
        if (DBG) {
            Log.d(TAG, "changeCursor(" + newCursor + ") count=" +
                    (newCursor == null ? 0 : newCursor.getCount()));
        }
        if (newCursor == mCurrentSuggestions) {
            if (newCursor != null) {
                // Shortcuts may have changed without the cursor changing.
                notifyDataSetChanged();
            }
            return;
        }
        mCurrentSuggestions = newCursor;
        if (mCurrentSuggestions != null) {
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    @Override
    public void onSuggestionClicked(long suggestionId) {
        if (mClosed) {
            Log.w(TAG, "onSuggestionClicked after close");
        } else if (mSuggestionClickListener != null) {
            mSuggestionClickListener.onSuggestionClicked(this, suggestionId);
        }
    }

    @Override
    public void onSuggestionQueryRefineClicked(long suggestionId) {
        if (mClosed) {
            Log.w(TAG, "onSuggestionQueryRefineClicked after close");
        } else if (mSuggestionClickListener != null) {
            mSuggestionClickListener.onSuggestionQueryRefineClicked(this, suggestionId);
        }
    }

    @Override
    public abstract A getListAdapter();

    protected abstract void notifyDataSetInvalidated();

    protected abstract void notifyDataSetChanged();

    private class MySuggestionsObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onSuggestionsChanged();
        }
    }

    private class SuggestionViewClickListener implements View.OnClickListener {
        private final long mSuggestionId;
        public SuggestionViewClickListener(long suggestionId) {
            mSuggestionId = suggestionId;
        }
        @Override
        public void onClick(View v) {
            onSuggestionClicked(mSuggestionId);
        }
    }

}

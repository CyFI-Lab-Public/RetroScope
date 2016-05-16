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

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestion;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Base class for suggestion views.
 */
public abstract class BaseSuggestionView extends RelativeLayout implements SuggestionView {

    protected TextView mText1;
    protected TextView mText2;
    protected ImageView mIcon1;
    protected ImageView mIcon2;
    private long mSuggestionId;
    private SuggestionsAdapter<?> mAdapter;

    public BaseSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BaseSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseSuggestionView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mText1 = (TextView) findViewById(R.id.text1);
        mText2 = (TextView) findViewById(R.id.text2);
        mIcon1 = (ImageView) findViewById(R.id.icon1);
        mIcon2 = (ImageView) findViewById(R.id.icon2);
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        setOnClickListener(new ClickListener());
    }

    @Override
    public void bindAdapter(SuggestionsAdapter<?> adapter, long suggestionId) {
        mAdapter = adapter;
        mSuggestionId = suggestionId;
    }

    protected boolean isFromHistory(Suggestion suggestion) {
        return suggestion.isSuggestionShortcut() || suggestion.isHistorySuggestion();
    }

    /**
     * Sets the first text line.
     */
    protected void setText1(CharSequence text) {
        mText1.setText(text);
    }

    /**
     * Sets the second text line.
     */
    protected void setText2(CharSequence text) {
        mText2.setText(text);
        if (TextUtils.isEmpty(text)) {
            mText2.setVisibility(GONE);
        } else {
            mText2.setVisibility(VISIBLE);
        }
    }

    protected void onSuggestionClicked() {
        if (mAdapter != null) {
            mAdapter.onSuggestionClicked(mSuggestionId);
        }
    }

    protected void onSuggestionQueryRefineClicked() {
        if (mAdapter != null) {
            mAdapter.onSuggestionQueryRefineClicked(mSuggestionId);
        }
    }

    private class ClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            onSuggestionClicked();
        }
    }

}

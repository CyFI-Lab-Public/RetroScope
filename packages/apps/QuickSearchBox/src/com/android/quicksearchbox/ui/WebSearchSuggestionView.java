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

import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionFormatter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

/**
 * View for web search suggestions.
 */
public class WebSearchSuggestionView extends BaseSuggestionView {

    private static final String VIEW_ID = "web_search";

    private final SuggestionFormatter mSuggestionFormatter;

    public WebSearchSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSuggestionFormatter = QsbApplication.get(context).getSuggestionFormatter();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        KeyListener keyListener = new KeyListener();
        setOnKeyListener(keyListener);
        mIcon2.setOnKeyListener(keyListener);
        mIcon2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSuggestionQueryRefineClicked();
            }
        });
        mIcon2.setFocusable(true);
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        super.bindAsSuggestion(suggestion, userQuery);

        CharSequence text1 = mSuggestionFormatter.formatSuggestion(userQuery,
                suggestion.getSuggestionText1());
        setText1(text1);
        setIsHistorySuggestion(suggestion.isHistorySuggestion());
    }

    private void setIsHistorySuggestion(boolean isHistory) {
        if (isHistory) {
            mIcon1.setImageResource(R.drawable.ic_history_suggestion);
            mIcon1.setVisibility(VISIBLE);
        } else {
            mIcon1.setVisibility(INVISIBLE);
        }
    }

    private class KeyListener implements View.OnKeyListener {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            boolean consumed = false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && v != mIcon2) {
                    consumed = mIcon2.requestFocus();
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && v == mIcon2) {
                    consumed = requestFocus();
                }
            }
            return consumed;
        }
    }

    public static class Factory extends SuggestionViewInflater {

        public Factory(Context context) {
            super(VIEW_ID, WebSearchSuggestionView.class, R.layout.web_search_suggestion, context);
        }

        @Override
        public boolean canCreateView(Suggestion suggestion) {
            return suggestion.isWebSearchSuggestion();
        }
    }

}

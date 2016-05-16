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

package com.android.mail.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.android.mail.ConversationListContext;
import com.android.mail.utils.Utils;
import com.android.mail.R;

/**
 * This class is used to show a custom actionbar for the search activity. This doesn't have any
 * custom views, but it shows/hides various menu items based on the viewmode.
 */
public class SearchMailActionBarView extends MailActionBarView {

    public SearchMailActionBarView(Context context) {
        this(context, null);
    }

    public SearchMailActionBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchMailActionBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Utils.setMenuItemVisibility(menu, R.id.manage_folders_item, false);
        switch (getMode()) {
            case ViewMode.SEARCH_RESULTS_LIST:
                setSearchQueryTerm();
                mActionBar.setDisplayHomeAsUpEnabled(true);
                // And immediately give up focus to avoid keyboard popping and suggestions.
                clearSearchFocus();
                break;
            case ViewMode.SEARCH_RESULTS_CONVERSATION:
                if (mIsOnTablet) {
                    setSearchQueryTerm();
                }
                mActionBar.setDisplayHomeAsUpEnabled(true);
                // And immediately give up focus to avoid keyboard popping and suggestions.
                clearSearchFocus();
                break;
        }
        return false;
    }

    @Override
    public void onViewModeChanged(int newMode) {
        super.onViewModeChanged(newMode);
        switch (getMode()) {
            case ViewMode.SEARCH_RESULTS_LIST:
                setEmptyMode();
                break;
        }
    }

    /**
     * Remove focus from the search field to avoid
     * 1. The keyboard popping in and out.
     * 2. The search suggestions shown up.
     */
    private void clearSearchFocus() {
        // Remove focus from the search action menu in search results mode so
        // the IME and the suggestions don't get in the way.
        final MenuItem search = getSearch();
        if (search != null) {
            final SearchView searchWidget = (SearchView) search.getActionView();
            searchWidget.clearFocus();
        }
    }

    /**
     * Sets the query term in the text field, so the user can see what was searched for.
     */
    private void setSearchQueryTerm() {
        final MenuItem search = getSearch();
        if (search != null) {
            search.expandActionView();
            final String query = mActivity.getIntent().getStringExtra(
                    ConversationListContext.EXTRA_SEARCH_QUERY);
            final SearchView searchWidget = (SearchView) search.getActionView();
            if (!TextUtils.isEmpty(query)) {
                searchWidget.setQuery(query, false);
            }
        }
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        super.onMenuItemActionCollapse(item);
        // When we are in the search activity, back closes the search action mode. At that point
        // we want to quit the activity entirely.
        final int mode = getMode();
        if (mode == ViewMode.SEARCH_RESULTS_LIST
                || (Utils.showTwoPaneSearchResults(getContext())
                        && mode == ViewMode.SEARCH_RESULTS_CONVERSATION)) {

            // When the action menu is collapsed, the search activity has finished.  We should exit
            // search at this point
            mController.exitSearchMode();
        }
        // The return value here is whether we want to collapse the action mode. Since we want to
        // collapse the action mode, we should return true.
        return true;
    }
}

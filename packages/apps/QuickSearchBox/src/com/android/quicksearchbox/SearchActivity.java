/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.android.common.Search;
import com.android.quicksearchbox.ui.SearchActivityView;
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;

import java.io.File;

/**
 * The main activity for Quick Search Box. Shows the search UI.
 *
 */
public class SearchActivity extends Activity {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchActivity";

    private static final String SCHEME_CORPUS = "qsb.corpus";

    private static final String INTENT_EXTRA_TRACE_START_UP = "trace_start_up";

    // Keys for the saved instance state.
    private static final String INSTANCE_KEY_QUERY = "query";

    private static final String ACTIVITY_HELP_CONTEXT = "search";

    private boolean mTraceStartUp;
    // Measures time from for last onCreate()/onNewIntent() call.
    private LatencyTracker mStartLatencyTracker;
    // Measures time spent inside onCreate()
    private LatencyTracker mOnCreateTracker;
    private int mOnCreateLatency;
    // Whether QSB is starting. True between the calls to onCreate()/onNewIntent() and onResume().
    private boolean mStarting;
    // True if the user has taken some action, e.g. launching a search, voice search,
    // or suggestions, since QSB was last started.
    private boolean mTookAction;

    private SearchActivityView mSearchActivityView;

    private Source mSource;

    private Bundle mAppSearchData;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSuggestionsTask = new Runnable() {
        @Override
        public void run() {
            updateSuggestions();
        }
    };

    private final Runnable mShowInputMethodTask = new Runnable() {
        @Override
        public void run() {
            mSearchActivityView.showInputMethodForQuery();
        }
    };

    private OnDestroyListener mDestroyListener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mTraceStartUp = getIntent().hasExtra(INTENT_EXTRA_TRACE_START_UP);
        if (mTraceStartUp) {
            String traceFile = new File(getDir("traces", 0), "qsb-start.trace").getAbsolutePath();
            Log.i(TAG, "Writing start-up trace to " + traceFile);
            Debug.startMethodTracing(traceFile);
        }
        recordStartTime();
        if (DBG) Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        // This forces the HTTP request to check the users domain to be
        // sent as early as possible.
        QsbApplication.get(this).getSearchBaseUrlHelper();

        mSource = QsbApplication.get(this).getGoogleSource();

        mSearchActivityView = setupContentView();

        if (getConfig().showScrollingResults()) {
            mSearchActivityView.setMaxPromotedResults(getConfig().getMaxPromotedResults());
        } else {
            mSearchActivityView.limitResultsToViewHeight();
        }

        mSearchActivityView.setSearchClickListener(new SearchActivityView.SearchClickListener() {
            @Override
            public boolean onSearchClicked(int method) {
                return SearchActivity.this.onSearchClicked(method);
            }
        });

        mSearchActivityView.setQueryListener(new SearchActivityView.QueryListener() {
            @Override
            public void onQueryChanged() {
                updateSuggestionsBuffered();
            }
        });

        mSearchActivityView.setSuggestionClickListener(new ClickHandler());

        mSearchActivityView.setVoiceSearchButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onVoiceSearchClicked();
            }
        });

        View.OnClickListener finishOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
        mSearchActivityView.setExitClickListener(finishOnClick);

        // First get setup from intent
        Intent intent = getIntent();
        setupFromIntent(intent);
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState);

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSearchActivityView.start();

        recordOnCreateDone();
    }

    protected SearchActivityView setupContentView() {
        setContentView(R.layout.search_activity);
        return (SearchActivityView) findViewById(R.id.search_activity_view);
    }

    protected SearchActivityView getSearchActivityView() {
        return mSearchActivityView;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DBG) Log.d(TAG, "onNewIntent()");
        recordStartTime();
        setIntent(intent);
        setupFromIntent(intent);
    }

    private void recordStartTime() {
        mStartLatencyTracker = new LatencyTracker();
        mOnCreateTracker = new LatencyTracker();
        mStarting = true;
        mTookAction = false;
    }

    private void recordOnCreateDone() {
        mOnCreateLatency = mOnCreateTracker.getLatency();
    }

    protected void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        String query = savedInstanceState.getString(INSTANCE_KEY_QUERY);
        setQuery(query, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // We don't save appSearchData, since we always get the value
        // from the intent and the user can't change it.

        outState.putString(INSTANCE_KEY_QUERY, getQuery());
    }

    private void setupFromIntent(Intent intent) {
        if (DBG) Log.d(TAG, "setupFromIntent(" + intent.toUri(0) + ")");
        String corpusName = getCorpusNameFromUri(intent.getData());
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);
        boolean selectAll = intent.getBooleanExtra(SearchManager.EXTRA_SELECT_QUERY, false);

        setQuery(query, selectAll);
        mAppSearchData = appSearchData;

    }

    private String getCorpusNameFromUri(Uri uri) {
        if (uri == null) return null;
        if (!SCHEME_CORPUS.equals(uri.getScheme())) return null;
        return uri.getAuthority();
    }

    private QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    private Config getConfig() {
        return getQsbApplication().getConfig();
    }

    protected SearchSettings getSettings() {
        return getQsbApplication().getSettings();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider();
    }

    private Logger getLogger() {
        return getQsbApplication().getLogger();
    }

    @VisibleForTesting
    public void setOnDestroyListener(OnDestroyListener l) {
        mDestroyListener = l;
    }

    @Override
    protected void onDestroy() {
        if (DBG) Log.d(TAG, "onDestroy()");
        mSearchActivityView.destroy();
        super.onDestroy();
        if (mDestroyListener != null) {
            mDestroyListener.onDestroyed();
        }
    }

    @Override
    protected void onStop() {
        if (DBG) Log.d(TAG, "onStop()");
        if (!mTookAction) {
            // TODO: This gets logged when starting other activities, e.g. by opening the search
            // settings, or clicking a notification in the status bar.
            // TODO we should log both sets of suggestions in 2-pane mode
            getLogger().logExit(getCurrentSuggestions(), getQuery().length());
        }
        // Close all open suggestion cursors. The query will be redone in onResume()
        // if we come back to this activity.
        mSearchActivityView.clearSuggestions();
        mSearchActivityView.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (DBG) Log.d(TAG, "onPause()");
        mSearchActivityView.onPause();
        super.onPause();
    }

    @Override
    protected void onRestart() {
        if (DBG) Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        if (DBG) Log.d(TAG, "onResume()");
        super.onResume();
        updateSuggestionsBuffered();
        mSearchActivityView.onResume();
        if (mTraceStartUp) Debug.stopMethodTracing();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Since the menu items are dynamic, we recreate the menu every time.
        menu.clear();
        createMenuItems(menu, true);
        return true;
    }

    public void createMenuItems(Menu menu, boolean showDisabled) {
        getQsbApplication().getHelp().addHelpMenuItem(menu, ACTIVITY_HELP_CONTEXT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Launch the IME after a bit
            mHandler.postDelayed(mShowInputMethodTask, 0);
        }
    }

    protected String getQuery() {
        return mSearchActivityView.getQuery();
    }

    protected void setQuery(String query, boolean selectAll) {
        mSearchActivityView.setQuery(query, selectAll);
    }

    /**
     * @return true if a search was performed as a result of this click, false otherwise.
     */
    protected boolean onSearchClicked(int method) {
        String query = CharMatcher.WHITESPACE.trimAndCollapseFrom(getQuery(), ' ');
        if (DBG) Log.d(TAG, "Search clicked, query=" + query);

        // Don't do empty queries
        if (TextUtils.getTrimmedLength(query) == 0) return false;

        mTookAction = true;

        // Log search start
        getLogger().logSearch(method, query.length());

        // Start search
        startSearch(mSource, query);
        return true;
    }

    protected void startSearch(Source searchSource, String query) {
        Intent intent = searchSource.createSearchIntent(query, mAppSearchData);
        launchIntent(intent);
    }

    protected void onVoiceSearchClicked() {
        if (DBG) Log.d(TAG, "Voice Search clicked");

        mTookAction = true;

        // Log voice search start
        getLogger().logVoiceSearch();

        // Start voice search
        Intent intent = mSource.createVoiceSearchIntent(mAppSearchData);
        launchIntent(intent);
    }

    protected Source getSearchSource() {
        return mSource;
    }

    protected SuggestionCursor getCurrentSuggestions() {
        return mSearchActivityView.getSuggestions().getResult();
    }

    protected SuggestionPosition getCurrentSuggestions(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition pos = adapter.getSuggestion(id);
        if (pos == null) {
            return null;
        }
        SuggestionCursor suggestions = pos.getCursor();
        int position = pos.getPosition();
        if (suggestions == null) {
            return null;
        }
        int count = suggestions.getCount();
        if (position < 0 || position >= count) {
            Log.w(TAG, "Invalid suggestion position " + position + ", count = " + count);
            return null;
        }
        suggestions.moveTo(position);
        return pos;
    }

    protected void launchIntent(Intent intent) {
        if (DBG) Log.d(TAG, "launchIntent " + intent);
        if (intent == null) {
            return;
        }
        try {
            startActivity(intent);
        } catch (RuntimeException ex) {
            // Since the intents for suggestions specified by suggestion providers,
            // guard against them not being handled, not allowed, etc.
            Log.e(TAG, "Failed to start " + intent.toUri(0), ex);
        }
    }

    private boolean launchSuggestion(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return false;

        if (DBG) Log.d(TAG, "Launching suggestion " + id);
        mTookAction = true;

        // Log suggestion click
        getLogger().logSuggestionClick(id, suggestion.getCursor(),
                Logger.SUGGESTION_CLICK_TYPE_LAUNCH);

        // Launch intent
        launchSuggestion(suggestion.getCursor(), suggestion.getPosition());

        return true;
    }

    protected void launchSuggestion(SuggestionCursor suggestions, int position) {
        suggestions.moveTo(position);
        Intent intent = SuggestionUtils.getSuggestionIntent(suggestions, mAppSearchData);
        launchIntent(intent);
    }

    protected void refineSuggestion(SuggestionsAdapter<?> adapter, long id) {
        if (DBG) Log.d(TAG, "query refine clicked, pos " + id);
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) {
            return;
        }
        String query = suggestion.getSuggestionQuery();
        if (TextUtils.isEmpty(query)) {
            return;
        }

        // Log refine click
        getLogger().logSuggestionClick(id, suggestion.getCursor(),
                Logger.SUGGESTION_CLICK_TYPE_REFINE);

        // Put query + space in query text view
        String queryWithSpace = query + ' ';
        setQuery(queryWithSpace, false);
        updateSuggestions();
        mSearchActivityView.focusQueryTextView();
    }

    private void updateSuggestionsBuffered() {
        if (DBG) Log.d(TAG, "updateSuggestionsBuffered()");
        mHandler.removeCallbacks(mUpdateSuggestionsTask);
        long delay = getConfig().getTypingUpdateSuggestionsDelayMillis();
        mHandler.postDelayed(mUpdateSuggestionsTask, delay);
    }

    private void gotSuggestions(Suggestions suggestions) {
        if (mStarting) {
            mStarting = false;
            String source = getIntent().getStringExtra(Search.SOURCE);
            int latency = mStartLatencyTracker.getLatency();
            getLogger().logStart(mOnCreateLatency, latency, source);
            getQsbApplication().onStartupComplete();
        }
    }

    public void updateSuggestions() {
        if (DBG) Log.d(TAG, "updateSuggestions()");
        final String query = CharMatcher.WHITESPACE.trimLeadingFrom(getQuery());
        updateSuggestions(query, mSource);
    }

    protected void updateSuggestions(String query, Source source) {
        if (DBG) Log.d(TAG, "updateSuggestions(\"" + query+"\"," + source + ")");
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(
                query, source);

        // Log start latency if this is the first suggestions update
        gotSuggestions(suggestions);

        showSuggestions(suggestions);
    }

    protected void showSuggestions(Suggestions suggestions) {
        mSearchActivityView.setSuggestions(suggestions);
    }

    private class ClickHandler implements SuggestionClickListener {

        @Override
        public void onSuggestionClicked(SuggestionsAdapter<?> adapter, long id) {
            launchSuggestion(adapter, id);
        }

        @Override
        public void onSuggestionQueryRefineClicked(SuggestionsAdapter<?> adapter, long id) {
            refineSuggestion(adapter, id);
        }
    }

    public interface OnDestroyListener {
        void onDestroyed();
    }

}

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

import com.android.common.Search;
import com.android.common.speech.Recognition;
import com.android.quicksearchbox.util.Util;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.text.Annotation;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Random;

/**
 * Search widget provider.
 *
 */
public class SearchWidgetProvider extends BroadcastReceiver {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SearchWidgetProvider";

    /**
     * The {@link Search#SOURCE} value used when starting searches from the search widget.
     */
    private static final String WIDGET_SEARCH_SOURCE = "launcher-widget";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG) Log.d(TAG, "onReceive(" + intent.toUri(0) + ")");
        String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)) {
            // nothing needs doing
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            updateSearchWidgets(context);
        } else {
            if (DBG) Log.d(TAG, "Unhandled intent action=" + action);
        }
    }

    private static SearchWidgetState[] getSearchWidgetStates(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(myComponentName(context));
        SearchWidgetState[] states = new SearchWidgetState[appWidgetIds.length];
        for (int i = 0; i<appWidgetIds.length; ++i) {
            states[i] = getSearchWidgetState(context, appWidgetIds[i]);
        }
        return states;
    }


    /**
     * Updates all search widgets.
     */
    public static void updateSearchWidgets(Context context) {
        if (DBG) Log.d(TAG, "updateSearchWidgets");
        SearchWidgetState[] states = getSearchWidgetStates(context);

        for (SearchWidgetState state : states) {
            state.updateWidget(context, AppWidgetManager.getInstance(context));
        }
    }

    /**
     * Gets the component name of this search widget provider.
     */
    private static ComponentName myComponentName(Context context) {
        String pkg = context.getPackageName();
        String cls = pkg + ".SearchWidgetProvider";
        return new ComponentName(pkg, cls);
    }

    private static Intent createQsbActivityIntent(Context context, String action,
            Bundle widgetAppData) {
        Intent qsbIntent = new Intent(action);
        qsbIntent.setPackage(context.getPackageName());
        qsbIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        qsbIntent.putExtra(SearchManager.APP_DATA, widgetAppData);
        return qsbIntent;
    }

    private static SearchWidgetState getSearchWidgetState(Context context, int appWidgetId) {
        if (DBG) Log.d(TAG, "Creating appwidget state " + appWidgetId);
        SearchWidgetState state = new SearchWidgetState(appWidgetId);

        Bundle widgetAppData = new Bundle();
        widgetAppData.putString(Search.SOURCE, WIDGET_SEARCH_SOURCE);

        // Text field click
        Intent qsbIntent = createQsbActivityIntent(
                context,
                SearchManager.INTENT_ACTION_GLOBAL_SEARCH,
                widgetAppData);
        state.setQueryTextViewIntent(qsbIntent);

        // Voice search button
        Intent voiceSearchIntent = getVoiceSearchIntent(context, widgetAppData);
        state.setVoiceSearchIntent(voiceSearchIntent);

        return state;
    }

    private static Intent getVoiceSearchIntent(Context context, Bundle widgetAppData) {
        VoiceSearch voiceSearch = QsbApplication.get(context).getVoiceSearch();
        return voiceSearch.createVoiceWebSearchIntent(widgetAppData);
    }

    private static class SearchWidgetState {
        private final int mAppWidgetId;
        private Intent mQueryTextViewIntent;
        private Intent mVoiceSearchIntent;

        public SearchWidgetState(int appWidgetId) {
            mAppWidgetId = appWidgetId;
        }

        public void setQueryTextViewIntent(Intent queryTextViewIntent) {
            mQueryTextViewIntent = queryTextViewIntent;
        }

        public void setVoiceSearchIntent(Intent voiceSearchIntent) {
            mVoiceSearchIntent = voiceSearchIntent;
        }

        public void updateWidget(Context context,AppWidgetManager appWidgetMgr) {
            if (DBG) Log.d(TAG, "Updating appwidget " + mAppWidgetId);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);

            setOnClickActivityIntent(context, views, R.id.search_widget_text,
                    mQueryTextViewIntent);
            // Voice Search button
            if (mVoiceSearchIntent != null) {
                setOnClickActivityIntent(context, views, R.id.search_widget_voice_btn,
                        mVoiceSearchIntent);
                views.setViewVisibility(R.id.search_widget_voice_btn, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.search_widget_voice_btn, View.GONE);
            }

            appWidgetMgr.updateAppWidget(mAppWidgetId, views);
        }

        private void setOnClickActivityIntent(Context context, RemoteViews views, int viewId,
                Intent intent) {
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(viewId, pendingIntent);
        }
    }

}

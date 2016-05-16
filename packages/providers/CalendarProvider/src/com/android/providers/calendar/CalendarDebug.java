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

package com.android.providers.calendar;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.view.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays info about all the user's calendars, for debugging.
 *
 * The info is displayed as a ListActivity, where each entry has the calendar name
 * followed by information about the calendar.
 */
public class CalendarDebug extends ListActivity {
    private static final String[] CALENDARS_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
    };
    private static final int INDEX_ID = 0;
    private static final int INDEX_DISPLAY_NAME = 1;

    private static final String[] EVENTS_PROJECTION = new String[]{
            CalendarContract.Events._ID,
    };
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";

    private ContentResolver mContentResolver;
    private ListActivity mActivity;

    /**
     *  Task to fetch info from the database and display as a ListActivity.
     */
    private class FetchInfoTask extends AsyncTask<Void, Void, List<Map<String, String>>> {
        /**
         * Starts spinner while task is running.
         *
         * @see #onPostExecute
         * @see #doInBackground
         */
        @Override
        protected void onPreExecute() {
              setProgressBarIndeterminateVisibility(true);
        }

        /**
         * Fetches debugging info from the database
         * @param params Void
         * @return a Map for each calendar
         */
        @Override
        protected List<Map<String, String>> doInBackground(Void... params) {
            Cursor cursor = null;
            // items is the list of items to display in the list.
            List<Map<String, String>> items = new ArrayList<Map<String, String>>();
            try {
                cursor = mContentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                        CALENDARS_PROJECTION,
                        null, null /* selectionArgs */,
                        CalendarContract.Calendars.DEFAULT_SORT_ORDER);
                if (cursor == null) {
                    addItem(items, mActivity.getString(R.string.calendar_info_error), "");
                } else {
                    while (cursor.moveToNext()) {
                        // Process each calendar
                        int id = cursor.getInt(INDEX_ID);
                        int eventCount = -1;
                        int dirtyCount = -1;
                        String displayName = cursor.getString(INDEX_DISPLAY_NAME);

                        // Compute number of events in the calendar
                        String where = CalendarContract.Events.CALENDAR_ID + "=" + id;
                        Cursor eventCursor = mContentResolver.query(
                                CalendarContract.Events.CONTENT_URI, EVENTS_PROJECTION, where,
                                null, null);
                        try {
                            eventCount = eventCursor.getCount();
                        } finally {
                            eventCursor.close();
                        }

                        // Compute number of dirty events in the calendar
                        String dirtyWhere = CalendarContract.Events.CALENDAR_ID + "=" + id
                                + " AND " + CalendarContract.Events.DIRTY + "=1";
                        Cursor dirtyCursor = mContentResolver.query(
                                CalendarContract.Events.CONTENT_URI, EVENTS_PROJECTION, dirtyWhere,
                                null, null);
                        try {
                            dirtyCount = dirtyCursor.getCount();
                        } finally {
                            dirtyCursor.close();
                        }

                        // Format the output
                        String text;
                        if (dirtyCount == 0) {
                            text = mActivity.getString(R.string.calendar_info_events,
                                    eventCount);
                        } else {
                            text = mActivity.getString(R.string.calendar_info_events_dirty,
                                    eventCount, dirtyCount);
                        }

                        addItem(items, displayName, text);
                    }
                }
            } catch (Exception e) {
                // Want to catch all exceptions.  The point of this code is to debug
                // when something bad is happening.
                addItem(items, mActivity.getString(R.string.calendar_info_error), e.toString());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (items.size() == 0) {
                addItem(items, mActivity.getString(R.string.calendar_info_no_calendars), "");
            }
            return items;
        }

        /**
         * Runs on the UI thread to display the debugging info.
         *
         * @param items The info items to display.
         * @see #onPreExecute
         * @see #doInBackground
         */
        @Override
        protected void onPostExecute(List<Map<String, String>> items) {
            setProgressBarIndeterminateVisibility(false);
            ListAdapter adapter = new SimpleAdapter(mActivity, items,
                    android.R.layout.simple_list_item_2, new String[]{KEY_TITLE, KEY_TEXT},
                    new int[]{android.R.id.text1, android.R.id.text2});

            // Bind to our new adapter.
            setListAdapter(adapter);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        mActivity = this;
        mContentResolver = getContentResolver();
        getListView(); // Instantiate, for spinner
        new FetchInfoTask().execute();

    }

    /**
     * Adds an item to the item map
     * @param items The item map to update
     * @param title Title of the item
     * @param text Text of the item
     */
    protected void addItem(List<Map<String, String>> items, String title, String text) {
        Map<String, String> itemMap = new HashMap<String, String>();
        itemMap.put(KEY_TITLE, title);
        itemMap.put(KEY_TEXT, text);
        items.add(itemMap);
    }
}

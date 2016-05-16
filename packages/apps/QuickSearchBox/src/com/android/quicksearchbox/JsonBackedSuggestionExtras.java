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

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * SuggestionExtras taking values from a {@link JSONObject}.
 */
public class JsonBackedSuggestionExtras implements SuggestionExtras {
    private static final String TAG = "QSB.JsonBackedSuggestionExtras";

    private final JSONObject mExtras;
    private final Collection<String> mColumns;

    public JsonBackedSuggestionExtras(String json) throws JSONException {
        mExtras = new JSONObject(json);
        mColumns = new ArrayList<String>(mExtras.length());
        Iterator<String> it = mExtras.keys();
        while (it.hasNext()) {
            mColumns.add(it.next());
        }
    }

    public JsonBackedSuggestionExtras(SuggestionExtras extras) throws JSONException {
        mExtras = new JSONObject();
        mColumns = extras.getExtraColumnNames();
        for (String column : extras.getExtraColumnNames()) {
            String value = extras.getExtra(column);
            mExtras.put(column, value == null ? JSONObject.NULL : value);
        }
    }

    public String getExtra(String columnName) {
        try {
            if (mExtras.isNull(columnName)) {
                return null;
            } else {
                return mExtras.getString(columnName);
            }
        } catch (JSONException e) {
            Log.w(TAG, "Could not extract JSON extra", e);
            return null;
        }
    }

    public Collection<String> getExtraColumnNames() {
        return mColumns;
    }

    @Override
    public String toString() {
        return mExtras.toString();
    }

    public String toJsonString() {
        return toString();
    }

}

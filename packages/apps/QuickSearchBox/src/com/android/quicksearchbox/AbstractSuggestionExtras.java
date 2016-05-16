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

import java.util.Collection;
import java.util.HashSet;

/**
 * Abstract SuggestionExtras supporting flattening to JSON.
 */
public abstract class AbstractSuggestionExtras implements SuggestionExtras {

    private final SuggestionExtras mMore;

    protected AbstractSuggestionExtras(SuggestionExtras more) {
        mMore = more;
    }

    public Collection<String> getExtraColumnNames() {
        HashSet<String> columns = new HashSet<String>();
        columns.addAll(doGetExtraColumnNames());
        if (mMore != null) {
            columns.addAll(mMore.getExtraColumnNames());
        }
        return columns;
    }

    protected abstract Collection<String> doGetExtraColumnNames();

    public String getExtra(String columnName) {
        String extra = doGetExtra(columnName);
        if (extra == null && mMore != null) {
            extra = mMore.getExtra(columnName);
        }
        return extra;
    }

    protected abstract String doGetExtra(String columnName);

    public String toJsonString() throws JSONException {
        return new JsonBackedSuggestionExtras(this).toString();
    }

}

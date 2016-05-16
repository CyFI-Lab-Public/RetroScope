/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.providers.contacts.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a selection clause by concatenating several clauses with AND.
 */
public class SelectionBuilder {
    private static final String[] EMPTY_STRING_ARRAY =  new String[0];
    private final List<String> mWhereClauses;

    /**
     * @param baseSelection The base selection to start with. This is typically
     *      the user supplied selection arg. Pass null if no base selection is
     *      required.
     */
    public SelectionBuilder(String baseSelection) {
        mWhereClauses = new ArrayList<String>();
        addClause(baseSelection);
    }

    /**
     * Adds a new clause to the selection. Nothing is added if the supplied clause
     * is null or empty.
     */
    public SelectionBuilder addClause(String clause) {
        if (!TextUtils.isEmpty(clause)) {
            mWhereClauses.add(clause);
        }
        return this;
    }

    /**
     * Returns a combined selection clause with AND of all clauses added using
     * {@link #addClause(String)}. Returns null if no clause has been added or
     * only null/empty clauses have been added till now.
     */
    public String build() {
        if (mWhereClauses.size() == 0) {
            return null;
        }
        return DbQueryUtils.concatenateClauses(mWhereClauses.toArray(EMPTY_STRING_ARRAY));
    }
}

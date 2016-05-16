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

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Set;

/**
 * Static methods for helping us build database query selection strings.
 */
public class DbQueryUtils {
    // Static class with helper methods, so private constructor.
    private DbQueryUtils() {
    }

    /** Returns a WHERE clause asserting equality of a field to a value. */
    public static String getEqualityClause(String field, String value) {
        return getClauseWithOperator(field, "=", value);
    }

    /** Returns a WHERE clause asserting equality of a field to a value. */
    public static String getEqualityClause(String field, long value) {
        return getClauseWithOperator(field, "=", value);
    }

    /** Returns a WHERE clause asserting in-equality of a field to a value. */
    public static String getInequalityClause(String field, long value) {
        return getClauseWithOperator(field, "!=", value);
    }

    private static String getClauseWithOperator(String field, String operator, String value) {
        StringBuilder clause = new StringBuilder();
        clause.append("(");
        clause.append(field);
        clause.append(" ").append(operator).append(" ");
        DatabaseUtils.appendEscapedSQLString(clause, value);
        clause.append(")");
        return clause.toString();
    }

    private static String getClauseWithOperator(String field, String operator, long value) {
        StringBuilder clause = new StringBuilder();
        clause.append("(");
        clause.append(field);
        clause.append(" ").append(operator).append(" ");
        clause.append(value);
        clause.append(")");
        return clause.toString();
    }

    /** Concatenates any number of clauses using "AND". */
    public static String concatenateClauses(String... clauses) {
        StringBuilder builder = new StringBuilder();
        for (String clause : clauses) {
            if (!TextUtils.isEmpty(clause)) {
                if (builder.length() > 0) {
                    builder.append(" AND ");
                }
                builder.append("(");
                builder.append(clause);
                builder.append(")");
            }
        }
        return builder.toString();
    }

    /**
     * Checks if the given ContentValues contains values within the projection
     * map.
     *
     * @throws IllegalArgumentException if any value in values is not found in
     * the projection map.
     */
    public static void checkForSupportedColumns(HashMap<String, String> projectionMap,
            ContentValues values) {
        checkForSupportedColumns(projectionMap.keySet(), values, "Is invalid.");
    }

    /**
     * @see #checkForSupportedColumns(HashMap, ContentValues)
     */
    public static void checkForSupportedColumns(Set<String> allowedColumns, ContentValues values,
            String msgSuffix) {
        for (String requestedColumn : values.keySet()) {
            if (!allowedColumns.contains(requestedColumn)) {
                throw new IllegalArgumentException("Column '" + requestedColumn + "'. " +
                        msgSuffix);
            }
        }
    }

    /**
     * Escape values to be used in LIKE sqlite clause.
     *
     * The LIKE clause has two special characters: '%' and '_'.  If either of these
     * characters need to be matched literally, then they must be escaped like so:
     *
     * WHERE value LIKE 'android\_%' ESCAPE '\'
     *
     * The ESCAPE clause is required and no default exists as the escape character in this context.
     * Since the escape character needs to be defined as part of the sql string, it must be
     * provided to this method so the escape characters match.
     *
     * @param sb The StringBuilder to append the escaped value to.
     * @param value The value to be escaped.
     * @param escapeChar The escape character to be defined in the sql ESCAPE clause.
     */
    public static void escapeLikeValue(StringBuilder sb, String value, char escapeChar) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '%' || ch == '_') {
                sb.append(escapeChar);
            }
            sb.append(ch);
        }
    }

}

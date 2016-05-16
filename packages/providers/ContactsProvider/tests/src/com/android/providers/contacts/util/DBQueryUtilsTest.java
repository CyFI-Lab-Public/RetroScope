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

import static com.android.providers.contacts.util.DbQueryUtils.checkForSupportedColumns;
import static com.android.providers.contacts.util.DbQueryUtils.concatenateClauses;
import static com.android.providers.contacts.util.DbQueryUtils.escapeLikeValue;

import android.content.ContentValues;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.common.content.ProjectionMap;
import com.android.providers.contacts.EvenMoreAsserts;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link DbQueryUtils} class.
 * Run the test like this:
 * <code>
 * runtest -c com.android.providers.contacts.util.DBQueryUtilsTest contactsprov
 * </code>
 */
@SmallTest
public class DBQueryUtilsTest extends TestCase {
    public void testGetEqualityClause() {
        assertEquals("(foo = 'bar')", DbQueryUtils.getEqualityClause("foo", "bar"));
        assertEquals("(foo = 2)", DbQueryUtils.getEqualityClause("foo", 2));
    }

    public void testGetInEqualityClause() {
        assertEquals("(foo != 2)", DbQueryUtils.getInequalityClause("foo", 2));
    }

    public void testConcatenateClauses() {
        assertEquals("(first)", concatenateClauses("first"));
        assertEquals("(first) AND (second)", concatenateClauses("first", "second"));
        assertEquals("(second)", concatenateClauses("second", null));
        assertEquals("(second)", concatenateClauses(null, "second"));
        assertEquals("(second)", concatenateClauses(null, "second", null));
        assertEquals("(a) AND (b) AND (c)", concatenateClauses(null, "a", "b", null, "c"));
        assertEquals("(WHERE \"a\" = \"b\")", concatenateClauses(null, "WHERE \"a\" = \"b\""));
    }

    public void testCheckForSupportedColumns() {
        final ProjectionMap projectionMap = new ProjectionMap.Builder()
                .add("A").add("B").add("C").build();
        final ContentValues values = new ContentValues();
        values.put("A", "?");
        values.put("C", "?");
        // No exception expected.
        checkForSupportedColumns(projectionMap, values);
        // Expect exception for invalid column.
        EvenMoreAsserts.assertThrows(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                values.put("D", "?");
                checkForSupportedColumns(projectionMap, values);
            }
        });
    }

    public void testEscapeLikeValuesEscapesUnderscores() {
        StringBuilder sb = new StringBuilder();
        DbQueryUtils.escapeLikeValue(sb, "my_test_string", '\\');
        assertEquals("my\\_test\\_string", sb.toString());

        sb = new StringBuilder();
        DbQueryUtils.escapeLikeValue(sb, "_test_", '\\');
        assertEquals("\\_test\\_", sb.toString());
    }

    public void testEscapeLikeValuesEscapesPercents() {
        StringBuilder sb = new StringBuilder();
        escapeLikeValue(sb, "my%test%string", '\\');
        assertEquals("my\\%test\\%string", sb.toString());

        sb = new StringBuilder();
        escapeLikeValue(sb, "%test%", '\\');
        assertEquals("\\%test\\%", sb.toString());
    }

    public void testEscapeLikeValuesNoChanges() {
        StringBuilder sb = new StringBuilder();
        escapeLikeValue(sb, "my test string", '\\');
        assertEquals("my test string", sb.toString());
    }
}

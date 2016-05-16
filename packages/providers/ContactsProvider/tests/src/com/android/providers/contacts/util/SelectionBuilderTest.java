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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Unit tests for {@link SelectionBuilder}.
 */
@SmallTest
public class SelectionBuilderTest extends AndroidTestCase {
    public void testEmptyClauses() {
        assertEquals(null, new SelectionBuilder(null).build());
        assertEquals(null, new SelectionBuilder("").build());
        assertEquals(null, new SelectionBuilder("").addClause(null).build());
        assertEquals(null, new SelectionBuilder(null).addClause("").build());
        assertEquals(null, new SelectionBuilder(null).addClause("").addClause(null).build());
    }

    public void testNonEmptyClauses() {
        assertEquals("(A)", new SelectionBuilder("A").build());
        assertEquals("(A) AND (B=bar) AND (C='1')", new SelectionBuilder("A")
                .addClause("B=bar")
                .addClause("C='1'")
                .build());

        // Skips null and empty clauses.
        assertEquals("(A) AND (B) AND (C)", new SelectionBuilder("A")
                .addClause("")
                .addClause("B")
                .addClause(null)
                .addClause("C")
                .addClause(null)
                .build());

        // Use base selection with constructor.
        assertEquals("(A)", new SelectionBuilder(null).addClause("A").build());
        assertEquals("(A)", new SelectionBuilder("").addClause("A").build());
        assertEquals("(A) AND (B)", new SelectionBuilder("A").addClause("B").build());
    }
}

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

import static com.google.common.base.Objects.equal;

import com.google.common.collect.UnmodifiableIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.Assert;

/**
 * Test utilities for {@link ShortcutCursor}.
 */
public class SuggestionCursorUtil extends Assert {

    public static void assertNoSuggestions(SuggestionCursor suggestions) {
        assertNoSuggestions("", suggestions);
    }

    public static void assertNoSuggestions(String message, SuggestionCursor suggestions) {
        assertNotNull(suggestions);
        assertEquals(message, 0, suggestions.getCount());
    }

    public static void assertSameSuggestion(String message, int position,
            SuggestionCursor expected, SuggestionCursor observed) {
        assertSameSuggestion(message, expected, position, observed, position);
    }

    public static void assertSameSuggestion(String message,
            SuggestionCursor expected, int positionExpected,
            SuggestionCursor observed, int positionObserved) {
        message +=  " at positions " + positionExpected + "(expected) "
                + positionObserved + " (observed)";
        expected.moveTo(positionExpected);
        observed.moveTo(positionObserved);
        assertSuggestionEquals(message, expected, observed);
    }

    public static void assertSameSuggestions(SuggestionCursor expected, SuggestionCursor observed) {
        assertSameSuggestions("", expected, observed);
    }

    public static void assertSameSuggestions(
            String message, SuggestionCursor expected, SuggestionCursor observed) {
        assertNotNull(message + ", expected == null", expected);
        assertNotNull(message + ", observed == null", observed);
        assertEquals(message + ", count", expected.getCount(), observed.getCount());
        assertEquals(message + ", userQuery", expected.getUserQuery(), observed.getUserQuery());
        int count = expected.getCount();
        for (int i = 0; i < count; i++) {
            assertSameSuggestion(message, i, expected, observed);
        }
    }

    public static void assertSameSuggestionsNoOrder(SuggestionCursor expected,
            SuggestionCursor observed) {
        assertSameSuggestionsNoOrder("", expected, observed);
    }

    public static void assertSameSuggestionsNoOrder(String message,
            SuggestionCursor expected, SuggestionCursor observed) {
        for (Suggestion expectedSuggestion : iterable(expected)) {
            assertContainsSuggestion(expectedSuggestion, observed);
        }
        for (Suggestion observedSuggestion : iterable(observed)) {
            assertContainsSuggestion(observedSuggestion, expected);
        }
    }

    public static void assertContainsSuggestion(Suggestion expected, SuggestionCursor observed) {
        for (Suggestion observedSuggestion : iterable(observed)) {
            if (checkSuggestionEquals(expected, observedSuggestion)) {
                return;
            }
        }
        fail(expected + " not found in " + observed);
    }

    public static Iterable<Suggestion> iterable(final SuggestionCursor cursor) {
        return new Iterable<Suggestion>() {
            @Override
            public Iterator<Suggestion> iterator() {
                return SuggestionCursorUtil.iterator(cursor);
            }
        };
    }

    public static UnmodifiableIterator<Suggestion> iterator(final SuggestionCursor cursor) {
        return new UnmodifiableIterator<Suggestion>() {
            private int mPos = 0;

            @Override
            public boolean hasNext() {
                return cursor.getPosition() < cursor.getCount() - 1;
            }
            @Override
            public Suggestion next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                mPos++;
                return new SuggestionPosition(cursor, mPos);
            }
        };
    }

    public static ListSuggestionCursor slice(SuggestionCursor cursor, int start) {
        return slice(cursor, start, cursor.getCount() - start);
    }

    public static ListSuggestionCursor slice(SuggestionCursor cursor, int start, int length) {
        ListSuggestionCursor out = new ListSuggestionCursor(cursor.getUserQuery());
        for (int i = start; i < start + length; i++) {
            out.add(new SuggestionPosition(cursor, i));
        }
        return out;
    }

    public static ListSuggestionCursor concat(SuggestionCursor... cursors) {
        ListSuggestionCursor out = new ListSuggestionCursor(cursors[0].getUserQuery());
        for (SuggestionCursor cursor : cursors) {
            for (int i = 0; i < cursor.getCount(); i++) {
                out.add(new SuggestionPosition(cursor, i));
            }
        }
        return out;
    }

    public static void assertSuggestionEquals(Suggestion expected, Suggestion observed) {
        assertSuggestionEquals(null, expected, observed);
    }

    public static void assertSuggestionEquals(String message, Suggestion expected,
            Suggestion observed) {
        assertFieldEquals(message, "source", expected.getSuggestionSource(),
                observed.getSuggestionSource());
        assertFieldEquals(message, "shortcutId", expected.getShortcutId(),
                observed.getShortcutId());
        assertFieldEquals(message, "spinnerWhileRefreshing", expected.isSpinnerWhileRefreshing(),
                observed.isSpinnerWhileRefreshing());
        assertFieldEquals(message, "format", expected.getSuggestionFormat(),
                observed.getSuggestionFormat());
        assertFieldEquals(message, "icon1", expected.getSuggestionIcon1(),
                observed.getSuggestionIcon1());
        assertFieldEquals(message, "icon2", expected.getSuggestionIcon2(),
                observed.getSuggestionIcon2());
        assertFieldEquals(message, "text1", expected.getSuggestionText1(),
                observed.getSuggestionText1());
        assertFieldEquals(message, "text2", expected.getSuggestionText2(),
                observed.getSuggestionText2());
        assertFieldEquals(message, "text2Url", expected.getSuggestionText2Url(),
                observed.getSuggestionText2Url());
        assertFieldEquals(message, "action", expected.getSuggestionIntentAction(),
                observed.getSuggestionIntentAction());
        assertFieldEquals(message, "data", expected.getSuggestionIntentDataString(),
                observed.getSuggestionIntentDataString());
        assertFieldEquals(message, "extraData", expected.getSuggestionIntentExtraData(),
                observed.getSuggestionIntentExtraData());
        assertFieldEquals(message, "query", expected.getSuggestionQuery(),
                observed.getSuggestionQuery());
        assertFieldEquals(message, "logType", expected.getSuggestionLogType(),
                observed.getSuggestionLogType());
    }

    private static void assertFieldEquals(String message, String field,
            Object expected, Object observed) {
        String msg = (message == null) ? field : message + ", " + field;
        assertEquals(msg, expected, observed);
    }

    public static void addAll(ListSuggestionCursor to, SuggestionCursor from) {
        if (from == null) return;
        int count = from.getCount();
        for (int i = 0; i < count; i++) {
            to.add(new SuggestionPosition(from, i));
        }
    }

    public static boolean checkSuggestionEquals(Suggestion expected, Suggestion observed) {
        return equal(expected.getSuggestionSource(), observed.getSuggestionSource())
                && equal(expected.getShortcutId(), observed.getShortcutId())
                && equal(expected.isSpinnerWhileRefreshing(), observed.isSpinnerWhileRefreshing())
                && equal(expected.getSuggestionFormat(), observed.getSuggestionFormat())
                && equal(expected.getSuggestionIcon1(), observed.getSuggestionIcon1())
                && equal(expected.getSuggestionIcon2(), observed.getSuggestionIcon2())
                && equal(expected.getSuggestionText1(), observed.getSuggestionText1())
                && equal(expected.getSuggestionText2(), observed.getSuggestionText2())
                && equal(expected.getSuggestionText2Url(), observed.getSuggestionText2Url())
                && equal(expected.getSuggestionIntentAction(), observed.getSuggestionIntentAction())
                && equal(expected.getSuggestionIntentDataString(), observed.getSuggestionIntentDataString())
                && equal(expected.getSuggestionIntentExtraData(), observed.getSuggestionIntentExtraData())
                && equal(expected.getSuggestionQuery(), observed.getSuggestionQuery())
                && equal(expected.getSuggestionLogType(), observed.getSuggestionLogType());
    }

    public static void assertSuggestionExtras(String message, SuggestionCursor observed,
            String extraColumn, Object expectedExtra) {
        assertNotNull(message + ", observed == null", observed);
        assertTrue(message + ", no suggestions", observed.getCount() > 0);
        for (int i = 0; i < observed.getCount(); ++i) {
            observed.moveTo(i);
            SuggestionExtras extras = observed.getExtras();
            assertNotNull(message + ", no extras at position " + i, extras);
            Collection<String> columns = extras.getExtraColumnNames();
            assertNotNull(message + ", extras columns is null at position " + i, columns);
            assertTrue(message + ", column '" + extraColumn +
                    "' not reported by extras at position " + i, columns.contains(extraColumn));
            Object extra = extras.getExtra(extraColumn);
            assertEquals(message + ", extra value", expectedExtra == null ? null :
                    expectedExtra.toString(), extra);
        }
    }

}

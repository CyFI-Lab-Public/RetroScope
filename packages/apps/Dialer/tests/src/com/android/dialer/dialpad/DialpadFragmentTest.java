/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.dialer.dialpad;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

/** Unit tests for {@link DialpadFragment}. */
@SmallTest
public class DialpadFragmentTest extends TestCase {

    public void testCanAddDigit_Valid() {
        // end, middle, selection to end, middle selection
        assertTrue(DialpadFragment.canAddDigit("123", 3, 3, ';'));
        assertTrue(DialpadFragment.canAddDigit("123", 1, 1, ','));
        assertTrue(DialpadFragment.canAddDigit("123", 1, 3, ';'));
        assertTrue(DialpadFragment.canAddDigit("123", 1, 2, ','));
    }

    public void testCanAddDigit_InvalidCharacter() {
        // only handles wait/pause
        assertFalse(DialpadFragment.canAddDigit("123", 1, 1, '5'));
    }

    public void testCanAddDigit_BadOrNoSelection() {
        // no selection
        assertFalse(DialpadFragment.canAddDigit("123", -1, -1, ';'));
        assertFalse(DialpadFragment.canAddDigit("123", -1, 1, ','));

        // start > end
        assertFalse(DialpadFragment.canAddDigit("123", 2, 1, ','));
    }

    public void testCanAddDigit_OutOfBounds() {
        // start or end is > digits.length()
        assertFalse(DialpadFragment.canAddDigit("123", 1, 4, ';'));
        assertFalse(DialpadFragment.canAddDigit("123", 4, 4, ','));
    }

    public void testCanAddDigit_AsFirstCharacter() {
        assertFalse(DialpadFragment.canAddDigit("", 0, 0, ','));
        assertFalse(DialpadFragment.canAddDigit("123", 0, 0, ';'));
        assertFalse(DialpadFragment.canAddDigit("123", 0, 2, ','));
        assertFalse(DialpadFragment.canAddDigit("123", 0, 3, ','));
    }

    public void testCanAddDigit_AdjacentCharacters_Before() {
        // before
        assertFalse(DialpadFragment.canAddDigit("55;55", 2, 2, ';')); // WAIT
        assertFalse(DialpadFragment.canAddDigit("55;55", 1, 2, ';'));
        assertTrue(DialpadFragment.canAddDigit("55,55", 2, 2, ',')); // PAUSE
        assertTrue(DialpadFragment.canAddDigit("55,55", 1, 2, ','));
        assertTrue(DialpadFragment.canAddDigit("55;55", 2, 2, ',')); // WAIT & PAUSE
        assertTrue(DialpadFragment.canAddDigit("55,55", 1, 2, ';'));
    }

    public void testCanAddDigit_AdjacentCharacters_After() {
        // after
        assertFalse(DialpadFragment.canAddDigit("55;55", 3, 3, ';')); // WAIT
        assertFalse(DialpadFragment.canAddDigit("55;55", 3, 4, ';'));
        assertTrue(DialpadFragment.canAddDigit("55,55", 3, 3, ',')); // PAUSE
        assertTrue(DialpadFragment.canAddDigit("55,55", 3, 4, ','));
        assertTrue(DialpadFragment.canAddDigit("55;55", 3, 3, ',')); // WAIT & PAUSE
        assertTrue(DialpadFragment.canAddDigit("55,55", 3, 4, ';'));
    }
}

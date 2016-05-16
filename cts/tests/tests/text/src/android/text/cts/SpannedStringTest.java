/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.text.cts;

import android.test.AndroidTestCase;
import android.text.SpannedString;

public class SpannedStringTest extends AndroidTestCase {
    public void testConstructor() {
        new SpannedString("test");

        try {
            new SpannedString(null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
        }
    }

    public void testValueOf() {
        String text = "test valueOf";
        SpannedString spanned = SpannedString.valueOf(text);
        assertEquals(text, spanned.toString());

        spanned = new SpannedString(text);
        assertSame(spanned, SpannedString.valueOf(spanned));

        try {
            SpannedString.valueOf(null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
        }
    }

    public void testSubSequence() {
        String text = "hello, world";
        SpannedString spanned = new SpannedString(text);

        CharSequence subSequence = spanned.subSequence(0, 2);
        assertTrue(subSequence instanceof SpannedString);
        assertEquals("he", subSequence.toString());

        subSequence = spanned.subSequence(0, text.length());
        assertTrue(subSequence instanceof SpannedString);
        assertEquals(text, subSequence.toString());

        try {
            spanned.subSequence(-1, text.length() + 1);
            fail("subSequence failed when index is out of bounds");
        } catch (StringIndexOutOfBoundsException e) {
        }

        try {
            spanned.subSequence(2, 0);
            fail("subSequence failed on invalid index");
        } catch (StringIndexOutOfBoundsException e) {
        }
    }
}

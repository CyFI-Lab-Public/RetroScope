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
import android.text.AndroidCharacter;

public class AndroidCharacterTest extends AndroidTestCase {

    public void testConstructor() {
        new AndroidCharacter();
    }

    public void testGetDirectionalities() {
        char[] src = new char[128];
        for (int i = 0; i < src.length; i++) {
            src[i] = (char) i;
        }
        byte[] dest = new byte[128];
        int count = 128;
        AndroidCharacter.getDirectionalities(src, dest, count);
        byte[] expected = {9, 9, 9, 9, 9, 9, 9, 9, 9, 11, 10, 11, 12, 10, 9,
                9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 11, 12, 13,
                13, 5, 5, 5, 13, 13, 13, 13, 13, 4, 7, 4, 7, 7, 3, 3, 3, 3, 3,
                3, 3, 3, 3, 3, 7, 13, 13, 13, 13, 13, 13, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13,
                13, 13, 13, 13, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 13, 13, 13, 9};
        for (int i = 0; i < dest.length; i++) {
            assertEquals(expected[i], dest[i]);
        }
    }

    public void testGetEastAsianWidth() {
        // LATIN CAPITAL LETTER U WITH CARON (U+01D3)
        assertEquals(AndroidCharacter.EAST_ASIAN_WIDTH_NEUTRAL,
                AndroidCharacter.getEastAsianWidth((char)0x01D3));

        // REPLACEMENT CHARACTER (U+FFFD)
        assertEquals(AndroidCharacter.EAST_ASIAN_WIDTH_AMBIGUOUS,
                AndroidCharacter.getEastAsianWidth((char)0xFFFD));

        // HALFWIDTH KATAKANA LETTER NI (U+FF86)
        assertEquals(AndroidCharacter.EAST_ASIAN_WIDTH_HALF_WIDTH,
                AndroidCharacter.getEastAsianWidth((char)0xFF86));

        // FULLWIDTH LATIN SMALL LETTER A (U+FF41)
        assertEquals(AndroidCharacter.EAST_ASIAN_WIDTH_FULL_WIDTH,
                AndroidCharacter.getEastAsianWidth((char)0xFF41));

        // LATIN CAPITAL LETTER A (U+0041)
        assertEquals(AndroidCharacter.EAST_ASIAN_WIDTH_NARROW,
                AndroidCharacter.getEastAsianWidth((char)0x0041));

        // IDEOGRAPHIC ANNOTATION MAN MARK (U+319F)
        assertEquals(AndroidCharacter.EAST_ASIAN_WIDTH_WIDE,
                AndroidCharacter.getEastAsianWidth((char)0x319F));
    }

    public void testGetEastAsianWidths() {
        char[] src = {
                0x01D3, 0xFFFD, 0xFF86, 0xFF41, 0x0041, 0x319f,
                0x319F, 0x0041, 0xFF41, 0xFF86, 0xFFFD, 0x01D3,
        };
        int start = 2;
        int count = 8;
        byte[] dest = new byte[count];
        AndroidCharacter.getEastAsianWidths(src, start, count, dest);
        byte[] expected = {2, 3, 4, 5, 5, 4, 3, 2};
        for (int i = 0; i < dest.length; i++) {
            assertEquals(expected[i], dest[i]);
        }
        try {
            AndroidCharacter.getEastAsianWidths(src, 24, 8, dest);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
        try {
            AndroidCharacter.getEastAsianWidths(src, -1024, 1, dest);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
        try {
            AndroidCharacter.getEastAsianWidths(src, 0, -1, dest);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
    }

    public void testGetMirror() {
        assertEquals('A', AndroidCharacter.getMirror('A'));
        assertEquals('B', AndroidCharacter.getMirror('B'));
        assertEquals('(', AndroidCharacter.getMirror(')'));
        assertEquals('[', AndroidCharacter.getMirror(']'));
        assertEquals('{', AndroidCharacter.getMirror('}'));
        assertEquals('<', AndroidCharacter.getMirror('>'));
    }

    public void testMirror() {
        char[] src = new char[64];
        for (int i = 0; i < src.length; i++) {
            src[i] = (char) i;
        }

        assertFalse(AndroidCharacter.mirror(src, 0, 0));
        assertTrue(AndroidCharacter.mirror(src, 40, 24));
        try {
            AndroidCharacter.mirror(src, 65, 1);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
        try {
            AndroidCharacter.mirror(src, 60, 10);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
        try {
            AndroidCharacter.mirror(src, -1024, 1);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
        try {
            AndroidCharacter.mirror(src, 0, -1);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
        String str = new String("if(a>b)");
        char[] strChar = str.toCharArray();
        assertTrue(AndroidCharacter.mirror(strChar, 0, str.length()));
        assertEquals("if)a<b(", new String(strChar));
        assertFalse(AndroidCharacter.mirror(str.toCharArray(), 0, 2));
    }
}


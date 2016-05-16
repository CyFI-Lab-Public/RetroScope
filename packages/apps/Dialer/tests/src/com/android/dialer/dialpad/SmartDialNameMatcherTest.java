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
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;
import android.test.AndroidTestCase;

import com.android.dialer.dialpad.SmartDialNameMatcher;
import com.android.dialer.dialpad.SmartDialPrefix;

import java.text.Normalizer;
import java.util.ArrayList;

import junit.framework.TestCase;

@SmallTest
public class SmartDialNameMatcherTest extends TestCase {
    private static final String TAG = "SmartDialNameMatcherTest";

    public void testMatches() {
        // Test to ensure that all alphabetic characters are covered
        checkMatches("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
                "22233344455566677778889999" + "22233344455566677778889999", true, 0, 26 * 2);
        // Should fail because of a mistyped 2 instead of 9 in the second last character
        checkMatches("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
                "22233344455566677778889999" + "22233344455566677778889929", false, 0, 0);

        // Basic name test
        checkMatches("joe", "5", true, 0, 1);
        checkMatches("joe", "56", true, 0, 2);
        checkMatches("joe", "563", true, 0, 3);

        // Matches only word boundary.
        checkMatches("joe", "63", false, 0, 0);
        checkMatches("joe oe", "63", true, 4, 6);

        // Test for a match across word boundaries
        checkMatches("joe oe", "56363", true, 0, 6);
    }

    public void testMatches_repeatedLetters() {
        checkMatches("aaaaaaaaaa", "2222222222", true, 0, 10);
        // Fails because of one extra 2
        checkMatches("aaaaaaaaaa", "22222222222", false, 0, 0);
        checkMatches("zzzzzzzzzz zzzzzzzzzz", "99999999999999999999", true, 0, 21);
    }

    public void testMatches_repeatedSpaces() {
        checkMatches("William     J  Smith", "9455426576", true, 0, 17);
        checkMatches("William     J  Smith", "576", true, 12, 17);
        // Fails because we start at non-word boundary
        checkMatches("William     J  Smith", "6576", false, 0, 0);
    }


    public void testMatches_Initial() {
        // wjs matches (W)illiam (J)ohn (S)mith
        checkMatches("William John Smith", "957", true, 0, 1, 8, 9, 13, 14);
        // wjsmit matches (W)illiam (J)ohn (Smit)h
        checkMatches("William John Smith", "957648", true, 0, 1, 8, 9, 13, 17);
        // wjohn matches (W)illiam (John) Smith
        checkMatches("William John Smith", "95646", true, 0, 1, 8, 12);
        // jsmi matches William (J)ohn (Smi)th
        checkMatches("William John Smith", "5764", true, 8, 9, 13, 16);
        // make sure multiple spaces don't mess things up
        checkMatches("William        John   Smith", "5764", true, 15, 16, 22, 25);
    }

    public void testMatches_InitialWithSeparator() {
        // wjs matches (W)illiam (J)ohn (S)mith
        checkMatches("William John-Smith", "957", true, 0, 1, 8, 9, 13, 14);
        // wjsmit matches (W)illiam (J)ohn-(OShe)a
        checkMatches("William John-O'Shea", "956743", true, 0, 1, 8, 9, 13, 18);
        // wjohn matches (W)illiam-(John) Smith
        checkMatches("William-John Smith", "95646", true, 0, 1, 8, 12);
        // jsmi matches William (J)ohn-(Smi)th
        checkMatches("William John-Smith", "5764", true, 8, 9, 13, 16);
        // wsmi matches (W)illiam John (Smi)th
        checkMatches("William John-Smith", "9764", true, 0, 1, 13, 16);
        // make sure multiple spaces don't mess things up
        checkMatches("William        John---Smith", "5764", true, 15, 16, 22, 25);
        // match tokens that are located directly after a non-space separator (studio)
        checkMatches("Berkeley Hair-Studio", "788346", true, 14, 20);
        // match tokens with same initials
        checkMatches("H.Harold", "427653", true, 2, 8);
        // various matching combinations of tokens with similar initials
        checkMatches("Yo-Yoghurt Land", "964487", true, 3, 9);
        checkMatches("Yo-Yoghurt Land", "96448785263", true, 3, 15);
        checkMatches("Yo-Yoghurt Land", "95263", true, 3, 4, 11, 15);
        checkMatches("Yo-Yoghurt Land", "995263", true, 0, 1, 3, 4, 11, 15);

        checkMatches("ab zz ef", "23", true, 0, 1, 6, 7);
    }

    public void testMatches_repeatedSeparators() {
        // Simple match for single token
        checkMatches("John,,,,,Doe", "5646", true, 0, 4);
        // Match across tokens
        checkMatches("John,,,,,Doe", "56463", true, 0, 10);
        // Match token after chain of separators
        checkMatches("John,,,,,Doe", "363", true, 9, 12);
    }

    public void testMatches_LatinMix() {
        // Latin + Chinese characters
        checkMatches("Lee王力Wang宏", "59264", true, 0, 1, 5, 9);
        // Latin + Japanese characters
        checkMatches("千Abcd佳智Efgh佳IJKL", "222333444555", true, 1, 16);
        // Latin + Arabic characters
        checkMatches("Peterعبد الرحمنJames", "752637", true, 0, 1, 15, 20);
    }

    public void testMatches_umlaut() {
        checkMatches("ÄÖÜäöü", "268268", true, 0, 6);
    }

    public void testMatches_NumberInName() {
        // Number used as display name
        checkMatches("+1-123-456-6789", "1234566789", true, 3, 15);
        // Mix of numbers and letters
        checkMatches("3rd Grade Teacher", "373", true, 0, 3);
        checkMatches("1800 Win A Prize", "1800", true, 0, 4);
        checkMatches("1800 Win A Prize", "1800946277493", true, 0, 16);
        checkMatches("1800 Win A Prize", "977493", true, 5, 6, 11, 16);
    }


    // TODO: Great if it was treated as "s" or "ss. Figure out if possible without prefix trie?
    @Suppress
    public void testMatches_germanSharpS() {
        checkMatches("ß", "s", true, 0, 1);
        checkMatches("ß", "ss", true, 0, 1);
    }

    // TODO: Add this and make it work
    @Suppress
    public void testMatches_greek() {
        // http://en.wikipedia.org/wiki/Greek_alphabet
        fail("Greek letters aren't supported yet.");
    }

    // TODO: Add this and make it work
    @Suppress
    public void testMatches_cyrillic() {
        // http://en.wikipedia.org/wiki/Cyrillic_script
        fail("Cyrillic letters aren't supported yet.");
    }


    public void testMatches_NumberBasic() {
        // Simple basic examples that start the match from the start of the number
        checkMatchesNumber("5103337596", "510", true, 0, 3);
        checkMatchesNumber("5103337596", "511", false, 0, 0);
        checkMatchesNumber("5103337596", "5103337596", true, 0, 10);
        checkMatchesNumber("123-456-789", "123456789", true, 0, 11);
        checkMatchesNumber("123-456-789", "123456788", false, 0, 0);
        checkMatchesNumber("09999999999", "099", true, 0, 3);
    }

    public void testMatches_NumberWithCountryCode() {
        // These matches should ignore the country prefix
        // USA (+1)
        checkMatchesNumber("+15103337596", "5103337596", true, 2, 12);
        checkMatchesNumber("+15103337596", "15103337596", true, 0, 12);

        // Singapore (+65)
        checkMatchesNumber("+6591776930", "6591", true, 0, 5);
        checkMatchesNumber("+6591776930", "9177", true, 3, 7);
        checkMatchesNumber("+6591776930", "5917", false, 3, 7);

        // Hungary (+36)
        checkMatchesNumber("+3612345678", "361234", true, 0, 7);
        checkMatchesNumber("+3612345678", "1234", true, 3, 7);

        // Hongkong (+852)
        checkMatchesNumber("+852 2222 2222", "85222222222", true, 0, 14);
        checkMatchesNumber("+852 2222 3333", "2222", true, 5, 9);

        // Invalid (+854)
        checkMatchesNumber("+854 1111 2222", "8541111", true, 0, 9);
        checkMatchesNumber("+854 1111 2222", "1111", false, 0, 0);
    }

    public void testMatches_NumberNANP() {
        // An 11 digit number prefixed with 1 should be matched by the 10 digit number, as well as
        // the 7 digit number (without area code)
        checkMatchesNumber("1-510-333-7596", "5103337596", true, true, 2, 14);
        checkMatchesNumber("1-510-333-7596", "3337596", true, true, 6, 14);

        // An 11 digit number prefixed with +1 should be matched by the 10 digit number, as well as
        // the 7 digit number (without area code)
        checkMatchesNumber("+1-510-333-7596", "5103337596", true, true, 3, 15);
        checkMatchesNumber("+1-510-333-7596", "3337596", true, true, 7, 15);
        checkMatchesNumber("+1-510-333-7596", "103337596", false, true, 0, 0);
        checkMatchesNumber("+1-510-333-7596", "337596", false, true, 0, 0);
        checkMatchesNumber("+1510 3337596", "5103337596", true, true, 2, 13);
        checkMatchesNumber("+1510 3337596", "3337596", true, true, 6, 13);
        checkMatchesNumber("+1510 3337596", "103337596", false, true, 0, 0);
        checkMatchesNumber("+1510 3337596", "37596", false, true, 0, 0);

        // Invalid NANP numbers should not be matched
        checkMatchesNumber("1-510-333-759", "510333759", false, true, 0, 0);
        checkMatchesNumber("510-333-759", "333759", false, true, 0, 0);

        // match should fail if NANP flag is switched off
        checkMatchesNumber("1-510-333-7596", "3337596", false, false, 0, 0);

        // A 10 digit number without a 1 prefix should be matched by the 7 digit number
        checkMatchesNumber("(650) 292 2323", "2922323", true, true, 6, 14);
        checkMatchesNumber("(650) 292 2323", "6502922323", true, true, 0, 14);
        // match should fail if NANP flag is switched off
        checkMatchesNumber("(650) 292 2323", "2922323", false, false, 0, 0);
        // But this should still match (since it is the full number)
        checkMatchesNumber("(650) 292 2323", "6502922323", true, false, 0, 14);
    }


    private void checkMatchesNumber(String number, String query, boolean expectedMatches,
            int matchStart, int matchEnd) {
        checkMatchesNumber(number, query, expectedMatches, false, matchStart, matchEnd);
    }

    private void checkMatchesNumber(String number, String query, boolean expectedMatches,
            boolean matchNanp, int matchStart, int matchEnd) {
        final SmartDialNameMatcher matcher = new SmartDialNameMatcher(query);
        final SmartDialMatchPosition pos = matcher.matchesNumber(number, query, matchNanp);
        assertEquals(expectedMatches, pos != null);
        if (expectedMatches) {
            assertEquals("start", matchStart, pos.start);
            assertEquals("end", matchEnd, pos.end);
        }
    }

    private void checkMatches(String displayName, String query, boolean expectedMatches,
            int... expectedMatchPositions) {
        final SmartDialNameMatcher matcher = new SmartDialNameMatcher(query);
        final ArrayList<SmartDialMatchPosition> matchPositions =
                new ArrayList<SmartDialMatchPosition>();
        final boolean matches = matcher.matchesCombination(
                displayName, query, matchPositions);
        Log.d(TAG, "query=" + query + "  text=" + displayName
                + "  nfd=" + Normalizer.normalize(displayName, Normalizer.Form.NFD)
                + "  nfc=" + Normalizer.normalize(displayName, Normalizer.Form.NFC)
                + "  nfkd=" + Normalizer.normalize(displayName, Normalizer.Form.NFKD)
                + "  nfkc=" + Normalizer.normalize(displayName, Normalizer.Form.NFKC)
                + "  matches=" + matches);
        assertEquals("matches", expectedMatches, matches);
        final int length = expectedMatchPositions.length;
        assertEquals(length % 2, 0);
        if (matches) {
            for (int i = 0; i < length/2; i++) {
                assertEquals("start", expectedMatchPositions[i * 2], matchPositions.get(i).start);
                assertEquals("end", expectedMatchPositions[i * 2 + 1], matchPositions.get(i).end);
            }
        }
    }

}

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
 * limitations under the License.
 */

package android.text.method.cts;

import android.text.method.WordIterator;

import java.text.BreakIterator;

import junit.framework.TestCase;

public class WordIteratorTest extends TestCase {

    WordIterator wi = new WordIterator();

    private void checkIsWordWithSurrogate(int beginning, int end, int surrogateIndex) {
        for (int i = beginning; i <= end; i++) {
            if (i == surrogateIndex) continue;
            assertEquals(beginning, wi.getBeginning(i));
            assertEquals(end, wi.getEnd(i));
        }
    }

    private void setCharSequence(String string) {
        wi.setCharSequence(string, 0, string.length());
    }

    private void checkIsWord(int beginning, int end) {
        checkIsWordWithSurrogate(beginning, end, -1);
    }

    private void checkIsNotWord(int beginning, int end) {
        for (int i = beginning; i <= end; i++) {
            assertEquals(BreakIterator.DONE, wi.getBeginning(i));
            assertEquals(BreakIterator.DONE, wi.getEnd(i));
        }
    }

    public void testEmptyString() {
        setCharSequence("");
        assertEquals(BreakIterator.DONE, wi.following(0));
        assertEquals(BreakIterator.DONE, wi.preceding(0));

        assertEquals(BreakIterator.DONE, wi.getBeginning(0));
        assertEquals(BreakIterator.DONE, wi.getEnd(0));
    }

    public void testOneWord() {
        setCharSequence("I");
        checkIsWord(0, 1);

        setCharSequence("am");
        checkIsWord(0, 2);

        setCharSequence("zen");
        checkIsWord(0, 3);
    }

    public void testSpacesOnly() {
        setCharSequence(" ");
        checkIsNotWord(0, 1);

        setCharSequence(", ");
        checkIsNotWord(0, 2);

        setCharSequence(":-)");
        checkIsNotWord(0, 3);
    }

    public void testBeginningEnd() {
        setCharSequence("Well hello,   there! ");
        //                  0123456789012345678901
        checkIsWord(0, 4);
        checkIsWord(5, 10);
        checkIsNotWord(11, 13);
        checkIsWord(14, 19);
        checkIsNotWord(20, 21);

        setCharSequence("  Another - sentence");
        //                  012345678901234567890
        checkIsNotWord(0, 1);
        checkIsWord(2, 9);
        checkIsNotWord(10, 11);
        checkIsWord(12, 20);

        setCharSequence("This is \u0644\u0627 tested"); // Lama-aleph
        //                  012345678     9     01234567
        checkIsWord(0, 4);
        checkIsWord(5, 7);
        checkIsWord(8, 10);
        checkIsWord(11, 17);
    }

    public void testSurrogate() {
        final String BAIRKAN = "\uD800\uDF31";

        setCharSequence("one we" + BAIRKAN + "ird word");
        //                  012345    67         890123456

        checkIsWord(0, 3);
        // Skip index 7 (there is no point in starting between the two surrogate characters)
        checkIsWordWithSurrogate(4, 11, 7);
        checkIsWord(12, 16);

        setCharSequence("one " + BAIRKAN + "xxx word");
        //                  0123    45         678901234

        checkIsWord(0, 3);
        checkIsWordWithSurrogate(4, 9, 5);
        checkIsWord(10, 14);

        setCharSequence("one xxx" + BAIRKAN + " word");
        //                  0123456    78         901234

        checkIsWord(0, 3);
        checkIsWordWithSurrogate(4, 9, 8);
        checkIsWord(10, 14);
    }
}

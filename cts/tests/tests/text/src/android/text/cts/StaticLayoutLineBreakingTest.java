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

package android.text.cts;

import android.test.AndroidTestCase;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.text.style.MetricAffectingSpan;
import android.util.Log;

public class StaticLayoutLineBreakingTest extends AndroidTestCase {
    // Span test are currently not supported because text measurement uses the MeasuredText
    // internal mWorkPaint instead of the provided MockTestPaint.
    private static final boolean SPAN_TESTS_SUPPORTED = false;
    private static final boolean DEBUG = false;

    private static final float SPACE_MULTI = 1.0f;
    private static final float SPACE_ADD = 0.0f;
    private static final int WIDTH = 100;
    private static final Alignment ALIGN = Alignment.ALIGN_LEFT;

    final static char SURR_FIRST = '\uD800';
    final static char SURR_SECOND = '\uDF31';

    private static final int[] NO_BREAK = new int[] {};

    private static final TextPaint mTextPaint = new MockTextPaint();

    private static class MockTextPaint extends TextPaint {

        @Override
        public float getTextRunAdvances(char[] chars, int index, int count,
                int contextIndex, int contextCount, int flags, float[] advances,
                int advancesIndex) {

            // Conditions copy pasted from Paint
            if (chars == null) {
                throw new IllegalArgumentException("text cannot be null");
            }

            if (flags != DIRECTION_LTR && flags != DIRECTION_RTL) {
                throw new IllegalArgumentException("unknown flags value: " + flags);
            }

            if ((index | count | contextIndex | contextCount | advancesIndex
                    | (index - contextIndex) | (contextCount - count)
                    | ((contextIndex + contextCount) - (index + count))
                    | (chars.length - (contextIndex + contextCount))
                    | (advances == null ? 0 :
                        (advances.length - (advancesIndex + count)))) < 0) {
                throw new IndexOutOfBoundsException();
            }

            float res = 0.0f;

            if (advances != null) {
                for (int i = 0; i < count; i++) {
                    float width = getCharWidth(chars[index + i]);
                    advances[advancesIndex + i] = width;
                    res += width;
                }
            }

            return res;
        }
    }

    private static float getCharWidth(char c) {
        switch (Character.toUpperCase(c)) {
            // Roman figures
            case 'I': return 1.0f;
            case 'V': return 5.0f;
            case 'X': return 10.0f;
            case 'L': return 50.0f;
            case 'C': return 100.0f; // equals to WIDTH
            case ' ': return 10.0f;
            case '_': return 0.0f; // 0-width character
            case SURR_FIRST: return 7.0f;
            case SURR_SECOND: return 3.0f; // Sum of SURR_FIRST-SURR_SECOND is 10
            default: return 10.0f;
        }
    }

    private static StaticLayout getStaticLayout(CharSequence source, int width) {
        return new StaticLayout(source, mTextPaint, width, ALIGN, SPACE_MULTI, SPACE_ADD, false);
    }

    private static int[] getBreaks(CharSequence source) {
        return getBreaks(source, WIDTH);
    }

    private static int[] getBreaks(CharSequence source, int width) {
        StaticLayout staticLayout = getStaticLayout(source, width);

        int[] breaks = new int[staticLayout.getLineCount() - 1];
        for (int line = 0; line < breaks.length; line++) {
            breaks[line] = staticLayout.getLineEnd(line);
        }
        return breaks;
    }

    private static void debugLayout(CharSequence source, StaticLayout staticLayout) {
        if (DEBUG) {
            int count = staticLayout.getLineCount();
            Log.i("StaticLayoutLineBreakingTest", "\"" + source.toString() + "\": " +
                    count + " lines");
            for (int line = 0; line < count; line++) {
                int lineStart = staticLayout.getLineStart(line);
                int lineEnd = staticLayout.getLineEnd(line);
                Log.i("StaticLayoutLineBreakingTest", "Line " + line + " [" + lineStart + ".." +
                        lineEnd + "]\t" + source.subSequence(lineStart, lineEnd));
            }
        }
    }

    private static void layout(CharSequence source, int[] breaks) {
        layout(source, breaks, WIDTH);
    }

    private static void layout(CharSequence source, int[] breaks, int width) {
        StaticLayout staticLayout = getStaticLayout(source, width);

        debugLayout(source, staticLayout);

        int lineCount = breaks.length + 1;
        assertEquals("Number of lines", lineCount, staticLayout.getLineCount());

        for (int line = 0; line < lineCount; line++) {
            int lineStart = staticLayout.getLineStart(line);
            int lineEnd = staticLayout.getLineEnd(line);

            if (line == 0) {
                assertEquals("Line start for first line", 0, lineStart);
            } else {
                assertEquals("Line start for line " + line, breaks[line - 1], lineStart);
            }

            if (line == lineCount - 1) {
                assertEquals("Line end for last line", source.length(), lineEnd);
            } else {
                assertEquals("Line end for line " + line, breaks[line], lineEnd);
            }
        }
    }

    private static void layoutMaxLines(CharSequence source, int[] breaks, int maxLines) {
        StaticLayout staticLayout = new StaticLayout(source, 0, source.length(), mTextPaint, WIDTH,
                ALIGN, TextDirectionHeuristics.LTR, SPACE_MULTI, SPACE_ADD, false /* includePad */,
                null, WIDTH, maxLines);

        debugLayout(source, staticLayout);

        int lineCount = staticLayout.getLineCount();
        assertTrue("Number of lines", lineCount <= maxLines);

        for (int line = 0; line < lineCount; line++) {
            int lineStart = staticLayout.getLineStart(line);
            int lineEnd = staticLayout.getLineEnd(line);

            if (line == 0) {
                assertEquals("Line start for first line", 0, lineStart);
            } else {
                assertEquals("Line start for line " + line, breaks[line - 1], lineStart);
            }

            if (line == lineCount - 1 && line != breaks.length - 1) {
                assertEquals("Line end for last line", source.length(), lineEnd);
            } else {
                assertEquals("Line end for line " + line, breaks[line], lineEnd);
            }
        }
    }

    final static int MAX_SPAN_COUNT = 10;
    final static int[] spanStarts = new int[MAX_SPAN_COUNT];
    final static int[] spanEnds = new int[MAX_SPAN_COUNT];

    private static MetricAffectingSpan getMetricAffectingSpan() {
        return new MetricAffectingSpan() {
            @Override
            public void updateDrawState(TextPaint tp) { /* empty */ }

            @Override
            public void updateMeasureState(TextPaint p) { /* empty */ }
        };
    }

    /**
     * Replaces the "<...>" blocks by spans, assuming non overlapping, correctly defined spans
     * @param text
     * @return A CharSequence with '<' '>' replaced by MetricAffectingSpan
     */
    private static CharSequence spanify(String text) {
        int startIndex = text.indexOf('<');
        if (startIndex < 0) return text;

        int spanCount = 0;
        do {
            int endIndex = text.indexOf('>');
            if (endIndex < 0) throw new IllegalArgumentException("Unbalanced span markers");

            text = text.substring(0, startIndex) + text.substring(startIndex + 1, endIndex) +
                    text.substring(endIndex + 1);

            spanStarts[spanCount] = startIndex;
            spanEnds[spanCount] = endIndex - 2;
            spanCount++;

            startIndex = text.indexOf('<');
        } while (startIndex >= 0);

        SpannableStringBuilder result = new SpannableStringBuilder(text);
        for (int i = 0; i < spanCount; i++) {
            result.setSpan(getMetricAffectingSpan(), spanStarts[i], spanEnds[i],
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return result;
    }

    public void testNoLineBreak() {
        // Width lower than WIDTH
        layout("", NO_BREAK);
        layout("I", NO_BREAK);
        layout("V", NO_BREAK);
        layout("X", NO_BREAK);
        layout("L", NO_BREAK);
        layout("I VILI", NO_BREAK);
        layout("XXXX", NO_BREAK);
        layout("LXXXX", NO_BREAK);

        // Width equal to WIDTH
        layout("C", NO_BREAK);
        layout("LL", NO_BREAK);
        layout("L XXXX", NO_BREAK);
        layout("XXXXXXXXXX", NO_BREAK);
        layout("XXX XXXXXX", NO_BREAK);
        layout("XXX XXXX X", NO_BREAK);
        layout("XXX XXXXX ", NO_BREAK);
        layout(" XXXXXXXX ", NO_BREAK);
        layout("  XX  XXX ", NO_BREAK);
        //      0123456789

        // Width greater than WIDTH, but no break
        layout("  XX  XXX  ", NO_BREAK);
        layout("XX XXX XXX ", NO_BREAK);
        layout("XX XXX XXX     ", NO_BREAK);
        layout("XXXXXXXXXX     ", NO_BREAK);
        //      01234567890
    }

    public void testOneLineBreak() {
        //      01234567890
        layout("XX XXX XXXX", new int[] {7});
        layout("XX XXXX XXX", new int[] {8});
        layout("XX XXXXX XX", new int[] {9});
        layout("XX XXXXXX X", new int[] {10});
        //      01234567890
        layout("XXXXXXXXXXX", new int[] {10});
        layout("XXXXXXXXX X", new int[] {10});
        layout("XXXXXXXX XX", new int[] {9});
        layout("XXXXXXX XXX", new int[] {8});
        layout("XXXXXX XXXX", new int[] {7});
        //      01234567890
        layout("LL LL", new int[] {3});
        layout("LLLL", new int[] {2});
        layout("C C", new int[] {2});
        layout("CC", new int[] {1});
    }

    public void testSpaceAtBreak() {
        //      0123456789012
        layout("XXXX XXXXX X", new int[] {11});
        layout("XXXXXXXXXX X", new int[] {11});
        layout("XXXXXXXXXV X", new int[] {11});
        layout("C X", new int[] {2});
    }

    public void testMultipleSpacesAtBreak() {
        //      0123456789012
        layout("LXX XXXX", new int[] {4});
        layout("LXX  XXXX", new int[] {5});
        layout("LXX   XXXX", new int[] {6});
        layout("LXX    XXXX", new int[] {7});
        layout("LXX     XXXX", new int[] {8});
    }

    public void testZeroWidthCharacters() {
        //      0123456789012345678901234
        layout("X_X_X_X_X_X_X_X_X_X", NO_BREAK);
        layout("___X_X_X_X_X_X_X_X_X_X___", NO_BREAK);
        layout("C_X", new int[] {2});
        layout("C__X", new int[] {3});
    }

    /**
     * Note that when the text has spans, StaticLayout does not use the provided TextPaint to
     * measure text runs anymore. This is probably a bug.
     * To be able to use the fake mTextPaint and make this test pass, use mPaint instead of
     * mWorkPaint in MeasuredText#addStyleRun
     */
    public void testWithSpans() {
        if (!SPAN_TESTS_SUPPORTED) return;

        layout(spanify("<012 456 89>"), NO_BREAK);
        layout(spanify("012 <456> 89"), NO_BREAK);
        layout(spanify("<012> <456>< 89>"), NO_BREAK);
        layout(spanify("<012> <456> <89>"), NO_BREAK);

        layout(spanify("<012> <456> <89>012"), new int[] {8});
        layout(spanify("<012> <456> 89<012>"), new int[] {8});
        layout(spanify("<012> <456> <89><012>"), new int[] {8});
        layout(spanify("<012> <456> 89 <123>"), new int[] {11});
        layout(spanify("<012> <456> 89< 123>"), new int[] {11});
        layout(spanify("<012> <456> <89> <123>"), new int[] {11});
        layout(spanify("012 456 89 <LXX> XX XX"), new int[] {11, 18});
    }

    /*
     * Adding a span to the string should not change the layout, since the metrics are unchanged.
     */
    public void testWithOneSpan() {
        if (!SPAN_TESTS_SUPPORTED) return;

        String[] texts = new String[] { "0123", "012 456", "012 456 89 123", "012 45678 012",
                "012 456 89012 456 89012", "0123456789012" };

        MetricAffectingSpan metricAffectingSpan = getMetricAffectingSpan();

        for (String text : texts) {
            // Get the line breaks without any span
            int[] breaks = getBreaks(text);

            // Add spans on all possible offsets
            for (int spanStart = 0; spanStart < text.length(); spanStart++) {
                for (int spanEnd = spanStart; spanEnd < text.length(); spanEnd++) {
                    SpannableStringBuilder ssb = new SpannableStringBuilder(text);
                    ssb.setSpan(metricAffectingSpan, spanStart, spanEnd,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    layout(ssb, breaks);
                }
            }
        }
    }

    public void testWithTwoSpans() {
        if (!SPAN_TESTS_SUPPORTED) return;

        String[] texts = new String[] { "0123", "012 456", "012 456 89 123", "012 45678 012",
                "012 456 89012 456 89012", "0123456789012" };

        MetricAffectingSpan metricAffectingSpan1 = getMetricAffectingSpan();
        MetricAffectingSpan metricAffectingSpan2 = getMetricAffectingSpan();

        for (String text : texts) {
            // Get the line breaks without any span
            int[] breaks = getBreaks(text);

            // Add spans on all possible offsets
            for (int spanStart1 = 0; spanStart1 < text.length(); spanStart1++) {
                for (int spanEnd1 = spanStart1; spanEnd1 < text.length(); spanEnd1++) {
                    SpannableStringBuilder ssb = new SpannableStringBuilder(text);
                    ssb.setSpan(metricAffectingSpan1, spanStart1, spanEnd1,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    for (int spanStart2 = 0; spanStart2 < text.length(); spanStart2++) {
                        for (int spanEnd2 = spanStart2; spanEnd2 < text.length(); spanEnd2++) {
                            ssb.setSpan(metricAffectingSpan2, spanStart2, spanEnd2,
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            layout(ssb, breaks);
                        }
                    }
                }
            }
        }
    }

    public static String replace(String string, char c, char r) {
        return string.replaceAll(String.valueOf(c), String.valueOf(r));
    }

    public void testClassIS() {
        char[] classISCharacters = new char[] {'.', ',', ':', ';'};
        char[] digitCharacters = new char[] {'0', '\u0660', '\u06F0', '\u0966', '\uFF10'};

        for (char c : classISCharacters) {
            // .,:; are class IS breakpoints... (but still shouldn't break alphabetic chars)
            //              01234567
            layout(replace("L XXX#X", '#', c), new int[] {2});
            layout(replace("L XXXX#X", '#', c), new int[] {2});

            // ...except when adjacent to digits
            for (char d : digitCharacters) {
                //                      01234567
                layout(replace(replace("L XX0#X", '#', c), '0', d), new int[] {2});
                layout(replace(replace("L XXX#0", '#', c), '0', d), new int[] {2});
                layout(replace(replace("L XXX0#X", '#', c), '0', d), new int[] {2});
                layout(replace(replace("L XXXX#0", '#', c), '0', d), new int[] {2});
            }
        }
    }

    public void testClassSYandHY() {
        char[] classSYorHYCharacters = new char[] {'/', '-'};
        char[] digitCharacters = new char[] {'0', '\u0660', '\u06F0', '\u0966', '\uFF10'};

        for (char c : classSYorHYCharacters) {
            // / is a class SY breakpoint, - a class HY...
            //              01234567
            layout(replace("L XXX#X", '#', c), new int[] {6});
            layout(replace("L XXXX#X", '#', c), new int[] {2});

            // ...except when followed by a digits
            for (char d : digitCharacters) {
                //                      01234567
                layout(replace(replace("L XX0#X", '#', c), '0', d), new int[] {6});
                layout(replace(replace("L XXX#0", '#', c), '0', d), new int[] {2});
                layout(replace(replace("L XXX0#X", '#', c), '0', d), new int[] {2});
                layout(replace(replace("L XXXX#0", '#', c), '0', d), new int[] {2});
            }
        }
    }

    public void testClassID() {
        char ideographic = '\u8a9e'; // regular ideographic character
        char hyphen = '\u30A0'; // KATAKANA-HIRAGANA DOUBLE HYPHEN, ideographic but non starter

        // Single ideographs are normal characters
        layout("L XXX" + ideographic, NO_BREAK);
        layout("L XXX" + ideographic + "X", new int[] {2});
        layout("L XXXX" + ideographic, new int[] {2});
        layout("L XXXX" + ideographic + "X", new int[] {2});

        // Two adjacent ideographs create a possible breakpoint
        layout("L X" + ideographic + ideographic + "X", NO_BREAK);
        layout("L X" + ideographic + ideographic + "XX", new int[] {4});
        layout("L XX" + ideographic + ideographic + "XX", new int[] {5});
        layout("L XXX" + ideographic + ideographic + "X", new int[] {6});
        layout("L XXXX" + ideographic + ideographic + "X", new int[] {2});

        // Except when the second one is a non starter
        layout("L X" + ideographic + hyphen + "X", NO_BREAK);
        layout("L X" + ideographic + hyphen + "XX", new int[] {2});
        layout("L XX" + ideographic + hyphen + "XX", new int[] {2});
        layout("L XXX" + ideographic + hyphen + "X", new int[] {2});
        layout("L XXXX" + ideographic + hyphen + "X", new int[] {2});

        // When the non-starter is first, a pair of ideographic characters is a line break
        layout("L X" + hyphen + ideographic + "X", NO_BREAK);
        layout("L X" + hyphen + ideographic + "XX", new int[] {4});
        layout("L XX" + hyphen + ideographic + "XX", new int[] {5});
        layout("L XXX" + hyphen + ideographic + "X", new int[] {6});
        layout("L XXXX" + hyphen + ideographic + "X", new int[] {2});
    }

    public void testReplacementSpan() {
        // Add ReplacementSpan to the string
    }

    public void testParagraphs() {
        // Add \n to the text
    }

    public void testWithEmoji() {
        // Surrogate emoji characters get replaced by a bitmap
    }

    public void testWithSurrogate() {
        layout("LX" + SURR_FIRST + SURR_SECOND, NO_BREAK);
        layout("LXXXX" + SURR_FIRST + SURR_SECOND, NO_BREAK);
        // LXXXXI (91) + SURR_FIRST (7) fits. But we should not break the surrogate pair
        // Bug: surrogate pair is broken, should be 6 (breaking after the 'V')
        // Maybe not: may be ok if the second character of a pair always has a 0-width
        layout("LXXXXI" + SURR_FIRST + SURR_SECOND, new int[] {7});

        // LXXXXI (95) + SURR_SECOND (3) fits, but this is not a valid surrogate pair, breaking it
        layout("LXXXXV" + SURR_SECOND + SURR_FIRST, new int[] {7});

        layout("C" + SURR_FIRST + SURR_SECOND, new int[] {1});
    }

    public void testNarrowWidth() {
        int[] widths = new int[] { 0, 4, 10 };
        String[] texts = new String[] { "", "X", " ", "XX", " X", "XXX" };

        for (String text: texts) {
            // 15 is such that only one character will fit
            int[] breaks = getBreaks(text, 15);

            // Width under 15 should all lead to the same line break
            for (int width: widths) {
                layout(text, breaks, width);
            }
        }
    }

    public void testNarrowWidthWithSpace() {
        int[] widths = new int[] { 0, 4 };
        for (int width: widths) {
            layout("X ", new int[] {1}, width);
            layout("X  ", new int[] {1}, width);
            layout("XX ", new int[] {1, 2}, width);
            layout("XX  ", new int[] {1, 2}, width);
            layout("X  X", new int[] {1, 3}, width);
            layout("X X", new int[] {1, 2}, width);

            layout(" ", NO_BREAK, width);
            layout(" X", new int[] {1}, width);
            layout("  ", NO_BREAK, width);
            layout(" X X", new int[] {1, 2, 3}, width);
            layout("  X", new int[] {2}, width);
        }
    }

    public void testNarrowWidthZeroWidth() {
        int[] widths = new int[] { 0, 4 };
        for (int width: widths) {
            layout("X.", new int[] {1}, width);
            layout("X__", new int[] {1}, width);
            layout("X__X", new int[] {1, 3}, width); // Could be {1}
            layout("X__X_", new int[] {1, 3, 4}, width); // Could be {1, 4}

            layout("_", NO_BREAK, width);
            layout("__", NO_BREAK, width);
            layout("_X", new int[] {1}, width); // Could be NO_BREAK
            layout("_X_", new int[] {1, 2}, width); // Could be {2}
            layout("_X__", new int[] {1, 2}, width); // Could be {2}
        }
    }

    public void testMaxLines() {
        layoutMaxLines("C", NO_BREAK, 1);
        layoutMaxLines("C C", new int[] {2}, 1);
        layoutMaxLines("C C", new int[] {2}, 2);
        layoutMaxLines("CC", new int[] {1}, 1);
        layoutMaxLines("CC", new int[] {1}, 2);
    }
}

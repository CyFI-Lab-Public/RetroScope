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

import com.android.quicksearchbox.MockTextAppearanceFactory.MockStyleSpan;
import com.android.quicksearchbox.util.LevenshteinDistance.Token;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.Spanned;

/**
 * Tests for {@link LevenshteinSuggestionFormatter}.
 */
@SmallTest
public class LevenshteinFormatterTest extends AndroidTestCase {

    private LevenshteinSuggestionFormatter mFormatter;
    private int mSuggestedStyle;
    private int mQueryStyle;

    @Override
    protected void setUp() throws Exception {
        mFormatter = new LevenshteinSuggestionFormatter(new MockTextAppearanceFactory());
        mSuggestedStyle = MockTextAppearanceFactory.ID_SUGGESTED_TEXT;
        mQueryStyle = MockTextAppearanceFactory.ID_QUERY_TEXT;
    }

    private void verifyTokenizeResult(String input, String... output) {
        Token[] tokens = mFormatter.tokenize(input);
        assertEquals(output.length, tokens.length);
        for (int i=0; i<output.length; ++i) {
            assertEquals(output[i], tokens[i].toString());
        }
    }

    public void testTokenizeNoTokens() {
        verifyTokenizeResult("");
        verifyTokenizeResult("  ");
    }

    public void testTokenizeSingleToken() {
        verifyTokenizeResult("singleToken", "singleToken");
    }

    public void testTokenizeTwoTokens() {
        verifyTokenizeResult("two tokens", "two", "tokens");
    }

    public void testTokenizeLeadingSpaces() {
        verifyTokenizeResult(" evil kittens", "evil", "kittens");
        verifyTokenizeResult("        furry lizards", "furry", "lizards");
    }

    public void testTokenizeTrailingSpaces() {
        verifyTokenizeResult("mechanical elephant ", "mechanical", "elephant");
        verifyTokenizeResult("disappointed dog       ", "disappointed", "dog");
    }

    public void testTokenizeManySpaces() {
        verifyTokenizeResult("happy     horses", "happy", "horses");
    }

    public void testTokenizeLongSentence() {
        verifyTokenizeResult("The fool looks at a finger that points at the sky",
                "The", "fool", "looks", "at", "a", "finger", "that", "points", "at", "the", "sky");
    }

    public void testTokenizeWithPunctuation() {
        verifyTokenizeResult("Hitchhiker's guide", "Hitchhiker's", "guide");
        verifyTokenizeResult("full. stop. ", "full.", "stop.");
        verifyTokenizeResult("' . ; . ..", "'", ".", ";", ".", "..");
    }

    public void testTokenizeWithTabs() {
        verifyTokenizeResult("paranoid\tandroid\t", "paranoid", "android");
    }

    private void verifyFindMatches(String source, String target, String... newTokensInTarget) {
        Token[] sourceTokens = mFormatter.tokenize(source);
        Token[] targetTokens = mFormatter.tokenize(target);

        int[] matches = mFormatter.findMatches(sourceTokens, targetTokens);
        assertEquals(targetTokens.length, matches.length);
        int newTokenCount = 0;
        int lastSourceToken = -1;
        for (int i=0; i<targetTokens.length; ++i) {

            int sourceIdx = matches[i];
            if (sourceIdx < 0) {
                String targetToken = targetTokens[i].toString();
                assertTrue("Unexpected new token '" + targetToken + "'",
                        newTokenCount < newTokensInTarget.length);

                assertEquals(newTokensInTarget[newTokenCount], targetToken);
                ++newTokenCount;
            } else {
                assertTrue("Source token out of order", lastSourceToken < sourceIdx);
                Token srcToken = sourceTokens[sourceIdx];
                Token trgToken = targetTokens[i];
                assertTrue("'" + srcToken + "' is not a prefix of '" + trgToken + "'",
                        srcToken.prefixOf(trgToken));
                lastSourceToken = sourceIdx;
            }
        }
    }

    public void testFindMatchesSameTokens() {
        verifyFindMatches("", "");
        verifyFindMatches("one", "one");
        verifyFindMatches("one two three", "one two three");
    }

    public void testFindMatchesNewTokens() {
        verifyFindMatches("", "one", "one");
        verifyFindMatches("one", "one two", "two");
        verifyFindMatches("one", "one two three", "two", "three");
        verifyFindMatches("two", "one two three", "one", "three");
        verifyFindMatches("pictures", "pictures of kittens", "of", "kittens");
    }

    public void testFindMatchesReplacedTokens() {
        verifyFindMatches("one", "two", "two");
        verifyFindMatches("one", "two three", "two", "three");
        verifyFindMatches("two", "one three", "one", "three");
        verifyFindMatches("pictures", "of kittens", "of", "kittens");
    }

    public void testFindMatchesDuplicateTokens() {
        verifyFindMatches("badger", "badger badger", "badger");
        verifyFindMatches("badger", "badger badger badger", "badger", "badger");
        verifyFindMatches("badger badger", "badger badger badger", "badger");
        verifyFindMatches("badger badger badger", "badger badger badger");
        // mushroom!
    }

    private void verifyFormatSuggestion(String query, String suggestion, SpanFormat... spans) {
        Spanned s = mFormatter.formatSuggestion(query, suggestion);
        for (SpanFormat span : spans) {
            span.verify(s);
        }
    }

    public void testFormatSuggestionEmptyStrings() {
        verifyFormatSuggestion("", "");
    }

    public void testFormatSuggestionEmptyQuery() {
        verifyFormatSuggestion("", "suggestion",
                new SpanFormat(0, "suggestion", mSuggestedStyle));
    }

    public void testFormatSuggestionQuerySuggested() {
        verifyFormatSuggestion("query", "query",
                new SpanFormat(0, "query", mQueryStyle));
    }

    public void testFormatSuggestionExtraWordsSuggested() {
        verifyFormatSuggestion("query", "query suggested",
                new SpanFormat(0, "query", mQueryStyle),
                new SpanFormat(6, "suggested", mSuggestedStyle));

        verifyFormatSuggestion("pictures", "pictures of kittens",
                new SpanFormat(0,  "pictures", mQueryStyle),
                new SpanFormat(9,  "of", mSuggestedStyle),
                new SpanFormat(12, "kittens", mSuggestedStyle));

        verifyFormatSuggestion("pictures of", "pictures of kittens dying",
                new SpanFormat(0,  "pictures", mQueryStyle),
                new SpanFormat(9,  "of", mQueryStyle),
                new SpanFormat(12, "kittens", mSuggestedStyle),
                new SpanFormat(20, "dying", mSuggestedStyle));
    }

    public void testFormatSuggestionExtraWordSuggestedAtStart() {
        verifyFormatSuggestion("query", "suggested query",
                new SpanFormat(0, "suggested", mSuggestedStyle),
                new SpanFormat(10, "query", mQueryStyle));
    }

    public void testFormatSuggestionAlternativeWordSuggested() {
        verifyFormatSuggestion("query", "suggested",
                new SpanFormat(0, "suggested", mSuggestedStyle));
    }

    public void testFormatSuggestionDuplicateWords() {
        verifyFormatSuggestion("", "badger badger",
                new SpanFormat(0, "badger", mSuggestedStyle),
                new SpanFormat(7, "badger", mSuggestedStyle));

        verifyFormatSuggestion("badger", "badger badger",
                new SpanFormat(0, "badger", mQueryStyle),
                new SpanFormat(7, "badger", mSuggestedStyle));

        verifyFormatSuggestion("badger badger", "badger badger",
                new SpanFormat(0, "badger", mQueryStyle),
                new SpanFormat(7, "badger", mQueryStyle));

        verifyFormatSuggestion("badger badger", "badger badger badger",
                new SpanFormat(0, "badger", mQueryStyle),
                new SpanFormat(7, "badger", mQueryStyle),
                new SpanFormat(14, "badger", mSuggestedStyle));
    }

    public void testFormatSuggestionDuplicateSequences() {
        verifyFormatSuggestion("dem bones", "dem bones dem bones",
                new SpanFormat(0, "dem", mQueryStyle),
                new SpanFormat(4, "bones", mQueryStyle),
                new SpanFormat(10, "dem", mSuggestedStyle),
                new SpanFormat(14, "bones", mSuggestedStyle)
        );

        verifyFormatSuggestion("dem bones", "dem dry bones dem bones",
                new SpanFormat(0, "dem", mQueryStyle),
                new SpanFormat(4, "dry", mSuggestedStyle),
                new SpanFormat(8, "bones", mQueryStyle),
                new SpanFormat(14, "dem", mSuggestedStyle),
                new SpanFormat(18, "bones", mSuggestedStyle)
        );

        verifyFormatSuggestion("dem dry bones", "dry bones dem dry bones dem dry bones",
                new SpanFormat(0, "dry", mSuggestedStyle),
                new SpanFormat(4, "bones", mSuggestedStyle),
                new SpanFormat(10, "dem", mQueryStyle),
                new SpanFormat(14, "dry", mQueryStyle),
                new SpanFormat(18, "bones", mQueryStyle),
                new SpanFormat(24, "dem", mSuggestedStyle),
                new SpanFormat(28, "dry", mSuggestedStyle),
                new SpanFormat(32, "bones", mSuggestedStyle)
        );
    }

    public void testFormatSuggestionWordCompletion() {
        verifyFormatSuggestion("hitch", "hitchhiker",
                new SpanFormat(0, "hitch", mQueryStyle),
                new SpanFormat(5, "hiker", mSuggestedStyle)
        );

        verifyFormatSuggestion("hitch", "hitchhiker's guide",
                new SpanFormat(0, "hitch", mQueryStyle),
                new SpanFormat(5, "hiker's", mSuggestedStyle),
                new SpanFormat(13, "guide", mSuggestedStyle)
        );

        verifyFormatSuggestion("hitchhiker's g", "hitchhiker's guide",
                new SpanFormat(0, "hitchhiker's", mQueryStyle),
                new SpanFormat(13, "g", mQueryStyle),
                new SpanFormat(14, "uide", mSuggestedStyle)
        );

        verifyFormatSuggestion("hitchhiker's g", "hitchhiker's guide to the galaxy",
                new SpanFormat(0, "hitchhiker's", mQueryStyle),
                new SpanFormat(13, "g", mQueryStyle),
                new SpanFormat(14, "uide", mSuggestedStyle),
                new SpanFormat(19, "to", mSuggestedStyle),
                new SpanFormat(22, "the", mSuggestedStyle),
                new SpanFormat(26, "galaxy", mSuggestedStyle)
        );
    }

    public void testFormatSuggestionWordSplitting() {
        verifyFormatSuggestion("dimsum", "dim sum",
                new SpanFormat(0, "dim", mSuggestedStyle),
                new SpanFormat(4, "sum", mSuggestedStyle)
        );

        verifyFormatSuggestion("dimsum london", "dim sum london",
                new SpanFormat(0, "dim", mSuggestedStyle),
                new SpanFormat(4, "sum", mSuggestedStyle),
                new SpanFormat(8, "london", mQueryStyle)
        );

        verifyFormatSuggestion("dimsum london", "dim sum london yummy",
                new SpanFormat(0, "dim", mSuggestedStyle),
                new SpanFormat(4, "sum", mSuggestedStyle),
                new SpanFormat(8, "london", mQueryStyle),
                new SpanFormat(15, "yummy", mSuggestedStyle)
        );
    }

    public void testFormatSuggestionWordCombining() {
        verifyFormatSuggestion("hos pital", "hospital",
                new SpanFormat(0, "hos", mQueryStyle),
                new SpanFormat(3, "pital", mSuggestedStyle)
        );

        verifyFormatSuggestion("hos pital", "hospital waiting times",
                new SpanFormat(0, "hos", mQueryStyle),
                new SpanFormat(3, "pital", mSuggestedStyle),
                new SpanFormat(9, "waiting", mSuggestedStyle),
                new SpanFormat(17, "times", mSuggestedStyle)
        );

        verifyFormatSuggestion("hos pital waiting", "hospital waiting",
                new SpanFormat(0, "hos", mQueryStyle),
                new SpanFormat(3, "pital", mSuggestedStyle),
                new SpanFormat(9, "waiting", mQueryStyle)
        );

        verifyFormatSuggestion("hospital wait ing times", "hospital waiting times",
                new SpanFormat(0, "hospital", mQueryStyle),
                new SpanFormat(9, "wait", mQueryStyle),
                new SpanFormat(13, "ing", mSuggestedStyle),
                new SpanFormat(17, "times", mQueryStyle)
        );
    }

    public void testFormatSuggestionCapitalization() {
        verifyFormatSuggestion("Flay", "flay",
                new SpanFormat(0, "flay", mQueryStyle));

        verifyFormatSuggestion("STEERPI", "steerpike",
                new SpanFormat(0, "steerpi", mQueryStyle),
                new SpanFormat(7, "ke", mSuggestedStyle));

        verifyFormatSuggestion("STEErpi", "steerpike",
                new SpanFormat(0, "steerpi", mQueryStyle),
                new SpanFormat(7, "ke", mSuggestedStyle));

        verifyFormatSuggestion("TITUS", "titus groan",
                new SpanFormat(0, "titus", mQueryStyle),
                new SpanFormat(6, "groan", mSuggestedStyle));
}

    private class SpanFormat {
        private final int mStart;
        private final int mEnd;
        private final String mExpectedText;
        private final int mStyle;
        public SpanFormat(int start, String expectedText, int style) {
            mStart = start;
            mEnd = start + expectedText.length();
            mExpectedText = expectedText;
            mStyle = style;
        }
        public void verify(Spanned spanned) {
            String spannedText = spanned.subSequence(mStart, mEnd).toString();
            assertEquals("Test error", mExpectedText, spannedText);
            MockStyleSpan[] spans = spanned.getSpans(mStart, mEnd, MockStyleSpan.class);
            assertEquals("Wrong number of spans in '" + spannedText + "'", 1, spans.length);
            assertEquals("Wrong style for '" + spannedText + "' at position " + mStart,
                    mStyle, spans[0].getId());
        }
    }

}

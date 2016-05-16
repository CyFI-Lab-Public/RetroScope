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

import java.util.ArrayList;

import android.test.AndroidTestCase;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

/**
 * Test {@link SpannableStringBuilder}.
 */
public class SpannableStringBuilderSpanTest extends AndroidTestCase {

    private static final boolean DEBUG = false;

    private SpanSet mSpanSet = new SpanSet();
    private SpanSet mReplacementSpanSet = new SpanSet();
    private int testCounter;

    public void testReplaceWithSpans() {
        testCounter = 0;
        String originals[] = { "", "A", "here", "Well, hello there" };
        String replacements[] = { "", "X", "test", "longer replacement" };

        for (String original: originals) {
            for (String replacement: replacements) {
                replace(original, replacement);
            }
        }
    }

    private void replace(String original, String replacement) {
        PositionSet positionSet = new PositionSet(4);
        positionSet.addPosition(0);
        positionSet.addPosition(original.length() / 3);
        positionSet.addPosition(2 * original.length() / 3);
        positionSet.addPosition(original.length());

        PositionSet replPositionSet = new PositionSet(4);
        replPositionSet.addPosition(0);
        replPositionSet.addPosition(replacement.length() / 3);
        replPositionSet.addPosition(2 * replacement.length() / 3);
        replPositionSet.addPosition(replacement.length());

        for (int s = 0; s < positionSet.size(); s++) {
            for (int e = s; e < positionSet.size(); e++) {
                for (int rs = 0; rs < replPositionSet.size(); rs++) {
                    for (int re = rs; re < replPositionSet.size(); re++) {
                        replaceWithRange(original,
                                positionSet.getPosition(s), positionSet.getPosition(e),
                                replacement,
                                replPositionSet.getPosition(rs), replPositionSet.getPosition(re));
                    }
                }
            }
        }
    }

    private void replaceWithRange(String original, int replaceStart, int replaceEnd,
            String replacement, int replacementStart, int replacementEnd) {
        int flags[] = { Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, Spanned.SPAN_INCLUSIVE_INCLUSIVE,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE, Spanned.SPAN_INCLUSIVE_EXCLUSIVE };


        for (int flag: flags) {
            replaceWithSpanFlag(original, replaceStart, replaceEnd,
                    replacement, replacementStart, replacementEnd, flag);
        }
    }

    private void replaceWithSpanFlag(String original, int replaceStart, int replaceEnd,
            String replacement, int replacementStart, int replacementEnd, int flag) {

        testCounter++;
        int debugTestNumber = -1;
        if (debugTestNumber >= 0 && testCounter != debugTestNumber) return;

        String subReplacement = replacement.substring(replacementStart, replacementEnd);
        String expected = original.substring(0, replaceStart) +
                subReplacement + original.substring(replaceEnd, original.length());
        if (DEBUG) System.out.println("#" + testCounter + ", replace \"" + original + "\" [" +
                replaceStart + " " + replaceEnd + "] by \"" + subReplacement + "\" -> \"" +
                expected + "\", flag=" + flag);

        SpannableStringBuilder originalSpannable = new SpannableStringBuilder(original);
        Spannable replacementSpannable = new SpannableStringBuilder(replacement);

        mSpanSet.initSpans(originalSpannable, replaceStart, replaceEnd, flag);
        mReplacementSpanSet.initSpans(replacementSpannable, replacementStart, replacementEnd, flag);

        originalSpannable.replace(replaceStart, replaceEnd, replacementSpannable,
                replacementStart, replacementEnd);

        assertEquals(expected, originalSpannable.toString());

        checkSpanPositions(originalSpannable, replaceStart, replaceEnd, subReplacement.length(),
                flag);
        checkReplacementSpanPositions(originalSpannable, replaceStart, replacementSpannable,
                replacementStart, replacementEnd, flag);
    }

    private void checkSpanPositions(Spannable spannable, int replaceStart, int replaceEnd,
            int replacementLength, int flag) {
        int count = 0;
        int replacedLength = replaceEnd - replaceStart;
        int delta = replacementLength - replacedLength;
        boolean textIsReplaced = replacedLength > 0 && replacementLength > 0;
        for (int s = 0; s < mSpanSet.mPositionSet.size(); s++) {
            for (int e = s; e < mSpanSet.mPositionSet.size(); e++) {
                Object span = mSpanSet.mSpans[count];
                int originalStart = mSpanSet.mPositionSet.getPosition(s);
                int originalEnd = mSpanSet.mPositionSet.getPosition(e);
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                int startStyle = mSpanSet.mSpanStartPositionStyle[count];
                int endStyle = mSpanSet.mSpanEndPositionStyle[count];
                count++;

                if (!isValidSpan(originalStart, originalEnd, flag)) continue;

                if (DEBUG) System.out.println("  " + originalStart + "," + originalEnd + " -> " +
                        start + "," + end + " | " + startStyle + " " + endStyle +
                        " delta=" + delta);

                // This is the exception to the following generic code where we need to consider
                // both the start and end styles.
                if (startStyle == SpanSet.INSIDE && endStyle == SpanSet.INSIDE &&
                        flag == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE &&
                        (replacementLength == 0 || originalStart > replaceStart ||
                        originalEnd < replaceEnd)) {
                    // 0-length spans should have been removed
                    assertEquals(-1, start);
                    assertEquals(-1, end);
                    mSpanSet.mRecorder.assertRemoved(span, originalStart, originalEnd);
                    continue;
                }

                switch (startStyle) {
                    case SpanSet.BEFORE:
                        assertEquals(originalStart, start);
                        break;
                    case SpanSet.INSIDE:
                        switch (flag) {
                            case Spanned.SPAN_EXCLUSIVE_EXCLUSIVE:
                            case Spanned.SPAN_EXCLUSIVE_INCLUSIVE:
                                // start is POINT
                                if (originalStart == replaceStart && textIsReplaced) {
                                    assertEquals(replaceStart, start);
                                } else {
                                    assertEquals(replaceStart + replacementLength, start);
                                }
                                break;
                            case Spanned.SPAN_INCLUSIVE_INCLUSIVE:
                            case Spanned.SPAN_INCLUSIVE_EXCLUSIVE:
                                // start is MARK
                                if (originalStart == replaceEnd && textIsReplaced) {
                                    assertEquals(replaceStart + replacementLength, start);
                                } else {
                                    assertEquals(replaceStart, start);
                                }
                                break;
                            case Spanned.SPAN_PARAGRAPH:
                                fail("TODO");
                                break;
                        }
                        break;
                    case SpanSet.AFTER:
                        assertEquals(originalStart + delta, start);
                        break;
                }

                switch (endStyle) {
                    case SpanSet.BEFORE:
                        assertEquals(originalEnd, end);
                        break;
                    case SpanSet.INSIDE:
                        switch (flag) {
                            case Spanned.SPAN_EXCLUSIVE_EXCLUSIVE:
                            case Spanned.SPAN_INCLUSIVE_EXCLUSIVE:
                                // end is MARK
                                if (originalEnd == replaceEnd && textIsReplaced) {
                                    assertEquals(replaceStart + replacementLength, end);
                                } else {
                                    assertEquals(replaceStart, end);
                                }
                                break;
                            case Spanned.SPAN_INCLUSIVE_INCLUSIVE:
                            case Spanned.SPAN_EXCLUSIVE_INCLUSIVE:
                                // end is POINT
                                if (originalEnd == replaceStart && textIsReplaced) {
                                    assertEquals(replaceStart, end);
                                } else {
                                    assertEquals(replaceStart + replacementLength, end);
                                }
                                break;
                            case Spanned.SPAN_PARAGRAPH:
                                fail("TODO");
                                break;
                        }
                        break;
                    case SpanSet.AFTER:
                        assertEquals(originalEnd + delta, end);
                        break;
                }

                if (start != originalStart || end != originalEnd) {
                    mSpanSet.mRecorder.assertChanged(span, originalStart, originalEnd, start, end);
                } else {
                    mSpanSet.mRecorder.assertUnmodified(span);
                }
            }
        }
    }

    private void checkReplacementSpanPositions(Spannable originalSpannable, int replaceStart,
            Spannable replacementSpannable, int replStart, int replEnd, int flag) {

        // Get all spans overlapping the replacement substring region
        Object[] addedSpans = replacementSpannable.getSpans(replStart, replEnd, Object.class);

        int count = 0;
        for (int s = 0; s < mReplacementSpanSet.mPositionSet.size(); s++) {
            for (int e = s; e < mReplacementSpanSet.mPositionSet.size(); e++) {
                Object span = mReplacementSpanSet.mSpans[count];
                int originalStart = mReplacementSpanSet.mPositionSet.getPosition(s);
                int originalEnd = mReplacementSpanSet.mPositionSet.getPosition(e);
                int start = originalSpannable.getSpanStart(span);
                int end = originalSpannable.getSpanEnd(span);
                count++;

                if (!isValidSpan(originalStart, originalEnd, flag)) continue;

                if (DEBUG) System.out.println("  replacement " + originalStart + "," + originalEnd +
                        " -> " + start + "," + end);

                // There should be no change reported to the replacement string spanWatcher
                mReplacementSpanSet.mRecorder.assertUnmodified(span);

                boolean shouldBeAdded = false;
                for (int i = 0; i < addedSpans.length; i++) {
                    if (addedSpans[i] == span) {
                        shouldBeAdded = true;
                        break;
                    }
                }

                if (shouldBeAdded) {
                    int newStart = Math.max(0, originalStart - replStart) + replaceStart;
                    int newEnd = Math.min(originalEnd, replEnd) - replStart + replaceStart;
                    if (isValidSpan(newStart, newEnd, flag)) {
                        assertEquals(start, newStart);
                        assertEquals(end, newEnd);
                        mSpanSet.mRecorder.assertAdded(span, start, end);
                        continue;
                    }
                }

                mSpanSet.mRecorder.assertUnmodified(span);
            }
        }
    }

    private static boolean isValidSpan(int start, int end, int flag) {
        // Zero length SPAN_EXCLUSIVE_EXCLUSIVE are not allowed
        if (flag == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE && start == end) return false;
        return true;
    }

    private static class PositionSet {
        private int[] mPositions;
        private int mSize;

        PositionSet(int capacity) {
            mPositions = new int[capacity];
            mSize = 0;
        }

        void addPosition(int position) {
            if (mSize == 0 || position > mPositions[mSize - 1]) {
                mPositions[mSize] = position;
                mSize++;
            }
        }

        void clear() {
            mSize = 0;
        }

        int size() {
            return mSize;
        }

        int getPosition(int index) {
            return mPositions[index];
        }
    }

    private static class SpanSet {
        private static final int NB_POSITIONS = 8;

        static final int BEFORE = 0;
        static final int INSIDE = 1;
        static final int AFTER = 2;

        private PositionSet mPositionSet;
        private Object[] mSpans;
        private int[] mSpanStartPositionStyle;
        private int[] mSpanEndPositionStyle;
        private SpanWatcherRecorder mRecorder;

        SpanSet() {
            mPositionSet = new PositionSet(NB_POSITIONS);
            int nbSpans = (NB_POSITIONS * (NB_POSITIONS + 1)) / 2;
            mSpanStartPositionStyle = new int[nbSpans];
            mSpanEndPositionStyle = new int[nbSpans];
            mSpans = new Object[nbSpans];
            for (int i = 0; i < nbSpans; i++) {
                mSpans[i] = new Object();
            }
            mRecorder = new SpanWatcherRecorder();
        }

        static int getPositionStyle(int position, int replaceStart, int replaceEnd) {
            if (position < replaceStart) return BEFORE;
            else if (position <= replaceEnd) return INSIDE;
            else return AFTER;
        }

        /**
         * Creates spans for all the possible interval cases. On short strings, or when the
         * replaced region is at the beginning/end of the text, some of these spans may have an
         * identical range
         */
        void initSpans(Spannable spannable, int rangeStart, int rangeEnd, int flag) {
            mPositionSet.clear();
            mPositionSet.addPosition(0);
            mPositionSet.addPosition(rangeStart / 2);
            mPositionSet.addPosition(rangeStart);
            mPositionSet.addPosition((2 * rangeStart + rangeEnd) / 3);
            mPositionSet.addPosition((rangeStart + 2 * rangeEnd) / 3);
            mPositionSet.addPosition(rangeEnd);
            mPositionSet.addPosition((rangeEnd + spannable.length()) / 2);
            mPositionSet.addPosition(spannable.length());

            int count = 0;
            for (int s = 0; s < mPositionSet.size(); s++) {
                for (int e = s; e < mPositionSet.size(); e++) {
                    int start = mPositionSet.getPosition(s);
                    int end = mPositionSet.getPosition(e);
                    if (isValidSpan(start, end, flag)) {
                        spannable.setSpan(mSpans[count], start, end, flag);
                    }
                    mSpanStartPositionStyle[count] = getPositionStyle(start, rangeStart, rangeEnd);
                    mSpanEndPositionStyle[count] = getPositionStyle(end, rangeStart, rangeEnd);
                    count++;
                }
            }

            // Must be done after all the spans were added, to not record these additions
            spannable.setSpan(mRecorder, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            mRecorder.reset(spannable);
        }
    }

    private static class SpanWatcherRecorder implements SpanWatcher {
        private ArrayList<AddedRemoved> mAdded = new ArrayList<AddedRemoved>();
        private ArrayList<AddedRemoved> mRemoved = new ArrayList<AddedRemoved>();
        private ArrayList<Changed> mChanged = new ArrayList<Changed>();

        private Spannable mSpannable;

        private class AddedRemoved {
            Object span;
            int start;
            int end;

            public AddedRemoved(Object span, int start, int end) {
                this.span = span;
                this.start = start;
                this.end = end;
            }
        }

        private class Changed {
            Object span;
            int oldStart;
            int oldEnd;
            int newStart;
            int newEnd;

            public Changed(Object span, int oldStart, int oldEnd, int newStart, int newEnd) {
                this.span = span;
                this.oldStart = oldStart;
                this.oldEnd = oldEnd;
                this.newStart = newStart;
                this.newEnd = newEnd;
            }
        }

        public void reset(Spannable spannable) {
            mSpannable = spannable;
            mAdded.clear();
            mRemoved.clear();
            mChanged.clear();
        }

        @Override
        public void onSpanAdded(Spannable text, Object span, int start, int end) {
            if (text == mSpannable) mAdded.add(new AddedRemoved(span, start, end));
        }

        @Override
        public void onSpanRemoved(Spannable text, Object span, int start, int end) {
            if (text == mSpannable) mRemoved.add(new AddedRemoved(span, start, end));
        }

        @Override
        public void onSpanChanged(Spannable text, Object span, int ostart, int oend, int nstart,
                int nend) {
            if (text == mSpannable) mChanged.add(new Changed(span, ostart, oend, nstart, nend));
        }

        public void assertUnmodified(Object span) {
            for (AddedRemoved added: mAdded) {
                if (added.span == span)
                    fail("Span " + span + " was added and not unmodified");
            }
            for (AddedRemoved removed: mRemoved) {
                if (removed.span == span)
                    fail("Span " + span + " was removed and not unmodified");
            }
            for (Changed changed: mChanged) {
                if (changed.span == span)
                    fail("Span " + span + " was changed and not unmodified");
            }
        }

        public void assertChanged(Object span, int oldStart, int oldEnd, int newStart, int newEnd) {
            for (Changed changed : mChanged) {
                if (changed.span == span) {
                    assertEquals(changed.newStart, newStart);
                    assertEquals(changed.newEnd, newEnd);
                    // TODO previous range is not correctly sent in case a bound was inside the
                    // affected range. See SpannableStringBuilder#sendToSpanWatchers limitation
                    //assertEquals(changed.oldStart, oldStart);
                    //assertEquals(changed.oldEnd, oldEnd);
                    return;
                }
            }
            fail("Span " + span + " was not changed");
        }

        public void assertAdded(Object span, int start, int end) {
            for (AddedRemoved added : mAdded) {
                if (added.span == span) {
                    assertEquals(added.start, start);
                    assertEquals(added.end, end);
                    return;
                }
            }
            fail("Span " + span + " was not added");
        }

        public void assertRemoved(Object span, int start, int end) {
            for (AddedRemoved removed : mRemoved) {
                if (removed.span == span) {
                    assertEquals(removed.start, start);
                    assertEquals(removed.end, end);
                    return;
                }
            }
            fail("Span " + span + " was not removed");
        }
    }

    // TODO Thoroughly test the SPAN_PARAGRAPH span flag.
}

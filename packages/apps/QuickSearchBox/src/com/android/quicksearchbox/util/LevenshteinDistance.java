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

package com.android.quicksearchbox.util;

/**
 * This class represents the matrix used in the Levenshtein distance algorithm, together
 * with the algorithm itself which operates on the matrix.
 *
 * We also track of the individual operations applied to transform the source string into the
 * target string so we can trace the path taken through the matrix afterwards, in order to
 * perform the formatting as required.
 */
public class LevenshteinDistance {
    public static final int EDIT_DELETE = 0;
    public static final int EDIT_INSERT = 1;
    public static final int EDIT_REPLACE = 2;
    public static final int EDIT_UNCHANGED = 3;

    private final Token[] mSource;
    private final Token[] mTarget;
    private final int[][] mEditTypeTable;
    private final int[][] mDistanceTable;

    public LevenshteinDistance(Token[] source, Token[] target) {
        final int sourceSize = source.length;
        final int targetSize = target.length;
        final int[][] editTab = new int[sourceSize+1][targetSize+1];
        final int[][] distTab = new int[sourceSize+1][targetSize+1];
        editTab[0][0] = EDIT_UNCHANGED;
        distTab[0][0] = 0;
        for (int i = 1; i <= sourceSize; ++i) {
            editTab[i][0] = EDIT_DELETE;
            distTab[i][0] = i;
        }
        for (int i = 1; i <= targetSize; ++i) {
            editTab[0][i] = EDIT_INSERT;
            distTab[0][i] = i;
        }
        mEditTypeTable = editTab;
        mDistanceTable = distTab;
        mSource = source;
        mTarget = target;
    }

    /**
     * Implementation of Levenshtein distance algorithm.
     *
     * @return The Levenshtein distance.
     */
    public int calculate() {
        final Token[] src = mSource;
        final Token[] trg = mTarget;
        final int sourceLen = src.length;
        final int targetLen = trg.length;
        final int[][] distTab = mDistanceTable;
        final int[][] editTab = mEditTypeTable;
        for (int s = 1; s <= sourceLen; ++s) {
            Token sourceToken = src[s-1];
            for (int t = 1; t <= targetLen; ++t) {
                Token targetToken = trg[t-1];
                int cost = sourceToken.prefixOf(targetToken) ? 0 : 1;

                int distance = distTab[s-1][t] + 1;
                int type = EDIT_DELETE;

                int d = distTab[s][t - 1];
                if (d + 1 < distance ) {
                    distance = d + 1;
                    type = EDIT_INSERT;
                }

                d = distTab[s - 1][t - 1];
                if (d + cost < distance) {
                    distance = d + cost;
                    type = cost == 0 ? EDIT_UNCHANGED : EDIT_REPLACE;
                }
                distTab[s][t] = distance;
                editTab[s][t] = type;
            }
        }
        return distTab[sourceLen][targetLen];
    }

    /**
     * Gets the list of operations which were applied to each target token; {@link #calculate} must
     * have been called on this object before using this method.
     * @return A list of {@link EditOperation}s indicating the origin of each token in the target
     *      string. The position of the token indicates the position in the source string of the
     *      token that was unchanged/replaced, or the position in the source after which a target
     *      token was inserted.
     */
    public EditOperation[] getTargetOperations() {
        final int trgLen = mTarget.length;
        final EditOperation[] ops = new EditOperation[trgLen];
        int targetPos = trgLen;
        int sourcePos = mSource.length;
        final int[][] editTab = mEditTypeTable;
        while (targetPos > 0) {
            int editType = editTab[sourcePos][targetPos];
            switch (editType) {
                case LevenshteinDistance.EDIT_DELETE:
                    sourcePos--;
                    break;
                case LevenshteinDistance.EDIT_INSERT:
                    targetPos--;
                    ops[targetPos] = new EditOperation(editType, sourcePos);
                    break;
                case LevenshteinDistance.EDIT_UNCHANGED:
                case LevenshteinDistance.EDIT_REPLACE:
                    targetPos--;
                    sourcePos--;
                    ops[targetPos] = new EditOperation(editType, sourcePos);
                    break;
            }
        }

        return ops;
    }

    public static final class EditOperation {
        private final int mType;
        private final int mPosition;
        public EditOperation(int type, int position) {
            mType = type;
            mPosition = position;
        }
        public int getType() {
            return mType;
        }
        public int getPosition() {
            return mPosition;
        }
    }

    public static final class Token implements CharSequence {
        private final char[] mContainer;
        public final int mStart;
        public final int mEnd;

        public Token(char[] container, int start, int end) {
            mContainer = container;
            mStart = start;
            mEnd = end;
        }

        public int length() {
            return mEnd - mStart;
        }

        @Override
        public String toString() {
            // used in tests only.
            return subSequence(0, length());
        }

        public boolean prefixOf(final Token that) {
            final int len = length();
            if (len > that.length()) return false;
            final int thisStart = mStart;
            final int thatStart = that.mStart;
            final char[] thisContainer = mContainer;
            final char[] thatContainer = that.mContainer;
            for (int i = 0; i < len; ++i) {
                if (thisContainer[thisStart + i] != thatContainer[thatStart + i]) {
                    return false;
                }
            }
            return true;
        }

        public char charAt(int index) {
            return mContainer[index + mStart];
        }

        public String subSequence(int start, int end) {
            return new String(mContainer, mStart + start, length());
        }

    }
}

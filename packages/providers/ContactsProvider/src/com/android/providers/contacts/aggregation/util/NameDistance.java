/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.providers.contacts.aggregation.util;

import java.util.Arrays;

/**
 * A string distance calculator, particularly suited for name matching.
* <p>
 * A detailed discussion of the topic of record linkage in general and name matching
 * in particular can be found in this article:
 * <blockquote>
 * Winkler, W. E. (2006). "Overview of Record Linkage and Current Research Directions".
 * Research Report Series, RRS.
 * </blockquote>
 */
public class NameDistance {

    private static final float WINKLER_BONUS_THRESHOLD = 0.7f;
    private static final int MIN_EXACT_PREFIX_LENGTH = 3;

    private final int mMaxLength;
    private final boolean mPrefixOnly;
    private final boolean[] mMatchFlags1;
    private final boolean[] mMatchFlags2;

    /**
     * Constructor.
     *
     * @param maxLength byte arrays are truncated if longer than this number
     */
    public NameDistance(int maxLength) {
        mMaxLength = maxLength;
        mPrefixOnly = false;
        mMatchFlags1 = new boolean[maxLength];
        mMatchFlags2 = new boolean[maxLength];
    }

    /**
     * Constructor for a matcher that only checks if one string is the exact prefix of the other
     */
    public NameDistance() {
        mPrefixOnly = true;
        mMaxLength = 0;
        mMatchFlags1 = mMatchFlags2 = null;
    }

    /**
     * Computes a string distance between two normalized strings passed as byte arrays.
     */
    public float getDistance(byte bytes1[], byte bytes2[]) {
        byte[] array1, array2;

        if (bytes1.length > bytes2.length) {
            array2 = bytes1;
            array1 = bytes2;
        } else {
            array2 = bytes2;
            array1 = bytes1;
        }

        int length1 = array1.length;
        if (length1 >= MIN_EXACT_PREFIX_LENGTH) {
            boolean prefix = true;
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] != array2[i]) {
                    prefix = false;
                    break;
                }
            }
            if (prefix) {
                return 1.0f;
            }
        }

        if (mPrefixOnly) {
            return 0;
        }

        if (length1 > mMaxLength) {
            length1 = mMaxLength;
        }

        int length2 = array2.length;
        if (length2 > mMaxLength) {
            length2 = mMaxLength;
        }

        Arrays.fill(mMatchFlags1, 0, length1, false);
        Arrays.fill(mMatchFlags2, 0, length2, false);

        int range = length2 / 2 - 1;
        if (range < 0) {
            range = 0;
        }

        int matches = 0;
        for (int i = 0; i < length1; i++) {
            byte c1 = array1[i];

            int from = i - range;
            if (from < 0) {
                from = 0;
            }

            int to = i + range + 1;
            if (to > length2) {
                to = length2;
            }

            for (int j = from; j < to; j++) {
                if (!mMatchFlags2[j] && c1 == array2[j]) {
                    mMatchFlags1[i] = mMatchFlags2[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) {
            return 0f;
        }

        int transpositions = 0;
        int j = 0;
        for (int i = 0; i < length1; i++) {
            if (mMatchFlags1[i]) {
                while (!mMatchFlags2[j]) {
                    j++;
                }
                if (array1[i] != array2[j]) {
                    transpositions++;
                }
                j++;
            }
        }

        float m = matches;
        float jaro = ((m / length1 + m / length2 + (m - (transpositions / 2f)) / m)) / 3;

        if (jaro < WINKLER_BONUS_THRESHOLD) {
            return jaro;
        }

        // Add Winkler bonus
        int prefix = 0;
        for (int i = 0; i < length1; i++) {
            if (bytes1[i] != bytes2[i]) {
                break;
            }
            prefix++;
        }

        return jaro + Math.min(0.1f, 1f / length2) * prefix * (1 - jaro);
    }
}

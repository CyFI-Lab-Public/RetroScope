/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.util.cts;

import android.util.SparseLongArray;

import junit.framework.TestCase;

/**
 * Tests for {@link SparseLongArray}.
 */
public class SparseLongArrayTest extends TestCase {
    private static final int[] KEYS = {12, 23, 4, 6, 8, 1, 3, -12, 0, -3, 11, 14, -23};
    private static final long[] VALUES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final int LENGTH = VALUES.length;
    private static final int NON_EXISTED_KEY = 123;
    private static final long VALUE_FOR_NON_EXISTED_KEY = -1;

    public void testSparseArrayWithDefaultCapacity() {
        SparseLongArray sparseArray = new SparseLongArray();
        assertEquals(0, sparseArray.size());

        int length = VALUES.length;

        for (int i = 0; i < length; i++) {
            sparseArray.put(KEYS[i], VALUES[i]);
            assertEquals(i + 1, sparseArray.size());
        }

        for (int i = 0; i < length; i++) {
            assertEquals(i, sparseArray.get(KEYS[i]));
        }

        for (int i = 0; i < length; i++) {
            assertEquals(sparseArray.indexOfValue(VALUES[i]), sparseArray.indexOfKey(KEYS[i]));
        }

        // for key already exist, old value will be replaced
        int existKey = KEYS[0];
        long oldValue = VALUES[0]; // 0
        long newValue = 100;
        assertEquals(oldValue, sparseArray.get(existKey));
        assertEquals(LENGTH, sparseArray.size());
        sparseArray.put(existKey, newValue);
        assertEquals(newValue, sparseArray.get(existKey));
        assertEquals(LENGTH, sparseArray.size());

        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                sparseArray.get(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(0L, sparseArray.get(NON_EXISTED_KEY)); // the default value is 0

        int size = sparseArray.size();
        sparseArray.append(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY);
        assertEquals(size + 1, sparseArray.size());
        assertEquals(size, sparseArray.indexOfKey(NON_EXISTED_KEY));
        assertEquals(size, sparseArray.indexOfValue(VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(NON_EXISTED_KEY, sparseArray.keyAt(size));
        assertEquals(VALUE_FOR_NON_EXISTED_KEY, sparseArray.valueAt(size));

        size = sparseArray.size();
        assertEquals(VALUES[1], sparseArray.get(KEYS[1]));
        assertFalse(VALUE_FOR_NON_EXISTED_KEY == VALUES[1]);
        sparseArray.delete(KEYS[1]);
        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                sparseArray.get(KEYS[1], VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(size - 1, sparseArray.size());

        size = sparseArray.size();
        assertEquals(VALUES[2], sparseArray.get(KEYS[2]));
        assertFalse(VALUE_FOR_NON_EXISTED_KEY == VALUES[2]);
        sparseArray.delete(KEYS[2]);
        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                sparseArray.get(KEYS[2], VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(size - 1, sparseArray.size());

        sparseArray.clear();
        assertEquals(0, sparseArray.size());
    }

    public void testSparseArrayWithSpecifiedCapacity() {
        SparseLongArray sparseArray = new SparseLongArray(5);
        assertEquals(0, sparseArray.size());

        int length = VALUES.length;

        for (int i = 0; i < length; i++) {
            sparseArray.put(KEYS[i], VALUES[i]);
            assertEquals(i + 1, sparseArray.size());
        }

        for (int i = 0; i < length; i++) {
            assertEquals(VALUES[i], sparseArray.get(KEYS[i]));
        }

        for (int i = 0; i < length; i++) {
            assertEquals(sparseArray.indexOfValue(VALUES[i]), sparseArray.indexOfKey(KEYS[i]));
        }

        // for key already exist, old value will be replaced
        int existKey = KEYS[0];
        long oldValue = VALUES[0]; // 0
        long newValue = 100;
        assertEquals(oldValue, sparseArray.get(existKey));
        assertEquals(LENGTH, sparseArray.size());
        sparseArray.put(existKey, newValue);
        assertEquals(newValue, sparseArray.get(existKey));
        assertEquals(LENGTH, sparseArray.size());

        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                     sparseArray.get(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(0L, sparseArray.get(NON_EXISTED_KEY)); // the default value is 0

        int size = sparseArray.size();
        sparseArray.append(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY);
        assertEquals(size + 1, sparseArray.size());
        assertEquals(size, sparseArray.indexOfKey(NON_EXISTED_KEY));
        assertEquals(size, sparseArray.indexOfValue(VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(NON_EXISTED_KEY, sparseArray.keyAt(size));
        assertEquals(VALUE_FOR_NON_EXISTED_KEY, sparseArray.valueAt(size));

        size = sparseArray.size();
        assertEquals(VALUES[1], sparseArray.get(KEYS[1]));
        assertFalse(VALUE_FOR_NON_EXISTED_KEY == VALUES[1]);
        sparseArray.delete(KEYS[1]);
        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                sparseArray.get(KEYS[1], VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(size - 1, sparseArray.size());

        size = sparseArray.size();
        assertEquals(VALUES[2], sparseArray.get(KEYS[2]));
        assertFalse(VALUE_FOR_NON_EXISTED_KEY == VALUES[2]);
        sparseArray.delete(KEYS[2]);
        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                sparseArray.get(KEYS[2], VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(size - 1, sparseArray.size());

        sparseArray.clear();
        assertEquals(0, sparseArray.size());
    }

    public void testIterationOrder() {
        SparseLongArray sparseArray = new SparseLongArray();
        // No matter in which order they are inserted.
        sparseArray.put(1, 2L);
        sparseArray.put(10, 20L);
        sparseArray.put(5, 40L);
        sparseArray.put(Integer.MAX_VALUE, Long.MIN_VALUE);
        // The keys are returned in order.
        assertEquals(1, sparseArray.keyAt(0));
        assertEquals(5, sparseArray.keyAt(1));
        assertEquals(10, sparseArray.keyAt(2));
        assertEquals(Integer.MAX_VALUE, sparseArray.keyAt(3));
        // The values are returned in the order of the corresponding keys.
        assertEquals(2L, sparseArray.valueAt(0));
        assertEquals(40L, sparseArray.valueAt(1));
        assertEquals(20L, sparseArray.valueAt(2));
        assertEquals(Long.MIN_VALUE, sparseArray.valueAt(3));
    }

}

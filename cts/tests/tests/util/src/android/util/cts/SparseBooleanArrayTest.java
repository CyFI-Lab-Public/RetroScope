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

package android.util.cts;

import android.test.AndroidTestCase;
import android.util.SparseBooleanArray;

public class SparseBooleanArrayTest extends AndroidTestCase {
    private static final int[] KEYS   = {12, 23, 4, 6, 8, 1, 3, -12, 0, -3, 11, 14, -23};
    private static final boolean[] VALUES =
        {true,  false, true, false, false, true, true, true, true, false, false, false, false};
    private static final int NON_EXISTED_KEY = 123;
    private static final boolean VALUE_FOR_NON_EXISTED_KEY = true;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSparseBooleanArrayWithDefaultCapacity() {
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        assertEquals(0, sparseBooleanArray.size());

        int length = VALUES.length;
        for (int i = 0; i < length; i++) {
            sparseBooleanArray.put(KEYS[i], VALUES[i]);
            assertEquals(i + 1, sparseBooleanArray.size());
        }
        for (int i = 0; i < length; i++) {
            assertEquals(VALUES[i], sparseBooleanArray.get(KEYS[i]));
        }
        int truePos = sparseBooleanArray.indexOfValue(true);
        int falsePos = sparseBooleanArray.indexOfValue(false);
        int expectPos;
        int keyIndex;
        for (int i = 0; i < length; i++) {
            keyIndex = sparseBooleanArray.indexOfKey(KEYS[i]);
            assertEquals(VALUES[i], sparseBooleanArray.valueAt(keyIndex));
            expectPos = VALUES[i] ? truePos : falsePos;
            assertEquals(expectPos, sparseBooleanArray.indexOfValue(VALUES[i]));
        }

        // for key already exist, old value will be replaced
        int existKey = KEYS[0];
        boolean oldValue = VALUES[0]; // true
        boolean newValue = false;
        assertEquals(oldValue, sparseBooleanArray.get(existKey));
        assertEquals(13, sparseBooleanArray.size());
        sparseBooleanArray.put(existKey, newValue);
        assertEquals(newValue, sparseBooleanArray.get(existKey));
        assertEquals(13, sparseBooleanArray.size());

        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                     sparseBooleanArray.get(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(false, sparseBooleanArray.get(NON_EXISTED_KEY)); // the default value is false

        int size = sparseBooleanArray.size();
        sparseBooleanArray.append(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY);
        assertEquals(size + 1, sparseBooleanArray.size());
        assertEquals(size, sparseBooleanArray.indexOfKey(NON_EXISTED_KEY));
        expectPos = VALUE_FOR_NON_EXISTED_KEY ? truePos : falsePos;
        assertEquals(expectPos, sparseBooleanArray.indexOfValue(VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(NON_EXISTED_KEY, sparseBooleanArray.keyAt(size));
        assertEquals(VALUE_FOR_NON_EXISTED_KEY, sparseBooleanArray.valueAt(size));

        assertEquals(VALUES[1], sparseBooleanArray.get(KEYS[1]));
        assertFalse(VALUE_FOR_NON_EXISTED_KEY == VALUES[1]);
        sparseBooleanArray.delete(KEYS[1]);
        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                sparseBooleanArray.get(KEYS[1], VALUE_FOR_NON_EXISTED_KEY));

        sparseBooleanArray.clear();
        assertEquals(0, sparseBooleanArray.size());
    }

    public void testSparseBooleanArrayWithSpecifiedCapacity() {
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray(5);
        assertEquals(0, sparseBooleanArray.size());

        int length = VALUES.length;
        for (int i = 0; i < length; i++) {
            sparseBooleanArray.put(KEYS[i], VALUES[i]);
            assertEquals(i + 1, sparseBooleanArray.size());
        }
        for (int i = 0; i < length; i++) {
            assertEquals(VALUES[i], sparseBooleanArray.get(KEYS[i]));
        }
        int truePos = sparseBooleanArray.indexOfValue(true);
        int falsePos = sparseBooleanArray.indexOfValue(false);
        int expectPos;
        int keyIndex;
        for (int i = 0; i < length; i++) {
            keyIndex = sparseBooleanArray.indexOfKey(KEYS[i]);
            assertEquals(VALUES[i], sparseBooleanArray.valueAt(keyIndex));
            expectPos = VALUES[i] ? truePos : falsePos;
            assertEquals(expectPos, sparseBooleanArray.indexOfValue(VALUES[i]));
        }

        // for key already exist, old value will be replaced
        int existKey = KEYS[0];
        boolean oldValue = VALUES[0]; // true
        boolean newValue = false;
        assertEquals(oldValue, sparseBooleanArray.get(existKey));
        assertEquals(13, sparseBooleanArray.size());
        sparseBooleanArray.put(existKey, newValue);
        assertEquals(newValue, sparseBooleanArray.get(existKey));
        assertEquals(13, sparseBooleanArray.size());

        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                     sparseBooleanArray.get(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(false, sparseBooleanArray.get(NON_EXISTED_KEY)); // the default value is false

        int size = sparseBooleanArray.size();
        sparseBooleanArray.append(NON_EXISTED_KEY, VALUE_FOR_NON_EXISTED_KEY);
        assertEquals(size + 1, sparseBooleanArray.size());
        assertEquals(size, sparseBooleanArray.indexOfKey(NON_EXISTED_KEY));
        expectPos = VALUE_FOR_NON_EXISTED_KEY ? truePos : falsePos;
        assertEquals(expectPos, sparseBooleanArray.indexOfValue(VALUE_FOR_NON_EXISTED_KEY));
        assertEquals(NON_EXISTED_KEY, sparseBooleanArray.keyAt(size));
        assertEquals(VALUE_FOR_NON_EXISTED_KEY, sparseBooleanArray.valueAt(size));

        assertEquals(VALUES[1], sparseBooleanArray.get(KEYS[1]));
        assertFalse(VALUE_FOR_NON_EXISTED_KEY == VALUES[1]);
        sparseBooleanArray.delete(KEYS[1]);
        assertEquals(VALUE_FOR_NON_EXISTED_KEY,
                sparseBooleanArray.get(KEYS[1], VALUE_FOR_NON_EXISTED_KEY));

        sparseBooleanArray.clear();
        assertEquals(0, sparseBooleanArray.size());
    }

    public void testIterationOrder() {
        SparseBooleanArray sparseArray = new SparseBooleanArray();
        // No matter in which order they are inserted.
        sparseArray.put(1, true);
        sparseArray.put(10, false);
        sparseArray.put(5, true);
        sparseArray.put(Integer.MAX_VALUE, false);
        // The keys are returned in order.
        assertEquals(1, sparseArray.keyAt(0));
        assertEquals(5, sparseArray.keyAt(1));
        assertEquals(10, sparseArray.keyAt(2));
        assertEquals(Integer.MAX_VALUE, sparseArray.keyAt(3));
        // The values are returned in the order of the corresponding keys.
        assertEquals(true, sparseArray.valueAt(0));
        assertEquals(true, sparseArray.valueAt(1));
        assertEquals(false, sparseArray.valueAt(2));
        assertEquals(false, sparseArray.valueAt(3));
    }

}


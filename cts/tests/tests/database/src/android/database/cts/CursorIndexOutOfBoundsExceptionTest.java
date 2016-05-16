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

package android.database.cts;

import android.database.CursorIndexOutOfBoundsException;
import android.test.AndroidTestCase;

public class CursorIndexOutOfBoundsExceptionTest extends AndroidTestCase {

    public void testConstructors() {
        int INDEX = 100;
        int SIZE = 99;
        String expected1 = "Expected exception message";
        String expected2 = "Index " + INDEX + " requested, with a size of " + SIZE;
        // Test CursorIndexOutOfBoundsException(String)
        try {
            throw new CursorIndexOutOfBoundsException(null);
        } catch (CursorIndexOutOfBoundsException e) {
            assertNull(e.getMessage());
        }

        try {
            throw new CursorIndexOutOfBoundsException(expected1);
        } catch (CursorIndexOutOfBoundsException e) {
            assertEquals(expected1, e.getMessage());
        }

        // Test CursorIndexOutOfBoundsException(int, int)
        try {
            throw new CursorIndexOutOfBoundsException(INDEX, SIZE);
        } catch (CursorIndexOutOfBoundsException e) {
            assertEquals(expected2, e.getMessage());
        }
    }
}

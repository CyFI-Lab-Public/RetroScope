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
 * limitations under the License.
 */
package android.gesture.cts;


import android.gesture.GesturePoint;

import junit.framework.TestCase;

/**
 * A (very) simple compatibility test for {@link GesturePoint}
 */
public class GesturePointTest extends TestCase {

    /**
     * Test for {@link GesturePoint#GesturePoint(float, float, long)
     *
     * Verifies member variables are properly set.
     */
   public void testGesturePointConstructor() {
        final float expected_x = 1.1f;
        final float expected_y = 1.1f;
        final long expected_timestamp = 1;
        GesturePoint point = new GesturePoint(expected_x, expected_y, expected_timestamp);
        assertEquals(expected_x, point.x);
        assertEquals(expected_y, point.y);
        assertEquals(expected_timestamp, point.timestamp);
    }
}

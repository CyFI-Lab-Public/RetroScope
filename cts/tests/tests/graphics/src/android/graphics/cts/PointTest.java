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

package android.graphics.cts;

import android.graphics.Point;
import android.test.AndroidTestCase;

public class PointTest extends AndroidTestCase {

    private Point mPoint;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPoint = null;
    }

    public void testConstructor() {

        // new the Point instance
        mPoint = new Point();

        // new the Point instance
        mPoint = new Point(10, 10);

        Point point = new Point(10, 10);
        // new the Point instance
        mPoint = new Point(point);

    }

    public void testSet() {
        mPoint = new Point();
        mPoint.set(3, 4);
        assertEquals(3, mPoint.x);
        assertEquals(4, mPoint.y);
    }

    public void testEquals1() {
        mPoint = new Point(3, 4);
        assertTrue(mPoint.equals(3, 4));
        assertFalse(mPoint.equals(4, 3));
    }

    public void testEquals2() {
        mPoint = new Point(3, 4);
        Point point = new Point(3, 4);
        assertTrue(mPoint.equals(point));
        point = new Point(4, 3);
        assertFalse(mPoint.equals(point));
    }

    public void testHashCode() {
        mPoint = new Point(10, 10);
        Point p = new Point(100, 10);
        assertTrue(p.hashCode() != mPoint.hashCode());
    }

    public void testToString() {
        mPoint = new Point();
        assertNotNull(mPoint.toString());
    }

    public void testOffset() {
        mPoint = new Point(10, 10);
        mPoint.offset(1, 1);
        assertEquals(11, mPoint.x);
        assertEquals(11, mPoint.y);
    }

    public void testNegate() {
        mPoint = new Point(10, 10);
        mPoint.negate();
        assertEquals(-10, mPoint.x);
        assertEquals(-10, mPoint.y);
    }

}

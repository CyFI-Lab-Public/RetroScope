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

import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.RegionIterator;
import android.test.AndroidTestCase;

public class RegionIteratorTest extends AndroidTestCase {

    public void testConstructor() {
        new RegionIterator(new Region());
    }

    public void testNext() {
        Region region = new Region();
        region.set(1, 1, 10, 10);
        Rect rect = new Rect();
        rect.set(1, 1, 1, 1);
        RegionIterator regionIterator = new RegionIterator(region);
        try {
            regionIterator.next(null);
            fail("should throw exception");
        } catch (Exception e) {
        }
        assertTrue(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(10, rect.right);
        assertEquals(10, rect.bottom);
        rect.set(1, 1, 1, 1);
        assertFalse(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(1, rect.right);
        assertEquals(1, rect.bottom);

        region.set(1, 1, 10, 10);
        rect.set(5, 5, 15, 15);
        region.op(rect, Region.Op.UNION);
        regionIterator = new RegionIterator(region);
        assertFalse(region.isRect());
        assertTrue(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(10, rect.right);
        assertEquals(5, rect.bottom);
        assertTrue(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(5, rect.top);
        assertEquals(15, rect.right);
        assertEquals(10, rect.bottom);
        assertTrue(regionIterator.next(rect));
        assertEquals(5, rect.left);
        assertEquals(10, rect.top);
        assertEquals(15, rect.right);
        assertEquals(15, rect.bottom);
        rect.set(1, 1, 1, 1);
        assertFalse(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(1, rect.right);
        assertEquals(1, rect.bottom);

        region.set(1, 1, 10, 10);
        rect.set(5, 5, 15, 15);
        region.op(rect, Region.Op.DIFFERENCE);
        regionIterator = new RegionIterator(region);
        assertFalse(region.isRect());
        assertTrue(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(10, rect.right);
        assertEquals(5, rect.bottom);
        assertTrue(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(5, rect.top);
        assertEquals(5, rect.right);
        assertEquals(10, rect.bottom);
        rect.set(1, 1, 1, 1);
        assertFalse(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(1, rect.right);
        assertEquals(1, rect.bottom);

        region.set(1, 1, 10, 10);
        rect.set(5, 5, 15, 15);
        region.op(rect, Region.Op.INTERSECT);
        regionIterator = new RegionIterator(region);
        assertTrue(region.isRect());
        assertTrue(regionIterator.next(rect));
        assertEquals(5, rect.left);
        assertEquals(5, rect.top);
        assertEquals(10, rect.right);
        assertEquals(10, rect.bottom);
        rect.set(1, 1, 1, 1);
        assertFalse(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(1, rect.right);
        assertEquals(1, rect.bottom);

        region.set(1, 1, 10, 10);
        rect.set(5, 5, 15, 15);
        region.op(rect, Region.Op.REVERSE_DIFFERENCE);
        regionIterator = new RegionIterator(region);
        assertFalse(region.isRect());
        assertTrue(regionIterator.next(rect));
        assertEquals(10, rect.left);
        assertEquals(5, rect.top);
        assertEquals(15, rect.right);
        assertEquals(10, rect.bottom);
        assertTrue(regionIterator.next(rect));
        assertEquals(5, rect.left);
        assertEquals(10, rect.top);
        assertEquals(15, rect.right);
        assertEquals(15, rect.bottom);
        rect.set(1, 1, 1, 1);
        assertFalse(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(1, rect.right);
        assertEquals(1, rect.bottom);

        region.set(1, 1, 10, 10);
        rect.set(5, 5, 15, 15);
        region.op(rect, Region.Op.XOR);
        regionIterator = new RegionIterator(region);
        assertFalse(region.isRect());
        assertTrue(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(10, rect.right);
        assertEquals(5, rect.bottom);
        assertTrue(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(5, rect.top);
        assertEquals(5, rect.right);
        assertEquals(10, rect.bottom);
        assertTrue(regionIterator.next(rect));
        assertEquals(10, rect.left);
        assertEquals(5, rect.top);
        assertEquals(15, rect.right);
        assertEquals(10, rect.bottom);
        assertTrue(regionIterator.next(rect));
        assertEquals(5, rect.left);
        assertEquals(10, rect.top);
        assertEquals(15, rect.right);
        assertEquals(15, rect.bottom);
        rect.set(1, 1, 1, 1);
        assertFalse(regionIterator.next(rect));
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(1, rect.right);
        assertEquals(1, rect.bottom);
    }

}

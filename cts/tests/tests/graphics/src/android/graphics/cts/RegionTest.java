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

import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class RegionTest extends AndroidTestCase {

    // DIFFERENCE
    private final static int[][] DIFFERENCE_WITH1 = {{0, 0}, {4, 4},
        {10, 10}, {19, 19}, {19, 0}, {10, 4}, {4, 10}, {0, 19}};
    private final static int[][] DIFFERENCE_WITHOUT1 = {{5, 5}, {9, 9},
        {9, 5}, {5, 9}};

    private final static int[][] DIFFERENCE_WITH2 = {{0, 0}, {19, 0}, {9, 9},
        {19, 9}, {0, 19}, {9, 19}};
    private final static int[][] DIFFERENCE_WITHOUT2 = {{10, 10}, {19, 10},
        {10, 19}, {19, 19}, {29, 10}, {29, 29}, {10, 29}};

    private final static int[][] DIFFERENCE_WITH3 = {{0, 0}, {19, 0}, {0, 19},
        {19, 19}};
    private final static int[][] DIFFERENCE_WITHOUT3 = {{40, 40}, {40, 59},
        {59, 40}, {59, 59}};

    // INTERSECT
    private final static int[][] INTERSECT_WITH1 = {{5, 5}, {9, 9},
        {9, 5}, {5, 9}};
    private final static int[][] INTERSECT_WITHOUT1 = {{0, 0}, {2, 2}, {4, 4},
        {10, 10}, {19, 19}, {19, 0}, {10, 4}, {4, 10}, {0, 19}};

    private final static int[][] INTERSECT_WITH2 = {{10, 10}, {19, 10},
        {10, 19}, {19, 19}};
    private final static int[][] INTERSECT_WITHOUT2 = {{0, 0}, {19, 0}, {9, 9},
        {19, 9}, {0, 19}, {9, 19}, {29, 10}, {29, 29}, {10, 29}};

    // UNION
    private final static int[][] UNION_WITH1 = {{0, 0}, {2, 2}, {4, 4}, {6, 6},
        {10, 10}, {19, 19}, {19, 0}, {10, 4}, {4, 10}, {0, 19},
        {5, 5}, {9, 9}, {9, 5}, {5, 9}};
    private final static int[][] UNION_WITHOUT1 = {{0, 20}, {20, 20}, {20, 0}};

    private final static int[][] UNION_WITH2 = {
        {0, 0}, {2, 2}, {19, 0}, {9, 9}, {19, 9}, {0, 19}, {9, 19}, {21, 21},
        {10, 10}, {19, 10}, {10, 19}, {19, 19}, {29, 10}, {29, 29}, {10, 29}};
    private final static int[][] UNION_WITHOUT2 = {
        {0, 29}, {0, 20}, {9, 29}, {9, 20},
        {29, 0}, {20, 0}, {29, 9}, {20, 9}};

    private final static int[][] UNION_WITH3 = {
        {0, 0}, {2, 2}, {19, 0}, {0, 19}, {19, 19},
        {40, 40}, {41, 41}, {40, 59}, {59, 40}, {59, 59}};
    private final static int[][] UNION_WITHOUT3 = {{20, 20}, {39, 39}};

    // XOR
    private final static int[][] XOR_WITH1 = {{0, 0}, {2, 2}, {4, 4},
        {10, 10}, {19, 19}, {19, 0}, {10, 4}, {4, 10}, {0, 19}};
    private final static int[][] XOR_WITHOUT1 = {{5, 5}, {6, 6}, {9, 9},
        {9, 5}, {5, 9}};

    private final static int[][] XOR_WITH2 = {
        {0, 0}, {2, 2}, {19, 0}, {9, 9}, {19, 9}, {0, 19}, {9, 19}, {21, 21},
        {29, 10}, {10, 29}, {20, 10}, {10, 20}, {20, 20}, {29, 29}};
    private final static int[][] XOR_WITHOUT2 = {{10, 10}, {11, 11}, {19, 10},
        {10, 19}, {19, 19}};

    private final static int[][] XOR_WITH3 = {
        {0, 0}, {2, 2}, {19, 0}, {0, 19}, {19, 19},
        {40, 40}, {41, 41}, {40, 59}, {59, 40}, {59, 59}};
    private final static int[][] XOR_WITHOUT3 = {{20, 20}, {39, 39}};

    // REVERSE_DIFFERENCE
    private final static int[][] REVERSE_DIFFERENCE_WITH2 = {{29, 10}, {10, 29},
        {20, 10}, {10, 20}, {20, 20}, {29, 29}, {21, 21}};
    private final static int[][] REVERSE_DIFFERENCE_WITHOUT2 = {{0, 0}, {19, 0},
        {0, 19}, {19, 19}, {2, 2}, {11, 11}};

    private final static int[][] REVERSE_DIFFERENCE_WITH3 = {{40, 40}, {40, 59},
        {59, 40}, {59, 59}, {41, 41}};
    private final static int[][] REVERSE_DIFFERENCE_WITHOUT3 = {{0, 0}, {19, 0},
        {0, 19}, {19, 19}, {20, 20}, {39, 39}, {2, 2}};

    private Region mRegion;

    private void assertPointsInsideRegion(int[][] area) {
        for (int i = 0; i < area.length; i ++) {
            assertTrue(mRegion.contains(area[i][0], area[i][1]));
        }
    }

    private void assertPointsOutsideRegion(int[][] area) {
        for (int i = 0; i < area.length; i ++) {
            assertFalse(mRegion.contains(area[i][0], area[i][1]));
        }
    }

    public void testConstructor() {
        // Test Region()
        new Region();

        // Test Region(Region)
        Region oriRegion = new Region();
        new Region(oriRegion);

        // Test Region(Rect)
        Rect rect = new Rect();
        new Region(rect);

        // Test Region(int, int, int, int)
        new Region(0, 0, 100, 100);
    }

    public void testSet1() {

        mRegion = new Region();
        Rect rect = new Rect(1, 2, 3, 4);
        Region oriRegion = new Region(rect);
        assertTrue(mRegion.set(oriRegion));
        assertEquals(1, mRegion.getBounds().left);
        assertEquals(2, mRegion.getBounds().top);
        assertEquals(3, mRegion.getBounds().right);
        assertEquals(4, mRegion.getBounds().bottom);
    }

    public void testSet2() {

        mRegion = new Region();
        Rect rect = new Rect(1, 2, 3, 4);
        assertTrue(mRegion.set(rect));
        assertEquals(1, mRegion.getBounds().left);
        assertEquals(2, mRegion.getBounds().top);
        assertEquals(3, mRegion.getBounds().right);
        assertEquals(4, mRegion.getBounds().bottom);
    }

    public void testSet3() {

        mRegion = new Region();
        assertTrue(mRegion.set(1, 2, 3, 4));
        assertEquals(1, mRegion.getBounds().left);
        assertEquals(2, mRegion.getBounds().top);
        assertEquals(3, mRegion.getBounds().right);
        assertEquals(4, mRegion.getBounds().bottom);
    }

    public void testIsRect() {

        mRegion = new Region();
        assertFalse(mRegion.isRect());
        mRegion = new Region(1, 2, 3, 4);
        assertTrue(mRegion.isRect());
    }

    public void testIsComplex() {

        // Region is null
        mRegion = new Region();
        assertFalse(mRegion.isComplex());

        // Only one rectangle
        mRegion = new Region();
        mRegion.set(1, 2, 3, 4);
        assertFalse(mRegion.isComplex());

        // More than one rectangle
        mRegion = new Region();
        mRegion.set(1, 1, 2, 2);
        mRegion.union(new Rect(3, 3, 5, 5));
        assertTrue(mRegion.isComplex());
    }

    public void testQuickContains1() {

        mRegion = new Region();
        Rect rect = new Rect(1, 2, 3, 4);
        // This region not contains expected rectangle.
        assertFalse(mRegion.quickContains(rect));
        mRegion.set(rect);
        // This region contains only one rectangle and it is the expected one.
        assertTrue(mRegion.quickContains(rect));
        mRegion.set(5, 6, 7, 8);
        // This region contains more than one rectangle.
        assertFalse(mRegion.quickContains(rect));
    }

    public void testQuickContains2() {

        mRegion = new Region();
        // This region not contains expected rectangle.
        assertFalse(mRegion.quickContains(1, 2, 3, 4));
        mRegion.set(1, 2, 3, 4);
        // This region contains only one rectangle and it is the expected one.
        assertTrue(mRegion.quickContains(1, 2, 3, 4));
        mRegion.set(5, 6, 7, 8);
        // This region contains more than one rectangle.
        assertFalse(mRegion.quickContains(1, 2, 3, 4));
    }

    public void testUnion() {

        Rect rect1 = new Rect();
        Rect rect2 = new Rect(0, 0, 20, 20);
        Rect rect3 = new Rect(5, 5, 10, 10);
        Rect rect4 = new Rect(10, 10, 30, 30);
        Rect rect5 = new Rect(40, 40, 60, 60);

        // union (inclusive-or) the two regions
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // union null rectangle
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.union(rect1));
        assertTrue(mRegion.contains(6, 6));

        // 1. union rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.union(rect3));
        assertPointsInsideRegion(UNION_WITH1);
        assertPointsOutsideRegion(UNION_WITHOUT1);

        // 2. union rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.union(rect4));
        assertPointsInsideRegion(UNION_WITH2);
        assertPointsOutsideRegion(UNION_WITHOUT2);

        // 3. union rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.union(rect5));
        assertPointsInsideRegion(UNION_WITH3);
        assertPointsOutsideRegion(UNION_WITHOUT3);
    }

    public void testContains() {

        mRegion = new Region();
        mRegion.set(2, 2, 5, 5);
        // Not contain (1, 1).
        assertFalse(mRegion.contains(1, 1));

        // Test point inside this region.
        assertTrue(mRegion.contains(3, 3));

        // Test left-top corner.
        assertTrue(mRegion.contains(2, 2));

        // Test left-bottom corner.
        assertTrue(mRegion.contains(2, 4));

        // Test right-top corner.
        assertTrue(mRegion.contains(4, 2));

        // Test right-bottom corner.
        assertTrue(mRegion.contains(4, 4));

        // Though you set 5, but 5 is not contained by this region.
        assertFalse(mRegion.contains(5, 5));
        assertFalse(mRegion.contains(2, 5));
        assertFalse(mRegion.contains(5, 2));

        // Set a new rectangle.
        mRegion.set(6, 6, 8, 8);
        assertFalse(mRegion.contains(3, 3));
        assertTrue(mRegion.contains(7, 7));
    }

    public void testEmpty() {

        mRegion = new Region();
        assertTrue(mRegion.isEmpty());
        mRegion = null;
        mRegion = new Region(1, 2, 3, 4);
        assertFalse(mRegion.isEmpty());
        mRegion.setEmpty();
        assertTrue(mRegion.isEmpty());
    }

    public void testGetBounds() {

        // Exception
        try {
            mRegion.getBounds(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            //except here
        }

        // Normal, return true.
        Rect rect1 = new Rect(1, 2, 3, 4);
        mRegion = new Region(rect1);
        assertTrue(mRegion.getBounds(rect1));

        mRegion.setEmpty();
        Rect rect2 = new Rect(5, 6, 7, 8);
        assertFalse(mRegion.getBounds(rect2));
    }

    public void testOp1() {

        Rect rect1 = new Rect();
        Rect rect2 = new Rect(0, 0, 20, 20);
        Rect rect3 = new Rect(5, 5, 10, 10);
        Rect rect4 = new Rect(10, 10, 30, 30);
        Rect rect5 = new Rect(40, 40, 60, 60);

        assertNullRegionOp1(rect1);
        assertDifferenceOp1(rect1, rect2, rect3, rect4, rect5);
        assertIntersectOp1(rect1, rect2, rect3, rect4, rect5);
        assertUnionOp1(rect1, rect2, rect3, rect4, rect5);
        assertXorOp1(rect1, rect2, rect3, rect4, rect5);
        assertReverseDifferenceOp1(rect1, rect2, rect3, rect4, rect5);
        assertReplaceOp1(rect1, rect2, rect3, rect4, rect5);
    }

    private void assertNullRegionOp1(Rect rect1) {
        // Region without rectangle
        mRegion = null;
        mRegion = new Region();
        assertFalse(mRegion.op(rect1, Region.Op.DIFFERENCE));
        assertFalse(mRegion.op(rect1, Region.Op.INTERSECT));
        assertFalse(mRegion.op(rect1, Region.Op.UNION));
        assertFalse(mRegion.op(rect1, Region.Op.XOR));
        assertFalse(mRegion.op(rect1, Region.Op.REVERSE_DIFFERENCE));
        assertFalse(mRegion.op(rect1, Region.Op.REPLACE));
    }

    private void assertDifferenceOp1(Rect rect1, Rect rect2, Rect rect3,
            Rect rect4, Rect rect5) {
        // DIFFERENCE, Region with rectangle
        // subtract the op region from the first region
        mRegion = null;
        mRegion = new Region();
        // subtract null rectangle
        mRegion.set(rect2);
        assertTrue(mRegion.op(rect1, Region.Op.DIFFERENCE));

        // 1. subtract rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(rect3, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH1);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT1);

        // 2. subtract rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(11, 11));
        assertTrue(mRegion.op(rect4, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH2);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT2);

        // 3. subtract rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.op(rect5, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH3);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT3);
    }

    private void assertIntersectOp1(Rect rect1, Rect rect2, Rect rect3,
            Rect rect4, Rect rect5) {
        // INTERSECT, Region with rectangle
        // intersect the two regions
        mRegion = null;
        mRegion = new Region();
        // intersect null rectangle
        mRegion.set(rect2);
        assertFalse(mRegion.op(rect1, Region.Op.INTERSECT));

        // 1. intersect rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.op(rect3, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH1);
        assertPointsOutsideRegion(INTERSECT_WITHOUT1);

        // 2. intersect rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(9, 9));
        assertTrue(mRegion.op(rect4, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH2);
        assertPointsOutsideRegion(INTERSECT_WITHOUT2);

        // 3. intersect rectangle out of this region
        mRegion.set(rect2);
        assertFalse(mRegion.op(rect5, Region.Op.INTERSECT));
    }

    private void assertUnionOp1(Rect rect1, Rect rect2, Rect rect3, Rect rect4,
            Rect rect5) {
        // UNION, Region with rectangle
        // union (inclusive-or) the two regions
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // union null rectangle
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(rect1, Region.Op.UNION));
        assertTrue(mRegion.contains(6, 6));

        // 1. union rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(rect3, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH1);
        assertPointsOutsideRegion(UNION_WITHOUT1);

        // 2. union rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(rect4, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH2);
        assertPointsOutsideRegion(UNION_WITHOUT2);

        // 3. union rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(rect5, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH3);
        assertPointsOutsideRegion(UNION_WITHOUT3);
    }

    private void assertXorOp1(Rect rect1, Rect rect2, Rect rect3, Rect rect4,
            Rect rect5) {
        // XOR, Region with rectangle
        // exclusive-or the two regions
        mRegion = null;
        mRegion = new Region();
        // xor null rectangle
        mRegion.set(rect2);
        assertTrue(mRegion.op(rect1, Region.Op.XOR));

        // 1. xor rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(rect3, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH1);
        assertPointsOutsideRegion(XOR_WITHOUT1);

        // 2. xor rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(11, 11));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(rect4, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH2);
        assertPointsOutsideRegion(XOR_WITHOUT2);

        // 3. xor rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(rect5, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH3);
        assertPointsOutsideRegion(XOR_WITHOUT3);
    }

    private void assertReverseDifferenceOp1(Rect rect1, Rect rect2, Rect rect3,
            Rect rect4, Rect rect5) {
        // REVERSE_DIFFERENCE, Region with rectangle
        // reverse difference the first region from the op region
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // reverse difference null rectangle
        assertFalse(mRegion.op(rect1, Region.Op.REVERSE_DIFFERENCE));

        // 1. reverse difference rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertFalse(mRegion.op(rect3, Region.Op.REVERSE_DIFFERENCE));

        // 2. reverse difference rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(11, 11));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(rect4, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH2);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT2);

        // 3. reverse difference rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(rect5, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH3);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT3);
    }

    private void assertReplaceOp1(Rect rect1, Rect rect2, Rect rect3, Rect rect4,
            Rect rect5) {
        // REPLACE, Region with rectangle
        // replace the dst region with the op region
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // subtract null rectangle
        assertFalse(mRegion.op(rect1, Region.Op.REPLACE));
        // subtract rectangle inside this region
        mRegion.set(rect2);
        assertEquals(rect2, mRegion.getBounds());
        assertTrue(mRegion.op(rect3, Region.Op.REPLACE));
        assertNotSame(rect2, mRegion.getBounds());
        assertEquals(rect3, mRegion.getBounds());
        // subtract rectangle overlap this region
        mRegion.set(rect2);
        assertEquals(rect2, mRegion.getBounds());
        assertTrue(mRegion.op(rect4, Region.Op.REPLACE));
        assertNotSame(rect2, mRegion.getBounds());
        assertEquals(rect4, mRegion.getBounds());
        // subtract rectangle out of this region
        mRegion.set(rect2);
        assertEquals(rect2, mRegion.getBounds());
        assertTrue(mRegion.op(rect5, Region.Op.REPLACE));
        assertNotSame(rect2, mRegion.getBounds());
        assertEquals(rect5, mRegion.getBounds());
    }

    public void testOp2() {

        Rect rect2 = new Rect(0, 0, 20, 20);
        Rect rect3 = new Rect(5, 5, 10, 10);
        Rect rect4 = new Rect(10, 10, 30, 30);
        Rect rect5 = new Rect(40, 40, 60, 60);

        assertNullRegionOp2();
        assertDifferenceOp2(rect2);
        assertIntersectOp2(rect2);
        assertUnionOp2(rect2);
        assertXorOp2(rect2);
        assertReverseDifferenceOp2(rect2);
        assertReplaceOp2(rect2, rect3, rect4, rect5);
    }

    private void assertNullRegionOp2() {
        // Region without rectangle
        mRegion = null;
        mRegion = new Region();
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.DIFFERENCE));
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.INTERSECT));
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.UNION));
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.XOR));
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.REVERSE_DIFFERENCE));
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.REPLACE));
    }

    private void assertDifferenceOp2(Rect rect2) {
        // DIFFERENCE, Region with rectangle
        // subtract the op region from the first region
        mRegion = null;
        mRegion = new Region();
        // subtract null rectangle
        mRegion.set(rect2);
        assertTrue(mRegion.op(0, 0, 0, 0, Region.Op.DIFFERENCE));

        // 1. subtract rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(5, 5, 10, 10, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH1);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT1);

        // 2. subtract rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(11, 11));
        assertTrue(mRegion.op(10, 10, 30, 30, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH2);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT2);

        // 3. subtract rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.op(40, 40, 60, 60, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH3);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT3);
    }

    private void assertIntersectOp2(Rect rect2) {
        // INTERSECT, Region with rectangle
        // intersect the two regions
        mRegion = null;
        mRegion = new Region();
        // intersect null rectangle
        mRegion.set(rect2);
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.INTERSECT));

        // 1. intersect rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.op(5, 5, 10, 10, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH1);
        assertPointsOutsideRegion(INTERSECT_WITHOUT1);

        // 2. intersect rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(9, 9));
        assertTrue(mRegion.op(10, 10, 30, 30, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH2);
        assertPointsOutsideRegion(INTERSECT_WITHOUT2);

        // 3. intersect rectangle out of this region
        mRegion.set(rect2);
        assertFalse(mRegion.op(40, 40, 60, 60, Region.Op.INTERSECT));
    }

    private void assertUnionOp2(Rect rect2) {
        // UNION, Region with rectangle
        // union (inclusive-or) the two regions
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // union null rectangle
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(0, 0, 0, 0, Region.Op.UNION));
        assertTrue(mRegion.contains(6, 6));

        // 1. union rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(5, 5, 10, 10, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH1);
        assertPointsOutsideRegion(UNION_WITHOUT1);

        // 2. union rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(10, 10, 30, 30, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH2);
        assertPointsOutsideRegion(UNION_WITHOUT2);

        // 3. union rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(40, 40, 60, 60, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH3);
        assertPointsOutsideRegion(UNION_WITHOUT3);
    }

    private void assertXorOp2(Rect rect2) {
        // XOR, Region with rectangle
        // exclusive-or the two regions
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // xor null rectangle
        assertTrue(mRegion.op(0, 0, 0, 0, Region.Op.XOR));

        // 1. xor rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(5, 5, 10, 10, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH1);
        assertPointsOutsideRegion(XOR_WITHOUT1);

        // 2. xor rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(11, 11));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(10, 10, 30, 30, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH2);
        assertPointsOutsideRegion(XOR_WITHOUT2);

        // 3. xor rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(40, 40, 60, 60, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH3);
        assertPointsOutsideRegion(XOR_WITHOUT3);
    }

    private void assertReverseDifferenceOp2(Rect rect2) {
        // REVERSE_DIFFERENCE, Region with rectangle
        // reverse difference the first region from the op region
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // reverse difference null rectangle
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.REVERSE_DIFFERENCE));
        // reverse difference rectangle inside this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertFalse(mRegion.op(5, 5, 10, 10, Region.Op.REVERSE_DIFFERENCE));
        // reverse difference rectangle overlap this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(11, 11));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(10, 10, 30, 30, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH2);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT2);
        // reverse difference rectangle out of this region
        mRegion.set(rect2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(40, 40, 60, 60, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH3);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT3);
    }

    private void assertReplaceOp2(Rect rect2, Rect rect3, Rect rect4, Rect rect5) {
        // REPLACE, Region w1ith rectangle
        // replace the dst region with the op region
        mRegion = null;
        mRegion = new Region();
        mRegion.set(rect2);
        // subtract null rectangle
        assertFalse(mRegion.op(0, 0, 0, 0, Region.Op.REPLACE));
        // subtract rectangle inside this region
        mRegion.set(rect2);
        assertEquals(rect2, mRegion.getBounds());
        assertTrue(mRegion.op(5, 5, 10, 10, Region.Op.REPLACE));
        assertNotSame(rect2, mRegion.getBounds());
        assertEquals(rect3, mRegion.getBounds());
        // subtract rectangle overlap this region
        mRegion.set(rect2);
        assertEquals(rect2, mRegion.getBounds());
        assertTrue(mRegion.op(10, 10, 30, 30, Region.Op.REPLACE));
        assertNotSame(rect2, mRegion.getBounds());
        assertEquals(rect4, mRegion.getBounds());
        // subtract rectangle out of this region
        mRegion.set(rect2);
        assertEquals(rect2, mRegion.getBounds());
        assertTrue(mRegion.op(40, 40, 60, 60, Region.Op.REPLACE));
        assertNotSame(rect2, mRegion.getBounds());
        assertEquals(rect5, mRegion.getBounds());
    }

    public void testOp3() {

        Region region1 = new Region();
        Region region2 = new Region(0, 0, 20, 20);
        Region region3 = new Region(5, 5, 10, 10);
        Region region4 = new Region(10, 10, 30, 30);
        Region region5 = new Region(40, 40, 60, 60);

        assertNullRegionOp3(region1);
        assertDifferenceOp3(region1, region2, region3, region4, region5);
        assertIntersectOp3(region1, region2, region3, region4, region5);
        assertUnionOp3(region1, region2, region3, region4, region5);
        assertXorOp3(region1, region2, region3, region4, region5);
        assertReverseDifferenceOp3(region1, region2, region3, region4, region5);
        assertReplaceOp3(region1, region2, region3, region4, region5);
    }

    private void assertNullRegionOp3(Region region1) {
        // Region without rectangle
        mRegion = null;
        mRegion = new Region();
        assertFalse(mRegion.op(region1, Region.Op.DIFFERENCE));
        assertFalse(mRegion.op(region1, Region.Op.INTERSECT));
        assertFalse(mRegion.op(region1, Region.Op.UNION));
        assertFalse(mRegion.op(region1, Region.Op.XOR));
        assertFalse(mRegion.op(region1, Region.Op.REVERSE_DIFFERENCE));
        assertFalse(mRegion.op(region1, Region.Op.REPLACE));
    }

    private void assertDifferenceOp3(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // DIFFERENCE, Region with rectangle
        // subtract the op region from the first region
        mRegion = null;
        mRegion = new Region();
        // subtract null rectangle
        mRegion.set(region2);
        assertTrue(mRegion.op(region1, Region.Op.DIFFERENCE));

        // 1. subtract rectangle inside this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(region3, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH1);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT1);

        // 2. subtract rectangle overlap this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(11, 11));
        assertTrue(mRegion.op(region4, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH2);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT2);

        // 3. subtract rectangle out of this region
        mRegion.set(region2);
        assertTrue(mRegion.op(region5, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH3);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT3);
    }

    private void assertIntersectOp3(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // INTERSECT, Region with rectangle
        // intersect the two regions
        mRegion = null;
        mRegion = new Region();
        mRegion.set(region2);
        // intersect null rectangle
        assertFalse(mRegion.op(region1, Region.Op.INTERSECT));

        // 1. intersect rectangle inside this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.op(region3, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH1);
        assertPointsOutsideRegion(INTERSECT_WITHOUT1);

        // 2. intersect rectangle overlap this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(9, 9));
        assertTrue(mRegion.op(region4, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH2);
        assertPointsOutsideRegion(INTERSECT_WITHOUT2);

        // 3. intersect rectangle out of this region
        mRegion.set(region2);
        assertFalse(mRegion.op(region5, Region.Op.INTERSECT));
    }

    private void assertUnionOp3(Region region1, Region region2, Region region3,
            Region region4, Region region5) {
        // UNION, Region with rectangle
        // union (inclusive-or) the two regions
        mRegion = null;
        mRegion = new Region();
        // union null rectangle
        mRegion.set(region2);
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(region1, Region.Op.UNION));
        assertTrue(mRegion.contains(6, 6));

        // 1. union rectangle inside this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(region3, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH1);
        assertPointsOutsideRegion(UNION_WITHOUT1);

        // 2. union rectangle overlap this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(region4, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH2);
        assertPointsOutsideRegion(UNION_WITHOUT2);

        // 3. union rectangle out of this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(region5, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH3);
        assertPointsOutsideRegion(UNION_WITHOUT3);
    }

    private void assertXorOp3(Region region1, Region region2, Region region3,
            Region region4, Region region5) {
        // XOR, Region with rectangle
        // exclusive-or the two regions
        mRegion = null;
        mRegion = new Region();
        // xor null rectangle
        mRegion.set(region2);
        assertTrue(mRegion.op(region1, Region.Op.XOR));

        // 1. xor rectangle inside this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertTrue(mRegion.op(region3, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH1);
        assertPointsOutsideRegion(XOR_WITHOUT1);

        // 2. xor rectangle overlap this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(11, 11));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(region4, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH2);
        assertPointsOutsideRegion(XOR_WITHOUT2);

        // 3. xor rectangle out of this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(region5, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH3);
        assertPointsOutsideRegion(XOR_WITHOUT3);
    }

    private void assertReverseDifferenceOp3(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // REVERSE_DIFFERENCE, Region with rectangle
        // reverse difference the first region from the op region
        mRegion = null;
        mRegion = new Region();
        // reverse difference null rectangle
        mRegion.set(region2);
        assertFalse(mRegion.op(region1, Region.Op.REVERSE_DIFFERENCE));

        // 1. reverse difference rectangle inside this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(6, 6));
        assertFalse(mRegion.op(region3, Region.Op.REVERSE_DIFFERENCE));

        // 2. reverse difference rectangle overlap this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertTrue(mRegion.contains(11, 11));
        assertFalse(mRegion.contains(21, 21));
        assertTrue(mRegion.op(region4, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH2);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT2);

        // 3. reverse difference rectangle out of this region
        mRegion.set(region2);
        assertTrue(mRegion.contains(2, 2));
        assertFalse(mRegion.contains(41, 41));
        assertTrue(mRegion.op(region5, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH3);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT3);
    }

    private void assertReplaceOp3(Region region1, Region region2, Region region3,
            Region region4, Region region5) {
        // REPLACE, Region with rectangle
        // replace the dst region with the op region
        mRegion = null;
        mRegion = new Region();
        mRegion.set(region2);
        // subtract null rectangle
        assertFalse(mRegion.op(region1, Region.Op.REPLACE));
        // subtract rectangle inside this region
        mRegion.set(region2);
        assertEquals(region2.getBounds(), mRegion.getBounds());
        assertTrue(mRegion.op(region3, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region3.getBounds(), mRegion.getBounds());
        // subtract rectangle overlap this region
        mRegion.set(region2);
        assertEquals(region2.getBounds(), mRegion.getBounds());
        assertTrue(mRegion.op(region4, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region4.getBounds(), mRegion.getBounds());
        // subtract rectangle out of this region
        mRegion.set(region2);
        assertEquals(region2.getBounds(), mRegion.getBounds());
        assertTrue(mRegion.op(region5, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region5.getBounds(), mRegion.getBounds());
    }

    public void testOp4() {

        Rect rect1 = new Rect();
        Rect rect2 = new Rect(0, 0, 20, 20);

        Region region1 = new Region();
        Region region2 = new Region(0, 0, 20, 20);
        Region region3 = new Region(5, 5, 10, 10);
        Region region4 = new Region(10, 10, 30, 30);
        Region region5 = new Region(40, 40, 60, 60);

        assertNullRegionOp4(rect1, region1);
        assertDifferenceOp4(rect1, rect2, region1, region3, region4, region5);
        assertIntersectOp4(rect1, rect2, region1, region3, region4, region5);
        assertUnionOp4(rect1, rect2, region1, region3, region4, region5);
        assertXorOp4(rect1, rect2, region1, region3, region4, region5);
        assertReverseDifferenceOp4(rect1, rect2, region1, region3, region4,
                region5);
        assertReplaceOp4(rect1, rect2, region1, region2, region3, region4,
                region5);
    }

    private void assertNullRegionOp4(Rect rect1, Region region1) {
        // Region without rectangle
        mRegion = null;
        mRegion = new Region();
        assertFalse(mRegion.op(rect1, region1, Region.Op.DIFFERENCE));
        assertFalse(mRegion.op(rect1, region1, Region.Op.INTERSECT));
        assertFalse(mRegion.op(rect1, region1, Region.Op.UNION));

        assertFalse(mRegion.op(rect1, region1, Region.Op.XOR));
        assertFalse(mRegion.op(rect1, region1, Region.Op.REVERSE_DIFFERENCE));
        assertFalse(mRegion.op(rect1, region1, Region.Op.REPLACE));
    }

    private void assertDifferenceOp4(Rect rect1, Rect rect2, Region region1,
            Region region3, Region region4, Region region5) {
        // DIFFERENCE, Region with rectangle
        // subtract the op region from the first region
        mRegion = null;
        mRegion = new Region();
        // subtract null rectangle
        assertTrue(mRegion.op(rect2, region1, Region.Op.DIFFERENCE));

        // 1. subtract rectangle inside this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region3, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH1);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT1);

        // 2. subtract rectangle overlap this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region4, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH2);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT2);

        // 3. subtract rectangle out of this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region5, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH3);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT3);
    }

    private void assertIntersectOp4(Rect rect1, Rect rect2, Region region1,
            Region region3, Region region4, Region region5) {
        // INTERSECT, Region with rectangle
        // intersect the two regions
        mRegion = null;
        mRegion = new Region();
        // intersect null rectangle
        mRegion.set(rect1);
        assertFalse(mRegion.op(rect2, region1, Region.Op.INTERSECT));

        // 1. intersect rectangle inside this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region3, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH1);
        assertPointsOutsideRegion(INTERSECT_WITHOUT1);

        // 2. intersect rectangle overlap this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region4, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH2);
        assertPointsOutsideRegion(INTERSECT_WITHOUT2);

        // 3. intersect rectangle out of this region
        mRegion.set(rect1);
        assertFalse(mRegion.op(rect2, region5, Region.Op.INTERSECT));
    }

    private void assertUnionOp4(Rect rect1, Rect rect2, Region region1,
            Region region3, Region region4, Region region5) {
        // UNION, Region with rectangle
        // union (inclusive-or) the two regions
        mRegion = null;
        mRegion = new Region();
        // union null rectangle
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region1, Region.Op.UNION));
        assertTrue(mRegion.contains(6, 6));

        // 1. union rectangle inside this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region3, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH1);
        assertPointsOutsideRegion(UNION_WITHOUT1);

        // 2. union rectangle overlap this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region4, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH2);
        assertPointsOutsideRegion(UNION_WITHOUT2);

        // 3. union rectangle out of this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region5, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH3);
        assertPointsOutsideRegion(UNION_WITHOUT3);
    }

    private void assertXorOp4(Rect rect1, Rect rect2, Region region1,
            Region region3, Region region4, Region region5) {
        // XOR, Region with rectangle
        // exclusive-or the two regions
        mRegion = null;
        mRegion = new Region();
        // xor null rectangle
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region1, Region.Op.XOR));

        // 1. xor rectangle inside this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region3, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH1);
        assertPointsOutsideRegion(XOR_WITHOUT1);

        // 2. xor rectangle overlap this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region4, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH2);
        assertPointsOutsideRegion(XOR_WITHOUT2);

        // 3. xor rectangle out of this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region5, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH3);
        assertPointsOutsideRegion(XOR_WITHOUT3);
    }

    private void assertReverseDifferenceOp4(Rect rect1, Rect rect2,
            Region region1, Region region3, Region region4, Region region5) {
        // REVERSE_DIFFERENCE, Region with rectangle
        // reverse difference the first region from the op region
        mRegion = null;
        mRegion = new Region();
        // reverse difference null rectangle
        mRegion.set(rect1);
        assertFalse(mRegion.op(rect2, region1, Region.Op.REVERSE_DIFFERENCE));

        // 1. reverse difference rectangle inside this region
        mRegion.set(rect1);
        assertFalse(mRegion.op(rect2, region3, Region.Op.REVERSE_DIFFERENCE));

        // 2. reverse difference rectangle overlap this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region4, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH2);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT2);

        // 3. reverse difference rectangle out of this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region5, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH3);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT3);
    }

    private void assertReplaceOp4(Rect rect1, Rect rect2, Region region1,
            Region region2, Region region3, Region region4, Region region5) {
        // REPLACE, Region with rectangle
        // replace the dst region with the op region
        mRegion = null;
        mRegion = new Region();
        // subtract null rectangle
        mRegion.set(rect1);
        assertFalse(mRegion.op(rect2, region1, Region.Op.REPLACE));
        // subtract rectangle inside this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region3, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region3.getBounds(), mRegion.getBounds());
        // subtract rectangle overlap this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region4, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region4.getBounds(), mRegion.getBounds());
        // subtract rectangle out of this region
        mRegion.set(rect1);
        assertTrue(mRegion.op(rect2, region5, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region5.getBounds(), mRegion.getBounds());
    }

    public void testOp5() {

        Region region1 = new Region();
        Region region2 = new Region(0, 0, 20, 20);
        Region region3 = new Region(5, 5, 10, 10);
        Region region4 = new Region(10, 10, 30, 30);
        Region region5 = new Region(40, 40, 60, 60);

        assertNullRegionOp5(region1);
        assertDifferenceOp5(region1, region2, region3, region4, region5);
        assertIntersectOp5(region1, region2, region3, region4, region5);
        assertUnionOp5(region1, region2, region3, region4, region5);
        assertXorOp5(region1, region2, region3, region4, region5);
        assertReverseDifferenceOp5(region1, region2, region3, region4, region5);
        assertReplaceOp5(region1, region2, region3, region4, region5);
    }

    private void assertNullRegionOp5(Region region1) {
        // Region without rectangle
        mRegion = null;
        mRegion = new Region();
        assertFalse(mRegion.op(mRegion, region1, Region.Op.DIFFERENCE));
        assertFalse(mRegion.op(mRegion, region1, Region.Op.INTERSECT));
        assertFalse(mRegion.op(mRegion, region1, Region.Op.UNION));
        assertFalse(mRegion.op(mRegion, region1, Region.Op.XOR));
        assertFalse(mRegion.op(mRegion, region1, Region.Op.REVERSE_DIFFERENCE));
        assertFalse(mRegion.op(mRegion, region1, Region.Op.REPLACE));
    }

    private void assertDifferenceOp5(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // DIFFERENCE, Region with rectangle
        // subtract the op region from the first region
        mRegion = null;
        mRegion = new Region();
        // subtract null rectangle
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region1, Region.Op.DIFFERENCE));

        // 1. subtract rectangle inside this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region3, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH1);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT1);

        // 2. subtract rectangle overlap this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region4, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH2);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT2);

        // 3. subtract rectangle out of this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region5, Region.Op.DIFFERENCE));
        assertPointsInsideRegion(DIFFERENCE_WITH3);
        assertPointsOutsideRegion(DIFFERENCE_WITHOUT3);
    }

    private void assertIntersectOp5(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // INTERSECT, Region with rectangle
        // intersect the two regions
        mRegion = null;
        mRegion = new Region();
        // intersect null rectangle
        mRegion.set(region1);
        assertFalse(mRegion.op(region2, region1, Region.Op.INTERSECT));

        // 1. intersect rectangle inside this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region3, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH1);
        assertPointsOutsideRegion(INTERSECT_WITHOUT1);

        // 2. intersect rectangle overlap this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region4, Region.Op.INTERSECT));
        assertPointsInsideRegion(INTERSECT_WITH2);
        assertPointsOutsideRegion(INTERSECT_WITHOUT2);

        // 3. intersect rectangle out of this region
        mRegion.set(region1);
        assertFalse(mRegion.op(region2, region5, Region.Op.INTERSECT));
    }

    private void assertUnionOp5(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // UNION, Region with rectangle
        // union (inclusive-or) the two regions
        mRegion = null;
        mRegion = new Region();
        // union null rectangle
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region1, Region.Op.UNION));
        assertTrue(mRegion.contains(6, 6));

        // 1. union rectangle inside this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region3, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH1);
        assertPointsOutsideRegion(UNION_WITHOUT1);

        // 2. union rectangle overlap this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region4, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH2);
        assertPointsOutsideRegion(UNION_WITHOUT2);

        // 3. union rectangle out of this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region5, Region.Op.UNION));
        assertPointsInsideRegion(UNION_WITH3);
        assertPointsOutsideRegion(UNION_WITHOUT3);
    }

    private void assertXorOp5(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // XOR, Region with rectangle
        // exclusive-or the two regions
        mRegion = null;
        mRegion = new Region();
        // xor null rectangle
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region1, Region.Op.XOR));

        // 1. xor rectangle inside this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region3, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH1);
        assertPointsOutsideRegion(XOR_WITHOUT1);

        // 2. xor rectangle overlap this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region4, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH2);
        assertPointsOutsideRegion(XOR_WITHOUT2);

        // 3. xor rectangle out of this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region5, Region.Op.XOR));
        assertPointsInsideRegion(XOR_WITH3);
        assertPointsOutsideRegion(XOR_WITHOUT3);
    }

    private void assertReverseDifferenceOp5(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // REVERSE_DIFFERENCE, Region with rectangle
        // reverse difference the first region from the op region
        mRegion = null;
        mRegion = new Region();
        // reverse difference null rectangle
        mRegion.set(region1);
        assertFalse(mRegion.op(region2, region1, Region.Op.REVERSE_DIFFERENCE));

        // 1. reverse difference rectangle inside this region
        mRegion.set(region1);
        assertFalse(mRegion.op(region2, region3, Region.Op.REVERSE_DIFFERENCE));

        // 2. reverse difference rectangle overlap this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region4, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH2);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT2);

        // 3. reverse difference rectangle out of this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region5, Region.Op.REVERSE_DIFFERENCE));
        assertPointsInsideRegion(REVERSE_DIFFERENCE_WITH3);
        assertPointsOutsideRegion(REVERSE_DIFFERENCE_WITHOUT3);
    }

    private void assertReplaceOp5(Region region1, Region region2,
            Region region3, Region region4, Region region5) {
        // REPLACE, Region with rectangle
        // replace the dst region with the op region
        mRegion = null;
        mRegion = new Region();
        // subtract null rectangle
        mRegion.set(region1);
        assertFalse(mRegion.op(region2, region1, Region.Op.REPLACE));
        // subtract rectangle inside this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region3, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region3.getBounds(), mRegion.getBounds());
        // subtract rectangle overlap this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region4, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region4.getBounds(), mRegion.getBounds());
        // subtract rectangle out of this region
        mRegion.set(region1);
        assertTrue(mRegion.op(region2, region5, Region.Op.REPLACE));
        assertNotSame(region2.getBounds(), mRegion.getBounds());
        assertEquals(region5.getBounds(), mRegion.getBounds());
    }

    public void testGetBoundaryPath1() {
        mRegion = new Region();
        assertTrue(mRegion.getBoundaryPath().isEmpty());

        // Both clip and path are non-null.
        Region clip = new Region(0, 0, 10, 10);
        Path path = new Path();
        path.addRect(0, 0, 10, 10, Path.Direction.CW);
        assertTrue(mRegion.setPath(path, clip));
        assertFalse(mRegion.getBoundaryPath().isEmpty());
    }

    public void testGetBoundaryPath2() {

        mRegion = new Region();
        Path path = new Path();
        assertFalse(mRegion.getBoundaryPath(path));

        // path is null
        mRegion = new Region(0, 0, 10, 10);
        path = new Path();
        assertTrue(mRegion.getBoundaryPath(path));

        // region is null
        mRegion = new Region();
        path = new Path();
        path.addRect(0, 0, 10, 10, Path.Direction.CW);
        assertFalse(mRegion.getBoundaryPath(path));

        // both path and region are non-null
        mRegion = new Region(0, 0, 10, 10);
        path = new Path();
        path.addRect(0, 0, 5, 5, Path.Direction.CW);
        assertTrue(mRegion.getBoundaryPath(path));
    }

    public void testSetPath() {

        mRegion = new Region();
        // Both clip and path are null.
        Region clip = new Region();
        Path path = new Path();
        assertFalse(mRegion.setPath(path, clip));

        // Only path is null.
        path = new Path();
        clip = new Region(0, 0, 10, 10);
        assertFalse(mRegion.setPath(path, clip));

        // Only clip is null.
        clip = new Region();
        path = new Path();
        path.addRect(0, 0, 10, 10, Path.Direction.CW);
        assertFalse(mRegion.setPath(path, clip));

        // Both clip and path are non-null.
        clip = new Region();
        path = new Path();
        clip = new Region(0, 0, 10, 10);
        path.addRect(0, 0, 10, 10, Path.Direction.CW);
        assertTrue(mRegion.setPath(path, clip));

        // Both clip and path are non-null.
        path = new Path();
        clip = new Region(0, 0, 5, 5);
        path.addRect(0, 0, 10, 10, Path.Direction.CW);
        assertTrue(mRegion.setPath(path, clip));
        Rect expected = new Rect(0, 0, 5, 5);
        Rect unexpected = new Rect(0, 0, 10, 10);
        Rect actual = mRegion.getBounds();
        assertEquals(expected.right, actual.right);
        assertNotSame(unexpected.right, actual.right);

        // Both clip and path are non-null.
        path = new Path();
        clip = new Region(0, 0, 10, 10);
        path.addRect(0, 0, 5, 5, Path.Direction.CW);
        assertTrue(mRegion.setPath(path, clip));
        expected = new Rect(0, 0, 5, 5);
        unexpected = new Rect(0, 0, 10, 10);
        actual = mRegion.getBounds();
        assertEquals(expected.right, actual.right);
        assertNotSame(unexpected.right, actual.right);
    }

    public void testTranslate1() {

        Rect rect1 = new Rect(0, 0, 20, 20);
        Rect rect2 = new Rect(10, 10, 30, 30);
        mRegion = new Region(0, 0, 20, 20);
        mRegion.translate(10, 10);
        assertNotSame(rect1, mRegion.getBounds());
        assertEquals(rect2, mRegion.getBounds());
    }

    public void testTranslate2() {

        Region dst = new Region();
        Rect rect1 = new Rect(0, 0, 20, 20);
        Rect rect2 = new Rect(10, 10, 30, 30);
        mRegion = new Region(0, 0, 20, 20);
        mRegion.translate(10, 10, dst);
        assertEquals(rect1, mRegion.getBounds());
        assertNotSame(rect2, mRegion.getBounds());
        assertNotSame(rect1, dst.getBounds());
        assertEquals(rect2, dst.getBounds());
    }

    public void testWriteToParcel() {

        int flags = 0;
        Rect oriRect = new Rect(0, 0, 10, 10);
        mRegion = new Region();

        Parcel p = Parcel.obtain();
        mRegion.writeToParcel(p, flags);
        assertEquals(8, p.dataSize());

        p = Parcel.obtain();
        mRegion.set(oriRect);
        mRegion.writeToParcel(p, flags);
        assertEquals(24, p.dataSize());

        p.setDataPosition(0);
        Region dst = Region.CREATOR.createFromParcel(p);
        assertEquals(oriRect.top, dst.getBounds().top);
        assertEquals(oriRect.left, dst.getBounds().left);
        assertEquals(oriRect.bottom, dst.getBounds().bottom);
        assertEquals(oriRect.right, dst.getBounds().right);
    }

    public void testDescribeContents() {

        mRegion = new Region();
        int actual = mRegion.describeContents();
        assertEquals(0, actual);
    }

    public void testQuickReject1() {
        Rect oriRect = new Rect(0, 0, 20, 20);
        Rect rect1 = new Rect();
        Rect rect2 = new Rect(40, 40, 60, 60);
        Rect rect3 = new Rect(0, 0, 10, 10);
        Rect rect4 = new Rect(10, 10, 30, 30);

        mRegion = new Region();
        // Return true if the region is empty
        assertTrue(mRegion.quickReject(rect1));
        mRegion.set(oriRect);
        assertTrue(mRegion.quickReject(rect2));
        mRegion.set(oriRect);
        assertFalse(mRegion.quickReject(rect3));
        mRegion.set(oriRect);
        assertFalse(mRegion.quickReject(rect4));
    }

    public void testQuickReject2() {
        mRegion = new Region();
        // Return true if the region is empty
        assertTrue(mRegion.quickReject(0, 0, 0, 0));
        mRegion.set(0, 0, 20, 20);
        assertTrue(mRegion.quickReject(40, 40, 60, 60));
        mRegion.set(0, 0, 20, 20);
        assertFalse(mRegion.quickReject(0, 0, 10, 10));
        mRegion.set(0, 0, 20, 20);
        assertFalse(mRegion.quickReject(10, 10, 30, 30));
    }

    public void testQuickReject3() {
        Region oriRegion = new Region(0, 0, 20, 20);
        Region region1 = new Region();
        Region region2 = new Region(40, 40, 60, 60);
        Region region3 = new Region(0, 0, 10, 10);
        Region region4 = new Region(10, 10, 30, 30);

        mRegion = new Region();
        // Return true if the region is empty
        assertTrue(mRegion.quickReject(region1));
        mRegion.set(oriRegion);
        assertTrue(mRegion.quickReject(region2));
        mRegion.set(oriRegion);
        assertFalse(mRegion.quickReject(region3));
        mRegion.set(oriRegion);
        assertFalse(mRegion.quickReject(region4));
    }

}

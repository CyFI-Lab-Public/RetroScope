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
import android.os.Parcel;
import android.test.AndroidTestCase;

public class RectTest extends AndroidTestCase {
    private Rect mRect;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRect = null;
    }

    public void testConstructor() {

        mRect = null;
        // new the Rect instance
        mRect = new Rect();

        mRect = null;
        // new the Rect instance
        mRect = new Rect(10, 10, 20, 20);

        mRect = null;
        Rect rect = new Rect(10, 10, 20, 20);
        // new the Rect instance
        mRect = new Rect(rect);

    }

    public void testSet1() {

        mRect = new Rect();
        mRect.set(1, 2, 3, 4);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.top);
        assertEquals(3, mRect.right);
        assertEquals(4, mRect.bottom);

    }

    public void testSet2() {

        Rect rect = new Rect(1, 2, 3, 4);
        mRect = new Rect();
        mRect.set(rect);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.top);
        assertEquals(3, mRect.right);
        assertEquals(4, mRect.bottom);
    }

    public void testIntersects1() {

        mRect = new Rect(0, 0, 10, 10);
        assertTrue(mRect.intersects(5, 5, 15, 15));
        assertEquals(0, mRect.left);
        assertEquals(0, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);

        mRect = new Rect(0, 0, 10, 10);
        assertFalse(mRect.intersects(15, 15, 25, 25));
        assertEquals(0, mRect.left);
        assertEquals(0, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);

    }

    public void testIntersects2() {

        Rect rect1;
        Rect rect2;

        rect1 = new Rect(0, 0, 10, 10);
        rect2 = new Rect(5, 5, 15, 15);
        assertTrue(Rect.intersects(rect1, rect2));

        rect1 = new Rect(0, 0, 10, 10);
        rect2 = new Rect(15, 15, 25, 25);
        assertFalse(Rect.intersects(rect1, rect2));

    }

    public void testHeight() {
        mRect = new Rect(6, 6, 10, 10);
        assertEquals(4, mRect.height());
    }

    public void testOffsetTo() {

        mRect = new Rect(5, 5, 10, 10);
        mRect.offsetTo(1, 1);
        assertEquals(1, mRect.left);
        assertEquals(1, mRect.top);
        assertEquals(6, mRect.right);
        assertEquals(6, mRect.bottom);

    }

    public void testSetIntersect() {

        Rect rect1 = new Rect(0, 0, 10, 10);
        Rect rect2 = new Rect(5, 5, 15, 15);

        // Empty Rectangle
        mRect = new Rect();
        assertTrue(mRect.setIntersect(rect1, rect2));
        assertEquals(5, mRect.left);
        assertEquals(5, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);

        // Not Empty Rectangle
        mRect = new Rect(0, 0, 15, 15);
        assertTrue(mRect.setIntersect(rect1, rect2));
        assertEquals(5, mRect.left);
        assertEquals(5, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);

    }

    public void testUnion1() {

        // Both rect1 and rect2 are not empty.
        // 1. left < right, top < bottom
        // this.left < this.right, this.top < this.bottom
        mRect = new Rect(0, 0, 1, 1);
        mRect.union(1, 1, 2, 2);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // 2. left < right, top < bottom
        // this.left > this.right, this.top > this.bottom
        // New rectangle will be set to the new arguments
        mRect = new Rect(1, 1, 0, 0);
        mRect.union(1, 1, 2, 2);
        assertEquals(1, mRect.top);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // 3. left > right, top > bottom
        // this.left < this.right, this.top < this.bottom
        // Nothing will be done.
        mRect = new Rect(0, 0, 1, 1);
        mRect.union(2, 2, 1, 1);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(1, mRect.right);
        assertEquals(1, mRect.bottom);

        // rect1 is empty, update to rect2.
        mRect = new Rect();
        mRect.union(1, 1, 2, 2);
        assertEquals(1, mRect.top);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // rect2 is empty, nothing changed.
        mRect = new Rect(0, 0, 1, 1);
        mRect.union(2, 2, 2, 2);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(1, mRect.right);
        assertEquals(1, mRect.bottom);
    }

    public void testUnion2() {

        Rect rect;

        // Both rect1 and rect2 are not empty.
        // 1. left < right, top < bottom
        // this.left < this.right, this.top < this.bottom
        mRect = new Rect(0, 0, 1, 1);
        rect = new Rect(1, 1, 2, 2);
        mRect.union(rect);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // 2. left < right, top < bottom
        // this.left > this.right, this.top > this.bottom
        // New rectangle will be set to the new arguments
        mRect = new Rect(1, 1, 0, 0);
        rect = new Rect(1, 1, 2, 2);
        mRect.union(rect);
        assertEquals(1, mRect.top);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // 3. left > right, top > bottom
        // this.left < this.right, this.top < this.bottom
        // Nothing will be done.
        mRect = new Rect(0, 0, 1, 1);
        rect = new Rect(2, 2, 1, 1);
        mRect.union(rect);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(1, mRect.right);
        assertEquals(1, mRect.bottom);

        // rect1 is empty, update to rect2.
        mRect = new Rect();
        rect = new Rect(1, 1, 2, 2);
        mRect.union(rect);
        assertEquals(1, mRect.top);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // rect2 is empty, nothing changed.
        mRect = new Rect(0, 0, 1, 1);
        rect = new Rect(2, 2, 2, 2);
        mRect.union(rect);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(1, mRect.right);
        assertEquals(1, mRect.bottom);
    }

    public void testUnion3() {

        // rect1 is not empty (x > right, y > bottom).
        mRect = new Rect(0, 0, 1, 1);
        mRect.union(2, 2);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // rect1 is not empty (x < left, y < top).
        mRect = new Rect(1, 1, 2, 2);
        mRect.union(0, 0);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // rect1 is not empty(point is inside of the rectangle).
        mRect = new Rect(1, 1, 2, 2);
        mRect.union(1, 1);
        assertEquals(1, mRect.top);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

        // rect1 is empty.
        mRect = new Rect();
        mRect.union(2, 2);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.left);
        assertEquals(2, mRect.right);
        assertEquals(2, mRect.bottom);

    }

    public void testContains1() {

        mRect = new Rect(1, 1, 20, 20);
        assertFalse(mRect.contains(0, 0));
        assertTrue(mRect.contains(1, 1));
        assertTrue(mRect.contains(19, 19));
        assertFalse(mRect.contains(20, 20));

    }

    public void testContains2() {

        mRect = new Rect(1, 1, 20, 20);
        assertTrue(mRect.contains(1, 1, 20, 20));
        assertTrue(mRect.contains(2, 2, 19, 19));
        assertFalse(mRect.contains(21, 21, 22, 22));
        assertFalse(mRect.contains(0, 0, 19, 19));

    }

    public void testContains3() {

        Rect rect;
        mRect = new Rect(1, 1, 20, 20);
        rect = new Rect(1, 1, 20, 20);
        assertTrue(mRect.contains(rect));
        rect = new Rect(2, 2, 19, 19);
        assertTrue(mRect.contains(rect));
        rect = new Rect(21, 21, 22, 22);
        assertFalse(mRect.contains(rect));
        rect = new Rect(0, 0, 19, 19);
        assertFalse(mRect.contains(rect));
    }

    public void testWidth() {
        mRect = new Rect(6, 6, 10, 10);
        assertEquals(4, mRect.width());
    }

    public void testIsEmpty() {

        mRect = new Rect();
        assertTrue(mRect.isEmpty());
        mRect = new Rect(1, 1, 1, 1);
        assertTrue(mRect.isEmpty());
        mRect = new Rect(0, 1, 2, 1);
        assertTrue(mRect.isEmpty());
        mRect = new Rect(1, 1, 20, 20);
        assertFalse(mRect.isEmpty());
    }

    public void testIntersect1() {

        mRect = new Rect(0, 0, 10, 10);
        assertTrue(mRect.intersect(5, 5, 15, 15));
        assertEquals(5, mRect.left);
        assertEquals(5, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);

        mRect = new Rect(0, 0, 10, 10);
        assertFalse(mRect.intersect(15, 15, 25, 25));
        assertEquals(0, mRect.left);
        assertEquals(0, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);
    }

    public void testIntersect2() {

        Rect rect;

        mRect = new Rect(0, 0, 10, 10);
        rect= new Rect(5, 5, 15, 15);
        assertTrue(mRect.intersect(rect));
        assertEquals(5, mRect.left);
        assertEquals(5, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);

        mRect = new Rect(0, 0, 10, 10);
        rect= new Rect(15, 15, 25, 25);
        assertFalse(mRect.intersect(rect));
        assertEquals(0, mRect.left);
        assertEquals(0, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);
    }

    public void testCenterY() {

        mRect = new Rect(10, 10, 20, 20);
        assertEquals(15, mRect.centerY());
        mRect = new Rect(10, 11, 20, 20);
        assertEquals(15, mRect.centerY());
        mRect = new Rect(10, 12, 20, 20);
        assertEquals(16, mRect.centerY());
    }

    public void testToString() {
        mRect = new Rect();
        assertNotNull(mRect.toString());
        assertNotNull(mRect.toShortString());

        mRect = new Rect(1, 2, 3, 4);
        assertNotNull(mRect.toString());
        assertNotNull(mRect.toShortString());
    }

    public void testSort() {

        mRect = new Rect(10, 10, 5, 5);
        assertEquals(10, mRect.left);
        assertEquals(10, mRect.top);
        assertEquals(5, mRect.right);
        assertEquals(5, mRect.bottom);

        mRect.sort();
        assertEquals(5, mRect.left);
        assertEquals(5, mRect.top);
        assertEquals(10, mRect.right);
        assertEquals(10, mRect.bottom);

    }

    public void testCenterX() {

        mRect = new Rect(10, 10, 20, 20);
        assertEquals(15, mRect.centerX());
        mRect = new Rect(11, 10, 20, 20);
        assertEquals(15, mRect.centerX());
        mRect = new Rect(12, 10, 20, 20);
        assertEquals(16, mRect.centerX());

    }

    public void testEquals() {

        mRect = new Rect(1, 2, 3, 4);
        Rect rect = new Rect(1, 2, 3, 4);
        assertTrue(mRect.equals(rect));
        rect = new Rect(2, 2, 3, 4);
        assertFalse(mRect.equals(rect));

    }

    public void testOffset() {

        mRect = new Rect(5, 5, 10, 10);
        mRect.offset(1, 1);
        assertEquals(6, mRect.left);
        assertEquals(6, mRect.top);
        assertEquals(11, mRect.right);
        assertEquals(11, mRect.bottom);

    }

    public void testInset() {

        mRect = new Rect(5, 5, 10, 10);
        mRect.inset(1, 1);
        assertEquals(6, mRect.left);
        assertEquals(6, mRect.top);
        assertEquals(9, mRect.right);
        assertEquals(9, mRect.bottom);

        mRect = new Rect(5, 5, 10, 10);
        mRect.inset(-1, -1);
        assertEquals(4, mRect.left);
        assertEquals(4, mRect.top);
        assertEquals(11, mRect.right);
        assertEquals(11, mRect.bottom);

    }

    public void testSetEmpty() {

        // Before setEmpty()
        mRect = new Rect(1, 2, 3, 4);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.top);
        assertEquals(3, mRect.right);
        assertEquals(4, mRect.bottom);

        // After setEmpty()
        mRect.setEmpty();
        assertEquals(0, mRect.left);
        assertEquals(0, mRect.top);
        assertEquals(0, mRect.right);
        assertEquals(0, mRect.bottom);

    }

    public void testExactCenterX() {
        mRect = new Rect(11, 10, 20, 20);
        assertEquals(15.5f, mRect.exactCenterX());
    }

    public void testExactCenterY() {
        mRect = new Rect(10, 11, 20, 20);
        assertEquals(15.5f, mRect.exactCenterY());
    }

    public void testAccessParcel() {

        Rect rect;
        Parcel p = Parcel.obtain();
        rect = new Rect(1, 2, 3, 4);
        rect.writeToParcel(p, 0);

        p.setDataPosition(0);
        mRect = new Rect();
        mRect.readFromParcel(p);
        assertEquals(1, mRect.left);
        assertEquals(2, mRect.top);
        assertEquals(3, mRect.right);
        assertEquals(4, mRect.bottom);

    }

    public void testFlattenToString() {
        Rect mRect = new Rect(1, 2, 3, 4);
        String flattenString = mRect.flattenToString();
        assertNotNull(flattenString);
        String unDefinedFormat = "TOPLEFT";
        Rect rect = Rect.unflattenFromString(flattenString);
        assertEquals(mRect, rect);
        rect = null;
        rect = Rect.unflattenFromString(unDefinedFormat);
        assertNull(rect);
    }

    public void testDescribeContents() {
        mRect = new Rect();
        assertEquals(0, mRect.describeContents());
    }

}

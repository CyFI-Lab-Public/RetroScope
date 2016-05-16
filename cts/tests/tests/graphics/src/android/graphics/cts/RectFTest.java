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
import android.graphics.RectF;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class RectFTest extends AndroidTestCase {

    private RectF mRectF;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRectF = null;
    }

    public void testConstructor() {

        mRectF = null;
        // new the RectF instance
        mRectF = new RectF();

        mRectF = null;
        // new the RectF instance
        mRectF = new RectF(1.5f, 2.5f, 20.3f, 40.9f);

        mRectF = null;
        RectF rectF = new RectF(1.5f, 2.5f, 20.3f, 40.9f);
        // new the RectF instance
        mRectF = new RectF(rectF);

        mRectF = null;
        Rect rect = new Rect(0, 0, 10, 10);
        // new the RectF instance
        mRectF = new RectF(rect);

    }

    public void testSort() {

        mRectF = new RectF(10, 10, 5, 5);
        assertEquals(10.0f, mRectF.left);
        assertEquals(10.0f, mRectF.top);
        assertEquals(5.0f, mRectF.right);
        assertEquals(5.0f, mRectF.bottom);

        mRectF.sort();
        assertEquals(5.0f, mRectF.left);
        assertEquals(5.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

    }

    public void testSet1() {

        mRectF = new RectF();
        mRectF.set(1.0f, 2.0f, 3.0f, 4.0f);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.top);
        assertEquals(3.0f, mRectF.right);
        assertEquals(4.0f, mRectF.bottom);

    }

    public void testSet2() {

        RectF rectF = new RectF(1.0f, 2.0f, 3.0f, 4.0f);
        mRectF = new RectF();
        mRectF.set(rectF);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.top);
        assertEquals(3.0f, mRectF.right);
        assertEquals(4.0f, mRectF.bottom);

    }

    public void testSet3() {

        Rect rect = new Rect(1, 2, 3, 4);
        mRectF = new RectF();
        mRectF.set(rect);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.top);
        assertEquals(3.0f, mRectF.right);
        assertEquals(4.0f, mRectF.bottom);

    }

    public void testIntersects1() {

        mRectF = new RectF(0, 0, 10, 10);
        assertTrue(mRectF.intersects(5, 5, 15, 15));
        assertEquals(0.0f, mRectF.left);
        assertEquals(0.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

        mRectF = new RectF(0, 0, 10, 10);
        assertFalse(mRectF.intersects(15, 15, 25, 25));
        assertEquals(0.0f, mRectF.left);
        assertEquals(0.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

    }

    public void testIntersects2() {

        RectF rectF1;
        RectF rectF2;

        rectF1 = new RectF(0, 0, 10, 10);
        rectF2 = new RectF(5, 5, 15, 15);
        assertTrue(RectF.intersects(rectF1, rectF2));

        rectF1 = new RectF(0, 0, 10, 10);
        rectF2 = new RectF(15, 15, 25, 25);
        assertFalse(RectF.intersects(rectF1, rectF2));

    }

    public void testIntersect1() {

        mRectF = new RectF(0, 0, 10, 10);
        assertTrue(mRectF.intersect(5, 5, 15, 15));
        assertEquals(5.0f, mRectF.left);
        assertEquals(5.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

        mRectF = new RectF(0, 0, 10, 10);
        assertFalse(mRectF.intersect(15, 15, 25, 25));
        assertEquals(0.0f, mRectF.left);
        assertEquals(0.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

    }

    public void testIntersect2() {

        RectF rectF;

        mRectF = new RectF(0, 0, 10, 10);
        rectF= new RectF(5, 5, 15, 15);
        assertTrue(mRectF.intersect(rectF));
        assertEquals(5.0f, mRectF.left);
        assertEquals(5.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

        mRectF = new RectF(0, 0, 10, 10);
        rectF= new RectF(15, 15, 25, 25);
        assertFalse(mRectF.intersect(rectF));
        assertEquals(0.0f, mRectF.left);
        assertEquals(0.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

    }

    public void testUnion1() {

        // Both rect1 and rect2 are not empty.
        // 1. left < right, top < bottom
        // this.left < this.right, this.top < this.bottom
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        mRectF.union(1.0f, 1.0f, 2.0f, 2.0f);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // 2. left < right, top < bottom
        // this.left > this.right, this.top > this.bottom
        // New rectangle will be set to the new arguments
        mRectF = new RectF(1.0f, 1.0f, 0.0f, 0.0f);
        mRectF.union(1.0f, 1.0f, 2.0f, 2.0f);
        assertEquals(1.0f, mRectF.top);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // 3. left > right, top > bottom
        // this.left < this.right, this.top < this.bottom
        // Nothing will be done.
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        mRectF.union(2.0f, 2.0f, 1.5f, 1.5f);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(1.0f, mRectF.right);
        assertEquals(1.0f, mRectF.bottom);

        // rect1 is empty, update to rect2.
        mRectF = new RectF();
        mRectF.union(1.0f, 1.0f, 2.0f, 2.0f);
        assertEquals(1.0f, mRectF.top);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // rect2 is empty, nothing changed.
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        mRectF.union(2.0f, 2.0f, 2.0f, 2.0f);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(1.0f, mRectF.right);
        assertEquals(1.0f, mRectF.bottom);

    }

    public void testUnion2() {

        RectF rectF;

        // Both rect1 and rect2 are not empty.
        // 1. left < right, top < bottom
        // this.left < this.right, this.top < this.bottom
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        rectF = new RectF(1.0f, 1.0f, 2.0f, 2.0f);
        mRectF.union(rectF);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // 2. left < right, top < bottom
        // this.left > this.right, this.top > this.bottom
        // New rectangle will be set to the new arguments
        mRectF = new RectF(1.0f, 1.0f, 0.0f, 0.0f);
        rectF = new RectF(1.0f, 1.0f, 2.0f, 2.0f);
        mRectF.union(rectF);
        assertEquals(1.0f, mRectF.top);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // 3. left > right, top > bottom
        // this.left < this.right, this.top < this.bottom
        // Nothing will be done.
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        rectF = new RectF(2.0f, 2.0f, 1.5f, 1.5f);
        mRectF.union(rectF);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(1.0f, mRectF.right);
        assertEquals(1.0f, mRectF.bottom);

        // rect1 is empty, update to rect2.
        mRectF = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        rectF = new RectF(1.0f, 1.0f, 2.0f, 2.0f);
        mRectF.union(rectF);
        assertEquals(1.0f, mRectF.top);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // rect2 is empty, nothing changed.
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        rectF = new RectF(2.0f, 2.0f, 2.0f, 2.0f);
        mRectF.union(rectF);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(1.0f, mRectF.right);
        assertEquals(1.0f, mRectF.bottom);

    }

    public void testUnion3() {

        // rect1 is not empty (x > right, y > bottom).
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        mRectF.union(2.0f, 2.0f);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // rect1 is not empty (x < left, y < top).
        mRectF = new RectF(1.0f, 1.0f, 2.0f, 2.0f);
        mRectF.union(0.0f, 0.0f);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // rect1 is not empty(point is inside of the rectangle).
        mRectF = new RectF(1.0f, 1.0f, 2.0f, 2.0f);
        mRectF.union(1.5f, 1.5f);
        assertEquals(1.0f, mRectF.top);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

        // rect1 is empty.
        mRectF = new RectF();
        mRectF.union(2.0f, 2.0f);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.left);
        assertEquals(2.0f, mRectF.right);
        assertEquals(2.0f, mRectF.bottom);

    }

    public void testContains1() {

        mRectF = new RectF(1.0f, 1.0f, 20.0f, 20.0f);
        assertFalse(mRectF.contains(0.9f, 0.9f));
        assertTrue(mRectF.contains(1.0f, 1.0f));
        assertTrue(mRectF.contains(19.9f, 19.9f));
        assertFalse(mRectF.contains(20.0f, 20.0f));

    }

    public void testContains2() {

        mRectF = new RectF(1.0f, 1.0f, 20.0f, 20.0f);
        assertTrue(mRectF.contains(1.0f, 1.0f, 20.0f, 20.0f));
        assertTrue(mRectF.contains(2.0f, 2.0f, 19.0f, 19.0f));
        assertFalse(mRectF.contains(21.0f, 21.0f, 22.0f, 22.0f));
        assertFalse(mRectF.contains(0.0f, 0.0f, 19.0f, 19.0f));
    }

    public void testContains3() {

        RectF rectF;
        mRectF = new RectF(1.0f, 1.0f, 20.0f, 20.0f);
        rectF = new RectF(1.0f, 1.0f, 20.0f, 20.0f);
        assertTrue(mRectF.contains(rectF));
        rectF = new RectF(2.0f, 2.0f, 19.0f, 19.0f);
        assertTrue(mRectF.contains(rectF));
        rectF = new RectF(21.0f, 21.0f, 22.0f, 22.0f);
        assertFalse(mRectF.contains(rectF));
        rectF = new RectF(0.0f, 0.0f, 19.0f, 19.0f);
        assertFalse(mRectF.contains(rectF));

    }

    public void testOffset() {

       mRectF = new RectF(5, 5, 10, 10);
       mRectF.offset(1.0f, 1.0f);
       assertEquals(6.0f, mRectF.left);
       assertEquals(6.0f, mRectF.top);
       assertEquals(11.0f, mRectF.right);
       assertEquals(11.0f, mRectF.bottom);

    }

    public void testInset() {

        mRectF = new RectF(5.0f, 5.0f, 10.0f, 10.0f);
        mRectF.inset(1.0f, 1.0f);
        assertEquals(6.0f, mRectF.left);
        assertEquals(6.0f, mRectF.top);
        assertEquals(9.0f, mRectF.right);
        assertEquals(9.0f, mRectF.bottom);

        mRectF = new RectF(5.0f, 5.0f, 10.0f, 10.0f);
        mRectF.inset(-1.0f, -1.0f);
        assertEquals(4.0f, mRectF.left);
        assertEquals(4.0f, mRectF.top);
        assertEquals(11.0f, mRectF.right);
        assertEquals(11.0f, mRectF.bottom);

    }

    public void testHeight() {
        mRectF = new RectF(1.0f, 1.0f, 20.5f, 20.5f);
        assertEquals(19.5f, mRectF.height());
    }

    public void testWidth() {
        mRectF = new RectF(1.0f, 1.0f, 20.5f, 20.5f);
        assertEquals(19.5f, mRectF.width());
    }

    public void testOffsetTo() {

        mRectF = new RectF(5, 5, 10, 10);
        mRectF.offsetTo(1.0f, 1.0f);
        assertEquals(1.0f, mRectF.left);
        assertEquals(1.0f, mRectF.top);
        assertEquals(6.0f, mRectF.right);
        assertEquals(6.0f, mRectF.bottom);

    }

    public void testSetEmpty() {

        // Before setEmpty()
        mRectF = new RectF(1, 2, 3, 4);
        assertEquals(1.0f, mRectF.left);
        assertEquals(2.0f, mRectF.top);
        assertEquals(3.0f, mRectF.right);
        assertEquals(4.0f, mRectF.bottom);

        // After setEmpty()
        mRectF.setEmpty();
        assertEquals(0.0f, mRectF.left);
        assertEquals(0.0f, mRectF.top);
        assertEquals(0.0f, mRectF.right);
        assertEquals(0.0f, mRectF.bottom);

    }

    public void testIsEmpty() {

        mRectF = new RectF();
        assertTrue(mRectF.isEmpty());
        mRectF = new RectF(1.0f, 1.0f, 1.0f, 1.0f);
        assertTrue(mRectF.isEmpty());
        mRectF = new RectF(0.0f, 1.0f, 2.0f, 1.0f);
        assertTrue(mRectF.isEmpty());
        mRectF = new RectF(1.0f, 1.0f, 20.0f, 20.0f);
        assertFalse(mRectF.isEmpty());

    }

    public void testCenterX() {

        mRectF = new RectF(10.0f, 10.0f, 20.0f, 20.0f);
        assertEquals(15.0f, mRectF.centerX());
        mRectF = new RectF(10.5f, 10.0f, 20.0f, 20.0f);
        assertEquals(15.25f, mRectF.centerX());
        mRectF = new RectF(10.4f, 10.0f, 20.0f, 20.0f);
        assertEquals(15.2f, mRectF.centerX());

    }

    public void testCenterY() {

        mRectF = new RectF(10.0f, 10.0f, 20.0f, 20.0f);
        assertEquals(15.0f, mRectF.centerY());
        mRectF = new RectF(10.0f, 10.5f, 20.0f, 20.0f);
        assertEquals(15.25f, mRectF.centerY());
        mRectF = new RectF(10.0f, 10.4f, 20.0f, 20.0f);
        assertEquals(15.2f, mRectF.centerY());
    }

    public void testToString() {
        mRectF = new RectF();
        assertNotNull(mRectF.toString());
    }

    public void testSetIntersect() {

        RectF rectF1 = new RectF(0, 0, 10, 10);
        RectF rectF2 = new RectF(5, 5, 15, 15);

        // Empty RectF
        mRectF = new RectF();
        assertTrue(mRectF.setIntersect(rectF1, rectF2));
        assertEquals(5.0f, mRectF.left);
        assertEquals(5.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

        // Not Empty RectF
        mRectF = new RectF(0, 0, 15, 15);
        assertTrue(mRectF.setIntersect(rectF1, rectF2));
        assertEquals(5.0f, mRectF.left);
        assertEquals(5.0f, mRectF.top);
        assertEquals(10.0f, mRectF.right);
        assertEquals(10.0f, mRectF.bottom);

    }

    public void testRoundOut() {

        Rect rect = new Rect();
        mRectF = new RectF(1.2f, 1.8f, 5.2f, 5.8f);
        mRectF.roundOut(rect);
        assertEquals(1, rect.left);
        assertEquals(1, rect.top);
        assertEquals(6, rect.right);
        assertEquals(6, rect.bottom);

    }

    public void testRound() {

        Rect rect = new Rect();
        mRectF = new RectF(1.2f, 1.8f, 5.2f, 5.8f);
        mRectF.round(rect);
        assertEquals(1, rect.left);
        assertEquals(2, rect.top);
        assertEquals(5, rect.right);
        assertEquals(6, rect.bottom);

    }

    public void testWriteReadParcel() {
        RectF rectOut = new RectF(0.0f,0.0f,10.0f,10.0f);
        Parcel out = Parcel.obtain();
        rectOut.writeToParcel(out, 0);
        out.setDataPosition(0);
        RectF rectIn = new RectF(1.0f,1.0f,1.0f,1.0f);
        rectIn.readFromParcel(out);
        assertEquals(rectOut.left, rectIn.left);
        assertEquals(rectOut.bottom, rectIn.bottom);
        assertEquals(rectOut.right, rectIn.right);
        assertEquals(rectOut.top, rectIn.top);
        assertNotNull(rectIn.toString());

        assertEquals(0, rectIn.describeContents());
    }
}

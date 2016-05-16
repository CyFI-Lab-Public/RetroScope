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

import junit.framework.TestCase;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;

public class PathTest extends TestCase {

    // Test constants
    private static final float LEFT = 10.0f;
    private static final float RIGHT = 50.0f;
    private static final float TOP = 10.0f;
    private static final float BOTTOM = 50.0f;
    private static final float XCOORD = 40.0f;
    private static final float YCOORD = 40.0f;

    public void testConstructor() {
        // new the Path instance
        new Path();

        // another the Path instance with different params
        new Path(new Path());
    }

    public void testAddRect1() {

        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF rect = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.addRect(rect, Path.Direction.CW);
        assertFalse(path.isEmpty());
    }

    public void testAddRect2() {

        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.addRect(LEFT, TOP, RIGHT, BOTTOM, Path.Direction.CW);
        assertFalse(path.isEmpty());
    }

    public void testMoveTo() {
        // new the Path instance
        Path path = new Path();
        path.moveTo(10.0f, 10.0f);
    }

    public void testSet() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        Path path1 = new Path();
        setPath(path1);
        path.set(path1);
        assertFalse(path.isEmpty());
    }

    public void testAccessFillType() {
        // set the expected value
        Path.FillType expected1 = Path.FillType.EVEN_ODD;
        Path.FillType expected2 = Path.FillType.INVERSE_EVEN_ODD;
        Path.FillType expected3 = Path.FillType.INVERSE_WINDING;
        Path.FillType expected4 = Path.FillType.WINDING;

        // new the Path instance
        Path path = new Path();
        // set FillType by {@link Path#setFillType(FillType)}
        path.setFillType(Path.FillType.EVEN_ODD);
        assertEquals(expected1, path.getFillType());
        path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        assertEquals(expected2, path.getFillType());
        path.setFillType(Path.FillType.INVERSE_WINDING);
        assertEquals(expected3, path.getFillType());
        path.setFillType(Path.FillType.WINDING);
        assertEquals(expected4, path.getFillType());
    }

    public void testRQuadTo() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.rQuadTo(5.0f, 5.0f, 10.0f, 10.0f);
        assertFalse(path.isEmpty());
    }

    public void testTransform1() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        Path dst = new Path();
        setPath(path);
        path.transform(new Matrix(), dst);
        assertFalse(dst.isEmpty());
    }

    public void testTransform2() {

    }

    public void testLineTo() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.lineTo(XCOORD, YCOORD);
        assertFalse(path.isEmpty());
    }

    public void testClose() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        setPath(path);
        path.close();
    }

    public void testQuadTo() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.quadTo(20.0f, 20.0f, 40.0f, 40.0f);
        assertFalse(path.isEmpty());
    }

    public void testAddCircle() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.addCircle(XCOORD, YCOORD, 10.0f, Path.Direction.CW);
        assertFalse(path.isEmpty());
    }

    public void testArcTo1() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF oval = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.arcTo(oval, 0.0f, 30.0f, true);
        assertFalse(path.isEmpty());
    }

    public void testArcTo2() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF oval = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.arcTo(oval, 0.0f, 30.0f);
        assertFalse(path.isEmpty());
    }

    public void testComputeBounds1() {

        RectF expected = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        assertEquals(expected.width(), bounds.width());
        assertEquals(expected.height(), bounds.height());
        path.computeBounds(bounds, false);
        assertEquals(expected.width(), bounds.width());
        assertEquals(expected.height(), bounds.height());
    }

    public void testComputeBounds2() {

        RectF expected = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF bounds = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.addRect(bounds, Path.Direction.CW);
        path.computeBounds(bounds, true);
        assertEquals(expected.width(), bounds.width());
        assertEquals(expected.height(), bounds.height());
        path.computeBounds(bounds, false);
        assertEquals(expected.width(), bounds.width());
        assertEquals(expected.height(), bounds.height());
    }

    public void testRMoveTo() {
        // new the Path instance
    }

    public void testSetLastPoint() {
        // new the Path instance
        Path path = new Path();
        path.setLastPoint(10.0f, 10.0f);
    }

    public void testRLineTo() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.rLineTo(10.0f, 10.0f);
        assertFalse(path.isEmpty());
    }

    public void testIsEmpty() {

        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        setPath(path);
        assertFalse(path.isEmpty());
    }

    public void testRewind() {

        // set the expected value
        Path.FillType expected = Path.FillType.EVEN_ODD;

        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        setPath(path);
        path.rewind();
        path.setFillType(Path.FillType.EVEN_ODD);
        assertTrue(path.isEmpty());
        assertEquals(expected, path.getFillType());
    }

    public void testAddOval() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF oval = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.addOval(oval, Path.Direction.CW);
        assertFalse(path.isEmpty());
    }

    public void testIsRect() {

        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        setPath(path);
    }

    public void testIncReserve() {
    }

    public void testAddPath1() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        Path src = new Path();
        setPath(src);
        path.addPath(src, 10.0f, 10.0f);
        assertFalse(path.isEmpty());
    }

    public void testAddPath2() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        Path src = new Path();
        setPath(src);
        path.addPath(src);
        assertFalse(path.isEmpty());
    }

    public void testAddPath3() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        Path src = new Path();
        setPath(src);
        Matrix matrix = new Matrix();
        path.addPath(src, matrix);
        assertFalse(path.isEmpty());
    }

    public void testAddRoundRect1() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF rect = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.addRoundRect(rect, XCOORD, YCOORD, Path.Direction.CW);
        assertFalse(path.isEmpty());
    }

    public void testAddRoundRect2() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF rect = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        float[] radii = new float[8];
        for (int i = 0; i < 8; i++) {
            radii[i] = 10.0f + i * 5.0f;
        }
        path.addRoundRect(rect, radii, Path.Direction.CW);
        assertFalse(path.isEmpty());
    }

    public void testIsInverseFillType() {

        // new the Path instance
        Path path = new Path();
        assertFalse(path.isInverseFillType());
        path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        assertTrue(path.isInverseFillType());
    }

    public void testOffset1() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        setPath(path);
        Path dst = new Path();
        path.offset(XCOORD, YCOORD, dst);
        assertFalse(dst.isEmpty());
    }

    public void testOffset2() {
        // new the Path instance
    }

    public void testCubicTo() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.cubicTo(10.0f, 10.0f, 20.0f, 20.0f, 30.0f, 30.0f);
        assertFalse(path.isEmpty());
    }

    public void testReset() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        Path path1 = new Path();
        setPath(path1);
        path.set(path1);
        assertFalse(path.isEmpty());
        path.reset();
        assertTrue(path.isEmpty());
    }

    public void testToggleInverseFillType() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.toggleInverseFillType();
        assertTrue(path.isInverseFillType());
    }

    public void testAddArc() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        RectF oval = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.addArc(oval, 0.0f, 30.0f);
        assertFalse(path.isEmpty());
    }

    public void testRCubicTo() {
        // new the Path instance
        Path path = new Path();
        assertTrue(path.isEmpty());
        path.rCubicTo(10.0f, 10.0f, 11.0f, 11.0f, 12.0f, 12.0f);
        assertFalse(path.isEmpty());
    }

    private void setPath(Path path) {
        RectF rect = new RectF(LEFT, TOP, RIGHT, BOTTOM);
        path.addRect(rect, Path.Direction.CW);
    }
}

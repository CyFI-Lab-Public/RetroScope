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


import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Path.Direction;
import android.test.AndroidTestCase;

public class PathMeasureTest extends AndroidTestCase {
    private PathMeasure mPathMeasure;
    private Path mPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPath = new Path();
        mPathMeasure = new PathMeasure();

    }

    public void testConstructor() {
        mPathMeasure = new PathMeasure();

        // new the PathMeasure instance
        Path path = new Path();
        mPathMeasure = new PathMeasure(path, true);

        // new the PathMeasure instance
        mPathMeasure = new PathMeasure(path, false);
    }

    public void testGetPosTan() {
        float distance = 1f;
        float[] pos = { 1f };
        float[] tan = { 1f };
        try {
            mPathMeasure.getPosTan(distance, pos, tan);
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
        float[] pos2 = { 1f, 2f };
        float[] tan2 = { 1f, 3f };
        assertFalse(mPathMeasure.getPosTan(distance, pos2, tan2));

        mPathMeasure.setPath(mPath, true);
        mPath.addRect(1f, 2f, 3f, 4f, Path.Direction.CW);
        mPathMeasure.setPath(mPath, true);
        float[] pos3 = { 1f, 2f, 3f, 4f };
        float[] tan3 = { 1f, 2f, 3f, 4f };
        assertTrue(mPathMeasure.getPosTan(0f, pos3, tan3));
    }

    public void testNextContour() {
        assertFalse(mPathMeasure.nextContour());
        mPath.addRect(1, 2, 3, 4, Path.Direction.CW);
        mPathMeasure.setPath(mPath, true);
        assertTrue(mPathMeasure.nextContour());
    }

    public void testGetLength() {
        assertEquals(0f, mPathMeasure.getLength());
        mPath.addRect(1, 2, 3, 4, Path.Direction.CW);
        mPathMeasure.setPath(mPath, true);
        assertEquals(8.0f, mPathMeasure.getLength());
    }

    public void testIsClosed() {
        Path circle = new Path();
        circle.addCircle(0, 0, 1, Direction.CW);

        PathMeasure measure = new PathMeasure(circle, false);
        assertTrue(measure.isClosed());
        measure.setPath(circle, true);
        assertTrue(measure.isClosed());

        Path line = new Path();
        line.lineTo(5, 5);

        measure.setPath(line, false);
        assertFalse(measure.isClosed());
        measure.setPath(line, true);
        assertTrue(measure.isClosed());
    }

    public void testSetPath() {
        mPathMeasure.setPath(mPath, true);
        //There is no getter and we can't obtain any status about it.
    }

    public void testGetSegment() {
        assertEquals(0f, mPathMeasure.getLength());
        mPath.addRect(1, 2, 3, 4, Path.Direction.CW);
        mPathMeasure.setPath(mPath, true);
        assertEquals(8f, mPathMeasure.getLength());
        Path dst = new Path();
        assertTrue(mPathMeasure.getSegment(0, mPathMeasure.getLength(), dst, true));
        assertFalse(mPathMeasure.getSegment(mPathMeasure.getLength(), 0, dst, true));
    }

    public void testGetMatrix() {
        Matrix matrix = new Matrix();
        assertFalse(mPathMeasure.getMatrix(1f, matrix, PathMeasure.POSITION_MATRIX_FLAG));
        matrix.setScale(1f, 2f);
        mPath.addRect(1f, 2f, 3f, 4f, Path.Direction.CW);
        mPathMeasure.setPath(mPath, true);
        assertTrue(mPathMeasure.getMatrix(0f, matrix, PathMeasure.TANGENT_MATRIX_FLAG));
    }
}

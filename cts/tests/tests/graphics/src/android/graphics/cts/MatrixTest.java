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
import android.graphics.RectF;
import android.graphics.Matrix.ScaleToFit;
import android.test.AndroidTestCase;

public class MatrixTest extends AndroidTestCase {
    private Matrix mMatrix;
    private float[] mValues;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMatrix = new Matrix();
        mValues = new float[9];
    }

    public void testConstractor() {
        new Matrix();
        new Matrix(mMatrix);
    }

    public void testIsIdentity() {
        assertTrue(mMatrix.isIdentity());
        mMatrix.setScale(0f, 0f);
        assertFalse(mMatrix.isIdentity());
    }

    public void testRectStaysRect() {
        assertTrue(mMatrix.rectStaysRect());
        mMatrix.postRotate(80);
        assertFalse(mMatrix.rectStaysRect());
    }

    public void testSet() {
        mValues[0] = 1000;
        mMatrix.getValues(mValues);
        Matrix matrix = new Matrix();
        matrix.set(mMatrix);
        mValues = new float[9];
        mValues[0] = 2000;
        matrix.getValues(mValues);
        assertEquals(1f, mValues[0]);
    }

    public void testEquals() {
        mMatrix.setScale(1f, 2f);
        Matrix matrix = new Matrix();
        matrix.set(mMatrix);
        assertFalse(mMatrix.equals(null));
        assertFalse(mMatrix.equals(new String()));
        assertTrue(mMatrix.equals(matrix));
    }

    public void testReset() {
        mMatrix.setScale(1f, 2f, 3f, 4f);
        String expect = "[1.0, 0.0, 0.0][0.0, 2.0, -4.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        mMatrix.reset();
        expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetScale() {
        String expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        mMatrix.setScale(1f, 2f);
        expect = "[1.0, 0.0, 0.0][0.0, 2.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetScale2() {
        String expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());

        mMatrix.setScale(1f, 2f, 3f, 4f);
        expect = "[1.0, 0.0, 0.0][0.0, 2.0, -4.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetRotate() {
        mMatrix.setRotate(1f);
        String expect = "[0.9998477, -0.017452406, 0.0]"
                + "[0.017452406, 0.9998477, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetRotate2() {
        mMatrix.setRotate(1f, 2f, 3f);
        String expect = "[0.9998477, -0.017452406, 0.0526618]"
                + "[0.017452406, 0.9998477, -0.034447942][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetSinCos() {
        mMatrix.setSinCos(1f, 2f);
        String expect = "[2.0, -1.0, 0.0][1.0, 2.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetSinCos2() {
        mMatrix.setSinCos(1f, 2f, 3f, 4f);
        String expect = "[2.0, -1.0, 1.0][1.0, 2.0, -7.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetSkew() {
        mMatrix.setSkew(1f, 2f);
        String expect = "[1.0, 1.0, 0.0][2.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetSkew2() {
        mMatrix.setSkew(1f, 2f, 3f, 4f);
        String expect = "[1.0, 1.0, -4.0][2.0, 1.0, -6.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetConcat() {
        Matrix a = new Matrix();
        Matrix b = new Matrix();
        mMatrix.setConcat(a, b);
        String expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        mMatrix = new Matrix();
        mMatrix.setConcat(mMatrix, b);
        mMatrix.setConcat(a, b);
        expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        mMatrix = new Matrix();
        mValues = new float[9];
        mMatrix.setConcat(a, mMatrix);
        mMatrix.getValues(mValues);
        expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreTranslate() {
        assertTrue(mMatrix.preTranslate(1f, 2f));
        String expect = "[1.0, 0.0, 1.0][0.0, 1.0, 2.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreScale() {
        assertTrue(mMatrix.preScale(1f, 2f));
        String expect = "[1.0, 0.0, 0.0][0.0, 2.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreScale2() {
        assertTrue(mMatrix.preScale(1f, 2f, 3f, 4f));
        String expect = "[1.0, 0.0, 0.0][0.0, 2.0, -4.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreRotate() {
        assertTrue(mMatrix.preRotate(1f));
        String expect = "[0.9998477, -0.017452406, 0.0][0.017452406, 0.9998477, "
                + "0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreRotate2() {
        assertTrue(mMatrix.preRotate(1f, 2f, 3f));
        float[] values = new float[9];
        mMatrix.getValues(values);
        String expect = "[0.9998477, -0.017452406, 0.0526618][0.017452406, 0.9998477,"
                + " -0.034447942][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreSkew() {
        assertTrue(mMatrix.preSkew(1f, 2f));
        String expect = "[1.0, 1.0, 0.0][2.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreSkew2() {
        assertTrue(mMatrix.preSkew(1f, 2f, 3f, 4f));
        String expect = "[1.0, 1.0, -4.0][2.0, 1.0, -6.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPreConcat() {
        float[] values = new float[9];
        values[0] = 1000;
        Matrix matrix = new Matrix();
        matrix.setValues(values);
        assertTrue(mMatrix.preConcat(matrix));
        String expect = "[1000.0, 0.0, 0.0][0.0, 0.0, 0.0][0.0, 0.0, 0.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostTranslate() {
        assertTrue(mMatrix.postTranslate(1f, 2f));
        String expect = "[1.0, 0.0, 1.0][0.0, 1.0, 2.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostScale() {
        assertTrue(mMatrix.postScale(1f, 2f));
        String expect = "[1.0, 0.0, 0.0][0.0, 2.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostScale2() {
        assertTrue(mMatrix.postScale(1f, 2f, 3f, 4f));
        String expect = "[1.0, 0.0, 0.0][0.0, 2.0, -4.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostRotate() {
        assertTrue(mMatrix.postRotate(1f));
        String expect = "[0.9998477, -0.017452406, 0.0]" +
         "[0.017452406, 0.9998477, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostRotate2() {
        assertTrue(mMatrix.postRotate(1f, 2f, 3f));
        String expect = "[0.9998477, -0.017452406, 0.0526618]" +
        "[0.017452406, 0.9998477, -0.034447942][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostSkew() {
        assertTrue(mMatrix.postSkew(1f, 2f));
        String expect = "[1.0, 1.0, 0.0][2.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostSkew2() {
        assertTrue(mMatrix.postSkew(1f, 2f, 3f, 4f));
        String expect = "[1.0, 1.0, -4.0][2.0, 1.0, -6.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testPostConcat() {
        Matrix matrix = new Matrix();
        float[] values = new float[9];
        values[0] = 1000;
        matrix.setValues(values);
        assertTrue(mMatrix.postConcat(matrix));

        String expect = "[1000.0, 0.0, 0.0][0.0, 0.0, 0.0][0.0, 0.0, 0.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetRectToRect() {
        RectF r1 = new RectF();
        r1.set(1f, 2f, 3f, 3f);
        RectF r2 = new RectF();
        r1.set(10f, 20f, 30f, 30f);
        assertTrue(mMatrix.setRectToRect(r1, r2, ScaleToFit.CENTER));
        String expect = "[0.0, 0.0, 0.0][0.0, 0.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        mMatrix.setRectToRect(r1, r2, ScaleToFit.END);

        assertEquals(expect, mMatrix.toShortString());
        mMatrix.setRectToRect(r1, r2, ScaleToFit.FILL);
        assertEquals(expect, mMatrix.toShortString());
        mMatrix.setRectToRect(r1, r2, ScaleToFit.START);
        assertEquals(expect, mMatrix.toShortString());

        assertFalse(mMatrix.setRectToRect(r2, r1, ScaleToFit.CENTER));

        expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        assertFalse(mMatrix.setRectToRect(r2, r1, ScaleToFit.FILL));
        assertEquals(expect, mMatrix.toShortString());
        assertFalse(mMatrix.setRectToRect(r2, r1, ScaleToFit.START));
        assertEquals(expect, mMatrix.toShortString());
        assertFalse(mMatrix.setRectToRect(r2, r1, ScaleToFit.END));
        assertEquals(expect, mMatrix.toShortString());

        try {
            mMatrix.setRectToRect(null, null, ScaleToFit.CENTER);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testInvert() {
        Matrix matrix = new Matrix();
        float[] values = new float[9];
        values[0] = 1000f;
        matrix.setValues(values);
        assertTrue(mMatrix.invert(matrix));
        String expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, matrix.toShortString());
        expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        boolean result = false;
        try {
            result = mMatrix.invert(null);
            fail("should throw exception");
        } catch (Exception e) {
        }
        assertFalse(result);
    }

    public void testSetPolyToPoly() {
        float[] src = new float[9];
        src[0] = 100f;
        float[] dst = new float[9];
        dst[0] = 200f;
        dst[1] = 300f;
        assertTrue(mMatrix.setPolyToPoly(src, 0, dst, 0, 1));
        String expect = "[1.0, 0.0, 100.0][0.0, 1.0, 300.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
        try {
            mMatrix.setPolyToPoly(src, 0, dst, 0, 5);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapPoints() {
        float[] value = new float[9];
        value[0] = 100f;
        mMatrix.mapPoints(value);
        assertEquals(value[0], 100f);
        try {
            mMatrix.mapPoints(null);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapPoints2() {
        float[] dst = new float[9];
        dst[0] = 100f;
        float[] src = new float[9];
        src[0] = 200f;
        mMatrix.mapPoints(dst, src);
        assertEquals(dst[0], 200f);
        try {
            mMatrix.mapPoints(new float[8], new float[9]);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapPoints3() {
        float[] dst = new float[9];
        dst[0] = 100f;
        float[] src = new float[9];
        src[0] = 200f;
        mMatrix.mapPoints(dst, 0, src, 0, 9 >> 1);
        assertEquals(dst[0], 200f);
        try {
            mMatrix.mapPoints(null, 0, new float[9], 0, 1);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapVectors() {
        float[] values = new float[9];
        values = new float[9];
        values[0] = 100f;
        mMatrix.mapVectors(values);
        assertEquals(values[0], 100f);
        try {
            mMatrix.mapVectors(null);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapVectors2() {
        float[] src = new float[9];
        src[0] = 100f;
        float[] dst = new float[9];
        dst[0] = 200f;
        mMatrix.mapVectors(dst, src);
        assertEquals(dst[0], 100f);

        try {
            mMatrix.mapVectors(new float[9], new float[8]);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapVectors3() {
        float[] src = new float[9];
        src[0] = 100f;
        float[] dst = new float[9];
        dst[0] = 200f;
        mMatrix.mapVectors(dst, 0, src, 0, 1);
        assertEquals(dst[0], 100f);
        try {
            mMatrix.mapVectors(dst, 0, src, 0, 10);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapRadius() {
        assertEquals(mMatrix.mapRadius(100f), 100f);
        assertEquals(mMatrix.mapRadius(Float.MAX_VALUE),
                Float.POSITIVE_INFINITY);
        assertEquals(mMatrix.mapRadius(Float.MIN_VALUE), 0f);
    }

    public void testMapRect() {
        RectF r = new RectF();
        r.set(1f, 2f, 3f, 4f);
        assertTrue(mMatrix.mapRect(r));
        assertEquals(1f, r.left);
        assertEquals(2f, r.top);
        assertEquals(3f, r.right);
        assertEquals(4f, r.bottom);

        try {
            mMatrix.mapRect(null);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testMapRect2() {
        RectF dst = new RectF();
        dst.set(100f, 100f, 200f, 200f);
        RectF src = new RectF();
        dst.set(10f, 10f, 20f, 20f);
        assertTrue(mMatrix.mapRect(dst, src));
        assertEquals(0f, dst.left);
        assertEquals(0f, dst.top);
        assertEquals(0f, dst.right);
        assertEquals(0f, dst.bottom);

        assertEquals(0f, src.left);
        assertEquals(0f, src.top);
        assertEquals(0f, src.right);
        assertEquals(0f, src.bottom);

        try {
            mMatrix.mapRect(null, null);
            fail("should throw exception");
        } catch (Exception e) {
        }
    }

    public void testAccessValues() {
        Matrix matrix = new Matrix();
        mMatrix.invert(matrix);
        float[] values = new float[9];
        values[0] = 9f;
        values[1] = 100f;
        mMatrix.setValues(values);
        values = new float[9];
        mMatrix.getValues(values);
        String expect = "[9.0, 100.0, 0.0][0.0, 0.0, 0.0][0.0, 0.0, 0.0]";
        assertEquals(expect, toShortString(values));
    }

    private String toShortString(float[] values) {
        return "[" + values[0] + ", " + values[1] + ", " + values[2] + "]["
                + values[3] + ", " + values[4] + ", " + values[5] + "]["
                + values[6] + ", " + values[7] + ", " + values[8] + "]";
    }

    public void testToString() {
        assertNotNull(mMatrix.toString());
    }

    public void testToShortString() {
        String expect = "[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

    public void testSetTranslate() {
        mMatrix.setTranslate(2f, 3f);
        String expect = "[1.0, 0.0, 2.0][0.0, 1.0, 3.0][0.0, 0.0, 1.0]";
        assertEquals(expect, mMatrix.toShortString());
    }

}

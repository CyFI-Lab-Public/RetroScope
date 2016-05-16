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

import javax.microedition.khronos.opengles.GL;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas.EdgeType;
import android.graphics.Canvas.VertexMode;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.Region.Op;
import android.test.InstrumentationTestCase;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;

import com.android.cts.stub.R;


public class CanvasTest extends InstrumentationTestCase {
    private final static int PAINT_COLOR = 0xff00ff00;
    private final static int BITMAP_WIDTH = 10;
    private final static int BITMAP_HEIGHT = 28;
    private final static int FLOAT_ARRAY_LEN = 9;

    private final Rect mRect = new Rect(0, 0, 10, 31);

    private final RectF mRectF = new RectF(0, 0, 10, 31);

    // used for save related methods tests
    private final float[] values1 = {
            1, 2, 3, 4, 5, 6, 7, 8, 9
    };

    private final float[] values2 = {
            9, 8, 7, 6, 5, 4, 3, 2, 1
    };

    private Paint mPaint;
    private Canvas mCanvas;
    private Bitmap mImmutableBitmap;
    private Bitmap mMutableBitmap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mPaint = new Paint();
        mPaint.setColor(PAINT_COLOR);

        final Resources res = getInstrumentation().getTargetContext().getResources();
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inScaled = false; // bitmap will only be immutable if not scaled during load
        mImmutableBitmap = BitmapFactory.decodeResource(res, R.drawable.start, opt);
        assertFalse(mImmutableBitmap.isMutable());
        mMutableBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        mCanvas = new Canvas(mMutableBitmap);
    }

    public void testCanvas1() {
        final Canvas c = new Canvas();
    }

    public void testCanvas2() {
        // abnormal case: bitmap to be constructed is immutable
        try {
            new Canvas(mImmutableBitmap);
            fail("should throw out IllegalStateException when creating Canvas with an ImmutableBitmap");
        } catch (IllegalStateException e) {
            // expected
        }

        // abnormal case: bitmap to be constructed is recycled
        mMutableBitmap.recycle();
        try {
            new Canvas(mMutableBitmap);
            fail("should throw out RuntimeException when creating Canvas with a"
                     + " MutableBitmap which is recycled");
        } catch (RuntimeException e) {
            // expected
        }

        mMutableBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        new Canvas(mMutableBitmap);
    }

    public void testSetBitmap() {
        // abnormal case: bitmap to be set is immutable
        try {
            mCanvas.setBitmap(mImmutableBitmap);
            fail("should throw out IllegalStateException when setting an "
                    + "ImmutableBitmap to a Canvas");
        } catch (IllegalStateException e) {
            // expected
        }

        // abnormal case: bitmap to be set has been recycled
        mMutableBitmap.recycle();
        try {
            mCanvas.setBitmap(mMutableBitmap);
            fail("should throw out RuntimeException when setting Bitmap which is recycled"
                          + " to a Canvas");
        } catch (RuntimeException e) {
            // expected
        }

        mMutableBitmap = Bitmap.createBitmap(BITMAP_WIDTH, 31, Config.ARGB_8888);
        mCanvas.setBitmap(mMutableBitmap);
        assertEquals(BITMAP_WIDTH, mCanvas.getWidth());
        assertEquals(31, mCanvas.getHeight());
    }

    public void testIsOpaque() {
        assertFalse(mCanvas.isOpaque());
    }

    public void testRestore() {
        // abnormal case: save not called before restore
        try {
            mCanvas.restore();
            fail("should throw out IllegalStateException because cannot restore Canvas"
                            + " before save");
        } catch (IllegalStateException e) {
            // expected
        }

        mCanvas.save();
        mCanvas.restore();
    }

    public void testSave1() {
        final Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save();

        final Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        final float[] values3 = new float[FLOAT_ARRAY_LEN];
        final Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        final float[] values4 = new float[FLOAT_ARRAY_LEN];
        final Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    public void testSave2() {
        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save(Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[FLOAT_ARRAY_LEN];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[FLOAT_ARRAY_LEN];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save(Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save(Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    public void testSaveLayer1() {
        final Paint p = new Paint();
        final RectF rF = new RectF(0, 10, 31, 0);

        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(rF, p, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[FLOAT_ARRAY_LEN];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[FLOAT_ARRAY_LEN];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(rF, p, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(rF, p, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    public void testSaveLayer2() {
        final Paint p = new Paint();

        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(10, 0, 0, 31, p, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[FLOAT_ARRAY_LEN];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[FLOAT_ARRAY_LEN];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(10, 0, 0, 31, p, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(10, 0, 0, 31, p, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    public void testSaveLayerAlpha1() {
        final RectF rF = new RectF(0, 10, 31, 0);

        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(rF, 0xff, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[FLOAT_ARRAY_LEN];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[FLOAT_ARRAY_LEN];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(rF, 0xff, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(rF, 0xff, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    public void testSaveLayerAlpha2() {
        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(0, 10, 31, 0, 0xff, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[FLOAT_ARRAY_LEN];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[FLOAT_ARRAY_LEN];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(0, 10, 31, 0, 0xff, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(0, 10, 31, 0, 0xff, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[FLOAT_ARRAY_LEN];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[FLOAT_ARRAY_LEN];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    public void testGetSaveCount() {
        // why is 1 not 0
        assertEquals(1, mCanvas.getSaveCount());
        mCanvas.save();
        assertEquals(2, mCanvas.getSaveCount());
        mCanvas.save();
        assertEquals(3, mCanvas.getSaveCount());
        mCanvas.saveLayer(new RectF(), new Paint(), Canvas.ALL_SAVE_FLAG);
        assertEquals(4, mCanvas.getSaveCount());
        mCanvas.saveLayerAlpha(new RectF(), 0, Canvas.ALL_SAVE_FLAG);
        assertEquals(5, mCanvas.getSaveCount());
    }

    public void testRestoreToCount() {
        // abnormal case: saveCount less than 1
        try {
            mCanvas.restoreToCount(0);
            fail("should throw out IllegalArgumentException because saveCount is less than 1");
        } catch (IllegalArgumentException e) {
            // expected
        }

        final Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        final int count = mCanvas.save();
        assertTrue(count > 0);

        final Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        final float[] values3 = new float[FLOAT_ARRAY_LEN];
        final Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restoreToCount(count);
        final float[] values4 = new float[FLOAT_ARRAY_LEN];
        final Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    public void testGetMatrix1() {
        final float[] f1 = {
                1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        final Matrix m1 = new Matrix();
        m1.setValues(f1);
        mCanvas.setMatrix(m1);

        final Matrix m2 = new Matrix(m1);
        mCanvas.getMatrix(m2);

        assertTrue(m1.equals(m2));

        final float[] f2 = new float[FLOAT_ARRAY_LEN];
        m2.getValues(f2);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(f1[i], f2[i]);
        }
    }

    public void testGetMatrix2() {
        final float[] f1 = {
                1, 2, 3, 4, 5, 6, 7, 8, 9
        };
        final Matrix m1 = new Matrix();
        m1.setValues(f1);

        mCanvas.setMatrix(m1);
        final Matrix m2 = mCanvas.getMatrix();

        assertTrue(m1.equals(m2));

        final float[] f2 = new float[FLOAT_ARRAY_LEN];
        m2.getValues(f2);

        for (int i = 0; i < FLOAT_ARRAY_LEN; i++) {
            assertEquals(f1[i], f2[i]);
        }
    }

    public void testTranslate() {
        preCompare();

        mCanvas.translate(0.10f, 0.28f);

        final float[] values = new float[FLOAT_ARRAY_LEN];
        mCanvas.getMatrix().getValues(values);
        assertEquals(1.0f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(0.1f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(1.0f, values[4]);
        assertEquals(0.28f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    public void testScale1() {
        preCompare();

        mCanvas.scale(0.5f, 0.5f);

        final float[] values = new float[FLOAT_ARRAY_LEN];
        mCanvas.getMatrix().getValues(values);
        assertEquals(0.5f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(0.5f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    public void testScale2() {
        preCompare();

        mCanvas.scale(3.0f, 3.0f, 1.0f, 1.0f);

        final float[] values = new float[FLOAT_ARRAY_LEN];
        mCanvas.getMatrix().getValues(values);
        assertEquals(3.0f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(-2.0f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(3.0f, values[4]);
        assertEquals(-2.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    public void testRotate1() {
        preCompare();

        mCanvas.rotate(90);

        final float[] values = new float[FLOAT_ARRAY_LEN];
        mCanvas.getMatrix().getValues(values);
        assertEquals(0.0f, values[0]);
        assertEquals(-1.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(1.0f, values[3]);
        assertEquals(0.0f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    public void testRotate2() {
        preCompare();

        mCanvas.rotate(30, 1.0f, 0.0f);

        final float[] values = new float[FLOAT_ARRAY_LEN];
        mCanvas.getMatrix().getValues(values);
        assertEquals(0.8660254f, values[0]);
        assertEquals(-0.5f, values[1]);
        assertEquals(0.13397461f, values[2]);
        assertEquals(0.5f, values[3]);
        assertEquals(0.8660254f, values[4]);
        assertEquals(-0.5f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    public void testSkew() {
        preCompare();

        mCanvas.skew(1.0f, 3.0f);

        final float[] values = new float[FLOAT_ARRAY_LEN];
        mCanvas.getMatrix().getValues(values);
        assertEquals(1.0f, values[0]);
        assertEquals(1.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(3.0f, values[3]);
        assertEquals(1.0f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    public void testConcat() {
        preCompare();

        final Matrix m = new Matrix();
        final float[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8};

        m.setValues(values);
        mCanvas.concat(m);

        mCanvas.getMatrix().getValues(values);
        assertEquals(0.0f, values[0]);
        assertEquals(1.0f, values[1]);
        assertEquals(2.0f, values[2]);
        assertEquals(3.0f, values[3]);
        assertEquals(4.0f, values[4]);
        assertEquals(5.0f, values[5]);
        assertEquals(6.0f, values[6]);
        assertEquals(7.0f, values[7]);
        assertEquals(8.0f, values[8]);
    }

    public void testClipRect1() {
        assertFalse(mCanvas.clipRect(mRectF, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRect(mRectF, Op.INTERSECT));
        assertTrue(mCanvas.clipRect(mRectF, Op.REPLACE));
        assertFalse(mCanvas.clipRect(mRectF, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipRect(mRectF, Op.UNION));
        assertFalse(mCanvas.clipRect(mRectF, Op.XOR));
    }

    public void testClipRect2() {
        assertFalse(mCanvas.clipRect(mRect, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRect(mRect, Op.INTERSECT));
        assertTrue(mCanvas.clipRect(mRect, Op.REPLACE));
        assertFalse(mCanvas.clipRect(mRect, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipRect(mRect, Op.UNION));
        assertFalse(mCanvas.clipRect(mRect, Op.XOR));
    }

    public void testClipRect3() {
        assertTrue(mCanvas.clipRect(mRectF));
    }

    public void testClipRect4() {
        assertTrue(mCanvas.clipRect(mRect));
    }

    public void testClipRect5() {
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.INTERSECT));
        assertTrue(mCanvas.clipRect(0, 0, 10, 31, Op.REPLACE));
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipRect(0, 0, 10, 31, Op.UNION));
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.XOR));
    }

    public void testClipRect6() {
        assertTrue(mCanvas.clipRect(0.5f, 0.5f, 10.5f, 31.5f));
    }

    public void testClipRect7() {
        assertTrue(mCanvas.clipRect(0, 0, 10, 31));
    }

    public void testClipPath1() {
        final Path p = new Path();
        p.addRect(mRectF, Direction.CCW);
        assertTrue(mCanvas.clipPath(p));
    }

    public void testClipPath2() {
        final Path p = new Path();
        p.addRect(mRectF, Direction.CCW);

        assertFalse(mCanvas.clipPath(p, Op.DIFFERENCE));
        assertFalse(mCanvas.clipPath(p, Op.INTERSECT));
        assertTrue(mCanvas.clipPath(p, Op.REPLACE));
        assertFalse(mCanvas.clipPath(p, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipPath(p, Op.UNION));
        assertFalse(mCanvas.clipPath(p, Op.XOR));
    }

    public void testClipRegion1() {
        assertFalse(mCanvas.clipRegion(new Region(0, 10, 29, 0)));
    }

    public void testClipRegion2() {
        final Region r = new Region(0, 10, 29, 0);

        assertTrue(mCanvas.clipRegion(r, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRegion(r, Op.INTERSECT));
        assertFalse(mCanvas.clipRegion(r, Op.REPLACE));
        assertFalse(mCanvas.clipRegion(r, Op.REVERSE_DIFFERENCE));
        assertFalse(mCanvas.clipRegion(r, Op.UNION));
        assertFalse(mCanvas.clipRegion(r, Op.XOR));
    }

    public void testGetDrawFilter() {
        assertNull(mCanvas.getDrawFilter());
        final DrawFilter dF = new DrawFilter();
        mCanvas.setDrawFilter(dF);

        assertTrue(dF.equals(mCanvas.getDrawFilter()));
    }

    public void testQuickReject1() {
        assertFalse(mCanvas.quickReject(mRectF, EdgeType.AA));
        assertFalse(mCanvas.quickReject(mRectF, EdgeType.BW));
    }

    public void testQuickReject2() {
        final Path p = new Path();
        p.addRect(mRectF, Direction.CCW);

        assertFalse(mCanvas.quickReject(p, EdgeType.AA));
        assertFalse(mCanvas.quickReject(p, EdgeType.BW));
    }

    public void testQuickReject3() {
        assertFalse(mCanvas.quickReject(0, 0, 10, 31, EdgeType.AA));
        assertFalse(mCanvas.quickReject(0, 0, 10, 31, EdgeType.BW));
    }

    public void testGetClipBounds1() {
        final Rect r = new Rect();

        assertTrue(mCanvas.getClipBounds(r));
        assertEquals(BITMAP_WIDTH, r.width());
        assertEquals(BITMAP_HEIGHT, r.height());
    }

    public void testGetClipBounds2() {
        final Rect r = mCanvas.getClipBounds();

        assertEquals(BITMAP_WIDTH, r.width());
        assertEquals(BITMAP_HEIGHT, r.height());
    }

    private void checkDrewColor(int color) {
        assertEquals(color, mMutableBitmap.getPixel(0, 0));
        assertEquals(color, mMutableBitmap.getPixel(BITMAP_WIDTH / 2, BITMAP_HEIGHT / 2));
        assertEquals(color, mMutableBitmap.getPixel(BITMAP_WIDTH - 1, BITMAP_HEIGHT - 1));
    }

    public void testDrawRGB() {
        final int alpha = 0xff;
        final int red = 0xff;
        final int green = 0xff;
        final int blue = 0xff;

        mCanvas.drawRGB(red, green, blue);

        final int color = alpha << 24 | red << 16 | green << 8 | blue;
        checkDrewColor(color);
    }

    public void testDrawARGB() {
        final int alpha = 0xff;
        final int red = 0x22;
        final int green = 0x33;
        final int blue = 0x44;

        mCanvas.drawARGB(alpha, red, green, blue);
        final int color = alpha << 24 | red << 16 | green << 8 | blue;
        checkDrewColor(color);
    }

    public void testDrawColor1() {
        final int color = 0xffff0000;

        mCanvas.drawColor(color);
        checkDrewColor(color);
    }

    public void testDrawColor2() {
        mCanvas.drawColor(0xffff0000, Mode.CLEAR);
        mCanvas.drawColor(0xffff0000, Mode.DARKEN);
        mCanvas.drawColor(0xffff0000, Mode.DST);
        mCanvas.drawColor(0xffff0000, Mode.DST_ATOP);
        mCanvas.drawColor(0xffff0000, Mode.DST_IN);
        mCanvas.drawColor(0xffff0000, Mode.DST_OUT);
        mCanvas.drawColor(0xffff0000, Mode.DST_OVER);
        mCanvas.drawColor(0xffff0000, Mode.LIGHTEN);
        mCanvas.drawColor(0xffff0000, Mode.MULTIPLY);
        mCanvas.drawColor(0xffff0000, Mode.SCREEN);
        mCanvas.drawColor(0xffff0000, Mode.SRC);
        mCanvas.drawColor(0xffff0000, Mode.SRC_ATOP);
        mCanvas.drawColor(0xffff0000, Mode.SRC_IN);
        mCanvas.drawColor(0xffff0000, Mode.SRC_OUT);
        mCanvas.drawColor(0xffff0000, Mode.SRC_OVER);
        mCanvas.drawColor(0xffff0000, Mode.XOR);
    }

    public void testDrawPaint() {
        mCanvas.drawPaint(mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    public void testDrawPoints1() {
        // abnormal case: invalid offset
        try {
            mCanvas.drawPoints(new float[] {
                    10.0f, 29.0f
            }, -1, 2, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because of invalid offset");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: invalid count
        try {
            mCanvas.drawPoints(new float[] {
                    10.0f, 29.0f
            }, 0, 31, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because of invalid count");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        mCanvas.drawPoints(new float[] {
                0, 0
        }, 0, 2, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    public void testDrawPoints2() {
        mCanvas.drawPoints(new float[]{0, 0}, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    public void testDrawPoint() {
        mCanvas.drawPoint(0, 0, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    public void testDrawLine() {
        mCanvas.drawLine(0, 0, 10, 12, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    public void testDrawLines1() {
        // abnormal case: invalid offset
        try {
            mCanvas.drawLines(new float[] {
                    0, 0, 10, 31
            }, 2, 4, new Paint());
            fail("should throw out ArrayIndexOutOfBoundsException because of invalid offset");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: invalid count
        try {
            mCanvas.drawLines(new float[] {
                    0, 0, 10, 31
            }, 0, 8, new Paint());
            fail("should throw out ArrayIndexOutOfBoundsException because of invalid count");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        mCanvas.drawLines(new float[] {
                0, 0, 10, 12
        }, 0, 4, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    public void testDrawLines2() {
        mCanvas.drawLines(new float[] {
                0, 0, 10, 12
        }, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    private void checkDrewPaint() {
        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(5, 6));
        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(9, 11));
    }

    public void testDrawRect1() {
        mCanvas.drawRect(new RectF(0, 0, 10, 12), mPaint);

        checkDrewPaint();
    }

    public void testDrawRect2() {
        mCanvas.drawRect(new Rect(0, 0, 10, 12), mPaint);

        checkDrewPaint();
    }

    public void testDrawRect3() {
        mCanvas.drawRect(0, 0, 10, 12, mPaint);

        checkDrewPaint();
    }

    public void testDrawOval() {
        // abnormal case: Oval is null
        try {
            mCanvas.drawOval(null, mPaint);
            fail("should throw out NullPointerException because oval is null");
        } catch (NullPointerException e) {
            // expected
        }

        // normal case
        mCanvas.drawOval(new RectF(0, 0, 10, 12), mPaint);
    }

    public void testDrawCircle() {
        // special case: circle's radius <= 0
        mCanvas.drawCircle(10.0f, 10.0f, -1.0f, mPaint);

        // normal case
        mCanvas.drawCircle(10, 12, 3, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(9, 11));
    }

    public void testDrawArc() {
        // abnormal case: oval is null
        try {
            mCanvas.drawArc(null, 10.0f, 29.0f, true, mPaint);
            fail("should throw NullPointerException because oval is null");
        } catch (NullPointerException e) {
            // expected
        }

        // normal case
        mCanvas.drawArc(new RectF(0, 0, 10, 12), 10, 11, false, mPaint);
        mCanvas.drawArc(new RectF(0, 0, 10, 12), 10, 11, true, mPaint);
    }

    public void testDrawRoundRect() {
        // abnormal case: RoundRect is null
        try {
            mCanvas.drawRoundRect(null, 10.0f, 29.0f, mPaint);
            fail("should throw out NullPointerException because RoundRect is null");
        } catch (NullPointerException e) {
            // expected
        }

        mCanvas.drawRoundRect(new RectF(0, 0, 10, 12), 8, 8, mPaint);
    }

    public void testDrawPath() {
        mCanvas.drawPath(new Path(), mPaint);
    }

    public void testDrawBitmap1() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        // abnormal case: the bitmap to be drawn is recycled
        b.recycle();
        try {
            mCanvas.drawBitmap(b, 10.0f, 29.0f, mPaint);
            fail("should throw out RuntimeException because bitmap has been recycled");
        } catch (RuntimeException e) {
            // expected
        }

        b = Bitmap.createBitmap(BITMAP_WIDTH, 12, Config.ARGB_8888);
        mCanvas.drawBitmap(b, 10, 12, null);
        mCanvas.drawBitmap(b, 5, 12, mPaint);
    }

    public void testDrawBitmap2() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        // abnormal case: the bitmap to be drawn is recycled
        b.recycle();
        try {
            mCanvas.drawBitmap(b, null, new RectF(), mPaint);
            fail("should throw out RuntimeException because bitmap has been recycled");
        } catch (RuntimeException e) {
            // expected
        }

        b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);
        mCanvas.drawBitmap(b, new Rect(), new RectF(), null);
        mCanvas.drawBitmap(b, new Rect(), new RectF(), mPaint);
    }

    public void testDrawBitmap3() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        // abnormal case: the bitmap to be drawn is recycled
        b.recycle();
        try {
            mCanvas.drawBitmap(b, null, new Rect(), mPaint);
            fail("should throw out RuntimeException because bitmap has been recycled");
        } catch (RuntimeException e) {
            // expected
        }

        b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);
        mCanvas.drawBitmap(b, new Rect(), new Rect(), null);
        mCanvas.drawBitmap(b, new Rect(), new Rect(), mPaint);
    }

    public void testDrawBitmap4() {
        final int[] colors = new int[2008];

        // abnormal case: width less than 0
        try {
            mCanvas.drawBitmap(colors, 10, 10, 10, 10, -1, 10, true, null);
            fail("should throw out IllegalArgumentException because width is less than 0");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // abnormal case: height less than 0
        try {
            mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, -1, true, null);
            fail("should throw out IllegalArgumentException because height is less than 0");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // abnormal case: stride less than width and bigger than -width
        try {
            mCanvas.drawBitmap(colors, 10, 5, 10, 10, 10, 10, true, null);
            fail("should throw out IllegalArgumentException because stride less than width and"
                            + " bigger than -width");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // abnormal case: offset less than 0
        try {
            mCanvas.drawBitmap(colors, -1, 10, 10, 10, 10, 10, true, null);
            fail("should throw out ArrayIndexOutOfBoundsException because offset less than 0");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: (offset + width) bigger than colors' length
        try {
            mCanvas.drawBitmap(new int[29], 10, 29, 10, 10, 20, 10, true, null);
            fail("should throw out ArrayIndexOutOfBoundsException because sum of offset and width"
                            + " is bigger than colors' length");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // special case: width equals to 0
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 0, 10, true, null);

        // special case: height equals to 0
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, 0, true, null);

        // normal case
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, 29, true, null);
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, 29, true, mPaint);
    }

    public void testDrawBitmap6() {
        final int[] colors = new int[2008];

        // abnormal case: width less than 0
        try {
            mCanvas.drawBitmap(colors, 10, 10, 10.0f, 10.0f, -1, 10, true, null);
            fail("should throw out IllegalArgumentException because width is less than 0");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // abnormal case: height less than 0
        try {
            mCanvas.drawBitmap(colors, 10, 10, 10.0f, 10.0f, 10, -1, true, null);
            fail("should throw out IllegalArgumentException because height is less than 0");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // abnormal case: stride less than width and bigger than -width
        try {
            mCanvas.drawBitmap(colors, 10, 5, 10.0f, 10.0f, 10, 10, true, null);
            fail("should throw out IllegalArgumentException because stride is less than width "
                                + "and bigger than -width");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // abnormal case: offset less than 0
        try {
            mCanvas.drawBitmap(colors, -1, 10, 10.0f, 10.0f, 10, 10, true, null);
            fail("should throw out IllegalArgumentException because offset is less than 0");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: (offset + width) bigger than colors' length
        try {
            mCanvas.drawBitmap(new int[29], 10, 29, 10.0f, 10.0f, 20, 10, true, null);
            fail("should throw out ArrayIndexOutOfBoundsException because sum of offset and width"
                            + " is bigger than colors' length");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // special case: width equals to 0
        mCanvas.drawBitmap(colors, 10, 10, 10.0f, 10.0f, 0, 10, true, null);

        // special case: height equals to 0
        mCanvas.drawBitmap(colors, 10, 10, 10.0f, 10.0f, 10, 0, true, null);

        // normal case
        mCanvas.drawBitmap(colors, 10, 10, 10.0f, 10.0f, 10, 29, true, null);
        mCanvas.drawBitmap(colors, 10, 10, 10.0f, 10.0f, 10, 29, true, mPaint);
    }

    public void testDrawBitmap5() {
        final Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);
        mCanvas.drawBitmap(b, new Matrix(), null);
        mCanvas.drawBitmap(b, new Matrix(), mPaint);
    }

    public void testDrawBitmapMesh() {
        final Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        // abnormal case: meshWidth less than 0
        try {
            mCanvas.drawBitmapMesh(b, -1, 10, null, 0, null, 0, null);
            fail("should throw out ArrayIndexOutOfBoundsException because meshWidth less than 0");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: meshHeight less than 0
        try {
            mCanvas.drawBitmapMesh(b, 10, -1, null, 0, null, 0, null);
            fail("should throw out ArrayIndexOutOfBoundsException because meshHeight "
                                    + "is less than 0");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: vertOffset less than 0
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, null, -1, null, 0, null);
            fail("should throw out ArrayIndexOutOfBoundsException because vertOffset "
                                                + "is less than 0");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: colorOffset less than 0
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, null, 10, null, -1, null);
            fail("should throw out ArrayIndexOutOfBoundsException because colorOffset is"
                                    + " less than 0");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // special case: meshWidth equals to 0
        mCanvas.drawBitmapMesh(b, 0, 10, null, 10, null, 10, null);

        // special case: meshHeight equals to 0
        mCanvas.drawBitmapMesh(b, 10, 0, null, 10, null, 10, null);

        // abnormal case: verts' length is too short
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, new float[] {
                    10.0f, 29.0f
            }, 10, null, 10, null);
            fail("should throw out ArrayIndexOutOfBoundsException because verts' length"
                                    + " is too short");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: colors' length is too short
        final float[] verts = new float[2008];
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, verts, 10, new int[] {
                    10, 29
            }, 10, null);
            fail("should throw out ArrayIndexOutOfBoundsException because colors' "
                        + "length is too short");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        final int[] colors = new int[2008];
        mCanvas.drawBitmapMesh(b, 10, 10, verts, 10, colors, 10, null);
        mCanvas.drawBitmapMesh(b, 10, 10, verts, 10, colors, 10, mPaint);
    }

    public void testDrawVertices() {
        final float[] verts = new float[10];
        final float[] texs = new float[10];
        final int[] colors = new int[10];
        final short[] indices = {
                0, 1, 2, 3, 4, 1
        };

        // abnormal case: (vertOffset + vertexCount) bigger than verts' length
        try {
            mCanvas.drawVertices(VertexMode.TRIANGLES, 10, verts, 8, texs, 0, colors, 0, indices,
                    0, 4, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because sum of vertOffset and"
                            + " vertexCount is bigger than verts' length");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: (texOffset + vertexCount) bigger than texs' length
        try {
            mCanvas.drawVertices(VertexMode.TRIANGLES, 10, verts, 0, texs, 30, colors, 0, indices,
                    0, 4, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because sum of texOffset and"
                                    + " vertexCount is bigger thatn texs' length");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: (colorOffset + vertexCount) bigger than colors' length
        try {
            mCanvas.drawVertices(VertexMode.TRIANGLES, 10, verts, 0, texs, 0, colors, 30, indices,
                    0, 4, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because sum of colorOffset and"
                                + " vertexCount is bigger than colors' length");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: (indexOffset + indexCount) bigger than indices' length
        try {
            mCanvas.drawVertices(VertexMode.TRIANGLES, 10, verts, 0, texs, 0, colors, 0, indices,
                    10, 30, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because sum of indexOffset and"
                            + " indexCount is bigger than indices' length");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // special case: in texs, colors, indices, one of them, two of them and
        // all are null
        mCanvas.drawVertices(VertexMode.TRIANGLES, 0, verts, 0, null, 0, colors, 0, indices, 0, 0,
                mPaint);

        mCanvas.drawVertices(VertexMode.TRIANGLE_STRIP, 0, verts, 0, null, 0, null, 0, indices, 0,
                0, mPaint);

        mCanvas.drawVertices(VertexMode.TRIANGLE_FAN, 0, verts, 0, null, 0, null, 0, null, 0, 0,
                mPaint);

        // normal case: texs, colors, indices are not null
        mCanvas.drawVertices(VertexMode.TRIANGLES, 10, verts, 0, texs, 0, colors, 0, indices, 0, 6,
                mPaint);

        mCanvas.drawVertices(VertexMode.TRIANGLE_STRIP, 10, verts, 0, texs, 0, colors, 0, indices,
                0, 6, mPaint);

        mCanvas.drawVertices(VertexMode.TRIANGLE_FAN, 10, verts, 0, texs, 0, colors, 0, indices, 0,
                6, mPaint);
    }

    public void testDrawText1() {
        final char[] text = {
                'a', 'n', 'd', 'r', 'o', 'i', 'd'
        };

        // abnormal case: index less than 0
        try {
            mCanvas.drawText(text, -1, 7, 10, 10, mPaint);
            fail("should throw out IndexOutOfBoundsException because index is less than 0");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: count less than 0
        try {
            mCanvas.drawText(text, 0, -1, 10, 10, mPaint);
            fail("should throw out IndexOutOfBoundsException because count is less than 0");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: (index + count) bigger than text's length
        try {
            mCanvas.drawText(text, 0, 10, 10, 10, mPaint);
            fail("should throw out IndexOutOfBoundsException because sum of index and count "
                                + "is bigger than text's length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        mCanvas.drawText(text, 0, 7, 10, 10, mPaint);
    }

    public void testDrawText2() {
        mCanvas.drawText("android", 10, 30, mPaint);
    }

    public void testDrawText3() {
        final String text = "android";

        // abnormal case: start less than 0
        try {
            mCanvas.drawText(text, -1, 7, 10, 30, mPaint);
            fail("should throw out IndexOutOfBoundsException because start is lesss than 0");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: end less than 0
        try {
            mCanvas.drawText(text, 0, -1, 10, 30, mPaint);
            fail("should throw out IndexOutOfBoundsException because end is less than 0");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: start bigger than end
        try {
            mCanvas.drawText(text, 3, 1, 10, 30, mPaint);
            fail("should throw out IndexOutOfBoundsException because start is bigger than end");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: (end - start) bigger than text's length
        try {
            mCanvas.drawText(text, 0, 10, 10, 30, mPaint);
            fail("should throw out IndexOutOfBoundsException because end subtracts start should"
                                + " bigger than text's length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        mCanvas.drawText(text, 0, 7, 10, 30, mPaint);
    }

    public void testDrawText4() {
        final String t1 = "android";
        mCanvas.drawText(t1, 0, 7, 10, 30, mPaint);

        final SpannedString t2 = new SpannedString(t1);
        mCanvas.drawText(t2, 0, 7, 10, 30, mPaint);

        final SpannableString t3 = new SpannableString(t2);
        mCanvas.drawText(t3, 0, 7, 10, 30, mPaint);

        final SpannableStringBuilder t4 = new SpannableStringBuilder(t1);
        mCanvas.drawText(t4, 0, 7, 10, 30, mPaint);

        final StringBuffer t5 = new StringBuffer(t1);
        mCanvas.drawText(t5, 0, 7, 10, 30, mPaint);
    }

    public void testDrawPosText1() {
        final char[] text = {
                'a', 'n', 'd', 'r', 'o', 'i', 'd'
        };
        final float[] pos = new float[] {
                0.0f, 0.0f, 1.0f, 1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 4.0f, 4.0f, 5.0f, 5.0f, 6.0f, 6.0f,
                7.0f, 7.0f
        };

        // abnormal case: index less than 0
        try {
            mCanvas.drawPosText(text, -1, 7, pos, mPaint);
            fail("should throw out IndexOutOfBoundsException because index is less than 0");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: index + count > text.length
        try {
            mCanvas.drawPosText(text, 1, 10, pos, mPaint);
            fail("should throw out IndexOutOfBoundsException because sum of index and count is"
                                + " bigger than text's length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: count*2 > pos.length
        try {
            mCanvas.drawPosText(text, 1, 10, new float[] {
                    10.0f, 30.f
            }, mPaint);
            fail("should throw out IndexOutOfBoundsException because 2 times of count is"
                                + " bigger than pos' length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        mCanvas.drawPosText(text, 0, 7, pos, mPaint);
    }

    public void testDrawPosText2() {
        final String text = "android";
        final float[] pos = new float[] {
                0.0f, 0.0f, 1.0f, 1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 4.0f, 4.0f, 5.0f, 5.0f, 6.0f, 6.0f,
                7.0f, 7.0f
        };

        // abnormal case: text.length()*2 > pos.length
        try {
            mCanvas.drawPosText(text, new float[] {
                    10.0f, 30.f
            }, mPaint);
            fail("should throw out IndexOutOfBoundsException because 2 times of text's length is"
                                + " bigger than pos' length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        mCanvas.drawPosText(text, pos, mPaint);
    }

    public void testDrawTextOnPath1() {
        final Path path = new Path();
        final char[] text = {
                'a', 'n', 'd', 'r', 'o', 'i', 'd'
        };

        // abnormal case: index < 0
        try {
            mCanvas.drawTextOnPath(text, -1, 7, path, 10.0f, 10.0f, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because index is smaller than 0");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // abnormal case: index + count > text.length
        try {
            mCanvas.drawTextOnPath(text, 0, 10, path, 10.0f, 10.0f, mPaint);
            fail("should throw out ArrayIndexOutOfBoundsException because sum of index and"
                            + " count is bigger than text's length");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        // normal case
        mCanvas.drawTextOnPath(text, 0, 7, path, 10.0f, 10.0f, mPaint);
    }

    public void testDrawTextOnPath2() {
        final Path path = new Path();
        String text = "";

        // no character in text
        mCanvas.drawTextOnPath(text, path, 10.0f, 10.0f, mPaint);

        // There are characters in text
        text = "android";
        mCanvas.drawTextOnPath(text, path, 10.0f, 10.0f, mPaint);
    }

    public void testDrawPicture1() {
        mCanvas.drawPicture(new Picture());
    }

    public void testDrawPicture2() {
        final RectF dst = new RectF(0, 0, 10, 31);
        final Picture p = new Picture();

        // picture width or length not bigger than 0
        mCanvas.drawPicture(p, dst);

        p.beginRecording(10, 30);
        mCanvas.drawPicture(p, dst);
    }

    public void testDrawPicture3() {
        final Rect dst = new Rect(0, 10, 30, 0);
        final Picture p = new Picture();

        // picture width or length not bigger than 0
        mCanvas.drawPicture(p, dst);

        p.beginRecording(10, 30);
        mCanvas.drawPicture(p, dst);
    }

    public void testDensity() {
        // set Density
        mCanvas.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        assertEquals(DisplayMetrics.DENSITY_DEFAULT, mCanvas.getDensity());

        // set Density
        mCanvas.setDensity(DisplayMetrics.DENSITY_HIGH);
        assertEquals(DisplayMetrics.DENSITY_HIGH, mCanvas.getDensity());
    }

    private void preCompare() {
        final float[] values = new float[FLOAT_ARRAY_LEN];
        mCanvas.getMatrix().getValues(values);
        assertEquals(1.0f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(1.0f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }
}

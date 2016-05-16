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

package android.view.animation.cts;

import android.graphics.Matrix;
import android.test.AndroidTestCase;
import android.view.animation.Transformation;

public class TransformationTest extends AndroidTestCase {

    public void testConstructor() {
        new Transformation();
    }

    public void testCompose() {
        final Transformation t1 = new Transformation();
        final Transformation t2 = new Transformation();
        t1.setAlpha(0.5f);
        t2.setAlpha(0.4f);
        t1.getMatrix().setScale(3, 1);
        t2.getMatrix().setScale(3, 1);
        t1.setTransformationType(Transformation.TYPE_MATRIX);
        t2.setTransformationType(Transformation.TYPE_ALPHA);
        t2.compose(t1);

        Matrix expectedMatrix = new Matrix();
        expectedMatrix.setScale(9, 1);
        assertEquals(expectedMatrix, t2.getMatrix());
        assertEquals(0.4f * 0.5f, t2.getAlpha());
        assertEquals(Transformation.TYPE_ALPHA, t2.getTransformationType());

        t1.setTransformationType(Transformation.TYPE_IDENTITY);
        t2.compose(t1);
        expectedMatrix = new Matrix();
        expectedMatrix.setScale(27, 1);
        assertEquals(expectedMatrix, t2.getMatrix());
        assertEquals(0.4f * 0.5f * 0.5f, t2.getAlpha());
        assertEquals(Transformation.TYPE_ALPHA, t2.getTransformationType());
    }

    public void testClear() {
        final Transformation t1 = new Transformation();
        final Transformation t2 = new Transformation();
        t2.set(t1);
        assertTransformationEquals(t1, t2);

        // Change the t2
        t2.setAlpha(0.0f);
        t2.getMatrix().setScale(2, 3);
        t2.setTransformationType(Transformation.TYPE_ALPHA);
        assertTransformationNotSame(t1, t2);

        // Clear the change
        t2.clear();
        assertTransformationEquals(t1, t2);
    }

    private void assertTransformationNotSame(Transformation expected, Transformation actual) {
        assertNotSame(expected.getAlpha(), actual.getAlpha());
        assertFalse(expected.getMatrix().equals(actual.getMatrix()));
        assertNotSame(expected.getTransformationType(), actual.getTransformationType());
    }

    private void assertTransformationEquals(Transformation expected, Transformation actual) {
        assertEquals(expected.getAlpha(), actual.getAlpha());
        assertEquals(expected.getMatrix(), actual.getMatrix());
        assertEquals(expected.getTransformationType(), actual.getTransformationType());
    }

    public void testAccessTransformationType() {
        final Transformation transformation = new Transformation();

        // From Javadoc of {@link Transformation#clear()}, we see the default type is TYPE_BOTH.
        assertEquals(Transformation.TYPE_BOTH, transformation.getTransformationType());

        transformation.setTransformationType(Transformation.TYPE_IDENTITY);
        assertEquals(Transformation.TYPE_IDENTITY, transformation.getTransformationType());

        transformation.setTransformationType(Transformation.TYPE_ALPHA);
        assertEquals(Transformation.TYPE_ALPHA, transformation.getTransformationType());

        transformation.setTransformationType(Transformation.TYPE_MATRIX);
        assertEquals(Transformation.TYPE_MATRIX, transformation.getTransformationType());

        transformation.setTransformationType(Transformation.TYPE_BOTH);
        assertEquals(Transformation.TYPE_BOTH, transformation.getTransformationType());
    }

    public void testSet() {
        final Transformation t1 = new Transformation();
        t1.setAlpha(0.0f);
        final Transformation t2 = new Transformation();
        t2.set(t1);
        assertTransformationEquals(t1, t2);
    }

    public void testAccessAlpha() {
        final Transformation transformation = new Transformation();

        transformation.setAlpha(0.0f);
        assertEquals(0.0f, transformation.getAlpha());

        transformation.setAlpha(0.5f);
        assertEquals(0.5f, transformation.getAlpha());

        transformation.setAlpha(1.0f);
        assertEquals(1.0f, transformation.getAlpha());
    }

    public void testToString() {
        assertNotNull(new Transformation().toString());
        assertNotNull(new Transformation().toShortString());
    }

    public void testGetMatrix() {
        final Matrix expected = new Matrix();
        final Transformation transformation = new Transformation();
        assertEquals(expected, transformation.getMatrix());
    }
}

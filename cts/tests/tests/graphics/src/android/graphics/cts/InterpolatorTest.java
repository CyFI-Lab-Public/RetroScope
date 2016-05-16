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
import android.graphics.Interpolator;
import android.graphics.Interpolator.Result;
import android.os.SystemClock;

public class InterpolatorTest extends TestCase {

    private static final int DEFAULT_KEYFRAME_COUNT = 2;
    private static final float TOLERANCE = 0.1f;

    public void testConstructor() {
        Interpolator i = new Interpolator(10);
        assertEquals(10, i.getValueCount());
        assertEquals(DEFAULT_KEYFRAME_COUNT, i.getKeyFrameCount());

        i = new Interpolator(15, 20);
        assertEquals(15, i.getValueCount());
        assertEquals(20, i.getKeyFrameCount());
    }

    public void testReset1() {
        final int expected = 100;
        Interpolator interpolator = new Interpolator(10);
        assertEquals(DEFAULT_KEYFRAME_COUNT, interpolator.getKeyFrameCount());
        interpolator.reset(expected);
        assertEquals(expected, interpolator.getValueCount());
        assertEquals(DEFAULT_KEYFRAME_COUNT, interpolator.getKeyFrameCount());
    }

    public void testReset2() {
        int expected1 = 100;
        int expected2 = 200;
        // new the Interpolator instance
        Interpolator interpolator = new Interpolator(10);
        interpolator.reset(expected1, expected2);
        assertEquals(expected1, interpolator.getValueCount());
        assertEquals(expected2, interpolator.getKeyFrameCount());
    }

    public void testTimeToValues1() throws InterruptedException {
        Interpolator interpolator = new Interpolator(1);
        assertEquals(1, interpolator.getValueCount());

        // NORMAL
        long time = SystemClock.uptimeMillis();
        // set key frames far enough apart so that processing time will not cause result to
        // deviate more than TOLERANCE
        interpolator.setKeyFrame(0, (int)(time - 10000), new float[] {1.0f});
        interpolator.setKeyFrame(1, (int)(time + 10000), new float[] {2.0f});
        assertValue(1.5f, Result.NORMAL, interpolator);

        // FREEZE_START
        interpolator.reset(1);
        time = SystemClock.uptimeMillis();
        interpolator.setKeyFrame(0, (int)(time + 1000), new float[] {2.0f});
        interpolator.setKeyFrame(1, (int)(time + 2000), new float[] {3.0f});
        assertValue(2.0f, Result.FREEZE_START, interpolator);

        // FREEZE_END
        interpolator.reset(1);
        time = SystemClock.uptimeMillis();
        interpolator.setKeyFrame(0, (int)(time - 2000), new float[] {2.0f});
        interpolator.setKeyFrame(1, (int)(time - 1000), new float[] {3.0f});
        assertValue(3.0f, Result.FREEZE_END, interpolator);

        final int valueCount = 2;
        interpolator.reset(valueCount);
        assertEquals(valueCount, interpolator.getValueCount());

        try {
            // value array too short
            interpolator.timeToValues(new float[valueCount - 1]);
            fail("should throw ArrayStoreException");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    public void testTimeToValues2() {
        Interpolator interpolator = new Interpolator(1);
        interpolator.setKeyFrame(0, 2000, new float[] {1.0f});
        interpolator.setKeyFrame(1, 4000, new float[] {2.0f});
        assertValue(1000, 1.0f, Result.FREEZE_START, interpolator);
        assertValue(3000, 1.5f, Result.NORMAL, interpolator);
        assertValue(6000, 2.0f, Result.FREEZE_END, interpolator);

        // known bug: time argument is unsigned 32bit in graphics library
        assertValue(-1000, 2.0f, Result.FREEZE_END, interpolator);

        interpolator.reset(1, 3);
        interpolator.setKeyFrame(0, 2000, new float[] {1.0f});
        interpolator.setKeyFrame(1, 4000, new float[] {2.0f});
        interpolator.setKeyFrame(2, 6000, new float[] {4.0f});
        assertValue(0, 1.0f, Result.FREEZE_START, interpolator);
        assertValue(3000, 1.5f, Result.NORMAL, interpolator);
        assertValue(5000, 3.0f, Result.NORMAL, interpolator);
        assertValue(8000, 4.0f, Result.FREEZE_END, interpolator);


        final int valueCount = 2;
        final int validTime = 0;
        interpolator.reset(valueCount);
        assertEquals(valueCount, interpolator.getValueCount());
        try {
            // value array too short
            interpolator.timeToValues(validTime, new float[valueCount - 1]);
            fail("should throw out ArrayStoreException");
        } catch (ArrayStoreException e) {
            // expected
        }

        interpolator.reset(2, 2);
        interpolator.setKeyFrame(0, 4000, new float[] {1.0f, 1.0f});
        interpolator.setKeyFrame(1, 6000, new float[] {2.0f, 4.0f});
        assertValues(2000, new float[] {1.0f, 1.0f}, Result.FREEZE_START, interpolator);
        assertValues(5000, new float[] {1.5f, 2.5f}, Result.NORMAL, interpolator);
        assertValues(8000, new float[] {2.0f, 4.0f}, Result.FREEZE_END, interpolator);
    }

    public void testSetRepeatMirror() {
        Interpolator interpolator = new Interpolator(1, 3);
        interpolator.setKeyFrame(0, 2000, new float[] {1.0f});
        interpolator.setKeyFrame(1, 4000, new float[] {2.0f});
        interpolator.setKeyFrame(2, 6000, new float[] {4.0f});

        assertValue(1000, 1.0f, Result.FREEZE_START, interpolator);
        assertValue(3000, 1.5f, Result.NORMAL, interpolator);
        assertValue(5000, 3.0f, Result.NORMAL, interpolator);
        assertValue(7000, 4.0f, Result.FREEZE_END, interpolator);

        // repeat once, no mirror
        interpolator.setRepeatMirror(2, false);
        assertValue( 1000, 4.0f, Result.FREEZE_END, interpolator); // known bug
        assertValue( 3000, 1.5f, Result.NORMAL, interpolator);
        assertValue( 5000, 3.0f, Result.NORMAL, interpolator);
        assertValue( 7000, 1.5f, Result.NORMAL, interpolator);
        assertValue( 9000, 3.0f, Result.NORMAL, interpolator);
        assertValue(11000, 4.0f, Result.FREEZE_END, interpolator);

        // repeat once, mirror
        interpolator.setRepeatMirror(2, true);
        assertValue( 1000, 4.0f, Result.FREEZE_END, interpolator); // known bug
        assertValue( 3000, 1.5f, Result.NORMAL, interpolator);
        assertValue( 5000, 3.0f, Result.NORMAL, interpolator);
        assertValue( 7000, 3.0f, Result.NORMAL, interpolator);
        assertValue( 9000, 1.5f, Result.NORMAL, interpolator);
        assertValue(11000, 4.0f, Result.FREEZE_END, interpolator);
    }

    public void testSetKeyFrame() {
        final float[] aZero = new float[] {0.0f};
        final float[] aOne = new float[] {1.0f};

        Interpolator interpolator = new Interpolator(1);
        interpolator.setKeyFrame(0, 2000, aZero);
        interpolator.setKeyFrame(1, 4000, aOne);
        assertValue(1000, 0.0f, Result.FREEZE_START, interpolator);
        assertValue(3000, 0.5f, Result.NORMAL, interpolator);
        assertValue(5000, 1.0f, Result.FREEZE_END, interpolator);

        final float[] linearBlend = new float[] {
                0.0f, 0.0f, 1.0f, 1.0f
        };
        final float[] accelerateBlend = new float[] {
                // approximate circle at PI/6 and PI/3
                0.5f, 1.0f - 0.866f, 0.866f, 0.5f
        };
        final float[] decelerateBlend = new float[] {
                // approximate circle at PI/6 and PI/3
                1.0f - 0.866f, 0.5f, 0.5f, 0.866f
        };

        // explicit linear blend should yield the same values
        interpolator.setKeyFrame(0, 2000, aZero, linearBlend);
        interpolator.setKeyFrame(1, 4000, aOne, linearBlend);
        assertValue(1000, 0.0f, Result.FREEZE_START, interpolator);
        assertValue(3000, 0.5f, Result.NORMAL, interpolator);
        assertValue(5000, 1.0f, Result.FREEZE_END, interpolator);

        // blend of end key frame is not used
        interpolator.setKeyFrame(0, 2000, aZero);
        interpolator.setKeyFrame(1, 4000, aOne, accelerateBlend);
        assertValue(1000, 0.0f, Result.FREEZE_START, interpolator);
        assertValue(3000, 0.5f, Result.NORMAL, interpolator);
        assertValue(5000, 1.0f, Result.FREEZE_END, interpolator);

        final float[] result = new float[1];

        interpolator.setKeyFrame(0, 2000, aZero, accelerateBlend);
        interpolator.setKeyFrame(1, 4000, aOne);
        assertValue(1000, 0.0f, Result.FREEZE_START, interpolator);
        assertEquals(Result.NORMAL, interpolator.timeToValues(3000, result));
        assertTrue(result[0] < 0.5f); // exact blend algorithm not known
        assertValue(5000, 1.0f, Result.FREEZE_END, interpolator);

        interpolator.setKeyFrame(0, 2000, aZero, decelerateBlend);
        interpolator.setKeyFrame(1, 4000, aOne);
        assertValue(1000, 0.0f, Result.FREEZE_START, interpolator);
        assertEquals(Result.NORMAL, interpolator.timeToValues(3000, result));
        assertTrue(result[0] > 0.5f); // exact blend algorithm not known
        assertValue(5000, 1.0f, Result.FREEZE_END, interpolator);

        final int validTime = 0;
        final int valueCount = 2;
        interpolator.reset(valueCount);
        assertEquals(valueCount, interpolator.getValueCount());
        try {
            // value array too short
            interpolator.setKeyFrame(0, validTime, new float[valueCount - 1]);
            fail("should throw ArrayStoreException");
        } catch (ArrayStoreException e) {
            // expected
        }

        try {
            // index too small
            interpolator.setKeyFrame(-1, validTime, new float[valueCount]);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            // index too large
            interpolator.setKeyFrame(2, validTime, new float[valueCount]);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            // blend array too short
            interpolator.setKeyFrame(0, validTime, new float[valueCount], new float[3]);
            fail("should throw ArrayStoreException");
        } catch (ArrayStoreException e) {
            // expected
        }

    }

    private void assertValue(int time, float expected, Result expectedResult,
            Interpolator interpolator) {
        float[] values = new float[1];
        assertEquals(expectedResult, interpolator.timeToValues(time, values));
        assertEquals(expected, values[0], TOLERANCE);
    }

    private void assertValues(int time, float[] expected, Result expectedResult,
            Interpolator interpolator) {
        float[] values = new float[expected.length];
        assertEquals(expectedResult, interpolator.timeToValues(time, values));
        assertFloatArray(expected, values);
    }

    private void assertValue(float expected, Result expectedResult, Interpolator interpolator) {
        float[] values = new float[1];
        assertEquals(expectedResult, interpolator.timeToValues(values));
        assertEquals(expected, values[0], TOLERANCE);
    }

    private void assertFloatArray(float[] expected, float[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], TOLERANCE);
        }
    }
}

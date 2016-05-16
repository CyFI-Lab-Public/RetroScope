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

package android.view.cts;

import android.test.AndroidTestCase;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.util.Log;

/**
 * Test {@link VelocityTracker}.
 */
public class VelocityTrackerTest extends AndroidTestCase {
    private static final String TAG = "VelocityTrackerTest";

    private static final float TOLERANCE_EXACT = 0.01f;
    private static final float TOLERANCE_TIGHT = 0.05f;
    private static final float TOLERANCE_WEAK = 0.1f;
    private static final float TOLERANCE_VERY_WEAK = 0.2f;

    private VelocityTracker mVelocityTracker;

    // Current position, velocity and acceleration.
    private long mTime;
    private long mLastTime;
    private float mPx, mPy;
    private float mVx, mVy;
    private float mAx, mAy;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mVelocityTracker = VelocityTracker.obtain();
        mTime = 1000;
        mLastTime = 0;
        mPx = 300;
        mPy = 600;
        mVx = 0;
        mVy = 0;
        mAx = 0;
        mAy = 0;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mVelocityTracker.recycle();
    }

    public void testNoMovement() {
        move(100, 10);
        assertVelocity(TOLERANCE_EXACT, "Expect exact bound when no movement occurs.");
    }

    public void testLinearMovement() {
        mVx = 2.0f;
        mVy = -4.0f;
        move(100, 10);
        assertVelocity(TOLERANCE_TIGHT, "Expect tight bound for linear motion.");
    }

    public void testAcceleratingMovement() {
        // A very good velocity tracking algorithm will produce a tight bound on
        // simple acceleration.  Certain alternate algorithms will fare less well but
        // may be more stable in the presence of bad input data.
        mVx = 2.0f;
        mVy = -4.0f;
        mAx = 1.0f;
        mAy = -0.5f;
        move(200, 10);
        assertVelocity(TOLERANCE_WEAK, "Expect weak bound when there is acceleration.");
    }

    public void testDeceleratingMovement() {
        // A very good velocity tracking algorithm will produce a tight bound on
        // simple acceleration.  Certain alternate algorithms will fare less well but
        // may be more stable in the presence of bad input data.
        mVx = 2.0f;
        mVy = -4.0f;
        mAx = -1.0f;
        mAy = 0.2f;
        move(200, 10);
        assertVelocity(TOLERANCE_WEAK, "Expect weak bound when there is deceleration.");
    }

    public void testLinearSharpDirectionChange() {
        // After a sharp change of direction we expect the velocity to eventually
        // converge but it might take a moment to get there.
        mVx = 2.0f;
        mVy = -4.0f;
        move(100, 10);
        assertVelocity(TOLERANCE_TIGHT, "Expect tight bound for linear motion.");
        mVx = -1.0f;
        mVy = -3.0f;
        move(100, 10);
        assertVelocity(TOLERANCE_WEAK, "Expect weak bound after 100ms of new direction.");
        move(100, 10);
        assertVelocity(TOLERANCE_TIGHT, "Expect tight bound after 200ms of new direction.");
    }

    public void testLinearSharpDirectionChangeAfterALongPause() {
        // Should be able to get a tighter bound if there is a pause before the
        // change of direction.
        mVx = 2.0f;
        mVy = -4.0f;
        move(100, 10);
        assertVelocity(TOLERANCE_TIGHT, "Expect tight bound for linear motion.");
        pause(100);
        mVx = -1.0f;
        mVy = -3.0f;
        move(100, 10);
        assertVelocity(TOLERANCE_TIGHT,
                "Expect tight bound after a 100ms pause and 100ms of new direction.");
    }

    public void testChangingAcceleration() {
        // In real circumstances, the acceleration changes continuously throughout a
        // gesture.  Try to model this and see how the algorithm copes.
        mVx = 2.0f;
        mVy = -4.0f;
        for (float change : new float[] { 1, -2, -3, -1, 1 }) {
            mAx += 1.0f * change;
            mAy += -0.5f * change;
            move(30, 10);
        }
        assertVelocity(TOLERANCE_VERY_WEAK,
                "Expect weak bound when there is changing acceleration.");
    }

    private void move(long duration, long step) {
        addMovement();
        while (duration > 0) {
            duration -= step;
            mTime += step;
            mPx += (mAx / 2 * step + mVx) * step;
            mPy += (mAy / 2 * step + mVy) * step;
            mVx += mAx * step;
            mVy += mAy * step;
            addMovement();
        }
    }

    private void pause(long duration) {
        mTime += duration;
    }

    private void addMovement() {
        if (mTime >= mLastTime) {
            MotionEvent ev = MotionEvent.obtain(0L, mTime, MotionEvent.ACTION_MOVE, mPx, mPy, 0);
            mVelocityTracker.addMovement(ev);
            ev.recycle();
            mLastTime = mTime;

            mVelocityTracker.computeCurrentVelocity(1);
            final float estimatedVx = mVelocityTracker.getXVelocity();
            final float estimatedVy = mVelocityTracker.getYVelocity();
            Log.d(TAG, String.format(
                    "[%d] x=%6.1f, y=%6.1f, vx=%6.1f, vy=%6.1f, ax=%6.1f, ay=%6.1f, "
                    + "evx=%6.1f (%6.1f%%), evy=%6.1f (%6.1f%%)",
                    mTime, mPx, mPy, mVx, mVy, mAx, mAy,
                    estimatedVx, error(mVx, estimatedVx) * 100.0f,
                    estimatedVy, error(mVy, estimatedVy) * 100.0f));
        }
    }

    private void assertVelocity(float tolerance, String message) {
        mVelocityTracker.computeCurrentVelocity(1);
        final float estimatedVx = mVelocityTracker.getXVelocity();
        final float estimatedVy = mVelocityTracker.getYVelocity();
        float errorVx = error(mVx, estimatedVx);
        float errorVy = error(mVy, estimatedVy);
        if (errorVx > tolerance || errorVy > tolerance) {
            fail(String.format("Velocity exceeds tolerance of %6.1f%%: "
                    + "expected vx=%6.1f, vy=%6.1f. "
                    + "actual vx=%6.1f (%6.1f%%), vy=%6.1f (%6.1f%%)",
                    tolerance * 100.0f, mVx, mVy,
                    estimatedVx, errorVx * 100.0f, estimatedVy, errorVy * 100.0f));
        }
    }

    private static float error(float expected, float actual) {
        float absError = Math.abs(actual - expected);
        if (absError < 0.001f) {
            return 0;
        }
        if (Math.abs(expected) < 0.001f) {
            return 1;
        }
        return absError / Math.abs(expected);
    }
}

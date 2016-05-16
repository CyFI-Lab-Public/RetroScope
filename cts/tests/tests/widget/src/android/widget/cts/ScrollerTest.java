/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.widget.cts;


import android.content.Context;
import android.test.InstrumentationTestCase;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

/**
 * Test {@link Scroller}.
 */
public class ScrollerTest extends InstrumentationTestCase {
    private Scroller mScroller;

    private Context mTargetContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTargetContext = getInstrumentation().getTargetContext();
        mScroller = new Scroller(mTargetContext);
    }

    public void testConstructor() {
        new Scroller(mTargetContext);

        new Scroller(mTargetContext, new LinearInterpolator());
    }

    public void testIsFinished() {
        mScroller.forceFinished(true);
        assertTrue(mScroller.isFinished());

        mScroller.forceFinished(false);
        assertFalse(mScroller.isFinished());
    }

    public void testGetDuration() {
        assertEquals(0, mScroller.getDuration());

        // if duration is not specified when start scroll, the duration is set to default value
        mScroller.startScroll(0, 0, 10000, 100);
        // the default duration depends on implementation
        assertTrue(mScroller.getDuration() > 0);

        mScroller.startScroll(0, 0, 10000, 100, 50000);
        assertEquals(50000, mScroller.getDuration());

        mScroller = new Scroller(mTargetContext);
        assertEquals(0, mScroller.getDuration());

        mScroller.fling(0, 0, 10, 4000, 0, 100, 0, 0);
        // the exact duration depends on implementation
        assertTrue(mScroller.getDuration() > 0);
    }

    public void testAccessFinalX() {
        assertEquals(0, mScroller.getFinalX());
        assertTrue(mScroller.isFinished());

        // force abort animation
        mScroller.abortAnimation();
        assertTrue(mScroller.isFinished());

        mScroller.setFinalX(5000);
        assertEquals(5000, mScroller.getFinalX());
        // check the side effect
        assertFalse(mScroller.isFinished());
    }

    public void testAccessFinalY() {
        assertEquals(0, mScroller.getFinalY());
        assertTrue(mScroller.isFinished());

        // force abort animation
        mScroller.abortAnimation();
        assertTrue(mScroller.isFinished());

        mScroller.setFinalY(5000);
        assertEquals(5000, mScroller.getFinalY());
        // check the side effect
        assertFalse(mScroller.isFinished());
    }

    // We can not get the precise currX and currY when scrolling
    public void testScrollMode() {
        assertEquals(0, mScroller.getFinalX());
        assertEquals(0, mScroller.getFinalY());
        assertEquals(0, mScroller.getDuration());
        assertTrue(mScroller.isFinished());

        mScroller.startScroll(0, 0, 2000, -2000, 5000);

        assertEquals(0, mScroller.getStartX());
        assertEquals(0, mScroller.getStartY());
        assertEquals(2000, mScroller.getFinalX());
        assertEquals(-2000, mScroller.getFinalY());
        assertEquals(5000, mScroller.getDuration());
        assertFalse(mScroller.isFinished());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }

        assertTrue(mScroller.computeScrollOffset());
        // between the start and final position
        assertTrue(mScroller.getCurrX() > 0);
        assertTrue(mScroller.getCurrX() < 2000);
        assertTrue(mScroller.getCurrY() > -2000);
        assertTrue(mScroller.getCurrY() < 0);
        assertFalse(mScroller.isFinished());

        int curX = mScroller.getCurrX();
        int curY = mScroller.getCurrY();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }

        assertTrue(mScroller.computeScrollOffset());
        // between the start and final position
        assertTrue(mScroller.getCurrX() > 0);
        assertTrue(mScroller.getCurrX() < 2000);
        assertTrue(mScroller.getCurrY() > -2000);
        assertTrue(mScroller.getCurrY() < 0);
        // between the last computation and the final postion
        assertTrue(mScroller.getCurrX() > curX);
        assertTrue(mScroller.getCurrY() < curY);
        assertFalse(mScroller.isFinished());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }

        assertTrue(mScroller.computeScrollOffset());
        // reach the final position
        assertEquals(2000, mScroller.getCurrX());
        assertEquals(-2000, mScroller.getCurrY());
        assertTrue(mScroller.isFinished());

        assertFalse(mScroller.computeScrollOffset());
    }

    // We can not get the precise currX and currY when scrolling
    public void testScrollModeWithDefaultDuration() {
        assertEquals(0, mScroller.getFinalX());
        assertEquals(0, mScroller.getFinalY());

        assertEquals(0, mScroller.getDuration());
        assertTrue(mScroller.isFinished());

        mScroller.startScroll(0, 0, -2000, 2000);

        assertEquals(0, mScroller.getStartX());
        assertEquals(0, mScroller.getStartY());
        assertEquals(-2000, mScroller.getFinalX());
        assertEquals(2000, mScroller.getFinalY());
        int defaultDuration = mScroller.getDuration();
        assertTrue(defaultDuration > 0);
        assertFalse(mScroller.isFinished());

        // the default duration is too short to get fine controlled
        // we just check whether the scroller reaches the destination
        try {
            Thread.sleep(defaultDuration);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }

        assertTrue(mScroller.computeScrollOffset());
        // reach the final position
        assertEquals(-2000, mScroller.getCurrX());
        assertEquals(2000, mScroller.getCurrY());
        assertTrue(mScroller.isFinished());

        assertFalse(mScroller.computeScrollOffset());
    }

    // We can not get the precise currX and currY when scrolling
    public void testFlingMode() {
        assertEquals(0, mScroller.getFinalX());
        assertEquals(0, mScroller.getFinalY());
        assertEquals(0, mScroller.getDuration());
        assertTrue(mScroller.isFinished());

        mScroller.fling(0, 0, - 3000, 4000, Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE);

        assertEquals(0, mScroller.getStartX());
        assertEquals(0, mScroller.getStartY());
        int duration = mScroller.getDuration();
        assertTrue(mScroller.getFinalX() < 0);
        assertTrue(mScroller.getFinalY() > 0);
        assertTrue(duration > 0);
        assertFalse(mScroller.isFinished());

        try {
            Thread.sleep(duration / 3);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        assertTrue(mScroller.computeScrollOffset());

        int currX = mScroller.getCurrX();
        int currY = mScroller.getCurrY();

        // scrolls to the position between the start and the final
        assertTrue(currX < 0);
        assertTrue(currX > mScroller.getFinalX());
        assertTrue(currY > 0);
        assertTrue(currY < mScroller.getFinalY());
        assertFalse(mScroller.isFinished());

        // wait for the same duration as the last
        try {
            Thread.sleep(duration / 3);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        assertTrue(mScroller.computeScrollOffset());

        int previousX = currX;
        int previousY = currY;
        currX = mScroller.getCurrX();
        currY = mScroller.getCurrY();

        // scrolls to the position between the last and the final
        assertTrue(currX < previousX);
        assertTrue(currX > mScroller.getFinalX());
        assertTrue(currY > previousY);
        assertTrue(currY < mScroller.getFinalY());
        assertFalse(mScroller.isFinished());

        // the fling mode has a deceleration effect
        assertTrue(Math.abs(currX - previousX) < Math.abs(previousX - 0));
        assertTrue(Math.abs(currY - previousY) < Math.abs(previousY - 0));

        // wait until the scroll finishes
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        assertTrue(mScroller.computeScrollOffset());

        assertEquals(mScroller.getFinalX(), mScroller.getCurrX());
        assertEquals(mScroller.getFinalY(), mScroller.getCurrY());
        assertTrue(mScroller.isFinished());

        assertFalse(mScroller.computeScrollOffset());
    }

    public void testAbortAnimation() {
        mScroller.startScroll(0, 0, 2000, -2000, 5000);
        mScroller.computeScrollOffset();
        assertFalse(mScroller.getCurrX() == mScroller.getFinalX());
        assertFalse(mScroller.getCurrY() == mScroller.getFinalY());
        assertFalse(mScroller.isFinished());
        mScroller.abortAnimation();
        assertTrue(mScroller.getCurrX() == mScroller.getFinalX());
        assertTrue(mScroller.getCurrY() == mScroller.getFinalY());
        assertTrue(mScroller.isFinished());

        mScroller.fling(0, 0, - 3000, 4000,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        mScroller.computeScrollOffset();
        assertFalse(mScroller.getCurrX() == mScroller.getFinalX());
        assertFalse(mScroller.getCurrY() == mScroller.getFinalY());
        assertFalse(mScroller.isFinished());
        mScroller.abortAnimation();
        assertTrue(mScroller.getCurrX() == mScroller.getFinalX());
        assertTrue(mScroller.getCurrY() == mScroller.getFinalY());
        assertTrue(mScroller.isFinished());
    }

    // We can not get the precise duration after it is extended
    public void testExtendDuration() {
        mScroller.startScroll(0, 0, 0, 0, 5000);
        assertEquals(5000, mScroller.getDuration());

        // the new duration = passed time(unknown) + extend time(5000)
        // can not get the precise duration after it is extended
        mScroller.extendDuration(5000);
        assertTrue(mScroller.getDuration() >= 5000);
        assertFalse(mScroller.isFinished());

        // start new scroll
        mScroller.startScroll(0, 0, 0, 0, 500);
        assertEquals(500, mScroller.getDuration());

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        // the duration get extended and the scroll get started again, though the animation
        // is forced aborted, can not get the precise duration after it is extended
        mScroller.abortAnimation();
        mScroller.extendDuration(500);
        assertTrue(mScroller.getDuration() >= 2000);
        assertFalse(mScroller.isFinished());

        //check the side effect, the velocity of scroll will change according to the duration
        mScroller = new Scroller(mTargetContext, new LinearInterpolator());
        mScroller.startScroll(0, 0, 5000, 5000, 5000);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        mScroller.computeScrollOffset();
        int curX = mScroller.getCurrX();
        int curY = mScroller.getCurrY();
        mScroller.extendDuration(9000);
        mScroller.computeScrollOffset();
        // the scrolling speeds are slowing down
        assertTrue(mScroller.getCurrX() - curX < curX - 0);
        assertTrue(mScroller.getCurrY() - curY < curY - 0);
    }

    public void testTimePassed() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        // can not get precise time
        assertTrue(mScroller.timePassed() > 1000);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        // can not get precise time
        // time has passed more than 2000 + 1000
        assertTrue(mScroller.timePassed() > 3000);
    }
}

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

package android.os.cts;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.test.InstrumentationTestCase;

public class CountDownTimerTest extends InstrumentationTestCase {
    private Context mContext;
    private CountDownTimerTestStub mActivity;
    private long mStartTime;
    private final long OFFSET = 200;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        Intent intent = new Intent(mContext, CountDownTimerTestStub.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity = (CountDownTimerTestStub) getInstrumentation().startActivitySync(intent);
        mStartTime = System.currentTimeMillis();
        mActivity.countDownTimer.start();
    }

    public void testCountDownTimer() {
        int count = (int) (mActivity.MILLISINFUTURE / mActivity.INTERVAL);
        final long TIMEOUT_MSEC = mActivity.MILLISINFUTURE + mActivity.INTERVAL + OFFSET * count;
        waitForAction(TIMEOUT_MSEC);
        assertTrue(mActivity.onFinished);
        assertEqualsTickTime(mActivity.tickTimes, OFFSET);
    }

    public void testCountDownTimerCancel() {
        final long DELAY_MSEC = mActivity.INTERVAL + OFFSET;
        assertTrue(DELAY_MSEC < mActivity.MILLISINFUTURE);
        waitForAction(DELAY_MSEC);
        assertFalse(mActivity.onFinished);
        mActivity.countDownTimer.cancel();
        final long TIMEOUT_MSEC = mActivity.MILLISINFUTURE + mActivity.INTERVAL;
        waitForAction(TIMEOUT_MSEC);
        assertFalse(mActivity.onFinished);
        // it will call onTick after start countDownTimer, so count plus 1;
        int count = Long.valueOf(DELAY_MSEC / mActivity.INTERVAL).intValue() + 1;
        assertEquals(count, mActivity.tickTimes.size());
        assertEqualsTickTime(mActivity.tickTimes, OFFSET);
    }

    private void assertEqualsTickTime(ArrayList<Long> tickTimes, long offset) {
        for (int i = 0; i < tickTimes.size(); i++) {
            long tickTime = tickTimes.get(i);
            long expecTickTime = mStartTime + i * mActivity.INTERVAL;
            assertTrue(Math.abs(expecTickTime - tickTime) < offset);
        }
    }

    /**
     * Wait for an action to complete.
     *
     * @param time The time to wait.
     */
    private void waitForAction(long time) {
        try {
            Thread.sleep(time);
        } catch (final InterruptedException e) {
            fail("error occurs when wait for an action: " + e.toString());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mActivity != null) {
            mActivity.finish();
        }
    }

}

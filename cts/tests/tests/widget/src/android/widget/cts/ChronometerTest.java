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

package android.widget.cts;


import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;

/**
 * Test {@link Chronometer}.
 */
public class ChronometerTest extends ActivityInstrumentationTestCase2<ChronometerStubActivity> {
    private ChronometerStubActivity mActivity;
    public ChronometerTest() {
        super("com.android.cts.stub", ChronometerStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructor() {
        new Chronometer(mActivity);

        new Chronometer(mActivity, null);

        new Chronometer(mActivity, null, 0);
    }

    @UiThreadTest
    public void testAccessBase() {
        Chronometer chronometer = mActivity.getChronometer();
        CharSequence oldText = chronometer.getText();

        int expected = 100000;
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());

        expected = 100;
        oldText = chronometer.getText();
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());

        expected = -1;
        oldText = chronometer.getText();
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());

        expected = Integer.MAX_VALUE;
        oldText = chronometer.getText();
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());
    }

    @UiThreadTest
    public void testAccessFormat() {
        Chronometer chronometer = mActivity.getChronometer();
        String expected = "header-%S-trail";

        chronometer.setFormat(expected);
        assertEquals(expected, chronometer.getFormat());

        chronometer.start();
        String text = chronometer.getText().toString();
        assertTrue(text.startsWith("header"));
        assertTrue(text.endsWith("trail"));
    }

    public void testFoo() {
        // Do not test these APIs. They are callbacks which:
        // 1. The callback machanism has been tested in super class
        // 2. The functionality is implmentation details, no need to test
    }

    public void testStartAndStop() throws Throwable {
        final Chronometer chronometer = mActivity.getChronometer();

        // we will check the text is really updated every 1000ms after start,
        // so we need sleep a moment to wait wait this time. The sleep code shouldn't
        // in the same thread with UI, that's why we use runOnMainSync here.
        runTestOnUiThread(new Runnable() {
            public void run() {
                // the text will update immediately when call start.
                CharSequence expected = chronometer.getText();
                chronometer.start();
                assertNotSame(expected, chronometer.getText());
            }
        });
        getInstrumentation().waitForIdleSync();
        CharSequence expected = chronometer.getText();
        Thread.sleep(1500);
        assertFalse(expected.equals(chronometer.getText()));

        // we will check the text is really NOT updated anymore every 1000ms after stop,
        // so we need sleep a moment to wait wait this time. The sleep code shouldn't
        // in the same thread with UI, that's why we use runOnMainSync here.
        runTestOnUiThread(new Runnable() {
            public void run() {
                // the text will never be updated when call stop.
                CharSequence expected = chronometer.getText();
                chronometer.stop();
                assertSame(expected, chronometer.getText());
            }
        });
        getInstrumentation().waitForIdleSync();
        expected = chronometer.getText();
        Thread.sleep(1500);
        assertTrue(expected.equals(chronometer.getText()));
    }

    public void testAccessOnChronometerTickListener() throws Throwable {
        final Chronometer chronometer = mActivity.getChronometer();
        final MockOnChronometerTickListener listener = new MockOnChronometerTickListener();

        runTestOnUiThread(new Runnable() {
            public void run() {
                chronometer.setOnChronometerTickListener(listener);
                chronometer.start();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(listener, chronometer.getOnChronometerTickListener());
        assertTrue(listener.hasCalledOnChronometerTick());
        listener.reset();
        Thread.sleep(1500);
        assertTrue(listener.hasCalledOnChronometerTick());
    }

    private static class MockOnChronometerTickListener implements OnChronometerTickListener {
        private boolean mCalledOnChronometerTick = false;

        public void onChronometerTick(Chronometer chronometer) {
            mCalledOnChronometerTick = true;
        }

        public boolean hasCalledOnChronometerTick() {
            return mCalledOnChronometerTick;
        }

        public void reset() {
            mCalledOnChronometerTick = false;
        }
    }
}

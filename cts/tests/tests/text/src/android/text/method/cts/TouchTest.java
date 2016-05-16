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

package android.text.method.cts;


import android.app.Activity;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.Touch;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.TextView;

public class TouchTest extends ActivityInstrumentationTestCase2<StubActivity> {
    private Activity mActivity;
    private static final String LONG_TEXT = "Scrolls the specified widget to the specified " +
            "coordinates, except constrains the X scrolling position to the horizontal regions " +
            "of the text that will be visible after scrolling to the specified Y position." +
            "This is the description of the test.";

    private boolean mReturnFromTouchEvent;

    public TouchTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testScrollTo() throws Throwable {
        final TextView tv = new TextView(mActivity);
        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(tv);
                tv.setSingleLine(true);
                tv.setLines(2);
            }
        });
        getInstrumentation().waitForIdleSync();
        TextPaint paint = tv.getPaint();
        final Layout layout = tv.getLayout();

        runTestOnUiThread(new Runnable() {
            public void run() {
                tv.setText(LONG_TEXT);
            }
        });
        getInstrumentation().waitForIdleSync();

        // get the total length of string
        final int width = getTextWidth(LONG_TEXT, paint);

        runTestOnUiThread(new Runnable() {
            public void run() {
                Touch.scrollTo(tv, layout, width - tv.getWidth() - 1, 0);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(width - tv.getWidth() - 1, tv.getScrollX());
        assertEquals(0, tv.getScrollY());

        // the X to which scroll is greater than the total length of string.
        runTestOnUiThread(new Runnable() {
            public void run() {
                Touch.scrollTo(tv, layout, width + 100, 5);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(width - tv.getWidth(), tv.getScrollX(), 1.0f);
        assertEquals(5, tv.getScrollY());

        runTestOnUiThread(new Runnable() {
            public void run() {
                Touch.scrollTo(tv, layout, width - 10, 5);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(width - tv.getWidth(), tv.getScrollX(), 1.0f);
        assertEquals(5, tv.getScrollY());
    }

    public void testOnTouchEvent() throws Throwable {
        final TextView tv = new TextView(mActivity);

        // Create a string that is wider than the screen.
        DisplayMetrics metrics = mActivity.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        TextPaint paint = tv.getPaint();
        String text = LONG_TEXT;
        int textWidth = Math.round(paint.measureText(text));
        while (textWidth < screenWidth) {
            text += LONG_TEXT;
            textWidth = Math.round(paint.measureText(text));
        }

        // Drag the difference between the text width and the screen width.
        int dragAmount = Math.min(screenWidth, textWidth - screenWidth);
        assertTrue(dragAmount > 0);
        final String finalText = text;
        final SpannableString spannable = new SpannableString(finalText);
        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(tv);
                tv.setSingleLine(true);
                tv.setText(finalText);
            }
        });
        getInstrumentation().waitForIdleSync();

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        final MotionEvent event1 = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, dragAmount, 0, 0);
        final MotionEvent event2 = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_MOVE, 0, 0, 0);
        final MotionEvent event3 = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_UP, 0, 0, 0);
        assertEquals(0, tv.getScrollX());
        assertEquals(0, tv.getScrollY());
        mReturnFromTouchEvent = false;
        runTestOnUiThread(new Runnable() {
            public void run() {
                mReturnFromTouchEvent = Touch.onTouchEvent(tv, spannable, event1);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mReturnFromTouchEvent);
        // TextView has not been scrolled.
        assertEquals(0, tv.getScrollX());
        assertEquals(0, tv.getScrollY());
        assertEquals(0, Touch.getInitialScrollX(tv, spannable));
        assertEquals(0, Touch.getInitialScrollY(tv, spannable));

        mReturnFromTouchEvent = false;
        runTestOnUiThread(new Runnable() {
            public void run() {
                mReturnFromTouchEvent = Touch.onTouchEvent(tv, spannable, event2);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mReturnFromTouchEvent);
        // TextView has been scrolled.
        assertEquals(dragAmount, tv.getScrollX());
        assertEquals(0, tv.getScrollY());
        assertEquals(0, Touch.getInitialScrollX(tv, spannable));
        assertEquals(0, Touch.getInitialScrollY(tv, spannable));

        mReturnFromTouchEvent = false;
        runTestOnUiThread(new Runnable() {
            public void run() {
                mReturnFromTouchEvent = Touch.onTouchEvent(tv, spannable, event3);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mReturnFromTouchEvent);
        // TextView has not been scrolled.
        assertEquals(dragAmount, tv.getScrollX());
        assertEquals(0, tv.getScrollY());
        assertEquals(-1, Touch.getInitialScrollX(tv, spannable));
        assertEquals(-1, Touch.getInitialScrollY(tv, spannable));
    }

    private int getTextWidth(String str, TextPaint paint) {
        float totalWidth = 0f;
        float[] widths = new float[str.length()];
        paint.getTextWidths(str, widths);
        for (float f : widths) {
            totalWidth += f;
        }
        return (int) totalWidth;
    }
}

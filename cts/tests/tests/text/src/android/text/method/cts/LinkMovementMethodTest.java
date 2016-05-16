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


import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * Test {@link LinkMovementMethod}. The class is an implementation of interface
 * {@link MovementMethod}. The typical usage of {@link MovementMethod} is tested in
 * {@link android.widget.cts.TextViewTest} and this test case is only focused on the
 * implementation of the methods.
 *
 * @see android.widget.cts.TextViewTest
 */
public class LinkMovementMethodTest extends
        ActivityInstrumentationTestCase2<StubActivity> {
    private static final String CONTENT = "clickable\nunclickable\nclickable";

    private LinkMovementMethod mMethod;

    private TextView mView;

    private Spannable mSpannable;

    private MockClickableSpan mClickable0;

    private MockClickableSpan mClickable1;

    public LinkMovementMethodTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMethod = new LinkMovementMethod();

        // Set the content view with a text view which contains 3 lines,
        mView = new TextView(getActivity());
        mView.setText(CONTENT, BufferType.SPANNABLE);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                getActivity().setContentView(mView);
            }
        });
        getInstrumentation().waitForIdleSync();

        mSpannable = (Spannable) mView.getText();
        // make first line clickable
        mClickable0 = markClickable(0, CONTENT.indexOf('\n'));
        // make last line clickable
        mClickable1 = markClickable(CONTENT.lastIndexOf('\n'), CONTENT.length());
    }

    public void testConstructor() {
        new LinkMovementMethod();
    }

    public void testGetInstance() {
        MovementMethod method0 = LinkMovementMethod.getInstance();
        assertTrue(method0 instanceof LinkMovementMethod);

        MovementMethod method1 = LinkMovementMethod.getInstance();
        assertNotNull(method1);
        assertSame(method0, method1);
    }

    public void testOnTakeFocus() {
        LinkMovementMethod method = new LinkMovementMethod();
        Spannable spannable = new SpannableString("test sequence");
        Selection.setSelection(spannable, 0, spannable.length());

        assertSelection(spannable, 0, spannable.length());
        assertTrue("Expected at least 2 spans", 2 <= spannable.getSpans(0, spannable.length(), Object.class).length);
        method.onTakeFocus(null, spannable, View.FOCUS_UP);
        assertSelection(spannable, -1);
        assertEquals(1, spannable.getSpans(0, spannable.length(), Object.class).length);
        Object span = spannable.getSpans(0, spannable.length(), Object.class)[0];
        assertEquals(0, spannable.getSpanStart(span));
        assertEquals(0, spannable.getSpanEnd(span));
        assertEquals(Spanned.SPAN_POINT_POINT, spannable.getSpanFlags(span));

        // focus forwards
        Selection.setSelection(spannable, 0, spannable.length());
        assertSelection(spannable, 0, spannable.length());
        assertTrue("Expected at least 3 spans", 3 <= spannable.getSpans(0, spannable.length(), Object.class).length);
        method.onTakeFocus(null, spannable, View.FOCUS_RIGHT);
        assertSelection(spannable, -1);
        assertEquals(0, spannable.getSpans(0, spannable.length(), Object.class).length);

        // force adding span while focus backward
        method.onTakeFocus(null, spannable, View.FOCUS_UP);
        // param direction is unknown(0)
        Selection.setSelection(spannable, 0, spannable.length());
        assertSelection(spannable, 0, spannable.length());
        assertTrue("Expected at least 3 spans", 3 <= spannable.getSpans(0, spannable.length(), Object.class).length);
        method.onTakeFocus(null, spannable, 0);
        assertSelection(spannable, -1);
        assertEquals(0, spannable.getSpans(0, spannable.length(), Object.class).length);

        // null parameters
        try {
            method.onTakeFocus(new TextView(getActivity()), null, View.FOCUS_RIGHT);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testOnKeyDown() {
        // no selection
        assertSelection(mSpannable, -1);
        mClickable0.reset();
        mClickable1.reset();
        assertFalse(mMethod.onKeyDown(mView, mSpannable, KeyEvent.KEYCODE_ENTER,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
        assertFalse(mClickable0.hasCalledOnClick());
        assertFalse(mClickable1.hasCalledOnClick());

        // select clickable0
        Selection.setSelection(mSpannable, mSpannable.getSpanStart(mClickable0),
                mSpannable.getSpanEnd(mClickable0));
        mClickable0.reset();
        mClickable1.reset();
        assertFalse(mMethod.onKeyDown(mView, mSpannable, KeyEvent.KEYCODE_DPAD_CENTER,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)));
        assertTrue(mClickable0.hasCalledOnClick());
        assertFalse(mClickable1.hasCalledOnClick());

        // select unclickable
        Selection.setSelection(mSpannable, mSpannable.getSpanEnd(mClickable0),
                mSpannable.getSpanStart(mClickable1));
        mClickable0.reset();
        mClickable1.reset();
        assertFalse(mMethod.onKeyDown(mView, mSpannable, KeyEvent.KEYCODE_ENTER,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
        assertFalse(mClickable0.hasCalledOnClick());
        assertFalse(mClickable1.hasCalledOnClick());

        // select all clickables(more than one)
        Selection.selectAll(mSpannable);
        mClickable0.reset();
        mClickable1.reset();
        assertFalse(mMethod.onKeyDown(mView, mSpannable, KeyEvent.KEYCODE_DPAD_CENTER,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)));
        assertFalse(mClickable0.hasCalledOnClick());
        assertFalse(mClickable1.hasCalledOnClick());

        // part of selection is clickable
        Selection.setSelection(mSpannable, mSpannable.getSpanEnd(mClickable0),
                mSpannable.getSpanEnd(mClickable1));
        mClickable0.reset();
        mClickable1.reset();
        assertFalse(mMethod.onKeyDown(mView, mSpannable, KeyEvent.KEYCODE_DPAD_CENTER,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)));
        assertFalse(mClickable0.hasCalledOnClick());
        assertTrue(mClickable1.hasCalledOnClick());

        // selection contains only clickable1 and repeat count of the event is not 0
        Selection.setSelection(mSpannable, mSpannable.getSpanEnd(mClickable0),
        mSpannable.getSpanEnd(mClickable1));
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER, 1);

        mClickable0.reset();
        mClickable1.reset();
        assertFalse(mMethod.onKeyDown(mView, mSpannable, KeyEvent.KEYCODE_DPAD_CENTER, event));
        assertFalse(mClickable0.hasCalledOnClick());
        assertFalse(mClickable1.hasCalledOnClick());

        // null parameters
        try {
            mMethod.onKeyDown(null, mSpannable, KeyEvent.KEYCODE_DPAD_CENTER,
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
            fail("The method did not throw NullPointerException when param view is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mMethod.onKeyDown(mView, null, KeyEvent.KEYCODE_DPAD_CENTER,
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mMethod.onKeyDown(mView, mSpannable, KeyEvent.KEYCODE_DPAD_CENTER, null);
            fail("The method did not throw NullPointerException when param keyEvent is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testOnKeyUp() {
        LinkMovementMethod method = new LinkMovementMethod();
        // always returns false
        assertFalse(method.onKeyUp(null, null, 0, null));
        assertFalse(method.onKeyUp(new TextView(getActivity()), null, 0, null));
        assertFalse(method.onKeyUp(null, new SpannableString("blahblah"), 0, null));
        assertFalse(method.onKeyUp(null, null, KeyEvent.KEYCODE_0,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0)));
    }

    @UiThreadTest
    public void testOnTouchEvent() {
        assertSelection(mSpannable, -1);

        // press on first line (Clickable)
        assertTrue(pressOnLine(0));
        assertSelectClickableLeftToRight(mSpannable, mClickable0);

        // release on first line
        assertFalse(mClickable0.hasCalledOnClick());
        assertTrue(releaseOnLine(0));
        assertTrue(mClickable0.hasCalledOnClick());

        // press on second line (unclickable)
        assertSelectClickableLeftToRight(mSpannable, mClickable0);
        // just clear selection
        pressOnLine(1);
        assertSelection(mSpannable, -1);

        // press on last line  (Clickable)
        assertTrue(pressOnLine(2));
        assertSelectClickableLeftToRight(mSpannable, mClickable1);

        // release on last line
        assertFalse(mClickable1.hasCalledOnClick());
        assertTrue(releaseOnLine(2));
        assertTrue(mClickable1.hasCalledOnClick());

        // release on second line (unclickable)
        assertSelectClickableLeftToRight(mSpannable, mClickable1);
        // just clear selection
        releaseOnLine(1);
        assertSelection(mSpannable, -1);

        // null parameters
        long now = SystemClock.uptimeMillis();
        int y = (mView.getLayout().getLineTop(1) + mView.getLayout().getLineBottom(1)) / 2;
        try {
            mMethod.onTouchEvent(null, mSpannable,
                    MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 5, y, 0));
            fail("The method did not throw NullPointerException when param view is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mMethod.onTouchEvent(mView, null,
                    MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 5, y, 0));
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mMethod.onTouchEvent(mView, mSpannable, null);
            fail("The method did not throw NullPointerException when param keyEvent is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testUp() {
        final MyLinkMovementMethod method = new MyLinkMovementMethod();
        assertSelection(mSpannable, -1);

        assertTrue(method.up(mView, mSpannable));
        assertSelectClickableRightToLeft(mSpannable, mClickable1);

        assertTrue(method.up(mView, mSpannable));
        assertSelectClickableRightToLeft(mSpannable, mClickable0);

        assertFalse(method.up(mView, mSpannable));
        assertSelectClickableRightToLeft(mSpannable, mClickable0);

        // null parameters
        try {
            method.up(null, mSpannable);
            fail("The method did not throw NullPointerException when param view is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            method.up(mView, null);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testDown() {
        final MyLinkMovementMethod method = new MyLinkMovementMethod();
        assertSelection(mSpannable, -1);

        assertTrue(method.down(mView, mSpannable));
        assertSelectClickableLeftToRight(mSpannable, mClickable0);

        assertTrue(method.down(mView, mSpannable));
        assertSelectClickableLeftToRight(mSpannable, mClickable1);

        assertFalse(method.down(mView, mSpannable));
        assertSelectClickableLeftToRight(mSpannable, mClickable1);

        // null parameters
        try {
            method.down(null, mSpannable);
            fail("The method did not throw NullPointerException when param view is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            method.down(mView, null);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testLeft() {
        final MyLinkMovementMethod method = new MyLinkMovementMethod();
        assertSelection(mSpannable, -1);

        assertTrue(method.left(mView, mSpannable));
        assertSelectClickableRightToLeft(mSpannable, mClickable1);

        assertTrue(method.left(mView, mSpannable));
        assertSelectClickableRightToLeft(mSpannable, mClickable0);

        assertFalse(method.left(mView, mSpannable));
        assertSelectClickableRightToLeft(mSpannable, mClickable0);

        // null parameters
        try {
            method.left(null, mSpannable);
            fail("The method did not throw NullPointerException when param view is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            method.left(mView, null);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testRight() {
        final MyLinkMovementMethod method = new MyLinkMovementMethod();
        assertSelection(mSpannable, -1);

        assertTrue(method.right(mView, mSpannable));
        assertSelectClickableLeftToRight(mSpannable, mClickable0);

        assertTrue(method.right(mView, mSpannable));
        assertSelectClickableLeftToRight(mSpannable, mClickable1);

        assertFalse(method.right(mView, mSpannable));
        assertSelectClickableLeftToRight(mSpannable, mClickable1);

        // null parameters
        try {
            method.right(null, mSpannable);
            fail("The method did not throw NullPointerException when param view is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            method.right(mView, null);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testMoveAroundUnclickable() {
        final MyLinkMovementMethod method = new MyLinkMovementMethod();
        mSpannable.removeSpan(mClickable0);
        mSpannable.removeSpan(mClickable1);
        assertSelection(mSpannable, -1);

        assertFalse(method.up(mView, mSpannable));
        assertSelection(mSpannable, -1);

        assertFalse(method.down(mView, mSpannable));
        assertSelection(mSpannable, -1);

        assertFalse(method.left(mView, mSpannable));
        assertSelection(mSpannable, -1);

        assertFalse(method.right(mView, mSpannable));
        assertSelection(mSpannable, -1);
    }

    public void testInitialize() {
        LinkMovementMethod method = new LinkMovementMethod();
        Spannable spannable = new SpannableString("test sequence");
        method.onTakeFocus(null, spannable, View.FOCUS_UP);
        Selection.setSelection(spannable, 0, spannable.length());

        assertSelection(spannable, 0, spannable.length());
        assertTrue("Expected at least 3 spans", 3 <= spannable.getSpans(0, spannable.length(), Object.class).length);
        method.initialize(null, spannable);
        assertSelection(spannable, -1);
        assertEquals(0, spannable.getSpans(0, spannable.length(), Object.class).length);

        try {
            method.initialize(mView, null);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    private MockClickableSpan markClickable(final int start, final int end) {
        final MockClickableSpan clickableSpan = new MockClickableSpan();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                mSpannable.setSpan(clickableSpan, start, end, Spanned.SPAN_MARK_MARK);
            }
        });
        getInstrumentation().waitForIdleSync();
        return clickableSpan;
    }

    private boolean performMotionOnLine(int line, int action) {
        int x = (mView.getLayout().getLineStart(line) + mView.getLayout().getLineEnd(line)) / 2;
        int y = (mView.getLayout().getLineTop(line) + mView.getLayout().getLineBottom(line)) / 2;
        long now = SystemClock.uptimeMillis();

        return mMethod.onTouchEvent(mView, mSpannable,
                MotionEvent.obtain(now, now, action, x, y, 0));
    }

    private boolean pressOnLine(int line) {
        return performMotionOnLine(line, MotionEvent.ACTION_DOWN);
    }

    private boolean releaseOnLine(int line) {
        return performMotionOnLine(line, MotionEvent.ACTION_UP);
    }

    private void assertSelection(Spannable spannable, int start, int end) {
        assertEquals(start, Selection.getSelectionStart(spannable));
        assertEquals(end, Selection.getSelectionEnd(spannable));
    }

    private void assertSelection(Spannable spannable, int position) {
        assertSelection(spannable, position, position);
    }

    private void assertSelectClickableLeftToRight(Spannable spannable,
            MockClickableSpan clickableSpan) {
        assertSelection(spannable, spannable.getSpanStart(clickableSpan),
                spannable.getSpanEnd(clickableSpan));
    }

    private void assertSelectClickableRightToLeft(Spannable spannable,
            MockClickableSpan clickableSpan) {
        assertSelection(spannable,  spannable.getSpanEnd(clickableSpan),
                spannable.getSpanStart(clickableSpan));
    }

    private static class MyLinkMovementMethod extends LinkMovementMethod {
        @Override
        protected boolean down(TextView widget, Spannable buffer) {
            return super.down(widget, buffer);
        }

        @Override
        protected boolean left(TextView widget, Spannable buffer) {
            return super.left(widget, buffer);
        }

        @Override
        protected boolean right(TextView widget, Spannable buffer) {
            return super.right(widget, buffer);
        }

        @Override
        protected boolean up(TextView widget, Spannable buffer) {
            return super.up(widget, buffer);
        }
    }

    private static class MockClickableSpan extends ClickableSpan {
        private boolean mHasCalledOnClick;

        @Override
        public void onClick(View widget) {
            mHasCalledOnClick = true;
        }

        public boolean hasCalledOnClick() {
            return mHasCalledOnClick;
        }

        public void reset() {
            mHasCalledOnClick = false;
        }
    }
}

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
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MetaKeyKeyListener;
import android.text.method.MovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * Test {@link ArrowKeyMovementMethod}. The class is an implementation of interface
 * {@link MovementMethod}. The typical usage of {@link MovementMethod} is tested in
 * {@link android.widget.cts.TextViewTest} and this test case is only focused on the
 * implementation of the methods.
 *
 * @see android.widget.cts.TextViewTest
 */
public class ArrowKeyMovementMethodTest extends ActivityInstrumentationTestCase2<StubActivity> {
    private static final String THREE_LINES_TEXT = "first line\nsecond line\nlast line";
    private static final int END_OF_ALL_TEXT = THREE_LINES_TEXT.length();
    private static final int END_OF_1ST_LINE = THREE_LINES_TEXT.indexOf('\n');
    private static final int START_OF_2ND_LINE = END_OF_1ST_LINE + 1;
    private static final int END_OF_2ND_LINE = THREE_LINES_TEXT.indexOf('\n', START_OF_2ND_LINE);
    private static final int START_OF_3RD_LINE = END_OF_2ND_LINE + 1;
    private static final int SPACE_IN_2ND_LINE = THREE_LINES_TEXT.indexOf(' ', START_OF_2ND_LINE);
    private TextView mTextView;
    private ArrowKeyMovementMethod mArrowKeyMovementMethod;
    private Editable mEditable;
    private MyMetaKeyKeyListener mMetaListener;

    public ArrowKeyMovementMethodTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMetaListener = new MyMetaKeyKeyListener();
        mArrowKeyMovementMethod = new ArrowKeyMovementMethod();

        initTextViewWithNullLayout();

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                getActivity().setContentView(mTextView);
                mTextView.setFocusable(true);
                mTextView.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertNotNull(mTextView.getLayout());
        assertTrue(mTextView.isFocused());
    }

    public void testConstructor() {
        new ArrowKeyMovementMethod();
    }

    public void testCanSelectArbitrarily() {
        assertTrue(new ArrowKeyMovementMethod().canSelectArbitrarily());
    }

    public void testGetInstance() {
        MovementMethod method0 = ArrowKeyMovementMethod.getInstance();
        assertNotNull(method0);

        MovementMethod method1 = ArrowKeyMovementMethod.getInstance();
        assertNotNull(method1);
        assertSame(method0, method1);
    }

    public void testOnTakeFocus() throws Throwable {
        /*
         * The following assertions depend on whether the TextView has a layout.
         * The text view will not get layout in setContent method but in other
         * handler's function. Assertion which is following the setContent will
         * not get the expecting result. It have to wait all the handlers'
         * operations on the UiTread to finish. So all these cases are divided
         * into several steps, setting the content at first, waiting the layout,
         * and checking the assertion at last.
         */
        assertSelection(-1);
        runTestOnUiThread(new Runnable() {
            public void run() {
                Selection.removeSelection(mEditable);
                mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_UP);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertSelection(END_OF_ALL_TEXT);

        runTestOnUiThread(new Runnable() {
            public void run() {
                Selection.removeSelection(mEditable);
                mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_LEFT);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertSelection(END_OF_ALL_TEXT);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mTextView.setSingleLine();
            }
        });
        // wait until the textView gets layout
        getInstrumentation().waitForIdleSync();
        assertNotNull(mTextView.getLayout());
        assertEquals(1, mTextView.getLayout().getLineCount());

        runTestOnUiThread(new Runnable() {
            public void run() {
                Selection.removeSelection(mEditable);
                mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_UP);
            }
        });
        assertSelection(END_OF_ALL_TEXT);

        runTestOnUiThread(new Runnable() {
            public void run() {
                Selection.removeSelection(mEditable);
                mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_LEFT);
            }
        });
        assertSelection(END_OF_ALL_TEXT);
    }

    public void testOnTakeFoucusWithNullLayout() {
        initTextViewWithNullLayout();
        assertSelectEndOfContent();
    }

    public void testOnTakeFocusWithNullParameters() {
        initTextViewWithNullLayout();
        try {
            mArrowKeyMovementMethod.onTakeFocus(null, mEditable, View.FOCUS_DOWN);
            fail("The method did not throw NullPointerException when param textView is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mArrowKeyMovementMethod.onTakeFocus(mTextView, null, View.FOCUS_DOWN);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testOnKeyDownWithKeyCodeUp() {
        // first line
        // second |line
        // last line
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressBothShiftAlt();
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_UP, event));
        // |first line
        // second |line
        // last line
        assertSelection(SPACE_IN_2ND_LINE, 0);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressShift();
        event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_SHIFT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_UP, event));
        // first lin|e
        // second |line
        // last line
        assertEquals(SPACE_IN_2ND_LINE, Selection.getSelectionStart(mEditable));
        int correspondingIn1stLine = Selection.getSelectionEnd(mEditable);
        assertTrue(correspondingIn1stLine >= 0);
        assertTrue(correspondingIn1stLine <= END_OF_1ST_LINE);

        pressShift();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_UP, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_UP)));
        // |first line
        // second |line
        // last line
        assertSelection(SPACE_IN_2ND_LINE, 0);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressAlt();
        event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_ALT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_UP, event));
        // |first line
        // second line
        // last line
        assertSelection(0);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        MetaKeyKeyListener.resetMetaState(mEditable);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_UP, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_UP)));
        // first lin|e
        // second line
        // last line
        assertSelection(correspondingIn1stLine);

        assertFalse(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_UP, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_UP)));
        // first lin|e
        // second line
        // last line
        assertSelection(correspondingIn1stLine);
    }

    @UiThreadTest
    public void testOnKeyDownWithKeyCodeDown() {
        // first line
        // second |line
        // last line
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressBothShiftAlt();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN)));
        // first line
        // second |line
        // last line|
        assertSelection(SPACE_IN_2ND_LINE, END_OF_ALL_TEXT);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressShift();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN)));
        // first line
        // second |line
        // last lin|e
        assertEquals(SPACE_IN_2ND_LINE, Selection.getSelectionStart(mEditable));
        int correspondingIn3rdLine = Selection.getSelectionEnd(mEditable);
        assertTrue(correspondingIn3rdLine >= START_OF_3RD_LINE);
        assertTrue(correspondingIn3rdLine <= END_OF_ALL_TEXT);

        pressShift();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN)));
        // first line
        // second |line
        // last line|
        assertSelection(SPACE_IN_2ND_LINE, END_OF_ALL_TEXT);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressAlt();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN)));
        // first line
        // second line
        // last line|
        assertSelection(END_OF_ALL_TEXT);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        MetaKeyKeyListener.resetMetaState(mEditable);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN)));
        // first line
        // second line
        // last lin|e
        assertSelection(correspondingIn3rdLine);

        assertFalse(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN)));
        // first line
        // second line
        // last lin|e
        assertSelection(correspondingIn3rdLine);
    }

    @UiThreadTest
    public void testOnKeyDownWithKeyCodeLeft() {
        // first line
        // second |line
        // last line
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressBothShiftAlt();
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, event));
        // first line
        // |second |line
        // last line
        assertSelection(SPACE_IN_2ND_LINE, START_OF_2ND_LINE);

        pressBothShiftAlt();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, event));
        // first line
        // |second |line
        // last line
        assertSelection(SPACE_IN_2ND_LINE, START_OF_2ND_LINE);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressShift();
        event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_SHIFT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, event));
        // first line
        // second| |line
        // last line
        assertSelection(SPACE_IN_2ND_LINE, SPACE_IN_2ND_LINE - 1);

        pressShift();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT)));
        // first line
        // secon|d |line
        // last line
        assertSelection(SPACE_IN_2ND_LINE, SPACE_IN_2ND_LINE - 2);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressAlt();
        event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_ALT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, event));
        // first line
        // |second line
        // last line
        assertSelection(START_OF_2ND_LINE);

        pressAlt();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, event));
        // first line
        // |second line
        // last line
        assertSelection(START_OF_2ND_LINE);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        MetaKeyKeyListener.resetMetaState(mEditable);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT)));
        // first line
        // second| line
        // last line
        assertSelection(SPACE_IN_2ND_LINE - 1);

        Selection.setSelection(mEditable, START_OF_2ND_LINE);
        // first line
        // |second line
        // last line
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT)));
        // first line|
        // second line
        // last line
        assertSelection(END_OF_1ST_LINE);
    }

    @UiThreadTest
    public void testOnKeyDownWithKeyCodeRight() {
        // first line
        // second |line
        // last line
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressBothShiftAlt();
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, event));
        // first line
        // second |line|
        // last line
        assertSelection(SPACE_IN_2ND_LINE, END_OF_2ND_LINE);

        pressBothShiftAlt();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, event));
        // first line
        // second |line|
        // last line
        assertSelection(SPACE_IN_2ND_LINE, END_OF_2ND_LINE);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressShift();
        event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_SHIFT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, event));
        // first line
        // second |l|ine
        // last line
        assertSelection(SPACE_IN_2ND_LINE, SPACE_IN_2ND_LINE + 1);

        pressShift();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, event));
        // first line
        // second |li|ne
        // last line
        assertSelection(SPACE_IN_2ND_LINE, SPACE_IN_2ND_LINE + 2);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        pressAlt();
        event = new KeyEvent(0, 0, KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_ALT_ON);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, event));
        // first line
        // second line|
        // last line
        assertSelection(END_OF_2ND_LINE);

        pressAlt();
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, event));
        // first line
        // second line|
        // last line
        assertSelection(END_OF_2ND_LINE);

        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        MetaKeyKeyListener.resetMetaState(mEditable);
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT)));
        // first line
        // second l|ine
        // last line
        assertSelection(SPACE_IN_2ND_LINE + 1);

        Selection.setSelection(mEditable, END_OF_2ND_LINE);
        // first line
        // second line|
        // last line
        assertTrue(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT)));
        // first line
        // second line
        // |last line
        assertSelection(START_OF_3RD_LINE);
    }

    public void testOnKeyDownWithNullLayout() {
        initTextViewWithNullLayout();
        try {
            mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable, KeyEvent.KEYCODE_DPAD_RIGHT,
                    null);
            fail("The method did not throw NullPointerException when layout of the view is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    public void testOnKeyOther() {
        // first line
        // second |line
        // last line
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);

        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_DPAD_CENTER, 2)));
        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_0, 2)));
        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_E, 2)));
        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_UNKNOWN, 2)));

        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0)));
        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0)));
        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0)));
        assertFalse(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0)));

        // only repeat arrow key events get handled
        assertTrue(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_DPAD_UP, 2)));
        assertTrue(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_DPAD_DOWN, 2)));
        assertTrue(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_DPAD_LEFT, 2)));
        assertTrue(mArrowKeyMovementMethod.onKeyOther(mTextView, mEditable,
                new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_DPAD_RIGHT, 2)));
    }

    @UiThreadTest
    public void testOnKeyDownWithOtherKeyCode() {
        // first line
        // second |line
        // last line
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);

        assertFalse(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_DPAD_CENTER, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_CENTER)));
        assertFalse(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_0, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0)));
        assertFalse(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_E, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_E)));
        assertFalse(mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable,
                KeyEvent.KEYCODE_UNKNOWN, new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_UNKNOWN)));
    }

    @UiThreadTest
    public void testOnTouchEvent() throws Throwable {
        long now = SystemClock.currentThreadTimeMillis();
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        assertFalse(mArrowKeyMovementMethod.onTouchEvent(mTextView, mEditable,
                MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 1, 1, 0)));
        assertSelection(SPACE_IN_2ND_LINE);

        assertFalse(mArrowKeyMovementMethod.onTouchEvent(mTextView, mEditable,
                MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 1, 1,
                        KeyEvent.META_SHIFT_ON)));
        assertSelection(SPACE_IN_2ND_LINE);
    }

    public void testOnTouchEventWithNullLayout() {
        initTextViewWithNullLayout();
        mTextView.setFocusable(true);
        mTextView.requestFocus();
        assertTrue(mTextView.isFocused());

        long now = SystemClock.currentThreadTimeMillis();
        assertFalse(mArrowKeyMovementMethod.onTouchEvent(mTextView, mEditable,
                    MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 1, 1, 0)));
    }

    @UiThreadTest
    public void testOnTouchEventWithoutFocus() {
        long now = SystemClock.currentThreadTimeMillis();
        Selection.setSelection(mEditable, SPACE_IN_2ND_LINE);
        assertFalse(mArrowKeyMovementMethod.onTouchEvent(mTextView, mEditable,
                MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 1, 1, 0)));
        assertSelection(SPACE_IN_2ND_LINE);
    }

    public void testOnTouchEventWithNullParameters() {
        initTextViewWithNullLayout();
        try {
            mArrowKeyMovementMethod.onTouchEvent(null, mEditable,
                    MotionEvent.obtain(0, 0, 0, 1, 1, 0));
            fail("The method did not throw NullPointerException when param textView is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mArrowKeyMovementMethod.onTouchEvent(mTextView, null,
                    MotionEvent.obtain(0, 0, 0, 1, 1, 0));
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mArrowKeyMovementMethod.onTouchEvent(mTextView, mEditable, null);
            fail("The method did not throw NullPointerException when param motionEvent is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testInitialize() {
        Spannable spannable = new SpannableString("test content");
        ArrowKeyMovementMethod method = new ArrowKeyMovementMethod();

        assertEquals(-1, Selection.getSelectionStart(spannable));
        assertEquals(-1, Selection.getSelectionEnd(spannable));
        method.initialize(null, spannable);
        assertEquals(0, Selection.getSelectionStart(spannable));
        assertEquals(0, Selection.getSelectionEnd(spannable));

        Selection.setSelection(spannable, 2);
        assertEquals(2, Selection.getSelectionStart(spannable));
        assertEquals(2, Selection.getSelectionEnd(spannable));
        method.initialize(null, spannable);
        assertEquals(0, Selection.getSelectionStart(spannable));
        assertEquals(0, Selection.getSelectionEnd(spannable));

        try {
            method.initialize(mTextView, null);
            fail("The method did not throw NullPointerException when param spannable is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testOnTrackballEven() {
        assertFalse(mArrowKeyMovementMethod.onTrackballEvent(mTextView, mEditable,
                MotionEvent.obtain(0, 0, 0, 1, 1, 0)));

        initTextViewWithNullLayout();

        assertFalse(mArrowKeyMovementMethod.onTrackballEvent(mTextView, mEditable,
                MotionEvent.obtain(0, 0, 0, 1, 1, 0)));

        assertFalse(mArrowKeyMovementMethod.onTrackballEvent(mTextView, null,
                MotionEvent.obtain(0, 0, 0, 1, 1, 0)));

        assertFalse(mArrowKeyMovementMethod.onTrackballEvent(mTextView, mEditable, null));
    }

    public void testOnKeyUp() {
        ArrowKeyMovementMethod method = new ArrowKeyMovementMethod();
        SpannableString spannable = new SpannableString("Test Content");
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        TextView view = new TextView(getActivity());

        assertFalse(method.onKeyUp(view, spannable, KeyEvent.KEYCODE_0, event));
        assertFalse(method.onKeyUp(null, null, 0, null));
        assertFalse(method.onKeyUp(null, spannable, KeyEvent.KEYCODE_0, event));
        assertFalse(method.onKeyUp(view, null, KeyEvent.KEYCODE_0, event));
        assertFalse(method.onKeyUp(view, spannable, 0, event));
        assertFalse(method.onKeyUp(view, spannable, KeyEvent.KEYCODE_0, null));
    }

    private static final String TEXT_WORDS =
            "Lorem ipsum; dolor sit \u00e4met, conse\u0ca0_\u0ca0ctetur?       Adipiscing"
            + ".elit.integ\u00e9r. Etiam    tristique\ntortor nec   ?:?    \n\n"
            + "lectus porta consequ\u00e4t...  LOReM iPSuM";

    @UiThreadTest
    public void testFollowingWordStartToEnd() {

        // NOTE: there seems to be much variation in how word boundaries are
        // navigated; the behaviors asserted here were derived from Google
        // Chrome 10.0.648.133 beta.

        initTextViewWithNullLayout(TEXT_WORDS);

        // |Lorem ipsum; dolor sit $met,
        Selection.setSelection(mEditable, 0);
        assertSelection(0);

        // Lorem| ipsum; dolor sit $met,
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(5);

        // Lorem ipsum|; dolor sit $met,
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(11);

        // Lorem ipsum; dolor| sit $met,
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(18);

        // Lorem ipsum; dolor sit| $met,
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(22);

        // $met|, conse$_$ctetur$       Adipiscing.elit.integ$r.
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(27);

        // $met, conse$_$ctetur|$       Adipiscing.elit.integ$r.
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(43);

        // TODO: enable these two additional word breaks when implemented
//        // $met, conse$_$ctetur$       Adipiscing|.elit.integ$r.
//        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
//        assertSelection(61);
//
//        // $met, conse$_$ctetur$       Adipiscing.elit|.integ$r.
//        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
//        assertSelection(66);

        // $met, conse$_$ctetur$       Adipiscing.elit.integ$r|.
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(74);

        // integ$r. Etiam|    tristique$tortor nec   ?:?    $$lectus porta
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(81);

        // integ$r. Etiam    tristique|$tortor nec   ?:?    $$lectus porta
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(94);

        // integ$r. Etiam    tristique$tortor| nec   ?:?    $$lectus porta
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(101);

        // integ$r. Etiam    tristique$tortor nec|   ?:?    $$lectus porta
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(105);

        // integ$r. Etiam    tristique$tortor nec   ?:?    $$lectus| porta
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(123);

        // $$lectus porta| consequ$t...  LOReM iPSuM
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(129);

        // $$lectus porta consequ$t|...  LOReM iPSuM
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(139);

        // $$lectus porta consequ$t...  LOReM| iPSuM
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(149);

        // $$lectus porta consequ$t...  LOReM iPSuM|
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(155);

        // keep trying to push beyond end, which should fail
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(155);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(155);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(155);

    }

    @UiThreadTest
    public void testPrecedingWordEndToStart() {

        // NOTE: there seems to be much variation in how word boundaries are
        // navigated; the behaviors asserted here were derived from Google
        // Chrome 10.0.648.133 beta.

        initTextViewWithNullLayout(TEXT_WORDS);

        // $$lectus porta consequ$t...  LOReM iPSuM|
        Selection.setSelection(mEditable, mEditable.length());
        assertSelection(155);

        // $$lectus porta consequ$t...  LOReM |iPSuM
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(150);

        // $$lectus porta consequ$t...  |LOReM iPSuM
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(144);

        // $$lectus porta |consequ$t...  LOReM iPSuM
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(130);

        // $$lectus |porta consequ$t...  LOReM iPSuM
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(124);

        // integ$r. Etiam    tristique$tortor nec   ?:?    $$|lectus
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(117);

        // integ$r. Etiam    tristique$tortor |nec   ?:?    $$lectus
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(102);

        // integ$r. Etiam    tristique$|tortor nec   ?:?    $$lectus
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(95);

        // integ$r. Etiam    |tristique$tortor nec   ?:?    $$lectus
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(85);

        // integ$r. |Etiam    tristique$tortor nec   ?:?    $$lectus
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(76);

        // TODO: enable these two additional word breaks when implemented
//        // dolor sit $met, conse$_$ctetur$       Adipiscing.elit.|integ$r.
//        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
//        assertSelection(67);
//
//        // dolor sit $met, conse$_$ctetur$       Adipiscing.|elit.integ$r.
//        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
//        assertSelection(62);

        // dolor sit $met, conse$_$ctetur$       |Adipiscing.elit.integ$r.
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(51);

        // dolor sit $met, |conse$_$ctetur$       Adipiscing.elit.integ$r.
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(29);

        // dolor sit |$met, conse$_$ctetur$       Adipiscing.elit.integ$r.
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(23);

        // Lorem ipsum; dolor |sit $met
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(19);

        // Lorem ipsum; |dolor sit $met
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(13);

        // Lorem |ipsum; dolor sit $met
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(6);

        // |Lorem ipsum; dolor sit $met
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);

        // keep trying to push before beginning, which should fail
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);

    }

    private static final String TEXT_WORDS_WITH_NUMBERS =
            "Lorem ipsum123,456.90   dolor sit.. 4-0.0=2 ADipiscing4";

    @UiThreadTest
    public void testFollowingWordStartToEndWithNumbers() {

        initTextViewWithNullLayout(TEXT_WORDS_WITH_NUMBERS);

        // |Lorem ipsum123,456.90   dolor sit.. 4-0.0=2 ADipiscing4
        Selection.setSelection(mEditable, 0);
        assertSelection(0);

        // Lorem| ipsum123,456.90   dolor sit.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(5);

        // Lorem ipsum123,456.90|   dolor sit.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(21);

        // Lorem ipsum123,456.90   dolor| sit.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(29);

        // Lorem ipsum123,456.90   dolor sit|.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(33);

        // Lorem ipsum123,456.90   dolor sit.. 4|-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(37);

        // Lorem ipsum123,456.90   dolor sit.. 4-0.0|=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(41);

        // Lorem ipsum123,456.90   dolor sit.. 4-0.0=2| ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(43);

        // Lorem ipsum123,456.90   dolor sit.. 4-0.0=2 ADipiscing4|
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(55);

        // keep trying to push beyond end, which should fail
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(55);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(55);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(55);

    }

    @UiThreadTest
    public void testFollowingWordEndToStartWithNumbers() {

        initTextViewWithNullLayout(TEXT_WORDS_WITH_NUMBERS);

        // Lorem ipsum123,456.90   dolor sit.. 4-0.0=2 ADipiscing4|
        Selection.setSelection(mEditable, mEditable.length());
        assertSelection(55);

        // Lorem ipsum123,456.90   dolor sit.. 4-0.0=2 |ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(44);

        // Lorem ipsum123,456.90   dolor sit.. 4-0.0=|2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(42);

        // Lorem ipsum123,456.90   dolor sit.. 4-|0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(38);

        // Lorem ipsum123,456.90   dolor sit.. |4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(36);

        // Lorem ipsum123,456.90   dolor |sit.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(30);

        // Lorem ipsum123,456.90   |dolor sit.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(24);

        // Lorem |ipsum123,456.90   dolor sit.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(6);

        // |Lorem ipsum123,456.90   dolor sit.. 4-0.0=2 ADipiscing4
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);

        // keep trying to push before beginning, which should fail
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);

    }

    private static final String TEXT_WORDS_WITH_1CHAR_FINAL_WORD = "abc d";

    @UiThreadTest
    public void testFollowingWordStartToEndWithOneCharFinalWord() {

        initTextViewWithNullLayout(TEXT_WORDS_WITH_1CHAR_FINAL_WORD);

        // |abc d
        Selection.setSelection(mEditable, 0);
        assertSelection(0);

        // abc| d
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(3);

        // abc d|
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertSelection(mEditable.length());

    }

    @UiThreadTest
    public void testFollowingWordEndToStartWithOneCharFinalWord() {

        initTextViewWithNullLayout(TEXT_WORDS_WITH_1CHAR_FINAL_WORD);

        // abc d|
        Selection.setSelection(mEditable, mEditable.length());
        assertSelection(5);

        // abc |d
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(4);

        // |abc d
        assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
        assertSelection(0);

    }

    @UiThreadTest
    public void testMovementFromMiddleOfWord() {

        initTextViewWithNullLayout("before word after");
        checkMoveFromInsideWord(7, 10);

        // Surrogate characters: bairkan should be considered as a standard letter
        final String BAIRKAN = "\uD800\uDF31";

        initTextViewWithNullLayout("before wo" + BAIRKAN + "rd after");
        checkMoveFromInsideWord(7, 12);

        initTextViewWithNullLayout("before " + BAIRKAN + BAIRKAN + "xx after");
        checkMoveFromInsideWord(7, 12);

        initTextViewWithNullLayout("before xx" + BAIRKAN + BAIRKAN + " after");
        checkMoveFromInsideWord(7, 12);

        initTextViewWithNullLayout("before x" + BAIRKAN + "x" + BAIRKAN + " after");
        checkMoveFromInsideWord(7, 12);

        initTextViewWithNullLayout("before " + BAIRKAN + "x" + BAIRKAN + "x after");
        checkMoveFromInsideWord(7, 12);

        initTextViewWithNullLayout("before " + BAIRKAN + BAIRKAN + BAIRKAN + " after");
        checkMoveFromInsideWord(7, 12);
    }

    private void checkMoveFromInsideWord(int wordStart, int wordEnd) {

        CharSequence text = mTextView.getText();

        // Check following always goes at the end of the word
        for (int offset = wordStart; offset != wordEnd + 1; offset++) {
            // Skip positions located between a pair of surrogate characters
            if (Character.isSurrogatePair(text.charAt(offset - 1), text.charAt(offset))) {
                continue;
            }
            Selection.setSelection(mEditable, offset);
            assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_RIGHT));
            assertSelection(wordEnd + 1);
        }

        // Check preceding always goes at the beginning of the word
        for (int offset = wordEnd + 1; offset != wordStart; offset--) {
            if (Character.isSurrogatePair(text.charAt(offset - 1), text.charAt(offset))) {
                continue;
            }
            Selection.setSelection(mEditable, offset);
            assertTrue(pressCtrlChord(KeyEvent.KEYCODE_DPAD_LEFT));
            assertSelection(wordStart);
        }
    }

    private void initTextViewWithNullLayout() {
        initTextViewWithNullLayout(THREE_LINES_TEXT);
    }

    private void initTextViewWithNullLayout(CharSequence text) {
        mTextView = new TextView(getActivity());
        mTextView.setText(text, BufferType.EDITABLE);
        assertNull(mTextView.getLayout());
        mEditable = (Editable) mTextView.getText();
    }

    private void pressMetaKey(int metakey, int expectedState) {
        mMetaListener.onKeyDown(null, mEditable, metakey, null);
        assertEquals(1, MetaKeyKeyListener.getMetaState(mEditable, expectedState));
    }

    private void pressShift() {
        MetaKeyKeyListener.resetMetaState(mEditable);
        pressMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, MetaKeyKeyListener.META_SHIFT_ON);
    }

    private void pressAlt() {
        MetaKeyKeyListener.resetMetaState(mEditable);
        pressMetaKey(KeyEvent.KEYCODE_ALT_LEFT, MetaKeyKeyListener.META_ALT_ON);
    }

    private void pressBothShiftAlt() {
        MetaKeyKeyListener.resetMetaState(mEditable);
        pressMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, MetaKeyKeyListener.META_SHIFT_ON);
        pressMetaKey(KeyEvent.KEYCODE_ALT_LEFT, MetaKeyKeyListener.META_ALT_ON);
    }

    private boolean pressCtrlChord(int keyCode) {
        final long now = System.currentTimeMillis();
        final KeyEvent keyEvent = new KeyEvent(
                now, now, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_LEFT_ON);
        return mArrowKeyMovementMethod.onKeyDown(mTextView, mEditable, keyCode, keyEvent);
    }

    private void assertSelection(int expectedPosition) {
        assertSelection(expectedPosition, expectedPosition);
    }

    private void assertSelection(int expectedStart, int expectedEnd) {
        final int actualStart = Selection.getSelectionStart(mEditable);
        final int actualEnd = Selection.getSelectionEnd(mEditable);

        assertCharSequenceIndexEquals(mEditable, expectedStart, actualStart);
        assertCharSequenceIndexEquals(mEditable, expectedEnd, actualEnd);
    }

    private static void assertCharSequenceIndexEquals(CharSequence text, int expected, int actual) {
        final String message = "expected <" + getCursorSnippet(text, expected) + "> but was <"
                + getCursorSnippet(text, actual) + ">";
        assertEquals(message, expected, actual);
    }

    private static String getCursorSnippet(CharSequence text, int index) {
        if (index >= 0 && index < text.length()) {
            return text.subSequence(Math.max(0, index - 5), index) + "|"
                    + text.subSequence(index, Math.min(text.length() - 1, index + 5));
        } else {
            return null;
        }
    }

    private void assertSelectEndOfContent() {
        Selection.removeSelection(mEditable);
        mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_DOWN);
        assertSelection(END_OF_ALL_TEXT);

        Selection.removeSelection(mEditable);
        mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_RIGHT);
        assertSelection(END_OF_ALL_TEXT);

        assertSelectEndOfContentExceptFocusForward();
    }

    private void assertSelectEndOfContentExceptFocusForward() {
        Selection.removeSelection(mEditable);
        mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_UP);
        assertSelection(END_OF_ALL_TEXT);

        Selection.removeSelection(mEditable);
        mArrowKeyMovementMethod.onTakeFocus(mTextView, mEditable, View.FOCUS_LEFT);
        assertSelection(END_OF_ALL_TEXT);
    }

    private static class MyMetaKeyKeyListener extends MetaKeyKeyListener {
    }
}

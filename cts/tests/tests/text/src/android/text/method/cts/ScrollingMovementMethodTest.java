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

import dalvik.annotation.KnownFailure;

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.cts.WidgetTestUtils;

/**
 * Test {@link ScrollingMovementMethod}. The class is an implementation of interface
 * {@link MovementMethod}. The typical usage of {@link MovementMethod} is tested in
 * {@link android.widget.cts.TextViewTest} and this test case is only focused on the
 * implementation of the methods.
 *
 * @see android.widget.cts.TextViewTest
 */
public class ScrollingMovementMethodTest extends ActivityInstrumentationTestCase2<StubActivity> {
    private static final int LITTLE_SPACE = 20;

    private static final String THREE_LINES_TEXT = "first line\nsecond line\nlast line";

    private TextView mTextView;

    private Spannable mSpannable;

    private int mScaledTouchSlop;

    public ScrollingMovementMethodTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTextView = new TextView(getActivity());
        mTextView.setText(THREE_LINES_TEXT, BufferType.EDITABLE);
        mSpannable = (Spannable) mTextView.getText();
        mScaledTouchSlop = ViewConfiguration.get(getActivity()).getScaledTouchSlop();
    }

    public void testConstructor() {
        new ScrollingMovementMethod();
    }

    public void testGetInstance() {
        MovementMethod method0 = ScrollingMovementMethod.getInstance();
        assertTrue(method0 instanceof ScrollingMovementMethod);

        MovementMethod method1 = ScrollingMovementMethod.getInstance();
        assertSame(method0, method1);
    }

    public void testOnTouchEventHorizontalMotion() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        final ScrollingMovementMethod method = new ScrollingMovementMethod();
        runActionOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText("hello world", BufferType.SPANNABLE);
                mTextView.setSingleLine();
                mSpannable = (Spannable) mTextView.getText();
                int width = WidgetTestUtils.convertDipToPixels(getActivity(), LITTLE_SPACE);
                getActivity().setContentView(mTextView,
                        new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        });
        assertNotNull(mTextView.getLayout());

        float rightMost = mTextView.getLayout().getLineRight(0) - mTextView.getWidth()
                + mTextView.getTotalPaddingLeft() + mTextView.getTotalPaddingRight();
        int leftMost = mTextView.getScrollX();

        final long now = SystemClock.uptimeMillis();
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // press
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
            }
        }));

        final int tinyDist = -(mScaledTouchSlop - 1);
        int previousScrollX = mTextView.getScrollX();
        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move for short distance
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, tinyDist, 0, 0));
            }
        }));
        assertEquals(previousScrollX, mTextView.getScrollX());

        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // release
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, tinyDist, 0, 0));
            }
        }));

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // press
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
            }
        }));

        final int distFar = -mScaledTouchSlop;
        previousScrollX = mTextView.getScrollX();
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move for enough distance
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, distFar, 0, 0));
            }
        }));
        assertTrue(mTextView.getScrollX() > previousScrollX);
        assertTrue(mTextView.getScrollX() < rightMost);

        previousScrollX = mTextView.getScrollX();
        final int distTooFar = (int) (-rightMost * 10);
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move for long distance
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, distTooFar, 0, 0));
            }
        }));
        assertTrue(mTextView.getScrollX() > previousScrollX);
        assertEquals(rightMost, mTextView.getScrollX(), 1.0f);

        previousScrollX = mTextView.getScrollX();
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move back
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, 0, 0));
            }
        }));
        assertTrue(mTextView.getScrollX() < previousScrollX);
        assertEquals(leftMost, mTextView.getScrollX());

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // release
                mResult = method.onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 0, 0, 0));
            }
        }));
    }

    public void testOnTouchEventVerticalMotion() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        final ScrollingMovementMethod method = new ScrollingMovementMethod();
        runActionOnUiThread(new Runnable() {
            public void run() {
                mTextView.setLines(1);
                getActivity().setContentView(mTextView,
                        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
        });
        assertNotNull(mTextView.getLayout());

        float bottom = mTextView.getLayout().getHeight() - mTextView.getHeight()
                + mTextView.getTotalPaddingTop() + mTextView.getTotalPaddingBottom();
        int top = mTextView.getScrollY();

        final long now = SystemClock.uptimeMillis();
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // press
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_DOWN, 0, 0, 0));
            }
        }));

        final int tinyDist = -(mScaledTouchSlop - 1);
        int previousScrollY = mTextView.getScrollY();
        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move for short distance
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_MOVE, 0, tinyDist, 0));
            }
        }));
        assertEquals(previousScrollY, mTextView.getScrollY());

        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // release
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_UP, 0, tinyDist, 0));
            }
        }));

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // press
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_DOWN, 0, 0, 0));
            }
        }));

        final int distFar = -mScaledTouchSlop;
        previousScrollY = mTextView.getScrollY();
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move for enough distance
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_MOVE, 0, distFar, 0));
            }
        }));
        assertTrue(mTextView.getScrollY() > previousScrollY);
        assertTrue(mTextView.getScrollX() < bottom);

        previousScrollY = mTextView.getScrollY();
        final int distTooFar = (int) (-bottom * 10);
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move for long distance
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_MOVE, 0, distTooFar, 0));
            }
        }));
        assertTrue(mTextView.getScrollY() > previousScrollY);
        assertEquals(bottom, mTextView.getScrollY(), 0f);

        previousScrollY = mTextView.getScrollY();
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // move back
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_MOVE, 0, 0, 0));
            }
        }));
        assertTrue(mTextView.getScrollY() < previousScrollY);
        assertEquals(top, mTextView.getScrollX());

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                // release
                mResult = method.onTouchEvent(mTextView, mSpannable, MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_UP, 0, 0, 0));
            }
        }));
    }

    public void testOnTouchEventExceptional() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        runActionOnUiThread(new Runnable() {
            public void run() {
                int width = WidgetTestUtils.convertDipToPixels(getActivity(), LITTLE_SPACE);
                getActivity().setContentView(mTextView,
                        new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        });
        assertNotNull(mTextView.getLayout());

        runActionOnUiThread(new Runnable() {
            public void run() {
                try {
                    new ScrollingMovementMethod().onTouchEvent(mTextView, mSpannable, null);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                long now = SystemClock.uptimeMillis();
                try {
                    new ScrollingMovementMethod().onTouchEvent(mTextView, null,
                            MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    new ScrollingMovementMethod().onTouchEvent(null, mSpannable,
                            MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                new ScrollingMovementMethod().onTouchEvent(mTextView, mSpannable,
                        MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                try {
                    new ScrollingMovementMethod().onTouchEvent(null, mSpannable,
                            MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, - 10000, 0, 0));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    new ScrollingMovementMethod().onTouchEvent(mTextView, null,
                            MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, - 10000, 0, 0));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    new ScrollingMovementMethod().onTouchEvent(null, mSpannable,
                            MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, - 10000, 0, 0));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    new ScrollingMovementMethod().onTouchEvent(mTextView, null,
                            MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, - 10000, 0, 0));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }
            }
        });
    }

    public void testCanSelectArbitrarily() {
        assertFalse(new ScrollingMovementMethod().canSelectArbitrarily());
    }

    public void testOnKeyDownVerticalMovement() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        runActionOnUiThread(new Runnable() {
            public void run() {
                getActivity().setContentView(mTextView);
            }
        });
        assertNotNull(mTextView.getLayout());

        assertVisibleLineInTextView(0);
        final MyScrollingMovementMethod method = new MyScrollingMovementMethod();
        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onKeyDown(mTextView, null, KeyEvent.KEYCODE_DPAD_DOWN,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
            }
        });
        assertVisibleLineInTextView(1);

        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onKeyDown(mTextView, null, KeyEvent.KEYCODE_DPAD_UP,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
            }
        });
        assertVisibleLineInTextView(0);
    }

    public void testOnKeyDownHorizontalMovement() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        runActionOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText("short");
                mTextView.setSingleLine();
                int width = WidgetTestUtils.convertDipToPixels(getActivity(), LITTLE_SPACE);
                getActivity().setContentView(mTextView,
                        new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        });
        assertNotNull(mTextView.getLayout());

        final MyScrollingMovementMethod method = new MyScrollingMovementMethod();
        int previousScrollX = mTextView.getScrollX();
        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onKeyDown(mTextView, (Spannable) mTextView.getText(),
                        KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_RIGHT));
            }
        });
        assertTrue(mTextView.getScrollX() > previousScrollX);

        previousScrollX = mTextView.getScrollX();
        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onKeyDown(mTextView, (Spannable) mTextView.getText(),
                        KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT));
            }
        });
        assertTrue(mTextView.getScrollX() < previousScrollX);

        previousScrollX = mTextView.getScrollX();
        assertVisibleLineInTextView(0);
        runActionOnUiThread(new Runnable() {
            public void run() {
                assertFalse(method.onKeyDown(mTextView, mSpannable, 0,
                        new KeyEvent(KeyEvent.ACTION_DOWN, 0)));
            }
        });
        assertEquals(previousScrollX, mTextView.getScrollX());
        assertVisibleLineInTextView(0);
    }

    public void testOnKeyDownExceptions() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        runActionOnUiThread(new Runnable() {
            public void run() {
                getActivity().setContentView(mTextView);
            }
        });
        assertNotNull(mTextView.getLayout());

        final MyScrollingMovementMethod method = new MyScrollingMovementMethod();
        runActionOnUiThread(new Runnable() {
            public void run() {
                try {
                    method.onKeyDown(null, mSpannable, KeyEvent.KEYCODE_DPAD_RIGHT,
                            new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    method.onKeyDown(mTextView, null, KeyEvent.KEYCODE_DPAD_RIGHT,
                            new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    method.onKeyDown(mTextView, mSpannable, KeyEvent.KEYCODE_DPAD_RIGHT, null);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }
            }
        });
    }

    public void testVerticalMovement() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        final MyScrollingMovementMethod method = new MyScrollingMovementMethod();
        runActionOnUiThread(new Runnable() {
            public void run() {
                mTextView.setLines(1);
                getActivity().setContentView(mTextView,
                        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
        });
        assertNotNull(mTextView.getLayout());

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.down(mTextView, mSpannable);
            }
        }));
        assertVisibleLineInTextView(1);

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.down(mTextView, mSpannable);
            }
        }));
        assertVisibleLineInTextView(2);

        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.down(mTextView, mSpannable);
            }
        }));
        assertVisibleLineInTextView(2);

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.up(mTextView, mSpannable);
            }
        }));
        assertVisibleLineInTextView(1);

        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.up(mTextView, mSpannable);
            }
        }));
        assertVisibleLineInTextView(0);

        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.up(mTextView, mSpannable);
            }
        }));
        assertVisibleLineInTextView(0);

        runActionOnUiThread(new Runnable() {
            public void run() {
                try {
                    method.up(null, mSpannable);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    method.up(mTextView, null);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    method.down(null, mSpannable);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    method.down(mTextView, null);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }
            }
        });
    }

    public void testMovementWithNullLayout() {
        assertNull(mTextView.getLayout());
        try {
            new MyScrollingMovementMethod().down(mTextView, mSpannable);
        } catch (NullPointerException e) {
            // NPE is acceptable
        }

        try {
            new MyScrollingMovementMethod().up(mTextView, mSpannable);
        } catch (NullPointerException e) {
            // NPE is acceptable
        }

        try {
            new MyScrollingMovementMethod().left(mTextView, mSpannable);
        } catch (NullPointerException e) {
            // NPE is acceptable
        }

        try {
            new MyScrollingMovementMethod().right(mTextView, mSpannable);
        } catch (NullPointerException e) {
            // NPE is acceptable
        }

        long now = SystemClock.uptimeMillis();
        try {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
            new ScrollingMovementMethod().onKeyDown(mTextView, mSpannable,
                    KeyEvent.KEYCODE_DPAD_RIGHT, event);
        } catch (NullPointerException e) {
            // NPE is acceptable
        }

        new ScrollingMovementMethod().onTouchEvent(mTextView, mSpannable,
                MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
        try {
            new ScrollingMovementMethod().onTouchEvent(mTextView, mSpannable,
                    MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, - 10000, 0, 0));
        } catch (NullPointerException e) {
            // NPE is acceptable
        }
    }

    public void testInitialize() {
        new ScrollingMovementMethod().initialize(null, null);
    }

    public void testOnTrackballEvent() {
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, 0, 2, -2, 0);
        MyScrollingMovementMethod mockMethod = new MyScrollingMovementMethod();

        assertFalse(mockMethod.onTrackballEvent(mTextView, mSpannable, event));
        assertFalse(mockMethod.onTrackballEvent(null, mSpannable, event));
        assertFalse(mockMethod.onTrackballEvent(mTextView, mSpannable, null));
        assertFalse(mockMethod.onTrackballEvent(mTextView, null, event));
    }

    public void testOnKeyUp() {
        ScrollingMovementMethod method = new ScrollingMovementMethod();
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

    public void testOnTakeFocus() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        final ScrollingMovementMethod method = new ScrollingMovementMethod();
        // wait until the text view gets layout
        assertNull(mTextView.getLayout());
        try {
            method.onTakeFocus(mTextView, mSpannable, View.FOCUS_BACKWARD);
        } catch (NullPointerException e) {
            // NPE is acceptable
        }

        runActionOnUiThread(new Runnable() {
            public void run() {
                int height = WidgetTestUtils.convertDipToPixels(getActivity(), LITTLE_SPACE);
                getActivity().setContentView(mTextView,
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                height));
            }
        });
        Layout layout = mTextView.getLayout();
        assertNotNull(layout);

        int previousScrollY = mTextView.getScrollY();
        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onTakeFocus(mTextView, mSpannable, View.FOCUS_BACKWARD);
            }
        });
        assertTrue(mTextView.getScrollY() >= previousScrollY);
        assertVisibleLineInTextView(2);

        previousScrollY = mTextView.getScrollY();
        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onTakeFocus(mTextView, mSpannable, View.FOCUS_FORWARD);
            }
        });
        assertTrue(mTextView.getScrollY() <= previousScrollY);
        assertVisibleLineInTextView(0);

        runActionOnUiThread(new Runnable() {
            public void run() {
                try {
                    method.onTakeFocus(null, mSpannable, View.FOCUS_FORWARD);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }

                try {
                    method.onTakeFocus(mTextView, null, View.FOCUS_FORWARD);
                } catch (NullPointerException e) {
                    // NPE is acceptable
                }
            }
        });
    }

    public void testHorizontalMovement() throws Throwable {
        /*
         * All these assertions depends on whether the TextView has a layout.The text view will not
         * get layout in setContent method but in otherhandler's function. Assertion which is
         * following the setContent will not get the expecting result. It have to wait all the
         * handlers' operations on the UiTread to finish. So all these cases are divided into
         * several steps,setting the content at first, waiting the layout, and checking the
         * assertion at last.
         */
        final MyScrollingMovementMethod method = new MyScrollingMovementMethod();
        runActionOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText("short");
                mTextView.setSingleLine();
                int width = WidgetTestUtils.convertDipToPixels(getActivity(), LITTLE_SPACE);
                getActivity().setContentView(mTextView,
                        new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        });
        assertNotNull(mTextView.getLayout());

        int previousScrollX = mTextView.getScrollX();
        assertTrue(getActionResult(new ActionRunnerWithResult() {

            public void run() {
                mResult = method.right(mTextView, mSpannable);
            }
        }));
        assertTrue(mTextView.getScrollX() > previousScrollX);

        previousScrollX = mTextView.getScrollX();
        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.right(mTextView, mSpannable);
            }
        }));
        assertEquals(previousScrollX, mTextView.getScrollX());

        previousScrollX = mTextView.getScrollX();
        assertTrue(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.left(mTextView, mSpannable);
            }
        }));
        assertTrue(mTextView.getScrollX() < previousScrollX);

        previousScrollX = mTextView.getScrollX();
        assertFalse(getActionResult(new ActionRunnerWithResult() {
            public void run() {
                mResult = method.left(mTextView, mSpannable);
            }
        }));
        assertEquals(previousScrollX, mTextView.getScrollX());
    }

    public void testOnKeyOther() throws Throwable {
        runActionOnUiThread(new Runnable() {
            public void run() {
                getActivity().setContentView(mTextView);
            }
        });
        assertNotNull(mTextView.getLayout());

        assertVisibleLineInTextView(0);
        final MyScrollingMovementMethod method = new MyScrollingMovementMethod();
        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onKeyOther(mTextView, null,
                        new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE,
                                KeyEvent.KEYCODE_DPAD_DOWN, 2));
            }
        });
        assertVisibleLineInTextView(1);

        runActionOnUiThread(new Runnable() {
            public void run() {
                method.onKeyOther(mTextView, null,
                        new KeyEvent(0, 0, KeyEvent.ACTION_MULTIPLE,
                                KeyEvent.KEYCODE_DPAD_UP, 2));
            }
        });
        assertVisibleLineInTextView(0);
    }

    private void assertVisibleLineInTextView(int line) {
        Layout layout = mTextView.getLayout();
        int scrollY = mTextView.getScrollY();
        int padding = mTextView.getTotalPaddingTop() + mTextView.getTotalPaddingBottom();
        assertTrue(layout.getLineForVertical(scrollY) <= line);
        assertTrue(layout.getLineForVertical(scrollY + mTextView.getHeight() - padding) >= line);
    }

    private boolean getActionResult(ActionRunnerWithResult actionRunner) throws Throwable {
        runActionOnUiThread(actionRunner);
        return actionRunner.getResult();
    }

    private void runActionOnUiThread(Runnable actionRunner) throws Throwable {
        runTestOnUiThread(actionRunner);
        getInstrumentation().waitForIdleSync();
    }

    private static abstract class ActionRunnerWithResult implements Runnable {
        protected boolean mResult = false;

        public boolean getResult() {
            return mResult;
        }
    }

    private static class MyScrollingMovementMethod extends ScrollingMovementMethod {
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
}

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

import com.android.cts.stub.R;


import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Test {@link ScrollView}.
 */
public class ScrollViewTest extends ActivityInstrumentationTestCase2<ScrollViewStubActivity> {
    // view dpi constants. Must match those defined in scroll_view layout
    private static final int ITEM_WIDTH_DPI  = 250;
    private static final int ITEM_HEIGHT_DPI = 100;
    private static final int ITEM_COUNT  = 15;
    private static final int PAGE_WIDTH_DPI  = 100;
    private static final int PAGE_HEIGHT_DPI = 100;
    private static final int TOLERANCE = 2;

    private int mItemWidth;
    private int mItemHeight;
    private int mPageWidth;
    private int mPageHeight;
    private int mScrollBottom;
    private int mScrollRight;

    private MyScrollView mScrollView;
    private Activity mActivity;

    public ScrollViewTest() {
        super("com.android.cts.stub", ScrollViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mScrollView = (MyScrollView) mActivity.findViewById(R.id.scroll_view);

        // calculate pixel positions from dpi constants.
        final float density = getActivity().getResources().getDisplayMetrics().density;
        mItemWidth = (int) (ITEM_WIDTH_DPI * density + 0.5f);
        mItemHeight = (int) (ITEM_HEIGHT_DPI * density + 0.5f);
        mPageWidth = (int) (PAGE_WIDTH_DPI * density + 0.5f);
        mPageHeight = (int) (PAGE_HEIGHT_DPI * density + 0.5f);

        mScrollBottom = mItemHeight * ITEM_COUNT - mPageHeight;
        mScrollRight = mItemWidth - mPageWidth;
    }

    public void testConstructor() {
        XmlPullParser parser = mActivity.getResources().getLayout(R.layout.scrollview_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new ScrollView(mActivity);

        new ScrollView(mActivity, attrs);

        new ScrollView(mActivity, attrs, 0);
    }

    public void testGetMaxScrollAmount() {
        // the value is half of total layout height
        ScrollView scrollView = new ScrollView(mActivity);
        scrollView.layout(0, 0, 100, 200);
        assertEquals((200 - 0) / 2, scrollView.getMaxScrollAmount());

        scrollView.layout(0, 0, 150, 100);
        assertEquals((100 - 0) / 2, scrollView.getMaxScrollAmount());
    }

    public void testAddView() {
        ScrollView scrollView = new ScrollView(mActivity);
        TextView child0 = new TextView(mActivity);
        scrollView.addView(child0);
        assertSame(child0, scrollView.getChildAt(0));

        assertEquals(1, scrollView.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            scrollView.addView(child1);
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, scrollView.getChildCount());
    }

    public void testAddViewWithIndex() {
        ScrollView scrollView = new ScrollView(mActivity);
        TextView child0 = new TextView(mActivity);
        scrollView.addView(child0, 0);
        assertSame(child0, scrollView.getChildAt(0));

        assertEquals(1, scrollView.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            scrollView.addView(child1, 1);
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, scrollView.getChildCount());

        scrollView.removeAllViews();
        scrollView = new ScrollView(mActivity);
        scrollView.addView(child0, -1);
        assertSame(child0, scrollView.getChildAt(0));

        assertEquals(1, scrollView.getChildCount());
        child1 = new TextView(mActivity);
        try {
            scrollView.addView(child1, -1);
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, scrollView.getChildCount());

        scrollView.removeAllViews();
        scrollView = new ScrollView(mActivity);
        try {
            scrollView.addView(child0, 1);
            fail("ScrollView can host only one direct child");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testAddViewWithLayoutParams() {
        ScrollView scrollView = new ScrollView(mActivity);
        TextView child0 = new TextView(mActivity);
        scrollView.addView(child0, new ViewGroup.LayoutParams(200, 100));
        assertSame(child0, scrollView.getChildAt(0));
        assertEquals(200, child0.getLayoutParams().width);
        assertEquals(100, child0.getLayoutParams().height);

        assertEquals(1, scrollView.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            scrollView.addView(child1, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, scrollView.getChildCount());

        scrollView.removeAllViews();
        scrollView = new ScrollView(mActivity);
        child0 = new TextView(mActivity);
        try {
            scrollView.addView(child0, null);
            fail("The LayoutParams should not be null!");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testAddViewWithIndexAndLayoutParams() {
        ScrollView scrollView = new ScrollView(mActivity);
        TextView child0 = new TextView(mActivity);
        scrollView.addView(child0, 0, new ViewGroup.LayoutParams(200, 100));
        assertSame(child0, scrollView.getChildAt(0));
        assertEquals(200, child0.getLayoutParams().width);
        assertEquals(100, child0.getLayoutParams().height);

        assertEquals(1, scrollView.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            scrollView.addView(child1, 0, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, scrollView.getChildCount());

        scrollView.removeAllViews();
        scrollView = new ScrollView(mActivity);
        child0 = new TextView(mActivity);
        try {
            scrollView.addView(child0, null);
            fail("The LayoutParams should not be null!");
        } catch (NullPointerException e) {
            // expected
        }

        scrollView.removeAllViews();
        scrollView = new ScrollView(mActivity);
        scrollView.addView(child0, -1, new ViewGroup.LayoutParams(300, 150));
        assertSame(child0, scrollView.getChildAt(0));
        assertEquals(300, child0.getLayoutParams().width);
        assertEquals(150, child0.getLayoutParams().height);

        assertEquals(1, scrollView.getChildCount());
        child1 = new TextView(mActivity);
        try {
            scrollView.addView(child1, -1, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, scrollView.getChildCount());

        scrollView.removeAllViews();
        scrollView = new ScrollView(mActivity);
        try {
            scrollView.addView(child0, 1, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testAccessFillViewport() {
        ScrollView scrollView = new ScrollView(mActivity);
        assertFalse(scrollView.isFillViewport());
        scrollView.layout(0, 0, 100, 100);
        assertFalse(scrollView.isLayoutRequested());

        scrollView.setFillViewport(false);
        assertFalse(scrollView.isFillViewport());
        assertFalse(scrollView.isLayoutRequested());

        scrollView.setFillViewport(true);
        assertTrue(scrollView.isFillViewport());
        assertTrue(scrollView.isLayoutRequested());

        scrollView.layout(0, 0, 100, 100);
        assertFalse(mScrollView.isLayoutRequested());

        scrollView.setFillViewport(false);
        assertFalse(scrollView.isFillViewport());
        assertTrue(scrollView.isLayoutRequested());
    }

    public void testAccessSmoothScrollingEnabled() throws Throwable {
        assertTrue(mScrollView.isSmoothScrollingEnabled());

        // scroll immediately
        mScrollView.setSmoothScrollingEnabled(false);
        assertFalse(mScrollView.isSmoothScrollingEnabled());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });

        assertEquals(mScrollBottom, mScrollView.getScrollY(), TOLERANCE);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.fullScroll(View.FOCUS_UP);
            }
        });
        assertEquals(0, mScrollView.getScrollY());

        // smooth scroll
        mScrollView.setSmoothScrollingEnabled(true);
        assertTrue(mScrollView.isSmoothScrollingEnabled());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        pollingCheckSmoothScrolling(0, 0, 0, mScrollBottom);
        assertEquals(mScrollBottom, mScrollView.getScrollY(), TOLERANCE);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.fullScroll(View.FOCUS_UP);
            }
        });
        pollingCheckSmoothScrolling(0, 0, mScrollBottom, 0);
        assertEquals(0, mScrollView.getScrollY());
    }

    public void testMeasureChild() {
        MyScrollView scrollView = new MyScrollView(mActivity);

        MyView child = new MyView(mActivity);
        child.setBackgroundDrawable(null);
        child.setPadding(0, 0, 0, 0);
        child.setMinimumHeight(30);
        child.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        child.measure(MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));

        assertEquals(100, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());

        scrollView.measureChild(child, MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));

        assertEquals(30, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());
    }

    public void testMeasureChildWithMargins() {
        MyScrollView mockScrollView = new MyScrollView(mActivity);

        MyView child = new MyView(mActivity);
        child.setBackgroundDrawable(null);
        child.setPadding(0, 0, 0, 0);
        child.setMinimumHeight(30);
        child.setLayoutParams(new ViewGroup.MarginLayoutParams(100, 100));
        child.measure(MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));

        assertEquals(100, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());

        mockScrollView.measureChildWithMargins(child,
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY), 5,
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY), 5);

        assertEquals(30, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());
    }

    @UiThreadTest
    public void testPageScroll() {
        mScrollView.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollView.getScrollY());

        assertTrue(mScrollView.pageScroll(View.FOCUS_DOWN));
        assertEquals(mPageHeight, mScrollView.getScrollY(), TOLERANCE);

        assertTrue(mScrollView.pageScroll(View.FOCUS_DOWN));
        assertEquals(mPageHeight * 2, mScrollView.getScrollY(), TOLERANCE);

        mScrollView.scrollTo(mPageWidth, mScrollBottom);
        assertFalse(mScrollView.pageScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollView.getScrollY(), TOLERANCE);

        assertTrue(mScrollView.pageScroll(View.FOCUS_UP));
        assertEquals(mScrollBottom - mPageHeight, mScrollView.getScrollY(), TOLERANCE);

        assertTrue(mScrollView.pageScroll(View.FOCUS_UP));
        assertEquals(mScrollBottom -mPageHeight * 2, mScrollView.getScrollY(), TOLERANCE);

        mScrollView.scrollTo(mPageWidth, 0);
        assertFalse(mScrollView.pageScroll(View.FOCUS_UP));
        assertEquals(0, mScrollView.getScrollY());
    }

    @UiThreadTest
    public void testFullScroll() {
        mScrollView.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollView.getScrollY());

        assertTrue(mScrollView.fullScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollView.getScrollY());

        assertFalse(mScrollView.fullScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollView.getScrollY());

        assertTrue(mScrollView.fullScroll(View.FOCUS_UP));
        assertEquals(0, mScrollView.getScrollY());

        assertFalse(mScrollView.fullScroll(View.FOCUS_UP));
        assertEquals(0, mScrollView.getScrollY());
    }

    @UiThreadTest
    public void testArrowScroll() {
        mScrollView.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollView.getScrollY());

        int y = mScrollView.getScrollY();
        while (mScrollBottom != y) {
            assertTrue(mScrollView.arrowScroll(View.FOCUS_DOWN));
            assertTrue(y <= mScrollView.getScrollY());
            y = mScrollView.getScrollY();
        }

        assertFalse(mScrollView.arrowScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollView.getScrollY());

        y = mScrollView.getScrollY();
        while (0 != y) {
            assertTrue(mScrollView.arrowScroll(View.FOCUS_UP));
            assertTrue(y >= mScrollView.getScrollY());
            y = mScrollView.getScrollY();
        }

        assertFalse(mScrollView.arrowScroll(View.FOCUS_UP));
        assertEquals(0, mScrollView.getScrollY());
    }

    public void testSmoothScrollBy() throws Throwable {
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(0, mScrollView.getScrollY());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.smoothScrollBy(mScrollRight, mScrollBottom);
            }
        });
        // smoothScrollBy doesn't scroll in X
        pollingCheckSmoothScrolling(0, 0, 0, mScrollBottom);
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(mScrollBottom, mScrollView.getScrollY());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.smoothScrollBy(-mScrollRight, -mScrollBottom);
            }
        });
        pollingCheckSmoothScrolling(mScrollRight, 0, mScrollBottom, 0);
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(0, mScrollView.getScrollY());
    }

    public void testSmoothScrollTo() throws Throwable {
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(0, mScrollView.getScrollY());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.smoothScrollTo(mScrollRight, mScrollBottom);
            }
        });
        // smoothScrollTo doesn't scroll in X
        pollingCheckSmoothScrolling(0, 0, 0, mScrollBottom);
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(mScrollBottom, mScrollView.getScrollY());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.smoothScrollTo(mPageWidth, mPageHeight);
            }
        });
        pollingCheckSmoothScrolling(0, 0, mScrollBottom, mPageHeight);
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(mPageHeight, mScrollView.getScrollY());
    }

    public void testComputeScrollDeltaToGetChildRectOnScreen() {
        mScrollView.setSmoothScrollingEnabled(false);
        int edge = mScrollView.getVerticalFadingEdgeLength();

        // Rect's height is smaller than scroll view
        Rect rect = new Rect(0, 0, 0, 0);
        assertEquals(0, mScrollView.computeScrollDeltaToGetChildRectOnScreen(rect));

        rect = new Rect(0, edge, 0, mPageHeight);
        assertEquals(0, mScrollView.computeScrollDeltaToGetChildRectOnScreen(rect));

        mScrollView.scrollTo(0, 0);
        rect = new Rect(0, edge + 1, 0, mPageHeight);
        assertEquals(edge, mScrollView.computeScrollDeltaToGetChildRectOnScreen(rect));
    }

    public void testComputeVerticalScrollRange() {
        assertTrue(mScrollView.getChildCount() > 0);
        assertEquals(mItemHeight * ITEM_COUNT,
                mScrollView.computeVerticalScrollRange(), TOLERANCE);

        MyScrollView myScrollView = new MyScrollView(mActivity);
        assertEquals(0, myScrollView.getChildCount());
        assertEquals(0, myScrollView.computeVerticalScrollRange());
    }

    @UiThreadTest
    public void testRequestChildFocus() {
        mScrollView.setSmoothScrollingEnabled(false);

        View firstChild = mScrollView.findViewById(R.id.first_child);
        View lastChild = mScrollView.findViewById(R.id.last_child);
        firstChild.requestFocus();

        int scrollY = mScrollView.getScrollY();
        mScrollView.requestChildFocus(lastChild, lastChild);
        // check scrolling to the child which wants focus
        assertTrue(mScrollView.getScrollY() > scrollY);

        scrollY = mScrollView.getScrollY();
        mScrollView.requestChildFocus(firstChild, firstChild);
        // check scrolling to the child which wants focus
        assertTrue(mScrollView.getScrollY() < scrollY);
    }

    @UiThreadTest
    public void testRequestChildRectangleOnScreen() {
        mScrollView.setSmoothScrollingEnabled(false);
        mScrollView.setVerticalFadingEdgeEnabled(true);
        int edge = mScrollView.getVerticalFadingEdgeLength();

        View child = mScrollView.findViewById(R.id.first_child);
        int orgRectSize = (int)(10 * getActivity().getResources().getDisplayMetrics().density);
        final Rect originalRect = new Rect(0, 0, orgRectSize, orgRectSize);
        final Rect newRect = new Rect(mItemWidth - orgRectSize, mItemHeight - orgRectSize,
                mItemWidth, mItemHeight);

        assertFalse(mScrollView.requestChildRectangleOnScreen(child, originalRect, true));
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(0, mScrollView.getScrollY());

        assertTrue(mScrollView.requestChildRectangleOnScreen(child, newRect, true));
        assertEquals(0, mScrollView.getScrollX());
        assertEquals(edge, mScrollView.getScrollY());
    }

    @UiThreadTest
    public void testRequestLayout() {
        mScrollView.requestLayout();

        assertTrue(mScrollView.isLayoutRequested());
    }

    public void testFling() throws Throwable {
        mScrollView.setSmoothScrollingEnabled(true);
        assertEquals(0, mScrollView.getScrollY());

        // fling towards bottom
        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.fling(2000);
            }
        });
        pollingCheckFling(0, true);

        final int currentY = mScrollView.getScrollY();
        // fling towards top
        runTestOnUiThread(new Runnable() {
            public void run() {
                mScrollView.fling(-2000);
            }
        });
        pollingCheckFling(currentY, false);
    }

    @UiThreadTest
    public void testScrollTo() {
        mScrollView.setSmoothScrollingEnabled(false);

        mScrollView.scrollTo(10, 10);
        assertEquals(10, mScrollView.getScrollY());
        assertEquals(10, mScrollView.getScrollX());

        mScrollView.scrollTo(mPageWidth, mPageHeight);
        assertEquals(mPageHeight, mScrollView.getScrollY());
        assertEquals(mPageWidth, mScrollView.getScrollX());

        mScrollView.scrollTo(mScrollRight, mScrollBottom);
        assertEquals(mScrollBottom, mScrollView.getScrollY());
        assertEquals(mScrollRight, mScrollView.getScrollX());

        // reach the top and left
        mScrollView.scrollTo(-10, -10);
        assertEquals(0, mScrollView.getScrollY());
        assertEquals(0, mScrollView.getScrollX());
    }

    public void testGetVerticalFadingEdgeStrengths() {
        assertTrue(mScrollView.getChildCount() > 0);
        assertTrue(mScrollView.getTopFadingEdgeStrength() <= 1.0f);
        assertTrue(mScrollView.getTopFadingEdgeStrength() >= 0.0f);
        assertTrue(mScrollView.getBottomFadingEdgeStrength() <= 1.0f);
        assertTrue(mScrollView.getBottomFadingEdgeStrength() >= 0.0f);

        MyScrollView myScrollView = new MyScrollView(mActivity);
        assertEquals(0, myScrollView.getChildCount());
        assertTrue(mScrollView.getTopFadingEdgeStrength() <= 1.0f);
        assertTrue(mScrollView.getTopFadingEdgeStrength() >= 0.0f);
        assertTrue(mScrollView.getBottomFadingEdgeStrength() <= 1.0f);
        assertTrue(mScrollView.getBottomFadingEdgeStrength() >= 0.0f);
    }

    public void testOnLayout() {
        // onLayout() is implementation details, do NOT test
    }

    public void testOnMeasure() {
        // onMeasure() is implementation details, do NOT test
    }

    public void testExecuteKeyEvent() {
        // executeKeyEvent() is implementation details, do NOT test
    }

    public void testOnRequestFocusInDescendants() {
        // onRequestFocusInDescendants() is implementation details, do NOT test
    }

    public void testOnSizeChanged() {
        // onSizeChanged() is implementation details, do NOT test
    }

    public void testDispatchKeyEvent() {
        // dispatchKeyEvent() is implementation details, do NOT test
    }

    public void testOnInterceptTouchEvent() {
        // onInterceptTouchEvent() is implementation details, do NOT test
    }

    public void testOnTouchEvent() {
        // onTouchEvent() is implementation details, do NOT test
    }

    public void testComputeScroll() {
        // computeScroll() is implementation details, do NOT test
    }

    private boolean isInRange(int current, int from, int to) {
        if (from < to) {
            return current >= from && current <= to;
        }
        return current <= from && current >= to;
    }

    private void pollingCheckSmoothScrolling(final int fromX, final int toX,
            final int fromY, final int toY) {

        if (fromX == toX && fromY == toY) {
            return;
        }

        if (fromY != toY) {
            new PollingCheck() {
                @Override
                protected boolean check() {
                    return isInRange(mScrollView.getScrollY(), fromY, toY);
                }
            }.run();
        }

        if (fromX != toX) {
            new PollingCheck() {
                @Override
                protected boolean check() {
                    return isInRange(mScrollView.getScrollX(), fromX, toX);
                }
            }.run();
        }

        new PollingCheck() {
            @Override
            protected boolean check() {
                return toX == mScrollView.getScrollX() && toY == mScrollView.getScrollY();
            }
        }.run();
    }

    private void pollingCheckFling(final int startPosition, final boolean movingDown) {
        new PollingCheck() {
            @Override
            protected boolean check() {
                if (movingDown) {
                    return mScrollView.getScrollY() > startPosition;
                }
                return mScrollView.getScrollY() < startPosition;
            }
        };

        new PollingCheck() {
            private int mPreviousScrollY = mScrollView.getScrollY();

            @Override
            protected boolean check() {
                if (mScrollView.getScrollY() == mPreviousScrollY) {
                    return true;
                } else {
                    mPreviousScrollY = mScrollView.getScrollY();
                    return false;
                }
            }
        }.run();
    }

    private static class MyView extends View {
        public MyView(Context context) {
            super(context);
        }
    }
}

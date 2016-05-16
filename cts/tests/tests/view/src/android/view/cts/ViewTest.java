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

import com.android.cts.stub.R;
import com.android.internal.view.menu.ContextMenuBuilder;
import com.google.android.collect.Lists;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.Vibrator;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.UiThreadTest;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.TouchDelegate;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.cts.StubActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test {@link View}.
 */
public class ViewTest extends ActivityInstrumentationTestCase2<ViewTestStubActivity> {
    public ViewTest() {
        super(ViewTestStubActivity.class);
    }

    private Resources mResources;
    private MockViewParent mMockParent;
    private ViewTestStubActivity mActivity;

    /** timeout delta when wait in case the system is sluggish */
    private static final long TIMEOUT_DELTA = 10000;

    private static final String LOG_TAG = "ViewTest";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        new PollingCheck() {
            @Override
                protected boolean check() {
                return mActivity.hasWindowFocus();
            }
        }.run();
        mResources = mActivity.getResources();
        mMockParent = new MockViewParent(mActivity);
        assertTrue(mActivity.waitForWindowFocus(5 * DateUtils.SECOND_IN_MILLIS));
    }

    public void testConstructor() {
        new View(mActivity);

        final XmlResourceParser parser = mResources.getLayout(R.layout.view_layout);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        new View(mActivity, attrs);

        new View(mActivity, null);

        try {
            new View(null, attrs);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        new View(mActivity, attrs, 0);

        new View(mActivity, null, 1);

        try {
            new View(null, null, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGetContext() {
        View view = new View(mActivity);
        assertSame(mActivity, view.getContext());
    }

    public void testGetResources() {
        View view = new View(mActivity);
        assertSame(mResources, view.getResources());
    }

    public void testGetAnimation() {
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        View view = new View(mActivity);
        assertNull(view.getAnimation());

        view.setAnimation(animation);
        assertSame(animation, view.getAnimation());

        view.clearAnimation();
        assertNull(view.getAnimation());
    }

    public void testSetAnimation() {
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        View view = new View(mActivity);
        assertNull(view.getAnimation());

        animation.initialize(100, 100, 100, 100);
        assertTrue(animation.isInitialized());
        view.setAnimation(animation);
        assertSame(animation, view.getAnimation());
        assertFalse(animation.isInitialized());

        view.setAnimation(null);
        assertNull(view.getAnimation());
    }

    public void testClearAnimation() {
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        View view = new View(mActivity);

        assertNull(view.getAnimation());
        view.clearAnimation();
        assertNull(view.getAnimation());

        view.setAnimation(animation);
        assertNotNull(view.getAnimation());
        view.clearAnimation();
        assertNull(view.getAnimation());
    }

    public void testStartAnimation() {
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        View view = new View(mActivity);

        try {
            view.startAnimation(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        animation.setStartTime(1L);
        assertEquals(1L, animation.getStartTime());
        view.startAnimation(animation);
        assertEquals(Animation.START_ON_FIRST_FRAME, animation.getStartTime());
    }

    public void testOnAnimation() throws Throwable {
        final Animation animation = new AlphaAnimation(0.0f, 1.0f);
        long duration = 2000L;
        animation.setDuration(duration);
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        // check whether it has started
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.startAnimation(animation);
            }
        });
        getInstrumentation().waitForIdleSync();

        new PollingCheck() {
            @Override
            protected boolean check() {
                return view.hasCalledOnAnimationStart();
            }
        }.run();

        // check whether it has ended after duration, and alpha changed during this time.
        new PollingCheck(duration + TIMEOUT_DELTA) {
            @Override
            protected boolean check() {
                return view.hasCalledOnSetAlpha() && view.hasCalledOnAnimationEnd();
            }
        }.run();
    }

    public void testGetParent() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        ViewGroup parent = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        assertSame(parent, view.getParent());
    }

    public void testFindViewById() {
        View parent = mActivity.findViewById(R.id.viewlayout_root);
        assertSame(parent, parent.findViewById(R.id.viewlayout_root));

        View view = parent.findViewById(R.id.mock_view);
        assertTrue(view instanceof MockView);
    }

    public void testAccessTouchDelegate() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        Rect rect = new Rect();
        final Button button = new Button(mActivity);
        final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
        final int btnHeight = view.getHeight()/3;
        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.addContentView(button,
                        new LinearLayout.LayoutParams(WRAP_CONTENT, btnHeight));
            }
        });
        getInstrumentation().waitForIdleSync();
        button.getHitRect(rect);
        MockTouchDelegate delegate = new MockTouchDelegate(rect, button);

        assertNull(view.getTouchDelegate());

        view.setTouchDelegate(delegate);
        assertSame(delegate, view.getTouchDelegate());
        assertFalse(delegate.hasCalledOnTouchEvent());
        TouchUtils.clickView(this, view);
        assertTrue(view.hasCalledOnTouchEvent());
        assertTrue(delegate.hasCalledOnTouchEvent());

        view.setTouchDelegate(null);
        assertNull(view.getTouchDelegate());
    }

    @UiThreadTest
    public void testAccessTag() {
        ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        MockView mockView = (MockView) mActivity.findViewById(R.id.mock_view);
        MockView scrollView = (MockView) mActivity.findViewById(R.id.scroll_view);

        ViewData viewData = new ViewData();
        viewData.childCount = 3;
        viewData.tag = "linearLayout";
        viewData.firstChild = mockView;
        viewGroup.setTag(viewData);
        viewGroup.setFocusable(true);
        assertSame(viewData, viewGroup.getTag());

        final String tag = "mock";
        assertNull(mockView.getTag());
        mockView.setTag(tag);
        assertEquals(tag, mockView.getTag());

        scrollView.setTag(viewGroup);
        assertSame(viewGroup, scrollView.getTag());

        assertSame(viewGroup, viewGroup.findViewWithTag(viewData));
        assertSame(mockView, viewGroup.findViewWithTag(tag));
        assertSame(scrollView, viewGroup.findViewWithTag(viewGroup));

        mockView.setTag(null);
        assertNull(mockView.getTag());
    }

    public void testOnSizeChanged() throws Throwable {
        final ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        final MockView mockView = new MockView(mActivity);
        assertEquals(-1, mockView.getOldWOnSizeChanged());
        assertEquals(-1, mockView.getOldHOnSizeChanged());
        runTestOnUiThread(new Runnable() {
            public void run() {
                viewGroup.addView(mockView);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mockView.hasCalledOnSizeChanged());
        assertEquals(0, mockView.getOldWOnSizeChanged());
        assertEquals(0, mockView.getOldHOnSizeChanged());

        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        assertTrue(view.hasCalledOnSizeChanged());
        view.reset();
        assertEquals(-1, view.getOldWOnSizeChanged());
        assertEquals(-1, view.getOldHOnSizeChanged());
        int oldw = view.getWidth();
        int oldh = view.getHeight();
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(200, 100);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.hasCalledOnSizeChanged());
        assertEquals(oldw, view.getOldWOnSizeChanged());
        assertEquals(oldh, view.getOldHOnSizeChanged());
    }

    public void testGetHitRect() {
        MockView view = new MockView(mActivity);
        Rect outRect = new Rect();

        try {
            view.getHitRect(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        View mockView = mActivity.findViewById(R.id.mock_view);
        mockView.getHitRect(outRect);
        assertEquals(0, outRect.left);
        assertEquals(0, outRect.top);
        assertEquals(mockView.getWidth(), outRect.right);
        assertEquals(mockView.getHeight(), outRect.bottom);
    }

    public void testForceLayout() {
        View view = new View(mActivity);

        assertFalse(view.isLayoutRequested());
        view.forceLayout();
        assertTrue(view.isLayoutRequested());

        view.forceLayout();
        assertTrue(view.isLayoutRequested());
    }

    public void testIsLayoutRequested() {
        View view = new View(mActivity);

        assertFalse(view.isLayoutRequested());
        view.forceLayout();
        assertTrue(view.isLayoutRequested());

        view.layout(0, 0, 0, 0);
        assertFalse(view.isLayoutRequested());
    }

    public void testRequestLayout() {
        MockView view = new MockView(mActivity);
        assertFalse(view.isLayoutRequested());
        assertNull(view.getParent());

        view.requestLayout();
        assertTrue(view.isLayoutRequested());

        view.setParent(mMockParent);
        assertFalse(mMockParent.hasRequestLayout());
        view.requestLayout();
        assertTrue(view.isLayoutRequested());
        assertTrue(mMockParent.hasRequestLayout());
    }

    public void testLayout() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        assertTrue(view.hasCalledOnLayout());

        view.reset();
        assertFalse(view.hasCalledOnLayout());
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.requestLayout();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.hasCalledOnLayout());
    }

    public void testGetBaseline() {
        View view = new View(mActivity);

        assertEquals(-1, view.getBaseline());
    }

    public void testAccessBackground() {
        View view = new View(mActivity);
        Drawable d1 = mResources.getDrawable(R.drawable.scenery);
        Drawable d2 = mResources.getDrawable(R.drawable.pass);

        assertNull(view.getBackground());

        view.setBackgroundDrawable(d1);
        assertEquals(d1, view.getBackground());

        view.setBackgroundDrawable(d2);
        assertEquals(d2, view.getBackground());

        view.setBackgroundDrawable(null);
        assertNull(view.getBackground());
    }

    public void testSetBackgroundResource() {
        View view = new View(mActivity);

        assertNull(view.getBackground());

        view.setBackgroundResource(R.drawable.pass);
        assertNotNull(view.getBackground());

        view.setBackgroundResource(0);
        assertNull(view.getBackground());
    }

    public void testAccessDrawingCacheBackgroundColor() {
        View view = new View(mActivity);

        assertEquals(0, view.getDrawingCacheBackgroundColor());

        view.setDrawingCacheBackgroundColor(0xFF00FF00);
        assertEquals(0xFF00FF00, view.getDrawingCacheBackgroundColor());

        view.setDrawingCacheBackgroundColor(-1);
        assertEquals(-1, view.getDrawingCacheBackgroundColor());
    }

    public void testSetBackgroundColor() {
        View view = new View(mActivity);
        ColorDrawable colorDrawable;
        assertNull(view.getBackground());

        view.setBackgroundColor(0xFFFF0000);
        colorDrawable = (ColorDrawable) view.getBackground();
        assertNotNull(colorDrawable);
        assertEquals(0xFF, colorDrawable.getAlpha());

        view.setBackgroundColor(0);
        colorDrawable = (ColorDrawable) view.getBackground();
        assertNotNull(colorDrawable);
        assertEquals(0, colorDrawable.getAlpha());
    }

    public void testVerifyDrawable() {
        MockView view = new MockView(mActivity);
        Drawable d1 = mResources.getDrawable(R.drawable.scenery);
        Drawable d2 = mResources.getDrawable(R.drawable.pass);

        assertNull(view.getBackground());
        assertTrue(view.verifyDrawable(null));
        assertFalse(view.verifyDrawable(d1));

        view.setBackgroundDrawable(d1);
        assertTrue(view.verifyDrawable(d1));
        assertFalse(view.verifyDrawable(d2));
    }

    public void testGetDrawingRect() {
        MockView view = new MockView(mActivity);
        Rect outRect = new Rect();

        view.getDrawingRect(outRect);
        assertEquals(0, outRect.left);
        assertEquals(0, outRect.top);
        assertEquals(0, outRect.right);
        assertEquals(0, outRect.bottom);

        view.scrollTo(10, 100);
        view.getDrawingRect(outRect);
        assertEquals(10, outRect.left);
        assertEquals(100, outRect.top);
        assertEquals(10, outRect.right);
        assertEquals(100, outRect.bottom);

        View mockView = mActivity.findViewById(R.id.mock_view);
        mockView.getDrawingRect(outRect);
        assertEquals(0, outRect.left);
        assertEquals(0, outRect.top);
        assertEquals(mockView.getWidth(), outRect.right);
        assertEquals(mockView.getHeight(), outRect.bottom);
    }

    public void testGetFocusedRect() {
        MockView view = new MockView(mActivity);
        Rect outRect = new Rect();

        view.getFocusedRect(outRect);
        assertEquals(0, outRect.left);
        assertEquals(0, outRect.top);
        assertEquals(0, outRect.right);
        assertEquals(0, outRect.bottom);

        view.scrollTo(10, 100);
        view.getFocusedRect(outRect);
        assertEquals(10, outRect.left);
        assertEquals(100, outRect.top);
        assertEquals(10, outRect.right);
        assertEquals(100, outRect.bottom);
    }

    public void testGetGlobalVisibleRectPoint() throws Throwable {
        final View view = mActivity.findViewById(R.id.mock_view);
        final ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        Rect rect = new Rect();
        Point point = new Point();

        assertTrue(view.getGlobalVisibleRect(rect, point));
        Rect rcParent = new Rect();
        Point ptParent = new Point();
        viewGroup.getGlobalVisibleRect(rcParent, ptParent);
        assertEquals(rcParent.left, rect.left);
        assertEquals(rcParent.top, rect.top);
        assertEquals(rect.left + view.getWidth(), rect.right);
        assertEquals(rect.top + view.getHeight(), rect.bottom);
        assertEquals(ptParent.x, point.x);
        assertEquals(ptParent.y, point.y);

        // width is 0
        final LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(0, 300);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams1);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.getGlobalVisibleRect(rect, point));

        // height is -10
        final LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(200, -10);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams2);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.getGlobalVisibleRect(rect, point));

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int halfWidth = display.getWidth() / 2;
        int halfHeight = display.getHeight() /2;

        final LinearLayout.LayoutParams layoutParams3 =
                new LinearLayout.LayoutParams(halfWidth, halfHeight);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams3);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.getGlobalVisibleRect(rect, point));
        assertEquals(rcParent.left, rect.left);
        assertEquals(rcParent.top, rect.top);
        assertEquals(rect.left + halfWidth, rect.right);
        assertEquals(rect.top + halfHeight, rect.bottom);
        assertEquals(ptParent.x, point.x);
        assertEquals(ptParent.y, point.y);
    }

    public void testGetGlobalVisibleRect() throws Throwable {
        final View view = mActivity.findViewById(R.id.mock_view);
        final ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        Rect rect = new Rect();

        assertTrue(view.getGlobalVisibleRect(rect));
        Rect rcParent = new Rect();
        viewGroup.getGlobalVisibleRect(rcParent);
        assertEquals(rcParent.left, rect.left);
        assertEquals(rcParent.top, rect.top);
        assertEquals(rect.left + view.getWidth(), rect.right);
        assertEquals(rect.top + view.getHeight(), rect.bottom);

        // width is 0
        final LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(0, 300);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams1);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.getGlobalVisibleRect(rect));

        // height is -10
        final LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(200, -10);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams2);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.getGlobalVisibleRect(rect));

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int halfWidth = display.getWidth() / 2;
        int halfHeight = display.getHeight() /2;

        final LinearLayout.LayoutParams layoutParams3 =
                new LinearLayout.LayoutParams(halfWidth, halfHeight);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams3);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.getGlobalVisibleRect(rect));
        assertEquals(rcParent.left, rect.left);
        assertEquals(rcParent.top, rect.top);
        assertEquals(rect.left + halfWidth, rect.right);
        assertEquals(rect.top + halfHeight, rect.bottom);
    }

    public void testComputeHorizontalScroll() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        assertEquals(0, view.computeHorizontalScrollOffset());
        assertEquals(view.getWidth(), view.computeHorizontalScrollRange());
        assertEquals(view.getWidth(), view.computeHorizontalScrollExtent());

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.scrollTo(12, 0);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(12, view.computeHorizontalScrollOffset());
        assertEquals(view.getWidth(), view.computeHorizontalScrollRange());
        assertEquals(view.getWidth(), view.computeHorizontalScrollExtent());

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.scrollBy(12, 0);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(24, view.computeHorizontalScrollOffset());
        assertEquals(view.getWidth(), view.computeHorizontalScrollRange());
        assertEquals(view.getWidth(), view.computeHorizontalScrollExtent());

        int newWidth = 200;
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(newWidth, 100);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(24, view.computeHorizontalScrollOffset());
        assertEquals(newWidth, view.getWidth());
        assertEquals(view.getWidth(), view.computeHorizontalScrollRange());
        assertEquals(view.getWidth(), view.computeHorizontalScrollExtent());
    }

    public void testComputeVerticalScroll() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        assertEquals(0, view.computeVerticalScrollOffset());
        assertEquals(view.getHeight(), view.computeVerticalScrollRange());
        assertEquals(view.getHeight(), view.computeVerticalScrollExtent());

        final int scrollToY = 34;
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.scrollTo(0, scrollToY);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(scrollToY, view.computeVerticalScrollOffset());
        assertEquals(view.getHeight(), view.computeVerticalScrollRange());
        assertEquals(view.getHeight(), view.computeVerticalScrollExtent());

        final int scrollByY = 200;
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.scrollBy(0, scrollByY);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(scrollToY + scrollByY, view.computeVerticalScrollOffset());
        assertEquals(view.getHeight(), view.computeVerticalScrollRange());
        assertEquals(view.getHeight(), view.computeVerticalScrollExtent());

        int newHeight = 333;
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(200, newHeight);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(scrollToY + scrollByY, view.computeVerticalScrollOffset());
        assertEquals(newHeight, view.getHeight());
        assertEquals(view.getHeight(), view.computeVerticalScrollRange());
        assertEquals(view.getHeight(), view.computeVerticalScrollExtent());
    }

    public void testGetFadingEdgeStrength() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        assertEquals(0f, view.getLeftFadingEdgeStrength());
        assertEquals(0f, view.getRightFadingEdgeStrength());
        assertEquals(0f, view.getTopFadingEdgeStrength());
        assertEquals(0f, view.getBottomFadingEdgeStrength());

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.scrollTo(10, 10);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(1f, view.getLeftFadingEdgeStrength());
        assertEquals(0f, view.getRightFadingEdgeStrength());
        assertEquals(1f, view.getTopFadingEdgeStrength());
        assertEquals(0f, view.getBottomFadingEdgeStrength());

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.scrollTo(-10, -10);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(0f, view.getLeftFadingEdgeStrength());
        assertEquals(1f, view.getRightFadingEdgeStrength());
        assertEquals(0f, view.getTopFadingEdgeStrength());
        assertEquals(1f, view.getBottomFadingEdgeStrength());
    }

    public void testGetLeftFadingEdgeStrength() {
        MockView view = new MockView(mActivity);

        assertEquals(0.0f, view.getLeftFadingEdgeStrength());

        view.scrollTo(1, 0);
        assertEquals(1.0f, view.getLeftFadingEdgeStrength());
    }

    public void testGetRightFadingEdgeStrength() {
        MockView view = new MockView(mActivity);

        assertEquals(0.0f, view.getRightFadingEdgeStrength());

        view.scrollTo(-1, 0);
        assertEquals(1.0f, view.getRightFadingEdgeStrength());
    }

    public void testGetBottomFadingEdgeStrength() {
        MockView view = new MockView(mActivity);

        assertEquals(0.0f, view.getBottomFadingEdgeStrength());

        view.scrollTo(0, -2);
        assertEquals(1.0f, view.getBottomFadingEdgeStrength());
    }

    public void testGetTopFadingEdgeStrength() {
        MockView view = new MockView(mActivity);

        assertEquals(0.0f, view.getTopFadingEdgeStrength());

        view.scrollTo(0, 2);
        assertEquals(1.0f, view.getTopFadingEdgeStrength());
    }

    public void testResolveSize() {
        assertEquals(50, View.resolveSize(50, View.MeasureSpec.UNSPECIFIED));

        assertEquals(40, View.resolveSize(50, 40 | View.MeasureSpec.EXACTLY));

        assertEquals(30, View.resolveSize(50, 30 | View.MeasureSpec.AT_MOST));

        assertEquals(20, View.resolveSize(20, 30 | View.MeasureSpec.AT_MOST));
    }

    public void testGetDefaultSize() {
        assertEquals(50, View.getDefaultSize(50, View.MeasureSpec.UNSPECIFIED));

        assertEquals(40, View.getDefaultSize(50, 40 | View.MeasureSpec.EXACTLY));

        assertEquals(30, View.getDefaultSize(50, 30 | View.MeasureSpec.AT_MOST));

        assertEquals(30, View.getDefaultSize(20, 30 | View.MeasureSpec.AT_MOST));
    }

    public void testAccessId() {
        View view = new View(mActivity);

        assertEquals(View.NO_ID, view.getId());

        view.setId(10);
        assertEquals(10, view.getId());

        view.setId(0xFFFFFFFF);
        assertEquals(0xFFFFFFFF, view.getId());
    }

    public void testAccessLongClickable() {
        View view = new View(mActivity);

        assertFalse(view.isLongClickable());

        view.setLongClickable(true);
        assertTrue(view.isLongClickable());

        view.setLongClickable(false);
        assertFalse(view.isLongClickable());
    }

    public void testAccessClickable() {
        View view = new View(mActivity);

        assertFalse(view.isClickable());

        view.setClickable(true);
        assertTrue(view.isClickable());

        view.setClickable(false);
        assertFalse(view.isClickable());
    }

    public void testGetContextMenuInfo() {
        MockView view = new MockView(mActivity);

        assertNull(view.getContextMenuInfo());
    }

    public void testSetOnCreateContextMenuListener() {
        View view = new View(mActivity);
        assertFalse(view.isLongClickable());

        view.setOnCreateContextMenuListener(null);
        assertTrue(view.isLongClickable());

        view.setOnCreateContextMenuListener(new OnCreateContextMenuListenerImpl());
        assertTrue(view.isLongClickable());
    }

    public void testCreateContextMenu() {
        OnCreateContextMenuListenerImpl listener = new OnCreateContextMenuListenerImpl();
        MockView view = new MockView(mActivity);
        ContextMenu contextMenu = new ContextMenuBuilder(mActivity);
        view.setParent(mMockParent);
        view.setOnCreateContextMenuListener(listener);
        assertFalse(view.hasCalledOnCreateContextMenu());
        assertFalse(mMockParent.hasCreateContextMenu());
        assertFalse(listener.hasOnCreateContextMenu());

        view.createContextMenu(contextMenu);
        assertTrue(view.hasCalledOnCreateContextMenu());
        assertTrue(mMockParent.hasCreateContextMenu());
        assertTrue(listener.hasOnCreateContextMenu());

        try {
            view.createContextMenu(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testAddFocusables() {
        View view = new View(mActivity);
        ArrayList<View> viewList = new ArrayList<View>();

        // view is not focusable
        assertFalse(view.isFocusable());
        assertEquals(0, viewList.size());
        view.addFocusables(viewList, 0);
        assertEquals(0, viewList.size());

        // view is focusable
        view.setFocusable(true);
        view.addFocusables(viewList, 0);
        assertEquals(1, viewList.size());
        assertEquals(view, viewList.get(0));

        // null array should be ignored
        view.addFocusables(null, 0);
    }

    public void testGetFocusables() {
        View view = new View(mActivity);
        ArrayList<View> viewList;

        // view is not focusable
        assertFalse(view.isFocusable());
        viewList = view.getFocusables(0);
        assertEquals(0, viewList.size());

        // view is focusable
        view.setFocusable(true);
        viewList = view.getFocusables(0);
        assertEquals(1, viewList.size());
        assertEquals(view, viewList.get(0));

        viewList = view.getFocusables(-1);
        assertEquals(1, viewList.size());
        assertEquals(view, viewList.get(0));
    }

    public void testGetRootView() {
        MockView view = new MockView(mActivity);

        assertNull(view.getParent());
        assertEquals(view, view.getRootView());

        view.setParent(mMockParent);
        assertEquals(mMockParent, view.getRootView());
    }

    public void testGetSolidColor() {
        View view = new View(mActivity);

        assertEquals(0, view.getSolidColor());
    }

    public void testSetMinimumWidth() {
        MockView view = new MockView(mActivity);
        assertEquals(0, view.getSuggestedMinimumWidth());

        view.setMinimumWidth(100);
        assertEquals(100, view.getSuggestedMinimumWidth());

        view.setMinimumWidth(-100);
        assertEquals(-100, view.getSuggestedMinimumWidth());
    }

    public void testGetSuggestedMinimumWidth() {
        MockView view = new MockView(mActivity);
        Drawable d = mResources.getDrawable(R.drawable.scenery);
        int drawableMinimumWidth = d.getMinimumWidth();

        // drawable is null
        view.setMinimumWidth(100);
        assertNull(view.getBackground());
        assertEquals(100, view.getSuggestedMinimumWidth());

        // drawable minimum width is larger than mMinWidth
        view.setBackgroundDrawable(d);
        view.setMinimumWidth(drawableMinimumWidth - 10);
        assertEquals(drawableMinimumWidth, view.getSuggestedMinimumWidth());

        // drawable minimum width is smaller than mMinWidth
        view.setMinimumWidth(drawableMinimumWidth + 10);
        assertEquals(drawableMinimumWidth + 10, view.getSuggestedMinimumWidth());
    }

    public void testSetMinimumHeight() {
        MockView view = new MockView(mActivity);
        assertEquals(0, view.getSuggestedMinimumHeight());

        view.setMinimumHeight(100);
        assertEquals(100, view.getSuggestedMinimumHeight());

        view.setMinimumHeight(-100);
        assertEquals(-100, view.getSuggestedMinimumHeight());
    }

    public void testGetSuggestedMinimumHeight() {
        MockView view = new MockView(mActivity);
        Drawable d = mResources.getDrawable(R.drawable.scenery);
        int drawableMinimumHeight = d.getMinimumHeight();

        // drawable is null
        view.setMinimumHeight(100);
        assertNull(view.getBackground());
        assertEquals(100, view.getSuggestedMinimumHeight());

        // drawable minimum height is larger than mMinHeight
        view.setBackgroundDrawable(d);
        view.setMinimumHeight(drawableMinimumHeight - 10);
        assertEquals(drawableMinimumHeight, view.getSuggestedMinimumHeight());

        // drawable minimum height is smaller than mMinHeight
        view.setMinimumHeight(drawableMinimumHeight + 10);
        assertEquals(drawableMinimumHeight + 10, view.getSuggestedMinimumHeight());
    }

    public void testAccessWillNotCacheDrawing() {
        View view = new View(mActivity);

        assertFalse(view.willNotCacheDrawing());

        view.setWillNotCacheDrawing(true);
        assertTrue(view.willNotCacheDrawing());
    }

    public void testAccessDrawingCacheEnabled() {
        View view = new View(mActivity);

        assertFalse(view.isDrawingCacheEnabled());

        view.setDrawingCacheEnabled(true);
        assertTrue(view.isDrawingCacheEnabled());
    }

    public void testGetDrawingCache() {
        MockView view = new MockView(mActivity);

        // should not call buildDrawingCache when getDrawingCache
        assertNull(view.getDrawingCache());

        // should call buildDrawingCache when getDrawingCache
        view = (MockView) mActivity.findViewById(R.id.mock_view);
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap1 = view.getDrawingCache();
        assertNotNull(bitmap1);
        assertEquals(view.getWidth(), bitmap1.getWidth());
        assertEquals(view.getHeight(), bitmap1.getHeight());

        view.setWillNotCacheDrawing(true);
        assertNull(view.getDrawingCache());

        view.setWillNotCacheDrawing(false);
        // build a new drawingcache
        Bitmap bitmap2 = view.getDrawingCache();
        assertNotSame(bitmap1, bitmap2);
    }

    public void testBuildAndDestroyDrawingCache() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        assertNull(view.getDrawingCache());

        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        assertNotNull(bitmap);
        assertEquals(view.getWidth(), bitmap.getWidth());
        assertEquals(view.getHeight(), bitmap.getHeight());

        view.destroyDrawingCache();
        assertNull(view.getDrawingCache());
    }

    public void testAccessWillNotDraw() {
        View view = new View(mActivity);

        assertFalse(view.willNotDraw());

        view.setWillNotDraw(true);
        assertTrue(view.willNotDraw());
    }

    public void testAccessDrawingCacheQuality() {
        View view = new View(mActivity);

        assertEquals(0, view.getDrawingCacheQuality());

        view.setDrawingCacheQuality(1);
        assertEquals(0, view.getDrawingCacheQuality());

        view.setDrawingCacheQuality(0x00100000);
        assertEquals(0x00100000, view.getDrawingCacheQuality());

        view.setDrawingCacheQuality(0x00080000);
        assertEquals(0x00080000, view.getDrawingCacheQuality());

        view.setDrawingCacheQuality(0xffffffff);
        // 0x00180000 is View.DRAWING_CACHE_QUALITY_MASK
        assertEquals(0x00180000, view.getDrawingCacheQuality());
    }

    public void testDispatchSetSelected() {
        MockView mockView1 = new MockView(mActivity);
        MockView mockView2 = new MockView(mActivity);
        mockView1.setParent(mMockParent);
        mockView2.setParent(mMockParent);

        mMockParent.dispatchSetSelected(true);
        assertFalse(mockView1.isSelected());
        assertFalse(mockView2.isSelected());

        mMockParent.dispatchSetSelected(false);
        assertFalse(mockView1.isSelected());
        assertFalse(mockView2.isSelected());
    }

    public void testAccessSelected() {
        View view = new View(mActivity);

        assertFalse(view.isSelected());

        view.setSelected(true);
        assertTrue(view.isSelected());
    }

    public void testDispatchSetPressed() {
        MockView mockView1 = new MockView(mActivity);
        MockView mockView2 = new MockView(mActivity);
        mockView1.setParent(mMockParent);
        mockView2.setParent(mMockParent);

        mMockParent.dispatchSetPressed(true);
        assertFalse(mockView1.isPressed());
        assertFalse(mockView2.isPressed());

        mMockParent.dispatchSetPressed(false);
        assertFalse(mockView1.isPressed());
        assertFalse(mockView2.isPressed());
    }

    public void testAccessPressed() {
        View view = new View(mActivity);

        assertFalse(view.isPressed());

        view.setPressed(true);
        assertTrue(view.isPressed());
    }

    public void testAccessSoundEffectsEnabled() {
        View view = new View(mActivity);

        assertTrue(view.isSoundEffectsEnabled());

        view.setSoundEffectsEnabled(false);
        assertFalse(view.isSoundEffectsEnabled());
    }

    public void testAccessKeepScreenOn() {
        View view = new View(mActivity);

        assertFalse(view.getKeepScreenOn());

        view.setKeepScreenOn(true);
        assertTrue(view.getKeepScreenOn());
    }

    public void testAccessDuplicateParentStateEnabled() {
        View view = new View(mActivity);

        assertFalse(view.isDuplicateParentStateEnabled());

        view.setDuplicateParentStateEnabled(true);
        assertTrue(view.isDuplicateParentStateEnabled());
    }

    public void testAccessEnabled() {
        View view = new View(mActivity);

        assertTrue(view.isEnabled());

        view.setEnabled(false);
        assertFalse(view.isEnabled());
    }

    public void testAccessSaveEnabled() {
        View view = new View(mActivity);

        assertTrue(view.isSaveEnabled());

        view.setSaveEnabled(false);
        assertFalse(view.isSaveEnabled());
    }

    public void testShowContextMenu() {
        MockView view = new MockView(mActivity);

        assertNull(view.getParent());
        try {
            view.showContextMenu();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        view.setParent(mMockParent);
        assertFalse(mMockParent.hasShowContextMenuForChild());

        assertFalse(view.showContextMenu());
        assertTrue(mMockParent.hasShowContextMenuForChild());
    }

    public void testFitSystemWindows() {
        final XmlResourceParser parser = mResources.getLayout(R.layout.view_layout);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        Rect insets = new Rect(10, 20, 30, 50);

        MockView view = new MockView(mActivity);
        assertFalse(view.fitSystemWindows(insets));
        assertFalse(view.fitSystemWindows(null));

        view = new MockView(mActivity, attrs, com.android.internal.R.attr.fitsSystemWindows);
        assertFalse(view.fitSystemWindows(insets));
        assertFalse(view.fitSystemWindows(null));
    }

    public void testPerformClick() {
        View view = new View(mActivity);
        OnClickListenerImpl listener = new OnClickListenerImpl();

        assertFalse(view.performClick());

        assertFalse(listener.hasOnClick());
        view.setOnClickListener(listener);

        assertTrue(view.performClick());
        assertTrue(listener.hasOnClick());

        view.setOnClickListener(null);
        assertFalse(view.performClick());
    }

    public void testSetOnClickListener() {
        View view = new View(mActivity);
        assertFalse(view.performClick());
        assertFalse(view.isClickable());

        view.setOnClickListener(null);
        assertFalse(view.performClick());
        assertTrue(view.isClickable());

        view.setOnClickListener(new OnClickListenerImpl());
        assertTrue(view.performClick());
        assertTrue(view.isClickable());
    }

    public void testPerformLongClick() {
        MockView view = new MockView(mActivity);
        OnLongClickListenerImpl listener = new OnLongClickListenerImpl();

        try {
            view.performLongClick();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        view.setParent(mMockParent);
        assertFalse(mMockParent.hasShowContextMenuForChild());
        assertFalse(view.performLongClick());
        assertTrue(mMockParent.hasShowContextMenuForChild());

        view.setOnLongClickListener(listener);
        mMockParent.reset();
        assertFalse(mMockParent.hasShowContextMenuForChild());
        assertFalse(listener.hasOnLongClick());
        assertTrue(view.performLongClick());
        assertFalse(mMockParent.hasShowContextMenuForChild());
        assertTrue(listener.hasOnLongClick());
    }

    public void testSetOnLongClickListener() {
        MockView view = new MockView(mActivity);
        view.setParent(mMockParent);
        assertFalse(view.performLongClick());
        assertFalse(view.isLongClickable());

        view.setOnLongClickListener(null);
        assertFalse(view.performLongClick());
        assertTrue(view.isLongClickable());

        view.setOnLongClickListener(new OnLongClickListenerImpl());
        assertTrue(view.performLongClick());
        assertTrue(view.isLongClickable());
    }

    public void testAccessOnFocusChangeListener() {
        View view = new View(mActivity);
        OnFocusChangeListener listener = new OnFocusChangeListenerImpl();

        assertNull(view.getOnFocusChangeListener());

        view.setOnFocusChangeListener(listener);
        assertSame(listener, view.getOnFocusChangeListener());
    }

    public void testAccessNextFocusUpId() {
        View view = new View(mActivity);

        assertEquals(View.NO_ID, view.getNextFocusUpId());

        view.setNextFocusUpId(1);
        assertEquals(1, view.getNextFocusUpId());

        view.setNextFocusUpId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, view.getNextFocusUpId());

        view.setNextFocusUpId(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, view.getNextFocusUpId());
    }

    public void testAccessNextFocusDownId() {
        View view = new View(mActivity);

        assertEquals(View.NO_ID, view.getNextFocusDownId());

        view.setNextFocusDownId(1);
        assertEquals(1, view.getNextFocusDownId());

        view.setNextFocusDownId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, view.getNextFocusDownId());

        view.setNextFocusDownId(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, view.getNextFocusDownId());
    }

    public void testAccessNextFocusLeftId() {
        View view = new View(mActivity);

        assertEquals(View.NO_ID, view.getNextFocusLeftId());

        view.setNextFocusLeftId(1);
        assertEquals(1, view.getNextFocusLeftId());

        view.setNextFocusLeftId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, view.getNextFocusLeftId());

        view.setNextFocusLeftId(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, view.getNextFocusLeftId());
    }

    public void testAccessNextFocusRightId() {
        View view = new View(mActivity);

        assertEquals(View.NO_ID, view.getNextFocusRightId());

        view.setNextFocusRightId(1);
        assertEquals(1, view.getNextFocusRightId());

        view.setNextFocusRightId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, view.getNextFocusRightId());

        view.setNextFocusRightId(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, view.getNextFocusRightId());
    }

    public void testAccessMeasuredDimension() {
        MockView view = new MockView(mActivity);
        assertEquals(0, view.getMeasuredWidth());
        assertEquals(0, view.getMeasuredHeight());

        view.setMeasuredDimensionWrapper(20, 30);
        assertEquals(20, view.getMeasuredWidth());
        assertEquals(30, view.getMeasuredHeight());
    }

    public void testMeasure() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        assertTrue(view.hasCalledOnMeasure());
        assertEquals(100, view.getMeasuredWidth());
        assertEquals(200, view.getMeasuredHeight());

        view.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.requestLayout();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.hasCalledOnMeasure());
        assertEquals(100, view.getMeasuredWidth());
        assertEquals(200, view.getMeasuredHeight());

        view.reset();
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(200, 100);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.hasCalledOnMeasure());
        assertEquals(200, view.getMeasuredWidth());
        assertEquals(100, view.getMeasuredHeight());
    }

    public void testAccessLayoutParams() {
        View view = new View(mActivity);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(10, 20);

        assertNull(view.getLayoutParams());

        try {
            view.setLayoutParams(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        assertFalse(view.isLayoutRequested());
        view.setLayoutParams(params);
        assertSame(params, view.getLayoutParams());
        assertTrue(view.isLayoutRequested());
    }

    public void testIsShown() {
        MockView view = new MockView(mActivity);

        view.setVisibility(View.INVISIBLE);
        assertFalse(view.isShown());

        view.setVisibility(View.VISIBLE);
        assertNull(view.getParent());
        assertFalse(view.isShown());

        view.setParent(mMockParent);
        // mMockParent is not a instance of ViewRoot
        assertFalse(view.isShown());
    }

    public void testGetDrawingTime() {
        View view = new View(mActivity);
        // mAttachInfo is null
        assertEquals(0, view.getDrawingTime());

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertEquals(SystemClock.uptimeMillis(), view.getDrawingTime(), 1000);
    }

    public void testScheduleDrawable() {
        View view = new View(mActivity);
        Drawable drawable = new StateListDrawable();
        Runnable what = new Runnable() {
            public void run() {
                // do nothing
            }
        };

        // mAttachInfo is null
        view.scheduleDrawable(drawable, what, 1000);

        view.setBackgroundDrawable(drawable);
        view.scheduleDrawable(drawable, what, 1000);

        view.scheduleDrawable(null, null, -1000);

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        view.scheduleDrawable(drawable, what, 1000);

        view.scheduleDrawable(view.getBackground(), what, 1000);
        view.unscheduleDrawable(view.getBackground(), what);

        view.scheduleDrawable(null, null, -1000);
    }

    public void testUnscheduleDrawable() {
        View view = new View(mActivity);
        Drawable drawable = new StateListDrawable();
        Runnable what = new Runnable() {
            public void run() {
                // do nothing
            }
        };

        // mAttachInfo is null
        view.unscheduleDrawable(drawable, what);

        view.setBackgroundDrawable(drawable);
        view.unscheduleDrawable(drawable);

        view.unscheduleDrawable(null, null);
        view.unscheduleDrawable(null);

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        view.unscheduleDrawable(drawable);

        view.scheduleDrawable(view.getBackground(), what, 1000);
        view.unscheduleDrawable(view.getBackground(), what);

        view.unscheduleDrawable(null);
        view.unscheduleDrawable(null, null);
    }

    public void testGetWindowVisibility() {
        View view = new View(mActivity);
        // mAttachInfo is null
        assertEquals(View.GONE, view.getWindowVisibility());

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertEquals(View.VISIBLE, view.getWindowVisibility());
    }

    public void testGetWindowToken() {
        View view = new View(mActivity);
        // mAttachInfo is null
        assertNull(view.getWindowToken());

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertNotNull(view.getWindowToken());
    }

    public void testHasWindowFocus() {
        View view = new View(mActivity);
        // mAttachInfo is null
        assertFalse(view.hasWindowFocus());

        // mAttachInfo is not null
        final View view2 = mActivity.findViewById(R.id.fit_windows);
        // Wait until the window has been focused.
        new PollingCheck(TIMEOUT_DELTA) {
            @Override
            protected boolean check() {
                return view2.hasWindowFocus();
            }
        }.run();
    }

    public void testGetHandler() {
        MockView view = new MockView(mActivity);
        // mAttachInfo is null
        assertNull(view.getHandler());
    }

    public void testRemoveCallbacks() throws InterruptedException {
        final long delay = 500L;
        View view = mActivity.findViewById(R.id.mock_view);
        MockRunnable runner = new MockRunnable();
        assertTrue(view.postDelayed(runner, delay));
        assertTrue(view.removeCallbacks(runner));
        assertTrue(view.removeCallbacks(null));
        assertTrue(view.removeCallbacks(new MockRunnable()));
        Thread.sleep(delay * 2);
        assertFalse(runner.hasRun);
        // check that the runner actually works
        runner = new MockRunnable();
        assertTrue(view.postDelayed(runner, delay));
        Thread.sleep(delay * 2);
        assertTrue(runner.hasRun);
    }

    public void testCancelLongPress() {
        View view = new View(mActivity);
        // mAttachInfo is null
        view.cancelLongPress();

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        view.cancelLongPress();
    }

    public void testGetViewTreeObserver() {
        View view = new View(mActivity);
        // mAttachInfo is null
        assertNotNull(view.getViewTreeObserver());

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertNotNull(view.getViewTreeObserver());
    }

    public void testGetWindowAttachCount() {
        MockView view = new MockView(mActivity);
        // mAttachInfo is null
        assertEquals(0, view.getWindowAttachCount());
    }

    @UiThreadTest
    public void testOnAttachedToAndDetachedFromWindow() {
        MockView mockView = new MockView(mActivity);
        ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);

        viewGroup.addView(mockView);
        assertTrue(mockView.hasCalledOnAttachedToWindow());

        viewGroup.removeView(mockView);
        assertTrue(mockView.hasCalledOnDetachedFromWindow());

        mockView.reset();
        mActivity.setContentView(mockView);
        assertTrue(mockView.hasCalledOnAttachedToWindow());

        mActivity.setContentView(R.layout.view_layout);
        assertTrue(mockView.hasCalledOnDetachedFromWindow());
    }

    public void testGetLocationInWindow() {
        int[] location = new int[] { -1, -1 };

        View layout = mActivity.findViewById(R.id.viewlayout_root);
        int[] layoutLocation = new int[] { -1, -1 };
        layout.getLocationInWindow(layoutLocation);

        final View mockView = mActivity.findViewById(R.id.mock_view);
        mockView.getLocationInWindow(location);
        assertEquals(layoutLocation[0], location[0]);
        assertEquals(layoutLocation[1], location[1]);

        View scrollView = mActivity.findViewById(R.id.scroll_view);
        scrollView.getLocationInWindow(location);
        assertEquals(layoutLocation[0], location[0]);
        assertEquals(layoutLocation[1] + mockView.getHeight(), location[1]);

        try {
            mockView.getLocationInWindow(null);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            mockView.getLocationInWindow(new int[] { 0 });
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetLocationOnScreen() {
        View view = new View(mActivity);
        int[] location = new int[] { -1, -1 };

        // mAttachInfo is not null
        View layout = mActivity.findViewById(R.id.viewlayout_root);
        int[] layoutLocation = new int[] { -1, -1 };
        layout.getLocationOnScreen(layoutLocation);

        View mockView = mActivity.findViewById(R.id.mock_view);
        mockView.getLocationOnScreen(location);
        assertEquals(layoutLocation[0], location[0]);
        assertEquals(layoutLocation[1], location[1]);

        View scrollView = mActivity.findViewById(R.id.scroll_view);
        scrollView.getLocationOnScreen(location);
        assertEquals(layoutLocation[0], location[0]);
        assertEquals(layoutLocation[1] + mockView.getHeight(), location[1]);

        try {
            scrollView.getLocationOnScreen(null);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            scrollView.getLocationOnScreen(new int[] { 0 });
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testAddTouchables() {
        View view = new View(mActivity);
        ArrayList<View> result = new ArrayList<View>();
        assertEquals(0, result.size());

        view.addTouchables(result);
        assertEquals(0, result.size());

        view.setClickable(true);
        view.addTouchables(result);
        assertEquals(1, result.size());
        assertSame(view, result.get(0));

        try {
            view.addTouchables(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        result.clear();
        view.setEnabled(false);
        assertTrue(view.isClickable());
        view.addTouchables(result);
        assertEquals(0, result.size());
    }

    public void testGetTouchables() {
        View view = new View(mActivity);
        ArrayList<View> result;

        result = view.getTouchables();
        assertEquals(0, result.size());

        view.setClickable(true);
        result = view.getTouchables();
        assertEquals(1, result.size());
        assertSame(view, result.get(0));

        result.clear();
        view.setEnabled(false);
        assertTrue(view.isClickable());
        result = view.getTouchables();
        assertEquals(0, result.size());
    }

    public void testInflate() {
        View view = View.inflate(mActivity, R.layout.view_layout, null);
        assertNotNull(view);
        assertTrue(view instanceof LinearLayout);

        MockView mockView = (MockView) view.findViewById(R.id.mock_view);
        assertNotNull(mockView);
        assertTrue(mockView.hasCalledOnFinishInflate());
    }

    public void testIsInTouchMode() {
        View view = new View(mActivity);
        // mAttachInfo is null
        assertFalse(view.isInTouchMode());

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertFalse(view.isInTouchMode());
    }

    public void testIsInEditMode() {
        View view = new View(mActivity);
        assertFalse(view.isInEditMode());
    }

    public void testPostInvalidate1() {
        View view = new View(mActivity);
        // mAttachInfo is null
        view.postInvalidate();

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        view.postInvalidate();
    }

    public void testPostInvalidate2() {
        View view = new View(mActivity);
        // mAttachInfo is null
        view.postInvalidate(0, 1, 2, 3);

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        view.postInvalidate(10, 20, 30, 40);
        view.postInvalidate(0, -20, -30, -40);
    }

    public void testPostInvalidateDelayed() {
        View view = new View(mActivity);
        // mAttachInfo is null
        view.postInvalidateDelayed(1000);
        view.postInvalidateDelayed(500, 0, 0, 100, 200);

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        view.postInvalidateDelayed(1000);
        view.postInvalidateDelayed(500, 0, 0, 100, 200);
        view.postInvalidateDelayed(-1);
    }

    public void testPost() {
        View view = new View(mActivity);
        MockRunnable action = new MockRunnable();

        // mAttachInfo is null
        assertTrue(view.post(action));
        assertTrue(view.post(null));

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertTrue(view.post(action));
        assertTrue(view.post(null));
    }

    public void testPostDelayed() {
        View view = new View(mActivity);
        MockRunnable action = new MockRunnable();

        // mAttachInfo is null
        assertTrue(view.postDelayed(action, 1000));
        assertTrue(view.postDelayed(null, -1));

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertTrue(view.postDelayed(action, 1000));
        assertTrue(view.postDelayed(null, 0));
    }

    @UiThreadTest
    public void testPlaySoundEffect() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        // sound effect enabled
        view.playSoundEffect(SoundEffectConstants.CLICK);

        // sound effect disabled
        view.setSoundEffectsEnabled(false);
        view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN);

        // no way to assert the soundConstant be really played.
    }

    public void testOnKeyShortcut() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setFocusable(true);
                view.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.isFocused());

        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU);
        getInstrumentation().sendKeySync(event);
        event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        getInstrumentation().sendKeySync(event);
        assertTrue(view.hasCalledOnKeyShortcut());
    }

    public void testOnKeyMultiple() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setFocusable(true);
            }
        });

        assertFalse(view.hasCalledOnKeyMultiple());
        view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_ENTER));
        assertTrue(view.hasCalledOnKeyMultiple());
    }

    @UiThreadTest
    public void testDispatchKeyShortcutEvent() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        view.setFocusable(true);

        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        view.dispatchKeyShortcutEvent(event);
        assertTrue(view.hasCalledOnKeyShortcut());

        try {
            view.dispatchKeyShortcutEvent(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testOnTrackballEvent() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setEnabled(true);
                view.setFocusable(true);
                view.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();

        long downTime = SystemClock.uptimeMillis();
        long eventTime = downTime;
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                1, 2, 0);
        getInstrumentation().sendTrackballEventSync(event);
        getInstrumentation().waitForIdleSync();
        assertTrue(view.hasCalledOnTrackballEvent());
    }

    @UiThreadTest
    public void testDispatchTrackballMoveEvent() {
        ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        MockView mockView1 = new MockView(mActivity);
        MockView mockView2 = new MockView(mActivity);
        viewGroup.addView(mockView1);
        viewGroup.addView(mockView2);
        mockView1.setFocusable(true);
        mockView2.setFocusable(true);
        mockView2.requestFocus();

        long downTime = SystemClock.uptimeMillis();
        long eventTime = downTime;
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                1, 2, 0);
        mockView1.dispatchTrackballEvent(event);
        // issue 1695243
        // It passes a trackball motion event down to itself even if it is not the focused view.
        assertTrue(mockView1.hasCalledOnTrackballEvent());
        assertFalse(mockView2.hasCalledOnTrackballEvent());

        mockView1.reset();
        mockView2.reset();
        downTime = SystemClock.uptimeMillis();
        eventTime = downTime;
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, 1, 2, 0);
        mockView2.dispatchTrackballEvent(event);
        assertFalse(mockView1.hasCalledOnTrackballEvent());
        assertTrue(mockView2.hasCalledOnTrackballEvent());
    }

    public void testDispatchUnhandledMove() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setFocusable(true);
                view.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();

        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
        getInstrumentation().sendKeySync(event);

        assertTrue(view.hasCalledDispatchUnhandledMove());
    }

    public void testWindowVisibilityChanged() throws Throwable {
        final MockView mockView = new MockView(mActivity);
        final ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);

        runTestOnUiThread(new Runnable() {
            public void run() {
                viewGroup.addView(mockView);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mockView.hasCalledOnWindowVisibilityChanged());

        mockView.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                getActivity().setVisible(false);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mockView.hasCalledDispatchWindowVisibilityChanged());
        assertTrue(mockView.hasCalledOnWindowVisibilityChanged());

        mockView.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                getActivity().setVisible(true);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mockView.hasCalledDispatchWindowVisibilityChanged());
        assertTrue(mockView.hasCalledOnWindowVisibilityChanged());

        mockView.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                viewGroup.removeView(mockView);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mockView.hasCalledOnWindowVisibilityChanged());
    }

    public void testGetLocalVisibleRect() throws Throwable {
        final View view = mActivity.findViewById(R.id.mock_view);
        Rect rect = new Rect();

        assertTrue(view.getLocalVisibleRect(rect));
        assertEquals(0, rect.left);
        assertEquals(0, rect.top);
        assertEquals(100, rect.right);
        assertEquals(200, rect.bottom);

        final LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(0, 300);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams1);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.getLocalVisibleRect(rect));

        final LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(200, -10);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams2);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.getLocalVisibleRect(rect));

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int halfWidth = display.getWidth() / 2;
        int halfHeight = display.getHeight() /2;

        final LinearLayout.LayoutParams layoutParams3 =
                new LinearLayout.LayoutParams(halfWidth, halfHeight);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setLayoutParams(layoutParams3);
                view.scrollTo(20, -30);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.getLocalVisibleRect(rect));
        assertEquals(20, rect.left);
        assertEquals(-30, rect.top);
        assertEquals(halfWidth + 20, rect.right);
        assertEquals(halfHeight - 30, rect.bottom);

        try {
            view.getLocalVisibleRect(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testMergeDrawableStates() {
        MockView view = new MockView(mActivity);

        int[] states = view.mergeDrawableStatesWrapper(new int[] { 0, 1, 2, 0, 0 },
                new int[] { 3 });
        assertNotNull(states);
        assertEquals(5, states.length);
        assertEquals(0, states[0]);
        assertEquals(1, states[1]);
        assertEquals(2, states[2]);
        assertEquals(3, states[3]);
        assertEquals(0, states[4]);

        try {
            view.mergeDrawableStatesWrapper(new int[] { 1, 2 }, new int[] { 3 });
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            view.mergeDrawableStatesWrapper(null, new int[] { 0 });
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            view.mergeDrawableStatesWrapper(new int [] { 0 }, null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testOnSaveAndRestoreInstanceState() {
        // it is hard to simulate operation to make callback be called.
    }

    public void testSaveAndRestoreHierarchyState() {
        int viewId = R.id.mock_view;
        MockView view = (MockView) mActivity.findViewById(viewId);
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();
        view.saveHierarchyState(container);
        assertTrue(view.hasCalledDispatchSaveInstanceState());
        assertTrue(view.hasCalledOnSaveInstanceState());
        assertEquals(viewId, container.keyAt(0));

        view.reset();
        container.put(R.id.mock_view, BaseSavedState.EMPTY_STATE);
        view.restoreHierarchyState(container);
        assertTrue(view.hasCalledDispatchRestoreInstanceState());
        assertTrue(view.hasCalledOnRestoreInstanceState());
        container.clear();
        view.saveHierarchyState(container);
        assertTrue(view.hasCalledDispatchSaveInstanceState());
        assertTrue(view.hasCalledOnSaveInstanceState());
        assertEquals(viewId, container.keyAt(0));

        container.clear();
        container.put(viewId, new BaseSavedState(BaseSavedState.EMPTY_STATE));
        try {
            view.restoreHierarchyState(container);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            view.restoreHierarchyState(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            view.saveHierarchyState(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testOnKeyDownOrUp() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setFocusable(true);
                view.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.isFocused());

        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        getInstrumentation().sendKeySync(event);
        assertTrue(view.hasCalledOnKeyDown());

        event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0);
        getInstrumentation().sendKeySync(event);
        assertTrue(view.hasCalledOnKeyUp());

        view.reset();
        assertTrue(view.isEnabled());
        assertFalse(view.isClickable());
        assertFalse(view.isPressed());
        event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER);
        getInstrumentation().sendKeySync(event);
        assertFalse(view.isPressed());
        assertTrue(view.hasCalledOnKeyDown());

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setEnabled(true);
                view.setClickable(true);
            }
        });
        view.reset();
        OnClickListenerImpl listener = new OnClickListenerImpl();
        view.setOnClickListener(listener);

        assertFalse(view.isPressed());
        event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER);
        getInstrumentation().sendKeySync(event);
        assertTrue(view.isPressed());
        assertTrue(view.hasCalledOnKeyDown());
        event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER);
        getInstrumentation().sendKeySync(event);
        assertFalse(view.isPressed());
        assertTrue(view.hasCalledOnKeyUp());
        assertTrue(listener.hasOnClick());

        view.setPressed(false);
        listener.reset();
        view.reset();

        assertFalse(view.isPressed());
        event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
        getInstrumentation().sendKeySync(event);
        assertTrue(view.isPressed());
        assertTrue(view.hasCalledOnKeyDown());
        event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
        getInstrumentation().sendKeySync(event);
        assertFalse(view.isPressed());
        assertTrue(view.hasCalledOnKeyUp());
        assertTrue(listener.hasOnClick());
    }

    @UiThreadTest
    public void testDispatchKeyEvent() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        MockView mockView1 = new MockView(mActivity);
        MockView mockView2 = new MockView(mActivity);
        ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        viewGroup.addView(mockView1);
        viewGroup.addView(mockView2);
        view.setFocusable(true);
        mockView1.setFocusable(true);
        mockView2.setFocusable(true);

        assertFalse(view.hasCalledOnKeyDown());
        assertFalse(mockView1.hasCalledOnKeyDown());
        assertFalse(mockView2.hasCalledOnKeyDown());
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        assertFalse(view.dispatchKeyEvent(event));
        assertTrue(view.hasCalledOnKeyDown());
        assertFalse(mockView1.hasCalledOnKeyDown());
        assertFalse(mockView2.hasCalledOnKeyDown());

        view.reset();
        mockView1.reset();
        mockView2.reset();
        assertFalse(view.hasCalledOnKeyDown());
        assertFalse(mockView1.hasCalledOnKeyDown());
        assertFalse(mockView2.hasCalledOnKeyDown());
        event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        assertFalse(mockView1.dispatchKeyEvent(event));
        assertFalse(view.hasCalledOnKeyDown());
        // issue 1695243
        // When the view has NOT focus, it dispatches to itself, which disobey the javadoc.
        assertTrue(mockView1.hasCalledOnKeyDown());
        assertFalse(mockView2.hasCalledOnKeyDown());

        assertFalse(view.hasCalledOnKeyUp());
        event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0);
        assertFalse(view.dispatchKeyEvent(event));
        assertTrue(view.hasCalledOnKeyUp());

        assertFalse(view.hasCalledOnKeyMultiple());
        event = new KeyEvent(1, 2, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_0, 2);
        assertFalse(view.dispatchKeyEvent(event));
        assertTrue(view.hasCalledOnKeyMultiple());

        try {
            view.dispatchKeyEvent(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        view.reset();
        event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0);
        OnKeyListenerImpl listener = new OnKeyListenerImpl();
        view.setOnKeyListener(listener);
        assertFalse(listener.hasOnKey());
        assertTrue(view.dispatchKeyEvent(event));
        assertTrue(listener.hasOnKey());
        assertFalse(view.hasCalledOnKeyUp());
    }

    @UiThreadTest
    public void testDispatchTouchEvent() {
        ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        MockView mockView1 = new MockView(mActivity);
        MockView mockView2 = new MockView(mActivity);
        viewGroup.addView(mockView1);
        viewGroup.addView(mockView2);

        int[] xy = new int[2];
        mockView1.getLocationOnScreen(xy);

        final int viewWidth = mockView1.getWidth();
        final int viewHeight = mockView1.getHeight();
        final float x = xy[0] + viewWidth / 2.0f;
        final float y = xy[1] + viewHeight / 2.0f;

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                x, y, 0);

        assertFalse(mockView1.hasCalledOnTouchEvent());
        assertFalse(mockView1.dispatchTouchEvent(event));
        assertTrue(mockView1.hasCalledOnTouchEvent());

        assertFalse(mockView2.hasCalledOnTouchEvent());
        assertFalse(mockView2.dispatchTouchEvent(event));
        // issue 1695243
        // it passes the touch screen motion event down to itself even if it is not the target view.
        assertTrue(mockView2.hasCalledOnTouchEvent());

        mockView1.reset();
        OnTouchListenerImpl listener = new OnTouchListenerImpl();
        mockView1.setOnTouchListener(listener);
        assertFalse(listener.hasOnTouch());
        assertTrue(mockView1.dispatchTouchEvent(event));
        assertTrue(listener.hasOnTouch());
        assertFalse(mockView1.hasCalledOnTouchEvent());
    }

    public void testInvalidate1() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        assertTrue(view.hasCalledOnDraw());

        view.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.invalidate();
            }
        });
        getInstrumentation().waitForIdleSync();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return view.hasCalledOnDraw();
            }
        }.run();

        view.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setVisibility(View.INVISIBLE);
                view.invalidate();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.hasCalledOnDraw());
    }

    public void testInvalidate2() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        assertTrue(view.hasCalledOnDraw());

        try {
            view.invalidate(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        view.reset();
        final Rect dirty = new Rect(view.getLeft() + 1, view.getTop() + 1,
                view.getLeft() + view.getWidth() / 2, view.getTop() + view.getHeight() / 2);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.invalidate(dirty);
            }
        });
        getInstrumentation().waitForIdleSync();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return view.hasCalledOnDraw();
            }
        }.run();

        view.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setVisibility(View.INVISIBLE);
                view.invalidate(dirty);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.hasCalledOnDraw());
    }

    public void testInvalidate3() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        assertTrue(view.hasCalledOnDraw());

        view.reset();
        final Rect dirty = new Rect(view.getLeft() + 1, view.getTop() + 1,
                view.getLeft() + view.getWidth() / 2, view.getTop() + view.getHeight() / 2);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.invalidate(dirty.left, dirty.top, dirty.right, dirty.bottom);
            }
        });
        getInstrumentation().waitForIdleSync();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return view.hasCalledOnDraw();
            }
        }.run();

        view.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setVisibility(View.INVISIBLE);
                view.invalidate(dirty.left, dirty.top, dirty.right, dirty.bottom);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.hasCalledOnDraw());
    }

    public void testInvalidateDrawable() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        final Drawable d1 = mResources.getDrawable(R.drawable.scenery);
        final Drawable d2 = mResources.getDrawable(R.drawable.pass);

        view.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setBackgroundDrawable(d1);
                view.invalidateDrawable(d1);
            }
        });
        getInstrumentation().waitForIdleSync();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return view.hasCalledOnDraw();
            }
        }.run();

        view.reset();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.invalidateDrawable(d2);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(view.hasCalledOnDraw());

        MockView viewTestNull = new MockView(mActivity);
        try {
            viewTestNull.invalidateDrawable(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @UiThreadTest
    public void testOnFocusChanged() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        mActivity.findViewById(R.id.fit_windows).setFocusable(true);
        view.setFocusable(true);
        assertFalse(view.hasCalledOnFocusChanged());

        view.requestFocus();
        assertTrue(view.hasCalledOnFocusChanged());

        view.reset();
        view.clearFocus();
        assertTrue(view.hasCalledOnFocusChanged());
    }

    public void testDrawableState() {
        MockView view = new MockView(mActivity);
        view.setParent(mMockParent);

        assertFalse(view.hasCalledOnCreateDrawableState());
        assertTrue(Arrays.equals(MockView.getEnabledStateSet(), view.getDrawableState()));
        assertTrue(view.hasCalledOnCreateDrawableState());

        view.reset();
        assertFalse(view.hasCalledOnCreateDrawableState());
        assertTrue(Arrays.equals(MockView.getEnabledStateSet(), view.getDrawableState()));
        assertFalse(view.hasCalledOnCreateDrawableState());

        view.reset();
        assertFalse(view.hasCalledDrawableStateChanged());
        view.setPressed(true);
        assertTrue(view.hasCalledDrawableStateChanged());
        assertFalse(view.hasCalledOnCreateDrawableState());
        assertTrue(Arrays.equals(MockView.getPressedEnabledStateSet(), view.getDrawableState()));
        assertTrue(view.hasCalledOnCreateDrawableState());

        view.reset();
        mMockParent.reset();
        assertFalse(view.hasCalledDrawableStateChanged());
        assertFalse(mMockParent.hasChildDrawableStateChanged());
        view.refreshDrawableState();
        assertTrue(view.hasCalledDrawableStateChanged());
        assertTrue(mMockParent.hasChildDrawableStateChanged());
        assertFalse(view.hasCalledOnCreateDrawableState());
        assertTrue(Arrays.equals(MockView.getPressedEnabledStateSet(), view.getDrawableState()));
        assertTrue(view.hasCalledOnCreateDrawableState());
    }

    public void testWindowFocusChanged() {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        // Wait until the window has been focused.
        new PollingCheck(TIMEOUT_DELTA) {
            @Override
            protected boolean check() {
                return view.hasWindowFocus();
            }
        }.run();

        new PollingCheck() {
            protected boolean check() {
                return view.hasCalledOnWindowFocusChanged();
            }
        }.run();

        assertTrue(view.hasCalledOnWindowFocusChanged());
        assertTrue(view.hasCalledDispatchWindowFocusChanged());

        view.reset();
        assertFalse(view.hasCalledOnWindowFocusChanged());
        assertFalse(view.hasCalledDispatchWindowFocusChanged());

        StubActivity activity = launchActivity("com.android.cts.stub", StubActivity.class, null);

        // Wait until the window lost focus.
        new PollingCheck(TIMEOUT_DELTA) {
            @Override
            protected boolean check() {
                return !view.hasWindowFocus();
            }
        }.run();

        assertTrue(view.hasCalledOnWindowFocusChanged());
        assertTrue(view.hasCalledDispatchWindowFocusChanged());

        activity.finish();
    }

    public void testDraw() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.requestLayout();
            }
        });
        getInstrumentation().waitForIdleSync();

        assertTrue(view.hasCalledOnDraw());
        assertTrue(view.hasCalledDispatchDraw());
    }

    public void testRequestFocusFromTouch() {
        View view = new View(mActivity);
        view.setFocusable(true);
        assertFalse(view.isFocused());

        view.requestFocusFromTouch();
        assertTrue(view.isFocused());

        view.requestFocusFromTouch();
        assertTrue(view.isFocused());
    }

    public void testRequestRectangleOnScreen1() {
        MockView view = new MockView(mActivity);
        Rect rectangle = new Rect(10, 10, 20, 30);
        MockViewGroupParent parent = new MockViewGroupParent(mActivity);

        // parent is null
        assertFalse(view.requestRectangleOnScreen(rectangle, true));
        assertFalse(view.requestRectangleOnScreen(rectangle, false));
        assertFalse(view.requestRectangleOnScreen(null, true));

        view.setParent(parent);
        view.scrollTo(1, 2);
        assertFalse(parent.hasRequestChildRectangleOnScreen());

        assertFalse(view.requestRectangleOnScreen(rectangle, true));
        assertTrue(parent.hasRequestChildRectangleOnScreen());

        parent.reset();
        view.scrollTo(11, 22);
        assertFalse(parent.hasRequestChildRectangleOnScreen());

        assertFalse(view.requestRectangleOnScreen(rectangle, true));
        assertTrue(parent.hasRequestChildRectangleOnScreen());

        try {
            view.requestRectangleOnScreen(null, true);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testRequestRectangleOnScreen2() {
        MockView view = new MockView(mActivity);
        Rect rectangle = new Rect();
        MockViewGroupParent parent = new MockViewGroupParent(mActivity);

        final Rect requestedRect = new Rect();
        MockViewGroupParent grandparent = new MockViewGroupParent(mActivity) {
            @Override
            public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
                    boolean immediate) {
                requestedRect.set(rectangle);
                return super.requestChildRectangleOnScreen(child, rectangle, immediate);
            }
        };

        // parent is null
        assertFalse(view.requestRectangleOnScreen(rectangle));
        assertFalse(view.requestRectangleOnScreen(null));
        assertEquals(0, rectangle.left);
        assertEquals(0, rectangle.top);
        assertEquals(0, rectangle.right);
        assertEquals(0, rectangle.bottom);

        parent.addView(view);
        parent.scrollTo(1, 2);
        grandparent.addView(parent);

        assertFalse(parent.hasRequestChildRectangleOnScreen());
        assertFalse(grandparent.hasRequestChildRectangleOnScreen());

        assertFalse(view.requestRectangleOnScreen(rectangle));

        assertTrue(parent.hasRequestChildRectangleOnScreen());
        assertTrue(grandparent.hasRequestChildRectangleOnScreen());

        assertEquals(-1, requestedRect.left);
        assertEquals(-2, requestedRect.top);
        assertEquals(-1, requestedRect.right);
        assertEquals(-2, requestedRect.bottom);

        try {
            view.requestRectangleOnScreen(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * For the duration of the tap timeout we are in a 'prepressed' state
     * to differentiate between taps and touch scrolls.
     * Wait at least this long before testing if the view is pressed
     * by calling this function.
     */
    private void waitPrepressedTimeout() {
        try {
            Thread.sleep(ViewConfiguration.getTapTimeout() + 10);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "waitPrepressedTimeout() interrupted! Test may fail!", e);
        }
        getInstrumentation().waitForIdleSync();
    }

    public void testOnTouchEvent() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        assertTrue(view.isEnabled());
        assertFalse(view.isClickable());
        assertFalse(view.isLongClickable());

        TouchUtils.clickView(this, view);
        assertTrue(view.hasCalledOnTouchEvent());

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setEnabled(true);
                view.setClickable(true);
                view.setLongClickable(true);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.isEnabled());
        assertTrue(view.isClickable());
        assertTrue(view.isLongClickable());

        // MotionEvent.ACTION_DOWN
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);

        final int viewWidth = view.getWidth();
        final int viewHeight = view.getHeight();
        float x = xy[0] + viewWidth / 2.0f;
        float y = xy[1] + viewHeight / 2.0f;

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN,
                x, y, 0);
        assertFalse(view.isPressed());
        getInstrumentation().sendPointerSync(event);
        waitPrepressedTimeout();
        assertTrue(view.hasCalledOnTouchEvent());
        assertTrue(view.isPressed());

        // MotionEvent.ACTION_MOVE
        // move out of the bound.
        view.reset();
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        int slop = ViewConfiguration.get(mActivity).getScaledTouchSlop();
        x = xy[0] + viewWidth + slop;
        y = xy[1] + viewHeight + slop;
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        assertTrue(view.hasCalledOnTouchEvent());
        assertFalse(view.isPressed());

        // move into view
        view.reset();
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        x = xy[0] + viewWidth - 1;
        y = xy[1] + viewHeight - 1;
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        waitPrepressedTimeout();
        assertTrue(view.hasCalledOnTouchEvent());
        assertFalse(view.isPressed());

        // MotionEvent.ACTION_UP
        OnClickListenerImpl listener = new OnClickListenerImpl();
        view.setOnClickListener(listener);
        view.reset();
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        assertTrue(view.hasCalledOnTouchEvent());
        assertFalse(listener.hasOnClick());

        view.reset();
        x = xy[0] + viewWidth / 2.0f;
        y = xy[1] + viewHeight / 2.0f;
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        assertTrue(view.hasCalledOnTouchEvent());

        // MotionEvent.ACTION_CANCEL
        view.reset();
        listener.reset();
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_CANCEL, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        assertTrue(view.hasCalledOnTouchEvent());
        assertFalse(view.isPressed());
        assertFalse(listener.hasOnClick());
    }

    public void testBringToFront() {
        MockView view = new MockView(mActivity);
        view.setParent(mMockParent);

        assertFalse(mMockParent.hasBroughtChildToFront());
        view.bringToFront();
        assertTrue(mMockParent.hasBroughtChildToFront());
    }

    public void testGetApplicationWindowToken() {
        View view = new View(mActivity);
        // mAttachInfo is null
        assertNull(view.getApplicationWindowToken());

        // mAttachInfo is not null
        view = mActivity.findViewById(R.id.fit_windows);
        assertNotNull(view.getApplicationWindowToken());
    }

    public void testGetBottomPaddingOffset() {
        MockView view = new MockView(mActivity);
        assertEquals(0, view.getBottomPaddingOffset());
    }

    public void testGetLeftPaddingOffset() {
        MockView view = new MockView(mActivity);
        assertEquals(0, view.getLeftPaddingOffset());
    }

    public void testGetRightPaddingOffset() {
        MockView view = new MockView(mActivity);
        assertEquals(0, view.getRightPaddingOffset());
    }

    public void testGetTopPaddingOffset() {
        MockView view = new MockView(mActivity);
        assertEquals(0, view.getTopPaddingOffset());
    }

    public void testIsPaddingOffsetRequired() {
        MockView view = new MockView(mActivity);
        assertFalse(view.isPaddingOffsetRequired());
    }

    @UiThreadTest
    public void testPadding() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view_padding_full);
        Drawable background = view.getBackground();
        Rect backgroundPadding = new Rect();
        background.getPadding(backgroundPadding);

        // There is some background with a non null padding
        assertNotNull(background);
        assertTrue(backgroundPadding.left != 0);
        assertTrue(backgroundPadding.right != 0);
        assertTrue(backgroundPadding.top != 0);
        assertTrue(backgroundPadding.bottom != 0);

        // The XML defines android:padding="0dp" and that should be the resulting padding
        assertEquals(0, view.getPaddingLeft());
        assertEquals(0, view.getPaddingTop());
        assertEquals(0, view.getPaddingRight());
        assertEquals(0, view.getPaddingBottom());

        // LEFT case
        view = (MockView) mActivity.findViewById(R.id.mock_view_padding_left);
        background = view.getBackground();
        backgroundPadding = new Rect();
        background.getPadding(backgroundPadding);

        // There is some background with a non null padding
        assertNotNull(background);
        assertTrue(backgroundPadding.left != 0);
        assertTrue(backgroundPadding.right != 0);
        assertTrue(backgroundPadding.top != 0);
        assertTrue(backgroundPadding.bottom != 0);

        // The XML defines android:paddingLeft="0dp" and that should be the resulting padding
        assertEquals(0, view.getPaddingLeft());
        assertEquals(backgroundPadding.top, view.getPaddingTop());
        assertEquals(backgroundPadding.right, view.getPaddingRight());
        assertEquals(backgroundPadding.bottom, view.getPaddingBottom());

        // RIGHT case
        view = (MockView) mActivity.findViewById(R.id.mock_view_padding_right);
        background = view.getBackground();
        backgroundPadding = new Rect();
        background.getPadding(backgroundPadding);

        // There is some background with a non null padding
        assertNotNull(background);
        assertTrue(backgroundPadding.left != 0);
        assertTrue(backgroundPadding.right != 0);
        assertTrue(backgroundPadding.top != 0);
        assertTrue(backgroundPadding.bottom != 0);

        // The XML defines android:paddingRight="0dp" and that should be the resulting padding
        assertEquals(backgroundPadding.left, view.getPaddingLeft());
        assertEquals(backgroundPadding.top, view.getPaddingTop());
        assertEquals(0, view.getPaddingRight());
        assertEquals(backgroundPadding.bottom, view.getPaddingBottom());

        // TOP case
        view = (MockView) mActivity.findViewById(R.id.mock_view_padding_top);
        background = view.getBackground();
        backgroundPadding = new Rect();
        background.getPadding(backgroundPadding);

        // There is some background with a non null padding
        assertNotNull(background);
        assertTrue(backgroundPadding.left != 0);
        assertTrue(backgroundPadding.right != 0);
        assertTrue(backgroundPadding.top != 0);
        assertTrue(backgroundPadding.bottom != 0);

        // The XML defines android:paddingTop="0dp" and that should be the resulting padding
        assertEquals(backgroundPadding.left, view.getPaddingLeft());
        assertEquals(0, view.getPaddingTop());
        assertEquals(backgroundPadding.right, view.getPaddingRight());
        assertEquals(backgroundPadding.bottom, view.getPaddingBottom());

        // BOTTOM case
        view = (MockView) mActivity.findViewById(R.id.mock_view_padding_bottom);
        background = view.getBackground();
        backgroundPadding = new Rect();
        background.getPadding(backgroundPadding);

        // There is some background with a non null padding
        assertNotNull(background);
        assertTrue(backgroundPadding.left != 0);
        assertTrue(backgroundPadding.right != 0);
        assertTrue(backgroundPadding.top != 0);
        assertTrue(backgroundPadding.bottom != 0);

        // The XML defines android:paddingBottom="0dp" and that should be the resulting padding
        assertEquals(backgroundPadding.left, view.getPaddingLeft());
        assertEquals(backgroundPadding.top, view.getPaddingTop());
        assertEquals(backgroundPadding.right, view.getPaddingRight());
        assertEquals(0, view.getPaddingBottom());

        // Case for interleaved background/padding changes
        view = (MockView) mActivity.findViewById(R.id.mock_view_padding_runtime_updated);
        background = view.getBackground();
        backgroundPadding = new Rect();
        background.getPadding(backgroundPadding);

        // There is some background with a null padding
        assertNotNull(background);
        assertTrue(backgroundPadding.left == 0);
        assertTrue(backgroundPadding.right == 0);
        assertTrue(backgroundPadding.top == 0);
        assertTrue(backgroundPadding.bottom == 0);

        final int paddingLeft = view.getPaddingLeft();
        final int paddingRight = view.getPaddingRight();
        final int paddingTop = view.getPaddingTop();
        final int paddingBottom = view.getPaddingBottom();
        assertEquals(8, paddingLeft);
        assertEquals(0, paddingTop);
        assertEquals(8, paddingRight);
        assertEquals(0, paddingBottom);

        // Manipulate background and padding
        background.setState(view.getDrawableState());
        background.jumpToCurrentState();
        view.setBackground(background);
        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

        assertEquals(8, view.getPaddingLeft());
        assertEquals(0, view.getPaddingTop());
        assertEquals(8, view.getPaddingRight());
        assertEquals(0, view.getPaddingBottom());
    }

    public void testGetWindowVisibleDisplayFrame() {
        Rect outRect = new Rect();
        View view = new View(mActivity);
        // mAttachInfo is null
        WindowManager wm = (WindowManager)mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        view.getWindowVisibleDisplayFrame(outRect);
        assertEquals(0, outRect.left);
        assertEquals(0, outRect.top);
        assertEquals(d.getWidth(), outRect.right);
        assertEquals(d.getHeight(), outRect.bottom);

        // mAttachInfo is not null
        outRect = new Rect();
        view = mActivity.findViewById(R.id.fit_windows);
        // it's implementation detail
        view.getWindowVisibleDisplayFrame(outRect);
    }

    public void testSetScrollContainer() throws Throwable {
        final MockView mockView = (MockView) mActivity.findViewById(R.id.mock_view);
        final MockView scrollView = (MockView) mActivity.findViewById(R.id.scroll_view);
        Bitmap bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.RGB_565);
        final BitmapDrawable d = new BitmapDrawable(bitmap);
        final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(300, 500);
        runTestOnUiThread(new Runnable() {
            public void run() {
                mockView.setBackgroundDrawable(d);
                mockView.setHorizontalFadingEdgeEnabled(true);
                mockView.setVerticalFadingEdgeEnabled(true);
                mockView.setLayoutParams(layoutParams);
                scrollView.setLayoutParams(layoutParams);

                mockView.setFocusable(true);
                mockView.requestFocus();
                mockView.setScrollContainer(true);
                scrollView.setScrollContainer(false);
                imm.showSoftInput(mockView, 0);
            }
        });
        getInstrumentation().waitForIdleSync();

        // FIXME: why the size of view doesn't change?

        runTestOnUiThread(new Runnable() {
            public void run() {
                imm.hideSoftInputFromInputMethod(mockView.getWindowToken(), 0);
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    public void testTouchMode() throws Throwable {
        final MockView mockView = (MockView) mActivity.findViewById(R.id.mock_view);
        final View fitWindowsView = mActivity.findViewById(R.id.fit_windows);
        runTestOnUiThread(new Runnable() {
            public void run() {
                mockView.setFocusableInTouchMode(true);
                fitWindowsView.setFocusable(true);
                fitWindowsView.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mockView.isFocusableInTouchMode());
        assertFalse(fitWindowsView.isFocusableInTouchMode());
        assertTrue(mockView.isFocusable());
        assertTrue(fitWindowsView.isFocusable());
        assertFalse(mockView.isFocused());
        assertTrue(fitWindowsView.isFocused());
        assertFalse(mockView.isInTouchMode());
        assertFalse(fitWindowsView.isInTouchMode());

        TouchUtils.tapView(this, mockView);
        assertFalse(fitWindowsView.isFocused());
        assertFalse(mockView.isFocused());
        runTestOnUiThread(new Runnable() {
            public void run() {
                mockView.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(mockView.isFocused());
        runTestOnUiThread(new Runnable() {
            public void run() {
                fitWindowsView.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(fitWindowsView.isFocused());
        assertTrue(mockView.isInTouchMode());
        assertTrue(fitWindowsView.isInTouchMode());

        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0);
        getInstrumentation().sendKeySync(keyEvent);
        assertTrue(mockView.isFocused());
        assertFalse(fitWindowsView.isFocused());
        runTestOnUiThread(new Runnable() {
            public void run() {
                fitWindowsView.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(mockView.isFocused());
        assertTrue(fitWindowsView.isFocused());
        assertFalse(mockView.isInTouchMode());
        assertFalse(fitWindowsView.isInTouchMode());
    }

    @UiThreadTest
    public void testScrollbarStyle() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        Bitmap bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.RGB_565);
        BitmapDrawable d = new BitmapDrawable(bitmap);
        view.setBackgroundDrawable(d);
        view.setHorizontalFadingEdgeEnabled(true);
        view.setVerticalFadingEdgeEnabled(true);

        view.setHorizontalScrollBarEnabled(true);
        view.setVerticalScrollBarEnabled(true);
        view.initializeScrollbars(mActivity.obtainStyledAttributes(android.R.styleable.View));
        assertTrue(view.isHorizontalScrollBarEnabled());
        assertTrue(view.isVerticalScrollBarEnabled());
        int verticalScrollBarWidth = view.getVerticalScrollbarWidth();
        int horizontalScrollBarHeight = view.getHorizontalScrollbarHeight();
        assertTrue(verticalScrollBarWidth > 0);
        assertTrue(horizontalScrollBarHeight > 0);
        assertEquals(0, view.getPaddingRight());
        assertEquals(0, view.getPaddingBottom());

        view.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        assertEquals(View.SCROLLBARS_INSIDE_INSET, view.getScrollBarStyle());
        assertEquals(verticalScrollBarWidth, view.getPaddingRight());
        assertEquals(horizontalScrollBarHeight, view.getPaddingBottom());

        view.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        assertEquals(View.SCROLLBARS_OUTSIDE_OVERLAY, view.getScrollBarStyle());
        assertEquals(0, view.getPaddingRight());
        assertEquals(0, view.getPaddingBottom());

        view.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
        assertEquals(View.SCROLLBARS_OUTSIDE_INSET, view.getScrollBarStyle());
        assertEquals(verticalScrollBarWidth, view.getPaddingRight());
        assertEquals(horizontalScrollBarHeight, view.getPaddingBottom());

        // TODO: how to get the position of the Scrollbar to assert it is inside or outside.
    }

    @UiThreadTest
    public void testScrollFading() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        Bitmap bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.RGB_565);
        BitmapDrawable d = new BitmapDrawable(bitmap);
        view.setBackgroundDrawable(d);

        assertFalse(view.isHorizontalFadingEdgeEnabled());
        assertFalse(view.isVerticalFadingEdgeEnabled());
        assertEquals(0, view.getHorizontalFadingEdgeLength());
        assertEquals(0, view.getVerticalFadingEdgeLength());

        view.setHorizontalFadingEdgeEnabled(true);
        view.setVerticalFadingEdgeEnabled(true);
        assertTrue(view.isHorizontalFadingEdgeEnabled());
        assertTrue(view.isVerticalFadingEdgeEnabled());
        assertTrue(view.getHorizontalFadingEdgeLength() > 0);
        assertTrue(view.getVerticalFadingEdgeLength() > 0);

        final int fadingLength = 20;
        view.setFadingEdgeLength(fadingLength);
        assertEquals(fadingLength, view.getHorizontalFadingEdgeLength());
        assertEquals(fadingLength, view.getVerticalFadingEdgeLength());
    }

    @UiThreadTest
    public void testScrolling() {
        MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        view.reset();
        assertEquals(0, view.getScrollX());
        assertEquals(0, view.getScrollY());
        assertFalse(view.hasCalledOnScrollChanged());

        view.scrollTo(0, 0);
        assertEquals(0, view.getScrollX());
        assertEquals(0, view.getScrollY());
        assertFalse(view.hasCalledOnScrollChanged());

        view.scrollBy(0, 0);
        assertEquals(0, view.getScrollX());
        assertEquals(0, view.getScrollY());
        assertFalse(view.hasCalledOnScrollChanged());

        view.scrollTo(10, 100);
        assertEquals(10, view.getScrollX());
        assertEquals(100, view.getScrollY());
        assertTrue(view.hasCalledOnScrollChanged());

        view.reset();
        assertFalse(view.hasCalledOnScrollChanged());
        view.scrollBy(-10, -100);
        assertEquals(0, view.getScrollX());
        assertEquals(0, view.getScrollY());
        assertTrue(view.hasCalledOnScrollChanged());

        view.reset();
        assertFalse(view.hasCalledOnScrollChanged());
        view.scrollTo(-1, -2);
        assertEquals(-1, view.getScrollX());
        assertEquals(-2, view.getScrollY());
        assertTrue(view.hasCalledOnScrollChanged());
    }

    public void testInitializeScrollbarsAndFadingEdge() {
        MockView view = (MockView) mActivity.findViewById(R.id.scroll_view);

        assertTrue(view.isHorizontalScrollBarEnabled());
        assertTrue(view.isVerticalScrollBarEnabled());
        assertFalse(view.isHorizontalFadingEdgeEnabled());
        assertFalse(view.isVerticalFadingEdgeEnabled());

        view = (MockView) mActivity.findViewById(R.id.scroll_view_2);
        final int fadingEdgeLength = 20;

        assertTrue(view.isHorizontalScrollBarEnabled());
        assertTrue(view.isVerticalScrollBarEnabled());
        assertTrue(view.isHorizontalFadingEdgeEnabled());
        assertTrue(view.isVerticalFadingEdgeEnabled());
        assertEquals(fadingEdgeLength, view.getHorizontalFadingEdgeLength());
        assertEquals(fadingEdgeLength, view.getVerticalFadingEdgeLength());
    }

    public void testOnStartAndFinishTemporaryDetach() throws Throwable {
        final MockListView listView = new MockListView(mActivity);
        List<String> items = Lists.newArrayList("1", "2", "3");
        final Adapter<String> adapter = new Adapter<String>(mActivity, 0, items);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(listView);
                listView.setAdapter(adapter);
            }
        });
        getInstrumentation().waitForIdleSync();
        final MockView focusChild = (MockView) listView.getChildAt(0);

        runTestOnUiThread(new Runnable() {
            public void run() {
                focusChild.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(listView.getChildAt(0).isFocused());

        runTestOnUiThread(new Runnable() {
            public void run() {
                listView.detachViewFromParent(focusChild);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertFalse(listView.hasCalledOnStartTemporaryDetach());
        assertFalse(listView.hasCalledOnFinishTemporaryDetach());
    }

    private static class MockListView extends ListView {
        private boolean mCalledOnStartTemporaryDetach = false;
        private boolean mCalledOnFinishTemporaryDetach = false;

        public MockListView(Context context) {
            super(context);
        }

        @Override
        protected void detachViewFromParent(View child) {
            super.detachViewFromParent(child);
        }

        @Override
        public void onFinishTemporaryDetach() {
            super.onFinishTemporaryDetach();
            mCalledOnFinishTemporaryDetach = true;
        }

        public boolean hasCalledOnFinishTemporaryDetach() {
            return mCalledOnFinishTemporaryDetach;
        }

        @Override
        public void onStartTemporaryDetach() {
            super.onStartTemporaryDetach();
            mCalledOnStartTemporaryDetach = true;
        }

        public boolean hasCalledOnStartTemporaryDetach() {
            return mCalledOnStartTemporaryDetach;
        }

        public void reset() {
            mCalledOnStartTemporaryDetach = false;
            mCalledOnFinishTemporaryDetach = false;
        }
    }

    private static class Adapter<T> extends ArrayAdapter<T> {
        ArrayList<MockView> views = new ArrayList<MockView>();

        public Adapter(Context context, int textViewResourceId, List<T> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); i++) {
                views.add(new MockView(context));
                views.get(i).setFocusable(true);
            }
        }

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return views.get(position);
        }
    }

    public void testKeyPreIme() throws Throwable {
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.setFocusable(true);
                view.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        assertTrue(view.hasCalledDispatchKeyEventPreIme());
        assertTrue(view.hasCalledOnKeyPreIme());
    }

    public void testHapticFeedback() {
        Vibrator vib = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        boolean hasVibrator = vib.hasVibrator();

        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        final int LONG_PRESS = HapticFeedbackConstants.LONG_PRESS;
        final int FLAG_IGNORE_VIEW_SETTING = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING;
        final int FLAG_IGNORE_GLOBAL_SETTING = HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING;
        final int ALWAYS = FLAG_IGNORE_VIEW_SETTING | FLAG_IGNORE_GLOBAL_SETTING;

        view.setHapticFeedbackEnabled(false);
        assertFalse(view.isHapticFeedbackEnabled());
        assertFalse(view.performHapticFeedback(LONG_PRESS));
        assertFalse(view.performHapticFeedback(LONG_PRESS, FLAG_IGNORE_GLOBAL_SETTING));
        assertEquals(hasVibrator, view.performHapticFeedback(LONG_PRESS, ALWAYS));

        view.setHapticFeedbackEnabled(true);
        assertTrue(view.isHapticFeedbackEnabled());
        assertEquals(hasVibrator, view.performHapticFeedback(LONG_PRESS, FLAG_IGNORE_GLOBAL_SETTING));
    }

    public void testInputConnection() throws Throwable {
        final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        final MockView view = (MockView) mActivity.findViewById(R.id.mock_view);
        final ViewGroup viewGroup = (ViewGroup) mActivity.findViewById(R.id.viewlayout_root);
        final MockEditText editText = new MockEditText(mActivity);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewGroup.addView(editText);
                editText.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(editText.isFocused());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                imm.showSoftInput(editText, 0);
            }
        });
        getInstrumentation().waitForIdleSync();

        new PollingCheck(TIMEOUT_DELTA) {
            @Override
            protected boolean check() {
                return editText.hasCalledOnCreateInputConnection();
            }
        }.run();

        assertTrue(editText.hasCalledOnCheckIsTextEditor());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue(imm.isActive(editText));
                assertFalse(editText.hasCalledCheckInputConnectionProxy());
                imm.isActive(view);
                assertTrue(editText.hasCalledCheckInputConnectionProxy());
            }
        });
    }

    private static class MockEditText extends EditText {
        private boolean mCalledCheckInputConnectionProxy = false;
        private boolean mCalledOnCreateInputConnection = false;
        private boolean mCalledOnCheckIsTextEditor = false;

        public MockEditText(Context context) {
            super(context);
        }

        @Override
        public boolean checkInputConnectionProxy(View view) {
            mCalledCheckInputConnectionProxy = true;
            return super.checkInputConnectionProxy(view);
        }

        public boolean hasCalledCheckInputConnectionProxy() {
            return mCalledCheckInputConnectionProxy;
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            mCalledOnCreateInputConnection = true;
            return super.onCreateInputConnection(outAttrs);
        }

        public boolean hasCalledOnCreateInputConnection() {
            return mCalledOnCreateInputConnection;
        }

        @Override
        public boolean onCheckIsTextEditor() {
            mCalledOnCheckIsTextEditor = true;
            return super.onCheckIsTextEditor();
        }

        public boolean hasCalledOnCheckIsTextEditor() {
            return mCalledOnCheckIsTextEditor;
        }

        public void reset() {
            mCalledCheckInputConnectionProxy = false;
            mCalledOnCreateInputConnection = false;
            mCalledOnCheckIsTextEditor = false;
        }
    }

    private final static class MockViewParent extends View implements ViewParent {
        private boolean mHasClearChildFocus = false;
        private boolean mHasRequestLayout = false;
        private boolean mHasCreateContextMenu = false;
        private boolean mHasShowContextMenuForChild = false;
        private boolean mHasGetChildVisibleRect = false;
        private boolean mHasInvalidateChild = false;
        private boolean mHasOnCreateDrawableState = false;
        private boolean mHasChildDrawableStateChanged = false;
        private boolean mHasBroughtChildToFront = false;
        private Rect mTempRect;

        private final static int[] DEFAULT_PARENT_STATE_SET = new int[] { 789 };

        public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
                boolean immediate) {
            return false;
        }

        public MockViewParent(Context context) {
            super(context);
        }

        public void bringChildToFront(View child) {
            mHasBroughtChildToFront = true;
        }

        public boolean hasBroughtChildToFront() {
            return mHasBroughtChildToFront;
        }

        public void childDrawableStateChanged(View child) {
            mHasChildDrawableStateChanged = true;
        }

        public boolean hasChildDrawableStateChanged() {
            return mHasChildDrawableStateChanged;
        }

        @Override
        protected void dispatchSetPressed(boolean pressed) {
            super.dispatchSetPressed(pressed);
        }

        @Override
        protected void dispatchSetSelected(boolean selected) {
            super.dispatchSetSelected(selected);
        }

        public void clearChildFocus(View child) {
            mHasClearChildFocus = true;
        }

        public boolean hasClearChildFocus() {
            return mHasClearChildFocus;
        }

        public void createContextMenu(ContextMenu menu) {
            mHasCreateContextMenu = true;
        }

        public boolean hasCreateContextMenu() {
            return mHasCreateContextMenu;
        }

        public View focusSearch(View v, int direction) {
            return v;
        }

        public void focusableViewAvailable(View v) {

        }

        public boolean getChildVisibleRect(View child, Rect r, Point offset) {
            mHasGetChildVisibleRect = true;
            return false;
        }

        public boolean hasGetChildVisibleRect() {
            return mHasGetChildVisibleRect;
        }

        public void invalidateChild(View child, Rect r) {
            mTempRect = new Rect(r);
            mHasInvalidateChild = true;
        }

        public Rect getTempRect() {
            return mTempRect;
        }

        public boolean hasInvalidateChild() {
            return mHasInvalidateChild;
        }

        public ViewParent invalidateChildInParent(int[] location, Rect r) {
            return null;
        }

        public boolean isLayoutRequested() {
            return false;
        }

        public void recomputeViewAttributes(View child) {

        }

        public void requestChildFocus(View child, View focused) {

        }

        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }

        public void requestLayout() {
            mHasRequestLayout = true;
        }

        public boolean hasRequestLayout() {
            return mHasRequestLayout;
        }

        public void requestTransparentRegion(View child) {

        }

        public boolean showContextMenuForChild(View originalView) {
            mHasShowContextMenuForChild = true;
            return false;
        }

        public ActionMode startActionModeForChild(View originalView,
                ActionMode.Callback callback) {
            return null;
        }

        public boolean hasShowContextMenuForChild() {
            return mHasShowContextMenuForChild;
        }

        @Override
        protected int[] onCreateDrawableState(int extraSpace) {
            mHasOnCreateDrawableState = true;
            return DEFAULT_PARENT_STATE_SET;
        }

        public boolean requestSendAccessibilityEvent(View child, AccessibilityEvent event) {
            return false;
        }

        public static int[] getDefaultParentStateSet() {
            return DEFAULT_PARENT_STATE_SET;
        }

        public boolean hasOnCreateDrawableState() {
            return mHasOnCreateDrawableState;
        }

        public void reset() {
            mHasClearChildFocus = false;
            mHasRequestLayout = false;
            mHasCreateContextMenu = false;
            mHasShowContextMenuForChild = false;
            mHasGetChildVisibleRect = false;
            mHasInvalidateChild = false;
            mHasOnCreateDrawableState = false;
            mHasChildDrawableStateChanged = false;
            mHasBroughtChildToFront = false;
        }

        public void childOverlayStateChanged(View child) {

        }

        @Override
        public void childHasTransientStateChanged(View child, boolean hasTransientState) {

        }

        @Override
        public ViewParent getParentForAccessibility() {
            return null;
        }

        @Override
        public void notifySubtreeAccessibilityStateChanged(View child,
            View source, int changeType) {

        }
    }

    private final class OnCreateContextMenuListenerImpl implements OnCreateContextMenuListener {
        private boolean mHasOnCreateContextMenu = false;

        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            mHasOnCreateContextMenu = true;
        }

        public boolean hasOnCreateContextMenu() {
            return mHasOnCreateContextMenu;
        }

        public void reset() {
            mHasOnCreateContextMenu = false;
        }
    }

    private static class MockViewGroupParent extends ViewGroup implements ViewParent {
        private boolean mHasRequestChildRectangleOnScreen = false;

        public MockViewGroupParent(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {

        }

        @Override
        public boolean requestChildRectangleOnScreen(View child,
                Rect rectangle, boolean immediate) {
            mHasRequestChildRectangleOnScreen = true;
            return super.requestChildRectangleOnScreen(child, rectangle, immediate);
        }

        public boolean hasRequestChildRectangleOnScreen() {
            return mHasRequestChildRectangleOnScreen;
        }

        @Override
        protected void detachViewFromParent(View child) {
            super.detachViewFromParent(child);
        }

        public void reset() {
            mHasRequestChildRectangleOnScreen = false;
        }
    }

    private static final class OnClickListenerImpl implements OnClickListener {
        private boolean mHasOnClick = false;

        public void onClick(View v) {
            mHasOnClick = true;
        }

        public boolean hasOnClick() {
            return mHasOnClick;
        }

        public void reset() {
            mHasOnClick = false;
        }
    }

    private static final class OnLongClickListenerImpl implements OnLongClickListener {
        private boolean mHasOnLongClick = false;

        public boolean hasOnLongClick() {
            return mHasOnLongClick;
        }

        public void reset() {
            mHasOnLongClick = false;
        }

        public boolean onLongClick(View v) {
            mHasOnLongClick = true;
            return true;
        }
    }

    private static final class OnFocusChangeListenerImpl implements OnFocusChangeListener {
        private boolean mHasOnFocusChange = false;

        public void onFocusChange(View v, boolean hasFocus) {
            mHasOnFocusChange = true;
        }

        public boolean hasOnFocusChange() {
            return mHasOnFocusChange;
        }

        public void reset() {
            mHasOnFocusChange = false;
        }
    }

    private static final class OnKeyListenerImpl implements OnKeyListener {
        private boolean mHasOnKey = false;

        public boolean onKey(View v, int keyCode, KeyEvent event) {
            mHasOnKey = true;
            return true;
        }

        public void reset() {
            mHasOnKey = false;
        }

        public boolean hasOnKey() {
            return mHasOnKey;
        }
    }

    private static final class OnTouchListenerImpl implements OnTouchListener {
        private boolean mHasOnTouch = false;

        public boolean onTouch(View v, MotionEvent event) {
            mHasOnTouch = true;
            return true;
        }

        public void reset() {
            mHasOnTouch = false;
        }

        public boolean hasOnTouch() {
            return mHasOnTouch;
        }
    }

    private static final class MockTouchDelegate extends TouchDelegate {
        public MockTouchDelegate(Rect bounds, View delegateView) {
            super(bounds, delegateView);
        }

        private boolean mCalledOnTouchEvent = false;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            mCalledOnTouchEvent = true;
            return super.onTouchEvent(event);
        }

        public boolean hasCalledOnTouchEvent() {
            return mCalledOnTouchEvent;
        }

        public void reset() {
            mCalledOnTouchEvent = false;
        }
    };

    private static final class ViewData {
        public int childCount;
        public String tag;
        public View firstChild;
    }

    private static final class MockRunnable implements Runnable {
        public boolean hasRun = false;

        public void run() {
            hasRun = true;
        }
    }
}

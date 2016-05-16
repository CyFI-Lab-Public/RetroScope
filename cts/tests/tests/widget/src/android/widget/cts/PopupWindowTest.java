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


import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.PopupWindow.OnDismissListener;

public class PopupWindowTest extends
        ActivityInstrumentationTestCase2<MockPopupWindowStubActivity> {
    private Instrumentation mInstrumentation;
    private Activity mActivity;
    /** The popup window. */
    private PopupWindow mPopupWindow;

    /**
     * Instantiates a new popup window test.
     */
    public PopupWindowTest() {
        super("com.android.cts.stub", MockPopupWindowStubActivity.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.test.ActivityInstrumentationTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
    }

    public void testConstructor() {
        new PopupWindow(mActivity);

        new PopupWindow(mActivity, null);

        new PopupWindow(mActivity, null, com.android.internal.R.attr.popupWindowStyle);

        mPopupWindow = new PopupWindow();
        assertEquals(0, mPopupWindow.getWidth());
        assertEquals(0, mPopupWindow.getHeight());

        mPopupWindow = new PopupWindow(50, 50);
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());

        mPopupWindow = new PopupWindow(-1, -1);
        assertEquals(-1, mPopupWindow.getWidth());
        assertEquals(-1, mPopupWindow.getHeight());

        TextView contentView = new TextView(mActivity);
        mPopupWindow = new PopupWindow(contentView);
        assertSame(contentView, mPopupWindow.getContentView());

        mPopupWindow = new PopupWindow(contentView, 0, 0);
        assertEquals(0, mPopupWindow.getWidth());
        assertEquals(0, mPopupWindow.getHeight());
        assertSame(contentView, mPopupWindow.getContentView());

        mPopupWindow = new PopupWindow(contentView, 50, 50);
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());
        assertSame(contentView, mPopupWindow.getContentView());

        mPopupWindow = new PopupWindow(contentView, -1, -1);
        assertEquals(-1, mPopupWindow.getWidth());
        assertEquals(-1, mPopupWindow.getHeight());
        assertSame(contentView, mPopupWindow.getContentView());

        mPopupWindow = new PopupWindow(contentView, 0, 0, true);
        assertEquals(0, mPopupWindow.getWidth());
        assertEquals(0, mPopupWindow.getHeight());
        assertSame(contentView, mPopupWindow.getContentView());
        assertTrue(mPopupWindow.isFocusable());

        mPopupWindow = new PopupWindow(contentView, 50, 50, false);
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());
        assertSame(contentView, mPopupWindow.getContentView());
        assertFalse(mPopupWindow.isFocusable());

        mPopupWindow = new PopupWindow(contentView, -1, -1, true);
        assertEquals(-1, mPopupWindow.getWidth());
        assertEquals(-1, mPopupWindow.getHeight());
        assertSame(contentView, mPopupWindow.getContentView());
        assertTrue(mPopupWindow.isFocusable());
    }

    public void testAccessBackground() {
        mPopupWindow = new PopupWindow(mActivity);

        Drawable drawable = new ColorDrawable();
        mPopupWindow.setBackgroundDrawable(drawable);
        assertSame(drawable, mPopupWindow.getBackground());

        mPopupWindow.setBackgroundDrawable(null);
        assertNull(mPopupWindow.getBackground());
    }

    public void testAccessAnimationStyle() {
        mPopupWindow = new PopupWindow(mActivity);
        // default is -1
        assertEquals(-1, mPopupWindow.getAnimationStyle());

        mPopupWindow.setAnimationStyle(com.android.internal.R.style.Animation_Toast);
        assertEquals(com.android.internal.R.style.Animation_Toast,
                mPopupWindow.getAnimationStyle());

        mPopupWindow.setAnimationStyle(com.android.internal.R.style.Animation_DropDownDown);
        assertEquals(com.android.internal.R.style.Animation_DropDownDown,
                mPopupWindow.getAnimationStyle());

        // abnormal values
        mPopupWindow.setAnimationStyle(-100);
        assertEquals(-100, mPopupWindow.getAnimationStyle());
    }

    public void testAccessContentView() {
        mPopupWindow = new PopupWindow(mActivity);
        assertNull(mPopupWindow.getContentView());

        View view = new TextView(mActivity);
        mPopupWindow.setContentView(view);
        assertSame(view, mPopupWindow.getContentView());

        mPopupWindow.setContentView(null);
        assertNull(mPopupWindow.getContentView());

        // can not set the content if the old content is shown
        mPopupWindow.setContentView(view);
        assertFalse(mPopupWindow.isShowing());
        showPopup();
        ImageView img = new ImageView(mActivity);
        assertTrue(mPopupWindow.isShowing());
        mPopupWindow.setContentView(img);
        assertSame(view, mPopupWindow.getContentView());
        dismissPopup();
    }

    public void testAccessFocusable() {
        mPopupWindow = new PopupWindow(mActivity);
        assertFalse(mPopupWindow.isFocusable());

        mPopupWindow.setFocusable(true);
        assertTrue(mPopupWindow.isFocusable());

        mPopupWindow.setFocusable(false);
        assertFalse(mPopupWindow.isFocusable());
    }

    public void testAccessHeight() {
        mPopupWindow = new PopupWindow(mActivity);
        // default is 0
        assertEquals(0, mPopupWindow.getHeight());

        int height = getDisplay().getHeight() / 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        height = getDisplay().getHeight();
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        mPopupWindow.setHeight(0);
        assertEquals(0, mPopupWindow.getHeight());

        height = getDisplay().getHeight() * 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        height = -getDisplay().getHeight() / 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());
    }

    /**
     * Gets the display.
     *
     * @return the display
     */
    private Display getDisplay() {
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
    }

    public void testAccessWidth() {
        mPopupWindow = new PopupWindow(mActivity);
        assertEquals(0, mPopupWindow.getWidth());

        int width = getDisplay().getWidth() / 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        width = getDisplay().getWidth();
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        mPopupWindow.setWidth(0);
        assertEquals(0, mPopupWindow.getWidth());

        width = getDisplay().getWidth() * 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        width = - getDisplay().getWidth() / 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());
    }

    public void testShowAsDropDown() {
        int[] anchorXY = new int[2];
        int[] viewOnScreenXY = new int[2];
        int[] viewInWindowXY = new int[2];

        mPopupWindow = createPopupWindow(createPopupContent());
        final View upperAnchor = mActivity.findViewById(R.id.anchor_upper);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.showAsDropDown(upperAnchor);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());

        mPopupWindow.getContentView().getLocationOnScreen(viewOnScreenXY);
        upperAnchor.getLocationOnScreen(anchorXY);
        mPopupWindow.getContentView().getLocationInWindow(viewInWindowXY);
        assertEquals(anchorXY[0] + viewInWindowXY[0], viewOnScreenXY[0]);
        assertEquals(anchorXY[1] + viewInWindowXY[1] + upperAnchor.getHeight(), viewOnScreenXY[1]);

        dismissPopup();
    }

    public void testShowAtLocation() {
        int[] viewInWindowXY = new int[2];
        int[] viewOnScreenXY = new int[2];

        mPopupWindow = createPopupWindow(createPopupContent());
        final View upperAnchor = mActivity.findViewById(R.id.anchor_upper);

        final int xOff = 10;
        final int yOff = 21;
        assertFalse(mPopupWindow.isShowing());
        mPopupWindow.getContentView().getLocationInWindow(viewInWindowXY);
        assertEquals(0, viewInWindowXY[0]);
        assertEquals(0, viewInWindowXY[1]);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.showAtLocation(upperAnchor, Gravity.NO_GRAVITY, xOff, yOff);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());
        mPopupWindow.getContentView().getLocationInWindow(viewInWindowXY);
        mPopupWindow.getContentView().getLocationOnScreen(viewOnScreenXY);
        assertTrue(viewInWindowXY[0] >= 0);
        assertTrue(viewInWindowXY[1] >= 0);
        assertEquals(viewInWindowXY[0] + xOff, viewOnScreenXY[0]);
        assertEquals(viewInWindowXY[1] + yOff, viewOnScreenXY[1]);

        dismissPopup();
    }

    public void testShowAsDropDownWithOffsets() {
        int[] anchorXY = new int[2];
        int[] viewOnScreenXY = new int[2];
        int[] viewInWindowXY = new int[2];

        mPopupWindow = createPopupWindow(createPopupContent());
        final View upperAnchor = mActivity.findViewById(R.id.anchor_upper);
        upperAnchor.getLocationOnScreen(anchorXY);
        int height = upperAnchor.getHeight();

        final int xOff = 11;
        final int yOff = 12;

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.showAsDropDown(upperAnchor, xOff, yOff);
            }
        });
        mInstrumentation.waitForIdleSync();

        mPopupWindow.getContentView().getLocationOnScreen(viewOnScreenXY);
        mPopupWindow.getContentView().getLocationInWindow(viewInWindowXY);
        assertEquals(anchorXY[0] + xOff + viewInWindowXY[0], viewOnScreenXY[0]);
        assertEquals(anchorXY[1] + height + yOff + viewInWindowXY[1], viewOnScreenXY[1]);

        dismissPopup();
    }

    public void testGetMaxAvailableHeight() {
        mPopupWindow = createPopupWindow(createPopupContent());

        View anchorView = mActivity.findViewById(R.id.anchor_upper);
        int avaliable = getDisplay().getHeight() - anchorView.getHeight();
        int maxAvailableHeight = mPopupWindow.getMaxAvailableHeight(anchorView);
        assertTrue(maxAvailableHeight > 0);
        assertTrue(maxAvailableHeight <= avaliable);
        int maxAvailableHeightWithOffset = mPopupWindow.getMaxAvailableHeight(anchorView, 2);
        assertEquals(maxAvailableHeight - 2, maxAvailableHeightWithOffset);
        maxAvailableHeightWithOffset =
                mPopupWindow.getMaxAvailableHeight(anchorView, maxAvailableHeight);
        assertTrue(maxAvailableHeightWithOffset > 0);
        assertTrue(maxAvailableHeightWithOffset <= avaliable);
        maxAvailableHeightWithOffset =
                mPopupWindow.getMaxAvailableHeight(anchorView, maxAvailableHeight / 2 - 1);
        assertTrue(maxAvailableHeightWithOffset > 0);
        assertTrue(maxAvailableHeightWithOffset <= avaliable);
        maxAvailableHeightWithOffset = mPopupWindow.getMaxAvailableHeight(anchorView, -1);
        assertTrue(maxAvailableHeightWithOffset > 0);
        assertTrue(maxAvailableHeightWithOffset <= avaliable);

        anchorView = mActivity.findViewById(R.id.anchor_lower);
        avaliable = getDisplay().getHeight() - anchorView.getHeight();
        maxAvailableHeight = mPopupWindow.getMaxAvailableHeight(anchorView);
        assertTrue(maxAvailableHeight > 0);
        assertTrue(maxAvailableHeight <= avaliable);

        anchorView = mActivity.findViewById(R.id.anchor_middle_left);
        avaliable = getDisplay().getHeight() - anchorView.getHeight()
                - mActivity.findViewById(R.id.anchor_upper).getHeight();
        maxAvailableHeight = mPopupWindow.getMaxAvailableHeight(anchorView);
        assertTrue(maxAvailableHeight > 0);
        assertTrue(maxAvailableHeight <= avaliable);
    }

    public void testDismiss() {
        mPopupWindow = createPopupWindow(createPopupContent());
        assertFalse(mPopupWindow.isShowing());
        View anchorView = mActivity.findViewById(R.id.anchor_upper);
        mPopupWindow.showAsDropDown(anchorView);

        mPopupWindow.dismiss();
        assertFalse(mPopupWindow.isShowing());

        mPopupWindow.dismiss();
        assertFalse(mPopupWindow.isShowing());
    }

    public void testSetOnDismissListener() {
        mPopupWindow = new PopupWindow(new TextView(mActivity));
        mPopupWindow.setOnDismissListener(null);

        MockOnDismissListener onDismissListener = new MockOnDismissListener();
        mPopupWindow.setOnDismissListener(onDismissListener);
        showPopup();
        dismissPopup();
        assertEquals(1, onDismissListener.getOnDismissCalledCount());

        showPopup();
        dismissPopup();
        assertEquals(2, onDismissListener.getOnDismissCalledCount());

        mPopupWindow.setOnDismissListener(null);
        showPopup();
        dismissPopup();
        assertEquals(2, onDismissListener.getOnDismissCalledCount());
    }

    public void testUpdate() {
        mPopupWindow = createPopupWindow(createPopupContent());
        mPopupWindow.setBackgroundDrawable(null);
        showPopup();

        mPopupWindow.setIgnoreCheekPress();
        mPopupWindow.setFocusable(true);
        mPopupWindow.setTouchable(false);
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.setOutsideTouchable(true);

        WindowManager.LayoutParams p = (WindowManager.LayoutParams)
                mPopupWindow.getContentView().getLayoutParams();

        assertEquals(0, WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES & p.flags);
        assertEquals(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE & p.flags);
        assertEquals(0, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE & p.flags);
        assertEquals(0, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH & p.flags);
        assertEquals(0, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS & p.flags);
        assertEquals(0, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM & p.flags);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.update();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES,
                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES & p.flags);
        assertEquals(0, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE & p.flags);
        assertEquals(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE & p.flags);
        assertEquals(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH & p.flags);
        assertEquals(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS & p.flags);
        assertEquals(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM & p.flags);
    }

    public void testUpdatePositionAndDimension() {
        int[] fstXY = new int[2];
        int[] sndXY = new int[2];
        int[] viewInWindowXY = new int[2];

        mPopupWindow = createPopupWindow(createPopupContent());
        // Do not update if it is not shown
        assertFalse(mPopupWindow.isShowing());
        assertEquals(100, mPopupWindow.getWidth());
        assertEquals(100, mPopupWindow.getHeight());

        showPopup();
        mPopupWindow.getContentView().getLocationInWindow(viewInWindowXY);

        // update if it is not shown
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.update(20, 50, 50, 50);
            }
        });

        mInstrumentation.waitForIdleSync();
        assertTrue(mPopupWindow.isShowing());
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());

        mPopupWindow.getContentView().getLocationOnScreen(fstXY);
        assertEquals(viewInWindowXY[0] + 20, fstXY[0]);
        assertEquals(viewInWindowXY[1] + 50, fstXY[1]);

        // ignore if width or height is -1
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.update(4, 0, -1, -1, true);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());

        mPopupWindow.getContentView().getLocationOnScreen(sndXY);
        assertEquals(viewInWindowXY[0] + 4, sndXY[0]);
        assertEquals(viewInWindowXY[1], sndXY[1]);

        dismissPopup();
    }

    public void testUpdateDimensionAndAlignAnchorView() {
        mPopupWindow = createPopupWindow(createPopupContent());

        View anchorView = mActivity.findViewById(R.id.anchor_upper);
        mPopupWindow.update(anchorView, 50, 50);
        // Do not update if it is not shown
        assertFalse(mPopupWindow.isShowing());
        assertEquals(100, mPopupWindow.getWidth());
        assertEquals(100, mPopupWindow.getHeight());

        mPopupWindow.showAsDropDown(anchorView);
        mInstrumentation.waitForIdleSync();
        // update if it is shown
        mPopupWindow.update(anchorView, 50, 50);
        assertTrue(mPopupWindow.isShowing());
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());

        // ignore if width or height is -1
        mPopupWindow.update(anchorView, -1, -1);
        assertTrue(mPopupWindow.isShowing());
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());

        mPopupWindow.dismiss();
    }

    public void testUpdateDimensionAndAlignAnchorViewWithOffsets() {
        int[] anchorXY = new int[2];
        int[] viewInWindowOff = new int[2];
        int[] viewXY = new int[2];

        mPopupWindow = createPopupWindow(createPopupContent());
        final View anchorView = mActivity.findViewById(R.id.anchor_upper);
        // Do not update if it is not shown
        assertFalse(mPopupWindow.isShowing());
        assertEquals(100, mPopupWindow.getWidth());
        assertEquals(100, mPopupWindow.getHeight());

        showPopup();
        anchorView.getLocationOnScreen(anchorXY);
        mPopupWindow.getContentView().getLocationInWindow(viewInWindowOff);

        // update if it is not shown
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.update(anchorView, 20, 50, 50, 50);
            }
        });

        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());

        mPopupWindow.getContentView().getLocationOnScreen(viewXY);
        // the position should be changed
        assertEquals(anchorXY[0] + 20 + viewInWindowOff[0], viewXY[0]);
        assertEquals(anchorXY[1] + anchorView.getHeight() + 50 + viewInWindowOff[1], viewXY[1]);

        // ignore width and height but change location
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.update(anchorView, 10, 50, -1, -1);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());
        assertEquals(50, mPopupWindow.getWidth());
        assertEquals(50, mPopupWindow.getHeight());

        mPopupWindow.getContentView().getLocationOnScreen(viewXY);
        // the position should be changed
        assertEquals(anchorXY[0] + 10 + viewInWindowOff[0], viewXY[0]);
        assertEquals(anchorXY[1] + anchorView.getHeight() + 50 + viewInWindowOff[1], viewXY[1]);

        final View anthoterView = mActivity.findViewById(R.id.anchor_middle_right);
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.update(anthoterView, 0, 0, 60, 60);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mPopupWindow.isShowing());
        assertEquals(60, mPopupWindow.getWidth());
        assertEquals(60, mPopupWindow.getHeight());

        int[] newXY = new int[2];
        anthoterView.getLocationOnScreen(newXY);
        mPopupWindow.getContentView().getLocationOnScreen(viewXY);
        // the position should be changed
        assertEquals(newXY[0] + viewInWindowOff[0], viewXY[0]);
        assertEquals(newXY[1] + anthoterView.getHeight() + viewInWindowOff[1], viewXY[1]);

        dismissPopup();
    }

    public void testAccessInputMethodMode() {
        mPopupWindow = new PopupWindow(mActivity);
        assertEquals(0, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_FROM_FOCUSABLE);
        assertEquals(PopupWindow.INPUT_METHOD_FROM_FOCUSABLE, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        assertEquals(PopupWindow.INPUT_METHOD_NEEDED, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        assertEquals(PopupWindow.INPUT_METHOD_NOT_NEEDED, mPopupWindow.getInputMethodMode());

        mPopupWindow.setInputMethodMode(-1);
        assertEquals(-1, mPopupWindow.getInputMethodMode());
    }

    public void testAccessClippingEnabled() {
        mPopupWindow = new PopupWindow(mActivity);
        assertTrue(mPopupWindow.isClippingEnabled());

        mPopupWindow.setClippingEnabled(false);
        assertFalse(mPopupWindow.isClippingEnabled());
    }

    public void testAccessOutsideTouchable() {
        mPopupWindow = new PopupWindow(mActivity);
        assertFalse(mPopupWindow.isOutsideTouchable());

        mPopupWindow.setOutsideTouchable(true);
        assertTrue(mPopupWindow.isOutsideTouchable());
    }

    public void testAccessTouchable() {
        mPopupWindow = new PopupWindow(mActivity);
        assertTrue(mPopupWindow.isTouchable());

        mPopupWindow.setTouchable(false);
        assertFalse(mPopupWindow.isTouchable());
    }

    public void testIsAboveAnchor() {
        mPopupWindow = createPopupWindow(createPopupContent());
        final View upperAnchor = mActivity.findViewById(R.id.anchor_upper);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.showAsDropDown(upperAnchor);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertFalse(mPopupWindow.isAboveAnchor());
        dismissPopup();

        mPopupWindow = createPopupWindow(createPopupContent());
        final View lowerAnchor = mActivity.findViewById(R.id.anchor_lower);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.showAsDropDown(lowerAnchor, 0, 0);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(mPopupWindow.isAboveAnchor());
        dismissPopup();
    }

    public void testSetTouchInterceptor() {
        mPopupWindow = new PopupWindow(new TextView(mActivity));

        MockOnTouchListener onTouchListener = new MockOnTouchListener();
        mPopupWindow.setTouchInterceptor(onTouchListener);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setOutsideTouchable(true);
        Drawable drawable = new ColorDrawable();
        mPopupWindow.setBackgroundDrawable(drawable);
        showPopup();

        int[] xy = new int[2];
        mPopupWindow.getContentView().getLocationOnScreen(xy);
        final int viewWidth = mPopupWindow.getContentView().getWidth();
        final int viewHeight = mPopupWindow.getContentView().getHeight();
        final float x = xy[0] + (viewWidth / 2.0f);
        float y = xy[1] + (viewHeight / 2.0f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        assertEquals(1, onTouchListener.getOnTouchCalledCount());

        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        assertEquals(2, onTouchListener.getOnTouchCalledCount());

        mPopupWindow.setTouchInterceptor(null);
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        getInstrumentation().sendPointerSync(event);
        assertEquals(2, onTouchListener.getOnTouchCalledCount());
    }

    public void testSetWindowLayoutMode() {
        mPopupWindow = new PopupWindow(new TextView(mActivity));
        showPopup();

        ViewGroup.LayoutParams p = mPopupWindow.getContentView().getLayoutParams();
        assertEquals(0, p.width);
        assertEquals(0, p.height);

        mPopupWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mPopupWindow.update(20, 50, 50, 50);
            }
        });

        assertEquals(LayoutParams.WRAP_CONTENT, p.width);
        assertEquals(LayoutParams.MATCH_PARENT, p.height);
    }

    /**
     * The listener interface for receiving OnDismiss events. The class that is
     * interested in processing a OnDismiss event implements this interface, and
     * the object created with that class is registered with a component using
     * the component's <code>setOnDismissListener<code> method. When
     * the OnDismiss event occurs, that object's appropriate
     * method is invoked.
     */
    private static class MockOnDismissListener implements OnDismissListener {

        /** The Ondismiss called count. */
        private int mOnDismissCalledCount;

        /**
         * Gets the onDismiss() called count.
         *
         * @return the on dismiss called count
         */
        public int getOnDismissCalledCount() {
            return mOnDismissCalledCount;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.PopupWindow.OnDismissListener#onDismiss()
         */
        public void onDismiss() {
            mOnDismissCalledCount++;
        }

    }

    /**
     * The listener interface for receiving touch events.
     */
    private static class MockOnTouchListener implements OnTouchListener {

        /** The onTouch called count. */
        private int mOnTouchCalledCount;

        /**
         * Gets the onTouch() called count.
         *
         * @return the onTouch() called count
         */
        public int getOnTouchCalledCount() {
            return mOnTouchCalledCount;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.PopupWindow.OnTouchListener#onDismiss()
         */
        public boolean onTouch(View v, MotionEvent event) {
            mOnTouchCalledCount++;
            return true;
        }
    }

    private View createPopupContent() {
        TextView popupView = new TextView(mActivity);
        popupView.setText("Popup");
        popupView.setLayoutParams(new ViewGroup.LayoutParams(50, 50));
        popupView.setBackgroundColor(Color.WHITE);

        return popupView;
    }

    private PopupWindow createPopupWindow() {
        PopupWindow window = new PopupWindow(mActivity);
        window.setWidth(100);
        window.setHeight(100);
        return window;
    }

    private PopupWindow createPopupWindow(View content) {
        PopupWindow window = createPopupWindow();
        window.setContentView(content);
        return window;
    }

    /**
     * Show PopupWindow.
     */
    // FIXME: logcat info complains that there is window leakage due to that mPopupWindow is not
    // clean up. Need to fix it.
    private void showPopup() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                if (mPopupWindow == null || mPopupWindow.isShowing()) {
                    return;
                }
                View anchor = mActivity.findViewById(R.id.anchor_upper);
                mPopupWindow.showAsDropDown(anchor);
                assertTrue(mPopupWindow.isShowing());
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    /**
     * Dismiss PopupWindow.
     */
    private void dismissPopup() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                if (mPopupWindow == null || !mPopupWindow.isShowing())
                    return;
                mPopupWindow.dismiss();
            }
        });
        mInstrumentation.waitForIdleSync();
    }
}

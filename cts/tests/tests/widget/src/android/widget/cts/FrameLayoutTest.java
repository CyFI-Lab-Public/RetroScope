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
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

public class FrameLayoutTest extends ActivityInstrumentationTestCase2<FrameLayoutStubActivity> {
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private FrameLayout mFrameLayout;

    public FrameLayoutTest() {
        super("com.android.cts.stub", FrameLayoutStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mFrameLayout = (FrameLayout) mActivity.findViewById(R.id.framelayout);
        assertNotNull(mActivity);
        assertNotNull(mInstrumentation);
        assertNotNull(mFrameLayout);
    }

    public void testConstructor() throws XmlPullParserException, IOException {
        AttributeSet attrs = getAttributeSet();

        new FrameLayout(mActivity);
        new FrameLayout(mActivity, attrs);
        new FrameLayout(mActivity, attrs, 0);
    }

    public void testSetForegroundGravity() {
        final BitmapDrawable foreground
                = (BitmapDrawable) mActivity.getResources().getDrawable(R.drawable.size_48x48);
        compareScaledPixels(48, foreground.getIntrinsicHeight());
        compareScaledPixels(48, foreground.getIntrinsicWidth());
        assertTrue(mFrameLayout.getHeight() > foreground.getIntrinsicHeight());
        assertTrue(mFrameLayout.getWidth() > foreground.getIntrinsicWidth());
        assertNull(mFrameLayout.getForeground());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mFrameLayout.setForeground(foreground);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertSame(foreground, mFrameLayout.getForeground());
        // check the default gravity FILL, it completely fills its container
        assertTrue(foreground.isVisible());
        final Rect rect = foreground.getBounds();
        // foreground has been stretched
        new PollingCheck() {
            @Override
            protected boolean check() {
                return mFrameLayout.getHeight() == rect.bottom - rect.top;
            }
        }.run();
        assertEquals(mFrameLayout.getWidth(), rect.right - rect.left);

        // should get a new foreground again, because former foreground has been stretched
        final BitmapDrawable newForeground
                = (BitmapDrawable) mActivity.getResources().getDrawable(R.drawable.size_48x48);
        compareScaledPixels(48, newForeground.getIntrinsicHeight());
        compareScaledPixels(48, newForeground.getIntrinsicWidth());
        assertTrue(mFrameLayout.getHeight() > newForeground.getIntrinsicHeight());
        assertTrue(mFrameLayout.getWidth() > foreground.getIntrinsicWidth());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mFrameLayout.setForeground(newForeground);
                mFrameLayout.setForegroundGravity(Gravity.CENTER);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertSame(newForeground, mFrameLayout.getForeground());
        assertTrue(newForeground.isVisible());
        Rect rect2 = newForeground.getBounds();
        // not changing its size
        assertEquals(foreground.getIntrinsicHeight(), rect2.bottom - rect2.top);
        assertEquals(foreground.getIntrinsicWidth(), rect2.right - rect2.left);
        assertCenterAligned(mFrameLayout, newForeground);
    }

    public void testGatherTransparentRegion() {
        final LinearLayout container
                = (LinearLayout) mActivity.findViewById(R.id.framelayout_container);
        final Drawable foreground = mActivity.getResources().getDrawable(R.drawable.size_48x48);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mFrameLayout.setForeground(foreground);
                mFrameLayout.setForegroundGravity(Gravity.CENTER);
            }
        });
        mInstrumentation.waitForIdleSync();
        Region region = new Region(foreground.getBounds());
        assertTrue(mFrameLayout.gatherTransparentRegion(region));

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                container.requestTransparentRegion(mFrameLayout);
            }
        });
        mInstrumentation.waitForIdleSync();
        region = new Region(foreground.getBounds());
        assertTrue(mFrameLayout.gatherTransparentRegion(region));
    }

    public void testAccessMeasureAllChildren() {
        final FrameLayout frameLayout
                = (FrameLayout) mActivity.findViewById(R.id.framelayout_measureall);
        assertFalse(frameLayout.getConsiderGoneChildrenWhenMeasuring());

        // text view and button are VISIBLE, they should be measured
        final TextView textView = (TextView) frameLayout.findViewById(R.id.framelayout_textview);
        compareScaledPixels(30, textView.getMeasuredHeight());
        compareScaledPixels(100, textView.getMeasuredWidth());
        assertEquals(textView.getMeasuredHeight(), frameLayout.getMeasuredHeight());
        assertEquals(textView.getMeasuredWidth(), frameLayout.getMeasuredWidth());

        // measureAll is false and text view is GONE, text view will NOT be measured
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                textView.setVisibility(View.GONE);
                frameLayout.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertFalse(frameLayout.getConsiderGoneChildrenWhenMeasuring());
        Button button = (Button) frameLayout.findViewById(R.id.framelayout_button);
        compareScaledPixels(15, button.getMeasuredHeight());
        compareScaledPixels(50, button.getMeasuredWidth());
        assertEquals(button.getMeasuredHeight(), frameLayout.getMeasuredHeight());
        assertEquals(button.getMeasuredWidth(), frameLayout.getMeasuredWidth());

        // measureAll is true and text view is GONE, text view will be measured
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                frameLayout.setMeasureAllChildren(true);
                frameLayout.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(frameLayout.getConsiderGoneChildrenWhenMeasuring());
        assertEquals(textView.getMeasuredHeight(), frameLayout.getMeasuredHeight());
        assertEquals(textView.getMeasuredWidth(), frameLayout.getMeasuredWidth());
    }

    /**
     * Helper method to compare expected pixels, scaled to device density, with actual
     */
    private void compareScaledPixels(int expected, int actual) {
        WidgetTestUtils.assertScaledPixels(expected, actual, getActivity());
    }

    public void testGenerateLayoutParams1() {
        MyFrameLayout myFrameLayout = new MyFrameLayout(mActivity);
        ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        ViewGroup.LayoutParams params = myFrameLayout.generateLayoutParams(p);
        assertNotNull(params);
        assertTrue(params instanceof FrameLayout.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, params.height);
    }

    public void testGenerateLayoutParams2() throws XmlPullParserException, IOException {
        AttributeSet attrs = getAttributeSet();

        FrameLayout.LayoutParams params = mFrameLayout.generateLayoutParams(attrs);
        assertNotNull(params);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.width);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.height);
        assertEquals(Gravity.BOTTOM, params.gravity);
    }

    public void testCheckLayoutParams() {
        MyFrameLayout myFrameLayout = new MyFrameLayout(mActivity);
        assertFalse(myFrameLayout.checkLayoutParams(null));

        int width = 120;
        int height = 80;
        ViewGroup.LayoutParams params1 = new ViewGroup.LayoutParams(width, height);
        assertFalse(myFrameLayout.checkLayoutParams(params1));

        FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(width, height);
        assertTrue(myFrameLayout.checkLayoutParams(params2));
    }

    public void testDrawableStateChanged() {
        // drawableStateChanged() is implementation details, do NOT test
    }

    public void testGenerateDefaultLayoutParams() {
        MyFrameLayout frameLayout = new MyFrameLayout(mActivity);
        FrameLayout.LayoutParams params = frameLayout.generateDefaultLayoutParams();

        assertNotNull(params);
        assertEquals(LayoutParams.MATCH_PARENT, params.width);
        assertEquals(LayoutParams.MATCH_PARENT, params.height);
    }

    public void testOnLayout() {
        // onLayout() is implementation details, do NOT test
    }

    public void testOnMeasure() {
        // onMeasure() is implementation details, do NOT test
    }

    public void testOnSizeChanged() {
        // onSizeChanged() is implementation details, do NOT test
    }

    public void testVerifyDrawable() {
        MyFrameLayout myFrameLayout = new MyFrameLayout(mActivity);

        Drawable drawable = mActivity.getResources().getDrawable(R.drawable.scenery);
        myFrameLayout.setForeground(drawable);
        assertTrue(myFrameLayout.verifyDrawable(drawable));

        drawable = mActivity.getResources().getDrawable(R.drawable.black);
        assertFalse(myFrameLayout.verifyDrawable(drawable));

        assertTrue(myFrameLayout.verifyDrawable(null));
    }

    private static void assertCenterAligned(View container, Drawable drawable) {
        Rect rect = drawable.getBounds();
        int leftDelta = rect.left - container.getLeft();
        int rightDelta = container.getRight() - rect.right;
        int topDelta = rect.top - container.getTop();
        int bottomDelta = container.getBottom() - rect.bottom;

        assertTrue(Math.abs(leftDelta - rightDelta) <= 1);
        assertTrue(Math.abs(topDelta - bottomDelta) <= 1);
    }

    private AttributeSet getAttributeSet() throws XmlPullParserException, IOException {
        XmlPullParser parser = mActivity.getResources().getLayout(R.layout.framelayout_layout);
        WidgetTestUtils.beginDocument(parser, "LinearLayout");
        return Xml.asAttributeSet(parser);
    }

    private static class MyFrameLayout extends FrameLayout {
        public MyFrameLayout(Context context) {
            super(context);
        }

        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return super.checkLayoutParams(p);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
        }

        @Override
        protected LayoutParams generateDefaultLayoutParams() {
            return super.generateDefaultLayoutParams();
        }

        @Override
        protected boolean verifyDrawable(Drawable who) {
            return super.verifyDrawable(who);
        }

        @Override
        protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return super.generateLayoutParams(p);
        }
    }
}

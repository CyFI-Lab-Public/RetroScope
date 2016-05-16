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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.Arrays;

public class CheckedTextViewTest extends
        ActivityInstrumentationTestCase2<CheckedTextViewStubActivity> {
    private Resources mResources;
    private Activity mActivity;
    private Instrumentation mInstrumentation;

    public CheckedTextViewTest() {
        super("com.android.cts.stub", CheckedTextViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mResources = mActivity.getResources();
    }

    public void testConstructor() {
        new MockCheckedTextView(mActivity, null, 0);
        new MockCheckedTextView(mActivity, null);
        new MockCheckedTextView(mActivity);

        try {
            new MockCheckedTextView(null, null, -1);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new MockCheckedTextView(null, null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new MockCheckedTextView(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testChecked() {
        final ListView lv = (ListView) mActivity.findViewById(R.id.checkedtextview_listview);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                lv.setAdapter(new CheckedTextViewAdapter());

                lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                lv.setItemChecked(1, true);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(1, lv.getCheckedItemPosition());
        assertTrue(lv.isItemChecked(1));
        assertFalse(lv.isItemChecked(0));

        ListAdapter adapter = lv.getAdapter();
        CheckedTextView view0 = (CheckedTextView) adapter.getView(0, null, null);
        CheckedTextView view1 = (CheckedTextView) adapter.getView(1, null, null);
        CheckedTextView view2 = (CheckedTextView) adapter.getView(2, null, null);
        assertFalse(view0.isChecked());
        assertTrue(view1.isChecked());
        assertFalse(view2.isChecked());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                lv.setItemChecked(2, true);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertFalse(view0.isChecked());
        assertTrue(view1.isChecked());
        assertTrue(view2.isChecked());

        view0.setChecked(true);
        view1.setChecked(false);
        view2.setChecked(false);
        assertTrue(view0.isChecked());
        assertFalse(view1.isChecked());
        assertFalse(view2.isChecked());
    }

    public void testToggle() {
        CheckedTextView checkedTextView = new MockCheckedTextView(mActivity);
        assertFalse(checkedTextView.isChecked());

        checkedTextView.toggle();
        assertTrue(checkedTextView.isChecked());

        checkedTextView.toggle();
        assertFalse(checkedTextView.isChecked());

        checkedTextView.setChecked(true);
        checkedTextView.toggle();
        assertFalse(checkedTextView.isChecked());
    }

    public void testDrawableStateChanged() {
        MockCheckedTextView checkedTextView = new MockCheckedTextView(mActivity);

        checkedTextView.reset();
        assertFalse(checkedTextView.hasDrawableStateChanged());
        checkedTextView.refreshDrawableState();
        assertTrue(checkedTextView.hasDrawableStateChanged());
    }

    public void testSetPadding() {
        final CheckedTextView lv
                = (CheckedTextView) mActivity.findViewById(R.id.checkedtextview_test);
        assertNotNull(lv);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                lv.setPadding(1, 2, 3, 4);
                lv.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        int origTop = lv.getPaddingTop();
        int origBottom = lv.getPaddingBottom();
        int origLeft = lv.getPaddingLeft();
        int origRight = lv.getPaddingRight();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                lv.setPadding(10, 20, 30, 40);
                lv.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(origTop < lv.getPaddingTop());
        assertTrue(origBottom < lv.getPaddingBottom());
        assertTrue(origLeft < lv.getPaddingLeft());
        assertTrue(origRight < lv.getPaddingRight());
    }

    private void cleanUpForceLayoutFlags(View view) {
        if (view != null) {
            view.layout(0, 0, 0, 0);
            assertFalse(view.isLayoutRequested());
        }
    }

    public void testSetCheckMarkDrawableByDrawable() {
        CheckedTextView checkedTextView;
        int basePaddingRight = 10;

        // set drawable when checkedTextView is GONE
        checkedTextView = new MockCheckedTextView(mActivity);
        checkedTextView.setVisibility(View.GONE);
        Drawable firstDrawable = mResources.getDrawable(R.drawable.scenery);
        firstDrawable.setVisible(true, false);
        assertEquals(StateSet.WILD_CARD, firstDrawable.getState());
        cleanUpForceLayoutFlags(checkedTextView);

        checkedTextView.setCheckMarkDrawable(firstDrawable);
        assertEquals(firstDrawable.getIntrinsicWidth(), checkedTextView.getPaddingRight());
        assertFalse(firstDrawable.isVisible());
        assertTrue(Arrays.equals(checkedTextView.getDrawableState(), firstDrawable.getState()));
        assertTrue(checkedTextView.isLayoutRequested());

        // update drawable when checkedTextView is VISIBLE
        checkedTextView = new MockCheckedTextView(mActivity);
        checkedTextView.setVisibility(View.VISIBLE);
        checkedTextView.setPadding(0, 0, basePaddingRight, 0);
        Drawable secondDrawable = mResources.getDrawable(R.drawable.pass);
        secondDrawable.setVisible(true, false);
        assertEquals(StateSet.WILD_CARD, secondDrawable.getState());
        cleanUpForceLayoutFlags(checkedTextView);

        checkedTextView.setCheckMarkDrawable(secondDrawable);
        assertEquals(secondDrawable.getIntrinsicWidth() + basePaddingRight,
                checkedTextView.getPaddingRight());
        assertTrue(secondDrawable.isVisible());
        assertTrue(Arrays.equals(checkedTextView.getDrawableState(), secondDrawable.getState()));
        assertTrue(checkedTextView.isLayoutRequested());

        cleanUpForceLayoutFlags(checkedTextView);
        checkedTextView.setCheckMarkDrawable(null);
        assertEquals(basePaddingRight, checkedTextView.getPaddingRight());
        assertTrue(checkedTextView.isLayoutRequested());
    }

    public void testSetCheckMarkDrawableById() {
        CheckedTextView checkedTextView;
        int basePaddingRight = 10;

        // set drawable
        checkedTextView = new MockCheckedTextView(mActivity);
        checkedTextView.setPadding(0, 0, basePaddingRight, 0);
        Drawable firstDrawable = mResources.getDrawable(R.drawable.scenery);
        cleanUpForceLayoutFlags(checkedTextView);

        checkedTextView.setCheckMarkDrawable(R.drawable.scenery);
        assertEquals(firstDrawable.getIntrinsicWidth() + basePaddingRight,
                checkedTextView.getPaddingRight());
        assertTrue(checkedTextView.isLayoutRequested());

        // set the same drawable again
        cleanUpForceLayoutFlags(checkedTextView);
        checkedTextView.setCheckMarkDrawable(R.drawable.scenery);
        assertEquals(firstDrawable.getIntrinsicWidth() + basePaddingRight,
                checkedTextView.getPaddingRight());
        assertFalse(checkedTextView.isLayoutRequested());

        // update drawable
        Drawable secondDrawable = mResources.getDrawable(R.drawable.pass);
        checkedTextView.setCheckMarkDrawable(secondDrawable);
        assertEquals(secondDrawable.getIntrinsicWidth() + basePaddingRight,
                checkedTextView.getPaddingRight());
        assertTrue(checkedTextView.isLayoutRequested());

        // resId is 0
        checkedTextView = new MockCheckedTextView(mActivity);
        checkedTextView.setPadding(0, 0, basePaddingRight, 0);
        cleanUpForceLayoutFlags(checkedTextView);

        checkedTextView.setCheckMarkDrawable(0);
        assertEquals(basePaddingRight, checkedTextView.getPaddingRight());
        assertFalse(checkedTextView.isLayoutRequested());
    }

    public void testOnDraw() {
        // Do not test. Implementation details.
    }

    public void testOnCreateDrawableState() {
        // Do not test. Implementation details.
    }

    private static final class MockCheckedTextView extends CheckedTextView {
        private boolean mHasRefreshDrawableState = false;
        private boolean mHasDrawableStateChanged = false;

        public MockCheckedTextView(Context context) {
            super(context);
        }

        public MockCheckedTextView(Context context, AttributeSet attrs) {
            super(context, attrs, 0);
        }

        public MockCheckedTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public static int[] getSuperViewStateSet() {
            return ENABLED_STATE_SET;
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            mHasDrawableStateChanged = true;
        }

        @Override
        protected int[] onCreateDrawableState(int extraSpace) {
            return super.onCreateDrawableState(extraSpace);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }

        @Override
        public void refreshDrawableState() {
            mHasRefreshDrawableState = true;
            super.refreshDrawableState();
        }

        public boolean hasRefreshDrawableState() {
            return mHasRefreshDrawableState;
        }

        public boolean hasDrawableStateChanged() {
            return mHasDrawableStateChanged;
        }

        public void reset() {
            mHasRefreshDrawableState = false;
            mHasDrawableStateChanged = false;
        }
    }

    private class CheckedTextViewAdapter extends BaseAdapter {
        private CheckedTextView[] mCheckedTextViews = new CheckedTextView[]{
                new MockCheckedTextView(mActivity),
                new MockCheckedTextView(mActivity),
                new MockCheckedTextView(mActivity),
        };

        public int getCount() {
            return mCheckedTextViews.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return mCheckedTextViews[position];
        }
    }
}

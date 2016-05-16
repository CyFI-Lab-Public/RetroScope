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
import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase;
import android.test.TouchUtils;
import android.test.UiThreadTest;
import android.test.ViewAsserts;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.GridLayoutAnimationController.AnimationParameters;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Test {@link GridView}.
 */
public class GridViewTest extends ActivityInstrumentationTestCase<GridViewStubActivity> {
    private GridView mGridView;
    private Activity mActivity;
    private Instrumentation mInstrumentation;

    public GridViewTest() {
        super("com.android.cts.stub", GridViewStubActivity.class);
    }

    private GridView findGridViewById(int id) {
        return (GridView) mActivity.findViewById(id);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mGridView = null;
        mActivity = getActivity();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return mActivity.hasWindowFocus();
            }
        }.run();
        mInstrumentation = getInstrumentation();
    }

    public void testConstructor() {
        new GridView(mActivity);

        new GridView(mActivity, null);

        new GridView(mActivity, null, com.android.internal.R.attr.gridViewStyle);

        XmlPullParser parser = mActivity.getResources().getXml(R.layout.gridview_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new GridView(mActivity, attrs);
        new GridView(mActivity, attrs, 0);

        try {
            new GridView(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new GridView(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new GridView(null, null, 0);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testAccessAdapter() {
        mGridView = new GridView(mActivity);
        // set Adapter
        ImageAdapter adapter = new ImageAdapter(mActivity);
        mGridView.setAdapter(adapter);
        assertSame(adapter, mGridView.getAdapter());

        mGridView.setAdapter(null);
        assertNull(mGridView.getAdapter());
    }

    public void testSetSelection() {
        mGridView = new GridView(mActivity);
        mGridView.setSelection(0);
        assertEquals(0, mGridView.getSelectedItemPosition());

        mGridView.setSelection(-1);
        assertEquals(-1, mGridView.getSelectedItemPosition());

        mGridView.setSelection(mGridView.getCount());
        assertEquals(mGridView.getCount(), mGridView.getSelectedItemPosition());
    }

    public void testPressKey() {
        final int NUM_COLUMNS = 3;
        mGridView = findGridViewById(R.id.gridview);

        MockOnItemClickListener listener = new MockOnItemClickListener();
        mGridView.setOnItemClickListener(listener);

        // this test case can not be ran in UI thread.
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new ImageAdapter(mActivity));
                mGridView.setNumColumns(NUM_COLUMNS);
                mGridView.invalidate();
                mGridView.requestLayout();
                mGridView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(0, mGridView.getSelectedItemPosition());
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
        mInstrumentation.sendKeySync(event);
        assertEquals(1, mGridView.getSelectedItemPosition());

        event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
        mInstrumentation.sendKeySync(event);
        assertEquals(0, mGridView.getSelectedItemPosition());

        assertEquals(0, mGridView.getSelectedItemPosition());
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        assertEquals(1, mGridView.getSelectedItemPosition());

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);
        assertEquals(0, mGridView.getSelectedItemPosition());

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        assertEquals(NUM_COLUMNS, mGridView.getSelectedItemPosition());

        assertFalse(listener.hasOnItemClickCalled());
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        assertTrue(listener.hasOnItemClickCalled());

        listener.reset();
        assertFalse(listener.hasOnItemClickCalled());
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
        assertTrue(listener.hasOnItemClickCalled());
    }

    public void testSetGravity() {
        mGridView = findGridViewById(R.id.gridview);

        View child;
        final int NUM_COLUMNS = 1;
        // this test case can not be ran in UI thread.
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new ImageAdapter(mActivity));
                mGridView.setNumColumns(NUM_COLUMNS);
                mGridView.setHorizontalSpacing(0);
                mGridView.setVerticalSpacing(0);
            }
        });
        mInstrumentation.waitForIdleSync();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setGravity(Gravity.CENTER_HORIZONTAL);
                mGridView.invalidate();
                mGridView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();

        child = mGridView.getChildAt(0); // get the first view.
        ViewAsserts.assertHorizontalCenterAligned(mGridView, child);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setGravity(Gravity.LEFT);
                mGridView.invalidate();
                mGridView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();

        child = mGridView.getChildAt(0); // get the first view.
        ViewAsserts.assertLeftAligned(mGridView, child, mGridView.getListPaddingLeft());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setGravity(Gravity.RIGHT);
                mGridView.invalidate();
                mGridView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();

        child = mGridView.getChildAt(0); // get the first view.
        ViewAsserts.assertRightAligned(mGridView, child, mGridView.getListPaddingRight());
    }

    public void testSetHorizontalSpacing() {
        mGridView = findGridViewById(R.id.gridview);
        mGridView.setStretchMode(GridView.NO_STRETCH);
        // Number of columns should be big enough, otherwise the
        // horizontal spacing cannot be correctly verified.
        mGridView.setNumColumns(28);


        // this test case can not be ran in UI thread.
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new MockGridViewAdapter(3));
                mGridView.setHorizontalSpacing(0);
            }
        });
        mInstrumentation.waitForIdleSync();

        View child0 = mGridView.getChildAt(0);
        View child1 = mGridView.getChildAt(1);
        assertEquals(0, child1.getLeft() - child0.getRight());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setHorizontalSpacing(5);
            }
        });
        mInstrumentation.waitForIdleSync();

        child0 = mGridView.getChildAt(0);
        child1 = mGridView.getChildAt(1);
        assertEquals(5, child1.getLeft() - child0.getRight());
    }

    public void testSetVerticalSpacing() {
        mGridView = findGridViewById(R.id.gridview);

        // this test case can not be ran in UI thread.
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new MockGridViewAdapter(3));
                mGridView.setVerticalSpacing(0);
            }
        });
        mInstrumentation.waitForIdleSync();

        View child0 = mGridView.getChildAt(0);
        View child1 = mGridView.getChildAt(1);
        assertEquals(0, child1.getTop() - child0.getBottom());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setVerticalSpacing(5);
            }
        });
        mInstrumentation.waitForIdleSync();

        child0 = mGridView.getChildAt(0);
        child1 = mGridView.getChildAt(1);
        assertEquals(5, child1.getTop() - child0.getBottom());
    }

    public void testAccessStretchMode() {
        mGridView = findGridViewById(R.id.gridview);
        View child;

        final int NUM_COLUMNS = 8;
        // this test case can not be ran in UI thread.
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new ImageAdapter(mActivity));
                mGridView.setColumnWidth(10);
                mGridView.setNumColumns(NUM_COLUMNS);
                mGridView.setHorizontalSpacing(0);
                mGridView.setVerticalSpacing(0);
                mGridView.invalidate();
                mGridView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();

        int[][] childRight = new int[3][3];
        int STRETCH_SPACING = 0;
        int STRETCH_COLUMN_WIDTH = 1;
        int STRETCH_SPACING_UNIFORM = 2;
        int INDEX_RIGHTMOST = 0;
        int INDEX_0 = 1;
        int INDEX_1 = 2;

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setColumnWidth(15);
                mGridView.setStretchMode(GridView.STRETCH_SPACING);
                mGridView.invalidate();
                mGridView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(GridView.STRETCH_SPACING, mGridView.getStretchMode());
        child = mGridView.getChildAt(NUM_COLUMNS - 1); // get the rightmost view at the first line.
        childRight[STRETCH_SPACING][INDEX_RIGHTMOST] = child.getRight();

        child = mGridView.getChildAt(0);
        childRight[STRETCH_SPACING][INDEX_0] = child.getRight();

        child = mGridView.getChildAt(1);
        childRight[STRETCH_SPACING][INDEX_1] = child.getRight();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setColumnWidth(15);
                mGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
                mGridView.invalidate();
                mGridView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(GridView.STRETCH_COLUMN_WIDTH, mGridView.getStretchMode());
        child = mGridView.getChildAt(NUM_COLUMNS - 1); // get the rightmost view at the first line.
        childRight[STRETCH_COLUMN_WIDTH][INDEX_RIGHTMOST] = child.getRight();

        child = mGridView.getChildAt(0);
        childRight[STRETCH_COLUMN_WIDTH][INDEX_0] = child.getRight();

        child = mGridView.getChildAt(1);
        childRight[STRETCH_COLUMN_WIDTH][INDEX_1] = child.getRight();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setColumnWidth(15);
                mGridView.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
                mGridView.invalidate();
                mGridView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(GridView.STRETCH_SPACING_UNIFORM, mGridView.getStretchMode());
        child = mGridView.getChildAt(NUM_COLUMNS - 1); // get the rightmost view at the first line.
        childRight[STRETCH_SPACING_UNIFORM][INDEX_RIGHTMOST] = child.getRight();

        child = mGridView.getChildAt(0);
        childRight[STRETCH_SPACING_UNIFORM][INDEX_0] = child.getRight();

        child = mGridView.getChildAt(1);
        childRight[STRETCH_SPACING_UNIFORM][INDEX_1] = child.getRight();

        assertTrue(childRight[STRETCH_SPACING][INDEX_RIGHTMOST]
                > childRight[STRETCH_COLUMN_WIDTH][INDEX_RIGHTMOST]);
        assertTrue(childRight[STRETCH_SPACING][INDEX_RIGHTMOST]
                > childRight[STRETCH_SPACING_UNIFORM][INDEX_RIGHTMOST]);
        assertTrue(childRight[STRETCH_SPACING][INDEX_0]
                == childRight[STRETCH_COLUMN_WIDTH][INDEX_0]);
        assertTrue(childRight[STRETCH_SPACING][INDEX_0]
                < childRight[STRETCH_SPACING_UNIFORM][INDEX_0]);
        assertTrue(childRight[STRETCH_SPACING][INDEX_1]
                > childRight[STRETCH_COLUMN_WIDTH][INDEX_1]);
        assertTrue(childRight[STRETCH_SPACING][INDEX_1]
                < childRight[STRETCH_SPACING_UNIFORM][INDEX_1]);
    }

    public void testSetNumColumns() {
        mGridView = findGridViewById(R.id.gridview);

        // this test case can not be ran in UI thread.
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new MockGridViewAdapter(10));
                mGridView.setHorizontalSpacing(0);
                mGridView.setVerticalSpacing(0);
                mGridView.setNumColumns(10);
            }
        });
        mInstrumentation.waitForIdleSync();

        View child0 = mGridView.getChildAt(0);
        View child9 = mGridView.getChildAt(9);
        assertEquals(child0.getBottom(), child9.getBottom());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setNumColumns(9);
            }
        });
        mInstrumentation.waitForIdleSync();

        child0 = mGridView.getChildAt(0);
        child9 = mGridView.getChildAt(9);
        assertEquals(child0.getBottom(), child9.getTop());
        assertEquals(child0.getLeft(), child9.getLeft());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setNumColumns(1);
            }
        });
        mInstrumentation.waitForIdleSync();

        for (int i = 0; i < mGridView.getChildCount(); i++) {
            View child = mGridView.getChildAt(i);
            assertEquals(0, child.getLeft() - mGridView.getListPaddingLeft());
        }
    }

    public void testGetNumColumns() {
        mGridView = new GridView(mActivity);

        assertEquals(mGridView.getNumColumns(), GridView.AUTO_FIT);

        mGridView = findGridViewById(R.id.gridview);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new MockGridViewAdapter(10));
                mGridView.setNumColumns(10);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(mGridView.getNumColumns(), 10);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setNumColumns(1);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(mGridView.getNumColumns(), 1);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setNumColumns(0);
            }
        });
        mInstrumentation.waitForIdleSync();

        //although setNumColumns(0) was called, the number of columns should be 1
        assertEquals(mGridView.getNumColumns(), 1);
    }

    public void testAttachLayoutAnimationParameters() {
        MockGridView mockGridView = new MockGridView(mActivity);
        ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(320, 480);
        mockGridView.attachLayoutAnimationParameters(null, p, 1, 2);
        AnimationParameters animationParams = (AnimationParameters) p.layoutAnimationParameters;
        assertEquals(1, animationParams.index);
        assertEquals(2, animationParams.count);
    }

    public void testLayoutChildren() {
        MockGridView mockGridView = new MockGridView(mActivity);
        mockGridView.layoutChildren();
    }

    @UiThreadTest
    public void testOnFocusChanged() {
        final MockGridView mockGridView = new MockGridView(mActivity);

        assertFalse(mockGridView.hasCalledOnFocusChanged());
        mockGridView.setAdapter(new MockGridViewAdapter(10));
        mockGridView.setFocusable(true);
        mockGridView.requestFocus();

        assertTrue(mockGridView.hasCalledOnFocusChanged());
        mockGridView.reset();
        assertFalse(mockGridView.hasCalledOnFocusChanged());

        mockGridView.clearFocus();

        assertTrue(mockGridView.hasCalledOnFocusChanged());
    }

    public void testOnMeasure() {
        // Do not test it. It's implementation detail.
    }

    public void testSetColumnWidth() {
        mGridView = findGridViewById(R.id.gridview);

        // this test case can not be ran in UI thread.
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setAdapter(new MockGridViewAdapter(10));
                mGridView.setNumColumns(GridView.AUTO_FIT);
                mGridView.setHorizontalSpacing(0);
                mGridView.setVerticalSpacing(0);
                mGridView.setColumnWidth(0);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Verify whether column number equals 2.
        View child0 = mGridView.getChildAt(0);
        View child1 = mGridView.getChildAt(1);
        View child2 = mGridView.getChildAt(2);
        assertEquals(child0.getBottom(), child1.getBottom());
        assertEquals(child0.getLeft(), child2.getLeft());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mGridView.setNumColumns(GridView.AUTO_FIT);
                mGridView.setColumnWidth(Integer.MAX_VALUE);
            }
        });
        mInstrumentation.waitForIdleSync();

        child0 = mGridView.getChildAt(0);
        child1 = mGridView.getChildAt(1);
        assertEquals(child0.getBottom(), child1.getTop());
        assertEquals(child0.getLeft(), child1.getLeft());
    }

    private static class MockGridView extends GridView {
        private boolean mCalledOnMeasure = false;
        private boolean mCalledOnFocusChanged = false;

        public boolean hasCalledOnMeasure() {
            return mCalledOnMeasure;
        }

        public boolean hasCalledOnFocusChanged() {
            return mCalledOnFocusChanged;
        }

        public void reset() {
            mCalledOnMeasure = false;
            mCalledOnFocusChanged = false;
        }

        public MockGridView(Context context) {
            super(context);
        }

        public MockGridView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MockGridView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public void attachLayoutAnimationParameters(View child,
                ViewGroup.LayoutParams params, int index, int count) {
            super.attachLayoutAnimationParameters(child, params, index, count);
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
        }

        @Override
        protected int computeVerticalScrollExtent() {
            return super.computeVerticalScrollExtent();
        }

        @Override
        protected int computeVerticalScrollOffset() {
            return super.computeVerticalScrollOffset();
        }

        @Override
        protected int computeVerticalScrollRange() {
            return super.computeVerticalScrollRange();
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction,
                Rect previouslyFocusedRect) {
            mCalledOnFocusChanged = true;
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mCalledOnMeasure = true;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    class MockOnItemClickListener implements OnItemClickListener {
        private boolean mOnItemClickCalled = false;

        public boolean hasOnItemClickCalled() {
            return mOnItemClickCalled;
        }

        public void reset() {
            mOnItemClickCalled = false;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mOnItemClickCalled = true;
        }
    }

    private class MockGridViewAdapter implements ListAdapter, Filterable {
        private final int mCount;

        MockGridViewAdapter(int count) {
            mCount = count;
        }

        MockGridViewAdapter() {
            this(1);
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int position) {
            return true;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        public int getCount() {
            return mCount;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return false;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if ((convertView != null) && (convertView instanceof ImageView)) {
                ((ImageView) convertView).setImageResource(R.drawable.size_48x48);
                return convertView;
            }

            ImageView newView = new ImageView(mActivity);
            AbsListView.LayoutParams params = new AbsListView.LayoutParams(
                                                  AbsListView.LayoutParams.WRAP_CONTENT,
                                                  AbsListView.LayoutParams.WRAP_CONTENT);
            newView.setLayoutParams(params);
            newView.setImageResource(R.drawable.size_48x48);
            return newView;
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return false;
        }

        public Filter getFilter() {
            return new FilterTest();
        }
    }

    private static class FilterTest extends Filter {
        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
        }
    }

    public class ImageAdapter implements ListAdapter {
        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return mThumbIds.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                int layoutSize = (int)(50 * mContext.getResources().getDisplayMetrics().density);
                imageView.setLayoutParams(new GridView.LayoutParams(layoutSize, layoutSize));
                imageView.setAdjustViewBounds(false);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
                imageView.setPadding(0, 0, 0, 0);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(mThumbIds[position]);

            return imageView;
        }

        private Context mContext;

        private Integer[] mThumbIds = {
                R.drawable.failed, R.drawable.pass,
                R.drawable.animated, R.drawable.black,
                R.drawable.blue, R.drawable.red,
                R.drawable.animated, R.drawable.black,
                R.drawable.blue, R.drawable.failed,
                R.drawable.pass, R.drawable.red,
        };

        private final DataSetObservable mDataSetObservable = new DataSetObservable();

        public boolean hasStableIds() {
            return false;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.registerObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.unregisterObserver(observer);
        }

        public void notifyDataSetChanged() {
            mDataSetObservable.notifyChanged();
        }

        public void notifyDataSetInvalidated() {
            mDataSetObservable.notifyInvalidated();
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int position) {
            return true;
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return getCount() == 0;
        }
    }
}

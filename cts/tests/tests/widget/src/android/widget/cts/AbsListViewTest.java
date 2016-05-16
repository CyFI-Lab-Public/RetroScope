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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AbsListViewTest extends ActivityInstrumentationTestCase2<ListViewStubActivity> {
    private final String[] mShortList = new String[] {
        "This", "is", "short", "!",
    };
    private final String[] mCountryList = new String[] {
        "Argentina", "Australia", "China", "France", "Germany", "Italy", "Japan", "United States",
        "Argentina", "Australia", "China", "France", "Germany", "Italy", "Japan", "United States",
        "Argentina", "Australia", "China", "France", "Germany", "Italy", "Japan", "United States"
    };

    private ListView mListView;
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private AttributeSet mAttributeSet;
    private ArrayAdapter<String> mAdapter_short;
    private ArrayAdapter<String> mAdapter_countries;

    private static final float DELTA = 0.001f;

    public AbsListViewTest() {
        super("com.android.cts.stub", ListViewStubActivity.class);
    }


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
        mInstrumentation = getInstrumentation();

        XmlPullParser parser = mActivity.getResources().getXml(R.layout.listview_layout);
        WidgetTestUtils.beginDocument(parser, "LinearLayout");
        mAttributeSet = Xml.asAttributeSet(parser);

        mAdapter_short = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_list_item_1, mShortList);
        mAdapter_countries = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_list_item_1, mCountryList);

        mListView = (ListView)mActivity.findViewById(R.id.listview_default);
    }

    public void testConstructor() {
        /**
         * We can not test the constructors.
         */
    }

    public void testAccessFastScrollEnabled() {
        mListView.setFastScrollEnabled(false);
        assertFalse(mListView.isFastScrollEnabled());

        mListView.setFastScrollEnabled(true);
        assertTrue(mListView.isFastScrollEnabled());
    }

    public void testAccessSmoothScrollbarEnabled() {
        mListView.setSmoothScrollbarEnabled(false);
        assertFalse(mListView.isSmoothScrollbarEnabled());

        mListView.setSmoothScrollbarEnabled(true);
        assertTrue(mListView.isSmoothScrollbarEnabled());
    }

    public void testAccessScrollingCacheEnabled() {
        mListView.setScrollingCacheEnabled(false);
        assertFalse(mListView.isScrollingCacheEnabled());

        mListView.setScrollingCacheEnabled(true);
        assertTrue(mListView.isScrollingCacheEnabled());
    }

    private void setAdapter() throws Throwable {
        setAdapter(mAdapter_countries);
    }

    private void setAdapter(final ListAdapter adapter) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.setAdapter(adapter);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void setListSelection(int index) throws Throwable {
        final int i = index;

        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.setSelection(i);
            }
        });
        mInstrumentation.waitForIdleSync();
    }
    public void testSetOnScrollListener() throws Throwable {
        MockOnScrollListener onScrollListener = new MockOnScrollListener();

        assertNull(onScrollListener.getView());
        assertEquals(0, onScrollListener.getFirstVisibleItem());
        assertEquals(0, onScrollListener.getVisibleItemCount());
        assertEquals(0, onScrollListener.getTotalItemCount());
        assertEquals(-1, onScrollListener.getScrollState());

        assertFalse(onScrollListener.isOnScrollCalled());
        assertFalse(onScrollListener.isOnScrollStateChangedCalled());

        mListView.setOnScrollListener(onScrollListener);
        assertSame(mListView, onScrollListener.getView());
        assertEquals(0, onScrollListener.getFirstVisibleItem());
        assertEquals(0, onScrollListener.getVisibleItemCount());
        assertEquals(0, onScrollListener.getTotalItemCount());
        assertEquals(-1, onScrollListener.getScrollState());

        assertTrue(onScrollListener.isOnScrollCalled());
        assertFalse(onScrollListener.isOnScrollStateChangedCalled());
        onScrollListener.reset();

        setAdapter();

        assertSame(mListView, onScrollListener.getView());
        assertEquals(0, onScrollListener.getFirstVisibleItem());
        assertEquals(mListView.getChildCount(), onScrollListener.getVisibleItemCount());
        assertEquals(mCountryList.length, onScrollListener.getTotalItemCount());
        assertEquals(-1, onScrollListener.getScrollState());

        assertTrue(onScrollListener.isOnScrollCalled());
        assertFalse(onScrollListener.isOnScrollStateChangedCalled());
        onScrollListener.reset();

        TouchUtils.scrollToBottom(this, mActivity, mListView);
        assertSame(mListView, onScrollListener.getView());
        assertEquals(mListView.getChildCount(), onScrollListener.getVisibleItemCount());
        assertEquals(mCountryList.length, onScrollListener.getTotalItemCount());
        assertEquals(OnScrollListener.SCROLL_STATE_IDLE, onScrollListener.getScrollState());

        assertTrue(onScrollListener.isOnScrollCalled());
        assertTrue(onScrollListener.isOnScrollStateChangedCalled());
    }

    public void testGetFocusedRect() throws Throwable {
        setAdapter(mAdapter_short);
        setListSelection(0);

        Rect r1 = new Rect();
        mListView.getFocusedRect(r1);

        assertEquals(0, r1.top);
        assertTrue(r1.bottom > 0);
        assertEquals(0, r1.left);
        assertTrue(r1.right > 0);

        setListSelection(3);
        Rect r2 = new Rect();
        mListView.getFocusedRect(r2);
        assertTrue(r2.top > 0);
        assertTrue(r2.bottom > 0);
        assertEquals(0, r2.left);
        assertTrue(r2.right > 0);

        assertTrue(r2.top > r1.top);
        assertEquals(r1.bottom - r1.top, r2.bottom - r2.top);
        assertEquals(r1.right, r2.right);
    }

    public void testAccessStackFromBottom() throws Throwable {
        setAdapter();

        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.setStackFromBottom(false);
            }
        });
        assertFalse(mListView.isStackFromBottom());
        assertEquals(0, mListView.getSelectedItemPosition());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.setStackFromBottom(true);
            }
        });

        mInstrumentation.waitForIdleSync();
        assertTrue(mListView.isStackFromBottom());
        // ensure last item in list is selected
        assertEquals(mCountryList.length-1, mListView.getSelectedItemPosition());
    }

    public void testAccessSelectedItem() throws Throwable {
        assertNull(mListView.getSelectedView());

        setAdapter();
        TextView tv = (TextView) mListView.getSelectedView();
        assertEquals(mCountryList[0], tv.getText().toString());

        setListSelection(5);
        tv = (TextView) mListView.getSelectedView();
        assertEquals(mCountryList[5], tv.getText().toString());

        setListSelection(2);
        tv = (TextView) mListView.getSelectedView();
        assertEquals(mCountryList[2], tv.getText().toString());
    }

    public void testAccessListPadding() throws Throwable {
        setAdapter();

        assertEquals(0, mListView.getListPaddingLeft());
        assertEquals(0, mListView.getListPaddingTop());
        assertEquals(0, mListView.getListPaddingRight());
        assertEquals(0, mListView.getListPaddingBottom());

        final Rect r = new Rect(0, 0, 40, 60);
        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.setPadding(r.left, r.top, r.right, r.bottom);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(r.left, mListView.getListPaddingLeft());
        assertEquals(r.top, mListView.getListPaddingTop());
        assertEquals(r.right, mListView.getListPaddingRight());
        assertEquals(r.bottom, mListView.getListPaddingBottom());
    }

    public void testAccessSelector() throws Throwable {
        setAdapter();

        final Drawable d = mActivity.getResources().getDrawable(R.drawable.pass);
        mListView.setSelector(d);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertSame(d, mListView.getSelector());
        assertTrue(mListView.verifyDrawable(d));

        mListView.setSelector(R.drawable.failed);
        mListView.setDrawSelectorOnTop(true);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();

        Drawable drawable = mListView.getSelector();
        assertNotNull(drawable);
        final Rect r = drawable.getBounds();

        final TextView v = (TextView) mListView.getSelectedView();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return v.getRight() == r.right;
            }
        }.run();
        assertEquals(v.getLeft(), r.left);
        assertEquals(v.getTop(), r.top);
        assertEquals(v.getBottom(), r.bottom);
    }

    public void testSetScrollIndicators() throws Throwable {
        TextView tv1 = (TextView) mActivity.findViewById(R.id.headerview1);
        TextView tv2 = (TextView) mActivity.findViewById(R.id.footerview1);

        setAdapter();

        mListView.setScrollIndicators(tv1, tv2);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.requestLayout();
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void testShowContextMenuForChild() throws Throwable {
        setAdapter();
        setListSelection(1);

        TextView tv = (TextView) mListView.getSelectedView();
        assertFalse(mListView.showContextMenuForChild(tv));

        // TODO: how to show the contextMenu success
    }

    public void testPointToPosition() throws Throwable {
        assertEquals(AbsListView.INVALID_POSITION, mListView.pointToPosition(-1, -1));
        assertEquals(AbsListView.INVALID_ROW_ID, mListView.pointToRowId(-1, -1));

        setAdapter();

        View row = mListView.getChildAt(0);
        int rowHeight = row.getHeight();
        int middleOfSecondRow = rowHeight + rowHeight/2;

        int position1 = mListView.pointToPosition(0, 0);
        int position2 = mListView.pointToPosition(50, middleOfSecondRow);

        assertEquals(mAdapter_countries.getItemId(position1), mListView.pointToRowId(0, 0));
        assertEquals(mAdapter_countries.getItemId(position2),
                mListView.pointToRowId(50, middleOfSecondRow));

        assertTrue(position2 > position1);
    }

    public void testDraw() {
        Canvas canvas = new Canvas();
        mListView.draw(canvas);

        MyListView listView = new MyListView(mActivity);
        listView.dispatchDraw(canvas);

        // TODO: how to check
    }

    public void testSetRecyclerListener() throws Throwable {
        setAdapter();

        MockRecyclerListener recyclerListener = new MockRecyclerListener();
        List<View> views = new ArrayList<View>();

        assertNull(recyclerListener.getView());
        mListView.setRecyclerListener(recyclerListener);
        mListView.reclaimViews(views);

        assertTrue(views.size() > 0);
        assertNotNull(recyclerListener.getView());

        assertSame(recyclerListener.getView(), views.get(views.size() - 1));
    }

    public void testAccessCacheColorHint() {
        mListView.setCacheColorHint(Color.RED);
        assertEquals(Color.RED, mListView.getCacheColorHint());
        assertEquals(Color.RED, mListView.getSolidColor());

        mListView.setCacheColorHint(Color.LTGRAY);
        assertEquals(Color.LTGRAY, mListView.getCacheColorHint());
        assertEquals(Color.LTGRAY, mListView.getSolidColor());

        mListView.setCacheColorHint(Color.GRAY);
        assertEquals(Color.GRAY, mListView.getCacheColorHint());
        assertEquals(Color.GRAY, mListView.getSolidColor());
    }

    public void testAccessTranscriptMode() {
        mListView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        assertEquals(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL, mListView.getTranscriptMode());

        mListView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
        assertEquals(AbsListView.TRANSCRIPT_MODE_DISABLED, mListView.getTranscriptMode());

        mListView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
        assertEquals(AbsListView.TRANSCRIPT_MODE_NORMAL, mListView.getTranscriptMode());
    }

    public void testCheckLayoutParams() {
        MyListView listView = new MyListView(mActivity);

        AbsListView.LayoutParams param1 = new AbsListView.LayoutParams(10, 10);
        assertTrue(listView.checkLayoutParams(param1));

        ViewGroup.LayoutParams param2 = new ViewGroup.LayoutParams(10, 10);
        assertFalse(listView.checkLayoutParams(param2));
    }

    public void testComputeVerticalScrollValues() {
        MyListView listView = new MyListView(mActivity);
        assertEquals(0, listView.computeVerticalScrollRange());
        assertEquals(0, listView.computeVerticalScrollOffset());
        assertEquals(0, listView.computeVerticalScrollExtent());

        listView.setAdapter(mAdapter_countries);
        listView.setSmoothScrollbarEnabled(false);
        assertEquals(mAdapter_countries.getCount(), listView.computeVerticalScrollRange());
        assertEquals(0, listView.computeVerticalScrollOffset());
        assertEquals(0, listView.computeVerticalScrollExtent());

        listView.setSmoothScrollbarEnabled(true);
        assertEquals(0, listView.computeVerticalScrollOffset());
        assertEquals(0, listView.computeVerticalScrollExtent());
    }

    public void testGenerateLayoutParams() throws XmlPullParserException, IOException {
        ViewGroup.LayoutParams res = mListView.generateLayoutParams(mAttributeSet);
        assertNotNull(res);
        assertTrue(res instanceof AbsListView.LayoutParams);

        MyListView listView = new MyListView(mActivity);
        ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                              ViewGroup.LayoutParams.WRAP_CONTENT);

        res = listView.generateLayoutParams(p);
        assertNotNull(res);
        assertTrue(res instanceof AbsListView.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, res.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, res.height);
    }

    public void testBeforeAndAfterTextChanged() {
        // The java doc says these two methods do nothing
        CharSequence str = "test";
        SpannableStringBuilder sb = new SpannableStringBuilder();

        mListView.beforeTextChanged(str, 0, str.length(), str.length());
        mListView.afterTextChanged(sb);

        // test callback
        MyListView listView = new MyListView(mActivity);
        TextView tv = new TextView(mActivity);

        assertFalse(listView.isBeforeTextChangedCalled());
        assertFalse(listView.isOnTextChangedCalled());
        assertFalse(listView.isAfterTextChangedCalled());

        tv.addTextChangedListener(listView);
        assertFalse(listView.isBeforeTextChangedCalled());
        assertFalse(listView.isOnTextChangedCalled());
        assertFalse(listView.isAfterTextChangedCalled());

        tv.setText("abc");
        assertTrue(listView.isBeforeTextChangedCalled());
        assertTrue(listView.isOnTextChangedCalled());
        assertTrue(listView.isAfterTextChangedCalled());
    }

    public void testAddTouchables() throws Throwable {
        ArrayList<View> views = new ArrayList<View>();
        assertEquals(0, views.size());

        setAdapter();

        mListView.addTouchables(views);
        assertEquals(mListView.getChildCount(), views.size());
    }

    public void testInvalidateViews() throws Throwable {
        TextView tv1 = (TextView) mActivity.findViewById(R.id.headerview1);
        TextView tv2 = (TextView) mActivity.findViewById(R.id.footerview1);

        setAdapter();

        mListView.setScrollIndicators(tv1, tv2);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mListView.invalidateViews();
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void testGetContextMenuInfo() throws Throwable {
        final MyListView listView = new MyListView(mActivity, mAttributeSet);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(listView);
                listView.setAdapter(mAdapter_countries);
                listView.setSelection(2);
            }
        });
        mInstrumentation.waitForIdleSync();

        final TextView v = (TextView) listView.getSelectedView();
        assertNull(listView.getContextMenuInfo());

        final MockOnItemLongClickListener listener = new MockOnItemLongClickListener();
        listView.setOnItemLongClickListener(listener);

        assertNull(listener.getParent());
        assertNull(listener.getView());
        assertEquals(0, listener.getPosition());
        assertEquals(0, listener.getID());

        mInstrumentation.waitForIdleSync();
        TouchUtils.longClickView(this, v);

        new PollingCheck() {
            @Override
            protected boolean check() {
                return v == listener.getView();
            }
        }.run();

        assertSame(listView, listener.getParent());
        assertEquals(2, listener.getPosition());
        assertEquals(listView.getItemIdAtPosition(2), listener.getID());

        ContextMenuInfo cmi = listView.getContextMenuInfo();
        assertNotNull(cmi);
    }

    public void testGetTopBottomFadingEdgeStrength() {
        MyListView listView = new MyListView(mActivity);

        assertEquals(0.0f, listView.getTopFadingEdgeStrength(), DELTA);
        assertEquals(0.0f, listView.getBottomFadingEdgeStrength(), DELTA);
    }

    public void testHandleDataChanged() {
        MyListView listView = new MyListView(mActivity, mAttributeSet, 0);
        listView.handleDataChanged();
        // TODO: how to check?
    }

    public void testSetFilterText() {
        MyListView listView = new MyListView(mActivity, mAttributeSet, 0);
        String filterText = "xyz";

        assertFalse(listView.isTextFilterEnabled());
        assertFalse(listView.hasTextFilter());
        assertFalse(listView.isInFilterMode());
        assertTrue(mListView.checkInputConnectionProxy(null));

        listView.setTextFilterEnabled(false);
        listView.setFilterText(filterText);
        assertFalse(listView.isTextFilterEnabled());
        assertFalse(listView.hasTextFilter());
        assertFalse(listView.isInFilterMode());

        listView.setTextFilterEnabled(true);
        listView.setFilterText(null);
        assertTrue(listView.isTextFilterEnabled());
        assertFalse(listView.hasTextFilter());
        assertFalse(listView.isInFilterMode());

        listView.setTextFilterEnabled(true);
        listView.setFilterText(filterText);
        assertTrue(listView.isTextFilterEnabled());
        assertTrue(listView.hasTextFilter());
        assertTrue(listView.isInFilterMode());

        listView.clearTextFilter();
        assertTrue(listView.isTextFilterEnabled());
        assertFalse(listView.hasTextFilter());
        assertFalse(listView.isInFilterMode());
    }

    public void testLayoutChildren() {
        /**
         * the subclass ListView and GridView override this method, so we can not test
         * this method.
         */
    }

    public void testFoo() {
        /**
         * Do not test these APIs. They are callbacks which:
         *
         * 1. The callback machanism has been tested in super class
         * 2. The functionality is implmentation details, no need to test
         */
    }

    private static class MockOnScrollListener implements OnScrollListener {
        private AbsListView mView;
        private int mFirstVisibleItem;
        private int mVisibleItemCount;
        private int mTotalItemCount;
        private int mScrollState;

        private boolean mIsOnScrollCalled;
        private boolean mIsOnScrollStateChangedCalled;

        private MockOnScrollListener() {
            mView = null;
            mFirstVisibleItem = 0;
            mVisibleItemCount = 0;
            mTotalItemCount = 0;
            mScrollState = -1;

            mIsOnScrollCalled = false;
            mIsOnScrollStateChangedCalled = false;
        }

        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            mView = view;
            mFirstVisibleItem = firstVisibleItem;
            mVisibleItemCount = visibleItemCount;
            mTotalItemCount = totalItemCount;
            mIsOnScrollCalled = true;
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mScrollState = scrollState;
            mIsOnScrollStateChangedCalled = true;
        }

        public AbsListView getView() {
            return mView;
        }

        public int getFirstVisibleItem() {
            return mFirstVisibleItem;
        }

        public int getVisibleItemCount() {
            return mVisibleItemCount;
        }

        public int getTotalItemCount() {
            return mTotalItemCount;
        }

        public int getScrollState() {
            return mScrollState;
        }

        public boolean isOnScrollCalled() {
            return mIsOnScrollCalled;
        }

        public boolean isOnScrollStateChangedCalled() {
            return mIsOnScrollStateChangedCalled;
        }

        public void reset() {
            mIsOnScrollCalled = false;
            mIsOnScrollStateChangedCalled = false;
        }
    }

    private static class MockRecyclerListener implements RecyclerListener {
        private View mView;

        private MockRecyclerListener() {
            mView = null;
        }

        public void onMovedToScrapHeap(View view) {
            mView = view;
        }

        public View getView() {
            return mView;
        }
    }

    private static class MockOnItemLongClickListener implements OnItemLongClickListener {
        private AdapterView<?> parent;
        private View view;
        private int position;
        private long id;

        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            this.parent = parent;
            this.view = view;
            this.position = position;
            this.id = id;
            return false;
        }

        public AdapterView<?> getParent() {
            return parent;
        }

        public View getView() {
            return view;
        }

        public int getPosition() {
            return position;
        }

        public long getID() {
            return id;
        }
    }

    /**
     * MyListView for test
     */
    private static class MyListView extends ListView {
        public MyListView(Context context) {
            super(context);
        }

        public MyListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyListView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return super.checkLayoutParams(p);
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
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
        }

        @Override
        protected void dispatchSetPressed(boolean pressed) {
            super.dispatchSetPressed(pressed);
        }

        @Override
        protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return super.generateLayoutParams(p);
        }

        @Override
        protected float getBottomFadingEdgeStrength() {
            return super.getBottomFadingEdgeStrength();
        }

        @Override
        protected ContextMenuInfo getContextMenuInfo() {
            return super.getContextMenuInfo();
        }

        @Override
        protected float getTopFadingEdgeStrength() {
            return super.getTopFadingEdgeStrength();
        }

        @Override
        protected void handleDataChanged() {
            super.handleDataChanged();
        }

        @Override
        protected boolean isInFilterMode() {
            return super.isInFilterMode();
        }

        private boolean mIsBeforeTextChangedCalled;
        private boolean mIsOnTextChangedCalled;
        private boolean mIsAfterTextChangedCalled;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mIsBeforeTextChangedCalled = true;
            super.beforeTextChanged(s, start, count, after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mIsOnTextChangedCalled = true;
            super.onTextChanged(s, start, before, count);
        }

        @Override
        public void afterTextChanged(Editable s) {
            mIsAfterTextChangedCalled = true;
            super.afterTextChanged(s);
        }

        public boolean isBeforeTextChangedCalled() {
            return mIsBeforeTextChangedCalled;
        }

        public boolean isOnTextChangedCalled() {
            return mIsOnTextChangedCalled;
        }

        public boolean isAfterTextChangedCalled() {
            return mIsAfterTextChangedCalled;
        }
    }
}

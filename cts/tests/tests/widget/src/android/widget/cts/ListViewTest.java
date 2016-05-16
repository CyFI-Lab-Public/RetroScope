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
import com.google.android.collect.Lists;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

public class ListViewTest extends ActivityInstrumentationTestCase2<ListViewStubActivity> {
    private final String[] mCountryList = new String[] {
        "Argentina", "Australia", "China", "France", "Germany", "Italy", "Japan", "United States"
    };
    private final String[] mNameList = new String[] {
        "Jacky", "David", "Kevin", "Michael", "Andy"
    };
    private final String[] mEmptyList = new String[0];

    private ListView mListView;
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private AttributeSet mAttributeSet;
    private ArrayAdapter<String> mAdapter_countries;
    private ArrayAdapter<String> mAdapter_names;
    private ArrayAdapter<String> mAdapter_empty;

    public ListViewTest() {
        super("com.android.cts.stub", ListViewStubActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        XmlPullParser parser = mActivity.getResources().getXml(R.layout.listview_layout);
        mAttributeSet = Xml.asAttributeSet(parser);

        mAdapter_countries = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_list_item_1, mCountryList);
        mAdapter_names = new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1,
                mNameList);
        mAdapter_empty = new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1,
                mEmptyList);

        mListView = (ListView) mActivity.findViewById(R.id.listview_default);
    }

    public void testConstructor() {
        new ListView(mActivity);
        new ListView(mActivity, mAttributeSet);
        new ListView(mActivity, mAttributeSet, 0);

        try {
            new ListView(null);
            fail("There should be a NullPointerException thrown out. ");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new ListView(null, null);
            fail("There should be a NullPointerException thrown out. ");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new ListView(null, null, -1);
            fail("There should be a NullPointerException thrown out. ");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testGetMaxScrollAmount() {
        setAdapter(mAdapter_empty);
        int scrollAmount = mListView.getMaxScrollAmount();
        assertEquals(0, scrollAmount);

        setAdapter(mAdapter_names);
        scrollAmount = mListView.getMaxScrollAmount();
        assertTrue(scrollAmount > 0);
    }

    private void setAdapter(final ArrayAdapter<String> adapter) {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(adapter);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void testAccessDividerHeight() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
            }
        });
        mInstrumentation.waitForIdleSync();

        Drawable d = mListView.getDivider();
        final Rect r = d.getBounds();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return r.bottom - r.top > 0;
            }
        }.run();

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setDividerHeight(20);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(20, mListView.getDividerHeight());
        assertEquals(20, r.bottom - r.top);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setDividerHeight(10);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(10, mListView.getDividerHeight());
        assertEquals(10, r.bottom - r.top);
    }

    public void testAccessItemsCanFocus() {
        mListView.setItemsCanFocus(true);
        assertTrue(mListView.getItemsCanFocus());

        mListView.setItemsCanFocus(false);
        assertFalse(mListView.getItemsCanFocus());

        // TODO: how to check?
    }

    public void testAccessAdapter() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertSame(mAdapter_countries, mListView.getAdapter());
        assertEquals(mCountryList.length, mListView.getCount());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_names);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertSame(mAdapter_names, mListView.getAdapter());
        assertEquals(mNameList.length, mListView.getCount());
    }

    @UiThreadTest
    public void testAccessItemChecked() {
        // NONE mode
        mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        assertEquals(ListView.CHOICE_MODE_NONE, mListView.getChoiceMode());

        mListView.setItemChecked(1, true);
        assertEquals(ListView.INVALID_POSITION, mListView.getCheckedItemPosition());
        assertFalse(mListView.isItemChecked(1));

        // SINGLE mode
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        assertEquals(ListView.CHOICE_MODE_SINGLE, mListView.getChoiceMode());

        mListView.setItemChecked(2, true);
        assertEquals(2, mListView.getCheckedItemPosition());
        assertTrue(mListView.isItemChecked(2));

        mListView.setItemChecked(3, true);
        assertEquals(3, mListView.getCheckedItemPosition());
        assertTrue(mListView.isItemChecked(3));
        assertFalse(mListView.isItemChecked(2));

        // test attempt to uncheck a item that wasn't checked to begin with
        mListView.setItemChecked(4, false);
        // item three should still be checked
        assertEquals(3, mListView.getCheckedItemPosition());
        assertFalse(mListView.isItemChecked(4));
        assertTrue(mListView.isItemChecked(3));
        assertFalse(mListView.isItemChecked(2));

        mListView.setItemChecked(4, true);
        assertTrue(mListView.isItemChecked(4));
        mListView.clearChoices();
        assertEquals(ListView.INVALID_POSITION, mListView.getCheckedItemPosition());
        assertFalse(mListView.isItemChecked(4));

        // MULTIPLE mode
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        assertEquals(ListView.CHOICE_MODE_MULTIPLE, mListView.getChoiceMode());

        mListView.setItemChecked(1, true);
        assertEquals(ListView.INVALID_POSITION, mListView.getCheckedItemPosition());
        SparseBooleanArray array = mListView.getCheckedItemPositions();
        assertTrue(array.get(1));
        assertFalse(array.get(2));
        assertTrue(mListView.isItemChecked(1));
        assertFalse(mListView.isItemChecked(2));

        mListView.setItemChecked(2, true);
        mListView.setItemChecked(3, false);
        mListView.setItemChecked(4, true);

        assertTrue(array.get(1));
        assertTrue(array.get(2));
        assertFalse(array.get(3));
        assertTrue(array.get(4));
        assertTrue(mListView.isItemChecked(1));
        assertTrue(mListView.isItemChecked(2));
        assertFalse(mListView.isItemChecked(3));
        assertTrue(mListView.isItemChecked(4));

        mListView.clearChoices();
        assertFalse(array.get(1));
        assertFalse(array.get(2));
        assertFalse(array.get(3));
        assertFalse(array.get(4));
        assertFalse(mListView.isItemChecked(1));
        assertFalse(mListView.isItemChecked(2));
        assertFalse(mListView.isItemChecked(3));
        assertFalse(mListView.isItemChecked(4));
    }

    public void testAccessFooterView() {
        final TextView footerView1 = new TextView(mActivity);
        footerView1.setText("footerview1");
        final TextView footerView2 = new TextView(mActivity);
        footerView2.setText("footerview2");

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setFooterDividersEnabled(true);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(0, mListView.getFooterViewsCount());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.addFooterView(footerView1, null, true);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(1, mListView.getFooterViewsCount());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.addFooterView(footerView2);
            }
        });

        mInstrumentation.waitForIdleSync();
        assertEquals(2, mListView.getFooterViewsCount());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
            }
        });
        mInstrumentation.waitForIdleSync();

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.removeFooterView(footerView1);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(1, mListView.getFooterViewsCount());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.removeFooterView(footerView2);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(0, mListView.getFooterViewsCount());
    }

    public void testAccessHeaderView() {
        final TextView headerView1 = (TextView) mActivity.findViewById(R.id.headerview1);
        final TextView headerView2 = (TextView) mActivity.findViewById(R.id.headerview2);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setHeaderDividersEnabled(true);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(0, mListView.getHeaderViewsCount());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.addHeaderView(headerView2, null, true);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(1, mListView.getHeaderViewsCount());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.addHeaderView(headerView1);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(2, mListView.getHeaderViewsCount());
    }

    public void testAccessDivider() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
            }
        });
        mInstrumentation.waitForIdleSync();

        Drawable defaultDrawable = mListView.getDivider();
        final Rect r = defaultDrawable.getBounds();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return r.bottom - r.top > 0;
            }
        }.run();

        final Drawable d = mActivity.getResources().getDrawable(R.drawable.scenery);

        Rect r2 = d.getBounds();
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setDivider(d);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertSame(d, mListView.getDivider());
        assertEquals(r2.bottom - r2.top, mListView.getDividerHeight());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setDividerHeight(10);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals(10, mListView.getDividerHeight());
        assertEquals(10, r2.bottom - r2.top);
    }

    public void testSetSelection() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
            }
        });
        mInstrumentation.waitForIdleSync();

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setSelection(1);
            }
        });
        mInstrumentation.waitForIdleSync();
        String item = (String) mListView.getSelectedItem();
        assertEquals(mCountryList[1], item);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setSelectionFromTop(5, 0);
            }
        });
        mInstrumentation.waitForIdleSync();
        item = (String) mListView.getSelectedItem();
        assertEquals(mCountryList[5], item);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setSelectionAfterHeaderView();
            }
        });
        mInstrumentation.waitForIdleSync();
        item = (String) mListView.getSelectedItem();
        assertEquals(mCountryList[0], item);
    }

    public void testOnKeyUpDown() {
        // implementation details, do NOT test
    }

    public void testPerformItemClick() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
            }
        });
        mInstrumentation.waitForIdleSync();

        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setSelection(2);
            }
        });
        mInstrumentation.waitForIdleSync();

        final TextView child = (TextView) mAdapter_countries.getView(2, null, mListView);
        assertNotNull(child);
        assertEquals(mCountryList[2], child.getText().toString());
        final long itemID = mAdapter_countries.getItemId(2);
        assertEquals(2, itemID);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.performItemClick(child, 2, itemID);
            }
        });
        mInstrumentation.waitForIdleSync();

        MockOnItemClickListener onClickListener = new MockOnItemClickListener();
        mListView.setOnItemClickListener(onClickListener);

        assertNull(onClickListener.getView());
        assertEquals(0, onClickListener.getPosition());
        assertEquals(0, onClickListener.getID());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.performItemClick(child, 2, itemID);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertSame(child, onClickListener.getView());
        assertEquals(2, onClickListener.getPosition());
        assertEquals(2, onClickListener.getID());
    }

    public void testSaveAndRestoreInstanceState() {
        // implementation details, do NOT test
    }

    public void testDispatchKeyEvent() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
                mListView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(mListView.hasFocus());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setSelection(1);
            }
        });
        mInstrumentation.waitForIdleSync();
        String item = (String) mListView.getSelectedItem();
        assertEquals(mCountryList[1], item);

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);
                mListView.dispatchKeyEvent(keyEvent);
            }
        });
        mInstrumentation.waitForIdleSync();

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
                mListView.dispatchKeyEvent(keyEvent);
                mListView.dispatchKeyEvent(keyEvent);
                mListView.dispatchKeyEvent(keyEvent);
            }
        });
        mInstrumentation.waitForIdleSync();
        item = (String)mListView.getSelectedItem();
        assertEquals(mCountryList[4], item);
    }

    public void testRequestChildRectangleOnScreen() {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mListView.setAdapter(mAdapter_countries);
            }
        });
        mInstrumentation.waitForIdleSync();

        TextView child = (TextView) mAdapter_countries.getView(0, null, mListView);
        assertNotNull(child);
        assertEquals(mCountryList[0], child.getText().toString());

        Rect rect = new Rect(0, 0, 10, 10);
        assertFalse(mListView.requestChildRectangleOnScreen(child, rect, false));

        // TODO: how to check?
    }

    public void testOnTouchEvent() {
        // implementation details, do NOT test
    }

    @UiThreadTest
    public void testCanAnimate() {
        MyListView listView = new MyListView(mActivity, mAttributeSet);

        assertFalse(listView.canAnimate());
        listView.setAdapter(mAdapter_countries);
        assertFalse(listView.canAnimate());

        LayoutAnimationController controller = new LayoutAnimationController(
                mActivity, mAttributeSet);
        listView.setLayoutAnimation(controller);

        assertTrue(listView.canAnimate());
    }

    @UiThreadTest
    public void testDispatchDraw() {
        // implementation details, do NOT test
    }

    @UiThreadTest
    public void testFindViewTraversal() {
        MyListView listView = new MyListView(mActivity, mAttributeSet);
        TextView headerView = (TextView) mActivity.findViewById(R.id.headerview1);

        assertNull(listView.findViewTraversal(R.id.headerview1));

        listView.addHeaderView(headerView);
        assertNotNull(listView.findViewTraversal(R.id.headerview1));
        assertSame(headerView, listView.findViewTraversal(R.id.headerview1));
    }

    @UiThreadTest
    public void testFindViewWithTagTraversal() {
        MyListView listView = new MyListView(mActivity, mAttributeSet);
        TextView headerView = (TextView) mActivity.findViewById(R.id.headerview1);

        assertNull(listView.findViewWithTagTraversal("header"));

        headerView.setTag("header");
        listView.addHeaderView(headerView);
        assertNotNull(listView.findViewWithTagTraversal("header"));
        assertSame(headerView, listView.findViewWithTagTraversal("header"));
    }

    public void testLayoutChildren() {
        // TODO: how to test?
    }

    public void testOnFinishInflate() {
        // implementation details, do NOT test
    }

    public void testOnFocusChanged() {
        // implementation details, do NOT test
    }

    public void testOnMeasure() {
        // implementation details, do NOT test
    }

    /**
     * MyListView for test
     */
    private static class MyListView extends ListView {
        public MyListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected boolean canAnimate() {
            return super.canAnimate();
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
        }

        @Override
        protected View findViewTraversal(int id) {
            return super.findViewTraversal(id);
        }

        @Override
        protected View findViewWithTagTraversal(Object tag) {
            return super.findViewWithTagTraversal(tag);
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
        }
    }

    private static class MockOnItemClickListener implements OnItemClickListener {
        private View mView;
        private int mPosition;
        private long mID;

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mView = view;
            mPosition = position;
            mID = id;
        }

        public View getView() {
            return mView;
        }

        public int getPosition() {
            return mPosition;
        }

        public long getID() {
            return mID;
        }
    }

    /**
     * The following functions are merged from frameworktest.
     */
    @MediumTest
    public void testRequestLayout() throws Exception {
        ListView listView = new ListView(mActivity);
        List<String> items = Lists.newArrayList("hello");
        Adapter<String> adapter = new Adapter<String>(mActivity, 0, items);
        listView.setAdapter(adapter);

        int measureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);

        adapter.notifyDataSetChanged();
        listView.measure(measureSpec, measureSpec);
        listView.layout(0, 0, 100, 100);

        MockView childView = (MockView) listView.getChildAt(0);

        childView.requestLayout();
        childView.onMeasureCalled = false;
        listView.measure(measureSpec, measureSpec);
        listView.layout(0, 0, 100, 100);
        Assert.assertTrue(childView.onMeasureCalled);
    }

    @MediumTest
    public void testNoSelectableItems() throws Exception {
        ListView listView = new ListView(mActivity);
        // We use a header as the unselectable item to remain after the selectable one is removed.
        listView.addHeaderView(new View(mActivity), null, false);
        List<String> items = Lists.newArrayList("hello");
        Adapter<String> adapter = new Adapter<String>(mActivity, 0, items);
        listView.setAdapter(adapter);

        listView.setSelection(1);

        int measureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);

        adapter.notifyDataSetChanged();
        listView.measure(measureSpec, measureSpec);
        listView.layout(0, 0, 100, 100);

        items.remove(0);

        adapter.notifyDataSetChanged();
        listView.measure(measureSpec, measureSpec);
        listView.layout(0, 0, 100, 100);
    }

    private class MockView extends View {

        public boolean onMeasureCalled = false;

        public MockView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            onMeasureCalled = true;
        }
    }

    private class Adapter<T> extends ArrayAdapter<T> {

        public Adapter(Context context, int resource, List<T> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return new MockView(getContext());
        }
    }

    public void testTransientStateUnstableIds() throws Exception {
        final ListView listView = mListView;
        final ArrayList<String> items = new ArrayList<String>(Arrays.asList(mCountryList));
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_list_item_1, items);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                listView.setAdapter(adapter);
            }
        });
        mInstrumentation.waitForIdleSync();

        final View oldItem = listView.getChildAt(2);
        final CharSequence oldText = ((TextView) oldItem.findViewById(android.R.id.text1))
                .getText();
        oldItem.setHasTransientState(true);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                adapter.remove(adapter.getItem(0));
                adapter.notifyDataSetChanged();
            }
        });
        mInstrumentation.waitForIdleSync();

        final View newItem = listView.getChildAt(2);
        final CharSequence newText = ((TextView) newItem.findViewById(android.R.id.text1))
                .getText();

        Assert.assertFalse(oldText.equals(newText));
    }

    public void testTransientStateStableIds() throws Exception {
        final ListView listView = mListView;
        final ArrayList<String> items = new ArrayList<String>(Arrays.asList(mCountryList));
        final StableArrayAdapter<String> adapter = new StableArrayAdapter<String>(mActivity,
                android.R.layout.simple_list_item_1, items);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                listView.setAdapter(adapter);
            }
        });
        mInstrumentation.waitForIdleSync();

        final Object tag = new Object();
        final View oldItem = listView.getChildAt(2);
        final CharSequence oldText = ((TextView) oldItem.findViewById(android.R.id.text1))
                .getText();
        oldItem.setHasTransientState(true);
        oldItem.setTag(tag);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                adapter.remove(adapter.getItem(0));
                adapter.notifyDataSetChanged();
            }
        });
        mInstrumentation.waitForIdleSync();

        final View newItem = listView.getChildAt(1);
        final CharSequence newText = ((TextView) newItem.findViewById(android.R.id.text1))
                .getText();

        Assert.assertTrue(newItem.hasTransientState());
        Assert.assertEquals(oldText, newText);
        Assert.assertEquals(tag, newItem.getTag());
    }

    private static class StableArrayAdapter<T> extends ArrayAdapter<T> {
        public StableArrayAdapter(Context context, int resource, List<T> objects) {
            super(context, resource, objects);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}

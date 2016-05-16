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

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.android.cts.stub.R;


public class AdapterViewTest extends ActivityInstrumentationTestCase2<AdapterViewStubActivity> {

    private final static int INVALID_ID = -1;

    private final static int LAYOUT_WIDTH = 200;
    private final static int LAYOUT_HEIGHT = 200;

    final String[] FRUIT = { "1", "2", "3", "4", "5", "6", "7", "8" };

    private Activity mActivity;
    private AdapterView<ListAdapter> mAdapterView;

    public AdapterViewTest() {
        super("com.android.cts.stub", AdapterViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mAdapterView = new ListView(mActivity);
    }

    public void testConstructor() {
        XmlPullParser parser = mActivity.getResources().getXml(R.layout.adapterview_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        new MockAdapterView(mActivity);

        new MockAdapterView(mActivity, attrs);

        new MockAdapterView(mActivity, attrs, 0);

        try {
            new MockAdapterView(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            //expected
        }

        new MockAdapterView(mActivity, null, INVALID_ID);
    }

    /**
     * test not supported methods, all should throw UnsupportedOperationException
     */
    public void testUnsupportedMethods() {
        ListView subView = new ListView(mActivity);

        try {
            mAdapterView.addView(subView);
            fail("addView(View) is not supported in AdapterView.");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            mAdapterView.addView(subView, 0);
            fail("addView(View, int) is not supported in AdapterView.");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            mAdapterView.addView(subView, (ViewGroup.LayoutParams) null);
            fail("addView(View, ViewGroup.LayoutParams) is not supported in AdapterView.");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            mAdapterView.addView(subView, 0, (ViewGroup.LayoutParams) null);
            fail("addView(View, int, ViewGroup.LayoutParams) is not supported in AdapterView.");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            mAdapterView.removeViewAt(0);
            fail("removeViewAt(int) is not supported in AdapterView");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            mAdapterView.removeAllViews();
            fail("removeAllViews() is not supported in AdapterView");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            mAdapterView.removeView(subView);
            fail("removeView(View) is not supported in AdapterView");
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            mAdapterView.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(View v) {
                }
            });
            fail("function setOnClickListener(android.view.View.OnClickListener) "
                    + "should throw out runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
    }

    public void testGetCount() {
        // Before setAdapter, the count should be zero.
        assertEquals(0, mAdapterView.getCount());

        setArrayAdapter(mAdapterView);

        // After setAdapter, the count should be the value return by adapter.
        assertEquals(FRUIT.length, mAdapterView.getCount());
    }

    public void testAccessEmptyView() {
        ImageView emptyView = new ImageView(mActivity);

        // If there is no adapter has been set, emptyView hasn't been set, there will be no
        // emptyView return by getEmptyView().
        assertEquals(null, mAdapterView.getEmptyView());

        // If the adapter is 0 count, emptyView has been set, the emptyView should be returned by
        // getEmptyView. EmptyView will be set to Visible.
        mAdapterView.setAdapter(new ArrayAdapter<String>(
                mActivity, R.layout.adapterview_layout, new String[]{}));
        emptyView.setVisibility(View.INVISIBLE);
        assertEquals(View.INVISIBLE, emptyView.getVisibility());

        // set empty view, for no item added, empty set to visible
        mAdapterView.setEmptyView(emptyView);
        assertSame(emptyView, mAdapterView.getEmptyView());
        assertEquals(View.VISIBLE, emptyView.getVisibility());

        // If the adapter is not empty, the emptyView should also be returned by
        // getEmptyView. EmptyView will be set to Gone.
        setArrayAdapter(mAdapterView);
        emptyView = new ImageView(mActivity);

        assertEquals(View.VISIBLE, emptyView.getVisibility());
        mAdapterView.setEmptyView(emptyView);
        // for item added, emptyview is set to gone
        assertEquals(emptyView, mAdapterView.getEmptyView());
        assertEquals(View.GONE, emptyView.getVisibility());

        // null adapter should also show empty view
        mAdapterView.setAdapter(null);
        emptyView = new ImageView(mActivity);
        emptyView.setVisibility(View.INVISIBLE);
        assertEquals(View.INVISIBLE, emptyView.getVisibility());
        // set empty view
        mAdapterView.setEmptyView(emptyView);
        assertEquals(emptyView, mAdapterView.getEmptyView());
        assertEquals(View.VISIBLE, emptyView.getVisibility());
    }

    public void testAccessVisiblePosition() {

        assertEquals(0, mAdapterView.getFirstVisiblePosition());
        // If no adapter has been set, the value should be -1;
        assertEquals(-1, mAdapterView.getLastVisiblePosition());

        setArrayAdapter(mAdapterView);

        // LastVisiblePosition should be adapter's getCount - 1,by mocking method
        float density = mActivity.getResources().getDisplayMetrics().density;
        int bottom = (int) (LAYOUT_HEIGHT * density);
        mAdapterView.layout(0, 0, LAYOUT_WIDTH, bottom);
        assertEquals(FRUIT.length - 1, mAdapterView.getLastVisiblePosition());
    }

    public void testItemOrItemIdAtPosition() {
        // no adapter set
        assertNull(mAdapterView.getItemAtPosition(0));
        assertEquals(AdapterView.INVALID_ROW_ID, mAdapterView.getItemIdAtPosition(1));

        // after adapter set
        setArrayAdapter(mAdapterView);
        int count = mAdapterView.getAdapter().getCount();

        for (int i = 0; i < count; i++) {
            assertEquals(FRUIT[i], mAdapterView.getItemAtPosition(i));
        }
        assertNull(mAdapterView.getItemAtPosition(-1));
        try {
            mAdapterView.getItemAtPosition(FRUIT.length);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            //expected
        }

        for (int i = 0; i < count; i++) {
            assertEquals(i, mAdapterView.getItemIdAtPosition(i));
        }
        assertEquals(AdapterView.INVALID_ROW_ID, mAdapterView.getItemIdAtPosition(-1));
        assertEquals(FRUIT.length, mAdapterView.getItemIdAtPosition(FRUIT.length));
    }

    public void testAccessOnItemClickAndLongClickListener() {
        MockOnItemClickListener clickListener = new MockOnItemClickListener();
        MockOnItemLongClickListener longClickListener = new MockOnItemLongClickListener();

        assertFalse(mAdapterView.performItemClick(null, 0, 0));

        mAdapterView.setOnItemClickListener(clickListener);
        mAdapterView.setOnItemLongClickListener(longClickListener);

        assertFalse(clickListener.isClicked());
        assertTrue(mAdapterView.performItemClick(null, 0, 0));
        assertTrue(clickListener.isClicked());

        setArrayAdapter(mAdapterView);
        assertFalse(longClickListener.isClicked());
        mAdapterView.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);
        assertTrue(mAdapterView.showContextMenuForChild(mAdapterView.getChildAt(0)));
        assertTrue(longClickListener.isClicked());
    }

    public void testAccessOnItemSelectedListener() {
        // FIXME: we can not select the item in touch mode, how can we change the mode to test
        setArrayAdapter(mAdapterView);
        MockOnItemSelectedListener selectedListener = new MockOnItemSelectedListener();
        mAdapterView.setOnItemSelectedListener(selectedListener);

//        mAdapterView.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);
//
//        assertFalse(selectedListener.isItemSelected());
//        assertFalse(selectedListener.isNothingSelected());
//
//        mAdapterView.setSelection(1);
//        assertTrue(selectedListener.isItemSelected());
//        assertFalse(selectedListener.isNothingSelected());
//
//        mAdapterView.setSelection(-1);
//        assertTrue(selectedListener.isItemSelected());
//        assertTrue(selectedListener.isNothingSelected());
//
//        mAdapterView.setSelection(FRUIT.length);
//        assertTrue(selectedListener.isItemSelected());
//        assertTrue(selectedListener.isNothingSelected());
    }

    /*
     * Get the position within the adapter's data set for the view, where view is a an adapter item
     * or a descendant of an adapter item.
     * when scroll down the list, the item's position may be 5 or 6 be on the screen
     * but to the layout parent ,it may still be the 1, 2 child for there always has 3,4 views there
     * it's hard to scroll the list in unit test, so we just test without scrolling
     * this means the position of item is same as position of the children in parent layout
     */
    public void testGetPositionForView() {
        setArrayAdapter(mAdapterView);
        mAdapterView.layout(0, 0, LAYOUT_WIDTH, LAYOUT_HEIGHT);

        int count = mAdapterView.getChildCount();
        for (int i = 0; i < count; i++) {
            assertEquals(i, mAdapterView.getPositionForView(mAdapterView.getChildAt(i)));
        }

        try {
            assertEquals(AdapterView.INVALID_POSITION, mAdapterView.getPositionForView(null));
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            //expected
        }

        try {
            assertEquals(AdapterView.INVALID_POSITION,
                    mAdapterView.getPositionForView(new ImageView(mActivity)));
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            //expected
        }
    }

    public void testChangeFocusable() {
        assertFalse(mAdapterView.isFocusable());
        assertFalse(mAdapterView.isFocusableInTouchMode());

        // no item added will never focusable
        assertNull(mAdapterView.getAdapter());
        mAdapterView.setFocusable(true);
        assertFalse(mAdapterView.isFocusable());
        assertFalse(mAdapterView.isFocusableInTouchMode());

        // only focusable with children added
        setArrayAdapter(mAdapterView);
        assertTrue(mAdapterView.getAdapter().getCount() > 0);
        mAdapterView.setFocusable(true);
        assertTrue(mAdapterView.isFocusable());
        assertTrue(mAdapterView.isFocusableInTouchMode());

        mAdapterView.setFocusable(false);
        assertFalse(mAdapterView.isFocusable());
        assertFalse(mAdapterView.isFocusableInTouchMode());
    }

    /*
     * skip this test, no need to test
     */
    public void testOnLayout() {
        // onLayout() is implementation details, do NOT test
    }

    /*
     * set and get the selected id, position and item.
     * values will not change if invalid id given.
     */
    public void testGetSelected() {
        assertEquals(AdapterView.INVALID_ROW_ID, mAdapterView.getSelectedItemId());
        assertEquals(AdapterView.INVALID_POSITION, mAdapterView.getSelectedItemPosition());
        assertEquals(null, mAdapterView.getSelectedItem());

        // set adapter, 0 selected by default
        setArrayAdapter(mAdapterView);
        assertEquals(0, mAdapterView.getSelectedItemId());
        assertEquals(0, mAdapterView.getSelectedItemPosition());
        assertEquals(FRUIT[0], mAdapterView.getSelectedItem());

        int expectedId = 1;
        mAdapterView.setSelection(expectedId);
        assertEquals(expectedId, mAdapterView.getSelectedItemId());
        assertEquals(expectedId, mAdapterView.getSelectedItemPosition());
        assertEquals(FRUIT[expectedId], mAdapterView.getSelectedItem());

        // invalid id will be ignored
        expectedId = -1;
        mAdapterView.setSelection(expectedId);
        assertEquals(1, mAdapterView.getSelectedItemId());
        assertEquals(1, mAdapterView.getSelectedItemPosition());
        assertEquals(FRUIT[1], mAdapterView.getSelectedItem());

        expectedId = mAdapterView.getCount();
        mAdapterView.setSelection(expectedId);
        assertEquals(1, mAdapterView.getSelectedItemId());
        assertEquals(1, mAdapterView.getSelectedItemPosition());
        assertEquals(FRUIT[1], mAdapterView.getSelectedItem());
    }

    /*
     * not update this test until the ViewGroup's test finish.
     */
    public void testDispatchSaveInstanceState() {
        MockAdapterView adapterView = new MockAdapterView(mActivity);
        adapterView.setSaveEnabled(true);
        adapterView.setId(1);
        SparseArray<Parcelable> sa = new SparseArray<Parcelable>();
        adapterView.dispatchSaveInstanceState(sa);
        assertTrue(sa.size() > 0);
    }

    /*
     * not update this test until the ViewGroup's test finish.
     */
    public void testDispatchRestoreInstanceState() {
        MockAdapterView adapterView = new MockAdapterView(mActivity);
        adapterView.setSaveEnabled(true);
        adapterView.setId(1);
        SparseArray<Parcelable> sparseArray = new SparseArray<Parcelable>();
        adapterView.dispatchRestoreInstanceState(sparseArray);
    }

    /*
     * whether this view can has animation layout
     * if no child added, it always return false
     * this method is protected, so we involve the mock
     */
    public void testCanAnimate() {
        MockAdapterView adapterView = new MockAdapterView(mActivity);
        LayoutAnimationController lAC = new LayoutAnimationController(new AnimationSet(true));

            // no child added, always false
        assertNull(adapterView.getAdapter());
        adapterView.setLayoutAnimation(lAC);
        assertFalse(adapterView.canAnimate());

        setArrayAdapter(adapterView);

        assertTrue(adapterView.getAdapter().getCount() > 0);
        assertTrue(adapterView.canAnimate());
    }

    private static class MockAdapterView extends ListView{

        public MockAdapterView(Context context) {
            super(context);
        }

        public MockAdapterView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MockAdapterView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
            super.dispatchRestoreInstanceState(container);
        }

        @Override
        protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
            super.dispatchSaveInstanceState(container);
        }

        @Override
        protected boolean canAnimate() {
            return super.canAnimate();
        }
    }

    private void setArrayAdapter(AdapterView<ListAdapter> adapterView) {
        ((ListView) adapterView).setAdapter(new ArrayAdapter<String>(
                mActivity, R.layout.adapterview_layout, FRUIT));
    }

    /**
     * this is a mock item click listener for check out call back
     */
    private class MockOnItemClickListener implements OnItemClickListener {
        private boolean mClicked;

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mClicked = true;
        }

        protected boolean isClicked() {
            return mClicked;
        }
    }

    /**
     * this is a mock long item click listener for check out call back
     */
    private class MockOnItemLongClickListener implements OnItemLongClickListener {
        private boolean mClicked;

        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            mClicked = true;
            return true;
        }

        protected boolean isClicked() {
            return mClicked;
        }
    }

    /**
     * this is a mock item selected listener for check out call lback
     */
    private class MockOnItemSelectedListener implements OnItemSelectedListener {
        private boolean mIsItemSelected;
        private boolean mIsNothingSelected;

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mIsItemSelected = true;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            mIsNothingSelected = true;
        }

        protected boolean isItemSelected() {
            return mIsItemSelected;
        }

        protected boolean isNothingSelected() {
            return mIsNothingSelected;
        }
    }
}

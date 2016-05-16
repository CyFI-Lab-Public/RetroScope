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

import com.android.internal.R;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.test.InstrumentationTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;
import android.widget.SimpleAdapter.ViewBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Test {@link SimpleAdapter}.
 */
public class SimpleAdapterTest extends InstrumentationTestCase {
    private static final int DEFAULT_ROW_COUNT = 20;

    private static final int DEFAULT_COLUMN_COUNT = 2;

    private static final int[] VIEWS_TO = new int[] { R.id.text1 };

    private static final String[] COLUMNS_FROM = new String[] { "column1" };

    /**
     * The original cursor and its content will be set to:
     * <TABLE>
     * <TR>
     * <TH>Column0</TH>
     * <TH>Column1</TH>
     * </TR>
     * <TR>
     * <TD>00</TD>
     * <TD>01</TD>
     * </TR>
     * <TR>
     * <TD>10</TD>
     * <TD>11</TD>
     * </TR>
     * <TR>
     * <TD>...</TD>
     * <TD>...</TD>
     * </TR>
     * <TR>
     * <TD>190</TD>
     * <TD>191</TD>
     * </TR>
     * </TABLE>
     * It has 2 columns and 20 rows. The default layout for item
     * is R.layout.simple_list_item_1
     */
    private SimpleAdapter mSimpleAdapter;

    private Context mContext;

    private LinearLayout mAdapterHost;

    private LayoutInflater mInflater;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdapterHost = (LinearLayout) mInflater.inflate(
                com.android.cts.stub.R.layout.cursoradapter_host, null);

        // new the SimpleAdapter instance
        mSimpleAdapter = new SimpleAdapter(mContext,
                createTestList(DEFAULT_COLUMN_COUNT, DEFAULT_ROW_COUNT),
                R.layout.simple_list_item_1, COLUMNS_FROM, VIEWS_TO);
    }

    public void testConstructor() {
        new SimpleAdapter(mContext, createTestList(DEFAULT_COLUMN_COUNT, DEFAULT_ROW_COUNT),
                R.layout.simple_list_item_1, COLUMNS_FROM, VIEWS_TO);
    }

    public void testGetCount() {
        mSimpleAdapter = new SimpleAdapter(mContext,
                createTestList(DEFAULT_COLUMN_COUNT, DEFAULT_ROW_COUNT),
                R.layout.simple_list_item_1, COLUMNS_FROM, VIEWS_TO);
        assertEquals(20, mSimpleAdapter.getCount());

        mSimpleAdapter = new SimpleAdapter(mContext, createTestList(DEFAULT_COLUMN_COUNT, 10),
                R.layout.simple_list_item_1, COLUMNS_FROM, VIEWS_TO);
        assertEquals(10, mSimpleAdapter.getCount());
    }

    public void testGetItem() {
        assertEquals("01", ((Map<?, ?>) mSimpleAdapter.getItem(0)).get("column1"));
        assertEquals("191", ((Map<?, ?>) mSimpleAdapter.getItem(19)).get("column1"));

        try {
            mSimpleAdapter.getItem(-1);
            fail("Should throw IndexOutOfBoundsException if index is negative");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            mSimpleAdapter.getItem(20);
            fail("Should throw IndexOutOfBoundsException if index is beyond the list's size");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testGetItemId() {
        assertEquals(0, mSimpleAdapter.getItemId(0));

        assertEquals(19, mSimpleAdapter.getItemId(19));

        // are the following behaviors correct?
        assertEquals(-1, mSimpleAdapter.getItemId(-1));

        assertEquals(20, mSimpleAdapter.getItemId(20));
    }

    public void testGetView() {
        // use the layout passed in to constructor
        View result = mSimpleAdapter.getView(0, null, mAdapterHost);
        assertTrue(result instanceof TextView);
        assertEquals("01", ((TextView) result).getText().toString());

        result = mSimpleAdapter.getView(19, null, mAdapterHost);
        assertTrue(result instanceof TextView);
        assertEquals("191", ((TextView) result).getText().toString());

        // use the previous result as the convert view
        // the param ViewGroup is never read
        TextView convertView = (TextView) result;
        result = mSimpleAdapter.getView(0, convertView, mAdapterHost);
        assertEquals("01", ((TextView) result).getText().toString());
        assertSame(convertView, result);

        // parent can be null
        result = mSimpleAdapter.getView(10, convertView, null);
        assertEquals("101", ((TextView) result).getText().toString());

        // the binder takes care of binding, the param ViewGroup is never readed
        MockViewBinder binder = new MockViewBinder(true);
        mSimpleAdapter.setViewBinder(binder);
        binder.reset();
        mSimpleAdapter.getView(0, null, mAdapterHost);
        assertTrue(binder.hasCalledSetViewValue());

        // binder try binding but fail
        binder = new MockViewBinder(false);
        mSimpleAdapter.setViewBinder(binder);
        binder.reset();
        result = mSimpleAdapter.getView(0, null, mAdapterHost);
        assertTrue(binder.hasCalledSetViewValue());
        assertEquals("01", ((TextView) result).getText().toString());

        try {
            mSimpleAdapter.getView(-1, convertView, null);
            fail("Should throw IndexOutOfBoundsException if index is negative");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            mSimpleAdapter.getView(20, convertView, null);
            fail("Should throw IndexOutOfBoundsException if index is beyond the list's size");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testSetDropDownViewResource() {
        mSimpleAdapter.setDropDownViewResource(R.layout.simple_list_item_2);
        View result = mSimpleAdapter.getDropDownView(0, null, mAdapterHost);
        assertTrue(result instanceof TwoLineListItem);
        assertEquals("01", ((TextView) result.findViewById(R.id.text1)).getText().toString());

        result = mSimpleAdapter.getDropDownView(19, null, mAdapterHost);
        assertTrue(result instanceof TwoLineListItem);
        assertEquals("191", ((TextView) result.findViewById(R.id.text1)).getText().toString());

        mSimpleAdapter.setDropDownViewResource(R.layout.simple_list_item_1);
        result = mSimpleAdapter.getDropDownView(0, null, mAdapterHost);
        assertTrue(result instanceof TextView);
        assertEquals("01", ((TextView) result).getText().toString());

        result = mSimpleAdapter.getDropDownView(19, null, mAdapterHost);
        assertTrue(result instanceof TextView);
        assertEquals("191", ((TextView) result).getText().toString());
    }

    public void testGetDropDownView() {
        View result = mSimpleAdapter.getDropDownView(0, null, mAdapterHost);
        assertTrue(result instanceof TextView);
        assertEquals("01", ((TextView) result).getText().toString());

        result = mSimpleAdapter.getDropDownView(19, null, mAdapterHost);
        assertTrue(result instanceof TextView);
        assertEquals("191", ((TextView) result).getText().toString());

        // use the previous result as the convert view
        TextView convertView = (TextView) result;
        // the param ViewGroup is never read
        result = mSimpleAdapter.getDropDownView(0, convertView, mAdapterHost);
        assertEquals("01", convertView.getText().toString());
        assertSame(convertView, result);

        // The parent can be null
        result = mSimpleAdapter.getDropDownView(10, convertView, null);
        assertEquals("101", ((TextView) result).getText().toString());

        // the binder takes care of binding, the param ViewGroup is never readed
        MockViewBinder binder = new MockViewBinder(true);
        mSimpleAdapter.setViewBinder(binder);
        binder.reset();
        mSimpleAdapter.getDropDownView(19, null, mAdapterHost);
        assertTrue(binder.hasCalledSetViewValue());

        // binder try binding but fail
        binder = new MockViewBinder(false);
        mSimpleAdapter.setViewBinder(binder);
        binder.reset();
        result = mSimpleAdapter.getDropDownView(19, null, mAdapterHost);
        assertTrue(binder.hasCalledSetViewValue());
        assertEquals("191", ((TextView)result).getText().toString());

        try {
            mSimpleAdapter.getDropDownView(-1, convertView, null);
            fail("Should throw IndexOutOfBoundsException if index is negative");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            mSimpleAdapter.getDropDownView(20, convertView, null);
            fail("Should throw IndexOutOfBoundsException if index is beyond the list's size");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testAccessViewBinder() {
        // no binder default
        assertNull(mSimpleAdapter.getViewBinder());

        // binder takes care of binding
        MockViewBinder binder = new MockViewBinder(true);
        mSimpleAdapter.setViewBinder(binder);
        assertSame(binder, mSimpleAdapter.getViewBinder());

        // binder try binding but fail
        binder = new MockViewBinder(false);
        mSimpleAdapter.setViewBinder(binder);
        assertSame(binder, mSimpleAdapter.getViewBinder());

        mSimpleAdapter.setViewBinder(null);
        assertNull(mSimpleAdapter.getViewBinder());
    }

    public void testSetViewImage() {
        // String represents resId
        ImageView view = new ImageView(mContext);
        assertNull(view.getDrawable());
        mSimpleAdapter.setViewImage(view, String.valueOf(com.android.cts.stub.R.drawable.scenery));
        BitmapDrawable d = (BitmapDrawable) mContext.getResources().getDrawable(
                com.android.cts.stub.R.drawable.scenery);
        WidgetTestUtils.assertEquals(d.getBitmap(),
                ((BitmapDrawable) view.getDrawable()).getBitmap());

        // blank
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        mSimpleAdapter.setViewImage(view, "");
        assertNull(view.getDrawable());

        // null
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        try {
            // Should declare NullPoinertException if the uri or value is null
            mSimpleAdapter.setViewImage(view, null);
            fail("Should throw NullPointerException if the uri or value is null");
        } catch (NullPointerException e) {
        }

        // resId
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        mSimpleAdapter.setViewImage(view, com.android.cts.stub.R.drawable.scenery);
        d = (BitmapDrawable) mContext.getResources()
                .getDrawable(com.android.cts.stub.R.drawable.scenery);
        WidgetTestUtils.assertEquals(d.getBitmap(),
                ((BitmapDrawable) view.getDrawable()).getBitmap());

        // illegal resid
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        mSimpleAdapter.setViewImage(view, Integer.MAX_VALUE);
        assertNull(view.getDrawable());

        // uri
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        try {
            mSimpleAdapter.setViewImage(view, SimpleCursorAdapterTest.createTestImage(mContext,
                    "testimage", com.android.cts.stub.R.raw.testimage));
            assertNotNull(view.getDrawable());
            Bitmap actualBitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
            Bitmap testBitmap = WidgetTestUtils.getUnscaledAndDitheredBitmap(mContext.getResources(),
                    com.android.cts.stub.R.raw.testimage, actualBitmap.getConfig());
            WidgetTestUtils.assertEquals(testBitmap, actualBitmap);
        } finally {
            SimpleCursorAdapterTest.destroyTestImage(mContext,"testimage");
        }
    }

    public void testSetViewText() {
        TextView view = new TextView(mContext);
        mSimpleAdapter.setViewText(view, "expected");
        assertEquals("expected", view.getText().toString());

        mSimpleAdapter.setViewText(view, null);
        assertEquals("", view.getText().toString());
    }

    public void testGetFilter() {
        assertNotNull(mSimpleAdapter.getFilter());
    }

    /**
     * Creates the test list.
     *
     * @param colCount the column count
     * @param rowCount the row count
     * @return the array list< hash map< string, string>>, Example:if the
     *         colCount = 2 and rowCount = 3, the list will be { {column0=>00,
     *         column1=>01}, {column0=>10, column1=>11}, {column0=>20,
     *         column1=>21} }
     */
    private ArrayList<HashMap<String, String>> createTestList(int colCount, int rowCount) {
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        String[] columns = new String[colCount];
        for (int i = 0; i < colCount; i++) {
            columns[i] = "column" + i;
        }

        for (int i = 0; i < rowCount; i++) {
            HashMap<String, String> row = new HashMap<String, String>();
            for (int j = 0; j < colCount; j++) {
                row.put(columns[j], "" + i + "" + j);
            }
            list.add(row);
        }

        return list;
    }

    private class MockViewBinder implements ViewBinder {
        private boolean mExpectedResult;

        private boolean mHasCalledSetViewValue;

        public MockViewBinder(boolean expectedResult) {
            mExpectedResult = expectedResult;
        }

        public void reset(){
            mHasCalledSetViewValue = false;
        }

        public boolean hasCalledSetViewValue() {
            return mHasCalledSetViewValue;
        }

        public boolean setViewValue(View view, Object data, String textRepresentation) {
            mHasCalledSetViewValue = true;
            return mExpectedResult;
        }
    }
}

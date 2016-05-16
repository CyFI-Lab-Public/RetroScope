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


import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

/**
 * Test {@link SimpleCursorTreeAdapter}.
 */
public class SimpleCursorTreeAdapterTest extends InstrumentationTestCase {
    private static final int GROUP_LAYOUT = R.layout.cursoradapter_group0;

    private static final int CHILD_LAYOUT = R.layout.cursoradapter_item0;

    private static final String[] COLUMNS_CHILD_FROM = new String[] {
        "column2"
    };

    private static final String[] COLUMNS_GROUP_FROM = new String[] {
        "column1"
    };

    private static final int[] VIEWS_GROUP_TO = new int[] {
        R.id.cursorAdapter_group0
    };

    private static final int[] VIEWS_CHILD_TO = new int[] {
        R.id.cursorAdapter_item0
    };

    private static final String SAMPLE_IMAGE_NAME = "testimage.jpg";

    private MockSimpleCursorTreeAdapter mSimpleCursorTreeAdapter;

    private Context mContext;

    private Cursor mGroupCursor;

    private Cursor mChildCursor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        mGroupCursor = createTestCursor(2, 20, "group");
        new MockSimpleCursorTreeAdapter(mContext, mGroupCursor,
                GROUP_LAYOUT, COLUMNS_GROUP_FROM, VIEWS_GROUP_TO,
                CHILD_LAYOUT, COLUMNS_CHILD_FROM, VIEWS_CHILD_TO);

        new MockSimpleCursorTreeAdapter(mContext, mGroupCursor,
                GROUP_LAYOUT, GROUP_LAYOUT, COLUMNS_GROUP_FROM,
                VIEWS_GROUP_TO, CHILD_LAYOUT, COLUMNS_CHILD_FROM, VIEWS_CHILD_TO);

        new MockSimpleCursorTreeAdapter(mContext, mGroupCursor,
                GROUP_LAYOUT, GROUP_LAYOUT, COLUMNS_GROUP_FROM, VIEWS_GROUP_TO,
                CHILD_LAYOUT, CHILD_LAYOUT, COLUMNS_CHILD_FROM, VIEWS_CHILD_TO);
    }

    public void testBindChildView() {
        mGroupCursor = createTestCursor(2, 20, "group");
        mChildCursor = createTestCursor(3, 4, "child");
        mChildCursor.moveToFirst();
        mSimpleCursorTreeAdapter = new MockSimpleCursorTreeAdapter(mContext, mGroupCursor,
                GROUP_LAYOUT, COLUMNS_GROUP_FROM, VIEWS_GROUP_TO,
                CHILD_LAYOUT, COLUMNS_CHILD_FROM, VIEWS_CHILD_TO);

        TextView view = new TextView(mContext);
        view.setId(R.id.cursorAdapter_item0);
        mSimpleCursorTreeAdapter.bindChildView(view, null, mChildCursor, true);
        assertEquals("child02", view.getText().toString());

        mChildCursor.moveToNext();
        mSimpleCursorTreeAdapter.bindChildView(view, null, mChildCursor, false);
        assertEquals("child12", view.getText().toString());
    }

    // The param context and isExpanded is never readed.
    public void testBindGroupView() {
        mGroupCursor = createTestCursor(2, 20, "group");
        mGroupCursor.moveToFirst();
        mSimpleCursorTreeAdapter = new MockSimpleCursorTreeAdapter(mContext, mGroupCursor,
                GROUP_LAYOUT, COLUMNS_GROUP_FROM, VIEWS_GROUP_TO,
                CHILD_LAYOUT, COLUMNS_CHILD_FROM, VIEWS_CHILD_TO);
        TextView view = new TextView(mContext);
        view.setId(R.id.cursorAdapter_group0);
        mSimpleCursorTreeAdapter.bindGroupView(view, null, mGroupCursor, true);
        assertEquals("group01", view.getText().toString());

        mGroupCursor.moveToNext();
        mSimpleCursorTreeAdapter.bindGroupView(view, null, mGroupCursor, false);
        assertEquals("group11", view.getText().toString());
    }

    public void testSetViewImage() {
        mGroupCursor = createTestCursor(2, 20, "group");
        mSimpleCursorTreeAdapter = new MockSimpleCursorTreeAdapter(mContext, mGroupCursor,
                GROUP_LAYOUT, COLUMNS_GROUP_FROM, VIEWS_GROUP_TO,
                CHILD_LAYOUT, COLUMNS_CHILD_FROM, VIEWS_CHILD_TO);

        // color drawable
        ImageView view = new ImageView(mContext);
        assertNull(view.getDrawable());
        mSimpleCursorTreeAdapter.setViewImage(view,
                String.valueOf(com.android.cts.stub.R.drawable.scenery));
        BitmapDrawable d = (BitmapDrawable) mContext.getResources().getDrawable(
                com.android.cts.stub.R.drawable.scenery);
        WidgetTestUtils.assertEquals(d.getBitmap(),
                ((BitmapDrawable) view.getDrawable()).getBitmap());

        // blank
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        mSimpleCursorTreeAdapter.setViewImage(view, "");
        assertNull(view.getDrawable());

        // null
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        try {
            // Should declare NullPoinertException if the uri or value is null
            mSimpleCursorTreeAdapter.setViewImage(view, null);
            fail("Should throw NullPointerException if the uri or value is null");
        } catch (NullPointerException e) {
        }

        // uri
        view = new ImageView(mContext);
        assertNull(view.getDrawable());
        try {
            mSimpleCursorTreeAdapter.setViewImage(view,
                    SimpleCursorAdapterTest.createTestImage(mContext, SAMPLE_IMAGE_NAME,
                            com.android.cts.stub.R.raw.testimage));
            Bitmap actualBitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
            Bitmap test = WidgetTestUtils.getUnscaledAndDitheredBitmap(mContext.getResources(),
                    com.android.cts.stub.R.raw.testimage, actualBitmap.getConfig());
            WidgetTestUtils.assertEquals(test, actualBitmap);
        } finally {
            SimpleCursorAdapterTest.destroyTestImage(mContext, SAMPLE_IMAGE_NAME);
        }
    }

    /**
     * Creates the test cursor.
     *
     * @param colCount the column count
     * @param rowCount the row count
     * @param prefix the prefix of each cell
     * @return the cursor
     */
    @SuppressWarnings("unchecked")
    private Cursor createTestCursor(int colCount, int rowCount, String prefix) {
        String[] columns = new String[colCount + 1];
        for (int i = 0; i < colCount; i++) {
            columns[i] = "column" + i;
        }
        columns[colCount] = "_id";

        MatrixCursor cursor = new MatrixCursor(columns, rowCount);
        Object[] row = new Object[colCount + 1];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                row[j] = prefix + i + "" + j;
            }
            row[colCount] = i;
            cursor.addRow(row);
        }
        return cursor;
    }

    private class MockSimpleCursorTreeAdapter extends SimpleCursorTreeAdapter {
        public MockSimpleCursorTreeAdapter(Context context, Cursor cursor,
                int collapsedGroupLayout, int expandedGroupLayout, String[] groupFrom,
                int[] groupTo, int childLayout, int lastChildLayout, String[] childFrom,
                int[] childTo) {
            super(context, cursor, collapsedGroupLayout, expandedGroupLayout, groupFrom, groupTo,
                    childLayout, lastChildLayout, childFrom, childTo);
        }

        public MockSimpleCursorTreeAdapter(Context context, Cursor cursor,
                int collapsedGroupLayout, int expandedGroupLayout, String[] groupFrom,
                int[] groupTo, int childLayout, String[] childFrom, int[] childTo) {
            super(context, cursor, collapsedGroupLayout, expandedGroupLayout, groupFrom, groupTo,
                    childLayout, childFrom, childTo);
        }

        public MockSimpleCursorTreeAdapter(Context c, Cursor cursor, int groupLayout,
                String[] groupFrom, int[] groupTo, int childLayout, String[] childFrom,
                int[] childTo) {
            super(c, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            return createTestCursor(3, 4, "child");
        }

        @Override
        protected void bindChildView(View v, Context context, Cursor cursor, boolean isLastChild) {
            super.bindChildView(v, context, cursor, isLastChild);
        }

        protected void bindGroupView(View v, Context context, Cursor cursor, boolean isExpanded) {
            super.bindGroupView(v, context, cursor, isExpanded);
        }

        @Override
        protected void setViewImage(ImageView v, String value) {
            super.setViewImage(v, value);
        }
    }
}

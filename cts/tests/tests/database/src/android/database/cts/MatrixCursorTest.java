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

package android.database.cts;


import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;

import java.util.ArrayList;

import junit.framework.TestCase;

public class MatrixCursorTest extends TestCase {
    private static final String COLUMN0_NAME = "column0";

    private static final String COLUMN1_NAME = "column1";

    private static final String COLUMN2_NAME = "column2";

    private static final int COLUMN0_INDEX = 0;

    private static final int COLUMN1_INDEX = 1;

    private static final int COLUMN2_INDEX = 2;

    private MatrixCursor mMatrixCursor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMatrixCursor = new MatrixCursor(new String[] { COLUMN0_NAME, COLUMN1_NAME, COLUMN2_NAME });
    }

    public void testMatrixCursor() {
        new MatrixCursor(new String[0]);

        new MatrixCursor(new String[] { COLUMN0_NAME }, 10);
    }

    public void testNewRow() {
        assertEquals(0, mMatrixCursor.getCount());
        RowBuilder builder = mMatrixCursor.newRow();
        assertNotNull(builder);
        assertEquals(1, mMatrixCursor.getCount());
        mMatrixCursor.moveToFirst();
        assertTrue(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertTrue(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertTrue(mMatrixCursor.isNull(COLUMN2_INDEX));

        builder.add(Integer.MIN_VALUE);
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertEquals(Integer.MIN_VALUE, mMatrixCursor.getInt(COLUMN0_INDEX));
        assertTrue(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertTrue(mMatrixCursor.isNull(COLUMN2_INDEX));

        builder.add(0);
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertEquals(Integer.MIN_VALUE, mMatrixCursor.getInt(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertEquals(0, mMatrixCursor.getInt(COLUMN1_INDEX));
        assertTrue(mMatrixCursor.isNull(COLUMN2_INDEX));

        builder.add(Integer.MAX_VALUE);
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertEquals(Integer.MIN_VALUE, mMatrixCursor.getInt(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertEquals(0, mMatrixCursor.getInt(COLUMN1_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN2_INDEX));
        assertEquals(Integer.MAX_VALUE, mMatrixCursor.getInt(COLUMN2_INDEX));

        try {
            builder.add(1);
            fail("Should throw CursorIndexOutOfBoundsException when adding too many values");
        } catch (CursorIndexOutOfBoundsException e) {
            // expected
        }

        assertNotNull(mMatrixCursor.newRow());
        assertEquals(2, mMatrixCursor.getCount());
    }

    public void testAddRow() {
        assertEquals(0, mMatrixCursor.getCount());

        mMatrixCursor.addRow(new Long[] { Long.MIN_VALUE, 0l, Long.MAX_VALUE });
        assertEquals(1, mMatrixCursor.getCount());
        mMatrixCursor.moveToLast();
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertEquals(Long.MIN_VALUE, mMatrixCursor.getLong(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertEquals(0, mMatrixCursor.getLong(COLUMN1_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN2_INDEX));
        assertEquals(Long.MAX_VALUE, mMatrixCursor.getLong(COLUMN2_INDEX));

        try {
            mMatrixCursor.addRow(new Long[2]);
            fail("Should throw IllegalArgumentException if values does not have the same length "
                    + "as the columns");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            mMatrixCursor.addRow(new Long[4]);
            fail("Should throw IllegalArgumentException if values does not have the same length "
                    + "as the columns");
        } catch (IllegalArgumentException e) {
            // expected
        }

        ArrayList<String> values = new ArrayList<String>();
        values.add("value0");
        values.add("value1");
        try {
            mMatrixCursor.addRow(values);
            fail("Should throw IllegalArgumentException if values does not have the same length "
                    + "as the columns");
        } catch (IllegalArgumentException e) {
            // expected
        }

        values.add("value2");
        mMatrixCursor.addRow(values);
        assertEquals(2, mMatrixCursor.getCount());
        mMatrixCursor.moveToLast();
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertEquals("value0", mMatrixCursor.getString(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertEquals("value1", mMatrixCursor.getString(COLUMN1_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN2_INDEX));
        assertEquals("value2", mMatrixCursor.getString(COLUMN2_INDEX));

        values.add("value3");
        try {
            mMatrixCursor.addRow(values);
            fail("Should throw IllegalArgumentException if values does not have the same length "
                    + "as the columns");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGetColumnNames() {
        String[] names = mMatrixCursor.getColumnNames();
        assertEquals(3, names.length);
        assertEquals(COLUMN0_NAME, names[0]);
        assertEquals(COLUMN1_NAME, names[1]);
        assertEquals(COLUMN2_NAME, names[2]);

        // the following methods from super class will call {@link MatrixCursor#getColumnNames()}
        assertEquals(COLUMN0_INDEX, mMatrixCursor.getColumnIndex(COLUMN0_NAME));
        assertEquals(COLUMN1_INDEX, mMatrixCursor.getColumnIndex(COLUMN1_NAME));
        assertEquals(COLUMN2_INDEX, mMatrixCursor.getColumnIndex(COLUMN2_NAME));
    }

    public void testGetters() {
        mMatrixCursor.addRow(new Short[] { Short.MIN_VALUE, 0, Short.MAX_VALUE });
        mMatrixCursor.moveToLast();
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN2_INDEX));

        // numbers can be converted to each other and strings
        assertEquals(Short.MIN_VALUE, mMatrixCursor.getShort(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getShort(COLUMN1_INDEX));
        assertEquals(Short.MAX_VALUE, mMatrixCursor.getShort(COLUMN2_INDEX));

        assertEquals(Short.MIN_VALUE, mMatrixCursor.getInt(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getInt(COLUMN1_INDEX));
        assertEquals(Short.MAX_VALUE, mMatrixCursor.getInt(COLUMN2_INDEX));

        assertEquals(Short.MIN_VALUE, mMatrixCursor.getLong(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getLong(COLUMN1_INDEX));
        assertEquals(Short.MAX_VALUE, mMatrixCursor.getLong(COLUMN2_INDEX));

        assertEquals(-32768f, mMatrixCursor.getFloat(COLUMN0_INDEX), 0.0f);
        assertEquals(0f, mMatrixCursor.getFloat(COLUMN1_INDEX), 0.0f);
        assertEquals(32767f, mMatrixCursor.getFloat(COLUMN2_INDEX), 0.0f);

        assertEquals(-32768d, mMatrixCursor.getDouble(COLUMN0_INDEX), 0.0d);
        assertEquals(0d, mMatrixCursor.getDouble(COLUMN1_INDEX), 0.0d);
        assertEquals(32767d, mMatrixCursor.getDouble(COLUMN2_INDEX), 0.0d);

        assertEquals("-32768", mMatrixCursor.getString(COLUMN0_INDEX));
        assertEquals("0", mMatrixCursor.getString(COLUMN1_INDEX));
        assertEquals("32767", mMatrixCursor.getString(COLUMN2_INDEX));

        mMatrixCursor.addRow(new Double[] { Double.MIN_VALUE, 0d, Double.MAX_VALUE });
        mMatrixCursor.moveToLast();
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN2_INDEX));

        // numbers can be converted to each other but loose accuracy
        assertEquals(0, mMatrixCursor.getShort(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getShort(COLUMN1_INDEX));
        assertEquals(-1, mMatrixCursor.getShort(COLUMN2_INDEX));

        assertEquals(0, mMatrixCursor.getInt(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getInt(COLUMN1_INDEX));
        assertEquals(Integer.MAX_VALUE, mMatrixCursor.getInt(COLUMN2_INDEX));

        assertEquals(0, mMatrixCursor.getLong(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getLong(COLUMN1_INDEX));
        assertEquals(Long.MAX_VALUE, mMatrixCursor.getLong(COLUMN2_INDEX));

        assertEquals(0.0f, mMatrixCursor.getFloat(COLUMN0_INDEX), 0.0f);
        assertEquals(0f, mMatrixCursor.getFloat(COLUMN1_INDEX), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, mMatrixCursor.getFloat(COLUMN2_INDEX), 0.0f);

        assertEquals(Double.MIN_VALUE, mMatrixCursor.getDouble(COLUMN0_INDEX), 0.0d);
        assertEquals(0d, mMatrixCursor.getDouble(COLUMN1_INDEX), 0.0d);
        assertEquals(Double.MAX_VALUE, mMatrixCursor.getDouble(COLUMN2_INDEX), 0.0d);

        assertEquals(Double.toString(Double.MIN_VALUE), mMatrixCursor.getString(COLUMN0_INDEX));
        assertEquals(Double.toString(0d), mMatrixCursor.getString(COLUMN1_INDEX));
        assertEquals(Double.toString(Double.MAX_VALUE), mMatrixCursor.getString(COLUMN2_INDEX));

        mMatrixCursor.addRow(new String[] { "-1", "0", "1" });
        mMatrixCursor.moveToLast();
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN2_INDEX));
        // strings can be converted to numbers
        assertEquals(-1, mMatrixCursor.getShort(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getShort(COLUMN1_INDEX));
        assertEquals(1, mMatrixCursor.getShort(COLUMN2_INDEX));

        assertEquals(-1, mMatrixCursor.getInt(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getInt(COLUMN1_INDEX));
        assertEquals(1, mMatrixCursor.getInt(COLUMN2_INDEX));

        assertEquals(-1, mMatrixCursor.getLong(COLUMN0_INDEX));
        assertEquals(0, mMatrixCursor.getLong(COLUMN1_INDEX));
        assertEquals(1, mMatrixCursor.getLong(COLUMN2_INDEX));

        assertEquals(-1f, mMatrixCursor.getFloat(COLUMN0_INDEX), 0.0f);
        assertEquals(0f, mMatrixCursor.getFloat(COLUMN1_INDEX), 0.0f);
        assertEquals(1f, mMatrixCursor.getFloat(COLUMN2_INDEX), 0.0f);

        assertEquals(-1d, mMatrixCursor.getDouble(COLUMN0_INDEX), 0.0d);
        assertEquals(0f, mMatrixCursor.getDouble(COLUMN1_INDEX), 0.0d);
        assertEquals(1d, mMatrixCursor.getDouble(COLUMN2_INDEX), 0.0d);

        assertEquals("-1", mMatrixCursor.getString(COLUMN0_INDEX));
        assertEquals("0", mMatrixCursor.getString(COLUMN1_INDEX));
        assertEquals("1", mMatrixCursor.getString(COLUMN2_INDEX));

        mMatrixCursor.addRow(new String[] { "1.0E2", "0xff", "not number" });
        mMatrixCursor.moveToLast();
        assertFalse(mMatrixCursor.isNull(COLUMN0_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN1_INDEX));
        assertFalse(mMatrixCursor.isNull(COLUMN2_INDEX));
        assertEquals("1.0E2", mMatrixCursor.getString(COLUMN0_INDEX));
        assertEquals("0xff", mMatrixCursor.getString(COLUMN1_INDEX));
        assertEquals("not number", mMatrixCursor.getString(COLUMN2_INDEX));

        assertEquals(100f, mMatrixCursor.getFloat(COLUMN0_INDEX), 0.0f);
        // strings can not be converted to numbers
        try {
            mMatrixCursor.getShort(COLUMN1_INDEX);
            fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            // expected
        }

        try {
            mMatrixCursor.getShort(COLUMN2_INDEX);
            fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            // expected
        }
    }
}

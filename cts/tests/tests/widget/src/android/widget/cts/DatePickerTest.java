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
import com.android.internal.util.XmlUtils;


import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.Xml;
import android.view.View;
import android.widget.DatePicker;

/**
 * Test {@link DatePicker}.
 */
public class DatePickerTest extends AndroidTestCase {
    public void testConstructor() {
        new DatePicker(mContext);

        new DatePicker(mContext, null);

        new DatePicker(mContext, getAttributeSet(R.layout.datepicker_layout));

        new DatePicker(mContext, getAttributeSet(R.layout.datepicker_layout), 0);

        // Test constructor with null Context, in fact, previous two functions will finally invoke
        // this version.
        try {
            // Test with null Context
            new DatePicker(null, getAttributeSet(R.layout.datepicker_layout), 0);
            fail("should throw NullPointerException");
        } catch (Exception e) {
        }
    }

    public void testSetEnabled() {
        MockDatePicker datePicker = createDatePicker();

        assertTrue(datePicker.isEnabled());

        datePicker.setEnabled(false);
        assertFalse(datePicker.isEnabled());

        datePicker.setEnabled(true);
        assertTrue(datePicker.isEnabled());
    }

    public void testInit() {
        MockOnDateChangedListener onDateChangedListener = new MockOnDateChangedListener();
        DatePicker datePicker = createDatePicker();

        /* The month display uses 1-12 but our internal state stores it
         * 0-11 so add one when setting the display.
         */
        datePicker.init(2000, 10, 15, onDateChangedListener);
        assertEquals(2000, datePicker.getYear());
        assertEquals(10, datePicker.getMonth());
        assertEquals(15, datePicker.getDayOfMonth());
    }

    public void testAccessDate() {
        DatePicker datePicker = createDatePicker();

        /* The month display uses 1-12 but our internal state stores it
         * 0-11 so add one when setting the display.
         */
        MockOnDateChangedListener onDateChangedListener = new MockOnDateChangedListener();
        datePicker.init(2000, 10, 15, onDateChangedListener);
        assertEquals(2000, datePicker.getYear());
        assertEquals(10, datePicker.getMonth());
        assertEquals(15, datePicker.getDayOfMonth());

        datePicker.updateDate(1989, 9, 19);
        assertEquals(1989, datePicker.getYear());
        assertEquals(9, datePicker.getMonth());
        assertEquals(19, datePicker.getDayOfMonth());
    }

    public void testUpdateDate() {
        DatePicker datePicker = createDatePicker();

        // Test normal input values
        /* The month display uses 1-12 but our internal state stores it
         * 0-11 so add one when setting the display.
         */
        datePicker.updateDate(1989, 9, 19);
        assertEquals(1989, datePicker.getYear());
        assertEquals(9, datePicker.getMonth());
        assertEquals(19, datePicker.getDayOfMonth());
    }

    public void testOnSaveInstanceState() {
        MockDatePicker datePicker = createDatePicker();

        datePicker.updateDate(2008, 9, 10);
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();

        // Test onSaveHierarchyState
        assertEquals(View.NO_ID, datePicker.getId());
        datePicker.setId(99);
        assertFalse(datePicker.hasCalledOnSaveInstanceState());
        datePicker.saveHierarchyState(container);
        assertEquals(1, datePicker.getChildCount());
        assertTrue(datePicker.hasCalledOnSaveInstanceState());

        // Test dispatchRestoreInstanceState
        datePicker = createDatePicker();
        datePicker.setId(99);
        assertFalse(datePicker.hasCalledOnRestoreInstanceState());
        datePicker.dispatchRestoreInstanceState(container);
        assertEquals(2008, datePicker.getYear());
        assertEquals(9, datePicker.getMonth());
        assertEquals(10, datePicker.getDayOfMonth());

        // Test onRestoreInstanceState
        assertTrue(datePicker.hasCalledOnRestoreInstanceState());
    }

    private AttributeSet getAttributeSet(int resourceId) {
        final XmlResourceParser parser = mContext.getResources().getXml(resourceId);
        try {
            XmlUtils.beginDocument(parser, "RelativeLayout");
        } catch (Exception e) {
            fail("Found unexpected loading process error before invoking generateLayoutParams.");
        }
        final AttributeSet attr = Xml.asAttributeSet(parser);
        assertNotNull(attr);
        return attr;
    }

    private MockDatePicker createDatePicker() {
        MockDatePicker datePicker = new MockDatePicker(mContext,
                getAttributeSet(R.layout.datepicker_layout));
        return datePicker;
    }

    private class MockDatePicker extends DatePicker {
        private boolean mCalledOnSaveInstanceState = false;
        private boolean mCalledOnRestoreInstanceState = false;

        public MockDatePicker(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
            super.dispatchRestoreInstanceState(container);
        }

        @Override
        protected Parcelable onSaveInstanceState() {
            mCalledOnSaveInstanceState = true;
            return super.onSaveInstanceState();
        }

        public boolean hasCalledOnSaveInstanceState() {
            return mCalledOnSaveInstanceState;
        }

        @Override
        protected void onRestoreInstanceState(Parcelable state) {
            mCalledOnRestoreInstanceState = true;
            super.onRestoreInstanceState(state);
        }

        public boolean hasCalledOnRestoreInstanceState() {
            return mCalledOnRestoreInstanceState;
        }
    }

    private class MockOnDateChangedListener implements DatePicker.OnDateChangedListener {
        public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        }
    }
}

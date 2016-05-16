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

package android.app.cts;


import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TimePicker;

/**
 * Test {@link TimePickerDialog}.
 */
public class TimePickerDialogTest extends ActivityInstrumentationTestCase2<DialogStubActivity> {
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String IS_24_HOUR = "is24hour";

    private static final int TARGET_HOUR = 15;
    private static final int TARGET_MINUTE = 9;

    private int mCallbackHour;
    private int mCallbackMinute;

    private OnTimeSetListener mOnTimeSetListener;

    private Context mContext;
    private DialogStubActivity mActivity;
    private TimePickerDialog mTimePickerDialog;

    public TimePickerDialogTest() {
        super("com.android.cts.stub", DialogStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getInstrumentation().getContext();
        mOnTimeSetListener = new OnTimeSetListener(){
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mCallbackHour = hourOfDay;
                mCallbackMinute = minute;
            }
        };
        mTimePickerDialog = new TimePickerDialog( mContext, mOnTimeSetListener, TARGET_HOUR,
                TARGET_MINUTE, true);
    }

    public void testSaveInstanceState(){
        TimePickerDialog tD = new TimePickerDialog( mContext, mOnTimeSetListener, TARGET_HOUR,
                TARGET_MINUTE, true);

        Bundle b = tD.onSaveInstanceState();

        assertEquals(TARGET_HOUR, b.getInt(HOUR));
        assertEquals(TARGET_MINUTE, b.getInt(MINUTE));
        assertTrue(b.getBoolean(IS_24_HOUR));

        int minute = 13;
        tD = new TimePickerDialog( mContext, com.android.cts.stub.R.style.Theme_AlertDialog,
                mOnTimeSetListener, TARGET_HOUR, minute, false);

        b = tD.onSaveInstanceState();

        assertEquals(TARGET_HOUR, b.getInt(HOUR));
        assertEquals(minute, b.getInt(MINUTE));
        assertFalse(b.getBoolean(IS_24_HOUR));
    }

    public void testOnClick(){
        mTimePickerDialog.onClick(null, 0);

        assertEquals(TARGET_HOUR, mCallbackHour);
        assertEquals(TARGET_MINUTE, mCallbackMinute);
    }

    public void testOnTimeChanged() throws Throwable {
        final int minute = 34;
        startDialogActivity(DialogStubActivity.TEST_TIMEPICKERDIALOG);
        final TimePickerDialog d = (TimePickerDialog) mActivity.getDialog();

        runTestOnUiThread(new Runnable() {
            public void run() {
                d.onTimeChanged(null, TARGET_HOUR, minute);
            }
        });
        getInstrumentation().waitForIdleSync();

    }

    public void testUpdateTime(){
        int minute = 18;
        mTimePickerDialog.updateTime(TARGET_HOUR, minute);

        //here call onSaveInstanceState is to check the data put by updateTime
        Bundle b = mTimePickerDialog.onSaveInstanceState();

        assertEquals(TARGET_HOUR, b.getInt(HOUR));
        assertEquals(minute, b.getInt(MINUTE));
    }

    public void testOnRestoreInstanceState(){
        int minute = 27;
        Bundle b1 = new Bundle();
        b1.putInt(HOUR, TARGET_HOUR);
        b1.putInt(MINUTE, minute);
        b1.putBoolean(IS_24_HOUR, false);

        mTimePickerDialog.onRestoreInstanceState(b1);

        //here call onSaveInstanceState is to check the data put by onRestoreInstanceState
        Bundle b2 = mTimePickerDialog.onSaveInstanceState();

        assertEquals(TARGET_HOUR, b2.getInt(HOUR));
        assertEquals(minute, b2.getInt(MINUTE));
        assertFalse(b2.getBoolean(IS_24_HOUR));
    }

    private void startDialogActivity(int dialogNumber) {
        mActivity = DialogStubActivity.startDialogActivity(this, dialogNumber);
    }
}

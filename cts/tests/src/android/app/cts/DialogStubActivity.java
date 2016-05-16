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

import com.android.cts.stub.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

/*
 * Stub class for  Dialog, AlertDialog, DatePickerDialog, TimePickerDialog etc.
 */
public class DialogStubActivity extends Activity {
    public static final int TEST_DIALOG_WITHOUT_THEME = 0;
    public static final int TEST_DIALOG_WITH_THEME = 1;
    public static final int TEST_ALERTDIALOG = 2;
    public static final int TEST_CUSTOM_ALERTDIALOG = 3;
    public static final int TEST_DATEPICKERDIALOG = 4;
    public static final int TEST_DATEPICKERDIALOG_WITH_THEME = 5;
    public static final int TEST_TIMEPICKERDIALOG = 6;
    public static final int TEST_TIMEPICKERDIALOG_WITH_THEME = 7;
    public static final int TEST_ONSTART_AND_ONSTOP = 8;
    public static final int TEST_ALERTDIALOG_DEPRECATED = 9;
    public static final int TEST_ALERTDIALOG_CALLBACK = 10;
    public static final int TEST_CUSTOM_ALERTDIALOG_VIEW = 11;
    public static final int TEST_ALERTDIALOG_DEPRECATED_WITH_MESSAGE = 12;
    public static final int TEST_ALERTDIALOG_THEME = 13;
    public static final int TEST_ALERTDIALOG_CANCELABLE = 14;
    public static final int TEST_ALERTDIALOG_NOT_CANCELABLE = 15;
    public static final int TEST_PROTECTED_CANCELABLE = 16;
    public static final int TEST_PROTECTED_NOT_CANCELABLE = 17;

    public static final int SPACING_LEFT = 10;
    public static final int SPACING_TOP = 20;
    public static final int SPACING_RIGHT = 30;
    public static final int SPACING_BOTTOM = 40;
    public static int buttonIndex;

    public static final String DEFAULT_ALERTDIALOG_TITLE = "AlertDialog";
    public static final String DEFAULT_ALERTDIALOG_MESSAGE = "AlertDialog message";
    private static final String LOG_TAG = "DialogStubActivity";

    public boolean isPositiveButtonClicked = false;
    public boolean isNegativeButtonClicked = false;
    public boolean isNeutralButtonClicked = false;
    public boolean isCallBackCalled;
    public boolean onCancelCalled;
    public boolean onKeyDownCalled;
    public boolean onKeyUpCalled;
    public boolean onCreateCalled;
    public boolean onCancelListenerCalled;
    public boolean onClickCalled;
    public static boolean onDateChangedCalled;
    public static boolean onRestoreInstanceStateCalled;
    public boolean onSaveInstanceStateCalled;
    public int updatedYear;
    public int updatedMonth;
    public int updatedDay;

    public final int INITIAL_YEAR = 2008;
    public final int INITIAL_MONTH = 7;
    public final int INITIAL_DAY_OF_MONTH = 27;
    private final int INITIAL_HOUR = 10;
    private final int INITIAL_MINUTE = 35;
    private Dialog mDialog;
    private AlertDialog mAlertDialog;
    private OnDateSetListener mOnDateSetListener = new OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            updatedYear = year;
            updatedMonth = monthOfYear;
            updatedDay = dayOfMonth;
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TEST_DIALOG_WITHOUT_THEME:
                mDialog = new Dialog(this);
                mDialog.setTitle("Hello, Dialog");
                break;

            case TEST_DIALOG_WITH_THEME:
                mDialog = new Dialog(this, 1);
                break;

            case TEST_ALERTDIALOG:
                mDialog = getAlertDialogInstance(false);
                break;

            case TEST_CUSTOM_ALERTDIALOG:
                mDialog = getCustomAlertDialogInstance(false);
                break;

            case TEST_CUSTOM_ALERTDIALOG_VIEW:
                mDialog = getCustomAlertDialogInstance(true);
                break;

            case TEST_DATEPICKERDIALOG:
                mDialog = new MockDatePickerDialog(this, mOnDateSetListener, INITIAL_YEAR,
                        INITIAL_MONTH, INITIAL_DAY_OF_MONTH);
                break;

            case TEST_DATEPICKERDIALOG_WITH_THEME:
                mDialog = new MockDatePickerDialog(this,
                        com.android.internal.R.style.Theme_Translucent, mOnDateSetListener,
                        INITIAL_YEAR, INITIAL_MONTH, INITIAL_DAY_OF_MONTH);
                break;

            case TEST_TIMEPICKERDIALOG:
                mDialog = new TimePickerDialog(this, new OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        isCallBackCalled = true;
                    }
                }, INITIAL_HOUR, INITIAL_MINUTE, true);
                break;

            case TEST_TIMEPICKERDIALOG_WITH_THEME:
                mDialog = new TimePickerDialog(this,
                        com.android.internal.R.style.Theme_Translucent, null, INITIAL_HOUR,
                        INITIAL_MINUTE, true);
                break;

            case TEST_ONSTART_AND_ONSTOP:
                mDialog = new TestDialog(this);
                Log.i(LOG_TAG, "mTestDialog:" + mDialog);
                return mDialog;

            case TEST_ALERTDIALOG_DEPRECATED:
                mDialog = getAlertDialogInstance(true);
                break;

            case TEST_ALERTDIALOG_DEPRECATED_WITH_MESSAGE:
                final Handler handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        buttonIndex = msg.what;
                        super.handleMessage(msg);
                    }
                };
                final Message positiveMessage = Message.obtain();
                positiveMessage.setTarget(handler);
                positiveMessage.what = DialogInterface.BUTTON_POSITIVE;

                final Message negativeMessage = Message.obtain();
                negativeMessage.setTarget(handler);
                negativeMessage.what = DialogInterface.BUTTON_NEGATIVE;

                final Message neutralMessage = Message.obtain();
                neutralMessage.setTarget(handler);
                neutralMessage.what = DialogInterface.BUTTON_NEUTRAL;
                mAlertDialog = getAlertDialogInstance(false);
                mAlertDialog.setButton(getString(R.string.alert_dialog_positive), positiveMessage);
                mAlertDialog.setButton2(getString(R.string.alert_dialog_negative), negativeMessage);
                mAlertDialog.setButton3(getString(R.string.alert_dialog_neutral), neutralMessage);
                mDialog = mAlertDialog;
                break;

            case TEST_ALERTDIALOG_CALLBACK:
                mDialog = new MockAlertDialog(this);
                break;
            case TEST_ALERTDIALOG_THEME:
                mDialog = new MockAlertDialog(this, R.style.Theme_AlertDialog);
                break;
            case TEST_ALERTDIALOG_CANCELABLE:
                mDialog = getAlertDialogCancelablInstance(true);
                break;
            case TEST_ALERTDIALOG_NOT_CANCELABLE:
                mDialog = getAlertDialogCancelablInstance(false);
                break;
            case TEST_PROTECTED_CANCELABLE:
                mDialog = new TestDialog(this, true, new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        onCancelListenerCalled = true;
                    }
                });
                break;
            case TEST_PROTECTED_NOT_CANCELABLE:
                mDialog = new TestDialog(this, false, new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        onCancelListenerCalled = true;
                    }
                });
                break;
            default:
                break;
        }

        Log.i(LOG_TAG, "mDialog:" + mDialog);
        return mDialog;
    }

    private AlertDialog getAlertDialogCancelablInstance(boolean cancelable) {
        OnCancelListener cancelListener = new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                onCancelCalled = true;
            }
        };
        return new MockAlertDialog(this, cancelable, cancelListener);
    }

    @SuppressWarnings("deprecation")
    private AlertDialog getAlertDialogInstance(boolean deprecated) {
        mAlertDialog = new AlertDialog.Builder(DialogStubActivity.this).create();
        mAlertDialog.setIcon(com.android.cts.stub.R.drawable.pass);
        mAlertDialog.setTitle(DEFAULT_ALERTDIALOG_TITLE);
        mAlertDialog.setMessage(DEFAULT_ALERTDIALOG_MESSAGE);
        mAlertDialog.setInverseBackgroundForced(true);
        final DialogInterface.OnClickListener positiveListener = new MockOnClickListener(
                DialogInterface.BUTTON_POSITIVE);
        final DialogInterface.OnClickListener netativeListener = new MockOnClickListener(
                DialogInterface.BUTTON_NEGATIVE);
        final DialogInterface.OnClickListener neutralListener = new MockOnClickListener(
                DialogInterface.BUTTON_NEUTRAL);

        if (deprecated) {
            mAlertDialog.setButton(getString(R.string.alert_dialog_positive), positiveListener);
            mAlertDialog.setButton2(getString(R.string.alert_dialog_negative), netativeListener);
            mAlertDialog.setButton3(getString(R.string.alert_dialog_neutral), neutralListener);
        } else {
            mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.alert_dialog_positive), positiveListener);
            mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.alert_dialog_negative), netativeListener);
            mAlertDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                    getString(R.string.alert_dialog_neutral), neutralListener);
        }
        return mAlertDialog;

    }

    private AlertDialog getCustomAlertDialogInstance(boolean withSpacing) {
        final LayoutInflater inflate = getLayoutInflater();
        final View customTitleViewCustom = inflate.inflate(R.layout.alertdialog_custom_title, null);
        final View textEntryView = inflate.inflate(R.layout.alert_dialog_text_entry_2, null);
        mAlertDialog = new AlertDialog.Builder(DialogStubActivity.this).create();
        mAlertDialog.setCustomTitle(customTitleViewCustom);
        mAlertDialog.setMessage(DEFAULT_ALERTDIALOG_MESSAGE);
        if (withSpacing) {
            mAlertDialog.setView(textEntryView, SPACING_LEFT, SPACING_TOP, SPACING_RIGHT,
                    SPACING_BOTTOM);
        } else {
            mAlertDialog.setView(textEntryView);
        }

        return mAlertDialog;

    }

    public Dialog getDialog() {
        return mDialog;
    }

    public String getDialogTitle() {
        return (String) mDialog.getWindow().getAttributes().getTitle();
    }

    private static final String TEST_DIALOG_NUMBER_EXTRA = "testDialogNumber";

    public static <T extends Activity> T startDialogActivity(
            ActivityInstrumentationTestCase2<T> testCase, int dialogNumber) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra(TEST_DIALOG_NUMBER_EXTRA, dialogNumber);
        testCase.setActivityIntent(intent);
        return testCase.getActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_stub_layout);

        Intent intent = getIntent();
        int dialogNum = intent.getIntExtra(TEST_DIALOG_NUMBER_EXTRA, -1);
        if (dialogNum != -1) {
            showDialog(dialogNum);
        }
    }

    public void setUpTitle(final String title) {
        runOnUiThread(new Runnable() {
            public void run() {
                getDialog().setTitle(title);
            }
        });
    }

    public void setUpTitle(final int id) {
        runOnUiThread(new Runnable() {
            public void run() {
                getDialog().setTitle(id);
            }
        });
    }

    class MockAlertDialog extends AlertDialog {
        public MockAlertDialog(Context context) {
            super(context);
        }

        public MockAlertDialog(Context context, int theme) {
            super(context, theme);
        }

        public MockAlertDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
            super(context, cancelable, cancelListener);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            onKeyDownCalled = true;
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            onKeyUpCalled = true;
            return super.onKeyUp(keyCode, event);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            onCreateCalled = true;
            super.onCreate(savedInstanceState);
        }

    }

    class MockOnClickListener implements DialogInterface.OnClickListener {
        private final int mId;

        public MockOnClickListener(final int buttonId) {
            mId = buttonId;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (mId) {
                case DialogInterface.BUTTON_POSITIVE:
                    isPositiveButtonClicked = true;
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    isNegativeButtonClicked = true;
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    isNeutralButtonClicked = true;
                    break;
                default:
                    break;
            }
        }
    }

    class MockDatePickerDialog extends DatePickerDialog {
        public MockDatePickerDialog(Context context, OnDateSetListener callBack, int year,
                int monthOfYear, int dayOfMonth) {
            super(context, callBack, year, monthOfYear, dayOfMonth);
        }

        public MockDatePickerDialog(Context context, int theme, OnDateSetListener callBack,
                int year, int monthOfYear, int dayOfMonth) {
            super(context, theme, callBack, year, monthOfYear, dayOfMonth);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            onClickCalled = true;
            super.onClick(dialog, which);
        }

        @Override
        public void onDateChanged(DatePicker view, int year, int month, int day) {
            onDateChangedCalled = true;
            super.onDateChanged(view, year, month, day);
        }

        @Override
        public void onRestoreInstanceState(Bundle savedInstanceState) {
            onRestoreInstanceStateCalled = true;
            super.onRestoreInstanceState(savedInstanceState);
        }

        @Override
        public Bundle onSaveInstanceState() {
            onSaveInstanceStateCalled = true;
            return super.onSaveInstanceState();
        }

    }
}

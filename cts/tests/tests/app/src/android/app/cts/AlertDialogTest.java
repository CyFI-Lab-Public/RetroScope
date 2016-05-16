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


import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.cts.util.PollingCheck;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

/*
 * Test AlertDialog
 */
public class AlertDialogTest extends ActivityInstrumentationTestCase2<DialogStubActivity> {
    private static final String ALERTDIALOG_CUSTOM_TITLE = "Hello, World!";

    private Instrumentation mInstrumentation;
    private DialogStubActivity mActivity;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private Button mNeutralButton;

    public AlertDialogTest() {
        super("com.android.cts.stub", DialogStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
    }

    protected void startDialogActivity(int dialogNumber) {
        mActivity = DialogStubActivity.startDialogActivity(this, dialogNumber);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return mActivity.getDialog().isShowing();
            }
        }.run();
    }

    public void testAlertDialog() throws Throwable {
        doTestAlertDialog(DialogStubActivity.TEST_ALERTDIALOG);
    }

    private void doTestAlertDialog(int index) throws Throwable {
        startDialogActivity(index);
        assertTrue(mActivity.getDialog().isShowing());

        mPositiveButton = ((AlertDialog) (mActivity.getDialog())).getButton(
                DialogInterface.BUTTON_POSITIVE);
        assertNotNull(mPositiveButton);
        assertEquals(mActivity.getString(com.android.cts.stub.R.string.alert_dialog_positive),
                mPositiveButton.getText());
        mNeutralButton = ((AlertDialog) (mActivity.getDialog())).getButton(
                DialogInterface.BUTTON_NEUTRAL);
        assertNotNull(mNeutralButton);
        assertEquals(mActivity.getString(com.android.cts.stub.R.string.alert_dialog_neutral),
                mNeutralButton.getText());
        mNegativeButton = ((AlertDialog) (mActivity.getDialog())).getButton(
                DialogInterface.BUTTON_NEGATIVE);
        assertNotNull(mNegativeButton);
        assertEquals(mActivity.getString(com.android.cts.stub.R.string.alert_dialog_negative),
                mNegativeButton.getText());

        assertFalse(mActivity.isPositiveButtonClicked);
        performClick(mPositiveButton);
        assertTrue(mActivity.isPositiveButtonClicked);

        assertFalse(mActivity.isNegativeButtonClicked);
        performClick(mNegativeButton);
        assertTrue(mActivity.isNegativeButtonClicked);

        assertFalse(mActivity.isNeutralButtonClicked);
        performClick(mNeutralButton);
        assertTrue(mActivity.isNeutralButtonClicked);
    }

    public void testAlertDialogDeprecatedAPI() throws Throwable {
        doTestAlertDialog(DialogStubActivity.TEST_ALERTDIALOG_DEPRECATED);
    }

    public void testAlertDialogDeprecatedAPIWithMessage() throws Throwable {
        startDialogActivity(DialogStubActivity.TEST_ALERTDIALOG_DEPRECATED_WITH_MESSAGE);
        assertTrue(mActivity.getDialog().isShowing());

        mPositiveButton = ((AlertDialog) (mActivity.getDialog())).getButton(
                DialogInterface.BUTTON_POSITIVE);
        assertNotNull(mPositiveButton);
        assertEquals(mActivity.getString(com.android.cts.stub.R.string.alert_dialog_positive),
                mPositiveButton.getText());
        mNegativeButton = ((AlertDialog) (mActivity.getDialog())).getButton(
                DialogInterface.BUTTON_NEGATIVE);
        assertNotNull(mNegativeButton);
        assertEquals(mActivity.getString(com.android.cts.stub.R.string.alert_dialog_negative),
                mNegativeButton.getText());
        mNeutralButton = ((AlertDialog) (mActivity.getDialog())).getButton(
                DialogInterface.BUTTON_NEUTRAL);
        assertNotNull(mNeutralButton);
        assertEquals(mActivity.getString(com.android.cts.stub.R.string.alert_dialog_neutral),
                mNeutralButton.getText());

        DialogStubActivity.buttonIndex = 0;
        performClick(mPositiveButton);
        assertEquals(DialogInterface.BUTTON_POSITIVE, DialogStubActivity.buttonIndex);

        DialogStubActivity.buttonIndex = 0;
        performClick(mNeutralButton);
        assertEquals(DialogInterface.BUTTON_NEUTRAL, DialogStubActivity.buttonIndex);

        DialogStubActivity.buttonIndex = 0;
        performClick(mNegativeButton);
        assertEquals(DialogInterface.BUTTON_NEGATIVE, DialogStubActivity.buttonIndex);
    }

    private void performClick(final Button button) throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                button.performClick();
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void testCustomAlertDialog() {
        startDialogActivity(DialogStubActivity.TEST_CUSTOM_ALERTDIALOG);
        assertTrue(mActivity.getDialog().isShowing());
    }

    public void testCustomAlertDialogView() {
        startDialogActivity(DialogStubActivity.TEST_CUSTOM_ALERTDIALOG_VIEW);
        assertTrue(mActivity.getDialog().isShowing());
    }


    public void testCallback() {
        startDialogActivity(DialogStubActivity.TEST_ALERTDIALOG_CALLBACK);
        assertTrue(mActivity.onCreateCalled);

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_0);
        assertTrue(mActivity.onKeyDownCalled);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_0);
        assertTrue(mActivity.onKeyUpCalled);
    }

    public void testAlertDialogTheme() throws Exception {
        startDialogActivity(DialogStubActivity.TEST_ALERTDIALOG_THEME);
        assertTrue(mActivity.getDialog().isShowing());
    }

    public void testAlertDialogCancelable() throws Exception {
        startDialogActivity(DialogStubActivity.TEST_ALERTDIALOG_CANCELABLE);
        assertTrue(mActivity.getDialog().isShowing());
        assertFalse(mActivity.onCancelCalled);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        mInstrumentation.waitForIdleSync();
        assertTrue(mActivity.onCancelCalled);
    }

    public void testAlertDialogNotCancelable() throws Exception {
        startDialogActivity(DialogStubActivity.TEST_ALERTDIALOG_NOT_CANCELABLE);
        assertTrue(mActivity.getDialog().isShowing());
        assertFalse(mActivity.onCancelCalled);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        assertFalse(mActivity.onCancelCalled);
    }
}

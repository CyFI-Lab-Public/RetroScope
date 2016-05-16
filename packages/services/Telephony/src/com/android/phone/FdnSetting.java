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

package com.android.phone;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;

/**
 * FDN settings UI for the Phone app.
 * Rewritten to look and behave closer to the other preferences.
 */
public class FdnSetting extends PreferenceActivity
        implements EditPinPreference.OnPinEnteredListener, DialogInterface.OnCancelListener {

    private static final String LOG_TAG = PhoneGlobals.LOG_TAG;
    private static final boolean DBG = false;

    private Phone mPhone;

    /**
     * Events we handle.
     * The first is used for toggling FDN enable, the second for the PIN change.
     */
    private static final int EVENT_PIN2_ENTRY_COMPLETE = 100;
    private static final int EVENT_PIN2_CHANGE_COMPLETE = 200;

    // String keys for preference lookup
    // We only care about the pin preferences here, the manage FDN contacts
    // Preference is handled solely in xml.
    private static final String BUTTON_FDN_ENABLE_KEY = "button_fdn_enable_key";
    private static final String BUTTON_CHANGE_PIN2_KEY = "button_change_pin2_key";

    private EditPinPreference mButtonEnableFDN;
    private EditPinPreference mButtonChangePin2;

    // State variables
    private String mOldPin;
    private String mNewPin;
    private String mPuk2;
    private static final int PIN_CHANGE_OLD = 0;
    private static final int PIN_CHANGE_NEW = 1;
    private static final int PIN_CHANGE_REENTER = 2;
    private static final int PIN_CHANGE_PUK = 3;
    private static final int PIN_CHANGE_NEW_PIN_FOR_PUK = 4;
    private static final int PIN_CHANGE_REENTER_PIN_FOR_PUK = 5;
    private int mPinChangeState;
    private boolean mIsPuk2Locked;    // Indicates we know that we are PUK2 blocked.

    private static final String SKIP_OLD_PIN_KEY = "skip_old_pin_key";
    private static final String PIN_CHANGE_STATE_KEY = "pin_change_state_key";
    private static final String OLD_PIN_KEY = "old_pin_key";
    private static final String NEW_PIN_KEY = "new_pin_key";
    private static final String DIALOG_MESSAGE_KEY = "dialog_message_key";
    private static final String DIALOG_PIN_ENTRY_KEY = "dialog_pin_entry_key";

    // size limits for the pin.
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;

    /**
     * Delegate to the respective handlers.
     */
    @Override
    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (preference == mButtonEnableFDN) {
            toggleFDNEnable(positiveResult);
        } else if (preference == mButtonChangePin2){
            updatePINChangeState(positiveResult);
        }
    }

    /**
     * Attempt to toggle FDN activation.
     */
    private void toggleFDNEnable(boolean positiveResult) {
        if (!positiveResult) {
            return;
        }

        // validate the pin first, before submitting it to the RIL for FDN enable.
        String password = mButtonEnableFDN.getText();
        if (validatePin (password, false)) {
            // get the relevant data for the icc call
            boolean isEnabled = mPhone.getIccCard().getIccFdnEnabled();
            Message onComplete = mFDNHandler.obtainMessage(EVENT_PIN2_ENTRY_COMPLETE);

            // make fdn request
            mPhone.getIccCard().setIccFdnEnabled(!isEnabled, password, onComplete);
        } else {
            // throw up error if the pin is invalid.
            displayMessage(R.string.invalidPin2);
        }

        mButtonEnableFDN.setText("");
    }

    /**
     * Attempt to change the pin.
     */
    private void updatePINChangeState(boolean positiveResult) {
        if (DBG) log("updatePINChangeState positive=" + positiveResult
                + " mPinChangeState=" + mPinChangeState
                + " mSkipOldPin=" + mIsPuk2Locked);

        if (!positiveResult) {
            // reset the state on cancel, either to expect PUK2 or PIN2
            if (!mIsPuk2Locked) {
                resetPinChangeState();
            } else {
                resetPinChangeStateForPUK2();
            }
            return;
        }

        // Progress through the dialog states, generally in this order:
        //   1. Enter old pin
        //   2. Enter new pin
        //   3. Re-Enter new pin
        // While handling any error conditions that may show up in between.
        // Also handle the PUK2 entry, if it is requested.
        //
        // In general, if any invalid entries are made, the dialog re-
        // appears with text to indicate what the issue is.
        switch (mPinChangeState) {
            case PIN_CHANGE_OLD:
                mOldPin = mButtonChangePin2.getText();
                mButtonChangePin2.setText("");
                // if the pin is not valid, display a message and reset the state.
                if (validatePin (mOldPin, false)) {
                    mPinChangeState = PIN_CHANGE_NEW;
                    displayPinChangeDialog();
                } else {
                    displayPinChangeDialog(R.string.invalidPin2, true);
                }
                break;
            case PIN_CHANGE_NEW:
                mNewPin = mButtonChangePin2.getText();
                mButtonChangePin2.setText("");
                // if the new pin is not valid, display a message and reset the state.
                if (validatePin (mNewPin, false)) {
                    mPinChangeState = PIN_CHANGE_REENTER;
                    displayPinChangeDialog();
                } else {
                    displayPinChangeDialog(R.string.invalidPin2, true);
                }
                break;
            case PIN_CHANGE_REENTER:
                // if the re-entered pin is not valid, display a message and reset the state.
                if (!mNewPin.equals(mButtonChangePin2.getText())) {
                    mPinChangeState = PIN_CHANGE_NEW;
                    mButtonChangePin2.setText("");
                    displayPinChangeDialog(R.string.mismatchPin2, true);
                } else {
                    // If the PIN is valid, then we submit the change PIN request.
                    mButtonChangePin2.setText("");
                    Message onComplete = mFDNHandler.obtainMessage(
                            EVENT_PIN2_CHANGE_COMPLETE);
                    mPhone.getIccCard().changeIccFdnPassword(
                            mOldPin, mNewPin, onComplete);
                }
                break;
            case PIN_CHANGE_PUK: {
                    // Doh! too many incorrect requests, PUK requested.
                    mPuk2 = mButtonChangePin2.getText();
                    mButtonChangePin2.setText("");
                    // if the puk is not valid, display
                    // a message and reset the state.
                    if (validatePin (mPuk2, true)) {
                        mPinChangeState = PIN_CHANGE_NEW_PIN_FOR_PUK;
                        displayPinChangeDialog();
                    } else {
                        displayPinChangeDialog(R.string.invalidPuk2, true);
                    }
                }
                break;
            case PIN_CHANGE_NEW_PIN_FOR_PUK:
                mNewPin = mButtonChangePin2.getText();
                mButtonChangePin2.setText("");
                // if the new pin is not valid, display
                // a message and reset the state.
                if (validatePin (mNewPin, false)) {
                    mPinChangeState = PIN_CHANGE_REENTER_PIN_FOR_PUK;
                    displayPinChangeDialog();
                } else {
                    displayPinChangeDialog(R.string.invalidPin2, true);
                }
                break;
            case PIN_CHANGE_REENTER_PIN_FOR_PUK:
                // if the re-entered pin is not valid, display
                // a message and reset the state.
                if (!mNewPin.equals(mButtonChangePin2.getText())) {
                    mPinChangeState = PIN_CHANGE_NEW_PIN_FOR_PUK;
                    mButtonChangePin2.setText("");
                    displayPinChangeDialog(R.string.mismatchPin2, true);
                } else {
                    // Both puk2 and new pin2 are ready to submit
                    mButtonChangePin2.setText("");
                    Message onComplete = mFDNHandler.obtainMessage(
                            EVENT_PIN2_CHANGE_COMPLETE);
                    mPhone.getIccCard().supplyPuk2(mPuk2, mNewPin, onComplete);
                }
                break;
        }
    }

    /**
     * Handler for asynchronous replies from the sim.
     */
    private final Handler mFDNHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                // when we are enabling FDN, either we are unsuccessful and display
                // a toast, or just update the UI.
                case EVENT_PIN2_ENTRY_COMPLETE: {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null && ar.exception instanceof CommandException) {
                            int attemptsRemaining = msg.arg1;
                            // see if PUK2 is requested and alert the user accordingly.
                            CommandException.Error e =
                                    ((CommandException) ar.exception).getCommandError();
                            switch (e) {
                                case SIM_PUK2:
                                    // make sure we set the PUK2 state so that we can skip
                                    // some redundant behaviour.
                                    displayMessage(R.string.fdn_enable_puk2_requested,
                                            attemptsRemaining);
                                    resetPinChangeStateForPUK2();
                                    break;
                                case PASSWORD_INCORRECT:
                                    displayMessage(R.string.pin2_invalid, attemptsRemaining);
                                    break;
                                default:
                                    displayMessage(R.string.fdn_failed, attemptsRemaining);
                                    break;
                            }
                        }
                        updateEnableFDN();
                    }
                    break;

                // when changing the pin we need to pay attention to whether or not
                // the error requests a PUK (usually after too many incorrect tries)
                // Set the state accordingly.
                case EVENT_PIN2_CHANGE_COMPLETE: {
                        if (DBG)
                            log("Handle EVENT_PIN2_CHANGE_COMPLETE");
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            int attemptsRemaining = msg.arg1;
                            log("Handle EVENT_PIN2_CHANGE_COMPLETE attemptsRemaining="
                                    + attemptsRemaining);
                            CommandException ce = (CommandException) ar.exception;
                            if (ce.getCommandError() == CommandException.Error.SIM_PUK2) {
                                // throw an alert dialog on the screen, displaying the
                                // request for a PUK2.  set the cancel listener to
                                // FdnSetting.onCancel().
                                AlertDialog a = new AlertDialog.Builder(FdnSetting.this)
                                    .setMessage(R.string.puk2_requested)
                                    .setCancelable(true)
                                    .setOnCancelListener(FdnSetting.this)
                                    .create();
                                a.getWindow().addFlags(
                                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                                a.show();
                            } else {
                                // set the correct error message depending upon the state.
                                // Reset the state depending upon or knowledge of the PUK state.
                                if (!mIsPuk2Locked) {
                                    displayMessage(R.string.badPin2, attemptsRemaining);
                                    resetPinChangeState();
                                } else {
                                    displayMessage(R.string.badPuk2, attemptsRemaining);
                                    resetPinChangeStateForPUK2();
                                }
                            }
                        } else {
                            if (mPinChangeState == PIN_CHANGE_PUK) {
                                displayMessage(R.string.pin2_unblocked);
                            } else {
                                displayMessage(R.string.pin2_changed);
                            }

                            // reset to normal behaviour on successful change.
                            resetPinChangeState();
                        }
                    }
                    break;
            }
        }
    };

    /**
     * Cancel listener for the PUK2 request alert dialog.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        // set the state of the preference and then display the dialog.
        resetPinChangeStateForPUK2();
        displayPinChangeDialog(0, true);
    }

    /**
     * Display a toast for message, like the rest of the settings.
     */
    private final void displayMessage(int strId, int attemptsRemaining) {
        String s = getString(strId);
        if ((strId == R.string.badPin2) || (strId == R.string.badPuk2) ||
                (strId == R.string.pin2_invalid)) {
            if (attemptsRemaining >= 0) {
                s = getString(strId) + getString(R.string.pin2_attempts, attemptsRemaining);
            } else {
                s = getString(strId);
            }
        }
        log("displayMessage: attemptsRemaining=" + attemptsRemaining + " s=" + s);
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private final void displayMessage(int strId) {
        displayMessage(strId, -1);
    }

    /**
     * The next two functions are for updating the message field on the dialog.
     */
    private final void displayPinChangeDialog() {
        displayPinChangeDialog(0, true);
    }

    private final void displayPinChangeDialog(int strId, boolean shouldDisplay) {
        int msgId;
        switch (mPinChangeState) {
            case PIN_CHANGE_OLD:
                msgId = R.string.oldPin2Label;
                break;
            case PIN_CHANGE_NEW:
            case PIN_CHANGE_NEW_PIN_FOR_PUK:
                msgId = R.string.newPin2Label;
                break;
            case PIN_CHANGE_REENTER:
            case PIN_CHANGE_REENTER_PIN_FOR_PUK:
                msgId = R.string.confirmPin2Label;
                break;
            case PIN_CHANGE_PUK:
            default:
                msgId = R.string.label_puk2_code;
                break;
        }

        // append the note / additional message, if needed.
        if (strId != 0) {
            mButtonChangePin2.setDialogMessage(getText(msgId) + "\n" + getText(strId));
        } else {
            mButtonChangePin2.setDialogMessage(msgId);
        }

        // only display if requested.
        if (shouldDisplay) {
            mButtonChangePin2.showPinDialog();
        }
    }

    /**
     * Reset the state of the pin change dialog.
     */
    private final void resetPinChangeState() {
        if (DBG) log("resetPinChangeState");
        mPinChangeState = PIN_CHANGE_OLD;
        displayPinChangeDialog(0, false);
        mOldPin = mNewPin = "";
        mIsPuk2Locked = false;
    }

    /**
     * Reset the state of the pin change dialog solely for PUK2 use.
     */
    private final void resetPinChangeStateForPUK2() {
        if (DBG) log("resetPinChangeStateForPUK2");
        mPinChangeState = PIN_CHANGE_PUK;
        displayPinChangeDialog(0, false);
        mOldPin = mNewPin = mPuk2 = "";
        mIsPuk2Locked = true;
    }

    /**
     * Validate the pin entry.
     *
     * @param pin This is the pin to validate
     * @param isPuk Boolean indicating whether we are to treat
     * the pin input as a puk.
     */
    private boolean validatePin(String pin, boolean isPuk) {

        // for pin, we have 4-8 numbers, or puk, we use only 8.
        int pinMinimum = isPuk ? MAX_PIN_LENGTH : MIN_PIN_LENGTH;

        // check validity
        if (pin == null || pin.length() < pinMinimum || pin.length() > MAX_PIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Reflect the updated FDN state in the UI.
     */
    private void updateEnableFDN() {
        if (mPhone.getIccCard().getIccFdnEnabled()) {
            mButtonEnableFDN.setTitle(R.string.enable_fdn_ok);
            mButtonEnableFDN.setSummary(R.string.fdn_enabled);
            mButtonEnableFDN.setDialogTitle(R.string.disable_fdn);
        } else {
            mButtonEnableFDN.setTitle(R.string.disable_fdn_ok);
            mButtonEnableFDN.setSummary(R.string.fdn_disabled);
            mButtonEnableFDN.setDialogTitle(R.string.enable_fdn);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.fdn_setting);

        mPhone = PhoneGlobals.getPhone();

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonEnableFDN = (EditPinPreference) prefSet.findPreference(BUTTON_FDN_ENABLE_KEY);
        mButtonChangePin2 = (EditPinPreference) prefSet.findPreference(BUTTON_CHANGE_PIN2_KEY);

        //assign click listener and update state
        mButtonEnableFDN.setOnPinEnteredListener(this);
        updateEnableFDN();

        mButtonChangePin2.setOnPinEnteredListener(this);

        // Only reset the pin change dialog if we're not in the middle of changing it.
        if (icicle == null) {
            resetPinChangeState();
        } else {
            mIsPuk2Locked = icicle.getBoolean(SKIP_OLD_PIN_KEY);
            mPinChangeState = icicle.getInt(PIN_CHANGE_STATE_KEY);
            mOldPin = icicle.getString(OLD_PIN_KEY);
            mNewPin = icicle.getString(NEW_PIN_KEY);
            mButtonChangePin2.setDialogMessage(icicle.getString(DIALOG_MESSAGE_KEY));
            mButtonChangePin2.setText(icicle.getString(DIALOG_PIN_ENTRY_KEY));
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPhone = PhoneGlobals.getPhone();
        updateEnableFDN();
    }

    /**
     * Save the state of the pin change.
     */
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(SKIP_OLD_PIN_KEY, mIsPuk2Locked);
        out.putInt(PIN_CHANGE_STATE_KEY, mPinChangeState);
        out.putString(OLD_PIN_KEY, mOldPin);
        out.putString(NEW_PIN_KEY, mNewPin);
        out.putString(DIALOG_MESSAGE_KEY, mButtonChangePin2.getDialogMessage().toString());
        out.putString(DIALOG_PIN_ENTRY_KEY, mButtonChangePin2.getText());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            CallFeaturesSetting.goUpToTopLevelSetting(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "FdnSetting: " + msg);
    }
}


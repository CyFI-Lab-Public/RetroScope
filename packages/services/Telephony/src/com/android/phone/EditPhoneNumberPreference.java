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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.DialerKeyListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class EditPhoneNumberPreference extends EditTextPreference {

    //allowed modes for this preference.
    /** simple confirmation (OK / CANCEL) */
    private static final int CM_CONFIRM = 0;
    /** toggle [(ENABLE / CANCEL) or (DISABLE / CANCEL)], use isToggled() to see requested state.*/
    private static final int CM_ACTIVATION = 1;

    private int mConfirmationMode;

    //String constants used in storing the value of the preference
    // The preference is backed by a string that holds the encoded value, which reads:
    //  <VALUE_ON | VALUE_OFF><VALUE_SEPARATOR><mPhoneNumber>
    // for example, an enabled preference with a number of 6502345678 would read:
    //  "1:6502345678"
    private static final String VALUE_SEPARATOR = ":";
    private static final String VALUE_OFF = "0";
    private static final String VALUE_ON = "1";

    //UI layout
    private ImageButton mContactPickButton;

    //Listeners
    /** Called when focus is changed between fields */
    private View.OnFocusChangeListener mDialogFocusChangeListener;
    /** Called when the Dialog is closed. */
    private OnDialogClosedListener mDialogOnClosedListener;
    /**
     * Used to indicate that we are going to request for a
     * default number. for the dialog.
     */
    private GetDefaultNumberListener mGetDefaultNumberListener;

    //Activity values
    private Activity mParentActivity;
    private Intent mContactListIntent;
    /** Arbitrary activity-assigned preference id value */
    private int mPrefId;

    //similar to toggle preference
    private CharSequence mEnableText;
    private CharSequence mDisableText;
    private CharSequence mChangeNumberText;
    private CharSequence mSummaryOn;
    private CharSequence mSummaryOff;

    // button that was clicked on dialog close.
    private int mButtonClicked;

    //relevant (parsed) value of the mText
    private String mPhoneNumber;
    private boolean mChecked;


    /**
     * Interface for the dialog closed listener, related to
     * DialogPreference.onDialogClosed(), except we also pass in a buttonClicked
     * value indicating which of the three possible buttons were pressed.
     */
    interface OnDialogClosedListener {
        void onDialogClosed(EditPhoneNumberPreference preference, int buttonClicked);
    }

    /**
     * Interface for the default number setting listener.  Handles requests for
     * the default display number for the dialog.
     */
    interface GetDefaultNumberListener {
        /**
         * Notify that we are looking for a default display value.
         * @return null if there is no contribution from this interface,
         *  indicating that the orignal value of mPhoneNumber should be
         *  displayed unchanged.
         */
        String onGetDefaultNumber(EditPhoneNumberPreference preference);
    }

    /*
     * Constructors
     */
    public EditPhoneNumberPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.pref_dialog_editphonenumber);

        //create intent to bring up contact list
        mContactListIntent = new Intent(Intent.ACTION_GET_CONTENT);
        mContactListIntent.setType(Phone.CONTENT_ITEM_TYPE);

        //get the edit phone number default settings
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.EditPhoneNumberPreference, 0, R.style.EditPhoneNumberPreference);
        mEnableText = a.getString(R.styleable.EditPhoneNumberPreference_enableButtonText);
        mDisableText = a.getString(R.styleable.EditPhoneNumberPreference_disableButtonText);
        mChangeNumberText = a.getString(R.styleable.EditPhoneNumberPreference_changeNumButtonText);
        mConfirmationMode = a.getInt(R.styleable.EditPhoneNumberPreference_confirmMode, 0);
        a.recycle();

        //get the summary settings, use CheckBoxPreference as the standard.
        a = context.obtainStyledAttributes(attrs, android.R.styleable.CheckBoxPreference, 0, 0);
        mSummaryOn = a.getString(android.R.styleable.CheckBoxPreference_summaryOn);
        mSummaryOff = a.getString(android.R.styleable.CheckBoxPreference_summaryOff);
        a.recycle();
    }

    public EditPhoneNumberPreference(Context context) {
        this(context, null);
    }


    /*
     * Methods called on UI bindings
     */
    @Override
    //called when we're binding the view to the preference.
    protected void onBindView(View view) {
        super.onBindView(view);

        // Sync the summary view
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        if (summaryView != null) {
            CharSequence sum;
            int vis;

            //set summary depending upon mode
            if (mConfirmationMode == CM_ACTIVATION) {
                if (mChecked) {
                    sum = (mSummaryOn == null) ? getSummary() : mSummaryOn;
                } else {
                    sum = (mSummaryOff == null) ? getSummary() : mSummaryOff;
                }
            } else {
                sum = getSummary();
            }

            if (sum != null) {
                summaryView.setText(sum);
                vis = View.VISIBLE;
            } else {
                vis = View.GONE;
            }

            if (vis != summaryView.getVisibility()) {
                summaryView.setVisibility(vis);
            }
        }
    }

    //called when we're binding the dialog to the preference's view.
    @Override
    protected void onBindDialogView(View view) {
        // default the button clicked to be the cancel button.
        mButtonClicked = DialogInterface.BUTTON_NEGATIVE;

        super.onBindDialogView(view);

        //get the edittext component within the number field
        EditText editText = getEditText();
        //get the contact pick button within the number field
        mContactPickButton = (ImageButton) view.findViewById(R.id.select_contact);

        //setup number entry
        if (editText != null) {
            // see if there is a means to get a default number,
            // and set it accordingly.
            if (mGetDefaultNumberListener != null) {
                String defaultNumber = mGetDefaultNumberListener.onGetDefaultNumber(this);
                if (defaultNumber != null) {
                    mPhoneNumber = defaultNumber;
                }
            }
            editText.setText(mPhoneNumber);
            editText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            editText.setKeyListener(DialerKeyListener.getInstance());
            editText.setOnFocusChangeListener(mDialogFocusChangeListener);
        }

        //set contact picker
        if (mContactPickButton != null) {
            mContactPickButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mParentActivity != null) {
                        mParentActivity.startActivityForResult(mContactListIntent, mPrefId);
                    }
                }
            });
        }
    }

    /**
     * Overriding EditTextPreference's onAddEditTextToDialogView.
     *
     * This method attaches the EditText to the container specific to this
     * preference's dialog layout.
     */
    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {

        // look for the container object
        ViewGroup container = (ViewGroup) dialogView
                .findViewById(R.id.edit_container);

        // add the edittext to the container.
        if (container != null) {
            container.addView(editText, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    //control the appearance of the dialog depending upon the mode.
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // modified so that we just worry about the buttons being
        // displayed, since there is no need to hide the edittext
        // field anymore.
        if (mConfirmationMode == CM_ACTIVATION) {
            if (mChecked) {
                builder.setPositiveButton(mChangeNumberText, this);
                builder.setNeutralButton(mDisableText, this);
            } else {
                builder.setPositiveButton(null, null);
                builder.setNeutralButton(mEnableText, this);
            }
        }
        // set the call icon on the title.
        builder.setIcon(R.mipmap.ic_launcher_phone);
    }


    /*
     * Listeners and other state setting methods
     */
    //set the on focus change listener to be assigned to the Dialog's edittext field.
    public void setDialogOnFocusChangeListener(View.OnFocusChangeListener l) {
        mDialogFocusChangeListener = l;
    }

    //set the listener to be called wht the dialog is closed.
    public void setDialogOnClosedListener(OnDialogClosedListener l) {
        mDialogOnClosedListener = l;
    }

    //set the link back to the parent activity, so that we may run the contact picker.
    public void setParentActivity(Activity parent, int identifier) {
        mParentActivity = parent;
        mPrefId = identifier;
        mGetDefaultNumberListener = null;
    }

    //set the link back to the parent activity, so that we may run the contact picker.
    //also set the default number listener.
    public void setParentActivity(Activity parent, int identifier, GetDefaultNumberListener l) {
        mParentActivity = parent;
        mPrefId = identifier;
        mGetDefaultNumberListener = l;
    }

    /*
     * Notification handlers
     */
    //Notify the preference that the pick activity is complete.
    public void onPickActivityResult(String pickedValue) {
        EditText editText = getEditText();
        if (editText != null) {
            editText.setText(pickedValue);
        }
    }

    //called when the dialog is clicked.
    @Override
    public void onClick(DialogInterface dialog, int which) {
        // The neutral button (button3) is always the toggle.
        if ((mConfirmationMode == CM_ACTIVATION) && (which == DialogInterface.BUTTON_NEUTRAL)) {
            //flip the toggle if we are in the correct mode.
            setToggled(!isToggled());
        }
        // record the button that was clicked.
        mButtonClicked = which;
        super.onClick(dialog, which);
    }

    @Override
    //When the dialog is closed, perform the relevant actions, including setting
    // phone numbers and calling the close action listener.
    protected void onDialogClosed(boolean positiveResult) {
        // A positive result is technically either button1 or button3.
        if ((mButtonClicked == DialogInterface.BUTTON_POSITIVE) ||
                (mButtonClicked == DialogInterface.BUTTON_NEUTRAL)){
            setPhoneNumber(getEditText().getText().toString());
            super.onDialogClosed(positiveResult);
            setText(getStringValue());
        } else {
            super.onDialogClosed(positiveResult);
        }

        // send the clicked button over to the listener.
        if (mDialogOnClosedListener != null) {
            mDialogOnClosedListener.onDialogClosed(this, mButtonClicked);
        }
    }


    /*
     * Toggle handling code.
     */
    //return the toggle value.
    public boolean isToggled() {
        return mChecked;
    }

    //set the toggle value.
    // return the current preference to allow for chaining preferences.
    public EditPhoneNumberPreference setToggled(boolean checked) {
        mChecked = checked;
        setText(getStringValue());
        notifyChanged();

        return this;
    }


    /**
     * Phone number handling code
     */
    public String getPhoneNumber() {
        // return the phone number, after it has been stripped of all
        // irrelevant text.
        return PhoneNumberUtils.stripSeparators(mPhoneNumber);
    }

    /** The phone number including any formatting characters */
    protected String getRawPhoneNumber() {
        return mPhoneNumber;
    }

    //set the phone number value.
    // return the current preference to allow for chaining preferences.
    public EditPhoneNumberPreference setPhoneNumber(String number) {
        mPhoneNumber = number;
        setText(getStringValue());
        notifyChanged();

        return this;
    }


    /*
     * Other code relevant to preference framework
     */
    //when setting default / initial values, make sure we're setting things correctly.
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValueFromString(restoreValue ? getPersistedString(getStringValue())
                : (String) defaultValue);
    }

    /**
     * Decides how to disable dependents.
     */
    @Override
    public boolean shouldDisableDependents() {
        // There is really only one case we care about, but for consistency
        // we fill out the dependency tree for all of the cases.  If this
        // is in activation mode (CF), we look for the encoded toggle value
        // in the string.  If this in confirm mode (VM), then we just
        // examine the number field.
        // Note: The toggle value is stored in the string in an encoded
        // manner (refer to setValueFromString and getStringValue below).
        boolean shouldDisable = false;
        if ((mConfirmationMode == CM_ACTIVATION) && (mEncodedText != null)) {
            String[] inValues = mEncodedText.split(":", 2);
            shouldDisable = inValues[0].equals(VALUE_ON);
        } else {
            shouldDisable = (TextUtils.isEmpty(mPhoneNumber) && (mConfirmationMode == CM_CONFIRM));
        }
        return shouldDisable;
    }

    /**
     * Override persistString so that we can get a hold of the EditTextPreference's
     * text field.
     */
    private String mEncodedText = null;
    @Override
    protected boolean persistString(String value) {
        mEncodedText = value;
        return super.persistString(value);
    }


    /*
     * Summary On handling code
     */
    //set the Summary for the on state (relevant only in CM_ACTIVATION mode)
    public EditPhoneNumberPreference setSummaryOn(CharSequence summary) {
        mSummaryOn = summary;
        if (isToggled()) {
            notifyChanged();
        }
        return this;
    }

    //set the Summary for the on state, given a string resource id
    // (relevant only in CM_ACTIVATION mode)
    public EditPhoneNumberPreference setSummaryOn(int summaryResId) {
        return setSummaryOn(getContext().getString(summaryResId));
    }

    //get the summary string for the on state
    public CharSequence getSummaryOn() {
        return mSummaryOn;
    }


    /*
     * Summary Off handling code
     */
    //set the Summary for the off state (relevant only in CM_ACTIVATION mode)
    public EditPhoneNumberPreference setSummaryOff(CharSequence summary) {
        mSummaryOff = summary;
        if (!isToggled()) {
            notifyChanged();
        }
        return this;
    }

    //set the Summary for the off state, given a string resource id
    // (relevant only in CM_ACTIVATION mode)
    public EditPhoneNumberPreference setSummaryOff(int summaryResId) {
        return setSummaryOff(getContext().getString(summaryResId));
    }

    //get the summary string for the off state
    public CharSequence getSummaryOff() {
        return mSummaryOff;
    }


    /*
     * Methods to get and set from encoded strings.
     */
    //set the values given an encoded string.
    protected void setValueFromString(String value) {
        String[] inValues = value.split(":", 2);
        setToggled(inValues[0].equals(VALUE_ON));
        setPhoneNumber(inValues[1]);
    }

    //retrieve the state of this preference in the form of an encoded string
    protected String getStringValue() {
        return ((isToggled() ? VALUE_ON : VALUE_OFF) + VALUE_SEPARATOR + getPhoneNumber());
    }

    /**
     * Externally visible method to bring up the dialog.
     *
     * Generally used when we are navigating the user to this preference.
     */
    public void showPhoneNumberDialog() {
        showDialog(null);
    }
}

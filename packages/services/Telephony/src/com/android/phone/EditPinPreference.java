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

import android.app.AlertDialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import java.util.Map;

/**
 * Class similar to the com.android.settings.EditPinPreference
 * class, with a couple of modifications, including a different layout 
 * for the dialog.
 */
public class EditPinPreference extends EditTextPreference {

    private boolean shouldHideButtons;
    
    interface OnPinEnteredListener {
        void onPinEntered(EditPinPreference preference, boolean positiveResult);
    }
    
    private OnPinEnteredListener mPinListener;

    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        mPinListener = listener;
    }
    
    public EditPinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditPinPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    /**
     * Overridden to setup the correct dialog layout, as well as setting up 
     * other properties for the pin / puk entry field.
     */
    @Override
    protected View onCreateDialogView() {
        // set the dialog layout
        setDialogLayoutResource(R.layout.pref_dialog_editpin);
        
        View dialog = super.onCreateDialogView();

        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        return dialog;
    }
    
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        
        // If the layout does not contain an edittext, hide the buttons.
        shouldHideButtons = (view.findViewById(android.R.id.edit) == null);
    }
    
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        
        // hide the buttons if we need to.
        if (shouldHideButtons) {
            builder.setPositiveButton(null, this);
            builder.setNegativeButton(null, this);
        }
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mPinListener != null) {
            mPinListener.onPinEntered(this, positiveResult);
        }
    }
    
    /**
     * Externally visible method to bring up the dialog to 
     * for multi-step / multi-dialog requests (like changing 
     * the SIM pin). 
     */
    public void showPinDialog() {
        showDialog(null);
    }
}

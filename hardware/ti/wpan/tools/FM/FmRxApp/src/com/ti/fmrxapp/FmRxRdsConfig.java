/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.fmrxapp;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.widget.CompoundButton;

public class FmRxRdsConfig extends Activity implements View.OnKeyListener,
        View.OnClickListener, CompoundButton.OnCheckedChangeListener,
        FmRxAppConstants {

    public static final String TAG = "FmRxRdsConfig";

    /********************************************
     * Widgets
     ********************************************/
    private Button btnCancel, btnOk;
    private Spinner spnBand, spnDeEmp, spnRdsSystem, spnMode,
            spnChannelSpacing;;
    private EditText textRssi;
    private CheckBox chbRdsMode;
    private CheckBox chbSetRdsAf;
    private ArrayAdapter<String> bandAdapter;
    private ArrayAdapter<String> channelSpaceAdapter;
    private ArrayAdapter<String> deEmpAdapter;
    private ArrayAdapter<String> rdsSystemAdapter;
    private ArrayAdapter<String> modeAdapter;
    private ArrayAdapter<String> emptyAdapter;
    private ArrayList<String> channelSpaceString = new ArrayList<String>();

    private ArrayList<String> bandString = new ArrayList<String>();
    private ArrayList<String> deEmpStrings = new ArrayList<String>();
    private ArrayList<String> rdsSystemStrings = new ArrayList<String>();
    private ArrayList<String> emptyStrings = new ArrayList<String>();
    private ArrayList<String> modeStrings = new ArrayList<String>();

    /********************************************
     * private variables
     ********************************************/
    private Context mContext;

    /********************************************
     * public variables
     ********************************************/
    public SharedPreferences fmConfigPreferences;

    /** Called when the activity is first created. */

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fmrxrdsconfig);
        initControl();
        setSpinners();
        setRdsSystemSpinner();

    }

    /** Initialise the Widget controls of the Activity */
    private void initControl() {
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnKeyListener(this);
        btnCancel.setOnClickListener(this);

        btnOk = (Button) findViewById(R.id.btnOk);
        btnOk.setOnKeyListener(this);
        btnOk.setOnClickListener(this);

        spnRdsSystem = (Spinner) findViewById(R.id.spnRdsSystem);
        spnBand = (Spinner) findViewById(R.id.spnBand);
        //spnDeEmp = (Spinner) findViewById(R.id.spnEmp);
        spnMode = (Spinner) findViewById(R.id.spnMode);

        spnChannelSpacing = (Spinner) findViewById(R.id.spnChannelSpace);

        textRssi = (EditText) findViewById(R.id.Rssi);

        chbRdsMode = (CheckBox) findViewById(R.id.chbRdsmode);
        chbRdsMode.setOnCheckedChangeListener(this);

        chbSetRdsAf = (CheckBox) findViewById(R.id.chbSetRdsAf);
        chbSetRdsAf.setOnCheckedChangeListener(this);
    }

    /**
     * sets the Band , De-Emp Filter and Mode option selections Spinner for the
     * User
     */
    private void setSpinners() {
        // BAnd Spinner
        bandAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, bandString);

        bandAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnBand.setAdapter(bandAdapter);
        bandAdapter.add("European");
        bandAdapter.add("Japanese");
        spnBand.setOnItemSelectedListener(gItemSelectedHandler);

        // ChannelSpace Spinner
        channelSpaceAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, channelSpaceString);

        channelSpaceAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnChannelSpacing.setAdapter(channelSpaceAdapter);
        channelSpaceAdapter.add("50 KHZ");
        channelSpaceAdapter.add("100 KHZ");
        channelSpaceAdapter.add("200 KHZ");
        spnChannelSpacing.setOnItemSelectedListener(gItemSelectedHandler);

        // De-Emp Spinner
        /*deEmpAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, deEmpStrings);

        deEmpAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDeEmp.setAdapter(deEmpAdapter);
        deEmpAdapter.add("0 sec");
        deEmpAdapter.add("50 sec");
        deEmpAdapter.add("75 sec");
        spnDeEmp.setOnItemSelectedListener(gItemSelectedHandler);
*/
        // Mode(Mono/Stereo) Spinner
        modeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, modeStrings);

        modeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnMode.setAdapter(modeAdapter);
        modeAdapter.add("STEREO");
        modeAdapter.add("MONO");
        spnMode.setOnItemSelectedListener(gItemSelectedHandler);

    }

    /** spinner to select Rds System option */
    private void setRdsSystemSpinner() {

        rdsSystemAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, rdsSystemStrings);

        rdsSystemAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnRdsSystem.setAdapter(rdsSystemAdapter);
        rdsSystemAdapter.clear();
        rdsSystemAdapter.add("RDB");
        //rdsSystemAdapter.add("RBDS");
        spnRdsSystem.setOnItemSelectedListener(gItemSelectedHandler);

    }

    /** Spinner with no options */
    private void setEmptySpinner() {
        emptyAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, emptyStrings);

        emptyAdapter
                .setDropDownViewResource(android.R.layout.select_dialog_item);
        spnRdsSystem.setAdapter(emptyAdapter);
        emptyAdapter.clear();
        emptyAdapter.add(" RDS is Disabled ");
        spnRdsSystem.setOnItemSelectedListener(gItemSelectedHandler);

    }

    public OnItemSelectedListener gItemSelectedHandler = new OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> arg0, View view, int arg2,
                long arg3) {

        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }

    };

    /** Pops up the alert Dialog */
    public void showAlert(Context context, String title, String msg) {

        new AlertDialog.Builder(context).setTitle(title).setIcon(
                android.R.drawable.ic_dialog_alert).setMessage(msg)
                .setNegativeButton(android.R.string.ok, null).show();

    }

    public void onClick(View view) {
        Log.i(TAG, "onClick()");
        int id = view.getId();
        switch (id) {
        case R.id.btnCancel:
            finish();
            break;
        case R.id.btnOk:
            savePrefernces();
            break;

        default:
            break;
        }
    }

    /** Send the Intent to Parent Activity */
    private void sendRdsIntent() {

        Intent rdsIntent = new Intent();
        setResult(RESULT_OK, rdsIntent);
        finish();

    }

    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        Log.i(TAG, "onCheckedChanged()");
        int id = view.getId();
        switch (id) {
        case R.id.chbSetRdsAf:
            break;

        case R.id.chbRdsmode:

            if (isChecked) {
                chbSetRdsAf.setEnabled(true);
                setRdsSystemSpinner();
            } else {
                chbSetRdsAf.setChecked(false);
                chbSetRdsAf.setEnabled(false);
                setEmptySpinner();
            }
            break;

        default:
            break;
        }

    }

    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        int action = keyEvent.getAction();

        if (keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            Log.v(TAG, "KEYCODE_SOFT_RIGHT ");
            finish();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
            Log.v(TAG, "KEYCODE_SOFT_LEFT ");
            savePrefernces();
            // finish();
            return true;
        }

        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER
                && keyCode != KeyEvent.KEYCODE_DPAD_UP
                && keyCode != KeyEvent.KEYCODE_DPAD_DOWN
                && keyCode != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        if (action == KeyEvent.ACTION_UP) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:

                break;

            case KeyEvent.KEYCODE_DPAD_UP:

                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:

                break;
            }
        }
        return true;
    }

    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()-Entered");
        updateUiFromPreference();
    }

    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()-Entered");

    }

    public void onStop() {
        Log.i(TAG, "onStop()-Entered");
        super.onStop();
    }

    public void onRestart() {
        Log.i(TAG, "onRestart()-Entered");
        super.onRestart();

    }

    /** Updates the UI with Default/last saved values */
    private void updateUiFromPreference() {
        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",
                MODE_PRIVATE);
        Log.i(TAG, "updateUiFromPreference()");

        chbRdsMode.setChecked(fmConfigPreferences.getBoolean(RDS, DEFAULT_RDS));
        boolean rdsON = fmConfigPreferences.getBoolean(RDS, DEFAULT_RDS);

        if (!rdsON) // Rds is Disabled
        {
            chbSetRdsAf.setChecked(false); // When the RDS is Disabled uncheck
            // Rds Af checkbox
            chbSetRdsAf.setEnabled(false); // When the RDS is Disabled disable
            // Rds Af checkbox
            setEmptySpinner(); // When the RDS is Disabled, disable RDS System
            // spinner
        } else// Rds Is Enable
        {
            chbSetRdsAf.setChecked(fmConfigPreferences.getBoolean(RDSAF,
                    DEFAULT_RDS_AF));
            spnRdsSystem.setSelection(fmConfigPreferences.getInt(RDSSYSTEM,
                    DEFAULT_RDS_SYSTEM));
        }

        textRssi.setText(fmConfigPreferences.getString(RSSI_STRING,
                DEF_RSSI_STRING));

        spnBand.setSelection(fmConfigPreferences.getInt(BAND, DEFAULT_BAND));
        //spnDeEmp.setSelection(fmConfigPreferences.getInt(DEEMP, DEFAULT_DEEMP));
        spnMode.setSelection(fmConfigPreferences.getInt(MODE, DEFAULT_MODE));

        int pos = 1;
        switch (fmConfigPreferences.getInt(CHANNELSPACE, DEFAULT_CHANNELSPACE)) {

        case 1:
            pos = 0;
            break;

        case 2:
            pos = 1;
            break;

        case 4:
            pos = 2;
            break;

        }
        spnChannelSpacing.setSelection(pos);

    }

    /** Saves Configuration settings in the Shared Preference */
    private void savePrefernces() {
        Log.i(TAG, "savePrefernces()");

        int mChannelSpacePos = 2;

        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",
                MODE_PRIVATE);

        SharedPreferences.Editor editor = fmConfigPreferences.edit();

        if (chbRdsMode.isChecked()) {
            editor.putBoolean(RDSAF, chbSetRdsAf.isChecked());
            editor.putBoolean(RDS, chbRdsMode.isChecked());
            editor.putInt(RDSSYSTEM, spnRdsSystem.getSelectedItemPosition());
        } else {
            editor.putBoolean(RDSAF, DEFAULT_RDS_AF);
            editor.putBoolean(RDS, DEFAULT_RDS);
            editor.putInt(RDSSYSTEM, DEFAULT_RDS_SYSTEM);
        }

        editor.putInt(BAND, spnBand.getSelectedItemPosition());

        //editor.putInt(DEEMP, spnDeEmp.getSelectedItemPosition());

        editor.putInt(MODE, spnMode.getSelectedItemPosition());

        switch (spnChannelSpacing.getSelectedItemPosition()) {
        case 0:
            mChannelSpacePos = 1;
            break;

        case 1:
            mChannelSpacePos = 2;
            break;

        case 2:
            mChannelSpacePos = 4;
            break;
        }

        editor.putInt(CHANNELSPACE, mChannelSpacePos);

        try {
            int rssiValue = Integer.parseInt(textRssi.getText().toString());
            boolean valid = rssiValid(rssiValue);
            if (valid || (textRssi.getText().toString() == null)) {
                editor.putString(RSSI_STRING, textRssi.getText().toString());
                if (textRssi.getText().toString() == null)
                    editor.putInt(RSSI, DEFAULT_RSSI);
                else {
                    editor.putInt(RSSI, rssiValue);
                }
                editor.commit();
                sendRdsIntent();
            } else {
                new AlertDialog.Builder(this).setIcon(
                        android.R.drawable.ic_dialog_alert).setMessage(
                        "Enter valid RSSI value in range 1-127!!")
                        .setNegativeButton(android.R.string.ok, null).show();
                textRssi.setText(null);
            }
        } catch (NumberFormatException nfe) {
            Log.d(TAG, "NumberFormatException:" + nfe.getMessage());
            new AlertDialog.Builder(this).setIcon(
                    android.R.drawable.ic_dialog_alert).setMessage(
                    "Enter valid RSSI value in range 1-127!!")
                    .setNegativeButton(android.R.string.ok, null).show();
            textRssi.setText(null);
        }

    }

    /** Checks the RSSI value for validity */
    private boolean rssiValid(int value) {
        //Log.d(TAG, "rssiValid " + value);
        if (value < RSSI_MIN || value > RSSI_MAX) {
            Log.d(TAG, "TAG,rssiValid %d." + value);

            return false;
        } else
            return true;

    }

}

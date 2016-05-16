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

package com.ti.fmtxapp;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.widget.CompoundButton;

public class FmTxConfig extends Activity implements  View.OnKeyListener,
View.OnClickListener,CompoundButton.OnCheckedChangeListener,FmTxAppConstants
{

    public static final String TAG = "FmRxRdsConfig";

    /********************************************
    * Widgets
    ********************************************/
    private Button btnCancel,btnOk;
    private Spinner spnMonoStereo/*spnEcc,spnRepetoire,spnTxMode,spnDisplayMode*/;
    private EditText txtPsStr,textPower,edtPiCode,edtAfCode;
    private EditText edtRadioText,edtPty;
    private CheckBox rbtnPs,rbtnRt;
/*    private CheckBox chbRdsMode;
    private CheckBox chbSetRdsAf;*/
    //private RadioButton rbtnTp,rbtnTa;
    private ArrayAdapter<String> monoStereoAdapter;
//        private ArrayAdapter<String> eccAdapter;
        /*private ArrayAdapter<String> repertoireAdapter;
    private ArrayAdapter<String> txModeAdapter;
        private ArrayAdapter<String> emptyAdapter;

        private ArrayAdapter<String> displayAdapter;        */
    private ArrayList<String> monoStereoStrings = new ArrayList<String>();
    private ArrayList<String> eccStrings = new ArrayList<String>();
    /*private ArrayList<String> repertoireStrings = new ArrayList<String>();
    private ArrayList<String> emptyStrings = new ArrayList<String>();
    private ArrayList<String> txModeStrings = new ArrayList<String>();
    private ArrayList<String> displayStrings = new ArrayList<String>();    */


    /********************************************
    * private variables
    ********************************************/
    private Context mContext;

    /********************************************
    * public variables
    ********************************************/
    public SharedPreferences fmConfigPreferences;

        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.fmtxrdsconfig);
          initControl();
          setSpinners();


        }

    /** Initialise the Widget controls of the Activity*/
    private void initControl()
        {
      btnCancel = (Button) findViewById(R.id.btnCancel);
       btnCancel.setOnKeyListener(this);
       btnCancel.setOnClickListener(this);

       btnOk = (Button) findViewById(R.id.btnOk);
       btnOk.setOnKeyListener(this);
       btnOk.setOnClickListener(this);

    /*    spnDisplayMode = (Spinner)findViewById(R.id.spnDisplayMode);
       spnTxMode = (Spinner)findViewById(R.id.spnTxMode);
       spnRepetoire = (Spinner)findViewById(R.id.spnRepetoire);*/
       spnMonoStereo= (Spinner)findViewById(R.id.spnMonoStereo);
      // spnEcc= (Spinner)findViewById(R.id.spnEcc);

       txtPsStr = (EditText)findViewById(R.id.EdtPSString);
    textPower = (EditText)findViewById(R.id.powLevel);

    edtPiCode = (EditText)findViewById(R.id.piCode);
    edtAfCode = (EditText)findViewById(R.id.afCode);
    //rbtnTp=(RadioButton)findViewById(R.id.rbtnTp);
    //rbtnTp.setOnCheckedChangeListener(this);

    //rbtnTa=(RadioButton)findViewById(R.id.rbtnTa);
    //rbtnTa.setOnCheckedChangeListener(this);


    //Edit Box
    edtRadioText = (EditText)findViewById(R.id.EdtRadioText);
    edtPty = (EditText)findViewById(R.id.EdtPty);

    rbtnPs = (CheckBox)findViewById(R.id.rbtnPs);
    rbtnRt = (CheckBox)findViewById(R.id.rbtnRt);

    rbtnPs.setOnCheckedChangeListener(this);
    rbtnRt.setOnCheckedChangeListener(this);

  }


    /** sets the Band , De-Emp Filter and Mode option  selections Spinner for the User*/
     private void setSpinners()
     {
         // Tx Mode Spinner
      /*    txModeAdapter = new ArrayAdapter<String>(this,
                       android.R.layout.simple_spinner_item, txModeStrings);

         txModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         spnTxMode.setAdapter(txModeAdapter);
         txModeAdapter.add("Manual");
         txModeAdapter.add("Automatic");
         spnTxMode.setOnItemSelectedListener(gItemSelectedHandler);*/


         // Mono/Stereo Spinner
         monoStereoAdapter = new ArrayAdapter<String>(this,
                       android.R.layout.simple_spinner_item, monoStereoStrings);

         monoStereoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         spnMonoStereo.setAdapter(monoStereoAdapter);
         //monoStereoAdapter.add("Mono");
         monoStereoAdapter.add("Stereo");
         spnMonoStereo.setOnItemSelectedListener(gItemSelectedHandler);


         // Ecc Spinner
         //eccAdapter = new ArrayAdapter<String>(this,                   android.R.layout.simple_spinner_item, eccStrings);

         //eccAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         //spnEcc.setAdapter(eccAdapter);
         //eccAdapter.add("0");
         //spnEcc.setOnItemSelectedListener(gItemSelectedHandler);


         // Repetoire Spinner
      /*    repertoireAdapter = new ArrayAdapter<String>(this,
                       android.R.layout.simple_spinner_item, repertoireStrings);

         repertoireAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         spnRepetoire.setAdapter(repertoireAdapter);
         repertoireAdapter.add("G0");
         repertoireAdapter.add("G1");
         repertoireAdapter.add("G2");
         spnRepetoire.setOnItemSelectedListener(gItemSelectedHandler);*/



         // Display Mode
        /*  displayAdapter = new ArrayAdapter<String>(this,
                       android.R.layout.simple_spinner_item, displayStrings);

         displayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         spnDisplayMode.setAdapter(displayAdapter);
         displayAdapter.add("Static");
         displayAdapter.add("Scroll");
         spnDisplayMode.setOnItemSelectedListener(gItemSelectedHandler);*/


    }



    public OnItemSelectedListener gItemSelectedHandler = new OnItemSelectedListener()
    {
//        @Override
        public void onItemSelected(AdapterView<?> arg0, View view, int arg2,long arg3) {
            Log.d(TAG,"mono selected"+arg0.getSelectedItemPosition() );



            // TODO Auto-generated method stub

        }

//        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    };

    /** Pops up the alert Dialog */
        public void showAlert(Context context,String title,String msg)
    {

       new AlertDialog.Builder(context)
       .setTitle(title)
       .setIcon(android.R.drawable.ic_dialog_alert)
       .setMessage(msg)
       .setNegativeButton(android.R.string.ok, null)
       .show();

    }

    //@Override
    public void onClick(View view)
    {
         Log.i(TAG,"onClick()");
      int id = view.getId();
      switch (id)
    {


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
    private void sendAdvanceConfigIntent(){

        Intent rdsIntent = new Intent();
        setResult(RESULT_OK, rdsIntent);
             finish();

    }



    /** Send the Intent to Parent Activity */
    private void sendRdsIntent(){

        Intent rdsIntent = new Intent();
        setResult(RESULT_OK, rdsIntent);
             finish();

    }



    public void onCheckedChanged(CompoundButton view, boolean isChecked)
    {
              Log.i(TAG,"onCheckedChanged()");
        int id = view.getId();
   switch(id)
        {
         case R.id.rbtnRt:

             Log.i(TAG,"onCheckedChanged() rbtnRt isChecked " +isChecked);

             break;
         case R.id.rbtnPs:

             Log.i(TAG,"onCheckedChanged() rbtnPs isChecked" +isChecked);
             }

    }

    //@Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
       int action = keyEvent.getAction();

       if (keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
           Log.v(TAG, "KEYCODE_SOFT_RIGHT " );
           finish();
          return true;
       }

       if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
            Log.v(TAG, "KEYCODE_SOFT_LEFT " );
            savePrefernces();
            //finish();
            return true;
         }


       if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
          keyCode != KeyEvent.KEYCODE_DPAD_UP &&
          keyCode != KeyEvent.KEYCODE_DPAD_DOWN &&
          keyCode != KeyEvent.KEYCODE_ENTER) {
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

    public void onResume(){
        super.onResume();
        Log.i(TAG,"onResume()-Entered");
        updateUiFromPreference();
    }

    public void onPause(){
        super.onPause();
        Log.i(TAG,"onPause()-Entered");

    }

    public void onStop(){
        Log.i(TAG,"onStop()-Entered");
        super.onStop();
    }

    public void onRestart(){
        Log.i(TAG,"onRestart()-Entered");
        super.onRestart();

    }


    /** Updates the UI with Default/last saved values */
    private void updateUiFromPreference() {
        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",
                MODE_PRIVATE);
        Log.i(TAG, "updateUiFromPreference()");

        edtRadioText.setText(fmConfigPreferences.getString(RT_STRING,DEF_RT_STRING));
        edtPty.setText(fmConfigPreferences.getString(PTY_STRING,DEF_PTY_STRING));
        edtPiCode.setText(fmConfigPreferences.getString(PICODE_STRING,DEFAULT_PICODE_STRING));

           rbtnPs.setChecked(fmConfigPreferences.getBoolean(PSENABLED,false));
           rbtnRt.setChecked(fmConfigPreferences.getBoolean(RTENABLED,false));

            Log.i(TAG,"updateUiFromPreference() --> PSENABLED = "+fmConfigPreferences.getBoolean(PSENABLED,false));
            Log.i(TAG,"updateUiFromPreference() --> RTENABLED = "+fmConfigPreferences.getBoolean(RTENABLED,false));

        textPower.setText(fmConfigPreferences.getString(POWER_STRING,DEF_POWER_STRING));
        txtPsStr.setText(fmConfigPreferences.getString(PS_STRING,DEF_PS_STRING));
        Log.i(TAG,"updateUiFromPreference() --> PS_STRING = "+txtPsStr.getText().toString());
        edtAfCode.setText(fmConfigPreferences.getString(AFCODE_STRING,DEFAULT_AFCODE_STRING));

        /*spnDisplayMode.setSelection(fmConfigPreferences.getInt(DISPLAY_MODE, DEFAULT_DISPLAYMODE));
        spnTxMode.setSelection(fmConfigPreferences.getInt(TX_MODE, DEFAULT_TXMODE));
        spnRepetoire.setSelection(fmConfigPreferences.getInt(REPERTOIRE, DEFAULT_REPERTOIRE));*/
        spnMonoStereo.setSelection(fmConfigPreferences.getInt(MONO_STEREO, DEFAULT_MONOSTEREO));
        //spnEcc.setSelection(fmConfigPreferences.getInt(ECC, DEFAULT_ECC));

        /*if(spnTxMode.getSelectedItemPosition() == 0) // If Tx Mode is MANUAL, Disable AF Code & PS Strg
        {
         edtAfCode.setEnabled(false);
         txtPsStr.setEnabled(false);
        }else{
         edtAfCode.setEnabled(true);
         txtPsStr.setEnabled(true);

        }*/

    }


    /** Saves Configuration  settings  in the Shared Preference */
    private void savePrefernces(){
        Log.i(TAG,"savePrefernces()");

        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",MODE_PRIVATE);

        SharedPreferences.Editor editor = fmConfigPreferences.edit();


       /*editor.putInt(DISPLAY_MODE, spnDisplayMode.getSelectedItemPosition());
        editor.putInt(TX_MODE, spnTxMode.getSelectedItemPosition());
        editor.putInt(REPERTOIRE, spnRepetoire.getSelectedItemPosition());*/
        editor.putInt(MONO_STEREO, spnMonoStereo.getSelectedItemPosition());
        //editor.putInt(ECC, spnEcc.getSelectedItemPosition());

        editor.putString(PS_STRING, txtPsStr.getText().toString());
        editor.putString(RT_STRING, edtRadioText.getText().toString());
        editor.putString(PTY_STRING, edtPty.getText().toString());
        editor.putString(PICODE_STRING, edtPiCode.getText().toString());
        editor.putBoolean(PSENABLED,rbtnPs.isChecked());
        editor.putBoolean(RTENABLED,rbtnRt.isChecked());

        editor.commit();
        Log.i(TAG,"savePrefernces() --> PS_STRING = "+txtPsStr.getText().toString());
        Log.i(TAG,"savePrefernces() --> RT_STRING = "+ edtRadioText.getText().toString());

        savePowerLevel();
        savePtyCode();
        sendAdvanceConfigIntent();
        savePiCode();
        saveAfCode();
    }

    /** Checks the Power value for validity */
    private boolean ptyValid(int value){
        Log.d(TAG,"ptyValid %d." +value );
        if (value < PTY_MIN || value > PTY_MAX)
        {
            Log.d(TAG,"TAG,ptyValid %d." +value );

            return false;
        }
        else
        return true;

    }

/*********************************************************************

    savePtyCode()

*********************************************************************/
    private void savePtyCode()
    {
        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",MODE_PRIVATE);

        SharedPreferences.Editor editor = fmConfigPreferences.edit();

         try{
             int ptyValue = Integer.parseInt(edtPty.getText().toString());
          boolean valid = ptyValid(ptyValue);
            if(valid || (edtPty.getText().toString()== null))
            {
                editor.putString(PTY_STRING, edtPty.getText().toString());
                if(edtPty.getText().toString()== null)
                editor.putInt(PTY, DEFAULT_PTY);
            else
            {
                editor.putInt(PTY, ptyValue);
            }
                editor.commit();
            //sendAdvanceConfigIntent();
            }
            else
        {
           new AlertDialog.Builder(this)
           .setIcon(android.R.drawable.ic_dialog_alert)
           .setMessage("Enter valid Pty value in range 0-31!!")
           .setNegativeButton(android.R.string.ok, null)
           .show();
           edtPty.setText(null);
        }
         }
         catch(NumberFormatException nfe)
         {
             Log.d(TAG,"--> NumberFormatException:" + nfe.getMessage());
             new AlertDialog.Builder(this)
             .setIcon(android.R.drawable.ic_dialog_alert)
             .setMessage("--> Enter valid pty value in range 0-31!!")
             .setNegativeButton(android.R.string.ok, null)
             .show();
             edtPty.setText(null);
              }

    }


/*********************************************************************

    savePowerLevel()

*********************************************************************/
    private void savePowerLevel()
    {
        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",MODE_PRIVATE);

        SharedPreferences.Editor editor = fmConfigPreferences.edit();

         try{
             int powerValue = Integer.parseInt(textPower.getText().toString());
          boolean valid = powerValid(powerValue);
            if(valid || (textPower.getText().toString()== null))
            {
                editor.putString(POWER_STRING, textPower.getText().toString());
                if(textPower.getText().toString()== null)
                editor.putInt(POWER, DEFAULT_POWER);
            else
            {
                editor.putInt(POWER, powerValue);
            }
                editor.commit();
            //sendRdsIntent();
            }
            else
        {
           new AlertDialog.Builder(this)
           .setIcon(android.R.drawable.ic_dialog_alert)
           .setMessage("Enter valid Power value in range 0-31!!")
           .setNegativeButton(android.R.string.ok, null)
           .show();
           textPower.setText(null);
        }
         }
         catch(NumberFormatException nfe)
         {
             Log.d(TAG,"--> NumberFormatException:" + nfe.getMessage());
             new AlertDialog.Builder(this)
             .setIcon(android.R.drawable.ic_dialog_alert)
             .setMessage("--> Enter valid power value in range 0-31!!")
             .setNegativeButton(android.R.string.ok, null)
             .show();
             textPower.setText(null);
              }

    }



/*********************************************************************

    saveAfCode()

*********************************************************************/
    private void saveAfCode()
    {

        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",MODE_PRIVATE);

        SharedPreferences.Editor editor = fmConfigPreferences.edit();

        Log.i(TAG,"saveAfCode()");
         try{
             int afCodeValue = Integer.parseInt(edtAfCode.getText().toString());
          boolean valid = afCodeValid(afCodeValue);
            if(valid || (edtAfCode.getText().toString()== null))
            {
                editor.putString(AFCODE_STRING, edtAfCode.getText().toString());
                if(edtAfCode.getText().toString()== null)
                editor.putInt(AF_CODE, DEFAULT_AFCODE);
            else
            {
                editor.putInt(AF_CODE, afCodeValue);
            }
                editor.commit();
            //sendRdsIntent();
            }
            else
        {
           new AlertDialog.Builder(this)
           .setIcon(android.R.drawable.ic_dialog_alert)
           .setMessage("Enter valid AfCode value in range 1 - 204!!")
           .setNegativeButton(android.R.string.ok, null)
           .show();
           edtAfCode.setText(null);
        }
         }
         catch(NumberFormatException nfe)
         {
             Log.d(TAG,"--> NumberFormatException:" + nfe.getMessage());
             new AlertDialog.Builder(this)
             .setIcon(android.R.drawable.ic_dialog_alert)
             .setMessage("--> Enter valid AfCode value in range 1 - 204 !!")
             .setNegativeButton(android.R.string.ok, null)
             .show();
             edtAfCode.setText(null);
              }

    }

/*********************************************************************

    savePiCode()

*********************************************************************/
    private void savePiCode()
    {

        fmConfigPreferences = getSharedPreferences("fmConfigPreferences",MODE_PRIVATE);

        SharedPreferences.Editor editor = fmConfigPreferences.edit();

            Log.i(TAG,"savePiCode()");

         try{
             int piCodeValue = Integer.parseInt(edtPiCode.getText().toString());
             boolean valid = piCodeValid(piCodeValue);
             if(valid || (edtPiCode.getText().toString()== null))
             {
                editor.putString(PICODE_STRING, edtPiCode.getText().toString());
                if(edtPiCode.getText().toString()== null)
                editor.putInt(PI_CODE, DEFAULT_PICODE);
             else
             {
                editor.putInt(PI_CODE, piCodeValue);
             }
                editor.commit();
                sendRdsIntent();
             }
             else
        {
           new AlertDialog.Builder(this)
           .setIcon(android.R.drawable.ic_dialog_alert)
           .setMessage("Enter valid Pi Code value in range 0-65535!!")
           .setNegativeButton(android.R.string.ok, null)
           .show();
           edtPiCode.setText(null);
        }
         }
         catch(NumberFormatException nfe)
         {
             Log.d(TAG,"--> NumberFormatException:" + nfe.getMessage());
             new AlertDialog.Builder(this)
             .setIcon(android.R.drawable.ic_dialog_alert)
             .setMessage("--> Enter valid Pi Code value in range 0-65535!!")
             .setNegativeButton(android.R.string.ok, null)
             .show();
             edtPiCode.setText(null);
              }

    }


    /** Checks the Power value for validity */
    private boolean powerValid(int value){
        Log.d(TAG,"powerValid %d." +value );
        if (value < POWER_MIN || value > POWER_MAX)
        {
            Log.d(TAG,"TAG,powerValid %d." +value );

            return false;
        }
        else
        return true;

    }

    /** Checks the AF code value for validity */
    private boolean afCodeValid(int value){
        Log.d(TAG,"afCodeValid %d." +value );
        if ((value < AFCODE_MIN || value > AFCODE_MAX))
        {
            if(value != DEFAULT_AFCODE){
            Log.d(TAG,"TAG,afCodeValid %d." +value );
            return false;
            }else{
            return true;
            }
            //return false;
        }
        else
        return true;

    }

    /** Checks the pi Code value for validity */
    private boolean piCodeValid(int value){
        Log.d(TAG,"piCodeValid %d." +value );
        if (value < PICODE_MIN || value > PICODE_MAX)
        {
            Log.d(TAG,"TAG,piCodeValid %d." +value );

            return false;
        }
        else
        return true;

    }
}

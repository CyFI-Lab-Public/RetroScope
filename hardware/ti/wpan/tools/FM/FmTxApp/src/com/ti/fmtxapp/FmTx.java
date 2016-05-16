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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View.OnKeyListener;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.DialogInterface;

/*************** For calling lower layer********************/
import com.ti.fm.FmRadio;
import com.ti.fm.FmRadioIntent;
import com.ti.fm.IFmConstants;
/**************************************************************/

public class FmTx extends Activity implements View.OnClickListener,
OnCheckedChangeListener,OnKeyListener,FmTxAppConstants,FmRadio.ServiceListener{
    /** Called when the activity is first created. */
    public static final String TAG = "FmTxApp";

    /********************************************
     * Menu Constants
     ********************************************/
    public static final int MENU_CONFIGURE = Menu.FIRST + 1;
//    public static final int MENU_ADVANCED = Menu.FIRST + 2;
    public static final int MENU_EXIT = Menu.FIRST + 2;
    public static final int MENU_ABOUT = Menu.FIRST + 3;


    /********************************************
     * Initail values
     ********************************************/
    private int mTxMode = INITIAL_VAL;
    private int mPiCode = INITIAL_VAL;
    private int mAfCode = INITIAL_VAL;
    private int mTuneFreq = INITIAL_VAL;
    private int mDeEmpFilter = INITIAL_VAL;
    private int mPowerLevel = INITIAL_VAL;
    private int mRepertoire = INITIAL_VAL;
    private int mMonoStereo = INITIAL_VAL;
    private int mDisplayMode = INITIAL_VAL;
    private int mEcc = INITIAL_VAL;
    private int mTxMusicSpeech = INITIAL_VAL;
    private int mTxEmpFilter = INITIAL_VAL;
    private int mTxPtyCode = INITIAL_VAL;
    private Float mFreq = (float)INITIAL_VAL;
    //private int mMute = INITIAL_VAL;
    /*Flag to check if service is connected*/
    boolean mFmServiceConnected = false;


    public SharedPreferences fmTxPreferences;
    private int mAppState = STATE_DEFAULT;

    public static Float freqValue = (float)0;
    private ProgressDialog pd = null,configPd;

        public static FmRadio sFmRadio;
    private CheckBox startTxBtn,enableRdsBtn,chbMute;
    private EditText textFreq;
    private boolean mStatus;
    private Button btnOk;
    private boolean isFmEnabled = false;

    /* Actvity result index */
    public static final int ACTIVITY_CONFIG = 1;


    private NotificationManager mNotificationManager;


    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       Log.d(TAG, "onCreate enter");
       mContext = this;


    // Register for FM TX intent broadcasts.
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(FmRadioIntent.FM_TX_ENABLED_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_TUNE_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_DISABLED_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_START_TRANSMISSION_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_STOP_TRANSMISSION_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_DESTROY_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_ENABLE_RSD_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_DISABLE_RSD_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_PS_DISPLAY_MODE_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_RDS_MUSIC_SPEECH_FLAG_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_RDS_TEXT_REPERTOIRE_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_TRANSMISSION_MODE_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_POWER_LEVEL_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_RDS_TEXT_PS_MSG_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_MONO_STEREO_MODE_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_MUTE_MODE_ACTION);
    intentFilter.addAction(FmRadioIntent.FM_TX_SET_RDS_TRANSMISSION_GROUPMASK_ACTION);
    registerReceiver(mReceiver, intentFilter);


        sFmRadio = new FmRadio(this, this);

       setContentView(R.layout.main);
    initControls();
    loadlastSaveddata();


    }

  @Override
    public void onPause() {
       super.onPause();
       Log.d(TAG, "onPause enter");
       saveLastData();
}
/******************************************
*
*        enableTx()
*
*******************************************/
       private void enableTx()
       {

       Log.i(TAG,"enableTx()");

    switch (sFmRadio.txGetFMState()){

    case STATE_DISABLED:
    case STATE_DEFAULT:
        mStatus = sFmRadio.txEnable();
        if (mStatus == false) {
            Log.i(TAG, "Fm Tx --> Cannot enable TX !!");

        CharSequence text = "Sorry!! Cannot enable TX";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(mContext, text, duration);
        toast.setGravity(android.view.Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();

        finish();

        }
        else{
            Log.i(TAG, "Fm Tx --> Enabling TX !!");
            pd = ProgressDialog.show(this, "Please wait..",    "Enabling FM TX", true, false);
        }


        break;

    case STATE_ENABLED:
             loadlastSaveddata();
        Log.i(TAG, "Fm Tx is already Enabled.");
        break;

    }


       }


public void onServiceConnected() {
    Log.i(TAG, "onServiceConnected");
    mFmServiceConnected =true;
    enableTx();
}

public void onServiceDisconnected() {
    Log.d(TAG, "Lost connection to service");
    mFmServiceConnected =false;
    sFmRadio = null;
}


/******************************************
*
*        onCreateOptionsMenu()
*
*******************************************/
        public boolean onCreateOptionsMenu(Menu menu) {

            super.onCreateOptionsMenu(menu);
            MenuItem item;

            item = menu.add(0, MENU_CONFIGURE, 0, R.string.configure);
            item.setIcon(R.drawable.configure);


            /*item = menu.add(0, MENU_ADVANCED, 0, R.string.advanced);
            item.setIcon(R.drawable.configure);*/

            item = menu.add(0, MENU_ABOUT, 0, R.string.about);
            item.setIcon(R.drawable.icon);

            item = menu.add(0, MENU_EXIT, 0, R.string.exit);
            item.setIcon(R.drawable.icon);

            return true;
        }

/******************************************
*
* onOptionsItemSelected()
*Handles item selections
*
*******************************************/

        public boolean onOptionsItemSelected(MenuItem item) {

            switch (item.getItemId()) {
            case MENU_CONFIGURE:

            /* Start the configuration window */
            Intent irds = new Intent("android.intent.action.RDSPARAM_CONFIG");
            startActivityForResult(irds, ACTIVITY_CONFIG);

                break;

     /*    case MENU_ADVANCED:
            Intent iAdvanced = new Intent("android.intent.action.TXPARAM_CONFIG");
            startActivityForResult(iAdvanced, ACTIVITY_ADVANCED);


             break;*/

            case MENU_EXIT:
                saveLastData();
                /*
                 * The exit from the FM application happens here The audio will be
                 * disabled and when he callback for this is recived, FM will be
                 * disabled
                 */
                mStatus = sFmRadio.txDisable();
                if (mStatus == false) {
                Log.i(TAG, "Fm Tx--> Cannot Disable TX !!");

                }
                else{
                Log.i(TAG, "Fm Tx--> Disabling TX !!");
                }
            break;

            case MENU_ABOUT:
                /* Start the help window */
                Intent iTxHelp = new Intent("android.intent.action.START_TXABOUT");
                startActivity(iTxHelp);
                break;


            }
            return super.onOptionsItemSelected(item);
        }


    /** Adds Delay of 2 seconds */
    private void insertDelayThread() {

        new Thread() {
            public void run() {
                try {
                    // Add some delay to make sure all configuration has been
                    // completed.
                    sleep(2000);
                } catch (Exception e) {
                    Log.e(TAG, "InsertDelayThread()-- Exception !!");
                }
                // Dismiss the Dialog
                configPd.dismiss();
            }
        }.start();

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult");
        switch (requestCode) {

/*        case (ACTIVITY_ADVANCED):{
            if (resultCode == Activity.RESULT_OK) {
                    setAdvancedConfig();
                    configPd = ProgressDialog.show(this, "Please wait..",
                            "Applying Advanced Configuration", true, false);
                    // The delay is inserted to make sure all the configurations
                    // have been completed.
                    insertDelayThread();

            }
        }
        break;

*/

        case (ACTIVITY_CONFIG): {
            if (resultCode == Activity.RESULT_OK) {
                //Log.i(TAG, "ActivityFmRdsConfig configurationState "+ configurationState);

                    setRdsConfig();
                    setAdvancedConfig();
                    configPd = ProgressDialog.show(this, "Please wait..",
                            "Applying new Configuration", true, false);
                    // The delay is inserted to make sure all the configurations
                    // have been completed.
                    insertDelayThread();

            }
        }
            break;

        }
    }




/*************************************

setAdvancedConfig()

*************************************/
private void setAdvancedConfig()
{
    Log.i(TAG, "setAdvancedConfig");
    SharedPreferences fmTxParamConfigPreferences = getSharedPreferences(                "fmConfigPreferences", MODE_PRIVATE);


    //Set Music-Speech Flag
/*    int txMusicSpeech = fmTxParamConfigPreferences.getInt(MUSIC_SPEECH, DEFAULT_MUSICSPEECH);
    Log.i(TAG,"setAdvancedConfig-- >txMusicSpeech = "+txMusicSpeech);
    if (mTxMusicSpeech != txMusicSpeech) {
        mTxMusicSpeech = txMusicSpeech;
        mStatus = sFmRadio.txSetRdsMusicSpeechFlag(txMusicSpeech);
        if (mStatus == false) {
        Log.i(TAG, "Fm Tx--> txSetRdsMusicSpeechFlag TX failed !!");
        }
        else{
        Log.i(TAG, "Fm Tx--> TX txSetRdsMusicSpeechFlag is SUCCESSFUll !!");
        }
    }
*/
    //Set Pre-Emp Filter
/*    int txEmpFilter = fmTxParamConfigPreferences.getInt(EMP_FILTER, DEFAULT_EMPFILTER);
    Log.i(TAG,"setAdvancedConfig-- >txEmpFilter = "+txMusicSpeech);
    if (mTxEmpFilter != txEmpFilter) {
        mTxEmpFilter = txEmpFilter;
        mStatus = sFmRadio.txSetPreEmphasisFilter(txEmpFilter);
        if (mStatus == false) {
        Log.i(TAG, "Fm Tx--> txSetPreEmphasisFilter TX failed !!");
        }
        else{
        Log.i(TAG, "Fm Tx--> TX txSetPreEmphasisFilter is SUCCESSFUll !!");
        }
    }
*/

    // Before Setting RT String , set the Tx group Mask

    Log.i(TAG, "txSetRdsTransmittedGroupsMask(RT)");
    mStatus = sFmRadio.txSetRdsTransmittedGroupsMask(RDS_RADIO_TRANSMITTED_GRP_RT_MASK | RDS_RADIO_TRANSMITTED_GRP_PS_MASK);
    if (mStatus == false) {
    Log.i(TAG, "Fm Tx--> txSetRdsTransmittedGroupsMask TX failed !!");
    }else{
    Log.i(TAG, "Fm Tx--> TX txSetRdsTransmittedGroupsMask is SUCCESSFUll !!");
    }

       //Set Radio Text Code

       String txRTCode = fmTxParamConfigPreferences.getString(RT_STRING, DEF_RT_STRING);
    boolean rtMask= fmTxParamConfigPreferences.getBoolean(RTENABLED,false);
    int rds_mode = 2;

       Log.i(TAG,"setAdvancedConfig-- >txRT = "+txRTCode);
       if (rtMask) {
             mStatus = sFmRadio.txSetRdsTextRtMsg(rds_mode, txRTCode, txRTCode.length());
             if (mStatus == false) {
             Log.i(TAG, "Fm Tx--> txSetRdsRadioText TX failed !!");
             }
             else{
             Log.i(TAG, "Fm Tx--> TX txSetRdsRadioText is SUCCESSFUll !!");
             }
     }else {
        txRTCode = "";
        mStatus = sFmRadio.txSetRdsTextRtMsg(rds_mode, txRTCode, txRTCode.length());
             if (mStatus == false) {
             Log.i(TAG, "Fm Tx--> txSetRdsRadioText TX failed !!");
             }
             else{
             Log.i(TAG, "Fm Tx--> TX txSetRdsRadioText is Emty!!");
             }
    }

       //Set PS Name
       String txPSName = fmTxParamConfigPreferences.getString(PS_STRING, DEF_PS_STRING);
    boolean psMask= fmTxParamConfigPreferences.getBoolean(PSENABLED,false);

       Log.i(TAG,"setAdvancedConfig-- >txPSName = "+txPSName);
       if (psMask) {
             mStatus = sFmRadio.txSetRdsTextPsMsg(txPSName);
             if (mStatus == false) {
             Log.i(TAG, "Fm Tx--> txSetRdsPSName TX failed !!");
             }
             else{
             Log.i(TAG, "Fm Tx--> TX txSetRdsPSName is SUCCESSFUll !!");
             }
     }else {
        txPSName = "";
             mStatus = sFmRadio.txSetRdsTextPsMsg(txPSName);
             if (mStatus == false) {
             Log.i(TAG, "Fm Tx--> txSetRdsPSName TX failed !!");
             }
             else{
             Log.i(TAG, "Fm Tx--> TX txSetRdsPSName is Empty !!");
        }
    }

    //Set Pty Code
    int txPtyCode = fmTxParamConfigPreferences.getInt(PTY,DEFAULT_PTY);
    Log.i(TAG,"setAdvancedConfig-- >txPtyCode = "+txPtyCode);
    if (mTxPtyCode != txPtyCode) {
        mTxPtyCode = txPtyCode;
        mStatus = sFmRadio.txSetRdsPtyCode(txPtyCode);
        if (mStatus == false) {
        Log.i(TAG, "Fm Tx--> txSetRdsPtyCode TX failed !!");
        }
        else{
        Log.i(TAG, "Fm Tx--> TX txSetRdsPtyCode is SUCCESSFUll !!");
        }
    }

/*
    int ecc = fmTxParamConfigPreferences.getInt(ECC, DEFAULT_ECC);
    Log.i(TAG,"setAdvancedConfig-- >ecc ="+ecc);
    if (mEcc != ecc) {
        mEcc = ecc;
        mStatus = sFmRadio.txSetRdsECC(ecc);
        if (mStatus == false) {
        Log.i(TAG, "Fm Tx--> txSetRdsECC TX failed !!");
        }
        else{
        Log.i(TAG, "Fm Tx--> TX txSetRdsECC is SUCCESSFUll !!");
        }
    }
*/


}



/*************************************

setRdsConfig()

*************************************/
private void setRdsConfig()
{
    Log.i(TAG, "setRdsConfig");
        SharedPreferences fmConfigPreferences = getSharedPreferences(
                "fmConfigPreferences", MODE_PRIVATE);

        String psStr = fmConfigPreferences.getString(PS_STRING,DEF_PS_STRING);
        Log.i(TAG, "setRdsConfig()--- psStr= " + psStr.toString());


        String rtstr = fmConfigPreferences.getString(RT_STRING,DEF_RT_STRING);
        Log.i(TAG, "setRdsConfig()--- RtStr= " + rtstr.toString());

        // Set TxMode
        int txMode = fmConfigPreferences.getInt(TX_MODE, DEFAULT_TXMODE);
        Log.i(TAG,"setRdsConfig-- >txMode = "+txMode);

            mStatus = sFmRadio.txSetRdsTransmissionMode(txMode);
        /*if (mTxMode != txMode) {
            mTxMode = txMode;
            mStatus = sFmRadio.txSetRdsTransmissionMode(txMode);
            if (mStatus == false) {
            Log.i(TAG, "Fm Tx--> txSetRdsTransmissionMode TX failed !!");
            }
            else{
            Log.i(TAG, "Fm Tx--> TX txSetRdsTransmissionMode is SUCCESSFUll !!");
            }
        }*/

        // Set Ps DispalyMode
        /*int psDisplayMode = fmConfigPreferences.getInt(DISPLAY_MODE, DEFAULT_DISPLAYMODE);
        Log.i(TAG,"setRdsConfig-- >psDisplayMode = "+psDisplayMode);
        if (mDisplayMode != psDisplayMode) {
            mDisplayMode = txMode;
            mStatus = sFmRadio.txSetRdsPsDisplayMode(psDisplayMode);
            if (mStatus == false) {
            Log.i(TAG, "Fm Tx--> txSetRdsPsDisplayMode TX failed !!");
            }
            else{
            Log.i(TAG, "Fm Tx--> TX txSetRdsPsDisplayMode is SUCCESSFUll !!");
            }
        }*/



        // Set Mono/Stereo Flag
        int monoStereo = fmConfigPreferences.getInt(MONO_STEREO, DEFAULT_MONOSTEREO);
        Log.i(TAG,"setRdsConfig-- >monoStereo = "+monoStereo);
        if (mMonoStereo != monoStereo) {
            mMonoStereo = monoStereo;
            mStatus = sFmRadio.txSetMonoStereoMode(monoStereo);
            if (mStatus == false) {
            Log.i(TAG, "Fm Tx--> txSetMonoStereoMode TX failed !!");
            }
            else{
            Log.i(TAG, "Fm Tx--> TX txSetMonoStereoMode is SUCCESSFUll !!");
            }
        }


        // Set Repetoire
        /*int repetoire = fmConfigPreferences.getInt(REPERTOIRE, DEFAULT_REPERTOIRE);
        Log.i(TAG,"setRdsConfig-- >repetoire = "+repetoire);
        if (mMonoStereo != repetoire) {
            mMonoStereo = repetoire;
            mStatus = sFmRadio.txSetRdsTextRepertoire(repetoire);
            if (mStatus == false) {
            Log.i(TAG, "Fm Tx--> txSetRdsTextRepertoire TX failed !!");
            }
            else{
            Log.i(TAG, "Fm Tx--> TX txSetRdsTextRepertoire is SUCCESSFUll !!");
            }
        }*/


        // Before Setting Ps String , set the Tx group Mask
        boolean rtMask= fmConfigPreferences.getBoolean(RTENABLED,false);
        boolean psMask= fmConfigPreferences.getBoolean(PSENABLED,false);
        int rdsMask=(int) RDS_RADIO_TRANSMITTED_GRP_RT_MASK;
        if(rtMask)
        rdsMask = (int) (rdsMask |RDS_RADIO_TRANSMITTED_GRP_RT_MASK) ;
        if(psMask)
        rdsMask = (int) (rdsMask | RDS_RADIO_TRANSMITTED_GRP_PS_MASK);

        Log.i(TAG, "txSetRdsTransmittedGroupsMask(rdsMask )" +rdsMask );
        mStatus = sFmRadio.txSetRdsTransmittedGroupsMask(RDS_RADIO_TRANSMITTED_GRP_PS_MASK);
        if (mStatus == false) {
        Log.i(TAG, "Fm Tx--> txSetRdsTransmittedGroupsMask TX failed !!");
        }else{
        Log.i(TAG, "Fm Tx--> TX txSetRdsTransmittedGroupsMask is SUCCESSFUll !!");
        }




        // Set Power Level
        int powerLevel = fmConfigPreferences.getInt(POWER,DEFAULT_POWER);
        Log.i(TAG,"setRdsConfig-- >powerLevel ="+powerLevel);
        if (mPowerLevel != powerLevel) {
            mPowerLevel = powerLevel;
            mStatus = sFmRadio.txSetPowerLevel(powerLevel);
            if (mStatus == false) {
            Log.i(TAG, "Fm Tx--> txSetPowerLevel TX failed !!");
            }
            else{
            Log.i(TAG, "Fm Tx--> TX txSetPowerLevel is SUCCESSFUll !!");
            }
        }



        // Set Pi Code
        int piCode = fmConfigPreferences.getInt(PI_CODE,DEFAULT_PICODE);
        Log.i(TAG,"setRdsConfig-- >piCode =" +    piCode );

        if (mPiCode != piCode) {
            mPiCode = piCode;
            mStatus = sFmRadio.txSetRdsPiCode(piCode);
            if (mStatus == false) {
            Log.i(TAG, "Fm Tx--> txSetRdsPiCode TX failed !!");
            }
            else{
            Log.i(TAG, "Fm Tx--> TX txSetRdsPiCode is SUCCESSFUll !!");
            }
        }


        // Set Af Code
        int afCode = fmConfigPreferences.getInt(AF_CODE,DEFAULT_AFCODE);
        Log.i(TAG,"setRdsConfig-- >afCode = "+afCode);
        if (mAfCode != afCode) {
            mAfCode = afCode;
            mStatus = sFmRadio.txSetRdsAfCode(afCode);
            if (mStatus == false) {
            Log.i(TAG, "Fm Tx--> txSetRdsAfCode TX failed !!");
            }
            else{
            Log.i(TAG, "Fm Tx--> TX txSetRdsAfCode is SUCCESSFUll !!");
            }
        }



}
/************************************************************************************
*
* initControls()
* Initialize all the UI controls
*
*************************************************************************************/
        public void initControls()
        {
         startTxBtn  = (CheckBox)findViewById(R.id.ChbTx);
         startTxBtn.setOnCheckedChangeListener(this);

         enableRdsBtn  = (CheckBox)findViewById(R.id.chbEnableRds);
         enableRdsBtn.setOnCheckedChangeListener(this);

        chbMute = (CheckBox)findViewById(R.id.chbMute);
              chbMute.setOnCheckedChangeListener(this);

        textFreq = (EditText)findViewById(R.id.edtTxFreq);

        btnOk = (Button) findViewById(R.id.btnOk);
        btnOk.setOnKeyListener(this);
        btnOk.setOnClickListener(this);

        // Get the notification manager service.
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);


        }


/************************************************************************************
*
* onCheckedChanged()
*
*************************************************************************************/

        public void onCheckedChanged(CompoundButton btn, boolean checkedState) {
            // TODO Auto-generated method stub
            /*if (checkedState ){
            startTxBtn.setText("Uncheck to stop Tx");
            }
            else{

            //sFmRadio.txStopTransmission();
            startTxBtn.setText("Check to start Tx");

            }*/

        int id = btn.getId();
        switch (id) {
        case R.id.ChbTx:
            if (checkedState ){
            startTxBtn.setText("Uncheck to stop Tx");
            }else{
            startTxBtn.setText("Check to start Tx");
            }
            break;

        case R.id.chbEnableRds:
            if (checkedState ){
            enableRdsBtn.setText("Uncheck to Disable Rds");
            }else{
            enableRdsBtn.setText("Check to Enable Rds");
            }
            break;

        default:
            break;
        }
        }

/************************************************************************************
*
* BaseFreq()
*
*************************************************************************************/
    static float BaseFreq() {
        return APP_FM_FIRST_FREQ_US_EUROPE_KHZ;
    }


/************************************************************************************
*
*LastFreq()
*
*************************************************************************************/
    static float LastFreq() {
        return APP_FM_LAST_FREQ_US_EUROPE_KHZ;
    }


/************************************************************************************
* Update the Frequency label with the given value
*
*************************************************************************************/


    float UpdateFrequency(float freq) {
        Log.d(TAG, "FM App: UpdateFrequency %d." + freq);
        if (freq < BaseFreq() || freq > LastFreq()) {
            freq = 0;
        }
        Log.d(TAG, "FM App: returned %d." + freq);
        return (float) freq;
    }

/************************************************************************************
*       writeFrequency()
*
*************************************************************************************/
    private void writeFrequency() {
        // get the text entered in edit box
        String text = textFreq.getText().toString();
        Log.d("writeFrequency()","--> text ="+text);
        try {

            float iFreq = Float.parseFloat(text);
            Log.d("writeFrequency()","--> iFreq ="+iFreq);
            Float validFreq = UpdateFrequency(iFreq);
            if (validFreq != 0) {
                freqValue = validFreq;

            }
            else {
                new AlertDialog.Builder(this).setIcon(
                        android.R.drawable.ic_dialog_alert).setMessage(
                        "Enter valid frequency!!").setNegativeButton(
                        android.R.string.ok, null).show();
                textFreq.setText(null);
            }
        } catch (NumberFormatException nfe) {
            Log.d(TAG, "NumberFormatException:" + nfe.getMessage());
            new AlertDialog.Builder(this).setIcon(
                    android.R.drawable.ic_dialog_alert).setMessage(
                    "Enter valid number!!").setNegativeButton(
                    android.R.string.ok, null).show();
            textFreq.setText(null);
                   freqValue = (float) 0;
        }

    }

/*************************************************************************
*
* This is a method implementation of OnKeyListener
*
*************************************************************************/
    public boolean onKey(View v, int keyCode, KeyEvent event) {
            Log.i(TAG,"onKey()");
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                writeFrequency();
                if (startTxBtn.isChecked()){
                 // Tune TX
        Log.i(TAG, "Fm Tx--> TX Tune is (long)(freqValue.floatValue()*1000) !!"+(long)(freqValue.floatValue()*1000));

                     mStatus =  sFmRadio.txTune((long)(freqValue.floatValue()*1000));
                if (mStatus == false) {
                Log.i(TAG, "Fm Tx--> Tune TX failed !!");
                }
                else{
                Log.i(TAG, "Fm Tx--> TX Tune is SUCCESSFUll !!");
                }
                }
                return true;
            }
        }
        return false;
    }


/*************************************************************************
*
* This is a method implementation of onKeyDown
*
*************************************************************************/
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG,"onKeyDown()");
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
            writeFrequency();
            if (startTxBtn.isChecked()){
                 /* Tune TX */
                     mStatus =  sFmRadio.txTune((long)(freqValue.floatValue()*1000));
                if (mStatus == false) {
                Log.i(TAG, "Fm Tx--> Tune TX failed !!");
                }
                else{
                Log.i(TAG, "Fm Tx--> TX Tune is SUCCESSFUll !!");
                }

            }
            return true;

        case KeyEvent.KEYCODE_DPAD_UP:
            return true;

        case KeyEvent.KEYCODE_BACK:
        Log.i("onKeyDown()","KEYCODE_BACK");
            finish();
            return true;

        case KeyEvent.KEYCODE_HOME:
        Log.i("onKeyDown()","KEYCODE_HOME");
            //this.showNotification(R.drawable.radio, R.string.app_name,textFreq.getText(), false);
            //moved the notification to TX
            //finish();
            return true;



            /* Keys A to L are mapped to different get APIs for Testing */
        case KeyEvent.KEYCODE_A:
               Log.i("onKeyDown()","KEYCODE_A");
            Log.i(TAG, "Testing txSetRdsTextPsMsg(psStr)    returned  = "
                    + sFmRadio.txSetRdsTextPsMsg("psStr"));
            return true;

        case KeyEvent.KEYCODE_B:
        Log.i("onKeyDown()","KEYCODE_B");
            Log.i(TAG, "Testing txWriteRdsRawData(RawData)    returned= "
                    + sFmRadio.txWriteRdsRawData("RdsRawData"));
            return true;

        case KeyEvent.KEYCODE_C:
        Log.i("onKeyDown()","KEYCODE_C");
            sFmRadio.txSetRdsTransmissionMode(1);

            return true;

        case KeyEvent.KEYCODE_D:
        Log.i("onKeyDown()","KEYCODE_D");
            Log.i(TAG, "Testing txSetMonoStereoMode(0)    returned= "
                    + sFmRadio.txSetMonoStereoMode(0));
            return true;

        case KeyEvent.KEYCODE_E:
        Log.i("onKeyDown()","KEYCODE_E");
            Log.i(TAG, "Testing txSetPreEmphasisFilter(1)    returned = "
                    + sFmRadio.txSetPreEmphasisFilter(1));
        return true;

        case KeyEvent.KEYCODE_F:
        Log.i("onKeyDown()","KEYCODE_F");
              Log.i(TAG, "Testing txSetMuteMode(0) returned = "
                    + sFmRadio.txSetMuteMode(0));
        return true;

        case KeyEvent.KEYCODE_G:
        Log.i("onKeyDown()","KEYCODE_G");
            Log.i(TAG, "Testing txSetRdsAfCode(224)  returned = "
                    + sFmRadio.txSetRdsAfCode(224));
            return true;

        case KeyEvent.KEYCODE_H:
        Log.i("onKeyDown()","KEYCODE_H");
            Log.i(TAG,"Testing txSetRdsPiCode(5)    returned = "
                            + sFmRadio.txSetRdsPiCode(5));
            return true;

        case KeyEvent.KEYCODE_I:
        Log.i("onKeyDown()","KEYCODE_I");
            Log.i(TAG, "Testing txSetRdsPtyCode(0) returned  = "
                    + sFmRadio.txSetRdsPtyCode(0));
            return true;

        case KeyEvent.KEYCODE_J:
        Log.i("onKeyDown()","KEYCODE_J");
            Log.i(TAG, "Testing txSetRdsTextRepertoire(0) returned = "
                    + sFmRadio.txSetRdsTextRepertoire(0));
            return true;

        case KeyEvent.KEYCODE_K:
        Log.i("onKeyDown()","KEYCODE_K");
            Log.i(TAG,"Testing txSetRdsPsDisplayMode(0) returned = "
                            + sFmRadio.txSetRdsPsDisplayMode(0));
            return true;

        case KeyEvent.KEYCODE_L:
        Log.i("onKeyDown()","KEYCODE_L");
            Log.i(TAG, "Testing txChangeDigitalSourceConfiguration(1)    returned  = "
                    + sFmRadio.txChangeDigitalSourceConfiguration(1));

            return true;

        case KeyEvent.KEYCODE_M:
        Log.i("onKeyDown()","KEYCODE_M");
            Log.i(TAG, "Testing txSetRdsPsScrollSpeed(3)    returned  = "
                    + sFmRadio.txSetRdsPsScrollSpeed(3));
            return true;

        case KeyEvent.KEYCODE_N:
        Log.i("onKeyDown()","KEYCODE_N");
            Log.i(TAG, "Testing txSetRdsTextRtMsg(2,RtMsg,5)    returned  = "
                    + sFmRadio.txSetRdsTextRtMsg(2,"RtMsg",5));
            return true;

        case KeyEvent.KEYCODE_O:
        Log.i("onKeyDown()","KEYCODE_O");
                    Log.i(TAG,
                    "Testing txSetRdsTransmittedGroupsMask(1) returned = "
                            + sFmRadio.txSetRdsTransmittedGroupsMask(7));
            return true;

        case KeyEvent.KEYCODE_P:
        Log.i("onKeyDown()","KEYCODE_P");
            Log.i(TAG,
                    "Testing txSetRdsTrafficCodes(0,0)    returned = "
                            + sFmRadio.txSetRdsTrafficCodes(0,0));

            return true;

        case KeyEvent.KEYCODE_Q:
        Log.i("onKeyDown()","KEYCODE_Q");
            Log.i(TAG,
                    "Testing txSetRdsMusicSpeechFlag(1)    returned = "
                            + sFmRadio.txSetRdsMusicSpeechFlag(1));

            return true;

        case KeyEvent.KEYCODE_R:
        Log.i("onKeyDown()","KEYCODE_R");
            Log.i(TAG, "Testing txSetRdsECC(0)    returned  = "
                    + sFmRadio.txSetRdsECC(0));

            return true;

        case KeyEvent.KEYCODE_S:
        Log.i("onKeyDown()","KEYCODE_S");
            Log.i(TAG,
                    "Testing txChangeAudioSource(0,0)    returned = "
                            + sFmRadio.txChangeAudioSource(0,0));
            return true;

        case KeyEvent.KEYCODE_T:
        Log.i("onKeyDown()","KEYCODE_T");
            Log.i(TAG,
                    "Testing txEnableRds()    returned = "
                            + sFmRadio.txEnableRds());
            return true;

        case KeyEvent.KEYCODE_U:
        Log.i("onKeyDown()","KEYCODE_U");
            Log.i(TAG,
                    "Testing txDisableRds()    returned = "
                            + sFmRadio.txDisableRds());
            return true;

        case KeyEvent.KEYCODE_V:
        Log.i("onKeyDown()","KEYCODE_V");
            Log.i(TAG,
                    "Testing txStartTransmission()    returned = "
                            + sFmRadio.txStartTransmission());
            return true;

        case KeyEvent.KEYCODE_W:
        Log.i("onKeyDown()","KEYCODE_W");
            Log.i(TAG,
                    "Testing txStopTransmission()    returned = "
                            + sFmRadio.txStopTransmission());
            return true;

        case KeyEvent.KEYCODE_X:
        Log.i("onKeyDown()","KEYCODE_X");
            Log.i(TAG,
                    "Testing txSetPowerLevel()    returned = "
                            + sFmRadio.txSetPowerLevel(0));
            return true;

        case KeyEvent.KEYCODE_Y:
        Log.i("onKeyDown()","KEYCODE_Y");
            Log.i(TAG,
                    "Testing txTune()    returned = "
                            + sFmRadio.txTune(104000));
            return true;

         }
        return false;
    }



private void setMuteMode()
{

    Log.i(TAG,"setMuteMode()");
    // Mute
    if(chbMute.isChecked()== true){
    mStatus = sFmRadio.txSetMuteMode(0);
    }else{
    mStatus = sFmRadio.txSetMuteMode(1);
    }

    if (mStatus == false) {
    Log.i(TAG, "Fm Tx--> Cannot Mute TX !!");
    }else{
    Log.i(TAG, "Fm Tx--> Muting TX !!");
    }

}


/*************************************************************************
*
* This is a method implementation of onClick
*
*************************************************************************/
    public void onClick(View view) {
        Log.i(TAG, "onClick()");
        int id = view.getId();
        switch (id) {
        case R.id.btnOk:
            setMuteMode();
            writeFrequency();
            fmTxPreferences = getSharedPreferences("fmTxPreferences", MODE_PRIVATE);

            if (startTxBtn.isChecked() && freqValue != 0 ){
                          Log.i(TAG, " FM-------> mFreq:" + mFreq);
                          Log.i(TAG, " FM-------> freqValue" + freqValue);
                if(((mFreq-freqValue) < -.00001) ||((mFreq-freqValue) > .00001)) {
                     /* Tune TX */
                     mFreq = freqValue;
                                Log.i(TAG, " FM-------> mFreq:" + mFreq);
                    mStatus =  sFmRadio.txTune((long)(freqValue.floatValue()*1000));
                    Log.i(TAG, "Fm Tx--> Tune TX freqValue !!" +(long)(freqValue.floatValue()*1000));
                    if (mStatus == false) {
                    Log.i(TAG, "Fm Tx--> Tune TX failed !!");
                    }
                    else{
                    Log.i(TAG, "Fm Tx--> TX Tune is SUCCESSFUll !!");
                    }
                } else {
                     /* Already Tuned */
                  mStatus = sFmRadio.txStartTransmission();
                  if (mStatus == false) {
                    Log.i(TAG, "Fm Tx--> Stop Tx failed !!");
                  }else {
                    Log.i(TAG, "Fm Tx--> Stop Transmission is SUCCESSFUll !!");
                  }
            }
            }else{
                mStatus = sFmRadio.txStopTransmission();
                if (mStatus == false) {
                Log.i(TAG, "Fm Tx--> Stop Tx failed !!");
                }
                else{
                Log.i(TAG, "Fm Tx--> Stop Transmission is SUCCESSFUll !!");
                }

            }

            if(enableRdsBtn.isChecked()){
            sFmRadio.txEnableRds();
            }else{
            sFmRadio.txDisableRds();
            }



            break;

        default:
            break;
        }
    }



/*************************************************************************
*
*
*
*************************************************************************/
    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {


            case EVENT_FM_TX_ENABLED:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_ENABLED");
                isFmEnabled = true;
                mAppState = STATE_ENABLED;
                if (pd != null){
                pd.dismiss();
                }
                pd =null;
                break;

            case EVENT_FM_TX_DISABLED:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_DISABLED");
                isFmEnabled = false;
                 mAppState = STATE_DISABLED;
                          startTxBtn.setChecked(false);
                finish(); //Close the Activity
                break;


            case EVENT_FM_TX_STARTTRANSMISSION:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_STARTTRANSMISSION");
                break;

            case EVENT_FM_TX_STOPTRANSMISSION:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_STOPTRANSMISSION");
                break;

            case EVENT_FM_TX_TUNE:
                mStatus = sFmRadio.txStartTransmission();
                if (mStatus == false) {
                Log.i(TAG, "Fm Tx--> TX txStartTransmission failed !!");
                }
                else{
                Log.i(TAG, "Fm Tx--> TX is Transmitting..");
                }
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_TUNE");
                break;


            case EVENT_FM_TX_ENABLE_RDS:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_ENABLE_RDS");
                break;


            case EVENT_FM_TX_DISABLE_RDS:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_DISABLE_RDS");
                break;

            case EVENT_FM_TX_SET_TRANSMISSION_MODE:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_TRANSMISSION_MODE");
                break;

            case EVENT_FM_TX_SET_PS_DISPLAY_MODE:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_PS_DISPLAY_MODE");
                break;

            case EVENT_FM_TX_SET_RDS_MUSIC_SPEECH_FLAG:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_RDS_MUSIC_SPEECH_FLAG");
                break;

            case EVENT_FM_TX_SET_MONO_STEREO_MODE:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_MONO_STEREO_MODE");
                break;

            case EVENT_FM_TX_SET_POWER_LEVEL:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_POWER_LEVEL");
                break;

            case EVENT_FM_TX_SET_MUTE_MODE:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_MUTE_MODE");
                break;

            case EVENT_FM_TX_SET_RDS_TEXT_PS_MSG:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_RDS_TEXT_PS_MSG");
                break;

            case EVENT_FM_TX_SET_RDS_TX_GRP_MASK_PS:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_RDS_TX_GRP_MASK_PS");

                SharedPreferences fmConfigPreferences = getSharedPreferences(
                "fmConfigPreferences", MODE_PRIVATE);

                String psStr = fmConfigPreferences.getString(PS_STRING,DEF_PS_STRING);
                Log.i(TAG, "txSetRdsTextPsMsg()--- psStr= " + psStr.toString());
                mStatus = sFmRadio.txSetRdsTextPsMsg(psStr);
                if (mStatus == false) {
                Log.i(TAG, "Fm Tx--> txSetRdsTextPsMsg TX failed !!");
                }else{
                Log.i(TAG, "Fm Tx--> TX txSetRdsTextPsMsg is SUCCESSFUll !!");
                }
                break;

            case EVENT_FM_TX_SET_RDS_TX_GRP_MASK_RT:
                Log.i(TAG, "enter handleMessage ----EVENT_FM_TX_SET_RDS_TX_GRP_MASK_RT");

                SharedPreferences fmTxParamConfigPreferences = getSharedPreferences(
                "fmTxParamConfigPreferences", MODE_PRIVATE);

                String rtStr = fmTxParamConfigPreferences.getString(RT_STRING,DEF_RT_STRING);
                Log.i(TAG, "txSetRdsTextRtMsg()--- rtStr= " + rtStr.toString());
                mStatus = sFmRadio.txSetRdsTextRtMsg(RDS_TEXT_TYPE_RT_B,rtStr,rtStr.length()); //msgType =4
                if (mStatus == false) {
                Log.i(TAG, "Fm Tx--> txSetRdsTextRtMsg TX failed !!");
                }else{
                Log.i(TAG, "Fm Tx--> TX txSetRdsTextRtMsg is SUCCESSFUll !!");
                }
                break;


 }}};


/*************************************************************************
*
*
*
*************************************************************************/
     private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String fmAction = intent.getAction();
            Log.i(TAG, "enter onReceive" + fmAction);
            if (fmAction.equals(FmRadioIntent.FM_TX_ENABLED_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_ENABLED_ACTION " + fmAction);
                mHandler.sendMessage(mHandler
                        .obtainMessage(EVENT_FM_TX_ENABLED, 0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_DISABLED_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_DISABLED_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_DISABLED,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_TUNE_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_TUNE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_TUNE,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_START_TRANSMISSION_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_START_TRANSMISSION_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_STARTTRANSMISSION,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_STOP_TRANSMISSION_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_STOP_TRANSMISSION_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_STOPTRANSMISSION,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_DESTROY_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_DESTROY_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_DESTROY,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_ENABLE_RSD_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_ENABLE_RSD_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_ENABLE_RDS,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_DISABLE_RSD_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_DISABLE_RSD_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_DISABLE_RDS,
                        0));
            }


            if (fmAction.equals(FmRadioIntent.FM_TX_SET_TRANSMISSION_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_TRANSMISSION_MODE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_TRANSMISSION_MODE,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_PS_DISPLAY_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_PS_DISPLAY_MODE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_PS_DISPLAY_MODE,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_SET_RDS_MUSIC_SPEECH_FLAG_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_RDS_MUSIC_SPEECH_FLAG_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_RDS_MUSIC_SPEECH_FLAG,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_SET_RDS_TEXT_REPERTOIRE_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_RDS_TEXT_REPERTOIRE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_RDS_TEXT_REPERTOIRE,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_SET_TRANSMISSION_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_TRANSMISSION_MODE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_TRANSMISSION_MODE,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_SET_POWER_LEVEL_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_POWER_LEVEL_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_POWER_LEVEL,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_SET_RDS_TEXT_PS_MSG_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_RDS_TEXT_PS_MSG_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_RDS_TEXT_PS_MSG,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_SET_MONO_STEREO_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_MONO_STEREO_MODE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_MONO_STEREO_MODE,
                        0));
            }


            if (fmAction.equals(FmRadioIntent.FM_TX_SET_MUTE_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_MUTE_MODE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_MUTE_MODE,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.FM_TX_SET_RDS_TRANSMISSION_GROUPMASK_ACTION)) {
                Log.i(TAG, "enter onReceive FM_TX_SET_RDS_TRANSMISSION_GROUPMASK_ACTION " + fmAction);

                Long grpMask = intent.getLongExtra(
                        FmRadioIntent.RDS_GRP_MASK, 0);
            /*
                if(grpMask == RDS_RADIO_TRANSMITTED_GRP_PS_MASK){
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_RDS_TX_GRP_MASK_PS,
                        0));
                }else if(grpMask == RDS_RADIO_TRANSMITTED_GRP_RT_MASK){
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_TX_SET_RDS_TX_GRP_MASK_RT,
                        0));
                }
            */

            }



        }
    };




    /* Save the values to the preference when the application exits */
    private void saveLastData() {
        Log.i(TAG, "saveLastData()---Entered");
        fmTxPreferences = getSharedPreferences("fmTxPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = fmTxPreferences.edit();

        editor.putBoolean(FMENABLED, startTxBtn.isChecked());
        editor.putBoolean(RDSENABLED, enableRdsBtn.isChecked());
        editor.putBoolean(MUTE, chbMute.isChecked());

        editor.putString(FREQUENCY_STRING, textFreq.getText().toString());
        editor.putString(FREQUENCY, DEFAULT_FREQ);

        Log.d(TAG, " save FMENABLED " +isFmEnabled + "FREQUENCY_STRING"+freqValue);
        editor.commit();
        Log.i(TAG, "saveLastData()---Exit");
    }

    /* Load the last saved values from the preference when the application starts */
    private void loadlastSaveddata() {

        Log.i(TAG, "loadlastSaveddata()-entered");
        fmTxPreferences = getSharedPreferences("fmTxPreferences", MODE_PRIVATE);

        isFmEnabled = fmTxPreferences.getBoolean(FMENABLED,false);
        if(isFmEnabled == true)
        {
            startTxBtn.setChecked(true);
            startTxBtn.setEnabled(true);

        }else{
            startTxBtn.setChecked(false);
            startTxBtn.setEnabled(true);
        }

        enableRdsBtn.setChecked(fmTxPreferences.getBoolean(RDSENABLED,false));
        enableRdsBtn.setEnabled(true);

        chbMute.setChecked(fmTxPreferences.getBoolean(MUTE,false));
        chbMute.setEnabled(true);

        textFreq.setText(fmTxPreferences.getString(FREQUENCY_STRING,null));

        Log.d(TAG, " Load FMENABLED " +isFmEnabled + "FREQUENCY_STRING"+freqValue);

        Log.i(TAG, "loadlastSaveddata()-exit");
    }




     public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }


    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if(mFmServiceConnected == true)
        enableTx();


    }

     public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        /*
         * Unregistering the receiver , so that we dont handle any FM events
         * when out of the FM application screen
         */

        unregisterReceiver(mReceiver);
    }

    public int getFmTxAppState() {
        Log.d(TAG, "getFmTxAppState()");
        return mAppState;

    }


}

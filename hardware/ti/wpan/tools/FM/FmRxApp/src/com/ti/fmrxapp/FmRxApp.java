/*
 *
 * Copyright 2001-2010 Texas Instruments, Inc. - http://www.ti.com/
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.ti.fm.FmRadio;
import com.ti.fm.FmRadioIntent;
import com.ti.fm.IFmConstants;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.view.Window;
import android.media.AudioManager;
import android.widget.Toast;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/*
 FM Boot up Sequence:

 FM APIS NON Blocking:

 sFmRadio.rxEnable() is called from FM application, when the user selects FM radio icon from main menu.

 Once  the callback for sFmRadio.rxEnable() is received (EVENT_FM_ENABLED),
 the default configurations which have been saved in the preference will be loaded using loadDefaultConfiguration() function .
 After this  sFmRadio.rxSetVolume() with default volume will be called.

 Once the callback for  sFmRadio.rxSetVolume() is received(EVENT_VOLUME_CHANGE),
 sFmRadio.rxSetBand() with default band will be called.

 Once the callback for sFmRadio.rxSetBand() is received(EVENT_BAND_CHANGE), sFmRadio.rxTune_nb()
 with default frequency will be called.

 Once the callback for sFmRadio.rxTune_nb()is received (EVENT_TUNE_COMPLETE)
 sFmRadio.rxEnableAudioRouting()  will be called to enable audio.

 After these sequences user can hear the FM audio.


 The rest of the APIS will be called based on the user actions like when user presses seek up or down
 sFmRadio.rxSeek_nb() will be called and the callback for the same will be EVENT_SEEK_STARTED.

 To increase decrease the volume  sFmRadio.rxSetVolume() will be called and the callback for
 the same will be EVENT_VOLUME_CHANGE.

 To mute /unmute, sFmRadio.rxSetMuteMode() will be called and the callback
 for the same will be EVENT_MUTE_CHANGE.


  FM APIS  Blocking:

In case of blocking FM APIS, the above sequence holds good. The difference will be the FM Events will not posted
as intents and the usage of FM APIS will be sequential.
 */

public class FmRxApp extends Activity implements View.OnClickListener,
        IFmConstants, FmRxAppConstants , FmRadio.ServiceListener {
    public static final String TAG = "FmRxApp";
    private static final boolean DBG = false;

    /********************************************
     * Widgets
     ********************************************/

    private ImageView imgFmPower, imgFmMode, imgFmVolume, imgFmAudiopath;
    private ImageButton imgFmSeekUp, imgFmSeekDown;
    private TextView txtStatusMsg, txtRadioText;
    private TextView txtPsText;
    static TextView txtStationName;
    private static Button btnStation1, btnStation2, btnStation3;
    private static Button btnStation4, btnStation5, btnStation6;
    private ProgressDialog pd = null, configPd;

    /********************************************
     * Menu Constants
     ********************************************/
    public static final int MENU_CONFIGURE = Menu.FIRST + 1;
    public static final int MENU_EXIT = Menu.FIRST + 2;
    public static final int MENU_ABOUT = Menu.FIRST + 3;
    public static final int MENU_PRESET = Menu.FIRST + 4;
    public static final int MENU_SETFREQ = Menu.FIRST + 5;

       public boolean mPreset = false;

    /********************************************
     * private variables
     ********************************************/
    private int mToggleMode = 0; // To toggle between the mono/stereo
    private int mToggleAudio = 1; // To toggle between the speaker/headset
    private boolean mToggleMute = false; // To toggle between the mute/unmute

    private boolean mRdsState = false;
    /* Default values */
    private int mVolume = DEF_VOLUME;
    private int mMode = DEFAULT_MODE;
    private boolean mRds = DEFAULT_RDS;
    private boolean mRdsAf = DEFAULT_RDS_AF;
    private int mRdsSystem = INITIAL_VAL;
    private int mDeEmpFilter = INITIAL_VAL;
    private int mRssi = INITIAL_RSSI;
    // Seek up/down direction
    private int mDirection = FM_SEEK_UP;

    /* State values */

    // variable to make sure that the next configuration change happens after
    // the current configuration request has been completed.
    private int configurationState = CONFIGURATION_STATE_IDLE;
    // variable to make sure that the next volume change happens after the
    // current volume request has been completed.
    private boolean mVolState = VOL_REQ_STATE_IDLE;
    // variable to make sure that the next seek happens after the current seek
    // request has been completed.
    private boolean mSeekState = SEEK_REQ_STATE_IDLE;

    private boolean mStatus;
    private int mIndex = 0;
    private int mStationIndex;


    /*
     * Variable to identify whether we need to do the default setting when
     * entering the FM application. Based on this variable,the default
     * configurations for the FM will be done for the first time
     */

    private static boolean sdefaultSettingOn = false;

    private static boolean mIsDbPresent = false;

    private NotificationManager mNotificationManager;

    static final String FM_INTERRUPTED_KEY = "fm_interrupted";
    static final String FM_STATE_KEY = "fm_state";
    /* Flag to know whether FM App was interrupted due to orientation change */
    boolean mFmInterrupted = false;

    /*Flag to check if service is connected*/
    boolean mFmServiceConnected = false;
    /********************************************
     * public variables
     ********************************************/
    public static int sBand = DEFAULT_BAND;
    public static int sChannelSpace = DEFAULT_CHANNELSPACE;

    public static Float lastTunedFrequency = (float) DEFAULT_FREQ_EUROPE;
    public static FmRadio sFmRadio;

    /** Arraylist of stations */
    public static ArrayList<HashMap<String, String>> stations = new ArrayList<HashMap<String, String>>(
            6);
    public static TextView txtFmRxTunedFreq;
    private OrientationListener mOrientationListener;

    Context mContext;

    DBAdapter db;

    /** Called when the activity is first created. */

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mContext = this;
        /* Retrieve the fm_state and find out whether FM App was interrupted */

        if (savedInstanceState != null) {
            Bundle fmState = savedInstanceState.getBundle(FM_STATE_KEY);
            if (fmState != null) {
                mFmInterrupted = fmState.getBoolean(FM_INTERRUPTED_KEY, false);

            }
        }

        // Create orientation listenter.


        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Register for FM intent broadcasts.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FmRadioIntent.FM_ENABLED_ACTION);
        intentFilter.addAction(FmRadioIntent.FM_DISABLED_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_FREQUENCY_ACTION);
        intentFilter.addAction(FmRadioIntent.SEEK_ACTION);
        intentFilter.addAction(FmRadioIntent.BAND_CHANGE_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_CHANNEL_SPACE_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_MODE_MONO_STEREO_ACTION);
        intentFilter.addAction(FmRadioIntent.VOLUME_CHANGED_ACTION);
        intentFilter.addAction(FmRadioIntent.RDS_TEXT_CHANGED_ACTION);
        intentFilter.addAction(FmRadioIntent.PS_CHANGED_ACTION);
        intentFilter.addAction(FmRadioIntent.AUDIO_PATH_CHANGED_ACTION);
        intentFilter.addAction(FmRadioIntent.TUNE_COMPLETE_ACTION);
        intentFilter.addAction(FmRadioIntent.SEEK_STOP_ACTION);
        intentFilter.addAction(FmRadioIntent.MUTE_CHANGE_ACTION);
        intentFilter.addAction(FmRadioIntent.DISPLAY_MODE_MONO_STEREO_ACTION);
        intentFilter.addAction(FmRadioIntent.ENABLE_RDS_ACTION);
        intentFilter.addAction(FmRadioIntent.DISABLE_RDS_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_RDS_AF_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_RDS_SYSTEM_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_DEEMP_FILTER_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_RSSI_THRESHHOLD_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_RF_DEPENDENT_MUTE_ACTION);
        intentFilter.addAction(FmRadioIntent.PI_CODE_CHANGED_ACTION);
        intentFilter.addAction(FmRadioIntent.MASTER_VOLUME_CHANGED_ACTION);
        intentFilter.addAction(FmRadioIntent.CHANNEL_SPACING_CHANGED_ACTION);
        intentFilter.addAction(FmRadioIntent.COMPLETE_SCAN_DONE_ACTION);
        intentFilter.addAction(FmRadioIntent.COMPLETE_SCAN_STOP_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_BAND_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_MONO_STEREO_MODE_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_MUTE_MODE_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_RF_MUTE_MODE_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_RSSI_THRESHHOLD_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_DEEMPHASIS_FILTER_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_VOLUME_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_RDS_SYSTEM_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_RDS_GROUPMASK_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_RDS_AF_SWITCH_MODE_ACTION);
        intentFilter.addAction(FmRadioIntent.GET_RSSI_ACTION);
        intentFilter.addAction(FmRadioIntent.COMPLETE_SCAN_PROGRESS_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_RDS_AF_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_RF_DEPENDENT_MUTE_ACTION);
        intentFilter.addAction(FmRadioIntent.SET_CHANNEL_SPACE_ACTION);

        registerReceiver(mReceiver, intentFilter);

       db = new DBAdapter(this);
       createEmptyList();
        /*
         * Need to enable the FM if it is not enabled earlier
         */

        sFmRadio = new FmRadio(this, this);
}
    private void setButtonLabels() {

       //Log.d(TAG, "setButtonLabels");
        Iterator<HashMap<String, String>> stationIterator = stations.iterator();
       while (stationIterator.hasNext()) {
          HashMap<String, String> item = (HashMap<String, String>) stationIterator.next();

          //Log.d(TAG, "setButtonLabels item " + item);
          String freq = item.get(ITEM_VALUE);
          int iButtonNo = Integer.parseInt(item.get(ITEM_KEY));
          switch (iButtonNo) {
             case 1:
                if (freq.equals(""))
                    btnStation1.setEnabled(false);
                else {
                    btnStation1.setEnabled(true);
                    btnStation1.setText(freq);
                }
                break;
             case 2:
                if (freq.equals(""))
                    btnStation2.setEnabled(false);
                else {
                    btnStation2.setEnabled(true);
                    btnStation2.setText(freq);
                }
                break;
             case 3:
                if (freq.equals(""))
                    btnStation3.setEnabled(false);
                else {
                    btnStation3.setEnabled(true);
                    btnStation3.setText(freq);
                }
                break;
             case 4:
                if (freq.equals(""))
                    btnStation4.setEnabled(false);
                else {
                    btnStation4.setEnabled(true);
                    btnStation4.setText(freq);
                }
                break;
             case 5:
                if (freq.equals(""))
                    btnStation5.setEnabled(false);
                else {
                    btnStation5.setEnabled(true);
                    btnStation5.setText(freq);
                }
                break;
             case 6:
                if (freq.equals(""))
                    btnStation6.setEnabled(false);
                else {
                    btnStation6.setEnabled(true);
                    btnStation6.setText(freq);
                }
                break;

          }

       }

    }
private void startup() {
        //Log.i(TAG, "startup");


        switch (sFmRadio.rxGetFMState()) {
        /* FM is alreary enabled. Just update the UI. */
        case FmRadio.STATE_ENABLED:
            Log.i(TAG, "FmRadio.STATE_ENABLED ");
            // Fm app is on and we are entering from the other screens
                    loadDefaultConfiguration();
                    setContentView(R.layout.fmrxmain);
                    initControls();
                   setButtonLabels();
                   if (sFmRadio.rxIsFMPaused()) {
                         Log.i(TAG, "FmRadio.STATE_PAUSE ");
                    mStatus = sFmRadio.resumeFm();
                  if (mStatus == false) {
                    showAlert(this, "FmRadio", "Cannot resume Radio!!!!");
                      }
            }

            break;

        case FmRadio.STATE_DISABLED:
            Log.i(TAG, "FmRadio.STATE_DISABLED ");
            sdefaultSettingOn = false;

            mStatus = sFmRadio.rxEnable();
            if (mStatus == false) {
                showAlert(this, "FmRadio", "Cannot enable Radio!!!!");

            }

            else { /* Display the dialog till FM is enabled */
                pd = ProgressDialog.show(this, "Please wait..",
                        "Powering on Radio", true, false);
            }

            break;
        /* FM has not been started. Start the FM */
        case FmRadio.STATE_DEFAULT:

            Log.i(TAG, "FmRadio.STATE_DEFAULT ");
            sdefaultSettingOn = false;
            /*
             * Make sure not to start the FM_Enable() again, if it has been
             * already called before orientation change
             */
            if (mFmInterrupted == false) {

                mStatus = sFmRadio.rxEnable();
                if (mStatus == false) {
                    showAlert(this, "FmRadio", "Cannot enable Radio!!!!");

                }

                else { /* Display the dialog till FM is enabled */
                    pd = ProgressDialog.show(this, "Please wait..",
                            "Powering on Radio", true, false);
                }
            } else {
                Log.w(TAG, "mFmInterrupted is true dont  call enable");

            }
            break;
        }
    }

    public void onServiceConnected() {
        //Log.i(TAG, "onServiceConnected");
        mFmServiceConnected = true;
        startup();
    }

    public void onServiceDisconnected() {
        //Log.d(TAG, "Lost connection to service");
        mFmServiceConnected = false ;
        sFmRadio = null;
    }


    /*
     * Set the arraylist with the selected station name and display the name on
     * main screen
     */
    public static void UpdateRenameStation(int index, String name) {
        txtStationName.setText(name.toString());
        // Store the name in the selected index in Arraylist
        SetStation(index, "", name);

    }

    /*
     * Set the arraylist and the buttons with the selected station and make the
     * button enabled so that user can select it to select the stored stations
     */
    public static void updateSetStation(int index, String freq, String name) {
        switch (index) {
        case 0:
            if (freq.equals(""))
                btnStation1.setEnabled(false);
            else {
                btnStation1.setEnabled(true);
                btnStation1.setText(freq);
            }

            break;
        case 1:
            if (freq.equals(""))
                btnStation2.setEnabled(false);
            else {
                btnStation2.setEnabled(true);
                btnStation2.setText(freq);
            }

            break;
        case 2:
            if (freq.equals(""))
                btnStation3.setEnabled(false);
            else {
                btnStation3.setEnabled(true);
                btnStation3.setText(freq);
            }
            break;
        case 3:
            if (freq.equals(""))
                btnStation4.setEnabled(false);
            else {
                btnStation4.setEnabled(true);
                btnStation4.setText(freq);
            }

            break;
        case 4:
            if (freq.equals(""))
                btnStation5.setEnabled(false);
            else {
                btnStation5.setEnabled(true);
                btnStation5.setText(freq);
            }

            break;
        case 5:
            if (freq.equals(""))
                btnStation6.setEnabled(false);
            else {
                btnStation6.setEnabled(true);
                btnStation6.setText(freq);
            }
            break;
        }
        // Store the freq in the selected index in Arraylist
        SetStation(index, freq, "");

    }

    /*
     * Unset the arraylist and the button with the empty station and make the
     * button disabled so that user cannot select it
     */

    public static void updateUnSetStation(int index) {
        switch (index) {
        case 0:
            btnStation1.setEnabled(false);
            btnStation1.setText("1");
            break;
        case 1:
            btnStation2.setEnabled(false);
            btnStation2.setText("2");
            break;
        case 2:
            btnStation3.setEnabled(false);
            btnStation3.setText("3");
            break;
        case 3:
            btnStation4.setEnabled(false);
            btnStation4.setText("4");
            break;
        case 4:

            btnStation5.setEnabled(false);
            btnStation5.setText("4");
            break;
        case 5:
            btnStation6.setEnabled(false);
            btnStation6.setText("6");
            break;
        }

        UnSetStation(index, "", "");

    }

    /* Set particular station name or frequency in the stations arraylist */
    public static void SetStation(Integer index, String freq, String name) {
        Log.i(TAG, "SetStation");
        Integer pos = new Integer(index.intValue() + 1);
        try {
            HashMap<String, String> item = stations.get(index.intValue());
            item.put(ITEM_KEY, pos.toString());
            // Update the name only
            if ((freq.equals(""))) {
                item.put(ITEM_NAME, name);

                // Empty name string is passed. Update the name
                if ((name.equals("")))
                    item.put(ITEM_NAME, name);
            }

            // Update the frequency only
            else if ((name.equals(""))) {
                item.put(ITEM_VALUE, freq);

            }
            stations.set(index.intValue(), item);
        } catch (NullPointerException e) {
            Log.e(TAG, "Tried to add null value");
        }
    }

    /* UnSet particular station name and frequency in the stations arraylist */
    public static void UnSetStation(Integer index, String freq, String name) {
       Log.i(TAG, "UnSetStation");
       Integer pos = new Integer(index.intValue() + 1);
       try {
          HashMap<String, String> item = stations.get(index.intValue());
          item.put(ITEM_KEY, pos.toString());
          item.put(ITEM_VALUE, freq);
          item.put(ITEM_NAME, name);
          stations.set(index.intValue(), item);
       } catch (NullPointerException e) {
          Log.e(TAG, "Tried to add null value");
       }
    }

    /* Get particular station from the stations arraylist */

    public static String GetStation(Integer index) {
        Log.i(TAG, "GetStation");
        Integer pos = new Integer(index.intValue() + 1);
        try {
            HashMap<String, String> item = stations.get(index.intValue());
            Object freq = item.get(ITEM_VALUE);
            Object name = item.get(ITEM_NAME);
            if (name != null) {
                txtStationName.setText(name.toString());
            } else {
                Log.w(TAG, "There is no name in the HashMap.");
            }

            if (freq != null) {
                return freq.toString();
            } else {
                Log.w(TAG, "There is no key in the HashMap.");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Tried to add null value");
        }
        return null;
    }

    /*
     * Handler for all the FM related events. The events will be handled only
     * when we are in the FM application main menu
     */

    private Handler mHandler = new Handler() {
    StringBuilder sb ;
    StringBuilder sbPs ;

        public void handleMessage(Message msg) {

            switch (msg.what) {

            /*
             * After FM is enabled dismiss the progress dialog and display the
             * FM main screen. Set the default volume.
             */

            case EVENT_FM_ENABLED:

                Log.i(TAG, "enter handleMessage ----EVENT_FM_ENABLED");
                if (pd != null)
                    pd.dismiss();

                loadDefaultConfiguration();
                setContentView(R.layout.fmrxmain);
                /* At Power up, FM should be always unmuted. */
                mToggleMute = false;
                //Log.i(TAG, " handleMessage  init mToggleMute" + mToggleMute);
                initControls();
                setButtonLabels();
                imgFmPower.setImageResource(R.drawable.poweron);

                break;

             /*
              * Update the icon on main screen with appropriate mono/stereo
              * icon
              */
            case EVENT_MONO_STEREO_CHANGE:

                Log.i(TAG, "enter handleMessage ---EVENT_MONO_STEREO_CHANGE");
                if (mMode == 0) {
                    imgFmMode.setImageResource(R.drawable.fm_stereo);
                } else {
                    imgFmMode.setImageResource(R.drawable.fm_mono);
                }
                break;

             /*
              * Update the icon on main screen with appropriate mono/stereo
              * icon
              */
            case EVENT_MONO_STEREO_DISPLAY:

                Log.i(TAG, "enter handleMessage ---EVENT_MONO_STEREO_DISPLAY");

                Integer mode = (Integer) msg.obj;
                //Log.i(TAG, "enter handleMessage ---mode" + mode.intValue());
                if (mode.intValue() == 0) {
                    imgFmMode.setImageResource(R.drawable.fm_stereo);
                } else {
                    imgFmMode.setImageResource(R.drawable.fm_mono);
                }
                break;

            /*
             * Update the icon on main screen with appropriate mute/unmute icon
             */
            case EVENT_MUTE_CHANGE:
                Log.i(TAG,"enter handleMessage ---EVENT_MUTE_CHANGE  ");

                break;

            case EVENT_SEEK_STOPPED:

                Log.i(TAG, "enter handleMessage ---EVENT_SEEK_STOPPED");
                Integer seekFreq = (Integer) msg.obj;
                          //Log.i(TAG, "enter handleMessage ----EVENT_SEEK_STOPPED seekFreq" + seekFreq);
                lastTunedFrequency = (float) seekFreq / 1000;
                txtStatusMsg.setText(R.string.playing);
                txtFmRxTunedFreq.setText(lastTunedFrequency.toString());

                break;

            case EVENT_FM_DISABLED:

                Log.i(TAG, "enter handleMessage ----EVENT_FM_DISABLED");

                /*
                 * we have exited the FM App. Set the sdefaultSettingOn flag to
                 * false Save the default configuration in the preference
                 */
                sdefaultSettingOn = false;
                saveDefaultConfiguration();
                finish();
                break;

            case EVENT_SEEK_STARTED:

                Integer freq = (Integer) msg.obj;
                          //Log.i(TAG, "enter handleMessage ----EVENT_SEEK_STARTED freq" + freq);
                lastTunedFrequency = (float) freq / 1000;
                txtStatusMsg.setText(R.string.playing);
                txtFmRxTunedFreq.setText(lastTunedFrequency.toString());
                // clear the RDS text
                txtRadioText.setText(null);

                // clear the PS text
                txtPsText.setText(null);

                /*
                 * Seek up/down will be completed here. So set the state to
                 * idle, so that user can seek to other frequency.
                 */

                mSeekState = SEEK_REQ_STATE_IDLE;

                break;

            /*
             * Set the default band , if the fm app is getting started first
             * time
             */
            case EVENT_VOLUME_CHANGE:
                Log.i(TAG, "enter handleMessage ----EVENT_VOLUME_CHANGE");

                /*
                 * volume change will be completed here. So set the state to
                 * idle, so that user can set other volume.
                 */
                mVolState = VOL_REQ_STATE_IDLE;
                /*
                 * Setting the default band after the volume change when FM app
                 * is started for the first time
                 */
                if (sdefaultSettingOn == false) {
                     /* Set the default band */
                        if (MAKE_FM_APIS_BLOCKING == true) {
                                 // Code for blocking call
                               mStatus = sFmRadio.rxSetBand(sBand);
                                if (mStatus == false) {
                            showAlert(getParent(), "FmRadio","Not able to setband!!!!");
                           } else {
                                lastTunedFrequency = (float) lastTunedFrequency ;
                                 mStatus = sFmRadio.rxTune_nb((int)(lastTunedFrequency.floatValue()* 1000));
                            if (mStatus == false) {
                                   showAlert(getParent(), "FmRadio","Not able to tune!!!!");
                            }
                        }

                   } else {
                       // Code for non blocking call
                      mStatus = sFmRadio.rxSetBand_nb(sBand);
                      if (mStatus == false) {
                           showAlert(getParent(), "FmRadio","Not able to setband!!!!");
                          }
                   }



                }
                break;

                case EVENT_COMPLETE_SCAN_PROGRESS:
                    Log.i(TAG, "enter handleMessage ----EVENT_COMPLETE_SCAN_PROGRESS");

                    Integer progress = (Integer) msg.obj;
                      // Log.i(TAG,"enter handleMessage ----EVENT_COMPLETE_SCAN_PROGRESS progress"+ progress);
                    break;

            /*
             * enable audio routing , if the fm app is getting started first
             * time
             */
            case EVENT_TUNE_COMPLETE:
                Integer tuneFreq = (Integer) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_TUNE_COMPLETE tuneFreq"
                                + tuneFreq);
                lastTunedFrequency = (float) tuneFreq / 1000;
                txtStatusMsg.setText(R.string.playing);
                txtFmRxTunedFreq.setText(lastTunedFrequency.toString());
                // clear the RDS text
                txtRadioText.setText(null);
                // clear the PS text
                txtPsText.setText(null);

                //Log.d(TAG, "sdefaultSettingOn" + sdefaultSettingOn);

                /*
                 * Enable the Audio routing after the tune complete , when FM
                 * app is started for the first time or after reentering the Fm
                 * app
                 */
                if (sdefaultSettingOn == false) {
/*
                mStatus = sFmRadio.rxEnableAudioRouting();
                    if (mStatus == false) {
                        showAlert(getParent(), "FmRadio",
                                "Not able to enable audio!!!!");
                    }
*/
                    /*
                     * The default setting for the FMApp are completed here. If
                     * the user selects back and goes out of the FM App, when he
                     * reenters we dont have to do the default configurations
                     */
                    sdefaultSettingOn = true;
                }

                // clear the RDS text
                txtRadioText.setText(null);

                // clear the PS text
                txtPsText.setText(null);

                break;

            /* Display the RDS text on UI */
            case EVENT_RDS_TEXT:
                Log.i(TAG, "enter handleMessage ----EVENT_RDS_TEXT");
                if (FM_SEND_RDS_IN_BYTEARRAY == true) {
                    byte[] rdsText = (byte[]) msg.obj;

                    for (int i = 0; i < 4; i++)
                        Log.i(TAG, "rdsText" + rdsText[i]);
                } else {
                       String rds = (String) msg.obj;
                    Log.i(TAG, "enter handleMessage ----EVENT_RDS_TEXT rds"    + rds);
                       sb = new StringBuilder("[RDS] ");
                       sb.append(rds);
                    Log.i(TAG, "enter handleMessage ----EVENT_RDS_TEXT sb.toString()"+ sb.toString());
                       txtRadioText.setText(sb.toString());
                }
                break;

            /* Display the RDS text on UI */
            case EVENT_PI_CODE:
                String pi = (String) msg.obj;
                Log.i(TAG, "enter handleMessage ----EVENT_PI_CODE rds" + pi);

                break;

            case EVENT_SET_CHANNELSPACE:
                Log.i(TAG, "enter handleMessage ----EVENT_SET_CHANNELSPACE");
                break;


            case EVENT_GET_CHANNEL_SPACE_CHANGE:
                Log.i(TAG, "enter handleMessage ----EVENT_GET_CHANNEL_SPACE_CHANGE");
                Long gChSpace = (Long) msg.obj;
                //Log.d(TAG, "enter handleMessage ----gChSpace" + gChSpace);
                    break;

            /* tune to default frequency after the band change callback . */

            case EVENT_BAND_CHANGE:
                Log.i(TAG, "enter handleMessage ----EVENT_BAND_CHANGE");
                /*
                 * Tune to the last stored frequency at the
                 * enable/re-enable,else tune to the default frequency when band
                 * changes
                 */

                if (sdefaultSettingOn == true) {
                    /* Set the default frequency */
                    if (sBand == FM_BAND_EUROPE_US)
                        lastTunedFrequency = (float) DEFAULT_FREQ_EUROPE;
                    else
                        lastTunedFrequency = (float) DEFAULT_FREQ_JAPAN;
                }

                mStatus = sFmRadio.rxTune_nb((int)(lastTunedFrequency.floatValue()*1000));
                if (mStatus == false) {
                    showAlert(getParent(), "FmRadio", "Not able to tune!!!!");
                }

                break;

            /* Enable RDS system after enale RDS callback . */

            case EVENT_ENABLE_RDS:
                Log.i(TAG, "enter handleMessage ----EVENT_ENABLE_RDS");
                break;

            /* Set RSSI after SET_RDS_AF callback */
            case EVENT_SET_RDS_AF:
                Log.i(TAG, "enter handleMessage ----EVENT_SET_RDS_AF");
                break;
            /* Set RDS AF after SET_RDS_SYSTEM callback */
            case EVENT_SET_RDS_SYSTEM:
                Log.i(TAG, "enter handleMessage ----EVENT_SET_RDS_SYSTEM");
                break;
            /* Set RSSI after disable RDS callback */
            case EVENT_DISABLE_RDS:
                Log.i(TAG, "enter handleMessage ----EVENT_DISABLE_RDS");
                txtPsText.setText(null);
                txtRadioText.setText(null);
                break;

            case EVENT_SET_DEEMP_FILTER:
                Log.i(TAG, "enter handleMessage ----EVENT_SET_DEEMP_FILTER");
                break;

            /* Display the PS text on UI */
            case EVENT_PS_CHANGED:
                Log.i(TAG, "enter handleMessage ----EVENT_PS_CHANGED");

                if (FM_SEND_RDS_IN_BYTEARRAY == true) {
                    byte[] psName = (byte[]) msg.obj;

                    for (int i = 0; i < 4; i++)
                        Log.i(TAG, "psName" + psName[i]);
                } else {
                   String ps = (String) msg.obj;
                   Log.i(TAG, "ps  String is " + ps);
                   sbPs = new StringBuilder("[PS] ");
                   sbPs.append(ps);
                   //Log.i(TAG, "enter handleMessage ----EVENT_PS_CHANGED sbPs.toString()"+ sbPs.toString());
                   txtPsText.setText(sbPs.toString());
                }

                break;

            case EVENT_SET_RSSI_THRESHHOLD:
                Log.i(TAG, "enter handleMessage ----EVENT_SET_RSSI_THRESHHOLD");
                /*
                 * All the configurations will be completed here. So set the
                 * state to idle, so that user can configure again
                 */
                configurationState = CONFIGURATION_STATE_IDLE;
                break;
            case EVENT_SET_RF_DEPENDENT_MUTE:
                Log.i(TAG,"enter handleMessage ----EVENT_SET_RF_DEPENDENT_MUTE");
                break;

            case EVENT_COMPLETE_SCAN_STOP:
                Log.i(TAG, "enter handleMessage ----EVENT_COMPLETE_SCAN_STOP");
                break;

            case EVENT_COMPLETE_SCAN_DONE:
                Log.i(TAG, "enter handleMessage ----EVENT_COMPLETE_SCAN_DONE");

                int[] channelList = (int[]) msg.obj;
                int noOfChannels = (int) msg.arg2;

                Log.i(TAG, "noOfChannels" + noOfChannels);

                for (int i = 0; i < noOfChannels; i++)
                    Log.i(TAG, "channelList" + channelList[i]);

                break;

            case EVENT_GET_BAND :

                Long gBand = (Long) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_GET_BAND");
                Log.d(TAG, "enter handleMessage ----gBand" + gBand);
                   break;

            case EVENT_GET_FREQUENCY :

                Integer gFreq = (Integer) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_GET_FREQUENCY");
                //Log.d(TAG, "enter handleMessage ----gFreq" + gFreq);
                   break;
            case EVENT_GET_VOLUME :
                Long gVol = (Long) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_GET_VOLUME");
                Log.d(TAG, "enter handleMessage ----gVol" + gVol);
                   break;
            case EVENT_GET_MODE :
                Long gMode = (Long) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_GET_MODE");
                Log.d(TAG, "enter handleMessage ----gMode" + gMode);
                   break;
            case EVENT_GET_MUTE_MODE :

                Long gMuteMode = (Long) msg.obj;

                if(gMuteMode ==(long) FM_UNMUTE)
                    imgFmVolume.setImageResource(R.drawable.fm_volume);
                else if (gMuteMode ==(long) FM_MUTE)
                    imgFmVolume.setImageResource(R.drawable.fm_volume_mute);

                Log.i(TAG,"enter handleMessage ----EVENT_GET_MUTE_MODE");
                //Log.d(TAG, "enter handleMessage ----gMuteMode" + gMuteMode);
                     break;
            case EVENT_GET_RF_MUTE_MODE :

                Long gRfMuteMode = (Long) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_GET_RF_MUTE_MODE");
                //Log.d(TAG, "enter handleMessage ----gRfMuteMode" + gRfMuteMode);
                   break;
            case EVENT_GET_RSSI_THRESHHOLD :
                Long gRssi = (Long) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_GET_RSSI_THRESHHOLD");
                Log.d(TAG, "enter handleMessage ----gRssi" + gRssi);
                   break;

            case EVENT_GET_RSSI :
                   Integer rssi = (Integer) msg.obj;
                   Log.i(TAG,"enter handleMessage ----EVENT_GET_RSSI");
                   Log.d(TAG, "enter handleMessage ----rssi" + rssi);
                   break;
            case EVENT_GET_DEEMPHASIS_FILTER :
                Long gFilter= (Long) msg.obj;
                Log.i(TAG,"enter handleMessage ----EVENT_GET_DEEMPHASIS_FILTER");
                Log.d(TAG, "enter handleMessage ----gFilter" + gFilter);
                           break;

            case EVENT_GET_RDS_SYSTEM :
                        Long gRdsSys= (Long) msg.obj;
                Log.i(TAG, "enter handleMessage ----EVENT_GET_RDS_SYSTEM");
                Log.d(TAG, "enter handleMessage ----gRdsSys" + gRdsSys);
            break;
            case EVENT_GET_RDS_GROUPMASK :
                Long gRdsMask= (Long) msg.obj;
                Log.i(TAG, "enter handleMessage ----EVENT_GET_RDS_GROUPMASK");
                Log.d(TAG, "enter handleMessage ----gRdsMask" + gRdsMask);
            break;

            case EVENT_MASTER_VOLUME_CHANGED:
                Integer vol = (Integer) msg.obj;
                mVolume = vol;
                Log.i(TAG, "enter handleMessage ----EVENT_MASTER_VOLUME_CHANGED");
                Log.d(TAG, "enter handleMessage ----mVolume" + vol);
            }
        }
    };

    /* Display alert dialog */
    public void showAlert(Context context, String title, String msg) {

        new AlertDialog.Builder(context).setTitle(title).setIcon(
                android.R.drawable.ic_dialog_alert).setMessage(msg)
                .setNegativeButton(android.R.string.cancel, null).show();

    }


    private void setRdsConfig() {
        Log.i(TAG, "setRdsConfig()-entered");
        configurationState = CONFIGURATION_STATE_PENDING;
        SharedPreferences fmConfigPreferences = getSharedPreferences(
                "fmConfigPreferences", MODE_PRIVATE);

            // Set Band
            int band = fmConfigPreferences.getInt(BAND, DEFAULT_BAND);
            Log.i(TAG, "setRdsConfig()--- band= " + band);
            if (band != sBand) // If Band is same as the one set already donot set
            // it again
            {

            if (MAKE_FM_APIS_BLOCKING == true) {
                  // Code for blocking call
                mStatus = sFmRadio.rxSetBand(band);
                if (mStatus == false) {
                    Log.e(TAG, "setRdsConfig()-- setBand ->Erorr");
                    showAlert(this, "FmRadio",
                            "Cannot  setBand to selected Value!!!!");
                } else {
                    sBand = band;
                    if (sdefaultSettingOn == true) {
                        /* Set the default frequency */
                        if (sBand == FM_BAND_EUROPE_US)
                            lastTunedFrequency = (float) DEFAULT_FREQ_EUROPE;
                        else
                            lastTunedFrequency = (float) DEFAULT_FREQ_JAPAN;
                    }

                    lastTunedFrequency = (float) lastTunedFrequency ;
                    mStatus = sFmRadio.rxTune_nb((int)(lastTunedFrequency.floatValue()*1000));
                    if (mStatus == false) {
                        showAlert(getParent(), "FmRadio", "Not able to tune!!!!");
                    }

                }

            }

            else
                {

                    mStatus = sFmRadio.rxSetBand_nb(band);
                            if (mStatus == false) {
                    Log.e(TAG, "setRdsConfig()-- setBand ->Erorr");
                    showAlert(this, "FmRadio",
                            "Cannot  setBand to selected Value!!!!");
                } else {
                    sBand = band;
                    if (sdefaultSettingOn == true) {
                        /* Set the default frequency */
                        if (sBand == FM_BAND_EUROPE_US)
                            lastTunedFrequency = (float) DEFAULT_FREQ_EUROPE;
                        else
                            lastTunedFrequency = (float) DEFAULT_FREQ_JAPAN;
                    }

                }
    }
}


        // Set De-emp Filter
        int deEmp = fmConfigPreferences.getInt(DEEMP, DEFAULT_DEEMP);
           if (mDeEmpFilter != deEmp)// If De-Emp filter is same as the one set
        // already donot set it again
        {
            if (MAKE_FM_APIS_BLOCKING == true)
            mStatus = sFmRadio.rxSetDeEmphasisFilter(deEmp);
            else
            mStatus = sFmRadio.rxSetDeEmphasisFilter_nb(deEmp);

            Log.i(TAG, "setRdsConfig()--- DeEmp= " + deEmp);

            if (mStatus == false) {
                Log.e(TAG, "setRdsConfig()-- setDeEmphasisFilter ->Erorr");
                showAlert(this, "FmRadio",
                        "Cannot set De-Emp Fileter to selected Value!!!!");

            }
            mDeEmpFilter = deEmp;

        }


                // Set Mode
        int mode = fmConfigPreferences.getInt(MODE, DEFAULT_MODE);
        if (mMode != mode)// If Mode is same as the one set already donot set it
        // again
        {

            if (MAKE_FM_APIS_BLOCKING == true)
            mStatus = sFmRadio.rxSetMonoStereoMode(mode);
            else
            mStatus = sFmRadio.rxSetMonoStereoMode_nb(mode);
            if (mStatus == false) {
                showAlert(this, "FmRadio", "Not able to setmode!!!!");
            } else {
                mMode = mode;
                if (mMode == 0) {
                    imgFmMode.setImageResource(R.drawable.fm_stereo);
                } else {
                    imgFmMode.setImageResource(R.drawable.fm_mono);
                }
            }

        }


        /** Set channel spacing to the one selected by the user */
        int channelSpace = fmConfigPreferences.getInt(CHANNELSPACE,
                DEFAULT_CHANNELSPACE);
        Log.i(TAG, "setChannelSpacing()--- channelSpace= " + channelSpace);
        if (channelSpace != sChannelSpace) // If channelSpace is same as the one
        // set already donot set
        // it again
        {
            if (MAKE_FM_APIS_BLOCKING == true)
                        mStatus = sFmRadio.rxSetChannelSpacing(channelSpace);
                    else
                        mStatus = sFmRadio.rxSetChannelSpacing_nb(channelSpace);

            if (mStatus == false) {
                Log.e(TAG, "setChannelSpacing()-- setChannelSpacing ->Erorr");
                showAlert(this, "FmRadio",
                        "Cannot  setChannelSpacing to selected Value!!!!");
            }
            sChannelSpace = channelSpace;
        }


        // set RDS related configuration
        boolean rdsEnable = fmConfigPreferences.getBoolean(RDS, DEFAULT_RDS);
        Log.i(TAG, "setRDS()--- rdsEnable= " + rdsEnable);
        if (mRds != rdsEnable) {

            if (rdsEnable) {
                    if (MAKE_FM_APIS_BLOCKING == true)
                        mStatus = sFmRadio.rxEnableRds();
                    else
                        mStatus = sFmRadio.rxEnableRds_nb();
                if (mStatus == false) {
                    Log.e(TAG, "setRDS()-- enableRds() ->Erorr");
                    showAlert(this, "FmRadio", "Cannot enable RDS!!!!");
                }

            } else {
                if (MAKE_FM_APIS_BLOCKING == true)
                    mStatus = sFmRadio.rxDisableRds();
                else
                    mStatus = sFmRadio.rxDisableRds_nb();

                if (mStatus == false) {
                    Log.e(TAG, "setRDS()-- disableRds() ->Erorr");
                    showAlert(this, "FmRadio", "Cannot disable RDS!!!!");
                } else {
                    Log.e(TAG, "setRDS()-- disableRds() ->success");
                    /* clear the PS and RDS text */
                    txtPsText.setText(null);
                    txtRadioText.setText(null);
                }
            }
            mRds = rdsEnable;
        }

        // setRdssystem
        int rdsSystem = fmConfigPreferences.getInt(RDSSYSTEM,
                DEFAULT_RDS_SYSTEM);
    if (DBG)
        Log.d(TAG, "setRdsSystem()--- rdsSystem= " + rdsSystem);
        if (mRdsSystem != rdsSystem) {
            // Set RDS-SYSTEM if a new choice is made by the user

            if (MAKE_FM_APIS_BLOCKING == true)
            mStatus = sFmRadio.rxSetRdsSystem(fmConfigPreferences.getInt(
                    RDSSYSTEM, DEFAULT_RDS_SYSTEM));
            else
            mStatus = sFmRadio.rxSetRdsSystem_nb(fmConfigPreferences.getInt(
                    RDSSYSTEM, DEFAULT_RDS_SYSTEM));

            if (mStatus == false) {
                Log.e(TAG, " setRdsSystem()-- setRdsSystem ->Erorr");
                showAlert(this, "FmRadio",
                        "Cannot set Rds System to selected Value!!!!");
            }
            mRdsSystem = rdsSystem;
        }

        boolean rdsAfSwitch = fmConfigPreferences.getBoolean(RDSAF,
                    DEFAULT_RDS_AF);
            int rdsAf = 0;
            rdsAf = rdsAfSwitch ? 1 : 0;
            if (DBG)
            Log.d(TAG, "setRdsAf()--- rdsAfSwitch= " + rdsAf);
            if (mRdsAf != rdsAfSwitch) {
                // Set RDS-AF if a new choice is made by the user

                if (MAKE_FM_APIS_BLOCKING == true)
                mStatus = sFmRadio.rxSetRdsAfSwitchMode(rdsAf);
                else
                mStatus = sFmRadio.rxSetRdsAfSwitchMode_nb(rdsAf);
                if (mStatus == false) {
                    Log.e(TAG, "setRdsAf()-- setRdsAfSwitchMode(1) ->Erorr");
                    showAlert(this, "FmRadio", "Cannot set RDS AF Mode ON!!!!");
                }
                mRdsAf = rdsAfSwitch;
            }
        // Set Rssi
        int rssiThreshHold = fmConfigPreferences.getInt(RSSI, DEFAULT_RSSI);
        Log.i(TAG, "setRssi()-ENTER --- rssiThreshHold= " + rssiThreshHold);

            // Set RSSI if a new value is entered by the user

            if (MAKE_FM_APIS_BLOCKING == true)
                        mStatus = sFmRadio.rxSetRssiThreshold(rssiThreshHold);
                    else
                        mStatus = sFmRadio.rxSetRssiThreshold_nb(rssiThreshHold);

            if (mStatus == false) {
                showAlert(this, "FmRadio", "Not able to setRssiThreshold!!!!");
            }

            mRssi = rssiThreshHold;


            Log.i(TAG, "setRdsConfig()-exit");

    }

    /* Load the Default values from the preference when the application starts */
    private void loadDefaultConfiguration() {

       Log.i(TAG, "loadDefaultConfiguration()-entered");
       SharedPreferences fmConfigPreferences = getSharedPreferences("fmConfigPreferences",
             MODE_PRIVATE);

         if (mPreset)
            saveObject();

       /* Read the stations stored in DB and update the UI */
       try {
          db.open();
          Cursor c = db.getStation(1);
          if (c != null && c.moveToFirst())
             mIsDbPresent = true;
          else
             mIsDbPresent = false;
          c.close();
          db.close();
       } catch (SQLException e) {
          mIsDbPresent = false;
       } catch (Exception ex) {
          mIsDbPresent = false;
       }

       if (mIsDbPresent == false) {

          Log.d(TAG, " mIsDbPresent writeobject" + mIsDbPresent);
          writeObject();
          mIsDbPresent = true;
       } else {
          Log.d(TAG, " mIsDbPresent readobject" + mIsDbPresent);
          readObject();
       }

       Log.d(TAG, " mIsDbPresent " + mIsDbPresent);
        sBand = fmConfigPreferences.getInt(BAND, DEFAULT_BAND);
        lastTunedFrequency = fmConfigPreferences.getFloat(FREQUENCY,
                (sBand == FM_BAND_EUROPE_US ? DEFAULT_FREQ_EUROPE
                        : DEFAULT_FREQ_JAPAN));
        mMode = fmConfigPreferences.getInt(MODE, DEFAULT_MODE);
        mToggleMute = fmConfigPreferences.getBoolean(MUTE, false);
       mRdsState = fmConfigPreferences.getBoolean(RDS, false);

       if (DBG)
          Log.d(TAG, " Load default band " + sBand + "default volume" + mVolume + "last fre"
                + lastTunedFrequency + "mode" + mMode + "mToggleMute" + mToggleMute +"mRdsState"+mRdsState);

    }

    private void createEmptyList() {

       Log.d(TAG, " createEmptyList ");
       stations = new ArrayList<HashMap<String, String>>(6);
       for (int i = 1; i < 7; i++) {
          HashMap<String, String> stationsMap = new HashMap<String, String>();

          stationsMap.put(ITEM_KEY, String.valueOf(i));
          stationsMap.put(ITEM_VALUE, "");
          stationsMap.put(ITEM_NAME, "");
          stations.add(i - 1, stationsMap);
       }

    }
    /* Save the Default values to the preference when the application exits */
    private void saveDefaultConfiguration() {
        Log.i(TAG, "saveDefaultConfiguration()-Entered");

        SharedPreferences fmConfigPreferences = getSharedPreferences(
                "fmConfigPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = fmConfigPreferences.edit();
        editor.putInt(BAND, sBand);
        editor.putBoolean(MUTE, mToggleMute);
        editor.putFloat(FREQUENCY, lastTunedFrequency);
             if (DBG)
            Log.d(TAG, " save default band " + sBand + "default volume" + mVolume
                + "last fre" + lastTunedFrequency+"mToggleMute"+mToggleMute);
        /* Write the stations stored to text file */
             saveObject();
        editor.commit();
    }

    /* Read the Stations from the text file in file system */
    void readObject() {
       Log.i(TAG, "readObject ");

       db.open();
       Cursor c = db.getAllStations();
       int count = c.getCount();

       Log.i(TAG, "readObject cursor count " + count);
       int iCount = 0;
       if (c.moveToFirst()) {
          do {

             HashMap<String, String> stations1 = new HashMap<String, String>();

             stations1.put(ITEM_KEY, c.getString(1));
             stations1.put(ITEM_VALUE, c.getString(2));
             stations1.put(ITEM_NAME, c.getString(3));
             stations.set(iCount, stations1);
             iCount++;

          } while (c.moveToNext());

       } else
          Toast.makeText(this, "No station found", Toast.LENGTH_LONG).show();
       c.close();
       db.close();

    }

    /* Write the Stations to the text file in file system */
    void writeObject() {
        Log.i(TAG, "writeObject ");

       db.open();
       long id;
       id = db.insertStation("1", "", "");
       id = db.insertStation("2", "", " ");
       id = db.insertStation("3", "", "");
       id = db.insertStation("4", "", " ");
       id = db.insertStation("5", "", " ");
       id = db.insertStation("6", "", " ");
       db.close();

    }

    void saveObject() {

       Log.i(TAG, "enter saveObject");
       db.open();
       Iterator iterator = stations.iterator();

       while (iterator.hasNext()) {
          HashMap<String, String> item = (HashMap<String, String>) iterator.next();
          Object v = item.get(ITEM_VALUE);
          Object n = item.get(ITEM_NAME);
          Object i = item.get(ITEM_KEY);

          try {
             mIndex = Integer.parseInt(i.toString());
             updateSetStation((mIndex - 1), v.toString(), n.toString());
             db.updateStation(mIndex, i.toString(), v.toString(), n.toString());
          } catch (Exception e) {
             Log.e(TAG, "Exception");
             e.printStackTrace();
          }

       }

       db.close();

    }
    /* Initialise all the widgets */
    private void initControls() {
        /**
         * Start FM RX
         */
        Log.i(TAG, "enter initControls");

        imgFmPower = (ImageView) findViewById(R.id.imgPower);
        imgFmPower.setOnClickListener(this);

        imgFmMode = (ImageView) findViewById(R.id.imgMode);
        if (mMode == 0) {
        Log.i(TAG, " setting stereo icon" +mMode);
        imgFmMode.setImageResource(R.drawable.fm_stereo);
        } else {
        Log.i(TAG, " setting mono icon" +mMode);
        imgFmMode.setImageResource(R.drawable.fm_mono);
        }

        imgFmVolume = (ImageView) findViewById(R.id.imgMute);
        imgFmVolume.setOnClickListener(this);

        if (mToggleMute == true) {
        imgFmVolume.setImageResource(R.drawable.fm_volume_mute);
        Log.i(TAG, " initControls  mute" +mToggleMute);
        } else {
        imgFmVolume.setImageResource(R.drawable.fm_volume);
        Log.i(TAG, " initControls  mute" +mToggleMute);
        }


        imgFmSeekUp = (ImageButton) findViewById(R.id.imgseekup);
        imgFmSeekUp.setOnClickListener(this);

        imgFmSeekDown = (ImageButton) findViewById(R.id.imgseekdown);
        imgFmSeekDown.setOnClickListener(this);

        txtFmRxTunedFreq = (TextView) findViewById(R.id.txtRxFreq);
        txtFmRxTunedFreq.setText(lastTunedFrequency.toString());

        txtStatusMsg = (TextView) findViewById(R.id.txtStatusMsg);
        txtRadioText = (TextView) findViewById(R.id.txtRadioText);
        txtPsText = (TextView) findViewById(R.id.txtPsText);

        txtStationName = (TextView) findViewById(R.id.txtStationName);
        txtStationName.setText(null);

        btnStation1 = (Button) findViewById(R.id.station1);
        btnStation1.setOnClickListener(this);
        btnStation1.setEnabled(false);

        btnStation2 = (Button) findViewById(R.id.station2);
        btnStation2.setOnClickListener(this);
        btnStation2.setEnabled(false);

        btnStation3 = (Button) findViewById(R.id.station3);
        btnStation3.setOnClickListener(this);
        btnStation3.setEnabled(false);

        btnStation4 = (Button) findViewById(R.id.station4);
        btnStation4.setOnClickListener(this);
        btnStation4.setEnabled(false);

        btnStation5 = (Button) findViewById(R.id.station5);
        btnStation5.setOnClickListener(this);
        btnStation5.setEnabled(false);

        btnStation6 = (Button) findViewById(R.id.station6);
        btnStation6.setOnClickListener(this);
        btnStation6.setEnabled(false);

        // Get the notification manager service.
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }

    /** Adds Delay of 3 seconds */
    private void insertDelayThread() {

        new Thread() {
            public void run() {
                try {
                    // Add some delay to make sure all configuration has been
                    // completed.
                    sleep(3000);
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
          case (ACTIVITY_PRESET): {
             if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Prests saved");
                saveObject();

             }
          }
             break;
        case (ACTIVITY_TUNE): {
            if (resultCode == Activity.RESULT_OK && data != null) {

                Bundle extras = data.getExtras();
                if (extras != null) {

                    lastTunedFrequency = (float) extras.getFloat(FREQ_VALUE, 0);
                    txtFmRxTunedFreq.setText(lastTunedFrequency.toString());
                    mStatus = sFmRadio.rxTune_nb((int)(lastTunedFrequency.floatValue()*1000));
                    if (mStatus == false) {
                        showAlert(this, "FmRadio", "Not able to tune!!!!");
                    }
                }
            }
        }
            break;

        case (ACTIVITY_CONFIG): {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "ActivityFmRdsConfig configurationState "
                        + configurationState);
            if(configurationState == CONFIGURATION_STATE_IDLE)
            {


                    setRdsConfig();
                    configPd = ProgressDialog.show(this, "Please wait..",
                            "Applying new Configuration", true, false);
                            // The delay is inserted to make sure all the configurations
                    // have been completed.
                    insertDelayThread();
                }
            }

        }
            break;

        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_DOWN:

            return true;
        case KeyEvent.KEYCODE_DPAD_UP:

            return true;
        case KeyEvent.KEYCODE_BACK:

            saveDefaultConfiguration();
            finish();
            return true;
            /* Keys A to L are mapped to different get APIs for Testing */
        case KeyEvent.KEYCODE_A:

            if (MAKE_FM_APIS_BLOCKING == true) {
                      // Code for blocking call
                                  Log.i(TAG, "Testing getVolume()  returned volume = "
                    + sFmRadio.rxGetVolume());

                    } else {
                      // Code for non blocking call
                                  Log.i(TAG, "Testing getVolume_nb()  returned volume = "
                    + sFmRadio.rxGetVolume_nb());

                    }


            return true;

        case KeyEvent.KEYCODE_B:

            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                      Log.i(TAG, "Testing getTunedFrequency()  returned Tuned Freq = "
                    + sFmRadio.rxGetTunedFrequency());

        } else {
          // Code for non blocking call
                      Log.i(TAG, "Testing getTunedFrequency_nb()  returned Tuned Freq = "
                    + sFmRadio.rxGetTunedFrequency_nb());

        }

            return true;

        case KeyEvent.KEYCODE_C:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG, "Testing getRssiThreshold()    returned RSSI thrshld = "
                    + sFmRadio.rxGetRssiThreshold());
        } else {
          // Code for non blocking call
                    Log.i(TAG, "Testing getRssiThreshold_nb()    returned RSSI thrshld = "
                    + sFmRadio.rxGetRssiThreshold_nb());
        }

            return true;

        case KeyEvent.KEYCODE_D:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG, "Testing getBand() returned Band  = "
                    + sFmRadio.rxGetBand());
        } else {
          // Code for non blocking call
                    Log.i(TAG, "Testing getBand_nb() returned Band  = "
                    + sFmRadio.rxGetBand_nb());
        }

            return true;

        case KeyEvent.KEYCODE_E:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG, "Testing getDeEmphasisFilter()    returned De-emp  = "
                    + sFmRadio.rxGetDeEmphasisFilter());
        } else {
          // Code for non blocking call
                    Log.i(TAG, "Testing getDeEmphasisFilter_nb()    returned De-emp  = "
                    + sFmRadio.rxGetDeEmphasisFilter_nb());
        }

            return true;

        case KeyEvent.KEYCODE_F:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                      Log.i(TAG, "Testing getMonoStereoMode() returned MonoStereo = "
                    + sFmRadio.rxGetMonoStereoMode());

        } else {
          // Code for non blocking call
                      Log.i(TAG, "Testing getMonoStereoMode_nb() returned MonoStereo = "
                    + sFmRadio.rxGetMonoStereoMode_nb());

        }

            return true;

        case KeyEvent.KEYCODE_G:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG, "Testing getMuteMode()  returned MuteMode = "
                    + sFmRadio.rxGetMuteMode());
        } else {
          // Code for non blocking call
                    Log.i(TAG, "Testing getMuteMode_nb()  returned MuteMode = "
                    + sFmRadio.rxGetMuteMode_nb());
        }

            return true;

        case KeyEvent.KEYCODE_H:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG,
                    "Testing getRdsAfSwitchMode()    returned RdsAfSwitchMode = "
                            + sFmRadio.rxGetRdsAfSwitchMode());
        } else {
          // Code for non blocking call
                    Log.i(TAG,
                    "Testing getRdsAfSwitchMode_nb()    returned RdsAfSwitchMode = "
                            + sFmRadio.rxGetRdsAfSwitchMode_nb());
        }

            return true;

        case KeyEvent.KEYCODE_I:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG, "Testing getRdsGroupMask() returned RdsGrpMask = "
                    + sFmRadio.rxGetRdsGroupMask());
        } else {
          // Code for non blocking call
                    Log.i(TAG, "Testing getRdsGroupMask_nb() returned RdsGrpMask = "
                    + sFmRadio.rxGetRdsGroupMask_nb());
        }

            return true;

        case KeyEvent.KEYCODE_J:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG, "Testing getRdsSystem() returned Rds System = "
                    + sFmRadio.rxGetRdsSystem());
        } else {
          // Code for non blocking call
                    Log.i(TAG, "Testing getRdsSystem_nb() returned Rds System = "
                    + sFmRadio.rxGetRdsSystem_nb());
        }

            return true;

        case KeyEvent.KEYCODE_K:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG,
                    "Testing getRfDependentMuteMode()    returned RfDepndtMuteMode = "
                            + sFmRadio.rxGetRfDependentMuteMode());
        } else {
          // Code for non blocking call
                    Log.i(TAG,
                    "Testing getRfDependentMuteMode_nb()    returned RfDepndtMuteMode = "
                            + sFmRadio.rxGetRfDependentMuteMode_nb());
        }

            return true;

        case KeyEvent.KEYCODE_L:

            if (MAKE_FM_APIS_BLOCKING == true) {

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast,
                    (ViewGroup) findViewById(R.id.toast_layout));
            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText("The current Rssi    " + sFmRadio.rxGetRssi());

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
                }
            else
                {
                Log.i(TAG,
                    "Testing rxGetRssi_nb()    returned  = "
                            + sFmRadio.rxGetRssi_nb());
                }

            return true;

        case KeyEvent.KEYCODE_M:
            Log.i(TAG, "Testing isValidChannel()    returned isValidChannel = "
                    + sFmRadio.rxIsValidChannel());

            return true;

        case KeyEvent.KEYCODE_N:
            Log.i(TAG, "Testing getFwVersion()    returned getFwVersion = "
                    + sFmRadio.rxGetFwVersion());

            return true;

        case KeyEvent.KEYCODE_O:
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                    Log.i(TAG,
                    "Testing getChannelSpacing()    returned getChannelSpacing = "
                            + sFmRadio.rxGetChannelSpacing());
        } else {
          // Code for non blocking call
                    Log.i(TAG,
                    "Testing getChannelSpacing_nb()    returned getChannelSpacing = "
                            + sFmRadio.rxGetChannelSpacing_nb());
        }


            return true;

        case KeyEvent.KEYCODE_P:
            Log.i(TAG, "Testing completescan()");
            sFmRadio.rxCompleteScan_nb();
            return true;

        case KeyEvent.KEYCODE_Q:

            if (MAKE_FM_APIS_BLOCKING == true) {
            Log.i(TAG,
                    "Testing getCompleteScanProgress()    returned scan progress = "
                            + sFmRadio.rxGetCompleteScanProgress());
                }
            else
                {
                            Log.i(TAG,
                    "Testing getCompleteScanProgress()    returned scan progress = "
                            + sFmRadio.rxGetCompleteScanProgress_nb());
                }

            return true;

        case KeyEvent.KEYCODE_R:
            if (MAKE_FM_APIS_BLOCKING == true) {
            Log.i(TAG, "Testing stopCompleteScan()    returned status = "
                    + sFmRadio.rxStopCompleteScan());
                }
            else
                {
                            Log.i(TAG, "Testing stopCompleteScan()    returned status = "
                    + sFmRadio.rxStopCompleteScan_nb());
                }

            return true;

        case KeyEvent.KEYCODE_S:

if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
            Log.i(TAG,
                    "Testing setRfDependentMuteMode()    returned RfDepndtMuteMode = "
                            + sFmRadio.rxSetRfDependentMuteMode(1));
        } else {
          // Code for non blocking call
    Log.i(TAG,
                    "Testing setRfDependentMuteMode()    returned RfDepndtMuteMode = "
                            + sFmRadio.rxSetRfDependentMuteMode_nb(1));
        }

            return true;

        }

        return false;
    }

    /* Get the stored frequency from the arraylist and tune to that frequency */
    void tuneStationFrequency(String text) {
        try {
            float iFreq = Float.parseFloat(text);
            if (iFreq != 0) {
                lastTunedFrequency = (float) iFreq ;
                if (DBG)
                Log.d(TAG, "lastTunedFrequency" + lastTunedFrequency);
                mStatus = sFmRadio.rxTune_nb((int)(lastTunedFrequency.floatValue()*1000));
                if (mStatus == false) {
                    showAlert(getParent(), "FmRadio", "Not able to tune!!!!");
                }
            } else {

                new AlertDialog.Builder(this).setIcon(
                        android.R.drawable.ic_dialog_alert).setMessage(
                        "Enter valid frequency!!").setNegativeButton(
                        android.R.string.ok, null).show();

            }
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "nfe");
        }
    }

    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
        case R.id.imgPower:

            /*
             * The exit from the FM application happens here The audio will be
             * disabled and when he callback for this is recived, FM will be
             * disabled
             */

//            mStatus = sFmRadio.rxDisableAudioRouting();
//            Log.d(TAG, "mStatus is : " + mStatus );

//            try{
//            if (mStatus == false) {
//                showAlert(getParent(), "FmRadio",
//                        "Not able to disable audio!!!!");
//            }
//            }catch (NumberFormatException e) {
//            Log.e(TAG, "showAlert - disableAudioRouting exception  (" + e.toString() + ")");
//            }

            mStatus = sFmRadio.rxDisable();
                    mPreset = false;
            break;
        case R.id.imgMode:
            break;
        //case R.id.imgAudiopath:

            // TODO
            //break;
        case R.id.imgMute:


            Log.d(TAG, " imgMute mToggleMute " + mToggleMute );
            int muteValue =0;
            if(mToggleMute ==true)
            muteValue =1 ;
            else
            muteValue =0;

            if (MAKE_FM_APIS_BLOCKING == true) {
                      // Code for blocking call
                        mStatus = sFmRadio.rxSetMuteMode(muteValue);
            } else {
                      // Code for non blocking call
                        mStatus = sFmRadio.rxSetMuteMode_nb(muteValue);
            }

            if (mStatus == false) {
                showAlert(this, "FmRadio", "Not able to setmute!!!!");
            } else {

                mToggleMute = !mToggleMute ;
                if (mToggleMute == true) {
                    imgFmVolume.setImageResource(R.drawable.fm_volume_mute);
                } else {
                    imgFmVolume.setImageResource(R.drawable.fm_volume);
                }
            }

            break;

        case R.id.imgseekdown:
            mDirection = FM_SEEK_DOWN;
            // FM seek down

            if (mSeekState == SEEK_REQ_STATE_IDLE) {
                txtStationName.setText(null); // set the station name to null
                mStatus = sFmRadio.rxSeek_nb(mDirection);
                if (mStatus == false) {
                    showAlert(this, "FmRadio", "Not able to seek down!!!!");
                } else {
                    mSeekState = SEEK_REQ_STATE_PENDING;
                    txtStatusMsg.setText(R.string.seeking);
                }

            }

            break;
        case R.id.imgseekup:
            mDirection = FM_SEEK_UP;
            // FM seek up
            if (mSeekState == SEEK_REQ_STATE_IDLE) {
                txtStationName.setText(null); // set the station name to null
                mStatus = sFmRadio.rxSeek_nb(mDirection);
                if (mStatus == false) {
                    showAlert(this, "FmRadio", "Not able to seek up!!!!");

                } else {
                    mSeekState = SEEK_REQ_STATE_PENDING;
                    txtStatusMsg.setText(R.string.seeking);
                }
            }

            break;
        case R.id.station1:
            mStationIndex = 0;
            updateStationDisplay(mStationIndex);

            break;
        case R.id.station2:
            mStationIndex = 1;
            updateStationDisplay(mStationIndex);

            break;
        case R.id.station3:
            mStationIndex = 2;
            updateStationDisplay(mStationIndex);

            break;
        case R.id.station4:
            mStationIndex = 3;
            updateStationDisplay(mStationIndex);
            break;

        case R.id.station5:
            mStationIndex = 4;
            updateStationDisplay(mStationIndex);

            break;
        case R.id.station6:
            mStationIndex = 5;
            updateStationDisplay(mStationIndex);
            break;
        }

    }

    /* Gets the stored frequency and tunes to it. */
    void updateStationDisplay(int index) {
        String tunedFreq = null;
        tunedFreq = GetStation(index);
        tuneStationFrequency(tunedFreq);
        txtFmRxTunedFreq.setText(tunedFreq.toString());

    }

    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        MenuItem item;

        item = menu.add(0, MENU_CONFIGURE, 0, R.string.configure);
        item.setIcon(R.drawable.configure);

        item = menu.add(0, MENU_ABOUT, 0, R.string.about);
        item.setIcon(R.drawable.fm_menu_help);

        item = menu.add(0, MENU_EXIT, 0, R.string.exit);
        item.setIcon(R.drawable.icon);

        item = menu.add(0, MENU_PRESET, 0, R.string.preset);
        item.setIcon(R.drawable.fm_menu_preferences);

        item = menu.add(0, MENU_SETFREQ, 0, R.string.setfreq);
        item.setIcon(R.drawable.fm_menu_manage);

        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case MENU_CONFIGURE:
            /* Start the configuration window */
            Intent irds = new Intent(INTENT_RDS_CONFIG);
            startActivityForResult(irds, ACTIVITY_CONFIG);
            break;
        case MENU_PRESET:
            /* Start the Presets window */
            Intent i = new Intent(INTENT_PRESET);
                    mPreset = true;
            startActivity(i);
            break;
        case MENU_EXIT:
            /*
             * The exit from the FM application happens here The audio will be
             * disabled and when he callback for this is recived, FM will be
             * disabled
             */
//            mStatus = sFmRadio.rxDisableAudioRouting();
//            if (mStatus == false) {
//                showAlert(getParent(), "FmRadio",
//                        "Not able to disable audio!!!!");
//            }
//            Log.d(TAG, "MENU_EXIT ------------------- 2");

            mStatus = sFmRadio.rxDisable();
                    mPreset = false;

            break;
        case MENU_ABOUT:
            /* Start the help window */
            Intent iTxHelp = new Intent(INTENT_RXHELP);
            startActivity(iTxHelp);
            break;

        case MENU_SETFREQ:
            /* Start the Manual frequency input window */
            Intent iRxTune = new Intent(INTENT_RXTUNE);
            startActivityForResult(iRxTune, ACTIVITY_TUNE);
            break;

        }
        return super.onOptionsItemSelected(item);
    }

    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        Log.i(TAG, "onSaveInstanceState");
        /* save the fm state into bundle for the activity restart */
        mFmInterrupted = true;
        Bundle fmState = new Bundle();
        fmState.putBoolean(FM_INTERRUPTED_KEY, mFmInterrupted);

        icicle.putBundle(FM_STATE_KEY, fmState);
    }

    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

        if (pd != null)
            pd.dismiss();

        saveDefaultConfiguration();

    }

    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

    }

    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if(mFmServiceConnected == true)
        startup();
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

    // Receives all of the FM intents and dispatches to the proper handler

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String fmAction = intent.getAction();

            Log.i(TAG, "enter onReceive" + fmAction);
            if (fmAction.equals(FmRadioIntent.FM_ENABLED_ACTION)) {
                Log.i(TAG, "enter onReceive FM_ENABLED_ACTION " + fmAction);

                mHandler.sendMessage(mHandler
                        .obtainMessage(EVENT_FM_ENABLED, 0));
            }
            if (fmAction.equals(FmRadioIntent.FM_DISABLED_ACTION)) {
                Log.i(TAG, "enter onReceive FM_DISABLED_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_FM_DISABLED,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.SET_MODE_MONO_STEREO_ACTION)) {
                Log.i(TAG, "enter onReceive SET_MODE_MONO_STEREO_ACTION "
                        + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_MONO_STEREO_CHANGE, 0));
            }
            if (fmAction
                    .equals(FmRadioIntent.DISPLAY_MODE_MONO_STEREO_ACTION)) {
                Log.i(TAG, "enter onReceive DISPLAY_MODE_MONO_STEREO_ACTION "
                        + fmAction);
                Integer modeDisplay = intent.getIntExtra(
                        FmRadioIntent.MODE_MONO_STEREO, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_MONO_STEREO_DISPLAY, modeDisplay));
            }

            if (fmAction.equals(FmRadioIntent.RDS_TEXT_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive RDS_TEXT_CHANGED_ACTION "
                        + fmAction);
                if (FM_SEND_RDS_IN_BYTEARRAY == true) {
                    Bundle extras = intent.getExtras();

                    byte[] rdsText = extras.getByteArray(FmRadioIntent.RDS);
                    int status = extras.getInt(FmRadioIntent.STATUS, 0);

                    mHandler.sendMessage(mHandler.obtainMessage(EVENT_RDS_TEXT,
                            status, 0, rdsText));
                } else {
                    String rdstext = intent
                            .getStringExtra(FmRadioIntent.RADIOTEXT_CONVERTED);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_RDS_TEXT,
                        rdstext));
            }
            }
            if (fmAction.equals(FmRadioIntent.PI_CODE_CHANGED_ACTION)) {
                Log
                        .i(TAG, "enter onReceive PI_CODE_CHANGED_ACTION "
                                + fmAction);

                Integer pi = intent.getIntExtra(FmRadioIntent.PI, 0);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_PI_CODE, pi
                        .toString()));
            }

            if (fmAction.equals(FmRadioIntent.TUNE_COMPLETE_ACTION)) {
                Log.i(TAG, "enter onReceive TUNE_COMPLETE_ACTION " + fmAction);

                int tuneFreq = intent.getIntExtra(
                        FmRadioIntent.TUNED_FREQUENCY, 0);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_TUNE_COMPLETE, tuneFreq));

            }

            if (fmAction.equals(FmRadioIntent.COMPLETE_SCAN_PROGRESS_ACTION)) {
                Log.i(TAG, "enter onReceive COMPLETE_SCAN_PROGRESS_ACTION " + fmAction);

                int progress = intent.getIntExtra(
                        FmRadioIntent.SCAN_PROGRESS, 0);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_COMPLETE_SCAN_PROGRESS, progress));

            }


            if (fmAction.equals(FmRadioIntent.VOLUME_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive VOLUME_CHANGED_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_VOLUME_CHANGE, 0));
            }

            if (fmAction.equals(FmRadioIntent.MUTE_CHANGE_ACTION)) {
                Log.i(TAG, "enter onReceive MUTE_CHANGE_ACTION " + fmAction);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_MUTE_CHANGE,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.SEEK_STOP_ACTION)) {
                Log.i(TAG, "enter onReceive SEEK_STOP_ACTION " + fmAction);

                int freq = intent.getIntExtra(FmRadioIntent.SEEK_FREQUENCY,
                        0);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SEEK_STOPPED,
                        freq));
            }

            if (fmAction.equals(FmRadioIntent.SEEK_ACTION)) {
                Log.i(TAG, "enter onReceive SEEK_ACTION " + fmAction);

                int freq = intent.getIntExtra(FmRadioIntent.SEEK_FREQUENCY,
                        0);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SEEK_STARTED,
                        freq));
            }

            if (fmAction.equals(FmRadioIntent.BAND_CHANGE_ACTION)) {
                Log.i(TAG, "enter onReceive BAND_CHANGE_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_BAND_CHANGE,
                        0));
            }


            if (fmAction.equals(FmRadioIntent.GET_CHANNEL_SPACE_ACTION)) {
                Log.i(TAG, "enter onReceive GET_CHANNEL_SPACE_ACTION " + fmAction);

                Long chSpace = intent.getLongExtra(
                        FmRadioIntent.GET_CHANNEL_SPACE, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_CHANNEL_SPACE_CHANGE, chSpace));
            }


            if (fmAction.equals(FmRadioIntent.SET_CHANNEL_SPACE_ACTION)) {
                Log.i(TAG, "enter onReceive SET_CHANNEL_SPACE_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_CHANNELSPACE, 0));
            }



            if (fmAction.equals(FmRadioIntent.GET_RDS_AF_SWITCH_MODE_ACTION)) {
                        Log.i(TAG, "enter onReceive GET_RDS_AF_SWITCH_MODE_ACTION " + fmAction);

                        Long switchMode = intent.getLongExtra(
                                FmRadioIntent.GET_RDS_AF_SWITCHMODE, 0);
                        mHandler.sendMessage(mHandler.obtainMessage(
                                EVENT_GET_RDS_AF_SWITCHMODE, switchMode));
                    }


            if (fmAction.equals(FmRadioIntent.GET_VOLUME_ACTION)) {
            Log.i(TAG, "enter onReceive GET_VOLUME_ACTION " + fmAction);

            Long gVolume = intent.getLongExtra(
                    FmRadioIntent.GET_VOLUME, 0);
            mHandler.sendMessage(mHandler.obtainMessage(
                    EVENT_GET_VOLUME, gVolume));
        }


                if (fmAction.equals(FmRadioIntent.GET_MONO_STEREO_MODE_ACTION)) {
            Log.i(TAG, "enter onReceive GET_MONO_STEREO_MODE_ACTION " + fmAction);

            Long gMode = intent.getLongExtra(
                    FmRadioIntent.GET_MODE, 0);
            mHandler.sendMessage(mHandler.obtainMessage(
                    EVENT_GET_MODE, gMode));
        }



                        if (fmAction.equals(FmRadioIntent.GET_MUTE_MODE_ACTION)) {
                    Log.i(TAG, "enter onReceive GET_MUTE_MODE_ACTION " + fmAction);

                    Long gMuteMode = intent.getLongExtra(
                            FmRadioIntent.GET_MUTE_MODE, 0);
                    mHandler.sendMessage(mHandler.obtainMessage(
                            EVENT_GET_MUTE_MODE, gMuteMode));
                }




                if (fmAction.equals(FmRadioIntent.GET_BAND_ACTION)) {
                Log.i(TAG, "enter onReceive GET_BAND_ACTION " + fmAction);

                Long gBand = intent.getLongExtra(
                        FmRadioIntent.GET_BAND, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_BAND, gBand));
            }


            if (fmAction.equals(FmRadioIntent.GET_FREQUENCY_ACTION)) {
            Log.i(TAG, "enter onReceive GET_FREQUENCY_ACTION " + fmAction);

            int gFreq = intent.getIntExtra(
                    FmRadioIntent.TUNED_FREQUENCY, 0);
            mHandler.sendMessage(mHandler.obtainMessage(
                    EVENT_GET_FREQUENCY, gFreq));
        }


                                if (fmAction.equals(FmRadioIntent.GET_RF_MUTE_MODE_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RF_MUTE_MODE_ACTION " + fmAction);

                Long gRfMuteMode = intent.getLongExtra(
                        FmRadioIntent.GET_RF_MUTE_MODE, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RF_MUTE_MODE, gRfMuteMode));
            }



                if (fmAction.equals(FmRadioIntent.GET_RSSI_THRESHHOLD_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RSSI_THRESHHOLD_ACTION " + fmAction);

                Long gRssiThreshhold = intent.getLongExtra(
                        FmRadioIntent.GET_RSSI_THRESHHOLD, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RSSI_THRESHHOLD, gRssiThreshhold));
            }


                if (fmAction.equals(FmRadioIntent.GET_DEEMPHASIS_FILTER_ACTION)) {
                Log.i(TAG, "enter onReceive GET_DEEMPHASIS_FILTER_ACTION " + fmAction);

                Long gFilter = intent.getLongExtra(
                        FmRadioIntent.GET_DEEMPHASIS_FILTER, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_DEEMPHASIS_FILTER, gFilter));
            }


                if (fmAction.equals(FmRadioIntent.GET_RSSI_ACTION)) {
                    Log.i(TAG, "enter onReceive GET_RSSI_ACTION " + fmAction);

                    int gRssi = intent.getIntExtra(
                            FmRadioIntent.GET_RSSI, 0);
                    mHandler.sendMessage(mHandler.obtainMessage(
                            EVENT_GET_RSSI, gRssi));
                }


                if (fmAction.equals(FmRadioIntent.GET_RDS_SYSTEM_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RDS_SYSTEM_ACTION " + fmAction);

                Long gRdsSystem = intent.getLongExtra(
                        FmRadioIntent.GET_RDS_SYSTEM, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RDS_SYSTEM, gRdsSystem));
            }


            if (fmAction.equals(FmRadioIntent.GET_RDS_GROUPMASK_ACTION)) {
                Log.i(TAG, "enter onReceive GET_RDS_GROUPMASK_ACTION " + fmAction);

                Long gRdsMask = intent.getLongExtra(
                        FmRadioIntent.GET_RDS_GROUPMASK, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_GET_RDS_GROUPMASK, gRdsMask));
            }



            if (fmAction.equals(FmRadioIntent.ENABLE_RDS_ACTION)) {
                Log.i(TAG, "enter onReceive ENABLE_RDS_ACTION " + fmAction);

                mHandler.sendMessage(mHandler
                        .obtainMessage(EVENT_ENABLE_RDS, 0));
            }

            if (fmAction.equals(FmRadioIntent.DISABLE_RDS_ACTION)) {
                Log.i(TAG, "enter onReceive DISABLE_RDS_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISABLE_RDS,
                        0));
            }

            if (fmAction.equals(FmRadioIntent.SET_RDS_AF_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RDS_AF_ACTION " + fmAction);

                mHandler.sendMessage(mHandler
                        .obtainMessage(EVENT_SET_RDS_AF, 0));
            }

            if (fmAction.equals(FmRadioIntent.SET_RDS_SYSTEM_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RDS_SYSTEM_ACTION " + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_RDS_SYSTEM, 0));
            }

            if (fmAction.equals(FmRadioIntent.SET_DEEMP_FILTER_ACTION)) {
                Log.i(TAG, "enter onReceive SET_DEEMP_FILTER_ACTION "
                        + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_DEEMP_FILTER, 0));
            }

            if (fmAction.equals(FmRadioIntent.PS_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive PS_CHANGED_ACTION " + fmAction);

                if (FM_SEND_RDS_IN_BYTEARRAY == true) {
                    Bundle extras = intent.getExtras();
                    byte[] psName = extras.getByteArray(FmRadioIntent.PS);
                    int status = extras.getInt(FmRadioIntent.STATUS, 0);

                mHandler.sendMessage(mHandler.obtainMessage(
                            EVENT_PS_CHANGED, status, 0, psName));
                } else {

                    String name = intent
                            .getStringExtra(FmRadioIntent.PS_CONVERTED);

                mHandler.sendMessage(mHandler.obtainMessage(
                            EVENT_PS_CHANGED, name));
                }
            }

            if (fmAction.equals(FmRadioIntent.SET_RSSI_THRESHHOLD_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RSSI_THRESHHOLD_ACTION "
                        + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_RSSI_THRESHHOLD, 0));
            }

            if (fmAction.equals(FmRadioIntent.SET_RF_DEPENDENT_MUTE_ACTION)) {
                Log.i(TAG, "enter onReceive SET_RF_DEPENDENT_MUTE_ACTION "
                        + fmAction);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_RF_DEPENDENT_MUTE, 0));
            }

            if (fmAction.equals(FmRadioIntent.COMPLETE_SCAN_DONE_ACTION)) {
                Log.i(TAG, "enter onReceive COMPLETE_SCAN_DONE_ACTION "
                        + fmAction);

                Bundle extras = intent.getExtras();

                int[] channelList = extras
                        .getIntArray(FmRadioIntent.SCAN_LIST);

                int noOfChannels = extras.getInt(
                        FmRadioIntent.SCAN_LIST_COUNT, 0);

                int status = extras.getInt(FmRadioIntent.STATUS, 0);

                Log.i(TAG, "noOfChannels" + noOfChannels);

                for (int i = 0; i < noOfChannels; i++)

                    Log.i(TAG, "channelList" + channelList[i]);

                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_COMPLETE_SCAN_DONE, status, noOfChannels,
                        channelList));
            }

            if (fmAction.equals(FmRadioIntent.COMPLETE_SCAN_STOP_ACTION)) {
                Log.i(TAG, "enter onReceive COMPLETE_SCAN_STOP_ACTION "
                        + fmAction);
                Bundle extras = intent.getExtras();
                int status = extras.getInt(FmRadioIntent.STATUS, 0);
                int channelValue = extras.getInt(
                FmRadioIntent.LAST_SCAN_CHANNEL, 0);
                Log.i(TAG, "Last Scanned Channel Frequency before calling Stop Scan" + channelValue);
                mHandler.sendMessage(mHandler.obtainMessage(
                EVENT_COMPLETE_SCAN_STOP, status , channelValue));
            }

            if (fmAction.equals(FmRadioIntent.MASTER_VOLUME_CHANGED_ACTION)) {
                Log.i(TAG, "enter onReceive MASTER_VOLUME_CHANGED_ACTION "
                        + fmAction);
                mVolume = intent.getIntExtra(FmRadioIntent.MASTER_VOLUME, 0);
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_MASTER_VOLUME_CHANGED, mVolume));
            }

        }
    };

    /* Get the volume */
    void getNewGain(int volume) {
        Log.d(TAG, "getNewGain" + volume);
        if (volume <= GAIN_STEP) {
            mVolume = MIN_VOLUME;
        }

        else if (volume >= MAX_VOLUME) {
            mVolume = MAX_VOLUME;
        } else {
            mVolume = volume;
        }
    }

}

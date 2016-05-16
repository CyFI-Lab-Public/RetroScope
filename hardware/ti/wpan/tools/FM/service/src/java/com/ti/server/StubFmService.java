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


package com.ti.server;

import com.ti.fm.FmRadio;
import com.ti.fm.IFmRadio;
import com.ti.fm.FmRadioIntent;
import com.ti.fm.IFmConstants;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.os.Handler;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue; /*  (additional packages required) */
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import android.os.ServiceManager;
import android.os.SystemClock;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent; /*need wakelock for delayed poweroff */
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.view.KeyEvent;
import java.io.IOException;
import android.content.res.Resources;
import android.content.ComponentName;
import com.ti.jfm.core.*;
import com.ti.server.R.*;


/*************************************************************************************************
 * Provides FM Radio functionality as a service in the Android Framework.
 * There is a single instance of this class that is created during system
 * startup as part of the system server. The class exposes its services via
 * IFmRadio.aidl.
 *
 * @hide
 *************************************************************************************************/

public class StubFmService extends IFmRadio.Stub implements
        JFmRx.ICallback,JFmTx.ICallback, IFmConstants {

    private static final String TAG = "StubFmService";
    private static final boolean DBG = false;
    private static final String FMRX_ADMIN_PERM = "ti.permission.FMRX_ADMIN";
    private static final String FMRX_PERM = "ti.permission.FMRX";
    public static final String FM_SERVICE = "StubFmService";
    private static final String AUDIO_RX_ENABLE = "audio_rx_enable";
    private static final String AUDIO_TX_ENABLE = "audio_tx_enable";

    /*FM specific commands*/
    public static final String FM_PAUSE_CMD = "com.ti.server.fmpausecmd";
    public static final String FM_RESUME_CMD = "com.ti.server.fmresumecmd";
    private static final String FM_RESTORE_VALUES = "com.ti.server.fmrestorecmd";
    public static final String FM_MUTE_CMD = "com.ti.server.fmmutecmd";
    public static final String FM_UNMUTE_CMD = "com.ti.server.fmunmutecmd";


    /*As the Alarm intents are not published in teh framework, using the string explicitly */
    // TODO: Publish these intents
    // This is a public action used in the manifest for receiving Alarm broadcasts
    // from the alarm manager.
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    // A public action sent by AlarmKlaxon when the alarm has stopped sounding
    // for any reason (e.g. because it has been dismissed from AlarmAlertFullScreen,
    // or killed due to an incoming phone call, etc).
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

       public static final String MUSIC_PAUSE_ACTION ="com.android.music.musicservicecommand";
       public static final String CMDPAUSE = "pause";

       private static final String ACTION_FMRx_PLUG = "ti.android.intent.action.FMRx_PLUG";
       private static final String ACTION_FMTx_PLUG = "ti.android.intent.action.FMTx_PLUG";
       private boolean mIsFmTxOn = false;

    private AudioManager mAudioManager = null;
    TelephonyManager tmgr;
    private Context mContext = null;

    private ComponentName mRemoteControlResponder;

    /*FM TX and RX notification Ids*/
    // TODO: When FM Service is made as an App Service, these will have to be moved there.
    private int FM_RX_NOTIFICATION_ID;
    private int FM_TX_NOTIFICATION_ID;

    /*
     * variable to make sure that the next volume change happens after the
     * current volume request has been completed.
     */
    private boolean mVolState = VOL_REQ_STATE_IDLE;

    /* Varibale to check whether to resume FM after call.*/

    private boolean mResumeAfterCall = false;

    private boolean mIsFmMuted = false;

    private Object mVolumeSynchronizationLock = new Object();


    /*  ***********Constants *********************** */
    private Handler mDelayedDisableHandler;
    private DelayedDisable mDelayedDisable;
    private DelayedPauseDisable mDelayedPauseDisable;
    private static final int FM_DISABLE_DELAY = 50;
    private WakeLock mWakeLock = null;
    private JFmRx mJFmRx;
    private JFmTx mJFmTx;

    private int mRxState = FmRadio.STATE_DEFAULT; // State of the FM Rx Service
    private int mTxState = STATE_DEFAULT; // State of the FM Tx Service

    private int mVolume = DEF_VOL; // to store the App Volume value
    /*
     * Variable to store the volume , so that when the value is changed intimate
     * to the application
     */
    private int mLastVolume = DEF_VOL;

    /* Variable to store the current Band */
    private static int mCurrentBand = FM_BAND_EUROPE_US;

    /* Variable to store the current tuned frequency */
    private static int mCurrentFrequency = FM_FIRST_FREQ_US_EUROPE_KHZ;

    /* Variable to store the current mute mode */
    private static int mCurrentMuteMode = FM_NOT_MUTE;
    /* Variable to store the current RDS state */
    private static boolean mCurrentRdsState = false;
    /*
     * Variable to store the Mode , so that when the value is changed ,intimate
     * to the application
     */
    private JFmRx.JFmRxMonoStereoMode mMode = JFmRx.JFmRxMonoStereoMode.FM_RX_STEREO;
    /*
     * Variable to store the PiCode , so that when the value is changed
     * ,intimate to the application
     */
    static int last_msg_printed = 255;

    /*
     * To identify the pause/resume of the FM and reload the values
     * accordingly(band,volume,freq)
     */
    private int mFmPauseResume =STATE_DISABLED;
    private IntentFilter mIntentFilter;

    /* Variables to store the return value of the FM APIs */
    private static int getBandValue = FM_BAND_EUROPE_US;
    private static int getMonoStereoModeValue = 0;
    private static int getMuteModeValue = FM_NOT_MUTE;
    private static int getRfDependentMuteModeValue = FM_RF_DEP_MUTE_OFF;
    private static int getRssiThresholdValue = FM_RSSI_THRESHHOLD;
    private static int getDeEmphasisFilterValue = 0;
    private static int getVolumeValue = FM_MAX_VOLUME / 2;
    private static int getChannelSpaceValue = FM_CHANNEL_SPACE;
    private static int getTunedFrequencyValue = FM_UNDEFINED_FREQ;
    private static int getRssiValue = 0;
    private static int getRdsSystemValue = 0;
    private static long getRdsGroupMaskValue = FM_UNDEFINED_FREQ;
    private static int getRdsAfSwitchModeValue = 0;
    private static double getFwVersion = 0.0;
    private static int getScanProgress = 0;
    private static boolean mIsValidChannel = false;
    private static int mStopCompleteScanStatus = 0;

    /*Variable to protect the FM APIS when Seek,Tune,CompleteScan is in progress*/
    private static boolean mIsCompleteScanInProgress = false;
    private static boolean mIsSeekInProgress = false;
    private static boolean mIsTuneInProgress = false;


    /* Varibale for fm TX zoom2 audio deafult */
    private static final int fmTxDeafultCalResource=0; /*CAL_RESOURCE_I2SH*/
    private static final int fmTxDeafultSampleFrequency=7; /*CAL_SAMPLE_FREQ_44100*/

    /*
     * Listener for Incoming call. Disable the FM and let the call proceed.
     */

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                /* Don't handle pause/resume if FM radio is off */
                if (!rxIsEnabled()) {
                    return;
                }
                Log.d(TAG,"onCallStateChanged:CALL_STATE_RINGING.Pause FM Radio ");
                pauseFm();
                /* Turn off speakerphone before call and save state */
                if (!mResumeAfterCall) {
                    /* only turn off speaker if not already paused */
                    mAudioManager.setSpeakerphoneOn(false);
                }
                mResumeAfterCall = true;

            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                /* pause the music while a conversation is in progress.
                 Don't handle pause/resume if FM radio is off */
                if (!rxIsEnabled()) {
                    return;
                }
                Log.d(TAG,"onCallStateChanged:CALL_STATE_OFFHOOK Pause FM Radio  ");
                pauseFm();
                /* Turn off speakerphone before call and save state */
                if (!mResumeAfterCall) {
                    /* only turn off speaker if not already paused */
                    mAudioManager.setSpeakerphoneOn(false);
                }
                mResumeAfterCall = true;

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                // start playing again
                if (DBG)
                    Log.d(TAG, "onCallStateChanged:CALL_STATE_IDLE) ");

                if (mResumeAfterCall) {
                    /* resume playback only if FM was playing
                     when the call was answered */
                    if (!rxIsFMPaused() ) {
                        return;
                    }
                    resumeFm();
                    mResumeAfterCall = false;
                }

            }
        }
    };

    /*************************************************************************************************
     *  Constructor
     *************************************************************************************************/

    public StubFmService() {

        super();
        //Log.d(TAG, "StubFmService: null constructor called");
    }

    private void initialise(Context context) {
        //Log.d(TAG, "StubFmService:  initialise called");
        // Save the context to be used later
        mContext = context;
        mRxState = STATE_DEFAULT;
        mFmPauseResume =STATE_DEFAULT;

        mDelayedDisable = new DelayedDisable();
        mDelayedPauseDisable = new DelayedPauseDisable();
        mDelayedDisableHandler = new Handler();
        mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        tmgr = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        PowerManager powerManager = (PowerManager)context.
                          getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
                mWakeLock = powerManager.
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
             }
             else {
            Log.e(TAG, "Failed to get Power Manager.");
            mWakeLock = null;
        }

        if (mWakeLock != null) {
            mWakeLock.setReferenceCounted(false);
             }
             else {
            Log.e(TAG, "Failed to create WakeLock.");
        }

       mRemoteControlResponder = new ComponentName(mContext.getPackageName(),
             MediaButtonBroadcastReceiver.class.getName());

    }

    /*************************************************************************************************
     * Must be called after construction, and before any other method.
     *************************************************************************************************/
    public synchronized void init(Context context) {

        initialise(context);
        // create a single new JFmRx instance
        mJFmRx = new JFmRx();
        // create a single new JFmTx instance
        mJFmTx = new JFmTx();

    }

    /*************************************************************************************************
     * Must be called when the service is no longer needed.But is the service
     * really going down?
     *************************************************************************************************/
    public synchronized void close() {
        JFmRxStatus status;
        //Log.d(TAG, "StubFmService:  close ");
        try {
            tmgr.listen(mPhoneStateListener, 0);
            // destroy the underlying FMRX & FMTX
            destroyJFmRx();
            destroyJFmTx();
        } catch (Exception e) {
            Log.e(TAG, "close: Exception thrown during close (" + e.toString()
                    + ")");
            return;
        }

    }
       /*
        * L27 Specific
        */

       public void enableRx(int state) {
         Intent fm_intent = new Intent(ACTION_FMRx_PLUG);
        fm_intent.putExtra("state", state);
          mContext.sendBroadcast(fm_intent);
       }

        public void enableTx(int state) {
                Intent fm_intent = new Intent(ACTION_FMTx_PLUG);
                fm_intent.putExtra("state", state);
                mContext.sendBroadcast(fm_intent);
           if (state == 1)
             mIsFmTxOn = true;
           else
             mIsFmTxOn = false;
    }

       public boolean isTransmissionOn() {
           return mIsFmTxOn;
       }
    /*************************************************************************************************
     * Must be called after construction, and before any other method.
     *************************************************************************************************/

    public boolean rxIsEnabled() {
        //Log.d(TAG, "StubFmService:  rxIsEnabled ");
        mContext.enforceCallingOrSelfPermission(FMRX_PERM,
                "Need FMRX_PERM permission");
        return (mRxState == STATE_ENABLED);

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public int rxGetFMState() {
        mContext.enforceCallingOrSelfPermission(FMRX_PERM,
                "Need FMRX_PERM permission");
        Log.i(TAG, "rxGetFMState mRxState" +mRxState );
        return mRxState;
    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    public boolean rxIsFMPaused() {
        mContext.enforceCallingOrSelfPermission(FMRX_PERM,
                "Need FMRX_PERM permission");
        Log.i(TAG, "rxIsFMPaused mFmPauseResume" + mFmPauseResume);
        return (mFmPauseResume == STATE_PAUSE);
    }

    /*************************************************************************************************
     * Implementation of conversion from Unsigned to nSigned Integer
     *************************************************************************************************/
    private int convertUnsignedToSignedInt(long a) {

        int nReturnVal;

        if ((a > 65535) || (a < 0)) {
            Log.d(TAG,"convertUnsignedToSignedInt: Error in conversion from Unsigned to nSigned Integer");
            nReturnVal = 0;
            return nReturnVal;
        }

        if (a > 32767)
            nReturnVal = (int) (a - 65536);
        else
            nReturnVal = (int) a;

        return nReturnVal;
    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public synchronized boolean rxEnable() {
        Log.i(TAG, "StubFmService:  rxEnable ");

        if(IsFmTxEnabled()==true){
            Log.e(TAG, "StubFmService rxEnable: FM TX is enabled could not Enable fm RX");
                     return false;
                        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        mDelayedDisableHandler.removeCallbacks(mDelayedDisable);
        mDelayedDisableHandler.removeCallbacks(mDelayedPauseDisable);
        JFmRxStatus status;
        try {
        /* Tell the music playback service to pause*/
            // TODO: these constants need to be published somewhere in the
             //framework
            Intent i = new Intent("com.android.music.musicservicecommand");
            i.putExtra("command", "pause");
            mContext.sendBroadcast(i);

            /*
             * register for the master Volume control Intent and music/video
             * playback
             */
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
            mIntentFilter.addAction(FM_PAUSE_CMD);
            mIntentFilter.addAction(FM_RESUME_CMD);
            /*This is for the  SMS and other notifications*/
            mIntentFilter.addAction(FM_MUTE_CMD);
            mIntentFilter.addAction(FM_UNMUTE_CMD);
            mIntentFilter.addAction(FM_RESTORE_VALUES);
            /*This is for the  Alarm notifications*/
            mIntentFilter.addAction(ALARM_ALERT_ACTION);
            mIntentFilter.addAction(ALARM_DONE_ACTION);
                    mIntentFilter.addAction(MUSIC_PAUSE_ACTION);
            mContext.registerReceiver(mFmRxIntentReceiver, mIntentFilter);

            /*Seperate Receiver for Headset*/
            IntentFilter inf = new IntentFilter();
            inf.addAction(Intent.ACTION_HEADSET_PLUG);
            mContext.registerReceiver(mHeadsetPlugReceiver, inf);

            /*Seperate Receiver for MediaButton*/
            mAudioManager.registerMediaButtonEventReceiver(
            mRemoteControlResponder);

        if((mRxState!= STATE_DEFAULT)&&(mRxState!= STATE_DISABLED))
              switch (mRxState) {
                   case STATE_ENABLED:
                      return true;
               default:
                  return false;
            }

            // try communicating for 5 seconds (by trying to create a valid FmRX context)
           boolean fmRxCreated = false;
          for (int count = 0; count< 10; count++) {
               //Log.i(TAG, "FmRxEnable: FmRx create try #" + count);
          //Log.i(TAG, "FmRxEnable: mJFmRx is:"+ mJFmRx);
             status = mJFmRx.create(this);
             //Log.i(TAG, "FmRxEnable: FmRx create returned " + status.toString());
             if (status == JFmRxStatus.SUCCESS) {
                fmRxCreated = true;
                break;
             }
             SystemClock.sleep(500);
          }

          if (fmRxCreated == false) {
             Log.e(TAG, "FmRxEnable: FmRx create failed. Aborting");
             return false;
          }

            status = mJFmRx.enable();
            Log.i(TAG, "mJFmRx.enable returned status " + status.toString());

            /* If the Operation Fail, Send false to the user and reset the MCP Monitor */
            if (status != JFmRxStatus.PENDING && status != JFmRxStatus.SUCCESS){
                Log.e(TAG, "mJFmRx.enable returned status "+ status.toString());
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "enable: Exception thrown during enable ("
                    + e.toString() + ")");
            return false;
        }
        mRxState = STATE_ENABLING;
        mFmPauseResume =STATE_DEFAULT;

          // if its already on, call the event
           if (status == JFmRxStatus.SUCCESS)
               {
               updateEnableConfiguration();
               enableIntent(JFmRxStatus.SUCCESS);
               }
        else {
            // set a delayed timeout message
            }
        return true;

    }
    private void  destroyJFmRx() {
        //Log.i(TAG, "StubFmService:  destroyJFmRx ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        try {
            if(mContext!= null)
            mContext.unregisterReceiver(mFmRxIntentReceiver);
            mContext.unregisterReceiver(mHeadsetPlugReceiver);
               mAudioManager.unregisterMediaButtonEventReceiver(
                     mRemoteControlResponder);

        if (mJFmRx != null) {
                mJFmRx.destroy();
            }

        } catch (Exception e) {
            Log.e(TAG, "destroyJFmRx: Exception thrown during destroy ("
                    + e.toString() + ")");
        }

        mRxState = STATE_DEFAULT;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    public synchronized boolean rxDisable() {
        Log.i(TAG, "StubFmRxService:  rxDisable ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "disable error: fm not enabled " + mRxState);
            return false;
        }

        mDelayedDisableHandler.postDelayed(mDelayedDisable, FM_DISABLE_DELAY);

        return true;

    }

    /*************************************************************************************************
     * Implementation DelayedDisable runnable class
     *************************************************************************************************/
    private class DelayedDisable implements Runnable {

        public final void run() {
            if ((mWakeLock != null) && (mWakeLock.isHeld())) {
                mWakeLock.release();
            }
            if (mRxState == STATE_ENABLED) {
                boolean success = true;
                try {
                    JFmRxStatus status = mJFmRx.disable();
                    if (DBG)
                        Log.d(TAG, "mJFmRx.disable returned status "
                                + status.toString());
                    if (JFmRxStatus.PENDING != status) {
                        success = false;
                        Log.e(TAG, "mJFmRx.disable returned status "
                                + status.toString());
                    }
                } catch (Exception e) {
                    success = false;
                    Log.e(TAG, "disable: Exception thrown during disable ("
                            + e.toString() + ")");
                }
                if (success) {
                    mRxState = STATE_DISABLING;
                    mFmPauseResume = STATE_DEFAULT;
                }
            } else {
                Log.e(TAG, "disable error: fm not enabled " + mRxState);
            }
        }
    }

    private class DelayedPauseDisable implements Runnable {
        public final void run() {
            if ((mWakeLock != null) && (mWakeLock.isHeld())) {
                mWakeLock.release();
            }
        // if its already off, call the completion function
            if (mRxState == STATE_ENABLED) {
                boolean success = true;
                try {
                    JFmRxStatus status = mJFmRx.disable();
                    if (DBG)
                        Log.d(TAG, "mJFmRx.disable returned status "
                                + status.toString());
                    if (JFmRxStatus.PENDING != status) {
                        success = false;
                        Log.e(TAG, "mJFmRx.disable returned status "
                                + status.toString());
                    }
                } catch (Exception e) {
                    success = false;
                    Log.e(TAG, "disable: Exception thrown during disable ("
                            + e.toString() + ")");
            }
                if (success) {
                    mRxState = STATE_PAUSE;
                    mFmPauseResume = STATE_PAUSE;
                }
            } else {
                Log.e(TAG, "disable error: fm not enabled " + mRxState);
        }
        }

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* separate queue for each set call synchronization */
    private BlockingQueue<String> mSetBandSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public boolean rxSetBand(int band) {
        Log.i(TAG, "StubFmService:rxSetBand   band " + band);
        mCurrentBand = band;
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetBand: failed, fm not enabled state " + mRxState);
            return false;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


        JFmRx.JFmRxBand lBand = JFmUtils.getEnumConst(JFmRx.JFmRxBand.class,
                band);
        if (lBand == null) {
            Log.e(TAG, "StubFmService:rxSetBand invalid  lBand " + lBand);
            return false;
        }

        JFmRxStatus status = mJFmRx.setBand(lBand);
        Log.i(TAG, "mJFmRx.setBand returned status " + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setBand returned status " + status.toString());
            return false;
        }

        /*implementation to make the set API Synchronous */
        try {
            String syncString = mSetBandSyncQueue.take();
            if (!syncString.equals("*")) {
                Log.e(TAG, "wrong sync string reseived: " + syncString);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "mJFmRx.setBand-- Wait() s Exception!!!");
        }

} else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxSetBand():--------- Exiting... ");

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    public boolean rxSetBand_nb(int band) {
        Log.i(TAG, "StubFmService:rxSetBand_nb   band " + band);
        mCurrentBand = band;
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetBand_nb: failed, fm not enabled state " + mRxState);
            return false;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxBand lBand = JFmUtils.getEnumConst(JFmRx.JFmRxBand.class,
                band);
        if (lBand == null) {
            Log.e(TAG, "StubFmService:rxSetBand_nb invalid  lBand " + lBand);
            return false;
        }

        JFmRxStatus status = mJFmRx.setBand(lBand);
        Log.i(TAG, "mJFmRx.setBand returned status " + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setBand returned status " + status.toString());
            return false;
        }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mBandSyncQueue = new LinkedBlockingQueue<String>(
            5);
    public synchronized int rxGetBand() {

        Log.i(TAG, "StubFmService:rxGetBand  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetBand: failed, fm not enabled  state " + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

    if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
            JFmRxStatus status = mJFmRx.getBand();
            Log.i(TAG, "mJFmRx.getBand returned status " + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getBand returned status "
                        + status.toString());
                return 0;
            }

            /* implementation to make the FM API Synchronous */
                try {
                String syncString = mBandSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
                } catch (InterruptedException e) {
                    Log.e(TAG, "mJFmRx.getBand-- Wait() s Exception!!!");
                }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetBand():--------- Exiting... ");
        return getBandValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public  boolean rxGetBand_nb() {

        Log.i(TAG, "StubFmService:rxGetBand_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetBand_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getBand();
            Log.i(TAG, "mJFmRx.getBand returned status " + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getBand returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mSetMonoStereoModeSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public boolean rxSetMonoStereoMode(int mode) {
        Log.i(TAG, "StubFmService:rxSetMonoStereoMode mode  " + mode);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetMonoStereoMode: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


        JFmRx.JFmRxMonoStereoMode lMode = JFmUtils.getEnumConst(
                JFmRx.JFmRxMonoStereoMode.class, mode);
        if (lMode == null) {
            Log.e(TAG, "StubFmService:rxSetMonoStereoMode invalid  lBand " + lMode);
            return false;
        }
        JFmRxStatus status = mJFmRx.setMonoStereoMode(lMode);
        Log.i(TAG, "mJFmRx.setMonoStereoMode returned status "
                + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setMonoStereoMode returned status "
                    + status.toString());
            return false;
        }

        /*implementation to make the set API Synchronous */
        try {
            String syncString = mSetMonoStereoModeSyncQueue.take();
            if (!syncString.equals("*")) {
                Log.e(TAG, "wrong sync string reseived: " + syncString);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "mJFmRx.setMonoStereoMode-- Wait() s Exception!!!");
        }

} else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxSetMonoStereoMode exiting");
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetMonoStereoMode_nb(int mode) {
        Log.i(TAG, "StubFmService:rxSetMonoStereoMode_nb mode  " + mode);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetMonoStereoMode_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxMonoStereoMode lMode = JFmUtils.getEnumConst(
                JFmRx.JFmRxMonoStereoMode.class, mode);
        if (lMode == null) {
            Log.e(TAG, "StubFmService:rxSetMonoStereoMode_nb invalid  lBand "
                    + lMode);
            return false;
        }
        JFmRxStatus status = mJFmRx.setMonoStereoMode(lMode);
        Log.i(TAG, "mJFmRx.setMonoStereoMode returned status "
                + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setMonoStereoMode returned status "
                    + status.toString());
            return false;
        }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mMonoStereoModeSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetMonoStereoMode() {
        Log.i(TAG, "StubFmService:rxGetMonoStereoMode  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetMonoStereoMode: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getMonoStereoMode();
            Log.i(TAG, "mJFmRx.getMonoStereoMode returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getMonoStereoMode returned status "
                        + status.toString());
                return 0;
            }

            /*implementation to make the FM API Synchronous */

                try {
                String syncString = mMonoStereoModeSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getMonoStereoMode-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }

        Log.i(TAG, "StubFmService:rxGetMonoStereoMode(): -------- Exiting ");

        return getMonoStereoModeValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetMonoStereoMode_nb() {
        Log.i(TAG, "StubFmService:rxGetMonoStereoMode_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetMonoStereoMode_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getMonoStereoMode();
            Log.i(TAG, "mJFmRx.getMonoStereoMode returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getMonoStereoMode returned status "
                        + status.toString());
                return false;
            }


        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }

        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mSetMuteModeSyncQueue = new LinkedBlockingQueue<String>(
            5);


    public boolean rxSetMuteMode(int muteMode) {
        Log.i(TAG, "StubFmService:rxSetMuteMode  muteMode" + muteMode);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetMuteMode: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRx.JFmRxMuteMode lMode = JFmUtils.getEnumConst(
                    JFmRx.JFmRxMuteMode.class, muteMode);
            if (lMode == null) {
                Log.e(TAG, "StubFmService:rxSetMuteMode invalid  lMode " + lMode);
                return false;
            }
            JFmRxStatus status = mJFmRx.setMuteMode(lMode);
                Log.i(TAG, "mJFmRx.SetMuteMode returned status "
                        + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.SetMuteMode returned status "
                        + status.toString());
                return false;
            }

            /*
             * (When muting turn off audio paths to lower power consumption and
             * reduce noise)
             */
            if (lMode == JFmRx.JFmRxMuteMode.FMC_MUTE) {

                 /* L25 Specific */
            //mAudioManager.setParameters(AUDIO_RX_ENABLE + "=false");
            //Log.i(TAG, "MUTE- On");
                 /* L27 Specific */
                    enableRx(0);

            } else {
                 /* L25 Specific */
            //mAudioManager.setParameters(AUDIO_RX_ENABLE + "=true");
            //Log.i(TAG, "MUTE- OFF");
                 /* L27 Specific */
            enableRx(1);
            }


            /* implementation to make the set API Synchronous */

            try {
                String syncString = mSetMuteModeSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.setMuteMode-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        mCurrentMuteMode = muteMode;
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetMuteMode_nb(int muteMode) {
        JFmRx.JFmRxMuteMode lMode;
        Log.i(TAG, "StubFmService:rxSetMuteMode_nb  muteode" + muteMode);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetMuteMode_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
                if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

        lMode = JFmUtils.getEnumConst(
                JFmRx.JFmRxMuteMode.class, muteMode);
        if (lMode == null) {
            Log.e(TAG, "StubFmService:rxSetMuteMode_nb invalid  lBand " + lMode);
            return false;
        }
        JFmRxStatus status = mJFmRx.setMuteMode(lMode);
        Log.i(TAG, "mJFmRx.SetMuteMode returned status " + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.SetMuteMode returned status "
                    + status.toString());
            return false;
        }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        /*
        * (When muting turn off audio paths to lower power consumption and
        * reduce noise)
          * L25 Specific
        */
        if (lMode == JFmRx.JFmRxMuteMode.FMC_MUTE) {
            //mAudioManager.setParameters(AUDIO_RX_ENABLE + "=false");

        } else {
            //mAudioManager.setParameters(AUDIO_RX_ENABLE + "=true");
        }

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mMuteModeSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetMuteMode() {
        Log.i(TAG, "StubFmService:rxGetMuteMode  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetMuteMode: failed, fm not enabled  state " + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getMuteMode();
            Log.i(TAG, "mJFmRx.getMuteMode returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getMuteMode returned status "
                        + status.toString());
                return 0;
            }

            /* implementation to make the FM API Synchronous */

                try {
                String syncString = mMuteModeSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
                } catch (InterruptedException e) {
                    Log.e(TAG, "mJFmRx.getMuteMode-- Wait() s Exception!!!");
                }


        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetMuteMode(): -------- Exiting... ");
        return getMuteModeValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetMuteMode_nb() {
        Log.i(TAG, "StubFmService:rxGetMuteMode_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetMuteMode_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getMuteMode();
            Log.i(TAG, "mJFmRx.getMuteMode returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getMuteMode returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mSetRfDependentMuteModeSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public boolean rxSetRfDependentMuteMode(int rfMuteMode) {
        Log.i(TAG, "StubFmService:rxSetRfDependentMuteMode  " + rfMuteMode);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRfDependentMuteMode: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


        JFmRx.JFmRxRfDependentMuteMode lrfMute = JFmUtils.getEnumConst(
                JFmRx.JFmRxRfDependentMuteMode.class, rfMuteMode);
        if (lrfMute == null) {
            Log.e(TAG, "StubFmService:rxSetRfDependentMuteMode invalid  lrfMute "
                    + lrfMute);
            return false;
        }

        JFmRxStatus status = mJFmRx.setRfDependentMuteMode(lrfMute);
        Log.i(TAG, "mJFmRx.setRfDependentMuteMode returned status "
                + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.setRfDependentMuteMode returned status "
                    + status.toString());
            return false;
        }

        /* implementation to make the set API Synchronous */

        try {
            String syncString = mSetRfDependentMuteModeSyncQueue.take();
            if (!syncString.equals("*")) {
                Log.e(TAG, "wrong sync string reseived: " + syncString);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "mJFmRx.setRfDependentMuteMode-- Wait() s Exception!!!");
        }

} else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxSetRfDependentMuteMode exiting");
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetRfDependentMuteMode_nb(int rfMuteMode) {
        Log.i(TAG, "StubFmService:rxSetRfDependentMuteMode_nb  " + rfMuteMode);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG,
                    "rxSetRfDependentMuteMode_nb: failed, fm not enabled  state "
                            + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxRfDependentMuteMode lrfMute = JFmUtils.getEnumConst(
                JFmRx.JFmRxRfDependentMuteMode.class, rfMuteMode);
        if (lrfMute == null) {
            Log.e(TAG,
                    "StubFmService:rxSetRfDependentMuteMode_nb invalid  lrfMute "
                            + lrfMute);
            return false;
        }

        JFmRxStatus status = mJFmRx.setRfDependentMuteMode(lrfMute);
        Log.i(TAG, "mJFmRx.setRfDependentMuteMode returned status "
                + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.setRfDependentMuteMode returned status "
                    + status.toString());
            return false;
        }


    } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mRfDependentMuteModeSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetRfDependentMuteMode() {
        Log.i(TAG, "StubFmService:rxGetRfDependentMuteMode  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRfDependentMuteMode: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) ||(mIsTuneInProgress == false)) {
            JFmRxStatus status = mJFmRx.getRfDependentMute();
                Log.i(TAG, "mJFmRx.getRfDependentMuteMode returned status "
                        + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getRfDependentMuteMode returned status "
                        + status.toString());

                return 0;
            }
            /* implementation to make the FM API Synchronous */

            try {
                String syncString = mRfDependentMuteModeSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log
                        .e(TAG,
                                "mJFmRx.getRfDependentMuteMode-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG,
                "StubFmService:rxGetRfDependentMuteMode(): --------- Exiting... ");
        return getRfDependentMuteModeValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetRfDependentMuteMode_nb() {
        Log.i(TAG, "StubFmService:rxGetRfDependentMuteMode_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRfDependentMuteMode_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRfDependentMute();
            Log.i(TAG, "mJFmRx.getRfDependentMuteMode returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getRfDependentMuteMode returned status "
                        + status.toString());

                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }
        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mSetRssiThresholdSyncQueue = new LinkedBlockingQueue<String>(
            5);


    public boolean rxSetRssiThreshold(int threshhold) {
        Log.i(TAG, "StubFmService:rxSetRssiThreshold  " + threshhold);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRssiThreshold: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


            JFmRx.JFmRxRssi lrssiThreshhold = new JFmRx.JFmRxRssi(threshhold);
            if (lrssiThreshhold == null) {
            Log.e(TAG, "StubFmService:rxSetRssiThreshold invalid rssi "
                        + lrssiThreshhold);
                return false;
            }
        Log.d(TAG, "StubFmService:rxSetRssiThreshold  " + lrssiThreshhold);
        JFmRxStatus status = mJFmRx.setRssiThreshold(lrssiThreshhold);
        Log.i(TAG, "mJFmRx.setRssiThreshold returned status "
                + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.setRssiThreshold returned status "
                    + status.toString());
            return false;
        }

        /*implementation to make the set API Synchronous */

        try {
            String syncString = mSetRssiThresholdSyncQueue.take();
            if (!syncString.equals("*")) {
                Log.e(TAG, "wrong sync string reseived: " + syncString);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "mJFmRx.setRssiThreshold-- Wait() s Exception!!!");
        }

} else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxSetRssiThreshold exiting");
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetRssiThreshold_nb(int threshhold) {
        Log.i(TAG, "StubFmService:rxSetRssiThreshold_nb  " + threshhold);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRssiThreshold_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxRssi lrssiThreshhold = new JFmRx.JFmRxRssi(threshhold);
        if (lrssiThreshhold == null) {
            Log.e(TAG, "StubFmService:setRssiThreshold_nb invalid rssi "
                        + lrssiThreshhold);
            return false;
        }
        Log.d(TAG, "StubFmService:setRssiThreshold_nb  " + lrssiThreshhold);
            JFmRxStatus status = mJFmRx.setRssiThreshold(lrssiThreshhold);
            if (DBG)
                Log.i(TAG, "mJFmRx.setRssiThreshold returned status "
                        + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.setRssiThreshold returned status "
                        + status.toString());
                return false;
            }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mRssiThresholdSyncQueue = new LinkedBlockingQueue<String>(
            5);


    public synchronized int rxGetRssiThreshold() {

        Log.i(TAG, "StubFmService:rxGetRssiThreshold --Entry  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRssiThreshold: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRssiThreshold();
            Log.i(TAG, "mJFmRx.getRssiThreshold returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getRssiThreshold returned status "
                        + status.toString());
                return 0;
            }

            /*implementation to make the FM API Synchronous */

                try {
                String syncString = mRssiThresholdSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getRssiThreshold-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetRssiThreshold(): ---------- Exiting ");
        return getRssiThresholdValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetRssiThreshold_nb() {

        Log.i(TAG, "StubFmService:rxGetRssiThreshold_nb --Entry  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRssiThreshold_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRssiThreshold();
            Log.i(TAG, "mJFmRx.getRssiThreshold returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getRssiThreshold returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }

        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mSetDeEmphasisFilterSyncQueue = new LinkedBlockingQueue<String>(
            5);


    public boolean rxSetDeEmphasisFilter(int filter) {
        Log.i(TAG, "StubFmService:rxSetDeEmphasisFilter filter " + filter);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetDeEmphasisFilter: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

        JFmRx.JFmRxEmphasisFilter lFilter = JFmUtils.getEnumConst(
                JFmRx.JFmRxEmphasisFilter.class, filter);
        if (lFilter == null) {
            Log.e(TAG, "StubFmService:rxSetDeEmphasisFilter invalid  lBand "
                    + lFilter);
            return false;
        }
        JFmRxStatus status = mJFmRx.SetDeEmphasisFilter(lFilter);
        Log.i(TAG, "mJFmRx.setDeEmphasisFilter returned status "
                + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setDeEmphasisFilter returned status "
                    + status.toString());
            return false;
        }

        /*implementation to make the set API Synchronous */

        try {
            String syncString = mSetDeEmphasisFilterSyncQueue.take();
            if (!syncString.equals("*")) {
                Log.e(TAG, "wrong sync string reseived: " + syncString);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "mJFmRx.setDeEmphasisFilter-- Wait() s Exception!!!");
        }

    } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxSetDeEmphasisFilter exiting");
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetDeEmphasisFilter_nb(int filter) {
        Log.i(TAG, "StubFmService:rxSetDeEmphasisFilter_nb filter " + filter);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetDeEmphasisFilter_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxEmphasisFilter lFilter = JFmUtils.getEnumConst(
                JFmRx.JFmRxEmphasisFilter.class, filter);
        if (lFilter == null) {
            Log.e(TAG, "StubFmService:rxSetDeEmphasisFilter_nb invalid  lBand "
                    + lFilter);
            return false;
        }
        JFmRxStatus status = mJFmRx.SetDeEmphasisFilter(lFilter);
        Log.i(TAG, "mJFmRx.setDeEmphasisFilter returned status "
                + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setDeEmphasisFilter returned status "
                    + status.toString());
            return false;
        }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mDeEmphasisFilterSyncQueue = new LinkedBlockingQueue<String>(
            5);


    public synchronized int rxGetDeEmphasisFilter() {
        Log.i(TAG, "StubFmService:rxGetDeEmphasisFilter  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetDeEmphasisFilter: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.GetDeEmphasisFilter();
            Log.i(TAG, "mJFmRx.getDeEmphasisFilter returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getDeEmphasisFilter returned status "
                        + status.toString());
                return 0;
            }
            /* :implementation to make the FM API Synchronous */

                try {
                String syncString = mDeEmphasisFilterSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
                } catch (InterruptedException e) {
                    Log
                            .e(TAG,
                                    "mJFmRx.getDeEmphasisFilter-- Wait() s Exception!!!");
                }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetDeEmphasisFilter(): -------- Exiting ");
        return getDeEmphasisFilterValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetDeEmphasisFilter_nb() {
        Log.i(TAG, "StubFmService:rxGetDeEmphasisFilter_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetDeEmphasisFilter_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.GetDeEmphasisFilter();
            Log.i(TAG, "mJFmRx.getDeEmphasisFilter returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getDeEmphasisFilter returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }

        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetVolume(int volume) {
        Log.i(TAG, "StubFmService:rxSetVolume  " + volume);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetVolume: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        mVolState = VOL_REQ_STATE_PENDING;
        JFmRx.JFmRxVolume lVolume = new JFmRx.JFmRxVolume(volume);
        if (lVolume == null) {
            Log.e(TAG, "StubFmService:rxSetVolume invalid  lVolume " + lVolume);
            return false;
        }

        synchronized (mVolumeSynchronizationLock) {

            JFmRxStatus status = mJFmRx.setVolume(lVolume);
            Log.i(TAG, "mJFmRx.setVolume returned status " + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.setVolume returned status "
                        + status.toString());
                return false;
            }

//            mVolState = VOL_REQ_STATE_PENDING;
        }

        return true;
    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetChannelSpacing_nb(int channelSpace) {

        Log.i(TAG, "StubFmService:rxSetChannelSpacing_nb  " + channelSpace);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetChannelSpacing_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

        JFmRx.JFmRxChannelSpacing lChannelSpace = JFmUtils.getEnumConst(
                JFmRx.JFmRxChannelSpacing.class, channelSpace);

        if (lChannelSpace == null) {
            Log.e(TAG, "StubFmService:rxSetChannelSpacing_nb invalid  lChannelSpace "
                    + lChannelSpace);
            return false;
        }
        JFmRxStatus status = mJFmRx.setChannelSpacing(lChannelSpace);
        Log.i(TAG, "mJFmRx.setChannelSpacing returned status "
                + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setChannelSpacing returned status "
                    + status.toString());
            return false;
        }

        } else {
                    Log.e(TAG, "Seek is in progress.cannot call the API");
                    return false;
                }

        return true;

    }


    /*************************************************************************************************
         * Implementation of IFmRadio IPC interface
         *************************************************************************************************/
        /*  (separate queue for each set call synchronization) */
        private BlockingQueue<String> mSetChannelSpacingSyncQueue = new LinkedBlockingQueue<String>(
                5);


    public boolean rxSetChannelSpacing(int channelSpace) {

            Log.i(TAG, "StubFmService:rxSetChannelSpacing  " + channelSpace);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetChannelSpacing: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
            if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                    && (mIsCompleteScanInProgress == false)) {

        JFmRx.JFmRxChannelSpacing lChannelSpace = JFmUtils.getEnumConst(
                JFmRx.JFmRxChannelSpacing.class, channelSpace);

                if (lChannelSpace == null) {
                    Log.e(TAG,
                            "StubFmService:rxSetChannelSpacing invalid    lChannelSpace "
                    + lChannelSpace);
            return false;
        }
        JFmRxStatus status = mJFmRx.setChannelSpacing(lChannelSpace);
        Log.i(TAG, "mJFmRx.setChannelSpacing returned status "
                + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.setChannelSpacing returned status "
                    + status.toString());
            return false;
        }

                    /* implementation to make the FM API Synchronous */
            try {
                Log
                        .i(TAG,
                                    "StubFmService:rxSetChannelSpacing(): -------- Waiting... ");
                    String syncString = mSetChannelSpacingSyncQueue.take();
                    if (!syncString.equals("*")) {
                        Log.e(TAG, "wrong sync string receieved: " + syncString);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "mJFmRx.setChannelSpacing-- Wait() s Exception!!!");
                }

            } else {
                Log.e(TAG, "Seek is in progress.cannot call the API");
                return false;
            }
            Log.i(TAG, "StubFmService:rxSetChannelSpacing exiting");
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mVolumeSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetVolume() {
        Log.i(TAG, "StubFmService:rxGetVolume  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetVolume: failed, fm not enabled  state " + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

            if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                    && (mIsCompleteScanInProgress == false)) {


            JFmRxStatus status = mJFmRx.getVolume();
            Log.i(TAG, "mJFmRx.getVolume returned status " + status.toString());

            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getVolume returned status "
                        + status.toString());
                return 0;
            }

            /*implementation to make the FM API Synchronous */

                try {
                String syncString = mVolumeSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getVolume-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetVolume(): -------- Exiting... ");
        return getVolumeValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetVolume_nb() {
        Log.i(TAG, "StubFmService:rxGetVolume_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetVolume_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getVolume();
            Log.i(TAG, "mJFmRx.rxGetVolume returned status " + status.toString());

            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.rxGetVolume returned status "
                        + status.toString());
                return false;
            }
        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }

        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mChannelSpacingSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetChannelSpacing() {
        Log.i(TAG, "StubFmService:rxGetChannelSpacing  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetChannelSpacing: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

            if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                    && (mIsCompleteScanInProgress == false)) {


            JFmRxStatus status = mJFmRx.getChannelSpacing();
            Log.i(TAG, "mJFmRx.rxGetChannelSpacing returned status "
                    + status.toString());

            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.rxGetChannelSpacing returned status "
                        + status.toString());
                return 0;
            }

            /* implementation to make the FM API Synchronous */

                try {
                String syncString = mChannelSpacingSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getChannelSpacing-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetChannelSpacing() --Exiting ");
        return getChannelSpaceValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public  boolean rxGetChannelSpacing_nb() {
        Log.i(TAG, "StubFmService:rxGetChannelSpacing_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetChannelSpacing_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getChannelSpacing();
            Log.i(TAG, "mJFmRx.getChannelSpacing returned status "
                    + status.toString());

            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getChannelSpacing returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }
        return false;

    }

    static int BaseFreq() {
        return mCurrentBand == FM_BAND_JAPAN ? FM_FIRST_FREQ_JAPAN_KHZ
                : FM_FIRST_FREQ_US_EUROPE_KHZ;
    }

    static int LastFreq() {
        return mCurrentBand == FM_BAND_JAPAN ? FM_LAST_FREQ_JAPAN_KHZ
                : FM_LAST_FREQ_US_EUROPE_KHZ;
    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxTune_nb(int freq) {
        Log.i(TAG, "StubFmService: rxTune_nb  " + freq);

        mCurrentFrequency = freq;
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, " rxTune_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        if (freq < BaseFreq() || freq > LastFreq()) {
            Log.e(TAG, "StubFmService: rxTune_nb invalid frequency not in range "
                    + freq);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmRx.JFmRxFreq lFreq = new JFmRx.JFmRxFreq(freq);
        if (lFreq == null) {
            Log.e(TAG, "StubFmService:tune invalid frequency " + lFreq);
            return false;
        }
        mIsTuneInProgress = true;
        Log.d(TAG, "StubFmService: rxTune_nb  " + lFreq);
        JFmRxStatus status = mJFmRx.tune(lFreq);
        Log.i(TAG, "mJFmRx.tune returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.tune returned status " + status.toString());
            return false;
        }
//        mIsTuneInProgress = true;
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mTunedFrequencySyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetTunedFrequency() {
        Log.i(TAG, "StubFmService:rxGetTunedFrequency  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetTunedFrequency: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


            JFmRxStatus status = mJFmRx.getTunedFrequency();
            Log.i(TAG, "mJFmRx.getTunedFrequency returned status "
                    + status.toString());

            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getTunedFrequency returned status "
                        + status.toString());
                return 0;
            }
            /* implementation to make the FM API Synchronous */
                try {
                String syncString = mTunedFrequencySyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getTunedFrequency-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetTunedFrequency(): ------- Exiting ");
        return getTunedFrequencyValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetTunedFrequency_nb() {
        Log.i(TAG, "StubFmService:rxGetTunedFrequency_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetTunedFrequency_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


            JFmRxStatus status = mJFmRx.getTunedFrequency();
            Log.i(TAG, "mJFmRx.getTunedFrequency returned status "
                    + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getTunedFrequency returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }

        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSeek_nb(int direction) {
        Log.i(TAG, "StubFmService:rxSeek_nb  " + direction);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSeek_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmRx.JFmRxSeekDirection lDir = JFmUtils.getEnumConst(
                JFmRx.JFmRxSeekDirection.class, direction);
        if (lDir == null) {
            Log.e(TAG, "StubFmService:rxSeek_nb invalid  lDir " + lDir);
            return false;
        }
        mIsSeekInProgress = true;
        JFmRxStatus status = mJFmRx.seek(lDir);
        Log.i(TAG, "mJFmRx.seek returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.seek returned status " + status.toString());
            return false;
        }

//        mIsSeekInProgress = true;
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxStopSeek_nb() {
        Log.i(TAG, "StubFmService:rxStopSeek_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxStopSeek_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRxStatus status = mJFmRx.stopSeek();
        Log.i(TAG, "mJFmRx.stopSeek returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.stopSeek returned status " + status.toString());
            return false;
        }


} else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }


    /*************************************************************************************************
         * Implementation of IFmRadio IPC interface
         *************************************************************************************************/
        /*  (separate queue for each set call synchronization) */
        private BlockingQueue<String> mStopSeekSyncQueue = new LinkedBlockingQueue<String>(
                5);
    public boolean rxStopSeek() {
            Log.i(TAG, "StubFmService:rxStopSeek  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxStopSeek: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
            if ((mIsTuneInProgress == false)
                    && (mIsCompleteScanInProgress == false)) {
        JFmRxStatus status = mJFmRx.stopSeek();
        Log.i(TAG, "mJFmRx.stopSeek returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
                    Log.e(TAG, "mJFmRx.stopSeek returned status "
                            + status.toString());
                    return false;
                }

                try {
                    Log.i(TAG, "StubFmService:stopSeek(): -------- Waiting... ");
                    String syncString = mStopSeekSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string receieved: " + syncString);
                }
            } catch (InterruptedException e) {
                    Log.e(TAG, "mJFmRx.stopSeek-- Wait() s Exception!!!");
                }

            } else {
                Log.e(TAG, "tune is in progress.cannot call the API");
                return false;
            }
            return true;

        }


    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetRssi_nb() {
        Log.i(TAG, "StubFmService:rxGetRssi_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRssi_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRxStatus status = mJFmRx.getRssi();
        Log.i(TAG, "mJFmRx.getRssi returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.getRssi returned status " + status.toString());
            return false;
        }

        } else {
                    Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mRssiSyncQueue = new LinkedBlockingQueue<String>(
            5);
    public synchronized int rxGetRssi() {

        Log.i(TAG, "StubFmService:rxGetRssi  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRssi: failed, fm not enabled  state " + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRssi();
            Log.i(TAG, "mJFmRx.getRssi returned status " + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getRssi returned status "
                        + status.toString());
                return 0;
            }
            /* implementation to make the FM API Synchronous */
                try {
                String syncString = mRssiSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
                } catch (InterruptedException e) {
                    Log.e(TAG, "mJFmRx.getRssi-- Wait() s Exception!!!");
                }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetRssi(): ---------- Exiting ");
        return getRssiValue;
    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mRdsSystemSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetRdsSystem() {
        Log.i(TAG, "StubFmService:rxGetRdsSystem  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRdsSystem: failed, fm not enabled  state " + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRdsSystem();
            Log.i(TAG, "mJFmRx.getRdsSystem returned status "
                    + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getRdsSystem returned status "
                        + status.toString());
                return 0;
            }
            /* implementation to make the FM API Synchronous */


                try {
                String syncString = mRdsSystemSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
                } catch (InterruptedException e) {
                    Log.e(TAG, "mJFmRx.getRdsSystem-- Wait() s Exception!!!");
                }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetRdsSystem(): ----------- Exiting ");
        return getRdsSystemValue;

    }

        /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetRdsSystem_nb() {
        Log.i(TAG, "StubFmService:rxGetRdsSystem_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRdsSystem_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRdsSystem();
            Log.i(TAG, "mJFmRx.getRdsSystem returned status "
                    + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getRdsSystem returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }
        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetRdsSystem_nb(int system) {

        Log.i(TAG, "StubFmService:rxSetRdsSystem_nb  " + system);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRdsSystem_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
                if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxRdsSystem lSystem = JFmUtils.getEnumConst(
                JFmRx.JFmRxRdsSystem.class, system);
        if (lSystem == null) {
            Log.e(TAG, "StubFmService:rxSetRdsSystem_nb invalid  lSystem " + lSystem);
            return false;
        }
        Log.d(TAG, "StubFmService:setRdsSystem   lSystem " + lSystem);
        JFmRxStatus status = mJFmRx.setRdsSystem(lSystem);
        Log.i(TAG, "mJFmRx.setRdsSystem returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.setRdsSystem returned status "
                    + status.toString());
            return false;
        }


        } else {
                    Log.e(TAG, "Seek is in progress.cannot call the API");
                    return false;
                }
        return true;

    }


        /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /*  (separate queue for each set call synchronization) */
    private BlockingQueue<String> mSetRdsSystemSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public boolean rxSetRdsSystem(int system) {

        Log.i(TAG, "StubFmService:rxSetRdsSystem  " + system);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRdsSystem: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

        JFmRx.JFmRxRdsSystem lSystem = JFmUtils.getEnumConst(
                JFmRx.JFmRxRdsSystem.class, system);
        if (lSystem == null) {
                Log.e(TAG, "StubFmService:rxSetRdsSystem invalid  lSystem "
                        + lSystem);
            return false;
        }
            Log.d(TAG, "StubFmService:rxSetRdsSystem   lSystem " + lSystem);
        JFmRxStatus status = mJFmRx.setRdsSystem(lSystem);
            Log.i(TAG, "mJFmRx.setRdsSystem returned status "
                    + status.toString());
        if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.rxSetRdsSystem returned status "
                    + status.toString());
            return false;
        }
            /* (separate queue for each get call synchronization) */
            try {
                Log.i(TAG, "StubFmService:rxSetRdsSystem(): -------- Waiting... ");
                String syncString = mSetRdsSystemSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string received: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.setRdsSystem-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mEnableRdsSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public boolean rxEnableRds() {
        Log.i(TAG, "StubFmService:rxEnableRds  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxEnableRds: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


        JFmRxStatus status = mJFmRx.enableRDS();
        Log.i(TAG, "mJFmRx.enableRds returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.enableRds returned status " + status.toString());
            return false;
        }
        mCurrentRdsState = true;
        Log.e(TAG, "rxEnableRds mCurrentRdsState"+mCurrentRdsState);
        /* implementation to make the set API Synchronous */
        try {
            String syncString = mEnableRdsSyncQueue.take();
            if (!syncString.equals("*")) {
                Log.e(TAG, "wrong sync string reseived: " + syncString);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "mJFmRx.enableRds-- Wait() s Exception!!!");
        }
} else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxEnableRds exiting");
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxEnableRds_nb() {
        Log.i(TAG, "StubFmService:rxEnableRds_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxEnableRds_nb: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRxStatus status = mJFmRx.enableRDS();
        Log.i(TAG, "mJFmRx.enableRds returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.enableRds returned status " + status.toString());
            return false;
        }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        mCurrentRdsState = true;
        Log.e(TAG, "rxEnableRds_nb mCurrentRdsState"+mCurrentRdsState);
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mDisableRdsSyncQueue = new LinkedBlockingQueue<String>(
            5);


    public boolean rxDisableRds() {
        Log.i(TAG, "StubFmService:rxDisableRds  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxDisableRds: failed, fm not enabled  state " + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {


        JFmRxStatus status = mJFmRx.DisableRDS();
        Log.i(TAG, "mJFmRx.disableRds returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log
                    .e(TAG, "mJFmRx.disableRds returned status "
                            + status.toString());
            return false;
        }
        mCurrentRdsState = false;
        Log.e(TAG, "rxDisableRds mCurrentRdsState"+mCurrentRdsState);
        /* implementation to make the set API Synchronous */

        try {
            String syncString = mDisableRdsSyncQueue.take();
            if (!syncString.equals("*")) {
                Log.e(TAG, "wrong sync string reseived: " + syncString);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "mJFmRx.disableRds-- Wait() s Exception!!!");
        }
} else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxDisableRds exiting");
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxDisableRds_nb() {
        Log.i(TAG, "StubFmService:rxDisableRds_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log
                    .e(TAG, "disableRds_nb: failed, fm not enabled  state "
                            + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRxStatus status = mJFmRx.DisableRDS();
        Log.i(TAG, "mJFmRx.disableRds returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log
                    .e(TAG, "mJFmRx.disableRds returned status "
                            + status.toString());
            return false;
        }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        mCurrentRdsState = false;
        Log.e(TAG, "rxDisableRds_nb mCurrentRdsState"+mCurrentRdsState);
        return true;

    }

/*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* separate queue for each set call synchronization */
    private BlockingQueue<String> mSetRdsGroupMaskSyncQueue = new LinkedBlockingQueue<String>(
            5);
    public boolean rxSetRdsGroupMask(int mask) {
        Log.i(TAG, "StubFmService:rxSetRdsGroupMask  " + mask);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRdsGroupMask: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRx.JFmRxRdsGroupTypeMask lMask = JFmUtils.getEnumConst(
                    JFmRx.JFmRxRdsGroupTypeMask.class, (long) mask);
            JFmRxStatus status = mJFmRx.setRdsGroupMask(lMask);
            Log.i(TAG, "mJFmRx.setRdsGroupMask returned status "
                    + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.setRdsGroupMask returned status "
                        + status.toString());
                return false;
            }
            /* implementation to make the set API Synchronous */

            try {
                Log.i(TAG,
                        "StubFmService:rxSetRdsGroupMask(): -------- Waiting... ");
                String syncString = mSetRdsGroupMaskSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string received: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.setRdsGroupMask-- Wait() s Exception!!!");
            }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetRdsGroupMask_nb(int mask) {
        Log.i(TAG, "StubFmService:rxSetRdsGroupMask_nb  " + mask);
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, " rxSetRdsGroupMask_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxRdsGroupTypeMask lMask = JFmUtils.getEnumConst(
                JFmRx.JFmRxRdsGroupTypeMask.class, (long) mask);
        JFmRxStatus status = mJFmRx.setRdsGroupMask(lMask);
        Log.i(TAG, "mJFmRx.setRdsSystem returned status " + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.setRdsSystem returned status "
                    + status.toString());
            return false;
        }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mRdsGroupMaskSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized long rxGetRdsGroupMask() {
        Log.i(TAG, "StubFmService:rxGetRdsGroupMask  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRdsGroupMask: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRdsGroupMask();
            Log.i(TAG, "mJFmRx.getRdsGroupMask returned status "
                    + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getRdsGroupMask returned status "
                        + status.toString());
                return 0;
            }
            /* implementation to make the set API Synchronous */

                try {
                String syncString = mRdsGroupMaskSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getRdsGroupMask-- Wait() s Exception!!!");
            }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetRdsGroupMask(): ---------- Exiting ");
        return getRdsGroupMaskValue;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxGetRdsGroupMask_nb() {
        Log.i(TAG, "StubFmService:rxGetRdsGroupMask_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRdsGroupMask_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRdsGroupMask();
            Log.i(TAG, "mJFmRx.getRdsGroupMask returned status "
                    + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getRdsGroupMask returned status "
                        + status.toString());
                return false;
            }

        } else {
            Log.e(TAG, "Seek is in progress.Cannot call the API");
            return false;
        }

        return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxSetRdsAfSwitchMode_nb(int mode) {
        Log.i(TAG, "StubFmService:rxSetRdsAfSwitchMode_nb  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRdsAfSwitchMode_nb: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

                if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
        && (mIsCompleteScanInProgress == false)) {
        JFmRx.JFmRxRdsAfSwitchMode lMode = JFmUtils.getEnumConst(
                JFmRx.JFmRxRdsAfSwitchMode.class, mode);
        if (lMode == null) {
            Log
                    .e(TAG, "StubFmService:rxSetRdsAfSwitchMode_nb invalid  lMode "
                            + lMode);
            return false;
        }
        JFmRxStatus status = mJFmRx.setRdsAfSwitchMode(lMode);
        Log.i(TAG, "mJFmRx.setRdsAfSwitchMode returned status "
                + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.setRdsAfSwitchMode returned status "
                    + status.toString());

            return false;
        }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each set call synchronization) */
    private BlockingQueue<String> mSetRdsAfSwitchModeSyncQueue = new LinkedBlockingQueue<String>(
            5);
    public boolean rxSetRdsAfSwitchMode(int mode) {
        Log.i(TAG, "StubFmService:rxSetRdsAfSwitchMode  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxSetRdsAfSwitchMode: failed, fm not enabled  state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

        JFmRx.JFmRxRdsAfSwitchMode lMode = JFmUtils.getEnumConst(
                JFmRx.JFmRxRdsAfSwitchMode.class, mode);
        if (lMode == null) {
                Log.e(TAG, "StubFmService:rxSetRdsAfSwitchMode invalid  lMode "
                            + lMode);
            return false;
        }
        JFmRxStatus status = mJFmRx.setRdsAfSwitchMode(lMode);
        Log.i(TAG, "mJFmRx.setRdsAfSwitchMode returned status "
                + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.setRdsAfSwitchMode returned status "
                    + status.toString());

            return false;
        }

            try {
                Log
                        .i(TAG,
                                "StubFmService:rxSetRdsAfSwitchMode(): -------- Waiting... ");
                String syncString = mSetRdsAfSwitchModeSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string received: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.setRdsAfSwitchMode-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mRdsAfSwitchModeSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetRdsAfSwitchMode() {

        Log.i(TAG, "rxGetRdsAfSwitchMode  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxGetRdsAfSwitchMode: failed, fm not enabled  state "
                    + mRxState);
            return 0;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

            JFmRxStatus status = mJFmRx.getRdsAfSwitchMode();
            Log.i(TAG, "mJFmRx.getRdsAfSwitchMode returned status "
                    + status.toString());
            if (status != JFmRxStatus.PENDING) {
                Log.e(TAG, "mJFmRx.getRdsAfSwitchMode returned status "
                        + status.toString());

                return 0;
            }
            /* implementation to make the set API Synchronous */

                try {
                String syncString = mRdsAfSwitchModeSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getRdsAfSwitchMode-- Wait() s Exception!!!");
            }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetRdsAfSwitchMode(): ---------- Exiting... ");
        return getRdsAfSwitchModeValue;

    }

    /*************************************************************************************************
         * Implementation of IFmRadio IPC interface
         *************************************************************************************************/

        public boolean rxGetRdsAfSwitchMode_nb() {

            Log.i(TAG, "rxGetRdsAfSwitchMode_nb    ");
            if (mRxState != STATE_ENABLED) {
                Log.e(TAG, "rxGetRdsAfSwitchMode_nb: failed, fm not enabled    state "
                        + mRxState);
                return false;
            }
            mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                    "Need FMRX_ADMIN_PERM permission");
            if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                    && (mIsCompleteScanInProgress == false)) {

                JFmRxStatus status = mJFmRx.getRdsAfSwitchMode();
                Log.i(TAG, "mJFmRx.getRdsAfSwitchMode returned status "
                        + status.toString());
                if (status != JFmRxStatus.PENDING) {
                    Log.e(TAG, "mJFmRx.getRdsAfSwitchMode returned status "
                            + status.toString());

                    return false;
                }

            } else {
                Log.e(TAG, "Seek is in progress.Cannot call the API");
                return false;
            }

            return false;

        }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxChangeAudioTarget(int mask, int digitalConfig) {
        Log.i(TAG, "StubFmService:rxChangeAudioTarget  ");

        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxChangeAudioTarget: failed, already in state " + mRxState);
            return false;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmRx.JFmRxEcalSampleFrequency lconfig = JFmUtils.getEnumConst(
                JFmRx.JFmRxEcalSampleFrequency.class, digitalConfig);
        JFmRx.JFmRxAudioTargetMask lMask = JFmUtils.getEnumConst(
                JFmRx.JFmRxAudioTargetMask.class, mask);
        if (lMask == null || lconfig == null) {
            Log.e(TAG, "StubFmService:rxChangeAudioTarget invalid  lMask , lconfig"
                    + lMask + "" + lconfig);
            return false;
        }

        JFmRxStatus status = mJFmRx.changeAudioTarget(lMask, lconfig);
        Log.i(TAG, "mJFmRx.changeAudioTarget returned status "
                + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.changeAudioTarget returned status "
                    + status.toString());
            return false;
        }

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxChangeDigitalTargetConfiguration(int digitalConfig) {
        Log.i(TAG, "StubFmService:rxChangeDigitalTargetConfiguration  ");

        if (mRxState != STATE_ENABLED) {
            Log.e(TAG,
                    "rxChangeDigitalTargetConfiguration: failed, already in state "
                            + mRxState);
            return false;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmRx.JFmRxEcalSampleFrequency lconfig = JFmUtils.getEnumConst(
                JFmRx.JFmRxEcalSampleFrequency.class, digitalConfig);
        if (lconfig == null) {
            Log.e(TAG,
                    "StubFmService:rxChangeDigitalTargetConfiguration invalid   lconfig"
                            + lconfig);
            return false;
        }

        JFmRxStatus status = mJFmRx.changeDigitalTargetConfiguration(lconfig);
        Log.i(TAG, "mJFmRx.changeDigitalTargetConfiguration returned status "
                    + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG,
                    "mJFmRx.changeDigitalTargetConfiguration returned status "
                            + status.toString());

            return false;
        }

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxEnableAudioRouting() {
        Log.i(TAG, "StubFmService:rxEnableAudioRouting  ");
        if (mRxState != STATE_ENABLED) {
            Log
                    .e(TAG, "rxEnableAudioRouting: failed, already in state "
                            + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmRxStatus status = mJFmRx.enableAudioRouting();
        Log.i(TAG, "mJFmRx.enableAudioRouting returned status "
                + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.enableAudioRouting returned status "
                    + status.toString());

            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public boolean rxDisableAudioRouting() {
        Log.i(TAG, "StubFmService:rxDisableAudioRouting  ");
        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxDisableAudioRouting: failed, already in state "
                    + mRxState);
            return false;
        }
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmRxStatus status = mJFmRx.disableAudioRouting();
        if (DBG)
            Log.i(TAG, "mJFmRx.disableAudioRouting returned status "
                    + status.toString());
        if (status != JFmRxStatus.PENDING) {
            Log.e(TAG, "mJFmRx.disableAudioRouting returned status "
                    + status.toString());
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /*When Asynchronous Events like Call, Music PLayback,VideoPlayback is happning, FM will be put into pause state
    by disabling the audio.*/
    public boolean pauseFm() {
        if (rxIsEnabled() == true) {
            Log.i(TAG, "StubFmService:pauseFm  ");
            if (mRxState != STATE_ENABLED) {
                Log.e(TAG, "pauseFm: failed, already in state " + mRxState);
                return false;
            }
            mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                    "Need FMRX_ADMIN_PERM permission");

               /* L25 Specific */
             //mAudioManager.setParameters(AUDIO_RX_ENABLE+"=false");
             int off = 0;
               /* L27 Specific */
            enableRx(off);

            if (mWakeLock != null) {
                mWakeLock.acquire();
            }

        try {
            JFmRxStatus status = mJFmRx.disableAudioRouting();
                Log.i(TAG, "mJFmRx.disableAudioRouting returned status "
                        + status.toString());
            if (JFmRxStatus.PENDING != status) {
                    Log.e(TAG, "mJFmRx.disableAudioRouting returned status "
                        + status.toString());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "disableAudioRouting: Exception thrown during disableAudioRouting ("
                    + e.toString() + ")");
            return false;
        }

            mDelayedDisableHandler.postDelayed(mDelayedPauseDisable,
                    FM_DISABLE_DELAY);
            return true;
        } else
            return false;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /*This API will be called when teh user wants to resume FM after it has been paused.
    With this API you resume the FM playback by restoring the values before pause.*/
    public boolean resumeFm() {

        Log.i(TAG, "StubFmService:  resumeFm ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        mDelayedDisableHandler.removeCallbacks(mDelayedPauseDisable);
        mDelayedDisableHandler.removeCallbacks(mDelayedDisable);

        // Tell the music playback service to pause
        // TODO: these constants need to be published somewhere in the
        // framework.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

           if ((mWakeLock != null) && (mWakeLock.isHeld())) {
                mWakeLock.release();
             }
          /* L25 Specific */
        //mAudioManager.setParameters(AUDIO_RX_ENABLE+"=true");
        int On=1;
          /* L27 Specific */
        enableRx(On);

        mRxState = STATE_RESUME;
        mFmPauseResume =STATE_RESUME;

        Log.d(TAG, "Sending restore values intent");
        Intent restoreValuesIntent = new Intent(FM_RESTORE_VALUES);
        mContext.sendBroadcast(restoreValuesIntent);


        return true;

    }


    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    public boolean rxCompleteScan_nb() {
        Log.i(TAG, "StubFmService:rxCompleteScan_nb");

        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxCompleteScan_nb: failed, fm not enabled state " + mRxState);
            return false;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmRxStatus status = mJFmRx.completeScan();
        Log.i(TAG, "mJFmRx.completeScan returned status " + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.completeScan returned status "
                    + status.toString());
            return false;
        }

        mIsCompleteScanInProgress = true;

        return true;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    public boolean rxStopCompleteScan_nb() {
        Log.i(TAG, "StubFmService:rxStopCompleteScan_nb");

        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxStopCompleteScan_nb: failed, fm not enabled state "
                    + mRxState);
            return false;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)) {
            JFmRxStatus status = mJFmRx.stopCompleteScan();
            Log.i(TAG, "mJFmRx.stopCompleteScan returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.stopCompleteScan returned status "
                        + status.toString());
                return false;
            }

            return true;
        } else {
            Log.e(TAG, "Seek/tune is in progress.cannot call the API");
            return false;
        }

    }


    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* separate queue for each set call synchronization */
    private BlockingQueue<String> mStopCompleteScanSyncQueue = new LinkedBlockingQueue<String>(
            5);
    public int rxStopCompleteScan() {
        Log.i(TAG, "StubFmService:rxStopCompleteScan");

        if (mRxState != STATE_ENABLED) {
            Log.e(TAG, "rxStopCompleteScan: failed, fm not enabled state "
                    + mRxState);
            return 0;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)) {
            JFmRxStatus status = mJFmRx.stopCompleteScan();
            Log.i(TAG, "mJFmRx.stopCompleteScan returned status "
                    + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.stopCompleteScan returned status "
                        + status.toString());
                if (JFmRxStatus.COMPLETE_SCAN_IS_NOT_IN_PROGRESS == status) {
                    return status.getValue();
                } else
                    return 0;
            }

            /* implementation to make the set API Synchronous */

            try {
                Log.i(TAG,
                        "StubFmService:rxStopCompleteScan(): -------- Waiting... ");
                String syncString = mStopCompleteScanSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string received: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.stopCompleteScan-- Wait() s Exception!!!");
            }

        } else {
            Log.e(TAG, "Seek/tune is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }

        Log.i(TAG, "StubFmService:stopCompleteScan(): ---------- Exiting... ");
        return mStopCompleteScanStatus;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mValidChannelSyncQueue = new LinkedBlockingQueue<String>(
            5);
    public synchronized boolean rxIsValidChannel() {
        Log.i(TAG, "StubFmService:rxIsValidChannel");

        if (mRxState != STATE_ENABLED) {
            Log
                    .e(TAG, "rxIsValidChannel: failed, fm not enabled state "
                            + mRxState);
            return false;
        }

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

        JFmRxStatus status = mJFmRx.isValidChannel();
            Log.i(TAG, "mJFmRx.isValidChannel returned status "
                        + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.isValidChannel returned status "
                    + status.toString());
            return false;
        }

        /* implementation to make the FM API Synchronous */

            try {
                String syncString = mValidChannelSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.isValidChannel-- Wait() s Exception!!!");
            }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return false;
        }
        Log.i(TAG, "StubFmService:rxIsValidChannel(): ---------- Exiting... ");
        return mIsValidChannel;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mFwVersionSyncQueue = new LinkedBlockingQueue<String>(
            5);
    public synchronized double rxGetFwVersion() {
        Log.i(TAG, "StubFmService:rxGetFwVersion");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)
                && (mIsCompleteScanInProgress == false)) {

        JFmRxStatus status = mJFmRx.getFwVersion();
            Log.i(TAG, "mJFmRx.getFwVersion returned status "
                    + status.toString());
        if (JFmRxStatus.PENDING != status) {
            Log.e(TAG, "mJFmRx.getFwVersion returned status "
                    + status.toString());
            return 0;
        }

                /* implementation to make the FM API Synchronous */

            try {
                String syncString = mFwVersionSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "mJFmRx.getFwVersion-- Wait() s Exception!!!");
            }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        Log.i(TAG, "StubFmService:rxGetFwVersion(): ---------- Exiting... ");
        return getFwVersion;

    }

    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/
    /* (separate queue for each get call synchronization) */
    private BlockingQueue<String> mCompleteScanProgressSyncQueue = new LinkedBlockingQueue<String>(
            5);

    public synchronized int rxGetCompleteScanProgress() {
        Log.i(TAG, "StubFmService:rxGetCompleteScanProgress");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)) {
            JFmRxStatus status = mJFmRx.getCompleteScanProgress();
            if (DBG)
                Log.i(TAG, "mJFmRx.getCompleteScanProgress returned status "
                        + status.toString());
            if (JFmRxStatus.PENDING != status) {
                Log.e(TAG, "mJFmRx.getCompleteScanProgress returned status "
                        + status.toString());
                if (JFmRxStatus.COMPLETE_SCAN_IS_NOT_IN_PROGRESS == status) {
                    return status.getValue();
                } else
                return 0;
            }
            /* implementation to make the FM API Synchronous */

            try {
                String syncString = mCompleteScanProgressSyncQueue.take();
                if (!syncString.equals("*")) {
                    Log.e(TAG, "wrong sync string reseived: " + syncString);
                }
            } catch (InterruptedException e) {
                Log
                        .e(TAG,
                                "mJFmRx.getCompleteScanProgress-- Wait() s Exception!!!");
            }
        } else {
            Log.e(TAG, "Seek is in progress.cannot call the API");
            return FM_SEEK_IN_PROGRESS;
        }
        if (DBG)
            Log
                .i(TAG,
                        "StubFmService:rxGetCompleteScanProgress(): ---------- Exiting... ");
        return getScanProgress;

    }


    /*************************************************************************************************
         * Implementation of IFmRadio IPC interface
         *************************************************************************************************/

        public  boolean rxGetCompleteScanProgress_nb() {
            Log.i(TAG, "StubFmService:rxGetCompleteScanProgress_nb");

            mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                    "Need FMRX_ADMIN_PERM permission");

            if ((mIsSeekInProgress == false) && (mIsTuneInProgress == false)) {
                JFmRxStatus status = mJFmRx.getCompleteScanProgress();
                Log.i(TAG, "mJFmRx.rxGetCompleteScanProgress_nb returned status "
                        + status.toString());
                if (JFmRxStatus.PENDING != status) {
                    Log.e(TAG, "mJFmRx.getCompleteScanProgress returned status "
                            + status.toString());
                        return false;
                }

            } else {
                Log.e(TAG, "Seek is in progress.cannot call the API");
                return false;
            }
            Log
                    .i(TAG,
                            "StubFmService:rxGetCompleteScanProgress(): ---------- Exiting... ");
            return true;

        }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txEnable() {
        /* If Rx is NOT ENABLED then only Enable TX*/
                    if(rxIsEnabled()==true){
                 Log.e(TAG, "Fm TX Enable: FM RX is enabled could not Enable fm TX");
                     return false;
                        }

        Log.i(TAG, "StubFmService:  txEnable ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmTxStatus status;

            /* Creating the Fm Tx Context*/

            // try communicating for 5 seconds (by trying to create a valid FmRX context)
           boolean fmTxCreated = false;
          for (int count = 0; count< 10; count++) {
             Log.i(TAG, "FmTxEnable: FmRx create try #" + count);
          Log.i(TAG, "FmTxEnable: mJFmTx is:"+ mJFmTx);
             status = mJFmTx.txCreate(this);
             Log.i(TAG, "FmTxEnable: FmRx create returned " + status.toString());
             if (status == JFmTxStatus.SUCCESS) {
                fmTxCreated = true;
                break;
                 }
                     SystemClock.sleep(500);
                  }

          if (fmTxCreated == false) {
             Log.e(TAG, "fmTxCreated: FmRx create failed. Aborting");
             return false;
                  }


/* Enabling  the Fm Tx module */
        try {
    // TODO: these constants need to be published somewhere in the framework.
            status = mJFmTx.txEnable();
            Log.i(TAG, "mJFmTx.txEnable returned status " + status.toString());

            /* If the Operation Fail, Send false to the user and reset the MCP Monitor */
            if (status != JFmTxStatus.PENDING && status != JFmTxStatus.SUCCESS) {
            Log.e(TAG, "mJFmTx.txEnable returned status "    + status.toString());
            return false;
            }
        } catch (Exception e) {
        Log.e(TAG, "enable: Exception thrown during enable ("+ e.toString() + ")");
        return false;
        }

        if (status == JFmTxStatus.SUCCESS)
        {
    Intent intentTxEnable = new Intent(FmRadioIntent.FM_TX_ENABLED_ACTION);
    intentTxEnable.putExtra(FmRadioIntent.STATUS, status);
    mContext.sendBroadcast(intentTxEnable, FMRX_PERM);

        }else {
    // set a delayed timeout message
    }

        return true;
    }



   public boolean IsFmTxEnabled() {
        Log.i(TAG, "StubFmService:  IsFmTxEnabled ");
        mContext.enforceCallingOrSelfPermission(FMRX_PERM,
                "Need FMRX_PERM permission");
        return (mTxState == STATE_ENABLED);

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/
    public boolean txDisable() {
        JFmTxStatus status;
        Log.i(TAG, "StubFmService:  disableTx ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        try {
            status = mJFmTx.txDisable();
        Log.i(TAG, "mJFmTx.txDisable returned status " + status.toString());
           if (status != JFmTxStatus.PENDING && status != JFmTxStatus.SUCCESS){
            destroyJFmTx(); /* destroy all connection with deamon on radioOff */
            return false;
        }
        } catch (Exception e) {
        Log.e(TAG, "txDisable: Exception thrown during disable ("+ e.toString() + ")");
        return false;
        }

        mTxState = STATE_DEFAULT;

        if (status == JFmTxStatus.SUCCESS)
            {
                //TODO - Need to see what to send here if the operation is synchronic

            }
        else {
            // set a delayed timeout message
        }

        return true;
    }


    /* Destroy Tx Context */
    private void destroyJFmTx() {
        JFmTxStatus status;
        Log.i(TAG, "StubFmService:  destroyJFmTx ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,"Need FMRX_ADMIN_PERM permission");

        try {
        if (mJFmTx != null) {
                 status = mJFmTx.txDestroy();
                 Log.i(TAG, "mJFmTx.destroyJFmTx returned status " + status.toString());
            }
        } catch (Exception e) {
        Log.e(TAG, "destroyJFmTx: Exception thrown during destroy ("+ e.toString() + ")");
        }

        mTxState = STATE_DEFAULT;

        }



    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/
    public boolean txStartTransmission() {
        Log.i(TAG, "StubFmService:  txStartTransmission ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        try {

            JFmTxStatus status = mJFmTx.txStartTransmission();
            Log.i(TAG, "mJFmTx.txStartTransmission returned status " + status.toString());
            if (JFmTxStatus.PENDING != status) {
                Log.e(TAG, "mJFmTx.txStartTransmission returned status "
                        + status.toString());
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "txStartTransmission: Exception thrown during disable ("
                    + e.toString() + ")");
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/
    public boolean txStopTransmission() {
        Log.i(TAG, "StubFmService:  txStopTransmission ");
        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        try {

            JFmTxStatus status = mJFmTx.txStopTransmission();
            Log.i(TAG, "mJFmTx.txStopTransmission returned status " + status.toString());
            if (JFmTxStatus.PENDING != status) {
                Log.e(TAG, "mJFmTx.txStopTransmission returned status "
                        + status.toString());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "txStopTransmission: Exception thrown during disable ("
                    + e.toString() + ")");
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txTune(long freq) {
        Log.i(TAG, "StubFmService:txTune  " + freq);

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmTx.JFmTxFreq lFreq = new JFmTx.JFmTxFreq(freq);
        if (lFreq == null) {
            Log.e(TAG, "StubFmService:txTune invalid frequency " + lFreq);
            return false;
        }
        Log.d(TAG, "StubFmService:txTune  " + lFreq);
        JFmTxStatus status = mJFmTx.txTune(lFreq);
        Log.i(TAG, "mJFmRx.tune returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txTune returned status " + status.toString());
            return false;
        }
        return true;

    }



    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txGetTunedFrequency( ) {
        Log.i(TAG, "StubFmService:txGetTunedFrequency  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetTunedFrequency();

        Log.i(TAG, "mJFmTx.txGetTunedFrequency returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetTunedFrequency returned status " + status.toString());
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txSetPowerLevel(int  powerLevel) {
        Log.i(TAG, "StubFmService:txSetPowerLevel  " + powerLevel);

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");
        JFmTx.JFmTxPowerLevel lPowerLevel = new JFmTx.JFmTxPowerLevel(powerLevel);
        if (lPowerLevel == null) {
            Log.e(TAG, "StubFmService:txSetPowerLevel invalid PowerLevel " + lPowerLevel);
            return false;
        }
        Log.d(TAG, "StubFmService:txSetPowerLevel  " + lPowerLevel);
        JFmTxStatus status = mJFmTx.txSetPowerLevel(lPowerLevel);
        Log.i(TAG, "mJFmTx.txSetPowerLevel returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetPowerLevel returned status " + status.toString());
            return false;
        }
        return true;

    }



    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txGetPowerLevel() {
        Log.i(TAG, "StubFmService:txGetPowerLevel  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetPowerLevel();
        Log.i(TAG, "mJFmTx.txGetPowerLevel returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetPowerLevel returned status " + status.toString());
            return false;
        }
        return true;

    }

    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txEnableRds() {
        Log.i(TAG, "StubFmService:txEnableRds  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txEnableRds();

        Log.i(TAG, "mJFmTx.txEnableRds returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txEnableRds returned status " + status.toString());
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txDisableRds() {
        Log.i(TAG, "StubFmService:txDisableRds  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txDisableRds();

        Log.i(TAG, "mJFmTx.txDisableRds returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txDisableRds returned status " + status.toString());
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txSetRdsTransmissionMode(int mode) {
        Log.i(TAG, "StubFmService:txSetRdsTransmissionMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTx.JFmTxRdsTransmissionMode lMode = JFmUtils.getEnumConst(JFmTx.JFmTxRdsTransmissionMode.class,
                mode);
        if (lMode == null) {
            Log.e(TAG, "StubFmService:txSetRdsTransmissionMode invalid  lMode " + lMode);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetRdsTransmissionMode(lMode);

        Log.i(TAG, "mJFmTx.txSetRdsTransmissionMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsTransmissionMode returned status " + status.toString());
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txGetRdsTransmissionMode( ) {
        Log.i(TAG, "StubFmService:txGetRdsTransmissionMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsTransmissionMode();

        Log.i(TAG, "mJFmTx.txGetRdsTransmissionMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsTransmissionMode returned status " + status.toString());
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txSetRdsTextPsMsg(String  psStr) {
        Log.i(TAG, "StubFmService:txSetRdsTextPsMsg --> psString = "+psStr);

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        int strLength = psStr.length();
        //byte[] psString = psStr.getBytes();// converting String to Byte Array

        JFmTxStatus status = mJFmTx.txSetRdsTextPsMsg(psStr,strLength);

        Log.i(TAG, "mJFmTx.txSetRdsTextPsMsg returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsTextPsMsg returned status " + status.toString());
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txGetRdsTextPsMsg( ) {
        Log.i(TAG, "StubFmService:txGetRdsTextPsMsg --> psString = ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsTextPsMsg();

        Log.i(TAG, "mJFmTx.txGetRdsTextPsMsg returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsTextPsMsg returned status " + status.toString());
            return false;
        }
        return true;

    }



    /*************************************************************************************************
     * Implementation of IFMReciever IPC interface
     *************************************************************************************************/

    public boolean txWriteRdsRawData(String msg) {
        Log.i(TAG, "StubFmService:txWriteRdsRawData  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        int strLength = msg.length();
        //byte[] rawData = msg.getBytes();// converting String to Byte Array

        JFmTxStatus status = mJFmTx.txWriteRdsRawData(msg,strLength);

        Log.i(TAG, "mJFmTx.txWriteRdsRawData returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txWriteRdsRawData returned status " + status.toString());
            return false;
        }
        return true;

    }

    public boolean  txChangeDigitalSourceConfiguration(int ecalSampleFreq){
        Log.i(TAG, "StubFmService:txChangeDigitalSourceConfiguration  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txChangeDigitalSourceConfiguration(ecalSampleFreq);

        Log.i(TAG, "mJFmTx.txChangeDigitalSourceConfiguration returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txChangeDigitalSourceConfiguration returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txChangeAudioSource(int audioSrc,int ecalSampleFreq){
        Log.i(TAG, "StubFmService:txChangeAudioSource  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTx.JFmTxEcalResource lAudioSrc = JFmUtils.getEnumConst(
                JFmTx.JFmTxEcalResource.class, audioSrc);
        if (lAudioSrc == null) {
            Log.e(TAG, "StubFmService:txChangeAudioSource invalid  lAudioSrc " + lAudioSrc);
            return false;
        }

        JFmTx.JFmTxEcalSampleFrequency lEcalSampleFreq = JFmUtils.getEnumConst(
                JFmTx.JFmTxEcalSampleFrequency.class, ecalSampleFreq);
        if (lAudioSrc == null) {
            Log.e(TAG, "StubFmService:txChangeAudioSource invalid  lEcalSampleFreq " + lEcalSampleFreq);
            return false;
        }


        JFmTxStatus status = mJFmTx.txChangeAudioSource(lAudioSrc,lEcalSampleFreq);

        Log.i(TAG, "mJFmTx.txChangeAudioSource returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txChangeAudioSource returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txSetRdsECC(int ecc){
        Log.i(TAG, "StubFmService:txSetRdsECC  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txSetRdsECC(ecc);

        Log.i(TAG, "mJFmTx.txSetRdsECC returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsECC returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txGetRdsECC( ){
        Log.i(TAG, "StubFmService:txGetRdsECC  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txGetRdsECC();

        Log.i(TAG, "mJFmTx.txGetRdsECC returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsECC returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txSetMonoStereoMode(int mode){
        Log.i(TAG, "StubFmService:txSetMonoStereoMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTx.JFmTxMonoStereoMode lMode = JFmUtils.getEnumConst(
                JFmTx.JFmTxMonoStereoMode.class, mode);
        if (lMode == null) {
            Log.e(TAG, "StubFmService:txSetPreEmphasisFilter invalid  lMode " + lMode);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetMonoStereoMode(lMode);

        Log.i(TAG, "mJFmTx.txSetMonoStereoMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetMonoStereoMode returned status " + status.toString());
            return false;
        }
        return true;

    }


       public boolean txGetMonoStereoMode(){
        Log.i(TAG, "StubFmService:txGetMonoStereoMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txGetMonoStereoMode();

        Log.i(TAG, "mJFmTx.txGetMonoStereoMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetMonoStereoMode returned status " + status.toString());
            return false;
        }
        return true;

    }


       public boolean txSetPreEmphasisFilter(int preEmpFilter) {
        Log.i(TAG, "StubFmService:txSetPreEmphasisFilter  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTx.JFmTxEmphasisFilter lPreEmpFilter = JFmUtils.getEnumConst(
                JFmTx.JFmTxEmphasisFilter.class, preEmpFilter);
        if (lPreEmpFilter == null) {
            Log.e(TAG, "StubFmService:txSetPreEmphasisFilter invalid  lPreEmpFilter " + lPreEmpFilter);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetPreEmphasisFilter(lPreEmpFilter);

        Log.i(TAG, "mJFmTx.txSetPreEmphasisFilter returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetPreEmphasisFilter returned status " + status.toString());
            return false;
        }
        return true;

    }


       public boolean txGetPreEmphasisFilter( ) {
        Log.i(TAG, "StubFmService:txGetPreEmphasisFilter  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txGetPreEmphasisFilter();

        Log.i(TAG, "mJFmTx.txGetPreEmphasisFilter returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetPreEmphasisFilter returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txSetMuteMode(int muteMode){
        Log.i(TAG, "StubFmService:txSetMuteMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTx.JFmTxMuteMode lMuteMode = JFmUtils.getEnumConst(
                JFmTx.JFmTxMuteMode.class, muteMode);
        if (lMuteMode == null) {
            Log.e(TAG, "StubFmService:txSetMuteMode invalid  lMuteMode " + lMuteMode);
            return false;
        }
        JFmTxStatus status = mJFmTx.txSetMuteMode(lMuteMode);

        Log.i(TAG, "mJFmTx.txSetMuteMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetMuteMode returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txGetMuteMode( ){
        Log.i(TAG, "StubFmService:txGetMuteMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetMuteMode();

        Log.i(TAG, "mJFmTx.txGetMuteMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetMuteMode returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txSetRdsAfCode(int afCode) {
        Log.i(TAG, "StubFmService:txSetRdsAfCode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txSetRdsAfCode(afCode);

        Log.i(TAG, "mJFmTx.txSetRdsAfCode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsAfCode returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txGetRdsAfCode( ) {
        Log.i(TAG, "StubFmService:txGetRdsAfCode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txGetRdsAfCode();

        Log.i(TAG, "mJFmTx.txGetRdsAfCode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsAfCode returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txSetRdsPiCode(int piCode){
        Log.i(TAG, "StubFmService:txSetRdsPiCode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTx.JFmTxRdsPiCode lPiCode = new JFmTx.JFmTxRdsPiCode(piCode);
        if (lPiCode == null) {
            Log.e(TAG, "StubFmService:txSetRdsPiCode invalid  lPiCode " + lPiCode);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetRdsPiCode(lPiCode);

        Log.i(TAG, "mJFmTx.txSetRdsPiCode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsPiCode returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txGetRdsPiCode( ){
        Log.i(TAG, "StubFmService:txGetRdsPiCode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsPiCode();

        Log.i(TAG, "mJFmTx.txGetRdsPiCode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsPiCode returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txSetRdsPtyCode(int ptyCode) {
        Log.i(TAG, "StubFmService:txSetRdsPtyCode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTx.JFmTxRdsPtyCode lPtyCode = new JFmTx.JFmTxRdsPtyCode(ptyCode);
        if (lPtyCode == null) {
            Log.e(TAG, "StubFmService:txSetRdsPtyCode invalid ptyCode " + lPtyCode);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetRdsPtyCode(lPtyCode);

        Log.i(TAG, "mJFmTx.txSetRdsPtyCode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsPtyCode returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txGetRdsPtyCode( ) {
        Log.i(TAG, "StubFmService:txGetRdsPtyCode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsPtyCode();

        Log.i(TAG, "mJFmTx.txGetRdsPtyCode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsPtyCode returned status " + status.toString());
            return false;
        }
        return true;

    }




    public boolean  txSetRdsTextRepertoire(int repertoire){
        Log.i(TAG, "StubFmService:txSetRdsTextRepertoire  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTx.JFmTxRepertoire lRepertoire = JFmUtils.getEnumConst(
                JFmTx.JFmTxRepertoire.class, repertoire);
        if (lRepertoire == null) {
            Log.e(TAG, "StubFmService:txSetRdsTextRepertoire invalid  lRepertoire " + lRepertoire);
            return false;
        }


        JFmTxStatus status = mJFmTx.txSetRdsTextRepertoire(lRepertoire);

        Log.i(TAG, "mJFmTx.txSetRdsTextRepertoire returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsTextRepertoire returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txGetRdsTextRepertoire( ){
        Log.i(TAG, "StubFmService:txGetRdsTextRepertoire  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsTextRepertoire();

        Log.i(TAG, "mJFmTx.txGetRdsTextRepertoire returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsTextRepertoire returned status " + status.toString());
            return false;
        }
        return true;

    }




    public boolean  txSetRdsPsDisplayMode(int dispalyMode) {
        Log.i(TAG, "StubFmService:txSetRdsPsDisplayMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTx.JFmRdsPsDisplayMode lPsDisplayMode = JFmUtils.getEnumConst(
                JFmTx.JFmRdsPsDisplayMode.class, dispalyMode);
        if (lPsDisplayMode == null) {
            Log.e(TAG, "StubFmService:txSetRdsPsDisplayMode invalid  lPsDisplayMode " + lPsDisplayMode);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetRdsPsDisplayMode(lPsDisplayMode);

        Log.i(TAG, "mJFmTx.txSetRdsPsDisplayMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsPsDisplayMode returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txGetRdsPsDisplayMode( ) {
        Log.i(TAG, "StubFmService:txGetRdsPsDisplayMode  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsPsDisplayMode();

        Log.i(TAG, "mJFmTx.txGetRdsPsDisplayMode returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsPsDisplayMode returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txSetRdsPsScrollSpeed(int scrollSpeed) {
        Log.i(TAG, "StubFmService:txSetRdsPsScrollSpeed  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txSetRdsPsScrollSpeed(scrollSpeed);

        Log.i(TAG, "mJFmTx.txSetRdsPsScrollSpeed returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsPsScrollSpeed returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txGetRdsPsScrollSpeed( ) {
        Log.i(TAG, "StubFmService:txGetRdsPsScrollSpeed  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txGetRdsPsScrollSpeed();

        Log.i(TAG, "mJFmTx.txGetRdsPsScrollSpeed returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsPsScrollSpeed returned status " + status.toString());
            return false;
        }
        return true;

    }

    public boolean  txSetRdsTextRtMsg(int msgType, String msg, int msgLength){
        Log.i(TAG, "StubFmService:txSetRdsTextRtMsg  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTx.JFmRdsRtMsgType lMsgType = JFmUtils.getEnumConst(
                JFmTx.JFmRdsRtMsgType.class, msgType);
        if (lMsgType == null) {
            Log.e(TAG, "StubFmService:txSetRdsTextRtMsg invalid  lMsgType " + lMsgType);
            return false;
        }
        JFmTxStatus status = mJFmTx.txSetRdsTextRtMsg(lMsgType,msg,msgLength);

        Log.i(TAG, "mJFmTx.txSetRdsTextRtMsg returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsTextRtMsg returned status " + status.toString());
            return false;
        }
        return true;

    }


    public boolean  txGetRdsTextRtMsg(){
        Log.i(TAG, "StubFmService:txGetRdsTextRtMsg  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsTextRtMsg();

        Log.i(TAG, "mJFmTx.txGetRdsTextRtMsg returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsTextRtMsg returned status " + status.toString());
            return false;
        }
        return true;

    }




    public boolean  txSetRdsTransmittedGroupsMask(long rdsTrasmittedGrpMask){
        Log.i(TAG, "StubFmService:txSetRdsTransmittedGroupsMask  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txSetRdsTransmittedGroupsMask(rdsTrasmittedGrpMask);

        Log.i(TAG, "mJFmTx.txSetRdsTransmittedGroupsMask returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsTransmittedGroupsMask returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txGetRdsTransmittedGroupsMask(){
        Log.i(TAG, "StubFmService:txGetRdsTransmittedGroupsMask  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txGetRdsTransmittedGroupsMask();

        Log.i(TAG, "mJFmTx.txGetRdsTransmittedGroupsMask returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsTransmittedGroupsMask returned status " + status.toString());
            return false;
        }
        return true;

    }




    public boolean  txSetRdsTrafficCodes(int taCode, int tpCode){
        Log.i(TAG, "StubFmService:txSetRdsTrafficCodes  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTx.JFmTaCode lTaCode = JFmUtils.getEnumConst(
                JFmTx.JFmTaCode.class, taCode);
        if (lTaCode == null) {
            Log.e(TAG, "StubFmService:txChangeAudioSource invalid  lTaCode " + lTaCode);
            return false;
        }

        JFmTx.JFmTpCode lTpCode = JFmUtils.getEnumConst(
                JFmTx.JFmTpCode.class, tpCode);
        if (lTaCode == null) {
            Log.e(TAG, "StubFmService:txChangeAudioSource invalid  lTpCode " + lTpCode);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetRdsTrafficCodes(lTaCode,lTpCode);

        Log.i(TAG, "mJFmTx.txSetRdsTrafficCodes returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsTrafficCodes returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txGetRdsTrafficCodes(){
        Log.i(TAG, "StubFmService:txGetRdsTrafficCodes  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");


        JFmTxStatus status = mJFmTx.txGetRdsTrafficCodes();

        Log.i(TAG, "mJFmTx.txGetRdsTrafficCodes returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsTrafficCodes returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txSetRdsMusicSpeechFlag(int musicSpeechFlag)  {
        Log.i(TAG, "StubFmService:txSetRdsMusicSpeechFlag  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTx.JFmMusicSpeechFlag lMusicSpeechFlag = JFmUtils.getEnumConst(
                JFmTx.JFmMusicSpeechFlag.class, musicSpeechFlag);
        if (lMusicSpeechFlag == null) {
            Log.e(TAG, "StubFmService:txChangeAudioSource invalid  lMusicSpeechFlag " + lMusicSpeechFlag);
            return false;
        }

        JFmTxStatus status = mJFmTx.txSetRdsMusicSpeechFlag(lMusicSpeechFlag);

        Log.i(TAG, "mJFmTx.txSetRdsMusicSpeechFlag returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txSetRdsMusicSpeechFlag returned status " + status.toString());
            return false;
        }
        return true;

    }



    public boolean  txGetRdsMusicSpeechFlag( )  {
        Log.i(TAG, "StubFmService:txGetRdsMusicSpeechFlag  ");

        mContext.enforceCallingOrSelfPermission(FMRX_ADMIN_PERM,
                "Need FMRX_ADMIN_PERM permission");

        JFmTxStatus status = mJFmTx.txGetRdsMusicSpeechFlag();

        Log.i(TAG, "mJFmTx.txGetRdsMusicSpeechFlag returned status " + status.toString());
        if (status != JFmTxStatus.PENDING) {
            Log.e(TAG, "mJFmTx.txGetRdsMusicSpeechFlag returned status " + status.toString());
            return false;
        }
        return true;

    }


    /*************************************************************************************************
     * Implementation of IFmRadio IPC interface
     *************************************************************************************************/

    public int txGetFMState() {
        mContext.enforceCallingOrSelfPermission(FMRX_PERM,
                "Need FMRX_PERM permission");
        return mTxState;
    }



    /*************************************************************************************************
     * JFmRxlback interface for receiving its events and for broadcasting them
     * as intents
     *************************************************************************************************/

    public void fmRxRawRDS(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxRdsGroupTypeMask bitInMaskValue, byte[] groupData)

    {
        Log.i(TAG, "StubFmService:fmRxRawRDS ");
        Log.d(TAG, "StubFmService:fmRxRawRDS status = " + status.toString());
        Log.d(TAG, "StubFmService:fmRxRawRDS bitInMaskValue = "
                + bitInMaskValue.getValue());

    }

    public void fmRxRadioText(JFmRx context, JFmRxStatus status,
            boolean resetDisplay, byte[] msg1, int len, int startIndex,
            JFmRx.JFmRxRepertoire repertoire) {
        Log.i(TAG, "StubFmService:fmRxRadioText ");
        Log.d(TAG,
                "StubFmService:fmRxRadioText status = , msg1 =  ,len =  , startIndex  = "
                        + status.toString() + " " +" " + len
                        + " " + startIndex);
        Log.d(TAG, "StubFmService:sending intent RDS_TEXT_CHANGED_ACTION");

        Intent intentRds = new Intent(FmRadioIntent.RDS_TEXT_CHANGED_ACTION);
        if (FM_SEND_RDS_IN_BYTEARRAY == true) {
            Bundle b = new Bundle();
            b.putByteArray(FmRadioIntent.RDS, msg1);// Send Byte Array to App
            intentRds.putExtras(b);
        } else {
            /*
             *Convert the received Byte Array to
             * appropriate String
             */
            String radioTextString = findFromLookupTable(msg1, repertoire);
            intentRds.putExtra(FmRadioIntent.RADIOTEXT_CONVERTED,
                    radioTextString);// converted String
        }

        intentRds.putExtra(FmRadioIntent.STATUS, status.getValue());// Status

        mContext.sendBroadcast(intentRds, FMRX_PERM);
    }

    public void fmRxPiCodeChanged(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxRdsPiCode pi) {

        Log.i(TAG, "StubFmService:fmRxPiCodeChanged ");
        Log.d(TAG, "StubFmService:fmRxPiCodeChanged status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxPiCodeChanged pi = " + pi.getValue());

        if (status == JFmRxStatus.SUCCESS) {
            /* simple mechanism to prevent displaying repetitious messages */
            if (pi.getValue() != last_msg_printed) {
                last_msg_printed = pi.getValue();
                Log.d(TAG, "StubFmService:sending intent PI_CODE_CHANGED_ACTION");
                Intent intentPi = new Intent(
                        FmRadioIntent.PI_CODE_CHANGED_ACTION);
                intentPi.putExtra(FmRadioIntent.PI, pi.getValue());
                intentPi.putExtra(FmRadioIntent.STATUS, status.getValue());
                mContext.sendBroadcast(intentPi, FMRX_PERM);
            }
        }
    }

    public void fmRxPtyCodeChanged(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxRdsPtyCode pty) {

        Log.i(TAG, "StubFmService:fmRxPtyCodeChanged ");
        Log.d(TAG, "StubFmService:fmRxPtyCodeChanged status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxPtyCodeChanged pty = " + pty.getValue());

    }

    public void fmRxPsChanged(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxFreq frequency, byte[] name,
            JFmRx.JFmRxRepertoire repertoire) {
        Log.i(TAG, "StubFmService:fmRxPsChanged ");
        Log.d(TAG, "StubFmService:fmRxPsChanged status = " + status.toString());
        Log.d(TAG, "StubFmService:fmRxPsChanged frequency = "
                + frequency.getValue());

        Log.d(TAG, "StubFmService:fmRxPsChanged repertoire = "
                + repertoire.getValue());
        Log.d(TAG, "StubFmService:sending intent PS_CHANGED_ACTION");


        Intent intentPs = new Intent(FmRadioIntent.PS_CHANGED_ACTION);
        if (FM_SEND_RDS_IN_BYTEARRAY == true) {
            Bundle b = new Bundle();
            b.putByteArray(FmRadioIntent.PS, name);// Send Byte Array to App
            intentPs.putExtras(b);
        } else {
            /*
             * Convert the received Byte Array to
             * appropriate String Broadcast the PS data Byte Array, Converted
             * string to App
             */

            String psString = findFromLookupTable(name, repertoire);
            Log.i(TAG, "fmRxPsChanged--> psString = " + psString);
            intentPs.putExtra(FmRadioIntent.PS_CONVERTED, psString);// converted
            // PS
            // String
        }

        intentPs.putExtra(FmRadioIntent.REPERTOIRE, repertoire.getValue());// repertoire
        intentPs.putExtra(FmRadioIntent.STATUS, status.getValue());// status

        mContext.sendBroadcast(intentPs, FMRX_PERM);

    }

    public void fmRxMonoStereoModeChanged(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxMonoStereoMode mode) {
        Log.e(TAG, "StubFmService:fmRxMonoStereoModeChanged status = "
                + status.toString());

        Log.i(TAG, "StubFmService:fmRxMonoStereoModeChanged ");
        Log.d(TAG, "StubFmService:fmRxMonoStereoModeChanged status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxMonoStereoModeChanged mode = "
                + mode.getValue());

        /* simple mechanism to prevent displaying repetitious messages */
        if (mode == mMode)
            return;

        mMode = mode;
        switch (mMode) {
        case FM_RX_MONO:
            Log.e(TAG, "Mono Mode");
            break;
        case FM_RX_STEREO:
            Log.e(TAG, "Stereo Mode");
            break;
        default:
            Log.e(TAG, "Illegal stereo mode received from stack: " + mode);
            break;
        }
        Log
                .d(TAG,
                        "StubFmService:sending intent DISPLAY_MODE_MONO_STEREO_ACTION");
        Intent intentMode = new Intent(
                FmRadioIntent.DISPLAY_MODE_MONO_STEREO_ACTION);
        intentMode.putExtra(FmRadioIntent.MODE_MONO_STEREO, mode.getValue());
        intentMode.putExtra(FmRadioIntent.STATUS, status.getValue());
        mContext.sendBroadcast(intentMode, FMRX_PERM);
    }

    public void fmRxAudioPathChanged(JFmRx context, JFmRxStatus status) {

        Log.i(TAG, "StubFmService:fmRxAudioPathChanged ");
        Log.d(TAG, "StubFmService:fmRxAudioPathChanged status = "
                + status.toString());
        Log.d(TAG, "StubFmService:sending intent AUDIO_PATH_CHANGED_ACTION");

        Intent intentPath = new Intent(
                FmRadioIntent.AUDIO_PATH_CHANGED_ACTION);
        intentPath.putExtra(FmRadioIntent.STATUS, status.getValue());
        mContext.sendBroadcast(intentPath, FMRX_PERM);
    }

    public void fmRxAfSwitchFreqFailed(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxRdsPiCode pi, JFmRx.JFmRxFreq tunedFreq,
            JFmRx.JFmRxAfFreq afFreq) {

        Log.i(TAG, "StubFmService:fmRxAfSwitchFreqFailed ");
        Log.d(TAG, "StubFmService:fmRxAfSwitchFreqFailed status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxAfSwitchFreqFailed pi = " + pi.getValue());
        Log.d(TAG, "StubFmService:fmRxAfSwitchFreqFailed tunedFreq = "
                + tunedFreq.getValue());
        Log.d(TAG, "StubFmService:fmRxAfSwitchFreqFailed afFreq = "
                + afFreq.getAfFreq());

    }

    public void fmRxAfSwitchStart(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxRdsPiCode pi, JFmRx.JFmRxFreq tunedFreq,
            JFmRx.JFmRxAfFreq afFreq) {

        Log.i(TAG, "StubFmService:fmRxAfSwitchStart ");
        Log.d(TAG, "StubFmService:fmRxAfSwitchStart status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxAfSwitchStart pi = " + pi.getValue());
        Log.d(TAG, "StubFmService:fmRxAfSwitchStart tunedFreq = "
                + tunedFreq.getValue());
        Log.d(TAG, "StubFmService:fmRxAfSwitchStart afFreq = "
                + afFreq.getAfFreq());

    }

    public void fmRxAfSwitchComplete(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxRdsPiCode pi, JFmRx.JFmRxFreq tunedFreq,
            JFmRx.JFmRxAfFreq afFreq) {

        Log.i(TAG, "StubFmService:fmRxAfSwitchComplete ");
        Log.d(TAG, "StubFmService:fmRxAfSwitchComplete status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxAfSwitchComplete pi = " + pi.getValue());
        Log.d(TAG, "StubFmService:fmRxAfSwitchComplete tunedFreq = "
                + tunedFreq.getValue());
        Log.d(TAG, "StubFmService:fmRxAfSwitchComplete afFreq = "
                + afFreq.getAfFreq());

    }

    public void fmRxAfListChanged(JFmRx context, JFmRxStatus status,
            JFmRx.JFmRxRdsPiCode pi, int[] afList,
            JFmRx.JFmRxAfListSize afListSize) {

        Log.i(TAG, "StubFmService:fmRxAfListChanged ");
        Log.d(TAG, "StubFmService:fmRxAfListChanged status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxAfListChanged pi = " + pi.getValue());
        Log.d(TAG, "StubFmService:fmRxAfListChanged afListSize = "
                + afListSize.getValue());

    }

    public void fmRxCompleteScanDone(JFmRx context, JFmRxStatus status,
            int numOfChannels, int[] channelsData) {

        mIsCompleteScanInProgress = false;

        Log.i(TAG, "StubFmService:fmRxCompleteScanDone ");
        Log.d(TAG, "StubFmService:fmRxCompleteScanDone status = "
                + status.toString());
        Log.d(TAG, "StubFmService:fmRxCompleteScanDone numOfChannels = "
                + numOfChannels);
        for (int i = 0; i < numOfChannels; i++)
            Log.d(TAG, "StubFmService:fmRxCompleteScanDone channelsData = " + i
                    + "  " + +channelsData[i]);

        Log.i(TAG, "StubFmService:COMPLETE_SCAN_DONE_ACTION ");
        Intent intentscan = new Intent(
                FmRadioIntent.COMPLETE_SCAN_DONE_ACTION);
        Bundle b = new Bundle();
        b.putIntArray(FmRadioIntent.SCAN_LIST, channelsData);
        b.putInt(FmRadioIntent.STATUS, status.getValue());
        b.putInt(FmRadioIntent.SCAN_LIST_COUNT, numOfChannels);
        intentscan.putExtras(b);
        mContext.sendBroadcast(intentscan, FMRX_PERM);
        }

    public  void fmRxCmdEnable(JFmRx context, JFmRxStatus status, int command,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdEnable ");
        Log.d(TAG, "  fmRxCmdEnable ( command: , status: , value: )" + command
                + "" + status + "" + value);

        if (status == JFmRxStatus.SUCCESS) {
            updateEnableConfiguration();
        }

        else {

            Log.e(TAG, "  fmRxCmdEnable fail (status: " + status + ", value: "
                    + value);
            mRxState = STATE_DEFAULT;
        }
        int RXicon = R.drawable.rxradio;
        sendNotificationRX();
        Log.d(TAG, "Sending restore values intent");
            Intent restoreValuesIntent = new Intent(FM_RESTORE_VALUES);
            mContext.sendBroadcast(restoreValuesIntent);
        enableIntent(status);
    }

    public synchronized void fmRxCmdDisable(JFmRx context, JFmRxStatus status, int command,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdDisable ");
        Log.d(TAG, "  fmRxCmdDisable ( command: , status: , value: )" + command
                + "" + status + "" + value);

    if (status != JFmRxStatus.SUCCESS) {
            Log.e(TAG, "StubFmService:fmRxCmdDisable failed ");
            return ;
        }
        destroyJFmRx();
            mRxState = STATE_DISABLED;
             int off =0;
              /* L27 Specific */
             enableRx(off);
	     /* Release the wake lock
	      * so that device can go into
	      * OFF mode
	      */
	     if ((mWakeLock != null) && (mWakeLock.isHeld()))
		     mWakeLock.release();
	     Log.d(TAG, "FM RX powered on mRxState " + mRxState);
        Log.d(TAG, "StubFmService:sending intent FM_DISABLED_ACTION");

        cancelNotification(FM_RX_NOTIFICATION_ID);

            if (mFmPauseResume == STATE_PAUSE) {
                ;
            } else {
                if (DBG)
                    Log
                            .d(TAG,
                                    "StubFmRxService:sending intent FM_DISABLED_ACTION");
        Intent intentDisable = new Intent(FmRadioIntent.FM_DISABLED_ACTION);
            intentDisable.putExtra(FmRadioIntent.STATUS, status.getValue());
                mContext.sendBroadcast(intentDisable, FMRX_PERM);
            }

    }

    public void fmRxCmdDestroy(JFmRx context, JFmRxStatus status, int command,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdDestroy ");
        Log.d(TAG, "  fmRxCmdDestroy ( command: , status: , value: )" + command
                + "" + status + "" + value);
    }

    public void fmRxCmdDisableAudio(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdDisableAudio ");
        Log.d(TAG, "  fmRxCmdDisableAudio ( command: , status: , value: )"
                + command + "" + status + "" + value);
    }

    public  void fmRxCmdGetRdsAfSwitchMode(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetRdsAfSwitchMode ");
        Log.d(TAG,
                "  fmRxCmdGetRdsAfSwitchMode ( command: , status: , value: )"
                        + command + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call

              getRdsAfSwitchModeValue = (int) value;

            /* implementation to make the FM API Synchronous */
            try {
                mRdsAfSwitchModeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

            Log.d(TAG,
                    "  fmRxCmdGetRdsAfSwitchMode ( getRdsAfSwitchModeValue: )"
                            + getRdsAfSwitchModeValue);

        } else {
          // Code for non blocking call
            Log.d(TAG,
                    "StubFmService:sending intent GET_RDS_AF_SWITCH_MODE_ACTION");

        Intent getRdsAf = new Intent(
        FmRadioIntent.GET_RDS_AF_SWITCH_MODE_ACTION);
        getRdsAf.putExtra(FmRadioIntent.GET_RDS_AF_SWITCHMODE, value);
        getRdsAf.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getRdsAf, FMRX_PERM);

        }



    }

    public void fmRxCmdSetRdsAfSwitchMode(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetRdsAfSwitchMode ");
        Log.d(TAG,
                "  fmRxCmdSetRdsAfSwitchMode ( command: , status: , value: )"
                        + command + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
        // Code for blocking call
            /* implementation to make the FM API Synchronous */
            try {
                mSetRdsAfSwitchModeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

        }
        else
        {

            Log.d(TAG, "StubFmService:sending intent SET_RDS_AF_ACTION");
        Intent intent = new Intent(FmRadioIntent.SET_RDS_AF_ACTION);
        intent.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intent, FMRX_PERM);
    }
    }

    public void fmRxCmdSetRdsSystem(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetRdsSystem ");
        Log.d(TAG, "  fmRxCmdSetRdsSystem ( command: , status: , value: )"
                + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
            getRdsSystemValue = (int) value;
                /* implementation to make the FM API Synchronous */
            try {
            mRdsSystemSyncQueue.put("*");
            } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

            Log.d(TAG, "  fmRxCmdGetRdsSystem ( getRdsSystemValue: )"
            + getRdsSystemValue);
        }
        else
        {
            Log.d(TAG, "StubFmService:sending intent SET_RDS_SYSTEM_ACTION");
        Intent intent = new Intent(FmRadioIntent.SET_RDS_SYSTEM_ACTION);
        intent.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intent, FMRX_PERM);
        }

    }

    public  void fmRxCmdGetRdsSystem(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetRdsSystem ");
        Log.d(TAG, "  fmRxCmdGetRdsSystem ( command: , status: , value: )"
                + command + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                        getRdsSystemValue = (int) value;

            /* implementation to make the FM API Synchronous */
            try {
                mRdsSystemSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
            Log.d(TAG, "  fmRxCmdGetRdsSystem ( getRdsSystemValue: )"
                    + getRdsSystemValue);
        } else {
          // Code for non blocking call
                    Log.d(TAG, "StubFmService:sending intent GET_RDS_SYSTEM_ACTION");

            Intent getRdsSystem = new Intent(
                    FmRadioIntent.GET_RDS_SYSTEM_ACTION);
        getRdsSystem.putExtra(FmRadioIntent.GET_RDS_SYSTEM, value);
        getRdsSystem.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getRdsSystem, FMRX_PERM);
        }

    }

    public void fmRxCmdDisableRds(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdDisableRds ");
        Log.d(TAG, "  fmRxCmdDisableRds ( command: , status: , value: )"
                + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
            /* implementation to make the FM API Synchronous */

            try {
                mDisableRdsSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;


        } else {
            // Code for non blocking call

        Log.d(TAG, "StubFmService:sending intent DISABLE_RDS_ACTION");
        Intent intent = new Intent(FmRadioIntent.DISABLE_RDS_ACTION);
        intent.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intent, FMRX_PERM);
    }

    }

    public void fmRxCmdEnableRds(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdEnableRds ");
        Log.d(TAG, "  fmRxCmdEnableRds ( command: , status: , value: )"
                + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
            /*implementation to make the set API Synchronous */

            try {
                mEnableRdsSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;


        } else {
            // Code for non blocking call
        Log.d(TAG, "StubFmService:sending intent ENABLE_RDS_ACTION");
        Intent intentRdsOn = new Intent(FmRadioIntent.ENABLE_RDS_ACTION);
        intentRdsOn.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentRdsOn, FMRX_PERM);
        }

    }

    public   void fmRxCmdGetRdsGroupMask(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetRdsGroupMask ");
        Log.d(TAG, "  fmRxCmdGetRdsGroupMask ( command: , status: , value: )"
                + command + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
          getRdsGroupMaskValue = value;

            /* implementation to make the FM API Synchronous */
            try {
                mRdsGroupMaskSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
          Log.d(TAG, "    fmRxCmdGetRdsGroupMask ( getRdsGroupMaskValue: )"
                  + getRdsGroupMaskValue);
        } else {
          // Code for non blocking call
                      Log.d(TAG, "StubFmService:sending intent GET_RDS_GROUPMASK_ACTION");

                Intent getRdsGroup = new Intent(
                FmRadioIntent.GET_RDS_GROUPMASK_ACTION);
        getRdsGroup.putExtra(FmRadioIntent.GET_RDS_GROUPMASK, value);
        getRdsGroup.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getRdsGroup, FMRX_PERM);
        }

    }

    public void fmRxCmdSetRdsGroupMask(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetRdsGroupMask ");
        Log.d(TAG, "  fmRxCmdSetRdsGroupMask ( command: , status: , value: )"
                + command + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
                  // Code for blocking call
        /* implementation to make the FM API Synchronous */
            try {
                mSetRdsGroupMaskSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
        }
        else
        {
        Intent intent = new Intent(FmRadioIntent.SET_RDS_GROUP_MASK_ACTION);
        intent.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intent, FMRX_PERM);
        }

    }

    public void fmRxCmdGetRssi(JFmRx context, JFmRxStatus status, int command,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetRssi ");
        Log.d(TAG, "  fmRxCmdGetRssi ( command: , status: , value: )" + command
                + "" + status + "" + value);
        // The RSSI value is 8 bit signed number in 2's complement format

        getRssiValue = convertUnsignedToSignedInt(value);

        if (MAKE_FM_APIS_BLOCKING == true) {
                  // Code for blocking call

        /*implementation to make the FM API Synchronous */
        try {
            mRssiSyncQueue.put("*");
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException on queue wakeup!");
        }
        ;
        Log.d(TAG, "  fmRxCmdGetRssi ( getRssiValue: )" + getRssiValue);
    }
        else
            {
                    Log.d(TAG, "StubFmService:sending intent GET_RSSI_ACTION");
        Intent intent = new Intent(FmRadioIntent.GET_RSSI_ACTION);
        intent.putExtra(FmRadioIntent.GET_RSSI, getRssiValue);
        intent.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intent, FMRX_PERM);
            }
    }

    public void fmRxCmdStopSeek(JFmRx context, JFmRxStatus status, int command,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdStopSeek ");
        Log.d(TAG, "  fmRxCmdStopSeek ( command: , status: , value: )"
                + command + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
                  // Code for blocking call
            try {
                mStopSeekSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
            }else
                      {
        Log.d(TAG, "StubFmService:sending intent SEEK_STOP_ACTION");
        Intent intentstop = new Intent(FmRadioIntent.SEEK_STOP_ACTION);
        intentstop.putExtra(FmRadioIntent.SEEK_FREQUENCY, value);
        intentstop.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentstop, FMRX_PERM);
    }
    }

    public void fmRxCmdSeek(JFmRx context, JFmRxStatus status, int command,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSeek ");
        Log.d(TAG, "  fmRxCmdSeek ( command: , status: , value: )" + command
                + "" + status + "" + value);
        Log.d(TAG, "StubFmService:sending intent SEEK_ACTION");
        mIsSeekInProgress = false;
        mCurrentFrequency = (int) value;

        Log.d(TAG, "StubFmService:sending intent SEEK_ACTION");
        Intent intentstart = new Intent(FmRadioIntent.SEEK_ACTION);
        intentstart.putExtra(FmRadioIntent.SEEK_FREQUENCY, mCurrentFrequency);
        intentstart.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentstart, FMRX_PERM);
    }

    public void fmRxCmdGetTunedFrequency(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetTunedFrequency ");
        Log.d(TAG, "  fmRxCmdGetTunedFrequency ( command: , status: , value: )"
                + command + "" + status + "" + value);
        getTunedFrequencyValue = (int) value;

        if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
              getTunedFrequencyValue = (int) value;

                /* implementation to make the FM API Synchronous */

            try {
                mTunedFrequencySyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
            Log.d(TAG, "  fmRxCmdGetTunedFrequency ( getTunedFrequencyValue: )"
                    + getTunedFrequencyValue);
        } else {
          // Code for non blocking call
                  Log.d(TAG, "StubFmService:sending intent GET_FREQUENCY_ACTION");
            Intent freqIntent = new Intent(
                    FmRadioIntent.GET_FREQUENCY_ACTION);
        freqIntent.putExtra(FmRadioIntent.TUNED_FREQUENCY, getTunedFrequencyValue);
        freqIntent.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(freqIntent, FMRX_PERM);
        }
    }

    public void fmRxCmdTune(JFmRx context, JFmRxStatus status, int command,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdTune ");
        Log.d(TAG, "  fmRxCmdTune ( command: , status: , value: )" + command
                + "" + status + "" + value);
        mIsTuneInProgress = false;
        mCurrentFrequency = (int) value;
        Log.d(TAG, "StubFmService:sending intent TUNE_COMPLETE_ACTION");
        /* Now FM resume has been completed.Set it to default. */
            if (mFmPauseResume ==STATE_RESUME) {

//                mFmPauseResume =STATE_DEFAULT;
            } else {
                Intent intentTune = new Intent(
                        FmRadioIntent.TUNE_COMPLETE_ACTION);
                intentTune.putExtra(FmRadioIntent.TUNED_FREQUENCY, mCurrentFrequency);
                intentTune.putExtra(FmRadioIntent.STATUS, status);
                mContext.sendBroadcast(intentTune, FMRX_PERM);
            }

    }

    public void fmRxCmdGetVolume(JFmRx context, JFmRxStatus status,
            int command, long value) {
        Log.i(TAG, "StubFmService:fmRxCmdGetVolume ");
        Log.d(TAG, "  fmRxCmdGetVolume ( command: , status: , value: )"
                + command + "" + status + "" + value);
            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                        getVolumeValue = (int) value;

            /*implementation to make the FM API Synchronous */
            try {
                mVolumeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
            Log.d(TAG, "  fmRxCmdGetVolume ( getVolumeValue: )"
                    + getVolumeValue);
        } else {
          // Code for non blocking call
              Log.d(TAG, "StubFmService:sending intent GET_VOLUME_ACTION");
                  Intent getVolume = new Intent(FmRadioIntent.GET_VOLUME_ACTION);
        getVolume.putExtra(FmRadioIntent.GET_VOLUME, value);
        getVolume.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getVolume, FMRX_PERM);

        }
    }

    public void fmRxCmdSetVolume(JFmRx context, JFmRxStatus status,
            int command, long value) {

            /*
         * volume change will be completed here. So set the state to idle, so
         * that user can set other volume.
             */
            synchronized (mVolumeSynchronizationLock) {
                mVolState = VOL_REQ_STATE_IDLE;
            }

        Log.d(TAG, "StubFmService:fmRxCmdDone  JFmRxCommand.CMD_SET_VOLUME");
        Log.d(TAG, "  fmRxCmdSetVolume ( command: , status: , value: )"
                    + command + "" + status + "" + value);
        Log.d(TAG, "StubFmService:sending intent VOLUME_CHANGED_ACTION");
            /* If the FM is resumed, then load the current band. */
            if (mFmPauseResume ==STATE_RESUME) {
            Log.d(TAG, "StubFmService:FM resuming . Dont do anything");
            }
            /*
         * If the FM is enabled, then send the intent to App to load the band..
             */
            else {
                Intent intentVolume = new Intent(
                        FmRadioIntent.VOLUME_CHANGED_ACTION);
                intentVolume.putExtra(FmRadioIntent.STATUS, status);
                mContext.sendBroadcast(intentVolume, FMRX_PERM);
            }

    }

    public void fmRxCmdGetDeemphasisFilter(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetDeemphasisFilter ");
        Log.d(TAG,
                "  fmRxCmdGetDeemphasisFilter ( command: , status: , value: )"
                        + command + "" + status + "" + value);

            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call

                getDeEmphasisFilterValue = (int) value;

            /*implementation to make the FM API Synchronous */

            try {
                mDeEmphasisFilterSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

            Log.d(TAG,
                    "  fmRxCmdGetDeemphasisFilter ( getDeEmphasisFilterValue: )"
                            + getDeEmphasisFilterValue);
        } else {
          // Code for non blocking call
          Log.d(TAG, "StubFmService:sending intent GET_DEEMPHASIS_FILTER_ACTION");
          Intent getDeEmpFilter = new Intent(
                      FmRadioIntent.GET_DEEMPHASIS_FILTER_ACTION);
              getDeEmpFilter.putExtra(FmRadioIntent.GET_DEEMPHASIS_FILTER, value);
              getDeEmpFilter.putExtra(FmRadioIntent.STATUS, status);
              mContext.sendBroadcast(getDeEmpFilter, FMRX_PERM);

        }
    }

    public void fmRxCmdSetDeemphasisFilter(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetDeemphasisFilter ");
        Log.d(TAG,
                "  fmRxCmdSetDeemphasisFilter ( command: , status: , value: )"
                        + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
            /*implementation to make the set API Synchronous */
            try {
                mSetDeEmphasisFilterSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

        } else {
            // Code for non blocking call
        Log.d(TAG, "StubFmService:sending intent SET_DEEMP_FILTER_ACTION");
        Intent intent = new Intent(FmRadioIntent.SET_DEEMP_FILTER_ACTION);
        intent.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intent, FMRX_PERM);
    }

    }

    public void fmRxCmdGetRssiThreshhold(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetRssiThreshhold ");
        Log.d(TAG, "  fmRxCmdGetRssiThreshhold ( command: , status: , value: )"
                + command + "" + status + "" + value);

            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call

            getRssiThresholdValue = (int) value;

            /*implementation to make the FM API Synchronous */
            try {
                mRssiThresholdSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
            Log.d(TAG, "  fmRxCmdGetRssiThreshhold ( getRssiThresholdValue: )"
                    + getRssiThresholdValue);

        } else {
          // Code for non blocking call
           Log.d(TAG, "StubFmService:sending intent GET_RSSI_THRESHHOLD_ACTION");
                Intent getRssiThreshHold = new Intent(
                FmRadioIntent.GET_RSSI_THRESHHOLD_ACTION);
            getRssiThreshHold.putExtra(FmRadioIntent.GET_RSSI_THRESHHOLD,
                    value);
        getRssiThreshHold.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getRssiThreshHold, FMRX_PERM);
        }
    }

    public void fmRxCmdSetRssiThreshhold(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetRssiThreshhold ");
        Log.d(TAG, "  fmRxCmdSetRssiThreshhold ( command: , status: , value: )"
                + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
                /* implementation to make the FM API Synchronous */

            try {
                mSetRssiThresholdSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;


        } else {
            // Code for non blocking call
        Log.d(TAG, "StubFmService:sending intent SET_RSSI_THRESHHOLD_ACTION");
        Intent intentRssi = new Intent(
                FmRadioIntent.SET_RSSI_THRESHHOLD_ACTION);
        intentRssi.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentRssi, FMRX_PERM);
        }

    }

    public void fmRxCmdGetRfDependentMuteMode(JFmRx context,
            JFmRxStatus status, int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetRfDependentMuteMode ");
        Log.d(TAG,
                "  fmRxCmdGetRfDependentMuteMode ( command: , status: , value: )"
                        + command + "" + status + "" + value);

            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call

          getRfDependentMuteModeValue = (int) value;

          /*implementation to make the FM API Synchronous */

            try {
                mRfDependentMuteModeSyncQueue.put("*");
            } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

          Log.d(TAG,
                  "  fmRxCmdGetRfDependentMuteMode ( getRfDependentMuteModeValue: )"
                          + getRfDependentMuteModeValue);
        } else {
          // Code for non blocking call
          Log.d(TAG, "StubFmService:sending intent GET_RF_MUTE_MODE_ACTION");

                  Intent getRfMuteMode = new Intent(
                FmRadioIntent.GET_RF_MUTE_MODE_ACTION);
        getRfMuteMode.putExtra(FmRadioIntent.GET_RF_MUTE_MODE, value);
        getRfMuteMode.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getRfMuteMode, FMRX_PERM);

        }
    }

    public void fmRxCmdSetRfDependentMuteMode(JFmRx context,
            JFmRxStatus status, int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetRfDependentMuteMode ");
        Log.d(TAG,
                "  fmRxCmdSetRfDependentMuteMode ( command: , status: , value: )"
                + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
            /*implementation to make the set API Synchronous */

            try {
                mSetRfDependentMuteModeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;


        } else {
            // Code for non blocking call
            Log.d(TAG,
                    "StubFmService:sending intent SET_RF_DEPENDENT_MUTE_ACTION");
        Intent intentRfMute = new Intent(
                FmRadioIntent.SET_RF_DEPENDENT_MUTE_ACTION);
        intentRfMute.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentRfMute, FMRX_PERM);
    }

            }

    public  void fmRxCmdSetMuteMode(JFmRx context, JFmRxStatus status,
            int command, long value) {
        Log.i(TAG, "StubFmService:fmRxCmdSetMuteMode ");
        Log.d(TAG, "  fmRxCmdSetMuteMode ( command: , status: , value: )"
                + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call

            /* implementation to make the FM API Synchronous */

            try {
                mSetMuteModeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;


        } else {
            // Code for non blocking call

        if(mIsFmMuted == false)
        {
            Log.d(TAG, "StubFmService:sending intent MUTE_CHANGE_ACTION");
            Intent intentMute = new Intent(FmRadioIntent.MUTE_CHANGE_ACTION);
            intentMute.putExtra(FmRadioIntent.STATUS, status);
            mContext.sendBroadcast(intentMute, FMRX_PERM);
        }
    }

    }

    public void fmRxCmdGetMuteMode(JFmRx context, JFmRxStatus status,
            int command, long value) {
        Log.i(TAG, "StubFmService:fmRxCmdGetMuteMode ");
        Log.d(TAG, "  fmRxCmdGetMuteMode ( command: , status: , value: )"
                        + command + "" + status + "" + value);

            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call

                    getMuteModeValue = (int) value;

            /* implementation to make the FM API Synchronous */
            try {
                mMuteModeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

            Log.d(TAG, "  fmRxCmdGetMuteMode ( getMuteModeValue: )"
                    + getMuteModeValue);
        } else {
          // Code for non blocking call
          Log.d(TAG, "StubFmService:sending intent GET_MUTE_MODE_ACTION");

            Intent getMuteMode = new Intent(
                    FmRadioIntent.GET_MUTE_MODE_ACTION);
        getMuteMode.putExtra(FmRadioIntent.GET_MUTE_MODE, value);
        getMuteMode.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getMuteMode, FMRX_PERM);

        }
    }

    public void fmRxCmdGetMonoStereoMode(JFmRx context, JFmRxStatus status,
            int command, long value) {
        Log.i(TAG, "StubFmService:fmRxCmdGetMonoStereoMode ");
        Log.d(TAG, "  fmRxCmdGetMonoStereoMode ( command: , status: , value: )"
                                + command + "" + status + "" + value);

            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
            getMonoStereoModeValue = (int) value;

                /* implementation to make the FM API Synchronous */

            try {
                mMonoStereoModeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

            Log.d(TAG, "  fmRxCmdGetMonoStereoMode ( getMonoStereoModeValue: )"
                    + getMonoStereoModeValue);

        } else {
          // Code for non blocking call
            Log.d(TAG, "StubFmService:sending intent GET_MONO_STEREO_MODE_ACTION");
                Intent getMode = new Intent(
                FmRadioIntent.GET_MONO_STEREO_MODE_ACTION);
        getMode.putExtra(FmRadioIntent.GET_MODE, value);
        getMode.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getMode, FMRX_PERM);
        }
    }

    public void fmRxCmdSetMonoStereoMode(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetMonoStereoMode ");
        Log.d(TAG, "  fmRxCmdSetMonoStereoMode ( command: , status: , value: )"
                                + command + "" + status + "" + value);
        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
            /* implementation to make the set API Synchronous */

            try {
                mSetMonoStereoModeSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

        } else {
            // Code for non blocking call
                Log
                        .d(TAG,
                            "StubFmService:sending intent SET_MODE_MONO_STEREO_ACTION");
        Intent intentMode = new Intent(
                FmRadioIntent.SET_MODE_MONO_STEREO_ACTION);
        intentMode.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentMode, FMRX_PERM);
    }

            }

    public void fmRxCmdGetBand(JFmRx context, JFmRxStatus status, int command,
            long value) {
        Log.i(TAG, "StubFmService:fmRxCmdGetBand ");
        Log.d(TAG, "  fmRxCmdGetBand ( command: , status: , value: )" + command
                + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call

            getBandValue = (int) value;

            /* implementation to make the FM API Synchronous */

            try {
                mBandSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;


            Log.d(TAG, "  fmRxCmdGetBand ( getBandValue: )" + getBandValue);
        } else {
          // Code for non blocking call
          Log.d(TAG, "StubFmService:sending intent GET_BAND_ACTION");
        Intent getBand = new Intent(FmRadioIntent.GET_BAND_ACTION);
        getBand.putExtra(FmRadioIntent.GET_BAND, value);
        getBand.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getBand, FMRX_PERM);

        }
            }

    public void fmRxCmdSetBand(JFmRx context, JFmRxStatus status, int command,
            long value) {
        Log.i(TAG, "StubFmService:fmRxCmdSetBand ");
        Log.d(TAG, "  fmRxCmdSetBand ( command: , status: , value: )" + command
                + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
            // Code for blocking call
            /*implementation to make the Set API Synchronous */

            try {
                mSetBandSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;


        } else {
            // Code for non blocking call
            Log.d(TAG, "StubFmService:FM resuming . mFmPauseResume"
                    + mFmPauseResume);
        /* If the FM is resumed, then load the previously tuned frequency. */
        if (mFmPauseResume ==STATE_RESUME) {
                Log.d(TAG, "StubFmService:FM resuming . Dont do anything");
        }
        /*
         * If the FM is enabled, then send the intent to App to load the
         * freq..
         */
        else {
                Log.d(TAG, "StubFmService:sending intent BAND_CHANGE_ACTION");
                Intent intentBand = new Intent(FmRadioIntent.BAND_CHANGE_ACTION);
            intentBand.putExtra(FmRadioIntent.STATUS, status);
            mContext.sendBroadcast(intentBand, FMRX_PERM);
        }

        }

    }

    public void fmRxCmdChangeAudioTarget(JFmRx context, JFmRxStatus status,
            int command, long AudioCmd) {
        Log.i(TAG, "StubFmService:fmRxCmdChangeAudioTarget ");
        Log.d(TAG,
                "  fmRxCmdChangeAudioTarget ( command: , status: , AudioCmd: )"
                        + command + "" + status + "" + AudioCmd);
    }

    public void fmRxCmdEnableAudio(JFmRx context, JFmRxStatus status,
            int command, long AudioCmd) {
        Log.i(TAG, "StubFmService:fmRxCmdEnableAudio ");
        Log.d(TAG,
                "  fmRxCmdEnableAudio ( command: , status: , AudioCmd: )"
                        + command + "" + status + "" + AudioCmd);
    }

    public void fmRxCmdChangeDigitalAudioConfiguration(JFmRx context,
            JFmRxStatus status, int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdChangeDigitalAudioConfiguration ");
                Log.d(TAG,
                "  fmRxCmdChangeDigitalAudioConfiguration ( command: , status: , value: )"
                                + command + "" + status + "" + value);
            }

    public void fmRxCmdGetChannelSpacing(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetChannelSpacing ");
        Log.d(TAG, "  fmRxCmdGetChannelSpacing ( command: , status: , value: )"
                        + command + "" + status + "" + value);

            if (MAKE_FM_APIS_BLOCKING == true) {
          // Code for blocking call
                            getChannelSpaceValue = (int) value;
                /* implementation to make the FM API Synchronous */

            try {
                mChannelSpacingSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;

            Log.d(TAG, "  fmRxCmdGetChannelSpacing ( getChannelSpaceValue: )"
                    + getChannelSpaceValue);

        } else {
          // Code for non blocking call

                    Log.d(TAG, "StubFmService:sending intent GET_CHANNEL_SPACE_ACTION");
            Intent getChSpace = new Intent(
                    FmRadioIntent.GET_CHANNEL_SPACE_ACTION);
        getChSpace.putExtra(FmRadioIntent.GET_CHANNEL_SPACE, value);
        getChSpace.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(getChSpace, FMRX_PERM);

        }
    }

    public void fmRxCmdSetChannelSpacing(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdSetChannelSpacing ");
        Log.d(TAG, "  fmRxCmdSetChannelSpacing ( command: , status: , value: )"
                        + command + "" + status + "" + value);

        if (MAKE_FM_APIS_BLOCKING == true) {
                /* implementation to make the FM API Synchronous */
            try {
                mSetChannelSpacingSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
            }
        else
            {

        Log.d(TAG, "StubFmService:sending intent CHANNEL_SPACING_CHANGED_ACTION");
            Intent intentChannelSpace = new Intent(
                    FmRadioIntent.CHANNEL_SPACING_CHANGED_ACTION);
            intentChannelSpace.putExtra(FmRadioIntent.STATUS, status);
            mContext.sendBroadcast(intentChannelSpace, FMRX_PERM);
    }
    }

    public void fmRxCmdIsValidChannel(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdIsValidChannel ");
        Log.d(TAG, "  fmRxCmdIsValidChannel ( command: , status: , value: )"
                                + command + "" + status + "" + value);
        if (value > 0)
            mIsValidChannel = true;
        else
            mIsValidChannel = false;

        /*implementation to make the FM API Synchronous */

        try {
            mValidChannelSyncQueue.put("*");
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException on queue wakeup!");
            }
        ;

        Log.d(TAG, "  fmRxCmdIsValidChannel ( isValidChannel: )"
                + mIsValidChannel);

            }

    public void fmRxCmdGetFwVersion(JFmRx context, JFmRxStatus status,
            int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetFwVersion ");
        Log.d(TAG, "  fmRxCmdGetFwVersion ( command: , status: , value: )"
                                + command + "" + status + "" + value);

            getFwVersion = ((double) value / 1000);

            /* implementation to make the FM API Synchronous */
            try {
                mFwVersionSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
        Log.d(TAG, "  fmRxCmdGetFwVersion ( getFwVersion: )" + getFwVersion);

    }

    public void fmRxCmdGetCompleteScanProgress(JFmRx context,
            JFmRxStatus status, int command, long value) {

        Log.i(TAG, "StubFmService:fmRxCmdGetCompleteScanProgress ");
                Log.d(TAG,
                        "  fmRxCmdGetCompleteScanProgress ( command: , status: , value: )"
                                + command + "" + status + "" + value);

            getScanProgress = (int) value;

            if (MAKE_FM_APIS_BLOCKING == true) {


                /* implementation to make the FM API Synchronous */

            try {
                mCompleteScanProgressSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }


        Log.d(TAG, "  fmRxCmdGetCompleteScanProgress ( getFwVersion: )"
                                + getScanProgress);
                }
            else
            {

                    Log.d(TAG, "StubFmService:sending intent COMPLETE_SCAN_PROGRESS_ACTION");
            Intent intent = new Intent(
                    FmRadioIntent.COMPLETE_SCAN_PROGRESS_ACTION);
            intent.putExtra(FmRadioIntent.STATUS, status);
            intent.putExtra(FmRadioIntent.SCAN_PROGRESS, getScanProgress);
            mContext.sendBroadcast(intent, FMRX_PERM);

        }

            }

    public void fmRxCmdStopCompleteScan(JFmRx context, JFmRxStatus status,
            int command, long value) {

    Log.i(TAG, "StubFmService:fmRxCmdStopCompleteScan ");
        Log.d(TAG, "  fmRxCmdStopCompleteScan ( command: , status: , value: )"
    + command + "" + status + "" + value);
            mIsCompleteScanInProgress = false;

    if (MAKE_FM_APIS_BLOCKING == true) {

        mStopCompleteScanStatus = status.getValue();

        /* implementation to make the FM API Synchronous */

            try {
                mStopCompleteScanSyncQueue.put("*");
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException on queue wakeup!");
            }
            ;
        Log.d(TAG, "  fmRxCmdStopCompleteScan ( mStopCompleteScanStatus: )"
                                + mStopCompleteScanStatus);
    }
    else
    {

        Log.d(TAG, "StubFmService:sending intent COMPLETE_SCAN_STOP_ACTION ");

    Intent intentStopScan = new Intent(
            FmRadioIntent.COMPLETE_SCAN_STOP_ACTION);

        Bundle b = new Bundle();
        b.putInt(FmRadioIntent.STATUS, status.getValue());
        b.putInt(FmRadioIntent.LAST_SCAN_CHANNEL, (int)value);
        intentStopScan.putExtras(b);
    mContext.sendBroadcast(intentStopScan, FMRX_PERM);
    }

    }






/*****  FM TX ******/

    public void fmTxCmdEnable(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdEnable ");
        Log.d(TAG, "  fmTxCmdEnable ( status: " + status + " )");

            Intent intentTxEnable = new Intent(FmRadioIntent.FM_TX_ENABLED_ACTION);
            intentTxEnable.putExtra(FmRadioIntent.STATUS, status);
            mContext.sendBroadcast(intentTxEnable, FMRX_PERM);
            mTxState = STATE_ENABLED;


	sendNotificationTX();
        /* Ne need to call again, let it take from default */

        //if(status ==JFmTxStatus.SUCCESS )
        //    {
          //        /*Enable the Audio deafult platfrom values */
        //        JFmTx.JFmTxEcalResource fmTxDeafultSourceConfig =
        //        JFmUtils.getEnumConst(    JFmTx.JFmTxEcalResource.class,fmTxDeafultCalResource);

        //        JFmTx.JFmTxEcalSampleFrequency fmTxDeafultFreqConfig = JFmUtils.getEnumConst(
        //        JFmTx.JFmTxEcalSampleFrequency.class,fmTxDeafultSampleFrequency);

        //        mJFmTx.txChangeAudioSource(fmTxDeafultSourceConfig,fmTxDeafultFreqConfig);
                /* Notify to APM */

        //    }
    }

    public void fmTxCmdDisable(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdDisable ");
        Log.d(TAG, "  fmTxCmdDisable ( status: " + status + " )");
        /* Notify to APM */

             if (isTransmissionOn()) {
                   Log.i(TAG, "StubFmService:Sending ACTION_FMTx_PLUG ");
                   enableTx(0);
                   Log.i(TAG, "StubFmService:Sent! ACTION_FMTx_PLUG ");
             }

             mIsFmTxOn = false;

        cancelNotification(FM_TX_NOTIFICATION_ID);

        destroyJFmTx();

        Intent intentTxDisable = new Intent(FmRadioIntent.FM_TX_DISABLED_ACTION);
        intentTxDisable.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentTxDisable, FMRX_PERM);
        mTxState = STATE_DISABLED;

    }


    public void fmTxCmdDestroy(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdDestroy ");
        Log.d(TAG, "  fmRxCmdDestroy (status: " + status + " )");

        Intent intentTxDestroy = new Intent(FmRadioIntent.FM_TX_DESTROY_ACTION);
        intentTxDestroy.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentTxDestroy, FMRX_PERM);

    }

    public void fmTxCmdStartTransmission(JFmTx context, JFmTxStatus status/*,
            JFmCcmVacUnavailResourceList ccmVacUnavailResourceList*/) {

        Log.i(TAG, "StubFmService:fmTxCmdStartTransmission ");
        Log.d(TAG, "  fmTxCmdStartTransmission (status: " + status + " )");
        Intent intentStartTransmission = new Intent(FmRadioIntent.FM_TX_START_TRANSMISSION_ACTION);
        intentStartTransmission.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentStartTransmission, FMRX_PERM);

        if(status ==JFmTxStatus.SUCCESS ) {
            //mAudioManager.setParameters(AUDIO_TX_ENABLE+"=true");
                    Log.i(TAG, "StubFmService:Sending ACTION_FMTx_PLUG ");
                    enableTx(1);
                    Log.i(TAG, "StubFmService:Sent! ACTION_FMTx_PLUG ");
             }

    }


    public void fmTxCmdStopTransmission(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdStopTransmission ");
        Log.d(TAG, "  fmTxCmdStopTransmission ( status: " + status + " )");

        Intent intentStopTransmission = new Intent(FmRadioIntent.FM_TX_STOP_TRANSMISSION_ACTION);
        intentStopTransmission.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentStopTransmission, FMRX_PERM);

        if(status ==JFmTxStatus.SUCCESS ) {
            //mAudioManager.setParameters(AUDIO_TX_ENABLE+"=false");
                    Log.i(TAG, "StubFmService:Sending ACTION_FMTx_PLUG ");
                    enableTx(0);
                    Log.i(TAG, "StubFmService:Sent! ACTION_FMTx_PLUG ");
             }

    }

    public void fmTxCmdSetPowerLevel(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetPowerLevel ");
        Log.d(TAG, "  fmTxCmdSetPowerLevel ( status: " + status + " )");
    }



        public void fmTxCmdEnableRds(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdEnableRds ");
        Log.d(TAG, "  fmTxCmdEnableRds ( status: " + status + " )");


        Intent intentTxEnableRds = new Intent(FmRadioIntent.FM_TX_ENABLE_RSD_ACTION);
        intentTxEnableRds.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentTxEnableRds, FMRX_PERM);

    }


        public void fmTxCmdDisableRds(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdDisableRds ");
        Log.d(TAG, "  fmTxCmdDisableRds (status: " + status + " )");

        Intent intentTxDisableRds = new Intent(FmRadioIntent.FM_TX_DISABLE_RSD_ACTION);
        intentTxDisableRds.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentTxDisableRds, FMRX_PERM);

    }

        public void fmTxCmdTune(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmRxCmdTune ");
        Log.d(TAG, "  fmRxCmdTune (status: " + status + " )");

            Intent intentTxTune = new Intent(
                    FmRadioIntent.FM_TX_TUNE_ACTION);
            intentTxTune.putExtra(FmRadioIntent.TUNED_FREQUENCY, value);
            intentTxTune.putExtra(FmRadioIntent.STATUS, status);
            mContext.sendBroadcast(intentTxTune, FMRX_PERM);

    }


        public void fmTxCmdGetTunedFrequency(JFmTx context, JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetTunedFrequency ");
        Log.d(TAG, "  fmTxCmdGetTunedFrequency ( status: " + status + " )");

    }



        public void fmTxCmdSetRdsTransmissionMode(JFmTx context,JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsTransmissionMode ");
        Log.d(TAG, "  fmTxCmdSetRdsTransmissionMode ( status: " + status + " )");


            Intent txMode = new Intent(
                    FmRadioIntent.FM_TX_SET_TRANSMISSION_MODE_ACTION);
            txMode.putExtra(FmRadioIntent.TX_MODE, value);
            txMode.putExtra(FmRadioIntent.STATUS, status);
            mContext.sendBroadcast(txMode, FMRX_PERM);


    }


        public void fmTxCmdGetRdsTransmissionMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsTransmissionMode ");
        Log.d(TAG, "  fmTxCmdGetRdsTransmissionMode ( status: " + status + " ) value: " + value + ")");

    }


        public void fmTxCmdSetMonoStereoMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetMonoStereoMode ");
        Log.d(TAG, "  fmTxCmdSetMonoStereoMode ( status: " + status + " )");

        Intent txModeMonoStereo = new Intent(
                FmRadioIntent.FM_TX_SET_MONO_STEREO_MODE_ACTION);
        txModeMonoStereo.putExtra(FmRadioIntent.MODE_MONO_STEREO, value);
        txModeMonoStereo.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(txModeMonoStereo, FMRX_PERM);


    }

        public void fmTxCmdGetMonoStereoMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetMonoStereoMode ");
        Log.d(TAG, "  fmTxCmdGetMonoStereoMode ( status: " + status + " ) value: " + value + ")");
    }

        public void fmTxCmdSetPreEmphasisFilter(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetPreEmphasisFilter ");
        Log.d(TAG, "  fmTxCmdSetPreEmphasisFilter ( status: " + status + " )");

        Log.i(TAG, "StubFmService:Calling txGetPreEmphasisFilter().. ");

    }

        public void fmTxCmdGetPreEmphasisFilter(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetPreEmphasisFilter ");
        Log.d(TAG, "  fmTxCmdGetPreEmphasisFilter ( status: " + status + " ) value: " + value + ")");
    }


        public void fmTxCmdSetMuteMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetMuteMode ");
        Log.d(TAG, "  fmTxCmdSetMuteMode ( status: " + status + " )");
        Intent intentMute = new Intent(FmRadioIntent.FM_TX_SET_MUTE_MODE_ACTION);
        intentMute.putExtra(FmRadioIntent.TX_REPERTOIRE, value);
        intentMute.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentMute, FMRX_PERM);

    }


        public void fmTxCmdGetMuteMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetMuteMode ");
        Log.d(TAG, "  fmTxCmdGetMuteMode ( status: " + status + " ) value: " + value + ")");

    }



        public void fmTxCmdSetRdsAfCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsAfCode ");
        Log.d(TAG, "  fmTxCmdSetRdsAfCode ( status: " + status + " )");
    }


        public void fmTxCmdGetRdsAfCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsAfCode ");
        Log.d(TAG, "  fmTxCmdGetRdsAfCode ( status: " + status + " ) value: " + value + ")");

    }



        public void fmTxCmdSetRdsPiCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsPiCode ");
        Log.d(TAG, "  fmTxCmdSetRdsPiCode (status: " + status + " )");

    }



        public void fmTxCmdGetRdsPiCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsPiCode ");
        Log.d(TAG, "  fmTxCmdGetRdsPiCode ( status: " + status + " ) value: " + value + ")");

    }




        public void fmTxCmdSetRdsPtyCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsPtyCode ");
        Log.d(TAG, "  fmTxCmdSetRdsPtyCode ( status: " + status + " )");

    }


        public void fmTxCmdGetRdsPtyCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsPtyCode ");
        Log.d(TAG, "  fmTxCmdGetRdsPtyCode ( status: " + status + " ) value: " + value + ")");

    }



        public void fmTxCmdSetRdsTextRepertoire(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsTextRepertoire ");
        Log.d(TAG, "  fmTxCmdSetRdsTextRepertoire ( status: " + status + " )");
        Intent intentRepertoire = new Intent(FmRadioIntent. FM_TX_SET_RDS_TEXT_REPERTOIRE_ACTION);
        intentRepertoire.putExtra(FmRadioIntent.TX_REPERTOIRE, value);
        intentRepertoire.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentRepertoire, FMRX_PERM);

    }


        public void fmTxCmdGetRdsTextRepertoire(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsTextRepertoire ");
        Log.d(TAG, "  fmTxCmdGetRdsTextRepertoire ( status: " + status + " ) value: " + value + ")");

    }


        public void fmTxCmdSetRdsPsDisplayMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsPsDisplayMode ");
        Log.d(TAG, "  fmTxCmdSetRdsPsDisplayMode ( status: " + status + " )");
        Intent intentRdsPsDisplayMode = new Intent(
                FmRadioIntent.FM_TX_PS_DISPLAY_MODE_ACTION);
        intentRdsPsDisplayMode.putExtra(FmRadioIntent.DISPLAY_MODE, value);
        intentRdsPsDisplayMode.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentRdsPsDisplayMode, FMRX_PERM);

    }



        public void fmTxCmdGetRdsPsDisplayMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsPsDisplayMode ");
        Log.d(TAG, "  fmTxCmdGetRdsPsDisplayMode ( status: " + status + " ) value: " + value + ")");

    }


        public void fmTxCmdSetRdsPsScrollSpeed(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsPsScrollSpeed ");
        Log.d(TAG, "  fmTxCmdSetRdsPsScrollSpeed ( status: " + status + " )");

    }


        public void fmTxCmdGetRdsPsScrollSpeed(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsPsScrollSpeed ");
        Log.d(TAG, "  fmTxCmdGetRdsPsScrollSpeed ( status: " + status + " ) value: " + value + ")");

    }



        public void fmTxCmdSetRdsTextRtMsg(JFmTx context,  JFmTxStatus status,
            int msgType,int msgLen,byte[]msg) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsTextRtMsg ");
        Log.d(TAG, "  fmTxCmdSetRdsTextRtMsg ( status: " + status + " )");
    }



        public void fmTxCmdGetRdsTextRtMsg(JFmTx context,  JFmTxStatus status,
            int msgType,int msgLen,byte[]msg) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsTextRtMsg ");
        Log.d(TAG, "  fmTxCmdgetRdsTextRtMsg ( status: " + status + "msgType: " + msgType +
            "msgLen: " + msgLen + ")");
    }



        public void fmTxCmdSetRdsTextPsMsg(JFmTx context,  JFmTxStatus status,
            int msgLen,byte[] msg) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsTextPsMsg ");
        Log.d(TAG, "  fmTxCmdSetRdsTextPsMsg ( status: " + status + " )");
        Intent intentPsMsg = new Intent(
                FmRadioIntent.FM_TX_SET_RDS_TEXT_PS_MSG_ACTION);

        String psString = new String(msg);
        intentPsMsg.putExtra(FmRadioIntent.PS_MSG, psString);
        intentPsMsg.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentPsMsg, FMRX_PERM);

    }



        public void fmTxCmdGetRdsTextPsMsg(JFmTx context,  JFmTxStatus status,
            int msgLen,byte[] msg) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsTextPsMsg ");
        Log.d(TAG, "  fmTxCmdGetRdsTextPsMsg ( status: " + status +
            "msgLen: " + msgLen + ")");
    }



/*
        public void fmTxCmdGetRdsTransmittedGroupsMask(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsTransmittedGroupsMask ");
        Log.d(TAG, "  fmTxCmdGetRdsTransmittedGroupsMask ( status: " + status + " ) value: " + value + ")");


    }
*/

        public void fmTxCmdSetRdsTrafficCodes(JFmTx context,  JFmTxStatus status,
            int tacode,int tpCode) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsTrafficCodes ");
        Log.d(TAG, "  fmTxCmdSetRdsTrafficCodes ( status: " + status + " )");

    }


        public void fmTxCmdGetRdsTrafficCodes(JFmTx context,  JFmTxStatus status,
            int tacode,int tpCode) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsTrafficCodes ");
        Log.d(TAG, "  fmTxCmdGetRdsTrafficCodes ( status: " + status + " ) tpCode: " +tpCode+ " tacode: " + tacode );
    }


        public void fmTxCmdSetRdsMusicSpeechFlag(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsMusicSpeechFlag ");
        Log.d(TAG, "  fmTxCmdSetRdsMusicSpeechFlag ( status: " + status + " )");


        Intent intentMusicSpeechFlag = new Intent(
                FmRadioIntent.FM_TX_SET_RDS_MUSIC_SPEECH_FLAG_ACTION);
        intentMusicSpeechFlag.putExtra(FmRadioIntent.MUSIC_SPEECH_FLAG, value);
        intentMusicSpeechFlag.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentMusicSpeechFlag, FMRX_PERM);

    }



        public void fmTxCmdGetRdsMusicSpeechFlag(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsMusicSpeechFlag ");
        Log.d(TAG, "  fmTxCmdGetRdsMusicSpeechFlag ( status: " + status + " ) value: " + value + ")");

    }




        public void fmTxCmdChangeAudioSource(JFmTx context,  JFmTxStatus status,
            JFmCcmVacUnavailResourceList ccmVacUnavailResourceList) {

        Log.i(TAG, "StubFmService:fmTxCmdChangeAudioSource ");
        Log.d(TAG, "  fmTxCmdChangeAudioSource ( status: " + status + " )");
    }
   /*
        public void fmTxCmdChangeDigitalAudioConfiguration(JFmTx context,  JFmTxStatus status,
            JFmCcmVacUnavailResourceList ccmVacUnavailResourceList) {

        Log.i(TAG, "StubFmService:fmTxCmdChangeDigitalAudioConfiguration ");
        Log.d(TAG, "  fmTxCmdChangeDigitalAudioConfiguration ( command: , status:  )" + command
                );
    }
*/



        public void fmTxCmdSetInterruptMask(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetInterruptMask ");
        Log.d(TAG, "  fmTxCmdSetInterruptMask ( status: " + status + " )");
    }


        public void fmTxCmdGetInterruptMask(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetInterruptMask ");
        Log.d(TAG, "  fmTxCmdGetInterruptMask ( status: " + status + " ) value: " + value + ")");
    }


        public void fmTxCmdSetRdsPsDisplaySpeed(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsPsDisplaySpeed ");
        Log.d(TAG, "  fmTxCmdSetRdsPsDisplaySpeed ( status: " + status + " )");

    }



        public void fmTxCmdGetRdsPsDisplaySpeed(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsPsDisplaySpeed ");
        Log.d(TAG, "  fmTxCmdGetRdsPsDisplaySpeed ( status: " + status + " ) value: " + value + ")");
    }


        public void fmTxCmdReadRdsRawData(JFmTx context,  JFmTxStatus status,
            int len,byte[] msg) {

        Log.i(TAG, "StubFmService:fmTxCmdReadRdsRawData ");
        Log.d(TAG, "  fmTxCmdReadRdsRawData ( status: " + status + " ) len: " + len + ")");
    }




        public void fmTxCmdSetRdsTransmittedMask(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsTransmittedMask ");
        Log.d(TAG, "  fmTxCmdSetRdsTransmittedMask (  status: " + status + " ) value: " + value + ")");


        Intent intentRdsTxGrpMask = new Intent(
                FmRadioIntent.FM_TX_SET_RDS_TRANSMISSION_GROUPMASK_ACTION);

        intentRdsTxGrpMask.putExtra(FmRadioIntent.RDS_GRP_MASK, value);
        intentRdsTxGrpMask.putExtra(FmRadioIntent.STATUS, status);
        mContext.sendBroadcast(intentRdsTxGrpMask, FMRX_PERM);


    }



        public void fmTxCmdGetRdsTransmittedMask(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsTransmittedMask ");
        Log.d(TAG, "  fmTxCmdGetRdsTransmittedMask ( status: " + status + " )");
    }



        public void fmTxCmdChangeDigitalAudioConfiguration(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdChangeDigitalAudioConfiguration ");
        Log.d(TAG, "  fmTxCmdChangeDigitalAudioConfiguration ( status: " + status + " )");
    }



        public void fmTxCmdSetRdsExtendedCountryCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsExtendedCountryCode ");
        Log.d(TAG, "  fmTxCmdSetRdsExtendedCountryCode ( status: " + status + " )");

    }



        public void fmTxCmdGetRdsExtendedCountryCode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsExtendedCountryCode ");
        Log.d(TAG, "  fmTxCmdGetRdsExtendedCountryCode (  status: " + status + " ) value: " + value + ")");
    }


        public void fmTxCmdWriteRdsRawData(JFmTx context,  JFmTxStatus status,
            int len, byte[] msg) {

        Log.i(TAG, "StubFmService:fmTxCmdWriteRdsRawData ");
        Log.d(TAG, "  fmTxCmdWriteRdsRawData ( status: " + status + " )");
    }



        public void fmTxCmdSetRdsPsDispalyMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdSetRdsPsDispalyMode ");
        Log.d(TAG, "  fmTxCmdSetRdsPsDispalyMode ( status: " + status + " )");
    }



        public void fmTxCmdGetRdsPsDispalyMode(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetRdsPsDispalyMode ");
        Log.d(TAG, "  fmTxCmdGetRdsPsDispalyMode (  status: " + status + " ) value: " + value + ")");
        }


        public void fmTxCmdGetPowerLevel(JFmTx context,  JFmTxStatus status,
            long value) {

        Log.i(TAG, "StubFmService:fmTxCmdGetPowerLevel ");
        Log.d(TAG, "  fmTxCmdGetPowerLevel (  status: " + status + " ) value: " + value + ")");
    }

    /*Play Mp3 file*/
    private void musicPlay(Context context)
    {
               MediaPlayer mp = MediaPlayer.create(context,
                    R.raw.ding);
            mp.start();
            // react on the end of the music-file:
            mp.setOnCompletionListener(new OnCompletionListener(){

                public void onCompletion(MediaPlayer mp) {
                    // File has ended !!!
                    cleanupPlayer(mp);
                }
            });
    }


       private void cleanupPlayer(MediaPlayer mp) {
          if (mp != null) {
             try {
                mp.stop();
                mp.release();
             } catch (IllegalStateException ex) {
                Log.w(TAG, "MediaPlayer IllegalStateException: "+ex);
             }
          }
       }

    /* Broadcast receiver for the HEDASET_PLUG broadcast intent.*/
    private final BroadcastReceiver mHeadsetPlugReceiver = new BroadcastReceiver() {

    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, " mHeadsetPlugReceiver--->onReceive");
             int state = intent.getIntExtra("state", 0);
             if (mRxState == STATE_ENABLED ){
                int on =1;
                if (state == 1) {
                   Log.i(TAG, " mHeadsetPlugReceiver--->Headset plugged");
                   /* L25 Specific */
                   /* HEADSET IS OR HAS BEEN CONNECTED */
                 /*Play a ding mp3 file to force the new routing to take effect,
                   as FM analog playback is not associated with any PCM stream to trigger
                   the routing change based on headset detection*/
                 //musicPlay(context);
                } else {
                  Log.i(TAG, " mHeadsetPlugReceiver--->Headset unplugged");
                }
                /* L27 Specific */

                enableRx(on-1);
                enableRx(on);
              }

       }
    };


    /* Broadcast receiver for the ACTION_MEDIA_BUTTON broadcast intent.*/

    private class MediaButtonBroadcastReceiver extends BroadcastReceiver {
       @Override
       public void onReceive(Context context, Intent intent) {
    Log.i(TAG, " MediaButtonBroadcastReceiver--->onReceive");
    String intentAction = intent.getAction();
    if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction))
    {
        KeyEvent event = (KeyEvent)
        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

        if (event == null) {
            return;
        }

        int keycode = event.getKeyCode();
        int action = event.getAction();

        boolean volState;

        synchronized (mVolumeSynchronizationLock) {
        volState = mVolState;
        }

        int mVolume = mAudioManager
        .getStreamVolume(AudioManager.STREAM_MUSIC);
        // Convert the stream volume to the FM specific volume.
        mVolume = (mVolume * FM_MAX_VOLUME)/ (mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        Log.i(TAG, "  Audio Manager mVolume  " + mVolume);

        switch (keycode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            mVolume ++;
            break;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
            mVolume --;
            break;
        }

        if (action == KeyEvent.ACTION_DOWN) {
            if (mVolState) {

                    if (!rxSetVolume(mVolume)) {
                    Log.i(TAG, "Not able, to set volume ");
                    }
                    Log.i(TAG, "send intent for UI updation ");
                    /* Send Intent to App for UI updation */
                    Intent intentMasterVolume = new Intent(
                    FmRadioIntent.MASTER_VOLUME_CHANGED_ACTION);
                    intentMasterVolume.putExtra(FmRadioIntent.MASTER_VOLUME,mVolume);
                    mContext.sendBroadcast(intentMasterVolume, FMRX_PERM);

                } // mVolState
                else {
                    Log.i(TAG, "previous volume set not complete ");
                }
        }

        if (isOrderedBroadcast()) {
        abortBroadcast();
        }

        }

    }

    }


    private class SmsBroadcastReceiver extends BroadcastReceiver {
       public void onReceive(Context context, Intent intent) {
        Log.i(TAG, " SmsBroadcastReceiver--->onReceive");
        rxSetMuteMode(FM_MUTE);

          }
    }


    // Receiver of the Intent Broadcasted by AudioService
    private final BroadcastReceiver mFmRxIntentReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String fmRxAction = intent.getAction();
            boolean setVolume = false;
            Log.d(TAG, " mFmRxIntentReceiver--->fmRxAction" + fmRxAction);
            int volType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC);




            /*
             * Intent received when the volume has been changed from the master
             * volume control keys. Convert it to the FM specific volume and set
             * the volume.
             */
            if (fmRxAction.equals(AudioManager.VOLUME_CHANGED_ACTION)) {
                Log.i(TAG, " AUDIOMANGER_VOLUME_CHANGED_ACTION    ");
                if ((mRxState == STATE_ENABLED)&&
                    (volType == AudioManager.STREAM_MUSIC) ){
                    Log.i(TAG, " AUDIOMANGER_VOLUME_CHANGED_ACTION ");
                    setVolume = true;
                } // mRxState & volType

                else {
                    Log.i(TAG,"VOLUME_CHANGED_ACTION Intent is Ignored, as FM is not yet Enabled");

                }
            } // volume _change

            /*
             * Intent recived if the video or Music playback has started.
             * Disable the FM and let the music/video playback proceed.
             */
            if (fmRxAction.equals(FM_PAUSE_CMD) ) {
                Log.i(TAG, FM_PAUSE_CMD);
                if (rxIsEnabled() == true) {
                    pauseFm();
                }
            }

            /*
             * Intent recived if the video or Music playback has started.
             * Disable the FM and let the music/video playback proceed.
             */
            if (fmRxAction.equals(FM_RESUME_CMD)) {
                Log.i(TAG, FM_RESUME_CMD);
                // resume playback only if FM was playing
                // when the call was answered
                resumeFm();

            }
            /*
             * Intent recived if the Notification sound playback has started.
             * Mute the FM and let the notification sound playback proceed.
             */
            if (fmRxAction.equals(FM_MUTE_CMD) ) {
                Log.i(TAG, FM_MUTE_CMD);
                if (rxIsEnabled() == true)
                {
                    mIsFmMuted = true;
                    if (MAKE_FM_APIS_BLOCKING == true) {
                    Log.i(TAG, " Blocking version");
                    rxSetMuteMode(FM_MUTE);
                    } else {
                    Log.i(TAG, " Non blocking version");
                    rxSetMuteMode_nb(FM_MUTE);
                    }
                }

            }

            /*
             * Intent recived if the Notification sound playback has stopped.
             * UnMute the FM and let the notification sound playback proceed.
             */
            if (fmRxAction.equals(FM_UNMUTE_CMD) ) {
                Log.i(TAG, FM_UNMUTE_CMD);
                if (rxIsEnabled() == true)
                {
                    mIsFmMuted = false ;
                    if (MAKE_FM_APIS_BLOCKING == true) {
                    Log.i(TAG, " Blocking version");
                    rxSetMuteMode(FM_NOT_MUTE);
                    } else {
                    Log.i(TAG, " Non blocking version");
                    rxSetMuteMode_nb(FM_NOT_MUTE);
                    }
                }

            }

            /*
             * Intent recived if the Alarm alert is recieved sound playback has stopped.
             * UnMute the FM and let the Alarm sound playback proceed.
             */

            if (fmRxAction.equals(ALARM_ALERT_ACTION)) {
                Log.i(TAG, ALARM_ALERT_ACTION);
                if (rxIsEnabled() == true)
                {
                    mIsFmMuted = true;
                    if (MAKE_FM_APIS_BLOCKING == true) {
                    Log.i(TAG, " Blocking version");
                    rxSetMuteMode(FM_MUTE);
                    } else {
                    Log.i(TAG, " Non blocking version");
                    rxSetMuteMode_nb(FM_MUTE);
                                enableRx(0);
                    }

                }
            }

                   if (fmRxAction.equals(MUSIC_PAUSE_ACTION)) {
                         Log.i(TAG, MUSIC_PAUSE_ACTION);
                         String cmd = intent.getStringExtra("command");
                         if (CMDPAUSE.equals(cmd)) {
                              if (rxIsEnabled() == true) {
                                pauseFm();
                              Log.i(TAG, "FM app exit");
                             }
                         }
                   }
            /*
             * Intent recived if the Alarm alert has stopped sounding for any reason
             * UnMute the FM and let the Alarm sound playback proceed.
             */

            if (fmRxAction.equals(ALARM_DONE_ACTION) ) {
                Log.i(TAG, ALARM_DONE_ACTION );
                if (rxIsEnabled() == true)
                {
                    mIsFmMuted = false ;
                    if (MAKE_FM_APIS_BLOCKING == true) {
                    Log.i(TAG, " Blocking version");
                    rxSetMuteMode(FM_NOT_MUTE);
                    } else {
                    Log.i(TAG, " Non blocking version");
                    rxSetMuteMode_nb(FM_NOT_MUTE);
                                enableRx(1);
                    }

                }
            }


            if (fmRxAction.equals(FM_RESTORE_VALUES)) {
                Log.e(TAG, "FM_RESTORE_VALUES intent received");

                setVolume = true;

                if (mFmPauseResume == STATE_RESUME) {

                    if (MAKE_FM_APIS_BLOCKING == true) {
                    Log.d(TAG, " Blocking version");
                    rxSetBand(mCurrentBand);
                    rxSetMuteMode(mCurrentMuteMode);
                    if(mCurrentRdsState)
                    {
                    Log.d(TAG, " restore mCurrentRdsState");
                    rxEnableRds();
                    }

                } else {

                    Log.d(TAG, " Non blocking version");
                    rxSetBand_nb(mCurrentBand);
                    rxSetMuteMode_nb(mCurrentMuteMode);
                    if(mCurrentRdsState)
                    {
                    Log.d(TAG, " restore mCurrentRdsState");
                    rxEnableRds_nb();
                    }
                    }
                    rxTune_nb(mCurrentFrequency);
                    //rxEnableAudioRouting();
                    mFmPauseResume = STATE_DEFAULT;

                }
            }

            if (setVolume) {
                boolean volState;

                synchronized (mVolumeSynchronizationLock) {
                    volState = mVolState;
                }
                if (mVolState) {
                    int mVolume = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
                    if (mVolume == 0) {
                        Log.i(TAG, "no volume setting in intent");
                        // read the current volume
                        mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    }

                Log.i(TAG, "  Audio Manager mVolume  " + mVolume);
                    // Convert the stream volume to the FM specific volume.
                    mVolume = (mVolume * FM_MAX_VOLUME)/ (mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

                    Log.i(TAG, " mVolume  " + mVolume);
                    if (!rxSetVolume(mVolume)) {
                    Log.i(TAG, "Not able, to set volume ");
                    }
                    Log.i(TAG, "send intent for UI updation ");
                    /* Send Intent to App for UI updation */
                    Intent intentMasterVolume = new Intent(
                            FmRadioIntent.MASTER_VOLUME_CHANGED_ACTION);
                intentMasterVolume.putExtra(FmRadioIntent.MASTER_VOLUME,mVolume);
                    mContext.sendBroadcast(intentMasterVolume, FMRX_PERM);

                } // mVolState
                else {
                Log.i(TAG, "previous volume set not complete ");
                }
            }

        }
    };

    private  void updateEnableConfiguration() {

            mRxState = STATE_ENABLED;
        Log.d(TAG, "FM RX powered on mRxState " + mRxState);
        Log.d(TAG, "StubFmService:sending intent FM_ENABLED_ACTION");

        /*
         * Tell the Audio Hardware interface that FM is enabled, so that routing
         * happens appropriately
         */
         int on = 1;
         enableRx(on);
         /*
          * Previously when FM playback use to happen the wake lock was present in kernel
          * which used to prevent device into suspend. And FM playback used to work
          * Now, the wake lock has been removed and the FM service will not control
          * by acquiring wake lock and releasing once FM is off
          */
	 if (mWakeLock != null) {
		 if (!mWakeLock.isHeld()) {
			 mWakeLock.acquire();
		 } else
			 Log.e(TAG," Wake lock is already held");
	 }
    }

        private  void enableIntent(JFmRxStatus status) {
            Intent intentEnable = new Intent(FmRadioIntent.FM_ENABLED_ACTION);
            intentEnable.putExtra(FmRadioIntent.STATUS, status);
            mContext.sendBroadcast(intentEnable, FMRX_PERM);
        }

    /***************************************************************
     *  findAndPrintFromLookup():
     *
     ****************************************************************/
    public String findFromLookupTable(byte[] indexArray,
            JFmRx.JFmRxRepertoire repertoire) {
        StringBuilder sb = new StringBuilder("");

        Log.i(TAG, "findFromLookupTable");
        Log.i(TAG, "Repertoire= " + repertoire);

        switch (repertoire) {
        case FMC_RDS_REPERTOIRE_G0_CODE_TABLE:
            Log.i(TAG, "FMC_RDS_REPERTOIRE_G0_CODE_TABLE");

            for (int i = 0; i < indexArray.length; i++) {
                int msb = (indexArray[i] & 0xF0) >> 4;
                int lsb = (indexArray[i] & 0x0F);
                sb.append(lookUpTable_G0[msb][lsb]);
            }
            break;

        case FMC_RDS_REPERTOIRE_G1_CODE_TABLE:
            Log.i(TAG, "FMC_RDS_REPERTOIRE_G1_CODE_TABLE");
            for (int i = 0; i < indexArray.length; i++) {
                int msb = (indexArray[i] & 0xF0) >> 4;
                int lsb = (indexArray[i] & 0x0F);
                sb.append(lookUpTable_G1[msb][lsb]);
            }
            break;

        case FMC_RDS_REPERTOIRE_G2_CODE_TABLE:
            Log.i(TAG, "FMC_RDS_REPERTOIRE_G2_CODE_TABLE");

            for (int i = 0; i < indexArray.length; i++) {
                int msb = (indexArray[i] & 0xF0) >> 4;
                int lsb = (indexArray[i] & 0x0F);
                sb.append(lookUpTable_G2[msb][lsb]);
            }
            break;

        default:
            Log.i(TAG, "Incorrect Repertoire received...");
            String convertedPsString = "???????????";
            break;
        }

        String convertedString = sb.toString();
        return convertedString;

    }




    private final void sendNotificationRX() {
        Log.i(TAG, "Sending FM running notification");

        // Pack up the values and broadcast them to everyone
        Intent FMplaybackIntent = new Intent(
                "android.intent.action.FM_PLAYBACK");
        NotificationManager mNotificationMgr = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence title = "FM Radio RX";
        Long lcurrentfreq;
        lcurrentfreq = new Long(mCurrentFrequency);
        CharSequence details = lcurrentfreq.toString();

        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                FMplaybackIntent, 0);
        Notification notification = new Notification();

        /* Currently making use of the icon in the service. when FM is made application Service
        we would use the APP icon  */
        notification.icon = R.drawable.rxradio;

        notification.setLatestEventInfo(mContext, title, details, intent);
        mNotificationMgr.notify(FM_RX_NOTIFICATION_ID, notification);

    }


    private final void sendNotificationTX() {
        Log.i(TAG, "Sending FM running notification");

        // Pack up the values and broadcast them to everyone
        Intent FMplaybackIntent = new Intent(
                "android.intent.action.FMTXRELAUNCH");
        NotificationManager mNotificationMgr = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence title = "FM Radio TX";

        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                FMplaybackIntent, 0);
        Notification notification = new Notification();
        /* Currently making use of the icon in the service. when FM is made application Service
        we would use the APP icon  */
        notification.icon = R.drawable.txradio;
        CharSequence emptychr = "";
        notification.setLatestEventInfo(mContext, title, emptychr, intent);
        mNotificationMgr.notify(FM_TX_NOTIFICATION_ID, notification);

    }

    private final void cancelNotification(int id) {
        Log.i(TAG, "Canceling FM  notification");
        NotificationManager mNotificationMgr = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // cancel notification since memory has been freed
        mNotificationMgr.cancel(id);

    }

}

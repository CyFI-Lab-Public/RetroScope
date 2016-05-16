/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.Phone;
/**
 * Ringer manager for the Phone app.
 */
public class Ringer {
    private static final String LOG_TAG = "Ringer";
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    private static final int PLAY_RING_ONCE = 1;
    private static final int STOP_RING = 3;

    private static final int VIBRATE_LENGTH = 1000; // ms
    private static final int PAUSE_LENGTH = 1000; // ms

    /** The singleton instance. */
    private static Ringer sInstance;

    // Uri for the ringtone.
    Uri mCustomRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;

    private final BluetoothManager mBluetoothManager;
    Ringtone mRingtone;
    Vibrator mVibrator;
    IPowerManager mPowerManager;
    volatile boolean mContinueVibrating;
    VibratorThread mVibratorThread;
    Context mContext;
    private Worker mRingThread;
    private Handler mRingHandler;
    private long mFirstRingEventTime = -1;
    private long mFirstRingStartTime = -1;

    /**
     * Initialize the singleton Ringer instance.
     * This is only done once, at startup, from PhoneApp.onCreate().
     */
    /* package */ static Ringer init(Context context, BluetoothManager bluetoothManager) {
        synchronized (Ringer.class) {
            if (sInstance == null) {
                sInstance = new Ringer(context, bluetoothManager);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private Ringer(Context context, BluetoothManager bluetoothManager) {
        mContext = context;
        mBluetoothManager = bluetoothManager;
        mPowerManager = IPowerManager.Stub.asInterface(
                ServiceManager.getService(Context.POWER_SERVICE));
        // We don't rely on getSystemService(Context.VIBRATOR_SERVICE) to make sure this
        // vibrator object will be isolated from others.
        mVibrator = new SystemVibrator(context);
    }

    /**
     * After a radio technology change, e.g. from CDMA to GSM or vice versa,
     * the Context of the Ringer has to be updated. This is done by that function.
     *
     * @parameter Phone, the new active phone for the appropriate radio
     * technology
     */
    void updateRingerContextAfterRadioTechnologyChange(Phone phone) {
        if(DBG) Log.d(LOG_TAG, "updateRingerContextAfterRadioTechnologyChange...");
        mContext = phone.getContext();
    }

    /**
     * @return true if we're playing a ringtone and/or vibrating
     *     to indicate that there's an incoming call.
     *     ("Ringing" here is used in the general sense.  If you literally
     *     need to know if we're playing a ringtone or vibrating, use
     *     isRingtonePlaying() or isVibrating() instead.)
     *
     * @see isVibrating
     * @see isRingtonePlaying
     */
    boolean isRinging() {
        synchronized (this) {
            return (isRingtonePlaying() || isVibrating());
        }
    }

    /**
     * @return true if the ringtone is playing
     * @see isVibrating
     * @see isRinging
     */
    private boolean isRingtonePlaying() {
        synchronized (this) {
            return (mRingtone != null && mRingtone.isPlaying()) ||
                    (mRingHandler != null && mRingHandler.hasMessages(PLAY_RING_ONCE));
        }
    }

    /**
     * @return true if we're vibrating in response to an incoming call
     * @see isVibrating
     * @see isRinging
     */
    private boolean isVibrating() {
        synchronized (this) {
            return (mVibratorThread != null);
        }
    }

    /**
     * Starts the ringtone and/or vibrator
     */
    void ring() {
        if (DBG) log("ring()...");

        synchronized (this) {
            try {
                if (mBluetoothManager.showBluetoothIndication()) {
                    mPowerManager.setAttentionLight(true, 0x000000ff);
                } else {
                    mPowerManager.setAttentionLight(true, 0x00ffffff);
                }
            } catch (RemoteException ex) {
                // the other end of this binder call is in the system process.
            }

            if (shouldVibrate() && mVibratorThread == null) {
                mContinueVibrating = true;
                mVibratorThread = new VibratorThread();
                if (DBG) log("- starting vibrator...");
                mVibratorThread.start();
            }
            AudioManager audioManager =
                    (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                if (DBG) log("skipping ring because volume is zero");
                return;
            }

            makeLooper();
            if (mFirstRingEventTime < 0) {
                mFirstRingEventTime = SystemClock.elapsedRealtime();
                mRingHandler.sendEmptyMessage(PLAY_RING_ONCE);
            } else {
                // For repeat rings, figure out by how much to delay
                // the ring so that it happens the correct amount of
                // time after the previous ring
                if (mFirstRingStartTime > 0) {
                    // Delay subsequent rings by the delta between event
                    // and play time of the first ring
                    if (DBG) {
                        log("delaying ring by " + (mFirstRingStartTime - mFirstRingEventTime));
                    }
                    mRingHandler.sendEmptyMessageDelayed(PLAY_RING_ONCE,
                            mFirstRingStartTime - mFirstRingEventTime);
                } else {
                    // We've gotten two ring events so far, but the ring
                    // still hasn't started. Reset the event time to the
                    // time of this event to maintain correct spacing.
                    mFirstRingEventTime = SystemClock.elapsedRealtime();
                }
            }
        }
    }

    boolean shouldVibrate() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if (CallFeaturesSetting.getVibrateWhenRinging(mContext)) {
            return ringerMode != AudioManager.RINGER_MODE_SILENT;
        } else {
            return ringerMode == AudioManager.RINGER_MODE_VIBRATE;
        }
    }

    /**
     * Stops the ringtone and/or vibrator if any of these are actually
     * ringing/vibrating.
     */
    void stopRing() {
        synchronized (this) {
            if (DBG) log("stopRing()...");

            try {
                mPowerManager.setAttentionLight(false, 0x00000000);
            } catch (RemoteException ex) {
                // the other end of this binder call is in the system process.
            }

            if (mRingHandler != null) {
                mRingHandler.removeCallbacksAndMessages(null);
                Message msg = mRingHandler.obtainMessage(STOP_RING);
                msg.obj = mRingtone;
                mRingHandler.sendMessage(msg);
                PhoneUtils.setAudioMode();
                mRingThread = null;
                mRingHandler = null;
                mRingtone = null;
                mFirstRingEventTime = -1;
                mFirstRingStartTime = -1;
            } else {
                if (DBG) log("- stopRing: null mRingHandler!");
            }

            if (mVibratorThread != null) {
                if (DBG) log("- stopRing: cleaning up vibrator thread...");
                mContinueVibrating = false;
                mVibratorThread = null;
            }
            // Also immediately cancel any vibration in progress.
            mVibrator.cancel();
        }
    }

    private class VibratorThread extends Thread {
        public void run() {
            while (mContinueVibrating) {
                mVibrator.vibrate(VIBRATE_LENGTH);
                SystemClock.sleep(VIBRATE_LENGTH + PAUSE_LENGTH);
            }
        }
    }
    private class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        public Looper getLooper() {
            return mLooper;
        }

        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }

        public void quit() {
            mLooper.quit();
        }
    }

    /**
     * Sets the ringtone uri in preparation for ringtone creation
     * in makeLooper().  This uri is defaulted to the phone-wide
     * default ringtone.
     */
    void setCustomRingtoneUri (Uri uri) {
        if (uri != null) {
            mCustomRingtoneUri = uri;
        }
    }

    private void makeLooper() {
        if (mRingThread == null) {
            mRingThread = new Worker("ringer");
            mRingHandler = new Handler(mRingThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Ringtone r = null;
                    switch (msg.what) {
                        case PLAY_RING_ONCE:
                            if (DBG) log("mRingHandler: PLAY_RING_ONCE...");
                            if (mRingtone == null && !hasMessages(STOP_RING)) {
                                // create the ringtone with the uri
                                if (DBG) log("creating ringtone: " + mCustomRingtoneUri);
                                r = RingtoneManager.getRingtone(mContext, mCustomRingtoneUri);
                                synchronized (Ringer.this) {
                                    if (!hasMessages(STOP_RING)) {
                                        mRingtone = r;
                                    }
                                }
                            }
                            r = mRingtone;
                            if (r != null && !hasMessages(STOP_RING) && !r.isPlaying()) {
                                PhoneUtils.setAudioMode();
                                r.play();
                                synchronized (Ringer.this) {
                                    if (mFirstRingStartTime < 0) {
                                        mFirstRingStartTime = SystemClock.elapsedRealtime();
                                    }
                                }
                            }
                            break;
                        case STOP_RING:
                            if (DBG) log("mRingHandler: STOP_RING...");
                            r = (Ringtone) msg.obj;
                            if (r != null) {
                                r.stop();
                            } else {
                                if (DBG) log("- STOP_RING with null ringtone!  msg = " + msg);
                            }
                            getLooper().quit();
                            break;
                    }
                }
            };
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}

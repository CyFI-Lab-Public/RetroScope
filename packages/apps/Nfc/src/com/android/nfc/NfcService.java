/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.nfc;

import com.android.nfc.DeviceHost.DeviceHostListener;
import com.android.nfc.DeviceHost.LlcpConnectionlessSocket;
import com.android.nfc.DeviceHost.LlcpServerSocket;
import com.android.nfc.DeviceHost.LlcpSocket;
import com.android.nfc.DeviceHost.NfcDepEndpoint;
import com.android.nfc.DeviceHost.TagEndpoint;
import com.android.nfc.handover.HandoverManager;
import com.android.nfc.cardemulation.AidRoutingManager;
import com.android.nfc.cardemulation.HostEmulationManager;
import com.android.nfc.cardemulation.RegisteredAidCache;
import com.android.nfc.dhimpl.NativeNfcManager;
import com.android.nfc.dhimpl.NativeNfcSecureElement;

import android.app.ActivityManager;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.nfc.ErrorCodes;
import android.nfc.FormatException;
import android.nfc.IAppCallback;
import android.nfc.INfcAdapter;
import android.nfc.INfcAdapterExtras;
import android.nfc.INfcCardEmulation;
import android.nfc.INfcTag;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TechListParcel;
import android.nfc.TransceiveResult;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.tech.Ndef;
import android.nfc.tech.TagTechnology;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

public class NfcService implements DeviceHostListener {
    private static final String ACTION_MASTER_CLEAR_NOTIFICATION = "android.intent.action.MASTER_CLEAR_NOTIFICATION";

    static final boolean DBG = false;
    static final String TAG = "NfcService";

    public static final String SERVICE_NAME = "nfc";

    /** Regular NFC permission */
    private static final String NFC_PERM = android.Manifest.permission.NFC;
    private static final String NFC_PERM_ERROR = "NFC permission required";

    /** NFC ADMIN permission - only for system apps */
    private static final String ADMIN_PERM = android.Manifest.permission.WRITE_SECURE_SETTINGS;
    private static final String ADMIN_PERM_ERROR = "WRITE_SECURE_SETTINGS permission required";

    public static final String PREF = "NfcServicePrefs";

    static final String PREF_NFC_ON = "nfc_on";
    static final boolean NFC_ON_DEFAULT = true;
    static final String PREF_NDEF_PUSH_ON = "ndef_push_on";
    static final boolean NDEF_PUSH_ON_DEFAULT = true;
    static final String PREF_FIRST_BEAM = "first_beam";
    static final String PREF_FIRST_BOOT = "first_boot";
    static final String PREF_AIRPLANE_OVERRIDE = "airplane_override";
    static final boolean SE_BROADCASTS_WITH_HCE = true;

    static final int MSG_NDEF_TAG = 0;
    static final int MSG_CARD_EMULATION = 1;
    static final int MSG_LLCP_LINK_ACTIVATION = 2;
    static final int MSG_LLCP_LINK_DEACTIVATED = 3;
    static final int MSG_TARGET_DESELECTED = 4;
    static final int MSG_MOCK_NDEF = 7;
    static final int MSG_SE_FIELD_ACTIVATED = 8;
    static final int MSG_SE_FIELD_DEACTIVATED = 9;
    static final int MSG_SE_APDU_RECEIVED = 10;
    static final int MSG_SE_EMV_CARD_REMOVAL = 11;
    static final int MSG_SE_MIFARE_ACCESS = 12;
    static final int MSG_SE_LISTEN_ACTIVATED = 13;
    static final int MSG_SE_LISTEN_DEACTIVATED = 14;
    static final int MSG_LLCP_LINK_FIRST_PACKET = 15;
    static final int MSG_ROUTE_AID = 16;
    static final int MSG_UNROUTE_AID = 17;
    static final int MSG_COMMIT_ROUTING = 18;

    static final int TASK_ENABLE = 1;
    static final int TASK_DISABLE = 2;
    static final int TASK_BOOT = 3;
    static final int TASK_EE_WIPE = 4;

    // Screen state, used by mScreenState
    static final int SCREEN_STATE_UNKNOWN = 0;
    static final int SCREEN_STATE_OFF = 1;
    static final int SCREEN_STATE_ON_LOCKED = 2;
    static final int SCREEN_STATE_ON_UNLOCKED = 3;

    // Copied from com.android.nfc_extras to avoid library dependency
    // Must keep in sync with com.android.nfc_extras
    static final int ROUTE_OFF = 1;
    static final int ROUTE_ON_WHEN_SCREEN_ON = 2;

    // Return values from NfcEe.open() - these are 1:1 mapped
    // to the thrown EE_EXCEPTION_ exceptions in nfc-extras.
    static final int EE_ERROR_IO = -1;
    static final int EE_ERROR_ALREADY_OPEN = -2;
    static final int EE_ERROR_INIT = -3;
    static final int EE_ERROR_LISTEN_MODE = -4;
    static final int EE_ERROR_EXT_FIELD = -5;
    static final int EE_ERROR_NFC_DISABLED = -6;

    /** minimum screen state that enables NFC polling (discovery) */
    static final int POLLING_MODE = SCREEN_STATE_ON_UNLOCKED;

    // Time to wait for NFC controller to initialize before watchdog
    // goes off. This time is chosen large, because firmware download
    // may be a part of initialization.
    static final int INIT_WATCHDOG_MS = 90000;

    // Time to wait for routing to be applied before watchdog
    // goes off
    static final int ROUTING_WATCHDOG_MS = 10000;

    // Amount of time to wait before closing the NFCEE connection
    // in a disable/shutdown scenario.
    static final int WAIT_FOR_NFCEE_OPERATIONS_MS = 5000;
    // Polling interval for waiting on NFCEE operations
    static final int WAIT_FOR_NFCEE_POLL_MS = 100;

    // Default delay used for presence checks
    static final int DEFAULT_PRESENCE_CHECK_DELAY = 125;

    // for use with playSound()
    public static final int SOUND_START = 0;
    public static final int SOUND_END = 1;
    public static final int SOUND_ERROR = 2;

    public static final String ACTION_RF_FIELD_ON_DETECTED =
        "com.android.nfc_extras.action.RF_FIELD_ON_DETECTED";
    public static final String ACTION_RF_FIELD_OFF_DETECTED =
        "com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED";
    public static final String ACTION_AID_SELECTED =
        "com.android.nfc_extras.action.AID_SELECTED";
    public static final String EXTRA_AID = "com.android.nfc_extras.extra.AID";

    public static final String ACTION_LLCP_UP =
            "com.android.nfc.action.LLCP_UP";

    public static final String ACTION_LLCP_DOWN =
            "com.android.nfc.action.LLCP_DOWN";

    public static final String ACTION_APDU_RECEIVED =
        "com.android.nfc_extras.action.APDU_RECEIVED";
    public static final String EXTRA_APDU_BYTES =
        "com.android.nfc_extras.extra.APDU_BYTES";

    public static final String ACTION_EMV_CARD_REMOVAL =
        "com.android.nfc_extras.action.EMV_CARD_REMOVAL";

    public static final String ACTION_MIFARE_ACCESS_DETECTED =
        "com.android.nfc_extras.action.MIFARE_ACCESS_DETECTED";
    public static final String EXTRA_MIFARE_BLOCK =
        "com.android.nfc_extras.extra.MIFARE_BLOCK";

    public static final String ACTION_SE_LISTEN_ACTIVATED =
            "com.android.nfc_extras.action.SE_LISTEN_ACTIVATED";
    public static final String ACTION_SE_LISTEN_DEACTIVATED =
            "com.android.nfc_extras.action.SE_LISTEN_DEACTIVATED";

    // NFC Execution Environment
    // fields below are protected by this
    private NativeNfcSecureElement mSecureElement;
    private OpenSecureElement mOpenEe;  // null when EE closed
    private final ReaderModeDeathRecipient mReaderModeDeathRecipient =
            new ReaderModeDeathRecipient();
    private int mEeRoutingState;  // contactless interface routing

    // fields below must be used only on the UI thread and therefore aren't synchronized
    boolean mP2pStarted = false;

    // fields below are used in multiple threads and protected by synchronized(this)
    final HashMap<Integer, Object> mObjectMap = new HashMap<Integer, Object>();
    // mSePackages holds packages that accessed the SE, but only for the owner user,
    // as SE access is not granted for non-owner users.
    HashSet<String> mSePackages = new HashSet<String>();
    int mScreenState;
    boolean mInProvisionMode; // whether we're in setup wizard and enabled NFC provisioning
    boolean mIsNdefPushEnabled;
    boolean mNfceeRouteEnabled;  // current Device Host state of NFC-EE routing
    boolean mNfcPollingEnabled;  // current Device Host state of NFC-C polling
    boolean mHostRouteEnabled;   // current Device Host state of host-based routing
    boolean mReaderModeEnabled;  // current Device Host state of reader mode
    ReaderModeParams mReaderModeParams;

    List<PackageInfo> mInstalledPackages; // cached version of installed packages

    // mState is protected by this, however it is only modified in onCreate()
    // and the default AsyncTask thread so it is read unprotected from that
    // thread
    int mState;  // one of NfcAdapter.STATE_ON, STATE_TURNING_ON, etc

    // fields below are final after onCreate()
    Context mContext;
    private DeviceHost mDeviceHost;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mPrefsEditor;
    private PowerManager.WakeLock mRoutingWakeLock;
    private PowerManager.WakeLock mEeWakeLock;

    int mStartSound;
    int mEndSound;
    int mErrorSound;
    SoundPool mSoundPool; // playback synchronized on this
    P2pLinkManager mP2pLinkManager;
    TagService mNfcTagService;
    NfcAdapterService mNfcAdapter;
    NfcAdapterExtrasService mExtrasService;
    CardEmulationService mCardEmulationService;
    boolean mIsAirplaneSensitive;
    boolean mIsAirplaneToggleable;
    boolean mIsDebugBuild;
    boolean mIsHceCapable;
    NfceeAccessControl mNfceeAccessControl;

    private NfcDispatcher mNfcDispatcher;
    private PowerManager mPowerManager;
    private KeyguardManager mKeyguard;
    private HandoverManager mHandoverManager;
    private ContentResolver mContentResolver;
    private RegisteredAidCache mAidCache;
    private HostEmulationManager mHostEmulationManager;
    private AidRoutingManager mAidRoutingManager;

    private static NfcService sService;

    public static void enforceAdminPerm(Context context) {
        context.enforceCallingOrSelfPermission(ADMIN_PERM, ADMIN_PERM_ERROR);
    }

    public static void validateUserId(int userId) {
        if (userId != UserHandle.getCallingUserId()) {
            throw new SecurityException("userId passed in it not the calling user.");
        }
    }

    public void enforceNfceeAdminPerm(String pkg) {
        if (pkg == null) {
            throw new SecurityException("caller must pass a package name");
        }
        mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);
        if (!mNfceeAccessControl.check(Binder.getCallingUid(), pkg)) {
            throw new SecurityException(NfceeAccessControl.NFCEE_ACCESS_PATH +
                    " denies NFCEE access to " + pkg);
        }
        if (UserHandle.getCallingUserId() != UserHandle.USER_OWNER) {
            throw new SecurityException("only the owner is allowed to call SE APIs");
        }
    }

    public static NfcService getInstance() {
        return sService;
    }

    @Override
    public void onRemoteEndpointDiscovered(TagEndpoint tag) {
        sendMessage(NfcService.MSG_NDEF_TAG, tag);
    }

    /**
     * Notifies transaction
     */
    @Override
    public void onCardEmulationDeselected() {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_TARGET_DESELECTED, null);
        }
    }

    /**
     * Notifies transaction
     */
    @Override
    public void onCardEmulationAidSelected(byte[] aid) {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_CARD_EMULATION, aid);
        }
    }

    /**
     * Notifies transaction
     */
    @Override
    public void onHostCardEmulationActivated() {
        if (mHostEmulationManager != null) {
            mHostEmulationManager.notifyHostEmulationActivated();
        }
    }

    @Override
    public void onHostCardEmulationData(byte[] data) {
        if (mHostEmulationManager != null) {
            mHostEmulationManager.notifyHostEmulationData(data);
        }
    }

    @Override
    public void onHostCardEmulationDeactivated() {
        if (mHostEmulationManager != null) {
            mHostEmulationManager.notifyNostEmulationDeactivated();
        }
    }

    /**
     * Notifies P2P Device detected, to activate LLCP link
     */
    @Override
    public void onLlcpLinkActivated(NfcDepEndpoint device) {
        sendMessage(NfcService.MSG_LLCP_LINK_ACTIVATION, device);
    }

    /**
     * Notifies P2P Device detected, to activate LLCP link
     */
    @Override
    public void onLlcpLinkDeactivated(NfcDepEndpoint device) {
        sendMessage(NfcService.MSG_LLCP_LINK_DEACTIVATED, device);
    }

    /**
     * Notifies P2P Device detected, first packet received over LLCP link
     */
    @Override
    public void onLlcpFirstPacketReceived(NfcDepEndpoint device) {
        sendMessage(NfcService.MSG_LLCP_LINK_FIRST_PACKET, device);
    }

    @Override
    public void onRemoteFieldActivated() {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_SE_FIELD_ACTIVATED, null);
        }
    }

    @Override
    public void onRemoteFieldDeactivated() {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_SE_FIELD_DEACTIVATED, null);
        }
    }

    @Override
    public void onSeListenActivated() {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_SE_LISTEN_ACTIVATED, null);
        }
    }

    @Override
    public void onSeListenDeactivated() {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_SE_LISTEN_DEACTIVATED, null);
        }
    }


    @Override
    public void onSeApduReceived(byte[] apdu) {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_SE_APDU_RECEIVED, apdu);
        }
    }

    @Override
    public void onSeEmvCardRemoval() {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_SE_EMV_CARD_REMOVAL, null);
        }
    }

    @Override
    public void onSeMifareAccess(byte[] block) {
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            sendMessage(NfcService.MSG_SE_MIFARE_ACCESS, block);
        }
    }

    final class ReaderModeParams {
        public int flags;
        public IAppCallback callback;
        public int presenceCheckDelay;
    }

    public NfcService(Application nfcApplication) {
        mNfcTagService = new TagService();
        mNfcAdapter = new NfcAdapterService();
        mExtrasService = new NfcAdapterExtrasService();
        mCardEmulationService = new CardEmulationService();

        Log.i(TAG, "Starting NFC service");

        sService = this;

        mContext = nfcApplication;
        mContentResolver = mContext.getContentResolver();
        mDeviceHost = new NativeNfcManager(mContext, this);

        mHandoverManager = new HandoverManager(mContext);
        boolean isNfcProvisioningEnabled = false;
        try {
            isNfcProvisioningEnabled = mContext.getResources().getBoolean(
                    R.bool.enable_nfc_provisioning);
        } catch (NotFoundException e) {
        }

        if (isNfcProvisioningEnabled) {
            mInProvisionMode = Settings.Secure.getInt(mContentResolver,
                    Settings.Global.DEVICE_PROVISIONED, 0) == 0;
        } else {
            mInProvisionMode = false;
        }

        mHandoverManager.setEnabled(!mInProvisionMode);
        mNfcDispatcher = new NfcDispatcher(mContext, mHandoverManager, mInProvisionMode);
        mP2pLinkManager = new P2pLinkManager(mContext, mHandoverManager,
                mDeviceHost.getDefaultLlcpMiu(), mDeviceHost.getDefaultLlcpRwSize());

        mSecureElement = new NativeNfcSecureElement(mContext);
        mEeRoutingState = ROUTE_OFF;

        mNfceeAccessControl = new NfceeAccessControl(mContext);

        mPrefs = mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        mPrefsEditor = mPrefs.edit();

        mState = NfcAdapter.STATE_OFF;
        mIsNdefPushEnabled = mPrefs.getBoolean(PREF_NDEF_PUSH_ON, NDEF_PUSH_ON_DEFAULT);

        mIsDebugBuild = "userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE);

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mRoutingWakeLock = mPowerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "NfcService:mRoutingWakeLock");
        mEeWakeLock = mPowerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "NfcService:mEeWakeLock");

        mKeyguard = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mScreenState = checkScreenState();

        ServiceManager.addService(SERVICE_NAME, mNfcAdapter);

        // Intents for all users
        IntentFilter filter = new IntentFilter(NativeNfcManager.INTERNAL_TARGET_DESELECTED_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        registerForAirplaneMode(filter);
        mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);

        PackageManager pm = mContext.getPackageManager();
        mIsHceCapable = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        if (mIsHceCapable) {
            mAidRoutingManager = new AidRoutingManager();
            mAidCache = new RegisteredAidCache(mContext, mAidRoutingManager);
            mHostEmulationManager = new HostEmulationManager(mContext, mAidCache);
        }
        if (!mIsHceCapable || SE_BROADCASTS_WITH_HCE) {
            IntentFilter ownerFilter = new IntentFilter(NativeNfcManager.INTERNAL_TARGET_DESELECTED_ACTION);
            ownerFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            ownerFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            ownerFilter.addAction(ACTION_MASTER_CLEAR_NOTIFICATION);

            mContext.registerReceiver(mOwnerReceiver, ownerFilter);

            ownerFilter = new IntentFilter();
            ownerFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            ownerFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            ownerFilter.addDataScheme("package");

            mContext.registerReceiver(mOwnerReceiver, ownerFilter);

            updatePackageCache();
        }

        new EnableDisableTask().execute(TASK_BOOT);  // do blocking boot tasks
    }

    void initSoundPool() {
        synchronized(this) {
            if (mSoundPool == null) {
                mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
                mStartSound = mSoundPool.load(mContext, R.raw.start, 1);
                mEndSound = mSoundPool.load(mContext, R.raw.end, 1);
                mErrorSound = mSoundPool.load(mContext, R.raw.error, 1);
            }
        }
    }

    void releaseSoundPool() {
        synchronized(this) {
            if (mSoundPool != null) {
                mSoundPool.release();
                mSoundPool = null;
            }
        }
    }

    void registerForAirplaneMode(IntentFilter filter) {
        final String airplaneModeRadios = Settings.System.getString(mContentResolver,
                Settings.Global.AIRPLANE_MODE_RADIOS);
        final String toggleableRadios = Settings.System.getString(mContentResolver,
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        mIsAirplaneSensitive = airplaneModeRadios == null ? true :
                airplaneModeRadios.contains(Settings.Global.RADIO_NFC);
        mIsAirplaneToggleable = toggleableRadios == null ? false :
            toggleableRadios.contains(Settings.Global.RADIO_NFC);

        if (mIsAirplaneSensitive) {
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        }
    }

    void updatePackageCache() {
        PackageManager pm = mContext.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0, UserHandle.USER_OWNER);
        synchronized (this) {
            mInstalledPackages = packages;
        }
    }

    int checkScreenState() {
        if (!mPowerManager.isScreenOn()) {
            return SCREEN_STATE_OFF;
        } else if (mKeyguard.isKeyguardLocked()) {
            return SCREEN_STATE_ON_LOCKED;
        } else {
            return SCREEN_STATE_ON_UNLOCKED;
        }
    }

    int doOpenSecureElementConnection() {
        mEeWakeLock.acquire();
        try {
            return mSecureElement.doOpenSecureElementConnection();
        } finally {
            mEeWakeLock.release();
        }
    }

    byte[] doTransceive(int handle, byte[] cmd) {
        mEeWakeLock.acquire();
        try {
            return doTransceiveNoLock(handle, cmd);
        } finally {
            mEeWakeLock.release();
        }
    }

    byte[] doTransceiveNoLock(int handle, byte[] cmd) {
        return mSecureElement.doTransceive(handle, cmd);
    }

    void doDisconnect(int handle) {
        mEeWakeLock.acquire();
        try {
            mSecureElement.doDisconnect(handle);
        } finally {
            mEeWakeLock.release();
        }
    }

    /**
     * Manages tasks that involve turning on/off the NFC controller.
     *
     * <p>All work that might turn the NFC adapter on or off must be done
     * through this task, to keep the handling of mState simple.
     * In other words, mState is only modified in these tasks (and we
     * don't need a lock to read it in these tasks).
     *
     * <p>These tasks are all done on the same AsyncTask background
     * thread, so they are serialized. Each task may temporarily transition
     * mState to STATE_TURNING_OFF or STATE_TURNING_ON, but must exit in
     * either STATE_ON or STATE_OFF. This way each task can be guaranteed
     * of starting in either STATE_OFF or STATE_ON, without needing to hold
     * NfcService.this for the entire task.
     *
     * <p>AsyncTask's are also implicitly queued. This is useful for corner
     * cases like turning airplane mode on while TASK_ENABLE is in progress.
     * The TASK_DISABLE triggered by airplane mode will be correctly executed
     * immediately after TASK_ENABLE is complete. This seems like the most sane
     * way to deal with these situations.
     *
     * <p>{@link #TASK_ENABLE} enables the NFC adapter, without changing
     * preferences
     * <p>{@link #TASK_DISABLE} disables the NFC adapter, without changing
     * preferences
     * <p>{@link #TASK_BOOT} does first boot work and may enable NFC
     * <p>{@link #TASK_EE_WIPE} wipes the Execution Environment, and in the
     * process may temporarily enable the NFC adapter
     */
    class EnableDisableTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... params) {
            // Sanity check mState
            switch (mState) {
                case NfcAdapter.STATE_TURNING_OFF:
                case NfcAdapter.STATE_TURNING_ON:
                    Log.e(TAG, "Processing EnableDisable task " + params[0] + " from bad state " +
                            mState);
                    return null;
            }

            /* AsyncTask sets this thread to THREAD_PRIORITY_BACKGROUND,
             * override with the default. THREAD_PRIORITY_BACKGROUND causes
             * us to service software I2C too slow for firmware download
             * with the NXP PN544.
             * TODO: move this to the DAL I2C layer in libnfc-nxp, since this
             * problem only occurs on I2C platforms using PN544
             */
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

            switch (params[0].intValue()) {
                case TASK_ENABLE:
                    enableInternal();
                    break;
                case TASK_DISABLE:
                    disableInternal();
                    break;
                case TASK_BOOT:
                    Log.d(TAG,"checking on firmware download");
                    boolean airplaneOverride = mPrefs.getBoolean(PREF_AIRPLANE_OVERRIDE, false);
                    if (mPrefs.getBoolean(PREF_NFC_ON, NFC_ON_DEFAULT) &&
                            (!mIsAirplaneSensitive || !isAirplaneModeOn() || airplaneOverride)) {
                        Log.d(TAG,"NFC is on. Doing normal stuff");
                        enableInternal();
                    } else {
                        Log.d(TAG,"NFC is off.  Checking firmware version");
                        mDeviceHost.checkFirmware();
                        // Build initial AID cache even if NFC is off
                        if (mIsHceCapable) {
                            mAidCache.invalidateCache(ActivityManager.getCurrentUser());
                        }
                    }
                    if (mPrefs.getBoolean(PREF_FIRST_BOOT, true)) {
                        Log.i(TAG, "First Boot");
                        mPrefsEditor.putBoolean(PREF_FIRST_BOOT, false);
                        mPrefsEditor.apply();
                        executeEeWipe();
                    }
                    break;
                case TASK_EE_WIPE:
                    executeEeWipe();
                    break;
            }

            // Restore default AsyncTask priority
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return null;
        }

        /**
         * Enable NFC adapter functions.
         * Does not toggle preferences.
         */
        boolean enableInternal() {
            if (mState == NfcAdapter.STATE_ON) {
                return true;
            }
            Log.i(TAG, "Enabling NFC");
            updateState(NfcAdapter.STATE_TURNING_ON);

            WatchDogThread watchDog = new WatchDogThread("enableInternal", INIT_WATCHDOG_MS);
            watchDog.start();
            try {
                mRoutingWakeLock.acquire();
                try {
                    if (!mDeviceHost.initialize()) {
                        Log.w(TAG, "Error enabling NFC");
                        updateState(NfcAdapter.STATE_OFF);
                        return false;
                    }
                } finally {
                    mRoutingWakeLock.release();
                }
            } finally {
                watchDog.cancel();
            }

            if (mIsHceCapable) {
                // Generate the initial card emulation routing table
                mAidCache.onNfcEnabled();
            }

            synchronized(NfcService.this) {
                mObjectMap.clear();
                mP2pLinkManager.enableDisable(mIsNdefPushEnabled, true);
                updateState(NfcAdapter.STATE_ON);
            }

            initSoundPool();

            /* Start polling loop */

            applyRouting(true);
            return true;
        }

        /**
         * Disable all NFC adapter functions.
         * Does not toggle preferences.
         */
        boolean disableInternal() {
            if (mState == NfcAdapter.STATE_OFF) {
                return true;
            }
            Log.i(TAG, "Disabling NFC");
            updateState(NfcAdapter.STATE_TURNING_OFF);

            /* Sometimes mDeviceHost.deinitialize() hangs, use a watch-dog.
             * Implemented with a new thread (instead of a Handler or AsyncTask),
             * because the UI Thread and AsyncTask thread-pools can also get hung
             * when the NFC controller stops responding */
            WatchDogThread watchDog = new WatchDogThread("disableInternal", ROUTING_WATCHDOG_MS);
            watchDog.start();

            if (mIsHceCapable) {
                mAidCache.onNfcDisabled();
            }

            mP2pLinkManager.enableDisable(false, false);

            /* The NFC-EE may still be opened by another process,
             * and a transceive() could still be in progress on
             * another Binder thread.
             * Give it a while to finish existing operations
             * before we close it.
             */
            Long startTime = SystemClock.elapsedRealtime();
            do {
                synchronized (NfcService.this) {
                    if (mOpenEe == null)
                        break;
                }
                try {
                    Thread.sleep(WAIT_FOR_NFCEE_POLL_MS);
                } catch (InterruptedException e) {
                    // Ignore
                }
            } while (SystemClock.elapsedRealtime() - startTime < WAIT_FOR_NFCEE_OPERATIONS_MS);

            synchronized (NfcService.this) {
                if (mOpenEe != null) {
                    try {
                        _nfcEeClose(-1, mOpenEe.binder);
                    } catch (IOException e) { }
                }
            }

            // Stop watchdog if tag present
            // A convenient way to stop the watchdog properly consists of
            // disconnecting the tag. The polling loop shall be stopped before
            // to avoid the tag being discovered again.
            maybeDisconnectTarget();

            mNfcDispatcher.setForegroundDispatch(null, null, null);

            boolean result = mDeviceHost.deinitialize();
            if (DBG) Log.d(TAG, "mDeviceHost.deinitialize() = " + result);

            watchDog.cancel();

            updateState(NfcAdapter.STATE_OFF);

            releaseSoundPool();

            return result;
        }

        void executeEeWipe() {
            // TODO: read SE reset list from /system/etc
            byte[][]apdus = mDeviceHost.getWipeApdus();

            if (apdus == null) {
                Log.d(TAG, "No wipe APDUs found");
                return;
            }

            boolean tempEnable = mState == NfcAdapter.STATE_OFF;
            // Hold a wake-lock over the entire wipe procedure
            mEeWakeLock.acquire();
            try {
                if (tempEnable && !enableInternal()) {
                    Log.w(TAG, "Could not enable NFC to wipe NFC-EE");
                    return;
                }
                try {
                    // NFC enabled
                    int handle = 0;
                    try {
                        Log.i(TAG, "Executing SE wipe");
                        handle = doOpenSecureElementConnection();
                        if (handle < 0) {
                            Log.w(TAG, "Could not open the secure element");
                            return;
                        }
                        // TODO: remove this hack
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // Ignore
                        }

                        mDeviceHost.setTimeout(TagTechnology.ISO_DEP, 10000);
                        try {
                            for (byte[] cmd : apdus) {
                                byte[] resp = doTransceiveNoLock(handle, cmd);
                                if (resp == null) {
                                    Log.w(TAG, "Transceive failed, could not wipe NFC-EE");
                                    break;
                                }
                            }
                        } finally {
                            mDeviceHost.resetTimeouts();
                        }
                    } finally {
                        if (handle >= 0) {
                            doDisconnect(handle);
                        }
                    }
                } finally {
                    if (tempEnable) {
                        disableInternal();
                    }
                }
            } finally {
                mEeWakeLock.release();
            }
            Log.i(TAG, "SE wipe done");
        }

        void updateState(int newState) {
            synchronized (NfcService.this) {
                if (newState == mState) {
                    return;
                }
                mState = newState;
                Intent intent = new Intent(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
                intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                intent.putExtra(NfcAdapter.EXTRA_ADAPTER_STATE, mState);
                mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        }
    }

    void saveNfcOnSetting(boolean on) {
        synchronized (NfcService.this) {
            mPrefsEditor.putBoolean(PREF_NFC_ON, on);
            mPrefsEditor.apply();
        }
    }

    public void playSound(int sound) {
        synchronized (this) {
            if (mSoundPool == null) {
                Log.w(TAG, "Not playing sound when NFC is disabled");
                return;
            }
            switch (sound) {
                case SOUND_START:
                    mSoundPool.play(mStartSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
                case SOUND_END:
                    mSoundPool.play(mEndSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
                case SOUND_ERROR:
                    mSoundPool.play(mErrorSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
            }
        }
    }


    final class NfcAdapterService extends INfcAdapter.Stub {
        @Override
        public boolean enable() throws RemoteException {
            NfcService.enforceAdminPerm(mContext);

            saveNfcOnSetting(true);

            if (mIsAirplaneSensitive && isAirplaneModeOn()) {
                if (!mIsAirplaneToggleable) {
                    Log.i(TAG, "denying enable() request (airplane mode)");
                    return false;
                }
                // Make sure the override survives a reboot
                mPrefsEditor.putBoolean(PREF_AIRPLANE_OVERRIDE, true);
                mPrefsEditor.apply();
            }
            new EnableDisableTask().execute(TASK_ENABLE);

            return true;
        }

        @Override
        public boolean disable(boolean saveState) throws RemoteException {
            NfcService.enforceAdminPerm(mContext);

            if (saveState) {
                saveNfcOnSetting(false);
            }

            new EnableDisableTask().execute(TASK_DISABLE);

            return true;
        }

        @Override
        public boolean isNdefPushEnabled() throws RemoteException {
            synchronized (NfcService.this) {
                return mState == NfcAdapter.STATE_ON && mIsNdefPushEnabled;
            }
        }

        @Override
        public boolean enableNdefPush() throws RemoteException {
            NfcService.enforceAdminPerm(mContext);
            synchronized(NfcService.this) {
                if (mIsNdefPushEnabled) {
                    return true;
                }
                Log.i(TAG, "enabling NDEF Push");
                mPrefsEditor.putBoolean(PREF_NDEF_PUSH_ON, true);
                mPrefsEditor.apply();
                mIsNdefPushEnabled = true;
                if (isNfcEnabled()) {
                    mP2pLinkManager.enableDisable(true, true);
                }
            }
            return true;
        }

        @Override
        public boolean disableNdefPush() throws RemoteException {
            NfcService.enforceAdminPerm(mContext);
            synchronized(NfcService.this) {
                if (!mIsNdefPushEnabled) {
                    return true;
                }
                Log.i(TAG, "disabling NDEF Push");
                mPrefsEditor.putBoolean(PREF_NDEF_PUSH_ON, false);
                mPrefsEditor.apply();
                mIsNdefPushEnabled = false;
                if (isNfcEnabled()) {
                    mP2pLinkManager.enableDisable(false, true);
                }
            }
            return true;
        }

        @Override
        public void setForegroundDispatch(PendingIntent intent,
                IntentFilter[] filters, TechListParcel techListsParcel) {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            // Short-cut the disable path
            if (intent == null && filters == null && techListsParcel == null) {
                mNfcDispatcher.setForegroundDispatch(null, null, null);
                return;
            }

            // Validate the IntentFilters
            if (filters != null) {
                if (filters.length == 0) {
                    filters = null;
                } else {
                    for (IntentFilter filter : filters) {
                        if (filter == null) {
                            throw new IllegalArgumentException("null IntentFilter");
                        }
                    }
                }
            }

            // Validate the tech lists
            String[][] techLists = null;
            if (techListsParcel != null) {
                techLists = techListsParcel.getTechLists();
            }

            mNfcDispatcher.setForegroundDispatch(intent, filters, techLists);
        }

        @Override
        public void setAppCallback(IAppCallback callback) {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);
            mP2pLinkManager.setNdefCallback(callback, Binder.getCallingUid());
        }

        @Override
        public INfcTag getNfcTagInterface() throws RemoteException {
            return mNfcTagService;
        }

        @Override
        public INfcAdapterExtras getNfcAdapterExtrasInterface(String pkg) {
            NfcService.this.enforceNfceeAdminPerm(pkg);
            return mExtrasService;
        }

        @Override
        public INfcCardEmulation getNfcCardEmulationInterface() {
            return mCardEmulationService;
        }

        @Override
        public int getState() throws RemoteException {
            synchronized (NfcService.this) {
                return mState;
            }
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            NfcService.this.dump(fd, pw, args);
        }

        @Override
        public void dispatch(Tag tag) throws RemoteException {
            enforceAdminPerm(mContext);
            mNfcDispatcher.dispatchTag(tag);
        }

        @Override
        public void setP2pModes(int initiatorModes, int targetModes) throws RemoteException {
            enforceAdminPerm(mContext);

            mDeviceHost.setP2pInitiatorModes(initiatorModes);
            mDeviceHost.setP2pTargetModes(targetModes);
            mDeviceHost.disableDiscovery();
            mDeviceHost.enableDiscovery();
        }

        @Override
        public void setReaderMode(IBinder binder, IAppCallback callback, int flags, Bundle extras)
                throws RemoteException {
            synchronized (NfcService.this) {
                if (flags != 0) {
                    try {
                        mReaderModeParams = new ReaderModeParams();
                        mReaderModeParams.callback = callback;
                        mReaderModeParams.flags = flags;
                        mReaderModeParams.presenceCheckDelay = extras != null ?
                                (extras.getInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,
                                DEFAULT_PRESENCE_CHECK_DELAY)) : DEFAULT_PRESENCE_CHECK_DELAY;
                        binder.linkToDeath(mReaderModeDeathRecipient, 0);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Remote binder has already died.");
                        return;
                    }
                } else {
                    try {
                        mReaderModeParams = null;
                        binder.unlinkToDeath(mReaderModeDeathRecipient, 0);
                    } catch (NoSuchElementException e) {
                        Log.e(TAG, "Reader mode Binder was never registered.");
                    }
                }
                applyRouting(false);
            }
        }
    }

    final class ReaderModeDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            synchronized (NfcService.this) {
                if (mReaderModeParams != null) {
                    mReaderModeParams = null;
                    applyRouting(false);
                }
            }
        }
    }

    final class CardEmulationService extends INfcCardEmulation.Stub {
        @Override
        public boolean isDefaultServiceForCategory(int userId, ComponentName service,
                String category) throws RemoteException {
            if (!mIsHceCapable) {
                return false;
            }
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);
            validateUserId(userId);
            return mAidCache.isDefaultServiceForCategory(userId, category, service);
        }

        @Override
        public boolean isDefaultServiceForAid(int userId, ComponentName service, String aid)
                throws RemoteException {
            if (!mIsHceCapable) {
                return false;
            }
            validateUserId(userId);
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);
            return mAidCache.isDefaultServiceForAid(userId, service, aid);
        }

        @Override
        public boolean setDefaultServiceForCategory(int userId, ComponentName service,
                String category) throws RemoteException {
            if (!mIsHceCapable) {
                return false;
            }
            validateUserId(userId);
            enforceAdminPerm(mContext);
            return mAidCache.setDefaultServiceForCategory(userId, service, category);
        }

        @Override
        public boolean setDefaultForNextTap(int userId, ComponentName service)
                throws RemoteException {
            if (!mIsHceCapable) {
                return false;
            }
            validateUserId(userId);
            enforceAdminPerm(mContext);
            mHostEmulationManager.setDefaultForNextTap(service);
            return mAidCache.setDefaultForNextTap(userId, service);
        }

        @Override
        public List<ApduServiceInfo> getServices(int userId, String category)
                throws RemoteException {
            if (!mIsHceCapable) {
                return null;
            }
            validateUserId(userId);
            enforceAdminPerm(mContext);
            return mAidCache.getServicesForCategory(userId, category);
        }
    };

    final class TagService extends INfcTag.Stub {
        @Override
        public int close(int nativeHandle) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag = null;

            if (!isNfcEnabled()) {
                return ErrorCodes.ERROR_NOT_INITIALIZED;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag != null) {
                /* Remove the device from the hmap */
                unregisterObject(nativeHandle);
                tag.disconnect();
                return ErrorCodes.SUCCESS;
            }
            /* Restart polling loop for notification */
            applyRouting(true);
            return ErrorCodes.ERROR_DISCONNECT;
        }

        @Override
        public int connect(int nativeHandle, int technology) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag = null;

            if (!isNfcEnabled()) {
                return ErrorCodes.ERROR_NOT_INITIALIZED;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag == null) {
                return ErrorCodes.ERROR_DISCONNECT;
            }

            if (!tag.isPresent()) {
                return ErrorCodes.ERROR_DISCONNECT;
            }

            // Note that on most tags, all technologies are behind a single
            // handle. This means that the connect at the lower levels
            // will do nothing, as the tag is already connected to that handle.
            if (tag.connect(technology)) {
                return ErrorCodes.SUCCESS;
            } else {
                return ErrorCodes.ERROR_DISCONNECT;
            }
        }

        @Override
        public int reconnect(int nativeHandle) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag = null;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return ErrorCodes.ERROR_NOT_INITIALIZED;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag != null) {
                if (tag.reconnect()) {
                    return ErrorCodes.SUCCESS;
                } else {
                    return ErrorCodes.ERROR_DISCONNECT;
                }
            }
            return ErrorCodes.ERROR_DISCONNECT;
        }

        @Override
        public int[] getTechList(int nativeHandle) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return null;
            }

            /* find the tag in the hmap */
            TagEndpoint tag = (TagEndpoint) findObject(nativeHandle);
            if (tag != null) {
                return tag.getTechList();
            }
            return null;
        }

        @Override
        public boolean isPresent(int nativeHandle) throws RemoteException {
            TagEndpoint tag = null;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return false;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag == null) {
                return false;
            }

            return tag.isPresent();
        }

        @Override
        public boolean isNdef(int nativeHandle) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag = null;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return false;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            int[] ndefInfo = new int[2];
            if (tag == null) {
                return false;
            }
            return tag.checkNdef(ndefInfo);
        }

        @Override
        public TransceiveResult transceive(int nativeHandle, byte[] data, boolean raw)
                throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag = null;
            byte[] response;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return null;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag != null) {
                // Check if length is within limits
                if (data.length > getMaxTransceiveLength(tag.getConnectedTechnology())) {
                    return new TransceiveResult(TransceiveResult.RESULT_EXCEEDED_LENGTH, null);
                }
                int[] targetLost = new int[1];
                response = tag.transceive(data, raw, targetLost);
                int result;
                if (response != null) {
                    result = TransceiveResult.RESULT_SUCCESS;
                } else if (targetLost[0] == 1) {
                    result = TransceiveResult.RESULT_TAGLOST;
                } else {
                    result = TransceiveResult.RESULT_FAILURE;
                }
                return new TransceiveResult(result, response);
            }
            return null;
        }

        @Override
        public NdefMessage ndefRead(int nativeHandle) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return null;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag != null) {
                byte[] buf = tag.readNdef();
                if (buf == null) {
                    return null;
                }

                /* Create an NdefMessage */
                try {
                    return new NdefMessage(buf);
                } catch (FormatException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public int ndefWrite(int nativeHandle, NdefMessage msg) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return ErrorCodes.ERROR_NOT_INITIALIZED;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag == null) {
                return ErrorCodes.ERROR_IO;
            }

            if (msg == null) return ErrorCodes.ERROR_INVALID_PARAM;

            if (tag.writeNdef(msg.toByteArray())) {
                return ErrorCodes.SUCCESS;
            } else {
                return ErrorCodes.ERROR_IO;
            }

        }

        @Override
        public boolean ndefIsWritable(int nativeHandle) throws RemoteException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int ndefMakeReadOnly(int nativeHandle) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return ErrorCodes.ERROR_NOT_INITIALIZED;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag == null) {
                return ErrorCodes.ERROR_IO;
            }

            if (tag.makeReadOnly()) {
                return ErrorCodes.SUCCESS;
            } else {
                return ErrorCodes.ERROR_IO;
            }
        }

        @Override
        public int formatNdef(int nativeHandle, byte[] key) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return ErrorCodes.ERROR_NOT_INITIALIZED;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag == null) {
                return ErrorCodes.ERROR_IO;
            }

            if (tag.formatNdef(key)) {
                return ErrorCodes.SUCCESS;
            } else {
                return ErrorCodes.ERROR_IO;
            }
        }

        @Override
        public Tag rediscover(int nativeHandle) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            TagEndpoint tag = null;

            // Check if NFC is enabled
            if (!isNfcEnabled()) {
                return null;
            }

            /* find the tag in the hmap */
            tag = (TagEndpoint) findObject(nativeHandle);
            if (tag != null) {
                // For now the prime usecase for rediscover() is to be able
                // to access the NDEF technology after formatting without
                // having to remove the tag from the field, or similar
                // to have access to NdefFormatable in case low-level commands
                // were used to remove NDEF. So instead of doing a full stack
                // rediscover (which is poorly supported at the moment anyway),
                // we simply remove these two technologies and detect them
                // again.
                tag.removeTechnology(TagTechnology.NDEF);
                tag.removeTechnology(TagTechnology.NDEF_FORMATABLE);
                tag.findAndReadNdef();
                // Build a new Tag object to return
                Tag newTag = new Tag(tag.getUid(), tag.getTechList(),
                        tag.getTechExtras(), tag.getHandle(), this);
                return newTag;
            }
            return null;
        }

        @Override
        public int setTimeout(int tech, int timeout) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);
            boolean success = mDeviceHost.setTimeout(tech, timeout);
            if (success) {
                return ErrorCodes.SUCCESS;
            } else {
                return ErrorCodes.ERROR_INVALID_PARAM;
            }
        }

        @Override
        public int getTimeout(int tech) throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            return mDeviceHost.getTimeout(tech);
        }

        @Override
        public void resetTimeouts() throws RemoteException {
            mContext.enforceCallingOrSelfPermission(NFC_PERM, NFC_PERM_ERROR);

            mDeviceHost.resetTimeouts();
        }

        @Override
        public boolean canMakeReadOnly(int ndefType) throws RemoteException {
            return mDeviceHost.canMakeReadOnly(ndefType);
        }

        @Override
        public int getMaxTransceiveLength(int tech) throws RemoteException {
            return mDeviceHost.getMaxTransceiveLength(tech);
        }

        @Override
        public boolean getExtendedLengthApdusSupported() throws RemoteException {
            return mDeviceHost.getExtendedLengthApdusSupported();
        }
    }

    void _nfcEeClose(int callingPid, IBinder binder) throws IOException {
        // Blocks until a pending open() or transceive() times out.
        //TODO: This is incorrect behavior - the close should interrupt pending
        // operations. However this is not supported by current hardware.

        synchronized (NfcService.this) {
            if (!isNfcEnabledOrShuttingDown()) {
                throw new IOException("NFC adapter is disabled");
            }
            if (mOpenEe == null) {
                throw new IOException("NFC EE closed");
            }
            if (callingPid != -1 && callingPid != mOpenEe.pid) {
                throw new SecurityException("Wrong PID");
            }
            if (mOpenEe.binder != binder) {
                throw new SecurityException("Wrong binder handle");
            }

            binder.unlinkToDeath(mOpenEe, 0);
            mDeviceHost.resetTimeouts();
            doDisconnect(mOpenEe.handle);
            mOpenEe = null;

            applyRouting(true);
        }
    }

    final class NfcAdapterExtrasService extends INfcAdapterExtras.Stub {
        private Bundle writeNoException() {
            Bundle p = new Bundle();
            p.putInt("e", 0);
            return p;
        }

        private Bundle writeEeException(int exceptionType, String message) {
            Bundle p = new Bundle();
            p.putInt("e", exceptionType);
            p.putString("m", message);
            return p;
        }

        @Override
        public Bundle open(String pkg, IBinder b) throws RemoteException {
            NfcService.this.enforceNfceeAdminPerm(pkg);

            Bundle result;
            int handle = _open(b);
            if (handle < 0) {
                result = writeEeException(handle, "NFCEE open exception.");
            } else {
                result = writeNoException();
            }
            return result;
        }

        /**
         * Opens a connection to the secure element.
         *
         * @return A handle with a value >= 0 in case of success, or a
         *         negative value in case of failure.
         */
        private int _open(IBinder b) {
            synchronized(NfcService.this) {
                if (!isNfcEnabled()) {
                    return EE_ERROR_NFC_DISABLED;
                }
                if (mInProvisionMode) {
                    // Deny access to the NFCEE as long as the device is being setup
                    return EE_ERROR_IO;
                }
                if (mDeviceHost.enablePN544Quirks() && mP2pLinkManager.isLlcpActive()) {
                    // Don't allow PN544-based devices to open the SE while the LLCP
                    // link is still up or in a debounce state. This avoids race
                    // conditions in the NXP stack around P2P/SMX switching.
                    return EE_ERROR_EXT_FIELD;
                }
                if (mOpenEe != null) {
                    return EE_ERROR_ALREADY_OPEN;
                }

                boolean restorePolling = false;
                if (mDeviceHost.enablePN544Quirks() && mNfcPollingEnabled) {
                    // Disable polling for tags/P2P when connecting to the SMX
                    // on PN544-based devices. Whenever nfceeClose is called,
                    // the polling configuration will be restored.
                    mDeviceHost.disableDiscovery();
                    mNfcPollingEnabled = false;
                    restorePolling = true;
                }

                int handle = doOpenSecureElementConnection();
                if (handle < 0) {

                    if (restorePolling) {
                        mDeviceHost.enableDiscovery();
                        mNfcPollingEnabled = true;
                    }
                    return handle;
                }
                mDeviceHost.setTimeout(TagTechnology.ISO_DEP, 30000);

                mOpenEe = new OpenSecureElement(getCallingPid(), handle, b);
                try {
                    b.linkToDeath(mOpenEe, 0);
                } catch (RemoteException e) {
                    mOpenEe.binderDied();
                }

                // Add the calling package to the list of packages that have accessed
                // the secure element.
                for (String packageName : mContext.getPackageManager().getPackagesForUid(getCallingUid())) {
                    mSePackages.add(packageName);
                }

                return handle;
           }
        }

        @Override
        public Bundle close(String pkg, IBinder binder) throws RemoteException {
            NfcService.this.enforceNfceeAdminPerm(pkg);

            Bundle result;
            try {
                _nfcEeClose(getCallingPid(), binder);
                result = writeNoException();
            } catch (IOException e) {
                result = writeEeException(EE_ERROR_IO, e.getMessage());
            }
            return result;
        }

        @Override
        public Bundle transceive(String pkg, byte[] in) throws RemoteException {
            NfcService.this.enforceNfceeAdminPerm(pkg);

            Bundle result;
            byte[] out;
            try {
                out = _transceive(in);
                result = writeNoException();
                result.putByteArray("out", out);
            } catch (IOException e) {
                result = writeEeException(EE_ERROR_IO, e.getMessage());
            }
            return result;
        }

        private byte[] _transceive(byte[] data) throws IOException {
            synchronized(NfcService.this) {
                if (!isNfcEnabled()) {
                    throw new IOException("NFC is not enabled");
                }
                if (mOpenEe == null) {
                    throw new IOException("NFC EE is not open");
                }
                if (getCallingPid() != mOpenEe.pid) {
                    throw new SecurityException("Wrong PID");
                }
            }

            return doTransceive(mOpenEe.handle, data);
        }

        @Override
        public int getCardEmulationRoute(String pkg) throws RemoteException {
            NfcService.this.enforceNfceeAdminPerm(pkg);
            return mEeRoutingState;
        }

        @Override
        public void setCardEmulationRoute(String pkg, int route) throws RemoteException {
            NfcService.this.enforceNfceeAdminPerm(pkg);
            mEeRoutingState = route;
            ApplyRoutingTask applyRoutingTask = new ApplyRoutingTask();
            applyRoutingTask.execute();
            try {
                // Block until route is set
                applyRoutingTask.get();
            } catch (ExecutionException e) {
                Log.e(TAG, "failed to set card emulation mode");
            } catch (InterruptedException e) {
                Log.e(TAG, "failed to set card emulation mode");
            }
        }

        @Override
        public void authenticate(String pkg, byte[] token) throws RemoteException {
            NfcService.this.enforceNfceeAdminPerm(pkg);
        }

        @Override
        public String getDriverName(String pkg) throws RemoteException {
            NfcService.this.enforceNfceeAdminPerm(pkg);
            return mDeviceHost.getName();
        }
    }

    /** resources kept while secure element is open */
    private class OpenSecureElement implements IBinder.DeathRecipient {
        public int pid;  // pid that opened SE
        // binder handle used for DeathReceipient. Must keep
        // a reference to this, otherwise it can get GC'd and
        // the binder stub code might create a different BinderProxy
        // for the same remote IBinder, causing mismatched
        // link()/unlink()
        public IBinder binder;
        public int handle; // low-level handle
        public OpenSecureElement(int pid, int handle, IBinder binder) {
            this.pid = pid;
            this.handle = handle;
            this.binder = binder;
        }
        @Override
        public void binderDied() {
            synchronized (NfcService.this) {
                Log.i(TAG, "Tracked app " + pid + " died");
                pid = -1;
                try {
                    _nfcEeClose(-1, binder);
                } catch (IOException e) { /* already closed */ }
            }
        }
        @Override
        public String toString() {
            return new StringBuilder('@').append(Integer.toHexString(hashCode())).append("[pid=")
                    .append(pid).append(" handle=").append(handle).append("]").toString();
        }
    }

    boolean isNfcEnabledOrShuttingDown() {
        synchronized (this) {
            return (mState == NfcAdapter.STATE_ON || mState == NfcAdapter.STATE_TURNING_OFF);
        }
    }

    boolean isNfcEnabled() {
        synchronized (this) {
            return mState == NfcAdapter.STATE_ON;
        }
    }

    class WatchDogThread extends Thread {
        final Object mCancelWaiter = new Object();
        final int mTimeout;
        boolean mCanceled = false;

        public WatchDogThread(String threadName, int timeout) {
            super(threadName);
            mTimeout = timeout;
        }

        @Override
        public void run() {
            try {
                synchronized (mCancelWaiter) {
                    mCancelWaiter.wait(mTimeout);
                    if (mCanceled) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                // Should not happen; fall-through to abort.
                Log.w(TAG, "Watchdog thread interruped.");
                interrupt();
            }
            Log.e(TAG, "Watchdog triggered, aborting.");
            mDeviceHost.doAbort();
        }

        public synchronized void cancel() {
            synchronized (mCancelWaiter) {
                mCanceled = true;
                mCancelWaiter.notify();
            }
        }
    }

    static byte[] hexStringToBytes(String s) {
        if (s == null || s.length() == 0) return null;
        int len = s.length();
        if (len % 2 != 0) {
            s = '0' + s;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Read mScreenState and apply NFC-C polling and NFC-EE routing
     */
    void applyRouting(boolean force) {
        synchronized (this) {
            if (!isNfcEnabledOrShuttingDown() || mOpenEe != null) {
                // PN544 cannot be reconfigured while EE is open
                return;
            }
            WatchDogThread watchDog = new WatchDogThread("applyRouting", ROUTING_WATCHDOG_MS);
            if (mInProvisionMode) {
                mInProvisionMode = Settings.Secure.getInt(mContentResolver,
                        Settings.Global.DEVICE_PROVISIONED, 0) == 0;
                if (!mInProvisionMode) {
                    // Notify dispatcher it's fine to dispatch to any package now
                    // and allow handover transfers.
                    mNfcDispatcher.disableProvisioningMode();
                    mHandoverManager.setEnabled(true);
                }
            }
            try {
                watchDog.start();

                if (mDeviceHost.enablePN544Quirks() && mScreenState == SCREEN_STATE_OFF) {
                    /* TODO undo this after the LLCP stack is fixed.
                     * Use a different sequence when turning the screen off to
                     * workaround race conditions in pn544 libnfc. The race occurs
                     * when we change routing while there is a P2P target connect.
                     * The async LLCP callback will crash since the routing code
                     * is overwriting globals it relies on.
                     */
                    if (POLLING_MODE > SCREEN_STATE_OFF) {
                        if (force || mNfcPollingEnabled) {
                            Log.d(TAG, "NFC-C OFF, disconnect");
                            mNfcPollingEnabled = false;
                            mDeviceHost.disableDiscovery();
                            maybeDisconnectTarget();
                        }
                    }
                    if (mEeRoutingState == ROUTE_ON_WHEN_SCREEN_ON) {
                        if (force || mNfceeRouteEnabled) {
                            Log.d(TAG, "NFC-EE OFF");
                            mNfceeRouteEnabled = false;
                            mDeviceHost.doDeselectSecureElement();
                        }
                    }
                    return;
                }

                if (mIsHceCapable && mScreenState >= SCREEN_STATE_ON_LOCKED &&
                        mAidRoutingManager.aidsRoutedToHost()) {
                    if (!mHostRouteEnabled || force) {
                        mHostRouteEnabled = true;
                        mDeviceHost.enableRoutingToHost();
                    }
                } else {
                    if (force || mHostRouteEnabled) {
                        mHostRouteEnabled = false;
                        mDeviceHost.disableRoutingToHost();
                    }
                }

                // configure NFC-EE routing
                if (mScreenState >= SCREEN_STATE_ON_LOCKED &&
                        mEeRoutingState == ROUTE_ON_WHEN_SCREEN_ON) {
                    if (force || !mNfceeRouteEnabled) {
                        Log.d(TAG, "NFC-EE ON");
                        mNfceeRouteEnabled = true;
                        mDeviceHost.doSelectSecureElement();
                    }
                } else {
                    if (force ||  mNfceeRouteEnabled) {
                        Log.d(TAG, "NFC-EE OFF");
                        mNfceeRouteEnabled = false;
                        mDeviceHost.doDeselectSecureElement();
                    }
                }

                // configure NFC-C polling
                if (mScreenState >= POLLING_MODE) {
                    if (force || !mNfcPollingEnabled) {
                        Log.d(TAG, "NFC-C ON");
                        mNfcPollingEnabled = true;
                        mDeviceHost.enableDiscovery();
                    }
                    if (mReaderModeParams != null && !mReaderModeEnabled) {
                        mReaderModeEnabled = true;
                        mDeviceHost.enableReaderMode(mReaderModeParams.flags);
                    }
                    if (mReaderModeParams == null && mReaderModeEnabled) {
                        mReaderModeEnabled = false;
                        mDeviceHost.disableReaderMode();
                    }
                } else if (mInProvisionMode && mScreenState >= SCREEN_STATE_ON_LOCKED) {
                    // Special case for setup provisioning
                    if (!mNfcPollingEnabled) {
                        Log.d(TAG, "NFC-C ON");
                        mNfcPollingEnabled = true;
                        mDeviceHost.enableDiscovery();
                    }
                } else {
                    if (force || mNfcPollingEnabled) {
                        Log.d(TAG, "NFC-C OFF");
                        if (mReaderModeEnabled) {
                            mReaderModeEnabled = false;
                            mDeviceHost.disableReaderMode();
                        }
                        mNfcPollingEnabled = false;
                        mDeviceHost.disableDiscovery();
                    }
                }
            } finally {
                watchDog.cancel();
            }
        }
    }

    /** Disconnect any target if present */
    void maybeDisconnectTarget() {
        if (!isNfcEnabledOrShuttingDown()) {
            return;
        }
        Object[] objectsToDisconnect;
        synchronized (this) {
            Object[] objectValues = mObjectMap.values().toArray();
            // Copy the array before we clear mObjectMap,
            // just in case the HashMap values are backed by the same array
            objectsToDisconnect = Arrays.copyOf(objectValues, objectValues.length);
            mObjectMap.clear();
        }
        for (Object o : objectsToDisconnect) {
            if (DBG) Log.d(TAG, "disconnecting " + o.getClass().getName());
            if (o instanceof TagEndpoint) {
                // Disconnect from tags
                TagEndpoint tag = (TagEndpoint) o;
                tag.disconnect();
            } else if (o instanceof NfcDepEndpoint) {
                // Disconnect from P2P devices
                NfcDepEndpoint device = (NfcDepEndpoint) o;
                if (device.getMode() == NfcDepEndpoint.MODE_P2P_TARGET) {
                    // Remote peer is target, request disconnection
                    device.disconnect();
                } else {
                    // Remote peer is initiator, we cannot disconnect
                    // Just wait for field removal
                }
            }
        }
    }

    Object findObject(int key) {
        synchronized (this) {
            Object device = mObjectMap.get(key);
            if (device == null) {
                Log.w(TAG, "Handle not found");
            }
            return device;
        }
    }

    void registerTagObject(TagEndpoint tag) {
        synchronized (this) {
            mObjectMap.put(tag.getHandle(), tag);
        }
    }

    void unregisterObject(int handle) {
        synchronized (this) {
            mObjectMap.remove(handle);
        }
    }

    /** For use by code in this process */
    public LlcpSocket createLlcpSocket(int sap, int miu, int rw, int linearBufferLength)
            throws LlcpException {
        return mDeviceHost.createLlcpSocket(sap, miu, rw, linearBufferLength);
    }

    /** For use by code in this process */
    public LlcpConnectionlessSocket createLlcpConnectionLessSocket(int sap, String sn)
            throws LlcpException {
        return mDeviceHost.createLlcpConnectionlessSocket(sap, sn);
    }

    /** For use by code in this process */
    public LlcpServerSocket createLlcpServerSocket(int sap, String sn, int miu, int rw,
            int linearBufferLength) throws LlcpException {
        return mDeviceHost.createLlcpServerSocket(sap, sn, miu, rw, linearBufferLength);
    }

    public void sendMockNdefTag(NdefMessage msg) {
        sendMessage(MSG_MOCK_NDEF, msg);
    }

    public void routeAids(String aid, int route) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_ROUTE_AID;
        msg.arg1 = route;
        msg.obj = aid;
        mHandler.sendMessage(msg);
    }

    public void unrouteAids(String aid) {
        sendMessage(MSG_UNROUTE_AID, aid);
    }

    public void commitRouting() {
        mHandler.sendEmptyMessage(MSG_COMMIT_ROUTING);
    }

	public boolean sendData(byte[] data) {
	    return mDeviceHost.sendRawFrame(data);
    }

    void sendMessage(int what, Object obj) {
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }

    final class NfcServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ROUTE_AID: {
                    int route = msg.arg1;
                    String aid = (String) msg.obj;
                    mDeviceHost.routeAid(hexStringToBytes(aid), route);
                    // Restart polling config
                    break;
                }
                case MSG_UNROUTE_AID: {
                    String aid = (String) msg.obj;
                    mDeviceHost.unrouteAid(hexStringToBytes(aid));
                    break;
                }
                case MSG_COMMIT_ROUTING: {
                    applyRouting(true);
                    break;
                }
                case MSG_MOCK_NDEF: {
                    NdefMessage ndefMsg = (NdefMessage) msg.obj;
                    Bundle extras = new Bundle();
                    extras.putParcelable(Ndef.EXTRA_NDEF_MSG, ndefMsg);
                    extras.putInt(Ndef.EXTRA_NDEF_MAXLENGTH, 0);
                    extras.putInt(Ndef.EXTRA_NDEF_CARDSTATE, Ndef.NDEF_MODE_READ_ONLY);
                    extras.putInt(Ndef.EXTRA_NDEF_TYPE, Ndef.TYPE_OTHER);
                    Tag tag = Tag.createMockTag(new byte[] { 0x00 },
                            new int[] { TagTechnology.NDEF },
                            new Bundle[] { extras });
                    Log.d(TAG, "mock NDEF tag, starting corresponding activity");
                    Log.d(TAG, tag.toString());
                    boolean delivered = mNfcDispatcher.dispatchTag(tag);
                    if (delivered) {
                        playSound(SOUND_END);
                    } else {
                        playSound(SOUND_ERROR);
                    }
                    break;
                }

                case MSG_NDEF_TAG:
                    if (DBG) Log.d(TAG, "Tag detected, notifying applications");
                    TagEndpoint tag = (TagEndpoint) msg.obj;
                    ReaderModeParams readerParams = null;
                    int presenceCheckDelay = DEFAULT_PRESENCE_CHECK_DELAY;
                    synchronized (NfcService.this) {
                        readerParams = mReaderModeParams;
                    }
                    if (readerParams != null) {
                        presenceCheckDelay = readerParams.presenceCheckDelay;
                        if ((readerParams.flags & NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK) != 0) {
                            if (DBG) Log.d(TAG, "Skipping NDEF detection in reader mode");
                            tag.startPresenceChecking(presenceCheckDelay);
                            dispatchTagEndpoint(tag, readerParams);
                            break;
                        }
                    }
                    if (readerParams == null ||
                            (readerParams.flags & NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS) == 0) {
                        playSound(SOUND_START);
                    }
                    if (tag.getConnectedTechnology() == TagTechnology.NFC_BARCODE) {
                        // When these tags start containing NDEF, they will require
                        // the stack to deal with them in a different way, since
                        // they are activated only really shortly.
                        // For now, don't consider NDEF on these.
                        if (DBG) Log.d(TAG, "Skipping NDEF detection for NFC Barcode");
                        tag.startPresenceChecking(presenceCheckDelay);
                        dispatchTagEndpoint(tag, readerParams);
                        break;
                    }
                    NdefMessage ndefMsg = tag.findAndReadNdef();

                    if (ndefMsg != null) {
                        tag.startPresenceChecking(presenceCheckDelay);
                        dispatchTagEndpoint(tag, readerParams);
                    } else {
                        if (tag.reconnect()) {
                            tag.startPresenceChecking(presenceCheckDelay);
                            dispatchTagEndpoint(tag, readerParams);
                        } else {
                            tag.disconnect();
                            playSound(SOUND_ERROR);
                        }
                    }
                    break;

                case MSG_CARD_EMULATION:
                    if (DBG) Log.d(TAG, "Card Emulation message");
                    /* Tell the host-emu manager an AID has been selected on
                     * a secure element.
                     */
                    if (mHostEmulationManager != null) {
                        mHostEmulationManager.notifyOffHostAidSelected();
                    }
                    byte[] aid = (byte[]) msg.obj;
                    /* Send broadcast */
                    Intent aidIntent = new Intent();
                    aidIntent.setAction(ACTION_AID_SELECTED);
                    aidIntent.putExtra(EXTRA_AID, aid);
                    if (DBG) Log.d(TAG, "Broadcasting " + ACTION_AID_SELECTED);
                    sendSeBroadcast(aidIntent);
                    break;

                case MSG_SE_EMV_CARD_REMOVAL:
                    if (DBG) Log.d(TAG, "Card Removal message");
                    /* Send broadcast */
                    Intent cardRemovalIntent = new Intent();
                    cardRemovalIntent.setAction(ACTION_EMV_CARD_REMOVAL);
                    if (DBG) Log.d(TAG, "Broadcasting " + ACTION_EMV_CARD_REMOVAL);
                    sendSeBroadcast(cardRemovalIntent);
                    break;

                case MSG_SE_APDU_RECEIVED:
                    if (DBG) Log.d(TAG, "APDU Received message");
                    byte[] apduBytes = (byte[]) msg.obj;
                    /* Send broadcast */
                    Intent apduReceivedIntent = new Intent();
                    apduReceivedIntent.setAction(ACTION_APDU_RECEIVED);
                    if (apduBytes != null && apduBytes.length > 0) {
                        apduReceivedIntent.putExtra(EXTRA_APDU_BYTES, apduBytes);
                    }
                    if (DBG) Log.d(TAG, "Broadcasting " + ACTION_APDU_RECEIVED);
                    sendSeBroadcast(apduReceivedIntent);
                    break;

                case MSG_SE_MIFARE_ACCESS:
                    if (DBG) Log.d(TAG, "MIFARE access message");
                    /* Send broadcast */
                    byte[] mifareCmd = (byte[]) msg.obj;
                    Intent mifareAccessIntent = new Intent();
                    mifareAccessIntent.setAction(ACTION_MIFARE_ACCESS_DETECTED);
                    if (mifareCmd != null && mifareCmd.length > 1) {
                        int mifareBlock = mifareCmd[1] & 0xff;
                        if (DBG) Log.d(TAG, "Mifare Block=" + mifareBlock);
                        mifareAccessIntent.putExtra(EXTRA_MIFARE_BLOCK, mifareBlock);
                    }
                    if (DBG) Log.d(TAG, "Broadcasting " + ACTION_MIFARE_ACCESS_DETECTED);
                    sendSeBroadcast(mifareAccessIntent);
                    break;

                case MSG_LLCP_LINK_ACTIVATION:
                    if (mIsDebugBuild) {
                        Intent actIntent = new Intent(ACTION_LLCP_UP);
                        mContext.sendBroadcast(actIntent);
                    }
                    llcpActivated((NfcDepEndpoint) msg.obj);
                    break;

                case MSG_LLCP_LINK_DEACTIVATED:
                    if (mIsDebugBuild) {
                        Intent deactIntent = new Intent(ACTION_LLCP_DOWN);
                        mContext.sendBroadcast(deactIntent);
                    }
                    NfcDepEndpoint device = (NfcDepEndpoint) msg.obj;
                    boolean needsDisconnect = false;

                    Log.d(TAG, "LLCP Link Deactivated message. Restart polling loop.");
                    synchronized (NfcService.this) {
                        /* Check if the device has been already unregistered */
                        if (mObjectMap.remove(device.getHandle()) != null) {
                            /* Disconnect if we are initiator */
                            if (device.getMode() == NfcDepEndpoint.MODE_P2P_TARGET) {
                                if (DBG) Log.d(TAG, "disconnecting from target");
                                needsDisconnect = true;
                            } else {
                                if (DBG) Log.d(TAG, "not disconnecting from initiator");
                            }
                        }
                    }
                    if (needsDisconnect) {
                        device.disconnect();  // restarts polling loop
                    }

                    mP2pLinkManager.onLlcpDeactivated();
                    break;
                case MSG_LLCP_LINK_FIRST_PACKET:
                    mP2pLinkManager.onLlcpFirstPacketReceived();
                    break;
                case MSG_TARGET_DESELECTED:
                    /* Broadcast Intent Target Deselected */
                    if (DBG) Log.d(TAG, "Target Deselected");
                    Intent intent = new Intent();
                    intent.setAction(NativeNfcManager.INTERNAL_TARGET_DESELECTED_ACTION);
                    if (DBG) Log.d(TAG, "Broadcasting Intent");
                    mContext.sendOrderedBroadcast(intent, NFC_PERM);
                    break;

                case MSG_SE_FIELD_ACTIVATED: {
                    if (DBG) Log.d(TAG, "SE FIELD ACTIVATED");
                    Intent eventFieldOnIntent = new Intent();
                    eventFieldOnIntent.setAction(ACTION_RF_FIELD_ON_DETECTED);
                    sendSeBroadcast(eventFieldOnIntent);
                    break;
                }

                case MSG_SE_FIELD_DEACTIVATED: {
                    if (DBG) Log.d(TAG, "SE FIELD DEACTIVATED");
                    Intent eventFieldOffIntent = new Intent();
                    eventFieldOffIntent.setAction(ACTION_RF_FIELD_OFF_DETECTED);
                    sendSeBroadcast(eventFieldOffIntent);
                    break;
                }

                case MSG_SE_LISTEN_ACTIVATED: {
                    if (DBG) Log.d(TAG, "SE LISTEN MODE ACTIVATED");
                    Intent listenModeActivated = new Intent();
                    listenModeActivated.setAction(ACTION_SE_LISTEN_ACTIVATED);
                    sendSeBroadcast(listenModeActivated);
                    break;
                }

                case MSG_SE_LISTEN_DEACTIVATED: {
                    if (DBG) Log.d(TAG, "SE LISTEN MODE DEACTIVATED");
                    Intent listenModeDeactivated = new Intent();
                    listenModeDeactivated.setAction(ACTION_SE_LISTEN_DEACTIVATED);
                    sendSeBroadcast(listenModeDeactivated);
                    break;
                }
                default:
                    Log.e(TAG, "Unknown message received");
                    break;
            }
        }

        private void sendSeBroadcast(Intent intent) {
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            // Resume app switches so the receivers can start activites without delay
            mNfcDispatcher.resumeAppSwitches();

            synchronized(this) {
                for (PackageInfo pkg : mInstalledPackages) {
                    if (pkg != null && pkg.applicationInfo != null) {
                        if (mNfceeAccessControl.check(pkg.applicationInfo)) {
                            intent.setPackage(pkg.packageName);
                            mContext.sendBroadcast(intent);
                        }
                    }
                }
            }
        }

        private boolean llcpActivated(NfcDepEndpoint device) {
            Log.d(TAG, "LLCP Activation message");

            if (device.getMode() == NfcDepEndpoint.MODE_P2P_TARGET) {
                if (DBG) Log.d(TAG, "NativeP2pDevice.MODE_P2P_TARGET");
                if (device.connect()) {
                    /* Check LLCP compliancy */
                    if (mDeviceHost.doCheckLlcp()) {
                        /* Activate LLCP Link */
                        if (mDeviceHost.doActivateLlcp()) {
                            if (DBG) Log.d(TAG, "Initiator Activate LLCP OK");
                            synchronized (NfcService.this) {
                                // Register P2P device
                                mObjectMap.put(device.getHandle(), device);
                            }
                            mP2pLinkManager.onLlcpActivated();
                            return true;
                        } else {
                            /* should not happen */
                            Log.w(TAG, "Initiator LLCP activation failed. Disconnect.");
                            device.disconnect();
                        }
                    } else {
                        if (DBG) Log.d(TAG, "Remote Target does not support LLCP. Disconnect.");
                        device.disconnect();
                    }
                } else {
                    if (DBG) Log.d(TAG, "Cannot connect remote Target. Polling loop restarted.");
                    /*
                     * The polling loop should have been restarted in failing
                     * doConnect
                     */
                }
            } else if (device.getMode() == NfcDepEndpoint.MODE_P2P_INITIATOR) {
                if (DBG) Log.d(TAG, "NativeP2pDevice.MODE_P2P_INITIATOR");
                /* Check LLCP compliancy */
                if (mDeviceHost.doCheckLlcp()) {
                    /* Activate LLCP Link */
                    if (mDeviceHost.doActivateLlcp()) {
                        if (DBG) Log.d(TAG, "Target Activate LLCP OK");
                        synchronized (NfcService.this) {
                            // Register P2P device
                            mObjectMap.put(device.getHandle(), device);
                        }
                        mP2pLinkManager.onLlcpActivated();
                        return true;
                    }
                } else {
                    Log.w(TAG, "checkLlcp failed");
                }
            }

            return false;
        }

        private void dispatchTagEndpoint(TagEndpoint tagEndpoint, ReaderModeParams readerParams) {
            Tag tag = new Tag(tagEndpoint.getUid(), tagEndpoint.getTechList(),
                    tagEndpoint.getTechExtras(), tagEndpoint.getHandle(), mNfcTagService);
            registerTagObject(tagEndpoint);
            if (readerParams != null) {
                try {
                    if ((readerParams.flags & NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS) == 0) {
                        playSound(SOUND_END);
                    }
                    if (readerParams.callback != null) {
                        readerParams.callback.onTagDiscovered(tag);
                        return;
                    } else {
                        // Follow normal dispatch below
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Reader mode remote has died, falling back.");
                    // Intentional fall-through
                } catch (Exception e) {
                    // Catch any other exception
                    Log.e(TAG, "App exception, not dispatching.");
                    return;
                }
            }
            if (!mNfcDispatcher.dispatchTag(tag)) {
                unregisterObject(tagEndpoint.getHandle());
                playSound(SOUND_ERROR);
            } else {
                playSound(SOUND_END);
            }
        }
    }

    private NfcServiceHandler mHandler = new NfcServiceHandler();

    class ApplyRoutingTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... params) {
            synchronized (NfcService.this) {
                if (params == null || params.length != 1) {
                    // force apply current routing
                    applyRouting(true);
                    return null;
                }
                mScreenState = params[0].intValue();

                mRoutingWakeLock.acquire();
                try {
                    applyRouting(false);
                } finally {
                    mRoutingWakeLock.release();
                }
                return null;
            }
        }
    }

    private final BroadcastReceiver mOwnerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_PACKAGE_REMOVED) ||
                    action.equals(Intent.ACTION_PACKAGE_ADDED) ||
                    action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE) ||
                    action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)) {
                updatePackageCache();

                if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                    // Clear the NFCEE access cache in case a UID gets recycled
                    mNfceeAccessControl.invalidateCache();

                    boolean dataRemoved = intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false);
                    if (dataRemoved) {
                        Uri data = intent.getData();
                        if (data == null) return;
                        String packageName = data.getSchemeSpecificPart();

                        synchronized (NfcService.this) {
                            if (mSePackages.contains(packageName)) {
                                new EnableDisableTask().execute(TASK_EE_WIPE);
                                mSePackages.remove(packageName);
                            }
                        }
                    }
                }
            } else if (action.equals(ACTION_MASTER_CLEAR_NOTIFICATION)) {
                EnableDisableTask eeWipeTask = new EnableDisableTask();
                eeWipeTask.execute(TASK_EE_WIPE);
                try {
                    eeWipeTask.get();  // blocks until EE wipe is complete
                } catch (ExecutionException e) {
                    Log.w(TAG, "failed to wipe NFC-EE");
                } catch (InterruptedException e) {
                    Log.w(TAG, "failed to wipe NFC-EE");
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(
                    NativeNfcManager.INTERNAL_TARGET_DESELECTED_ACTION)) {
                // Perform applyRouting() in AsyncTask to serialize blocking calls
                new ApplyRoutingTask().execute();
            } else if (action.equals(Intent.ACTION_SCREEN_ON)
                    || action.equals(Intent.ACTION_SCREEN_OFF)
                    || action.equals(Intent.ACTION_USER_PRESENT)) {
                // Perform applyRouting() in AsyncTask to serialize blocking calls
                int screenState = SCREEN_STATE_OFF;
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    screenState = SCREEN_STATE_OFF;
                } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    screenState = mKeyguard.isKeyguardLocked() ?
                            SCREEN_STATE_ON_LOCKED : SCREEN_STATE_ON_UNLOCKED;
                } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                    screenState = SCREEN_STATE_ON_UNLOCKED;
                }
                new ApplyRoutingTask().execute(Integer.valueOf(screenState));
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
                // Query the airplane mode from Settings.System just to make sure that
                // some random app is not sending this intent
                if (isAirplaneModeOn != isAirplaneModeOn()) {
                    return;
                }
                if (!mIsAirplaneSensitive) {
                    return;
                }
                mPrefsEditor.putBoolean(PREF_AIRPLANE_OVERRIDE, false);
                mPrefsEditor.apply();
                if (isAirplaneModeOn) {
                    new EnableDisableTask().execute(TASK_DISABLE);
                } else if (!isAirplaneModeOn && mPrefs.getBoolean(PREF_NFC_ON, NFC_ON_DEFAULT)) {
                    new EnableDisableTask().execute(TASK_ENABLE);
                }
            } else if (action.equals(Intent.ACTION_USER_SWITCHED)) {
                mP2pLinkManager.onUserSwitched();
                if (mIsHceCapable) {
                    mAidCache.invalidateCache(intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 0));
                }
            }
        }
    };

    /** Returns true if airplane mode is currently on */
    boolean isAirplaneModeOn() {
        return Settings.System.getInt(mContentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    /** for debugging only - no i18n */
    static String stateToString(int state) {
        switch (state) {
            case NfcAdapter.STATE_OFF:
                return "off";
            case NfcAdapter.STATE_TURNING_ON:
                return "turning on";
            case NfcAdapter.STATE_ON:
                return "on";
            case NfcAdapter.STATE_TURNING_OFF:
                return "turning off";
            default:
                return "<error>";
        }
    }

    /** For debugging only - no i18n */
    static String screenStateToString(int screenState) {
        switch (screenState) {
            case SCREEN_STATE_OFF:
                return "OFF";
            case SCREEN_STATE_ON_LOCKED:
                return "ON_LOCKED";
            case SCREEN_STATE_ON_UNLOCKED:
                return "ON_UNLOCKED";
            default:
                return "UNKNOWN";
        }
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump nfc from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid()
                    + " without permission " + android.Manifest.permission.DUMP);
            return;
        }

        synchronized (this) {
            pw.println("mState=" + stateToString(mState));
            pw.println("mIsZeroClickRequested=" + mIsNdefPushEnabled);
            pw.println("mScreenState=" + screenStateToString(mScreenState));
            pw.println("mNfcPollingEnabled=" + mNfcPollingEnabled);
            pw.println("mNfceeRouteEnabled=" + mNfceeRouteEnabled);
            pw.println("mIsAirplaneSensitive=" + mIsAirplaneSensitive);
            pw.println("mIsAirplaneToggleable=" + mIsAirplaneToggleable);
            pw.println("mOpenEe=" + mOpenEe);
            mP2pLinkManager.dump(fd, pw, args);
            if (mIsHceCapable) {
                mAidCache.dump(fd, pw, args);
            }
            mNfceeAccessControl.dump(fd, pw, args);
            mNfcDispatcher.dump(fd, pw, args);
            pw.println(mDeviceHost.dump());

        }
    }
}

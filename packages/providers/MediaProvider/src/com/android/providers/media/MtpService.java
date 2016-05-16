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

package com.android.providers.media;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDatabase;
import android.mtp.MtpServer;
import android.mtp.MtpStorage;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

public class MtpService extends Service {
    private static final String TAG = "MtpService";
    private static final boolean LOGD = true;

    // We restrict PTP to these subdirectories
    private static final String[] PTP_DIRECTORIES = new String[] {
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_PICTURES,
    };

    private void addStorageDevicesLocked() {
        if (mPtpMode) {
            // In PTP mode we support only primary storage
            final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
            final String path = primary.getPath();
            if (path != null) {
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    addStorageLocked(mVolumeMap.get(path));
                }
            }
        } else {
            for (StorageVolume volume : mVolumeMap.values()) {
                addStorageLocked(volume);
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                // If the media scanner is running, it may currently be calling
                // sendObjectAdded/Removed, which also synchronizes on mBinder
                // (and in addition to that, all the native MtpServer methods
                // lock the same Mutex). If it happens to be in an mtp device
                // write(), it may block for some time, so process this broadcast
                // in a thread.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mBinder) {
                            // Unhide the storage units when the user has unlocked the lockscreen
                            if (mMtpDisabled) {
                                addStorageDevicesLocked();
                                mMtpDisabled = false;
                            }
                        }
                    }}, "addStorageDevices").start();
            }
        }
    };

    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            synchronized (mBinder) {
                Log.d(TAG, "onStorageStateChanged " + path + " " + oldState + " -> " + newState);
                if (Environment.MEDIA_MOUNTED.equals(newState)) {
                    volumeMountedLocked(path);
                } else if (Environment.MEDIA_MOUNTED.equals(oldState)) {
                    StorageVolume volume = mVolumeMap.remove(path);
                    if (volume != null) {
                        removeStorageLocked(volume);
                    }
                }
            }
        }
    };

    private MtpDatabase mDatabase;
    private MtpServer mServer;
    private StorageManager mStorageManager;
    /** Flag indicating if MTP is disabled due to keyguard */
    private boolean mMtpDisabled;
    private boolean mPtpMode;
    private final HashMap<String, StorageVolume> mVolumeMap = new HashMap<String, StorageVolume>();
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<String, MtpStorage>();
    private StorageVolume[] mVolumes;

    @Override
    public void onCreate() {
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));

        mStorageManager = StorageManager.from(this);
        synchronized (mBinder) {
            updateDisabledStateLocked();
            mStorageManager.registerListener(mStorageEventListener);
            StorageVolume[] volumes = mStorageManager.getVolumeList();
            mVolumes = volumes;
            for (int i = 0; i < volumes.length; i++) {
                String path = volumes[i].getPath();
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    volumeMountedLocked(path);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (mBinder) {
            updateDisabledStateLocked();
            mPtpMode = (intent == null ? false
                    : intent.getBooleanExtra(UsbManager.USB_FUNCTION_PTP, false));
            String[] subdirs = null;
            if (mPtpMode) {
                int count = PTP_DIRECTORIES.length;
                subdirs = new String[count];
                for (int i = 0; i < count; i++) {
                    File file =
                            Environment.getExternalStoragePublicDirectory(PTP_DIRECTORIES[i]);
                    // make sure this directory exists
                    file.mkdirs();
                    subdirs[i] = file.getPath();
                }
            }
            final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
            mDatabase = new MtpDatabase(this, MediaProvider.EXTERNAL_VOLUME,
                    primary.getPath(), subdirs);
            manageServiceLocked();
        }

        return START_STICKY;
    }

    private void updateDisabledStateLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        final KeyguardManager keyguardManager = (KeyguardManager) getSystemService(
                Context.KEYGUARD_SERVICE);
        mMtpDisabled = (keyguardManager.isKeyguardLocked() && keyguardManager.isKeyguardSecure())
                || !isCurrentUser;
        if (LOGD) {
            Log.d(TAG, "updating state; isCurrentUser=" + isCurrentUser + ", mMtpLocked="
                    + mMtpDisabled);
        }
    }

    /**
     * Manage {@link #mServer}, creating only when running as the current user.
     */
    private void manageServiceLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        if (mServer == null && isCurrentUser) {
            Log.d(TAG, "starting MTP server in " + (mPtpMode ? "PTP mode" : "MTP mode"));
            mServer = new MtpServer(mDatabase, mPtpMode);
            if (!mMtpDisabled) {
                addStorageDevicesLocked();
            }
            mServer.start();
        } else if (mServer != null && !isCurrentUser) {
            Log.d(TAG, "no longer current user; shutting down MTP server");
            // Internally, kernel will close our FD, and server thread will
            // handle cleanup.
            mServer = null;
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        mStorageManager.unregisterListener(mStorageEventListener);
    }

    private final IMtpService.Stub mBinder =
            new IMtpService.Stub() {
        public void sendObjectAdded(int objectHandle) {
            synchronized (mBinder) {
                if (mServer != null) {
                    mServer.sendObjectAdded(objectHandle);
                }
            }
        }

        public void sendObjectRemoved(int objectHandle) {
            synchronized (mBinder) {
                if (mServer != null) {
                    mServer.sendObjectRemoved(objectHandle);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void volumeMountedLocked(String path) {
        for (int i = 0; i < mVolumes.length; i++) {
            StorageVolume volume = mVolumes[i];
            if (volume.getPath().equals(path)) {
                mVolumeMap.put(path, volume);
                if (!mMtpDisabled) {
                    // In PTP mode we support only primary storage
                    if (volume.isPrimary() || !mPtpMode) {
                        addStorageLocked(volume);
                    }
                }
                break;
            }
        }
    }

    private void addStorageLocked(StorageVolume volume) {
        MtpStorage storage = new MtpStorage(volume, getApplicationContext());
        String path = storage.getPath();
        mStorageMap.put(path, storage);

        Log.d(TAG, "addStorageLocked " + storage.getStorageId() + " " + path);
        if (mDatabase != null) {
            mDatabase.addStorage(storage);
        }
        if (mServer != null) {
            mServer.addStorage(storage);
        }
    }

    private void removeStorageLocked(StorageVolume volume) {
        MtpStorage storage = mStorageMap.remove(volume.getPath());
        if (storage == null) {
            Log.e(TAG, "no MtpStorage for " + volume.getPath());
            return;
        }

        Log.d(TAG, "removeStorageLocked " + storage.getStorageId() + " " + storage.getPath());
        if (mDatabase != null) {
            mDatabase.removeStorage(storage);
        }
        if (mServer != null) {
            mServer.removeStorage(storage);
        }
    }
}

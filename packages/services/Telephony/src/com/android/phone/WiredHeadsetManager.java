/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.google.android.collect.Lists;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.util.Log;

import java.util.List;

/**
 * Listens for and caches headset state.  Used By the AudioRouter for maintaining
 * overall audio state for use in the UI layer. Also provides method for connecting the bluetooth
 * headset to the phone call.
 */
public class WiredHeadsetManager {
    private static final String LOG_TAG = WiredHeadsetManager.class.getSimpleName();
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);

    // True if a wired headset is currently plugged in, based on the state
    // from the latest Intent.ACTION_HEADSET_PLUG broadcast we received in
    // mReceiver.onReceive().
    private boolean mIsHeadsetPlugged = false;
    private final WiredHeadsetBroadcastReceiver mReceiver;
    private final List<WiredHeadsetListener> mListeners = Lists.newArrayList();

    public WiredHeadsetManager(Context context) {
        mReceiver = new WiredHeadsetBroadcastReceiver();

        // Register for misc other intent broadcasts.
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(mReceiver, intentFilter);
    }

    /**
     * Returns connection state of the wires headset.
     */
    public boolean isHeadsetPlugged() {
        return mIsHeadsetPlugged;
    }

    /**
     * Add a listener for wired headset connection status.
     */
    public void addWiredHeadsetListener(WiredHeadsetListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Called when we get an event from the system for the headset connection state.
     */
    private void onHeadsetConnection(boolean pluggedIn) {
        if (DBG) {
            Log.d(LOG_TAG, "Wired headset connected: " +  pluggedIn);
        }
        mIsHeadsetPlugged = pluggedIn;

        notifyListeners();
    }

    private void notifyListeners() {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onWiredHeadsetConnection(mIsHeadsetPlugged);
        }
    }

    /**
     * Receiver for misc intent broadcasts the BluetoothManager cares about.
     */
    private class WiredHeadsetBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_HEADSET_PLUG");
                if (VDBG) Log.d(LOG_TAG, "    state: " + intent.getIntExtra("state", 0));
                if (VDBG) Log.d(LOG_TAG, "    name: " + intent.getStringExtra("name"));
                onHeadsetConnection(intent.getIntExtra("state", 0) == 1);
            }
        }
    }

    /**
     * Listeners for those that want to know about the headset state.
     */
    public interface WiredHeadsetListener {
        void onWiredHeadsetConnection(boolean pluggedIn);
    }
}

/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.mail.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Holds the storage state of the system. Allows for registering a single handler which can
 * adjust application state to deal with STORAGE_LOW and STORAGE_OK state:
 * Reference: {@link Intent#ACTION_DEVICE_STORAGE_LOW}, {@link Intent#ACTION_DEVICE_STORAGE_OK}
 */
public final class StorageLowState {
    /**
     * Methods that are called when a device enters/leaves storage low mode.
     */
    public interface LowStorageHandler {
        /**
         * Method to be called when the device enters storage low mode.
         */
        void onStorageLow();

        /**
         * Method to be run when the device recovers from storage low mode.
         */
        void onStorageOk();
    }
    /** True if the system has entered STORAGE_LOW state. */
    private static boolean sIsStorageLow = false;
    /** If non-null, this represents a handler that is notified of changes to state. */
    private static LowStorageHandler sHandler = null;

    /** Private constructor to avoid class instantiation. */
    private StorageLowState() {
        // Do nothing.
    }

    /**
     * Checks if the device is in storage low state. If the state changes, the handler is notified
     * of it. The handler is not notified if the state remains the same as before.
     */
    public static void checkStorageLowMode(Context context) {
        // Identify if we are in low storage mode. This works because storage low is a sticky
        // intent, so we are guaranteed a non-null intent if that broadcast was sent and not
        // cleared subsequently.
        final IntentFilter filter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        final Intent result = context.registerReceiver(null, filter);
        setIsStorageLow(result != null);
    }

    /**
     * Notifies {@link StorageLowState} that the device has entered storage low state.
     */
    public static void setIsStorageLow(boolean newValue) {
        if (sIsStorageLow == newValue) {
            // The state is unchanged, nothing to do.
            return;
        }
        sIsStorageLow = newValue;
        if (sHandler == null) {
            return;
        }
        if (newValue) {
            sHandler.onStorageLow();
        } else {
            sHandler.onStorageOk();
        }
    }

    /**
     * Sets the handler that can adjust application state to deal with storage low and
     * storage ok intents.
     * Reference: {@link Intent#ACTION_DEVICE_STORAGE_LOW}, {@link Intent#ACTION_DEVICE_STORAGE_OK}
     * @param in a handler that can deal with changes to the storage state.
     */
    public static void registerHandler(LowStorageHandler in) {
        sHandler = in;
        // If we are currently in low storage mode, let the handler deal with it immediately.
        if (sIsStorageLow) {
            sHandler.onStorageLow();
        }
    }
}

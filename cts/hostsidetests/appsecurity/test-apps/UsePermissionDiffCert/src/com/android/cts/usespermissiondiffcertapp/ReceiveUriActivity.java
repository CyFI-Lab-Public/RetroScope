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

package com.android.cts.usespermissiondiffcertapp;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue.IdleHandler;
import android.os.SystemClock;
import android.util.Log;

public class ReceiveUriActivity extends Activity {
    static final String TAG = "ReceiveUriActivity";
    private static final Object sLock = new Object();
    private static boolean sStarted;
    private static boolean sNewIntent;
    private static boolean sDestroyed = true;
    private static ReceiveUriActivity sCurInstance;

    private static final long TIMEOUT_MILLIS = 30 * SECOND_IN_MILLIS;

    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        synchronized (sLock) {
            Log.i(TAG, "onCreate: sCurInstance=" + sCurInstance);
            if (sCurInstance != null) {
                finishCurInstance();
            }
            sCurInstance = this;
            sStarted = true;
            sDestroyed = false;
            sLock.notifyAll();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        synchronized (sLock) {
            Log.i(TAG, "onNewIntent: sCurInstance=" + sCurInstance);
            sNewIntent = true;
            sLock.notifyAll();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: sCurInstance=" + sCurInstance);
        Looper.myQueue().addIdleHandler(new IdleHandler() {
            @Override
            public boolean queueIdle() {
                synchronized (sLock) {
                    sDestroyed = true;
                    sLock.notifyAll();
                }
                return false;
            }
        });
    }

    public static void finishCurInstance() {
        synchronized (sLock) {
            if (sCurInstance != null) {
                sCurInstance.finish();
                sCurInstance = null;
            }
        }
    }

    public static void finishCurInstanceSync() {
        finishCurInstance();

        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sDestroyed) {
                try {
                    sLock.wait(TIMEOUT_MILLIS);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime + TIMEOUT_MILLIS)) {
                    throw new RuntimeException("Timeout");
                }
            }
        }
    }

    public static void clearStarted() {
        synchronized (sLock) {
            sStarted = false;
        }
    }

    public static void clearNewIntent() {
        synchronized (sLock) {
            sNewIntent = false;
        }
    }

    public static void waitForStart() {
        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sStarted) {
                try {
                    sLock.wait(TIMEOUT_MILLIS);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime + TIMEOUT_MILLIS)) {
                    throw new RuntimeException("Timeout");
                }
            }
        }
    }

    public static void waitForNewIntent() {
        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sNewIntent) {
                try {
                    sLock.wait(TIMEOUT_MILLIS);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime + TIMEOUT_MILLIS)) {
                    throw new RuntimeException("Timeout");
                }
            }
        }
    }
}

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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.MessageQueue.IdleHandler;

public class ReceiveUriService extends Service {
    private static final Object sLock = new Object();
    private static boolean sStarted;
    private static boolean sDestroyed;
    private static int sCurStartId;
    private static ReceiveUriService sCurInstance;

    Handler mHandler = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (sLock) {
            sCurStartId = startId;
            sCurInstance = this;
            sStarted = true;
            sDestroyed = false;
            sLock.notifyAll();
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Looper.myQueue().addIdleHandler(new IdleHandler() {
            @Override
            public boolean queueIdle() {
                synchronized (sLock) {
                    sDestroyed = true;
                    sCurInstance = null;
                    sLock.notifyAll();
                }
                return false;
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void stop(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.cts.usespermissiondiffcertapp",
                "com.android.cts.usespermissiondiffcertapp.ReceiveUriService"));
        context.stopService(intent);
    }

    public static int getCurStartId() {
        synchronized (sLock) {
            return sCurStartId;
        }
    }

    public static void stopCurWithId(int id) {
        synchronized (sLock) {
            sCurInstance.stopSelf(id);
        }
    }

    public static void stopSync(Context context) {
        stop(context);

        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sDestroyed) {
                try {
                    sLock.wait(5000);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime+5000)) {
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

    public static void waitForStart() {
        synchronized (sLock) {
            final long startTime = SystemClock.uptimeMillis();
            while (!sStarted) {
                try {
                    sLock.wait(5000);
                } catch (InterruptedException e) {
                }
                if (SystemClock.uptimeMillis() >= (startTime+5000)) {
                    throw new RuntimeException("Timeout");
                }
            }
        }
    }
}

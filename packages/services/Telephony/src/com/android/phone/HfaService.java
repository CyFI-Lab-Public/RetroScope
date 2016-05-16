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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Service for performing HfaActivation without any UI.
 */
public class HfaService extends Service {
    private static final String TAG = HfaService.class.getSimpleName();

    private HfaLogic mHfaLogic;

    @Override
    public void onCreate() {
        Log.i(TAG, "service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PendingIntent otaResponseIntent = intent.getParcelableExtra(
                OtaUtils.EXTRA_OTASP_RESULT_CODE_PENDING_INTENT);

        mHfaLogic = new HfaLogic(this, new HfaLogic.HfaLogicCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess");
                onComplete();
            }

            @Override
            public void onError(String msg) {
                Log.i(TAG, "onError: " + msg);
                // We do not respond from this service. On success or failure
                // we do the same thing...finish.
                onComplete();
            }
        }, otaResponseIntent);
        mHfaLogic.start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onComplete() {
        stopSelf();
    }
}

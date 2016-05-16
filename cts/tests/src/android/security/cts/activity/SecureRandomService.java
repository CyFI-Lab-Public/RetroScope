/*
 * Copyright 2013 The Android Open Source Project
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

package android.security.cts.activity;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.security.cts.activity.ISecureRandomService;

import android.app.Service;
import android.content.Intent;

import java.security.SecureRandom;

public class SecureRandomService extends Service {
    /**
     * This helps the process shut down a little faster and get us a new
     * PID earlier than calling stopService.
     */
    private Handler mShutdownHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            stopSelf();
        }
    };

    private final ISecureRandomService.Stub mBinder = new ISecureRandomService.Stub() {

        /**
         * Returns output from SecureRandom and the current process PID. Note
         * that this should only be called once. To ensure that it's only called
         * once, this will throw an error if it's called twice in a row.
         */
        public int getRandomBytesAndPid(byte[] randomBytes) {
            mShutdownHandler.sendEmptyMessage(-1);

            SecureRandom sr = new SecureRandom();
            sr.nextBytes(randomBytes);
            return android.os.Process.myPid();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}

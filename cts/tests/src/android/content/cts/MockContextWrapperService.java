/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.content.cts;

import android.app.Service;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

/**
 * This class is used for {@link ContextWrapper}
 *
 * @see ContextWrapperTest
 */
public class MockContextWrapperService extends Service {
    private static boolean mHadCalledOnBind = false;
    private static boolean mHadCalledOnUnbind = false;
    private static boolean mHadCalledOnStart = false;
    private static boolean mHadCalledOnDestory = false;
    private static final int TEST_MESSAGE_WHAT = 1;

    private final IBinder mBinder = new Binder();

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(TEST_MESSAGE_WHAT), 1000);
        }
    };

    @Override
    public void onCreate() {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(TEST_MESSAGE_WHAT), 1000);
    }

    @Override
    public void onDestroy() {
        mHadCalledOnDestory = true;
        mHandler.removeMessages(1);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHadCalledOnUnbind = true;
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mHadCalledOnBind = true;
        return mBinder;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mHadCalledOnStart = true;
    }

    public static void reset() {
        mHadCalledOnBind = false;
        mHadCalledOnUnbind = false;
        mHadCalledOnStart = false;
        mHadCalledOnDestory = false;
    }

    public static boolean hadCalledOnBind() {
        return mHadCalledOnBind;
    }

    public static boolean hadCalledOnUnbind() {
        return mHadCalledOnUnbind;
    }

    public static boolean hadCalledOnStart() {
        return mHadCalledOnStart;
    }

    public static boolean hadCalledOnDestory() {
        return mHadCalledOnDestory;
    }
}


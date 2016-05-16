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

package com.android.cts.verifier.p2p.testcase;


import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.util.Log;

/**
 * The utility class for testing android.net.wifi.p2p.WifiP2pManager.ActionListener
 * callback function.
 */
public class ActionListenerTest extends ListenerTest implements ActionListener {

    private static final String TAG = "ActionListenerTest";

    public static final Argument SUCCESS = new Argument();
    public static final Argument FAIL_NO_SERVICE =
            new Argument(WifiP2pManager.NO_SERVICE_REQUESTS);

    @Override
    public void onFailure(int reason) {
        Log.d(TAG, "onFailure() reason="+reason);
        Argument arg = new Argument(reason);
        receiveCallback(arg);
    }

    @Override
    public void onSuccess() {
        Log.d(TAG, "onSuccess()");
        Argument arg = new Argument();
        receiveCallback(arg);
    }

    /**
     * The container of the argument of {@link #onFailure} or
     * {@link #onSuccess}.
     */
    static class Argument extends ListenerArgument {

        boolean mIsSuccess;

        int mResult;

        /**
         * For {@link #onSuccess}
         */
        Argument() {
            mIsSuccess = true;
            mResult = 0;
        }

        /**
         * For {@link #onFailure}
         * @param result
         */
        Argument(int result) {
            mIsSuccess = false;
            mResult = result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Argument)) {
                return false;
            }
            Argument arg = (Argument)obj;
            return (mIsSuccess == arg.mIsSuccess) && (mResult == arg.mResult);
        }

        @Override
        public String toString() {
            return "isSuccess=" + mIsSuccess + " result=" + mResult;
        }
    }
}

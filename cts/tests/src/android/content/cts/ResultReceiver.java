/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class is used for testing android.content.ContentWrapper.
 *
 * @see ContextWrapperTest
 */
public class ResultReceiver extends BroadcastReceiver {
    public static final String MOCK_ACTION =
        "android.content.cts.ContextWrapperTest.BROADCAST_RESULT";

    private boolean mReceivedBroadCast;

    public void onReceive(Context context, Intent intent) {
        mReceivedBroadCast = MOCK_ACTION.equals(intent.getAction());
    }

    public boolean hasReceivedBroadCast() {
        return mReceivedBroadCast;
    }

    public void reset() {
        mReceivedBroadCast = false;
    }
}


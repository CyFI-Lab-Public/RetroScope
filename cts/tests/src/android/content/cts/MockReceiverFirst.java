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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * This class is used for testing android.content.BroadcastReceiver.
 *
 * @see BroadcastReceiver
 */
public class MockReceiverFirst extends BroadcastReceiver {
    public static final int RESULT_CODE = 2;
    public static final String RESULT_DATA = "first";
    public static final String RESULT_EXTRAS_FIRST_KEY = "first";
    public static final String RESULT_EXTRAS_FIRST_VALUE = "first value";

    public void onReceive(Context context, Intent intent) {
        Bundle map = getResultExtras(false);
        map.putString(RESULT_EXTRAS_FIRST_KEY, RESULT_EXTRAS_FIRST_VALUE);
        setResult(RESULT_CODE, RESULT_DATA, map);
    }
}

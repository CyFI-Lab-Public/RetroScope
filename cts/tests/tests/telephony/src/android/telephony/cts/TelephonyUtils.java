/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.telephony.cts;

import android.telephony.TelephonyManager;

class TelephonyUtils {

    public static boolean isSkt(TelephonyManager telephonyManager) {
        return isOperator(telephonyManager, "45005");
    }

    public static boolean isKt(TelephonyManager telephonyManager) {
        return isOperator(telephonyManager, "45002")
                || isOperator(telephonyManager, "45004")
                || isOperator(telephonyManager, "45008");
    }

    private static boolean isOperator(TelephonyManager telephonyManager, String operator) {
        String simOperator = telephonyManager.getSimOperator();
        return simOperator != null && simOperator.equals(operator);
    }

    private TelephonyUtils() {
    }
}

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

package android.net.wifi.cts;

import android.net.wifi.SupplicantState;
import android.test.AndroidTestCase;

public class SupplicantStateTest extends AndroidTestCase {

    public void testIsValidState() {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        assertTrue(SupplicantState.isValidState(SupplicantState.DISCONNECTED));
        assertTrue(SupplicantState.isValidState(SupplicantState.INACTIVE));
        assertTrue(SupplicantState.isValidState(SupplicantState.SCANNING));
        assertTrue(SupplicantState.isValidState(SupplicantState.ASSOCIATING));
        assertTrue(SupplicantState.isValidState(SupplicantState.ASSOCIATED));
        assertTrue(SupplicantState.isValidState(SupplicantState.FOUR_WAY_HANDSHAKE));
        assertTrue(SupplicantState.isValidState(SupplicantState.GROUP_HANDSHAKE));
        assertTrue(SupplicantState.isValidState(SupplicantState.COMPLETED));
        assertTrue(SupplicantState.isValidState(SupplicantState.DORMANT));
        assertFalse(SupplicantState.isValidState(SupplicantState.UNINITIALIZED));
        assertFalse(SupplicantState.isValidState(SupplicantState.INVALID));
    }

}

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

import android.content.Context;
import android.net.wifi.WpsInfo;

/**
 * Test case to try go negotiation with wps push button.
 */
public class GoNegReqPbcTestCase extends ConnectReqTestCase {

    public GoNegReqPbcTestCase(Context context) {
        super(context);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {
        return connectTest(false, WpsInfo.PBC);
    }

    @Override
    public String getTestName() {
        return "Go negotiation test (push button)";
    }
}

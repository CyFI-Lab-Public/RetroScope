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
package com.android.cts.verifier.p2p;

import android.content.Context;

import com.android.cts.verifier.p2p.testcase.GoNegReqTestSuite;
import com.android.cts.verifier.p2p.testcase.ReqTestCase;

/**
 * Test activity that tries to connect to the p2p device with group owner negotiation.
 * This activity is invoked from GoNegRequesterTestListActivity.
 */
public class GoNegRequesterTestActivity  extends RequesterTestActivity {

    @Override
    protected ReqTestCase getTestCase(Context context, String testId) {
        return GoNegReqTestSuite.getTestCase(context, testId);
    }
}

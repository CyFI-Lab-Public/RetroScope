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

import java.util.ArrayList;

import android.content.Context;

/**
 * Test suite to join a p2p group.
 */
public class P2pClientTestSuite {

    private static ArrayList<ReqTestCase> sTestSuite = null;

    /**
     * Return test suite.
     * @param context
     * @return
     */
    public static ArrayList<ReqTestCase> getTestSuite(Context context) {
        initialize(context);
        return sTestSuite;
    }

    /**
     * Return the specified test case.
     * @param context
     * @param testId
     * @return
     */
    public static ReqTestCase getTestCase(Context context,
            String testId) {
        initialize(context);

        for (ReqTestCase test: sTestSuite) {
            if (test.getTestId().equals(testId)) {
                return test;
            }
        }
        return null;
    }

    private static void initialize(Context context) {
        if (sTestSuite != null) {
            return;
        }

        sTestSuite = new ArrayList<ReqTestCase>();
        sTestSuite.add(new P2pClientPbcTestCase(context));
        sTestSuite.add(new P2pClientPinTestCase(context));
    }
}

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

public class ServReqTestSuite {

    private static ArrayList<ReqTestCase> sTestSuite = null;

    public static ArrayList<ReqTestCase> getTestSuite(Context context) {
        initialize(context);
        return sTestSuite;
    }

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
        sTestSuite.add(new ServReqAllTestCase01(context));
        sTestSuite.add(new ServReqAllTestCase02(context));
        sTestSuite.add(new ServReqAllTestCase03(context));
        sTestSuite.add(new ServReqDnsPtrTestCase(context));
        sTestSuite.add(new ServReqDnsTxtTestCase(context));
        sTestSuite.add(new ServReqUpnpAllTestCase(context));
        sTestSuite.add(new ServReqUpnpRootDeviceTestCase(context));
        sTestSuite.add(new ServReqRemoveRequestTestCase(context));
        sTestSuite.add(new ServReqClearRequestTestCase(context));
        sTestSuite.add(new ServReqMultiClientTestCase01(context));
        sTestSuite.add(new ServReqMultiClientTestCase02(context));
        sTestSuite.add(new ServReqMultiClientTestCase03(context));
    }
}

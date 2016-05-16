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

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;

import com.android.cts.verifier.R;
import com.android.cts.verifier.p2p.testcase.P2pClientTestSuite;
import com.android.cts.verifier.p2p.testcase.ReqTestCase;

/**
 * Activity that lists all the joining group owner tests.
 */
public class P2pClientTestListActivity extends RequesterTestListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setInfoResources(R.string.p2p_join_go,
                R.string.p2p_join_go_info, -1);
    }

    @Override
    protected ArrayList<ReqTestCase> getTestSuite(Context context) {
        return P2pClientTestSuite.getTestSuite(context);
    }

    @Override
    protected Class<?> getRequesterActivityClass() {
        return P2pClientTestActivity.class;
    }
}

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
import android.os.Bundle;

import com.android.cts.verifier.R;
import com.android.cts.verifier.p2p.testcase.GoTestCase;
import com.android.cts.verifier.p2p.testcase.TestCase;

/**
 * Test activity that accepts a connection from p2p client.
 */
public class GoTestActivity extends ResponderTestActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setInfoResources(R.string.p2p_accept_client,
                R.string.p2p_accept_client_info, -1);
    }

    @Override
    protected TestCase getTestCase(Context context) {
        return new GoTestCase(context);
    }

    @Override
    protected int getReadyMsgId() {
        return R.string.p2p_go_ready;
    }
}

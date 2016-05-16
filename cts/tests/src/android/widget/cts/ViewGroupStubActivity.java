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

package android.widget.cts;

import android.app.Activity;
import android.app.cts.CTSResult;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class ViewGroupStubActivity extends Activity {

    public static final String ACTION_INVALIDATE_CHILD = "invalidateChild";

    private final Handler mHandler = new Handler();
    private static CTSResult sResult;
    public static void setResult(CTSResult result) {
        sResult = result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.android.cts.stub.R.layout.viewgrouptest_stub);
        TextView textView = (TextView)findViewById(com.android.cts.stub.R.id.viewgrouptest_stub);
        textView.setText("test");
    }

    @Override
    protected void onResume() {
        super.onResume();

        String action = getIntent().getAction();
        if (action.equals(ACTION_INVALIDATE_CHILD)) {
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    MockLinearLayout mll =
                        (MockLinearLayout) findViewById(com.android.cts.stub.R.id.
                                                                        mocklinearlayout);
                    if (!mll.mIsInvalidateChildInParentCalled) {
                        fail();
                        return;
                    }
                    sResult.setResult(CTSResult.RESULT_OK);
                    finish();
                }
            }, 2000);
        }
    }

    private void fail() {
        sResult.setResult(CTSResult.RESULT_FAIL);
        finish();
    }
}


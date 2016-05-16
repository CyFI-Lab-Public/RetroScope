/*
 * Copyright 2012 The Android Open Source Project
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

package android.test.wrappedgtest;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class WrappedGTestInstrumentation extends Instrumentation {

    private static final String TAG = "WrappedGTestInstrumentation";
    private WrappedGTestActivity mActivity;
    protected Class mActivityClass;

    public WrappedGTestInstrumentation() {
    }

    @Override
    public void onCreate(Bundle arguments) {
        // attempt to disable keyguard,  if current test has permission to do so
        if (getContext().checkCallingOrSelfPermission(android.Manifest.permission.DISABLE_KEYGUARD)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Disabling keyguard");
            KeyguardManager keyguardManager =
                (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.newKeyguardLock("cts").disableKeyguard();
        } else {
            Log.i(TAG, "Test lacks permission to disable keyguard. " +
                    "UI based tests may fail if keyguard is up");
        }
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = new Intent(getTargetContext(), mActivityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mActivity = (WrappedGTestActivity)startActivitySync(intent);
        mActivity.setInstrumentation(this);
        mActivity.runGTests();

        finish(Activity.RESULT_OK, new Bundle());
    }
}

/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.deviceadmin.cts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.google.common.annotations.VisibleForTesting;

/**
 * Helper {@link Activity} for CTS tests of Device Admin activation. The {@code Activity}
 * enables tests to capture the invocations of its {@link #onActivityResult(int, int, Intent)} by
 * providing a {@link OnActivityResultListener}.
 */
public class CtsDeviceAdminActivationTestActivity extends Activity {
    public interface OnActivityResultListener {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    private OnActivityResultListener mOnActivityResultListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dismiss keyguard and keep screen on while this Activity is displayed.
        // This is needed because on older platforms, when the keyguard is on, onActivityResult is
        // not invoked when a Device Admin activation is rejected without user interaction.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @VisibleForTesting
    public void setOnActivityResultListener(OnActivityResultListener listener) {
        mOnActivityResultListener = listener;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mOnActivityResultListener != null) {
            mOnActivityResultListener.onActivityResult(requestCode, resultCode, data);
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}

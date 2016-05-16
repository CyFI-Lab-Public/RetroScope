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
package android.app.cts;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

public class KeyguardManagerActivity extends Activity {
    private static final String TAG = "KeyguardManagerActivity";
    public static final boolean DEBUG = false;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyLock;
    public int keyCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mKeyLock = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) {
            Log.d(TAG, "onResume");
        }
        if (mKeyLock == null) {
            mKeyLock = mKeyguardManager.newKeyguardLock(TAG);
            mKeyLock.disableKeyguard();
            if (DEBUG) {
                Log.d(TAG, "disableKeyguard");
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        this.keyCode = keyCode;
        if (keyCode == KeyEvent.KEYCODE_0 && mKeyLock != null) {
            mKeyLock.reenableKeyguard();
            mKeyLock = null;
            if (DEBUG) {
                Log.d(TAG, "reenableKeyguard");
            }
        }
        if (DEBUG) {
            Log.d(TAG, "onKeyDown");
        }
        return super.onKeyDown(keyCode, event);
    }
}

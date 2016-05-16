/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import android.app.Dialog;
import android.app.StatusBarManager;
import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.Window;
import android.os.Bundle;

/**
 * Base class for ICC-related panels in the Phone UI.
 */
public class IccPanel extends Dialog {
    protected static final String TAG = PhoneGlobals.LOG_TAG;

    private StatusBarManager mStatusBarManager;

    public IccPanel(Context context) {
        super(context, R.style.IccPanel);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window winP = getWindow();
        winP.setType(WindowManager.LayoutParams.TYPE_PRIORITY_PHONE);
        winP.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        winP.setGravity(Gravity.CENTER);

        // TODO: Ideally, we'd like this dialog to be visible in front of the
        // keyguard, so the user will see it immediately after boot (without
        // needing to enter the lock pattern or dismiss the keyguard first.)
        //
        // However that's not easy to do.  It's not just a matter of setting
        // the FLAG_SHOW_WHEN_LOCKED and FLAG_DISMISS_KEYGUARD flags on our
        // window, since we're a Dialog (not an Activity), and the framework
        // won't ever let a dialog hide the keyguard (because there could
        // possibly be stuff behind it that shouldn't be seen.)
        //
        // So for now, we'll live with the fact that the user has to enter the
        // lock pattern (or dismiss the keyguard) *before* being able to type
        // a SIM network unlock PIN.  That's not ideal, but still OK.
        // (And eventually this will be a moot point once this UI moves
        // from the phone app to the framework; see bug 1804111).

        // TODO: we shouldn't need the mStatusBarManager calls here either,
        // once this dialog gets moved into the framework and becomes a truly
        // full-screen UI.
        PhoneGlobals app = PhoneGlobals.getInstance();
        mStatusBarManager = (StatusBarManager) app.getSystemService(Context.STATUS_BAR_SERVICE);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
    }

    @Override
    public void onStop() {
        super.onStop();
        mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}

/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;

/**
 * <p>
 * A complete Mail activity instance. This is the toplevel class that creates the views and handles
 * the activity lifecycle.</p>
 *
 * <p>This class is abstract to allow many other activities to be quickly created by subclassing
 * this activity and overriding a small subset of the life cycle methods: for example
 * ComposeActivity and CreateShortcutActivity.</p>
 *
 * <p>In the Gmail codebase, this was called GmailBaseActivity</p>
 *
 */
public abstract class AbstractMailActivity extends Activity
        implements HelpCallback, RestrictedActivity {

    private final UiHandler mUiHandler = new UiHandler();

    private static final boolean STRICT_MODE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }

        super.onCreate(savedInstanceState);
        mUiHandler.setEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mUiHandler.setEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mUiHandler.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUiHandler.setEnabled(true);
    }

    /**
     * Get the contextual help parameter for this activity. This can be overridden
     * to allow the extending activities to return different help context strings.
     * The default implementation is to return "gm".
     * @return The help context of this activity.
     */
    @Override
    public String getHelpContext() {
        return "Mail";
    }

    @Override
    public Context getActivityContext() {
        return this;
    }
}

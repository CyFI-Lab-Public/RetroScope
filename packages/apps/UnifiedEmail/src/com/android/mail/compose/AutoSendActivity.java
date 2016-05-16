/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.compose;

import android.os.Bundle;

/**
 * An activity that will automatically queue a message, from the contents in
 * an ACTION_SEND intent
 *
 * AutoSendActivity extends ComposeActivity, since the logic that handles the SEND intent
 * is in ComposeActivity.
 */
public class AutoSendActivity extends ComposeActivity {
    // For testing, this extra will cause the message not be be saved or sent
    public static final String EXTRA_DONT_SEND_OR_SAVE = "dontSendOrSave";
    private boolean mDontSaveOrSend = false;


    // =============================================================================================
    // ComposeActivity methods
    // =============================================================================================
    /**
     * Returns a boolean indicating whether warnings should be shown for empty subject
     * and body fields
     *
     * @return True if a warning should be shown for empty text fields
     */
    @Override
    protected boolean showEmptyTextWarnings() {
        return false;
    }

    /**
     * Returns a boolean indicating whether the user should confirm each send
     * Since this is an auto send, the user doesn't confirm the send
     *
     * @return True if a warning should be on each send
     */
    @Override
    protected boolean showSendConfirmation() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDontSaveOrSend = getIntent().getBooleanExtra(EXTRA_DONT_SEND_OR_SAVE, false);
        sendOrSaveWithSanityChecks(false /* send */, true /* show  toast */);
    }

    protected boolean sendOrSaveWithSanityChecks(final boolean save,
            final boolean showToast) {
        if (mDontSaveOrSend) {
            return false;
        }
        return super.sendOrSaveWithSanityChecks(save, showToast, false, true);
    }
}

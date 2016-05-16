/*
 * Copyright (C) 2013 Google Inc.
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

package com.android.mail.browse;

import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import com.android.mail.compose.ComposeActivity;
import com.android.mail.providers.Account;

public class EmailAddressSpan extends URLSpan {

    private final Account mAccount;
    private final String mEmailAddress;

    public EmailAddressSpan(Account account, String emailAddress) {
        super("mailto:" + emailAddress);
        mAccount = account;
        mEmailAddress = emailAddress;
    }

    @Override
    public void onClick(View view) {
        ComposeActivity.composeToAddress(view.getContext(), mAccount, mEmailAddress);
    }

    /**
     * Makes the text in the link color and not underlined.
     */
    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(ds.linkColor);
        ds.setUnderlineText(false);
    }
}

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
package com.android.mail;

import android.text.Html;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a single email address.  Thread-safe, immutable value class suitable for
 * caching.
 * TODO(pwestbro): move to provider
 */
public class EmailAddress {
    public static final String LOG_TAG = LogTag.getLogTag();

    private final String mName;

    private final String mAddress;

    private static final Matcher sEmailMatcher =
            Pattern.compile("\\\"?([^\"<]*?)\\\"?\\s*<(.*)>").matcher("");

    private EmailAddress(String name, String address) {
        mName = name;
        mAddress = address;
    }

    public String getName() {
        return mName;
    }

    /**
     * @return either the parsed out e-mail address, or the full raw address if it is not in
     *     an expected format. This is not guaranteed to be HTML safe.
     */
    public String getAddress() {
        return mAddress;
    }

    // TODO (pwestbro): move to provider
    public static synchronized EmailAddress getEmailAddress(String rawAddress) {
        String name, address;
        if (rawAddress == null) {
            LogUtils.e(LOG_TAG, "null rawAddress in EmailAddress#getEmailAddress");
            rawAddress = "";
        }
        Matcher m = sEmailMatcher.reset(rawAddress);
        if (m.matches()) {
            name = m.group(1);
            address = m.group(2);
            if (name == null) {
                name = "";
            } else {
                name = Html.fromHtml(name.trim()).toString();
            }
            if (address == null) {
                address = "";
            } else {
                address = Html.fromHtml(address).toString();
            }
        } else {
            // Try and tokenize the string
            final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(rawAddress);
            if (tokens.length > 0) {
                final String tokenizedName = tokens[0].getName();
                name = tokenizedName != null ? Html.fromHtml(tokenizedName.trim()).toString() : "";
                address = Html.fromHtml(tokens[0].getAddress()).toString();
            } else {
                name = "";
                address = Html.fromHtml(rawAddress).toString();
            }
        }
        return new EmailAddress(name, address);
    }
}
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

package com.android.mail.providers;

import android.content.Context;
import android.content.Intent;

public class UnifiedAccountCacheProvider extends MailAppProvider {
    // The authority of our conversation provider (a forwarding provider)
    // This string must match the declaration in AndroidManifest.xml
    private static final String sAuthority = "com.android.mail.accountcache";
    /**
     * Authority for the suggestions provider. This is specified in AndroidManifest.xml and
     * res/xml/searchable.xml.
     */
    private static final String sSuggestionsAuthority = "com.android.mail.suggestionsprovider";

    @Override
    protected String getAuthority() {
        return sAuthority;
    }

    @Override
    protected Intent getNoAccountsIntent(Context context) {
        return null;
    }

    @Override
    public String getSuggestionAuthority() {
        return sSuggestionsAuthority;
    }
}

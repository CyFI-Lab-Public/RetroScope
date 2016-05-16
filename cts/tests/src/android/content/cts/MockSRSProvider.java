/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.content.cts;

import android.content.SearchRecentSuggestionsProvider;

public class MockSRSProvider extends SearchRecentSuggestionsProvider {
    final static String AUTHORITY = "android.content.cts.MockSRSProvider";
    final static int MODE = DATABASE_MODE_QUERIES + DATABASE_MODE_2LINES;

    public static boolean setupSuggestCalled;
    private boolean mOnCreateCalled;

    public MockSRSProvider() {
        super();
        setupSuggestions(AUTHORITY, MODE);
    }

    public MockSRSProvider(String tag) {
        super();
    }

    @Override
    public void setupSuggestions(String authority, int mode) {
        setupSuggestCalled = true;
        super.setupSuggestions(authority, mode);
    }

    @Override
    public boolean onCreate() {
        mOnCreateCalled = true;
        return super.onCreate();
    }

    public boolean isOnCreateCalled() {
        return mOnCreateCalled;
    }
}

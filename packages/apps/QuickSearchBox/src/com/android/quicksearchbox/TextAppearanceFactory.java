/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.quicksearchbox;

import android.content.Context;
import android.text.style.TextAppearanceSpan;

/**
 * Factory class for text appearances.
 */
public class TextAppearanceFactory {
    private final Context mContext;

    public TextAppearanceFactory(Context context) {
        mContext = context;
    }

    public Object[] createSuggestionQueryTextAppearance() {
        return new Object[]{
                new TextAppearanceSpan(mContext, R.style.SuggestionText1_Query)
        };
    }

    public Object[] createSuggestionSuggestedTextAppearance() {
        return new Object[]{
                new TextAppearanceSpan(mContext, R.style.SuggestionText1_Suggested)
        };
    }

}

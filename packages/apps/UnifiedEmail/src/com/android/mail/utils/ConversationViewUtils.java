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

package com.android.mail.utils;

import android.content.res.Resources;
import android.webkit.WebSettings;

import com.android.mail.R;

public class ConversationViewUtils {
    public static void setTextZoom(Resources resources, WebSettings settings) {
        final float fontScale = resources.getConfiguration().fontScale;
        final int desiredFontSizePx = resources.getInteger(
                R.integer.conversation_desired_font_size_px);
        final int unstyledFontSizePx = resources.getInteger(
                R.integer.conversation_unstyled_font_size_px);

        int textZoom = settings.getTextZoom();
        // apply a correction to the default body text style to get regular text to the size we want
        textZoom = textZoom * desiredFontSizePx / unstyledFontSizePx;
        // then apply any system font scaling
        textZoom = (int) (textZoom * fontScale);
        settings.setTextZoom(textZoom);
    }
}

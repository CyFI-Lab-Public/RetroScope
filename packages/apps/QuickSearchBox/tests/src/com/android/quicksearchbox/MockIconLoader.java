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

package com.android.quicksearchbox;

import com.android.quicksearchbox.util.Now;
import com.android.quicksearchbox.util.NowOrLater;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Mock implementation of {@link IconLoader}.
 *
 */
public class MockIconLoader implements IconLoader {

    private final Context mContext;

    public MockIconLoader(Context context) {
        mContext = context;
    }

    public NowOrLater<Drawable> getIcon(String drawableId) {
        if (TextUtils.isEmpty(drawableId) || "0".equals(drawableId)) {
            return new Now<Drawable>(null);
        } else {
            return new Now<Drawable>(
                    mContext.getResources().getDrawable(android.R.drawable.star_on));
        }
    }

    public Uri getIconUri(String drawableId) {
        if (TextUtils.isEmpty(drawableId) || "0".equals(drawableId)) {
            return null;
        }
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(mContext.getPackageName())
                .appendEncodedPath(String.valueOf(android.R.drawable.star_on))
                .build();
    }

}
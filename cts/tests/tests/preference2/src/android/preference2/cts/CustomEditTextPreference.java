/*
 * Copyright (C) 2012 The Android Open Source Project
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
package android.preference2.cts;

import com.android.cts.preference2.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class CustomEditTextPreference extends EditTextPreference {
    private Drawable mIcon;
    private String mTitle;
    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        setIcon(mIcon);
        this.setTitle(mTitle);
    }

    public CustomEditTextPreference(Context context) {
        super(context);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
        setIcon(mIcon);
        this.setTitle(mTitle);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.CustPref);
        mTitle =a.getString(R.styleable.CustPref_title);
        mIcon  = a.getDrawable(R.styleable.CustPref_icon);
    }
}

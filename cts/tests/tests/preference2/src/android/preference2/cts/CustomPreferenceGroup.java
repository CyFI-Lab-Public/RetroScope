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
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.AttributeSet;

public class CustomPreferenceGroup extends PreferenceGroup {
    protected boolean mOnPrepareCalled;

    public CustomPreferenceGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CustomPreferenceGroup(Context context) {
        super(context, null, 0);
    }

    public CustomPreferenceGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.CustPref);
        setTitle(a.getString(R.styleable.CustPref_title));
        setIcon(a.getDrawable(R.styleable.CustPref_icon));
    }

    public boolean isOnSameScreenAsChildren() {
        return super.isOnSameScreenAsChildren();
    }

    public boolean onPrepareAddPreference (Preference preference) {
        mOnPrepareCalled = true;
        return super.onPrepareAddPreference(preference);
    }
}

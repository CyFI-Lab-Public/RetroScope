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
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class CustomPreference extends Preference {
    protected boolean mOnPrepareCalled;

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CustomPreference(Context context) {
        this(context, null, 0);
    }

    public CustomPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.CustPref);
        setTitle(a.getString(R.styleable.CustPref_title));
        setIcon(a.getDrawable(R.styleable.CustPref_icon));
    }
    
    @Override
    protected boolean callChangeListener(Object newValue) {
        return super.callChangeListener(newValue);
    }

    @Override
    protected Preference findPreferenceInHierarchy(String key) {
        return super.findPreferenceInHierarchy(key);
    }

    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        return super.getPersistedBoolean(defaultReturnValue);
    }

    @Override
    protected float getPersistedFloat(float defaultReturnValue) {
        return super.getPersistedFloat(defaultReturnValue);
    }

    @Override
    protected int getPersistedInt(int defaultReturnValue) { 
        return super.getPersistedInt(defaultReturnValue);
    }

    @Override
    protected long getPersistedLong(long defaultReturnValue) {
        return super.getPersistedLong(defaultReturnValue);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return super.getPersistedString(defaultReturnValue);
    }

    @Override
    protected void notifyChanged() {
        super.notifyChanged();
    }

    @Override
    protected void notifyHierarchyChanged() {
        super.notifyHierarchyChanged();
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    protected void onClick() {
        super.onClick();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return super.onCreateView(parent);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return super.onGetDefaultValue(a, index);
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
       
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
            Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        return super.persistBoolean(value);
    }

    @Override
    protected boolean persistFloat(float value) {
        return super.persistFloat(value);
    }

    @Override
    protected boolean persistInt(int value) {
        return super.persistInt(value);
    }

    @Override
    protected boolean persistLong(long value) {
        return super.persistLong(value);
    }

    @Override
    protected boolean persistString(String value) {
        return super.persistString(value);
    }

    @Override
    protected boolean shouldPersist() {  
        return super.shouldPersist();
    } 
}


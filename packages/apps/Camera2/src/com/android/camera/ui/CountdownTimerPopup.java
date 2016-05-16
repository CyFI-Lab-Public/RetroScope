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

package com.android.camera.ui;

import java.util.Locale;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;

import com.android.camera.ListPreference;
import com.android.camera2.R;

/**
 * This is a popup window that allows users to specify a countdown timer
 */

public class CountdownTimerPopup extends AbstractSettingPopup {
    private static final String TAG = "TimerSettingPopup";
    private NumberPicker mNumberSpinner;
    private String[] mDurations;
    private ListPreference mTimer;
    private ListPreference mBeep;
    private Listener mListener;
    private Button mConfirmButton;
    private View mPickerTitle;
    private CheckBox mTimerSound;
    private View mSoundTitle;

    static public interface Listener {
        public void onListPrefChanged(ListPreference pref);
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public CountdownTimerPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(ListPreference timer, ListPreference beep) {
        mTimer = timer;
        mBeep = beep;
        // Set title.
        mTitle.setText(mTimer.getTitle());

        // Duration
        CharSequence[] entries = mTimer.getEntryValues();
        mDurations = new String[entries.length];
        Locale locale = getResources().getConfiguration().locale;
        mDurations[0] = getResources().getString(R.string.setting_off); // Off
        for (int i = 1; i < entries.length; i++)
            mDurations[i] =  String.format(locale, "%d", Integer.parseInt(entries[i].toString()));
        int durationCount = mDurations.length;
        mNumberSpinner = (NumberPicker) findViewById(R.id.duration);
        mNumberSpinner.setMinValue(0);
        mNumberSpinner.setMaxValue(durationCount - 1);
        mNumberSpinner.setDisplayedValues(mDurations);
        mNumberSpinner.setWrapSelectorWheel(false);
        mNumberSpinner.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
                setTimeSelectionEnabled(newValue != 0);
            }
        });
        mConfirmButton = (Button) findViewById(R.id.timer_set_button);
        mPickerTitle = findViewById(R.id.set_time_interval_title);

        // Disable focus on the spinners to prevent keyboard from coming up
        mNumberSpinner.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateInputState();
            }
        });
        mTimerSound = (CheckBox) findViewById(R.id.sound_check_box);
        mSoundTitle = findViewById(R.id.beep_title);
    }

    private void restoreSetting() {
        int index = mTimer.findIndexOfValue(mTimer.getValue());
        if (index == -1) {
            Log.e(TAG, "Invalid preference value.");
            mTimer.print();
            throw new IllegalArgumentException();
        } else {
            setTimeSelectionEnabled(index != 0);
            mNumberSpinner.setValue(index);
        }
        boolean checked = mBeep.findIndexOfValue(mBeep.getValue()) != 0;
        mTimerSound.setChecked(checked);
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == View.VISIBLE) {
            if (getVisibility() != View.VISIBLE) {
                // Set the number pickers and on/off switch to be consistent
                // with the preference
                restoreSetting();
            }
        }
        super.setVisibility(visibility);
    }

    protected void setTimeSelectionEnabled(boolean enabled) {
        mPickerTitle.setVisibility(enabled ? VISIBLE : INVISIBLE);
        mTimerSound.setEnabled(enabled);
        mSoundTitle.setEnabled(enabled);
    }

    @Override
    public void reloadPreference() {
    }

    private void updateInputState() {
        mTimer.setValueIndex(mNumberSpinner.getValue());
        mBeep.setValueIndex(mTimerSound.isChecked() ? 1 : 0);
        if (mListener != null) {
            mListener.onListPrefChanged(mTimer);
            mListener.onListPrefChanged(mBeep);
        }
    }
}

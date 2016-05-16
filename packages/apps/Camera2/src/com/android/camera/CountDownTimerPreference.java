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

package com.android.camera;

import android.content.Context;
import android.util.AttributeSet;

import com.android.camera2.R;

public class CountDownTimerPreference extends ListPreference {
    private static final int[] DURATIONS = {
        0, 1, 2, 3, 4, 5, 10, 15, 20, 30, 60
    };
    public CountDownTimerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCountDownDurationChoices(context);
    }

    private void initCountDownDurationChoices(Context context) {
        CharSequence[] entryValues = new CharSequence[DURATIONS.length];
        CharSequence[] entries = new CharSequence[DURATIONS.length];
        for (int i = 0; i < DURATIONS.length; i++) {
            entryValues[i] = Integer.toString(DURATIONS[i]);
            if (i == 0) {
                entries[0] = context.getString(R.string.setting_off); // Off
            } else {
                entries[i] = context.getResources()
                        .getQuantityString(R.plurals.pref_camera_timer_entry, i, i);
            }
        }
        setEntries(entries);
        setEntryValues(entryValues);
    }
}

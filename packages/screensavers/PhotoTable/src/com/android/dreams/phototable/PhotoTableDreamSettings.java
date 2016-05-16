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
package com.android.dreams.phototable;

import android.os.Bundle;

/**
 * Settings panel for photo flipping dream.
 */
public class PhotoTableDreamSettings extends FlipperDreamSettings {
    @SuppressWarnings("unused")
    private static final String TAG = "PhotoTableDreamSettings";
    public static final String PREFS_NAME = PhotoTableDream.TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        init();
    }
}

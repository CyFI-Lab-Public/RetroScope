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

package com.android.mail.ui;

import android.app.Fragment;

import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

/**
 * Small Runnable-like wrapper that first checks that the Fragment is in a good state before
 * doing any work. Ideal for use with a {@link android.os.Handler}.
 */
public abstract class FragmentRunnable implements Runnable {
    private static final String LOG_TAG = LogTag.getLogTag();

    private final String mOpName;
    private final Fragment mFragment;

    public FragmentRunnable(String opName, Fragment fragment) {
        mOpName = opName;
        mFragment = fragment;
    }

    public abstract void go();

    @Override
    public void run() {
        if (!mFragment.isAdded()) {
            LogUtils.i(LOG_TAG, "Unable to run op='%s' b/c fragment is not attached: %s",
                    mOpName, mFragment);
            return;
        }
        go();
    }
}
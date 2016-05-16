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
package com.android.gallery3d.util;

import com.android.gallery3d.common.Utils;

public class UpdateHelper {

    private boolean mUpdated = false;

    public int update(int original, int update) {
        if (original != update) {
            mUpdated = true;
            original = update;
        }
        return original;
    }

    public long update(long original, long update) {
        if (original != update) {
            mUpdated = true;
            original = update;
        }
        return original;
    }

    public double update(double original, double update) {
        if (original != update) {
            mUpdated = true;
            original = update;
        }
        return original;
    }

    public <T> T update(T original, T update) {
        if (!Utils.equals(original, update)) {
            mUpdated = true;
            original = update;
        }
        return original;
    }

    public boolean isUpdated() {
        return mUpdated;
    }
}

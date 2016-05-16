/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.holo.cts;

import android.content.Intent;

import java.util.Iterator;

/**
 * {@link Iterator} over all the possible permutations of theme and layout intents.
 */
class AllThemesIterator implements Iterator<Intent> {

    private final ThemeAdapter mThemeAdapter = new ThemeAdapter(null);
    private final LayoutAdapter mLayoutAdapter;

    private final int mTask;
    private final int mAdapterMode;
    private int mThemeIndex;
    private int mLayoutIndex;

    AllThemesIterator(int task, int adapterMode) {
        mTask = task;
        mAdapterMode = adapterMode;
        mLayoutAdapter = new LayoutAdapter(null, adapterMode);
    }

    @Override
    public boolean hasNext() {
        return mThemeIndex < mThemeAdapter.getCount();
    }

    @Override
    public Intent next() {
        Intent intent = new Intent();
        intent.putExtra(LayoutTestActivity.EXTRA_THEME_INDEX, mThemeIndex);
        intent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_INDEX, mLayoutIndex);
        intent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_ADAPTER_MODE, mAdapterMode);
        intent.putExtra(LayoutTestActivity.EXTRA_TASK, mTask);

        mLayoutIndex++;
        if (mLayoutIndex >= mLayoutAdapter.getCount()) {
            mThemeIndex++;
            mLayoutIndex = 0;
        }

        return intent;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("I don't think so...");
    }
}

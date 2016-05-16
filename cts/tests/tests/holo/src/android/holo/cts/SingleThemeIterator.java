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
 * {@link Iterator} over a single theme and all the layouts.
 */
class SingleThemeIterator implements Iterator<Intent> {

    private final LayoutAdapter mLayoutAdapter;

    private final int mTask;
    private final int mThemeIndex;
    private final int mLayoutAdapterMode;
    private int mLayoutIndex;

    SingleThemeIterator(int themeIndex, int task, int layoutAdapterMode) {
        mTask = task;
        mLayoutAdapterMode = layoutAdapterMode;
        mLayoutAdapter = new LayoutAdapter(null, layoutAdapterMode);
        mThemeIndex = themeIndex;
    }

    @Override
    public boolean hasNext() {
        return mLayoutIndex < mLayoutAdapter.getCount();
    }

    @Override
    public Intent next() {
        Intent intent = new Intent();
        intent.putExtra(LayoutTestActivity.EXTRA_THEME_INDEX, mThemeIndex);
        intent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_INDEX, mLayoutIndex);
        intent.putExtra(LayoutTestActivity.EXTRA_TASK, mTask);
        intent.putExtra(LayoutTestActivity.EXTRA_LAYOUT_ADAPTER_MODE, mLayoutAdapterMode);

        mLayoutIndex++;

        return intent;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Please stop! That tickles!");
    }
}

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

package com.android.gallery3d.ingest.adapter;

import java.util.ArrayList;
import java.util.Collection;

public abstract class CheckBroker {
    private Collection<OnCheckedChangedListener> mListeners =
            new ArrayList<OnCheckedChangedListener>();

    public interface OnCheckedChangedListener {
        public void onCheckedChanged(int position, boolean isChecked);
        public void onBulkCheckedChanged();
    }

    public abstract void setItemChecked(int position, boolean checked);

    public void onCheckedChange(int position, boolean checked) {
        if (isItemChecked(position) != checked) {
            for (OnCheckedChangedListener l : mListeners) {
                l.onCheckedChanged(position, checked);
            }
        }
    }

    public void onBulkCheckedChange() {
        for (OnCheckedChangedListener l : mListeners) {
            l.onBulkCheckedChanged();
        }
    }

    public abstract boolean isItemChecked(int position);

    public void registerOnCheckedChangeListener(OnCheckedChangedListener l) {
        mListeners.add(l);
    }

    public void unregisterOnCheckedChangeListener(OnCheckedChangedListener l) {
        mListeners.remove(l);
    }
}

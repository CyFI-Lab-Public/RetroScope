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

package android.content.cts;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.os.Bundle;

public class ClipboardManagerListenerActivity extends Activity {

    private ClipboardManager mClipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        synchronized(this) {
            mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        }
    }

    public synchronized void addPrimaryClipChangedListener(OnPrimaryClipChangedListener listener) {
        mClipboardManager.addPrimaryClipChangedListener(listener);
    }

    public synchronized void removePrimaryClipChangedListener(OnPrimaryClipChangedListener listener) {
        mClipboardManager.removePrimaryClipChangedListener(listener);
    }

    public synchronized void setPrimaryClip(ClipData clip) {
        mClipboardManager.setPrimaryClip(clip);
    }
}

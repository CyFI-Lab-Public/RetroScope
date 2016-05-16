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
package com.android.gallery3d.app;

import com.android.gallery3d.util.IntentHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Trampoline activity that launches the Gallery activity defined in IntentHelper. */
public class Gallery extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = IntentHelper.getGalleryIntent(Gallery.this);
        // Since this is being launched from a homescreen shortcut,
        // it's already in a new task. Start Gallery activity and
        // reset the task to its initial state if needed.
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(intent);
        finish();
    }
}

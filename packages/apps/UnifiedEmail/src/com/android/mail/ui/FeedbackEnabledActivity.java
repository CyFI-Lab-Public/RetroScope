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

import android.app.Activity;
import android.content.Context;
import android.view.Window;

/**
 * {@link FeedbackEnabledActivity} gives access to a subset of {@link android.app.Activity}
 * which are required for reporting feedback.
 * These methods match the signatures from {@link android.app.Activity}.
 */
public interface FeedbackEnabledActivity {
    /**
     * @see android.app.Activity#getWindow()
     */
    Window getWindow();

    /**
     * Returns the context associated with the activity. This is different from the value returned
     * by {@link Activity#getApplicationContext()}, which is the single context of the root
     * activity. Some components (dialogs) require the context of the activity. When implementing
     * this, you can return this, since each activity is also a context.
     * @return the context associated with this activity.
     */
    Context getActivityContext();

}

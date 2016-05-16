/*******************************************************************************
 *      Copyright (C) 2013 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.analytics;

import android.app.Activity;

/**
 * Analytics libraries should implement this interface.
 */
public interface Tracker {

    void activityStart(Activity a);
    void activityStop(Activity a);
    void sendEvent(String category, String action, String label, long value);
    void sendMenuItemEvent(String category, int itemResId, String label, long value);
    void sendView(String view);
    void setCustomDimension(int index, String value);
    void setCustomMetric(int index, Long value);

    void debugDispatchNow();

}

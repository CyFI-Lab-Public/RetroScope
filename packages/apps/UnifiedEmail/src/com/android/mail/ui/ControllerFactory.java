/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
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

package com.android.mail.ui;


/**
 * Creates the appropriate {@link ActivityController} to control {@link MailActivity}.
 *
 */
public class ControllerFactory {
    /**
     * Create the appropriate type of ActivityController.
     *
     * @return the appropriate {@link ActivityController} to control {@link MailActivity}.
     */
    public static ActivityController forActivity(MailActivity activity, ViewMode viewMode,
            boolean isTabletDevice) {
        return isTabletDevice ? new TwoPaneController(activity, viewMode)
        : new OnePaneController(activity, viewMode);
    }
}

/*
 * Copyright (C) 2012 Google Inc.
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

package com.android.mail.browse;

public interface AttachmentViewInterface {

    /**
     * View an attachment. The different attachment types handle this
     * action differently and so each view handles it in their
     * own manner.
     */
    public void viewAttachment();

    /**
     * Allows the view to know when it should update its progress.
     * @param showDeterminateProgress true if the the view should show a determinate
     * progress value
     */
    public void updateProgress(boolean showDeterminateProgress);

    /**
     * Allows the view to do some view-specific status updating.
     * Called in {@link AttachmentActionHandler#updateStatus}.
     */
    public void onUpdateStatus();
}

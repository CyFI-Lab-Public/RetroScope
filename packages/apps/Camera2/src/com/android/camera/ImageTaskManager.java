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

package com.android.camera;

import android.net.Uri;

/**
 * The interface for background image processing task manager.
 */
public interface ImageTaskManager {

    /**
     * Callback interface for task events.
     */
    public interface TaskListener {
        public void onTaskQueued(String filePath, Uri imageUri);
        public void onTaskDone(String filePath, Uri imageUri);
        public void onTaskProgress(
                String filePath, Uri imageUri, int progress);
    }

    public void addTaskListener(TaskListener l);

    public void removeTaskListener(TaskListener l);

    /**
     * Get task progress by Uri.
     *
     * @param uri         The Uri of the final image file to identify the task.
     * @return            Integer from 0 to 100, or -1. The percentage of the task done
     *                    so far. -1 means not found.
     */
    public int getTaskProgress(Uri uri);
}

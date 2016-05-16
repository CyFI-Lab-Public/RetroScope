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
 * A listener interested in specific layout changes.  This is only useful for a layout that can show
 * both conversation lists and conversations at the same time. For example, tablets can do this, but
 * not most phones.
 */
public interface LayoutListener {
    /**
     * Called when the conversation list changes its visibility.
     * @param visible True if the conversation list is now visible. False otherwise
     */
    void onConversationListVisibilityChanged(boolean visible);

    /**
     * Called when the conversation view changes its visibility.
     * @param visible True if the conversation list is now visible. False otherwise
     */
    void onConversationVisibilityChanged(boolean visible);
}

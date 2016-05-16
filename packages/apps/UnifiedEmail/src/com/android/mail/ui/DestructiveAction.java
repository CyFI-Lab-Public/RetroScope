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

package com.android.mail.ui;

import com.android.mail.browse.SelectedConversationsActionMenu;

/**
 * Interface for listening to completed UI actions like Archive, Delete, star, etc.
 * Implement this interface and use this interface in the constructor of
 * {@link SelectedConversationsActionMenu}. Once the actions are completed, the method
 * {@link #performAction()} will get called.
 */
public interface DestructiveAction {
    /**
     * Performs the destructive action.
     */
    public void performAction();
}

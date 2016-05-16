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

import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Smoke;

import com.android.mail.ui.ViewMode.ModeChangeListener;

@Smoke
public class ViewModeTests extends AndroidTestCase {
    /**
     * Saving and restoring a view mode work correctly.
     */
    @SmallTest
    public void testBundleSaveRestorePreserveState() {
        Bundle state = new Bundle();
        ViewMode first = new ViewMode();
        // Set the state to something known.
        first.enterConversationListMode();
        first.handleSaveInstanceState(state);
        ViewMode second = new ViewMode();
        second.handleRestore(state);
        assertEquals(ViewMode.CONVERSATION_LIST, second.getMode());
    }

    /**
     * Register a listener for mode changes. Change a mode, and verify that the listener was
     * called. Then unregister the listener and change the mode again. Verify that the listener
     * was NOT called.
     */
    @SmallTest
    public void testListenerCalledAfterRegistering() {
        class Ears implements ModeChangeListener {
            public int numCalls = 0;
            @Override
            public void onViewModeChanged(int mode) {
                numCalls++;
            }
        }

        ViewMode mode = new ViewMode();
        Ears ears = new Ears();
        mode.addListener(ears);
        mode.enterConversationListMode();
        assertEquals(ears.numCalls, 1);
        mode.removeListener(ears);
        mode.enterConversationMode();
        assertEquals(ears.numCalls, 1);
    }

    /**
     * The view mode can transition to a state only if it isn't already in that state.
     */
    @SmallTest
    public void testMultipleTransitionsFail() {
        ViewMode first = new ViewMode();
        // Set the state to something known.
        first.enterConversationListMode();
        assertEquals(ViewMode.CONVERSATION_LIST, first.getMode());
        // Cannot transition to Conversation List mode. I'm in it already.
        first.enterConversationListMode();
        assertEquals(ViewMode.CONVERSATION_LIST, first.getMode());
    }
}
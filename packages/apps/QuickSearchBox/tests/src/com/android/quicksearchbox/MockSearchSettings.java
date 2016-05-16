/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.quicksearchbox;

import android.content.Intent;
import android.view.Menu;

/**
 * Mock implementation of {@link SearchSettings}.
 */
public class MockSearchSettings implements SearchSettings {

    public void addMenuItems(Menu menu, boolean showDisabled) {
    }

    public void broadcastSettingsChanged() {
    }

    public void upgradeSettingsIfNeeded() {
    }

    public void resetVoiceSearchHintFirstSeenTime() {
    }

    public boolean haveVoiceSearchHintsExpired(int currentVoiceSearchVersion) {
        return false;
    }

    public int getNextVoiceSearchHintIndex(int size) {
        return 0;
    }

    public boolean shouldUseGoogleCom() {
        return true;
    }

    public void setUseGoogleCom(boolean useGoogleCom) {
        // Do nothing.
    }

    public long getSearchBaseDomainApplyTime() {
        return -1L;
    }

    public String getSearchBaseDomain() {
        return "www.google.com";
    }

    public void setSearchBaseDomain(String searchBaseUrl) {
        // Do nothing.
    }

}

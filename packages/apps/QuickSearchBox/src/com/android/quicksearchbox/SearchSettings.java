/*
 * Copyright (C) 2009 The Android Open Source Project
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


/**
 * Interface for search settings.
 *
 * NOTE: Currently, this is not used very widely, in most instances
 * implementers of this interface are passed around by class name.
 * Should this be deprecated ?
 */
public interface SearchSettings {

    public void upgradeSettingsIfNeeded();

    /**
     * Informs our listeners about the updated settings data.
     */
    public void broadcastSettingsChanged();

    public int getNextVoiceSearchHintIndex(int size);

    public void resetVoiceSearchHintFirstSeenTime();

    public boolean haveVoiceSearchHintsExpired(int currentVoiceSearchVersion);

    /**
     * Determines whether google.com should be used as the base path
     * for all searches (as opposed to using its country specific variants).
     */
    public boolean shouldUseGoogleCom();

    public void setUseGoogleCom(boolean useGoogleCom);

    public long getSearchBaseDomainApplyTime();

    public String getSearchBaseDomain();

    public void setSearchBaseDomain(String searchBaseUrl);
}

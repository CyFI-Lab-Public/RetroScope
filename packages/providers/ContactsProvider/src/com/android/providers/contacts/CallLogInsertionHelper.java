/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.ContentValues;

/**
 * Helper class to be used when inserting values in the call log.
 */
public interface CallLogInsertionHelper {
    /** Adds to the content values those key/value pairs which needs to added automatically. */
    public void addComputedValues(ContentValues values);
    /** Returns the geocoded location for a given phone number. */
    public String getGeocodedLocationFor(String number, String countryIso);
}

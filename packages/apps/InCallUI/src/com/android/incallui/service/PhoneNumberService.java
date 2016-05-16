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
 * limitations under the License
 */

package com.android.incallui.service;

import android.graphics.Bitmap;

/**
 * Provides phone number lookup services.
 */
public interface PhoneNumberService {

    /**
     * Get a phone number number asynchronously.
     *
     * @param phoneNumber The phone number to lookup.
     * @param listener The listener to notify when the phone number lookup is complete.
     * @param imageListener The listener to notify when the image lookup is complete.
     */
    public void getPhoneNumberInfo(String phoneNumber, NumberLookupListener listener,
            ImageLookupListener imageListener, boolean isIncoming);

    public interface NumberLookupListener {

        /**
         * Callback when a phone number has been looked up.
         *
         * @param info The looked up information.  Or (@literal null} if there are no results.
         */
        public void onPhoneNumberInfoComplete(PhoneNumberInfo info);
    }

    public interface ImageLookupListener {

        /**
         * Callback when a image has been fetched.
         *
         * @param bitmap The fetched image.
         */
        public void onImageFetchComplete(Bitmap bitmap);
    }

    public interface PhoneNumberInfo {
        public String getDisplayName();
        public String getNumber();
        public int getPhoneType();
        public String getPhoneLabel();
        public String getNormalizedNumber();
        public String getImageUrl();
        public boolean isBusiness();
    }
}

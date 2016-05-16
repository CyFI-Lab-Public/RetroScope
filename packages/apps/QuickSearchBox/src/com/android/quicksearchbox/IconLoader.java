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

import com.android.quicksearchbox.util.NowOrLater;

import android.content.ContentResolver;
import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * Interface for icon loaders.
 *
 */
public interface IconLoader {

    /**
     * Gets a drawable given an ID.
     *
     * The ID could be just the string value of a resource id
     * (e.g., "2130837524"), in which case we will try to retrieve a drawable from
     * the provider's resources. If the ID is not an integer, it is
     * treated as a Uri and opened with
     * {@link ContentResolver#openOutputStream(android.net.Uri, String)}.
     *
     * All resources and URIs are read using the suggestion provider's context.
     *
     * @return a {@link NowOrLater} for retrieving the icon. If the ID is not formatted as expected,
     *      or no drawable can be found for the provided value, the value from this will be null.
     *
     * @param drawableId a string like "2130837524",
     *        "android.resource://com.android.alarmclock/2130837524",
     *        or "content://contacts/photos/253".
     */
    NowOrLater<Drawable> getIcon(String drawableId);

    /**
     * Converts a drawable ID to a Uri that can be used from other packages.
     */
    Uri getIconUri(String drawableId);

}

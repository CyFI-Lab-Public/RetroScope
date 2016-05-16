/*
 * Copyright (C) 2013 Google Inc.
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

package com.android.mail.utils;

import android.net.Uri;

/**
 * A holder for a Folder {@link Uri} that can be compared, ignoring any query parameters.
 */
public class FolderUri {
    public static final FolderUri EMPTY = new FolderUri(Uri.EMPTY);

    /**
     * The full {@link Uri}. This should be used for any queries.
     */
    public final Uri fullUri;
    /**
     * Equivalent to {@link #fullUri}, but without any query parameters, and can safely be used in
     * comparisons to determine if two {@link Uri}s point to the same object.
     */
    private Uri mComparisonUri = null;

    public FolderUri(final Uri uri) {
        fullUri = uri;
    }

    private static Uri buildComparisonUri(final Uri fullUri) {
        final Uri.Builder builder = new Uri.Builder();
        builder.scheme(fullUri .getScheme());
        builder.encodedAuthority(fullUri.getEncodedAuthority());
        builder.encodedPath(fullUri.getEncodedPath());

        return builder.build();
    }

    public Uri getComparisonUri() {
        if (mComparisonUri == null) {
            mComparisonUri = buildComparisonUri(fullUri);
        }

        return mComparisonUri;
    }

    @Override
    public int hashCode() {
        return getComparisonUri().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof FolderUri) {
            return getComparisonUri().equals(((FolderUri) o).getComparisonUri());
        }

        return getComparisonUri().equals(o);
    }

    @Override
    public String toString() {
        return fullUri.toString();
    }
}

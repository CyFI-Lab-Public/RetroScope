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
package com.android.providers.contacts.util;

import android.content.UriMatcher;
import android.net.Uri;

/**
 * Implementation of {@link TypedUriMatcher}.
 *
 * @param <T> the type of the URI
 */
public class TypedUriMatcherImpl<T extends UriType> implements TypedUriMatcher<T> {
    private final String mAuthority;
    private final T[] mValues;
    private final T mNoMatchUriType;
    private final UriMatcher mUriMatcher;

    public TypedUriMatcherImpl(String authority, T[] values) {
        mAuthority = authority;
        mValues = values;
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        T candidateNoMatchUriType = null;
        for (T value : values) {
            String path = value.path();
            if (path != null) {
                addUriType(path, value);
            } else {
                candidateNoMatchUriType = value;
            }
        }
        this.mNoMatchUriType = candidateNoMatchUriType;
    }

    private void addUriType(String path, T value) {
        mUriMatcher.addURI(mAuthority, path, value.ordinal());
    }

    @Override
    public T match(Uri uri) {
        int match = mUriMatcher.match(uri);
        if (match == UriMatcher.NO_MATCH) {
            return mNoMatchUriType;
        }
        return mValues[match];
    }
}

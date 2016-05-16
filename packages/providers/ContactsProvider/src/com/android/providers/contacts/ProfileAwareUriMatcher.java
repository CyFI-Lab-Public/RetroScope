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
 * limitations under the License.
 */

package com.android.providers.contacts;

import android.content.UriMatcher;
import android.net.Uri;
import android.provider.ContactsContract;

import com.google.android.collect.Lists;
import com.google.android.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A subclass of URI matcher with additional logic and awareness around profile-specific URIs.
 */
public class ProfileAwareUriMatcher extends UriMatcher {

    private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile("/");

    private static final String PROFILE_SEGMENT = "profile";
    private static final String LOOKUP_SEGMENT = "lookup";
    private static final String VCARD_SEGMENT = "as_vcard";
    private static final String ID_SEGMENT = "#";
    private static final String WILDCARD_SEGMENT = "*";

    // URIs matching to these constants must always use the profile database.
    private static final List<Integer> PROFILE_URIS = Lists.newArrayList();

    // URIs in this map will direct to the profile database if the ID (which is at the segment
    // path with the location specified by the value in the map) is in the profile ID-space.
    private static final Map<Integer, Integer> PROFILE_URI_ID_MAP = Maps.newHashMap();

    // URIs in this map will direct to the profile database if the lookup key (which is at the
    // segment path with the location specified by the value in the map) is the special profile
    // lookup key (see {@link ProfileAggregator#PROFILE_LOOKUP_KEY}).
    private static final Map<Integer, Integer> PROFILE_URI_LOOKUP_KEY_MAP = Maps.newHashMap();

    /**
     * Creates the root node of the URI tree.
     *
     * @param code the code to match for the root URI
     */
    public ProfileAwareUriMatcher(int code) {
        super(code);
    }

    @Override
    public void addURI(String authority, String path, int code) {
        super.addURI(authority, path, code);

        // Do a second tokenization pass to determine whether the URI may apply to profiles.
        if (path != null) {
            String[] tokens = PATH_SPLIT_PATTERN.split(path);
            if (tokens != null) {

                // Keep track of whether we've passed a "lookup" token in the path; wildcards after
                // that token will be interpreted as lookup keys.  For our purposes, vcard paths
                // also count as lookup tokens, since the vcard is specified by lookup key.
                boolean afterLookup = false;
                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    if (token.equals(PROFILE_SEGMENT)) {
                        PROFILE_URIS.add(code);
                        return;
                    } else if (token.equals(LOOKUP_SEGMENT)
                            || token.equals(VCARD_SEGMENT)) {
                        afterLookup = true;
                        continue;
                    } else if (token.equals(ID_SEGMENT)) {
                        PROFILE_URI_ID_MAP.put(code, i);
                    } else if (token.equals(WILDCARD_SEGMENT)) {
                        if (afterLookup) {
                            PROFILE_URI_LOOKUP_KEY_MAP.put(code, i);
                        }
                    }
                    afterLookup = false;
                }
            }
        }
    }

    /**
     * Determines whether the given URI is intended for the profile DB rather than contacts.
     * This is true under any of three conditions:
     * 1. The URI itself is specifically for the profile (it contains a "profile" segment).
     * 2. The URI contains ID references that are in the profile ID-space.
     * 3. The URI contains lookup key references that match the special profile lookup key.
     * @param uri The URI to examine.
     * @return Whether the operation for this URI is intended for the profile DB.
     */
    public boolean mapsToProfile(Uri uri) {
        int match = match(uri);
        if (PROFILE_URIS.contains(match)) {
            return true;
        } else if (PROFILE_URI_ID_MAP.containsKey(match)) {
            int idSegment = PROFILE_URI_ID_MAP.get(match);
            long id = Long.parseLong(uri.getPathSegments().get(idSegment));
            if (ContactsContract.isProfileId(id)) {
                return true;
            }
        } else if (PROFILE_URI_LOOKUP_KEY_MAP.containsKey(match)) {
            int lookupKeySegment = PROFILE_URI_LOOKUP_KEY_MAP.get(match);
            String lookupKey = uri.getPathSegments().get(lookupKeySegment);
            if (ContactLookupKey.PROFILE_LOOKUP_KEY.equals(lookupKey)) {
                return true;
            }
        }
        return false;
    }
}

/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.providers.contacts.aggregation.util;

import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupType;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class ContactMatcherTest extends AndroidTestCase {

    public void testMatchName_invalidHexDecimal() {
        final ContactMatcher matcher = new ContactMatcher();

        // This shouldn't throw.  Bug 6827136
        matcher.matchName(1, NameLookupType.NAME_COLLATION_KEY, "InvalidHex",
                NameLookupType.NAME_COLLATION_KEY, "InvalidHex2",
                ContactMatcher.MATCHING_ALGORITHM_CONSERVATIVE);
    }
}

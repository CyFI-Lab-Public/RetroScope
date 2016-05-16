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

package com.android.providers.contacts;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * Unit tests for {@link PhotoPriorityResolver}.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.PhotoPriorityResolverTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@MediumTest
public class PhotoPriorityResolverTest extends AndroidTestCase {

    public void testLoadPicturePriorityFromXml() {
        PhotoPriorityResolver resolver = new PhotoPriorityResolver(getContext());

        // See the res/xml/contacts.xml file where this priority is specified.
        assertEquals(42,
                resolver.resolvePhotoPriorityFromMetaData("com.android.providers.contacts.tests"));

        assertEquals(PhotoPriorityResolver.DEFAULT_PRIORITY,
                resolver.resolvePhotoPriorityFromMetaData("no.such.package"));
    }
}


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

import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Unit tests for {@link TypedUriMatcherImpl}.
 * Run the test like this:
 * <code>
 * runtest -c com.android.providers.contacts.util.TypedUriMatcherImplTest contactsprov
 * </code>
 */
@SmallTest
public class TypedUriMatcherImplTest extends AndroidTestCase {
    /** URI type used for testing. */
    private static enum TestUriType implements UriType {
        NO_MATCH(null),
        SIMPLE_URI("build"),
        URI_WITH_ID("build/#"),
        URI_WITH_TWO_IDS("project/*/build/#");

        private String path;

        private TestUriType(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return path;
        }
    }

    private final static String AUTHORITY = "authority";
    private final static String BASE_URI = "scheme://" + AUTHORITY + "/";

    /** The object under test. */
    TypedUriMatcherImpl<TestUriType> mTypedUriMatcherImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTypedUriMatcherImpl =
                new TypedUriMatcherImpl<TestUriType>(AUTHORITY, TestUriType.values());
    }

    public void testMatch_NoMatch() {
        // Incorrect authority.
        assertUriTypeMatch(TestUriType.NO_MATCH, "scheme://authority1/build");
        // Incorrect path.
        assertUriTypeMatch(TestUriType.NO_MATCH, BASE_URI + "test");
    }

    public void testMatch_SimpleUri() {
        assertUriTypeMatch(TestUriType.SIMPLE_URI, BASE_URI + "build");
    }

    public void testMatch_UriWithId() {
        assertUriTypeMatch(TestUriType.URI_WITH_ID, BASE_URI + "build/2");
        // Argument must be a number.
        assertUriTypeMatch(TestUriType.NO_MATCH, BASE_URI + "build/a");
        // Additional arguments not allowed.
        assertUriTypeMatch(TestUriType.NO_MATCH, BASE_URI + "build/2/more");
    }

    public void testMatch_UriWithTwoIds() {
        assertUriTypeMatch(TestUriType.URI_WITH_TWO_IDS, BASE_URI + "project/vm/build/3");
        // Missing argument.
        assertUriTypeMatch(TestUriType.NO_MATCH, BASE_URI + "project/vm/build/");
        // Argument cannot contain / itself
        assertUriTypeMatch(TestUriType.NO_MATCH, BASE_URI + "project/vm/x/build/3");
    }

    private void assertUriTypeMatch(UriType expectedType, String uri) {
        assertEquals(expectedType, mTypedUriMatcherImpl.match(Uri.parse(uri)));
    }
}

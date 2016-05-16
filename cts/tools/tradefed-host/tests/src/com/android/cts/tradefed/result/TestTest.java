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
package com.android.cts.tradefed.result;

import junit.framework.TestCase;

/**
 * Unit tests for {@link Test}.
 * <p/>
 * Lets hope a TestTestTest is not needed...
 */
public class TestTest extends TestCase {

    /**
     * Test {@link Test#getFailureMessageFromStackTrace(String)} for an empty stack
     */
    public void testGetFailureMessageFromStackTrace_empty() {
        final String stack = "";
        assertEquals(stack, Test.getFailureMessageFromStackTrace(stack));
    }

    /**
     * Test {@link Test#getFailureMessageFromStackTrace(String)} for a one line stack
     */
    public void testGetFailureMessageFromStackTrace_oneLine() {
        final String stack = "this is a line";
        assertEquals(stack, Test.getFailureMessageFromStackTrace(stack));
    }

    /**
     * Test {@link Test#getFailureMessageFromStackTrace(String)} for a one line stack with a newline
     * char
     */
    public void testGetFailureMessageFromStackTrace_oneNewLine() {
        final String stack = "this is a line\n";
        assertEquals(stack, Test.getFailureMessageFromStackTrace(stack));
    }

    /**
     * Test {@link Test#getFailureMessageFromStackTrace(String)} for a two line stack
     */
    public void testGetFailureMessageFromStackTrace_twoLines() {
        final String stack = "this is a line\nthis is also a line";
        assertEquals(stack, Test.getFailureMessageFromStackTrace(stack));
    }

    /**
     * Test {@link Test#getFailureMessageFromStackTrace(String)} for a multi line stack
     */
    public void testGetFailureMessageFromStackTrace_multiLines() {
        final String stack = "this is a line\nthis is also a line\n oh look another line";
        assertEquals("this is a line\nthis is also a line",
                Test.getFailureMessageFromStackTrace(stack));
    }
}

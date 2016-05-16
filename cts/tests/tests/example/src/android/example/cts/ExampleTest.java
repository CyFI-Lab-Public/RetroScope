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

package android.example.cts;

import android.example.Example;

import junit.framework.TestCase;

/**
 * Example test to demonstrate how tests work as well as to serve as
 * a smoke test for the CTS. (If the example test is "broken," then it
 * probably means that there's something fundamentally wrong with your
 * setup.)
 */
public class ExampleTest extends TestCase {
    /*
     * You can define standard JUnit setUp() and tearDown() methods here,
     * if needed.
     *
     * @Override protected void setUp() throws Exception { ... }
     * @Override protected void tearDown() throws Exception { ... }
     */

    /**
     * Test {@link Example#blort}.
     */
    public void testBlort() {
        assertEquals("blort", Example.blort());
    }

    // Add more tests here.
}

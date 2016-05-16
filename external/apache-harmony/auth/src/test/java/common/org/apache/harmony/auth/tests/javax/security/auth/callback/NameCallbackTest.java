/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
* @author Maxim V. Makarov
*/

package org.apache.harmony.auth.tests.javax.security.auth.callback;

import javax.security.auth.callback.NameCallback;

import junit.framework.TestCase;

/**
 * Tests NameCallback class
 */

public class NameCallbackTest extends TestCase {

    NameCallback nc;

    /**
     * Class under test for void NameCallback(String)
     */
    public final void testNameCallback_01() {
        nc = new NameCallback("prompt", "defaultName");
        assertEquals("prompt", nc.getPrompt());
        assertEquals("defaultName", nc.getDefaultName());
        nc.setName("Name");
        assertEquals("Name", nc.getName());
    }

    /**
     * Test for NameCallback(String p, String msg) when
     * prompt and message is null and empty. 
     */
    public final void testNameCallback_02() {
        try {
            nc = new NameCallback("", "DefaultName");
            fail("Prompt and DefaultName should not be empty");
        } catch (IllegalArgumentException e) {
        }
        try {
            nc = new NameCallback(null, "DefaultName");
            fail("Prompt and DefaultName should not null");
        } catch (IllegalArgumentException e) {
        }
        try {
            nc = new NameCallback("Prompt", "");
            fail("Prompt and DefaultName should not be empty");
        } catch (IllegalArgumentException e) {
        }
        try {
            nc = new NameCallback("Prompt", null);
            fail("Prompt and DefaultName should not null");
        } catch (IllegalArgumentException e) {
        }

    }

}

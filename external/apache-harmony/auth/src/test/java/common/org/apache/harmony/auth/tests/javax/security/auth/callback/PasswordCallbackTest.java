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


import javax.security.auth.callback.PasswordCallback;

import junit.framework.TestCase;

/**
 * Tests PasswordCallback class
 */

public class PasswordCallbackTest extends TestCase {

    PasswordCallback pc;

    public final void testPasswordCallback() {
        pc = new PasswordCallback("prompt", true);
        assertEquals("prompt", pc.getPrompt());
        assertTrue(pc.isEchoOn());
        pc.setPassword(null);
        pc.clearPassword();
        assertNull(pc.getPassword());
        char[] pwd = {'a','b','c'};
        pc.setPassword(pwd);
        assertEquals(new String(pwd), new String(pc.getPassword()));
        pc.clearPassword();
        assertEquals(pwd.length, pc.getPassword().length);
        assertFalse(new String(pwd).equals(pc.getPassword()));
        char[] p = new char[5];
        pc.setPassword(p);
        pc.clearPassword();
        assertEquals(p.length, pc.getPassword().length);
    }
    
    public final void testInit_01() {
        try {
        pc = new PasswordCallback("", true);
        fail("Prompt and DefaultName should not be empty");
        } catch (IllegalArgumentException e){}
    }

    public final void testInit_02() {
        try {
        pc = new PasswordCallback(null, true);
        fail("Prompt and DefaultName should not null");
        } catch (IllegalArgumentException e){}
    }

}

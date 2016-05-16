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
import javax.security.auth.callback.UnsupportedCallbackException;

import junit.framework.TestCase;

/**
 * Tests UnsupportedCallbackException class
 */

public class UnsupportedCallbackExceptionTest extends TestCase {

    NameCallback nc = new NameCallback("prompt");
    
    /**
     * Test for UnsupportedCallbackException(Callback c) ctor 
     */
    public final void testUnsupportedCallbackException_01() {
        UnsupportedCallbackException ce = new UnsupportedCallbackException(nc);
        assertEquals(nc, ce.getCallback());
        try {
            throw ce;
        }catch (Exception e){
            assertTrue(ce.equals(e));
            assertEquals(nc, ce.getCallback());
        }

    }

    /**
     * Test for UnsupportedCallbackException(Callback c, String msg) ctor 
     */
    public final void testUnsupportedCallbackException_02() {
        UnsupportedCallbackException ce = new UnsupportedCallbackException(nc, "message");
        assertEquals ("message", ce.getMessage());
        assertEquals(nc, ce.getCallback());
        try {
            throw ce;
        }catch (Exception e){
            assertTrue(ce.equals(e));
            assertEquals(nc, ce.getCallback());
        }

    }

    /**
     * Test for UnsupportedCallbackException(Callback c, String msg) ctor 
     * when callback and msg is null 
     */
    public final void testUnsupportedCallbackException_03() {
        UnsupportedCallbackException ce = new UnsupportedCallbackException(null, null);
        assertNull(ce.getMessage());
        assertNull(ce.getCallback());
        try {
            throw ce;
        }catch (Exception e){
            assertTrue(ce.equals(e));
        }
    }

}

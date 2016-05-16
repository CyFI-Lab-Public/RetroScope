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

package org.apache.harmony.xnet.tests.javax.net.ssl;

import javax.net.ssl.SSLHandshakeException;

import junit.framework.TestCase;


/**
 * Tests for <code>SSLHandshakeException</code> class constructors and
 * methods.
 * 
 */
public class SSLHandshakeExceptionTest extends TestCase {

    public static void main(String[] args) {
    }

    /**
     * Constructor for SSLHandshakeExceptionTests.
     * 
     * @param arg0
     */
    public SSLHandshakeExceptionTest(String arg0) {
        super(arg0);
    }

    static String[] msgs = {
            "",
            "Check new message",
            "Check new message Check new message Check new message Check new message Check new message" };

    static Throwable tCause = new Throwable("Throwable for exception");

    /**
     * Test for <code>SSLHandshakeException(String)</code> constructor
     * Assertion: constructs SSLHandshakeException with detail message msg.
     * Parameter <code>msg</code> is not null.
     */
    public void testSSLHandshakeException01() {
        SSLHandshakeException tE;
        for (int i = 0; i < msgs.length; i++) {
            tE = new SSLHandshakeException(msgs[i]);
            assertEquals("getMessage() must return: ".concat(msgs[i]), tE
                    .getMessage(), msgs[i]);
            assertNull("getCause() must return null", tE.getCause());
        }
    }

    /**
     * Test for <code>SSLHandshakeException(String)</code> constructor
     * Assertion: constructs SSLHandshakeException when <code>msg</code> is
     * null
     */
    public void testSSLHandshakeException02() {
        String msg = null;
        SSLHandshakeException tE = new SSLHandshakeException(msg);
        assertNull("getMessage() must return null.", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }
}

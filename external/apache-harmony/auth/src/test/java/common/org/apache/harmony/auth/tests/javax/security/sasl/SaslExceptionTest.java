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
* @author Vera Y. Petrashkova
*/

package org.apache.harmony.auth.tests.javax.security.sasl;

import javax.security.sasl.SaslException;

import junit.framework.TestCase;

/**
 * Tests for constructors and methods of SaslException class
 */
public class SaslExceptionTest extends TestCase {
    static String[] msgs = {
            "",
            "Check new message",
            "Check new message Check new message Check new message Check new message Check new message" };

    private static final Throwable[] thUpd = { 
            new Exception("New exception"),
            new Exception(), 
            new Exception("exception", new Throwable()),
            new Throwable("throwable", new Throwable("New throwable")),
            new Exception(new Exception("Another exception"))
    };
 
    static Throwable tCause = new Throwable("Throwable for exception");

    /**
     * Test for <code>SaslException()</code> constructor 
     * Assertion: constructs SaslException with null message and 
     * null root exception. 
     */
    public void testSaslException01() {
        SaslException tE;
        tE = new SaslException();
        assertNull("getMessage() must return null", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }

    /**
     * Test for <code>SaslException(String detail)</code> constructor 
     * Assertion:
     * constructs SaslException with defined detail message. 
     * Parameter <code>detail</code> is not null.
     */
    public void testSaslException02() {
        SaslException tE;
        for (int i = 0; i < msgs.length; i++) {
            tE = new SaslException(msgs[i]);
            assertEquals("getMessage() must return: ".concat(msgs[i]), tE
                    .getMessage(), msgs[i]);
            assertNull("getCause() must return null", tE.getCause());
        }
    }

    /**
     * Test for <code>SaslException(String detail)</code> constructor 
     * Assertion: constructs SaslException when <code>detail</code> is null
     */
    public void testSaslException03() {
        String msg = null;
        SaslException tE = new SaslException(msg);
        assertNull("getMessage() must return null.", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }

    /**
     * Test for <code>SaslException(String detail, Throwable ex)</code> constructor
     * Assertion: constructs SaslException when <code>ex</code> is null
     * <code>detail</code> is null
     */
    public void testSaslException04() {
        SaslException tE = new SaslException(null, null);
        assertNull("getMessage() must return null", tE.getMessage());
        assertNull("getCause() must return null", tE.getCause());
    }

    /**
     * Test for <code>SaslException(String detail, Throwable ex)</code> constructor
     * Assertion: constructs SaslException when <code>ex</code> is null
     * <code>detail</code> is not null
     */
    public void testSaslException05() {
        SaslException tE;
        for (int i = 0; i < msgs.length; i++) {
            tE = new SaslException(msgs[i], null);
            assertEquals("getMessage() must return: ".concat(msgs[i]), tE
                    .getMessage(), msgs[i]);
            assertNull("getCause() must return null", tE.getCause());
        }
    }

    /**
     * Test for <code>SaslException(String detail, Throwable  ex)</code> constructor
     * Assertion: constructs SaslException when <code>ex</code> is not null
     * <code>detail</code> is null
     */
    public void testSaslException06() {        
        SaslException tE = new SaslException(null, tCause);
        if (tE.getMessage() != null) {
            String toS = tCause.toString();
            String getM = tE.getMessage();
            assertTrue("getMessage() must should ".concat(toS), (getM
                    .indexOf(toS) != -1));
        }
        // SaslException is subclass of IOException, but IOException has not
        // constructors with Throwable parameters
        if (tE.getCause() != null) {
            //	assertNotNull("getCause() must not return null", tE.getCause());
            assertEquals("getCause() must return ".concat(tCause.toString()),
                    tE.getCause(), tCause);
        }
    }

    /**
     * Test for <code>SaslException(String detail, Throwable ex)</code> constructor
     * Assertion: constructs SaslException when <code>ex</code> is not null
     * <code>detail</code> is not null
     */
    public void testSaslException07() {
        SaslException tE;
        for (int i = 0; i < msgs.length; i++) {
            tE = new SaslException(msgs[i], tCause);
            String getM = tE.getMessage();
            String toS = tCause.toString();
            if (msgs[i].length() > 0) {
                assertTrue("getMessage() must contain ".concat(msgs[i]), getM
                        .indexOf(msgs[i]) != -1);
                if (!getM.equals(msgs[i])) {
                    assertTrue("getMessage() should contain ".concat(toS), getM
                            .indexOf(toS) != -1);
                }
            }
            // SaslException is subclass of IOException, but IOException has not
            // constructors with Throwable parameters
            if (tE.getCause() != null) {
                //	assertNotNull("getCause() must not return null",
                // tE.getCause());
                assertEquals("getCause() must return "
                        .concat(tCause.toString()), tE.getCause(), tCause);
            }
        }
    }
    
    /**
     * Test for <code>toString()</code> method
     * Assertion: returns not null string
     */
    public void testToString() {
        Throwable[] th = {
                null,
                new Exception(
                        "New Exception for toString() method verification"),
                new Throwable(), new Exception("exception", new Exception()) };

        SaslException eT;
        eT = new SaslException();
        assertNotNull("Incorrect null string", eT.toString());
        for (int i = 0; i < msgs.length; i++) {
            eT = new SaslException(msgs[i]);
            assertTrue("Incorrect result string", eT.toString()
                    .indexOf(msgs[i]) >= 0);

            for (int j = 0; j < th.length; j++) {
                eT = new SaslException(msgs[i], th[j]);
                assertTrue("Incorrect result string", eT.toString().indexOf(
                        msgs[i]) >= 0);
                if (th[j] != null) {
                    assertTrue("Incorrect result string", eT.toString()
                            .indexOf(th[j].toString()) >= 0);
                }
            }
        }
    }
    
    /**
     * Test for <code>getCause()</code> and <code>initCause(Throwable cause)</code> 
     * methods
     * Assertion: return cause
     */
    public void testInitCause01() {
        SaslException eT;
        Throwable eT1;
        eT = new SaslException();

        for (int l = 0; l < thUpd.length; l++) {
            try {
                eT1 = eT.initCause(thUpd[l]);
                assertEquals("Incorrect throwable", eT1, eT);
                assertEquals("Incorrect cause", eT.getCause(), thUpd[l]);
            } catch (IllegalStateException e) {
                if (l == 0) {
                    fail("Unexpected exception " + e);
                }
            }
        }
    }
    
    /**
     * Test for <code>getCause()</code> and <code>initCause(Throwable cause)</code> 
     * methods
     * Assertion: return cause
     */
    public void testInitCause02() {
        SaslException eT;
        Throwable eT1;
        eT = new SaslException();

        for (int i = 0; i < msgs.length; i++) {
            eT = new SaslException(msgs[i]);

            for (int l = (thUpd.length - 1); l >= 0; l--) {
                try {
                    eT1 = eT.initCause(thUpd[l]);
                    assertEquals("Incorrect throwable", eT1, eT);
                    assertEquals("Incorrect cause", eT.getCause(), thUpd[l]);
                } catch (IllegalStateException e) {
                    if (l == (thUpd.length - 1)) {
                        fail("Unexpected exception " + e);
                    }
                }
            }
        }
    }

    /**
     * Test for <code>getCause()</code> and <code>initCause(Throwable cause)</code> 
     * methods
     * Assertion: return cause
     */
    public void testInitCause03() {
        Throwable[] th = { null,
                new Exception("Long Exception Message long exception message"),
                new Throwable(), new Exception("New msg", new Exception()) };

        SaslException eT;
        Throwable eT1;
        eT = new SaslException();

        boolean mod = false;
        for (int i = 0; i < msgs.length; i++) {
            for (int j = 0; j < th.length; j++) {
                mod = false;
                for (int l = 0; l < thUpd.length; l++) {
                    eT = new SaslException(msgs[i], th[j]);
                    try {
                        eT1 = eT.initCause(thUpd[l]);
                        assertEquals(eT1, eT);
                        mod = true;
                        if ((th[j] == null) && !mod) {
                            assertEquals("Incorrect cause", eT.getCause(),
                                    thUpd[l]);
                        }
                    } catch (IllegalStateException e) {
                        if ((th[j] == null) && !mod) {
                            fail("Unexpected exception: " + e);
                        }
                    }
                }
            }
        }
    }
}

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

import javax.net.ssl.SSLEngineResult;

import junit.framework.TestCase;

/**
 * Tests for SSLEngineResult class
 * 
 */
public class SSLEngineResultTest extends TestCase {

    /**
     * Test for <code>SSLEngineResult(SSLEngineResult.Status status,
     *              SSLEngineResult.HandshakeStatus handshakeStatus,
     *              int bytesConsumed,
     *              int bytesProduced) </code> constructor and 
     * <code>getHandshakeStatus()</code>
     * <code>getStatus()</code>
     * <code>bytesConsumed()</code>
     * <code>bytesProduced()</code>
     * <code>toString()</code>
     * methods
     * Assertions: 
     * constructor throws IllegalArgumentException when bytesConsumed
     * or bytesProduced is negative or when status or handshakeStatus
     * is null  
     *
     */
    public void testSSLEngineResultConstructor() {
    
        int[] neg = { -1, -10, -1000, Integer.MIN_VALUE,
                (Integer.MIN_VALUE + 1) };
        int[] pos = { 0, 1, 1000, Integer.MAX_VALUE, (Integer.MAX_VALUE - 1) };
        try {
            new SSLEngineResult(null, SSLEngineResult.HandshakeStatus.FINISHED,
                    1, 1);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, null,
                    1, 1);
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        for (int i = 0; i < neg.length; i++) {
            try {
                new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW,
                        SSLEngineResult.HandshakeStatus.FINISHED, neg[i], 1);
                fail("IllegalArgumentException must be thrown");
            } catch (IllegalArgumentException e) {
            }
        }
        for (int i = 0; i < neg.length; i++) {
            try {
                new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW,
                        SSLEngineResult.HandshakeStatus.FINISHED, 1, neg[i]);
                fail("IllegalArgumentException must be thrown");
            } catch (IllegalArgumentException e) {
            }
        }
        SSLEngineResult.Status [] enS = SSLEngineResult.Status.values();
        SSLEngineResult.HandshakeStatus [] enHS = SSLEngineResult.HandshakeStatus
                .values();
        SSLEngineResult res;
        String toS;
        for (int i = 0; i < enS.length; i++) {
            for (int j = 0; j < enHS.length; j++) {
                for (int n = 0; n < pos.length; n++) {
                    for (int l = 0; l < pos.length; l++) {
                        res = new SSLEngineResult(enS[i], enHS[j], pos[n],
                                pos[l]);

                        assertEquals("Incorrect bytesProduced", res
                                .bytesProduced(), pos[l]);
                        assertEquals("Incorrect bytesConsumed", res
                                .bytesConsumed(), pos[n]);
                        assertEquals("Incorrect HandshakeStatus", res
                                .getHandshakeStatus(), enHS[j]);
                        assertEquals("Incorrect Status", res.getStatus(),
                                enS[i]);
                        toS = res.toString();
                        assertNotNull("Result of toSring() method is null", toS);
                    }
                }
            }
        }
        
    }
    /**
     * Test for <code>SSLEngineResult.Status.values()</code> method
     */

    public void testStatus01() {
        SSLEngineResult.Status [] enS = SSLEngineResult.Status.values();
        assertTrue("Incorrect array of Status objects", enS.length > 0);
        assertTrue("OK object does not define", findEl(enS,
                SSLEngineResult.Status.OK));
        assertTrue("CLOSED object does not define", findEl(enS,
                SSLEngineResult.Status.CLOSED));
        assertTrue("BUFFER_OVERFLOW object does not define", findEl(enS,
                SSLEngineResult.Status.BUFFER_OVERFLOW));
        assertTrue("BUFFER_UNDERFLOW object does not define", findEl(enS,
                SSLEngineResult.Status.BUFFER_UNDERFLOW));
    }
    
    /**
     * Test for <code>SSLEngineResult.Status.valueOf(String name)</code> method
     * Assertion: 
     * throws IllegalArgumentException when there is no constan with specified
     * name 
     */

    public void testStatus02() {
        String [] invalid = {"", "OK1", "BUFFER_overflow", "BUFFER_UND",
                "CLOSED_CLOSED", "Bad string for verification valueOf method"
        };
        assertEquals(SSLEngineResult.Status.valueOf("BUFFER_OVERFLOW"),
                SSLEngineResult.Status.BUFFER_OVERFLOW);
        assertEquals(SSLEngineResult.Status.valueOf("BUFFER_UNDERFLOW"),
                SSLEngineResult.Status.BUFFER_UNDERFLOW);
        assertEquals(SSLEngineResult.Status.valueOf("CLOSED"),
                SSLEngineResult.Status.CLOSED);
        assertEquals(SSLEngineResult.Status.valueOf("OK"),
                SSLEngineResult.Status.OK);
        for (int i = 0; i < invalid.length; i++) {
            try {
                SSLEngineResult.Status.valueOf(invalid[i]);
                fail("IllegalArgumentException must be thrown for name: " + invalid[i]);
            } catch (IllegalArgumentException e) {                
            }
        }                 
    }
    
    /**
     * Test for <code>SSLEngineResult.HandshakeStatus.values()</code> method
     */

    public void testHandshakeStatus01() {
        SSLEngineResult.HandshakeStatus [] enHS = SSLEngineResult.HandshakeStatus
                .values();
        assertTrue("Incorrect array of HandshakeStatus objects",
                enHS.length > 0);
        assertTrue("FINISHED object does not define", findEl(enHS,
                SSLEngineResult.HandshakeStatus.FINISHED));
        assertTrue("NEED_UNWRAP object does not define", findEl(enHS,
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP));
        assertTrue("NEED_WRAP object does not define", findEl(enHS,
                SSLEngineResult.HandshakeStatus.NEED_WRAP));
        assertTrue("NEED_TASK object does not define", findEl(enHS,
                SSLEngineResult.HandshakeStatus.NEED_TASK));
        assertTrue("NOT_HANDSHAKING object does not define", findEl(enHS,
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING));
    }
    
    /**
     * Test for <code>SSLEngineResult.HandshakeStatus.valueOf(String name)</code> method
     * Assertion: 
     * throws IllegalArgumentException when there is no constan with specified
     * name 
     */

    public void testHandshakeStatus02() {
        String [] invalid = {"", "FINISHED1", "NEED_task", "NEED_UN",
                "NEED_WRAP_WRAP", "not_HANDSHAKING", "Bad string for verification valueOf method"
        };
        assertEquals(SSLEngineResult.HandshakeStatus.valueOf("NOT_HANDSHAKING"),
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);
        assertEquals(SSLEngineResult.HandshakeStatus.valueOf("NEED_WRAP"),
                SSLEngineResult.HandshakeStatus.NEED_WRAP);
        assertEquals(SSLEngineResult.HandshakeStatus.valueOf("NEED_UNWRAP"),
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP);
        assertEquals(SSLEngineResult.HandshakeStatus.valueOf("FINISHED"),
                SSLEngineResult.HandshakeStatus.FINISHED);
        assertEquals(SSLEngineResult.HandshakeStatus.valueOf("NEED_TASK"),
                SSLEngineResult.HandshakeStatus.NEED_TASK);
        for (int i = 0; i < invalid.length; i++) {
            try {
                SSLEngineResult.HandshakeStatus.valueOf(invalid[i]);
                fail("IllegalArgumentException must be thrown for name: " + invalid[i]);
            } catch (IllegalArgumentException e) {                
            }
        }
    } 
    
    private boolean findEl(Object[] arr, Object el) {
        boolean ok = false;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(el)) {
                ok = true;
                break;
            }
        }
        return ok;
    }

}

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

package org.apache.harmony.auth.tests.javax.security.auth.kerberos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

/**
 * Tests KerberosKey class implementation.
 * 
 * @see http://www.ietf.org/rfc/rfc3961.txt
 */
public class KerberosKeyTest extends TestCase {

    // principal object for testing
    private final KerberosPrincipal principal = new KerberosPrincipal(
            "name@aaa.com", 1);

    // byte array for testing
    private final byte[] keyBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07 };

    /**
     * @tests javax.security.auth.kerberos.KerberosKey#KerberosKey(
     *        javax.security.auth.kerberos.KerberosPrincipal, byte[], int, int)
     */
    public void test_Ctor1() {

        // OK to pass null value for principal parameter
        assertNull(new KerberosKey(null, keyBytes, 0, 0).getPrincipal());

        // NPE for null keyBytes parameter
        try {
            new KerberosKey(principal, null, 0, 0);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }

        // construct with DES algorithm
        KerberosKey key = new KerberosKey(principal, keyBytes, 1, 123);
        assertEquals("DES algorithm", "DES", key.getAlgorithm());
        assertEquals("version number", 123, key.getVersionNumber());
        assertEquals("format", "RAW", key.getFormat());
        assertSame("principal", principal, key.getPrincipal());
        assertFalse("is destroyed", key.isDestroyed());

        // construct with NULL algorithm
        key = new KerberosKey(principal, keyBytes, 0, 0);
        assertEquals("NULL algorithm", "NULL", key.getAlgorithm());
        assertEquals("version number", 0, key.getVersionNumber());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosKey#KerberosKey(
     *        javax.security.auth.kerberos.KerberosPrincipal, char[],
     *        java.lang.String)
     */
    public void test_Ctor2() {

        // NPE for null value for principal parameter
        try {
            new KerberosKey(null, new char[10], "DES");
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }

        // NPE for null password value
        try {
            new KerberosKey(principal, null, "DES");
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }

        // IAE for unsupported algorithm
        try {
            new KerberosKey(principal, new char[10],
                    "there_is_no_such_algorithm");
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        // if algorithm parameter is null then DES is used
        KerberosKey key = new KerberosKey(principal, new char[10], null);

        assertEquals("algorithm", "DES", key.getAlgorithm());
        assertEquals("format", "RAW", key.getFormat());
        assertEquals("key type", 3, key.getKeyType());
        assertEquals("version number", 0, key.getVersionNumber());
        assertFalse("is destroyed", key.isDestroyed());
        assertSame("principal", principal, key.getPrincipal());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosKey#getEncoded()
     */
    public void test_getEncoded() {

        KerberosKey key = new KerberosKey(principal, keyBytes, 1, 123);

        byte[] keyBytes1 = key.getEncoded();
        assertTrue("encoded", Arrays.equals(keyBytes, keyBytes1));

        // bytes are copied each time we invoke the method
        assertNotSame("keyBytes immutability 1 ", keyBytes, keyBytes1);
        assertNotSame("keyBytes immutability 2 ", keyBytes1, key.getEncoded());

        // Test generation of DES key from password
        // test data from RFC 3961 (http://www.ietf.org/rfc/rfc3961.txt)
        // see A.2 test vectors
        // test data format: principal/password/DES key
        Object[][] testcases = {
                {
                        "raeburn@ATHENA.MIT.EDU",
                        "password",
                        new byte[] { (byte) 0xcb, (byte) 0xc2, (byte) 0x2f,
                                (byte) 0xae, (byte) 0x23, (byte) 0x52,
                                (byte) 0x98, (byte) 0xe3 } },
                {
                        "danny@WHITEHOUSE.GOV",
                        "potatoe",
                        new byte[] { (byte) 0xdf, (byte) 0x3d, (byte) 0x32,
                                (byte) 0xa7, (byte) 0x4f, (byte) 0xd9,
                                (byte) 0x2a, (byte) 0x01 } },
        // TODO add "pianist@EXAMPLE.COM" and "Juri ... @ATHENA.MIT.EDU"
        };

        for (Object[] element : testcases) {
            KerberosPrincipal kp = new KerberosPrincipal(
                    (String) element[0], 1);

            key = new KerberosKey(kp, ((String) element[1]).toCharArray(),
                    "DES");

            assertTrue("Testcase: " + (String) element[0], Arrays.equals(
                    (byte[]) element[2], key.getEncoded()));
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosKey#destroy()
     */
    public void test_destroy() throws Exception {

        KerberosKey key = new KerberosKey(principal, new char[10], "DES");

        assertFalse("not destroyed", key.isDestroyed());

        key.destroy();
        assertTrue("destroyed", key.isDestroyed());

        // no exceptions for second destroy() call
        key.destroy();

        // check that IllegalStateException is thrown for certain methods
        try {
            key.getAlgorithm();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            key.getEncoded();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            key.getFormat();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            key.getKeyType();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            key.getPrincipal();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            key.getVersionNumber();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            // but for serialization IOException is expected
            ObjectOutputStream out = new ObjectOutputStream(
                    new ByteArrayOutputStream());
            out.writeObject(key);
            fail("No expected IOException");
        } catch (IOException e) {
        }

        try {
            key.toString();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }
    
    /**
     * @tests javax.security.auth.kerberos.KerberosKey#equals(java.lang.Object)
     */
    public void test_equals() {
        KerberosKey kerberosKey1 = new KerberosKey(principal, keyBytes, 1, 123);
        KerberosKey kerberosKey2 = new KerberosKey(principal, keyBytes, 1, 123);
        KerberosKey kerberosKey3 = new KerberosKey(principal, new byte[] { 1,
                3, 4, 5 }, 1, 123);
        assertEquals("kerberosKey1 and kerberosKey2 should be equivalent ",
                kerberosKey1, kerberosKey2);
        assertFalse("kerberosKey1 and kerberosKey3 sholudn't be equivalent ",
                kerberosKey1.equals(kerberosKey3));
        try {
            kerberosKey2.destroy();
        } catch (DestroyFailedException e) {
            fail("kerberosKey2 destroy failed");
        }
        assertFalse("Destroyed kerberosKey sholudn't be equivalent ",
                kerberosKey1.equals(kerberosKey2));
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosKey#hashCode()
     */
    public void test_hashCode() {
        KerberosKey kerberosKey1 = new KerberosKey(principal, keyBytes, 1, 123);
        KerberosKey kerberosKey2 = new KerberosKey(principal, keyBytes, 1, 123);
        assertEquals("kerberosKey1 and kerberosKey2 should be equivalent ",
                kerberosKey1, kerberosKey2);
        assertEquals("hashCode should be equivalent", kerberosKey1.hashCode(),
                kerberosKey2.hashCode());
    }
}

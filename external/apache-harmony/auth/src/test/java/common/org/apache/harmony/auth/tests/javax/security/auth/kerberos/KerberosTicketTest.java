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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.RefreshFailedException;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;

import junit.framework.TestCase;

import org.apache.harmony.auth.tests.support.TestUtils;

public class KerberosTicketTest extends TestCase {

    private static final String ENV_KDC = "java.security.krb5.kdc";

    private static final String ENV_REALM = "java.security.krb5.realm";

    // ticket's ASN.1 encoding  
    private static final byte[] ticket = { 0x01, 0x02, 0x03, 0x04 };

    // client's principal 
    private static final KerberosPrincipal pClient = new KerberosPrincipal(
            "client@apache.org");

    // server's principal 
    private static final KerberosPrincipal pServer = new KerberosPrincipal(
            "server@apache.org");

    // session key
    private static final byte[] sessionKey = { 0x01, 0x04, 0x03, 0x02 };

    private static final int KEY_TYPE = 1;

    // number of flags used by Kerberos protocol
    private static final int FLAGS_NUM = 32;

    private static final boolean[] flags = { true, false, true, false, true,
            false, true, false, true, false, true, false, };

    private static final int AUTH_TIME = 0;

    private static final Date authTime = new Date(AUTH_TIME);

    private static final int START_TIME = 1;

    private static final Date startTime = new Date(START_TIME);

    private static final int END_TIME = 2;

    private static final Date endTime = new Date(END_TIME);

    private static final Date renewTill = new Date(3);

    private static final InetAddress[] addesses;

    static {
        try {
            addesses = new InetAddress[] { InetAddress.getLocalHost() };
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getAuthTime() 
     */
    public void test_getAuthTime() throws Exception {

        Date newAuthTime = new Date(AUTH_TIME);

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, newAuthTime, startTime, endTime,
                renewTill, addesses);

        // initial value is not copied
        newAuthTime.setTime(AUTH_TIME + 1);
        assertEquals(AUTH_TIME + 1, krbTicket.getAuthTime().getTime());

        // returned value is copied
        assertNotSame(krbTicket.getAuthTime(), krbTicket.getAuthTime());

        // auth time: null value is illegal for constructor
        try {
            new KerberosTicket(ticket, pClient, pServer, sessionKey, KEY_TYPE,
                    flags, null, startTime, endTime, renewTill, addesses);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getClient() 
     */
    public void test_getClient() throws Exception {

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, endTime,
                renewTill, addesses);

        assertSame(pClient, krbTicket.getClient());

        // client principal: null value is illegal for constructor
        try {
            new KerberosTicket(ticket, null, pServer, sessionKey, KEY_TYPE,
                    flags, authTime, startTime, endTime, renewTill, addesses);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getClientAddresses() 
     */
    public void test_getClientAddresses() throws Exception {

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, endTime,
                renewTill, addesses);

        assertTrue(Arrays.equals(addesses, krbTicket.getClientAddresses()));

        // initial value is copied
        assertNotSame(addesses, krbTicket.getClientAddresses());

        // KerberosTicket instance is immutable 
        assertNotSame(krbTicket.getClientAddresses(), krbTicket
                .getClientAddresses());

        // addesses: null value is OK for constructor
        krbTicket = new KerberosTicket(ticket, pClient, pServer, sessionKey,
                KEY_TYPE, flags, authTime, startTime, endTime, renewTill, null);
        assertNull(krbTicket.getClientAddresses());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getEncoded() 
     */
    public void test_getEncoded() throws Exception {

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, endTime,
                renewTill, addesses);

        assertTrue(Arrays.equals(ticket, krbTicket.getEncoded()));

        // initial byte array is copied
        assertNotSame(ticket, krbTicket.getEncoded());

        // KerberosTicket instance is immutable 
        assertNotSame(krbTicket.getEncoded(), krbTicket.getEncoded());

        // ticket: null value is illegal for constructor
        try {
            new KerberosTicket(null, pClient, pServer, sessionKey, KEY_TYPE,
                    flags, authTime, startTime, endTime, renewTill, addesses);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getEndTime() 
     */
    public void test_getEndTime() throws Exception {

        Date newEndTime = new Date(END_TIME);

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, newEndTime,
                renewTill, addesses);

        // initial value is not copied
        newEndTime.setTime(END_TIME + 1);
        assertEquals(END_TIME + 1, krbTicket.getEndTime().getTime());

        // returned value is copied
        assertNotSame(krbTicket.getEndTime(), krbTicket.getEndTime());

        // end time: null value is illegal for constructor
        try {
            new KerberosTicket(ticket, pClient, pServer, sessionKey, KEY_TYPE,
                    flags, authTime, startTime, null, renewTill, addesses);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getFlags()
     */
    public void test_getFlags() {

        boolean[] myFlags = new boolean[] { true, //reserved
                true, // forwardable
                true, // forwarded
                true, // proxiable
                true, // proxy
                true, // may-postdate 
                true, // postdated
                true, // invalid
                true, // renewable
                true, // initial
                true, // pre-authent
                true // hw-authent 
        };

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, myFlags, // <=== we test this
                authTime, startTime, endTime, renewTill, addesses);

        // test: returned value is copied
        assertNotSame(krbTicket.getFlags(), krbTicket.getFlags());

        // test: flags values
        assertTrue(krbTicket.isForwardable());
        assertTrue(krbTicket.isForwarded());
        assertTrue(krbTicket.isInitial());
        assertTrue(krbTicket.isPostdated());
        assertTrue(krbTicket.isProxiable());
        assertTrue(krbTicket.isProxy());
        assertTrue(krbTicket.isRenewable());

        //
        // test: number of flags less the in Kerberos protocol (<32)
        //
        boolean[] ktFlags = krbTicket.getFlags();
        assertEquals("flags length", FLAGS_NUM, ktFlags.length);
        int index = 0;
        // must match to initial array
        for (; index < flags.length; index++) {
            assertEquals("Index: " + index, myFlags[index], ktFlags[index]);
        }
        // the rest is expected to be false
        for (; index < FLAGS_NUM; index++) {
            assertEquals("Index: " + index, false, ktFlags[index]);
        }

        //
        // test: flags array is greater then 32
        //
        myFlags = new boolean[50];

        krbTicket = new KerberosTicket(ticket, pClient, pServer, sessionKey,
                KEY_TYPE, myFlags, // <=== we test this 
                authTime, startTime, endTime, renewTill, addesses);

        ktFlags = krbTicket.getFlags();

        assertEquals(myFlags.length, ktFlags.length);
        for (index = 0; index < ktFlags.length; index++) {
            assertEquals(false, ktFlags[index]);
        }

        // initial array is copied
        assertFalse(krbTicket.isForwardable());
        myFlags[1] = true;
        assertFalse(krbTicket.isForwardable());

        //
        // test: Null value
        //
        krbTicket = new KerberosTicket(ticket, pClient, pServer, sessionKey,
                KEY_TYPE, null, // <=== we test this
                authTime, startTime, endTime, renewTill, addesses);
        assertTrue(Arrays.equals(new boolean[FLAGS_NUM], krbTicket.getFlags()));
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getServer() 
     */
    public void test_getServer() throws Exception {

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, endTime,
                renewTill, addesses);

        assertSame(pServer, krbTicket.getServer());

        // server principal: null value is illegal for constructor
        try {
            new KerberosTicket(ticket, pClient, null, sessionKey, KEY_TYPE,
                    flags, authTime, startTime, endTime, renewTill, addesses);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getSessionKey() 
     */
    public void test_getSessionKey() throws Exception {

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, endTime,
                renewTill, addesses);

        assertSame(krbTicket.getSessionKey(), krbTicket.getSessionKey());

        // test returned SecretKey object
        SecretKey sKey = krbTicket.getSessionKey();
        byte[] keyBytes = sKey.getEncoded();

        assertTrue(Arrays.equals(sessionKey, keyBytes));
        // initial byte array is copied
        assertNotSame(sessionKey, sKey.getEncoded());
        // key instance is immutable 
        assertNotSame(sKey.getEncoded(), sKey.getEncoded());

        assertEquals("algorithm", "DES", sKey.getAlgorithm());
        assertEquals("format", "RAW", sKey.getFormat());

        // sessionKey: null value is illegal for constructor
        try {
            new KerberosTicket(ticket, pClient, pServer, null, KEY_TYPE, flags,
                    authTime, startTime, endTime, renewTill, addesses);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getSessionKeyType()
     */
    public void test_getSessionKeyType() throws Exception {

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, endTime,
                renewTill, addesses);

        assertEquals(KEY_TYPE, krbTicket.getSessionKeyType());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#getStartTime() 
     */
    public void test_getStartTime() throws Exception {

        Date newStartTime = new Date(START_TIME);

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, newStartTime, endTime,
                renewTill, addesses);

        // initial value is copied
        newStartTime.setTime(START_TIME + 1);
        assertEquals(START_TIME + 1, krbTicket.getStartTime().getTime());

        // returned value is copied 
        assertNotSame(krbTicket.getStartTime(), krbTicket.getStartTime());

        // start time: null value is valid for constructor
        krbTicket = new KerberosTicket(ticket, pClient, pServer, sessionKey,
                KEY_TYPE, flags, authTime, null, endTime, renewTill, addesses);
        assertEquals(authTime, krbTicket.getStartTime());
        assertNotSame(authTime, krbTicket.getStartTime());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#destroy()
     * @tests javax.security.auth.kerberos.KerberosTicket#isDestroyed()
     */
    public void test_Destroyable() throws Exception {

        KerberosTicket kt = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, flags, authTime, startTime, endTime,
                renewTill, addesses);

        assertFalse(kt.isDestroyed());

        kt.destroy();
        assertTrue(kt.isDestroyed());

        // no exceptions for second destroy
        kt.destroy();

        assertNull(kt.getAuthTime());
        assertNull(kt.getClient());
        assertNull(kt.getClientAddresses());

        try {
            kt.getEncoded();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        assertNull(kt.getEndTime());
        assertNull(kt.getFlags());
        assertNull(kt.getRenewTill());
        assertNull(kt.getServer());

        try {
            kt.getSessionKey();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            kt.getSessionKeyType();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }

        try {
            kt.toString();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#refresh()
     */
    public void test_refresh() throws Exception {

        boolean[] myFlags = new boolean[] { true, //reserved
                true, // forwardable
                true, // forwarded
                true, // proxiable
                true, // proxy
                true, // may-postdate 
                true, // postdated
                true, // invalid
                true, // renewable: <=== we test this
                true, // initial
                true, // pre-authent
                true // hw-authent 
        };

        //
        // test: should not renew ticket because renewTill < current time
        //
        Date newRenewTill = new Date((new Date()).getTime() - 3600000);

        KerberosTicket krbTicket = new KerberosTicket(ticket, pClient, pServer,
                sessionKey, KEY_TYPE, myFlags, authTime, startTime, endTime,
                newRenewTill, // <=== we test this: it is less then current time
                addesses);

        try {
            krbTicket.refresh();
            fail("No expected RefreshFailedException");
        } catch (RefreshFailedException e) {
        }

        //
        // test: should not renew ticket because renewable flag is false
        //
        newRenewTill = new Date((new Date()).getTime() + 3600000);
        myFlags[8] = false;

        krbTicket = new KerberosTicket(encTicket, pClient, pServer, sessionKey,
                KEY_TYPE, myFlags, // <=== we test this: it is not renewable
                authTime, startTime, endTime, newRenewTill, addesses);

        try {
            krbTicket.refresh();
            fail("No expected RefreshFailedException");
        } catch (RefreshFailedException e) {
        }

        //
        // test: dependency on system props 'kdc' and 'realm'
        //

        // verify that env. is clean
        assertNull(System.getProperty(ENV_KDC));
        assertNull(System.getProperty(ENV_REALM));

        // create real DES key
        byte[] newSessionKey = new KerberosKey(new KerberosPrincipal(
                "me@MY.REALM"), "pwd".toCharArray(), "DES").getEncoded();

        myFlags[8] = true;
        krbTicket = new KerberosTicket(encTicket, pClient, pServer,
                newSessionKey, KEY_TYPE, myFlags, authTime, startTime, endTime,
                newRenewTill, addesses);

        // case 1: unset 'kdc' and set 'realm'
        TestUtils.setSystemProperty(ENV_KDC, "some_value");
        try {
            krbTicket.refresh();
            fail("No expected RefreshFailedException");
        } catch (RefreshFailedException e) {
        } finally {
            TestUtils.setSystemProperty(ENV_KDC, null);
        }

        // case 2: set 'kdc' and unset 'realm' sys.props
        TestUtils.setSystemProperty(ENV_REALM, "some_value");
        try {
            krbTicket.refresh();
            fail("No expected RefreshFailedException");
        } catch (RefreshFailedException e) {
        } finally {
            TestUtils.setSystemProperty(ENV_REALM, null);
        }

        // TODO test: ticket refreshing 
    }
    
    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#equals(java.lang.Object)
     */
    public void test_equals() throws Exception {
        KerberosTicket krbTicket1 = new KerberosTicket(ticket, pClient,
                pServer, sessionKey, KEY_TYPE, flags, authTime, startTime,
                endTime, renewTill, addesses);
        KerberosTicket krbTicket2 = new KerberosTicket(ticket, pClient,
                pServer, sessionKey, KEY_TYPE, flags, authTime, startTime,
                endTime, renewTill, addesses);
        KerberosTicket krbTicket3 = new KerberosTicket(ticket, pClient,
                pServer, sessionKey, KEY_TYPE, new boolean[] { true, false },
                authTime, startTime, endTime, renewTill, addesses);
        assertEquals("krbTicket1 and krbTicket2 should be equivalent ",
                krbTicket1, krbTicket2);
        assertFalse("krbTicket1 and krbTicket3 sholudn't be equivalent ",
                krbTicket1.equals(krbTicket3));
        try {
            krbTicket2.destroy();
        } catch (DestroyFailedException e) {
            fail("krbTicket2 destroy failed");
        }
        assertFalse("Destroyed krbTicket sholudn't be equivalent ", krbTicket1
                .equals(krbTicket2));
        
        //Regression test for KerberosTicket.equals().
        final KerberosPrincipal clientPrincipal = new KerberosPrincipal(
                "leo@EXAMPLE.COM");
        final KerberosPrincipal serverPrincipal = new KerberosPrincipal(
                "krbtgt/EXAMPLE.COM@EXAMPLE.COM");
        KerberosTicket tgt = new KerberosTicket(new byte[0], clientPrincipal,
                serverPrincipal, new byte[0], 1, new boolean[0],
                new Date(1000), null, new Date(new Date().getTime() + 1000),
                null, null);
        assertEquals(tgt, tgt);
        KerberosTicket tgt1 = new KerberosTicket(new byte[0], clientPrincipal,
                serverPrincipal, new byte[0], 1, new boolean[0],
                new Date(1000), null, new Date(new Date().getTime() + 1000),
                null, null);
        assertEquals(tgt, tgt1);
        assertEquals(tgt1, tgt);
        
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosTicket#hashCode()
     */
    public void test_hashCode() {
        KerberosTicket krbTicket1 = new KerberosTicket(ticket, pClient,
                pServer, sessionKey, KEY_TYPE, flags, authTime, startTime,
                endTime, renewTill, addesses);
        KerberosTicket krbTicket2 = new KerberosTicket(ticket, pClient,
                pServer, sessionKey, KEY_TYPE, flags, authTime, startTime,
                endTime, renewTill, addesses);
        assertEquals("krbTicket1 and krbTicket2 should be equivalent",
                krbTicket1, krbTicket2);
        assertEquals("hashCode should be equivalent", krbTicket1.hashCode(),
                krbTicket2.hashCode());
    }

    // Hands-created ticket encoding:
    // - tkt-vno: 5
    // - realm: 'MY.REALM'
    // - sname: {type=0, string=krbtgt/MY.REALM}
    // - enc-part: {etype=3,kvno=1,cipher=0} (i.e. it is empty)
    private static final byte[] encTicket = {
            // [APPLICATION 1]
            (byte) 0x61,
            (byte) 0x45,
            // SEQUENCE 
            (byte) 0x30,
            (byte) 0x43,

            // tkt-vno [0] INTEGER (5)
            (byte) 0xa0,
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x05,

            // realm [1] Realm = 'MY.REALM'
            (byte) 0xa1, (byte) 0x0a, (byte) 0x1b, (byte) 0x08, (byte) 0x4d,
            (byte) 0x59, (byte) 0x2e, (byte) 0x52,
            (byte) 0x45,
            (byte) 0x41,
            (byte) 0x4c,
            (byte) 0x4d,

            // sname [2] PrincipalName
            (byte) 0xa2,
            (byte) 0x1d,
            (byte) 0x30,
            (byte) 0x1b,
            // name-type
            (byte) 0xa0, (byte) 0x03,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x00,
            // name-string: SEQUENCE OF krbtgt/MY.REALM
            (byte) 0xa1, (byte) 0x14, (byte) 0x30, (byte) 0x12, (byte) 0x1b,
            (byte) 0x06, (byte) 0x6b, (byte) 0x72, (byte) 0x62, (byte) 0x74,
            (byte) 0x67, (byte) 0x74, (byte) 0x1b, (byte) 0x08, (byte) 0x4d,
            (byte) 0x59, (byte) 0x2e, (byte) 0x52, (byte) 0x45, (byte) 0x41,
            (byte) 0x4c, (byte) 0x4d,

            // enc-part [3] EncryptedData 
            (byte) 0xa3, (byte) 0x11,
            // SEQUENCE
            (byte) 0x30, (byte) 0x0F,
            // etype
            (byte) 0xa0, (byte) 0x03, (byte) 0x02, (byte) 0x01, (byte) 0x03,
            // kvno
            (byte) 0xa1, (byte) 0x03, (byte) 0x02, (byte) 0x01, (byte) 0x01,
            // cipher  
            (byte) 0xa2, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x00 };
}

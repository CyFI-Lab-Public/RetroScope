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

package org.apache.harmony.auth.tests.javax.security.auth.kerberos.serialization;

import java.io.Serializable;
import java.util.Arrays;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class KerberosKeyTest extends TestCase {

    private static final KerberosPrincipal PRINCIPAL = new KerberosPrincipal(
            "principal@apache.org");

    private static final byte[] KEY_BYTES = { 0x00, 0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07 };

    private static final int KEY_TYPE = 22;

    private static final int VERSION_NUM = 33;

    // comparator for KerberosKey objects
    private static final SerializableAssert COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            KerberosKey initKey = (KerberosKey) initial;
            KerberosKey desrKey = (KerberosKey) deserialized;

            assertEquals("Principal", initKey.getPrincipal(), desrKey
                    .getPrincipal());
            assertTrue("Bytes", Arrays.equals(initKey.getEncoded(), desrKey
                    .getEncoded()));
            assertEquals("Type", initKey.getKeyType(), desrKey.getKeyType());
            assertEquals("Version", initKey.getVersionNumber(), desrKey
                    .getVersionNumber());
        }
    };

    /**
     * @tests serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {
        SerializationTest.verifySelf(new KerberosKey(PRINCIPAL, KEY_BYTES,
                KEY_TYPE, VERSION_NUM), COMPARATOR);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {
        SerializationTest.verifyGolden(this, new KerberosKey(PRINCIPAL,
                KEY_BYTES, KEY_TYPE, VERSION_NUM), COMPARATOR);
    }
}

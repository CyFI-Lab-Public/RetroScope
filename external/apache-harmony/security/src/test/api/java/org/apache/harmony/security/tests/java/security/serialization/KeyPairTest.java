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
* @author Vladimir N. Molotkov
*/

package org.apache.harmony.security.tests.java.security.serialization;

import java.io.Serializable;
import java.security.Key;
import java.security.KeyPair;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.PrivateKeyStub;
import org.apache.harmony.security.tests.support.PublicKeyStub;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;


/**
 * Tests for <code>KeyPair</code> serialization
 */
public class KeyPairTest extends TestCase {

    // key pair for testing
    private KeyPair keyPair;

    protected void setUp() throws Exception {
        PrivateKeyStub privateKey = new PrivateKeyStub("privateAlgorithm",
                "privateFormat", new byte[] { 0x00, 0x05, 0x10 });
        PublicKeyStub publicKey = new PublicKeyStub("publicAlgorithm",
                "publicFormat", new byte[] { 0x01, 0x02, 0x12 });

        keyPair = new KeyPair(publicKey, privateKey);
    }

    /**
     * Tests serialization compatibility
     */
    public void testSerializationCompatibility() throws Exception {

        SerializationTest.verifyGolden(this, keyPair, comparator);
    }
    
    /**
     * Tests serialization/deserialization
     */
    public void testSerializationSelf() throws Exception {

        SerializationTest.verifySelf(keyPair, comparator);
    }


    // comparator for KeyPair objects
    private static SerializableAssert comparator = new SerializableAssert(){
        public void assertDeserialized(Serializable reference, Serializable test) {

            // check result: compare public keys
            Key key1 = ((KeyPair)test).getPublic();
            Key key2 = ((KeyPair)reference).getPublic();

            assertEquals("PublicKey class", key1.getClass(), key2.getClass());
            assertEquals("PublicKey algorithm", key1.getAlgorithm(), key2
                    .getAlgorithm());
            assertEquals("PublicKey format", key1.getFormat(), key2.getFormat());
            assertTrue("PublicKey encoded", Arrays.equals(key1.getEncoded(),
                    key2.getEncoded()));

            // check result: compare private keys
            key1 = ((KeyPair)test).getPrivate();
            key2 = ((KeyPair)reference).getPrivate();

            assertEquals("PrivateKey class", key1.getClass(), key2.getClass());
            assertEquals("PrivateKey algorithm", key1.getAlgorithm(), key2
                    .getAlgorithm());
            assertEquals("PrivateKey format", key1.getFormat(), key2.getFormat());
            assertTrue("PrivateKey encoded", Arrays.equals(key1.getEncoded(),
                    key2.getEncoded()));
        }
    };
}

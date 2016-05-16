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

package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.harmony.crypto.tests.support.MyKeyAgreementSpi;

import junit.framework.TestCase;

/**
 * Tests for <code>KeyAgreementSpi</code> class constructors and methods.
 * 
 */

public class KeyAgreementSpiTest extends TestCase {
    /**
     * Constructor for KeyAgreementSpiTests.
     * 
     * @param arg0
     */
    public KeyAgreementSpiTest(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>KeyAgreementSpi</code> constructor Assertion: constructs
     * KeyAgreementSpi
     */
    public void testKeyAgreementSpi01() throws InvalidKeyException,
            ShortBufferException, NoSuchAlgorithmException, 
            InvalidAlgorithmParameterException {
        KeyAgreementSpi kaSpi = new MyKeyAgreementSpi();

        assertNull("Not null result", kaSpi.engineDoPhase(null, true));
        try {
            kaSpi.engineDoPhase(null, false);
            fail("IllegalStateException must be thrown");
        } catch (IllegalStateException e) {
        }
        byte[] bb = kaSpi.engineGenerateSecret();
        assertEquals("Length is not 0", bb.length, 0);        
        assertEquals("Returned integer is not 0", 
                kaSpi.engineGenerateSecret(new byte[1], 10), 
                -1);
        assertNull("Not null result", kaSpi.engineGenerateSecret("aaa"));
        try {
            kaSpi.engineGenerateSecret("");
            fail("NoSuchAlgorithmException must be thrown");
        } catch (NoSuchAlgorithmException e) {
        }
        Key key = null;
        try {
            kaSpi.engineInit(key, new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        AlgorithmParameterSpec params = null;
        try {
            kaSpi.engineInit(key, params, new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }        
    }
}
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

package org.apache.harmony.security.tests.java.security.interfaces;


import java.math.BigInteger;
import java.security.interfaces.RSAPrivateCrtKey;

import junit.framework.TestCase;

/**
 * Tests for <code>RSAPrivateCrtKey</code> class field
 * 
 */
public class RSAPrivateCrtKeyTest extends TestCase {

    /**
     * Constructor for RSAPrivateCrtKeyTest.
     * 
     * @param arg0
     */
    public RSAPrivateCrtKeyTest(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>serialVersionUID</code> field
     */
    public void testField() {
        checkRSAPrivateCrtKey key = new checkRSAPrivateCrtKey();
        assertEquals("Incorrect serialVersionUID",
                key.getSerVerUID(), //RSAPrivateCrtKey.serialVersionUID
                -5682214253527700368L);
    }
    public class checkRSAPrivateCrtKey implements RSAPrivateCrtKey {
        public String getAlgorithm() {
            return "RSAPrivateCrtKey";
        }
        public String getFormat() {
            return "Format";
        }
        public byte[] getEncoded() {
            return new byte[0];
        }
        public long getSerVerUID() {
            return serialVersionUID;
        }
        public BigInteger getCrtCoefficient() {
            return null;
        }
        public BigInteger getPrimeP() {
            return null;
        }
        public BigInteger getPrimeQ() {
            return null;
        }
        public BigInteger getPrimeExponentP() {
            return null;
        }
        public BigInteger getPrimeExponentQ() {
            return null;
        }
        public BigInteger getPublicExponent() {
            return null;
        }
        public BigInteger getModulus() {
            return null;
        }
        public BigInteger getPrivateExponent() {
            return null;
        }
        
    }
}

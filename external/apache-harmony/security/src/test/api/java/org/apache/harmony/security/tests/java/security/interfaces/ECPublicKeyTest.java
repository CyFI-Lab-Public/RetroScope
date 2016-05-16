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


import java.security.spec.ECPoint;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;

import junit.framework.TestCase;

/**
 * Tests for <code>ECPublicKey</code> class field
 * 
 */
public class ECPublicKeyTest extends TestCase {

    /**
     * Constructor for ECPublicKeyTest.
     * 
     * @param arg0
     */
    public ECPublicKeyTest(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>serialVersionUID</code> field
     */
    public void testField() {
        checkECPublicKey key = new checkECPublicKey();
        assertEquals("Incorrect serialVersionUID",
                key.getSerVerUID(), // ECPublicKey.serialVersionUID
                -3314988629879632826L);
    }
    public class checkECPublicKey implements ECPublicKey {
        public String getAlgorithm() {
            return "ECPublicKey";
        }
        public String getFormat() {
            return "Format";
        }
        public byte[] getEncoded() {
            return new byte[0];
        }        
        public ECPoint getW() {
            return null;
        }
        public ECParameterSpec getParams() {
            return null;
        }
        public long getSerVerUID() {
            return serialVersionUID;
        }
    }
}

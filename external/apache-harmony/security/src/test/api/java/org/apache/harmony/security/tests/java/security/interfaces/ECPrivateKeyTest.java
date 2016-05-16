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
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;

import junit.framework.TestCase;

/**
 * Tests for <code>ECPrivateKey</code> class field
 * 
 */
public class ECPrivateKeyTest extends TestCase {

    /**
     * Constructor for ECPrivateKeyTest.
     * 
     * @param arg0
     */
    public ECPrivateKeyTest(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>serialVersionUID</code> field
     */
    public void testField() {
        checkECPrivateKey k = new checkECPrivateKey();
        assertEquals("Incorrect serialVersionUID",
                k.getSerVerUID(), //ECPrivateKey.serialVersionUID
                -7896394956925609184L);
    }
    
    public class checkECPrivateKey implements ECPrivateKey {
        public String getAlgorithm() {
            return "ECPrivateKey";
        }
        public String getFormat() {
            return "Format";
        }
        public byte[] getEncoded() {
            return new byte[0];
        }        
        public BigInteger getS() {
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

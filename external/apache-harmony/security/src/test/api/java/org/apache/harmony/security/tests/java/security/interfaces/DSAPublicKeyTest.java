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
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;

import junit.framework.TestCase;

/**
 * Tests for <code>DSAPublicKey</code> class field
 * 
 */
public class DSAPublicKeyTest extends TestCase {

    /**
     * Constructor for DSAPublicKeyTest.
     * 
     * @param arg0
     */
    public DSAPublicKeyTest(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>serialVersionUID</code> field
     */
    public void testField() {
        checkDSAPublicKey k = new checkDSAPublicKey();
        assertEquals("Incorrect serialVersionUID",
                k.getSerVerUID(), //DSAPublicKey.serialVersionUID 
                1234526332779022332L);
    }
    public class checkDSAPublicKey implements DSAPublicKey {
        public String getAlgorithm() {
            return "DSAPublicKey";
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
        public BigInteger getY() {
            return null;
        }
        public DSAParams getParams() {
            return null;
        }        
    }
}

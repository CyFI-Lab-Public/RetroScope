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

package org.apache.harmony.security.tests.java.security.spec;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;

import junit.framework.TestCase;

/**
 * Tests for <code>DSAParameterSpec</code>
 * 
 */
public class DSAParameterSpecTest extends TestCase {

    /**
     * Constructor for DSAParameterSpecTest.
     * @param name
     */
    public DSAParameterSpecTest(String name) {
        super(name);
    }

    /**
     * Ctor test 
     */
    public final void testDSAParameterSpec() {
        AlgorithmParameterSpec aps = new DSAParameterSpec(
                new BigInteger("1"),
                new BigInteger("2"),
                new BigInteger("3"));
        
        assertTrue(aps instanceof DSAParameterSpec);
    }

    /**
     * getG() test
     */
    public final void testGetG() {
        DSAParameterSpec dps = new DSAParameterSpec(
                new BigInteger("1"),
                new BigInteger("2"),
				new BigInteger("3"));
        
        assertEquals(3, dps.getG().intValue());
    }

    /**
     * getP() test
     */
    public final void testGetP() {
        DSAParameterSpec dps = new DSAParameterSpec(
                new BigInteger("1"),
                new BigInteger("2"),
                new BigInteger("3"));
        
        assertEquals(1, dps.getP().intValue());
    }

    /**
     * getQ() test 
     */
    public final void testGetQ() {
        DSAParameterSpec dps = new DSAParameterSpec(
                new BigInteger("1"),
                new BigInteger("2"),
                new BigInteger("3"));
        
        assertEquals(2, dps.getQ().intValue());
    }
}

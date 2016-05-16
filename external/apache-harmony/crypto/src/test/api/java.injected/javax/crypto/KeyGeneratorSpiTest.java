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
import java.security.SecureRandom;

import java.security.spec.AlgorithmParameterSpec;

import org.apache.harmony.crypto.tests.support.MyKeyGeneratorSpi;

import junit.framework.TestCase;

/**
 * Tests for <code>KeyGeneratorSpi</code> class constructors and methods.
 * 
 */

public class KeyGeneratorSpiTest extends TestCase {

    /**
     * Constructor for KeyGeneratorSpiTests.
     * 
     * @param arg0
     */
    public KeyGeneratorSpiTest(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>KeyGeneratorSpi</code> constructor Assertion: constructs
     * KeyGeneratorSpi
     */
    public void testKeyGeneratorSpi01() throws InvalidAlgorithmParameterException {
        KeyGeneratorSpi kgSpi = new MyKeyGeneratorSpi();
        assertNull("Not null result", kgSpi.engineGenerateKey());
        try {
            kgSpi.engineInit(77, new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            kgSpi.engineInit(new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        AlgorithmParameterSpec aps = null;        
        try {
            kgSpi.engineInit(aps, new SecureRandom());
            fail("InvalidAlgorithmParameterException must be thrown when parameter is null");            
       } catch (InvalidAlgorithmParameterException e) {
       }
       aps = new APSpecSpi();
       kgSpi.engineInit(aps, new SecureRandom());
    }
}
class APSpecSpi implements AlgorithmParameterSpec {
    
}

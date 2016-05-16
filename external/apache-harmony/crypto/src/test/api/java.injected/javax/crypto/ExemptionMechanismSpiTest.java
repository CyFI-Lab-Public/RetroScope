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

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.harmony.crypto.tests.support.MyExemptionMechanismSpi;

import junit.framework.TestCase;


/**
 * Tests for <code>ExemptionMechanismSpi</code> class constructors and
 * methods.
 * 
 */

public class ExemptionMechanismSpiTest extends TestCase {
    /**
     * Constructor for ExemptionMechanismSpiTests.
     * 
     * @param arg0
     */
    public ExemptionMechanismSpiTest(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>ExemptionMechanismSpi</code> constructor Assertion:
     * constructs ExemptionMechanismSpi
     */
    public void testExemptionMechanismSpi01() 
            throws  ExemptionMechanismException,
            ShortBufferException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        ExemptionMechanismSpi emSpi = new MyExemptionMechanismSpi();
        int len = MyExemptionMechanismSpi.getLength();
        byte [] bbRes = emSpi.engineGenExemptionBlob();
        assertEquals("Incorrect length", bbRes.length, len);
        assertEquals("Incorrect result", 
                emSpi.engineGenExemptionBlob(new byte[1], len), len);
        assertEquals("Incorrect output size", 10, emSpi.engineGetOutputSize(100));
        Key key = null;
        AlgorithmParameters params = null;        
        AlgorithmParameterSpec parSpec = null;
        try {
            emSpi.engineInit(key);
            fail("InvalidKeyException must be thrown");
        } catch (InvalidKeyException e) {
        }
        try {
            emSpi.engineInit(key, params);
            fail("InvalidKeyException must be thrown");
        } catch (InvalidKeyException e) {
        }
        try {
            emSpi.engineInit(key, parSpec);
            fail("InvalidKeyException must be thrown");
        } catch (InvalidKeyException e) {
        }
        key = ((MyExemptionMechanismSpi)emSpi).new tmp1Key("Proba", new byte[0]);
        try {
            emSpi.engineInit(key);
            fail("ExemptionMechanismException must be thrown");
        } catch (ExemptionMechanismException e) {
        }
        try {
            emSpi.engineInit(key, params);
            fail("ExemptionMechanismException must be thrown");
        } catch (ExemptionMechanismException e) {
        }
        try {
            emSpi.engineInit(key, parSpec);
            fail("ExemptionMechanismException must be thrown");
        } catch (ExemptionMechanismException e) {
        }
        key = ((MyExemptionMechanismSpi)emSpi).new tmpKey("Proba", new byte[0]);
        emSpi.engineInit(key);
        emSpi.engineInit(key, params);
        emSpi.engineInit(key, parSpec);
        
        assertEquals("Incorrect result", 10, emSpi.engineGetOutputSize(100));
    }
}

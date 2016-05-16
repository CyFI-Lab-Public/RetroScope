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

package org.apache.harmony.security.tests.java.security;

import java.security.AlgorithmParameterGenerator;
import java.security.InvalidParameterException;
import java.security.Provider;
import java.security.SecureRandom;

import org.apache.harmony.security.tests.support.SpiEngUtils;

import junit.framework.TestCase;

/**
 * Tests for <code>AlgorithmParameterGenerator</code> class constructors and
 * methods.
 */
public class AlgorithmParameterGenerator_ImplTest extends TestCase {

    private static String validAlgName = "DSA";
            
    public static final String srvAlgorithmParameterGenerator = "AlgorithmParameterGenerator";


    private static String validProviderName = null;

    private static Provider validProvider = null;

    private static boolean DSASupported = false;

    static {
        validProvider = SpiEngUtils.isSupport(
                validAlgName,
                srvAlgorithmParameterGenerator);
        DSASupported = (validProvider != null);
        validProviderName = (DSASupported ? validProvider.getName() : null);
    }

    protected AlgorithmParameterGenerator[] createAPGen() throws Exception {
        if (!DSASupported) {
            fail(validAlgName + " algorithm is not supported");
            return null;
        }
        AlgorithmParameterGenerator[] apg = new AlgorithmParameterGenerator[3];
        apg[0] = AlgorithmParameterGenerator.getInstance(validAlgName);
        apg[1] = AlgorithmParameterGenerator.getInstance(validAlgName,
                validProvider);
        apg[2] = AlgorithmParameterGenerator.getInstance(validAlgName,
                validProviderName);
        return apg;
    }

    /**
     * Test for <code>init(int size)</code> and
     * <code>init(int size, SecureRandom random<code> methods
     * Assertion: throws InvalidParameterException when size is incorrect
     */
    public void testAlgorithmParameterGenerator11() throws Exception {        
        if (!DSASupported) {
            fail(validAlgName + " algorithm is not supported");
            return;
        }
        // Invalid key strengths (strength must be from 512 - 1024 and a multiple of 64)
        int [] keys = {-10000, -512, -1, 0, 10000};
        SecureRandom random = new SecureRandom();
        AlgorithmParameterGenerator[] apgs = createAPGen();
        assertNotNull("AlgorithmParameterGenerator objects were not created",
                apgs);

        for (int i = 0; i < apgs.length; i++) {
            
            // Test invalid strengths
            for (int j = 0; j < keys.length; j++) {
                try {
                    apgs[i].init(keys[j]);
                    fail("Expected an invalid parameter exception for strength "
                            + keys[j]);
                } catch (InvalidParameterException e) {
                    // expected
                }
                try {
                    apgs[i].init(keys[j], random);
                    fail("Expected an invalid parameter exception for strength "
                            + keys[j]);
                } catch (InvalidParameterException e) {
                    // expected
                }
                try {
                    apgs[i].init(keys[j], null);
                    fail("Expected an invalid parameter exception for strength "
                            + keys[j]);
                } catch (InvalidParameterException e) {
                    // expected
                }
            }
            
            // Test valid strengths
            apgs[i].init(512);
            apgs[i].init(512, random);
            apgs[i].init(512 + 64);
            apgs[i].init(512 + 64 + 64, random);
            apgs[i].init(1024);
            apgs[i].init(1024, random);
        }
    }
    
}

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

package org.apache.harmony.crypto.tests.javax.crypto;

import java.security.InvalidKeyException;
import java.security.Provider;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import org.apache.harmony.security.tests.support.SpiEngUtils;
import junit.framework.TestCase;
/**
 * Tests for <code>SecretKeyFactory</code> class constructors and methods.
 * 
 */

public class SecretKeyFactory_Impl1Test extends TestCase {
    
    public static final String srvSecretKeyFactory = "SecretKeyFactory";
        
    private static String defaultAlgorithm1 = "DESede";
    private static String defaultAlgorithm2 = "DES";
    
    public static String defaultAlgorithm = null;

    private static String defaultProviderName = null;

    private static Provider defaultProvider = null;
    
    private static boolean DEFSupported = false;

    private static final String NotSupportMsg = "Default algorithm is not supported";

    static {
        defaultProvider = SpiEngUtils.isSupport(defaultAlgorithm1,
                srvSecretKeyFactory);
        DEFSupported = (defaultProvider != null);
        if (DEFSupported) {
            defaultAlgorithm = defaultAlgorithm1;
            defaultProviderName = defaultProvider.getName();
        } else {
            defaultProvider = SpiEngUtils.isSupport(defaultAlgorithm2,
                    srvSecretKeyFactory);
            DEFSupported = (defaultProvider != null);
            if (DEFSupported) {
                defaultAlgorithm = defaultAlgorithm2;
                defaultProviderName = defaultProvider.getName();
            } else {
                defaultAlgorithm = null;
                defaultProviderName = null;
                defaultProvider = null;
            }
        }
    }

    protected SecretKeyFactory[] createSKFac() {
        if (!DEFSupported) {
            fail(defaultAlgorithm + " algorithm is not supported");
            return null;
        }
        SecretKeyFactory[] skF = new SecretKeyFactory[3];
        try {
            skF[0] = SecretKeyFactory.getInstance(defaultAlgorithm);
            skF[1] = SecretKeyFactory.getInstance(defaultAlgorithm,
                    defaultProvider);
            skF[2] = SecretKeyFactory.getInstance(defaultAlgorithm,
                    defaultProviderName);
            return skF;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Test for <code>translateKey(SecretKey key)</code> method
     * Assertion: 
     * throw InvalidKeyException if parameter is inappropriate
     */    
    public void testSecretKeyFactory11() throws InvalidKeyException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        byte[] bb = new byte[10];
        SecretKeySpec secKeySpec = new SecretKeySpec(bb,defaultAlgorithm);
        SecretKeyFactory[] skF = createSKFac();
        assertNotNull("SecretKeyFactory object were not created", skF);
        for (int i = 0; i < skF.length; i++) {
            try { 
                skF[i].translateKey(null);
                fail("InvalidKeyException must be thrown: " + i);
            } catch (InvalidKeyException e) {
            }

            skF[i].translateKey(secKeySpec);
        }
    }
}


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

package org.apache.harmony.security.tests.provider.crypto;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests against CryptoProvider.
 */
public class CryptoProviderTest extends TestCase {

    private static final String providerName = "Crypto";     // name of provider
    private static final String shaprng      = "SHA1PRNG";   // name of algorithm
    private static final String sha_1        = "SHA-1";      // name of algorithm
    private static final String sha_1_alias  = "SHA1";       // alias name
    private static final String sha_1_alias2 = "SHA";        // alias name

    private static final String dsaNames[] = { "SHA1withDSA",
                                               "SHAwithDSA",
                                               "DSAwithSHA1",
                                               "SHA1/DSA",
                                               "SHA/DSA",
                                               "SHA-1/DSA",
                                               "DSA",
                                               "DSS",
                                               "OID.1.2.840.10040.4.3",
                                               "1.2.840.10040.4.3",
                                               "1.3.14.3.2.13",
                                               "1.3.14.3.2.27" };

    private static final String keyFactoryNames[] = { "DSA",
                                                      "1.3.14.3.2.12",
                                                      "1.2.840.10040.4.1" };

    /**
     * Test against CryptoProvider() methods.
     */
    public void testCrypto() throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom sr;
        MessageDigest md;
        Signature sign;
        KeyFactory keyFactory;

        sr = SecureRandom.getInstance(shaprng, providerName);

        md = MessageDigest.getInstance(sha_1, providerName);
        md = MessageDigest.getInstance(sha_1_alias, providerName);
        md = MessageDigest.getInstance(sha_1_alias2, providerName);

        for ( int i = 0; i < dsaNames.length; i++ ) {
            sign = Signature.getInstance(dsaNames[i], providerName);
        }

        for ( int i = 0; i < keyFactoryNames.length; i++ ) {
            keyFactory = KeyFactory.getInstance(keyFactoryNames[i], providerName);
        }
    }


    public static Test suite() {
        return new TestSuite(CryptoProviderTest.class);
    }

 }

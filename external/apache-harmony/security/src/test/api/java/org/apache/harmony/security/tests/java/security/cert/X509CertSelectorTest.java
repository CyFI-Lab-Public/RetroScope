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

package org.apache.harmony.security.tests.java.security.cert;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509CertSelector;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * X509CertSelectorTest
 */
public class X509CertSelectorTest extends TestCase {

    /**
     * @tests java.security.cert.X509CertSelector#addSubjectAlternativeName(int, byte[])
     */
    public void test_addSubjectAlternativeNameLintLbyte_array() throws IOException {
        // Regression for HARMONY-2487
        int[] types = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
        for (int i = 0; i < types.length; i++) {
            try {
                new X509CertSelector().addSubjectAlternativeName(types[i],
                        (byte[]) null);
                fail("No expected NullPointerException for type: " + types[i]);
            } catch (NullPointerException e) {
            }
        }
    }

    /**
     * @tests java.security.cert.X509CertSelector#addSubjectAlternativeName(int, String)
     */
    public void test_addSubjectAlternativeNameLintLjava_lang_String() throws IOException {
        // Regression for HARMONY-727
        int[] types = { 0, 3, 4, 5, 6, 7, 8 };
        for (int i = 0; i < types.length; i++) {
            try {
                new X509CertSelector().addSubjectAlternativeName(types[i],
                        "0xDFRF");
                fail("IOException expected for type: " + types[i]);
            } catch (IOException e) {
            }
        }
        // Tests for DNSGeneralName
        // Legal DNS names
        new X509CertSelector().addSubjectAlternativeName(2, "0xDFRF");
        new X509CertSelector().addSubjectAlternativeName(2, "");
        new X509CertSelector().addSubjectAlternativeName(2, "foo.example.com");
        new X509CertSelector().addSubjectAlternativeName(2, "3g.example.com");
        new X509CertSelector().addSubjectAlternativeName(2, "*.example.com");
        // Illegal DNS names
        String[] names = new String[] {"*", "*.", "%anything"};
        for (String badName : names) {
            try {
                new X509CertSelector().addSubjectAlternativeName(2, badName);
                fail("IOException expected for DNS name " + badName);
            } catch (IOException e) {
                // Expected
            }
        }
    }

    /**
     * @tests java.security.cert.X509CertSelector#addPathToName(int, byte[])
     */
    public void test_addPathToNameLintLbyte_array() throws IOException {
        // Regression for HARMONY-2487
        int[] types = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
        for (int i = 0; i < types.length; i++) {
            try {
                new X509CertSelector().addPathToName(types[i], (byte[]) null);
                fail("No expected NullPointerException for type: " + types[i]);
            } catch (NullPointerException e) {
            }
        }
    }

    /**
     * @tests java.security.cert.X509CertSelector#addPathToName(int, String)
     */
    public void test_addPathToNameLintLjava_lang_String() {
        // Regression for HARMONY-724
        for (int type = 0; type <= 8; type++) {
            try {
                new X509CertSelector().addPathToName(type, (String) null);
                fail("IOException expected for type: " + type);
            } catch (IOException ioe) {
                // expected
            }
        }
    }
    
    /**
     * @tests java.security.cert.X509CertSelector#setSubjectPublicKey(byte[])
     */
    public void test_setSubjectPublicKeyLB$() throws Exception {

        //SubjectPublicKeyInfo  ::=  SEQUENCE  {
        //    algorithm            AlgorithmIdentifier,
        //    subjectPublicKey     BIT STRING  }
        byte[] enc = { 0x30, 0x0E, // SEQUENCE
                0x30, 0x07, // SEQUENCE
                0x06, 0x02, 0x03, 0x05,//OID
                0x01, 0x01, 0x07, //ANY
                0x03, 0x03, 0x01, 0x01, 0x06, // subjectPublicKey
        };

        X509CertSelector selector = new X509CertSelector();

        selector.setSubjectPublicKey(enc);
        PublicKey key = selector.getSubjectPublicKey();
        assertEquals("0.3.5", key.getAlgorithm());
        assertEquals("X.509", key.getFormat());
        assertTrue(Arrays.equals(enc, key.getEncoded()));
        assertNotNull(key.toString());
    }
}

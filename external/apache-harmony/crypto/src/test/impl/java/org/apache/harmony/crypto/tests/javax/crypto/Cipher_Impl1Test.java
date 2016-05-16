/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.crypto.tests.javax.crypto;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

public class Cipher_Impl1Test extends junit.framework.TestCase {
    
    static Key cipherKey;
    final static String algorithm = "DESede";
    final static int keyLen = 168;
    
    static {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(algorithm);
            kg.init(keyLen, new SecureRandom());
            cipherKey = kg.generateKey();
        } catch (Exception e) {
            fail("No key " + e);
        }
    }


    /**
     * @tests javax.crypto.Cipher#getIV()
     * @tests javax.crypto.Cipher#init(int, java.security.Key,
     *        java.security.AlgorithmParameters)
     */
    public void test_getIV() throws Exception {
        /*
         * If this test is changed, implement the following:
         * test_initILjava_security_KeyLjava_security_AlgorithmParameters()
         */

        SecureRandom sr = new SecureRandom();
        Cipher cipher = null;

        byte[] iv = new byte[8];
        sr.nextBytes(iv);
        AlgorithmParameters ap = AlgorithmParameters.getInstance(algorithm);
        ap.init(iv, "RAW");

        cipher = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, cipherKey, ap);

        byte[] cipherIV = cipher.getIV();

        assertTrue("IVs differ", Arrays.equals(cipherIV, iv));
    }

    /**
     * @tests javax.crypto.Cipher#getParameters()
     * @tests javax.crypto.Cipher#init(int, java.security.Key,
     *        java.security.AlgorithmParameters, java.security.SecureRandom)
     */
    public void test_getParameters() throws Exception {

        /*
         * If this test is changed, implement the following:
         * test_initILjava_security_KeyLjava_security_AlgorithmParametersLjava_security_SecureRandom()
         */

        SecureRandom sr = new SecureRandom();
        Cipher cipher = null;

        byte[] apEncoding = null;

        byte[] iv = new byte[8];
        sr.nextBytes(iv);

        AlgorithmParameters ap = AlgorithmParameters.getInstance("DESede");
        ap.init(iv, "RAW");
        apEncoding = ap.getEncoded("ASN.1");

        cipher = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, cipherKey, ap, sr);

        byte[] cipherParmsEnc = cipher.getParameters().getEncoded("ASN.1");
        assertTrue("Parameters differ", Arrays.equals(apEncoding,
                cipherParmsEnc));
    }
}

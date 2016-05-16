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

package org.apache.harmony.xnet.provider.jsse;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import junit.framework.TestCase;

/**
 * Tests for <code>DigitalSignature</code> constructor and methods
 *  
 */
public class DigitalSignatureTest extends TestCase {
    
    private PrivateKey key;
    private Certificate cert;
    
    @Override
    public void setUp() throws Exception {

        char[] pwd = JSSETestData.KS_PASSWORD;
        KeyStore ks = JSSETestData.getKeyStore();
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks
                .getEntry("ssl_test_store",
                        new KeyStore.PasswordProtection(pwd));
        key = entry.getPrivateKey();
        cert = entry.getCertificate();
    }

    public void testDigitalSignature_1() throws Exception {

        MessageDigest md5 = null;
        MessageDigest sha = null;

        md5 = MessageDigest.getInstance("MD5");
        sha = MessageDigest.getInstance("SHA-1");

        DigitalSignature ds_sign = new DigitalSignature(
                CipherSuite.KeyExchange_RSA_EXPORT);
        DigitalSignature ds_verify = new DigitalSignature(
                CipherSuite.KeyExchange_RSA_EXPORT);
        ds_sign.init(key);
        // use pivateKeyEncoding as signed data
        byte[] pivateKeyEncoding = key.getEncoded();
        ds_sign.update(pivateKeyEncoding);
        byte[] hash = ds_sign.sign();
        
        // verify
        byte[] md5_hash = new byte[16];
        byte[] sha_hash = new byte[20];
        sha.update(pivateKeyEncoding);
        md5.update(pivateKeyEncoding);

        sha.digest(sha_hash, 0, sha_hash.length);
        md5.digest(md5_hash, 0, md5_hash.length);

        ds_verify.init(cert);
        ds_verify.setMD5(md5_hash);
        ds_verify.setSHA(sha_hash);
        
        assertTrue(ds_verify.verifySignature(hash));
    }

    public void testDigitalSignature_2() throws Exception {      

        DigitalSignature ds_sign = new DigitalSignature(
                CipherSuite.KeyExchange_RSA_EXPORT);
        DigitalSignature ds_verify = new DigitalSignature(
                CipherSuite.KeyExchange_RSA_EXPORT);
        ds_sign.init(key);
        
        byte[] md5_hash = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
                14, 15, 16 };
        byte[] sha_hash = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
                14, 15, 16, 17, 18, 19, 20 };
        ds_sign.setMD5(md5_hash);
        ds_sign.setSHA(sha_hash);      
        byte[] hash = ds_sign.sign();      

        // verify
        ds_verify.init(cert);
        ds_verify.setMD5(md5_hash);
        ds_verify.setSHA(sha_hash);
        assertTrue(ds_verify.verifySignature(hash));
    }

}
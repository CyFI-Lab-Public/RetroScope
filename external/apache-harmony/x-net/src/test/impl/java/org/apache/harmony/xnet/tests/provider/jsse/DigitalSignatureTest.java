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

package org.apache.harmony.xnet.tests.provider.jsse;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import junit.framework.TestCase;

import org.apache.harmony.luni.util.Base64;
import org.apache.harmony.xnet.provider.jsse.DigitalSignature;

/**
 * Tests for <code>DigitalSignature</code>class
 */
public class DigitalSignatureTest extends TestCase {

    public void test_Sign() throws Exception {

        // Regression for HARMONY-2029
        // data arrays were taken from HARMONY-2125

        byte[] b64PublicKeySpec = ("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk7q/qebK6/tSLydRUCAvwcqRlS95aau5"
                + "xj2fFpLRYYG0avuO0qXn14HNmoC8kztk66Q+4oZS9SkQYwr24x+9ide0s9xfjQ5ohDpY6P+mtD6k"
                + "0piKwYmLXtqi7BTgpgoDXtYi6VJYzvBxLhe050vi1lUNe2iCl/jsU4IcBCcOjV4CwbTDRhq6PzT7"
                + "70uWtMhAV28E/jcszlyxHYZ5qK0wp8BoBdcNQf3tihBuQkrsv57z94tbEJxg5JeMOl1aWVtw6LLR"
                + "K2GQBaDrwvy7R4FA2oOc/JS9PsiT0ieKO1dhPGmqJDaVMlZMFeUY41hTzU3BAmcjYBWQI2oNqHRv"
                + "ya9tUQIDAQAB").getBytes("UTF-8");

        byte[] b64PrivateKeySpec = ("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCTur+p5srr+1IvJ1FQIC/BypGV"
                + "L3lpq7nGPZ8WktFhgbRq+47SpefXgc2agLyTO2TrpD7ihlL1KRBjCvbjH72J17Sz3F+NDmiEOljo"
                + "/6a0PqTSmIrBiYte2qLsFOCmCgNe1iLpUljO8HEuF7TnS+LWVQ17aIKX+OxTghwEJw6NXgLBtMNG"
                + "Gro/NPvvS5a0yEBXbwT+NyzOXLEdhnmorTCnwGgF1w1B/e2KEG5CSuy/nvP3i1sQnGDkl4w6XVpZ"
                + "W3DostErYZAFoOvC/LtHgUDag5z8lL0+yJPSJ4o7V2E8aaokNpUyVkwV5RjjWFPNTcECZyNgFZAj"
                + "ag2odG/Jr21RAgMBAAECggEAJevfPUbQOilGXHJUTiQk/jL4kfogyX5absfsqYfAla4M2RWAARSz"
                + "Yb+hPpLjVUv+yPpdZhqi+umymin7XCwOpG6ppS3hnTzgmWi83/qYGVanSqP7olijXRL0lXN6g0S4"
                + "vsRrK8eGooBYHUPanTD+ppQopNAcDdTJHVqdxHceJi1gum4c3yZmoe510TjlqpOezAO6B0N1dfJf"
                + "KEIQjMdlWgKRq2WNzPDFO8Luhyg40PoTcoWZRhKWV5xPBmq4ew4epxuZpNnuVIzxs9gpjuK55xFg"
                + "ukXyyhmjsfJzdRs/9kToJdGU3prxf716fk+7OgzgdfWTXK0/uVBE9JIUPBxuAQKBgQDvT+jtTLYN"
                + "781CujruJUGIn5HHAsqzs1PuKhXS3Nb/+UPkdz0SK3BqReDWIF7db5bxcDDsbfe+OP90DGhd5O6w"
                + "QbuWA7PdyUJiz2tZ2gBZwvfKYJ/N45KYrFv+f2YmIqhS8lOCV9OoU1/snn8kHRa/UlFfWc8sXSub"
                + "frWjPAT+4QKBgQCeB/KHW5nv0vjifx3afxo6ZPfD4QOtDRkG9mduzJFkFE5pFFuaLVMHuXph+RFL"
                + "/vxcukEZOdBBy0/srXN5PV/1kwrhXcRh5YUq8UHF4ExYctCnSn0NbMPazVHY5HSCVwWdNrMk3jIb"
                + "vNBnTMXtoi7XpSDHM3nk4mF41KZc8nRscQKBgQCQyMXhm8GhWO3UaxtwLTYi1He78aJ1ag9jTi75"
                + "7gZdw48h0EowjftrMG/A3MDIM9UcqYXP5RA4E//pVABonjMSjBJTxlWx3yu84ETQjaYcqGqGFENa"
                + "q05r9AuMQ8OnWtx/ooCHoV86vYaRf6roTHkQs1rr10gSTSQu5VA7O/rBwQKBgElAUdEgSqh52FfU"
                + "qFfhVpz+tEIdiQCr84/go20ecb48E2RtxVAf9j68YNgNBVF+rielRguVWs1EmpWQiNgH9PT15bM1"
                + "LZRbOXEAR4abQ4g0IDeLNZAFfHttdKTesIrCH54R/tP2Eq/8w3U+hvhxltjqd9keKUBJFvlVSJAI"
                + "6qJhAoGBAJU8ET4NHv4/AS3SiTcg65TE4ktrTsVZixNYVORE32bFZCZClWLI2aUattiIk8UqvG74"
                + "wugnxUGk8tJMcw1J/gh+UlnGrX7HqiWTCzEyepOiDQV3NkOQ+9z9WeQNNUtQyIFZZB4wQHaH21BB"
                + "a1kmrzyqihpAVWrpBOJCipel8S0X").getBytes("UTF-8");

        // Create public/private keys and certificate
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory
                .generatePrivate(new PKCS8EncodedKeySpec(Base64
                        .decode(b64PrivateKeySpec)));
        final PublicKey publicKey = keyFactory
                .generatePublic(new X509EncodedKeySpec(Base64
                        .decode(b64PublicKeySpec)));

        @SuppressWarnings("serial")
        Certificate cert = new Certificate("myType") {

            @Override
            public PublicKey getPublicKey() {
                return publicKey;
            }

            @Override
            public byte[] getEncoded() {
                return null;
            }

            @Override
            public String toString() {
                return null;
            }

            @Override
            public void verify(PublicKey key) throws CertificateException,
                    NoSuchAlgorithmException, InvalidKeyException,
                    NoSuchProviderException, SignatureException {
            }

            @Override
            public void verify(PublicKey key, String sigProvider)
                    throws CertificateException, NoSuchAlgorithmException,
                    InvalidKeyException, NoSuchProviderException,
                    SignatureException {
            }
        };

        // Sign first: init DigitalSignature with md5 and sha1
        // CipherSuite.KeyExchange_RSA_EXPORT == 2
        DigitalSignature dsig = new DigitalSignature(2);

        dsig.init(privateKey);

        byte[] md5 = new byte[] {
                0x00, // <=== this is a problem byte (see HARMONY-2125) 
                0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a,
                0x0b, 0x0c, 0x0d, 0x0e, 0x0f };

        byte[] sha1 = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10,
                0x11, 0x12, 0x13 };

        dsig.setMD5(md5);
        dsig.setSHA(sha1);

        byte[] enc = dsig.sign();

        // Now let's verify
        dsig.init(cert);
        assertTrue(dsig.verifySignature(enc));
    }
}

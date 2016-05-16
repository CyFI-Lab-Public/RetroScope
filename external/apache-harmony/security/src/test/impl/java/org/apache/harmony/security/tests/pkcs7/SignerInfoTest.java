/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.harmony.security.tests.pkcs7;

import java.math.BigInteger;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;

import org.apache.harmony.security.pkcs7.SignerInfo;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;

public class SignerInfoTest extends TestCase {

    public void testEncode() throws Exception {

        Object[] issuerAndSerialNumber = new Object[] { new Name("CN=test"),
                BigInteger.TEN.toByteArray() };

        SignerInfo signerInfo = new SignerInfo(1, issuerAndSerialNumber,
                new AlgorithmIdentifier("1.3.14.3.2.26"),// SHA1 OID
                null, new AlgorithmIdentifier("1.2.840.10040.4.1"),// DSA OID
                new byte[] { 0x01 },// signature
                null);

        byte[] encoding = SignerInfo.ASN1.encode(signerInfo);

        signerInfo = (SignerInfo) SignerInfo.ASN1.decode(encoding);

        assertEquals(new X500Principal("CN=test"), signerInfo.getIssuer());
        assertEquals(new BigInteger("10"), signerInfo.getSerialNumber());
    }
}

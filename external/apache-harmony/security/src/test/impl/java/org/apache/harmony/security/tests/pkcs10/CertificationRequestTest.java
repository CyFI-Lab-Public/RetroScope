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

package org.apache.harmony.security.tests.pkcs10;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.harmony.security.pkcs10.CertificationRequest;
import org.apache.harmony.security.pkcs10.CertificationRequestInfo;
import org.apache.harmony.security.x501.AttributeTypeAndValue;
import org.apache.harmony.security.x501.AttributeValue;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;



public class CertificationRequestTest extends TestCase {

    /**
     * Test method for 'com.openintel.drl.security.pkcs10.CertificationRequest'.
     * Creates CertificationRequest, gets its values, encodes and decodes the
     * encoded form.
     * 
     */
    public void testCertificationRequest() throws IOException {
        int version = 2;// v3
        Name subject = new Name("O=subject");
        SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(
                new AlgorithmIdentifier("1.2.840.113549.1.1.2"), new byte[4]);
        List attributes = new ArrayList();
        // 1.2.840.113549.1.9.1 is OID of EMAILADDRESS
        attributes.add(new AttributeTypeAndValue("1.2.840.113549.1.9.1",
                new AttributeValue("a@b.com", false)));
        CertificationRequestInfo certReqInfo = new CertificationRequestInfo(
                version, subject, spki, attributes);
        AlgorithmIdentifier signatureAlgId = new AlgorithmIdentifier(
                "1.2.3.44.555");
        byte[] signature = { (byte) 0x01, (byte) 0x02, (byte) 0x03,
                (byte) 0x04, (byte) 0x05 };

        CertificationRequest certReq = new CertificationRequest(certReqInfo,
                signatureAlgId, signature);

        // check what we have constructed
        assertEquals(certReqInfo, certReq.getInfo());
        assertEquals(signatureAlgId, certReq.getAlgId());
        assertTrue(Arrays.equals(signature, certReq.getSignature()));

        // decode the encoded CSR 
        byte[] encoding = certReq.getEncoded();
        CertificationRequest decoded = (CertificationRequest) CertificationRequest.ASN1
                .decode(encoding);

        // check what was decoded
        CertificationRequestInfo decodedCRinfo = certReq.getInfo();
        
        assertEquals(certReqInfo.getSubject(), decodedCRinfo.getSubject());
        assertEquals(certReqInfo.getSubjectPublicKeyInfo(), decodedCRinfo
                .getSubjectPublicKeyInfo());
        assertEquals(certReqInfo.getVersion(), decodedCRinfo.getVersion());
        assertEquals(certReqInfo.getAttributes(), decodedCRinfo.getAttributes());
        
        assertEquals(certReq.getAlgId(), decoded.getAlgId());
        assertTrue(Arrays.equals(certReq.getSignature(), decoded.getSignature()));
    }
}


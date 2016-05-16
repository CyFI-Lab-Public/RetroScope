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

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;

import org.apache.harmony.security.pkcs10.CertificationRequestInfo;
import org.apache.harmony.security.x501.AttributeTypeAndValue;
import org.apache.harmony.security.x501.AttributeValue;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;


public class CertificationRequestInfoTest extends TestCase {

    /**
     * Test method for CertificationRequestInfo. Creates
     * CertificationRequestInfo, gets its values, encodes and decodes the
     * encoded form.
     * 
     */
    public void testCertificationRequestInfo() throws IOException {
        int version = 2;// X.509 v3
        Name subject = new Name("O=subject");
        SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(
                new AlgorithmIdentifier("1.2.840.113549.1.1.2"), new byte[4]);
        List attributes = new ArrayList();
        // 1.2.840.113549.1.9.1 is OID of EMAILADDRESS
        attributes.add(new AttributeTypeAndValue("1.2.840.113549.1.9.1",
                new AttributeValue("a@b.com", false)));

        CertificationRequestInfo certReqInfo = new CertificationRequestInfo(
                version, subject, spki, attributes);

        // check what we have constructed
        assertEquals(version, certReqInfo.getVersion());
        assertEquals(subject.getName(X500Principal.RFC1779), certReqInfo
                .getSubject().getName(X500Principal.RFC1779));
        assertTrue(Arrays.equals(spki.getEncoded(), certReqInfo
                .getSubjectPublicKeyInfo().getEncoded()));
        assertEquals(attributes, certReqInfo.getAttributes());

        // decode the encoded CertificationRequestInfo 
        byte[] encoding = certReqInfo.getEncoded();
        CertificationRequestInfo decoded = 
                (CertificationRequestInfo) CertificationRequestInfo.ASN1
                        .decode(encoding);

        // check what was decoded
        assertEquals(certReqInfo.getVersion(), decoded.getVersion());
        assertEquals(certReqInfo.getSubject().getName(X500Principal.CANONICAL),
                decoded.getSubject().getName(X500Principal.CANONICAL));
        assertTrue(Arrays.equals(certReqInfo.getSubjectPublicKeyInfo()
                .getEncoded(), decoded.getSubjectPublicKeyInfo().getEncoded()));
        
        AttributeTypeAndValue certReqInfoATaV = (AttributeTypeAndValue) certReqInfo
                .getAttributes().get(0); 
        AttributeTypeAndValue decodedATaV = (AttributeTypeAndValue) decoded
                .getAttributes().get(0);
        assertEquals(certReqInfoATaV.getType(), decodedATaV.getType());
    }
}


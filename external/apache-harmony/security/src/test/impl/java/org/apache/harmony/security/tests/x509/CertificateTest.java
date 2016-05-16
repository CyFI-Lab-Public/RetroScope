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
* @author Alexander Y. Kleymenov
*/

package org.apache.harmony.security.tests.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.Certificate;
import org.apache.harmony.security.x509.EDIPartyName;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.NameConstraints;
import org.apache.harmony.security.x509.ORAddress;
import org.apache.harmony.security.x509.OtherName;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;
import org.apache.harmony.security.x509.TBSCertificate;
import org.apache.harmony.security.x509.Validity;

/**
 * Testing the encoding/decoding work of the following structure:
 * (as specified in RFC 3280 -
 *  Internet X.509 Public Key Infrastructure.
 *  Certificate and Certificate Revocation List (CRL) Profile.
 *  http://www.ietf.org/rfc/rfc3280.txt):
 *
 * <pre>
 *   Certificate  ::=  SEQUENCE  {
 *        tbsCertificate       TBSCertificate,
 *        signatureAlgorithm   AlgorithmIdentifier,
 *        signatureValue       BIT STRING
 *   }
 *
 *   TBSCertificate  ::=  SEQUENCE  {
 *        version         [0]  EXPLICIT Version DEFAULT v1,
 *        serialNumber         CertificateSerialNumber,
 *        signature            AlgorithmIdentifier,
 *        issuer               Name,
 *        validity             Validity,
 *        subject              Name,
 *        subjectPublicKeyInfo SubjectPublicKeyInfo,
 *        issuerUniqueID  [1]  IMPLICIT UniqueIdentifier OPTIONAL,
 *                             -- If present, version MUST be v2 or v3
 *        subjectUniqueID [2]  IMPLICIT UniqueIdentifier OPTIONAL,
 *                             -- If present, version MUST be v2 or v3
 *        extensions      [3]  EXPLICIT Extensions OPTIONAL
 *                             -- If present, version MUST be v3
 *   }
 *
 *   Version  ::=  INTEGER  {  v1(0), v2(1), v3(2)  }
 *
 *   CertificateSerialNumber  ::=  INTEGER
 *
 *   Validity ::= SEQUENCE {
 *        notBefore      Time,
 *        notAfter       Time
 *   }
 *
 *   Time ::= CHOICE {
 *        utcTime        UTCTime,
 *        generalTime    GeneralizedTime
 *   }
 *
 *   UniqueIdentifier  ::=  BIT STRING
 *
 *   SubjectPublicKeyInfo  ::=  SEQUENCE  {
 *        algorithm            AlgorithmIdentifier,
 *        subjectPublicKey     BIT STRING
 *   }
 *
 *   Extensions  ::=  SEQUENCE SIZE (1..MAX) OF Extension
 *
 *   Extension  ::=  SEQUENCE  {
 *        extnID      OBJECT IDENTIFIER,
 *        critical    BOOLEAN DEFAULT FALSE,
 *        extnValue   OCTET STRING
 *   }
 * </pre>
 */

public class CertificateTest extends TestCase {

    /**
     * Certificate(TBSCertificate tbsCertificate, AlgorithmIdentifier
     * signatureAlgorithm, byte[] signatureValue) method testing.
     * Makes the certificate, gets its encoded form, makes new certificate
     * from this encoded form by CertificateFactory, and decodes encoded
     * form.
     */
    public void testCertificate() throws Exception {
        // make the TBSCertificate for Certificate
        int version = 2; //v3
        BigInteger serialNumber = BigInteger.valueOf(555L);
        AlgorithmIdentifier signature = new AlgorithmIdentifier("1.2.3.44.555"); // random value
        Name issuer = new Name("O=Certificate Issuer");
        Validity validity = new Validity(new Date(100000000), new Date(200000000));
        Name subject = new Name("O=Subject Organization");
        SubjectPublicKeyInfo subjectPublicKeyInfo =
            new SubjectPublicKeyInfo(new AlgorithmIdentifier("1.2.840.113549.1.1.2"),
                    new byte[10]);
        boolean[]   issuerUniqueID  = new boolean[]
                    {true, false, true, false, true, false, true, false}; // random value
        boolean[]   subjectUniqueID = new boolean[]
                    {false, true, false, true, false, true, false, true}; // random value
        // make the Extensions for TBSCertificate
        // Subject Alternative Names
        GeneralName[] san = new GeneralName[] {
            new GeneralName(
                new OtherName("1.2.3.4.5",
                        ASN1Integer.getInstance().encode(
                                BigInteger.valueOf(55L).toByteArray()))),
            new GeneralName(1, "rfc@822.Name"),
            new GeneralName(2, "dNSName"),
            new GeneralName(new ORAddress()),
            new GeneralName(4, "O=Organization"),
            new GeneralName(new EDIPartyName("assigner","party")),
            new GeneralName(6, "http://Resource.Id"),
            new GeneralName(new byte[] {1, 1, 1, 1}),
            new GeneralName(8, "1.2.3.4444.55555")
        };
        GeneralNames sans = new GeneralNames(Arrays.asList(san));
        Extension extension = new Extension("2.5.29.17", true, sans.getEncoded());
        Extensions extensions = new Extensions();
        extensions.addExtension(extension);

        byte[] encoding = extensions.getEncoded();
        Extensions.ASN1.decode(encoding);

        TBSCertificate tbsCertificate = new TBSCertificate(version, serialNumber,
                signature, issuer, validity, subject, subjectPublicKeyInfo,
                issuerUniqueID, subjectUniqueID, extensions);

        encoding = tbsCertificate.getEncoded();
        TBSCertificate.ASN1.decode(encoding);

        Certificate certificate = new Certificate(tbsCertificate, signature, new byte[10]);

        encoding = certificate.getEncoded();

        Certificate.ASN1.decode(encoding);

        encoding = Certificate.ASN1.encode(certificate);

        ByteArrayInputStream bais = new ByteArrayInputStream(encoding);

        //try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cf.generateCertificate(bais);
        //} catch (CertificateException e) {
            // there is no X.509 certificate factory implementation installed
        //}
    }

    /**
     * getTbsCertificate() method testing.
     */
    public void testGetTbsCertificate() throws IOException {
        // manually derived data:
        byte[] encoding = new byte[] {
            (byte)0x30,(byte)0x13, // NameConstraints
                (byte)0xa1,(byte)0x11, // GeneralSubtrees (excludedSubtrees)
                    (byte)0x30,(byte)0x0f, // GeneralSubtree
                        (byte)0xa0,(byte)0x0a, // GeneralName
                            // OtherName:
                            (byte)0x06,(byte)0x03, // type-id (OID)
                                (byte)0x00,(byte)0x01,(byte)0x02, // oid
                            (byte)0xA0,(byte)0x03, // value (raw)
                                1, 1, (byte)0xff,  // boolean
                        (byte)0x80, (byte)0x01, (byte)0x00 // minimum
        };
        NameConstraints.ASN1.decode(encoding);
    }

    /**
     * getSignatureAlgorithm() method testing.
     */
    public void testGetSignatureAlgorithm() {
    }

    /**
     * getSignatureValue() method testing.
     */
    public void testGetSignatureValue() {
    }

    /**
     * getValue() method testing.
     */
    public void testGetValue() {
    }

    public static Test suite() {
        return new TestSuite(CertificateTest.class);
    }

}

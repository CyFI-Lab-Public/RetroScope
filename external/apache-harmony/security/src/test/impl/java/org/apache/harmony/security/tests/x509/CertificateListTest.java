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
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.harmony.security.asn1.ASN1GeneralizedTime;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.CertificateList;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.TBSCertList;

/**
 * CertificateListTest
 */
public class CertificateListTest extends TestCase {

    // OID was taken from http://oid.elibel.tm.fr
    private static String algOID          = "1.2.840.10040.4.3";
    //private static String algName         = "SHA1withDSA";
    private static byte[] algParams       = {1, 1, 0}; // DER boolean false encoding
    private static AlgorithmIdentifier signature;
    private static byte[] signatureValue = new byte[10];
    static {
        signature = new AlgorithmIdentifier(algOID, algParams);
    }
    private static String issuerName      = "O=Certificate Issuer";
    private static Date thisUpdate = new Date();
    private static Date nextUpdate;
    static {
        nextUpdate = new Date(thisUpdate.getTime()+100000);
    }
    private static Extension crlEntryExtension;
    static {
        // Invalidity Date Extension (rfc 3280)
        crlEntryExtension = new Extension("2.5.29.24",
                    ASN1GeneralizedTime.getInstance().encode(new Date()));
    }
    private static Extensions crlEntryExtensions = new Extensions();
    static {
        //*
        crlEntryExtensions.addExtension(crlEntryExtension);
        // add the Certificate Issuer Extension to check if implementation
        // support indirect CRLs. As says rfc 3280 (p.62):
        // "If used by conforming CRL issuers, this extension MUST always be
        // critical. If an implementation ignored this extension it could not
        // correctly attribute CRL entries to certificates. This specification
        // RECOMMENDS that implementations recognize this extension."
        try {
            crlEntryExtensions.addExtension(
                    new Extension("2.5.29.29", true,
                        //*
                        //ASN1OctetString.getInstance().encode(
                            GeneralNames.ASN1.encode(
                                new GeneralNames(Arrays.asList(
                                    new GeneralName[] {
                                        new GeneralName(new Name("O=Cert Organization"))//new GeneralName(4, "O=Organization")
                                    })
                                )
                            )
                        //)
                        //*/
                    )
                );
        } catch (Exception e) {
            e.printStackTrace();
        }
        //*/
    }
    private static Date revocationDate = new Date();
    private static List revokedCertificates = Arrays.asList(
            new TBSCertList.RevokedCertificate[] {
                new TBSCertList.RevokedCertificate(BigInteger.valueOf(555),
                    revocationDate, null),//crlEntryExtensions),
                new TBSCertList.RevokedCertificate(BigInteger.valueOf(666),
                    revocationDate, crlEntryExtensions),
                new TBSCertList.RevokedCertificate(BigInteger.valueOf(777),
                    revocationDate, null),//crlEntryExtensions)
            });
    private static Extensions crlExtensions = new Extensions(
        Arrays.asList(new Extension[] {
            new Extension("2.5.29.20", // CRL Number Extension (rfc 3280)
                    ASN1Integer.getInstance().encode(
                        BigInteger.valueOf(4444).toByteArray())),
        }));

    private CertificateList certificateList;
    private TBSCertList tbscertlist;
    private byte[] encoding;

    protected void setUp() throws java.lang.Exception {
        try {
            Name issuer = new Name(issuerName);

            tbscertlist =
                new TBSCertList(2, signature, issuer, thisUpdate,
                    nextUpdate, revokedCertificates, crlExtensions);

            certificateList =
                new CertificateList(tbscertlist, signature, signatureValue);

            encoding = CertificateList.ASN1.encode(certificateList);

            certificateList = (CertificateList)
                CertificateList.ASN1.decode(encoding);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown: "+e.getMessage());
        }
    }


    /**
     * CertificateList(TBSCertList tbsCertList, AlgorithmIdentifier
     * signatureAlgorithm, byte[] signatureValue) method testing.
     */
    public void testCertificateList() {
        try {
            AlgorithmIdentifier signature =
                new AlgorithmIdentifier(algOID, algParams);
            Name issuer = new Name(issuerName);
            TBSCertList tbscl =
                new TBSCertList(signature, issuer, thisUpdate);
            CertificateList cl =
                new CertificateList(tbscl, signature, new byte[] {0});

            byte[] encoding = CertificateList.ASN1.encode(cl);
            CertificateList.ASN1.decode(encoding);

            tbscl = new TBSCertList(2, signature, issuer, thisUpdate,
                    nextUpdate, revokedCertificates, crlExtensions);

            cl = new CertificateList(tbscl, signature, new byte[] {0});

            encoding = CertificateList.ASN1.encode(cl);
            CertificateList.ASN1.decode(encoding);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown: "+e.getMessage());
        }
    }

    /**
     * getTbsCertList() method testing.
     */
    public void testGetTbsCertList() {
        assertTrue("Returned tbsCertList value is incorrect",
                tbscertlist.equals(certificateList.getTbsCertList()));
    }

    /**
     * getSignatureAlgorithm() method testing.
     */
    public void testGetSignatureAlgorithm() {
        assertTrue("Returned signatureAlgorithm value is incorrect",
                signature.equals(certificateList.getSignatureAlgorithm()));
    }

    /**
     * getSignatureValue() method testing.
     */
    public void testGetSignatureValue() {
        assertTrue("Returned signatureAlgorithm value is incorrect",
                Arrays.equals(signatureValue, certificateList.getSignatureValue()));
    }

    public void testSupportIndirectCRLs() throws Exception {
        X509CRL crl = (X509CRL)
            CertificateFactory.getInstance("X.509").generateCRL(
                    new ByteArrayInputStream(encoding));
        Set rcerts = crl.getRevokedCertificates();
        System.out.println(">> rcerts:"+rcerts);

        System.out.println("}>> "+ rcerts.toArray()[0]);
        System.out.println("}>> "+((X509CRLEntry) rcerts.toArray()[0]).getCertificateIssuer());
        System.out.println("}>> "+((X509CRLEntry) rcerts.toArray()[1]).getCertificateIssuer());
        System.out.println("}>> "+((X509CRLEntry) rcerts.toArray()[2]).getCertificateIssuer());
        System.out.println(">> "+crl.getRevokedCertificate(
                    BigInteger.valueOf(555)).getCertificateIssuer());
    }

    public static Test suite() {
        return new TestSuite(CertificateListTest.class);
    }

}

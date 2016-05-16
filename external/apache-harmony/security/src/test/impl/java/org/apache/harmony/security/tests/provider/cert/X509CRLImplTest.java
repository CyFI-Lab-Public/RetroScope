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

package org.apache.harmony.security.tests.provider.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CRLException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.harmony.security.provider.cert.X509CRLImpl;
import org.apache.harmony.security.provider.cert.X509CertImpl;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.AuthorityKeyIdentifier;
import org.apache.harmony.security.x509.CRLNumber;
import org.apache.harmony.security.x509.Certificate;
import org.apache.harmony.security.x509.CertificateIssuer;
import org.apache.harmony.security.x509.CertificateList;
import org.apache.harmony.security.x509.DistributionPointName;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.InvalidityDate;
import org.apache.harmony.security.x509.IssuingDistributionPoint;
import org.apache.harmony.security.x509.ReasonCode;
import org.apache.harmony.security.x509.ReasonFlags;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;
import org.apache.harmony.security.x509.TBSCertList;
import org.apache.harmony.security.x509.TBSCertificate;
import org.apache.harmony.security.x509.Validity;

/**
 * X509CRLImplTest
 */
public class X509CRLImplTest extends TestCase {

    // Algorithm name and its OID (http://oid.elibel.tm.fr)
    private static String algOID          = "1.2.840.10040.4.3";
    private static String algName         = "SHA1withDSA";
    // DER boolean false encoding (http://asn1.elibel.tm.fr)
    // Makes no sense. For testing purposes we need just provide
    // some ASN.1 structure:
    private static byte[] algParams       = {1, 1, 0};
    private static AlgorithmIdentifier signature;
    private static byte[] signatureValue;
    static {
        signature = new AlgorithmIdentifier(algOID, algParams);
    }
    private static String issuerName      = "O=CRL Issuer";
    private static String certIssuerName      = "O=Certificate Issuer";

    private static BigInteger certSerialNumber1 = BigInteger.valueOf(555);
    private static BigInteger certSerialNumber2 = BigInteger.valueOf(567);
    private static BigInteger certSerialNumber3 = BigInteger.valueOf(777);

    private static Date thisUpdate = new Date();
    private static Date nextUpdate;
    static {
        nextUpdate = new Date(thisUpdate.getTime()+100000);
    }
    private static Extensions crlEntryExtensions = new Extensions();
    static {
        // Reason Code
        crlEntryExtensions.addExtension(
                new Extension("2.5.29.21", Extension.NON_CRITICAL,
                    new ReasonCode(ReasonCode.KEY_COMPROMISE)));
        // Invalidity Date Extension
        crlEntryExtensions.addExtension(
                new Extension("2.5.29.24", Extension.NON_CRITICAL,
                    new InvalidityDate(new Date())));
        // add the Certificate Issuer Extension to check if implementation
        // support indirect CRLs. As says rfc 3280 (p.62):
        // "If used by conforming CRL issuers, this extension MUST always be
        // critical. If an implementation ignored this extension it could not
        // correctly attribute CRL entries to certificates. This specification
        // RECOMMENDS that implementations recognize this extension."
        try {
            crlEntryExtensions.addExtension(
                    new Extension("2.5.29.29", true,
                        new CertificateIssuer(
                            new GeneralName(new Name(certIssuerName))
                            ))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static Date revocationDate = new Date();
    private static List revokedCertificates = Arrays.asList(
            new TBSCertList.RevokedCertificate[] {
                new TBSCertList.RevokedCertificate(certSerialNumber1,
                    revocationDate, null),
                // the item for certificate issued by other authority
                new TBSCertList.RevokedCertificate(certSerialNumber2,
                    revocationDate, crlEntryExtensions),
                new TBSCertList.RevokedCertificate(certSerialNumber3,
                    revocationDate, null),
            });
    private static Extensions crlExtensions;
    static {
        try {
            crlExtensions = new Extensions(
                Arrays.asList(new Extension[] {
                    // CRL Number Extension
                    new Extension("2.5.29.20", Extension.NON_CRITICAL,
                        new CRLNumber(BigInteger.valueOf(4444))),
                    // Authority Key Identifier
                    new Extension("2.5.29.35", false,
                        new AuthorityKeyIdentifier(
                            // keyIdentifier (random value)
                            new byte[] {1, 2, 3, 4, 5},
                            // authorityCertIssuer
                            new GeneralNames(
                                Arrays.asList(new GeneralName[] {
                                    new GeneralName(new Name(certIssuerName))
                            })),
                            // authorityCertSerialNumber
                            certSerialNumber2)),
                    // Issuing Distribution Point
                    new Extension("2.5.29.28", Extension.CRITICAL,
                        new IssuingDistributionPoint(
                            new DistributionPointName(new GeneralNames(
                                Arrays.asList(new GeneralName[] {
                                    new GeneralName(1, "rfc@822.Name"),
                                    new GeneralName(2, "dNSName"),
                                    new GeneralName(4, "O=Organization"),
                                    new GeneralName(6, "http://uniform.Resource.Id"),
                                    new GeneralName(7, "255.255.255.0"),
                                    new GeneralName(8, "1.2.3.4444.55555")
                                }))),
                            new ReasonFlags(new boolean[] {true, true, false, false, true, true})
                            )),
                }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // keys are using to make signature and to verify it
    private static PublicKey   publicKey;
    private static PrivateKey  privateKey;
    private static byte[] signatureValueBytes;
    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.genKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static X509CRLImpl crl;
    private static CertificateList certificateList;
    private static TBSCertList tbscertlist;
    private static byte[] encoding;
    private static byte[] tbsEncoding;
    static CertificateFactory factory;

    static {
        try {
            Name issuer = new Name(issuerName);

            tbscertlist =
                new TBSCertList(2, signature, issuer, thisUpdate,
                    nextUpdate, revokedCertificates, crlExtensions);

            tbsEncoding = tbscertlist.getEncoded();

            try {
                Signature sig= Signature.getInstance("DSA");
                sig.initSign(privateKey);
                sig.update(tbsEncoding, 0, tbsEncoding.length);
                signatureValueBytes = sig.sign();
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unexpected exception was thrown: "+e.getMessage());
            }
            factory = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception was thrown: "+e.getMessage());
        }
    }

    ByteArrayInputStream stream;

    protected void setUp() throws java.lang.Exception {
        if ("testVerify3".equals(getName())) {
            signatureValue = new byte[signatureValueBytes.length];
            // make incorrect signature value:
            System.arraycopy(signatureValueBytes, 0,
                    signatureValue, 0, signatureValueBytes.length);
            signatureValue[20]++;
        } else {
            signatureValue = signatureValueBytes;
        }
        certificateList =
            new CertificateList(tbscertlist, signature, signatureValue);

        encoding = CertificateList.ASN1.encode(certificateList);
        stream = new ByteArrayInputStream(encoding);

        crl = new X509CRLImpl(certificateList);
    }

    private static int XXX = 0, counter = 0;

    public void testCreationCRL() throws Exception {
        byte[] stamp = new byte[10];
        if ((++counter)%10 != 0) {
            XXX++;
        }
        byte tmp[] = BigInteger.valueOf(XXX).toByteArray();
        System.arraycopy(tmp, 0, stamp, 0, tmp.length);
        System.arraycopy(stamp, 0, encoding,
                encoding.length-stamp.length, stamp.length);

        stream.reset();
        java.security.cert.X509CRL c = (java.security.cert.X509CRL)
            factory.generateCRL(stream);

        if (counter == 1) {
            System.out.println("\nUSING: "+ c.getClass());
        }

        byte[] enc = c.getEncoded();
        byte[] stamp_chek = new byte[stamp.length];

        System.arraycopy(enc, enc.length - stamp.length,
                stamp_chek, 0, stamp.length);

        if (!Arrays.equals(stamp, stamp_chek)) {
            fail("Wrong encoding received.");
        }
    }

    /**
     * X509CRLImpl(CertificateList crl) method testing.
     * Tested during setup.
     */
    public void testX509CRLImpl() {
        try {
            new X509CRLImpl((CertificateList)
                    CertificateList.ASN1.decode(encoding));
        } catch (IOException e) {
            fail("Unexpected exception was thrown");
        }
    }

    /**
     * getEncoded() method testing.
     */
    public void testGetEncoded() {
        try {
            if (!Arrays.equals(encoding, crl.getEncoded())) {
                fail("Incorrect encoding of CRL.");
            }
        } catch (CRLException e) {
            fail("Unexpected CRLException was thrown.");
        }
    }

    /**
     * getVersion() method testing.
     */
    public void testGetVersion() {
        assertEquals("Incorrect version value", 2, crl.getVersion());
    }

    /**
     * getIssuerDN() method testing.
     */
    public void testGetIssuerDN() {
        assertEquals("Incorrect issuer value",
                new X500Principal(issuerName), crl.getIssuerDN());
    }

    /**
     * getIssuerX500Principal() method testing.
     */
    public void testGetIssuerX500Principal() {
        assertEquals("Incorrect issuer value",
                new X500Principal(issuerName), crl.getIssuerDN());
    }

    /**
     * getThisUpdate() method testing.
     */
    public void testGetThisUpdate() {
        assertTrue("Incorrect thisUpdate value",
                thisUpdate.getTime()/1000 == crl.getThisUpdate().getTime()/1000);
    }

    /**
     * getNextUpdate() method testing.
     */
    public void testGetNextUpdate() {
        assertTrue("Incorrect nextUpdate value",
                nextUpdate.getTime()/1000 == crl.getNextUpdate().getTime()/1000);
    }

    /**
     * getRevokedCertificate(X509Certificate certificate) method testing.
     */
    public void testGetRevokedCertificate1() {
        try {
            X509CertImpl cert1 = new X509CertImpl(
                new Certificate(
                    new TBSCertificate(2, certSerialNumber1, signature,
                        new Name(certIssuerName),
                        new Validity(new Date(), new Date()),
                    new Name(certIssuerName),
                    new SubjectPublicKeyInfo(signature, new byte[10]),
                    null, null, null),
                signature, new byte[10]));
            X509CertImpl cert2 = new X509CertImpl(
                new Certificate(
                    new TBSCertificate(2, certSerialNumber2, signature,
                        new Name(certIssuerName),
                        new Validity(new Date(), new Date()),
                    new Name(certIssuerName),
                    new SubjectPublicKeyInfo(signature, new byte[10]),
                    null, null, null),
                signature, new byte[10]));
            X509CertImpl cert3 = new X509CertImpl(
                new Certificate(
                    new TBSCertificate(2, certSerialNumber3, signature,
                        new Name("O=Another Cert Issuer"),
                        new Validity(new Date(), new Date()),
                    new Name(certIssuerName),
                    new SubjectPublicKeyInfo(signature, new byte[10]),
                    null, null, null),
                signature, new byte[10]));
            assertNull("Certificate should not be presented in CRL "
                    + "because issuer is not the same as CRL issuer",
                    crl.getRevokedCertificate(cert1));
            assertNotNull("Certificate should be presented in CRL",
                    crl.getRevokedCertificate(cert2));
            assertNull("Certificate should not be presented in CRL "
                    + "because issuer is not the same as CRL issuer",
                    crl.getRevokedCertificate(cert3));
        } catch (IOException e) {
            // should never happen;
            e.printStackTrace();
            fail("Unexpected IOException was thrown:"+e.getMessage());
        }
    }

    /**
     * getRevokedCertificate(BigInteger serialNumber) method testing.
     */
    public void testGetRevokedCertificate2() {
        assertNotNull("The revoked certificate with the serial number '"
                + certSerialNumber1 + "' should be presented in CRL.",
                crl.getRevokedCertificate(certSerialNumber1));
        assertNull("The revoked certificate with the serial number '"
                + certSerialNumber2 + "' should not be presented in CRL.",
                crl.getRevokedCertificate(certSerialNumber2));
        assertNull("The revoked certificate with the serial number '"
                + certSerialNumber3 + "' should not be presented in CRL.",
                crl.getRevokedCertificate(certSerialNumber3));
    }

    /**
     * getRevokedCertificates() method testing.
     */
    public void testGetRevokedCertificates() {
        Set rcerts = crl.getRevokedCertificates();
        assertNotNull("The set should not be null", rcerts);
        assertTrue("The size of returned set is incorrect",
                rcerts.size() == revokedCertificates.size());
    }

    /**
     * getTBSCertList() method testing.
     */
    public void testGetTBSCertList() {
        try {
            assertTrue(
                "Retrieved tbsCertList encoding does not equal to expected",
                Arrays.equals(tbscertlist.getEncoded(),
                crl.getTBSCertList()));
        } catch(CRLException e) {
            e.printStackTrace();
            fail("Unexpected CRLException was thrown: "+e.getMessage());
        }
    }

    /**
     * getSignature() method testing.
     */
    public void testGetSignature() {
        if (!Arrays.equals(signatureValueBytes, crl.getSignature())) {
            fail("Incorrect Signature value.");
        }
    }

    /**
     * getSigAlgName() method testing.
     */
    public void testGetSigAlgName() {
        assertEquals("Incorrect value of signature algorithm name",
                algName, crl.getSigAlgName());
    }

    /**
     * getSigAlgOID() method testing.
     */
    public void testGetSigAlgOID() {
        assertEquals("Incorrect value of signature algorithm OID",
                algOID, crl.getSigAlgOID());
    }

    /**
     * getSigAlgParams() method testing.
     */
    public void testGetSigAlgParams() {
        if (!Arrays.equals(algParams, crl.getSigAlgParams())) {
            fail("Incorrect SigAlgParams value.");
        }
    }

    /**
     * verify(PublicKey key) method testing.
     */
    public void testVerify1() throws Exception {
        crl.verify(publicKey);
    }

    /**
     * verify(PublicKey key, String sigProvider) method testing.
     */
    public void testVerify2() throws Exception {
        crl.verify(publicKey, Signature.getInstance("SHA1withDSA")
                .getProvider().getName());
    }

    /**
     * verify(PublicKey key) method testing.
     */
    public void testVerify3() throws Exception {
        try {
            crl.verify(publicKey);
            fail("Incorrect signature successfully verified.");
        } catch (Exception e) {
        }
    }

    /**
     * isRevoked(Certificate cert) method testing.
     */
    public void testIsRevoked() {
        try {
            X509CertImpl cert1 = new X509CertImpl(
                new Certificate(
                    new TBSCertificate(2, certSerialNumber1, signature,
                        new Name(certIssuerName),
                        new Validity(new Date(), new Date()),
                    new Name(certIssuerName),
                    new SubjectPublicKeyInfo(signature, new byte[10]),
                    null, null, null),
                signature, new byte[10]));
            X509CertImpl cert2 = new X509CertImpl(
                new Certificate(
                    new TBSCertificate(2, certSerialNumber2, signature,
                        new Name(certIssuerName),
                        new Validity(new Date(), new Date()),
                    new Name(certIssuerName),
                    new SubjectPublicKeyInfo(signature, new byte[10]),
                    null, null, null),
                signature, new byte[10]));
            X509CertImpl cert3 = new X509CertImpl(
                new Certificate(
                    new TBSCertificate(2, certSerialNumber3, signature,
                        new Name("O=Another Cert Issuer"),
                        new Validity(new Date(), new Date()),
                    new Name(certIssuerName),
                    new SubjectPublicKeyInfo(signature, new byte[10]),
                    null, null, null),
                signature, new byte[10]));
            assertFalse("Certificate should not be presented in CRL "
                    + "because issuer is not the same as CRL issuer",
                    crl.isRevoked(cert1));
            assertTrue("Certificate should be presented in CRL",
                    crl.isRevoked(cert2));
            assertFalse("Certificate should not be presented in CRL "
                    + "because issuer is not the same as CRL issuer",
                    crl.isRevoked(cert3));
        } catch (IOException e) {
            // should never happen;
            e.printStackTrace();
            fail("Unexpected IOException was thrown:"+e.getMessage());
        }
    }

    /**
     * toString() method testing.
     */
    public void testToString() {
        assertNotNull("The string representation should not be null",
                crl.toString());
    }

    // the following implementations are tested in X509CertImplTest:

    /**
     * getNonCriticalExtensionOIDs() method testing.
     */
    public void testGetNonCriticalExtensionOIDs() {
        System.out.println("getNonCriticalExtensionOIDs: "
                + crl.getNonCriticalExtensionOIDs());
    }

    /**
     * getCriticalExtensionOIDs() method testing.
     */
    public void testGetCriticalExtensionOIDs() {
        System.out.println("getCriticalExtensionOIDs: "
                + crl.getCriticalExtensionOIDs());
    }

    /**
     * getExtensionValue(String oid) method testing.
     */
    public void testGetExtensionValue() throws Exception {
        assertNotNull(crl.getExtensionValue("2.5.29.20"));
        assertNull("Null value should be returned in the case of "
                + "nonexisting extension", crl.getExtensionValue("1.1.1.1"));
    }

    /**
     * hasUnsupportedCriticalExtension() method testing.
     */
    public void testHasUnsupportedCriticalExtension() {
        System.out.println("hasUnsupportedCriticalExtension: "
                + crl.hasUnsupportedCriticalExtension());
    }

    public static Test suite() {
        return new TestSuite(X509CRLImplTest.class);
    }

}

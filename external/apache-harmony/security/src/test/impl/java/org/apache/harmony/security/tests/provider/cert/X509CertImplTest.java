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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.harmony.security.asn1.ObjectIdentifier;
import org.apache.harmony.security.provider.cert.X509CertImpl;
import org.apache.harmony.security.tests.support.cert.TestUtils;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.AlternativeName;
import org.apache.harmony.security.x509.AuthorityKeyIdentifier;
import org.apache.harmony.security.x509.BasicConstraints;
import org.apache.harmony.security.x509.CRLDistributionPoints;
import org.apache.harmony.security.x509.Certificate;
import org.apache.harmony.security.x509.CertificatePolicies;
import org.apache.harmony.security.x509.DistributionPoint;
import org.apache.harmony.security.x509.DistributionPointName;
import org.apache.harmony.security.x509.ExtendedKeyUsage;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.GeneralSubtree;
import org.apache.harmony.security.x509.GeneralSubtrees;
import org.apache.harmony.security.x509.InhibitAnyPolicy;
import org.apache.harmony.security.x509.KeyUsage;
import org.apache.harmony.security.x509.NameConstraints;
import org.apache.harmony.security.x509.PolicyConstraints;
import org.apache.harmony.security.x509.PolicyInformation;
import org.apache.harmony.security.x509.ReasonFlags;
import org.apache.harmony.security.x509.SubjectKeyIdentifier;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;
import org.apache.harmony.security.x509.TBSCertificate;
import org.apache.harmony.security.x509.Validity;

/**
 * X509CertImplTest
 */
public class X509CertImplTest extends TestCase {

    //
    // The values of certificate's fields:
    //

    int         version         = 2; //v3
    BigInteger  serialNumber    = BigInteger.valueOf(555L);

    // Algorithm name and its OID (http://oid.elibel.tm.fr)
    String      algOID          = "1.2.840.10040.4.3";
    String      algName         = "SHA1withDSA";

    // DER boolean false encoding (http://asn1.elibel.tm.fr)
    // Makes no sense. For testing purposes we need just provide
    // some ASN.1 structure:
    byte[]      algParams       = {1, 1, 0};
    String      issuerName      = "O=Certificate Issuer";
    long        notBefore       = 1000000000L;
    long        notAfter        = 2000000000L;
    String      subjectName     = "O=Subject Organization";

    // keys are using to make signature and to verify it
    static PublicKey   publicKey;
    static PrivateKey  privateKey;
    byte[]      key             = new byte[] {1, 2, 3, 4, 5, 6, 7, 8}; // random value
    byte[]      keyEncoding     = null;
    boolean[]   issuerUniqueID  = new boolean[]
                {true, false, true, false, true, false, true, false}; // random value
    boolean[]   subjectUniqueID = new boolean[]
                {false, true, false, true, false, true, false, true}; // random value

    // Extensions' values
    byte[]      extValEncoding  = new byte[] {1, 1, 1}; // random value
    boolean[]   extnKeyUsage    = new boolean[]
                {true, false, true, false, true, false, true, false, true}; // random value
    List    extnExtendedKeyUsage = Arrays.asList(new int[][] {

        // Extended key usage values as specified in rfc 3280
        // (http://www.ietf.org/rfc/rfc3280.txt):
        ObjectIdentifier.toIntArray("2.5.29.37.0"),       // Any extended key usage
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.1"), // TLS Web server authentication
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.1"), // TLS Web server authentication
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.2"), // TLS Web client authentication
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.3"), // Code Signing
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.4"), // E-mail protection
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.5"), // IP security end system
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.6"), // IP security tunnel termination
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.7"), // IP security user
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.8"), // Timestamping
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.7.3.9"), // OCSP signing
        ObjectIdentifier.toIntArray("1.3.6.1.5.5.8.2.2"), // iKEIntermediate
        ObjectIdentifier.toIntArray("1.3.6.1.4.1.311.10.3.3"), // MS Server Gated Cryptography
        ObjectIdentifier.toIntArray("2.16.840.1.113730.4.1"), // Netscape Server Gated Cryptography
    });
    static NameConstraints nameConstraints;
    int extnBCLen = 5;
    static GeneralNames extnSANames;
    static GeneralNames extnIANames;

    static {
        try {
            extnSANames = new GeneralNames(
                Arrays.asList(new GeneralName[] {
                    new GeneralName(1, "rfc@822.Name"),
                    new GeneralName(2, "dNSName"),
                    new GeneralName(4, "O=Organization"),
                    new GeneralName(6, "http://uniform.Resource.Id"),
                    new GeneralName(7, "255.255.255.0"),
                    new GeneralName(8, "1.2.3.4444.55555")
            }));
            GeneralSubtrees permittedNames = new GeneralSubtrees()
                    .addSubtree(new GeneralSubtree(
                            new GeneralName(1, "rfc@822.Name"), 1, 2))
                    .addSubtree(new GeneralSubtree(
                            new GeneralName(2, "dNSName")))
                    .addSubtree(new GeneralSubtree(
                            new GeneralName(8, "1.2.3.4444.55555"), 2));
            GeneralSubtrees excludedNames = new GeneralSubtrees()
                    .addSubtree(new GeneralSubtree(
                            new GeneralName(1, "rfc@822.BadName"), 1, 2))
                    .addSubtree(new GeneralSubtree(
                            new GeneralName(2, "BadDNSName")))
                    .addSubtree(new GeneralSubtree(
                            new GeneralName(8, "2.3.4.4444.222"), 2));
            nameConstraints =
                new NameConstraints(permittedNames, excludedNames);
        } catch (IOException e) {
            // should not be thrown
            e.printStackTrace();
            extnSANames = new GeneralNames();
            nameConstraints = new NameConstraints();
        }
        extnIANames = extnSANames;

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

    // Extensions
    Extension[] extensions      = new Extension[] {

        // Supported critical extensions (as specified in rfc 3280
        // http://www.ietf.org/rfc/rfc3280.txt):
        // Key Usage
        new Extension("2.5.29.15", true, new KeyUsage(extnKeyUsage)),
        // Basic Constraints
        new Extension("2.5.29.19", true, new BasicConstraints(true, extnBCLen)),
        // Certificate Policies with ANY policy
        new Extension("2.5.29.32", true,
                new CertificatePolicies()
                .addPolicyInformation(new PolicyInformation("2.5.29.32.0"))),
        // Subject Alternative Name
        new Extension("2.5.29.17", true,
                new AlternativeName(AlternativeName.SUBJECT, extnSANames)),
        // Name Constraints
        new Extension("2.5.29.30", true, nameConstraints),
        // Policy Constraints
        new Extension("2.5.29.36", true, new PolicyConstraints(1, 2)),
        // Extended Key Usage
        new Extension("2.5.29.37", true, new ExtendedKeyUsage(extnExtendedKeyUsage)),
        // Inhibit Any-Policy
        new Extension("2.5.29.54", true, new InhibitAnyPolicy(1)),


        // Unsupported critical extensions:
        new Extension("1.2.77.777", true, extValEncoding),


        // Non-critical extensions (as specified in rfc 3280
        // http://www.ietf.org/rfc/rfc3280.txt):

        // Issuer Alternative Name
        new Extension("2.5.29.18", false,
                new AlternativeName(AlternativeName.ISSUER, extnSANames)),
        // CRL Distribution Points
        new Extension("2.5.29.31", false,
                new CRLDistributionPoints(Arrays.asList(new DistributionPoint[] {
                    new DistributionPoint(
                        new DistributionPointName(extnSANames),
                        new ReasonFlags(extnKeyUsage),
                        extnSANames
                        ),
                }))),
        // Authority Key Identifier
        new Extension("2.5.29.35", false,
                new AuthorityKeyIdentifier(
                    // random value for key identifier
                    new byte[] {1, 2, 3, 4, 5}, extnSANames, serialNumber)),
        // Subject Key Identifier
        new Extension("2.5.29.14", false,
                // random value for key identifier
                new SubjectKeyIdentifier(new byte[] {1, 2, 3, 4, 5})),
        // Policy Mappings
        new Extension("2.5.29.33", false, extValEncoding),
    };
    List allCritical = Arrays.asList(new String[] {"2.5.29.15", "2.5.29.19",
        "2.5.29.32", "2.5.29.17", "2.5.29.30", "2.5.29.36", "2.5.29.37",
        "2.5.29.54", "1.2.77.777"});
    List allNonCritical = Arrays.asList(new String[] {"2.5.29.18", "2.5.29.35",
        "2.5.29.14", "2.5.29.33", "2.5.29.31"});

    public X509Certificate certificate;
    byte[] tbsCertEncoding;
    byte[] signatureValue;
    // to minimize efforts on signature generation the signature will be
    // stored in this field
    static byte[] signatureValueBytes;
    byte[] certEncoding;

    /**
     * Creates the master certificate on the base of which
     * all functionality will be tested.
     * @return
     * @throws java.lang.Exception
     */
    protected void setUp() throws java.lang.Exception {
        AlgorithmIdentifier signature =
            new AlgorithmIdentifier(algOID, algParams);
        Name issuer = new Name(issuerName);
        Name subject = new Name(subjectName);
        Validity validity =
            new Validity(new Date(notBefore), new Date(notAfter));

        SubjectPublicKeyInfo subjectPublicKeyInfo = (SubjectPublicKeyInfo)
            SubjectPublicKeyInfo.ASN1.decode(publicKey.getEncoded());
        keyEncoding = subjectPublicKeyInfo.getEncoded();

        Extensions exts = new Extensions(Arrays.asList(extensions));

        TBSCertificate tbsCertificate =
            new TBSCertificate(version, serialNumber,
                signature, issuer, validity, subject, subjectPublicKeyInfo,
                issuerUniqueID, subjectUniqueID, exts);
        tbsCertEncoding = tbsCertificate.getEncoded();

        if (signatureValueBytes == null) {
            try {
                Signature sig= Signature.getInstance("DSA");//new byte[32];
                sig.initSign(privateKey);
                sig.update(tbsCertEncoding, 0, tbsCertEncoding.length);
                signatureValueBytes = sig.sign();
            } catch (Exception e) {
                e.printStackTrace();
                signatureValueBytes = new byte[10];
            }
        }
        if ("testVerify3".equals(getName())) {
            signatureValue = new byte[signatureValueBytes.length];
            // make incorrect signature value:
            System.arraycopy(signatureValueBytes, 0,
                    signatureValue, 0, signatureValueBytes.length);
            signatureValue[20]++;
        } else {
            signatureValue = signatureValueBytes;
        }

        Certificate cert =
            new Certificate(tbsCertificate, signature, signatureValue);

        certEncoding = cert.getEncoded();

        cert = (Certificate) Certificate.ASN1.decode(certEncoding);
        certificate = new X509CertImpl(cert);
    }

    public void testCreation() throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(certEncoding);
        certificate = new X509CertImpl(bis);
    }

    /**
     * checkValidity() method testing.
     */
    public void testCheckValidity1() {
        try {
            certificate.checkValidity();
            fail("CertificateExpiredException should be thrown.");
        } catch (CertificateNotYetValidException e) {
            fail("Unexpected CertificateNotYetValidException was thrown.");
        } catch (CertificateExpiredException e) {
        }
    }

    /**
     * checkValidity(Date date) method testing.
     */
    public void testCheckValidity2() {
        try {
            certificate.checkValidity(new Date(3000000000L));
            fail("CertificateExpiredException should be thrown.");
        } catch (CertificateNotYetValidException e) {
            fail("Unexpected CertificateNotYetValidException was thrown.");
        } catch (CertificateExpiredException e) {
        }
        try {
            certificate.checkValidity(new Date(100000000L));
            fail("CertificateNotYetValidException be thrown.");
        } catch (CertificateExpiredException e) {
            fail("Unexpected CertificateExpiredException was thrown.");
        } catch (CertificateNotYetValidException e) {
        }
        try {
            certificate.checkValidity(new Date(1000000000L));
            certificate.checkValidity(new Date(1500000000L));
            certificate.checkValidity(new Date(2000000000L));
        } catch (CertificateExpiredException e) {
            fail("Unexpected CertificateExpiredException was thrown.");
        } catch (CertificateNotYetValidException e) {
            fail("Unexpected CertificateNotYetValidException was thrown.");
        }
    }

    /**
     * @tests java.security.cert.X509Certificate#getVersion()
     */
    public void testGetVersion() throws IOException {

        byte[] x509CertEnc = TestUtils.getX509Certificate_v1();

        // test initial version that is 1
        assertEquals("Version 1:", 1, new X509CertImpl(x509CertEnc)
                .getVersion());

        // as fas as we know offset of version byte in the cert. encoding
        // it is possible to change it to test other versions

        // offset of version byte in certificate encoding
        int offset = 8;

        // set version to 2
        x509CertEnc[offset] = 1;
        assertEquals("Version 2:", 2, new X509CertImpl(x509CertEnc)
                .getVersion());

        // set version to 3
        x509CertEnc[offset] = 2;
        assertEquals("Version 3:", 3, new X509CertImpl(x509CertEnc)
                .getVersion());
    }

    /**
     * getSerialNumber() method testing.
     */
    public void testGetSerialNumber() {
        assertEquals("Incorrect value of version",
                serialNumber, certificate.getSerialNumber());
    }

    /**
     * getIssuerDN() method testing.
     */
    public void testGetIssuerDN() {
        assertEquals("Incorrect issuer",
                new X500Principal(issuerName).getName(),
                certificate.getIssuerDN().getName());
    }

    /**
     * getIssuerX500Principal() method testing.
     */
    public void testGetIssuerX500Principal() {
        assertEquals("Incorrect issuer",
                new X500Principal(issuerName),
                certificate.getIssuerX500Principal());
    }

    /**
     * getSubjectDN() method testing.
     */
    public void testGetSubjectDN() {
        assertEquals("Incorrect subject",
                new X500Principal(subjectName).getName(),
                certificate.getSubjectDN().getName());
    }

    /**
     * getSubjectX500Principal() method testing.
     */
    public void testGetSubjectX500Principal() {
        assertEquals("Incorrect subject",
                new X500Principal(subjectName),
                certificate.getSubjectX500Principal());
    }

    /**
     * getNotBefore() method testing.
     */
    public void testGetNotBefore() {
        assertEquals("Incorrect notBefore date",
                new Date(notBefore), certificate.getNotBefore());
    }

    /**
     * getNotAfter() method testing.
     */
    public void testGetNotAfter() {
        assertEquals("Incorrect notAfter date",
                new Date(notAfter), certificate.getNotAfter());
    }

    public static void printAsHex(int perLine, String prefix,
                                        String delimiter, byte[] data) {
        for (int i=0; i<data.length; i++) {
            String tail = Integer.toHexString(0x000000ff & data[i]);
            if (tail.length() == 1) {
                tail = "0" + tail;
            }
            System.out.print(prefix + "0x" + tail + delimiter);

            if (((i+1)%perLine) == 0) {
                System.out.println();
            }
        }
        System.out.println();
    }

    /**
     * getTBSCertificate() method testing.
     */
    public void testGetTBSCertificate() {
        try {
            if (!Arrays.equals(
                        tbsCertEncoding, certificate.getTBSCertificate())) {
                System.out.println("TBSCertificate encoding missmatch:");
                System.out.println("Expected:");
                printAsHex(20, "", " ", tbsCertEncoding);
                System.out.println("Got:");
                printAsHex(20, "", " ", certificate.getTBSCertificate());
                fail("Incorrect encoding of TBSCertificate.");
            }
        } catch (CertificateEncodingException e) {
            fail("Unexpected CertificateEncodingException was thrown.");
        }
    }

    /**
     * getSignature() method testing.
     */
    public void testGetSignature() {
        if (!Arrays.equals(signatureValue, certificate.getSignature())) {
            fail("Incorrect Signature value.");
        }
    }

    /**
     * getSigAlgName() method testing.
     */
    public void testGetSigAlgName() {
        assertEquals("Incorrect value of signature algorithm name",
                algName, certificate.getSigAlgName());
    }

    /**
     * getSigAlgOID() method testing.
     */
    public void testGetSigAlgOID() {
        assertEquals("Incorrect value of signature algorithm OID",
                algOID, certificate.getSigAlgOID());
    }

    /**
     * getSigAlgParams() method testing.
     */
    public void testGetSigAlgParams() {
        if (!Arrays.equals(algParams, certificate.getSigAlgParams())) {
            fail("Incorrect SigAlgParams value.");
        }
    }

    /**
     * getIssuerUniqueID() method testing.
     */
    public void testGetIssuerUniqueID() {
        if (!Arrays.equals(issuerUniqueID, certificate.getIssuerUniqueID())) {
            fail("Incorrect issuerUniqueID value.");
        }
    }

    /**
     * getSubjectUniqueID() method testing.
     */
    public void testGetSubjectUniqueID() {
        if (!Arrays.equals(subjectUniqueID, certificate.getSubjectUniqueID())) {
            fail("Incorrect subjectUniqueID value.");
        }
    }

    /**
     * getKeyUsage() method testing.
     */
    public void testGetKeyUsage() {
        boolean[] ku = certificate.getKeyUsage();
        if ((ku == null) || (ku.length < extnKeyUsage.length)) {
            fail("Incorrect Key Usage value.");
        }
        for (int i=0; i<extnKeyUsage.length; i++) {
            if (extnKeyUsage[i] != ku[i]) {
                fail("Incorrect Key Usage value.");
            }
        }
    }

    /**
     * getExtendedKeyUsage() method testing.
     */
    public void testGetExtendedKeyUsage() throws Exception {
        List exku = certificate.getExtendedKeyUsage();
        if ((exku == null)
                || (exku.size() != extnExtendedKeyUsage.size())) {
            fail("Incorrect Extended Key Usage value.");
        }
        for (int i=0; i<extnExtendedKeyUsage.size(); i++) {
            String ku = ObjectIdentifier
                    .toString((int[]) extnExtendedKeyUsage.get(i));
            if (!exku.contains(ku)) {
                fail("Missing value:" + ku);
            }
        }
    }

    /**
     * getBasicConstraints() method testing.
     */
    public void testGetBasicConstraints() {
        assertEquals(extnBCLen, certificate.getBasicConstraints());
    }

    /**
     * getSubjectAlternativeNames() method testing.
     */
    public void testGetSubjectAlternativeNames() {
        try {
            Collection certSans = certificate.getSubjectAlternativeNames();
            if (certSans == null) {
                fail("Returned value should not be null.");
            }
            List sans = extnSANames.getPairsList();
            if (sans.size() != certSans.size()) {
                fail("Size of returned collection does not match to the actual");
            }
            Iterator it = certSans.iterator();
            while (it.hasNext()) {
                List extnSan = (List) it.next();
                Integer tag = (Integer) extnSan.get(0);
                for (int i=0; i< sans.size(); i++) {
                    List san = (List) sans.get(i);
                    if (tag.equals(san.get(0))) {
                        assertEquals(
                                "Incorrect value of Subject Alternative Name",
                                extnSan.get(1), san.get(1));
                    }
                }
            }
        } catch (CertificateParsingException e) {
            fail("Subject Alternative Name extension was incorrectly encoded.");
        }
    }

    /**
     * getIssuerAlternativeNames() method testing.
     */
    public void testGetIssuerAlternativeNames() {
        try {
            Collection certIans = certificate.getIssuerAlternativeNames();
            if (certIans == null) {
                fail("Returned value should not be null.");
            }
            List ians = extnSANames.getPairsList();
            if (ians.size() != certIans.size()) {
                fail("Size of returned collection does not match to the actual");
            }
            Iterator it = certIans.iterator();
            while (it.hasNext()) {
                List extnIan = (List) it.next();
                Integer tag = (Integer) extnIan.get(0);
                for (int i=0; i< ians.size(); i++) {
                    List ian = (List) ians.get(i);
                    if (tag.equals(ian.get(0))) {
                        assertEquals(
                                "Incorrect value of Issuer Alternative Name",
                                extnIan.get(1), ian.get(1));
                    }
                }
            }
        } catch (CertificateParsingException e) {
            fail("Issuer Alternative Name extension was incorrectly encoded.");
        }
    }

    /**
     * getEncoded() method testing.
     */
    public void testGetEncoded() {
        try {
            if (!Arrays.equals(certEncoding, certificate.getEncoded())) {
                fail("Incorrect encoding of Certificate.");
            }
        } catch (CertificateEncodingException e) {
            fail("Unexpected CertificateEncodingException was thrown.");
        }
    }

    /**
     * getPublicKey() method testing.
     */
    public void testGetPublicKey() {
        if (!Arrays.equals(keyEncoding, certificate.getPublicKey().getEncoded())) {
            fail("Incorrect Public Key.");
        }
    }

    /**
     * getExtensionValue(String oid) method testing.
     */
    public void testGetExtensionValue() {
        for (int i=0; i<extensions.length; i++) {
            String id = extensions[i].getExtnID();
            byte[] certExtnValue = certificate.getExtensionValue(id);
            byte[] certExtnValue2Check = extensions[i].getRawExtnValue();
                certificate.getExtensionValue(id);
            if (!Arrays.equals(certExtnValue2Check, certExtnValue)) {
                System.out.println("Extension encoding mismatch for "+id);
                System.out.println("Expected:");
                printAsHex(20, "", " ", certExtnValue2Check);
                System.out.println("But has been got:");
                if (certExtnValue == null) {
                    System.out.println("null");
                } else {
                    printAsHex(20, "", " ", certExtnValue);
                }
                fail("The values for extension "+id+" differ.");
            }
        }
        assertNull("Null value should be returned in the case of "
                + "nonexisting extension",
                certificate.getExtensionValue("1.1.1.1"));
    }

    /**
     * getCriticalExtensionOIDs() method testing.
     */
    public void testGetCriticalExtensionOIDs() {
        Set certCEOids = certificate.getCriticalExtensionOIDs();
        if (!(certCEOids.containsAll(allCritical)
                    && allCritical.containsAll(certCEOids))) {
            fail("Incorrect value of Critical Extension OIDs");
        }
    }

    /**
     * getNonCriticalExtensionOIDs() method testing.
     */
    public void testGetNonCriticalExtensionOIDs() {
        Set certNCEOids = certificate.getNonCriticalExtensionOIDs();
        if (!(certNCEOids.containsAll(allNonCritical)
                    && allNonCritical.containsAll(certNCEOids))) {
            fail("Incorrect value of Non Critical Extension OIDs");
        }
    }

    /**
     * hasUnsupportedCriticalExtension() method testing.
     */
    public void testHasUnsupportedCriticalExtension() {
        assertTrue("Incorrect value of hasUnsupportedCriticalExtension",
                certificate.hasUnsupportedCriticalExtension());

        if (!certificate.hasUnsupportedCriticalExtension()) {
            fail("Incorrect value of hasUnsupportedCriticalExtension");
        }
    }

    /**
     * toString() method testing.
     */
    public void testToString() {
        assertNotNull("String representation should not be null",
                certificate.toString());
    }

    /**
     * TODO
     * verify(PublicKey key) method testing.
     */
    public void testVerify1() throws Exception {
        certificate.verify(publicKey);
    }

    /**
     * TODO
     * verify(PublicKey key, String sigProvider) method testing.
     */
    public void testVerify2() throws Exception {
        certificate.verify(publicKey, Signature.getInstance("SHA1withDSA")
                .getProvider().getName());
    }

    /**
     * TODO
     * verify(PublicKey key) method testing.
     */
    public void testVerify3() throws Exception {
        try {
            certificate.verify(publicKey);
            fail("Incorrect signature successfully verified.");
        } catch (Exception e) {
        }
    }

    public static Test suite() {
        return new TestSuite(X509CertImplTest.class);
    }

}

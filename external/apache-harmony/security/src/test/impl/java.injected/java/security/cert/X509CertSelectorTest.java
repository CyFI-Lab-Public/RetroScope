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

package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import javax.security.auth.x500.X500Principal;

import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Oid;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;

import org.apache.harmony.security.tests.support.TestKeyPair;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.CertificatePolicies;
import org.apache.harmony.security.x509.EDIPartyName;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.GeneralSubtree;
import org.apache.harmony.security.x509.GeneralSubtrees;
import org.apache.harmony.security.x509.NameConstraints;
import org.apache.harmony.security.x509.ORAddress;
import org.apache.harmony.security.x509.OtherName;
import org.apache.harmony.security.x509.PolicyInformation;
import org.apache.harmony.security.x509.PrivateKeyUsagePeriod;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;
import org.apache.harmony.security.x509.TBSCertificate;
import org.apache.harmony.security.x509.Validity;



import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * X509CertSelectorTest
 */
public class X509CertSelectorTest extends TestCase {

    /**
     * The abstract class stub implementation.
     */
    private class TestCert extends X509Certificate {

        /* Stuff fields */
        protected String equalCriteria = null; // to simplify method equals()
        protected BigInteger serialNumber = null;
        protected X500Principal issuer = null;
        protected X500Principal subject = null;
        protected byte[] keyIdentifier = null;
        protected Date date = null;
        protected Date notBefore = null;
        protected Date notAfter = null;
        protected PublicKey key = null;
        protected boolean[] keyUsage = null;
        protected List extKeyUsage = null;
        protected int pathLen = -1;
        protected GeneralNames sans = null;
        protected byte[] encoding = null;
        protected String[] policies = null;
        protected NameConstraints nameConstraints = null;

        /* Stuff methods */
        public TestCert() {}

        public TestCert(GeneralNames sans) {
            setSubjectAlternativeNames(sans);
        }

        public TestCert(NameConstraints nameConstraints) {
            this.nameConstraints = nameConstraints;
        }

        public TestCert(String equalCriteria) {
            setEqualCriteria(equalCriteria);
        }

        public TestCert(String[] policies) {
            setPolicies(policies);
        }

        public TestCert(BigInteger serial) {
            setSerialNumber(serial);
        }

        public TestCert(X500Principal principal) {
            setIssuer(principal);
            setSubject(principal);
        }

        public TestCert(byte[] array) {
            setKeyIdentifier(array);
        }

        public TestCert(Date date) {
            setDate(date);
        }

        public TestCert(Date notBefore, Date notAfter) {
            setPeriod(notBefore, notAfter);
        }

        public TestCert(PublicKey key) {
            setPublicKey(key);
        }

        public TestCert(boolean[] keyUsage) {
            setKeyUsage(keyUsage);
        }

        public TestCert(Set extKeyUsage) {
            setExtendedKeyUsage(extKeyUsage);
        }

        public TestCert(int pathLen) {
            this.pathLen = pathLen;
        }

        public void setPolicies(String[] policies) {
            this.policies = policies;
        }

        public void setSubjectAlternativeNames(GeneralNames sans) {
            this.sans = sans;
        }

        public void setExtendedKeyUsage(Set extKeyUsage) {
            this.extKeyUsage = (extKeyUsage == null)
                                ? null
                                : new ArrayList(extKeyUsage);
        }

        public void setKeyUsage(boolean[] keyUsage) {
            this.keyUsage = (keyUsage == null) ? null
                                               : (boolean[]) keyUsage.clone();
        }

        public void setPublicKey(PublicKey key) {
            this.key = key;
        }

        public void setPeriod(Date notBefore, Date notAfter) {
            this.notBefore = notBefore;
            this.notAfter = notAfter;
        }

        public void setSerialNumber(BigInteger serial) {
            this.serialNumber = serial;
        }

        public void setEqualCriteria(String equalCriteria) {
            this.equalCriteria = equalCriteria;
        }

        public void setIssuer(X500Principal issuer) {
            this.issuer = issuer;
        }

        public void setSubject(X500Principal subject) {
            this.subject = subject;
        }

        public void setKeyIdentifier(byte[] subjectKeyID) {
            this.keyIdentifier = subjectKeyID.clone();
        }

        public void setDate(Date date) {
            this.date = new Date(date.getTime());
        }

        public void setEncoding(byte[] encoding) {
            this.encoding = encoding;
        }

        /* Method implementations */
        public boolean equals(Object cert) {
            if (cert == null) {
                return false;
            }
            if ((equalCriteria == null)
                    || (((TestCert)cert).equalCriteria == null)) {
                return false;
            } else {
                return equalCriteria.equals(((TestCert)cert).equalCriteria);
            }
        }

        public String toString() {
            if (equalCriteria != null) {
                return equalCriteria;
            }
            return "";
        }

        public void checkValidity() throws CertificateExpiredException,
                                           CertificateNotYetValidException {}

        public void checkValidity(Date date)
                                    throws CertificateExpiredException,
                                           CertificateNotYetValidException {
            if (this.date == null) {
                throw new CertificateExpiredException();
            }
            int result = this.date.compareTo(date);
            if (result > 0) {
                throw new CertificateExpiredException();
            }
            if (result < 0) {
                throw new CertificateNotYetValidException();
            }
        }

        public int getVersion() {
            return 3;
        }

        public BigInteger getSerialNumber() {
            return (serialNumber == null)
                    ? new BigInteger("1111")
                    : serialNumber;
        }

        public Principal getIssuerDN() {
            return issuer;
        }

        public X500Principal getIssuerX500Principal() {
            return issuer;
        }

        public Principal getSubjectDN() {
            return subject;
        }

        public X500Principal getSubjectX500Principal() {
            return subject;
        }

        public Date getNotBefore() {
            return null;
        }

        public Date getNotAfter() {
            return null;
        }

        public byte[] getTBSCertificate()
                            throws CertificateEncodingException
        {
            return null;
        }

        public byte[] getSignature() {
            return null;
        }

        public String getSigAlgName() {
            return null;
        }

        public String getSigAlgOID() {
            return null;
        }

        public byte[] getSigAlgParams() {
            return null;
        }

        public boolean[] getIssuerUniqueID() {
            return null;
        }

        public boolean[] getSubjectUniqueID() {
            return null;
        }

        public boolean[] getKeyUsage() {
            return keyUsage;
        }

        public List/*<String>*/ getExtendedKeyUsage()
                                    throws CertificateParsingException {
            return extKeyUsage;
        }

        public int getBasicConstraints() {
            return pathLen;
        }

        public Collection/*<List<?>>*/ getSubjectAlternativeNames()
                                    throws CertificateParsingException {
            return sans.getPairsList();
        }


        public void verify(PublicKey key)
                     throws CertificateException, NoSuchAlgorithmException,
                            InvalidKeyException, NoSuchProviderException,
                            SignatureException
        {
        }

        public void verify(PublicKey key,
                                    String sigProvider)
                     throws CertificateException, NoSuchAlgorithmException,
                            InvalidKeyException, NoSuchProviderException,
                            SignatureException
        {
        }

        public PublicKey getPublicKey() {
            return key;
        }

        public byte[] getEncoded() throws CertificateEncodingException
        {
            return encoding;
        }

        public Set getNonCriticalExtensionOIDs() {
            return null;
        }

        public Set getCriticalExtensionOIDs() {
            return null;
        }

        public byte[] getExtensionValue(String oid) {
            if (("2.5.29.14".equals(oid)) || ("2.5.29.35".equals(oid))) {
                // Extension value is represented as an OctetString
                return ASN1OctetString.getInstance().encode(keyIdentifier);
            }
            if ("2.5.29.16".equals(oid)) {
                PrivateKeyUsagePeriod pkup =
                                new PrivateKeyUsagePeriod(notBefore, notAfter);
                byte[] encoded = pkup.getEncoded();
                return ASN1OctetString.getInstance().encode(encoded);
            }
            if (("2.5.29.37".equals(oid)) && (extKeyUsage != null)) {
                ASN1Oid[] oa = new ASN1Oid[extKeyUsage.size()];
                String[] val = new String[extKeyUsage.size()];
                Iterator it = extKeyUsage.iterator();
                int id = 0;
                while (it.hasNext()) {
                    oa[id] = ASN1Oid.getInstanceForString();
                    val[id++] = (String) it.next();
                }
                return ASN1OctetString.getInstance().encode(
                    new ASN1Sequence(oa).encode(val));
            }
            if ("2.5.29.19".equals(oid)) {
                return ASN1OctetString.getInstance().encode(
                            new ASN1Sequence(
                                    new ASN1Type[] {
                                            ASN1Boolean.getInstance(),
                                            ASN1Integer.getInstance()
                                    }).encode(
                                           new Object[] {
                                                   new Boolean(pathLen != -1),
                                                   BigInteger.valueOf(pathLen).
                                                   toByteArray()
                                           })
                            );
            }
            if ("2.5.29.17".equals(oid) && (sans != null)) {
                if (sans.getNames() == null) {
                    return null;
                }
                return ASN1OctetString.getInstance().encode(
                            GeneralNames.ASN1.encode(sans));
            }
            if ("2.5.29.32".equals(oid) && (policies != null)
                                                    && (policies.length > 0)) {
                //  Certificate Policies Extension (as specified in rfc 3280)
                CertificatePolicies certificatePolicies =
                                                new CertificatePolicies();
                for (int i=0; i<policies.length; i++) {
                    PolicyInformation policyInformation =
                                            new PolicyInformation(policies[i]);
                    certificatePolicies.addPolicyInformation(policyInformation);
                }
                return ASN1OctetString.getInstance().encode(
                            certificatePolicies.getEncoded());
            }
            if ("2.5.29.30".equals(oid) && (nameConstraints != null)) {
                // Name Constraints Extension (as specified in rfc 3280)
                return ASN1OctetString.getInstance().encode(
                            nameConstraints.getEncoded());
            }
            return null;
        }

        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }
    }

    /* ********************************************************************** */
    /* ************************* Test implementation ************************ */
    /* ********************************************************************** */

    /**
     * setCertificate(X509Certificate certificate) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetCertificate() {
        TestCert cert_1 = new TestCert("same certificate");
        TestCert cert_2 = new TestCert("other certificate");
        X509CertSelector selector = new X509CertSelector();

        selector.setCertificate(null);
        assertTrue("Any certificates should match in the case of null "
                                                + "certificateEquals criteria.",
                            selector.match(cert_1) && selector.match(cert_2));
        selector.setCertificate(cert_1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setCertificate(cert_2);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getCertificate() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetCertificate() {
        TestCert cert_1 = new TestCert("same certificate");
        TestCert cert_2 = new TestCert("other certificate");
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getCertificate());
        selector.setCertificate(cert_1);
        assertEquals("The returned certificate should be equal to specified",
                                        cert_1, selector.getCertificate());
        assertFalse("The returned certificate should differ",
                                cert_2.equals(selector.getCertificate()));
    }

    /**
     * setSerialNumber(BigInteger serial) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetSerialNumber() {
        BigInteger ser1 = new BigInteger("10000");
        BigInteger ser2 = new BigInteger("10001");
        TestCert cert_1 = new TestCert(ser1);
        TestCert cert_2 = new TestCert(ser2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSerialNumber(null);
        assertTrue("Any certificate should match in the case of null "
                                                + "serialNumber criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setSerialNumber(ser1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setSerialNumber(ser2);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getSerialNumber() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetSerialNumber() {
        BigInteger ser1 = new BigInteger("10000");
        BigInteger ser2 = new BigInteger("10001");
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getSerialNumber());
        selector.setSerialNumber(ser1);
        assertEquals("The returned serial number should be equal to specified",
                                        ser1, selector.getSerialNumber());
        assertFalse("The returned serial number should differ",
                                    ser2.equals(selector.getSerialNumber()));
    }

    /**
     * setIssuer(X500Principal issuer) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetIssuer1() {
        X500Principal iss1 = new X500Principal("O=First Org.");
        X500Principal iss2 = new X500Principal("O=Second Org.");
        TestCert cert_1 = new TestCert(iss1);
        TestCert cert_2 = new TestCert(iss2);
        X509CertSelector selector = new X509CertSelector();

        selector.setIssuer((X500Principal) null);
        assertTrue("Any certificates should match "
                                + "in the case of null issuer criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setIssuer(iss1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setIssuer(iss2);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getIssuer() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetIssuer() {
        X500Principal iss1 = new X500Principal("O=First Org.");
        X500Principal iss2 = new X500Principal("O=Second Org.");
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getIssuer());
        selector.setIssuer(iss1);
        assertEquals("The returned issuer should be equal to specified",
                                        iss1, selector.getIssuer());
        assertFalse("The returned issuer should differ",
                                        iss2.equals(selector.getIssuer()));
    }

    /**
     * setIssuer(String issuerDN) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetIssuer2() throws IOException {
        String name1 = "O=First Org.";
        String name2 = "O=Second Org.";
        X500Principal iss1 = new X500Principal(name1);
        X500Principal iss2 = new X500Principal(name2);
        TestCert cert_1 = new TestCert(iss1);
        TestCert cert_2 = new TestCert(iss2);
        X509CertSelector selector = new X509CertSelector();

        selector.setIssuer((String) null);
        assertTrue(
                "Any certificates should match in the case of null issuer criteria.",
                selector.match(cert_1) && selector.match(cert_2));

        selector.setIssuer(name1);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                selector.match(cert_2));
        selector.setIssuer(name2);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_2));
    }

    /**
     * getIssuerAsString() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetIssuerAsString() {
        String name1 = "O=First Org.";
        String name2 = "O=Second Org.";
        X500Principal iss1 = new X500Principal(name1);
        X500Principal iss2 = new X500Principal(name2);
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getIssuerAsString());
        selector.setIssuer(iss1);
        assertEquals("The returned issuer should be equal to specified",
                            new X500Principal(name1),
                            new X500Principal(selector.getIssuerAsString()));
        assertFalse("The returned issuer should differ",
                            new X500Principal(name2).equals(
                            new X500Principal(selector.getIssuerAsString())));
        selector.setIssuer(iss2);
        assertEquals("The returned issuer should be equal to specified",
                            new X500Principal(name2),
                            new X500Principal(selector.getIssuerAsString()));
    }

    /**
     * setIssuer(byte[] issuerDN) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetIssuer3() throws IOException {
        byte[] name1 = new byte[]
            //manually obtained DER encoding of "O=First Org." issuer name;
            {48, 21, 49, 19, 48, 17, 6, 3, 85, 4, 10, 19, 10,
                70, 105, 114, 115, 116, 32, 79, 114, 103, 46};
        byte[] name2 = new byte[]
            //manually obtained DER encoding of "O=Second Org." issuer name;
            {48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11,
            83, 101, 99, 111, 110, 100, 32, 79, 114, 103, 46};
        X500Principal iss1 = new X500Principal(name1);
        X500Principal iss2 = new X500Principal(name2);
        TestCert cert_1 = new TestCert(iss1);
        TestCert cert_2 = new TestCert(iss2);
        X509CertSelector selector = new X509CertSelector();

        selector.setIssuer((byte[]) null);
        assertTrue(
                "Any certificates should match in the case of null issuer criteria.",
                selector.match(cert_1) && selector.match(cert_2));

        selector.setIssuer(name1);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                selector.match(cert_2));
        selector.setIssuer(name2);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_2));
    }

    /**
     * getIssuerAsBytes() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetIssuerAsBytes() throws IOException {
        byte[] name1 = new byte[]
            //manually obtained DER encoding of "O=First Org." issuer name;
            {48, 21, 49, 19, 48, 17, 6, 3, 85, 4, 10, 19, 10,
                70, 105, 114, 115, 116, 32, 79, 114, 103, 46};
        byte[] name2 = new byte[]
            //manually obtained DER encoding of "O=Second Org." issuer name;
            {48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11,
            83, 101, 99, 111, 110, 100, 32, 79, 114, 103, 46};
        X500Principal iss1 = new X500Principal(name1);
        X500Principal iss2 = new X500Principal(name2);
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getIssuerAsBytes());

        selector.setIssuer(iss1);
        assertEquals("The returned issuer should be equal to specified",
                new X500Principal(name1), new X500Principal(selector
                        .getIssuerAsBytes()));
        assertFalse("The returned issuer should differ", new X500Principal(
                name2).equals(new X500Principal(selector.getIssuerAsBytes())));

        selector.setIssuer(iss2);
        assertEquals("The returned issuer should be equal to specified",
                new X500Principal(name2), new X500Principal(selector
                        .getIssuerAsBytes()));
    }

    /**
     * setSubject(X500Principal subject) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetSubject1() {
        X500Principal sub1 = new X500Principal("O=First Org.");
        X500Principal sub2 = new X500Principal("O=Second Org.");
        TestCert cert_1 = new TestCert(sub1);
        TestCert cert_2 = new TestCert(sub2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSubject((X500Principal) null);
        assertTrue("Any certificates should match "
                                + "in the case of null subjcet criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setSubject(sub1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setSubject(sub2);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getSubject() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetSubject() {
        X500Principal sub1 = new X500Principal("O=First Org.");
        X500Principal sub2 = new X500Principal("O=Second Org.");
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getSubject());
        selector.setSubject(sub1);
        assertEquals("The returned subject should be equal to specified",
                                        sub1, selector.getSubject());
        assertFalse("The returned subject should differ",
                                        sub2.equals(selector.getSubject()));
    }

    /**
     * setSubject(String subjectDN) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetSubject2() throws IOException {
        String name1 = "O=First Org.";
        String name2 = "O=Second Org.";
        X500Principal sub1 = new X500Principal(name1);
        X500Principal sub2 = new X500Principal(name2);
        TestCert cert_1 = new TestCert(sub1);
        TestCert cert_2 = new TestCert(sub2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSubject((String) null);
        assertTrue(
                "Any certificates should match in the case of null subject criteria.",
                selector.match(cert_1) && selector.match(cert_2));

        selector.setSubject(name1);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                selector.match(cert_2));

        selector.setSubject(name2);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_2));
    }

    /**
     * getSubjectAsString() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetSubjectAsString() {
        String name1 = "O=First Org.";
        String name2 = "O=Second Org.";
        X500Principal sub1 = new X500Principal(name1);
        X500Principal sub2 = new X500Principal(name2);
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                                selector.getSubjectAsString());
        selector.setSubject(sub1);
        assertEquals("The returned subject should be equal to specified",
                            new X500Principal(name1),
                            new X500Principal(selector.getSubjectAsString()));
        assertFalse("The returned subject should differ",
                            new X500Principal(name2).equals(
                            new X500Principal(selector.getSubjectAsString())));
        selector.setSubject(sub2);
        assertEquals("The returned subject should be equal to specified",
                            new X500Principal(name2),
                            new X500Principal(selector.getSubjectAsString()));
    }

    /**
     * setSubject(byte[] subjectDN) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetSubject3() throws IOException {
        byte[] name1 = new byte[]
            //manually obtained DER encoding of "O=First Org." issuer name;
            {48, 21, 49, 19, 48, 17, 6, 3, 85, 4, 10, 19, 10,
                70, 105, 114, 115, 116, 32, 79, 114, 103, 46};
        byte[] name2 = new byte[]
            //manually obtained DER encoding of "O=Second Org." issuer name;
            {48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11,
            83, 101, 99, 111, 110, 100, 32, 79, 114, 103, 46};
        X500Principal sub1 = new X500Principal(name1);
        X500Principal sub2 = new X500Principal(name2);
        TestCert cert_1 = new TestCert(sub1);
        TestCert cert_2 = new TestCert(sub2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSubject((byte[]) null);
        assertTrue(
                "Any certificates should match in the case of null issuer criteria.",
                selector.match(cert_1) && selector.match(cert_2));

        selector.setSubject(name1);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                selector.match(cert_2));

        selector.setSubject(name2);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_2));
    }

    /**
     * getSubjectAsBytes() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetSubjectAsBytes() throws IOException {
        byte[] name1 = new byte[]
            //manually obtained DER encoding of "O=First Org." issuer name;
            {48, 21, 49, 19, 48, 17, 6, 3, 85, 4, 10, 19, 10,
                70, 105, 114, 115, 116, 32, 79, 114, 103, 46};
        byte[] name2 = new byte[]
            //manually obtained DER encoding of "O=Second Org." issuer name;
            {48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11,
            83, 101, 99, 111, 110, 100, 32, 79, 114, 103, 46};
        X500Principal sub1 = new X500Principal(name1);
        X500Principal sub2 = new X500Principal(name2);
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getSubjectAsBytes());
        selector.setSubject(sub1);

        assertEquals("The returned issuer should be equal to specified",
                new X500Principal(name1), new X500Principal(selector
                        .getSubjectAsBytes()));
        assertFalse("The returned issuer should differ", new X500Principal(
                name2).equals(new X500Principal(selector.getSubjectAsBytes())));

        selector.setSubject(sub2);
        assertEquals("The returned issuer should be equal to specified",
                new X500Principal(name2), new X500Principal(selector
                        .getSubjectAsBytes()));
    }

    /**
     * setSubjectKeyIdentifier(byte[] subjectKeyID) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match, and if the initialization
     * object are copied during the initialization.
     */
    public void testSetSubjectKeyIdentifier() {
        byte[] skid1 = new byte[] {1, 2, 3, 4, 5}; // random value
        byte[] skid2 = new byte[] {5, 4, 3, 2, 1}; // random value
        TestCert cert_1 = new TestCert(skid1);
        TestCert cert_2 = new TestCert(skid2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSubjectKeyIdentifier(null);
        assertTrue("Any certificate should match in the case of null "
                                                + "serialNumber criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setSubjectKeyIdentifier(skid1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setSubjectKeyIdentifier(skid2);
        skid2[0] ++;
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getSubjectKeyIdentifier() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     * and its modification does not cause the modification of internal object.
     */
    public void testGetSubjectKeyIdentifier() {
        byte[] skid1 = new byte[] {1, 2, 3, 4, 5}; // random value
        byte[] skid2 = new byte[] {4, 5, 5, 4, 3, 2, 1}; // random value
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                            selector.getSubjectKeyIdentifier());
        selector.setSubjectKeyIdentifier(skid1);
        assertTrue("The returned keyID should be equal to specified",
                    Arrays.equals(skid1, selector.getSubjectKeyIdentifier()));
        selector.getSubjectKeyIdentifier()[0] ++;
        assertTrue("The returned keyID should be equal to specified",
                    Arrays.equals(skid1, selector.getSubjectKeyIdentifier()));
        assertFalse("The returned keyID should differ",
                    Arrays.equals(skid2, selector.getSubjectKeyIdentifier()));
    }

    /**
     * setAuthorityKeyIdentifier(byte[] authorityKeyID) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match, and if the initialization
     * object are copied during the initialization.
     */
    public void testSetAuthorityKeyIdentifier() {
        byte[] akid1 = new byte[] {1, 2, 3, 4, 5}; // random value
        byte[] akid2 = new byte[] {5, 4, 3, 2, 1}; // random value
        TestCert cert_1 = new TestCert(akid1);
        TestCert cert_2 = new TestCert(akid2);
        X509CertSelector selector = new X509CertSelector();

        selector.setAuthorityKeyIdentifier(null);
        assertTrue("Any certificate should match in the case of null "
                                                + "serialNumber criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setAuthorityKeyIdentifier(akid1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setAuthorityKeyIdentifier(akid2);
        akid2[0] ++;
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getAuthorityKeyIdentifier() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     * and its modification does not cause the modification of internal object.
     */
    public void testGetAuthorityKeyIdentifier() {
        byte[] akid1 = new byte[] {4, 5, 1, 2, 3, 4, 5}; // random value
        byte[] akid2 = new byte[] {4, 5, 5, 4, 3, 2, 1}; // random value
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                        selector.getAuthorityKeyIdentifier());
        selector.setAuthorityKeyIdentifier(akid1);
        assertTrue("The returned keyID should be equal to specified",
                    Arrays.equals(akid1, selector.getAuthorityKeyIdentifier()));
        selector.getAuthorityKeyIdentifier()[0] ++;
        assertTrue("The returned keyID should be equal to specified",
                    Arrays.equals(akid1, selector.getAuthorityKeyIdentifier()));
        assertFalse("The returned keyID should differ",
                    Arrays.equals(akid2, selector.getAuthorityKeyIdentifier()));
    }

    /**
     * setCertificateValid(Date certificateValid) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match, and if the initialization
     * object are copied during the initialization.
     */
    public void testSetCertificateValid() {
        Date date1 = new Date(100);
        Date date2 = new Date(200);
        TestCert cert_1 = new TestCert(date1);
        TestCert cert_2 = new TestCert(date2);
        X509CertSelector selector = new X509CertSelector();

        selector.setCertificateValid(null);
        assertTrue("Any certificate should match in the case of null "
                                                + "serialNumber criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setCertificateValid(date1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setCertificateValid(date2);
        date2.setTime(300);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getCertificateValid() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     * and its modification does not cause the modification of internal object.
     */
    public void testGetCertificateValid() {
        Date date1 = new Date(100);
        Date date2 = new Date(200);
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                        selector.getCertificateValid());
        selector.setCertificateValid(date1);
        assertTrue("The returned date should be equal to specified",
                    date1.equals(selector.getCertificateValid()));
        selector.getCertificateValid().setTime(200);
        assertTrue("The returned date should be equal to specified",
                    date1.equals(selector.getCertificateValid()));
        assertFalse("The returned date should differ",
                    date2.equals(selector.getCertificateValid()));
    }

    /**
     * setPrivateKeyValid(Date privateKeyValid) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match, and if the initialization
     * object are copied during the initialization.
     */
    public void testSetPrivateKeyValid() {
        Date date1 = new Date(100000000);
        Date date2 = new Date(200000000);
        Date date3 = new Date(300000000);
        Date date4 = new Date(150000000);
        Date date5 = new Date(250000000);
        TestCert cert_1 = new TestCert(date1, date2);
        TestCert cert_2 = new TestCert(date2, date3);
        X509CertSelector selector = new X509CertSelector();

        selector.setPrivateKeyValid(null);
        assertTrue("Any certificate should match in the case of null "
                                            + "privateKeyValid criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setPrivateKeyValid(date4);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setPrivateKeyValid(date5);
        date5.setTime(date4.getTime());
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getPrivateKeyValid() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     * and its modification does not cause the modification of internal object.
     */
    public void testGetPrivateKeyValid() {
        Date date1 = new Date(100);
        Date date2 = new Date(200);
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                        selector.getPrivateKeyValid());
        selector.setPrivateKeyValid(date1);
        assertTrue("The returned date should be equal to specified",
                    date1.equals(selector.getPrivateKeyValid()));
        selector.getPrivateKeyValid().setTime(200);
        assertTrue("The returned date should be equal to specified",
                    date1.equals(selector.getPrivateKeyValid()));
        assertFalse("The returned date should differ",
                    date2.equals(selector.getPrivateKeyValid()));
    }

    /**
     * setSubjectPublicKeyAlgID(String oid) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match
     */
    public void testSetSubjectPublicKeyAlgID() throws Exception {
        String pkaid1 = "1.2.840.113549.1.1.1"; // RSA (source: http://asn1.elibel.tm.fr)
        String pkaid2 = "1.2.840.10040.4.1"; // DSA (source: http://asn1.elibel.tm.fr)

        PublicKey pkey1 = new TestKeyPair("RSA").getPublic();
        PublicKey pkey2 = new TestKeyPair("DSA").getPublic();

        TestCert cert_1 = new TestCert(pkey1);
        TestCert cert_2 = new TestCert(pkey2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSubjectPublicKeyAlgID(null);
        assertTrue("Any certificate should match in the case of null "
                + "subjectPublicKeyAlgID criteria.", selector.match(cert_1)
                && selector.match(cert_2));

        selector.setSubjectPublicKeyAlgID(pkaid1);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                selector.match(cert_2));

        selector.setSubjectPublicKeyAlgID(pkaid2);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_2));
    }

    /**
     * @tests java.security.cert.X509CertSelector#setSubjectPublicKeyAlgID(java.lang.String)
     */
    public void test_setSubjectPublicKeyAlgIDLjava_lang_String() throws Exception {
        //Regression for HARMONY-465
        X509CertSelector obj = new X509CertSelector();
        try {
            obj.setSubjectPublicKeyAlgID("abc");
            fail("IOException expected");
        } catch (IOException e) {
            // expected
        }
    }

    /**
     * getSubjectPublicKeyAlgID() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     */
    public void testGetSubjectPublicKeyAlgID() throws IOException {
        String pkaid1 = "1.2.840.113549.1.1.1"; // RSA encryption (source: http://asn1.elibel.tm.fr)
        String pkaid2 = "1.2.840.113549.1.1.2"; // MD2 with RSA encryption (source: http://asn1.elibel.tm.fr)
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                        selector.getSubjectPublicKeyAlgID());

        selector.setSubjectPublicKeyAlgID(pkaid1);
        assertTrue("The returned oid should be equal to specified",
                    pkaid1.equals(selector.getSubjectPublicKeyAlgID()));
        assertFalse("The returned oid should differ",
                    pkaid2.equals(selector.getSubjectPublicKeyAlgID()));
    }

    /**
     * setSubjectPublicKey(PublicKey key) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match.
     */
    public void testSetSubjectPublicKey1() throws Exception {
        PublicKey pkey1 = new TestKeyPair("RSA").getPublic();
        PublicKey pkey2 = new TestKeyPair("DSA").getPublic();

        TestCert cert_1 = new TestCert(pkey1);
        TestCert cert_2 = new TestCert(pkey2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSubjectPublicKey((PublicKey) null);
        assertTrue("Any certificate should match in the case of null "
                                            + "subjectPublicKey criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setSubjectPublicKey(pkey1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setSubjectPublicKey(pkey2);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getSubjectPublicKey() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value corresponds to specified
     */
    public void testGetSubjectPublicKey1() throws Exception {

        PublicKey pkey = new TestKeyPair("RSA").getPublic();

        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                            selector.getSubjectPublicKey());
        selector.setSubjectPublicKey(pkey);
        PublicKey result = selector.getSubjectPublicKey();

        assertEquals("The name of algorithm should be RSA",
                                        result.getAlgorithm(), "RSA");
    }

    /**
     * setSubjectPublicKey(byte[] key) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match, and if the initialization
     * object are copied during the initialization.
     */
    public void testSetSubjectPublicKey2() throws Exception {
        PublicKey pkey1 = new TestKeyPair("RSA").getPublic();
        PublicKey pkey2 = new TestKeyPair("DSA").getPublic();

        byte[] encoding1 = pkey1.getEncoded();
        byte[] encoding2 = pkey2.getEncoded();
        TestCert cert_1 = new TestCert(pkey1);
        TestCert cert_2 = new TestCert(pkey2);
        X509CertSelector selector = new X509CertSelector();

        selector.setSubjectPublicKey((byte[]) null);
        assertTrue("Any certificate should match in the case of null "
                + "subjectPublicKey criteria.", selector.match(cert_1)
                && selector.match(cert_2));

        selector.setSubjectPublicKey(encoding1);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));

        encoding1[0]++;
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                selector.match(cert_2));

        selector.setSubjectPublicKey(encoding2);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_2));
    }

    /**
     * getSubjectPublicKey() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value corresponds to specified
     */
    public void testGetSubjectPublicKey2() throws Exception {

        PublicKey pkey = new TestKeyPair("RSA").getPublic();

        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                            selector.getSubjectPublicKey());

        selector.setSubjectPublicKey(pkey.getEncoded());

        PublicKey result = selector.getSubjectPublicKey();

        assertEquals("The name of algorithm should be RSA",
                                        result.getAlgorithm(), "RSA");
    }

    /**
     * setKeyUsage(boolean[] keyUsage) method testing.
     * Tests if any certificates match in the case of null criteria,
     * if [not]proper certificates [do not]match, and if the initialization
     * object are copied during the initialization. Also checks if selector
     * matches the certificate which does not have a keyUsage extension.
     */
    public void testSetKeyUsage() {
        boolean[] ku1 = new boolean[]
                    {true, true, true, true, true, true, true, true, true};
        // decipherOnly is disallowed
        boolean[] ku2 = new boolean[]
                    {true, true, true, true, true, true, true, true, false};
        TestCert cert_1 = new TestCert(ku1);
        TestCert cert_2 = new TestCert(ku2);
        TestCert cert_3 = new TestCert((boolean[]) null);
        X509CertSelector selector = new X509CertSelector();

        selector.setKeyUsage(null);
        assertTrue("Any certificate should match in the case of null "
                                                        + "keyUsage criteria.",
                            selector.match(cert_1) && selector.match(cert_2));
        selector.setKeyUsage(ku1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        assertTrue("The certificate which does not have a keyUsage extension "
                   + "implicitly allows all keyUsage values.",
                                                        selector.match(cert_3));
        selector.setKeyUsage(ku2);
        ku2[0] = !ku2[0];
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getKeyUsage() method testing.
     * Tests if the method return null in the case of not specified criteria,
     * if the returned value [does not]corresponds to [not]specified
     * and its modification does not cause the modification of internal object.
     */
    public void testGetKeyUsage() {
        boolean[] ku = new boolean[]
                    {true, false, true, false, true, false, true, false, true};
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null", selector.getKeyUsage());
        selector.setKeyUsage(ku);
        assertTrue("The returned date should be equal to specified",
                                    Arrays.equals(ku, selector.getKeyUsage()));
        boolean[] result = selector.getKeyUsage();
        result[0] = !result[0];
        assertTrue("The returned keyUsage should be equal to specified",
                                    Arrays.equals(ku, selector.getKeyUsage()));
    }

    /**
     * setExtendedKeyUsage(Set<String> keyPurposeSet) method testing.
     */
    public void testSetExtendedKeyUsage() throws IOException {
        HashSet ku1 = new HashSet(Arrays.asList(new String[] {
                "1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2", "1.3.6.1.5.5.7.3.3",
                "1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.8", "1.3.6.1.5.5.7.3.9",
                "1.3.6.1.5.5.7.3.5", "1.3.6.1.5.5.7.3.6", "1.3.6.1.5.5.7.3.7"}
                              ));
        HashSet ku2 = new HashSet(Arrays.asList(new String[] {
                "1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2", "1.3.6.1.5.5.7.3.3",
                "1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.8", "1.3.6.1.5.5.7.3.9",
                "1.3.6.1.5.5.7.3.5", "1.3.6.1.5.5.7.3.6"}));
        TestCert cert_1 = new TestCert(ku1);
        TestCert cert_2 = new TestCert(ku2);
        TestCert cert_3 = new TestCert((Set) null);
        X509CertSelector selector = new X509CertSelector();

        selector.setExtendedKeyUsage(null);
        assertTrue("Any certificate should match in the case of null "
                + "extendedKeyUsage criteria.", selector.match(cert_1)
                && selector.match(cert_2));

        selector.setExtendedKeyUsage(ku1);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                selector.match(cert_2));
        assertTrue("The certificate which does not have a keyUsage extension "
                + "implicitly allows all keyUsage values.", selector
                .match(cert_3));
        ku1.remove("1.3.6.1.5.5.7.3.7"); // remove the missing in ku2 keyUsage
        assertFalse("The modification of initialization object "
                + "should not affect the modification of internal object.",
                selector.match(cert_2));

        selector.setExtendedKeyUsage(ku2);
        assertTrue("The certificate should match the selection criteria.",
                selector.match(cert_2));
    }

    /**
     * getExtendedKeyUsage() method testing.
     */
    public void testGetExtendedKeyUsage() {
        HashSet ku = new HashSet(Arrays.asList(new String[] {
                "1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2", "1.3.6.1.5.5.7.3.3",
                "1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.8", "1.3.6.1.5.5.7.3.9",
                "1.3.6.1.5.5.7.3.5", "1.3.6.1.5.5.7.3.6", "1.3.6.1.5.5.7.3.7"}
                            ));
        X509CertSelector selector = new X509CertSelector();

        assertNull("Selector should return null",
                                                selector.getExtendedKeyUsage());
        try {
            selector.setExtendedKeyUsage(ku);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The returned extendedKeyUsage should be equal to specified",
                            ku.equals(selector.getExtendedKeyUsage()));
        try {
            selector.getExtendedKeyUsage().add("KRIBLE-GRABLI");
            fail("The returned Set should be immutable.");
        } catch (UnsupportedOperationException e) {
        }
    }

    /**
     * setSubjectAlternativeNames(Collection<List<?>> names) method testing.
     */
    public void testSetSubjectAlternativeNames() {
        try {
            GeneralName san0 =
                new GeneralName(new OtherName("1.2.3.4.5",
                            new byte[] {1, 2, 0, 1}));
            GeneralName san1 = new GeneralName(1, "rfc@822.Name");
            GeneralName san2 = new GeneralName(2, "dNSName");
            GeneralName san3 = new GeneralName(new ORAddress());
            GeneralName san4 = new GeneralName(new Name("O=Organization"));
            GeneralName san5 =
                new GeneralName(new EDIPartyName("assigner", "party"));
            GeneralName san6 = new GeneralName(6, "http://uniform.Resource.Id");
            GeneralName san7 = new GeneralName(7, "1.1.1.1");
            GeneralName san8 = new GeneralName(8, "1.2.3.4444.55555");

            GeneralNames sans_1 = new GeneralNames();
            sans_1.addName(san0);
            sans_1.addName(san1);
            sans_1.addName(san2);
            sans_1.addName(san3);
            sans_1.addName(san4);
            sans_1.addName(san5);
            sans_1.addName(san6);
            sans_1.addName(san7);
            sans_1.addName(san8);
            GeneralNames sans_2 = new GeneralNames();
            sans_2.addName(san0);

            TestCert cert_1 = new TestCert(sans_1);
            TestCert cert_2 = new TestCert(sans_2);
            X509CertSelector selector = new X509CertSelector();
            selector.setMatchAllSubjectAltNames(true);

            selector.setSubjectAlternativeNames(null);
            assertTrue("Any certificate should match in the case of null "
                                        + "subjectAlternativeNames criteria.",
                            selector.match(cert_1) && selector.match(cert_2));

            Collection sans = sans_1.getPairsList();
            selector.setSubjectAlternativeNames(sans);
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertFalse("The certificate should not match "
                        + "the selection criteria.",    selector.match(cert_2));
            sans.clear();
            assertTrue("The modification of initialization object "
                        + "should not affect the modification "
                        + "of internal object.",        selector.match(cert_1));
            selector.setSubjectAlternativeNames(sans_2.getPairsList());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * addSubjectAlternativeName(int type, String name) method testing.
     */
    public void testAddSubjectAlternativeName1() throws IOException {
        String name1 = "rfc@822.Name";
        String name2 = "dNSName";
        String name4 = "O=Organization";
        String name6 = "http://uniform.Resource.Id";
        String name7 = "255.255.255.0";
        String name8 = "1.2.3.4444.55555";

        GeneralName san1 = new GeneralName(1, name1);
        GeneralName san2 = new GeneralName(2, name2);
        GeneralName san4 = new GeneralName(4, name4);
        GeneralName san6 = new GeneralName(6, name6);
        GeneralName san7 = new GeneralName(7, name7);
        GeneralName san8 = new GeneralName(8, name8);

        GeneralNames sans_1 = new GeneralNames();
        sans_1.addName(san1);
        sans_1.addName(san2);
        sans_1.addName(san4);
        sans_1.addName(san6);
        sans_1.addName(san7);
        sans_1.addName(san8);
        GeneralNames sans_2 = new GeneralNames();
        sans_2.addName(san1);
        sans_2.addName(san2);

        TestCert cert_1 = new TestCert(sans_1);
        TestCert cert_2 = new TestCert(sans_2);
        X509CertSelector selector = new X509CertSelector();
        selector.setMatchAllSubjectAltNames(true);

        try {
            selector.addSubjectAlternativeName(1, name1);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));

        try {
            selector.addSubjectAlternativeName(2, name2);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));

        try {
            selector.addSubjectAlternativeName(4, name4);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        try {
            selector.addSubjectAlternativeName(6, name6);
            selector.addSubjectAlternativeName(7, name7);
            selector.addSubjectAlternativeName(8, name8);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * addSubjectAlternativeName(int type, byte[] name) method testing.
     */
    public void testAddSubjectAlternativeName2() {
        try {
            GeneralName san0 =
                new GeneralName(new OtherName("1.2.3.4.5",
                        ASN1Integer.getInstance().encode(
                                BigInteger.valueOf(55L).toByteArray())
                            ));
            GeneralName san1 = new GeneralName(1, "rfc@822.Name");
            GeneralName san2 = new GeneralName(2, "dNSName");
            GeneralName san3 = new GeneralName(new ORAddress());
            GeneralName san4 = new GeneralName(new Name("O=Organization"));
            GeneralName san5 =
                new GeneralName(new EDIPartyName("assigner", "party"));
            GeneralName san6 = new GeneralName(6, "http://uniform.Resource.Id");
            GeneralName san7 = new GeneralName(new byte[] {1, 1, 1, 1});
            GeneralName san8 = new GeneralName(8, "1.2.3.4444.55555");

            GeneralNames sans_1 = new GeneralNames();
            sans_1.addName(san0);
            sans_1.addName(san1);
            sans_1.addName(san2);
            sans_1.addName(san3);
            sans_1.addName(san4);
            sans_1.addName(san5);
            sans_1.addName(san6);
            sans_1.addName(san7);
            sans_1.addName(san8);
            GeneralNames sans_2 = new GeneralNames();
            sans_2.addName(san0);
            sans_2.addName(san1);
            sans_2.addName(san2);

            TestCert cert_1 = new TestCert(sans_1);
            TestCert cert_2 = new TestCert(sans_2);
            X509CertSelector selector = new X509CertSelector();
            selector.setMatchAllSubjectAltNames(true);

            selector.addSubjectAlternativeName(0, san0.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
            selector.addSubjectAlternativeName(1, san1.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
            selector.addSubjectAlternativeName(2, san2.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
            selector.addSubjectAlternativeName(3, san3.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                    selector.match(cert_1));
            assertFalse("The certificate should not match the selection criteria.",
                                                    selector.match(cert_2));
            selector.addSubjectAlternativeName(4, san4.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertFalse("The certificate should not match "
                        + "the selection criteria.",    selector.match(cert_2));
            selector.addSubjectAlternativeName(5, san5.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertFalse("The certificate should not match "
                        + "the selection criteria.",    selector.match(cert_2));
            selector.addSubjectAlternativeName(6, san6.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertFalse("The certificate should not match "
                        + "the selection criteria.",    selector.match(cert_2));
            selector.addSubjectAlternativeName(7, san7.getEncodedName());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertFalse("The certificate should not match "
                        + "the selection criteria.",    selector.match(cert_2));
            byte[] oid = san8.getEncodedName();
            selector.addSubjectAlternativeName(8, oid);
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            assertFalse("The certificate should not match "
                        + "the selection criteria.",    selector.match(cert_2));
            oid[3] += 1;
            assertTrue("The byte array should be cloned to protect against "
                       + "subsequent modifications.",   selector.match(cert_1));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * getSubjectAlternativeNames() method testing.
     */
    public void testGetSubjectAlternativeNames() {
        try {
            GeneralName san1 = new GeneralName(1, "rfc@822.Name");
            GeneralName san2 = new GeneralName(2, "dNSName");

            GeneralNames sans = new GeneralNames();
            sans.addName(san1);
            sans.addName(san2);

            TestCert cert_1 = new TestCert(sans);
            X509CertSelector selector = new X509CertSelector();

            assertNull("Selector should return null",
                                        selector.getSubjectAlternativeNames());

            selector.setSubjectAlternativeNames(sans.getPairsList());
            assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
            selector.getSubjectAlternativeNames().clear();
            assertTrue("The modification of initialization object "
                        + "should not affect the modification "
                        + "of internal object.",        selector.match(cert_1));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * setMatchAllSubjectAltNames(boolean matchAllNames) method testing.
     */
    public void testSetMatchAllSubjectAltNames() {
        try {
            GeneralName san1 = new GeneralName(1, "rfc@822.Name");
            GeneralName san2 = new GeneralName(2, "dNSName");

            GeneralNames sans_1 = new GeneralNames();
            sans_1.addName(san1);
            GeneralNames sans_2 = new GeneralNames();
            sans_2.addName(san1);
            sans_2.addName(san2);

            TestCert cert = new TestCert(sans_1);
            X509CertSelector selector = new X509CertSelector();
            selector.setMatchAllSubjectAltNames(true);

            selector.setSubjectAlternativeNames(sans_2.getPairsList());
            assertFalse("Only certificate which contain all of the specified "
                       + "subject alternative names should match.",
                                                        selector.match(cert));
            selector.setMatchAllSubjectAltNames(false);
            /*
            assertTrue("The certificate which contain at least one of the "
                       + "specified subject alternative names must match.",
                                                        selector.match(cert));
                                                        */
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * getMatchAllSubjectAltNames() method testing.
     */
    public void testGetMatchAllSubjectAltNames() {
        X509CertSelector selector = new X509CertSelector();
        assertTrue("The matchAllNames initially should be true",
                selector.getMatchAllSubjectAltNames());
        selector.setMatchAllSubjectAltNames(false);
        assertFalse("The value should be false",
                selector.getMatchAllSubjectAltNames());
    }

    /**
     * setNameConstraints(byte[] bytes) method testing.
     * Constructs the NameConstraints DER structure with
     * GeneralNames of types: 1, 2, 6, 7 and set it as a criterion.
     */
    public void testSetNameConstraints0() throws IOException {
        // Restrictions apply only when the specified name form is present.
        // If no name of the type is in the certificate,
        // the certificate is acceptable (rfc 3280).

        GeneralName [] name_constraints = new GeneralName[] {
            new GeneralName(1, "822.Name"),
            new GeneralName(1, "rfc@822.Name"),
            new GeneralName(2, "Name.org"),
            new GeneralName(2, "dNS.Name.org"),
            //new GeneralName(4, "O=Organization"),
            new GeneralName(6, "http://.Resource.Id"),
            new GeneralName(6, "http://uniform.Resource.Id"),
            new GeneralName(7, "1.1.1.1"),
            // new GeneralName(7, new byte[] {1, 1, 1, 1, 3, 3, 3, 3}),
            new GeneralName(new byte[] {1, 1, 1, 1, 1, 1, 1, 1,
                                        1, 1, 1, 1, 1, 1, 1, 1}),
            // new GeneralName(7, new byte[] {1, 1, 1, 1, 1, 1, 1, 1,
            //                                1, 1, 1, 1, 1, 1, 1, 1,
            //                                3, 3, 3, 3, 3, 3, 3, 3,
            //                                3, 3, 3, 3, 3, 3, 3, 3})
        };

        // names which should match divided from names which should not
        // match by null
        GeneralName[][] alternative_names = new GeneralName[][] {
            {
                new GeneralName(1, "rfc@822.Name"),
                null,
                new GeneralName(1, "rfc@Other.Name")
            }, {
                new GeneralName(1, "rfc@822.Name"),
                null,
                new GeneralName(1, "rfc@Other.Name")
            }, {
                new GeneralName(2, "Name.org"),
                new GeneralName(2, "dNS.Name.org"),
                null,
                new GeneralName(2, "dNS.OtherName.org")
            }, {
                new GeneralName(2, "dNS.Name.org"),
                null,
                new GeneralName(2, "Name.org"),
                new GeneralName(2, "dNS.OtherName.org")
            }, {

            //    new GeneralName(4, "O=Organization"),
            //    null,
            //    new GeneralName(4, "O=OtherOrganization")
            //}, {

                new GeneralName(6, "http://uniform.Resource.Id/location"),
                null,
                //new GeneralName(6, "http://Resource.Id")
            }, {
                new GeneralName(6, "http://uniform.Resource.Id"),
                null,
                new GeneralName(6, "http://Resource.Id")
            }, {
                new GeneralName(new byte[] {1, 1, 1, 1}),
                null,
                new GeneralName(new byte[] {2, 2, 2, 2})
            // }, {
            //     new GeneralName(7, new byte[] {1, 1, 1, 1}),
            //     new GeneralName(7, new byte[] {2, 2, 2, 2}),
            //     new GeneralName(7, new byte[] {3, 3, 3, 3}),
            //     null,
            //     new GeneralName(7, new byte[] {4, 4, 4, 4})
            }, {
                new GeneralName(new byte[] {1, 1, 1, 1, 1, 1, 1, 1,
                                            1, 1, 1, 1, 1, 1, 1, 1}),
                null,
                new GeneralName(new byte[] {2, 2, 2, 2, 2, 2, 2, 2,
                                            2, 2, 2, 2, 2, 2, 2, 2}),
            // }, {
            //     new GeneralName(7, new byte[] {1, 1, 1, 1, 1, 1, 1, 1,
            //                                    1, 1, 1, 1, 1, 1, 1, 1}),
            //     new GeneralName(7, new byte[] {2, 2, 2, 2, 2, 2, 2, 2,
            //                                    2, 2, 2, 2, 2, 2, 2, 2}),
            //     new GeneralName(7, new byte[] {3, 3, 3, 3, 3, 3, 3, 3,
            //                                    3, 3, 3, 3, 3, 3, 3, 3}),
            //     null,
            //     new GeneralName(7, new byte[] {4, 4, 4, 4, 4, 4, 4, 4,
            //                                    4, 4, 4, 4, 4, 4, 4, 4}),
            }
        };

        X509CertSelector selector = new X509CertSelector();
        String subject = "O=Organization";
        X500Principal x500Subject = new X500Principal(subject);
        try {
            Name nameSubject = new Name(subject);
            for (int i=0; i<name_constraints.length; i++) {
                // make the subtrees (part of name constraints)
                // this subtrees will be used as permited and as excluded
                GeneralSubtree subtree =
                    new GeneralSubtree(name_constraints[i]);
                GeneralSubtrees subtrees = new GeneralSubtrees();
                NameConstraints constraints;
                subtrees.addSubtree(subtree);
                // start the checking for each alt. name corresponding
                // to current name_constraints[i]
                boolean check_matching = true;
                for (int j=0; j<alternative_names[i].length; j++) {
                    GeneralNames alt_names_extension = new GeneralNames();
                    if (alternative_names[i][j] == null) {
                        // double trick: turn the switch and check that the
                        // restrictions apply only when the specified name
                        // form is presented.  If no name of the type is in the
                        // certificate, the certificate is acceptable.
                        check_matching = false;
                    } else {
                        alt_names_extension.addName(alternative_names[i][j]);
                    }
                    TestCert certificate = new TestCert(alt_names_extension);
                    certificate.setSubject(x500Subject);
                    certificate.setEncoding(getCertEncoding(nameSubject,
                                                     alt_names_extension));
                    // first check if permited name match
                    constraints = new NameConstraints(subtrees, null);
                    selector.setNameConstraints(constraints.getEncoded());
                    boolean expected = check_matching
                                       || (alternative_names[i][j] == null);
                    assertTrue("The method match() for:\n        "
                               + alternative_names[i][j]
                               + "\nand permited name\n        "
                               + name_constraints[i]
                               + "\nshould return: "+expected,
                               selector.match(certificate) == expected);
                    // second check if excluded name does not match
                    constraints = (check_matching)
                                    // check for 'Any name matching a
                                    // restriction in the excludedSubtrees
                                    // field is invalid regardless of
                                    // information appearing in the
                                    // permittedSubtrees'.
                                    ? new NameConstraints(subtrees, subtrees)
                                    : new NameConstraints(null, subtrees);
                    selector.setNameConstraints(constraints.getEncoded());
                    expected = !check_matching
                               || (alternative_names[i][j] == null);
                    assertTrue("The method match() for:\n        "
                               + alternative_names[i][j]
                               + "\nand excluded name\n        "
                               + name_constraints[i]
                               + "\nshould return: "+expected,
                               selector.match(certificate) == expected);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * setNameConstraints(byte[] bytes) method testing.
     * Constructs the NameConstraints DER structure with
     * GeneralNames of types: 1, 2, 6, 7 and set it as a criterion.
     */
    public void testSetNameConstraints1() throws IOException {

        GeneralName [] name_constraints = new GeneralName[] {
            new GeneralName(1, "822.Name"),
            new GeneralName(1, "rfc@822.Name"),
            new GeneralName(2, "Name.org"),
            new GeneralName(2, "dNS.Name.org"),
            new GeneralName(6, "http://.Resource.Id"),
            new GeneralName(6, "http://uniform.Resource.Id"),
            new GeneralName(7, "1.1.1.1"),
            new GeneralName(7, "1.1.1.1/3.3.3.3"),
            new GeneralName(7, "0101:0101:0101:0101:0101:0101:0101:0101"),
            new GeneralName(7, "0101:0101:0101:0101:0101:0101:0101:0101"
                            + "/0303:0303:0303:0303:0303:0303:0303:0303"),
        };

        // Names which should match divided from names which should not
        // match by null.
        // Restrictions apply only when the specified name form is present.
        // If no name of the type is in the certificate, the certificate
        // is acceptable (rfc 3280). This assertion is checked during processing
        // of null GeneralName object (it also serves as separator).
        GeneralName[][] alternative_names = new GeneralName[][] {
            {
                new GeneralName(1, "rfc@822.Name"),
                null,
                new GeneralName(1, "rfc@Other.Name")
            }, {
                new GeneralName(1, "rfc@822.Name"),
                null,
                new GeneralName(1, "rfc@Other.Name")
            }, {
                new GeneralName(2, "Name.org"),
                new GeneralName(2, "dNS.Name.org"),
                null,
                new GeneralName(2, "dNS.OtherName.org")
            }, {
                new GeneralName(2, "dNS.Name.org"),
                null,
                new GeneralName(2, "Name.org"),
                new GeneralName(2, "dNS.OtherName.org")
            }, {

                new GeneralName(6, "http://uniform.Resource.Id/location"),
                null,
                new GeneralName(6, "http://Resource.Id")
            }, {
                new GeneralName(6, "http://uniform.Resource.Id"),
                null,
                new GeneralName(6, "http://Resource.Id")
            }, {
                new GeneralName(new byte[] {1, 1, 1, 1}),
                null,
                new GeneralName(new byte[] {2, 2, 2, 2})
            }, {
                new GeneralName(new byte[] {1, 1, 1, 1}),
                new GeneralName(new byte[] {2, 2, 2, 2}),
                new GeneralName(new byte[] {3, 3, 3, 3}),
                null,
                new GeneralName(new byte[] {4, 4, 4, 4})
            }, {
                new GeneralName(new byte[] {1, 1, 1, 1, 1, 1, 1, 1,
                                            1, 1, 1, 1, 1, 1, 1, 1}),
                null,
                new GeneralName(new byte[] {2, 2, 2, 2, 2, 2, 2, 2,
                                            2, 2, 2, 2, 2, 2, 2, 2}),
            }, {
                new GeneralName(new byte[] {1, 1, 1, 1, 1, 1, 1, 1,
                                            1, 1, 1, 1, 1, 1, 1, 1}),
                new GeneralName(new byte[] {2, 2, 2, 2, 2, 2, 2, 2,
                                            2, 2, 2, 2, 2, 2, 2, 2}),
                new GeneralName(new byte[] {3, 3, 3, 3, 3, 3, 3, 3,
                                            3, 3, 3, 3, 3, 3, 3, 3}),
                null,
                new GeneralName(new byte[] {4, 4, 4, 4, 4, 4, 4, 4,
                                            4, 4, 4, 4, 4, 4, 4, 4}),
            }
        };

        X509CertSelector selector = new X509CertSelector();
        String subject = "O=Organization";
        X500Principal x500Subject = new X500Principal(subject);
        try {
            Name nameSubject = new Name(subject);
            for (int i=0; i<name_constraints.length; i++) {
                // make the subtrees (part of name constraints)
                // this subtrees will be used as permited and as excluded
                GeneralSubtree subtree =
                    new GeneralSubtree(name_constraints[i]);
                GeneralSubtrees subtrees = new GeneralSubtrees();
                NameConstraints constraints;
                subtrees.addSubtree(subtree);
                // start the checking for each alt. name corresponding
                // to current name_constraints[i]
                boolean check_matching = true;
                for (int j=0; j<alternative_names[i].length; j++) {
                    GeneralNames alt_names_extension = new GeneralNames();
                    if (alternative_names[i][j] == null) {
                        // double trick: turn the switch and check that the
                        // restrictions apply only when the specified name
                        // form is presented.  If no name of the type is in the
                        // certificate, the certificate is acceptable.
                        check_matching = false;
                    } else {
                        alt_names_extension.addName(alternative_names[i][j]);
                    }
                    TestCert certificate = new TestCert(alt_names_extension);
                    certificate.setSubject(x500Subject);
                    certificate.setEncoding(getCertEncoding(nameSubject,
                                                     alt_names_extension));
                    // first check if permited name match
                    constraints = new NameConstraints(subtrees, null);
                    selector.setNameConstraints(constraints.getEncoded());
                    boolean expected = check_matching
                                       || (alternative_names[i][j] == null);
                    assertTrue("The method match() for:\n        "
                               + alternative_names[i][j]
                               + "\nand permited name\n        "
                               + name_constraints[i]
                               + "\nshould return: "+expected,
                               selector.match(certificate) == expected);
                    // second check if excluded name does not match
                    constraints = (check_matching)
                                    // check for 'Any name matching a
                                    // restriction in the excludedSubtrees
                                    // field is invalid regardless of
                                    // information appearing in the
                                    // permittedSubtrees'.
                                    ? new NameConstraints(subtrees, subtrees)
                                    : new NameConstraints(null, subtrees);
                    selector.setNameConstraints(constraints.getEncoded());
                    expected = !check_matching
                               || (alternative_names[i][j] == null);
                    assertTrue("The method match() for:\n        "
                               + alternative_names[i][j]
                               + "\nand excluded name\n        "
                               + name_constraints[i]
                               + "\nshould return: "+expected,
                               selector.match(certificate) == expected);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * setNameConstraints(byte[] bytes) method testing.
     * Constructs the different NameConstraints DER structures with
     * GeneralNames of type 4 and checks if the different certificates
     * matches or does not.
     */
    public void testSetNameConstraints2() {
        // As specified in rfc 3280:
        //
        // Restrictions apply only when the specified name form is present.
        // If no name of the type is in the certificate,
        // the certificate is acceptable.
        //
        // Restrictions of the form directoryName MUST be applied to the
        // subject field in the certificate and to the subjectAltName
        // extensions of type directoryName.
        //
        // According to p. 4.1.2.4 comparing the encoded forms of the names.

        String[][] variants = new String[][] {
            //  subject    Alternative   Presented name     Absent name
            //   name         name       perm(t)/excl(f)  perm(f)/excl(t)
            {"O=Org",       "O=Org",        "O=Org",        "O=Org2"},
            {"O=Org",       "O=Org1",       "O=Org",        "O=Org2"},
            {"O=Org1",      "O=Org",        "O=Org",        "O=Org2"},
        };

        X509CertSelector selector = new X509CertSelector();
        try {
            for (int i=0; i<variants.length; i++) {
                // make the names objects
                X500Principal subject = new X500Principal(variants[i][0]);
                Name subject_name = new Name(variants[i][0]);
                GeneralName alt_name = new GeneralName(4, variants[i][1]);
                // make the certificate to be checked
                GeneralNames alt_names_extension = new GeneralNames();
                alt_names_extension.addName(alt_name);
                TestCert certificate = new TestCert(alt_names_extension);
                certificate.setSubject(subject);
                certificate.setEncoding(getCertEncoding(subject_name,
                                                 alt_names_extension));
                // make the subtrees (part of name constraints)
                // this subtrees will be used as permited and as excluded
                // name which is presented in certificate:
                GeneralSubtrees pos_subtrees = new GeneralSubtrees();
                pos_subtrees.addSubtree(
                        new GeneralSubtree(
                            new GeneralName(4, variants[i][2])));
                // name which is absent in certificate:
                GeneralSubtrees neg_subtrees = new GeneralSubtrees();
                neg_subtrees.addSubtree(
                        new GeneralSubtree(
                            new GeneralName(4, variants[i][3])));

                NameConstraints constraints;
                // Work with name which is presented in certificate
                // first check if certificate with permited name matches:
                constraints = new NameConstraints(pos_subtrees, null);
                selector.setNameConstraints(constraints.getEncoded());
                assertTrue("The method match() for certificate "
                           + "with subject:\n        "
                           + variants[i][0]
                           + "\nand with alternative name:\n        "
                           + variants[i][1]
                           + "\nand permited name\n        "
                           + variants[i][2]
                           + "\nshould return true",
                           selector.match(certificate));
                // second check if certificate with excluded name doesn't match:
                constraints = new NameConstraints(pos_subtrees, pos_subtrees);
                selector.setNameConstraints(constraints.getEncoded());
                assertTrue("The method match() for certificate "
                           + "with subject:\n        "
                           + variants[i][0]
                           + "\nand with alternative name:\n        "
                           + variants[i][1]
                           + "\nand excluded name\n        "
                           + variants[i][2]
                           + "\nshould return false",
                           !selector.match(certificate));
                // Work with name which is not presented in certificate
                // first check if the certificate without permited name
                // does not match:
                constraints = new NameConstraints(neg_subtrees, null);
                selector.setNameConstraints(constraints.getEncoded());
                assertTrue("The method match() for certificate "
                           + "with subject:\n        "
                           + variants[i][0]
                           + "\nand with alternative name:\n        "
                           + variants[i][1]
                           + "\nand permited name\n        "
                           + variants[i][3]
                           + "\nshould return false",
                           !selector.match(certificate));
                // second check if certificate without excluded name matches:
                constraints = new NameConstraints(neg_subtrees, neg_subtrees);
                selector.setNameConstraints(constraints.getEncoded());
                assertTrue("The method match() for certificate "
                           + "with subject:\n        "
                           + variants[i][0]
                           + "\nand with alternative name:\n        "
                           + variants[i][1]
                           + "\nand excluded name\n        "
                           + variants[i][3]
                           + "\nshould return false",
                           !selector.match(certificate));
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * Constructs the encoded form of certificate with specified subject field
     * of TBSCertificate and specified alternative names.
     */
    private byte[] getCertEncoding(Name subject, GeneralNames subjectAltNames)
                                                        throws IOException {
        // make the TBSCertificate for Certificate
        int version = 2; //v3
        BigInteger serialNumber = BigInteger.valueOf(555L);
        AlgorithmIdentifier signature = new AlgorithmIdentifier("1.2.3.44.555");
        Name issuer = new Name("O=Certificate Issuer");
        Validity validity = new Validity(new Date(100000000),
                                         new Date(200000000));
        SubjectPublicKeyInfo subjectPublicKeyInfo =
            new SubjectPublicKeyInfo(
                    new AlgorithmIdentifier("1.2.840.113549.1.1.2"),
                                            new byte[10]);
        boolean[] issuerUniqueID  = new boolean[]
                    {true, false, true, false, true, false, true, false};
        boolean[] subjectUniqueID = new boolean[]
                    {false, true, false, true, false, true, false, true};

        Extension extension = new Extension("2.5.29.17",
                                            true, subjectAltNames.getEncoded());
        Extensions extensions = new Extensions();
        extensions.addExtension(extension);

        TBSCertificate tbsCertificate = new TBSCertificate(version,
                serialNumber, signature, issuer, validity, subject,
                subjectPublicKeyInfo, issuerUniqueID, subjectUniqueID,
                extensions);

        // make the Certificate
        org.apache.harmony.security.x509.Certificate certificate =
                        new org.apache.harmony.security.x509.Certificate
                                    (tbsCertificate, signature, new byte[10]);

        return certificate.getEncoded();
    }

    /**
     * getNameConstraints() method testing.
     */
    public void testGetNameConstraints() {
    }

    /**
     * setBasicConstraints(int minMaxPathLen) method testing.
     */
    public void testSetBasicConstraints() {
        try {
            new X509CertSelector().setBasicConstraints(-3);
            fail("IllegalArgumentException should be thrown.");
        } catch (IllegalArgumentException e) {
        }

        int plen1 = 2;
        int plen2 = -1;
        TestCert cert_1 = new TestCert(plen1);
        TestCert cert_2 = new TestCert(plen2);
        X509CertSelector selector = new X509CertSelector();

        selector.setBasicConstraints(-1);
        assertTrue("Any certificate should match in the case of -1 "
                                                + "pathLen criteria.",
                    selector.match(cert_1) && selector.match(cert_2));
        selector.setBasicConstraints(plen1);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_1));
        assertFalse("The certificate should not match the selection criteria.",
                                                        selector.match(cert_2));
        selector.setBasicConstraints(plen2);
        assertTrue("The certificate should match the selection criteria.",
                                                        selector.match(cert_2));
    }

    /**
     * getBasicConstraints() method testing.
     */
    public void testGetBasicConstraints() {
        int plen1 = 2;
        int plen2 = -1;
        X509CertSelector selector = new X509CertSelector();

        assertEquals("Selector should return -1",
                                        selector.getBasicConstraints(), -1);
        selector.setBasicConstraints(plen1);
        assertEquals("The returned value should be equal to specified",
                    plen1, selector.getBasicConstraints());
        assertFalse("The returned value should differ",
                    plen2 == selector.getBasicConstraints());
    }

    /**
     * setPolicy(Set<String> certPolicySet) method testing.
     */
    public void testSetPolicy() {
        String[] policies_1 = new String[] {
            "0.0.0.0.0.0",
            "1.1.1.1.1.1",
        };
        String[] policies_2 = new String[] {
            "0.0.0.0.0.0",
            "1.1.1.1.1.1",
            "2.2.2.2.2.2"
        };
        String[] policies_3 = new String[] {
            "2.2.2.2.2.2"
        };
        String[] policies_4 = new String[] {};
        X509CertSelector selector = new X509CertSelector();
        HashSet set = new HashSet(Arrays.asList(policies_1));
        try {
            selector.setPolicy(set);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        TestCert cert_1 = new TestCert(policies_1);
        TestCert cert_2 = new TestCert(policies_2);
        TestCert cert_3 = new TestCert(policies_3);
        TestCert cert_4 = new TestCert(policies_4);
        assertTrue("The certificate should match the specified criteria",
                                                        selector.match(cert_1));
        assertTrue("The certificate should match the specified criteria",
                                                        selector.match(cert_2));
        assertFalse("The certificate should not match the specified criteria",
                                                        selector.match(cert_3));
        assertFalse("The certificate should not match the specified criteria",
                                                        selector.match(cert_4));
        set.add("2.2.2.2.2.2");
        assertFalse("The modification of the set should not cause the "
                    + "modification of internal object",
                                                        selector.match(cert_3));
        set = new HashSet();
        try {
            selector.setPolicy(set);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The certificate should match the specified criteria",
                                                        selector.match(cert_1));
        assertTrue("The certificate should match the specified criteria",
                                                        selector.match(cert_2));
        assertTrue("The certificate should match the specified criteria",
                                                        selector.match(cert_3));
        assertFalse("The certificate should not match the specified criteria",
                                                        selector.match(cert_4));
        set.add("2.2.2.2.2.2");
        try {
            selector.setPolicy(set);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertFalse("The certificate should not match the specified criteria",
                                                        selector.match(cert_1));
        assertTrue("The certificate should match the specified criteria",
                                                        selector.match(cert_2));
        assertTrue("The certificate should match the specified criteria",
                                                        selector.match(cert_3));
        assertFalse("The certificate should not match the specified criteria",
                                                        selector.match(cert_4));
    }

    /**
     * getPolicy() method testing.
     */
    public void testGetPolicy() {
        String[] policies = new String[] {
            "0.0.0.0.0.0",
            "1.1.1.1.1.1",
            "2.2.2.2.2.2"
        };
        X509CertSelector selector = new X509CertSelector();
        HashSet set = new HashSet(Arrays.asList(policies));
        try {
            selector.setPolicy(set);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        Set result = selector.getPolicy();
        try {
            result.remove(policies[0]);
            fail("An immutable set should be returned.");
        } catch (UnsupportedOperationException e) {
        }
        if (result.size() != 3) {
            fail("The size of returned set differs from specified.");
        }
        for (int i=0; i<policies.length; i++) {
            if (!result.contains(policies[i])) {
                fail("The set does not have specified policy.");
            }
        }
    }

    /**
     * setPathToNames(Collection<List<?>> names) method testing.
     */
    public void testSetPathToNames() {
        try {
            GeneralName[] names = new GeneralName[] {
                new GeneralName(1, "rfc@822.Name"),
                new GeneralName(1, "rfc@822.AnotherName"),
                new GeneralName(2, "dNSName"),
                new GeneralName(2, "AnotherdNSName"),
                new GeneralName(4, "O=Organization"),
                new GeneralName(4, "O=Another Organization"),
                new GeneralName(6, "http://uniform.Resource.Id"),
                new GeneralName(6, "http://another.uniform.Resource.Id"),
                new GeneralName(7, "1.1.1.1"),
                new GeneralName(7, "2.2.2.2")
            };

            X509CertSelector selector = new X509CertSelector();

            TestCert cert;
            GeneralSubtrees subtrees;
            NameConstraints constraints;
            for (int i=0; i<names.length; i+=2) {
                // Set up the pathToNames criterion
                ArrayList pathToNames = new ArrayList();
                pathToNames.add(names[i].getAsList());
                selector.setPathToNames(pathToNames);

                // Construct the subtrees without the current name
                subtrees = new GeneralSubtrees();
                for (int j=0; j<names.length; j++) {
                    if (i != j && i+1 != j) {
                        subtrees.addSubtree(new GeneralSubtree(names[j]));
                    }
                }
                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                subtrees.addSubtree(new GeneralSubtree(names[i+1]));

                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as a permitted name so method match() "
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as an excluded name but it does not "
                            + "contain this name as a permitted so match()"
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as an excluded name so method match() "
                            + "should return true", selector.match(cert));

                subtrees.addSubtree(new GeneralSubtree(names[i]));

                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as a permitted name so method match() "
                            + "should return true", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as an excluded name so method match() "
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as an excluded name so method match() "
                            + "should return false", selector.match(cert));

                pathToNames.clear();
                assertFalse("The modification of initialization parameter "
                            + "should not cause the modification of "
                            + "internal object ", selector.match(cert));
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * addPathToName(int type, String name) method testing.
     */
    public void testAddPathToName1() {
        try {
            int[] types = new int[] {1, 1, 2, 2, 4, 4, 6, 6, 7, 7};
            String[] names = new String[] {
                        "rfc@822.Name",
                        "rfc@822.AnotherName",
                        "dNSName",
                        "AnotherdNSName",
                        "O=Organization",
                        "O=Another Organization",
                        "http://uniform.Resource.Id",
                        "http://another.uniform.Resource.Id",
                        "1.1.1.1",
                        "2.2.2.2"
            };

            X509CertSelector selector = new X509CertSelector();

            TestCert cert;
            GeneralSubtrees subtrees;
            NameConstraints constraints;
            for (int i=0; i<names.length-2; i+=2) {
                // Set up the pathToNames criterion
                selector.addPathToName(types[i], names[i]);

                // Construct the subtrees without the current name
                subtrees = new GeneralSubtrees();
                for (int j=i+2; j<names.length; j++) {
                    if (i != j && i+1 != j) {
                        subtrees.addSubtree(
                                new GeneralSubtree(
                                    new GeneralName(types[j], names[j])));
                    }
                }
                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                subtrees.addSubtree(
                        new GeneralSubtree(
                            new GeneralName(types[i+1], names[i+1])));

                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as a permitted name so method match() "
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as an excluded name but it does not "
                            + "contain this name as a permitted so match()"
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as an excluded name so method match() "
                            + "should return true", selector.match(cert));

                subtrees.addSubtree(
                        new GeneralSubtree(
                            new GeneralName(types[i], names[i])));

                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as a permitted name so method match() "
                            + "should return true", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as an excluded name so method match() "
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as an excluded name so method match() "
                            + "should return false", selector.match(cert));
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * addPathToName(int type, byte[] name) method testing.
     */
    public void testAddPathToName2() {
        try {
            int[] types = new int[] {1, 1, 2, 2, 4, 4, 6, 6, 7, 7};
            byte[][] names = new byte[][] {
                new GeneralName(1, "rfc@822.Name").getEncodedName(),
                new GeneralName(1, "rfc@822.AnotherName").getEncodedName(),
                new GeneralName(2, "dNSName").getEncodedName(),
                new GeneralName(2, "AnotherdNSName").getEncodedName(),
                new GeneralName(4, "O=Organization").getEncodedName(),
                new GeneralName(4, "O=Another Organization").getEncodedName(),
                new GeneralName(6, "http://uniform.Resource.Id")
                                                            .getEncodedName(),
                new GeneralName(6, "http://another.uniform.Resource.Id")
                                                            .getEncodedName(),
                new GeneralName(7, "1.1.1.1").getEncodedName(),
                new GeneralName(7, "2.2.2.2").getEncodedName()
            };

            X509CertSelector selector = new X509CertSelector();

            TestCert cert;
            GeneralSubtrees subtrees;
            NameConstraints constraints;
            for (int i=0; i<names.length-2; i+=2) {
                // Set up the pathToNames criterion
                selector.addPathToName(types[i], names[i]);

                // Construct the subtrees without the current name
                subtrees = new GeneralSubtrees();
                for (int j=i+2; j<names.length; j++) {
                    if (i != j && i+1 != j) {
                        subtrees.addSubtree(
                                new GeneralSubtree(
                                    new GeneralName(types[j], names[j])));
                    }
                }
                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the names "
                            + "of such type so method match() should "
                            + "return true.", selector.match(cert));

                subtrees.addSubtree(
                        new GeneralSubtree(
                            new GeneralName(types[i+1], names[i+1])));

                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as a permitted name so method match() "
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as an excluded name but it does not "
                            + "contain this name as a permitted so match()"
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate does not contain the name "
                            + "as an excluded name so method match() "
                            + "should return true", selector.match(cert));

                subtrees.addSubtree(
                        new GeneralSubtree(
                            new GeneralName(types[i], names[i])));

                constraints = new NameConstraints(subtrees, null);
                cert = new TestCert(constraints);
                assertTrue("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as a permitted name so method match() "
                            + "should return true", selector.match(cert));

                constraints = new NameConstraints(subtrees, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as an excluded name so method match() "
                            + "should return false", selector.match(cert));

                constraints = new NameConstraints(null, subtrees);
                cert = new TestCert(constraints);
                assertFalse("The Name Constraints Extension of the "
                            + "certificate contains the name "
                            + "as an excluded name so method match() "
                            + "should return false", selector.match(cert));
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * getPathToNames() method testing.
     */
    public void testGetPathToNames() {
        try {
            byte[] encoding =
                new GeneralName(1, "rfc@822.Name").getEncodedName();

            X509CertSelector selector = new X509CertSelector();

            selector.addPathToName(1, encoding);
            encoding[0]++;
            Collection coll = selector.getPathToNames();
            Iterator it = coll.iterator();
            List list = (List) it.next();
            Object result = list.get(1);
            if ((result instanceof byte[])
                    && (encoding[0] == ((byte[])result)[0])) {
                fail("Deep copy should be performed on pathToNames.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
    }

    /**
     * toString() method testing.
     */
    public void testToString() throws Exception {
        BigInteger serial = new BigInteger("10000");
        X500Principal issuer = new X500Principal("O=Issuer Org.");
        X500Principal subject = new X500Principal("O=Subject Org.");
        byte[] subject_auth_KeyID = new byte[] {1, 2, 3, 4, 5};
        Date certValid = new Date(2000000000);
        Date[] privateKeyValid = new Date[] {
                new Date(100000000L),
                new Date(200000000L),
                new Date(300000000L)
        };
        String pkAlgID = "1.2.840.113549.1.1.4"; // MD5 with RSA encryption (source: http://asn1.elibel.tm.fr)
        PublicKey pkey;

        pkey = new TestKeyPair("RSA").getPublic();

        boolean[] keyUsage = new boolean[]
                    {true, true, true, true, true, true, true, true, false};
        // OID values was taken from rfc 3280
        HashSet extKeyUsage = new HashSet(Arrays.asList(new String[] {
                "1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2", "1.3.6.1.5.5.7.3.3",
                "1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.8", "1.3.6.1.5.5.7.3.9",
                "1.3.6.1.5.5.7.3.5", "1.3.6.1.5.5.7.3.6", "1.3.6.1.5.5.7.3.7"}
        ));
        GeneralNames subjectAltNames = new GeneralNames(Arrays.asList(
            new GeneralName[] {
                new GeneralName(1, "rfc@822.Name"),
                new GeneralName(2, "dNSName"),
                new GeneralName(6, "http://uniform.Resource.Id"),
                new GeneralName(7, "1.1.1.1")
            }
        ));
        String[] policies = new String[] {
            "0.0.0.0.0.0",
            "1.1.1.1.1.1",
        };
        TestCert cert = new TestCert("certificate equality criteria");

        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cert);
        selector.setSerialNumber(serial);
        selector.setIssuer(issuer);
        selector.setSubject(subject);
        selector.setSubjectKeyIdentifier(subject_auth_KeyID);
        selector.setAuthorityKeyIdentifier(subject_auth_KeyID);
        selector.setCertificateValid(certValid);
        selector.setPrivateKeyValid(privateKeyValid[1]);
        selector.setSubjectPublicKey(pkey);
        selector.setSubjectPublicKeyAlgID(pkAlgID);
        selector.setKeyUsage(keyUsage);
        selector.setExtendedKeyUsage(extKeyUsage);
        selector.setSubjectAlternativeNames(subjectAltNames.getPairsList());
        selector.setMatchAllSubjectAltNames(true);
        selector.setPolicy(new HashSet(Arrays.asList(policies)));

        assertNotNull("The result should not be null.",
                selector.toString());
    }

    /**
     * match(Certificate cert) method testing.
     * Tests if the null object matches to the selector or does not,
     * and if the certificate conforming to the multiple matching criteria
     * matches or does not..
     */
    public void testMatch() throws Exception {
        BigInteger serial = new BigInteger("10000");
        X500Principal issuer = new X500Principal("O=Issuer Org.");
        X500Principal subject = new X500Principal("O=Subject Org.");
        byte[] subject_auth_KeyID = new byte[] {1, 2, 3, 4, 5}; // random value
        Date certValid = new Date(2000000000);
        Date[] privateKeyValid = new Date[] {
                new Date(100000000L),
                new Date(200000000L),
                new Date(300000000L)
        };
        String pkAlgID = "1.2.840.113549.1.1.1"; // RSA encryption (source: http://asn1.elibel.tm.fr)
        PublicKey pkey;

        pkey = new TestKeyPair("RSA").getPublic();

        boolean[] keyUsage = new boolean[]
                    {true, true, true, true, true, true, true, true, false};
        // OID values was taken from rfc 3280
        HashSet extKeyUsage = new HashSet(Arrays.asList(new String[] {
                "1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2", "1.3.6.1.5.5.7.3.3",
                "1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.8", "1.3.6.1.5.5.7.3.9",
                "1.3.6.1.5.5.7.3.5", "1.3.6.1.5.5.7.3.6", "1.3.6.1.5.5.7.3.7"}
        ));
        GeneralNames subjectAltNames = new GeneralNames(Arrays.asList(
            new GeneralName[] {
                new GeneralName(1, "rfc@822.Name"),
                new GeneralName(2, "dNSName"),
                new GeneralName(6, "http://uniform.Resource.Id"),
                new GeneralName(7, "1.1.1.1")
            }
        ));
        String[] policies = new String[] {
            "0.0.0.0.0.0",
            "1.1.1.1.1.1",
        };

        TestCert cert = new TestCert("certificate equality criteria");
        cert.setSerialNumber(serial);
        cert.setIssuer(issuer);
        cert.setSubject(subject);
        cert.setKeyIdentifier(subject_auth_KeyID);
        cert.setDate(certValid);
        cert.setPeriod(privateKeyValid[0], privateKeyValid[2]);
        cert.setPublicKey(pkey);
        cert.setKeyUsage(keyUsage);
        cert.setExtendedKeyUsage(extKeyUsage);
        cert.setSubjectAlternativeNames(subjectAltNames);
        cert.setPolicies(policies);

        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cert);
        selector.setSerialNumber(serial);
        selector.setIssuer(issuer);
        selector.setSubject(subject);
        selector.setSubjectKeyIdentifier(subject_auth_KeyID);
        selector.setAuthorityKeyIdentifier(subject_auth_KeyID);
        selector.setCertificateValid(certValid);
        selector.setPrivateKeyValid(privateKeyValid[1]);
        selector.setSubjectPublicKey(pkey);
        selector.setSubjectPublicKeyAlgID(pkAlgID);
        selector.setKeyUsage(keyUsage);
        selector.setExtendedKeyUsage(extKeyUsage);
        selector.setSubjectAlternativeNames(subjectAltNames.getPairsList());
        selector.setMatchAllSubjectAltNames(true);
        selector.setPolicy(new HashSet(Arrays.asList(policies)));

        assertFalse("The null object should not match",
                                    selector.match((X509Certificate) null));
        assertTrue("The certificate should match the selector",
                                    selector.match(cert));
    }

    /**
     * @tests java.security.cert.X509CertSelector#match(java.security.cert.Certificate)
     */
    public void test_matchLjava_security_cert_Certificate() {

        // Regression for HARMONY-186
        TestCert cert = new TestCert();
        cert.setKeyUsage(new boolean[] { true, false, true, false, false,
                false, false, false, false });

        X509CertSelector certSelector = new X509CertSelector();

        certSelector.setKeyUsage(new boolean[] { true, false, true });
        assertTrue("Assert 1: ", certSelector.match(cert));

        certSelector.setKeyUsage(new boolean[] { true, true, true });
        assertFalse("Assert 2: ", certSelector.match(cert));
    }

    /**
     * clone() method testing.
     */
    public void testClone() throws Exception {
        BigInteger serial = new BigInteger("10000");
        X500Principal issuer = new X500Principal("O=Issuer Org.");
        X500Principal subject = new X500Principal("O=Subject Org.");
        byte[] subject_auth_KeyID = new byte[] {1, 2, 3, 4, 5}; // random value
        Date certValid = new Date(2000000000);
        Date[] privateKeyValid = new Date[] {
                new Date(100000000L),
                new Date(200000000L),
                new Date(300000000L)
        };
        String pkAlgID = "1.2.840.113549.1.1.1"; // RSA encryption (source: http://asn1.elibel.tm.fr)
        PublicKey pkey;

        pkey = new TestKeyPair("RSA").getPublic();

        boolean[] keyUsage = new boolean[]
                    {true, true, true, true, true, true, true, true, false};
        // OID values was taken from rfc 3280
        HashSet extKeyUsage = new HashSet(Arrays.asList(new String[] {
                "1.3.6.1.5.5.7.3.1", "1.3.6.1.5.5.7.3.2", "1.3.6.1.5.5.7.3.3",
                "1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.8", "1.3.6.1.5.5.7.3.9",
                "1.3.6.1.5.5.7.3.5", "1.3.6.1.5.5.7.3.6", "1.3.6.1.5.5.7.3.7"}
        ));
        GeneralNames subjectAltNames = new GeneralNames(Arrays.asList(
            new GeneralName[] {
                new GeneralName(1, "rfc@822.Name"),
                new GeneralName(2, "dNSName"),
                new GeneralName(6, "http://uniform.Resource.Id"),
                new GeneralName(7, "1.1.1.1")
            }
        ));
        String[] policies = new String[] {
            "0.0.0.0.0.0",
            "1.1.1.1.1.1",
        };

        TestCert cert = new TestCert("certificate equality criteria");
        cert.setSerialNumber(serial);
        cert.setIssuer(issuer);
        cert.setSubject(subject);
        cert.setKeyIdentifier(subject_auth_KeyID);
        cert.setDate(certValid);
        cert.setPeriod(privateKeyValid[0], privateKeyValid[2]);
        cert.setPublicKey(pkey);
        cert.setKeyUsage(keyUsage);
        cert.setExtendedKeyUsage(extKeyUsage);
        cert.setSubjectAlternativeNames(subjectAltNames);
        cert.setPolicies(policies);

        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cert);
        selector.setSerialNumber(serial);
        selector.setIssuer(issuer);
        selector.setSubject(subject);
        selector.setSubjectKeyIdentifier(subject_auth_KeyID);
        selector.setAuthorityKeyIdentifier(subject_auth_KeyID);
        selector.setCertificateValid(certValid);
        selector.setPrivateKeyValid(privateKeyValid[1]);
        selector.setSubjectPublicKey(pkey);
        selector.setSubjectPublicKeyAlgID(pkAlgID);
        selector.setKeyUsage(keyUsage);
        selector.setExtendedKeyUsage(extKeyUsage);
        selector.setSubjectAlternativeNames(subjectAltNames.getPairsList());
        selector.setMatchAllSubjectAltNames(true);
        selector.setPolicy(new HashSet(Arrays.asList(policies)));

        assertTrue("The certificate should match the selector",
                            ((X509CertSelector)selector.clone()).match(cert));
    }


    public static Test suite() {
        return new TestSuite(X509CertSelectorTest.class);
    }

}

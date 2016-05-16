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
import java.security.cert.CRLException;
import java.security.cert.X509CRLEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 */

public class X509CRLSelectorTest extends TestCase {

    /**
     * The abstract class stub implementation.
     */
    private class TestCRL extends X509CRL {

        private X500Principal principal = null;
        private BigInteger crlNumber = null;
        private Date thisUpdate = null;
        private Date nextUpdate = null;

        public TestCRL(X500Principal principal) {
            this.principal = principal;
        }

        public TestCRL(Date thisUpdate, Date nextUpdate) {
            setUpdateDates(thisUpdate, nextUpdate);
        }

        public TestCRL(BigInteger crlNumber) {
            setCrlNumber(crlNumber);
        }

        public void setUpdateDates(Date thisUpdate, Date nextUpdate) {
            this.thisUpdate = thisUpdate;
            this.nextUpdate = nextUpdate;
        }

        public void setCrlNumber(BigInteger crlNumber) {
            this.crlNumber = crlNumber;
        }

        public X500Principal getIssuerX500Principal() {
            return principal;
        }

        public String toString() {
            return null;
        }

        public boolean isRevoked(Certificate cert) {
            return true;
        }

        public Set getNonCriticalExtensionOIDs() {
            return null;
        }

        public Set getCriticalExtensionOIDs() {
            return null;
        }

        public byte[] getExtensionValue(String oid) {
            if ("2.5.29.20".equals(oid) && (crlNumber != null)) {
                return ASN1OctetString.getInstance().encode(
                        ASN1Integer.getInstance().encode(
                                crlNumber.toByteArray()));
            }
            return null;
        }

        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        public byte[] getEncoded() {
            return null;
        }

        public void verify(PublicKey key)
                 throws CRLException, NoSuchAlgorithmException,
                        InvalidKeyException, NoSuchProviderException,
                        SignatureException
        {
        }

        public void verify(PublicKey key, String sigProvider)
                 throws CRLException, NoSuchAlgorithmException,
                        InvalidKeyException, NoSuchProviderException,
                        SignatureException
        {
        }

        public int getVersion() {
            return 2;
        }

        public Principal getIssuerDN() {
            return null;
        }

        public Date getThisUpdate() {
            return thisUpdate;
        }

        public Date getNextUpdate() {
            return nextUpdate;
        }

        public X509CRLEntry getRevokedCertificate(BigInteger serialNumber) {
            return null;
        }

        public Set getRevokedCertificates() {
            return null;
        }

        public byte[] getTBSCertList() {
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
    }

    /**
     * setIssuers(Collection <X500Principal> issuers) method testing.
     * Tests if CRLs with any issuers match the selector in the case of
     * null issuerNames criteria, if specified issuers match the selector,
     * and if not specified issuer does not match the selector.
     */
    public void testSetIssuers() {
        X509CRLSelector selector = new X509CRLSelector();
        X500Principal iss1 = new X500Principal("O=First Org.");
        X500Principal iss2 = new X500Principal("O=Second Org.");
        X500Principal iss3 = new X500Principal("O=Third Org.");
        TestCRL crl1 = new TestCRL(iss1);
        TestCRL crl2 = new TestCRL(iss2);
        TestCRL crl3 = new TestCRL(iss3);

        selector.setIssuers(null);
        assertTrue("Any CRL issuers should match in the case of null issuers.",
                    selector.match(crl1) && selector.match(crl2));

        ArrayList issuers = new ArrayList(2);
        issuers.add(iss1);
        issuers.add(iss2);
        selector.setIssuers(issuers);
        assertTrue("The CRL should match the selection criteria.",
                    selector.match(crl1) && selector.match(crl2));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl3));
        issuers.add(iss3);
        assertFalse("The internal issuer collection is not protected "
                    + "against the modifications.", selector.match(crl3));
    }

    /**
     * setIssuerNames(Collection <?> names) method testing.
     * Tests if CRLs with any issuers match the selector in the case of
     * null issuerNames criteria, if specified issuers match the selector,
     * if not specified issuer does not match the selector, and if the
     * internal collection of issuer names is copied during initialization.
     */
    public void testSetIssuerNames() {
        X509CRLSelector selector = new X509CRLSelector();
        String iss1 = "O=First Org.";
        byte[] iss2 = new byte[]
            //manually obtained DER encoding of "O=Second Org." issuer name;
            {48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11,
            83, 101, 99, 111, 110, 100, 32, 79, 114, 103, 46};
        String iss3 = "O=Third Org.";
        TestCRL crl1 = new TestCRL(new X500Principal(iss1));
        TestCRL crl2 = new TestCRL(new X500Principal(iss2));
        TestCRL crl3 = new TestCRL(new X500Principal(iss3));

        try {
            selector.setIssuerNames(null);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("Any CRL issuers should match in the case of null issuers.",
                    selector.match(crl1) && selector.match(crl2));

        ArrayList issuers = new ArrayList(2);
        issuers.add(iss1);
        issuers.add(iss2);
        try {
            selector.setIssuerNames(issuers);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The CRL should match the selection criteria.",
                    selector.match(crl1) && selector.match(crl2));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl3));
        issuers.add(iss3);
        assertFalse("The internal issuer collection is not protected "
                    + "against the modifications.", selector.match(crl3));
    }

    /**
     * addIssuer(X500Principal issuer) method testing.
     * Tests if CRLs with specified issuers match the selector,
     * and if not specified issuer does not match the selector.
     */
    public void testAddIssuer() {
        X509CRLSelector selector = new X509CRLSelector();
        X500Principal iss1 = new X500Principal("O=First Org.");
        X500Principal iss2 = new X500Principal("O=Second Org.");
        TestCRL crl1 = new TestCRL(iss1);
        TestCRL crl2 = new TestCRL(iss2);

        selector.addIssuer(iss1);
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl1));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl2));
        selector.addIssuer(iss2);
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl2));
    }

    /**
     * addIssuerName(String name) method testing.
     * Tests if CRLs with specified issuers match the selector,
     * and if not specified issuer does not match the selector.
     */
    public void testAddIssuerName1() {
        X509CRLSelector selector = new X509CRLSelector();
        String iss1 = "O=First Org.";
        String iss2 = "O=Second Org.";
        TestCRL crl1 = new TestCRL(new X500Principal(iss1));
        TestCRL crl2 = new TestCRL(new X500Principal(iss2));

        try {
            selector.addIssuerName(iss1);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl1));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl2));
        try {
            selector.addIssuerName(iss2);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl2));
    }

    /**
     * addIssuerName(byte[] name) method testing.
     * Tests if CRLs with specified issuers match the selector,
     * and if not specified issuer does not match the selector.
     */
    public void testAddIssuerName2() {
        X509CRLSelector selector = new X509CRLSelector();
        byte[] iss1 = new byte[]
            //manually obtained DER encoding of "O=First Org." issuer name;
            {48, 21, 49, 19, 48, 17, 6, 3, 85, 4, 10, 19, 10,
                70, 105, 114, 115, 116, 32, 79, 114, 103, 46};
        byte[] iss2 = new byte[]
            //manually obtained DER encoding of "O=Second Org." issuer name;
            {48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11,
            83, 101, 99, 111, 110, 100, 32, 79, 114, 103, 46};
        TestCRL crl1 = new TestCRL(new X500Principal(iss1));
        TestCRL crl2 = new TestCRL(new X500Principal(iss2));

        try {
            selector.addIssuerName(iss1);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl1));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl2));
        try {
            selector.addIssuerName(iss2);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl2));
    }

    /**
     * setMinCRLNumber(BigInteger minCRL) method testing.
     * Tests if CRLs with any crl number value match the selector in the case of
     * null crlNumber criteria, if specified minCRL value matches the selector,
     * and if CRL with inappropriate crlNumber value does not match the selector.
     */
    public void testSetMinCRLNumber() {
        X509CRLSelector selector = new X509CRLSelector();
        BigInteger minCRL = new BigInteger("10000");
        TestCRL crl = new TestCRL(minCRL);

        selector.setMinCRLNumber(null);
        assertTrue("Any CRL should match in the case of null minCRLNumber.",
                                            selector.match(crl));
        selector.setMinCRLNumber(minCRL);
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl));
        selector.setMinCRLNumber(new BigInteger("10001"));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl));
    }

    /**
     * setMaxCRLNumber(BigInteger maxCRL) method testing.
     * Tests if CRLs with any crl number value match the selector in the case of
     * null crlNumber criteria, if specified maxCRL value matches the selector,
     * and if CRL with inappropriate crlNumber value does not match the selector.
     */
    public void testSetMaxCRLNumber() {
        X509CRLSelector selector = new X509CRLSelector();
        BigInteger maxCRL = new BigInteger("10000");
        TestCRL crl = new TestCRL(maxCRL);

        selector.setMaxCRLNumber(null);
        assertTrue("Any CRL should match in the case of null minCRLNumber.",
                                            selector.match(crl));
        selector.setMaxCRLNumber(maxCRL);
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl));
        selector.setMaxCRLNumber(new BigInteger("9999"));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl));
    }

    /**
     * setDateAndTime(Date dateAndTime) method testing.
     * Tests if CRLs with any update dates match the selector in the case of
     * null dateAndTime criteria, if correct dates match and incorrect
     * do not match the selector.
     */
    public void testSetDateAndTime() {
        X509CRLSelector selector = new X509CRLSelector();
        TestCRL crl = new TestCRL(new Date(200), new Date(300));
        selector.setDateAndTime(null);
        assertTrue("Any CRL should match in the case of null dateAndTime.",
                                            selector.match(crl));
        selector.setDateAndTime(new Date(200));
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl));
        selector.setDateAndTime(new Date(250));
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl));
        selector.setDateAndTime(new Date(300));
        assertTrue("The CRL should match the selection criteria.",
                                            selector.match(crl));
        selector.setDateAndTime(new Date(150));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl));
        selector.setDateAndTime(new Date(350));
        assertFalse("The CRL should not match the selection criteria.",
                                            selector.match(crl));
    }

    /**
     * getIssuers() method testing.
     * Tests if the method return null in the case of not specified issuers,
     * if the returned collection corresponds to the specified issuers and
     * this collection is unmodifiable.
     */
    public void testGetIssuers() throws Exception {
        X509CRLSelector selector = new X509CRLSelector();
        X500Principal iss1 = new X500Principal("O=First Org.");
        X500Principal iss2 = new X500Principal("O=Second Org.");
        X500Principal iss3 = new X500Principal("O=Third Org.");
        String iss_name_1 = "O=First String DN";
        String iss_name_2 = "O=Second String DN";
        String iss_name_3 = "O=Third String DN";
        assertNull("The collection should be null.",
                                        selector.getIssuers());
        selector.addIssuerName(iss_name_1);
        selector.addIssuer(iss1);
        selector.addIssuerName(iss_name_2);
        selector.addIssuer(iss2);
        selector.addIssuerName(iss_name_3);

        Collection result = selector.getIssuers();
        assertEquals("Size does not correspond to expected",
                5, result.size());
        try {
            result.add(iss3);
            fail("The returned collection should be unmodifiable.");
        } catch (UnsupportedOperationException e) {
        }
        assertTrue("The collection should contain the specified DN.",
                                            result.contains(iss1));
        assertTrue("The collection should contain the specified DN.",
                                            result.contains(iss2));
        assertTrue("The collection should contain the specified DN.",
                        result.contains(new X500Principal(iss_name_1)));
        assertTrue("The collection should contain the specified DN.",
                        result.contains(new X500Principal(iss_name_2)));
        selector.addIssuer(iss3);
        assertTrue("The collection should contain the specified DN.",
                                            result.contains(iss3));
    }

    /**
     * getIssuerNames() method testing.
     * Tests if the method return null in the case of not specified issuers,
     * if the returned collection corresponds to the specified issuers.
     */
    public void testGetIssuerNames() {
        X509CRLSelector selector = new X509CRLSelector();
        byte[] iss1 = new byte[]
            //manually obtained DER encoding of "O=First Org." issuer name;
            {48, 21, 49, 19, 48, 17, 6, 3, 85, 4, 10, 19, 10,
                70, 105, 114, 115, 116, 32, 79, 114, 103, 46};
        byte[] iss2 = new byte[]
            //manually obtained DER encoding of "O=Second Org." issuer name;
            {48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11,
            83, 101, 99, 111, 110, 100, 32, 79, 114, 103, 46};
        assertNull("The collection should be null.",
                                        selector.getIssuerNames());
        try {
            selector.addIssuerName(iss1);
            selector.addIssuerName(iss2);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected IOException was thrown.");
        }
        Collection result = selector.getIssuerNames();
        assertEquals("The collection should contain all of the specified DNs.",
                                                2, result.size());
    }

    /**
     * getMinCRL() method testing.
     * Tests if the method return null in the case of not specified minCRL
     * criteria, and if the returned value corresponds to the specified one.
     */
    public void testGetMinCRL() {
        X509CRLSelector selector = new X509CRLSelector();
        assertNull("Initially the minCRL should be null.",
                                        selector.getMinCRL());
        BigInteger minCRL = new BigInteger("10000");
        selector.setMinCRLNumber(minCRL);
        assertTrue("The result should be equal to specified.",
                                        minCRL.equals(selector.getMinCRL()));
    }

    /**
     * getMaxCRL() method testing.
     * Tests if the method return null in the case of not specified maxCRL
     * criteria, and if the returned value corresponds to the specified one.
     */
    public void testGetMaxCRL() {
        X509CRLSelector selector = new X509CRLSelector();
        assertNull("Initially the maxCRL should be null.",
                                        selector.getMaxCRL());
        BigInteger maxCRL = new BigInteger("10000");
        selector.setMaxCRLNumber(maxCRL);
        assertTrue("The result should be equal to specified.",
                                        maxCRL.equals(selector.getMaxCRL()));
    }

    /**
     * getDateAndTime() method testing.
     * Tests if the method return null in the case of not specified dateAndTime
     * criteria, and if the returned value corresponds to the specified one.
     */
    public void testGetDateAndTime() {
        X509CRLSelector selector = new X509CRLSelector();
        assertNull("Initially the dateAndTime criteria should be null.",
                                        selector.getDateAndTime());
        Date date = new Date(200);
        selector.setDateAndTime(date);
        assertTrue("The result should be equal to specified.",
                                        date.equals(selector.getDateAndTime()));
    }

    /**
     * match(CRL crl) method testing.
     * Tests if the null object matches to the selector or not.
     */
    public void testMatch() {
        X509CRLSelector selector = new X509CRLSelector();
        assertFalse("The null object should not match",
                                        selector.match((X509CRL) null));
    }

    /**
     * clone() method testing.
     * Tests if the selector is cloned correctly: the crl which matche to
     * the initial selector should match to the clone and the change of clone
     * should not cause the change of initial selector.
     */
    public void testClone() {
        X509CRLSelector selector = new X509CRLSelector();
        X500Principal iss1 = new X500Principal("O=First Org.");
        X500Principal iss2 = new X500Principal("O=Second Org.");
        X500Principal iss3 = new X500Principal("O=Third Org.");
        BigInteger minCRL = new BigInteger("10000");
        BigInteger maxCRL = new BigInteger("10000");
        Date date = new Date(200);

        selector.addIssuer(iss1);
        selector.addIssuer(iss2);
        selector.setMinCRLNumber(minCRL);
        selector.setMaxCRLNumber(maxCRL);
        selector.setDateAndTime(date);

        X509CRLSelector clone = (X509CRLSelector) selector.clone();
        TestCRL crl = new TestCRL(iss1);
        crl.setCrlNumber(minCRL);
        crl.setUpdateDates(new Date(200), new Date(200));
        assertTrue("The specified CRL should match the clone selector.",
                    selector.match(crl));

        clone.addIssuer(iss3);
        assertFalse("The changes of the clone selector should not cause "
                    + "the changes of initial object",
                                    selector.getIssuerNames().size() == 3);
    }

    public void testToString() {
        X509CRLSelector selector = new X509CRLSelector();
        X500Principal iss1 = new X500Principal("O=First Org.");
        X500Principal iss2 = new X500Principal("O=Second Org.");
        BigInteger minCRL = new BigInteger("10000");
        BigInteger maxCRL = new BigInteger("10000");
        Date date = new Date(200);

        selector.addIssuer(iss1);
        selector.addIssuer(iss2);
        selector.setMinCRLNumber(minCRL);
        selector.setMaxCRLNumber(maxCRL);
        selector.setDateAndTime(date);

        assertNotNull("The result should not be null.", selector.toString());
    }

    public static Test suite() {
        return new TestSuite(X509CRLSelectorTest.class);
    }

}

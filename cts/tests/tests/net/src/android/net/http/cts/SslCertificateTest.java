/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.http.cts;

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
import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

import junit.framework.TestCase;
import android.net.http.SslCertificate;
import android.net.http.SslCertificate.DName;
import android.os.Bundle;

public class SslCertificateTest extends TestCase {

    public void testConstructor() {
        // new the SslCertificate instance
        String date = DateFormat.getInstance().format(new Date());
        new SslCertificate("c=129", "e=weji", date, date);

        // new the SslCertificate instance
        new SslCertificate(new MockX509Certificate());

    }

    class MockX509Certificate extends X509Certificate {

        @Override
        public void checkValidity() throws CertificateExpiredException,
                CertificateNotYetValidException {
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException,
                CertificateNotYetValidException {
        }

        @Override
        public int getBasicConstraints() {
            return 0;
        }

        @Override
        public Principal getIssuerDN() {
            return new MockPrincipal();
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return null;
        }

        @Override
        public boolean[] getKeyUsage() {
            return null;
        }

        @Override
        public Date getNotAfter() {
            return new Date(System.currentTimeMillis());
        }

        @Override
        public Date getNotBefore() {
            return new Date(System.currentTimeMillis() - 1000);
        }

        @Override
        public BigInteger getSerialNumber() {
            return null;
        }

        @Override
        public String getSigAlgName() {
            return null;
        }

        @Override
        public String getSigAlgOID() {
            return null;
        }

        @Override
        public byte[] getSigAlgParams() {
            return null;
        }

        @Override
        public byte[] getSignature() {
            return null;
        }

        @Override
        public Principal getSubjectDN() {
            return new MockPrincipal();
        }

        class MockPrincipal implements Principal {
            public String getName() {
                return null;
            }
        }
        @Override
        public boolean[] getSubjectUniqueID() {
            return null;
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return null;
        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return null;
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException, SignatureException {
        }

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException,
                NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
                SignatureException {
        }

        public Set<String> getCriticalExtensionOIDs() {
            return null;
        }

        public byte[] getExtensionValue(String oid) {
            return null;
        }

        public Set<String> getNonCriticalExtensionOIDs() {
            return null;
        }

        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }
    }

    public void testState() {
        // set the expected value

        Date date1 = new Date(System.currentTimeMillis() - 1000);
        Date date2 = new Date(System.currentTimeMillis());
        SslCertificate ssl = new SslCertificate("c=129", "e=weji", date1, date2);
        Bundle saved = SslCertificate.saveState(ssl);
        assertTrue(saved.size() == 4);

        assertNotNull(saved.getString("issued-to"));
        assertNotNull(saved.getString("issued-by"));
        assertNotNull(saved.getString("valid-not-before"));
        assertNotNull(saved.getString("valid-not-after"));
        assertNull(SslCertificate.saveState(null));

        SslCertificate restored = SslCertificate.restoreState(saved);
        assertEquals(ssl.getValidNotAfter(), restored.getValidNotAfter());
        assertEquals(ssl.getValidNotBefore(), restored.getValidNotBefore());
    }

    public void testSslCertificate() {

        final String TO = "c=ccc,o=testOName,ou=testUName,cn=testCName";
        final String BY = "e=aeei,c=adb,o=testOName,ou=testUName,cn=testCName";
        // new the SslCertificate instance
        Date date1 = new Date(System.currentTimeMillis() - 1000);
        Date date2 = new Date(System.currentTimeMillis());
        SslCertificate ssl = new SslCertificate(TO, BY, date1, date2);
        DName issuedTo = ssl.getIssuedTo();
        DName issuedBy = ssl.getIssuedBy();

        assertEquals("testCName", issuedTo.getCName());
        assertEquals(TO, issuedTo.getDName());
        assertEquals("testOName", issuedTo.getOName());
        assertEquals("testUName", issuedTo.getUName());

        assertEquals("testCName", issuedBy.getCName());
        assertEquals(BY, issuedBy.getDName());
        assertEquals("testOName", issuedBy.getOName());
        assertEquals("testUName", issuedBy.getUName());

        assertEquals(date1, ssl.getValidNotBeforeDate());
        assertEquals(date2, ssl.getValidNotAfterDate());
        final String EXPECTED = "Issued to: c=ccc,o=testOName,ou=testUName,cn=testCName;\n"
            + "Issued by: e=aeei,c=adb,o=testOName,ou=testUName,cn=testCName;\n";
        assertEquals(EXPECTED, ssl.toString());
    }

}

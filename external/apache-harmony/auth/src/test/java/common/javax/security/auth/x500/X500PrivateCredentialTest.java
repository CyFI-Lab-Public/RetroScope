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
* @author Alexander V. Esin
*/

package javax.security.auth.x500;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

import junit.framework.TestCase;
/**
 * Tests implementation of X500PrivateCredential class
 */
@SuppressWarnings("serial")
public class X500PrivateCredentialTest extends TestCase {

    X509Certificate cert= new X509Certificate() {
        @Override
        public void checkValidity(){}
        @Override
        public void checkValidity(Date date){}
        @Override
        public int getVersion() {
            return 0;
        }
        @Override
        public BigInteger getSerialNumber() {
            return null;
        }
        @Override
        public Principal getIssuerDN() {
            return null;
        }
        @Override
        public Principal getSubjectDN() {
            return null;
        }
        @Override
        public Date getNotBefore() {
            return null;
        }
        @Override
        public Date getNotAfter() {
            return null;
        }
        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return null;
        }
        @Override
        public byte[] getSignature() {
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
        public boolean[] getIssuerUniqueID() {
            return null;
        }
        @Override
        public boolean[] getSubjectUniqueID() {
            return null;
        }
        @Override
        public boolean[] getKeyUsage() {
            return null;
        }
        @Override
        public int getBasicConstraints() {
            return 0;
        }
        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return null;
        }
        @Override
        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        }
        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        }
        @Override
        public String toString() {
            return null;
        }
        @Override
        public PublicKey getPublicKey() {
            return null;
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
    };
    
    PrivateKey key= new PrivateKey() {
        public String getAlgorithm() {
            return null;
        }

        public String getFormat() {
            return null;
        }

        public byte[] getEncoded() {
            return null;
        }
        
    };
    public void testGetCert() {
        X500PrivateCredential cred= new X500PrivateCredential(cert, key);
        X509Certificate c= cred.getCertificate();
        assertNotNull(c);
    }
    
    public void testGetKey() {
        X500PrivateCredential cred= new X500PrivateCredential(cert, key);
        PrivateKey k= cred.getPrivateKey();
        assertNotNull(k);
    }
    
    public void testIsDestroyed() {
        X500PrivateCredential cred= new X500PrivateCredential(cert, key);
        cred.destroy();
        assertTrue(cred.isDestroyed());
    }
    
    public void testDestroy() {
        X500PrivateCredential cred= new X500PrivateCredential(cert, key);
        cred.destroy();
        String al= cred.getAlias();
        assertNull(al);
    }
    
    public void testGetAlias() {
        X500PrivateCredential cred= new X500PrivateCredential(cert, key, "ALIAS");
        String al= cred.getAlias();
        assertEquals("ALIAS", al);
    }
    
    public void testIllegalArg() {
        try {
            new X500PrivateCredential(cert, key, null);
            fail("No IllegalArgumentException on null value");
        }
        catch(IllegalArgumentException e) {
            //ignore
        }
    }

    public void testIllegalArg_0() {
        try {
            new X500PrivateCredential(cert, null, null);
            fail("No IllegalArgumentException on null value");
        }
        catch(IllegalArgumentException e) {
            //ignore
        }
    }

    public void testIllegalArg_1() {
        try {
            new X500PrivateCredential(null, key, "");
            fail("No IllegalArgumentException on null value");
        }
        catch(IllegalArgumentException e) {
            //ignore
        }
    }
}

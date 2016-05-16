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
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.harmony.security.provider.cert.X509CertPathImpl;

/**
 * X509CertPathImplTest
 */
public class X509CertPathImplTest extends TestCase {

    private X509Certificate certificate;
    {
        try {
            X509CertImplTest test = new X509CertImplTest();
            test.setUp();
            certificate = test.certificate;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private X509CertPathImpl certPath;
    private List certList;

    protected void setUp() throws java.lang.Exception {
        certList = new ArrayList();
        for (int i=0; i<2; i++) {
            certList.add(certificate);
        }
        certPath = new X509CertPathImpl(certList);
    }

    /**
     * @tests org.apache.harmony.security.provider.cert.X509CertPathImpl.X509CertPathImpl(List)
     */
    public void test_X509CertPathImpl_List() throws Exception {
        assertEquals("Certificate list size missmatch",
                certList.size(), certPath.getCertificates().size());
    }

    /**
     * @tests org.apache.harmony.security.provider.cert.X509CertPathImpl.getInstance(InputStream)
     */
    public void test_getInstance_InputStream() throws Exception {
        byte[] encoding = certPath.getEncoded();
        ByteArrayInputStream bais = new ByteArrayInputStream(encoding);
        X509CertPathImpl cpath = X509CertPathImpl.getInstance(bais);
        assertEquals("Certificate list size missmatch", certList.size(), cpath
                .getCertificates().size());
    }

    /**
     * @tests org.apache.harmony.security.provider.cert.X509CertPathImpl.getInstance(byte[])
     */
    public void test_getInstance_$B() throws Exception {
        byte[] encoding = certPath.getEncoded();
        X509CertPathImpl cpath = X509CertPathImpl.getInstance(encoding);
        assertEquals("Certificate list size missmatch", certList.size(), cpath
                .getCertificates().size());
    }

    /**
     * @tests org.apache.harmony.security.provider.cert.X509CertPathImpl.getInstance(byte[], String)
     */
    public void test_getInstance$BLjava_lang_String() throws Exception {

        // Test: getInstance(byte[] in, "PKCS7")
        // reconverting of the encoded form: from default (PkiPath) to PKCS7
        byte[] encoding = certPath.getEncoded();

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream bais = new ByteArrayInputStream(encoding);

        CertPath cert_path = factory.generateCertPath(bais);

        encoding = cert_path.getEncoded("PKCS7");

        X509CertPathImpl cpath = X509CertPathImpl
                .getInstance(encoding, "PKCS7");
        assertEquals("Certificate list size missmatch", certList.size(), cpath
                .getCertificates().size());

        bais = new ByteArrayInputStream(encoding);

        cpath = X509CertPathImpl.getInstance(bais, "PKCS7");
        assertEquals("Certificate list size missmatch", certList.size(), cpath
                .getCertificates().size());
    }

    /**
     * @tests org.apache.harmony.security.provider.cert.X509CertPathImpl.getCertificates()
     */
    public void test_getCertificates() throws Exception {
        try {
            byte[] encoding = certPath.getEncoded();
            X509CertPathImpl cpath = X509CertPathImpl.getInstance(encoding);
            assertEquals("Certificate list size missmatch", certList.size(),
                    cpath.getCertificates().size());
            cpath.getCertificates().remove(0);
            fail("UnsupportedOperationException should be thrown");
        } catch (UnsupportedOperationException e) {
            //pass
        }
    }

    /**
     * getEncoded() method testing.
     */
    public void testGetEncoded1() throws Exception {
        certPath.getEncoded();
    }

    /**
     * getEncoded(String encoding) method testing.
     */
    public void testGetEncoded2() {
        try {
            certPath.getEncoded("ABRACADABRA");
            fail("CertificateEncodingException should be thrown");
        } catch (CertificateEncodingException e) {
        }
    }

    /**
     * getEncodings() method testing.
     */
    public void testGetEncodings() {
        try {
            Iterator it = certPath.getEncodings();
            Object encoding  = it.next();
            assertNotNull("Default encodings should not be null", encoding);
            it.remove();
            fail("UnsupportedOperationException should be thrown");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    public static Test suite() {
        return new TestSuite(X509CertPathImplTest.class);
    }

}

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
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.harmony.security.provider.cert.X509CertFactoryImpl;
import org.apache.harmony.security.tests.support.provider.cert.CertFactoryTestData;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @com.intel.drl.spec_ref
 */
public class X509CertFactoryImplTest extends TestCase {


    /**
     * engineGenerateCertificate(InputStream inStream) method testing.
     */
    public void testEngineGenerateCertificate() throws Exception {
        X509CertFactoryImpl certFactory = new X509CertFactoryImpl();
        Certificate cert;

        // DER encoded certificate generation testing
        ByteArrayInputStream bais =
            new ByteArrayInputStream(
                    CertFactoryTestData.getCertEncoding());
        cert = certFactory.engineGenerateCertificate(bais);
        assertNotNull("First generated certificate is null", cert);
        cert = certFactory.engineGenerateCertificate(bais);
        assertNotNull("Second generated certificate is null", cert);

        try {
            certFactory.engineGenerateCertificate(bais);
            fail("Expected CertificateException was not thrown.");
        } catch (CertificateException e) {
        }

        // Base64 testing
        bais = new ByteArrayInputStream(
                CertFactoryTestData.getBase64CertEncoding());
        cert = certFactory.engineGenerateCertificate(bais);
        assertNotNull("First generated certificate is null", cert);
        cert = certFactory.engineGenerateCertificate(bais);
        assertNotNull("Second generated certificate is null", cert);

        try {
            certFactory.engineGenerateCertificate(bais);
            fail("Expected CertificateException was not thrown.");
        } catch (CertificateException e) {
        }
    }

    /**
     * engineGenerateCertificates(InputStream inStream) method testing.
     */
    public void testEngineGenerateCertificates() throws Exception {
        X509CertFactoryImpl certFactory = new X509CertFactoryImpl();

        // DER encoded certificate generation testing
        ByteArrayInputStream bais =
            new ByteArrayInputStream(
                    CertFactoryTestData.getCertEncoding());
        assertEquals("The size of collection is not correct", 2, certFactory
                .engineGenerateCertificates(bais).size());

        // Base64 testing
        bais = new ByteArrayInputStream(
                CertFactoryTestData.getBase64CertEncoding());
        assertEquals("The size of collection is not correct", 2, certFactory
                .engineGenerateCertificates(bais).size());
    }

    /**
     * engineGenerateCRL(InputStream inStream) method testing.
     */
    public void testEngineGenerateCRL() throws Exception {
        X509CertFactoryImpl certFactory = new X509CertFactoryImpl();
        CRL crl;

        // DER encoded crt generation testing
        ByteArrayInputStream bais =
            new ByteArrayInputStream(
                    CertFactoryTestData.getCRLEncoding());
        crl = certFactory.engineGenerateCRL(bais);
        assertNotNull("First generated CRL is null", crl);
        crl = certFactory.engineGenerateCRL(bais);
        assertNotNull("Second generated CRL is null", crl);

        try {
            certFactory.engineGenerateCRL(bais);
            fail("Expected CRLException was not thrown.");
        } catch (CRLException e) {
        }

        // Base64 testing
        bais = new ByteArrayInputStream(CertFactoryTestData
                .getBase64CRLEncoding());

        crl = certFactory.engineGenerateCRL(bais);
        assertNotNull("First generated CRL is null", crl);
        crl = certFactory.engineGenerateCRL(bais);
        assertNotNull("Second generated CRL is null", crl);

        try {
            certFactory.engineGenerateCRL(bais);
            fail("Expected CRLException was not thrown.");
        } catch (CRLException e) {
        }
    }

    /**
     * engineGenerateCRLs(InputStream inStream) method testing.
     */
    public void testEngineGenerateCRLs() throws Exception {
        X509CertFactoryImpl certFactory = new X509CertFactoryImpl();

        // DER encoded crt generation testing
        ByteArrayInputStream bais =
            new ByteArrayInputStream(
                    CertFactoryTestData.getCRLEncoding());
        assertEquals("The size of collection is not correct", 2, certFactory
                .engineGenerateCRLs(bais).size());

        // Base64 testing
        bais = new ByteArrayInputStream(CertFactoryTestData
                .getBase64CRLEncoding());
        assertEquals("The size of collection is not correct", 2, certFactory
                .engineGenerateCRLs(bais).size());
    }

    /**
     * engineGenerateCertPath(InputStream inStream) method testing.
     */
    public void testEngineGenerateCertPath() throws Exception {
        X509CertFactoryImpl certFactory = new X509CertFactoryImpl();
        ByteArrayInputStream bais =
                new ByteArrayInputStream(
                        CertFactoryTestData.getCertPathPkiPathEncoding());
        certFactory.engineGenerateCertPath(bais);

        try {
            certFactory.engineGenerateCertPath(bais);
            fail("Expected CertificateException was not thrown.");
        } catch (CertificateException e) {
        }
    }

    /**
     * engineGenerateCertPath(InputStream inStream, String encoding) method
     * testing.
     */
    public void testEngineGenerateCertPath1() throws Exception {
        X509CertFactoryImpl certFactory = new X509CertFactoryImpl();
        ByteArrayInputStream bais =
                new ByteArrayInputStream(
                        CertFactoryTestData.getCertPathPKCS7Encoding());
        certFactory.engineGenerateCertPath(bais, "PKCS7");

        try {
            certFactory.engineGenerateCertPath(bais, "PKCS7");
            fail("Expected CertificateException was not thrown.");
        } catch (CertificateException e) {
        }
    }

    /**
     * engineGenerateCertPath(List certificates) method testing.
     */
    public void testEngineGenerateCertPath2() throws Exception {

        X509CertImplTest test = new X509CertImplTest();
        test.setUp();
        X509Certificate certificate = test.certificate;

        ArrayList certList = new ArrayList();
        for (int i=0; i<2; i++) {
            certList.add(certificate);
        }

        X509CertFactoryImpl certFactory = new X509CertFactoryImpl();
        certFactory.engineGenerateCertPath(certList);

        certList.add(new Integer(5));
        try {
            certFactory.engineGenerateCertPath(certList);
            fail("Expected CertificateException was not thrown.");
        } catch (CertificateException e) {
        }
    }

    /**
     * engineGetCertPathEncodings() method testing.
     */
    public void testEngineGetCertPathEncodings() {
        try {
            Iterator it =
                new X509CertFactoryImpl().engineGetCertPathEncodings();
            Object encoding  = it.next();
            assertNotNull("Default encodings should not be null", encoding);
            it.remove();
            fail("UnsupportedOperationException should be thrown");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    public static Test suite() {
        return new TestSuite(X509CertFactoryImplTest.class);
    }

}

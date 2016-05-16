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
* @author Vladimir N. Molotkov
*/

package org.apache.harmony.security.tests.java.security.cert.serialization;

import java.io.ByteArrayInputStream;
import java.io.ObjectStreamException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.cert.MyCertificate;
import org.apache.harmony.security.tests.support.cert.TestUtils;
import org.apache.harmony.testframework.serialization.SerializationTest;

/**
 * Tests for <code>Certificate</code> serialization
 */
public class CertificateTest extends TestCase {

    // certificate type to be created during testing
    private static final String certType = "X.509";

    /**
     * @tests serialization/deserialization.
     */
    public void testSerializationSelf() throws Exception {

        CertificateFactory cf = CertificateFactory.getInstance(certType);

        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(
                TestUtils.getEncodedX509Certificate()));

        SerializationTest.verifySelf(cert);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {

        CertificateFactory cf = CertificateFactory.getInstance(certType);

        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(
                TestUtils.getEncodedX509Certificate()));

        SerializationTest.verifyGolden(this, cert);
    }

    /**
     * Test for <code>Certificate.CertificateRep.readResolve()</code> method<br>
     * 
     * Assertion: ObjectStreamException if a <code>CertPath</code> could not
     * be constructed
     */
    public final void testCertificateRep_readResolve() throws Exception {
        // Create object to be serialized
        Certificate c1 = new MyCertificate("DUMMY", new byte[] {(byte)0, (byte)1});

        // try to serialize/deserialize cert
        try {
            SerializationTest.copySerializable(c1);
            fail("No expected ObjectStreamException");
        } catch (ObjectStreamException e) {
        }
    }

    /**
     * Test for <code>writeReplace()</code> method<br>
     * ByteArray streams used.
     */
    public final void testWriteReplace() throws Exception
               {
        // Create object to be serialized
        Certificate c1 = new MyCertificate("DUMMY", null);

        // Try to serialize cert
        try {
            SerializationTest.copySerializable(c1);
            fail("No exception");
        } catch (ObjectStreamException e) {
        } catch (NullPointerException e) {
        }
    }
}

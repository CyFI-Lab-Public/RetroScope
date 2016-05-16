/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.security.tests.java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.apache.harmony.security.tests.support.cert.TestUtils;

import tests.support.resource.Support_Resources;

public class X509Certificate2Test extends junit.framework.TestCase {

    /**
     * @tests java.security.cert.X509Certificate#getExtensionValue(java.lang.String)
     */
    public void test_getExtensionValueLjava_lang_String() throws Exception {

        InputStream is = Support_Resources
                .getResourceStream("hyts_certificate_PEM.txt");

        CertificateFactory certFact = CertificateFactory.getInstance("X509");
        X509Certificate pemCert = (X509Certificate) certFact
                .generateCertificate(is);

        Vector<String> extensionOids = new Vector<String>();
        extensionOids.addAll(pemCert.getCriticalExtensionOIDs());
        extensionOids.addAll(pemCert.getNonCriticalExtensionOIDs());
        Iterator i = extensionOids.iterator();
        while (i.hasNext()) {
            String oid = (String) i.next();
            byte[] value = pemCert.getExtensionValue(oid);
            if (value != null && value.length > 0) {
                // check that it is an encoded as a OCTET STRING
                assertEquals("The extension value for the oid " + oid
                        + " was not encoded as an OCTET STRING", 0x04, value[0]);
            }
        }
    }

    /**
     * Test for X.509 Certificate provider
     */
    public void test_toString() throws Exception {

        // Regression for HARMONY-3384
        CertificateFactory certFact = CertificateFactory.getInstance("X509");
        X509Certificate pemCert = (X509Certificate) certFact
                .generateCertificate(new ByteArrayInputStream(TestUtils
                        .getX509Certificate_v3()));

        // extension value is empty sequence
        byte[] extnValue = pemCert.getExtensionValue("2.5.29.35");
        assertTrue(Arrays.equals(new byte[] { 0x04, 0x02, 0x30, 0x00 },
                extnValue));
        assertNotNull(pemCert.toString());
        // End regression for HARMONY-3384
    }
}

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

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Vector;

import tests.support.resource.Support_Resources;

import junit.framework.TestCase;

public class X509CRLEntry2Test extends junit.framework.TestCase {

	private X509Certificate pemCert = null;

	protected void setUp() throws Exception {

        InputStream is = Support_Resources
                .getResourceStream("hyts_certificate_PEM.txt");

        CertificateFactory certFact = CertificateFactory.getInstance("X509");
        pemCert = (X509Certificate) certFact.generateCertificate(is);
    }

	/**
	 * @tests java.security.cert.X509CRLEntry#getExtensionValue(java.lang.String)
	 */
	public void test_getExtensionValueLjava_lang_String() {
		if (pemCert != null) {
			Vector extensionOids = new Vector();
			extensionOids.addAll(pemCert.getCriticalExtensionOIDs());
			extensionOids.addAll(pemCert.getNonCriticalExtensionOIDs());
			Iterator i = extensionOids.iterator();
			while (i.hasNext()) {
				String oid = (String) i.next();
				byte[] value = pemCert.getExtensionValue(oid);
				if (value != null && value.length > 0) {
					// check that it is an encoded as a OCTET STRING
					assertTrue("The extension value for the oid " + oid
							+ " was not encoded as an OCTET STRING",
							value[0] == 0x04);
				}
			}// end while
		} else {
			fail("Unable to obtain X509Certificate");
		}
	}
}
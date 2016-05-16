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

import java.math.BigInteger;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

import org.apache.harmony.security.provider.cert.X509CRLEntryImpl;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.ReasonCode;
import org.apache.harmony.security.x509.TBSCertList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * X509CRLEntryImplTest test
 */
public class X509CRLEntryImplTest extends TestCase {

    /**
     * getExtensionValue(String oid) method testing.
     */
    public void testGetExtensionValue() throws Exception {
        // revoked certificate issuer
        X500Principal issuer =
            new X500Principal("O=Certificate Issuer");
        // revoked certificate serial number
        BigInteger serialNumber = BigInteger.valueOf(555);
        // crl entry extensions
        Extensions crlEntryExtensions = new Extensions();
        // add reason code extension which OID is 2.5.29.21
        // see RFC 3280 http://www.ietf.org/rfc/rfc3280.txt
        crlEntryExtensions.addExtension(
                new Extension("2.5.29.21", Extension.NON_CRITICAL,
                    new ReasonCode(ReasonCode.KEY_COMPROMISE)));
        // crl entry
        X509CRLEntryImpl crlEntry = new X509CRLEntryImpl(
                new TBSCertList.RevokedCertificate(
                        serialNumber,
                        new Date(),
                        crlEntryExtensions
                    ),
                issuer
            );
        assertNotNull(crlEntry.getExtensionValue("2.5.29.21"));
        assertNull("Null value should be returned in the case of "
                + "nonexisting extension",
                // demand absent Invalidity Date extension
                // which OID is 2.5.29.24 (RFC 3280)
                crlEntry.getExtensionValue("2.5.29.24"));
    }

    public static Test suite() {
        return new TestSuite(X509CRLEntryImplTest.class);
    }

}

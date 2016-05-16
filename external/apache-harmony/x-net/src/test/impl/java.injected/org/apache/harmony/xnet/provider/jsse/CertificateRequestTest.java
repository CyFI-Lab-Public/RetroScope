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

package org.apache.harmony.xnet.provider.jsse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;

/**
 * Test for <code>CertificateRequest</code> constructors and methods
 *  
 */
public class CertificateRequestTest extends TestCase {

    private static String base64certEncoding = "-----BEGIN CERTIFICATE-----\n"
            + "MIIC+jCCAragAwIBAgICAiswDAYHKoZIzjgEAwEBADAdMRswGQYDVQQKExJDZXJ0a"
            + "WZpY2F0ZSBJc3N1ZXIwIhgPMTk3MDAxMTIxMzQ2NDBaGA8xOTcwMDEyNDAzMzMyMF"
            + "owHzEdMBsGA1UEChMUU3ViamVjdCBPcmdhbml6YXRpb24wGTAMBgcqhkjOOAQDAQE"
            + "AAwkAAQIDBAUGBwiBAgCqggIAVaOCAhQwggIQMA8GA1UdDwEB/wQFAwMBqoAwEgYD"
            + "VR0TAQH/BAgwBgEB/wIBBTAUBgNVHSABAf8ECjAIMAYGBFUdIAAwZwYDVR0RAQH/B"
            + "F0wW4EMcmZjQDgyMi5OYW1lggdkTlNOYW1lpBcxFTATBgNVBAoTDE9yZ2FuaXphdG"
            + "lvboYaaHR0cDovL3VuaWZvcm0uUmVzb3VyY2UuSWSHBP///wCIByoDolyDsgMwDAY"
            + "DVR0eAQH/BAIwADAMBgNVHSQBAf8EAjAAMIGZBgNVHSUBAf8EgY4wgYsGBFUdJQAG"
            + "CCsGAQUFBwMBBggrBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDB"
            + "AYIKwYBBQUHAwUGCCsGAQUFBwMGBggrBgEFBQcDBwYIKwYBBQUHAwgGCCsGAQUFBw"
            + "MJBggrBgEFBQgCAgYKKwYBBAGCNwoDAwYJYIZIAYb4QgQBMA0GA1UdNgEB/wQDAgE"
            + "BMA4GBCpNhgkBAf8EAwEBATBkBgNVHRIEXTBbgQxyZmNAODIyLk5hbWWCB2ROU05h"
            + "bWWkFzEVMBMGA1UEChMMT3JnYW5pemF0aW9uhhpodHRwOi8vdW5pZm9ybS5SZXNvd"
            + "XJjZS5JZIcE////AIgHKgOiXIOyAzAJBgNVHR8EAjAAMAoGA1UdIwQDAQEBMAoGA1"
            + "UdDgQDAQEBMAoGA1UdIQQDAQEBMAwGByqGSM44BAMBAQADMAAwLQIUAL4QvoazNWP"
            + "7jrj84/GZlhm09DsCFQCBKGKCGbrP64VtUt4JPmLjW1VxQA==\n"
            + "-----END CERTIFICATE-----\n";

public void testCertificateRequest() throws Exception {

        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        ByteArrayInputStream bais = new ByteArrayInputStream(base64certEncoding
                .getBytes("UTF-8"));
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(bais);
        X509Certificate[] accepted = {cert};
        X500Principal[] certificate_authorities = {cert.getIssuerX500Principal()};
        
        byte[] certificate_types = new byte[] { CertificateRequest.RSA_SIGN,
                CertificateRequest.RSA_FIXED_DH };
        CertificateRequest message = new CertificateRequest(certificate_types,
                accepted);
        assertEquals("incorrect type", Handshake.CERTIFICATE_REQUEST, message
                .getType());
        assertTrue("incorrect CertificateRequest", Arrays.equals(
                message.certificate_types, certificate_types));
        assertTrue("incorrect CertificateRequest", Arrays.equals(
                message.certificate_authorities, certificate_authorities));

		HandshakeIODataStream out = new HandshakeIODataStream();
		message.send(out);
		byte[] encoded = out.getData(1000);
        assertEquals("incorrect out data length", message.length(), encoded.length);

		HandshakeIODataStream in = new HandshakeIODataStream();
		in.append(encoded);
		CertificateRequest message_2 = new CertificateRequest(in, message.length());
        assertTrue("incorrect message decoding", 
                Arrays.equals(message.certificate_types, message_2.certificate_types));
        assertTrue("incorrect message decoding", 
                Arrays.equals(message.certificate_authorities, message_2.certificate_authorities));

		in.append(encoded);
		try {
			message_2 = new CertificateRequest(in, message.length() - 1);
			fail("Small length: No expected AlertException");
		} catch (AlertException e) {
		}

		in.append(encoded);
		in.append(new byte[] { 1, 2, 3 });
		try {
			message_2 = new CertificateRequest(in, message.length() + 3);
			fail("Extra bytes: No expected AlertException ");
		} catch (AlertException e) {
		}
	}
}

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

package org.apache.harmony.security.tests.java.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import tests.support.Support_TestProvider;
import tests.support.resource.Support_Resources;

public class KeyStore2Test extends junit.framework.TestCase {
    static PrivateKey privateKey;
    static {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator
                    .getInstance("DSA");

            SecureRandom secureRandom = new SecureRandom();
            keyPairGenerator.initialize(1024, secureRandom);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            privateKey = keyPair.getPrivate();
        } catch (Exception e) {
            fail("initialization failed: " + e);
        }
    }

	final char[] pssWord = { 'a', 'b', 'c' };
        private Provider support_TestProvider;

	// creating a certificate
	String certificate = "-----BEGIN CERTIFICATE-----\n"
			+ "MIICZTCCAdICBQL3AAC2MA0GCSqGSIb3DQEBAgUAMF8xCzAJBgNVBAYTAlVTMSAw\n"
			+ "HgYDVQQKExdSU0EgRGF0YSBTZWN1cml0eSwgSW5jLjEuMCwGA1UECxMlU2VjdXJl\n"
			+ "IFNlcnZlciBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAeFw05NzAyMjAwMDAwMDBa\n"
			+ "Fw05ODAyMjAyMzU5NTlaMIGWMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZv\n"
			+ "cm5pYTESMBAGA1UEBxMJUGFsbyBBbHRvMR8wHQYDVQQKExZTdW4gTWljcm9zeXN0\n"
			+ "ZW1zLCBJbmMuMSEwHwYDVQQLExhUZXN0IGFuZCBFdmFsdWF0aW9uIE9ubHkxGjAY\n"
			+ "BgNVBAMTEWFyZ29uLmVuZy5zdW4uY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB\n"
			+ "iQKBgQCofmdY+PiUWN01FOzEewf+GaG+lFf132UpzATmYJkA4AEA/juW7jSi+LJk\n"
			+ "wJKi5GO4RyZoyimAL/5yIWDV6l1KlvxyKslr0REhMBaD/3Z3EsLTTEf5gVrQS6sT\n"
			+ "WMoSZAyzB39kFfsB6oUXNtV8+UKKxSxKbxvhQn267PeCz5VX2QIDAQABMA0GCSqG\n"
			+ "SIb3DQEBAgUAA34AXl3at6luiV/7I9MN5CXYoPJYI8Bcdc1hBagJvTMcmlqL2uOZ\n"
			+ "H9T5hNMEL9Tk6aI7yZPXcw/xI2K6pOR/FrMp0UwJmdxX7ljV6ZtUZf7pY492UqwC\n"
			+ "1777XQ9UEZyrKJvF5ntleeO0ayBqLGVKCWzWZX9YsXCpv47FNLZbupE=\n"
			+ "-----END CERTIFICATE-----\n";

	ByteArrayInputStream certArray = new ByteArrayInputStream(certificate
			.getBytes());

	String certificate2 = "-----BEGIN CERTIFICATE-----\n"
			+ "MIICZzCCAdCgAwIBAgIBGzANBgkqhkiG9w0BAQUFADBhMQswCQYDVQQGEwJVUzEY\n"
			+ "MBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsT\n"
			+ "A1BLSTEcMBoGA1UEAxMTRG9EIFBLSSBNZWQgUm9vdCBDQTAeFw05ODA4MDMyMjAy\n"
			+ "MjlaFw0wODA4MDQyMjAyMjlaMGExCzAJBgNVBAYTAlVTMRgwFgYDVQQKEw9VLlMu\n"
			+ "IEdvdmVybm1lbnQxDDAKBgNVBAsTA0RvRDEMMAoGA1UECxMDUEtJMRwwGgYDVQQD\n"
			+ "ExNEb0QgUEtJIE1lZCBSb290IENBMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n"
			+ "gQDbrM/J9FrJSX+zxFUbsI9Vw5QbguVBIa95rwW/0M8+sM0r5gd+DY6iubm6wnXk\n"
			+ "CSvbfQlFEDSKr4WYeeGp+d9WlDnQdtDFLdA45tCi5SHjnW+hGAmZnld0rz6wQekF\n"
			+ "5xQaa5A6wjhMlLOjbh27zyscrorMJ1O5FBOWnEHcRv6xqQIDAQABoy8wLTAdBgNV\n"
			+ "HQ4EFgQUVrmYR6m9701cHQ3r5kXyG7zsCN0wDAYDVR0TBAUwAwEB/zANBgkqhkiG\n"
			+ "9w0BAQUFAAOBgQDVX1Y0YqC7vekeZjVxtyuC8Mnxbrz6D109AX07LEIRzNYzwZ0w\n"
			+ "MTImSp9sEzWW+3FueBIU7AxGys2O7X0qmN3zgszPfSiocBuQuXIYQctJhKjF5KVc\n"
			+ "VGQRYYlt+myhl2vy6yPzEVCjiKwMEb1Spu0irCf+lFW2hsdjvmSQMtZvOw==\n"
			+ "-----END CERTIFICATE-----\n";

	ByteArrayInputStream certArray2 = new ByteArrayInputStream(certificate2
			.getBytes());

	String certificate3 = "-----BEGIN CERTIFICATE-----\n"
			+ "MIIDXDCCAsWgAwIBAgIBSjANBgkqhkiG9w0BAQUFADBWMQswCQYDVQQGEwJVUzEY\n"
			+ "MBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsT\n"
			+ "A1BLSTERMA8GA1UEAxMITWVkIENBLTEwHhcNOTgwODAyMTgwMjQwWhcNMDEwODAy\n"
			+ "MTgwMjQwWjB0MQswCQYDVQQGEwJVUzEYMBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50\n"
			+ "MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsTA1BLSTENMAsGA1UECxMEVVNBRjEgMB4G\n"
			+ "A1UEAxMXR3VtYnkuSm9zZXBoLjAwMDAwMDUwNDQwgZ8wDQYJKoZIhvcNAQEBBQAD\n"
			+ "gY0AMIGJAoGBALT/R7bPqs1c1YqXAg5HNpZLgW2HuAc7RCaP06cE4R44GBLw/fQc\n"
			+ "VRNLn5pgbTXsDnjiZVd8qEgYqjKFQka4/tNhaF7No2tBZB+oYL/eP0IWtP+h/W6D\n"
			+ "KR5+UvIIdgmx7k3t9jp2Q51JpHhhKEb9WN54trCO9Yu7PYU+LI85jEIBAgMBAAGj\n"
			+ "ggEaMIIBFjAWBgNVHSAEDzANMAsGCWCGSAFlAgELAzAfBgNVHSMEGDAWgBQzOhTo\n"
			+ "CWdhiGUkIOx5cELXppMe9jAdBgNVHQ4EFgQUkLBJl+ayKgzOp/wwBX9M1lSkCg4w\n"
			+ "DgYDVR0PAQH/BAQDAgbAMAwGA1UdEwEB/wQCMAAwgZ0GA1UdHwSBlTCBkjCBj6CB\n"
			+ "jKCBiYaBhmxkYXA6Ly9kcy0xLmNoYW1iLmRpc2EubWlsL2NuJTNkTWVkJTIwQ0El\n"
			+ "MmQxJTJjb3UlM2RQS0klMmNvdSUzZERvRCUyY28lM2RVLlMuJTIwR292ZXJubWVu\n"
			+ "dCUyY2MlM2RVUz9jZXJ0aWZpY2F0ZVJldm9jYXRpb25MaXN0JTNiYmluYXJ5MA0G\n"
			+ "CSqGSIb3DQEBBQUAA4GBAFjapuDHMvIdUeYRyEYdShBR1JZC20tJ3MQnyBQveddz\n"
			+ "LGFDGpIkRAQU7T/5/ne8lMexyxViC21xOlK9LdbJCbVyywvb9uEm/1je9wieQQtr\n"
			+ "kjykuB+WB6qTCIslAO/eUmgzfzIENvnH8O+fH7QTr2PdkFkiPIqBJYHvw7F3XDqy\n"
			+ "-----END CERTIFICATE-----\n";

	ByteArrayInputStream certArray3 = new ByteArrayInputStream(certificate3
			.getBytes());

	private byte[] creatCertificate() throws Exception {
		ByteArrayOutputStream out = null;

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);
		// alias 1
		keyTest.setCertificateEntry("alias1", cert[0]);

		// alias 2
        keyTest.setKeyEntry("alias2", privateKey, pssWord, cert);

		// alias 3
		keyTest.setCertificateEntry("alias3", cert[1]);

		out = new ByteArrayOutputStream();
		keyTest.store(out, pssWord);
		out.close();

		return out.toByteArray();
	}

	/**
	 * @tests java.security.KeyStore#aliases()
	 */
	public void test_aliases() throws Exception {
		// Test for method java.util.Enumeration
		// java.security.KeyStore.aliases()
		// NOT COMPATIBLE WITH PCS#12
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		// KeyStore keyTest =
		// KeyStore.getInstance(KeyStore.getDefaultType());
		// alias 1
		keyTest.setCertificateEntry("alias1", cert[0]);

		// alias 2
		keyTest.setCertificateEntry("alias2", cert[0]);

		// alias 3
		keyTest.setCertificateEntry("alias3", cert[0]);

		// obtaining the aliase
		Enumeration aliase = keyTest.aliases();
		Set alia = new HashSet();
		int i = 0;
		while (aliase.hasMoreElements()) {
			alia.add(aliase.nextElement());
			i++;
		}
		assertTrue("the alias names were returned wrong", i == 3
				&& alia.contains("alias1") && alia.contains("alias2")
				&& alia.contains("alias3"));
	}

	/**
	 * @tests java.security.KeyStore#containsAlias(java.lang.String)
	 */
	public void test_containsAliasLjava_lang_String() throws Exception {
		// Test for method boolean
		// java.security.KeyStore.containsAlias(java.lang.String)
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		// alias 1
		keyTest.setCertificateEntry("alias1", cert[0]);

		// alias 2
		keyTest.setCertificateEntry("alias2", cert[0]);

		assertTrue("alias1 does not exist", keyTest.containsAlias("alias1"));
		assertTrue("alias2 does not exist", keyTest.containsAlias("alias2"));
		assertFalse("alias3 exists", keyTest.containsAlias("alias3"));
	}

	/**
	 * @tests java.security.KeyStore#getCertificate(java.lang.String)
	 */
	public void test_getCertificateLjava_lang_String() throws Exception {
		// Test for method java.security.cert.Certificate
		// java.security.KeyStore.getCertificate(java.lang.String)
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		// alias 1
		PublicKey pub = cert[0].getPublicKey();
		keyTest.setCertificateEntry("alias1", cert[0]);

		java.security.cert.Certificate certRes = keyTest
				.getCertificate("alias1");
		assertTrue("the public key of the certificate from getCertificate() "
				+ "did not equal the original certificate", certRes
				.getPublicKey() == pub);

		// alias 2
		keyTest.setCertificateEntry("alias2", cert[0]);

		// testing for a certificate chain
		java.security.cert.Certificate cert2 = keyTest.getCertificate("alias2");
		assertTrue("the certificate for alias2 is supposed to exist",
				cert2 != null && cert2.equals(cert[0]));

	}

	/**
	 * @tests java.security.KeyStore#getCertificateAlias(java.security.cert.Certificate)
	 */
	public void test_getCertificateAliasLjava_security_cert_Certificate()
			throws Exception {
		// Test for method java.lang.String
		// java.security.KeyStore.getCertificateAlias(java.security.cert.Certificate)
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		// certificate entry
		keyTest.setCertificateEntry("alias1", cert[1]);
		String alias = keyTest.getCertificateAlias(cert[1]);
		assertTrue("certificate entry - the alias returned for this "
				+ "certificate was wrong", alias.equals("alias1"));

		// key entry

        keyTest.setKeyEntry("alias2", privateKey, pssWord, cert);
		alias = keyTest.getCertificateAlias(cert[0]);
		assertTrue("key entry - the alias returned for this "
				+ "certificate was wrong", alias.equals("alias2"));

		// testing case with a nonexistent certificate
		X509Certificate cert2 = (X509Certificate) cf
				.generateCertificate(certArray3);
		String aliasNull = keyTest.getCertificateAlias(cert2);
		assertNull("the alias returned for the nonexist certificate "
				+ "was NOT null", aliasNull);
	}

	/**
	 * @tests java.security.KeyStore#getCertificateChain(java.lang.String)
	 */
	public void test_getCertificateChainLjava_lang_String() throws Exception {
		// Test for method java.security.cert.Certificate []
		// java.security.KeyStore.getCertificateChain(java.lang.String)
		// creatCertificate();
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		// alias 1
		keyTest.setCertificateEntry("alias1", cert[0]);

		// alias 2
        keyTest.setKeyEntry("alias2", privateKey, pssWord, cert);

		java.security.cert.Certificate[] certRes = keyTest
				.getCertificateChain("alias2");
		assertTrue("there are more than two certificate returned "
				+ "from getCertificateChain", certRes.length == 2);
		assertTrue("the certificates returned from getCertificateChain "
				+ "is not correct", cert[0].getPublicKey() == certRes[0]
				.getPublicKey()
				&& cert[1].getPublicKey() == certRes[1].getPublicKey());
		java.security.cert.Certificate[] certResNull = keyTest
				.getCertificateChain("alias1");
		assertNull("the certificate chain returned from "
				+ "getCertificateChain is NOT null", certResNull);
	}

	/**
	 * @tests java.security.KeyStore#getInstance(java.lang.String)
	 */
	public void test_getInstanceLjava_lang_String() throws Exception {
		// Test for method java.security.KeyStore
		// java.security.KeyStore.getInstance(java.lang.String)
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		assertTrue("the method getInstance did not obtain "
				+ "the correct type", keyTest.getType().equals(
				KeyStore.getDefaultType()));
	}

	/**
	 * @tests java.security.KeyStore#getInstance(java.lang.String,
	 *        java.lang.String)
	 */
	public void test_getInstanceLjava_lang_StringLjava_lang_String()
			throws Exception {
		// Test for method java.security.KeyStore
		// java.security.KeyStore.getInstance(java.lang.String,
		// java.lang.String)
		KeyStore keyTest = KeyStore.getInstance("PKCS#12/Netscape",
				"TestProvider");
		assertTrue("the method getInstance did not obtain the "
				+ "correct provider and type", keyTest.getProvider().getName()
				.equals("TestProvider")
				&& keyTest.getType().equals("PKCS#12/Netscape"));
	}

	/**
	 * @tests java.security.KeyStore#getKey(java.lang.String, char[])
	 */
	public void test_getKeyLjava_lang_String$C() throws Exception {

		// Test for method java.security.Key
		// java.security.KeyStore.getKey(java.lang.String, char [])
		// creatCertificate();
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		keyTest.setKeyEntry("alias2", privateKey, pssWord, cert);
		PrivateKey returnedKey = (PrivateKey) keyTest.getKey("alias2", pssWord);
		byte[] retB = returnedKey.getEncoded();
		byte[] priB = privateKey.getEncoded();
		boolean equality = Arrays.equals(retB, priB);
		equality &= returnedKey.getAlgorithm()
				.equals(privateKey.getAlgorithm());
		equality &= returnedKey.getFormat().equals(privateKey.getFormat());
		assertTrue("the private key returned from getKey for a "
				+ "key entry did not equal the original key", equality);

		try {
			keyTest.getKey("alias2", "wrong".toCharArray());
			fail("Should have thrown UnrecoverableKeyException");
		} catch (UnrecoverableKeyException e) {
			// expected
		}

		keyTest.setCertificateEntry("alias1", cert[1]);
		assertNull("the private key returned from getKey for "
				+ "a certificate entry is not null", keyTest.getKey("alias1",
				pssWord));
	}

	/**
	 * @tests java.security.KeyStore#getProvider()
	 */
	public void test_getProvider() throws Exception {
		// Test for method java.security.Provider
		// java.security.KeyStore.getProvider()
		KeyStore keyTest = KeyStore.getInstance("PKCS#12/Netscape",
				"TestProvider");
		Provider provKeyStore = keyTest.getProvider();
		assertEquals("the provider should be TestProvider", "TestProvider",
				provKeyStore.getName());
	}

	/**
	 * @tests java.security.KeyStore#getType()
	 */
	public void test_getType() throws Exception {
		// Test for method java.lang.String java.security.KeyStore.getType()
		KeyStore keyTest = KeyStore.getInstance("PKCS#12/Netscape",
				"TestProvider");
		assertEquals(
				"type should be PKCS#12/Netscape for provider TestProvider",
				"PKCS#12/Netscape", keyTest.getType());
	}

	/**
	 * @tests java.security.KeyStore#isCertificateEntry(java.lang.String)
	 */
	public void test_isCertificateEntryLjava_lang_String() throws Exception {
		// Test for method boolean
		// java.security.KeyStore.isCertificateEntry(java.lang.String)
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);
		// alias 1
		keyTest.setCertificateEntry("alias1", cert[0]);

		// alias 2
        keyTest.setKeyEntry("alias2", privateKey, pssWord, cert);

		assertTrue("isCertificateEntry method returns false for a certificate",
				keyTest.isCertificateEntry("alias1"));
		assertFalse("isCertificateEntry method returns true for noncertificate",
				keyTest.isCertificateEntry("alias2"));
	}

	/**
	 * @tests java.security.KeyStore#isKeyEntry(java.lang.String)
	 */
	public void test_isKeyEntryLjava_lang_String() throws Exception {
		// Test for method boolean
		// java.security.KeyStore.isKeyEntry(java.lang.String)
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);
		// alias 1
		keyTest.setCertificateEntry("alias1", cert[0]);

		// alias 2
        keyTest.setKeyEntry("alias2", privateKey, pssWord, cert);

		assertTrue("isKeyEntry method returns false for a certificate", keyTest
				.isKeyEntry("alias2"));
		assertFalse("isKeyEntry method returns true for noncertificate", keyTest
				.isKeyEntry("alias1"));
	}

	/**
	 * @tests java.security.KeyStore#load(java.io.InputStream, char[])
	 */
	public void test_loadLjava_io_InputStream$C() throws Exception {
		// Test for method void java.security.KeyStore.load(java.io.InputStream,
		// char [])
		byte[] keyStore = creatCertificate();
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream in = new ByteArrayInputStream(keyStore);
		keyTest.load(in, pssWord);
		in.close();
		assertTrue("alias1 is not a certificate", keyTest
				.isCertificateEntry("alias1"));
		assertTrue("alias2 is not a keyEntry",
				keyTest.isKeyEntry("alias2"));
		assertTrue("alias3 is not a certificate", keyTest
				.isCertificateEntry("alias3"));

		// test with null password
		keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		in = new ByteArrayInputStream(keyStore);
		keyTest.load(in, null);
		in.close();
		assertTrue("alias1 is not a certificate", keyTest
				.isCertificateEntry("alias1"));
		assertTrue("alias2 is not a keyEntry",
				keyTest.isKeyEntry("alias2"));
		assertTrue("alias3 is not a certificate", keyTest
				.isCertificateEntry("alias3"));

		keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream v1in = Support_Resources.getStream("hyts_ks.bks");
		char[] pass = "abcdef".toCharArray();
		keyTest.load(v1in, pass);
		v1in.close();
		keyTest.getKey("mykey", pass);
	}

	/**
	 * @tests java.security.KeyStore#setCertificateEntry(java.lang.String,
	 *        java.security.cert.Certificate)
	 */
	public void test_setCertificateEntryLjava_lang_StringLjava_security_cert_Certificate()
			throws Exception {
		// Test for method void
		// java.security.KeyStore.setCertificateEntry(java.lang.String,
		// java.security.cert.Certificate)
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) cf
				.generateCertificate(certArray);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		PublicKey pub = cert.getPublicKey();
		keyTest.setCertificateEntry("alias1", cert);
		assertTrue(
				"the entry specified by the alias alias1 is not a certificate",
				keyTest.isCertificateEntry("alias1"));
		java.security.cert.Certificate resultCert = keyTest
				.getCertificate("alias1");
		assertTrue(
				"the public key of the certificate from getCertificate() did not equal the original certificate",
				resultCert.getPublicKey() == pub);
	}

	/**
	 * @tests java.security.KeyStore#setKeyEntry(java.lang.String, byte[],
	 *        java.security.cert.Certificate[])
	 */
	public void test_setKeyEntryLjava_lang_String$B$Ljava_security_cert_Certificate()
			throws Exception {

//		fail("Test hangs - requires a full math implementation ??");

		// Test for method void
		// java.security.KeyStore.setKeyEntry(java.lang.String, byte [],
		// java.security.cert.Certificate [])

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		// set the same alias as keyEntry
		keyTest.setKeyEntry("alias2", privateKey.getEncoded(), cert);
		assertTrue("the entry specified by the alias alias2 is not a keyEntry",
				keyTest.isKeyEntry("alias2"));
	}

	/**
	 * @tests java.security.KeyStore#setKeyEntry(java.lang.String,
	 *        java.security.Key, char[], java.security.cert.Certificate[])
	 */
	public void test_setKeyEntryLjava_lang_StringLjava_security_Key$C$Ljava_security_cert_Certificate()
			throws Exception {

		// Test for method void
		// java.security.KeyStore.setKeyEntry(java.lang.String,
		// java.security.Key, char [], java.security.cert.Certificate [])

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);

		keyTest.setKeyEntry("alias3", privateKey, pssWord, cert);
		assertTrue("the entry specified by the alias alias3 is not a keyEntry",
				keyTest.isKeyEntry("alias3"));
	}

	/**
	 * @tests java.security.KeyStore#size()
	 */
	public void test_size() throws Exception {
		// Test for method int java.security.KeyStore.size()

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert[] = new X509Certificate[2];
		cert[0] = (X509Certificate) cf.generateCertificate(certArray);
		cert[1] = (X509Certificate) cf.generateCertificate(certArray2);
		KeyStore keyTest = KeyStore.getInstance(KeyStore.getDefaultType());
		keyTest.load(null, null);
		// alias 1
		keyTest.setCertificateEntry("alias1", cert[0]);

		// alias 2
        keyTest.setKeyEntry("alias2", privateKey, pssWord, cert);

		// alias 3
		keyTest.setCertificateEntry("alias3", cert[1]);

		assertEquals("the size of the keyStore is not 3", 3, keyTest.size());
	}
    
    
    public void test_Constructor() throws Exception {
        KeyStore ks = new MockKeyStore(new MockKeyStoreSpi(), null,
                "MockKeyStore");
        ks.load(null, null);
        ks.store(null, null);

        ks = new MockKeyStore(new MockKeyStoreSpi(), null, "MockKeyStore");
        ks.load(null);
        try {
            ks.store(null);
            fail("should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected.
        }
    }
    
    public class MockKeyStore extends KeyStore{

        public MockKeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type) {
            super(keyStoreSpi, provider, type);            
        }
    }
    
    public class MockKeyStoreSpi extends KeyStoreSpi {

        @Override
        public Enumeration<String> engineAliases() {
            return null;
        }

        @Override
        public boolean engineContainsAlias(String alias) {
            return false;
        }

        @Override
        public void engineDeleteEntry(String alias) throws KeyStoreException {
            return;
        }

        @Override
        public Certificate engineGetCertificate(String alias) {
            return null;
        }

        @Override
        public String engineGetCertificateAlias(Certificate cert) {
            return null;
        }

        @Override
        public Certificate[] engineGetCertificateChain(String alias) {
            return null;
        }

        @Override
        public Date engineGetCreationDate(String alias) {
            return null;
        }

        @Override
        public Key engineGetKey(String alias, char[] password)
                throws NoSuchAlgorithmException, UnrecoverableKeyException {
            return null;
        }

        @Override
        public boolean engineIsCertificateEntry(String alias) {
            return false;
        }

        @Override
        public boolean engineIsKeyEntry(String alias) {
            return false;
        }

        @Override
        public void engineLoad(InputStream stream, char[] password)
                throws IOException, NoSuchAlgorithmException,
                CertificateException {
            return;
        }

        @Override
        public void engineSetCertificateEntry(String alias, Certificate cert)
                throws KeyStoreException {
            return;
        }

        @Override
        public void engineSetKeyEntry(String alias, Key key, char[] password,
                Certificate[] chain) throws KeyStoreException {
            return;
        }

        @Override
        public void engineSetKeyEntry(String alias, byte[] key,
                Certificate[] chain) throws KeyStoreException {
            return;
        }

        @Override
        public int engineSize() {
            return 0;
        }

        @Override
        public void engineStore(OutputStream stream, char[] password)
                throws IOException, NoSuchAlgorithmException,
                CertificateException {
            return;
        }
        
    }

	/**
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 */
	protected void setUp() throws Exception {
        super.setUp();
        support_TestProvider = new Support_TestProvider();
        Security.addProvider(support_TestProvider);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        Security.removeProvider(support_TestProvider.getName());
    }
}
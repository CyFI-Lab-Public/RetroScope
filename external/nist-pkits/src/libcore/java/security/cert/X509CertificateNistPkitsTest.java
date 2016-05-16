/*
 * Copyright (C) 2013 The Android Open Source Project
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

package libcore.java.security.cert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class X509CertificateNistPkitsTest extends TestCase {
    public static final String RESOURCE_PACKAGE = "/tests/resources/";

    public static InputStream getStream(String name) {
        // If we have the resources packaged up in our jar file, get them that way.
        String path = RESOURCE_PACKAGE + name;
        InputStream result = X509CertificateNistPkitsTest.class.getResourceAsStream(path);
        if (result != null) {
            return result;
        }
        // Otherwise, if we're in an Android build tree, get the files directly.
        String ANDROID_BUILD_TOP = System.getenv("ANDROID_BUILD_TOP");
        if (ANDROID_BUILD_TOP != null) {
            File resource = new File(ANDROID_BUILD_TOP + "/external/nist-pkits/res" + path);
            if (resource.exists()) {
                try {
                    return new FileInputStream(resource);
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Couldn't open: " + resource, ex);
                }
            }
        }
        throw new IllegalArgumentException("No such resource: " + path);
    }

    private final X509Certificate getCertificate(CertificateFactory f, String name)
            throws Exception {
        final String fileName = "nist-pkits/certs/" + name;
        final InputStream is = getStream(fileName);
        assertNotNull("File does not exist: " + fileName, is);
        try {
            return (X509Certificate) f.generateCertificate(is);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    private final X509Certificate[] getCertificates(CertificateFactory f, String[] names)
            throws Exception {
        X509Certificate[] certs = new X509Certificate[names.length];

        for (int i = 0; i < names.length; i++) {
            certs[i] = getCertificate(f, names[i]);
        }

        return certs;
    }

    private final X509CRL getCRL(CertificateFactory f, String name) throws Exception {
        final String fileName = "nist-pkits/crls/" + name;
        final InputStream is = getStream(fileName);
        assertNotNull("File does not exist: " + fileName, is);
        try {
            return (X509CRL) f.generateCRL(is);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    private final X509CRL[] getCRLs(CertificateFactory f, String[] names) throws Exception {
        X509CRL[] crls = new X509CRL[names.length];

        for (int i = 0; i < names.length; i++) {
            crls[i] = getCRL(f, names[i]);
        }

        return crls;
    }

    private CertPath getTestPath(CertificateFactory f, String[] pathCerts) throws Exception {
        X509Certificate[] certs = getCertificates(f, pathCerts);
        return f.generateCertPath(Arrays.asList(certs));
    }

    private PKIXParameters getTestPathParams(CertificateFactory f, String trustedCAName,
            String[] pathCerts, String[] pathCRLs) throws Exception {
        X509Certificate[] certs = getCertificates(f, pathCerts);
        X509CRL[] crls = getCRLs(f, pathCRLs);
        X509Certificate trustedCA = getCertificate(f, trustedCAName);

        Collection<Object> certCollection = new ArrayList<Object>();
        certCollection.addAll(Arrays.asList(crls));
        certCollection.addAll(Arrays.asList(certs));
        certCollection.add(trustedCA);
        CollectionCertStoreParameters certStoreParams = new CollectionCertStoreParameters(
                certCollection);
        CertStore certStore = CertStore.getInstance("Collection", certStoreParams);

        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        anchors.add(new TrustAnchor(trustedCA, null));

        PKIXParameters params = new PKIXParameters(anchors);
        params.addCertStore(certStore);

        return params;
    }

    private void assertInvalidPath(String trustAnchor, String[] certs, String[] crls)
            throws Exception, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        CertificateFactory f = CertificateFactory.getInstance("X.509");

        PKIXParameters params = getTestPathParams(f, trustAnchor, certs, crls);
        CertPath cp = getTestPath(f, certs);
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");

        try {
            PKIXCertPathValidatorResult cpvResult = (PKIXCertPathValidatorResult) cpv.validate(cp,
                    params);
            fail();
        } catch (CertPathValidatorException expected) {
        }
    }

    private void assertValidPath(String trustAnchor, String[] certs, String[] crls)
            throws Exception, NoSuchAlgorithmException, CertPathValidatorException,
            InvalidAlgorithmParameterException {
        CertificateFactory f = CertificateFactory.getInstance("X.509");

        PKIXParameters params = getTestPathParams(f, trustAnchor, certs, crls);
        CertPath cp = getTestPath(f, certs);
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");

        PKIXCertPathValidatorResult cpvResult = (PKIXCertPathValidatorResult) cpv.validate(cp,
                params);
    }

    /* DO NOT MANUALLY EDIT -- BEGIN AUTOMATICALLY GENERATED TESTS */
    /** NIST PKITS test 4.1.1 */
    public void testSignatureVerification_ValidSignaturesTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidCertificatePathTest1EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.1.2 */
    public void testSignatureVerification_InvalidCASignatureTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidCASignatureTest2EE.crt",
                "BadSignedCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BadSignedCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.1.3 */
    public void testSignatureVerification_InvalidEESignatureTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidEESignatureTest3EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.1.4 */
    public void testSignatureVerification_ValidDSASignaturesTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDSASignaturesTest4EE.crt",
                "DSACACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "DSACACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.1.5 */
    public void testSignatureVerification_ValidDSAParameterInheritanceTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDSAParameterInheritanceTest5EE.crt",
                "DSAParametersInheritedCACert.crt",
                "DSACACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "DSACACRL.crl",
                "DSAParametersInheritedCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.1.6 */
    public void testSignatureVerification_InvalidDSASignatureTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDSASignatureTest6EE.crt",
                "DSACACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "DSACACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.1 */
    public void testValidityPeriods_InvalidCAnotBeforeDateTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidCAnotBeforeDateTest1EE.crt",
                "BadnotBeforeDateCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BadnotBeforeDateCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.2 */
    public void testValidityPeriods_InvalidEEnotBeforeDateTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidEEnotBeforeDateTest2EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.3 */
    public void testValidityPeriods_Validpre2000UTCnotBeforeDateTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "Validpre2000UTCnotBeforeDateTest3EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.4 */
    public void testValidityPeriods_ValidGeneralizedTimenotBeforeDateTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidGeneralizedTimenotBeforeDateTest4EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.5 */
    public void testValidityPeriods_InvalidCAnotAfterDateTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidCAnotAfterDateTest5EE.crt",
                "BadnotAfterDateCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BadnotAfterDateCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.6 */
    public void testValidityPeriods_InvalidEEnotAfterDateTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidEEnotAfterDateTest6EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.7 */
    public void testValidityPeriods_Invalidpre2000UTCEEnotAfterDateTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "Invalidpre2000UTCEEnotAfterDateTest7EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.2.8 */
    public void testValidityPeriods_ValidGeneralizedTimenotAfterDateTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidGeneralizedTimenotAfterDateTest8EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.1 */
    public void testVerifyingNameChaining_InvalidNameChainingEETest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidNameChainingTest1EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.2 */
    public void testVerifyingNameChaining_InvalidNameChainingOrderTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidNameChainingOrderTest2EE.crt",
                "NameOrderingCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "NameOrderCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.3 */
    public void testVerifyingNameChaining_ValidNameChainingWhitespaceTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidNameChainingWhitespaceTest3EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.4 */
    public void testVerifyingNameChaining_ValidNameChainingWhitespaceTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidNameChainingWhitespaceTest4EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.5 */
    public void testVerifyingNameChaining_ValidNameChainingCapitalizationTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidNameChainingCapitalizationTest5EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.6 */
    public void testVerifyingNameChaining_ValidNameChainingUIDsTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidNameUIDsTest6EE.crt",
                "UIDCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "UIDCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.7 */
    public void testVerifyingNameChaining_ValidRFC3280MandatoryAttributeTypesTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidRFC3280MandatoryAttributeTypesTest7EE.crt",
                "RFC3280MandatoryAttributeTypesCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "RFC3280MandatoryAttributeTypesCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.8 */
    public void testVerifyingNameChaining_ValidRFC3280OptionalAttributeTypesTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidRFC3280OptionalAttributeTypesTest8EE.crt",
                "RFC3280OptionalAttributeTypesCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "RFC3280OptionalAttributeTypesCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.9 */
    public void testVerifyingNameChaining_ValidUTF8StringEncodedNamesTest9() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidUTF8StringEncodedNamesTest9EE.crt",
                "UTF8StringEncodedNamesCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "UTF8StringEncodedNamesCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.10 */
    public void testVerifyingNameChaining_ValidRolloverfromPrintableStringtoUTF8StringTest10() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidRolloverfromPrintableStringtoUTF8StringTest10EE.crt",
                "RolloverfromPrintableStringtoUTF8StringCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "RolloverfromPrintableStringtoUTF8StringCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.3.11 */
    public void testVerifyingNameChaining_ValidUTF8StringCaseInsensitiveMatchTest11() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidUTF8StringCaseInsensitiveMatchTest11EE.crt",
                "UTF8StringCaseInsensitiveMatchCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "UTF8StringCaseInsensitiveMatchCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.1 */
    public void testBasicCertificateRevocationTests_MissingCRLTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidMissingCRLTest1EE.crt",
                "NoCRLCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.2 */
    public void testBasicCertificateRevocationTests_InvalidRevokedCATest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidRevokedCATest2EE.crt",
                "RevokedsubCACert.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
                "RevokedsubCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.3 */
    public void testBasicCertificateRevocationTests_InvalidRevokedEETest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidRevokedEETest3EE.crt",
                "GoodCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.4 */
    public void testBasicCertificateRevocationTests_InvalidBadCRLSignatureTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidBadCRLSignatureTest4EE.crt",
                "BadCRLSignatureCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BadCRLSignatureCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.5 */
    public void testBasicCertificateRevocationTests_InvalidBadCRLIssuerNameTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidBadCRLIssuerNameTest5EE.crt",
                "BadCRLIssuerNameCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BadCRLIssuerNameCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.6 */
    public void testBasicCertificateRevocationTests_InvalidWrongCRLTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidWrongCRLTest6EE.crt",
                "WrongCRLCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "WrongCRLCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.7 */
    public void testBasicCertificateRevocationTests_ValidTwoCRLsTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidTwoCRLsTest7EE.crt",
                "TwoCRLsCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "TwoCRLsCAGoodCRL.crl",
                "TwoCRLsCABadCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.8 */
    public void testBasicCertificateRevocationTests_InvalidUnknownCRLEntryExtensionTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidUnknownCRLEntryExtensionTest8EE.crt",
                "UnknownCRLEntryExtensionCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "UnknownCRLEntryExtensionCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.9 */
    public void testBasicCertificateRevocationTests_InvalidUnknownCRLExtensionTest9() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidUnknownCRLExtensionTest9EE.crt",
                "UnknownCRLExtensionCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "UnknownCRLExtensionCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.10 */
    public void testBasicCertificateRevocationTests_InvalidUnknownCRLExtensionTest10() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidUnknownCRLExtensionTest10EE.crt",
                "UnknownCRLExtensionCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "UnknownCRLExtensionCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.11 */
    public void testBasicCertificateRevocationTests_InvalidOldCRLnextUpdateTest11() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidOldCRLnextUpdateTest11EE.crt",
                "OldCRLnextUpdateCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "OldCRLnextUpdateCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.12 */
    public void testBasicCertificateRevocationTests_Invalidpre2000CRLnextUpdateTest12() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "Invalidpre2000CRLnextUpdateTest12EE.crt",
                "pre2000CRLnextUpdateCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pre2000CRLnextUpdateCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.13 */
    public void testBasicCertificateRevocationTests_ValidGeneralizedTimeCRLnextUpdateTest13() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidGeneralizedTimeCRLnextUpdateTest13EE.crt",
                "GeneralizedTimeCRLnextUpdateCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GeneralizedTimeCRLnextUpdateCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.14 */
    public void testBasicCertificateRevocationTests_ValidNegativeSerialNumberTest14() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidNegativeSerialNumberTest14EE.crt",
                "NegativeSerialNumberCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "NegativeSerialNumberCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.15 */
    public void testBasicCertificateRevocationTests_InvalidNegativeSerialNumberTest15() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidNegativeSerialNumberTest15EE.crt",
                "NegativeSerialNumberCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "NegativeSerialNumberCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.16 */
    public void testBasicCertificateRevocationTests_ValidLongSerialNumberTest16() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidLongSerialNumberTest16EE.crt",
                "LongSerialNumberCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "LongSerialNumberCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.17 */
    public void testBasicCertificateRevocationTests_ValidLongSerialNumberTest17() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidLongSerialNumberTest17EE.crt",
                "LongSerialNumberCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "LongSerialNumberCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.18 */
    public void testBasicCertificateRevocationTests_InvalidLongSerialNumberTest18() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidLongSerialNumberTest18EE.crt",
                "LongSerialNumberCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "LongSerialNumberCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.19 */
    public void testBasicCertificateRevocationTests_ValidSeparateCertificateandCRLKeysTest19() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidSeparateCertificateandCRLKeysTest19EE.crt",
                "SeparateCertificateandCRLKeysCRLSigningCert.crt",
                "SeparateCertificateandCRLKeysCertificateSigningCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "SeparateCertificateandCRLKeysCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.20 */
    public void testBasicCertificateRevocationTests_InvalidSeparateCertificateandCRLKeysTest20() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidSeparateCertificateandCRLKeysTest20EE.crt",
                "SeparateCertificateandCRLKeysCRLSigningCert.crt",
                "SeparateCertificateandCRLKeysCertificateSigningCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "SeparateCertificateandCRLKeysCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.4.21 */
    public void testBasicCertificateRevocationTests_InvalidSeparateCertificateandCRLKeysTest21() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidSeparateCertificateandCRLKeysTest21EE.crt",
                "SeparateCertificateandCRLKeysCA2CRLSigningCert.crt",
                "SeparateCertificateandCRLKeysCA2CertificateSigningCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "SeparateCertificateandCRLKeysCA2CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.1 */
    public void testVerifyingPathswithSelfIssuedCertificates_ValidBasicSelfIssuedOldWithNewTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidBasicSelfIssuedOldWithNewTest1EE.crt",
                "BasicSelfIssuedNewKeyOldWithNewCACert.crt",
                "BasicSelfIssuedNewKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedNewKeyCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.2 */
    public void testVerifyingPathswithSelfIssuedCertificates_InvalidBasicSelfIssuedOldWithNewTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidBasicSelfIssuedOldWithNewTest2EE.crt",
                "BasicSelfIssuedNewKeyOldWithNewCACert.crt",
                "BasicSelfIssuedNewKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedNewKeyCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.3 */
    public void testVerifyingPathswithSelfIssuedCertificates_ValidBasicSelfIssuedNewWithOldTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidBasicSelfIssuedNewWithOldTest3EE.crt",
                "BasicSelfIssuedOldKeyNewWithOldCACert.crt",
                "BasicSelfIssuedOldKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedOldKeySelfIssuedCertCRL.crl",
                "BasicSelfIssuedOldKeyCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.4 */
    public void testVerifyingPathswithSelfIssuedCertificates_ValidBasicSelfIssuedNewWithOldTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidBasicSelfIssuedNewWithOldTest4EE.crt",
                "BasicSelfIssuedOldKeyNewWithOldCACert.crt",
                "BasicSelfIssuedOldKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedOldKeySelfIssuedCertCRL.crl",
                "BasicSelfIssuedOldKeyCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.5 */
    public void testVerifyingPathswithSelfIssuedCertificates_InvalidBasicSelfIssuedNewWithOldTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidBasicSelfIssuedNewWithOldTest5EE.crt",
                "BasicSelfIssuedOldKeyNewWithOldCACert.crt",
                "BasicSelfIssuedOldKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedOldKeySelfIssuedCertCRL.crl",
                "BasicSelfIssuedOldKeyCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.6 */
    public void testVerifyingPathswithSelfIssuedCertificates_ValidBasicSelfIssuedCRLSigningKeyTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidBasicSelfIssuedCRLSigningKeyTest6EE.crt",
                "BasicSelfIssuedCRLSigningKeyCRLCert.crt",
                "BasicSelfIssuedCRLSigningKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedCRLSigningKeyCRLCertCRL.crl",
                "BasicSelfIssuedCRLSigningKeyCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.7 */
    public void testVerifyingPathswithSelfIssuedCertificates_InvalidBasicSelfIssuedCRLSigningKeyTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidBasicSelfIssuedCRLSigningKeyTest7EE.crt",
                "BasicSelfIssuedCRLSigningKeyCRLCert.crt",
                "BasicSelfIssuedCRLSigningKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedCRLSigningKeyCRLCertCRL.crl",
                "BasicSelfIssuedCRLSigningKeyCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.5.8 */
    public void testVerifyingPathswithSelfIssuedCertificates_InvalidBasicSelfIssuedCRLSigningKeyTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidBasicSelfIssuedCRLSigningKeyTest8EE.crt",
                "BasicSelfIssuedCRLSigningKeyCRLCert.crt",
                "BasicSelfIssuedCRLSigningKeyCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "BasicSelfIssuedCRLSigningKeyCRLCertCRL.crl",
                "BasicSelfIssuedCRLSigningKeyCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.1 */
    public void testVerifyingBasicConstraints_InvalidMissingbasicConstraintsTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidMissingbasicConstraintsTest1EE.crt",
                "MissingbasicConstraintsCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "MissingbasicConstraintsCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.2 */
    public void testVerifyingBasicConstraints_InvalidcAFalseTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidcAFalseTest2EE.crt",
                "basicConstraintsCriticalcAFalseCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "basicConstraintsCriticalcAFalseCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.3 */
    public void testVerifyingBasicConstraints_InvalidcAFalseTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidcAFalseTest3EE.crt",
                "basicConstraintsNotCriticalcAFalseCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "basicConstraintsNotCriticalcAFalseCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.4 */
    public void testVerifyingBasicConstraints_ValidbasicConstraintsNotCriticalTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidbasicConstraintsNotCriticalTest4EE.crt",
                "basicConstraintsNotCriticalCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "basicConstraintsNotCriticalCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.5 */
    public void testVerifyingBasicConstraints_InvalidpathLenConstraintTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidpathLenConstraintTest5EE.crt",
                "pathLenConstraint0subCACert.crt",
                "pathLenConstraint0CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint0CACRL.crl",
                "pathLenConstraint0subCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.6 */
    public void testVerifyingBasicConstraints_InvalidpathLenConstraintTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidpathLenConstraintTest6EE.crt",
                "pathLenConstraint0subCACert.crt",
                "pathLenConstraint0CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint0CACRL.crl",
                "pathLenConstraint0subCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.7 */
    public void testVerifyingBasicConstraints_ValidpathLenConstraintTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidpathLenConstraintTest7EE.crt",
                "pathLenConstraint0CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint0CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.8 */
    public void testVerifyingBasicConstraints_ValidpathLenConstraintTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidpathLenConstraintTest8EE.crt",
                "pathLenConstraint0CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint0CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.9 */
    public void testVerifyingBasicConstraints_InvalidpathLenConstraintTest9() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidpathLenConstraintTest9EE.crt",
                "pathLenConstraint6subsubCA00Cert.crt",
                "pathLenConstraint6subCA0Cert.crt",
                "pathLenConstraint6CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint6CACRL.crl",
                "pathLenConstraint6subCA0CRL.crl",
                "pathLenConstraint6subsubCA00CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.10 */
    public void testVerifyingBasicConstraints_InvalidpathLenConstraintTest10() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidpathLenConstraintTest10EE.crt",
                "pathLenConstraint6subsubCA00Cert.crt",
                "pathLenConstraint6subCA0Cert.crt",
                "pathLenConstraint6CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint6CACRL.crl",
                "pathLenConstraint6subCA0CRL.crl",
                "pathLenConstraint6subsubCA00CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.11 */
    public void testVerifyingBasicConstraints_InvalidpathLenConstraintTest11() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidpathLenConstraintTest11EE.crt",
                "pathLenConstraint6subsubsubCA11XCert.crt",
                "pathLenConstraint6subsubCA11Cert.crt",
                "pathLenConstraint6subCA1Cert.crt",
                "pathLenConstraint6CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint6CACRL.crl",
                "pathLenConstraint6subCA1CRL.crl",
                "pathLenConstraint6subsubCA11CRL.crl",
                "pathLenConstraint6subsubsubCA11XCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.12 */
    public void testVerifyingBasicConstraints_InvalidpathLenConstraintTest12() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidpathLenConstraintTest12EE.crt",
                "pathLenConstraint6subsubsubCA11XCert.crt",
                "pathLenConstraint6subsubCA11Cert.crt",
                "pathLenConstraint6subCA1Cert.crt",
                "pathLenConstraint6CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint6CACRL.crl",
                "pathLenConstraint6subCA1CRL.crl",
                "pathLenConstraint6subsubCA11CRL.crl",
                "pathLenConstraint6subsubsubCA11XCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.13 */
    public void testVerifyingBasicConstraints_ValidpathLenConstraintTest13() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidpathLenConstraintTest13EE.crt",
                "pathLenConstraint6subsubsubCA41XCert.crt",
                "pathLenConstraint6subsubCA41Cert.crt",
                "pathLenConstraint6subCA4Cert.crt",
                "pathLenConstraint6CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint6CACRL.crl",
                "pathLenConstraint6subCA4CRL.crl",
                "pathLenConstraint6subsubCA41CRL.crl",
                "pathLenConstraint6subsubsubCA41XCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.14 */
    public void testVerifyingBasicConstraints_ValidpathLenConstraintTest14() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidpathLenConstraintTest14EE.crt",
                "pathLenConstraint6subsubsubCA41XCert.crt",
                "pathLenConstraint6subsubCA41Cert.crt",
                "pathLenConstraint6subCA4Cert.crt",
                "pathLenConstraint6CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint6CACRL.crl",
                "pathLenConstraint6subCA4CRL.crl",
                "pathLenConstraint6subsubCA41CRL.crl",
                "pathLenConstraint6subsubsubCA41XCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.15 */
    public void testVerifyingBasicConstraints_ValidSelfIssuedpathLenConstraintTest15() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidSelfIssuedpathLenConstraintTest15EE.crt",
                "pathLenConstraint0SelfIssuedCACert.crt",
                "pathLenConstraint0CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint0CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.16 */
    public void testVerifyingBasicConstraints_InvalidSelfIssuedpathLenConstraintTest16() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidSelfIssuedpathLenConstraintTest16EE.crt",
                "pathLenConstraint0subCA2Cert.crt",
                "pathLenConstraint0SelfIssuedCACert.crt",
                "pathLenConstraint0CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint0CACRL.crl",
                "pathLenConstraint0subCA2CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.6.17 */
    public void testVerifyingBasicConstraints_ValidSelfIssuedpathLenConstraintTest17() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidSelfIssuedpathLenConstraintTest17EE.crt",
                "pathLenConstraint1SelfIssuedsubCACert.crt",
                "pathLenConstraint1subCACert.crt",
                "pathLenConstraint1SelfIssuedCACert.crt",
                "pathLenConstraint1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "pathLenConstraint1CACRL.crl",
                "pathLenConstraint1subCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.7.1 */
    public void testKeyUsage_InvalidkeyUsageCriticalkeyCertSignFalseTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidkeyUsageCriticalkeyCertSignFalseTest1EE.crt",
                "keyUsageCriticalkeyCertSignFalseCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "keyUsageCriticalkeyCertSignFalseCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.7.2 */
    public void testKeyUsage_InvalidkeyUsageNotCriticalkeyCertSignFalseTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidkeyUsageNotCriticalkeyCertSignFalseTest2EE.crt",
                "keyUsageNotCriticalkeyCertSignFalseCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "keyUsageNotCriticalkeyCertSignFalseCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.7.3 */
    public void testKeyUsage_ValidkeyUsageNotCriticalTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidkeyUsageNotCriticalTest3EE.crt",
                "keyUsageNotCriticalCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "keyUsageNotCriticalCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.7.4 */
    public void testKeyUsage_InvalidkeyUsageCriticalcRLSignFalseTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidkeyUsageCriticalcRLSignFalseTest4EE.crt",
                "keyUsageCriticalcRLSignFalseCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "keyUsageCriticalcRLSignFalseCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.7.5 */
    public void testKeyUsage_InvalidkeyUsageNotCriticalcRLSignFalseTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidkeyUsageNotCriticalcRLSignFalseTest5EE.crt",
                "keyUsageNotCriticalcRLSignFalseCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "keyUsageNotCriticalcRLSignFalseCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    // skipping sections 4.8 to 4.12

    /** NIST PKITS test 4.13.1 */
    public void testKeyUsage_ValidDNnameConstraintsTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest1EE.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.2 */
    public void testKeyUsage_InvalidDNnameConstraintsTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest2EE.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.3 */
    public void testKeyUsage_InvalidDNnameConstraintsTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest3EE.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.4 */
    public void testKeyUsage_ValidDNnameConstraintsTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest4EE.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.5 */
    public void testKeyUsage_ValidDNnameConstraintsTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest5EE.crt",
                "nameConstraintsDN2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN2CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.6 */
    public void testKeyUsage_ValidDNnameConstraintsTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest6EE.crt",
                "nameConstraintsDN3CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN3CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.7 */
    public void testKeyUsage_InvalidDNnameConstraintsTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest7EE.crt",
                "nameConstraintsDN3CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN3CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.8 */
    public void testKeyUsage_InvalidDNnameConstraintsTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest8EE.crt",
                "nameConstraintsDN4CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN4CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.9 */
    public void testKeyUsage_InvalidDNnameConstraintsTest9() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest9EE.crt",
                "nameConstraintsDN4CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN4CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.10 */
    public void testKeyUsage_InvalidDNnameConstraintsTest10() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest10EE.crt",
                "nameConstraintsDN5CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN5CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.11 */
    public void testKeyUsage_ValidDNnameConstraintsTest11() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest11EE.crt",
                "nameConstraintsDN5CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN5CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.12 */
    public void testKeyUsage_InvalidDNnameConstraintsTest12() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest12EE.crt",
                "nameConstraintsDN1subCA1Cert.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
                "nameConstraintsDN1subCA1CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.13 */
    public void testKeyUsage_InvalidDNnameConstraintsTest13() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest13EE.crt",
                "nameConstraintsDN1subCA2Cert.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
                "nameConstraintsDN1subCA2CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.14 */
    public void testKeyUsage_ValidDNnameConstraintsTest14() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest14EE.crt",
                "nameConstraintsDN1subCA2Cert.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
                "nameConstraintsDN1subCA2CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.15 */
    public void testKeyUsage_InvalidDNnameConstraintsTest15() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest15EE.crt",
                "nameConstraintsDN3subCA1Cert.crt",
                "nameConstraintsDN3CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN3CACRL.crl",
                "nameConstraintsDN3subCA1CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.16 */
    public void testKeyUsage_InvalidDNnameConstraintsTest16() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest16EE.crt",
                "nameConstraintsDN3subCA1Cert.crt",
                "nameConstraintsDN3CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN3CACRL.crl",
                "nameConstraintsDN3subCA1CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.17 */
    public void testKeyUsage_InvalidDNnameConstraintsTest17() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest17EE.crt",
                "nameConstraintsDN3subCA2Cert.crt",
                "nameConstraintsDN3CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN3CACRL.crl",
                "nameConstraintsDN3subCA2CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.18 */
    public void testKeyUsage_ValidDNnameConstraintsTest18() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest18EE.crt",
                "nameConstraintsDN3subCA2Cert.crt",
                "nameConstraintsDN3CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN3CACRL.crl",
                "nameConstraintsDN3subCA2CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.19 */
    public void testKeyUsage_ValidSelfIssuedDNnameConstraintsTest19() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNnameConstraintsTest19EE.crt",
                "nameConstraintsDN1SelfIssuedCACert.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.20 */
    public void testKeyUsage_InvalidSelfIssuedDNnameConstraintsTest20() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNnameConstraintsTest20EE.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.21 */
    public void testKeyUsage_ValidRFC822nameConstraintsTest21() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidRFC822nameConstraintsTest21EE.crt",
                "nameConstraintsRFC822CA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsRFC822CA1CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.22 */
    public void testKeyUsage_InvalidRFC822nameConstraintsTest22() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidRFC822nameConstraintsTest22EE.crt",
                "nameConstraintsRFC822CA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsRFC822CA1CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.23 */
    public void testKeyUsage_ValidRFC822nameConstraintsTest23() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidRFC822nameConstraintsTest23EE.crt",
                "nameConstraintsRFC822CA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsRFC822CA2CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.24 */
    public void testKeyUsage_InvalidRFC822nameConstraintsTest24() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidRFC822nameConstraintsTest24EE.crt",
                "nameConstraintsRFC822CA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsRFC822CA2CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.25 */
    public void testKeyUsage_ValidRFC822nameConstraintsTest25() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidRFC822nameConstraintsTest25EE.crt",
                "nameConstraintsRFC822CA3Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsRFC822CA3CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.26 */
    public void testKeyUsage_InvalidRFC822nameConstraintsTest26() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidRFC822nameConstraintsTest26EE.crt",
                "nameConstraintsRFC822CA3Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsRFC822CA3CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.27 */
    public void testKeyUsage_ValidDNandRFC822nameConstraintsTest27() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNandRFC822nameConstraintsTest27EE.crt",
                "nameConstraintsDN1subCA3Cert.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
                "nameConstraintsDN1subCA3CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.28 */
    public void testKeyUsage_InvalidDNandRFC822nameConstraintsTest28() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNandRFC822nameConstraintsTest28EE.crt",
                "nameConstraintsDN1subCA3Cert.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
                "nameConstraintsDN1subCA3CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.29 */
    public void testKeyUsage_InvalidDNandRFC822nameConstraintsTest29() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNandRFC822nameConstraintsTest29EE.crt",
                "nameConstraintsDN1subCA3Cert.crt",
                "nameConstraintsDN1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDN1CACRL.crl",
                "nameConstraintsDN1subCA3CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.30 */
    public void testKeyUsage_ValidDNSnameConstraintsTest30() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNSnameConstraintsTest30EE.crt",
                "nameConstraintsDNS1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDNS1CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.31 */
    public void testKeyUsage_InvalidDNSnameConstraintsTest31() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNSnameConstraintsTest31EE.crt",
                "nameConstraintsDNS1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDNS1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.32 */
    public void testKeyUsage_ValidDNSnameConstraintsTest32() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidDNSnameConstraintsTest32EE.crt",
                "nameConstraintsDNS2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDNS2CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.33 */
    public void testKeyUsage_InvalidDNSnameConstraintsTest33() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNSnameConstraintsTest33EE.crt",
                "nameConstraintsDNS2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDNS2CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.34 */
    public void testKeyUsage_ValidURInameConstraintsTest34() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidURInameConstraintsTest34EE.crt",
                "nameConstraintsURI1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsURI1CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.35 */
    public void testKeyUsage_InvalidURInameConstraintsTest35() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidURInameConstraintsTest35EE.crt",
                "nameConstraintsURI1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsURI1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.36 */
    public void testKeyUsage_ValidURInameConstraintsTest36() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidURInameConstraintsTest36EE.crt",
                "nameConstraintsURI2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsURI2CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.37 */
    public void testKeyUsage_InvalidURInameConstraintsTest37() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidURInameConstraintsTest37EE.crt",
                "nameConstraintsURI2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsURI2CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.13.38 */
    public void testKeyUsage_InvalidDNSnameConstraintsTest38() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidDNSnameConstraintsTest38EE.crt",
                "nameConstraintsDNS1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "nameConstraintsDNS1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.1 */
    public void testDistributionPoints_ValiddistributionPointTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddistributionPointTest1EE.crt",
                "distributionPoint1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint1CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.2 */
    public void testDistributionPoints_InvaliddistributionPointTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddistributionPointTest2EE.crt",
                "distributionPoint1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.3 */
    public void testDistributionPoints_InvaliddistributionPointTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddistributionPointTest3EE.crt",
                "distributionPoint1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint1CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.4 */
    public void testDistributionPoints_ValiddistributionPointTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddistributionPointTest4EE.crt",
                "distributionPoint1CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint1CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.5 */
    public void testDistributionPoints_ValiddistributionPointTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddistributionPointTest5EE.crt",
                "distributionPoint2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint2CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.6 */
    public void testDistributionPoints_InvaliddistributionPointTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddistributionPointTest6EE.crt",
                "distributionPoint2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint2CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.7 */
    public void testDistributionPoints_ValiddistributionPointTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddistributionPointTest7EE.crt",
                "distributionPoint2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint2CACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.8 */
    public void testDistributionPoints_InvaliddistributionPointTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddistributionPointTest8EE.crt",
                "distributionPoint2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint2CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.9 */
    public void testDistributionPoints_InvaliddistributionPointTest9() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddistributionPointTest9EE.crt",
                "distributionPoint2CACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "distributionPoint2CACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.10 */
    public void testDistributionPoints_ValidNoissuingDistributionPointTest10() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidNoissuingDistributionPointTest10EE.crt",
                "NoissuingDistributionPointCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "NoissuingDistributionPointCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.11 */
    public void testDistributionPoints_InvalidonlyContainsUserCertsCRLTest11() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlyContainsUserCertsTest11EE.crt",
                "onlyContainsUserCertsCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlyContainsUserCertsCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.12 */
    public void testDistributionPoints_InvalidonlyContainsCACertsCRLTest12() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlyContainsCACertsTest12EE.crt",
                "onlyContainsCACertsCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlyContainsCACertsCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.13 */
    public void testDistributionPoints_ValidonlyContainsCACertsCRLTest13() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidonlyContainsCACertsTest13EE.crt",
                "onlyContainsCACertsCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlyContainsCACertsCACRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.14 */
    public void testDistributionPoints_InvalidonlyContainsAttributeCertsTest14() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlyContainsAttributeCertsTest14EE.crt",
                "onlyContainsAttributeCertsCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlyContainsAttributeCertsCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.15 */
    public void testDistributionPoints_InvalidonlySomeReasonsTest15() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlySomeReasonsTest15EE.crt",
                "onlySomeReasonsCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlySomeReasonsCA1compromiseCRL.crl",
                "onlySomeReasonsCA1otherreasonsCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.16 */
    public void testDistributionPoints_InvalidonlySomeReasonsTest16() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlySomeReasonsTest16EE.crt",
                "onlySomeReasonsCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlySomeReasonsCA1compromiseCRL.crl",
                "onlySomeReasonsCA1otherreasonsCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.17 */
    public void testDistributionPoints_InvalidonlySomeReasonsTest17() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlySomeReasonsTest17EE.crt",
                "onlySomeReasonsCA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlySomeReasonsCA2CRL1.crl",
                "onlySomeReasonsCA2CRL2.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.18 */
    public void testDistributionPoints_ValidonlySomeReasonsTest18() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidonlySomeReasonsTest18EE.crt",
                "onlySomeReasonsCA3Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlySomeReasonsCA3compromiseCRL.crl",
                "onlySomeReasonsCA3otherreasonsCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.19 */
    public void testDistributionPoints_ValidonlySomeReasonsTest19() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidonlySomeReasonsTest19EE.crt",
                "onlySomeReasonsCA4Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlySomeReasonsCA4compromiseCRL.crl",
                "onlySomeReasonsCA4otherreasonsCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.20 */
    public void testDistributionPoints_InvalidonlySomeReasonsTest20() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlySomeReasonsTest20EE.crt",
                "onlySomeReasonsCA4Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlySomeReasonsCA4compromiseCRL.crl",
                "onlySomeReasonsCA4otherreasonsCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.21 */
    public void testDistributionPoints_InvalidonlySomeReasonsTest21() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidonlySomeReasonsTest21EE.crt",
                "onlySomeReasonsCA4Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "onlySomeReasonsCA4compromiseCRL.crl",
                "onlySomeReasonsCA4otherreasonsCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.22 */
    public void testDistributionPoints_ValidIDPwithindirectCRLTest22() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidIDPwithindirectCRLTest22EE.crt",
                "indirectCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA1CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.23 */
    public void testDistributionPoints_InvalidIDPwithindirectCRLTest23() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidIDPwithindirectCRLTest23EE.crt",
                "indirectCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA1CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.24 */
    public void testDistributionPoints_ValidIDPwithindirectCRLTest24() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidIDPwithindirectCRLTest24EE.crt",
                "indirectCRLCA1Cert.crt",
                "indirectCRLCA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA1CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.25 */
    public void testDistributionPoints_ValidIDPwithindirectCRLTest25() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidIDPwithindirectCRLTest25EE.crt",
                "indirectCRLCA1Cert.crt",
                "indirectCRLCA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA1CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.26 */
    public void testDistributionPoints_InvalidIDPwithindirectCRLTest26() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidIDPwithindirectCRLTest26EE.crt",
                "indirectCRLCA1Cert.crt",
                "indirectCRLCA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA1CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.27 */
    public void testDistributionPoints_InvalidcRLIssuerTest27() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidcRLIssuerTest27EE.crt",
                "GoodCACert.crt",
                "indirectCRLCA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "GoodCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.28 */
    public void testDistributionPoints_ValidcRLIssuerTest28() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidcRLIssuerTest28EE.crt",
                "indirectCRLCA3cRLIssuerCert.crt",
                "indirectCRLCA3Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA3CRL.crl",
                "indirectCRLCA3cRLIssuerCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.29 */
    public void testDistributionPoints_ValidcRLIssuerTest29() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidcRLIssuerTest29EE.crt",
                "indirectCRLCA3cRLIssuerCert.crt",
                "indirectCRLCA3Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA3CRL.crl",
                "indirectCRLCA3cRLIssuerCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.30 */
    public void testDistributionPoints_ValidcRLIssuerTest30() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidcRLIssuerTest30EE.crt",
                "indirectCRLCA4cRLIssuerCert.crt",
                "indirectCRLCA4Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA4cRLIssuerCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.31 */
    public void testDistributionPoints_InvalidcRLIssuerTest31() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidcRLIssuerTest31EE.crt",
                "indirectCRLCA6Cert.crt",
                "indirectCRLCA5Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA5CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.32 */
    public void testDistributionPoints_InvalidcRLIssuerTest32() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidcRLIssuerTest32EE.crt",
                "indirectCRLCA6Cert.crt",
                "indirectCRLCA5Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA5CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.33 */
    public void testDistributionPoints_ValidcRLIssuerTest33() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidcRLIssuerTest33EE.crt",
                "indirectCRLCA6Cert.crt",
                "indirectCRLCA5Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA5CRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.34 */
    public void testDistributionPoints_InvalidcRLIssuerTest34() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidcRLIssuerTest34EE.crt",
                "indirectCRLCA5Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA5CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.14.35 */
    public void testDistributionPoints_InvalidcRLIssuerTest35() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvalidcRLIssuerTest35EE.crt",
                "indirectCRLCA5Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "indirectCRLCA5CRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.1 */
    public void testDeltaCRLs_InvaliddeltaCRLIndicatorNoBaseTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddeltaCRLIndicatorNoBaseTest1EE.crt",
                "deltaCRLIndicatorNoBaseCACert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLIndicatorNoBaseCACRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.2 */
    public void testDeltaCRLs_ValiddeltaCRLTest2() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddeltaCRLTest2EE.crt",
                "deltaCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA1CRL.crl",
                "deltaCRLCA1deltaCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.3 */
    public void testDeltaCRLs_InvaliddeltaCRLTest3() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddeltaCRLTest3EE.crt",
                "deltaCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA1CRL.crl",
                "deltaCRLCA1deltaCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.4 */
    public void testDeltaCRLs_InvaliddeltaCRLTest4() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddeltaCRLTest4EE.crt",
                "deltaCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA1CRL.crl",
                "deltaCRLCA1deltaCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.5 */
    public void testDeltaCRLs_ValiddeltaCRLTest5() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddeltaCRLTest5EE.crt",
                "deltaCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA1CRL.crl",
                "deltaCRLCA1deltaCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.6 */
    public void testDeltaCRLs_InvaliddeltaCRLTest6() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddeltaCRLTest6EE.crt",
                "deltaCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA1CRL.crl",
                "deltaCRLCA1deltaCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.7 */
    public void testDeltaCRLs_ValiddeltaCRLTest7() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddeltaCRLTest7EE.crt",
                "deltaCRLCA1Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA1CRL.crl",
                "deltaCRLCA1deltaCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.8 */
    public void testDeltaCRLs_ValiddeltaCRLTest8() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValiddeltaCRLTest8EE.crt",
                "deltaCRLCA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA2CRL.crl",
                "deltaCRLCA2deltaCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.9 */
    public void testDeltaCRLs_InvaliddeltaCRLTest9() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddeltaCRLTest9EE.crt",
                "deltaCRLCA2Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA2CRL.crl",
                "deltaCRLCA2deltaCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.15.10 */
    public void testDeltaCRLs_InvaliddeltaCRLTest10() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "InvaliddeltaCRLTest10EE.crt",
                "deltaCRLCA3Cert.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
                "deltaCRLCA3CRL.crl",
                "deltaCRLCA3deltaCRL.crl",
        };

        assertInvalidPath(trustAnchor, certs, crls);
    }

    /** NIST PKITS test 4.16.1 */
    public void testPrivateCertificateExtensions_ValidUnknownNotCriticalCertificateExtensionTest1() throws Exception {
        String trustAnchor = "TrustAnchorRootCertificate.crt";

        String[] certs = {
                "ValidUnknownNotCriticalCertificateExtensionTest1EE.crt",
        };

        String[] crls = {
                "TrustAnchorRootCRL.crl",
        };

        assertValidPath(trustAnchor, certs, crls);
    }

    /* DO NOT MANUALLY EDIT -- END AUTOMATICALLY GENERATED TESTS */
}

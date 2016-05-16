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

package org.apache.harmony.xnet.tests.javax.net.ssl;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPermission;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.harmony.security.fortress.Services;
import org.apache.harmony.xnet.tests.support.SpiEngUtils;
import org.apache.harmony.xnet.tests.support.MySSLContextSpi;
import junit.framework.TestCase;

/**
 * Tests for <code>SSLContext</code> class constructors and methods.
 * 
 */

public class SSLContext1Test extends TestCase {

    private static String srvSSLContext = "SSLContext";

    public static String defaultProtocol = "TLS";

    private static final String NotSupportMsg = "Default protocol is not supported";

    private static String defaultProviderName = null;

    private static Provider defaultProvider = null;

    private static final String[] invalidValues = SpiEngUtils.invalidValues;

    private static boolean DEFSupported = false;

    private static final String NotSupportedMsg = "There is no suitable provider for SSLContext";

    private static String[] validValues = new String[3];
    static {
        defaultProvider = SpiEngUtils.isSupport(defaultProtocol, srvSSLContext);
        DEFSupported = (defaultProvider != null);
        if (DEFSupported) {
            defaultProviderName = (DEFSupported ? defaultProvider.getName()
                    : null);
            validValues[0] = defaultProtocol;
            validValues[1] = defaultProtocol.toUpperCase();
            validValues[2] = defaultProtocol.toLowerCase();
        } else {
            defaultProtocol = null;
        }
        
        SSLParameters staticSupportSSLParameter = new SSLParameters(new String[] {
                "TLS_RSA_WITH_RC4_128_MD5", "TLS_RSA_WITH_RC4_128_SHA" },
                new String[] { "TLSv1", "SSLv3" });
        
        SSLParameters staticDefaultSSLParameter = new SSLParameters(new String[] {
                "TLS_RSA_WITH_RC4_128_MD5", "TLS_RSA_WITH_RC4_128_SHA" },
                new String[] { "TLSv1", "SSLv3" });
    }

    protected SSLContext[] createSSLCon() {
        if (!DEFSupported) {
            fail(defaultProtocol + " protocol is not supported");
            return null;
        }
        SSLContext[] sslC = new SSLContext[3];
        try {
            sslC[0] = SSLContext.getInstance(defaultProtocol);
            sslC[1] = SSLContext.getInstance(defaultProtocol, defaultProvider);
            sslC[2] = SSLContext.getInstance(defaultProtocol,
                    defaultProviderName);
            return sslC;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Test for <code>getInstance(String protocol)</code> method Assertion:
     * returns SSLContext object
     */
    public void testSSLContext01() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        SSLContext sslContext;
        for (int i = 0; i < validValues.length; i++) {
            sslContext = SSLContext.getInstance(validValues[i]);
            assertTrue("Not SSLContext object",
                    sslContext instanceof SSLContext);
            assertEquals("Invalid protocol", sslContext.getProtocol(),
                    validValues[i]);
        }
    }

    /**
     * Test for <code>getInstance(String protocol)</code> method Assertion:
     * throws NullPointerException when protocol is null; throws
     * NoSuchAlgorithmException when protocol is not correct;
     */
    public void testSSLContext02() {
        try {
            SSLContext.getInstance(null);
            fail("NoSuchAlgorithmException or NullPointerException should be thrown (protocol is null");
        } catch (NoSuchAlgorithmException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                SSLContext.getInstance(invalidValues[i]);
                fail("NoSuchAlgorithmException was not thrown as expected for provider: "
                        .concat(invalidValues[i]));
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String protocol, String provider)</code>
     * method Assertion: throws IllegalArgumentException when provider is null
     * or empty
     */
    public void testSSLContext03() throws NoSuchProviderException,
            NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        String provider = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                SSLContext.getInstance(defaultProtocol, provider);
                fail("IllegalArgumentException must be thrown when provider is null");
            } catch (IllegalArgumentException e) {
            }
            try {
                SSLContext.getInstance(defaultProtocol, "");
                fail("IllegalArgumentException must be thrown when provider is empty");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String protocol, String provider)</code>
     * method Assertion: throws NullPointerException when protocol is null;
     * throws NoSuchAlgorithmException when protocol is not correct;
     */
    public void testSSLContext04() throws NoSuchProviderException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        try {
            SSLContext.getInstance(null, defaultProviderName);
            fail("NoSuchAlgorithmException or NullPointerException should be thrown (protocol is null");
        } catch (NoSuchAlgorithmException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                SSLContext.getInstance(invalidValues[i], defaultProviderName);
                fail("NoSuchAlgorithmException was not thrown as expected (protocol: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String protocol, String provider)</code>
     * method Assertion: throws NoSuchProviderException when provider has
     * invalid value
     */
    public void testSSLContext05() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        for (int i = 1; i < invalidValues.length; i++) {
            for (int j = 0; j < validValues.length; j++) {
                try {
                    SSLContext.getInstance(validValues[j], invalidValues[i]);
                    fail("NuSuchProviderException must be thrown (protocol: "
                            .concat(validValues[j]).concat(" provider: ")
                            .concat(invalidValues[i]).concat(")"));
                } catch (NoSuchProviderException e) {
                }
            }
        }
    }

    /**
     * Test for <code>getInstance(String protocol, String provider)</code>
     * method Assertion: returns instance of SSLContext
     */
    public void testSSLContext06() throws NoSuchAlgorithmException,
            NoSuchProviderException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        SSLContext sslContext;
        for (int i = 0; i < validValues.length; i++) {
            sslContext = SSLContext.getInstance(validValues[i],
                    defaultProviderName);
            assertTrue("Not SSLContext object",
                    sslContext instanceof SSLContext);
            assertEquals("Invalid protocol", sslContext.getProtocol(),
                    validValues[i]);
            assertEquals("Invalid provider", sslContext.getProvider(),
                    defaultProvider);
        }
    }

    /**
     * Test for <code>getInstance(String protocol, Provider provider)</code>
     * method Assertion: throws IllegalArgumentException when provider is null
     */
    public void testSSLContext07() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        Provider provider = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                SSLContext.getInstance(validValues[i], provider);
                fail("IllegalArgumentException must be thrown when provider is null");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String protocol, Provider provider)</code>
     * method Assertion: throws NullPointerException when protocol is null;
     * throws NoSuchAlgorithmException when protocol is not correct;
     */
    public void testSSLContext08() {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        try {
            SSLContext.getInstance(null, defaultProvider);
            fail("NoSuchAlgorithmException or NullPointerException should be thrown (protocol is null");
        } catch (NoSuchAlgorithmException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                SSLContext.getInstance(invalidValues[i], defaultProvider);
                fail("Expected NoSuchAlgorithmException was not thrown as expected");
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String protocol, Provider provider)</code>
     * method Assertion: returns instance of SSLContext
     */
    public void testSSLContext09() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        SSLContext sslContext;
        for (int i = 0; i < validValues.length; i++) {
            sslContext = SSLContext
                    .getInstance(validValues[i], defaultProvider);
            assertTrue("Not SSLContext object",
                    sslContext instanceof SSLContext);
            assertEquals("Invalid protocol", sslContext.getProtocol(),
                    validValues[i]);
            assertEquals("Invalid provider", sslContext.getProvider(),
                    defaultProvider);
        }
    }

    /**
     * Test for <code>getClientSessionContext()</code>
     * <code>getServiceSessionContext()</code>
     * methods Assertion: returns correspondent object
     */
    public void testSSLContext10() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        SSLContext[] sslC = createSSLCon();
        assertNotNull("SSLContext objects were not created", sslC);
        for (int i = 0; i < sslC.length; i++) {
            assertTrue(sslC[i].getClientSessionContext() instanceof SSLSessionContext);
            assertTrue(sslC[i].getServerSessionContext() instanceof SSLSessionContext);
        }
    }

    /**
     * Test for <code>getServerSocketFactory()</code>
     * <code>getSocketFactory()</code>
     * <code>init(KeyManager[] km, TrustManager[] tm, SecureRandom random)</code>
     * methods Assertion: returns correspondent object
     * 
     */
    
     public void testSSLContext11() throws NoSuchAlgorithmException,
            KeyManagementException, KeyStoreException,
            UnrecoverableKeyException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        SSLContext[] sslC = createSSLCon();
        assertNotNull("SSLContext objects were not created", sslC);
        String tAlg = TrustManagerFactory.getDefaultAlgorithm();
        String kAlg = KeyManagerFactory.getDefaultAlgorithm();
        if (tAlg == null) {
            fail("TrustManagerFactory default algorithm is not defined");
            return;
        }
        if (kAlg == null) {
            fail("KeyManagerFactory default algorithm is not defined");
            return;
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(kAlg);
        KeyStore ks = null;
        kmf.init(ks, new char[10]);
        KeyManager[] kms = kmf.getKeyManagers();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tAlg);
        tmf.init(ks);
        TrustManager[] tms = tmf.getTrustManagers();
        for (int i = 0; i < sslC.length; i++) {
            sslC[i].init(kms, tms, new SecureRandom());
            assertTrue(sslC[i].getServerSocketFactory() instanceof SSLServerSocketFactory);
            assertTrue(sslC[i].getSocketFactory() instanceof SSLSocketFactory);
        }
    }

    /**
     * Test for <code>SSLContext</code> constructor Assertion: returns
     * SSLContext object
     */
    public void testSSLContext12() throws NoSuchAlgorithmException,
            KeyManagementException {
        if (!DEFSupported) {
            fail(NotSupportMsg);
            return;
        }
        SSLContextSpi spi = new MySSLContextSpi();
        SSLContext sslContext = new MySSLContext(spi, defaultProvider,
                defaultProtocol);
        assertTrue("Not CertStore object", sslContext instanceof SSLContext);
        assertEquals("Incorrect protocol", sslContext.getProtocol(),
                defaultProtocol);
        assertEquals("Incorrect provider", sslContext.getProvider(),
                defaultProvider);
        TrustManager[] tm = null;
        KeyManager[] km = null;
        sslContext.init(km, tm, new SecureRandom());
        assertTrue(sslContext.createSSLEngine() instanceof SSLEngine);
        assertTrue(sslContext.createSSLEngine("host host", 8888) instanceof SSLEngine);
        try {
            sslContext.init(km, tm, null);
            fail("KeyManagementException should be thrown for null SEcureRandom");
        } catch (KeyManagementException e) {
        }

        sslContext = new MySSLContext(null, null, null);
        assertTrue("Not CertStore object", sslContext instanceof SSLContext);
        assertNull("Incorrect protocol", sslContext.getProtocol());
        assertNull("Incorrect provider", sslContext.getProvider());
        try {
            sslContext.createSSLEngine();
            fail("NullPointerException should be thrown");
        } catch (NullPointerException e) {
        }
        try {
            sslContext.getSocketFactory();
            fail("NullPointerException should be thrown");
        } catch (NullPointerException e) {
        }
    }
    
    public void testGetDefault() throws Exception {
        //TODO: Need evaluation
        class PrivateClassLoader extends ClassLoader {}
        try {
            // register my provider and its service.
            Security.addProvider(new MyProvider());
            // FIXME 
            ClassLoader privateClassLoader = new PrivateClassLoader();
            Class class1 = privateClassLoader
                    .loadClass("org.apache.harmony.xnet.tests.javax.net.ssl.MySSLContext");
            SSLContext sslContext = (SSLContext) class1.newInstance();      
            System.out.println(SSLContext.getInstance("Default"));
            assertTrue((sslContext.getDefault()) instanceof SSLContext);
        } catch (NoSuchAlgorithmException e) {
            // expected            
        }
    }    
    


    
    
    public void testGetDefaultSSLParameters() throws Exception {
        SSLContext[] sslContexts = createSSLCon();
        assertNotNull("SSLContext objects were not created", sslContexts);

        for (int i = 0; i < sslContexts.length; i++) {
            sslContexts[i].init(null, null, null);
            SSLParameters defaultSSLParameters = sslContexts[i]
                    .getDefaultSSLParameters();
            SSLSocket sslSocket = (SSLSocket) (sslContexts[i]
                    .getSocketFactory().createSocket());

            String[] enabledCipherSuites = sslSocket.getEnabledCipherSuites();
            String[] enabledProtocols = sslSocket.getEnabledProtocols();

            for (int j = 0; j < enabledCipherSuites.length; j++)
                assertEquals((defaultSSLParameters.getCipherSuites())[j],
                        enabledCipherSuites[j]);
            for (int k = 0; k < enabledProtocols.length; k++)
                assertEquals((defaultSSLParameters.getProtocols())[k],
                        enabledProtocols[k]);
        }
    }

    public void testGetSupportedSSLParameters() throws Exception {
        SSLContext[] sslContexts = createSSLCon();
        assertNotNull("SSLContext objects were not created", sslContexts);

        for (int i = 0; i < sslContexts.length; i++) {
            sslContexts[i].init(null, null, null);
            SSLParameters defaultSSLParameters = sslContexts[i]
                    .getSupportedSSLParameters();
            SSLSocket sslSocket = (SSLSocket) (sslContexts[i]
                    .getSocketFactory().createSocket());
            String[] supportedCipherSuites = sslSocket.getSupportedCipherSuites();
            String[] supportedProtocols = sslSocket.getSupportedProtocols();

            for (int j = 0; j < supportedCipherSuites.length; j++)
                assertEquals((defaultSSLParameters.getCipherSuites())[j],
                        supportedCipherSuites[j]);
            for (int k = 0; k < supportedProtocols.length; k++)
                assertEquals((defaultSSLParameters.getProtocols())[k],
                        supportedProtocols[k]);
        }
    }
}

/**
 * Addifional class to verify SSLContext constructor
 */
class MyProvider extends Provider {    
    MyProvider() {
        super("MyProviderForSSLContextTest", 1.0, "Provider for testing");
        put("SSLContext.Default", "org.apache.harmony.xnet.tests.javax.net.ssl.MySSLContext");
    }
}
    
class MySSLContext extends SSLContext {
    public MySSLContext(SSLContextSpi spi, Provider prov, String alg) {
        super(spi, prov, alg);
    }
    
    public MySSLContext(){
        super(null, null, null);
    }
}

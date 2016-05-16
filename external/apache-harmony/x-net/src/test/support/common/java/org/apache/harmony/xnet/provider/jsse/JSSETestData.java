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

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import tests.support.resource.Support_Resources;

/**
 * JSSETestData
 */
public class JSSETestData {

    private static Exception initException;

    // the password to the store
    public static final char[] KS_PASSWORD = "password".toCharArray();

    private static SSLContext context;
    private static KeyStore keyStore;
    private static SSLParameters sslParameters;

    static {
        try {
            String ksDefaultType = KeyStore.getDefaultType();
            InputStream is = Support_Resources.getResourceStream(
                    "key_store." + ksDefaultType.toLowerCase());

            keyStore = KeyStore.getInstance(ksDefaultType);
            keyStore.load(is, KS_PASSWORD);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(keyStore, KS_PASSWORD);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keyStore);

            sslParameters = new SSLParameters(kmf.getKeyManagers(), tmf
                    .getTrustManagers(), new SecureRandom(),
                    new SSLSessionContextImpl(), new SSLSessionContextImpl());

            context = SSLContext.getInstance("TLSv1");
            context.init(kmf.getKeyManagers(),
                    tmf.getTrustManagers(), new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
            initException = e;
        }
    }

    public static SSLContext getContext() throws Exception {
        if (initException != null) {
            throw initException;
        }
        return context;
    }

    public static SSLParameters getSSLParameters() throws Exception {
        if (initException != null) {
            throw initException;
        }
        return sslParameters;
    }

    public static KeyStore getKeyStore() throws Exception {
        if (initException != null) {
            throw initException;
        }
        return keyStore;
    }
}

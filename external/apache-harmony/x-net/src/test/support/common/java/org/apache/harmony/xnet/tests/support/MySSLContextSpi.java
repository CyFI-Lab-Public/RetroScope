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

package org.apache.harmony.xnet.tests.support;

import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Additional class for verification of SSLContextSpi and SSLContext
 * functionality
 * 
 */

public class MySSLContextSpi extends SSLContextSpi {
    private boolean init = false;
    @Override
    protected void engineInit(KeyManager[] km, TrustManager[] tm,
            SecureRandom sr) throws KeyManagementException {
        if (sr == null) {
            throw new KeyManagementException(
                    "secureRandom is null");
        }
        init = true;
    }

    @Override
    protected SSLSocketFactory engineGetSocketFactory() {
        if (!init) {
            throw new RuntimeException("Not initialiazed");
        };   
        return null;
    }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        if (!init) {
            throw new RuntimeException("Not initialiazed");
        }
        return null;
    }

    @Override
    protected SSLSessionContext engineGetServerSessionContext() {
        if (!init) {
            throw new RuntimeException("Not initialiazed");
        }
        return null;
    }

    @Override
    protected SSLSessionContext engineGetClientSessionContext() {
        if (!init) {
            throw new RuntimeException("Not initialiazed");
        }
        return null;
    }

    /*
     * FIXME: add these methods
     */   
    @Override
    protected SSLEngine engineCreateSSLEngine(String host, int port) {
        if (!init) {
            throw new RuntimeException("Not initialiazed");
        }
        return new tmpSSLEngine(host, port);
    }

    @Override
    protected SSLEngine engineCreateSSLEngine() {
        if (!init) {
            throw new RuntimeException("Not initialiazed");
        }
        return new tmpSSLEngine();
    }
    
    public class tmpSSLEngine extends SSLEngine {
        String tmpHost;
        int tmpPort;
        public tmpSSLEngine() {
            tmpHost = null;
            tmpPort = 0;        
        }
        public tmpSSLEngine(String host, int port) {
            tmpHost = host;
            tmpPort = port;        
        }
        @Override
        public String getPeerHost() {
            return tmpHost;        
        }
        @Override
        public int getPeerPort() {
            return tmpPort;
        }
        @Override
        public void beginHandshake() throws SSLException { }
        @Override
        public void closeInbound() throws SSLException { }
        @Override
        public void closeOutbound() {}
        @Override
        public Runnable getDelegatedTask() { return null; }
        @Override
        public String[] getEnabledCipherSuites() { return null; }
        @Override
        public String[] getEnabledProtocols() {return null; }
        @Override
        public boolean getEnableSessionCreation() { return true; }
        @Override
        public SSLEngineResult.HandshakeStatus getHandshakeStatus() { return null; };
        @Override
        public boolean getNeedClientAuth() { return true; }
        @Override
        public SSLSession getSession() { return null; }
        @Override
        public String[] getSupportedCipherSuites()  { return null; }
        @Override
        public String[] getSupportedProtocols()  { return null; }
        @Override
        public boolean getUseClientMode()  { return true; }
        @Override
        public boolean getWantClientAuth()  { return true; }
        @Override
        public boolean isInboundDone()  { return true; }
        @Override
        public boolean isOutboundDone()  { return true; }
        @Override
        public void setEnabledCipherSuites(String[] suites) { }
        @Override
        public void setEnabledProtocols(String[] protocols) { }
        @Override
        public void setEnableSessionCreation(boolean flag) { }
        @Override
        public void setNeedClientAuth(boolean need) { }
        @Override
        public void setUseClientMode(boolean mode) { }
        @Override
        public void setWantClientAuth(boolean want) { }        
        @Override
        public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts,
                int offset, int length) throws SSLException {
            return null;
        }        
        @Override
        public SSLEngineResult wrap(ByteBuffer[] srcs, int offset,
                int length, ByteBuffer dst) throws SSLException { 
            return null;
        }

        @Override
        public SSLParameters getSSLParameters() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setSSLParameters(SSLParameters sslP) {
            // TODO Auto-generated method stub

        }
    }    
    
    @Override
    protected SSLParameters engineGetDefaultSSLParameters() {
        return new SSLParameters(new String[] { "Default_SSL_Parameters_For_Test1" },
                new String[] { "TLSv1" });
    }

    @Override
    protected SSLParameters engineGetSupportedSSLParameters() {
        return new SSLParameters(new String[] { "Default_SSL_Parameters_For_Test1",
                "Default_SSL_Parameters_For_Test2" }, new String[] { "TLSv1", "SSLv3" });
    }
}
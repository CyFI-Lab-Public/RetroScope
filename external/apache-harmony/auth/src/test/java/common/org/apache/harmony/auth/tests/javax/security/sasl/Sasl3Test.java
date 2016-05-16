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
* @author Vera Y. Petrashkova
*/

package org.apache.harmony.auth.tests.javax.security.sasl;

import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

import junit.framework.TestCase;

import org.apache.harmony.auth.tests.support.SpiEngUtils;

/**
 * Test for Sasl class
 */
public class Sasl3Test extends TestCase {
    private static final String CLNTSRV = "SaslClientFactory.";

    private static final String fClientClass = mySaslClientFactory.class.getName();;

    private Provider [] provs;
    private boolean initProvs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!initProvs) {
            provs = Security.getProviders();
            initProvs = true;
        }
        if (provs != null) {
            for (Provider element : provs) {
                Security.removeProvider(element.getName());
            }
        }
    }

    protected Provider[] mProv;

    private void addProviders() {
        for (Provider element : mProv) {
            Security.insertProviderAt(element, 1);
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mProv != null) {
            for (Provider element : mProv) {
                Security.removeProvider(element.getName());
            }
        }
        if (provs != null) {
            for (int i = 0; i < provs.length; i++) {
                Security.insertProviderAt(provs[i], (i+1));
            }
        }
    }

    /**
     * Test for <code>createSaslClient(String[] mechanisms, 
     *      String authanticationID, String protocol, String serverName,
     *      Map prop, CallbackHandler cbh))</code>
     * method Assertions: throws NullPointerException when mechanisms is null;
     * throws SaslException when parameters (protocol, cbh, mechanisms) are
     * wrong.
     * 
     * All providers are previously removed and 2 new providers were added.
     */
    public void testCreateClient01() throws SaslException {
        mProv = new Provider[] {
                (new SpiEngUtils()).new MyProvider("MySaslClientProvider1",
                        "Testing provider SaslClientFactory - 1", CLNTSRV
                                .concat("NAME-1"), fClientClass),
                (new SpiEngUtils()).new MyProvider("MySaslClientProvider2",
                        "Testing provider SaslClientFactory - 2", CLNTSRV
                                .concat("NAME-2"), fClientClass) };
        addProviders();

        CallbackHandler cbH = new cbHand();
        try {
            Sasl.createSaslClient(null, null, null, null, null, cbH);
            fail("NullPointerException should be thrown when mechanisms is null");
        } catch (NullPointerException e) {
        }
        try {
            Sasl.createSaslClient(new String[] { "NAME-2" }, null, "protocol",
                    null, null, cbH);
            fail("SaslException should be thrown when CallbackHandler is wrong");
        } catch (SaslException e) {
        }
        cbH = new cbHandN();
        try {
            Sasl.createSaslClient(new String[] { "NAME-1" }, null, "protocol",
                    null, null, cbH);
            fail("SaslException should be thrown when mechanisms is wrong");
        } catch (SaslException e) {
        }
        try {
            Sasl.createSaslClient(new String[] { "NAME-2" }, null, null, null,
                    null, cbH);
            fail("SaslException should be thrown when protocol is null");
        } catch (SaslException e) {
        }
    }

    /**
     * Test for <code>createSaslClient(String[] mechanisms, 
     *      String authanticationID, String protocol, String serverName,
     *      Map prop, CallbackHandler cbh))</code>
     * method Assertions: throws NullPointerException when mechanisms is null;
     * returns null SaslClient.
     * 
     * All providers are previously removed.
     */
    public void testCreateClient02() throws SaslException {
        try {
            Sasl.createSaslClient(null, null, null, null, null, null);
            fail("NullPointerException should be thrown when mechanisms is null");
        } catch (NullPointerException e) {
        }
        assertNull("Not null result", Sasl.createSaslClient(
                new String[] { "NAME-999" }, null, null, null, null, null));
    }

    /**
     * Test for <code>createSaslClient(String[] mechanisms, 
     *      String authanticationID, String protocol, String serverName,
     *      Map prop, CallbackHandler cbh))</code>
     * method
     * 
     * Assertions: returns SaslClient; throws SaslClient for NAME-1 mechanism
     * 
     * All providers are previously removed and 2 new providers were added.
     */
    public void testCreateClient03() throws SaslException {
        mProv = new Provider[] {
                (new SpiEngUtils()).new MyProvider("MySaslClientProvider1",
                        "Testing provider SaslClientFactory - 1", CLNTSRV
                                .concat("NAME-1"), fClientClass),
                (new SpiEngUtils()).new MyProvider("MySaslClientProvider2",
                        "Testing provider SaslClientFactory - 2", CLNTSRV
                                .concat("NAME-2"), fClientClass) };
        addProviders();

        CallbackHandler cbH = new cbHandN();
        SaslClient saslC = Sasl.createSaslClient(new String[] { "NAME-2" },
                null, "protocol", null, null, cbH);
        assertNotNull("Null result", saslC);
        try {
            saslC.unwrap(null, 1, 1);
            fail("SaslException sould be thrown");
        } catch (SaslException e) {
        }
        assertFalse("Incorrect isComplete() result", saslC.isComplete());
        // try to create client for wrong mechanism
        try {
            saslC = Sasl.createSaslClient(new String[] { "NAME-1" }, null,
                    "protocol", null, null, cbH);
            fail("SaslException sould be thrown");
        } catch (SaslException e) {
        }
    }

    /**
     * Test for <code>createSaslClient(String[] mechanisms, 
     *      String authanticationID, String protocol, String serverName,
     *      Map prop, CallbackHandler cbh))</code>
     * method
     * 
     * Assertions: returns SaslClient; throws SaslClient for NAME-1 mechanism
     * 
     * All providers are previously removed and 1 new provider was added.
     */
    public void testCreateClient04() throws SaslException {
        mProv = new Provider[] { (new SpiEngUtils()).new MyProvider(
                "MySaslClientProvider1",
                "Testing provider SaslClientFactory - 1", CLNTSRV
                        .concat("NAME-1"), fClientClass) };
        mProv[0].put(CLNTSRV.concat("NAME-2"), fClientClass);
        addProviders();
        CallbackHandler cbH = new cbHandN();
        SaslClient saslC = Sasl.createSaslClient(new String[] { "NAME-2" },
                null, "protocol", null, null, cbH);
        assertNotNull("Null result for NAME-2", saslC);
        assertFalse("Incorrect isComplete() result", saslC.isComplete());
        // try to create client for wrong mechanism
        try {
            saslC = Sasl.createSaslClient(new String[] { "NAME-1" }, null,
                    "protocol", null, null, cbH);
            fail("SaslException sould be thrown");
        } catch (SaslException e) {
        }
    }

    /**
     * Test for <code>createSaslClient(String[] mechanisms, 
     *      String authanticationID, String protocol, String serverName,
     *      Map prop, CallbackHandler cbh))</code>
     * method
     * 
     * Assertions: return null client when there is no provider supported some
     * mechanism returns SaslClient when incorrect mechanism is used
     * 
     * All providers are previously removed and 2 new providers were added.
     */
    public void testCreateClient05() throws SaslException {
        mProv = new Provider[] {
                (new SpiEngUtils()).new MyProvider("MySaslClientProvider1",
                        "Testing provider SaslClientFactory - 1", CLNTSRV
                                .concat("NAME-2"), fClientClass.concat("Ext")),
                (new SpiEngUtils()).new MyProvider("MySaslClientProvider2",
                        "Testing provider SaslClientFactory - 2", CLNTSRV
                                .concat("NAME-1"), fClientClass),
                (new SpiEngUtils()).new MyProvider("MySaslClientProvider3",
                        "Testing provider SaslClientFactory - 3", CLNTSRV
                                .concat("NAME-6"), fClientClass) };
        addProviders();

        CallbackHandler cbH = new cbHandN();

        SaslClient saslC;
        // try to create SaslClient for wrong mechanism
        // there is no provider supported NAME-77, NAME-66 mechanisms

        assertNull("Not null object was created for wrong mechanism", Sasl
                .createSaslClient(new String[] { "NAME-77", "NAME-66" }, null,
                        "protocol", null, null, cbH));

        saslC = Sasl.createSaslClient(new String[] { "NAME-2" }, null,
                "protocol", null, null, cbH);
        assertNotNull("Null result for NAME-2", saslC);
        try {
            saslC.unwrap(null, 1, 1);
            fail("SaslException sould be thrown");
        } catch (SaslException e) {
        }
        assertFalse("Incorrect isComplete() result", saslC.isComplete());
        // NAME-1 was defined in some provider but it is supported in
        // another provider
        try {
            Sasl.createSaslClient(new String[] { "NAME-1" }, null, "protocol",
                    null, null, cbH);
            fail("SaslException sould be thrown");
        } catch (SaslException e) {
        }
        // NAME-6 and NAME-5 were defined in one provider but they are
        // supported
        // in another provider
        saslC = Sasl.createSaslClient(new String[] { "NAME-6", "NAME-5" },
                null, "protocol", null, null, cbH);
        assertNotNull("Null result for NAME-6 and NAME-5", saslC);
    }
    
    /*
     * Additional classes for creating SaslClient and SaslServer objects
     */

    public static class mySaslClientFactory implements SaslClientFactory {
        public mySaslClientFactory() {
            super();
        }

        public String[] getMechanismNames(Map<String, ?> prop) {
            return new String[] { "NAME-1", "NAME-2", "NAME-3", "NAME-4" };
        }

        public SaslClient createSaslClient(String[] mech, String id,
                String protocol, String srvName, Map<String, ?> prop, CallbackHandler hnd)
                throws SaslException {
            if (mech == null) {
                throw new SaslException();
            }
            if ("NAME-1".equals(mech[0])) {
                throw new SaslException("Incorrect mechanisms");
            }
            if (protocol == null) {
                throw new SaslException("Protocol is null");
            }
            TextOutputCallback[] cb = { new TextOutputCallback(
                    TextOutputCallback.INFORMATION, "Information") };
            try {
                hnd.handle(cb);
            } catch (UnsupportedCallbackException e) {
                throw new SaslException("Incorrect callback handlere", e);
            } catch (IOException e) {
                throw new SaslException("Incorrect callback handlere", e);
            }
            return new mySaslClient();
        }

        public class mySaslClient implements SaslClient {
            public mySaslClient() {
                super();
            }

            public Object getNegotiatedProperty(String s) {
                return "";
            }

            public String getMechanismName() {
                return "Proba";
            }

            public boolean isComplete() {
                return false;
            }

            public boolean hasInitialResponse() {
                return false;
            }

            public void dispose() throws SaslException {
            }

            public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
                return new byte[0];
            }

            public byte[] unwrap(byte[] incoming, int offset, int len)
                    throws SaslException {
                throw new SaslException();
            }

            public byte[] wrap(byte[] outgoing, int offset, int len)
                    throws SaslException {
                return new byte[0];
            }
        }
    }

    public static class mySaslClientFactoryExt extends mySaslClientFactory {
        @Override
        public String[] getMechanismNames(Map<String, ?> prop) {
            return new String[] { "NAME-5", "NAME-6" };
        }

        @Override
        public SaslClient createSaslClient(String[] mech, String id,
                String protocol, String srvName, Map<String, ?> prop, CallbackHandler hnd)
                throws SaslException {
            if (mech == null) {
                throw new SaslException();
            }
            return new mySaslClient();
        }
    }

    public static class cbHand implements CallbackHandler {
        public cbHand() {
        }

        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (Callback element : callbacks) {
                if (element instanceof NameCallback) {
                    NameCallback nc = (NameCallback) element;
                    nc.setName("Ok");
                } else if (element instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) element;
                    System.err.print(pc.getPrompt());
                    System.err.flush();
                    pc.setPassword(new char[] { 'O', 'k' });
                } else {
                    throw new UnsupportedCallbackException(element,
                            "Callback should be NamCallback or PasswordCallback");
                }
            }
        }
    }

    public static class cbHandN implements CallbackHandler {
        public cbHandN() {
        }

        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (Callback element : callbacks) {
                if (element instanceof TextOutputCallback) {
                    TextOutputCallback toc = (TextOutputCallback) element;
                    if (toc.getMessageType() != TextOutputCallback.INFORMATION) {
                        throw new IOException("Unsupported message type: "
                                + toc.getMessageType());
                    }
                } else {
                    throw new UnsupportedCallbackException(element,
                            "Callback should be TextOutputCallback");
                }
            }
        }
    }
}

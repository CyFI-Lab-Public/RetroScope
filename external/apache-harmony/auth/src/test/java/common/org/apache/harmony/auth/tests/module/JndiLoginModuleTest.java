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

package org.apache.harmony.auth.tests.module;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.apache.harmony.auth.module.JndiLoginModule;

public class JndiLoginModuleTest extends TestCase {

    // module options
    private HashMap<String, String> options = new HashMap<String, String>();

    private final String LDAP_SERVER_LOCATION = "";

    private final String USER_PROVIDER_URL = "ldap://" + LDAP_SERVER_LOCATION
            + ":389/ou=People,o=JNDITutorial,dc=my-domain,dc=com";

    private final String GROUP_PROVIDER_URL = "ldap://" + LDAP_SERVER_LOCATION
            + ":389/ou=Groups,o=JNDITutorial,dc=my-domain,dc=com";

    protected void setUp() throws Exception {
        options.put("user.provider.url", USER_PROVIDER_URL);
        options.put("group.provider.url", GROUP_PROVIDER_URL);
    }

    @Override
    protected void tearDown() throws Exception {
        options.clear();
    }

    /**
     * Test method for
     * {@link org.apache.harmony.auth.module.JndiLoginModule#abort()}.
     */
    public void test_abort() throws LoginException {
        JndiLoginModule jlm = new JndiLoginModule();
        try {
            assertFalse("Should return false if login failed or no login", jlm
                    .abort());
        } catch (LoginException e) {
            fail("Abort failed");
        }
        Subject subject = new Subject();
        subject.setReadOnly();
        jlm.initialize(subject, null, null, options);

        assertFalse("Should return false if login failed or no login", jlm.abort());

        subject = new Subject();
        jlm.initialize(subject, new FaultCallbackHandler(), null, options);
        try {
            jlm.login();
            fail("login should fail");
        } catch (LoginException e) {
            assertFalse("Should return false because of login failure", jlm
                    .abort());
        }
        subject = new Subject();
        jlm.initialize(subject, new MockCallbackHandler(), null, options);
        jlm.login();
        assertTrue("Should return true if login was successful", jlm.abort());
    }

    /**
     * Test method for
     * {@link org.apache.harmony.auth.module.JndiLoginModule#commit()}.
     */
    public void test_commit() {
        JndiLoginModule module = new JndiLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new MockCallbackHandler(), null, options);
        try {
            assertTrue("Login should be successful", module.login());
            module.commit();
        } catch (LoginException e) {
            fail("Login shouldn't fail");
        }
        Set<Principal> principals = subject.getPrincipals();
        assertFalse("Should get at least one principal", principals.isEmpty());
        subject = new Subject();
        subject.setReadOnly();
        module.initialize(subject, new MockCallbackHandler(), null, options);
        try {
            assertFalse("Commit shouldn't be successful", module.commit());
            fail("Should throw LoginException here because of trying to clear read-only subject");
        } catch (LoginException e) {
            // expected LoginException here
        }
    }

    /**
     * Test method for
     * {@link org.apache.harmony.auth.module.JndiLoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)}.
     */
    public void test_initialize() {
        JndiLoginModule module = new JndiLoginModule();
        try {
            module.initialize(null, null, null, null);
            fail("Should throw NullPointerException here.");
        } catch (NullPointerException e) {
            // expected NullPointerException
        }
    }

    /**
     * Test method for
     * {@link org.apache.harmony.auth.module.JndiLoginModule#login()}.
     */
    public void test_login() {
        JndiLoginModule module = new JndiLoginModule();
        HashMap<String, String> emptyOptions = new HashMap<String, String>();
        module.initialize(null, new MockCallbackHandler(), null, emptyOptions);
        try {
            module.login();
            fail("Should throw LoginException here.");
        } catch (LoginException e) {
            // expected LoginException
        }

        Subject subject = new Subject();
        module.initialize(subject, new MockCallbackHandler(), null, options);
        try {
            assertTrue("Login should be successful", module.login());
        } catch (LoginException e) {
            fail("Login shouldn't fail");
        }
        module.initialize(subject, new FaultCallbackHandler(), null, options);
        try {
            assertFalse("Login shouldn't be successful", module.login());
            fail("Login should fail");
        } catch (LoginException e) {
            // expected Loginexception here
        }
    }

    /**
     * Test method for
     * {@link org.apache.harmony.auth.module.JndiLoginModule#logout()}.
     */
    public void test_logout() {
        JndiLoginModule module = new JndiLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new MockCallbackHandler(), null, options);
        try {
            assertTrue("Login should be successful", module.login());
            module.commit();
        } catch (LoginException e) {
            fail("Login shouldn't fail");
        }
        Set<Principal> principals = subject.getPrincipals();
        assertFalse("Should get at least one principal", principals.isEmpty());
        try {
            assertTrue("Should be true", module.logout());
        } catch (LoginException e) {
            fail("Logout failed");
        }
        principals = subject.getPrincipals();
        assertTrue("Principals should be cleared", principals.isEmpty());
    }

    public void test_optionsAndSharedStatus() throws LoginException {
        options.put("debug", "true");
        options.put("useFirstPass", "true");
        HashMap<String, Object> status = new HashMap<String, Object>();
        status.put("javax.security.auth.login.name", "leo");
        status.put("javax.security.auth.login.password", "faultPass"
                .toCharArray());
        JndiLoginModule module = new JndiLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new MockCallbackHandler(), status, options);
        try {
            module.login();
            fail("Should be failed for using password from shared state");
        } catch (LoginException e) {
            // expected LoginException here
        }

        options.remove("useFirstPass");
        options.put("tryFirstPass", "true");
        module.initialize(subject, new MockCallbackHandler(), status, options);
        try {
            module.login();
            module.commit();
        } catch (LoginException e) {
            fail("Login should be failed");
        } finally {
            module.logout();
        }

        options.remove("tryFirstPass");
        options.put("clearPass", "true");
        status.put("javax.security.auth.login.name", "leo");
        status.put("javax.security.auth.login.password", "passw0rd"
                .toCharArray());
        module.initialize(subject, new MockCallbackHandler(), status, options);
        try {
            module.login();
            module.commit();
            assertNull(
                    "javax.security.auth.login.name in shared state should be null when clearPass switch on",
                    status.get("javax.security.auth.login.name"));
            assertNull(
                    "javax.security.auth.login.password in shared state should be null when clearPass switch on",
                    status.get("javax.security.auth.login.password"));
        } catch (LoginException e) {
            fail("Login shouldn't fail");
        } finally {
            module.logout();
        }

        status = new HashMap<String, Object>();
        options.remove("clearPass");
        options.put("storePass", "true");
        module.initialize(subject, new FaultCallbackHandler(), status, options);
        try {
            module.login();
            module.commit();
        } catch (LoginException e) {
            assertNull(
                    "javax.security.auth.login.name in shared state should be null when login failed",
                    status.get("javax.security.auth.login.name"));
            assertNull(
                    "javax.security.auth.login.password in shared state should be null when login failed",
                    status.get("javax.security.auth.login.password"));
        } finally {
            module.logout();
        }

        module.initialize(subject, new MockCallbackHandler(), status, options);
        try {
            module.login();
            module.commit();
        } catch (LoginException e) {
            fail("Login failed");
        } finally {
            module.logout();
        }
        assertNotNull(
                "javax.security.auth.login.name should be stored in shared state when storePass switch on",
                status.get("javax.security.auth.login.name"));
        assertNotNull(
                "javax.security.auth.login.password should be stored in shared state when storePass switch on",
                status.get("javax.security.auth.login.password"));

        status.put("javax.security.auth.login.name", "tester");
        status.put("javax.security.auth.login.password", "testerPass");
        module.initialize(subject, new MockCallbackHandler(), status, options);
        try {
            module.login();
            module.commit();
        } catch (LoginException e) {
            fail("Login failed");
        } finally {
            module.logout();
        }
        assertEquals("Should't override the username value in sharedState",
                status.get("javax.security.auth.login.name"), "tester");
        assertEquals("Should't override the password value in sharedState",
                status.get("javax.security.auth.login.password"), "testerPass");
    }

    static private class MockCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName("leo");
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword("passw0rd".toCharArray());
                } else {
                    throw new Error(callbacks[i].getClass().toString());
                }
            }
        }
    }

    static private class FaultCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName("leo");
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword("password".toCharArray());
                } else {
                    throw new Error(callbacks[i].getClass().toString());
                }
            }
        }
    }
}

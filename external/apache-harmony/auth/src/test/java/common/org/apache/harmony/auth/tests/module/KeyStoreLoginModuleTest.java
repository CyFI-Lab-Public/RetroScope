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

import java.security.Principal;
import java.util.HashMap;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.apache.harmony.auth.module.KeyStoreLoginModule;

import tests.support.resource.Support_Resources;

public class KeyStoreLoginModuleTest extends TestCase {

    // module options
    private HashMap<String, String> options = new HashMap<String, String>();

    private final String KEYSTORE_URL = "file:"
            + Support_Resources.getAbsoluteResourcePath("hyts_ks.bks");

    private final String KEYSTORE_PASSWORD_URL = "file:"
            + Support_Resources.getAbsoluteResourcePath("hyts_ks_pass");

    private final String KEYSTORE_FAULTPASSWORD_URL = "file:"
            + Support_Resources.getAbsoluteResourcePath("fault_pass");

    private final String KEYSTORE_ALIAS = "mykey";

    public void test_abort() throws LoginException {
        KeyStoreLoginModule ksm = new KeyStoreLoginModule();
        try {
            assertFalse("Should return false if login failed or no login", ksm
                    .abort());
        } catch (LoginException e) {
            fail("Abort failed");
        }
        Subject subject = new Subject();
        subject.setReadOnly();
        ksm.initialize(subject, null, null, options);

        assertFalse("Should return false if login failed or no login", ksm.abort());

        options.remove("keyStorePasswordURL");
        options.put("keyStorePasswordURL", KEYSTORE_FAULTPASSWORD_URL);
        subject = new Subject();
        ksm.initialize(subject, null, null, options);
        try {
            ksm.login();
            fail("login should fail");
        } catch (LoginException e) {
            assertFalse("Should return false because of login failure", ksm
                    .abort());
        }
        options.remove("keyStorePasswordURL");
        options.put("keyStorePasswordURL", KEYSTORE_PASSWORD_URL);
        subject = new Subject();
        ksm.initialize(subject, null, null, options);
        ksm.login();
        assertTrue("Should return true if login was successful", ksm
                .abort());
    }

    public void test_commit() {
        KeyStoreLoginModule module = new KeyStoreLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, null, null, options);
        try {
            assertTrue("Login should be successful", module.login());
            module.commit();
        } catch (LoginException e) {
            e.printStackTrace();
            fail("Login shouldn't fail");
        }
        Set<Principal> principals = subject.getPrincipals();
        assertFalse("Should get at least one principal", principals.isEmpty());
        Set<Object> subjects = subject.getPrivateCredentials();
        assertFalse("Should get at least one private credential", subjects
                .isEmpty());
        Set<Object> subjects2 = subject.getPublicCredentials();
        assertFalse("Should get at least one public credential", subjects2
                .isEmpty());
        subject = new Subject();
        subject.setReadOnly();
        module.initialize(subject, null, null, options);
        try {
            assertFalse("Commit shouldn't be successful", module.commit());
            fail("Should throw LoginException here because of trying to clear read-only subject");
        } catch (LoginException e) {
            // expected LoginException here
        }

    }

    public void test_initialize() {
        KeyStoreLoginModule module = new KeyStoreLoginModule();
        try {
            module.initialize(null, null, null, null);
            fail("Should throw NullPointerException here.");
        } catch (NullPointerException e) {
            // expected NullPointerException
        }
    }

    public void test_login() {
        KeyStoreLoginModule module = new KeyStoreLoginModule();
        HashMap<String, String> emptyOptions = new HashMap<String, String>();
        module.initialize(null, null, null, emptyOptions);
        try {
            module.login();
            fail("Should throw LoginException here.");
        } catch (LoginException e) {
            // expected LoginException
        }

        Subject subject = new Subject();
        module.initialize(subject, null, null, options);
        try {
            assertTrue("Login should be successful", module.login());
        } catch (LoginException e) {
            fail("Login shouldn't fail");
        }
        options.put("keyStorePasswordURL", KEYSTORE_FAULTPASSWORD_URL);
        module.initialize(subject, null, null, options);
        try {
            assertFalse("Login shouldn't be successful", module.login());
            fail("Login should fail");
        } catch (LoginException e) {
            // expected Loginexception here
        }
    }

    public void test_logout() {
        KeyStoreLoginModule module = new KeyStoreLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, null, null, options);
        try {
            assertTrue("Login should be successful", module.login());
            module.commit();
        } catch (LoginException e) {
            fail("Login shouldn't fail");
        }
        Set<Principal> principals = subject.getPrincipals();
        assertFalse("Should get at least one principal", principals.isEmpty());
        Set<Object> subjects = subject.getPrivateCredentials();
        assertFalse("Should get at least one private credential", subjects
                .isEmpty());
        Set<Object> subjects2 = subject.getPublicCredentials();
        assertFalse("Should get at least one public credential", subjects2
                .isEmpty());
        try {
            assertTrue("Should be true", module.logout());
        } catch (LoginException e) {
            fail("Logout failed");
        }
        principals = subject.getPrincipals();
        assertTrue("Principals should be cleared", principals.isEmpty());
        subjects = subject.getPrivateCredentials();
        assertTrue("Private credential should be cleared", subjects.isEmpty());
        subjects2 = subject.getPublicCredentials();
        assertTrue("Public credential should be cleared", subjects2.isEmpty());
    }

    protected void setUp() throws Exception {
        options.put("keyStoreURL", KEYSTORE_URL);
        options.put("keyStorePasswordURL", KEYSTORE_PASSWORD_URL);
        options.put("keyStoreAlias", KEYSTORE_ALIAS);
    }

    @Override
    protected void tearDown() throws Exception {
        options.clear();
    }
}

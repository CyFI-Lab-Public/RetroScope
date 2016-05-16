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
 * @author Alexander V. Astapchuk
 */
package org.apache.harmony.auth.module;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;
import org.apache.harmony.auth.module.UnixLoginModule;


/**
 * Unit test for UnixLoginModule
 */
@SuppressWarnings("unchecked")
public class UnixLoginModuleTest extends TestCase {

    UnixLoginModule lm = new UnixLoginModule();

    @Override
    protected void setUp() throws Exception {
        Subject subj = new Subject();
        CallbackHandler cbh = new TestCallbackHandler();
        Map sharedState = new HashMap();
        Map options = new HashMap();
        lm.initialize(subj, cbh, sharedState, options);
    }

    private static class TestCallbackHandler implements CallbackHandler {
        public void handle(Callback[] cbs) {
            // does nothing
        }
    }

    /**
     * Test for UnixLoginModule.initialize()
     */
    public void testInitialize() {
        // Need new, non initialized instance of LoginModule
        lm = new UnixLoginModule();
        
        Map shared = new HashMap();
        Map options = new HashMap();
        CallbackHandler cbh = new TestCallbackHandler();
        // must not accept null for subject
        try {
            lm.initialize(null, cbh, shared, options);
            fail("must not pass here");
        } catch (NullPointerException _) {
            // gut
        }
        Subject subj = new Subject();
        // must accept null for handler
        lm.initialize(subj, null, shared, options);
        // must accept null for sharedState
        lm.initialize(subj, cbh, null, options);
        // must not accept null for options
        try {
            lm.initialize(subj, cbh, shared, null);
            fail("must not pass here");
        } catch (NullPointerException _) {
            // gut
        }
    }

    public void testAbort() throws  LoginException {
        lm.login();
        lm.abort();
    }

    public void testCommit() throws  LoginException {
        lm.login();
        lm.commit();
        lm.logout();
    }

    public void testLogin() throws  LoginException {
        lm.login();
        lm.abort();
    }

    public void testLogout() throws LoginException {
        lm.logout();
    }

}

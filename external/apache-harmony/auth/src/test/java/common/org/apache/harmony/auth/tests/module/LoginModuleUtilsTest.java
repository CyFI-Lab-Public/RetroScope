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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import javax.security.auth.login.LoginException;

import org.apache.harmony.auth.module.LoginModuleUtils;

import junit.framework.TestCase;

public class LoginModuleUtilsTest extends TestCase {

    public void testGetPassword() throws Exception {
        final String PASSWORD_AS_STRING = "TESTPASSWORD";
        final char[] PASSWORD_AS_CHARS = PASSWORD_AS_STRING.toCharArray();

        String password_file_content = PASSWORD_AS_STRING;
        InputStream in = new ByteArrayInputStream(password_file_content
                .getBytes());
        char[] password = LoginModuleUtils.getPassword(in);
        assertTrue(Arrays.equals(PASSWORD_AS_CHARS, password));

        password_file_content = "TESTPASSWORD" + "\nNONsense";
        in = new ByteArrayInputStream(password_file_content.getBytes());
        password = LoginModuleUtils.getPassword(in);
        assertTrue(Arrays.equals(PASSWORD_AS_CHARS, password));

        password_file_content = "TESTPASSWORD" + "\r\nNONsense";
        in = new ByteArrayInputStream(password_file_content.getBytes());
        password = LoginModuleUtils.getPassword(in);
        assertTrue(Arrays.equals(PASSWORD_AS_CHARS, password));

        password_file_content = "TESTPASSWORD" + "\n\rNONsense";
        in = new ByteArrayInputStream(password_file_content.getBytes());
        password = LoginModuleUtils.getPassword(in);
        assertTrue(Arrays.equals(PASSWORD_AS_CHARS, password));

        password_file_content = "TESTPASSWORD" + "\r\r\nNONsense";
        in = new ByteArrayInputStream(password_file_content.getBytes());
        password = LoginModuleUtils.getPassword(in);
        String expectedString = PASSWORD_AS_STRING + "\r";
        assertTrue(Arrays.equals(expectedString.toCharArray(), password));
    }
    
    public void testClearPassword() throws Exception {
        final String PASSWORD_AS_STRING = "TESTPASSWORD";

        char[] password = PASSWORD_AS_STRING.toCharArray();
        LoginModuleUtils.clearPassword(password);
        for (char c : password) {
            assertEquals('\0', c);
        }

        password = null;
        LoginModuleUtils.clearPassword(password);
        password = new char[0];
        LoginModuleUtils.clearPassword(password);
    }
    
    public void testLoginModuleStatus() throws Exception {
        LoginModuleUtils.LoginModuleStatus status = new LoginModuleUtils.LoginModuleStatus();
        try {
            status.checkLogin();
        } catch (LoginException e) {
            // expected
        }

        try {
            status.checkCommit();
        } catch (LoginException e) {
            // expected
        }

        try {
            status.checkLogout();
        } catch (LoginException e) {
            // expected
        }

        status.initialized();
        assertEquals(LoginModuleUtils.ACTION.login, status
                .checkLogin());

        try {
            status.checkCommit();
        } catch (LoginException e) {
            // expected
        }

        assertEquals(LoginModuleUtils.ACTION.no_action,
                status.checkLogout());

        status.logined();
        assertEquals(LoginModuleUtils.ACTION.no_action,
                status.checkLogin());
        assertEquals(LoginModuleUtils.ACTION.commit, status
                .checkCommit());
        assertEquals(LoginModuleUtils.ACTION.no_action,
                status.checkLogout());

        status.committed();
        assertEquals(LoginModuleUtils.ACTION.no_action,
                status.checkLogin());
        assertEquals(LoginModuleUtils.ACTION.no_action,
                status.checkCommit());
        assertEquals(LoginModuleUtils.ACTION.logout, status
                .checkLogout());

        status.logined();
        assertEquals(LoginModuleUtils.ACTION.no_action,
                status.checkLogin());
        assertEquals(LoginModuleUtils.ACTION.commit, status
                .checkCommit());
        assertEquals(LoginModuleUtils.ACTION.no_action,
                status.checkLogout());
    }

}

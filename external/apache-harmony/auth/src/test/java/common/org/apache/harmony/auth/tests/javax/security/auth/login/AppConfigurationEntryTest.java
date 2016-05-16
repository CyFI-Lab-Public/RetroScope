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
 * @author Maxim V. Makarov
 */

package org.apache.harmony.auth.tests.javax.security.auth.login;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;

import junit.framework.TestCase;

/**
 * Tests AppConfigurationEntry class
 */
public class AppConfigurationEntryTest extends TestCase {

    String loginModule = "LoginModule";

    private static final String msg = "LoginModuleControlFlag: ";

    AppConfigurationEntry entry = null;

    Map<String, Object> options = new HashMap<String, Object>();

    @Override
    public void setUp() {
        options.put("debug", "true");
        options.put("testing", "false");
    }

    /**
     * Test for ctor
     *
     * 
     */
    public void testCtor_01() {
        entry = new AppConfigurationEntry(loginModule,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
        assertEquals("LoginModule", entry.getLoginModuleName());
        assertEquals(msg + "required", entry.getControlFlag().toString());
        entry = new AppConfigurationEntry(loginModule,
                AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, options);
        assertEquals(msg + "requisite", entry.getControlFlag().toString());
        entry = new AppConfigurationEntry(loginModule,
                AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL, options);
        assertEquals(msg + "optional", entry.getControlFlag().toString());
        entry = new AppConfigurationEntry(loginModule,
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                options);
        assertEquals(msg + "sufficient", entry.getControlFlag().toString());
    }

    /**
     * Test for ctor with incorrect arguments. If name of the login module is null
     * or an empty, if the control flag or options is null then must be throw IAE
     */
    public void testCtor_02() {

        try {
            entry = new AppConfigurationEntry(null,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            entry = new AppConfigurationEntry("",
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e1) {
        }
        /*        try {
         entry = new AppConfigurationEntry(" ", 
         AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
         fail("Expected IllegalArgumentException");
         } catch (IllegalArgumentException e2) {
         }
         */try {
            entry = new AppConfigurationEntry(loginModule, null, options);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e3) {
        }
        try {
            entry = new AppConfigurationEntry(loginModule,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e4) {
        }
    }

    /**
     * test for array of the AppConfigurationEntry and for the method getOptions 
     */
    public void testArrayCtor() {
        Map<String, String> options1 = new HashMap<String, String>();
        options1.put("debug", "false");
        AppConfigurationEntry[] entries = {
                new AppConfigurationEntry("LoginModule1",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options),
                new AppConfigurationEntry("LoginModule2",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options1),
                new AppConfigurationEntry("LoginModule3",
                        AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL,
                        new HashMap<String, String>()) };

        assertEquals("LoginModule1", entries[0].getLoginModuleName());
        assertEquals(options, entries[0].getOptions());
        assertEquals("LoginModule2", entries[1].getLoginModuleName());
        assertEquals(options1, entries[1].getOptions());
        assertEquals("LoginModule3", entries[2].getLoginModuleName());
        assertTrue(entries[2].getOptions().isEmpty());

    }

    /**
     * test for the AppConfigurationEntry.LoginModuleControlFlag 
     * 
     */
    public void testControlFlags() {
        assertEquals(AppConfigurationEntry.LoginModuleControlFlag.class,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED
                        .getClass());
        assertEquals(AppConfigurationEntry.LoginModuleControlFlag.class,
                AppConfigurationEntry.LoginModuleControlFlag.REQUISITE
                        .getClass());
        assertEquals(AppConfigurationEntry.LoginModuleControlFlag.class,
                AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL
                        .getClass());
        assertEquals(AppConfigurationEntry.LoginModuleControlFlag.class,
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT
                        .getClass());

        assertEquals(msg + "required",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED
                        .toString());
        assertEquals(msg + "requisite",
                AppConfigurationEntry.LoginModuleControlFlag.REQUISITE
                        .toString());
        assertEquals(msg + "optional",
                AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL
                        .toString());
        assertEquals(msg + "sufficient",
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT
                        .toString());
    }

    @SuppressWarnings("unchecked")
    public void testImmutability() {
        AppConfigurationEntry entry = new AppConfigurationEntry("LoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);

        Map<String, Object> eOptions = (Map<String, Object>)entry.getOptions();

        try {
            eOptions.put("a", "b");
            fail("No expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            eOptions.clear();
            fail("No expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals("Reference", eOptions, entry.getOptions());

        assertEquals("Content before", options, eOptions);
        options.put("key", "value");
        assertEquals("Content after", options, entry.getOptions());
    }
}

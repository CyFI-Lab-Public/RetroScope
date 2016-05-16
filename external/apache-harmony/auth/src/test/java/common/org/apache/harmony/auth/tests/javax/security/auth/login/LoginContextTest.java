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
* @author Stepan M. Mishura
*/

package org.apache.harmony.auth.tests.javax.security.auth.login;

import java.io.File;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.spi.LoginModule;

import junit.framework.TestCase;

import org.apache.harmony.auth.tests.support.TestUtils;

import tests.support.Support_Exec;

/**
 * Tests LoginContext class
 */
public class LoginContextTest extends TestCase {

    // system property to specify another login configuration file 
    private static final String AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

    private static final String moduleName = "moduleName";

    private final Subject subject = new Subject();

    private final MyCallbackHandler handler = new MyCallbackHandler();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Configuration.setConfiguration(new MyConfig());
    }

    /**
     * Constructor: LoginContext(java.lang.String)
     * 
     * Parameters: valid app. name
     * 
     * Expected: no exceptions
     */
    public final void testLC_String() throws Exception {

        // configuration contains requested login module
        LoginContext context = new LoginContext(moduleName);

        assertTrue("Requested module", MyConfig.getLastAppName() == moduleName);

        //FIXME lazy subject initialization? should it be initialized in ctor?
        assertNull("Instantiated subject", context.getSubject());
    }

    /**
     * Constructor: LoginContext(java.lang.String)
     * 
     * Parameters: invalid (null app. name)
     * 
     * Expected: LoginException
     */
    public final void testLC_String_NullApp() {

        try {
            new LoginContext(null);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(java.lang.String)
     * 
     * Precondition: app. name absent in the current configuration
     * 
     * Expected: LoginException
     */
    public final void testLC_String_NoApp() {

        // configuration doesn't contain requested application name
        MyConfig.resetConfiguration();

        try {
            new LoginContext(moduleName);
            fail("No expected LoginException");
        } catch (LoginException e) {
            assertEquals("Default module", "other", MyConfig.getLastAppName());
        }
    }

    /**
     * Constructor: LoginContext(java.lang.String)
     * 
     * Precondition: configuration contains requested login module and
     *             default callback handler is specified via security
     *             property but its class is not accessible
     * 
     * Expected: LoginException
     */
    public final void testLC_String_InaccessibleCallbackHandler() {

        try {
            Security.setProperty("auth.login.defaultCallbackHandler",
                    "absentCallBackhandlerClassName");

            new LoginContext(moduleName);
            fail("No expected LoginException");
        } catch (LoginException e) {
            assertTrue("Default module",
                    MyConfig.getLastAppName() == moduleName);
        } finally {
            //FIXME how to reset security property correctly?
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(java.lang.String)
     * 
     * Precondition: configuration contains requested login module and
     *             default callback handler is specified via security
     *             property but its class doesn't implement
     *             CallbackHandler interface
     * 
     * Expected: LoginException
     */
    public final void testLC_String_InvalidCallbackHandler() throws Exception {

        try {
            Security.setProperty("auth.login.defaultCallbackHandler",
                    LoginContextTest.class.getName());

            new LoginContext(moduleName);
            fail("No expected ClassCastException");
        } catch (ClassCastException e) {
        } finally {
            //FIXME how to reset security property correctly?
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(java.lang.String)
     * 
     * Expected: creation of default callback handler
     */
    public final void testLC_String_InitCallbackHandler() throws Exception {

        // checks initialization of specified callback handler
        MyCallbackHandler.initialized = false;
        try {
            Security
                    .setProperty("auth.login.defaultCallbackHandler",
                            MyCallbackHandler.class.getName());

            new LoginContext(moduleName);

            assertTrue("Initialization", MyCallbackHandler.initialized);
        } finally {
            //FIXME how to reset security property correctly?
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(java.lang.String)
     * 
     * Precondition: parameters for login module initialization
     *               are created in the constructor above
     * 
     * Expected: not null subject, null callback handler or
     *           wrapped default callback handler, not null shared
     *           state and not null options.
     */
    public final void testLC_String_LoginModuleInitialize() throws Exception {

        Hashtable<String, Object> options = new Hashtable<String, Object>();

        // add required module to the current configuration
        MyConfig.addRequired("MyLoginModule", options);

        // reset initialized login modules list 
        MyLoginModule.reset();

        LoginContext context = new LoginContext(moduleName);

        context.login();

        // only one module must be created
        assertEquals("Number of modules", 1, MyLoginModule.list.size());

        MyLoginModule module = MyLoginModule.list.get(0);

        // login context instantiates subject object itself. 
        assertNotNull("Subject", module.subject);
        assertTrue("getSubject", module.subject == context.getSubject());

        // login context doesn't have callback handler
        assertNull("Handler", module.handler);

        // login context provides login module with shared state object
        assertNotNull("Shared state", module.sharedState);

        // login context provides login module with module's options
        assertTrue("Option references", module.options != options);
        assertEquals("Option objects", module.options, options);

        // checks initialization of specified callback handler
        MyLoginModule.reset();
        try {
            Security
                    .setProperty("auth.login.defaultCallbackHandler",
                            MyCallbackHandler.class.getName());

            context = new LoginContext(moduleName);

            context.login();

            // TODO how to test default callback handler wrapping for LoginContext(java.lang.String)?

            // FIXME wrap a handler
            //assertFalse("Handler", MyLoginModule.handler.getClass().equals(
            //        MyCallbackHandler.class));
        } finally {
            // FIXME how to reset security property correctly?
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(String, CallbackHandler)
     * 
     * Parameters: are valid
     * 
     * Expected: no exceptions
     */
    public final void testLC_StringCallbackHandler() throws Exception {

        LoginContext context = new LoginContext(moduleName, handler);

        assertTrue("Requested module", MyConfig.getLastAppName() == moduleName);

        //FIXME lazy subject initialization? should it be initialized in ctor?
        assertNull("Instantiated subject", context.getSubject());
    }

    /**
     * Constructor: LoginContext(String, CallbackHandler)
     * 
     * Parameters: invalid (null app. name)
     * 
     * Expected: LoginException
     */
    public final void testLC_StringCallbackHandler_NullApp() {

        try {
            new LoginContext(null, handler);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(String, CallbackHandler)
     * 
     * Parameters: invalid (null callback handler)
     * 
     * Expected: LoginException
     */
    public final void testLC_StringCallbackHandler_NullCallbackHandler() {

        try {
            new LoginContext(moduleName, (CallbackHandler) null);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(String, CallbackHandler)
     * 
     * Precondition: app. name absent in the current configuration
     * 
     * Expected: LoginException
     */
    public final void testLC_StringCallbackHandler_NoApp() {

        // configuration doesn't contain requested application name
        MyConfig.resetConfiguration();

        try {
            new LoginContext(moduleName, handler);
            fail("No expected LoginException");
        } catch (LoginException e) {
            assertEquals("Default module", "other", MyConfig.getLastAppName());
        }
    }

    /**
     * Constructor: LoginContext(String, CallbackHandler)
     * 
     * Precondition: configuration contains requested login module and
     *             default callback handler is specified via security property
     *
     * Expected: no default callback handler initialization
     */
    public final void testLC_StringCallbackHandler_NoInit() throws Exception {

        // checks initialization of specified callback handler
        MyCallbackHandler.initialized = false;
        try {
            Security
                    .setProperty("auth.login.defaultCallbackHandler",
                            MyCallbackHandler.class.getName());

            new LoginContext(moduleName, handler);

            assertFalse("Initialization", MyCallbackHandler.initialized);
        } finally {
            //FIXME how to reset security property correctly?
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(String, CallbackHandler)
     * 
     * Precondition: parameters for login module initialization
     *               are created in the constructor above
     * 
     * Expected: not null subject, wrapped provided callback handler,
     *           not null shared state and not null options.
     */
    public final void testLC_StringCallbackHandler_LoginModuleInitialize()
            throws Exception {

        Hashtable<String, Object> options = new Hashtable<String, Object>();

        // add required module to the current configuration
        MyConfig.addRequired("MyLoginModule", options);

        // reset initialized login modules list 
        MyLoginModule.reset();

        LoginContext context = new LoginContext(moduleName, handler);

        context.login();

        // only one module must be created
        assertEquals("Number of modules", 1, MyLoginModule.list.size());

        MyLoginModule module = MyLoginModule.list.get(0);

        // login context instantiates subject object itself. 
        assertNotNull("Subject", module.subject);
        assertTrue("getSubject", module.subject == context.getSubject());

        //  TODO how to test callback handler wrapping for LoginContext(String, CallbackHandler)?

        // FIXME wrap a handler
        //assertFalse("Handler", MyLoginModule.handler.getClass().equals(
        //        handler.getClass()));

        // login context provides login module with shared state object
        assertNotNull("Shared state", module.sharedState);

        // login context provides login module with module's options
        assertTrue("Option references", module.options != options);
        assertEquals("Option objects", module.options, options);
    }

    /**
     * Constructor: LoginContext(String, Subject)
     * 
     * Parameters: are valid
     * 
     * Expected: no exceptions
     */
    public final void testLC_StringSubject() throws Exception {

        LoginContext context = new LoginContext(moduleName, subject);

        assertTrue("Requested module", MyConfig.getLastAppName() == moduleName);
        assertTrue("Instantiated subject", context.getSubject() == subject);
    }

    /**
     * Constructor: LoginContext(String, Subject)
     * 
     * Parameters: invalid (null app. name)
     * 
     * Expected: LoginException
     */
    public final void testLC_StringSubject_NullApp() {

        try {
            new LoginContext(null, subject);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(String, Subject)
     * 
     * Parameters: invalid (null subject)
     * 
     * Expected: LoginException
     */
    public final void testLC_StringSubject_NullSubject() {

        try {
            new LoginContext(moduleName, (Subject) null);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(String, Subject)
     * 
     * Precondition: app. name absent in the current configuration
     * 
     * Expected: LoginException
     */
    public final void testLC_StringSubject_NoApp() throws Exception {

        MyConfig.resetConfiguration();
        try {
            new LoginContext(moduleName, subject);
            fail("No expected LoginException");
        } catch (LoginException e) {
            assertEquals("Default module", "other", MyConfig.getLastAppName());
        }
    }

    /**
     * Constructor: LoginContext(String, Subject)
     * 
     * Precondition: configuration contains requested login module and
     *             default callback handler is specified via security
     *             property but its class is not accessible
     * 
     * Expected: LoginException
     */
    public final void testLC_StringSubject_InaccessibleCallbackHandler() {

        try {
            Security.setProperty("auth.login.defaultCallbackHandler",
                    "absentCallBackhandlerClassName");

            new LoginContext(moduleName, subject);
            fail("No expected LoginException");
        } catch (LoginException e) {
            assertTrue("Default module",
                    MyConfig.getLastAppName() == moduleName);
        } finally {
            // FIXME how to reset security property correctly?
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(String, Subject)
     * 
     * Expected: creation of default callback handler
     */
    public final void testLC_StringSubject_InitCallbackHandler()
            throws Exception {

        // checks initialization of specified callback handler
        MyCallbackHandler.initialized = false;
        try {
            Security
                    .setProperty("auth.login.defaultCallbackHandler",
                            MyCallbackHandler.class.getName());

            new LoginContext(moduleName, subject);

            assertTrue("Initialization", MyCallbackHandler.initialized);
        } finally {
            // FIXME how to reset security property correctly?
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(String, Subject)
     * 
     * Precondition: parameters for login module initialization
     *               are created in the constructor above
     * 
     * Expected: provided subject, null callback handler or
     *           wrapped default callback handler, not null shared
     *           state and not null options.
     */
    public final void testLC_StringSubject_LoginModuleInitialize()
            throws Exception {

        Hashtable<String, Object> options = new Hashtable<String, Object>();

        // add required module to the current configuration
        MyConfig.addRequired("MyLoginModule", options);

        // reset initialized login modules list 
        MyLoginModule.reset();

        LoginContext context = new LoginContext(moduleName, subject);

        context.login();

        // only one module must be created
        assertEquals("Number of modules", 1, MyLoginModule.list.size());

        MyLoginModule module = MyLoginModule.list.get(0);

        // login context has provided subject 
        assertTrue("Subject", module.subject == subject);
        assertTrue("getSubject", module.subject == context.getSubject());

        // login context doesn't have callback handler
        assertNull("Handler", module.handler);

        // login context provides login module with shared state object
        assertNotNull("Shared state", module.sharedState);

        // login context provides login module with module's options
        assertTrue("Option references", module.options != options);
        assertEquals("Option objects", module.options, options);

        // checks initialization of specified callback handler
        MyLoginModule.reset();
        try {
            Security
                    .setProperty("auth.login.defaultCallbackHandler",
                            MyCallbackHandler.class.getName());

            context = new LoginContext(moduleName, subject);

            context.login();

            // TODO how to test callback handler wrapping for LoginContext(String, Subject)?

            // FIXME wrap a handler
            //assertFalse("Handler", MyLoginModule.handler.getClass().equals(
            //        MyCallbackHandler.class));
        } finally {
            Security.setProperty("auth.login.defaultCallbackHandler", "");
        }
    }

    /**
     * Constructor: LoginContext(String, Subject, CallbackHandler)
     * 
     * Parameters: are valid
     * 
     * Expected: no exceptions
     */
    public final void testLC_StringSubjectCallbackHandler() throws Exception {

        LoginContext context = new LoginContext(moduleName, subject, handler);

        assertTrue("Requested module", MyConfig.getLastAppName() == moduleName);
        assertTrue("Instantiated subject", context.getSubject() == subject);
    }

    /**
     * Constructor: LoginContext(String, Subject, CallbackHandler)
     * 
     * Parameters: invalid (null app. name)
     * 
     * Expected: LoginException
     */

    public final void testLC_StringSubjectCallbackHandler_NullApp() {
        try {
            new LoginContext(null, subject, handler);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(String, Subject, CallbackHandler)
     * 
     * Parameters: invalid (null subject)
     * 
     * Expected: LoginException
     */
    public final void testLC_StringSubjectCallbackHandler_NullSubject() {
        try {
            new LoginContext(moduleName, null, handler);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(String, Subject, CallbackHandler)
     * 
     * Parameters: invalid (null callback handler)
     * 
     * Expected: LoginException
     */
    public final void testLC_StringSubjectCallbackHandler_NullCallbackHandler() {
        try {
            new LoginContext(moduleName, subject, null);
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /**
     * Constructor: LoginContext(String, Subject, CallbackHandler)
     * 
     * Precondition: app. name absent in the current configuration
     * 
     * Expected: LoginException
     */
    public final void testLC_StringSubjectCallbackHandler_NoApp() {

        // configuration doesn't contain requested login module
        MyConfig.resetConfiguration();

        try {
            new LoginContext(moduleName, subject, handler);
            fail("No expected LoginException");
        } catch (LoginException e) {
            assertEquals("Default module", "other", MyConfig.getLastAppName());
        }
    }

    /**
     * Constructor: LoginContext(String, Subject, CallbackHandler)
     * 
     * Precondition: parameters for login module initialization
     *               are created in the constructor above
     * 
     * Expected: provided subject, wrapped default callback handler,
     *           not null shared state and not null options.
     */

    public final void testLC_StringSubjectCallbackHandler_LoginModuleInitialize()
            throws Exception {

        Hashtable<String, Object> options = new Hashtable<String, Object>();

        // add required module to the current configuration
        MyConfig.addRequired("MyLoginModule", options);

        // reset initialized login modules list
        MyLoginModule.reset();

        LoginContext context = new LoginContext(moduleName, subject, handler);

        context.login();

        // only one module must be created
        assertEquals("Number of modules", 1, MyLoginModule.list.size());

        MyLoginModule module = MyLoginModule.list.get(0);

        // login context has provided subject 
        assertTrue("Subject", module.subject == subject);
        assertTrue("getSubject", module.subject == context.getSubject());

        //  TODO how to test callback handler wrapping for LoginContext(String, Subject, CallbackHandler)?

        // FIXME wrap a handler
        //assertFalse("Handler", MyLoginModule.handler.getClass().equals(
        //        handler.getClass()));

        // login context provides login module with shared state object
        assertNotNull("Shared state", module.sharedState);

        // login context provides login module with module's options
        assertTrue("Option references", module.options != options);
        assertEquals("Option objects", module.options, options);
    }

    /**
     * Method: LoginContext.login()
     * 
     * Precondition: returned list of login modules is empty  
     * 
     * Expected: LoginException
     */
    public final void testLogin_EmptyModulesList() throws Exception {

        LoginContext context = new LoginContext(moduleName);

        try {
            context.login();
            fail("No expected LoginException");
        } catch (LoginException e) {
        }
    }

    /*
     * Method: LoginContext.login()
     * 
    public final void testLogin_() throws Exception {

        Hashtable options = new Hashtable();
        
        // add required module to the current configuration
        MyConfig.addRequired("MyLoginModule", options);
        
        LoginContext context = new LoginContext(moduleName);

        context.login();
        context.login();
    }
    */

    public final void testLogout() {
        //TODO Implement logout().
    }

    public final void testGetSubject() {
        //TODO Implement getSubject().
        // test failed login
    }

    /**
     * @tests javax.security.auth.login.LoginContext
     */
    public void test_LoginContext_defaultConfigProvider() throws Exception {

        // test: LoginContext constructor fails because there are no config
        // files to be read (the test is modification of test case
        // in ConfigurationTest)

        // no login.config.url.N security properties should be set
        String javaSecurityFile = TestUtils
                .createJavaPropertiesFile(new Properties());

        // tmp user home to avoid presence of ${user.home}/.java.login.config
        String tmpUserHome = System.getProperty("java.io.tmpdir")
                + File.separatorChar + "tmpUserHomeForLoginContextTest";
        File dir = new File(tmpUserHome);
        if (!dir.exists()) {
            dir.mkdirs();
            dir.deleteOnExit();
        }
        String javaLoginConfig = tmpUserHome + File.separatorChar
                + ".java.login.config";
        assertFalse("There should be no login config file: " + javaLoginConfig,
                new File(javaLoginConfig).exists());

        String[] arg = new String[] { "-Duser.home=" + tmpUserHome,
                "-Djava.security.properties=" + javaSecurityFile,
                NoConfigFileToBeRead.class.getName() };

        Support_Exec.execJava(arg, null, true);
    }

    public static class NoConfigFileToBeRead {

        // the test is based on assumption that security properties 
        // login.config.url.N are unset and there is no file
        // ${user.home}/.java.login.config
        public static void main(String[] args) throws LoginException {

            //reset path to alternative configuration file
            TestUtils.setSystemProperty(AUTH_LOGIN_CONFIG, null);

            Configuration.setConfiguration(null); // reset default config
            try {
                // Regression for HARMONY-771
                new LoginContext("0"); 
                fail("No expected SecurityException");
            } catch (SecurityException e) {
            }
        }
    }
    /**
     * @tests javax.security.auth.login.LoginContext.login()
     */
    public void test_login_resourcesLeakage() throws Exception {

        // This is a compatibility test.
        // The test verifies that LoginContext allows to invoke login() method
        // multiple times without invoking logout() before. In testing scenario
        // each login() invocation adds new credentials to the passed subject.
        Configuration.setConfiguration(new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[] { new AppConfigurationEntry(
                        MyModule.class.getName(),
                        LoginModuleControlFlag.REQUIRED,
                        new HashMap<String, Object>()) };
            }

            @Override
            public void refresh() {
            }
        });

        LoginContext context = new LoginContext("moduleName", new Subject());

        context.login();
        context.login();

        Subject subject = context.getSubject();

        assertEquals(2, subject.getPrivateCredentials().size());
        assertEquals(2, subject.getPublicCredentials().size());
    }

    public static class MyModule implements LoginModule {

        Subject sub;

        public boolean abort() throws LoginException {
            return false;
        }

        public boolean commit() throws LoginException {
            sub.getPrivateCredentials().add(new Object());
            return true;
        }

        public void initialize(Subject arg0, CallbackHandler arg1,
                Map<String, ?> arg2, Map<String, ?> arg3) {
            sub = arg0;
        }

        public boolean login() throws LoginException {
            sub.getPublicCredentials().add(new Object());
            return true;
        }

        public boolean logout() throws LoginException {
            return false;
        }
    }

    private static class MyConfig extends Configuration {

        private String appName;

        private ArrayList<AppConfigurationEntry> entries;

        public MyConfig() {
            entries = new ArrayList<AppConfigurationEntry>();
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {
            appName = applicationName;
            if (entries != null) {
                if (entries.size() == 0) {
                    return new AppConfigurationEntry[0];
                }
                AppConfigurationEntry[] appEntries = new AppConfigurationEntry[entries.size()];
                entries.toArray(appEntries);
                return appEntries;
            }
            return null;
        }

        @Override
        public void refresh() {
        }

        /**
         * Returns the last application name requested by LoginContext constructor
         *
         * @return the last application name
         */
        public static String getLastAppName() {
            return ((MyConfig) Configuration.getConfiguration()).appName;
        }

        /**
         * Reset configuration.
         * 
         * After invocation the configuration doesn't have login modules
         * and the method Configuration.getAppConfigurationEntry(String)
         * always returns null;
         */
        public static void resetConfiguration() {
            ((MyConfig) Configuration.getConfiguration()).entries = null;
        }

        /**
         * Appends required login module to the current configuration
         */
        public static void addRequired(String name, Map<String, ?> options) {
            ArrayList<AppConfigurationEntry> list = ((MyConfig) Configuration.getConfiguration()).entries;

            AppConfigurationEntry entry = new AppConfigurationEntry(
                    LoginContextTest.class.getName() + '$' + name,
                    javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options);

            list.add(entry);
        }
    }

    public static class MyCallbackHandler implements CallbackHandler {

        public static boolean initialized;

        public MyCallbackHandler() {
            initialized = true;
        }

        public void handle(Callback[] callbacks) {
        }
    }

    public static class MyLoginModule implements LoginModule {

        public static ArrayList<MyLoginModule> list = new ArrayList<MyLoginModule>();

        public static void reset() {
            list = new ArrayList<MyLoginModule>();
        }

        public boolean aborted;

        public boolean commited;

        public boolean initialized;

        public boolean logined;

        public boolean logouted;

        public Subject subject;

        public CallbackHandler handler;

        public Map<String, ?> sharedState;

        public Map<String, ?> options;

        public MyLoginModule() {
            list.add(this);
        }

        public boolean abort() throws LoginException {

            if (!initialized || !logined) {
                throw new AssertionError(
                        "MUST initialize and try to login first before abort");
            }
            aborted = true;
            return true;
        }

        public boolean commit() throws LoginException {

            if (!initialized || !logined) {
                throw new AssertionError(
                        "MUST initialize and try to login first before abort");
            }
            commited = true;
            return true;
        }

        public void initialize(Subject subject, CallbackHandler handler,
                Map<String, ?> sharedState, Map<String, ?> options) {

            if (logined || commited || aborted) {
                throw new AssertionError("MUST be initialized first");
            }

            this.subject = subject;
            this.handler = handler;
            this.sharedState = sharedState;
            this.options = options;

            initialized = true;
        }

        public boolean login() throws LoginException {

            if (!initialized) {
                throw new AssertionError("MUST be initialized first");
            }
            if (commited || aborted) {
                throw new AssertionError(
                        "MUST try to login first before commit/abort");
            }
            logined = true;
            return true;
        }

        public boolean logout() throws LoginException {
            logouted = true;
            return true;
        }
    }

    public static class TestLoginModule implements LoginModule {

        public boolean abort() throws LoginException {
            return false;
        }

        public boolean commit() throws LoginException {
            return false;
        }

        public void initialize(Subject arg0, CallbackHandler arg1, Map<String, ?> arg2,
                Map<String, ?> arg3) {
        }

        public boolean login() throws LoginException {
            return false;
        }

        public boolean logout() throws LoginException {
            return false;
        }
    }
}

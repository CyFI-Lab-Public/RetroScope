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

package org.apache.harmony.auth.tests.javax.security.auth.login;

import java.io.IOException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.BasicPermission;
import java.security.CodeSource;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import junit.framework.TestCase;


/**
 * Unit tests for LoginContext
 */
public class LoginContext1Test extends TestCase {

    private static final String CONFIG_NAME = "testConfiguration";

    private static final String DEFAULT_CBHANDLER_PROPERTY = "auth.login.defaultCallbackHandler";

    private static final int OPTIONAL = 0;

    private static final int REQUIRED = 1;

    private static final int REQUISITE = 2;

    private static final int SUFFICIENT = 3;

    /**
     * Converts short (local) class names (like TestLoginModule) into the
     * global (fully qualified) ones.<br>
     * For example:<br>
     * TestLoginModule => javax.security.auth.login.LoginContextTest$TestLoginModule
     * @param shortName
     * @return fully qualified name
     */
    private static String getGlobalClassName(String shortName) {
        return LoginContext1Test.class.getName() + "$" + shortName;
    }

    /**
     * Maps an integer value into appropriate LoginModuleControlFlag.
     * @param flag
     * @return
     */
    private static LoginModuleControlFlag mapControlFlag(int flag) {
        switch (flag) {
        case OPTIONAL:
            return LoginModuleControlFlag.OPTIONAL;
        case REQUIRED:
            return LoginModuleControlFlag.REQUIRED;
        case REQUISITE:
            return LoginModuleControlFlag.REQUISITE;
        case SUFFICIENT:
            return LoginModuleControlFlag.SUFFICIENT;
        }
        throw new Error("Unknown flag:" + flag);
    }

    /**
     * A special purpose Configuration.<br>
     * Special functions are:<br>
     * <il>
     * <li>it keeps track of the configuration names requested via
     * getAppConfigurationEntry - see {@link #wasTheNameQueried(String)}
     * <li>can dynamically add modules - see
     * add{Optional|Required|Requisite|Sufficient}
     * <li>with a presumption that the statically installed Configuration
     * is also of type TestConfig, allows to dynamically add modules to that
     * installed Configuration - see addInstalled{*}.
     * <li>can handle several Configurations with a different names for the
     * {@link #getAppConfigurationEntry(String)} - see
     * {@link #addConfig(String, Configuration)} (again, with a presumption that
     * the Configuration.getConfiguration() is instanceof TestConfig )
     * </il>
     */
    private static final class TestConfig extends Configuration {
        private String name;

        private final ArrayList<AppConfigurationEntry> entries = new ArrayList<AppConfigurationEntry>();

        // A map 'name'=>'some specific configuration'
        private final HashMap<String, Configuration> configs = new HashMap<String, Configuration>();

        // An array which keeps track of the configuration names requested.
        private final ArrayList<String> requests = new ArrayList<String>();

        public TestConfig() {
        }

        public TestConfig(String name) {
            this.name = name;
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {

            if (!requests.contains(appName)) {
                requests.add(appName);
            }

            if (name == null || !name.equals(appName)) {

                Configuration conf = configs.get(appName);
                if (conf != null) {
                    return conf.getAppConfigurationEntry(appName);
                }

                if (!CONFIG_NAME.equals(appName)) {
                    return null;
                }
            }

            AppConfigurationEntry ret[] = new AppConfigurationEntry[entries
                    .size()];
            entries.toArray(ret);
            return ret;
        }

        @Override
        public void refresh() {
            // do nothing
        }

        public boolean wasTheNameQueried(String name) {
            return requests.contains(name);
        }

        String addRequired(String name) {
            return add(name,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    new HashMap<String, Object>());
        }

        String addOptional(String name) {
            return add(name,
                    AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL,
                    new HashMap<String, Object>());
        }

        String addSufficient(String name) {
            return add(name,
                    AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                    new HashMap<String, Object>());
        }

        String addRequisite(String name) {
            return add(name, LoginModuleControlFlag.REQUISITE, new HashMap<String, Object>());
        }

        String add(int type, String name, Map<String, ?> options) {
            return add(name, mapControlFlag(type), options);
        }

        String add(String name,
                AppConfigurationEntry.LoginModuleControlFlag flag, Map<String, ?> options) {
            String fullName = getGlobalClassName(name);
            AppConfigurationEntry entry = new AppConfigurationEntry(fullName,
                    flag, options);
            entries.add(entry);
            return fullName;
        }

        public static String addInstalledRequired(String name) {
            return ((TestConfig) Configuration.getConfiguration())
                    .addRequired(name);
        }

        public static String addInstalledOptional(String name) {
            return ((TestConfig) Configuration.getConfiguration())
                    .addOptional(name);
        }

        public static String addInstalledRequisite(String name) {
            return ((TestConfig) Configuration.getConfiguration())
                    .addRequisite(name);
        }

        public static String addInstalledSufficient(String name) {
            return ((TestConfig) Configuration.getConfiguration())
                    .addSufficient(name);
        }

        public static String addInstalled(int type, String name) {
            return ((TestConfig) Configuration.getConfiguration()).add(type,
                    name, new HashMap<String, Object>());
        }

        public static void clear() {
            ((TestConfig) Configuration.getConfiguration()).entries.clear();
            ((TestConfig) Configuration.getConfiguration()).configs.clear();
        }

        public static void addConfig(String name, Configuration conf) {
            ((TestConfig) Configuration.getConfiguration()).configs.put(name,
                    conf);
        }

        public static TestConfig get() {
            return (TestConfig) Configuration.getConfiguration();
        }
    }

    /**
     * A special-purpose LoginModule.<br>
     * It has the following features:<br>
     * <il>
     * <li>its behaviour is managed - either on by-instance level (via a flags
     * passed to its ctor) ar on a global level - through the static mask
     *
     * <li>the behaviour managed includes: returning a specified value
     * (true/false) from defined methods/ctor, or throwing an
     * Error/RuntimeException/LoginException -either specified by user or
     * created dynamically - from defined methods
     *
     * <li>it keeps track of instances created
     *
     * <li>each instance keeps track of which method was called
     * </il>
     *
     * The behaviour can be managed either for each particular instance - by
     * passing an appropriate mask into ctor, or can be managed globally - via
     * static field {@link #staticMask}.<br>
     * By default, all the methods return <code>true</code> (success) and do not
     * throw anything.<br>
     * The constants <b>FAIL_AT_*</b> shows where to fail. Then if
     * {@link staticRE} field is set, then this RuntimeException will be thrown,
     * otherwise if {@link #staticErr} is set, then this Error will be thrown,
     * otherwise, if {@link #staticLE} is set, then this LoginException will be
     * thrown, otherwise, finally, a new LoginException will be constructed and
     * thrown.<br>
     * The constants <b>FALSE_AT_*</b> shows which method must return
     * <code>false</code>.
     * The <b><code>FAIL_AT_*</code></b> constants have priority before
     * <b><code>FALSE_AT_*</code></b><br>
     * Note: if an instance executes <code>FAIL_AT_CTOR</code>, then this
     * instance do <b>not</b> get tracked.
     */

    public static class TestLoginModule implements LoginModule {
        public static final int FAIL_AT_CTOR = 0x001;

        public static final int FAIL_AT_INIT = 0x002;

        public static final int FAIL_AT_LOGIN = 0x004;

        public static final int FAIL_AT_COMMIT = 0x008;

        public static final int FAIL_AT_LOGOUT = 0x010;

        public static final int FAIL_AT_ABORT = 0x020;

        //
        public static final int FALSE_AT_LOGIN = 0x040;

        public static final int FALSE_AT_COMMIT = 0x080;

        public static final int FALSE_AT_LOGOUT = 0x100;

        public static final int FALSE_AT_ABORT = 0x200;

        // A message used to construct LoginException
        private static final String msg = "Managed test exception. Nothing serious.";

        public static int staticMask = 0;

        public static RuntimeException staticRE;

        public static Error staticERR;

        public static LoginException staticLE;

        //
        protected static ArrayList<TestLoginModule> instances = new ArrayList<TestLoginModule>();

        private boolean initCalled;

        private boolean loginCalled;

        private boolean abortCalled;

        private boolean commitCalled;

        private boolean logoutCalled;

        public CallbackHandler cbHandler;

        //
        private int mask;

        private RuntimeException re;

        private Error err;

        private LoginException le;

        /**
         * returns <code>i</code> item from the list of tracked items.
         */
        static TestLoginModule get(int i) {
            return instances.get(i);
        }

        /**
         * Clears the list of tracked instances.
         */
        static void clear() {
            instances.clear();
        }

        /**
         * Returns the number of items tracked.
         */
        static int size() {
            return instances.size();
        }

        public TestLoginModule() {
            this(-1, staticRE, staticERR, staticLE);
        }

        protected TestLoginModule(int mask) {
            this(mask, null, null, null);
        }

        protected TestLoginModule(int mask, RuntimeException re) {
            this(mask, re, null, null);
        }

        protected TestLoginModule(int mask, Error err) {
            this(mask, null, err, null);
        }

        protected TestLoginModule(int mask, LoginException le) {
            this(mask, null, null, le);
        }

        /**
         * Constructs and initializes instance of the TestLoginModule.<br>
         * If you want the instance to use {@link #staticMask} then pass
         * <code>-1</code> as <code>mask</code>. To fail at nowhere, pass
         * <code>0</code> as <code>mask</code>.<br>
         * The appropriate <code>re</code>, <code>err</code> and
         * <code>le</code> will be used to throw appropriate Throwable (if any).
         *
         * @param mask
         * @param re
         * @param err
         * @param le
         */
        protected TestLoginModule(int mask, RuntimeException re, Error err,
                LoginException le) {
            this.mask = mask;
            this.re = re;
            this.err = err;
            this.le = le;
            boolean doit = ((mask == -1) && (staticMask & FAIL_AT_CTOR) != 0)
                    || ((mask != -1) && (mask & FAIL_AT_CTOR) != 0);
            if (doit) {
                check();
                throw new RuntimeException(msg);
            }
            instances.add(this);
        }

        /**
         * Checks whether the instance variables are set and throw either
         * RuntimeException (checked first) or Error (checked next).<br>
         * If none of them specified, then the method returns silently.
         * @throws RuntimeException
         * @throws Error
         */
        private final void check() {
            if (re != null) {
                throw re;
            }
            if (err != null) {
                throw err;
            }
        }

        /**
         * This method calls {@link #check()} first to check for possible
         * RuntimeException or Error.<br>
         * Then, it checks whether an instance variable specifying
         * LoginException to throw is set. If the variable is set, then this
         * LoginException is thrown. Otherwise new LoginException is created
         * and thrown.
         *
         * @throws LoginException
         */
        private final void throw_le() throws LoginException {
            check();
            throw le == null ? new LoginException(msg) : le;
        }

        /**
         * Checks whether the passed <code>msk</code> is set - either at
         * instance level or on the global level and returns appropriate
         * value.<br>
         * If this instance's <code>mask</mask> is not set, then
         * {@link #staticMask} is checked. If the <code>staticMask</code> is
         * also not set, then the method returns <code>true</code>. If either
         * the static mask or the instance mask is set and match the
         * <code>msk</code> passed, then the method returns false.
         * @param msk
         * @return
         */
        private final boolean ret(int msk) {

            if (mask == -1 && (staticMask & msk) != 0) {
                return false;
            }
            if (mask != -1 && (mask & msk) != 0) {
                return false;
            }
            return true;
        }

        public void initialize(Subject subject,
                CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {

            this.cbHandler = callbackHandler;
            initCalled = true;

            boolean doit = ((mask == -1) && (staticMask & FAIL_AT_INIT) != 0)
                    || ((mask != -1) && (mask & FAIL_AT_INIT) != 0);
            if (doit) {
                check();
                throw new RuntimeException(msg);
            }
        }

        /**
         * See javax.security.auth.spi.LoginModule.login()
         */
        public boolean login() throws LoginException {
            loginCalled = true;
            boolean doit = ((mask == -1) && (staticMask & FAIL_AT_LOGIN) != 0)
                    || ((mask != -1) && (mask & FAIL_AT_LOGIN) != 0);
            if (doit) {
                throw_le();
            }
            return ret(FALSE_AT_LOGIN);
        }

        /**
         * See javax.security.auth.spi.LoginModule.commit()
         */
        public boolean commit() throws LoginException {
            commitCalled = true;
            boolean doit = ((mask == -1) && (staticMask & FAIL_AT_COMMIT) != 0)
                    || ((mask != -1) && (mask & FAIL_AT_COMMIT) != 0);
            if (doit) {
                throw_le();
            }
            return ret(FALSE_AT_COMMIT);
        }

        /**
         * See javax.security.auth.spi.LoginModule.logout()
         */
        public boolean logout() throws LoginException {
            logoutCalled = true;
            boolean doit = ((mask == -1) && (staticMask & FAIL_AT_LOGOUT) != 0)
                    || ((mask != -1) && (mask & FAIL_AT_LOGOUT) != 0);
            if (doit) {
                throw_le();
            }
            return ret(FALSE_AT_LOGOUT);
        }

        /**
         * See javax.security.auth.spi.LoginModule.abort()
         */
        public boolean abort() throws LoginException {
            abortCalled = true;
            boolean doit = ((mask == -1) && (staticMask & FAIL_AT_ABORT) != 0)
                    || ((mask != -1) && (mask & FAIL_AT_ABORT) != 0);
            if (doit) {
                throw_le();
            }
            return ret(FALSE_AT_ABORT);
        }
    }

    /**
     * A special-purpose LoginModule whose operations are always successful.
     */
    public static final class TestLoginModule_Success extends TestLoginModule {
        public TestLoginModule_Success() {
            // '0' here means 'the mask is set, fail at nowhere'
            super(0);
        }
    }

    /**
     * A special-purpose LoginModule whose initialize() method throws
     * RuntimeException.
     */
    public static final class TestLoginModule_InitFails extends TestLoginModule {
        public TestLoginModule_InitFails() {
            super(FAIL_AT_INIT, new RuntimeException(
                    "init: test runtime exception."
                            + " don't worry about it too much."));
        }
    }

    /**
     * A special-purpose LoginModule whose ctor() throws RuntimeException.
     */
    public static final class TestLoginModule_CtorFails extends TestLoginModule {
        public TestLoginModule_CtorFails() {
            super(FAIL_AT_CTOR, new RuntimeException(
                    "ctor: test runtime exception."
                            + " don't worry about it too much."));
        }
    }

    /**
     * A special-purpose LoginModule whose commit() method throws
     * RuntimeException.
     */
    public static final class TestLoginModule_CommitFails extends
            TestLoginModule {
        public TestLoginModule_CommitFails() {
            super(FAIL_AT_COMMIT, new RuntimeException(
                    "commit: test runtime exception."
                            + " don't worry about it too much."));
        }
    }

    /**
     * A special-purpose LoginModule whose methods (but ctor) throw
     * LoginException.
     */
    public static final class TestLoginModule_Fail extends TestLoginModule {
        public TestLoginModule_Fail() {
            super(FAIL_AT_INIT | FAIL_AT_LOGIN | FAIL_AT_COMMIT
                    | FAIL_AT_LOGOUT | FAIL_AT_ABORT);
        }
    }

    /**
     * A special-purpose LoginModule whose methods (where applicable) return
     * <code>false</code>.
     */
    public static final class TestLoginModule_Ignore extends TestLoginModule {
        public TestLoginModule_Ignore() {
            super(FALSE_AT_LOGIN | FALSE_AT_COMMIT | FALSE_AT_LOGOUT
                    | FALSE_AT_ABORT);
        }
    }

    /**
     * A special-purpose CallbackHandler which keeps track of instances
     * created.<br>
     */
    public static class TestCallbackHandler implements CallbackHandler {
        protected static ArrayList<TestCallbackHandler> instances = new ArrayList<TestCallbackHandler>();

        /**
         * Returns number of the instances tracked.
         */
        public static int size() {
            return instances.size();
        }

        /**
         * Clears the stored items.
         */
        public static void clear() {
            instances.clear();
        }

        /**
         * ctor.
         */
        public TestCallbackHandler() {
            instances.add(this);
        }

        /**
         * Does nothing.
         */
        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {

        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Configuration.setConfiguration(new TestConfig());
        clear();
    }

    private void clear() {
        TestConfig.clear();
        TestLoginModule.clear();
        TestCallbackHandler.clear();
        TestLoginModule.staticRE = null;
        TestLoginModule.staticERR = null;
        TestLoginModule.staticLE = null;
        TestLoginModule.staticMask = 0;
        //
        Security.setProperty(DEFAULT_CBHANDLER_PROPERTY, "");
    }

    /**
     * Tests LoginContext(String)
     */
    public void testLoginContextString() throws Exception {
        // Must not accept nulls
        try {
            new LoginContext(null);
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        // Invalid names (which are not presented in Config) must not
        // be accepted - of no 'other' configuration exists
        String name = "some strange and non existing name";
        try {
            new LoginContext(name);
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        assertTrue(TestConfig.get().wasTheNameQueried(name));

        // Invalid names must be processed as 'other' if such Configuration
        // exists
        TestConfig conf = new TestConfig("other");
        conf.addRequired("TestLoginModule_Success");
        TestConfig.addConfig("other", conf);
        name = "this strange and non existing name will be treated as 'other'";
        new LoginContext(name);

        //1st, static installed Config must be queried
        assertTrue(TestConfig.get().wasTheNameQueried(name));
        //2d, the another config must be queried for other
        assertTrue(TestConfig.get().wasTheNameQueried("other"));

        // Valid names which exist but does not have any entries must
        // also be accepted. Empty set will be considered as a problem
        // much later - at login() phase
        new LoginContext(CONFIG_NAME);
        //
        Security.setProperty(DEFAULT_CBHANDLER_PROPERTY, "no such class");
        try {
            new LoginContext(CONFIG_NAME);
            fail("must not pass here");
        } catch (LoginException ex) {
            // gut
        }
        String klassName = getGlobalClassName("TestCallbackHandler");

        Security.setProperty(DEFAULT_CBHANDLER_PROPERTY, klassName);
        // This also shows that the cbHandler is instantiated at the ctor
        new LoginContext(CONFIG_NAME);
        assertEquals(1, TestCallbackHandler.size());
        // ugh... cant set 'null' here...
        Security.setProperty(DEFAULT_CBHANDLER_PROPERTY, "");
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext(String, CallbackHandler)
     */
    public void testLoginContextStringCallbackHandler() {
        // Must not accept nulls as CallbackHandler, name
        // The exception to be thrown is LoginException and not NPE
        try {
            new LoginContext(CONFIG_NAME, (CallbackHandler) null);
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        try {
            new LoginContext(null, new TestCallbackHandler());
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext(String, Subject)
     */
    public void testLoginContextStringSubject() {
        // Must not accept nulls as Subject, name
        // The exception to be thrown is LoginException and not NPE
        try {
            new LoginContext(CONFIG_NAME, (Subject) null);
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        try {
            new LoginContext(null, new Subject());
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext(String, Subject, CallbackHandler)
     */
    public void testLoginContextStringSubjectCallbackHandler() {
        // The exceptions to be thrown are LoginExceptions and not NPEs

        // Must not accept null as Subject, CallbackHandler, name
        try {
            new LoginContext(CONFIG_NAME, (Subject) null,
                    new TestCallbackHandler());
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        try {
            new LoginContext(CONFIG_NAME, new Subject(), null);
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        try {
            new LoginContext(null, new Subject(), new TestCallbackHandler());
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext(String, Subject, CallbackHandler, Configuration)
     */
    public void testLoginContextStringSubjectCallbackHandlerConfiguration()
            throws Exception {
        // Must accept null everywhere, but at name
        try {
            new LoginContext(null, new Subject(), new TestCallbackHandler(),
                    new TestConfig());
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        new LoginContext(CONFIG_NAME, null, null, null);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * Make sure that the proper (context) class loader is used.
     */
    public void testLogin_minus1() throws Exception {
        final ArrayList<String> requests = new ArrayList<String>();
        ClassLoader saveCCL = Thread.currentThread().getContextClassLoader();

        ClassLoader testClassLoader = new ClassLoader() {
            @Override
            protected synchronized Class<?> loadClass(String klassName,
                    boolean resolve) throws ClassNotFoundException {
                requests.add(klassName);
                return super.loadClass(klassName, resolve);
            }
        };
        Thread.currentThread().setContextClassLoader(testClassLoader);
        String klassName = TestConfig
                .addInstalledRequired("NoSuchClassHere");
        try {
            LoginContext lc = new LoginContext(CONFIG_NAME);
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // gut
        } finally {
            Thread.currentThread().setContextClassLoader(saveCCL);
        }
        // If failed, then it seems, that wrong (not context) class loader was
        // used - the class was not requested to load
        assertTrue(requests.contains(klassName));
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * Second attempt to login() on context already logged, must be successful.<br>
     * This is how the RI works.
     */
    public void testLogin_00() throws Exception {
        TestConfig.addInstalledSufficient("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        lc.login();
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If no Subject provided, then new Subject created and this subject is
     * used for all subsequent operations.
     */
    public void testLogin_01() throws Exception {
        TestConfig.addInstalledSufficient("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        Subject subj0 = lc.getSubject();
        assertNotNull(subj0);
        lc.logout();
        //
        lc.login();
        Subject subj1 = lc.getSubject();
        assertSame(subj0, subj1);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * init() must be called only once.
     */
    public void testLogin_02_0() throws Exception {
        TestConfig.addInstalledSufficient("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        assertEquals(1, TestLoginModule.size());
        assertTrue(TestLoginModule.get(0).initCalled);
        assertTrue(TestLoginModule.get(0).loginCalled);

        lc.logout();
        assertTrue(TestLoginModule.get(0).logoutCalled);
        //
        TestLoginModule.get(0).loginCalled = false;
        TestLoginModule.get(0).initCalled = false;

        lc.login();

        assertEquals(1, TestLoginModule.size());
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertFalse(TestLoginModule.get(0).initCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * init() must be called only once - even if it fails first time.
     */
    public void testLogin_02_1() throws Exception {
        TestConfig.addInstalledSufficient("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_INIT;

        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException _) {
            //ok
        }

        assertEquals(1, TestLoginModule.size());
        assertTrue(TestLoginModule.get(0).initCalled);
        assertFalse(TestLoginModule.get(0).loginCalled);
        assertFalse(TestLoginModule.get(0).commitCalled);
        assertTrue(TestLoginModule.get(0).abortCalled);
        assertFalse(TestLoginModule.get(0).logoutCalled);

        lc.logout();

        TestLoginModule.get(0).initCalled = false;
        TestLoginModule.get(0).loginCalled = false;
        TestLoginModule.get(0).commitCalled = false;
        TestLoginModule.get(0).abortCalled = false;
        TestLoginModule.get(0).logoutCalled = false;

        TestLoginModule.staticMask = 0;
        lc.login();

        assertEquals(1, TestLoginModule.size());
        assertFalse(TestLoginModule.get(0).initCalled);
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        assertFalse(TestLoginModule.get(0).abortCalled);
        assertFalse(TestLoginModule.get(0).logoutCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If any module not found, then overall attempt must fail.
     * If a module fails during it's ctor/init/commit - it must be processed
     * the same way as if it were failing during it's login().<br>
     * Various combinations are checked.
     */
    public void testLogin_03() throws Exception {
        // A little bit sophisticated structure
        // if variant[i][0] == -1 then:
        // 	- it starts a new variant
        //	- variants[i][1] contains description for the variant started
        //	- variants[i][2] contains expected result (true - success, false-failure) for the variant
        Object[][] variants = new Object[][] {
                //
                { new Integer(-1), "[if no class found - overall failure]",
                        Boolean.FALSE },
                { new Integer(OPTIONAL), "no such class" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },

                { new Integer(-1),
                        "[ctor failure treated as failed login() - 0] ",
                        Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_CtorFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },

                { new Integer(-1),
                        "[ctor failure treated as failed login() - 1] ",
                        Boolean.FALSE },
                { new Integer(REQUISITE), "TestLoginModule_CtorFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },

                { new Integer(-1),
                        "[init failure treated as failed login() - 0]",
                        Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_InitFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },

                { new Integer(-1),
                        "[init failure treated as failed login() - 1]",
                        Boolean.FALSE },
                { new Integer(REQUIRED), "TestLoginModule_InitFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase 0", Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_Fail" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase 1", Boolean.FALSE },
                { new Integer(REQUIRED), "no such class" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase 2", Boolean.FALSE },
                { new Integer(REQUISITE), "no such class" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase 3", Boolean.FALSE },
                { new Integer(SUFFICIENT), "no such class" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase 4", Boolean.FALSE },
                { new Integer(REQUIRED), "TestLoginModule_InitFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase 5", Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_InitFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Failing" },

                { new Integer(-1), "simple testcase 6", Boolean.TRUE },
                { new Integer(REQUISITE), "TestLoginModule_Success" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Failing" },

                { new Integer(-1), "simple testcase 7", Boolean.FALSE },
                { new Integer(REQUIRED), "TestLoginModule_CtorFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase 8", Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_CtorFails" },
                { new Integer(SUFFICIENT), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_Failing" },

                { new Integer(-1), "simple testcase 9", Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },
                { new Integer(SUFFICIENT), "TestLoginModule_CtorFails" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase A", Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },
                { new Integer(SUFFICIENT), "TestLoginModule_CommitFails" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },

                { new Integer(-1), "simple testcase B", Boolean.TRUE },
                { new Integer(OPTIONAL), "TestLoginModule_Success" },
                { new Integer(OPTIONAL), "TestLoginModule_CommitFails" },
                { new Integer(OPTIONAL), "TestLoginModule_Success" }, };

        TestConfig.clear();
        boolean expectedResult = ((Boolean) variants[0][2]).booleanValue();
        String caseName = variants[0][1].toString();

        int startIndex = 0;
        for (int i = 0; i < variants.length; i++) {
            int flag = ((Integer) variants[i][0]).intValue();
            if (flag == -1 || (i == variants.length - 1)) {
                if (i != 0) {
                    LoginContext lc = new LoginContext(CONFIG_NAME);
                    try {
                        lc.login();
                        if (!expectedResult) {
                            fail("must not pass here: caseStart@" + startIndex
                                    + "; desc=" + caseName);
                        }
                    } catch (LoginException ex) {
                        if (expectedResult) {
                            fail("must not pass here: caseStart@" + startIndex
                                    + "; desc=" + caseName);
                        }
                    }
                }
                if (i != variants.length - 1) {
                    caseName = variants[i][1].toString();
                    expectedResult = ((Boolean) variants[i][2]).booleanValue();
                }
                TestConfig.clear();
            } else {
                TestConfig.addInstalled(flag, variants[i][1].toString());
            }
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If module fails during it's ctor, then it must be
     * created/initialized/logged next time.
     */
    public void testLogin_04() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        assertEquals(0, TestLoginModule.size());
        LoginContext lc = new LoginContext(CONFIG_NAME);
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_CTOR;
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok
        }
        assertEquals(0, TestLoginModule.size());
        // fail nowhere
        TestLoginModule.staticMask = 0;
        lc.login(); // must be successful now
        assertEquals(1, TestLoginModule.size());
        assertTrue(TestLoginModule.get(0).initCalled);
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If module fails during init():
     * 		- its login() must NOT be called
     * 		- its abort() must be called anyway.
     */
    public void testLogin_05() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule_InitFails");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
        }

        assertFalse(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If module fails during init():
     * 		- its login() must NOT be called
     * 		- its login() MUST be called on the next attempt
     */
    public void testLogin_06() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_INIT;
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok
        }
        assertEquals(1, TestLoginModule.size());
        assertFalse(TestLoginModule.get(0).loginCalled);
        assertFalse(TestLoginModule.get(0).commitCalled); // self check
        // fail nowhere
        TestLoginModule.staticMask = 0;
        lc.login(); // must be successful now
        // no new module must be created
        assertEquals(1, TestLoginModule.size());
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If no class found, then following modules must not be instantiated
     */
    public void testLogin_07() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule_Success");
        TestConfig.addInstalledOptional("no such class");
        TestConfig.addInstalledOptional("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            //ok
        }
        assertEquals(1, TestLoginModule.size());
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).abortCalled);
        assertFalse(TestLoginModule.get(0).commitCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * RuntimeException-s (if any) must be handled and then LoginException must
     * be thrown
     */
    public void testLogin_08_0() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticRE = new RuntimeException("ctor");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        //
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_CTOR;
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * RuntimeException-s (if any) must be handled and then LoginException must
     * be thrown
     */
    public void testLogin_08_1() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticRE = new RuntimeException("init");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_INIT;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * RuntimeException-s (if any) must be handled and then LoginException must be thrown
     */
    public void testLogin_08_2() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticRE = new RuntimeException("login");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGIN;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * RuntimeException-s (if any) must be handled and then LoginException must be thrown
     */
    public void testLogin_08_3() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticRE = new RuntimeException("commit");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_COMMIT;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * Error-s (if any) must be handled and wrapped into LoginException
     */
    public void testLogin_09_0() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticERR = new Error("ctor");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_CTOR;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        //
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * Error-s (if any) must be handled and wrapped into LoginException
     */
    public void testLogin_09_1() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticERR = new Error("init");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_INIT;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * Error-s (if any) must be handled and wrapped into LoginException
     */
    public void testLogin_09_2() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticERR = new Error("login");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGIN;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * Error-s (if any) must be handled and wrapped into LoginException
     */
    public void testLogin_09_3() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticERR = new Error("commit");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_COMMIT;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // ok. the RI does not initCause() with a given RuntimeException
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * A failure during commit() must be treated exactly as a failure during
     * login()/whatever
     */
    public void testLogin_0A_0() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule_Success");
        TestConfig.addInstalledOptional("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_COMMIT;
        LoginContext lc = new LoginContext(CONFIG_NAME);

        lc.login();

        assertEquals(2, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        assertFalse(TestLoginModule.get(0).abortCalled);
        //
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertTrue(TestLoginModule.get(1).commitCalled);
        assertFalse(TestLoginModule.get(1).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * see testLogin_0A_0().
     */
    public void testLogin_0A_1() throws Exception {
        TestConfig.addInstalledOptional("TestLoginModule_Success");
        TestConfig.addInstalledRequired("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_COMMIT;
        TestLoginModule.staticLE = new LoginException();
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException le) {
            assertSame(TestLoginModule.staticLE, le);
        }

        assertEquals(2, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        assertTrue(TestLoginModule.get(0).abortCalled);
        //
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertTrue(TestLoginModule.get(1).commitCalled);
        assertTrue(TestLoginModule.get(1).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * A failure during commit() must not stop further commit()s get called.
     */
    public void testLogin_0B() throws Exception {
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_COMMIT;
        TestConfig.addInstalledRequired("TestLoginModule");
        TestConfig.addInstalledOptional("TestLoginModule_Success");
        TestConfig.addInstalledOptional("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);

        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        assertEquals(3, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        //
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertTrue(TestLoginModule.get(1).commitCalled);
        //
        assertTrue(TestLoginModule.get(2).loginCalled);
        assertTrue(TestLoginModule.get(2).commitCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * A failure during abort() must not stop further abort()s get called.
     */
    public void testLogin_0C() throws Exception {
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_ABORT
                | TestLoginModule.FAIL_AT_COMMIT;
        TestConfig.addInstalledRequired("TestLoginModule");
        TestConfig.addInstalledOptional("TestLoginModule_Success");
        TestConfig.addInstalledOptional("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);

        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        assertEquals(3, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        assertTrue(TestLoginModule.get(0).abortCalled);
        //
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertTrue(TestLoginModule.get(1).commitCalled);
        assertTrue(TestLoginModule.get(1).abortCalled);
        //
        assertTrue(TestLoginModule.get(2).loginCalled);
        assertTrue(TestLoginModule.get(2).commitCalled);
        assertTrue(TestLoginModule.get(2).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If any a module returns false from commit() - then nothing happens.
     */
    public void testLogin_0D() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule_Success");
        TestLoginModule.staticMask = TestLoginModule.FALSE_AT_COMMIT;
        TestConfig.addInstalledRequired("TestLoginModule");
        LoginContext lc = new LoginContext(CONFIG_NAME);

        lc.login();

        assertEquals(2, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        assertFalse(TestLoginModule.get(0).abortCalled);
        //
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertTrue(TestLoginModule.get(1).commitCalled);
        assertFalse(TestLoginModule.get(1).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If any REQUIRED module returns false from login() - this does
     * not break the overall attempt.<br>
     * It sounds odd, but its commit() method will also be called.
     */
    public void testLogin_0E_0() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule_Success");
        TestLoginModule.staticMask = TestLoginModule.FALSE_AT_LOGIN;
        TestConfig.addInstalledRequired("TestLoginModule");
        LoginContext lc = new LoginContext(CONFIG_NAME);

        lc.login();

        assertEquals(2, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).initCalled);
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        assertFalse(TestLoginModule.get(0).abortCalled);
        //
        assertTrue(TestLoginModule.get(1).initCalled);
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertTrue(TestLoginModule.get(1).commitCalled);
        assertFalse(TestLoginModule.get(1).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If any REQUISITE module returns false from login() - this does
     * not break the overall attempt.<br>
     * It sounds odd, but its commit() method will be called anyway.
     */
    public void testLogin_0E_1() throws Exception {
        TestConfig.addInstalledRequisite("TestLoginModule_Success");
        TestLoginModule.staticMask = TestLoginModule.FALSE_AT_LOGIN;
        TestConfig.addInstalledRequisite("TestLoginModule");
        LoginContext lc = new LoginContext(CONFIG_NAME);

        lc.login();

        assertEquals(2, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).initCalled);
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertTrue(TestLoginModule.get(0).commitCalled);
        assertFalse(TestLoginModule.get(0).abortCalled);
        //
        assertTrue(TestLoginModule.get(1).initCalled);
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertTrue(TestLoginModule.get(1).commitCalled);
        assertFalse(TestLoginModule.get(1).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * If any REQUIRED module fails during its login() -
     * then its abort() gets called anyway.
     */
    public void testLogin_0F() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule_Success");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGIN;
        TestConfig.addInstalledRequired("TestLoginModule");
        LoginContext lc = new LoginContext(CONFIG_NAME);

        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException ex) {
            // gut
        }

        assertEquals(2, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).initCalled);
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertFalse(TestLoginModule.get(0).commitCalled);
        assertTrue(TestLoginModule.get(0).abortCalled);
        //
        assertTrue(TestLoginModule.get(1).initCalled);
        assertTrue(TestLoginModule.get(1).loginCalled);
        assertFalse(TestLoginModule.get(1).commitCalled);
        assertTrue(TestLoginModule.get(1).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.login()<br>
     * RI investigation: check if an exception thrown from abort() method gets
     * rethrown, or a new LoginException is created.
     */
    public void testLogin_10() throws Exception {
        TestLoginModule.staticMask = TestLoginModule.FALSE_AT_LOGIN
                | TestLoginModule.FAIL_AT_ABORT;

        TestLoginModule.staticLE = new LoginException();

        TestConfig.addInstalledRequired("TestLoginModule");
        LoginContext lc = new LoginContext(CONFIG_NAME);

        try {
            lc.login();
        } catch (LoginException ex) {
            // RI does not rethrow this exception, but I do.
            // Anyway, the login() failed - that is expected result
            // assertSame( ex, TestLoginModule.staticLE);
        }

        assertEquals(1, TestLoginModule.size());
        //
        assertTrue(TestLoginModule.get(0).initCalled);
        assertTrue(TestLoginModule.get(0).loginCalled);
        assertFalse(TestLoginModule.get(0).commitCalled);
        assertTrue(TestLoginModule.get(0).abortCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.logout()<br>
     * Calling logout() simply invokes logout() to be called.
     */
    public void testLogout_00_0() throws Exception {
        // No modules configured - must fail in both login() and logout()
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        try {
            lc.logout();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.logout()<br>
     * Calling logout() on a context that had login() called, but is still not
     * logged in must be successful - as it just invokes logout() for modules
     */
    public void testLogout_00_1() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGIN;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        lc.logout();
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.logout()<br>
     * Calling logout() on a context which had no login() called - must fail.
     */
    public void testLogout_00_2() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        try {
            lc.logout();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        assertEquals(0, TestLoginModule.size());
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.logout()<br>
     * An exception thrown from inside logout() must not stop calls to other logout()s.
     */
    public void testLogout_01() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGOUT;
        TestConfig.addInstalledRequired("TestLoginModule_Success");

        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        try {
            lc.logout();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }
        assertEquals(2, TestLoginModule.size());
        assertTrue(TestLoginModule.get(0).logoutCalled);
        assertTrue(TestLoginModule.get(1).logoutCalled);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.logout()<br>
     * It must rethrow LoginException thrown from the logout()
     */
    public void testLogout_02() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGOUT;
        TestLoginModule.staticLE = new LoginException();

        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        try {
            lc.logout();
            fail("must not pass here");
        } catch (LoginException le) {
            assertSame(TestLoginModule.staticLE, le);
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.logout()<br>
     * It must wrap RuntimeExceptions into LoginException
     */
    public void testLogout_03() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGOUT;
        TestLoginModule.staticRE = new RuntimeException();

        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        try {
            lc.logout();
            fail("must not pass here");
        } catch (LoginException le) {
            // ok
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.logout()<br>
     * It must wrap Errors into LoginException
     */
    public void testLogout_04() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGOUT;
        TestLoginModule.staticERR = new Error();

        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        try {
            lc.logout();
            fail("must not pass here");
        } catch (LoginException le) {
            // ok
        }
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.getSubject()<br>
     * Must return null if no subject was provided by user an no login attempt
     * was made.
     */
    public void testGetSubject_00() throws Exception {
        LoginContext lc = new LoginContext(CONFIG_NAME);
        assertNull(lc.getSubject());
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.getSubject()<br>
     * Must return subject created after successful login.
     */
    public void testGetSubject_01() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule_Success");
        LoginContext lc = new LoginContext(CONFIG_NAME);
        lc.login();
        assertNotNull(lc.getSubject());
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests LoginContext.getSubject()<br>
     * Must return null until successful login().
     */
    public void testGetSubject_02() throws Exception {
        TestConfig.addInstalledRequired("TestLoginModule");
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGIN;
        LoginContext lc = new LoginContext(CONFIG_NAME);
        assertNull(lc.getSubject());
        try {
            lc.login();
        } catch (LoginException _) {
            // ok
        }
        assertNull(lc.getSubject());
        TestLoginModule.staticMask = 0;
        lc.login();
        Subject saveSubject = lc.getSubject();
        assertNotNull(saveSubject);
        // Must return the same subject on subsequent calls
        lc.logout();
        lc.login();
        assertSame(lc.getSubject(), saveSubject);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests context usage.
     * Case 0: If no Config provided by user, then LoginContext uses
     * its own context to invoke LoginModule's methods.
     */
    public void testContextUsage_0() throws Exception {
        Subject dummySubj = new Subject();
        final DomainCombiner dc = new SubjectDomainCombiner(dummySubj);
        AccessControlContext acc = new AccessControlContext(AccessController
                .getContext(), dc);
        PrivilegedExceptionAction<Void> action = new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                implTestContextUsage(true, dc);
                return null;
            }
        };
        AccessController.doPrivileged(action, acc);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    /**
     * Tests context usage.
     * Case 1: If Config was provided by user, then LoginContext
     * uses stored user's context and performs all call to LoginModule's
     * methods in that context.
     */
    public void testContextUsage_1() throws Exception {
        Subject dummySubj = new Subject();
        final DomainCombiner dc = new SubjectDomainCombiner(dummySubj);
        AccessControlContext acc = new AccessControlContext(AccessController
                .getContext(), dc);
        PrivilegedExceptionAction<Void> action = new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                implTestContextUsage(false, dc);
                return null;
            }
        };
        AccessController.doPrivileged(action, acc);
        // additional cleanup to make it PerfTests compatible
        clear();
    }

    static boolean TestLoginModule_Contexted_staticDone = false;

    static boolean TestCallbackHandler_Contexted_staticDone = false;

    /**
     * A special-purpose CallbackHandler<br>
     * with the following abilities:<br>
     * <il>
     * <li>it shows whether its class was initialized or not (via the external
     * variable <code>boolean TestCallbackHandler_Contexted_staticDone</code>)
     *
     * <li>keeps track of the active security contexts for each operation -
     * static{}, ctor(), handle()
     * </il>
     */
    public static class TestCallbackHandler_Contexted extends
            TestCallbackHandler {
        public static AccessControlContext accStatic;

        public AccessControlContext accCtor;

        public AccessControlContext accHandle;

        static {
            accStatic = AccessController.getContext();
            TestCallbackHandler_Contexted_staticDone = true;
        }

        /**
         * Provides an easy access to the first CallbackHandler created.
         */
        public static TestCallbackHandler_Contexted item() {
            return (TestCallbackHandler_Contexted) instances.get(0);
        }

        public TestCallbackHandler_Contexted() {
            super();
            accCtor = AccessController.getContext();
        }

        @Override
        public void handle(Callback[] cbs) {
            accHandle = AccessController.getContext();
        }
    }

    /**
     * A special-purpose LoginModule<br>
     * with the following features:<br>
     * <il>
     * <li>it shows whether its class was initialized or not (via the external
     * variable <code>boolean TestLoginModule_Contexted_staticDone</code>)
     *
     * <li>keeps track of the active security contexts for each operation -
     * static{}, ctor(), initialize/login/commit/logout/abort
     *
     * <li>it also invokes callback handler (if any) during its login() method
     * </il>
     */
    public static class TestLoginModule_Contexted extends TestLoginModule {
        public static AccessControlContext accStatic;
        static {
            accStatic = AccessController.getContext();
            TestLoginModule_Contexted_staticDone = true;
        }

        /**
         * Provides an easy access to instances of
         * TestLoginModule_Contexted_staticDone.
         */
        public static TestLoginModule_Contexted item(int i) {
            return (TestLoginModule_Contexted) TestLoginModule.get(i);
        }

        /**
         * Provides an easy access to the very first instance of
         * TestLoginModule_Contexted_staticDone.
         */
        public static TestLoginModule_Contexted item() {
            return item(0);
        }
        // Below are AccessControlContext-s for the appropriate operations:
        public AccessControlContext accCtor;

        public AccessControlContext accInit;

        public AccessControlContext accLogin;

        public AccessControlContext accCommit;

        public AccessControlContext accLogout;

        public AccessControlContext accAbort;

        public TestLoginModule_Contexted() {
            super();
            accCtor = AccessController.getContext();
        }

        @Override
        public void initialize(Subject subject,
                CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
            accInit = AccessController.getContext();
            super.initialize(subject, callbackHandler, sharedState, options);
        }

        @Override
        public boolean login() throws LoginException {
            accLogin = AccessController.getContext();
            if (cbHandler != null) {
                try {
                    cbHandler.handle(null);
                } catch (UnsupportedCallbackException _) {
                    throw (LoginException) new LoginException().initCause(_);
                } catch (IOException _) {
                    throw (LoginException) new LoginException().initCause(_);
                }
            }
            return super.login();
        }

        @Override
        public boolean commit() throws LoginException {
            accCommit = AccessController.getContext();
            return super.commit();
        }

        @Override
        public boolean logout() throws LoginException {
            accLogout = AccessController.getContext();
            return super.logout();
        }

        @Override
        public boolean abort() throws LoginException {
            accAbort = AccessController.getContext();
            return super.abort();
        }
    }
    /**
     * The real implementation of TestContextUsage_0 and TestContextUsage_1
     * methods.
     * @param useInstalledConfig
     * @param dc - which domain combiner to test for
     * @throws Exception
     */
    private void implTestContextUsage(boolean useInstalledConfig,
            DomainCombiner dc) throws Exception {

        // the class was not loaded/initialized yet. it's legal to test statics
        boolean checkModuleStatic = !TestLoginModule_Contexted_staticDone;
        boolean checkCBHandlerStatic = !TestCallbackHandler_Contexted_staticDone;
        //        //_FIXME: debug
        //        if( checkModuleStatic ) {
        //            System.err.println("module static was checked. r u happy ? "+useInstalledConfig);
        //        }
        //        if( checkCBHandlerStatic ) {
        //            System.err.println("handler static was checked. r u happy ? "+useInstalledConfig);
        //        }
        //        //~fixme
        TestConfig.addInstalledRequired("TestLoginModule_Contexted");

        //
        //CallbackHandler cbHandler = new TestCallbackHandler_Contexted();
        // The property will be cleared at setUp()
        Security.setProperty(DEFAULT_CBHANDLER_PROPERTY,
                getGlobalClassName("TestCallbackHandler_Contexted"));

        LoginContext lc = new LoginContext(CONFIG_NAME, null, null,
                useInstalledConfig ? null : TestConfig.get());
        lc.login();
        lc.logout();
        //
        assertEquals(1, TestCallbackHandler.size());

        // now, get abort() called
        TestLoginModule.staticMask = TestLoginModule.FAIL_AT_LOGIN;
        try {
            lc.login();
            fail("must not pass here");
        } catch (LoginException _) {
            // gut
        }

        DomainCombiner match = useInstalledConfig ? null : dc;
        //
        if (checkModuleStatic) {
            assertSame(TestLoginModule_Contexted.accStatic.getDomainCombiner(),
                    match);
        }
        //
        assertSame(
                TestLoginModule_Contexted.item().accCtor.getDomainCombiner(),
                match);
        //
        assertSame(
                TestLoginModule_Contexted.item().accInit.getDomainCombiner(),
                match);
        //
        assertSame(TestLoginModule_Contexted.item().accLogin
                .getDomainCombiner(), match);
        //
        assertSame(TestLoginModule_Contexted.item().accCommit
                .getDomainCombiner(), match);
        //
        assertSame(TestLoginModule_Contexted.item().accLogout
                .getDomainCombiner(), match);
        //
        assertSame(TestLoginModule_Contexted.item().accAbort
                .getDomainCombiner(), match);

        // handle() must be called from the wrapper, so it must be 'dc' and not
        // 'match' here
        assertSame(TestCallbackHandler_Contexted.item().accHandle
                .getDomainCombiner(), dc);

        // ctor() for the Handler is called somewhere in
        // LoginContext.LoginContext().
        // It seems it's always get called from inside the powerful context
        // of LoginContext, so it's always 'null' here - they seem to use
        // doPriv(action) there
        assertSame(TestCallbackHandler_Contexted.item().accCtor
                .getDomainCombiner(), null);

        if (useInstalledConfig) {
            assertNotNull(TestLoginModule_Contexted.item().cbHandler);
            assertNotSame(TestCallbackHandler_Contexted.item(),
                    TestLoginModule_Contexted.item().cbHandler);

            if (checkCBHandlerStatic) {
                assertSame(TestCallbackHandler_Contexted.accStatic
                        .getDomainCombiner(), match);
            }
        } else {
            assertSame(TestCallbackHandler_Contexted.item(),
                    TestLoginModule_Contexted.item().cbHandler);
        }
    }
}

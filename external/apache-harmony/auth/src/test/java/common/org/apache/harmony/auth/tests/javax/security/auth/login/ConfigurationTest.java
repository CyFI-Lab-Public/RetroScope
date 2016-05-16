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

import java.io.File;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.AuthPermission;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.ConfigurationSpi;

import junit.framework.TestCase;

import org.apache.harmony.auth.tests.support.TestUtils;

import tests.support.Support_Exec;
import tests.support.resource.Support_Resources;

/**
 * Tests Configuration class
 */
public class ConfigurationTest extends TestCase {

    // system property to specify another login configuration file
    private static final String AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

    // security property to specify default configuration implementation
    private static final String LOGIN_CONFIG_PROVIDER = "login.configuration.provider";

    // testing config file
    private static String testConfFile = Support_Resources
            .getAbsoluteResourcePath("auth.conf");

	/**
	 * Ease the configuration class
	 */
	public static class ConfTestProvider extends Configuration {

		@Override
        public AppConfigurationEntry[] getAppConfigurationEntry(
				String applicationName) {
			return null;
		}

		@Override
        public void refresh() {
		}
	}

    // default implementation of Configuration class
    Configuration defaultConfig;

    // value of java.security.auth.login.config system property
    private String oldAuthConfig;

    @Override
    protected void setUp() {

        // point to some existing file to be read
        oldAuthConfig = System.setProperty(AUTH_LOGIN_CONFIG, "="
                + testConfFile);

        defaultConfig = Configuration.getConfiguration();
    }

    @Override
    protected void tearDown() {

        TestUtils.setSystemProperty(AUTH_LOGIN_CONFIG, oldAuthConfig);

        Configuration.setConfiguration(defaultConfig);
    }

	/**
	 * Tests loading of a default provider, both valid and invalid class
	 * references.
	 */
    public void test_loadDefaultProvider() {

        String oldProvider = Security.getProperty(LOGIN_CONFIG_PROVIDER);
        try {
            // test: loading custom test provider
            Security.setProperty(LOGIN_CONFIG_PROVIDER, ConfTestProvider.class
                    .getName());
            Configuration.setConfiguration(null); // reset default config
            assertEquals(ConfTestProvider.class, Configuration
                    .getConfiguration().getClass());

            // test: loading absent class as test provider
            Security.setProperty(LOGIN_CONFIG_PROVIDER, "ThereIsNoSuchClass");
            Configuration.setConfiguration(null); // reset default config
            try {
                Configuration.getConfiguration();
                fail("No SecurityException on failed provider");
            } catch (SecurityException ok) {
                assertTrue(ok.getCause() instanceof ClassNotFoundException);
            }

            // test: loading wrong class as test provider
            // a name of this unit test is used as config provider
            Security.setProperty(LOGIN_CONFIG_PROVIDER, this.getClass()
                    .getName());
            Configuration.setConfiguration(null);// reset default config
            try {
                Configuration.getConfiguration();
                fail("No expected ClassCastException");
            } catch (ClassCastException ok) {
            }

        } finally {
            //TODO reset security property if oldProvider==null
            Security.setProperty(LOGIN_CONFIG_PROVIDER,
                    (oldProvider == null) ? "" : oldProvider);
        }
    }

    /**
     * Tests reading config files by default provider
     */
    public void test_defaultProvider() throws Exception {

        // test: there are no config files to be read
        // Regression for HARMONY-1715

        // no login.config.url.N security properties should be set
        String javaSecurityFile = TestUtils
                .createJavaPropertiesFile(new Properties());

        // tmp user home to avoid presence of ${user.home}/.java.login.config
        String tmpUserHome = System.getProperty("java.io.tmpdir")
                + File.separatorChar + "tmpUserHomeForConfigurationTest";
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
        public static void main(String[] args) {

            //reset path to alternative configuration file
            TestUtils.setSystemProperty(AUTH_LOGIN_CONFIG, null);

            Configuration.setConfiguration(null); // reset default config
            try {
                Configuration.getConfiguration();
                fail("No expected SecurityException");
            } catch (SecurityException e) {
            }
        }
    }

    /**
     * Tests loading config files specified with the security properties
     * login.config.url.N
     *
     * TODO create test for loading a default config file:
     * ${user.home}/.java.login.config
     */
    public void test_defaultProvider_securityProperties() throws Exception {

        // create tmp config file
        File tmpConfFile = File.createTempFile("login", "config");
        tmpConfFile.deleteOnExit();

        String newConfFile = "LoginNew {\n org.apache.harmony.auth.module.LoginModule1 optional"
                + " debug=\"true\" test=false;\n};";

        FileOutputStream out = new FileOutputStream(tmpConfFile);
        out.write(newConfFile.getBytes());
        out.close();

        // set up security properties
        Properties props = new Properties();
        props.setProperty("login.config.url.1", "file:"
                + tmpConfFile.getCanonicalPath());
        props.setProperty("login.config.url.2", "file:"
                + new File(testConfFile).getCanonicalPath());
        String javaSecurityFile = TestUtils
                .createJavaPropertiesFile(props);

        // run test
        String[] arg = new String[] {
                "-Djava.security.properties=" + javaSecurityFile,
                SecurityPropertiesToBeRead.class.getName() };

        Support_Exec.execJava(arg, null, true);
    }

    /**
     * @tests javax.security.auth.login.Configuration#getInstance(java.lang.String, javax.security.auth.login.Configuration.Parameters, java.security.Provider)
     */
    public void test_getInstance_String_Parameters_Provider() throws NoSuchAlgorithmException{
        MockConfigurationParameters mcp = new MockConfigurationParameters();
        MockProvider mp = new MockProvider();
        Configuration cf = Configuration.getInstance("MockType", mcp, mp);
        assertEquals("Configuration parameters got should be equals to parameters provided",cf.getParameters(),mcp);
        assertEquals("Configuration provider got should be equals to provider provided",cf.getProvider(),mp);
        assertEquals("Configuration type got should be equals to type provided",cf.getType(),"MockType");
        try{
            Configuration.getInstance(null, mcp, mp);
            fail("Should throw NullPointerException here");
        }
        catch(NullPointerException e){
            //expect to catch NullPointerException here
        }

        try{
            Configuration.getInstance("MockType2", mcp, mp);
            fail("Should throw NoSuchAlgorithmException here");
        }
        catch(NoSuchAlgorithmException e){
            //expect to catch NoSuchAlgorithmException here
        }

        try{
            Configuration.getInstance("MockType2", mcp, (Provider)null);
            fail("Should throw IllegalArgumentException here");
        }
        catch(IllegalArgumentException e){
            //expect to catch NoSuchAlgorithmException here
        }

        cf = Configuration.getInstance("MockType", null, mp);
        assertEquals("MockType", cf.getType());
        assertNull(cf.getParameters());
    }

    /**
     * @tests javax.security.auth.login.Configuration#getInstance(java.lang.String, javax.security.auth.login.Configuration.Parameters, java.lang.String)
     */
    public void test_getInstance_String_Parameters_String() throws NoSuchAlgorithmException, NoSuchProviderException{
        MockConfigurationParameters mcp = new MockConfigurationParameters();
        MockProvider mp = new MockProvider();
        Security.addProvider(mp);
        Configuration cf = Configuration.getInstance("MockType", mcp, "MockProvider");

        assertEquals("Configuration parameters got should be equals to parameters provided",cf.getParameters(),mcp);
        assertEquals("Configuration provider got should be equals to provider provided",cf.getProvider(),mp);
        assertEquals("Configuration type got should be equals to type provided",cf.getType(),"MockType");
        try{
            Configuration.getInstance(null, mcp, "MockProvider");
            fail("Should throw NullPointerException here");
        }
        catch(NullPointerException e){
            //expect to catch NullPointerException here
        }

        try{
            Configuration.getInstance("MockType2", mcp, "MockProvider");
            fail("Should throw NoSuchAlgorithmException here");
        }
        catch(NoSuchAlgorithmException e){
            //expect to catch NoSuchAlgorithmException here
        }

        try{
            Configuration.getInstance("MockType2", mcp, (String)null);
            fail("Should throw IllegalArgumentException here");
        }
        catch(IllegalArgumentException e){
            //expect to catch NoSuchAlgorithmException here
        }

        try{
            Configuration.getInstance("MockType2", mcp, "");
            fail("Should throw IllegalArgumentException here");
        }
        catch(IllegalArgumentException e){
            //expect to catch NoSuchAlgorithmException here
        }

        try{
            Configuration.getInstance("MockType2", mcp, "not_exist_provider");
            fail("Should throw NoSuchProviderException here");
        }
        catch(NoSuchProviderException e){
            //expect to catch NoSuchAlgorithmException here
        }

        Security.removeProvider("MockProvider");
    }

    /**
     * @tests javax.security.auth.login.Configuration#getInstance(java.lang.String, javax.security.auth.login.Configuration.Parameters)
     */
    public void test_getInstance_String_Parameters() throws NoSuchAlgorithmException{
        MockConfigurationParameters mcp = new MockConfigurationParameters();
        MockProvider mp = new MockProvider();
        Security.addProvider(mp);
        Configuration cf = Configuration.getInstance("MockType", mcp);

        assertEquals("Configuration parameters got should be equals to parameters provided",cf.getParameters(),mcp);
        assertEquals("Configuration provider got should be equals to provider provided",cf.getProvider(),mp);
        assertEquals("Configuration type got should be equals to type provided",cf.getType(),"MockType");

        try{
            Configuration.getInstance(null, mcp);
            fail("Should throw NullPointerException here");
        }
        catch(NullPointerException e){
            //expect to catch NullPointerException here
        }

        try{
            Configuration.getInstance("MockType2", mcp);
            fail("Should throw NoSuchAlgorithmException here");
        }
        catch(NoSuchAlgorithmException e){
            //expect to catch NoSuchAlgorithmException here
        }

        Security.removeProvider("MockProvider");
    }

    /**
     * @throws NoSuchAlgorithmException
     * @tests javax.security.auth.login.Configuration#getProvider()
     */
    public void test_getProvider() throws NoSuchAlgorithmException{
        MockConfigurationParameters mcp = new MockConfigurationParameters();
        MockProvider mp = new MockProvider();
        Configuration cf = Configuration.getInstance("MockType", mcp, mp);
        assertEquals("Configuration provider got should be equals to provider provided",cf.getProvider(),mp);
    }

    /**
     * @throws NoSuchAlgorithmException
     * @tests javax.security.auth.login.Configuration#getProvider()
     */
    public void test_getParameter() throws NoSuchAlgorithmException{
        MockConfigurationParameters mcp = new MockConfigurationParameters();
        MockProvider mp = new MockProvider();
        Configuration cf = Configuration.getInstance("MockType", mcp, mp);
        assertEquals("Configuration parameters got should be equals to parameters provided",cf.getParameters(),mcp);
    }

    /**
     * @throws NoSuchAlgorithmException
     * @tests javax.security.auth.login.Configuration#getProvider()
     */
    public void test_getType() throws NoSuchAlgorithmException{
        MockConfigurationParameters mcp = new MockConfigurationParameters();
        MockProvider mp = new MockProvider();
        Configuration cf = Configuration.getInstance("MockType", mcp, mp);
        assertEquals("Configuration type got should be equals to type provided",cf.getType(),"MockType");
    }

    private static class MockConfigurationParameters implements Configuration.Parameters{

    }

    public static class MockConfiguration extends ConfigurationSpi {

        public MockConfiguration(Configuration.Parameters params) {

        }

        @Override
        protected AppConfigurationEntry[] engineGetAppConfigurationEntry(
                String name) {
            return null;
        }
    }

    private static class MockProvider extends Provider{
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public MockProvider(){
            super("MockProvider",1.0,"MokeProvider for configuration test");
            put("Configuration.MockType", MockConfiguration.class.getName());
        }
    }



    public static class SecurityPropertiesToBeRead {

        // the test is based on assumption that security properties
        // login.config.url.1 and login.config.url.1 are set
        public static void main(String[] args) {

            //reset path to alternative configuration file
            TestUtils.setSystemProperty(AUTH_LOGIN_CONFIG, null);

            Configuration.setConfiguration(null); // reset default config

            Configuration config = Configuration.getConfiguration();

            AppConfigurationEntry[] ents = config
                    .getAppConfigurationEntry("LoginNew");
            assertNotNull(ents);
            assertEquals("org.apache.harmony.auth.module.LoginModule1", ents[0]
                    .getLoginModuleName());
            Map<String, String> m = new HashMap<String, String>();
            m.put("debug", "true");
            m.put("test", "false");
            assertEquals(m, ents[0].getOptions());
            assertEquals("LoginModuleControlFlag: optional", ents[0]
                    .getControlFlag().toString());

            ents = config.getAppConfigurationEntry("Login1");
            assertNotNull(ents);
            for (AppConfigurationEntry element : ents) {
                assertEquals("com.intel.security.auth.module.LoginModule1",
                        element.getLoginModuleName());
                m.clear();
                m.put("debug1", "true");
                m.put("test1", "false");
                assertEquals(m, element.getOptions());
                assertEquals("LoginModuleControlFlag: required", element
                        .getControlFlag().toString());
            }
        }
    }
}

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

package org.apache.harmony.auth.tests.javax.security.auth.kerberos;

import java.io.File;
import java.io.FileOutputStream;

import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

import org.apache.harmony.auth.tests.support.TestUtils;

import tests.support.Support_Exec;

/**
 * Tests KerberosPrincipal class implementation.
 */
public class KerberosPrincipalTest extends TestCase {

    // system property for specifying the default realm
    private static final String KRB5_REALM_SYS_PROP = "java.security.krb5.realm";

    // system property for specifying the default kdc
    private static final String KRB5_KDC_SYS_PROP = "java.security.krb5.kdc";

    // system property for specifying the default config file
    private static final String KRB5_CONF_SYS_PROP = "java.security.krb5.conf";

    /**
     * @tests javax.security.auth.kerberos.KerberosPrincipal#KerberosPrincipal(
     *        java.lang.String)
     */
    public void test_Ctor1() {

        // null value is invalid
        try {
            new KerberosPrincipal(null);
            fail("No expected IllegalArgumentException for null");
        } catch (IllegalArgumentException e) {
        }

        // testing illegal kerberos principal names
        String[] illegalNames = new String[] { "bbb@a:a.com", // ':' char
                "bbb@a/a.com", // '/' char
                "bbb@a\0a.com",// null char
                "@/" // Regression for HARMONY-770
        };
        for (String illegalName : illegalNames) {
            try {
                new KerberosPrincipal(illegalName);

                fail("No expected IllegalArgumentException for: " + illegalName);
            } catch (IllegalArgumentException e) {
            }
        }

        // valid values
        KerberosPrincipal principal = new KerberosPrincipal("name@apache.org");

        assertEquals("name@apache.org", principal.getName());
        assertEquals("apache.org", principal.getRealm());
        assertEquals(KerberosPrincipal.KRB_NT_PRINCIPAL, principal
                .getNameType());
        
        // Regression for HARMONY-1182
        principal = new KerberosPrincipal("file:C://@apache.org");
        assertEquals("file:C:@apache.org", principal.getName());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosPrincipal#KerberosPrincipal(
     *        java.lang.String, int)
     */
    public void test_Ctor2() {

        // null value is invalid
        try {
            new KerberosPrincipal(null, KerberosPrincipal.KRB_NT_UNKNOWN);
            fail("No expected IllegalArgumentException for null");
        } catch (IllegalArgumentException e) {
        }

        // '-1' nameType value is invalid
        try {
            new KerberosPrincipal("name@apache.org", -1);
            fail("No expected IllegalArgumentException for -1 nameType value");
        } catch (IllegalArgumentException e) {
        }

        // '6' nameType value is invalid
        try {
            new KerberosPrincipal("name@apache.org", 6);
            fail("No expected IllegalArgumentException 6 nameType value");
        } catch (IllegalArgumentException e) {
        }

        // valid values
        KerberosPrincipal principal = new KerberosPrincipal("name@apache.org",
                KerberosPrincipal.KRB_NT_UNKNOWN);

        assertEquals("name@apache.org", principal.getName());
        assertEquals("apache.org", principal.getRealm());
        assertEquals(KerberosPrincipal.KRB_NT_UNKNOWN, principal.getNameType());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosPrincipal#KerberosPrincipal(
     *        java.lang.String)
     */
    public void test_Ctor_defaultRealm() throws Exception {

        // test: if the input name has no realm part then default realm is used.
        //
        // the test forks a separate VM for each case because   
        // default realm value is cashed

        // test: default realm is unset (has null value)
        Support_Exec.execJava(new String[] { DefaultRealm_NullValue.class
                .getName() }, null, true);

        // test: default realm name is specified via system property
        Support_Exec.execJava(new String[] { DefaultRealm_SystemProperty.class
                .getName() }, null, true);

        // test: default realm name is specified in config file
        Support_Exec.execJava(new String[] { DefaultRealm_ConfigFile.class
                .getName() }, null, true);
    }

    /**
     * Test: default realm is unset
     */
    public static class DefaultRealm_NullValue {

        // Regression for HARMONY-1090
        public static void main(String[] av) throws Exception {

            // clear system properties
            TestUtils.setSystemProperty(KRB5_REALM_SYS_PROP, null);
            TestUtils.setSystemProperty(KRB5_KDC_SYS_PROP, null);

            // point to empty config file
            File f = File.createTempFile("krb5", "conf");
            f.deleteOnExit();
            TestUtils.setSystemProperty(KRB5_CONF_SYS_PROP, f
                    .getCanonicalPath());

            // test: default realm value is 'null'
            KerberosPrincipal principal = new KerberosPrincipal("name");
            assertEquals("name", principal.getName());
            assertNull(principal.getRealm());
        }
    }

    /**
     * Tests: default realm name is specified via system property
     */
    public static class DefaultRealm_SystemProperty {
        public static void main(String[] av) {

            // case 1: unset 'kdc' and set 'realm'
            TestUtils.setSystemProperty(KRB5_REALM_SYS_PROP, "some_value");
            TestUtils.setSystemProperty(KRB5_KDC_SYS_PROP, null);
            try {
                new KerberosPrincipal("name");
                fail("No expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
            } finally {
                TestUtils.setSystemProperty(KRB5_REALM_SYS_PROP, null);
            }

            // case 2: set 'kdc' and unset 'realm' sys.props
            TestUtils.setSystemProperty(KRB5_KDC_SYS_PROP, "some_value");
            try {
                new KerberosPrincipal("name");
                fail("No expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
            } finally {
                TestUtils.setSystemProperty(KRB5_KDC_SYS_PROP, null);
            }
            
            String testRealm = "This_is_test_realm";

            System.setProperty(KRB5_REALM_SYS_PROP, testRealm);
            System.setProperty(KRB5_KDC_SYS_PROP, "some_value");

            // test
            KerberosPrincipal principal = new KerberosPrincipal("name");
            assertEquals("name@" + testRealm, principal.getName());
            assertEquals(testRealm, principal.getRealm());

            // test: default realm value is cashed 
            // change system property value 
            System.setProperty(KRB5_REALM_SYS_PROP,
                    "Another_test_realm");
            principal = new KerberosPrincipal("name");
            assertEquals("name@" + testRealm, principal.getName());
            assertEquals(testRealm, principal.getRealm());
        }
    }

    /**
     * Tests: default realm name is specified in config file
     */
    public static class DefaultRealm_ConfigFile {
        public static void main(String[] av) throws Exception {

            // clear system properties
            TestUtils.setSystemProperty(KRB5_REALM_SYS_PROP, null);
            TestUtils.setSystemProperty(KRB5_KDC_SYS_PROP, null);

            // point to config file
            File f = File.createTempFile("krb5", "conf");
            f.deleteOnExit();
            FileOutputStream out = new FileOutputStream(f);
            out.write("[libdefaults]\n".getBytes());
            out.write("         default_realm = MY.TEST_REALM".getBytes());
            out.close();
            
            TestUtils.setSystemProperty(KRB5_CONF_SYS_PROP, f
                    .getCanonicalPath());

            // test
            KerberosPrincipal principal = new KerberosPrincipal("name");
            assertEquals("name@MY.TEST_REALM", principal.getName());
            assertEquals("MY.TEST_REALM", principal.getRealm());
        }
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosPrincipal#hashCode()
     */
    public void test_hashCode() {
        KerberosPrincipal principal = new KerberosPrincipal("name@apache.org");

        assertEquals(principal.getName().hashCode(), principal.hashCode());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosPrincipal#toString()
     */
    public void test_toString() {

        String name = "name@apache.org";

        KerberosPrincipal principal = new KerberosPrincipal(name);

        assertEquals(name, principal.toString());
    }

    /**
     * @tests javax.security.auth.kerberos.KerberosPrincipal#equals(Object)
     */
    public void test_equals() {

        KerberosPrincipal p = new KerberosPrincipal("A@B");

        assertTrue(p.equals(new KerberosPrincipal("A@B")));
        assertFalse(p.equals(new KerberosPrincipal("A@B.org")));
        assertFalse(p.equals(new KerberosPrincipal("aaa@B")));
        assertFalse(p.equals(new KerberosPrincipal("A@B",
                KerberosPrincipal.KRB_NT_UID)));

        // Regression for HARMONY-744
        assertFalse(p.equals(null));
        assertFalse(p.equals(new Object()));
    }
}

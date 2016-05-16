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

package org.apache.harmony.luni.tests.java.util;

import static java.util.ResourceBundle.Control.FORMAT_CLASS;
import static java.util.ResourceBundle.Control.FORMAT_DEFAULT;
import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ResourceBundle.Control;

import tests.support.resource.Support_Resources;
import org.apache.harmony.luni.tests.support.B;

/**
 * Test cases for java.util.ResourceBundle.
 */
public class ResourceBundleTest extends junit.framework.TestCase {

    private static final String PROPERTIES_NAME = Support_Resources.RESOURCE_PACKAGE_NAME
            + ".hyts_resource"; //$NON-NLS-1$

    private static final String SUBFOLDER_PROPERTIES_NAME = "tests.resources.subfolder.tests.resources.hyts_resource"; //$NON-NLS-1$

    private static final String SUBFOLDER_NOROOT_NAME = "tests.resources.subfolder.tests.norootresources.hyts_resource"; //$NON-NLS-1$

    private static final String CLASS_NAME = "tests.support.Support_TestResource"; //$NON-NLS-1$

    private static final String PROPERTIES_NAME_COPY = "hyts_resource_copy"; //$NON-NLS-1$

    private static final ClassLoader URL_LOADER = getURLClassLoader();

    private static final ClassLoader SYSTEM_LOADER = ClassLoader
            .getSystemClassLoader();

    private static final Control CLASS_CONTROL = Control
            .getControl(FORMAT_CLASS);

    private static final Control PROPERTIES_CONTROL = Control
            .getControl(FORMAT_PROPERTIES);

    private static final Control DEFAULT_CONTROL = Control
            .getControl(FORMAT_DEFAULT);

    /**
     * @tests java.util.ResourceBundle#getString(java.lang.String)
     */
    public void test_getStringLjava_lang_String() {
        ResourceBundle bundle;
        String name = "tests.support.Support_TestResource";
        Locale.setDefault(new Locale("en", "US"));
        bundle = ResourceBundle.getBundle(name, new Locale("fr", "FR", "VAR"));
        assertEquals("Wrong value parent4", "frFRVARValue4", bundle
                .getString("parent4"));
        assertEquals("Wrong value parent3", "frFRValue3", bundle
                .getString("parent3"));
        assertEquals("Wrong value parent2", "frValue2", bundle
                .getString("parent2"));
        assertEquals("Wrong value parent1", "parentValue1", bundle
                .getString("parent1"));
        assertEquals("Wrong value child3", "frFRVARChildValue3", bundle
                .getString("child3"));
        assertEquals("Wrong value child2", "frFRVARChildValue2", bundle
                .getString("child2"));
        assertEquals("Wrong value child1", "frFRVARChildValue1", bundle
                .getString("child1"));

        // Regression test for Harmony-5698
        try {
            ResourceBundle.getBundle("Does not exist");
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            assertNotNull(e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("nls")
    private static final Locale LOCALE_FRFR = new Locale("fr", "FR");

    @SuppressWarnings("nls")
    private static final Locale LOCALE_FR = new Locale("fr");

    @SuppressWarnings("nls")
    private static final Locale LOCALE_EN = new Locale("en");

    @SuppressWarnings("nls")
    private static final Locale LOCALE_ENUSVAR = new Locale("en", "US", "VAR");

    private ResourceBundle bundle;

    private Locale defLocale = null;

    /**
     * @tests java.util.ResourceBundle#getBundle(java.lang.String,
     *        java.util.Locale)
     */
    @SuppressWarnings("nls")
    public void test_getBundleLjava_lang_StringLjava_util_Locale() {
        bundle = ResourceBundle.getBundle(CLASS_NAME, new Locale("fr", "FR",
                "VAR"));
        assertEquals("Wrong bundle fr_FR_VAR", "frFRVARValue4", bundle
                .getString("parent4"));
        bundle = ResourceBundle.getBundle(CLASS_NAME, new Locale("fr", "FR",
                "v1"));
        assertEquals("Wrong bundle fr_FR_v1", "frFRValue4", bundle
                .getString("parent4"));
        bundle = ResourceBundle.getBundle(CLASS_NAME, new Locale("fr", "US",
                "VAR"));
        assertEquals("Wrong bundle fr_US_var", "frValue4", bundle
                .getString("parent4"));
        bundle = ResourceBundle.getBundle(CLASS_NAME, new Locale("de", "FR",
                "VAR"));
        assertEquals("Wrong bundle de_FR_var", "enUSValue4", bundle
                .getString("parent4"));

        Locale.setDefault(new Locale("fr", "FR", "VAR"));
        bundle = ResourceBundle.getBundle(CLASS_NAME, new Locale("de", "FR",
                "v1"));
        assertEquals("Wrong bundle de_FR_var 2", "frFRVARValue4", bundle
                .getString("parent4"));

        Locale.setDefault(new Locale("de", "US"));
        bundle = ResourceBundle.getBundle(CLASS_NAME, new Locale("de", "FR",
                "var"));
        assertEquals("Wrong bundle de_FR_var 2", "parentValue4", bundle
                .getString("parent4"));

        // Regression test for Harmony-5698
        try {
            ResourceBundle.getBundle("Does not exist", Locale.getDefault());
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            assertNotNull(e.getLocalizedMessage());
        }
    }

    /**
     * @tests java.util.ResourceBundle#getBundle(java.lang.String,
     *        java.util.Locale, java.lang.ClassLoader)
     */
    @SuppressWarnings("nls")
    public void test_getBundleLjava_lang_StringLjava_util_LocaleLjava_lang_ClassLoader() {
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_EN);
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_EN,
                URL_LOADER);
        assertEquals("Wrong cached value", "en_resource", bundle
                .getString("property"));

        String classPath = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(classPath, File.pathSeparator);
        Vector urlVec = new Vector();
        String resPackage = Support_Resources.RESOURCE_PACKAGE;
        try {
            while (tok.hasMoreTokens()) {
                String path = (String) tok.nextToken();
                String url;
                if (new File(path).isDirectory())
                    url = "file:" + path + resPackage + "subfolder/";
                else
                    url = "jar:file:" + path + "!" + resPackage + "subfolder/";
                urlVec.addElement(new URL(url));
            }
        } catch (MalformedURLException e) {
        }
        URL[] urls = new URL[urlVec.size()];
        for (int i = 0; i < urlVec.size(); i++)
            urls[i] = (URL) urlVec.elementAt(i);
        URLClassLoader loader = new URLClassLoader(urls, null);

        String name = Support_Resources.RESOURCE_PACKAGE_NAME
                + ".hyts_resource";
        ResourceBundle bundle = ResourceBundle.getBundle(name, Locale
                .getDefault());
        assertEquals("Wrong value read", "parent", bundle.getString("property"));
        bundle = ResourceBundle.getBundle(name, Locale.getDefault(), loader);
        assertEquals("Wrong cached value", "en_US_resource", bundle
                .getString("property"));

        // Regression test for Harmony-3823
        B bb = new B();
        String s = bb.find("nonexistent");
        s = bb.find("name");
        assertEquals("Wrong property got", "Name", s);

        // Regression test for Harmony-5698
        try {
            ResourceBundle.getBundle("Does not exist", Locale.getDefault(),
                    loader);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            assertNotNull(e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("nls")
    static URLClassLoader getURLClassLoader() {
        String classPath = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(classPath, File.pathSeparator);
        Vector<URL> urlVec = new Vector<URL>();
        String resPackage = Support_Resources.RESOURCE_PACKAGE;
        try {
            while (tok.hasMoreTokens()) {
                String path = tok.nextToken();
                String url;
                if (new File(path).isDirectory()) {
                    url = "file:" + path + resPackage + "subfolder/";
                } else {
                    url = "jar:file:" + path + "!" + resPackage + "subfolder/";
                }
                urlVec.addElement(new URL(url));
            }
        } catch (MalformedURLException e) {
            // ignore
        }
        // add temp path if possible
        String tmpdir = System.getProperty("java.io.tmpdir");
        if (null != tmpdir) {
            try {
                urlVec.add(new URL("file:" + tmpdir));
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        URL[] urls = new URL[urlVec.size()];
        for (int i = 0; i < urlVec.size(); i++) {
            urls[i] = urlVec.elementAt(i);
        }
        URLClassLoader loader = new URLClassLoader(urls, null);
        return loader;
    }

    /**
     * @tests java.util.ResourceBundle#getObject(java.lang.String)
     */
    public void test_getObjectLjava_lang_String() {
        // Regression test for Harmony-5698
        try {
            ResourceBundle bundle;
            String name = "tests.support.Support_TestResource";
            Locale.setDefault(new Locale("en", "US"));
            bundle = ResourceBundle.getBundle(name, new Locale("fr", "FR",
                    "VAR"));
            bundle.getObject("not exist");
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            assertNotNull(e.getLocalizedMessage());
        }
    }

    public void test_getBundle_getClassName() {
        // Regression test for Harmony-1759
        Locale locale = Locale.GERMAN;
        String nonExistentBundle = "Non-ExistentBundle";
        try {
            ResourceBundle.getBundle(nonExistentBundle, locale, this.getClass()
                    .getClassLoader());
            fail("MissingResourceException expected!");
        } catch (MissingResourceException e) {
            assertEquals(nonExistentBundle + "_" + locale, e.getClassName());
        }

        try {
            ResourceBundle.getBundle(nonExistentBundle, locale);
            fail("MissingResourceException expected!");
        } catch (MissingResourceException e) {
            assertEquals(nonExistentBundle + "_" + locale, e.getClassName());
        }

        locale = Locale.getDefault();
        try {
            ResourceBundle.getBundle(nonExistentBundle);
            fail("MissingResourceException expected!");
        } catch (MissingResourceException e) {
            assertEquals(nonExistentBundle + "_" + locale, e.getClassName());
        }
    }

    /**
     * Can cause {@link IllegalArgumentException} in getBundle method
     */
    static class NullCandidateLocalesControl extends Control {
        /**
         * @see java.util.ResourceBundle.Control#getCandidateLocales(java.lang.String,
         *      java.util.Locale)
         */
        @Override
        public List<Locale> getCandidateLocales(@SuppressWarnings("unused")
        String baseName, @SuppressWarnings("unused")
        Locale locale) {
            return null;
        }
    }

    /*
     * the class and constructor must be public so ResourceBundle has the
     * possibility to instantiate
     */
    public static class GetBundleTest {
        public GetBundleTest() {
            // Try to load a resource with the same name as the class.
            // getBundle() should not try to instantiate the class since
            // its not a ResourceBundle. If a .properties file exists it
            // would be loaded.
            ResourceBundle
                    .getBundle("org.apache.harmony.luni.tests.java.util.ResourceBundleTest$GetBundleTest");
        }
    }

    /**
     * @tests java.util.ResourceBundle#getBundle(java.lang.String)
     */
    public void test_getBundleLjava_lang_String() {
        /* ResourceBundle.getBundle recursion loading class name */
        try {
            new GetBundleTest();
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
    }

	/**
     * The control with given formats.
     */
    static class GivenFormatsControl extends Control {
        List<String> formats;
        GivenFormatsControl(List<String> theFormats) {
            super();
            formats = theFormats;
        }

        /**
         * @see java.util.ResourceBundle.Control#getFormats(java.lang.String)
         */
        @SuppressWarnings("nls")
        @Override
        public List<String> getFormats(@SuppressWarnings("unused")
        String baseName) {
            return formats;
        }
    }

    /**
     * The control with reverse formats with default Control and no fallback
     * locale.
     */
    static class ReverseNoFallbackLocaleControl extends Control {
        /**
         * @see java.util.ResourceBundle.Control#getFormats(java.lang.String)
         */
        @SuppressWarnings("nls")
        @Override
        public List<String> getFormats(@SuppressWarnings("unused")
        String baseName) {
            return Arrays
                    .asList(new String[] { "java.properties", "java.class" });
        }

        /**
         * @see java.util.ResourceBundle.Control#getFallbackLocale(java.lang.String,
         *      java.util.Locale)
         */
        @Override
        public Locale getFallbackLocale(@SuppressWarnings("unused")
        String baseName, @SuppressWarnings("unused")
        Locale locale) {
            return null;
        }
    }

    /**
     * @tests {@link java.util.ResourceBundle#getBundle(String, java.util.ResourceBundle.Control)}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_getBundle_LStringLControl() {
        Locale.setDefault(LOCALE_FRFR);
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                DEFAULT_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle("wrongName", DEFAULT_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(null, DEFAULT_CONTROL);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, (Control) null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        // illegal control causes IllegalArgumentException
        Control otherControl = new NullCandidateLocalesControl();
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, otherControl);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.util.ResourceBundle#getBundle(String, Locale, java.util.ResourceBundle.Control)}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_getBundle_LStringLLocaleLControl() {
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, DEFAULT_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle("wrongName", LOCALE_FRFR, DEFAULT_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(null, LOCALE_FRFR, DEFAULT_CONTROL);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, null,
                    DEFAULT_CONTROL);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    (Control) null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        // illegal control causes IllegalArgumentException
        Control otherControl = new NullCandidateLocalesControl();
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    otherControl);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.util.ResourceBundle#getBundle(String, Locale, ClassLoader, java.util.ResourceBundle.Control)}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_getBundle_LStringLLocaleLClassLoaderLControl() {
        getBundleWithControlTester();
    }

    @SuppressWarnings("nls")
    private void getBundleWithControlTester() {
        // 1. cache feature: is tested in other methods
        // 2. Formats/Locale
        // the "reverse" control that take java.properties first
        Control reverseControl = new GivenFormatsControl(Arrays
                .asList(new String[] { "java.properties", "java.class" }));
        // locale that has both class file and properties file support.
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, CLASS_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(4, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("fr_FR_resource", bundle.getString("property"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));
        assertFalse(bundle.containsKey("subParent1"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, reverseControl);
        assertEquals(4, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("fr_FR_resource", bundle.getString("property"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));
        assertFalse(bundle.containsKey("subParent1"));

        // locale that has only properties file support.
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_EN,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(5, bundle.keySet().size());
        assertEquals("en_resource", bundle.getString("property"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("subParentValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("subChild1"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_EN,
                SYSTEM_LOADER, CLASS_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("subParentValue2", bundle.getString("subParent2"));
        assertEquals("enUSValue3", bundle.getString("subParent3"));
        assertEquals("enUSValue4", bundle.getString("subParent4"));
        assertEquals("enUSChildValue1", bundle.getString("subChild1"));
        assertEquals("enUSChildValue2", bundle.getString("subChild2"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_EN,
                SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(2, bundle.keySet().size());
        assertEquals("en_resource", bundle.getString("property"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_EN,
                SYSTEM_LOADER, reverseControl);
        assertEquals(2, bundle.keySet().size());
        assertEquals("en_resource", bundle.getString("property"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));

        // locale that has only class file support.
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_ENUSVAR, SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(8, bundle.keySet().size());
        assertEquals("enUSVARChildValue3", bundle.getString("subChild3"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("en_resource", bundle.getString("property"));
        assertEquals("enUSVARValue4", bundle.getString("subParent4"));
        assertEquals("enUSValue3", bundle.getString("subParent3"));
        assertEquals("subParentValue2", bundle.getString("subParent2"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_ENUSVAR, SYSTEM_LOADER, CLASS_CONTROL);
        assertEquals(7, bundle.keySet().size());
        assertEquals("enUSVARChildValue3", bundle.getString("subChild3"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("enUSVARValue4", bundle.getString("subParent4"));
        assertEquals("enUSValue3", bundle.getString("subParent3"));
        assertEquals("subParentValue2", bundle.getString("subParent2"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_ENUSVAR, SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(2, bundle.keySet().size());
        assertEquals("en_US_resource", bundle.getString("property"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_ENUSVAR, SYSTEM_LOADER, reverseControl);
        assertEquals(6, bundle.keySet().size());
        assertEquals("en_US_resource", bundle.getString("property"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("enUSVARChildValue1", bundle.getString("subChild1"));
        assertEquals("enUSVARChildValue2", bundle.getString("subChild2"));
        assertEquals("enUSVARChildValue3", bundle.getString("subChild3"));
        assertEquals("enUSVARValue4", bundle.getString("subParent4"));

        // root locale
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ROOT, SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(4, bundle.keySet().size());
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("subParentValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));
        assertFalse(bundle.containsKey("subChild1"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ROOT, SYSTEM_LOADER, CLASS_CONTROL);
        assertEquals(4, bundle.keySet().size());
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("subParentValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));
        assertFalse(bundle.containsKey("subChild1"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ROOT, SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(2, bundle.keySet().size());
        assertEquals("resource", bundle.getString("property"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ROOT, SYSTEM_LOADER, reverseControl);
        assertEquals(2, bundle.keySet().size());
        assertEquals("resource", bundle.getString("property"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));

        // 3.use the FallbackLocale
        Locale.setDefault(LOCALE_FRFR);
        // no resource for Locale.ITALY
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue1", bundle.getString("subChild1"));
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, CLASS_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue1", bundle.getString("subChild1"));
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(4, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("fr_FR_resource", bundle.getString("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, reverseControl);
        assertEquals(4, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("fr_FR_resource", bundle.getString("property"));

        // with NoFallbackControl
        Control noFallbackControl = Control
                .getNoFallbackControl(FORMAT_DEFAULT);
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, noFallbackControl);
        assertEquals(4, bundle.keySet().size());
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("subParentValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        noFallbackControl = Control.getNoFallbackControl(FORMAT_CLASS);
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, noFallbackControl);
        assertEquals(4, bundle.keySet().size());
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("subParentValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        noFallbackControl = Control.getNoFallbackControl(FORMAT_PROPERTIES);
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, noFallbackControl);
        assertEquals(2, bundle.keySet().size());
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("resource", bundle.getString("property"));

        noFallbackControl = new ReverseNoFallbackLocaleControl();
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, noFallbackControl);
        assertEquals(2, bundle.keySet().size());
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("resource", bundle.getString("property"));

        // locale is equal to the default locale
        Locale.setDefault(Locale.ITALY);
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                Locale.ITALY, SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(4, bundle.keySet().size());
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("subParentValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));
        assertFalse(bundle.containsKey("subChild1"));

        // 4.test the resources without a root resource
        // locale that has both class file and properties file support.
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, LOCALE_FRFR,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(5, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));
        assertFalse(bundle.containsKey("subParent1"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, LOCALE_FRFR,
                SYSTEM_LOADER, CLASS_CONTROL);
        assertEquals(5, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));
        assertFalse(bundle.containsKey("subParent1"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, LOCALE_FRFR,
                SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(3, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("fr_FR_resource", bundle.getString("property"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, LOCALE_FRFR,
                SYSTEM_LOADER, reverseControl);
        assertEquals(3, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("fr_FR_resource", bundle.getString("property"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));

        // root locale
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ROOT,
                    SYSTEM_LOADER, DEFAULT_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ROOT,
                    SYSTEM_LOADER, CLASS_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ROOT,
                    SYSTEM_LOADER, PROPERTIES_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ROOT,
                    SYSTEM_LOADER, reverseControl);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        // fall back locale
        Locale.setDefault(LOCALE_FRFR);
        // no resource for Locale.ITALY
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(5, bundle.keySet().size());
        assertEquals("frFRChildValue1", bundle.getString("subChild1"));
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("frValue2", bundle.getString("subParent2"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertEquals("frFRValue3", bundle.getString("subParent3"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                SYSTEM_LOADER, CLASS_CONTROL);
        assertEquals(5, bundle.keySet().size());
        assertEquals("frFRChildValue1", bundle.getString("subChild1"));
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("frValue2", bundle.getString("subParent2"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertEquals("frFRValue3", bundle.getString("subParent3"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(3, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));
        assertEquals("fr_FR_resource", bundle.getString("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                SYSTEM_LOADER, reverseControl);
        assertEquals(3, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));
        assertEquals("fr_FR_resource", bundle.getString("property"));

        // with NoFallbackControl
        noFallbackControl = Control.getNoFallbackControl(FORMAT_DEFAULT);
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                    SYSTEM_LOADER, noFallbackControl);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        noFallbackControl = Control.getNoFallbackControl(FORMAT_CLASS);
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                    SYSTEM_LOADER, noFallbackControl);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        noFallbackControl = Control.getNoFallbackControl(FORMAT_PROPERTIES);
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                    SYSTEM_LOADER, noFallbackControl);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        noFallbackControl = new ReverseNoFallbackLocaleControl();
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                    SYSTEM_LOADER, noFallbackControl);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        // locale is equal to the default locale
        Locale.setDefault(Locale.ITALY);
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_NOROOT_NAME, Locale.ITALY,
                    SYSTEM_LOADER, noFallbackControl);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }

        // 5.simple exceptions
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle("wrongName", LOCALE_FRFR, SYSTEM_LOADER,
                    DEFAULT_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    URL_LOADER, DEFAULT_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(null, LOCALE_FRFR, SYSTEM_LOADER,
                    DEFAULT_CONTROL);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, null,
                    SYSTEM_LOADER, DEFAULT_CONTROL);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    null, DEFAULT_CONTROL);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    SYSTEM_LOADER, null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        // 6. exceptions caused by control
        // illegal control causes IllegalArgumentException
        Control otherControl = new NullCandidateLocalesControl();
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    SYSTEM_LOADER, otherControl);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // illegal control with illegal format
        otherControl = new GivenFormatsControl(Arrays
                .asList(new String[] { "java.test" }));
        illegalFormatControlTester(otherControl);

        // illegal control with other format
        otherControl = new GivenFormatsControl(Arrays
                .asList(new String[] { "other.format" }));
        illegalFormatControlTester(otherControl);
    }

    @SuppressWarnings("nls")
    private void illegalFormatControlTester(Control otherControl) {
        ResourceBundle.clearCache();
        ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        // cache can ignore the illegal control
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, otherControl);
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        ResourceBundle.clearCache();
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    SYSTEM_LOADER, otherControl);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
        // cache can also ignore the legal control
        try {
            ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FRFR,
                    SYSTEM_LOADER, DEFAULT_CONTROL);
            fail("Should throw MissingResourceException");
        } catch (MissingResourceException e) {
            // expected
        }
        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
    }

    /**
     * @tests {@link java.util.ResourceBundle#clearCache()}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_clearCache() {
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, DEFAULT_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        // the cache used
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache(URL_LOADER);
        // system loader's cache is still there
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(6, bundle.keySet().size());
        assertEquals("frFRChildValue2", bundle.getString("subChild2"));
        assertEquals("subParentValue1", bundle.getString("subParent1"));
        assertEquals("frFRValue4", bundle.getString("subParent4"));
        assertFalse(bundle.containsKey("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME,
                LOCALE_FRFR, SYSTEM_LOADER, PROPERTIES_CONTROL);
        assertEquals(4, bundle.keySet().size());
        assertEquals("valueInFRFR", bundle.getString("propertyInFRFR"));
        assertEquals("valueInURLParent", bundle
                .getString("propertyInURLParent"));
        assertEquals("fr_FR_resource", bundle.getString("property"));
        assertEquals("valueInFR", bundle.getString("propertyInFR"));
        assertFalse(bundle.containsKey("subParent1"));
    }

    /**
     * @throws IOException
     * @tests {@link java.util.ResourceBundle#clearCache(ClassLoader)}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_clearCacheLjava_lang_ClassLoader() throws IOException {
        // copy the file to test
        URL srcFile = URL_LOADER.getResource(DEFAULT_CONTROL.toResourceName(
                DEFAULT_CONTROL.toBundleName(PROPERTIES_NAME, LOCALE_FRFR),
                "properties"));
        File copyFile = ControlTest.copyFile(srcFile);
        if (null != URL_LOADER
                .getResourceAsStream("hyts_resource_copy_fr_FR.properties")) {
            // load first time
            bundle = ResourceBundle.getBundle(PROPERTIES_NAME_COPY,
                    LOCALE_FRFR, URL_LOADER, DEFAULT_CONTROL);
            assertEquals("fr_FR_resource", bundle.getString("property"));
            ControlTest.changeProperties(copyFile);
            bundle = ResourceBundle.getBundle(PROPERTIES_NAME_COPY,
                    LOCALE_FRFR, URL_LOADER, DEFAULT_CONTROL);
            // value from cache, unchanged
            assertEquals("fr_FR_resource", bundle.getString("property"));
            ResourceBundle.clearCache();
            bundle = ResourceBundle.getBundle(PROPERTIES_NAME_COPY,
                    LOCALE_FRFR, URL_LOADER, DEFAULT_CONTROL);
            // value from cache, unchanged
            assertEquals("fr_FR_resource", bundle.getString("property"));
            ResourceBundle.clearCache(URL_LOADER);
            bundle = ResourceBundle.getBundle(PROPERTIES_NAME_COPY,
                    LOCALE_FRFR, URL_LOADER, DEFAULT_CONTROL);
            // value changed
            assertEquals("changedValue", bundle.getString("property"));
        } else {
            System.err
                    .println("Can not find the test file, some code of this test 'test_clearCacheLjava_lang_ClassLoader' did not run.");
        }

        try {
            ResourceBundle.clearCache(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.util.ResourceBundle#containsKey(String)}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_containsKeyLjava_lang_String() {
        ResourceBundle.clearCache(URL_LOADER);
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_FRFR,
                URL_LOADER, DEFAULT_CONTROL);
        assertTrue(bundle.containsKey("property"));
        assertTrue(bundle.containsKey("propertyInFRFR"));
        assertTrue(bundle.containsKey("propertyInURLParent"));
        assertFalse(bundle.containsKey("propertyInSystemParent"));
        assertTrue(bundle.containsKey("propertyInFR"));

        ResourceBundle.clearCache(URL_LOADER);
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_FR,
                URL_LOADER, DEFAULT_CONTROL);
        assertTrue(bundle.containsKey("property"));
        assertFalse(bundle.containsKey("propertyInFRFR"));
        assertTrue(bundle.containsKey("propertyInFR"));
        assertTrue(bundle.containsKey("propertyInURLParent"));
        assertFalse(bundle.containsKey("propertyInSystemParent"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_FR,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        assertTrue(bundle.containsKey("property"));
        assertFalse(bundle.containsKey("propertyInURLParent"));
        assertTrue(bundle.containsKey("propertyInSystemParent"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(CLASS_NAME, LOCALE_FR, SYSTEM_LOADER,
                DEFAULT_CONTROL);
        assertTrue(bundle.containsKey("parent2"));
        assertTrue(bundle.containsKey("parent1"));
        assertTrue(bundle.containsKey("child1"));
        assertFalse(bundle.containsKey("child2"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FR,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        assertTrue(bundle.containsKey("subParent1"));
        assertTrue(bundle.containsKey("subParent2"));
        assertTrue(bundle.containsKey("subParent3"));
        assertTrue(bundle.containsKey("subParent4"));
        assertTrue(bundle.containsKey("subChild1"));
        assertFalse(bundle.containsKey("subChild2"));
        assertFalse(bundle.containsKey("subChild3"));

        assertNotNull(bundle);
        try {
            bundle.containsKey(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests {@link java.util.ResourceBundle#keySet()}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_keySet() {
        ResourceBundle.clearCache(URL_LOADER);
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_FRFR,
                URL_LOADER, DEFAULT_CONTROL);
        Set<String> keys = bundle.keySet();
        assertEquals(4, keys.size());
        assertNotSame(keys, bundle.keySet());
        keys.add("wrongKey");
        keys = bundle.keySet();
        assertEquals(4, keys.size());
        assertTrue(keys.getClass() == HashSet.class);
        assertTrue(keys.contains("propertyInFRFR"));
        assertTrue(keys.contains("propertyInURLParent"));
        assertTrue(keys.contains("propertyInFR"));
        assertTrue(keys.contains("property"));

        ResourceBundle.clearCache(URL_LOADER);
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_FR,
                URL_LOADER, DEFAULT_CONTROL);
        keys = bundle.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("propertyInURLParent"));
        assertTrue(keys.contains("propertyInFR"));
        assertTrue(keys.contains("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(PROPERTIES_NAME, LOCALE_FR,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        keys = bundle.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("propertyInSystemParent"));
        assertTrue(keys.contains("property"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(CLASS_NAME, LOCALE_FR, SYSTEM_LOADER,
                DEFAULT_CONTROL);
        keys = bundle.keySet();
        assertEquals(5, keys.size());
        assertTrue(keys.contains("parent1"));
        assertTrue(keys.contains("parent2"));
        assertTrue(keys.contains("parent3"));
        assertTrue(keys.contains("parent4"));
        assertTrue(keys.contains("child1"));

        ResourceBundle.clearCache();
        bundle = ResourceBundle.getBundle(SUBFOLDER_PROPERTIES_NAME, LOCALE_FR,
                SYSTEM_LOADER, DEFAULT_CONTROL);
        keys = bundle.keySet();
        assertEquals(5, keys.size());
        assertTrue(keys.contains("subParent1"));
        assertTrue(keys.contains("subParent2"));
        assertTrue(keys.contains("subParent3"));
        assertTrue(keys.contains("subParent4"));
        assertTrue(keys.contains("subChild1"));
    }

    /**
     * @tests {@link java.util.ResourceBundle#handleKeySet()}
     * @since 1.6
     */
    @SuppressWarnings("nls")
    public void test_handleKeySet() {
        class SubBundle extends ResourceBundle {
            public SubBundle() {
                super();
            }

            @Override
            public Set<String> handleKeySet() {
                return super.handleKeySet();
            }

            @Override
            public Enumeration<String> getKeys() {
                Vector<String> keys = new Vector<String>();
                keys.add("key1InThis");
                keys.add("key2InThis");
                keys.add("key1InParent");
                keys.add("key2InParent");
                return keys.elements();
            }

            @Override
            protected Object handleGetObject(String key) {
                if (key.equals("key1InParent") || key.equals("key2InParent")) {
                    return null;
                }
                return new Object();
            }
        }
        SubBundle subBundle = new SubBundle();
        Set<String> keys = subBundle.handleKeySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1InThis"));
        assertTrue(keys.contains("key2InThis"));
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @SuppressWarnings("nls")
    @Override
    protected void setUp() {
        defLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() {
        Locale.setDefault(defLocale);
    }
}

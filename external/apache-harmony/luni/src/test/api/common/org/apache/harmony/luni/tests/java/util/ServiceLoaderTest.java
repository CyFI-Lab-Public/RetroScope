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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import junit.framework.TestCase;
import tests.resources.ServiceLoader.AbstractService;
import tests.resources.ServiceLoader.Service;
import tests.resources.ServiceLoader.ServiceDuplicateIn2File;
import tests.resources.ServiceLoader.ServiceFinalClass;
import tests.resources.ServiceLoader.ServiceForAllCommentTest;
import tests.resources.ServiceLoader.ServiceForEmptyTest;
import tests.resources.ServiceLoader.ServiceForIllegalNameTest;
import tests.resources.ServiceLoader.ServiceForWrongNameTest;
import tests.resources.ServiceLoader.ServiceIn2File;
import tests.resources.ServiceLoader.ServiceIn2FileWithEmptyConfig;
import tests.resources.ServiceLoader.ServiceMoreThanOne;
import tests.resources.ServiceLoader.ServiceWithDuplicateSons;
import tests.support.resource.Support_Resources;

/**
 * Test cases for java.util.ServiceLoader
 */
public class ServiceLoaderTest extends TestCase {

    private static URL jarFile = null;

    /**
     * @throws MalformedURLException
     * @tests {@link java.util.ServiceLoader#reload()}.
     */
    @SuppressWarnings("nls")
    public void test_reload() throws MalformedURLException {
        class SubURLClassLoader extends URLClassLoader {
            /**
             * @param urls
             */
            public SubURLClassLoader(URL[] urls) {
                super(urls);
            }

            @Override
            public void addURL(URL url) {
                super.addURL(url);
            }
        }
        SubURLClassLoader ucl = new SubURLClassLoader(new URL[] { new URL(
                "file:/no/such/file") });
        ServiceLoader<Service> serviceLoader = ServiceLoader.load(
                Service.class, ucl);
        Iterator<Service> itr = serviceLoader.iterator();
        assertFalse(itr.hasNext());
        // change the ucl to install a jar file
        ucl.addURL(jarFile);
        // before reload, the Iterator is unchanged
        itr = serviceLoader.iterator();
        assertNotSame(itr, serviceLoader.iterator());
        assertFalse(itr.hasNext());
        // after reload, the Iterator update
        serviceLoader.reload();
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfService", itr.next().myNameIs());
        assertFalse(itr.hasNext());
    }

    /**
     * @tests {@link java.util.ServiceLoader#iterator()}.
     */
    @SuppressWarnings( { "nls", "unchecked" })
    public void test_iterator() {
        URLClassLoader ucl = new URLClassLoader(new URL[] { jarFile });
        Iterator itr = ServiceLoader.load(Service.class, ucl).iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfService", ((Service) itr.next())
                .myNameIs());
        assertFalse(itr.hasNext());
        try {
            itr.remove();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        itr = ServiceLoader.load(ServiceForWrongNameTest.class, ucl).iterator();
        assertTrue(itr.hasNext());
        try {
            itr.next();
            fail("Should throw ServiceConfigurationError");
        } catch (ServiceConfigurationError e) {
            // expected
        }
        try {
            itr.remove();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // null test
        itr = ServiceLoader.load(null).iterator();
        nullIteratorTester(itr);

        itr = ServiceLoader.load(null, null).iterator();
        nullIteratorTester(itr);

        itr = ServiceLoader.load(null, ClassLoader.getSystemClassLoader())
                .iterator();
        nullIteratorTester(itr);

        itr = ServiceLoader.load(Service.class, null).iterator();
        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
        try {
            itr.remove();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @SuppressWarnings( { "nls", "unchecked" })
    private void nullIteratorTester(Iterator itr) {
        assertNotNull(itr);
        try {
            itr.hasNext();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            itr.next();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            itr.remove();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /**
     * @throws MalformedURLException
     * @tests {@link java.util.ServiceLoader#load(java.lang.Class, java.lang.ClassLoader)}.
     */
    @SuppressWarnings( { "nls", "unchecked" })
    public void test_loadLjava_lang_ClassLjava_lang_ClassLoader()
            throws MalformedURLException {
        URLClassLoader ucl = new URLClassLoader(new URL[] { jarFile });
        // normal config file
        ServiceLoader serviceLoader = ServiceLoader.load(Service.class, ucl);
        Iterator itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfService", ((Service) itr.next())
                .myNameIs());
        assertFalse(itr.hasNext());

        // class that can not cast correctly
        serviceLoader = ServiceLoader.load(ServiceFinalClass.class, ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        try {
            itr.next();
            fail("Should throw ServiceConfigurationError");
        } catch (ServiceConfigurationError e) {
            // expected
        }

        // abstract class with comment in config file
        serviceLoader = ServiceLoader.load(AbstractService.class, ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfAbstractService", ((AbstractService) itr
                .next()).myNameIs());
        assertFalse(itr.hasNext());

        // one service with two implementation class
        serviceLoader = ServiceLoader.load(ServiceMoreThanOne.class, ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        String name = ((ServiceMoreThanOne) itr.next()).myNameIs();
        if ("ImplementationOfServiceMoreThanOne1".equals(name)) {
            assertEquals("ImplementationOfServiceMoreThanOne2",
                    ((ServiceMoreThanOne) itr.next()).myNameIs());
        } else if ("ImplementationOfServiceMoreThanOne2".equals(name)) {
            assertEquals("ImplementationOfServiceMoreThanOne1",
                    ((ServiceMoreThanOne) itr.next()).myNameIs());
        } else {
            fail("Should load ImplementationOfServiceMoreThanOne1 or ImplementationOfServiceMoreThanOne2");
        }
        assertFalse(itr.hasNext());

        // config file only contains comments
        serviceLoader = ServiceLoader.load(ServiceForAllCommentTest.class, ucl);
        itr = serviceLoader.iterator();
        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        // empty config file
        serviceLoader = ServiceLoader.load(ServiceForEmptyTest.class, ucl);
        itr = serviceLoader.iterator();
        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        // config file with illegal char
        serviceLoader = ServiceLoader
                .load(ServiceForIllegalNameTest.class, ucl);
        itr = serviceLoader.iterator();
        try {
            itr.hasNext();
            fail("Should throw ServiceConfigurationError");
        } catch (ServiceConfigurationError e) {
            // expected
        }

        // config file with legal string, but the class does not exist
        serviceLoader = ServiceLoader.load(ServiceForWrongNameTest.class, ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        try {
            itr.next();
            fail("Should throw ServiceConfigurationError");
        } catch (ServiceConfigurationError e) {
            // expected
        }

        // config file for an internal class
        serviceLoader = ServiceLoader.load(
                AbstractService.InternalService.class, ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfAbstractServiceInternalService",
                ((AbstractService.InternalService) itr.next())
                        .myInternalNameIs());
        assertFalse(itr.hasNext());

        // config files in the 2 jar files
        serviceLoader = ServiceLoader.load(ServiceIn2File.class, ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfServiceIn2File1", ((ServiceIn2File) itr
                .next()).myNameIs());
        assertFalse(itr.hasNext());
        // add the second file
        URL jarFile2 = prepairJar("hyts_services2.jar");
        URLClassLoader ucl2 = new URLClassLoader(
                new URL[] { jarFile, jarFile2 });
        serviceLoader = ServiceLoader.load(ServiceIn2File.class, ucl2);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        name = ((ServiceIn2File) itr.next()).myNameIs();
        if ("ImplementationOfServiceIn2File1".equals(name)) {
            assertEquals("ImplementationOfServiceIn2File2",
                    ((ServiceIn2File) itr.next()).myNameIs());
        } else if ("ImplementationOfServiceIn2File2".equals(name)) {
            assertEquals("ImplementationOfServiceIn2File1",
                    ((ServiceIn2File) itr.next()).myNameIs());
        } else {
            fail("Should load ImplementationOfServiceIn2File1 or ImplementationOfServiceIn2File2");
        }
        assertFalse(itr.hasNext());

        // same config files in 2 jar files
        serviceLoader = ServiceLoader.load(ServiceDuplicateIn2File.class, ucl2);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfServiceDuplicateIn2File_1",
                ((ServiceDuplicateIn2File) itr.next()).myNameIs());
        assertFalse(itr.hasNext());
        ucl2 = new URLClassLoader(new URL[] { jarFile2, jarFile });
        serviceLoader = ServiceLoader.load(ServiceDuplicateIn2File.class, ucl2);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfServiceDuplicateIn2File_2",
                ((ServiceDuplicateIn2File) itr.next()).myNameIs());
        assertFalse(itr.hasNext());

        // one config file in one jar, another empty config in another jar.
        serviceLoader = ServiceLoader.load(ServiceIn2FileWithEmptyConfig.class,
                ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfServiceIn2FileWithEmptyConfig",
                ((ServiceIn2FileWithEmptyConfig) itr.next()).myNameIs());
        assertFalse(itr.hasNext());
        ucl2 = new URLClassLoader(new URL[] { jarFile, jarFile2 });
        serviceLoader = ServiceLoader.load(ServiceIn2FileWithEmptyConfig.class,
                ucl2);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfServiceIn2FileWithEmptyConfig",
                ((ServiceIn2FileWithEmptyConfig) itr.next()).myNameIs());
        assertFalse(itr.hasNext());

        // config file with duplicate items
        serviceLoader = ServiceLoader.load(ServiceWithDuplicateSons.class, ucl);
        itr = serviceLoader.iterator();
        assertTrue(itr.hasNext());
        assertEquals("ImplementationOfServiceWithDuplicateSons",
                ((ServiceWithDuplicateSons) itr.next()).myNameIs());
        assertFalse(itr.hasNext());

        // can not load by system classloader
        serviceLoader = ServiceLoader.load(Service.class, ClassLoader
                .getSystemClassLoader());
        assertFalse(serviceLoader.iterator().hasNext());

        // can not load by Thread.currentThread().getContextClassLoader()
        serviceLoader = ServiceLoader.load(Service.class, Thread
                .currentThread().getContextClassLoader());
        assertFalse(serviceLoader.iterator().hasNext());

        serviceLoader = ServiceLoader.load(Service.class, Service.class
                .getClassLoader());
        assertFalse(serviceLoader.iterator().hasNext());

        // String is a final class, no sub-class for it
        serviceLoader = ServiceLoader.load(String.class, ucl);
        assertFalse(serviceLoader.iterator().hasNext());
    }

    /**
     * @tests {@link java.util.ServiceLoader#load(java.lang.Class)}.
     */
    @SuppressWarnings( { "nls", "unchecked" })
    public void test_loadLjava_lang_Class() {
        ServiceLoader serviceLoader = ServiceLoader.load(Service.class);
        assertFalse(serviceLoader.iterator().hasNext());
        // String is a final class, no sub-class for it
        serviceLoader = ServiceLoader.load(String.class);
        assertFalse(serviceLoader.iterator().hasNext());
    }

    /**
     * @param fileName
     * @return the URL of the jar file
     * @throws MalformedURLException
     */
    @SuppressWarnings("nls")
    private static URL prepairJar(String fileName) throws MalformedURLException {
        File resources = Support_Resources.createTempFolder();
        String resPath = resources.toString();
        if (resPath.charAt(0) == '/' || resPath.charAt(0) == '\\') {
            resPath = resPath.substring(1);
        }
        Support_Resources.copyFile(resources, "ServiceLoader", fileName);
        URL resourceURL = new URL("file:/" + resPath + "/ServiceLoader/"
                + fileName);
        return resourceURL;
    }

    /**
     * @tests {@link java.util.ServiceLoader#loadInstalled(java.lang.Class)}.
     */
    public void test_loadInstalledLjava_lang_Class() {
        ServiceLoader<Service> serviceLoader = ServiceLoader
                .loadInstalled(Service.class);
        assertFalse(serviceLoader.iterator().hasNext());

        serviceLoader = ServiceLoader.loadInstalled(null);
        Iterator<Service> itr = serviceLoader.iterator();
        nullIteratorTester(itr);
    }

    /**
     * @tests {@link java.util.ServiceLoader#toString()}.
     */
    @SuppressWarnings( { "unchecked", "nls" })
    public void test_toString() {
        URLClassLoader ucl = new URLClassLoader(new URL[] { jarFile });
        ServiceLoader serviceLoader = ServiceLoader.load(Service.class, ucl);
        assertTrue(serviceLoader.toString().length() > 0);

        serviceLoader = ServiceLoader.load(String.class, ucl);
        assertTrue(serviceLoader.toString().length() > 0);

        serviceLoader = ServiceLoader.load(Service.class);
        assertTrue(serviceLoader.toString().length() > 0);

        serviceLoader = ServiceLoader.load(String.class);
        assertTrue(serviceLoader.toString().length() > 0);

        serviceLoader = ServiceLoader.loadInstalled(Service.class);
        assertTrue(serviceLoader.toString().length() > 0);

        serviceLoader = ServiceLoader.loadInstalled(String.class);
        assertTrue(serviceLoader.toString().length() > 0);

        serviceLoader = ServiceLoader.load(null, ucl);
        assertNotNull(serviceLoader);
        try {
            serviceLoader.toString();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        serviceLoader = ServiceLoader.load(null, null);
        assertNotNull(serviceLoader);
        try {
            serviceLoader.toString();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        serviceLoader = ServiceLoader.load(Service.class, null);
        assertTrue(serviceLoader.toString().length() > 0);

        serviceLoader = ServiceLoader.load(null);
        assertNotNull(serviceLoader);
        try {
            serviceLoader.toString();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        serviceLoader = ServiceLoader.loadInstalled(null);
        assertNotNull(serviceLoader);
        try {
            serviceLoader.toString();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @SuppressWarnings("nls")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jarFile = prepairJar("hyts_services.jar");
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        new File(jarFile.getFile()).delete();
    }

}

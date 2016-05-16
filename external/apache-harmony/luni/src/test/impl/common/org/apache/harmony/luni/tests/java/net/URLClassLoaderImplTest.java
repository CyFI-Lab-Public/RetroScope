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

package org.apache.harmony.luni.tests.java.net;

import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import junit.framework.TestCase;

import libcore.net.url.JarHandler;

/**
 * Depends on: file://<basedir>/src/test/resources/org/apache/harmony/luni/tests/java/net/lf.jar
 */
public class URLClassLoaderImplTest extends TestCase {

    /**
     * @tests java.net.URLClassLoader#URLClassLoader(java.net.URL[], java.lang.ClassLoader,
     *        java.net.URLStreamHandlerFactory)
     */
    public void test_Constructor$Ljava_net_URLLjava_lang_ClassLoaderLjava_net_URLStreamHandlerFactory()
            throws Exception {
        class TestFactory implements URLStreamHandlerFactory {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if ("jar".equals(protocol)) {
                    return new JarHandler();
                }

                fail("Should be JarHandler. But " + protocol);
                return null;
            }
        }

        final URL base = getClass().getResource("lf.jar");

        final URL[] urls = { base };
        final URLClassLoader ucl = new URLClassLoader(urls, null, new TestFactory());

        final URL res = ucl.findResource("swt.dll");
        assertNotNull(res);

        final URI e = new URI("jar:" + base.toExternalForm() + "!/swt.dll");
        final URI a = res.toURI();
        assertEquals(e, a);
    }

}

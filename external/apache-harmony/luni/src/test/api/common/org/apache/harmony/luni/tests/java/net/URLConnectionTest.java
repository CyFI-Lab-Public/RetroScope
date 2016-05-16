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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import tests.support.Support_Configuration;
import tests.support.resource.Support_Resources;

public class URLConnectionTest extends junit.framework.TestCase {

    static class MockURLConnection extends URLConnection {

        public MockURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() {
            connected = true;
        }
    }

    static class NewHandler extends URLStreamHandler {
        protected URLConnection openConnection(URL u) throws IOException {
            return new HttpURLConnection(u) {
                @Override
                public void connect() throws IOException {
                    connected = true;
                }

                @Override
                public void disconnect() {
                    // do nothing
                }

                @Override
                public boolean usingProxy() {
                    return false;
                }
            };
        }
    }

    static String getContentType(String fileName) throws IOException {
        String resourceName = "org/apache/harmony/luni/tests/" + fileName;
        URL url = ClassLoader.getSystemClassLoader().getResource(resourceName);
        assertNotNull("Cannot find test resource " + resourceName, url);
        return url.openConnection().getContentType();
    }

    URL url;
    URLConnection uc;

    protected void setUp() throws Exception {
        url = new URL("http://localhost/");
        uc = url.openConnection();
    }

    protected void tearDown() {
        ((HttpURLConnection) uc).disconnect();
    }

    /**
     * @tests java.net.URLConnection#addRequestProperty(String, String)
     */
    public void test_addRequestProperty() throws MalformedURLException,
            IOException {

        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        try {
            // Regression for HARMONY-604
            u.addRequestProperty(null, "someValue");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        u.addRequestProperty("key", "value");
        u.addRequestProperty("key", "value2");
        assertEquals("value2", u.getRequestProperty("key"));
        ArrayList list = new ArrayList();
        list.add("value2");
        list.add("value");

        Map<String,List<String>> propertyMap = u.getRequestProperties();
        // Check this map is unmodifiable
        try {
            propertyMap.put("test", null);
            fail("Map returned by URLConnection.getRequestProperties() should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        List<String> valuesList = propertyMap.get("key");
        // Check this list is also unmodifiable
        try {
            valuesList.add("test");
            fail("List entries in the map returned by URLConnection.getRequestProperties() should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        assertEquals(list, valuesList);

        u.connect();
        try {
            // state of connection is checked first
            // so no NPE in case of null 'field' param
            u.addRequestProperty(null, "someValue");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#addRequestProperty(java.lang.String,java.lang.String)
     */
    public void test_addRequestPropertyLjava_lang_StringLjava_lang_String()
            throws IOException {
        uc.setRequestProperty("prop", "yo");
        uc.setRequestProperty("prop", "yo2");
        assertEquals("yo2", uc.getRequestProperty("prop"));
        Map<String, List<String>> map = uc.getRequestProperties();
        List<String> props = uc.getRequestProperties().get("prop");
        assertEquals(1, props.size());

        try {
            // the map should be unmodifiable
            map.put("hi", Arrays.asList(new String[] { "bye" }));
            fail("could modify map");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        try {
            // the list should be unmodifiable
            props.add("hi");
            fail("could modify list");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, "hyts_att.jar");
        URL fUrl1 = new URL("jar:file:" + resources.getPath()
                + "/hyts_att.jar!/");
        JarURLConnection con1 = (JarURLConnection) fUrl1.openConnection();
        map = con1.getRequestProperties();
        assertNotNull(map);
        assertEquals(0, map.size());
        try {
            // the map should be unmodifiable
            map.put("hi", Arrays.asList(new String[] { "bye" }));
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    /**
     * @tests java.net.URLConnection#getAllowUserInteraction()
     */
    public void test_getAllowUserInteraction() {
        uc.setAllowUserInteraction(false);
        assertFalse("getAllowUserInteraction should have returned false", uc
                .getAllowUserInteraction());

        uc.setAllowUserInteraction(true);
        assertTrue("getAllowUserInteraction should have returned true", uc
                .getAllowUserInteraction());
    }

    /**
     * @tests java.net.URLConnection#getContentEncoding()
     */
    public void test_getContentEncoding() {
        // should not be known for a file
        assertNull("getContentEncoding failed: " + uc.getContentEncoding(), uc
                .getContentEncoding());
    }

    /**
     * @tests java.net.URLConnection#getContentType()
     */
    public void test_getContentType_regression() throws IOException {
        // Regression for HARMONY-4699
        assertEquals("application/rtf", getContentType("test.rtf"));
        assertEquals("text/plain", getContentType("test.java"));
        // RI would return "content/unknown"
        assertEquals("application/msword", getContentType("test.doc"));
        assertEquals("text/html", getContentType("test.htx"));
        assertEquals("application/xml", getContentType("test.xml"));
        assertEquals("text/plain", getContentType("."));
    }

    /**
     * @tests java.net.URLConnection#getDate()
     */
    public void test_getDate() {
        // should be greater than 930000000000L which represents the past
        if (uc.getDate() == 0) {
            System.out
                    .println("WARNING: server does not support 'Date', in test_getDate");
        } else {
            assertTrue("getDate gave wrong date: " + uc.getDate(),
                    uc.getDate() > 930000000000L);
        }
    }

    /**
     * @tests java.net.URLConnection#getDefaultAllowUserInteraction()
     */
    public void test_getDefaultAllowUserInteraction() {
        boolean oldSetting = URLConnection.getDefaultAllowUserInteraction();

        URLConnection.setDefaultAllowUserInteraction(false);
        assertFalse(
                "getDefaultAllowUserInteraction should have returned false",
                URLConnection.getDefaultAllowUserInteraction());

        URLConnection.setDefaultAllowUserInteraction(true);
        assertTrue("getDefaultAllowUserInteraction should have returned true",
                URLConnection.getDefaultAllowUserInteraction());

        URLConnection.setDefaultAllowUserInteraction(oldSetting);
    }

    /**
     * @tests java.net.URLConnection#getDefaultRequestProperty(java.lang.String)
     */
    @SuppressWarnings("deprecation")
    public void test_getDefaultRequestPropertyLjava_lang_String() {
        URLConnection.setDefaultRequestProperty("Shmoo", "Blah");
        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Shmoo"));

        URLConnection.setDefaultRequestProperty("Shmoo", "Boom");
        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Shmoo"));

        assertNull("setDefaultRequestProperty should have returned: null",
                URLConnection.getDefaultRequestProperty("Kapow"));

        URLConnection.setDefaultRequestProperty("Shmoo", null);
    }

    /**
     * @tests java.net.URLConnection#getDefaultUseCaches()
     */
    public void test_getDefaultUseCaches() {
        boolean oldSetting = uc.getDefaultUseCaches();

        uc.setDefaultUseCaches(false);
        assertFalse("getDefaultUseCaches should have returned false", uc
                .getDefaultUseCaches());

        uc.setDefaultUseCaches(true);
        assertTrue("getDefaultUseCaches should have returned true", uc
                .getDefaultUseCaches());

        uc.setDefaultUseCaches(oldSetting);
    }

    /**
     * @tests java.net.URLConnection#getDoInput()
     */
    public void test_getDoInput() {
        assertTrue("Should be set to true by default", uc.getDoInput());

        uc.setDoInput(true);
        assertTrue("Should have been set to true", uc.getDoInput());

        uc.setDoInput(false);
        assertFalse("Should have been set to false", uc.getDoInput());
    }

    /**
     * @tests java.net.URLConnection#getDoOutput()
     */
    public void test_getDoOutput() {
        assertFalse("Should be set to false by default", uc.getDoOutput());

        uc.setDoOutput(true);
        assertTrue("Should have been set to true", uc.getDoOutput());

        uc.setDoOutput(false);
        assertFalse("Should have been set to false", uc.getDoOutput());
    }

    /**
     * @tests java.net.URLConnection#getExpiration()
     */
    public void test_getExpiration() {
        // should be unknown
        assertEquals("getExpiration returned wrong expiration", 0, uc
                .getExpiration());
    }

    /**
     * @tests java.net.URLConnection#getFileNameMap()
     */
    public void test_getFileNameMap() {
        // Tests for the standard MIME types -- users may override these
        // in their JRE
        FileNameMap map = URLConnection.getFileNameMap();

        // These types are defaulted
        assertEquals("text/html", map.getContentTypeFor(".htm"));
        assertEquals("text/html", map.getContentTypeFor(".html"));
        assertEquals("text/plain", map.getContentTypeFor(".text"));
        assertEquals("text/plain", map.getContentTypeFor(".txt"));

        // These types come from the properties file
        assertEquals("application/pdf", map.getContentTypeFor(".pdf"));
        assertEquals("application/zip", map.getContentTypeFor(".zip"));

        URLConnection.setFileNameMap(new FileNameMap() {
            public String getContentTypeFor(String fileName) {
                return "Spam!";
            }
        });
        try {
            assertEquals("Incorrect FileNameMap returned", "Spam!",
                    URLConnection.getFileNameMap().getContentTypeFor(null));
        } finally {
            // unset the map so other tests don't fail
            URLConnection.setFileNameMap(null);
        }
        // RI fails since it does not support fileName that does not begin with
        // '.'
        assertEquals("image/gif", map.getContentTypeFor("gif"));
    }

    /**
     * @tests java.net.URLConnection#getHeaderFieldDate(java.lang.String, long)
     */
    public void test_getHeaderFieldDateLjava_lang_StringJ() {

        if (uc.getHeaderFieldDate("Date", 22L) == 22L) {
            System.out
                    .println("WARNING: Server does not support 'Date', test_getHeaderFieldDateLjava_lang_StringJ not run");
            return;
        }
        assertTrue("Wrong value returned: "
                + uc.getHeaderFieldDate("Date", 22L), uc.getHeaderFieldDate(
                "Date", 22L) > 930000000000L);

        long time = uc.getHeaderFieldDate("Last-Modified", 0);
        assertEquals("Wrong date: ", time,
                Support_Configuration.URLConnectionLastModified);
    }

    /**
     * @tests java.net.URLConnection#getHeaderField(java.lang.String)
     */
    public void test_getHeaderFieldLjava_lang_String() {
        String hf;
        hf = uc.getHeaderField("Content-Encoding");
        if (hf != null) {
            assertNull(
                    "Wrong value returned for header field 'Content-Encoding': "
                            + hf, hf);
        }
        hf = uc.getHeaderField("Content-Length");
        if (hf != null) {
            assertEquals(
                    "Wrong value returned for header field 'Content-Length': ",
                    "25", hf);
        }
        hf = uc.getHeaderField("Content-Type");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'Content-Type': "
                    + hf, hf.contains("text/html"));
        }
        hf = uc.getHeaderField("content-type");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'content-type': "
                    + hf, hf.contains("text/html"));
        }
        hf = uc.getHeaderField("Date");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'Date': " + hf,
                    Integer.parseInt(hf.substring(hf.length() - 17,
                            hf.length() - 13)) >= 1999);
        }
        hf = uc.getHeaderField("Expires");
        if (hf != null) {
            assertNull(
                    "Wrong value returned for header field 'Expires': " + hf,
                    hf);
        }
        hf = uc.getHeaderField("SERVER");
        if (hf != null) {
            assertTrue("Wrong value returned for header field 'SERVER': " + hf
                    + " (expected " + Support_Configuration.HomeAddressSoftware
                    + ")", hf.equals(Support_Configuration.HomeAddressSoftware));
        }
        hf = uc.getHeaderField("Last-Modified");
        if (hf != null) {
            assertTrue(
                    "Wrong value returned for header field 'Last-Modified': "
                            + hf,
                    hf
                            .equals(Support_Configuration.URLConnectionLastModifiedString));
        }
        hf = uc.getHeaderField("accept-ranges");
        if (hf != null) {
            assertTrue(
                    "Wrong value returned for header field 'accept-ranges': "
                            + hf, hf.equals("bytes"));
        }
        hf = uc.getHeaderField("DoesNotExist");
        if (hf != null) {
            assertNull("Wrong value returned for header field 'DoesNotExist': "
                    + hf, hf);
        }
    }

    /**
     * @tests java.net.URLConnection#getIfModifiedSince()
     */
    public void test_getIfModifiedSince() {
        uc.setIfModifiedSince(200);
        assertEquals("Returned wrong ifModifiedSince value", 200, uc
                .getIfModifiedSince());
    }

    /**
     * @tests java.net.URLConnection#getLastModified()
     */
    public void test_getLastModified() {
        if (uc.getLastModified() == 0) {
            System.out
                    .println("WARNING: Server does not support 'Last-Modified', test_getLastModified() not run");
            return;
        }
        assertTrue(
                "Returned wrong getLastModified value.  Wanted: "
                        + Support_Configuration.URLConnectionLastModified
                        + " got: " + uc.getLastModified(),
                uc.getLastModified() == Support_Configuration.URLConnectionLastModified);
    }

    /**
     * @tests java.net.URLConnection#getRequestProperties()
     */
    public void test_getRequestProperties() {
        uc.setRequestProperty("whatever", "you like");
        Map headers = uc.getRequestProperties();

        // content-length should always appear
        List header = (List) headers.get("whatever");
        assertNotNull(header);

        assertEquals("you like", header.get(0));

        assertTrue(headers.size() >= 1);

        try {
            // the map should be unmodifiable
            headers.put("hi", "bye");
            fail();
        } catch (UnsupportedOperationException e) {
        }
        try {
            // the list should be unmodifiable
            header.add("hi");
            fail();
        } catch (UnsupportedOperationException e) {
        }

    }

    /**
     * @tests java.net.URLConnection#getRequestProperties()
     */
    public void test_getRequestProperties_Exception() throws IOException {
        URL url = new URL("http", "test", 80, "index.html", new NewHandler());
        URLConnection urlCon = url.openConnection();
        urlCon.connect();

        try {
            urlCon.getRequestProperties();
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#getRequestProperty(java.lang.String)
     */
    public void test_getRequestProperty_LString_Exception() throws IOException {
        URL url = new URL("http", "test", 80, "index.html", new NewHandler());
        URLConnection urlCon = url.openConnection();
        urlCon.setRequestProperty("test", "testProperty");
        assertEquals("testProperty", urlCon.getRequestProperty("test"));

        urlCon.connect();
        try {
            urlCon.getRequestProperty("test");
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#getRequestProperty(java.lang.String)
     */
    public void test_getRequestPropertyLjava_lang_String() {
        uc.setRequestProperty("Yo", "yo");
        assertTrue("Wrong property returned: " + uc.getRequestProperty("Yo"),
                uc.getRequestProperty("Yo").equals("yo"));
        assertNull("Wrong property returned: " + uc.getRequestProperty("No"),
                uc.getRequestProperty("No"));
    }

    /**
     * @tests java.net.URLConnection#getURL()
     */
    public void test_getURL() {
        assertTrue("Incorrect URL returned", uc.getURL().equals(url));
    }

    /**
     * @tests java.net.URLConnection#getUseCaches()
     */
    public void test_getUseCaches() {
        uc.setUseCaches(false);
        assertTrue("getUseCaches should have returned false", !uc
                .getUseCaches());
        uc.setUseCaches(true);
        assertTrue("getUseCaches should have returned true", uc.getUseCaches());
    }

    /**
     * @tests java.net.URLConnection#guessContentTypeFromStream(java.io.InputStream)
     */
    public void test_guessContentTypeFromStreamLjava_io_InputStream()
            throws IOException {
        String[] headers = new String[] { "<html>", "<head>", " <head ",
                "<body", "<BODY ", "<!DOCTYPE html", "<?xml " };
        String[] expected = new String[] { "text/html", "text/html",
                "text/html", "text/html", "text/html", "text/html",
                "application/xml" };

        String[] encodings = new String[] { "ASCII", "UTF-8", "UTF-16BE",
                "UTF-16LE", "UTF-32BE", "UTF-32LE" };
        for (int i = 0; i < headers.length; i++) {
            for (String enc : encodings) {
                InputStream is = new ByteArrayInputStream(toBOMBytes(
                        headers[i], enc));
                String mime = URLConnection.guessContentTypeFromStream(is);
                assertEquals("checking " + headers[i] + " with " + enc,
                        expected[i], mime);
            }
        }

        // Try simple case
        try {
            URLConnection.guessContentTypeFromStream(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        // Test magic bytes
        byte[][] bytes = new byte[][] { { 'P', 'K' }, { 'G', 'I' } };
        expected = new String[] { "application/zip", "image/gif" };

        for (int i = 0; i < bytes.length; i++) {
            InputStream is = new ByteArrayInputStream(bytes[i]);
            assertEquals(expected[i], URLConnection
                    .guessContentTypeFromStream(is));
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        assertNull(URLConnection.guessContentTypeFromStream(bais));
        bais.close();
    }

    /**
     * @tests java.net.URLConnection#setAllowUserInteraction(boolean)
     */
    public void test_setAllowUserInteractionZ() throws MalformedURLException {
        // Regression for HARMONY-72
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        u.connect();
        try {
            u.setAllowUserInteraction(false);
            fail("Assert 0: expected an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }

    }

    /**
     * @tests java.net.URLConnection#setConnectTimeout(int)
     */
    public void test_setConnectTimeoutI() throws Exception {
        URLConnection uc = new URL("http://localhost").openConnection();
        assertEquals(0, uc.getConnectTimeout());
        uc.setConnectTimeout(0);
        assertEquals(0, uc.getConnectTimeout());
        try {
            uc.setConnectTimeout(-100);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(0, uc.getConnectTimeout());
        uc.setConnectTimeout(100);
        assertEquals(100, uc.getConnectTimeout());
        try {
            uc.setConnectTimeout(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(100, uc.getConnectTimeout());
    }

    /**
     * @tests java.net.URLConnection#setDefaultAllowUserInteraction(boolean)
     */
    public void test_setDefaultAllowUserInteractionZ() {
        assertTrue("Used to test", true);
    }

    /**
     * @tests java.net.URLConnection#setDefaultRequestProperty(java.lang.String,
     *        java.lang.String)
     */
    public void test_setDefaultRequestPropertyLjava_lang_StringLjava_lang_String() {
        assertTrue("Used to test", true);
    }

    /**
     * @tests java.net.URLConnection#setDefaultUseCaches(boolean)
     */
    public void test_setDefaultUseCachesZ() {
        assertTrue("Used to test", true);
    }

    /**
     * @throws IOException
     * @tests java.net.URLConnection#setFileNameMap(java.net.FileNameMap)
     */
    public void test_setFileNameMapLjava_net_FileNameMap() throws IOException {
        // nothing happens if set null
        URLConnection.setFileNameMap(null);
        // take no effect
        assertNotNull(URLConnection.getFileNameMap());
    }

    /**
     * @tests java.net.URLConnection#setIfModifiedSince(long)
     */
    public void test_setIfModifiedSinceJ() throws IOException {
        URL url = new URL("http://localhost:8080/");
        URLConnection connection = url.openConnection();
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(2000, Calendar.MARCH, 5);

        long sinceTime = cal.getTime().getTime();
        connection.setIfModifiedSince(sinceTime);
        assertEquals("Wrong date set", sinceTime, connection
                .getIfModifiedSince());

    }

    /**
     * @tests java.net.URLConnection#setReadTimeout(int)
     */
    public void test_setReadTimeoutI() throws Exception {
        URLConnection uc = new URL("http://localhost").openConnection();
        assertEquals(0, uc.getReadTimeout());
        uc.setReadTimeout(0);
        assertEquals(0, uc.getReadTimeout());
        try {
            uc.setReadTimeout(-100);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(0, uc.getReadTimeout());
        uc.setReadTimeout(100);
        assertEquals(100, uc.getReadTimeout());
        try {
            uc.setReadTimeout(-1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
        assertEquals(100, uc.getReadTimeout());
    }

    /**
     * @tests java.net.URLConnection#setRequestProperty(String, String)
     */
    public void test_setRequestProperty() throws MalformedURLException,
            IOException {

        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        try {
            u.setRequestProperty(null, "someValue");
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        u.connect();
        try {
            // state of connection is checked first
            // so no NPE in case of null 'field' param
            u.setRequestProperty(null, "someValue");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#setRequestProperty(java.lang.String,
     *        java.lang.String)
     */
    public void test_setRequestPropertyLjava_lang_StringLjava_lang_String()
                throws MalformedURLException{
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));

        u.setRequestProperty("", "");
        assertEquals("", u.getRequestProperty(""));

        u.setRequestProperty("key", "value");
        assertEquals("value", u.getRequestProperty("key"));
    }

    /**
     * @tests java.net.URLConnection#setUseCaches(boolean)
     */
    public void test_setUseCachesZ() throws MalformedURLException {
        // Regression for HARMONY-71
        MockURLConnection u = new MockURLConnection(new URL(
                "http://www.apache.org"));
        u.connect();
        try {
            u.setUseCaches(true);
            fail("Assert 0: expected an IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    /**
     * @tests java.net.URLConnection#toString()
     */
    public void test_toString() {
        assertTrue("Wrong toString: " + uc.toString(), uc.toString().indexOf(
                "URLConnection") > 0);
    }

    private byte[] toBOMBytes(String text, String enc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        if (enc.equals("UTF-8")) {
            bos.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
        }
        if (enc.equals("UTF-16BE")) {
            bos.write(new byte[] { (byte) 0xFE, (byte) 0xFF });
        }
        if (enc.equals("UTF-16LE")) {
            bos.write(new byte[] { (byte) 0xFF, (byte) 0xFE });
        }
        if (enc.equals("UTF-32BE")) {
            bos.write(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE,
                    (byte) 0xFF });
        }
        if (enc.equals("UTF-32LE")) {
            bos.write(new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00,
                    (byte) 0x00 });
        }

        bos.write(text.getBytes(enc));
        return bos.toByteArray();
    }
}

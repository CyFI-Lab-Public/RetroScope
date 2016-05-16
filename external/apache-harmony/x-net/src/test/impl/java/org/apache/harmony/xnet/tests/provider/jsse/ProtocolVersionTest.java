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

package org.apache.harmony.xnet.tests.provider.jsse;

import org.apache.harmony.xnet.provider.jsse.ProtocolVersion;
import junit.framework.TestCase;

/**
 * Tests for <code>ProtocolVersion</code> constructor and methods
 *  
 */
public class ProtocolVersionTest extends TestCase {

    public void testEquals() {
        assertEquals(ProtocolVersion.getByVersion(new byte[] { 3, 0 }),
                ProtocolVersion.getByName("SSLv3"));
        assertEquals(ProtocolVersion.getByVersion(new byte[] { 3, 1 }),
                ProtocolVersion.getByName("TLSv1"));
        assertFalse(ProtocolVersion.getByVersion(new byte[] { 3, 0 }).equals(
                ProtocolVersion.getByName("TLSv1")));

    }

    /*
     * Class under test for boolean isSupported(byte[])
     */
    public void testIsSupportedbyteArray() {
        assertTrue(ProtocolVersion.isSupported(new byte[] { 3, 0 }));
        assertTrue(ProtocolVersion.isSupported(new byte[] { 3, 1 }));
        assertFalse(ProtocolVersion.isSupported(new byte[] { 3, 2 }));
    }

    public void testGetByVersion() {
        assertNull(ProtocolVersion.getByVersion(new byte[] { 2, 1 }));
        assertEquals("SSLv3",
                     ProtocolVersion.getByVersion(new byte[] { 3, 0 }).name);
        assertEquals("TLSv1",
                     ProtocolVersion.getByVersion(new byte[] { 3, 1 }).name);
    }

    /*
     * Class under test for boolean isSupported(String)
     */
    public void testIsSupportedString() {
        assertTrue(ProtocolVersion.isSupported("SSLv3"));
        assertTrue(ProtocolVersion.isSupported("SSL"));
        assertTrue(ProtocolVersion.isSupported("TLSv1"));
        assertTrue(ProtocolVersion.isSupported("TLS"));
        assertFalse(ProtocolVersion.isSupported("SSLv4"));
    }

    public void testGetByName() {
        assertNull(ProtocolVersion.getByName("SSLv2"));
        assertEquals("SSLv3", ProtocolVersion.getByName("SSLv3").name);
        assertEquals("TLSv1", ProtocolVersion.getByName("TLSv1").name);
    }

    public void testGetLatestVersion() {
        ProtocolVersion ver = ProtocolVersion.getLatestVersion(new String[] {
                "SSLv2", "TLSv1", "SSLv3" });
        assertEquals("Incorrect protocol version", "TLSv1", ver.name);

        ver = ProtocolVersion.getLatestVersion(new String[] {"SSLv3",
                "unknown", "SSLv2" });
        assertEquals("Incorrect protocol version", "SSLv3", ver.name);
    }

}
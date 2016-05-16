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

package org.apache.harmony.xnet.provider.jsse;

import java.io.ByteArrayInputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * SSLStreamedInputTest test
 */
public class SSLStreamedInputTest extends TestCase {

    /**
     * SSLStreamedInput(InputStream in) method testing.
     */
    public void testSSLStreamedInput() throws Exception {
        byte[] data = {1, 0, 0, 0, 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 5};
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        SSLStreamedInput sslsi = new SSLStreamedInput(bis);
        assertEquals(bis.available(), sslsi.available());
        assertEquals(1, sslsi.read());
        assertEquals(1, sslsi.readUint32());
        sslsi.skip(3);
        assertEquals(5, sslsi.readUint64());
        try {
            sslsi.read();
            fail("Expected EndOfSourceException was not thrown");
        } catch (EndOfSourceException e) {
        }
    }

    public static Test suite() {
        return new TestSuite(SSLStreamedInputTest.class);
    }

}

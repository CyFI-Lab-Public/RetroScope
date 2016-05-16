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

import org.apache.harmony.xnet.provider.jsse.AlertException;
import org.apache.harmony.xnet.provider.jsse.Handshake;
import org.apache.harmony.xnet.provider.jsse.HandshakeIODataStream;
import org.apache.harmony.xnet.provider.jsse.HelloRequest;

import junit.framework.TestCase;

/**
 * Tests for <code>HelloRequest</code> constructor and methods
 *
 */
public class HelloRequestTest extends TestCase {

    public void testHelloRequest() throws Exception {
        HelloRequest message = new HelloRequest();
        assertEquals("incorrect type", Handshake.HELLO_REQUEST, message
                .getType());
        assertEquals("incorrect HelloRequest", 0, message.length());

        HandshakeIODataStream out = new HandshakeIODataStream();
        message.send(out);
        byte[] encoded = out.getData(1000);
        assertEquals("incorrect out data length", message.length(),
                encoded.length);

        HandshakeIODataStream in = new HandshakeIODataStream();
        in.append(encoded);
        HelloRequest message_2 = new HelloRequest(in, message.length());
        assertEquals("incorrect message decoding", 0, message_2.length());

        in.append(encoded);
        try {
            new HelloRequest(in, message.length() - 1);
            fail("Small length: No expected AlertException");
        } catch (AlertException e) {
        }

        in.append(encoded);
        in.append(new byte[] { 1, 2, 3 });
        try {
            new HelloRequest(in, message.length() + 3);
            fail("Extra bytes: No expected AlertException ");
        } catch (AlertException e) {
        }
    }

}

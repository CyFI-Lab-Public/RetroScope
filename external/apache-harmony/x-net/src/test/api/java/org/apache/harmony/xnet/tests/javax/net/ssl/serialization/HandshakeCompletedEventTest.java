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

package org.apache.harmony.xnet.tests.javax.net.ssl.serialization;

import java.io.Serializable;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.apache.harmony.testframework.serialization.SerializationTest;

/**
 * Test for HandshakeCompletedEvent serialization
 * 
 */

public class HandshakeCompletedEventTest extends SerializationTest implements
        SerializationTest.SerializableAssert {

    @Override
    protected Object[] getData() {
        try {
            SSLContext cont = SSLContext.getInstance("TLS");
            cont.init(null, null, null);
            SSLSocket soc = (SSLSocket )cont.getSocketFactory().createSocket();
            return new Object[] { new HandshakeCompletedEvent(soc, soc.getSession())};
        } catch (Exception e) {
            fail("Can not create data: "+ e);
            return null;
        }
        
    }

    public void assertDeserialized(Serializable oref, Serializable otest) {
        HandshakeCompletedEvent test = (HandshakeCompletedEvent) otest;
        test.getSession();
        test.getSocket();
    }
}
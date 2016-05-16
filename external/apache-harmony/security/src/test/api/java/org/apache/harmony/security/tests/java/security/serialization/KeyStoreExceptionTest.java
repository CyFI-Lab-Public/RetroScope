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
* @author Vera Y. Petrashkova
*/

package org.apache.harmony.security.tests.java.security.serialization;

import java.security.KeyStoreException;

import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Test for KeyStoreException serialization
 *
 */

public class KeyStoreExceptionTest extends SerializationTest {

    public static String[] msgs = {
            "New message",
            "Long message for Exception. Long message for Exception. Long message for Exception." };

    protected Object[] getData() {
        Exception cause = new Exception(msgs[1]);
        KeyStoreException dExc = new KeyStoreException(msgs[0], cause);
        String msg = null;
        Throwable th = null;
        return new Object[] { new KeyStoreException(), new KeyStoreException(msg),
                new KeyStoreException(msgs[1]),
                new KeyStoreException(new Throwable()), new KeyStoreException(th),
                new KeyStoreException(msgs[1], dExc) };
    }

}

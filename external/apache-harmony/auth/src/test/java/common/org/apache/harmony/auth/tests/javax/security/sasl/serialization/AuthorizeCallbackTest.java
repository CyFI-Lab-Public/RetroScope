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

package org.apache.harmony.auth.tests.javax.security.sasl.serialization;

import java.io.Serializable;

import javax.security.sasl.AuthorizeCallback;

import org.apache.harmony.testframework.serialization.SerializationTest;

/**
 * Test for AuthorizeCallback serialization
 * 
 */

public class AuthorizeCallbackTest extends SerializationTest implements
        SerializationTest.SerializableAssert {

    public static String[] msgs = {
            "New String",
            "Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID. Long stringID." };

    @Override
    protected Object[] getData() {
        String msg = null;
        return new Object[] { new AuthorizeCallback(msg, msg),
                new AuthorizeCallback("", null),
                new AuthorizeCallback(null, msgs[0]),
                new AuthorizeCallback(msgs[1], msgs[1]), };
    }

    public void assertDeserialized(Serializable oref, Serializable otest) {
        AuthorizeCallback ref = (AuthorizeCallback) oref;
        AuthorizeCallback test = (AuthorizeCallback) otest;
        String idC = ref.getAuthenticationID();
        String idZ = ref.getAuthorizationID();
        String id = ref.getAuthorizedID();
        boolean is = ref.isAuthorized();
        if (idC == null) {
            assertNull(test.getAuthenticationID());
        } else {
            assertEquals(test.getAuthenticationID(), idC);
        }
        if (idZ == null) {
            assertNull(test.getAuthorizationID());
        } else {
            assertEquals(test.getAuthorizationID(), idZ);
        }
        if (id == null) {
            assertNull(test.getAuthorizedID());
        } else {
            assertEquals(test.getAuthorizedID(), id);
        }
        assertEquals(test.isAuthorized(), is);

    }
}
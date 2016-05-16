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

package org.apache.harmony.auth.tests.javax.security.sasl;

import javax.security.sasl.AuthorizeCallback;

import junit.framework.TestCase;

/**
 * Tests for constructor and methods of AuthorizeCallback class
 * 
 */

public class AuthorizeCallbackTest extends TestCase {
    /**
     * Test for <code>AuthorizeCallback(String authnID, String authzID)</code>
     * and get/set methods
     */
    public void test01() {
        AuthorizeCallback auth = new AuthorizeCallback(null, null);
        assertNull(auth.getAuthenticationID());
        assertNull(auth.getAuthorizationID());
        assertNull(auth.getAuthorizedID());
        assertFalse(auth.isAuthorized());

        auth.setAuthorized(true);
        assertTrue(auth.isAuthorized());
        assertNull(auth.getAuthorizedID());

        auth.setAuthorized(false);
        assertNull(auth.getAuthorizedID());
        assertFalse(auth.isAuthorized());

        auth.setAuthorizedID("ZZZ");
        auth.setAuthorized(true);
        assertEquals(auth.getAuthorizedID(), "ZZZ");
        assertNull(auth.getAuthorizationID());
        assertTrue(auth.isAuthorized());
    }

    /**
     * Test for <code>AuthorizeCallback(String authnID, String authzID)</code>
     * and get/set methods
     */
    public void test02() {
        String[] authenticationIDs = {
                "",
                "authenticationIDs",
                "Long String LongString Long String LongString Long String LongString Long String LongString Long String LongString Long String LongString" };
        String[] authorizedIDs = {
                "",
                "authorizedIDs",
                "Long String LongString Long String LongString Long String LongString Long String LongString Long String LongString Long String LongString" };
        String[] newAuthorizedIDs = {
                "new authorizedIDs",
                "another authorizedIDs",
                "some long string for authorized IDs some long string for authorized IDs some long string for authorized IDs" };
        AuthorizeCallback auth;
        for (int i = 0; i < authenticationIDs.length; i++) {
            for (int j = 0; j < authorizedIDs.length; j++) {
                auth = new AuthorizeCallback(authenticationIDs[i],
                        authorizedIDs[j]);
                assertEquals(auth.getAuthenticationID(), authenticationIDs[i]);
                assertEquals(auth.getAuthorizationID(), authorizedIDs[j]);
                assertNull(auth.getAuthorizedID());
                assertFalse(auth.isAuthorized());

                auth.setAuthorized(true);
                assertTrue(auth.isAuthorized());
                assertEquals(auth.getAuthorizedID(), auth.getAuthorizationID());

                auth.setAuthorized(false);
                assertNull(auth.getAuthorizedID());
                assertFalse(auth.isAuthorized());

                for (int l = 0; l < newAuthorizedIDs.length; l++) {
                    auth.setAuthorizedID(newAuthorizedIDs[l]);
                    assertNull(auth.getAuthorizedID());
                    auth.setAuthorized(true);
                    assertFalse(auth.getAuthorizedID().equals(
                            auth.getAuthorizationID()));
                    assertEquals(auth.getAuthorizedID(), newAuthorizedIDs[l]);
                    auth.setAuthorizedID(newAuthorizedIDs[l] + " ZZZ");
                    assertFalse(auth.getAuthorizedID().equals(
                            auth.getAuthorizationID()));
                    assertFalse(auth.getAuthorizedID().equals(
                            newAuthorizedIDs[l]));
                    assertEquals(auth.getAuthorizedID(), newAuthorizedIDs[l]
                            + " ZZZ");

                    auth.setAuthorized(false);
                }

            }
        }
    }
}

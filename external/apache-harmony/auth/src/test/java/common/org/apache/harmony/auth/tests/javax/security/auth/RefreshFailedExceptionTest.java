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
* @author Maxim V. Makarov
*/

package org.apache.harmony.auth.tests.javax.security.auth;

import javax.security.auth.RefreshFailedException;

import junit.framework.TestCase;

/**
 * Tests RefreshFailedException class
 */
public class RefreshFailedExceptionTest extends TestCase {

    /**
     * @tests javax.security.auth.RefreshFailedException#RefreshFailedException()
     */
    public final void testCtor1() {
        assertNull(new RefreshFailedException().getMessage());
    }

    /**
     * @tests javax.security.auth.RefreshFailedException#RefreshFailedException(
     *        java.lang.String)
     */
    public final void testCtor2() {
        assertNull(new RefreshFailedException(null).getMessage());

        String message = "";
        assertSame(message, new RefreshFailedException(message).getMessage());

        message = "message";
        assertSame(message, new RefreshFailedException(message).getMessage());
    }
}

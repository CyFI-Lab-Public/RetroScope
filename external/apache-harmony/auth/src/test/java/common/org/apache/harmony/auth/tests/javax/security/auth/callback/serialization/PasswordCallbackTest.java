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

package org.apache.harmony.auth.tests.javax.security.auth.callback.serialization;

import java.io.Serializable;

import javax.security.auth.callback.PasswordCallback;

import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Serialization test for PasswordCallback class
 */
public class PasswordCallbackTest extends SerializationTest implements
        SerializationTest.SerializableAssert {

    @Override
    protected Object[] getData() {
        char[] pwd = {'a', 'b', 'c'};
        PasswordCallback p = new PasswordCallback("prmpt", true);
        p.setPassword(pwd);
        return new Object[] { new PasswordCallback("prompt", true), p };
    }

  
    public void assertDeserialized(Serializable golden, Serializable test) {
        assertTrue(test instanceof PasswordCallback);
        assertEquals(((PasswordCallback) golden).getPrompt(),
                ((PasswordCallback) test).getPrompt());
        if (((PasswordCallback) test).getPassword() != null) {
            assertEquals(new String(((PasswordCallback) golden)
                    .getPassword()), new String(
                    ((PasswordCallback) test).getPassword()));
        }
    }
}
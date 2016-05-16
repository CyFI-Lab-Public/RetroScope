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
import java.util.Locale;
import javax.security.auth.callback.LanguageCallback;

import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Serialization test for LanguageCallback class
 */
public class LanguageCallbackTest extends SerializationTest implements
        SerializationTest.SerializableAssert {

    @Override
    protected Object[] getData() {
        LanguageCallback lc = new LanguageCallback();
        lc.setLocale(Locale.UK);
        return new Object[] { lc };
    }

    public void assertDeserialized(Serializable golden, Serializable test) {
        assertTrue(test instanceof LanguageCallback);
        assertEquals(((LanguageCallback) golden).getLocale(),
                ((LanguageCallback) test).getLocale());
    }
}
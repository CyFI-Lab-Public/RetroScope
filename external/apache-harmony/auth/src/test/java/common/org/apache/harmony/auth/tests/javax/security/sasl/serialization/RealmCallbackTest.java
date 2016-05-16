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

import javax.security.sasl.RealmCallback;

import org.apache.harmony.testframework.serialization.SerializationTest;

/**
 * Test for RealmCallback serialization
 * 
 */

public class RealmCallbackTest extends SerializationTest implements
        SerializationTest.SerializableAssert {

    public static String[] msgs = {
            "New String",
            "Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string." };

    public static String addText = "This text was set to RealmCallback";
    
    @Override
    protected Object[] getData() {
        Object [] oo = {
                new RealmCallback(msgs[0], msgs[1]),
                new RealmCallback(msgs[1], msgs[0]),
                new RealmCallback(msgs[1], msgs[1])
        };
        for (Object element : oo) {
            ((RealmCallback)element).setText(addText);
        }
        return oo;
    }

    public void assertDeserialized(Serializable oref, Serializable otest) {
        RealmCallback ref = (RealmCallback) oref;
        RealmCallback test = (RealmCallback) otest;
        String dText = ref.getDefaultText();
        String text = ref.getText();
        String prompt = ref.getPrompt();
        assertEquals(dText, test.getDefaultText());
        assertEquals(text, test.getText());
        assertEquals(prompt, test.getPrompt());

    }
}
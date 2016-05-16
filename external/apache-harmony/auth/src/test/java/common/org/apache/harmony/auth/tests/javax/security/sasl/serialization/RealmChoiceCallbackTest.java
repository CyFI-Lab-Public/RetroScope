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

import javax.security.sasl.RealmChoiceCallback;

import org.apache.harmony.testframework.serialization.SerializationTest;

/**
 * Test for RealmChoiceCallback serialization
 * 
 */

public class RealmChoiceCallbackTest extends SerializationTest implements
        SerializationTest.SerializableAssert {

    public static String[] msgs = {
            "New String",
            "Another string",
            "Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string. Long string.",
            "t"};

    public static final int [] idx = {2, 3};
    
    @Override
    protected Object[] getData() {
        Object [] oo = {
                new RealmChoiceCallback(msgs[0], msgs, 0, true),
                new RealmChoiceCallback(msgs[1], msgs, 1, true),
                new RealmChoiceCallback(msgs[1], msgs, 0, false),
                new RealmChoiceCallback(msgs[2], msgs, 0, false)

        };        
        for (Object element : oo) {
            RealmChoiceCallback rc = (RealmChoiceCallback)element;           
            if (rc.allowMultipleSelections()) {
                rc.setSelectedIndexes(idx);
            } else {
                rc.setSelectedIndex(msgs.length - 1);
            }
        }
        return oo;
    }

    public void assertDeserialized(Serializable oref, Serializable otest) {
        RealmChoiceCallback ref = (RealmChoiceCallback) oref;
        RealmChoiceCallback test = (RealmChoiceCallback) otest;
        
        boolean all = ref.allowMultipleSelections();
        assertEquals(all, test.allowMultipleSelections());
        String prompt = ref.getPrompt();
        assertEquals(prompt, test.getPrompt());
        
        String [] ch = ref.getChoices();
        String [] tCh = test.getChoices();        
        assertEquals(ch.length, tCh.length);        
        for (int i = 0; i < ch.length; i++) {
            assertEquals(ch[i], tCh[i]);
        }
        assertEquals(ref.getDefaultChoice(), test.getDefaultChoice());
        int [] in = ref.getSelectedIndexes();
        int [] tIn = test.getSelectedIndexes();
//        assertNull("in is not null", in);            
//        assertNull("tIn is not null", tIn);

        if (!all) {
            assertEquals("Incorrect length in ", in.length, 1);
            assertEquals("Incorrect length tIn ", tIn.length, 1);
            assertEquals("Incorrect index", in[0], tIn[0]);
        } else {
            assertEquals("Incorrect length", in.length, tIn.length);
            for (int i = 0; i < in.length; i++) {
                assertEquals(in[i], tIn[i]);
            }            
        }
    }
}
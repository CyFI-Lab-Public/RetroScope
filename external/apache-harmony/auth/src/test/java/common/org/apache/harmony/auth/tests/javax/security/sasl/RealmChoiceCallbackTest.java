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

import javax.security.sasl.RealmChoiceCallback;

import junit.framework.TestCase;

/**
 * Tests for constructor and methods ofRealmChoiceCallback class 
 * 
 */

public class RealmChoiceCallbackTest extends TestCase {
    private static final String[] prompts = {
            "Prompts",
            "Another prompt",
            "Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt Long String for prompt" };

    private static final String [] emptyCh = {};
    private static final String [] wrongCh1 = {"Default1", "", "Default2"};
    private static final String [] wrongCh2 = {"Default1", null, "Default2"};
    private static final String [] choices = {
            "DefaultRealmInfo",
            "Another default realm info",
            "Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info Long string for default realm info " };
    private static final int indexes[] = { Integer.MIN_VALUE, -1, 0, 100,
            20000, Integer.MAX_VALUE };
    
    /**
     * Test for <code>RealmChoiceCallback(String prompt, String[] choices,
     * int defaultChoice, boolean multiple) </code> constructor
     * 
     * Assertion: throws IllegalArgumentException when
     * string parameters are null or empty and if defaultChoice is not
     * within of choices array
     */
    public void test01() {
        try {
            new RealmChoiceCallback(null, choices, 0, true);
            fail("IllegalArgumentException should be thrown for null prompt");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("", choices, 0, true);
            fail("IllegalArgumentException should be thrown for empty prompt");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("prompt", null, 0, true);
            fail("IllegalArgumentException should be thrown for null choices");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("prompt", emptyCh, 0, true);
            fail("IllegalArgumentException should be thrown for empty choices");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("prompt", wrongCh1, 0, true);
            fail("IllegalArgumentException should be thrown for incorrect choices");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("prompt", wrongCh2, 0, false);
            fail("IllegalArgumentException should be thrown for incorrect choices");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("prompt", choices, -1, true);
            fail("IllegalArgumentException should be thrown for incorrect choices");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("prompt", choices, choices.length, true);
            fail("IllegalArgumentException should be thrown for incorrect default index");
        } catch (IllegalArgumentException e) {            
        }
        try {
            new RealmChoiceCallback("prompt", choices, choices.length + 1, true);
            fail("IllegalArgumentException should be thrown for incorrect default index");
        } catch (IllegalArgumentException e) {            
        }
    }
    
    /**
     * Test for <code>RealmChoiceCallback(String prompt, String[] choices,
     * int defaultChoice, boolean multiple) </code> constructor
     * 
     * Assertion: creates RealmChoiceCallback object which does not allow
     * multiple choices
     */ 
    public void test02() {
        RealmChoiceCallback rCCB;
        for (int i = 0; i < prompts.length; i++) {
            rCCB = new RealmChoiceCallback(prompts[i], choices, 0, false);
            assertEquals("Incorrect prompt", rCCB.getPrompt(), prompts[i]);
            String [] ch = rCCB.getChoices();
            assertEquals("Incorrect choices length", ch.length, choices.length);
            for (int j = 0; j < ch.length; j++) {
                assertEquals("Incorrect choice number: " + j, ch[j], choices[j]);                
            }
            assertFalse("Incorrect multiple", rCCB.allowMultipleSelections());
            int [] ind = rCCB.getSelectedIndexes();
            assertNull("Incorrect selected indexes", ind);
            ind = new int[3];
            try {
                rCCB.setSelectedIndexes(ind);
                fail("UnsupportedOperationException should be thrown for non-multiple callback");
            } catch (UnsupportedOperationException e) {
            }
            try {
                rCCB.setSelectedIndexes(null);
                fail("UnsupportedOperationException should be thrown for non-multiple callback");
            } catch (UnsupportedOperationException e) {
            }
            for (int j = 0; j < indexes.length; j++) {
                rCCB.setSelectedIndex(indexes[j]);
                ind = rCCB.getSelectedIndexes();
                assertEquals("Incorrect index length", ind.length, 1);
                assertEquals("Incorrect index", ind[0], indexes[j]);
            }
        }        
    }
    
    /**
     * Test for <code>RealmChoiceCallback(String prompt, String[] choices,
     * int defaultChoice, boolean multiple) </code> constructor
     * 
     * Assertion: creates RealmChoiceCallback object which allows
     * multiple choices
     */ 
    public void test03() {
        RealmChoiceCallback rCCB;
        for (int i = 0; i < prompts.length; i++) {
            rCCB = new RealmChoiceCallback(prompts[i], choices, 0, true);
            assertEquals("Incorrect prompt", rCCB.getPrompt(), prompts[i]);
            String[] ch = rCCB.getChoices();
            assertEquals("Incorrect choices length", ch.length, choices.length);
            for (int j = 0; j < ch.length; j++) {
                assertEquals("Incorrect choice number: " + j, ch[j], choices[j]);
            }
            assertTrue("Incorrect multiple", rCCB.allowMultipleSelections());
            int[] ind = rCCB.getSelectedIndexes();
            assertNull("Incorrect selected indexes", ind);
            rCCB.setSelectedIndexes(indexes);
            ind = rCCB.getSelectedIndexes();
            assertEquals("Incorrect index length", ind.length, indexes.length);
            for (int j = 0; j < indexes.length; j++) {
                assertEquals("Incorrect index number: " + Integer.toString(j),
                        ind[j], indexes[j]);
            }
            for (int j = indexes.length - 1; j >= 0; j--) {
                rCCB.setSelectedIndex(indexes[j]);
                ind = rCCB.getSelectedIndexes();
                assertEquals("Incorrect index length", ind.length, 1);
                assertEquals("Incorrect index", ind[0], indexes[j]);
            }
            rCCB.setSelectedIndexes(indexes);
            ind = rCCB.getSelectedIndexes();
            assertEquals("Incorrect index length", ind.length, indexes.length);
            for (int j = 0; j < indexes.length; j++) {
                assertEquals("Incorrect index number: " + Integer.toString(j),
                        ind[j], indexes[j]);
            }
            rCCB.setSelectedIndexes(null);
            assertNull("Incorrect indexes array", rCCB.getSelectedIndexes());
        }
    }
}

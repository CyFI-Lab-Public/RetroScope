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

package org.apache.harmony.auth.tests.javax.security.auth.callback;

import javax.security.auth.callback.ChoiceCallback;

import junit.framework.TestCase;

/**
 * Tests ChoiceCallback class
 */

public class ChoiceCallbackTest extends TestCase {

    ChoiceCallback cb;

    String prompt = "prompt";

    int defaultChoice = 1;

    String[] choices = { "AAA", "BBB" };

    int index[] = { 1, 2, 3 };

    /**
     * Class under test for Ctor
     */
    public final void testChoiceCallback_01() {
        cb = new ChoiceCallback(prompt, choices, defaultChoice, true);
        assertEquals(this.prompt, cb.getPrompt());
        assertEquals(this.choices, cb.getChoices());
        assertEquals(this.defaultChoice, cb.getDefaultChoice());
        assertTrue(cb.allowMultipleSelections());
    }

    /**
     * test for the method setSelectedIndexes
     */
    public final void testChoiceCallback_02() {
        cb = new ChoiceCallback(prompt, choices, defaultChoice, false);
        try {
            cb.setSelectedIndexes(index);
            fail("should be throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
        cb.setSelectedIndex(1);
        assertEquals(1, cb.getSelectedIndexes()[0]);
    }
    
    public final void testChoiceCallback_03() {
        
        try {
            cb = new ChoiceCallback(prompt, null, defaultChoice, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            cb = new ChoiceCallback(null, choices, defaultChoice, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            cb = new ChoiceCallback(prompt, choices, -1, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            cb = new ChoiceCallback(prompt, new String[0], defaultChoice, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            cb = new ChoiceCallback("", choices, defaultChoice, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            cb = new ChoiceCallback(prompt, choices, 5, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            cb = new ChoiceCallback(prompt, choices, 2, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public final void testChoiceCallback_04() {
        String[] ch = {null};

        try {
            cb = new ChoiceCallback(prompt, ch, 5, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        
        String[] ch1 = {""};

        try {
            cb = new ChoiceCallback(prompt, ch1, 5, true);
            fail("should be throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        

    }
    /**
     * test whether implementation is mutable
     */
    public final void testMutable() {
        String c[] = { "A", "B", "C" };
        cb = new ChoiceCallback(prompt, c, defaultChoice, false);
        c[0] = "D";
        assertFalse("A".equals(cb.getChoices()[0]));
        int selection[] = { 1, 2, 3 };
        cb = new ChoiceCallback(prompt, c, defaultChoice, true);
        cb.setSelectedIndexes(selection);
        selection[0] = 4;
        assertEquals(4, cb.getSelectedIndexes()[0]);
        cb.getSelectedIndexes()[0] = 5;
        assertEquals(5, cb.getSelectedIndexes()[0]);
    }

}

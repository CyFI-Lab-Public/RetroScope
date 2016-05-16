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
 * @author Alexander V. Esin
 */
package org.ietf.jgss;

import junit.framework.TestCase;

/**
 * Tests MessageProp class
 */
public class MessagePropTest extends TestCase {

    public void testGetQOP() {
        MessageProp mp= new MessageProp(true);
        int qop= mp.getQOP();
        assertEquals(0, qop);
    }
    
    public void testGetQOP_0() {
        MessageProp mp= new MessageProp(10, true);
        int qop= mp.getQOP();
        assertEquals(10, qop);
    }
    
    public void testGetQOP_1() {
        MessageProp mp= new MessageProp(true);
        mp.setQOP(5);
        int qop= mp.getQOP();
        assertEquals(5, qop);
    }
    
    public void testGetQOP_2() {
        MessageProp mp= new MessageProp(10, true);
        mp.setQOP(5);
        int qop= mp.getQOP();
        assertEquals(5, qop);
    }
    
    public void testGetPrivacy() {
        MessageProp mp= new MessageProp(true);
        boolean privacy= mp.getPrivacy();
        assertTrue(privacy);
    }
    
    public void testGetPrivacy_0() {
        MessageProp mp= new MessageProp(false);
        mp.setPrivacy(true);
        boolean privacy= mp.getPrivacy();
        assertTrue(privacy);
    }
    
    public void testGetPrivacy_1() {
        MessageProp mp= new MessageProp(10, true);
        boolean privacy= mp.getPrivacy();
        assertTrue(privacy);
    }
    
    public void testGetPrivacy_2() {
        MessageProp mp= new MessageProp(10, true);
        mp.setPrivacy(false);
        boolean privacy= mp.getPrivacy();
        assertTrue(!privacy);
    }
    
    public void testIsDuplicateToken() {
        MessageProp mp= new MessageProp(true);
        boolean d= mp.isDuplicateToken();
        assertTrue(!d);
    }
    
    public void testIsDuplicateToken_0() {
        MessageProp mp= new MessageProp(true);
        mp.setSupplementaryStates(true, false, false, false, 1, "minor string");
        boolean d= mp.isDuplicateToken();
        assertTrue(d);
    }
    
    public void testIsOldToken() {
        MessageProp mp= new MessageProp(true);
        boolean d= mp.isOldToken();
        assertTrue(!d);
    }
    
    public void testIsOldToken_0() {
        MessageProp mp= new MessageProp(true);
        mp.setSupplementaryStates(false, true, false, false, 1, "minor string");
        boolean d= mp.isOldToken();
        assertTrue(d);
    }
    
    public void testIsUnseqToken() {
        MessageProp mp= new MessageProp(true);
        boolean d= mp.isUnseqToken();
        assertTrue(!d);
    }
    
    public void testIsUnseqToken_0() {
        MessageProp mp= new MessageProp(true);
        mp.setSupplementaryStates(false, false, true, false, 1, "minor string");
        boolean d= mp.isUnseqToken();
        assertTrue(d);
    }
    
    public void testIsGapToken() {
        MessageProp mp= new MessageProp(true);
        boolean d= mp.isGapToken();
        assertTrue(!d);
    }
    
    public void testIsGapToken_0() {
        MessageProp mp= new MessageProp(true);
        mp.setSupplementaryStates(false, false, true, true, 1, "minor string");
        boolean d= mp.isGapToken();
        assertTrue(d);
    }
    
    public void testGetMinorStatus() {
        MessageProp mp= new MessageProp(true);
        int ms= mp.getMinorStatus();
        assertEquals(0, ms);
    }
    
    public void testGetMinorStatus_0() {
        MessageProp mp= new MessageProp(true);
        mp.setSupplementaryStates(false, false, true, true, 10, "minor string");
        int ms= mp.getMinorStatus();
        assertEquals(10, ms);
    }
    
    public void testGetMinorString() {
        MessageProp mp= new MessageProp(true);
        String s= mp.getMinorString();
        assertNull(s);
    }
    
    public void testGetMinorString_0() {
        MessageProp mp= new MessageProp(true);
        mp.setSupplementaryStates(false, false, true, true, 10, "minor string");
        String s= mp.getMinorString();
        assertEquals("minor string", s);
    }
}

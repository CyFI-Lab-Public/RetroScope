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
 * Tests GSSException class
 */
public class GSSExceptionTest extends TestCase {

    public void testGetMajor() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        assertEquals(GSSException.BAD_NAME,  gssException.getMajor() );
    }
    
    public void testGetMajor_0() {
        GSSException gssException= new GSSException(0);
        assertEquals(GSSException.FAILURE,  gssException.getMajor() );
    }

    public void testGetMajor_1() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME, GSSException.BAD_NAME, "Bad day today");
        assertEquals(GSSException.BAD_NAME,  gssException.getMajor() );
    }
    
    public void testGetMajor_2() {
        GSSException gssException= new GSSException(0, 0, "Bad day today");
        assertEquals(GSSException.FAILURE,  gssException.getMajor() );
    }

    public void testGetMajor_3() {
        GSSException gssException= new GSSException(GSSException.NO_CRED);
        assertEquals(GSSException.NO_CRED,  gssException.getMajor());
    }
    
    public void testGetMajor_4() {
        GSSException gssException= new GSSException(-1, -1, "Bad day today");
        assertEquals(GSSException.FAILURE,  gssException.getMajor());
    }
        
    public void testGetMajorString() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        String majorString= gssException.getMajorString();
        assertEquals("BAD NAME",  majorString );
    }
    
    public void testGetMinor() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        assertEquals(0,  gssException.getMinor() );
    }
    
    public void testGetMinor_0() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME, GSSException.BAD_NAME, "Bad day today");
        assertEquals(GSSException.BAD_NAME,  gssException.getMinor() );
    }
    
    public void testGetMinor_1() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        gssException.setMinor(GSSException.BAD_NAME, "Unclear reason");
        assertEquals(GSSException.BAD_NAME,  gssException.getMinor() );
    }
    
    public void testGetMinor_2() {
        GSSException gssException= new GSSException(-1, -1, "Bad day today");
        assertEquals(-1,  gssException.getMinor() );
    }
    
    public void testGetMinor_3() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        gssException.setMinor(-1, "Unclear reason");
        assertEquals(-1,  gssException.getMinor() );
    }
    
    
    public void testGetMinorString() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        String minorString= gssException.getMinorString();
        assertNull(minorString);
    }
        
    public void testGetMinorString_0() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME, GSSException.BAD_NAME, "Bad day today");
        String minorString= gssException.getMinorString();
        assertEquals("Bad day today",  minorString );
    }
    
    public void testGetMinorString_1() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        gssException.setMinor(-1, "Bad day today");
        String minorString= gssException.getMinorString();
        assertEquals("Bad day today",  minorString );
    }
    public void testGetMinorString_2() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME, 0, "Bad day today");
        String minorString= gssException.getMinorString();
        assertNull(minorString);
    }
    
    public void testGetMinorString_3() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        gssException.setMinor(0, "Bad day today");
        String minorString= gssException.getMinorString();
        assertNull(minorString);
    }
    
    public void testGetMessage() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        assertEquals("BAD NAME",  gssException.getMessage() );
    }
    
    public void testGetMessage_0() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME, GSSException.BAD_NAME, "Bad day today");
        assertEquals("BAD NAME (Bad day today)",  gssException.getMessage() );
    }
    
    public void testGetMessage_1() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        gssException.setMinor(GSSException.BAD_NAME, "Unclear reason");
        assertEquals("BAD NAME (Unclear reason)",  gssException.getMessage() );
    }
    
    public void testGetMessage_2() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME, GSSException.BAD_NAME, "Bad day today");
        gssException.setMinor(GSSException.BAD_NAME, "Unclear reason");
        assertEquals("BAD NAME (Unclear reason)",  gssException.getMessage() );
    }
    
    public void testToString() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME);
        assertEquals("GSSException: BAD NAME",  gssException.toString() );
    }
    
    public void testToString_0() {
        GSSException gssException= new GSSException(GSSException.BAD_NAME, GSSException.BAD_NAME, "Bad day today");
        assertEquals("GSSException: BAD NAME (Bad day today)",  gssException.toString() );
    }
}

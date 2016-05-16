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
 * Tests GSSName interface
 */
public class GSSNameTest extends TestCase {

    public void testNT_ANONYMOUS() throws Exception {
        Oid oid= new Oid("1.3.6.1.5.6.3");
        assertTrue(oid.equals(GSSName.NT_ANONYMOUS));
    }
    
    public void testNT_EXPORT_NAME() throws Exception {
        Oid oid= new Oid("1.3.6.1.5.6.4");
        assertTrue(oid.equals(GSSName.NT_EXPORT_NAME));
    }
    
    public void testNT_HOSTBASED_SERVICE() throws Exception {
        Oid oid= new Oid("1.3.6.1.5.6.2");
        assertTrue(oid.equals(GSSName.NT_HOSTBASED_SERVICE));
    }
    
    public void testNT_MACHINE_UID_NAME() throws Exception {
        Oid oid= new Oid("1.2.840.113554.1.2.1.2");
        assertTrue(oid.equals(GSSName.NT_MACHINE_UID_NAME));
    }
    
    public void testNT_STRING_UID_NAME() throws Exception {
        Oid oid= new Oid("1.2.840.113554.1.2.1.3");
        assertTrue(oid.equals(GSSName.NT_STRING_UID_NAME));
    }
    
    public void testNT_USER_NAME() throws Exception {
        Oid oid= new Oid("1.2.840.113554.1.2.1.1");
        assertTrue(oid.equals(GSSName.NT_USER_NAME));
    }
}

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

package org.apache.harmony.auth.tests.jgss.kerberos;

import java.util.Arrays;

import org.apache.harmony.auth.jgss.kerberos.KerberosName;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import junit.framework.TestCase;

public class KerberosNameTest extends TestCase {

	
	public void testExport() throws Exception {
		KerberosName kerberosName = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		byte[] exported = kerberosName.export();
		byte[] expected = new byte[] { 4,1,0,11,6,9,42,-122,72,-122,-9,18,1,2,2,0,0,0,17,115,101,114,118,105,99,101,47,108,111,99,97,108,104,111,115,116 };
		assertTrue(Arrays.equals(expected, exported));		
	}
	
	public void testEquals() throws Exception{
		KerberosName one = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		KerberosName another = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		assertEquals(one, another);
		
		one = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		another = new KerberosName("service/localhost", GSSName.NT_HOSTBASED_SERVICE);
		assertEquals(one, another);
		
		one = new KerberosName("service@localhost", GSSName.NT_USER_NAME);
		another = new KerberosName("service@localhost", GSSName.NT_USER_NAME);
		assertEquals(one, another);
		
		one = new KerberosName("service@localhost", GSSName.NT_USER_NAME);
		another = new KerberosName("service/localhost", GSSName.NT_USER_NAME);
		assertFalse(one.equals(another));
		
		final Oid KRB5_PRINCIPAL_NAMETYPE = new Oid("1.2.840.113554.1.2.2.1");
		one = new KerberosName("service@localhost", KRB5_PRINCIPAL_NAMETYPE);
		another = new KerberosName("service@localhost", KRB5_PRINCIPAL_NAMETYPE);
		assertEquals(one, another);
		
		one = new KerberosName("service@localhost", KRB5_PRINCIPAL_NAMETYPE);
		another = new KerberosName("service/localhost",KRB5_PRINCIPAL_NAMETYPE);
		assertFalse(one.equals(another));
		
		one = new KerberosName("service@localhost", KRB5_PRINCIPAL_NAMETYPE);
		another = new KerberosName("service@localhost", GSSName.NT_USER_NAME);
		assertEquals(one,another);
		
		one = new KerberosName("service@localhost", KRB5_PRINCIPAL_NAMETYPE);
		another = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		assertFalse(one.equals(another));
		
		one = new KerberosName("service/localhost", KRB5_PRINCIPAL_NAMETYPE);
		another = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		assertEquals(one,another);
		
		one = new KerberosName("service@localhost", GSSName.NT_USER_NAME);
		another = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		assertFalse(one.equals(another));
		
		one = new KerberosName("service/localhost", GSSName.NT_USER_NAME);
		another = new KerberosName("service@localhost", GSSName.NT_HOSTBASED_SERVICE);
		assertFalse(one.equals(another));
	}
}

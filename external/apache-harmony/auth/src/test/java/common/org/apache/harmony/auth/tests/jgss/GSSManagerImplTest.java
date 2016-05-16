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

package org.apache.harmony.auth.tests.jgss;

import java.util.Arrays;

import org.apache.harmony.auth.jgss.kerberos.KerberosUtils;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import junit.framework.TestCase;

public class GSSManagerImplTest extends TestCase {
	
	private GSSManager gssManager;
	
	public void testGetMechs() throws Exception{
		Oid[] mechs = gssManager.getMechs();
		Oid kerberosMech = new Oid("1.2.840.113554.1.2.2");
		Oid[] expectedMechs = new Oid[]{kerberosMech};
		assertTrue(Arrays.equals(expectedMechs, mechs));
	}
	
	public void testGetMechsForName() throws Exception {
		Oid nameType = GSSName.NT_ANONYMOUS;
		Oid[] mechs = gssManager.getMechsForName(nameType);
		assertEquals(0, mechs.length);

		nameType = GSSName.NT_MACHINE_UID_NAME;
		mechs = gssManager.getMechsForName(nameType);
		assertEquals(0, mechs.length);

		nameType = GSSName.NT_STRING_UID_NAME;
		mechs = gssManager.getMechsForName(nameType);
		assertEquals(0, mechs.length);

		nameType = GSSName.NT_USER_NAME;
		mechs = gssManager.getMechsForName(nameType);
		Oid kerberosMech = new Oid("1.2.840.113554.1.2.2");
		Oid[] expectedMechs = new Oid[] { kerberosMech };
		assertTrue(Arrays.equals(expectedMechs, mechs));

		nameType = GSSName.NT_HOSTBASED_SERVICE;
		mechs = gssManager.getMechsForName(nameType);
		assertTrue(Arrays.equals(expectedMechs, mechs));

		nameType = GSSName.NT_EXPORT_NAME;
		mechs = gssManager.getMechsForName(nameType);
		assertTrue(Arrays.equals(expectedMechs, mechs));

		nameType = KerberosUtils.KRB5_PRINCIPAL_NAMETYPE;
		mechs = gssManager.getMechsForName(nameType);
		assertTrue(Arrays.equals(expectedMechs, mechs));
	}
	
	public void testGetNamesForMech() throws Exception {
		Oid kerberosMech = new Oid("1.2.840.113554.1.2.2");
		Oid[] nameTypes = gssManager.getNamesForMech(kerberosMech);
		Oid[] expectedNameTypes = new Oid[] { GSSName.NT_USER_NAME,
				GSSName.NT_HOSTBASED_SERVICE, GSSName.NT_EXPORT_NAME,
				KerberosUtils.KRB5_PRINCIPAL_NAMETYPE };
		assertEquals(expectedNameTypes.length, nameTypes.length);
		for (Oid expectedNameType : expectedNameTypes) {
			boolean got = false;
			for (Oid nameType : nameTypes) {
				if (nameType.equals(expectedNameType)) {
					got = true;
					break;
				}
			}
			if (!got) {
				fail("Missing expected NameType " + expectedNameType);
			}
		}
	}
	
	public void testCreateName() throws Exception {

		GSSName gssName = gssManager.createName("username",
				GSSName.NT_USER_NAME);
		assertEquals(GSSName.NT_USER_NAME, gssName.getStringNameType());

		gssName = gssManager.createName("service@host",
				GSSName.NT_HOSTBASED_SERVICE);
		assertEquals(GSSName.NT_HOSTBASED_SERVICE, gssName.getStringNameType());

		final Oid kerberosPrincipalOid = new Oid("1.2.840.113554.1.2.2.1");
		gssName = gssManager.createName("kerberosPrincipal",
				kerberosPrincipalOid);
		assertEquals(kerberosPrincipalOid, gssName.getStringNameType());

		byte[] encoded = new byte[] { 4, 1, 0, 11, 6, 9, 42, -122, 72, -122,
				-9, 18, 1, 2, 2, 0, 0, 0, 17, 115, 101, 114, 118, 105, 99, 101,
				47, 108, 111, 99, 97, 108, 104, 111, 115, 116 };
		gssName = gssManager.createName(encoded, GSSName.NT_EXPORT_NAME);
		assertEquals(kerberosPrincipalOid, gssName.getStringNameType());
		GSSName expectedGSSName = gssManager.createName("service/localhost", kerberosPrincipalOid);		
		assertEquals(expectedGSSName, gssName);		
	}
	
	public void setUp() throws Exception{		
		gssManager = GSSManager.getInstance();		
	}

}

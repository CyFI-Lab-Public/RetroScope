/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.api.javax.security.auth.x500;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

public class X500PrincipalTest extends junit.framework.TestCase {

	/**
	 * @tests javax.security.auth.x500.X500Principal#X500Principal(java.lang.String)
	 */
	public void test_ConstructorLjava_lang_String() {
		X500Principal principal = new X500Principal(
				"CN=Hermione Granger, O=Apache Software Foundation, OU=Harmony, L=Hogwarts, ST=Hants, C=GB");
		String name = principal.getName();
		String expectedOuput = "CN=Hermione Granger,O=Apache Software Foundation,OU=Harmony,L=Hogwarts,ST=Hants,C=GB";
		assertEquals("Output order precedence problem", expectedOuput, name);
	}
    
    /**
     * @tests javax.security.auth.x500.X500Principal#X500Principal(java.lang.String, java.util.Map)
     */
    public void test_ConstructorLjava_lang_String_java_util_Map(){
        Map<String, String> keyword = new HashMap<String, String>();
        keyword.put("CN", "2.19");
        keyword.put("OU", "1.2.5.19");
        keyword.put("O", "1.2.5");
        X500Principal X500p = new X500Principal("CN=Duke, OU=JavaSoft, O=Sun Microsystems, C=US ,CN=DD",keyword);
        String name = X500p.getName();
        String expectedOut = "2.19=#130444756b65,1.2.5.19=#13084a617661536f6674,1.2.5=#131053756e204d6963726f73797374656d73,C=US,2.19=#13024444";
        assertEquals("Output order precedence problem", expectedOut, name);
    }

	/**
	 * @tests javax.security.auth.x500.X500Principal#getName(java.lang.String)
	 */
	public void test_getNameLjava_lang_String() {
		X500Principal principal = new X500Principal(
				"CN=Dumbledore, OU=Administration, O=Hogwarts School, C=GB");
		String canonical = principal.getName(X500Principal.CANONICAL);
		String expected = "cn=dumbledore,ou=administration,o=hogwarts school,c=gb";
		assertEquals("CANONICAL output differs from expected result", expected,
				canonical);
	}
    
    /**
     * @tests javax.security.auth.x500.X500Principal#getName(java.lang.String, java.util.Map)
     */
    public void test_getNameLjava_lang_String_java_util_Map() {
        Map<String, String> keyword = new HashMap<String, String>();
        keyword.put("CN", "2.19");
        keyword.put("OU", "1.2.5.19");
        keyword.put("O", "1.2.5");
        X500Principal X500p = new X500Principal("CN=Duke, OU=JavaSoft, O=Sun Microsystems, C=US ,CN=DD",keyword);
        keyword = new HashMap<String, String>();
        keyword.put("2.19", "mystring");
        String rfc1779Name = X500p.getName("RFC1779",keyword);
        String rfc2253Name = X500p.getName("RFC2253",keyword);
        String expected1779Out = "mystring=Duke, OID.1.2.5.19=JavaSoft, OID.1.2.5=Sun Microsystems, C=US, mystring=DD";
        String expected2253Out = "mystring=Duke,1.2.5.19=#13084a617661536f6674,1.2.5=#131053756e204d6963726f73797374656d73,C=US,mystring=DD";
        assertEquals("Output order precedence problem", expected1779Out, rfc1779Name);
        assertEquals("Output order precedence problem", expected2253Out, rfc2253Name);
        try{
            X500p.getName("CANONICAL",keyword);
            fail("Should throw IllegalArgumentException exception here");
        }
        catch(IllegalArgumentException e){
            //expected IllegalArgumentException here
        }
    }
}
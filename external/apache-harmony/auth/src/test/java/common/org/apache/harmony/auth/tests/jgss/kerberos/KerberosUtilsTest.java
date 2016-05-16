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

import java.util.Date;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;

import org.apache.harmony.auth.jgss.kerberos.KerberosUtils;
import org.apache.harmony.auth.jgss.kerberos.toolbox.KerberosToolboxImpl;
import org.apache.harmony.auth.jgss.kerberos.toolbox.KerberosToolboxSpi;

import junit.framework.TestCase;

public class KerberosUtilsTest extends TestCase {

    public void testGetKerberosToolBox() throws Exception {
        KerberosToolboxSpi kerberosToolBoxSpi = KerberosUtils
                .getKerberosToolbox("TESTKDCNAME");
        assertTrue(kerberosToolBoxSpi instanceof KerberosToolboxImpl);
    }

    public void testGetTGT_fromContext() throws Exception {
        final KerberosPrincipal clientPrincipal = new KerberosPrincipal(
                "leo@EXAMPLE.COM");
        final KerberosPrincipal serverPrincipal = new KerberosPrincipal(
                "krbtgt/EXAMPLE.COM@EXAMPLE.COM");
        KerberosTicket tgt = new KerberosTicket(new byte[0], clientPrincipal,
                serverPrincipal, new byte[0], 1, new boolean[0],
                new Date(1000), null, new Date(new Date().getTime() + 1000), null, null);        
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(tgt);
        KerberosTicket tgtFromContext = (KerberosTicket) Subject.doAs(subject, new PrivilegedAction<KerberosTicket>(){
            public KerberosTicket run(){
                return KerberosUtils.getTGT(clientPrincipal);
            }
        });
        assertNotNull(tgtFromContext);
        assertEquals(tgt, tgtFromContext);
    }
    
    public void testGetTGT_fromLoginContext() throws Exception {
        final KerberosPrincipal clientPrincipal = new KerberosPrincipal(
                "leo@EXAMPLE.COM");
        KerberosTicket tgt = KerberosUtils.getTGT(clientPrincipal);
        assertNull(tgt);
    }
}

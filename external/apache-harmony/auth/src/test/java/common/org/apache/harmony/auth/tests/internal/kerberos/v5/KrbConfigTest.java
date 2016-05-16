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

package org.apache.harmony.auth.tests.internal.kerberos.v5;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.harmony.auth.internal.kerberos.v5.KrbConfig;

import tests.support.resource.Support_Resources;

public class KrbConfigTest extends TestCase {

    public void test_Ctor() throws IOException {

        File f = new File(Support_Resources
                .getAbsoluteResourcePath("KrbConfigTest.txt"));

        KrbConfig config = new KrbConfig(f);

        assertEquals("MY.REALM", config
                .getValue("libdefaults", "default_realm"));
        assertEquals("true", config.getValue("libdefaults", "dns_lookup_kdc"));

        assertEquals("SYSLOG:INFO", config.getValue("logging", "default"));
        assertEquals("FILE:/var/kdc.log", config.getValue("logging", "kdc"));
    }
}

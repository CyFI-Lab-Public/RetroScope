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

package org.apache.harmony.auth.tests.javax.security.auth.kerberos.serialization;

import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class KerberosPrincipalTest extends TestCase {

    private static final KerberosPrincipal[] data = new KerberosPrincipal[] {
            new KerberosPrincipal("aaa@somehost.net"),
            new KerberosPrincipal("aaa/bbb/ccc/ddd/eee@anotherHost.org",
                    KerberosPrincipal.KRB_NT_SRV_INST) };

    /**
     * @tests serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {
        SerializationTest.verifySelf(data);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {
        SerializationTest.verifyGolden(this, data);
    }
}

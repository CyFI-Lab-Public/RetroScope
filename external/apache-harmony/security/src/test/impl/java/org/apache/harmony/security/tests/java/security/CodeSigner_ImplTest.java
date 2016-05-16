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
* @author Alexander V. Astapchuk
*/

package org.apache.harmony.security.tests.java.security;

import java.security.CodeSigner;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.util.Date;

import org.apache.harmony.security.tests.support.TestCertUtils;
import junit.framework.TestCase;

/**
 * Unit test for CodeSigner. 
 */

public class CodeSigner_ImplTest extends TestCase {

    private CertPath cpath = TestCertUtils.genCertPath(3, 0);
    private Date now = new Date();

    private Timestamp ts = new Timestamp(now, cpath);

    /**
     * Tests CodeSigner.hashCode()
     */
    public void testHashCode() {
        assertTrue(new CodeSigner(cpath, ts).hashCode() == (cpath.hashCode() ^ ts
                .hashCode()));
        assertTrue(new CodeSigner(cpath, null).hashCode() == cpath.hashCode());
    }

}

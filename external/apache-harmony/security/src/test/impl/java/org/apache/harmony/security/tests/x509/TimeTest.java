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

package org.apache.harmony.security.tests.x509;

import java.util.Date;

import junit.framework.TestCase;

import org.apache.harmony.security.asn1.ASN1Constants;
import org.apache.harmony.security.x509.Time;

/**
 * Time test
 */
public class TimeTest extends TestCase {

    /**
     * Tests the result of encoding work on the data before and after 2050.
     */
    public void test_Encoding() throws Exception {

        long march2006 = 1143115180000L;
        long march2332 = 11431151800000L;

        // verify that date before 2050 encoded as UTCTime
        byte[] enc = Time.ASN1.encode(new Date(march2006));
        assertEquals("UTCTime", ASN1Constants.TAG_UTCTIME, enc[0]);

        // verify that date after 2050 encoded as GeneralizedTime
        enc = Time.ASN1.encode(new Date(march2332));
        assertEquals("GeneralizedTime", ASN1Constants.TAG_GENERALIZEDTIME,
                enc[0]);
    }

}

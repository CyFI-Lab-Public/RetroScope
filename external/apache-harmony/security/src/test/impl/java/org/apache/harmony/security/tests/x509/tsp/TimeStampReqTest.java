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

package org.apache.harmony.security.tests.x509.tsp;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.tsp.MessageImprint;
import org.apache.harmony.security.x509.tsp.TimeStampReq;

public class TimeStampReqTest extends TestCase {

    /**
     * @throws IOException 
     * @tests 'org.apache.harmony.security.x509.tsp.TimeStampReq.getEncoded()'
     */
    public void testTimeStampReq() throws IOException {
        // SHA1 OID
        MessageImprint msgImprint = new MessageImprint(new AlgorithmIdentifier(
                "1.3.14.3.2.26"), new byte[20]);
        String reqPolicy = "1.2.3.4.5";
        BigInteger nonce = BigInteger.valueOf(1234567890L); 
        Extensions exts = new Extensions();
        int[] extOID = new int[] { 1, 2, 3, 2, 1 };
        byte[] extValue = new byte[] { (byte) 1, (byte) 2, (byte) 3 };
        Extension ext = new Extension(extOID, false, extValue);
        exts.addExtension(ext);

        TimeStampReq req = new TimeStampReq(1, msgImprint, reqPolicy,
                nonce, Boolean.FALSE, exts);
        byte[] encoding = req.getEncoded();
        TimeStampReq decoded = (TimeStampReq) TimeStampReq.ASN1
                .decode(encoding);
        assertEquals("Decoded version is incorrect", req.getVersion(), decoded
                .getVersion());
        assertTrue("Decoded messageImprint is incorrect", Arrays.equals(
                MessageImprint.ASN1.encode(msgImprint), MessageImprint.ASN1
                        .encode(decoded.getMessageImprint())));
        assertEquals("Decoded reqPolicy is incorrect", reqPolicy, decoded
                .getReqPolicy());
        assertEquals("Decoded nonce is incorrect", nonce, decoded.getNonce());
        assertFalse("Decoded certReq is incorrect", decoded.getCertReq()
                .booleanValue());
        assertEquals("Decoded extensions is incorrect", exts, decoded
                .getExtensions());
    }
}


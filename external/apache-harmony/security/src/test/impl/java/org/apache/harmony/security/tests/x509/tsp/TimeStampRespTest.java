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
import java.util.Collections;
import java.util.Date;

import junit.framework.TestCase;

import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.pkcs7.ContentInfo;
import org.apache.harmony.security.pkcs7.SignedData;
import org.apache.harmony.security.pkcs7.SignerInfo;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.Extension;
import org.apache.harmony.security.x509.Extensions;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.tsp.MessageImprint;
import org.apache.harmony.security.x509.tsp.PKIFailureInfo;
import org.apache.harmony.security.x509.tsp.PKIStatus;
import org.apache.harmony.security.x509.tsp.PKIStatusInfo;
import org.apache.harmony.security.x509.tsp.TSTInfo;
import org.apache.harmony.security.x509.tsp.TimeStampResp;

public class TimeStampRespTest extends TestCase {

    /**
     * @throws IOException 
     * @tests 'org.apache.harmony.security.x509.tsp.TimeStampResp.getEncoded()'
     */
    public void testGetEncoded() throws IOException {
        String statusString = "statusString";
        PKIStatusInfo status = new PKIStatusInfo(PKIStatus.REJECTION,
                Collections.singletonList(statusString),
                PKIFailureInfo.BAD_REQUEST);
        
        
        // creating TimeStampToken
        String policy = "1.2.3.4.5";
        String sha1 = "1.3.14.3.2.26";
        MessageImprint msgImprint = new MessageImprint(new AlgorithmIdentifier(
                sha1), new byte[20]);
        Date genTime = new Date();
        BigInteger nonce = BigInteger.valueOf(1234567890L);
        // accuracy is 1 second
        int[] accuracy = new int[] { 1, 0, 0 };
        GeneralName tsa = new GeneralName(new Name("CN=AnAuthority"));
        Extensions exts = new Extensions();
        // Time-Stamping extension OID: as defined in RFC 3161
        int[] timeStampingExtOID = new int[] { 1, 3, 6, 1, 5, 5, 7, 3, 8 };
        byte[] timeStampingExtValue = new byte[] { (byte) 1, (byte) 2, (byte) 3 };
        Extension ext = new Extension(timeStampingExtOID, true,
                timeStampingExtValue);
        exts.addExtension(ext);

        TSTInfo tSTInfo = new TSTInfo(1, policy, msgImprint, BigInteger.TEN,
                genTime, accuracy, Boolean.FALSE, nonce, tsa, exts);
        
        Object[] issuerAndSerialNumber = new Object[] { new Name("CN=issuer"),
                ASN1Integer.fromIntValue(12345) };
        // SHA1withDSA OID
        String sha1dsa = "1.2.840.10040.4.3";
        SignerInfo sigInfo = new SignerInfo(1, issuerAndSerialNumber,
                new AlgorithmIdentifier(sha1), null, new AlgorithmIdentifier(
                        sha1dsa), new byte[20], null);
        // TSTInfo OID according to RFC 3161
        int[] tSTInfoOid = new int[] { 1, 2, 840, 113549, 1, 9, 16, 1, 4 };
        ContentInfo tSTInfoEncoded = new ContentInfo(tSTInfoOid,
                ASN1OctetString.getInstance().encode(
                        TSTInfo.ASN1.encode(tSTInfo)));
        SignedData tokenContent = new SignedData(1, Collections
                .singletonList(new AlgorithmIdentifier(sha1)), tSTInfoEncoded,
                null, null, Collections.singletonList(sigInfo));
        ContentInfo timeStampToken = new ContentInfo(ContentInfo.SIGNED_DATA,
                tokenContent);
        
        TimeStampResp response = new TimeStampResp(status, timeStampToken);
        
        byte [] encoding = TimeStampResp.ASN1.encode(response);
        TimeStampResp decoded = (TimeStampResp) TimeStampResp.ASN1
                .decode(encoding);

        // deeper checks are performed in the corresponding unit tests
        assertTrue("Decoded status is incorrect", Arrays.equals(
                PKIStatusInfo.ASN1.encode(status), PKIStatusInfo.ASN1
                        .encode(decoded.getStatus())));
        assertTrue("Decoded timeStampToken is incorrect", Arrays.equals(
                timeStampToken.getEncoded(), decoded.getTimeStampToken()
                        .getEncoded()));
    }
}


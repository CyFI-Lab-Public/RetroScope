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
import java.util.ArrayList;
import java.util.List;

import org.apache.harmony.security.x509.tsp.PKIFailureInfo;
import org.apache.harmony.security.x509.tsp.PKIStatus;
import org.apache.harmony.security.x509.tsp.PKIStatusInfo;

import junit.framework.TestCase;

public class PKIStatusInfoTest extends TestCase {

    /**
     * @throws IOException 
     * @tests 'org.apache.harmony.security.x509.tsp.PKIStatusInfo.getEncoded()'
     */
    public void testGetEncoded() throws IOException {
        ArrayList statusStr = new ArrayList(2);
        statusStr.add("one");
        statusStr.add("two");
        PKIStatusInfo info = new PKIStatusInfo(PKIStatus.REJECTION, statusStr,
                PKIFailureInfo.BAD_DATA_FORMAT);
        byte [] encoding = PKIStatusInfo.ASN1.encode(info);
        PKIStatusInfo decoded = (PKIStatusInfo) PKIStatusInfo.ASN1
                .decode(encoding);
        
        assertEquals(info.getStatus(), decoded.getStatus());
        List decodedStString = decoded.getStatusString();
        assertNotNull(decodedStString);
        assertEquals("one", decodedStString.get(0));
        assertEquals("two", decodedStString.get(1));
        assertEquals(info.getFailInfo(), decoded.getFailInfo());
    }
}


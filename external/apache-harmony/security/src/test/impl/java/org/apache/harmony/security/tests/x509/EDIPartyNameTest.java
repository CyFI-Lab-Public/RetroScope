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
* @author Alexander Y. Kleymenov
*/

package org.apache.harmony.security.tests.x509;

import java.util.Arrays;

import org.apache.harmony.security.x509.EDIPartyName;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 */

public class EDIPartyNameTest extends TestCase {

    public static void printAsHex(int perLine, String prefix,
                                  String delimiter, byte[] data) {
        for (int i=0; i<data.length; i++) {
            String tail = Integer.toHexString(0x000000ff & data[i]);
            if (tail.length() == 1) {
                tail = "0" + tail;
            }
            System.out.print(prefix + "0x" + tail + delimiter);

            if (((i+1)%perLine) == 0) {
                System.out.println();
            }
        }
        System.out.println();
    }

    /**
     * EDIPartyName(String nameAssigner, String partyName) method testing.
     */
    public void _testEDIPartyName1() {
        boolean pass = true;

        EDIPartyName ediPN = new EDIPartyName("nameAssigner", "partyName");
        byte[] encoded = ediPN.getEncoded();
        // manually derived data:
        byte[] _encoded = {
            (byte) 0x30, (byte) 0x1d, (byte) 0x80, (byte) 0x0e,
            (byte) 0x13, (byte) 0x0c, (byte) 0x6e, (byte) 0x61,
            (byte) 0x6d, (byte) 0x65, (byte) 0x41, (byte) 0x73,
            (byte) 0x73, (byte) 0x69, (byte) 0x67, (byte) 0x6e,
            (byte) 0x65, (byte) 0x72, (byte) 0x81, (byte) 0x0b,
            (byte) 0x13, (byte) 0x09, (byte) 0x70, (byte) 0x61,
            (byte) 0x72, (byte) 0x74, (byte) 0x79, (byte) 0x4e,
            (byte) 0x61, (byte) 0x6d, (byte) 0x65
        };
        if (!Arrays.equals(encoded, _encoded)) {
            System.out.println("Got encoded form of EDIPartyName is:");
            printAsHex(16, "", " ", encoded);
            System.out.println("But should be like this:");
            printAsHex(16, "", " ", _encoded);
            System.out.println("");
            pass = false;
        }

        GeneralName gName = new GeneralName(ediPN);
        encoded = gName.getEncoded();
        // manually derived data:
        _encoded = new byte[] {
            (byte) 0xa5, (byte) 0x1d, (byte) 0x80, (byte) 0x0e,
            (byte) 0x13, (byte) 0x0c, (byte) 0x6e, (byte) 0x61,
            (byte) 0x6d, (byte) 0x65, (byte) 0x41, (byte) 0x73,
            (byte) 0x73, (byte) 0x69, (byte) 0x67, (byte) 0x6e,
            (byte) 0x65, (byte) 0x72, (byte) 0x81, (byte) 0x0b,
            (byte) 0x13, (byte) 0x09, (byte) 0x70, (byte) 0x61,
            (byte) 0x72, (byte) 0x74, (byte) 0x79, (byte) 0x4e,
            (byte) 0x61, (byte) 0x6d, (byte) 0x65
        };
        if (!Arrays.equals(encoded, _encoded)) {
            System.out.println("Got encoded form of GeneralName is:");
            printAsHex(16, "", " ", encoded);
            System.out.println("But should be like this:");
            printAsHex(16, "", " ", _encoded);
            System.out.println("");
            pass = false;
        }

        GeneralNames gNames = new GeneralNames();
        gNames.addName(gName);
        encoded = gNames.getEncoded();
        // manually derived data:
        _encoded = new byte[] {
            (byte) 0x30, (byte) 0x1f, (byte) 0xa5, (byte) 0x1d,
            (byte) 0x80, (byte) 0x0e, (byte) 0x13, (byte) 0x0c,
            (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65,
            (byte) 0x41, (byte) 0x73, (byte) 0x73, (byte) 0x69,
            (byte) 0x67, (byte) 0x6e, (byte) 0x65, (byte) 0x72,
            (byte) 0x81, (byte) 0x0b, (byte) 0x13, (byte) 0x09,
            (byte) 0x70, (byte) 0x61, (byte) 0x72, (byte) 0x74,
            (byte) 0x79, (byte) 0x4e, (byte) 0x61, (byte) 0x6d,
            (byte) 0x65
        };
        if (!Arrays.equals(encoded, _encoded)) {
            System.out.println("Got encoded form of GeneralNames is:");
            printAsHex(16, "", " ", encoded);
            System.out.println("But should be like this:");
            printAsHex(16, "", " ", _encoded);
            System.out.println("");
            pass = false;
        }

        assertTrue("Some problems occured.", pass);
    }

    /**
     * EDIPartyName(String nameAssigner, String partyName, byte[] encoding)
     * method testing.
     */
    public void testEDIPartyName2() throws Exception {
        EDIPartyName ediName = new EDIPartyName("assigner", "party");
        byte[] encoding = EDIPartyName.ASN1.encode(ediName);
        EDIPartyName.ASN1.decode(encoding);
        new GeneralName(5, encoding);

        GeneralName gn = new GeneralName(ediName);

        new GeneralName(5, gn.getEncodedName());
    }

    /**
     * getNameAssigner() method testing.
     */
    public void testGetNameAssigner() {
    }

    /**
     * getPartyName() method testing.
     */
    public void testGetPartyName() {
    }

    /**
     * getEncoded() method testing.
     */
    public void testGetEncoded() {
    }

    /**
     * getEncoder() method testing.
     */
    public void testGetEncoder() {
    }

    /**
     * DerEncoder(String nameAssigner, String partyName) method testing.
     */
    public void testDerEncoder() {
    }

    /**
     * getDirStrAlternatives() method testing.
     */
    public void testGetDirStrAlternatives() {
    }

    /**
     * DerDecoder() method testing.
     */
    public void testDerDecoder() {
    }

    /**
     * getValue(byte[] encoding) method testing.
     */
    public void testGetValue1() {
    }

    /**
     * getValue() method testing.
     */
    public void testGetValue2() {
    }

    public static Test suite() {
        return new TestSuite(EDIPartyNameTest.class);
    }

}

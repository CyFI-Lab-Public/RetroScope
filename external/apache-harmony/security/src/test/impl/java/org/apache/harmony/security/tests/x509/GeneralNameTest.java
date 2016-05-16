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

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.EDIPartyName;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.ORAddress;
import org.apache.harmony.security.x509.OtherName;


/**
 * GeneralNameTest
 */
public class GeneralNameTest extends TestCase {

    public void testGeneralName() {
        try {
            GeneralName san0 =
                new GeneralName(new OtherName("1.2.3.4.5", new byte[] {1, 2, 0, 1}));
            GeneralName san1 = new GeneralName(1, "rfc@822.Name");
            GeneralName san2 = new GeneralName(2, "dNSName");
            GeneralName san3 = new GeneralName(new ORAddress());
            GeneralName san4 = new GeneralName(new Name("O=Organization"));
            GeneralName san5 =
                new GeneralName(new EDIPartyName("assigner", "party"));
            GeneralName san6 = new GeneralName(6, "http://uniform.Resource.Id");
            GeneralName san7 = new GeneralName(7, "1.1.1.1");
            GeneralName san8 = new GeneralName(8, "1.2.3.4444.55555");

            GeneralNames sans_1 = new GeneralNames();
            sans_1.addName(san0);
            sans_1.addName(san1);
            sans_1.addName(san2);
            sans_1.addName(san3);
            sans_1.addName(san4);
            sans_1.addName(san5);
            sans_1.addName(san6);
            sans_1.addName(san7);
            sans_1.addName(san8);

            byte[] encoding =  GeneralNames.ASN1.encode(sans_1);
            GeneralNames.ASN1.decode(encoding);
        } catch (Exception e) {
            // should not be thrown:
            // provided string representations are correct
            e.printStackTrace();
        }
    }

    public void testGeneralName1() throws Exception {
        OtherName on =
            new OtherName("1.2.3.4.5", new byte[] {1, 2, 0, 1});
        byte[] encoding = OtherName.ASN1.encode(on);
        new GeneralName(0, encoding);
        OtherName.ASN1.decode(encoding);
        GeneralName gn = new GeneralName(on);
        new GeneralName(0, gn.getEncodedName());
        assertEquals(gn, new GeneralName(0, gn.getEncodedName()));
    }

    /**
     * ipStrToBytes method testing.
     */
    public void testIpStrToBytes() throws Exception {
        // Regression for HARMONY-727
        Object[][] positives = {
            {"010a:020b:3337:1000:FFFA:ABCD:9999:0000",
            new int[] {0x01, 0x0a, 0x02, 0x0b, 0x33, 0x37, 0x10, 0x00, 0xFF,
                0xFA, 0xAB, 0xCD, 0x99, 0x99, 0x00, 0x00}},
            {"010a:020b:3337:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            new int[] {0x01, 0x0a, 0x02, 0x0b, 0x33, 0x37, 0x10, 0x00, 0xFF,
                0xFA, 0xAB, 0xCD, 0x99, 0x99, 0x00, 0x00, 0x01, 0x02, 0x03,
                0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0b, 0x0c, 0x0D,
                0x0e, 0x0f, 0x10}},
            {"010a:020b:1133:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            new int[] {0x01, 0x0a, 0x02, 0x0b, 0x11, 0x33, 0x10, 0x00, 0xFF,
                0xFA, 0xAB, 0xCD, 0x99, 0x99, 0x00, 0x00, 0x01, 0x02, 0x03,
                0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0b, 0x0c, 0x0D,
                0x0e, 0x0f, 0x10}},
            {"010a:020b:1133:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            new int[] {0x01, 0x0a, 0x02, 0x0b, 0x11, 0x33, 0x10, 0x00, 0xFF,
                0xFA, 0xAB, 0xCD, 0x99, 0x99, 0x00, 0x00, 0x01, 0x02, 0x03,
                0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0b, 0x0c, 0x0D,
                0x0e, 0x0f, 0x10}},
            {"100.2.35.244",
            new int[] {100, 2, 35, 244}},
            {"100.2.35.244/51.6.79.118",
            new int[] {100, 2, 35, 244, 51, 6, 79, 118}},
        };
        String[] negatives = {
            "010a:0000:3333:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0",
            "010a:020b:3:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            "010a:020b:33:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            "010a:020b:333:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            "010a:020b:1133:10V0:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            "010a:020b:1133:1000-FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            "010a:020b:1133:1000:FFFA:ABCD:9999",
            "010a:020b:1133:1000:FFFA:ABCD:9999/0000:0102:0304:0506:0708:090A:0b0c:0D0e:0f10",
            "010a:020b:1133:1000:FFFA:ABCD:9999:0000:0102/0304:0506:0708:090A:0b0c:0D0e:0f10",
            "010a:020b:1133:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e:0f10:1234",
            "100.2.35.244/51.6.79.118.119",
            "100.2.35.244.115/79.118.119",
            "100.2.35.244/79.118.119.1167",
            "100.2.35.244/79.118.119.116.7",
            "100.2.35.244.79/118.119.116.7",
            "100.2.35/79/118.119.116.7",
            "100.2.35.79/118/119.116.7",
            "100.2..35.79/118/119.116.7",
            "100.2.a.35.79/118/119.116.7",
            "100.2.35.79/119.116.7-1",
            "100.2.35.244.111",
            "100.2.35.244/111",
            "010a:020b:1133:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e0f10",
            "010a:020b:1133:1000:FFFA:ABCD:9999:0000/0102:0304:0506:0708:090A:0b0c:0D0e0f:10",
            "010a:020b:1133:1000:FFFA:ABCD:9999:0000/0102/0304:0506:0708:090A:0b0c:0D0e0f:10",
            "010a:020b:1133:1000:FFFA:ABCD:9999:0000/0102030405060708090A0b0c:0D0e:0f10:ffff",
            "010a:020b:1133:1000:FFFA:ABCD:9999:00000102030405060708090A/0b0c:0D0e:0f10:ffff",
        };
        for (int i=0; i<positives.length; i++) {
            byte[] res = GeneralName.ipStrToBytes((String)positives[i][0]);
            int[] ref = (int[])positives[i][1];
            assertEquals("Length differs for "+positives[i][0], ref.length, res.length);
            for (int j=0; j<res.length; j++) {
                assertEquals("Element differs for "+positives[i][0], (byte)ref[j], res[j]);
            }
        }
        for (int n=0; n<negatives.length; n++) {
            String ip = negatives[n];
            try {
                byte[] bts = GeneralName.ipStrToBytes(ip);
                for (int i=0; i<bts.length; i++) {
                    System.out.print((bts[i]&0xFF)+" ");
                }
                System.out.println("");
                System.out.println(ip);
                fail("No expected IOException was thrown for " + n);
            } catch (IOException e) {
                // expected
            }
        }
    }

    /**
     * oidStrToInts method testing
     */
    public void testOidStrToInts() throws Exception {
        // Regression for HARMONY-727
        Object[][] positives = {
                { "1.2", new int[] { 1, 2 } },
                { "1.2.3.4.5", new int[] { 1, 2, 3, 4, 5 } },
                { "123.456.7890.1234567890",
                        new int[] { 123, 456, 7890, 1234567890 } }, };
        String[] negatives = { ".1.2", "1.2.", "11-22.44.22", "111..222" };
        for (int i = 0; i < positives.length; i++) {
            int[] res = GeneralName.oidStrToInts((String) positives[i][0]);
            int[] ref = (int[]) positives[i][1];
            assertEquals("Length differs for " + positives[i][0], ref.length,
                    res.length);
            for (int j = 0; j < res.length; j++) {
                if (res[j] != ref[j]) {
                    assertEquals("Element differs for " + positives[i][0],
                            (byte) ref[j], res[j]);
                }
            }
        }
        for (int i = 0; i < negatives.length; i++) {
            try {
                GeneralName.oidStrToInts(negatives[i]);
                fail("Expected IOException was not thrown for " + negatives[i]);
            } catch (IOException e) {
                // expected
            }
        }
    }
}

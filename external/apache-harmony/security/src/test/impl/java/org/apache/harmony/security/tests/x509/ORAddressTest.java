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

import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.ORAddress;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * ORAddressTest
 */
public class ORAddressTest extends TestCase {

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
     * ORAddress() method testing.
     */
    public void testORAddress() {
        ORAddress ora = new ORAddress();
        System.out.println("");
        System.out.println("ORAddress:");
        printAsHex(8, "", " ", ora.getEncoded());
        System.out.println("");

        GeneralName gName = new GeneralName(ora);
        System.out.println("GeneralName:");
        printAsHex(8, "", " ", gName.getEncoded());
        System.out.println("");

        GeneralNames gNames = new GeneralNames();
        gNames.addName(gName);
        System.out.println("GeneralNames:");
        printAsHex(8, "", " ", gNames.getEncoded());
        System.out.println("");
    }

    public static Test suite() {
        return new TestSuite(ORAddressTest.class);
    }

}

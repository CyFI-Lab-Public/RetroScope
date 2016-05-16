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
* @author Vladimir N. Molotkov
*/

package org.apache.harmony.crypto.tests.javax.crypto;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;

import javax.crypto.EncryptedPrivateKeyInfo;

import org.apache.harmony.crypto.tests.support.EncryptedPrivateKeyInfoData;

import junit.framework.TestCase;

/**
 * Test for EncryptedPrivateKeyInfo class.
 * 
 * All binary data for this test were generated using
 * BEA JRockit j2sdk1.4.2_04 (http://www.bea.com) with
 * security providers list extended by Bouncy Castle's one
 * (http://www.bouncycastle.org)
 */
public class EncryptedPrivateKeyInfo_ImplTest extends TestCase {

    /**
     * Test #1 for <code>getAlgName()</code> method <br>
     * Assertion: Returns the encryption algorithm name <br>
     * Test preconditions: test object created using ctor which takes encoded
     * form as the only parameter <br>
     * Expected: corresponding algorithm name must be returned
     * 
     * @throws IOException
     */
    public final void testGetAlgName01() throws IOException {
        boolean performed = false;
        for (int i = 0; i < EncryptedPrivateKeyInfoData.algName0.length; i++) {
            try {
                EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(
                        EncryptedPrivateKeyInfoData
                                .getValidEncryptedPrivateKeyInfoEncoding(
                                        EncryptedPrivateKeyInfoData.algName0[i][0]));
                assertEquals(EncryptedPrivateKeyInfoData.algName0[i][1], epki
                        .getAlgName());
                performed = true;
            } catch (NoSuchAlgorithmException allowed) {
            }
        }
        assertTrue("Test not performed", performed);
    }

    /**
     * Test #2 for <code>getAlgName()</code> method <br>
     * Assertion: Returns the encryption algorithm name <br>
     * Test preconditions: test object created using ctor which takes algorithm
     * name and encrypted data as a parameters <br>
     * Expected: corresponding algorithm name must be returned
     * 
     * @throws IOException
     */
    public final void testGetAlgName02() {
        boolean performed = false;
        for (int i = 0; i < EncryptedPrivateKeyInfoData.algName0.length; i++) {
            try {
                EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(
                        EncryptedPrivateKeyInfoData.algName0[i][0],
                        EncryptedPrivateKeyInfoData.encryptedData);
                assertEquals(EncryptedPrivateKeyInfoData.algName0[i][1], epki
                        .getAlgName());
                performed = true;
            } catch (NoSuchAlgorithmException allowedFailure) {
            }
        }
        assertTrue("Test not performed", performed);
    }

    /**
     * Test #3 for <code>getAlgName()</code> method <br>
     * Assertion: Returns the encryption algorithm name <br>
     * Test preconditions: test object created using ctor which takes
     * AlgorithmParameters and encrypted data as a parameters <br>
     * Expected: corresponding algorithm name must be returned
     * 
     * @throws IOException
     */
    public final void testGetAlgName03() throws IOException {
        boolean performed = false;
        for (int i = 0; i < EncryptedPrivateKeyInfoData.algName0.length; i++) {
            try {
                AlgorithmParameters ap = AlgorithmParameters
                        .getInstance(EncryptedPrivateKeyInfoData.algName0[i][0]);
                // use pregenerated AlgorithmParameters encodings
                ap.init(EncryptedPrivateKeyInfoData.getParametersEncoding(
                        EncryptedPrivateKeyInfoData.algName0[i][0]));
                EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(ap,
                        EncryptedPrivateKeyInfoData.encryptedData);
                assertEquals(EncryptedPrivateKeyInfoData.algName0[i][1], epki
                        .getAlgName());
                performed = true;
            } catch (NoSuchAlgorithmException allowedFailure) {
            }
        }
        assertTrue("Test not performed", performed);
    }
}

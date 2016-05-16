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
import java.util.Arrays;

import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;

import junit.framework.TestCase;

/**
 * TODO Put your class description here
 */

public class GeneralNamesTest extends TestCase {

    public void test_EncodeDecode() throws IOException {

        GeneralNames subj_alt_names = new GeneralNames(Arrays
                .asList(new GeneralName[] { new GeneralName(1, "rfc@822.Name"),
                        new GeneralName(2, "dNSName"),
                        new GeneralName(4, "O=Organization"),
                        new GeneralName(6, "http://uniform.Resource.Id"),
                        new GeneralName(7, "255.255.255.0"),
                        new GeneralName(8, "1.2.3.4444.55555") }));

        byte[] encoding = GeneralNames.ASN1.encode(subj_alt_names);

        GeneralNames gnames = (GeneralNames) GeneralNames.ASN1.decode(encoding);

        assertEquals("Names: ", subj_alt_names.getNames(), gnames.getNames());
    }
}

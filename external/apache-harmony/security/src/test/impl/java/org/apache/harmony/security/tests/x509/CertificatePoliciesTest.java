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

import java.util.Iterator;
import java.util.List;

import org.apache.harmony.security.x509.CertificatePolicies;
import org.apache.harmony.security.x509.PolicyInformation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * CertificatePoliciesTest
 */
public class CertificatePoliciesTest extends TestCase {

    /**
     * CertificatePolicies() method testing.
     */
    public void testCertificatePolicies() throws Exception {
        String[] policies = new String[] {
            "0.0.0.0.0.0",
            "1.1.1.1.1.1",
            "2.2.2.2.2.2"
        };
        CertificatePolicies certificatePolicies =
                                        new CertificatePolicies();
        for (int i=0; i<policies.length; i++) {
            PolicyInformation policyInformation =
                                    new PolicyInformation(policies[i]);
            certificatePolicies.addPolicyInformation(policyInformation);
        }

        byte[] encoding = certificatePolicies.getEncoded();
        List policyInformations = ((CertificatePolicies)
                CertificatePolicies.ASN1.decode(encoding))
                .getPolicyInformations();
        Iterator it = policyInformations.iterator();
        ((PolicyInformation) it.next()).getPolicyIdentifier();
    }

    public static Test suite() {
        return new TestSuite(CertificatePoliciesTest.class);
    }

}

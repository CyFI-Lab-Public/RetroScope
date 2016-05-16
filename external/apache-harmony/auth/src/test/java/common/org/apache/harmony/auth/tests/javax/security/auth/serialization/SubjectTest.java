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
* @author Stepan M. Mishura
*/

package org.apache.harmony.auth.tests.javax.security.auth.serialization;

import javax.security.auth.Subject;

import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Serialization test for Subject class
 */

public class SubjectTest extends SerializationTest {

    @Override
    protected Object[] getData() {

        Subject subject = new Subject();

        return new Object[] { subject, subject.getPrincipals(),
                subject.getPrivateCredentials(), subject.getPublicCredentials() };
    }
}
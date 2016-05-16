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
* @author Alexander V. Astapchuk
*/

package org.apache.harmony.security.tests.java.security.serialization;

import java.io.Serializable;
import java.security.PrivilegedActionException;

import org.apache.harmony.testframework.serialization.SerializationTest;


/**
 * Serialization testing for PrivilegedActionException.
 */

public class PrivilegedActionExceptionTest extends SerializationTest implements
        SerializationTest.SerializableAssert {

    protected Object[] getData() {
        Exception ex = new Exception();
        PrivilegedActionException ex1 = new PrivilegedActionException(ex);
        return new PrivilegedActionException[] {
              new PrivilegedActionException(null),
              new PrivilegedActionException(ex),
              new PrivilegedActionException(ex1)
        };
    }

    public void assertDeserialized(Serializable reference, Serializable otest) {

        // common checks
        THROWABLE_COMPARATOR.assertDeserialized(reference, otest);

        // class specific checks
        PrivilegedActionException ref = (PrivilegedActionException)reference;
        PrivilegedActionException test = (PrivilegedActionException)otest;
        if( ref.getException() == null ) {
            assertNull( test.getException() );
        } else {
            THROWABLE_COMPARATOR.assertDeserialized(ref.getException(), test
                    .getException());
        }
    }


}

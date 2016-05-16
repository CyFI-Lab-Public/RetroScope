/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dot.junit;

import junit.framework.TestCase;

public class DxTestCase extends TestCase {
    
    // omit the "extends TestCase" and uncomment the following methods if you would like to run the tests as rolled-out, separate tests.
    
/*    
    static public void assertEquals(int expected, int actual) {
        if (expected != actual) throw new RuntimeException("AssertionFailedError: not equals");
    }

    static public void assertEquals(long expected, long actual) {
        if (expected != actual) throw new RuntimeException("AssertionFailedError: not equals");
    }

    static public void assertEquals(double expected, double actual, double delta) {
        if(!(Math.abs(expected-actual) <= delta)) throw new RuntimeException("AssertionFailedError: not within delta");
    }
    
    static public void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null)
            return;
        if (expected != null && expected.equals(actual))
            return;
        throw new RuntimeException("AssertionFailedError: not the same");
    }
    
    static public void assertTrue(boolean condition) {
        if (!condition) throw new RuntimeException("AssertionFailedError: condition was false");
    }
    
    static public void assertFalse(boolean condition) {
        if (condition) throw new RuntimeException("AssertionFailedError: condition was true");
    }
    
    static public void assertNotNull(Object object) {
        if (object == null) throw new RuntimeException("AssertionFailedError: object was null");
    }
    
    static public void assertNull(Object object) {
        if (object != null) throw new RuntimeException("AssertionFailedError: object was not null");
    }
    
    static public void fail(String message) {
        throw new RuntimeException("AssertionFailedError msg:"+message);
    }
*/    
    
    
}

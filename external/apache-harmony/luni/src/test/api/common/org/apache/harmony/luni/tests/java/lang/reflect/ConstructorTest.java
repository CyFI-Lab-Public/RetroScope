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

package org.apache.harmony.luni.tests.java.lang.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Vector;

public class ConstructorTest extends junit.framework.TestCase {

    static class ConstructorTestHelper extends Object {
        int cval;

        public ConstructorTestHelper() throws IndexOutOfBoundsException {
            cval = 99;
        }

        public ConstructorTestHelper(Object x) {
        }

        private ConstructorTestHelper(int a) {
        }

        protected ConstructorTestHelper(long a) {
        }

        public int check() {
            return cval;
        }
    }

    /**
     * @tests java.lang.reflect.Constructor#equals(java.lang.Object)
     */
    public void test_equalsLjava_lang_Object() throws Exception {
        Class[] types = null;
        Constructor ctor1 = null, ctor2 = null;
        ctor1 = new ConstructorTestHelper().getClass().getConstructor(
                new Class[0]);

        Class[] parms = null;
        parms = new Class[1];
        parms[0] = new Object().getClass();
        ctor2 = new ConstructorTestHelper().getClass().getConstructor(parms);

        assertTrue("Different Constructors returned equal", !ctor1
                .equals(ctor2));
    }

    /**
     * @tests java.lang.reflect.Constructor#getDeclaringClass()
     */
    public void test_getDeclaringClass() throws Exception {
        // Test for method java.lang.Class
        // java.lang.reflect.Constructor.getDeclaringClass()
        boolean val = false;
        Class pclass = new ConstructorTestHelper().getClass();
        Constructor ctor = pclass.getConstructor(new Class[0]);
        val = ctor.getDeclaringClass().equals(pclass);

        assertTrue("Returned incorrect declaring class", val);
    }

    /**
     * @tests java.lang.reflect.Constructor#getExceptionTypes()
     */
    public void test_getExceptionTypes() throws Exception {
        Class[] exceptions = null;
        Class ex = null;
        Constructor ctor = new ConstructorTestHelper().getClass()
                .getConstructor(new Class[0]);
        exceptions = ctor.getExceptionTypes();
        ex = new IndexOutOfBoundsException().getClass();

        assertEquals("Returned exception list of incorrect length", 1,
                exceptions.length);
        assertTrue("Returned incorrect exception", exceptions[0].equals(ex));
    }

    /**
     * @tests java.lang.reflect.Constructor#getModifiers()
     */
    public void test_getModifiers() {
        int mod = 0;
        try {
            Constructor ctor = new ConstructorTestHelper().getClass()
                    .getConstructor(new Class[0]);
            mod = ctor.getModifiers();
            assertTrue("Returned incorrect modifers for public ctor",
                    ((mod & Modifier.PUBLIC) == Modifier.PUBLIC)
                            && ((mod & Modifier.PRIVATE) == 0));
        } catch (NoSuchMethodException e) {
            fail("Exception during test : " + e.getMessage());
        }
        try {
            Class[] cl = { int.class };
            Constructor ctor = new ConstructorTestHelper().getClass()
                    .getDeclaredConstructor(cl);
            mod = ctor.getModifiers();
            assertTrue("Returned incorrect modifers for private ctor",
                    ((mod & Modifier.PRIVATE) == Modifier.PRIVATE)
                            && ((mod & Modifier.PUBLIC) == 0));
        } catch (NoSuchMethodException e) {
            fail("Exception during test : " + e.getMessage());
        }
        try {
            Class[] cl = { long.class };
            Constructor ctor = new ConstructorTestHelper().getClass()
                    .getDeclaredConstructor(cl);
            mod = ctor.getModifiers();
            assertTrue("Returned incorrect modifers for private ctor",
                    ((mod & Modifier.PROTECTED) == Modifier.PROTECTED)
                            && ((mod & Modifier.PUBLIC) == 0));
        } catch (NoSuchMethodException e) {
            fail("NoSuchMethodException during test : " + e.getMessage());
        }
    }

    /**
     * @tests java.lang.reflect.Constructor#getName()
     */
    public void test_getName() throws Exception {
        Constructor ctor = new ConstructorTestHelper().getClass()
                .getConstructor(new Class[0]);
        assertTrue(
                "Returned incorrect name: " + ctor.getName(),
                ctor
                        .getName()
                        .equals(
                                "org.apache.harmony.luni.tests.java.lang.reflect.ConstructorTest$ConstructorTestHelper"));
    }

    /**
     * @tests java.lang.reflect.Constructor#getParameterTypes()
     */
    public void test_getParameterTypes() throws Exception {
        Class[] types = null;
        Constructor ctor = new ConstructorTestHelper().getClass()
                .getConstructor(new Class[0]);
        types = ctor.getParameterTypes();

        assertEquals("Incorrect parameter returned", 0, types.length);

        Class[] parms = null;
        parms = new Class[1];
        parms[0] = new Object().getClass();
        ctor = new ConstructorTestHelper().getClass().getConstructor(parms);
        types = ctor.getParameterTypes();

        assertTrue("Incorrect parameter returned", types[0].equals(parms[0]));
    }

    /**
     * @tests java.lang.reflect.Constructor#newInstance(java.lang.Object[])
     */
    public void test_newInstance$Ljava_lang_Object() throws Exception {
        ConstructorTestHelper test = null;
        Constructor ctor = new ConstructorTestHelper().getClass()
                .getConstructor(new Class[0]);
        test = (ConstructorTestHelper) ctor.newInstance((Object[]) null);

        assertEquals("improper instance created", 99, test.check());
    }
    
    /**
     * @tests java.lang.reflect.Constructor#newInstance(java.lang.Object[])
     */
    public void test_newInstance_IAE() throws Exception {
        Constructor constructor = Vector.class
                .getConstructor(new Class[] { Integer.TYPE });

        try {
            constructor.newInstance(new Object[] { null });
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    public void test_newInstance_InvocationTargetException() throws Exception {
        Constructor constructor = MockObject.class.getConstructor(Class.class);

        try {
            constructor.newInstance(InvocationTargetException.class);
            fail("should throw InvocationTargetException");
        } catch (InvocationTargetException e) {
            // Expected
        }

        try {
            constructor.newInstance(IllegalAccessException.class);
            fail("should throw InvocationTargetException");
        } catch (InvocationTargetException e) {
            // Expected
        }

        try {
            constructor.newInstance(IllegalArgumentException.class);
            fail("should throw InvocationTargetException");
        } catch (InvocationTargetException e) {
            // Expected
        }

        try {
            constructor.newInstance(InvocationTargetException.class);
            fail("should throw InvocationTargetException");
        } catch (InvocationTargetException e) {
            // Expected
        }
        
        try {
            constructor.newInstance(Throwable.class);
            fail("should throw InvocationTargetException");
        } catch (InvocationTargetException e) {
            // Expected
        }
    }

    static class MockObject {

        public MockObject(Class<?> clazz) throws Exception {
            if (clazz == InstantiationException.class) {
                throw new InstantiationException();
            } else if (clazz == IllegalAccessException.class) {
                throw new IllegalAccessException();
            } else if (clazz == IllegalArgumentException.class) {
                throw new IllegalArgumentException();
            } else if (clazz == InvocationTargetException.class) {
                throw new InvocationTargetException(new Throwable());
            } else {
                throw new Exception();
            }
        }

    }

    /**
     * @tests java.lang.reflect.Constructor#toString()
     */
    public void test_toString() throws Exception {
        Class[] parms = null;
        Constructor ctor = null;
        parms = new Class[1];
        parms[0] = new Object().getClass();
        ctor = new ConstructorTestHelper().getClass().getConstructor(parms);

        assertTrue(
                "Returned incorrect string representation: " + ctor.toString(),
                ctor
                        .toString()
                        .equals(
                                "public org.apache.harmony.luni.tests.java.lang.reflect.ConstructorTest$ConstructorTestHelper(java.lang.Object)"));
    }
}

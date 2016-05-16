/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.jni.cts;


/**
 * Basic static method tests. The "nonce" class being tested by this
 * class is a class defined in this package that declares the bulk of
 * its methods as native.
 */
public class JniStaticTest extends JniTestCase {

    /**
     * Test a simple no-op and void-returning method call.
     */
    public void test_nop() {
        StaticNonce.nop();
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnBoolean() {
        assertEquals(true, StaticNonce.returnBoolean());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnByte() {
        assertEquals(123, StaticNonce.returnByte());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnShort() {
        assertEquals(-12345, StaticNonce.returnShort());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnChar() {
        assertEquals(34567, StaticNonce.returnChar());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnInt() {
        assertEquals(12345678, StaticNonce.returnInt());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnLong() {
        assertEquals(-1098765432109876543L, StaticNonce.returnLong());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnFloat() {
        assertEquals(-98765.4321F, StaticNonce.returnFloat());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnDouble() {
        assertEquals(12345678.9, StaticNonce.returnDouble());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnNull() {
        assertNull(StaticNonce.returnNull());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnString() {
        assertEquals("blort", StaticNonce.returnString());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnShortArray() {
        short[] array = StaticNonce.returnShortArray();
        assertSame(short[].class, array.getClass());
        assertEquals(3, array.length);
        assertEquals(10, array[0]);
        assertEquals(20, array[1]);
        assertEquals(30, array[2]);
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnStringArray() {
        String[] array = StaticNonce.returnStringArray();
        assertSame(String[].class, array.getClass());
        assertEquals(100, array.length);
        assertEquals("blort", array[0]);
        assertEquals(null,    array[1]);
        assertEquals("zorch", array[50]);
        assertEquals("fizmo", array[99]);
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call,
     * that returns the class that the method is defined on.
     */
    public void test_returnThisClass() {
        assertSame(StaticNonce.class, StaticNonce.returnThisClass());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call,
     * that returns the class that the method is defined on.
     */
    public void test_returnInstance() {
        StaticNonce nonce = StaticNonce.returnInstance();
        assertSame(StaticNonce.class, nonce.getClass());
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeBoolean() {
        assertTrue(StaticNonce.takeBoolean(true));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeByte() {
        assertTrue(StaticNonce.takeByte((byte) -99));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeShort() {
        assertTrue(StaticNonce.takeShort((short) 19991));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeChar() {
        assertTrue(StaticNonce.takeChar((char) 999));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeInt() {
        assertTrue(StaticNonce.takeInt(-999888777));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeLong() {
        assertTrue(StaticNonce.takeLong(999888777666555444L));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeFloat() {
        assertTrue(StaticNonce.takeFloat(-9988.7766F));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeDouble() {
        assertTrue(StaticNonce.takeDouble(999888777.666555));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeNull() {
        assertTrue(StaticNonce.takeNull(null));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeString() {
        assertTrue(StaticNonce.takeString("fuzzbot"));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value. In particular, this test passes the
     * class the method is defined on.
     */
    public void test_takeThisClass() {
        assertTrue(StaticNonce.takeThisClass(StaticNonce.class));
    }

    /**
     * Test a simple multiple value-taking method call, that returns whether it
     * got the expected values.
     */
    public void test_takeIntLong() {
        assertTrue(StaticNonce.takeIntLong(914, 9140914091409140914L));
    }

    /**
     * Test a simple multiple value-taking method call, that returns whether it
     * got the expected values.
     */
    public void test_takeLongInt() {
        assertTrue(StaticNonce.takeLongInt(-4321L, 12341234));
    }

    /**
     * Test a simple multiple value-taking method call, that returns whether it
     * got the expected values.
     */
    public void test_takeOneOfEach() {
        assertTrue(StaticNonce.takeOneOfEach((boolean) false, (byte) 1,
                        (short) 2, (char) 3, (int) 4, 5L, "six", 7.0f, 8.0,
                        new int[] { 9, 10 }));
    }

    /**
     * Test a simple multiple value-taking method call, that returns whether it
     * got the expected values.
     */
    public void test_takeCoolHandLuke() {
        assertTrue(StaticNonce.takeCoolHandLuke(1, 2, 3, 4, 5, 6, 7, 8, 9,
                        10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                        20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                        30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
                        40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                        50));
    }
}

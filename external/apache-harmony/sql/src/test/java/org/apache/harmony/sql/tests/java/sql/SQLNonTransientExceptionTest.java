/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.sql.tests.java.sql;

import java.sql.SQLNonTransientException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLNonTransientExceptionTest extends TestCase {

    private SQLNonTransientException sQLNonTransientException;

    protected void setUp() throws Exception {
        sQLNonTransientException = new SQLNonTransientException("MYTESTSTRING",
                "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLNonTransientException = null;
    }

    /**
     * @test java.sql.SQLNonTransientException(String)
     */
    public void test_Constructor_LString() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING");
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String)
     */
    public void test_Constructor_LString_1() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                (String) null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLNonTransientException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING");
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                (String) null, (String) null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, 1);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, 0);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, -1);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The reason of SQLNonTransientException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE", sQLNonTransientException
                        .getMessage());
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                (Throwable) null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                (String) null, (Throwable) null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());

    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLNonTransientException);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());

    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLNonTransientException);
        assertEquals(
                "The SQLState of SQLNonTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, 1, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, 1, null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 1",
                sQLNonTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, 0, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, 0, null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, -1, cause);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientException set and get should be equivalent",
                cause, sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException(
                null, null, -1, null);
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be -1",
                sQLNonTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLNonTransientException should be null",
                sQLNonTransientException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientException()
     */
    public void test_Constructor() {
        SQLNonTransientException sQLNonTransientException = new SQLNonTransientException();
        assertNotNull(sQLNonTransientException);
        assertNull("The SQLState of SQLNonTransientException should be null",
                sQLNonTransientException.getSQLState());
        assertNull("The reason of SQLNonTransientException should be null",
                sQLNonTransientException.getMessage());
        assertEquals("The error code of SQLNonTransientException should be 0",
                sQLNonTransientException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLNonTransientException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLNonTransientException);
    }
}

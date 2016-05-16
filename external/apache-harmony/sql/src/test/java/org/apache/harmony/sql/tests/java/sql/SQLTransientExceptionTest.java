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

import java.sql.SQLTransientException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLTransientExceptionTest extends TestCase {

    private SQLTransientException sQLTransientException;

    protected void setUp() throws Exception {
        sQLTransientException = new SQLTransientException("MYTESTSTRING",
                "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLTransientException = null;
    }

    /**
     * @test java.sql.SQLTransientException(String)
     */
    public void test_Constructor_LString() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING");
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String)
     */
    public void test_Constructor_LString_1() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                (String) null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLTransientException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING");
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                (String) null, (String) null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, 1);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, 0);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, -1);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The reason of SQLTransientException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE", sQLTransientException
                        .getMessage());
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                (Throwable) null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, cause);
        assertNotNull(sQLTransientException);
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                (String) null, (Throwable) null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());

    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLTransientException);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertEquals(
                "The reason of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());

    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLTransientException);
        assertEquals(
                "The SQLState of SQLTransientException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, 1, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, 1, null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 1",
                sQLTransientException.getErrorCode(), 1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, 0, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, 0, null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, -1, cause);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientException set and get should be equivalent",
                cause, sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLTransientException sQLTransientException = new SQLTransientException(
                null, null, -1, null);
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be -1",
                sQLTransientException.getErrorCode(), -1);
        assertNull("The cause of SQLTransientException should be null",
                sQLTransientException.getCause());
    }

    /**
     * @test java.sql.SQLTransientException()
     */
    public void test_Constructor() {
        SQLTransientException sQLTransientException = new SQLTransientException();
        assertNotNull(sQLTransientException);
        assertNull("The SQLState of SQLTransientException should be null",
                sQLTransientException.getSQLState());
        assertNull("The reason of SQLTransientException should be null",
                sQLTransientException.getMessage());
        assertEquals("The error code of SQLTransientException should be 0",
                sQLTransientException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLTransientException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLTransientException);
    }
}

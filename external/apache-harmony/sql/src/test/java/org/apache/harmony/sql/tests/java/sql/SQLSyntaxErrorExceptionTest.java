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

import java.sql.SQLSyntaxErrorException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLSyntaxErrorExceptionTest extends TestCase {

    private SQLSyntaxErrorException sQLSyntaxErrorException;

    protected void setUp() throws Exception {
        sQLSyntaxErrorException = new SQLSyntaxErrorException("MYTESTSTRING",
                "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLSyntaxErrorException = null;
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String)
     */
    public void test_Constructor_LString() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING");
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String)
     */
    public void test_Constructor_LString_1() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                (String) null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING");
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                (String) null, (String) null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, 1);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, 0);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, -1);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The reason of SQLSyntaxErrorException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE", sQLSyntaxErrorException
                        .getMessage());
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                (Throwable) null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                (String) null, (Throwable) null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING2", sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING1", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());

    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertEquals(
                "The reason of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());

    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertEquals(
                "The SQLState of SQLSyntaxErrorException set and get should be equivalent",
                "MYTESTSTRING", sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, 1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, 1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 1",
                sQLSyntaxErrorException.getErrorCode(), 1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, 0, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, 0, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, -1, cause);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLSyntaxErrorException set and get should be equivalent",
                cause, sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException(
                null, null, -1, null);
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be -1",
                sQLSyntaxErrorException.getErrorCode(), -1);
        assertNull("The cause of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getCause());
    }

    /**
     * @test java.sql.SQLSyntaxErrorException()
     */
    public void test_Constructor() {
        SQLSyntaxErrorException sQLSyntaxErrorException = new SQLSyntaxErrorException();
        assertNotNull(sQLSyntaxErrorException);
        assertNull("The SQLState of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getSQLState());
        assertNull("The reason of SQLSyntaxErrorException should be null",
                sQLSyntaxErrorException.getMessage());
        assertEquals("The error code of SQLSyntaxErrorException should be 0",
                sQLSyntaxErrorException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLSyntaxErrorException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLSyntaxErrorException);
    }
}

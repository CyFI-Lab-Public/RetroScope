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

import java.sql.SQLIntegrityConstraintViolationException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLIntegrityConstraintViolationExceptionTest extends TestCase {

    private SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException;

    protected void setUp() throws Exception {
        sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLIntegrityConstraintViolationException = null;
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String)
     */
    public void test_Constructor_LString() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING");
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String)
     */
    public void test_Constructor_LString_1() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                (String) null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING");
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                (String) null, (String) null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, 1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, 0);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, -1);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE",
                sQLIntegrityConstraintViolationException.getMessage());
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                (Throwable) null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String,
     *       Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String,
     *       Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String,
     *       Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String,
     *       Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                (String) null, (Throwable) null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING2", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING1", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());

    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertEquals(
                "The reason of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());

    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertEquals(
                "The SQLState of SQLIntegrityConstraintViolationException set and get should be equivalent",
                "MYTESTSTRING", sQLIntegrityConstraintViolationException
                        .getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, 1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, 1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 1",
                sQLIntegrityConstraintViolationException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, 0, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, 0, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, -1, cause);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLIntegrityConstraintViolationException set and get should be equivalent",
                cause, sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException(String, String,
     *       int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException(
                null, null, -1, null);
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be -1",
                sQLIntegrityConstraintViolationException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getCause());
    }

    /**
     * @test java.sql.SQLIntegrityConstraintViolationException()
     */
    public void test_Constructor() {
        SQLIntegrityConstraintViolationException sQLIntegrityConstraintViolationException = new SQLIntegrityConstraintViolationException();
        assertNotNull(sQLIntegrityConstraintViolationException);
        assertNull(
                "The SQLState of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getSQLState());
        assertNull(
                "The reason of SQLIntegrityConstraintViolationException should be null",
                sQLIntegrityConstraintViolationException.getMessage());
        assertEquals(
                "The error code of SQLIntegrityConstraintViolationException should be 0",
                sQLIntegrityConstraintViolationException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLIntegrityConstraintViolationException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this,
                sQLIntegrityConstraintViolationException);
    }
}

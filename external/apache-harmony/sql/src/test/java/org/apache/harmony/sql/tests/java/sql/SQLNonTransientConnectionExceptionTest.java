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

import java.sql.SQLNonTransientConnectionException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLNonTransientConnectionExceptionTest extends TestCase {

    private SQLNonTransientConnectionException sQLNonTransientConnectionException;

    protected void setUp() throws Exception {
        sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLNonTransientConnectionException = null;
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String)
     */
    public void test_Constructor_LString() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING");
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String)
     */
    public void test_Constructor_LString_1() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                (String) null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING");
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                (String) null, (String) null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, 1);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, 0);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, -1);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The reason of SQLNonTransientConnectionException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE",
                sQLNonTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                (Throwable) null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                (String) null, (Throwable) null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLNonTransientConnectionException
                        .getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLNonTransientConnectionException
                        .getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());

    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());

    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertEquals(
                "The SQLState of SQLNonTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLNonTransientConnectionException
                        .getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, 1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, 1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 1",
                sQLNonTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, 0, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, 0, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, -1, cause);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLNonTransientConnectionException set and get should be equivalent",
                cause, sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException(
                null, null, -1, null);
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be -1",
                sQLNonTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLNonTransientConnectionException()
     */
    public void test_Constructor() {
        SQLNonTransientConnectionException sQLNonTransientConnectionException = new SQLNonTransientConnectionException();
        assertNotNull(sQLNonTransientConnectionException);
        assertNull(
                "The SQLState of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLNonTransientConnectionException should be null",
                sQLNonTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLNonTransientConnectionException should be 0",
                sQLNonTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLNonTransientConnectionException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest
                .verifyGolden(this, sQLNonTransientConnectionException);
    }
}

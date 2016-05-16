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

import java.sql.SQLInvalidAuthorizationSpecException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLInvalidAuthorizationSpecExceptionTest extends TestCase {

    private SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException;

    protected void setUp() throws Exception {
        sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLInvalidAuthorizationSpecException = null;
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String)
     */
    public void test_Constructor_LString() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING");
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String)
     */
    public void test_Constructor_LString_1() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                (String) null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING");
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                (String) null, (String) null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, 1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, 0);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, -1);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                (Throwable) null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                (String) null, (Throwable) null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING2", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING1", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());

    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertEquals(
                "The reason of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());

    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertEquals(
                "The SQLState of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                "MYTESTSTRING", sQLInvalidAuthorizationSpecException
                        .getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, 1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, 1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, 0, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, 0, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, -1, cause);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLInvalidAuthorizationSpecException set and get should be equivalent",
                cause, sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException(
                null, null, -1, null);
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be -1",
                sQLInvalidAuthorizationSpecException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getCause());
    }

    /**
     * @test java.sql.SQLInvalidAuthorizationSpecException()
     */
    public void test_Constructor() {
        SQLInvalidAuthorizationSpecException sQLInvalidAuthorizationSpecException = new SQLInvalidAuthorizationSpecException();
        assertNotNull(sQLInvalidAuthorizationSpecException);
        assertNull(
                "The SQLState of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getSQLState());
        assertNull(
                "The reason of SQLInvalidAuthorizationSpecException should be null",
                sQLInvalidAuthorizationSpecException.getMessage());
        assertEquals(
                "The error code of SQLInvalidAuthorizationSpecException should be 0",
                sQLInvalidAuthorizationSpecException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLInvalidAuthorizationSpecException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this,
                sQLInvalidAuthorizationSpecException);
    }
}

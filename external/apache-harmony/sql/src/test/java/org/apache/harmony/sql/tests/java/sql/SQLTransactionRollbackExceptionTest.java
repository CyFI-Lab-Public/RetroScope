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

import java.sql.SQLTransactionRollbackException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLTransactionRollbackExceptionTest extends TestCase {

    private SQLTransactionRollbackException sQLTransactionRollbackException;

    protected void setUp() throws Exception {
        sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLTransactionRollbackException = null;
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String)
     */
    public void test_Constructor_LString() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING");
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String)
     */
    public void test_Constructor_LString_1() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                (String) null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING");
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                (String) null, (String) null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, 1);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, 0);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, -1);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The reason of SQLTransactionRollbackException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE",
                sQLTransactionRollbackException.getMessage());
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                (Throwable) null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                (String) null, (Throwable) null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());

    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertEquals(
                "The reason of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());

    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertEquals(
                "The SQLState of SQLTransactionRollbackException set and get should be equivalent",
                "MYTESTSTRING", sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, 1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, 1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 1",
                sQLTransactionRollbackException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, 0, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, 0, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, -1, cause);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransactionRollbackException set and get should be equivalent",
                cause, sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException(
                null, null, -1, null);
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be -1",
                sQLTransactionRollbackException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getCause());
    }

    /**
     * @test java.sql.SQLTransactionRollbackException()
     */
    public void test_Constructor() {
        SQLTransactionRollbackException sQLTransactionRollbackException = new SQLTransactionRollbackException();
        assertNotNull(sQLTransactionRollbackException);
        assertNull(
                "The SQLState of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getSQLState());
        assertNull(
                "The reason of SQLTransactionRollbackException should be null",
                sQLTransactionRollbackException.getMessage());
        assertEquals(
                "The error code of SQLTransactionRollbackException should be 0",
                sQLTransactionRollbackException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLTransactionRollbackException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLTransactionRollbackException);
    }
}

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

import java.sql.SQLTransientConnectionException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLTransientConnectionExceptionTest extends TestCase {

    private SQLTransientConnectionException sQLTransientConnectionException;

    protected void setUp() throws Exception {
        sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLTransientConnectionException = null;
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String)
     */
    public void test_Constructor_LString() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING");
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String)
     */
    public void test_Constructor_LString_1() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                (String) null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING");
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                (String) null, (String) null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, 1);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, 0);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, -1);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The reason of SQLTransientConnectionException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE",
                sQLTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                (Throwable) null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                (String) null, (Throwable) null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING2", sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING1", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());

    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLTransientConnectionException);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertEquals(
                "The reason of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());

    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLTransientConnectionException);
        assertEquals(
                "The SQLState of SQLTransientConnectionException set and get should be equivalent",
                "MYTESTSTRING", sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, 1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, 1, null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 1",
                sQLTransientConnectionException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, 0, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, 0, null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, -1, cause);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTransientConnectionException set and get should be equivalent",
                cause, sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException(
                null, null, -1, null);
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be -1",
                sQLTransientConnectionException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getCause());
    }

    /**
     * @test java.sql.SQLTransientConnectionException()
     */
    public void test_Constructor() {
        SQLTransientConnectionException sQLTransientConnectionException = new SQLTransientConnectionException();
        assertNotNull(sQLTransientConnectionException);
        assertNull(
                "The SQLState of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getSQLState());
        assertNull(
                "The reason of SQLTransientConnectionException should be null",
                sQLTransientConnectionException.getMessage());
        assertEquals(
                "The error code of SQLTransientConnectionException should be 0",
                sQLTransientConnectionException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLTransientConnectionException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLTransientConnectionException);
    }
}

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

import java.sql.SQLFeatureNotSupportedException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLFeatureNotSupportedExceptionTest extends TestCase {

    private SQLFeatureNotSupportedException sQLFeatureNotSupportedException;

    protected void setUp() throws Exception {
        sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLFeatureNotSupportedException = null;
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String)
     */
    public void test_Constructor_LString() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING");
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String)
     */
    public void test_Constructor_LString_1() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                (String) null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING");
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                (String) null, (String) null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, 1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, 0);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, -1);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The reason of SQLFeatureNotSupportedException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE",
                sQLFeatureNotSupportedException.getMessage());
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                (Throwable) null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                (String) null, (Throwable) null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING2", sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING1", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());

    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertEquals(
                "The reason of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());

    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertEquals(
                "The SQLState of SQLFeatureNotSupportedException set and get should be equivalent",
                "MYTESTSTRING", sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, 1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, 1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 1",
                sQLFeatureNotSupportedException.getErrorCode(), 1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, 0, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, 0, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, -1, cause);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLFeatureNotSupportedException set and get should be equivalent",
                cause, sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException(String, String, int,
     *       Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException(
                null, null, -1, null);
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be -1",
                sQLFeatureNotSupportedException.getErrorCode(), -1);
        assertNull(
                "The cause of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getCause());
    }

    /**
     * @test java.sql.SQLFeatureNotSupportedException()
     */
    public void test_Constructor() {
        SQLFeatureNotSupportedException sQLFeatureNotSupportedException = new SQLFeatureNotSupportedException();
        assertNotNull(sQLFeatureNotSupportedException);
        assertNull(
                "The SQLState of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getSQLState());
        assertNull(
                "The reason of SQLFeatureNotSupportedException should be null",
                sQLFeatureNotSupportedException.getMessage());
        assertEquals(
                "The error code of SQLFeatureNotSupportedException should be 0",
                sQLFeatureNotSupportedException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLFeatureNotSupportedException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLFeatureNotSupportedException);
    }
}

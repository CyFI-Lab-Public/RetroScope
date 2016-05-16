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

import java.sql.SQLRecoverableException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLRecoverableExceptionTest extends TestCase {

    private SQLRecoverableException sQLRecoverableException;

    protected void setUp() throws Exception {
        sQLRecoverableException = new SQLRecoverableException("MYTESTSTRING",
                "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLRecoverableException = null;
    }

    /**
     * @test java.sql.SQLRecoverableException(String)
     */
    public void test_Constructor_LString() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING");
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String)
     */
    public void test_Constructor_LString_1() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                (String) null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLRecoverableException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING");
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                (String) null, (String) null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", 1);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", 0);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", -1);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, 1);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, 0);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, -1);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLRecoverableException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The reason of SQLRecoverableException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE", sQLRecoverableException
                        .getMessage());
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                (Throwable) null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLRecoverableException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                (String) null, (Throwable) null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING2", sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING1", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());

    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLRecoverableException);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertEquals(
                "The reason of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", 1, cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", 1, null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", 0, cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", 0, null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());

    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", -1, cause);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, "MYTESTSTRING", -1, null);
        assertNotNull(sQLRecoverableException);
        assertEquals(
                "The SQLState of SQLRecoverableException set and get should be equivalent",
                "MYTESTSTRING", sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, 1, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, 1, null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 1",
                sQLRecoverableException.getErrorCode(), 1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, 0, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, 0, null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, -1, cause);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLRecoverableException set and get should be equivalent",
                cause, sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException(
                null, null, -1, null);
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be -1",
                sQLRecoverableException.getErrorCode(), -1);
        assertNull("The cause of SQLRecoverableException should be null",
                sQLRecoverableException.getCause());
    }

    /**
     * @test java.sql.SQLRecoverableException()
     */
    public void test_Constructor() {
        SQLRecoverableException sQLRecoverableException = new SQLRecoverableException();
        assertNotNull(sQLRecoverableException);
        assertNull("The SQLState of SQLRecoverableException should be null",
                sQLRecoverableException.getSQLState());
        assertNull("The reason of SQLRecoverableException should be null",
                sQLRecoverableException.getMessage());
        assertEquals("The error code of SQLRecoverableException should be 0",
                sQLRecoverableException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLRecoverableException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLRecoverableException);
    }
}

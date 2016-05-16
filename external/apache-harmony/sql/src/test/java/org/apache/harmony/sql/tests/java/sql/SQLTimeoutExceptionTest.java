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

import java.sql.SQLTimeoutException;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLTimeoutExceptionTest extends TestCase {

    private SQLTimeoutException sQLTimeoutException;

    protected void setUp() throws Exception {
        sQLTimeoutException = new SQLTimeoutException("MYTESTSTRING",
                "MYTESTSTRING", 1, new Exception("MYTHROWABLE"));
    }

    protected void tearDown() throws Exception {
        sQLTimeoutException = null;
    }

    /**
     * @test java.sql.SQLTimeoutException(String)
     */
    public void test_Constructor_LString() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING");
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String)
     */
    public void test_Constructor_LString_1() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                (String) null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLTimeoutException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING");
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                (String) null, (String) null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", 1);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", 0);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", -1);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, 1);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, 0);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, -1);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLTimeoutException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The reason of SQLTimeoutException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE", sQLTimeoutException
                        .getMessage());
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                (Throwable) null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLTimeoutException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                (String) null, (Throwable) null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING2", sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING1", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());

    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLTimeoutException);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertEquals(
                "The reason of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", 1, cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", 1, null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", 0, cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", 0, null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());

    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", -1, cause);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                "MYTESTSTRING", -1, null);
        assertNotNull(sQLTimeoutException);
        assertEquals(
                "The SQLState of SQLTimeoutException set and get should be equivalent",
                "MYTESTSTRING", sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, 1, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, 1, null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 1",
                sQLTimeoutException.getErrorCode(), 1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, 0, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, 0, null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, -1, cause);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLTimeoutException set and get should be equivalent",
                cause, sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException(null,
                null, -1, null);
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be -1",
                sQLTimeoutException.getErrorCode(), -1);
        assertNull("The cause of SQLTimeoutException should be null",
                sQLTimeoutException.getCause());
    }

    /**
     * @test java.sql.SQLTimeoutException()
     */
    public void test_Constructor() {
        SQLTimeoutException sQLTimeoutException = new SQLTimeoutException();
        assertNotNull(sQLTimeoutException);
        assertNull("The SQLState of SQLTimeoutException should be null",
                sQLTimeoutException.getSQLState());
        assertNull("The reason of SQLTimeoutException should be null",
                sQLTimeoutException.getMessage());
        assertEquals("The error code of SQLTimeoutException should be 0",
                sQLTimeoutException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLTimeoutException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLTimeoutException);
    }
}

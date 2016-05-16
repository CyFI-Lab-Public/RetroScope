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

import java.sql.SQLDataException;
import junit.framework.TestCase;
import org.apache.harmony.testframework.serialization.SerializationTest;

public class SQLDataExceptionTest extends TestCase {

    private SQLDataException sQLDataException;

    @Override
    protected void setUp() throws Exception {
        sQLDataException = new SQLDataException("MYTESTSTRING", "MYTESTSTRING",
                1, new Exception("MYTHROWABLE"));
    }

    @Override
    protected void tearDown() throws Exception {
        sQLDataException = null;
    }

    /**
     * @test java.sql.SQLDataException(String)
     */
    public void test_Constructor_LString() {
        SQLDataException sQLDataException = new SQLDataException("MYTESTSTRING");
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String)
     */
    public void test_Constructor_LString_1() {
        SQLDataException sQLDataException = new SQLDataException((String) null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String)
     */
    public void test_Constructor_LStringLString() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2");
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);

    }

    /**
     * @test java.sql.SQLDataException(String, String)
     */
    public void test_Constructor_LStringLString_1() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", (String) null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String)
     */
    public void test_Constructor_LStringLString_2() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING");
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String)
     */
    public void test_Constructor_LStringLString_3() {
        SQLDataException sQLDataException = new SQLDataException((String) null,
                (String) null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_1() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_2() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_3() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, 1);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_4() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, 0);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_5() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, -1);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_6() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", 1);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_7() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", 0);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_8() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", -1);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_9() {
        SQLDataException sQLDataException = new SQLDataException(null, null, 1);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_10() {
        SQLDataException sQLDataException = new SQLDataException(null, null, 0);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, String, int)
     */
    public void test_Constructor_LStringLStringI_11() {
        SQLDataException sQLDataException = new SQLDataException(null, null, -1);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
    }

    /**
     * @test java.sql.SQLDataException(Throwable)
     */
    public void test_Constructor_LThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The reason of SQLDataException should be equals to cause.toString()",
                "java.lang.Exception: MYTHROWABLE", sQLDataException
                        .getMessage());
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(Throwable)
     */
    public void test_Constructor_LThrowable_1() {
        SQLDataException sQLDataException = new SQLDataException(
                (Throwable) null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_1() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", (Throwable) null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null, cause);
        assertNotNull(sQLDataException);
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test java.sql.SQLDataException(String, Throwable)
     */
    public void test_Constructor_LStringLThrowable_3() {
        SQLDataException sQLDataException = new SQLDataException((String) null,
                (Throwable) null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_1() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_3() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_5() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null, null,
                cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, Throwable)
     */
    public void test_Constructor_LStringLStringLThrowable_7() {
        SQLDataException sQLDataException = new SQLDataException(null, null,
                null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_1() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", 1, null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_2() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_3() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", 0, null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_4() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_5() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING1", "MYTESTSTRING2", -1, null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING2", sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING1", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());

    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_6() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, 1, cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_7() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, 1, null);
        assertNotNull(sQLDataException);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_8() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, 0, cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_9() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, 0, null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_10() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, -1, cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_11() {
        SQLDataException sQLDataException = new SQLDataException(
                "MYTESTSTRING", null, -1, null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertEquals(
                "The reason of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_12() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", 1, cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_13() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", 1, null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_14() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", 0, cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_15() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", 0, null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());

    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_16() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", -1, cause);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_17() {
        SQLDataException sQLDataException = new SQLDataException(null,
                "MYTESTSTRING", -1, null);
        assertNotNull(sQLDataException);
        assertEquals(
                "The SQLState of SQLDataException set and get should be equivalent",
                "MYTESTSTRING", sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_18() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null, null, 1,
                cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_19() {
        SQLDataException sQLDataException = new SQLDataException(null, null, 1,
                null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 1",
                sQLDataException.getErrorCode(), 1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_20() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null, null, 0,
                cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_21() {
        SQLDataException sQLDataException = new SQLDataException(null, null, 0,
                null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_22() {
        Throwable cause = new Exception("MYTHROWABLE");
        SQLDataException sQLDataException = new SQLDataException(null, null,
                -1, cause);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertEquals(
                "The cause of SQLDataException set and get should be equivalent",
                cause, sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException(String, String, int, Throwable)
     */
    public void test_Constructor_LStringLStringILThrowable_23() {
        SQLDataException sQLDataException = new SQLDataException(null, null,
                -1, null);
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be -1",
                sQLDataException.getErrorCode(), -1);
        assertNull("The cause of SQLDataException should be null",
                sQLDataException.getCause());
    }

    /**
     * @test java.sql.SQLDataException()
     */
    public void test_Constructor() {
        SQLDataException sQLDataException = new SQLDataException();
        assertNotNull(sQLDataException);
        assertNull("The SQLState of SQLDataException should be null",
                sQLDataException.getSQLState());
        assertNull("The reason of SQLDataException should be null",
                sQLDataException.getMessage());
        assertEquals("The error code of SQLDataException should be 0",
                sQLDataException.getErrorCode(), 0);
    }

    /**
     * @test serialization/deserialization compatibility.
     */
    public void test_serialization() throws Exception {
        SerializationTest.verifySelf(sQLDataException);
    }

    /**
     * @test serialization/deserialization compatibility with RI.
     */
    public void test_compatibilitySerialization() throws Exception {
        SerializationTest.verifyGolden(this, sQLDataException);
    }
}

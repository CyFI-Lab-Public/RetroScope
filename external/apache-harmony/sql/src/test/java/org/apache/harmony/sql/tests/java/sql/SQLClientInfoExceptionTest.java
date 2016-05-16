/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.sql.tests.java.sql;

import java.io.Serializable;
import java.sql.ClientInfoStatus;
import java.sql.SQLClientInfoException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class SQLClientInfoExceptionTest extends TestCase {

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException()
     */
    public void test_Constructor() {
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException();
        assertNull("The SQLState of SQLClientInfoException should be null",
                sqlClientInfoException.getSQLState());
        assertNull("The reason of SQLClientInfoException should be null",
                sqlClientInfoException.getMessage());
        assertNull(
                "The FailedProperties of SQLClientInfoException should be null",
                sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);
    }

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException(Map<String,ClientInfoStatus>)
     */
    public void test_Constructor_LMap() {
        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                failedProperties);
        assertNull("The SQLState of SQLClientInfoException should be null",
                sqlClientInfoException.getSQLState());
        assertNull("The reason of SQLClientInfoException should be null",
                sqlClientInfoException.getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);
    }

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException(Map<String,ClientInfoStatus>,Throwable)
     */
    public void test_Constructor_LMapLThrowable() {
        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        Throwable cause = new RuntimeException("Message");
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                failedProperties, cause);
        assertNull("The SQLState of SQLClientInfoException should be null",
                sqlClientInfoException.getSQLState());
        assertEquals(
                "The reason of SQLClientInfoException should be equals to cause.toString()",
                "java.lang.RuntimeException: Message", sqlClientInfoException
                        .getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLClientInfoException set and get should be equivalent",
                cause, sqlClientInfoException.getCause());

        sqlClientInfoException = new SQLClientInfoException(failedProperties,
                null);
        assertNull("The SQLState of SQLClientInfoException should be null",
                sqlClientInfoException.getSQLState());
        assertNull("The reason of SQLClientInfoException should be null",
                sqlClientInfoException.getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);
    }

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException(String,Map<String,ClientInfoStatus>)
     */
    public void test_Constructor_LStringLMap() {
        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                "Message", failedProperties);
        assertNull("The SQLState of SQLClientInfoException should be null",
                sqlClientInfoException.getSQLState());
        assertEquals(
                "The reason of SQLClientInfoException set and get should be equivalent",
                "Message", sqlClientInfoException.getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);

    }

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException(String,Map<String,ClientInfoStatus>,Throwable)
     */
    public void test_Constructor_LStringLMapLThrowable() {
        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        Throwable cause = new RuntimeException("Message");
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                "Message", failedProperties, cause);
        assertNull("The SQLState of SQLClientInfoException should be null",
                sqlClientInfoException.getSQLState());
        assertEquals(
                "The reason of SQLClientInfoException set and get should be equivalent",
                "Message", sqlClientInfoException.getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLClientInfoException set and get should be equivalent",
                cause, sqlClientInfoException.getCause());

    }

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException(String,String,Map<String,ClientInfoStatus>)
     */
    public void test_Constructor_LStringLStringLMap() {
        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                "Message", "State", failedProperties);
        assertEquals(
                "The SQLState of SQLClientInfoException set and get should be equivalent",
                "State", sqlClientInfoException.getSQLState());
        assertEquals(
                "The reason of SQLClientInfoException set and get should be equivalent",
                "Message", sqlClientInfoException.getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);
    }

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException(String,String,Map<String,ClientInfoStatus>,Throwable)
     */
    public void test_Constructor_LStringLStringLMapLThrowable() {
        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        Throwable cause = new RuntimeException("Message");
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                "Message", "State", failedProperties, cause);
        assertEquals(
                "The SQLState of SQLClientInfoException set and get should be equivalent",
                "State", sqlClientInfoException.getSQLState());
        assertEquals(
                "The reason of SQLClientInfoException set and get should be equivalent",
                "Message", sqlClientInfoException.getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 0",
                sqlClientInfoException.getErrorCode(), 0);
        assertEquals(
                "The cause of SQLClientInfoException set and get should be equivalent",
                cause, sqlClientInfoException.getCause());
    }

    /**
     * @tests java.sql.SQLClientInfoException#SQLClientInfoException(String,String,int,Map<String,ClientInfoStatus>,Throwable)
     */
    public void test_Constructor_LStringLStringILMapLThrowable() {
        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        Throwable cause = new RuntimeException("Message");
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                "Message", "State", 1, failedProperties, cause);
        assertEquals(
                "The SQLState of SQLClientInfoException set and get should be equivalent",
                "State", sqlClientInfoException.getSQLState());
        assertEquals(
                "The reason of SQLClientInfoException set and get should be equivalent",
                "Message", sqlClientInfoException.getMessage());
        assertEquals(
                "The FailedProperties of SQLClientInfoException set and get should be equivalent",
                failedProperties, sqlClientInfoException.getFailedProperties());
        assertEquals("The error code of SQLClientInfoException should be 1",
                sqlClientInfoException.getErrorCode(), 1);
        assertEquals(
                "The cause of SQLClientInfoException set and get should be equivalent",
                cause, sqlClientInfoException.getCause());
    }

    // comparator for SQLClientInfoException objects
    private static final SerializableAssert exComparator = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            SerializationTest.THROWABLE_COMPARATOR.assertDeserialized(initial,
                    deserialized);

            SQLClientInfoException initEx = (SQLClientInfoException) initial;
            SQLClientInfoException desrEx = (SQLClientInfoException) deserialized;

            assertEquals("Message", initEx.getMessage(), desrEx.getMessage());
            assertEquals("Class", initEx.getClass(), desrEx.getClass());
            assertEquals("Map", initEx.getFailedProperties(), desrEx
                    .getFailedProperties());
        }
    };

    /**
     * @tests serialization/deserialization.
     */
    public void testSerializationSelf() throws Exception {

        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        Throwable cause = new RuntimeException("Message");
        SQLClientInfoException sqlClientInfoException = new SQLClientInfoException(
                "Message", "State", 1, failedProperties, cause);
        SerializationTest.verifySelf(sqlClientInfoException, exComparator);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {

        Map<String, ClientInfoStatus> failedProperties = new HashMap<String, ClientInfoStatus>();
        failedProperties.put("String1", ClientInfoStatus.REASON_VALUE_INVALID);
        failedProperties
                .put("String2", ClientInfoStatus.REASON_VALUE_TRUNCATED);
        Throwable cause = new RuntimeException("Message");
        SerializationTest.verifyGolden(this, new SQLClientInfoException(
                "Message", "State", 1, failedProperties, cause), exComparator);
    }
}

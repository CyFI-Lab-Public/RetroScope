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

package org.apache.harmony.sql.tests.javax.sql;

import java.io.Serializable;
import java.sql.SQLException;

import javax.sql.PooledConnection;
import javax.sql.StatementEvent;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

/**
 * Test class for javax.sql.StatementEvent.
 * 
 * @since 1.6
 */
public class StatementEventTest extends TestCase {

    private static PooledConnection pc = new Impl_PooledConnection();

    private static StatementEvent st = new StatementEvent(pc, null);

    /**
     * @tests {@link javax.sql.StatementEvent#StatementEvent(PooledConnection, java.sql.PreparedStatement)}
     */
    public void testStatementEventPooledConnectionPreparedStatementSQLException() {
        SQLException e = new SQLException();
        StatementEvent st2 = new StatementEvent(pc, null, e);
        assertNotNull(st2);

        assertEquals(e, st2.getSQLException());
    }

    /**
     * @tests {@link javax.sql.StatementEvent#StatementEvent(PooledConnection, java.sql.PreparedStatement)}
     */
    public void testStatementEventPooledConnectionPreparedStatement() {
        assertNotNull(st);

        try {
            new StatementEvent(null, null);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.StatementEvent#getStatement()}
     */
    public void testGetStatement() {
        assertNull(st.getStatement());
    }

    /**
     * @tests {@link javax.sql.StatementEvent#getSQLException()}
     */
    public void testGetSQLException() {
        assertNull(st.getSQLException());
    }

    /**
     * @tests serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {
        SerializationTest.verifySelf(st, STATEMENTEVENT_COMPARATOR);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {
        StatementEvent st3 = new StatementEvent(pc, null, new SQLException(
                "test message"));
        SerializationTest.verifyGolden(this, st3, STATEMENTEVENT_COMPARATOR);
    }

    private static final SerializableAssert STATEMENTEVENT_COMPARATOR = new SerializableAssert() {

        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {
            StatementEvent iniSt = (StatementEvent) initial;
            StatementEvent dserSt = (StatementEvent) deserialized;
            if (null != iniSt.getSQLException()) {
                assertEquals(iniSt.getSQLException().getMessage(), dserSt
                        .getSQLException().getMessage());
            }
            assertEquals(iniSt.getStatement(), dserSt.getStatement());
        }

    };
}

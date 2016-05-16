/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.sql.tests.java.sql;

import static java.sql.DatabaseMetaData.*;

import junit.framework.TestCase;

public class DatabaseMetaDataTest extends TestCase {

    /*
     * Public statics test
     */
    public void testPublicStatics() {
        assertEquals(attributeNoNulls, 0);
        assertEquals(attributeNullable, 1);
        assertEquals(attributeNullableUnknown, 2);
        assertEquals(bestRowNotPseudo, 1);
        assertEquals(bestRowPseudo, 2);
        assertEquals(bestRowSession, 2);
        assertEquals(bestRowTemporary, 0);
        assertEquals(bestRowTransaction, 1);
        assertEquals(bestRowUnknown, 0);
        assertEquals(columnNoNulls, 0);
        assertEquals(columnNullable, 1);
        assertEquals(columnNullableUnknown, 2);
        assertEquals(functionColumnIn, 1);
        assertEquals(functionColumnInOut, 2);
        assertEquals(functionColumnOut, 3);
        assertEquals(functionColumnResult, 5);
        assertEquals(functionColumnUnknown, 0);
        assertEquals(functionNoNulls, 0);
        assertEquals(functionNoTable, 1);
        assertEquals(functionNullable, 1);
        assertEquals(functionNullableUnknown, 2);
        assertEquals(functionResultUnknown, 0);
        assertEquals(functionReturn, 4);
        assertEquals(functionReturnsTable, 2);
        assertEquals(importedKeyCascade, 0);
        assertEquals(importedKeyInitiallyDeferred, 5);
        assertEquals(importedKeyInitiallyImmediate, 6);
        assertEquals(importedKeyNoAction, 3);
        assertEquals(importedKeyNotDeferrable, 7);
        assertEquals(importedKeyRestrict, 1);
        assertEquals(importedKeySetDefault, 4);
        assertEquals(importedKeySetNull, 2);
        assertEquals(procedureColumnIn, 1);
        assertEquals(procedureColumnInOut, 2);
        assertEquals(procedureColumnOut, 4);
        assertEquals(procedureColumnResult, 3);
        assertEquals(procedureColumnReturn, 5);
        assertEquals(procedureColumnUnknown, 0);
        assertEquals(procedureNoNulls, 0);
        assertEquals(procedureNoResult, 1);
        assertEquals(procedureNullable, 1);
        assertEquals(procedureNullableUnknown, 2);
        assertEquals(procedureResultUnknown, 0);
        assertEquals(procedureReturnsResult, 2);
        assertEquals(sqlStateSQL, 2);
        assertEquals(sqlStateSQL99, 2);
        assertEquals(sqlStateXOpen, 1);
        assertEquals(tableIndexClustered, 1);
        assertEquals(tableIndexHashed, 2);
        assertEquals(tableIndexOther, 3);
        assertEquals(tableIndexStatistic, 0);
        assertEquals(typeNoNulls, 0);
        assertEquals(typeNullable, 1);
        assertEquals(typeNullableUnknown, 2);
        assertEquals(typePredBasic, 2);
        assertEquals(typePredChar, 1);
        assertEquals(typePredNone, 0);
        assertEquals(typeSearchable, 3);
        assertEquals(versionColumnNotPseudo, 1);
        assertEquals(versionColumnPseudo, 2);
        assertEquals(versionColumnUnknown, 0);
    }
}

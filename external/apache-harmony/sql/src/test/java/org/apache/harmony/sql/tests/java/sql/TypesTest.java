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

import junit.framework.TestCase;
import static java.sql.Types.*;

public class TypesTest extends TestCase {

    /*
     * Public statics test
     */
    public void testPublicStatics() {
        assertEquals(ARRAY, 2003);
        assertEquals(BIGINT, -5);
        assertEquals(BINARY, -2);
        assertEquals(BIT, -7);
        assertEquals(BLOB, 2004);
        assertEquals(BOOLEAN, 16);
        assertEquals(CHAR, 1);
        assertEquals(CLOB, 2005);
        assertEquals(DATALINK, 70);
        assertEquals(DATE, 91);
        assertEquals(DECIMAL, 3);
        assertEquals(DISTINCT, 2001);
        assertEquals(DOUBLE, 8);
        assertEquals(FLOAT, 6);
        assertEquals(INTEGER, 4);
        assertEquals(JAVA_OBJECT, 2000);
        assertEquals(LONGNVARCHAR, -16);
        assertEquals(LONGVARBINARY, -4);
        assertEquals(LONGVARCHAR, -1);
        assertEquals(NCHAR, -15);
        assertEquals(NCLOB, 2011);
        assertEquals(NULL, 0);
        assertEquals(NUMERIC, 2);
        assertEquals(NVARCHAR, -9);
        assertEquals(OTHER, 1111);
        assertEquals(REAL, 7);
        assertEquals(REF, 2006);
        assertEquals(ROWID, -8);
        assertEquals(SMALLINT, 5);
        assertEquals(SQLXML, 2009);
        assertEquals(STRUCT, 2002);
        assertEquals(TIME, 92);
        assertEquals(TIMESTAMP, 93);
        assertEquals(TINYINT, -6);
        assertEquals(VARBINARY, -3);
        assertEquals(VARCHAR, 12);
    }

}

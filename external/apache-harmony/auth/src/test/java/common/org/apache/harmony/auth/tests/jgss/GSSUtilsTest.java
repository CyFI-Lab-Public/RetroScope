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

package org.apache.harmony.auth.tests.jgss;

import org.apache.harmony.auth.jgss.GSSUtils;

import junit.framework.TestCase;

public class GSSUtilsTest extends TestCase {

	public void testGSSUtils_getBytes_and_getInt() throws Exception {
		int i = 0x7F;
		byte[] bytes = GSSUtils.getBytes(i, 1);
		int j = GSSUtils.toInt(bytes, 0 , 1);
		assertEquals(i, j);

		i = 0x0F;
		bytes = GSSUtils.getBytes(i, 1);
		j = GSSUtils.toInt(bytes, 0 , 1);
		assertEquals(i, j);

		i = 0x01FF;
		bytes = GSSUtils.getBytes(i, 2);
		j = GSSUtils.toInt(bytes, 0, 2);
		assertEquals(i, j);

		i = 0x0503;
		bytes = GSSUtils.getBytes(i, 2);
		j = GSSUtils.toInt(bytes, 0, 2);
		assertEquals(i, j);

		i = 0x05804E;
		bytes = GSSUtils.getBytes(i, 3);
		j = GSSUtils.toInt(bytes, 0 , 3);
		assertEquals(i, j);

		i = 0x0580E2;
		bytes = GSSUtils.getBytes(i, 4);
		j = GSSUtils.toInt(bytes, 0, 4);
		assertEquals(i, j);
	}
}

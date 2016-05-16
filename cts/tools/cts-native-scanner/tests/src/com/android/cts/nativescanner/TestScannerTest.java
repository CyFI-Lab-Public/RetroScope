/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.cts.nativescanner;

import com.android.cts.nativescanner.TestScanner;

import junit.framework.TestCase;

import java.io.File;
import java.io.StringReader;
import java.lang.StringBuilder;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Unit tests for {@link TestScanner}.
 */
public class TestScannerTest extends TestCase {

    public void testScanFile() {
        TestScanner testScanner = new TestScanner(new File("unused"), "TestSuite");

        String newLine = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append("foobar" + newLine);  // ignored
        sb.append("TEST_F(TestCase1, TestName1)" + newLine);  // valid
        sb.append("TEST_F(TestCase1, TestName2)" + newLine);  // valid
        sb.append("TEST_F(TestCase2, TestName1) foo" + newLine);  // valid
        sb.append("TEST_F(TestCase2, TestName1 foo)" + newLine);  // ignored
        sb.append("foo TEST_F(TestCase2, TestName1)" + newLine);  // ignored

        List<String> names = new ArrayList<String>();
        Scanner scanner = new Scanner(new StringReader(sb.toString()));
        testScanner.scanFile(scanner, names);
        Iterator it = names.iterator();

        assertEquals("suite:TestSuite", it.next());
        assertEquals("case:TestCase1", it.next());
        assertEquals("test:TestName1", it.next());
        assertEquals("test:TestName2", it.next());
        assertEquals("suite:TestSuite", it.next());
        assertEquals("case:TestCase2", it.next());
        assertEquals("test:TestName1", it.next());
        assertFalse(it.hasNext());
        scanner.close();
    }
}

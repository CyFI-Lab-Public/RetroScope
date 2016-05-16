/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.xmlgenerator;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Parser of test lists that are in the format of:
 *
 * suite:android.holo.cts
 * case:HoloTest
 * test:testHolo
 * test:testHoloDialog[:timeout_value]
 */
class TestListParser {

    public Collection<TestSuite> parse(InputStream input) {
        Map<String, TestSuite> suiteMap = new HashMap<String, TestSuite>();
        TestSuite currentSuite = null;
        TestCase currentCase = null;
        Scanner scanner = null;
        try {
            scanner = new Scanner(input);
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(":");
                if (tokens.length < 2) {
                    continue;
                }

                String key = tokens[0];
                String value = tokens[1];
                if ("suite".equals(key)) {
                    currentSuite = handleSuite(suiteMap, value);
                } else if ("case".equals(key)) {
                    currentCase = handleCase(currentSuite, value);
                } else if ("test".equals(key)) {
                    int timeout = -1;
                    if (tokens.length == 3) {
                        timeout = Integer.parseInt(tokens[2]);
                    }
                    handleTest(currentCase, value, timeout);
                }
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return suiteMap.values();
    }

    private TestSuite handleSuite(Map<String, TestSuite> suiteMap, String fullSuite) {
        String[] suites = fullSuite.split("\\.");
        int numSuites = suites.length;
        TestSuite lastSuite = null;

        for (int i = 0; i < numSuites; i++) {
            String name = suites[i];
            if (lastSuite != null) {
                if (lastSuite.hasSuite(name)) {
                    lastSuite = lastSuite.getSuite(name);
                } else {
                    TestSuite newSuite = new TestSuite(name);
                    lastSuite.addSuite(newSuite);
                    lastSuite = newSuite;
                }
            } else if (suiteMap.containsKey(name)) {
                lastSuite = suiteMap.get(name);
            } else {
                lastSuite = new TestSuite(name);
                suiteMap.put(name, lastSuite);
            }
        }

        return lastSuite;
    }

    private TestCase handleCase(TestSuite suite, String caseName) {
        TestCase testCase = new TestCase(caseName);
        suite.addCase(testCase);
        return testCase;
    }

    private void handleTest(TestCase testCase, String test, int timeout) {
        testCase.addTest(test, timeout);
    }
}

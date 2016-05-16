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

import vogar.Expectation;
import vogar.ExpectationStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Generator of TestPackage XML files for native tests.
 *
 * It takes in an input of the following form:
 *
 * suite: x.y.z
 * case:TestClass1
 * test:testMethod1
 * test:testMethod2
 * case:TestClass2
 * test:testMethod1
 * suite: x.y
 * case:TestClass3
 * test:testMethod2
 */
class XmlGenerator {

    /** Example: com.android.cts.holo */
    private final String mAppNamespace;

    /** Test package name like "android.nativemedia" to group the tests. */
    private final String mAppPackageName;

    /** Name of the native executable. */
    private final String mName;

    /** Test runner */
    private final String mRunner;

    private final String mTargetBinaryName;

    private final String mTargetNameSpace;

    private final String mJarPath;

    private final String mTestType;

    /** Path to output file or null to just dump to standard out. */
    private final String mOutputPath;

    /** ExpectationStore to filter out known failures. */
    private final ExpectationStore mExpectations;

    XmlGenerator(ExpectationStore expectations, String appNameSpace, String appPackageName,
            String name, String runner, String targetBinaryName, String targetNameSpace,
            String jarPath, String testType, String outputPath) {
        mAppNamespace = appNameSpace;
        mAppPackageName = appPackageName;
        mName = name;
        mRunner = runner;
        mTargetBinaryName = targetBinaryName;
        mTargetNameSpace = targetNameSpace;
        mJarPath = jarPath;
        mTestType = testType;
        mOutputPath = outputPath;
        mExpectations = expectations;
    }

    public void writePackageXml() throws IOException {
        OutputStream output = System.out;
        if (mOutputPath != null) {
            File outputFile = new File(mOutputPath);
            output = new FileOutputStream(outputFile);
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(output);
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writeTestPackage(writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void writeTestPackage(PrintWriter writer) {
        writer.append("<TestPackage");
        if (mAppNamespace != null) {
            writer.append(" appNameSpace=\"").append(mAppNamespace).append("\"");
        }

        writer.append(" appPackageName=\"").append(mAppPackageName).append("\"");
        writer.append(" name=\"").append(mName).append("\"");

        if (mRunner != null) {
            writer.append(" runner=\"").append(mRunner).append("\"");
        }

        if (mAppNamespace != null && mTargetNameSpace != null
                && !mAppNamespace.equals(mTargetNameSpace)) {
            writer.append(" targetBinaryName=\"").append(mTargetBinaryName).append("\"");
            writer.append(" targetNameSpace=\"").append(mTargetNameSpace).append("\"");
        }

        if (mTestType != null && !mTestType.isEmpty()) {
            writer.append(" testType=\"").append(mTestType).append("\"");
        }

        if (mJarPath != null) {
            writer.append(" jarPath=\"").append(mJarPath).append("\"");
        }

        writer.println(" version=\"1.0\">");

        TestListParser parser = new TestListParser();
        Collection<TestSuite> suites = parser.parse(System.in);
        StringBuilder nameCollector = new StringBuilder();
        writeTestSuites(writer, suites, nameCollector);
        writer.println("</TestPackage>");
    }

    private void writeTestSuites(PrintWriter writer, Collection<TestSuite> suites,
            StringBuilder nameCollector) {
        Collection<TestSuite> sorted = sortCollection(suites);
        for (TestSuite suite : sorted) {
            writer.append("<TestSuite name=\"").append(suite.getName()).println("\">");

            String namePart = suite.getName();
            if (nameCollector.length() > 0) {
                namePart = "." + namePart;
            }
            nameCollector.append(namePart);

            writeTestSuites(writer, suite.getSuites(), nameCollector);
            writeTestCases(writer, suite.getCases(), nameCollector);

            nameCollector.delete(nameCollector.length() - namePart.length(),
                    nameCollector.length());
            writer.println("</TestSuite>");
        }
    }

    private void writeTestCases(PrintWriter writer, Collection<TestCase> cases,
            StringBuilder nameCollector) {
        Collection<TestCase> sorted = sortCollection(cases);
        for (TestCase testCase : sorted) {
            String name = testCase.getName();
            writer.append("<TestCase name=\"").append(name).println("\">");
            nameCollector.append('.').append(name);

            writeTests(writer, testCase.getTests(), nameCollector);

            nameCollector.delete(nameCollector.length() - name.length() - 1,
                    nameCollector.length());
            writer.println("</TestCase>");
        }
    }

    private void writeTests(PrintWriter writer, Collection<Test> tests,
            StringBuilder nameCollector) {
        Collection<Test> sorted = sortCollection(tests);
        for (Test test : sorted) {
            nameCollector.append('#').append(test.getName());
            writer.append("<Test name=\"").append(test.getName()).append("\"");
            if (isKnownFailure(mExpectations, nameCollector.toString())) {
                writer.append(" expectation=\"failure\"");
            }
            if (test.getTimeout() >= 0) {
                writer.append(" timeout=\"" + test.getTimeout() + "\"");
            }
            writer.println(" />");

            nameCollector.delete(nameCollector.length() - test.getName().length() - 1,
                    nameCollector.length());
        }
    }

    private <E extends Comparable<E>> Collection<E> sortCollection(Collection<E> col) {
        List<E> list = new ArrayList<E>(col);
        Collections.sort(list);
        return list;
    }

    public static boolean isKnownFailure(ExpectationStore expectationStore, String testName) {
        return expectationStore != null && expectationStore.get(testName) != Expectation.SUCCESS;
    }
}

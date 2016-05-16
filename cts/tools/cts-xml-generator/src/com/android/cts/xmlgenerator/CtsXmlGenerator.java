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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import vogar.ExpectationStore;
import vogar.ModeId;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

/** Class that outputs a test package xml. */
public class CtsXmlGenerator {

    private static void usage(String[] args) {
        System.err.println("Arguments: " + Arrays.asList(args));
        System.err.println("Usage: cts-xml-generator -p PACKAGE_NAME -n NAME [-t TEST_TYPE]"
                + " [-j JAR_PATH] [-i INSTRUMENTATION] [-m MANIFEST_FILE] [-e EXPECTATION_FILE]"
                + " [-o OUTPUT_FILE]");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        String appPackageName = null;
        String name = null;
        String outputPath = null;
        Set<File> expectationFiles = new HashSet<File>();
        File manifestFile = null;
        String instrumentation = null;
        String testType = null;
        String jarPath = null;
        String appNameSpace = null;
        String targetNameSpace = null;

        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i])) {
                appPackageName = getArg(args, ++i, "Missing value for test package");
            } else if ("-n".equals(args[i])) {
                name = getArg(args, ++i, "Missing value for executable name");
            } else if ("-t".equals(args[i])) {
                testType = getArg(args, ++i, "Missing value for test type");
            } else if ("-j".equals(args[i])) {
                jarPath = getArg(args, ++i, "Missing value for jar path");
            } else if ("-m".equals(args[i])) {
                manifestFile = new File(getArg(args, ++i, "Missing value for manifest"));
            } else if ("-i".equals(args[i])) {
                instrumentation = getArg(args, ++i, "Missing value for instrumentation");
            } else if ("-e".equals(args[i])) {
                expectationFiles.add(new File(getArg(args, ++i,
                        "Missing value for expectation store")));
            } else if ("-o".equals(args[i])) {
                outputPath = getArg(args, ++i, "Missing value for output file");
            } else if ("-a".equals(args[i])) {
                appNameSpace =  getArg(args, ++i, "Missing value for app name space");
            } else if ("-r".equals(args[i])) {
                targetNameSpace =  getArg(args, ++i, "Missing value for target name space");
            } else {
                System.err.println("Unsupported flag: " + args[i]);
                usage(args);
            }
        }

        String runner = null;

        if (manifestFile != null) {
            Document manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(manifestFile);
            Element documentElement = manifest.getDocumentElement();
            appNameSpace = documentElement.getAttribute("package");
            runner = getElementAttribute(documentElement, "instrumentation",
                    "android:name");
            targetNameSpace = getElementAttribute(documentElement, "instrumentation",
                    "android:targetPackage");
        }

        if (appPackageName == null) {
            System.out.println("Package name is required");
            usage(args);
        } else if (name == null) {
            System.out.println("Executable name is required");
            usage(args);
        }

        ExpectationStore store = ExpectationStore.parse(expectationFiles, ModeId.DEVICE);
        XmlGenerator generator = new XmlGenerator(store, appNameSpace, appPackageName,
                name, runner, instrumentation, targetNameSpace, jarPath, testType, outputPath);
        generator.writePackageXml();
    }

    private static String getArg(String[] args, int index, String message) {
        if (index < args.length) {
            return args[index];
        } else {
            System.err.println(message);
            usage(args);
            return null;
        }
    }

    private static String getElementAttribute(Element parentElement, String elementName,
            String attributeName) {
        NodeList nodeList = parentElement.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
             Element element = (Element) nodeList.item(0);
             return element.getAttribute(attributeName);
        }
        return null;
    }
}

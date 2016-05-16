/*
 * Copyright (C) 2008 The Android Open Source Project
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import vogar.Expectation;
import vogar.ExpectationStore;
import vogar.ModeId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;

public class CollectAllTests extends DescriptionGenerator {

    private static final String ATTRIBUTE_RUNNER = "runner";
    private static final String ATTRIBUTE_PACKAGE = "appPackageName";
    private static final String ATTRIBUTE_NS = "appNameSpace";
    private static final String ATTRIBUTE_TARGET = "targetNameSpace";
    private static final String ATTRIBUTE_TARGET_BINARY = "targetBinaryName";
    private static final String ATTRIBUTE_HOST_SIDE_ONLY = "hostSideOnly";
    private static final String ATTRIBUTE_VM_HOST_TEST = "vmHostTest";
    private static final String ATTRIBUTE_JAR_PATH = "jarPath";
    private static final String ATTRIBUTE_JAVA_PACKAGE_FILTER = "javaPackageFilter";

    private static final String JAR_PATH = "LOCAL_JAR_PATH :=";
    private static final String TEST_TYPE = "LOCAL_TEST_TYPE :";

    public static void main(String[] args) {
        if (args.length < 4 || args.length > 6) {
            System.err.println("usage: CollectAllTests <output-file> <manifest-file> <jar-file> "
                               + "<java-package> [expectation-dir [makefile-file]]");
            if (args.length != 0) {
                System.err.println("received:");
                for (String arg : args) {
                    System.err.println("  " + arg);
                }
            }
            System.exit(1);
        }

        final String outputPathPrefix = args[0];
        File manifestFile = new File(args[1]);
        String jarFileName = args[2];
        final String javaPackageFilter = args[3];
        // Validate the javaPackageFilter value if non null.
        if (javaPackageFilter.length() != 0) {
            if (!isValidJavaPackage(javaPackageFilter)) {
                System.err.println("Invalid " + ATTRIBUTE_JAVA_PACKAGE_FILTER + ": " +
                        javaPackageFilter);
                System.exit(1);
                return;
            }
        }
        String libcoreExpectationDir = (args.length > 4) ? args[4] : null;
        String androidMakeFile = (args.length > 5) ? args[5] : null;

        final TestType testType = TestType.getTestType(androidMakeFile);

        Document manifest;
        try {
            manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                  new FileInputStream(manifestFile));
        } catch (Exception e) {
            System.err.println("cannot open manifest " + manifestFile);
            e.printStackTrace();
            System.exit(1);
            return;
        }

        Element documentElement = manifest.getDocumentElement();
        documentElement.getAttribute("package");
        final String runner = getElementAttribute(documentElement,
                                                  "instrumentation",
                                                  "android:name");
        final String packageName = documentElement.getAttribute("package");
        final String target = getElementAttribute(documentElement,
                                                  "instrumentation",
                                                  "android:targetPackage");

        String outputXmlFile = outputPathPrefix + ".xml";
        final String xmlName = new File(outputPathPrefix).getName();
        XMLGenerator xmlGenerator;
        try {
            xmlGenerator = new XMLGenerator(outputXmlFile) {
                {
                    Node testPackageElem = mDoc.getDocumentElement();

                    setAttribute(testPackageElem, ATTRIBUTE_NAME, xmlName);
                    setAttribute(testPackageElem, ATTRIBUTE_RUNNER, runner);
                    setAttribute(testPackageElem, ATTRIBUTE_PACKAGE, packageName);
                    setAttribute(testPackageElem, ATTRIBUTE_NS, packageName);
                    setAttribute(testPackageElem, ATTRIBUTE_JAVA_PACKAGE_FILTER, javaPackageFilter);

                    if (testType.type == TestType.HOST_SIDE_ONLY) {
                        setAttribute(testPackageElem, ATTRIBUTE_HOST_SIDE_ONLY, "true");
                        setAttribute(testPackageElem, ATTRIBUTE_JAR_PATH, testType.jarPath);
                    }

                    if (testType.type == TestType.VM_HOST_TEST) {
                        setAttribute(testPackageElem, ATTRIBUTE_VM_HOST_TEST, "true");
                        setAttribute(testPackageElem, ATTRIBUTE_JAR_PATH, testType.jarPath);
                    }

                    if (!packageName.equals(target)) {
                        setAttribute(testPackageElem, ATTRIBUTE_TARGET, target);
                        setAttribute(testPackageElem, ATTRIBUTE_TARGET_BINARY, target);
                    }
                }
            };
        } catch (ParserConfigurationException e) {
            System.err.println("Can't initialize XML Generator " + outputXmlFile);
            System.exit(1);
            return;
        }

        ExpectationStore libcoreVogarExpectationStore;
        ExpectationStore ctsVogarExpectationStore;

        try {
            libcoreVogarExpectationStore
                    = VogarUtils.provideExpectationStore(libcoreExpectationDir);
            ctsVogarExpectationStore = VogarUtils.provideExpectationStore(CTS_EXPECTATION_DIR);
        } catch (IOException e) {
            System.err.println("Can't initialize vogar expectation store from "
                               + libcoreExpectationDir);
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        }
        ExpectationStore[] expectations = new ExpectationStore[] {
            libcoreVogarExpectationStore, ctsVogarExpectationStore
        };

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarFileName);
        } catch (Exception e) {
            System.err.println("cannot open jarfile " + jarFileName);
            e.printStackTrace();
            System.exit(1);
        }

        Map<String,TestClass> testCases = new LinkedHashMap<String, TestClass>();

        String javaPackagePrefix = javaPackageFilter.isEmpty() ? "" : (javaPackageFilter + ".");

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String name = jarEntry.getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            String className
                    = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            if (!className.startsWith(javaPackagePrefix)) {
                continue;
            }
            try {
                Class<?> klass = Class.forName(className,
                                               false,
                                               CollectAllTests.class.getClassLoader());
                if (!TestCase.class.isAssignableFrom(klass)) {
                    continue;
                }
                if (Modifier.isAbstract(klass.getModifiers())) {
                    continue;
                }
                if (!Modifier.isPublic(klass.getModifiers())) {
                    continue;
                }
                try {
                    klass.getConstructor(new Class<?>[] { String.class } );
                    addToTests(expectations, testCases, klass.asSubclass(TestCase.class));
                    continue;
                } catch (NoSuchMethodException e) {
                } catch (SecurityException e) {
                    System.out.println("Known bug (Working as intended): problem with class "
                            + className);
                    e.printStackTrace();
                }
                try {
                    klass.getConstructor(new Class<?>[0]);
                    addToTests(expectations, testCases, klass.asSubclass(TestCase.class));
                    continue;
                } catch (NoSuchMethodException e) {
                } catch (SecurityException e) {
                    System.out.println("Known bug (Working as intended): problem with class "
                            + className);
                    e.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                System.out.println("class not found " + className);
                e.printStackTrace();
                System.exit(1);
            }
        }

        for (Iterator<TestClass> iterator = testCases.values().iterator(); iterator.hasNext();) {
            TestClass type = iterator.next();
            xmlGenerator.addTestClass(type);
        }

        try {
            xmlGenerator.dump();
        } catch (Exception e) {
            System.err.println("cannot dump xml to " + outputXmlFile);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class TestType {
        private static final int HOST_SIDE_ONLY = 1;
        private static final int DEVICE_SIDE_ONLY = 2;
        private static final int VM_HOST_TEST = 3;

        private final int type;
        private final String jarPath;

        private TestType (int type, String jarPath) {
            this.type = type;
            this.jarPath = jarPath;
        }

        private static TestType getTestType(String makeFileName) {
            if (makeFileName == null || makeFileName.isEmpty()) {
                return new TestType(DEVICE_SIDE_ONLY, null);
            }
            int type = TestType.DEVICE_SIDE_ONLY;
            String jarPath = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(makeFileName));
                String line;

                while ((line =reader.readLine())!=null) {
                    if (line.startsWith(TEST_TYPE)) {
                        if (line.indexOf(ATTRIBUTE_VM_HOST_TEST) >= 0) {
                            type = VM_HOST_TEST;
                        } else {
                            type = HOST_SIDE_ONLY;
                        }
                    } else if (line.startsWith(JAR_PATH)) {
                        jarPath = line.substring(JAR_PATH.length(), line.length()).trim();
                    }
                }
                reader.close();
            } catch (IOException e) {
            }
            return new TestType(type, jarPath);
        }
    }

    private static Element getElement(Element element, String tagName) {
        NodeList elements = element.getElementsByTagName(tagName);
        if (elements.getLength() > 0) {
            return (Element) elements.item(0);
        } else {
            return null;
        }
    }

    private static String getElementAttribute(Element element,
                                              String elementName,
                                              String attributeName) {
        Element e = getElement(element, elementName);
        if (e != null) {
            return e.getAttribute(attributeName);
        } else {
            return "";
        }
    }

    private static String getKnownFailure(final Class<? extends TestCase> testClass,
            final String testName) {
        return getAnnotation(testClass, testName, KNOWN_FAILURE);
    }

    private static boolean isKnownFailure(final Class<? extends TestCase> testClass,
            final String testName) {
        return getAnnotation(testClass, testName, KNOWN_FAILURE) != null;
    }

    private static boolean isBrokenTest(final Class<? extends TestCase> testClass,
            final String testName)  {
        return getAnnotation(testClass, testName, BROKEN_TEST) != null;
    }

    private static boolean isSuppressed(final Class<? extends TestCase> testClass,
            final String testName)  {
        return getAnnotation(testClass, testName, SUPPRESSED_TEST) != null;
    }

    private static boolean hasSideEffects(final Class<? extends TestCase> testClass,
            final String testName) {
        return getAnnotation(testClass, testName, SIDE_EFFECT) != null;
    }

    private static String getAnnotation(final Class<? extends TestCase> testClass,
            final String testName, final String annotationName) {
        try {
            Method testMethod = testClass.getMethod(testName, (Class[])null);
            Annotation[] annotations = testMethod.getAnnotations();
            for (Annotation annot : annotations) {

                if (annot.annotationType().getName().equals(annotationName)) {
                    String annotStr = annot.toString();
                    String knownFailure = null;
                    if (annotStr.contains("(value=")) {
                        knownFailure =
                            annotStr.substring(annotStr.indexOf("=") + 1,
                                    annotStr.length() - 1);

                    }

                    if (knownFailure == null) {
                        knownFailure = "true";
                    }

                    return knownFailure;
                }

            }

        } catch (NoSuchMethodException e) {
        }

        return null;
    }

    private static void addToTests(ExpectationStore[] expectations,
                                   Map<String,TestClass> testCases,
                                   Class<? extends TestCase> test) {
        Class testClass = test;
        Set<String> testNames = new HashSet<String>();
        while (TestCase.class.isAssignableFrom(testClass)) {
            Method[] testMethods = testClass.getDeclaredMethods();
            for (Method testMethod : testMethods) {
                String testName = testMethod.getName();
                if (testNames.contains(testName)) {
                    continue;
                }
                if (!testName.startsWith("test")) {
                    continue;
                }
                if (testMethod.getParameterTypes().length != 0) {
                    continue;
                }
                if (!testMethod.getReturnType().equals(Void.TYPE)) {
                    continue;
                }
                if (!Modifier.isPublic(testMethod.getModifiers())) {
                    continue;
                }
                testNames.add(testName);
                addToTests(expectations, testCases, test, testName);
            }
            testClass = testClass.getSuperclass();
        }
    }

    private static void addToTests(ExpectationStore[] expectations,
                                   Map<String,TestClass> testCases,
                                   Class<? extends TestCase> test,
                                   String testName) {

        String testClassName = test.getName();
        String knownFailure = getKnownFailure(test, testName);

        if (isKnownFailure(test, testName)) {
            System.out.println("ignoring known failure: " + test + "#" + testName);
            return;
        } else if (isBrokenTest(test, testName)) {
            System.out.println("ignoring broken test: " + test + "#" + testName);
            return;
        } else if (isSuppressed(test, testName)) {
            System.out.println("ignoring suppressed test: " + test + "#" + testName);
            return;
        } else if (hasSideEffects(test, testName)) {
            System.out.println("ignoring test with side effects: " + test + "#" + testName);
            return;
        } else if (VogarUtils.isVogarKnownFailure(expectations,
                                                  testClassName,
                                                  testName)) {
            System.out.println("ignoring expectation known failure: " + test
                               + "#" + testName);
            return;
        }

        TestClass testClass = null;
        if (testCases.containsKey(testClassName)) {
            testClass = testCases.get(testClassName);
        } else {
            testClass = new TestClass(testClassName, new ArrayList<TestMethod>());
            testCases.put(testClassName, testClass);
        }

        testClass.mCases.add(new TestMethod(testName, "", "", knownFailure, false, false));
    }

    /**
     * Determines if a given string is a valid java package name
     * @param javaPackageName
     * @return true if it is valid, false otherwise
     */
    private static boolean isValidJavaPackage(String javaPackageName) {
        String[] strSections = javaPackageName.split(".");
        for (String strSection : strSections) {
          if (!isValidJavaIdentifier(strSection)) {
              return false;
          }
        }
        return true;
    }

    /**
     * Determines if a given string is a valid java identifier.
     * @param javaIdentifier
     * @return true if it is a valid identifier, false otherwise
     */
    private static boolean isValidJavaIdentifier(String javaIdentifier) {
        if (javaIdentifier.length() == 0 ||
                !Character.isJavaIdentifierStart(javaIdentifier.charAt(0))) {
            return false;
        }
        for (int i = 1; i < javaIdentifier.length(); i++) {
            if (!Character.isJavaIdentifierPart(javaIdentifier.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

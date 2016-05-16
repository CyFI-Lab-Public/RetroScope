/*
 * Copyright (C) The Android Open Source Project
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import vogar.ExpectationStore;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;

/**
 * This is only a very simple and brief JavaDoc parser for the CTS.
 *
 * Input: The source files of the test cases. It will be represented
 *          as a list of ClassDoc
 * Output: Generate file description.xml, which defines the TestPackage
 *          TestSuite and TestCases.
 *
 * Note:
 *  1. Since this class has dependencies on com.sun.javadoc package which
 *       is not implemented on Android. So this class can't be compiled.
 *  2. The TestSuite can be embedded, which means:
 *      TestPackage := TestSuite*
 *      TestSuite := TestSuite* | TestCase*
 */
public class DescriptionGenerator extends Doclet {
    static final String HOST_CONTROLLER = "dalvik.annotation.HostController";
    static final String KNOWN_FAILURE = "dalvik.annotation.KnownFailure";
    static final String BROKEN_TEST = "dalvik.annotation.BrokenTest";
    static final String SIDE_EFFECT = "dalvik.annotation.SideEffect";
    static final String SUPPRESSED_TEST = "android.test.suitebuilder.annotation.Suppress";
    static final String CTS_EXPECTATION_DIR = "cts/tests/expectations";

    static final String JUNIT_TEST_CASE_CLASS_NAME = "junit.framework.testcase";
    static final String TAG_PACKAGE = "TestPackage";
    static final String TAG_SUITE = "TestSuite";
    static final String TAG_CASE = "TestCase";
    static final String TAG_TEST = "Test";
    static final String TAG_DESCRIPTION = "Description";

    static final String ATTRIBUTE_NAME_VERSION = "version";
    static final String ATTRIBUTE_VALUE_VERSION = "1.0";
    static final String ATTRIBUTE_NAME_FRAMEWORK = "AndroidFramework";
    static final String ATTRIBUTE_VALUE_FRAMEWORK = "Android 1.0";

    static final String ATTRIBUTE_NAME = "name";
    static final String ATTRIBUTE_HOST_CONTROLLER = "HostController";

    static final String XML_OUTPUT_PATH = "./description.xml";

    static final String OUTPUT_PATH_OPTION = "-o";

    /**
     * Start to parse the classes passed in by javadoc, and generate
     * the xml file needed by CTS packer.
     *
     * @param root The root document passed in by javadoc.
     * @return Whether the document has been processed.
     */
    public static boolean start(RootDoc root) {
        ClassDoc[] classes = root.classes();
        if (classes == null) {
            Log.e("No class found!", null);
            return true;
        }

        String outputPath = XML_OUTPUT_PATH;
        String[][] options = root.options();
        for (String[] option : options) {
            if (option.length == 2 && option[0].equals(OUTPUT_PATH_OPTION)) {
                outputPath = option[1];
            }
        }

        XMLGenerator xmlGenerator = null;
        try {
            xmlGenerator = new XMLGenerator(outputPath);
        } catch (ParserConfigurationException e) {
            Log.e("Cant initialize XML Generator!", e);
            return true;
        }

        ExpectationStore ctsExpectationStore = null;
        try {
            ctsExpectationStore = VogarUtils.provideExpectationStore("./" + CTS_EXPECTATION_DIR);
        } catch (IOException e) {
            Log.e("Couldn't load expectation store.", e);
            return false;
        }

        for (ClassDoc clazz : classes) {
            if ((!clazz.isAbstract()) && (isValidJUnitTestCase(clazz))) {
                xmlGenerator.addTestClass(new TestClass(clazz, ctsExpectationStore));
            }
        }

        try {
            xmlGenerator.dump();
        } catch (Exception e) {
            Log.e("Can't dump to XML file!", e);
        }

        return true;
    }

    /**
     * Return the length of any doclet options we recognize
     * @param option The option name
     * @return The number of words this option takes (including the option) or 0 if the option
     * is not recognized.
     */
    public static int optionLength(String option) {
        if (option.equals(OUTPUT_PATH_OPTION)) {
            return 2;
        }
        return 0;
    }

    /**
     * Check if the class is valid test case inherited from JUnit TestCase.
     *
     * @param clazz The class to be checked.
     * @return If the class is valid test case inherited from JUnit TestCase, return true;
     *         else, return false.
     */
    static boolean isValidJUnitTestCase(ClassDoc clazz) {
        while((clazz = clazz.superclass()) != null) {
            if (JUNIT_TEST_CASE_CLASS_NAME.equals(clazz.qualifiedName().toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Log utility.
     */
    static class Log {
        private static boolean TRACE = true;
        private static BufferedWriter mTraceOutput = null;

        /**
         * Log the specified message.
         *
         * @param msg The message to be logged.
         */
        static void e(String msg, Exception e) {
            System.out.println(msg);

            if (e != null) {
                e.printStackTrace();
            }
        }

        /**
         * Add the message to the trace stream.
         *
         * @param msg The message to be added to the trace stream.
         */
        public static void t(String msg) {
            if (TRACE) {
                try {
                    if ((mTraceOutput != null) && (msg != null)) {
                        mTraceOutput.write(msg + "\n");
                        mTraceOutput.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Initialize the trace stream.
         *
         * @param name The class name.
         */
        public static void initTrace(String name) {
            if (TRACE) {
                try {
                    if (mTraceOutput == null) {
                        String fileName = "cts_debug_dg_" + name + ".txt";
                        mTraceOutput = new BufferedWriter(new FileWriter(fileName));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Close the trace stream.
         */
        public static void closeTrace() {
            if (mTraceOutput != null) {
                try {
                    mTraceOutput.close();
                    mTraceOutput = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class XMLGenerator {
        String mOutputPath;

        /**
         * This document is used to represent the description XML file.
         * It is construct by the classes passed in, which contains the
         * information of all the test package, test suite and test cases.
         */
        Document mDoc;

        XMLGenerator(String outputPath) throws ParserConfigurationException {
            mOutputPath = outputPath;

            mDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            Node testPackageElem = mDoc.appendChild(mDoc.createElement(TAG_PACKAGE));

            setAttribute(testPackageElem, ATTRIBUTE_NAME_VERSION, ATTRIBUTE_VALUE_VERSION);
            setAttribute(testPackageElem, ATTRIBUTE_NAME_FRAMEWORK, ATTRIBUTE_VALUE_FRAMEWORK);
        }

        void addTestClass(TestClass tc) {
            appendSuiteToElement(mDoc.getDocumentElement(), tc);
        }

        void dump() throws TransformerFactoryConfigurationError,
                FileNotFoundException, TransformerException {
            //rebuildDocument();

            Transformer t = TransformerFactory.newInstance().newTransformer();

            // enable indent in result file
            t.setOutputProperty("indent", "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","4");

            File file = new File(mOutputPath);
            file.getParentFile().mkdirs();

            t.transform(new DOMSource(mDoc),
                    new StreamResult(new FileOutputStream(file)));
        }

        /**
         * Rebuild the document, merging empty suite nodes.
         */
        void rebuildDocument() {
            // merge empty suite nodes
            Collection<Node> suiteElems = getUnmutableChildNodes(mDoc.getDocumentElement());
            Iterator<Node> suiteIterator = suiteElems.iterator();
            while (suiteIterator.hasNext()) {
                Node suiteElem = suiteIterator.next();

                mergeEmptySuites(suiteElem);
            }
        }

        /**
         * Merge the test suite which only has one sub-suite. In this case, unify
         * the name of the two test suites.
         *
         * @param suiteElem The suite element of which to be merged.
         */
        void mergeEmptySuites(Node suiteElem) {
            Collection<Node> suiteChildren = getSuiteChildren(suiteElem);
            if (suiteChildren.size() > 1) {
                for (Node suiteChild : suiteChildren) {
                    mergeEmptySuites(suiteChild);
                }
            } else if (suiteChildren.size() == 1) {
                // do merge
                Node child = suiteChildren.iterator().next();

                // update name
                String newName = getAttribute(suiteElem, ATTRIBUTE_NAME) + "."
                        + getAttribute(child, ATTRIBUTE_NAME);
                setAttribute(child, ATTRIBUTE_NAME, newName);

                // update parent node
                Node parentNode = suiteElem.getParentNode();
                parentNode.removeChild(suiteElem);
                parentNode.appendChild(child);

                mergeEmptySuites(child);
            }
        }

        /**
         * Get the unmuatable child nodes for specified node.
         *
         * @param node The specified node.
         * @return A collection of copied child node.
         */
        private Collection<Node> getUnmutableChildNodes(Node node) {
            ArrayList<Node> nodes = new ArrayList<Node>();
            NodeList nodelist = node.getChildNodes();

            for (int i = 0; i < nodelist.getLength(); i++) {
                nodes.add(nodelist.item(i));
            }

            return nodes;
        }

        /**
         * Append a named test suite to a specified element. Including match with
         * the existing suite nodes and do the real creation and append.
         *
         * @param elem The specified element.
         * @param testSuite The test suite to be appended.
         */
        void appendSuiteToElement(Node elem, TestClass testSuite) {
            String suiteName = testSuite.mName;
            Collection<Node> children = getSuiteChildren(elem);
            int dotIndex = suiteName.indexOf('.');
            String name = dotIndex == -1 ? suiteName : suiteName.substring(0, dotIndex);

            boolean foundMatch = false;
            for (Node child : children) {
                String childName = child.getAttributes().getNamedItem(ATTRIBUTE_NAME)
                        .getNodeValue();

                if (childName.equals(name)) {
                    foundMatch = true;
                    if (dotIndex == -1) {
                        appendTestCases(child, testSuite.mCases);
                    } else {
                        testSuite.mName = suiteName.substring(dotIndex + 1, suiteName.length());
                        appendSuiteToElement(child, testSuite);
                    }
                }

            }

            if (!foundMatch) {
                appendSuiteToElementImpl(elem, testSuite);
            }
        }

        /**
         * Get the test suite child nodes of a specified element.
         *
         * @param elem The specified element node.
         * @return The matched child nodes.
         */
        Collection<Node> getSuiteChildren(Node elem) {
            ArrayList<Node> suites = new ArrayList<Node>();

            NodeList children = elem.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                if (child.getNodeName().equals(DescriptionGenerator.TAG_SUITE)) {
                    suites.add(child);
                }
            }

            return suites;
        }

        /**
         * Create test case node according to the given method names, and append them
         * to the test suite element.
         *
         * @param elem The test suite element.
         * @param cases A collection of test cases included by the test suite class.
         */
        void appendTestCases(Node elem, Collection<TestMethod> cases) {
            if (cases.isEmpty()) {
                // if no method, remove from parent
                elem.getParentNode().removeChild(elem);
            } else {
                for (TestMethod caze : cases) {
                    if (caze.mIsBroken || caze.mIsSuppressed || caze.mKnownFailure != null) {
                        continue;
                    }
                    Node caseNode = elem.appendChild(mDoc.createElement(TAG_TEST));

                    setAttribute(caseNode, ATTRIBUTE_NAME, caze.mName);
                    if ((caze.mController != null) && (caze.mController.length() != 0)) {
                        setAttribute(caseNode, ATTRIBUTE_HOST_CONTROLLER, caze.mController);
                    }

                    if (caze.mDescription != null && !caze.mDescription.equals("")) {
                        caseNode.appendChild(mDoc.createElement(TAG_DESCRIPTION))
                                .setTextContent(caze.mDescription);
                    }
                }
            }
        }

        /**
         * Set the attribute of element.
         *
         * @param elem The element to be set attribute.
         * @param name The attribute name.
         * @param value The attribute value.
         */
        protected void setAttribute(Node elem, String name, String value) {
            Attr attr = mDoc.createAttribute(name);
            attr.setNodeValue(value);

            elem.getAttributes().setNamedItem(attr);
        }

        /**
         * Get the value of a specified attribute of an element.
         *
         * @param elem The element node.
         * @param name The attribute name.
         * @return The value of the specified attribute.
         */
        private String getAttribute(Node elem, String name) {
            return elem.getAttributes().getNamedItem(name).getNodeValue();
        }

        /**
         * Do the append, including creating test suite nodes and test case nodes, and
         * append them to the element.
         *
         * @param elem The specified element node.
         * @param testSuite The test suite to be append.
         */
        void appendSuiteToElementImpl(Node elem, TestClass testSuite) {
            Node parent = elem;
            String suiteName = testSuite.mName;

            int dotIndex;
            while ((dotIndex = suiteName.indexOf('.')) != -1) {
                String name = suiteName.substring(0, dotIndex);

                Node suiteElem = parent.appendChild(mDoc.createElement(TAG_SUITE));
                setAttribute(suiteElem, ATTRIBUTE_NAME, name);

                parent = suiteElem;
                suiteName = suiteName.substring(dotIndex + 1, suiteName.length());
            }

            Node leafSuiteElem = parent.appendChild(mDoc.createElement(TAG_CASE));
            setAttribute(leafSuiteElem, ATTRIBUTE_NAME, suiteName);

            appendTestCases(leafSuiteElem, testSuite.mCases);
        }
    }

    /**
     * Represent the test class.
     */
    static class TestClass {
        String mName;
        Collection<TestMethod> mCases;

        /**
         * Construct an test suite object.
         *
         * @param name Full name of the test suite, such as "com.google.android.Foo"
         * @param cases The test cases included in this test suite.
         */
        TestClass(String name, Collection<TestMethod> cases) {
            mName = name;
            mCases = cases;
        }

        /**
         * Construct a TestClass object using ClassDoc.
         *
         * @param clazz The specified ClassDoc.
         */
        TestClass(ClassDoc clazz, ExpectationStore expectationStore) {
            mName = clazz.toString();
            mCases = getTestMethods(expectationStore, clazz);
        }

        /**
         * Get all the TestMethod from a ClassDoc, including inherited methods.
         *
         * @param clazz The specified ClassDoc.
         * @return A collection of TestMethod.
         */
        Collection<TestMethod> getTestMethods(ExpectationStore expectationStore, ClassDoc clazz) {
            Collection<MethodDoc> methods = getAllMethods(clazz);

            ArrayList<TestMethod> cases = new ArrayList<TestMethod>();
            Iterator<MethodDoc> iterator = methods.iterator();

            while (iterator.hasNext()) {
                MethodDoc method = iterator.next();

                String name = method.name();

                AnnotationDesc[] annotations = method.annotations();
                String controller = "";
                String knownFailure = null;
                boolean isBroken = false;
                boolean isSuppressed = false;
                for (AnnotationDesc cAnnot : annotations) {

                    AnnotationTypeDoc atype = cAnnot.annotationType();
                    if (atype.toString().equals(HOST_CONTROLLER)) {
                        controller = getAnnotationDescription(cAnnot);
                    } else if (atype.toString().equals(KNOWN_FAILURE)) {
                        knownFailure = getAnnotationDescription(cAnnot);
                    } else if (atype.toString().equals(BROKEN_TEST)) {
                        isBroken = true;
                    } else if (atype.toString().equals(SUPPRESSED_TEST)) {
                        isSuppressed = true;
                    }
                }

                if (VogarUtils.isVogarKnownFailure(expectationStore, clazz.toString(), name)) {
                    isBroken = true;
                }

                if (name.startsWith("test")) {
                    cases.add(new TestMethod(name, method.commentText(), controller, knownFailure,
                            isBroken, isSuppressed));
                }
            }

            return cases;
        }

        /**
         * Get annotation description.
         *
         * @param cAnnot The annotation.
         */
        String getAnnotationDescription(AnnotationDesc cAnnot) {
            ElementValuePair[] cpairs = cAnnot.elementValues();
            ElementValuePair evp = cpairs[0];
            AnnotationValue av = evp.value();
            String description = av.toString();
            // FIXME: need to find out the reason why there are leading and trailing "
            description = description.substring(1, description.length() -1);
            return description;
        }

        /**
         * Get all MethodDoc of a ClassDoc, including inherited methods.
         *
         * @param clazz The specified ClassDoc.
         * @return A collection of MethodDoc.
         */
        Collection<MethodDoc> getAllMethods(ClassDoc clazz) {
            ArrayList<MethodDoc> methods = new ArrayList<MethodDoc>();

            for (MethodDoc method : clazz.methods()) {
                methods.add(method);
            }

            ClassDoc superClass = clazz.superclass();
            while (superClass != null) {
                for (MethodDoc method : superClass.methods()) {
                    methods.add(method);
                }

                superClass = superClass.superclass();
            }

            return methods;
        }

    }

    /**
     * Represent the test method inside the test class.
     */
    static class TestMethod {
        String mName;
        String mDescription;
        String mController;
        String mKnownFailure;
        boolean mIsBroken;
        boolean mIsSuppressed;

        /**
         * Construct an test case object.
         *
         * @param name The name of the test case.
         * @param description The description of the test case.
         * @param knownFailure The reason of known failure.
         */
        TestMethod(String name, String description, String controller, String knownFailure,
                boolean isBroken, boolean isSuppressed) {
            mName = name;
            mDescription = description;
            mController = controller;
            mKnownFailure = knownFailure;
            mIsBroken = isBroken;
            mIsSuppressed = isSuppressed;
        }
    }
}

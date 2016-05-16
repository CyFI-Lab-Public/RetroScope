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
package com.android.cts.javascannerdoclet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
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

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationTypeElementDoc;

/**
 * Doclet that outputs in the following format:
 *
 * suite:android.holo.cts
 * case:HoloTest
 * test:testHolo
 * test:testHoloDialog[:timeout_value]
 */
public class CtsJavaScannerDoclet extends Doclet {

    static final String JUNIT_TEST_CASE_CLASS_NAME = "junit.framework.testcase";

    public static boolean start(RootDoc root) {
        ClassDoc[] classes = root.classes();
        if (classes == null) {
            return false;
        }

        PrintWriter writer = new PrintWriter(System.out);

        for (ClassDoc clazz : classes) {
            if (clazz.isAbstract() || !isValidJUnitTestCase(clazz)) {
                continue;
            }
            writer.append("suite:").println(clazz.containingPackage().name());
            writer.append("case:").println(clazz.name());
            for (; clazz != null; clazz = clazz.superclass()) {
                for (MethodDoc method : clazz.methods()) {
                    if (!method.name().startsWith("test")) {
                        continue;
                    }
                    int timeout = -1;
                    AnnotationDesc[] annotations = method.annotations();
                    for (AnnotationDesc annot : annotations) {
                        AnnotationTypeDoc atype = annot.annotationType();
                        if (atype.toString().equals("com.android.cts.util.TimeoutReq")) {
                            ElementValuePair[] cpairs = annot.elementValues();
                            for (ElementValuePair pair: cpairs) {
                                AnnotationTypeElementDoc elem = pair.element();
                                AnnotationValue value = pair.value();
                                if (elem.name().equals("minutes")) {
                                    timeout = ((Integer)value.value());
                                }
                            }
                        }
                    }
                    writer.append("test:");
                    if (timeout >= 0) {
                        writer.append(method.name()).println(":" + timeout);
                    } else {
                        writer.println(method.name());
                    }
                }
            }
        }

        writer.close();
        return true;
    }

    private static boolean isValidJUnitTestCase(ClassDoc clazz) {
        while((clazz = clazz.superclass()) != null) {
            if (JUNIT_TEST_CASE_CLASS_NAME.equals(clazz.qualifiedName().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}

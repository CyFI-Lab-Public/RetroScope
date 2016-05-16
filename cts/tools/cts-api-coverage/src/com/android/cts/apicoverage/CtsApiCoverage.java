/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.apicoverage;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

/**
 * Tool that generates a report of what Android framework methods are being called from a given
 * set of APKS. See the {@link #printUsage()} method for more details.
 */
public class CtsApiCoverage {

    private static final int FORMAT_TXT = 0;

    private static final int FORMAT_XML = 1;

    private static final int FORMAT_HTML = 2;

    private static void printUsage() {
        System.out.println("Usage: cts-api-coverage [OPTION]... [APK]...");
        System.out.println();
        System.out.println("Generates a report about what Android framework methods are called ");
        System.out.println("from the given APKs.");
        System.out.println();
        System.out.println("Use the Makefiles rules in CtsTestCoverage.mk to generate the report ");
        System.out.println("rather than executing this directly. If you still want to run this ");
        System.out.println("directly, then this must be used from the $ANDROID_BUILD_TOP ");
        System.out.println("directory and dexdeps must be built via \"make dexdeps\".");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o FILE                output file or standard out if not given");
        System.out.println("  -f [txt|xml|html]      format of output");
        System.out.println("  -d PATH                path to dexdeps or expected to be in $PATH");
        System.out.println("  -a PATH                path to the API XML file");
        System.out.println("  -p PACKAGENAMEPREFIX   report coverage only for package that start with");
        System.out.println("  -t TITLE               report title");
        System.out.println();
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        List<File> testApks = new ArrayList<File>();
        File outputFile = null;
        int format = FORMAT_TXT;
        String dexDeps = "dexDeps";
        String apiXmlPath = "";
        // By default only care about packages starting with "android"
        String packageFilter = "android";
        String reportTitle = "CTS API Coverage";

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if ("-o".equals(args[i])) {
                    outputFile = new File(getExpectedArg(args, ++i));
                } else if ("-f".equals(args[i])) {
                    String formatSpec = getExpectedArg(args, ++i);
                    if ("xml".equalsIgnoreCase(formatSpec)) {
                        format = FORMAT_XML;
                    } else if ("txt".equalsIgnoreCase(formatSpec)) {
                        format = FORMAT_TXT;
                    } else if ("html".equalsIgnoreCase(formatSpec)) {
                        format = FORMAT_HTML;
                    } else {
                        printUsage();
                    }
                } else if ("-d".equals(args[i])) {
                    dexDeps = getExpectedArg(args, ++i);
                } else if ("-a".equals(args[i])) {
                    apiXmlPath = getExpectedArg(args, ++i);
                } else if ("-p".equals(args[i])) {
                    packageFilter = getExpectedArg(args, ++i);
                } else if ("-t".equals(args[i])) {
                    reportTitle = getExpectedArg(args, ++i);
                } else {
                    printUsage();
                }
            } else {
                testApks.add(new File(args[i]));
            }
        }

        /*
         * 1. Create an ApiCoverage object that is a tree of Java objects representing the API
         *    in current.xml. The object will have no information about the coverage for each
         *    constructor or method yet.
         *
         * 2. For each provided APK, scan it using dexdeps, parse the output of dexdeps, and
         *    call methods on the ApiCoverage object to cumulatively add coverage stats.
         *
         * 3. Output a report based on the coverage stats in the ApiCoverage object.
         */

        ApiCoverage apiCoverage = getEmptyApiCoverage(apiXmlPath);
        apiCoverage.removeEmptyAbstractClasses();
        for (File testApk : testApks) {
            addApiCoverage(apiCoverage, testApk, dexDeps);
        }
        outputCoverageReport(apiCoverage, testApks, outputFile, format, packageFilter, reportTitle);
    }

    /** Get the argument or print out the usage and exit. */
    private static String getExpectedArg(String[] args, int index) {
        if (index < args.length) {
            return args[index];
        } else {
            printUsage();
            return null;    // Never will happen because printUsage will call exit(1)
        }
    }

    /**
     * Creates an object representing the API that will be used later to collect coverage
     * statistics as we iterate over the test APKs.
     *
     * @param apiXmlPath to the API XML file
     * @return an {@link ApiCoverage} object representing the API in current.xml without any
     *     coverage statistics yet
     */
    private static ApiCoverage getEmptyApiCoverage(String apiXmlPath)
            throws SAXException, IOException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        CurrentXmlHandler currentXmlHandler = new CurrentXmlHandler();
        xmlReader.setContentHandler(currentXmlHandler);

        File currentXml = new File(apiXmlPath);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(currentXml);
            xmlReader.parse(new InputSource(fileReader));
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
        }

        return currentXmlHandler.getApi();
    }

    /**
     * Adds coverage information gleamed from running dexdeps on the APK to the
     * {@link ApiCoverage} object.
     *
     * @param apiCoverage object to which the coverage statistics will be added to
     * @param testApk containing the tests that will be scanned by dexdeps
     */
    private static void addApiCoverage(ApiCoverage apiCoverage, File testApk, String dexdeps)
            throws SAXException, IOException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        DexDepsXmlHandler dexDepsXmlHandler = new DexDepsXmlHandler(apiCoverage);
        xmlReader.setContentHandler(dexDepsXmlHandler);

        Process process = new ProcessBuilder(dexdeps, "--format=xml", testApk.getPath()).start();
        xmlReader.parse(new InputSource(process.getInputStream()));
    }

    private static void outputCoverageReport(ApiCoverage apiCoverage, List<File> testApks,
            File outputFile, int format, String packageFilter, String reportTitle)
                throws IOException, TransformerException, InterruptedException {

        OutputStream out = outputFile != null
                ? new FileOutputStream(outputFile)
                : System.out;

        try {
            switch (format) {
                case FORMAT_TXT:
                    TextReport.printTextReport(apiCoverage, packageFilter, out);
                    break;

                case FORMAT_XML:
                    XmlReport.printXmlReport(testApks, apiCoverage, packageFilter, reportTitle, out);
                    break;

                case FORMAT_HTML:
                    HtmlReport.printHtmlReport(testApks, apiCoverage, packageFilter, reportTitle, out);
                    break;
            }
        } finally {
            out.close();
        }
    }
}

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
package com.android.cts.tradefed.testtype;

import com.android.ddmlib.Log;
import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Retrieves CTS test package definitions from the repository.
 */
public class TestPackageRepo implements ITestPackageRepo {

    private static final String LOG_TAG = "TestCaseRepo";

    private final File mTestCaseDir;

    /** mapping of uri to test definition */
    private final Map<String, TestPackageDef> mTestMap;

    private final boolean mIncludeKnownFailures;

    /**
     * Creates a {@link TestPackageRepo}, initialized from provided repo files
     *
     * @param testCaseDir directory containing all test case definition xml and build files
     */
    public TestPackageRepo(File testCaseDir, boolean includeKnownFailures) {
        mTestCaseDir = testCaseDir;
        mTestMap = new Hashtable<String, TestPackageDef>();
        mIncludeKnownFailures = includeKnownFailures;
        parse(mTestCaseDir);
    }

    /**
     * Builds mTestMap based on directory contents
     */
    private void parse(File dir) {
        File[] xmlFiles = dir.listFiles(new XmlFilter());
        for (File xmlFile : xmlFiles) {
            parseTestFromXml(xmlFile);
        }
    }

    private void parseTestFromXml(File xmlFile)  {
        TestPackageXmlParser parser = new TestPackageXmlParser(mIncludeKnownFailures);
        try {
            parser.parse(createStreamFromFile(xmlFile));
            TestPackageDef def = parser.getTestPackageDef();
            if (def != null) {
                mTestMap.put(def.getUri(), def);
            } else {
                Log.w(LOG_TAG, String.format("Could not find test package info in xml file %s",
                        xmlFile.getAbsolutePath()));
            }
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, String.format("Could not find test case xml file %s",
                    xmlFile.getAbsolutePath()));
            Log.e(LOG_TAG, e);
        } catch (ParseException e) {
            Log.e(LOG_TAG, String.format("Failed to parse test case xml file %s",
                    xmlFile.getAbsolutePath()));
            Log.e(LOG_TAG, e);
        }
    }

    /**
     * Helper method to create a stream to read data from given file
     * <p/>
     * Exposed for unit testing
     *
     * @param xmlFile
     * @return stream to read data
     *
     */
    InputStream createStreamFromFile(File xmlFile) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(xmlFile));
    }

    private static class XmlFilter implements FilenameFilter {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".xml");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestPackageDef getTestPackage(String testUri) {
        return mTestMap.get(testUri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String findPackageForTest(String testClassName) {
        for (Map.Entry<String, TestPackageDef> entry : mTestMap.entrySet()) {
            if (entry.getValue().isKnownTestClass(testClassName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * @return list of all package names found in repo
     */
    @Override
    public Collection<String> getPackageNames() {
        List<String> packageNames = new ArrayList<String>();
        packageNames.addAll(mTestMap.keySet());
        Collections.sort(packageNames);
        return packageNames;
    }
}

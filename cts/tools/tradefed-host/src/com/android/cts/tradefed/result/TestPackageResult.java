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
package com.android.cts.tradefed.result;

import android.tests.getinfo.DeviceInfoConstants;

import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.cts.tradefed.testtype.CtsTest;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.log.LogUtil.CLog;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data structure for a CTS test package result.
 * <p/>
 * Provides methods to serialize to XML.
 */
class TestPackageResult  extends AbstractXmlPullParser {

    static final String TAG = "TestPackage";
    private static final String DIGEST_ATTR = "digest";
    private static final String APP_PACKAGE_NAME_ATTR = "appPackageName";
    private static final String NAME_ATTR = "name";
    private static final String ns = CtsXmlResultReporter.ns;
    private static final String SIGNATURE_TEST_PKG = "android.tests.sigtest";

    private String mAppPackageName;
    private String mName;
    private String mDigest;

    private Map<String, String> mMetrics = new HashMap<String, String>();

    private TestSuite mSuiteRoot = new TestSuite(null);

    public void setAppPackageName(String appPackageName) {
        mAppPackageName = appPackageName;
    }

    public String getAppPackageName() {
        return mAppPackageName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setDigest(String digest) {
        mDigest = digest;
    }

    public String getDigest() {
        return mDigest;
    }

    /**
     * Return the {@link TestSuite}s
     */
    public Collection<TestSuite> getTestSuites() {
        return mSuiteRoot.getTestSuites();
    }

    /**
     * Adds a test result to this test package
     *
     * @param testId
     * @param testResult
     */
    public Test insertTest(TestIdentifier testId) {
        return findTest(testId, true);
    }

    private Test findTest(TestIdentifier testId, boolean insertIfMissing) {
        List<String> classNameSegments = new LinkedList<String>();
        Collections.addAll(classNameSegments, testId.getClassName().split("\\."));
        if (classNameSegments.size() <= 0) {
            CLog.e("Unrecognized package name format for test class '%s'",
                    testId.getClassName());
            // should never happen
            classNameSegments.add("UnknownTestClass");
        }
            String testCaseName = classNameSegments.remove(classNameSegments.size()-1);
            return mSuiteRoot.findTest(classNameSegments, testCaseName, testId.getTestName(), insertIfMissing);
    }


    /**
     * Find the test result for given {@link TestIdentifier}.
     * @param testId
     * @return the {@link Test} or <code>null</code>
     */
    public Test findTest(TestIdentifier testId) {
        return findTest(testId, false);
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, TAG);
        serializeAttribute(serializer, NAME_ATTR, mName);
        serializeAttribute(serializer, APP_PACKAGE_NAME_ATTR, mAppPackageName);
        serializeAttribute(serializer, DIGEST_ATTR, getDigest());
        if (SIGNATURE_TEST_PKG.equals(mName)) {
            serializer.attribute(ns, "signatureCheck", "true");
        }
        mSuiteRoot.serialize(serializer);
        serializer.endTag(ns, TAG);
    }

    /**
     * Helper method to serialize attributes.
     * Can handle null values. Useful for cases where test package has not been fully populated
     * such as when unit testing.
     *
     * @param attrName
     * @param attrValue
     * @throws IOException
     */
    private void serializeAttribute(KXmlSerializer serializer, String attrName, String attrValue)
            throws IOException {
        attrValue = attrValue == null ? "" : attrValue;
        serializer.attribute(ns, attrName, attrValue);
    }

    /**
     * Populates this class with package result data parsed from XML.
     *
     * @param parser the {@link XmlPullParser}. Expected to be pointing at start
     *            of TestPackage tag
     */
    @Override
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (!parser.getName().equals(TAG)) {
            throw new XmlPullParserException(String.format(
                    "invalid XML: Expected %s tag but received %s", TAG, parser.getName()));
        }
        setAppPackageName(getAttribute(parser, APP_PACKAGE_NAME_ATTR));
        setName(getAttribute(parser, NAME_ATTR));
        setDigest(getAttribute(parser, DIGEST_ATTR));
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(TestSuite.TAG)) {
                TestSuite suite = new TestSuite();
                suite.parse(parser);
                mSuiteRoot.insertSuite(suite);
            }
            if (eventType == XmlPullParser.END_TAG && parser.getName().equals(TAG)) {
                return;
            }
            eventType = parser.next();
        }
    }

    /**
     * Return a list of {@link TestIdentifer}s contained in this result with the given status
     *
     * @param resultFilter the {@link CtsTestStatus} to filter by
     * @return a collection of {@link TestIdentifer}s
     */
    public Collection<TestIdentifier> getTestsWithStatus(CtsTestStatus resultFilter) {
        Collection<TestIdentifier> tests = new LinkedList<TestIdentifier>();
        Deque<String> suiteNames = new LinkedList<String>();
        mSuiteRoot.addTestsWithStatus(tests, suiteNames, resultFilter);
        return tests;
    }

    /**
     * Populate values in this package result from run metrics
     * @param runResult
     */
    public void populateMetrics(Map<String, String> metrics) {
        String name = metrics.get(CtsTest.PACKAGE_NAME_METRIC);
        if (name != null) {
            setName(name);
        }
        String digest = metrics.get(CtsTest.PACKAGE_DIGEST_METRIC);
        if (digest != null) {
            setDigest(digest);
        }
        if (DeviceInfoCollector.APP_PACKAGE_NAME.equals(getAppPackageName())) {
            storeDeviceMetrics(metrics);
        } else {
            mMetrics.putAll(metrics);
        }
    }

    /**
     * Check that the provided device info metrics are consistent with the currently stored metrics.
     * <p/>
     * If any inconsistencies occur, logs errors and stores error messages in the metrics map
     *
     * @param metrics
     */
    private void storeDeviceMetrics(Map<String, String> metrics) {
        // TODO centralize all the device metrics handling into a single class
        if (mMetrics.isEmpty()) {
            // nothing to check!
            mMetrics.putAll(metrics);
            return;
        }
        // ensure all the metrics we expect to be identical actually are
        checkMetrics(metrics, DeviceInfoConstants.BUILD_FINGERPRINT,
                DeviceInfoConstants.BUILD_MODEL, DeviceInfoConstants.BUILD_BRAND,
                DeviceInfoConstants.BUILD_MANUFACTURER, DeviceInfoConstants.BUILD_BOARD,
                DeviceInfoConstants.BUILD_DEVICE, DeviceInfoConstants.PRODUCT_NAME,
                DeviceInfoConstants.BUILD_ABI, DeviceInfoConstants.BUILD_ABI2,
                DeviceInfoConstants.SCREEN_SIZE);
    }

    private void checkMetrics(Map<String, String> metrics, String... keysToCheck) {
        Set<String> keyCheckSet = new HashSet<String>();
        Collections.addAll(keyCheckSet, keysToCheck);
        for (Map.Entry<String, String> metricEntry : metrics.entrySet()) {
            String currentValue = mMetrics.get(metricEntry.getKey());
            if (keyCheckSet.contains(metricEntry.getKey()) && currentValue != null
                    && !metricEntry.getValue().equals(currentValue)) {
                CLog.e("Inconsistent info collected from devices. "
                        + "Current result has %s='%s', Received '%s'. Are you sharding or " +
                        "resuming a test run across different devices and/or builds?",
                        metricEntry.getKey(), currentValue, metricEntry.getValue());
                mMetrics.put(metricEntry.getKey(),
                        String.format("ERROR: Inconsistent results: %s, %s",
                                metricEntry.getValue(), currentValue));
            } else {
                mMetrics.put(metricEntry.getKey(), metricEntry.getValue());
            }
        }
    }

    /**
     * Report the given test as a failure.
     *
     * @param test
     * @param status
     * @param trace
     */
    public void reportTestFailure(TestIdentifier test, CtsTestStatus status, String trace) {
        Test result = findTest(test);
        result.setResultStatus(status);
        result.setStackTrace(trace);
    }

    /**
     * report performance result
     * @param test
     * @param status
     * @param perf
     */
    public void reportPerformanceResult(TestIdentifier test, CtsTestStatus status, String summary, String details) {
        Test result = findTest(test);
        result.setResultStatus(status);
        result.setSummary(summary);
        result.setDetails(details);
    }

    /**
     * Report that the given test has completed.
     *
     * @param test
     */
    public void reportTestEnded(TestIdentifier test) {
        Test result = findTest(test);
        if (!result.getResult().equals(CtsTestStatus.FAIL)) {
            result.setResultStatus(CtsTestStatus.PASS);
        }
        result.updateEndTime();
    }

    /**
     * Return the number of tests with given status
     *
     * @param status
     * @return the total number of tests with given status
     */
    public int countTests(CtsTestStatus status) {
        return mSuiteRoot.countTests(status);
    }

    /**
     * @return
     */
    public Map<String, String> getMetrics() {
        return mMetrics;
    }
}

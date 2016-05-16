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

import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.testtype.InstrumentationTest;
import com.android.tradefed.util.StreamUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Container for CTS test info.
 * <p/>
 * Knows how to translate this info into a runnable {@link IRemoteTest}.
 */
class TestPackageDef implements ITestPackageDef {

    public static final String HOST_SIDE_ONLY_TEST = "hostSideOnly";
    public static final String NATIVE_TEST = "native";
    public static final String WRAPPED_NATIVE_TEST = "wrappednative";
    public static final String VM_HOST_TEST = "vmHostTest";
    public static final String ACCESSIBILITY_TEST =
        "com.android.cts.tradefed.testtype.AccessibilityTestRunner";
    public static final String ACCESSIBILITY_SERVICE_TEST =
        "com.android.cts.tradefed.testtype.AccessibilityServiceTestRunner";
    public static final String DISPLAY_TEST =
            "com.android.cts.tradefed.testtype.DisplayTestRunner";
    public static final String UIAUTOMATOR_TEST = "uiAutomator";

    private static final String SIGNATURE_TEST_METHOD = "testSignature";
    private static final String SIGNATURE_TEST_CLASS = "android.tests.sigtest.SignatureTest";

    private String mUri = null;
    private String mAppNameSpace = null;
    private String mName = null;
    private String mRunner = null;
    private boolean mIsVMHostTest = false;
    private String mTestType = null;
    private String mJarPath = null;
    private boolean mIsSignatureTest = false;
    private String mTestPackageName = null;
    private String mDigest = null;

    // use a LinkedHashSet for predictable iteration insertion-order, and fast
    // lookups
    private Collection<TestIdentifier> mTests = new LinkedHashSet<TestIdentifier>();
    // also maintain an index of known test classes
    private Collection<String> mTestClasses = new LinkedHashSet<String>();

    // dynamic options, not parsed from package xml
    private String mClassName;
    private String mMethodName;
    private TestFilter mExcludedTestFilter = new TestFilter();
    private String mTargetBinaryName;
    private String mTargetNameSpace;
    // only timeout per package is supported. To change this to method granularity,
    // test invocation should be done in method level.
    // So for now, only max timeout for the package is used.
    private int mTimeoutInMins = -1;

    void setUri(String uri) {
        mUri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUri() {
        return mUri;
    }

    void setAppNameSpace(String appNameSpace) {
        mAppNameSpace = appNameSpace;
    }

    String getAppNameSpace() {
        return mAppNameSpace;
    }

    void setName(String name) {
        mName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return mName;
    }

    void setRunner(String runnerName) {
        mRunner = runnerName;
    }

    String getRunner() {
        return mRunner;
    }

    void setTestType(String testType) {
        mTestType = testType;
    }

    String getTestType() {
        return mTestType;
    }

    void setJarPath(String jarPath) {
        mJarPath = jarPath;
    }

    String getJarPath() {
        return mJarPath;
    }

    void setIsSignatureCheck(boolean isSignatureCheckTest) {
        mIsSignatureTest = isSignatureCheckTest;
    }

    boolean isSignatureCheck() {
        return mIsSignatureTest;
    }

    void setTestPackageName(String testPackageName) {
        mTestPackageName = testPackageName;
    }

    void setTargetBinaryName(String targetBinaryName) {
        mTargetBinaryName = targetBinaryName;
    }

    void setTargetNameSpace(String targetNameSpace) {
        mTargetNameSpace = targetNameSpace;
    }

    @Override
    public String getTargetApkName() {
       if (mTargetBinaryName != null && !mTargetBinaryName.isEmpty()) {
           return String.format("%s.apk", mTargetBinaryName);
       }
       return null;
    }

    @Override
    public String getTargetPackageName() {
        if (mTargetNameSpace != null && mTargetNameSpace.isEmpty()) {
            return null;
        }
        return mTargetNameSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExcludedTestFilter(TestFilter excludeFilter) {
        mExcludedTestFilter = excludeFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClassName(String className, String methodName) {
        mClassName = className;
        mMethodName = methodName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRemoteTest createTest(File testCaseDir) {
        mExcludedTestFilter.setTestInclusion(mClassName, mMethodName);
        mTests = filterTests();

        if (HOST_SIDE_ONLY_TEST.equals(mTestType)) {
            CLog.d("Creating host test for %s", mName);
            JarHostTest hostTest = new JarHostTest();
            if (mTimeoutInMins >= 0) {
                CLog.d("Setting new timeout to " + mTimeoutInMins + " mins");
                hostTest.setTimeout(mTimeoutInMins * 60 * 1000);
            }
            hostTest.setRunName(getUri());
            hostTest.setJarFileName(mJarPath);
            hostTest.setTests(mTests);
            mDigest = generateDigest(testCaseDir, mJarPath);
            return hostTest;
        } else if (VM_HOST_TEST.equals(mTestType)) {
            CLog.d("Creating vm host test for %s", mName);
            VMHostTest vmHostTest = new VMHostTest();
            vmHostTest.setRunName(getUri());
            vmHostTest.setJarFileName(mJarPath);
            vmHostTest.setTests(mTests);
            mDigest = generateDigest(testCaseDir, mJarPath);
            return vmHostTest;
        } else if (NATIVE_TEST.equals(mTestType)) {
            return new GeeTest(mUri, mName);
        } else if (WRAPPED_NATIVE_TEST.equals(mTestType)) {
            CLog.d("Creating new wrapped native test for %s", mName);
            return new WrappedGTest(mAppNameSpace, mUri, mName, mRunner);
        } else if (ACCESSIBILITY_TEST.equals(mTestType)) {
            AccessibilityTestRunner test = new AccessibilityTestRunner();
            return setInstrumentationTest(test, testCaseDir);
        } else if (ACCESSIBILITY_SERVICE_TEST.equals(mTestType)) {
            AccessibilityServiceTestRunner test = new AccessibilityServiceTestRunner();
            return setInstrumentationTest(test, testCaseDir);
        } else if (DISPLAY_TEST.equals(mTestType)) {
            DisplayTestRunner test = new DisplayTestRunner();
            return setInstrumentationTest(test, testCaseDir);
        } else if (UIAUTOMATOR_TEST.equals(mTestType)) {
            UiAutomatorJarTest uiautomatorTest = new UiAutomatorJarTest();
            return setUiAutomatorTest(uiautomatorTest);
        } else if (mIsSignatureTest) {
            // TODO: hardcode the runner/class/method for now, since current package xml points to
            // specialized instrumentation. Eventually this special case for signatureTest can be
            // removed, and it can be treated just like a normal InstrumentationTest
            CLog.d("Creating signature test %s", mName);
            InstrumentationApkTest instrTest = new InstrumentationApkTest();
            instrTest.setPackageName(mAppNameSpace);
            instrTest.setRunnerName("android.test.InstrumentationTestRunner");
            instrTest.setClassName(SIGNATURE_TEST_CLASS);
            instrTest.setMethodName(SIGNATURE_TEST_METHOD);
            // set expected tests to the single signature test
            TestIdentifier t = new TestIdentifier(SIGNATURE_TEST_CLASS, SIGNATURE_TEST_METHOD);
            mTests.clear();
            mTests.add(t);
            // mName means 'apk file name' for instrumentation tests
            instrTest.addInstallApk(String.format("%s.apk", mName), mAppNameSpace);
            mDigest = generateDigest(testCaseDir, String.format("%s.apk", mName));
            return instrTest;
        } else {
            CLog.d("Creating instrumentation test for %s", mName);
            InstrumentationApkTest instrTest = new InstrumentationApkTest();
            if (mTimeoutInMins >= 0) {
                // as timeout cannot be set for each test,
                // increase the time-out of the whole package
                CLog.d("Setting new timeout to " + mTimeoutInMins + " mins");
                instrTest.setTestTimeout(mTimeoutInMins * 60 * 1000);
            }
            return setInstrumentationTest(instrTest, testCaseDir);
        }
    }

    /**
     * Populates given {@link InstrumentationApkTest} with data from the package xml.
     *
     * @param testCaseDir
     * @param instrTest
     * @return the populated {@link InstrumentationTest} or <code>null</code>
     */
    private InstrumentationTest setInstrumentationTest(InstrumentationApkTest instrTest,
            File testCaseDir) {
        instrTest.setRunName(getUri());
        instrTest.setPackageName(mAppNameSpace);
        instrTest.setRunnerName(mRunner);
        instrTest.setTestPackageName(mTestPackageName);
        instrTest.setClassName(mClassName);
        instrTest.setMethodName(mMethodName);
        instrTest.setTestsToRun(mTests,
                !mExcludedTestFilter.hasExclusion()
                /* only force batch mode if no tests are excluded */);
        // mName means 'apk file name' for instrumentation tests
        instrTest.addInstallApk(String.format("%s.apk", mName), mAppNameSpace);
        mDigest = generateDigest(testCaseDir, String.format("%s.apk", mName));
        if (mTests.size() > 1000) {
            // TODO: hack, large test suites can take longer to collect tests, increase timeout
            instrTest.setCollectsTestsShellTimeout(10 * 60 * 1000);
        }
        return instrTest;
    }

    /**
     * Populates given {@link UiAutomatorJarTest} with data from the package xml.
     *
     * @param uiautomatorTest
     * @return the populated {@link UiAutomatorJarTest} or <code>null</code>
     */
    private IRemoteTest setUiAutomatorTest(UiAutomatorJarTest uiautomatorTest) {
        uiautomatorTest.setInstallArtifacts(getJarPath());
        if (mClassName != null) {
            if (mMethodName != null) {
                CLog.logAndDisplay(LogLevel.WARN, "ui automator tests don't currently support" +
                        "running  individual methods");
            }
            uiautomatorTest.addClassName(mClassName);
        } else {
            uiautomatorTest.addClassNames(mTestClasses);
        }
        uiautomatorTest.setRunName(getUri());
        uiautomatorTest.setCaptureLogs(false);
        return uiautomatorTest;
    }

    /**
     * Filter the tests to run based on list of excluded tests, class and method name.
     *
     * @return the filtered collection of tests
     */
    private Collection<TestIdentifier> filterTests() {
        mExcludedTestFilter.setTestInclusion(mClassName, mMethodName);
        return mExcludedTestFilter.filter(mTests);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isKnownTest(TestIdentifier testDef) {
        return mTests.contains(testDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isKnownTestClass(String className) {
        return mTestClasses.contains(className);
    }

    /**
     * Add a {@link TestIdentifier} to the list of tests in this package.
     *
     * @param testDef
     * @param timeout in mins
     */
    void addTest(TestIdentifier testDef, int timeout) {
        mTests.add(testDef);
        mTestClasses.add(testDef.getClassName());
        // 0 means no timeout, so keep 0 if already is.
        if ((timeout > mTimeoutInMins) && (mTimeoutInMins != 0)) {
            mTimeoutInMins = timeout;
        }
    }

    /**
     * Get the collection of tests in this test package.
     */
    @Override
    public Collection<TestIdentifier> getTests() {
        return mTests;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDigest() {
        return mDigest;
    }

    /**
     * Generate a sha1sum digest for a file.
     * <p/>
     * Exposed for unit testing.
     *
     * @param fileDir the directory of the file
     * @param fileName the name of the file
     * @return a hex {@link String} of the digest
     */
    String generateDigest(File fileDir, String fileName) {
        final String algorithm = "SHA-1";
        InputStream fileStream = null;
        DigestInputStream d = null;
        try {
            fileStream = getFileStream(fileDir, fileName);
            MessageDigest md = MessageDigest.getInstance(algorithm);
            d = new DigestInputStream(fileStream, md);
            byte[] buffer = new byte[8196];
            while (d.read(buffer) != -1) {
            }
            return toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return algorithm + " not found";
        } catch (IOException e) {
            CLog.e(e);
        } finally {
            StreamUtil.closeStream(d);
            StreamUtil.closeStream(fileStream);
        }
        return "failed to generate digest";
    }

    /**
     * Retrieve an input stream for given file
     * <p/>
     * Exposed so unit tests can mock.
     */
    InputStream getFileStream(File fileDir, String fileName) throws FileNotFoundException {
        InputStream fileStream;
        fileStream = new BufferedInputStream(new FileInputStream(new File(fileDir, fileName)));
        return fileStream;
    }

    /**
     * Convert the given byte array into a lowercase hex string.
     *
     * @param arr The array to convert.
     * @return The hex encoded string.
     */
    private String toHexString(byte[] arr) {
        StringBuffer buf = new StringBuffer(arr.length * 2);
        for (byte b : arr) {
            buf.append(String.format("%02x", b & 0xFF));
        }
        return buf.toString();
    }
}

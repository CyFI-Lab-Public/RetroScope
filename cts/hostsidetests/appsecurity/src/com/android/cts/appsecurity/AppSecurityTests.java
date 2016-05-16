/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.cts.appsecurity;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.Log;
import com.android.ddmlib.testrunner.InstrumentationResultParser;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.TestResult;
import com.android.tradefed.result.TestResult.TestStatus;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Set of tests that verify various security checks involving multiple apps are properly enforced.
 */
public class AppSecurityTests extends DeviceTestCase implements IBuildReceiver {

    // testSharedUidDifferentCerts constants
    private static final String SHARED_UI_APK = "CtsSharedUidInstall.apk";
    private static final String SHARED_UI_PKG = "com.android.cts.shareuidinstall";
    private static final String SHARED_UI_DIFF_CERT_APK = "CtsSharedUidInstallDiffCert.apk";
    private static final String SHARED_UI_DIFF_CERT_PKG =
        "com.android.cts.shareuidinstalldiffcert";

    // testAppUpgradeDifferentCerts constants
    private static final String SIMPLE_APP_APK = "CtsSimpleAppInstall.apk";
    private static final String SIMPLE_APP_PKG = "com.android.cts.simpleappinstall";
    private static final String SIMPLE_APP_DIFF_CERT_APK = "CtsSimpleAppInstallDiffCert.apk";

    // testAppFailAccessPrivateData constants
    private static final String APP_WITH_DATA_APK = "CtsAppWithData.apk";
    private static final String APP_WITH_DATA_PKG = "com.android.cts.appwithdata";
    private static final String APP_WITH_DATA_CLASS =
            "com.android.cts.appwithdata.CreatePrivateDataTest";
    private static final String APP_WITH_DATA_CREATE_METHOD =
            "testCreatePrivateData";
    private static final String APP_WITH_DATA_CHECK_NOEXIST_METHOD =
            "testEnsurePrivateDataNotExist";
    private static final String APP_ACCESS_DATA_APK = "CtsAppAccessData.apk";
    private static final String APP_ACCESS_DATA_PKG = "com.android.cts.appaccessdata";

    // External storage constants
    private static final String COMMON_EXTERNAL_STORAGE_APP_CLASS = "com.android.cts.externalstorageapp.CommonExternalStorageTest";
    private static final String EXTERNAL_STORAGE_APP_APK = "CtsExternalStorageApp.apk";
    private static final String EXTERNAL_STORAGE_APP_PKG = "com.android.cts.externalstorageapp";
    private static final String EXTERNAL_STORAGE_APP_CLASS = EXTERNAL_STORAGE_APP_PKG + ".ExternalStorageTest";
    private static final String READ_EXTERNAL_STORAGE_APP_APK = "CtsReadExternalStorageApp.apk";
    private static final String READ_EXTERNAL_STORAGE_APP_PKG = "com.android.cts.readexternalstorageapp";
    private static final String READ_EXTERNAL_STORAGE_APP_CLASS = READ_EXTERNAL_STORAGE_APP_PKG + ".ReadExternalStorageTest";
    private static final String WRITE_EXTERNAL_STORAGE_APP_APK = "CtsWriteExternalStorageApp.apk";
    private static final String WRITE_EXTERNAL_STORAGE_APP_PKG = "com.android.cts.writeexternalstorageapp";
    private static final String WRITE_EXTERNAL_STORAGE_APP_CLASS = WRITE_EXTERNAL_STORAGE_APP_PKG + ".WriteExternalStorageTest";

    // testInstrumentationDiffCert constants
    private static final String TARGET_INSTRUMENT_APK = "CtsTargetInstrumentationApp.apk";
    private static final String TARGET_INSTRUMENT_PKG = "com.android.cts.targetinstrumentationapp";
    private static final String INSTRUMENT_DIFF_CERT_APK = "CtsInstrumentationAppDiffCert.apk";
    private static final String INSTRUMENT_DIFF_CERT_PKG =
        "com.android.cts.instrumentationdiffcertapp";

    // testPermissionDiffCert constants
    private static final String DECLARE_PERMISSION_APK = "CtsPermissionDeclareApp.apk";
    private static final String DECLARE_PERMISSION_PKG = "com.android.cts.permissiondeclareapp";
    private static final String DECLARE_PERMISSION_COMPAT_APK = "CtsPermissionDeclareAppCompat.apk";
    private static final String DECLARE_PERMISSION_COMPAT_PKG = "com.android.cts.permissiondeclareappcompat";

    private static final String PERMISSION_DIFF_CERT_APK = "CtsUsePermissionDiffCert.apk";
    private static final String PERMISSION_DIFF_CERT_PKG =
        "com.android.cts.usespermissiondiffcertapp";

    private static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";

    private static final String MULTIUSER_STORAGE_APK = "CtsMultiUserStorageApp.apk";
    private static final String MULTIUSER_STORAGE_PKG = "com.android.cts.multiuserstorageapp";
    private static final String MULTIUSER_STORAGE_CLASS = MULTIUSER_STORAGE_PKG
            + ".MultiUserStorageTest";

    private static final String LOG_TAG = "AppSecurityTests";

    private CtsBuildHelper mCtsBuild;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    private File getTestAppFile(String fileName) throws FileNotFoundException {
        return mCtsBuild.getTestApp(fileName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // ensure build has been set before test is run
        assertNotNull(mCtsBuild);
    }

    /**
     * Test that an app that declares the same shared uid as an existing app, cannot be installed
     * if it is signed with a different certificate.
     */
    public void testSharedUidDifferentCerts() throws Exception {
        Log.i(LOG_TAG, "installing apks with shared uid, but different certs");
        try {
            // cleanup test apps that might be installed from previous partial test run
            getDevice().uninstallPackage(SHARED_UI_PKG);
            getDevice().uninstallPackage(SHARED_UI_DIFF_CERT_PKG);

            String installResult = getDevice().installPackage(getTestAppFile(SHARED_UI_APK),
                    false);
            assertNull(String.format("failed to install shared uid app, Reason: %s", installResult),
                    installResult);
            installResult = getDevice().installPackage(getTestAppFile(SHARED_UI_DIFF_CERT_APK),
                    false);
            assertNotNull("shared uid app with different cert than existing app installed " +
                    "successfully", installResult);
            assertEquals("INSTALL_FAILED_SHARED_USER_INCOMPATIBLE", installResult);
        }
        finally {
            getDevice().uninstallPackage(SHARED_UI_PKG);
            getDevice().uninstallPackage(SHARED_UI_DIFF_CERT_PKG);
        }
    }

    /**
     * Test that an app update cannot be installed over an existing app if it has a different
     * certificate.
     */
    public void testAppUpgradeDifferentCerts() throws Exception {
        Log.i(LOG_TAG, "installing app upgrade with different certs");
        try {
            // cleanup test app that might be installed from previous partial test run
            getDevice().uninstallPackage(SIMPLE_APP_PKG);

            String installResult = getDevice().installPackage(getTestAppFile(SIMPLE_APP_APK),
                    false);
            assertNull(String.format("failed to install simple app. Reason: %s", installResult),
                    installResult);
            installResult = getDevice().installPackage(getTestAppFile(SIMPLE_APP_DIFF_CERT_APK),
                    true /* reinstall */);
            assertNotNull("app upgrade with different cert than existing app installed " +
                    "successfully", installResult);
            assertEquals("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES", installResult);
        }
        finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    /**
     * Test that an app cannot access another app's private data.
     */
    public void testAppFailAccessPrivateData() throws Exception {
        Log.i(LOG_TAG, "installing app that attempts to access another app's private data");
        try {
            // cleanup test app that might be installed from previous partial test run
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);
            getDevice().uninstallPackage(APP_ACCESS_DATA_PKG);

            String installResult = getDevice().installPackage(getTestAppFile(APP_WITH_DATA_APK),
                    false);
            assertNull(String.format("failed to install app with data. Reason: %s", installResult),
                    installResult);
            // run appwithdata's tests to create private data
            assertTrue("failed to create app's private data", runDeviceTests(APP_WITH_DATA_PKG,
                    APP_WITH_DATA_CLASS, APP_WITH_DATA_CREATE_METHOD));

            installResult = getDevice().installPackage(getTestAppFile(APP_ACCESS_DATA_APK),
                    false);
            assertNull(String.format("failed to install app access data. Reason: %s",
                    installResult), installResult);
            // run appaccessdata's tests which attempt to access appwithdata's private data
            assertTrue("could access app's private data", runDeviceTests(APP_ACCESS_DATA_PKG));
        }
        finally {
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);
            getDevice().uninstallPackage(APP_ACCESS_DATA_PKG);
        }
    }

    /**
     * Verify that app with no external storage permissions works correctly.
     */
    public void testExternalStorageNone() throws Exception {
        try {
            wipePrimaryExternalStorage(getDevice());

            getDevice().uninstallPackage(EXTERNAL_STORAGE_APP_PKG);
            assertNull(getDevice()
                    .installPackage(getTestAppFile(EXTERNAL_STORAGE_APP_APK), false));
            assertTrue("Failed external storage with no permissions",
                    runDeviceTests(EXTERNAL_STORAGE_APP_PKG));
        } finally {
            getDevice().uninstallPackage(EXTERNAL_STORAGE_APP_PKG);
        }
    }

    /**
     * Verify that app with
     * {@link android.Manifest.permission#READ_EXTERNAL_STORAGE} works
     * correctly.
     */
    public void testExternalStorageRead() throws Exception {
        try {
            wipePrimaryExternalStorage(getDevice());

            getDevice().uninstallPackage(READ_EXTERNAL_STORAGE_APP_PKG);
            assertNull(getDevice()
                    .installPackage(getTestAppFile(READ_EXTERNAL_STORAGE_APP_APK), false));
            assertTrue("Failed external storage with read permissions",
                    runDeviceTests(READ_EXTERNAL_STORAGE_APP_PKG));
        } finally {
            getDevice().uninstallPackage(READ_EXTERNAL_STORAGE_APP_PKG);
        }
    }

    /**
     * Verify that app with
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} works
     * correctly.
     */
    public void testExternalStorageWrite() throws Exception {
        try {
            wipePrimaryExternalStorage(getDevice());

            getDevice().uninstallPackage(WRITE_EXTERNAL_STORAGE_APP_PKG);
            assertNull(getDevice()
                    .installPackage(getTestAppFile(WRITE_EXTERNAL_STORAGE_APP_APK), false));
            assertTrue("Failed external storage with write permissions",
                    runDeviceTests(WRITE_EXTERNAL_STORAGE_APP_PKG));
        } finally {
            getDevice().uninstallPackage(WRITE_EXTERNAL_STORAGE_APP_PKG);
        }
    }

    /**
     * Verify that app with WRITE_EXTERNAL can leave gifts in external storage
     * directories belonging to other apps, and those apps can read.
     */
    public void testExternalStorageGifts() throws Exception {
        try {
            wipePrimaryExternalStorage(getDevice());

            getDevice().uninstallPackage(EXTERNAL_STORAGE_APP_PKG);
            getDevice().uninstallPackage(READ_EXTERNAL_STORAGE_APP_PKG);
            getDevice().uninstallPackage(WRITE_EXTERNAL_STORAGE_APP_PKG);
            assertNull(getDevice()
                    .installPackage(getTestAppFile(EXTERNAL_STORAGE_APP_APK), false));
            assertNull(getDevice()
                    .installPackage(getTestAppFile(READ_EXTERNAL_STORAGE_APP_APK), false));
            assertNull(getDevice()
                    .installPackage(getTestAppFile(WRITE_EXTERNAL_STORAGE_APP_APK), false));

            assertTrue("Failed to write gifts", runDeviceTests(WRITE_EXTERNAL_STORAGE_APP_PKG,
                    WRITE_EXTERNAL_STORAGE_APP_CLASS, "doWriteGifts"));

            assertTrue("Read failed to verify gifts", runDeviceTests(READ_EXTERNAL_STORAGE_APP_PKG,
                    READ_EXTERNAL_STORAGE_APP_CLASS, "doVerifyGifts"));
            assertTrue("None failed to verify gifts", runDeviceTests(EXTERNAL_STORAGE_APP_PKG,
                    EXTERNAL_STORAGE_APP_CLASS, "doVerifyGifts"));

        } finally {
            getDevice().uninstallPackage(EXTERNAL_STORAGE_APP_PKG);
            getDevice().uninstallPackage(READ_EXTERNAL_STORAGE_APP_PKG);
            getDevice().uninstallPackage(WRITE_EXTERNAL_STORAGE_APP_PKG);
        }
    }

    /**
     * Test that uninstall of an app removes its private data.
     */
    public void testUninstallRemovesData() throws Exception {
        Log.i(LOG_TAG, "Uninstalling app, verifying data is removed.");
        try {
            // cleanup test app that might be installed from previous partial test run
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);

            String installResult = getDevice().installPackage(getTestAppFile(APP_WITH_DATA_APK),
                    false);
            assertNull(String.format("failed to install app with data. Reason: %s", installResult),
                    installResult);
            // run appwithdata's tests to create private data
            assertTrue("failed to create app's private data", runDeviceTests(APP_WITH_DATA_PKG,
                    APP_WITH_DATA_CLASS, APP_WITH_DATA_CREATE_METHOD));

            getDevice().uninstallPackage(APP_WITH_DATA_PKG);

            installResult = getDevice().installPackage(getTestAppFile(APP_WITH_DATA_APK),
                    false);
            assertNull(String.format("failed to install app with data second time. Reason: %s",
                    installResult), installResult);
            // run appwithdata's 'check if file exists' test
            assertTrue("app's private data still exists after install", runDeviceTests(
                    APP_WITH_DATA_PKG, APP_WITH_DATA_CLASS, APP_WITH_DATA_CHECK_NOEXIST_METHOD));

        }
        finally {
            getDevice().uninstallPackage(APP_WITH_DATA_PKG);
        }
    }

    /**
     * Test that an app cannot instrument another app that is signed with different certificate.
     */
    public void testInstrumentationDiffCert() throws Exception {
        Log.i(LOG_TAG, "installing app that attempts to instrument another app");
        try {
            // cleanup test app that might be installed from previous partial test run
            getDevice().uninstallPackage(TARGET_INSTRUMENT_PKG);
            getDevice().uninstallPackage(INSTRUMENT_DIFF_CERT_PKG);

            String installResult = getDevice().installPackage(
                    getTestAppFile(TARGET_INSTRUMENT_APK), false);
            assertNull(String.format("failed to install target instrumentation app. Reason: %s",
                    installResult), installResult);

            // the app will install, but will get error at runtime when starting instrumentation
            installResult = getDevice().installPackage(getTestAppFile(INSTRUMENT_DIFF_CERT_APK),
                    false);
            assertNull(String.format(
                    "failed to install instrumentation app with diff cert. Reason: %s",
                    installResult), installResult);
            // run INSTRUMENT_DIFF_CERT_PKG tests
            // this test will attempt to call startInstrumentation directly and verify
            // SecurityException is thrown
            assertTrue("running instrumentation with diff cert unexpectedly succeeded",
                    runDeviceTests(INSTRUMENT_DIFF_CERT_PKG));
        }
        finally {
            getDevice().uninstallPackage(TARGET_INSTRUMENT_PKG);
            getDevice().uninstallPackage(INSTRUMENT_DIFF_CERT_PKG);
        }
    }

    /**
     * Test that an app cannot use a signature-enforced permission if it is signed with a different
     * certificate than the app that declared the permission.
     */
    public void testPermissionDiffCert() throws Exception {
        Log.i(LOG_TAG, "installing app that attempts to use permission of another app");
        try {
            // cleanup test app that might be installed from previous partial test run
            getDevice().uninstallPackage(DECLARE_PERMISSION_PKG);
            getDevice().uninstallPackage(DECLARE_PERMISSION_COMPAT_PKG);
            getDevice().uninstallPackage(PERMISSION_DIFF_CERT_PKG);

            String installResult = getDevice().installPackage(
                    getTestAppFile(DECLARE_PERMISSION_APK), false);
            assertNull(String.format("failed to install declare permission app. Reason: %s",
                    installResult), installResult);

            installResult = getDevice().installPackage(
                    getTestAppFile(DECLARE_PERMISSION_COMPAT_APK), false);
            assertNull(String.format("failed to install declare permission compat app. Reason: %s",
                    installResult), installResult);

            // the app will install, but will get error at runtime
            installResult = getDevice().installPackage(getTestAppFile(PERMISSION_DIFF_CERT_APK),
                    false);
            assertNull(String.format("failed to install permission app with diff cert. Reason: %s",
                    installResult), installResult);
            // run PERMISSION_DIFF_CERT_PKG tests which try to access the permission
            TestRunResult result = doRunTests(PERMISSION_DIFF_CERT_PKG, null, null);
            assertDeviceTestsPass(result);
        }
        finally {
            getDevice().uninstallPackage(DECLARE_PERMISSION_PKG);
            getDevice().uninstallPackage(DECLARE_PERMISSION_COMPAT_PKG);
            getDevice().uninstallPackage(PERMISSION_DIFF_CERT_PKG);
        }
    }

    /**
     * Test multi-user emulated storage environment, ensuring that each user has
     * isolated storage.
     */
    public void testMultiUserStorage() throws Exception {
        final String PACKAGE = MULTIUSER_STORAGE_PKG;
        final String CLAZZ = MULTIUSER_STORAGE_CLASS;

        if (!isMultiUserSupportedOnDevice(getDevice())) {
            Log.d(LOG_TAG, "Single user device; skipping isolated storage tests");
            return;
        }

        int owner = 0;
        int secondary = -1;
        try {
            // Create secondary user
            secondary = createUserOnDevice(getDevice());

            // Install our test app
            getDevice().uninstallPackage(MULTIUSER_STORAGE_PKG);
            final String installResult = getDevice()
                    .installPackage(getTestAppFile(MULTIUSER_STORAGE_APK), false);
            assertNull("Failed to install: " + installResult, installResult);

            // Clear data from previous tests
            assertDeviceTestsPass(
                    doRunTestsAsUser(PACKAGE, CLAZZ, "cleanIsolatedStorage", owner));
            assertDeviceTestsPass(
                    doRunTestsAsUser(PACKAGE, CLAZZ, "cleanIsolatedStorage", secondary));

            // Have both users try writing into isolated storage
            assertDeviceTestsPass(
                    doRunTestsAsUser(PACKAGE, CLAZZ, "writeIsolatedStorage", owner));
            assertDeviceTestsPass(
                    doRunTestsAsUser(PACKAGE, CLAZZ, "writeIsolatedStorage", secondary));

            // Verify they both have isolated view of storage
            assertDeviceTestsPass(
                    doRunTestsAsUser(PACKAGE, CLAZZ, "readIsolatedStorage", owner));
            assertDeviceTestsPass(
                    doRunTestsAsUser(PACKAGE, CLAZZ, "readIsolatedStorage", secondary));
        } finally {
            getDevice().uninstallPackage(MULTIUSER_STORAGE_PKG);
            if (secondary != -1) {
                removeUserOnDevice(getDevice(), secondary);
            }
        }
    }

    /**
     * Helper method that checks that all tests in given result passed, and attempts to generate
     * a meaningful error message if they failed.
     *
     * @param result
     */
    private void assertDeviceTestsPass(TestRunResult result) {
        // TODO: consider rerunning if this occurred
        assertFalse(String.format("Failed to successfully run device tests for %s. Reason: %s",
                result.getName(), result.getRunFailureMessage()), result.isRunFailure());

        if (result.hasFailedTests()) {
            // build a meaningful error message
            StringBuilder errorBuilder = new StringBuilder("on-device tests failed:\n");
            for (Map.Entry<TestIdentifier, TestResult> resultEntry :
                result.getTestResults().entrySet()) {
                if (!resultEntry.getValue().getStatus().equals(TestStatus.PASSED)) {
                    errorBuilder.append(resultEntry.getKey().toString());
                    errorBuilder.append(":\n");
                    errorBuilder.append(resultEntry.getValue().getStackTrace());
                }
            }
            fail(errorBuilder.toString());
        }
    }

    /**
     * Helper method that will the specified packages tests on device.
     *
     * @param pkgName Android application package for tests
     * @return <code>true</code> if all tests passed.
     * @throws DeviceNotAvailableException if connection to device was lost.
     */
    private boolean runDeviceTests(String pkgName) throws DeviceNotAvailableException {
        return runDeviceTests(pkgName, null, null);
    }

    /**
     * Helper method that will the specified packages tests on device.
     *
     * @param pkgName Android application package for tests
     * @return <code>true</code> if all tests passed.
     * @throws DeviceNotAvailableException if connection to device was lost.
     */
    private boolean runDeviceTests(String pkgName, String testClassName, String testMethodName)
            throws DeviceNotAvailableException {
        TestRunResult runResult = doRunTests(pkgName, testClassName, testMethodName);
        return !runResult.hasFailedTests();
    }

    /**
     * Helper method to run tests and return the listener that collected the results.
     *
     * @param pkgName Android application package for tests
     * @return the {@link TestRunResult}
     * @throws DeviceNotAvailableException if connection to device was lost.
     */
    private TestRunResult doRunTests(String pkgName, String testClassName,
            String testMethodName) throws DeviceNotAvailableException {

        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(pkgName,
                getDevice().getIDevice());
        if (testClassName != null && testMethodName != null) {
            testRunner.setMethodName(testClassName, testMethodName);
        }
        CollectingTestListener listener = new CollectingTestListener();
        getDevice().runInstrumentationTests(testRunner, listener);
        return listener.getCurrentRunResults();
    }

    private static boolean isMultiUserSupportedOnDevice(ITestDevice device)
            throws DeviceNotAvailableException {
        // TODO: move this to ITestDevice once it supports users
        final String output = device.executeShellCommand("pm get-max-users");
        try {
            return Integer.parseInt(output.substring(output.lastIndexOf(" ")).trim()) > 1;
        } catch (NumberFormatException e) {
            fail("Failed to parse result: " + output);
        }
        return false;
    }

    private static int createUserOnDevice(ITestDevice device) throws DeviceNotAvailableException {
        // TODO: move this to ITestDevice once it supports users
        final String name = "CTS_" + System.currentTimeMillis();
        final String output = device.executeShellCommand("pm create-user " + name);
        if (output.startsWith("Success")) {
            try {
                return Integer.parseInt(output.substring(output.lastIndexOf(" ")).trim());
            } catch (NumberFormatException e) {
                fail("Failed to parse result: " + output);
            }
        } else {
            fail("Failed to create user: " + output);
        }
        throw new IllegalStateException();
    }

    private static void removeUserOnDevice(ITestDevice device, int userId)
            throws DeviceNotAvailableException {
        // TODO: move this to ITestDevice once it supports users
        final String output = device.executeShellCommand("pm remove-user " + userId);
        if (output.startsWith("Error")) {
            fail("Failed to remove user: " + output);
        }
    }

    private TestRunResult doRunTestsAsUser(
            String pkgName, String testClassName, String testMethodName, int userId)
            throws DeviceNotAvailableException {
        // TODO: move this to RemoteAndroidTestRunner once it supports users
        final String cmd = "am instrument --user " + userId + " -w -r -e class " + testClassName
                + "#" + testMethodName + " " + pkgName + "/android.test.InstrumentationTestRunner";
        Log.i(LOG_TAG, "Running " + cmd + " on " + getDevice().getSerialNumber());

        CollectingTestListener listener = new CollectingTestListener();
        InstrumentationResultParser parser = new InstrumentationResultParser(pkgName, listener);

        getDevice().executeShellCommand(cmd, parser);
        return listener.getCurrentRunResults();
    }

    private static void wipePrimaryExternalStorage(ITestDevice device)
            throws DeviceNotAvailableException {
        device.executeShellCommand("rm -rf /sdcard/*");
    }
}

# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cts_security_apps_list := \
	CtsAppAccessData \
	CtsAppWithData \
	CtsExternalStorageApp \
	CtsInstrumentationAppDiffCert \
	CtsPermissionDeclareApp \
	CtsPermissionDeclareAppCompat \
	CtsReadExternalStorageApp \
	CtsSharedUidInstall \
	CtsSharedUidInstallDiffCert \
	CtsSimpleAppInstall \
	CtsSimpleAppInstallDiffCert \
	CtsTargetInstrumentationApp \
	CtsUsePermissionDiffCert \
	CtsWriteExternalStorageApp \
	CtsMultiUserStorageApp

cts_support_packages := \
    CtsDeviceOpenGl \
    CtsDeviceTaskswitchingAppA \
    CtsDeviceTaskswitchingAppB \
    CtsDeviceTaskswitchingControl \
    CtsDeviceUi \
    CtsAccelerationTestStubs \
    CtsDeviceAdmin \
    CtsMonkeyApp \
    CtsMonkeyApp2 \
    CtsSomeAccessibilityServices \
    CtsTestStubs \
    SignatureTest \
    TestDeviceSetup \
    CtsUiAutomatorApp \
    CtsUsbSerialTestApp \
    $(cts_security_apps_list)

cts_external_packages := \
	com.replica.replicaisland

# Any APKs that need to be copied to the CTS distribution's testcases
# directory but do not require an associated test package XML.
CTS_TEST_CASE_LIST := \
	$(cts_support_packages) \
	$(cts_external_packages)

# Test packages that require an associated test package XML.
cts_test_packages := \
    CtsDeviceFilePerf \
    CtsDeviceUi \
    CtsDeviceDram \
    CtsDeviceSimpleCpu \
    CtsDeviceBrowserBench \
    CtsDeviceVideoPerf \
    CtsDeviceOpenGl \
    CtsAccelerationTestCases \
    CtsAccountManagerTestCases \
    CtsAccessibilityServiceTestCases \
    CtsAccessibilityTestCases \
    CtsAdminTestCases \
    CtsAnimationTestCases \
    CtsAppTestCases \
    CtsBluetoothTestCases \
    CtsCalendarcommon2TestCases \
    CtsContentTestCases \
    CtsDatabaseTestCases \
    CtsDisplayTestCases \
    CtsDpiTestCases \
    CtsDpiTestCases2 \
    CtsDreamsTestCases \
    CtsDrmTestCases \
    CtsEffectTestCases \
    CtsExampleTestCases \
    CtsGestureTestCases \
    CtsGraphicsTestCases \
    CtsGraphics2TestCases \
    CtsHardwareTestCases \
    CtsHoloTestCases \
    CtsJniTestCases \
    CtsKeystoreTestCases \
    CtsLocationTestCases \
    CtsMediaStressTestCases \
    CtsMediaTestCases \
    CtsNativeOpenGLTestCases \
    CtsNdefTestCases \
    CtsNetTestCases \
    CtsOpenGLTestCases \
    CtsOpenGlPerfTestCases \
    CtsOsTestCases \
    CtsPermissionTestCases \
    CtsPermission2TestCases \
    CtsPreferenceTestCases \
    CtsPreference2TestCases \
    CtsProviderTestCases \
    CtsRenderscriptTestCases \
    CtsRenderscriptGraphicsTestCases \
    CtsRsCppTestCases \
    CtsSaxTestCases \
    CtsSecurityTestCases \
    CtsSpeechTestCases \
    CtsTelephonyTestCases \
    CtsTextTestCases \
    CtsTextureViewTestCases \
    CtsThemeTestCases \
    CtsUtilTestCases \
    CtsViewTestCases \
    CtsWebkitTestCases \
    CtsWidgetTestCases

# All APKs that need to be scanned by the coverage utilities.
CTS_COVERAGE_TEST_CASE_LIST := \
	$(cts_support_packages) \
	$(cts_test_packages)


# Host side only tests
cts_host_libraries := \
    CtsHostUi \
    CtsHostJank \
    CtsAdbTests \
    CtsAppSecurityTests \
    CtsMonkeyTestCases \
    CtsUsbTests


# Native test executables that need to have associated test XMLs.
cts_native_exes := \
	NativeMediaTest_SL \
	NativeMediaTest_XA \
	bionic-unit-tests-cts \

cts_ui_tests := \
    CtsUiAutomatorTests

cts_device_jars := \
    CtsDeviceJank

# All the files that will end up under the repository/testcases
# directory of the final CTS distribution.
CTS_TEST_CASES := $(call cts-get-lib-paths,$(cts_host_libraries)) \
    $(call cts-get-package-paths,$(cts_test_packages)) \
    $(call cts-get-native-paths,$(cts_native_exes)) \
    $(call cts-get-ui-lib-paths,$(cts_ui_tests)) \
    $(call cts-get-ui-lib-paths,$(cts_device_jars))

# All the XMLs that will end up under the repository/testcases
# and that need to be created before making the final CTS distribution.
CTS_TEST_XMLS := $(call cts-get-test-xmls,$(cts_host_libraries)) \
    $(call cts-get-test-xmls,$(cts_test_packages)) \
    $(call cts-get-test-xmls,$(cts_native_exes)) \
    $(call cts-get-test-xmls,$(cts_ui_tests))


# The following files will be placed in the tools directory of the CTS distribution
CTS_TOOLS_LIST :=
# Copyright 2012 The Android Open Source Project
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

#
# Builds a package and defines a rule to generate the associated test
# package XML needed by CTS.
#
# Replace "include $(BUILD_PACKAGE)" with "include $(BUILD_CTS_GTEST_PACKAGE)"
#

# Disable by default so "m cts" will work in emulator builds
LOCAL_DEX_PREOPT := false
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

cts_package_apk := $(CTS_TESTCASES_OUT)/$(LOCAL_PACKAGE_NAME).apk
cts_package_xml := $(CTS_TESTCASES_OUT)/$(LOCAL_PACKAGE_NAME).xml

$(cts_package_apk): PRIVATE_PACKAGE := $(LOCAL_PACKAGE_NAME)
$(cts_package_apk): $(call intermediates-dir-for,APPS,$(LOCAL_PACKAGE_NAME))/package.apk | $(ACP)
	$(hide) mkdir -p $(CTS_TESTCASES_OUT)
	$(hide) $(ACP) -fp $(call intermediates-dir-for,APPS,$(PRIVATE_PACKAGE))/package.apk $@

$(cts_package_xml): PRIVATE_PATH := $(LOCAL_PATH)
$(cts_package_xml): PRIVATE_TEST_PACKAGE := android.$(notdir $(LOCAL_PATH))
$(cts_package_xml): PRIVATE_EXECUTABLE := $(LOCAL_MODULE)
$(cts_package_xml): PRIVATE_MANIFEST := $(LOCAL_PATH)/AndroidManifest.xml
$(cts_package_xml): $(addprefix $(LOCAL_PATH)/,$(LOCAL_SRC_FILES))  $(CTS_NATIVE_TEST_SCANNER) $(CTS_XML_GENERATOR)
	$(hide) echo Generating test description for wrapped native package $(PRIVATE_EXECUTABLE)
	$(hide) mkdir -p $(CTS_TESTCASES_OUT)
	$(hide) $(CTS_NATIVE_TEST_SCANNER) -s $(PRIVATE_PATH) \
						-t $(PRIVATE_TEST_PACKAGE) | \
			$(CTS_XML_GENERATOR) -t wrappednative \
                                                -m $(PRIVATE_MANIFEST) \
						-n $(PRIVATE_EXECUTABLE) \
						-p $(PRIVATE_TEST_PACKAGE) \
						-e $(CTS_EXPECTATIONS) \
						-o $@

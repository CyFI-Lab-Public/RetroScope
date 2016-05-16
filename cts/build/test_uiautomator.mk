# Copyright (C) 2012 The Android Open Source Project
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
# Builds a uiautomator library and defines a rule to generate the associated test
# package XML needed by CTS.
#

LOCAL_DEX_PREOPT := false

include $(BUILD_JAVA_LIBRARY)

cts_library_xml := $(CTS_TESTCASES_OUT)/$(LOCAL_MODULE).xml 
cts_library_jar := $(CTS_TESTCASES_OUT)/$(LOCAL_MODULE).jar

$(cts_library_jar): PRIVATE_MODULE := $(LOCAL_MODULE)
$(cts_library_jar): $(call intermediates-dir-for,JAVA_LIBRARIES,$(LOCAL_MODULE))/javalib.jar | $(ACP)
	$(hide) mkdir -p $(CTS_TESTCASES_OUT)
	$(hide) $(ACP) -fp $(call intermediates-dir-for,JAVA_LIBRARIES,$(PRIVATE_MODULE))/javalib.jar $@

$(cts_library_xml): PRIVATE_PATH := $(LOCAL_PATH)/src
$(cts_library_xml): PRIVATE_TEST_APP_PACKAGE := $(LOCAL_CTS_TEST_APP_PACKAGE)
$(cts_library_xml): PRIVATE_TEST_PACKAGE := $(LOCAL_CTS_TEST_PACKAGE)
$(cts_library_xml): PRIVATE_TEST_APK := $(LOCAL_CTS_TEST_APK)
$(cts_library_xml): PRIVATE_LIBRARY := $(LOCAL_MODULE)
$(cts_library_xml): PRIVATE_JAR_PATH := $(LOCAL_MODULE).jar
$(cts_library_xml): $(call intermediates-dir-for,JAVA_LIBRARIES,$(LOCAL_MODULE))/javalib.jar $(CTS_EXPECTATIONS) $(CTS_JAVA_TEST_SCANNER_DOCLET) $(CTS_JAVA_TEST_SCANNER) $(CTS_XML_GENERATOR)
	$(hide) echo Generating test description for uiautomator library $(PRIVATE_LIBRARY)
	$(hide) mkdir -p $(CTS_TESTCASES_OUT)
	$(hide) $(CTS_JAVA_TEST_SCANNER) -s $(PRIVATE_PATH) \
						-d $(CTS_JAVA_TEST_SCANNER_DOCLET) | \
			$(CTS_XML_GENERATOR) -t uiAutomator \
						-i $(PRIVATE_TEST_APK) \
						-j $(PRIVATE_JAR_PATH) \
						-a $(PRIVATE_TEST_PACKAGE) \
						-n $(PRIVATE_LIBRARY) \
						-p $(PRIVATE_TEST_PACKAGE) \
						-r $(PRIVATE_TEST_APP_PACKAGE) \
						-e $(CTS_EXPECTATIONS) \
						-o $@

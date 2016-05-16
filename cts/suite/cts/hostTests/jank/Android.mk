# Copyright (C) 2013 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE := CtsHostJank

LOCAL_JAVA_LIBRARIES := cts-tradefed tradefed-prebuilt ddmlib-prebuilt

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_CTS_TEST_PACKAGE := com.android.cts.jank

LOCAL_DEVICE_JAR_ := CtsDeviceJank
cts_library_jar_ := $(CTS_TESTCASES_OUT)/$(LOCAL_DEVICE_JAR_).jar

$(cts_library_jar_): $(call intermediates-dir-for,JAVA_LIBRARIES,$(LOCAL_DEVICE_JAR_))/javalib.jar | $(ACP)
	$(hide) mkdir -p $(CTS_TESTCASES_OUT)
	$(hide) $(ACP) -fp $(call intermediates-dir-for,JAVA_LIBRARIES,$(LOCAL_DEVICE_JAR_))/javalib.jar $@

$(CTS_TESTCASES_OUT)/CtsHostJank.xml: $(cts_library_jar_)

include $(BUILD_CTS_HOST_JAVA_LIBRARY)

# Build the library using its own makefile
include $(call all-makefiles-under,$(LOCAL_PATH))

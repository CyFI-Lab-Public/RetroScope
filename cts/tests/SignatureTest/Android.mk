# Copyright (C) 2008 The Android Open Source Project
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

# don't include this package in any target
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_PACKAGE_NAME := SignatureTest

LOCAL_SDK_VERSION := current

# To be passed in on command line
CTS_API_VERSION ?= current
ifeq (current,$(CTS_API_VERSION))
android_api_description := frameworks/base/api/$(CTS_API_VERSION).txt
else
android_api_description := $(SRC_API_DIR)/$(CTS_API_VERSION).txt
endif

# Can't call local-intermediates-dir directly here because we have to
# include BUILD_CTS_PACKAGE first.  Can't include BUILD_CTS_PACKAGE first
# because we have to override LOCAL_RESOURCE_DIR first.  Hence this
# hack.
intermediates.COMMON := $(call intermediates-dir-for,APPS,$(LOCAL_PACKAGE_NAME),,COMMON)
signature_res_dir := $(intermediates.COMMON)/genres
LOCAL_RESOURCE_DIR := $(signature_res_dir) $(LOCAL_PATH)/res

include $(BUILD_CTS_PACKAGE)

$(info $(call local-intermediates-dir))

generated_res_stamp := $(intermediates.COMMON)/genres.stamp
api_ver_file := $(intermediates.COMMON)/api_ver_is_$(CTS_API_VERSION)

# The api_ver_file keeps track of which api version was last built.
# By only ever having one of these magic files in existence and making
# sure the generated resources rule depend on it, we can ensure that
# the proper version of the api resource gets generated.
$(api_ver_file):
	$(hide) rm -f $(dir $@)/api_ver_is_* \
		&& mkdir -p $(dir $@) && touch $@

android_api_xml_description := $(intermediates.COMMON)/api.xml
$(android_api_xml_description): $(android_api_description) | $(APICHECK)
	@ echo "Convert api file to xml: $@"
	@ mkdir -p $(dir $@)
	$(hide) $(APICHECK_COMMAND) -convert2xml $< $@

# Split up config/api/1.xml by "package" and save the files as the
# resource files of SignatureTest.
$(generated_res_stamp): PRIVATE_PATH := $(LOCAL_PATH)
$(generated_res_stamp): PRIVATE_MODULE := $(LOCAL_MODULE)
$(generated_res_stamp): PRIVATE_RES_DIR := $(signature_res_dir)
$(generated_res_stamp): PRIVATE_API_XML_DESC := $(android_api_xml_description)
$(generated_res_stamp): $(api_ver_file)
$(generated_res_stamp): $(android_api_xml_description)
	@ echo "Copy generated resources: $(PRIVATE_MODULE)"
	$(hide) python cts/tools/utils/android_api_description_splitter.py \
		$(PRIVATE_API_XML_DESC) $(PRIVATE_RES_DIR) package
	$(hide) touch $@

$(R_file_stamp): $(generated_res_stamp)

# clean up temp vars
android_api_xml_description :=
api_ver_file :=
generated_res_stamp :=
signature_res_dir :=
android_api_description :=
CTS_API_VERSION :=

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

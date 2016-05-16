#
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

LOCAL_PATH:= $(call my-dir)

uiautomator_internal_api_file := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/ub_uiautomator_api.txt

###############################################
# Build core library
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_MODULE := ub-uiautomator
LOCAL_SDK_VERSION := current

gen := $(call intermediates-dir-for,JAVA_LIBRARIES,$(LOCAL_MODULE),,COMMON)/BuildConstants.java
$(gen) : $(LOCAL_PATH)/BuildConstants.java.in
	@echo Generating: $@
	@ mkdir -p $(dir $@)
	$(hide) sed -e 's/%BUILD_NUMBER%/$(BUILD_NUMBER)/' \
		$< > $@

LOCAL_GENERATED_SOURCES += $(gen)


include $(BUILD_STATIC_JAVA_LIBRARY)
# establish dependency on apicheck
uiautomator_library := $(LOCAL_BUILT_MODULE)
###############################################


###############################################
# Generate the stub source files
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SDK_VERSION := current
LOCAL_MODULE_CLASS := DOCS
LOCAL_DROIDDOC_HTML_DIR :=
LOCAL_DROIDDOC_OPTIONS:= \
    -stubpackages com.android.uiautomator.core:com.android.uiautomator.testrunner \
    -api $(uiautomator_internal_api_file)
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk

LOCAL_MODULE := ub-uiautomator-docs

include $(BUILD_DROIDDOC)

uiautomator_stubs_stamp := $(full_target)
$(uiautomator_internal_api_file) : $(full_target)

###############################################
# API check
# Please refer to build/core/tasks/apicheck.mk.
uiautomator_api_dir := frameworks/uiautomator/api
last_released_sdk_version := $(lastword $(call numerically_sort, \
    $(filter-out current, \
        $(patsubst $(uiautomator_api_dir)/%.txt,%, $(wildcard $(uiautomator_api_dir)/*.txt)) \
    )))

checkapi_last_error_level_flags := \
    -hide 2 -hide 3 -hide 4 -hide 5 -hide 6 -hide 24 -hide 25 \
    -error 7 -error 8 -error 9 -error 10 -error 11 -error 12 -error 13 -error 14 -error 15 \
    -error 16 -error 17 -error 18

# Check that the API we're building hasn't broken the last-released SDK version.
$(eval $(call check-api, \
    ub-uiautomator-checkapi-last, \
    $(uiautomator_api_dir)/$(last_released_sdk_version).txt, \
    $(uiautomator_internal_api_file), \
    $(checkapi_last_error_level_flags), \
    cat $(LOCAL_PATH)/apicheck_msg_last.txt, \
    $(uiautomator_library), \
    $(uiautomator_stubs_stamp)))

checkapi_current_error_level_flags := \
    -error 2 -error 3 -error 4 -error 5 -error 6 \
    -error 7 -error 8 -error 9 -error 10 -error 11 -error 12 -error 13 -error 14 -error 15 \
    -error 16 -error 17 -error 18 -error 19 -error 20 -error 21 -error 23 -error 24 \
    -error 25

# Check that the API we're building hasn't changed from the not-yet-released
# SDK version.
$(eval $(call check-api, \
    ub-uiautomator-checkapi-current, \
    $(uiautomator_api_dir)/current.txt, \
    $(uiautomator_internal_api_file), \
    $(checkapi_current_error_level_flags), \
    cat $(LOCAL_PATH)/apicheck_msg_current.txt, \
    $(uiautomator_library), \
    $(uiautomator_stubs_stamp)))

.PHONY: update-ub-uiautomator-api
update-ub-uiautomator-api: PRIVATE_API_DIR := $(uiautomator_api_dir)
update-ub-uiautomator-api: $(uiautomator_internal_api_file) | $(ACP)
	@echo Copying uiautomator current.txt
	$(hide) $(ACP) $< $(PRIVATE_API_DIR)/current.txt

###############################################
# clean up temp vars
uiautomator_stubs_stamp :=
uiautomator_internal_api_file :=
uiautomator_library :=
uiautomator_api_dir :=
checkapi_last_error_level_flags :=
checkapi_current_error_level_flags :=

###############################################
# Build tests
include $(LOCAL_PATH)/tests/Android.mk

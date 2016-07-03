#
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
#

LOCAL_PATH:= $(call my-dir)

ifneq (,$(TARGET_BUILD_APPS)$(filter true,$(TARGET_BUILD_PDK)))

##################################
include $(CLEAR_VARS)

# We can't simple call $(BUILD_PREBUILT) here, because $(ACP) is not
# available yet..

LOCAL_MODULE := acp
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional

$(ACP): $(LOCAL_PATH)/$(HOST_OS)/acp$(HOST_EXECUTABLE_SUFFIX)
	@echo "Copy: acp ($@)"
	$(copy-file-to-target-with-cp)
	$(hide) chmod 755 $@

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := aidl
LOCAL_SRC_FILES := $(HOST_OS)/aidl$(HOST_EXECUTABLE_SUFFIX)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(HOST_EXECUTABLE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := aidl$(HOST_EXECUTABLE_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := aapt
LOCAL_SRC_FILES := $(HOST_OS)/aapt$(HOST_EXECUTABLE_SUFFIX)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(HOST_EXECUTABLE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := aapt$(HOST_EXECUTABLE_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################

include $(CLEAR_VARS)

LOCAL_MODULE := zipalign
LOCAL_SRC_FILES := $(HOST_OS)/zipalign$(HOST_EXECUTABLE_SUFFIX)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(HOST_EXECUTABLE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := zipalign$(HOST_EXECUTABLE_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := signapk
LOCAL_SRC_FILES := lib/signapk.jar
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := signapk$(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := dx
LOCAL_SRC_FILES := lib/dx.jar
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := dx$(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

# We have to call copy-file-to-new-target instead of simply including
# $(BUILD_PREBUILT) here, because we must put dx.jar as dependecy of dx.

LOCAL_MODULE := dx
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): $(HOST_OUT_JAVA_LIBRARIES)/dx$(COMMON_JAVA_PACKAGE_SUFFIX)
$(LOCAL_BUILT_MODULE): $(LOCAL_PATH)/dx | $(ACP)
	@echo "Copy: $(PRIVATE_MODULE) ($@)"
	$(copy-file-to-new-target)
	$(hide) chmod 755 $@

##################################

endif # TARGET_BUILD_APPS or TARGET_BUILD_PDK

# Only use these prebuilts in unbundled branches
# Don't use prebuilts in PDK

ifneq (,$(TARGET_BUILD_APPS))

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := llvm-rs-cc
LOCAL_SRC_FILES := $(HOST_OS)/llvm-rs-cc$(HOST_EXECUTABLE_SUFFIX)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := libLLVM libclang
LOCAL_MODULE_SUFFIX := $(HOST_EXECUTABLE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := llvm-rs-cc$(HOST_EXECUTABLE_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := bcc_compat
LOCAL_SRC_FILES := $(HOST_OS)/$(LOCAL_MODULE)$(HOST_EXECUTABLE_SUFFIX)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := libbcc libbcinfo
LOCAL_MODULE_SUFFIX := $(HOST_EXECUTABLE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := $(LOCAL_MODULE)$(HOST_EXECUTABLE_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := libbcc
LOCAL_SRC_FILES := $(HOST_OS)/$(LOCAL_MODULE)$(HOST_SHLIB_SUFFIX)
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(HOST_SHLIB_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := libbcinfo
LOCAL_SRC_FILES := $(HOST_OS)/$(LOCAL_MODULE)$(HOST_SHLIB_SUFFIX)
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(HOST_SHLIB_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################

include $(CLEAR_VARS)

LOCAL_MODULE := libLLVM
LOCAL_SRC_FILES := $(HOST_OS)/$(LOCAL_MODULE)$(HOST_SHLIB_SUFFIX)
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(HOST_SHLIB_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := libclang
LOCAL_SRC_FILES := $(HOST_OS)/$(LOCAL_MODULE)$(HOST_SHLIB_SUFFIX)
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(HOST_SHLIB_SUFFIX)
LOCAL_IS_HOST_MODULE := true

include $(BUILD_PREBUILT)

##################################

endif # TARGET_BUILD_APPS only

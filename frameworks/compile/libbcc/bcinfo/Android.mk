#
# Copyright (C) 2011-2012 The Android Open Source Project
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

# Don't build for unbundled branches
ifeq (,$(TARGET_BUILD_APPS))

local_cflags_for_libbcinfo := -Wall -Wno-unused-parameter -Werror
ifneq ($(TARGET_BUILD_VARIANT),eng)
local_cflags_for_libbcinfo += -D__DISABLE_ASSERTS
endif

ifeq "REL" "$(PLATFORM_VERSION_CODENAME)"
  BCINFO_API_VERSION := $(PLATFORM_SDK_VERSION)
else
  # Increment by 1 whenever this is not a final release build, since we want to
  # be able to see the RS version number change during development.
  # See build/core/version_defaults.mk for more information about this.
  BCINFO_API_VERSION := "(1 + $(PLATFORM_SDK_VERSION))"
endif
local_cflags_for_libbcinfo += -DBCINFO_API_VERSION=$(BCINFO_API_VERSION)

LOCAL_PATH := $(call my-dir)

libbcinfo_SRC_FILES := \
  BitcodeTranslator.cpp \
  BitcodeWrapper.cpp \
  MetadataExtractor.cpp

libbcinfo_C_INCLUDES := \
  $(LOCAL_PATH)/../include \
  $(LOCAL_PATH)/../../slang

libbcinfo_STATIC_LIBRARIES := \
  libLLVMWrap \
  libLLVMBitReader_2_7 \
  libLLVMBitReader_3_0 \
  libLLVMBitWriter_3_2

LLVM_ROOT_PATH := external/llvm

include $(CLEAR_VARS)

LOCAL_MODULE := libbcinfo
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
intermediates := $(local-intermediates-dir)

LOCAL_SRC_FILES := $(libbcinfo_SRC_FILES)

LOCAL_CFLAGS += $(local_cflags_for_libbcinfo)

LOCAL_C_INCLUDES := $(libbcinfo_C_INCLUDES)

LOCAL_STATIC_LIBRARIES := $(libbcinfo_STATIC_LIBRARIES)
LOCAL_SHARED_LIBRARIES := libLLVM libcutils liblog libstlport

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := libbcinfo
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_IS_HOST_MODULE := true

LOCAL_SRC_FILES := $(libbcinfo_SRC_FILES)

LOCAL_CFLAGS += $(local_cflags_for_libbcinfo)

LOCAL_C_INCLUDES := $(libbcinfo_C_INCLUDES)

LOCAL_STATIC_LIBRARIES += $(libbcinfo_STATIC_LIBRARIES)
LOCAL_STATIC_LIBRARIES += libcutils liblog
LOCAL_SHARED_LIBRARIES += libLLVM

ifndef USE_MINGW
LOCAL_LDLIBS := -ldl -lpthread
endif

include $(LLVM_ROOT_PATH)/llvm-host-build.mk
include $(BUILD_HOST_SHARED_LIBRARY)

endif # don't build for unbundled branches

#=====================================================================
# Include Subdirectories
#=====================================================================
include $(call all-makefiles-under,$(LOCAL_PATH))

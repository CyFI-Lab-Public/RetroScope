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

LOCAL_PATH := $(call my-dir)

# Don't build for unbundled branches
ifeq (,$(TARGET_BUILD_APPS))

# Executable for host
# ========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := bcc_compat
LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_SHARED_LIBRARIES := \
  libbcc \
  libbcinfo \
  libLLVM

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/../../include

ifndef USE_MINGW
LOCAL_LDLIBS = -ldl
endif

LOCAL_SRC_FILES := Main.cpp

include $(LIBBCC_HOST_BUILD_MK)
include $(LIBBCC_GEN_CONFIG_MK)
include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_EXECUTABLE)

endif  # Don't build for PDK or unbundled branches

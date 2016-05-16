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

LLVM_ROOT_PATH := $(LOCAL_PATH)/../../../../../external/llvm
include $(LLVM_ROOT_PATH)/llvm.mk

llvm_wrap_SRC_FILES := \
  bitcode_wrapperer.cpp \
  file_wrapper_input.cpp \
  file_wrapper_output.cpp \
  in_memory_wrapper_input.cpp \
  wrapper_output.cpp

llvm_wrap_C_INCLUDES := $(LOCAL_PATH)/../../include

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMWrap
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(llvm_wrap_SRC_FILES)
LOCAL_CFLAGS += -D__HOST__
LOCAL_C_INCLUDES := $(llvm_wrap_C_INCLUDES)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMWrap
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(llvm_wrap_SRC_FILES)
LOCAL_C_INCLUDES := $(llvm_wrap_C_INCLUDES)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

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
include $(LOCAL_PATH)/../common.mk

# Build opcontrol executable on target
include $(CLEAR_VARS)

ifeq ($(ARCH_ARM_HAVE_ARMV7A), true)
    LOCAL_CFLAGS += -DWITH_ARM_V7_A
endif

LOCAL_SRC_FILES:= \
	opcontrol.cpp

LOCAL_STATIC_LIBRARIES := \
	liboprofile_popt \
	liboprofile_util \
	liboprofile_db \
	liboprofile_abi \
	liboprofile_op

LOCAL_C_INCLUDES := $(common_target_c_includes)
LOCAL_CFLAGS := $(common_target_cflags)

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE:= opcontrol

include $(BUILD_EXECUTABLE)

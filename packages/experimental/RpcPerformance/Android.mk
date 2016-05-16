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

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := tests   # Allows non-localized strings
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += src/com/android/rpc_performance/IService.aidl
LOCAL_PACKAGE_NAME := RpcPerformance
include $(BUILD_PACKAGE)

#include $(CLEAR_VARS)
#LOCAL_MODULE := rpcperftest
#LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
#LOCAL_SHARED_LIBRARIES := libbinder
#LOCAL_SRC_FILES := rpcperftest.cpp
#include $(BUILD_EXECUTABLE)

#include $(CLEAR_VARS)
#LOCAL_FORCE_STATIC_EXECUTABLE := true
#LOCAL_MODULE := rpcperftest_static
#LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
#LOCAL_STATIC_LIBRARIES := libbinder libutils libcutils libstdc++ libc
#LOCAL_SRC_FILES := rpcperftest.cpp
#include $(BUILD_EXECUTABLE)

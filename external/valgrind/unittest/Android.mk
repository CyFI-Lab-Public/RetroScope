# Copyright (C) 2011 The Android Open Source Project
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

common_cflags := -DDYNAMIC_ANNOTATIONS_ENABLED=1 -O0 -g -UNDEBUG

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= \
	racecheck_unittest.cc \
	old_test_suite.cc \
	test_utils.cc
LOCAL_MODULE:= racecheck_unittest
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS := $(common_cflags)

LOCAL_C_INCLUDES := \
        bionic \
        external/stlport/stlport \
	external/valgrind/main/include \
	external/valgrind/dynamic_annotations \
	external/gtest/include

LOCAL_CPP_EXTENSION := .cc

LOCAL_STATIC_LIBRARIES := libgtest
LOCAL_SHARED_LIBRARIES := libc libstlport libdynamic_annotations

include $(BUILD_EXECUTABLE)

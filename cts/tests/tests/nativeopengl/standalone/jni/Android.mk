# Copyright 2013 The Android Open Source Project
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

MY_LOCAL_PATH := $(call my-dir)
MY_GTEST_PATH := $(MY_LOCAL_PATH)/../../../../../../external/gtest

# gtest

LOCAL_PATH := $(MY_GTEST_PATH)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc
LOCAL_MODULE := libgtest
LOCAL_C_INCLUDES := $(MY_GTEST_PATH)/include
LOCAL_SRC_FILES := src/gtest-all.cc

include $(BUILD_STATIC_LIBRARY)

# nativetests

LOCAL_PATH := $(MY_LOCAL_PATH)

include $(CLEAR_VARS)

LIB_PATH := $(LOCAL_PATH)/../libs/$(TARGET_ARCH_ABI)/
LOCAL_C_INCLUDES := $(MY_GTEST_PATH)/include
LOCAL_LDLIBS    := -L$(LIB_PATH) -landroid -lEGL -lGLESv2 -llog
LOCAL_STATIC_LIBRARIES := libgtest
LOCAL_MODULE    := nativeopengltests
LOCAL_SRC_FILES := GLTestHelper.cpp \
                   register.cpp \
                   tests/GLTest_test.cpp \
                   tests/EGLCleanup_test.cpp \
                   tests/EGLCreateContext_test.cpp

LOCAL_SHARED_LIBRARIES := libgtest

include $(BUILD_SHARED_LIBRARY)

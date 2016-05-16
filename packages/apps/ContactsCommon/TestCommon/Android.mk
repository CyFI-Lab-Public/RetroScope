# Copyright 2012, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# This has to be LOCAL_JAVA instead of LOCAL_STATIC since this test util is installed in the same
# vm as the packages to be tested. Otherwise you will get error
# "Class ref in pre-verified class resolved to unexpected implementation"
# when running the unit tests.
LOCAL_JAVA_LIBRARIES := guava android.test.runner

LOCAL_INSTRUMENTATION_FOR := com.android.contacts.common

LOCAL_MODULE := com.android.contacts.common.test
include $(BUILD_STATIC_JAVA_LIBRARY)

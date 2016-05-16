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

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := antlr-runtime
#LOCAL_SDK_VERSION := 8
LOCAL_SRC_FILES := $(call all-java-files-under, antlr-3.4/runtime/Java/src/main/java)
#Remove DOTTreeGenerator.java, so that we don't have the StringTemplate library as a dependency
LOCAL_SRC_FILES := $(filter-out antlr-3.4/runtime/Java/src/main/java/org/antlr/runtime/tree/DOTTreeGenerator.java, $(LOCAL_SRC_FILES))
LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_JAVA_LIBRARY)

# Also build a host-side library
# include $(CLEAR_VARS)
# 
# LOCAL_SRC_FILES := $(call all-java-files-under, src)
# LOCAL_MODULE := antlrlib
# 
# include $(BUILD_HOST_JAVA_LIBRARY)


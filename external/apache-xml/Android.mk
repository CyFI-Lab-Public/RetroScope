#
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
#
LOCAL_PATH := $(call my-dir)

apache-xml_src_files := $(call all-java-files-under,src/main/java)

include $(CLEAR_VARS)
LOCAL_MODULE := apache-xml
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(apache-xml_src_files)
LOCAL_JAVACFLAGS := -encoding UTF-8
LOCAL_JAVA_LIBRARIES := core
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_RESOURCE_DIRS := src/main/java
include $(BUILD_JAVA_LIBRARY)

ifeq ($(WITH_HOST_DALVIK),true)
    include $(CLEAR_VARS)
    LOCAL_MODULE := apache-xml-hostdex
    LOCAL_MODULE_TAGS := optional
    LOCAL_SRC_FILES := $(apache-xml_src_files)
    LOCAL_JAVACFLAGS := -encoding UTF-8
    LOCAL_BUILD_HOST_DEX := true
    LOCAL_MODULE_TAGS := optional
    LOCAL_JAVA_RESOURCE_DIRS := src/main/java
    include $(BUILD_HOST_JAVA_LIBRARY)
endif

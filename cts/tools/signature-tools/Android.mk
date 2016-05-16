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

LOCAL_PATH := $(call my-dir)

# signature-tools java library
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under,src)
LOCAL_JAVA_RESOURCE_DIRS := templates

LOCAL_MODULE := signature-tools
LOCAL_JAVA_LIBRARIES := dx dex-tools
LOCAL_STATIC_JAVA_LIBRARIES := stringtemplate antlr-2.7.7
LOCAL_CLASSPATH := $(HOST_JDK_TOOLS_JAR)

include $(BUILD_HOST_JAVA_LIBRARY)

# prebuilt stringtemplate.jar
# ============================================================
include $(CLEAR_VARS)

LOCAL_PREBUILT_JAVA_LIBRARIES := stringtemplate:lib/stringtemplate.jar

include $(BUILD_HOST_PREBUILT)

# prebuilt antlr-2.7.7.jar
# ============================================================
include $(CLEAR_VARS)

LOCAL_PREBUILT_JAVA_LIBRARIES := antlr-2.7.7:lib/antlr-2.7.7.jar

include $(BUILD_HOST_PREBUILT)

# signature-tool script
# ============================================================
include $(CLEAR_VARS)

LOCAL_PREBUILT_EXECUTABLES := sig
include $(BUILD_HOST_PREBUILT)

# signature-create script
# ============================================================
include $(CLEAR_VARS)

LOCAL_PREBUILT_EXECUTABLES := sig-create
include $(BUILD_HOST_PREBUILT)

# signature-check script
# ============================================================
include $(CLEAR_VARS)

LOCAL_PREBUILT_EXECUTABLES := sig-check
include $(BUILD_HOST_PREBUILT)

#
# Copyright (C) 2009 The Android Open Source Project
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

include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := android.test.runner
# We use the DefaultManifestAttributesTest from the android.cts.dpi package.
LOCAL_STATIC_JAVA_LIBRARIES := android.cts.dpi ctstestrunner

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := CtsDpiTestCases2

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

# We would set LOCAL_SDK_VERSION := 3 here, but the build system
# doesn't currently support setting LOCAL_SDK_VERSION to anything but
# current.

include $(BUILD_CTS_PACKAGE)

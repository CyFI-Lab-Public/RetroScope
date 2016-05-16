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
local_target_dir := $(TARGET_OUT_DATA)/local/tmp
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE := com.android.uiautomator.platform.common

LOCAL_SDK_VERSION := 16

LOCAL_JAVA_LIBRARIES := uiautomator_sdk_v$(LOCAL_SDK_VERSION)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_PATH := $(local_target_dir)

include $(BUILD_STATIC_JAVA_LIBRARY)

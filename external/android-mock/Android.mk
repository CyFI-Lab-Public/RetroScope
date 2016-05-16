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
#
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := tests
LOCAL_SDK_VERSION := 8
LOCAL_STATIC_JAVA_LIBRARIES := easymocklib
LOCAL_MODULE := android-mock-runtimelib
LOCAL_MOCKING_PATH := src/com/google/android/testing/mocking/
LOCAL_SRC_FILES := \
      $(LOCAL_MOCKING_PATH)/AndroidMock.java \
      $(LOCAL_MOCKING_PATH)/SdkVersion.java \
      $(LOCAL_MOCKING_PATH)/FileUtils.java \
      $(LOCAL_MOCKING_PATH)/GeneratedClassFile.java \
      $(LOCAL_MOCKING_PATH)/MockObject.java \
      $(LOCAL_MOCKING_PATH)/UsesMocks.java

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := android-mock-generatorlib
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := easymock javassist

include $(BUILD_HOST_JAVA_LIBRARY)

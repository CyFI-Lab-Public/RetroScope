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

# build only for linux
ifeq ($(HOST_OS),linux)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_CPP_EXTENSION := .cpp
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(patsubst ./%,%, $(shell cd $(LOCAL_PATH); \
  find . -name "*.cpp" -and -not -name ".*"))
#$(info $(LOCAL_SRC_FILES))
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include $(LOCAL_PATH)/src /usr/include/ \
	external/tinyalsa/include/ external/tinyxml/ libcore/include
LOCAL_STATIC_LIBRARIES += libutils liblog libtinyalsa libcutils libtinyxml
LOCAL_CFLAGS:= -g -fno-exceptions
LOCAL_LDFLAGS:= -g -lrt -ldl -lm -fno-exceptions
LOCAL_MODULE:= libcts_audio_quality
include $(BUILD_HOST_STATIC_LIBRARY)

endif # linux

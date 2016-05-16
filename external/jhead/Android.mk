#
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
#
LOCAL_PATH := $(my-dir)

#########################################
# non-jni part

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	exif.c \
	gpsinfo.c \
	iptc.c \
	jhead.c \
	jpgfile.c \
	makernote.c

LOCAL_MODULE := libexif

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	liblog

include $(BUILD_SHARED_LIBRARY)

#########################################
# jni part

# allow jni build if java is supported, necessary for PDK
ifneq ($(TARGET_BUILD_JAVA_SUPPORT_LEVEL),)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
	main.c

LOCAL_MODULE := libexif_jni

LOCAL_SHARED_LIBRARIES := \
	libnativehelper \
	libcutils \
	libutils \
	liblog \
	libexif

include $(BUILD_SHARED_LIBRARY)

endif # JAVA_SUPPORT

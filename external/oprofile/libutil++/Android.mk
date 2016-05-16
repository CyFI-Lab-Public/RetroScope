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

LOCAL_PATH:= $(call my-dir)
include $(LOCAL_PATH)/../common.mk

# Build libutil++ on host
ifeq ($(HAVE_LIBBFD),true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	bfd_support.cpp \
	bfd_spu_support.cpp \
	child_reader.cpp \
	cverb.cpp \
	file_manip.cpp \
	glob_filter.cpp \
	op_bfd.cpp \
	op_exception.cpp \
	op_spu_bfd.cpp \
	path_filter.cpp \
	stream_util.cpp \
	string_filter.cpp \
	string_manip.cpp \
	xml_output.cpp

LOCAL_C_INCLUDES := $(common_host_c_includes)
LOCAL_CFLAGS := $(common_host_cflags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := liboprofile_util++

include $(BUILD_HOST_STATIC_LIBRARY)
endif

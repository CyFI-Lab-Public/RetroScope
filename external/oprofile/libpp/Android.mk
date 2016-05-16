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

# Build libpp on host
ifeq ($(HAVE_LIBBFD),true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	arrange_profiles.cpp \
	callgraph_container.cpp \
	diff_container.cpp \
	filename_spec.cpp \
	format_output.cpp \
	image_errors.cpp \
	locate_images.cpp \
	name_storage.cpp \
	op_header.cpp \
	symbol.cpp \
	parse_filename.cpp \
	populate.cpp \
	profile.cpp \
	profile_container.cpp \
	profile_spec.cpp \
	sample_container.cpp \
	symbol_container.cpp \
	symbol_functors.cpp \
	symbol_sort.cpp \
	xml_utils.cpp \
	populate_for_spu.cpp

LOCAL_C_INCLUDES := $(common_host_c_includes)
LOCAL_CFLAGS := $(common_host_cflags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := liboprofile_pp

include $(BUILD_HOST_STATIC_LIBRARY)
endif

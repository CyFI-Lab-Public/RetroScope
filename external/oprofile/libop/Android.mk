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

LOCAL_PATH:= $(call my-dir)
include $(LOCAL_PATH)/../common.mk

common_src := \
	op_alloc_counter.c \
	op_config.c \
	op_cpu_type.c \
	op_events.c \
	op_get_interface.c \
	op_mangle.c \
	op_parse_event.c \
	op_xml_events.c \
	op_xml_out.c

# Build libop on target
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= $(common_src)
LOCAL_C_INCLUDES := $(common_target_c_includes)
LOCAL_CFLAGS := $(common_target_cflags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := liboprofile_op

include $(BUILD_STATIC_LIBRARY)

# Build libop on host
ifeq ($(HAVE_LIBBFD),true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= $(common_src)
LOCAL_C_INCLUDES := $(common_host_c_includes)
LOCAL_CFLAGS := $(common_host_cflags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := liboprofile_op

include $(BUILD_HOST_STATIC_LIBRARY)
endif

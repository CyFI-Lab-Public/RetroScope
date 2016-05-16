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

common_src := common_option.cpp

common_libs := \
	liboprofile_pp \
	liboprofile_db \
	liboprofile_op_regex \
	liboprofile_opt++ \
	liboprofile_util++ \
	liboprofile_popt \
	liboprofile_op \
	liboprofile_util \
	libbfd \
	libiberty \
	libintl

common_ldlibs := -lz $(common_host_ldlibs_libiconv) -ldl

ifeq ($(HAVE_LIBBFD),true)

# Build opreport on host
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	$(common_src) \
	opreport.cpp \
	opreport_options.cpp

LOCAL_STATIC_LIBRARIES := $(common_libs)
LOCAL_C_INCLUDES := $(common_host_c_includes)
LOCAL_CFLAGS := $(common_host_cflags)
LOCAL_LDLIBS := $(common_ldlibs)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= opreport

include $(BUILD_HOST_EXECUTABLE)

# Build opannotate on host
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	$(common_src) \
	opannotate.cpp \
	opannotate_options.cpp

LOCAL_STATIC_LIBRARIES := $(common_libs)
LOCAL_C_INCLUDES := $(common_host_c_includes)
LOCAL_CFLAGS := $(common_host_cflags) $(common_host_extra_flags)
LOCAL_LDLIBS := $(common_ldlibs)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= opannotate

include $(BUILD_HOST_EXECUTABLE)

# Build opgprof
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	$(common_src) \
	opgprof.cpp \
	opgprof_options.cpp

LOCAL_STATIC_LIBRARIES := $(common_libs)
LOCAL_C_INCLUDES := $(common_host_c_includes)
LOCAL_CFLAGS := $(common_host_cflags)
LOCAL_LDLIBS := $(common_ldlibs)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= opgprof

include $(BUILD_HOST_EXECUTABLE)

# Build oparchive
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	$(common_src) \
	oparchive.cpp \
	oparchive_options.cpp

LOCAL_STATIC_LIBRARIES := $(common_libs)
LOCAL_C_INCLUDES := $(common_host_c_includes)
LOCAL_CFLAGS := $(common_host_cflags)
LOCAL_LDLIBS := $(common_ldlibs)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= oparchive

include $(BUILD_HOST_EXECUTABLE)

endif

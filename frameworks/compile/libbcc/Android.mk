#
# Copyright (C) 2010-2012 The Android Open Source Project
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

# Don't build for unbundled branches
ifeq (,$(TARGET_BUILD_APPS))

LOCAL_PATH := $(call my-dir)
LIBBCC_ROOT_PATH := $(LOCAL_PATH)
include $(LIBBCC_ROOT_PATH)/libbcc.mk

#=====================================================================
# Whole Static Library to Be Linked In
#=====================================================================

libbcc_WHOLE_STATIC_LIBRARIES += \
  libbccRenderscript \
  libbccExecutionEngine \
  libbccCore \
  libbccSupport

#=====================================================================
# Calculate SHA1 checksum for libbcc.so, libRS.so and libclcore.bc
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbcc.sha1
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

libbcc_SHA1_SRCS := \
  $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/libbcc.so \
  $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/libcompiler_rt.so \
  $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/libRS.so \
  $(call intermediates-dir-for,SHARED_LIBRARIES,libclcore.bc,,)/libclcore.bc \
  $(call intermediates-dir-for,SHARED_LIBRARIES,libclcore_debug.bc,,)/libclcore_debug.bc

ifeq ($(ARCH_ARM_HAVE_NEON),true)
  libbcc_SHA1_SRCS += \
    $(call intermediates-dir-for,SHARED_LIBRARIES,libclcore_neon.bc,,)/libclcore_neon.bc
endif

libbcc_GEN_SHA1_STAMP := $(LOCAL_PATH)/tools/build/gen-sha1-stamp.py
intermediates := $(call local-intermediates-dir)

libbcc_SHA1_ASM := $(intermediates)/libbcc.sha1.S
LOCAL_GENERATED_SOURCES += $(libbcc_SHA1_ASM)
$(libbcc_SHA1_ASM): PRIVATE_SHA1_SRCS := $(libbcc_SHA1_SRCS)
$(libbcc_SHA1_ASM): $(libbcc_SHA1_SRCS) $(libbcc_GEN_SHA1_STAMP)
	@echo libbcc.sha1: $@
	$(hide) mkdir -p $(dir $@)
	$(hide) $(libbcc_GEN_SHA1_STAMP) $(PRIVATE_SHA1_SRCS) > $@

LOCAL_CFLAGS += -D_REENTRANT -DPIC -fPIC
LOCAL_CFLAGS += -O3 -nodefaultlibs -nostdlib

include $(BUILD_SHARED_LIBRARY)

#=====================================================================
# Device Shared Library libbcc
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbcc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

LOCAL_WHOLE_STATIC_LIBRARIES := $(libbcc_WHOLE_STATIC_LIBRARIES)

LOCAL_WHOLE_STATIC_LIBRARIES += librsloader

LOCAL_SHARED_LIBRARIES := libbcinfo libLLVM libdl libutils libcutils liblog libstlport

# Modules that need get installed if and only if the target libbcc.so is
# installed.
LOCAL_REQUIRED_MODULES := libclcore.bc libclcore_debug.bc libbcc.sha1 libcompiler_rt

ifeq ($(ARCH_X86_HAVE_SSE2),true)
LOCAL_REQUIRED_MODULES += libclcore_x86.bc
endif

ifeq ($(ARCH_ARM_HAVE_NEON),true)
  LOCAL_REQUIRED_MODULES += libclcore_neon.bc
endif

# Generate build information (Build time + Build git revision + Build Semi SHA1)
include $(LIBBCC_ROOT_PATH)/libbcc-gen-build-info.mk

include $(LIBBCC_DEVICE_BUILD_MK)
include $(BUILD_SHARED_LIBRARY)

#=====================================================================
# Host Shared Library libbcc
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libbcc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_IS_HOST_MODULE := true

LOCAL_WHOLE_STATIC_LIBRARIES += $(libbcc_WHOLE_STATIC_LIBRARIES)

LOCAL_WHOLE_STATIC_LIBRARIES += librsloader

LOCAL_STATIC_LIBRARIES += \
  libutils \
  libcutils \
  liblog

LOCAL_SHARED_LIBRARIES := libbcinfo libLLVM

ifndef USE_MINGW
LOCAL_LDLIBS := -ldl -lpthread
endif

# Generate build information (Build time + Build git revision + Build Semi SHA1)
include $(LIBBCC_ROOT_PATH)/libbcc-gen-build-info.mk

include $(LIBBCC_HOST_BUILD_MK)
include $(BUILD_HOST_SHARED_LIBRARY)

endif # Don't build in unbundled branches

#=====================================================================
# Include Subdirectories
#=====================================================================
include $(call all-makefiles-under,$(LOCAL_PATH))

#
# Copyright (C) 2013 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

# C/LLVM-IR source files for the library
clcore_base_files := \
    rs_allocation.c \
    rs_cl.c \
    rs_core.c \
    rs_element.c \
    rs_mesh.c \
    rs_matrix.c \
    rs_program.c \
    rs_sample.c \
    rs_sampler.c \
    convert.ll \
    allocation.ll \
    rsClamp.ll

clcore_files := \
    $(clcore_base_files) \
    math.ll \
    arch/generic.c

clcore_neon_files := \
    $(clcore_base_files) \
    math.ll \
    arch/neon.ll \
    arch/clamp.c

ifeq ($(ARCH_X86_HAVE_SSE2), true)
    clcore_x86_files := \
    $(clcore_base_files) \
    arch/generic.c \
    arch/x86_sse2.ll

    # FIXME: without SSE3, it is still able to get better code through PSHUFD. But,
    # so far, there is no such device with SSE2 only.
    ifeq ($(ARCH_X86_HAVE_SSE3), true)
        clcore_x86_files += arch/x86_sse3.ll
    endif
endif

ifeq "REL" "$(PLATFORM_VERSION_CODENAME)"
  RS_VERSION := $(PLATFORM_SDK_VERSION)
else
  # Increment by 1 whenever this is not a final release build, since we want to
  # be able to see the RS version number change during development.
  # See build/core/version_defaults.mk for more information about this.
  RS_VERSION := "(1 + $(PLATFORM_SDK_VERSION))"
endif

# Build the base version of the library
include $(CLEAR_VARS)
LOCAL_MODULE := libclcore.bc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES := $(clcore_files)

include $(LOCAL_PATH)/build_bc_lib.mk

# Build a debug version of the library
include $(CLEAR_VARS)
LOCAL_MODULE := libclcore_debug.bc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
rs_debug_runtime := 1
LOCAL_SRC_FILES := $(clcore_files)

include $(LOCAL_PATH)/build_bc_lib.mk

# Build an optimized version of the library if the device is SSE2- or above
# capable.
ifeq ($(ARCH_X86_HAVE_SSE2),true)
include $(CLEAR_VARS)
LOCAL_MODULE := libclcore_x86.bc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES := $(clcore_x86_files)

include $(LOCAL_PATH)/build_bc_lib.mk
endif

# Build a NEON-enabled version of the library (if possible)
ifeq ($(ARCH_ARM_HAVE_NEON),true)
  include $(CLEAR_VARS)
  LOCAL_MODULE := libclcore_neon.bc
  LOCAL_MODULE_TAGS := optional
  LOCAL_MODULE_CLASS := SHARED_LIBRARIES
  LOCAL_SRC_FILES := $(clcore_neon_files)
  LOCAL_CFLAGS += -DARCH_ARM_HAVE_NEON

  include $(LOCAL_PATH)/build_bc_lib.mk
endif

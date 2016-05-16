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
#

LOCAL_PATH := $(call my-dir)

# The following list contains platform-independent functionalities.
#
# Skip apple_versioning.c since it is unused.
# Skip atomic.c since it needs to be built separately according to the docs.
# Skip clear_cache.c since it redefines a system function on Android.
# Skip gcc_personality_v0.c since it depends on libunwind.
libcompiler_rt_common_SRC_FILES := \
  lib/absvdi2.c \
  lib/absvsi2.c \
  lib/absvti2.c \
  lib/adddf3.c \
  lib/addsf3.c \
  lib/addvdi3.c \
  lib/addvsi3.c \
  lib/addvti3.c \
  lib/ashldi3.c \
  lib/ashlti3.c \
  lib/ashrdi3.c \
  lib/ashrti3.c \
  lib/clzdi2.c \
  lib/clzsi2.c \
  lib/clzti2.c \
  lib/cmpdi2.c \
  lib/cmpti2.c \
  lib/comparedf2.c \
  lib/comparesf2.c \
  lib/ctzdi2.c \
  lib/ctzsi2.c \
  lib/ctzti2.c \
  lib/divdc3.c \
  lib/divdf3.c \
  lib/divdi3.c \
  lib/divmoddi4.c \
  lib/divmodsi4.c \
  lib/divsc3.c \
  lib/divsf3.c \
  lib/divsi3.c \
  lib/divti3.c \
  lib/divxc3.c \
  lib/enable_execute_stack.c \
  lib/eprintf.c \
  lib/extendsfdf2.c \
  lib/ffsdi2.c \
  lib/ffsti2.c \
  lib/fixdfdi.c \
  lib/fixdfsi.c \
  lib/fixdfti.c \
  lib/fixsfdi.c \
  lib/fixsfsi.c \
  lib/fixsfti.c \
  lib/fixunsdfdi.c \
  lib/fixunsdfsi.c \
  lib/fixunsdfti.c \
  lib/fixunssfdi.c \
  lib/fixunssfsi.c \
  lib/fixunssfti.c \
  lib/fixunsxfdi.c \
  lib/fixunsxfsi.c \
  lib/fixunsxfti.c \
  lib/fixxfdi.c \
  lib/fixxfti.c \
  lib/floatdidf.c \
  lib/floatdisf.c \
  lib/floatdixf.c \
  lib/floatsidf.c \
  lib/floatsisf.c \
  lib/floattidf.c \
  lib/floattisf.c \
  lib/floattixf.c \
  lib/floatundidf.c \
  lib/floatundisf.c \
  lib/floatundixf.c \
  lib/floatunsidf.c \
  lib/floatunsisf.c \
  lib/floatuntidf.c \
  lib/floatuntisf.c \
  lib/floatuntixf.c \
  lib/int_util.c \
  lib/lshrdi3.c \
  lib/lshrti3.c \
  lib/moddi3.c \
  lib/modsi3.c \
  lib/modti3.c \
  lib/muldc3.c \
  lib/muldf3.c \
  lib/muldi3.c \
  lib/mulodi4.c \
  lib/mulosi4.c \
  lib/muloti4.c \
  lib/mulsc3.c \
  lib/mulsf3.c \
  lib/multi3.c \
  lib/mulvdi3.c \
  lib/mulvsi3.c \
  lib/mulvti3.c \
  lib/mulxc3.c \
  lib/negdf2.c \
  lib/negdi2.c \
  lib/negsf2.c \
  lib/negti2.c \
  lib/negvdi2.c \
  lib/negvsi2.c \
  lib/negvti2.c \
  lib/paritydi2.c \
  lib/paritysi2.c \
  lib/parityti2.c \
  lib/popcountdi2.c \
  lib/popcountsi2.c \
  lib/popcountti2.c \
  lib/powidf2.c \
  lib/powisf2.c \
  lib/powitf2.c \
  lib/powixf2.c \
  lib/subdf3.c \
  lib/subsf3.c \
  lib/subvdi3.c \
  lib/subvsi3.c \
  lib/subvti3.c \
  lib/trampoline_setup.c \
  lib/truncdfsf2.c \
  lib/ucmpdi2.c \
  lib/ucmpti2.c \
  lib/udivdi3.c \
  lib/udivmoddi4.c \
  lib/udivmodsi4.c \
  lib/udivmodti4.c \
  lib/udivsi3.c \
  lib/udivti3.c \
  lib/umoddi3.c \
  lib/umodsi3.c \
  lib/umodti3.c

# ARM-specific runtimes
libcompiler_rt_arm_SRC_FILES := \
  lib/arm/aeabi_dcmp.S \
  lib/arm/aeabi_fcmp.S \
  lib/arm/aeabi_idivmod.S \
  lib/arm/aeabi_ldivmod.S \
  lib/arm/aeabi_memcmp.S \
  lib/arm/aeabi_memcpy.S \
  lib/arm/aeabi_memmove.S \
  lib/arm/aeabi_memset.S \
  lib/arm/aeabi_uidivmod.S \
  lib/arm/aeabi_uldivmod.S \
  lib/arm/comparesf2.S \
  lib/arm/divmodsi4.S \
  lib/arm/divsi3.S \
  lib/arm/modsi3.S \
  lib/arm/udivmodsi4.S \
  lib/arm/udivsi3.S \
  lib/arm/umodsi3.S

# MIPS-specific runtimes
libcompiler_rt_mips_SRC_FILES := # nothing to add

# X86-specific runtimes
#
# We don't support x86-64 right now
libcompiler_rt_x86_SRC_FILES := \
  lib/i386/ashldi3.S \
  lib/i386/ashrdi3.S \
  lib/i386/divdi3.S \
  lib/i386/floatdidf.S \
  lib/i386/floatdisf.S \
  lib/i386/floatdixf.S \
  lib/i386/floatundidf.S \
  lib/i386/floatundisf.S \
  lib/i386/floatundixf.S \
  lib/i386/lshrdi3.S \
  lib/i386/moddi3.S \
  lib/i386/muldi3.S \
  lib/i386/udivdi3.S \
  lib/i386/umoddi3.S

# The following list contains functions that are not available in libgcc.a, so
# we potentially need them when using a Clang-built component (e.g., -ftrapv
# with 64-bit integer multiplies. See http://llvm.org/bugs/show_bug.cgi?id=14469.)
libcompiler_rt_extras_SRC_FILES := \
  lib/mulodi4.c

# $(1): arch
define get-libcompiler-rt-source-files
  $(if $(findstring $(1),arm),$(call get-libcompiler-rt-arm-source-files),
      $(if $(findstring $(1),mips),$(call get-libcompiler-rt-mips-source-files),
          $(if $(findstring $(1),x86),$(call get-libcompiler-rt-x86-source-files),
  $(error Unsupported ARCH $(1)))))
endef

# $(1): source list
# $(2): arch
#
# If lib/<arch>/X.S is included in the source list, we should filter out lib/X.c
# in the result source list (i.e., use the one optimized for the arch.) Otherwise
# there'll be multiple definitions for one symbol.
define filter-libcompiler-rt-common-source-files
  $(filter-out $(patsubst lib/$(strip $(2))/%.S,lib/%.c,$(filter %.S,$(1))),$(1))
endef

define get-libcompiler-rt-arm-common-source-files
  $(call filter-libcompiler-rt-common-source-files,
      $(libcompiler_rt_common_SRC_FILES) \
      $(libcompiler_rt_arm_SRC_FILES), arm)
endef

# $(1): common runtime list
#
# Add ARM runtimes implemented in VFP
define add-libcompiler-rt-arm-vfp-source-files
  $(filter-out $(addprefix lib/,adddf3.c addsf3.c comparedf2.c comparesf2.c         \
                                arm/comparesf2.S divdf3.c divsf3.c extendsfdf2.c    \
                                fixdfsi.c fixsfsi.c fixunsdfsi.c fixunssfsi.c       \
                                floatsidf.c floatsisf.c floatunsidf.c floatunsisf.c \
                                muldf3.c mulsf3.c negdf2.c negsf2.c subdf3.c        \
                                subsf3.c truncdfsf2.c),$(1)) lib/arm/vfp_alias.S
endef

define get-libcompiler-rt-arm-source-files
  $(if $(findstring $(ARCH_ARM_HAVE_VFP),true),
      $(call add-libcompiler-rt-arm-vfp-source-files,
          $(call get-libcompiler-rt-arm-common-source-files)),
      $(call get-libcompiler-rt-arm-common-source-files))
endef

define get-libcompiler-rt-mips-source-files
  $(call filter-libcompiler-rt-common-source-files,
      $(libcompiler_rt_common_SRC_FILES) \
      $(libcompiler_rt_mips_SRC_FILES),mips)
endef

define get-libcompiler-rt-x86-source-files
  $(call filter-libcompiler-rt-common-source-files,
      $(libcompiler_rt_common_SRC_FILES) \
      $(libcompiler_rt_x86_SRC_FILES),i386)
endef

# $(1): target or host
# $(2): static or shared
define build-libcompiler-rt
  ifneq ($(1),target)
    ifneq ($(1),host)
      $$(error expected target or host for argument 1, received $(1))
    endif
  endif
  ifneq ($(2),static)
    ifneq ($(2),shared)
      $$(error expected static or shared for argument 2, received $(2))
    endif
  endif

  target_or_host := $(1)
  static_or_shared := $(2)

  arch :=
  ifeq ($$(target_or_host),target)
    arch := $(TARGET_ARCH)
  else
    arch := $(HOST_ARCH)
  endif

  include $(CLEAR_VARS)

  LOCAL_MODULE := libcompiler_rt
  LOCAL_MODULE_TAGS := optional

  ifeq ($$(static_or_shared),static)
    LOCAL_MODULE_CLASS := STATIC_LIBRARIES
  else
    LOCAL_MODULE_CLASS := SHARED_LIBRARIES
  endif

  # TODO: Fix -integrated-as
  # LOCAL_CFLAGS := -integrated-as

  # Add -D__ARM_EABI__ for ARM
  ifeq ($$(arch),arm)
    LOCAL_CFLAGS += -D__ARM_EABI__
  endif

  # Use MC assembler to compile assembly
  LOCAL_ASFLAGS += -integrated-as

  # Use Clang to compile libcompiler_rt
  LOCAL_CLANG := true
  LOCAL_SRC_FILES := $$(call get-libcompiler-rt-source-files,$$(arch))
  LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

  ifeq ($$(target_or_host),target)
    ifeq ($$(static_or_shared),static)
      include $(BUILD_STATIC_LIBRARY)
    else
      include $(BUILD_SHARED_LIBRARY)
    endif
  else
    LOCAL_IS_HOST_MODULE := true
    ifeq ($$(static_or_shared),static)
      include $(BUILD_HOST_STATIC_LIBRARY)
    else
      include $(BUILD_HOST_SHARED_LIBRARY)
    endif
  endif
endef

#=====================================================================
# Device Static Library: libcompiler_rt-extras
#=====================================================================

include $(CLEAR_VARS)

LOCAL_MODULE := libcompiler_rt-extras
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_CLANG := true
LOCAL_SRC_FILES := $(libcompiler_rt_extras_SRC_FILES)
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

include $(BUILD_STATIC_LIBRARY)

#=====================================================================
# Device Static Library: libcompiler_rt
#=====================================================================
$(eval $(call build-libcompiler-rt,target,static))

#=====================================================================
# Device Shared Library: libcompiler_rt
#=====================================================================
$(eval $(call build-libcompiler-rt,target,shared))

# Build ASan
include $(LOCAL_PATH)/lib/asan/Android.mk

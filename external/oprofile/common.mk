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

# Common flags
common_c_includes := \
	external/oprofile \
	external/oprofile/libabi \
	external/oprofile/libdb \
	external/oprofile/libop \
	external/oprofile/libop++ \
	external/oprofile/libopt++ \
	external/oprofile/libpp \
	external/oprofile/libregex \
	external/oprofile/libutil \
	external/oprofile/libutil++

common_cflags := -DHAVE_CONFIG_H

# Common target flags
common_target_c_includes := $(common_c_includes)
common_target_cflags := $(common_cflags)

# Common host flags
HAVE_LIBBFD := false

ifeq ($(TARGET_ARCH),arm)
toolchain := prebuilts/gcc/$(HOST_PREBUILT_TAG)/arm/arm-linux-androideabi-4.6
common_host_extra_flags := -DANDROID_TARGET_ARM
endif

ifeq ($(TARGET_ARCH),mips)
toolchain := prebuilts/gcc/$(HOST_PREBUILT_TAG)/mips/mipsel-linux-android-4.6
common_host_extra_flags := -DANDROID_TARGET_MIPS
endif

ifneq ($(filter arm mips,$(TARGET_ARCH)),)
common_host_c_includes := $(common_c_includes) $(toolchain)/include
common_host_cflags := $(common_cflags) -fexceptions -DANDROID_HOST -DHAVE_XCALLOC
common_host_ldlibs_libiconv :=

ifeq ($(HOST_OS)-$(HOST_ARCH),darwin-x86)
HAVE_LIBBFD := true
common_host_cflags += -DMISSING_MREMAP
common_host_ldlibs_libiconv := -liconv
else
ifeq ($(HOST_OS)-$(HOST_ARCH),linux-x86)
HAVE_LIBBFD := true
endif
endif

endif

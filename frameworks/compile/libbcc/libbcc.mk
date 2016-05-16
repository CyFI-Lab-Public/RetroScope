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

ifeq ($(LIBBCC_ROOT_PATH),)
$(error Must set variable LIBBCC_ROOT_PATH before including this! $(LOCAL_PATH))
endif

#=====================================================================
# Root Path for Other Projects
#=====================================================================

LLVM_ROOT_PATH          := external/llvm
MCLD_ROOT_PATH          := frameworks/compile/mclinker
RSLOADER_ROOT_PATH      := frameworks/rs/cpu_ref/linkloader

#=====================================================================
# Related Makefile Paths of libbcc
#=====================================================================

LIBBCC_HOST_BUILD_MK    := $(LIBBCC_ROOT_PATH)/libbcc-host-build.mk
LIBBCC_DEVICE_BUILD_MK  := $(LIBBCC_ROOT_PATH)/libbcc-device-build.mk
LIBBCC_GEN_CONFIG_MK    := $(LIBBCC_ROOT_PATH)/libbcc-gen-config-from-mk.mk

#=====================================================================
# Configuration of libbcc
#=====================================================================
include $(LIBBCC_ROOT_PATH)/libbcc-config.mk

#=====================================================================
# Related Makefile Paths of LLVM
#=====================================================================
include $(LLVM_ROOT_PATH)/llvm.mk

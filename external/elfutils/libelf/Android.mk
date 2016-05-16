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

LOCAL_PATH := $(call my-dir)

supported_platforms := linux-x86 darwin-x86
cur_platform := $(filter $(HOST_OS)-$(HOST_ARCH),$(supported_platforms))

ifdef cur_platform

#
# host libelf
#

include $(CLEAR_VARS)

LIBELF_SRC_FILES := \
	elf32_checksum.c \
	elf32_fsize.c \
	elf32_getehdr.c \
	elf32_getphdr.c \
	elf32_getshdr.c \
	elf32_newehdr.c \
	elf32_newphdr.c \
	elf32_offscn.c \
	elf32_updatefile.c \
	elf32_updatenull.c \
	elf32_xlatetof.c \
	elf32_xlatetom.c \
	elf64_checksum.c \
	elf64_fsize.c \
	elf64_getehdr.c \
	elf64_getphdr.c \
	elf64_getshdr.c \
	elf64_newehdr.c \
	elf64_newphdr.c \
	elf64_offscn.c \
	elf64_updatefile.c \
	elf64_updatenull.c \
	elf64_xlatetof.c \
	elf64_xlatetom.c \
	elf_begin.c \
	elf_clone.c \
	elf_cntl.c \
	elf_end.c \
	elf_error.c \
	elf_fill.c \
	elf_flagdata.c \
	elf_flagehdr.c \
	elf_flagelf.c \
	elf_flagphdr.c \
	elf_flagscn.c \
	elf_flagshdr.c \
	elf_getarhdr.c \
	elf_getaroff.c \
	elf_getarsym.c \
	elf_getbase.c \
	elf_getdata.c \
	elf_getdata_rawchunk.c \
	elf_getident.c \
	elf_getscn.c \
	elf_getshnum.c \
	elf_getshstrndx.c \
	elf_gnu_hash.c \
	elf_hash.c \
	elf_kind.c \
	elf_memory.c \
	elf_ndxscn.c \
	elf_newdata.c \
	elf_newscn.c \
	elf_next.c \
	elf_nextscn.c \
	elf_rand.c \
	elf_rawdata.c \
	elf_rawfile.c \
	elf_readall.c \
	elf_scnshndx.c \
	elf_strptr.c \
	elf_update.c \
	elf_version.c \
	gelf_checksum.c \
	gelf_fsize.c \
	gelf_getauxv.c \
	gelf_getclass.c \
	gelf_getdyn.c \
	gelf_getehdr.c \
	gelf_getlib.c \
	gelf_getmove.c \
	gelf_getnote.c \
	gelf_getphdr.c \
	gelf_getrela.c \
	gelf_getrel.c \
	gelf_getshdr.c \
	gelf_getsym.c \
	gelf_getsyminfo.c \
	gelf_getsymshndx.c \
	gelf_getverdaux.c \
	gelf_getverdef.c \
	gelf_getvernaux.c \
	gelf_getverneed.c \
	gelf_getversym.c \
	gelf_newehdr.c \
	gelf_newphdr.c \
	gelf_offscn.c \
	gelf_update_auxv.c \
	gelf_update_dyn.c \
	gelf_update_ehdr.c \
	gelf_update_lib.c \
	gelf_update_move.c \
	gelf_update_phdr.c \
	gelf_update_rela.c \
	gelf_update_rel.c \
	gelf_update_shdr.c \
	gelf_update_sym.c \
	gelf_update_syminfo.c \
	gelf_update_symshndx.c \
	gelf_update_verdaux.c \
	gelf_update_verdef.c \
	gelf_update_vernaux.c \
	gelf_update_verneed.c \
	gelf_update_versym.c \
	gelf_xlate.c \
	gelf_xlatetof.c \
	gelf_xlatetom.c \
	libelf_crc32.c \
	libelf_next_prime.c \
	nlist.c

LOCAL_SRC_FILES := $(LIBELF_SRC_FILES)

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../lib \
	$(LOCAL_PATH)/../libelf

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../host-$(HOST_OS)-fixup

LOCAL_CFLAGS += -DHAVE_CONFIG_H -std=gnu99 -D_GNU_SOURCE

# to suppress the "pointer of type ‘void *’ used in arithmetic" warning
LOCAL_CFLAGS += -Wno-pointer-arith

ifeq ($(HOST_OS),darwin)
	LOCAL_CFLAGS += -fnested-functions
endif

# to fix machine-dependent issues
LOCAL_CFLAGS += -include $(LOCAL_PATH)/../host-$(HOST_OS)-fixup/AndroidFixup.h

LOCAL_MODULE := libelf

include $(BUILD_HOST_STATIC_LIBRARY)

#
# target libelf
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LIBELF_SRC_FILES)

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../lib \
	$(LOCAL_PATH)/../libelf

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../bionic-fixup

LOCAL_CFLAGS += -DHAVE_CONFIG_H -std=gnu99 -Werror

# to suppress the "pointer of type ‘void *’ used in arithmetic" warning
LOCAL_CFLAGS += -Wno-pointer-arith

LOCAL_CFLAGS += -include $(LOCAL_PATH)/../bionic-fixup/AndroidFixup.h

LOCAL_MODULE := libelf

include $(BUILD_STATIC_LIBRARY)

endif #cur_platform

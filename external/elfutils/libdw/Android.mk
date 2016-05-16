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
# host libdw
#

include $(CLEAR_VARS)

LIBDW_SRC_FILES := \
        dwarf_abbrevhaschildren.c \
        dwarf_abbrev_hash.c \
        dwarf_addrdie.c \
        dwarf_arrayorder.c \
        dwarf_attr.c \
        dwarf_attr_integrate.c \
        dwarf_begin.c \
        dwarf_begin_elf.c \
        dwarf_bitoffset.c \
        dwarf_bitsize.c \
        dwarf_bytesize.c \
        dwarf_child.c \
        dwarf_cuoffset.c \
        dwarf_decl_column.c \
        dwarf_decl_file.c \
        dwarf_decl_line.c \
        dwarf_diecu.c \
        dwarf_diename.c \
        dwarf_dieoffset.c \
        dwarf_end.c \
        dwarf_entry_breakpoints.c \
        dwarf_entrypc.c \
        dwarf_error.c \
        dwarf_filesrc.c \
        dwarf_formaddr.c \
        dwarf_formblock.c \
        dwarf_formflag.c \
        dwarf_formref.c \
        dwarf_formref_die.c \
        dwarf_formsdata.c \
        dwarf_formstring.c \
        dwarf_formudata.c \
        dwarf_func_inline.c \
        dwarf_getabbrevattr.c \
        dwarf_getabbrev.c \
        dwarf_getabbrevcode.c \
        dwarf_getabbrevtag.c \
        dwarf_getarange_addr.c \
        dwarf_getarangeinfo.c \
        dwarf_getaranges.c \
        dwarf_getattrcnt.c \
        dwarf_getattrs.c \
        dwarf_getelf.c \
        dwarf_getfuncs.c \
        dwarf_getlocation.c \
        dwarf_getmacros.c \
        dwarf_getpubnames.c \
        dwarf_getscopes.c \
        dwarf_getscopes_die.c \
        dwarf_getscopevar.c \
        dwarf_getsrc_die.c \
        dwarf_getsrcdirs.c \
        dwarf_getsrc_file.c \
        dwarf_getsrcfiles.c \
        dwarf_getsrclines.c \
        dwarf_getstring.c \
        dwarf_hasattr.c \
        dwarf_hasattr_integrate.c \
        dwarf_haschildren.c \
        dwarf_hasform.c \
        dwarf_haspc.c \
        dwarf_highpc.c \
        dwarf_lineaddr.c \
        dwarf_linebeginstatement.c \
        dwarf_lineblock.c \
        dwarf_linecol.c \
        dwarf_lineendsequence.c \
        dwarf_lineepiloguebegin.c \
        dwarf_lineno.c \
        dwarf_lineprologueend.c \
        dwarf_linesrc.c \
        dwarf_lowpc.c \
        dwarf_macro_opcode.c \
        dwarf_macro_param1.c \
        dwarf_macro_param2.c \
        dwarf_nextcu.c \
        dwarf_offabbrev.c \
        dwarf_offdie.c \
        dwarf_onearange.c \
        dwarf_onesrcline.c \
        dwarf_ranges.c \
        dwarf_siblingof.c \
        dwarf_srclang.c \
        dwarf_tag.c \
        dwarf_whatattr.c \
        dwarf_whatform.c \
        libdw_alloc.c \
        libdw_findcu.c \
        libdw_form.c \
        libdw_visit_scopes.c \
        memory-access.c

LOCAL_SRC_FILES := $(LIBDW_SRC_FILES)

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../lib \
	$(LOCAL_PATH)/../libdw \
	$(LOCAL_PATH)/../libelf

ifeq ($(HOST_OS),darwin)
	LOCAL_CFLAGS += -fnested-functions
endif

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../host-$(HOST_OS)-fixup

LOCAL_CFLAGS += -DHAVE_CONFIG_H -std=gnu99 -D_GNU_SOURCE -DIS_LIBDW

# to fix machine-dependent issues
LOCAL_CFLAGS += -include $(LOCAL_PATH)/../host-$(HOST_OS)-fixup/AndroidFixup.h

LOCAL_MODULE:= libdw

include $(BUILD_HOST_STATIC_LIBRARY)

#
# target libdw
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(LIBDW_SRC_FILES)

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../lib \
	$(LOCAL_PATH)/../libdw \
	$(LOCAL_PATH)/../libelf

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../bionic-fixup

LOCAL_CFLAGS += -include $(LOCAL_PATH)/../bionic-fixup/AndroidFixup.h

LOCAL_CFLAGS += -DHAVE_CONFIG_H -std=gnu99 -DIS_LIBDW -Werror

# to suppress the "pointer of type ‘void *’ used in arithmetic" warning
LOCAL_CFLAGS += -Wno-pointer-arith

LOCAL_MODULE_TAGS := eng

LOCAL_MODULE:= libdw

include $(BUILD_STATIC_LIBRARY)

endif #cur_platform

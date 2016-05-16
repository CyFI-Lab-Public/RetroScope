#
# Copyright (C) 2010 The Android Open Source Project
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

# The prebuilt tools should be used when we are doing app-only build.
ifeq ($(TARGET_BUILD_APPS),)

LOCAL_PATH := $(call my-dir)

local_cflags_for_slang := -Wno-sign-promo -Wall -Wno-unused-parameter -Werror
ifeq ($(TARGET_BUILD_VARIANT),eng)
local_cflags_for_slang += -O0
else
local_cflags_for_slang += -D__DISABLE_ASSERTS
endif
local_cflags_for_slang += -DTARGET_BUILD_VARIANT=$(TARGET_BUILD_VARIANT)

ifeq "REL" "$(PLATFORM_VERSION_CODENAME)"
  RS_VERSION := $(PLATFORM_SDK_VERSION)
else
  # Increment by 1 whenever this is not a final release build, since we want to
  # be able to see the RS version number change during development.
  # See build/core/version_defaults.mk for more information about this.
  RS_VERSION := "(1 + $(PLATFORM_SDK_VERSION))"
endif
local_cflags_for_slang += -DRS_VERSION=$(RS_VERSION)

static_libraries_needed_by_slang := \
	libLLVMBitWriter_2_9 \
	libLLVMBitWriter_2_9_func \
	libLLVMBitWriter_3_2

# Static library libslang for host
# ========================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LLVM_ROOT_PATH := external/llvm
CLANG_ROOT_PATH := external/clang

include $(CLANG_ROOT_PATH)/clang.mk

LOCAL_MODULE := libslang
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += $(local_cflags_for_slang)

TBLGEN_TABLES :=    \
	AttrList.inc	\
	Attrs.inc	\
	CommentCommandList.inc \
	CommentNodes.inc \
	DeclNodes.inc	\
	DiagnosticCommonKinds.inc	\
	DiagnosticFrontendKinds.inc	\
	DiagnosticSemaKinds.inc	\
	StmtNodes.inc

LOCAL_SRC_FILES :=	\
	slang.cpp	\
	slang_utils.cpp	\
	slang_backend.cpp	\
	slang_pragma_recorder.cpp	\
	slang_diagnostic_buffer.cpp

LOCAL_C_INCLUDES += frameworks/compile/libbcc/include

LOCAL_LDLIBS := -ldl -lpthread

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# Host static library containing rslib.bc
# ========================================================
include $(CLEAR_VARS)

input_data_file := frameworks/compile/slang/rslib.bc
slangdata_output_var_name := rslib_bc

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE := librslib
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := STATIC_LIBRARIES

include $(LOCAL_PATH)/SlangData.mk
include $(BUILD_HOST_STATIC_LIBRARY)

# Executable slang-data for host
# ========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := slang-data
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_SRC_FILES := slang-data.c

include $(BUILD_HOST_EXECUTABLE)

# Executable rs-spec-gen for host
# ========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := rs-spec-gen
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_SRC_FILES :=	\
	slang_rs_spec_table.cpp

include $(BUILD_HOST_EXECUTABLE)

# ========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := llvm-rs-as
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_SRC_FILES :=	\
	llvm-rs-as.cpp

LOCAL_CFLAGS += $(local_cflags_for_slang)
LOCAL_STATIC_LIBRARIES :=	\
	libslang \
	$(static_libraries_needed_by_slang)
LOCAL_SHARED_LIBRARIES := \
	libLLVM

include $(CLANG_HOST_BUILD_MK)
include $(BUILD_HOST_EXECUTABLE)

# Executable llvm-rs-cc for host
# ========================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE := llvm-rs-cc
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_CFLAGS += $(local_cflags_for_slang)

TBLGEN_TABLES :=    \
	AttrList.inc    \
	Attrs.inc    \
	CommentCommandList.inc \
	CommentNodes.inc \
	DeclNodes.inc    \
	DiagnosticCommonKinds.inc   \
	DiagnosticDriverKinds.inc	\
	DiagnosticFrontendKinds.inc	\
	DiagnosticSemaKinds.inc	\
	StmtNodes.inc	\
	RSCCOptions.inc

RS_SPEC_TABLES :=	\
	RSClangBuiltinEnums.inc	\
	RSDataTypeEnums.inc	\
	RSDataElementEnums.inc	\
	RSMatrixTypeEnums.inc	\
	RSObjectTypeEnums.inc

LOCAL_SRC_FILES :=	\
	llvm-rs-cc.cpp	\
	slang_rs.cpp	\
	slang_rs_ast_replace.cpp	\
	slang_rs_check_ast.cpp	\
	slang_rs_context.cpp	\
	slang_rs_pragma_handler.cpp	\
	slang_rs_backend.cpp	\
	slang_rs_exportable.cpp	\
	slang_rs_export_type.cpp	\
	slang_rs_export_element.cpp	\
	slang_rs_export_var.cpp	\
	slang_rs_export_func.cpp	\
	slang_rs_export_foreach.cpp \
	slang_rs_object_ref_count.cpp	\
	slang_rs_reflection.cpp \
	slang_rs_reflection_base.cpp \
	slang_rs_reflection_cpp.cpp \
	slang_rs_reflect_utils.cpp

LOCAL_STATIC_LIBRARIES :=	\
	libslang \
	$(static_libraries_needed_by_slang)

LOCAL_SHARED_LIBRARIES := \
	libclang \
	libLLVM

ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS := -limagehlp -lpsapi
else
  LOCAL_LDLIBS := -ldl -lpthread
endif

# For build RSCCOptions.inc from RSCCOptions.td
intermediates := $(call local-intermediates-dir)
LOCAL_GENERATED_SOURCES += $(intermediates)/RSCCOptions.inc
$(intermediates)/RSCCOptions.inc: $(LOCAL_PATH)/RSCCOptions.td $(LLVM_ROOT_PATH)/include/llvm/Option/OptParser.td $(TBLGEN)
	@echo "Building Renderscript compiler (llvm-rs-cc) Option tables with tblgen"
	$(call transform-host-td-to-out,opt-parser-defs)

include frameworks/compile/slang/RSSpec.mk
include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_EXECUTABLE)

endif  # TARGET_BUILD_APPS

#=====================================================================
# Include Subdirectories
#=====================================================================
include $(call all-makefiles-under,$(LOCAL_PATH))

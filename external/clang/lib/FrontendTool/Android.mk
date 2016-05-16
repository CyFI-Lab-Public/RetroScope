LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libclangFrontendTool

LOCAL_MODULE_TAGS := optional

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DiagnosticFrontendKinds.inc \
  CC1Options.inc

clang_frontend_tool_SRC_FILES := \
  ExecuteCompilerInvocation.cpp

LOCAL_SRC_FILES := $(clang_frontend_tool_SRC_FILES)

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

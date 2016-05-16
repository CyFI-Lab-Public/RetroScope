LOCAL_PATH:= $(call my-dir)

clang_static_analyzer_frontend_C_INCLUDES := \
  $(CLANG_ROOT_PATH)/lib/StaticAnalyzer/Checkers

clang_static_analyzer_frontend_TBLGEN_TABLES := \
  AttrList.inc \
  Attrs.inc \
  CommentCommandList.inc \
  CommentNodes.inc \
  DeclNodes.inc \
  DiagnosticCommonKinds.inc \
  DiagnosticFrontendKinds.inc \
  StmtNodes.inc

clang_static_analyzer_frontend_SRC_FILES := \
  AnalysisConsumer.cpp \
  CheckerRegistration.cpp \
  FrontendActions.cpp

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := $(clang_static_analyzer_frontend_TBLGEN_TABLES)

LOCAL_SRC_FILES := $(clang_static_analyzer_frontend_SRC_FILES)

LOCAL_C_INCLUDES := $(clang_static_analyzer_frontend_C_INCLUDES)

LOCAL_MODULE:= libclangStaticAnalyzerFrontend

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(CLANG_VERSION_INC_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

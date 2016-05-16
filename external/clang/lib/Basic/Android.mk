LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticASTKinds.inc \
  DiagnosticAnalysisKinds.inc \
  DiagnosticCommentKinds.inc \
  DiagnosticCommonKinds.inc \
  DiagnosticDriverKinds.inc \
  DiagnosticFrontendKinds.inc \
  DiagnosticGroups.inc \
  DiagnosticIndexName.inc \
  DiagnosticLexKinds.inc \
  DiagnosticParseKinds.inc \
  DiagnosticSemaKinds.inc \
  DiagnosticSerializationKinds.inc \
  arm_neon.inc

clang_basic_SRC_FILES := \
  Builtins.cpp \
  CharInfo.cpp \
  Diagnostic.cpp \
  DiagnosticIDs.cpp \
  FileManager.cpp \
  FileSystemStatCache.cpp \
  IdentifierTable.cpp \
  LangOptions.cpp \
  Module.cpp \
  ObjCRuntime.cpp \
  OpenMPKinds.cpp \
  OperatorPrecedence.cpp \
  SourceLocation.cpp \
  SourceManager.cpp \
  TargetInfo.cpp \
  Targets.cpp \
  TokenKinds.cpp \
  Version.cpp \
  VersionTuple.cpp

LOCAL_SRC_FILES := $(clang_basic_SRC_FILES)

LOCAL_MODULE:= libclangBasic
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

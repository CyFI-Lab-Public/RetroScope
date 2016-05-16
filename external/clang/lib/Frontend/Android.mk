LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  AttrList.inc \
  Attrs.inc \
  AttrParsedAttrList.inc \
  CC1Options.inc \
  CommentCommandList.inc \
  CommentNodes.inc \
  DiagnosticASTKinds.inc \
  DiagnosticCommonKinds.inc \
  DiagnosticDriverKinds.inc \
  DiagnosticFrontendKinds.inc \
  DiagnosticLexKinds.inc \
  DiagnosticSemaKinds.inc \
  DeclNodes.inc \
  StmtNodes.inc

clang_frontend_SRC_FILES := \
  ASTConsumers.cpp \
  ASTMerge.cpp \
  ASTUnit.cpp \
  CacheTokens.cpp \
  ChainedDiagnosticConsumer.cpp \
  ChainedIncludesSource.cpp \
  CompilerInstance.cpp \
  CompilerInvocation.cpp \
  CreateInvocationFromCommandLine.cpp \
  DependencyFile.cpp \
  DependencyGraph.cpp \
  DiagnosticRenderer.cpp \
  FrontendAction.cpp \
  FrontendActions.cpp \
  FrontendOptions.cpp \
  HeaderIncludeGen.cpp \
  InitHeaderSearch.cpp \
  InitPreprocessor.cpp \
  LangStandards.cpp \
  LayoutOverrideSource.cpp \
  LogDiagnosticPrinter.cpp \
  MultiplexConsumer.cpp \
  PrintPreprocessedOutput.cpp \
  SerializedDiagnosticPrinter.cpp \
  TextDiagnostic.cpp \
  TextDiagnosticBuffer.cpp \
  TextDiagnosticPrinter.cpp \
  Warnings.cpp \
  VerifyDiagnosticConsumer.cpp

LOCAL_SRC_FILES := $(clang_frontend_SRC_FILES)

LOCAL_MODULE:= libclangFrontend
LOCAL_MODULE_TAGS:= optional

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(CLANG_VERSION_INC_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

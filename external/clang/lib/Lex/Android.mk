LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticLexKinds.inc \
  DiagnosticCommonKinds.inc \
  AttrSpellings.inc

clang_lex_SRC_FILES := \
  HeaderMap.cpp \
  HeaderSearch.cpp \
  Lexer.cpp \
  LiteralSupport.cpp \
  MacroArgs.cpp \
  MacroInfo.cpp \
  ModuleMap.cpp \
  PPCaching.cpp \
  PPCallbacks.cpp \
  PPConditionalDirectiveRecord.cpp \
  PPDirectives.cpp \
  PPExpressions.cpp \
  PPLexerChange.cpp \
  PPMacroExpansion.cpp \
  PTHLexer.cpp \
  Pragma.cpp \
  PreprocessingRecord.cpp \
  Preprocessor.cpp \
  PreprocessorLexer.cpp \
  ScratchBuffer.cpp \
  TokenConcatenation.cpp \
  TokenLexer.cpp

LOCAL_SRC_FILES := $(clang_lex_SRC_FILES)

LOCAL_MODULE:= libclangLex
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

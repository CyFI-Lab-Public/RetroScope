LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libclangRewriteFrontend

LOCAL_MODULE_TAGS := optional

TBLGEN_TABLES := \
  AttrList.inc \
  Attrs.inc \
  AttrParsedAttrList.inc \
  CommentCommandList.inc \
  CommentNodes.inc \
  DeclNodes.inc \
  DiagnosticCommonKinds.inc \
  DiagnosticFrontendKinds.inc \
  StmtNodes.inc

clang_rewrite_frontend_SRC_FILES := \
  FixItRewriter.cpp \
  FrontendActions.cpp \
  HTMLPrint.cpp \
  InclusionRewriter.cpp \
  RewriteMacros.cpp \
  RewriteModernObjC.cpp \
  RewriteObjC.cpp \
  RewriteTest.cpp

LOCAL_SRC_FILES := $(clang_rewrite_frontend_SRC_FILES)


include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

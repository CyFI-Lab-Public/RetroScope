LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libclangRewriteCore

LOCAL_MODULE_TAGS := optional

TBLGEN_TABLES := \
  AttrList.inc \
  Attrs.inc \
  AttrParsedAttrList.inc \
  CommentNodes.inc \
  DeclNodes.inc \
  DiagnosticCommonKinds.inc \
  DiagnosticFrontendKinds.inc \
  StmtNodes.inc

clang_rewrite_core_SRC_FILES := \
  DeltaTree.cpp \
  HTMLRewrite.cpp \
  RewriteRope.cpp \
  Rewriter.cpp \
  TokenRewriter.cpp

LOCAL_SRC_FILES := $(clang_rewrite_core_SRC_FILES)


include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

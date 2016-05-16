LOCAL_PATH:= $(call my-dir)

clang_edit_SRC_FILES := \
  Commit.cpp \
  EditedSource.cpp \
  RewriteObjCFoundationAPI.cpp


# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  Attrs.inc \
  AttrList.inc \
  CommentCommandList.inc \
  CommentNodes.inc \
  DeclNodes.inc \
  DiagnosticCommonKinds.inc \
  StmtNodes.inc

LOCAL_SRC_FILES := $(clang_edit_SRC_FILES)

LOCAL_MODULE:= libclangEdit

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_VERSION_INC_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

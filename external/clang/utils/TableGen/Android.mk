LOCAL_PATH:= $(call my-dir)

clang_tablegen_SRC_FILES := \
  ClangASTNodesEmitter.cpp \
  ClangAttrEmitter.cpp \
  ClangCommentCommandInfoEmitter.cpp \
  ClangCommentHTMLNamedCharacterReferenceEmitter.cpp \
  ClangCommentHTMLTagsEmitter.cpp \
  ClangDiagnosticsEmitter.cpp \
  ClangSACheckersEmitter.cpp \
  NeonEmitter.cpp \
  TableGen.cpp

include $(CLEAR_VARS)

LOCAL_MODULE := clang-tblgen
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(clang_tablegen_SRC_FILES)

REQUIRES_EH := 1
REQUIRES_RTTI := 1

LOCAL_STATIC_LIBRARIES := \
  libLLVMTableGen \
  libLLVMSupport

LOCAL_LDLIBS += -lm
ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS += -limagehlp -lpsapi
else
  LOCAL_LDLIBS += -lpthread -ldl
endif

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_EXECUTABLE)

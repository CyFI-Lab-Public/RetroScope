LOCAL_PATH:= $(call my-dir)

llvm_as_SRC_FILES := \
  llvm-as.cpp

include $(CLEAR_VARS)

LOCAL_MODULE := llvm-as
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(llvm_as_SRC_FILES)
LOCAL_LDLIBS += -lm
ifdef USE_MINGW
LOCAL_LDLIBS += -limagehlp
else
LOCAL_LDLIBS += -lpthread -ldl
endif

REQUIRES_EH := 1
REQUIRES_RTTI := 1

LOCAL_STATIC_LIBRARIES := \
  libLLVMAsmParser \
  libLLVMBitWriter \
  libLLVMCore \
  libLLVMSupport

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_EXECUTABLE)

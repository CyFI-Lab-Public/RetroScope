LOCAL_PATH:= $(call my-dir)

llvm_dis_SRC_FILES := \
  llvm-dis.cpp

include $(CLEAR_VARS)

LOCAL_MODULE := llvm-dis
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(llvm_dis_SRC_FILES)
LOCAL_LDLIBS += -lpthread -lm -ldl

REQUIRES_EH := 1
REQUIRES_RTTI := 1

LOCAL_STATIC_LIBRARIES := \
  libLLVMAnalysis \
  libLLVMBitReader \
  libLLVMCore \
  libLLVMSupport

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_EXECUTABLE)

LOCAL_PATH:= $(call my-dir)

transforms_vectorize_SRC_FILES := \
  BBVectorize.cpp \
  LoopVectorize.cpp \
  SLPVectorizer.cpp \
  Vectorize.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(transforms_vectorize_SRC_FILES)
LOCAL_MODULE:= libLLVMVectorize

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(transforms_vectorize_SRC_FILES)
LOCAL_MODULE:= libLLVMVectorize

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

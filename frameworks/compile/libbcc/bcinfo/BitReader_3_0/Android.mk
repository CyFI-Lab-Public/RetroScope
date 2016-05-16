LOCAL_PATH:= $(call my-dir)

LLVM_ROOT_PATH := $(LOCAL_PATH)/../../../../../external/llvm
include $(LLVM_ROOT_PATH)/llvm.mk

bitcode_reader_3_0_SRC_FILES := \
  BitcodeReader.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMBitReader_3_0
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(bitcode_reader_3_0_SRC_FILES)
LOCAL_CFLAGS += -D__HOST__

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMBitReader_3_0
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(bitcode_reader_3_0_SRC_FILES)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

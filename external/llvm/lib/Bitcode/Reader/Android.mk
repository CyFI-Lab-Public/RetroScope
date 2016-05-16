LOCAL_PATH:= $(call my-dir)

bitcode_reader_SRC_FILES := \
	BitReader.cpp	\
	BitcodeReader.cpp \
	BitstreamReader.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(bitcode_reader_SRC_FILES)

LOCAL_MODULE:= libLLVMBitReader

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(bitcode_reader_SRC_FILES)

LOCAL_MODULE:= libLLVMBitReader

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

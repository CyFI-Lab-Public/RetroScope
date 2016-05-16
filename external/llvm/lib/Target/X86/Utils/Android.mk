LOCAL_PATH := $(call my-dir)

x86_utils_SRC_FILES := \
  X86ShuffleDecode.cpp

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(x86_utils_SRC_FILES)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/..

LOCAL_MODULE:= libLLVMX86Utils

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(x86_utils_SRC_FILES)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/..

LOCAL_MODULE:= libLLVMX86Utils

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

LOCAL_PATH:= $(call my-dir)

option_SRC_FILES := \
    Arg.cpp \
    ArgList.cpp \
    Option.cpp \
    OptTable.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

# FIXME: This only requires RTTI because tblgen uses it.  Fix that.
REQUIRES_RTTI := 1

LOCAL_SRC_FILES := $(option_SRC_FILES)

LOCAL_MODULE:= libLLVMOption

LOCAL_CFLAGS := -D__android__

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(option_SRC_FILES)

LOCAL_MODULE:= libLLVMOption

LOCAL_CFLAGS := -D__android__

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

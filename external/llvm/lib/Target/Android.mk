LOCAL_PATH:= $(call my-dir)

target_SRC_FILES := \
  Mangler.cpp \
  Target.cpp \
  TargetIntrinsicInfo.cpp \
  TargetJITInfo.cpp \
  TargetLibraryInfo.cpp \
  TargetLoweringObjectFile.cpp \
  TargetMachineC.cpp \
  TargetMachine.cpp \
  TargetSubtargetInfo.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(target_SRC_FILES)

LOCAL_MODULE:= libLLVMTarget

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(target_SRC_FILES)

LOCAL_MODULE:= libLLVMTarget

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

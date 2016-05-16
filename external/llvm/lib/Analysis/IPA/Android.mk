LOCAL_PATH:= $(call my-dir)

analysis_ipa_SRC_FILES := \
  CallGraph.cpp \
  CallGraphSCCPass.cpp \
  CallPrinter.cpp \
  FindUsedTypes.cpp \
  GlobalsModRef.cpp \
  IPA.cpp \
  InlineCost.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(analysis_ipa_SRC_FILES)

LOCAL_MODULE:= libLLVMipa

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(analysis_ipa_SRC_FILES)

LOCAL_MODULE:= libLLVMipa

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

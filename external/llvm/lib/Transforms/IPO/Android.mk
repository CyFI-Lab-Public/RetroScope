LOCAL_PATH:= $(call my-dir)

transforms_ipo_SRC_FILES := \
  ArgumentPromotion.cpp \
  BarrierNoopPass.cpp \
  ConstantMerge.cpp \
  DeadArgumentElimination.cpp \
  ExtractGV.cpp \
  FunctionAttrs.cpp \
  GlobalDCE.cpp \
  GlobalOpt.cpp \
  IPConstantPropagation.cpp \
  IPO.cpp \
  InlineAlways.cpp \
  InlineSimple.cpp \
  Inliner.cpp \
  Internalize.cpp \
  LoopExtractor.cpp \
  MergeFunctions.cpp \
  PartialInlining.cpp \
  PassManagerBuilder.cpp \
  PruneEH.cpp \
  StripDeadPrototypes.cpp \
  StripSymbols.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(transforms_ipo_SRC_FILES)
LOCAL_MODULE:= libLLVMipo

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(transforms_ipo_SRC_FILES)
LOCAL_MODULE:= libLLVMipo

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

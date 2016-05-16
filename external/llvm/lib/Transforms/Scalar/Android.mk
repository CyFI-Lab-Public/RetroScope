LOCAL_PATH:= $(call my-dir)

transforms_scalar_SRC_FILES := \
  ADCE.cpp \
  BasicBlockPlacement.cpp \
  CodeGenPrepare.cpp \
  ConstantProp.cpp \
  CorrelatedValuePropagation.cpp \
  DCE.cpp \
  DeadStoreElimination.cpp \
  EarlyCSE.cpp \
  GlobalMerge.cpp \
  GVN.cpp \
  IndVarSimplify.cpp \
  JumpThreading.cpp \
  LICM.cpp \
  LoopDeletion.cpp \
  LoopIdiomRecognize.cpp \
  LoopInstSimplify.cpp \
  LoopRotation.cpp \
  LoopStrengthReduce.cpp \
  LoopUnrollPass.cpp \
  LoopUnswitch.cpp \
  LowerAtomic.cpp \
  MemCpyOptimizer.cpp \
  Reassociate.cpp \
  Reg2Mem.cpp \
  SCCP.cpp \
  SROA.cpp \
  Scalar.cpp \
  ScalarReplAggregates.cpp \
  SimplifyCFGPass.cpp \
  Sink.cpp \
  StructurizeCFG.cpp \
  TailRecursionElimination.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=	\
	$(transforms_scalar_SRC_FILES)

LOCAL_MODULE:= libLLVMScalarOpts

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(transforms_scalar_SRC_FILES)
LOCAL_MODULE:= libLLVMScalarOpts

# Override the default optimization level to work around a SIGSEGV
# on x86 target builds for SROA.cpp.
# Bug: 8047767
ifeq ($(TARGET_ARCH),x86)
LOCAL_CFLAGS += -O1
endif

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

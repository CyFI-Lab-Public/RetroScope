LOCAL_PATH := $(call my-dir)

arm_codegen_TBLGEN_TABLES := \
  ARMGenRegisterInfo.inc \
  ARMGenInstrInfo.inc \
  ARMGenCodeEmitter.inc \
  ARMGenMCCodeEmitter.inc \
  ARMGenMCPseudoLowering.inc \
  ARMGenAsmWriter.inc \
  ARMGenAsmMatcher.inc \
  ARMGenDAGISel.inc \
  ARMGenFastISel.inc \
  ARMGenCallingConv.inc \
  ARMGenSubtargetInfo.inc \
  ARMGenDisassemblerTables.inc

arm_codegen_SRC_FILES := \
  A15SDOptimizer.cpp \
  ARMAsmPrinter.cpp \
  ARMBaseInstrInfo.cpp \
  ARMBaseRegisterInfo.cpp \
  ARMCodeEmitter.cpp \
  ARMConstantIslandPass.cpp \
  ARMConstantPoolValue.cpp \
  ARMExpandPseudoInsts.cpp \
  ARMFastISel.cpp \
  ARMFrameLowering.cpp \
  ARMHazardRecognizer.cpp \
  ARMISelDAGToDAG.cpp \
  ARMISelLowering.cpp \
  ARMInstrInfo.cpp \
  ARMJITInfo.cpp \
  ARMLoadStoreOptimizer.cpp \
  ARMMCInstLower.cpp \
  ARMMachineFunctionInfo.cpp \
  ARMRegisterInfo.cpp \
  ARMSelectionDAGInfo.cpp \
  ARMSubtarget.cpp \
  ARMTargetMachine.cpp \
  ARMTargetObjectFile.cpp \
  ARMTargetTransformInfo.cpp \
  MLxExpansionPass.cpp \
  Thumb1FrameLowering.cpp \
  Thumb1InstrInfo.cpp \
  Thumb1RegisterInfo.cpp \
  Thumb2ITBlockPass.cpp \
  Thumb2InstrInfo.cpp \
  Thumb2RegisterInfo.cpp \
  Thumb2SizeReduction.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMARMCodeGen
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(arm_codegen_SRC_FILES)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/MCTargetDesc
TBLGEN_TABLES := $(arm_codegen_TBLGEN_TABLES)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device only
# =====================================================
ifeq ($(TARGET_ARCH),arm)
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMARMCodeGen
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(arm_codegen_SRC_FILES)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/MCTargetDesc
TBLGEN_TABLES := $(arm_codegen_TBLGEN_TABLES)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
endif

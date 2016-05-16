LOCAL_PATH := $(call my-dir)

mips_codegen_TBLGEN_TABLES := \
  MipsGenRegisterInfo.inc \
  MipsGenInstrInfo.inc \
  MipsGenCodeEmitter.inc \
  MipsGenMCCodeEmitter.inc \
  MipsGenMCPseudoLowering.inc \
  MipsGenAsmWriter.inc \
  MipsGenDAGISel.inc \
  MipsGenCallingConv.inc \
  MipsGenSubtargetInfo.inc

mips_codegen_SRC_FILES := \
  Mips16FrameLowering.cpp \
  Mips16HardFloat.cpp \
  Mips16ISelDAGToDAG.cpp \
  Mips16ISelLowering.cpp \
  Mips16InstrInfo.cpp \
  Mips16RegisterInfo.cpp \
  MipsAnalyzeImmediate.cpp \
  MipsAsmPrinter.cpp \
  MipsCodeEmitter.cpp \
  MipsConstantIslandPass.cpp \
  MipsDelaySlotFiller.cpp \
  MipsFrameLowering.cpp \
  MipsInstrInfo.cpp \
  MipsISelDAGToDAG.cpp \
  MipsISelLowering.cpp \
  MipsJITInfo.cpp \
  MipsLongBranch.cpp \
  MipsMachineFunction.cpp \
  MipsMCInstLower.cpp \
  MipsModuleISelDAGToDAG.cpp \
  MipsOptimizeMathLibCalls.cpp \
  MipsOs16.cpp \
  MipsRegisterInfo.cpp \
  MipsSEFrameLowering.cpp \
  MipsSEISelDAGToDAG.cpp \
  MipsSEISelLowering.cpp \
  MipsSEInstrInfo.cpp \
  MipsSERegisterInfo.cpp \
  MipsSelectionDAGInfo.cpp \
  MipsSubtarget.cpp \
  MipsTargetMachine.cpp \
  MipsTargetObjectFile.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsCodeGen
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(mips_codegen_SRC_FILES)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/MCTargetDesc

TBLGEN_TABLES := $(mips_codegen_TBLGEN_TABLES)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device only
# =====================================================
ifeq ($(TARGET_ARCH),mips)
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsCodeGen
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(mips_codegen_SRC_FILES)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/MCTargetDesc

TBLGEN_TABLES := $(mips_codegen_TBLGEN_TABLES)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
endif

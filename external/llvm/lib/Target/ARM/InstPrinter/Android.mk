LOCAL_PATH := $(call my-dir)

arm_asm_printer_TBLGEN_TABLES := \
  ARMGenAsmWriter.inc \
  ARMGenRegisterInfo.inc \
  ARMGenSubtargetInfo.inc \
  ARMGenInstrInfo.inc

arm_asm_printer_SRC_FILES := \
  ARMInstPrinter.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := $(arm_asm_printer_TBLGEN_TABLES)

TBLGEN_TD_DIR := $(LOCAL_PATH)/..

LOCAL_SRC_FILES := $(arm_asm_printer_SRC_FILES)

LOCAL_MODULE:= libLLVMARMAsmPrinter

LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/..

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := $(arm_asm_printer_TBLGEN_TABLES)

TBLGEN_TD_DIR := $(LOCAL_PATH)/..

LOCAL_SRC_FILES := $(arm_asm_printer_SRC_FILES)

LOCAL_C_INCLUDES+= \
    $(LOCAL_PATH)/..

LOCAL_MODULE:= libLLVMARMAsmPrinter

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_STATIC_LIBRARY)


LOCAL_PATH := $(call my-dir)

mips_asm_printer_TBLGEN_TABLES := \
  MipsGenAsmWriter.inc \
  MipsGenRegisterInfo.inc \
  MipsGenSubtargetInfo.inc \
  MipsGenInstrInfo.inc

mips_asm_printer_SRC_FILES := \
  MipsInstPrinter.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsAsmPrinter
LOCAL_MODULE_TAGS := optional

TBLGEN_TABLES := $(mips_asm_printer_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

LOCAL_SRC_FILES := $(mips_asm_printer_SRC_FILES)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/..

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsAsmPrinter
LOCAL_MODULE_TAGS := optional

TBLGEN_TABLES := $(mips_asm_printer_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

LOCAL_SRC_FILES := $(mips_asm_printer_SRC_FILES)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/..

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_STATIC_LIBRARY)

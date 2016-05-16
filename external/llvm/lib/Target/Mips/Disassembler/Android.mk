LOCAL_PATH := $(call my-dir)

mips_disassembler_TBLGEN_TABLES := \
  MipsGenDisassemblerTables.inc \
  MipsGenInstrInfo.inc \
  MipsGenRegisterInfo.inc \
  MipsGenSubtargetInfo.inc

mips_disassembler_SRC_FILES := \
  MipsDisassembler.cpp

# For the device
# =====================================================
ifeq ($(TARGET_ARCH),mips)
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsDisassembler
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(mips_disassembler_SRC_FILES)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/..

TBLGEN_TABLES := $(mips_disassembler_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_STATIC_LIBRARY)
endif

# For the host
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsDisassembler
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(mips_disassembler_SRC_FILES)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/..

TBLGEN_TABLES := $(mips_disassembler_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

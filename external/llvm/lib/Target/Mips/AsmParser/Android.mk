LOCAL_PATH := $(call my-dir)

#===---------------------------------------------------------------===
# libLLVMMipsAsmParser (common)
#===---------------------------------------------------------------===

mips_asm_parser_SRC_FILES := \
  MipsAsmParser.cpp

mips_asm_parser_C_INCLUDES := $(LOCAL_PATH)/..

mips_asm_parser_TBLGEN_TABLES := \
  MipsGenAsmMatcher.inc \
  MipsGenInstrInfo.inc \
  MipsGenRegisterInfo.inc \
  MipsGenSubtargetInfo.inc

mips_asm_parser_TBLGEN_TD_DIR := $(LOCAL_PATH)/..


#===---------------------------------------------------------------===
# libLLVMMipsAsmParser (host)
#===---------------------------------------------------------------===
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsAsmParser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(mips_asm_parser_SRC_FILES)
LOCAL_C_INCLUDES += $(mips_asm_parser_C_INCLUDES)
TBLGEN_TABLES := $(mips_asm_parser_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)


#===---------------------------------------------------------------===
# libLLVMMipsAsmParser (target)
#===---------------------------------------------------------------===
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsAsmParser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(mips_asm_parser_SRC_FILES)
LOCAL_C_INCLUDES += $(mips_asm_parser_C_INCLUDES)
TBLGEN_TABLES := $(mips_asm_parser_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_STATIC_LIBRARY)

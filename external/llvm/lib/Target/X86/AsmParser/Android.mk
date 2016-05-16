LOCAL_PATH := $(call my-dir)

# For the device only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

x86_asm_parser_SRC_FILES :=	\
	X86AsmParser.cpp

x86_asm_parser_TBLGEN_TABLES :=	\
	X86GenAsmMatcher.inc	\
	X86GenInstrInfo.inc	\
	X86GenRegisterInfo.inc \
	X86GenSubtargetInfo.inc

x86_asm_parser_TBLGEN_TD_DIR := $(LOCAL_PATH)/..

x86_asm_parser_C_INCLUDES +=	\
	$(LOCAL_PATH)/..


#===---------------------------------------------------------------===
# libX86AsmParser (host)
#===---------------------------------------------------------------===
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMX86AsmParser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(x86_asm_parser_SRC_FILES)
LOCAL_C_INCLUDES += $(x86_asm_parser_C_INCLUDES)
TBLGEN_TABLES := $(x86_asm_parser_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(x86_asm_parser_TBLGEN_TD_DIR)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)


#===---------------------------------------------------------------===
# libX86AsmParser (target)
#===---------------------------------------------------------------===
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMX86AsmParser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(x86_asm_parser_SRC_FILES)
LOCAL_C_INCLUDES += $(x86_asm_parser_C_INCLUDES)
TBLGEN_TABLES := $(x86_asm_parser_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(x86_asm_parser_TBLGEN_TD_DIR)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_STATIC_LIBRARY)

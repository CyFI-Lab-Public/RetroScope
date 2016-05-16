LOCAL_PATH := $(call my-dir)

#===---------------------------------------------------------------===
# libARMAsmParser (common)
#===---------------------------------------------------------------===

arm_asm_parser_SRC_FILES := \
  ARMAsmParser.cpp

arm_asm_parser_TBLGEN_TABLES := \
  ARMGenInstrInfo.inc \
  ARMGenRegisterInfo.inc \
  ARMGenAsmMatcher.inc \
  ARMGenSubtargetInfo.inc

arm_asm_parser_TBLGEN_TD_DIR := $(LOCAL_PATH)/..

arm_asm_parser_C_INCLUDES := $(LOCAL_PATH)/..


#===---------------------------------------------------------------===
# libARMAsmParser (host)
#===---------------------------------------------------------------===
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMARMAsmParser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(arm_asm_parser_SRC_FILES)
LOCAL_C_INCLUDES += $(arm_asm_parser_C_INCLUDES)
TBLGEN_TABLES := $(arm_asm_parser_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(arm_asm_parser_TBLGEN_TD_DIR)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
ifneq (,$(filter windows darwin,$(HOST_OS)))
# Override the default optimization level to work around taking forever (~50m)
# to compile ARMAsmParser.cpp on Mac with gcc 4.2,
# or on Linux with mingw32msvc-gcc 4.2, which is used to cross-compile
# the win_sdk.
LOCAL_CFLAGS += -O0
endif
include $(BUILD_HOST_STATIC_LIBRARY)


#===---------------------------------------------------------------===
# libARMAsmParser (target)
#===---------------------------------------------------------------===
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMARMAsmParser
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(arm_asm_parser_SRC_FILES)
LOCAL_C_INCLUDES += $(arm_asm_parser_C_INCLUDES)
TBLGEN_TABLES := $(arm_asm_parser_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(arm_asm_parser_TBLGEN_TD_DIR)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_STATIC_LIBRARY)

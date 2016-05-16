LOCAL_PATH:= $(call my-dir)

asm_parser_SRC_FILES := \
  LLLexer.cpp \
  LLParser.cpp \
  Parser.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(asm_parser_SRC_FILES)

LOCAL_MODULE:= libLLVMAsmParser

LOCAL_MODULE_TAGS := optional

include $(LOCAL_PATH)/../../llvm-host-build.mk
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(asm_parser_SRC_FILES)

LOCAL_MODULE:= libLLVMAsmParser

LOCAL_MODULE_TAGS := optional

include $(LOCAL_PATH)/../../llvm-device-build.mk
include $(BUILD_STATIC_LIBRARY)

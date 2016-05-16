LOCAL_PATH:= $(call my-dir)

mc_parser_SRC_FILES := \
  AsmLexer.cpp \
  AsmParser.cpp \
  COFFAsmParser.cpp \
  DarwinAsmParser.cpp \
  ELFAsmParser.cpp \
  MCAsmLexer.cpp \
  MCAsmParser.cpp \
  MCAsmParserExtension.cpp \
  MCTargetAsmParser.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mc_parser_SRC_FILES)

LOCAL_MODULE:= libLLVMMCParser

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mc_parser_SRC_FILES)

LOCAL_MODULE:= libLLVMMCParser

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

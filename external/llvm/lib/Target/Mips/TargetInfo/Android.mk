LOCAL_PATH := $(call my-dir)

mips_target_info_TBLGEN_TABLES := \
  MipsGenInstrInfo.inc \
  MipsGenRegisterInfo.inc \
  MipsGenSubtargetInfo.inc

mips_target_info_SRC_FILES := \
  MipsTargetInfo.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsInfo
LOCAL_MODULE_TAGS := optional

TBLGEN_TABLES := $(mips_target_info_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

LOCAL_SRC_FILES := $(mips_target_info_SRC_FILES)
LOCAL_C_INCLUDES +=	$(LOCAL_PATH)/..

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
ifeq ($(TARGET_ARCH),mips)
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMMipsInfo
LOCAL_MODULE_TAGS := optional

TBLGEN_TABLES := $(mips_target_info_TBLGEN_TABLES)
TBLGEN_TD_DIR := $(LOCAL_PATH)/..

LOCAL_SRC_FILES := $(mips_target_info_SRC_FILES)
LOCAL_C_INCLUDES +=	$(LOCAL_PATH)/..

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(BUILD_STATIC_LIBRARY)
endif

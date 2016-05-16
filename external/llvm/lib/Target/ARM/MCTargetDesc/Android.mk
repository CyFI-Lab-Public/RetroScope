LOCAL_PATH := $(call my-dir)

arm_mc_desc_TBLGEN_TABLES := \
  ARMGenRegisterInfo.inc \
  ARMGenInstrInfo.inc \
  ARMGenMCCodeEmitter.inc \
  ARMGenSubtargetInfo.inc

arm_mc_desc_SRC_FILES := \
  ARMAsmBackend.cpp \
  ARMELFObjectWriter.cpp \
  ARMELFStreamer.cpp \
  ARMMCAsmInfo.cpp \
  ARMMCCodeEmitter.cpp \
  ARMMCExpr.cpp \
  ARMMCTargetDesc.cpp \
  ARMMachObjectWriter.cpp \
  ARMMachORelocationInfo.cpp \
  ARMUnwindOpAsm.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)


LOCAL_MODULE:= libLLVMARMDesc
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(arm_mc_desc_SRC_FILES)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/..

TBLGEN_TD_DIR := $(LOCAL_PATH)/..
TBLGEN_TABLES := $(arm_mc_desc_TBLGEN_TABLES)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device only
# =====================================================
ifeq ($(TARGET_ARCH),arm)
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE:= libLLVMARMDesc
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(arm_mc_desc_SRC_FILES)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/..

TBLGEN_TD_DIR := $(LOCAL_PATH)/..
TBLGEN_TABLES := $(arm_mc_desc_TBLGEN_TABLES)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_TBLGEN_RULES_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
endif

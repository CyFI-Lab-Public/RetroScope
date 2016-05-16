LOCAL_PATH:= $(call my-dir)

mcld_mips_target_SRC_FILES := \
  MipsDiagnostic.cpp  \
  MipsELFDynamic.cpp  \
  MipsELFMCLinker.cpp  \
  MipsEmulation.cpp \
  MipsGOT.cpp \
  MipsLDBackend.cpp \
  MipsMCLinker.cpp \
  MipsRelocator.cpp \
  MipsTargetMachine.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_mips_target_SRC_FILES)
LOCAL_MODULE:= libmcldMipsTarget

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
ifeq ($(TARGET_ARCH),mips)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_mips_target_SRC_FILES)
LOCAL_MODULE:= libmcldMipsTarget

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

endif

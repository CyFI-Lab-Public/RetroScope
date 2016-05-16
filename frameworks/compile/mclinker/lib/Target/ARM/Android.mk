LOCAL_PATH:= $(call my-dir)

mcld_arm_target_SRC_FILES := \
  ARMDiagnostic.cpp \
  ARMELFDynamic.cpp \
  ARMELFMCLinker.cpp \
  ARMEmulation.cpp \
  ARMGOT.cpp  \
  ARMLDBackend.cpp  \
  ARMMCLinker.cpp  \
  ARMPLT.cpp  \
  ARMRelocator.cpp  \
  ARMTargetMachine.cpp \
  ARMToARMStub.cpp \
  ARMToTHMStub.cpp \
  THMToARMStub.cpp \
  THMToTHMStub.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_arm_target_SRC_FILES)
LOCAL_MODULE:= libmcldARMTarget

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
ifeq ($(TARGET_ARCH),arm)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_arm_target_SRC_FILES)
LOCAL_MODULE:= libmcldARMTarget

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

endif

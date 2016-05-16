LOCAL_PATH:= $(call my-dir)

mcld_x86_target_SRC_FILES := \
  X86Diagnostic.cpp \
  X86ELFDynamic.cpp \
  X86ELFMCLinker.cpp \
  X86Emulation.cpp  \
  X86GOT.cpp  \
  X86GOTPLT.cpp  \
  X86LDBackend.cpp  \
  X86MCLinker.cpp  \
  X86PLT.cpp  \
  X86Relocator.cpp  \
  X86TargetMachine.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_x86_target_SRC_FILES)
LOCAL_MODULE:= libmcldX86Target

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
ifeq ($(TARGET_ARCH),x86)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_x86_target_SRC_FILES)
LOCAL_MODULE:= libmcldX86Target

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)

endif

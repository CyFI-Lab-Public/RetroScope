LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DiagnosticDriverKinds.inc \
  Options.inc \
  CC1Options.inc \
  CC1AsOptions.inc

clang_driver_SRC_FILES := \
  Action.cpp \
  CC1AsOptions.cpp \
  Compilation.cpp \
  Driver.cpp \
  DriverOptions.cpp \
  Job.cpp \
  Phases.cpp \
  Tool.cpp \
  ToolChain.cpp \
  ToolChains.cpp \
  Tools.cpp \
  Types.cpp \
  WindowsToolChain.cpp

LOCAL_SRC_FILES := $(clang_driver_SRC_FILES)

LOCAL_MODULE := libclangDriver
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(CLANG_VERSION_INC_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

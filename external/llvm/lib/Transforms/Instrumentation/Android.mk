LOCAL_PATH:= $(call my-dir)

instrumentation_SRC_FILES := \
  AddressSanitizer.cpp \
  BoundsChecking.cpp \
  EdgeProfiling.cpp \
  GCOVProfiling.cpp \
  Instrumentation.cpp \
  MemorySanitizer.cpp \
  OptimalEdgeProfiling.cpp \
  PathProfiling.cpp \
  ProfilingUtils.cpp \
  ThreadSanitizer.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMInstrumentation
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(instrumentation_SRC_FILES)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the target
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMInstrumentation
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(instrumentation_SRC_FILES)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)

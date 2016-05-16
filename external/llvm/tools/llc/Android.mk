LOCAL_PATH := $(call my-dir)

LLVM_ROOT_PATH := $(LOCAL_PATH)/../..


#===---------------------------------------------------------------===
# llc command line tool (common)
#===---------------------------------------------------------------===

llvm_llc_SRC_FILES := \
  llc.cpp

llvm_llc_mips_STATIC_LIBRARIES := \
  libLLVMMipsAsmParser \
  libLLVMMipsInfo \
  libLLVMMipsCodeGen \
  libLLVMMipsDesc \
  libLLVMMipsAsmPrinter \

llvm_llc_x86_STATIC_LIBRARIES := \
  libLLVMX86Info \
  libLLVMX86AsmParser \
  libLLVMX86CodeGen \
  libLLVMX86Disassembler \
  libLLVMX86Desc \
  libLLVMX86AsmPrinter \
  libLLVMX86Utils

llvm_llc_arm_STATIC_LIBRARIES := \
  libLLVMARMCodeGen \
  libLLVMARMDisassembler \
  libLLVMARMAsmParser \
  libLLVMARMAsmPrinter \
  libLLVMARMDesc \
  libLLVMARMInfo

llvm_llc_STATIC_LIBRARIES := \
  libLLVMAsmPrinter \
  libLLVMIRReader \
  libLLVMAsmParser \
  libLLVMBitReader \
  libLLVMBitWriter \
  libLLVMSelectionDAG \
  libLLVMInstCombine \
  libLLVMInstrumentation \
  libLLVMCodeGen \
  libLLVMObject \
  libLLVMipo \
  libLLVMipa \
  libLLVMLinker \
  libLLVMMC \
  libLLVMMCParser \
  libLLVMScalarOpts \
  libLLVMTransformUtils \
  libLLVMVectorize \
  libLLVMAnalysis \
  libLLVMTarget \
  libLLVMCore \
  libLLVMSupport


#===---------------------------------------------------------------===
# llc command line tool (host)
#===---------------------------------------------------------------===

include $(CLEAR_VARS)

LOCAL_MODULE := llc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_IS_HOST_MODULE := true

LOCAL_SRC_FILES := $(llvm_llc_SRC_FILES)

LOCAL_STATIC_LIBRARIES := \
  $(llvm_llc_arm_STATIC_LIBRARIES) \
  $(llvm_llc_mips_STATIC_LIBRARIES) \
  $(llvm_llc_x86_STATIC_LIBRARIES) \
  $(llvm_llc_STATIC_LIBRARIES)

LOCAL_LDLIBS += -lpthread -lm -ldl
LOCAL_C_INCLUDES += external/llvm/include

include $(LLVM_ROOT_PATH)/llvm.mk
include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_EXECUTABLE)


#===---------------------------------------------------------------===
# llc command line tool (target)
#===---------------------------------------------------------------===

include $(CLEAR_VARS)

LOCAL_MODULE := llc
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := EXECUTABLES

LOCAL_SRC_FILES := $(llvm_llc_SRC_FILES)
LOCAL_C_INCLUDES += external/llvm/include

ifeq ($(TARGET_ARCH),arm)
  LOCAL_STATIC_LIBRARIES := $(llvm_llc_arm_STATIC_LIBRARIES)
else
  ifeq ($(TARGET_ARCH),mips)
    LOCAL_STATIC_LIBRARIES := $(llvm_llc_mips_STATIC_LIBRARIES)
  else
    ifeq ($(TARGET_ARCH),x86)
      LOCAL_STATIC_LIBRARIES := $(llvm_llc_x86_STATIC_LIBRARIES)
    else
      $(error "Unsupport llc target $(TARGET_ARCH)")
    endif
  endif
endif

LOCAL_STATIC_LIBRARIES += $(llvm_llc_STATIC_LIBRARIES)

LOCAL_SHARED_LIBRARIES :=  \
  libcutils  \
  libdl  \
  libstlport

include $(LLVM_ROOT_PATH)/llvm.mk
include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_EXECUTABLE)

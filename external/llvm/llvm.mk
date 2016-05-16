ifeq ($(LLVM_ROOT_PATH),)
$(error Must set variable LLVM_ROOT_PATH before including this! $(LOCAL_PATH))
endif

TBLGEN := $(BUILD_OUT_EXECUTABLES)/tblgen$(BUILD_EXECUTABLE_SUFFIX)

CLEAR_TBLGEN_VARS := $(LLVM_ROOT_PATH)/clear_tblgen_vars.mk
LLVM_HOST_BUILD_MK := $(LLVM_ROOT_PATH)/llvm-host-build.mk
LLVM_DEVICE_BUILD_MK := $(LLVM_ROOT_PATH)/llvm-device-build.mk
LLVM_GEN_INTRINSICS_MK := $(LLVM_ROOT_PATH)/llvm-gen-intrinsics.mk
LLVM_TBLGEN_RULES_MK := $(LLVM_ROOT_PATH)/llvm-tblgen-rules.mk

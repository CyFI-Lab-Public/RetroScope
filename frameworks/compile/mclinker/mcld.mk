ifeq ($(MCLD_ROOT_PATH),)
$(error Must set variable MCLD_ROOT_PATH before including this! $(LOCAL_PATH))
endif

MCLD_HOST_BUILD_MK := $(MCLD_ROOT_PATH)/mcld-host-build.mk
MCLD_DEVICE_BUILD_MK := $(MCLD_ROOT_PATH)/mcld-device-build.mk

ifeq ($(LLVM_ROOT_PATH),)
$(error Must set variable LLVM_ROOT_PATH before including this! $(LOCAL_PATH))
endif

include $(LLVM_ROOT_PATH)/llvm.mk

ifeq ($(CLANG_ROOT_PATH),)
$(error Must set variable CLANG_ROOT_PATH before including this! $(LOCAL_PATH))
endif

CLANG_TBLGEN := $(BUILD_OUT_EXECUTABLES)/clang-tblgen$(BUILD_EXECUTABLE_SUFFIX)

CLANG_HOST_BUILD_MK := $(CLANG_ROOT_PATH)/clang-host-build.mk
CLANG_TBLGEN_RULES_MK := $(CLANG_ROOT_PATH)/clang-tblgen-rules.mk
CLANG_VERSION_INC_MK := $(CLANG_ROOT_PATH)/clang-version-inc.mk

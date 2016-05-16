###########################################################
## Generate clang/Basic/Version.inc
###########################################################
ifeq ($(LOCAL_MODULE_CLASS),)
    LOCAL_MODULE_CLASS := STATIC_LIBRARIES
endif

intermediates := $(call local-intermediates-dir)

LLVMVersion := $(shell grep PACKAGE_VERSION $(LLVM_ROOT_PATH)/host/include/llvm/Config/config.h | sed -e 's/\#define PACKAGE_VERSION "\(.*\)"/\1/g')

# Compute the Clang version from the LLVM version, unless specified explicitly.
# (Copy from include/clang/Basic/Makefile)
CLANG_VERSION := $(subst svn,,$(LLVMVersion))
CLANG_VERSION_COMPONENTS := $(subst ., ,$(CLANG_VERSION))
CLANG_VERSION_MAJOR := $(word 1,$(CLANG_VERSION_COMPONENTS))
CLANG_VERSION_MINOR := $(word 2,$(CLANG_VERSION_COMPONENTS))
CLANG_VERSION_PATCHLEVEL := $(word 3,$(CLANG_VERSION_COMPONENTS))
ifeq ($(CLANG_VERSION_PATCHLEVEL),)
    CLANG_HAS_VERSION_PATCHLEVEL := 0
else
    CLANG_HAS_VERSION_PATCHLEVEL := 1
endif

LOCAL_GENERATED_SOURCES += $(intermediates)/include/clang/Basic/Version.inc
$(intermediates)/include/clang/Basic/Version.inc: $(CLANG_ROOT_PATH)/include/clang/Basic/Version.inc.in
	@echo "Updating Clang version info."
	@mkdir -p $(dir $@)
	$(hide) sed -e "s#@CLANG_VERSION@#$(CLANG_VERSION)#g" \
	-e "s#@CLANG_VERSION_MAJOR@#$(CLANG_VERSION_MAJOR)#g" \
	-e "s#@CLANG_VERSION_MINOR@#$(CLANG_VERSION_MINOR)#g" \
	-e "s#@CLANG_VERSION_PATCHLEVEL@#$(CLANG_VERSION_PATCHLEVEL)#g" \
	-e "s#@CLANG_HAS_VERSION_PATCHLEVEL@#$(CLANG_HAS_VERSION_PATCHLEVEL)#g" \
	$< > $@


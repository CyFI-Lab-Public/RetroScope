# -*- mode: makefile -*-
# Copyright (C) 2007 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Definitions for building the Java library and associated tests.
#

#
# Common definitions for host and target.
#

# libcore is divided into modules.
#
# The structure of each module is:
#
#   src/
#       main/               # To be shipped on every device.
#            java/          # Java source for library code.
#            native/        # C++ source for library code.
#            resources/     # Support files.
#       test/               # Built only on demand, for testing.
#            java/          # Java source for tests.
#            native/        # C++ source for tests (rare).
#            resources/     # Support files.
#
# All subdirectories are optional (hence the "2> /dev/null"s below).

define all-main-java-files-under
$(foreach dir,$(1),$(patsubst ./%,%,$(shell cd $(LOCAL_PATH) && find $(dir)/src/main/java -name "*.java" 2> /dev/null)))
endef

define all-test-java-files-under
$(foreach dir,$(1),$(patsubst ./%,%,$(shell cd $(LOCAL_PATH) && find $(dir)/src/test/java -name "*.java" 2> /dev/null)))
endef

define all-core-resource-dirs
$(shell cd $(LOCAL_PATH) && ls -d */src/$(1)/{java,resources} 2> /dev/null)
endef

# The Java files and their associated resources.
common_core_src_files := $(call all-main-java-files-under,dalvik dex dom json luni xml)
core_resource_dirs := $(call all-core-resource-dirs,main)
test_resource_dirs := $(call all-core-resource-dirs,test)

ifeq ($(EMMA_INSTRUMENT),true)
ifneq ($(EMMA_INSTRUMENT_STATIC),true)
    common_core_src_files += $(call all-java-files-under, ../external/emma/core ../external/emma/pregenerated)
    core_resource_dirs += ../external/emma/core/res ../external/emma/pregenerated/res
endif
endif

libdvm_core_src_files += $(common_core_src_files) $(call all-main-java-files-under,libdvm)
libart_core_src_files += $(common_core_src_files) $(call all-main-java-files-under,libart)

local_javac_flags=-encoding UTF-8
#local_javac_flags+=-Xlint:all -Xlint:-serial,-deprecation,-unchecked
local_javac_flags+=-Xmaxwarns 9999999

#
# Build for the target (device).
#

# Definitions to make the core library.

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(libdvm_core_src_files)
LOCAL_JAVA_RESOURCE_DIRS := $(core_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_DX_FLAGS := --core-library
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := core
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
LOCAL_REQUIRED_MODULES := tzdata
include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(libart_core_src_files)
LOCAL_JAVA_RESOURCE_DIRS := $(core_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_DX_FLAGS := --core-library
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := core-libart
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
LOCAL_REQUIRED_MODULES := tzdata
include $(BUILD_JAVA_LIBRARY)

# Create the conscrypt library
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-main-java-files-under,crypto)
LOCAL_JAVA_LIBRARIES := core
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_JARJAR_RULES := $(LOCAL_PATH)/crypto/jarjar-rules.txt
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := conscrypt
LOCAL_REQUIRED_MODULES := libjavacrypto
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_JAVA_LIBRARY)

# Create the conscrypt library without jarjar for tests
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-main-java-files-under,crypto)
LOCAL_JAVA_LIBRARIES := core
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := conscrypt-nojarjar
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_STATIC_JAVA_LIBRARY)


ifeq ($(LIBCORE_SKIP_TESTS),)
# Make the core-tests library.
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-test-java-files-under,dalvik dom harmony-tests json luni xml)
LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := bouncycastle core core-junit okhttp
LOCAL_STATIC_JAVA_LIBRARIES := core-tests-support sqlite-jdbc mockwebserver nist-pkix-tests okhttp-tests
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE := core-tests
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_STATIC_JAVA_LIBRARY)
endif

ifeq ($(LIBCORE_SKIP_TESTS),)
# Make the core-tests-support library.
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-test-java-files-under,support)
LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := bouncycastle core core-junit
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE := core-tests-support
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_STATIC_JAVA_LIBRARY)
endif

ifeq ($(LIBCORE_SKIP_TESTS),)
# Make the conscrypt-tests library.
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-test-java-files-under,crypto)
LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := bouncycastle core core-junit
LOCAL_STATIC_JAVA_LIBRARIES := core-tests-support conscrypt-nojarjar
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := conscrypt-tests
LOCAL_REQUIRED_MODULES := libjavacrypto
LOCAL_JARJAR_RULES := $(LOCAL_PATH)/crypto/jarjar-rules.txt
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_STATIC_JAVA_LIBRARY)
endif

# This one's tricky. One of our tests needs to have a
# resource with a "#" in its name, but Perforce doesn't
# allow us to submit such a file. So we create it here
# on-the-fly.
TMP_RESOURCE_DIR := $(intermediates.COMMON)/tmp/
TMP_RESOURCE_FILE := org/apache/harmony/luni/tests/java/lang/test\#.properties

$(TMP_RESOURCE_DIR)$(TMP_RESOURCE_FILE):
	@mkdir -p $(dir $@)
	@echo "Hello, world!" > $@

$(LOCAL_INTERMEDIATE_TARGETS): PRIVATE_EXTRA_JAR_ARGS := $(extra_jar_args) -C "$(TMP_RESOURCE_DIR)" "$(TMP_RESOURCE_FILE)"
$(LOCAL_INTERMEDIATE_TARGETS): $(TMP_RESOURCE_DIR)$(TMP_RESOURCE_FILE)


#
# Build for the host.
#

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-main-java-files-under, dex)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := dex-host
include $(BUILD_HOST_JAVA_LIBRARY)

ifeq ($(WITH_HOST_DALVIK),true)

    # Definitions to make the core library.
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(libdvm_core_src_files)
    LOCAL_JAVA_RESOURCE_DIRS := $(core_resource_dirs)
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_DX_FLAGS := --core-library
    LOCAL_BUILD_HOST_DEX := true
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := core-hostdex
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    LOCAL_REQUIRED_MODULES := tzdata-host
    include $(BUILD_HOST_JAVA_LIBRARY)

    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(libart_core_src_files)
    LOCAL_JAVA_RESOURCE_DIRS := $(core_resource_dirs)
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_DX_FLAGS := --core-library
    LOCAL_BUILD_HOST_DEX := true
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := core-libart-hostdex
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    LOCAL_REQUIRED_MODULES := tzdata-host
    include $(BUILD_HOST_JAVA_LIBRARY)

    # Make the conscrypt-hostdex library
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(call all-main-java-files-under,crypto)
    LOCAL_JAVA_LIBRARIES := core-hostdex
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_JARJAR_RULES := $(LOCAL_PATH)/crypto/jarjar-rules.txt
    LOCAL_BUILD_HOST_DEX := true
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := conscrypt-hostdex
    LOCAL_REQUIRED_MODULES := libjavacrypto
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    include $(BUILD_HOST_JAVA_LIBRARY)

    # Make the conscrypt-hostdex-nojarjar for tests
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(call all-main-java-files-under,crypto)
    LOCAL_JAVA_LIBRARIES := core-hostdex
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_BUILD_HOST_DEX := true
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := conscrypt-hostdex-nojarjar
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    include $(BUILD_HOST_JAVA_LIBRARY)

    # Make the core-tests library.
    ifeq ($(LIBCORE_SKIP_TESTS),)
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(call all-test-java-files-under,dalvik dom json luni xml)
    LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVA_LIBRARIES := bouncycastle-hostdex core-hostdex core-junit-hostdex core-tests-support-hostdex okhttp-hostdex
    LOCAL_STATIC_JAVA_LIBRARIES := sqlite-jdbc-host mockwebserver-host nist-pkix-tests-host
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := core-tests-hostdex
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    LOCAL_BUILD_HOST_DEX := true
    include $(BUILD_HOST_JAVA_LIBRARY)
    endif

    # Make the core-tests-support library.
    ifeq ($(LIBCORE_SKIP_TESTS),)
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(call all-test-java-files-under,support)
    LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVA_LIBRARIES := bouncycastle-hostdex core-hostdex core-junit-hostdex
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := core-tests-support-hostdex
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    LOCAL_BUILD_HOST_DEX := true
    include $(BUILD_HOST_JAVA_LIBRARY)
    endif

    # Make the conscrypt-tests library.
    ifeq ($(LIBCORE_SKIP_TESTS),)
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(call all-test-java-files-under,crypto)
    LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVA_LIBRARIES := bouncycastle-hostdex core-hostdex core-junit-hostdex core-tests-support-hostdex conscrypt-hostdex-nojarjar
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := conscrypt-tests-hostdex
    LOCAL_REQUIRED_MODULES := libjavacrypto
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    LOCAL_BUILD_HOST_DEX := true
    include $(BUILD_HOST_JAVA_LIBRARY)
    endif
endif

#
# Local droiddoc for faster libcore testing
#
#
# Run with:
#     mm -j32 libcore-docs
#
# Main output:
#     ../out/target/common/docs/libcore/reference/packages.html
#
# All text for proofreading (or running tools over):
#     ../out/target/common/docs/libcore-proofread.txt
#
# TODO list of missing javadoc, etc:
#     ../out/target/common/docs/libcore-docs-todo.html
#
# Rerun:
#     rm -rf ../out/target/common/docs/libcore-timestamp && mm -j32 libcore-docs
#
include $(CLEAR_VARS)

# for shared defintion of libcore_to_document
include $(LOCAL_PATH)/Docs.mk

LOCAL_SRC_FILES:=$(call libcore_to_document,$(LOCAL_PATH))
# rerun doc generation without recompiling the java
LOCAL_JAVA_LIBRARIES:=
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE_CLASS:=JAVA_LIBRARIES

LOCAL_MODULE := libcore
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk

LOCAL_DROIDDOC_OPTIONS := \
 -offlinemode \
 -title "libcore" \
 -proofread $(OUT_DOCS)/$(LOCAL_MODULE)-proofread.txt \
 -todo ../$(LOCAL_MODULE)-docs-todo.html \
 -hdf android.whichdoc offline

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR:=build/tools/droiddoc/templates-sdk

include $(BUILD_DROIDDOC)

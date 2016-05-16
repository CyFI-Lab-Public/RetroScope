# Copyright (C) 2010 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

# build smali jar
# ============================================================

include $(CLEAR_VARS)

LOCAL_MODULE := smalilib

LOCAL_MODULE_TAGS := optional

#LOCAL_MODULE_CLASS and LOCAL_IS_HOST_MODULE must be defined before calling $(local-intermediates-dir)
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_IS_HOST_MODULE := true

intermediates := $(call local-intermediates-dir,COMMON)

GEN := $(addprefix $(intermediates)/, \
			smaliParser.java \
			smaliTreeWalker.java \
        )

ANTLR_JAR = $(call java-lib-deps,antlr,true)

$(intermediates)/smaliTreeWalker.java: $(intermediates)/smaliParser.java

$(GEN): $(ANTLR_JAR)
$(GEN): PRIVATE_PATH := $(LOCAL_PATH)
$(GEN): PRIVATE_CUSTOM_TOOL = java -jar $(ANTLR_JAR) -fo $(dir $@) $<
$(GEN): $(intermediates)/%.java :  $(LOCAL_PATH)/src/main/antlr3/%.g
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)

LOCAL_SRC_FILES := \
	$(call all-java-files-under, src/main/java) \
	$(call all-java-files-under, ../dexlib/src/main/java) \
	$(call all-java-files-under, ../util/src/main/java)

LOCAL_JAR_MANIFEST := manifest.txt

LOCAL_STATIC_JAVA_LIBRARIES := \
	antlr-runtime \
	commons-cli-1.2 \
	guavalib \
	jsr305lib

#read in the version number
SMALI_VERSION := $(shell cat $(LOCAL_PATH)/../version)

#create a new smali.properties file using the correct version
$(intermediates)/resources/smali.properties:
	$(hide) mkdir -p $(dir $@)
	$(hide) echo "application.version=$(SMALI_VERSION)" > $@

LOCAL_JAVA_RESOURCE_FILES := $(intermediates)/resources/smali.properties

include $(BUILD_HOST_JAVA_LIBRARY)



# copy smali script
# ============================================================

include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := smali

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): $(HOST_OUT_JAVA_LIBRARIES)/smalilib.jar
$(LOCAL_BUILT_MODULE): $(LOCAL_PATH)/../scripts/smali | $(ACP)
	@echo "Copy: $(PRIVATE_MODULE) ($@)"
	$(copy-file-to-new-target)
	$(hide) chmod 755 $@
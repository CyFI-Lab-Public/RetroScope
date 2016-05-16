#
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
#
LOCAL_PATH := $(call my-dir)
RS_SPEC_GEN := $(BUILD_OUT_EXECUTABLES)/rs-spec-gen$(BUILD_EXECUTABLE_SUFFIX)

define generate-rs-spec-inc
@mkdir -p $(dir $@)
@echo "Host RSSpecGen: $(LOCAL_MODULE) (gen-$(1)) <= $<"
$(hide) $(RS_SPEC_GEN) \
  -gen-$(strip $(1))  \
  > $@
endef

ifneq ($(strip $(TBLGEN_TABLES)),)

ifneq ($(findstring RSClangBuiltinEnums.inc,$(RS_SPEC_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/RSClangBuiltinEnums.inc
$(intermediates)/RSClangBuiltinEnums.inc: $(RS_SPEC_GEN)
	$(call generate-rs-spec-inc,clang-builtin-enums)
endif

ifneq ($(findstring RSDataTypeEnums.inc,$(RS_SPEC_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/RSDataTypeEnums.inc
$(intermediates)/RSDataTypeEnums.inc: $(RS_SPEC_GEN)
	$(call generate-rs-spec-inc,rs-data-type-enums)
endif

ifneq ($(findstring RSMatrixTypeEnums.inc,$(RS_SPEC_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/RSMatrixTypeEnums.inc
$(intermediates)/RSMatrixTypeEnums.inc: $(RS_SPEC_GEN)
	$(call generate-rs-spec-inc,rs-matrix-type-enums)
endif

ifneq ($(findstring RSObjectTypeEnums.inc,$(RS_SPEC_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/RSObjectTypeEnums.inc
$(intermediates)/RSObjectTypeEnums.inc: $(RS_SPEC_GEN)
	$(call generate-rs-spec-inc,rs-object-type-enums)
endif

ifneq ($(findstring RSDataElementEnums.inc,$(RS_SPEC_TABLES)),)
LOCAL_GENERATED_SOURCES += $(intermediates)/RSDataElementEnums.inc
$(intermediates)/RSDataElementEnums.inc: $(RS_SPEC_GEN)
	$(call generate-rs-spec-inc,rs-data-element-enums)
endif

endif

#
# Copyright (C) 2012 The Android Open Source Project
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

# NOTE: Following rules are extracted from base_rules.mk and binary.mk.
# We must ensure that they are synchronized.

LOCAL_IS_HOST_MODULE := $(strip $(LOCAL_IS_HOST_MODULE))
ifdef LOCAL_IS_HOST_MODULE
  ifneq ($(LOCAL_IS_HOST_MODULE),true)
    $(error $(LOCAL_PATH): LOCAL_IS_HOST_MODULE must be "true" or empty, not "$(LOCAL_IS_HOST_MODULE)")
  endif
  my_prefix:=HOST_
else
  my_prefix:=TARGET_
endif

so_suffix := $($(my_prefix)SHLIB_SUFFIX)
a_suffix := $($(my_prefix)STATIC_LIB_SUFFIX)

# Extract Depended Libraries
LOCAL_LIBBCC_LIB_DEPS := \
  $(foreach lib,$(LOCAL_STATIC_LIBRARIES), \
    $(call intermediates-dir-for, \
      STATIC_LIBRARIES,$(lib),$(LOCAL_IS_HOST_MODULE))/$(lib)$(a_suffix)) \
  $(foreach lib,$(LOCAL_WHOLE_STATIC_LIBRARIES), \
    $(call intermediates-dir-for, \
      STATIC_LIBRARIES,$(lib),$(LOCAL_IS_HOST_MODULE))/$(lib)$(a_suffix)) \
  $(addprefix $($(my_prefix)OUT_INTERMEDIATE_LIBRARIES)/, \
    $(addsuffix $(so_suffix), $(LOCAL_SHARED_LIBRARIES))) \

# Build Rules for Automatically Generated Build Information
GEN := $(local-intermediates-dir)/BuildInfo.cpp

gen_build_info := $(LOCAL_PATH)/tools/build/gen-build-info.py

$(GEN): PRIVATE_PATH := $(LOCAL_PATH)
$(GEN): PRIVATE_DEPS := $(LOCAL_LIBBCC_LIB_DEPS)
$(GEN): PRIVATE_CUSTOM_TOOL = $(gen_build_info) $(PRIVATE_PATH) \
                              $(PRIVATE_DEPS) > $@
$(GEN): $(gen_build_info) $(LOCAL_LIBBCC_LIB_DEPS) \
        $(wildcard $(LOCAL_PATH)/.git/COMMIT_EDITMSG)
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)

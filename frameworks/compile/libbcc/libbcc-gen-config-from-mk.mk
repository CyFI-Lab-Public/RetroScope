#
# Copyright (C) 2011-2012 The Android Open Source Project
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

# Build rules for extracting configuration from Android.mk
intermediates := $(call local-intermediates-dir)

GEN_CONFIG_FROM_MK := $(intermediates)/ConfigFromMk.h

$(GEN_CONFIG_FROM_MK): PRIVATE_PATH := $(LIBBCC_ROOT_PATH)
$(GEN_CONFIG_FROM_MK): PRIVATE_CUSTOM_TOOL = \
        $(PRIVATE_PATH)/tools/build/gen-config-from-mk.py < $< > $@
$(GEN_CONFIG_FROM_MK): $(LIBBCC_ROOT_PATH)/libbcc-config.mk \
        $(LIBBCC_ROOT_PATH)/tools/build/gen-config-from-mk.py
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN_CONFIG_FROM_MK)

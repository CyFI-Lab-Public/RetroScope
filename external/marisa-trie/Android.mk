# Copyright (C) 2008 The Android Open Source Project
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

include $(CLEAR_VARS)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/lib/marisa

LOCAL_CPP_EXTENSION := .cc

LOCAL_SRC_FILES := lib/marisa/base.cc \
	lib/marisa/intvector.cc \
	lib/marisa/progress.cc \
	lib/marisa/tail.cc \
	lib/marisa/trie.cc \
	lib/marisa/trie-search.cc \
	lib/marisa/bitvector.cc \
	lib/marisa/mapper.cc \
	lib/marisa/reader.cc \
	lib/marisa/trie-build.cc \
	lib/marisa/trie-c.cc \
	lib/marisa/writer.cc

LOCAL_MODULE := libmarisa-trie-gnustl-rtti
LOCAL_MODULE_TAGS := optional

LOCAL_NDK_STL_VARIANT := gnustl_static
LOCAL_SDK_VERSION := 14

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/v0_1_5/lib/marisa_alpha

LOCAL_CPP_EXTENSION := .cc

LOCAL_SRC_FILES := v0_1_5/lib/marisa_alpha/base.cc \
	v0_1_5/lib/marisa_alpha/bitvector.cc \
	v0_1_5/lib/marisa_alpha/intvector.cc \
	v0_1_5/lib/marisa_alpha/mapper.cc \
	v0_1_5/lib/marisa_alpha/progress.cc \
	v0_1_5/lib/marisa_alpha/reader.cc \
	v0_1_5/lib/marisa_alpha/tail.cc \
	v0_1_5/lib/marisa_alpha/trie-build.cc \
	v0_1_5/lib/marisa_alpha/trie.cc \
	v0_1_5/lib/marisa_alpha/trie-c.cc \
	v0_1_5/lib/marisa_alpha/trie-search.cc \
	v0_1_5/lib/marisa_alpha/writer.cc

LOCAL_MODULE := libmarisa_alpha-trie-gnustl-rtti
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS := -frtti -fexceptions
LOCAL_NDK_STL_VARIANT := gnustl_static
LOCAL_SDK_VERSION := 14

include $(BUILD_STATIC_LIBRARY)

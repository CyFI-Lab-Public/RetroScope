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

#
# Common definitions used for building RS header files.
#
# Prior to including this file, the following variables should be
# set for each variant:
#
#			LOCAL_MODULE -- set (as usual) to name the module being built
#			input_data_file -- the path of the prebuilt data file to use
#			slangdata_output_var_name -- name of the symbol that needs to be defined
#												 					 in the data file
#
SLANG_DATA := frameworks/compile/slang/slangdata.py

intermediates := $(call local-intermediates-dir)

asm_file := $(intermediates)/$(slangdata_output_var_name).S
LOCAL_GENERATED_SOURCES += $(asm_file)

$(asm_file): PRIVATE_OUTPUT_VAR_NAME = $(basename $(notdir $@))
$(asm_file): PRIVATE_CUSTOM_TOOL = python $(SLANG_DATA) $(PRIVATE_OUTPUT_VAR_NAME) < $< > $@
$(asm_file): $(SLANG_DATA)
$(asm_file): $(input_data_file)
	$(transform-generated-source)

LOCAL_CFLAGS  += -D_REENTRANT -DPIC -fPIC
LOCAL_CFLAGS  += -O3 -nodefaultlibs -nostdlib

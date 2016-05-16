## Copyright 2008, The Android Open Source Project
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##


LOCAL_PATH := $(call my-dir)

ifeq ($(TARGET_ARCH),x86)

include $(CLEAR_VARS)

############################
# First, build stage1

LOCAL_SRC_FILES := \
        stage1/stage1.S

LOCAL_CFLAGS := \
        -Wall -Wmissing-prototypes -Wunused -Wshadow \
        -Wpointer-arith -falign-jumps=1 -falign-loops=1 \
        -falign-functions=1 -Wundef
LOCAL_CFLAGS += -m32 -O2 -fno-builtin -nostdinc -fno-reorder-functions -fno-stack-protector

LOCAL_C_INCLUDES := $(LOCAL_PATH)/stage1

LOCAL_MODULE := grub_stage1
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/grub

LOCAL_SYSTEM_SHARED_LIBRARIES :=
LOCAL_SHARED_LIBRARIES :=

include $(BUILD_RAW_EXECUTABLE)

$(LOCAL_BUILT_MODULE) : PRIVATE_LINK_SCRIPT :=
$(LOCAL_BUILT_MODULE) : PRIVATE_LIBS :=
$(LOCAL_BUILT_MODULE) : PRIVATE_RAW_EXECUTABLE_LDFLAGS := \
	-nostdlib -N -Ttext=0x7C00 -melf_i386

###################################################################
###################################################################
## For stage2, we have to do it in several parts.
##   1) Build pre_stage2 that contains all the source.
##   2) Get the size of pre_stage2 from (1) and generate a header file.
##   3) Build the "start sector" with the header file.
##   4) concatenate start + pre_stage2 into stage2.
###################################################################
###################################################################

###################################
## So, build pre_stage2 target  (1)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	stage2/asm.S \
	stage2/bios.c \
	stage2/boot.c \
	stage2/builtins.c \
	stage2/char_io.c \
	stage2/cmdline.c \
	stage2/common.c \
	stage2/console.c \
	stage2/disk_io.c \
	stage2/fsys_ext2fs.c \
	stage2/gunzip.c \
	stage2/serial.c \
	stage2/smp-imps.c \
	stage2/stage2.c \
	stage2/terminfo.c \
	stage2/tparm.c \
	stage2/preset_menu.c

LOCAL_CFLAGS := \
	-Wall -Wmissing-prototypes -Wunused -Wshadow \
	-Wpointer-arith -falign-jumps=1 -falign-loops=1 \
	-falign-functions=1 -Wundef

LOCAL_CFLAGS += -m32 -Os -fno-builtin -nostdinc -fno-reorder-functions -fno-stack-protector

LOCAL_CFLAGS += -DHAVE_CONFIG_H -DFSYS_EXT2FS=1 -DSUPPORT_SERIAL=1

LOCAL_CFLAGS += -DPRESET_MENU_EXTERNAL

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/stage1 \
	$(LOCAL_PATH)/stage2

LOCAL_MODULE := grub_pre_stage2
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/grub

LOCAL_SYSTEM_SHARED_LIBRARIES :=
LOCAL_SHARED_LIBRARIES :=

include $(BUILD_RAW_EXECUTABLE)

$(LOCAL_BUILT_MODULE) : PRIVATE_LINK_SCRIPT :=
$(LOCAL_BUILT_MODULE) : PRIVATE_LIBS :=
$(LOCAL_BUILT_MODULE) : PRIVATE_RAW_EXECUTABLE_LDFLAGS := \
	-nostdlib -N -Ttext=0x8200 -melf_i386

#############################################
## Generate the stage2 start file  (2) + (3)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	stage2/start.S

LOCAL_CFLAGS := \
	-Wall -Wmissing-prototypes -Wunused -Wshadow \
	-Wpointer-arith -falign-jumps=1 -falign-loops=1 \
	-falign-functions=1 -Wundef

LOCAL_CFLAGS += -m32 -Os -fno-builtin -nostdinc -fno-reorder-functions -fno-stack-protector

LOCAL_CFLAGS += -DHAVE_CONFIG_H -DFSYS_EXT2FS=1 -DSUPPORT_SERIAL=1

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/stage1 \
	$(LOCAL_PATH)/stage2

LOCAL_SYSTEM_SHARED_LIBRARIES :=
LOCAL_SHARED_LIBRARIES :=

LOCAL_MODULE := grub_start_stage2
LOCAL_MODULE_TAGS := eng
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/grub

# <generate the header file>
LOCAL_MODULE_CLASS := EXECUTABLES
intermediates := $(call local-intermediates-dir)

STAGE2_SIZE_OBJ := $(intermediates)/stage2_size.h
$(STAGE2_SIZE_OBJ) : PRIVATE_CUSTOM_TOOL = \
	echo "\#define STAGE2_SIZE `stat -c '%s' $<`" > $@

LOCAL_GENERATED_SOURCES := $(STAGE2_SIZE_OBJ)
$(STAGE2_SIZE_OBJ): $(PRODUCT_OUT)/grub/grub_pre_stage2
	@echo "target Generating: $@" 
	$(transform-generated-source)
# </generate the header file>

include $(BUILD_RAW_EXECUTABLE)

$(all_objects): $(STAGE2_SIZE_OBJ)
$(LOCAL_BUILT_MODULE) : PRIVATE_LINK_SCRIPT :=
$(LOCAL_BUILT_MODULE) : PRIVATE_LIBS :=
$(LOCAL_BUILT_MODULE) : PRIVATE_RAW_EXECUTABLE_LDFLAGS := \
	-nostdlib -N -Ttext=0x8200 -melf_i386

#############################################
## Generate the real deal stage2  (4)

include $(CLEAR_VARS)

my_files := $(PRODUCT_OUT)/grub/grub_start_stage2 \
	$(PRODUCT_OUT)/grub/grub_pre_stage2

file := $(PRODUCT_OUT)/grub/grub_stage2
$(file) : $(my_files) 
	@echo "target Creating: $@"
	$(hide) cat $^ > $@
#ALL_PREBUILT += $(file)


#############################################################################
## Generate a full stage1+stage2 bin that we can just drop @ offset 0 on disk
include $(CLEAR_VARS)
grub_stage1 := $(PRODUCT_OUT)/grub/grub_stage1
grub_stage2 := $(PRODUCT_OUT)/grub/grub_stage2
grub_full := $(PRODUCT_OUT)/grub/grub.bin

$(grub_full) : $(grub_stage1) $(grub_stage2)
	@echo "target Generating GRUB bin: $@"
	$(hide) rm -f $@
	$(hide) dd if=$(grub_stage1) of=$@ bs=512 count=1 2>/dev/null
	$(hide) dd if=$(grub_stage2) of=$@ bs=512 seek=1 2>/dev/null
#ALL_PREBUILT += $(grub_full)

endif # x86

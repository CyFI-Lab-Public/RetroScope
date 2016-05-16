# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

OPENFSTSDK=$(ASR_ROOT_DIR)/tools/thirdparty/OpenFst

LOCAL_SRC_FILES:= \
	gr_iface.cpp \
	netw_dump.cpp \
	sub_base.cpp \
	sub_grph.cpp \
	sub_min.cpp \
	sub_supp.cpp \
	grxmlcompile.cpp \
	grxmldoc.cpp \
	hashmap.cpp \

ifndef OPENFSTSDK
LOCAL_SRC_FILES += sub_phon.cpp vocab.cpp
endif

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH) \
	$(ASR_ROOT_DIR)/shared/include \
	$(ASR_ROOT_DIR)/portable/include \
	$(ASR_ROOT_DIR)/srec/include \
	$(ASR_ROOT_DIR)/srec/AcousticModels/include \
	$(ASR_ROOT_DIR)/srec/EventLog/include \
	$(ASR_ROOT_DIR)/srec/Grammar/include \
	$(ASR_ROOT_DIR)/srec/Nametag/include \
	$(ASR_ROOT_DIR)/srec/Recognizer/include \
	$(ASR_ROOT_DIR)/srec/Session/include \
	$(ASR_ROOT_DIR)/srec/Semproc/include \
	$(ASR_ROOT_DIR)/srec/Vocabulary/include \
	external/tinyxml \

LOCAL_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \
	-fexceptions \
	
LOCAL_SHARED_LIBRARIES := \
	libESR_Shared \
	libESR_Portable \
	libSR_AcousticModels \
	libSR_AcousticState \
	libSR_Core \
	libSR_EventLog \
	libSR_G2P \
	libSR_Grammar \
	libSR_Nametag \
	libSR_Session \
	libSR_Semproc \
	libSR_Vocabulary \

ifdef OPENFSTSDK
LOCAL_C_INCLUDES += $(OPENFSTSDK)
LOCAL_CFLAGS +=	-DOPENFSTSDK
LOCAL_SHARED_LIBRARIES += libfst
endif

LOCAL_STATIC_LIBRARIES := \
	libtinyxml \

LOCAL_LDLIBS := \
	-lm \
	-lpthread \
	-ldl \

LOCAL_MODULE:= grxmlcompile

include $(BUILD_HOST_EXECUTABLE)

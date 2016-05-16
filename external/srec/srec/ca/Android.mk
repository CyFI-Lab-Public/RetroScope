# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

common_SRC_FILES:= \
	acc_basi.c \
	cnfd_scr.c \
	par_basi.c \
	pat_basi.c \
	rec_basi.c \
	rec_load.c \
	rec_nbes.c \
	rec_resu.c \
	syn_srec.c \
	utt_basi.c \
	utt_data.c \
	utt_proc.c \
	voc_basi.c \
	\
	../cfront/ca_cms.c \
	../cfront/ca_front.c \
	../cfront/ca_wave.c \
	../cfront/cheldsp4.c \
	../cfront/chelfep.c \
	../cfront/chelmel4.c \
	../cfront/frontobj.c \
	../cfront/frontpar.c \
	../cfront/log_tabl.c \
	../cfront/sp_fft.c \
	../cfront/spec_anl.c \
	../cfront/wav_acc.c \
	\
	../clib/cnorm_tr.c \
	../clib/fpi_tgt.c \
	../clib/imeld_rd.c \
	../clib/imeld_tr.c \
	../clib/jacobi.c \
	../clib/log_add.c \
	../clib/matrix_i.c \
	../clib/matx_ops.c \
	../clib/specnorm.c \
	../clib/srec_arb.c \
	../clib/swicms.c \
	../clib/swimodel.c \
	../clib/voc_read.c \
	../clib/voicing.c \
	\
	../crec/astar.c \
	../crec/astar_pphash.c \
	../crec/c47mulsp.c \
	../crec/get_fram.c \
	../crec/priority_q.c \
	../crec/rec_norm.c \
	../crec/srec.c \
	../crec/srec_context.c \
	../crec/srec_debug.c \
	../crec/srec_eosd.c \
	../crec/srec_initialize.c \
	../crec/srec_results.c \
	../crec/srec_stats.c \
	../crec/srec_tokens.c \
	../crec/text_parser.c \
	../crec/word_lattice.c \
#	../crec/comp_stats.c \

common_C_INCLUDES := \
	$(ASR_ROOT_DIR)/portable/include \
	$(ASR_ROOT_DIR)/shared/include \
	$(ASR_ROOT_DIR)/srec/include \
	$(ASR_ROOT_DIR)/srec/clib \
	$(ASR_ROOT_DIR)/srec/cfront \

common_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \

common_SHARED_LIBRARIES := \
	libESR_Shared \
	libESR_Portable \

common_STATIC_LIBRARIES := \
	libzipfile \
	libunz \

common_TARGET:= libSR_Core


# For the host
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES := $(common_C_INCLUDES)
LOCAL_CFLAGS += $(common_CFLAGS)

LOCAL_SHARED_LIBRARIES := $(common_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES := $(common_STATIC_LIBRARIES)

LOCAL_MODULE := $(common_TARGET)

include $(BUILD_HOST_SHARED_LIBRARY)


# For the device
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES := $(common_C_INCLUDES)
LOCAL_CFLAGS += $(common_CFLAGS)

LOCAL_MODULE := $(common_TARGET)

include $(BUILD_STATIC_LIBRARY)

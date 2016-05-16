ifneq ($(BUILD_TINY_ANDROID),true)

ROOT_DIR := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PATH:= $(ROOT_DIR)

# ---------------------------------------------------------------------------------
# 				Common definitons
# ---------------------------------------------------------------------------------

libmm-venc-def := -g -O3 -Dlrintf=_ffix_r
libmm-venc-def += -D__align=__alignx
libmm-venc-def += -D__alignx\(x\)=__attribute__\(\(__aligned__\(x\)\)\)
libmm-venc-def += -DT_ARM
libmm-venc-def += -Dinline=__inline
libmm-venc-def += -D_ANDROID_
libmm-venc-def += -UENABLE_DEBUG_LOW
libmm-venc-def += -DENABLE_DEBUG_HIGH
libmm-venc-def += -DENABLE_DEBUG_ERROR
libmm-venc-def += -UINPUT_BUFFER_LOG
libmm-venc-def += -UOUTPUT_BUFFER_LOG
libmm-venc-def += -USINGLE_ENCODER_INSTANCE
ifeq ($(TARGET_BOARD_PLATFORM),msm8660)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -UENABLE_GET_SYNTAX_HDR
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm8960)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -DMAX_RES_1080P_EBI
libmm-venc-def += -UENABLE_GET_SYNTAX_HDR
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm8974)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -DMAX_RES_1080P_EBI
libOmxVdec-def += -DPROCESS_EXTRADATA_IN_OUTPUT_PORT
libmm-venc-def += -D_MSM8974_
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm7627a)
libmm-venc-def += -DMAX_RES_720P
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm7630_surf)
libmm-venc-def += -DMAX_RES_720P
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm8610)
libmm-venc-def += -DMAX_RES_720P
libmm-venc-def += -D_MSM8974_
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm8226)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -D_MSM8974_
endif
ifeq ($(TARGET_BOARD_PLATFORM),apq8084)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -DMAX_RES_1080P_EBI
libOmxVdec-def += -DPROCESS_EXTRADATA_IN_OUTPUT_PORT
libmm-venc-def += -D_MSM8974_
endif
ifeq ($(TARGET_BOARD_PLATFORM),mpq8092)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -DMAX_RES_1080P_EBI
libOmxVdec-def += -DPROCESS_EXTRADATA_IN_OUTPUT_PORT
libmm-venc-def += -D_MSM8974_
endif

ifeq ($(TARGET_USES_ION),true)
libmm-venc-def += -DUSE_ION
endif

libmm-venc-def += -D_ANDROID_ICS_
# ---------------------------------------------------------------------------------
# 			Make the Shared library (libOmxVenc)
# ---------------------------------------------------------------------------------

include $(CLEAR_VARS)

libmm-venc-inc      := bionic/libc/include
libmm-venc-inc      += bionic/libstdc++/include
libmm-venc-inc      += $(LOCAL_PATH)/inc
libmm-venc-inc      += $(OMX_VIDEO_PATH)/vidc/common/inc
libmm-venc-inc      += hardware/qcom/media/mm-core/inc
libmm-venc-inc      += hardware/qcom/media/libstagefrighthw
libmm-venc-inc      += hardware/qcom/display/$(TARGET_BOARD_PLATFORM)/libgralloc
libmm-venc-inc      += frameworks/native/include/media/hardware
libmm-venc-inc      += frameworks/native/include/media/openmax
libmm-venc-inc      += hardware/qcom/media/libc2dcolorconvert
libmm-venc-inc      += hardware/qcom/display/$(TARGET_BOARD_PLATFORM)/libcopybit
libmm-venc-inc      += frameworks/av/include/media/stagefright
libmm-venc-inc      += frameworks/av/include/media/hardware
libmm-venc-inc      += $(venc-inc)

LOCAL_MODULE                    := libOmxVenc
LOCAL_MODULE_TAGS               := optional
LOCAL_CFLAGS                    := $(libmm-venc-def)
LOCAL_C_INCLUDES                := $(libmm-venc-inc)

LOCAL_PRELINK_MODULE      := false
LOCAL_SHARED_LIBRARIES    := liblog libutils libbinder libcutils \
                             libc2dcolorconvert libdl

LOCAL_SRC_FILES   := src/omx_video_base.cpp
LOCAL_SRC_FILES   += src/omx_video_encoder.cpp
ifneq ($(filter msm8974 msm8610 msm8226 apq8084 mpq8092,$(TARGET_BOARD_PLATFORM)),)
LOCAL_SRC_FILES   += src/video_encoder_device_v4l2.cpp
else
LOCAL_SRC_FILES   += src/video_encoder_device.cpp
endif

LOCAL_SRC_FILES   += ../common/src/extra_data_handler.cpp

include $(BUILD_SHARED_LIBRARY)

# -----------------------------------------------------------------------------
#  #                       Make the apps-test (mm-venc-omx-test720p)
# -----------------------------------------------------------------------------

include $(CLEAR_VARS)

mm-venc-test720p-inc            := $(TARGET_OUT_HEADERS)/mm-core
mm-venc-test720p-inc            += $(LOCAL_PATH)/inc
mm-venc-test720p-inc            += $(OMX_VIDEO_PATH)/vidc/common/inc
mm-venc-test720p-inc            += hardware/qcom/media/mm-core/inc
mm-venc-test720p-inc            += hardware/qcom/display/$(TARGET_BOARD_PLATFORM)/libgralloc
mm-venc-test720p-inc            += $(venc-inc)

LOCAL_MODULE                    := mm-venc-omx-test720p
LOCAL_MODULE_TAGS               := optional
LOCAL_CFLAGS                    := $(libmm-venc-def)
LOCAL_C_INCLUDES                := $(mm-venc-test720p-inc)
LOCAL_PRELINK_MODULE            := false
LOCAL_SHARED_LIBRARIES          := libmm-omxcore libOmxVenc libbinder liblog

LOCAL_SRC_FILES                 := test/venc_test.cpp
LOCAL_SRC_FILES                 += test/camera_test.cpp
LOCAL_SRC_FILES                 += test/venc_util.c
LOCAL_SRC_FILES                 += test/fb_test.c

include $(BUILD_EXECUTABLE)

# -----------------------------------------------------------------------------
# 			Make the apps-test (mm-video-driver-test)
# -----------------------------------------------------------------------------

include $(CLEAR_VARS)

venc-test-inc                   += $(LOCAL_PATH)/inc
venc-test-inc                   += hardware/qcom/display/$(TARGET_BOARD_PLATFORM)/libgralloc
venc-test-inc                   += $(venc-inc)

LOCAL_MODULE                    := mm-video-encdrv-test
LOCAL_MODULE_TAGS               := optional
LOCAL_C_INCLUDES                := $(venc-test-inc)
LOCAL_C_INCLUDES                += hardware/qcom/media/mm-core/inc

LOCAL_PRELINK_MODULE            := false

LOCAL_SRC_FILES                 := test/video_encoder_test.c
LOCAL_SRC_FILES                 += test/queue.c

include $(BUILD_EXECUTABLE)

endif #BUILD_TINY_ANDROID

# ---------------------------------------------------------------------------------
# 					END
# ---------------------------------------------------------------------------------

ifneq ($(BUILD_TINY_ANDROID),true)

ROOT_DIR := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PATH:= $(ROOT_DIR)

# ---------------------------------------------------------------------------------
# 				Common definitons
# ---------------------------------------------------------------------------------

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
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm8960)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -DMAX_RES_1080P_EBI
endif
ifeq ($(TARGET_BOARD_PLATFORM),msm8974)
libmm-venc-def += -DMAX_RES_1080P
libmm-venc-def += -DMAX_RES_1080P_EBI
libmm-venc-def += -DBADGER
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
libmm-venc-inc      := $(LOCAL_PATH)/inc
libmm-venc-inc      += $(OMX_VIDEO_PATH)/vidc/common/inc
libmm-venc-inc      += hardware/qcom/media/mm-core/inc
libmm-venc-inc      += hardware/qcom/media/libstagefrighthw

ifneq ($(filter msm8974 msm8x74,$(TARGET_BOARD_PLATFORM)),)
libmm-venc-inc      += hardware/qcom/display/msm8974/libgralloc
else
libmm-venc-inc      += hardware/qcom/display/msm8960/libgralloc
endif

libmm-venc-inc      += frameworks/native/include/media/hardware
libmm-venc-inc      += frameworks/native/include/media/openmax
libmm-venc-inc      += hardware/qcom/media/libc2dcolorconvert
ifneq ($(filter msm8974 msm8x74,$(TARGET_BOARD_PLATFORM)),)
libmm-venc-inc      += hardware/qcom/display/msm8974/libcopybit
else
libmm-venc-inc      += hardware/qcom/display/msm8960/libcopybit
endif
libmm-venc-inc      += frameworks/av/include/media/stagefright



LOCAL_MODULE                    := libOmxVenc
LOCAL_MODULE_TAGS               := optional
LOCAL_CFLAGS                    := $(libmm-venc-def)
LOCAL_C_INCLUDES                := $(libmm-venc-inc)

LOCAL_PRELINK_MODULE      := false
LOCAL_SHARED_LIBRARIES    := liblog libutils libbinder libcutils \
                             libc2dcolorconvert libdl

LOCAL_SRC_FILES   := src/omx_video_base.cpp
LOCAL_SRC_FILES   += src/omx_video_encoder.cpp
ifeq ($(TARGET_BOARD_PLATFORM),msm8974)
LOCAL_SRC_FILES   += src/video_encoder_device_copper.cpp
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
ifneq ($(filter msm8974 msm8x74,$(TARGET_BOARD_PLATFORM)),)
mm-venc-test720p-inc            += hardware/qcom/display/msm8974/libgralloc
else
mm-venc-test720p-inc            += hardware/qcom/display/msm8960/libgralloc
endif

LOCAL_MODULE                    := mm-venc-omx-test720p
LOCAL_MODULE_TAGS               := optional
LOCAL_CFLAGS                    := $(libmm-venc-def)
LOCAL_C_INCLUDES                := $(mm-venc-test720p-inc)
LOCAL_PRELINK_MODULE            := false
LOCAL_SHARED_LIBRARIES          := libmm-omxcore libOmxVenc libbinder

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


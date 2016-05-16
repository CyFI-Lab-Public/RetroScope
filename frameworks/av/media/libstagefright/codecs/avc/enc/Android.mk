LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    src/avcenc_api.cpp \
    src/bitstream_io.cpp \
    src/block.cpp \
    src/findhalfpel.cpp \
    src/header.cpp \
    src/init.cpp \
    src/intra_est.cpp \
    src/motion_comp.cpp \
    src/motion_est.cpp \
    src/rate_control.cpp \
    src/residual.cpp \
    src/sad.cpp \
    src/sad_halfpel.cpp \
    src/slice.cpp \
    src/vlc_encode.cpp


LOCAL_MODULE := libstagefright_avcenc

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/src \
    $(LOCAL_PATH)/../common/include \
    $(TOP)/frameworks/av/media/libstagefright/include \
    $(TOP)/frameworks/native/include/media/openmax

LOCAL_CFLAGS := \
    -DOSCL_IMPORT_REF= -DOSCL_UNUSED_ARG= -DOSCL_EXPORT_REF=

include $(BUILD_STATIC_LIBRARY)

################################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        SoftAVCEncoder.cpp

LOCAL_C_INCLUDES := \
        frameworks/av/media/libstagefright/include \
        frameworks/native/include/media/hardware \
        frameworks/native/include/media/openmax \
        $(LOCAL_PATH)/src \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/../common/include \
        $(LOCAL_PATH)/../common

LOCAL_CFLAGS := \
    -DOSCL_IMPORT_REF= -DOSCL_UNUSED_ARG= -DOSCL_EXPORT_REF=


LOCAL_STATIC_LIBRARIES := \
        libstagefright_avcenc

LOCAL_SHARED_LIBRARIES := \
        libstagefright \
        libstagefright_avc_common \
        libstagefright_enc_common \
        libstagefright_foundation \
        libstagefright_omx \
        libutils \
        liblog \
        libui


LOCAL_MODULE := libstagefright_soft_h264enc
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

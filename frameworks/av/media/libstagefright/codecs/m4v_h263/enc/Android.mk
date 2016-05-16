LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    src/bitstream_io.cpp \
    src/combined_encode.cpp \
    src/datapart_encode.cpp \
    src/dct.cpp \
    src/findhalfpel.cpp \
    src/fastcodemb.cpp \
    src/fastidct.cpp \
    src/fastquant.cpp \
    src/me_utils.cpp \
    src/mp4enc_api.cpp \
    src/rate_control.cpp \
    src/motion_est.cpp \
    src/motion_comp.cpp \
    src/sad.cpp \
    src/sad_halfpel.cpp \
    src/vlc_encode.cpp \
    src/vop.cpp


LOCAL_MODULE := libstagefright_m4vh263enc

LOCAL_CFLAGS := \
    -DBX_RC \
    -DOSCL_IMPORT_REF= -DOSCL_UNUSED_ARG= -DOSCL_EXPORT_REF=

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/src \
    $(LOCAL_PATH)/include \
    $(TOP)/frameworks/av/media/libstagefright/include \
    $(TOP)/frameworks/native/include/media/openmax

include $(BUILD_STATIC_LIBRARY)

################################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        SoftMPEG4Encoder.cpp

LOCAL_C_INCLUDES := \
        frameworks/av/media/libstagefright/include \
        frameworks/native/include/media/openmax \
        frameworks/native/include/media/hardware \
        $(LOCAL_PATH)/src \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/../common/include \
        $(LOCAL_PATH)/../common

LOCAL_CFLAGS := \
    -DBX_RC \
    -DOSCL_IMPORT_REF= -DOSCL_UNUSED_ARG= -DOSCL_EXPORT_REF=


LOCAL_STATIC_LIBRARIES := \
        libstagefright_m4vh263enc

LOCAL_SHARED_LIBRARIES := \
        libstagefright \
        libstagefright_enc_common \
        libstagefright_foundation \
        libstagefright_omx \
        libutils \
        liblog \
        libui


LOCAL_MODULE := libstagefright_soft_mpeg4enc
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

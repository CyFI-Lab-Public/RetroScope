OMX_VIDEO_PATH := $(call my-dir)
include $(CLEAR_VARS)

include $(OMX_VIDEO_PATH)/vidc/vdec/Android.mk
include $(OMX_VIDEO_PATH)/vidc/venc/Android.mk
include $(OMX_VIDEO_PATH)/DivxDrmDecrypt/Android.mk

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
      SoftAAC2.cpp

LOCAL_C_INCLUDES := \
      frameworks/av/media/libstagefright/include \
      frameworks/native/include/media/openmax \
      external/aac/libAACdec/include \
      external/aac/libPCMutils/include \
      external/aac/libFDK/include \
      external/aac/libMpegTPDec/include \
      external/aac/libSBRdec/include \
      external/aac/libSYS/include

LOCAL_CFLAGS :=

LOCAL_STATIC_LIBRARIES := libFraunhoferAAC

LOCAL_SHARED_LIBRARIES := \
      libstagefright_omx libstagefright_foundation libutils libcutils liblog

LOCAL_MODULE := libstagefright_soft_aacdec
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
